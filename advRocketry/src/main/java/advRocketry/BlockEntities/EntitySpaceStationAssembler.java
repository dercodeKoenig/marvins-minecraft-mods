package advRocketry.BlockEntities;

import ARLib.ARLib;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Blocks.CargoHold;
import advRocketry.Blocks.GuidanceComputer;
import advRocketry.Blocks.LaunchPad;
import advRocketry.Blocks.StructureTower;
import advRocketry.Config;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemLinker;
import advRocketry.Items.ItemSpaceStationContainer;
import advRocketry.Main;
import advRocketry.Registry;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.BTN_BLACK;
import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.BTN_W;
import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;
import static advRocketry.Registry.ENTITY_SPACE_STATION_ASSEMBLER;

// i use the rocket assembler as base class because it already has the scanning & render code
// i just need to make small changes to the gui and tick methods
public class EntitySpaceStationAssembler extends EntityRocketAssembler {

    ItemStackHandler inventory;
    UUID stationOwner;

    public EntitySpaceStationAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_SPACE_STATION_ASSEMBLER.get(), pos, blockState);
        inventory = new ItemStackHandler(3) {
            @Override
            public void onContentsChanged(int slot) {
                EntitySpaceStationAssembler.this.setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.getItem() instanceof ItemLinker;
            }
        };
        // remake the gui after inventory is created!
        makeGui();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySpaceStationAssembler) t).tick();
    }

    public void popInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), inventory.extractItem(i, inventory.getStackInSlot(i).getCount(), false));
        }
        setChanged();
    }

    @Override
    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);

        // use 1 as id, 0 would trigger rocket build
        int id = 1;
        buildButton = new guiModuleButton(id++, "build", guiHandler, 10, 30, 40, 20, BTN_BLACK, BTN_W, BTN_W);
        guiHandler.modules.add(buildButton);

        guiHandler.modules.add(
                new guiModuleItemHandlerSlot(id++, inventory, 0, 1, 0, guiHandler, 70, 23)
        );
        guiHandler.modules.add(
                new guiModuleItemHandlerSlot(id++, inventory, 1, 1, 0, guiHandler, 120, 23)
        );
        guiHandler.modules.add(
                new guiModuleItemHandlerSlot(id++, inventory, 2, 1, 0, guiHandler, 120, 43)
        );
        guiHandler.modules.add(
                new guiModuleImage(guiHandler, 90, 33, 25, 20, ResourceLocation.fromNamespaceAndPath(ARLib.MODID, "textures/gui/arrow_right.png"), 16, 12)
        );
        guiHandler.modules.add(
                new guiModuleImage(guiHandler, 73, 43, 12, 12, ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/item/linker.png"), 16, 16)
        );

        guiHandler.modules.add(
                new guiModuleEnergy(id++, battery, guiHandler, 155, 7)
        );

        guiModuleText title = new guiModuleText(id++, "Space Station Assembler", guiHandler, 10, 10, 0x00000000, false);
        guiHandler.modules.add(title);

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 165, 100, 0, 1, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 100, 200, 0, 1, guiHandler));
    }

    @Override
    public void updateGuiDockingSettings() {
// empty, does not exist here but if it is called in onload it would crash because the gui modules are not initialized
    }

        public SpaceStationDimension createNewSpaceStationDimension(String name, UUID owner) {
        SpaceStationDimensionProperties props = new SpaceStationDimensionProperties();
        props.dimensionId = ResourceLocation.fromNamespaceAndPath(Main.MODID, UUID.randomUUID().toString());
        props.owner = owner;
        props.name = name;
        // position and parent will be set when the rocket first goes there
        SpaceStationDimension spaceStation = new SpaceStationDimension(props, DimensionManager.INSTANCE_SERVER);
        DimensionManager.INSTANCE_SERVER.addDimension(spaceStation);
        return spaceStation;
    }

    // mostly copied from rocket assembler
    public boolean buildStation(boolean simulate) {
        if (level.isClientSide) return false;

        if (areaMin == null) return false;
        if (areaMax == null) return false;

        if (!inventory.getStackInSlot(1).isEmpty()) return false;
        if (!inventory.getStackInSlot(2).isEmpty()) return false;
        if (!(inventory.getStackInSlot(0).getItem() instanceof ItemLinker)) return false;

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
        // also allow empty blocks
        //if (blocks.isEmpty())
        //    return false;
        if (simulate)
            return true;

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

        ItemStack linker = inventory.extractItem(0, 1, false);

        // create station
        SpaceStationDimension spaceStationDimension = createNewSpaceStationDimension("name", stationOwner);

        // write to linker
        ItemLinker.selectBlockPos(linker, spaceStationDimension.getDimensionId().toString(), new BlockPos(0, 100, 0));

        // create output items
        inventory.setStackInSlot(1, linker);

        ItemStack container = new ItemStack(Registry.ITEM_SPACE_STATION_CONTAINER.get(), 1);
        ItemSpaceStationContainer.writeBlocks(container, blocks);
        inventory.setStackInSlot(2, container);

        setChanged();
        return true;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        super.readServer(compoundTag, serverPlayer);
        if (compoundTag.contains("guiButtonClick")) {
            int id = compoundTag.getInt("guiButtonClick");
            if (id == 1) {
                boolean ret = buildStation(true);
                if (ret) {
                    // add more time for the client structure tower to go up and stay and wait, this is why multiplier and offset
                    buildProgress = (int) (Config.INSTANCE.rocket_Assembler_Build_Time_Base * (areaMax.getY() - areaMin.getY() + 2) * 1.5);

                    // signal client to close the gui
                    guiHandler.signalCloseGui(serverPlayer);

                    stationOwner = serverPlayer.getUUID(); // set the owner for when the build completes
                }
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        super.readClient(compoundTag);
    }

    @Override
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
                    boolean shouldConsumeEnergy = buildProgress <= Config.INSTANCE.rocket_Assembler_Build_Time_Base * (areaMax.getY() - areaMin.getY() + 2);
                    if (battery.getEnergyStored() >= Config.INSTANCE.rocket_Assembler_Energy_Per_Tick || !shouldConsumeEnergy) {
                        buildProgress--;
                        if (shouldConsumeEnergy)
                            battery.extractEnergy(Config.INSTANCE.rocket_Assembler_Energy_Per_Tick, false);
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
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 190, true);
    }
}
