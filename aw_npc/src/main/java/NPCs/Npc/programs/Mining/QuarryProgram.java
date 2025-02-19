package NPCs.Npc.programs.Mining;

import NPCs.Npc.WorkerNPC;
import NPCs.Utils;
import NPCs.Npc.programs.TakeToolProgram;
import WorkSites.Quarry.EntityQuarry;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static NPCs.Utils.*;

public class QuarryProgram {
    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    // destroy progress seems to go from 0 to 10 and -1 is remove block
    float blockDestroyProgress;
    int lastDestroyProgressUpdated;

    public WorkerNPC worker;
    int scanInterval = 20 * 20;
    int requiredDistanceToPositionToWork = 3;
    int requiredFreeSlotsToHarvest = 2;

    public BlockPos currentTargetPos = null;
    long lastScan = 0;
    int workDelay = 0;
    boolean hasWork = false;

    TakeToolProgram takePickAxeProgram;

    public QuarryProgram(WorkerNPC worker) {
        this.worker = worker;
        takePickAxeProgram = new TakeToolProgram(worker);
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


    public static TreeSet<BlockPos> sortPositionsToMine(TreeSet<BlockPos> list, Entity e) {
        Vec3 position = e.getPosition(0).add(0,1,0);

        TreeSet<BlockPos> sorted = new TreeSet<>(new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos o1, BlockPos o2) {
                // priority on higher y level blocks
                double d1 = o1.getCenter().distanceTo(position)-o1.getY()*4;
                double d2 = o2.getCenter().distanceTo(position)-o2.getY()*4;
                if (d1 > d2) return 1;
                if (d1 < d2) return -1;
                else {
                    if(o1.getY() != o2.getY()) return (int) Math.signum(o1.getY() - o2.getY());
                    else if(o1.getX() != o2.getX()) return (int) Math.signum(o1.getX() - o2.getX());
                    else if(o1.getZ() != o2.getZ()) return (int) Math.signum(o1.getZ() - o2.getZ());
                }
                return 0;
            }
        });
        int startY = list.getFirst().getY();
        for (BlockPos p : list){
            if(p.getY() + 3 >= startY){
                sorted.add(p);
            }else break;
        }
        return sorted;
    }

    public BlockPos getNextHarvestTargetFromFarm(EntityQuarry farm) {

        if (farm.blocksToMine.isEmpty()) return null;
        for (BlockPos i : sortPositionsToMine(farm.blocksToMine, worker)) {
            if (isPositionWorkable(i)) {
                BlockState state = farm.getLevel().getBlockState(i);
                if (state.requiresCorrectToolForDrops()) {
                    if (
                            takePickAxeProgram.hasToolForDrops(state) ||
                                    (takePickAxeProgram.pickupCorrectToolForDrops(state, farm.mainInventory, true) ||
                                            takePickAxeProgram.pickupCorrectToolForDrops(state, farm.inputsInventory, true) ||
                                            takePickAxeProgram.pickupCorrectToolForDrops(state, farm.specialResourcesInventory, true)
                                    )
                    ) {
                        return i;
                    }
                } else
                    return i;
            }
        }
        return null;
    }

