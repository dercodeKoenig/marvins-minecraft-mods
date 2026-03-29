package advRocketry.BlockEntities;

import ARLib.blockentities.EntityItemInputBlock;
import ARLib.blockentities.EntityItemOutputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Missions.MissionManager;
import advRocketry.Missions.RocketMission;
import advRocketry.Registry.BlockEntities;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketPrograms.ProgramAsteroidMiningMission;
import advRocketry.Rocket.RocketPrograms.ProgramMissionStartBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;

public class EntityLaunchStationAsteroidMissions extends EntityLaunchStation {

    UUID lastLaunchedMissionUUID = null;
    UUID lastLaunchedRocketUUID = null;
    guiModuleText statusText;

    public EntityLaunchStationAsteroidMissions(BlockPos pos, BlockState blockState) {
        super(BlockEntities.ENTITY_LAUNCH_STATION_ASTEROID_MISSIONS.get(), pos, blockState);
    }

    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);

        guiHandler.modules.add(new guiModuleText(0, "Asteroid Mission Launch Station", guiHandler, 5, 5, 0xff000000, false));

        guiHandler.modules.add(new guiModuleItemHandlerSlot(1, inventory, 0, 0, 1, guiHandler, 50, 20));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 2000, 1, 0, guiHandler));

        guiModuleButton launchButton = new guiModuleButton(launch_btn_id, "launch", guiHandler, 90, 20, 40, 15, BTN_GREEN, BTN_W, BTN_H);
        guiHandler.modules.add(launchButton);

        statusText = new guiModuleText(76984, "status", guiHandler, 10, 50, 0xff000000, false);
        guiHandler.modules.add(statusText);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemAsteroidIdChip) return true;
        return false;
    }

    public void openGui() {
        guiHandler.openGui(176, 165, true);
    }


    public boolean launch() {
        if (linkedRocket != null && linkedRocket.currentProgram == null) {
            ItemStack navigationItem = inventory.getStackInSlot(0);
            if (navigationItem.getItem() instanceof ItemAsteroidIdChip) {
                AsteroidManager.DiscoveredAsteroid target = ItemAsteroidIdChip.getSelectedAsteroid(navigationItem);
                if (target == null || target.isExpired()) {
                    ItemAsteroidIdChip.setDescriptionForAsteroid(navigationItem, "Asteroid invalid");
                } else {
                    lastLaunchedMissionUUID = UUID.randomUUID();
                    BlockPos landPos = linkedRocket.getDockingStationPos();
                    if (landPos == null) landPos = linkedRocket.blockPosition();
                    ProgramMissionStartBase programMissionStartBase = new ProgramAsteroidMiningMission(linkedRocket, target.key, level.dimension().location(), landPos, lastLaunchedMissionUUID);
                    linkedRocket.setProgramAndSync(programMissionStartBase);
                    lastLaunchedRocketUUID = linkedRocket.getUUID();
                    cycleNavigationItem();
                    return true;
                }
            }
            cycleNavigationItem();
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level instanceof ServerLevel serverLevel && !guiHandler.playersTrackingGui.isEmpty()) {
            if (lastLaunchedRocketUUID != null && serverLevel.getEntity(lastLaunchedRocketUUID) instanceof EntityRocket rocket) {
                if (rocket.currentProgram instanceof ProgramAsteroidMiningMission) {
                    statusText.setTextAndSync("starting asteroid mining");
                } else {
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
