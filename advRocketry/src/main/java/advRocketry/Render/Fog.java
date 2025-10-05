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
        double brightnessMultiplier = dimension.getAccumulatedWorldBrightness((float)event.getPartialTick(),0.2f, null);

        // just some adjustments because it looks better. make it change dark to bright faster and stay bright for longer
        brightnessMultiplier = Math.clamp(Math.pow(brightnessMultiplier, 0.8)*2, 0,1);

        fogColor = fogColor.mul((float) brightnessMultiplier);

        // (i do not want the bright gamma corection for my fog)
        fogColor.x = (float) Math.pow(fogColor.x / (1+fogColor.x), 1f/2.2f);
        fogColor.y = (float) Math.pow(fogColor.y / (1+fogColor.y), 1f/2.2f);
        fogColor.z = (float) Math.pow(fogColor.z / (1+fogColor.z), 1f/2.2f);

        event.setRed((float) (fogColor.x));
        event.setGreen((float) (fogColor.y));
        event.setBlue((float) (fogColor.z));
    }
}
