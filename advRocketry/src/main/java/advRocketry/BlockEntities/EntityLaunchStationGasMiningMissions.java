package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import advRocketry.Data.DataTypes;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Items.ItemGalaxyDatabase;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Missions.MissionManager;
import advRocketry.Missions.RocketMission;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.Items;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketPrograms.ProgramAsteroidMiningMission;
import advRocketry.Rocket.RocketPrograms.ProgramGasMiningMission;
import advRocketry.Rocket.RocketPrograms.ProgramMissionStartBase;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;

public class EntityLaunchStationGasMiningMissions extends EntityLaunchStation {

    // TODO: add inventory back, requires galaxy database with full composition unlocked for mining!

    public static int gas_selector_btn_id = 600384;
    public UUID lastLaunchedMissionUUID = null;
    public UUID lastLaunchedRocketUUID = null;
    public guiModuleText statusText;
    public guiModuleButton gasSelector;
    public guiModuleButton launchButton;

    public EntityLaunchStationGasMiningMissions(BlockPos pos, BlockState blockState) {
        super(BlockEntities.ENTITY_LAUNCH_STATION_GAS_MINING_MISSIONS.get(), pos, blockState);
    }

    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);

        guiHandler.modules.add(new guiModuleText(0, "Gas Mining Mission Terminal", guiHandler, 5, 5, 0xff000000, false));

        launchButton = new guiModuleButton(launch_btn_id, "launch", guiHandler, 10, 20, 40, 15, BTN_GREEN, BTN_W, BTN_H);
        guiHandler.modules.add(launchButton);

        guiHandler.modules.add(new guiModuleItemHandlerSlot(1, inventory, 0, 0, 1, guiHandler, 7, 60));
        guiHandler.modules.add(new guiModuleItemStackRender(3, new ItemStack(Items.ITEM_GALAXY_DATABASE.get(), 1), 0.9f, guiHandler, 27, 60));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 150, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 90, 2000, 1, 0, guiHandler));

        gasSelector = new guiModuleButton(gas_selector_btn_id, "no gas selected", guiHandler, 60, 20, 100, 15, BTN_BLACK, BTN_W, BTN_H);
        gasSelector.color = 0xffffffff;
        guiHandler.modules.add(gasSelector);

        statusText = new guiModuleText(76967884, "status", guiHandler, 10, 45, 0xff000000, false);
        guiHandler.modules.add(statusText);
    }

    public SpaceStationDimension getSpaceStation() {
        Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (!(myDim instanceof SpaceStationDimension spaceStationDimension)) {
            return null;
        }
        return spaceStationDimension;
    }

    public Pair<Boolean, String> canMine() {
        SpaceStationDimension spaceStationDimension = getSpaceStation();
        if (spaceStationDimension == null) {
            return Pair.of(false, "requires space station");
        }
        Dimension parentDim = DimensionManager.INSTANCE_SERVER.get(spaceStationDimension.getParentDimensionId());
        if (!(parentDim instanceof PlanetDimension parentPlanet) || !spaceStationDimension.isInOrbit()) {
            return Pair.of(false, "not in orbit");
        }

        ItemStack stack = inventory.getStackInSlot(0);
        if (!(stack.getItem() instanceof ItemGalaxyDatabase)) {
            return Pair.of(false, "missing database");
        }
        ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(stack, parentPlanet);
        int composition = 0;
        if (info != null)
            composition = info.get(DataTypes.composition);
        int pointsRequired = ItemGalaxyDatabase.POINTS_UNLOCKED(parentPlanet);
        if (composition < pointsRequired) {
            return Pair.of(false, "composition data: " + composition + " / " + pointsRequired);
        }


        Set<String> options = parentPlanet.getGasMiningOptions();
        String selected = gasSelector.text;
        if (options.isEmpty()) {
            return Pair.of(false, "gas mining not possible here");
        } else if (!options.contains(selected)) {
            return Pair.of(false, "selected gas not found here");
        }
        return Pair.of(true, selected);
    }

    public void updateGuiText() {

        if (level instanceof ServerLevel serverLevel && !guiHandler.playersTrackingGui.isEmpty()) {
            if (lastLaunchedRocketUUID != null) {
                if (serverLevel.getEntity(lastLaunchedRocketUUID) instanceof EntityRocket rocket && rocket.currentProgram instanceof ProgramGasMiningMission programGasMiningMission) {
                    statusText.setTextAndSync("starting gas mining\nTarget: " + programGasMiningMission.targetGas);
                } else {
                    // no longer valid program
                    lastLaunchedRocketUUID = null;
                }
            } else if (lastLaunchedMissionUUID != null) {
                if (MissionManager.missions.get(lastLaunchedMissionUUID) instanceof RocketMission runningMission) {
                    int eta = (int) (runningMission.completeTime - GlobalTime.getGlobalTime()) / 20;
                    statusText.setTextAndSync("Mission in progress, eta: " + eta + "s");
                } else {
                    lastLaunchedMissionUUID = null;
                }
            } else {
                Pair<Boolean, String> canMine = canMine();
                if (!canMine.getFirst()) {
                    statusText.setTextAndSync(canMine.getSecond());
                    launchButton.setBackgroundAndSync(BTN_RED, BTN_W, BTN_H);
                }else {
                    launchButton.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                    statusText.setTextAndSync("status: ready for gas mining");
                }
            }
        }
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof ItemGalaxyDatabase;
    }

    public void openGui() {
        guiHandler.openGui(176, 177, true);
    }


    public boolean launch() {
        Pair<Boolean, String> canMine = canMine();
        if (!canMine.getFirst())
            return false;

        if (linkedRocket != null && linkedRocket.currentProgram == null) {
            lastLaunchedMissionUUID = UUID.randomUUID();
            BlockPos landPos = linkedRocket.getDockingStationPos();
            if (landPos == null) landPos = linkedRocket.blockPosition();
            ProgramMissionStartBase programMissionStartBase = new ProgramGasMiningMission(linkedRocket, canMine.getSecond(), getSpaceStation().getParentDimensionId(), level.dimension().location(), landPos, lastLaunchedMissionUUID);
            linkedRocket.setProgramAndSync(programMissionStartBase);
            lastLaunchedRocketUUID = linkedRocket.getUUID();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level.isClientSide && !guiHandler.playersTrackingGui.isEmpty()) {
            updateGuiText();
        }
    }

    public void cycleSelection() {
        Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (myDim instanceof SpaceStationDimension spaceStation) {
            Dimension parentDim = DimensionManager.INSTANCE_SERVER.get(spaceStation.getParentDimensionId());
            if (parentDim instanceof PlanetDimension parentPlanet && spaceStation.isInOrbit()) {

                // only allow selection of gas when composition is unlocked to not spoiler the composition
                ItemStack stack = inventory.getStackInSlot(0);
                if (!(stack.getItem() instanceof ItemGalaxyDatabase)) {
                    gasSelector.setTextAndSync("no gas selected");
                    return;
                }
                ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(stack, parentPlanet);
                if (info == null) {
                    gasSelector.setTextAndSync("no gas selected");
                    return;
                }
                int composition = info.get(DataTypes.composition);
                int pointsRequired = ItemGalaxyDatabase.POINTS_UNLOCKED(parentPlanet);
                if (composition < pointsRequired) {
                    gasSelector.setTextAndSync("no gas selected");
                    return;
                }


                // 1. Fetch the available options
                List<String> options = new ArrayList<>(parentPlanet.getGasMiningOptions());
                // 2. Handle the empty case
                if (options.isEmpty()) {
                    gasSelector.setTextAndSync("no gas selected");
                    return;
                }
                // 3. Sort to ensure "cycling" isn't random every time the function runs
                Collections.sort(options);
                // 4. Find where our current string sits in the list
                int currentIndex = options.indexOf(gasSelector.text);
                // 5. If not found, or if it's the last item, wrap to the start (index 0)
                // Otherwise, move to the next index
                int nextIndex = (currentIndex == -1 || currentIndex == options.size() - 1)
                        ? 0
                        : currentIndex + 1;

                String next = options.get(nextIndex);
                gasSelector.setTextAndSync(next);
            }
        }
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        super.readServer(compoundTag, serverPlayer);

        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            if (btn == gas_selector_btn_id) {
                // cycle gas mining selection
                cycleSelection();
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (lastLaunchedMissionUUID != null) tag.putUUID("lastLaunchedMissionUUID", lastLaunchedMissionUUID);
        if (lastLaunchedRocketUUID != null) tag.putUUID("lastLaunchedRocketUUID", lastLaunchedRocketUUID);
        tag.putString("gasSelector", gasSelector.text);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("lastLaunchedMissionUUID")) lastLaunchedMissionUUID = tag.getUUID("lastLaunchedMissionUUID");
        if (tag.contains("lastLaunchedRocketUUID")) lastLaunchedRocketUUID = tag.getUUID("lastLaunchedRocketUUID");
        gasSelector.setTextAndSync(tag.getString("gasSelector"));
    }
}
