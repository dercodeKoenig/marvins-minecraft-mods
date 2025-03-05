package NPCs.Npc.programs.Combat;

import NPCs.Npc.CombatNPC;
import NPCs.Npc.HostileEntities;
import NPCs.Npc.programs.TakeToolProgram;
import NPCs.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.EnumSet;
import java.util.Optional;

import static NPCs.Utils.EXIT_FAIL;
import static NPCs.Utils.EXIT_SUCCESS;
import static net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow;

public class RangedBowAttackProgram extends Goal {
    public CombatNPC npc;
    public double speedModifier;
    public int attackIntervalMin;
    public float attackRadiusSqr;
    public float attackRadius;
    public int attackTime;
    public int seeTime;
    public boolean strafingClockwise;
    public boolean strafingBackwards;
    public int strafingTime;
    long lastTimeSafeCheck;
    boolean shouldStrafe;
    public TakeToolProgram takeBowProgram;
    public TakeToolProgram takeArrowProgram;


    public RangedBowAttackProgram(CombatNPC npc, double speedModifier, int attackIntervalMin, float attackRadius) {
        this.attackTime = -1;
        this.strafingTime = -1;
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.attackRadius = attackRadius;
        takeArrowProgram = new TakeToolProgram(npc);
        takeBowProgram = new TakeToolProgram(npc);

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        boolean c =  this.npc.getTarget() != null && npc.getTarget().isAlive() &&
                takeBowProgram.hasTool(BowItem.class) &&
                takeArrowProgram.hasTool(ArrowItem.class) &&
                npc.hunger > npc.maxHunger * 0.05;
        return c;
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void start() {
        super.start();
        lastTimeSafeCheck = 0;
        //this.npc.setAggressive(true);
    }

    public void stop() {
        //this.npc.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.npc.stopUsingItem();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        takeBowProgram.takeToolToMainHand(BowItem.class);
        if (attackTime <= attackIntervalMin / 2 && this.seeTime >= -60 && !(npc.getOffhandItem().getItem() instanceof ArrowItem) && !npc.isUsingItem()) {
            int arrowIndex = takeArrowProgram.getToolIndex(ArrowItem.class);
            Utils.moveItemStackToOffHand(npc.combinedInventory.getStackInSlot(arrowIndex), npc);
            npc.swing(InteractionHand.OFF_HAND);
        }
        if (npc.isUsingItem()) {
            Utils.moveItemStackToOffHand(ItemStack.EMPTY, npc);
        }

        LivingEntity livingentity = this.npc.getTarget();
        if (livingentity != null) {
            double d0 = this.npc.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
            boolean flag = this.npc.getSensing().hasLineOfSight(livingentity);
            boolean flag1 = this.seeTime > 0;
            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if ((d0 < (double) this.attackRadiusSqr) && this.seeTime >= 20) {
                this.npc.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.npc.getNavigation().moveTo(livingentity.getX(), livingentity.getY(), livingentity.getZ(), (int) (attackRadius - 1), this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime > -1) {
                if (d0 > (double) (this.attackRadiusSqr * 0.85F)) {
                    this.strafingBackwards = false;
                } else if (d0 < (double) (this.attackRadiusSqr * 0.5F)) {
                    this.strafingBackwards = true;
                }

                if (lastTimeSafeCheck + 20 < npc.level().getGameTime()) {
                    lastTimeSafeCheck = npc.level().getGameTime();
                    if (npc.slowMobNavigation.pathFinder.findPath(npc.getTarget().getOnPos(), (int) (attackRadius*1.5), 2, (int) (attackRadius*1.7), 1000).exitCode == EXIT_SUCCESS) {
                        shouldStrafe = true;
                    } else {
                        shouldStrafe = false;
                    }
                }
                if (shouldStrafe) {

                    if (this.strafingTime >= 20) {
                        if ((double) this.npc.getRandom().nextFloat() < 0.3) {
                            this.strafingClockwise = !this.strafingClockwise;
                        }

                        if ((double) this.npc.getRandom().nextFloat() < 0.3) {
                            this.strafingBackwards = !this.strafingBackwards;
                        }

                        this.strafingTime = 0;
                    }

                    this.npc.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                }
                Entity var7 = this.npc.getControlledVehicle();
                if (var7 instanceof Mob) {
                    Mob mob = (Mob) var7;
                    mob.lookAt(livingentity, 30.0F, 30.0F);
                }

                //this.npc.lookAt(livingentity, 30.0F, 30.0F); // does this do anything?
                this.npc.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            } else {
                this.npc.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            }

            if (this.npc.isUsingItem()) {
                if (!flag && this.seeTime < -60) {
                    this.npc.stopUsingItem();
                } else if (flag) {
                    int i = this.npc.getTicksUsingItem();
                    if (i >= 25) {

                        double d8 = Double.MAX_VALUE;
                        Entity hitEntity = null;
                        for(Entity entity1 : npc.level().getEntities(npc, npc.getBoundingBox().expandTowards(npc.getTarget().position().subtract(npc.position())).inflate(1), (x)->true)) {
                            AABB aabb = entity1.getBoundingBox().inflate(1);
                            Optional<Vec3> optional = aabb.clip(npc.getEyePosition(0), npc.getTarget().getEyePosition());
                            if (optional.isPresent()) {
                                double d1 = npc.getEyePosition(0).distanceToSqr(optional.get());
                                if (d1 < d8) {
                                    hitEntity = entity1;
                                    d8 = d1;
                                }
                            }
                        }
                        if (hitEntity != null && !HostileEntities.isUnableToAttack(hitEntity, npc)) {
                            this.npc.stopUsingItem();
                            performRangedAttack(BowItem.getPowerForTime(i));
                            this.attackTime = this.attackIntervalMin;
                        }
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.npc.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.npc, (item) -> item instanceof BowItem));
            }
        }
    }


    public void performRangedAttack(float distanceFactor) {
        ItemStack weapon = npc.getMainHandItem();
        int arrowSlot = takeArrowProgram.getToolIndex(ArrowItem.class);
        if (arrowSlot < 0) return;
        ItemStack arrowStack = npc.combinedInventory.extractItem(arrowSlot, 1, false);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(npc, arrowStack, distanceFactor, weapon);
        Item var7 = weapon.getItem();
        if (var7 instanceof ProjectileWeaponItem weaponItem) {
            arrow = weaponItem.customArrow(arrow, arrowStack, weapon);
        }

        LivingEntity target = npc.getTarget();
        double d0 = target.getX() - npc.getX();
        double d1 = target.getY(0.5) - arrow.getY();
        double d2 = target.getZ() - npc.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2); // Horizontal distance

        float speed = 3f;
        float spread = 6f;
        double gravity = 0.05;
        double time = d3 / speed;
        double vy = (d1 + 0.5 * gravity * time * time) / time;
        arrow.shoot(d0/time, vy, d2/time, speed, spread);

        npc.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (npc.getRandom().nextFloat() * 0.4F + 0.8F));
        npc.level().addFreshEntity(arrow);
    }
}

