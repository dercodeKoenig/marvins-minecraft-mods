package advRocketry.BlockEntities;

import ARLib.blockentities.EntityItemInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Config;
import advRocketry.Items.ItemLinker;
import advRocketry.Registry;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Blocks.RocketItemLoader.IS_DRAIN;
import static advRocketry.Registry.ENTITY_ROCKET_ITEM_LOADER;

public class EntityRocketItemLoader extends EntityItemInputBlock implements ItemLinker.linkable, ItemLinker.linkableToEntity {

    public static float maxDistance = 30;

    public BlockPos linkedAssemblerPos = null;
    public EntityRocket linkedRocket = null;

    public BlockEntityBattery battery;

    public guiModuleButton drainFillToggleButton;

    public boolean shouldOutputSignal = false;


    public EntityRocketItemLoader(BlockPos pos, BlockState blockState) {
        super(ENTITY_ROCKET_ITEM_LOADER.get(), pos, blockState);

        this.guiHandler = new GuiHandlerBlockEntity(this);
        int containergroup = 0;
        int playerinventorygroup = 1;
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(0, this, 0, containergroup, playerinventorygroup, this.guiHandler, 10, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(1, this, 1, containergroup, playerinventorygroup, this.guiHandler, 10, 30));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(2, this, 2, containergroup, playerinventorygroup, this.guiHandler, 30, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(3, this, 3, containergroup, playerinventorygroup, this.guiHandler, 30, 30));

        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 125, 100, playerinventorygroup, containergroup, this.guiHandler)) {
            this.guiHandler.getModules().add(i);
        }

        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 65, 200, playerinventorygroup, containergroup, this.guiHandler)) {
            this.guiHandler.getModules().add(i);
        }

        this.inventory = new ItemStackHandler(4) {
            public void onContentsChanged(int slot) {
                EntityRocketItemLoader.this.setChanged();
            }
        };

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
    public void signalOpenGui(ServerPlayer player) {
        this.guiHandler.signalOpenGui(player, 176, 150, true);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        super.readServer(compoundTag, serverPlayer);
        if (compoundTag.contains("toggleDrainFill")) {
            level.setBlock(getBlockPos(), getBlockState().setValue(IS_DRAIN, !getBlockState().getValue(IS_DRAIN)), 3);
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        super.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedAssemblerPos != null) tag.put("linkedAssemblerPos", NbtUtils.writeBlockPos(linkedAssemblerPos));
        tag.putInt("energy", battery.getEnergyStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("linkedAssemblerPos"))
            linkedAssemblerPos = NbtUtils.readBlockPos(tag, "linkedAssemblerPos").get();
        battery.setEnergy(tag.getInt("energy"));
    }

    public boolean loadOneItem(EntityRocket linkedRocket) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack canExtract = inventory.extractItem(i, 1, true);
            if (!canExtract.isEmpty()) {
                for (BlockEntity be : linkedRocket.blockEntities.values()) {
                    if (be instanceof EntityCargoHold cargoHold) {
                        for (int j = 0; j < cargoHold.itemStackHandler.getSlots(); j++) {
                            ItemStack notInserted = cargoHold.itemStackHandler.insertItem(j, canExtract, true);
                            if (notInserted.isEmpty()) {
                                // commit the transaction
                                ItemStack extracted = inventory.extractItem(i, 1, false);
                                cargoHold.itemStackHandler.insertItem(j, extracted, false);
                                linkedRocket.onBlockEntityChanged(cargoHold.getBlockPos());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean unLoadOneItem(EntityRocket linkedRocket) {
        for (BlockEntity be : linkedRocket.blockEntities.values()) {
            if (be instanceof EntityCargoHold cargoHold) {
                for (int j = 0; j < cargoHold.itemStackHandler.getSlots(); j++) {
                    ItemStack canExtract = cargoHold.itemStackHandler.extractItem(j, 1, true);
                    if (!canExtract.isEmpty()) {
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            ItemStack notInserted = inventory.insertItem(i, canExtract, true);
                            if (notInserted.isEmpty()) {
                                // commit the transaction
                                ItemStack extracted = cargoHold.itemStackHandler.extractItem(j, 1, false);
                                inventory.insertItem(i, extracted, false);
                                linkedRocket.onBlockEntityChanged(cargoHold.getBlockPos());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void setOutputSignal(boolean signal) {
        if (signal != shouldOutputSignal) {
            shouldOutputSignal = signal;
            setChanged();
        }
    }

    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            boolean isDrain = getBlockState().getValue(IS_DRAIN);

            if (linkedAssemblerPos != null) {
                linkedRocket = null;
                BlockEntity be = level.getBlockEntity(linkedAssemblerPos);
                if (be instanceof EntityRocketAssembler assembler) {
                    linkedRocket = assembler.currentRocket;
                } else linkedAssemblerPos = null;
            }

            if (linkedRocket != null) {
                if (linkedRocket.getCurrentProgram() == null) {
                    if (battery.getEnergyStored() >= Config.INSTANCE.item_Loader_Energy_Per_Tick) {
                        if (!isDrain) {
                            // FILL the rocket
                            if (loadOneItem(linkedRocket)) {
                                setOutputSignal(false);
                                battery.extractEnergy(Config.INSTANCE.item_Loader_Energy_Per_Tick, false);
                            } else {
                                setOutputSignal(true);
                            }
                        } else {
                            // DRAIN the rocket
                            if (unLoadOneItem(linkedRocket)) {
                                setOutputSignal(false);
                                battery.extractEnergy(Config.INSTANCE.item_Loader_Energy_Per_Tick, false);
                            } else {
                                setOutputSignal(false);
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

            if (linkedRocket != null) {
                if (linkedRocket.isRemoved() || linkedRocket.position().distanceTo(getBlockPos().getCenter()) >= maxDistance)
                    linkedRocket = null;
            }
        }
    }

    @Override
    public boolean link(BlockPos otherpos, Level otherLevel) {
        if (otherLevel == level) {
            Block otherBlock = level.getBlockState(otherpos).getBlock();
            if (otherBlock.equals(Registry.ROCKET_ASSEMBLER.get())) {
                if (otherpos.getCenter().distanceTo(getBlockPos().getCenter()) < maxDistance) {
                    linkedAssemblerPos = otherpos;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean link(Entity e) {
        if (e instanceof EntityRocket rocket) {
            if (rocket.position().distanceTo(getBlockPos().getCenter()) < maxDistance) {
                if (rocket.level().equals(level)) {
                    linkedRocket = rocket;
                    linkedAssemblerPos = null;
                    return true;
                }
            }
        }
        return false;
    }
}
