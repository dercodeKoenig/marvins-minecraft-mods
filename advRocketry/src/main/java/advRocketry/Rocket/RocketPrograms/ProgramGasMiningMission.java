package advRocketry.Rocket.RocketPrograms;

import advRocketry.API;
import advRocketry.BlockEntities.EntityPressureTank;
import advRocketry.Blocks.GasIntake;
import advRocketry.GlobalTime;
import advRocketry.Missions.RocketMission;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.GasRegistry;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.UUID;

public class ProgramGasMiningMission extends ProgramMissionStartBase {
    public String targetGas;
    public ResourceLocation targetLevelId;

    public ProgramGasMiningMission() {

    }

    public ProgramGasMiningMission(EntityRocket rocket, String targetGas, ResourceLocation targetLevelId, ResourceLocation returnLevel, BlockPos returnPos, UUID missionId) {
        super(rocket, returnLevel, returnPos, missionId);
        this.targetGas = targetGas;
        this.targetLevelId = targetLevelId;
    }

    public void startMission(EntityRocket rocket) {

        // calculate amount to mine
        // max amount is based on fuel and tank capacity
        // every liquid bucket will require some fuel to be collected or fuel consumption would be too low for such a task
        int intakeBlocks = 0;
        for (BlockState state : rocket.blocks.values()) {
            if (state.getBlock() instanceof GasIntake) {
                intakeBlocks++;
            }
        }

        // keep some fuel for the return home (in space we dont need much fuel)
        int fuel = rocket.getFuel();
        int requiredFuelForReturn = rocket.getFuelRateMax() * 20 * 10 + 1000;
        int availableFuel = fuel - requiredFuelForReturn;
        int mbPerFuel = 10; // 10 mb gas for every mb of fuel

        int totalFilled = 0;

        Fluid toMine = GasRegistry.gases.get(targetGas).fluid;
        if (intakeBlocks > 0 && toMine != Fluids.EMPTY) {
            for (BlockEntity be : rocket.blockEntities.values()) {
                if(availableFuel <= 0)
                    break;
                if (be instanceof EntityPressureTank pressureTank) {
                    FluidStack toFill = new FluidStack(toMine, availableFuel * mbPerFuel);
                    int filled = pressureTank.tank.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                    totalFilled += filled;
                    int fuelDrain = (int) Math.ceil((double) filled / mbPerFuel);
                    availableFuel -= fuelDrain;
                    rocket.fuelTank.drain(fuelDrain, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        // remove the gas from composition
        API.addGasInBuckets(ResourceLocation.parse(targetLevelId.toString()),targetGas, (double) -totalFilled / 1000);

        int time = 20 * 30;
        if (intakeBlocks > 0) {
            int extraTime = totalFilled * 20 / 1000 / 2; // 1s for 2 buckets
            extraTime /= intakeBlocks;
            time += extraTime;
        }
        RocketMission mission = new RocketMission();
        mission.startMission(rocket, GlobalTime.getGlobalTime() + time, missionId, returnLevel, returnPos);
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        super.readFromNbt(nbt);
        if (nbt.contains("targetGas"))
            targetGas = nbt.getString("targetGas");
        if (nbt.contains("targetLevelId"))
            targetLevelId = ResourceLocation.parse(nbt.getString("targetLevelId"));
    }

    @Override
    public CompoundTag saveToNbt() {
        CompoundTag tag = super.saveToNbt();
        if (targetGas != null)
            tag.putString("targetGas", targetGas);
        if(targetLevelId != null)
            tag.putString("targetLevelId", targetLevelId.toString());
        return tag;
    }
}
