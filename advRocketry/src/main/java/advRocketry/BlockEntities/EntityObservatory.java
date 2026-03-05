package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import advRocketry.Config;
import advRocketry.Data.DataTypes;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemGalaxyStorageDisk;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Registry;
import advRocketry.Render.starmap.SpaceMapScreen;
import advRocketry.utils.AxisDirections;
import advRocketry.utils.ClientUtils;
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

public class EntityObservatory extends EntityMultiblockMachineMasterWithData {

    public static String DATA_SCANNING_FOR_PLANETS = DataTypes.distance;

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

            // for planet write and disk sync i do not change open/close and rotation states

            // close it for this task
            if (task == Task.IDLE) {
                should_open = false;
                yawTarget = 0;
                pitchTarget = 0;
                yawSpeed = 0.2f;
                pitchSpeed = 0.2f;
            }
            // open and animate for these tasks
            if (task == Task.ANALYZE_PLANET ||
                    task == Task.SCANNING_FOR_PLANETS ||
                    task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED ||
                    task == Task.SCANNING_FOR_ASTEROIDS) {

                should_open = true;

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
                    if(new Random().nextBoolean()){
                        // move to a new target
                        yawTarget = (float) (Math.random() * 360);
                        pitchTarget = (float) (Math.random() * 90);
                        yawSpeed = 0.2f;
                        pitchSpeed = 0.2f;
                        actionTimeout = (int) Math.max(20 * 20, Math.random() * 20 * 30);
                    }else{
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
    }

    public RenderData renderData = new RenderData();

    public ItemStackHandler itemStackHandler;
    int STORAGE_DISK_SLOT_1 = 0;
    int STORAGE_DISK_SLOT_2 = 1;
    int PLANET_ID_CHIP_SLOT = 2;


    public Task task = Task.IDLE;
    public ResourceLocation taskTarget = null;
    public Task lastTask = Task.IDLE;
    public ResourceLocation lastTaskTarget = null;
    public int taskProgress;
    public static int writePlanetToChipTicks = 20 * 5;
    public static int syncStorageDisksTicks = 20 * 10;

    boolean hasEnoughEnergy = false;

    public GuiHandlerBlockEntity guiHandler;
    ARLib.gui.modules.guiModuleItemHandlerSlot storageDiskSlot1;
    ARLib.gui.modules.guiModuleItemHandlerSlot storageDiskSlot2;
    ARLib.gui.modules.guiModuleItemHandlerSlot planetIdChipSlot;
    ARLib.gui.modules.guiModuleButton scanPlanetBtn;
    ARLib.gui.modules.guiModuleButton scanAsteroidBtn;
    ARLib.gui.modules.guiModuleButton syncStorageDisksBtn;
    ARLib.gui.modules.guiModuleProgressBarHorizontal6px guiProgressBar;
    ARLib.gui.modules.guiModuleVerticalProgressBar energyBar;
    ARLib.gui.modules.guiModuleText statusText;
    int customStatusTimeout = 0;


    public EntityObservatory(BlockPos pos, BlockState state) {
        super(Registry.ENTITY_OBSERVATORY.get(), pos, state);
        //super.forwardInteractionToMaster = true;

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.maxDistance = 16;

        itemStackHandler = new ItemStackHandler(3) {
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == STORAGE_DISK_SLOT_1 || slot == STORAGE_DISK_SLOT_2)
                    return stack.getItem().equals(Registry.ITEM_GALAXY_STORAGE_DISK.get());
                if (slot == PLANET_ID_CHIP_SLOT)
                    return stack.getItem().equals(Registry.ITEM_PLANET_ID_CHIP.get());
                return false;
            }

            public void onContentsChanged(int slot) {
                // move from storage slot 2 to slot 1 if slot 1 is empty
                if (slot == STORAGE_DISK_SLOT_2 || slot == STORAGE_DISK_SLOT_1) {
                    if (getStackInSlot(STORAGE_DISK_SLOT_1).isEmpty() && !getStackInSlot(STORAGE_DISK_SLOT_2).isEmpty()) {
                        insertItem(STORAGE_DISK_SLOT_1, extractItem(STORAGE_DISK_SLOT_2, getStackInSlot(STORAGE_DISK_SLOT_2).getCount(), false), false);
                    }
                }

                // make sure the current planet is always unlocked
                // also make sure it displays known-by-default planets as known, so add them as unlocked planets
                ItemStack stack = getStackInSlot(slot);
                if (stack.getItem() instanceof ItemGalaxyStorageDisk && level != null) {
                    for (Dimension dim : DimensionManager.INSTANCE_SERVER.dimensions.values()) {
                        if (dim instanceof PlanetDimension planetDimension) {
                            if (dim.getDimensionId().equals(level.dimension().location()) || planetDimension.isKnown())
                                ItemGalaxyStorageDisk.setUnlockPoints(stack, dim.getDimensionId().toString(), ItemGalaxyStorageDisk.POINTS_UNLOCKED());
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
                new ARLib.gui.modules.guiModuleText(3, "galaxy data storage:", guiHandler, 10, 153, 0xff000000, false)
        );

        planetIdChipSlot = new guiModuleItemHandlerSlot(4, itemStackHandler, PLANET_ID_CHIP_SLOT, 1, 0, guiHandler, 150, 130);
        guiHandler.modules.add(planetIdChipSlot);
        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleText(5, "planet id chip:", guiHandler, 10, 133, 0xff000000, false)
        );

        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            guiHandler.modules.add(
                    new ARLib.gui.modules.guiModuleButton(100, "open galaxy", guiHandler, 10, 10, 70, 15, BTN_BLACK, BTN_W, BTN_H) {
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

                                            if (!planet.isKnown() && clientGetDiscoverStatusFromCurrentStorageItem(dimensionId) != ItemGalaxyStorageDisk.POINTS_UNLOCKED()) {
                                                // if the planet is not known but still rendered / clicked, assume the storage disk is inserted, no need to check
                                                return "Analyze";
                                            }

                                            if (!planetIdChipSlot.client_getItemStackToRender().isEmpty()) {
                                                return "burn to chip";
                                            }
                                            return "";
                                        }

                                        public String getPlanetInfoText(ResourceLocation dimensionId) {
                                            PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
                                            if (planet == null) return "";

                                            if (!planet.isKnown() && clientGetDiscoverStatusFromCurrentStorageItem(dimensionId) != ItemGalaxyStorageDisk.POINTS_UNLOCKED()) {
                                                return "We require more information about this planet.";
                                            }

                                            return super.getPlanetInfoText(dimensionId);
                                        }

                                        public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
                                            Dimension d = DimensionManager.INSTANCE_CLIENT.get(dimensionId);
                                            if (d == null) return false;

                                            if (((PlanetDimension) (d)).isKnown()) {
                                                return true;
                                            }

                                            int discoverStatus = clientGetDiscoverStatusFromCurrentStorageItem(dimensionId);
                                            if (discoverStatus != -1)
                                                return true;

                                            return false;
                                        }
                                    }
                            );
                        }
                    }
            );
        }

        statusText = new ARLib.gui.modules.guiModuleText(199, "current task:", guiHandler, 10, 30, 0xff000000, false);
        guiHandler.modules.add(statusText);

        guiProgressBar = new ARLib.gui.modules.guiModuleProgressBarHorizontal6px(200, 0xffffffff, guiHandler, 10, 50);
        guiHandler.modules.add(guiProgressBar);

        scanPlanetBtn = new ARLib.gui.modules.guiModuleButton(201, "Scan for Planets", guiHandler, 10, 60, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "ScanPlanet");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(scanPlanetBtn);


        scanAsteroidBtn = new ARLib.gui.modules.guiModuleButton(202, "Scan for Asteroids", guiHandler, 10, 80, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "ScanAsteroid");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(scanAsteroidBtn);

        syncStorageDisksBtn = new ARLib.gui.modules.guiModuleButton(203, "Sync Storage Disks", guiHandler, 10, 100, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "syncStorageDisks");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(syncStorageDisksBtn);

        energyBar = new ARLib.gui.modules.guiModuleVerticalProgressBar(300, guiHandler, 155, 60);
        guiHandler.modules.add(energyBar);

        guiHandler.modules.addAll(ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 175, 10000, 0, 1, guiHandler));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("onLoad", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        } else {
            updateActionButtonStates();
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
                if (ItemGalaxyStorageDisk.isDimensionKnown(storageDisk, dimId.toString())) {
                    // check if not unlocked
                    if (!ItemGalaxyStorageDisk.isDimensionUnlocked(storageDisk, dimId.toString())) {
                        toggleTask(Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED, dimId);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void analyzeRandomPlanetOrTurnOff(ItemStack storageDisk) {
        if (!(storageDisk.getItem() instanceof ItemGalaxyStorageDisk)) {
            // has no data disk, can not work
            toggleTask(Task.IDLE, null);
            setStatusText("no galaxy storage disk found");
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
                    if (!ItemGalaxyStorageDisk.isDimensionKnown(storageDisk, dimId.toString())) {
                        // if the dimension has a parent dimension that is unknown, the parent has to be discovered first!
                        if (planetDimension.getParentDimensionId() != null) {
                            // has a parent
                            Dimension parent = DimensionManager.INSTANCE_SERVER.get(planetDimension.getParentDimensionId());
                            if (parent instanceof PlanetDimension parentPlanet) {
                                // parent is a planet
                                if (!parentPlanet.isKnown()) {
                                    // parent is not known by default
                                    if (!ItemGalaxyStorageDisk.isDimensionKnown(storageDisk, parent.getDimensionId().toString())) {
                                        // parent is also not known on disk
                                        // we can not discover the planet until parent is known
                                        continue;
                                    }
                                }
                            }
                        }
                        // TODO: artifact check, is artifact required and supplied in input hatch?

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

            if (!getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
                toggleTask(Task.IDLE, null);
            } else {

                List<EntityEnergyInputBlock> energyInputBlocks = getEnergyInputTiles();
                List<EntityDataStorageBlock> dataTiles = getDataTiles();

                int energy = this.getTotalEnergyStored(energyInputBlocks);
                int maxEnergy = this.getMaxEnergyStored(energyInputBlocks);

                boolean newHasEnoughEnergy = energy > Config.INSTANCE.observatory_Energy_Per_Tick;
                if (newHasEnoughEnergy != hasEnoughEnergy) {
                    hasEnoughEnergy = newHasEnoughEnergy;
                    sendUpdatePacket(null); // client needs to know about energy to stop the rotate animations
                }

                if (customStatusTimeout <= 0) {
                    if (!hasEnoughEnergy) {
                        statusText.setTextAndSync("OUT OF ENERGY!");
                    } else {
                        String s = "Status:\n" + task.label;
                        if(taskTarget != null){
                            if(DimensionManager.INSTANCE_SERVER.get(taskTarget) instanceof PlanetDimension targetPlanet){
                                s += ": "+targetPlanet.getName();
                            }
                        }
                        if(task == Task.SCANNING_FOR_PLANETS && getData(DATA_SCANNING_FOR_PLANETS, dataTiles) == 0){
                            s += "\n("+DATA_SCANNING_FOR_PLANETS+" data would help)";
                        }
                        statusText.setTextAndSync(s);
                    }
                } else {
                    customStatusTimeout--;
                }

                energyBar.setProgressAndSync((double) energy / maxEnergy);
                energyBar.setHoverInfoAndSync(energy + " / " + maxEnergy + " RF");

                if (task == Task.IDLE) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                }

                if (task == Task.SCANNING_FOR_PLANETS) {
                    ItemStack storageDisk = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_1);
                    if (!(storageDisk.getItem() instanceof ItemGalaxyStorageDisk)) {
                        // has no data disk, can not work
                        toggleTask(Task.IDLE, null);
                        setStatusText("no galaxy storage disk found");
                    } else {
                        guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                        if (hasEnoughEnergy) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            double p = Math.random();
                            double pTarget = Config.INSTANCE.observatory_Find_Planet_P_Per_Tick;
                            if (getData(DATA_SCANNING_FOR_PLANETS, dataTiles) > 0) {
                                super.consumeData(DATA_SCANNING_FOR_PLANETS, 1, dataTiles);
                                pTarget *= 10; // increase probability of finding something at the cost of data
                            }
                            if (p < pTarget) {
                                // discover a new random planet that is not already known
                                PlanetDimension nextToDiscover = getNextPlanetToDiscover(storageDisk);
                                if (nextToDiscover != null) {
                                    ItemGalaxyStorageDisk.setUnlockPoints(storageDisk, nextToDiscover.getDimensionId().toString(), 0); // add the planet to the list
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

                if (task == Task.SYNC_STORAGE_DISKS) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                    ItemStack storageDisk1 = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_1);
                    ItemStack storageDisk2 = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_2);
                    if (!(storageDisk1.getItem() instanceof ItemGalaxyStorageDisk) || !(storageDisk2.getItem() instanceof ItemGalaxyStorageDisk)) {
                        // has no 2 data disks, can not work
                        toggleTask(this.lastTask, this.lastTaskTarget);
                    } else {
                        if (hasEnoughEnergy) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            taskProgress++;
                            guiProgressBar.setProgressAndSync((double) taskProgress / syncStorageDisksTicks);
                            guiProgressBar.setHoverInfoAndSync("sync disks...");
                            if (taskProgress > syncStorageDisksTicks) {

                                HashMap<String, Integer> dimensionData = new HashMap<>();
                                // accumulate all known planets and their unlock points
                                for (ItemStack stack : List.of(storageDisk1, storageDisk2)) {
                                    for (String s : ItemGalaxyStorageDisk.getKnownDimensions(stack)) {
                                        dimensionData.putIfAbsent(s, 0);
                                        dimensionData.put(s, Math.max(
                                                dimensionData.get(s),
                                                ItemGalaxyStorageDisk.getUnlockPoints(stack, s)
                                        ));
                                    }
                                }
                                // now write this data into both disks
                                for (ItemStack stack : List.of(storageDisk1, storageDisk2)) {
                                    for (String s : dimensionData.keySet()) {
                                        ItemGalaxyStorageDisk.setUnlockPoints(stack, s, dimensionData.get(s));
                                    }
                                }

                                // resume task
                                toggleTask(this.lastTask, this.lastTaskTarget);
                            }
                            setChanged();
                        }
                    }
                }

                if (task == Task.ANALYZE_PLANET || task == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                    ItemStack storageDisk = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_1);
                    if (!(storageDisk.getItem() instanceof ItemGalaxyStorageDisk)) {
                        // has no data disk, can not work
                        toggleTask(Task.IDLE, null);
                        setStatusText("no galaxy storage disk found");
                    } else {
                        if (hasEnoughEnergy) {
                            consumeEnergy(Config.INSTANCE.observatory_Energy_Per_Tick, energyInputBlocks);
                            taskProgress = ItemGalaxyStorageDisk.getUnlockPoints(storageDisk, taskTarget.toString());
                            guiProgressBar.setProgressAndSync((double) taskProgress / ItemGalaxyStorageDisk.POINTS_UNLOCKED());
                            guiProgressBar.setHoverInfoAndSync("analyzing planet...");
                            if (taskProgress < ItemGalaxyStorageDisk.POINTS_UNLOCKED()) {
                                ItemGalaxyStorageDisk.setUnlockPoints(storageDisk, taskTarget.toString(), taskProgress + 1);
                            } else {
                                // fully unlocked!
                                if (this.lastTask == Task.SCANNING_FOR_ASTEROIDS || this.lastTask == Task.SCANNING_FOR_PLANETS)
                                    toggleTask(this.lastTask, this.lastTaskTarget);
                                else if(this.lastTask == Task.ANALYZE_PLANETS_AFTER_ALL_DISCOVERED){
                                    toggleTask(Task.SCANNING_FOR_PLANETS, null);
                                }
                                else {
                                    toggleTask(Task.IDLE, null);
                                }
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
                                ItemPlanetIdChip.setSelectedDimension(taskTarget, planetChip);
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

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityObservatory) t).tick();
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
            ItemStack storageDisk = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_1);
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
        updateActionButtonStates();
        sendUpdatePacket(null);
    }

    public void updateActionButtonStates() {

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
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("task", task.ordinal());
        if (taskTarget != null) {
            tag.putString("taskTarget", taskTarget.toString());
        }
        tag.putBoolean("hasEnoughEnergy", hasEnoughEnergy);
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
                    ItemStack storageDisk = itemStackHandler.getStackInSlot(STORAGE_DISK_SLOT_1);
                    if (ItemGalaxyStorageDisk.isDimensionUnlocked(storageDisk, target.toString())) {
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
        updateActionButtonStates();
    }

    public void popInventory() {
        for (int i = 0; i < itemStackHandler.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), itemStackHandler.getStackInSlot(i));
            itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }


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

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(Registry.OBSERVATORY.get()));
        charMapping.put('s', List.of(ARLibRegistry.BLOCK_STRUCTURE.get()));
        charMapping.put('t', List.of(Registry.STRUCTURE_TOWER.get()));
        charMapping.put('g', List.of(Blocks.GLASS));
        charMapping.put('a', List.of(Blocks.AIR));
        charMapping.put('m', List.of(ARLibRegistry.BLOCK_MOTOR.get()));
        charMapping.put('*', List.of(
                ARLibRegistry.BLOCK_STRUCTURE.get(),
                ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get(),
                Registry.DATA_STORAGE_BLOCK.get()
        ));
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
        if (block.equals(Registry.DATA_STORAGE_BLOCK.get()))
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
    public int clientGetDiscoverStatusFromCurrentStorageItem(ResourceLocation dimensionId) {
        return ItemGalaxyStorageDisk.getUnlockPoints(storageDiskSlot1.client_getItemStackToRender(), dimensionId.toString());
    }

    public enum Task {
        IDLE("idle"),
        SCANNING_FOR_PLANETS("scanning for planets"),
        SCANNING_FOR_ASTEROIDS("scanning for asteroids"),
        ANALYZE_PLANET("analyzing planet"),
        ANALYZE_PLANETS_AFTER_ALL_DISCOVERED("analyzing planet"), // will activate when scanning for planets when all is discovered
        WRITE_PLANET_TO_CHIP("writing planet to chip"),
        SYNC_STORAGE_DISKS("syncing storage disks"),;

        public final String label;

        Task(String label) {
            this.label = label;
        }
    }
}
