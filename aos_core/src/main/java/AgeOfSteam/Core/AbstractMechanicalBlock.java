package AgeOfSteam.Core;

import ARLib.network.PacketBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.*;

import static AgeOfSteam.Static.*;

// look at the Example class and the EntityFlyWheelBase to see how to use this
public abstract class AbstractMechanicalBlock {

    public int id; // in case you have multiple mechanical blocks in one blockentity (for example the clutch)
    public IMechanicalBlockProvider me;

    // your can read from this and use it to update your machines
    // I recommend you use internalVelocity, because the rotation will at some point "reset" to avoid numerical errors
    // maybe make a counter "energy" and add abs(velocity) every tick until target progress is reached
    // use rad_to_degree(velocity) / TPS to get speed in degree / tick
    public double currentRotation;
    public double internalVelocity;


    // for server client sync stuff
    public Map<UUID, Integer> clientsTrackingThisAsMaster = new HashMap<>();
    public int cttam_timeout = 100;
    public int lastPing = 999999;
    public double serverRotation;
    public double serverVelocity;
    public double last_internalVelocity;
    public double last_currentRotation;

    public double resetRotationAfterX = 360;
    // only one part of the network will be master during a tick, all the others will skip update
    public long updateTime;
    public Map<Direction, AbstractMechanicalBlock> connectedParts = new HashMap<>();

// for stress calculations
    public double stress = 0;
    public double lastConsumedForce = 0;
    public double lastConsumedForce_filled1 = 0; // for positive and negative force.
    public double lastConsumedForce_filled2 = 0; // is probably not 100% physically correct but i need to keep it simple and can not do full simulation for performance reasons
    public double lastAddedForce = 0;
    public Deque<nodeInfo> forceDistributionDeq = new ArrayDeque<>();
    public boolean lastTickHadForceToDistribute = false;


    public AbstractMechanicalBlock(int id, IMechanicalBlockProvider me) {
        this.id = id;
        this.me = me;
    }

    public abstract double getMaxStress();

    public abstract double getInertia(Direction face);

    public abstract double getTorqueResistance(Direction face);

    public abstract double getTorqueProduced(Direction face);

    // to convert received rotation into an internal representation of rotation/velocity
    public abstract double getRotationMultiplierToInside(@Nullable Direction receivingFace);


    public double getRotationMultiplierToOutside(@Nullable Direction outputFace) {
        return 1 / getRotationMultiplierToInside(outputFace);
    }


     // called at the start of the tick update
    // notifies other parts in the network that the initial tick is done
    public void propagateTickBeforeUpdate() {
        long gameTime = me.getBlockEntity().getLevel().getGameTime();
        if (updateTime != gameTime) {
            updateTime = gameTime;
            connectedParts = me.getConnectedParts(me, this);

            for (AbstractMechanicalBlock i : connectedParts.values()) {
                i.propagateTickBeforeUpdate();
            }
        }
    }


// scans recursively through the network and collects mass, and forces and transforms them to match the current master
    public void getPropagatedData(MechanicalFlowData data, @org.jetbrains.annotations.Nullable Direction requestedFrom, HashSet<AbstractMechanicalBlock> workedPositions) {

        BlockEntity myTile = me.getBlockEntity();
        if (!workedPositions.contains(this)) {
            workedPositions.add(this);

            MechanicalFlowData myInputFlowData = new MechanicalFlowData();
            // update the connected parts
            for (Direction i : connectedParts.keySet()) {
                MechanicalFlowData d = new MechanicalFlowData();
                AbstractMechanicalBlock b = connectedParts.get(i);
                b.getPropagatedData(d, i.getOpposite(), workedPositions);

                double rotationMultiplierToInside = getRotationMultiplierToInside(i);

                myInputFlowData.combinedTransformedForce += d.combinedTransformedForce / rotationMultiplierToInside;
                myInputFlowData.combinedTransformedInertia += Math.abs(d.combinedTransformedInertia / (rotationMultiplierToInside));
                myInputFlowData.combinedTransformedMomentum += d.combinedTransformedMomentum * Math.signum(rotationMultiplierToInside);
                myInputFlowData.combinedTransformedResistanceForce += Math.abs(d.combinedTransformedResistanceForce / rotationMultiplierToInside);
            }

            double rotationMultiplierToOutside = getRotationMultiplierToOutside(requestedFrom);

            myInputFlowData.combinedTransformedForce += getTorqueProduced(requestedFrom);
            myInputFlowData.combinedTransformedInertia += getInertia(requestedFrom);
            myInputFlowData.combinedTransformedMomentum += internalVelocity * getInertia(requestedFrom);
            myInputFlowData.combinedTransformedResistanceForce += getTorqueResistance(requestedFrom);


            data.combinedTransformedForce += myInputFlowData.combinedTransformedForce / rotationMultiplierToOutside;
            data.combinedTransformedResistanceForce += Math.abs(myInputFlowData.combinedTransformedResistanceForce / rotationMultiplierToOutside);
            data.combinedTransformedInertia += Math.abs(myInputFlowData.combinedTransformedInertia / rotationMultiplierToOutside);
            data.combinedTransformedMomentum += myInputFlowData.combinedTransformedMomentum * Math.signum(rotationMultiplierToOutside);

        }
    }

