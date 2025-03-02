package Vehicles;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public class EntityBallista extends Entity {

    private static final EntityDataAccessor<Float> DRAW_PROGRESS = SynchedEntityData.defineId(EntityBallista.class, EntityDataSerializers.FLOAT);


    float client_drawProcess;
    float client_drawProcessPrev;
    int clien_ticksAfterShoot = 0;
    double client_lastYRot = 0;
    double client_currentYRot = 0;

    UUID controllingEntity;

    public EntityBallista(EntityType<EntityBallista> entityType, Level level) {
        super(entityType, level);
    }


    public float getDrawProcess() {
        return getEntityData().get(DRAW_PROGRESS);
    }

    public void setDrawProcess(float process) {
        process = Math.clamp(process,-1,1);
        getEntityData().set(DRAW_PROGRESS, process);
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel serverLevel) {

            applyGravity();
            Vec3 vec3d1 = this.getDeltaMovement();
            this.move(MoverType.SELF, new Vec3(vec3d1.x, vec3d1.y, vec3d1.z));

            if (controllingEntity != null) {
                Entity controller = serverLevel.getEntity(controllingEntity);
                if(controller.getPosition(0).distanceTo(getPosition(0))>4){
                    controllingEntity = null;
                }

                setDrawProcess(getDrawProcess() + 0.05f);
                //if (getDrawProcess() > 1.05) {
                //    setDrawProcess(-1);
                //}

                if (controller instanceof LivingEntity l) {
                    float yRotTarget = controller.getYRot();
                    float xRotTarget = controller.getXRot();

                    float yRotCurrent = getYRot();
                    float xRotCurrent = getXRot();

                    float yRotDiff = 99999;
                    float yRotDiff0 = yRotTarget - yRotCurrent;
                    float yRotDiff1 = yRotTarget - yRotCurrent - 360;
                    float yRotDiff2 = yRotTarget - yRotCurrent + 360;
                    if (Math.abs(yRotDiff0) < Math.abs(yRotDiff))
                        yRotDiff = yRotDiff0;
                    if (Math.abs(yRotDiff1) < Math.abs(yRotDiff))
                        yRotDiff = yRotDiff1;
                    if (Math.abs(yRotDiff2) < Math.abs(yRotDiff))
                        yRotDiff = yRotDiff2;

                    float toRotateY = Math.clamp(yRotDiff, -1f, 1f);
                    setYRot(yRotCurrent + toRotateY);

                    float xRotDiff = 99999;
                    float xRotDiff0 = xRotTarget - xRotCurrent;
                    float xRotDiff1 = xRotTarget - xRotCurrent - 360;
                    float xRotDiff2 = xRotTarget - xRotCurrent + 360;
                    if (Math.abs(xRotDiff0) < Math.abs(xRotDiff))
                        xRotDiff = xRotDiff0;
                    if (Math.abs(xRotDiff1) < Math.abs(xRotDiff))
                        xRotDiff = xRotDiff1;
                    if (Math.abs(xRotDiff2) < Math.abs(xRotDiff))
                        xRotDiff = xRotDiff2;

                    float toRotateX = Math.clamp(xRotDiff, -5f, 5f);
                    setXRot(xRotCurrent + toRotateX);


                    //Vec3 targetPos = getPosition(0).subtract(calculateViewVector(0,yRotTarget).scale(2));
                    Vec3 look = getLookAngle();
                    Vec3 lookNoY = new Vec3(look.x,0,look.z);

                    Vec3 targetPos = getPosition(0).subtract(lookNoY.normalize().scale(2));

                    //controller.teleportTo(targetPos.x, getY(), targetPos.z);

                } else controllingEntity = null;
            }
        }
        if (level().isClientSide) {

            client_lastYRot = client_currentYRot;
            float yRotDiff = 99999;
            float yRotDiff0 = (float) (getYRot()-client_currentYRot);
            float yRotDiff1 = (float) (getYRot()-client_currentYRot - 360);
            float yRotDiff2 = (float) (getYRot()-client_currentYRot + 360);
            if (Math.abs(yRotDiff0) < Math.abs(yRotDiff))
                yRotDiff = yRotDiff0;
            if (Math.abs(yRotDiff1) < Math.abs(yRotDiff))
                yRotDiff = yRotDiff1;
            if (Math.abs(yRotDiff2) < Math.abs(yRotDiff))
                yRotDiff = yRotDiff2;
            client_currentYRot += (yRotDiff)*0.3;

            if (getDrawProcess() <= 0)
                clien_ticksAfterShoot++;
            else
                clien_ticksAfterShoot = 0;

            client_drawProcessPrev = client_drawProcess; // Store previous value
            if (getDrawProcess() <= 0) {
                client_drawProcess -= Math.min(0.5f, client_drawProcess);
            } else {
                client_drawProcess += (getDrawProcess() - client_drawProcess) * 0.1f; // Smoothly lerp
            }
        }


    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            if (!player.isShiftKeyDown()) {
                if (!player.getUUID().equals(controllingEntity)) {
                    controllingEntity = player.getUUID();
                    //player.startRiding(this);
                } else if (getDrawProcess() == 1) {

                    AbstractArrow a = new Arrow(level(), player, new ItemStack(Items.ARROW), null) {
                        protected void onHitEntity(EntityHitResult result) {
                            System.out.println(result.getEntity());
                            if (result.getEntity().equals(EntityBallista.this) || result.getEntity().equals(player))
                                return;
                            super.onHitEntity(result);
                        }
                    };
                    level().addFreshEntity(a);
                    a.shoot(getLookAngle().x, getLookAngle().y, getLookAngle().z, 8, 1);
                    setDrawProcess(-1);
                }
            } else {
                controllingEntity = null;
            }
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected double getDefaultGravity() {
        return (double) 0.1F;
    }

    public Vec3 getPassengerRidingPosition(Entity entity) {
        Vec3 look = calculateViewVector((float) getX(), (float) client_currentYRot);
        Vec3 lookNoY = new Vec3(look.x,0,look.z);

        Vec3 targetPos = getPosition(0).subtract(lookNoY.normalize().scale(2)).add(new Vec3(0,0.5,0));
        return targetPos;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DRAW_PROGRESS, 0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean isPickable() {
        return !this.isRemoved();
    }

}
