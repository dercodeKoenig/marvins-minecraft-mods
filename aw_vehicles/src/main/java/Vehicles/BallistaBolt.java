package Vehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

public class BallistaBolt extends AbstractArrow {
    public boolean shouldTick = false;
    double x, y, z, lx, ly, lz;

    protected BallistaBolt(EntityType<? extends Projectile> entityType, Level level) {
        super(Registry.ENTITY_BALLISTA_BOLT.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity.equals(getOwner())) return false;
        return super.canCollideWith(entity);
    }

    @Override
    public void tick() {
        lx = getX() - x;
        ly = getY() - y;
        lz = getZ() - z;
        x = getX();
        y = getY();
        z = getZ();

        inGround = false;
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
                        this.inGround = true;
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
                System.out.println("hit");
                if (hitresult instanceof BlockHitResult b) {
                    setPos(hitresult.getLocation().add(getDeltaMovement().normalize().scale(-0.01)));
                    setDeltaMovement(Vec3.ZERO);
                    inGround = true;
                }
            }

            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(this.level(), this, vec32, vec33, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate((double) 10.0F), (x) -> true);
            if (entityhitresult != null) {
                Entity entity = entityhitresult.getEntity();
                System.out.println(entity.getName().getString());
                if (entity != getOwner()) {
                    System.out.println("hit entity");
                }
            }
        }

        if (!inGround) {
            if (getDeltaMovement().lengthSqr() > 0)
                this.setXRot((float) (Mth.atan2(getDeltaMovement().y, getDeltaMovement().horizontalDistance()) * (double) 180.0F / (double) (float) Math.PI));
            applyGravity();
            setPos(position().add(getDeltaMovement()));
        }
    }
}
