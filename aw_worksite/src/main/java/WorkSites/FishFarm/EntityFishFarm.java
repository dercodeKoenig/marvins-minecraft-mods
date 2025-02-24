package WorkSites.FishFarm;

import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.utils.InventoryUtils;
import WorkSites.Config;
import WorkSites.EntityWorkSiteBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

import static WorkSites.Registry.ENTITY_FISH_FARM;

public class EntityFishFarm extends EntityWorkSiteBase {
    public static Set<BlockPos> knownFishFarms = new HashSet<>();

    public int energy_try_fish = Config.INSTANCE.energy_try_fish;
    int depth = 5;

    // a fish farm with this size would always find something during fish step
    // a fish farm half the size will find about half as often something
    // set it larger to reduce findings
    int maxVolumeForP = 64 * 64 * depth * 2;


    public Set<BlockPos> waterBlocks = new HashSet<>();

    public ItemStackHandler mainInventory = new ItemStackHandler(18) {
        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    public ItemStackHandler inputsInventory = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    public ItemStackHandler specialResourcesInventory = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.getItem().equals(Items.FISHING_ROD))
                return true;
            else return false;
        }

        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    int currentBlockToScanIndex = 0;

    public EntityFishFarm(BlockPos pos, BlockState blockState) {
        super(ENTITY_FISH_FARM.get(), pos, blockState);

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
    }
    @Override
    public void onLoad(){
        super.onLoad();
        knownFishFarms.add(this.getBlockPos());
    }
    @Override
    public void setRemoved(){
        knownFishFarms.remove(this.getBlockPos());
        super.setRemoved();
    }

    @Override
    public void updateBoundsBp() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos p1 = getBlockPos().relative(facing, controllerOffsetH - 1);
        p1 = p1.relative(facing.getClockWise(), controllerOffsetW);
        p1 = p1.relative(Direction.DOWN, depth);
        BlockPos p2 = p1.relative(facing.getCounterClockWise(), w - 1).relative(facing.getOpposite(), h - 1).relative(Direction.UP, depth - 1);

        pmin = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        pmax = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

        // this farm does not use blacklist
        updateAllowedBlocksList();
    }


    public boolean tryFish() {
        for (int i = 0; i < specialResourcesInventory.getSlots(); i++) {
            ItemStack tool = specialResourcesInventory.getStackInSlot(i);
            if (tool.getItem().equals(Items.FISHING_ROD)) {
                // if small area it will not always catch something
                double myVolume = depth * w * h;
                double r = level.random.nextFloat();
                if (r > myVolume / maxVolumeForP) {
                    return true;
                } else {
                    LootParams lootparams = new LootParams.Builder((ServerLevel) this.level)
                            .withParameter(LootContextParams.ORIGIN, getBlockPos().getCenter())
                            .withParameter(LootContextParams.TOOL, tool)
                            .withLuck(tool.getEnchantmentLevel(level.registryAccess().holderOrThrow(Enchantments.LUCK_OF_THE_SEA)))
                            .create(LootContextParamSets.FISHING);

                    LootTable loottable = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
                    List<ItemStack> list = loottable.getRandomItems(lootparams);

                    if (InventoryUtils.canInsertAllItems(List.of(mainInventory), list)) {
                        tool.setDamageValue(tool.getDamageValue() + 1);
                        if (tool.getDamageValue() >= tool.getMaxDamage()) {
                            tool.shrink(1);
                        }

                        for (ItemStack itemStack : list) {
                        for (int j = 0; j < mainInventory.getSlots(); j++) {
                                itemStack = mainInventory.insertItem(j, itemStack, false);
                            }
                        }
                        setChanged();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void scanStep() {
        if (!allowedBlocksList.isEmpty()) {
            if (currentBlockToScanIndex >= allowedBlocksList.size()) {
                currentBlockToScanIndex = 0;
            }
            BlockPos nextPosToScan = allowedBlocksList.get(currentBlockToScanIndex);
            currentBlockToScanIndex += 1;

            if (blackListAsBlockPos.contains(nextPosToScan))
                return;

            BlockState s = level.getBlockState(nextPosToScan);
            if (s.getBlock().equals(Blocks.WATER)) {
                waterBlocks.add(nextPosToScan);
            } else {
                waterBlocks.remove(nextPosToScan);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            scanStep();

            if (battery.getEnergyStored() > energy_try_fish) {
                if (tryFish()) {
                    battery.extractEnergy(energy_try_fish, false);
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
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mainInventory.deserializeNBT(registries, tag.getCompound("inv1"));
        inputsInventory.deserializeNBT(registries, tag.getCompound("inv2"));
        specialResourcesInventory.deserializeNBT(registries, tag.getCompound("inv3"));
    }
}
