package advRocketry.Satellites;

import advRocketry.Data.DataStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class SatelliteDataCollectorBase extends Satellite {

    DataStack dataBuffer;

    @Nullable
    public DataStack extractData(int amount, boolean simulate) {
        if(dataBuffer == null)
            return null;
        int toExtract = Math.min(amount, dataBuffer.amount);
        DataStack extracted = new DataStack(dataBuffer.type, toExtract);
        if(!simulate){
            dataBuffer.amount -= toExtract;
            if(dataBuffer.isEmpty())
                dataBuffer = null;
        }
        return extracted;
    }

    public float getDataCollectedPerTick() {
        return 0;
    }


    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        if (dataBuffer != null)
            tag.put("dataBuffer", dataBuffer.saveToNbt());
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
        if (tag.contains("dataBuffer"))
            dataBuffer = DataStack.createFromNbt(tag.getCompound("dataBuffer"));
    }
}
