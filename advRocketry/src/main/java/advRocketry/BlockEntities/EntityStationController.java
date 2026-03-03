package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleSlider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static advRocketry.Registry.ENTITY_STATION_CONTROLLER;

public class EntityStationController extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleSlider yawSlider;

    public EntityStationController(BlockPos pos, BlockState blockState) {
        super(ENTITY_STATION_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        yawSlider = new guiModuleSlider(0,guiHandler,10,10,50,10) {
            @Override
            protected void onValueChanged(double value) {
                System.out.println(value);
            }
        };
        guiHandler.modules.add(yawSlider);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
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
        if (!level.isClientSide) {
            guiHandler.serverTick();
            yawSlider.setValueAndSync(0.9);
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStationController) t).tick();
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 140, true);
    }
}
