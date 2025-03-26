package ARMachines.lathe;


import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.IGuiHandler;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import ARLib.obj.ModelFormatException;
import ARLib.obj.WavefrontObject;
import ARLib.utils.MachineRecipe;
import ARLib.utils.MultiblockMachineRecipeManager;
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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ARLib.ARLibRegistry.*;
import static ARMachines.MultiblockRegistry.BLOCK_LATHE;
import static ARMachines.MultiblockRegistry.ENTITY_LATHE;

public class EntityLathe extends EntityMultiblockMachineMaster {

    // defines what blocks are valid for a char in the structure
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();
    // structure is defined by char / Block objects. char objects can have multiple valid blocks
    // "c" is ALWAYS used for the controller/master block.
    public     static Object[][][] structure = {
            {{'c', BLOCK_MOTOR.get(), Blocks.AIR, 'I'}},
            {{'P', BLOCK_STRUCTURE.get(), BLOCK_STRUCTURE.get(), 'O'}},
    };
    // setup all the blocks that can be used for a char in the structure
    // I is item input, O is item output, P is power input
    static {
        // "c" is ALWAYS used for the controller/master block.
        List<Block> c = new ArrayList<>();
        c.add(BLOCK_LATHE.get());
        charMapping.put('c', c);

        List<Block> I = new ArrayList<>();
        I.add(BLOCK_ITEM_INPUT_BLOCK.get());
        charMapping.put('I', I);

        List<Block> O = new ArrayList<>();
        O.add(BLOCK_ITEM_OUTPUT_BLOCK.get());
        charMapping.put('O', O);

        List<Block> P = new ArrayList<>();
        P.add(BLOCK_ENERGY_INPUT_BLOCK.get());
        charMapping.put('P', P);
    }
    @Override
    public  Object[][][] getStructure() {
        return structure;
    }
    @Override
    public  HashMap<Character, List<Block>> getCharMapping(){
        return charMapping;
    }



    // this is used for gui
    GuiHandlerBlockEntity guiHandler;

    // self explaining
    boolean isRunning;

    MultiblockMachineRecipeManager<EntityLathe> recipeManager;

    // this is on client side, the client has no recipe manager but it still needs to know
    // the progress and current recipe total time for rendering the multiblock
    int client_recipeMaxTime = 1;
    int client_recipeProgress = 0;
    boolean client_hasRecipe = false;

    guiModuleProgressBarHorizontal6px progressBar6px;

    public EntityLathe(BlockPos pos, BlockState state) {
        super(ENTITY_LATHE.get(), pos, state);
        this.forwardInteractionToMaster = true; // makes the master gui open no matter what block of the multiblock is clicked
        recipeManager = new MultiblockMachineRecipeManager<>(this);
        recipeManager.recipes = LatheConfig.INSTANCE.recipes;
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
    // this method will be called if the structure goes from incomplete to completes.
    // this usually happens when the master block is clicked && the multiblock is not complete
    // it will also be called onLoad() if the structure is scanned & completed OR on client side, if the structure is completed (blockstate value checked)
    public void onStructureComplete() {
        super.onStructureComplete();

        // create a empty guiHandler
        guiHandler = new GuiHandlerBlockEntity(this);

        // client has no idea about the input/output tiles. only server needs to know them
        // they are not needed on the client side of the gui. do not try to access the tiles on the clientside, they do not exist here

        // 4 slots for the input block
        // every sot has a groupId and a instantTransferId - this way you can specify what slots will be targeted on instant-item-transfer during shift click
        guiModuleItemHandlerSlot slotI1 = new guiModuleItemHandlerSlot(1, level.isClientSide ? null : this.itemInTiles.get(0), 0, 1, 0, guiHandler, 50, 10);
        guiModuleItemHandlerSlot slotI2 = new guiModuleItemHandlerSlot(2, level.isClientSide ? null : this.itemInTiles.get(0), 1, 1, 0, guiHandler, 50, 30);
        guiModuleItemHandlerSlot slotI3 = new guiModuleItemHandlerSlot(3, level.isClientSide ? null : this.itemInTiles.get(0), 2, 1, 0, guiHandler, 70, 10);
        guiModuleItemHandlerSlot slotI4 = new guiModuleItemHandlerSlot(4, level.isClientSide ? null : this.itemInTiles.get(0), 3, 1, 0, guiHandler, 70, 30);
        guiHandler.getModules().add(slotI1);
        guiHandler.getModules().add(slotI2);
        guiHandler.getModules().add(slotI3);
        guiHandler.getModules().add(slotI4);

        // 4 slots for the output block
        guiModuleItemHandlerSlot slotO1 = new guiModuleItemHandlerSlot(5, level.isClientSide ? null : this.itemOutTiles.get(0), 0, 2, 0, guiHandler, 130, 10);
        guiModuleItemHandlerSlot slotO2 = new guiModuleItemHandlerSlot(6, level.isClientSide ? null : this.itemOutTiles.get(0), 1, 2, 0, guiHandler, 130, 30);
        guiModuleItemHandlerSlot slotO3 = new guiModuleItemHandlerSlot(7, level.isClientSide ? null : this.itemOutTiles.get(0), 2, 2, 0, guiHandler, 110, 10);
        guiModuleItemHandlerSlot slotO4 = new guiModuleItemHandlerSlot(8, level.isClientSide ? null : this.itemOutTiles.get(0), 3, 2, 0, guiHandler, 110, 30);
        guiHandler.getModules().add(slotO1);
        guiHandler.getModules().add(slotO2);
        guiHandler.getModules().add(slotO3);
        guiHandler.getModules().add(slotO4);

        guiModuleEnergy energyBar = new guiModuleEnergy(9, level.isClientSide ? null : this.energyInTiles.get(0), guiHandler, 10, 10);
        guiHandler.getModules().add(energyBar);

        // create the hotbar slots first, inventory-instant-item-transfer will try slots by the order they were registered
        List<guiModulePlayerInventorySlot> playerHotBar = guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 100, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerHotBar)
            guiHandler.getModules().add(i);

        List<guiModulePlayerInventorySlot> playerInventory = guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 70, 200, 0, 1, this.guiHandler);
        for (guiModulePlayerInventorySlot i : playerInventory)
            guiHandler.getModules().add(i);


        progressBar6px = new guiModuleProgressBarHorizontal6px(-1, 0xFFF0F0F0, guiHandler, 60, 55);
        guiHandler.getModules().add(progressBar6px);

        guiHandler.getModules().add(new guiModuleImage(guiHandler, 90, 20, 16, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 16, 12));
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

    void getUpdateTag(CompoundTag info){
        info.putBoolean("isRunning", this.isRunning);
        info.putInt("recipeProgress", recipeManager.progress);
        info.putBoolean("hasRecipe", recipeManager.currentRecipe != null);
        if(recipeManager.currentRecipe != null) {
            info.putInt("recipeTime", recipeManager.currentRecipe.ticksRequired);
        }
        info.putLong("time", System.currentTimeMillis());
    }

    // used on serverside, will notify the client that the machine is running or not
    // and sends the recipe time and current progress.
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
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        super.readClient(tag);
        if (tag.contains("openGui")) {
            this.guiHandler.openGui(176, 165,true);
        }
        if(tag.contains("time") && tag.getLong("time") > lastUpdateTime) {
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
        EntityLathe t1 = (EntityLathe) t;
        t1.tick();
    }
}
