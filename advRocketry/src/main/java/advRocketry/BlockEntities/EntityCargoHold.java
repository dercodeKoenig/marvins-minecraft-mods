package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
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

import static advRocketry.Registry.ENTITY_CARGO_HOLD;

public class EntityCargoHold extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public ItemStackHandler itemStackHandler;

    public EntityCargoHold(BlockPos pos, BlockState blockState) {
        super(ENTITY_CARGO_HOLD.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        itemStackHandler = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        guiHandler.modules.add(new guiModuleItemHandlerSlot(0, itemStackHandler, 0, 0, 1, guiHandler, 50, 20));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 110, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 50, 2000, 1, 0, guiHandler));

    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < itemStackHandler.getSlots(); i++) {
                ItemStack stack = itemStackHandler.getStackInSlot(i);
                Block.popResource(level,getBlockPos(),stack);
                itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }


    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemStackHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemStackHandler.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityCargoHold) t).tick();
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 140, true);
    }
}
