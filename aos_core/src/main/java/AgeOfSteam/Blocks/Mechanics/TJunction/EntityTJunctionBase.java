package AgeOfSteam.Blocks.Mechanics.TJunction;

import ARLib.network.INetworkTagReceiver;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;

public abstract class EntityTJunctionBase extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    public VertexBuffer vertexBuffer;
    public MeshData mesh;
    public     VertexBuffer vertexBuffer2;
    public MeshData mesh2;
    public int lastLight;

    double myInertia;
    double myFriction;
    double maxStress;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return maxStress;
        }

        @Override
        public double getInertia(Direction face) {
            return myInertia;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return myFriction;
        }

        @Override
        public double getTorqueProduced(Direction face) {
            return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@Nullable Direction receivingFace) {
            if (receivingFace == null) return 1;
            BlockState myState = getBlockState();

            if (myState.getBlock() instanceof BlockTJunctionBase) {
                Direction.Axis myAxis = myState.getValue(BlockTJunctionBase.AXIS);
                if(myAxis == receivingFace.getAxis()){
                    return 1;
                }
            }

            double inversionMultiplier =myState.getValue(BlockTJunctionBase.INVERTED) ? -1:1;
            if(receivingFace == myState.getValue(BlockTJunctionBase.FACING)){
                if(receivingFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
                    return -1*inversionMultiplier;
                else
                    return 1*inversionMultiplier;
            }
            return 1;
        }

        double getRotationOffsetForFace(Direction face){

            if(face == null)return 0;

            BlockState myState = getBlockState();
            if(myState.getValue(BlockTJunctionBase.FACING) == face) {
                    return 14.7f;
            }

            return 0;
        }

        @Override
        public void propagateResetRotation(double rotation,Direction receivingFace, HashSet<AbstractMechanicalBlock> workedPositions) {
            if (!workedPositions.contains(this)) {
                workedPositions.add(this);
                Map<Direction, AbstractMechanicalBlock> connections = me.getConnectedParts(me, this);

                if (receivingFace != null) {
                    rotation +=getRotationOffsetForFace(receivingFace) * getRotationMultiplierToInside(receivingFace);
                    rotation *= getRotationMultiplierToInside(receivingFace);
                }

                currentRotation = rotation;

                for (Direction i : connections.keySet()) {
                    double rotationToOutside = (-getRotationOffsetForFace(i)+currentRotation) * getRotationMultiplierToOutside(i);
                    connections.get(i).propagateResetRotation(rotationToOutside, i.getOpposite(), workedPositions);
                }
            }
        }
    };

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
    }


    public void tick() {
        myMechanicalBlock.mechanicalTick();
    }


    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(tag,p);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }


    public EntityTJunctionBase(BlockEntityType type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
                vertexBuffer2.close();
            });
        }
    }


    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (side.getAxis() == myState.getValue(BlockTJunctionBase.AXIS) || side == myState.getValue(BlockTJunctionBase.FACING))
            return myMechanicalBlock;
        return null;
    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityTJunctionBase) t).tick();
    }
}