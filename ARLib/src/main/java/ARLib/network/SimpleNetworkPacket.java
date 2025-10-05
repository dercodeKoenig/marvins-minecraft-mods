package ARLib.network;

import ARLib.utils.DimensionUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

public class SimpleNetworkPacket implements CustomPacketPayload {

    public interface SimpleNetworkDataReceiver {
        default void readServer(String data, ServerPlayer player) {

        }

        default void readClient(String data) {

        }
    }

    static Map<String, SimpleNetworkDataReceiver> receivers = new HashMap<>();

    public static void registerReceiver(String id, SimpleNetworkDataReceiver receiver) {
        receivers.put(id, receiver);
    }

    public static final Type<SimpleNetworkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("arlib", "simple_network_packet"));


    public SimpleNetworkPacket(String id, String data) {
        this.id = id;
        this.data = data;
    }

    String id, data;


    public String getData() {
        return data;
    }

    public String getId() {
        return id;
    }

    public static final StreamCodec<ByteBuf, SimpleNetworkPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SimpleNetworkPacket::getId,
            ByteBufCodecs.STRING_UTF8,
            SimpleNetworkPacket::getData,
            SimpleNetworkPacket::new
    );


    public static void readClient(final SimpleNetworkPacket data, final IPayloadContext context) {
        if(receivers.containsKey(data.getId())){
            receivers.get(data.getId()).readClient(data.data);
        }else{
            System.out.println("for packet with id "+data.getId()+" is no receiver registered");
        }
    }

    public static void readServer(final SimpleNetworkPacket data, final IPayloadContext context) {
        if(receivers.containsKey(data.getId())){
            receivers.get(data.getId()).readServer(data.data, (ServerPlayer) context.player());
        }else{
            System.out.println("for packet with id "+data.getId()+" is no receiver registered");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                SimpleNetworkPacket.TYPE,
                SimpleNetworkPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        SimpleNetworkPacket::readClient,
                        SimpleNetworkPacket::readServer
                )
        );
    }
}