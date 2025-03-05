package NPCs.Npc.programs.Combat;

import ARLib.utils.BlockIdentifier;
import NPCs.Blocks.Armory.EntityArmory;
import NPCs.Npc.CombatNPC;
import NPCs.Npc.programs.TakeToolProgram;
import NPCs.Utils;
import Vehicles.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ArrowItem;
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
    TakeToolProgram takeArrowProgram;
    TakeToolProgram takeBallistaBoltProgram;

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
        takeArrowProgram = new TakeToolProgram(worker);
        takeBallistaBoltProgram = new TakeToolProgram(worker);
    }

    @Override
    public boolean canUse() {

        if (worker.level().getGameTime() < lastCheck + 20 * 10) {
            return false;
        }
        lastCheck = worker.level().getGameTime();

        // check if he has already armor in his normal inventory
        if (takeArmorProgram.swapArmorFromTarget(worker.combinedInventory, false)) {
            return false;
        }

        if (worker.townHall != null) {
            Set<BlockPos> armoryPositions = EntityArmory.knownBlocksForTownhallPosition.getOrDefault(new BlockIdentifier(worker.level(), worker.townHall), Set.of());
            for (BlockPos p : sortBlockPosByDistanceToVec(armoryPositions,worker.position())) {
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
                        if (worker.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.archer.ordinal()) {
                            if (worker.takeBowWeaponProgram.swapWeaponFromTarget(armory.inventory, true)) {
                                targetPos = p;
                                targetInventory = armory.inventory;
                                lockTargetPosition();
                                return true;
                            }
                            int arrows = Utils.countItems(ArrowItem.class, worker.combinedInventory);
                            double distance = Utils.distanceManhattan(p.getCenter(), worker.getOnPos().getCenter());
                            int minRequiredCount = 20;
                            if (distance < 5) {
                                minRequiredCount = 64;
                            }

                            if (arrows < minRequiredCount) {
                                if (takeArrowProgram.pickupToolFromTarget(ArrowItem.class, armory.inventory, true)) {
                                    lockTargetPosition();
                                    targetPos = p;
                                    targetInventory = armory.inventory;
                                    return true;
                                }
                            }
                        }


                        if (worker.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.siege_engineer.ordinal()) {
                            int arrows = Utils.countItems(Registry.ITEM_BALLISTA_BOLT.get(), worker.combinedInventory);
                            double distance = Utils.distanceManhattan(p.getCenter(), worker.getOnPos().getCenter());
                            int minRequiredCount = 1;
                            if (distance < 5) {
                                minRequiredCount = 4;
                            }

                            if (arrows < minRequiredCount) {
                                if (takeBallistaBoltProgram.pickupToolFromTarget(Registry.ITEM_BALLISTA_BOLT.get(), armory.inventory, true)) {
                                    lockTargetPosition();
                                    targetPos = p;
                                    targetInventory = armory.inventory;
                                    return true;
                                }
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

        if (worker.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.fighter.ordinal()) {
            int exit = worker.takeWeaponProgram.run(targetPos, targetInventory);
            if (exit == SUCCESS_STILL_RUNNING) {
                return;
            }
        }
        if (worker.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.archer.ordinal()) {
            int exit = worker.takeBowWeaponProgram.run(targetPos, targetInventory);
            if (exit == SUCCESS_STILL_RUNNING) {
                return;
            }

            int arrows = Utils.countItems(ArrowItem.class, worker.combinedInventory);
            double distance = Utils.distanceManhattan(targetPos.getCenter(), worker.getOnPos().getCenter());
            int minRequiredCount = 20;
            if (distance < 5) {
                minRequiredCount = 64;
            }
            if (arrows < minRequiredCount) {
                exit = takeArrowProgram.run(ArrowItem.class, targetPos, targetInventory, true);
                if (exit == SUCCESS_STILL_RUNNING || exit == EXIT_SUCCESS) {
                    return;
                }
            }
        }

        int exit = takeArmorProgram.run(targetPos, targetInventory);
        if (exit == SUCCESS_STILL_RUNNING) {
            return;
        }


        if (worker.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.siege_engineer.ordinal()) {
            int arrows = Utils.countItems(Registry.ITEM_BALLISTA_BOLT.get(), worker.combinedInventory);
            double distance = Utils.distanceManhattan(targetPos.getCenter(), worker.getOnPos().getCenter());
            int minRequiredCount = 1;
            if (distance < 5) {
                minRequiredCount = 4;
            }
            if (arrows < minRequiredCount) {
                exit = takeBallistaBoltProgram.run(Registry.ITEM_BALLISTA_BOLT.get(), targetPos, targetInventory, true);
                if (exit == SUCCESS_STILL_RUNNING || exit == EXIT_SUCCESS) {
                    return;
                }
            }
        }

        targetPos = null;
        targetInventory = null;

    }
}