    public void applyRotations() {
        currentRotation += rad_to_degree(internalVelocity) / TPS;
        if (currentRotation > resetRotationAfterX ) currentRotation -= resetRotationAfterX ;
        if (currentRotation < -resetRotationAfterX) currentRotation += resetRotationAfterX ;
    }

// will recursively apply the velocity update to the entire network
    public void propagateVelocityUpdate(double velocity, @org.jetbrains.annotations.Nullable Direction receivingFace, HashSet<AbstractMechanicalBlock> workedPositions, boolean ignorePreviousUpdates, boolean resetStress) {
        BlockEntity myTile = me.getBlockEntity();
        Level level = myTile.getLevel();
        if (!ignorePreviousUpdates && !level.isClientSide && workedPositions.contains(this) && Math.abs(velocity * getRotationMultiplierToInside(receivingFace) - internalVelocity) > 0.00001) {
            // break this block because something is wrong with the network
            System.out.println("breaking the network because something is wrong: this tile received a different velocity update in the same tick:" + myTile.getBlockPos() + ", id: " + id);
            System.out.println("current reveiced rotation from face " + receivingFace + ":" + velocity * getRotationMultiplierToInside(receivingFace) + ". Last received velocity: " + internalVelocity);

            level.destroyBlock(myTile.getBlockPos(), true);
            return;
        }

        if (!workedPositions.contains(this)) {
            workedPositions.add(this);

            double lastVelocity = internalVelocity;

            double currentProducedForceBeforeVelocityChange = getTorqueProduced(receivingFace);
            double currentResistanceBeforeVelocityChange = getTorqueResistance(receivingFace);
            double currentMassBeforeVelocityChange = getInertia(receivingFace);

            internalVelocity = velocity;
            if (receivingFace != null) {
                internalVelocity *= getRotationMultiplierToInside(receivingFace);
            }

            // forward the transformed rotation to the other blocks
            for (Direction i : connectedParts.keySet()) {
                AbstractMechanicalBlock b = connectedParts.get(i);
                double outputVelocity = internalVelocity * getRotationMultiplierToOutside(i);
                b.propagateVelocityUpdate(outputVelocity, i.getOpposite(), workedPositions, ignorePreviousUpdates, resetStress);
            }


            // all for stress calculation later
            if (resetStress && !me.getBlockEntity().getLevel().isClientSide()) {
                forceDistributionDeq.clear();
                stress = 0;
                lastConsumedForce_filled1 = 0;
                lastConsumedForce_filled2 = 0;
                double acceleration = (internalVelocity - lastVelocity) * TPS;
                double requiredForce1 = currentMassBeforeVelocityChange * acceleration * Math.signum(lastVelocity);
                double requiredForce2 = currentResistanceBeforeVelocityChange + requiredForce1;
                double absResistance = Math.max(requiredForce2, 0);
                double inducedForce = Math.max(-requiredForce2, 0) * Math.signum(lastVelocity);

                double totalForceWorking = inducedForce + currentProducedForceBeforeVelocityChange;
                if (Math.abs(totalForceWorking) >= absResistance) {
                    totalForceWorking -= absResistance * Math.signum(totalForceWorking);
                    absResistance = 0;
                }
                if (Math.abs(totalForceWorking) < 0.01)
                    //ignore
                    totalForceWorking = 0;

                lastConsumedForce = absResistance;
                lastAddedForce = totalForceWorking;
                //System.out.println(me.getBlockEntity().getBlockState().getBlock() + ":" + me.getBlockEntity().getBlockPos() + ":" + lastAddedForce + ":" + lastConsumedForce);
            }
        }
    }

