package ARLib.gui;

import ARLib.gui.modules.GuiModuleBase;
import ARLib.network.PacketBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;


/**
 * How to use a GuiHandler on a BlockEntity
 * <p>
 * <p>
 * - create a GuiHandlerBlockEntity instance in you BlockEntity class:
 * IGuiHandler guiHandler = new GuiHandlerBlockEntity(this);
 * <p>
 * <p>
 * - register gui modules:
 * guiHandler.registerModule(
 * new guiModuleEnergy(0,[your_IEnergyStorage_object],guiHandler,10,10)
 * );
 * <p>
 * - implement INetworkTagReceiver in your BlockEntity class
 * This IGuiHandler uses PacketBlockEntity to send data to your BlockEntity.
 * You need to forward this data to the IGuiHandler:
 * <p>
 * in  readServer(CompoundTag tag) call guiHandler.readServer(CompoundTag tag)
 * in  readClient(CompoundTag tag) call guiHandler.readClient(CompoundTag tag)
 * <p>
 * - register your BlockEntity to have a tick() method
 * In IGuiHandler.serverTick(...), the server scans for changes in the gui data if one or more clients watch the gui.
 * You need to call guiHandler.serverTick([your_gui_handler_instance]) on server side every tick to allow data sync.
 * If no clients watch the gui, serverTick will instantly return to keep the code efficient and not waste time.
 * <p>
 * example to use for your Block class:
 *
 * @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
 * return EntityLathe::tick;
 * }
 * example to use for your BlockEntity class:
 * public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
 * if(!level.isClientSide)
 * ((EntityLathe)t).guiHandler.serverTick();
 * }
 * <p>
 * <p>
 * - open your gui from anywhere using [your_gui_handler_instance].openGui(), for example on block click.
 * example:
 * @Override public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
 * BlockEntity e = world.getBlockEntity(pos);
 * if (e instanceof EntityLathe ee) {
 * if (world.isClientSide) {
 * ee.guiHandler.openGui();
 * }
 * }
 * return InteractionResult.SUCCESS;
 * }
 */
public class GuiHandlerBlockEntity implements IGuiHandler {

    public Map<UUID, Integer> playersTrackingGui;
    public List<GuiModuleBase> modules;
    public int last_ping = 0;
    public BlockEntity parentBE;
    public Object screen; // this will hold modularScreen in case you need to access it but server shits itself when loading the class so i use object

    public float maxDistance = 8;

    public GuiHandlerBlockEntity(BlockEntity parentBlockEntity) {
        this.playersTrackingGui = new HashMap<>();
        modules = new ArrayList<>();
        this.parentBE = parentBlockEntity;
    }

    @Override
    public List<GuiModuleBase> getModules() {
        return modules;
    }

    public void openGui(int w, int h, boolean renderBackground) {
        sendPing();
        // fix for not syncing in creative mode, player should never be null bc this is called on client
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.inventoryMenu.setCarried(ItemStack.EMPTY);
        screen = new ModularScreen(this, w, h, renderBackground);
        Minecraft.getInstance().setScreen((Screen) screen);
    }

    @Override
    public void sendToServer(CompoundTag tag) {
        PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(parentBE, tag));
    }

    @Override
    public void broadcastUpdate(CompoundTag tag) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (UUID uid : playersTrackingGui.keySet()) {
                ServerPlayer p = server.getPlayerList().getPlayer(uid);
                if (p != null) {
                    PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(parentBE, tag));
                }
            }
        }
    }

    void removePlayerFromGui(UUID uid) {
        playersTrackingGui.remove(uid);
        Player p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uid);
        if (p != null) {
            dropPlayersCarriedItem(p);
        }
    }

    @Override
    public void readServer(CompoundTag tag) {
        IGuiHandler.super.readServer(tag);

        if (tag.contains("guiPing")) {
            UUID uid = tag.getUUID("guiPing");
            // update data asap when a client opens the gui new
            if (!playersTrackingGui.containsKey(uid)) {
                CompoundTag guiData = new CompoundTag();
                for (GuiModuleBase guiModule : getModules()) {
                    guiModule.server_writeDataToSyncToClient(guiData);
                }
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                ServerPlayer p = server.getPlayerList().getPlayer(uid);
                if (p != null) {
                    PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(parentBE, guiData));
                }
            }
            playersTrackingGui.put(uid, 0);
        }
        if (tag.contains("closeGui")) {
            UUID uid = tag.getUUID("closeGui");
            // a client said he no longer has the gui open
            if (playersTrackingGui.containsKey(uid)) {
                removePlayerFromGui(uid);
            }
        }
    }

    @Override
    public void readClient(CompoundTag tag) {
        IGuiHandler.super.readClient(tag);
        if (tag.contains("guiHandler_closeGui"))
            if(screen instanceof Screen)
                ((Screen) screen).onClose();
        if(tag.contains("guiHandler_openGui")){
            CompoundTag openGuiTag = tag.getCompound("guiHandler_openGui");
            int w = openGuiTag.getInt("w");
            int h = openGuiTag.getInt("h");
            boolean renderBackground = openGuiTag.getBoolean("renderBackground");
            openGui(w,h,renderBackground);
        }
    }

    public void signalCloseGui(ServerPlayer player){
        CompoundTag closeGuiTag = new CompoundTag();
        closeGuiTag.putInt("guiHandler_closeGui", 0);
        PacketDistributor.sendToPlayer(player, PacketBlockEntity.getBlockEntityPacket(parentBE, closeGuiTag));
    }

    public void signalOpenGui(ServerPlayer player,  int w, int h, boolean renderBackground) {{
        CompoundTag openGuiTag = new CompoundTag();
        openGuiTag.putInt("w", w);
        openGuiTag.putInt("h", h);
        openGuiTag.putBoolean("renderBackground", renderBackground);
        CompoundTag infoTag = new CompoundTag();
        infoTag.put("guiHandler_openGui", openGuiTag);
        PacketDistributor.sendToPlayer(player,PacketBlockEntity.getBlockEntityPacket(parentBE, infoTag));
    }}

    @Override
    public void serverTick() {
        IGuiHandler.super.serverTick();

        if (!playersTrackingGui.isEmpty()) {
            // if a player has not sent a gui ping for 10 seconds, he no longer has the gui open
            // this should usually not happen because the client will unregister itself on gui close but just to be safe....
            for (UUID uid : playersTrackingGui.keySet()) {
                playersTrackingGui.put(uid, playersTrackingGui.get(uid) + 1);
                if (playersTrackingGui.get(uid) > 200) {
                    removePlayerFromGui(uid);
                }
                Entity entity = ((ServerLevel) parentBE.getLevel()).getEntity(uid);
                if (entity instanceof ServerPlayer player && entity.position().distanceTo(parentBE.getBlockPos().getCenter()) > maxDistance) {
                    removePlayerFromGui(uid);
                    signalCloseGui(player);
                }
            }
        }
    }

    @Override
    public void onGuiClientTick(Player player) {
        last_ping += 1;
        if (last_ping > 20) {
            last_ping = 0;
            sendPing();
        }
    }

    public void sendPing() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("guiPing", Minecraft.getInstance().player.getUUID());
        sendToServer(tag);
    }

    @Override
    public void onGuiClose() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("closeGui", Minecraft.getInstance().player.getUUID());
        sendToServer(tag);
    }
}
