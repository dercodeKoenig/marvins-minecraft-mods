package advRocketry.Render;

import advRocketry.Dimension.Dimension;
import advRocketry.Utils.ClientUtils;
import advRocketry.Utils.RenderUtils;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

public class Fog {
    // can modify fog distance
    public static void renderFogEvent(ViewportEvent.RenderFog event) {
//        ResourceLocation dimension = Minecraft.getInstance().level.dimension().location();
//        event.setNearPlaneDistance(event.getNearPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
//        event.setFarPlaneDistance(event.getFarPlaneDistance() * (float) (1d / (DimensionManager.INSTANCE.dimensions.get(dimension).atmosphereDensity + 0.0001)));
//        event.setCanceled(true);
    }


    // can modify fog color, apparently used for terrain shading
    public static void computeFogColorEvent(ViewportEvent.ComputeFogColor event) {
        Dimension myDimension = ClientUtils.getPlayerDimension();
        if (myDimension != null) {
            Vector3f fogColor = myDimension.computeTerrainFogColor((float) event.getPartialTick());
            // fog color comes in linear hdr format
            // apply same tone mapping and gamma correction as in shader so it matches the color
            fogColor = RenderUtils.reinhard(fogColor);
            fogColor = RenderUtils.gamma_correcct(fogColor);
            event.setRed(fogColor.x);
            event.setGreen(fogColor.y);
            event.setBlue(fogColor.z);
        }
    }
}
