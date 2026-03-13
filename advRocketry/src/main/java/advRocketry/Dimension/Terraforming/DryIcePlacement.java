package advRocketry.Dimension.Terraforming;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.GasRegistry;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public class DryIcePlacement {

    // TODO: mixin to IceBlock.melt & Biome.shouldFreeze for water ice logic
    //      force always freeze if temp < 0, allow default on temp 0 < 50, never allow freeze at temp > 50

}
