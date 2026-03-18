package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import static advRocketry.Registry.BlockEntities.ENTITY_CARGO_HOLD;
import static advRocketry.Registry.BlockEntities.ENTITY_FLUID_RELEASE;

public class EntityFluidRelease extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public FluidTank tank;

    public EntityFluidRelease(BlockPos pos, BlockState blockState) {
        super(ENTITY_FLUID_RELEASE.get(), pos, blockState);
        tank = new FluidTank(10000){
            @Override
            public void onContentsChanged(){
                setChanged();
            }
        };
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    public void tick() {

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityFluidRelease) t).tick();
    }

}
