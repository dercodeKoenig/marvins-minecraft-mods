package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import advRocketry.Registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;

public class EntityLaserDrill extends EntityMultiblockMachineMaster {


    public static Object[][][] structure =
            new Object[][][]{
                    {
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, null, null, null, null, null, null, null, null, null},
                            {ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, null, null, null, null, null, null, null, null},
                            {null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null},
                            {null, null, null, null, null, null, null, null, null, null, null}
                    },
                    {
                            {null, null, null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.GLASS, Blocks.GLASS, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, null, Blocks.GLASS, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null},
                            {ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.GLASS, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, null, null, null, null, null, null},
                            {ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, null, Blocks.GLASS, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.GLASS, Blocks.GLASS, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {null, null, null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null}
                    },
                    {
                            {null, null, null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.GLASS, Blocks.GLASS, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {'O', 'c', 'O', null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, null, Blocks.GLASS, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null},
                            {ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, Blocks.GLASS, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, null, null, null, null, null, null},
                            {ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, ARLibRegistry.BLOCK_STRUCTURE, null, Blocks.GLASS, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.GLASS, Blocks.GLASS, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, 'P'},
                            {null, null, null, null, null, null, ARLibRegistry.BLOCK_ADVANCED_STRUCTURE, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, Blocks.VACUUM_LASER, null}
                    },
            };

    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(advRocketry.Registry.Blocks.LASERDRILL.get()));
        charMapping.put('O', List.of(ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get()));
    }


    public EntityLaserDrill(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_LASERDRILL.get(), pos, state);
        super.forwardInteractionToMaster = true;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityLaserDrill) t).tick();
    }

    public void tick(){
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }


    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        super.readServer(tag, player);
    }

    @Override
    public void readClient(CompoundTag tag) {
        super.readClient(tag);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        return true;
    }

    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            openGui((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    public void openGui(ServerPlayer player) {

    }
}