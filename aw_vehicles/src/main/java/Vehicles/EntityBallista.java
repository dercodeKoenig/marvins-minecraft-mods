package Vehicles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityBallista extends Entity {

    private static final EntityDataAccessor<Float> DRAW_PROGRESS = SynchedEntityData.defineId(EntityBallista.class, EntityDataSerializers.FLOAT);


    public EntityBallista(EntityType<EntityBallista> entityType, Level level) {
        super(entityType, level);
    }


    public float getDrawProcess(){
        return getEntityData().get(DRAW_PROGRESS);
    }
    public void setDrawProcess(float process){
        getEntityData().set(DRAW_PROGRESS, process);
    }
    float client_drawProcess;
    float client_drawProcessPrev;
    int ticksAfterShoot = 0;
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setDrawProcess(getDrawProcess() + 0.01f);
            if (getDrawProcess() > 1.05) {
                setDrawProcess(-1);
            }
        }
        if (level().isClientSide) {

            if (getDrawProcess() <= 0)
                ticksAfterShoot++;
            else
                ticksAfterShoot = 0;

            client_drawProcessPrev = client_drawProcess; // Store previous value
            if(getDrawProcess()<=0){
                client_drawProcess -= Math.min(0.5f, client_drawProcess);
            }else {
                client_drawProcess += (getDrawProcess() - client_drawProcess) * 0.1f; // Smoothly lerp
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
builder.define(DRAW_PROGRESS,0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }
}
