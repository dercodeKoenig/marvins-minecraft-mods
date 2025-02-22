package NPCs.Blocks.StrategyTable;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleDefaultButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import NPCs.Blocks.TownHall.TownHallNames;
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


    public Map<Integer, workTargetManager> targetManagerMap_Fighters = new HashMap<>();

    GuiHandlerBlockEntity guiHandler;
    BlockPos townHall;
    String owner;
    public guiModuleText townHallText;
    public guiModuleDefaultButton redstoneControlButton;
    boolean useNormalRedstoneSignal;

    public void updateRedstoneButtontext(){
        if(useNormalRedstoneSignal == false)
            redstoneControlButton.setTextAndSync("inverted");
        else{
            redstoneControlButton.setTextAndSync("normal");
        }
    }

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

    ItemStackHandler handler_fighters = new ItemStackHandler(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
            scanSlot_fighters(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.getItem() instanceof ItemWorkOrder) {
                return true;
            }
            return false;
        }
    };

    public void scanSlot_fighters(int slot){
        ItemStack stack = handler_fighters.getStackInSlot(slot);
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

    public workTargetManager getManagerForUUID(UUID worker) {
        if (level instanceof ServerLevel serverLevel) {

            if(!level.hasNeighborSignal(getBlockPos()) && useNormalRedstoneSignal)
                return null;
            if(level.hasNeighborSignal(getBlockPos()) && !useNormalRedstoneSignal)
                return null;

            Entity e = serverLevel.getEntity(worker);
            if (e instanceof CombatNPC npc) {
                if (npc.getEntityData().get(DATA_WORKTYPE) == CombatNPC.WorkTypes.fighter.ordinal()) {
                    for (int n : targetManagerMap_Fighters.keySet()) {
                        workTargetManager m = targetManagerMap_Fighters.get(n);
                        if (Objects.equals(m.lastWorker, worker) && Objects.equals(npc.fighterFollowWorkOrderByStrategyTable.lastUsedStrategyTable, getBlockPos())) {
                            return m;
                        }
                    }
                }


                List<Integer> keys = new ArrayList<>(targetManagerMap_Fighters.keySet());
                Collections.shuffle(keys); // Shuffle the list randomly so that it will not get stuck at something it can not reach
                for (int n : keys) {
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
                            // clear this worker from other positions or it will return the wrong manager later
                            for (int n2 : keys) {
                                workTargetManager m2 = targetManagerMap_Fighters.get(n2);
                                if(Objects.equals(m2.lastWorker, worker)){
                                    m2.lastWorker=null;
                                }
                            }
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
        townHallText = new guiModuleText(2002, "townhallpos", guiHandler, 5, 5, 0xff000000, false);
        guiHandler.getModules().add(townHallText);

        guiHandler.getModules().add(new guiModuleText(8798, "Redstone Control:", guiHandler, 5, 17, 0xff000000, false));
        redstoneControlButton = new guiModuleDefaultButton(8799,"",guiHandler,100,12,50,15);
        guiHandler.getModules().add(redstoneControlButton);

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
        }
    }


    public static void updateAllTownHalls() {
        for (BlockIdentifier b : knownStrategyTables) {
            BlockEntity be = b.level.getBlockEntity(b.pos);
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
            if (!TownHallOwners.getOwners(level, townHall).contains(owner)) {
                BlockIdentifier townhallId = new BlockIdentifier(level,townHall);
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
            townHallText.setTextAndSync("Town: " + TownHallNames.getName(level, townHall));
        } else {
            townHallText.setTextAndSync("Town: none");
        }

        if(townHall != null) {
            BlockIdentifier townhallId = new BlockIdentifier(level,townHall);
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
            knownStrategyTables.add(new BlockIdentifier(level, getBlockPos()));

            updateTownHall();

            // read the work orders on load
            for (int i = 0; i < handler_fighters.getSlots(); i++) {
                scanSlot_fighters(i);
            }

            updateRedstoneButtontext();

        }
    }
    @Override
    public void setRemoved(){
        super.setRemoved();
        if(townHall != null) {
            BlockIdentifier townhallId = new BlockIdentifier(level,townHall);
            Set<BlockPos> strategyTables = knownStrategyTablesForTownhallPosition.get(townhallId);
            if(strategyTables != null){
                strategyTables.remove(getBlockPos());
            }
        }
        knownStrategyTables.remove(new BlockIdentifier(level, getBlockPos()));
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
        if(compoundTag.contains("guiButtonClick")){
            int buttonId = compoundTag.getInt("guiButtonClick");
            if(buttonId==8799){
                useNormalRedstoneSignal = !useNormalRedstoneSignal;
            updateRedstoneButtontext();
            }
        }
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

        tag.putBoolean("invRedstone", useNormalRedstoneSignal);

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

        useNormalRedstoneSignal = tag.getBoolean("invRedstone");

        if (tag.contains("townHallX") && tag.contains("townHallY") && tag.contains("townHallZ")) {
            townHall = new BlockPos(tag.getInt("townHallX"), tag.getInt("townHallY"), tag.getInt("townHallZ"));
        }

        if (tag.contains("owner")) {
            owner = tag.getString("owner");
        }
    }
}
