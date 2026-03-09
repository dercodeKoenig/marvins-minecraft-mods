package advRocketry.Items;

import advRocketry.Config;
import advRocketry.Data.DataTypes;
import advRocketry.Data.IDataStorage;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ItemGalaxyDatabase extends Item {

    public ItemGalaxyDatabase() {
        super(new Properties().stacksTo(1));
    }

    public static int POINTS_UNLOCKED(PlanetDimension planet) {
        if (planet == null)
            return 1;
        return planet.getDataRequiredForUnlock();
    }

    public static Set<String> getKnownDimensions(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        return tag.getAllKeys();
    }

    @Nullable
    public static PlanetInfo getPlanetInfo(ItemStack stack, String dimensionId) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains(dimensionId)) {
            return PlanetInfo.deserialize(tag.getCompound(dimensionId));
        }
        return null;
    }

    @Nullable
    public static PlanetInfo getPlanetInfo(ItemStack stack, ResourceLocation dimensionId) {
        return getPlanetInfo(stack, dimensionId.toString());
    }

    @Nullable
    public static PlanetInfo getPlanetInfo(ItemStack stack, PlanetDimension planet) {
        return getPlanetInfo(stack, planet.getDimensionId().toString());
    }

    public static void setPlanetInfo(ItemStack stack, String dimensionId, PlanetInfo info) {
        CompoundTag infoTag = info.serialize();
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.put(dimensionId, infoTag);
        ItemUtils.setTag(stack, tag);
    }

    public static void setPlanetInfo(ItemStack stack, ResourceLocation dimensionId, PlanetInfo info) {
        setPlanetInfo(stack, dimensionId.toString(), info);
    }

    public static void setPlanetInfo(ItemStack stack, PlanetDimension planet, PlanetInfo info) {
        setPlanetInfo(stack, planet.getDimensionId(), info);
    }

    public static void discoverPlanet(ItemStack stack, String dimensionId) {
        setPlanetInfo(stack, dimensionId, new PlanetInfo());
    }

    public static void discoverPlanet(ItemStack stack, PlanetDimension planet) {
        discoverPlanet(stack, planet.getDimensionId().toString());
    }

    public static boolean isDimensionKnown(ItemStack stack, String dimensionId) {
        return getKnownDimensions(stack).contains(dimensionId);
    }

    public static boolean isDimensionKnown(ItemStack stack, ResourceLocation dimensionId) {
        return isDimensionKnown(stack, dimensionId.toString());
    }

    public static boolean isDimensionKnown(ItemStack stack, PlanetDimension planet) {
        return isDimensionKnown(stack, planet.getDimensionId());
    }

    public static boolean isDistanceUnlocked(ItemStack stack, PlanetDimension planet) {
        PlanetInfo info = getPlanetInfo(stack, planet.getDimensionId());
        if (info == null) return false;
        return info.get(DataTypes.distance) >= POINTS_UNLOCKED(planet);
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal(
                        "known planets: " + getKnownDimensions(stack).size()
                ).withStyle(ChatFormatting.GRAY)
        );
    }

    public static class PlanetInfo {
        public HashMap<String, Integer> data = new HashMap<>();

        public PlanetInfo() {
            data.put(DataTypes.distance, 0);
            data.put(DataTypes.mass, 0);
            data.put(DataTypes.composition, 0);
        }

        public static PlanetInfo deserialize(CompoundTag tag) {
            PlanetInfo info = new PlanetInfo();
            for (String key : tag.getAllKeys()) {
                info.data.put(key, tag.getInt(key));
            }
            return info;
        }

        public int get(String key) {
            return data.get(key);
        }

        public void put(String key, int value) {
            data.put(key, value);
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            for (String key : data.keySet()) {
                tag.putInt(key, data.get(key));
            }
            return tag;
        }
    }
}
