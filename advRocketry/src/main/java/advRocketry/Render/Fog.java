package advRocketry.Render;

import advRocketry.Dimension.Dimension;
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
        Dimension dimension = DimensionManager.get(dimensionId);

        Vector3f fogColor  = dimension.getFogColor();
        double brightnessMultiplier = dimension.getAccumulatedTerrainBrightness((float)event.getPartialTick(), null)+0.1;

        event.setRed((float) (fogColor.x*brightnessMultiplier));
        event.setGreen((float) (fogColor.y*brightnessMultiplier));
        event.setBlue((float) (fogColor.z*brightnessMultiplier));
    }
}
