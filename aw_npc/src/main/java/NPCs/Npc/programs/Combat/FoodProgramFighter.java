package NPCs.Npc.programs.Combat;

import NPCs.Items.ItemFoodOrder;
import NPCs.Npc.NPCBase;
import NPCs.Blocks.TownHall.EntityTownHall;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumSet;

import static NPCs.Utils.EXIT_FAIL;
import static NPCs.Utils.SUCCESS_STILL_RUNNING;

public class FoodProgramFighter extends Goal {
    NPCBase worker;
    BlockPos target;
    IItemHandler inventoryTarget;
    int workDelay = 0;
    boolean canUse;
    int minStock = 3;
    long lastCheck = 0;

    public FoodProgramFighter(NPCBase worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public boolean takeFood(IItemHandler target, boolean simulate) {
        for (int j = 0; j < target.getSlots(); j++) {
            ItemStack stackInSlot = target.getStackInSlot(j);
            if (stackInSlot.has(DataComponents.FOOD)) {
                for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
                    if (worker.combinedInventory.insertItem(i, stackInSlot.copyWithCount(1), true).isEmpty()) {
                        if (!simulate) {
                            worker.combinedInventory.insertItem(i, target.extractItem(j, 1, false), false);
                            worker.swing(Utils.moveItemStackToAnyHand(worker.combinedInventory.getStackInSlot(i), worker));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasAnyFood(){
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stackInSlot = worker.combinedInventory.getStackInSlot(i);
            if (stackInSlot.has(DataComponents.FOOD)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveFoodToMainHand() {
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stackInSlot = worker.combinedInventory.getStackInSlot(i);
            if (stackInSlot.has(DataComponents.FOOD)) {
                Utils.moveItemStackToMainHand(stackInSlot, worker);
                return true;
            }
        }
        return false;
    }

    public int countFoodItems(){
        int foodItems = 0;
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stack = worker.combinedInventory.getStackInSlot(i);
            if(stack.has(DataComponents.FOOD)){
                foodItems+=stack.getCount();
            }
        }
        return foodItems;
    }

    @Override
    public boolean canUse() {

        if(worker.level().getGameTime() < lastCheck + 20*5){
            return false;
        }
        lastCheck = worker.level().getGameTime();

        int foodItems = countFoodItems();

        if (worker.hunger >= worker.maxHunger && foodItems >= minStock)
            return false;

        if(worker.hunger < worker.maxHunger) {
            if (hasAnyFood()) return true;
        }

        ItemStack foodOrderStack = worker.foodOrderStackHandler.getStackInSlot(0);
        if(foodOrderStack.getItem() instanceof ItemFoodOrder){
            CompoundTag itemTag = Utils.getStackTagOrEmpty(foodOrderStack);
            if(itemTag.contains("x") && itemTag.contains("y") && itemTag.contains("z") && itemTag.contains("face")){
                BlockPos targetPos = new BlockPos(itemTag.getInt("x"), itemTag.getInt("y"), itemTag.getInt("z"));
                Direction face = Direction.values()[itemTag.getInt("face")];
                IItemHandler inventory = worker.level().getCapability(Capabilities.ItemHandler.BLOCK,targetPos,face);
                if(inventory != null){
                    target = targetPos;
                    inventoryTarget = inventory;
                    if(foodItems == 0 || worker.blockPosition().getCenter().distanceTo(target.getCenter()) < 10) {
                        return true;
                    }
                }
            }
        }else {
            if (worker.townHall != null) {
                if (worker.slowMobNavigation.isPositionCachedAsInvalid(worker.townHall)) {
                    return false;
                }
                BlockEntity e = worker.level().getBlockEntity(worker.townHall);
                if (e instanceof EntityTownHall t) {
                    IItemHandler itemHandler = t.inventory;
                    if (takeFood(itemHandler, true)) {
                        target = worker.townHall;
                        inventoryTarget = t.inventory;
                        if(foodItems == 0 || worker.blockPosition().getCenter().distanceTo(target.getCenter()) < 10) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse;
    }
    @Override
    public void start(){
        canUse = true;
        workDelay = 0;
    }

    public void exit(){
        target = null;
        canUse = false;
        inventoryTarget = null;
    }


    @Override
    public void tick() {
        int numFoodItems = countFoodItems();

        if(worker.hunger >= worker.maxHunger && numFoodItems >= minStock) {
            exit();
            return;
        }

        if(worker.hunger<worker.maxHunger) {
            ItemStack mainHandStack = worker.getMainHandItem();
            if (mainHandStack.has(DataComponents.FOOD)) {
                if (worker.isUsingItem() && worker.getUsedItemHand() == InteractionHand.OFF_HAND) {
                    worker.stopUsingItem();
                }
                if (!worker.isUsingItem()) {
                    workDelay++;
                    if (workDelay >= 20) {
                        worker.startUsingItem(InteractionHand.MAIN_HAND);
                        workDelay = 0;
                    }
                } else {
                    worker.swing(InteractionHand.MAIN_HAND);
                }
                return;
            } else {
                if (moveFoodToMainHand()) {
                    workDelay = 0;
                    return;
                }
            }
        }

        if(target == null || inventoryTarget == null){
            exit();
            return;
        }

        int moveExit = worker.slowMobNavigation.moveToPosition(target, 2, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick);
        if (moveExit == EXIT_FAIL) {
            exit();
            return;
        }
        if (moveExit == SUCCESS_STILL_RUNNING) {
            workDelay = 0;
            return;
        }

        worker.lookAt(EntityAnchorArgument.Anchor.EYES, target.getCenter());
        worker.lookAt(EntityAnchorArgument.Anchor.FEET, target.getCenter());

        if (workDelay >= 20) {
            workDelay = 0;
            if(!takeFood(inventoryTarget,false)){
                exit();
                return;
            }
        }
        workDelay++;
    }
}
