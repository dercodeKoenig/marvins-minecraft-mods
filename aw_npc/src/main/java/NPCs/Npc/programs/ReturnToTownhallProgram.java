package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

import static NPCs.Utils.EXIT_SUCCESS;
import static net.minecraft.world.level.block.BedBlock.OCCUPIED;

public class ReturnToTownhallProgram extends Goal {
    NPCBase worker;
    long lastCheck;

    public ReturnToTownhallProgram(NPCBase worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {

        long gameTime = worker.level().getGameTime();
        if (lastCheck + 20 * 10 > gameTime)
            return false;
        lastCheck = gameTime;

        if (worker.townHall == null)
            return false;
        if (worker.position().distanceTo(worker.townHall.getCenter()) < 128)
            return false;
        return true;
    }

    @Override
    public void stop() {

    }

    @Override
    public void tick() {
        if (worker.townHall != null) {
            worker.slowMobNavigation.moveToPosition(worker.townHall, 100, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick, 0.8f);
        }
    }
}

