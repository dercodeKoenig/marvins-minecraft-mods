package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import NPCs.Utils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static NPCs.Utils.*;

public class PickupItemsOnGroundProgram extends Goal {
    NPCBase npc;
    long lastScanTime = 0;
    boolean canUse = false;
    int radius;
    UUID targetItem = null;

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
        return false;
    }


    @Override
    public boolean canUse() {
        long gametime = npc.level().getGameTime();
        if (lastScanTime + 20 > gametime) {
            return false;
        }
        lastScanTime = gametime;

        if (Utils.countEmptySlots(npc) < 1) return false;
        for (ItemEntity i : itemsOnGround()) {
            if (Math.abs(i.getDeltaMovement().length()) < 0.01) {
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
    @Override
    public void stop(){
        targetItem = null;
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

        long gametime = npc.level().getGameTime();
        if (lastScanTime + 20 > gametime) {
            return;
        }
        lastScanTime = gametime;

        if (Utils.countEmptySlots(npc) < 1) {
            canUse = false;
            return;
        }

        if(targetItem != null){
            Entity e = ((ServerLevel)npc.level()).getEntity(targetItem);
            if(e instanceof ItemEntity item){
                int pathFindExit = npc.slowMobNavigation.moveToPosition(
                        item.getOnPos(),
                        1,
                        radius * 6,
                        128,
                        npc.slowNavigationStepPerTick
                );
                if (pathFindExit == SUCCESS_STILL_RUNNING) {
                    return;
                }
            }
            targetItem = null;
        }
        TreeSet<ItemEntity> itemsOnGround = sortByDistanceTo(itemsOnGround());
        for (ItemEntity i : itemsOnGround) {
            if (Math.abs(new Vec3(i.getDeltaMovement().x, 0, i.getDeltaMovement().z).lengthSqr()) < 0.01) {
                if (!npc.slowMobNavigation.isPositionCachedAsInvalid(i.getOnPos())) {
                    int pathFindExit = npc.slowMobNavigation.moveToPosition(
                            i.getOnPos(),
                            1,
                            radius * 6,
                            128,
                            npc.slowNavigationStepPerTick
                    );
                    if (pathFindExit == SUCCESS_STILL_RUNNING) {
                        targetItem = i.getUUID();
                        return;
                    }
                }
            }
        }
        canUse = false;
    }
}
