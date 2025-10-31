package advRocketry.Rocket;

import ARLib.gui.GuiHandlerEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketEntity;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.Blocks.FuelTank;
import advRocketry.Blocks.RocketMotor;
import advRocketry.Dimension.*;
import advRocketry.Registry;
import advRocketry.Rocket.RocketUtils.ProgramNavigateToPlanetPosition;
import advRocketry.Rocket.RocketUtils.RocketController;
import advRocketry.Rocket.RocketUtils.RotationUtils;
import advRocketry.utils.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EntityRocket extends Entity implements INetworkTagReceiver {

    // static variables
    public static int ENGINE_BOOT_TIME = 100;

    // rocket structure
    public Map<BlockPos, BlockState> blocks;
    public Map<BlockPos, BlockEntity> blockEntities;
    public Vec3i size;
    public FluidTank fuelTank = null;

    public ItemStack usedNavigationItem = ItemStack.EMPTY; // the current one used (guidance computer item can be overwritten in launch terminal)

    // cached values
    private float cachedThrust = -1;
    private ArrayList<BlockPos> cachedEnginePositions = null;

    // rocket control
    public BlockPos lastLaunchPosition = new BlockPos(0, 0, 0);
    public Vec3 targetPosition = null; // the target for the rocket to move towards
    boolean canUseSecondaryEngines = true; // enable in space for breaking and fine steering,
    boolean shouldEnableMainEngines = false;
    int mainEnginesBootup = 0;
    public Vec3 heading = new Vec3(0, 1, 0);
    public Vec3 targetHeading = new Vec3(0, 0, 0);
    public Vec3 defaultTargetHeading = new Vec3(0, 1, 0); // the default heading when it does not need to rotate for main engine use
    public Vec3 front = new Vec3(0, 0, 1);
    public Vec3 targetFront = new Vec3(0, 0, 1); // the target front, it should rotate around heading to get closer to it
    public Vec3 initialFront = new Vec3(0, 0, 1); // the initial front vector when the rocket is created that was used to calculate all the block positions in the rocket
    public double controllerKDMultiplier = 1;
    public RocketProgram currentProgram = null;
    public double currentThrust = 0;
    public Vec3 currentSecondaryThrust = new Vec3(0, 0, 0);


    // render variables
    public HashMap<RenderType, RenderData> renderDataMap = new HashMap<>();
    public int lastLight = 0;
    public boolean requiresMeshUpdate = false;

    public GuiHandlerEntity guiHandler;

    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);
        guiHandler = new GuiHandlerEntity(this);
        blocks = new HashMap<>();
        blockEntities = new HashMap<>();
        size = new Vec3i(1, 1, 1);
        fuelTank = new FluidTank(0);

        RenderSystem.recordRenderCall(() -> {
            for (RenderType type : RenderType.chunkBufferLayers()) {
                RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(type, false);
                if (!renderDataMap.containsKey(entityRenderType)) {
                    RenderData data = new RenderData();
                    VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                    data.vertexBuffer = vbo;
                    renderDataMap.put(entityRenderType, data);
                }
            }
        });
    }

    public static EntityRocket create(Level level, Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> blockEntities, Vec3i size, Vec3 front) {
        EntityRocket rocket = new EntityRocket(Registry.ENTITY_ROCKET.get(), level);
        rocket.blockEntities = blockEntities;
        rocket.blocks = blocks;
        rocket.front = front;
        rocket.targetFront = front;
        rocket.initialFront = front;
        rocket.size = size;
        int fuelCapacity = 0;
        for (BlockState state : rocket.blocks.values()) {
            if (state.getBlock() instanceof FuelTank fuelTank) {
                fuelCapacity += fuelTank.getFuelCapacity();
            }
        }
        rocket.fuelTank = new FluidTank(fuelCapacity);
        rocket.makeGui();
        return rocket;
    }


    public void closeVertexBuffer() {
        RenderSystem.recordRenderCall(() -> {
            for (RenderData data : renderDataMap.values()) {
                data.vertexBuffer.close();
            }
        });
    }

    @Override
    public void onAddedToLevel() {
        if (level().isClientSide) {
            CompoundTag req = new CompoundTag();
            req.putInt("ping", 0);
            PacketDistributor.sendToServer(PacketEntity.getEntityPacket(this, req));
        }
    }
    @Override
    public float getPickRadius() {
        return (float) size.distManhattan(new Vec3i(0, 0, 0));
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true; // Rocket cannot be damaged
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }


    @Override
    public AABB makeBoundingBox() {
        if (size == null)
            return super.makeBoundingBox(); // happens because minecraft calls makeBoundingBox in constructor before the size value is assigned

        double maxW = Math.max(size.getX(), size.getZ());

        return new AABB(
                position().x - maxW / 2,
                position().y - (double) size.getY() / 2,
                position().z - maxW / 2,
                position().x + maxW / 2,
                position().y + (double) size.getY() / 2,
                position().z + maxW / 2
        );
    }

    @Override
    public double getDefaultGravity() {
        //if(true)return 0;
        Dimension dim = DimensionManager.get(level().dimension().location());
        if (dim != null && dim.getType() == DimensionProperties.PlanetType.SPACE_STATION)
            return 0;
        if (level().dimension().location().equals(SpaceTravelManager.dimId))
            return 0;
        return 0.08;
    }

    public void makeGui() {
        guiHandler.modules.clear();
        guiModuleFluidTankDisplay fuelDisplay = new guiModuleFluidTankDisplay(1, fuelTank, 0, guiHandler, 155, 10);
        guiHandler.modules.add(fuelDisplay);
        for (BlockEntity i : blockEntities.values()) {
            if (i instanceof EntityGuidanceComputer computer) {
                guiModuleItemHandlerSlot chipSlot = new guiModuleItemHandlerSlot(0, computer.itemStackHandler, 0, 0, 1, guiHandler, 10, 10);
                guiHandler.modules.add(chipSlot);
            }
        }

        guiModuleButton deconstructButton = new guiModuleButton(2, "deconstruct", guiHandler, 30, 10, 70, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_red.png"), 64, 20);
        deconstructButton.color = 0xffffffff;
        guiHandler.modules.add(deconstructButton);
        guiModuleButton launchButton = new guiModuleButton(3, "launch", guiHandler, 110, 10, 40, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_black.png"), 64, 20);
        launchButton.color = 0xffffffff;
        guiHandler.modules.add(launchButton);

        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 170, 1000, 1, 0, guiHandler)) {
            guiHandler.modules.add(i);
        }
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 110, 2000, 1, 0, guiHandler)) {
            guiHandler.modules.add(i);
        }
    }

    public void openGui() {
        if (level().isClientSide) {
            //makeGui();
            guiHandler.openGui(180, 200, true);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        openGui();
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    public float getThrustMax() {
        if (cachedThrust < 0) {
            cachedThrust = 0;
            for (BlockState state : blocks.values()) {
                if (state.getBlock() instanceof RocketMotor motor) {
                    cachedThrust += motor.getThrust();
                }
            }
        }
        return cachedThrust;
    }

    public float getBootTimeThrustMultiplier() {
        int halfBootTime = ENGINE_BOOT_TIME / 2;
        if (mainEnginesBootup < halfBootTime) return 0;

        return (float) Math.pow((float) (mainEnginesBootup - halfBootTime) / halfBootTime, 2);
    }

    public int getFuel() {
        return fuelTank.getFluidAmount();
    }

    public float getMass() {
        return 1f * blocks.size();
    }

    public float getMaxAcceleration() {
        return 3f / 20;
    }

    public ArrayList<BlockPos> getEnginePositions() {
        if (cachedEnginePositions == null) {
            if (blocks.isEmpty()) return new ArrayList<>(); // still waiting for block data sync
            cachedEnginePositions = new ArrayList<>();
            for (BlockPos pos : blocks.keySet()) {
                BlockState state = blocks.get(pos);
                if (state.getBlock() instanceof RocketMotor motor) {
                    cachedEnginePositions.add(pos);
                }
            }
        }
        return cachedEnginePositions;
    }

    public void setCurrentThrustAndSync(double thrust) {
        if (currentThrust != thrust) {
            currentThrust = thrust;
            CompoundTag tag = new CompoundTag();
            tag.putDouble("currentThrust", thrust);
            PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, tag));
        }
    }

    public void setCurrentSecondaryThrustAndSync(Vec3 thrust) {
        if (!currentSecondaryThrust.equals(thrust)) {
            currentSecondaryThrust = thrust;
            CompoundTag tag = new CompoundTag();
            tag.put("currentSecondaryThrust", Utils.serializeVec3(thrust));
            PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, tag));
        }
    }

    public boolean shouldEnableMainEngines() {
        return shouldEnableMainEngines;
    }

    public int getMainEnginesBootup() {
        return mainEnginesBootup;
    }

    public void enableMainEngines(boolean canUseMainEngines) {
        if (this.shouldEnableMainEngines != canUseMainEngines) {
            this.shouldEnableMainEngines = canUseMainEngines;
            if (!level().isClientSide) {
                CompoundTag tag = new CompoundTag();
                tag.putBoolean("shouldEnableMainEngines", shouldEnableMainEngines);
                tag.putInt("mainEnginesBootup", mainEnginesBootup);
                PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, tag));
            }
        }
    }

    public boolean canUseSecondaryEngines() {
        return canUseSecondaryEngines;
    }

    public void enableSecondaryEngines(boolean canUseSecondaryEngines) {
        if (this.canUseSecondaryEngines != canUseSecondaryEngines) {
            this.canUseSecondaryEngines = canUseSecondaryEngines;
            if (!level().isClientSide) {
                CompoundTag tag = new CompoundTag();
                tag.putBoolean("secondaryEngines", canUseSecondaryEngines);
                PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, tag));
            }
        }
    }

    public void setTargetHeading(Vec3 target) {
        targetHeading = target;
    }

    public void setTargetFront(Vec3 target) {
        targetFront = target;
    }

    public void setTargetPosition(Vec3 target) {
        targetPosition = target;
    }

    public void endProgram() {
        currentProgram = null;
        setTargetPosition(null);
        controllerKDMultiplier = 1;
        enableSecondaryEngines(false);
        enableMainEngines(false);
        setCurrentThrustAndSync(0);
        setCurrentSecondaryThrustAndSync(new Vec3(0, 0, 0));
    }


    @Override
    public void tick() {
        if (!level().isClientSide) {
            guiHandler.serverTick();
        }

        // tick engine bootup / shutdown
        if (shouldEnableMainEngines) {
            if (mainEnginesBootup < ENGINE_BOOT_TIME) {
                mainEnginesBootup++;
            }
        } else {
            if (mainEnginesBootup > 0) {
                mainEnginesBootup--;
            }
        }

        if (!level().isClientSide) {
            RocketController.tickController(this);
            RocketController.tickRotation(this);

            //setTargetFront(new Vec3(0, 0, 1));

            if (currentProgram != null)
                currentProgram.run(this);
        }

        if (level().isClientSide) {
            if (mainEnginesBootup != 0) {
                float relativeBootTimeLin = (float) mainEnginesBootup / ENGINE_BOOT_TIME;
                for (BlockPos i : getEnginePositions()) {
                    Vec3 worldPos = RotationUtils.localToWorld(this, new Vec3(i.getX() + 0.5, i.getY() + 0.02, i.getZ() + 0.5));
                    boolean shouldCreateParticle = mainEnginesBootup == ENGINE_BOOT_TIME;
                    if (!shouldCreateParticle) {
                        shouldCreateParticle = level().random.nextFloat() <= Math.sqrt(relativeBootTimeLin);
                    }
                    if (shouldCreateParticle) {
                        for (int j = 0; j < 2; j++) {
                            level().addParticle(
                                    Registry.ROCKET_FLAME.get(),
                                    worldPos.x,
                                    worldPos.y,
                                    worldPos.z,
                                    heading.x * -1 * (currentThrust + 1) * relativeBootTimeLin,
                                    heading.y * -1 * (currentThrust + 1) * relativeBootTimeLin,
                                    heading.z * -1 * (currentThrust + 1) * relativeBootTimeLin
                            );
                        }
                    }
                }
            }
        }

        if (!level().isClientSide) {

            applyGravity();

            // simulate some air friction
            float atmDensity = 0;
            Dimension myDimension = DimensionManager.get(level().dimension().location());
            if (myDimension != null)
                atmDensity = myDimension.getAtmosphereDensity();
            Vec3 airBreak = getDeltaMovement().normalize().scale(-1 * atmDensity * size.getY() * getDeltaMovement().length() * 0.01 / getMass());
            setDeltaMovement(getDeltaMovement().add(airBreak));

        }

        // Move the entity based on the new velocity vector (getDeltaMovement)
        move(MoverType.SELF, getDeltaMovement());

        if (GlobalTime.getGlobalTime() % 100 == 0) {
            System.out.println(level().isClientSide + ":  still ticking");

            if (!level().isClientSide) {
                if (level() == DimensionManager.getServerLevel(level().getServer(), SpaceTravelManager.dimId)) {
                    System.out.println("i am in space! " + blockPosition());
                    SpaceTravelManager.keepChunkLoaded(chunkPosition());
                }
            }
        }
    }

    public void launch() {
        /*
        ServerLevel target = DimensionManager.getServerLevel(level().getServer(), SpaceTravelManager.dimId);
        ChunkPos targetPos = SpaceTravelManager.getNextFreeChunkPos();
        BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);
        teleportTo(target, targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ(), new HashSet<>(), getYRot(), getXRot());
        SpaceTravelManager.keepChunkLoaded(targetPos);
         */


        /*
        moveTo(getX(), getY() + 100, getZ()); // entry height
        setDeltaMovement(0, 0, 0); // entry speed
        */

        ProgramNavigateToPlanetPosition p = new ProgramNavigateToPlanetPosition();
        p.targetDimensionId = level().dimension().location();
        p.target = new BlockPos((int) position().x, 0, (int) position().z);
        setPos(position().x, position().y, position().z + 60);
        currentProgram = p;
    }

    public void deconstruct() {
        Vec3 minPos = position().subtract(new Vec3((double) size.getX() / 2, (double) size.getY() / 2, (double) size.getZ() / 2));
        for (BlockPos pos : blocks.keySet()) {
            BlockState state = blocks.get(pos);
            BlockPos target = new BlockPos(
                    (int) Math.round(minPos.x + pos.getX()),
                    (int) Math.round(minPos.y + pos.getY()),
                    (int) Math.round(minPos.z + pos.getZ())
            );
            level().setBlock(target, state, 3);
            if (blockEntities.get(pos) != null) {
                BlockEntity be = blockEntities.get(pos);
                CompoundTag tag = be.saveCustomOnly(level().registryAccess());
                level().getBlockEntity(target).loadCustomOnly(tag, level().registryAccess());
            }
        }
        kill();
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        RocketSaveAndLoad.readAdditionalSaveData(this, compoundTag);
        this.makeGui();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        RocketSaveAndLoad.addAdditionalSaveData(this, compoundTag);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("ping")) {
            CompoundTag additionalSaveData = new CompoundTag();
            addAdditionalSaveData(additionalSaveData);
            CompoundTag info = new CompoundTag();
            info.put("additionalSaveData", additionalSaveData);
            PacketDistributor.sendToPlayer(serverPlayer, PacketEntity.getEntityPacket(this, info));
        }

        if (compoundTag.contains("guiButtonClick")) {
            int id = compoundTag.getInt("guiButtonClick");
            if (id == 2) {
                deconstruct();
            }
            if (id == 3) {
                launch();
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
        if (compoundTag.contains("additionalSaveData")) {
            readAdditionalSaveData(compoundTag.getCompound("additionalSaveData"));
            requiresMeshUpdate = true;
        }

        if (compoundTag.contains("heading")) {
            heading = Utils.deSerializeVec3(compoundTag.getCompound("heading"));
        }
        if (compoundTag.contains("front")) {
            front = Utils.deSerializeVec3(compoundTag.getCompound("front"));
        }

        if (compoundTag.contains("secondaryEngines"))
            canUseSecondaryEngines = compoundTag.getBoolean("secondaryEngines");

        if (compoundTag.contains("shouldEnableMainEngines"))
            shouldEnableMainEngines = compoundTag.getBoolean("shouldEnableMainEngines");
        if (compoundTag.contains("mainEnginesBootup"))
            mainEnginesBootup = compoundTag.getInt("mainEnginesBootup");

        if (compoundTag.contains("currentThrust"))
            currentThrust = compoundTag.getDouble("currentThrust");

        if (compoundTag.contains("currentSecondaryThrust"))
            currentSecondaryThrust = Utils.deSerializeVec3(compoundTag.getCompound("currentSecondaryThrust"));
    }
}
