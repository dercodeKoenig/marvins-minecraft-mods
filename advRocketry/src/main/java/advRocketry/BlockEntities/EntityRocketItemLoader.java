package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Config;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Blocks.RocketItemLoader.IS_DRAIN;
import static advRocketry.Registry.BlockEntities.ENTITY_ROCKET_ITEM_LOADER;

public class EntityRocketItemLoader extends EntityRocketInfrastructureBase implements INetworkTagReceiver {

    public BlockEntityBattery battery;
    public ItemStackHandler inventory;

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleButton drainFillToggleButton;

    public boolean shouldOutputSignal = false;


    public EntityRocketItemLoader(BlockPos pos, BlockState blockState) {
        super(ENTITY_ROCKET_ITEM_LOADER.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);

        inventory = new ItemStackHandler(4) {
            public void onContentsChanged(int slot) {
                setChanged();
            }
        };
        int containerGroup = 0;
        int playerInventoryGroup = 1;
        guiHandler.getModules().add(new guiModuleItemHandlerSlot(0, inventory, 0, containerGroup, playerInventoryGroup, guiHandler, 10, 10));
        guiHandler.getModules().add(new guiModuleItemHandlerSlot(1, inventory, 1, containerGroup, playerInventoryGroup, guiHandler, 10, 30));
        guiHandler.getModules().add(new guiModuleItemHandlerSlot(2, inventory, 2, containerGroup, playerInventoryGroup, guiHandler, 30, 10));
        guiHandler.getModules().add(new guiModuleItemHandlerSlot(3, inventory, 3, containerGroup, playerInventoryGroup, guiHandler, 30, 30));


        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 125, 100, playerInventoryGroup, containerGroup, guiHandler));
        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 65, 200, playerInventoryGroup, containerGroup, guiHandler));

        battery = new BlockEntityBattery(this, 10000);
        guiHandler.modules.add(new guiModuleEnergy(11000, battery, guiHandler, 155, 7));

        drainFillToggleButton = new guiModuleButton(11001, "text", guiHandler, 70, 10, 40, 15, BTN_GREEN, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.put("toggleDrainFill", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityRocketItemLoader.this, info));
            }
        };

        guiHandler.modules.add(drainFillToggleButton);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityRocketItemLoader) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("toggleDrainFill")) {
            level.setBlock(getBlockPos(), getBlockState().setValue(IS_DRAIN, !getBlockState().getValue(IS_DRAIN)), 3);
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
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        battery.deserializeNBT(registries, tag.get("battery"));
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); ++i) {
                Block.popResource(level, getBlockPos(), inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    /// tries to load 1 item into the rocket
    /// returns 1 if success, 0 if there are no items to load, -1 if no item could be loaded because rocket is full
    public int loadOneItem(EntityRocket linkedRocket) {
        boolean isEmpty = true;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack canExtract = inventory.extractItem(i, 1, true);
            if (!canExtract.isEmpty()) {
                isEmpty = false;
                for (BlockEntity be : linkedRocket.blockEntities.values()) {
                    if (be instanceof EntityCargoHold cargoHold) {
                        for (int j = 0; j < cargoHold.itemStackHandler.getSlots(); j++) {
                            ItemStack notInserted = cargoHold.itemStackHandler.insertItem(j, canExtract, true);
                            if (notInserted.isEmpty()) {
                                // commit the transaction
                                ItemStack extracted = inventory.extractItem(i, 1, false);
                                cargoHold.itemStackHandler.insertItem(j, extracted, false);
                                return 1;
                            }
                        }
                    }
                }
            }
        }
        if (isEmpty)
            return 0;
        else
            return -1;
    }


    /// tries to load 1 item from the rocket into the internal inventory
    /// returns: 1 if success, 0 if rocket has no items, -1 if rocket has items but we could not unload them
    public int unLoadOneItem(EntityRocket linkedRocket) {
        boolean isEmpty = true;
        for (BlockEntity be : linkedRocket.blockEntities.values()) {
            if (be instanceof EntityCargoHold cargoHold) {
                for (int j = 0; j < cargoHold.itemStackHandler.getSlots(); j++) {
                    ItemStack canExtract = cargoHold.itemStackHandler.extractItem(j, 1, true);
                    if (!canExtract.isEmpty()) {
                        isEmpty = false;
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            ItemStack notInserted = inventory.insertItem(i, canExtract, true);
                            if (notInserted.isEmpty()) {
                                // commit the transaction
                                ItemStack extracted = cargoHold.itemStackHandler.extractItem(j, 1, false);
                                inventory.insertItem(i, extracted, false);
                                return 1;
                            }
                        }
                    }
                }
            }
        }
        if (isEmpty)
            return 0;
        else
            return -1;
    }

    public void setOutputSignal(boolean signal) {
        if (signal != shouldOutputSignal) {
            shouldOutputSignal = signal;
            setChanged();
        }
    }

    public void tick() {

        if (!level.isClientSide) {
            guiHandler.serverTick();
            super.serverTick();

            boolean isDrain = getBlockState().getValue(IS_DRAIN);

            if (linkedRocket != null) {
                if (linkedRocket.getCurrentProgram() == null) {
                    if (battery.getEnergyStored() >= Config.INSTANCE.item_Loader_Energy_Per_Tick) {
                        if (!isDrain) {
                            // FILL the rocket
                            int res = loadOneItem(linkedRocket);
                            // -1 = rocket full
                            // 0 = no item loaded ( item loader empty )
                            // 1 = item was loaded
                            if (res != -1) {
                                setOutputSignal(false);
                                if (res == 1) // load success
                                    battery.extractInternal(Config.INSTANCE.item_Loader_Energy_Per_Tick, false);
                            } else {
                                // item loading failed because rocket is full
                                setOutputSignal(true);
                            }
                        } else {
                            // DRAIN the rocket
                            int res = unLoadOneItem(linkedRocket);
                            // -1 = item unload fail ( inventory full )
                            // 1 = item unloaded
                            // 0 = rocket empty
                            if (res != 0) {
                                setOutputSignal(false);
                                if (res == 1) // unload success
                                    battery.extractInternal(Config.INSTANCE.item_Loader_Energy_Per_Tick, false);
                            } else {
                                // rocket is empty
                                setOutputSignal(true);
                            }
                        }
                    }
                }
            }

            if (isDrain) {
                drainFillToggleButton.setBackgroundAndSync(BTN_BLACK, BTN_W, BTN_H);
                drainFillToggleButton.setTextAndSync("UNLOAD");
            } else {
                drainFillToggleButton.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                drainFillToggleButton.setTextAndSync("LOAD");
            }
        }
    }
}
