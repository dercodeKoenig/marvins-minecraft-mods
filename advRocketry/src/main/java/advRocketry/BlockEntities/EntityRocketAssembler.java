package advRocketry.BlockEntities;

import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.LaunchPad;
import advRocketry.Blocks.StructureTower;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;

public class EntityRocketAssembler extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    private static final Logger log = LoggerFactory.getLogger(EntityRocketAssembler.class);
    public BlockPos areaMin;
    public BlockPos areaMax;

    public EntityRocketAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_ROCKET_ASSEMBLER.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            CompoundTag ping = new CompoundTag();
            ping.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, ping));
        } else {
            scanArea();
        }
    }

    public void scanForSpaceDockingArea() {

    }

    public void scanForLaunchPadArea() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // the position behind & below the assembler
        BlockPos startingPos = getBlockPos().below().offset(facing.getOpposite().getStepX(), facing.getOpposite().getStepY(), facing.getOpposite().getStepZ());

        // going left & right up to 16 blocks to find the largest rectangle area
        // it will use as starting points one of side1 and one of side2 so the area will always include the starting pos
        Set<BlockPos> side1 = new HashSet<>();
        Set<BlockPos> side2 = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            Direction side1Direction = facing.getClockWise();
            Direction side2Direction = facing.getCounterClockWise();
            side1.add(new BlockPos(startingPos).offset(side1Direction.getStepX() * i, side1Direction.getStepY() * i, side1Direction.getStepZ() * i));
            side2.add(new BlockPos(startingPos).offset(side2Direction.getStepX() * i, side2Direction.getStepY() * i, side2Direction.getStepZ() * i));
        }

        // we keep the area with the largest volume because there can be many possible areas
        int largestAreaVolume = 0;
        areaMin = null;
        areaMax = null;

        // try all combinations of left & right positions with a min and max distance
        for (BlockPos p1 : side1) {
            for (BlockPos p2 : side2) {
                if (p1.distManhattan(p2) < 2 || p1.distManhattan(p2) > 16) {
                    continue;
                }
                // try all possible depth values
                for (int depth = 2; depth < 16; depth++) {
                    BlockPos p3 = p1.offset(facing.getOpposite().getStepX() * depth, facing.getOpposite().getStepY() * depth, facing.getOpposite().getStepZ() * depth);
                    int minX = Math.min(Math.min(p1.getX(), p2.getX()), p3.getX());
                    int minZ = Math.min(Math.min(p1.getZ(), p2.getZ()), p3.getZ());
                    int maxX = Math.max(Math.max(p1.getX(), p2.getX()), p3.getX());
                    int maxZ = Math.max(Math.max(p1.getZ(), p2.getZ()), p3.getZ());

                    // this are the min and max positions of the target area
                    BlockPos minPos = new BlockPos(minX, startingPos.getY(), minZ);
                    BlockPos maxPos = new BlockPos(maxX, startingPos.getY(), maxZ);

                    boolean isAllValid = true;
                    // see if for the current rectangle all positions are a valid launchpad
                    for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
                        for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                            BlockPos target = new BlockPos(x, startingPos.getY(), z);

                            // make sure chunk is loaded to scan
                            ChunkPos pos = new ChunkPos(target);
                            level.getChunk(pos.x,pos.z, ChunkStatus.FULL,true);

                            Block block = level.getBlockState(target).getBlock();
                            if (!(block instanceof LaunchPad)) {
                                isAllValid = false;
                            }
                        }
                    }
                    if (isAllValid) {
                        // all of the blocks are a launchpad. good.
                        // now check if there is a structure tower around the area
                        // the tower will be placed outside of the launch area by 1 block
                        // find the height of the biggest tower
                        // if launchpad is x, structure tower can be at any s position
                        // ssssss
                        // sxxxxs
                        // sxxxxs
                        // sxxxxs
                        // ssssss
                        List<BlockPos> possibleTowerPositions = new ArrayList<>();
                        for (int x = minPos.getX() - 1; x < maxPos.getX() + 1; x++) {
                            BlockPos towerPos1 = new BlockPos(x, getBlockPos().getY(), minPos.getZ() - 1);
                            BlockPos towerPos2 = new BlockPos(x, getBlockPos().getY(), maxPos.getZ() + 1);
                            possibleTowerPositions.add(towerPos1);
                            possibleTowerPositions.add(towerPos2);
                        }
                        for (int z = minPos.getZ() - 1; z < maxPos.getZ() + 1; z++) {
                            BlockPos towerPos1 = new BlockPos(minPos.getX() - 1, getBlockPos().getY(), z);
                            BlockPos towerPos2 = new BlockPos(maxPos.getX() + 1, getBlockPos().getY(), z);
                            possibleTowerPositions.add(towerPos1);
                            possibleTowerPositions.add(towerPos2);
                        }
                        int maxTowerHeight = 0;
                        for (BlockPos towerPos : possibleTowerPositions) {
                            int towerHeight = 0;
                            while (level.getBlockState(towerPos.offset(0, towerHeight, 0)).getBlock() instanceof StructureTower) {
                                towerHeight++;
                            }
                            maxTowerHeight = Math.max(maxTowerHeight, towerHeight);
                        }
                        if (maxTowerHeight > 4) {
                            // valid launchpad area
                            maxPos = maxPos.offset(0, maxTowerHeight, 0);
                            int volume = (maxPos.getX() - minPos.getX())
                                    * (maxPos.getY() - minPos.getY())
                                    * (maxPos.getZ() - minPos.getZ());
                            if (volume > largestAreaVolume) {
                                largestAreaVolume = volume;
                                areaMin = minPos.above();
                                areaMax = maxPos;
                            }
                        }
                    }
                }
            }
        }
    }

    public void scanArea() {
        //long t0 = System.currentTimeMillis();
        Dimension myDim = DimensionManager.get(level.dimension().location());
        if (myDim != null && myDim.getType() == DimensionProperties.PlanetType.SPACE_STATION) {
            scanForSpaceDockingArea();
        } else {
            scanForLaunchPadArea();
        }
        broadcastInformationToPlayers(null);
        //long t1 = System.currentTimeMillis();
        //System.out.println("scan complete in " +(t1-t0) +"ms");
        if(areaMin != null)level.setBlock(areaMin,Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        if(areaMax != null)level.setBlock(areaMax,Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
    }

    public void broadcastInformationToPlayers(ServerPlayer p) {
        CompoundTag info = new CompoundTag();
        if (areaMin != null) {
            info.putInt("minX", areaMin.getX());
            info.putInt("minY", areaMin.getY());
            info.putInt("minZ", areaMin.getZ());
        }
        if (areaMax != null) {
            info.putInt("maxX", areaMax.getX());
            info.putInt("maxY", areaMax.getY());
            info.putInt("maxZ", areaMax.getZ());
        }
        PacketBlockEntity packet = PacketBlockEntity.getBlockEntityPacket(this, info);
        if (p == null) {
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), packet);
        } else {
            PacketDistributor.sendToPlayer(p, packet);
        }
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        if (compoundTag.contains("ping")) {
            broadcastInformationToPlayers(serverPlayer);
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        areaMin = null;
        if (compoundTag.contains("minX") && compoundTag.contains("minY") && compoundTag.contains("minZ"))
            areaMin = new BlockPos(compoundTag.getInt("minX"), compoundTag.getInt("minY"), compoundTag.getInt("minZ"));

        areaMax = null;
        if (compoundTag.contains("maxX") && compoundTag.contains("maxY") && compoundTag.contains("maxZ"))
            areaMax = new BlockPos(compoundTag.getInt("maxX"), compoundTag.getInt("maxY"), compoundTag.getInt("maxZ"));


        //System.out.println(areaMin);
        //System.out.println(areaMax);
    }
}
