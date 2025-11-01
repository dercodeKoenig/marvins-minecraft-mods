package advRocketry.Rocket.RocketUtils;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import advRocketry.Dimension.SpaceTravelManager;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

// just a helper program, is not actually a real full program
public class ProgramNavigateToSpaceTravel {

    public static int orbitHeight = 2000;

    public static boolean run(EntityRocket rocket) {

        if (rocket.level().dimension().location().equals(SpaceTravelManager.dimId)) {
            return true;
        }

        rocket.enableMainEngines(true, false);
        rocket.enableSecondaryEngines(false, false);

        Dimension myDim = DimensionManager.get(rocket.level().dimension().location());
        if(myDim != null && myDim.getType() == DimensionProperties.PlanetType.SPACE_STATION){
            // logic for space station
            // undock from station and move to launchpos.y-50, then thrust away
        }else {
            // normal logic; just fly high up!
            rocket.enableMainEngines(true, false);
            rocket.enableSecondaryEngines(false, false);
            rocket.setTargetPosition(new Vec3(rocket.position().x, orbitHeight, rocket.position().z), false);

            orbitHeight = 400;
            if(rocket.position().y > orbitHeight && rocket.level() instanceof ServerLevel serverLevel){
                // teleport to space travel dimension
                ServerLevel target = DimensionManager.getServerLevel(serverLevel.getServer(), SpaceTravelManager.dimId);
                ChunkPos targetPos = SpaceTravelManager.getNextFreeChunkPos();
                BlockPos targetBlockPos = targetPos.getMiddleBlockPosition(100);
                rocket.setDeltaMovement(0,0,0);
                rocket.setTargetPosition(null,false);
                rocket.teleportTo(target, targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ(), new HashSet<>(), rocket.getYRot(), rocket.getXRot());
                SpaceTravelManager.keepChunkLoaded(targetPos);
            }
        }

        return false;
    }
}
