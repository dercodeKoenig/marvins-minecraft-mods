package advRocketry.Rocket;

import advRocketry.BlockEntities.EntityPressureTank;
import advRocketry.Blocks.PressureTank;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.RocketTravelDimension;
import advRocketry.Dimension.SpaceStationDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class PressureTankUtils {

    static boolean transfer(FluidTank t1, FluidTank t2, int amount) {
        FluidStack canExtract = t1.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        int canFill = t2.fill(canExtract, IFluidHandler.FluidAction.SIMULATE);
        if (canFill > 0) {
            t2.fill(t1.drain(canFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
            return true;
        }
        return false;
    }

    static boolean maybeTransferFluids(EntityPressureTank current, EntityPressureTank target, Dimension dimension) {
        // in space, equalize fluid level
        if (dimension instanceof SpaceStationDimension spaceStation) {
            // equalize fluid levels
            int c = current.tank.getFluidAmount();
            int t = target.tank.getFluidAmount();
            if (c > t + 1) {
                int toTransfer = (c - t) / 2;
                return transfer(current.tank, target.tank, toTransfer);
            }
        } else {
            // transfer everything to the lower tank
            if (target.getBlockPos().getY() < current.getBlockPos().getY()) {
                return transfer(current.tank, target.tank, current.tank.getFluidAmount());
            }
        }
        return false;
    }

    public static void tick(EntityRocket rocket) {
        Dimension dim = DimensionManager.getDimensionManager(rocket.level().isClientSide).get(rocket.level().dimension().location());

        for (BlockPos p : rocket.getPressureTankPositions()) {
            BlockState state = rocket.blocks.get(p);
            BlockEntity be = rocket.blockEntities.get(p);
            if (be instanceof EntityPressureTank pressureTank && state.getBlock() instanceof PressureTank) {

                // adjust render mode
                if (dim instanceof SpaceStationDimension)
                    pressureTank.renderMode = 1;
                else if (dim instanceof RocketTravelDimension)
                    pressureTank.renderMode = 1;
                else
                    pressureTank.renderMode = 0;

                if (state.getValue(PressureTank.connectedBelow)) {
                    BlockPos belowPos = p.below();
                    BlockEntity other = rocket.blockEntities.get(belowPos);
                    if (other instanceof EntityPressureTank otherTank) {
                        if (!rocket.level().isClientSide) {
                            if (maybeTransferFluids(pressureTank, otherTank, dim)) {
                                onChanged(p, rocket);
                                onChanged(belowPos, rocket);
                            }
                        } else {
                            if (FluidStack.isSameFluidSameComponents(otherTank.tank.getFluid(), pressureTank.tank.getFluid())) {
                                pressureTank.renderBottomFace = false;
                            }else{
                                pressureTank.renderBottomFace = true;
                            }
                        }
                    }
                }
                if (state.getValue(PressureTank.connectedAbove)) {
                    BlockPos abovePos = p.above();
                    BlockEntity other = rocket.blockEntities.get(abovePos);
                    if (other instanceof EntityPressureTank otherTank) {
                        if (!rocket.level().isClientSide) {
                            if (maybeTransferFluids(pressureTank, otherTank, dim)) {
                                onChanged(p, rocket);
                                onChanged(abovePos, rocket);
                            }
                        } else {
                            if (FluidStack.isSameFluidSameComponents(otherTank.tank.getFluid(), pressureTank.tank.getFluid())) {
                                pressureTank.renderTopFace = false;
                            }else{
                                pressureTank.renderTopFace = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void onChanged(BlockPos position, EntityRocket rocket) {
        BlockEntity blockEntity = rocket.blockEntities.get(position);
        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("blockPos", NbtUtils.writeBlockPos(position));
        blockEntityTag.put("blockEntity", blockEntity.saveCustomOnly(rocket.registryAccess()));
        CompoundTag info = new CompoundTag();
        info.put("updatePressureTank", blockEntityTag);
        rocket.sendToClients(info);
    }

    public static void readClient(CompoundTag compoundTag, EntityRocket rocket) {
        if (compoundTag.contains("updatePressureTank")) {
            CompoundTag blockEntityTag = compoundTag.getCompound("updatePressureTank");
            BlockPos p = NbtUtils.readBlockPos(blockEntityTag, "blockPos").get();
            BlockState state = rocket.blocks.get(p);
            CompoundTag entityData = blockEntityTag.getCompound("blockEntity");
            BlockEntity existingBlockEntity = rocket.blockEntities.get(p);
            if (state != null) {
                if (existingBlockEntity != null && existingBlockEntity.isValidBlockState(state))
                    existingBlockEntity.loadCustomOnly(entityData, rocket.registryAccess());
            }
        }
    }
}
