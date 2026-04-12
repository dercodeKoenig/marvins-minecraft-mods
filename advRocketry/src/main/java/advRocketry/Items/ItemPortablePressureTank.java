package advRocketry.Items;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.PlanetDimensionProperties;
import advRocketry.Registry.GasRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;
import java.util.Set;

public class ItemPortablePressureTank extends Item {

    public int capacity;

    public ItemPortablePressureTank(int capacity) {
        super(new Properties().stacksTo(16));
        this.capacity = capacity;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        IFluidHandlerItem fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler != null) {
            tooltipComponents.add(Component.literal(
                    fluidHandler.getFluidInTank(0).getFluid().toString()+
                            ":" +
                            fluidHandler.getFluidInTank(0).getAmount()
            ));
        }
    }
}
