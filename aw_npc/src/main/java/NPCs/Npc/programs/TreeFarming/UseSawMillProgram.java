package NPCs.Npc.programs.TreeFarming;

import AOSWorkshopExpansion.WoodMill.EntityWoodMill;
import AOSWorkshopExpansion.WoodMill.WoodMillConfig;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.ItemUtils;
import ARLib.utils.RecipePartWithProbability;
import NPCs.Npc.WorkerNPC;
import NPCs.Utils;
import WorkSites.TreeFarm.EntityTreeFarm;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static NPCs.Utils.*;


public class UseSawMillProgram {

    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    WorkerNPC worker;
    EntityWoodMill currentWoodmill;
    int workDelay = 0;
    int scanInterval = 20 * 20;
    long lastScan = 0;
    boolean takeOutput = false;
    ItemStack canPutInputsFromInventory = ItemStack.EMPTY;
    ItemStack canPutInputsFromFarm = ItemStack.EMPTY;
    int requiredDistance = 2;
    int requiredDistanceToMill = 3;
    boolean hasWork;

    public UseSawMillProgram(WorkerNPC worker) {
        this.worker = worker;
    }


    public void lockTargetPosition() {
        long gameTime = worker.level().getGameTime();
        positionsInUseWithLastUseTime.put(currentWoodmill.getBlockPos(), gameTime);
    }

