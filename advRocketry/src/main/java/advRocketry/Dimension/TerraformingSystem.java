package advRocketry.Dimension;

import advRocketry.Main;
import advRocketry.Registry.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.List;

public class TerraformingSystem {
    // biomeid : top block
    // when terraforming we replace old biomes top block with new biomes top block if old top block is present
    public static HashMap<ResourceLocation, Block> topBlocks = new HashMap<>();
    static {
        topBlocks.put(ResourceLocation.fromNamespaceAndPath(Main.MODID, "moon"), Blocks.MOON_TURF.get());
    }
    public static Block getTopBlock(ResourceLocation biomeId){
        return topBlocks.getOrDefault(biomeId, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    }
    public static void changeBiome(ServerLevel level, BlockPos pos, ResourceLocation biomeId){
        LevelChunk chunk = (LevelChunk) level.getChunk(pos);
        Holder<Biome> target = ServerLifecycleHooks.getCurrentServer().registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeId).get();

        int qx = QuartPos.fromBlock(pos.getX());
        int lx = QuartPos.quartLocal(qx);
        int qz = QuartPos.fromBlock(pos.getZ());
        int lz = QuartPos.quartLocal(qz);

        // work the entire column for every section, every y quart
        for(int k = chunk.getMinSection(); k < chunk.getMaxSection(); ++k) {
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
}
