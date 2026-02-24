package AOSWorkshopExpansion.WoodMill;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.*;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static AOSWorkshopExpansion.Registry.*;
import static AgeOfSteam.Registry.CASING_SLAB;

public class EntityWoodMill extends EntityMultiblockMaster implements IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {


    public VertexBuffer vertexBuffer_saw;
    public MeshData mesh_saw;
    public VertexBuffer vertexBuffer_arm;
    public MeshData mesh_arm;
    public int lastLight;

    // aw npc compat
    public static Set<BlockIdentifier> knownBlockEntities = new HashSet<>();
    public HashMap<Entity, Integer> workersWorkingHereWithTimeout = new HashMap<>();


    public static class workingRecipe {
        ItemStack currentInput;
        double progress = 0;
        double additionalResistance = 0;
        List<ItemStack> outputStacks = new ArrayList<>();
    }

    public List<workingRecipe> currentWorkingRecipes = new ArrayList<>();

    double myFriction = 1;
    double timeRequired = 50;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return WoodMillConfig.INSTANCE.maxStress;
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
            return 0;
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

    public static Object[][][] structure = {
            {{'C'}, {'c'}, {'C'}}
    };
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        List<Block> c = new ArrayList<>();
        c.add(WOODMILL.get());
        charMapping.put('c', c);

