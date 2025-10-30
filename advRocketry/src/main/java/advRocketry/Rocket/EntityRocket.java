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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class EntityRocket extends Entity implements INetworkTagReceiver {

    public Map<BlockPos, BlockState> blocks;
    public Map<BlockPos, BlockEntity> blockEntities;
    public Vec3i size;
    public Vec3 heading = new Vec3(0, 1, 0);
    public Vec3 front = new Vec3(0, 0, 1);
    public Vec3 targetHeading = new Vec3(0, 1, 0);
    public ItemStack usedNavigationItem = ItemStack.EMPTY; // the current one used (guidance computer item can be overwritten in launch terminal)
    public FluidTank fuelTank = null;
    private float cachedThrust = -1;

    public boolean canUseMainEngines= true;
    public boolean canUseSecondaryEngines= true;
    public Vec3 targetPosition = new Vec3(0,0,0);
    public Vec3 defaultTargetHeading = new Vec3(0,1,0);

    public GuiHandlerEntity guiHandler;

    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);
        guiHandler = new GuiHandlerEntity(this);
        blocks = new HashMap<>();
        blockEntities = new HashMap<>();
        size = new Vec3i(1, 1, 1);
        fuelTank = new FluidTank(0);
    }

    public static EntityRocket create(Level level, Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> blockEntities, Vec3i size) {
        EntityRocket rocket = new EntityRocket(Registry.ENTITY_ROCKET.get(), level);
        rocket.blockEntities = blockEntities;
        rocket.blocks = blocks;
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
    public AABB makeBoundingBox() {
        if (size == null)
            return super.makeBoundingBox(); // happens because minecraft calls makeBoundingBox in constructor before the size value is assigned
        return new AABB(
                position().x - (double) size.getX() / 2,
                position().y,
                position().z - (double) size.getZ() / 2,
                position().x + (double) size.getX() / 2,
                position().y + size.getY(),
                position().z + (double) size.getZ() / 2
        );
    }

    @Override
    public double getDefaultGravity() {
        if(true)return 0;
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

    public float getThrust() {
        if (cachedThrust >= 0) return cachedThrust;
        cachedThrust = 0;
        for (BlockState state : blocks.values()) {
            if (state.getBlock() instanceof RocketMotor motor) {
                cachedThrust += motor.getThrust();
            }
        }
        return cachedThrust;
    }

    public int getFuel() {
        return fuelTank.getFluidAmount();
    }

    public float getMass() {
        return 1f * blocks.size();
    }

    public float getMaxAcceleration() {
        return 3f/20;
    }

    // client and server use this, server only syncs target heading
    public void tickHeading() {
        // Rotation Speed: How quickly the rocket can turn its heading towards the target acceleration vector.
        final double ROTATION_RATE = 0.05 / size.getY() * getThrust() / getMass();
        // Slowly interpolate the rocket's current 'heading' vector towards the 'targetHeading'.
        // This simulates the actual rotational speed limit of the rocket.
        Vec3 rotationCorrection = targetHeading.subtract(heading).scale(ROTATION_RATE);
        heading = heading.add(rotationCorrection).normalize();
    }

    public void setTargetHeading(Vec3 target) {
        if(!level().isClientSide && targetHeading.subtract(target).length() > 0.0001) {
            CompoundTag headingUpdate = new CompoundTag();
            headingUpdate.putFloat("headingX", (float) target.x);
            headingUpdate.putFloat("headingY", (float) target.y);
            headingUpdate.putFloat("headingZ", (float) target.z);
            PacketDistributor.sendToPlayersTrackingEntity(this, PacketEntity.getEntityPacket(this, headingUpdate));
        }
        targetHeading = target;
    }

    // pd controller mostly written by gemini should be used to have the rocket spawn at some offset and find its way down to the landing area
    // it should also scan (if no launchpad structure) to land at some area where there is a flat area
    public void tickController(){
        // --- Configuration Parameters (Tune these for desired behavior) ---
        // Proportional Gain: How aggressively the rocket tries to close the distance.
        final double K_P = 0.01 / size.getY() * getThrust() / getMass();
        // Damping Gain (Derivative-like): How aggressively the rocket slows down to prevent overshoot.
        final double K_D = 0.4;
        // Structural/Breakage Limit: This is the maximum acceleration the vehicle can withstand.
        final double MAX_STRUCTURAL_ACCEL = getMaxAcceleration();
        // secondary thruster force
        final double SECONDARY_THRUSTERS_FORCE = getThrust() / 1000;

        // --- 1. Calculate Required Acceleration (The PD Controller) ---

        // B. Position Error (p_target - p)
        Vec3 positionError = targetPosition.subtract(getPosition(0));

        // C. Damping (Velocity Error - using current velocity for simplicity)
        Vec3 currentVelocity = getDeltaMovement();

        // D. Desired Acceleration (a_desired)
        // Formula: a_desired = (K_P * Position_Error) - (K_D * Current_Velocity)
        // The result is the absolute acceleration vector the rocket *needs* to follow the path.
        Vec3 desiredAcceleration = positionError.scale(K_P).subtract(currentVelocity.scale(K_D));

        // NOTE: If you needed to factor in gravity/other external forces, you would
        // add an opposing vector here: desiredAcceleration = ... .add(Vec3.GRAVITY.scale(-1));
        desiredAcceleration = desiredAcceleration.add(new Vec3(0, 1, 0).scale(getGravity()));

        // --- 2. Calculate Thrust & Heading ---

        if (canUseSecondaryEngines) {
            // use secondary thrusters in space for fine controll
            Vec3 secondaryThrustersForce = desiredAcceleration.scale(getMass());
            if (secondaryThrustersForce.length() > SECONDARY_THRUSTERS_FORCE) {
                secondaryThrustersForce = secondaryThrustersForce.normalize().scale(SECONDARY_THRUSTERS_FORCE);
            }
            // TODO: render secondaryThrustersForce particles
            Vec3 secondaryThrustersAcceleration = secondaryThrustersForce.scale(1 / getMass());
            desiredAcceleration.subtract(secondaryThrustersAcceleration);
            setDeltaMovement(getDeltaMovement().add(secondaryThrustersAcceleration));
        }

        if (desiredAcceleration.length() > 0.0001 && canUseMainEngines) {
            // 1. Max Acceleration the engine can *possibly* deliver.
            final double MAX_PHYSICAL_ACCEL = getThrust() / getMass();
            // 2. The absolute maximum acceleration we are allowed to use this frame.
            // This ensures we never break the rocket (MAX_STRUCTURAL_ACCEL) AND never demand more thrust than the engine can provide (MAX_PHYSICAL_ACCEL).
            final double MAX_ALLOWED_ACCEL = Math.min(MAX_PHYSICAL_ACCEL, MAX_STRUCTURAL_ACCEL);
            // The heading the rocket *needs* to point towards to achieve the desired acceleration.
            Vec3 targetHeading = desiredAcceleration.normalize();
            setTargetHeading(targetHeading);
            // Calculate the magnitude of acceleration needed from the PD controller.
            double neededAcceleration = desiredAcceleration.length();
            // Cap the needed acceleration by the final allowed limit.
            // We only need to use the MAX_ALLOWED_ACCEL cap here.
            double effectiveAcceleration = Math.min(neededAcceleration, MAX_ALLOWED_ACCEL);
            // The component of the effective acceleration that aligns with the current (limited) heading.
            // This ensures we only thrust in the direction we are currently pointing.
            double actualThrustAccel = effectiveAcceleration * Math.max(0, heading.dot(targetHeading) * 3 - 2);
            // Thrust is applied along the current 'heading' direction.
            // We use the 'actualThrustAccel' determined by the PD control and the rotation limit.
            Vec3 thrustVector = heading.scale(actualThrustAccel);
            setDeltaMovement(getDeltaMovement().add(thrustVector));
            // Calculate the Thrust Multiplier (0.0 to 1.0)
            // This is the fraction of MAX_THRUST that is needed to achieve the 'effectiveAcceleration'.
            // Thrust_Multiplier = (Effective_Accel * Mass) / Max_Thrust
            double ThrustMultiplier = (actualThrustAccel * getMass()) / getThrust();
            // TODO: render rocket flame & smoke particles
            // TODO: burn fuel
        }else{
            setTargetHeading(defaultTargetHeading);
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            guiHandler.serverTick();
        }
        if(!level().isClientSide){
            tickController();
        }
        tickHeading();

        targetPosition = new Vec3(50,70,0);
        canUseMainEngines = false;
        canUseSecondaryEngines = true;

        if (!level().isClientSide) {

            // Ensure you apply gravity
            applyGravity();

            // simulate some air friction
            float atmDensity = 1;
            Dimension myDimension = DimensionManager.get(level().dimension().location());
            if (myDimension != null)
                atmDensity = myDimension.getAtmosphereDensity();
            Vec3 airBreak = getDeltaMovement().normalize().scale(-1 * atmDensity * size.getY() * getDeltaMovement().length() * 0.01 / getMass());
            setDeltaMovement(getDeltaMovement().add(airBreak));
            //System.out.println(airBreak.length()+":"+getDeltaMovement().length());
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
        moveTo(getX(), getY() + 10000, getZ()); // entry height
        setDeltaMovement(0, -100, 0); // entry speed
    }

    public void deconstruct() {
        BlockPos minPos = blockPosition().subtract(new Vec3i(size.getX() / 2, 0, size.getZ() / 2));
        for (BlockPos pos : blocks.keySet()) {
            BlockState state = blocks.get(pos);
            BlockPos target = new BlockPos(
                    minPos.getX() + pos.getX(),
                    minPos.getY() + pos.getY(),
                    minPos.getZ() + pos.getZ()
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
        CompoundTag sizeTag = compoundTag.getCompound("size");
        size = new Vec3i(sizeTag.getInt("x"), sizeTag.getInt("y"), sizeTag.getInt("z"));

        fuelTank.readFromNBT(level().registryAccess(), compoundTag.getCompound("fuelTank"));

        blocks = new HashMap<>();
        ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockTags.size(); i++) {
            CompoundTag blockTag = blockTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
            blocks.put(p, state);
        }

        blockEntities = new HashMap<>();
        ListTag blockEntityTags = compoundTag.getList("blockEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntityTags.size(); i++) {
            CompoundTag blockTag = blockEntityTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = blocks.get(p);
            BlockEntity be = ((EntityBlock) state.getBlock()).newBlockEntity(p, state);
            be.loadCustomOnly(blockTag.getCompound("blockEntity"), registryAccess());
            blockEntities.put(p, be);
        }

        makeGui();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        CompoundTag sizeTag = new CompoundTag();
        sizeTag.putInt("x", size.getX());
        sizeTag.putInt("y", size.getY());
        sizeTag.putInt("z", size.getZ());
        compoundTag.put("size", sizeTag);

        compoundTag.put("fuelTank", fuelTank.writeToNBT(level().registryAccess(), new CompoundTag()));

        ListTag blockTags = new ListTag(blocks.size());
        for (BlockPos i : blocks.keySet()) {
            BlockState state = blocks.get(i);
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockTag.put("block", NbtUtils.writeBlockState(state));
            blockTags.add(blockTag);
        }
        compoundTag.put("blocks", blockTags);

        ListTag blockEntityTags = new ListTag(blockEntities.size());
        for (BlockPos i : blockEntities.keySet()) {
            BlockEntity blockEntity = blockEntities.get(i);
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockEntityTag.put("blockEntity", blockEntity.saveCustomOnly(registryAccess()));
            blockEntityTags.add(blockEntityTag);
        }
        compoundTag.put("blockEntities", blockEntityTags);

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
        if (compoundTag.contains("additionalSaveData"))
            readAdditionalSaveData(compoundTag.getCompound("additionalSaveData"));

        if (compoundTag.contains("headingX") && compoundTag.contains("headingY") && compoundTag.contains("headingZ"))
            setTargetHeading(new Vec3(compoundTag.getFloat("headingX"), compoundTag.getFloat("headingY"), compoundTag.getFloat("headingZ")));

    }
}
