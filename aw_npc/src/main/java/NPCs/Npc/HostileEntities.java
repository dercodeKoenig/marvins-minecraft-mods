package NPCs.Npc;

import ARLib.utils.BlockIdentifier;
import Vehicles.Ballista.Ballista;
import Vehicles.SiegeEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.*;

public class HostileEntities {

    public static class TemporaryHostile {
        public UUID id;
        public long gameTimeStart = 0;
        public int tickDuration = 300;
    }

    public static HashMap<BlockIdentifier, Set<TemporaryHostile>> hostilesToTownhall = new HashMap<>();

    public static void addTemporaryHostile(Entity e, NPCBase npc, int tickDuration) {
        if (npc.townHall != null) {
            BlockIdentifier id = new BlockIdentifier(npc.level(), npc.townHall);
            Set<TemporaryHostile> hostiles = hostilesToTownhall.get(id);
            if (hostiles == null)
                hostiles = new HashSet<>();
            TemporaryHostile t = new TemporaryHostile();
            t.id = e.getUUID();
            t.tickDuration = tickDuration;
            t.gameTimeStart = npc.level().getGameTime();
            hostiles.add(t);
            hostilesToTownhall.put(id, hostiles);
        }
    }

    public static boolean isUnableToAttack(Entity e, NPCBase npc) {
        if (e instanceof NPCBase otherNPC) {
            if (npc.isFriendlyTo(otherNPC)) {
                return true;
            }
        }

        if (e instanceof Player p) {
            String pName = p.getName().getString();
            if (npc.isFriendlyTo(p)) {
                return true;
            }
        }

        if (npc.level() instanceof ServerLevel serverLevel) {
            // try to not attack siege engines
            if (e instanceof Ballista b) {
                if (b.controllingEntity == null || isUnableToAttack(serverLevel.getEntity(b.controllingEntity), npc)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean shouldAttack(Entity e, NPCBase npc) {

        if (isUnableToAttack(e, npc))
            return false;

        // attack monsters
        if (e instanceof Monster)
            if (!(e instanceof Creeper))
                return true;

        if (e instanceof NPCBase)
            return true;
        if (e instanceof Player)
            return true;

        // attack enemy controlled siege engines
        if (npc.level() instanceof ServerLevel serverLevel) {
            if (e instanceof Ballista b) {
                if (b.controllingEntity != null && shouldAttack(serverLevel.getEntity(b.controllingEntity), npc) && !b.getEntityData().get(SiegeEngine.IS_BROKEN)) {
                    return true;
                }
            }
        }


        // the following attacks temporary hostiles
        // cleanup old entries
        for (BlockIdentifier id : hostilesToTownhall.keySet()) {
            Set<TemporaryHostile> temporaryHostiles = hostilesToTownhall.get(id);
            for (TemporaryHostile i : new HashSet<>(temporaryHostiles)) {
                if (i.gameTimeStart + i.tickDuration < npc.level().getGameTime())
                    temporaryHostiles.remove(i);
            }
            if (temporaryHostiles.isEmpty()) {
                hostilesToTownhall.remove(id);
                break;
            }
        }
        if (npc.townHall != null) {
            Set<TemporaryHostile> temporaryHostiles = hostilesToTownhall.get(new BlockIdentifier(npc.level(), npc.townHall));
            if (temporaryHostiles != null) {
                for (TemporaryHostile i : new HashSet<>(temporaryHostiles)) {
                    if (i.id.equals(e.getUUID())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Monster && e instanceof Mob mob && !(e instanceof Creeper)) {
            Goal attackGoal1 = new NearestAttackableTargetGoal<>(mob, NPCBase.class, 20, true, true, (entity) -> true);
            ((Monster) e).goalSelector.addGoal(1, attackGoal1);

            Goal attackGoal2 = new NearestAttackableTargetGoal<>(mob, SiegeEngine.class, 20, true, true, (entity) -> {
                return !entity.getEntityData().get(SiegeEngine.IS_BROKEN);
            });
            ((Monster) e).goalSelector.addGoal(2, attackGoal2);
        }
    }
}
