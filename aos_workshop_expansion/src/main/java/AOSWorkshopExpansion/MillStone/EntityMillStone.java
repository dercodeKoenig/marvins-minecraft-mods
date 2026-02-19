package AOSWorkshopExpansion.MillStone;

import ARLib.ARLibRegistry;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.holoProjector.itemHoloProjector;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockIdentifier;
import ARLib.utils.DimensionUtils;
import ARLib.utils.ItemUtils;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static AOSWorkshopExpansion.Registry.ENTITY_MILLSTONE;
import static AOSWorkshopExpansion.Registry.MILLSTONE;
import static AgeOfSteam.Registry.WOODEN_AXLE;

public class EntityMillStone extends EntityMultiblockMaster implements IMechanicalBlockProvider {


    public VertexBuffer vertexBufferPlate;
    public MeshData meshPlate;
    public VertexBuffer vertexBufferStone;
    public MeshData meshStone;
    public VertexBuffer vertexBufferAxle;
    public MeshData meshAxle;
    public int lastLight;

    public GuiHandlerBlockEntity guiHandler;

    // aw npc compat
    public static Set<BlockIdentifier> knownBlockEntities = new HashSet<>();
    public HashMap<Entity, Integer> workersWorkingHereWithTimeout = new HashMap<>();


    AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 900;
        }

        @Override
        public double getInertia(Direction direction) {
            return 1000;
        }

        @Override
        public double getTorqueResistance(Direction direction) {
            return MillStoneConfig.INSTANCE.resistance;
        }

        @Override
        public double getTorqueProduced(Direction direction) {
            return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@Nullable Direction direction) {
            return 1;
        }
    };

    public ItemStackHandler inventory = new ItemStackHandler(18) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public void onContentsChanged(int slot) {
            setChanged();
            sendUpdateTag(null);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            for (MillStoneConfig.MillStoneRecipe i : MillStoneConfig.INSTANCE.recipes) {
                if (ItemUtils.matches(i.inputItem.id, stack) || ItemUtils.matches(i.outputItem.id, stack))
                    return true;
            }
            return false;
        }

    };

    public EntityMillStone(BlockPos pos, BlockState blockState) {
        super(ENTITY_MILLSTONE.get(), pos, blockState);
        myMechanicalBlock.resetRotationAfterX = 360 * 4;
        super.forwardInteractionToMaster = true;

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBufferStone = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBufferPlate = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBufferAxle = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }

        guiHandler = new GuiHandlerBlockEntity(this);
        guiHandler.modules.addAll(
                ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 135, 1000, 0, 1, guiHandler)
        );
        guiHandler.modules.addAll(
                ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 75, 2000, 0, 1, guiHandler)
        );
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 2; y++) {
                int slotId = x + y * 9;
                int slotx = 10 + x * 18;
                int sloty = 10 + y * 18;
                guiHandler.modules.add(
                        new ARLib.gui.modules.guiModuleItemHandlerSlot(slotId, inventory, slotId, 1, 0, guiHandler, slotx, sloty)
                );
            }
        }
    }

    @Override
    public void onLoad() {
        if (level.isClientSide) {
            CompoundTag i = new CompoundTag();
            i.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, i));
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBufferAxle.close();
                vertexBufferStone.close();
                vertexBufferPlate.close();
            });
        }
        super.setRemoved();
    }

    public void placeStructurePreview() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        CompoundTag info = new CompoundTag();
        info.putString("dimension", DimensionUtils.getLevelId(level));
        info.putString("selectedMachine", "MillStone");
        info.putInt("y", 0);
        BlockPos offset = new BlockPos(0, 0, 0).relative(facing).relative(facing.getClockWise());
        info.putInt("posX", getBlockPos().getX() + offset.getX());
        info.putInt("posY", getBlockPos().getY());
        info.putInt("posZ", getBlockPos().getZ() + offset.getZ());
        info.putInt("stepX", facing.getStepX());
        info.putInt("stepZ", facing.getStepZ());
        ((itemHoloProjector) ARLibRegistry.ITEM_HOLOPROJECTOR.get()).placeLayer(info);
    }

    public void sendUpdateTag(@Nullable ServerPlayer target) {
        if (target == null) {
            if (level instanceof ServerLevel l) {
                PacketDistributor.sendToPlayersTrackingChunk(l, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
            }
        } else {
            PacketDistributor.sendToPlayer(target, PacketBlockEntity.getBlockEntityPacket(this, getUpdateTag()));
        }
    }

    public CompoundTag getUpdateTag() {
        CompoundTag info = new CompoundTag();
        info.put("inventory", inventory.serializeNBT(level.registryAccess()));
        return info;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer)
            guiHandler.signalOpenGui(serverPlayer, 190, 160, true);
        return InteractionResult.SUCCESS_NO_ITEM_USED;
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

    public void popInventory() {
        ItemStack stack;
        for (int i = 0; i < 18; i++) {
            stack = inventory.getStackInSlot(i).copy();
            Block.popResource(level, getBlockPos(), stack);
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
        sendUpdateTag(null);
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();
        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
                double directionMultiplier = 1;
                Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (facing == Direction.EAST || facing == Direction.NORTH)
                    directionMultiplier = -1;

                float directionOffset = 360;
                if (facing == Direction.NORTH) directionOffset += 90;
                if (facing == Direction.SOUTH) directionOffset += 270;
                if (facing == Direction.EAST) directionOffset += 360;
                if (facing == Direction.WEST) directionOffset += 180;

                for (int i = 0; i < 18; i++) {
                    double slotRotation = directionOffset + 20 * i - (float) (directionMultiplier * myMechanicalBlock.currentRotation * 0.25);
                    slotRotation = slotRotation % 360;
                    double lastSlotRotation = directionOffset + 20 * i - (float) ((directionMultiplier * (myMechanicalBlock.currentRotation - Static.rad_to_degree(myMechanicalBlock.internalVelocity) / Static.TPS)) * 0.25);
                    lastSlotRotation = lastSlotRotation % 360;

                    if (Math.abs(slotRotation - lastSlotRotation) > Math.abs(Static.rad_to_degree(myMechanicalBlock.internalVelocity) / Static.TPS) * 2) {
                        ItemStack stackInSlot = inventory.getStackInSlot(i);
                        if (!stackInSlot.isEmpty()) {
                            for (MillStoneConfig.MillStoneRecipe r : MillStoneConfig.INSTANCE.recipes) {
                                if (ItemUtils.matches(r.inputItem.id, stackInSlot)) {
                                    float rf = level.random.nextFloat();
                                    if (rf <= 1f / r.timeRequired) {
                                        ItemStack output = ItemUtils.getItemStackFromIdOrTag(r.outputItem.id, r.outputItem.amount * stackInSlot.getCount(), level.registryAccess());
                                        inventory.setStackInSlot(i, output);
                                        setChanged();
                                        sendUpdateTag(null);
                                    }
                                    break;
                                }
                            }
                        }

                    }
                }
            } else {
                if (level.getGameTime() % 51 == 0) {
                    super.scanStructure();
                }
            }
        }

        for (int i = 0; i < 1; i++) {
            SoundEvent[] sounds = {
                    SoundEvents.GRAVEL_BREAK,
                    SoundEvents.STONE_HIT
            };
            int randomIndex = level.random.nextInt(sounds.length);
            SoundEvent randomEvent = sounds[randomIndex];
            level.playSound(null, getBlockPos(), randomEvent,
                    SoundSource.BLOCKS, 0.02f * (float) ((Math.abs(myMechanicalBlock.internalVelocity))), 0.1f * (float) (Math.abs(myMechanicalBlock.internalVelocity)));  //
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
        ((EntityMillStone) t).tick();
    }

    // "c" is ALWAYS used for the controller/master block.
    public static Object[][][] structure = {
            {{'S', 'A', 'S'}, {'S', 'c', 'S'}, {'S', 'A', 'S'}}
    };
    public static boolean[][][] hideBlocks = {
            {{true, false, true}, {true, true, true}, {true, false, true}}
    };

    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        List<Block> c = new ArrayList<>();
        c.add(MILLSTONE.get());
        charMapping.put('c', c);

        List<Block> A = new ArrayList<>();
        A.add(WOODEN_AXLE.get());
        charMapping.put('A', A);

        List<Block> S = new ArrayList<>();
        S.add(Blocks.STONE_SLAB);
        charMapping.put('S', S);
    }

    public boolean[][][] hideBlocks() {
        return hideBlocks;
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
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        if (!getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) return null;
        if (direction.getAxis() == getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis())
            return myMechanicalBlock;
        else return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
        guiHandler.readClient(tag);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(level.registryAccess(), tag.getCompound("inventory"));
        }
        super.readClient(tag);
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(tag, p);
        guiHandler.readServer(tag);
        if (tag.contains("ping")) {
            sendUpdateTag(p);
        }
        super.readServer(tag, p);
    }
}
