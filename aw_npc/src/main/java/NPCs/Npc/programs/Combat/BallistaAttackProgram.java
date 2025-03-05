package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.HostileEntities;
import NPCs.Npc.programs.TakeToolProgram;
import NPCs.Utils;
import Vehicles.Ballista;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
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
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.attackRadius = attackRadius;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void lockTargetPosition() {
        long gameTime = npc.level().getGameTime();
        positionsInUseWithLastUseTime.put(ballista.blockPosition(), gameTime);
    }

    public boolean isPositionLocked(BlockPos p) {
        // if I lock the position, it is not locked for ME, only for OTHER WORKERS
        if (ballista !=null && Objects.equals(p, ballista.blockPosition())) return false;

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

        boolean c = this.npc.getTarget() != null && npc.getTarget().isAlive() && npc.hunger > npc.maxHunger * 0.05;
        if (!c) return false;

        List<Ballista> nearbyBallistas = npc.level().getEntitiesOfClass(Ballista.class, new AABB(npc.blockPosition()).inflate(64));
        for (Ballista i : nearbyBallistas) {
            if (isPositionWorkable(i.blockPosition()) && i.bolt != null && i.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) == 17 && !i.getEntityData().get(Ballista.IS_BROKEN)) {
                ballista = i;
                lockTargetPosition();
                return true;
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        return ballista != null && this.npc.getTarget() != null && npc.getTarget().isAlive() && npc.hunger > npc.maxHunger * 0.05;
    }

    public void start() {
        super.start();
        attackTime = 0;
    }

    public void stop() {
        if (ballista != null && Objects.equals(ballista.controllingEntity, npc.getUUID()))
            ballista.controllingEntity = null;
        super.stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {

        if (ballista == null || ballista.bolt == null || ballista.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) != 17 || ballista.getEntityData().get(Ballista.IS_BROKEN)) {
            ballista = null;
            return;
        }
        lockTargetPosition();

        double distToTarget = Utils.distanceManhattan(npc.position(), ballista.position());
        if (distToTarget > 5.5) {
            int exit = npc.slowMobNavigation.moveToPosition(ballista.blockPosition(), 4, npc.slowNavigationMaxDistance, npc.slowNavigationMaxNodes, npc.slowNavigationStepPerTick, (float) speedModifier);
            if (exit == SUCCESS_STILL_RUNNING)
                return;
            if (exit == EXIT_FAIL) {
                ballista = null;
                return;
            }
        }

        LivingEntity livingentity = this.npc.getTarget();
        if (livingentity != null) {

            Vec3 look = ballista.calculateViewVector(ballista.getXRot(), ballista.getYRot());
            Vec3 lookNoY = new Vec3(look.x, 0.0, look.z);
            Vec3 targetPosition = ballista.position().subtract(lookNoY.normalize().scale(2.0)).add(new Vec3((double) 0.0F, (double) 0.5F, (double) 0.0F));
            distToTarget = Utils.distanceManhattan(npc.position(), targetPosition);
            if (distToTarget > 1) {
                npc.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, 1);
                return;
            } else {
                npc.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            }
            ballista.controllingEntity = npc.getUUID();

            //double distanceToSqr = this.npc.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            boolean lineOfSight = this.npc.getSensing().hasLineOfSight(livingentity);
            if (lineOfSight) {
                double d0 = livingentity.getX() - ballista.getX();
                double d1 = livingentity.getZ() - ballista.getZ();
                double targetYRot = (Mth.atan2(d1, d0) * (double) 180.0F / (double) (float) Math.PI) - 90.0F;
                ballista.targetYRot = (float) targetYRot;
                if (Math.abs((360+ballista.getYRot()) % 360 - (360 + ballista.targetYRot) % 360) < 0.1) {
                    double d2 = livingentity.getY(0.5) - ballista.getY();
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1); // Horizontal distance

                    float speed = 8f;
                    double gravity = 0.05;
                    double time = d3 / speed;
                    double vy = (d2 + 0.5 * gravity * time * time) / time;

                    ballista.targetXRot = (float) Math.atan(vy / d3);

                    attackTime++;
                    System.out.println(attackTime);
                    if (Math.abs(ballista.getXRot() - ballista.targetXRot) < 0.05 && attackTime > attackWait) {

                        List<Entity> entities = npc.level().getEntities((Entity) null, npc.getBoundingBox().expandTowards(npc.getTarget().getPosition(0).subtract(npc.getPosition(0))).inflate(1), (Predicate<Entity>) entity -> entity instanceof LivingEntity);
                        boolean freeToFire = true;
                        // scan for friendly entities in area
                        for (Entity entity1 : entities) {
                            AABB aabb = entity1.getBoundingBox().inflate(2);
                            Optional<Vec3> optional = aabb.clip(ballista.bolt.position(), ballista.bolt.position().add(ballista.getLookAngle().normalize().scale(100)));
                            if (optional.isPresent()) {
                                if (HostileEntities.isUnableToAttack(entity1, npc)) {
                                    freeToFire = false;
                                    break;
                                }
                            }
                        }

                        if (freeToFire) {
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

