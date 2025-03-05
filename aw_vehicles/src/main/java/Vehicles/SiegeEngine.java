package Vehicles;

import AgeOfSteam.Items.Hammer.ItemHammer;
import Vehicles.Ballista.Ballista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public abstract class SiegeEngine extends Entity  implements NoGhostBlockCollider  {

    public static final EntityDataAccessor<Integer> CONSTRUCTION_PROGRESS = SynchedEntityData.defineId(SiegeEngine.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_BROKEN = SynchedEntityData.defineId(SiegeEngine.class, EntityDataSerializers.BOOLEAN);

    public SiegeEngine(EntityType<?> entityType, Level level) {
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
        if(!level().isClientSide){
            // set ghost block for pathfinding
            if (level().getBlockState(blockPosition()).getBlock() != Registry.GHOST_BLOCK.get()) {
                level().setBlock(blockPosition(), Registry.GHOST_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CONSTRUCTION_PROGRESS, 0);
        builder.define(IS_BROKEN, false);
    }

    @Override
    protected double getDefaultGravity() {
        return (double) 0.1F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("construction")) {
            getEntityData().set(CONSTRUCTION_PROGRESS, compoundTag.getInt("construction"));
        }
        if (compoundTag.contains("isBroken")) {
            getEntityData().set(IS_BROKEN, compoundTag.getBoolean("isBroken"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putInt("construction", getEntityData().get(CONSTRUCTION_PROGRESS));
        compoundTag.putBoolean("isBroken", getEntityData().get(IS_BROKEN));
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean isPickable() {
        return !this.isRemoved();
    }
}
