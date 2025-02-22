package NPCs.Npc.programs.Combat;

import ARLib.utils.BlockIdentifier;
import NPCs.Blocks.Armory.EntityArmory;
import NPCs.Npc.CombatNPC;
import NPCs.Npc.programs.TakeToolProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

import static NPCs.Npc.CombatNPC.DATA_WORKTYPE;
import static NPCs.Utils.*;

public class ArmoryProgram extends Goal {

    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    CombatNPC worker;
    long lastCheck = 0;

    BlockPos targetPos;
    IItemHandler targetInventory;

    TakeArmorProgram takeArmorProgram;

    public void lockTargetPosition() {
        long gameTime = worker.level().getGameTime();
        positionsInUseWithLastUseTime.put(targetPos, gameTime);
    }

    public boolean isPositionLocked(BlockPos p) {
        // if I lock the position, it is not locked for ME, only for OTHER WORKERS
        if (Objects.equals(p, targetPos)) return false;

        long gameTime = worker.level().getGameTime();
        return (positionsInUseWithLastUseTime.containsKey(p) &&
                positionsInUseWithLastUseTime.get(p) + 5 > gameTime);
    }

    public boolean isPositionWorkable(BlockPos p) {
        // if the position was recently locked, another worker works there so i can not work here
        if (isPositionLocked(p))
            return false;

        // if the position is cached as not reachable, i can not work here
        if (worker.slowMobNavigation.isPositionCachedAsInvalid(p)) {
            return false;
        }
        return true;
    }


    public ArmoryProgram(CombatNPC worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        takeArmorProgram = new TakeArmorProgram(worker);
    }

    @Override
    public boolean canUse() {

        if (worker.level().getGameTime() < lastCheck + 20 * 1) {
            return false;
        }
        lastCheck = worker.level().getGameTime();

        if (worker.townHall != null) {
            Set<BlockPos> armoryPositions = EntityArmory.knownBlocksForTownhallPosition.get(new BlockIdentifier(worker.level(), worker.townHall));
            if (armoryPositions != null) {
                for (BlockPos p : armoryPositions) {
                    if (isPositionWorkable(p)) {
                        BlockEntity e = worker.level().getBlockEntity(p);
                        if (e instanceof EntityArmory armory) {

                            if (worker.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.fighter.ordinal()) {
                                if (worker.takeWeaponProgram.swapWeaponFromTarget(armory.inventory, true)) {
                                    targetPos = p;
                                    targetInventory = armory.inventory;
                                    lockTargetPosition();
                                    return true;
                                }
                            }


                            if (takeArmorProgram.swapArmorFromTarget(armory.inventory, true)) {
                                targetPos = p;
                                targetInventory = armory.inventory;
                                lockTargetPosition();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPos != null && targetInventory != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    @Override
    public void tick() {

        if (targetPos == null || targetInventory == null) return;

        lockTargetPosition();

        int exit = worker.takeWeaponProgram.run(targetPos, targetInventory);
        if (exit == SUCCESS_STILL_RUNNING) {
            return;
        }

        exit = takeArmorProgram.run(targetPos, targetInventory);
        if (exit == SUCCESS_STILL_RUNNING) {
            return;
        }

        targetPos = null;
        targetInventory = null;

    }
}
