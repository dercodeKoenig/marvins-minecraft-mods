package AgeOfSteam.Blocks.Mechanics.Gearbox;

import ARLib.network.INetworkTagReceiver;
import ARLib.utils.VertexBufferCleaner;
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

public class EntityGearboxBase extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    public VertexBuffer vertexBuffer_in;
    public VertexBuffer vertexBuffer_out;
    public VertexBuffer vertexBuffer_mid;
    public MeshData mesh_in;
    public MeshData mesh_out;
    public MeshData mesh_mid;
    public int lastLight = 0;


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
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            if (receivingFace == null) return 1;
            BlockState myState = getBlockState();

            if (myState.getBlock() instanceof BlockGearboxBase) {
                Direction facing = myState.getValue(BlockGearboxBase.FACING);

                if (receivingFace == facing.getOpposite())
                    return (double) -3 / 2;
                if (receivingFace == facing)
                    return (double) -2 / 3;
            }
            return 1;
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

    public EntityGearboxBase(BlockEntityType type, BlockPos pos, BlockState blockState) {
        super(type,pos,blockState);
        // because the input/output do not rotate with the same speed, reset only when they all made a full rotation
        // I think the gearbox should have a ratio of 2:3 for both sides to a total of 4:9 or 9:4
        // if we reset after 6 rotations, the high rpm part should have completed 9 rotations and the low rpm part should completed 4 rotations
        myMechanicalBlock.resetRotationAfterX = 360*6;

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer_in = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_out = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_mid = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                VertexBufferCleaner.register(this, vertexBuffer_in);
                VertexBufferCleaner.register(this, vertexBuffer_out);
                VertexBufferCleaner.register(this, vertexBuffer_mid);
            });
        }
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (side.getAxis() == myState.getValue(BlockGearboxBase.FACING).getAxis())
            return myMechanicalBlock;
        return null;
    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityGearboxBase) t).tick();
    }
}