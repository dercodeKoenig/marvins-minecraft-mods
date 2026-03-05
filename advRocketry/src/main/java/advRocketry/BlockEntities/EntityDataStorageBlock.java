package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleImage;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleVerticalProgressBar;
import advRocketry.Data.DataStack;
import advRocketry.Data.DataStorage;
import advRocketry.Data.SimpleDataContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.xml.crypto.Data;

import static advRocketry.Registry.ENTITY_DATA_STORAGE_BLOCK;

public class EntityDataStorageBlock extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public DataStorage dataStorage;

    public GuiHandlerBlockEntity guiHandler;
    public ItemStackHandler itemStackHandler;
    public guiModuleVerticalProgressBar progressBar;
    public SimpleDataContainer simpleDataContainer;

    public EntityDataStorageBlock(BlockPos pos, BlockState blockState) {
        super(ENTITY_DATA_STORAGE_BLOCK.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);

        dataStorage = new DataStorage(4000) {
            @Override
            public void onChange() {
                setChanged();
                updateDataBar();
            }
        };

        progressBar = new guiModuleVerticalProgressBar(0, guiHandler, 10, 10);
        progressBar.progress = 0;
        progressBar.bar = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_vertical_progress_bar_green.png");
        guiHandler.modules.add(progressBar);


        itemStackHandler = new ItemStackHandler(2) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };

        simpleDataContainer = new SimpleDataContainer(dataStorage, itemStackHandler);
        // i could use the simpleDataContainer as itemhandler in gui but i choose to use itemStackHandler
        // because it allows players to block the lower slot with a fully filled disk so it writes only full disks
        guiHandler.modules.add(new guiModuleItemHandlerSlot(1, itemStackHandler, 0, 0, 1, guiHandler, 30, 10));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(2, itemStackHandler, 1, 0, 1, guiHandler, 30, 45));
        ResourceLocation arrow = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png");
        guiHandler.modules.add(new guiModuleImage(guiHandler, 30, 28, 16, 16, arrow, 12, 16));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 2000, 1, 0, guiHandler));

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityDataStorageBlock) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(!level.isClientSide)
            updateDataBar();

        DataStack test = new DataStack("testData", (int)(Math.random()*1000));
        dataStorage.setStackDirect(test);
    }

    public void updateDataBar() {
        int data = 0;
        String pre = "";
        if (dataStorage.getDataStack() != null) {
            data = dataStorage.getDataStack().amount;
            pre = dataStorage.getDataStack().type + ": ";
        }
        progressBar.setProgressAndSync((double) data / dataStorage.getDataCapacity());
        progressBar.setHoverInfoAndSync(pre  + data + "/" + dataStorage.getDataCapacity());
    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < itemStackHandler.getSlots(); i++) {
                Block.popResource(level, getBlockPos(), itemStackHandler.getStackInSlot(i));
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
        tag.put("dataStorage", dataStorage.saveToNbt());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemStackHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        dataStorage.readFromNbt(tag.getCompound("dataStorage"));
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
            simpleDataContainer.performPossibleDataTransfer();
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 168, true);
    }
}
