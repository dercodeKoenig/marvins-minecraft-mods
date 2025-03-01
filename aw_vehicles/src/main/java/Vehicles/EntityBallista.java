package Vehicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityBallista extends Entity {

    double drawProgress;

    public EntityBallista(EntityType<EntityBallista> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick(){
        super.tick();
        drawProgress+=0.01;
        if(drawProgress > 1)drawProgress = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }
}
