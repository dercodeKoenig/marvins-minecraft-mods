package Vehicles;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BallistaBolt extends AbstractArrow {
    public boolean shouldTick = false;
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
        if(shouldTick)
            super.tick();
    }
}
