package BetterPipes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

// INetworkPacket.java
public interface INetworkTagReceiver {
    void readServer(CompoundTag tag, ServerPlayer player);
    void readClient(CompoundTag tag);
}
