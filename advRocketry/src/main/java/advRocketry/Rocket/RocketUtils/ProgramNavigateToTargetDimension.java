package advRocketry.Rocket.RocketUtils;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import advRocketry.Dimension.SpaceTravelManager;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

// just a helper program, is not actually a real full program
public class ProgramNavigateToTargetDimension {

    public static int orbitHeight = 2000;

    public static boolean run(EntityRocket rocket, ResourceLocation target) {

        if (rocket.level().dimension().location().equals(target)) {
            return true;
        }

        // all movement is virtual
        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);
        rocket.setTargetPosition(null, false);
        rocket.setDeltaMovement(0,0,0);

        // move to target dimension
        Vec3 targetPosition = new Vec3(0,0,0); // in case of null
        Dimension targetDim = DimensionManager.get(target);
        if(targetDim != null)
            targetPosition = targetDim.getPosition(0);

        Vec3 direction = targetPosition.subtract(rocket.universePosition);
        rocket.setDefaultTargetHeading(direction, false);

        // ifrocket.hasSatellites && shouldDeployThem -> deploy satellites shortly before dimension jump

        // jump dimension


        return false;
    }
}
