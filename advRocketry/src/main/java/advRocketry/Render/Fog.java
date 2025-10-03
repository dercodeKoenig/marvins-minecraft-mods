package advRocketry.Render;

import advRocketry.Dimension.DimensionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

public class Fog {
    // can modify fog distance
    public static void renderFogEvent(ViewportEvent.RenderFog event) {
// TODO: i do not want to artificially limit render distance by fog. maybe just fade out fog on low atm? or blend it to black?

//        ResourceLocation dimension = Minecraft.getInstance().level.dimension().location();
//        event.setNearPlaneDistance(event.getNearPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
//        event.setFarPlaneDistance(event.getFarPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
//        event.setCanceled(true);
    }

    // can modify fog color, apparently used for terrain shading
    public static void computeFogColorEvent(ViewportEvent.ComputeFogColor event) {
        Level currentLevel = Minecraft.getInstance().level;
        ResourceLocation dimensionId = currentLevel.dimension().location();
        Vector3f color = DimensionManager.INSTANCE.dimensions.get(dimensionId).getBrightnessAdjustedFogColor();
        // TODO: if using hdr in atmosphere shader, also apply tonemapping to the fog here?
        event.setRed(color.x);
        event.setGreen(color.y);
        event.setBlue(color.z);
    }
}
