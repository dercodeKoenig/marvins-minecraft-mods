package ARLib.blockentities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.guiModuleFluidTankDisplay;
import ARLib.gui.modules.guiModuleImage;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.utils.SimpleFluidContainer;
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

public class EntityFluidInputBlock extends BlockEntity implements INetworkTagReceiver {

    public FluidTank myTank;
    public ItemStackHandler inventory;
    public SimpleFluidContainer simpleFluidContainer;
    public GuiHandlerBlockEntity guiHandler;

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
        simpleFluidContainer = new SimpleFluidContainer(myTank, inventory);

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.getModules().add(new guiModuleFluidTankDisplay(0, simpleFluidContainer, 0, guiHandler, 10, 10));
        guiModuleItemHandlerSlot s1 = new guiModuleItemHandlerSlot(1, simpleFluidContainer, 0, 1, 0, guiHandler, 30, 10);
        s1.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18, 18);
        guiHandler.getModules().add(s1);
        guiHandler.getModules().add(new guiModuleItemHandlerSlot(2, simpleFluidContainer, 1, 1, 0, guiHandler, 30, 45));

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
        simpleFluidContainer.popItems(level, getBlockPos());
        super.setRemoved();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        simpleFluidContainer.loadAdditional(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        simpleFluidContainer.saveAdditional(tag, registries);

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


    public void tick(){
        if(!level.isClientSide){
            guiHandler.serverTick();
            simpleFluidContainer.performPossibleFluidTransfer();
        }
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        ((EntityFluidInputBlock) t).tick();
    }
}
