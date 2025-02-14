package BetterPipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static BetterPipes.Registry.*;

public class BlockPipe extends Block implements EntityBlock {
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

    public static Map<Direction, EnumProperty<ConnectionState>> connections = new HashMap<>();

    public static BooleanProperty pipe_is_extraction = BooleanProperty.create("main_isextraction");
    public static BooleanProperty pipe_is_extraction_active = BooleanProperty.create("main_isextractionactive");

    public static Map<Direction, BooleanProperty> otherConnections = new HashMap<>();
    static{

        otherConnections.put(Direction.EAST,BooleanProperty.create("c_east"));
        otherConnections.put(Direction.WEST,BooleanProperty.create("c_west"));
        otherConnections.put(Direction.NORTH,BooleanProperty.create("c_north"));
        otherConnections.put(Direction.SOUTH,BooleanProperty.create("c_south"));

    }

    static {
        for (Direction i : Direction.values()) {
            connections.put(i, EnumProperty.create(i.getName(), ConnectionState.class));
        }
    }


    public BlockPipe(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (Direction i : Direction.values()) {
            state = state.setValue(connections.get(i), ConnectionState.NONE);
        }
        for (Direction i : otherConnections.keySet()){
            state =        state.setValue(otherConnections.get(i), false);
        }
        state = state.setValue(pipe_is_extraction, false);
        state = state.setValue(pipe_is_extraction_active, false);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction i : Direction.values()) {
            builder.add(connections.get(i));
        }
        builder.add(pipe_is_extraction);
        builder.add(pipe_is_extraction_active);
        for (Direction i : otherConnections.keySet()) {
            builder.add(otherConnections.get(i));
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.create(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ENTITY_PIPE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player.getMainHandItem().isEmpty()) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof EntityPipe pipe) {
                if (player.isShiftKeyDown()) {
                    pipe.toggleExtractionMode();
                } else {
                    pipe.toggleExtractionActive();
                }
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos, updateFromNeighbourShapes(state, level, pos),3) ;
    }

    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 2;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof EntityPipe pipe)) return state;


if(otherConnections.containsKey(direction)) {
    if (neighborState.getBlock() instanceof BlockTank) {
        state = state.setValue(otherConnections.get(direction), true);
    } else {
        state = state.setValue(otherConnections.get(direction), false);
    }
}

        IFluidHandler fluidHandler = pipe.connections.get(direction).neighborFluidHandler();

        if (fluidHandler != null) {
            state = state.setValue(connections.get(direction), ConnectionState.CONNECTED);
        } else {

            if (neighborState.isSolidRender(tile.getLevel(), neighborPos)) {
                state = state.setValue(connections.get(direction), ConnectionState.STRUCTURE);
            }else{
                state = state.setValue(connections.get(direction), ConnectionState.NONE);
            }

            pipe.connections.get(direction).tank.setFluid(FluidStack.EMPTY);
            pipe.connections.get(direction).update();
            pipe.connections.get(direction).syncTanks();

            boolean hasAnyExtraction = false;
            for (Direction i : Direction.values()) {
                if ((state.getValue(connections.get(i)) == ConnectionState.CONNECTED || state.getValue(connections.get(i)) == ConnectionState.EXTRACTION) && !(pipe.connections.get(i).neighborFluidHandler() instanceof PipeConnection))
                    hasAnyExtraction = true;
            }
            if (!hasAnyExtraction) {
                state = pipe.setExtractionMode(state, false);
            }
        }

        return state;
    }

    //@Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityPipe::tick;
    }
}