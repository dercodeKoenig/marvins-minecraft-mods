package AgeOfSteam.Blocks.Mechanics.HandGenerator;

import ARLib.network.INetworkTagReceiver;
import ARLib.utils.VertexBufferCleaner;
import AgeOfSteam.Config.Config;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import static AgeOfSteam.Registry.ENTITY_HAND_GENERATOR;
import static AgeOfSteam.Static.WOODEN_SOUNDS;

public class EntityHandGenerator extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {

    public VertexBuffer vertexBuffer;
    public MeshData mesh;
    public     VertexBuffer vertexBuffer2;
    public MeshData mesh2;
    public int lastLight;

    public double myForce = 0;

    public  double maxForce = Config.INSTANCE.hand_generator_max_force;
    public  double maxSpeed = Config.INSTANCE.hand_generator_max_speed;
    double myFriction = Config.INSTANCE.hand_generator_friction;
    double myInertia = Config.INSTANCE.hand_generator_inertia;
    double maxStress = Config.INSTANCE.hand_generator_max_stress;

    int ticksRemainingForForce = 0;

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
            double actualForce = myForce * Math.max(0, (1 - Math.abs(internalVelocity) / maxSpeed));
            return actualForce;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
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

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockHandGenerator) {
            if (side == myState.getValue(BlockHandGenerator.FACING))
                return myMechanicalBlock;
        }
        return null;
    }

    public EntityHandGenerator(BlockPos pos, BlockState blockState) {
        super(ENTITY_HAND_GENERATOR.get(), pos, blockState);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                VertexBufferCleaner.register(this, vertexBuffer);
                VertexBufferCleaner.register(this, vertexBuffer2);
            });
        }
    }


    public boolean onPlayerClicked(boolean isShift) {
        if (!isShift) {
            if (ticksRemainingForForce < 5) {
                ticksRemainingForForce += 5;
                return true;
            }
        } else {
            if (ticksRemainingForForce > -5) {
                ticksRemainingForForce -= 5;
                return true;
            }
        }
        return false;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (ticksRemainingForForce > 0) {
            ticksRemainingForForce--;
            myForce = maxForce;
        } else if (ticksRemainingForForce < 0) {
            ticksRemainingForForce++;
            myForce = -maxForce;
        } else {
            myForce = 0;
        }

        if(level.random.nextFloat() < 0.01*Math.abs(myMechanicalBlock.internalVelocity)) {
            int randomIndex = level.random.nextInt(WOODEN_SOUNDS.length);
            SoundEvent randomEvent = WOODEN_SOUNDS[randomIndex];
            level.playSound(null, getBlockPos(), randomEvent,
                    SoundSource.BLOCKS, 0.002f * (float) Math.abs(myMechanicalBlock.internalVelocity), 1.0f);  //
        }
    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityHandGenerator) t).tick();
    }
}
