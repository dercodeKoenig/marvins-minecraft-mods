package NPCs;

import NPCs.TownHall.TownHallOwners;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Objects;

public class HostileEntities {

    public static boolean shouldAttack(Entity e, NPCBase npc) {

        // attack monsters
        if (e instanceof Monster)
            if (!(e instanceof Creeper))
                return true;

        // attack other npcs that are not owned by the owner of this npc && that are not owned by any owner of the townhall
        if (npc.townHall != null && e instanceof NPCBase otherNPC) {
            if (!TownHallOwners.getOwners(npc.level(), npc.townHall).contains(otherNPC.owner)) {
                if (!Objects.equals(npc.owner, otherNPC.owner)) {
                    return true;
                }
            }
        }

        // attack players that are not owner of this npc && that are not a owner of the townhall that this npc belongs to
        if (e instanceof Player p) {
            if (!TownHallOwners.getOwners(npc.level(), npc.townHall).contains(p.getUUID())) {
                if (!p.getName().getString().equals(npc.owner)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Monster && e instanceof Mob mob && !(e instanceof Creeper)) {
            Goal attackGoal = new NearestAttackableTargetGoal<NPCBase>(mob, NPCBase.class, 20, true, true, (entity) -> true);
            ((Monster) e).goalSelector.addGoal(1, attackGoal);
        }
    }
}
