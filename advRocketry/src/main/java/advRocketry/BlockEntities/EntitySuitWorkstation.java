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
    public static int JETPACK_SLOT = 4;

    public GuiHandlerBlockEntity guiHandler = new GuiHandlerBlockEntity(this);
    public ItemStackHandler chestInventory;
    public ItemStackHandler bootsInventory;
    public ItemStackHandler helmetInventory;
    public ItemStackHandler legsInventory;
    public ItemStackHandler jetpackInventory;

    public ItemStackHandler inventory = new ItemStackHandler(5) {

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
            if (slot == JETPACK_SLOT)
                return stack.getItem() instanceof Jetpack;
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
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, HELMET_SLOT, 0, 1, guiHandler, 25, 10));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, CHEST_SLOT, 0, 1, guiHandler, 25, 30));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, JETPACK_SLOT, 0, 1, guiHandler, 25, 50));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, LEGS_SLOT, 0, 1, guiHandler, 25, 70));
        guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, inventory, BOOTS_SLOT, 0, 1, guiHandler, 25, 90));

        // armor itemstack render
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_SPACE_SUIT_HELMET.get(), 1), 0.9f, guiHandler, 5,10));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_SPACE_SUIT_CHESTPLATE.get(), 1), 0.9f, guiHandler, 5,30));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_JETPACK.get(), 1), 0.9f, guiHandler, 5,50));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_SPACE_SUIT_LEGGINGS.get(), 1), 0.9f, guiHandler, 5,70));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_SPACE_SUIT_BOOTS.get(), 1), 0.9f, guiHandler, 5,90));

        // inventory helper itemstack render
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_NIGHTVISION_UPGRADE.get(), 1), 0.9f, guiHandler, 90,11));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_JETPACK.get(), 1), 0.9f, guiHandler, 90,31));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_PORTABLE_PRESSURE_TANK_ALUMINUM.get(), 2), 0.9f, guiHandler, 105,31));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_PORTABLE_PRESSURE_TANK_ALUMINUM.get(), 2), 0.9f, guiHandler, 90,51));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_LEGS_UPGRADE.get(), 1), 0.9f, guiHandler, 90,71));
        guiHandler.modules.add(new guiModuleItemStackRender(id++, new ItemStack(Items.ITEM_GRAVITYBOOTS_UPGRADE.get(), 1), 0.9f, guiHandler, 90,91));

        ProxyItemHandler helmetHandler = new ProxyItemHandler(Items.ITEM_SPACE_SUIT_HELMET.get().getInventorySlots()) {
            @Override
            public IItemHandler getItemHandler() {
                return helmetInventory;
            }
            @Override
            public void onContentsMaybeChanged() {
                saveSuit();
            }
        };
        for (int x = 0; x < helmetHandler.getSlots(); x++) {
            guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, helmetHandler, x, 0, 1, guiHandler, 50+20*x, 10));
        }

        ProxyItemHandler chestPlateHandler = new ProxyItemHandler(Items.ITEM_SPACE_SUIT_CHESTPLATE.get().getInventorySlots()) {
            @Override
            public IItemHandler getItemHandler() {
                return chestInventory;
            }
            @Override
            public void onContentsMaybeChanged() {
                saveSuit();
            }
        };
        for (int x = 0; x < chestPlateHandler.getSlots(); x++) {
            guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, chestPlateHandler, x, 0, 1, guiHandler, 50+20*x, 30));
        }

        ProxyItemHandler jetpackHandler = new ProxyItemHandler(Items.ITEM_JETPACK.get().getInventorySlots()) {
            @Override
            public IItemHandler getItemHandler() {
                return jetpackInventory;
            }
            @Override
            public void onContentsMaybeChanged() {
                saveSuit();
            }
        };
        for (int x = 0; x < jetpackHandler.getSlots(); x++) {
            guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, jetpackHandler, x, 0, 1, guiHandler, 50+20*x, 50));
        }

        ProxyItemHandler legsHandler = new ProxyItemHandler(Items.ITEM_SPACE_SUIT_LEGGINGS.get().getInventorySlots()) {
            @Override
            public IItemHandler getItemHandler() {
                return legsInventory;
            }
            @Override
            public void onContentsMaybeChanged() {
                saveSuit();
            }
        };
        for (int x = 0; x < legsHandler.getSlots(); x++) {
            guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, legsHandler, x, 0, 1, guiHandler, 50+20*x, 70));
        }

        ProxyItemHandler bootsHandler = new ProxyItemHandler(Items.ITEM_SPACE_SUIT_BOOTS.get().getInventorySlots()) {
            @Override
            public IItemHandler getItemHandler() {
                return bootsInventory;
            }
            @Override
            public void onContentsMaybeChanged() {
                saveSuit();
            }
        };
        for (int x = 0; x < bootsHandler.getSlots(); x++) {
            guiHandler.modules.add(new guiModuleItemHandlerSlot(id++, bootsHandler, x, 0, 1, guiHandler, 50+20*x, 90));
        }

        // player inventory
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 170, 100, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 110, 200, 1, 0, guiHandler));

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySuitWorkstation) t).tick();
    }

    void saveSuit() {
        ISpaceSuitInventory.saveInventory(helmetInventory, inventory.getStackInSlot(HELMET_SLOT), level.registryAccess());
        ISpaceSuitInventory.saveInventory(chestInventory, inventory.getStackInSlot(CHEST_SLOT), level.registryAccess());
        ISpaceSuitInventory.saveInventory(jetpackInventory, inventory.getStackInSlot(JETPACK_SLOT), level.registryAccess());
        ISpaceSuitInventory.saveInventory(legsInventory, inventory.getStackInSlot(LEGS_SLOT), level.registryAccess());
        ISpaceSuitInventory.saveInventory(bootsInventory, inventory.getStackInSlot(BOOTS_SLOT), level.registryAccess());

        loadSuit();
        setChanged();
    }

    void loadSuit() {
        helmetInventory = ISpaceSuitInventory.loadInventory(inventory.getStackInSlot(HELMET_SLOT), level.registryAccess());
        chestInventory = ISpaceSuitInventory.loadInventory(inventory.getStackInSlot(CHEST_SLOT), level.registryAccess());
        jetpackInventory = ISpaceSuitInventory.loadInventory(inventory.getStackInSlot(JETPACK_SLOT), level.registryAccess());
        legsInventory = ISpaceSuitInventory.loadInventory(inventory.getStackInSlot(LEGS_SLOT), level.registryAccess());
        bootsInventory = ISpaceSuitInventory.loadInventory(inventory.getStackInSlot(BOOTS_SLOT), level.registryAccess());
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
