package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleSlider;
import ARLib.gui.modules.guiModuleText;
import advRocketry.Dimension.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_ORIENTATION_CONTROLLER;

public class EntityOrientationController extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    GuiHandlerBlockEntity guiHandler;

    SpaceStationDimensionProperties.RotationMode rotationMode = SpaceStationDimensionProperties.RotationMode.disabled;
    guiModuleButton directionModeButton;
    guiModuleSlider yawRelative;
    guiModuleSlider rollRelative;
    guiModuleSlider pitchRelative;
    guiModuleSlider yawAbsolute;
    guiModuleSlider rollAbsolute;
    guiModuleSlider pitchAbsolute;

    guiModuleText yawValueText;
    guiModuleText rollValueText;
    guiModuleText pitchValueText;


    public EntityOrientationController(BlockPos pos, BlockState blockState) {
        super(ENTITY_ORIENTATION_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        int i = 10;

        guiModuleText title = new guiModuleText(-1, "Orientation Controller", guiHandler, 5,5,0xff000000,false);
        guiHandler.modules.add(title);

        guiModuleButton applyButton = new guiModuleButton(0, "apply", guiHandler, 10, 100, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        applyButton.color = 0xffaaaaaa;
        guiHandler.modules.add(applyButton);
        guiModuleButton revertButton = new guiModuleButton(1, "revert", guiHandler, 60, 100, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        revertButton.color = 0xffaaaaaa;
        guiHandler.modules.add(revertButton);
        guiModuleButton resetButton = new guiModuleButton(2, "reset", guiHandler, 110, 100, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        resetButton.color = 0xffaaaaaa;
        guiHandler.modules.add(resetButton);

        directionModeButton = new guiModuleButton(3, "relative", guiHandler, 90, 17, 60, 15, BTN_BLACK, BTN_W, BTN_H);
        directionModeButton.color = 0xffaaaaaa;
        guiHandler.modules.add(directionModeButton);
        guiModuleText directionModeText = new guiModuleText(i++, "rotation mode:", guiHandler, 10, 20, 0xff000000, false);
        guiHandler.modules.add(directionModeText);

        guiHandler.modules.add(new guiModuleText(i++, "yaw", guiHandler, 10, 40, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "roll", guiHandler, 10, 60, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "pitch", guiHandler, 10, 80, 0xff000000, false));

        yawValueText = new guiModuleText(i++, "100°", guiHandler, 140, 40, 0xff000000, false);
        guiHandler.modules.add(yawValueText);
        rollValueText = new guiModuleText(i++, "-120°", guiHandler, 140, 60, 0xff000000, false);
        guiHandler.modules.add(rollValueText);
        pitchValueText = new guiModuleText(i++, "6°", guiHandler, 140, 80, 0xff000000, false);
        guiHandler.modules.add(pitchValueText);


        yawRelative = new guiModuleSlider(i++, guiHandler, 60, 39, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGui();
                setChanged();
            }
        };
        guiHandler.modules.add(yawRelative);
        rollRelative = new guiModuleSlider(i++, guiHandler, 60, 59, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGui();
                setChanged();
            }
        };
        guiHandler.modules.add(rollRelative);
        pitchRelative = new guiModuleSlider(i++, guiHandler, 60, 79, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGui();
                setChanged();
            }
        };
        guiHandler.modules.add(pitchRelative);


        yawAbsolute = new guiModuleSlider(i++, guiHandler, 60, 39, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGui();
                setChanged();
            }
        };
        guiHandler.modules.add(yawAbsolute);
        rollAbsolute = new guiModuleSlider(i++, guiHandler, 60, 59, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGui();
                setChanged();
            }
        };
        guiHandler.modules.add(rollAbsolute);
        pitchAbsolute = new guiModuleSlider(i++, guiHandler, 60, 79, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGui();
                setChanged();
            }
        };
        guiHandler.modules.add(pitchAbsolute);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityOrientationController) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if(!level.isClientSide)
            updateGui();
    }

    String getRotText(double value) {
        return Math.round(value * 360 - 180) + "°";
    }

    public void updateGui() {
        // server side only
        directionModeButton.setTextAndSync(rotationMode.toString());
        boolean isRalativeRotation = rotationMode == SpaceStationDimensionProperties.RotationMode.relative;
        boolean isAbsoluteRotation = rotationMode == SpaceStationDimensionProperties.RotationMode.absolute;
        boolean isAbsoluteOrRelative = isRalativeRotation || isAbsoluteRotation;
        yawRelative.setIsEnabledAndBroadcastUpdate(isRalativeRotation);
        yawAbsolute.setIsEnabledAndBroadcastUpdate(isAbsoluteRotation);
        pitchRelative.setIsEnabledAndBroadcastUpdate(isRalativeRotation);
        pitchAbsolute.setIsEnabledAndBroadcastUpdate(isAbsoluteRotation);
        rollRelative.setIsEnabledAndBroadcastUpdate(isRalativeRotation);
        rollAbsolute.setIsEnabledAndBroadcastUpdate(isAbsoluteRotation);
        if (isRalativeRotation) {
            yawValueText.setTextAndSync(getRotText(yawRelative.value));
            rollValueText.setTextAndSync(getRotText(rollRelative.value));
            pitchValueText.setTextAndSync(getRotText(pitchRelative.value));
        } else if (isAbsoluteRotation) {
            yawValueText.setTextAndSync(getRotText(yawAbsolute.value));
            rollValueText.setTextAndSync(getRotText(rollAbsolute.value));
            pitchValueText.setTextAndSync(getRotText(pitchAbsolute.value));
        }
        yawValueText.setIsEnabledAndBroadcastUpdate(isAbsoluteOrRelative);
        rollValueText.setIsEnabledAndBroadcastUpdate(isAbsoluteOrRelative);
        pitchValueText.setIsEnabledAndBroadcastUpdate(isAbsoluteOrRelative);
    }

    public void reset() {
        if(rotationMode == SpaceStationDimensionProperties.RotationMode.absolute){
            yawAbsolute.setValueAndSync(0.5);
            rollAbsolute.setValueAndSync(0.5);
            pitchAbsolute.setValueAndSync(0.5);
        }
        if(rotationMode == SpaceStationDimensionProperties.RotationMode.relative) {
            yawRelative.setValueAndSync(0.5);
            rollRelative.setValueAndSync(0.5);
            pitchRelative.setValueAndSync(0.5);
        }
        updateGui();
        setChanged();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
            if (myDim instanceof SpaceStationDimension spaceStationDimension) {
                if (btn == 0) {
                    // apply
                    if (rotationMode == SpaceStationDimensionProperties.RotationMode.disabled)
                        spaceStationDimension.setRotationSettings(0, 0, 0, rotationMode);
                    if (rotationMode == SpaceStationDimensionProperties.RotationMode.absolute)
                        spaceStationDimension.setRotationSettings(yawAbsolute.value, rollAbsolute.value, pitchAbsolute.value, rotationMode);
                    if (rotationMode == SpaceStationDimensionProperties.RotationMode.relative)
                        spaceStationDimension.setRotationSettings(yawRelative.value, rollRelative.value, pitchRelative.value, rotationMode);
                    guiHandler.signalCloseGui(serverPlayer);
                }
                if (btn == 1) {
                    // revert
                    rotationMode = spaceStationDimension.getRotationMode();
                    Vec3 rotationSettings = spaceStationDimension.getRotationSettings();
                    if (rotationMode == SpaceStationDimensionProperties.RotationMode.absolute) {
                        yawAbsolute.setValueAndSync(rotationSettings.x);
                        rollAbsolute.setValueAndSync(rotationSettings.y);
                        pitchAbsolute.setValueAndSync(rotationSettings.z);
                    }
                    if (rotationMode == SpaceStationDimensionProperties.RotationMode.relative) {
                        yawRelative.setValueAndSync(rotationSettings.x);
                        rollRelative.setValueAndSync(rotationSettings.y);
                        pitchRelative.setValueAndSync(rotationSettings.z);
                    }
                }
                if (btn == 2) {
                    // reset
                    reset();
                }
                if (btn == 3) {
                    // change rotation mode
                    if (rotationMode == SpaceStationDimensionProperties.RotationMode.absolute)
                        rotationMode = SpaceStationDimensionProperties.RotationMode.relative;
                    else if (rotationMode == SpaceStationDimensionProperties.RotationMode.relative)
                        rotationMode = SpaceStationDimensionProperties.RotationMode.disabled;
                    else if (rotationMode == SpaceStationDimensionProperties.RotationMode.disabled)
                        rotationMode = SpaceStationDimensionProperties.RotationMode.absolute;
                }
                updateGui();
                setChanged();
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
        tag.putInt("directionMode", rotationMode.ordinal());
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
        rotationMode = SpaceStationDimensionProperties.RotationMode.values()[tag.getInt("directionMode")];
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
            guiHandler.openGui(190, 125, true);
    }
}
