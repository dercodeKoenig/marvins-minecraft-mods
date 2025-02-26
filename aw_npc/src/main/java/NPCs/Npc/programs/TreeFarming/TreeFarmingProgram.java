package NPCs.Npc.programs.TreeFarming;

import NPCs.Npc.WorkerNPC;
import NPCs.Utils;
import NPCs.Npc.programs.TakeToolProgram;
import WorkSites.TreeFarm.EntityTreeFarm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static NPCs.Utils.*;

public class TreeFarmingProgram {
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
    boolean hasWorkTakeSeeds;

    TakeToolProgram takeAxeProgram;

    public TreeFarmingProgram(WorkerNPC worker) {
        this.worker = worker;
        takeAxeProgram = new TakeToolProgram(worker);
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
    public BlockPos getNextHarvestTargetFromFarm(EntityTreeFarm farm) {
        List<BlockPos> potentialTargets = new ArrayList<>();

        // try to cut only leaves on lower parts to not waste too much time / tools breaking leaves
        for (BlockPos i : farm.positionsToHarvest_Leaves) {
            if (i.getY() <= farm.getBlockPos().getY() + 2) {
                if (isPositionWorkable(i)) {
                    potentialTargets.add(i);
                }
            }
        }
        if (!potentialTargets.isEmpty()) {
            for (BlockPos i : Utils.sortBlockPosByDistanceToVec(potentialTargets, worker)) {
                return i;
            }
        }


        for (BlockPos i : farm.positionsToHarvest_Logs) {
            if (isPositionWorkable(i)) {
                potentialTargets.add(i);
            }
        }
        // work wood blocks top to bottom
        int highestY = -99999;
        for (BlockPos p : potentialTargets) {
            highestY = Math.max(highestY, p.getY());
        }
        List<BlockPos> potentialTargets2 = new ArrayList<>();
        for (BlockPos p : potentialTargets) {
            if (p.getY() == highestY) {
                potentialTargets2.add(p);
            }
        }
        for (BlockPos i : Utils.sortBlockPosByDistanceToVec(potentialTargets2, worker)) {
            return i;
        }

        return null;
    }

    public boolean canHarvestFarm(EntityTreeFarm target) {

        if (!takeAxeProgram.hasTool(AxeItem.class) &&
                (
                        !takeAxeProgram.pickupToolFromTarget(AxeItem.class, target.mainInventory, true) &&
                                !takeAxeProgram.pickupToolFromTarget(AxeItem.class, target.inputsInventory, true) &&
                                !takeAxeProgram.pickupToolFromTarget(AxeItem.class, target.specialResourcesInventory, true)
                )
        ) {
            return false;
        }

        if (getNextHarvestTargetFromFarm(target) == null)
            return false;

        
        int numEmptySlotsIgnoreTool = 0;
        boolean ignoredTool = false;
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            if (worker.combinedInventory.getStackInSlot(i).isEmpty() ||
                    (worker.combinedInventory.getStackInSlot(i).getItem() instanceof AxeItem && ! ignoredTool))
                numEmptySlotsIgnoreTool++;
            if(worker.combinedInventory.getStackInSlot(i).getItem() instanceof AxeItem){
                ignoredTool = true;
            }
        }
        if (Utils.distanceManhattan(worker, target.getBlockPos().getCenter()) > 5) {
            if (numEmptySlotsIgnoreTool < requiredFreeSlotsToHarvest) return false;
        } else {
            if (numEmptySlotsIgnoreTool < requiredFreeSlotsToHarvest + 3) return false;
        }

        return true;
    }

