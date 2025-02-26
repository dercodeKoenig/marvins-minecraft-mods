package ResearchSystem.ResearchStation;

import ARLib.gui.GuiHandlerMainHandItem;
import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.network.PacketPlayerMainHand;
import ARLib.utils.DimensionUtils;
import ARLib.utils.InventoryUtils;
import ARLib.utils.ItemUtils;
import ARLib.utils.RecipePart;
import ResearchSystem.Config.RecipeConfig;
import ResearchSystem.Config.ResearchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemResearchBook extends Item implements INetworkTagReceiver {

    ItemStack client_currentBookStackOpen = ItemStack.EMPTY;

    GuiHandlerMainHandItem guiHandler = new GuiHandlerMainHandItem() {
        @Override
        public void onGuiClose() {
            // this happens on client side
            if (client_currentBookStackOpen.getItem() instanceof ItemResearchBook irb) {
                CompoundTag t = getStackTagOrEmpty(client_currentBookStackOpen);
                if (t.getBoolean("isInStation")) {
                    // re-open the gui from the station
                    BlockPos pos = new BlockPos(t.getInt("sx"), t.getInt("sy"), t.getInt("sz"));
                    BlockEntity station = Minecraft.getInstance().player.level().getBlockEntity(pos);
                    if (station instanceof EntityResearchStation r) {
                        r.openGui();
                    }
                }
            }
        }

        @Override
        public void onGuiClientTick(Player p) {
            if (client_currentBookStackOpen.getItem() instanceof ItemResearchBook irb) {
                CompoundTag t = getStackTagOrEmpty(client_currentBookStackOpen);
                if (!t.getBoolean("isInStation")) {
                    // if the item is in main hand, check if the stack changed to update gui
                    ItemStack stackInHand = p.getMainHandItem();
                    if (!ItemStack.isSameItemSameComponents(client_currentBookStackOpen, stackInHand)) {
                        // probably updated nbt so update gui
                        // and reset client_currentBookStackOpen to detect future changes.
                        // it appears that the .copy() is not required. maybe because it creates a new object on sync
                        // but i just add it because we should not compare the same object reference and using .copy() makes it more clear
                        client_currentBookStackOpen = stackInHand.copy();
                        makeGui(stackInHand);
                    }
                } else {
                    BlockPos pos = new BlockPos(t.getInt("sx"), t.getInt("sy"), t.getInt("sz"));
                    BlockEntity station = p.level().getBlockEntity(pos);
                    if (station instanceof EntityResearchStation r) {
                        // this will ping the server to notify that i am still tracking the gui.
                        // i am not really tracking the gui but as long as the server thinks i am tracking the gui it will
                        // update the book stack nbt
                        r.guiHandler.onGuiClientTick(p);
                    }
                }
            }
        }
    };

    public ItemResearchBook() {
        super(new Properties().stacksTo(1));
        //makeGui();
    }

    public void makeGui(ItemStack bookStack) {

        guiHandler.getModules().clear();
        guiModuleImage i1 = new guiModuleImage(guiHandler, 0, 0, 190, 200, ResourceLocation.fromNamespaceAndPath("research_station", "textures/gui/book.png"), 148, 180);
        guiHandler.getModules().add(i1);
        guiModuleImage i2 = new guiModuleImage(guiHandler, 190, 0, 190, 200, ResourceLocation.fromNamespaceAndPath("research_station", "textures/gui/book.png"), 148, 180);
        guiHandler.getModules().add(i2);

        List<String> researchInQueue = getQueuedResearches_readOnly(bookStack);
        List<String> researchCompleted = getCompletedResearches_readOnly(bookStack);

        List<GuiModuleBase> researchList = new ArrayList<>();
        for (int n = 0; n < ResearchConfig.INSTANCE.researchList.size(); n++) {
            ResearchConfig.Research i = ResearchConfig.INSTANCE.researchList.get(n);

            String name = i.id;
            int y = 14 * n + 2;
            guiModuleText t = new guiModuleText(10000 + n, name, guiHandler, 2, y + 3, 0xFF000000, false);
            researchList.add(t);

            guiModuleButton db = new guiModuleButton(20000 + n, "?", guiHandler, 140, y, 12, 12, ResourceLocation.fromNamespaceAndPath("research_station", "textures/gui/btn.png"), 10, 10) {
                @Override
                public void onButtonClicked() {
                    client_updateResearchPreview(bookStack, i.id);
                }
            };

            db.color = 0xFFFFA0A0;
            if (researchInQueue.contains(i.id) || i.id.equals(getCurrentResearch(bookStack))) {
                db.color = 0xFFF0F080;
            }
            if (researchCompleted.contains(i.id)) {
                db.color = 0xFFA0FFA0;
            }
            researchList.add(db);
        }

        guiModuleScrollContainer c = new guiModuleScrollContainer(researchList, 0x00000000, guiHandler, 18, 7, 173, 183);
        guiHandler.getModules().add(c);

        List<GuiModuleBase> infoContainerModules = new ArrayList<>();

        String previewId = getSelectedResearchPreview(bookStack);
        if (!previewId.isEmpty()) {
            ResearchConfig.Research selected = ResearchConfig.INSTANCE.getResearchMap().get(previewId);
            if (selected == null) {
                // can happen when you change config
            } else {
                //title
                infoContainerModules.add(
                        new guiModuleText(-1, previewId, guiHandler, 5, 10, 0xFF000000, false)
                );


                //unlocked items
                infoContainerModules.add(
                        new guiModuleText(-2, "Unlocked Items:", guiHandler, 5, 25, 0xFF000000, false)
                );

                int itemsInRow = 5;
                int baseX = 5;
                int baseY = 35;
                int n = 0;
                for (RecipeConfig.Recipe i : RecipeConfig.INSTANCE.recipeList) {
                    int x = n % itemsInRow * 18 + baseX;
                    int y = n / itemsInRow * 18 + baseY;
                    if (i.requiredResearch.equals(previewId)) {
                        infoContainerModules.add(
                                new guiModuleItemPreview(guiHandler, x, y, ItemUtils.getItemStackFromIdOrTag(i.output.id, 1, Minecraft.getInstance().player.level().registryAccess()))
                        );
                        n++;
                    }
                }

                // requirements
                int ty = 30 + n / itemsInRow * 18 + baseY;
                infoContainerModules.add(
                        new guiModuleText(-2, "requirements:", guiHandler, 5, ty, 0xFF000000, false)
                );
                ty += 10;

                n = 0;
                for (RecipePart i : selected.requiredItems) {
                    int x = n % itemsInRow * 18 + baseX;
                    int y = n / itemsInRow * 18 + ty;
                    infoContainerModules.add(
                            new guiModuleItemPreview(guiHandler, x, y, ItemUtils.getItemStackFromIdOrTag(i.id, i.amount, Minecraft.getInstance().player.level().registryAccess()))
                    );

                    n++;
                }
                ty += (n / itemsInRow+1) * 18;

                for (String r : selected.requiredResearches) {
                    infoContainerModules.add(
                            new guiModuleText(-ty, r, guiHandler, 5, ty + 3, 0xFF000000, false)
                    );
                    guiModuleButton db = new guiModuleButton(-20000 - ty, "?", guiHandler, 140, ty, 12, 12, ResourceLocation.fromNamespaceAndPath("research_station", "textures/gui/btn.png"), 10, 10) {
                        @Override
                        public void onButtonClicked() {
                            client_updateResearchPreview(bookStack, r);
                        }
                    };

                    db.color = 0xFFFFA0A0;
                    if (researchInQueue.contains(r) || r.equals(getCurrentResearch(bookStack))) {
                        db.color = 0xFFF0F080;
                    }
                    if (researchCompleted.contains(r)) {
                        db.color = 0xFFA0FFA0;
                    }
                    infoContainerModules.add(db);

                    ty += 10;
                }
            }
        }
        guiModuleScrollContainer infoContainer = new guiModuleScrollContainer(infoContainerModules, 0x00000000, guiHandler, 190 + 18, 7, 173, 183);
        guiHandler.getModules().add(infoContainer);

        if (guiHandler.screen instanceof ModularScreen m)
            m.calculateGuiOffsetAndNotifyModules();
    }

    public void openGui(ItemStack bookStack) {
        makeGui(bookStack);
        this.client_currentBookStackOpen = bookStack;
        guiHandler.openGui(380, 200, false);
    }

    public CompoundTag getStackTagOrEmpty(ItemStack stack) {
        try {
            return stack.get(DataComponents.CUSTOM_DATA).copyTag();
        } catch (Exception e) {
            CompoundTag itemTag = new CompoundTag();
            ListTag completed = new ListTag();
            itemTag.put("completed", completed);
            ListTag queued = new ListTag();
            itemTag.put("queued", queued);

            itemTag.putString("currentResearch", "");
            itemTag.putInt("currentProgress", 0);

            itemTag.putString("selectedResearchPreview", "");

            // this is required to set the selected research.
            // if the book is in station the update packet to server needs to target the station
            // if not in station, player holds it in hand and it needs to target the item in player hand
            itemTag.putBoolean("isInStation", false);
            itemTag.put("slevelId", StringTag.valueOf(""));
            itemTag.put("sx", IntTag.valueOf(0));
            itemTag.put("sy", IntTag.valueOf(0));
            itemTag.put("sz", IntTag.valueOf(0));


            return itemTag;
        }
    }

    public void setStackTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }


    public void setIsInStation(ItemStack bookstack, @Nullable BlockEntity station) {
        CompoundTag itemTag = getStackTagOrEmpty(bookstack);
        if (station == null) {
            itemTag.putBoolean("isInStation", false);
        } else {
            itemTag.putBoolean("isInStation", true);
            itemTag.putString("slevelId", DimensionUtils.getLevelId(station.getLevel()));
            itemTag.putInt("sx", station.getBlockPos().getX());
            itemTag.putInt("sy", station.getBlockPos().getY());
            itemTag.putInt("sz", station.getBlockPos().getZ());
        }
        setStackTag(bookstack, itemTag);
    }

    public String getSelectedResearchPreview(CompoundTag itemTag) {
        return itemTag.getString("selectedResearchPreview");
    }

    public String getSelectedResearchPreview(ItemStack stack) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        return getSelectedResearchPreview(itemTag);
    }

    public void setSelectedResearchPreview(ItemStack stack, String researchId) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        itemTag.putString("selectedResearchPreview", researchId);
        setStackTag(stack, itemTag);
    }

    public String getCurrentResearch(CompoundTag itemTag) {
        return itemTag.getString("currentResearch");
    }

    public String getCurrentResearch(ItemStack stack) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        return getCurrentResearch(itemTag);
    }

    public void setCurrentResearch(ItemStack stack, String researchId) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        itemTag.putString("currentResearch", researchId);
        setStackTag(stack, itemTag);
    }

    public int getCurrentProgress(CompoundTag itemTag) {
        return itemTag.getInt("currentProgress");
    }

    public int getCurrentProgress(ItemStack stack) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        return getCurrentProgress(itemTag);
    }

    public void setCurrentProgress(ItemStack stack, int progress) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        itemTag.putInt("currentProgress", progress);
        setStackTag(stack, itemTag);
    }

    public List<String> getCompletedResearches_readOnly(CompoundTag itemTag) {
        List<String> completedStringList = new ArrayList<>();
        ListTag completedResearchesT = itemTag.getList("completed", Tag.TAG_STRING);
        for (int i = 0; i < completedResearchesT.size(); i++) {
            completedStringList.add(completedResearchesT.getString(i));
        }
        return completedStringList;
    }

    public List<String> getCompletedResearches_readOnly(ItemStack stack) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        return getCompletedResearches_readOnly(itemTag);
    }

    public void setCompletedResearches(ItemStack stack, List<String> completedResearches) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        ListTag t = new ListTag();
        for (String i : completedResearches) {
            t.add(StringTag.valueOf(i));
        }
        itemTag.put("completed", t);
        setStackTag(stack, itemTag);
    }

    public List<String> getQueuedResearches_readOnly(CompoundTag itemTag) {
        List<String> queuedStringList = new ArrayList<>();
        ListTag queuedResearchesT = itemTag.getList("queued", Tag.TAG_STRING);
        for (int i = 0; i < queuedResearchesT.size(); i++) {
            queuedStringList.add(queuedResearchesT.getString(i));
        }
        return queuedStringList;
    }

    public List<String> getQueuedResearches_readOnly(ItemStack stack) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        return getQueuedResearches_readOnly(itemTag);
    }

    public void setQueuedResearches(ItemStack stack, List<String> queuedResearches) {
        CompoundTag itemTag = getStackTagOrEmpty(stack);
        ListTag t = new ListTag();
        for (String i : queuedResearches) {
            t.add(StringTag.valueOf(i));
        }
        itemTag.put("queued", t);
        setStackTag(stack, itemTag);

        removeInvalidQueuedResearches(stack);
    }

    public void removeInvalidQueuedResearches(ItemStack stack) {
        List<String> queuedResearches = getQueuedResearches_readOnly(stack);
        List<String> completedResearches = getCompletedResearches_readOnly(stack);
        if (!getCurrentResearch(stack).isEmpty())
            // if a research is in progress, assume it is completed to step to the next one
            completedResearches.add(getCurrentResearch(stack));

        for (int i = 0; i < queuedResearches.size(); i++) {
            String name = queuedResearches.get(i);
            ResearchConfig.Research r = ResearchConfig.INSTANCE.getResearchMap().get(name);

            if (!completedResearches.containsAll(r.requiredResearches)) {
                queuedResearches.remove(name);
                setQueuedResearches(stack, queuedResearches);
                removeInvalidQueuedResearches(stack);
                return;
            } else {
                // if this queued research is valid, assume it is completed to step to the next one
                completedResearches.add(name);
            }
        }
    }

    public boolean tryCompleteResearch(ItemStack stack) {
        String researchId = getCurrentResearch(stack);
        List<String> queuedResearches = getQueuedResearches_readOnly(stack);
        queuedResearches.remove(researchId);
        setQueuedResearches(stack, queuedResearches);

        List<String> completedResearches = getCompletedResearches_readOnly(stack);


        ResearchConfig.Research r = ResearchConfig.INSTANCE.getResearchMap().get(researchId);
        boolean hasAllRequired = completedResearches.containsAll(r.requiredResearches);
        setCurrentResearch(stack, "");
        setCurrentProgress(stack, 0);
        if (hasAllRequired) {
            completedResearches.add(researchId);
            setCompletedResearches(stack, completedResearches);
            return true;
        }
        return false;
    }

    public void startResearchIfPossibleAndConsumeElements(ItemStack stack, IItemHandler inventory) {
        if (getCurrentResearch(stack).isEmpty()) {
            List<String> queued = getQueuedResearches_readOnly(stack);
            if (!queued.isEmpty()) {
                String first = queued.removeFirst();
                ResearchConfig.Research i = ResearchConfig.INSTANCE.getResearchMap().get(first);
                if (i == null) {
                    setCurrentResearch(stack, "");
                    setQueuedResearches(stack, queued);
                    return; // cam happen if config was changed
                }
                if (InventoryUtils.hasInputs(List.of(inventory), new ArrayList<>(), i.requiredItems)) {
                    for (RecipePart p : i.requiredItems) {
                        InventoryUtils.consumeElements(new ArrayList<>(), List.of(inventory), p.id, p.amount, false);
                    }
                    setCurrentResearch(stack, first);
                    setCurrentProgress(stack, 0);
                    setQueuedResearches(stack, queued);
                }
            }
        }
    }

    public List<RecipePart> getRequiredItemsForResearch(ItemStack stack) {
        if (getCurrentResearch(stack).isEmpty()) {
            List<String> queued = getQueuedResearches_readOnly(stack);
            if (!queued.isEmpty()) {
                String first = queued.getFirst();
                ResearchConfig.Research c = ResearchConfig.INSTANCE.getResearchMap().get(first);
                if (c != null) // can happen if config was changes. dont want it to crash the game
                    return c.requiredItems;
                else return List.of();
            }
        }
        return List.of();
    }

    public void tickResearch(ItemStack stack, int increment) {
        String currentResearch = getCurrentResearch(stack);
        if (!currentResearch.isEmpty()) {
            int progress = getCurrentProgress(stack);
            progress += increment;
            setCurrentProgress(stack, progress);
            ResearchConfig.Research i = ResearchConfig.INSTANCE.getResearchMap().get(currentResearch);
            if (i == null)
                // can happen if config was changed
                setCurrentResearch(stack, "");
            else if (progress >= i.ticksRequired) {
                tryCompleteResearch(stack);
            }
        }
    }

    public List<ResearchConfig.Research> getAvailableResearches(ItemStack stack) {
        List<ResearchConfig.Research> availableResearch = new ArrayList<>();
        List<String> completedAndQueuedResearches = new ArrayList<>();
        completedAndQueuedResearches.addAll(getCompletedResearches_readOnly(stack));
        completedAndQueuedResearches.addAll(getQueuedResearches_readOnly(stack));
        if (!getCurrentResearch(stack).isEmpty())
            // assume it will be completed next
            completedAndQueuedResearches.add(getCurrentResearch(stack));

        for (ResearchConfig.Research r : ResearchConfig.INSTANCE.researchList) {
            if (completedAndQueuedResearches.containsAll(r.requiredResearches) && !completedAndQueuedResearches.contains(r.id) && !r.id.equals(getCurrentResearch(stack))) {
                // return all except already completed or already in queue or currently researched
                availableResearch.add(r);
            }
        }

        return availableResearch;
    }


    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);

        if (level.isClientSide && itemstack.getItem() instanceof ItemResearchBook) {
            openGui(itemstack);
        } else {
            setIsInStation(itemstack, null);
        }

        return InteractionResultHolder.success(itemstack);
    }

    public void client_updateResearchPreview(ItemStack bookStack, String id) {
        CompoundTag info = new CompoundTag();
        info.putString("setPreviewResearch", id);
        CustomPacketPayload p = null;
        CompoundTag t = getStackTagOrEmpty(bookStack);
        //System.out.println(t);
        if (t.getBoolean("isInStation")) {
            BlockPos pos = new BlockPos(t.getInt("sx"), t.getInt("sy"), t.getInt("sz"));
            p = PacketBlockEntity.getBlockEntityPacket(Minecraft.getInstance().player.level(), pos, info);
        } else {
            p = new PacketPlayerMainHand(info);
        }
        PacketDistributor.sendToServer(p);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        if (compoundTag.contains("setPreviewResearch")) {
            String id = compoundTag.getString("setPreviewResearch");
            ItemStack itemStack = p.getInventory().getSelected();
            setSelectedResearchPreview(itemStack, id);
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {

    }
}
