package advRocketry.Satellites;

import net.minecraft.world.item.ItemStack;

public interface SatelliteBattery{
    double getEnergyStored(ItemStack stack);

    double getCapacity(ItemStack stack);

    // returns the energy received
    double receiveEnergy(ItemStack stack, double amount);

    // returns the energy extracted
    double extractEnergy(ItemStack stack, double amount);
}
