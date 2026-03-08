package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.INetworkTagReceiver;
import advRocketry.Blocks.LaunchStation;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemSatelliteIdChip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.BlockEntities.ENTITY_LAUNCH_STATION;
import static advRocketry.Registry.BlockEntities.ENTITY_SATELLITE_MONITOR;

public class EntitySatelliteMonitor extends BlockEntity implements INetworkTagReceiver {

    public ItemStackHandler inventory;

    public GuiHandlerBlockEntity guiHandler;

    public EntitySatelliteMonitor(BlockPos pos, BlockState blockState) {
        super(ENTITY_SATELLITE_MONITOR.get(), pos, blockState);

        inventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ItemSatelliteIdChip;
            }
        };

        makeGui();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySatelliteMonitor) t).tick();
    }

    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);

        guiHandler.modules.add(new guiModuleText(0, "Satellite Monitor", guiHandler, 5, 5, 0xff000000, false));

        guiHandler.modules.add(new guiModuleItemHandlerSlot(1, inventory, 0, 0, 1, guiHandler, 50, 20));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 2000, 1, 0, guiHandler));
    }

    public void openGui(){
        guiHandler.openGui(176, 165, true);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }
}
