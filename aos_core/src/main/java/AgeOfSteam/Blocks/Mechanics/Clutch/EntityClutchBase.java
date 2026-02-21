package AgeOfSteam.Blocks.Mechanics.Clutch;

import ARLib.network.INetworkTagReceiver;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.*;

public abstract class EntityClutchBase extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    public double inertiaPerSide;
    public double baseFrictionPerSide;
    public double maxStress;
    public double maxForce;

    int timeSinceConnectStart;
    boolean isFullyConnected;
    boolean last_wasPowered = false;
    double currentForceA;
    double currentForceB;
    double currentResistanceA;
    double currentResistanceB;
    double initialRotationDiffSign = 0;
    double lastRotationDiff = 0;
    boolean shouldConnectNextTick = false;

    public AbstractMechanicalBlock myMechanicalBlockA = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return maxStress;
        }

        @Override
        public double getInertia(Direction face) {
            return inertiaPerSide;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return baseFrictionPerSide + currentResistanceA;
        }

        @Override
        public double getTorqueProduced(Direction face) {
            return currentForceA;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };


    public AbstractMechanicalBlock myMechanicalBlockB = new AbstractMechanicalBlock(1, this) {
        @Override
        public double getMaxStress() {
            return maxStress;
        }

        @Override
        public double getInertia(Direction face) {
            return inertiaPerSide;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return baseFrictionPerSide + currentResistanceB;
        }

        @Override
        public double getTorqueProduced(Direction face) {
            return currentForceB;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };

    public EntityClutchBase(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlockA.mechanicalOnload();
        myMechanicalBlockB.mechanicalOnload();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }


    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockClutchBase) {
            if (side == myState.getValue(BlockClutchBase.FACING))
                return myMechanicalBlockA;
            if (side == myState.getValue(BlockClutchBase.FACING).getOpposite())
                return myMechanicalBlockB;
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityClutchBase) t).tick();
    }


    public void tick() {

        myMechanicalBlockA.mechanicalTick();
        myMechanicalBlockB.mechanicalTick();
        if (!level.isClientSide) {
            if (level.hasNeighborSignal(getBlockPos())) {
                if (!last_wasPowered) {
                    // prepare to start connection
                    last_wasPowered = true;
                    shouldConnectNextTick = false;
                    timeSinceConnectStart = 0;
                    initialRotationDiffSign = Math.signum(myMechanicalBlockB.internalVelocity - myMechanicalBlockA.internalVelocity);
                }
                if (!isFullyConnected) {
                    double newRotationDiff = myMechanicalBlockB.internalVelocity - myMechanicalBlockA.internalVelocity;
                    double newRotationDiffSign = Math.signum(newRotationDiff);
                    if (initialRotationDiffSign != newRotationDiffSign || shouldConnectNextTick || Math.abs(newRotationDiff) < 0.00001)
                        isFullyConnected = true;
                    else {
                        double rotationDiff = myMechanicalBlockB.internalVelocity - myMechanicalBlockA.internalVelocity;

                        // if the rotationDiff is less than the change in rotation diff, it would over-deliver force
                        // with this i try to scale force lower to not over-deliver. it is not perfect but better than nothing
                        double a = lastRotationDiff - rotationDiff;
                        lastRotationDiff = rotationDiff;
                        double forceMultiplier = Math.min(1, Math.abs(rotationDiff / a));
                        if (forceMultiplier < 1) {
                            shouldConnectNextTick = true;
                        } else {
                            timeSinceConnectStart += 1;
                        }
                        double forceConstant = 2;
                        double outputForce = Math.signum(rotationDiff) * forceConstant * forceMultiplier * timeSinceConnectStart;
                        outputForce = Math.signum(outputForce) * Math.min(Math.abs(outputForce), maxForce);
                        //System.out.println(outputForce);

                        if (Math.signum(myMechanicalBlockA.internalVelocity) == Math.signum(outputForce) || myMechanicalBlockA.internalVelocity == 0) {
                            currentForceA = outputForce;
                            currentResistanceA = 0;
                        } else {
                            currentForceA = 0;
                            currentResistanceA = Math.abs(outputForce);
                        }

                        if (Math.signum(myMechanicalBlockB.internalVelocity) == Math.signum(-outputForce) || myMechanicalBlockB.internalVelocity == 0) {
                            currentForceB = -outputForce;
                            currentResistanceB = 0;
                        } else {
                            currentForceB = 0;
                            currentResistanceB = Math.abs(outputForce);
                        }
                        //System.out.println(currentForceA+":"+currentResistanceA+ "   " + currentForceB+":"+currentResistanceB);
                    }
                } else {
                    currentForceB = 0;
                    currentResistanceB = 0;
                    currentForceA = 0;
                    currentResistanceA = 0;
                    lastRotationDiff = 0;
                }
            } else {
                last_wasPowered = false;
                isFullyConnected = false;
                currentForceB = 0;
                currentResistanceB = 0;
                currentForceA = 0;
                currentResistanceA = 0;
                lastRotationDiff = 0;
            }
        }
        if (level.isClientSide) {
            if (level.hasNeighborSignal(getBlockPos()) && Math.abs(myMechanicalBlockB.internalVelocity - myMechanicalBlockA.internalVelocity) > 0.5) {
                int i = Math.abs(level.random.nextInt() % 1000);
                boolean doParticle = i < timeSinceConnectStart * 10;
                timeSinceConnectStart++;

                if (doParticle) {
                    double x = level.random.nextDouble() - 0.5;
                    double y = level.random.nextDouble() - 0.5;
                    double z = level.random.nextDouble() - 0.5;
                    level.addParticle(new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 1f), getBlockPos().getCenter().x + x, getBlockPos().getCenter().y + 0.5 + y, getBlockPos().getCenter().z + z, x, y, z);
                }

            } else timeSinceConnectStart = 0;
        }

        if (level.hasNeighborSignal(getBlockPos()) && Math.abs(myMechanicalBlockB.internalVelocity - myMechanicalBlockA.internalVelocity) > 0.5) {
            for (int i = 0; i < 2; i++) {
                SoundEvent[] clutch_sounds = {
                        SoundEvents.GRAVEL_BREAK,
                        SoundEvents.STONE_HIT
                };
                int randomIndex = level.random.nextInt(clutch_sounds.length);
                SoundEvent randomEvent = clutch_sounds[randomIndex];
                level.playSound(null, getBlockPos(), randomEvent,
                        SoundSource.BLOCKS, 0.005f * (float) ((Math.abs(myMechanicalBlockA.internalVelocity - myMechanicalBlockB.internalVelocity))), 0.1f * (float) (Math.abs(myMechanicalBlockA.internalVelocity - myMechanicalBlockB.internalVelocity)));  //
            }
        }
    }

    @Override
    public Map<Direction, AbstractMechanicalBlock> getConnectedParts(IMechanicalBlockProvider mechanicalBlockProvider, AbstractMechanicalBlock MechanicalBlock) {
        Map<Direction, AbstractMechanicalBlock> connectedBlocks = new HashMap<>();

        if (isFullyConnected) {
            if (MechanicalBlock == myMechanicalBlockB) {
                connectedBlocks.put(getBlockState().getValue(BlockClutchBase.FACING), myMechanicalBlockA);
            }
            if (MechanicalBlock == myMechanicalBlockA) {
                connectedBlocks.put(getBlockState().getValue(BlockClutchBase.FACING).getOpposite(), myMechanicalBlockB);
            }
        }

        if (MechanicalBlock == myMechanicalBlockB) {
            BlockEntity otherBE = level.getBlockEntity(getBlockPos().relative(getBlockState().getValue(BlockClutchBase.FACING).getOpposite()));
            if (otherBE instanceof IMechanicalBlockProvider p) {
                AbstractMechanicalBlock other = p.getMechanicalBlock(getBlockState().getValue(BlockClutchBase.FACING));
                if (other instanceof AbstractMechanicalBlock otherMechBlock) {
                    connectedBlocks.put(getBlockState().getValue(BlockClutchBase.FACING).getOpposite(), otherMechBlock);
                }
            }
        }
        if (MechanicalBlock == myMechanicalBlockA) {
            BlockEntity otherBE = level.getBlockEntity(getBlockPos().relative(getBlockState().getValue(BlockClutchBase.FACING)));
            if (otherBE instanceof IMechanicalBlockProvider p) {
                AbstractMechanicalBlock other = p.getMechanicalBlock(getBlockState().getValue(BlockClutchBase.FACING).getOpposite());
                if (other instanceof AbstractMechanicalBlock otherMechBlock) {
                    connectedBlocks.put(getBlockState().getValue(BlockClutchBase.FACING), otherMechBlock);
                }
            }
        }

        return connectedBlocks;
    }

    public void readServer(CompoundTag tag, ServerPlayer p) {
        myMechanicalBlockA.mechanicalReadServer(tag, p);
        myMechanicalBlockB.mechanicalReadServer(tag, p);
    }

    public void readClient(CompoundTag tag) {
        myMechanicalBlockA.mechanicalReadClient(tag);
        myMechanicalBlockB.mechanicalReadClient(tag);
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        myMechanicalBlockA.mechanicalLoadAdditional(tag, registries);
        myMechanicalBlockB.mechanicalLoadAdditional(tag, registries);
        super.loadAdditional(tag, registries);
    }

    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        myMechanicalBlockA.mechanicalSaveAdditional(tag, registries);
        myMechanicalBlockB.mechanicalSaveAdditional(tag, registries);
        super.saveAdditional(tag, registries);
    }
}