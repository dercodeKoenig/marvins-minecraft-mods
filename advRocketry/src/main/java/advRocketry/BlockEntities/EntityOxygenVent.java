package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.SimpleFluidContainer;
import advRocketry.Config;
import advRocketry.GlobalTime;
import advRocketry.LifeSupport.LifeSupportSupplier;
import advRocketry.LifeSupport.LifeSupportSystem;
import advRocketry.Registry.GasRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static advRocketry.Registry.BlockEntities.ENTITY_OXYGEN_VENT;

/**
 * An oxygen vent that sustains the life support system.
 * <p>
 * It holds a fluid tank that accepts only nitrogen or oxygen. It does not consume power.
 * It requires a redstone signal to run.
 * <p>
 * If the tank is filled with oxygen and the vent is powered, it acts as an {@link LifeSupportSystem.LifeSupportType#OXYGEN_SUPPLIER}
 * and consumes oxygen. If the tank is filled with nitrogen, it acts as a {@link LifeSupportSystem.LifeSupportType#PRESSURE_SUPPLIER}
 * and consumes nitrogen. While it has a redstone signal and enough of the matching fluid the life support system stays active;
 * once the redstone signal or the fluid runs out, the supplier goes inactive.
 * <p>
 * A neighboring active {@link EntityCo2Scrubber} cuts the vent's gas consumption by a configurable
 * amount (95% oxygen / 98% nitrogen by default); when the discounted rate drops below 1 mb/tick the
 * remainder is applied as a random chance to consume 1 mb so the average still matches the rate.
 */
public class EntityOxygenVent extends BlockEntity implements INetworkTagReceiver {

    public static final int TANK_CAPACITY = 10000;

    public FluidTank tank;
    public SimpleFluidContainer simpleFluidContainer;
    public ItemStackHandler inventory;
    public GuiHandlerBlockEntity guiHandler;

    /** oxygen supplier, registered with the life support system on load */
    LifeSupportSupplier oxygenSupplier;
    /** pressure supplier, registered with the life support system on load */
    LifeSupportSupplier pressureSupplier;

    /** whether the vent had a redstone signal during the last tick */
    public boolean hasRedstone = false;
    /** whether the oxygen supplier was active during the last tick */
    public boolean isOxygenActive = false;
    /** whether the pressure supplier was active during the last tick */
    public boolean isPressureActive = false;

    /** client side: how long to keep spawning smoke particles after the last packet */
    int clientParticleTimeout = 0;
    /** client side: how many smoke particles to still spawn from the last packet (the intensity) */
    int clientParticleCount = 0;
    /** server side: throttle particle packets to watching clients */
    long lastParticleSent = 0;

    public EntityOxygenVent(BlockPos pos, BlockState blockState) {
        super(ENTITY_OXYGEN_VENT.get(), pos, blockState);

        tank = new FluidTank(TANK_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return isValidVentFluid(stack);
            }

            @Override
            public void onContentsChanged() {
                setChanged();
            }
        };

        inventory = new ItemStackHandler(2) {
            @Override
            public void onContentsChanged(int slot) {
                setChanged();
            }
        };

