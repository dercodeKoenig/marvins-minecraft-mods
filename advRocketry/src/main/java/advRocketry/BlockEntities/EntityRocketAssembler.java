package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.PacketBlockEntity;
import advRocketry.Blocks.GuidanceComputer;
import advRocketry.Blocks.LaunchPad;
import advRocketry.Blocks.StructureTower;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.DimensionProperties;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;

public class EntityRocketAssembler extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public static int maxSize = 20;
    public static int buildTimeBase = 5;//20;

    // the current rocket is the one on launchpad.
    // if there is none on launchpad area, it will keep the reference to the previous one until it is removed or ends its program
    EntityRocket currentRocket;

    GuiHandlerBlockEntity guiHandler;
    guiModuleButton buildButton;
    guiModuleText statusText;

    public BlockPos areaMin;
    public BlockPos areaMax;

    public int buildProgress = -1;
    public int clientBuildProgress = -1; // used for smooth rendering of the build structure tower animation
    public float clientBuildDiffPerTick = 0; // used for smooth rendering of the build structure tower animation


    public EntityRocketAssembler(BlockPos pos, BlockState blockState) {
        super(ENTITY_ROCKET_ASSEMBLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        buildButton = new guiModuleButton(0, "build", guiHandler, 10, 10, 40, 20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_red.png"), 64, 20);
        statusText = new guiModuleText(1, "status:", guiHandler, 10, 35, 0x00000000, false);
        guiHandler.modules.add(buildButton);
        guiHandler.modules.add(statusText);
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

    public Vec3 getLandingPos(@Nullable EntityRocket rocket) {
        if (areaMin == null || areaMax == null) {
            // if there is no launchpad area, the rocket should land just behind the assembler
            int rocketSize = maxSize; // assume max size by default
            if (rocket != null)
                rocketSize = Math.max(rocket.size.getZ(), rocket.size.getX());
            int offset = rocketSize / 2 + 3;
            Direction launchpadDir = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
            BlockPos landingPos = getBlockPos();
            for (int i = 0; i < offset; i++) {
                landingPos = landingPos.relative(launchpadDir);
            }
            return new Vec3(landingPos.getCenter().x, landingPos.getCenter().y, landingPos.getCenter().z);
        } else {
            // if there is a launchpad, land in center
            Vec3 landingPos = new Vec3(
                    (double) (areaMin.getX() + areaMax.getX()) / 2 + 0.5,
                    areaMin.getY(),
                    (double) (areaMin.getZ() + areaMax.getZ()) / 2 + 0.5
            );
            return landingPos;
        }
    }

    public EntityRocket getRocket() {
        // return current rocket reference
        return currentRocket;
    }

    public void scanForSpaceDockingArea() {

    }

    public void scanForLaunchPadArea() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // the position behind & below the assembler
        BlockPos startingPos = getBlockPos().below().offset(facing.getOpposite().getStepX(), facing.getOpposite().getStepY(), facing.getOpposite().getStepZ());

        // going left & right up to maxSize blocks to find the largest rectangle area
        // it will use as starting points one of side1 and one of side2 so the area will always include the starting pos
        Set<BlockPos> side1 = new HashSet<>();
        Set<BlockPos> side2 = new HashSet<>();
        for (int i = 0; i < maxSize; i++) {
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
                if (p1.distManhattan(p2) < 2 || p1.distManhattan(p2) > maxSize) {
                    continue;
                }
                // try all possible depth values
                for (int depth = 2; depth < maxSize; depth++) {
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
                            level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);

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

                                // add 1 block padding to area
                                areaMin = new BlockPos(areaMin.getX() + 1, areaMin.getY(), areaMin.getZ() + 1);
                                areaMax = new BlockPos(areaMax.getX() - 1, areaMax.getY(), areaMax.getZ() - 1);
                            }
                        }
                    }
                }
            }
        }
    }

    public void scanArea() {
        if (level.isClientSide) return;
        //long t0 = System.currentTimeMillis();
        Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (myDim != null && myDim.getType() == DimensionProperties.DimensionType.SPACE_STATION) {
            scanForSpaceDockingArea();
        } else {
            scanForLaunchPadArea();
        }
        broadcastInformationToPlayers(null);
        //long t1 = System.currentTimeMillis();
        //System.out.println("scan complete in " +(t1-t0) +"ms");
    }

    public constuctionInfo buildRocket(boolean simulate) {
        if (areaMin == null) return new constuctionInfo(false, "invalid launchpad");
        if (areaMax == null) return new constuctionInfo(false, "invalid launchpad");
        if (level.isClientSide) return new constuctionInfo(false, "");

        EntityGuidanceComputer guidanceComputer = null;

        int minX = areaMax.getX();
        int maxX = areaMin.getX();
        int minY = areaMax.getY();
        int maxY = areaMin.getY();
        int minZ = areaMax.getZ();
        int maxZ = areaMin.getZ();
        for (int x = areaMin.getX(); x <= areaMax.getX(); x++) {
            for (int y = areaMin.getY(); y <= areaMax.getY(); y++) {
                for (int z = areaMin.getZ(); z <= areaMax.getZ(); z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)) != Blocks.AIR.defaultBlockState()) {
                        if (minX > x)
                            minX = x;
                        if (minY > y)
                            minY = y;
                        if (minZ > z)
                            minZ = z;

                        if (maxX < x)
                            maxX = x;
                        if (maxY < y)
                            maxY = y;
                        if (maxZ < z)
                            maxZ = z;
                    }
                }
            }
        }

        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    BlockPos inRocketPos = pos.subtract(new BlockPos(minX, minY, minZ));
                    blocks.put(inRocketPos, state);

                    if (state.getBlock() instanceof EntityBlock entityBlock) {
                        boolean shouldSaveNbt = false; // only save for some BEs
                        if (entityBlock instanceof GuidanceComputer)
                            shouldSaveNbt = true;

                        CompoundTag tag = null;
                        if (shouldSaveNbt) {
                            BlockEntity be = level.getBlockEntity(pos);
                            tag = be.saveCustomOnly(level.registryAccess());
                        }
                        BlockEntity newEntity = entityBlock.newBlockEntity(inRocketPos, state);
                        if (shouldSaveNbt) {
                            newEntity.loadCustomOnly(tag, level.registryAccess());
                        }
                        blockEntities.put(inRocketPos, newEntity);
                    }

                    if (state.getBlock() instanceof GuidanceComputer) {
                        if (guidanceComputer != null)
                            return new constuctionInfo(false, "multiple guidance computers found");
                        guidanceComputer = (EntityGuidanceComputer) level.getBlockEntity(pos);
                    }
                }
            }
        }
        if (guidanceComputer == null) {
            return new constuctionInfo(false, "missing guidance computer");
        }

        if (!simulate) {
            Vec3i size = new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
            Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            Vec3 front = new Vec3(facing.getStepX(), 0, facing.getStepZ());
            EntityRocket rocket = EntityRocket.create(level, blocks, blockEntities, size, front);
            double launchPadCenterX = (double) (areaMax.getX() + areaMin.getX()) / 2 + 0.5;
            double launchPadCenterZ = (double) (areaMax.getZ() + areaMin.getZ()) / 2 + 0.5;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // prevent item pops when breaking the block for specific blocks that carry their inventory to the rocket
                        if (level.getBlockEntity(pos) instanceof EntityGuidanceComputer guidanceComputer1)
                            guidanceComputer1.itemStackHandler.setStackInSlot(0, ItemStack.EMPTY);

                        // if i understand this correctly, 2 = send to clients, 16 = no neighbor update
                        // neighbor could break some blocks like sign that would pop away
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
            }
            rocket.moveTo(launchPadCenterX, areaMin.getY() + 0.02, launchPadCenterZ, 0, 0);
            level.addFreshEntity(rocket);
        }
        return new constuctionInfo(true, "");
    }

    public void broadcastInformationToPlayers(ServerPlayer p) {
        CompoundTag info = new CompoundTag();
        if (areaMin != null && areaMax != null) {
            info.putInt("minX", areaMin.getX());
            info.putInt("minY", areaMin.getY());
            info.putInt("minZ", areaMin.getZ());
            info.putInt("maxX", areaMax.getX());
            info.putInt("maxY", areaMax.getY());
            info.putInt("maxZ", areaMax.getZ());
        } else {
            info.put("noArea", new CompoundTag());
        }

        info.putInt("buildProgress", buildProgress);

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

        if (compoundTag.contains("guiButtonClick")) {
            int id = compoundTag.getInt("guiButtonClick");
            if (id == 0) {
                constuctionInfo ret = buildRocket(true);
                statusText.setTextAndSync(ret.info);
                if (ret.canConstruct) {
                    // add more time for the client structure tower to go up and stay and wait, this is why multiplier and offset
                    buildProgress = (int) (buildTimeBase * (areaMax.getY() - areaMin.getY() + 2) * 1.5);
                }
            }
        }

        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        if (compoundTag.contains("noArea")) {
            areaMin = null;
            areaMax = null;
        }

        if (compoundTag.contains("minX") && compoundTag.contains("minY") && compoundTag.contains("minZ"))
            areaMin = new BlockPos(compoundTag.getInt("minX"), compoundTag.getInt("minY"), compoundTag.getInt("minZ"));

        if (compoundTag.contains("maxX") && compoundTag.contains("maxY") && compoundTag.contains("maxZ"))
            areaMax = new BlockPos(compoundTag.getInt("maxX"), compoundTag.getInt("maxY"), compoundTag.getInt("maxZ"));

        if (compoundTag.contains("buildProgress")) {
            buildProgress = compoundTag.getInt("buildProgress");
        }

        //System.out.println(areaMin);
        //System.out.println(areaMax);
        guiHandler.readClient(compoundTag);
    }

    public void tick() {

        if (level.isClientSide) {
            if (clientBuildProgress < buildProgress) {
                clientBuildProgress += 2;
                clientBuildDiffPerTick = 2;
            } else if (clientBuildProgress > buildProgress) {
                clientBuildProgress--;
                clientBuildDiffPerTick = -1;
            } else {
                clientBuildDiffPerTick = 0;
            }
        }

        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (buildProgress > -1) {
                if (areaMin != null && areaMax != null) {
                    buildProgress--;
                    if (buildProgress == -1) {
                        buildRocket(false);
                    }
                } else {
                    buildProgress = -1;
                }
                broadcastInformationToPlayers(null);
            }


            // remove reference to current rocket if it is removed
            if (currentRocket != null && currentRocket.isRemoved())
                currentRocket = null;

            // remove reference when the rocket program is null,
            // the following scan will reset it if it is still on launchpad area
            if (currentRocket != null && currentRocket.getCurrentProgram() == null)
                currentRocket = null;

            // scan if there is a new rocket in the landing area to be the new rocket reference
            // TODO: if this takes too long, maybe do it only once per second ?
            AABB area;
            if (areaMin == null || areaMax == null) {
                Vec3 landingPos = getLandingPos(null);
                area = new AABB(landingPos.subtract(1, 1, 1), landingPos.add(1, 1, 1)).inflate((double) maxSize /2+1);
            } else {
                area = new AABB(new Vec3(areaMin.getX(), areaMin.getY(), areaMin.getZ()), new Vec3(areaMax.getX() + 1, areaMax.getY(), areaMax.getZ() + 1)).inflate(1, 2, 1);
            }
            List<EntityRocket> rockets = level.getEntitiesOfClass(EntityRocket.class, area);
            if (!rockets.isEmpty()) {
                rockets.sort((r1,r2)->{
                    double dr1 = r1.position().distanceTo(getBlockPos().getCenter());
                    double dr2 = r2.position().distanceTo(getBlockPos().getCenter());
                    return (dr1 > dr2) ? 1:-1;
                });
                currentRocket = rockets.getFirst();
            }


            if (currentRocket != null) {

                // TODO: this with nice vertical / horizontal progress bars?
                String newStatus = new String();
                if (currentRocket.getCurrentProgram() == null)
                    newStatus += "rocket landed\n";
                else
                    newStatus += "rocket in flight\n";
                newStatus += "Thrust: " + ((float) Math.round(currentRocket.getThrustMax() * 100) / 100) + "\n";
                newStatus += "Mass: " + ((float) Math.round(currentRocket.getMass() * 100) / 100) + "\n";
                newStatus += "Weight: " + ((float) Math.round(currentRocket.getMass() * currentRocket.getGravity() * 100) / 100) + "\n";
                newStatus += "Thrust: " + Math.round(currentRocket.controller.getCurrentThrust() * 100) + "%\n";
                newStatus += "Fuel: " + ((float) currentRocket.getFuel() / 1000) + " / " + ((float) currentRocket.fuelTank.getCapacity() / 1000) + "\n";
                statusText.setTextAndSync(newStatus);

                buildButton.setIsEnabledAndBroadcastUpdate(false);
            }

            if(currentRocket == null) {
                if (areaMin == null || areaMax == null) {
                    if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()).getType() != DimensionProperties.DimensionType.SPACE_STATION)
                        statusText.setTextAndSync("No launchpad detected");
                    else
                        statusText.setTextAndSync("Launch zone not detected");

                    buildButton.setIsEnabledAndBroadcastUpdate(false);
                } else {
                    buildButton.setIsEnabledAndBroadcastUpdate(true);

                    statusText.setTextAndSync(
                            "Ready to build a rocket!\n\nA Rocket requires\n" +
                                    "- 1 Guidance Computer\n"+
                                    "- Thrusters\n"+
                                    "- FuelTanks\n"
                    );
                }
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("buildProgress", buildProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        buildProgress = tag.getInt("buildProgress");
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityRocketAssembler) t).tick();
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(200, 200, true);
    }

    public static class constuctionInfo {
        boolean canConstruct = false;
        String info = "";

        constuctionInfo(boolean canConstruct, String info) {
            this.canConstruct = canConstruct;
            this.info = info;
        }
    }
}
