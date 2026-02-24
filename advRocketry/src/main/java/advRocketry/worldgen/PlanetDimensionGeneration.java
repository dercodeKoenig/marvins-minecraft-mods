package advRocketry.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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



        SurfaceRules.RuleSource surfaceRules = SurfaceRules.sequence(
                CustomSurfaceRules.customBiomeRule,
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
