package NPCs.Npc.programs.Combat;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;

public class TakeMeleeWeaponProgram {

    NPCBase npc;
    int requiredDistance = 2;
    int bestWeaponIndex = 0;
    int workDelay = 0;

    public TakeMeleeWeaponProgram(NPCBase npc) {
        this.npc = npc;
        findBestWeaponIndex();
    }

    public void findBestWeaponIndex(){
        for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
            ItemStack bestWeaponStack = npc.combinedInventory.getStackInSlot(bestWeaponIndex);
            ItemStack current = npc.combinedInventory.getStackInSlot(i);
            if(isWeaponBetter(current, bestWeaponStack)){
                bestWeaponIndex = i;
            }
        }
    }

    public void takeBestWeaponToMainHand() {
        // this assumes that findBestWeaponIndex() is called every time the inventory changes
        if (bestWeaponIndex == 0) return; // is already in main hand

        ItemStack stack = npc.combinedInventory.getStackInSlot(bestWeaponIndex);
        Utils.moveItemStackToMainHand(stack, npc);
    }

    public boolean swapWeaponFromTarget(IItemHandler target, boolean simulate) {

        float currentDur = getRemainingDurabilityRelative(npc.combinedInventory.getStackInSlot(bestWeaponIndex));

        // Variables to store the best target slot index for each piece (-1 means no better item found)
        int otherBestIndex = -1;

        // Iterate over target inventory
        for (int j = 0; j < target.getSlots(); j++) {
            ItemStack stack = target.getStackInSlot(j);
            float d = getRemainingDurabilityRelative(stack);
            // Check each armor type
            if (((currentDur < 0.2 && d > 0.2) || currentDur < 0 || (d > 0.2 && isWeaponBetter(stack, npc.combinedInventory.getStackInSlot(bestWeaponIndex))))) {
                currentDur = d;
                otherBestIndex = j;
            }
        }

        // Swap each armor piece if a better one was found
        if (otherBestIndex != -1) {
            if (!simulate) {
                swapWeapon(bestWeaponIndex, otherBestIndex, target);
            }
            return true;
        }
        return false;
    }

    public static boolean isWeaponBetter(ItemStack s1, ItemStack s2) {
        ItemAttributeModifiers modifiers1 = s1.getAttributeModifiers();
        ItemAttributeModifiers modifiers2 = s2.getAttributeModifiers();

        double armor1V = 0;
        for (ItemAttributeModifiers.Entry i : modifiers1.modifiers()){
            if(i.attribute() == Attributes.ATTACK_DAMAGE){
                armor1V = i.modifier().amount();
            }
        }
        double armor2V = 0;
        for (ItemAttributeModifiers.Entry i : modifiers2.modifiers()){
            if(i.attribute() == Attributes.ATTACK_DAMAGE){
                armor2V = i.modifier().amount();
            }
        }

        return armor1V > armor2V;
    }

    // Helper to compute remaining durability
    private float getRemainingDurabilityRelative(ItemStack stack) {
        return stack.isEmpty() ? -1f : (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
    }

    private void swapWeapon(int slotIndex, int targetSlotIndex, IItemHandler target) {
        ItemStack currentStack = npc.combinedInventory.getStackInSlot(slotIndex);
        ItemStack targetStack = target.getStackInSlot(targetSlotIndex);

        ItemStack extractedCurrent = npc.combinedInventory.extractItem(slotIndex, currentStack.getCount(), false);
        ItemStack extractedTarget = target.extractItem(targetSlotIndex, targetStack.getCount(), false);

        ItemStack remainderNpc = npc.combinedInventory.insertItem(slotIndex, extractedTarget, false);
        ItemStack remainderTarget = target.insertItem(targetSlotIndex, extractedCurrent, false);

        if (!remainderNpc.isEmpty() || !remainderTarget.isEmpty()) {
            System.err.println("Error: itemstack not empty for " + slotIndex + " swap: " + remainderNpc + " : " + remainderTarget);
        }
        npc.swing(InteractionHand.MAIN_HAND);
    }


    public int run(BlockPos targetPos, IItemHandler targetInventory) {
        if (!swapWeaponFromTarget(targetInventory, true)) {
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
            if (swapWeaponFromTarget(targetInventory, false))
                return EXIT_SUCCESS;
            else
                return EXIT_FAIL; // should never trigger because first line in run() checks if it can take tool
        }
        workDelay++;
        return SUCCESS_STILL_RUNNING;
    }
}
