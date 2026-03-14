package advRocketry.BlockEntities;

import ARLib.utils.BlockEntityBattery;
import ARLib.utils.SimpleFluidContainer;
import advRocketry.LifeSupport.LifeSupportSupplier;
import advRocketry.LifeSupport.LifeSupportSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static advRocketry.Registry.BlockEntities.ENTITY_OXYGEN_VENT;


public class EntityOxygenVent extends BlockEntity {

    LifeSupportSupplier oxygenSupplier;
    public BlockEntityBattery battery;

    SimpleFluidContainer fluidContainer;
    // TODO: THIS ALL HERE IS NOT COMPLETE

    public EntityOxygenVent(BlockPos pos, BlockState blockState) {
        super(ENTITY_OXYGEN_VENT.get(), pos, blockState);
        battery = new BlockEntityBattery(this, 10000);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityOxygenVent) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        oxygenSupplier = new LifeSupportSupplier(level, getBlockPos());
        LifeSupportSystem.registerLifeSupportSupplier(level, oxygenSupplier, LifeSupportSystem.LifeSupportType.OXYGEN_SUPPLIER);

    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LifeSupportSystem.removeLifeSupportSupplier(level, oxygenSupplier, LifeSupportSystem.LifeSupportType.OXYGEN_SUPPLIER);
    }

    public void tick() {

    }
}
