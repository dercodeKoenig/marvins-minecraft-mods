package advRocketry.experiments;

import advRocketry.Dimension.DimensionManager;
import advRocketry.Main;
import com.mojang.datafixers.util.Pair;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

// TODO: maybe fine a way to generate the chunks without structures / decorations for biome changing?
//  this would be very important for the biome changer system

public class ModDimensions {

    private static Climate.Parameter scale(Climate.Parameter original) {
        float min = Climate.unquantizeCoord(original.min());
        float max = Climate.unquantizeCoord(original.max());

        // Example: compress range to half and shift toward 0
        float newMin = Mth.clamp(min * 0.5f, -1f, 1f);
        float newMax = Mth.clamp(max * 0.5f, -1f, 1f);

        return new Climate.Parameter(Climate.quantizeCoord(newMin), Climate.quantizeCoord(newMax));
    }

    public static void addDimensions(String id) {

        RegistryAccess r = ServerLifecycleHooks.getCurrentServer().registryAccess();

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomelist = new ArrayList<>();

        biomelist.add(Pair.of(
                Climate.parameters(
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(0, 1),
                        0
                ), r.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.ICE_SPIKES)
        ));

        biomelist.add(Pair.of(
                Climate.parameters(
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 1),
                        Climate.Parameter.span(-1, 0),
                        0
                ), r.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT)
        ));


        Climate.ParameterList<Holder<Biome>> biomeList = new Climate.ParameterList<>(
                biomelist
        );


        // use this generator to replicate a level exactly for terraforming
        ServerLevel l = DimensionManager.getServerLevel(ServerLifecycleHooks.getCurrentServer(), ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        l.getChunkSource().getGenerator();

        // same map different biomes. heightmap seems to be very similar
        ChunkGenerator generator = new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromList(biomeList),
                ((NoiseBasedChunkGenerator) l.getChunkSource().getGenerator()).generatorSettings()
        );


        DynamicDimensionRegistry dd = DynamicDimensionRegistry.from(ServerLifecycleHooks.getCurrentServer());
        dd.createDynamicDimension(
                ResourceLocation.fromNamespaceAndPath(Main.MODID, id),
                generator,
                //l.getChunkSource().getGenerator(),
                new DimensionType(
                        OptionalLong.of(6000),
                        true,
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
                        0.0f, // ambientLight
                        new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
                )
        );

    }

    static boolean state1 = false;

    public static void serverTick(ServerTickEvent.Post event) {

        ServerLevel l = DimensionManager.getServerLevel(event.getServer(), ResourceLocation.fromNamespaceAndPath("adv_rocketry", "2"));
        if (l != null && !state1) {
            state1 = true;
        }
    }

}

