package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.HostileEntities;
import NPCs.Utils;
import Vehicles.Ballista.Ballista;
import Vehicles.Config;
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
    public long lastCheck;
    public Ballista ballista;


    public BallistaAttackProgram(CombatNPC npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
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
        if (npc.level().getGameTime() < lastCheck + 20 * 1) {
            return false;
        }
        lastCheck = npc.level().getGameTime();

        System.out.println("scan");

        if (!(npc.hunger > npc.maxHunger * 0.05)) return false;
        TreeSet<Ballista> nearbyBallistas = sortedEntitiesByDistanceTo(npc.level().getEntitiesOfClass(Ballista.class, new AABB(npc.blockPosition()).inflate(64)), npc.position());
        TreeSet<LivingEntity> nearbyTargets = sortedEntitiesByDistanceTo(npc.level().getEntitiesOfClass(LivingEntity.class, new AABB(npc.blockPosition()).inflate(64), (entity) -> HostileEntities.shouldAttack(entity, npc)), npc.position());

        for (LivingEntity target : nearbyTargets) {
            entityTest:
            {
                boolean c = target.isAlive() && npc.position().distanceTo(target.position()) > 6;
                if (!c) break entityTest;


                for (Ballista i : nearbyBallistas) {
                    // do not work this ballista when other hostile creatures are around. consider it a enemy ballista
                    // only applies when i am far away so i do not run into enemy lines
                    if(i.position().distanceTo(npc.position()) > 8) {
                        List<LivingEntity> entitiesAroundBallista = npc.level().getEntitiesOfClass(LivingEntity.class, new AABB(i.blockPosition()).inflate(8));
                        for (LivingEntity j : entitiesAroundBallista) {
                            if (HostileEntities.shouldAttack(j, npc)) {
                                System.out.println(i + ":" + target + ":tc");
                                break entityTest;
                            }
                        }
                    }

                    if (i.bolt == null){
                        System.out.println(i+":"+target+": no bolt");
                        break entityTest;
                    }

                    List<Entity> entities = npc.level().getEntities((Entity) null, i.getBoundingBox().expandTowards(target.position().subtract(i.position())).inflate(1), (Predicate<Entity>) entity -> true);
                    // scan for friendly entities in area
                    for (Entity entity1 : entities) {
                        AABB aabb = entity1.getBoundingBox().inflate(1);
                        Optional<Vec3> optional = aabb.clip(i.bolt.position(), i.bolt.position().add(target.position().subtract(i.bolt.position()).normalize().scale(100)));
                        if (optional.isPresent()) {
                            if (entity1 != i && entity1 != npc) {
                                if (HostileEntities.isUnableToAttack(entity1, npc)) {
                                    System.out.println(i+":"+target+":ff");
                                    break entityTest;
                                }
                            }
                        }
                    }

                    boolean canSee = i.hasLineOfSight(target);
                    if (canSee && (i.controllingEntity == null || Objects.equals(i.controllingEntity, npc.getUUID())) && isPositionWorkable(i.blockPosition()) && i.getEntityData().get(Ballista.CONSTRUCTION_PROGRESS) == 17 && !i.getEntityData().get(Ballista.IS_BROKEN)) {
                        ballista = i;
                        lockTargetPosition();
                        npc.setTarget(target);
                        return true;
                    }
                    System.out.println("no");
                }
            }
        }
        System.out.println("no work");
        return false;
    }


    public boolean canContinueToUse() {
        return ballista != null && npc.getTarget() != null && npc.getTarget().isAlive() && npc.hunger > npc.maxHunger * 0.05 && npc.position().distanceTo(npc.getTarget().position()) > 6;
    }

    public void start() {
        super.start();
    }

    public void stop() {
        if (ballista != null && Objects.equals(ballista.controllingEntity, npc.getUUID()))
            ballista.controllingEntity = null;
        ballista = null;
        npc.setTarget(null);
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
            if (distToTarget > 1 && distToTarget < 2) {
                if (npc.slowMobNavigation.pathFinder.findPath(new BlockPos(Mth.floor(targetPosition.x), Mth.floor(targetPosition.y), Mth.floor(targetPosition.z)), 5, 0, 10, 1000).exitCode == EXIT_SUCCESS) {
                    npc.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, 1);
                    return;
                }
            } else if (distToTarget >= 2) {
                npc.slowMobNavigation.moveToPosition(new BlockPos(Mth.floor(targetPosition.x), Mth.floor(targetPosition.y), Mth.floor(targetPosition.z)), 1, 10, 10, 10, 1);
            } else {
                npc.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            }
            ballista.controllingEntity = npc.getUUID();

            //double distanceToSqr = this.npc.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            boolean lineOfSight = ballista.hasLineOfSight(livingentity);
            if (lineOfSight) {
                // Calculate differences between the ballista's bolt and the target entity.
                double dx = livingentity.getX() - ballista.bolt.getX();
                double dz = livingentity.getZ() - ballista.bolt.getZ();
// Horizontal distance (x-z plane)
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

// Compute the horizontal (yaw) angle: arctan2 returns the angle relative to the x-axis,
// so adjust by -90 degrees if necessary (depending on your coordinate system).
                double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
                ballista.targetYRot = (float) targetYaw;

// Vertical difference between the target and the bolt.
                double dy = livingentity.getY(0.5) - ballista.bolt.getY();

                float speed = Config.INSTANCE.ballista_bolt_velocity;
                double gravity = 0.05;

// Precompute some values for clarity.
                double speedSq = speed * speed;

// Compute the discriminant of the quadratic equation for tan(theta)
// v^4 - g*(g*d^2 + 2*dy*v^2)
                double discriminant = speedSq * speedSq - gravity * (gravity * horizontalDistance * horizontalDistance + 2 * dy * speedSq);

                if (discriminant < 0) {
                    // If the discriminant is negative, the target is unreachable with the given speed.
                    // Use a default angle (here, -45 degrees) or handle the error as needed.
                    ballista.targetXRot = -45.0f;
                } else {
                    double sqrtDisc = Math.sqrt(discriminant);
                    // Use the lower trajectory solution (minus sign) for a more direct shot.
                    double tanTheta = (speedSq - sqrtDisc) / (gravity * horizontalDistance);
                    double theta = Math.atan(tanTheta);

                    // The targetXRot is set to the negative of the angle in degrees (to match your coordinate system conventions).
                    ballista.targetXRot = (float) (-Math.toDegrees(theta));
                }


                if (Math.abs(ballista.getXRot() - ballista.targetXRot) < 0.2 && Math.abs((360 + ballista.getYRot()) % 360 - (360 + ballista.targetYRot) % 360) < 0.2) {

                    List<Entity> entities = npc.level().getEntities((Entity) null, ballista.getBoundingBox().expandTowards(npc.getTarget().position().subtract(ballista.position())).inflate(1), (Predicate<Entity>) entity -> true);
                    boolean freeToFire = true;
                    // scan for friendly entities in area
                    for (Entity entity1 : entities) {
                        AABB aabb = entity1.getBoundingBox().inflate(1);
                        Optional<Vec3> optional = aabb.clip(ballista.bolt.position(), ballista.bolt.position().add(ballista.getLookAngle().normalize().scale(100)));
                        if (optional.isPresent()) {
                            if (entity1 != ballista && entity1 != npc) {
                                if (HostileEntities.isUnableToAttack(entity1, npc)) {
                                    freeToFire = false;
                                    ballista = null;
                                    break;
                                }
                            }
                        }
                    }

                    if (freeToFire && distToTarget <= 1) {
                        npc.swing(InteractionHand.MAIN_HAND);
                        ballista.shoot();
                        lastCheck = 0;
                    }
                }

            }
        } else {
            ballista = null;
        }
    }
}

