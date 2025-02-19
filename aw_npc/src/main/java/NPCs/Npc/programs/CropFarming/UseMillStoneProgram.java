package NPCs.Npc.programs.CropFarming;

import AOSWorkshopExpansion.MillStone.EntityMillStone;
import AOSWorkshopExpansion.MillStone.MillStoneConfig;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.utils.ItemUtils;
import NPCs.Npc.WorkerNPC;
import NPCs.Utils;
import WorkSites.CropFarm.EntityCropFarm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Objects;

import static NPCs.Utils.*;
import static NPCs.Utils.SUCCESS_STILL_RUNNING;


public class UseMillStoneProgram {

    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    WorkerNPC worker;
    EntityMillStone currentMillstone;
    int workDelay = 0;
    int scanInterval = 20 * 20;
    long lastScan = 0;
    boolean canTakeOutputs = false;
    ItemStack canPutInputsFromInventory = ItemStack.EMPTY;
    ItemStack canPutInputsFromFarm = ItemStack.EMPTY;
    int stackSizeToTakeFromFarm = 16;
    int requiredDistance = 2;
    int requiredDistanceToMillStone = 3;
    boolean hasWork;

    public UseMillStoneProgram(WorkerNPC worker) {
        this.worker = worker;
    }


    public void lockTargetPosition() {
        long gameTime = worker.level().getGameTime();
        positionsInUseWithLastUseTime.put(currentMillstone.getBlockPos(), gameTime);
    }

