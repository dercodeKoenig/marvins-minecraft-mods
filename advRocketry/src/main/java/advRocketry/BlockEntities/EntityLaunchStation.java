package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.LaunchPad;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_LAUNCH_STATION;
import static advRocketry.Registry.LAUNCH_STATION;

public class EntityLaunchStation extends EntityRocketInfrastructureBase implements ItemLinker.linkable, ItemLinker.linkableToEntity, INetworkTagReceiver {

    public ItemStackHandler inventory;

    public GuiHandlerBlockEntity guiHandler;

    public boolean isRedstonePowered = false;
    int activeTimeout = 0; // when launch, shortly change the block state to active, change back after a few ticks

    public EntityLaunchStation(BlockPos pos, BlockState blockState) {
        super(ENTITY_LAUNCH_STATION.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);

        inventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (stack.getItem() instanceof ItemPlanetIdChip)
                    return true;
                if (stack.getItem() instanceof ItemLinker)
                    return true;
                return false;
            }
        };
        guiHandler.modules.add(new guiModuleItemHandlerSlot(0, inventory, 0, 0, 1, guiHandler, 50, 20));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 110, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 50, 2000, 1, 0, guiHandler));

        guiModuleButton launchButton = new guiModuleButton(11001, "launch", guiHandler, 70, 10, 40, 15, BTN_GREEN, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.put("launch", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityLaunchStation.this, info));
            }
        };
        guiHandler.modules.add(launchButton);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityLaunchStation) t).tick();
    }

    public void launch() {
        if (linkedRocket != null) {
            ItemStack navigationItem = inventory.getStackInSlot(0);
            linkedRocket.launch(navigationItem);
        }
        level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.ACTIVE, true), 3);
        activeTimeout = 40;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("launch")) {
            launch();
            guiHandler.signalCloseGui(serverPlayer);
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
                if (activeTimeout == 0)
                    level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.ACTIVE, false), 3);
            }
        }
    }
}
