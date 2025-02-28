package NPCs.Npc.programs.Combat;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.items.IItemHandler;

import static NPCs.Utils.*;

public class TakeBowWeaponProgram extends TakeMeleeWeaponProgram {
    public TakeBowWeaponProgram(NPCBase npc) {
        super(npc);
    }


    public boolean isWeaponBetter(ItemStack s1, ItemStack s2) {
        // bow = bow.
        return s1.getItem() instanceof BowItem && !(s2.getItem() instanceof BowItem);
    }

    public void findBestWeaponIndex(){
        bestWeaponIndex = 0;
        for (int i = 0; i < npc.combinedInventory.getSlots(); i++) {
            ItemStack current = npc.combinedInventory.getStackInSlot(i);
            if(current.getItem() instanceof BowItem){
                bestWeaponIndex = i;
                break;
            }
        }
    }
}
