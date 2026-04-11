package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import advRocketry.Items.ItemSatellite;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Registry.Items;
import advRocketry.SpaceSuit.*;
import advRocketry.Utils.ItemUtils;
import advRocketry.Utils.ProxyItemHandler;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import static advRocketry.Registry.BlockEntities.ENTITY_SUIT_WORKSTATION;

public class EntitySuitWorkstation extends BlockEntity implements INetworkTagReceiver {

    public static int HELMET_SLOT = 0;
    public static int CHEST_SLOT = 1;
    public static int LEGS_SLOT = 2;
    public static int BOOTS_SLOT = 3;

    public GuiHandlerBlockEntity guiHandler = new GuiHandlerBlockEntity(this);
    public ItemStackHandler chestInventory;

    public ItemStackHandler inventory = new ItemStackHandler(4) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == HELMET_SLOT)
                return stack.getItem() instanceof Helmet;
            if (slot == CHEST_SLOT)
                return stack.getItem() instanceof ChestPlate;
            if (slot == LEGS_SLOT)
                return stack.getItem() instanceof Leggings;
            if (slot == BOOTS_SLOT)
                return stack.getItem() instanceof Boots;
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public void onContentsChanged(int slot) {
            loadSuit();
            setChanged();
        }
    };

    public EntitySuitWorkstation(BlockPos pos, BlockState blockState) {
        super(ENTITY_SUIT_WORKSTATION.get(), pos, blockState);

        int id = 0;

        // inventory slots
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, HELMET_SLOT, 0, 1, guiHandler, 20, 20));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, CHEST_SLOT, 0, 1, guiHandler, 20, 40));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, LEGS_SLOT, 0, 1, guiHandler, 20, 60));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, BOOTS_SLOT, 0, 1, guiHandler, 20, 80));

        ProxyItemHandler chestPlanetHandler = new ProxyItemHandler(Items.ITEM_SPACE_SUIT_CHESTPLATE.get().getInventorySlots()) {
            @Override
            public IItemHandler getItemHandler() {
                return chestInventory;
            }
            @Override
            public void onContentsMaybeChanged() {
                saveSuit();
            }
        };
        for (int x = 0; x < chestPlanetHandler.getSlots(); x++) {
            guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, chestPlanetHandler, x, 0, 1, guiHandler, 50, 40));
        }

        // player inventory
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 170, 100, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 110, 200, 1, 0, guiHandler));

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySuitWorkstation) t).tick();
    }

    void saveSuit() {
        SpaceSuit.saveInventory(chestInventory, inventory.getStackInSlot(CHEST_SLOT), level.registryAccess());
        loadSuit();
        setChanged();
    }

    void loadSuit() {
        chestInventory = SpaceSuit.loadInventory(inventory.getStackInSlot(CHEST_SLOT), level.registryAccess());
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

    @Override
    public void onLoad() {
        super.onLoad();
        loadSuit();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {guiHandler.readServer(compoundTag);}

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
            guiHandler.openGui(178, 198, true);
        }
    }
}
