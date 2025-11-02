package advRocketry.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public class SpaceDimensionGeneration {
    public static DimensionType makeDimensionType() {
        return new DimensionType(
                OptionalLong.of(6000),
                false,
                false,
                false,
                false,
                1.0,
                false,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                1f, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        );
    }

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> makeDimensionConfig() {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> list = new ArrayList<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RegistryAccess registryAccess = server.registryAccess();
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        list.add(Pair.of(
                new Climate.ParameterPoint(
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        0
                ), biomeRegistry.getHolderOrThrow(Biomes.THE_VOID)
        ));
        return list;
    }

    public static ChunkGenerator makeChunkGenerator() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RegistryAccess registryAccess = server.registryAccess();

        DensityFunction zero = DensityFunctions.constant(-100);

        NoiseRouter emptyRouter = new NoiseRouter(
                zero, // barrierNoise
                zero, // fluidLevelFloodednessNoise
                zero, // fluidLevelSpreadNoise
                zero, // lavaNoise
                zero, // temperature
                zero, // vegetation
                zero, // continents
                zero, // erosion
                zero, // depth
                zero, // ridges
                zero, // initialDensityWithoutJaggedness
                zero, // finalDensity
                zero, // veinToggle
                zero, // veinRidged
                zero  // veinGap
        );

        SurfaceRules.RuleSource airSurfaceRule = SurfaceRules.state(Blocks.AIR.defaultBlockState());

        ChunkGenerator generator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(makeDimensionConfig())),
                Holder.direct(new NoiseGeneratorSettings(
                        new NoiseSettings(-64, 384, 1, 1),
                        Blocks.AIR.defaultBlockState(),
                        Blocks.AIR.defaultBlockState(),
                        emptyRouter,
                        airSurfaceRule,
                        List.of(),
                        0,
                        true,
                        false,
                        false,
                        false
                ))
        );
        return generator;
    }
}