    // used to recursively set the current rotation in the network
    public void propagateResetRotation(double rotation, Direction receivingFace, HashSet<AbstractMechanicalBlock> workedPositions) {
        if (!workedPositions.contains(this)) {
            workedPositions.add(this);
            Map<Direction, AbstractMechanicalBlock> connections = me.getConnectedParts(me, this);

            if (receivingFace != null)
                rotation *= getRotationMultiplierToInside(receivingFace);

            currentRotation = rotation;

            for (Direction i : connections.keySet()) {
                double rotationToOutside = currentRotation * getRotationMultiplierToOutside(i);
                connections.get(i).propagateResetRotation(rotationToOutside, i.getOpposite(), workedPositions);
            }
        }
    }


    // will reset the rotation for all parts so that they render correctly connected to each other
    // will also compute the momentum and calculate new velocity
    public void mechanicalOnload() {

        propagateResetRotation(currentRotation, null, new HashSet<>());

        if (!me.getBlockEntity().getLevel().isClientSide()) {
            MechanicalFlowData data = new MechanicalFlowData();
            connectedParts = me.getConnectedParts(me, this);
            HashSet<AbstractMechanicalBlock> worked = new HashSet<>();
            getPropagatedData(data, null, worked);
            worked.clear();

            double target_velocity = 0;
            if (data.combinedTransformedInertia != 0) {
                target_velocity = data.combinedTransformedMomentum / data.combinedTransformedInertia;
            }
            propagateVelocityUpdate(target_velocity, null, worked, true, false);
        }
    }

    // all the following is for stress calculations.
    // I do not claim to fully understand it all, sometimes i just changed around some +/- until it worked

    // After reading it a year later, it appears to work like this:
    // When velocity changes, we calculate the force required for the acceleration.
    // For example a flywheel does not produce force directly, but when you slow it down by connecting other parts,
    // it will slow down more than it would without connected parts, and it will create an opposite force.
    // In other words, a sawmill has high resistance. If it is connected to a flywheel,
    // the flywheel provides force to spin the sawmill even if it does not directly produce force.
    // This force will propagate to all connected parts until it is fully exhausted, and once exhausted the stress on every part is calculated.
    // picture this: axle3 <- motor -> axle1 -> axle2 -> sawmill
    // motor makes 10 force and consumes 1 as friction and forwards 9 force to axle1 and axle3
    // axle1 gets 9 force, consumes 1 as friction and forwards 8 to the next axle. it adds 1 stress to all previous parts until it gets to motor
    // axle3 gets 9 force, consumes 1 as friction and has no more parts to forward. it adds 1 stress to motor.
    // axle2 gets 8 force, consumes 1 as friction and forwards 7 to sawmill. it adds 1 stress to axle1 and 1 stress to motor
    // sawmill gets 7 force, consumes 5 as friction and has no part to forward to. it adds 5 stress to axle2, axle1 and motor.
    // since total force (10) is higher than resistance(1+1+1+5=8) the network will probably accelerate next tick
    // axle 1 now has a stress of 1 from axle2 + 5 from sawmill = 6
    // motor has a stress of 1+1+5 from axle1,2 and sawmill and 1 from axle3, so stress = 8
    // since the motor is the only origin of force, it has to cover all resistance so a stress of 8 for a total resistance of 8 sounds correct!

    public class nodeInfo {
        public Direction nextInputFace;
        public AbstractMechanicalBlock nextTarget;
        public forceDistributionNode node;
    }

    public class MechanicalBlockWithForceTransformation {
        public AbstractMechanicalBlock block;
        public double forceTransformation;

        public MechanicalBlockWithForceTransformation(AbstractMechanicalBlock block, double forceTransformation) {
            this.block = block;
            this.forceTransformation = forceTransformation;
        }
    }

    public void collectConnectedParts(Direction receivingFace, Set<AbstractMechanicalBlock> parts) {
        if (!parts.contains(this)) {
            parts.add(this);
            for (Direction i : connectedParts.keySet()) {
                if (receivingFace != null && receivingFace == i) continue;
                AbstractMechanicalBlock b = connectedParts.get(i);
                b.collectConnectedParts(i.getOpposite(), parts);
            }
        }
    }

