package AOSWorkshopExpansion.Sieve;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.ItemUtils;
import ARLib.utils.RecipePart;
import ARLib.utils.RecipePartWithProbability;
import AOSWorkshopExpansion.Sieve.Items.ItemSieveUpgrade;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import AgeOfSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static AOSWorkshopExpansion.Registry.ENTITY_SIEVE;
import static AOSWorkshopExpansion.Registry.SIEVE_HOPPER_UPGRADE;

public class EntitySieve extends BlockEntity implements IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {


    public VertexBuffer vertexBuffer;
    public MeshData mesh;
    public VertexBuffer vertexBuffer2;
    public MeshData mesh2;
    public VertexBuffer vertexBuffer3;
    public MeshData mesh3;
    public VertexBuffer myInputRendererBuffer;
    public VertexBuffer myHopperInputRendererBuffer;
    public int lastLight;


    // aw npc compat
    public static Set<BlockIdentifier> knownBlockEntities = new HashSet<>();
    public HashMap<Entity, Integer> workersWorkingHereWithTimeout = new HashMap<>();


    ItemStack lastInputStackForRender = ItemStack.EMPTY;
    ResourceLocation inputStackTexture = ResourceLocation.withDefaultNamespace("textures/block/air");

    ItemStack lastHopperInputStackForRender = ItemStack.EMPTY;
    ResourceLocation hopperStackTexture = ResourceLocation.withDefaultNamespace("textures/block/air");


    ItemStack myMesh = ItemStack.EMPTY;
    ItemStack myInputs = ItemStack.EMPTY;
    ItemStack myHopperInputs = ItemStack.EMPTY;

    SieveConfig.SieveRecipe currentRecipe = null;
    double currentProgress;
    double client_syncedCurrentRecipeTime;

