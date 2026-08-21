package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Utils.ChunkUtils;
import advRocketry.Utils.NoiseUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.include.com.google.common.base.Objects;

import java.util.*;

public class TerraformingSystem {

    public static final String chunkEntryKey = "terraforming_data";

    // when terraforming we replace old biomes top block with new biomes top block if old top block is present
    public static HashMap<ResourceLocation, Block> topBlocks = new HashMap<>();
    // per biome decorations like sapling to place with a given probability
    public static HashMap<ResourceLocation, Map<Block, Double>> decorations = new HashMap<>();
    // surface patches
    public static HashMap<ResourceLocation, Map<Block, patchData>> patches = new HashMap<>();

    public static void setup() {
        topBlocks.clear();
        decorations.clear();
        patches.clear();

        // --- Mod Specific ---
        topBlocks.put(ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon"), advRocketry.Registry.Blocks.MOON_TURF.get());
        topBlocks.put(ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon_dark"), advRocketry.Registry.Blocks.MOON_TURF_DARK.get());
        topBlocks.put(ResourceLocation.fromNamespaceAndPath(Main.MODID, "ice_crystals"), Blocks.SNOW_BLOCK);


        // --- Forests & Woods ---
        addDecoration(rl("minecraft:forest"), 0.03, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:forest"), 0.01, Blocks.BIRCH_SAPLING);
        addBaseGrass(rl("minecraft:forest"), 0.1, 0.02);
        addBaseFlowers(rl("minecraft:forest"), 0.02);
        addPatch(rl("minecraft:forest"), Blocks.COARSE_DIRT, 0.2, 0.05);

        addDecoration(rl("minecraft:birch_forest"), 0.05, Blocks.BIRCH_SAPLING);
        addBaseGrass(rl("minecraft:birch_forest"), 0.1, 0.02);
        addBaseFlowers(rl("minecraft:birch_forest"), 0.02);
        addDecoration(rl("minecraft:old_growth_birch_forest"), 0.05, Blocks.BIRCH_SAPLING);
        addBaseGrass(rl("minecraft:old_growth_birch_forest"), 0.1, 0.02);
        addBaseFlowers(rl("minecraft:old_growth_birch_forest"), 0.02);

        addDecoration(rl("minecraft:flower_forest"), 0.02, Blocks.OAK_SAPLING);
        addBaseGrass(rl("minecraft:flower_forest"), 0.1, 0.05);
        addBaseFlowers(rl("minecraft:flower_forest"), 0.1);
        addTulips(rl("minecraft:flower_forest"), 0.05);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.CORNFLOWER);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.LILY_OF_THE_VALLEY);

        addDecoration(rl("minecraft:dark_forest"), 0.08, Blocks.DARK_OAK_SAPLING);
        addDecoration(rl("minecraft:dark_forest"), 0.02, Blocks.BROWN_MUSHROOM);
        addDecoration(rl("minecraft:dark_forest"), 0.02, Blocks.RED_MUSHROOM);
        addBaseGrass(rl("minecraft:dark_forest"), 0.05, 0.01);
        addPatch(rl("minecraft:dark_forest"), Blocks.COARSE_DIRT, 0.3, 0.08);

        addDecoration(rl("minecraft:cherry_grove"), 0.05, Blocks.CHERRY_SAPLING);
        addDecoration(rl("minecraft:cherry_grove"), 0.15, Blocks.PINK_PETALS);
        addBaseGrass(rl("minecraft:cherry_grove"), 0.05, 0.01);

        // --- Taigas ---
        addDecoration(rl("minecraft:taiga"), 0.08, Blocks.SPRUCE_SAPLING);
        addBaseGrass(rl("minecraft:taiga"), 0.05, 0.02);
        addPatch(rl("minecraft:taiga"), Blocks.PODZOL, 0.2, 0.05);

        addDecoration(rl("minecraft:old_growth_pine_taiga"), 0.08, Blocks.SPRUCE_SAPLING);
        addPatch(rl("minecraft:old_growth_pine_taiga"), Blocks.PODZOL, 0.6, 0.08);
        addPatch(rl("minecraft:old_growth_pine_taiga"), Blocks.COARSE_DIRT, 0.2, 0.05);

        addDecoration(rl("minecraft:old_growth_spruce_taiga"), 0.08, Blocks.SPRUCE_SAPLING);
        addPatch(rl("minecraft:old_growth_spruce_taiga"), Blocks.PODZOL, 0.6, 0.08);
        addPatch(rl("minecraft:old_growth_spruce_taiga"), Blocks.COARSE_DIRT, 0.2, 0.05);

        // --- Plains & Meadows ---
        addDecoration(rl("minecraft:plains"), 0.01, Blocks.OAK_SAPLING);
        addBaseGrass(rl("minecraft:plains"), 0.2, 0.05);
        addBaseFlowers(rl("minecraft:plains"), 0.02);
        addTulips(rl("minecraft:plains"), 0.01);

        addDecoration(rl("minecraft:sunflower_plains"), 0.1, Blocks.SUNFLOWER);
        addBaseGrass(rl("minecraft:sunflower_plains"), 0.2, 0.05);

        addDecoration(rl("minecraft:meadow"), 0.005, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:meadow"), 0.005, Blocks.BIRCH_SAPLING);
        addBaseGrass(rl("minecraft:meadow"), 0.1, 0.05);
        addTulips(rl("minecraft:meadow"), 0.05);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.CORNFLOWER);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.LILY_OF_THE_VALLEY);

        // --- Jungles ---
        addDecoration(rl("minecraft:jungle"), 0.08, Blocks.JUNGLE_SAPLING);
        addDecoration(rl("minecraft:jungle"), 0.01, Blocks.BAMBOO_SAPLING);
        addDecoration(rl("minecraft:jungle"), 0.02, Blocks.MELON);
        addBaseGrass(rl("minecraft:jungle"), 0.1, 0.05);
        addPatch(rl("minecraft:jungle"), Blocks.PODZOL, 0.2, 0.05);

        addDecoration(rl("minecraft:sparse_jungle"), 0.04, Blocks.JUNGLE_SAPLING);
        addDecoration(rl("minecraft:sparse_jungle"), 0.01, Blocks.MELON);
        addBaseGrass(rl("minecraft:sparse_jungle"), 0.1, 0.02);

        addDecoration(rl("minecraft:bamboo_jungle"), 0.1, Blocks.BAMBOO_SAPLING);
        addPatch(rl("minecraft:bamboo_jungle"), Blocks.PODZOL, 0.4, 0.06);

        // --- Swamps (Using Water Patches!) ---
        topBlocks.put(rl("minecraft:swamp"), Blocks.MUD);
        addDecoration(rl("minecraft:swamp"), 0.03, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:swamp"), 0.05, Blocks.BROWN_MUSHROOM);
        addDecoration(rl("minecraft:swamp"), 0.02, Blocks.BLUE_ORCHID);
        addBaseGrass(rl("minecraft:swamp"), 0.05, 0.05);
        addPatch(rl("minecraft:swamp"), Blocks.WATER, 0.25, 0.06); // Puddles/Shallow lakes
        addPatch(rl("minecraft:swamp"), Blocks.DIRT, 0.3, 0.1);

        topBlocks.put(rl("minecraft:mangrove_swamp"), Blocks.MUD);
        addDecoration(rl("minecraft:mangrove_swamp"), 0.08, Blocks.MANGROVE_PROPAGULE);
        addPatch(rl("minecraft:mangrove_swamp"), Blocks.WATER, 0.35, 0.08); // More watery
        addPatch(rl("minecraft:mangrove_swamp"), Blocks.DIRT, 0.2, 0.1);

        // --- Savannas ---
        addDecoration(rl("minecraft:savanna"), 0.02, Blocks.ACACIA_SAPLING);
        addBaseGrass(rl("minecraft:savanna"), 0.1, 0.1); // High tall grass
        addPatch(rl("minecraft:savanna"), Blocks.COARSE_DIRT, 0.15, 0.05);

        addDecoration(rl("minecraft:savanna_plateau"), 0.02, Blocks.ACACIA_SAPLING);
        addBaseGrass(rl("minecraft:savanna_plateau"), 0.1, 0.1);

        addDecoration(rl("minecraft:windswept_savanna"), 0.01, Blocks.ACACIA_SAPLING);
        addBaseGrass(rl("minecraft:windswept_savanna"), 0.05, 0.05);

        // --- Deserts & Badlands ---
        topBlocks.put(rl("minecraft:desert"), Blocks.SAND);
        addDecoration(rl("minecraft:desert"), 0.01, Blocks.CACTUS);
        addDecoration(rl("minecraft:desert"), 0.03, Blocks.DEAD_BUSH);
        addPatch(rl("minecraft:desert"), Blocks.SANDSTONE, 0.05, 0.05);

        topBlocks.put(rl("minecraft:badlands"), Blocks.TERRACOTTA);
        addDecoration(rl("minecraft:badlands"), 0.02, Blocks.DEAD_BUSH);
        addDecoration(rl("minecraft:badlands"), 0.01, Blocks.CACTUS);
        addPatch(rl("minecraft:badlands"), Blocks.RED_SAND, 0.3, 0.05); // Sand layers

        topBlocks.put(rl("minecraft:eroded_badlands"), Blocks.TERRACOTTA);
        addDecoration(rl("minecraft:eroded_badlands"), 0.01, Blocks.DEAD_BUSH);
        addPatch(rl("minecraft:eroded_badlands"), Blocks.RED_SAND, 0.2, 0.05);

        addDecoration(rl("minecraft:wooded_badlands"), 0.03, Blocks.OAK_SAPLING);
        addBaseGrass(rl("minecraft:wooded_badlands"), 0.05, 0.01);
        addPatch(rl("minecraft:wooded_badlands"), Blocks.COARSE_DIRT, 0.4, 0.06);
        addPatch(rl("minecraft:wooded_badlands"), Blocks.TERRACOTTA, 0.2, 0.05);

        // --- Snow & Ice ---
        topBlocks.put(rl("minecraft:snowy_plains"), Blocks.SNOW_BLOCK);
        addDecoration(rl("minecraft:snowy_plains"), 0.2, Blocks.SNOW); // Snow layers
        addPatch(rl("minecraft:snowy_plains"), Blocks.POWDER_SNOW, 0.1, 0.05); // Traps!
        addPatch(rl("minecraft:snowy_plains"), Blocks.DIRT, 0.05, 0.05);
        addDecoration(rl("minecraft:snowy_plains"), 0.1, Blocks.SPRUCE_SAPLING);

        topBlocks.put(rl("minecraft:snowy_taiga"), Blocks.SNOW_BLOCK);
        addDecoration(rl("minecraft:snowy_taiga"), 0.3, Blocks.SNOW);
        addPatch(rl("minecraft:snowy_taiga"), Blocks.DIRT, 0.25, 0.05); // Exposed dirt
        addDecoration(rl("minecraft:snowy_taiga"), 0.3, Blocks.SPRUCE_SAPLING);

        topBlocks.put(rl("minecraft:snowy_slopes"), Blocks.SNOW_BLOCK);
        addDecoration(rl("minecraft:snowy_slopes"), 0.5, Blocks.SNOW);
        addPatch(rl("minecraft:snowy_slopes"), Blocks.POWDER_SNOW, 0.2, 0.08);
        addPatch(rl("minecraft:snowy_slopes"), Blocks.STONE, 0.1, 0.1);

        topBlocks.put(rl("minecraft:ice_spikes"), Blocks.SNOW_BLOCK);
        addPatch(rl("minecraft:ice_spikes"), Blocks.PACKED_ICE, 0.3, 0.05); // Ice lakes
        addPatch(rl("minecraft:ice_spikes"), Blocks.BLUE_ICE, 0.05, 0.02);

        // --- Mountains & Hills ---
        topBlocks.put(rl("minecraft:grove"), Blocks.SNOW_BLOCK);
        addPatch(rl("minecraft:grove"), Blocks.DIRT, 0.2, 0.05);
        addDecoration(rl("minecraft:grove"), 0.25, Blocks.SPRUCE_SAPLING);

        topBlocks.put(rl("minecraft:jagged_peaks"), Blocks.STONE);
        addDecoration(rl("minecraft:jagged_peaks"), 0.2, Blocks.SNOW);
        addPatch(rl("minecraft:jagged_peaks"), Blocks.SNOW_BLOCK, 0.4, 0.08);

        topBlocks.put(rl("minecraft:stony_peaks"), Blocks.STONE);
        addPatch(rl("minecraft:stony_peaks"), Blocks.CALCITE, 0.1, 0.05);
        addPatch(rl("minecraft:stony_peaks"), Blocks.GRAVEL, 0.2, 0.05);

        topBlocks.put(rl("minecraft:frozen_peaks"), Blocks.STONE);
        addPatch(rl("minecraft:frozen_peaks"), Blocks.SNOW_BLOCK, 0.5, 0.08);
        addPatch(rl("minecraft:frozen_peaks"), Blocks.PACKED_ICE, 0.3, 0.05);
        addDecoration(rl("minecraft:frozen_peaks"), 0.2, Blocks.SNOW);

        topBlocks.put(rl("minecraft:windswept_hills"), Blocks.STONE);
        addPatch(rl("minecraft:windswept_hills"), Blocks.GRASS_BLOCK, 0.4, 0.05);
        addDecoration(rl("minecraft:windswept_hills"), 0.05, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:windswept_hills"), 0.05, Blocks.SPRUCE_SAPLING);

        topBlocks.put(rl("minecraft:windswept_forest"), Blocks.STONE);
        addPatch(rl("minecraft:windswept_forest"), Blocks.GRASS_BLOCK, 0.6, 0.05);
        addDecoration(rl("minecraft:windswept_forest"), 0.1, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:windswept_forest"), 0.1, Blocks.SPRUCE_SAPLING);

        topBlocks.put(rl("minecraft:windswept_gravelly_hills"), Blocks.GRAVEL);
        addPatch(rl("minecraft:windswept_gravelly_hills"), Blocks.GRASS_BLOCK, 0.2, 0.05);

        // --- Shores & Oceans ---
        topBlocks.put(rl("minecraft:beach"), Blocks.SAND);
        topBlocks.put(rl("minecraft:snowy_beach"), Blocks.SAND);
        addDecoration(rl("minecraft:snowy_beach"), 0.2, Blocks.SNOW);
        topBlocks.put(rl("minecraft:stony_shore"), Blocks.STONE);
        addPatch(rl("minecraft:stony_shore"), Blocks.GRAVEL, 0.3, 0.08);

        // --- Unique ---
        topBlocks.put(rl("minecraft:mushroom_fields"), Blocks.MYCELIUM);
        addDecoration(rl("minecraft:mushroom_fields"), 0.03, Blocks.RED_MUSHROOM);
        addDecoration(rl("minecraft:mushroom_fields"), 0.05, Blocks.BROWN_MUSHROOM);
    }

    public static void addDecoration(ResourceLocation biome, double p, Block block) {
        decorations.putIfAbsent(biome, new HashMap<>());
        decorations.get(biome).put(block, p);
    }

    public static void addPatch(ResourceLocation biome, Block block, double p, double scale) {
        patches.putIfAbsent(biome, new HashMap<>());
        patches.get(biome).put(block, new patchData(p, scale));
    }

    public static Map<Block, Double> getDecoration(ResourceLocation biome) {
        return decorations.getOrDefault(biome, new HashMap<>());
    }

    public static Block getTopBlock(ResourceLocation biomeId) {
        return topBlocks.getOrDefault(biomeId, Blocks.GRASS_BLOCK);
    }

    public static Map<Block, patchData> getPatches(ResourceLocation biome) {
        return patches.getOrDefault(biome, new HashMap<>());
    }

    public static void addBaseGrass(ResourceLocation biome, double shortGrassProb, double tallGrassProb) {
        if (shortGrassProb > 0) addDecoration(biome, shortGrassProb, Blocks.SHORT_GRASS);
        if (tallGrassProb > 0) addDecoration(biome, tallGrassProb, Blocks.TALL_GRASS);
    }

    public static void addBaseFlowers(ResourceLocation biome, double prob) {
        addDecoration(biome, prob, Blocks.DANDELION);
        addDecoration(biome, prob, Blocks.POPPY);
    }

    public static void addTulips(ResourceLocation biome, double prob) {
        addDecoration(biome, prob, Blocks.WHITE_TULIP);
        addDecoration(biome, prob, Blocks.ORANGE_TULIP);
        addDecoration(biome, prob, Blocks.RED_TULIP);
        addDecoration(biome, prob, Blocks.PINK_TULIP);
    }

    public static boolean isValidTopBlock(ResourceLocation currentBiomeId, Block block) {
        Set<Block> validBlocks = new HashSet<>();
        validBlocks.add(getTopBlock(currentBiomeId));
        validBlocks.addAll(getPatches(currentBiomeId).keySet());
        if (validBlocks.contains(Blocks.GRASS_BLOCK))
            validBlocks.add(Blocks.DIRT);
        if (validBlocks.contains(Blocks.DIRT))
            validBlocks.add(Blocks.GRASS_BLOCK);
        return validBlocks.contains(block);
    }

    public static void changeBiome(ServerLevel level, int blockX, int blockZ, ResourceLocation biomeId) {
        BlockPos pos = new BlockPos(blockX, 0, blockZ);
        LevelChunk chunk = (LevelChunk) level.getChunk(pos);
        Holder<Biome> target = ServerLifecycleHooks.getCurrentServer().registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeId).get();

        int qx = QuartPos.fromBlock(pos.getX());
        int lx = QuartPos.quartLocal(qx);
        int qz = QuartPos.fromBlock(pos.getZ());
        int lz = QuartPos.quartLocal(qz);

        // work the entire column for every section, every y quart
        for (int k = chunk.getMinSection(); k < chunk.getMaxSection(); ++k) {
            LevelChunkSection levelchunksection = chunk.getSection(chunk.getSectionIndexFromSectionY(k));
            for (int ly = 0; ly < 4; ly++) {
                // minecraft wraps this in a read only interface but the reference should still be the normal container
                PalettedContainer<Holder<Biome>> container = (PalettedContainer<Holder<Biome>>) levelchunksection.getBiomes();
                container.set(lx, ly, lz, target);
            }
        }
        chunk.setUnsaved(true);

        ClientboundChunksBiomesPacket packet = new ClientboundChunksBiomesPacket(List.of(new ClientboundChunksBiomesPacket.ChunkBiomeData(chunk)));
        level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false).forEach(player -> {
            player.connection.send(packet);
        });
    }

    // We replace maybePatch entirely with this O(1) mathematical lookup
    public static Block getPatchedTopBlock(ResourceLocation biomeId, int x, int z, Block defaultTop) {
        Map<Block, patchData> possiblePatches = getPatches(biomeId);
        if (possiblePatches.isEmpty()) {
            return defaultTop;
        }

        for (Map.Entry<Block, patchData> entry : possiblePatches.entrySet()) {
            Block block = entry.getKey();
            patchData d = entry.getValue();

            // Create a unique offset for this block so Gravel and Coarse Dirt don't overlap perfectly
            // We just use the block's hash code as an arbitrary large number.
            long blockOffset = block.hashCode();

            // Generate the noise using this block's specific scale and offset
            double noiseValue = NoiseUtils.getNormalized2D(
                    (x + blockOffset) * d.scale,
                    (z + blockOffset) * d.scale
            );

            // Since noiseValue is 0.0 to 1.0, we just check if it's in the top 'p' percent
            // If p is 0.05 (5%), we check if noise is >= 0.95.
            if (noiseValue >= (1.0 - d.p)) {
                return block; // First patch to succeed wins the overlap!
            }
        }

        return defaultTop;
    }

    public static void maybeDecorate(ResourceLocation biomeId, Level level, BlockPos surfacePos) {
        Map<Block, Double> decorations = getDecoration(biomeId);
        ArrayList<Block> shuffled = new ArrayList<>(decorations.keySet());
        Collections.shuffle(shuffled);
        for (Block block : shuffled) {
            double p = decorations.get(block);
            if (Math.random() < p) {
                BlockState toPlaceState = block.defaultBlockState();
                BlockPos placePos = surfacePos.above();
                if (toPlaceState.canSurvive(level, placePos) && level.getBlockState(placePos).canBeReplaced()) {
                    level.setBlock(placePos, toPlaceState, 3);
                    return;
                }
            }
        }
    }

    public static void maybeUpdateBlocksForNewBiome(ServerLevel level, int x, int z) {
        // biomes are 3d now but we sample the one at the surface for all the math
        BlockPos pos = new BlockPos(x, 0, z);
        ResourceLocation currentBiomeId = getCurrentSurfaceBiome(level, x, z);
        ResourceLocation previousBiomeId = getGeneratedBiome(level.getChunkAt(pos), pos.getX(), pos.getZ());

        if (!Objects.equal(currentBiomeId, previousBiomeId)) {
            // the blocks not always perfectly align with biome borders so aso consider top blocks from next biomes for replacement
            Set<ResourceLocation> nearbyBiomes = new HashSet<>();
            nearbyBiomes.add(previousBiomeId);
            for (Direction i : new Direction[]{Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH}) {
                BlockPos neighbor = pos.relative(i, 4);
                nearbyBiomes.add(getGeneratedBiome(level.getChunkAt(neighbor), neighbor.getX(), neighbor.getZ()));
            }

            for (int y = level.getMaxBuildHeight(); y > level.getMinBuildHeight(); y--) {
                BlockState current = level.getBlockState(pos.atY(y));
                boolean isValidTopBlock = false;
                for (ResourceLocation biomeId : nearbyBiomes) {
                    if (isValidTopBlock(biomeId, current.getBlock())) {
                        isValidTopBlock = true;
                    }
                }
                if (isValidTopBlock) {
                    Block toPlace = getPatchedTopBlock(currentBiomeId, x, z, getTopBlock(currentBiomeId));
                    level.setBlock(pos.atY(y), toPlace.defaultBlockState(), 3);

                    // maye add decorations above this block
                    maybeDecorate(currentBiomeId, level, pos.atY(y));

                    break;
                }
            }

            // replace the blocks and save new generated biome id
            storeGeneratedBiome(currentBiomeId, level.getChunkAt(pos), pos.getX(), pos.getZ());
        }
    }

    public static ResourceLocation getCurrentSurfaceBiome(ServerLevel level, int blockX, int blockZ) {
        BlockPos pos = new BlockPos(blockX, level.getHeight(Heightmap.Types.OCEAN_FLOOR, blockX, blockZ), blockZ);
        return level.registryAccess().registryOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
    }

    public static String getPosKey(int x, int z) {
        return x + "_" + z;
    }

    public static void ensureKeyExists(String key, CompoundTag tag) {
        if (!tag.contains(key)) {
            tag.put(key, new CompoundTag());
        }
    }

    public static void storeGeneratedBiome(ResourceLocation biomeId, ChunkAccess chunk, int x, int z) {
        CompoundTag entry = ChunkUtils.getEntryOrNew(chunk, chunkEntryKey);
        String posKey = getPosKey(x, z);
        ensureKeyExists(posKey, entry);
        CompoundTag posTag = entry.getCompound(posKey);
        posTag.putString("generatedBiome", biomeId.toString());
        ChunkUtils.setEntry(chunk, chunkEntryKey, entry);
    }

    public static ResourceLocation getGeneratedBiome(ChunkAccess chunk, int x, int z) {
        CompoundTag entry = ChunkUtils.getEntryOrNew(chunk, chunkEntryKey);
        String posKey = getPosKey(x, z);
        ensureKeyExists(posKey, entry);
        CompoundTag posTag = entry.getCompound(posKey);
        if (posTag.contains("generatedBiome"))
            return rl(posTag.getString("generatedBiome"));
        return null;
    }

    public static void storeGeneratedBiomePreset(String presetName, ChunkAccess chunk, int x, int z) {
        CompoundTag entry = ChunkUtils.getEntryOrNew(chunk, chunkEntryKey);
        String posKey = getPosKey(x, z);
        ensureKeyExists(posKey, entry);
        CompoundTag posTag = entry.getCompound(posKey);
        posTag.putString("generatedPreset", presetName);
        ChunkUtils.setEntry(chunk, chunkEntryKey, entry);
    }

    public static String getGeneratedBiomePreset(ChunkAccess chunk, int x, int z) {
        CompoundTag entry = ChunkUtils.getEntryOrNew(chunk, chunkEntryKey);
        String posKey = getPosKey(x, z);
        ensureKeyExists(posKey, entry);
        CompoundTag posTag = entry.getCompound(posKey);
        if (posTag.contains("generatedPreset"))
            return posTag.getString("generatedPreset");
        return "";
    }

    // short for parse
    public static ResourceLocation rl(String id) {
        return ResourceLocation.parse(id);
    }

    // Replace your old patchData with this:
    public static class patchData {
        double scale; // Controls the SIZE of the blobs (Replaces min/max)
        double p;     // Controls the CHANCE/COVERAGE amount

        public patchData(double p, double scale) {
            this.scale = scale;
            this.p = p;
        }
    }
}
