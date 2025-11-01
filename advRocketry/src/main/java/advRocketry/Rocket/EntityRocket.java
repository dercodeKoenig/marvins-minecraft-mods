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
import advRocketry.utils.CelestialUtils;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

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
    private BlockPos lastLaunchPosition = new BlockPos(0, 0, 0);
    private Vec3 targetPosition = null; // the target for the rocket to move towards
    private boolean canUseSecondaryEngines = true; // enable in space for breaking and fine steering,
    private boolean canUseMainEngines = false;
    private int mainEnginesBootup = 0;
    Vec3 heading = new Vec3(0, 1, 0);
    private Vec3 defaultTargetHeading = new Vec3(0, 1, 0); // the default heading when it does not need to rotate for main engine use
    Vec3 front = new Vec3(0, 0, 1);
    private Vec3 targetFront = new Vec3(0, 0, 1); // the target front, it should rotate around heading to get closer to it
    Vec3 initialFront = new Vec3(0, 0, 1); // the initial front vector when the rocket is created that was used to calculate all the block positions in the rocket
    private RocketProgram currentProgram = null;
    RocketController controller;


    // render variables
    public Map<RenderType, RenderData> renderDataMap = new LinkedHashMap<>();
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

        controller = new RocketController(this);

        if (FMLEnvironment.dist.isClient()) {
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
        rocket.refreshDimensions();
        return rocket;
    }


    public void closeVertexBuffer() {
        RenderSystem.recordRenderCall(() -> {
            for (RenderData data : renderDataMap.values()) {
                data.vertexBuffer.close();
            }
        });
    }

    ////  Entity class overrides ////

    @Override
    public void onAddedToLevel() {
        if (level().isClientSide) {
            CompoundTag req = new CompoundTag();
            req.putInt("ping", 0);
            PacketDistributor.sendToServer(PacketEntity.getEntityPacket(this, req));
        }
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public double getDefaultGravity() {
        if (level().dimension().location().equals(SpaceTravelManager.dimId))
            return 0;
        return 0.08 * CelestialUtils.getGravityMultiplier(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable((float) Math.max(size.getX(), size.getZ()), (float) size.getY());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        openGui();
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

   //// get and set methods ////



    public void enableMainEngines(boolean canUseMainEngines, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.canUseMainEngines != canUseMainEngines) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("canUseMainEngines", canUseMainEngines);
            tag.putInt("mainEnginesBootup", mainEnginesBootup);
            sendToClients(tag);
        }
        this.canUseMainEngines = canUseMainEngines;
    }

    public boolean canUseMainEngines() {
        return canUseMainEngines;
    }

    public void setMainEnginesBootup(int bootup, boolean syncToClient){
        if (!level().isClientSide && syncToClient && this.mainEnginesBootup != bootup) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("mainEnginesBootup", bootup);
            sendToClients(tag);
        }
        this.mainEnginesBootup = bootup;
    }

    public int getMainEnginesBootUp(){
        return this.mainEnginesBootup;
    }

    public void enableSecondaryEngines(boolean canUseSecondaryEngines, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.canUseSecondaryEngines != canUseSecondaryEngines) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("secondaryEngines", canUseSecondaryEngines);
            sendToClients(tag);
        }
        this.canUseSecondaryEngines = canUseSecondaryEngines;
    }

    public boolean canUseSecondaryEngines() {
        return canUseSecondaryEngines;
    }

    public void setLastLaunchPosition(BlockPos target, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && !Objects.equals(target, lastLaunchPosition)) {
            CompoundTag tag = new CompoundTag();
            tag.put("lastLaunchPosition", Utils.serializeVec3i(target));
            sendToClients(tag);
        }
        lastLaunchPosition = target;
    }

    public  BlockPos getLastLaunchPosition(){
        return lastLaunchPosition;
    }

    public void setDefaultTargetHeading(Vec3 target, boolean syncToClient) {
        target = target.normalize();
        if (!level().isClientSide && syncToClient && !Objects.equals(target, defaultTargetHeading)) {
            CompoundTag tag = new CompoundTag();
            tag.put("defaultTargetHeading", Utils.serializeVec3(target));
            sendToClients(tag);
        }
        defaultTargetHeading = target;
    }

    public Vec3 getDefaultTargetHeading(){
        return defaultTargetHeading;
    }

    public void setTargetFront(Vec3 target, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && !Objects.equals(target, targetFront)) {
            CompoundTag tag = new CompoundTag();
            tag.put("targetFront", Utils.serializeVec3(target));
            sendToClients(tag);
        }
        targetFront = target;
    }

    public Vec3 getTargetFront(){
        return targetFront;
    }

    public void setTargetPosition(@Nullable Vec3 target, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && !Objects.equals(target, targetPosition)) {
            CompoundTag tag = new CompoundTag();
            tag.put("targetPosition", Utils.serializeVec3(target));
            sendToClients(tag);
        }
        targetPosition = target;
    }

    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    public void setProgramAndSync(RocketProgram program) {
        if (!level().isClientSide) {
            CompoundTag tag = new CompoundTag();
            tag.put("currentProgram", RocketProgram.saveToNbt(program));
            sendToClients(tag);
        }
        currentProgram = program;
    }

    public RocketProgram getCurrentProgram(){
        return currentProgram;
    }

    //// main rocket methods ////

    public void endProgram() {
        currentProgram = null;
        setTargetPosition(null, false);
        enableSecondaryEngines(false, false);
        enableMainEngines(false, false);

        if (!level().isClientSide) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("endRocketProgram", 0);
            sendToClients(tag);
        }
    }


    @Override
    public void tick() {
        if (!level().isClientSide) {
            guiHandler.serverTick();
        }


        // tick engine bootup / shutdown
        if (canUseMainEngines) {
            if (mainEnginesBootup < ENGINE_BOOT_TIME) {
                mainEnginesBootup++;
            }
        } else {
            if (mainEnginesBootup > 0) {
                mainEnginesBootup--;
            }
        }


        controller.tick();

        //setTargetFront(new Vec3(0, 0, 1));

        if (currentProgram != null)
            currentProgram.run(this);

        applyGravity();

        // simulate some air friction
        float atmDensity = 0;
        Dimension myDimension = DimensionManager.get(level().dimension().location());
        if (myDimension != null)
            atmDensity = myDimension.getAtmosphereDensity();
        Vec3 airBreak = getDeltaMovement().normalize().scale(-1 * atmDensity * size.getY() * getDeltaMovement().length() * 0.01 / getMass());
        setDeltaMovement(getDeltaMovement().add(airBreak));


        // Move the entity based on the new velocity vector (getDeltaMovement)
        move(MoverType.SELF, getDeltaMovement());

        if (GlobalTime.getGlobalTime() % 100 == 0) {
            System.out.println(level().isClientSide + ":  still ticking at " + blockPosition());

            if (!level().isClientSide) {
                if (level() == DimensionManager.getServerLevel(level().getServer(), SpaceTravelManager.dimId)) {
                    System.out.println("i am in space! " + blockPosition());
                    SpaceTravelManager.keepChunkLoaded(chunkPosition());
                }
            }
        }
    }

    public void launch() {
        ProgramNavigateToPlanetPosition p = new ProgramNavigateToPlanetPosition();
        p.targetDimensionId = level().dimension().location();
        //p.targetDimensionId = ResourceLocation.fromNamespaceAndPath("adv_rocketry", "moon2");
        p.targetDimensionId = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        p.target = new BlockPos((int) position().x + random.nextInt() % 10 - 5, 0, (int) position().z + random.nextInt() % 10 - 5);
        setProgramAndSync(p);
    }

    public void deconstruct() {
        Vec3 minPos = position().subtract(new Vec3((double) size.getX() / 2, 0, (double) size.getZ() / 2));
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

    //// save, load and sync ////

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        RocketSaveAndLoad.readAdditionalSaveData(this, compoundTag);
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
            sendToClients(additionalSaveData);
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
        readAdditionalSaveData(compoundTag);

        if (compoundTag.contains("endRocketProgram"))
            endProgram();
    }

    public void sendToClients(CompoundTag compoundTag) {
        PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, compoundTag));
    }

//// gui ////

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

    public int getFuel() {
        return fuelTank.getFluidAmount();
    }

    public float getMass() {
        return 3f * blocks.size();
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

}
