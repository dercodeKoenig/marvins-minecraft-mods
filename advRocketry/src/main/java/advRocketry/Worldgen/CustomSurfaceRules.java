package advRocketry.Worldgen;

import advRocketry.Main;
import advRocketry.Registry.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.SurfaceRules;

public class CustomSurfaceRules {
    public static SurfaceRules.RuleSource customBiomeRule = SurfaceRules.sequence(


            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon"))),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.MOON_TURF.get().defaultBlockState())),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.MOON_TURF.get().defaultBlockState()))
                    )
            ),


            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon_dark"))),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.MOON_TURF_DARK.get().defaultBlockState())),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.MOON_TURF_DARK.get().defaultBlockState()))
                    )
            ),


            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("basalt_deltas"))),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState())),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState()))
                    )
            )


    );
}
