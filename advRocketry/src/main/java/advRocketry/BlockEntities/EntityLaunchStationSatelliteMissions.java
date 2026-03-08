package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.LaunchStation;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Items.ItemSatelliteIdChip;
import advRocketry.Registry.BlockEntities;
import advRocketry.Rocket.RocketPrograms.ProgramMissionStartBase;
import advRocketry.Rocket.RocketPrograms.ProgramSatelliteDeployment;
import advRocketry.Rocket.RocketPrograms.ProgramSatelliteRecovery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

    UUID lastLaunchedUUID = null;

    public EntityLaunchStationSatelliteMissions(BlockPos pos, BlockState blockState) {
        super(BlockEntities.ENTITY_LAUNCH_STATION_SATELLITE_MISSIONS.get(), pos, blockState);
    }

    // overwrite in subclasses
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemPlanetIdChip)
            return true;
        if (stack.getItem() instanceof ItemSatelliteIdChip)
            return true;
        return false;
    }

    // overwrite in subclasses
    public void onInventoryChanged() {

    }

    public void launch() {
        if (linkedRocket != null) {
            ItemStack navigationItem = inventory.getStackInSlot(0);

            BlockPos landPos = linkedRocket.getDockingStationPos();
            if (landPos == null)
                landPos = linkedRocket.blockPosition();

            lastLaunchedUUID = UUID.randomUUID();

            if (navigationItem.getItem() instanceof ItemSatelliteIdChip) {
                ProgramMissionStartBase programMissionStartBase = new ProgramSatelliteRecovery(
                        linkedRocket,
                        ItemSatelliteIdChip.getTarget(navigationItem),
                        level.dimension().location(),
                        landPos,
                        lastLaunchedUUID
                );
                linkedRocket.setProgramAndSync(programMissionStartBase);
            } else if (navigationItem.getItem() instanceof ItemPlanetIdChip) {
                ResourceLocation targetPlanet = ItemPlanetIdChip.getSelectedDimension(navigationItem);
                if (targetPlanet != null) {
                    ProgramMissionStartBase programMissionStartBase = new ProgramSatelliteDeployment(
                            linkedRocket,
                            targetPlanet,
                            level.dimension().location(),
                            landPos,
                            lastLaunchedUUID
                    );
                    linkedRocket.setProgramAndSync(programMissionStartBase);
                }
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("lastLaunchedUUID", lastLaunchedUUID);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastLaunchedUUID = tag.getUUID("lastLaunchedUUID");
    }
}
