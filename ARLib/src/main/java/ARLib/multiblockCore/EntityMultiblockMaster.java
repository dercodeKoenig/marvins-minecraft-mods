package ARLib.multiblockCore;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ARLib.ARLibRegistry.*;
import static ARLib.multiblockCore.BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED;

public abstract class EntityMultiblockMaster extends BlockEntity implements INetworkTagReceiver {


    abstract public Object[][][] getStructure();

    abstract public HashMap<Character, List<Block>> getCharMapping();

    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
    return InteractionResult.PASS;
    }

    // returns the array with the same shape as structure and tells if this block should be hidden
    // if a value is false, a multiblock part will not be set to STATE_MULTIBLOCK_FORMED and a placeholder will render the replaced block
    // the master will always be set to STATE_MULTIBLOCK_FORMED and will ignore this
    // also look at the method below this one for alternative way of hiding or not hiding a block
    public boolean[][][] hideBlocks() {
        Object[][][] structure = getStructure();
        boolean[][][] booleanArray = new boolean[structure.length][][];
        for (int i = 0; i < structure.length; i++) {
            Object[][] subArray = structure[i];
            booleanArray[i] = new boolean[subArray.length][];
            for (int j = 0; j < subArray.length; j++) {
                Object[] innerArray = subArray[j];
                booleanArray[i][j] = new boolean[innerArray.length];
                // Fill the boolean array with `true`
                for (int k = 0; k < innerArray.length; k++) {
                    booleanArray[i][j][k] = true;
                }
            }
        }
        return booleanArray;
    }
    // this can also be modified to hide a block based on its blockstate or not
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld){
        boolean[][][] hideBlocks = hideBlocks();
        return hideBlocks[y][z][x];
    }

    // set this to true to make the master block gui open for a click on any machine part block
    // must be implemented on the machine part block
    public boolean forwardInteractionToMaster = false;

    // called after the structure is completed, it will scan during onLoad() and complete structure if possible
    // on client it will execute if the structure is completed during onLoad()
    // server sends network packet to client so client will call this too
    public void onStructureComplete() {

    }
    // will be called if the stucture is scanned and can not be completed AND the structure was completed before
    // runs on client and server
    public void onStructureInvalid() {

    }


    public EntityMultiblockMaster(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }


    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            scanStructure();
        }
        if(level.isClientSide){
            if(getBlockState().getValue(STATE_MULTIBLOCK_FORMED)){
                onStructureComplete();
            }
        }
    }

    public Vec3i getControllerOffset(Object[][][] structure) {
        for (int y = 0; y < structure.length; y++) {
            for (int z = 0; z < structure[0].length; z++) {
                for (int x = 0; x < structure[0][0].length; x++) {
                    if (structure[y][z][x] instanceof Character && (Character) structure[y][z][x] == 'c')
                        return new Vec3i(x, y, z);
                }
            }
        }
        return null;
    }

    void un_replace_blocks() {
        Object[][][] structure = getStructure();

        Direction front = getFront();
        if (front == null) return;

        Vec3i offset = getControllerOffset(structure);

        for (int y = 0; y < structure.length; y++) {
            for (int z = 0; z < structure[y].length; z++) {
                for (int x = 0; x < structure[y][z].length; x++) {
                    //Ignore nulls
                    if (structure[y][z][x] == null)
                        continue;

                    int globalX = getBlockPos().getX() + (x - offset.getX()) * front.getStepZ() - (z - offset.getZ()) * front.getStepX();
                    int globalY = getBlockPos().getY() - y + offset.getY();
                    int globalZ = getBlockPos().getZ() - (x - offset.getX()) * front.getStepX() - (z - offset.getZ()) * front.getStepZ();
                    BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);

                    // this should load the chunk if it is not loaded
                    ChunkAccess chunk = level.getChunk(globalPos);
                    level.getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, true);

                    BlockEntity tile = level.getBlockEntity(globalPos);
                    if (tile instanceof EntityMultiblockPlaceholder t) {
                        if (t.replacedState != null) {
                            level.setBlock(globalPos, t.replacedState, 3);
                        } else {
                            level.setBlock(globalPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }

                    BlockState blockState = level.getBlockState(globalPos);
                    Block newBlock = blockState.getBlock();
                    if (newBlock instanceof BlockMultiblockPart bmp) {
                        bmp.setMaster(new BlockIdentifier(level, globalPos), null);
                    }
                    if (newBlock instanceof BlockMultiblockPart || newBlock instanceof BlockMultiblockMaster ) {
                        level.setBlock(globalPos, blockState.setValue(STATE_MULTIBLOCK_FORMED, false), 3);
                    }
                }
            }
        }
    }



    void replace_blocks() {
        Object[][][] structure = getStructure();
        boolean[][][] hideBlocks = hideBlocks();
        Direction front = getFront();
        if (front == null) return;

        Vec3i offset = getControllerOffset(structure);

        for (int y = 0; y < structure.length; y++) {
            for (int z = 0; z < structure[y].length; z++) {
                for (int x = 0; x < structure[y][z].length; x++) {
                    //Ignore nulls
                    if (structure[y][z][x] == null)
                        continue;

                    int globalX = getBlockPos().getX() + (x - offset.getX()) * front.getStepZ() - (z - offset.getZ()) * front.getStepX();
                    int globalY = getBlockPos().getY() - y + offset.getY();
                    int globalZ = getBlockPos().getZ() - (x - offset.getX()) * front.getStepX() - (z - offset.getZ()) * front.getStepZ();
                    BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);

                    // this should load the chunk if it is not loaded
                    ChunkAccess chunk = level.getChunk(globalPos);
                    level.getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, true);


                    // replace blocks that are not multiblock parts with placeholders to make them not render
                    BlockState blockState = level.getBlockState(globalPos);

                    // ignore air
                    if(blockState.getBlock().equals(Blocks.AIR))
                        continue;

                    if (!(blockState.getBlock() instanceof BlockMultiblockPart) &&
                            !(blockState.getBlock() instanceof BlockMultiblockMaster)
                    ) {
                        BlockMultiblockPlaceholder p = (BlockMultiblockPlaceholder) BLOCK_PLACEHOLDER.get();
                        BlockState newState = p.defaultBlockState();
                        level.setBlock(globalPos, newState, 3);
                        EntityMultiblockPlaceholder tile = (EntityMultiblockPlaceholder) level.getBlockEntity(globalPos);
                        tile.replacedState = blockState;
                        tile.renderBlock = !shouldHideBlock(y,z,x,blockState);
                    }

                    // at this point the block is a multiBlockPart or multiBlockMaster
                    blockState = level.getBlockState(globalPos);
                    if (shouldHideBlock(y,z,x,blockState) || blockState.getBlock() instanceof BlockMultiblockMaster) // always set master to be formed
                        level.setBlock(globalPos, blockState.setValue(STATE_MULTIBLOCK_FORMED, true), 3);

                    blockState = level.getBlockState(globalPos);
                    if (blockState.getBlock() instanceof BlockMultiblockPart t) {
                        t.setMaster(new BlockIdentifier(level, globalPos), getBlockPos());
                    }
                }
            }
        }
    }


    // when structure is assembled it sets all blockstates new because it changes the STATE_MULTIBLOCK_FORMED
    // this triggers re-scan and messes up tiles so block scanning while scanning
    boolean isScanning = false;

    public void scanStructure() {
        if (level.isClientSide) return;
        if (isScanning) return;
        isScanning = true;

        boolean canComplete = canCompleteStructure();

        if (!canComplete) {
            if(getBlockState().getValue(STATE_MULTIBLOCK_FORMED)){
                CompoundTag info = new CompoundTag();
                info.putBoolean("onStructureInvalid", true);
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
                onStructureInvalid();
            }
            un_replace_blocks();
        } else {
            replace_blocks();
            onStructureComplete();

            CompoundTag info = new CompoundTag();
            info.putBoolean("onStructureComplete", true);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        isScanning = false;
    }

    Direction directionFallbackWhenAfterDestroy;

    public Direction getFront() {
        BlockState state = level.getBlockState(getBlockPos());
        Direction front;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            front = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            directionFallbackWhenAfterDestroy = front;
            return front;
        } else {
            if (directionFallbackWhenAfterDestroy != null) {
                front = directionFallbackWhenAfterDestroy;
                return front;
            } else {
                return null;
            }
        }
    }

    public boolean canCompleteStructure() {
        Object[][][] structure = getStructure();

        Direction front = getFront();
        if (front == null) return false;


        Vec3i offset = getControllerOffset(structure);

        for (int y = 0; y < structure.length; y++) {
            for (int z = 0; z < structure[y].length; z++) {
                for (int x = 0; x < structure[y][z].length; x++) {
                    //Ignore nulls
                    if (structure[y][z][x] == null)
                        continue;

                    int globalX = getBlockPos().getX() + (x - offset.getX()) * front.getStepZ() - (z - offset.getZ()) * front.getStepX();
                    int globalY = getBlockPos().getY() - y + offset.getY();
                    int globalZ = getBlockPos().getZ() - (x - offset.getX()) * front.getStepX() - (z - offset.getZ()) * front.getStepZ();
                    BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);

                    // this should load the chunk if it is not loaded
                    ChunkAccess chunk = level.getChunk(globalPos);
                    level.getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, true);


                    BlockState blockState = level.getBlockState(globalPos);
                    Block block = blockState.getBlock();
                    BlockEntity tile = level.getBlockEntity(globalPos);

                    if (tile instanceof EntityMultiblockPlaceholder t && t.replacedState != null) {
                        block = t.replacedState.getBlock();
                    }
                    if (!getAllowableBlocks(structure[y][z][x]).contains(block)) {
                        //for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                        //    player.sendSystemMessage(Component.literal("Invalid Block at "+globalPos+" ( "+block + " ) "));
                        //}
                        return false;
                    }
                }
            }
        }
        return true;
    }


    public List<Block> getAllowableBlocks(Object input) {
        if (input instanceof Character && getCharMapping().containsKey(input)) {
            return getCharMapping().get(input);
        } else if (input instanceof String) { //OreDict entry

        } else if (input instanceof Block) {
            List<Block> list = new ArrayList<>();
            list.add((Block) input);
            return list;
        } else if (input instanceof List) {
            return (List<Block>) input;
        }
        return new ArrayList<>();
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {

    }

    @Override
    public void readClient(CompoundTag tag) {
        if(tag.contains("onStructureComplete")){
            onStructureComplete();
        }
        if(tag.contains("onStructureInvalid")){
            onStructureInvalid();
        }
    }
}
