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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public class SpaceDimensionGeneration {
    public static DimensionType makeDimensionType() {
        return new DimensionType(
                OptionalLong.empty(),
                false,
                false,
                false,
                false,
                1.0,
                false,
                false,
                0,
                1,
                1,
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                0.0f, // ambientLight
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

        NoiseGeneratorSettings overworldSettings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS).get(NoiseGeneratorSettings.OVERWORLD);


        ChunkGenerator generator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(makeDimensionConfig())),
                Holder.direct(new NoiseGeneratorSettings(
                        new NoiseSettings(0, 1, 1, 1),
                        Blocks.AIR.defaultBlockState(),
                        Blocks.AIR.defaultBlockState(),
                        overworldSettings.noiseRouter(),
                        overworldSettings.surfaceRule(),
                        overworldSettings.spawnTarget(),
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
