package NPCs.Npc.programs;

import NPCs.Blocks.TownHall.EntityTownHall;
import NPCs.Npc.CombatNPC;
import NPCs.Npc.NPCBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumSet;

import static NPCs.Utils.SUCCESS_STILL_RUNNING;

public class DropLootProgram extends Goal {
    long lastCheck = 0;
    NPCBase worker;
    UnloadInventoryProgram unloadInventoryProgram;
    ItemStack stackToUnload = ItemStack.EMPTY;
    BlockPos targetPos;
    IItemHandler targetInventory;

    public DropLootProgram(NPCBase worker) {
        this.worker = worker;
        unloadInventoryProgram = new UnloadInventoryProgram(worker);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }


    @Override
    public boolean canUse() {
        if (worker.level().getGameTime() < lastCheck + 20 * 1) {
            return canContinueToUse();
        }
        lastCheck = worker.level().getGameTime();

        targetPos = null;
        targetInventory = null;
        stackToUnload = ItemStack.EMPTY;

        if (worker.townHall != null) {
            BlockEntity be = worker.level().getBlockEntity(worker.townHall);
            if (be instanceof EntityTownHall townhall) {
                ItemStack canUnload = UnloadInventoryProgram.getAnyItemToUnload(townhall.inventory,worker);
                if (!canUnload.isEmpty()) {
                    stackToUnload = canUnload.copy();
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