    int ticksRemainingForForce = 0;
    double myFriction = 1;
    double myForce = 0;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 100;
        }

        @Override
        public double getInertia(Direction face) {
            return 1;
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

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
        if (level.isClientSide) {
            CompoundTag myOnloadTag = new CompoundTag();
            myOnloadTag.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, myOnloadTag));
        }

        if (!level.isClientSide) {
            knownBlockEntities.add(new BlockIdentifier(level, getBlockPos()));
        }
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                myInputRendererBuffer.close();
                myHopperInputRendererBuffer.close();
                vertexBuffer3.close();
                vertexBuffer.close();
                vertexBuffer2.close();
            });
        }

        if (!level.isClientSide) {
            knownBlockEntities.remove(new BlockIdentifier(level, getBlockPos()));
        }
        super.setRemoved();
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
        if (tag.contains("hasMesh")) {
            if (!tag.getBoolean("hasMesh")) {
                myMesh = ItemStack.EMPTY;
            }
        }
        if (tag.contains("mesh")) {
            myMesh = ItemStack.parse(level.registryAccess(), tag.getCompound("mesh")).get();
        }

        if (tag.contains("hasHopperInput")) {
            if (!tag.getBoolean("hasHopperInput")) {
                myHopperInputs = ItemStack.EMPTY;
            }
        }
        if (tag.contains("hopperInputs")) {
            myHopperInputs = ItemStack.parse(level.registryAccess(), tag.getCompound("hopperInputs")).get();
        }

        if (tag.contains("hasInput")) {
            if (!tag.getBoolean("hasInput")) {
                myInputs = ItemStack.EMPTY;
            }
        }
        if (tag.contains("inputs")) {
            ItemStack oldStack = myInputs.copy();
            myInputs = ItemStack.parse(level.registryAccess(), tag.getCompound("inputs")).get();
            if (oldStack.getItem().equals(myInputs.getItem()) && myInputs.getCount() + 1 == oldStack.getCount()) {
                // this was an update that the recipe was processed so myStack decreased by one, update the progress
                currentProgress = 0;
            }
        }
        if (tag.contains("timeRequired")) {
            client_syncedCurrentRecipeTime = tag.getDouble("timeRequired");
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(tag, p);
        if (tag.contains("ping")) {
            CompoundTag info = getMeshUpdateTag();
            if (currentRecipe != null) {
                info.putDouble("timeRequired", currentRecipe.timeRequired);
            }
            PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        if (tag.contains("mesh")) {
            CompoundTag meshTag = tag.getCompound("mesh");
            myMesh = ItemStack.parse(registries, meshTag).get();
        }
        if (tag.contains("inputs")) {
            CompoundTag inputsTag = tag.getCompound("inputs");
            myInputs = ItemStack.parse(registries, inputsTag).get();
        }
        if (tag.contains("hopperInputs")) {
            CompoundTag hopperInputsTag = tag.getCompound("hopperInputs");
            myHopperInputs = ItemStack.parse(registries, hopperInputsTag).get();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        if (!myMesh.isEmpty()) {
            Tag meshTag = myMesh.save(registries);
            tag.put("mesh", meshTag);
        }
        if (!myInputs.isEmpty()) {
            Tag inputsTag = myInputs.save(registries);
            tag.put("inputs", inputsTag);
        }
        if (!myHopperInputs.isEmpty()) {
            Tag hopperInputsTag = myHopperInputs.save(registries);
            tag.put("hopperInputs", hopperInputsTag);
        }
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockSieve) {
            if (side == myState.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
                if (t instanceof EntityCrankShaftBase cs && cs.myType == CrankShaftType.SMALL) {
                    if (cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()) {
                        return myMechanicalBlock;
                    }
                }
            }
        }
        return null;
    }

    public EntitySieve(BlockPos pos, BlockState blockState) {
        super(ENTITY_SIEVE.get(), pos, blockState);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                myInputRendererBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                myHopperInputRendererBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer3 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    CompoundTag getMeshUpdateTag() {
        CompoundTag meshInfo = new CompoundTag();
        meshInfo.putBoolean("hasMesh", !myMesh.isEmpty());
        if (!myMesh.isEmpty()) {
            Tag meshTag = myMesh.save(level.registryAccess());
            meshInfo.put("mesh", meshTag);
        }

        meshInfo.putBoolean("hasInput", !myInputs.isEmpty());
        if (!myInputs.isEmpty()) {
            Tag inputsTag = myInputs.save(level.registryAccess());
            meshInfo.put("inputs", inputsTag);
        }

        meshInfo.putBoolean("hasHopperInput", !myHopperInputs.isEmpty());
        if (!myHopperInputs.isEmpty()) {
            Tag inputsTag = myHopperInputs.save(level.registryAccess());
            meshInfo.put("hopperInputs", inputsTag);
        }

        return meshInfo;
    }

    void broadcastChangeOfInventoryAndSetChanged() {
        if (!level.isClientSide) {
            CompoundTag meshInfo = getMeshUpdateTag();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, meshInfo));
            setChanged();
        }
    }

    void removeMyMesh() {
        if (!myMesh.isEmpty()) {
            ItemEntity i = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), myMesh);
            i.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i);
            myMesh = ItemStack.EMPTY;

            ItemEntity i2 = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), myInputs);
            i2.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i2);
            myInputs = ItemStack.EMPTY;

            broadcastChangeOfInventoryAndSetChanged();
        }
    }

    void removeHopperUpgrade() {
        if (getBlockState().getValue(BlockSieve.HOPPER_UPGRADE)) {
            ItemEntity i = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), new ItemStack(SIEVE_HOPPER_UPGRADE.get(), 1));
            i.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i);
            if (level.getBlockState(getBlockPos()).getBlock() instanceof BlockSieve)
                level.setBlock(getBlockPos(), getBlockState().setValue(BlockSieve.HOPPER_UPGRADE, false), 3);

            ItemEntity i2 = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), myHopperInputs);
            i2.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i2);
            myHopperInputs = ItemStack.EMPTY;

            broadcastChangeOfInventoryAndSetChanged();
        }
    }

    SieveConfig.SieveRecipe getRecipeForInputs(ItemStack inputs) {
        for (SieveConfig.SieveRecipe i : SieveConfig.INSTANCE.recipes) {
            if (ItemUtils.matches(i.requiredMesh, myMesh)) {
                RecipePart input = i.inputItem;
                if (ItemUtils.matches(input.id, inputs)) {
                    return i;
                }
            }
        }
        return null;
    }

    void completeRecipe() {
        if (currentRecipe != null) {
            for (RecipePartWithProbability item : currentRecipe.outputItems) {
                int actual_num = 0;
                for (int i = 0; i < item.amount; ++i) {
                    if (item.p >= 1.0F || Math.random() < (double) item.p) {
                        ++actual_num;
                    }
                }
                ItemStack output = ItemUtils.getItemStackFromIdOrTag(item.id, actual_num, level.registryAccess());
                for (Direction i : Direction.values()) {
                    if (i == Direction.UP) continue;
                    IItemHandler inv = level.getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos().relative(i), i.getOpposite());
                    if (inv instanceof IItemHandler) {
                        for (int j = 0; j < inv.getSlots(); j++) {
                            output = inv.insertItem(j, output, false);
                            if (output.isEmpty()) break;
                        }
                    }
                }
                if (!output.isEmpty()) {
                    ItemEntity ie = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), output);
                    level.addFreshEntity(ie);
                }
            }
            myInputs.shrink(1);
            broadcastChangeOfInventoryAndSetChanged();
            currentRecipe = null;
        }
        myFriction = SieveConfig.INSTANCE.baseResistance;
    }

    public boolean tryAddElementToSieveInventory(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (myInputs.isEmpty()) {
            if (getRecipeForInputs(stack) != null) {
                myInputs = stack.copyWithCount(1);
                stack.shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        } else {
            if (ItemStack.isSameItemSameComponents(stack, myInputs)) {
                int maxStackSize = Math.min(myInputs.getMaxStackSize(), SieveConfig.INSTANCE.inventorySize);
                int toAdd = Math.min(maxStackSize - myInputs.getCount(), 1);
                myInputs.grow(toAdd);
                stack.shrink(toAdd);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        }
        return false;
    }

    public boolean tryAddElementToHopperInventory(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!getBlockState().getValue(BlockSieve.HOPPER_UPGRADE)) return false;
        if (myHopperInputs.isEmpty()) {
            if (getRecipeForInputs(stack) != null) {
                myHopperInputs = stack.copyWithCount(1);
                stack.shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        } else {
            if (ItemStack.isSameItemSameComponents(stack, myHopperInputs)) {
                int maxStackSize = Math.min(myHopperInputs.getMaxStackSize(), SieveConfig.INSTANCE.inventorySizeHopper);
                int toAdd = Math.min(maxStackSize - myHopperInputs.getCount(), 1);
                myHopperInputs.grow(toAdd);
                stack.shrink(toAdd);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        }
        return false;
    }

    public InteractionResult use(Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS_NO_ITEM_USED;

        if (!player.isShiftKeyDown()) {
            if (player.getMainHandItem().getItem() instanceof IMesh) {
                removeMyMesh();
                myMesh = player.getMainHandItem().copyWithCount(1);
                player.getMainHandItem().shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
                return InteractionResult.SUCCESS;
            }
            if (player.getMainHandItem().getItem() instanceof ItemSieveUpgrade && !getBlockState().getValue(BlockSieve.HOPPER_UPGRADE)) {
                level.setBlock(getBlockPos(), getBlockState().setValue(BlockSieve.HOPPER_UPGRADE, true), 3);
                player.getMainHandItem().shrink(1);
            } else if (tryAddElementToHopperInventory(player.getMainHandItem())) return InteractionResult.SUCCESS;
            else if (tryAddElementToHopperInventory(player.getOffhandItem())) return InteractionResult.SUCCESS;
            else if (tryAddElementToSieveInventory(player.getMainHandItem())) return InteractionResult.SUCCESS;
            else if (tryAddElementToSieveInventory(player.getOffhandItem())) return InteractionResult.SUCCESS;
            else {
                if (tryAddManualWork()) {
                    player.causeFoodExhaustion(0.5f);
                }
                return InteractionResult.SUCCESS_NO_ITEM_USED;
            }
        } else {
            if (player.getMainHandItem().isEmpty()) {
                removeMyMesh();
                broadcastChangeOfInventoryAndSetChanged();
                return InteractionResult.SUCCESS_NO_ITEM_USED;
            }
        }
        return InteractionResult.PASS;
    }

    public boolean tryAddManualWork() {
        if (ticksRemainingForForce < 5 && getMechanicalBlock(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) == null) {
            ticksRemainingForForce += 5;
            return true;
        }
        return false;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (!level.isClientSide) {
            if (ticksRemainingForForce > 0 && getMechanicalBlock(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) == null) {
                ticksRemainingForForce--;
                myForce = SieveConfig.INSTANCE.clickForce - SieveConfig.INSTANCE.k * myMechanicalBlock.internalVelocity;
            } else {
                myForce = 0;
                ticksRemainingForForce = 0;
            }
        }
        if (!level.isClientSide) {
            if (getBlockState().getValue(BlockSieve.HOPPER_UPGRADE)) {
                IItemHandler inventoryAbove = level.getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos().relative(Direction.UP), Direction.DOWN);
                if (inventoryAbove instanceof IItemHandler) {
                    for (int j = 0; j < inventoryAbove.getSlots(); j++) {
                        ItemStack stack = inventoryAbove.getStackInSlot(j);
                        tryAddElementToHopperInventory(stack);
                    }
                }
            }
            if (!myHopperInputs.isEmpty()) {
                tryAddElementToSieveInventory(myHopperInputs);
            }
            if (currentRecipe != null) {
                currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
                if (currentProgress >= currentRecipe.timeRequired) {
                    completeRecipe();
                    currentProgress = 0;
                }
            } else {
                if (!myInputs.isEmpty() && !myMesh.isEmpty()) {
                    currentRecipe = getRecipeForInputs(myInputs);
                    if (currentRecipe != null) {
                        myFriction = SieveConfig.INSTANCE.baseResistance + currentRecipe.additionalResistance;
                        CompoundTag timeRequiredTag = new CompoundTag();
                        timeRequiredTag.putDouble("timeRequired", currentRecipe.timeRequired);
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, timeRequiredTag));
                    }
                }
            }
        } else {
            if (!myInputs.isEmpty()) {
                currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
                if (currentProgress > client_syncedCurrentRecipeTime) {

                }

                if (myInputs.getItem() instanceof BlockItem b) {
                    BlockState blockState = b.getBlock().defaultBlockState();
                    double dps = Math.abs(Static.rad_to_degree(myMechanicalBlock.internalVelocity));
                    int o = 100;
                    int n = (int) Math.min(7, dps / o);
                    if (level.random.nextFloat() * o < (int) dps % o) n += 1;
                    for (int i = 0; i < n; i++) { // Spawn multiple particles for better effect
                        double x = getBlockPos().getX() + 0.5 + (Math.random() - 0.5) * 0.5;
                        double y = getBlockPos().getY() + 0.5;
                        double z = getBlockPos().getZ() + 0.5 + (Math.random() - 0.5) * 0.5;

                        level.addParticle(
                                new DustParticleOptions(Vec3.fromRGB24(b.getBlock().defaultMapColor().col).toVector3f(), 0.4f),
                                x, y, z,
                                0.0, 0.0, 0.0 // Velocity: this shit does not work
                        );
                    }
                }
            } else currentProgress = 0;
        }

        if (!level.isClientSide) {
            // timeout workers
            for (Entity e : workersWorkingHereWithTimeout.keySet()) {
                int ticks = workersWorkingHereWithTimeout.get(e);
                workersWorkingHereWithTimeout.put(e, ticks + 1);
                if (ticks > 100) {
                    workersWorkingHereWithTimeout.remove(e);
                    break;
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySieve) t).tick();
    }


    static List<CrankShaftType> allowedCrankshaftTypes = new ArrayList();

    static {
        allowedCrankshaftTypes.add(CrankShaftType.SMALL);
    }

    @Override
    public List<CrankShaftType> getConnectableCrankshafts() {
        return allowedCrankshaftTypes;
    }
}
