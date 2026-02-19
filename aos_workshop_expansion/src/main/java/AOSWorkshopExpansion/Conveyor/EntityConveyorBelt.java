package AOSWorkshopExpansion.Conveyor;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static AOSWorkshopExpansion.Registry.ENTITY_CONVEYOR_BELT;

public class EntityConveyorBelt extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver {


    public double myInertia = 1;
    public double myFriction = 1;
    public double maxStress = 600;

    // items and progress
    public HashMap<ItemStack, Float> items_progress = new HashMap<>();
    // unique id and same item reference for server/client sync
    public HashMap<Long, ItemStack> id_items = new HashMap<>();

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
        if(level.isClientSide){
            CompoundTag ping = new CompoundTag();
            ping.putInt("ping",0);
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

        // move items
        float progress = (float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / Static.TPS / 360);
        if (progress != 0) {
            for (Long id : new ArrayList<>(id_items.keySet())) {
                ItemStack stack = id_items.get(id);
                items_progress.put(stack, items_progress.get(stack) + progress);
                float newProgress = items_progress.get(stack);

                if (newProgress > 1) {
                    Direction.Axis axis = getBlockState().getValue(ConveyorBelt.AXIS);

                    Direction target = Direction.NORTH;
                    if (axis == Direction.Axis.X)
                        target = Direction.EAST;

                    BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(target));
                    if (neighbor instanceof EntityConveyorBelt neighborBelt) {
                        if(neighbor.getBlockState().getValue(ConveyorBelt.AXIS) == getBlockState().getValue(ConveyorBelt.AXIS))
                            neighborBelt.addItem(id, stack, newProgress - 1, false, level.registryAccess());
                        else
                            neighborBelt.addItem(id, stack, 0.5f, false, level.registryAccess());
                        removeItem(id, false);
                    } else {
                        popItem(id, target);
                    }
                }
                if (newProgress < 0) {
                    Direction.Axis axis = getBlockState().getValue(ConveyorBelt.AXIS);

                    Direction target = Direction.SOUTH;
                    if (axis == Direction.Axis.X)
                        target = Direction.WEST;

                    BlockEntity neighbor = level.getBlockEntity(getBlockPos().relative(target));
                    if (neighbor instanceof EntityConveyorBelt neighborBelt) {
                        if(neighbor.getBlockState().getValue(ConveyorBelt.AXIS) == getBlockState().getValue(ConveyorBelt.AXIS))
                            neighborBelt.addItem(id, stack, newProgress + 1, false, level.registryAccess());
                        else
                            neighborBelt.addItem(id, stack, 0.5f, false, level.registryAccess());
                        removeItem(id, false);
                    } else {
                        popItem(id, target);
                    }
                }
            }
            setChanged();
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityConveyorBelt) t).tick();
    }

    public CompoundTag writeEntry(Long id, ItemStack stack, float progress, HolderLookup.Provider registryAccess){
        CompoundTag addTag = new CompoundTag();
        addTag.putLong("id", id);
        addTag.put("stack", stack.save(registryAccess));
        addTag.putFloat("progress", progress);
        return addTag;
    }
    public void loadEntry(CompoundTag addItemTag, HolderLookup.Provider registryAccess){
        Long id = addItemTag.getLong("id");
        ItemStack stack = ItemStack.parse(registryAccess, addItemTag.get("stack")).get();
        float progress = addItemTag.getFloat("progress");
        id_items.put(id, stack);
        items_progress.put(stack, progress);
    }
    public void loadItemsFromTag(CompoundTag tag, HolderLookup.Provider registryAccess){
        if(tag.contains("id_item_progress_entries")) {
            ListTag entries = tag.getList("id_item_progress_entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                loadEntry(entries.getCompound(0), registryAccess);
            }
        }
    }
    public void writeItemsToTag(CompoundTag tag, HolderLookup.Provider registryAccess){
        ListTag items_tag = new ListTag();
        for(Long id : id_items.keySet()){
            ItemStack item = id_items.get(id);
            float progress = items_progress.get(item);
            CompoundTag entry= writeEntry(id, item, progress, registryAccess);
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
        if(compoundTag.contains("ping")){
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
        loadItemsFromTag(tag,registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        writeItemsToTag(tag,registries);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof ConveyorBelt) {

            if (direction == Direction.DOWN) {
                // below check for engine block
                BlockState below = level.getBlockState(getBlockPos().below());
                if (below.getBlock() instanceof ConveyorEngine) {
                    if (below.getValue(ConveyorEngine.AXIS) != getBlockState().getValue(ConveyorBelt.AXIS))
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
