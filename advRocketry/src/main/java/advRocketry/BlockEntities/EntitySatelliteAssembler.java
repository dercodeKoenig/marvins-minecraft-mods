package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import advRocketry.Items.ItemSatellite;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Registry.Items;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatellitePrimaryFunction;
import advRocketry.Utils.ItemUtils;
import com.mojang.datafixers.util.Pair;
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

import java.util.UUID;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.BlockEntities.ENTITY_SATELLITE_ASSEMBLER;

public class EntitySatelliteAssembler extends BlockEntity implements INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler = new GuiHandlerBlockEntity(this);
    public guiModuleText statusText;
    public Satellite satellite;
    public int infoTimeout = 0;
    public int satellite_input_slot = 0;
    public int satellite_output_slot = 1;
    public int chip_main_slot = 2;
    public int chip_slot_2 = 3;

    public EntitySatelliteAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_SATELLITE_ASSEMBLER.get(), pos, blockState);

        int id = 0;

        // inventory slots
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, satellite_input_slot, 0, 1, guiHandler, 20, 20));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, satellite_output_slot, 0, 1, guiHandler, 70, 20));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, chip_main_slot, 0, 1, guiHandler, 20, 45));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, chip_slot_2, 0, 1, guiHandler, 70, 45));
        guiHandler.modules.add(
                new guiModuleImage(guiHandler, 40, 30, 25, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/arrow_right.png"), 16, 12)
        );

        // action buttons
        guiModuleButton buildBtn = new guiModuleButton(1000, "build", guiHandler, 10, 75, 60, 20, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(buildBtn);
        guiModuleButton copyChipBtn = new guiModuleButton(1001, "copy chip", guiHandler, 80, 75, 60, 20, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(copyChipBtn);

        statusText = new guiModuleText(id++, "", guiHandler, 10, 100, 0xff000000, false);
        guiHandler.modules.add(statusText);

        // satellite inventory slots
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 0, 0, 1, guiHandler, 150, 10));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 1, 0, 1, guiHandler, 110, 30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 2, 0, 1, guiHandler, 130, 30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 3, 0, 1, guiHandler, 150, 30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 4, 0, 1, guiHandler, 110, 50));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 5, 0, 1, guiHandler, 130, 50));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, proxyItemHandler, 6, 0, 1, guiHandler, 150, 50));

        // player inventory
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 170, 100, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 110, 200, 1, 0, guiHandler));

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySatelliteAssembler) t).tick();
    }

    void saveSatelliteToInventory() {
        if (satellite == null)
            return;
        ItemStack stack = inventory.getStackInSlot(satellite_input_slot);
        ItemSatellite.saveToStack(stack, satellite, level.registryAccess());
        loadSatelliteFromInventory();
        setChanged();
    }

    void loadSatelliteFromInventory() {
        satellite = ItemSatellite.createFromItem(inventory.getStackInSlot(satellite_input_slot), level.registryAccess());
    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                Block.popResource(level, getBlockPos(), inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    public Pair<Boolean, String> performBuild(boolean simulate) {
        if (satellite == null)
            return Pair.of(false, "no satellite");
        if (!inventory.getStackInSlot(satellite_output_slot).isEmpty())
            return Pair.of(false, "slot blocked");
        ItemStack satelliteChip = inventory.getStackInSlot(chip_main_slot);
        if (!(satelliteChip.getItem() instanceof ItemSatelliteIdChip))
            return Pair.of(false, "missing id chip");

        ItemStack stack = satellite.inventory.getStackInSlot(0);
        if (stack.getItem() instanceof SatellitePrimaryFunction primaryFunction) {
            Pair<Satellite, Pair<Boolean, String>> res = primaryFunction.build(satellite, satelliteChip);
            Satellite resultSatellite = res.getFirst();
            if (resultSatellite != null) {
                UUID uuid = UUID.randomUUID();
                resultSatellite.uuid = uuid;
                ItemSatelliteIdChip.setTarget(satelliteChip, uuid);
                if (!simulate) {
                    ItemStack satellite = inventory.extractItem(satellite_input_slot, 1, false);
                    ItemSatellite.saveToStack(satellite, resultSatellite, level.registryAccess());
                    inventory.setStackInSlot(satellite_output_slot, satellite);
                    setChanged();
                }
            }
            return res.getSecond();
        }

        return Pair.of(false, "missing primary function");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        loadSatelliteFromInventory();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            if (btn == 1000) {
                // build
               Pair<Boolean, String> res = performBuild(false);
               if(!res.getFirst()){
                   statusText.setTextAndSync(res.getSecond());
                   infoTimeout = 20 * 10;
               }
            }
            if (btn == 1001) {
                // copy chip
                ItemStack src = inventory.getStackInSlot(chip_main_slot);
                ItemStack dst = inventory.getStackInSlot(chip_slot_2);
                if(src.getItem() == Items.ITEM_SATELLITE_ID_CHIP && dst.getItem() == Items.ITEM_SATELLITE_ID_CHIP){
                    ItemUtils.setTag(dst, ItemUtils.getStacktagOrEmpty(src));
                    setChanged();
                }
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
            if (infoTimeout > 0) {
                infoTimeout--;
                if (infoTimeout == 0) {
                    statusText.setTextAndSync("");
                }
            }
        }
    }

    public void openGui() {
        if (level.isClientSide) {
            guiHandler.openGui(178, 198, true);
        }
    }

    // the inventory of the rocket assembler, 4 slots (satellite io + 2 chip slots for copy)
    public ItemStackHandler inventory = new ItemStackHandler(4) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == satellite_input_slot)
                return stack.getItem() instanceof ItemSatellite;
            if (slot == chip_slot_2 || slot == chip_main_slot)
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
