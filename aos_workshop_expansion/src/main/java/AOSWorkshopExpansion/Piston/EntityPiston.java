package AOSWorkshopExpansion.Piston;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import static AOSWorkshopExpansion.Registry.ENTITY_PISTON;

public class EntityPiston extends BlockEntity {
    public EntityPiston(BlockPos pos, BlockState blockState) {
        super(ENTITY_PISTON.get(), pos, blockState);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        //((EntityPiston) t).tick();
    }

}
