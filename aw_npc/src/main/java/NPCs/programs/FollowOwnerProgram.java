package NPCs.programs;

import NPCs.NPCBase;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

import static NPCs.Utils.EXIT_FAIL;
import static NPCs.Utils.EXIT_SUCCESS;

public class FollowOwnerProgram extends Goal {
    NPCBase worker;

    public FollowOwnerProgram(NPCBase worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return worker.followOwner != null;
    }

    @Override
    public void tick() {
        if (worker.followOwner == null) return;

        if (worker.level() instanceof ServerLevel l) {
            Entity owner = l.getEntity(worker.followOwner);
            if (owner instanceof Player) {
                int moveExit = worker.slowMobNavigation.moveToPosition(
                        owner.getOnPos(),
                        5, worker.slowNavigationMaxDistance, worker.slowNavigationMaxNodes, worker.slowNavigationStepPerTick
                );
                if (moveExit == EXIT_FAIL) {
                    worker.followOwner = null;
                }
                if (moveExit == EXIT_SUCCESS) {
                    worker.lookAt(EntityAnchorArgument.Anchor.EYES, owner.getEyePosition());
                }
            } else {
                worker.followOwner = null;
            }
        }
    }
}

