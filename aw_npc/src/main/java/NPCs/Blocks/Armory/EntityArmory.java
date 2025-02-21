package NPCs.Blocks.Armory;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import NPCs.Blocks.TownHall.TownHallOwners;
import NPCs.Items.ItemWorkOrder;
import NPCs.Npc.CombatNPC;
import NPCs.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static NPCs.Npc.CombatNPC.DATA_WORKTYPE;
import static NPCs.Registry.ENTITY_ARMORY;
import static NPCs.Registry.ENTITY_STRATEGY_TABLE;

public class EntityArmory extends BlockEntity implements INetworkTagReceiver {

    public static HashMap<BlockIdentifier, Set<BlockPos>> knownBlocksForTownhallPosition = new HashMap<>();
    public static HashSet<BlockIdentifier> knownBlocks = new HashSet<>();

    GuiHandlerBlockEntity guiHandler;
    BlockPos townHall;
    String owner;

    ItemStackHandler handler = new ItemStackHandler(18) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };


    public EntityArmory(BlockPos pos, BlockState blockState) {
        super(ENTITY_ARMORY.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 2; y++) {
                guiModuleItemHandlerSlot m = new guiModuleItemHandlerSlot(y * 9 + x, handler, x, 1, 0, guiHandler, x * 18 + 10, y*18+30);
                guiHandler.getModules().add(m);
            }
        }

        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 175, 1000, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }
        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 120, 1100, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }

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


    public static void updateAllTownHalls() {
        for (BlockIdentifier b : knownBlocks) {
            BlockEntity be = b.level.getBlockEntity(b.pos);
            if (be instanceof EntityArmory t) {
                t.updateTownHall();
            }
        }

        for (BlockIdentifier th : new HashSet<>(knownBlocksForTownhallPosition.keySet())){
            if(knownBlocksForTownhallPosition.get(th) == null || knownBlocksForTownhallPosition.get(th).isEmpty()){
                knownBlocksForTownhallPosition.remove(th);
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
            if (TownHallOwners.getEntry(level, townHall) == null || !TownHallOwners.getOwners(level, townHall).contains(owner)) {
                BlockIdentifier townhallId = new BlockIdentifier(level,townHall);
                Set<BlockPos> strategyTables = knownBlocksForTownhallPosition.get(townhallId);
                if(strategyTables != null){
                    strategyTables.remove(getBlockPos());
                }
                knownBlocksForTownhallPosition.put(townhallId, strategyTables);
                townHall = null;
                updateTownHall();
            }
        }
        if (townHall != null) {
            //townHallText.setTextAndSync("Town: " + TownHallNames.getName(level(), townHall));
        } else {
            //townHallText.setTextAndSync("Town: none");
        }

        if(townHall != null) {
            BlockIdentifier townhallId = new BlockIdentifier(level,townHall);
            Set<BlockPos> strategyTables = knownBlocksForTownhallPosition.get(townhallId);
            if(strategyTables == null)
                strategyTables = new HashSet<>();
            strategyTables.add(getBlockPos());
            knownBlocksForTownhallPosition.put(townhallId, strategyTables);
        }
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

        }
    }
    @Override
    public void setRemoved(){
        super.setRemoved();
        if(townHall != null) {
            BlockIdentifier townhallId = new BlockIdentifier(level,townHall);
            Set<BlockPos> strategyTables = knownBlocksForTownhallPosition.get(townhallId);
            if(strategyTables != null){
                strategyTables.remove(getBlockPos());
            }
            knownBlocks.remove(new BlockIdentifier(level, getBlockPos()));
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
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);

        if (compoundTag.contains("openGui")) {
            guiHandler.openGui(180, 200, true);
        }
    }


    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory1", handler.serializeNBT(registries));


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
        handler.deserializeNBT(registries, tag.getCompound("inventory1"));


        if (tag.contains("townHallX") && tag.contains("townHallY") && tag.contains("townHallZ")) {
            townHall = new BlockPos(tag.getInt("townHallX"), tag.getInt("townHallY"), tag.getInt("townHallZ"));
        }

        if (tag.contains("owner")) {
            owner = tag.getString("owner");
        }
    }
}
