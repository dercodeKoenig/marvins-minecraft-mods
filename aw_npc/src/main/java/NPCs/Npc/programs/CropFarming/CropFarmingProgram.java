package NPCs.Npc.programs.CropFarming;

import NPCs.Npc.WorkerNPC;
import NPCs.Utils;
import NPCs.Npc.programs.TakeToolProgram;
import WorkSites.CropFarm.EntityCropFarm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static NPCs.Utils.*;

public class CropFarmingProgram {
    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    public WorkerNPC worker;
    int scanInterval = 20 * 20;
    int requiredDistanceToFarmToPickup = 2;
    int requiredDistanceToPositionToWork = 2;
    int requiredFreeSlotsToHarvest = 3;

    public BlockPos currentTargetPos = null;
    long lastScan = 0;
    int workDelay = 0;
    boolean hasWork = false;
    boolean hasWorkHarvest;
    boolean hasWorkPlant;
    boolean hasWorkTill;
    boolean hasWorkTakeSeeds;

    TakeToolProgram takeHoeProgram;

    public CropFarmingProgram(WorkerNPC worker) {
        this.worker = worker;
        takeHoeProgram = new TakeToolProgram(worker);
    }


    public void lockTargetPosition() {
        long gameTime = worker.level().getGameTime();
        positionsInUseWithLastUseTime.put(currentTargetPos, gameTime);
    }

    public boolean isPositionLocked(BlockPos p) {
        // if I lock the position, it is not locked for ME, only for OTHER WORKERS
        if (Objects.equals(p, currentTargetPos)) return false;

        long gameTime = worker.level().getGameTime();
        return (positionsInUseWithLastUseTime.containsKey(p) &&
                positionsInUseWithLastUseTime.get(p) + 5 > gameTime);
    }

    public boolean isPositionWorkable(BlockPos p) {
        // if the position was recently locked, another worker works there so i can not work here
        if (isPositionLocked(p))
            return false;

        // if the position is cached as not reachable, i can not work here
        if (worker.slowMobNavigation.isPositionCachedAsInvalid(p)) {
            return false;
        }
        return true;
    }


    ///  HARVEST PROGRAM CODE START ///
    public BlockPos getNextHarvestTargetFromFarm(EntityCropFarm farm) {
        for (BlockPos i : Utils.sortBlockPosByDistanceToVec(farm.positionsToHarvest, worker)) {
            if (isPositionWorkable(i)) {
                return i;
            }
        }
        return null;
    }

    public boolean canHarvestFarm(EntityCropFarm target) {
        // check if i have a hoe or if i can take a hoe from farm.
        // if not, i can not harvest
        if (!takeHoeProgram.hasTool(HoeItem.class) &&
                (
                        !takeHoeProgram.pickupToolFromTarget(HoeItem.class, target.mainInventory, true) &&
                                !takeHoeProgram.pickupToolFromTarget(HoeItem.class, target.inputsInventory, true) &&
                                !takeHoeProgram.pickupToolFromTarget(HoeItem.class, target.specialResourcesInventory, true)
                )
        ) {
            return false;
        }

        // check if the farm has valid harvest targets
        if (getNextHarvestTargetFromFarm(target) == null)
            return false;


        // The worker needs to have some empty slots in his inventory.
        // If the worker is close to the farm, he should first try to unload some more before he goes to work,
        // or else he would harvest one block, inventory would have less than x stacks free, he goes to farm, unloads 1 item, inventory has x stacks free and so on.....
        // But because if he takes a hoe, he will use up one free slot, this can cause him to have x free slots sp he can work, he takes a hoe, he has x-1 free slots, he can not work, he puts hoe back, he has x free......
        // This is why the hoe should be ignored
        int numEmptySlotsIgnoreHoe = 0;
        boolean ignoredTool = false; // only ignore one tool item
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            if (worker.combinedInventory.getStackInSlot(i).isEmpty() ||
                    (worker.combinedInventory.getStackInSlot(i).getItem() instanceof HoeItem && !ignoredTool)) {
                numEmptySlotsIgnoreHoe++;
                if(worker.combinedInventory.getStackInSlot(i).getItem() instanceof HoeItem){
                    ignoredTool = true;
                }
            }
        }
        if (Utils.distanceManhattan(worker, target.getBlockPos().getCenter()) > 5) {
            if (numEmptySlotsIgnoreHoe < requiredFreeSlotsToHarvest) return false;
        } else {
            if (numEmptySlotsIgnoreHoe < requiredFreeSlotsToHarvest + 3) return false;
        }

