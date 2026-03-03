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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_STATION_CONTROLLER;

public class EntityStationController extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    GuiHandlerBlockEntity guiHandler;

    guiModuleSlider x;
    guiModuleSlider y;
    guiModuleSlider z;
    guiModuleSlider distance;
    guiModuleText distanceValueText;


    public EntityStationController(BlockPos pos, BlockState blockState) {
        super(ENTITY_STATION_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        int i = 10;

        guiModuleText title = new guiModuleText(-1, "Station Controller", guiHandler, 5, 5, 0xff000000, false);
        guiHandler.modules.add(title);

        guiModuleButton applyButton = new guiModuleButton(0, "apply", guiHandler, 10, 125, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        applyButton.color = 0xffaaaaaa;
        guiHandler.modules.add(applyButton);
        guiModuleButton revertButton = new guiModuleButton(1, "revert", guiHandler, 60, 125, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        revertButton.color = 0xffaaaaaa;
        guiHandler.modules.add(revertButton);
        guiModuleButton resetButton = new guiModuleButton(2, "reset", guiHandler, 110, 125, 40, 15, BTN_BLACK, BTN_W, BTN_H);
        resetButton.color = 0xffaaaaaa;
        guiHandler.modules.add(resetButton);

        guiModuleButton setFrontButton = new guiModuleButton(3, "set front", guiHandler, 10, 20, 160, 15, BTN_BLACK, BTN_W, BTN_H);
        setFrontButton.color = 0xffaaaaaa;
        guiHandler.modules.add(setFrontButton);

        guiHandler.modules.add(new guiModuleText(i++, "orbitX", guiHandler, 10, 45, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "orbitY", guiHandler, 10, 65, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "orbitZ", guiHandler, 10, 85, 0xff000000, false));
        guiHandler.modules.add(new guiModuleText(i++, "distance", guiHandler, 10, 105, 0xff000000, false));


        x = new guiModuleSlider(90, guiHandler, 60, 45, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                fixOrientationAxis(0);
                setChanged();
            }
        };
        guiHandler.modules.add(x);
        y = new guiModuleSlider(91, guiHandler, 60, 65, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                fixOrientationAxis(1);
                setChanged();
            }
        };
        guiHandler.modules.add(y);
        z = new guiModuleSlider(92, guiHandler, 60, 85, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                fixOrientationAxis(2);
                setChanged();
            }
        };
        guiHandler.modules.add(z);

        distance = new guiModuleSlider(i++, guiHandler, 60, 105, 70, 10) {
            public void onValueChangeReceivedOnServer(double value) {
                updateGuiDistanceText();
                setChanged();
            }
        };
        guiHandler.modules.add(distance);
        distanceValueText = new guiModuleText(i++, "", guiHandler, 140, 105, 0xff000000, false);
        guiHandler.modules.add(distanceValueText);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStationController) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide)
            updateGuiDistanceText();
    }

    public void updateGuiDistanceText() {
        distanceValueText.setTextAndSync(Math.round(distance.value * 100) + "%");
    }

    public Vec3 createCorrectAxis(Vec3 axis, int fixed) {
        if (axis.length() < 0.001)
            return new Vec3(0, 1, 0);

        double x = axis.x;
        double y = axis.y;
        double z = axis.z;

        switch (fixed) {
            case 0: // X is fixed
                x = Math.max(-1.0, Math.min(1.0, x)); // Clip to [-1, 1]
                double targetLenSqX = 1.0 - (x * x);
                double currentLenSqX = (y * y) + (z * z);

                // Edge case: If the other values are essentially 0, we have to invent a direction
                if (currentLenSqX < 0.000001) {
                    y = Math.sqrt(targetLenSqX);
                    z = 0.0;
                } else {
                    double scaleX = Math.sqrt(targetLenSqX / currentLenSqX);
                    y *= scaleX;
                    z *= scaleX;
                }
                break;

            case 1: // Y is fixed
                y = Math.max(-1.0, Math.min(1.0, y)); // Clip to [-1, 1]
                double targetLenSqY = 1.0 - (y * y);
                double currentLenSqY = (x * x) + (z * z);

                if (currentLenSqY < 0.000001) {
                    x = Math.sqrt(targetLenSqY);
                    z = 0.0;
                } else {
                    double scaleY = Math.sqrt(targetLenSqY / currentLenSqY);
                    x *= scaleY;
                    z *= scaleY;
                }
                break;

            case 2: // Z is fixed
                z = Math.max(-1.0, Math.min(1.0, z)); // Clip to [-1, 1]
                double targetLenSqZ = 1.0 - (z * z);
                double currentLenSqZ = (x * x) + (y * y);

                if (currentLenSqZ < 0.000001) {
                    x = Math.sqrt(targetLenSqZ);
                    y = 0.0;
                } else {
                    double scaleZ = Math.sqrt(targetLenSqZ / currentLenSqZ);
                    x *= scaleZ;
                    y *= scaleZ;
                }
                break;

            default:
                // If an invalid 'fixed' value is passed, just normalize normally as a fallback
                return axis.normalize();
        }

        return new Vec3(x, y, z);
    }

    public void fixOrientationAxis(int fixed) {
        Vec3 correct = createCorrectAxis(new Vec3(x.value * 2 - 1, y.value * 2 - 1, z.value * 2 - 1), fixed);
        x.setValueAndSync((correct.x + 1) / 2);
        y.setValueAndSync((correct.y + 1) / 2);
        z.setValueAndSync((correct.z + 1) / 2);
        System.out.println(correct);
    }

    public void reset() {
        x.setValueAndSync(0.5);
        y.setValueAndSync(1);
        z.setValueAndSync(0.5);
        distance.setValueAndSync(0.2);
        updateGuiDistanceText();
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
                    spaceStationDimension.setTargetOrbitAxis(new Vec3(x.value * 2 - 1, y.value * 2 - 1, z.value * 2 - 1));
                    spaceStationDimension.setTargetOrbitDistance((float) distance.value);
                    guiHandler.signalCloseGui(serverPlayer);
                }
                if (btn == 1) {
                    // revert
                    Vec3 orbitAxis = spaceStationDimension.getTargetOrbitAxis();
                    x.setValueAndSync((orbitAxis.x + 1) / 2);
                    y.setValueAndSync((orbitAxis.y + 1) / 2);
                    z.setValueAndSync((orbitAxis.z + 1) / 2);
                    distance.setValueAndSync(spaceStationDimension.getTargetOrbitDistance());
                }
                if (btn == 2) {
                    // reset
                    reset();
                }
                if (btn == 3) {
                    // set front facing
                    spaceStationDimension.setFrontFacing(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
                }
                updateGuiDistanceText();
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
        tag.putDouble("distance", distance.value);
        tag.putDouble("x", x.value);
        tag.putDouble("y", y.value);
        tag.putDouble("z", z.value);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        distance.value = tag.getDouble("distance");
        x.value = tag.getDouble("x");
        y.value = tag.getDouble("y");
        z.value = tag.getDouble("z");
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(190, 150, true);
    }
}
