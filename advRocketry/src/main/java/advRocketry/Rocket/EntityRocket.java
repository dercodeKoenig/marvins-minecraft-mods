package advRocketry.Rocket;

import ARLib.gui.GuiHandlerEntity;
import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.network.PacketEntity;
import ARLib.utils.DimensionUtils;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.Blocks.FuelTank;
import advRocketry.Blocks.RocketMotor;
import advRocketry.Blocks.Seat;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemUtils;
import advRocketry.Registry;
import advRocketry.Rocket.RocketPrograms.ProgramNavigateToPlanetPosition;
import advRocketry.utils.CelestialUtils;
import advRocketry.utils.ClientUtils;
import advRocketry.utils.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.fluids.FluidStack;
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
    public RocketFuelTank fuelTank = null;

    public static class RocketFuelTank extends FluidTank {
        EntityRocket rocket;

        public RocketFuelTank(int capacity, EntityRocket rocket) {
            super(capacity);
            this.rocket = rocket;
        }

        // the weight calculation uses fuel to calculate weight.
        // the client usually has no idea about the fuel tank so it needs to be synced to client
        public void onContentsChanged() {
            if (!rocket.level().isClientSide) {
                CompoundTag info = new CompoundTag();
                info.put("fuelTank", rocket.fuelTank.writeToNBT(rocket.level().registryAccess(), new CompoundTag()));
                rocket.sendToClients(info);
            }
        }
    }

    // cached values
    private float cachedThrust = -1;
    private int cachedFuelRate = -1;
    private ArrayList<BlockPos> cachedEnginePositions = null;
    private ArrayList<BlockPos> cachedSeatPositions = null;

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
    public RocketController controller;

    // smooth position interpolation when server sends position update
    private double lerpX, lerpY, lerpZ;
    int lerpSteps;
    private Vec3 lerpDeltaMovement;
    int lerpDeltaMovementSteps;

    // render variables
    public Map<RenderType, RenderData> renderDataMap = new LinkedHashMap<>();
    public int lastLight = 0;
    public boolean requiresMeshUpdate = false;

    // for space travel
    public Vec3 universePosition = new Vec3(0, 0, 0);
    public double universeTravelSpeed = 0; // simplified, this should be vec3 but we just float and the direction = heading
    public double universeTravelAccelerationCurrent = 0; // to make it more smooth, i interpolate to the target acceleration
    public Vec3 universeHeading = new Vec3(0, 1, 0);
    public Vec3 universeTargetHeading = new Vec3(0, 1, 0);
    public Vec3 universeFront = new Vec3(0, 0, 1);

    // passenger
    Map<UUID, BlockPos> passengers = new HashMap<>();
    private boolean firstTick = true; // used to fix client out of sync with rocket, needs unmount and remount, minecraft bug maybe?

    // gui
    public GuiHandlerEntity guiHandler;
    public ARLib.gui.modules.guiModuleText infoText;
    public int temporaryInfoTimeout = 0; // for temporary messages like planet can not be reached... display the alternate info for a few ticks

    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);
        guiHandler = new GuiHandlerEntity(this);
        blocks = new HashMap<>();
        blockEntities = new HashMap<>();
        size = new Vec3i(1, 1, 1);
        fuelTank = new RocketFuelTank(0, this);

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
                requiresMeshUpdate = true;
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
        rocket.fuelTank = new RocketFuelTank(fuelCapacity, rocket);
        rocket.refreshDimensions();
        rocket.makeGui();
        return rocket;
    }


    public void closeVertexBuffer() {
        if (FMLEnvironment.dist.isClient()) {
            RenderSystem.recordRenderCall(() -> {
                for (RenderData data : renderDataMap.values()) {
                    data.vertexBuffer.close();
                }
            });
        }
    }

    /// /  Entity class overrides ////

    @Override
    public void onAddedToLevel() {
        if (level().isClientSide) {
            CompoundTag req = new CompoundTag();
            req.putInt("ping", 0);
            PacketDistributor.sendToServer(PacketEntity.getEntityPacket(this, req));
            firstTick = true;
            lerpSteps = -1;
            lerpDeltaMovementSteps = -1;
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

    @Override
    public void onBelowWorld() {
        if (currentProgram == null)
            super.onBelowWorld();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            if (!level().isClientSide) {
                player.startRiding(this);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    /// / passenger logic ////

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.passengers.size() < getSeatPositions().size();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (level().isClientSide) {
            if (passenger == Minecraft.getInstance().player) {
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
        if (!level().isClientSide) {
            if (passengers.keySet().contains(passenger.getUUID())) return;
            ArrayList<BlockPos> seats = new ArrayList<>(this.getSeatPositions());
            Collections.shuffle(seats, new Random());
            for (BlockPos seatPos : seats) {
                if (!passengers.values().contains(seatPos)) {
                    passengers.put(passenger.getUUID(), seatPos);
                    break;
                }
            }
            setPassengersPositions(passengers);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide) {
            if (passenger == Minecraft.getInstance().player) {
                Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
        if (!level().isClientSide) {
            passengers.remove(passenger.getUUID());
            setPassengersPositions(passengers);
        }
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        UUID entityUUID = entity.getUUID();
        BlockPos seatPos = passengers.get(entityUUID);
        if (seatPos == null) return new Vec3(0, 0, 0); // this should never happen
        return RotationUtils.localToWorld(this, new Vec3(seatPos.getX() + 0.5, seatPos.getY() + 0.2, seatPos.getZ() + 0.5));
    }


    /// / smooth Motion / Position lerp system ////

    // we need slow movement but also the correct initial positions / movements when the entity loads, for example after dimension change
    public void lerpMotion(double x, double y, double z) {
        if (lerpDeltaMovementSteps < 0) {
            setDeltaMovement(x, y, z);
            lerpDeltaMovementSteps = 0;
        } else {
            this.lerpDeltaMovement = new Vec3(x, y, z);
            if (currentProgram != null)
                // let the program do most of the job or it could jump around if server/client slightly desync
                this.lerpDeltaMovementSteps = 20 * 120;
            else
                // normal lerp on ground
                this.lerpDeltaMovementSteps = 20 * 1;
        }
    }

    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (lerpSteps < 0) {
            setPos(x, y, z);
            lerpSteps = 0;
        } else {
            this.lerpX = x;
            this.lerpY = y;
            this.lerpZ = z;
            float distance = (float) position().distanceTo(new Vec3(x, y, z));
            this.lerpSteps = (int) (20 + distance * 50); // dynamic time, fast sync for little correction, slow sync for large correction

        }
        this.setRot(yRot, xRot);
    }

    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    /// / get and set methods ////

    public void setHeadingAndFrontDirect(Vec3 heading, Vec3 front) {
        this.defaultTargetHeading = heading;
        this.heading = heading;
        this.front = front;
    }

    public void setPassengersPositions(Map<UUID, BlockPos> passengers) {
        this.passengers = passengers;
        CompoundTag tag = new CompoundTag();
        tag.put("passengers", RocketSaveAndLoad.savePassengerPositions(passengers));
        sendToClients(tag);
    }

    public Map<UUID, BlockPos> getPassengersPositions() {
        return passengers;
    }

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

    public void setMainEnginesBootup(int bootup, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && this.mainEnginesBootup != bootup) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("mainEnginesBootup", bootup);
            sendToClients(tag);
        }
        this.mainEnginesBootup = bootup;
    }

    public int getMainEnginesBootUp() {
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

    public BlockPos getLastLaunchPosition() {
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

    public Vec3 getDefaultTargetHeading() {
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

    public Vec3 getTargetFront() {
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

    public RocketProgram getCurrentProgram() {
        return currentProgram;
    }

    /// / main rocket methods ////

    public void endProgram() {
        setProgramAndSync(null);
    }


    @Override
    public void tick() {

        super.tick();

        if (!level().isClientSide) {
            guiHandler.serverTick();

            // if out of fuel, end program
            if (fuelTank.isEmpty() && !level().dimension().location().equals(RocketTravelDimension.dimId))
                endProgram();

            if (temporaryInfoTimeout > 0) {
                temporaryInfoTimeout--;
            } else {
                String newInfotext = "";
                newInfotext += "Thrust max: " + ((float) Math.round(getThrustMax() * 100) / 100) + "\n";
                newInfotext += "Mass: " + ((float) Math.round(getMass() * 100) / 100) + "\n";
                newInfotext += "Weight: " + ((float) Math.round(getMass() * getGravity() * 100) / 100) + "\n";
                newInfotext += "Thrust: " + Math.round(controller.getCurrentThrust() * 100) + "%";
                infoText.setTextAndSync(newInfotext);
            }
        }

        if (firstTick) {
            firstTick = false;
            // fix the out of sync bug where the player is not where the rocket is
            // unmounting and remounting will trigger some syncing again and make the player at the correct position
            if (level().isClientSide) {
                if (Minecraft.getInstance().player.getVehicle() == this) {
                    Minecraft.getInstance().player.stopRiding();
                    Minecraft.getInstance().player.startRiding(this, true);
                }
            }
        }

        // lerp logic for smooth position sync
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpTargetX(), this.lerpTargetY(), this.lerpZ, this.getYRot(), this.getXRot());
            --this.lerpSteps;
        }
        if (this.lerpDeltaMovementSteps > 0) {
            this.addDeltaMovement(new Vec3((this.lerpDeltaMovement.x - this.getDeltaMovement().x) / (double) this.lerpDeltaMovementSteps, (this.lerpDeltaMovement.y - this.getDeltaMovement().y) / (double) this.lerpDeltaMovementSteps, (this.lerpDeltaMovement.z - this.getDeltaMovement().z) / (double) this.lerpDeltaMovementSteps));
            --this.lerpDeltaMovementSteps;
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

        // tick rocket controller
        controller.tick();

        // run program or shutdown
        if (currentProgram != null)
            currentProgram.run(this);
        else {
            setTargetPosition(null, false);
            enableSecondaryEngines(false, false);
            enableMainEngines(false, false);
        }

        applyGravity();


        Dimension myDimension = DimensionManager.getDimensionManager(level().isClientSide).get(level().dimension().location());

        // simulate some air friction
        if (getDeltaMovement().length() > 0.01) { // you really dont want to normalize 0 vector. velocity will become like (NaN, Infinity, NaN) and the game freezes forever. took me 2 hours to realize this
            float atmDensity = 0;
            if (myDimension != null)
                atmDensity = myDimension.getAtmosphereDensity();
            Vec3 airBreak = getDeltaMovement().normalize().scale(-1 * atmDensity * size.getY() * getDeltaMovement().length() * 0.01 / getMass());
            if (airBreak.length() > getDeltaMovement().length()) {
                setDeltaMovement(0, 0, 0);
            } else {
                setDeltaMovement(getDeltaMovement().add(airBreak));
            }
        }


        move(MoverType.SELF, getDeltaMovement());

        // when in space travel dim, make sure to keep chunk loaded!!
        if (!level().isClientSide) {
            if (GlobalTime.getGlobalTime() % 100 == 0) {
                if (level().dimension().location().equals(RocketTravelDimension.dimId)) {
                    RocketTravelDimension.keepChunkLoaded(chunkPosition());
                }
            }
        }
    }

    public boolean launch(ItemStack navigationItem) {

        Level targetLevel = null;
        BlockPos targetPos = null;

        if (navigationItem.getItem() instanceof ItemLinker linker) {
            // navigate using linker item
            CompoundTag tag = ItemUtils.getStacktagOrEmpty(navigationItem);
            if (tag.contains("p") && tag.contains("l")) {
                // extract level & pos
                targetPos = NbtUtils.readBlockPos(tag, "p").get();
                targetLevel = DimensionUtils.getDimensionLevelServer(tag.getString("l"));
            }
        }
        if (navigationItem.getItem() instanceof ItemPlanetIdChip idChip) {
            ResourceLocation targetLocation = ItemPlanetIdChip.getSelectedDimension(navigationItem);
            if (targetLocation != null) {
                targetPos = getOnPos();
                targetLevel = DimensionUtils.getDimensionLevelServer(targetLocation.toString());
            }
        }

        if (targetLevel != null) {
            ResourceLocation targetLevelId = targetLevel.dimension().location();
            Dimension targetDimesion = DimensionManager.getDimensionManager(level().isClientSide).get(targetLevelId);
            if (targetDimesion.canVisit()) {
                if (targetDimesion.getType() == DimensionProperties.DimensionType.PLANET) {
                    // target level is planet, use planet navigation program
                    ProgramNavigateToPlanetPosition p = new ProgramNavigateToPlanetPosition();
                    p.target = targetPos;
                    p.targetDimensionId = targetLevelId;
                    setProgramAndSync(p);

                    setLastLaunchPosition(blockPosition(), true);
                    temporaryInfoTimeout = 0;
                    return true;
                }
            } else {
                infoText.setTextAndSync("Target invalid");
                temporaryInfoTimeout = 20 * 15;
            }
        } else {
            infoText.setTextAndSync("Target invalid");
            temporaryInfoTimeout = 20 * 15;
        }
        return false;
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

    public EntityRocket teleportTo(ServerLevel target, Vec3 targetPos, Vec3 velocity) {

        setTargetPosition(null, false); // position is probably invalid because dimension change

        // the dimension change is like this:
        // 1: unmount entities, but store where they were seated
        // 1: teleport every entity to the new dimension and put the new uuid to the new seat map
        // 2: teleport rocket
        // 3: find the entities by the new uuid and mount them at random position
        // 4: fix the seat position
        // 5: on client side: trigger remount on first tick because minecraft fails to sync correctly

        // store the passengers to remount them after dimension change at correct positions
        Map<UUID, BlockPos> newPassengerPositions = new HashMap<>();

        // unmount, teleport and store new uuid
        for (Entity passenger : getPassengers()) {
            if (passenger != null) {
                DimensionTransition transition = new DimensionTransition(target, targetPos, new Vec3(0, 0, 0), getYRot(), getXRot(), false, DimensionTransition.DO_NOTHING);
                BlockPos seatPos = getPassengersPositions().get(passenger.getUUID());
                passenger.stopRiding();
                Entity newEntity = passenger.changeDimension(transition);
                newPassengerPositions.put(newEntity.getUUID(), seatPos);
            }
        }

        // teleport rocket
        DimensionTransition transition = new DimensionTransition(target, targetPos, velocity, getYRot(), getXRot(), false, DimensionTransition.DO_NOTHING);
        EntityRocket newRocket = (EntityRocket) changeDimension(transition);

        // remount passengers
        for (UUID passengerUUID : newPassengerPositions.keySet()) {
            Entity e = (target).getEntity(passengerUUID);
            if (e != null) {
                e.startRiding(newRocket);
            }
        }

        // fix passengers positions
        newRocket.setPassengersPositions(newPassengerPositions);

        return newRocket;
    }

    /// / save, load and sync ////

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
                // closing gui client side on button click because rocket no longer exists
            }
            if (id == 3) {
                for (BlockEntity i : blockEntities.values()) {
                    if (i instanceof EntityGuidanceComputer computer) {
                        if (launch(computer.itemStackHandler.getStackInSlot(0))) {
                            guiHandler.signalCloseGui(serverPlayer);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
        readAdditionalSaveData(compoundTag);
    }

    public void sendToClients(CompoundTag compoundTag) {
        PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, compoundTag));
    }

    /// / gui ////

    public void makeGui() {
        guiHandler.modules.clear();

        for (BlockEntity i : blockEntities.values()) {
            if (i instanceof EntityGuidanceComputer computer) {
                guiModuleItemHandlerSlot chipSlot = new guiModuleItemHandlerSlot(0, computer.itemStackHandler, 0, 0, 1, guiHandler, 10, 10);
                guiHandler.modules.add(chipSlot);
            }
        }

        guiModuleFluidTankDisplay fuelDisplay = new guiModuleFluidTankDisplay(1, fuelTank, 0, guiHandler, 155, 10);
        guiHandler.modules.add(fuelDisplay);

        guiModuleButton deconstructButton = new guiModuleButton(2, "deconstruct", guiHandler, 30, 10, 70, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_red.png"), 64, 20){
            public void onButtonClicked() {
                super.onButtonClicked();
                // close the gui on deconstruct, this packet can not be sent by server because the rocket no longer exists
                if(EntityRocket.this.guiHandler.screen instanceof Screen screen){
                    screen.onClose();
                }
            }
        };
        deconstructButton.color = 0xffffffff;
        guiHandler.modules.add(deconstructButton);
        guiModuleButton launchButton = new guiModuleButton(3, "launch", guiHandler, 110, 10, 40, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_black.png"), 64, 20);
        launchButton.color = 0xffffffff;
        guiHandler.modules.add(launchButton);


        infoText = new guiModuleText(4, "info", guiHandler, 10, 40, 0xff000000, false);
        guiHandler.modules.add(infoText);


        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 170, 1000, 1, 0, guiHandler)) {
            guiHandler.modules.add(i);
        }
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 110, 2000, 1, 0, guiHandler)) {
            guiHandler.modules.add(i);
        }
    }

    public void openGui() {
        if (level().isClientSide) {
            guiHandler.openGui(180, 200, true);
        }
    }


    /// / other rocket methods ////

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

    public int getFuelRateMax() {
        if (cachedFuelRate < 0) {
            cachedFuelRate = 0;
            for (BlockState state : blocks.values()) {
                if (state.getBlock() instanceof RocketMotor motor) {
                    cachedFuelRate += motor.getFuelRateMax();
                }
            }
        }
        return cachedFuelRate;
    }

    public float getMass() {
        float mass = 0.00001f; // prevent divide by 0 if no blocks for some reason very important or the game will freeze forever because it might get inf velocity vectors and tries to check inf blocks for collision
        mass += 3f * blocks.size(); // block weight
        mass += getFuel() * 0.0005f; // fuel weight, tank is synced to client
        return mass;
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


    public ArrayList<BlockPos> getSeatPositions() {
        if (cachedSeatPositions == null) {
            if (blocks.isEmpty()) return new ArrayList<>(); // still waiting for block data sync
            cachedSeatPositions = new ArrayList<>();
            for (BlockPos pos : blocks.keySet()) {
                BlockState state = blocks.get(pos);
                if (state.getBlock() instanceof Seat) {
                    cachedSeatPositions.add(pos);
                }
            }
        }
        return cachedSeatPositions;
    }


    public static void onClientTickEvent() {
        Player player = ClientUtils.getSinglePlayer();
        if (player != null && player.getVehicle() instanceof EntityRocket rocket) {
            if (Minecraft.getInstance().options.keyUse.isDown()) {
                rocket.openGui();
                Minecraft.getInstance().options.keyUse.consumeClick();
            }
        }
    }
}
