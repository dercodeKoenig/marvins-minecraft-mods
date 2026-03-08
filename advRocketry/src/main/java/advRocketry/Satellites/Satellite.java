package advRocketry.Satellites;

import advRocketry.Config;
import advRocketry.Dimension.DimensionManager;
import advRocketry.GlobalTime;
import advRocketry.Registry.Items;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.text.CompactNumberFormat;
import java.util.ArrayList;
import java.util.UUID;

public class Satellite {
    // data to save
    public ItemStackHandler inventory;
    public ResourceLocation parentDimensionId;
    public UUID uuid;

    // runtime generated, "cache" data
    ArrayList<ItemStack> equipment = new ArrayList<>();
    ArrayList<ItemStack> batteries = new ArrayList<>();
    ArrayList<ItemStack> energyProducers = new ArrayList<>();
    boolean hasRadiationShield = false;
    boolean hasLoraModule = false;

    // it is required to have a constructor with no args
    public Satellite() {
        inventory = new ItemStackHandler(7) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == 0)
                    return stack.getItem() instanceof SatellitePrimaryFunction;
                if (slot == 1 || slot == 2 || slot == 3)
                    return stack.getItem() instanceof SatelliteEnergyProducer || stack.getItem() instanceof SatelliteBattery;
                if (slot == 4 || slot == 5 || slot == 6)
                    return stack.getItem() instanceof SatelliteEquipment || stack.getItem() instanceof SatelliteBattery;
                return false;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    public Pair<Boolean, String> validateBuild() {
        return Pair.of(false, "base class is no valid satellite");
    }

    public String getName() {
        return "Satellite";
    }

    // build the list of equipment and energy storages before starting to tick
    public void iterateEquipment() {
        energyProducers.clear();
        batteries.clear();
        equipment.clear();
        hasRadiationShield = false;
        hasLoraModule = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof SatelliteEnergyProducer) {
                energyProducers.add(stack);
            }
            if (stack.getItem() instanceof SatelliteBattery) {
                batteries.add(stack);
            }
            if (stack.getItem() instanceof SatelliteEquipment) {
                equipment.add(stack);
            }
            if (stack.getItem().equals(Items.ITEM_LORA_MODULE.get()))
                hasLoraModule = true;
            if (stack.getItem().equals(Items.ITEM_RADIATION_SHIELD.get()))
                hasRadiationShield = true;
        }
    }

    public void onDeploymentStart(ResourceLocation parentDimensionId) {
        if (!validateBuild().getFirst()) {
            throw new RuntimeException("a satellite build was invalid");
        }
        this.parentDimensionId = parentDimensionId;
        iterateEquipment();
    }

    /// returns energy extracted, will extract from all batteries until amount is satisfied
    public double extractEnergy(double amount) {
        double extracted = 0;
        for (ItemStack stack : batteries) {
            double remaining = amount - extracted;
            extracted += ((SatelliteBattery) stack.getItem()).extractEnergy(stack, remaining);
        }
        return extracted;
    }

    /// returns energy stored in all batteries combined
    public double getEnergyStored() {
        double total = 0;
        for (ItemStack stack : batteries) {
            total += ((SatelliteBattery) stack.getItem()).getEnergyStored(stack);
        }
        return total;
    }

    /// returns total energy capacity
    public double getEnergyCapacity() {
        double total = 0;
        for (ItemStack stack : batteries) {
            total += ((SatelliteBattery) stack.getItem()).getCapacity(stack);
        }
        return total;
    }

    public boolean hasLoraModule() {
        return hasLoraModule;
    }

    public boolean hasRadiationShield() {
        return hasRadiationShield;
    }

    public void tick() {
        this.generateEnergyAndFillBatteries();
        if (!hasRadiationShield() && GlobalTime.getGlobalTime() % 20 == 0) {
            double p = Math.random();
            if (p < Config.INSTANCE.satellite_damage_prob_per_second) {
                // satellite takes radiation damage and dies
                SatelliteManager.removeSatellite(uuid);
                for (Player player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(Component.literal("A satellite (" + getName() + ") was lost to radiation damage in orbit around " + parentDimensionId + "."));
                    player.sendSystemMessage(Component.literal("A radiation shield would have helped"));
                }
            }
        }
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", inventory.serializeNBT(registries));
        if (parentDimensionId != null)
            tag.putString("parentDimensionId", parentDimensionId.toString());
        if (uuid != null)
            tag.putUUID("uuid", uuid);
        return tag;
    }

    public void deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("parentDimensionId"))
            parentDimensionId = ResourceLocation.parse(tag.getString("parentDimensionId"));
        if (tag.contains("uuid"))
            uuid = tag.getUUID("uuid");
    }

    // generated energy from the energy producers and puts it in the batteries if space is available
    private void generateEnergyAndFillBatteries() {
        double energyProduced = 0;
        // generate energy
        for (ItemStack stack : energyProducers) {
            energyProduced += ((SatelliteEnergyProducer) stack.getItem()).produceEnergy(this);
        }
        // move generated energy into batteries
        for (ItemStack stack : batteries) {
            double received = ((SatelliteBattery) stack.getItem()).receiveEnergy(stack, energyProduced);
            energyProduced -= received;
        }
    }
}
