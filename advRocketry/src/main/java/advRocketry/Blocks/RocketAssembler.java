package advRocketry.Blocks;

import advRocketry.BlockEntities.EntityRocketAssembler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static advRocketry.Registry.BlockEntities.ENTITY_ROCKET_ASSEMBLER;

public class RocketAssembler extends Block implements EntityBlock {

    public RocketAssembler() {
        super(Properties.of()
            .destroyTime(2.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.ANVIL)
        );
        registerDefaultState(getStateDefinition().any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    // when a launch pad structure is placed or removed, trigger a rescan of all nearby rocket assembling machines
    public static void propagateScanRequestToMaster(Set<BlockPos> completed, BlockPos current, Level level) {
        if (completed.contains(current)) return;
        if (level.isClientSide) return;
        completed.add(current);

        if (level.getBlockEntity(current) instanceof EntityRocketAssembler rocketAssembler) {
            rocketAssembler.scanArea();
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos next = new BlockPos(current.offset(x, y, z));
                    Block nextBlock = level.getBlockState(next).getBlock();
                    if (
                            nextBlock instanceof LaunchPad ||
                                    nextBlock instanceof StructureTower ||
                                    nextBlock instanceof RocketAssembler
                    ) {
                        if (!completed.contains(next))
                            propagateScanRequestToMaster(completed, next, level);
                    }
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_ROCKET_ASSEMBLER.get().create(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @javax.annotation.Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof EntityRocketAssembler rocketAssembler) {
            rocketAssembler.scanArea();
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EntityRocketAssembler rocketAssembler) {
            if (rocketAssembler.isRedstoneOutputActive()) {
                return 15;
            }
        }
        return 0;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof EntityRocketAssembler rocketAssembler)
            rocketAssembler.openGui();
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityRocketAssembler::tick;
    }
}
