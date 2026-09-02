package ARMachines.rollingMachine;


import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import ARLib.utils.ItemFluidStacks;
import ARLib.utils.MachineRecipe;
import ARLib.utils.MultiblockMachineRecipeManager;
import ARMachines.lathe.EntityLathe;
import ARMachines.lathe.LatheConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class EntityRollingMachine extends EntityMultiblockMachineMaster {

    // defines what blocks are valid for a char in the structure
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();
    // structure is defined by char / Block objects. char objects can have multiple valid blocks
    // "c" is ALWAYS used for the controller/master block.
    public static Object[][][] structure = {
            {{'c', null, Blocks.AIR, Blocks.AIR},
                    {'I', Blocks.AIR, BLOCK_STRUCTURE.get(), Blocks.AIR},
                    {'I', Blocks.AIR, BLOCK_STRUCTURE.get(), Blocks.AIR}},

            {{'P', 'i', BLOCK_STRUCTURE.get(), null},
                    {'C', BLOCK_STRUCTURE.get(), BLOCK_STRUCTURE.get(), 'O'},
                    {'C', BLOCK_STRUCTURE.get(), BLOCK_STRUCTURE.get(), 'O'}}
    };

    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        // The new ARLib hides every structure block by default; select positions are kept visible by
        // returning false, preserving the old hideBlocks() logic (the two coil 'C' blocks at layer 1).
        if (y == 1 && z == 2 && x == 0) return false;
        if (y == 1 && z == 1 && x == 0) return false;
        return true;
    }
    // setup all the blocks that can be used for a char in the structure
    // I is item input, O is item output, P is power input
    static {
        // "c" is ALWAYS used for the controller/master block.
        List<Block> c = new ArrayList<>();
        c.add(BLOCK_ROLLINGMACHINE.get());
        charMapping.put('c', c);

        List<Block> C = new ArrayList<>();
        C.add(BLOCK_COIL_COPPER.get());
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

    // this is used for simple recipe management - you dont need to worry about recipe logic
    public MultiblockMachineRecipeManager<EntityRollingMachine> recipeManager;

    // self explaining
    boolean isRunning;

    // this is on client side, the client has no recipe manager but it still needs to know
    // the progress and current recipe total time for rendering the multiblock
    int client_recipeMaxTime = 1;
    int client_recipeProgress = 0;
    boolean client_hasRecipe = false;
    ItemFluidStacks client_nextConsumedStacks = new ItemFluidStacks();
    ItemFluidStacks client_nextProducedStacks = new ItemFluidStacks();

    // this gui module is not sync-able, it uses a set() method to set the value so we need to keep an instance of it
    guiModuleProgressBarHorizontal6px progressBar6px;



    public EntityRollingMachine(BlockPos pos, BlockState state) {
        super(ENTITY_ROLLINGMACHINE.get(), pos, state);
        this.forwardInteractionToMaster = true; // makes the master gui open no matter what block of the multiblock is clicked
        recipeManager = new MultiblockMachineRecipeManager<>(this);
        recipeManager.recipes = LatheConfig.INSTANCE.recipes; // copy static loaded recipes to the manager

        // create the guiHandler - this is only to prevent nullpointer when readClient or readServer or tick is called
        // it is just a placeholder for now
        // we fill modules only when the structure is complete
        // because we need access to the item/fluid blocks and
        // we only get this after the structure is completed
        guiHandler = new GuiHandlerBlockEntity(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {

        }
    }

    @Override
    public void onStructureComplete() {
        super.onStructureComplete();

        guiHandler = new GuiHandlerBlockEntity(this);

        guiModuleEnergy energyBar = new guiModuleEnergy(17, level.isClientSide ? null : getEnergyInputTiles().get(0).energyStorage, guiHandler, 10, 10);
        guiHandler.getModules().add(energyBar);

        guiModuleFluidTankDisplay fluidInput = new guiModuleFluidTankDisplay(18,level.isClientSide ? null :getFluidInTiles().get(0).myTank,0,guiHandler,34,10);
        guiHandler.getModules().add(fluidInput);
        guiModuleItemHandlerSlot fluidInSlot = new guiModuleItemHandlerSlot(19, level.isClientSide ? null : getFluidInTiles().get(0).inventory, 0, 1, 0, guiHandler, 50, 10);
        fluidInSlot.setSlotBackground(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background_bucket.png"), 18,18);
        guiModuleItemHandlerSlot fluidOutSlot = new guiModuleItemHandlerSlot(20, level.isClientSide ? null : getFluidInTiles().get(0).inventory, 1, 1, 0, guiHandler, 50, 45);
        guiHandler.getModules().add(fluidInSlot);
        guiHandler.getModules().add(fluidOutSlot);
        guiHandler.getModules().add(new guiModuleImage(guiHandler, 50, 30, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_down.png"), 16, 12));

        // 8 slots for the input block
        // every sot has a groupId and a instantTransferId - this way you can specify what slots will be targeted on instant-item-transfer during shift click
        guiModuleItemHandlerSlot slotI1 = new guiModuleItemHandlerSlot(1, level.isClientSide ? null : getItemInTiles().get(0).inventory, 0, 1, 0, guiHandler, 90, 50);
        guiModuleItemHandlerSlot slotI2 = new guiModuleItemHandlerSlot(2, level.isClientSide ? null : getItemInTiles().get(0).inventory, 1, 1, 0, guiHandler, 90, 70);
        guiModuleItemHandlerSlot slotI3 = new guiModuleItemHandlerSlot(3, level.isClientSide ? null : getItemInTiles().get(0).inventory, 2, 1, 0, guiHandler, 110, 50);
        guiModuleItemHandlerSlot slotI4 = new guiModuleItemHandlerSlot(4, level.isClientSide ? null : getItemInTiles().get(0).inventory, 3, 1, 0, guiHandler, 110, 70);
        guiHandler.getModules().add(slotI1);
        guiHandler.getModules().add(slotI2);
        guiHandler.getModules().add(slotI3);
        guiHandler.getModules().add(slotI4);
        guiModuleItemHandlerSlot slotI5 = new guiModuleItemHandlerSlot(5, level.isClientSide ? null : getItemInTiles().get(1).inventory, 0, 1, 0, guiHandler, 90, 10);
        guiModuleItemHandlerSlot slotI6 = new guiModuleItemHandlerSlot(6, level.isClientSide ? null : getItemInTiles().get(1).inventory, 1, 1, 0, guiHandler, 90, 30);
        guiModuleItemHandlerSlot slotI7 = new guiModuleItemHandlerSlot(7, level.isClientSide ? null : getItemInTiles().get(1).inventory, 2, 1, 0, guiHandler, 110, 10);
        guiModuleItemHandlerSlot slotI8 = new guiModuleItemHandlerSlot(8, level.isClientSide ? null : getItemInTiles().get(1).inventory, 3, 1, 0, guiHandler, 110, 30);
        guiHandler.getModules().add(slotI5);
        guiHandler.getModules().add(slotI6);
        guiHandler.getModules().add(slotI7);
        guiHandler.getModules().add(slotI8);

        // 8 slots for the output block
        guiModuleItemHandlerSlot slotO1 = new guiModuleItemHandlerSlot(9, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 0, 2, 0, guiHandler, 150, 50);
        guiModuleItemHandlerSlot slotO2 = new guiModuleItemHandlerSlot(10, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 1, 2, 0, guiHandler, 150, 70);
        guiModuleItemHandlerSlot slotO3 = new guiModuleItemHandlerSlot(11, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 2, 2, 0, guiHandler, 170, 50);
        guiModuleItemHandlerSlot slotO4 = new guiModuleItemHandlerSlot(12, level.isClientSide ? null : getItemOutTiles().get(0).inventory, 3, 2, 0, guiHandler, 170, 70);
        guiHandler.getModules().add(slotO1);
        guiHandler.getModules().add(slotO2);
        guiHandler.getModules().add(slotO3);
        guiHandler.getModules().add(slotO4);
        guiModuleItemHandlerSlot slotO5 = new guiModuleItemHandlerSlot(13, level.isClientSide ? null : getItemOutTiles().get(1).inventory, 0, 2, 0, guiHandler, 150, 10);
        guiModuleItemHandlerSlot slotO6 = new guiModuleItemHandlerSlot(14, level.isClientSide ? null : getItemOutTiles().get(1).inventory, 1, 2, 0, guiHandler, 150, 30);
        guiModuleItemHandlerSlot slotO7 = new guiModuleItemHandlerSlot(15, level.isClientSide ? null : getItemOutTiles().get(1).inventory, 2, 2, 0, guiHandler, 170, 10);
        guiModuleItemHandlerSlot slotO8 = new guiModuleItemHandlerSlot(16, level.isClientSide ? null : getItemOutTiles().get(1).inventory, 3, 2, 0, guiHandler, 170, 30);
        guiHandler.getModules().add(slotO5);
        guiHandler.getModules().add(slotO6);
        guiHandler.getModules().add(slotO7);
        guiHandler.getModules().add(slotO8);

        // create the hotbar slots first, inventory-instant-item-transfer will try slots by the order they were registered
        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(17, 180, 100, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar)
            guiHandler.getModules().add(i);

        List<guiModulePlayerInventorySlot> playerInventory = guiModulePlayerInventorySlot.makePlayerInventoryModules(17, 110, 200, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInventory)
            guiHandler.getModules().add(i);


        progressBar6px = new guiModuleProgressBarHorizontal6px(-1, 0xFFF0F0F0, guiHandler, 10, 80);
        guiHandler.getModules().add(progressBar6px);

        guiHandler.getModules().add(new guiModuleImage(guiHandler, 130, 30, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 16, 12));

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

    // we sync isRunning, recipeProgress, recipe time and inputs/outputs to be able to render / animate the machine
    void getUpdateTag(CompoundTag info) {
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
        info.putLong("time", System.currentTimeMillis());
    }


    void setIsRunning(boolean isrunning) {
        if (this.isRunning != isrunning) {
            this.isRunning = isrunning;
            CompoundTag info = new CompoundTag();
            getUpdateTag(info);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);
        super.readServer(tag, player);
        if (tag.contains("client_onload")) {
            CompoundTag info = new CompoundTag();
            getUpdateTag(info);
            PacketDistributor.sendToPlayer(player, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    long lastUpdateTime = 0; // because network packets can come in different order from what they are sent

    @Override
    // incoming nbt from network to this client will be received here
    public void readClient(CompoundTag tag) {
        // dont forget to pass the tag to the guiHandler as it uses the same interface to update the gui elements
        guiHandler.readClient(tag);

        // the super class uses this method too to update the status of is stucture complete
        // we do not need to call the super method in readServer as it is empty there
        super.readClient(tag);

        // this are the commands I use in communication
        if (tag.contains("openGui")) {
            this.guiHandler.openGui(200, 210,true);
        }
        if (tag.contains("time") && tag.getLong("time") > lastUpdateTime) {
            lastUpdateTime = tag.getLong("time");
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
            if (tag.contains("nextConsumedStacks")) {
                CompoundTag nextConsumedStacks = tag.getCompound("nextConsumedStacks");
                client_nextConsumedStacks.fromNBT(nextConsumedStacks, level.registryAccess());
            }
            if (tag.contains("nextProducedStacks")) {
                CompoundTag nextProducedStacks = tag.getCompound("nextProducedStacks");
                client_nextProducedStacks.fromNBT(nextProducedStacks, level.registryAccess());
            }
        }
    }

    public void tick(){
        if (!level.isClientSide) {
            // update the guiHandler, it checks if anything has changed in the gui and sends changes to the clients tracking the gui
            guiHandler.serverTick();

            if (getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
                // if the machine is complete, let the recipe manager do it's job.
                // it will automatically scan for recipes and process them
                // it will return true if it is working and false if it has no work or is unable to work
                setIsRunning(recipeManager.update());

                if (recipeManager.currentRecipe != null)
                    progressBar6px.setProgressAndSync((double) recipeManager.progress / recipeManager.currentRecipe.ticksRequired);
            }
        }

        if (level.isClientSide) {
            if (isRunning) {
                client_recipeProgress++;
            }
        }
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        EntityRollingMachine t1 = (EntityRollingMachine) t;
        t1.tick();
    }
}
