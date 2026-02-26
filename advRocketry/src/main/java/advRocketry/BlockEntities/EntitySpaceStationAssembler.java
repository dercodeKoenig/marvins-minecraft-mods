package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Blocks.CargoHold;
import advRocketry.Blocks.GuidanceComputer;
import advRocketry.Blocks.LaunchPad;
import advRocketry.Blocks.StructureTower;
import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.BTN_BLACK;
import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.BTN_W;
import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;
import static advRocketry.Registry.ENTITY_SPACE_STATION_ASSEMBLER;

// i use the rocket assembler as base class because it already has the scanning & render code
// i just need to make small changes to the gui and tick methods
public class EntitySpaceStationAssembler extends EntityRocketAssembler{


    public EntitySpaceStationAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_SPACE_STATION_ASSEMBLER.get(), pos, blockState);
    }

    @Override
    public void makeGui(){
        // guihandler needs redesign
        guiHandler = new GuiHandlerBlockEntity(this);
        // use 1 as id, 0 would trigger rocket build
        buildButton = new guiModuleButton(1, "build", guiHandler, 10, 10, 40, 20, BTN_BLACK, BTN_W, BTN_W);
        statusText = new guiModuleText(2, "", guiHandler, 10, 10, 0x00000000, false);
        guiHandler.modules.add(buildButton);
        guiHandler.modules.add(statusText);

        guiHandler.modules.add(
                new guiModuleEnergy(2,battery,guiHandler,138,7)
        );
    }

    // mostly copied from rocket assembler
    ConstructionResult buildStation(boolean simulate){
        if (level.isClientSide) return null;

        if (areaMin == null) return ConstructionResult.INVALID_LAUNCHPAD;
        if (areaMax == null) return ConstructionResult.INVALID_LAUNCHPAD;

        int minX = areaMax.getX();
        int maxX = areaMin.getX();
        int minY = areaMax.getY();
        int maxY = areaMin.getY();
        int minZ = areaMax.getZ();
        int maxZ = areaMin.getZ();
        for (int x = areaMin.getX(); x <= areaMax.getX(); x++) {
            for (int y = areaMin.getY(); y <= areaMax.getY(); y++) {
                for (int z = areaMin.getZ(); z <= areaMax.getZ(); z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        if (minX > x)
                            minX = x;
                        if (minY > y)
                            minY = y;
                        if (minZ > z)
                            minZ = z;

                        if (maxX < x)
                            maxX = x;
                        if (maxY < y)
                            maxY = y;
                        if (maxZ < z)
                            maxZ = z;
                    }
                }
            }
        }

        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    BlockPos inStationPos = pos.subtract(new BlockPos(minX, minY, minZ));
                    blocks.put(inStationPos, state);
                }
            }
        }

        if (!simulate) {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        // if i understand this correctly, 2 = send to clients, 16 = no neighbor update
                        // neighbor could break some blocks like sign that would pop away
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
            }
        }

        return ConstructionResult.SUCCESS;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        super.readServer(compoundTag, serverPlayer);
        if (compoundTag.contains("guiButtonClick")) {
            int id = compoundTag.getInt("guiButtonClick");
            if (id == 1) {
                ConstructionResult ret = buildStation(true);
                if (ret == ConstructionResult.SUCCESS) {
                    // add more time for the client structure tower to go up and stay and wait, this is why multiplier and offset
                    buildProgress = (int) (Config.INSTANCE.rocket_Assembler_Build_Time_Base * (areaMax.getY() - areaMin.getY() + 2) * 1.5);

                    // signal client to close the gui
                    guiHandler.signalCloseGui(serverPlayer);
                }
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        super.readClient(compoundTag);
    }

    public void tick() {

        if (level.isClientSide) {
            // build progress logic client
            if (clientBuildProgress < buildProgress) {
                clientBuildProgress += 2;
                clientBuildDiffPerTick = 2;
            } else if (clientBuildProgress > buildProgress) {
                clientBuildProgress--;
                clientBuildDiffPerTick = -1;
            } else {
                clientBuildDiffPerTick = 0;
            }
        }

        if (!level.isClientSide) {
            guiHandler.serverTick();

            // build progress logic server
            if (buildProgress > -1) {
                if (areaMin != null && areaMax != null) {
                    boolean shouldConsumeEnergy = buildProgress <= Config.INSTANCE.rocket_Assembler_Build_Time_Base * (areaMax.getY() - areaMin.getY()+2);
                    if(battery.getEnergyStored() >= Config.INSTANCE.rocket_Assembler_Energy_Per_Tick || !shouldConsumeEnergy) {
                        buildProgress--;
                        if(shouldConsumeEnergy)
                            battery.extractEnergy(Config.INSTANCE.rocket_Assembler_Energy_Per_Tick,false);
                        if (buildProgress == -1) {
                            buildStation(false);
                        }
                    }
                } else {
                    buildProgress = -1;
                }
                broadcastInformationToPlayers(null);
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySpaceStationAssembler) t).tick();
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(160, 100, true);
    }
}
