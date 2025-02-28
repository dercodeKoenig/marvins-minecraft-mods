package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;

public class TakeToolProgram {

    NPCBase npc;
    int cachedToolIndex = 0;
    int requiredDistance = 2;

    int workDelay = 0;

    public TakeToolProgram(NPCBase npc) {
        this.npc = npc;
    }


    public boolean takeToolForDropsToMainHand(BlockState state) {
        ItemStack stackInHand = npc.getMainHandItem();
        if (stackInHand.isCorrectToolForDrops(state)) return true;
        if (!hasToolForDrops(state)) return false;

        ItemStack stack = npc.combinedInventory.getStackInSlot(cachedToolIndex);
        if (stack.isCorrectToolForDrops(state)) {
            Utils.moveItemStackToMainHand(stack, npc);
            return true;
        }
        return false;
    }

    public boolean hasToolForDrops(BlockState state) {
        if(!state.requiresCorrectToolForDrops()) return true;
        if (npc.combinedInventory.getStackInSlot(cachedToolIndex).isCorrectToolForDrops(state)) {
            return true;
        }

        for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
            if (npc.combinedInventory.getStackInSlot(i).isCorrectToolForDrops(state)) {
                cachedToolIndex = i;
                return true;
            }
        }
        return false;
    }


    public boolean pickupCorrectToolForDrops(BlockState stateToMine, IItemHandler target, boolean simulate) {
        for (int j = 0; j < target.getSlots(); j++) {
            ItemStack stackInSlot = target.getStackInSlot(j);
            if (stackInSlot.isCorrectToolForDrops(stateToMine)) {
                for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
                    if (npc.combinedInventory.insertItem(i, stackInSlot.copyWithCount(1), true).isEmpty()) {
                        if (!simulate) {
                            npc.combinedInventory.insertItem(i, target.extractItem(j, 1, false), false);
                            npc.swing(Utils.moveItemStackToAnyHand(stackInSlot, npc));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public boolean takeToolToMainHand(Class<?> itemClass) {
        ItemStack stackInHand = npc.getMainHandItem();
        if (itemClass.isInstance(stackInHand.getItem())) return true;
        if (!hasTool(itemClass)) return false;

        ItemStack stack = npc.combinedInventory.getStackInSlot(cachedToolIndex);
        if (itemClass.isInstance(stack.getItem())) {
            Utils.moveItemStackToMainHand(stack, npc);
            return true;
        }
        return false;
    }

    public boolean hasTool(Class<?> itemCLass) {
        if (itemCLass.isInstance(npc.combinedInventory.getStackInSlot(cachedToolIndex).getItem()))
            return true;

        for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
            if (itemCLass.isInstance(npc.combinedInventory.getStackInSlot(i).getItem())) {
                cachedToolIndex = i;
                return true;
            }
        }
        return false;
    }

    public int getToolIndex(Class<?> itemCLass) {
        if (itemCLass.isInstance(npc.combinedInventory.getStackInSlot(cachedToolIndex).getItem()))
            return cachedToolIndex;

        for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
            if (itemCLass.isInstance(npc.combinedInventory.getStackInSlot(i).getItem())) {
                cachedToolIndex = i;
                return cachedToolIndex;
            }
        }
        return -1;
    }

    public boolean pickupToolFromTarget(Class<?> itemClass, IItemHandler target, boolean simulate) {
        for (int j = 0; j < target.getSlots(); j++) {
            ItemStack stackInSlot = target.getStackInSlot(j);
            if (itemClass.isInstance(stackInSlot.getItem())) {
                for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
                    if (npc.combinedInventory.insertItem(i, stackInSlot.copyWithCount(1), true).isEmpty()) {
                        if (!simulate) {
                            npc.combinedInventory.insertItem(i, target.extractItem(j, 1, false), false);
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
        return run(toolClass,targetPos,targetInventory, false);
    }
    public int run(Class<?> toolClass, BlockPos targetPos, IItemHandler targetInventory, boolean ignoreExisting) {
        if (!ignoreExisting && hasTool(toolClass)) return EXIT_SUCCESS;
        else if (!pickupToolFromTarget(toolClass, targetInventory, true)) {
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

    public int run(BlockState stateToMine, BlockPos targetPos, IItemHandler targetInventory) {
        if (hasToolForDrops(stateToMine)) return EXIT_SUCCESS;
        if (!pickupCorrectToolForDrops(stateToMine, targetInventory, true)) {
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
            if (pickupCorrectToolForDrops(stateToMine, targetInventory, false))
                return EXIT_SUCCESS;
            else
                return EXIT_FAIL; // should never trigger because first line in run() checks if it can take tool
        }
        workDelay++;
        return SUCCESS_STILL_RUNNING;
    }
}
