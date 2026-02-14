package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.network.PacketBlockEntity;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemGalaxyStorageDisk;
import advRocketry.Registry;
import advRocketry.Render.starmap.SpaceMapScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;

public class EntityObservatory extends EntityMultiblockMachineMaster {

    public MeshData meshAxle;
    public MeshData meshScope;
    public MeshData meshCasingXPlus;
    public MeshData meshCasingXMinus;
    public MeshData meshBase;

    public VertexBuffer axle;
    public VertexBuffer scope;
    public VertexBuffer casingXPlus;
    public VertexBuffer casingXMinus;
    public VertexBuffer base;

    public int lastLight;

    public boolean should_open = false;
    public int openingTicks = 0;
    public int openingTicksMax = 300;
    public float rotationTarget;

    public ItemStackHandler itemStackHandler;
    int STORAGE_DISK_SLOT_1 = 0;
    int STORAGE_DISK_SLOT_2 = 1;
    int PLANET_ID_CHIP_SLOT = 2;


    public Task task = Task.IDLE;
    public ResourceLocation taskTarget = null;

    public GuiHandlerBlockEntity guiHandler;
    ARLib.gui.modules.guiModuleItemHandlerSlot storageDiskSlot1;
    ARLib.gui.modules.guiModuleItemHandlerSlot storageDiskSlot2;
    ARLib.gui.modules.guiModuleItemHandlerSlot planetIdChipSlot;
    ARLib.gui.modules.guiModuleText currentTaskText;
    ARLib.gui.modules.guiModuleButton scanPlanetBtn;
    ARLib.gui.modules.guiModuleButton scanAsteroidBtn;
    ARLib.gui.modules.guiModuleButton syncStorageDisksBtn;
    ARLib.gui.modules.guiModuleProgressBarHorizontal6px guiProgressBar;

    ResourceLocation BTN_BLACK = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_black.png");
    ResourceLocation BTN_RED = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_red.png");
    ResourceLocation BTN_GREEN = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_green.png");
    int BTN_W = 64;
    int BTN_H = 20;


