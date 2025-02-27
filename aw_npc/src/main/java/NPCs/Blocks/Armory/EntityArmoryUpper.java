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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static NPCs.Registry.ENTITY_ARMORY;
import static NPCs.Registry.ENTITY_ARMORY_UPPER;


// this thing is really only to fix the IItemHandler Cap not available when using the upper block
// this is bad if a user clicks the upper half with the routing order and without this fix, it would never work
public class EntityArmoryUpper extends BlockEntity {
    public EntityArmoryUpper(BlockPos pos, BlockState blockState) {
        super(ENTITY_ARMORY_UPPER.get(), pos, blockState);
    }
}
