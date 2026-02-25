package advRocketry.BlockEntities;

import ARLib.blockentities.EntityFluidInputBlock;
import ARLib.blocks.BlockFluidInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import advRocketry.Oxygen.OxygenSupplier;
import advRocketry.Oxygen.OxygenSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static advRocketry.Registry.ENTITY_OXYGEN_VENT;


public class EntityOxygenVent extends EntityFluidInputBlock {

    OxygenSupplier oxygenSupplier;

    public EntityOxygenVent(BlockPos pos, BlockState blockState) {
        super(ENTITY_OXYGEN_VENT.get(), pos, blockState);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityOxygenVent) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        oxygenSupplier = new OxygenSupplier(level, getBlockPos());
        OxygenSystem.registerOxygenSupplier(level, oxygenSupplier);

    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        OxygenSystem.removeOxygenSupplier(level, oxygenSupplier);
    }

    public void tick() {
        super.tick();
    }
}
