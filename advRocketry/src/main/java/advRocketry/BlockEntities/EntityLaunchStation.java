package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import advRocketry.Blocks.LaunchStation;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
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

public class EntityLaunchStation extends EntityRocketInfrastructureBase implements ItemLinker.linkable, ItemLinker.linkableToEntity, INetworkTagReceiver {

    public ItemStackHandler inventory;

    public GuiHandlerBlockEntity guiHandler;

    public boolean isRedstonePowered = false;
    public int activeTimeout = 0; // when launch, shortly change the block state to active, change back after a few ticks
    protected int launch_btn_id = 10001;

    public EntityLaunchStation(BlockPos pos, BlockState blockState) {
        this(ENTITY_LAUNCH_STATION.get(), pos, blockState);
    }

    public EntityLaunchStation(BlockEntityType type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);

        inventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                onInventoryChanged();
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return EntityLaunchStation.this.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };

        makeGui();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityLaunchStation) t).tick();
    }

    // maybe overwrite this
    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);

        guiHandler.modules.add(new guiModuleText(0, "Launch Station", guiHandler, 5, 5, 0xff000000, false));

        guiHandler.modules.add(new guiModuleItemHandlerSlot(1, inventory, 0, 0, 1, guiHandler, 50, 20));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 110, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 50, 2000, 1, 0, guiHandler));

        guiModuleButton launchButton = new guiModuleButton(launch_btn_id, "launch", guiHandler, 90, 20, 40, 15, BTN_GREEN, BTN_W, BTN_H);
        guiHandler.modules.add(launchButton);
    }

    // overwrite in subclasses
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemPlanetIdChip)
            return true;
        if (stack.getItem() instanceof ItemLinker)
            return true;
        return false;
    }

    // overwrite in subclasses
    public void onInventoryChanged() {

    }

    public void openGui() {
        guiHandler.openGui(176, 135, true);
    }

    public boolean launch() {
        if (linkedRocket != null) {
            ItemStack navigationItem = inventory.getStackInSlot(0);
            linkedRocket.launch(navigationItem);
            return true;
        }
        return false;
    }

    public final boolean _launch() {
        boolean res = launch();
        if (res) {
            level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.STATE, LaunchStation.State.active), 3);
            activeTimeout = 40;
        }
        return res;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            if (btn == launch_btn_id) {
                if (_launch())
                    guiHandler.signalCloseGui(serverPlayer);
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
        tag.putBoolean("isRedstonePowered", isRedstonePowered);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        isRedstonePowered = tag.getBoolean("isRedstonePowered");
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
            super.serverTick();
            if (activeTimeout > 0) {
                activeTimeout--;
            } else {
                if (linkedRocket != null && getBlockState().getValue(LaunchStation.STATE) != LaunchStation.State.rocket_landed)
                    level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.STATE, LaunchStation.State.rocket_landed), 3);
                if (linkedRocket == null && getBlockState().getValue(LaunchStation.STATE) != LaunchStation.State.idle)
                    level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.STATE, LaunchStation.State.idle), 3);
            }
        }
    }
}
