package NPCs.Npc.programs.Combat;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;

public class TakeArmorProgram {

    NPCBase npc;
    int cachedToolIndex = 0;
    int requiredDistance = 2;

    int workDelay = 0;

    public TakeArmorProgram(NPCBase npc) {
        this.npc = npc;
    }

    public boolean pickupToolFromTarget(Class<?> itemClass, IItemHandler target, boolean simulate) {
        for (int j = 0; j < target.getSlots(); j++) {
            ItemStack stackInSlot = target.getStackInSlot(j);
            if (itemClass.isInstance(stackInSlot.getItem())) {
                for (int i = 0; i < npc.armorInventory.getSlots(); i++) {
                    if (npc.armorInventory.insertItem(i, stackInSlot.copyWithCount(1), true).isEmpty()) {
                        if (!simulate) {
                            npc.armorInventory.insertItem(i, target.extractItem(j, 1, false), false);
                            npc.swing(Utils.moveItemStackToAnyHand(stackInSlot, npc));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int run(Class<?> toolClass, BlockPos targetPos, IItemHandler targetInventory) {
        if (!pickupToolFromTarget(toolClass, targetInventory, true)) {
            return -2;
        }

        int pathFindExit = npc.slowMobNavigation.moveToPosition(
                targetPos,
                requiredDistance,
                npc.slowNavigationMaxDistance,
                npc.slowNavigationMaxNodes,
                npc.slowNavigationStepPerTick
        );

        if (pathFindExit == EXIT_FAIL) {
            return EXIT_FAIL;
        } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
            workDelay = 0;
            return SUCCESS_STILL_RUNNING;
        }

        npc.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos.getCenter());
        npc.lookAt(EntityAnchorArgument.Anchor.FEET, targetPos.getCenter());

        if (workDelay > 20) {
            workDelay = 0;
            if (pickupToolFromTarget(toolClass, targetInventory, false))
                return EXIT_SUCCESS;
            else
                return EXIT_FAIL; // should never trigger because first line in run() checks if it can take tool
        }
        workDelay++;
        return SUCCESS_STILL_RUNNING;
    }
}
