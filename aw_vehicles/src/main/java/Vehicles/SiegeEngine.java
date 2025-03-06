package Vehicles;

import AgeOfSteam.Items.Hammer.ItemHammer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.ArrayList;

// it is a livingentity so that other mobs can attack it

public abstract class SiegeEngine extends LivingEntity implements NoGhostBlockCollider  {

    public static final EntityDataAccessor<Integer> CONSTRUCTION_PROGRESS = SynchedEntityData.defineId(SiegeEngine.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_BROKEN = SynchedEntityData.defineId(SiegeEngine.class, EntityDataSerializers.BOOLEAN);

    public SiegeEngine(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }


    public boolean isHammerItem(ItemStack item) {
        if (ModList.get().isLoaded("age_of_steam")) {
            return item.getItem() instanceof ItemHammer;
        } else {
            return item.getItem().equals(Registry.ITEM_WOODEN_HAMMER.get());
        }
    }

    public void tick(){
        super.tick();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONSTRUCTION_PROGRESS, 0);
        builder.define(IS_BROKEN, false);
        super.defineSynchedData(builder);
    }

    @Override
    protected double getDefaultGravity() {
        return (double) 0.1F;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("construction")) {
            getEntityData().set(CONSTRUCTION_PROGRESS, compoundTag.getInt("construction"));
        }
        if (compoundTag.contains("isBroken")) {
            getEntityData().set(IS_BROKEN, compoundTag.getBoolean("isBroken"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("construction", getEntityData().get(CONSTRUCTION_PROGRESS));
        compoundTag.putBoolean("isBroken", getEntityData().get(IS_BROKEN));
    }

    @Override
    public float getHealth() {
        return getEntityData().get(IS_BROKEN) ? 0 : 20;
    }

    @Override
    public boolean isDeadOrDying() {
        // because it would despawn 
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return new ArrayList<>();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot equipmentSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {}
}
