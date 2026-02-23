package advRocketry.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class CustomChunkGenerator extends NoiseBasedChunkGenerator {
    boolean shouldMakeStructures;

    public CustomChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, boolean createStructures) {
        super(biomeSource, settings);
        shouldMakeStructures = createStructures;
    }

    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager structureTemplateManager) {
        if (shouldMakeStructures)
            super.createStructures(registryAccess, structureState, structureManager, chunk, structureTemplateManager);
    }
}
