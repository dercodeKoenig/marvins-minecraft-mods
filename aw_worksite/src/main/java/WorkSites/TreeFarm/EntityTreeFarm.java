package WorkSites.TreeFarm;

import ARLib.gui.modules.*;
import ARLib.utils.InventoryUtils;
import WorkSites.Config;
import WorkSites.EntityWorkSiteBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

import static WorkSites.Registry.ENTITY_TREE_FARM;

public class EntityTreeFarm extends EntityWorkSiteBase {

    public static Set<BlockPos> knownTreeFarms = new HashSet<>();

    public int energy_plant = Config.INSTANCE.tree_farm_energy_plant;
    public int energy_harvest_leaves = Config.INSTANCE.tree_farm_energy_harvest_leaves;
    public int energy_harvest_logs = Config.INSTANCE.tree_farm_energy_harvest_logs;
    public int energy_boneMeal = Config.INSTANCE.tree_farm_energy_boneMeal;

    public int useWoodmillsInRadius = 32;
    guiModuleTextInput useWoodmillsInRadiusTextInput;

    double maxEnergy = 0;

    public ItemStackHandler mainInventory = new ItemStackHandler(18) {
        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    public ItemStackHandler inputsInventory = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemValidSapling(stack) || stack.getItem() instanceof AxeItem;
        }

        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    public ItemStackHandler specialResourcesInventory = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.getItem().equals(Items.BONE_MEAL) || stack.getItem().equals(Items.SHEARS) || stack.getItem() instanceof AxeItem)
                return true;
            else return false;
        }

        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    //public Set<BlockPos> positionsToTill = new HashSet<>();
    public Set<BlockPos> positionsToPlant = new HashSet<>();
    public Set<BlockPos> positionsToHarvest_Leaves = new HashSet<>();
    public Set<BlockPos> positionsToHarvest_Logs = new HashSet<>();
    public Set<BlockPos> positionsToBoneMeal = new HashSet<>();
    int currentBlockToScanIndex = 0;

    public EntityTreeFarm(BlockPos pos, BlockState blockState) {
        super(ENTITY_TREE_FARM.get(), pos, blockState);

        maxEnergy = Math.max(maxEnergy,energy_plant);
        maxEnergy = Math.max(maxEnergy,energy_boneMeal);
        maxEnergy = Math.max(maxEnergy,energy_harvest_leaves);
        maxEnergy = Math.max(maxEnergy,energy_harvest_logs);

        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 210, 500, 0, 1, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }
        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 150, 600, 0, 1, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }


        guiModuleText t1 = new guiModuleText(11001, "Output", guiHandlerMain, 10, 30, 0xff000000, false);
        guiHandlerMain.getModules().add(t1);
        guiModuleText t2 = new guiModuleText(11002, "Resources", guiHandlerMain, 10, 78, 0xff000000, false);
        guiHandlerMain.getModules().add(t2);
        guiModuleText t3 = new guiModuleText(11003, "Special Resources", guiHandlerMain, 10, 110, 0xff000000, false);
        guiHandlerMain.getModules().add(t3);

        for (int i = 0; i < inputsInventory.getSlots(); i++) {
            int x = i * 18 + 10;
            int y = 89;
            guiModuleItemHandlerSlot s = new guiModuleItemHandlerSlot(900 + i, inputsInventory, i, 1, 0, guiHandlerMain, x, y);
            guiHandlerMain.getModules().add(s);
        }

        for (int i = 0; i < specialResourcesInventory.getSlots(); i++) {
            int x = i * 18 + 10;
            int y = 120;
            guiModuleItemHandlerSlot s = new guiModuleItemHandlerSlot(1000 + i, specialResourcesInventory, i, 1, 0, guiHandlerMain, x, y);
            guiHandlerMain.getModules().add(s);
        }

        int w = 9;
        for (int i = 0; i < mainInventory.getSlots(); i++) {
            int x = i % w * 18 + 10;
            int y = i / w * 18 + 40;
            guiModuleItemHandlerSlot s = new guiModuleItemHandlerSlot(800 + i, mainInventory, i, 1, 0, guiHandlerMain, x, y);
            guiHandlerMain.getModules().add(s);
        }

        guiModuleText useWoodmillsInRadiusText = new guiModuleText(-1, "use woodmills: r=", guiHandlerMain, 60, 13, 0xff000000, false);
        useWoodmillsInRadiusTextInput = new guiModuleTextInput(6, guiHandlerMain, 150, 12, 20, 10) {
            @Override
            public void server_readNetworkData(CompoundTag tag) {
                String lastText = new String(text);
                super.server_readNetworkData(tag);
                try {
                    if(text.length() > 1){
                        while (text.charAt(0) == '0')
                            this.text = this.text.substring(1, this.text.length());
                    }
                    useWoodmillsInRadius = Integer.parseInt(text);
                    broadcastModuleUpdate();
                } catch (NumberFormatException e) {
                    if(text == "")
                        text = "0";
                    else
                        text = lastText;
                    broadcastModuleUpdate();
                }
            }
        };
        if(ModList.get().isLoaded("aw_npc")) {
            guiHandlerMain.getModules().add(useWoodmillsInRadiusText);
            guiHandlerMain.getModules().add(useWoodmillsInRadiusTextInput);
        }
        useWoodmillsInRadiusTextInput.text = String.valueOf(useWoodmillsInRadius);
    }
    @Override
    public void onLoad(){
        super.onLoad();
        knownTreeFarms.add(this.getBlockPos());
    }
    @Override
    public void setRemoved(){
        knownTreeFarms.remove(this.getBlockPos());
        super.setRemoved();
    }

    public static boolean isItemValidSapling(ItemStack s) {
        if (s.getItem() instanceof BlockItem bi) {
            Block itemBlock = bi.getBlock();
            if (itemBlock instanceof SaplingBlock) {
                return true;
            }
            //if (itemBlock instanceof ChorusPlantBlock) {
            //    return true;
            //}
        }
        return false;
    }

    public boolean canPlant(BlockPos p) {
        BlockState state = level.getBlockState(p);
        if (state.isAir()) return true;
        if (state.canBeReplaced()) return true;
        return false;
    }
    public boolean tryPlantPosition(BlockPos p) {
        if (!canPlant(p)) return false;
        for (int i = 0; i < inputsInventory.getSlots(); i++) {
            ItemStack s = inputsInventory.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                if (bi.getBlock().defaultBlockState().canSurvive(level, p)) {
                    level.setBlock(p, bi.getBlock().defaultBlockState(), 3);
                    s.shrink(1);
                    setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canBoneMeal(BlockPos p){
        BlockState state = level.getBlockState(p);
        return state.getBlock() instanceof BonemealableBlock bab && bab.isValidBonemealTarget(level,p,state);
    }
    public boolean canHarvestPosition(BlockPos p) {
        BlockState state = level.getBlockState(p);
        if(state.is(BlockTags.LOGS)){
            return true;
        }
        if(state.is(BlockTags.LEAVES)){
            return true;
        }
        return false;
    }

    public boolean harvestPosition(BlockPos p) {
        if (canHarvestPosition(p)) {
            BlockState s = level.getBlockState(p);
            ItemStack tool = new ItemStack(Items.IRON_AXE);
            if(s.is(BlockTags.LEAVES)){
                for (int i = 0; i < specialResourcesInventory.getSlots(); i++) {
                    ItemStack stackInSlot = specialResourcesInventory.getStackInSlot(i);
                    if(stackInSlot.getItem().equals(Items.SHEARS)){
                        tool = stackInSlot;
                        tool.setDamageValue(tool.getDamageValue()+1);
                        if(tool.getDamageValue()>=tool.getMaxDamage()){
                            stackInSlot.shrink(1);
                        }
                        setChanged();
                        break;
                    }
                }
            }
            LootParams.Builder b = new LootParams.Builder((ServerLevel) level)
                    .withParameter(LootContextParams.TOOL,tool)
                    .withParameter(LootContextParams.ORIGIN,getBlockPos().getCenter());
            List<ItemStack> drops = s.getDrops(b);

            //this does not consider that inputsInventory only can hold valid seeds
            // worst case some items get "voided" if mainInventory is full but inputsInventory is not
            if(InventoryUtils.canInsertAllItems(List.of(inputsInventory,mainInventory),drops)) {
                level.destroyBlock(p,false);
                for (ItemStack i : drops) {
                    if (isItemValidSapling(i)) {
                        for (int j = 0; j < inputsInventory.getSlots(); j++) {
                            i = inputsInventory.insertItem(j,i,false);
                        }
                    }
                    for (int j = 0; j < mainInventory.getSlots(); j++) {
                        i = mainInventory.insertItem(j,i,false);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void scanStep(){
        if (!allowedBlocksList.isEmpty()) {
            if (currentBlockToScanIndex >= allowedBlocksList.size()) {
                currentBlockToScanIndex = 0;
            }
            BlockPos nextPosToScan = allowedBlocksList.get(currentBlockToScanIndex);
            currentBlockToScanIndex += 1;

            if(blackListAsBlockPos.contains(nextPosToScan))
                return;

            if (canPlant(nextPosToScan)) {
                positionsToPlant.add(nextPosToScan);
            }
            if(canBoneMeal(nextPosToScan)){
                positionsToBoneMeal.add(nextPosToScan);
            }

            if(!positionsToHarvest_Logs.contains(nextPosToScan)) {
                BlockState state = level.getBlockState(nextPosToScan);
                if (state.is(BlockTags.LOGS)) {
                    //use new sets because it could miss blocks else
                    Set<BlockPos> newLeavePos = new HashSet<>();
                    Set<BlockPos> newLogPos = new HashSet<>();
                    TreeScanner.scanDefaultTree(level, nextPosToScan, pmin, pmax, newLeavePos, newLogPos);
                    positionsToHarvest_Leaves.addAll(newLeavePos);
                    positionsToHarvest_Logs.addAll(newLogPos);
                }
            }
        }
    }

    public boolean tryPlant(){
        while (!positionsToPlant.isEmpty()) {
            BlockPos target = positionsToPlant.iterator().next();
            positionsToPlant.remove(target);
            if (tryPlantPosition(target)) {
                return true;
            }
        }
        return false;
    }
    public boolean tryHarvestLeaves(){
        if (!positionsToHarvest_Leaves.isEmpty()) {
            // Find the topmost position in the set
            BlockPos topmost = null;
            for (BlockPos pos : positionsToHarvest_Leaves) {
                if (topmost == null || pos.getY() > topmost.getY()) {
                    topmost = pos;
                }
            }
            // Remove the topmost position and process it
            positionsToHarvest_Leaves.remove(topmost);
            if (harvestPosition(topmost)) {
                return true;
            }
        }
        return false;
    }
    public boolean tryHarvestLogs(){
        if (!positionsToHarvest_Logs.isEmpty()) {
            // Find the topmost position in the set
            BlockPos topmost = null;
            for (BlockPos pos : positionsToHarvest_Logs) {
                if (topmost == null || pos.getY() > topmost.getY()) {
                    topmost = pos;
                }
            }
            // Remove the topmost position and process it
            positionsToHarvest_Logs.remove(topmost);
            if (harvestPosition(topmost)) {
                return true;
            }
        }
        return false;
    }
    public boolean tryBoneMeal(){
        while (!positionsToBoneMeal.isEmpty()) {
            List<BlockPos> shuffledList = new ArrayList<>(positionsToBoneMeal);
            Collections.shuffle(shuffledList);
            BlockPos target = shuffledList.getFirst();
            positionsToBoneMeal.remove(target);

            if (canBoneMeal(target)) {
                for (int i = 0; i < specialResourcesInventory.getSlots(); i++) {
                    ItemStack stackInSlot = specialResourcesInventory.getStackInSlot(i);
                    if(stackInSlot.getItem().equals(Items.BONE_MEAL)){
                        stackInSlot.shrink(1);
                        BlockState s = level.getBlockState(target);
                        if (s.getBlock() instanceof BonemealableBlock bab) {
                            bab.performBonemeal((ServerLevel) level, level.random, target, s);
                        }
                        setChanged();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            scanStep();

            a:
            {
                if (battery.getEnergyStored() > maxEnergy) {
                    if (tryPlant()) {
                        battery.extractEnergy(energy_plant, false);
                        break a;
                    }
                    if (tryHarvestLeaves()) {
                        battery.extractEnergy(energy_harvest_leaves, false);
                        break a;
                    }
                    if (tryHarvestLogs()) {
                        battery.extractEnergy(energy_harvest_logs, false);
                        break a;
                    }
                    if (tryBoneMeal()) {
                        battery.extractEnergy(energy_boneMeal, false);
                        break a;
                    }
                }
            }
        }
    }

    @Override
    public void openMainGui() {
        if (level.isClientSide) {
            guiHandlerMain.openGui(180, 240, true);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv1", mainInventory.serializeNBT(registries));
        tag.put("inv2", inputsInventory.serializeNBT(registries));
        tag.put("inv3", specialResourcesInventory.serializeNBT(registries));

        tag.putInt("useWoodmillRadius", useWoodmillsInRadius);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mainInventory.deserializeNBT(registries, tag.getCompound("inv1"));
        inputsInventory.deserializeNBT(registries, tag.getCompound("inv2"));
        specialResourcesInventory.deserializeNBT(registries, tag.getCompound("inv3"));

        useWoodmillsInRadius = tag.getInt("useWoodmillRadius");
        useWoodmillsInRadiusTextInput.text = String.valueOf(useWoodmillsInRadius);
    }
}
