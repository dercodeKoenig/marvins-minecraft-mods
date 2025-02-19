package NPCs.Npc.programs;

import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import NPCs.Blocks.StrategyTable.EntityStrategyTable;
import NPCs.Items.ItemWorkOrder;
import NPCs.Npc.CombatNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static NPCs.Utils.*;

public class FighterFollowWorkOrderByStrategyTable extends Goal {


    CombatNPC worker;
    long lastCheck = 0;
    long lastCheckTick = 0;
    public BlockPos lastUsedStrategyTable = null;

    public FighterFollowWorkOrderByStrategyTable(CombatNPC worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (worker.level().getGameTime() < lastCheck + 20 * 5) {
            return lastUsedStrategyTable != null;
        }
        lastCheck = worker.level().getGameTime();

        if (worker.hunger < worker.maxHunger * 0.05) {
            lastUsedStrategyTable = null;
            return false;
        }
        if (worker.townHall != null) {
            BlockIdentifier b_id = new BlockIdentifier(DimensionUtils.getLevelId(worker.level()), worker.townHall);
            Set<BlockPos> strategyTables = EntityStrategyTable.knownStrategyTablesForTownhallPosition.get(b_id);
            for (BlockPos p : strategyTables) {
                BlockEntity e = worker.level().getBlockEntity(p);
                if (e instanceof EntityStrategyTable table) {
                    EntityStrategyTable.workTargetManager m = table.getManagerForUUID(worker.getUUID());
                    if (m != null) {
                        BlockPos nextTarget = m.getTarget(); // check if the target is cached as invalid
                        if (nextTarget != null && !worker.slowMobNavigation.isPositionCachedAsInvalid(nextTarget)) {
                            lastUsedStrategyTable = p;
                            return true;
                        }
                    }
                } else {
                    System.err.println("error: " + p + " is not a strategy table");
                    strategyTables.remove(p);
                    break;
                }
            }
        }

        lastUsedStrategyTable = null;
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return lastUsedStrategyTable != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    @Override
    public void tick() {
        if (worker.level().getGameTime() < lastCheckTick + 20 * 1) {
            return;
        }
        lastCheckTick = worker.level().getGameTime();

        if (lastUsedStrategyTable != null) {
            BlockEntity e = worker.level().getBlockEntity(lastUsedStrategyTable);
            if (e instanceof EntityStrategyTable table) {
                EntityStrategyTable.workTargetManager m = table.getManagerForUUID(worker.getUUID());
                if (m != null) {
                    BlockPos target = m.getTarget();
                    if(target == null){
                        lastUsedStrategyTable = null;
                        return;
                    }
                    int moveExit = worker.slowMobNavigation.moveToPosition(target, 0, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick);
                    if (moveExit == EXIT_FAIL) {
                        lastUsedStrategyTable = null;
                        return;
                    }
                    if (moveExit == SUCCESS_STILL_RUNNING) {
                        m.timer = -1;
                        return;
                    }

                    if (moveExit == EXIT_SUCCESS) {
                        if (m.timer == -1)
                            m.timer = worker.level().getGameTime();

                        if (worker.level().getGameTime() > m.timer + 20 * 10) {
                            m.index++;
                        }
                    }
                } else {
                    lastUsedStrategyTable = null;
                }
            } else {
                lastUsedStrategyTable = null;
            }
        }
    }
}
