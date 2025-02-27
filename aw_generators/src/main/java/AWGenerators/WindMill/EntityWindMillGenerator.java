package AWGenerators.WindMill;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import AWGenerators.Config.Config;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static AWGenerators.Registry.ENTITY_WINDMILL_GENERATOR;


public class EntityWindMillGenerator extends BlockEntity implements INetworkTagReceiver, IMechanicalBlockProvider {


    public  double forcePerBlock = Config.INSTANCE.windmill_forcePerBlock;
    public  double windSpeedMultiplier = Config.INSTANCE.windmill_windSpeedMultiplier;
    public double frictionPerBlock = Config.INSTANCE.windmill_frictionPerBlock;
    public double inertiaPerBlock = Config.INSTANCE.windmill_inertiaPerBlock;
    public int max_size = Config.INSTANCE.windmill_maxSize;

    // todo: make a small offset for wind and include rotation multiplier based on facing
    public static PerlinSimplexNoise noise          = new PerlinSimplexNoise(new SingleThreadedRandomSource(8082003),List.of(-2, -1, 0, 1, 2));;
    public static PerlinSimplexNoise noiseDirection          = new PerlinSimplexNoise(new SingleThreadedRandomSource(4092003),List.of(-2, -1, 0, 1, 2));;
    PerlinSimplexNoise currentRandomNoiseOffset = new PerlinSimplexNoise(new SingleThreadedRandomSource(new Random().nextInt()),List.of(-2, -1, 0, 1, 2));;;;

    // usually, changes in wind are slowly with noise. but on server start or entity load,
    // it will just start at a random starting value and this can cause a huge sudden change in force.
    // this change in force will overstress the network and cause it to break.
    // this is why we have to slowly increase force so that it does not have this big spikes in force
    double forceSteps = 0.0005; // should be 1 after 100 seconds
    double currentForceMultiplier = 0;

    VertexBuffer vertexBuffer;
    MeshData mesh;

    int size;
    int last_size_for_meshUpdate;

