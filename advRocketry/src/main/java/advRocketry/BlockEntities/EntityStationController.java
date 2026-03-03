package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleSlider;
import ARLib.gui.modules.guiModuleText;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Dimension.SpaceStationDimensionProperties;
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

    GuiHandlerBlockEntity guiHandler;

    SpaceStationDimensionProperties.RotationMode directionMode = SpaceStationDimensionProperties.RotationMode.disabled;
    guiModuleButton directionModeButton;
    guiModuleSlider yawRelative;
    guiModuleSlider rollRelative;
    guiModuleSlider pitchRelative;
    guiModuleSlider yawAbsolute;
    guiModuleSlider rollAbsolute;
    guiModuleSlider pitchAbsolute;
    guiModuleSlider distance;

    guiModuleText yawValueText;
    guiModuleText rollValueText;
    guiModuleText pitchValueText;
    guiModuleText distanceValueText;


    public EntityStationController(BlockPos pos, BlockState blockState) {
        super(ENTITY_STATION_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        int i = 10;

        guiModuleButton applyButton = new guiModuleButton(0, "apply", guiHandler, 10, 115, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        applyButton.color = 0xffaaaaaa;
        guiHandler.modules.add(applyButton);
        guiModuleButton revertButton = new guiModuleButton(1, "revert", guiHandler, 60, 115, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        revertButton.color = 0xffaaaaaa;
        guiHandler.modules.add(revertButton);
        guiModuleButton resetButton = new guiModuleButton(2, "reset", guiHandler, 110, 115, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        resetButton.color = 0xffaaaaaa;
        guiHandler.modules.add(resetButton);

        directionModeButton = new guiModuleButton(3, "relative", guiHandler, 90, 7, 60, 15, BTN_BLACK, BTN_W, BTN_H);
        directionModeButton.color = 0xffaaaaaa;
        guiHandler.modules.add(directionModeButton);
        guiModuleText directionModeText = new guiModuleText(i++, "rotation mode:", guiHandler, 10, 10, 0xff000000, false);
        guiHandler.modules.add(directionModeText);

        guiHandler.modules.add(new guiModuleText(i++, "yaw", guiHandler, 10, 30, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "roll", guiHandler, 10, 50, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "pitch", guiHandler, 10, 70, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "distance", guiHandler, 10, 90, 0xff000000, false));


        yawValueText = new guiModuleText(i++, "100°", guiHandler, 140, 30, 0xff000000, false);
        guiHandler.modules.add(yawValueText);
        rollValueText = new guiModuleText(i++, "-120°", guiHandler, 140, 50, 0xff000000, false);
        guiHandler.modules.add(rollValueText);
        pitchValueText = new guiModuleText(i++, "6°", guiHandler, 140, 70, 0xff000000, false);
        guiHandler.modules.add(pitchValueText);
        distanceValueText = new guiModuleText(i++, "12345km", guiHandler, 140, 90, 0xff000000, false);
        guiHandler.modules.add(distanceValueText);


        yawRelative = new guiModuleSlider(i++, guiHandler, 60, 30, 70, 10);
        guiHandler.modules.add(yawRelative);
        rollRelative = new guiModuleSlider(i++, guiHandler, 60, 50, 70, 10);
        guiHandler.modules.add(rollRelative);
        pitchRelative = new guiModuleSlider(i++, guiHandler, 60, 70, 70, 10);
        guiHandler.modules.add(pitchRelative);


        yawAbsolute = new guiModuleSlider(i++, guiHandler, 60, 30, 70, 10);
        guiHandler.modules.add(yawAbsolute);
        rollAbsolute = new guiModuleSlider(i++, guiHandler, 60, 50, 70, 10);
        guiHandler.modules.add(rollAbsolute);
        pitchAbsolute = new guiModuleSlider(i++, guiHandler, 60, 70, 70, 10);
        guiHandler.modules.add(pitchAbsolute);

        distance = new guiModuleSlider(i++, guiHandler, 60, 90, 70, 10);
        guiHandler.modules.add(distance);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStationController) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateGui();
    }

    public void updateGui() {
        directionModeButton.setTextAndSync(directionMode.toString());
        boolean isRalativeRotation = directionMode == SpaceStationDimensionProperties.RotationMode.relative;
        boolean isAbsoluteRotation = directionMode == SpaceStationDimensionProperties.RotationMode.absolute;
        boolean isAbsoluteOrRelative = isRalativeRotation || isAbsoluteRotation;
        yawRelative.setIsEnabledAndBroadcastUpdate(isRalativeRotation);
        yawAbsolute.setIsEnabledAndBroadcastUpdate(isAbsoluteRotation);
        pitchRelative.setIsEnabledAndBroadcastUpdate(isRalativeRotation);
        pitchAbsolute.setIsEnabledAndBroadcastUpdate(isAbsoluteRotation);
        rollRelative.setIsEnabledAndBroadcastUpdate(isRalativeRotation);
        rollAbsolute.setIsEnabledAndBroadcastUpdate(isAbsoluteRotation);
        if (isRalativeRotation) {
            yawValueText.setTextAndSync((yawRelative.value * 360 - 180) + "°");
            rollValueText.setTextAndSync((rollRelative.value * 360 - 180) + "°");
            pitchValueText.setTextAndSync((pitchRelative.value * 360 - 180) + "°");
        } else if (isAbsoluteRotation) {
            yawValueText.setTextAndSync((yawAbsolute.value * 360 - 180) + "°");
            rollValueText.setTextAndSync((rollAbsolute.value * 360 - 180) + "°");
            pitchValueText.setTextAndSync((pitchAbsolute.value * 360 - 180) + "°");
        }
        yawValueText.setIsEnabledAndBroadcastUpdate(isAbsoluteOrRelative);
        rollValueText.setIsEnabledAndBroadcastUpdate(isAbsoluteOrRelative);
        pitchValueText.setIsEnabledAndBroadcastUpdate(isAbsoluteOrRelative);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
            if(myDim instanceof SpaceStationDimension spaceStationDimension) {
                if (btn == 0) {
                    // apply
                    spaceStationDimension.properties()
                }
                if (btn == 1) {
                    // revert
                }
                if (btn == 2) {
                    // reset
                }
                if (btn == 3) {
                    // change rotation mode
                }
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("directionMode", directionMode.ordinal());
        tag.putDouble("distance", distance.value);
        tag.putDouble("yawRelative", yawRelative.value);
        tag.putDouble("rollRelative", rollRelative.value);
        tag.putDouble("pitchRelative", pitchRelative.value);
        tag.putDouble("yawAbsolute", yawAbsolute.value);
        tag.putDouble("rollAbsolute", rollAbsolute.value);
        tag.putDouble("pitchAbsolute", pitchAbsolute.value);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        directionMode = SpaceStationDimensionProperties.RotationMode.values()[tag.getInt("directionMode")];
        distance.value = tag.getDouble("distance");
        yawRelative.value = tag.getDouble("yawRelative");
        rollRelative.value = tag.getDouble("rollRelative");
        pitchRelative.value = tag.getDouble("pitchRelative");
        yawAbsolute.value = tag.getDouble("yawAbsolute");
        rollAbsolute.value = tag.getDouble("rollAbsolute");
        pitchAbsolute.value = tag.getDouble("pitchAbsolute");
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(190, 140, true);
    }
}
