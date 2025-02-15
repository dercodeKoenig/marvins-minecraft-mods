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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;

public class PacketFluidUpdate implements CustomPacketPayload {



    public static final Type<PacketFluidUpdate> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("betterpipes", "connection_sync_fluid_to_client"));

    public static PacketFluidUpdate getPacketFluidUpdate(BlockPos pos, @Nullable Direction d, Fluid fluid){
        int direction;
        if(d==null)direction = -1;
        else direction = d.ordinal();
        return new PacketFluidUpdate(pos.getX(),pos.getY(),pos.getZ(),direction,new FluidStack(fluid,1), System.currentTimeMillis());
    }

    public PacketFluidUpdate(int x, int y, int z, int direction, FluidStack fluidInTank, long time) {
        this.side = direction;
        this.x=x;
        this.y=y;
        this.z=z;
        this.fluid = fluidInTank;
        this.time = time;
    }
    int x;int x(){return x;}
    int y;int y(){return y;}
    int z;int z(){return z;}
    long time;long time(){return time;}

    int  side;
    FluidStack fluid;
    public int side() {
        return side;
    }
    public FluidStack fluid(){return fluid;}


    public static final StreamCodec<ByteBuf, PacketFluidUpdate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PacketFluidUpdate::x,
            ByteBufCodecs.INT,
            PacketFluidUpdate::y,
            ByteBufCodecs.INT,
            PacketFluidUpdate::z,
            ByteBufCodecs.INT,
            PacketFluidUpdate::side,
            ByteBufCodecs.fromCodec(FluidStack.CODEC),
            PacketFluidUpdate::fluid,
            ByteBufCodecs.VAR_LONG,
            PacketFluidUpdate::time,
            PacketFluidUpdate::new
    );

    @OnlyIn(Dist.CLIENT)     // <- this is bc the server shits itself on this: Minecraft.getInstance().level;
    public static void readClient_onlyonclient(final PacketFluidUpdate data, final IPayloadContext context) {
        // use the current Dimension, the client does not need to find the dimension by String
        Level world = Minecraft.getInstance().level;
        BlockEntity tile = world.getBlockEntity(new BlockPos(data.x(),data.y(),data.z()));
        if (tile instanceof EntityPipe pipe){
            if(data.side==-1){
                pipe.setFluidInTank(data.fluid.getFluid(), data.time());
            }else {
                pipe.connections.get(Direction.values()[data.side()]).setFluidInTank(data.fluid.getFluid(), data.time());
            }
        }

    }
    //  this can not be dist.client because it is used in register method
    public static void readClient(final PacketFluidUpdate data, final IPayloadContext context) {
        readClient_onlyonclient(data,context);
    }
    public static void readServer(final PacketFluidUpdate data, final IPayloadContext context) {
        //nothing to do
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                PacketFluidUpdate.TYPE,
                PacketFluidUpdate.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PacketFluidUpdate::readClient,
                        PacketFluidUpdate::readServer
                )
        );
    }
}