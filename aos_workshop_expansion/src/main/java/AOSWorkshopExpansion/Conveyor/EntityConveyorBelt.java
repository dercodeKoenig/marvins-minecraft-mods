package AOSWorkshopExpansion.Conveyor;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.InventoryUtils;
import ARLib.utils.ItemUtils;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
import BetterPipes.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_BELT;

public class EntityConveyorBelt extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver, IItemHandler {


    // items and progress
    public HashMap<ItemStack, Float> items_progress = new HashMap<>();
    // unique id and same item reference for server/client sync
    public HashMap<Long, ItemStack> id_items = new LinkedHashMap<>();

    public int lastLight;
    public boolean requiresMeshUpdate;
    public MeshData mesh;
    public VertexBuffer vertexBuffer;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return ConveyorConfig.INSTANCE.conveyorMaxStress;
        }

        @Override
        public double getInertia(Direction face) {
            return ConveyorConfig.INSTANCE.conveyorInertia;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return ConveyorConfig.INSTANCE.conveyorResistance;
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
        if (level.isClientSide) {
            CompoundTag ping = new CompoundTag();
            ping.putInt("ping", 0);
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, ping));
        }
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

    public void popItem(Long id, Direction target) {
        ItemStack stack = id_items.get(id);
        Vec3 pos = getBlockPos().getCenter().relative(target, 0.7);
        ItemEntity ie = new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy());
        float speed = 0.05f;
        ie.setDeltaMovement(target.getStepX() * speed, speed * 2, target.getStepZ() * speed);
        level.addFreshEntity(ie);
        removeItem(id, true);
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        // collect all item entities at my block
        if (!level.isClientSide) {
            AABB scanningArea = new AABB(getBlockPos());
            List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, scanningArea);
            for (ItemEntity item : itemEntities) {
                ItemStack stack = item.getItem().copy();
                item.discard();
                Long id = new Random().nextLong();
                addItem(id, stack, 0.5f, true, level.registryAccess());
            }
        }


        float progress = (float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / Static.TPS / 360);
        if (progress != 0) {

            Direction facing = getBlockState().getValue(ConveyorBelt.FACING);
            Direction.Axis axis = facing.getAxis();
            boolean isDiagonal = getBlockState().getValue(ConveyorBelt.DIAGONAL);

            Direction target = null;
            if (progress > 0) {
                target = Direction.NORTH;
                if (axis == Direction.Axis.X)
                    target = Direction.EAST;
            }
            if (progress < 0) {
                target = Direction.SOUTH;
                if (axis == Direction.Axis.X)
                    target = Direction.WEST;
            }

            // pull from inventories
            if (!level.isClientSide) {
                IItemHandler neighbor = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        getBlockPos().relative(target.getOpposite()),
                        target
                );
                if (neighbor != null && !(neighbor instanceof EntityConveyorBelt)) {
                    // see if we can extract something
                    // but do not extract too fast
                    boolean canExtract = false;
                    float initialProgress = 0;
                    if (progress > 0) {
                        if (items_progress.isEmpty() || Collections.min(items_progress.values()) > 0.4) {
                            canExtract = true;
                            initialProgress = 0;
                        }
                    } else {
                        if (items_progress.isEmpty() || Collections.max(items_progress.values()) < 0.6) {
                            canExtract = true;
                            initialProgress = 1;
                        }
                    }
                    if (canExtract) {
                        for (int i = 0; i < neighbor.getSlots(); i++) {
                            ItemStack extracted = neighbor.extractItem(i, 1, false);
                            if (!extracted.isEmpty()) {
                                Long id = new Random().nextLong();
                                addItem(id, extracted.copy(), initialProgress, true, level.registryAccess());
                            }
                        }
                    }
                }
            }

            // move items
            for (Long id : new ArrayList<>(id_items.keySet())) {
                ItemStack stack = id_items.get(id);
                items_progress.put(stack, items_progress.get(stack) + progress);
                float newProgress = items_progress.get(stack);

                boolean doOutput = false;
                if (newProgress < 0) {
                    newProgress += 1;
                    doOutput = true;
                }
                if (newProgress > 1) {
                    newProgress -= 1;
                    doOutput = true;
                }

                if (doOutput) {
                    // output to next conveyor or pop item
                    // there are 2 conveyors to check:
                    // if diagonal: neighbor & above neighbor
                    // else: neighbor & below neighbor
                    BlockPos targetPos = getBlockPos().relative(target);
                    if (target == facing && isDiagonal)
                        targetPos = targetPos.above();

                    EntityConveyorBelt targetBelt = getConnectedConveyor(targetPos);
                    if (targetBelt == null)
                        targetBelt = getConnectedConveyor(targetPos.below());

                    if (targetBelt != null) {
                        targetBelt.addItem(id, stack, newProgress, false, level.registryAccess());
                        removeItem(id, false);
                    } else {
                        if (!level.isClientSide) {
                            // check if the neighbor has a itemhandler to insert items into an inventory
                            targetPos = getBlockPos().relative(target);
                            IItemHandler neighborItemHandler = level.getCapability(
                                    Capabilities.ItemHandler.BLOCK,
                                    targetPos,
                                    target.getOpposite()
                            );

                            ItemStack remaining = stack;

                            // special case: neighbor is a conveyor but it is wrong oriented
                            if (neighborItemHandler instanceof EntityConveyorBelt neighborBelt) {
                                neighborBelt.addItem(id, stack, 0.5f, true, level.registryAccess());
                                remaining = ItemStack.EMPTY;
                            } else if (neighborItemHandler != null) {
                                for (int i = 0; i < neighborItemHandler.getSlots(); i++) {
                                    if (remaining.isEmpty())
                                        break;
                                    remaining = neighborItemHandler.insertItem(i, remaining.copy(), false);
                                }


                                // pop to the side when there is an itemhandler in front or it will land on the belt again
                                if (new Random().nextBoolean())
                                    target = target.getClockWise();
                                else
                                    target = target.getCounterClockWise();

                            }

                            // pop remaining items
                            id_items.put(id, remaining);
                            items_progress.put(remaining, 0f);
                            popItem(id, target);
                        }
                    }
                }
            }
            setChanged();
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityConveyorBelt) t).tick();
    }

    public CompoundTag writeEntry(Long id, ItemStack stack, float progress, HolderLookup.Provider registryAccess) {
        CompoundTag addTag = new CompoundTag();
        addTag.putLong("id", id);
        addTag.put("stack", stack.save(registryAccess));
        addTag.putFloat("progress", progress);
        return addTag;
    }

    public void loadEntry(CompoundTag addItemTag, HolderLookup.Provider registryAccess) {
        Long id = addItemTag.getLong("id");
        ItemStack stack = ItemStack.parse(registryAccess, addItemTag.get("stack")).get();
        float progress = addItemTag.getFloat("progress");
        id_items.put(id, stack);
        items_progress.put(stack, progress);
    }

    public void loadItemsFromTag(CompoundTag tag, HolderLookup.Provider registryAccess) {
        if (tag.contains("id_item_progress_entries")) {
            ListTag entries = tag.getList("id_item_progress_entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                loadEntry(entries.getCompound(0), registryAccess);
            }
        }
    }

    public void writeItemsToTag(CompoundTag tag, HolderLookup.Provider registryAccess) {
        ListTag items_tag = new ListTag();
        for (Long id : id_items.keySet()) {
            ItemStack item = id_items.get(id);
            float progress = items_progress.get(item);
            CompoundTag entry = writeEntry(id, item, progress, registryAccess);
            items_tag.add(entry);
        }
        tag.put("id_item_progress_entries", items_tag);
    }

    public void addItem(Long id, ItemStack stack, float progress, boolean syncToClient, RegistryAccess registryAccess) {
        items_progress.put(stack, progress);
        id_items.put(id, stack);
        if (syncToClient && !level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("addItem", writeEntry(id, stack, progress, registryAccess));
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        setChanged();
    }

    public void removeItem(Long id, boolean syncToClient) {
        ItemStack stack = id_items.get(id);
        id_items.remove(id);
        items_progress.remove(stack);
        if (syncToClient && !level.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.putLong("removeId", id);
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        setChanged();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        myMechanicalBlock.mechanicalReadServer(compoundTag, serverPlayer);
        if (compoundTag.contains("ping")) {
            CompoundTag info = new CompoundTag();
            writeItemsToTag(info, level.registryAccess());
            PacketDistributor.sendToPlayer(serverPlayer, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
        if (compoundTag.contains("removeId")) {
            Long id = compoundTag.getLong("removeId");
            ItemStack stack = id_items.get(id);
            id_items.remove(id);
            items_progress.remove(stack);
        }
        if (compoundTag.contains("addItem")) {
            CompoundTag addItemTag = compoundTag.getCompound("addItem");
            loadEntry(addItemTag, level.registryAccess());
        }
        loadItemsFromTag(compoundTag, level.registryAccess());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        loadItemsFromTag(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        writeItemsToTag(tag, registries);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof ConveyorBelt) {
            if (direction == Direction.DOWN) {
                // below check for engine block
                BlockState below = level.getBlockState(getBlockPos().below());
                if (below.getBlock() instanceof ConveyorEngine) {
                    if (below.getValue(ConveyorEngine.AXIS) != getBlockState().getValue(ConveyorBelt.FACING).getAxis())
                        return myMechanicalBlock;
                }
            }
        }
        // connections to other conveyor belts we will handle ourselves in getConnectedParts
        // we do not expose the mechanical block to anyone else
        return null;
    }


    public EntityConveyorBelt getConnectedConveyor(BlockPos otherPos) {

        Direction myFacing = getBlockState().getValue(ConveyorBelt.FACING);
        BlockPos myPos = getBlockPos();
        boolean isDiagonal = getBlockState().getValue(ConveyorBelt.DIAGONAL);

        BlockState otherState = level.getBlockState(otherPos);
        if (!(otherState.getBlock() instanceof ConveyorBelt))
            return null;
        Direction otherFacing = otherState.getValue(ConveyorBelt.FACING);
        boolean otherIsDiagonal = otherState.getValue(ConveyorBelt.DIAGONAL);
        BlockEntity other = level.getBlockEntity(otherPos);
        if (!(other instanceof EntityConveyorBelt otherConveyor))
            return null;

        if (otherFacing.getAxis() != myFacing.getAxis())
            // not same axis
            return null;

        BlockPos behind = myPos.relative(myFacing.getOpposite());
        if (otherPos.equals(behind)) {
            // behind can be flat (__> or _/>)
            if (!otherIsDiagonal)
                return otherConveyor;
            // if behind is not flat, it has to be facing away (\_> or \/>)
            if ((otherIsDiagonal && otherFacing.getOpposite() == myFacing))
                return otherConveyor;
        }

        BlockPos behind_below = behind.below();
        if (otherPos.equals(behind_below)) {
            // it has to be diagonal and facing toward us (/‾>)
            if ((otherIsDiagonal && otherFacing == myFacing))
                return otherConveyor;
        }

        BlockPos infront = myPos.relative(myFacing);
        if (otherPos.equals(infront)) {
            // both can be flat (_>_)
            if (!isDiagonal && !otherIsDiagonal)
                return otherConveyor;
            // we can be flat and the other is diagonal away from us (_>/)
            if (!isDiagonal && otherIsDiagonal && otherFacing == myFacing)
                return otherConveyor;
            // we can be diagonal, but then the other has to be diagonal and facing toward us (/>\)
            if (isDiagonal && otherIsDiagonal && otherFacing == myFacing.getOpposite())
                return otherConveyor;
        }

        BlockPos infront_below = infront.below();
        if (otherPos.equals(infront_below)) {
            // only 1 option - we are flat and other is diagonal toward us (‾>\)
            if (!isDiagonal && otherIsDiagonal && otherFacing == myFacing.getOpposite())
                return otherConveyor;
        }

        BlockPos infront_above = infront.above();
        if (otherPos.equals(infront_above)) {
            //  we are diagonal and the other is flat (/>‾)
            if (isDiagonal && !otherIsDiagonal)
                return otherConveyor;
            //  we are diagonal and the other is also diagonal but facing away (/>/)
            if (isDiagonal && otherIsDiagonal && otherFacing == myFacing)
                return otherConveyor;

        }

        return null;
    }

    // the default method only checks all directions, but we need to check diagonal too
    public Map<Direction, AbstractMechanicalBlock> getConnectedParts(IMechanicalBlockProvider mechanicalBlockProvider, @Nullable AbstractMechanicalBlock MechanicalBlock) {
        Map<Direction, AbstractMechanicalBlock> connectedBlocks = new HashMap();

        // check for engine below
        AbstractMechanicalBlock mechanicalBlock = mechanicalBlockProvider.getMechanicalBlock(Direction.DOWN);
        if (mechanicalBlock != null) {
            // getMechanicalBlock already checks for direction below if there is a engine in correct rotation
            connectedBlocks.put(Direction.DOWN, mechanicalBlock);
        }

        // check for conveyor
        // we consider 3 blocks behind and 3 blocks infront
        // we first check flat connections and then diagonal if flat has no connections
        BlockPos myPos = getBlockPos();
        Direction myFacing = getBlockState().getValue(ConveyorBelt.FACING);
        Direction myFacingOpposite = myFacing.getOpposite();

        if (getConnectedConveyor(myPos.relative(myFacing)) instanceof EntityConveyorBelt otherBelt)
            connectedBlocks.put(myFacing, otherBelt.myMechanicalBlock);
        else if (getConnectedConveyor(myPos.relative(myFacing).above()) instanceof EntityConveyorBelt otherBelt)
            connectedBlocks.put(myFacing, otherBelt.myMechanicalBlock);
        else if (getConnectedConveyor(myPos.relative(myFacing).below()) instanceof EntityConveyorBelt otherBelt)
            connectedBlocks.put(myFacing, otherBelt.myMechanicalBlock);

        if (getConnectedConveyor(myPos.relative(myFacingOpposite)) instanceof EntityConveyorBelt otherBelt)
            connectedBlocks.put(myFacingOpposite, otherBelt.myMechanicalBlock);
        else if (getConnectedConveyor(myPos.relative(myFacingOpposite).above()) instanceof EntityConveyorBelt otherBelt)
            connectedBlocks.put(myFacingOpposite, otherBelt.myMechanicalBlock);
        else if (getConnectedConveyor(myPos.relative(myFacingOpposite).below()) instanceof EntityConveyorBelt otherBelt)
            connectedBlocks.put(myFacingOpposite, otherBelt.myMechanicalBlock);


        return connectedBlocks;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public int getSlots() {
        return id_items.size();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i < id_items.size())
            return new ArrayList<>(id_items.values()).get(i);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int ignored, ItemStack itemStack, boolean simulate) {
        // i do not allow insertion, i pull from inventories myself
        return itemStack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < id_items.size()) {
            Long id = new ArrayList<>(id_items.keySet()).get(slot);
            ItemStack item = id_items.get(id);
            if (!simulate)
                removeItem(id, true);
            return item.copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int i) {
        return 99;
    }

    @Override
    public boolean isItemValid(int i, ItemStack itemStack) {
        // i do not allow insertion, i pull from inventories myself
        return false;
    }
}
