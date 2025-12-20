package ARLib.blockentities;


import ARLib.gui.IGuiHandler;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.utils.BlockEntityItemStackHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

import static ARLib.ARLibRegistry.ENTITY_ITEM_INPUT_BLOCK;
import static net.minecraft.world.level.block.Block.popResource;

public class EntityItemInputBlock extends BlockEntity implements IItemHandler, INetworkTagReceiver {

    public BlockEntityItemStackHandler inventory;
    public GuiHandlerBlockEntity guiHandler;


    public EntityItemInputBlock(BlockPos pos, BlockState blockState) {
        this(ENTITY_ITEM_INPUT_BLOCK.get(), pos, blockState);
    }

    public EntityItemInputBlock(BlockEntityType t, BlockPos pos, BlockState blockState) {
        super(t, pos, blockState);


        guiHandler = new GuiHandlerBlockEntity(this);

        int containergroup = 0;
        int playerinventorygroup = 1;
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(0, this, 0, containergroup, playerinventorygroup, this.guiHandler, 45, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(1, this, 1, containergroup, playerinventorygroup, this.guiHandler, 65, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(2, this, 2, containergroup, playerinventorygroup, this.guiHandler, 85, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(3, this, 3, containergroup, playerinventorygroup, this.guiHandler, 105, 10));

        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 100, 100, playerinventorygroup, containergroup, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar) {
            this.guiHandler.getModules().add(i);
        }

        List<guiModulePlayerInventorySlot> playerInv = guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 40, 200, playerinventorygroup, containergroup, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInv) {
            this.guiHandler.getModules().add(i);
        }


        inventory = new BlockEntityItemStackHandler(4, this);
    }


    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag inv = inventory.serializeNBT(registries);
        tag.put("inventory", inv);
    }


    @Override
    public void readServer(CompoundTag tagIn, ServerPlayer p) {
        this.guiHandler.readServer(tagIn);
    }

    @Override
    public void readClient(CompoundTag tagIn) {
        this.guiHandler.readClient(tagIn);
        if (tagIn.contains("openGui")) {
            openGui();
        }
    }

    public void openGui() {
        guiHandler.openGui(176, 126, true);
    }

    public void popItems() {
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i).copy();
                popResource(level, getBlockPos(), stack);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 99;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inventory.isItemValid(slot, stack);
    }

    public void tick() {
        if (!level.isClientSide)
            guiHandler.serverTick();
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        ((EntityItemInputBlock) t).tick();
    }
}
