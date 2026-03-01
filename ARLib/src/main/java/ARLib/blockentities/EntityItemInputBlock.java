package ARLib.blockentities;


import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

import static ARLib.ARLibRegistry.ENTITY_ITEM_INPUT_BLOCK;
import static net.minecraft.world.level.block.Block.popResource;

public class EntityItemInputBlock extends BlockEntity implements INetworkTagReceiver {

    public ItemStackHandler inventory;
    public GuiHandlerBlockEntity guiHandler;


    public EntityItemInputBlock(BlockPos pos, BlockState blockState) {
        this(ENTITY_ITEM_INPUT_BLOCK.get(), pos, blockState);
    }

    public EntityItemInputBlock(BlockEntityType t, BlockPos pos, BlockState blockState) {
        super(t, pos, blockState);

        inventory = new ItemStackHandler(4){
            @Override
            public void onContentsChanged(int slot){
                EntityItemInputBlock.this.setChanged();
            }
        };

        guiHandler = new GuiHandlerBlockEntity(this);

        int containergroup = 0;
        int playerinventorygroup = 1;
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(0, inventory, 0, containergroup, playerinventorygroup, this.guiHandler, 45, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(1, inventory, 1, containergroup, playerinventorygroup, this.guiHandler, 65, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(2, inventory, 2, containergroup, playerinventorygroup, this.guiHandler, 85, 10));
        this.guiHandler.getModules().add(new guiModuleItemHandlerSlot(3, inventory, 3, containergroup, playerinventorygroup, this.guiHandler, 105, 10));

        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 100, 100, playerinventorygroup, containergroup, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar) {
            this.guiHandler.getModules().add(i);
        }

        List<guiModulePlayerInventorySlot> playerInv = guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 40, 200, playerinventorygroup, containergroup, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInv) {
            this.guiHandler.getModules().add(i);
        }
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
    }

    public void signalOpenGui(ServerPlayer player) {
        guiHandler.signalOpenGui(player,176, 126, true);
    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i).copy();
                popResource(level, getBlockPos(), stack);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    public void tick() {
        if (!level.isClientSide)
            guiHandler.serverTick();
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        ((EntityItemInputBlock) t).tick();
    }
}
