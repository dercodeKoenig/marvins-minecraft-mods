package NPCs.Blocks.Armory;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import NPCs.Blocks.TownHall.TownHallNames;
import NPCs.Blocks.TownHall.TownHallOwners;
import NPCs.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static NPCs.Registry.ENTITY_ARMORY;

public class EntityArmory extends BlockEntity implements INetworkTagReceiver {

    public static HashMap<BlockIdentifier, Set<BlockPos>> knownBlocksForTownhallPosition = new HashMap<>();
    public static HashSet<BlockIdentifier> knownBlocks = new HashSet<>();

    GuiHandlerBlockEntity guiHandler;
    BlockPos townHall;
    String owner;

    public ItemStackHandler inventory = new ItemStackHandler(18) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
            sendUpdateTag(null);
        }
    };
    public guiModuleText townHallText;


    public EntityArmory(BlockPos pos, BlockState blockState) {
        super(ENTITY_ARMORY.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 2; y++) {
                guiModuleItemHandlerSlot m = new guiModuleItemHandlerSlot(y * 9 + x, inventory, x+y*9, 1, 0, guiHandler, x * 18 + 10, y*18+30);
                guiHandler.getModules().add(m);
            }
        }

        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 175, 1000, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }
        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 120, 1100, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }

        townHallText = new guiModuleText(2002, "townhallpos", guiHandler, 5, 5, 0xff000000, false);
        guiHandler.getModules().add(townHallText);

    }

    public void useWithoutItem(Player p) {
        if (!level.isClientSide) {
            if (townHall == null || (TownHallOwners.getOwners(level, townHall) != null && TownHallOwners.getOwners(level, townHall).contains(p.getName().getString()))) {
                if (!guiHandler.playersTrackingGui.containsKey(p.getUUID())) {
                    CompoundTag tag = new CompoundTag();
                    tag.put("openGui", new CompoundTag());
                    PacketDistributor.sendToPlayer((ServerPlayer) p, PacketBlockEntity.getBlockEntityPacket(this, tag));
                }
            }
        }
    }

    public CompoundTag getUpdateTag() {
        CompoundTag info = new CompoundTag();
        info.put("inventory", this.inventory.serializeNBT(this.level.registryAccess()));
        return info;
    }
    public void sendUpdateTag(@Nullable ServerPlayer target) {
        if (target == null) {
            if (level instanceof ServerLevel l) {
                PacketDistributor.sendToPlayersTrackingChunk(l, new ChunkPos(this.getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, this.getUpdateTag()), new CustomPacketPayload[0]);
            }
        } else {
            PacketDistributor.sendToPlayer(target, PacketBlockEntity.getBlockEntityPacket(this, this.getUpdateTag()), new CustomPacketPayload[0]);
        }
    }



    public static void updateAllTownHalls() {
        for (BlockIdentifier b : knownBlocks) {
            BlockEntity be = b.level.getBlockEntity(b.pos);
            if (be instanceof EntityArmory t) {
                t.updateTownHall();
            }
        }
        updateSortedTownhallMap();
    }

    public static void updateSortedTownhallMap() {
        knownBlocksForTownhallPosition.clear();
        for (BlockIdentifier b : knownBlocks) {
            BlockEntity be = b.level.getBlockEntity(b.pos);
            if (be instanceof EntityArmory t) {
                if (t.townHall != null) {
                    BlockIdentifier bi = new BlockIdentifier(t.getLevel(), t.townHall);
                    Set<BlockPos> strategyTables = knownBlocksForTownhallPosition.get(bi);
                    if (strategyTables == null)
                        strategyTables = new HashSet<>();
                    strategyTables.add(t.getBlockPos());
                    knownBlocksForTownhallPosition.put(bi, strategyTables);
                }
            }
        }
    }

    public void updateTownHall() {
        // assign to townhall
        if (townHall == null) {
            // scan for townhall, use anyone where owner is registered as an owner of the townhall
            for (BlockPos p : Utils.sortBlockPosByDistanceToNPC(TownHallOwners.getEntries(level).keySet(), getBlockPos().getCenter())) {
                if (Utils.distanceManhattan(getBlockPos().getCenter(), p.getCenter()) > 512)
                    break;

                if (TownHallOwners.getOwners(level, p).contains(owner)) {
                    townHall = p;
                    break;
                }
            }
        } else {
            if (!TownHallOwners.getOwners(level, townHall).contains(owner)) {
                townHall = null;
                updateTownHall();
            }
        }
        if (townHall != null) {
            townHallText.setTextAndSync("Town: " + TownHallNames.getName(level, townHall));
        } else {
            townHallText.setTextAndSync("Town: none");
        }
    }

    @Override
    public void setRemoved(){
        super.setRemoved();
        knownBlocks.remove(new BlockIdentifier(level, getBlockPos()));
        updateSortedTownhallMap();
    }
    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            if (owner == null) {
                Player closestPlayer = null;
                double closestDistance = 999;
                for (Player p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    if (getBlockPos().getCenter().distanceTo(p.getPosition(0)) < closestDistance) {
                        closestDistance = getBlockPos().getCenter().distanceTo(p.getPosition(0));
                        closestPlayer = p;
                    }
                }
                if (closestPlayer != null) {
                    owner = closestPlayer.getName().getString();
                }
            }
            if (owner != null) {
                //ownerText.setTextAndSync("Owner: " + owner);
            }
            knownBlocks.add(new BlockIdentifier(level, getBlockPos()));
            updateTownHall();
            updateSortedTownhallMap();
        }
        if (level.isClientSide) {
            CompoundTag i = new CompoundTag();
            i.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, i));
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityArmory) t).tick();
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }


    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("ping")) {
            this.sendUpdateTag(serverPlayer);
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
        if (compoundTag.contains("inventory")) {
            this.inventory.deserializeNBT(this.level.registryAccess(), compoundTag.getCompound("inventory"));
        }
        if (compoundTag.contains("openGui")) {
            guiHandler.openGui(180, 200, true);
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory1", inventory.serializeNBT(registries));


        if (townHall != null) {
            tag.putInt("townHallX", townHall.getX());
            tag.putInt("townHallY", townHall.getY());
            tag.putInt("townHallZ", townHall.getZ());
        }

        if (owner != null) {
            tag.putString("owner", owner);
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory1"));


        if (tag.contains("townHallX") && tag.contains("townHallY") && tag.contains("townHallZ")) {
            townHall = new BlockPos(tag.getInt("townHallX"), tag.getInt("townHallY"), tag.getInt("townHallZ"));
        }

        if (tag.contains("owner")) {
            owner = tag.getString("owner");
        }
    }
}
