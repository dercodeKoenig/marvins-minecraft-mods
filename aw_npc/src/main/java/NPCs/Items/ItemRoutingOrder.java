package NPCs.Items;

import ARLib.gui.GuiHandlerMainHandItem;
import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import NPCs.Utils;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ItemRoutingOrder extends Item implements INetworkTagReceiver, guiModuleItemGuiItemstackFakeSlot.StackBasedItemHandler {

    GuiHandlerMainHandItem guiHandler;
    ItemStack lastStackInHand = ItemStack.EMPTY;

    public ItemRoutingOrder() {
        super(new Properties());
        guiHandler = new GuiHandlerMainHandItem() {
            public void onGuiClientTick(Player p) {
                ItemStack stackInHand = p.getMainHandItem();
                if (!ItemStack.isSameItemSameComponents(lastStackInHand, stackInHand)) {
                    lastStackInHand = stackInHand;
                    makeGui(stackInHand, p);
                }
            }
        };
    }
    public void makeGui(ItemStack stack, Player p) {
        guiHandler.getModules().clear();
        List<RoutingEntry> entries = getRoutingEntries(stack,p.level().registryAccess());

        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10,170,10000,2,0,guiHandler)){
            if(i.targetSlot != p.getInventory().selected)
                guiHandler.getModules().add(i);
        }
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10,110,20000,2,0,guiHandler)){
            guiHandler.getModules().add(i);
        }

        guiModuleScrollContainer container = new guiModuleScrollContainer(new ArrayList<>(),0x00ffffff,guiHandler,7,10,166,100);
        guiHandler.getModules().add(container);

        int y = 0;
        int id = 0;
        int slotId = 0;
        for(RoutingEntry entry : entries){
            guiModuleText info = new guiModuleText(id++, ("Position: "+entry.posX+","+entry.posY+","+entry.posZ+" : "+Direction.values()[entry.facingOrdinal]),guiHandler,0,y,0x00000000,false);
            container.modules.add(info);
            y+=10;
            for (int i = 0; i < 9; i++) {
                int x = i * 18;
                guiModuleItemGuiItemstackFakeSlot slot = new guiModuleItemGuiItemstackFakeSlot(this, slotId++,id++,guiHandler,5,6,x,y);
                container.modules.add(slot);
            }
            y += 20;
        }

        if(guiHandler.screen instanceof ModularScreen m)
            m.calculateGuiOffsetAndNotifyModules();
    }

    public void openGui(ItemStack bookStack, Player p) {
        makeGui(bookStack, p);
        lastStackInHand = bookStack.copy();
        guiHandler.openGui(180, 200, true);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (level.isClientSide && itemstack.getItem() instanceof ItemRoutingOrder) {
            openGui(itemstack, player);
        }
        return InteractionResultHolder.success(itemstack);
    }


    public static List<RoutingEntry> getRoutingEntries(ItemStack stack, RegistryAccess registry) {
        List<RoutingEntry> entries = new ArrayList<>();
        CompoundTag stackTag = Utils.getStackTagOrEmpty(stack);
        if (stackTag.contains("routingEntries")) {
            ListTag entriesTag = stackTag.getList("routingEntries", Tag.TAG_COMPOUND);
            for (int i = 0; i < entriesTag.size(); i++) {
                CompoundTag entryTag = entriesTag.getCompound(i);
                RoutingEntry e = new RoutingEntry();
                e.posX = entryTag.getInt("posX");
                e.posY = entryTag.getInt("posY");
                e.posZ = entryTag.getInt("posZ");
                e.facingOrdinal = entryTag.getInt("facingOrdinal");
                e.mode = entryTag.getInt("mode");
                e.durabilityPercentFilter = entryTag.getInt("durabilityPercentFilter");
                e.durability_needsToBeAboveFilter = entryTag.getBoolean("durability_needsToBeAboveFilter");
                e.filterInventory.deserializeNBT(registry, entryTag.getCompound("filterInv"));
                entries.add(e);
            }
        }
        return entries;
    }

    public static void setRoutingEntries(ItemStack stack, List<RoutingEntry> entries, RegistryAccess registry) {
        CompoundTag stackTag = Utils.getStackTagOrEmpty(stack);
        ListTag entriesTag = new ListTag();
        for (RoutingEntry e : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("posX", e.posX);
            entryTag.putInt("posY", e.posY);
            entryTag.putInt("posZ", e.posZ);
            entryTag.putInt("facingOrdinal", e.facingOrdinal);
            entryTag.putInt("mode", e.mode);
            entryTag.putInt("durabilityPercentFilter", e.durabilityPercentFilter);
            entryTag.putBoolean("durability_needsToBeAboveFilter", e.durability_needsToBeAboveFilter);
            entryTag.put("filterInv", e.filterInventory.serializeNBT(registry));
            entriesTag.add(entryTag);
        }
        stackTag.put("routingEntries", entriesTag);
        Utils.setStackTag(stack, stackTag);
    }

    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            List<RoutingEntry> entries = getRoutingEntries(context.getItemInHand(), context.getLevel().registryAccess());
            RoutingEntry e = new RoutingEntry();
            e.facingOrdinal = context.getClickedFace().ordinal();
            e.posX = context.getClickedPos().getX();
            e.posY = context.getClickedPos().getY();
            e.posZ = context.getClickedPos().getZ();
            entries.add(e);
            setRoutingEntries(context.getItemInHand(), entries, context.getLevel().registryAccess());
            if (context.getPlayer() != null)
                context.getPlayer().sendSystemMessage(Component.literal("position set to " + context.getClickedPos() + ":" + context.getClickedFace()));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
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
    public ItemStack getStackInSlot(ItemStack stack, int slot, RegistryAccess registry) {
        List<RoutingEntry> entries = getRoutingEntries(stack, registry);
        int targetModuleIndex = slot / 9;
        int targetSlot = slot % 9;
        if(targetModuleIndex >= entries.size())
            // this can happen if the user no longer holds the gui item in main hand
            return ItemStack.EMPTY;
        RoutingEntry entry = entries.get(targetModuleIndex);
        return entry.filterInventory.getStackInSlot(targetSlot);
    }

    @Override
    public ItemStack insertItem(ItemStack stack, int slot, ItemStack stackToInsert, boolean simulate, RegistryAccess registry) {
        List<RoutingEntry> entries = getRoutingEntries(stack,registry);
        int targetModuleIndex = slot / 9;
        int targetSlot = slot % 9;
        if(targetModuleIndex >= entries.size())
            // this can happen if the user no longer holds the gui item in main hand
            return stack;
        RoutingEntry entry = entries.get(targetModuleIndex);
        ItemStack insert = entry.filterInventory.insertItem(targetSlot,stackToInsert,simulate);
        if(!simulate){
            setRoutingEntries(stack, entries, registry);
        }
        return  insert;
    }

    @Override
    public ItemStack extractItem(ItemStack stack, int slot, int amount, boolean simulate, RegistryAccess registry) {
        List<RoutingEntry> entries = getRoutingEntries(stack,registry);
        int targetModuleIndex = slot / 9;
        int targetSlot = slot % 9;
        if(targetModuleIndex >= entries.size())
            // this can happen if the user no longer holds the gui item in main hand
            return ItemStack.EMPTY;
        RoutingEntry entry = entries.get(targetModuleIndex);
        ItemStack extracted = entry.filterInventory.extractItem(targetSlot,amount,simulate);
        if(!simulate){
            setRoutingEntries(stack, entries, registry);
        }
        return  extracted;
    }

    @Override
    public int getSlotLimit(ItemStack stack, int slot, RegistryAccess registry) {
        List<RoutingEntry> entries = getRoutingEntries(stack,registry);
        int targetModuleIndex = slot / 9;
        int targetSlot = slot % 9;
        if(targetModuleIndex >= entries.size())
            // this can happen if the user no longer holds the gui item in main hand
            return 0;
        RoutingEntry entry = entries.get(targetModuleIndex);
        return entry.filterInventory.getSlotLimit(targetSlot);
    }
}
