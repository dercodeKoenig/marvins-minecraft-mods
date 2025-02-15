package BetterPipes.Network;

import BetterPipes.Pipe.EntityPipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;

public class PacketFluidAmountUpdate implements CustomPacketPayload {



    public static final Type<PacketFluidAmountUpdate> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("betterpipes", "connection_sync_fluid_amount_to_client"));

    public static PacketFluidAmountUpdate getPacketFluidUpdate(BlockPos pos, @Nullable Direction d, int amount){
        int direction;
        if(d==null)direction = -1;
        else direction = d.ordinal();
        return new PacketFluidAmountUpdate(pos.getX(),pos.getY(),pos.getZ(),direction,amount, System.currentTimeMillis());
    }

    public PacketFluidAmountUpdate(int x, int y, int z, int direction, int amount, long time) {
        this.side = direction;
        this.x=x;
        this.y=y;
        this.z=z;
        this.amount = amount;
        this.time = time;
    }
    int x;int x(){return x;}
    int y;int y(){return y;}
    int z;int z(){return z;}
    long time;long time(){return time;}

    int  side;
    int amount;
    public int side() {
        return side;
    }
    public int amount(){return amount;}


    public static final StreamCodec<ByteBuf, PacketFluidAmountUpdate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PacketFluidAmountUpdate::x,
            ByteBufCodecs.INT,
            PacketFluidAmountUpdate::y,
            ByteBufCodecs.INT,
            PacketFluidAmountUpdate::z,
            ByteBufCodecs.INT,
            PacketFluidAmountUpdate::side,
            ByteBufCodecs.INT,
            PacketFluidAmountUpdate::amount,
            ByteBufCodecs.VAR_LONG,
            PacketFluidAmountUpdate::time,
            PacketFluidAmountUpdate::new
    );

    @OnlyIn(Dist.CLIENT)     // <- this is bc the server shits itself on this: Minecraft.getInstance().level;
    public static void readClient_onlyonclient(final PacketFluidAmountUpdate data, final IPayloadContext context) {
        // use the current Dimension, the client does not need to find the dimension by String
        Level world = Minecraft.getInstance().level;
        BlockEntity tile = world.getBlockEntity(new BlockPos(data.x(),data.y(),data.z()));
        if (tile instanceof EntityPipe pipe){
            if(data.side==-1){
                pipe.setFluidAmountInTank(data.amount(), data.time());
            }else {
                pipe.connections.get(Direction.values()[data.side()]).setFluidAmountInTank(data.amount(), data.time());
            }
        }

    }
    //  this can not be dist.client because it is used in register method
    public static void readClient(final PacketFluidAmountUpdate data, final IPayloadContext context) {
        readClient_onlyonclient(data,context);
    }
    public static void readServer(final PacketFluidAmountUpdate data, final IPayloadContext context) {
        //nothing to do
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                PacketFluidAmountUpdate.TYPE,
                PacketFluidAmountUpdate.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PacketFluidAmountUpdate::readClient,
                        PacketFluidAmountUpdate::readServer
                )
        );
    }
}