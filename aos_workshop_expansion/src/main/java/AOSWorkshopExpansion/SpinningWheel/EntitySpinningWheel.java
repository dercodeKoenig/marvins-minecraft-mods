package AOSWorkshopExpansion.SpinningWheel;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleImage;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.utils.*;
import AgeOfSteam.Core.AbstractMechanicalBlock;
import AgeOfSteam.Core.IMechanicalBlockProvider;
import AgeOfSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

import static AOSWorkshopExpansion.Registry.ENTITY_SPINNING_WHEEL;

public class EntitySpinningWheel extends BlockEntity implements INetworkTagReceiver, IMechanicalBlockProvider {


    public VertexBuffer vertexBuffer;
    public MeshData mesh;
    public int lastLight;


    // aw npc compat
    public static Set<BlockIdentifier> knownBlockEntities = new HashSet<>();
    public HashMap<Entity, Integer> workersWorkingHereWithTimeout = new HashMap<>();


    public SpinningWheelConfig.SpinningWheelRecipe currentRecipe = null;
    public double currentProgress;

    public ItemStackHandler inventoryOutput;
    public ItemStackHandler inventoryInput;

    public GuiHandlerBlockEntity guiHandler;

    public int ticksRemainingForForce = 0;
    double myFriction = 1;
    double myForce = 0;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return 100;
        }

        @Override
        public double getInertia(Direction face) {
            return 10;
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
            if(receivingFace==null)return 1;
            return receivingFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1:-1;
        }
    };

    public EntitySpinningWheel(BlockPos pos, BlockState blockState) {
        super(ENTITY_SPINNING_WHEEL.get(), pos, blockState);

        inventoryInput = new ItemStackHandler(9){
            @Override
            public void onContentsChanged(int slot){
                EntitySpinningWheel.this.setChanged();
            }
        };
        inventoryOutput = new ItemStackHandler(9){
            @Override
            public void onContentsChanged(int slot){
                EntitySpinningWheel.this.setChanged();
            }
        };


        guiHandler = new GuiHandlerBlockEntity(this);
        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 130, 100, 1, 0, guiHandler)) {
            guiHandler.getModules().add(i);
        }
        for (guiModulePlayerInventorySlot i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 70, 200, 1, 0, guiHandler)) {
            guiHandler.getModules().add(i);
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                guiModuleItemHandlerSlot i1 = new guiModuleItemHandlerSlot(x + y * 3 + 10000, inventoryInput, x + y * 3, 0, 1, guiHandler, 20 + x * 20, 10 + y * 20);
                guiHandler.getModules().add(i1);
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                guiModuleItemHandlerSlot o1 = new guiModuleItemHandlerSlot(x + y * 3 + 10100, inventoryOutput, x + y * 3, 2, 1, guiHandler, 110 + x * 20, 10 + y * 20);
                guiHandler.getModules().add(o1);
            }
        }


        guiHandler.getModules().add(
                new guiModuleImage(guiHandler, 80, 30, 20, 18, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 16, 12)
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    @Override
    public void onLoad(){
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
        if (!level.isClientSide) {
            knownBlockEntities.add(new BlockIdentifier(level, getBlockPos()));
        }
    }
    @Override
    public void setRemoved(){
        if (!level.isClientSide) {
            knownBlockEntities.remove(new BlockIdentifier(level, getBlockPos()));
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
            });
        }
        super.setRemoved();
    }

    public void popInventory(){
        ItemStack stack;
        for (int i = 0; i < 4; i++) {
            stack = inventoryInput.getStackInSlot(i).copy();
            Block.popResource(level, getBlockPos(), stack);
            inventoryInput.setStackInSlot(i,ItemStack.EMPTY);

            stack = inventoryOutput.getStackInSlot(i).copy();
            Block.popResource(level, getBlockPos(), stack);
            inventoryOutput.setStackInSlot(i,ItemStack.EMPTY);
        }
        setChanged();
    }

    public void resetRecipe() {
        currentRecipe = null;
        currentProgress = 0;
    }

    public void scanFornewRecipe() {
        for (SpinningWheelConfig.SpinningWheelRecipe r : SpinningWheelConfig.INSTANCE.recipes) {
            if(InventoryUtils.hasInputs(List.of(inventoryInput), new ArrayList<>(), List.of(new RecipePart(r.inputItem.id,r.inputItem.amount)))){
                currentRecipe = r;
                break;
            }
        }
    }

    public boolean tryAddManualWork() {
        if (ticksRemainingForForce < 5 ) {
            ticksRemainingForForce += 5;
            return true;
        }
        return false;
    }
    public InteractionResult use(Player player) {
        if(player.isShiftKeyDown()) {
            if (tryAddManualWork()) {
                player.causeFoodExhaustion(0.2f);
            }
        }
        else{
            if(level.isClientSide)
                guiHandler.openGui(180,160 ,true);
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    public void completeCurrentRecipe() {
        for (int i = 0; i < currentRecipe.inputItem.amount; i++) {
            if (level.random.nextFloat() < currentRecipe.inputItem.p) {
                InventoryUtils.consumeElements(new ArrayList<>(), List.of(inventoryInput), currentRecipe.inputItem.id, 1, false);
            }
        }
        for (RecipePartWithProbability output : currentRecipe.outputItems) {
            output.computeRandomAmount();
            InventoryUtils.createElements(new ArrayList<>(), List.of(inventoryOutput), output.id, output.getRandomAmount(),level.registryAccess());
        }
        resetRecipe();
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();
        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (currentRecipe == null) {
                scanFornewRecipe();
            } else {
                if (InventoryUtils.hasInputs(List.of(inventoryInput), new ArrayList<>(), List.of(new RecipePart(currentRecipe.inputItem.id, currentRecipe.inputItem.amount)))) {
                    double progressMade = Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
                    currentProgress += progressMade;
                    if (currentProgress >= currentRecipe.timeRequired) {
                        if (InventoryUtils.canFitElements(List.of(inventoryOutput), new ArrayList<>(),new ArrayList<>(currentRecipe.outputItems),level.registryAccess())) {
                            completeCurrentRecipe();
                        }
                    }
                } else {
                    resetRecipe();
                }
            }
            if (currentRecipe == null) {
                myFriction = SpinningWheelConfig.INSTANCE.baseResistance;
            } else {
                myFriction = SpinningWheelConfig.INSTANCE.baseResistance + currentRecipe.additionalResistance;
            }

            if (ticksRemainingForForce > 0) {
                ticksRemainingForForce--;
                myForce = SpinningWheelConfig.INSTANCE.clickForce - SpinningWheelConfig.INSTANCE.k * myMechanicalBlock.internalVelocity;
            } else {
                myForce = 0;
                ticksRemainingForForce = 0;
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
        ((EntitySpinningWheel) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        myMechanicalBlock.mechanicalReadServer(compoundTag, p);
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
        guiHandler.readClient(compoundTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);

        inventoryOutput.deserializeNBT(registries,tag.getCompound("inventoryOutput"));
        inventoryInput.deserializeNBT(registries,tag.getCompound("inventoryInput"));
    }
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);

        tag.put("inventoryOutput", inventoryOutput.serializeNBT(registries)) ;
        tag.put("inventoryInput", inventoryInput.serializeNBT(registries)) ;
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        if(direction == getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()){
            return myMechanicalBlock;
        }
            return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }
}