        simpleFluidContainer = new SimpleFluidContainer(tank, inventory);

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.modules.addAll(simpleFluidContainer.makeGuiModules(0, 10, 10, guiHandler));

        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 100, 0, 1, guiHandler));
        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 75, 200, 0, 1, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityOxygenVent) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // register an oxygen supplier and a pressure supplier, the life support system will keep
        // both registered but only the one whose type matches the fluid inside (and that is powered)
        // will report itself as active
        oxygenSupplier = new LifeSupportSupplier(level, getBlockPos()) {
            @Override
            public LifeSupportSystem.LifeSupportType getType() {
                return LifeSupportSystem.LifeSupportType.OXYGEN_SUPPLIER;
            }

            @Override
            public boolean isActive() {
                return isOxygenActive;
            }
        };
        pressureSupplier = new LifeSupportSupplier(level, getBlockPos()) {
            @Override
            public LifeSupportSystem.LifeSupportType getType() {
                return LifeSupportSystem.LifeSupportType.PRESSURE_SUPPLIER;
            }

            @Override
            public boolean isActive() {
                return isPressureActive;
            }
        };
        LifeSupportSystem.registerLifeSupportSupplier(level, oxygenSupplier);
        LifeSupportSystem.registerLifeSupportSupplier(level, pressureSupplier);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LifeSupportSystem.removeLifeSupportSupplier(level, oxygenSupplier);
        LifeSupportSystem.removeLifeSupportSupplier(level, pressureSupplier);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
        // the server told us the vent is (or was) active, keep spawning smoke particles for a bit.
        // the carried int is the particle intensity (1 = scrubber-discounted, 4 = full billow)
        if (compoundTag.contains("vent_particles")) {
            clientParticleTimeout = 20;
            clientParticleCount = compoundTag.getInt("vent_particles");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        simpleFluidContainer.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        simpleFluidContainer.loadAdditional(tag, registries);
    }

    public void openGui() {
        if (level.isClientSide) {
            guiHandler.openGui(176, 166, true);
        }
    }

    /** the tank only accepts nitrogen or oxygen */
    boolean isValidVentFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        Fluid oxygen = GasRegistry.gases.get(GasRegistry.oxygen).fluid;
        Fluid nitrogen = GasRegistry.gases.get(GasRegistry.nitrogen).fluid;
        Fluid fluid = stack.getFluid();
        return fluid.equals(oxygen) || fluid.equals(nitrogen);
    }

    /** @return true if at least one of the 6 neighboring blocks is an active co2 scrubber */
    boolean hasNeighborActiveScrubber() {
        if (level == null || level.isClientSide) return false;
        for (Direction d : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(d));
            if (neighbor instanceof EntityCo2Scrubber scrubber && scrubber.isRunning) {
                return true;
            }
        }
        return false;
    }

    public void tick() {
        if (level.isClientSide) {
            if (clientParticleTimeout > 0) {
                clientParticleTimeout--;
                if (GlobalTime.getGlobalTime() % 3 == 0 && clientParticleCount > 0) {
                    spawnSmokeParticle();
                    clientParticleCount--;
                }
            }
            return;
        }

        guiHandler.serverTick();
        simpleFluidContainer.performPossibleFluidTransfer();

        // a redstone signal is required for the vent to run
        hasRedstone = level.hasNeighborSignal(getBlockPos());
        // whether a neighboring co2 scrubber is currently active (discounts gas usage + quiets the smoke)
        boolean scrubberActive = hasNeighborActiveScrubber();

        FluidStack fluid = tank.getFluid();
        Fluid oxygen = GasRegistry.gases.get(GasRegistry.oxygen).fluid;
        Fluid nitrogen = GasRegistry.gases.get(GasRegistry.nitrogen).fluid;

        // reset state from the previous tick, set again below if we end up consuming
        boolean activeThisTick = false;
        isOxygenActive = false;
        isPressureActive = false;

        if (hasRedstone && !fluid.isEmpty()) {
            boolean isOxygen = fluid.getFluid().equals(oxygen);
            boolean isNitrogen = fluid.getFluid().equals(nitrogen);
            if (isOxygen || isNitrogen) {
                int base = isOxygen ? Config.INSTANCE.oxygen_vent_Oxygen_per_tick : Config.INSTANCE.oxygen_vent_Nitrogen_per_tick;
                // a neighboring active co2 scrubber cuts the gas usage of whichever gas is being distributed
                // (scrubberActive was computed once above so it can also drive the smoke intensity)

                // work out how much fluid has to actually disappear this tick for the discounted / full rate.
                // the amount we require in the tank is exactly the amount we drain, so a player can not keep the
                // supplier active by slowly trickling 1 mb in: the full (undiscounted) rate still needs a full portion.
                int drain;
                double chance;
                if (scrubberActive) {
                    // apply the configurable discount per gas type (95% oxygen / 98% nitrogen by default)
                    float reduction = isOxygen ? Config.INSTANCE.co2_scrubber_oxygen_reduction : Config.INSTANCE.co2_scrubber_nitrogen_reduction;
                    double reduced = base * (1.0 - reduction);
                    if (reduced >= 1.0) {
                        drain = (int) Math.floor(reduced);
                        chance = 1.0;
                    } else {
                        // below 1 mb/tick: randomly consume 1 mb so the average matches the reduced rate
                        drain = 1;
                        chance = reduced;
                    }
                } else {
                    drain = base;
                    chance = 1.0;
                }

                // only mark the supplier as active when there is enough fluid to pay the drain this tick
                if (fluid.getAmount() >= drain) {
                    if (isOxygen) {
                        isOxygenActive = true;
                    } else {
                        isPressureActive = true;
                    }
                    activeThisTick = true;
                    if (level.random.nextDouble() < chance) {
                        tank.drain(drain, IFluidHandler.FluidAction.EXECUTE);
                        setChanged();
                    }
                }
                // not enough fluid: the supplier goes inactive
            }
            // any other fluid is also ignored
        }
        // no redstone: both suppliers go inactive

        // while active, spawn tiny smoke particles to watching clients (like the fluid release does).
        // the int carried in the packet is the particle intensity: a scrubber-discounted vent billows
        // far less smoke than one running at full gas consumption
        if (activeThisTick && GlobalTime.getGlobalTime() > lastParticleSent + 18) {
            CompoundTag info = new CompoundTag();
            info.putInt("vent_particles", scrubberActive ? 1 : 4);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
            lastParticleSent = GlobalTime.getGlobalTime();
        }
    }

    /** spawns a single tiny smoke particle on the client, similar to the fluid release.
     *  the gas vents out of a randomly chosen free (non-blocked) horizontal side and drifts slowly. */
    void spawnSmokeParticle() {
        Vec3 center = getBlockPos().getCenter().add(0, 0, 0);
        Direction[] sides = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

        Vec3 pos = center;
        Vec3 vel = Vec3.ZERO;
        // pick a random side that is actually free (air) so the smoke can escape
        for (int i = 0; i < sides.length; i++) {
            Direction side = sides[level.random.nextInt(sides.length)];
            BlockPos adj = getBlockPos().relative(side);
            if (!level.getBlockState(adj).isCollisionShapeFullBlock(level, adj)) {
                pos = center.add(side.getStepX() * 0.48, 0, side.getStepZ() * 0.48);
                // slow outward drift, barely rising
                vel = new Vec3(side.getStepX() * 0.015, (Math.random() - 0.2) * 0.015, side.getStepZ() * 0.015);
                break;
            }
        }

        level.addParticle(
                ParticleTypes.CLOUD,
                true,
                pos.x + (Math.random() - 0.5) * 0.05,
                pos.y,
                pos.z + (Math.random() - 0.5) * 0.05,
                vel.x,
                vel.y,
                vel.z
        );
    }
}