    public class forceDistributionNode {
        public AbstractMechanicalBlock daddy;
        public Set<AbstractMechanicalBlock> path = new HashSet<>();
        public List<MechanicalBlockWithForceTransformation> pathWithForceTransformations = new ArrayList<>();
        public double lastOutputForceMultiplier = 1;
        public double currentEffectiveForceMultiplier = 1;

        public forceDistributionNode(AbstractMechanicalBlock me) {
            daddy = me;
        }

        public forceDistributionNode copy() {
            forceDistributionNode n = new forceDistributionNode(daddy);
            n.path.addAll(path);
            n.lastOutputForceMultiplier = lastOutputForceMultiplier;
            n.currentEffectiveForceMultiplier = currentEffectiveForceMultiplier;
            n.pathWithForceTransformations.addAll(pathWithForceTransformations);
            return n;
        }
    }

    void addStressBackwards(forceDistributionNode n, double stress) {
        for (int i = n.pathWithForceTransformations.size() - 1; i >= 0; i--) {
            n.pathWithForceTransformations.get(i).block.stress += Math.abs(stress);
            stress /= Math.abs(n.pathWithForceTransformations.get(i).forceTransformation);
            if (n.pathWithForceTransformations.get(i).block.stress > n.pathWithForceTransformations.get(i).block.getMaxStress()) {
                n.pathWithForceTransformations.get(i).block.me.getBlockEntity().getLevel().destroyBlock(n.pathWithForceTransformations.get(i).block.me.getBlockEntity().getBlockPos(), true);
                System.out.println("overstressed part: "+n.pathWithForceTransformations.get(i).block.me.getBlockEntity().getBlockPos()+" - "+n.pathWithForceTransformations.get(i).block.stress);
            }
        }
    }

    public void walkDistributeForce(Direction receivingFace, forceDistributionNode n) {
        if (!n.path.contains(this)) {
            forceDistributionNode myNode = n.copy();
            myNode.path.add(this);
            double forceMultiplierForNode = 1 * myNode.lastOutputForceMultiplier;
            if (receivingFace != null) {
                forceMultiplierForNode *= 1 / getRotationMultiplierToInside(receivingFace);
            }
            myNode.pathWithForceTransformations.add(new MechanicalBlockWithForceTransformation(this, forceMultiplierForNode));

            myNode.currentEffectiveForceMultiplier *= forceMultiplierForNode;

            double currentEffectiveForce = myNode.currentEffectiveForceMultiplier * myNode.daddy.lastAddedForce;
//System.out.println( lastConsumedForce + ":"+me.getBlockEntity().getBlockPos()+":"+currentEffectiveForce);

            if (currentEffectiveForce > 0) {
                double toSubtract = lastConsumedForce - lastConsumedForce_filled1;
                toSubtract = Math.min(toSubtract, currentEffectiveForce);
                myNode.daddy.lastAddedForce -= toSubtract / myNode.currentEffectiveForceMultiplier;
                lastConsumedForce_filled1 += toSubtract;
                addStressBackwards(myNode, toSubtract);

            }
            if (currentEffectiveForce < 0) {
                double toSubtract = lastConsumedForce - lastConsumedForce_filled2;
                toSubtract = Math.min(toSubtract, -currentEffectiveForce);
                myNode.daddy.lastAddedForce += toSubtract / myNode.currentEffectiveForceMultiplier;
                lastConsumedForce_filled2 += toSubtract;
                addStressBackwards(myNode, toSubtract);
            }
            if (lastAddedForce != 0) {
                if (Math.signum(lastAddedForce) != Math.signum(currentEffectiveForce)) {
                    double toSubtract;
                    toSubtract = Math.min(Math.abs(lastAddedForce), Math.abs(currentEffectiveForce));
                    myNode.daddy.lastAddedForce -= toSubtract * Math.signum(currentEffectiveForce) / myNode.currentEffectiveForceMultiplier;
                    addStressBackwards(myNode, toSubtract);
                }
            }
            if (Math.abs(currentEffectiveForce) > 0.01) {
                for (Direction i : connectedParts.keySet()) {
                    if (i == receivingFace) continue;
                    forceDistributionNode newNode = myNode.copy();
                    newNode.lastOutputForceMultiplier = 1 / getRotationMultiplierToOutside(i);
                    nodeInfo info = new nodeInfo();
                    info.nextTarget = connectedParts.get(i);
                    info.node = newNode;
                    info.nextInputFace = i.getOpposite();
                    forceDistributionDeq.addLast(info);
                }
            } else {
                myNode.daddy.lastAddedForce = 0;
            }
        }
    }

