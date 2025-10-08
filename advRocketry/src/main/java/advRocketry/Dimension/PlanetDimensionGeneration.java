package advRocketry.Dimension;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public class PlanetDimensionGeneration {
    public static DimensionType makePlanetDimensionType() {
        return new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                0.0f, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        );
    }

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> makeHotDryDimensionConfig() {
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
                ), biomeRegistry.getHolderOrThrow(Biomes.DESERT)
        ));
        return list;
    }

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> makeFrozenDimensionConfig() {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> list = new ArrayList<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RegistryAccess registryAccess = server.registryAccess();
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        list.add(Pair.of(
                new Climate.ParameterPoint(
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 0),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        0
                ), biomeRegistry.getHolderOrThrow(Biomes.FROZEN_OCEAN)
        ));
        list.add(Pair.of(
                new Climate.ParameterPoint(
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(0, 1),
                        Climate.Parameter.span(-1, 0),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        0
                ), biomeRegistry.getHolderOrThrow(Biomes.ICE_SPIKES)
        ));
        list.add(Pair.of(
                new Climate.ParameterPoint(
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(0, 1),
                        Climate.Parameter.span(0, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        0
                ), biomeRegistry.getHolderOrThrow(Biomes.SNOWY_PLAINS)
        ));
        return list;
    }

    public static ChunkGenerator makeChunkGenerator(BlockState defaultBlock, BlockState defaultFluid, int sealevel, List<Pair<Climate.ParameterPoint, Holder<Biome>>> dimensionConfig) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RegistryAccess registryAccess = server.registryAccess();

        NoiseGeneratorSettings overworldSettings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS).get(NoiseGeneratorSettings.OVERWORLD);


        ChunkGenerator generator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(dimensionConfig)),
                Holder.direct(new NoiseGeneratorSettings(
                        new NoiseSettings(-64, 384, 1, 1),
                        defaultBlock,
                        defaultFluid,
                        overworldSettings.noiseRouter(),
                        overworldSettings.surfaceRule(),
                        overworldSettings.spawnTarget(),
                        sealevel,
                        true,
                        true,
                        true,
                        false
                ))
        );

        return generator;
    }
}
