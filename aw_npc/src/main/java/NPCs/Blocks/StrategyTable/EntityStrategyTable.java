package NPCs.Blocks.StrategyTable;

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
import com.google.gson.Gson;
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
import static NPCs.Registry.ENTITY_STRATEGY_TABLE;

public class EntityStrategyTable extends BlockEntity implements INetworkTagReceiver {

    public static HashMap<BlockIdentifier, Set<BlockPos>> knownStrategyTablesForTownhallPosition = new HashMap<>();
    public static HashSet<BlockIdentifier> knownStrategyTables = new HashSet<>();

    public static class workTargetManager {
        public int index;
        public List<BlockPos> workPositions = new ArrayList<>();
        public UUID lastWorker;
        public long timer = 0;

        public void reset() {
            index = 0;
            workPositions.clear();
            lastWorker = null;
        }

        public BlockPos getTarget() {
            if(workPositions.isEmpty())return null;
            if (index >= workPositions.size()) index = 0;
            return workPositions.get(index);
        }
    }

    public Map<Integer, workTargetManager> targetManagerMap_Fighters = new HashMap<>();

    GuiHandlerBlockEntity guiHandler;
    BlockPos townHall;
    String owner;

    ItemStackHandler handler_fighters = new ItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();

            ItemStack stack = getStackInSlot(slot);

            targetManagerMap_Fighters.get(slot).reset();

            if (stack.getItem() instanceof ItemWorkOrder) {
                List<ItemWorkOrder.vec3> vecs = ItemWorkOrder.getBlockList(stack);
                List<BlockPos> blocks = new ArrayList<>();
                for (ItemWorkOrder.vec3 v : vecs) {
                    blocks.add(new BlockPos(v.x, v.y, v.z));
                }
                if (!blocks.isEmpty()) {
                    targetManagerMap_Fighters.get(slot).workPositions = blocks;
                }
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.getItem() instanceof ItemWorkOrder) {
                return true;
            }
            return false;
        }
    };

    public workTargetManager getManagerForUUID(UUID worker) {
        if (level instanceof ServerLevel serverLevel) {
            Entity e = serverLevel.getEntity(worker);
            if (e instanceof CombatNPC npc) {
                if (npc.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.fighter.ordinal()) {
                    for (int n : targetManagerMap_Fighters.keySet()) {
                        workTargetManager m = targetManagerMap_Fighters.get(n);
                        if (Objects.equals(m.lastWorker, worker)) {
                            return m;
                        }
                    }
                }


                for (int n : targetManagerMap_Fighters.keySet()) {
                    workTargetManager m = targetManagerMap_Fighters.get(n);
                    if (!m.workPositions.isEmpty()) {
                        if (m.lastWorker != null) {
                            // if the last worker is
                            // no longer alive or
                            // changed workType or
                            // has its own work order or
                            // received work from a different strategy table,
                            // remove him from this entry
                            Entity workingHereEntity = serverLevel.getEntity(m.lastWorker);
                            if (!(workingHereEntity instanceof CombatNPC)) {
                                m.lastWorker = null;
                            }
                            if (workingHereEntity instanceof CombatNPC alreadyWorkingNPC) {
                                if (alreadyWorkingNPC.getEntityData().get(DATA_WORKTYPE) != CombatNPC.WorkTypes.fighter.ordinal()) {
                                    m.lastWorker = null;
                                }
                                if (alreadyWorkingNPC.fighterFollowWorkOrderProgram.canUse()) {
                                    m.lastWorker = null;
                                }
                                if (!Objects.equals(alreadyWorkingNPC.fighterFollowWorkOrderByStrategyTable.lastUsedStrategyTable, getBlockPos())) {
                                    m.lastWorker = null;
                                }
                            }
                        }
                        if (m.lastWorker == null) {
                            m.lastWorker = worker;
                            return m;
                        }
                    }
                }
            }
        }
        return null;
    }

    public EntityStrategyTable(BlockPos pos, BlockState blockState) {
        super(ENTITY_STRATEGY_TABLE.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);

        for (int x = 0; x < 9; x++) {
            guiModuleItemHandlerSlot m = new guiModuleItemHandlerSlot(1 * 9 + x, handler_fighters, x, 1, 0, guiHandler, x * 18 + 10, 30);
            guiHandler.getModules().add(m);
        }

        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 175, 1000, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }
        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 120, 1100, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }


        for (int i = 0; i < handler_fighters.getSlots(); i++) {
            targetManagerMap_Fighters.put(i, new workTargetManager());
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
            Gson g = new Gson();
            System.out.println(g.toJson(knownStrategyTablesForTownhallPosition));
        }
    }


    public static void updateAllTownHalls() {
        for (BlockIdentifier b : knownStrategyTables) {
            BlockEntity be = DimensionUtils.getDimensionLevelServer(b.levelId).getBlockEntity(b.pos);
            if (be instanceof EntityStrategyTable t) {
                t.updateTownHall();
            }
        }

        for (BlockIdentifier th : new HashSet<>(knownStrategyTablesForTownhallPosition.keySet())){
            if(knownStrategyTablesForTownhallPosition.get(th) == null || knownStrategyTablesForTownhallPosition.get(th).isEmpty()){
                knownStrategyTablesForTownhallPosition.remove(th);
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
                BlockIdentifier townhallId = new BlockIdentifier(DimensionUtils.getLevelId(level),townHall);
                Set<BlockPos> strategyTables = knownStrategyTablesForTownhallPosition.get(townhallId);
                if(strategyTables != null){
                    strategyTables.remove(getBlockPos());
                }
                knownStrategyTablesForTownhallPosition.put(townhallId, strategyTables);
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
            BlockIdentifier townhallId = new BlockIdentifier(DimensionUtils.getLevelId(level),townHall);
            Set<BlockPos> strategyTables = knownStrategyTablesForTownhallPosition.get(townhallId);
            if(strategyTables == null)
                strategyTables = new HashSet<>();
            strategyTables.add(getBlockPos());
            knownStrategyTablesForTownhallPosition.put(townhallId, strategyTables);
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
            knownStrategyTables.add(new BlockIdentifier(DimensionUtils.getLevelId(level), getBlockPos()));

            updateTownHall();
        }
    }
    @Override
    public void setRemoved(){
        super.setRemoved();
        if(townHall != null) {
            BlockIdentifier townhallId = new BlockIdentifier(DimensionUtils.getLevelId(level),townHall);
            Set<BlockPos> strategyTables = knownStrategyTablesForTownhallPosition.get(townhallId);
            if(strategyTables != null){
                strategyTables.remove(getBlockPos());
            }
            knownStrategyTables.remove(new BlockIdentifier(DimensionUtils.getLevelId(level), getBlockPos()));
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStrategyTable) t).tick();
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
        tag.put("inventory1", handler_fighters.serializeNBT(registries));


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
        handler_fighters.deserializeNBT(registries, tag.getCompound("inventory1"));


        if (tag.contains("townHallX") && tag.contains("townHallY") && tag.contains("townHallZ")) {
            townHall = new BlockPos(tag.getInt("townHallX"), tag.getInt("townHallY"), tag.getInt("townHallZ"));
        }

        if (tag.contains("owner")) {
            owner = tag.getString("owner");
        }
    }
}