    // end of stress calculation stuff


    // this should be called in the tick method of the blockentity.
    // I recommend to call it at the start directly so it will make sure that the entire network
    // will perform its tick on the same state of the network.
    public void mechanicalTick() {

        BlockEntity myTile = me.getBlockEntity();
        long gameTime = me.getBlockEntity().getLevel().getGameTime();
        if (myTile.getLevel().isClientSide()) {
            if (gameTime != updateTime) {
                propagateTickBeforeUpdate();

                double rotationDiff1 = serverRotation - currentRotation;
                double rotationDiff2 = serverRotation+resetRotationAfterX - currentRotation;
                double rotationDiff3 = serverRotation-resetRotationAfterX - currentRotation;
                double rotationDiff =rotationDiff1;
                if(Math.abs(rotationDiff2) < Math.abs(rotationDiff))
                    rotationDiff = rotationDiff2;
                if(Math.abs(rotationDiff3) < Math.abs(rotationDiff))
                    rotationDiff = rotationDiff3;



                internalVelocity = serverVelocity;
                    internalVelocity += rotationDiff * 0.01;

                propagateVelocityUpdate(internalVelocity, null, new HashSet<>(), false, false);

                if (lastPing > cttam_timeout / 2) {
                    lastPing = 0;
                    CompoundTag tag = new CompoundTag();
                    tag.put("masterPing", new CompoundTag());
                    tag.putInt("id", id);
                    PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(myTile, tag));
                }
            }
        }

        if (!myTile.getLevel().isClientSide()) {
            if (updateTime != gameTime) {
                propagateTickBeforeUpdate();

                HashSet<AbstractMechanicalBlock> workedPositions = new HashSet<>();
                MechanicalFlowData data = new MechanicalFlowData();
                getPropagatedData(data, null, workedPositions);
                workedPositions.clear();

                double t = (double) 1 / TPS;

                data.combinedTransformedInertia = Math.max(data.combinedTransformedInertia, 0.01);
                //System.out.println(data.combinedTransformedMass+":"+data.combinedTransformedForce+":"+data.combinedTransformedResistanceForce+":"+internalVelocity);
                double newVelocity = internalVelocity;
                newVelocity += data.combinedTransformedForce / data.combinedTransformedInertia * t;
                float signBefore = (float) Math.signum(newVelocity);
                newVelocity -= data.combinedTransformedResistanceForce * Math.signum(newVelocity) / data.combinedTransformedInertia * t;
                float signAfter = (float) Math.signum(newVelocity);
                if (Math.abs(newVelocity) < 0.0001) newVelocity = 0;

                if ((signAfter < 0 && signBefore > 0) || (signAfter > 0 && signBefore < 0))
                    newVelocity = 0;
                if (newVelocity > internalVelocity + 90)
                    newVelocity = internalVelocity + 90;
                if (newVelocity < internalVelocity - 90)
                    newVelocity = internalVelocity - 90;

                //System.out.println(t+":"+newVelocity + ":" + myTile.getBlockPos() + ":" + data.combinedTransformedForce + ":" + data.combinedTransformedMass + ":" + data.combinedTransformedResistanceForce);

                boolean resetStress = (me.getBlockEntity().getLevel().random.nextInt(CALC_STRESS_EVERY_X_TICKS) == 0) && !lastTickHadForceToDistribute;

                propagateVelocityUpdate(newVelocity, null, workedPositions, false, resetStress);

                if (resetStress) {
                    lastTickHadForceToDistribute = true;
                    Set<AbstractMechanicalBlock> connectedBlocks = new HashSet<>();
                    collectConnectedParts(null, connectedBlocks);
                    for (AbstractMechanicalBlock i : connectedBlocks) {
                        if (i.lastAddedForce != 0) {
                            forceDistributionNode n = new forceDistributionNode(i);
                            nodeInfo info = new nodeInfo();
                            info.nextInputFace = null;
                            info.nextTarget = i;
                            info.node = n;
                            i.forceDistributionDeq.addLast(info);
                        }
                    }
                }
                if (lastTickHadForceToDistribute) {
                    lastTickHadForceToDistribute = false;
                    Set<AbstractMechanicalBlock> connectedBlocks = new HashSet<>();
                    collectConnectedParts(null, connectedBlocks);
                    for (AbstractMechanicalBlock i : connectedBlocks) {
                        if (!i.forceDistributionDeq.isEmpty()) {
                            nodeInfo info = i.forceDistributionDeq.removeFirst();
                            info.nextTarget.walkDistributeForce(info.nextInputFace, info.node);
                            lastTickHadForceToDistribute = true;
                        }
                    }
                }
            }
        }
        applyRotations();

