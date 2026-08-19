package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Config;
import advRocketry.GlobalTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.BlockEntities.ENTITY_CO2_SCRUBBER;

/**
 * A co2 scrubber that reduces the gas consumption of a neighboring oxygen vent while it is running.
 * The discount is 95% for oxygen and 98% for nitrogen by default (both configurable).
 * It consumes energy each tick it stays active and only switches on when a neighboring oxygen vent
 * is actively distributing a gas (oxygen or nitrogen) to the life support system.
 * <p>
 * It does not handle fluids itself: it is powered through its {@link BlockEntityBattery} which
 * can be filled by neighboring energy providers. The active state is reflected in the block's
 * {@link BlockStateProperties#LIT} property (the {@code scrubber_on} model).
 */
public class EntityCo2Scrubber extends BlockEntity implements INetworkTagReceiver {

    /**
     * 5s (100 ticks) the scrubber must stay off after shutting down so it does not flicker on/off when low on power
     */
    private static final long OFFLINE_COOLDOWN_TICKS = 100;
    public BlockEntityBattery battery;
    public GuiHandlerBlockEntity guiHandler;
    public guiModuleButton toggleButton;
    /**
     * player toggled on/off through the gui button
     */
    public boolean isEnabled = true;
    /**
     * whether the scrubber is currently running, i.e. it is enabled, a neighboring oxygen vent is
     * actively distributing a gas and it has enough energy to pay the running cost this tick.
     * read by neighboring oxygen vents to apply the gas consumption cut
     */
    public boolean isRunning = false;
    /**
     * global-tick time the scrubber last stopped running; used only for the local flicker cooldown (not persisted)
     */
    public long last_went_out_of_power = 0;

    public EntityCo2Scrubber(BlockPos pos, BlockState blockState) {
        super(ENTITY_CO2_SCRUBBER.get(), pos, blockState);

        battery = new BlockEntityBattery(this, 10000);

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.modules.add(new guiModuleEnergy(0, battery, guiHandler, 10, 10));

        // the toggle button enables/disable the scrubber's auto-run behavior
        toggleButton = new guiModuleButton(1, "text", guiHandler, 40, 12, 40, 15, BTN_GREEN, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.put("toggleEnabled", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityCo2Scrubber.this, info));
            }
        };
        guiHandler.modules.add(toggleButton);

        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 100, 0, 1, guiHandler));
        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 200, 0, 1, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityCo2Scrubber) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("toggleEnabled")) {
            isEnabled = !isEnabled;
            setChanged();
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("battery", battery.serializeNBT(registries));
        tag.putBoolean("isEnabled", isEnabled);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("battery")) {
            battery.deserializeNBT(registries, tag.get("battery"));
        }
        isEnabled = tag.getBoolean("isEnabled");
    }

    public void openGui() {
        if (level.isClientSide) {
            guiHandler.openGui(176, 166, true);
        }
    }

    /**
     * @return true when at least one of the 6 neighboring blocks is an oxygen vent that is
     * currently distributing a gas (oxygen or nitrogen) to the life support system
     */
    boolean hasNeighborActiveVent() {
        if (level == null || level.isClientSide) return false;
        for (Direction d : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(d));
            if (neighbor instanceof EntityOxygenVent vent && (vent.isOxygenActive || vent.isPressureActive)) {
                return true;
            }
        }
        return false;
    }

    public void tick() {
        if (level.isClientSide) {
            return;
        }

        guiHandler.serverTick();

        long now = GlobalTime.getGlobalTime();
        int energyCost = Config.INSTANCE.co2_scrubber_energy_per_tick;

        // the scrubber runs only when all of these hold: enabled by the button, a neighbor is distributing a gas,
        // the offline flicker cooldown has elapsed and there is enough energy to pay this tick
        boolean shouldRun = isEnabled
                && hasNeighborActiveVent()
                && now > last_went_out_of_power + OFFLINE_COOLDOWN_TICKS
                && battery.getEnergyStored() >= energyCost;


        // when running and it goes out of energy, force a cooldown until it goes active again
        // so it does not flicker the block state while starved of power,
        if (battery.getEnergyStored() < energyCost && isRunning) {
            last_went_out_of_power = now;
        }

        if (shouldRun) {
            battery.extractEnergy(energyCost, false);
            setChanged();
        }
        isRunning = shouldRun;

        // reflect the running state in the blockstate (lit = scrubber_on texture)
        BlockState state = getBlockState();
        boolean currentlyLit = state.getValue(BlockStateProperties.LIT);
        if (currentlyLit != isRunning) {
            level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.LIT, isRunning), 3);
        }

        // keep the toggle button appearance in sync with the enabled flag for watching clients
        if (!guiHandler.playersTrackingGui.isEmpty()) {
            if (isEnabled) {
                toggleButton.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                toggleButton.setTextAndSync("ON");
            } else {
                toggleButton.setBackgroundAndSync(BTN_BLACK, BTN_W, BTN_H);
                toggleButton.setTextAndSync("OFF");
            }
        }
    }
}
