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

import java.util.ArrayList;
import java.util.List;

import static ARLib.ARLibRegistry.ENTITY_FLUID_INPUT_BLOCK;
import static net.minecraft.world.level.block.Block.popResource;

public class EntityFluidInputBlock extends BlockEntity implements IItemHandler,IFluidHandler, INetworkTagReceiver {

    FluidTank myTank;
    GuiHandlerBlockEntity guiHandler;

    List<ItemStack> inventory;

    public EntityFluidInputBlock(BlockPos pos, BlockState blockState) {
        this(ENTITY_FLUID_INPUT_BLOCK.get(), pos, blockState);
    }

    public EntityFluidInputBlock(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        myTank = new FluidTank(4000);

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.getModules().add(new guiModuleFluidTankDisplay(0, this, 0, guiHandler, 10, 10));
        guiModuleItemHandlerSlot s1 = new guiModuleItemHandlerSlot(1, this, 0, 1, 0, guiHandler, 30, 10);
        s1.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18,18);
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

        inventory = new ArrayList<>();
        inventory.add(ItemStack.EMPTY);
        inventory.add(ItemStack.EMPTY);
    }

    public void popItems(){
        if(!level.isClientSide) {
            ItemStack stack1 = inventory.get(0).copy();
            popResource(level, getBlockPos(), stack1);

            ItemStack stack2 = inventory.get(1).copy();
            popResource(level, getBlockPos(), stack2);
        }
        super.setRemoved();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myTank.readFromNBT(registries, tag.getCompound("tank"));
        if (tag.getBoolean("inv1"))
            inventory.set(0, ItemStack.parse(registries, tag.getCompound("inventorySlot1")).get());
        if (tag.getBoolean("inv2"))
            inventory.set(1, ItemStack.parse(registries, tag.getCompound("inventorySlot2")).get());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankNBT = new CompoundTag();
        myTank.writeToNBT(registries, tankNBT);
        tag.put("tank", tankNBT);

        if (!inventory.get(0).isEmpty()) {
            Tag inventorySlot = inventory.get(0).save(registries);
            tag.put("inventorySlot1", inventorySlot);
            tag.putBoolean("inv1", true);
        } else {
            tag.putBoolean("inv1", false);
        }
        if (!inventory.get(1).isEmpty()) {
            Tag inventorySlot = inventory.get(1).save(registries);
            tag.put("inventorySlot2", inventorySlot);
            tag.putBoolean("inv2", true);
        } else {
            tag.putBoolean("inv2", false);
        }

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
        return myTank.isFluidValid(tank,stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = myTank.fill(resource, action);
        if (filled > 0)
            this.setChanged();
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack drained = myTank.drain(resource, action);
        if (drained != FluidStack.EMPTY)
            this.setChanged();
        return drained;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack drained = myTank.drain(maxDrain, action);
        if (drained != FluidStack.EMPTY)
            this.setChanged();
        return drained;
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        guiHandler.readServer(tag);
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        if(tag.contains("openGui")){
            openGui();
        }
    }

    public void openGui() {
        guiHandler.openGui(176, 165, true);
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insertItem(slot, stack, simulate, false);
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, boolean ignore_filter) {
        // slot 0 is for insert, slot 1 for extract so do not allow insert into slot 1
        if (slot == 1 && !ignore_filter) return stack;

        if (!isItemValid(slot, stack) && !ignore_filter) return stack;

        int stackLimit = stack.getMaxStackSize();
        ItemStack existing = inventory.get(slot);
        if (existing.isEmpty()) {
            int toInsert = Math.min(stack.getCount(), stackLimit);
            if (!simulate) {
                inventory.set(slot, stack.copyWithCount(toInsert));
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - toInsert);
        } else if (ItemStack.isSameItemSameComponents(stack, existing)) {
            int existingCount = existing.getCount();
            int toInsert = Math.min(stackLimit - existingCount, stack.getCount());
            if (!simulate) {
                inventory.set(slot, stack.copyWithCount(toInsert+existingCount));
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - toInsert);
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!inventory.get(slot).isEmpty()) {
            int toExtract = Math.min(amount, inventory.get(slot).getCount());
            ItemStack ret = inventory.get(slot).copyWithCount(toExtract);
            if (!simulate) {
                inventory.get(slot).shrink(toExtract);
                setChanged();
            }
            return ret;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 99;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == 1) return false; // extraction slot
        return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }


    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        if (!level.isClientSide) {
            ((EntityFluidInputBlock) t).guiHandler.serverTick();


            EntityFluidInputBlock tile = (EntityFluidInputBlock) t;
            ItemStack stack = tile.getStackInSlot(0);
            if (!stack.isEmpty()) {

                // Make a single-item copy to operate on
                ItemStack currentItem = stack.copyWithCount(1);
                IFluidHandlerItem fluidHandler = currentItem.getCapability(Capabilities.FluidHandler.ITEM);

                if (fluidHandler != null) {
                    FluidStack drained;
                    FluidStack fluidInTank = ((IFluidHandler) tile).getFluidInTank(0);
                    int tankCapacity = ((IFluidHandler) tile).getTankCapacity(0);

                    // 1. Try to drain fluid from the item into the tank
                    if (fluidInTank.isEmpty()) {
                        // Tank is empty; try to drain as much as possible from the item
                        drained = fluidHandler.drain(tankCapacity, FluidAction.EXECUTE);
                        // Get the resulting container item after executing the drain
                        ItemStack resultItem = fluidHandler.getContainer();

                        // Try inserting result item into slot 1
                        if (!drained.isEmpty()&&tile.insertItem(1, resultItem, true, true).isEmpty()) {
                            // Commit the drain, fluid transfer, and item movement
                            ((IFluidHandler) tile).fill(drained, FluidAction.EXECUTE);
                            tile.extractItem(0, 1, false);
                            tile.insertItem(1, resultItem, false, true);

                        }
                    } else {
                        // Tank has fluid; calculate maximum fillable amount
                        int maxFill = tankCapacity - fluidInTank.getAmount();
                        drained = fluidHandler.drain(maxFill, FluidAction.EXECUTE);
                        int filled = tile.fill(drained, FluidAction.SIMULATE);
                        ItemStack resultItem = fluidHandler.getContainer();

                        // Try inserting result item into slot 1
                        if(!drained.isEmpty() && filled == drained.getAmount()) {
                            if (tile.insertItem(1, resultItem, true, true).isEmpty()) {
                                // Commit the drain, fluid transfer, and item movement
                                ((IFluidHandler) tile).fill(drained, FluidAction.EXECUTE);
                                tile.extractItem(0, 1, false);
                                tile.insertItem(1, resultItem, false, true);
                            }
                        }else{
                            drained = FluidStack.EMPTY;
                        }
                    }

                    // 2. If draining yielded no fluid, try filling the item instead
                    if (drained.isEmpty()) {
                        //make new copy because it may have been modified in tee code above
                        currentItem = stack.copyWithCount(1);
                        fluidHandler = currentItem.getCapability(Capabilities.FluidHandler.ITEM);

                        FluidStack wasInTank = fluidInTank.copy();
                        // Execute the fill operation and get the transformed container item
                        int filled = fluidHandler.fill(wasInTank, FluidAction.EXECUTE);
                        ItemStack resultItem = fluidHandler.getContainer();
                        // Try inserting result item into slot 1
                        if (tile.insertItem(1, resultItem, true, true).isEmpty()) {
                            // Commit the fill, fluid transfer, and item movement
                            ((IFluidHandler) tile).drain(wasInTank.copyWithAmount(filled), FluidAction.EXECUTE);
                            tile.extractItem(0, 1, false);
                            tile.insertItem(1, resultItem, false, true);
                        }
                    }
                }
            }
        }
    }
}
