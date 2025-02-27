package AgeOfSteam.Blocks.Mechanics.FlyWheel;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static AgeOfSteam.Blocks.Mechanics.FlyWheel.BlockFlyWheelBase.ROTATION_AXIS;

public class EntityFlyWheelBase extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    public VertexBuffer vertexBuffer;
    public MeshData mesh;
    public int lastLight = 0;


    public double myInertia;
    public double myFriction;
    public double maxStress;

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
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }

        @Override
        public void propagateResetRotation(double rotation, Direction receivingFace, HashSet<AbstractMechanicalBlock> workedPositions) {
            if (!workedPositions.contains(this)) {
                workedPositions.add(this);
                Map<Direction, AbstractMechanicalBlock> connections = me.getConnectedParts(me, this);

                currentRotation = rotation;

                for (Direction i : connections.keySet()) {
                    double rotationToOutside = currentRotation  * getRotationMultiplierToOutside(i);
                    connections.get(i).propagateResetRotation(rotationToOutside, i.getOpposite(), workedPositions);
                }
            }
        }
    };

    public EntityFlyWheelBase(BlockEntityType t, BlockPos pos, BlockState blockState) {
        super(t, pos, blockState);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
            });
        }
    }


    public void tick(){
        myMechanicalBlock.mechanicalTick();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityFlyWheelBase) t).tick();
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockFlyWheelBase) {
            Direction.Axis blockAxis = myState.getValue(ROTATION_AXIS);
            if (side.getAxis() == blockAxis) {
                return myMechanicalBlock;
            }
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity(){return this;}


    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
        if (level.isClientSide) {
            CompoundTag i = new CompoundTag();
            i.putUUID("request_rotationOffset", Minecraft.getInstance().player.getUUID());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, i));
        }
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
}