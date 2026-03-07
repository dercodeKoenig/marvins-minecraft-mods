package advRocketry.Rocket.RocketPrograms;

import advRocketry.BlockEntities.EntityCargoHold;
import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Dimension.*;
import advRocketry.Items.ItemSpaceStationContainer;
import advRocketry.Rocket.EntityRocket;
import advRocketry.Rocket.RocketProgram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.Objects;

public class ProgramNavigateToSpaceStation implements RocketProgram {

    public static String id = "ProgramNavigateToSpaceStation";

    public ResourceLocation targetDimensionId;
    public ResourceLocation originDimensionId;
    public BlockPos target;

    // the position where we will spawn depends on where the docking station is placed and how it is rotated
    // using random position when no docking station is there
    BlockPos spawnPos;

    // for docking to a rocket assembler
    StationDockingProgram.DockingProgram dockingProgram;
    // for navigating from one docking station to another on same station, we need to undock first
    StationDockingProgram.UnDockingProgram unDockingProgram;
    // in case we are not already on target dimension
    NavigateToSpaceTravelDimension navigateToSpaceTravelDimension;

    public ProgramNavigateToSpaceStation() {
        // empty constructor required for save & load
    }

    public ProgramNavigateToSpaceStation( EntityRocket rocket, ResourceLocation targetDimensionId, BlockPos target) {
        this.target = target;
        this.targetDimensionId = targetDimensionId;
        this.originDimensionId = rocket.level().dimension().location();


        ServerLevel targetLevel = DimensionManager.getServerLevel(targetDimensionId);
        targetLevel.getChunk(target); // should load the chunk
        if (targetLevel.getBlockEntity(target) instanceof EntityRocketAssembler rocketAssembler) {
            Vec3 dockingPosition = rocketAssembler.getLandingPos(rocket);
            Direction dockingStationFacing = rocketAssembler.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction dockingDirection = rocketAssembler.getDockingDirection();
            spawnPos = target.relative(dockingStationFacing.getOpposite(), 200);
            dockingProgram = new StationDockingProgram.DockingProgram(target, dockingPosition, dockingDirection);
        } else {
            spawnPos = target.relative(Direction.NORTH, 200);
        }

        if (originDimensionId.equals(targetDimensionId)) {
            BlockPos undockingStationPos = rocket.getDockingStationPos();
            BlockEntity undockingStation = null;
            if (undockingStationPos != null) {
                targetLevel.getChunk(undockingStationPos); // should load the chunk
                undockingStation = rocket.level().getBlockEntity(undockingStationPos);
            }

            if (undockingStation instanceof EntityRocketAssembler rocketAssembler) {
                unDockingProgram = new StationDockingProgram.UnDockingProgram(
                        rocketAssembler.getLandingPos(rocket),
                        rocketAssembler.getDockingDirection(),
                        rocketAssembler.getMoveAwayDirection()
                );
            }
        } else {
            navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension(rocket);
        }
    }