    double myFriction = 1;
    double myInertia = 10;
    double maxStress = Config.INSTANCE.windmill_maxStress;
    double myForce = 0;

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
            return myForce;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };

    public EntityWindMillGenerator(BlockPos pos, BlockState blockState) {
        super(ENTITY_WINDMILL_GENERATOR.get(), pos, blockState);
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
        if (level.isClientSide) {
            CompoundTag request = new CompoundTag();
            request.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, request));
        }
        if(!level.isClientSide){
            scanStructure();
        }
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
            });
        }
        if(!level.isClientSide){
            Direction myFacing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos center = getBlockPos().relative(myFacing.getOpposite());

            int zMultiplier = myFacing.getAxis() == Direction.Axis.X ? 1 : 0;
            int xMultiplier = myFacing.getAxis() == Direction.Axis.Z ? 1 : 0;
            resetInvalidBlocks(center,new HashSet<BlockPos>(),xMultiplier,zMultiplier);
        }
        super.setRemoved();
    }

    boolean isBlockValidAt(BlockPos p) {
        // this should load the chunk if it is not loaded
        ChunkAccess chunk = level.getChunk(p);
        level.getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, true);


        BlockState state = level.getBlockState(p);
        if (state.getBlock() instanceof BlockWindMillBlade b) {
            if (!state.getValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED)) {
                return true;
            } else {
                BlockPos masterPos = BlockWindMillBlade.getMasterPos(new BlockIdentifier(level, p));
                if (masterPos == null || masterPos.equals(getBlockPos())) {
                    return true;
                }
            }
        }

        return false;
    }

    void resetInvalidBlocks(BlockPos center, Set<BlockPos> validBlocks, int xMultiplier, int zMultiplier){
        // now reset all blocks that could have been modified by this generator, except for the valid blocks
        // if the block has a different master, do not reset it, it belongs to another controller
        for (int x = -max_size; x <= max_size; x++) {
            for (int y = -max_size; y <= max_size; y++) {
                BlockPos targetBlock = center.offset(x * xMultiplier, y, x * zMultiplier);
                if(!validBlocks.contains(targetBlock) ) {

                    // this should load the chunk if it is not loaded
                    ChunkAccess chunk = level.getChunk(targetBlock);
                    level.getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, true);

                    BlockState state = level.getBlockState(targetBlock);
                    if (state.getBlock() instanceof BlockWindMillBlade) {
                        if(state.getValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED)) {
                            BlockPos masterPos = BlockWindMillBlade.getMasterPos(new BlockIdentifier(level, targetBlock));

                            if(masterPos==null){
                                // this should never print if i did all correct
                                System.out.println("error: master pos should not be null!");
                            }

                            if (masterPos==null || masterPos.equals(getBlockPos())) {
                                // this should call onRemove for the block and remove it from the master block map
                                level.setBlock(targetBlock, state.setValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED, false), 3);
                            }
                        }
                    }
                }
            }
        }
    }

    boolean isScanning = false;
    public void scanStructure() {
        if (level.isClientSide) return;
        // I use setBlock to update the state of the blade and the blade will call scanStructure when it was removed.
        // So I make sure it can not re-scan while it is already scanning
        if (isScanning) return;
        isScanning = true;

        Direction myFacing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos center = getBlockPos().relative(myFacing.getOpposite());

        int zMultiplier = myFacing.getAxis() == Direction.Axis.X ? 1 : 0;
        int xMultiplier = myFacing.getAxis() == Direction.Axis.Z ? 1 : 0;


        boolean doScan = true;
        int maxValidSize = 0;
        int s = 0;
        Set<BlockPos> validBlocks = new HashSet<>();
        while (doScan) {
            A:{
                List<BlockPos> validBlocks_tmp = new ArrayList<>();
                for (int x = -s; x <= s; x++) {
                    for (int y = -1; y <= 1; y++) {
                        BlockPos targetBlock = center.offset(x * xMultiplier, y, x * zMultiplier);
                        if (!isBlockValidAt(targetBlock)) {
                            doScan = false;
                            break A;
                        } else {
                            validBlocks_tmp.add(targetBlock);
                        }
                    }
                }
            for (int y = -s; y <= s; y++) {
                for (int x = -1; x <= 1; x++) {
                    BlockPos targetBlock = center.offset(x * xMultiplier, y, x * zMultiplier);
                    if (!isBlockValidAt(targetBlock)) {
                        doScan = false;
                        break A;
                    } else {
                        validBlocks_tmp.add(targetBlock);
                    }
                }
            }
                maxValidSize = s;
                validBlocks.addAll(validBlocks_tmp);
                s++;

                if(maxValidSize == max_size){
                    doScan = false;
                }
            }
        }
        this.size = maxValidSize - 1; // size 0 is the axle, size 1 is the axle connection, blades starting at size 2

        if (size > 0) {
            // the structure is valid, update the client with the new size and set state to true
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
            level.setBlock(getBlockPos(), getBlockState().setValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED, true), 3);

            // now for all valid blocks set the state to true and set this as master pos
            for (BlockPos i : validBlocks) {
                if (level.getBlockState(i).getBlock() instanceof BlockWindMillBlade b) {
                    // first change blockstate and after this set master because onRemove will clear master pos again
                    level.setBlock(i, level.getBlockState(i).setValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED, true), 3);
                    BlockWindMillBlade.setMaster(new BlockIdentifier(level, i), getBlockPos());
                }
            }
        }else {
            // structure is not valid, set state t0 false
            level.setBlock(getBlockPos(), getBlockState().setValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED, false), 3);
            // also clear all valid blocks because it can still have some from s0 and s1
            validBlocks.clear();
        }

        resetInvalidBlocks(center,validBlocks,xMultiplier,zMultiplier);

        currentForceMultiplier = 0;
        isScanning = false;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if(!level.isClientSide) {
            if (getBlockState().getValue(BlockWindMillGenerator.STATE_MULTIBLOCK_FORMED)) {
                if(currentForceMultiplier < 1){
                    currentForceMultiplier += forceSteps;
                }else{
                    currentForceMultiplier = 1;
                }
                double randomOffset = currentRandomNoiseOffset.getValue((double) System.currentTimeMillis() / 100000,0,false);
                double noiseval = noise.getValue((double) System.currentTimeMillis() / 80000 + randomOffset / 10, 0, false);
                // use sqrt to make it more evenly distributed and less low wind speeds
                double windSpeed = windSpeedMultiplier * Math.sqrt(Math.abs(noiseval)) * Math.signum(noiseval) ;

                // make some difference in wind direction, but not too much, we want the windmill to be spinning!
                double windDirection = noiseDirection.getValue((double) System.currentTimeMillis() / 60000,0,false);
                if(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == Direction.Axis.X)
                    windSpeed = windSpeed *( 0.4 + 0.6 * Math.abs(Math.cos(2*Math.PI*windDirection)));
                if(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == Direction.Axis.Z)
                    windSpeed = windSpeed * ( 0.4 + 0.6 * Math.abs(Math.sin(2*Math.PI*windDirection)));

                myForce = 0;
                myInertia = 0;
                int numberOfBlocks = 0;
                for (int i = 0; i < size; i++) {
                    int r = i + 2;
                    int bladeNumOnThisRadius = 4*3;
                    double bladeSpeed = myMechanicalBlock.internalVelocity * r;
                    myForce += forcePerBlock * bladeNumOnThisRadius * Math.pow(windSpeed - bladeSpeed, 2) * Math.signum(windSpeed - bladeSpeed) * r;
                    myInertia += inertiaPerBlock* bladeNumOnThisRadius * r;
                    numberOfBlocks+=bladeNumOnThisRadius;
                }

                myFriction = frictionPerBlock * numberOfBlocks;
                myForce *= currentForceMultiplier; // will slowly increase to 1 over a few ticks
                //System.out.println(currentForceMultiplier+":"+windSpeed+" --  "+myForce+":"+myInertia+":"+myFriction+":"+myMechanicalBlock.internalVelocity);

            } else {
                currentForceMultiplier = 0;
                myForce = 0;
                myFriction = 1;
                myInertia = 1;
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWindMillGenerator) t).tick();
    }

    CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("size", size);
        return tag;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(compoundTag, p);
        if (compoundTag.contains("ping")) {
                PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
        if (compoundTag.contains("size")) {
            this.size = compoundTag.getInt("size");
        }
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
        if (direction == getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            return myMechanicalBlock;
        }
        return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }
}