    public int runHarvestProgram(EntityTreeFarm farm) {

        if (farm.positionsToHarvest_Leaves.contains(currentTargetPos) || farm.positionsToHarvest_Logs.contains(currentTargetPos)) {

            // lock the target so no other worker goes there
            lockTargetPosition();

            // make sure I have a hoe in inventory
            int takeHoeExit = runTakeAxeFromFarmAnyInventory(farm);
            if (takeHoeExit == SUCCESS_STILL_RUNNING){
                return SUCCESS_STILL_RUNNING;
            }
            if (takeHoeExit == EXIT_FAIL) {
                recalculateHasWork(farm);
                return EXIT_FAIL;
            }

            // take the tool to main hand
            takeAxeProgram.takeToolToMainHand(AxeItem.class);

            // move to the y level of the farm
            int pathFindExit = worker.slowMobNavigation.moveToPosition(
                    new BlockPos(currentTargetPos.getX(), farm.getBlockPos().getY(), currentTargetPos.getZ()),
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
                farm.positionsToHarvest_Logs.remove(currentTargetPos);
                farm.positionsToHarvest_Leaves.remove(currentTargetPos);
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
    public boolean isAllowedToPlantItemByWorkOrder(ItemStack seed, EntityTreeFarm farm) {
        // check if seed is whitelisted in work order for current farm
        return true;
    }

    public boolean isValidSeedItemForFarm(EntityTreeFarm farm, ItemStack stack) {
        boolean ret = (EntityTreeFarm.isItemValidSapling(stack) &&
                canPlantAnywhere(stack, farm) != null &&
                isAllowedToPlantItemByWorkOrder(stack, farm));
        return ret;
    }

    public BlockPos canPlantAnywhere(ItemStack seed, EntityTreeFarm farm) {
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

    public BlockPos getNextPlantTarget(EntityTreeFarm farm) {
        for (BlockPos p : Utils.sortBlockPosByDistanceToVec(farm.positionsToPlant, worker)){
            if(!isPositionWorkable(p)) continue;
            ItemStack s = getStackToPlantAtPosition(farm, p);
            if(!s.isEmpty()){
                return p;
            }
        }
        return null;
    }



    public ItemStack getStackToPlantAtPosition(EntityTreeFarm farm, BlockPos p) {
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

    public boolean canPlantAtFarm(EntityTreeFarm farm) {
        return getNextPlantTarget(farm) != null;
    }

    public int runPlantProgram(EntityTreeFarm farm) {
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
    public boolean hasAnyValidSeedItem(EntityTreeFarm farm) {
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stackInSlot = worker.combinedInventory.getStackInSlot(i);
            if (isValidSeedItemForFarm(farm, stackInSlot)) return true;
        }
        return false;
    }

    public ItemStack takeOneSeedFromFarm(EntityTreeFarm farm, boolean simulate) {
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

    public boolean shouldTakeSeedsFromFarm(EntityTreeFarm farm) {

        // only take 1 sapling at a time
            if (hasAnyValidSeedItem(farm))
                return false;


        // check if it can not pick up any seeds from the farm because inventory full or no seeds in farm, do not run
        if (takeOneSeedFromFarm(farm, true) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }
    public int runTakeSeedProgram(EntityTreeFarm farm) {

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



    public boolean recalculateHasWork(EntityTreeFarm target) {

        hasWorkHarvest = canHarvestFarm(target);
        hasWorkPlant = canPlantAtFarm(target);
        hasWorkTakeSeeds = shouldTakeSeedsFromFarm(target);

        boolean canPickupDrops = false;
        // if there are items on the ground
        if(Utils.countEmptySlots(worker) > 0) {
            List<ItemEntity> entitiesOnGround = worker.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(target.pmin.getCenter(), target.pmax.getCenter()).inflate(1));
            if (!entitiesOnGround.isEmpty()) {
                for (ItemEntity i : entitiesOnGround) {
                    if (!worker.slowMobNavigation.isPositionCachedAsInvalid(i.getOnPos())) {
                        canPickupDrops = true;
                        break;
                    }
                }
            }
        }

        hasWork = hasWorkHarvest || hasWorkPlant || hasWorkTakeSeeds || canPickupDrops;
        hasWork = hasWork && isPositionWorkable(target.getBlockPos()) && worker.hunger > worker.maxHunger * 0.25;
        return hasWork;
    }

    public int run(EntityTreeFarm farm) {

        long gameTime = worker.level().getGameTime();
        if (gameTime > lastScan + scanInterval) {
            lastScan = gameTime;
            recalculateHasWork(farm);
        }

        if (!hasWork) {
            return EXIT_SUCCESS;
        }

        if (hasWorkHarvest)
            return runHarvestProgram(farm);

        if (hasWorkTakeSeeds)
            return runTakeSeedProgram(farm);

        if (hasWorkPlant)
            return runPlantProgram(farm);


        // if there are items on the ground
        if (Utils.countEmptySlots(worker) > 0) {
            List<ItemEntity> entitiesOnGround = worker.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(farm.pmin.getCenter(), farm.pmax.getCenter()).inflate(1));
            if (!entitiesOnGround.isEmpty()) {
                for (ItemEntity item : entitiesOnGround) {
                    if (!worker.slowMobNavigation.isPositionCachedAsInvalid(item.getOnPos())) {

                        int pathFindExit = worker.slowMobNavigation.moveToPosition(
                                item.getOnPos(),
                                1,
                                worker.slowNavigationMaxDistance,
                                worker.slowNavigationMaxNodes,
                                worker.slowNavigationStepPerTick
                        );

                        if (pathFindExit == EXIT_FAIL) {
                            // hm... bad
                            continue;
                        } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                            workDelay = 0;
                            return SUCCESS_STILL_RUNNING;
                        }

                        // at this point the pickup items program should continue
                        return SUCCESS_STILL_RUNNING;
                    }
                }
            }
        }
        recalculateHasWork(farm);
        return SUCCESS_STILL_RUNNING;
    }

    public int runTakeAxeFromFarmAnyInventory(EntityTreeFarm farm) {
        // first take from main inventory
        int takeHoeExit = takeAxeProgram.run(AxeItem.class, farm.getBlockPos(), farm.mainInventory);
        // if no tool there, take from inputs inventory
        if (takeHoeExit == -2)
            takeHoeExit = takeAxeProgram.run(AxeItem.class, farm.getBlockPos(), farm.inputsInventory);
        // if still no tool there, take from special resources inventory
        if (takeHoeExit == -2)
            takeHoeExit = takeAxeProgram.run(AxeItem.class, farm.getBlockPos(), farm.specialResourcesInventory);
        // if it is still tool not found, the program failed to get a hoe from any farm inventory
        if (takeHoeExit == -2)
            return EXIT_FAIL;

        return takeHoeExit;
    }
}