        // if he has / can take hoe, has positions to work on and has free space in inventory, he can work
        return true;
    }

    public int runHarvestProgram(EntityCropFarm farm) {

        if (farm.positionsToHarvest.contains(currentTargetPos)) {

            // lock the target so no other worker goes there
            lockTargetPosition();

            // make sure I have a hoe in inventory
            int takeHoeExit = runTakeHoeFromFarmAnyInventory(farm);
            if (takeHoeExit == SUCCESS_STILL_RUNNING){
                return SUCCESS_STILL_RUNNING;
            }
            if (takeHoeExit == EXIT_FAIL) {
                recalculateHasWork(farm);
                return EXIT_FAIL;
            }

            // take the tool to main hand
            takeHoeProgram.takeToolToMainHand(HoeItem.class);

            int pathFindExit = worker.slowMobNavigation.moveToPosition(
                    currentTargetPos,
                    requiredDistanceToPositionToWork,
                    worker.slowNavigationMaxDistance,
                    worker.slowNavigationMaxNodes,
                    worker.slowNavigationStepPerTick
            );


            if (pathFindExit == EXIT_FAIL) {
                currentTargetPos = null;
                recalculateHasWork(farm);
                return SUCCESS_STILL_RUNNING;
            } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                workDelay = 0;
                return SUCCESS_STILL_RUNNING;
            }
            worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentTargetPos.getCenter());
            worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentTargetPos.getCenter());

            workDelay++;
            if (workDelay > 20) {
                workDelay = 0;
                // time to Harvest
                // check again to make sure everything will work
                // i will harvest even if it not fit all in inventory
                // i require 3 free slots to harvest
                if (farm.canHarvestPosition(currentTargetPos)) {
                    BlockState s = worker.level().getBlockState(currentTargetPos);
                    LootParams.Builder b = (new LootParams.Builder((ServerLevel) worker.level())).withParameter(LootContextParams.TOOL, worker.getMainHandItem()).withParameter(LootContextParams.ORIGIN, worker.getPosition(0));
                    List<ItemStack> drops = s.getDrops(b);
                    for (ItemStack i : drops) {
                        for (int j = 0; j < worker.combinedInventory.getSlots(); ++j) {
                            i = worker.combinedInventory.insertItem(j, i, false);
                        }
                    }
                    worker.level().destroyBlock(currentTargetPos, false);
                    worker.swing(InteractionHand.MAIN_HAND);
                    Utils.damageMainHandItem(worker);
                }
                farm.positionsToHarvest.remove(currentTargetPos);
                recalculateHasWork(farm);
            }
            return SUCCESS_STILL_RUNNING;
        }
        currentTargetPos = getNextHarvestTargetFromFarm(farm);
        recalculateHasWork(farm);
        return SUCCESS_STILL_RUNNING;
    }
    ///  HARVEST PROGRAM CODE END ///




    ///  PLANT PROGRAM CODE START ///
    public boolean isAllowedToPlantItemByWorkOrder(ItemStack seed, EntityCropFarm farm) {
        // check if seed is whitelisted in work order for current farm
        return true;
    }

    public boolean isValidSeedItemForFarm(EntityCropFarm farm, ItemStack stack) {
        boolean ret = (EntityCropFarm.isItemValidSeed(stack) &&
                canPlantAnywhere(stack, farm) != null &&
                isAllowedToPlantItemByWorkOrder(stack, farm));
        return ret;
    }

    public BlockPos canPlantAnywhere(ItemStack seed, EntityCropFarm farm) {
        Item var5 = seed.getItem();
        for (BlockPos p : farm.positionsToPlant) {
            if(!isPositionWorkable(p)) continue;
            if (var5 instanceof BlockItem bi) {
                if (bi.getBlock().defaultBlockState().canSurvive(worker.level(), p)) {
                    return p;
                }
            }
        }
        return null;
    }

    public BlockPos getNextPlantTarget(EntityCropFarm farm) {
        for (BlockPos p : Utils.sortBlockPosByDistanceToVec(farm.positionsToPlant, worker)){
            if(!isPositionWorkable(p)) continue;
            ItemStack s = getStackToPlantAtPosition(farm, p);
            if(!s.isEmpty()){
                return p;
            }
        }
        return null;
    }



    public ItemStack getStackToPlantAtPosition(EntityCropFarm farm, BlockPos p) {
        if (!farm.canPlant(p)) return ItemStack.EMPTY;
        if(!isPositionWorkable(p)) return ItemStack.EMPTY;

        for (int i = 0; i < worker.combinedInventory.getSlots(); ++i) {
            ItemStack s = worker.combinedInventory.getStackInSlot(i);
            if (!s.isEmpty() && isValidSeedItemForFarm(farm,s)) {
                Item var5 = s.getItem();
                if (var5 instanceof BlockItem bi) {
                    if (bi.getBlock().defaultBlockState().canSurvive(worker.level(), p)) {
                        return s;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean canPlantAtFarm(EntityCropFarm farm) {
        return getNextPlantTarget(farm) != null;
    }

    public int runPlantProgram(EntityCropFarm farm) {
        if (farm.positionsToPlant.contains(currentTargetPos)) {
            ItemStack stackToPlant = getStackToPlantAtPosition(farm, currentTargetPos);
            if (!stackToPlant.isEmpty()) {

                lockTargetPosition();

                int pathFindExit = worker.slowMobNavigation.moveToPosition(
                        currentTargetPos,
                        requiredDistanceToPositionToWork,
                        worker.slowNavigationMaxDistance,
                        worker.slowNavigationMaxNodes,
                        worker.slowNavigationStepPerTick
                );

                if (!ItemStack.isSameItemSameComponents(worker.getMainHandItem(), stackToPlant) &&
                        !ItemStack.isSameItemSameComponents(worker.getOffhandItem(), stackToPlant)) {
                    Utils.moveItemStackToAnyHand(stackToPlant, worker);
                }

                if (pathFindExit == EXIT_FAIL) {
                    currentTargetPos = null;
                    recalculateHasWork(farm);
                    return SUCCESS_STILL_RUNNING;
                }
                if (pathFindExit == SUCCESS_STILL_RUNNING) {
                    workDelay = 0;
                    return SUCCESS_STILL_RUNNING;
                }
                worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentTargetPos.getCenter());
                worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentTargetPos.getCenter());

                workDelay++;
                if (workDelay > 20) {
                    workDelay = 0;
                    // time to plant
                    worker.level().setBlock(currentTargetPos, ((BlockItem) (stackToPlant.getItem())).getBlock().defaultBlockState(), 3);
                    stackToPlant.shrink(1);
                    if (worker.getMainHandItem().getItem().equals(stackToPlant.getItem()))
                        worker.swing(InteractionHand.MAIN_HAND);
                    else if (worker.getOffhandItem().getItem().equals(stackToPlant.getItem()))
                        worker.swing(InteractionHand.OFF_HAND);

                    farm.positionsToPlant.remove(currentTargetPos);
                    recalculateHasWork(farm);
                }
                return SUCCESS_STILL_RUNNING;
            }
        }
        currentTargetPos = getNextPlantTarget(farm);
        recalculateHasWork(farm);
        return SUCCESS_STILL_RUNNING;
    }
    ///  PLANT PROGRAM CODE END ///



    ///  TAKE SEED PROGRAM CODE START ///
    public boolean hasAnyValidSeedItem(EntityCropFarm farm) {
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stackInSlot = worker.combinedInventory.getStackInSlot(i);
            if (isValidSeedItemForFarm(farm, stackInSlot)) return true;
        }
        return false;
    }

    public ItemStack takeOneSeedFromFarm(EntityCropFarm farm, boolean simulate) {
        // check if the farm has any seed in inputs inventory and if worker has space for seed
        for (int i = 0; i < farm.inputsInventory.getSlots(); i++) {
            ItemStack canExtract = farm.inputsInventory.extractItem(i, 1, true);
            if (!canExtract.isEmpty() && isValidSeedItemForFarm(farm, canExtract)) {
                for (int j = 0; j < worker.combinedInventory.getSlots(); j++) {
                    ItemStack notInserted = worker.combinedInventory.insertItem(j, canExtract, true);
                    if (notInserted.isEmpty()) {
                        if (!simulate) {
                            // if it can be inserted, do the actual movement and break
                            ItemStack extracted = farm.inputsInventory.extractItem(i, 1, false);
                            worker.combinedInventory.insertItem(j, extracted, false);
                            InteractionHand movedTo = Utils.moveItemStackToAnyHand(worker.combinedInventory.getStackInSlot(j), worker);
                            worker.swing(movedTo);
                        }
                        return canExtract.copy();
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean shouldTakeSeedsFromFarm(EntityCropFarm farm) {

        // if the worker already has one or more seed items he does not need to reload
        // but we want him not to load 1 item and run away and come back all the time
        // so if he is currently near the farm, count how many seeds he has and only exit if he has enough
        if (worker.cachedDistanceManhattanToWorksite > requiredDistanceToFarmToPickup + 2) {
            if (hasAnyValidSeedItem(farm))
                return false;
        } else {
            int seedItemsCount = 0;
            for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
                ItemStack stackInSlot = worker.combinedInventory.getStackInSlot(i);
                if (isValidSeedItemForFarm(farm, stackInSlot)) {
                    seedItemsCount += stackInSlot.getCount();
                }
            }

            // TODO not all positions are plantable because of block.cansurvive so he always takes a bit more
            if (seedItemsCount >= Math.min(farm.positionsToPlant.size(), 32)) {
                return false;
            }
        }

        // check if it can not pick up any seeds from the farm because inventory full or no seeds in farm, do not run
        if (takeOneSeedFromFarm(farm, true) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }
    public int runTakeSeedProgram(EntityCropFarm farm) {

        int pathFindExit = worker.slowMobNavigation.moveToPosition(
                farm.getBlockPos(),
                requiredDistanceToFarmToPickup,
                worker.slowNavigationMaxDistance,
                worker.slowNavigationMaxNodes,
                worker.slowNavigationStepPerTick
        );
        if (pathFindExit == EXIT_FAIL) {
            recalculateHasWork(farm);
            return SUCCESS_STILL_RUNNING;
        }
        if (pathFindExit == SUCCESS_STILL_RUNNING) {
            workDelay = 0;
            return SUCCESS_STILL_RUNNING;
        }

        worker.lookAt(EntityAnchorArgument.Anchor.EYES, farm.getBlockPos().getCenter());
        worker.lookAt(EntityAnchorArgument.Anchor.FEET, farm.getBlockPos().getCenter());

        if (workDelay > 5) {
            workDelay = 0;
            // try to take 1 seed item
            takeOneSeedFromFarm(farm, false);
            recalculateHasWork(farm);
        }
        workDelay++;

        return SUCCESS_STILL_RUNNING;
    }

    ///  TAKE SEED PROGRAM CODE END ///


    ///  TILL PROGRAM CODE START ///
    public boolean canTillPosition(EntityCropFarm farm, BlockPos p) {
        // first check if the position is valid to grow anything (not blocked by other blocks or stem plants)
        if (!farm.canPlant(p)) return false;

        if(!isPositionWorkable(p)) return false;

        // if i can already plant here, do not till
        if (getStackToPlantAtPosition(farm, p) != ItemStack.EMPTY)
            return false;

        BlockState s = worker.level().getBlockState(p.below());
        Block b = s.getBlock();
        if (b.equals(Blocks.DIRT) || b.equals(Blocks.DIRT_PATH) || b.equals(Blocks.GRASS_BLOCK)) {
            return true;
        }

        return false;
    }

    public BlockPos getNextTillTarget(EntityCropFarm farm){
        for (BlockPos p : Utils.sortBlockPosByDistanceToVec(farm.positionsToPlant, worker)) {
            if (canTillPosition(farm, p)) {
                return p;
            }
        }
        return null;
    }

    public boolean canTill(EntityCropFarm target) {

        // check if i have a hoe or if i can take a hoe from farm.
        // if not, i can not till
        if (!takeHoeProgram.hasTool(HoeItem.class) &&
                (
                        !takeHoeProgram.pickupToolFromTarget(HoeItem.class, target.mainInventory, true) &&
                                !takeHoeProgram.pickupToolFromTarget(HoeItem.class, target.inputsInventory, true) &&
                                !takeHoeProgram.pickupToolFromTarget(HoeItem.class, target.specialResourcesInventory, true)
                )
        ) {
            return false;
        }

        if(getNextTillTarget(target) == null ) {
            return false;
        }

        return true;
    }


    public int runTillProgram(EntityCropFarm farm) {

        if (farm.positionsToPlant.contains(currentTargetPos)) {
            if (canTillPosition(farm, currentTargetPos)) {

                lockTargetPosition();

                // make sure I have a hoe in inventory
                int takeHoeExit = runTakeHoeFromFarmAnyInventory(farm);
                if (takeHoeExit == SUCCESS_STILL_RUNNING) return SUCCESS_STILL_RUNNING;
                if (takeHoeExit == EXIT_FAIL) {
                    recalculateHasWork(farm);
                    return EXIT_FAIL;
                }

                // take the tool to main hand
                takeHoeProgram.takeToolToMainHand(HoeItem.class);


                int pathFindExit = worker.slowMobNavigation.moveToPosition(
                        currentTargetPos,
                        requiredDistanceToPositionToWork,
                        worker.slowNavigationMaxDistance,
                        worker.slowNavigationMaxNodes,
                        worker.slowNavigationStepPerTick
                );


                if (pathFindExit == EXIT_FAIL) {
                    currentTargetPos = null;
                    recalculateHasWork(farm);
                    return SUCCESS_STILL_RUNNING;
                } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                    workDelay = 0;
                    return SUCCESS_STILL_RUNNING;
                }


                worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentTargetPos.getCenter());
                worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentTargetPos.getCenter());

                workDelay++;
                if (workDelay > 20) {
                    workDelay = 0;
                    // time to Till
                    worker.level().setBlock(currentTargetPos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
                    worker.swing(InteractionHand.MAIN_HAND);
                    farm.positionsToPlant.remove(currentTargetPos);
                    Utils.damageMainHandItem(worker);
                    recalculateHasWork(farm);
                }
                return SUCCESS_STILL_RUNNING;
            }
        }
        currentTargetPos = getNextTillTarget(farm);
        recalculateHasWork(farm);

        return SUCCESS_STILL_RUNNING;
    }
    /// TILL PROGRAM CODE END ///


    public boolean recalculateHasWork(EntityCropFarm target) {

        hasWorkHarvest = canHarvestFarm(target);
        hasWorkPlant = canPlantAtFarm(target);
        hasWorkTakeSeeds = shouldTakeSeedsFromFarm(target);
        hasWorkTill = canTill(target);

        hasWork = hasWorkHarvest || hasWorkPlant || hasWorkTakeSeeds || hasWorkTill;
        hasWork = hasWork && isPositionWorkable(target.getBlockPos()) && worker.hunger > worker.maxHunger * 0.25;
        return hasWork;
    }

    public int run(EntityCropFarm farm) {

        long gameTime = worker.level().getGameTime();
        if (gameTime > lastScan + scanInterval) {
            lastScan = gameTime;
            recalculateHasWork(farm);
        }

        if (!hasWork) {
            return EXIT_SUCCESS;
        }
//System.out.println(hasWorkTakeSeeds+":"+hasWorkHarvest+":"+hasWorkTill+":"+hasWorkPlant);
        if(hasWorkTakeSeeds)
            return runTakeSeedProgram(farm);

        if(hasWorkPlant)
            return runPlantProgram(farm);

        if (hasWorkHarvest)
            return runHarvestProgram(farm);

        if (hasWorkTill)
            return runTillProgram(farm);


        return EXIT_SUCCESS;
    }

    public int runTakeHoeFromFarmAnyInventory(EntityCropFarm farm) {
        // first take from main inventory
        int takeHoeExit = takeHoeProgram.run(HoeItem.class, farm.getBlockPos(), farm.mainInventory);
        // if no tool there, take from inputs inventory
        if (takeHoeExit == -2)
            takeHoeExit = takeHoeProgram.run(HoeItem.class, farm.getBlockPos(), farm.inputsInventory);
        // if still no tool there, take from special resources inventory
        if (takeHoeExit == -2)
            takeHoeExit = takeHoeProgram.run(HoeItem.class, farm.getBlockPos(), farm.specialResourcesInventory);
        // if it is still tool not found, the program failed to get a hoe from any farm inventory
        if (takeHoeExit == -2)
            return EXIT_FAIL;

        return takeHoeExit;
    }
}
