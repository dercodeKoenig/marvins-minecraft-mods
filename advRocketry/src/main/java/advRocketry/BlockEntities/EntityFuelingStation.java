package advRocketry.BlockEntities;

import ARLib.blockentities.EntityFluidInputBlock;
import advRocketry.Items.ItemLinker;
import advRocketry.Registry;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import static advRocketry.Registry.ENTITY_FUELING_STATION;

public class EntityFuelingStation extends EntityFluidInputBlock implements ItemLinker.linkable, ItemLinker.linkableToEntity {

    public BlockPos linkedAssemblerPos = null;
    public EntityRocket linkedRocket = null;

    public static float maxDistance = 30;

    public EntityFuelingStation(BlockPos pos, BlockState blockState) {
        super(ENTITY_FUELING_STATION.get(), pos, blockState);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        super.readServer(compoundTag, serverPlayer);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        super.readClient(compoundTag);
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

    public void tick() {
        super.tick();
        if (linkedAssemblerPos != null) {
            linkedRocket = null;
            BlockEntity rocketAssembler = level.getBlockEntity(linkedAssemblerPos);
            if (rocketAssembler instanceof EntityRocketAssembler assembler) {
                if (!myTank.isEmpty()) {
                    EntityRocket currentRocket = assembler.currentRocket;
                    if (currentRocket != null && currentRocket.getCurrentProgram() == null) {
                        linkedRocket = currentRocket;
                    }
                }
            } else
                linkedAssemblerPos = null;
        }

        if (linkedRocket != null) {
            if (linkedRocket.getCurrentProgram() == null) {
                FluidStack available = myTank.drain(10, FluidAction.SIMULATE);
                int canFill = linkedRocket.fuelTank.fill(available, FluidAction.SIMULATE);
                FluidStack drained = myTank.drain(canFill, FluidAction.EXECUTE);
                linkedRocket.fuelTank.fill(drained, FluidAction.EXECUTE);
            }
        }

        if (linkedRocket != null) {
            if (linkedRocket.isRemoved() || linkedRocket.position().distanceTo(getBlockPos().getCenter()) >= maxDistance)
                linkedRocket = null;
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityFuelingStation) t).tick();
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
