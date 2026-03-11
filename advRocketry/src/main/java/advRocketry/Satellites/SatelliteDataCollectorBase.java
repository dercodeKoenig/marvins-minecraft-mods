package advRocketry.Satellites;

import advRocketry.Data.DataStack;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemDataStorage;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public abstract class SatelliteDataCollectorBase extends Satellite {

    abstract double energyPerData();

    abstract String dataBaseTypeToGenerate();

    abstract int getMinDataGenTicks();

    long lastTimeDataCollected = 0;

    @Override
    public Pair<Boolean, String> validateBuild() {
        iterateEquipment();

        if (getEnergyCapacity() < energyPerData())
            return Pair.of(false, "need more batteries");

        if (getEnergyStored() < 20000 && energyProducers.isEmpty()) {
            return Pair.of(false, "not enough energy");
        }

        if (getDataCapacity() < 100) {
            return Pair.of(false, "not enough data storage");
        }

        return Pair.of(true, "");
    }


    // returns the total data capacity of the satellite
    public int getDataCapacity() {
        int total = 0;
        for (ItemStack stack : this.equipment) {
            if (stack.getItem() instanceof ItemDataStorage itemDataStorage) {
                total += itemDataStorage.getDataCapacity(stack);
            }
        }
        return total;
    }
    // returns the total data stored in the satellite
    public int getDataStored() {
        int total = 0;
        for (ItemStack stack : this.equipment) {
            if (stack.getItem() instanceof ItemDataStorage itemDataStorage) {
                DataStack dataStack = itemDataStorage.getDataStack(stack);
                if(dataStack != null)
                    total += dataStack.amount;
            }
        }
        return total;
    }

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
        if (getEnergyStored() > energyPerData() && lastTimeDataCollected + getMinDataGenTicks() < GlobalTime.getGlobalTime()) {
            // join base type + where it was collected
            String type = DataStack.join(dataBaseTypeToGenerate(), parentDimensionId);
            if (insertOneDataUnit(type, true) == 1) {
                extractEnergy(energyPerData());
                insertOneDataUnit(type, false);
                lastTimeDataCollected = GlobalTime.getGlobalTime();
            }
        }
    }
}
