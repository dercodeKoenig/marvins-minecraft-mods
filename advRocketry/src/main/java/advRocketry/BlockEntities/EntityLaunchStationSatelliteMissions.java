package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.LaunchStation;
import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Items.ItemSolarPanel;
import advRocketry.Missions.*;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.Items;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketPrograms.ProgramMissionStartBase;
import advRocketry.Rocket.RocketPrograms.ProgramSatelliteDeployment;
import advRocketry.Rocket.RocketPrograms.ProgramSatelliteRecovery;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.BlockEntities.ENTITY_LAUNCH_STATION;

public class EntityLaunchStationSatelliteMissions extends EntityLaunchStation {

    // TODO: maybe make option to recover asteroids by planet chip without asteroid id chip ?

    static double maxRangeForMission = 10;

    UUID lastLaunchedMissionUUID = null;
    UUID lastLaunchedRocketUUID = null;
    guiModuleText statusText;

    public EntityLaunchStationSatelliteMissions(BlockPos pos, BlockState blockState) {
        super(BlockEntities.ENTITY_LAUNCH_STATION_SATELLITE_MISSIONS.get(), pos, blockState);
    }

    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);

        guiHandler.modules.add(new guiModuleText(0, "Satellite Launch Station", guiHandler, 5, 5, 0xff000000, false));

        guiHandler.modules.add(new guiModuleItemHandlerSlot(1, inventory, 0, 0, 1, guiHandler, 50, 20));
        guiHandler.modules.add(new guiModuleItemStackRender(2, new ItemStack(Items.ITEM_PLANET_ID_CHIP.get(), 1), 0.9f, guiHandler, 32, 20));
        guiHandler.modules.add(new guiModuleItemStackRender(3, new ItemStack(Items.ITEM_SATELLITE_ID_CHIP.get(), 1), 0.9f, guiHandler, 70, 20));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 2000, 1, 0, guiHandler));

        guiModuleButton launchButton = new guiModuleButton(launch_btn_id, "launch", guiHandler, 90, 20, 40, 15, BTN_GREEN, BTN_W, BTN_H);
        guiHandler.modules.add(launchButton);

        statusText = new guiModuleText(76988794, "status", guiHandler, 10, 50, 0xff000000, false);
        guiHandler.modules.add(statusText);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemPlanetIdChip) return true;
        if (stack.getItem() instanceof ItemSatelliteIdChip) return true;
        return false;
    }

    public void openGui() {
        guiHandler.openGui(176, 165, true);
    }

    public long getMissionDuration(double distanceToTarget) {
        long duration = 20 * 30; // base time
        double extraSecond = Math.max(distanceToTarget, 0) / Config.INSTANCE.rocket_SpaceTravel_AU_Per_Second;
        duration += (long) (20 * extraSecond * 3); // extra time for moving to long distance planets
        return duration;
    }

    public double distanceToTarget(ItemStack navigationItem) {
        Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (myDim == null)
            return -1;
        if (navigationItem.getItem() instanceof ItemSatelliteIdChip) {
            Satellite sat = SatelliteManager.getSatellite(ItemSatelliteIdChip.getTarget(navigationItem));
            if (sat == null)
                return -1;
            if (DimensionManager.INSTANCE_SERVER.get(sat.parentDimensionId) instanceof Dimension dim) {
                return dim.getPosition(0).distanceTo(myDim.getPosition(0));
            }
        }
        if (navigationItem.getItem() instanceof ItemPlanetIdChip) {
            ResourceLocation targetPlanet = ItemPlanetIdChip.getSelectedDimension(navigationItem);
            if (DimensionManager.INSTANCE_SERVER.get(targetPlanet) instanceof Dimension dim) {
                return dim.getPosition(0).distanceTo(myDim.getPosition(0));

            }
        }
        return -1;
    }

    public boolean launch() {
        if (linkedRocket != null && linkedRocket.currentProgram == null) {
            ItemStack navigationItem = inventory.getStackInSlot(0);

            double distance = distanceToTarget(navigationItem);
            if (distance > maxRangeForMission)
                return false;

            long duration = getMissionDuration(distance);

            BlockPos landPos = linkedRocket.getDockingStationPos();
            if (landPos == null) landPos = linkedRocket.blockPosition();

            lastLaunchedMissionUUID = UUID.randomUUID();

            if (navigationItem.getItem() instanceof ItemSatelliteIdChip) {
                UUID satId = ItemSatelliteIdChip.getTarget(navigationItem);
                if (SatelliteManager.getSatellite(satId) == null)
                    return false; // sat does not exist
                ProgramMissionStartBase programMissionStartBase = new ProgramSatelliteRecovery(linkedRocket, satId, duration, level.dimension().location(), landPos, lastLaunchedMissionUUID);
                linkedRocket.setProgramAndSync(programMissionStartBase);
                lastLaunchedRocketUUID = linkedRocket.getUUID();
                cycleNavigationItem(); // cycle item in case you want to pull back many satellites
                return true;

            } else if (navigationItem.getItem() instanceof ItemPlanetIdChip) {
                ResourceLocation targetPlanet = ItemPlanetIdChip.getSelectedDimension(navigationItem);
                if (targetPlanet != null) {
                    ProgramMissionStartBase programMissionStartBase = new ProgramSatelliteDeployment(linkedRocket, targetPlanet, duration, level.dimension().location(), landPos, lastLaunchedMissionUUID);
                    linkedRocket.setProgramAndSync(programMissionStartBase);
                    lastLaunchedRocketUUID = linkedRocket.getUUID();
                    return true;
                }
            } else {
                cycleNavigationItem();
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level instanceof ServerLevel serverLevel && !guiHandler.playersTrackingGui.isEmpty()) {
            if (lastLaunchedRocketUUID != null && serverLevel.getEntity(lastLaunchedRocketUUID) instanceof EntityRocket rocket) {
                if (rocket.currentProgram instanceof ProgramSatelliteDeployment) {
                    statusText.setTextAndSync("starting satellite deployment");
                } else if (rocket.currentProgram instanceof ProgramSatelliteRecovery) {
                    statusText.setTextAndSync("starting satellite recovery");
                } else {
                    // no longer valid program
                    lastLaunchedRocketUUID = null;
                }
            } else if (lastLaunchedMissionUUID != null) {
                if (MissionManager.getMission(lastLaunchedMissionUUID) instanceof RocketMission runningMission) {
                    int eta = (int) (runningMission.completeTime - GlobalTime.getGlobalTime()) / 20;
                    statusText.setTextAndSync("Mission in progress, eta: " + eta + "s");
                } else {
                    lastLaunchedMissionUUID = null;
                }
            } else {
                ItemStack navItem = inventory.getStackInSlot(0);
                double distance = distanceToTarget(navItem);
                long time = getMissionDuration(distance);
                int seconds = (int) (time / 20);
                if (distance > maxRangeForMission) {
                    statusText.setTextAndSync("target out of range");
                } else if (distance >= 0) {
                    statusText.setTextAndSync("ETA: " + seconds + "s");
                } else {
                    statusText.setTextAndSync("");
                }
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (lastLaunchedMissionUUID != null) tag.putUUID("lastLaunchedMissionUUID", lastLaunchedMissionUUID);
        if (lastLaunchedRocketUUID != null) tag.putUUID("lastLaunchedRocketUUID", lastLaunchedRocketUUID);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("lastLaunchedMissionUUID")) lastLaunchedMissionUUID = tag.getUUID("lastLaunchedMissionUUID");
        if (tag.contains("lastLaunchedRocketUUID")) lastLaunchedRocketUUID = tag.getUUID("lastLaunchedRocketUUID");
    }
}
