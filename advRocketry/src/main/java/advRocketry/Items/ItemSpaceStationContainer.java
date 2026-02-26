package advRocketry.Items;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemSpaceStationContainer extends Item {
    public ItemSpaceStationContainer() {
        super(new Properties().stacksTo(1));
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        HashMap<BlockPos, BlockState> blocks = readBlocks(stack, context.registries());
        tooltipComponents.add(
                Component.literal("size: " + blocks.size())
        );
    }

    public static HashMap<BlockPos, BlockState> readBlocks(ItemStack stack, HolderLookup.Provider registries) {
        HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        CompoundTag compoundTag = ItemUtils.getStacktagOrEmpty(stack);
        if (compoundTag.contains("blocks")) {
            ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blockTags.size(); i++) {
                CompoundTag blockTag = blockTags.getCompound(i);
                BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
                BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
                blocks.put(p, state);
            }
        }
        return blocks;
    }

    public static void writeBlocks(ItemStack stack, Map<BlockPos, BlockState> blocks) {
        ListTag blockTags = new ListTag(blocks.size());
        for (BlockPos i : blocks.keySet()) {
            BlockState state = blocks.get(i);
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockTag.put("block", NbtUtils.writeBlockState(state));
            blockTags.add(blockTag);
        }
        CompoundTag itemTag = ItemUtils.getStacktagOrEmpty(stack);
        itemTag.put("blocks", blockTags);
        ItemUtils.setTag(stack, itemTag);
    }
}
