package advRocketry.Items;

import ARLib.utils.DimensionUtils;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Satellites.SatelliteEquipment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static advRocketry.Utils.ItemUtils.getStacktagOrEmpty;
import static advRocketry.Utils.ItemUtils.setTag;
import static net.minecraft.network.chat.Style.DEFAULT_FONT;
import static net.minecraft.network.chat.Style.EMPTY;

public class ItemLoraModule extends Item implements SatelliteEquipment {
    public ItemLoraModule() {
        super(new Properties());
    }
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal("enables long range data transfer").withStyle(ChatFormatting.GRAY)
        );
    }
}