    public boolean isPositionLocked(BlockPos p) {
        // if I lock the position, it is not locked for ME, only for OTHER WORKERS
        if(currentMillstone != null)
            if (Objects.equals(p, currentMillstone.getBlockPos())) return false;

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

    public static boolean isItemValidRecipeOutput(ItemStack item) {
        for (MillStoneConfig.MillStoneRecipe r : MillStoneConfig.INSTANCE.recipes) {
            if (ItemUtils.matches(r.outputItem.id, item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isItemValidRecipeInput(ItemStack item) {
        for (MillStoneConfig.MillStoneRecipe r : MillStoneConfig.INSTANCE.recipes) {
            if (ItemUtils.matches(r.inputItem.id, item)) {
                return true;
            }
        }
        return false;
    }


    public static ItemStack takeOneValidMillStoneInputFromFarm(ItemStack stackToTakeFrom, EntityCropFarm farm, WorkerNPC worker, boolean simulate) {
        for (int i = 0; i < farm.mainInventory.getSlots(); i++) {
            ItemStack canExtract = farm.mainInventory.extractItem(i, 1, true);
            if (!canExtract.isEmpty() && isItemValidRecipeInput(canExtract) && stackToTakeFrom.getItem().equals(canExtract.getItem())) {
                for (int j = 0; j < worker.combinedInventory.getSlots(); j++) {
                    ItemStack notInserted = worker.combinedInventory.insertItem(j, canExtract, true);
                    if (notInserted.isEmpty()) {
                        if (!simulate) {
                            ItemStack extracted = farm.mainInventory.extractItem(i, 1, false);
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


    public static ItemStack unloadOneItemIntoMillstone(EntityMillStone millstone, WorkerNPC worker, boolean simulate) {
        IItemHandler inventory = worker.combinedInventory;
        for (int j = 0; j < inventory.getSlots(); j++) {
            ItemStack canExtract = inventory.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = inventory.getStackInSlot(j).copy();
            if (!canExtract.isEmpty() && isItemValidRecipeInput(canExtract)) {

                for (int i = 0; i < millstone.inventory.getSlots(); i++) {
                    ItemStack notInserted = millstone.inventory.insertItem(i, canExtract, true);
                    if (notInserted.isEmpty()) {
                        if (!simulate) {
                            if (worker.getMainHandItem().getItem().equals(canExtract.getItem()))
                                worker.swing(InteractionHand.MAIN_HAND);
                            else if (worker.getOffhandItem().getItem().equals(canExtract.getItem()))
                                worker.swing(InteractionHand.OFF_HAND);

                            ItemStack extracted = inventory.extractItem(j, 1, false);
                            millstone.inventory.insertItem(i, extracted, false);
                        }
                        return stackCopyToReturn;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getPossibleMillstoneInput(IItemHandler inventory, EntityMillStone millstone, int requiredMinStockInInvventory) {
        for (int j = 0; j < inventory.getSlots(); j++) {
            ItemStack canExtract = inventory.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = inventory.getStackInSlot(j).copy();
            if (!canExtract.isEmpty() && isItemValidRecipeInput(canExtract) && Utils.countItems(canExtract.getItem(), inventory) > requiredMinStockInInvventory) {

                for (int i = 0; i < millstone.inventory.getSlots(); i++) {
                    ItemStack notInserted = millstone.inventory.insertItem(i, canExtract, true);
                    if (notInserted.isEmpty()) {
                        return stackCopyToReturn;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean takeItemOutOfMillStone(EntityMillStone millStone, WorkerNPC worker, boolean simulate) {
        for (int i = 0; i < millStone.inventory.getSlots(); i++) {
            ItemStack canExtract = millStone.inventory.extractItem(i, 1, true);

            if (!canExtract.isEmpty() && isItemValidRecipeOutput(canExtract)) {
                for (int j = 0; j < worker.combinedInventory.getSlots(); j++) {
                    ItemStack notInserted = worker.combinedInventory.insertItem(j, canExtract, true);
                    if (notInserted.isEmpty()) {
                        if (!simulate) {
                            ItemStack extracted = millStone.inventory.extractItem(i, 1, false);
                            worker.combinedInventory.insertItem(j, extracted, false);
                            worker.swing(Utils.moveItemStackToAnyHand(worker.combinedInventory.getStackInSlot(j), worker));
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }


    static class workinfo{
        public boolean canTakeOutputs = false;
        public ItemStack canPutInputsFromFarm = ItemStack.EMPTY;
        public ItemStack canPutInputsFromInventory = ItemStack.EMPTY;
    }

    public workinfo recalculateWorkForMillStone(EntityCropFarm farm, EntityMillStone millstone) {

        workinfo w = new workinfo();

        if (worker.hunger < worker.maxHunger * 0.25) {
            return new workinfo();
        }

        if (!millstone.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            return new workinfo();
        }
        if (!isPositionWorkable(millstone.getBlockPos())) {
            return new workinfo();
        }

        w.canTakeOutputs = takeItemOutOfMillStone(millstone, worker, true);

        // only take from farm if more than 64 are present
        w.canPutInputsFromFarm = getPossibleMillstoneInput(farm.mainInventory, millstone, 64);

        // if the farm is not stocked up with enough inputs, do not put into millstone but into farm first.
        // so count if the farm has enough stock
        w.canPutInputsFromInventory = unloadOneItemIntoMillstone(millstone, worker, true);
        if (Utils.countItems(w.canPutInputsFromInventory.getItem(), farm.mainInventory) < 64) {
            w.canPutInputsFromInventory = ItemStack.EMPTY;
        }

        // if we can put inputs from farm check if the worker can take from farm ( in case inventory full)
        if (!w.canPutInputsFromFarm.isEmpty()) {
            if (takeOneValidMillStoneInputFromFarm(w.canPutInputsFromFarm, farm, worker, true).isEmpty()) {
                w.canPutInputsFromFarm = ItemStack.EMPTY;
            }
        }

        // it is a problem that the farmer unloads one item at the farm and then he notices that he can pick up from millstone
        // and he will run from farm to millstone and every time only unload 1 item at the farm if he reaches the farm
        // if the distance is longer sometimes he will not even reach the farm and turn back half way because more items are processed and can be picked up
        // so i make it like this:
        // if the worker already HAS output items and is NOT near the millstone, assume he is on its way to bring the items to the farm
        // in this case, ignore that he can pick up items from the millstone
        if (Utils.distanceManhattan(worker, millstone.getBlockPos().getCenter()) > 5) {
            for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
                if (isItemValidRecipeOutput(worker.combinedInventory.getStackInSlot(i))) {
                    w.canTakeOutputs = false;
                    break;
                }
            }
        }
        return w;
    }


    public boolean recalculateHasWork(EntityCropFarm farm) {

        canTakeOutputs = false;
        canPutInputsFromFarm = ItemStack.EMPTY;
        canPutInputsFromInventory = ItemStack.EMPTY;
        hasWork = false;

        if(!isPositionWorkable(farm.getBlockPos()) ||  worker.hunger < worker.maxHunger * 0.25){
            return false;
        }

        if(currentMillstone != null) {
            workinfo w = recalculateWorkForMillStone(farm, currentMillstone);
            hasWork = !w.canPutInputsFromFarm.isEmpty() || w.canTakeOutputs || !w.canPutInputsFromInventory.isEmpty();
            if (!hasWork) currentMillstone = null;
            else {
                canTakeOutputs = w.canTakeOutputs;
                canPutInputsFromFarm = w.canPutInputsFromFarm;
                canPutInputsFromInventory = w.canPutInputsFromInventory;
                lockTargetPosition();
            }
            return hasWork;
        } else {
            for (BlockPos p : Utils.sortBlockPosByDistanceToNPC(EntityMillStone.knownBlockEntities, farm.getBlockPos().getCenter())) {
                if (Utils.distanceManhattan(farm.getBlockPos().getCenter(), p.getCenter()) > farm.useMillStonesInRadius)
                    break;
                BlockEntity be = worker.level().getBlockEntity(p);
                if (be instanceof EntityMillStone millStone) {
                    workinfo w = recalculateWorkForMillStone(farm, millStone);
                    boolean _hasWork = !w.canPutInputsFromFarm.isEmpty() || w.canTakeOutputs || !w.canPutInputsFromInventory.isEmpty();
                    if (_hasWork){
                        hasWork = true;
                        currentMillstone = millStone;
                        canTakeOutputs = w.canTakeOutputs;
                        canPutInputsFromFarm = w.canPutInputsFromFarm;
                        canPutInputsFromInventory = w.canPutInputsFromInventory;
                        lockTargetPosition();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int run(EntityCropFarm farm) {

        long gameTime = worker.level().getGameTime();
        if (gameTime > lastScan + scanInterval) {
            lastScan = gameTime;
            recalculateHasWork(farm);
        }

        if (currentMillstone == null) return EXIT_SUCCESS;
        if (!currentMillstone.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            currentMillstone = null;
            return EXIT_SUCCESS;
        }

        // block millstone from beeing used by others
        lockTargetPosition();

        // first try to take a batch of items from the farm to carry to millstone
        // note that it can cause the worker to deposit one item in millstone and run back to farm to restock the batch
        // because of this, only consider this if we are more than x block away from millstone or when we do not have any input in inventory
        if (!canPutInputsFromFarm.isEmpty() && (Utils.distanceManhattan(worker, currentMillstone.getBlockPos().getCenter()) > 10 || canPutInputsFromInventory.isEmpty())) {
            // count how many items of insertable type i have in inventory already do decide if i should take more
            // i may have more items to insert in inventory because both only return the first valid item but this is not a problem
            int itemsToInsertTotal =
                    (Utils.countItems(canPutInputsFromFarm.getItem(), worker.combinedInventory)
                            + Utils.countItems(canPutInputsFromInventory.getItem(), worker.combinedInventory)) / 2; // because it can count double

            if (itemsToInsertTotal < stackSizeToTakeFromFarm) {

                int pathFindExit = worker.slowMobNavigation.moveToPosition(
                        farm.getBlockPos(),
                        requiredDistance,
                        worker.slowNavigationMaxDistance,
                        worker.slowNavigationMaxNodes,
                        worker.slowNavigationStepPerTick
                );


                if (pathFindExit == EXIT_FAIL) {
                    recalculateHasWork(farm);
                    return SUCCESS_STILL_RUNNING;
                } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                    workDelay = 0;
                    return SUCCESS_STILL_RUNNING;
                }

                worker.lookAt(EntityAnchorArgument.Anchor.EYES, farm.getBlockPos().getCenter());
                worker.lookAt(EntityAnchorArgument.Anchor.FEET, farm.getBlockPos().getCenter());

                if (workDelay > 10) {
                    workDelay = 0;
                    takeOneValidMillStoneInputFromFarm(canPutInputsFromFarm,farm, worker, false);
                    recalculateHasWork(farm);
                }
                workDelay++;

                return SUCCESS_STILL_RUNNING;
            }
        }


        // now carry the items to millstone
        if (!canPutInputsFromInventory.isEmpty()) {

            //take the item in hand, just for visuals
            if (!ItemStack.isSameItemSameComponents(worker.getMainHandItem(), canPutInputsFromInventory) &&
                    !ItemStack.isSameItemSameComponents(worker.getOffhandItem(), canPutInputsFromInventory)) {
                Utils.moveItemStackToAnyHand(canPutInputsFromInventory, worker);
            }

            int pathFindExit = worker.slowMobNavigation.moveToPosition(
                    currentMillstone.getBlockPos(),
                    requiredDistanceToMillStone,
                    worker.slowNavigationMaxDistance,
                    worker.slowNavigationMaxNodes,
                    worker.slowNavigationStepPerTick
            );


            if (pathFindExit == EXIT_FAIL) {
                recalculateHasWork(farm);
                return SUCCESS_STILL_RUNNING;
            } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                workDelay = 0;
                return SUCCESS_STILL_RUNNING;
            }

            worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentMillstone.getBlockPos().getCenter());
            worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentMillstone.getBlockPos().getCenter());

            if (workDelay > 10) {
                workDelay = 0;
                unloadOneItemIntoMillstone(currentMillstone, worker, false);
                recalculateHasWork(farm);
            }
            workDelay++;
            return SUCCESS_STILL_RUNNING;
        }


        // now check if outputs can be taken out of millstone
        if (canTakeOutputs) {

            int pathFindExit = worker.slowMobNavigation.moveToPosition(
                    currentMillstone.getBlockPos(),
                    requiredDistanceToMillStone,
                    worker.slowNavigationMaxDistance,
                    worker.slowNavigationMaxNodes,
                    worker.slowNavigationStepPerTick
            );


            if (pathFindExit == EXIT_FAIL) {
                recalculateHasWork(farm);
                return SUCCESS_STILL_RUNNING;
            } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                workDelay = 0;
                return SUCCESS_STILL_RUNNING;
            }
            worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentMillstone.getBlockPos().getCenter());
            worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentMillstone.getBlockPos().getCenter());

            if (workDelay > 10) {
                workDelay = 0;
                takeItemOutOfMillStone(currentMillstone, worker, false);
                recalculateHasWork(farm);
            }
            workDelay++;

            return SUCCESS_STILL_RUNNING;
        }

        return EXIT_SUCCESS;
    }
}
