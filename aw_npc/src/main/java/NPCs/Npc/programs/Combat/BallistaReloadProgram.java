package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.programs.TakeToolProgram;
import Vehicles.Ballista;
import Vehicles.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static NPCs.Utils.*;

public class BallistaReloadProgram extends Goal {
    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    public CombatNPC npc;
    public long lastCheck;
    public Ballista ballista;
int waitTimer = 0;
TakeToolProgram takeBoltProgram;

    public BallistaReloadProgram(CombatNPC npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        takeBoltProgram = new TakeToolProgram(npc);
    }

    public void lockTargetPosition() {
        long gameTime = npc.level().getGameTime();
        positionsInUseWithLastUseTime.put(ballista.blockPosition(), gameTime);
    }

    public boolean isPositionLocked(BlockPos p) {
        // if I lock the position, it is not locked for ME, only for OTHER WORKERS
        if (ballista != null && Objects.equals(p, ballista.blockPosition())) return false;

        long gameTime = npc.level().getGameTime();
        return (positionsInUseWithLastUseTime.containsKey(p) &&
                positionsInUseWithLastUseTime.get(p) + 5 > gameTime);
    }

    public boolean isPositionWorkable(BlockPos p) {
        // if the position was recently locked, another worker works there so i can not work here
        if (isPositionLocked(p))
            return false;

        // if the position is cached as not reachable, i can not work here
        if (npc.slowMobNavigation.isPositionCachedAsInvalid(p)) {
            return false;
        }
        return true;
    }

    public boolean canUse() {
        if (npc.level().getGameTime() < lastCheck + 20 * 1 && npc.getTarget() == null) {
            return false;
        }
        lastCheck = npc.level().getGameTime();

        if (npc.hunger > npc.maxHunger * 0.05) {
        } else return false;

        List<Ballista> nearbyBallistas = npc.level().getEntitiesOfClass(Ballista.class, new AABB(npc.blockPosition()).inflate(64));
        for (Ballista i : nearbyBallistas) {
            if (isPositionWorkable(i.blockPosition()) && i.getDrawProgress() < 1 && i.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) == 17 && !i.getEntityData().get(Ballista.IS_BROKEN)) {
                ballista = i;
                lockTargetPosition();
                return true;
            }
            if (isPositionWorkable(i.blockPosition()) && takeBoltProgram.hasTool(Registry.ITEM_BALLISTA_BOLT.get()) && i.getDrawProgress() == 1 && i.bolt == null && i.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) == 17 && !i.getEntityData().get(Ballista.IS_BROKEN)) {
                ballista = i;
                lockTargetPosition();
                return true;
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        return ballista != null && npc.hunger > npc.maxHunger * 0.05;
    }

    public void start() {
        super.start();
        waitTimer = 0;
    }

    public void stop() {
        super.stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {

        if (ballista == null  || ballista.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) != 17 || ballista.getEntityData().get(Ballista.IS_BROKEN)) {
            ballista = null;
            return;
        }
        lockTargetPosition();


        int exit = npc.slowMobNavigation.moveToPosition(ballista.blockPosition(), 3, npc.slowNavigationMaxDistance, npc.slowNavigationMaxNodes, npc.slowNavigationStepPerTick, 1);
        if (exit == SUCCESS_STILL_RUNNING)
            return;
        if (exit == EXIT_FAIL) {
            ballista = null;
            return;
        }

        npc.getLookControl().setLookAt(ballista, 30, 30);

        if (ballista.getDrawProgress() < 1) {
            waitTimer++;
            if (ballista.reloadTicksRemaining == 0 && waitTimer > 20) {
                ballista.resetReloadTimer();
                npc.swing(InteractionHand.MAIN_HAND);
                waitTimer = 0;
            }
            return;
        }
        if (ballista.getDrawProgress() == 1 && ballista.bolt == null && takeBoltProgram.hasTool(Registry.ITEM_BALLISTA_BOLT.get())) {
            takeBoltProgram.takeToolToMainHand(Registry.ITEM_BALLISTA_BOLT.get());
            waitTimer++;
            if (waitTimer > 20) {
                ballista.load();
                npc.combinedInventory.extractItem(0, 1, false);
                npc.swing(InteractionHand.MAIN_HAND);
                waitTimer = 0;
            }
            return;
        }

        ballista = null;
    }
}

