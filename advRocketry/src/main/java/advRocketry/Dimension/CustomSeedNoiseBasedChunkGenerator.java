package advRocketry.Dimension;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.mutable.MutableObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CustomSeedNoiseBasedChunkGenerator extends NoiseBasedChunkGenerator {
    RandomState customRandomState;
    boolean shouldMakeStructures;

    public CustomSeedNoiseBasedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, long customSeed, boolean createStructures) {
        super(biomeSource, settings);
        customRandomState = RandomState.create(ServerLifecycleHooks.getCurrentServer().registryAccess().asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, customSeed);
        shouldMakeStructures = createStructures;
    }


    public CompletableFuture<ChunkAccess> createBiomes(RandomState ignored, Blender blender, StructureManager structureManager, ChunkAccess chunk) {
        return super.createBiomes(customRandomState, blender, structureManager, chunk);
    }

    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState ignored) {
        return super.getBaseHeight(x, z, type, level, customRandomState);
    }

    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState ignored) {
        return super.getBaseColumn(x, z, height, customRandomState);
    }

    public void addDebugScreenInfo(List<String> info, RandomState ifnored, BlockPos pos) {
        super.addDebugScreenInfo(info, customRandomState, pos);
    }

    protected OptionalInt iterateNoiseColumn(LevelHeightAccessor level, RandomState ignored, int x, int z, @Nullable MutableObject<NoiseColumn> column, @Nullable Predicate<BlockState> stoppingState) {
        return super.iterateNoiseColumn(level, customRandomState, x, z, column, stoppingState);
    }

    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState ignored, ChunkAccess chunk) {
        super.buildSurface(level, structureManager, customRandomState, chunk);
    }

    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager structureTemplateManager) {
        if (shouldMakeStructures)
            super.createStructures(registryAccess, structureState, structureManager, chunk, structureTemplateManager);
    }

    @VisibleForTesting
    public void buildSurface(ChunkAccess chunk, WorldGenerationContext context, RandomState ignored, StructureManager structureManager, BiomeManager biomeManager, Registry<Biome> biomes, Blender blender) {
        super.buildSurface(chunk, context, customRandomState, structureManager, biomeManager, biomes, blender);
    }

    public void applyCarvers(WorldGenRegion level, long seed, RandomState ignored, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        super.applyCarvers(level, seed, customRandomState, biomeManager, structureManager, chunk, step);
    }

    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState ignored, StructureManager structureManager, ChunkAccess chunk) {
        return super.fillFromNoise(blender, customRandomState, structureManager, chunk);
    }
}
