package Vehicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class Ballista extends Entity {

    private static final EntityDataAccessor<Float> DRAW_PROGRESS = SynchedEntityData.defineId(Ballista.class, EntityDataSerializers.FLOAT);


    float client_drawProcess;
    float client_drawProcessPrev;
    int clien_ticksAfterShoot = 0;
    double client_lastYRot = 0;
    double client_currentYRot = 0;

    UUID controllingEntity;
boolean shouldReload = false;
    BallistaBolt bolt = null;

    public Ballista(EntityType<Ballista> entityType, Level level) {
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

            if (shouldReload)
                setDrawProcess(getDrawProcess() + 0.05f);

            if (getDrawProcess() == 1 && bolt == null) {
                List<BallistaBolt> bolts = level().getEntitiesOfClass(BallistaBolt.class, getBoundingBox());
                if (!bolts.isEmpty())
                    bolt = bolts.getFirst();
                else {
                    bolt = new BallistaBolt(Registry.ENTITY_BALLISTA_BOLT.get(), level());
                    level().addFreshEntity(bolt);
                }
                bolt.setOwner(this);
                bolt.setNoGravity(true);
            }

            if (controllingEntity != null) {
                Entity controller = serverLevel.getEntity(controllingEntity);

                if (controller.getPosition(0).distanceTo(getPosition(0)) > 4 || !(controller instanceof LivingEntity)) {
                    controllingEntity = null;
                } else {
                    shouldReload = true;

                    float yRotTarget = controller.getYRot();
                    float xRotTarget = controller.getXRot();

                    float yRotCurrent = getYRot();
                    float xRotCurrent = getXRot();

                    float yRotDiff = yRotTarget - yRotCurrent;
                    if (Math.abs(yRotDiff - 360) < Math.abs(yRotDiff))
                        yRotDiff -=360;
                    if (Math.abs(yRotDiff + 360) < Math.abs(yRotDiff))
                        yRotDiff += 360;

                    float toRotateY = Math.clamp(yRotDiff, -1f, 1f);

                    float xRotDiff = xRotTarget - xRotCurrent;
                    float toRotateX = Math.clamp(xRotDiff, -5f, 5f);

                    setRot(yRotCurrent + toRotateY, xRotCurrent + toRotateX);
                }
            }
            if (bolt != null) {
                bolt.setPos(getPosition(0).add(0, 1, 0));
                bolt.setXRot(-getXRot());
                bolt.setYRot(getYRot()-180);
            }
        }
        if (level().isClientSide) {

            client_lastYRot = client_currentYRot;

            if(getYRot() < client_currentYRot - 180){
                client_currentYRot -= 360;
                client_lastYRot -=360;
            }
            if(getYRot() > client_currentYRot + 180){
                client_currentYRot += 360;
                client_lastYRot +=360;
            }
            float yRotDiff = (float) (getYRot()-client_currentYRot);

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
                if (getDrawProcess() == 1) {
                    if (bolt != null) {
                        bolt.setDeltaMovement(getLookAngle().scale(4));
                        setDrawProcess(-1);
                        bolt.setNoGravity(false);
                        bolt = null;
                        //shouldReload = false;
                    }
                }else{
                    shouldReload = true;
                }
            } else {
                if (!player.getUUID().equals(controllingEntity)) {
                    controllingEntity = player.getUUID();
                } else {
                    controllingEntity = null;
                }
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
