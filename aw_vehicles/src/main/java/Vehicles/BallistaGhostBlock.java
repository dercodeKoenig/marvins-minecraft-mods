package Vehicles;

import AgeOfSteam.Blocks.Mechanics.Axle.EntityAxleBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Random;


// ghost blocks to avoid entities getting stuck trying to go through siege weapons because pathfinders do not know they can not pass
public class BallistaGhostBlock extends Block {
    public BallistaGhostBlock() {
        super(Properties.of().noOcclusion());
    }

    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return true;
    }

    VoxelShape shape = Shapes.create((double) 0.4F, (double) 0.1F, (double) 0.4F, (double) 0.6F, (double) 0.2F, (double) 0.6F);
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<Ballista> hosts = level.getEntitiesOfClass(Ballista.class, new AABB(pos));
        if (hosts.isEmpty()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!world.isClientSide()) {
            world.scheduleTick(pos, this, 20);
        }
        super.onPlace(state, world, pos, oldState, isMoving);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof Ballista) {
                return Shapes.empty();
            }
            if (entity instanceof BallistaBolt) {
                return Shapes.empty();
            }
        }
        return this.shape;
    }
}
