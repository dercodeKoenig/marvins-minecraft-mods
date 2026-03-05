package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Blocks.CargoHold;
import advRocketry.Blocks.GuidanceComputer;
import advRocketry.Blocks.LaunchPad;
import advRocketry.Blocks.StructureTower;
import advRocketry.Config;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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

import javax.annotation.Nullable;
import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_ROCKET_ASSEMBLER;

public class EntityRocketAssembler extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    // the current rocket is the one on launchpad.
    // if there is none on launchpad area, it will keep the reference to the previous one until it is removed or ends its program
    public EntityRocket currentRocket;

    // we output redstone to a comparator when a rocket is landed and has no program running
    public boolean isRedstoneOutputActive = false;

    public BlockEntityBattery battery;

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleButton buildButton;
    public guiModuleButton dockingDirectionButton;
    public guiModuleButton horizontalDockingButton;
    public guiModuleText dockingSettingsTitle;
    public guiModuleText statusText;
    public guiModuleEnergy energyBar;

    public BlockPos areaMin;
    public BlockPos areaMax;

    // for space stations: docking mode settings
    public Direction dockingDirection = Direction.DOWN;
    public boolean horizontalDocking = false;

    public int buildProgress = -1;
    public int clientBuildProgress = -1; // used for smooth rendering of the build structure tower animation
    public float clientBuildDiffPerTick = 0; // used for smooth rendering of the build structure tower animation

    public EntityRocketAssembler(BlockPos pos, BlockState blockState) {
        this(ENTITY_ROCKET_ASSEMBLER.get(), pos, blockState);
    }

    public EntityRocketAssembler(BlockEntityType type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);

        battery = new BlockEntityBattery(this, 10000,1000);

        makeGui();
    }

    public void makeGui() {
        guiHandler = new GuiHandlerBlockEntity(this);
        buildButton = new guiModuleButton(0, "build", guiHandler, 10, 10, 40, 20, BTN_BLACK, BTN_W, BTN_W);
        statusText = new guiModuleText(1, "status:", guiHandler, 10, 10, 0x00000000, false);
        guiHandler.modules.add(buildButton);
        guiHandler.modules.add(statusText);
        energyBar = new guiModuleEnergy(2, battery, guiHandler, 138, 7);
        guiHandler.modules.add(energyBar);

        dockingDirectionButton = new guiModuleButton(30, "direction", guiHandler, 10, 100, 60, 20, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(dockingDirectionButton);
        horizontalDockingButton = new guiModuleButton(31, "mode", guiHandler, 90, 100, 60, 20, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(horizontalDockingButton);
        dockingSettingsTitle = new guiModuleText(32, "Docking Settings:", guiHandler, 10, 85, 0x00000000, false);
        guiHandler.modules.add(dockingSettingsTitle);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level.isClientSide) {
            CompoundTag ping = new CompoundTag();
            ping.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, ping));
        } else {
            onDockingSettingsChanged();
        }
    }

    public Direction getDockingDirection(){
        if(DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location()) instanceof  SpaceStationDimension){
            return dockingDirection;
        }
        return Direction.UP; // this should not even be required anywhere but just for having it correct
    }
    public Vec3 getMoveAwayDirection(){
        Direction moveAwayDirection = Direction.UP;
        if(DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location()) instanceof  SpaceStationDimension){
            moveAwayDirection = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        Vec3 moveAway = new Vec3(moveAwayDirection.getStepX(), moveAwayDirection.getStepY(), moveAwayDirection.getStepZ());
        return moveAway;
    }

    public Vec3 getLandingPos(@Nullable EntityRocket rocket) {
        Vec3 landPos = Vec3.ZERO;
        boolean isInSpaceStation = DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location()) instanceof SpaceStationDimension;
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (areaMin == null || areaMax == null || isInSpaceStation) {
            // if there is no launchpad area, the rocket should land just behind the assembler
            // also in space stations we calculate this independent of the launchpad structure to align the rocket at the correct position
            int rocketSize = Config.INSTANCE.rocket_Assembler_Max_Size; // assume max size by default
            if (rocket != null)
                rocketSize = Math.max(rocket.size.getZ(), rocket.size.getX());
            int offset = rocketSize / 2 + 2;
            BlockPos landingPos = getBlockPos().relative(facing.getOpposite(), offset);
            landPos = new Vec3(landingPos.getCenter().x, landingPos.getY(), landingPos.getCenter().z);
        } else {
            // if there is a launchpad, land in center
            Vec3 landingPos = new Vec3(
                    (double) (areaMin.getX() + areaMax.getX()) / 2 + 0.5,
                    areaMin.getY(),
                    (double) (areaMin.getZ() + areaMax.getZ()) / 2 + 0.5
            );
            landPos = landingPos;
        }

        // if in space station with horizontal docking, the position has to be adjusted
        // this can only be done if rocket is supplied, the programs should supply the current rocket for calculations
        if (isInSpaceStation) {
            if (horizontalDocking && rocket != null) {
                // a horizontal rocket rotates around center, so size.Y / 2
                // so the docking position needs to be lowered by half y size
                int sizeY = rocket.size.getY();
                double halfY = (double) sizeY / 2;
                int sizeHorizontal = Math.max(rocket.size.getX(), rocket.size.getZ());
                double halfSizeHorizontal = (double) sizeHorizontal / 2;
                landPos = new Vec3(landPos.x, landPos.y - halfY + halfSizeHorizontal, landPos.z);

                // the rocket will also need to move more away
                landPos = landPos.relative(facing.getOpposite(),halfY - halfSizeHorizontal);
            }
        }

        return landPos;
    }

    public EntityRocket getRocket() {
        // return current rocket reference
        return currentRocket;
    }

    public boolean isRedstoneOutputActive(){
        return this.isRedstoneOutputActive;
    }

    public void scanForSpaceDockingArea() {
        scanForLaunchPadArea();
    }

    public void scanForLaunchPadArea() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // the position behind & below the assembler
        BlockPos startingPos = getBlockPos().below().offset(facing.getOpposite().getStepX(), facing.getOpposite().getStepY(), facing.getOpposite().getStepZ());

        // going left & right up to maxSize blocks to find the largest rectangle area
        // it will use as starting points one of side1 and one of side2 so the area will always include the starting pos
        Set<BlockPos> side1 = new HashSet<>();
        Set<BlockPos> side2 = new HashSet<>();
        for (int i = 0; i < Config.INSTANCE.rocket_Assembler_Max_Size; i++) {
            Direction side1Direction = facing.getClockWise();
            Direction side2Direction = facing.getCounterClockWise();
            BlockPos side1Next = new BlockPos(startingPos).offset(side1Direction.getStepX() * i, side1Direction.getStepY() * i, side1Direction.getStepZ() * i);
            BlockPos side2Next = new BlockPos(startingPos).offset(side2Direction.getStepX() * i, side2Direction.getStepY() * i, side2Direction.getStepZ() * i);
            // early check if there is a launchpad because if not, the entire row can be ignored
            // this makes significant speedup
            if(level.getBlockState(side1Next).getBlock() instanceof LaunchPad)
                side1.add(side1Next);
            if(level.getBlockState(side2Next).getBlock() instanceof LaunchPad)
                side2.add(side2Next);
        }

        // we keep the area with the largest volume because there can be many possible areas
        int largestAreaVolume = 0;
        areaMin = null;
        areaMax = null;

        // try all combinations of left & right positions with a min and max distance
        for (BlockPos p1 : side1) {
            for (BlockPos p2 : side2) {
                if (p1.distManhattan(p2) < 2 || p1.distManhattan(p2) > Config.INSTANCE.rocket_Assembler_Max_Size) {
                    continue;
                }
                // try all possible depth values
                for (int depth = 2; depth < Config.INSTANCE.rocket_Assembler_Max_Size; depth++) {
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
                            level.getChunk(pos.x, pos.z);

                            Block block = level.getBlockState(target).getBlock();
                            if (!(block instanceof LaunchPad)) {
                                isAllValid = false;
                                break;
                            }
                        }
                        if(!isAllValid)
                            break;
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

    // TODO: lazy scanning please!!! it causes too much lag every time i place a block connected to the assembler
    //      maybe wait 2 seconds before scan so we only scan when player is done placing blocks?
    //       or another thread?
    public void scanArea() {
        if (level.isClientSide) return;
        //long t0 = System.currentTimeMillis();
        Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
        if (myDim instanceof SpaceStationDimension) {
            scanForSpaceDockingArea();
        } else {
            scanForLaunchPadArea();
        }
        broadcastInformationToPlayers(null);
        setChanged();
        //long t1 = System.currentTimeMillis();
        //System.out.println("scan complete in " +(t1-t0) +"ms");
    }

    public ConstructionResult buildRocket(boolean simulate) {
        if (level.isClientSide) return null;

        if (areaMin == null) return ConstructionResult.INVALID_LAUNCHPAD;
        if (areaMax == null) return ConstructionResult.INVALID_LAUNCHPAD;

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
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
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
                    if(state.isAir())
                        continue;

                    BlockPos inRocketPos = pos.subtract(new BlockPos(minX, minY, minZ));
                    blocks.put(inRocketPos, state);

                    if (state.getBlock() instanceof EntityBlock entityBlock) {
                        boolean shouldSaveNbt = false; // only save for some BEs
                        if (entityBlock instanceof GuidanceComputer)
                            shouldSaveNbt = true;
                        if (entityBlock instanceof CargoHold)
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
                            return ConstructionResult.TOO_MANY_GUIDANCE_COMPUTERS;
                        guidanceComputer = (EntityGuidanceComputer) level.getBlockEntity(pos);
                    }
                }
            }
        }
        if (guidanceComputer == null) {
            return ConstructionResult.NO_GUIDANCE_COMPUTER;
        }

        if (!simulate) {

            // remove blocks
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // prevent item pops when breaking the block for specific blocks that carry their inventory to the rocket
                        if (level.getBlockEntity(pos) instanceof EntityGuidanceComputer guidanceComputer1)
                            guidanceComputer1.itemStackHandler.setStackInSlot(0, ItemStack.EMPTY);
                        if (level.getBlockEntity(pos) instanceof EntityCargoHold cargoHold)
                            cargoHold.itemStackHandler.setStackInSlot(0, ItemStack.EMPTY);

                        // if i understand this correctly, 2 = send to clients, 16 = no neighbor update
                        // neighbor could break some blocks like sign that would pop away
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
            }

            // spawn rocket
            Vec3i size = new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
            Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            Vec3 front = new Vec3(facing.getStepX(), 0, facing.getStepZ());
            EntityRocket rocket = EntityRocket.create(level, blocks, blockEntities, size, front);
            double launchPadCenterX = (double) (areaMax.getX() + areaMin.getX()) / 2 + 0.5;
            double launchPadCenterZ = (double) (areaMax.getZ() + areaMin.getZ()) / 2 + 0.5;
            rocket.moveTo(launchPadCenterX, areaMin.getY() + 0.02, launchPadCenterZ, 0, 0);
            level.addFreshEntity(rocket);
        }
        return ConstructionResult.SUCCESS;
    }

    public void onDockingSettingsChanged() {
        if (!(DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location()) instanceof SpaceStationDimension)) {
            dockingDirectionButton.setIsEnabledAndBroadcastUpdate(false);
            horizontalDockingButton.setIsEnabledAndBroadcastUpdate(false);
            dockingSettingsTitle.setIsEnabledAndBroadcastUpdate(false);
        } else {
            dockingDirectionButton.setIsEnabledAndBroadcastUpdate(true);
            horizontalDockingButton.setIsEnabledAndBroadcastUpdate(true);
            dockingSettingsTitle.setIsEnabledAndBroadcastUpdate(true);
        }
        dockingDirectionButton.setTextAndSync(dockingDirection.getName());
        horizontalDockingButton.setTextAndSync(horizontalDocking ? "horizontal" : "vertical");
        broadcastInformationToPlayers(null);
        setChanged();
    }

    public void broadcastInformationToPlayers(ServerPlayer p) {
        CompoundTag info = new CompoundTag();
        saveAdditional(info, level.registryAccess()); // most of the save data is required on client side so just send it all
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
                ConstructionResult ret = buildRocket(true);
                if (ret == ConstructionResult.SUCCESS) {
                    // add more time for the client structure tower to go up and stay and wait, this is why multiplier and offset
                    buildProgress = (int) (Config.INSTANCE.rocket_Assembler_Build_Time_Base * (areaMax.getY() - areaMin.getY() + 2) * 1.5);

                    // signal client to close the gui
                    guiHandler.signalCloseGui(serverPlayer);
                }
            }

            if (id == 30) {
                if (dockingDirection == Direction.UP)
                    dockingDirection = Direction.DOWN;
                else if (dockingDirection == Direction.DOWN)
                    dockingDirection = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
                else
                    dockingDirection = Direction.UP;
                onDockingSettingsChanged();
            }
            if (id == 31) {
                horizontalDocking = !horizontalDocking;
                onDockingSettingsChanged();
            }
            setChanged();
        }

        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        loadAdditional(compoundTag, level.registryAccess());
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("buildProgress", buildProgress);
        tag.putInt("energy", battery.getEnergyStored());
        tag.putInt("dockingDirection", dockingDirection.ordinal());
        tag.putBoolean("horizontalDocking", horizontalDocking);
        if (areaMin != null && areaMax != null) {
            tag.putInt("minX", areaMin.getX());
            tag.putInt("minY", areaMin.getY());
            tag.putInt("minZ", areaMin.getZ());
            tag.putInt("maxX", areaMax.getX());
            tag.putInt("maxY", areaMax.getY());
            tag.putInt("maxZ", areaMax.getZ());
        } else {
            tag.put("noArea", new CompoundTag());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("noArea")) {
            areaMin = null;
            areaMax = null;
        }
        if (tag.contains("minX") && tag.contains("minY") && tag.contains("minZ"))
            areaMin = new BlockPos(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"));
        if (tag.contains("maxX") && tag.contains("maxY") && tag.contains("maxZ"))
            areaMax = new BlockPos(tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ"));
        if (tag.contains("buildProgress"))
            buildProgress = tag.getInt("buildProgress");
        if (tag.contains("energy"))
            battery.setEnergy(tag.getInt("energy"));
        if (tag.contains("dockingDirection"))
            dockingDirection = Direction.values()[tag.getInt("dockingDirection")];
        if (tag.contains("horizontalDocking"))
            horizontalDocking = tag.getBoolean("horizontalDocking");
    }

    public void tick() {

        if (level.isClientSide) {
            // build progress logic client
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

            // build progress logic server
            if (buildProgress > -1) {
                if (areaMin != null && areaMax != null) {
                    boolean shouldConsumeEnergy = buildProgress <= Config.INSTANCE.rocket_Assembler_Build_Time_Base * (areaMax.getY() - areaMin.getY()+2);
                    if(battery.getEnergyStored() >= Config.INSTANCE.rocket_Assembler_Energy_Per_Tick || !shouldConsumeEnergy) {
                        buildProgress--;
                        if(shouldConsumeEnergy)
                            battery.extractEnergy(Config.INSTANCE.rocket_Assembler_Energy_Per_Tick,false);
                        if (buildProgress == -1) {
                            buildRocket(false);
                        }
                        setChanged();
                    }
                } else {
                    buildProgress = -1;
                }
                broadcastInformationToPlayers(null);
            }

            // recalculate redstone output and notify level of change
            boolean shouldOutputRedstone = currentRocket != null && currentRocket.getCurrentProgram() == null;
            if(shouldOutputRedstone != isRedstoneOutputActive){
                isRedstoneOutputActive = shouldOutputRedstone;
                setChanged(); // <- updates neighbors for redstone signal
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
                Direction facingOpposite = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
                BlockPos start = getBlockPos().relative(facingOpposite);
                double scale = (double) Config.INSTANCE.rocket_Assembler_Max_Size / 2 + 5;
                // i will scan a narrow area in front where a rocket could possibly have landed / docked when there was no docking area / launchpad
                area = new AABB(start).inflate(1).inflate(facingOpposite.getStepX()*scale, 0, facingOpposite.getStepZ()*scale);
            } else {
                area = new AABB(new Vec3(areaMin.getX(), areaMin.getY(), areaMin.getZ()), new Vec3(areaMax.getX() + 1, areaMax.getY(), areaMax.getZ() + 1)).inflate(1, 2, 1);
            }
            List<EntityRocket> rockets = level.getEntitiesOfClass(EntityRocket.class, area);
            if (!rockets.isEmpty()) {
                rockets.sort((r1, r2) -> {
                    double dr1 = r1.position().distanceTo(getBlockPos().getCenter());
                    double dr2 = r2.position().distanceTo(getBlockPos().getCenter());
                    return (dr1 > dr2) ? 1 : -1;
                });
                currentRocket = rockets.getFirst();
            }

            if(currentRocket != null) {
                if(currentRocket.getCurrentProgram() == null) {
                    // when the rocket is landed, set its docking station position to this
                    currentRocket.setDockingStationPos(getBlockPos(), true);
                }
            }

            // update gui
            if (currentRocket != null) {
                String newStatus = new String();
                if (currentRocket.getCurrentProgram() == null)
                    newStatus += "rocket landed\n";
                else
                    newStatus += "rocket in flight\n";
                newStatus += "\nThrust max: " + ((float) Math.round(currentRocket.getThrustMax() * 100) / 100) + "\n";
                newStatus += "Mass: " + ((float) Math.round(currentRocket.getMass() * 100) / 100) + "\n";
                newStatus += "Weight: " + ((float) Math.round(currentRocket.getMass() * currentRocket.getGravity() * 100) / 100) + "\n";
                newStatus += "Thrust: " + Math.round(currentRocket.controller.getCurrentThrust() * 100) + "%\n";
                newStatus += "Fuel: " + String.format("%.2f", ((float) currentRocket.getFuel() / 1000)) + " / " + ((float) currentRocket.fuelTank.getCapacity() / 1000) + "\n";
                statusText.setTextAndSync(newStatus);

                buildButton.setIsEnabledAndBroadcastUpdate(false);
                energyBar.setIsEnabledAndBroadcastUpdate(false);
            }

            if (currentRocket == null) {
                if (areaMin == null || areaMax == null) {
                    if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof SpaceStationDimension)
                        statusText.setTextAndSync("Launch zone not detected");
                    else
                        statusText.setTextAndSync("No launchpad detected");

                    buildButton.setIsEnabledAndBroadcastUpdate(false);
                    energyBar.setIsEnabledAndBroadcastUpdate(false);

                } else {
                    buildButton.setIsEnabledAndBroadcastUpdate(true);
                    energyBar.setIsEnabledAndBroadcastUpdate(true);

                    statusText.setTextAndSync(
                            "\n\n\nReady to build a rocket!\n\nA Rocket requires\n" +
                                    "- 1 Guidance Computer\n" +
                                    "- Thrusters\n" +
                                    "- FuelTanks\n"
                    );
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityRocketAssembler) t).tick();
    }

    public void openGui() {
        if (level.isClientSide) {
            int h = 100;
            if(DimensionManager.getDimensionManager(level.isClientSide).get(level.dimension().location()) instanceof SpaceStationDimension)
                h = 130; // the docking mode buttons
            guiHandler.openGui(160, h, true);
        }
    }

    public enum ConstructionResult{
        SUCCESS(""),
        NO_GUIDANCE_COMPUTER("NO_GUIDANCE_COMPUTER"),
        TOO_MANY_GUIDANCE_COMPUTERS("TOO_MANY_GUIDANCE_COMPUTERS"),
        INVALID_LAUNCHPAD("INVALID_LAUNCHPAD");

        final String info;
        ConstructionResult(String info){
            this.info = info;
        }
    }
}
