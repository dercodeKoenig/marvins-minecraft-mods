package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.*;

import static NPCs.Utils.EXIT_FAIL;
import static NPCs.Utils.SUCCESS_STILL_RUNNING;

public class PickupItemsOnGroundProgram extends Goal {
    NPCBase npc;
    long lastScanTime = 0;
    boolean canUse = false;
    int workDelay = 0;
    int radius;

    public PickupItemsOnGroundProgram(NPCBase npc, int radius) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.radius = radius;
    }

    public List<ItemEntity> itemsOnGround() {
        return npc.level().getEntitiesOfClass(ItemEntity.class, new AABB(npc.getOnPos()).inflate(radius));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    @Override
    public boolean canUse() {
        long gametime = npc.level().getGameTime();
        if (gametime > lastScanTime + 10) {

            if (Utils. countEmptySlots(npc) < 1) return false;

            lastScanTime = gametime;

            for (ItemEntity i : itemsOnGround()) {
                if (!npc.slowMobNavigation.isPositionCachedAsInvalid(i.getOnPos())) {
                    return true;
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
    public void start() {
        canUse = true;
    }

    public TreeSet<ItemEntity> sortByDistanceTo(Collection<ItemEntity> list) {
        TreeSet<ItemEntity> sorted = new TreeSet<>(new Comparator<ItemEntity>() {
            @Override
            public int compare(ItemEntity o1, ItemEntity o2) {
                return (int) (Math.signum(o1.getPosition(0).distanceTo(npc.getPosition(0)) - o2.getPosition(0).distanceTo(npc.getPosition(0))));
            }
        });
        sorted.addAll(list);
        return sorted;
    }

    @Override
    public void tick() {

        if (Utils. countEmptySlots(npc)  < 1) {
            canUse = false;
            return;
        }

        TreeSet<ItemEntity> itemsOnGround = sortByDistanceTo(itemsOnGround());
        for (ItemEntity i : itemsOnGround) {
            if (!npc.slowMobNavigation.isPositionCachedAsInvalid(i.getOnPos())) {

                int pathFindExit = npc.slowMobNavigation.moveToPosition(
                        i.getOnPos(),
                        2,
                        npc.slowNavigationMaxDistance,
                        npc.slowNavigationMaxNodes,
                        npc.slowNavigationStepPerTick
                );


                if (pathFindExit == EXIT_FAIL) {
                    return;
                } else if (pathFindExit == SUCCESS_STILL_RUNNING) {
                    workDelay = 0;
                    return;
                }
                npc.lookAt(EntityAnchorArgument.Anchor.EYES, i.getPosition(0));
                npc.lookAt(EntityAnchorArgument.Anchor.FEET, i.getPosition(0));

                if (workDelay > 20) {
                    workDelay = 0;
                    ItemStack onGround = i.getItem();
                    ItemStack toInsertCopy = onGround.copy();
                    for (int n = 0; n < npc.combinedInventory.getSlots(); n++) {
                        toInsertCopy = npc.combinedInventory.insertItem(n, toInsertCopy, false);
                    }
                    onGround.setCount(0);
                }
                workDelay++;
                return;
            }
        }
        canUse = false;
    }
}
