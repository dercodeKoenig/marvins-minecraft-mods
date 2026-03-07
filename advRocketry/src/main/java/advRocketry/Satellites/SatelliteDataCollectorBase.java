package advRocketry.Satellites;

import advRocketry.Data.DataStack;
import advRocketry.Items.ItemDataStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class SatelliteDataCollectorBase extends Satellite {

    @Nullable
    // extract 1 data unit from the first data storage that contains any data
    public DataStack extractOneDataUnit(boolean simulate) {
        for (ItemStack stack : this.equipment) {
            if (stack.getItem() instanceof ItemDataStorage itemDataStorage) {
                DataStack extracted = itemDataStorage.extractData(stack, 1, simulate);
                if (extracted != null)
                    return extracted;
            }
        }
        return null;
    }

    public float getDataCollectedPerTick() {
        return 0;
    }

    public void tick() {
        super.tick();


    }


    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = super.serialize(registries);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        super.deserialize(tag, registries);
    }
}
