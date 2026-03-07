package advRocketry.BlockEntities;

import ARLib.ARLib;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleImage;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import advRocketry.Items.ItemSatellite;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Satellites.Satellite;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.spi.AbstractResourceBundleProvider;

import static advRocketry.Registry.BlockEntities.ENTITY_SATELLITE_ASSEMBLER;

public class EntitySatelliteAssembler extends BlockEntity implements INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler = new GuiHandlerBlockEntity(this);

    public Satellite satellite;
    int satellite_input_slot = 0;
    int satellite_output_slot = 1;
    int chip_main_slot = 2;
    int chip_slot_2 = 3;

    public EntitySatelliteAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_SATELLITE_ASSEMBLER.get(), pos, blockState);

        int id = 0;

        // inventory slots
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, satellite_input_slot, 0, 1, guiHandler, 20, 20));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, satellite_output_slot, 0, 1, guiHandler, 70, 20));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, chip_main_slot, 0, 1, guiHandler, 20, 45));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, chip_slot_2, 0, 1, guiHandler, 40, 45));
        guiHandler.modules.add(
                new guiModuleImage(guiHandler, 40, 20, 25, 20, ResourceLocation.fromNamespaceAndPath(ARLib.MODID, "textures/gui/arrow_right.png"), 16, 12)
        );

        // satellite inventory slots
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 0, 0, 1, guiHandler, 150,10));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 1, 0, 1, guiHandler, 110,30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 2, 0, 1, guiHandler, 130,30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 3, 0, 1, guiHandler, 150,30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 4, 0, 1, guiHandler, 110,50));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 5, 0, 1, guiHandler, 130,50));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 6, 0, 1, guiHandler, 150,50));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 100,1,0,guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 200,1,0,guiHandler));

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySatelliteAssembler) t).tick();
    }

    void saveSatelliteToInventory() {
        if(satellite == null)
            return;
        ItemStack stack = inventory.getStackInSlot(satellite_input_slot);
        ItemSatellite.saveToStack(stack, satellite, level.registryAccess());
        loadSatelliteFromInventory();
        setChanged();
    }

    void loadSatelliteFromInventory() {
        satellite = ItemSatellite.createFromItem(inventory.getStackInSlot(satellite_input_slot), level.registryAccess());
    }

    public void popInventory(){
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                Block.popResource(level,getBlockPos(),inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        loadSatelliteFromInventory();
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
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }

    public void openGui() {
        if (level.isClientSide) {
            guiHandler.openGui(178, 168, true);
        }
    }

    // the inventory of the rocket assembler, 4 slots (satellite io + 2 chip slots for copy)
    public ItemStackHandler inventory = new ItemStackHandler(4) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if(slot == satellite_input_slot)
                return stack.getItem() instanceof ItemSatellite;
            if(slot == chip_slot_2 || slot == chip_main_slot)
                return stack.getItem() instanceof ItemSatelliteIdChip;
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public void onContentsChanged(int slot) {
            // move chip from slot 2 to 1
            if (slot == chip_slot_2) {
                if (getStackInSlot(chip_main_slot).isEmpty() && !getStackInSlot(chip_slot_2).isEmpty()) {
                    insertItem(chip_main_slot, extractItem(chip_slot_2, 1, false), false);
                }
            }
            if (slot == satellite_input_slot) {
                loadSatelliteFromInventory();
            }
            setChanged();
        }
    };

    // a proxy item handler to access the satellite inventory and to detect a change,
    // so it can re-write the satellite to the itemStack
    public IItemHandler proxyItemHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 7;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            if (satellite != null)
                return satellite.inventory.getStackInSlot(i);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
            if (satellite != null) {
                ItemStack res = satellite.inventory.insertItem(i, itemStack, b);
                saveSatelliteToInventory();
                return res;
            }
            return itemStack;
        }

        @Override
        public ItemStack extractItem(int i, int i1, boolean b) {
            if (satellite != null) {
                ItemStack res = satellite.inventory.extractItem(i, i1, b);
                saveSatelliteToInventory();
                return res;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int i) {
            if (satellite != null)
                return satellite.inventory.getSlotLimit(i);
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (satellite != null)
                return satellite.inventory.isItemValid(slot, stack);
            return false;
        }
    };


}