        if(me.getBlockEntity(). getLevel().isClientSide) {
            serverRotation += rad_to_degree(serverVelocity) / TPS ;
            if (serverRotation > resetRotationAfterX ) serverRotation -= resetRotationAfterX ;
            if (serverRotation < -resetRotationAfterX) serverRotation += resetRotationAfterX ;

            if( lastPing < cttam_timeout)
                lastPing++;
        }

        if (!myTile.getLevel().isClientSide()) {
            for (UUID i : clientsTrackingThisAsMaster.keySet()) {
                // increment timeout counter
                clientsTrackingThisAsMaster.put(i, clientsTrackingThisAsMaster.get(i) + 1);
                if (clientsTrackingThisAsMaster.get(i) > cttam_timeout) {
                    clientsTrackingThisAsMaster.remove(i);
                    break; // break to prevent concurrent modification bs
                }
            }
            if (last_currentRotation != currentRotation) {
                last_currentRotation = currentRotation;
                me.getBlockEntity().setChanged();
            }
            if (last_internalVelocity != internalVelocity) {
                last_internalVelocity = internalVelocity;
                me.getBlockEntity().setChanged();
                CompoundTag updateTag = new CompoundTag();
                updateTag.putDouble("velocity", internalVelocity);
                updateTag.putDouble("rotation", currentRotation);
                updateTag.putInt("id", id);
                for (UUID i : clientsTrackingThisAsMaster.keySet()) {
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(i);
                    if (player != null) {
                        PacketDistributor.sendToPlayer(player, PacketBlockEntity.getBlockEntityPacket(myTile, updateTag));
                    }
                }
            }
            if (Math.abs(internalVelocity) > 100000 || Double.isNaN(internalVelocity)) {
                System.out.println("set block to air because velocity is way too high!  " + me.getBlockEntity().getBlockPos()+":"+internalVelocity);
                me.getBlockEntity().getLevel().destroyBlock(me.getBlockEntity().getBlockPos(), true);
            }
        }
    }

    public void mechanicalReadServer(CompoundTag tag, ServerPlayer p) {
        if (tag.contains("masterPing") && tag.contains("id")) {
            int id = tag.getInt("id");
            if (id == this.id) {
                clientsTrackingThisAsMaster.put(p.getUUID(), 0);
                CompoundTag updateTag = new CompoundTag();
                updateTag.putDouble("velocity", internalVelocity);
                updateTag.putDouble("rotation", currentRotation);
                updateTag.putInt("id", id);
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(me.getBlockEntity(), updateTag));
            }
        }
    }

    public void mechanicalReadClient(CompoundTag tag) {
        if (tag.contains("velocity") && tag.contains("id"))
            if (tag.getInt("id") == this.id)
                serverVelocity = tag.getDouble("velocity");
        if (tag.contains("rotation") && tag.contains("id"))
            if (tag.getInt("id") == this.id)
                serverRotation = tag.getDouble("rotation");
    }


    public void mechanicalLoadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag myTag = tag.getCompound("MechanicalBlock_" + this.id);
        internalVelocity = myTag.getDouble("internalVelocity");
        currentRotation = myTag.getDouble("currentRotation");
    }


    public void mechanicalSaveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag myTag = new CompoundTag();
        myTag.putDouble("internalVelocity", internalVelocity);
        myTag.putDouble("currentRotation", currentRotation);

        tag.put("MechanicalBlock_" + this.id, myTag);
    }
}


