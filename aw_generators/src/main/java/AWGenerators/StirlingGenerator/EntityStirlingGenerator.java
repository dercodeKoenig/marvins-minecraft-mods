package AWGenerators.StirlingGenerator;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityItemStackHandler;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

import static AWGenerators.Registry.ENTITY_STIRLING_GENERATOR;


public class EntityStirlingGenerator extends BlockEntity implements INetworkTagReceiver, IMechanicalBlockProvider {


    public VertexBuffer vertexBuffer_flywheel;
    public MeshData mesh_flywheel;
    public VertexBuffer vertexBuffer_flywheel_arm;
    public MeshData mesh_flywheel_arm;
    public VertexBuffer vertexBuffer_piston_arm1;
    public MeshData mesh_piston_arm1;
    public VertexBuffer vertexBuffer_piston_arm2;
    public MeshData mesh_piston_arm2;
    public VertexBuffer vertexBuffer_piston_crank1;
    public MeshData mesh_piston_crank1;
    public VertexBuffer vertexBuffer_piston_crank2;
    public MeshData mesh_piston_crank2;
    int lastLight;


    public  double maxForceMultiplier = Config.INSTANCE.stirlingGenerator_maxForceMultiplier;
    public  double k = Config.INSTANCE.stirlingGenerator_k;
    public  double burnTimeMultiplier = Config.INSTANCE.stirlingGenerator_burnTimeMultiplier;

    public  double myFriction = Config.INSTANCE.stirlingGenerator_friction;
    public  double myInertia = Config.INSTANCE.stirlingGenerator_inertia;
    public  double maxStress = Config.INSTANCE.stirlingGenerator_maxStress;

    public double myForce = 0;

    public GuiHandlerBlockEntity guiHandler;
    public BlockEntityItemStackHandler inventory;

    public int currentBurnTime;

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

    public EntityStirlingGenerator(BlockPos pos, BlockState blockState) {
        super(ENTITY_STIRLING_GENERATOR.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);
        inventory = new BlockEntityItemStackHandler(1, this) {
            public boolean isItemValid(int slot, ItemStack stack) {
                if (stack.getItem().getBurnTime(stack, null) > 0) {
                    return true;
                }
                return false;
            }
        };

        guiModuleItemHandlerSlot s1 = new guiModuleItemHandlerSlot(0, inventory, 0, 1, 0, guiHandler, 70, 10);
        guiHandler.getModules().add(s1);
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 120, 200, 0, 1, guiHandler)) {
            guiHandler.getModules().add(i);
        }
        for (GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 50, 100, 0, 1, guiHandler)) {
            guiHandler.getModules().add(i);
        }


        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer_flywheel = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_flywheel_arm = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_piston_arm1 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_piston_arm2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_piston_crank1 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer_piston_crank2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    public void openGui(){
        if(level.isClientSide) {
            guiHandler.openGui(180, 150, true);
        }
    }

    CompoundTag getUpdateTag(){
        CompoundTag t = new CompoundTag();
        t.putInt("burnTime", currentBurnTime);
        return t;
    }

    @Override
    public void setRemoved(){
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer_flywheel.close();
                vertexBuffer_flywheel_arm.close();
                vertexBuffer_piston_arm1.close();
                vertexBuffer_piston_arm2.close();
                vertexBuffer_piston_crank1.close();
                vertexBuffer_piston_crank2.close();
            });
        }
        super.setRemoved();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
        if(level.isClientSide){
            CompoundTag onLoadRequest = new CompoundTag();
            onLoadRequest.put("ping", new CompoundTag());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this,onLoadRequest));
        }
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();
        currentBurnTime --;
        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (currentBurnTime <= 0) {
                Item currentBurnItem = inventory.extractItem(0, 1, false).getItem();
                currentBurnTime = (int) (burnTimeMultiplier * currentBurnItem.getBurnTime(new ItemStack(currentBurnItem), null));
                PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this,getUpdateTag()));
            }
            if (currentBurnTime > 0) {
                float directionMultiplier = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
                myForce = directionMultiplier * maxForceMultiplier - k * myMechanicalBlock.internalVelocity;
            } else {
                myForce = 0;
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityStirlingGenerator) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag,ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(compoundTag,p);
        guiHandler.readServer(compoundTag);

        if(compoundTag.contains("ping")){
                PacketDistributor.sendToPlayer(p,PacketBlockEntity.getBlockEntityPacket(this,getUpdateTag()));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
        guiHandler.readClient(compoundTag);

        if(compoundTag.contains("burnTime")){
            currentBurnTime = compoundTag.getInt("burnTime");
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        inventory.deserializeNBT(registries,tag.getCompound("inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
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