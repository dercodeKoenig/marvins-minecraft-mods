package AOSWorkshopExpansion.Piston;

import AOSWorkshopExpansion.Conveyor.ConveyorConfig;
import AgeOfSteam.Items.Hammer.ItemHammer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static AOSWorkshopExpansion.Registry.ENTITY_PISTON;

public class Piston extends Block implements EntityBlock, ItemHammer.HammerInteractionBlock {

    public static EnumProperty<SpecialFacing> SPECIALFACING = EnumProperty.create("facing", SpecialFacing.class);

    public enum SpecialFacing implements StringRepresentable {
        UP(Direction.UP, "up"), UP2(Direction.UP, "up2"),
        DOWN(Direction.DOWN, "down"), DOWN2(Direction.DOWN, "down2"),
        EAST(Direction.EAST, "east"),
        WEST(Direction.WEST, "west"),
        NORTH(Direction.NORTH, "north"),
        SOUTH(Direction.SOUTH, "south");


        public Direction direction;
        public String name;

        SpecialFacing(Direction direction, String name) {
            this.direction = direction;
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
        public static SpecialFacing fromDirection(Direction d){
            if(d == Direction.UP) return UP;
            if(d == Direction.DOWN) return DOWN;
            if(d == Direction.EAST) return EAST;
            if(d == Direction.WEST) return WEST;
            if(d == Direction.NORTH) return NORTH;
            if(d == Direction.SOUTH) return SOUTH;
            return null;
        }
    }

    public static BooleanProperty STATE1 = BooleanProperty.create("state1");


    public Piston() {
        super(Properties.of().noOcclusion().dynamicShape());
        BlockState state = this.stateDefinition.any();
        state = state.setValue(SPECIALFACING, SpecialFacing.NORTH).setValue(STATE1, false);
        this.registerDefaultState(state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Max Stress: " + PistonConfig.INSTANCE.maxStress));
        tooltipComponents.add(Component.literal("Friction: " + PistonConfig.INSTANCE.baseResistance));
        tooltipComponents.add(Component.literal("Friction per block: " + PistonConfig.INSTANCE.perBlockResistance));
        tooltipComponents.add(Component.literal("Inertia: " + PistonConfig.INSTANCE.inertia));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_PISTON.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPECIALFACING);
        builder.add(STATE1);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            SpecialFacing facing = SpecialFacing.fromDirection(player.getNearestViewDirection().getOpposite());
            Direction.Axis horizontalAxis = player.getDirection().getAxis();
            if(facing == SpecialFacing.UP && horizontalAxis == Direction.Axis.Z)
                facing = SpecialFacing.UP2;
            if(facing == SpecialFacing.DOWN && horizontalAxis == Direction.Axis.Z)
                facing = SpecialFacing.DOWN2;
            state = state.setValue(SPECIALFACING, facing);
            level.setBlock(pos, state, 3);
        }
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityPiston::tick;
    }


    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(SPECIALFACING).direction;
        return Shapes.create(
                new AABB(0,0,0,1,1,1)
                        .expandTowards(
                                facing.getStepX()*0.5f,
                                facing.getStepY()*0.5f,
                                facing.getStepZ()*0.5f)
        );
    }

    @Override
    public InteractionResult onHammer(ItemStack itemStack, Level level, BlockPos blockPos, BlockState blockState, Player player, InteractionHand interactionHand) {
        SpecialFacing facing = blockState.getValue(SPECIALFACING);
        if(facing == SpecialFacing.UP2)
            level.setBlock(blockPos, blockState.setValue(SPECIALFACING, SpecialFacing.UP),3);
        if(facing == SpecialFacing.UP)
            level.setBlock(blockPos, blockState.setValue(SPECIALFACING, SpecialFacing.UP2),3);
        if(facing == SpecialFacing.DOWN2)
            level.setBlock(blockPos, blockState.setValue(SPECIALFACING, SpecialFacing.DOWN),3);
        if(facing == SpecialFacing.DOWN)
            level.setBlock(blockPos, blockState.setValue(SPECIALFACING, SpecialFacing.DOWN2),3);
        return InteractionResult.SUCCESS;
    }
}
