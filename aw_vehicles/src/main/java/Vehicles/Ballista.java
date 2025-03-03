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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class Ballista extends Entity {

    private static final EntityDataAccessor<Float> DRAW_PROGRESS = SynchedEntityData.defineId(Ballista.class, EntityDataSerializers.FLOAT);


    float client_drawProgress;
    float client_drawProgressPrev;
    int clien_ticksAfterShoot = 0;
    float client_lastYRot = 0;
    float client_currentYRot = 0;
    float client_lastxRot = 0;
    float client_currentxRot = 0;

    UUID controllingEntity;
    BallistaBolt bolt = null;

    int reloadTicksRemaining = 0;

    public Ballista(EntityType<Ballista> entityType, Level level) {
        super(entityType, level);
    }


    public float getDrawProgress() {
        return getEntityData().get(DRAW_PROGRESS);
    }

    public void setDrawProgress(float process) {
        process = Math.clamp(process, -1, 1);
        getEntityData().set(DRAW_PROGRESS, process);
    }

    @Override
    public void onAddedToLevel(){
        super.onAddedToLevel();
        checkExistingBolt();
    }

    public void setBoltPosition(){
        if (bolt != null) {
            bolt.setPos(getPosition(0).add(0, 1, 0));
            bolt.setXRot(-getXRot());
            bolt.setYRot(getYRot() - 180);
        }
    }
    public void checkExistingBolt(){
        if (getDrawProgress() == 1 && bolt == null) {
            List<BallistaBolt> bolts = level().getEntitiesOfClass(BallistaBolt.class, getBoundingBox());
            if (!bolts.isEmpty()) {
                for (BallistaBolt i : bolts) {
                    if (!i.shotEnd) {
                        bolt = i;
                        bolt.owner = this;
                        bolt.hitEntities.clear();
                        bolt.setNoGravity(true);
                        setBoltPosition();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        Vec3 c = blockPosition().getCenter();
        setPos(c.x, getY(), c.z);
        super.tick();

        if (level() instanceof ServerLevel serverLevel) {

            applyGravity();
            Vec3 vec3d1 = this.getDeltaMovement();
            this.move(MoverType.SELF, new Vec3(vec3d1.x, vec3d1.y, vec3d1.z));

            if(getDrawProgress() < 0){
                setDrawProgress(Math.min(getDrawProgress() + 0.05f,0));
            }
            else if (reloadTicksRemaining > 0) {
                setDrawProgress(getDrawProgress() + 0.02f);
                reloadTicksRemaining -=1;
            }

            if (controllingEntity != null) {
                Entity controller = serverLevel.getEntity(controllingEntity);
                if (controller == null || controller.getPosition(0).distanceTo(getPosition(0)) > 4) {
                    controllingEntity = null;
                } else {
                    float yRotTarget = controller.getYRot();
                    float xRotTarget = controller.getXRot();

                    float yRotCurrent = getYRot();
                    float xRotCurrent = getXRot();

                    float yRotDiff = yRotTarget - yRotCurrent;
                    if (Math.abs(yRotDiff - 360) < Math.abs(yRotDiff))
                        yRotDiff -= 360;
                    if (Math.abs(yRotDiff + 360) < Math.abs(yRotDiff))
                        yRotDiff += 360;

                    float toRotateY = Math.clamp(yRotDiff, -1f, 1f);

                    float xRotDiff = xRotTarget - xRotCurrent;
                    float toRotateX = Math.clamp(xRotDiff, -5f, 5f);

                    setRot(yRotCurrent + toRotateY, xRotCurrent + toRotateX);
                }
            }
            setBoltPosition();

        }
        if (level().isClientSide) {

            client_lastYRot = client_currentYRot;
            if (getYRot() < client_currentYRot - 180) {
                client_currentYRot -= 360;
                client_lastYRot -= 360;
            }
            if (getYRot() > client_currentYRot + 180) {
                client_currentYRot += 360;
                client_lastYRot += 360;
            }
            float yRotDiff = (float) (getYRot() - client_currentYRot);
            client_currentYRot += (yRotDiff) * 0.3;


            client_lastxRot = client_currentxRot;
            if (getXRot() < client_currentxRot - 180) {
                client_currentxRot -= 360;
                client_lastxRot -= 360;
            }
            if (getXRot() > client_currentxRot + 180) {
                client_currentxRot += 360;
                client_lastxRot += 360;
            }
            float xRotDiff = (float) (getXRot() - client_currentxRot);
            client_currentxRot += (float) ((xRotDiff) * 0.3);


            if (getDrawProgress() <= 0)
                clien_ticksAfterShoot++;
            else
                clien_ticksAfterShoot = 0;

            client_drawProgressPrev = client_drawProgress; // Store previous value
            if (getDrawProgress() <= 0) {
                client_drawProgress -= Math.min(0.5f, client_drawProgress);
            } else {
                client_drawProgress += (getDrawProgress() - client_drawProgress) * 0.1f; // Smoothly lerp
            }
        }


    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            if (!player.isShiftKeyDown()) {
                if (getDrawProgress() == 1) {
                    if (bolt != null) {
                        bolt.setDeltaMovement(getLookAngle().scale(4));
                        bolt.setNoGravity(false);
                        bolt = null;
                        setDrawProgress(-1);
                        //shouldReload = false;
                    } else {
                        if (player.getItemInHand(hand).getItem().equals(Registry.ITEM_BALLISTA_BOLT.get())) {
                            player.getItemInHand(hand).shrink(1);
                            BallistaBolt newBolt = new BallistaBolt(Registry.ENTITY_BALLISTA_BOLT.get(), level());
                            newBolt.setPos(position());
                            level().addFreshEntity(newBolt);
                        }
                    }
                } else {
                    if(player.getItemInHand(hand).isEmpty())
                        reloadTicksRemaining = 5;
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
        Vec3 lookNoY = new Vec3(look.x, 0, look.z);

        Vec3 targetPos = getPosition(0).subtract(lookNoY.normalize().scale(2)).add(new Vec3(0, 0.5, 0));
        return targetPos;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DRAW_PROGRESS, 0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        setDrawProgress(compoundTag.getFloat("drawProgress"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putFloat("drawProgress", getDrawProgress());
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean isPickable() {
        return !this.isRemoved();
    }
}
