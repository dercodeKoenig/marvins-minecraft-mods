package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleSlider;
import ARLib.gui.modules.guiModuleText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_STATION_CONTROLLER;

public class EntityStationController extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleSlider yawRelative;
    public guiModuleSlider rollRelative;
    public guiModuleSlider pitchRelative;
    public guiModuleSlider yawAbsolute;
    public guiModuleSlider rollAbsolute;
    public guiModuleSlider pitchAbsolute;

    public EntityStationController(BlockPos pos, BlockState blockState) {
        super(ENTITY_STATION_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        int i = 0;

        guiModuleButton applyButton = new guiModuleButton(i++, "apply", guiHandler, 10, 95, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(applyButton);
        guiModuleButton revertButton = new guiModuleButton(i++, "revert", guiHandler, 60, 95, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(revertButton);
        guiModuleButton resetButton = new guiModuleButton(i++, "reset", guiHandler, 110, 95, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(resetButton);

        guiModuleButton directionModeButton = new guiModuleButton(i++, "relative", guiHandler, 90, 7, 60, 15, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(directionModeButton);
        guiModuleText directionModeText = new guiModuleText(i++, "rotation mode:", guiHandler, 10, 10, 0xff000000, false);
        guiHandler.modules.add(directionModeText);

        guiModuleText yawText = new guiModuleText(i++, "yaw", guiHandler, 10, 30, 0xff000000, false);
        guiHandler.modules.add(yawText);
        guiModuleText rollText = new guiModuleText(i++, "roll", guiHandler, 10, 50, 0xff000000, false);
        guiHandler.modules.add(rollText);
        guiModuleText pitchText = new guiModuleText(i++, "pitch", guiHandler, 10, 70, 0xff000000, false);
        guiHandler.modules.add(pitchText);


        yawRelative = new guiModuleSlider(i++, guiHandler, 40, 30, 50, 10);
        guiHandler.modules.add(yawRelative);
        rollRelative = new guiModuleSlider(i++, guiHandler, 40, 50, 50, 10);
        guiHandler.modules.add(rollRelative);
        pitchRelative = new guiModuleSlider(i++, guiHandler, 40, 70, 50, 10);
        guiHandler.modules.add(pitchRelative);
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
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStationController) t).tick();
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(180, 120, true);
    }
}
