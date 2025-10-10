package advRocketry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;

public class EntityRocketAssembler extends BlockEntity {
    public EntityRocketAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_ROCKET_ASSEMBLER.get(), pos, blockState);
    }
}
