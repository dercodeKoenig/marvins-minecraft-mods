package advRocketry.Items;

import advRocketry.Config;
import advRocketry.utils.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class ItemGalaxyDatabase extends Item {

    public static int POINTS_UNLOCKED(){
        return Config.INSTANCE.data_Unlocked_Points;
    }

    public ItemGalaxyDatabase() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal("known planets: " + getKnownDimensions(stack).size())
        );
    }

    public static Set<String> getKnownDimensions(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        return tag.getAllKeys();
    }

    @Nullable
    public static PlanetInfo getPlanetInfo(ItemStack stack, ResourceLocation dimensionId) {
        return getPlanetInfo(stack, dimensionId.toString());
    }

    @Nullable
    public static PlanetInfo getPlanetInfo(ItemStack stack, String dimensionId) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains(dimensionId)) {
            return PlanetInfo.deserialize(tag.getCompound(dimensionId));
        }
        return null;
    }

    public static void setPlanetInfo(ItemStack stack, ResourceLocation dimensionId, PlanetInfo info) {
        setPlanetInfo(stack, dimensionId.toString(), info);
    }
    public static void setPlanetInfo(ItemStack stack, String dimensionId, PlanetInfo info) {
        CompoundTag infoTag = info.serialize();
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.put(dimensionId, infoTag);
        ItemUtils.setTag(stack, tag);
    }

    public static void discoverPlanet(ItemStack stack, String dimensionId){
        setPlanetInfo(stack, dimensionId, new PlanetInfo());
    }

    public static boolean isDimensionKnown(ItemStack stack, String dimensionId) {
        return getKnownDimensions(stack).contains(dimensionId);
    }

    public static boolean isDistanceUnlocked(ItemStack stack, String dimensionId) {
        PlanetInfo info = getPlanetInfo(stack, dimensionId);
        if(info == null) return false;
        return info.distance >= POINTS_UNLOCKED();
    }

    public static class PlanetInfo {
        public int distance = 0;
        public int mass = 0;
        public int composition = 0;

        public static PlanetInfo deserialize(CompoundTag tag){
            PlanetInfo info = new PlanetInfo();
            info.distance = tag.getInt("distance");
            info.mass = tag.getInt("mass");
            info.composition = tag.getInt("composition");
            return info;
        }

        public CompoundTag serialize(){
            CompoundTag tag = new CompoundTag();
            tag.putInt("distance", distance);
            tag.putInt("mass", mass);
            tag.putInt("composition", composition);
            return tag;
        }
    }
}
