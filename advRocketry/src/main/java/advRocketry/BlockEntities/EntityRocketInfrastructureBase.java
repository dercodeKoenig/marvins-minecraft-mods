package advRocketry.BlockEntities;

import advRocketry.Items.ItemLinker;
import advRocketry.Registry;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EntityRocketInfrastructureBase extends BlockEntity implements ItemLinker.linkable, ItemLinker.linkableToEntity{

    public float maxDistance = 30;
    public BlockPos linkedAssemblerPos = null;
    public EntityRocket linkedRocket = null;

    public EntityRocketInfrastructureBase(BlockEntityType type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedAssemblerPos != null)
            tag.put("linkedAssemblerPos", NbtUtils.writeBlockPos(linkedAssemblerPos));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("linkedAssemblerPos"))
            linkedAssemblerPos = NbtUtils.readBlockPos(tag, "linkedAssemblerPos").get();
    }

    public void serverTick() {
        if (linkedAssemblerPos != null) {
            linkedRocket = null;
            if (level.getBlockEntity(linkedAssemblerPos) instanceof EntityRocketAssembler assembler) {
                linkedRocket = assembler.currentRocket;
            } else
                linkedAssemblerPos = null;
        }
        if (linkedRocket != null && linkedRocket.isRemoved()) {
            linkedRocket = null;
        }
        if (linkedRocket != null && linkedRocket.position().distanceTo(getBlockPos().getCenter()) >= maxDistance){
            linkedRocket = null;
        }
    }

    @Override
    public boolean link(BlockPos otherpos, Level otherLevel) {
        if (otherLevel == level) {
            Block otherBlock = level.getBlockState(otherpos).getBlock();
            if (otherBlock.equals(Registry.ROCKET_ASSEMBLER.get())) {
                if (otherpos.getCenter().distanceTo(getBlockPos().getCenter()) < maxDistance) {
                    linkedAssemblerPos = otherpos;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean link(Entity e) {
        if (e instanceof EntityRocket rocket) {
            if (rocket.position().distanceTo(getBlockPos().getCenter()) < maxDistance) {
                if (rocket.level().equals(level)) {
                    linkedRocket = rocket;
                    linkedAssemblerPos = null;
                    return true;
                }
            }
        }
        return false;
    }
}
