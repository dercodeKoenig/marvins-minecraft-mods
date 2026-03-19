package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.blockentities.EntityItemInputBlock;
import ARLib.blockentities.EntityItemOutputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.Observatory;
import advRocketry.Config;
import advRocketry.Data.DataTypes;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemAsteroidIdChip;
import advRocketry.Items.ItemGalaxyDatabase;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Missions.AsteroidManager;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.Items;
import advRocketry.Render.starmap.SpaceMapScreen;
import advRocketry.Utils.AxisDirections;
import advRocketry.Utils.ClientUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Blocks.Observatory.TASK_STATE;

public class EntityObservatory extends EntityMultiblockMachineMasterWithData {

    public static final String REQUIRED_DATA = DataTypes.distance;
    public static final int writePlanetToChipTicks = 20 * 5;
    public static final int syncStorageDisksTicks = 20 * 10;
    public static final int STORAGE_DISK_SLOT_1 = 0;
    public static final int STORAGE_DISK_SLOT_2 = 1;
    public static final int PLANET_ID_CHIP_SLOT = 2;

    public static Object[][][] structure =
            new Object[][][]{
                    {{null, null, null, null, null},
                            {null, 's', 'g', 's', null},
                            {null, 's', 's', 's', null},
                            {null, 's', 's', 's', null},
                            {null, null, null, null, null}},

                    {{null, null, null, null, null},
                            {null, 's', 's', 's', null},
                            {null, 's', 'g', 's', null},
                            {null, 's', 's', 's', null},
                            {null, null, null, null, null}},

                    {{null, 's', 's', 's', null},
                            {'s', 'a', 'a', 'a', 's'},
                            {'s', 'a', 'a', 'a', 's'},
                            {'s', 'a', 'g', 'a', 's'},
                            {null, 's', 's', 's', null}},

                    {{null, 's', 'c', 's', null},
                            {'s', 's', 's', 's', 's'},
                            {'s', 's', 's', 's', 's'},
                            {'s', 's', 's', 's', 's'},
                            {null, 's', 's', 's', null}},

                    {{null, '*', '*', '*', null},
                            {'*', 't', 't', 't', '*'},
                            {'*', 't', 'm', 't', '*'},
                            {'*', 't', 't', 't', '*'},
                            {null, '*', '*', '*', null}}};
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(advRocketry.Registry.Blocks.OBSERVATORY.get()));
        charMapping.put('s', List.of(ARLibRegistry.BLOCK_STRUCTURE.get()));
        charMapping.put('t', List.of(advRocketry.Registry.Blocks.STRUCTURE_TOWER.get()));
        charMapping.put('g', List.of(Blocks.GLASS));
        charMapping.put('a', List.of(Blocks.AIR));
        charMapping.put('m', List.of(ARLibRegistry.BLOCK_MOTOR.get()));
        charMapping.put('*', List.of(
                ARLibRegistry.BLOCK_STRUCTURE.get(),
                ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get(),
                advRocketry.Registry.Blocks.DATA_STORAGE_BLOCK.get()
        ));
    }

    public RenderData renderData = new RenderData();

    public ItemStackHandler itemStackHandler;
    public Task task = Task.IDLE;
    public ResourceLocation taskTarget = null;
    public Task lastTask = Task.IDLE;
    public ResourceLocation lastTaskTarget = null;
    public int taskProgress;
    public boolean hasEnoughEnergy = false;
    public boolean hasEnoughData = false; // for analyzing
    public boolean hasAsteroidChips = false; // for asteroid mining task render logic
    public boolean hasFreeOutputSlots = false; // for asteroid mining task render logic

    public GuiHandlerBlockEntity guiHandler;
    guiModuleItemHandlerSlot storageDiskSlot1;
    guiModuleItemHandlerSlot storageDiskSlot2;
    guiModuleItemHandlerSlot planetIdChipSlot;
    guiModuleButton scanPlanetBtn;
    guiModuleButton scanAsteroidBtn;
    guiModuleButton syncStorageDisksBtn;
    guiModuleProgressBarHorizontal6px guiProgressBar;
    guiModuleVerticalProgressBar energyBar;
    guiModuleText statusText;
    int customStatusTimeout = 0;
    // to not have the status text go on/off every tick when data or energy is going in slow
    int noEnergyTickCounter = 0;
    int noDataTickCounter = 0;

    public EntityObservatory(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_OBSERVATORY.get(), pos, state);
        //super.forwardInteractionToMaster = true;

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.maxDistance = 16;

        itemStackHandler = new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == STORAGE_DISK_SLOT_1 || slot == STORAGE_DISK_SLOT_2)
                    return stack.getItem().equals(Items.ITEM_GALAXY_DATABASE.get());
                if (slot == PLANET_ID_CHIP_SLOT)
                    return stack.getItem().equals(Items.ITEM_PLANET_ID_CHIP.get());
                return false;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public void onContentsChanged(int slot) {
                // move from storage slot 2 to slot 1 if slot 1 is empty
                if (slot == STORAGE_DISK_SLOT_2 || slot == STORAGE_DISK_SLOT_1) {
                    if (getStackInSlot(STORAGE_DISK_SLOT_1).isEmpty() && !getStackInSlot(STORAGE_DISK_SLOT_2).isEmpty()) {
                        insertItem(STORAGE_DISK_SLOT_1, extractItem(STORAGE_DISK_SLOT_2, getStackInSlot(STORAGE_DISK_SLOT_2).getCount(), false), false);
                    }
                }

                // make sure the current planet is always distance-unlocked
                // also make sure it displays known-by-default planets as known, so add them as unlocked planets
                ItemStack stack = getStackInSlot(slot);
                if (stack.getItem() instanceof ItemGalaxyDatabase && level != null) {
                    for (Dimension dim : DimensionManager.INSTANCE_SERVER.dimensions.values()) {
                        if (dim instanceof PlanetDimension planetDimension) {
                            int maxData = ItemGalaxyDatabase.POINTS_UNLOCKED(planetDimension);
                            if (dim.getDimensionId().equals(level.dimension().location())) {
                                // current dimension, distance is 100% unlocked
                                ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(stack, planetDimension);
                                if (info == null)
                                    info = new ItemGalaxyDatabase.PlanetInfo();
                                info.put(DataTypes.distance, maxData);
                                ItemGalaxyDatabase.setPlanetInfo(stack, planetDimension, info);
                            }
                            if (planetDimension.isKnown()) {
                                // known by default, unlock all data
                                ItemGalaxyDatabase.PlanetInfo info = new ItemGalaxyDatabase.PlanetInfo();
                                info.put(DataTypes.distance, maxData);
                                info.put(DataTypes.mass, maxData);
                                info.put(DataTypes.composition, maxData);
                                ItemGalaxyDatabase.setPlanetInfo(stack, planetDimension, info);
                            }
                        }
                    }
                }

                EntityObservatory.this.setChanged();
            }
        };
        storageDiskSlot1 = new guiModuleItemHandlerSlot(0, itemStackHandler, STORAGE_DISK_SLOT_1, 1, 0, guiHandler, 130, 150);
        guiHandler.modules.add(storageDiskSlot1);
        storageDiskSlot2 = new guiModuleItemHandlerSlot(1, itemStackHandler, STORAGE_DISK_SLOT_2, 1, 0, guiHandler, 150, 150);
        guiHandler.modules.add(storageDiskSlot2);
        guiHandler.modules.add(
                new guiModuleText(3, "galaxy database:", guiHandler, 10, 153, 0xff000000, false)
        );

        planetIdChipSlot = new guiModuleItemHandlerSlot(4, itemStackHandler, PLANET_ID_CHIP_SLOT, 1, 0, guiHandler, 150, 130);
        guiHandler.modules.add(planetIdChipSlot);
        guiHandler.modules.add(
                new guiModuleText(5, "planet id chip:", guiHandler, 10, 133, 0xff000000, false)
        );

        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            guiHandler.modules.add(
                    new guiModuleButton(100, "open galaxy", guiHandler, 10, 10, 70, 15, BTN_BLACK, BTN_W, BTN_H) {
                        public void onButtonClicked() {
                            Minecraft.getInstance().setScreen(
                                    new SpaceMapScreen() {
                                        @Override
                                        public void tick() {
                                            super.tick();
                                            // make sure the main gui stays in sync
                                            EntityObservatory.this.guiHandler.onGuiClientTick(ClientUtils.getSinglePlayer());
                                        }

                                        @Override
                                        public void onClose() {
                                            super.onClose();
                                            // open the main gui again
                                            openGui(null);
                                        }

                                        public void interact(ResourceLocation dimensionId) {
                                            CompoundTag info = new CompoundTag();
                                            info.putString("interact", dimensionId.toString());
                                            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
                                        }

                                        public String getInteractText(ResourceLocation dimensionId) {
                                            PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
                                            if (planet == null) return "";

                                            if (!planet.isKnown() && !client_IsDistanceUnlocked(dimensionId)) {
                                                // if the planet is not known but still rendered / clicked, assume the storage disk is inserted, no need to check
                                                return "Analyze";
                                            }

                                            if (!planetIdChipSlot.client_getItemStackToRender().isEmpty()) {
                                                return "burn to chip";
                                            }
                                            return "";
                                        }

                                        public String getPlanetInfoText(ResourceLocation dimensionId, ItemGalaxyDatabase.PlanetInfo ignored) {
                                            PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
                                            if (planet == null) return "";

                                            if (!planet.isKnown() && !client_IsDistanceUnlocked(dimensionId)) {
                                                String s = "We require more information about this planet.";
                                                ItemGalaxyDatabase.PlanetInfo info = client_getPlanetInfo(dimensionId);
                                                if (info != null) {
                                                    s += "\ndistance: " + info.get(DataTypes.distance) + " / " + ItemGalaxyDatabase.POINTS_UNLOCKED(planet) + "\n";
                                                }
                                                return s;
                                            }

                                            return super.getPlanetInfoText(dimensionId, client_getPlanetInfo(dimensionId));
                                        }

                                        public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
                                            Dimension d = DimensionManager.INSTANCE_CLIENT.get(dimensionId);
                                            if (d == null) return false;

                                            if (((PlanetDimension) (d)).isKnown()) {
                                                return true;
                                            }

                                            if (client_IsDimensionKnown(dimensionId))
                                                return true;

                                            return false;
                                        }
                                    }
                            );
                        }
                    }
            );
        }

        statusText = new guiModuleText(199, "current task:", guiHandler, 10, 30, 0xff000000, false);
        guiHandler.modules.add(statusText);

        guiProgressBar = new guiModuleProgressBarHorizontal6px(200, 0xffffffff, guiHandler, 10, 50);
        guiHandler.modules.add(guiProgressBar);

        scanPlanetBtn = new guiModuleButton(201, "Scan for Planets", guiHandler, 10, 60, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "ScanPlanet");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(scanPlanetBtn);


        scanAsteroidBtn = new guiModuleButton(202, "Scan for Asteroids", guiHandler, 10, 80, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "ScanAsteroid");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(scanAsteroidBtn);

        syncStorageDisksBtn = new guiModuleButton(203, "Sync Storage Disks", guiHandler, 10, 100, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "syncStorageDisks");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(syncStorageDisksBtn);

        energyBar = new guiModuleVerticalProgressBar(300, guiHandler, 155, 60);
        guiHandler.modules.add(energyBar);

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 175, 10000, 0, 1, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityObservatory) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("onLoad", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    public void setStatusText(String status) {
        customStatusTimeout = 20 * 60;
        statusText.setTextAndSync(status);
    }

    public boolean startAnalyzingRandomPlanet(ItemStack storageDisk) {
        // find a random planet that is discovered but not unlocked
        List<ResourceLocation> randomDimIds = new ArrayList<>(DimensionManager.INSTANCE_SERVER.dimensions.keySet());
        Collections.shuffle(randomDimIds);
        for (ResourceLocation dimId : randomDimIds) {
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(dimId);
            if (dim instanceof PlanetDimension planetDimension && !planetDimension.isKnown()) {
                // check if known
                if (ItemGalaxyDatabase.isDimensionKnown(storageDisk, planetDimension)) {
                    // check if not unlocked
                    if (!ItemGalaxyDatabase.isDistanceUnlocked(storageDisk, planetDimension)) {
                        toggleTask(Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED, dimId);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void analyzeRandomPlanetOrTurnOff(ItemStack storageDisk) {
        if (!(storageDisk.getItem() instanceof ItemGalaxyDatabase)) {
            // has no data disk, can not work
            toggleTask(Task.IDLE, null);
            setStatusText("no galaxy database found");
        }

        if (!startAnalyzingRandomPlanet(storageDisk)) {
            // if nothing there to analyze, go idle
            toggleTask(Task.IDLE, null);
            setStatusText("unable to find any new planets");
        }
    }

    public PlanetDimension getNextPlanetToDiscover(ItemStack storageDisk) {
        List<ResourceLocation> randomDimIds = new ArrayList<>(DimensionManager.INSTANCE_SERVER.dimensions.keySet());
        Collections.shuffle(randomDimIds);
        for (ResourceLocation dimId : randomDimIds) {
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(dimId);
            // only consider planet dimensions
            if (dim instanceof PlanetDimension planetDimension) {
                // only consider planets not known by default
                if (!planetDimension.isKnown()) {
                    // only consider planets that are not already known in the supplied storage disk
                    if (!ItemGalaxyDatabase.isDimensionKnown(storageDisk, planetDimension)) {
                        // if the dimension has a parent dimension that is unknown, the parent has to be discovered first!
                        if (planetDimension.getParentDimensionId() != null) {
                            // has a parent
                            Dimension parent = DimensionManager.INSTANCE_SERVER.get(planetDimension.getParentDimensionId());
                            if (parent instanceof PlanetDimension parentPlanet) {
                                // parent is a planet
                                if (!parentPlanet.isKnown()) {
                                    // parent is not known by default
                                    if (!ItemGalaxyDatabase.isDimensionKnown(storageDisk, parentPlanet)) {
                                        // parent is also not known on disk
                                        // we can not discover the planet until parent is known
                                        continue;
                                    }
                                }
                            }
                        }
                        // TODO: artifact check, is artifact required and supplied in input hatch?
                        // TODO: sort discovered planets by distance?

                        // discover this planet!
                        return planetDimension;
                    }
                }
            }
        }
        return null;
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (!getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED) && task != Task.IDLE) {
                toggleTask(Task.IDLE, null);
                if (getBlockState().getValue(TASK_STATE) != Observatory.TaskState.idle)
                    level.setBlock(getBlockPos(), getBlockState().setValue(TASK_STATE, Observatory.TaskState.idle), 3);
            } else {

                // set blockstate based on task
                Observatory.TaskState myState = getBlockState().getValue(TASK_STATE);
                if (task == Task.SCANNING_FOR_ASTEROIDS) {
                    if (myState != Observatory.TaskState.scanning_asteroid)
                        level.setBlock(getBlockPos(), getBlockState().setValue(TASK_STATE, Observatory.TaskState.scanning_asteroid), 3);
                } else if (task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED ||
                        task == Task.ANALYZE_PLANET
                ) {
                    if (myState != Observatory.TaskState.scanning_planet)
                        level.setBlock(getBlockPos(), getBlockState().setValue(TASK_STATE, Observatory.TaskState.scanning_planet), 3);
                } else if (task == Task.SCANNING_FOR_PLANETS) {
                    if (myState != Observatory.TaskState.searching_planet)
                        level.setBlock(getBlockPos(), getBlockState().setValue(TASK_STATE, Observatory.TaskState.searching_planet), 3);
                } else {
                    if (myState != Observatory.TaskState.idle)
                        level.setBlock(getBlockPos(), getBlockState().setValue(TASK_STATE, Observatory.TaskState.idle), 3);
                }


                List<EntityEnergyInputBlock> energyInputBlocks = getEnergyInputTiles();
                List<EntityDataStorageBlock> dataTiles = getDataTiles();

                int energy = this.getTotalEnergyStored(energyInputBlocks);
                int maxEnergy = this.getMaxEnergyStored(energyInputBlocks);
                int data = this.getData(REQUIRED_DATA, dataTiles, false);

                // update energy status
                boolean newHasEnoughEnergy = energy > Config.INSTANCE.observatory_Energy_Per_Tick;
                if (newHasEnoughEnergy != hasEnoughEnergy) {
                    hasEnoughEnergy = newHasEnoughEnergy;
                    sendUpdatePacket(null); // client needs to know about energy to stop the rotate animations
                }
                if (!hasEnoughEnergy)
                    noEnergyTickCounter++;
                else
                    noEnergyTickCounter = 0;

                // update data status
                boolean newHasEnoughData = false;
                if (task == Task.ANALYZE_PLANET || task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED
                ) {
                    if (data > 0)
                        newHasEnoughData = true;
                } else if (task == Task.SCANNING_FOR_ASTEROIDS) {
                    if (data > 0)
                        newHasEnoughData = true;
                } else {
                    // no data required
                    newHasEnoughData = true;
                }
                if (newHasEnoughData != hasEnoughData) {
                    hasEnoughData = newHasEnoughData;
                    sendUpdatePacket(null);
                }
                if (!hasEnoughData)
                    noDataTickCounter++;
                else
                    noDataTickCounter = 0;

                // update gui
                if (!guiHandler.playersTrackingGui.isEmpty()) {

                    if (task == Task.SCANNING_FOR_PLANETS || task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED) {
                        scanPlanetBtn.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                    } else {
                        scanPlanetBtn.setBackgroundAndSync(BTN_RED, BTN_W, BTN_H);
                    }

                    if (task == Task.SCANNING_FOR_ASTEROIDS) {
                        scanAsteroidBtn.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                    } else {
                        scanAsteroidBtn.setBackgroundAndSync(BTN_RED, BTN_W, BTN_H);
                    }

                    if (task == Task.SYNC_STORAGE_DISKS) {
                        syncStorageDisksBtn.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                    } else {
                        syncStorageDisksBtn.setBackgroundAndSync(BTN_RED, BTN_W, BTN_H);
                    }

                    if (customStatusTimeout <= 0) {
                        if (noEnergyTickCounter > 20 * 5) {
                            statusText.setTextAndSync("OUT OF ENERGY!");
                        } else if (noDataTickCounter > 20 * 5) {
                            statusText.setTextAndSync("OUT OF DISTANCE DATA!");
                        } else {
                            String s = "Status:\n" + task.label;
                            if (taskTarget != null) {
                                if (DimensionManager.INSTANCE_SERVER.get(taskTarget) instanceof PlanetDimension targetPlanet) {
                                    s += ": " + targetPlanet.getName();
                                }
                            }
                            statusText.setTextAndSync(s);
                        }
                    } else {
                        customStatusTimeout--;
                    }

                    energyBar.setProgressAndSync((double) energy / maxEnergy);
                    energyBar.setHoverInfoAndSync(energy + " / " + maxEnergy + " RF");
                }

                if (task == Task.IDLE) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                }

                if (task == Task.SCANNING_FOR_ASTEROIDS) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                    if (hasEnoughData && hasEnoughEnergy) {
                        List<EntityItemInputBlock> inputBlocks = super.getItemInTiles();
                        List<EntityItemOutputBlock> outputBlocks = super.getItemOutTiles();
                        boolean hasAsteroidChip = false;
                        int inputBlockIndex = 0;
                        int inputSlotIndex = 0;
                        boolean hasFreeOutputSlot = false;
                        int outputBlockIndex = 0;
                        int outputSlotIndex = 0;
                        for (int i = 0; i < inputBlocks.size(); i++) {
                            for (int j = 0; j < inputBlocks.get(i).inventory.getSlots(); j++) {
                                if (inputBlocks.get(i).inventory.getStackInSlot(j).getItem() instanceof ItemAsteroidIdChip) {
                                    hasAsteroidChip = true;
                                    inputBlockIndex = i;
                                    inputSlotIndex = j;
                                    break;
                                }
                            }
                            if (hasAsteroidChip)
                                break;
                        }
                        for (int i = 0; i < outputBlocks.size(); i++) {
                            for (int j = 0; j < outputBlocks.get(i).inventory.getSlots(); j++) {
                                if (outputBlocks.get(i).inventory.getStackInSlot(j).isEmpty()) {
                                    hasFreeOutputSlot = true;
                                    outputBlockIndex = i;
                                    outputSlotIndex = j;
                                    break;
                                }
                            }
                            if (hasFreeOutputSlot)
                                break;
                        }
                        if (!hasAsteroidChip)
                            setStatusText("MISSING ASTEROID CHIP");
                        else if (!hasFreeOutputSlot)
                            setStatusText("NO SPACE IN INVENTORY");
                        else
                            customStatusTimeout = 0;
                        if (hasAsteroidChip != this.hasAsteroidChips || hasFreeOutputSlot != this.hasFreeOutputSlots) {
                            this.hasAsteroidChips = hasAsteroidChip;
                            this.hasFreeOutputSlots = hasFreeOutputSlot;
                            sendUpdatePacket(null);
                        }
                        if (hasAsteroidChip && hasFreeOutputSlot) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            double p = Math.random();
                            double pTarget = (double) 1 / 10;
                            // limit speed to max 2 steps / tick on average
                            if (p < pTarget) {
                                extractData(REQUIRED_DATA, 1, dataTiles, false);
                                p = Math.random();
                                pTarget = Config.INSTANCE.observatory_Find_Asteroid_P_Per_Tick;
                                if (p < pTarget) {
                                    // find a new asteroid
                                    AsteroidManager.DiscoveredAsteroid discoveredAsteroid = AsteroidManager.discoverNewAsteroid();
                                    if (discoveredAsteroid == null) {
                                        // this should not happen if asteroids are configured
                                        setStatusText("NO ASTEROIDS OUT THERE");
                                        toggleTask(Task.IDLE, null);
                                    } else {
                                        ItemStack asteroidChip = inputBlocks.get(inputBlockIndex).inventory.extractItem(inputSlotIndex, 1, false);
                                        ItemAsteroidIdChip.setSelectedAsteroid(discoveredAsteroid, asteroidChip);
                                        outputBlocks.get(outputBlockIndex).inventory.insertItem(outputSlotIndex, asteroidChip, false);
                                    }
                                }
                            }
                        }
                    }
                }

                if (task == Task.SCANNING_FOR_PLANETS) {
                    ItemStack storageDisk = getMainDatabase();
                    if (!(storageDisk.getItem() instanceof ItemGalaxyDatabase)) {
                        // has no data disk, can not work
                        toggleTask(Task.IDLE, null);
                        setStatusText("no galaxy database found");
                    } else {
                        guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                        if (hasEnoughEnergy) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            double p = Math.random();
                            double pTarget = Config.INSTANCE.observatory_Find_Planet_P_Per_Tick;
                            if (p < pTarget) {
                                // discover a new random planet that is not already known
                                PlanetDimension nextToDiscover = getNextPlanetToDiscover(storageDisk);
                                if (nextToDiscover != null) {
                                    // add the planet to the list
                                    ItemGalaxyDatabase.discoverPlanet(storageDisk, nextToDiscover);
                                    // send a message to nearby players
                                    for (Player player : level.players()) {
                                        if (player.position().distanceTo(getBlockPos().getCenter()) < 32) {
                                            player.sendSystemMessage(Component.literal("A nearby Observatory discovered a new Planet: " + nextToDiscover.getName()));
                                        }
                                    }
                                }
                                // check if there are still any planets left that can be discovered
                                if (getNextPlanetToDiscover(storageDisk) != null) {
                                    // just continue the work
                                } else {
                                    // start analyzing random planets if everything is discovered
                                    analyzeRandomPlanetOrTurnOff(storageDisk);
                                }
                            }
                            setChanged();
                        }
                    }
                }

                if (task == Task.ANALYZE_PLANET || task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                    ItemStack storageDisk = getMainDatabase();
                    if (!(storageDisk.getItem() instanceof ItemGalaxyDatabase)) {
                        // has no data disk, can not work
                        toggleTask(Task.IDLE, null);
                        setStatusText("no galaxy database found");
                    } else {
                        if (hasEnoughEnergy && hasEnoughData) {
                            PlanetDimension planet = (PlanetDimension) DimensionManager.INSTANCE_SERVER.get(taskTarget);
                            if (planet == null) {
                                toggleTask(Task.IDLE, null); // should not happen
                            } else {
                                consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                                double p = Math.random();
                                double pTarget = (double) 3 / 20;
                                if (p < pTarget) {
                                    int maxData = planet.getDataRequiredForUnlock();
                                    extractData(REQUIRED_DATA, 1, dataTiles, false);
                                    ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(storageDisk, planet);
                                    if (info == null)
                                        info = new ItemGalaxyDatabase.PlanetInfo(); // should not happen, but just to be safe
                                    taskProgress = info.get(DataTypes.distance);
                                    guiProgressBar.setProgressAndSync((double) taskProgress / maxData);
                                    guiProgressBar.setHoverInfoAndSync("analyzing planet...");
                                    if (taskProgress < maxData) {
                                        info.put(DataTypes.distance, taskProgress + 1);
                                        ItemGalaxyDatabase.setPlanetInfo(storageDisk, planet, info);
                                    } else {
                                        // fully unlocked!
                                        if (this.lastTask == Task.SCANNING_FOR_ASTEROIDS || this.lastTask == Task.SCANNING_FOR_PLANETS)
                                            toggleTask(this.lastTask, this.lastTaskTarget);
                                        else if (this.lastTask == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED) {
                                            toggleTask(Task.SCANNING_FOR_PLANETS, null);
                                        } else {
                                            toggleTask(Task.IDLE, null);
                                        }
                                    }
                                }
                                setChanged();
                            }
                        }
                    }
                }

                if (task == Task.SYNC_STORAGE_DISKS) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                    ItemStack storageDisk1 = getMainDatabase();
                    ItemStack storageDisk2 = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_2);
                    if (!(storageDisk1.getItem() instanceof ItemGalaxyDatabase) || !(storageDisk2.getItem() instanceof ItemGalaxyDatabase)) {
                        // has no 2 data disks, can not work
                        toggleTask(this.lastTask, this.lastTaskTarget);
                    } else {
                        if (hasEnoughEnergy) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            taskProgress++;
                            guiProgressBar.setProgressAndSync((double) taskProgress / syncStorageDisksTicks);
                            guiProgressBar.setHoverInfoAndSync("sync disks...");
                            if (taskProgress > syncStorageDisksTicks) {

                                HashMap<String, ItemGalaxyDatabase.PlanetInfo> dimensionData = new HashMap<>();
                                // accumulate all known planets and their unlock points
                                for (ItemStack stack : List.of(storageDisk1, storageDisk2)) {
                                    for (String s : ItemGalaxyDatabase.getKnownDimensions(stack)) {
                                        ItemGalaxyDatabase.PlanetInfo info = new ItemGalaxyDatabase.PlanetInfo();
                                        if (dimensionData.containsKey(s)) {
                                            info = dimensionData.get(s);
                                        }
                                        ItemGalaxyDatabase.PlanetInfo newInfo = ItemGalaxyDatabase.getPlanetInfo(stack, s);
                                        if (newInfo != null) { // should usually not be null because s is in known dimensions
                                            for (String key : info.data.keySet()) {
                                                info.put(key, Math.max(info.get(key), newInfo.get(key)));
                                            }
                                        }
                                        dimensionData.put(s, info);
                                    }
                                }
                                // now write this data into both disks
                                for (ItemStack stack : List.of(storageDisk1, storageDisk2)) {
                                    for (String s : dimensionData.keySet()) {
                                        ItemGalaxyDatabase.setPlanetInfo(stack, s, dimensionData.get(s));
                                    }
                                }

                                // resume task
                                toggleTask(this.lastTask, this.lastTaskTarget);
                            }
                            setChanged();
                        }
                    }
                }

                if (task == Task.WRITE_PLANET_TO_CHIP) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                    ItemStack planetChip = itemStackHandler.getStackInSlot(PLANET_ID_CHIP_SLOT);
                    if (!(planetChip.getItem() instanceof ItemPlanetIdChip)) {
                        // has no id chip, can not work
                        toggleTask(this.lastTask, this.lastTaskTarget);
                    } else {
                        if (hasEnoughEnergy) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            taskProgress++;
                            guiProgressBar.setHoverInfoAndSync("writing to chip...");
                            guiProgressBar.setProgressAndSync((double) taskProgress / writePlanetToChipTicks);
                            if (taskProgress > writePlanetToChipTicks) {
                                boolean massKnown = false;
                                if (DimensionManager.INSTANCE_SERVER.get(taskTarget) instanceof PlanetDimension planet) {
                                    if (planet.isKnown())
                                        massKnown = true;
                                    else {
                                        ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(getMainDatabase(), taskTarget);
                                        if (info != null && info.get(DataTypes.mass) >= ItemGalaxyDatabase.POINTS_UNLOCKED(planet)) {
                                            massKnown = true;
                                        }
                                    }
                                }
                                ItemPlanetIdChip.setSelectedDimension(taskTarget, planetChip, massKnown);
                                // resume last task
                                toggleTask(this.lastTask, this.lastTaskTarget);
                            }
                            setChanged();
                        }
                    }
                }
            }
        }

        if (level.isClientSide) {
            renderData.tick(this);
        }
    }

    public void toggleTask(Task task, ResourceLocation taskTarget) {
        customStatusTimeout = 0; // reset when the task was changed
        if (this.task.equals(task)) {
            // toggle on / off for these tasks:
            if (task.equals(Task.SCANNING_FOR_PLANETS) ||
                    task.equals(Task.SCANNING_FOR_ASTEROIDS) ||
                    task.equals(Task.SYNC_STORAGE_DISKS)) {
                task = Task.IDLE;
                taskTarget = null;
            }
        }

        this.lastTask = this.task;
        this.lastTaskTarget = this.taskTarget;
        this.task = task;
        this.taskTarget = taskTarget;
        this.taskProgress = 0;

        if (task == Task.SCANNING_FOR_PLANETS) {
            ItemStack storageDisk = getMainDatabase();
            if (getNextPlanetToDiscover(storageDisk) == null) {
                // there is nothing to discover!
                // start analyzing random planets.
                // after the planet is analyzed it will toggle the last task (scan for planets) and it will check again if anything changed maybe
                analyzeRandomPlanetOrTurnOff(storageDisk);
                return;
                // code above will run toggleTask again
            }
        }

        setChanged();
        sendUpdatePacket(null);
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("task", task.ordinal());
        if (taskTarget != null) {
            tag.putString("taskTarget", taskTarget.toString());
        }
        tag.putBoolean("hasAsteroidChips", hasAsteroidChips);
        tag.putBoolean("hasFreeOutputSlots", hasFreeOutputSlots);
        tag.putBoolean("hasEnoughEnergy", hasEnoughEnergy);
        tag.putBoolean("hasEnoughData", hasEnoughData);
        return tag;
    }

    public void sendUpdatePacket(ServerPlayer player) {
        if (player != null)
            PacketDistributor.sendToPlayer(player, PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        else {
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);

        if (tag.contains("onLoad")) {
            sendUpdatePacket(player);
        }

        if (tag.contains("interact")) {
            ResourceLocation target = ResourceLocation.tryParse(tag.getString("interact"));
            if (target != null && DimensionManager.INSTANCE_SERVER.get(target) instanceof PlanetDimension planet) {
                boolean isUnlocked = planet.isKnown();
                if (!isUnlocked) {
                    ItemStack storageDisk = getMainDatabase();
                    if (ItemGalaxyDatabase.isDistanceUnlocked(storageDisk, planet)) {
                        isUnlocked = true;
                    }
                }
                if (isUnlocked) {
                    toggleTask(Task.WRITE_PLANET_TO_CHIP, target);
                } else {
                    toggleTask(Task.ANALYZE_PLANET, target);
                }

                // make player re-open normal gui
                openGui(player);
            }
        }

        if (tag.contains("setTask")) {
            String taskStr = tag.getString("setTask");
            if (taskStr.equals("syncStorageDisks"))
                toggleTask(Task.SYNC_STORAGE_DISKS, null);
            if (taskStr.equals("ScanPlanet")) {
                if (this.task.equals(Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED)) {
                    toggleTask(Task.IDLE, null);
                } else {
                    toggleTask(Task.SCANNING_FOR_PLANETS, null);
                }
            }
            if (taskStr.equals("ScanAsteroid"))
                toggleTask(Task.SCANNING_FOR_ASTEROIDS, null);
        }
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        if (tag.contains("task")) {
            task = Task.values()[tag.getInt("task")];
        }
        if (tag.contains("taskTarget")) {
            taskTarget = ResourceLocation.parse(tag.getString("taskTarget"));
        }
        if (tag.contains("hasEnoughEnergy")) {
            hasEnoughEnergy = tag.getBoolean("hasEnoughEnergy");
        }
        if (tag.contains("hasFreeOutputSlots")) {
            hasFreeOutputSlots = tag.getBoolean("hasFreeOutputSlots");
        }
        if (tag.contains("hasAsteroidChips")) {
            hasAsteroidChips = tag.getBoolean("hasAsteroidChips");
        }
        if (tag.contains("hasEnoughData")) {
            hasEnoughData = tag.getBoolean("hasEnoughData");
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("storageItemStackHandler", itemStackHandler.serializeNBT(registries));

        tag.putInt("task", task.ordinal());
        tag.putInt("taskProgress", taskProgress);
        if (taskTarget != null) {
            tag.putString("taskTarget", taskTarget.toString());
        }
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemStackHandler.deserializeNBT(registries, tag.getCompound("storageItemStackHandler"));

        task = Task.values()[tag.getInt("task")];
        taskProgress = tag.getInt("taskProgress");
        if (tag.contains("taskTarget")) {
            taskTarget = ResourceLocation.parse(tag.getString("taskTarget"));
        }
    }

    public void popInventory() {
        for (int i = 0; i < itemStackHandler.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), itemStackHandler.getStackInSlot(i));
            itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        Block block = stateInWorld.getBlock();
        if (block.equals(ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()))
            return false;
        if (block.equals(ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get()))
            return false;
        if (block.equals(ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get()))
            return false;
        if (block.equals(advRocketry.Registry.Blocks.DATA_STORAGE_BLOCK.get()))
            return false;

        return true;
    }

    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            openGui((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    public void openGui(ServerPlayer player) {
        if (level.isClientSide)
            guiHandler.openGui(176, 200, true);
        else if (player != null)
            guiHandler.signalOpenGui(player, 176, 200, true);
    }

    // helper methods for gui rendering
    public boolean client_IsDimensionKnown(ResourceLocation dimensionId) {
        return ItemGalaxyDatabase.isDimensionKnown(storageDiskSlot1.client_getItemStackToRender(), dimensionId);
    }

    public boolean client_IsDistanceUnlocked(ResourceLocation dimensionId) {
        if (DimensionManager.INSTANCE_CLIENT.get(dimensionId) instanceof PlanetDimension planetDimension)
            return ItemGalaxyDatabase.isDistanceUnlocked(storageDiskSlot1.client_getItemStackToRender(), planetDimension);
        else return false;
    }

    public ItemGalaxyDatabase.PlanetInfo client_getPlanetInfo(ResourceLocation dimensionId) {
        return ItemGalaxyDatabase.getPlanetInfo(storageDiskSlot1.client_getItemStackToRender(), dimensionId);
    }

    public ItemStack getMainDatabase() {
        return itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_1);
    }

    public enum Task {
        IDLE("idle"),
        SCANNING_FOR_PLANETS("scanning for planets"),
        SCANNING_FOR_ASTEROIDS("scanning for asteroids"),
        ANALYZE_PLANET("analyzing planet"),
        ANALYZE_PLANETS_AFTER_ALL_DISCOVERED("analyzing planet"), // will activate when scanning for planets when all is discovered
        WRITE_PLANET_TO_CHIP("writing to chip"),
        SYNC_STORAGE_DISKS("syncing storage disks"),
        ;

        public final String label;

        Task(String label) {
            this.label = label;
        }
    }

    // holds methods and variables for rendering
    public static class RenderData {
        public boolean should_open = false;
        public int openingTicks = 0;
        public int openingTicksMax = 300;

        public float yaw;
        public float yawD;
        public float yawSpeed;
        public float yawTarget;

        public float pitch;
        public float pitchD;
        public float pitchSpeed;
        public float pitchTarget;

        int actionTimeout; // for random movement, when 0 -> select a new target & speed and reset actionTimeout or wait a bit

        public static Pair<Float, Float> getYawAndPitch(Dimension targetDim, Dimension myDim, float partialTick) {
            float yaw = 0f;
            float pitch = 0f;

            // try to look to target space object
            Vec3 targetPos = targetDim.getPosition(partialTick);
            Vec3 myPos = myDim.getPosition(partialTick);

            Vector3f relative = targetPos.subtract(myPos).toVector3f();

            AxisDirections myGlobalAxis = myDim.getGlobalAxisDirections(partialTick);

            Matrix4f worldMatrix = new Matrix4f().lookAt(
                    new Vector3f(0, 0, 0),
                    myGlobalAxis.front.toVector3f(),
                    myGlobalAxis.up.toVector3f()
            );
            Vector3f relativeWorldSpace = worldMatrix.transformDirection(relative);
            relativeWorldSpace = relativeWorldSpace.normalize();

            // Since the model faces West (-X) by default:
            // We use Z for the first parameter (the "y" in standard atan2)
            // We use -X for the second parameter (the "x" in standard atan2)
            yaw = (float) Math.atan2(relativeWorldSpace.z, -relativeWorldSpace.x);

            // Math.asin(y) gives the elevation angle above the XZ plane
            pitch = (float) Math.asin(relativeWorldSpace.y);

            return Pair.of(yaw, pitch);
        }

        // Calculates the shortest difference between two angles (-180 to 180)
        private float getAngleDifference(float target, float current) {
            float diff = target - current;
            // Normalize to -180 to +180
            return (diff + 540) % 360 - 180;
        }

        void tick(EntityObservatory observatory) {
            Task task = observatory.task;

            if (should_open && openingTicks < openingTicksMax)
                openingTicks++;
            if (!should_open && openingTicks > 0)
                openingTicks--;


            if (!observatory.hasEnoughEnergy)
                observatory.noEnergyTickCounter++;
            else
                observatory.noEnergyTickCounter = 0;

            if (!observatory.hasEnoughData)
                observatory.noDataTickCounter++;
            else
                observatory.noDataTickCounter = 0;

            if (task == Task.IDLE)
                should_open = false;
            else if (task == Task.SCANNING_FOR_ASTEROIDS) {
                if (observatory.hasFreeOutputSlots && observatory.hasAsteroidChips)
                    should_open = true;
                else
                    should_open = false;
            } else if (task == Task.ANALYZE_PLANET ||
                    task == Task.SCANNING_FOR_PLANETS ||
                    task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED
            )
                should_open = true;

            // overwrite opening status if we are out of energy / data for some time
            if (observatory.noDataTickCounter > 20 * 20)
                should_open = false;
            if (observatory.noEnergyTickCounter > 20 * 20)
                should_open = false;

            if (!should_open) {
                yawTarget = 0;
                pitchTarget = 0;
                yawSpeed = 0.2f;
                pitchSpeed = 0.2f;
            } else {

                if (observatory.taskTarget != null && observatory.hasEnoughEnergy) {
                    Dimension targetDim = DimensionManager.INSTANCE_CLIENT.get(observatory.taskTarget);
                    Dimension myDim = DimensionManager.INSTANCE_CLIENT.get(observatory.getLevel().dimension().location());

                    if (targetDim != null && myDim != null && myDim != targetDim) {
                        Pair<Float, Float> yaw_pitch = getYawAndPitch(targetDim, myDim, 0);
                        if (yaw_pitch.getSecond() > 0) {
                            yawTarget = yaw_pitch.getFirst() * 180 / (float) Math.PI;
                            pitchTarget = yaw_pitch.getSecond() * 180 / (float) Math.PI;
                            actionTimeout = 200; // reset so it doesnt do other things
                            pitchSpeed = 0.2f;
                            yawSpeed = 0.2f;
                        }
                    }
                }

                actionTimeout--;
                if (actionTimeout <= 0 && observatory.hasEnoughEnergy) {
                    // choose a new action, slow movement or fast movement followed by pause
                    if (new Random().nextBoolean()) {
                        // move to a new target
                        yawTarget = (float) (Math.random() * 360);
                        pitchTarget = (float) (Math.random() * 90);
                        yawSpeed = 0.2f;
                        pitchSpeed = 0.2f;
                        actionTimeout = (int) Math.max(20 * 20, Math.random() * 20 * 30);
                    } else {
                        // make a slow move around
                        yawTarget = (float) (yaw + (Math.random() * 20)) % 360;
                        pitchTarget = (float) (pitch + (Math.random() * 10));
                        pitchTarget = Math.clamp(pitchTarget, 0, 90);
                        yawSpeed = 0.05f;
                        pitchSpeed = 0.05f;
                        actionTimeout = (int) Math.max(20 * 20, Math.random() * 20 * 30);
                    }
                }
            }
            // --- YAW LOGIC ---
            // 1. Calculate the shortest distance to the target (handles wrapping)
            float yawDiff = getAngleDifference(yawTarget, yaw);

            // 2. Check if we are close enough to reach the target this tick
            if (Math.abs(yawDiff) <= yawSpeed) {
                // We can reach the target exactly
                yawD = yawDiff;
                yaw = yawTarget;
            } else {
                // We need to move towards the target at max speed
                // Math.signum returns 1.0 for positive, -1.0 for negative
                yawD = Math.signum(yawDiff) * yawSpeed;
                yaw += yawD;
            }

            // Normalize yaw to keep it within 0-360 range
            if (yaw > 0) yaw -= 360;
            if (yaw < 0) yaw += 360;


            // --- PITCH LOGIC ---
            // 1. Calculate difference, use normal diff because pitch can not wrap around
            float pitchDiff = pitchTarget - pitch;

            // 2. Check for overshoot
            if (Math.abs(pitchDiff) <= pitchSpeed) {
                pitchD = pitchDiff;
                pitch = pitchTarget;
            } else {
                pitchD = Math.signum(pitchDiff) * pitchSpeed;
                pitch += pitchD;
            }
        }
    }
}