    public EntityObservatory(BlockPos pos, BlockState state) {
        super(Registry.ENTITY_OBSERVATORY.get(), pos, state);
        super.forwardInteractionToMaster = true;

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                axle = new VertexBuffer(VertexBuffer.Usage.STATIC);
                scope = new VertexBuffer(VertexBuffer.Usage.STATIC);
                casingXMinus = new VertexBuffer(VertexBuffer.Usage.STATIC);
                casingXPlus = new VertexBuffer(VertexBuffer.Usage.STATIC);
                base = new VertexBuffer(VertexBuffer.Usage.STATIC);
            });
        }

        guiHandler = new GuiHandlerBlockEntity(this);
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
                ItemStack stack = getStackInSlot(slot);
                if(stack.getItem() instanceof ItemGalaxyStorageDisk  && level != null){
                    ItemGalaxyStorageDisk.setUnlockPoints(stack,level.dimension().location().toString(),ItemGalaxyStorageDisk.UNLOCKED_POINTS);
                }

                EntityObservatory.this.setChanged();
            }
        };
        storageDiskSlot1 = new guiModuleItemHandlerSlot(0, itemStackHandler, STORAGE_DISK_SLOT_1, 1, 0, guiHandler, 130, 160);
        guiHandler.modules.add(storageDiskSlot1);
        storageDiskSlot2 = new guiModuleItemHandlerSlot(1, itemStackHandler, STORAGE_DISK_SLOT_2, 1, 0, guiHandler, 150, 160);
        guiHandler.modules.add(storageDiskSlot2);
        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleText(3, "galaxy data storage:", guiHandler, 10, 163, 0xff000000, false)
        );

        planetIdChipSlot = new guiModuleItemHandlerSlot(4, itemStackHandler, PLANET_ID_CHIP_SLOT, 1, 0, guiHandler, 150, 140);
        guiHandler.modules.add(planetIdChipSlot);
        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleText(5, "planet id chip:", guiHandler, 10, 143, 0xff000000, false)
        );

        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleButton(100, "open galaxy", guiHandler, 10, 10, 70, 15, BTN_BLACK, BTN_W, BTN_H) {
                    public void onButtonClicked() {
                        Minecraft.getInstance().setScreen(
                                new SpaceMapScreen() {
                                    @Override
                                    public void tick() {
                                        super.tick();
                                        // make sure the main gui stays in sync
                                        EntityObservatory.this.guiHandler.sendPing();
                                    }

                                    @Override
                                    public void onClose() {
                                        super.onClose();
                                        // open the main gui again
                                        openGui();
                                    }

                                    public void interact(ResourceLocation dimensionId) {
                                        CompoundTag info = new CompoundTag();
                                        info.putString("interact", dimensionId.toString());
                                        PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
                                    }

                                    public String getInteractText(ResourceLocation dimensionId) {
                                        PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));

                                        if (!planet.isKnown() && clientGetDiscoverStatusFromCurrentStorageItem(dimensionId) != ItemGalaxyStorageDisk.UNLOCKED_POINTS) {
                                            if (!storageDiskSlot1.client_getItemStackToRender().isEmpty()) {
                                                return "Analyze";
                                            }
                                        }
                                        if (!planetIdChipSlot.client_getItemStackToRender().isEmpty()) {
                                            return "burn to chip";
                                        }
                                        return "";
                                    }

                                    public String getPlanetInfoText(ResourceLocation dimensionId) {

                                        PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));

                                        if (!planet.isKnown() && clientGetDiscoverStatusFromCurrentStorageItem(dimensionId) != ItemGalaxyStorageDisk.UNLOCKED_POINTS) {
                                            return "We require more information about this planet.";
                                        }

                                        return planet.getName() + "\n" +
                                                "g:" + planet.getGravitationalMultiplier() + "\n";
                                        // todo: add more information, temperature, atm density/composition
                                    }

                                    public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
                                        Dimension d = DimensionManager.INSTANCE_CLIENT.get(dimensionId);
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

        currentTaskText = new ARLib.gui.modules.guiModuleText(199, "current task:", guiHandler, 10, 30, 0xff000000, false);
        guiHandler.modules.add(currentTaskText);

        guiProgressBar = new ARLib.gui.modules.guiModuleProgressBarHorizontal6px(200, 0xffffffff, guiHandler, 10, 40);
        guiHandler.modules.add(guiProgressBar);

        scanPlanetBtn = new ARLib.gui.modules.guiModuleButton(201, "Scan for Planets", guiHandler, 10, 50, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "ScanPlanet");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(scanPlanetBtn);


        scanAsteroidBtn = new ARLib.gui.modules.guiModuleButton(202, "Scan for Asteroids", guiHandler, 10, 70, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "ScanAsteroid");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(scanAsteroidBtn);

        syncStorageDisksBtn = new ARLib.gui.modules.guiModuleButton(203, "Sync Storage Disks", guiHandler, 10, 90, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.putString("setTask", "syncStorageDisks");
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
            }
        };
        guiHandler.modules.add(syncStorageDisksBtn);


        guiHandler.modules.addAll(ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerHotbarModules(15, 185, 10000, 0, 1, guiHandler));
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                axle.close();
                scope.close();
                casingXPlus.close();
                casingXMinus.close();
                base.close();
            });
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        CompoundTag info = new CompoundTag();
        info.put("onLoad", new CompoundTag());
        PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();


            if(!getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)){
                toggleTask(Task.IDLE, null);
            }
            else {

                if (task == Task.IDLE) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                }

                if (task == Task.SCANNING_FOR_PLANETS) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(false);
                }

                if (task == Task.SYNC_STORAGE_DISKS) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                }

                if (task == Task.ANALYZE_PLANET) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                }

                if (task == Task.WRITE_PLANET_TO_CHIP) {
                    guiProgressBar.setIsEnabledAndBroadcastUpdate(true);
                }

            }
        }

        if(level.isClientSide){
            if(task == Task.IDLE)
                should_open = false;
            if (task == Task.ANALYZE_PLANET ||
                    task == Task.WRITE_PLANET_TO_CHIP ||
                    task == Task.SCANNING_FOR_PLANETS ||
                    task == Task.SCANNING_FOR_ASTEROIDS)
                should_open = true;
            if (should_open && openingTicks < openingTicksMax)
                openingTicks++;
            if (!should_open && openingTicks > 0)
                openingTicks--;
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityObservatory) t).tick();
    }

    public void openGui() {
        guiHandler.openGui(200, 210, true);
    }

    // helper methods for gui rendering
    public int clientGetDiscoverStatusFromCurrentStorageItem(ResourceLocation dimensionId) {
        return ItemGalaxyStorageDisk.getUnlockPoints(storageDiskSlot1.client_getItemStackToRender(), dimensionId.toString());
    }

    public void toggleTask(Task task, ResourceLocation taskTarget) {
        if (this.task.equals(task)) {
            task = Task.IDLE;
            taskTarget = null;
        }
        this.task = task;
        this.taskTarget = taskTarget;
        setChanged();
        updateActionButtonStates();
        sendUpdatePacket(null);
    }

    public void updateActionButtonStates() {

        currentTaskText.setTextAndSync("Task: " + task.name());

        if (task == Task.SCANNING_FOR_PLANETS) {
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

    public CompoundTag getUpdateTag(){
        CompoundTag tag = new CompoundTag();
        tag.putInt("task", task.ordinal());
        if (taskTarget != null) {
            tag.putString("taskTarget", taskTarget.toString());
        }
        return tag;
    }

    public void sendUpdatePacket(ServerPlayer player){
        if(player != null)
            PacketDistributor.sendToPlayer(player,PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        else{
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);

        if(tag.contains("onLoad")){
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

                // make it re-open normal gui
                CompoundTag info = new CompoundTag();
                info.put("openGui", new CompoundTag());
                PacketDistributor.sendToPlayer((ServerPlayer) player, PacketBlockEntity.getBlockEntityPacket(this, info));
            }
        }

        if (tag.contains("setTask")) {
            String taskStr = tag.getString("setTask");
            if (taskStr.equals("syncStorageDisks"))
                toggleTask(Task.SYNC_STORAGE_DISKS, null);
            if (taskStr.equals("ScanPlanet"))
                toggleTask(Task.SCANNING_FOR_PLANETS, null);
            if (taskStr.equals("ScanAsteroid"))
                toggleTask(Task.SCANNING_FOR_ASTEROIDS, null);

        }
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        if (tag.contains("openGui")) {
            openGui();
        }
        if (tag.contains("task")) {
            task = Task.values()[tag.getInt("task")];
        }
        if (tag.contains("taskTarget")) {
            taskTarget = ResourceLocation.parse(tag.getString("taskTarget"));
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("storageItemStackHandler", itemStackHandler.serializeNBT(registries));

        tag.putInt("task", task.ordinal());
        if (taskTarget != null) {
            tag.putString("taskTarget", taskTarget.toString());
        }
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemStackHandler.deserializeNBT(registries, tag.getCompound("storageItemStackHandler"));

        task = Task.values()[tag.getInt("task")];
        if (tag.contains("taskTarget")) {
            taskTarget = ResourceLocation.parse(tag.getString("taskTarget"));
        }
        updateActionButtonStates();
    }

    public void popInventory() {
        for (int i = 0; i < itemStackHandler.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), itemStackHandler.getStackInSlot(i));
        }
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
                ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()
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

        return true;
    }


    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("openGui", new CompoundTag());
            PacketDistributor.sendToPlayer((ServerPlayer) player, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        return InteractionResult.SUCCESS;
    }

    public enum Task {
        IDLE,
        SCANNING_FOR_PLANETS,
        SCANNING_FOR_ASTEROIDS,
        ANALYZE_PLANET,
        WRITE_PLANET_TO_CHIP,
        SYNC_STORAGE_DISKS
    }
}
