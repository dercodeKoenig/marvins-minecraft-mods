package NPCs.Npc.programs.Combat;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import com.google.common.collect.Multimap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;

public class TakeArmorProgram {

    NPCBase npc;
    int requiredDistance = 2;

    int workDelay = 0;

    public TakeArmorProgram(NPCBase npc) {
        this.npc = npc;
    }

    public boolean swapArmorFromTarget(IItemHandler target, boolean simulate) {

        // Get current durability for each armor piece
        float headDur = getRemainingDurabilityRelative(npc.armorInventory.getStackInSlot(EquipmentSlot.HEAD.getIndex()));
        float chestDur = getRemainingDurabilityRelative(npc.armorInventory.getStackInSlot(EquipmentSlot.CHEST.getIndex()));
        float legsDur = getRemainingDurabilityRelative(npc.armorInventory.getStackInSlot(EquipmentSlot.LEGS.getIndex()));
        float feetDur = getRemainingDurabilityRelative(npc.armorInventory.getStackInSlot(EquipmentSlot.FEET.getIndex()));

        // Variables to store the best target slot index for each piece (-1 means no better item found)
        int bestHeadIndex = -1;
        int bestChestIndex = -1;
        int bestLegsIndex = -1;
        int bestFeetIndex = -1;

        // Iterate over target inventory
        for (int j = 0; j < target.getSlots(); j++) {
            ItemStack stack = target.getStackInSlot(j);
            if(stack.getItem() instanceof Equipable a) {

                float d = getRemainingDurabilityRelative(stack);
                EquipmentSlot slot = a.getEquipmentSlot();

                // Check each armor type
                if (slot == EquipmentSlot.HEAD && ((headDur < 0.2 && d > 0.2) || headDur < 0 || (d > 0.2 && isArmorBetter(stack,npc.armorInventory.getStackInSlot(EquipmentSlot.HEAD.getIndex()))))) {
                    headDur = d;
                    bestHeadIndex = j;
                }
                if (slot == EquipmentSlot.CHEST && ((chestDur < 0.2 && d > 0.2) || chestDur < 0 || (d > 0.2 && isArmorBetter(stack,npc.armorInventory.getStackInSlot(EquipmentSlot.CHEST.getIndex()))))) {
                    chestDur = d;
                    bestChestIndex = j;
                }
                if (slot == EquipmentSlot.LEGS && ((legsDur < 0.2 && d > 0.2) || legsDur < 0 || (d > 0.2 && isArmorBetter(stack,npc.armorInventory.getStackInSlot(EquipmentSlot.LEGS.getIndex()))))) {
                    legsDur = d;
                    bestLegsIndex = j;
                }
                if (slot == EquipmentSlot.FEET && ((feetDur < 0.2 && d > 0.2) || feetDur < 0 || (d > 0.2 && isArmorBetter(stack,npc.armorInventory.getStackInSlot(EquipmentSlot.FEET.getIndex()))))) {
                    feetDur = d;
                    bestFeetIndex = j;
                }
            }
        }

        // Swap each armor piece if a better one was found
        if (bestHeadIndex != -1) {
            if (!simulate) {
                swapArmorPiece(EquipmentSlot.HEAD, bestHeadIndex, target);
            }
            return true;
        }
        if (bestChestIndex != -1) {
            if (!simulate) {
                swapArmorPiece(EquipmentSlot.CHEST, bestChestIndex, target);
            }
            return true;
        }
        if (bestLegsIndex != -1) {
            if (!simulate) {
                swapArmorPiece(EquipmentSlot.LEGS, bestLegsIndex, target);
            }
            return true;
        }
        if (bestFeetIndex != -1) {
            if (!simulate) {
                swapArmorPiece(EquipmentSlot.FEET, bestFeetIndex, target);
            }
            return true;
        }

        return false;
    }

    public static boolean isArmorBetter(ItemStack armor1, ItemStack armor2) {
        // Get the armor values
        ItemAttributeModifiers modifiers1 = armor1.getAttributeModifiers();
        ItemAttributeModifiers modifiers2 = armor2.getAttributeModifiers();

        double armor1V = 0;
        for (ItemAttributeModifiers.Entry i : modifiers1.modifiers()){
            if(i.attribute() == Attributes.ARMOR){
                armor1V = i.modifier().amount();
            }
        }
        double armor2V = 0;
        for (ItemAttributeModifiers.Entry i : modifiers2.modifiers()){
            if(i.attribute() == Attributes.ARMOR){
                armor2V = i.modifier().amount();
            }
        }

        return armor1V > armor2V;
    }

    // Helper method to swap a single armor piece
    private void swapArmorPiece(EquipmentSlot slot, int targetSlotIndex, IItemHandler target) {
        int npcSlotIndex = slot.getIndex();
        ItemStack currentArmor = npc.armorInventory.getStackInSlot(npcSlotIndex);
        ItemStack targetArmor = target.getStackInSlot(targetSlotIndex);

        // Extract both items from their inventories
        ItemStack extractedCurrent = npc.armorInventory.extractItem(npcSlotIndex, currentArmor.getCount(), false);
        ItemStack extractedTarget = target.extractItem(targetSlotIndex, targetArmor.getCount(), false);

        // Swap them
        ItemStack remainderNpc = npc.armorInventory.insertItem(npcSlotIndex, extractedTarget, false);
        ItemStack remainderTarget = target.insertItem(targetSlotIndex, extractedCurrent, false);

        if (!remainderNpc.isEmpty() || !remainderTarget.isEmpty()) {
            System.err.println("Error: itemstack not empty for " + slot + " swap: " + remainderNpc + " : " + remainderTarget);
        }
        npc.swing(InteractionHand.MAIN_HAND);
    }


    public int run(BlockPos targetPos, IItemHandler targetInventory) {
        if (!swapArmorFromTarget(targetInventory, true)) {
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
            if (swapArmorFromTarget(targetInventory, false))
                return EXIT_SUCCESS;
            else
                return EXIT_FAIL; // should never trigger because first line in run() checks if it can take tool
        }
        workDelay++;
        return SUCCESS_STILL_RUNNING;
    }
}
