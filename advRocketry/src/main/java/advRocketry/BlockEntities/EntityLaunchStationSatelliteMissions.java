package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.LaunchStation;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Missions.*;
import advRocketry.Registry.BlockEntities;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketPrograms.ProgramMissionStartBase;
import advRocketry.Rocket.RocketPrograms.ProgramSatelliteDeployment;
import advRocketry.Rocket.RocketPrograms.ProgramSatelliteRecovery;
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

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 2000, 1, 0, guiHandler));

        guiModuleButton launchButton = new guiModuleButton(launch_btn_id, "launch", guiHandler, 90, 20, 40, 15, BTN_GREEN, BTN_W, BTN_H);
        guiHandler.modules.add(launchButton);

        statusText = new guiModuleText(76984, "status", guiHandler, 10, 50, 0xff000000, false);
        guiHandler.modules.add(statusText);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemPlanetIdChip) return true;
        if (stack.getItem() instanceof ItemSatelliteIdChip) return true;
        return false;
    }

    public void onInventoryChanged() {

    }

    public void openGui() {
        guiHandler.openGui(176, 165, true);
    }

    public void launch() {
        if (linkedRocket != null && linkedRocket.currentProgram == null) {
            ItemStack navigationItem = inventory.getStackInSlot(0);

            BlockPos landPos = linkedRocket.getDockingStationPos();
            if (landPos == null) landPos = linkedRocket.blockPosition();

            lastLaunchedMissionUUID = UUID.randomUUID();

            if (navigationItem.getItem() instanceof ItemSatelliteIdChip) {
                ProgramMissionStartBase programMissionStartBase = new ProgramSatelliteRecovery(linkedRocket, ItemSatelliteIdChip.getTarget(navigationItem), level.dimension().location(), landPos, lastLaunchedMissionUUID);
                linkedRocket.setProgramAndSync(programMissionStartBase);
                lastLaunchedRocketUUID = linkedRocket.getUUID();
                level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.STATE, LaunchStation.State.active), 3);
                activeTimeout = 40;

            } else if (navigationItem.getItem() instanceof ItemPlanetIdChip) {
                ResourceLocation targetPlanet = ItemPlanetIdChip.getSelectedDimension(navigationItem);
                if (targetPlanet != null) {
                    ProgramMissionStartBase programMissionStartBase = new ProgramSatelliteDeployment(linkedRocket, targetPlanet, level.dimension().location(), landPos, lastLaunchedMissionUUID);
                    linkedRocket.setProgramAndSync(programMissionStartBase);
                    lastLaunchedRocketUUID = linkedRocket.getUUID();
                    level.setBlock(getBlockPos(), getBlockState().setValue(LaunchStation.STATE, LaunchStation.State.active), 3);
                    activeTimeout = 40;
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level instanceof ServerLevel serverLevel && !guiHandler.playersTrackingGui.isEmpty()) {
            if (lastLaunchedRocketUUID != null && serverLevel.getEntity(lastLaunchedRocketUUID) instanceof EntityRocket rocket) {
                if (rocket.currentProgram instanceof ProgramSatelliteDeployment) {
                    statusText.setTextAndSync("starting satellite deployment");
                }
                else if (rocket.currentProgram instanceof ProgramSatelliteRecovery) {
                    statusText.setTextAndSync("starting satellite recovery");
                }
                else{
                    // no longer valid program
                    lastLaunchedRocketUUID = null;
                }
            } else if (lastLaunchedMissionUUID != null && MissionManager.missions.get(lastLaunchedMissionUUID) instanceof RocketMission runningMission) {
                int eta = (int) (runningMission.completeTime - GlobalTime.getGlobalTime()) / 20;
                statusText.setTextAndSync("Mission in progress, eta: " + eta + "s");
            } else {
                statusText.setTextAndSync("");
                lastLaunchedMissionUUID = null;
                lastLaunchedRocketUUID = null;
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
