package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Blocks.SatelliteMonitor;
import advRocketry.Data.DataStack;
import advRocketry.Data.DataStorage;
import advRocketry.Data.SimpleDataContainer;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemDataStorage;
import advRocketry.Items.ItemSatellite;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Main;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteDataCollectorBase;
import advRocketry.Satellites.SatelliteManager;
import advRocketry.Satellites.SatelliteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.BlockEntities.ENTITY_SATELLITE_MONITOR;

public class EntitySatelliteMonitor extends BlockEntity implements INetworkTagReceiver {

    public ItemStackHandler inventory;
    public ItemStackHandler dataChipInventory;
    public DataStorage dataStorage;
    public GuiHandlerBlockEntity guiHandler;
    public SimpleDataContainer simpleDataContainer;
    public guiModuleVerticalProgressBar dataBar;
    public guiModuleText statusText;
    public guiModuleButton collectDataBtn;
    public BlockEntityBattery battery;
    boolean shouldCollectData = false;
    int btn_collect_data = 1008582;
    long lastTimeDataReceived = 0;

    public EntitySatelliteMonitor(BlockPos pos, BlockState blockState) {
        super(ENTITY_SATELLITE_MONITOR.get(), pos, blockState);

        dataStorage = new DataStorage(1000) {
            @Override
            public void onChange() {
                setChanged();
            }
        };

        dataChipInventory = new ItemStackHandler(2) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ItemDataStorage;
            }
        };
        simpleDataContainer = new SimpleDataContainer(dataStorage, dataChipInventory);

        inventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ItemSatelliteIdChip;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };

        battery = new BlockEntityBattery(this, 1000);

        makeGui();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySatelliteMonitor) t).tick();
    }

    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);
        int id = 0;
        guiHandler.modules.add(new guiModuleText(id++, "Satellite Monitor", guiHandler, 5, 5, 0xff000000, false));

        // id chip slot
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, 0, 0, 1, guiHandler, 60, 20));
        ResourceLocation id_chip = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/item/satellite_id_chip.png");
        guiHandler.modules.add(new guiModuleImage(guiHandler, 60, 40, 16, 16, id_chip, 12, 16));

        // data bar & inventory
        dataBar = new guiModuleVerticalProgressBar(id++, guiHandler, 10, 20);
        dataBar.progress = 0;
        dataBar.bar = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_vertical_progress_bar_green.png");
        guiHandler.modules.add(dataBar);
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, dataChipInventory, 0, 0, 1, guiHandler, 30, 20));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, dataChipInventory, 1, 0, 1, guiHandler, 30, 55));
        ResourceLocation arrow = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/arrow_down.png");
        guiHandler.modules.add(new guiModuleImage(guiHandler, 30, 38, 16, 16, arrow, 12, 16));

        statusText = new guiModuleText(id++, "", guiHandler, 90, 20, 0xff000000, false);
        guiHandler.modules.add(statusText);

        collectDataBtn = new guiModuleButton(btn_collect_data, "download data", guiHandler, 90, 55, 90, 20, BTN_RED, BTN_W, BTN_H);
        guiHandler.modules.add(collectDataBtn);

        guiModuleEnergy energyBar = new guiModuleEnergy(id++, battery, guiHandler, 190, 20);
        guiHandler.modules.add(energyBar);

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(27, 150, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(27, 90, 2000, 1, 0, guiHandler));
    }

    public void openGui() {
        guiHandler.openGui(216, 178, true);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            if (btn == btn_collect_data) {
                shouldCollectData = !shouldCollectData;
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("dataChipInventory", dataChipInventory.serializeNBT(registries));
        tag.putBoolean("shouldCollectData", shouldCollectData);
        tag.put("battery", battery.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        dataChipInventory.deserializeNBT(registries, tag.getCompound("dataChipInventory"));
        shouldCollectData = tag.getBoolean("shouldCollectData");
        battery.deserializeNBT(registries, tag.get("battery"));
    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); ++i) {
                Block.popResource(level, getBlockPos(), inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < dataChipInventory.getSlots(); ++i) {
                Block.popResource(level, getBlockPos(), dataChipInventory.getStackInSlot(i));
                dataChipInventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
            simpleDataContainer.performPossibleDataTransfer();

            Satellite connectedSatellite = null;
            boolean isInRange = false;
            ItemStack satelliteChip = inventory.getStackInSlot(0);
            if (satelliteChip.getItem() instanceof ItemSatelliteIdChip) {
                UUID uuid = ItemSatelliteIdChip.getTarget(satelliteChip);
                if (uuid != null) {
                    connectedSatellite = SatelliteManager.getSatellite(uuid);
                }
            }
            if (connectedSatellite != null) {
                Dimension orbitedDim = DimensionManager.INSTANCE_SERVER.get(connectedSatellite.parentDimensionId);
                Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
                if (myDim != null && orbitedDim != null) {
                    double distanceAU = myDim.getPosition(0).distanceTo(orbitedDim.getPosition(0));
                    isInRange = distanceAU < 0.3;
                }
            }

            if (shouldCollectData && connectedSatellite instanceof SatelliteDataCollectorBase dataCollector && battery.getEnergyStored() >= 10) {
                DataStack canExtract = dataCollector.extractOneDataUnit(true);
                int canInsert = dataStorage.insertData(canExtract, true);
                if (canInsert > 0) {
                    dataStorage.insertData(dataCollector.extractOneDataUnit(false), false);
                    lastTimeDataReceived = GlobalTime.getGlobalTime();
                    battery.extractEnergy(10, false);
                }
            }

            if (connectedSatellite == null && getBlockState().getValue(SatelliteMonitor.STATE) != SatelliteMonitor.State.idle) {
                level.setBlock(getBlockPos(), getBlockState().setValue(SatelliteMonitor.STATE, SatelliteMonitor.State.idle), 3);
            }
            boolean hasRecentlyReceivedData = lastTimeDataReceived + 20 * 10 > GlobalTime.getGlobalTime();
            if (connectedSatellite != null && hasRecentlyReceivedData && shouldCollectData && getBlockState().getValue(SatelliteMonitor.STATE) != SatelliteMonitor.State.active) {
                level.setBlock(getBlockPos(), getBlockState().setValue(SatelliteMonitor.STATE, SatelliteMonitor.State.active), 3);
            }
            if (connectedSatellite != null && (!hasRecentlyReceivedData || !shouldCollectData) && getBlockState().getValue(SatelliteMonitor.STATE) != SatelliteMonitor.State.satellite_connected) {
                level.setBlock(getBlockPos(), getBlockState().setValue(SatelliteMonitor.STATE, SatelliteMonitor.State.satellite_connected), 3);
            }

            // gui updates only when players are watching gui
            if (!guiHandler.playersTrackingGui.isEmpty()) {
                double data = 0;
                double maxData = dataStorage.getDataCapacity();
                String dataType = "data";
                DataStack dataStack = dataStorage.getDataStack();
                if (dataStack != null) {
                    data = dataStack.amount;
                    dataType = dataStack.type;
                }
                dataBar.setProgressAndSync(data / maxData);
                dataBar.setHoverInfoAndSync(dataType + ": " + data + " / " + maxData);

                String newStatusText = "";
                if (connectedSatellite == null) {
                    newStatusText = "satellite not found";
                } else if (!isInRange) {
                    newStatusText = connectedSatellite.getName() + "\n";
                    newStatusText += "out of range";
                } else {
                    newStatusText = connectedSatellite.getName() + "\n";
                    newStatusText += "rf: " + connectedSatellite.getEnergyStored() + "\n";
                    if (connectedSatellite instanceof SatelliteDataCollectorBase dataCollector) {
                        newStatusText += "data: " + dataCollector.getDataStored() + " / " + dataCollector.getDataCapacity();
                    }
                }
                statusText.setTextAndSync(newStatusText);

                if (shouldCollectData)
                    collectDataBtn.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                else
                    collectDataBtn.setBackgroundAndSync(BTN_RED, BTN_W, BTN_H);
            }
        }
    }
}
