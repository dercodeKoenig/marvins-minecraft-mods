package BetterPipes.Network;


import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PacketBlockEntity implements CustomPacketPayload {

    public static final Type<PacketBlockEntity> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("betterpipes", "packetblockentity"));


    public PacketBlockEntity(String dim, int x, int y, int z, CompoundTag tag) {
        this.tag = tag;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
    }

    String dim;
    int x, y, z;
    CompoundTag tag;

    public CompoundTag getTag() {
        return tag;
    }

    public String dim() {
        return dim;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public static final StreamCodec<ByteBuf, PacketBlockEntity> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            PacketBlockEntity::dim,
            ByteBufCodecs.INT,
            PacketBlockEntity::x,
            ByteBufCodecs.INT,
            PacketBlockEntity::y,
            ByteBufCodecs.INT,
            PacketBlockEntity::z,
            ByteBufCodecs.COMPOUND_TAG,
            PacketBlockEntity::getTag,
            PacketBlockEntity::new
    );

    @OnlyIn(Dist.CLIENT)     // <- this is bc the server shits itself on this: Minecraft.getInstance().level;
    public static void readClient_onlyonclient(final PacketBlockEntity data, final IPayloadContext context) {
        // use the current Dimension, the client does not need to find the dimension by String
        Level world = Minecraft.getInstance().level;
        BlockEntity tile = world.getBlockEntity(new BlockPos(data.x(), data.y(), data.z()));
        if (tile instanceof INetworkTagReceiver) {
            ((INetworkTagReceiver) tile).readClient(data.getTag());
        }
    }

    //  this can not be dist.client because it is used in register method
    public static void readClient(final PacketBlockEntity data, final IPayloadContext context) {
        readClient_onlyonclient(data, context);
    }

    public static void readServer(final PacketBlockEntity data, final IPayloadContext context) {
        Level world = DimensionUtils.getDimensionLevelServer(data.dim);
        BlockEntity tile = world.getBlockEntity(new BlockPos(data.x(), data.y(), data.z()));
        if (tile instanceof INetworkTagReceiver) {
            ((INetworkTagReceiver) tile).readServer(data.getTag(), (ServerPlayer) context.player());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                PacketBlockEntity.TYPE,
                PacketBlockEntity.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PacketBlockEntity::readClient,
                        PacketBlockEntity::readServer
                )
        );
    }


    public static PacketBlockEntity getBlockEntityPacket(BlockEntity be, CompoundTag tag) {
        return getBlockEntityPacket(be.getLevel(), be.getBlockPos(), tag);
    }

    public static PacketBlockEntity getBlockEntityPacket(Level l, BlockPos p, CompoundTag tag) {
        return new PacketBlockEntity(
                l.isClientSide ? DimensionUtils.getLevelId(l) : "",
                p.getX(),
                p.getY(),
                p.getZ(),
                tag
        );
    }


    public static class DimensionUtils {
        public static String getLevelId(Level level) {
            ResourceKey<Level> key = level.dimension();
            return key.location().toString();
        }

        public static Level getDimensionLevelServer(String dimensionId) {
            ResourceLocation location = ResourceLocation.bySeparator(dimensionId, ':');
            return ServerLifecycleHooks.getCurrentServer().getLevel(ResourceKey.create(Registries.DIMENSION, location));

        }
    }
}