    public boolean isPositionLocked(BlockPos p) {
        // if I lock the position, it is not locked for ME, only for OTHER WORKERS
        if (currentWoodmill != null)
            if (Objects.equals(p, currentWoodmill.getBlockPos())) return false;

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


    public static boolean isItemValidRecipeInput(ItemStack item) {
        return EntityWoodMill.getRecipeForInputs(item) != null;
    }

    public static boolean isItemValidRecipeOutput(ItemStack item) {
        for(WoodMillConfig.WoodMillRecipe r : WoodMillConfig.INSTANCE.recipes){
            for(RecipePartWithProbability i : r.outputItems){
                if(ItemUtils.matches(i.id,item)){
                    return true;
                }
            }
        }
        return false;
    }


    public static ItemStack takeOneValidWoodmillInputFromFarm(ItemStack stackToTakeFrom, EntityTreeFarm farm, WorkerNPC worker, boolean simulate) {
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


    public static ItemStack unloadOneItemIntoWoodmill(EntityWoodMill mill, WorkerNPC worker, boolean simulate) {
        IItemHandler inventory = worker.combinedInventory;
        for (int j = 0; j < inventory.getSlots(); j++) {
            ItemStack canExtract = inventory.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = inventory.getStackInSlot(j).copy();
            if (!canExtract.isEmpty() && isItemValidRecipeInput(canExtract)) {
                if (!simulate) {
                    if (mill.canFitInput()) {
                        if (worker.getMainHandItem().getItem().equals(canExtract.getItem()))
                            worker.swing(InteractionHand.MAIN_HAND);
                        else if (worker.getOffhandItem().getItem().equals(canExtract.getItem()))
                            worker.swing(InteractionHand.OFF_HAND);

                        ItemStack extracted = inventory.extractItem(j, 1, false);
                        mill.tryAddInput(extracted);

                        return stackCopyToReturn;
                    }
                } else {
                    return stackCopyToReturn;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getPossibleWoodmillInput(IItemHandler inventory, EntityWoodMill mill, int requiredMinStockInInventory) {
        for (int j = 0; j < inventory.getSlots(); j++) {
            ItemStack canExtract = inventory.extractItem(j, 1, true);
            ItemStack stackCopyToReturn = inventory.getStackInSlot(j).copy();
            if (!canExtract.isEmpty() &&
                    isItemValidRecipeInput(canExtract) &&
                    Utils.countItems(canExtract.getItem(), inventory) > requiredMinStockInInventory) {
                return stackCopyToReturn;
            }
        }
        return ItemStack.EMPTY;
    }

    static class workinfo {
        public ItemStack canPutInputsFromFarm = ItemStack.EMPTY;
        public ItemStack canPutInputsFromInventory = ItemStack.EMPTY;
        public boolean takeOutput = false;
    }

    public workinfo recalculateWorkForWoodmill(EntityTreeFarm farm, EntityWoodMill mill) {
        workinfo w = new workinfo();

        // it can happen that because a worker goes eat or sleep, another worker will start working
        // this is usually not a problem for other worksite, but on the woodmill they will wait until they can input wood
        // this can cause workers to get stuck waiting forever at a woodmill
        // so check if other workers are nearby that are currently working on this mill
        List<WorkerNPC> workersAround = worker.level().getEntitiesOfClass(WorkerNPC.class,new AABB(mill.getBlockPos()).inflate(10));
        for (WorkerNPC i : workersAround)
            if(i.getId() != worker.getId())
                if(i.lumberjackProgram != null)
                    if(i.lumberjackProgram.useSawMillProgram != null)
                        if(i.lumberjackProgram.useSawMillProgram.currentWoodmill != null)
                            if(i.lumberjackProgram.useSawMillProgram.currentWoodmill.getBlockPos().equals(mill.getBlockPos())) {
                                //System.out.println("other worker has woodmill");
                                return w;
                            }



        if (worker.hunger < worker.maxHunger * 0.25) {
            return w;
        }

        // we need some space to fit the outputs, so count empty slots
        int numEmptySlotsIgnoreFirstInputStack = 0;
        boolean hadFoundInput = false;
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            if (worker.combinedInventory.getStackInSlot(i).isEmpty() ||
                    (!hadFoundInput && isItemValidRecipeInput(worker.combinedInventory.getStackInSlot(i))))
                numEmptySlotsIgnoreFirstInputStack++;
            if (isItemValidRecipeInput(worker.combinedInventory.getStackInSlot(i))) {
                hadFoundInput = true;
            }
        }
        if (Utils.distanceManhattan(worker, mill.getBlockPos().getCenter()) > 5) {
            if (numEmptySlotsIgnoreFirstInputStack < 3) return w;
        } else {
            if (numEmptySlotsIgnoreFirstInputStack < 5) return w;
        }

        if (!mill.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            return w;
        }
        if (!isPositionWorkable(mill.getBlockPos())) {
            return w;
        }

        // if there are items on the ground or the mill is moving and has recipes working, take outputs / wait for them to finish
        List<ItemEntity> entitiesOnGround =worker.level().getEntitiesOfClass(ItemEntity.class, new AABB(mill.getBlockPos()).inflate(5));
        for (ItemEntity e : entitiesOnGround){
            if(isItemValidRecipeOutput(e.getItem())){
                w.takeOutput = true;
                break;
            }
        }

        // if not moving, there is nothing to do except pick up items on the ground
        if (!(Math.abs(mill.myMechanicalBlock.internalVelocity) > 0)) {
            return w;
        }


        w.canPutInputsFromInventory = unloadOneItemIntoWoodmill(mill, worker, true);

        if (w.canPutInputsFromInventory.isEmpty()) {
            // only take from farm if more than 64 are present and if not a valid input already exists in inventory
            w.canPutInputsFromFarm = getPossibleWoodmillInput(farm.mainInventory, mill, 64);
        }

        // if the farm is not stocked up with enough inputs, do not put into mill but into farm first.
        // so count if the farm has enough stock
        if (Utils.countItems(w.canPutInputsFromInventory.getItem(), farm.mainInventory) < 64) {
            w.canPutInputsFromInventory = ItemStack.EMPTY;
        }

        // if we can put inputs from farm check if the worker can take from farm ( in case inventory full)
        if (!w.canPutInputsFromFarm.isEmpty()) {
            if (takeOneValidWoodmillInputFromFarm(w.canPutInputsFromFarm,farm, worker, true).isEmpty()) {
                w.canPutInputsFromFarm = ItemStack.EMPTY;
            }
        }

        return w;
    }


    public boolean recalculateHasWork(EntityTreeFarm farm) {

        takeOutput = false;
        canPutInputsFromFarm = ItemStack.EMPTY;
        canPutInputsFromInventory = ItemStack.EMPTY;
        hasWork = false;

        if(!isPositionWorkable(farm.getBlockPos()) ||  worker.hunger < worker.maxHunger * 0.25){
            return false;
        }
        if (currentWoodmill != null) {
            if(currentWoodmill.isRemoved())currentWoodmill = null;
        }
        if (currentWoodmill != null) {
            workinfo w = recalculateWorkForWoodmill(farm, currentWoodmill);
            hasWork = !w.canPutInputsFromFarm.isEmpty() || w.takeOutput || !w.canPutInputsFromInventory.isEmpty();
            if (!hasWork) currentWoodmill = null;
            else {
                takeOutput = w.takeOutput;
                canPutInputsFromFarm = w.canPutInputsFromFarm;
                canPutInputsFromInventory = w.canPutInputsFromInventory;
                lockTargetPosition();
            }
            //System.out.println(takeOutput+":"+canPutInputsFromFarm+":"+canPutInputsFromInventory);
            return hasWork;
        } else {
            List<BlockPos> knownSawmills = new ArrayList<>();
            for(BlockIdentifier identifier : EntityWoodMill.knownBlockEntities){
                if(identifier.level == worker.level())
                    knownSawmills.add(identifier.pos);
            }
            for (BlockPos p : Utils.sortBlockPosByDistanceToVec(knownSawmills, farm.getBlockPos().getCenter())) {
                if (Utils.distanceManhattan(farm.getBlockPos().getCenter(), p.getCenter()) > farm.useWoodmillsInRadius)
                    break;
                BlockEntity be = worker.level().getBlockEntity(p);
                if (be instanceof EntityWoodMill mill) {
                    workinfo w = recalculateWorkForWoodmill(farm, mill);
                    boolean _hasWork = !w.canPutInputsFromFarm.isEmpty() || w.takeOutput || !w.canPutInputsFromInventory.isEmpty();
                    if (_hasWork) {
                        hasWork = true;
                        currentWoodmill = mill;
                        takeOutput = w.takeOutput;
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

    public int run(EntityTreeFarm farm) {

        long gameTime = worker.level().getGameTime();
        if (gameTime > lastScan + scanInterval) {
            lastScan = gameTime;
            recalculateHasWork(farm);
        }

        if (currentWoodmill == null) return EXIT_SUCCESS;
        if (!currentWoodmill.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            currentWoodmill = null;
            return EXIT_SUCCESS;
        }


        // block mill from beeing used by others
        lockTargetPosition();

        if (takeOutput) {
            // wait for or take outputs
            List<ItemEntity> itemsOnGround = worker.level().getEntitiesOfClass(ItemEntity.class, new AABB(currentWoodmill.getBlockPos()).inflate(5));

            if (!itemsOnGround.isEmpty()) {
                for (ItemEntity item : itemsOnGround) {
                    if(!isItemValidRecipeOutput(item.getItem())) continue;

                    int pathFindExit = worker.slowMobNavigation.moveToPosition(
                            item.getOnPos(),
                            requiredDistance,
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

        if (!canPutInputsFromInventory.isEmpty()) {

            //take the item in hand, just for visuals
            if (!ItemStack.isSameItemSameComponents(worker.getMainHandItem(), canPutInputsFromInventory) &&
                    !ItemStack.isSameItemSameComponents(worker.getOffhandItem(), canPutInputsFromInventory)) {
                Utils.moveItemStackToAnyHand(canPutInputsFromInventory, worker);
            }

            int pathFindExit = worker.slowMobNavigation.moveToPosition(
                    currentWoodmill.getBlockPos().relative(currentWoodmill.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)),
                    requiredDistanceToMill,
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

            worker.lookAt(EntityAnchorArgument.Anchor.EYES, currentWoodmill.getBlockPos().getCenter());
            worker.lookAt(EntityAnchorArgument.Anchor.FEET, currentWoodmill.getBlockPos().getCenter());

            if (workDelay > 10) {
                if (currentWoodmill.canFitInput()) {
                    workDelay = 0;
                    unloadOneItemIntoWoodmill(currentWoodmill, worker, false);
                    recalculateHasWork(farm);
                }
            }
            workDelay++;
            return SUCCESS_STILL_RUNNING;
        }

        if (!canPutInputsFromFarm.isEmpty()) {

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
                    takeOneValidWoodmillInputFromFarm(canPutInputsFromFarm,farm, worker, false);
                    recalculateHasWork(farm);
                }
                workDelay++;

                return SUCCESS_STILL_RUNNING;

        }
        recalculateHasWork(farm);
        return SUCCESS_STILL_RUNNING;
    }
}
