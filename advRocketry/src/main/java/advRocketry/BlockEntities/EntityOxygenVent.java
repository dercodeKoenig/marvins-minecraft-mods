package advRocketry.BlockEntities;

import ARLib.blockentities.EntityFluidInputBlock;
import ARLib.blocks.BlockFluidInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import advRocketry.Dimension.OxygenSupplier;
import advRocketry.Dimension.OxygenSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import static advRocketry.Registry.ENTITY_OXYGEN_VENT;


public class EntityOxygenVent extends EntityFluidInputBlock {

    OxygenSupplier oxygenSupplier;

    public EntityOxygenVent(BlockPos pos, BlockState blockState) {
        super(ENTITY_OXYGEN_VENT.get(), pos, blockState);
    }

    @Override
    public void onLoad(){
        super.onLoad();
        oxygenSupplier = new OxygenSupplier(level, getBlockPos());
        OxygenSystem.registerOxygenSupplier(level.dimension().location(),oxygenSupplier);
    }
    @Override
    public void setRemoved(){
        super.setRemoved();
        OxygenSystem.removeOxygenSupplier(level.dimension().location(),oxygenSupplier);
    }

    public void tick() {
      super.tick();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityOxygenVent) t).tick();
    }
}
