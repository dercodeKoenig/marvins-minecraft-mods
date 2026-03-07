package advRocketry.Render;

import advRocketry.Dimension.Dimension;
import advRocketry.Utils.ClientUtils;
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
        Dimension myDimension = ClientUtils.getPlayerDimension();
        if (myDimension != null) {
            Vector3f fogColor = myDimension.computeTerrainFogColor((float) event.getPartialTick());
            event.setRed((float) (fogColor.x));
            event.setGreen((float) (fogColor.y));
            event.setBlue((float) (fogColor.z));
        }
    }
}
