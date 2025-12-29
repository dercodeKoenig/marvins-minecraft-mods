package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.multiblockCore.EntityMultiblockMaster;
import advRocketry.Registry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.List;

public class EntityObservatory extends EntityMultiblockMaster {

    public MeshData meshAxle;
    public MeshData meshScope;
    public MeshData meshCasingXPlus;
    public MeshData meshCasingXMinus;

    public VertexBuffer axle;
    public VertexBuffer scope;
    public VertexBuffer casingXPlus;
    public VertexBuffer casingXMinus;

    public int lastLight;

    public EntityObservatory(BlockPos pos, BlockState state) {
        super(Registry.ENTITY_OBSERVATORY.get(), pos, state);
        super.forwardInteractionToMaster = true;

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                axle = new VertexBuffer(VertexBuffer.Usage.STATIC);
                scope = new VertexBuffer(VertexBuffer.Usage.STATIC);
                casingXMinus = new VertexBuffer(VertexBuffer.Usage.STATIC);
                casingXPlus = new VertexBuffer(VertexBuffer.Usage.STATIC);
            });
        }
    }

    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                axle.close();
                scope.close();
                casingXPlus.close();
                casingXMinus.close();
            });
        }
    }


    public static Object[][][] structure =
            new Object[][][]{
                    {{null, null, null, null, null},
                            {null, 's', 'g', 's', null},
                            {null, 's', 's', 's', null},
                            {null, 's', 's', 's', null},
                            {null, null, null, null, null}},

                    {{null, null, null, null, null},
                            {null, 's', 's', 's', null},
                            {null, 's', 'g', 's', null},
                            {null, 's', 's', 's', null},
                            {null, null, null, null, null}},

                    {{null, 's', 's', 's', null},
                            {'s', 'a', 'a', 'a', 's'},
                            {'s', 'a', 'a', 'a', 's'},
                            {'s', 'a', 'g', 'a', 's'},
                            {null, 's', 's', 's', null}},

                    {{null, '*', 'c', '*', null},
                            {'*', 's', 's', 's', '*'},
                            {'*', 's', 's', 's', '*'},
                            {'*', 's', 's', 's', '*'},
                            {null, '*', '*', '*', null}},

                    {{null, '*', '*', '*', null},
                            {'*', 't', 't', 't', '*'},
                            {'*', 't', 'm', 't', '*'},
                            {'*', 't', 't', 't', '*'},
                            {null, '*', '*', '*', null}}};

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(Registry.OBSERVATORY.get()));
        charMapping.put('s', List.of(ARLibRegistry.BLOCK_STRUCTURE.get()));
        charMapping.put('t', List.of(Registry.STRUCTURE_TOWER.get()));
        charMapping.put('g', List.of(Blocks.GLASS));
        charMapping.put('a', List.of(Blocks.AIR));
        charMapping.put('m', List.of(ARLibRegistry.BLOCK_MOTOR.get()));
        charMapping.put('*', List.of(
                ARLibRegistry.BLOCK_STRUCTURE.get(),
                ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()
        ));
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }


    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        Block block = stateInWorld.getBlock();
        if (block.equals(ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()))
            return false;
        if (block.equals(ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get()))
            return false;
        if (block.equals(ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get()))
            return false;

        return true;

    }
}