    public void run(EntityRocket rocket) {

        SpaceStationDimension targetDimension = (SpaceStationDimension) DimensionManager.getDimensionManager(rocket.level().isClientSide).get(targetDimensionId);
        if (targetDimension == null) {
            rocket.setDeltaMovement(0, 0, 0);
            rocket.endProgram();
            return;
        }

        if (rocket.level().dimension().location().equals(targetDimensionId)) {

            if(unDockingProgram != null) {
                unDockingProgram.run(rocket,
                        () -> {
                            // when on same dimension, stop after undocked from other station
                            unDockingProgram = null;
                        },
                        () -> {
                            // if we are already undocked for some reason but still run this program, end it here!
                            unDockingProgram = null;
                        });
            }
            else {
                if (dockingProgram == null) {
                    runWithoutDockingStation(rocket);
                } else {
                    dockingProgram.run(rocket);
                }
            }

        } else {
            if (rocket.level().dimension().location().equals(RocketTravelDimension.dimId)) {
                // we are in space, navigate to the target planet and teleport the rocket to target dim
                NavigateInSpaceToTargetDimension.run(rocket, targetDimensionId, originDimensionId, () -> {
                    teleportToStation(rocket);
                });
            } else {
                // we are not at target dim, move to space!
                navigateToSpaceTravelDimension.run(rocket, new NavigateToSpaceTravelDimension.SpaceReachedCallback() {
                    public boolean onSpaceReached() {
                        if(rocket.level().isClientSide) return false;

                        if (!targetDimension.isPositionInitialized()) {
                            // it has no location in space at this time, it would probably just fly to 0 0 0
                            // the position will be initialized on space reached, no going to travel dimension
                            teleportToStation(rocket);
                            return true;
                        }

                        Dimension rocketDimension = DimensionManager.INSTANCE_SERVER.get(rocket.level().dimension().location());
                        if (rocketDimension instanceof SpaceStationDimension otherStation) {
                            if (Objects.equals(otherStation.getParentDimensionId(), targetDimension.getParentDimensionId())) {
                                if (otherStation.isInOrbit() && targetDimension.isInOrbit()) {
                                    // skip space travel on station 2 station when in same orbit
                                    teleportToStation(rocket);
                                    return true;
                                }
                            }
                        }
                        if (rocketDimension instanceof PlanetDimension planet) {
                            if (Objects.equals(planet.getDimensionId(), targetDimension.getParentDimensionId())) {
                                if (targetDimension.isInOrbit()) {
                                    // skip space travel on planet 2 station when station is in planet orbit
                                    teleportToStation(rocket);
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                });
            }
        }
    }



    void runWithoutDockingStation(EntityRocket rocket) {

        rocket.controller.enableMainEngines(true, false);
        rocket.controller.enableSecondaryEngines(true, false);
        rocket.controller.setRotationRateMultiplier(1, false);
        rocket.controller.setDefaultTargetHeading(rocket.controller.getHeading(), false);
        rocket.controller.setTargetFront(new Vec3(0,1,0), false);

        Vec3 toTarget = target.getCenter().subtract(rocket.position());

        double maxD = 100;
        if (toTarget.length() > maxD) {
            toTarget = toTarget.normalize().scale(maxD);
        }
        Vec3 targetVec3 = rocket.position().add(toTarget);

        rocket.controller.setTargetPosition(targetVec3, false);

        // check if stopped (at target or collision maybe)
        if (rocket.getDeltaMovement().length() < 0.01 && toTarget.length() < 50) {
            if(!rocket.level().isClientSide) {
                rocket.setDeltaMovement(0, 0, 0);
            }
            rocket.endProgram();


            // place the initial blocks from container if possible / required
            SpaceStationDimension spaceStationDimension = (SpaceStationDimension) DimensionManager.INSTANCE_SERVER.get(targetDimensionId);
            if (!spaceStationDimension.initialBlocksPlaced()) {
                placeInitialBlocks(rocket);
            }
        }
    }

    public void teleportToStation(EntityRocket rocket) {
        if(rocket.level().isClientSide) return;

        ServerLevel targetLevel = DimensionManager.getServerLevel(targetDimensionId);

        Vec3 targetPos = spawnPos.getCenter();

        Vec3 entrySpeed = new Vec3(0, 0, 0);

        EntityRocket newRocket = rocket.teleportTo(targetLevel, targetPos, entrySpeed);

        Vec3 toTarget = target.getCenter().subtract(newRocket.position());
        newRocket.controller.setHeadingAndFrontDirect(toTarget, toTarget.cross(new Vec3(0, 1, 0).cross(toTarget)));

        // if station is first visited, set position and parent dimension
        SpaceStationDimension spaceStationDimension = (SpaceStationDimension) DimensionManager.INSTANCE_SERVER.get(targetDimensionId);
        if (!spaceStationDimension.isPositionInitialized()) {
            // set the position and parent for the station depending on where we launch it
            if (DimensionManager.INSTANCE_SERVER.get(originDimensionId) instanceof PlanetDimension planet) {
                spaceStationDimension.initializePosition(
                        null,
                        planet.getDimensionId()
                );
            } else if (DimensionManager.INSTANCE_SERVER.get(originDimensionId) instanceof SpaceStationDimension originStation) {
                spaceStationDimension.initializePosition(
                        originStation.getPosition(0),
                        originStation.getParentDimensionId()
                );
            } else {
                spaceStationDimension.initializePosition(rocket.universePosition, null);
            }
        }
    }

    void placeInitialBlocks(EntityRocket rocket) {
        for (BlockEntity e : rocket.blockEntities.values()) {
            if (e instanceof EntityCargoHold cargoHold) {
                for (int i = 0; i < cargoHold.itemStackHandler.getSlots(); i++) {
                    ItemStack stack = cargoHold.itemStackHandler.extractItem(i, 1, true);
                    if (stack.getItem() instanceof ItemSpaceStationContainer) {
                        stack = cargoHold.itemStackHandler.extractItem(i, 1, false);
                        Map<BlockPos, BlockState> blocks = ItemSpaceStationContainer.readBlocks(stack, rocket.level().registryAccess());
                        // find max y x and z to know how far to go down
                        int maxX = 0;
                        int maxY = 0;
                        int maxZ = 0;
                        for (BlockPos p : blocks.keySet()) {
                            maxX = Math.max(p.getX(), maxX);
                            maxY = Math.max(p.getY(), maxY);
                            maxZ = Math.max(p.getZ(), maxZ);
                        }
                        int offsetX = maxX / 2;
                        int offsetZ = maxZ / 2;
                        int offsetY = maxY + 2;
                        BlockPos rocketPos = rocket.blockPosition();
                        for (BlockPos p : blocks.keySet()) {
                            BlockPos target = new BlockPos(
                                    p.getX() - offsetX + rocketPos.getX(),
                                    p.getY() - offsetY + rocketPos.getY(),
                                    p.getZ() - offsetZ + rocketPos.getZ()
                            );
                            // sync to client but no block update
                            rocket.level().setBlock(target, blocks.get(p), 2 | 16);
                        }
                        SpaceStationDimension spaceStationDimension = (SpaceStationDimension) DimensionManager.INSTANCE_SERVER.get(targetDimensionId);
                        spaceStationDimension.setInitialBlocksPlaced();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        target = NbtUtils.readBlockPos(nbt, "target").get();
        spawnPos = NbtUtils.readBlockPos(nbt, "spawnPos").get();
        targetDimensionId = ResourceLocation.parse(nbt.getString("targetDimensionId"));
        originDimensionId = ResourceLocation.parse(nbt.getString("originDimensionId"));

        if (nbt.contains("dockingProgram")) {
            dockingProgram = new StationDockingProgram.DockingProgram();
            dockingProgram.readFromNbt(nbt.getCompound("dockingProgram"));
        }
        if (nbt.contains("unDockingProgram")) {
            unDockingProgram = new StationDockingProgram.UnDockingProgram();
            unDockingProgram.readFromNbt(nbt.getCompound("unDockingProgram"));
        }
        if (nbt.contains("navigateToSpaceTravelDimension")) {
            navigateToSpaceTravelDimension = new NavigateToSpaceTravelDimension();
            navigateToSpaceTravelDimension.readFromNbt(nbt.getCompound("navigateToSpaceTravelDimension"));
        }
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("target", NbtUtils.writeBlockPos(target));
        tag.put("spawnPos", NbtUtils.writeBlockPos(spawnPos));
        tag.putString("targetDimensionId", targetDimensionId.toString());
        tag.putString("originDimensionId", originDimensionId.toString());

        if(dockingProgram != null) {
            tag.put("dockingProgram", dockingProgram.saveToNbt());
        }
        if(unDockingProgram != null){
            tag.put("unDockingProgram", unDockingProgram.saveToNbt());
        }
        if(navigateToSpaceTravelDimension != null){
            tag.put("navigateToSpaceTravelDimension", navigateToSpaceTravelDimension.saveToNbt());
        }
        return tag;
    }
}
