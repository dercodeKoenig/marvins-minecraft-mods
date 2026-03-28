package BetterPipes.PipeBase;

import AgeOfSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import BetterPipes.Tank.BlockTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static BetterPipes.Registry.*;

abstract public class BlockPipe extends Block implements EntityBlock {
    public static Map<Direction, EnumProperty<ConnectionState>> connections = new HashMap<>();

    static {
        for (Direction i : Direction.values()) {
            connections.put(i, EnumProperty.create(i.getName(), ConnectionState.class));
        }
    }

    public BlockPipe() {
        super(BlockBehaviour.Properties.of().noOcclusion().strength(1.0f).instabreak());
        BlockState state = this.stateDefinition.any();
        for (Direction i : Direction.values()) {
            state = state.setValue(connections.get(i), ConnectionState.NONE);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction i : Direction.values()) {
            builder.add(connections.get(i));
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.create(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player.getMainHandItem().isEmpty()) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof EntityPipe pipe) {
                if (player.isShiftKeyDown()) {
                    pipe.toggleExtractionMode(hitResult.getDirection());
                }
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // required because we can connect to fluid handlers
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos), 3);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 2;
    }

    public void updateTankCons(EntityPipe pipe, BlockState neighborState, Direction direction) {
        if (neighborState.getBlock() instanceof BlockTank) {
            if (direction == Direction.EAST) pipe.tankEast = true;
            if (direction == Direction.WEST) pipe.tankWest = true;
            if (direction == Direction.NORTH) pipe.tankNorth = true;
            if (direction == Direction.SOUTH) pipe.tankSouth = true;
        } else {
            if (direction == Direction.EAST) pipe.tankEast = false;
            if (direction == Direction.WEST) pipe.tankWest = false;
            if (direction == Direction.NORTH) pipe.tankNorth = false;
            if (direction == Direction.SOUTH) pipe.tankSouth = false;
        }
        pipe.setChanged();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof EntityPipe pipe)) return state;

        updateTankCons(pipe, neighborState, direction);

        IFluidHandler fluidHandler = pipe.connections.get(direction).neighborFluidHandler();

        if (fluidHandler != null) {
            ConnectionState current = state.getValue(connections.get(direction));
            if (current != ConnectionState.CONNECTED && current != ConnectionState.EXTRACTION)
                state = state.setValue(connections.get(direction), ConnectionState.CONNECTED);
        } else {

            if (neighborState.isFaceSturdy(level, neighborPos, direction.getOpposite())) {
                state = state.setValue(connections.get(direction), ConnectionState.STRUCTURE);
            } else {
                state = state.setValue(connections.get(direction), ConnectionState.NONE);
            }

            pipe.connections.get(direction).tank.setFluid(FluidStack.EMPTY);

        }

        BlockEntity other = level.getBlockEntity(neighborPos);
        if (other instanceof EntityCrankShaftBase cs &&
                cs.myType == ICrankShaftConnector.CrankShaftType.SMALL &&
                cs.getMechanicalBlock(direction.getOpposite()) != null &&
                cs.getBlockEntity().getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != direction.getAxis()
        ) {
            if (((EntityPipe) tile).crankShaftSide == null) {
                ((EntityPipe) tile).crankShaftSide = direction;
            }
        } else {
            if (((EntityPipe) tile).crankShaftSide == direction)
                ((EntityPipe) tile).crankShaftSide = null;
        }

        return state;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityPipe::tick;
    }

    public enum ConnectionState implements StringRepresentable {
        NONE("none"),
        CONNECTED("connection"),
        EXTRACTION("extraction"),
        STRUCTURE("structure");

        private final String name;

        ConnectionState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}