package advRocketry.Rocket;

import ARLib.gui.GuiHandlerEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketEntity;
import ARLib.utils.DimensionUtils;
import ARLib.utils.VertexBufferCleaner;
import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.Blocks.FuelTank;
import advRocketry.Blocks.RocketMotor;
import advRocketry.Blocks.Seat;
import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.ForcedChunkManager;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Rocket.RocketPrograms.*;
import advRocketry.Utils.ItemUtils;
import advRocketry.Utils.CelestialUtils;
import advRocketry.Utils.ClientUtils;
import advRocketry.Utils.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static advRocketry.Registry.GeneralRegistry.ENTITY_ROCKET;

public class EntityRocket extends Entity implements INetworkTagReceiver {

    // rocket structure
    public Map<BlockPos, BlockState> blocks;
    public Map<BlockPos, BlockEntity> blockEntities;
    public Vec3i size;
    public FluidTank fuelTank;
    public float currentMass;

    // for space travel
    public Vec3 universePosition = new Vec3(0, 0, 0);
    public double universeTravelSpeed = 0; // simplified, this should be vec3 but we just float and the direction = heading
    public Vec3 universeHeading = new Vec3(0, 1, 0);
    public Vec3 universeTargetHeading = new Vec3(0, 1, 0);
    public Vec3 universeFront = new Vec3(0, 0, 1);

    // gui
    public GuiHandlerEntity guiHandler;
    public ARLib.gui.modules.guiModuleText infoText;
    public int temporaryInfoTimeout = 0; // for temporary messages like planet can not be reached... display the alternate info for a few ticks

    // rocket control
    public RocketProgram currentProgram = null;
    public RocketController controller;
    BlockPos dockingStationPos = null;
    Vec3 initialFront = new Vec3(0, 0, 1); // the initial front vector when the rocket is created that was used to calculate all the block positions in the rocket
    Vec3 lastDeltaMovement = Vec3.ZERO; // detect crash when onground and last delta movement is high

    // passenger
    Map<UUID, BlockPos> passengers = new HashMap<>();

    // render variables
    Map<RenderType, RenderData> renderDataMap = new LinkedHashMap<>();
    int lastLight = 0;
    boolean requiresMeshUpdate = true;

    // smooth position interpolation when server sends position update
    double lerpX, lerpY, lerpZ;
    Vec3 lerpDeltaMovement = Vec3.ZERO;
    int lerpSteps;
    int lerpDeltaMovementSteps;

    // used to fix client out of sync with rocket, needs unmount and remount, minecraft bug maybe?
    private boolean firstTick = true;

