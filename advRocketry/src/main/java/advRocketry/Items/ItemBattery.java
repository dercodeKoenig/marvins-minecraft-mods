package advRocketry.Items;

import advRocketry.Satellites.SatelliteBattery;
import advRocketry.utils.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

public class ItemBattery extends Item implements SatelliteBattery {

    public static double capacity = 100000;

    String key = "energy";

    public ItemBattery() {
        super(new Properties());
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal(
                        "energy: " + getEnergyStored(stack) + " / " + capacity
                )
        );
    }

    public void setEnergyStored(ItemStack stack, double energy) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(key, energy);
        ItemUtils.setTag(stack, tag);
    }

    @Override
    public double getEnergyStored(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains(key))
            return tag.getDouble(key);
        return 0;
    }

    @Override
    public double getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public double receiveEnergy(ItemStack stack, double amount) {
        double existingEnergy = getEnergyStored(stack);
        double maxReceive = Math.min(getCapacity(stack) - existingEnergy, amount);
        double newEnergy = existingEnergy + maxReceive;
        setEnergyStored(stack, newEnergy);
        return maxReceive;
    }

    @Override
    public double extractEnergy(ItemStack stack, double amount) {
        double existingEnergy = getEnergyStored(stack);
        double maxExtract = Math.min(existingEnergy, amount);
        double newEnergy = existingEnergy - maxExtract;
        setEnergyStored(stack, newEnergy);
        return maxExtract;
    }
}