boolean canQuarry(EntityQuarry target){

    if(getNextHarvestTargetFromFarm(target) == null)
        return false;

    int numEmptySlotsIgnorePickAxe = 0;
    for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
        if (worker.combinedInventory.getStackInSlot(i).isEmpty() ||
                worker.combinedInventory.getStackInSlot(i).getItem() instanceof PickaxeItem)
            numEmptySlotsIgnorePickAxe++;
    }
    if (Utils.distanceManhattan(worker, target.getBlockPos().getCenter()) > 7) {
        if (numEmptySlotsIgnorePickAxe < requiredFreeSlotsToHarvest) return false;
    } else {
        if (numEmptySlotsIgnorePickAxe < requiredFreeSlotsToHarvest + 5) return false;
    }

    return true;
}

    public int runQuarryProgram(EntityQuarry farm) {


        if (currentTargetPos != null && farm.blocksToMine.contains(currentTargetPos)) {
            // lock the target so no other worker goes there
            lockTargetPosition();

            BlockState stateToMine = farm.getLevel().getBlockState(currentTargetPos);
            if(stateToMine.isAir()){
                farm.blocksToMine.remove(currentTargetPos);
                return SUCCESS_STILL_RUNNING;
            }

            int takePickExit = runTakePickFromFarmAnyInventory(farm, stateToMine);
            if (takePickExit == SUCCESS_STILL_RUNNING){
                return SUCCESS_STILL_RUNNING;
            }
            if (takePickExit == EXIT_FAIL) {
                recalculateHasWork(farm);
                return EXIT_FAIL;
            }

            // take the tool to main hand
            takePickAxeProgram.takeToolForDropsToMainHand(stateToMine);

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
                lastDestroyProgressUpdated=0;
                blockDestroyProgress = 0;
                return SUCCESS_STILL_RUNNING;
            }

            worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentTargetPos.getCenter());
            worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentTargetPos.getCenter());

            workDelay++;
            if (workDelay > 20) {

                worker.swing(InteractionHand.MAIN_HAND);
                BlockState s = worker.level().getBlockState(currentTargetPos);
                float destroyTime = s.getDestroySpeed(worker.level(),currentTargetPos);
                float destroySpeed = worker.getMainHandItem().getDestroySpeed(s);
                blockDestroyProgress+=destroySpeed / 100f;

                int relativeScaledProgress = (int) (10 * blockDestroyProgress / destroyTime);
                if(relativeScaledProgress != lastDestroyProgressUpdated){
                    lastDestroyProgressUpdated = relativeScaledProgress;
                    worker.level().destroyBlockProgress(worker.getId(),currentTargetPos,relativeScaledProgress);
                }


                if(relativeScaledProgress >= 10){
                    LootParams.Builder b = (new LootParams.Builder((ServerLevel) worker.level())).withParameter(LootContextParams.TOOL, worker.getMainHandItem()).withParameter(LootContextParams.ORIGIN, worker.getPosition(0));
                    List<ItemStack> drops = s.getDrops(b);
                    for (ItemStack i : drops) {
                        for (int j = 0; j < worker.combinedInventory.getSlots(); ++j) {
                            i = worker.combinedInventory.insertItem(j, i, false);
                        }
                    }
                    worker.level().destroyBlock(currentTargetPos, false);

                    Utils.damageMainHandItem(worker);

                    workDelay = 0;
                    farm.blocksToMine.remove(currentTargetPos);
                    lastDestroyProgressUpdated = 0;
                    blockDestroyProgress= 0;

                    worker.level().destroyBlockProgress(worker.getId(),currentTargetPos,-1);

                    recalculateHasWork(farm);
                }
            }
            return SUCCESS_STILL_RUNNING;
        }
        lastDestroyProgressUpdated=0;
        blockDestroyProgress = 0;
        currentTargetPos = getNextHarvestTargetFromFarm(farm);
        recalculateHasWork(farm);
        return SUCCESS_STILL_RUNNING;
    }

    public boolean recalculateHasWork(EntityQuarry target) {
        hasWork = canQuarry(target);
        hasWork = hasWork && isPositionWorkable(target.getBlockPos()) && worker.hunger > worker.maxHunger * 0.25;
        return hasWork;
    }

    public int run(EntityQuarry farm) {

        long gameTime = worker.level().getGameTime();
        if (gameTime > lastScan + scanInterval) {
            lastScan = gameTime;
            recalculateHasWork(farm);
        }

        if (!hasWork) {
            return EXIT_SUCCESS;
        }

        return runQuarryProgram(farm);

        //return EXIT_SUCCESS;
    }

    public int runTakePickFromFarmAnyInventory(EntityQuarry farm, BlockState stateToMine) {
        // first take from main inventory
        int takeHoeExit = takePickAxeProgram.run(stateToMine, farm.getBlockPos(), farm.mainInventory);
        // if no tool there, take from inputs inventory
        if (takeHoeExit == -2)
            takeHoeExit = takePickAxeProgram.run(stateToMine, farm.getBlockPos(), farm.inputsInventory);
        // if still no tool there, take from special resources inventory
        if (takeHoeExit == -2)
            takeHoeExit = takePickAxeProgram.run(stateToMine, farm.getBlockPos(), farm.specialResourcesInventory);
        // if it is still tool not found, the program failed to get a hoe from any farm inventory
        if (takeHoeExit == -2)
            return EXIT_FAIL;

        return takeHoeExit;
    }
}
