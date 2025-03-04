package Vehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BallistaBolt extends Entity {
    public boolean shotEnd = false;
    public Set<Entity> hitEntities = new HashSet<>();
    public double x, y, z, dx, dy, dz;
    public Entity owner;
    double client_lastYRot = 0;
    double client_currentYRot = 0;
    double client_lastxRot = 0;
    double client_currentxRot = 0;

    int ticksInGround = 0;

    public BallistaBolt(EntityType<BallistaBolt> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        List<Ballista> ballistas = level().getEntitiesOfClass(Ballista.class, getBoundingBox());
        if (!ballistas.isEmpty()) {
            ballistas.get(0).checkExistingBolt();
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity.equals(owner)) return false;
        return super.canCollideWith(entity);
    }

    @Override
    public void tick() {

        dx = getX() - x;
        dy = getY() - y;
        dz = getZ() - z;
        x = getX();
        y = getY();
        z = getZ();

        boolean inGround = false;
        // prevent it from beeing inside the block and having packedlight of 0
        Vec3 posOffset = position().subtract(getLookAngle().normalize().scale(0.05));
        int i = Mth.floor(posOffset.x);
        int j = Mth.floor(posOffset.y);
        int k = Mth.floor(posOffset.z);
        BlockPos blockpos = new BlockPos(i, j, k);
        BlockState blockstate = this.level().getBlockState(blockpos);
        if (!blockstate.isAir()) {
            VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
            if (!voxelshape.isEmpty()) {
                for (AABB aabb : voxelshape.toAabbs()) {
                    if (aabb.move(blockpos).contains(posOffset)) {
                        inGround = true;
                        shotEnd = true;
                        setDeltaMovement(Vec3.ZERO);
                        break;
                    }
                }
            }
        }

        if (!inGround) {
            Vec3 vec32 = this.position();
            Vec3 vec33 = vec32.add(getDeltaMovement());
            HitResult hitresult = this.level().clip(new ClipContext(vec32, vec33, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (hitresult.getType() != HitResult.Type.MISS) {
                //System.out.println("hit");
                if (hitresult instanceof BlockHitResult b) {
                    setPos(hitresult.getLocation().add(getDeltaMovement().normalize().scale(-0.01)));
                    setDeltaMovement(Vec3.ZERO);
                    inGround = true;
                    if (!shotEnd) {
                        // whatever here
                        level().playSound(null,blockPosition(), Registry.SOUND_BALLISTA_GROUND_HIT.get(), SoundSource.BLOCKS,1,1);
                    }
                }
            }

            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(this.level(), this, vec32, vec33, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate((double) 10.0F), (x) -> true);
            if (entityhitresult != null) {
                Entity entity = entityhitresult.getEntity();
                //System.out.println(entity.getName().getString());
                if (entity != owner && owner != null) {
                    if (!shotEnd && !hitEntities.contains(entity)) {
                        hitEntities.add(entity);
                        entity.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW), null, owner, owner.position()), 50);
                        level().playSound(null,blockPosition(), Registry.SOUND_BALLISTA_ENTITY_HIT.get(), SoundSource.BLOCKS,1,1);
                        //System.out.println("hit entity");
                    }
                }
            }
        }

        if (!inGround) {
            if (getDeltaMovement().lengthSqr() > 0)
                this.setXRot((float) (Mth.atan2(getDeltaMovement().y, getDeltaMovement().horizontalDistance()) * (double) 180.0F / (double) (float) Math.PI));
            // gravity
            if(!isNoGravity())
                this.setDeltaMovement(this.getDeltaMovement().add((double)0.0F, -0.08, (double)0.0F));

            setPos(position().add(getDeltaMovement()));
        }

        if (inGround) {
            ticksInGround++;
            if (ticksInGround > 20 * 5 * 60) {
                discard();
            }
        } else
            ticksInGround = 0;

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
            client_currentxRot += (xRotDiff) * 0.3;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("shotEnd")) {
            shotEnd = compoundTag.getBoolean("shotEnd");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putBoolean("shotEnd", shotEnd);
    }
}
