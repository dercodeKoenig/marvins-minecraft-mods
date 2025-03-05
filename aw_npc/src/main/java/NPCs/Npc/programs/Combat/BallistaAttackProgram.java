package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.HostileEntities;
import NPCs.Utils;
import Vehicles.Ballista.Ballista;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

import static NPCs.Utils.*;

public class BallistaAttackProgram extends Goal {
    public static HashMap<BlockPos, Long> positionsInUseWithLastUseTime = new HashMap<>();

    public CombatNPC npc;
    public double speedModifier;
    public int attackWait;
    public float attackRadiusSqr;
    public float attackRadius;
    public long lastCheck;
    public Ballista ballista;
    int attackTime;


    public BallistaAttackProgram(CombatNPC npc, double speedModifier, int attackWait) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.attackWait = attackWait;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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

        boolean c = npc.getTarget() != null && npc.getTarget().isAlive() && npc.hunger > npc.maxHunger * 0.05 && npc.position().distanceTo(npc.getTarget().position()) > 6;
        if (!c) return false;

        List<Ballista> nearbyBallistas = npc.level().getEntitiesOfClass(Ballista.class, new AABB(npc.blockPosition()).inflate(128));
        for (Ballista i : sortedEntitiesByDistanceTo(nearbyBallistas, npc.position())) {
            // do not work this ballista when other hostile creatures are around. consider it a enemy ballista
            List<LivingEntity> entitiesAroundBallista = npc.level().getEntitiesOfClass(LivingEntity.class, new AABB(i.blockPosition()).inflate(8));
            for (LivingEntity j : entitiesAroundBallista) {
                if (HostileEntities.shouldAttack(j, npc)) {
                    //System.out.println(i+":tc");
                    return false;
                }
            }

            if (i.bolt == null) return false;

            List<Entity> entities = npc.level().getEntities((Entity) null, i.getBoundingBox().expandTowards(npc.getTarget().position().subtract(i.position())).inflate(1), (Predicate<Entity>) entity -> true);
            // scan for friendly entities in area
            for (Entity entity1 : entities) {
                AABB aabb = entity1.getBoundingBox().inflate(1);
                Optional<Vec3> optional = aabb.clip(i.bolt.position(), i.bolt.position().add(npc.getTarget().position().subtract(i.bolt.position()).normalize().scale(100)));
                if (optional.isPresent()) {
                    if(entity1 != i) {
                        if (HostileEntities.isUnableToAttack(entity1, npc)) {
                            //System.out.println(i+":ff");
                            return false;
                        }
                    }
                }
            }

            Vec3 vec3 = new Vec3(i.getX(), i.getEyeY(), i.getZ());
            Vec3 vec31 = new Vec3(npc.getTarget().getX(), npc.getTarget().getEyeY(), npc.getTarget().getZ());
            boolean canSee = npc.level().clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS;

            if (canSee && (i.controllingEntity == null || Objects.equals(i.controllingEntity, npc.getUUID())) && isPositionWorkable(i.blockPosition())  && i.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) == 17 && !i.getEntityData().get(Ballista.IS_BROKEN)) {
                ballista = i;
                lockTargetPosition();
                return true;
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        return ballista != null && npc.getTarget() != null && npc.getTarget().isAlive() && npc.hunger > npc.maxHunger * 0.05 && npc.position().distanceTo(npc.getTarget().position()) > 6;
    }

    public void start() {
        super.start();
        attackTime = 0;
    }

    public void stop() {
        if (ballista != null && Objects.equals(ballista.controllingEntity, npc.getUUID()))
            ballista.controllingEntity = null;
        ballista = null;
        super.stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {

        if (ballista == null || (ballista.controllingEntity != null && !Objects.equals(npc.getUUID(), ballista.controllingEntity)) || ballista.bolt == null || ballista.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) != 17 || ballista.getEntityData().get(Ballista.IS_BROKEN)) {
            ballista = null;
            return;
        }
        lockTargetPosition();

        ballista.controllingEntity = npc.getUUID();

        double distToTarget;
        int exit = npc.slowMobNavigation.moveToPosition(ballista.blockPosition(), 2, npc.slowNavigationMaxDistance, npc.slowNavigationMaxNodes, npc.slowNavigationStepPerTick, (float) speedModifier);
        if (exit == SUCCESS_STILL_RUNNING)
            return;
        if (exit == EXIT_FAIL) {
            //System.out.println("failed to move to "+ballista);
            ballista = null;
            return;
        }

        LivingEntity livingentity = this.npc.getTarget();
        if (livingentity != null) {
            Vec3 look = ballista.calculateViewVector(ballista.getXRot(), ballista.getYRot());
            Vec3 lookNoY = new Vec3(look.x, 0.0, look.z);
            Vec3 targetPosition = ballista.position().subtract(lookNoY.normalize().scale(2.0)).add(new Vec3((double) 0.0F, (double) 0.5F, (double) 0.0F));
            distToTarget = npc.position().distanceTo(targetPosition);
            if (distToTarget > 1) {
                if (npc.slowMobNavigation.pathFinder.findPath(new BlockPos(Mth.floor(targetPosition.x), Mth.floor(targetPosition.y), Mth.floor(targetPosition.z)), 5, 0, 10, 1000).exitCode == EXIT_SUCCESS) {
                    npc.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, 1);
                    return;
                }
            } else {
                npc.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            }
            ballista.controllingEntity = npc.getUUID();

            //double distanceToSqr = this.npc.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            boolean lineOfSight = this.npc.getSensing().hasLineOfSight(livingentity);
            if (lineOfSight) {
                double d0 = livingentity.getX() - ballista.bolt.getX();
                double d1 = livingentity.getZ() - ballista.bolt.getZ();
                double targetYRot = (Mth.atan2(d1, d0) * (double) 180.0F / (double) (float) Math.PI) - 90.0F;
                ballista.targetYRot = (float) targetYRot;
                if (Math.abs((360 + ballista.getYRot()) % 360 - (360 + ballista.targetYRot) % 360) < 0.1) {
                    double d2 = livingentity.getY(0.5) - (ballista.bolt.getY());
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1); // Horizontal distance

                    float speed = 8f;
                    double gravity = 0.05;
                    double time = d3 / speed;
                    double vy = (d2 + 0.5 * gravity * time * time) / time;

                    ballista.targetXRot = -(float) (Math.atan(vy / speed) * 180f / Math.PI);

                    attackTime++;
                    if (Math.abs(ballista.getXRot() - ballista.targetXRot) < 0.05 && attackTime > attackWait) {

                        List<Entity> entities = npc.level().getEntities((Entity) null, ballista.getBoundingBox().expandTowards(npc.getTarget().position().subtract(ballista.position())).inflate(1), (Predicate<Entity>) entity -> true);
                        boolean freeToFire = true;
                        // scan for friendly entities in area
                        for (Entity entity1 : entities) {
                            AABB aabb = entity1.getBoundingBox().inflate(1);
                            Optional<Vec3> optional = aabb.clip(ballista.bolt.position(), ballista.bolt.position().add(ballista.getLookAngle().normalize().scale(100)));
                            if (optional.isPresent()) {
                                if(entity1 != ballista) {
                                    if (HostileEntities.isUnableToAttack(entity1, npc)) {
                                        freeToFire = false;
                                        break;
                                    }
                                }
                            }
                        }

                        if (freeToFire && distToTarget <= 1) {
                            npc.swing(InteractionHand.MAIN_HAND);
                            ballista.shoot();
                            attackTime = 0;
                        }
                    }

                }
            }
        } else {
            ballista = null;
        }
    }
}

