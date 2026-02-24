package advRocketry.worldgen;

import advRocketry.Main;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public class PlanetDimensionGeneration {
    public static DimensionType makePlanetDimensionType(OptionalLong fixedTime) {
        return new DimensionType(
                fixedTime,
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


    public static ChunkGenerator makeChunkGenerator(BlockState defaultBlock, BlockState defaultFluid, int sealevel, BiomeConfig biomeConfig, boolean structuresEnabled) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        RegistryAccess registryAccess = server.registryAccess();

        NoiseGeneratorSettings overworldSettings = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS).get(NoiseGeneratorSettings.OVERWORLD);
        NoiseGeneratorSettings netherSetting = registryAccess.registryOrThrow(Registries.NOISE_SETTINGS).get(NoiseGeneratorSettings.NETHER);

        SurfaceRules.RuleSource customBiomeRule = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon"))),
                SurfaceRules.sequence(
                        // The very top block (grass layer)
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.DIAMOND_BLOCK.defaultBlockState())),
                        // The blocks right under the top block (dirt layer)
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.REDSTONE_BLOCK.defaultBlockState()))
                )
        );

        SurfaceRules.RuleSource surfaceRules = SurfaceRules.sequence(
                customBiomeRule,
                overworldSettings.surfaceRule()
                //netherSetting.surfaceRule()
        );

        ChunkGenerator generator = new CustomChunkGenerator(
                MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomeConfig.createBiomeConfig())),
                Holder.direct(new NoiseGeneratorSettings(
                        new NoiseSettings(-64, 384, 2, 2),
                        defaultBlock,
                        defaultFluid,
                        overworldSettings.noiseRouter(),
                        surfaceRules,
                        overworldSettings.spawnTarget(),
                        sealevel,
                        true,
                        true,
                        false,
                        false
                )),
                structuresEnabled
        );

        return generator;
    }
}
