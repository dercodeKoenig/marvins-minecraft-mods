package AOSWorkshopExpansion.Conveyor;

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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_BELT;

public class EntityConveyorBelt extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {


    public double myInertia = 1;
    public double myFriction = 1;
    public double maxStress = 600;

    public int lastLight;
    public MeshData mesh;
    public VertexBuffer vertexBuffer;

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
    };


    public EntityConveyorBelt(BlockPos pos, BlockState blockState) {
        super(ENTITY_CONVEYOR_BELT.get(), pos, blockState);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
            });
        }
        super.setRemoved();
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityConveyorBelt) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        myMechanicalBlock.mechanicalReadServer(compoundTag, serverPlayer);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
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

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof ConveyorBelt) {

            if (direction == Direction.DOWN) {
                // below check for engine block
                BlockState below = level.getBlockState(getBlockPos().below());
                if (below.getBlock() instanceof ConveyorEngine) {
                    if(below.getValue(ConveyorEngine.AXIS) != getBlockState().getValue(ConveyorBelt.AXIS))
                        return myMechanicalBlock;
                }
            }

            if (direction.getAxis() == getBlockState().getValue(ConveyorBelt.AXIS)) {
                // next to me check if it is a conveyor too and if it is in my correct direction
                BlockState neighbor = level.getBlockState(getBlockPos().relative(direction));
                if (neighbor.getBlock() instanceof ConveyorBelt) {
                    return myMechanicalBlock;
                }
            }
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }
}
