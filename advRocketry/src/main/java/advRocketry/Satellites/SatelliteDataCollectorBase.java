package advRocketry.Satellites;

import advRocketry.Data.DataStack;
import advRocketry.Items.ItemDataStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class SatelliteDataCollectorBase extends Satellite {

    double energyPerData = 100;
    String dataTypeToGenerate = "";

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

    public int insertOneDataUnit(String type, boolean simulate) {
        for (ItemStack stack : this.equipment) {
            if (stack.getItem() instanceof ItemDataStorage itemDataStorage) {
                int inserted = itemDataStorage.insertData(stack, new DataStack(type, 1), simulate);
                if (inserted == 1)
                    return 1;
            }
        }
        return 0;
    }

    public void tick() {
        super.tick();
        if (getEnergyStored() > energyPerData && insertOneDataUnit(dataTypeToGenerate, true) == 1) {
            extractEnergy(energyPerData);
            insertOneDataUnit(dataTypeToGenerate, false);
        }
    }
}
