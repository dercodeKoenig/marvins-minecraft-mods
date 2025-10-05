package advRocketry.Dimension;

import advRocketry.Main;
import com.mojang.serialization.MapCodec;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

public class ModDimensions {


    public static void addDimensions(String id) {

        RegistryAccess r = ServerLifecycleHooks.getCurrentServer().registryAccess();
        ChunkGenerator generator = new FlatLevelSource(
                new FlatLevelGeneratorSettings(
                        Optional.empty(),
                        r.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS),
                        List.of()
                )
        );


        DynamicDimensionRegistry dd = DynamicDimensionRegistry.from(ServerLifecycleHooks.getCurrentServer());
        dd.createDynamicDimension(
                ResourceLocation.fromNamespaceAndPath(Main.MODID, id),
                generator,
                new DimensionType(
                        OptionalLong.empty(), // fixedTime
                        true, // hasSkylight
                        false, // hasCeiling
                        false, // ultrawarm
                        true, // natural
                        1.0, // coordinateScale
                        true, // bedWorks
                        false, // respawnAnchorWorks
                        -64, // minY
                        384, // height
                        384, // logicalHeight
                        BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                        ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                        0.0f, // ambientLight
                        new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0)
                )
        );

    }
}

