package NPCs.Npc.programs;

import NPCs.Npc.NPCBase;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

import static NPCs.Utils.EXIT_SUCCESS;
import static net.minecraft.world.level.block.BedBlock.OCCUPIED;

public class SleepProgram extends Goal {
    NPCBase worker;
    long lastCheck;

    public SleepProgram(NPCBase worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {

        if(!worker.level().isNight()) return false;


        long gameTime = worker.level().getGameTime();
        if (lastCheck + 20*10 > gameTime)
            return false;
        lastCheck = gameTime;


        if(worker.homePosition == null){
            if(worker.townHall == null)
                return false;
            if(worker.position().distanceTo(worker.townHall.getCenter()) < 16)
                return false;
        }else {
            if (worker.slowMobNavigation.isPositionCachedAsInvalid(worker.homePosition))
                return false;
        }

        return  true;
    }
    @Override
    public void stop(){
        worker.stopSleeping();
    }

    @Override
    public void tick() {
        if (worker.isSleeping()) return;

        if (worker.homePosition == null) {
            if(worker.townHall != null) {
                worker.slowMobNavigation.moveToPosition(worker.townHall,6,worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick, 0.8f);
            }

        } else {
            if (worker.slowMobNavigation.moveToPosition(worker.homePosition, 2, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick) == EXIT_SUCCESS) {
                BlockState b = worker.level().getBlockState(worker.homePosition);
                if (b.getBlock() instanceof BedBlock bed) {
                    if (!b.getValue(OCCUPIED)) {
                        worker.getNavigation().stop();
                        worker.startSleeping(worker.homePosition);
                    }
                }
            }
        }
    }
}

