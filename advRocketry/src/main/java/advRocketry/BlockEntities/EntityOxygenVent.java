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
    LifeSupportSupplier heatSupplier;
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
        oxygenSupplier = new LifeSupportSupplier(level, getBlockPos()){
            @Override
            public LifeSupportSystem.LifeSupportType getType() {
                return LifeSupportSystem.LifeSupportType.OXYGEN_SUPPLIER;
            }
        };
        heatSupplier = new LifeSupportSupplier(level, getBlockPos()){
            @Override
            public LifeSupportSystem.LifeSupportType getType() {
                return LifeSupportSystem.LifeSupportType.HEAT_SUPPLIER;
            }
        };
        LifeSupportSystem.registerLifeSupportSupplier(level, oxygenSupplier);
        LifeSupportSystem.registerLifeSupportSupplier(level, heatSupplier);

    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LifeSupportSystem.removeLifeSupportSupplier(level, oxygenSupplier);
        LifeSupportSystem.removeLifeSupportSupplier(level, heatSupplier);
    }

    public void tick() {

    }
}
