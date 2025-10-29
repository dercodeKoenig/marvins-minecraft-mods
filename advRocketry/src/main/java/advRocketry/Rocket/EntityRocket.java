package advRocketry.Rocket;

import ARLib.gui.GuiHandlerEntity;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.network.PacketEntity;
import advRocketry.BlockEntities.EntityGuidanceComputer;
import advRocketry.BlockEntities.EntityRocketAssembler;
import advRocketry.Blocks.FuelTank;
import advRocketry.Blocks.RocketMotor;
import advRocketry.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class EntityRocket extends Entity implements INetworkTagReceiver {

    public Map<BlockPos, BlockState> blocks;
    public Map<BlockPos, BlockEntity> blockEntities;
    public Vec3i size;
    public ItemStack usedNavigationItem = ItemStack.EMPTY; // the current one used (guidance computer item can be overwritten in launch terminal)
    public FluidTank fuelTank = null;
    private float thrust = -1;

    public GuiHandlerEntity guiHandler;

    public EntityRocket(EntityType<?> entityType, Level level) {
        super(entityType, level);
        guiHandler = new GuiHandlerEntity(this);
        blocks = new HashMap<>();
        blockEntities = new HashMap<>();
        size = new Vec3i(1,1,1);
        fuelTank = new FluidTank(0);
    }

    public static EntityRocket create(Level level, Map<BlockPos, BlockState> blocks, Map<BlockPos, BlockEntity> blockEntities, Vec3i size) {
        EntityRocket rocket = new EntityRocket(Registry.ENTITY_ROCKET.get(), level);
        rocket.blockEntities = blockEntities;
        rocket.blocks = blocks;
        rocket.size = size;
        int fuelCapacity = 0;
        for (BlockState state : rocket.blocks.values()){
            if(state.getBlock() instanceof FuelTank fuelTank){
                fuelCapacity += fuelTank.getFuelCapacity();
            }
        }
        rocket.fuelTank = new FluidTank(fuelCapacity);
        rocket.makeGui();
        return rocket;
    }

    @Override
    public void onAddedToLevel() {
        if (level().isClientSide) {
            CompoundTag req = new CompoundTag();
            req.putInt("ping", 0);
            PacketDistributor.sendToServer(PacketEntity.getEntityPacket(this, req));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true; // Rocket cannot be damaged
    }
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    @Override
    public boolean isPickable() {return true;}


    @Override
    public AABB makeBoundingBox() {
        if(size == null)return super.makeBoundingBox(); // happens because minecraft calls makeBoundingBox in constructor before the size value is assigned
        return new AABB(
                position().x - (double) size.getX() / 2,
                position().y,
                position().z - (double) size.getZ() / 2,
                position().x + (double) size.getX() / 2,
                position().y + size.getY(),
                position().z + (double) size.getZ() / 2
        );
    }
    public void makeGui(){
        guiHandler.modules.clear();
        guiModuleFluidTankDisplay fuelDisplay = new guiModuleFluidTankDisplay(1,fuelTank,0,guiHandler,155,10);
        guiHandler.modules.add(fuelDisplay);
        for (BlockEntity i : blockEntities.values()){
            if(i instanceof EntityGuidanceComputer computer){
                guiModuleItemHandlerSlot chipSlot = new guiModuleItemHandlerSlot(0, computer.itemStackHandler, 0, 0, 1, guiHandler, 10,10);
                guiHandler.modules.add(chipSlot);
            }
        }

        guiModuleButton deconstructButton = new guiModuleButton(2,"deconstruct",guiHandler,30,10,60,20, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID,"textures/gui/gui_button_red.png"),64,20);
        guiHandler.modules.add(deconstructButton);

        for(GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerHotbarModules(10,170,1000,1,0,guiHandler)){
            guiHandler.modules.add(i);
        }
        for(GuiModuleBase i : guiModulePlayerInventorySlot.makePlayerInventoryModules(10,110,2000,1,0,guiHandler)){
            guiHandler.modules.add(i);
        }
    }

    public void openGui() {
        if (level().isClientSide) {
            //makeGui();
            guiHandler.openGui(180, 200, true);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        openGui();
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    public float getThrust(){
        if (thrust >= 0)return thrust;
        thrust = 0;
        for(BlockState state : blocks.values()){
            if(state.getBlock() instanceof RocketMotor motor){
                thrust+=motor.getThrust();
            }
        }
        return thrust;
    }
    public int getFuel(){
        return fuelTank.getFluidAmount();
    }
    public float getMass(){
        return 0;
    }
    public float getMaxAcceleration(){
        return 0;
    }


    @Override
    public void tick() {
        if (!level().isClientSide) {
            guiHandler.serverTick();
        }
        //setRot(getYRot(),getXRot()+0.5f);
        //setRot(getYRot()+0.5f,getXRot());
        //setPos(getX(),80,getZ());
    }

    public void deconstruct(){
        BlockPos minPos = blockPosition().subtract(new Vec3i(size.getX() / 2, 0, size.getZ() / 2));
        for (BlockPos pos : blocks.keySet()){
            BlockState state = blocks.get(pos);
            BlockPos target = new BlockPos(
                    minPos.getX() + pos.getX(),
                    minPos.getY() + pos.getY(),
                    minPos.getZ() + pos.getZ()
            );
            level().setBlock(target, state, 3);
            if(blockEntities.get(pos) != null){
                BlockEntity be =blockEntities.get(pos);
                CompoundTag tag = be.saveCustomOnly(level().registryAccess());
                level().getBlockEntity(target).loadCustomOnly(tag, level().registryAccess());
            }
        }
        kill();
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        CompoundTag sizeTag = compoundTag.getCompound("size");
        size = new Vec3i(sizeTag.getInt("x"), sizeTag.getInt("y"), sizeTag.getInt("z"));

        fuelTank.readFromNBT(level().registryAccess(),compoundTag.getCompound("fuelTank"));

        blocks = new HashMap<>();
        ListTag blockTags = compoundTag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockTags.size(); i++) {
            CompoundTag blockTag = blockTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = NbtUtils.readBlockState(level().registryAccess().lookupOrThrow(Registries.BLOCK), blockTag.getCompound("block"));
            blocks.put(p, state);
        }

        blockEntities = new HashMap<>();
        ListTag blockEntityTags = compoundTag.getList("blockEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockEntityTags.size(); i++) {
            CompoundTag blockTag = blockEntityTags.getCompound(i);
            BlockPos p = NbtUtils.readBlockPos(blockTag, "blockPos").get();
            BlockState state = blocks.get(p);
            BlockEntity be = ((EntityBlock)state.getBlock()).newBlockEntity(p,state);
            be.loadCustomOnly(blockTag.getCompound("blockEntity"), registryAccess());
            blockEntities.put(p, be);
        }

        makeGui();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        CompoundTag sizeTag = new CompoundTag();
        sizeTag.putInt("x", size.getX());
        sizeTag.putInt("y", size.getY());
        sizeTag.putInt("z", size.getZ());
        compoundTag.put("size", sizeTag);

        compoundTag.put("fuelTank", fuelTank.writeToNBT(level().registryAccess(),new CompoundTag()));

        ListTag blockTags = new ListTag(blocks.size());
        for (BlockPos i : blocks.keySet()) {
            BlockState state = blocks.get(i);
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockTag.put("block", NbtUtils.writeBlockState(state));
            blockTags.add(blockTag);
        }
        compoundTag.put("blocks", blockTags);

        ListTag blockEntityTags = new ListTag(blockEntities.size());
        for (BlockPos i : blockEntities.keySet()) {
            BlockEntity blockEntity = blockEntities.get(i);
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.put("blockPos", NbtUtils.writeBlockPos(i));
            blockEntityTag.put("blockEntity", blockEntity.saveCustomOnly(registryAccess()));
            blockEntityTags.add(blockEntityTag);
        }
        compoundTag.put("blockEntities", blockEntityTags);

    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("ping")) {
            CompoundTag additionalSaveData = new CompoundTag();
            addAdditionalSaveData(additionalSaveData);
            CompoundTag info = new CompoundTag();
            info.put("additionalSaveData", additionalSaveData);
            PacketDistributor.sendToPlayer(serverPlayer, PacketEntity.getEntityPacket(this, info));
        }

        if (compoundTag.contains("guiButtonClick")) {
            int id = compoundTag.getInt("guiButtonClick");
            if (id == 2) {
                deconstruct();
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
        if (compoundTag.contains("additionalSaveData"))
            readAdditionalSaveData(compoundTag.getCompound("additionalSaveData"));
    }
}
