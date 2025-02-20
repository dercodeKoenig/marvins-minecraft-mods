package NPCs.Blocks.Armory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static NPCs.Registry.ENTITY_ARMORY;
import static NPCs.Registry.ENTITY_STRATEGY_TABLE;

public class BlockArmory extends Block implements EntityBlock {
    public BlockArmory() {
        super(Properties.of().noOcclusion());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_ARMORY.get().create(blockPos,blockState);
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityArmory::tick;
    }


    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity e = level.getBlockEntity(pos);
        if (e instanceof EntityArmory t) {
            t.useWithoutItem(player);
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }


    // this is just because it blocks light and i dont know how to prevent it else
    VoxelShape notFullBlock = Shapes.create(0.01, 0.01, 0.01, 0.99, 0.99, 0.99);
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.notFullBlock;
    }
}
