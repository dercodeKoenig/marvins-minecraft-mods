package ARMachines.crystallizer;


import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.*;
import ARLib.network.PacketBlockEntity;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import ARLib.utils.ItemFluidStacks;
import ARLib.utils.MachineRecipe;
import ARLib.utils.MultiblockMachineRecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ARLib.ARLibRegistry.*;
import static ARMachines.MultiblockRegistry.*;


public class EntityCrystallizer extends EntityMultiblockMachineMaster {

    // defines what blocks are valid for a char in the structure
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();
    // structure is defined by char / Block objects. char objects can have multiple valid blocks
    // "c" is ALWAYS used for the controller/master block.
    public static Object[][][] structure = {
            {{'C', 'C', 'C'}, {'C', 'C', 'C'}},
            {{'O', 'c', 'I'}, {'o', 'P', 'i'}},
    };

    // setup all the blocks that can be used for a char in the structure
    static {
        // "c" is ALWAYS used for the controller/master block.
        List<Block> c = new ArrayList<>();
        c.add(BLOCK_CRYSTALLIZER.get());
        charMapping.put('c', c);

        List<Block> C = new ArrayList<>();
        C.add(Blocks.CAULDRON);
        charMapping.put('C', C);

        List<Block> I = new ArrayList<>();
        I.add(BLOCK_ITEM_INPUT_BLOCK.get());
        charMapping.put('I', I);

        List<Block> i = new ArrayList<>();
        i.add(BLOCK_FLUID_INPUT_BLOCK.get());
        charMapping.put('i', i);

        List<Block> O = new ArrayList<>();
        O.add(BLOCK_ITEM_OUTPUT_BLOCK.get());
        charMapping.put('O', O);

        List<Block> o = new ArrayList<>();
        o.add(BLOCK_FLUID_OUTPUT_BLOCK.get());
        charMapping.put('o', o);

        List<Block> P = new ArrayList<>();
        P.add(BLOCK_ENERGY_INPUT_BLOCK.get());
        charMapping.put('P', P);
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    GuiHandlerBlockEntity guiHandler;


    class working_status {
        boolean isRunning;
        int client_recipeMaxTime = 1;
        int client_recipeProgress = 0;
        boolean client_hasRecipe = false;
        ItemFluidStacks client_nextConsumedStacks = new ItemFluidStacks();
        ItemFluidStacks client_nextProducedStacks = new ItemFluidStacks();
        MultiblockMachineRecipeManager<EntityCrystallizer> recipeManager;
        guiModuleProgressBarHorizontal6px progressBar6px;

        public working_status(MultiblockMachineRecipeManager<EntityCrystallizer> recipeManager) {
            this.recipeManager = recipeManager;
        }

        void tick() {
            if (isRunning) {
                if (level.isClientSide) {
                    client_recipeProgress++;
                    if (client_recipeProgress >= client_recipeMaxTime) {
                        isRunning = false;
                    }
                } else {
                    if (recipeManager.currentRecipe != null) {
                        progressBar6px.setProgressAndSync((double) recipeManager.progress / recipeManager.currentRecipe.ticksRequired);
                    }
                    else{
                        progressBar6px.setProgressAndSync(0);
                    }
                }
            }
        }

        CompoundTag getUpdateTag() {
            CompoundTag info = new CompoundTag();
            info.putBoolean("isRunning", this.isRunning);
            info.putInt("recipeProgress", recipeManager.progress);
            info.putBoolean("hasRecipe", recipeManager.currentRecipe != null);
            if (recipeManager.currentRecipe != null) {
                info.putInt("recipeTime", recipeManager.currentRecipe.ticksRequired);
                ItemFluidStacks usedStacks = consumeInput(recipeManager.currentRecipe.inputs, true, getFluidInTiles(), getItemInTiles());
                CompoundTag usedStacksNBT = new CompoundTag();
                usedStacks.toNBT(usedStacksNBT, level.registryAccess());
                info.put("nextConsumedStacks", usedStacksNBT);

                ItemFluidStacks nextProducedStacks = recipeManager.getNextProducedItems();
                CompoundTag nextProducedStacksNBT = new CompoundTag();
                nextProducedStacks.toNBT(nextProducedStacksNBT, level.registryAccess());
                info.put("nextProducedStacks", nextProducedStacksNBT);
            }
            return info;
        }

        void readUpdateTag(CompoundTag tag) {
            if (tag.contains("isRunning")) {
                this.isRunning = tag.getBoolean("isRunning");
            }
            if (tag.contains("recipeProgress")) {
                client_recipeProgress = tag.getInt("recipeProgress");
            }
            if (tag.contains("hasRecipe")) {
                client_hasRecipe = tag.getBoolean("hasRecipe");
            }
            if (tag.contains("recipeTime")) {
                client_recipeMaxTime = tag.getInt("recipeTime");
            }
            if (tag.contains("nextProducedStacks")) {
                CompoundTag nextProducedStacks = tag.getCompound("nextProducedStacks");
                client_nextProducedStacks.fromNBT(nextProducedStacks, level.registryAccess());
            }
            if (tag.contains("nextConsumedStacks")) {
                CompoundTag nextConsumedStacks = tag.getCompound("nextConsumedStacks");
                client_nextConsumedStacks.fromNBT(nextConsumedStacks, level.registryAccess());
            }
        }
    }

    public MultiRecipeManager<EntityCrystallizer> multiRecipeManager;
    public MultiblockMachineRecipeManager<EntityCrystallizer> recipeManager1;
    public MultiblockMachineRecipeManager<EntityCrystallizer> recipeManager2;
    public MultiblockMachineRecipeManager<EntityCrystallizer> recipeManager3;
    // 3 tanks
    working_status tank1;
    working_status tank2;
    working_status tank3;

    public EntityCrystallizer(BlockPos pos, BlockState state) {
        super(ENTITY_CRYSTALLIZER.get(), pos, state);
        recipeManager1 = new MultiblockMachineRecipeManager<>(this);
        recipeManager2 = new MultiblockMachineRecipeManager<>(this);
        recipeManager3 = new MultiblockMachineRecipeManager<>(this);

        this.forwardInteractionToMaster = true;

        recipeManager1.recipes = CrystallizerConfig.INSTANCE.recipes;
        recipeManager2.recipes = CrystallizerConfig.INSTANCE.recipes;
        recipeManager3.recipes = CrystallizerConfig.INSTANCE.recipes;

        tank1 = new working_status(recipeManager1);
        tank2 = new working_status(recipeManager2);
        tank3 = new working_status(recipeManager3);

        List<MultiblockMachineRecipeManager<EntityCrystallizer>> recipeManagers = new ArrayList<>();
        recipeManagers.add(recipeManager1);
        recipeManagers.add(recipeManager2);
        recipeManagers.add(recipeManager3);
        multiRecipeManager = new MultiRecipeManager<>(recipeManagers);

        guiHandler = new GuiHandlerBlockEntity(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {

        }
    }


    @Override
    public void onStructureComplete() {
        super.onStructureComplete();

        // create a empty guiHandler
        guiHandler = new GuiHandlerBlockEntity(this);

        //energy
        guiModuleEnergy energyBar = new guiModuleEnergy(17, level.isClientSide ? null : getEnergyInputTiles().get(0).energyStorage, guiHandler, 10, 10);
        guiHandler.getModules().add(energyBar);

        //fluid input
        guiModuleFluidTankDisplay fluidInput = new guiModuleFluidTankDisplay(18, level.isClientSide ? null : getFluidInTiles().get(0).myTank, 0, guiHandler, 50, 10);
        guiHandler.getModules().add(fluidInput);
        guiModuleItemHandlerSlot fluidInSlot = new guiModuleItemHandlerSlot(19, level.isClientSide ? null : getFluidInTiles().get(0).inventory, 0, 1, 0, guiHandler, 30, 10);
        fluidInSlot.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18, 18);
        guiModuleItemHandlerSlot fluidOutSlot = new guiModuleItemHandlerSlot(20, level.isClientSide ? null : getFluidInTiles().get(0).inventory, 1, 1, 0, guiHandler, 30, 45);
        guiHandler.getModules().add(fluidInSlot);
        guiHandler.getModules().add(fluidOutSlot);
        guiHandler.getModules().add(new guiModuleImage(guiHandler, 30, 30, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png"), 16, 12));

        //fluid output
        guiModuleFluidTankDisplay fluidOutput = new guiModuleFluidTankDisplay(21, level.isClientSide ? null : getFluidOutTiles().get(0).myTank, 0, guiHandler, 174, 10);
        guiHandler.getModules().add(fluidOutput);
        guiModuleItemHandlerSlot fluidInSlot2 = new guiModuleItemHandlerSlot(22, level.isClientSide ? null : getFluidOutTiles().get(0).inventory, 0, 1, 0, guiHandler, 190, 10);
        fluidInSlot2.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18, 18);
        guiModuleItemHandlerSlot fluidOutSlot2 = new guiModuleItemHandlerSlot(23, level.isClientSide ? null : getFluidOutTiles().get(0).inventory, 1, 1, 0, guiHandler, 190, 45);
        guiHandler.getModules().add(fluidInSlot2);
        guiHandler.getModules().add(fluidOutSlot2);
        guiHandler.getModules().add(new guiModuleImage(guiHandler, 190, 30, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png"), 16, 12));


        // 4 slots for the input block
        guiModuleItemHandlerSlot slotI1 = new guiModuleItemHandlerSlot(1, level.isClientSide ? null : getItemInTiles().get(0).inventory, 0, 1, 0, guiHandler, 70, 20);
        guiModuleItemHandlerSlot slotI2 = new guiModuleItemHandlerSlot(2, level.isClientSide ? null : getItemInTiles().get(0).inventory, 1, 1, 0, guiHandler, 70, 40);
        guiModuleItemHandlerSlot slotI3 = new guiModuleItemHandlerSlot(3, level.isClientSide ? null : getItemInTiles().get(0).inventory, 2, 1, 0, guiHandler, 90, 20);
        guiModuleItemHandlerSlot slotI4 = new guiModuleItemHandlerSlot(4, level.isClientSide ? null : getItemInTiles().get(0).inventory, 3, 1, 0, guiHandler, 90, 40);
        guiHandler.getModules().add(slotI1);
        guiHandler.getModules().add(slotI2);
        guiHandler.getModules().add(slotI3);
        guiHandler.getModules().add(slotI4);

        // 8 slots for the output block
        guiModuleItemHandlerSlot slotO1 = new guiModuleItemHandlerSlot(9, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 0, 2, 0, guiHandler, 130, 20);
        guiModuleItemHandlerSlot slotO2 = new guiModuleItemHandlerSlot(10, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 1, 2, 0, guiHandler, 130, 40);
        guiModuleItemHandlerSlot slotO3 = new guiModuleItemHandlerSlot(11, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 2, 2, 0, guiHandler, 150, 20);
        guiModuleItemHandlerSlot slotO4 = new guiModuleItemHandlerSlot(12, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 3, 2, 0, guiHandler, 150, 40);
        guiHandler.getModules().add(slotO1);
        guiHandler.getModules().add(slotO2);
        guiHandler.getModules().add(slotO3);
        guiHandler.getModules().add(slotO4);


        // create the hotbar slots first, inventory-instant-item-transfer will try slots by the order they were registered
        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(27, 160, 100, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar)
            guiHandler.getModules().add(i);

        List<guiModulePlayerInventorySlot> playerInventory = guiModulePlayerInventorySlot.makePlayerInventoryModules(27, 90, 200, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInventory)
            guiHandler.getModules().add(i);


        guiHandler.getModules().add(new guiModuleImage(guiHandler, 110, 33, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 16, 12));

        tank1.progressBar6px = new guiModuleProgressBarHorizontal6px(10001, 0xFFF0F0F0, guiHandler, 10, 70);
        tank2.progressBar6px = new guiModuleProgressBarHorizontal6px(10002, 0xFFF0F0F0, guiHandler, 80, 70);
        tank3.progressBar6px = new guiModuleProgressBarHorizontal6px(10003, 0xFFF0F0F0, guiHandler, 150, 70);

        guiHandler.getModules().add(tank1.progressBar6px);
        guiHandler.getModules().add(tank2.progressBar6px);
        guiHandler.getModules().add(tank3.progressBar6px);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            // when the client loads, send a packet to the server and request initial nbt required for rendering
            CompoundTag info = new CompoundTag();
            info.put("client_onload", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("openGui", new CompoundTag());
            PacketDistributor.sendToPlayer((ServerPlayer) player, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        return InteractionResult.SUCCESS;
    }


    void getUpdateTag(CompoundTag info) {
        info.put("tank1", tank1.getUpdateTag());
        info.put("tank2", tank2.getUpdateTag());
        info.put("tank3", tank3.getUpdateTag());
        info.putLong("time", System.currentTimeMillis());
    }

    void readUpdateTag(CompoundTag info) {
        tank1.readUpdateTag(info.getCompound("tank1"));
        tank2.readUpdateTag(info.getCompound("tank2"));
        tank3.readUpdateTag(info.getCompound("tank3"));
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);

        if (tag.contains("client_onload")) {
            CompoundTag info = new CompoundTag();
            getUpdateTag(info);
            PacketDistributor.sendToPlayer(player, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    long lastUpdateTime = 0; // because network packets can come in different order from what they are sent

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        super.readClient(tag);

        if (tag.contains("openGui")) {
            this.guiHandler.openGui(216, 185, true);
        }
        if (tag.contains("time") && tag.getLong("time") > lastUpdateTime) {
            lastUpdateTime = tag.getLong("time");
            readUpdateTag(tag);
        }
    }

    // this is the tick method
    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        EntityCrystallizer t1 = (EntityCrystallizer) t;
        if (!level.isClientSide) {
            t1.guiHandler.serverTick();
            if (t1.getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
                List<Boolean> isRunningList = t1.multiRecipeManager.update();
                boolean isrunning1 = isRunningList.get(0);
                boolean isrunning2 = isRunningList.get(1);
                boolean isrunning3 = isRunningList.get(2);

                boolean sendUpdate = false;
                if (t1.tank1.isRunning != isrunning1) {
                    t1.tank1.isRunning = isrunning1;
                    sendUpdate = true;
                }
                if (t1.tank2.isRunning != isrunning2) {
                    t1.tank2.isRunning = isrunning2;
                    sendUpdate = true;
                }
                if (t1.tank3.isRunning != isrunning3) {
                    t1.tank3.isRunning = isrunning3;
                    sendUpdate = true;
                }
                if (sendUpdate) {
                    CompoundTag info = new CompoundTag();
                    t1.getUpdateTag(info);
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(t1.getBlockPos()), PacketBlockEntity.getBlockEntityPacket(t1, info));
                }

            }
        }


        t1.tank1.tick();
        t1.tank2.tick();
        t1.tank3.tick();
    }
}