    // cached values
    private float cachedThrust = -1;
    private int cachedFuelRate = -1;
    private float cachedBlockMass = -1;
    private ArrayList<BlockPos> cachedEnginePositions = null;
    private ArrayList<BlockPos> cachedSeatPositions = null;


    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);

        guiHandler = new GuiHandlerEntity(this);
        blocks = new HashMap<>();
        blockEntities = new HashMap<>();
        size = new Vec3i(1, 1, 1);
        fuelTank = new FluidTank(0);

        controller = new RocketController(this);

        initVertexBuffers();
    }

    public static EntityRocket create(Level level, Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> blockEntities, Vec3i size, Vec3 front) {
        EntityRocket rocket = ENTITY_ROCKET.get().create(level);
        rocket.blockEntities = blockEntities;
        rocket.blocks = blocks;
        rocket.controller.setHeadingAndFrontDirect(new Vec3(0, 1, 0), front);
        rocket.initialFront = front;
        rocket.size = size;
        int fuelCapacity = 0;
        for (BlockState state : rocket.blocks.values()) {
            if (state.getBlock() instanceof FuelTank fuelTank) {
                fuelCapacity += fuelTank.getFuelCapacity();
            }
        }
        rocket.fuelTank = new FluidTank(fuelCapacity);
        rocket.refreshDimensions();
        rocket.makeGui();
        return rocket;
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

    public void initVertexBuffers() {
        if (FMLEnvironment.dist.isClient()) {
            RenderSystem.recordRenderCall(() -> {
                for (RenderType type : RenderType.chunkBufferLayers()) {
                    RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(type, false);
                    if (!renderDataMap.containsKey(entityRenderType)) {
                        RenderData data = new RenderData();
                        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                        data.vertexBuffer = vbo;
                        renderDataMap.put(entityRenderType, data);

                        VertexBufferCleaner.register(this, vbo);
                    }
                }
                requiresMeshUpdate = true;
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
        reapplyPosition(); // make correct bounding box
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
        Dimension myDim = DimensionManager.getDimensionManager(level().isClientSide).get(level().dimension().location());
        if (myDim instanceof RocketTravelDimension || myDim instanceof SpaceStationDimension)
            return 0;
        return 0.08 * CelestialUtils.getGravityMultiplier(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed((float) Math.max(size.getX(), size.getZ()), (float) size.getY());
    }

    public Direction.Axis findClosestAxis(Vec3 direction) {
        double maxX = Math.abs(direction.x);
        double maxY = Math.abs(direction.y);
        double maxZ = Math.abs(direction.z);
        if (maxX > maxY && maxX > maxZ) {
            return Direction.Axis.X;
        }
        if (maxZ > maxY && maxZ > maxX) {
            return Direction.Axis.Z;
        }
        return Direction.Axis.Y;
    }

    @Override
    public AABB makeBoundingBox() {
        if (controller == null) return super.makeBoundingBox();
        if (size == null) return super.makeBoundingBox();
        Vec3 heading = controller.getHeading();
        return makeBoundingBox(findClosestAxis(heading));
    }

    public AABB makeBoundingBox(Direction.Axis axis) {
        double w = Math.max(size.getX(), size.getZ());
        double h = size.getY();
        // i make it -offset on every side to allow for better docking
        // for example a 1x1 rocket should fit through a 1x1 block hole and in full bb it would need infinite precision
        double offset = 0.03;
        if (axis == Direction.Axis.X) {
            return new AABB(
                    position().x - h / 2,
                    position().y - w / 2 + h / 2 + offset,
                    position().z - w / 2 + offset,
                    position().x + h / 2,
                    position().y + w / 2 + h / 2 - offset,
                    position().z + w / 2 - offset
            );
        }
        if (axis == Direction.Axis.Z) {
            return new AABB(
                    position().x - w / 2 + offset,
                    position().y - w / 2 + h / 2 + offset,
                    position().z - h / 2,
                    position().x + w / 2 - offset,
                    position().y + w / 2 + h / 2 - offset,
                    position().z + h / 2
            );
        }
        // normal bb
        return new AABB(
                position().x - w / 2 + offset,
                position().y,
                position().z - w / 2 + offset,
                position().x + w / 2 - offset,
                position().y + h,
                position().z + w / 2 - offset
        );
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
            if (passengers.containsKey(passenger.getUUID())) return;
            ArrayList<BlockPos> seats = new ArrayList<>(this.getSeatPositions());
            Collections.shuffle(seats, new Random());
            for (BlockPos seatPos : seats) {
                if (!passengers.containsValue(seatPos)) {
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
            //System.out.println("lerp movement: "+lerpDeltaMovement.y+" - current: "+getDeltaMovement().y);
            if (currentProgram != null)
                // let the program do most of the job or it could jump around if server/client slightly desync
                this.lerpDeltaMovementSteps = 20 * 10;
            else
                // normal lerp on ground
                this.lerpDeltaMovementSteps = 20;
        }
    }

    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        //System.out.println("lerp: "+y+" - expected: "+lerpY);
        if (lerpSteps < 0) {
            setPos(x, y, z);
            lerpSteps = 0;
        } else {
            this.lerpX = x;
            this.lerpY = y;
            this.lerpZ = z;
            this.lerpSteps = 50;
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

    @Override
    // i use move and not setPos to get the collision, this is important because I forecast lerp target and without collision i can move into ground
    protected void lerpPositionAndRotationStep(int steps, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot) {
        double d0 = (double) 1.0F / (double) steps;
        Vec3 movement = new Vec3(targetX, targetY, targetZ).subtract(new Vec3(getX(), getY(), getZ())).scale(d0);
        move(MoverType.SELF, movement);

        float f = (float) Mth.rotLerp(d0, this.getYRot(), targetYRot);
        float f1 = (float) Mth.lerp(d0, this.getXRot(), targetXRot);
        this.setRot(f, f1);
    }

    /// / get and set methods ////

    public Map<UUID, BlockPos> getPassengersPositions() {
        return passengers;
    }

    public void setPassengersPositions(Map<UUID, BlockPos> passengers) {
        this.passengers = passengers;
        CompoundTag tag = new CompoundTag();
        tag.put("passengers", RocketSaveAndLoad.savePassengerPositions(passengers));
        sendToClients(tag);
    }

    public void setDockingStationPos(Vec3i target, boolean syncToClient) {
        if (!level().isClientSide && syncToClient && !Objects.equals(target, dockingStationPos)) {
            CompoundTag tag = new CompoundTag();
            tag.put("dockingStationPos", Utils.serializeVec3i(target));
            sendToClients(tag);
        }
        if (target == null)
            dockingStationPos = null;
        else {
            dockingStationPos = new BlockPos(target.getX(), target.getY(), target.getZ());
        }
    }

    public BlockPos getDockingStationPos() {
        return dockingStationPos;
    }

    public void setProgramAndSync(RocketProgram program) {
        if (!level().isClientSide) {
            CompoundTag tag = new CompoundTag();
            tag.put("currentProgram", ProgramRegistry.saveToNbt(program));
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

            // display the default info text
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

            // update the mass and sync changes to client
            recalculateMass();
        }

        if (firstTick) {
            firstTick = false;
            // fix the out of sync bug where the player is not where the rocket is
            // unmounting and remounting will trigger some syncing again and make the player at the correct position
            // TODO: can this issue be resolved by force chunk loading before teleport?
            if (level().isClientSide) {
                if (Minecraft.getInstance().player.getVehicle() == this) {
                    Minecraft.getInstance().player.stopRiding();
                    Minecraft.getInstance().player.startRiding(this, true);
                }
            }
        }

        // lerp logic for smooth position sync
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpTargetX(), this.lerpTargetY(), this.lerpTargetZ(), this.getYRot(), this.getXRot());
            --this.lerpSteps;
            // move lerp target along server motion to forecast the target
            // in best case, the lerp target should already match the target given in lerpTo when called
            // it is precise enough
            lerpX += lerpDeltaMovement.x;
            lerpY += lerpDeltaMovement.y;
            lerpZ += lerpDeltaMovement.z;
        }
        if (this.lerpDeltaMovementSteps > 0) {
            this.addDeltaMovement(new Vec3((this.lerpDeltaMovement.x - this.getDeltaMovement().x) / (double) this.lerpDeltaMovementSteps, (this.lerpDeltaMovement.y - this.getDeltaMovement().y) / (double) this.lerpDeltaMovementSteps, (this.lerpDeltaMovement.z - this.getDeltaMovement().z) / (double) this.lerpDeltaMovementSteps));
            --this.lerpDeltaMovementSteps;
        }

        // apply before rocket controller / program runs so they have the correct movement data to work with
        applyGravity();

        // tick rocket controller
        controller.tick();

        // run program or shutdown
        if (currentProgram != null) {
            setDockingStationPos(null, false); // reset it by default BEFORE the program runs so the program can pre-set the next docking station
            currentProgram.run(this);
            for (Entity e : getPassengers()) {
                e.resetFallDistance();
            }
            resetFallDistance();
        } else {
            controller.setTargetPosition(null, false);
            controller.enableSecondaryEngines(false, false);
            controller.enableMainEngines(false, false);
        }


        // this ensures the rocket will not float away in space.
        setDeltaMovement(getDeltaMovement().scale(0.999));
        if (currentProgram == null)
            // more breaking
            setDeltaMovement(getDeltaMovement().scale(0.99));

        // apply the movement
        move(MoverType.SELF, getDeltaMovement());


        // force load chunks when the rocket does a program
        if (!level().isClientSide) {
            if (currentProgram != null) {
                ChunkPos nextChunkPos = chunkPosition();
                ForcedChunkManager.keepChunkForceLoaded(level(), nextChunkPos);
            }
        }


        // detect possible crash landing
        if(!level().isClientSide){
            double safeVelocity = -0.5;
            if(onGround() && lastDeltaMovement.y < safeVelocity) {
                for (Player p : level().players()) {
                    if (p.position().distanceTo(position()) < 64) {
                        p.sendSystemMessage(
                                Component.literal("rocket hit ground too hard: " + lastDeltaMovement.y)
                        );
                        p.sendSystemMessage(
                                Component.literal("safe velocity would be: " + safeVelocity)
                        );
                    }
                }
                breakAndPopRocket();
            }
            lastDeltaMovement = getDeltaMovement();
        }

    }

    /// the normal launch code
    /// missions (asteroid mining, gas mining, satellite deployment) will need their own launch code
    public boolean launch(ItemStack navigationItem) {
        if (level().isClientSide) return false;

        Level targetLevel = null;
        BlockPos targetPos = null;
        String extraInfo = "";

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
            if (targetLocation != null && ItemPlanetIdChip.containsMassData(navigationItem)) {
                targetPos = getOnPos();
                targetLevel = DimensionUtils.getDimensionLevelServer(targetLocation.toString());
            }
            if (!ItemPlanetIdChip.containsMassData(navigationItem)) {
                extraInfo = "missing mass data";
            }
        }

        if (targetLevel != null) {
            ResourceLocation targetLevelId = targetLevel.dimension().location();
            Dimension targetDimension = DimensionManager.getDimensionManager(level().isClientSide).get(targetLevelId);
            if (targetDimension != null && targetDimension.canVisit()) {
                if (targetDimension instanceof PlanetDimension) {
                    // target level is planet, use planet navigation program
                    ProgramNavigateToPlanetPosition p = new ProgramNavigateToPlanetPosition(this, targetLevelId, targetPos);
                    setProgramAndSync(p);
                    temporaryInfoTimeout = 0;
                    return true;
                }
                if (targetDimension instanceof SpaceStationDimension) {
                    ProgramNavigateToSpaceStation p = new ProgramNavigateToSpaceStation(this, targetLevelId, targetPos);
                    setProgramAndSync(p);
                    temporaryInfoTimeout = 0;
                    return true;
                }
            } else {
                infoText.setTextAndSync("Target invalid\n" + extraInfo);
                temporaryInfoTimeout = 20 * 15;
            }
        } else {
            infoText.setTextAndSync("Target invalid\n" + extraInfo);
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

    public void popInventory(ItemStackHandler inventory){
        for (int i = 0; i < inventory.getSlots(); i++) {
            Block.popResource(level(),blockPosition().above(),inventory.getStackInSlot(i));
        }
    }
    public void breakAndPopRocket() {
        this.discard();
        // all blocks will pop into the world,
        // also pop inventory for guidance computer and cargo hold
        for (BlockEntity e : blockEntities.values()) {
            if (e instanceof EntityGuidanceComputer c)
                popInventory(c.itemStackHandler);
            if (e instanceof EntityCargoHold c)
                popInventory(c.itemStackHandler);
        }

        for (BlockPos p : blocks.keySet()) {
            BlockState state = blocks.get(p);
            Block.popResource(level(), blockPosition().above(), new ItemStack(state.getBlock(), 1));
        }
    }

    public EntityRocket teleportTo(Level level, Vec3 targetPos, Vec3 velocity) {

        controller.setTargetPosition(null, false); // position is probably invalid because dimension change

        if (level != level() && level instanceof ServerLevel serverLevel) {

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
                    DimensionTransition transition = new DimensionTransition(serverLevel, targetPos, new Vec3(0, 0, 0), getYRot(), getXRot(), false, DimensionTransition.DO_NOTHING);
                    BlockPos seatPos = getPassengersPositions().get(passenger.getUUID());
                    passenger.stopRiding();
                    Entity newEntity = passenger.changeDimension(transition);
                    newPassengerPositions.put(newEntity.getUUID(), seatPos);
                }
            }

            // teleport rocket
            DimensionTransition transition = new DimensionTransition(serverLevel, targetPos, velocity, getYRot(), getXRot(), false, DimensionTransition.DO_NOTHING);
            EntityRocket newRocket = (EntityRocket) changeDimension(transition);

            // remount passengers
            for (UUID passengerUUID : newPassengerPositions.keySet()) {
                Entity e = (serverLevel).getEntity(passengerUUID);
                if (e != null) {
                    e.startRiding(newRocket);
                }
            }

            // fix passengers positions
            newRocket.setPassengersPositions(newPassengerPositions);

            // keep the chunk loaded initially
            ForcedChunkManager.keepChunkForceLoaded(serverLevel, newRocket.chunkPosition());

            return newRocket;
        } else {
            setPos(targetPos);
            setDeltaMovement(velocity);
            ForcedChunkManager.keepChunkForceLoaded(level, chunkPosition());
            return this;
        }
    }

    // sync changes to client for render (for example the fluid level in a fluid container)
    public void onBlockEntityChanged(BlockPos position) {
        BlockEntity blockEntity = blockEntities.get(position);
        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("blockPos", NbtUtils.writeBlockPos(position));
        blockEntityTag.put("blockEntity", blockEntity.saveCustomOnly(registryAccess()));
        CompoundTag info = new CompoundTag();
        info.put("updateBlockEntity", blockEntityTag);
        sendToClients(info);
    }

    /// / save, load and sync ////

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        RocketSaveAndLoad.readAdditionalSaveData(this, compoundTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        RocketSaveAndLoad.addAdditionalSaveData(this, compoundTag);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("ping")) {
            CompoundTag additionalSaveData = new CompoundTag();
            addAdditionalSaveData(additionalSaveData);
            // client can handle special logic on initial load like instant rotation without lerp
            additionalSaveData.put("initialLoad", new CompoundTag());
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

        // notification about mass change
        if (compoundTag.contains("currentMass"))
            currentMass = compoundTag.getFloat("currentMass");

        // notification about change in blockentity (useful for rendering)
        if (compoundTag.contains("updateBlockEntity")) {
            CompoundTag blockTag = compoundTag.getCompound("updateBlockEntity");
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = blocks.get(p);
            CompoundTag blockEntityTag = blockTag.getCompound("blockEntity");
            BlockEntity existingBlockEntity = blockEntities.get(p);
            if (existingBlockEntity != null && existingBlockEntity.isValidBlockState(state))
                existingBlockEntity.loadCustomOnly(blockEntityTag, registryAccess());
            else {
                BlockEntity be = ((EntityBlock) state.getBlock()).newBlockEntity(p, state);
                be.loadCustomOnly(blockEntityTag, registryAccess());
                blockEntities.put(p, be);
            }
        }
    }

    public void sendToClients(CompoundTag compoundTag) {
        PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, compoundTag));
    }

    /// / gui ////

    public void makeGui() {
        guiHandler.modules.clear();

        // add guidance computer slot
        for (BlockEntity i : blockEntities.values()) {
            if (i instanceof EntityGuidanceComputer computer) {
                guiModuleItemHandlerSlot chipSlot = new guiModuleItemHandlerSlot(0, computer.itemStackHandler, 0, 0, 1, guiHandler, 10, 10);
                guiHandler.modules.add(chipSlot);
            }
        }

        // add fuel slot
        guiModuleFluidTankDisplay fuelDisplay = new guiModuleFluidTankDisplay(1, fuelTank, 0, guiHandler, 155, 10);
        guiHandler.modules.add(fuelDisplay);

        // deconstruct button
        guiModuleButton deconstructButton = new guiModuleButton(2, "deconstruct", guiHandler, 30, 10, 70, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_red.png"), 64, 20) {
            public void onButtonClicked() {
                super.onButtonClicked();
                // close the gui on deconstruct, this packet can not be sent by server because the rocket no longer exists
                if (EntityRocket.this.guiHandler.screen instanceof Screen screen) {
                    screen.onClose();
                }
            }
        };
        deconstructButton.color = 0xffffffff;
        guiHandler.modules.add(deconstructButton);

        // launch button
        guiModuleButton launchButton = new guiModuleButton(3, "launch", guiHandler, 110, 10, 40, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_black.png"), 64, 20);
        launchButton.color = 0xffffffff;
        guiHandler.modules.add(launchButton);

        // status / info
        infoText = new guiModuleText(4, "info", guiHandler, 10, 40, 0xff000000, false);
        guiHandler.modules.add(infoText);


        // add player inventory slots
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 190, 1000, 1, 0, guiHandler)) {
            guiHandler.modules.add(i);
        }
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 130, 2000, 1, 0, guiHandler)) {
            guiHandler.modules.add(i);
        }

        // add inventory slots for cargo hold
        guiModuleScrollContainer inventoriesContainer =
                new guiModuleScrollContainer(new ArrayList<>(), 0xffa0a0a0, guiHandler, 10, 80, 162, 40);
        guiHandler.modules.add(inventoriesContainer);

        int x = 0;
        int y = 0;
        int id = 20000;
        for (BlockEntity i : blockEntities.values()) {
            if (i instanceof EntityCargoHold cargoHold) {
                for (int slotIndex = 0; slotIndex < cargoHold.itemStackHandler.getSlots(); slotIndex++) {
                    guiModuleItemHandlerSlot slot =
                            new guiModuleItemHandlerSlot(
                                    id,
                                    cargoHold.itemStackHandler,
                                    slotIndex,
                                    0,
                                    1,
                                    guiHandler,
                                    x * 18,
                                    y * 18
                            ) {
                                @Override
                                public void server_handleInventoryClick(Player player, int button, boolean isShift) {
                                    super.server_handleInventoryClick(player, button, isShift);
                                    onBlockEntityChanged(cargoHold.getBlockPos());
                                }
                            };
                    inventoriesContainer.modules.add(slot);
                    id++;
                    x++;
                    if (x > 9) {
                        x = 0;
                        y++;
                    }
                }
            }
        }
    }

    public void openGui() {
        if (level().isClientSide) {
            guiHandler.openGui(180, 220, true);
        }
    }

    /// / other rocket methods ////

    public void setStructureChanged(){
        cachedThrust = -1;
        cachedFuelRate = -1;
        cachedBlockMass = -1;
        cachedEnginePositions = null;
        cachedSeatPositions = null;
        requiresMeshUpdate = true;
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

    public float getBlockMass() {
        if (cachedBlockMass < 0) {
            cachedBlockMass = 0;
            for (BlockPos p : blocks.keySet()) {
                Block block = blocks.get(p).getBlock();
                float weightModulator = 1;
                if(block instanceof ICustomWeightBlock customWeightBlock)
                    weightModulator = customWeightBlock.getWeightMultiplier();
                cachedBlockMass += Config.INSTANCE.rocket_Block_Weight * weightModulator;
            }
        }
        return cachedBlockMass;
    }

    public void recalculateMass() {
        float mass = 0;
        mass += getBlockMass();
        mass += getFuel() * Config.INSTANCE.rocket_Fuel_Weight_Per_MB; // fuel weight
        for (BlockEntity e : blockEntities.values()) {
            if (e instanceof EntityCargoHold entityCargoHold) {
                ItemStack carried = entityCargoHold.itemStackHandler.getStackInSlot(0);
                if (!carried.isEmpty()) {
                    float relativeFill = (float) carried.getCount() / carried.getMaxStackSize();
                    mass += relativeFill * Config.INSTANCE.rocket_ItemStack_Weight;
                }
            }
        }
        if (mass != currentMass) {
            currentMass = mass;
            CompoundTag info = new CompoundTag();
            info.putFloat("currentMass", mass);
            sendToClients(info);
        }
    }

    public float getMass() {
        // prevent divide by 0 caused by mass = 0 if no blocks for some reason (for example blocks not yet synced)
        // very important or the game will freeze forever because it might get inf velocity vectors and tries to check inf blocks for collision
        return Math.max(0.00001f, currentMass);
    }

    public float getMaxAcceleration() {
        // this method usually runs when:
        // the controller ticks and a target position is given
        // when the rocket lands to calculate its target velocity
        // when there is no program running, this method should never run and the ground check should not be a concern
        Dimension myDim = DimensionManager.getDimensionManager(level().isClientSide).get(level().dimension().location());
        if (myDim instanceof SpaceStationDimension) {
            // on space station, lower acceleration for more fine controll
            // usually it should never demand this much but anyway....
            return 0.01f;
        }
        if (myDim instanceof PlanetDimension planet) {
            // lower acc near ground where there is probably more atmosphere and whatever it looks better
            int y = Utils.findGroundY(level(), blockPosition());
            double MAX_STRUCTURAL_ACC = 0.08 * 3;
            double h = position().y - y;
            double minH = 100;
            double minA = Math.min(MAX_STRUCTURAL_ACC, getGravity() * 1.05);
            double currentMaxA = minA + (MAX_STRUCTURAL_ACC - minA) * Math.min(1, h / minH);

            // next: limit by velocity, too fast = too much stress by atmosphere
            // if we go faster than target velocity, reduce acceleration
            double atmMultiplier = 1 - (planet.getAtmosphereDensity() / (1 + planet.getAtmosphereDensity()));
            double targetSpeedPerTick = 10 * atmMultiplier;
            double overspeedAllowance = 5;
            double currentSpeed = getDeltaMovement().y;
            // current ~ target -> 1
            // current >> target -> -inf (too fast, slow down)
            // current << target -> +inf (too slow or falling, no limit on acc)
            double accelerationModifier = 1 + (targetSpeedPerTick - currentSpeed) / overspeedAllowance;
            accelerationModifier = Math.clamp(accelerationModifier, 0, 1);

            return (float) (currentMaxA * accelerationModifier);
        }
        return 1;
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
}
