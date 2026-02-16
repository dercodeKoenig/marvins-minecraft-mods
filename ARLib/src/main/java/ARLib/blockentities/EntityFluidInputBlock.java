package ARLib.blockentities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.guiModuleFluidTankDisplay;
import ARLib.gui.modules.guiModuleImage;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

import static ARLib.ARLibRegistry.ENTITY_FLUID_INPUT_BLOCK;
import static net.minecraft.world.level.block.Block.popResource;

public class EntityFluidInputBlock extends BlockEntity implements IItemHandler, IFluidHandler, INetworkTagReceiver {

    public FluidTank myTank;
    public GuiHandlerBlockEntity guiHandler;

    public ItemStackHandler inventory;

    public EntityFluidInputBlock(BlockPos pos, BlockState blockState) {
        this(ENTITY_FLUID_INPUT_BLOCK.get(), pos, blockState);
    }

    public EntityFluidInputBlock(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        myTank = new FluidTank(4000) {
            @Override
            protected void onContentsChanged() {
                EntityFluidInputBlock.this.setChanged();
            }
        };
        inventory = new ItemStackHandler(2) {
            @Override
            protected void onContentsChanged(int slot) {
                EntityFluidInputBlock.this.setChanged();
            }
            // no isItemValid filter here, we do our own filter to allow insert into slot 2 only from our internal logic
        };

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.getModules().add(new guiModuleFluidTankDisplay(0, this, 0, guiHandler, 10, 10));
        guiModuleItemHandlerSlot s1 = new guiModuleItemHandlerSlot(1, this, 0, 1, 0, guiHandler, 30, 10);
        s1.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18, 18);
        guiHandler.getModules().add(s1);
        guiHandler.getModules().add(new guiModuleItemHandlerSlot(2, this, 1, 1, 0, guiHandler, 30, 45));

        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 10, 0, 1, guiHandler)) {
            guiHandler.getModules().add(i);
        }
        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 70, 30, 0, 1, guiHandler)) {
            guiHandler.getModules().add(i);
        }
        ResourceLocation arrow = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png");
        guiHandler.getModules().add(new guiModuleImage(guiHandler, 32, 28, 16, 16, arrow, 12, 16));
    }

    public void popItems() {
        if (!level.isClientSide) {
            ItemStack stack1 = inventory.getStackInSlot(0).copy();
            popResource(level, getBlockPos(), stack1);
            inventory.setStackInSlot(0, ItemStack.EMPTY);

            ItemStack stack2 = inventory.getStackInSlot(1).copy();
            popResource(level, getBlockPos(), stack2);
            inventory.setStackInSlot(1, ItemStack.EMPTY);

            setChanged();
        }
        super.setRemoved();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myTank.readFromNBT(registries, tag.getCompound("tank"));
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankNBT = new CompoundTag();
        myTank.writeToNBT(registries, tankNBT);
        tag.put("tank", tankNBT);

        CompoundTag inventoryTag = inventory.serializeNBT(registries);
        tag.put("inventory", inventoryTag);

    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        guiHandler.readServer(tag);
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
    }

    public void signalOpenGui(ServerPlayer player) {
        guiHandler.signalOpenGui(player, 176, 165, true);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return myTank.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return myTank.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return myTank.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return myTank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return myTank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return myTank.drain(maxDrain, action);
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (isItemValid(slot, stack)) // inventory has all valid because we need to allow insert into slot 2 from internal logic
            return inventory.insertItem(slot, stack, simulate);
        else return stack;
    }

    public ItemStack insertItemIgnoreFilter(int slot, ItemStack stack, boolean simulate) {
        return inventory.insertItem(slot, stack, simulate);
    }


    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && stack.getCapability(Capabilities.FluidHandler.ITEM) != null; // slot 1 is output only
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();

            ItemStack stack = getStackInSlot(0);
            if (!stack.isEmpty()) {

                // Make a single-item copy to operate on
                ItemStack stackCopy = stack.copyWithCount(1);
                IFluidHandlerItem fluidHandlerCopy = stackCopy.getCapability(Capabilities.FluidHandler.ITEM);

                if (fluidHandlerCopy != null) {
                    FluidStack fluidInTank = getFluidInTank(0);
                    int tankCapacity = getTankCapacity(0);

                    // 1. Try to drain fluid from the item into the tank
                    int maxFill = tankCapacity - fluidInTank.getAmount();
                    FluidStack drained = fluidHandlerCopy.drain(maxFill, FluidAction.EXECUTE);
                    int canFill = fill(drained, FluidAction.SIMULATE);
                    ItemStack resultItem = fluidHandlerCopy.getContainer();

                    // If all drained can fit into the tank, and we can insert result item into slot 1, commit!
                    if (!drained.isEmpty() && canFill == drained.getAmount() && insertItemIgnoreFilter(1, resultItem, true).isEmpty()) {
                        // Commit the drain, fluid transfer, and item movement
                        fill(drained, FluidAction.EXECUTE);
                        extractItem(0, 1, false);
                        insertItemIgnoreFilter(1, resultItem, false);
                    }

                    // 2. If draining did not work, try filling the item instead
                    else {
                        //make new copy because it may have been modified in tee code above
                        stackCopy = stack.copyWithCount(1);
                        fluidHandlerCopy = stackCopy.getCapability(Capabilities.FluidHandler.ITEM);

                        // Execute the fill operation and get the transformed container item
                        // make a copy of the fluidStack so that the original tank is not modified
                        int filled = fluidHandlerCopy.fill(fluidInTank.copy(), FluidAction.EXECUTE);
                        resultItem = fluidHandlerCopy.getContainer();
                        // Try inserting result item into slot 1
                        if (insertItemIgnoreFilter(1, resultItem, true).isEmpty()) {
                            // Commit the fill, fluid transfer, and item movement
                            drain(fluidInTank.copyWithAmount(filled), FluidAction.EXECUTE);
                            extractItem(0, 1, false);
                            insertItemIgnoreFilter(1, resultItem, false);
                        }
                    }
                }
            }
        }
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        ((EntityFluidInputBlock) t).tick();
    }
}
