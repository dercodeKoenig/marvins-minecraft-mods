package advRocketry.BlockEntities;

import ARLib.network.INetworkTagReceiver;
import advRocketry.Blocks.WirelessTransceiver;
import advRocketry.Data.DataStack;
import advRocketry.Data.DataStorage;
import advRocketry.Data.IDataStorage;
import advRocketry.Data.IDataStorageProvider;
import advRocketry.GlobalTime;
import advRocketry.Items.ItemLinker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Objects;

import static advRocketry.Blocks.WirelessTransceiver.STATE;
import static advRocketry.Registry.BlockEntities.ENTITY_WIRELESS_TRANSCEIVER;

public class EntityWirelessTransceiver extends BlockEntity implements ItemLinker.linkable, INetworkTagReceiver, IDataStorageProvider {

    public static int MAX_DISTANCE = 64;

    public BlockPos linkedPos;
    public boolean isSender = false;
    public DataStorage dataStorage;

    public EntityWirelessTransceiver(BlockPos pos, BlockState blockState) {
        super(ENTITY_WIRELESS_TRANSCEIVER.get(), pos, blockState);
        dataStorage = new DataStorage(10);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWirelessTransceiver) t).tick();
    }


    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {

    }

    @Override
    public void readClient(CompoundTag compoundTag) {

    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedPos != null)
            tag.put("linkedPos", NbtUtils.writeBlockPos(linkedPos));
        tag.putBoolean("isSender", isSender);
        tag.put("dataStorage", dataStorage.saveToNbt());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("linkedPos"))
            linkedPos = NbtUtils.readBlockPos(tag, "linkedPos").get();
        isSender = tag.getBoolean("isSender");
        dataStorage.readFromNbt(tag.getCompound("dataStorage"));
    }


    public EntityWirelessTransceiver getPartner() {
        if (linkedPos != null) {
            if (!level.isLoaded(linkedPos)) {
                return null;
            }
            BlockEntity other = level.getBlockEntity(linkedPos);
            if (!(other instanceof EntityWirelessTransceiver)) {
                linkedPos = null;
                return null;
            }
            EntityWirelessTransceiver otherTransceiver = (EntityWirelessTransceiver) other;
            if (otherTransceiver.isSender == isSender) {
                linkedPos = null;
                return null;
            }
            if (!(Objects.equals(otherTransceiver.linkedPos, getBlockPos()))) {
                linkedPos = null;
                return null;
            }
            return otherTransceiver;
        }
        return null;
    }

    public void tick() {
        if (!level.isClientSide) {

            EntityWirelessTransceiver partner = getPartner();

            if (partner == null) {
                if (getBlockState().getValue(STATE) != WirelessTransceiver.State.not_connected)
                    level.setBlock(getBlockPos(), getBlockState().setValue(STATE, WirelessTransceiver.State.not_connected), 3);
            } else {

                int maxTransfer = 1;

                Direction facing = getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
                BlockPos neighborPos = getBlockPos().relative(facing);

                if (isSender) {

                    boolean didWork = false;

                    // extract from neighbor
                    if (level.getBlockEntity(neighborPos) instanceof IDataStorageProvider dataStorageProvider) {
                        IDataStorage neighborStorage = dataStorageProvider.getDataStorage(facing);
                        DataStack canExtract = neighborStorage.extractData(maxTransfer, true);
                        int canInsert = dataStorage.insertData(canExtract, true);
                        if (canInsert > 0) {
                            DataStack extracted = neighborStorage.extractData(canInsert, false);
                            dataStorage.insertData(extracted, false);
                            didWork = true;
                        }
                    }

                    // send to partner
                    DataStack canExtract = dataStorage.extractData(maxTransfer, true);
                    int canReceive = partner.dataStorage.insertData(canExtract, true);
                    if (canReceive > 0) {
                        DataStack extracted = dataStorage.extractData(canReceive, false);
                        partner.dataStorage.insertData(extracted, false);
                        didWork = true;
                    }

                    if (didWork && getBlockState().getValue(STATE) != WirelessTransceiver.State.sender_active)
                        level.setBlock(getBlockPos(), getBlockState().setValue(STATE, WirelessTransceiver.State.sender_active), 3);
                    else if (!didWork && getBlockState().getValue(STATE) != WirelessTransceiver.State.sender)
                        level.setBlock(getBlockPos(), getBlockState().setValue(STATE, WirelessTransceiver.State.sender), 3);

                } else {

                    boolean didWork = false;


                    // insert into neighbor
                    if (level.getBlockEntity(neighborPos) instanceof IDataStorageProvider dataStorageProvider) {
                        IDataStorage neighborStorage = dataStorageProvider.getDataStorage(facing);
                        DataStack canExtract = dataStorage.extractData(maxTransfer, true);
                        int canInsert = neighborStorage.insertData(canExtract, true);
                        if (canInsert > 0) {
                            DataStack extracted = dataStorage.extractData(canInsert, false);
                            neighborStorage.insertData(extracted, false);
                            didWork = true;
                        }
                    }

                    if (didWork && getBlockState().getValue(STATE) != WirelessTransceiver.State.receiver_active)
                        level.setBlock(getBlockPos(), getBlockState().setValue(STATE, WirelessTransceiver.State.receiver_active), 3);
                    else if (!didWork && getBlockState().getValue(STATE) != WirelessTransceiver.State.receiver)
                        level.setBlock(getBlockPos(), getBlockState().setValue(STATE, WirelessTransceiver.State.receiver), 3);
                }
            }
        }
    }


    @Override
    public boolean link(BlockPos otherpos, Level otherLevel) {
        if (otherLevel == level) {
            BlockEntity other = level.getBlockEntity(otherpos);
            if (other instanceof EntityWirelessTransceiver otherTransceiver) {
                if (otherpos.getCenter().distanceTo(getBlockPos().getCenter()) < MAX_DISTANCE) {
                    linkedPos = otherpos;
                    isSender = false;
                    otherTransceiver.isSender = !isSender;
                    otherTransceiver.linkedPos = getBlockPos();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public IDataStorage getDataStorage(Direction face) {
        return dataStorage;
    }
}
