package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Utils.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

    public static void setup() {
        topBlocks.put(ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon"), advRocketry.Registry.Blocks.MOON_TURF.get());
        topBlocks.put(ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon_dark"), advRocketry.Registry.Blocks.MOON_TURF_DARK.get());

        topBlocks.put(rl("minecraft:badlands"), Blocks.TERRACOTTA);

        addDecoration(rl("minecraft:bamboo_jungle"), 0.05, Blocks.BAMBOO_SAPLING);

        topBlocks.put(rl("minecraft:beach"), Blocks.SAND);

        addDecoration(rl("minecraft:birch_forest"), 0.05, Blocks.BIRCH_SAPLING);

        addDecoration(rl("minecraft:cherry_grove"), 0.05, Blocks.CHERRY_SAPLING);

        addDecoration(rl("minecraft:dark_forest"), 0.05, Blocks.DARK_OAK_SAPLING);

        topBlocks.put(rl("minecraft:desert"), Blocks.SAND);
        addDecoration(rl("minecraft:desert"), 0.01, Blocks.CACTUS);
        addDecoration(rl("minecraft:desert"), 0.05, Blocks.DEAD_BUSH);

        topBlocks.put(rl("minecraft:eroded_badlands"), Blocks.TERRACOTTA);

        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.CORNFLOWER);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.SUNFLOWER);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.WHITE_TULIP);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.ORANGE_TULIP);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.RED_TULIP);
        addDecoration(rl("minecraft:flower_forest"), 0.05, Blocks.PINK_TULIP);

        addDecoration(rl("minecraft:forest"), 0.05, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:forest"), 0.02, Blocks.BIRCH_SAPLING);

        topBlocks.put(rl("minecraft:grove"), Blocks.STONE);

        topBlocks.put(rl("minecraft:ice_spikes"), Blocks.SNOW_BLOCK);

        topBlocks.put(rl("minecraft:jagged_peaks"), Blocks.STONE);
        addDecoration(rl("minecraft:jagged_peaks"), 0.1, Blocks.SNOW);

        addDecoration(rl("minecraft:jungle"), 0.05, Blocks.JUNGLE_SAPLING);
        addDecoration(rl("minecraft:jungle"), 0.005, Blocks.BAMBOO_SAPLING);

        topBlocks.put(rl("minecraft:mangrove_swamp"), Blocks.MUD);
        addDecoration(rl("minecraft:mangrove_swamp"), 0.005, Blocks.MANGROVE_PROPAGULE);

        addDecoration(rl("minecraft:meadow"), 0.005, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:meadow"), 0.005, Blocks.BIRCH_SAPLING);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.TORCHFLOWER);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.WHITE_TULIP);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.ORANGE_TULIP);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.RED_TULIP);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.PINK_TULIP);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.LILY_OF_THE_VALLEY);
        addDecoration(rl("minecraft:meadow"), 0.05, Blocks.SHORT_GRASS);

        topBlocks.put(rl("minecraft:mushroom_field"), Blocks.MYCELIUM);
        addDecoration(rl("minecraft:mushroom_field"), 0.03, Blocks.RED_MUSHROOM);
        addDecoration(rl("minecraft:mushroom_field"), 0.05, Blocks.BROWN_MUSHROOM);

        addDecoration(rl("minecraft:old_growth_birch_forest"), 0.05, Blocks.BIRCH_SAPLING);
        addDecoration(rl("minecraft:old_growth_pine_taiga"), 0.05, Blocks.SPRUCE_SAPLING);
        addDecoration(rl("minecraft:old_growth_spruce_taiga"), 0.05, Blocks.SPRUCE_SAPLING);

        addDecoration(rl("minecraft:plains"), 0.01, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:plains"), 0.02, Blocks.WHITE_TULIP);
        addDecoration(rl("minecraft:plains"), 0.02, Blocks.ORANGE_TULIP);
        addDecoration(rl("minecraft:plains"), 0.02, Blocks.RED_TULIP);
        addDecoration(rl("minecraft:plains"), 0.02, Blocks.PINK_TULIP);
        addDecoration(rl("minecraft:plains"), 0.02, Blocks.SHORT_GRASS);

        addDecoration(rl("minecraft:savanna"), 0.02, Blocks.ACACIA_SAPLING);
        addDecoration(rl("minecraft:savanna"), 0.02, Blocks.SHORT_GRASS);
        addDecoration(rl("minecraft:savanna"), 0.01, Blocks.TALL_GRASS);

        addDecoration(rl("minecraft:savanna_plateau"), 0.02, Blocks.ACACIA_SAPLING);
        addDecoration(rl("minecraft:savanna_plateau"), 0.02, Blocks.SHORT_GRASS);
        addDecoration(rl("minecraft:savanna_plateau"), 0.01, Blocks.TALL_GRASS);

        topBlocks.put(rl("minecraft:snowy_beach"), Blocks.SAND);

        addDecoration(rl("minecraft:snowy_plains"), 0.005, Blocks.SPRUCE_SAPLING);
        addDecoration(rl("minecraft:snowy_plains"), 0.01, Blocks.TALL_GRASS);
        addDecoration(rl("minecraft:snowy_plains"), 0.5, Blocks.SNOW);

        topBlocks.put(rl("minecraft:snowy_slopes"), Blocks.SNOW_BLOCK);
        addDecoration(rl("minecraft:snowy_slopes"), 0.5, Blocks.SNOW);

        addDecoration(rl("minecraft:snowy_taiga"), 0.05, Blocks.SPRUCE_SAPLING);
        addDecoration(rl("minecraft:snowy_taiga"), 0.01, Blocks.TALL_GRASS);
        addDecoration(rl("minecraft:snowy_taiga"), 0.5, Blocks.SNOW);

        addDecoration(rl("minecraft:sparse_jungle"), 0.05, Blocks.JUNGLE_SAPLING);

        topBlocks.put(rl("minecraft:stony_peaks"), Blocks.STONE);

        topBlocks.put(rl("minecraft:stony_shore"), Blocks.STONE);

        addDecoration(rl("minecraft:sunflower_plains"), 0.1, Blocks.SUNFLOWER);

        addDecoration(rl("minecraft:swamp"), 0.03, Blocks.OAK_SAPLING);
        addDecoration(rl("minecraft:swamp"), 0.05, Blocks.BROWN_MUSHROOM);
        addDecoration(rl("minecraft:swamp"), 0.02, Blocks.BLUE_ORCHID);

        addDecoration(rl("minecraft:taiga"), 0.08, Blocks.SPRUCE_SAPLING);
        addDecoration(rl("minecraft:taiga"), 0.02, Blocks.TALL_GRASS);

        addDecoration(rl("minecraft:windswept_forest"), 0.02, Blocks.OAK_SAPLING);

        topBlocks.put(rl("minecraft:windswept_gravelly_hills"), Blocks.GRAVEL);

        addDecoration(rl("minecraft:windswept_hills"), 0.002, Blocks.OAK_SAPLING);

        addDecoration(rl("minecraft:windswept_savanna"), 0.002, Blocks.ACACIA_SAPLING);

        addDecoration(rl("minecraft:wooded_badlands"), 0.01, Blocks.OAK_SAPLING);

    }

    public static void addDecoration(ResourceLocation biome, double p, Block block) {
        decorations.putIfAbsent(biome, new HashMap<>());
        decorations.get(biome).put(block, p);
    }

    public static Map<Block, Double> getDecoration(ResourceLocation biome) {
        return decorations.getOrDefault(biome, new HashMap<>());
    }

    public static Block getTopBlock(ResourceLocation biomeId) {
        return topBlocks.getOrDefault(biomeId, Blocks.GRASS_BLOCK);
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

    public static void maybeUpdateBlocksForNewBiome(ServerLevel level, int x, int z) {
        // biomes are 3d now but we sample the one at the surface for all the math
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
        ResourceLocation currentBiomeId = getCurrentSurfaceBiome(level, x, z);
        ResourceLocation generatedBiomeId = getGeneratedBiome(level.getChunkAt(pos), pos.getX(), pos.getZ());
        if (!Objects.equal(currentBiomeId, generatedBiomeId)) {
            Block toReplace = getTopBlock(generatedBiomeId);
            Block toPlace = getTopBlock(currentBiomeId);
            System.out.println(pos + ": " + generatedBiomeId + " - " + currentBiomeId);
            for (int y = level.getMaxBuildHeight(); y > level.getMinBuildHeight(); y--) {
                BlockState current = level.getBlockState(pos.atY(y));
                if (current.getBlock().equals(toReplace)) {
                    level.setBlock(pos.atY(y), toPlace.defaultBlockState(), 3);
                    // maybe place decoration on top?
                    Map<Block, Double> decorations = getDecoration(currentBiomeId);
                    ArrayList<Block> shuffled = new ArrayList<>(decorations.keySet());
                    Collections.shuffle(shuffled);
                    for (Block block : shuffled) {
                        double p = decorations.get(block);
                        if (Math.random() < p) {
                            BlockState toPlaceState = block.defaultBlockState();
                            BlockPos toPlacePos = pos.atY(y + 1);
                            if (toPlaceState.canSurvive(level, toPlacePos)) {
                                level.setBlock(toPlacePos, toPlaceState, 3);
                                break;
                            }
                        }
                    }
                    // only replace top block
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
}