        List<Block> C = new ArrayList<>();
        C.add(CASING_SLAB.get());
        charMapping.put('C', C);
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    @Override
    public void onStructureComplete() {
        if (level.isClientSide)
            // this is executed before minecraft updates the blockstate on client
            // but resetRotation (to make it sync to the rotation) checks for connected mechanical blocks and it only connects to other mechanical blocks when the multiblock is formed
            // so i update it directly here
            level.setBlock(getBlockPos(), getBlockState().setValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED, true), 3);
        myMechanicalBlock.mechanicalOnload();
    }

    @Override
    public void onLoad() {
        if (level.isClientSide) {
            CompoundTag myOnloadTag = new CompoundTag();
            myOnloadTag.putUUID("ClientWoodMillOnload", Minecraft.getInstance().player.getUUID());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, myOnloadTag));
        }
        if (!level.isClientSide) {
            knownBlockEntities.add(new BlockIdentifier(level, getBlockPos()));
        }
        super.onLoad();
    }

    @Override
    public void setRemoved() {
        if (!level.isClientSide) {
            knownBlockEntities.remove(new BlockIdentifier(level, getBlockPos()));
        }
        super.setRemoved();
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
        if (tag.contains("inputs")) {
            currentWorkingRecipes.clear();
            ListTag inputs = tag.getList("inputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < inputs.size(); i++) {
                workingRecipe r = new workingRecipe();
                r.progress = inputs.getCompound(i).getDouble("progress");
                r.currentInput = ItemStack.parse(level.registryAccess(), inputs.getCompound(i).getCompound("input")).get();
                currentWorkingRecipes.add(r);
            }
        }
        super.readClient(tag);
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(tag, p);
        if (tag.contains("ping")) {
            CompoundTag info = getClientSyncUpdateTag();
            PacketDistributor.sendToPlayer(p, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        super.readServer(tag, p);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        if (!getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) return null;
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockWoodMill) {
            if (side == Direction.DOWN) {
                BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
                if (t instanceof EntityCrankShaftBase cs && cs.myType == CrankShaftType.LARGE) {
                    if (cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()) {
                        return myMechanicalBlock;
                    }
                }
            }
        }
        return null;
    }

    public EntityWoodMill(BlockPos pos, BlockState blockState) {
        super(ENTITY_WOODMILL.get(), pos, blockState);
        super.forwardInteractionToMaster = true;

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer_arm = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_saw = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                VertexBufferCleaner.register(this,vertexBuffer_arm);
                VertexBufferCleaner.register(this,vertexBuffer_saw);
            });
        }
    }

    CompoundTag getClientSyncUpdateTag() {
        CompoundTag info = new CompoundTag();
        ListTag inputs = new ListTag();
        for (workingRecipe i : currentWorkingRecipes) {
            CompoundTag t = new CompoundTag();
            t.putDouble("progress", i.progress);
            t.put("input", i.currentInput.save(level.registryAccess()));
            inputs.add(t);
        }
        info.put("inputs", inputs);

        return info;
    }

    void broadcastChangeOfInventoryAndSetChanged() {
        if (!level.isClientSide) {
            CompoundTag info = getClientSyncUpdateTag();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
            setChanged();
        }
    }


    public static WoodMillConfig.WoodMillRecipe getRecipeForInputs(ItemStack inputs) {
        for (WoodMillConfig.WoodMillRecipe i : WoodMillConfig.INSTANCE.recipes) {
            RecipePart input = i.inputItem;
            if (ItemUtils.matches(input.id, inputs)) {
                return i;
            }
        }
        return null;
    }

    void removeCurrentInputStacks() {
        for (workingRecipe i : currentWorkingRecipes) {
            BlockPos op = getBlockPos();
            ItemEntity ie = new ItemEntity(level, op.getX(), op.getY() + 3, op.getZ(), i.currentInput);
            level.addFreshEntity(ie);
        }
        currentWorkingRecipes.clear();
        broadcastChangeOfInventoryAndSetChanged();
    }

    void completeRecipe(workingRecipe r) {
        for (ItemStack output : r.outputStacks) {
            if (!output.isEmpty()) {
                Vec3 op = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()).getCenter();
                ItemEntity ie = new ItemEntity(level, op.x, op.y, op.z, output);
                float speed = 0.1f;
                ie.setDeltaMovement(-getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getStepX() * speed, speed * 2, -getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getStepZ() * speed);
                level.addFreshEntity(ie);
            }
        }
    }

    public boolean canFitInput() {
        boolean canFitInputs = true;
        for (workingRecipe i : currentWorkingRecipes)
            if (i.progress / timeRequired < 0.65) {
                canFitInputs = false;
                break;
            }

        return canFitInputs;
    }

    public boolean tryAddInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!canFitInput()) return false;

        WoodMillConfig.WoodMillRecipe r = getRecipeForInputs(stack);
        if (r != null) {
            if (!level.isClientSide) {
                workingRecipe w = new workingRecipe();
                w.currentInput = stack.copyWithCount(1);
                for (RecipePartWithProbability i : r.outputItems) {
                    i.computeRandomAmount();
                    w.outputStacks.add(ItemUtils.getItemStackFromIdOrTag(i.id, i.getRandomAmount(), level.registryAccess()));
                }
                w.additionalResistance = r.additionalResistance;
                stack.shrink(1);
                currentWorkingRecipes.add(w);
                broadcastChangeOfInventoryAndSetChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown()) {
            if (tryAddInput(player.getMainHandItem()))
                return InteractionResult.SUCCESS;
            else
                return InteractionResult.SUCCESS_NO_ITEM_USED;
        }
        return InteractionResult.PASS;
    }

    public workingRecipe getRecipeAtSawblade() {
        for (workingRecipe i : currentWorkingRecipes) {
            if (i.progress / timeRequired > 0.165 && i.progress / timeRequired < 0.72)
                return i;
        }
        return null;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            double progressMade = Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS)) * WoodMillConfig.INSTANCE.speedMultiplier;

            if (!level.isClientSide) {
                List<workingRecipe> toRemove = new ArrayList<>();
                for (workingRecipe i : currentWorkingRecipes) {
                    i.progress += progressMade;
                    if (i.progress >= timeRequired) {
                        completeRecipe(i);
                        toRemove.add(i);
                    }
                }
                if (!toRemove.isEmpty()) {
                    currentWorkingRecipes.removeAll(toRemove);
                    broadcastChangeOfInventoryAndSetChanged();
                }
                workingRecipe r = getRecipeAtSawblade();
                if (r != null) {
                    myFriction = WoodMillConfig.INSTANCE.baseResistance + r.additionalResistance;
                } else {
                    myFriction = WoodMillConfig.INSTANCE.baseResistance;
                }


                BlockPos ip = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));

                double minX = ip.getX();
                double minY = ip.getY() + 0.25;
                double minZ = ip.getZ();
                double maxX = minX + 1;
                double maxY = minY + 1;
                double maxZ = minZ + 1;

                // Get all entities of class ItemEntity within the bounding box
                List<ItemEntity> itemEntities = level.getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(minX, minY, minZ, maxX, maxY, maxZ)
                );
                for (ItemEntity i : itemEntities) {
                    tryAddInput(i.getItem());
                    i.setExtendedLifetime();
                }

            } else {
                for (workingRecipe i : currentWorkingRecipes) {
                    i.progress += progressMade;
                    if (i.progress >= timeRequired) {

                    }
                }
            }

            workingRecipe recipeAtSawBlade = getRecipeAtSawblade();
            if (progressMade > 0.0001 && recipeAtSawBlade != null) {
                //if(level.random.nextFloat() < progressMade) {
                //if (level.getGameTime() % 5 == 0) {
                float maxOffset = 0.25f;
                float maxSpeedForNoOffset = 0.2f;
                float offset = (float) (maxOffset - (progressMade / maxSpeedForNoOffset) * maxOffset);


                double rotationScaled = ((Math.abs(myMechanicalBlock.currentRotation)) % 360) / 360; // just making sure it is positive
                //System.out.println(rotationScaled+":"+progressMade);
                if ((rotationScaled > offset && rotationScaled - progressMade < offset) || (rotationScaled > offset + 0.5 && rotationScaled - progressMade < offset + 0.5)) {
                    level.playSound(null, getBlockPos(), SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 0.2f, 0.5f);
                }

            }
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
        ((EntityWoodMill) t).tick();
    }


    static List<CrankShaftType> allowedCrankshaftTypes = new ArrayList();

    static {
        allowedCrankshaftTypes.add(CrankShaftType.LARGE);
    }

    @Override
    public List<CrankShaftType> getConnectableCrankshafts() {
        return allowedCrankshaftTypes;
    }
}
