package NPCs.Blocks.TownHall;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import NPCs.Blocks.StrategyTable.EntityStrategyTable;
import NPCs.Npc.CombatNPC;
import NPCs.Npc.NPCBase;
import NPCs.Npc.WorkerNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static NPCs.Registry.ENTITY_TOWNHALL;

public class EntityTownHall extends BlockEntity implements INetworkTagReceiver {
    public static HashSet<BlockPos> knownTownHalls = new HashSet<>();

    public ItemStackHandler inventory = new ItemStackHandler(9 * 2) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

    GuiHandlerBlockEntity guiHandler;
    GuiHandlerBlockEntity ownersMenu;
    guiModuleScrollContainer ownersList;
    guiModuleTextInput addOwner;
    guiModuleTextInput townNameInput;


    public EntityTownHall(BlockPos pos, BlockState blockState) {
        super(ENTITY_TOWNHALL.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 2; y++) {
                guiModuleItemHandlerSlot m = new guiModuleItemHandlerSlot(y * 9 + x, inventory, y * 9 + x, 1, 0, guiHandler, x * 18 + 10, y * 18 + 75);
                guiHandler.getModules().add(m);
            }
        }

        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 175, 1000, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }
        for (guiModulePlayerInventorySlot m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 120  , 1100, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }

        guiHandler.getModules().add(new guiModuleText(5001, "Name:", guiHandler, 10, 10, 0xff000000, false));
        townNameInput = new guiModuleTextInput(5000, guiHandler, 40, 10, 120, 10) {
            @Override
            public void server_readNetworkData(CompoundTag tag) {
                super.server_readNetworkData(tag);
                updateTownhalls(); // update if the name changes
                TownHallNames.setName(level, getBlockPos(), text);
            }
        };

        guiHandler.getModules().add(townNameInput);

        guiModuleButton openOwnersMenuButton = new guiModuleButton(6110, "owners", guiHandler, 10, 30, 40, 15, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20) {
            @Override
            public void onButtonClicked() {
                ownersMenu.openGui(180, 180, true);
                CompoundTag getOwnersTag = new CompoundTag();
                getOwnersTag.put("getOwners", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityTownHall.this, getOwnersTag));
            }
        };
        openOwnersMenuButton.color = 0xffffffff;
        guiHandler.getModules().add(openOwnersMenuButton);

        guiModuleButton callWorkersButton = new guiModuleButton(6111, "call workers", guiHandler, 110, 50, 65, 15, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20) {
            @Override
            public void onButtonClicked() {
                CompoundTag callWorkersTag = new CompoundTag();
                callWorkersTag.put("callWorkers", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityTownHall.this, callWorkersTag));
            }
        };
        callWorkersButton.color = 0xffffffff;
        guiHandler.getModules().add(callWorkersButton);

        guiModuleButton uncallButton = new guiModuleButton(6113, "release NPCs", guiHandler, 10, 50, 70, 15, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20) {
            @Override
            public void onButtonClicked() {
                CompoundTag callWorkersTag = new CompoundTag();
                callWorkersTag.put("releaseNPCs", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityTownHall.this, callWorkersTag));
            }
        };
        uncallButton.color = 0xffffffff;
        guiHandler.getModules().add(uncallButton);

        guiModuleButton callFightersButton = new guiModuleButton(6112, "call fighters", guiHandler, 110, 30, 65, 15, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20) {
            @Override
            public void onButtonClicked() {
                CompoundTag callWorkersTag = new CompoundTag();
                callWorkersTag.put("callFighters", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityTownHall.this, callWorkersTag));
            }
        };
        callFightersButton.color = 0xffffffff;
        guiHandler.getModules().add(callFightersButton);


        ownersMenu = new GuiHandlerBlockEntity(this) {
            @Override
            public void onGuiClose() {
                super.onGuiClose();
                EntityTownHall.this.guiHandler.openGui(180, 200, true);
            }
        };

        ownersList = new guiModuleScrollContainer(new ArrayList<>(), 0x00000000, ownersMenu, 10, 40, 160, 110);
        ownersMenu.getModules().add(ownersList);

        guiModuleButton b = new guiModuleButton(10909, "+", ownersMenu, 10, 9, 12, 12, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20);
        b.color = 0xffffffff;
        ownersMenu.getModules().add(b);
        addOwner = new guiModuleTextInput(9990, ownersMenu, 30, 10, 120, 10);
        ownersMenu.getModules().add(addOwner);
    }

    public static void updateTownhalls(){
        NPCBase.updateAllTownHalls();
        EntityStrategyTable.updateAllTownHalls();
    }

    public void updateOwnerMenu(List<String> owners) {
        ownersList.modules.clear();

        int y = 0;
        for (String i : owners) {
            guiModuleText t = new guiModuleText(-1, i, ownersMenu, 20, y + 2, 0xff000000, false);
            guiModuleButton b = new guiModuleButton(-1, "X", ownersMenu, 0, y, 10, 10, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20) {
                @Override
                public void onButtonClicked() {
                    CompoundTag request = new CompoundTag();
                    request.putString("removeOwner", i);
                    PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityTownHall.this, request));
                }
            };
            b.color = 0xffffffff;
            ownersList.modules.add(t);
            ownersList.modules.add(b);
            y += 20;
        }

        if (ownersMenu.screen instanceof ModularScreen m) {
            m.calculateGuiOffsetAndNotifyModules();
        }
    }

    public Set<String> getOwners() {
        return TownHallOwners.getOwners(level, getBlockPos());
    }

    public void useWithoutItem(Player p) {
        if (!level.isClientSide) {
            if (getOwners().contains(p.getName().getString())) {
                if (!guiHandler.playersTrackingGui.containsKey(p.getUUID())) {
                    CompoundTag tag = new CompoundTag();
                    tag.put("openGui", new CompoundTag());
                    PacketDistributor.sendToPlayer((ServerPlayer) p, PacketBlockEntity.getBlockEntityPacket(this, tag));
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            knownTownHalls.add(getBlockPos());
            String name = TownHallNames.getName(level, getBlockPos());
            if (name != null)
                townNameInput.text = name;
            else {
                System.out.println("for some reason, the town at " + getBlockPos() + " does not have a name entry");
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        knownTownHalls.remove(getBlockPos());
    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityTownHall) t).tick();
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }


    public ListTag ownersTag() {
        ListTag l = new ListTag();
        for (String i : getOwners()) {
            l.add(StringTag.valueOf(i));
        }
        return l;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        // verify server side that the player is friend or owner before allow anything to go to the gui
        if (getOwners().contains(p.getName().getString())) {
            guiHandler.readServer(compoundTag);
            ownersMenu.readServer(compoundTag);

            if (compoundTag.contains("getOwners")) {
                CompoundTag ret = new CompoundTag();
                ret.put("owners", ownersTag());
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, ret));
            }

            if (compoundTag.contains("guiButtonClick")) {
                int btn = compoundTag.getInt("guiButtonClick");
                if (btn == 10909) {
                    TownHallOwners.addOwner(level, getBlockPos(), addOwner.text);
                    addOwner.text = "";
                    addOwner.broadcastModuleUpdate();
                    CompoundTag ret = new CompoundTag();
                    ret.put("owners", ownersTag());
                    PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, ret));
                    updateTownhalls();
                }
            }
            if (compoundTag.contains("removeOwner")) {
                String toRemove = compoundTag.getString("removeOwner");
                TownHallOwners.removeOwner(level, getBlockPos(), toRemove);
                CompoundTag ret = new CompoundTag();
                ret.put("owners", ownersTag());
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, ret));
                updateTownhalls();
            }
            if (compoundTag.contains("callWorkers")) {
                for (Entity e : ((ServerLevel) p.level()).getEntities().getAll()) {
                    if (e instanceof WorkerNPC npc) {
                        if (npc.townHall != null) {
                            if (npc.townHall.equals(getBlockPos())) {
                                npc.followOwner = p.getUUID();
                            }
                        }
                    }
                }
                CompoundTag response = new CompoundTag();
                response.put("closeGui", new CompoundTag());
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, response));
            }
            if (compoundTag.contains("callFighters")) {
                for (Entity e : ((ServerLevel) p.level()).getEntities().getAll()) {
                    if (e instanceof CombatNPC npc) {
                        if (npc.townHall != null) {
                            if (npc.townHall.equals(getBlockPos())) {
                                npc.followOwner = p.getUUID();
                            }
                        }
                    }
                }
                CompoundTag response = new CompoundTag();
                response.put("closeGui", new CompoundTag());
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, response));
            }
            if (compoundTag.contains("releaseNPCs")) {
                for (Entity e : ((ServerLevel) p.level()).getEntities().getAll()) {
                    if (e instanceof NPCBase npc) {
                        if (npc.townHall != null) {
                            if (npc.townHall.equals(getBlockPos())) {
                                if(Objects.equals(npc.followOwner, p.getUUID())) {
                                    npc.followOwner = null;
                                }
                            }
                        }
                    }
                }
                CompoundTag response = new CompoundTag();
                response.put("closeGui", new CompoundTag());
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, response));
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
        ownersMenu.readClient(compoundTag);

        if (compoundTag.contains("owners")) {
            ListTag ownersTag = compoundTag.getList("owners", Tag.TAG_STRING);
            List<String> owners = new ArrayList<>();
            for (int i = 0; i < ownersTag.size(); i++) {
                String owner = ownersTag.get(i).getAsString();
                owners.add(owner);
            }
            updateOwnerMenu(owners);
        }

        if (compoundTag.contains("openGui")) {
            guiHandler.openGui(180, 200, true);
        }
        if (compoundTag.contains("closeGui")) {
            if (guiHandler.screen instanceof ModularScreen m) {
                m.onClose();
            }
            if (ownersMenu.screen instanceof ModularScreen m) {
                m.onClose();
            }
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }
}