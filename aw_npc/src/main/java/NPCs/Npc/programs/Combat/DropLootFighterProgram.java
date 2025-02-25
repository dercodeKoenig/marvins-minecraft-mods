package NPCs.Npc.programs.Combat;

import NPCs.Blocks.TownHall.EntityTownHall;
import NPCs.Npc.CombatNPC;
import NPCs.Npc.programs.UnloadInventoryProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumSet;

import static NPCs.Utils.SUCCESS_STILL_RUNNING;

public class DropLootFighterProgram extends Goal {
    long lastCheck = 0;
    CombatNPC worker;
    UnloadInventoryProgram unloadInventoryProgram;
    ItemStack stackToUnload = ItemStack.EMPTY;
    BlockPos targetPos;
    IItemHandler targetInventory;

    public DropLootFighterProgram(CombatNPC worker) {
        this.worker = worker;
        unloadInventoryProgram = new UnloadInventoryProgram(worker);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }


    public int countFoodItems() {
        int foodItems = 0;
        for (int i = 0; i < worker.combinedInventory.getSlots(); i++) {
            ItemStack stack = worker.combinedInventory.getStackInSlot(i);
            if (stack.has(DataComponents.FOOD)) {
                foodItems += stack.getCount();
            }
        }
        return foodItems;
    }


    public ItemStack getAnyItemToUnload(IItemHandler itemHandlerTarget) {
        int numFoodItems = countFoodItems();

        for (int j = 0; j < worker.combinedInventory.getSlots(); j++) {
            if (j == worker.takeWeaponProgram.bestWeaponIndex) {
                continue;
            }
            ItemStack canExtract = worker.combinedInventory.extractItem(j, 1, true);
            if (canExtract.has(DataComponents.FOOD) && numFoodItems <= 3) {
                continue;
            }
            if (!canExtract.isEmpty()) {
                // try to insert in inventory
                for (int i = 0; i < itemHandlerTarget.getSlots(); i++) {
                    ItemStack notInserted = itemHandlerTarget.insertItem(i, canExtract, true);
                    if (notInserted.isEmpty()) {
                        return canExtract.copy();
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse() {
        if (worker.level().getGameTime() < lastCheck + 20 * 10) {
            return false;
        }
        lastCheck = worker.level().getGameTime();

        targetPos = null;
        targetInventory = null;
        stackToUnload = ItemStack.EMPTY;

        if (worker.townHall != null) {
            BlockEntity be = worker.level().getBlockEntity(worker.townHall);
            if (be instanceof EntityTownHall townhall) {
                ItemStack canUnload = getAnyItemToUnload(townhall.inventory);
                if (!canUnload.isEmpty()) {
                    stackToUnload = canUnload;
                    targetPos = worker.townHall;
                    targetInventory = townhall.inventory;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return stackToUnload != ItemStack.EMPTY && targetPos != null && targetInventory != null;
    }

    @Override
    public void tick() {
        if (canContinueToUse()) {
            if (unloadInventoryProgram.run(targetInventory, targetPos, stackToUnload) != SUCCESS_STILL_RUNNING) {
                lastCheck = 0;
                canUse();
            }
        }
    }
}
