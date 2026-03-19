package BetterPipes.Pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import static BetterPipes.Pipe.EntityPipe.*;

public class PipeConnection implements IFluidHandler {

    EntityPipe parent;
    Direction myDirection;
    FluidTank tank;
    FluidRenderData renderData;

    int lastInputFromInside;
    int lastInputFromOutside;
    boolean getsInputFromInside;
    boolean getsInputFromOutside;
    int lastOutputToOutside;
    int lastOutputToInside;
    boolean outputsToInside;
    boolean outputsToOutside;
    int ticksWithFluidInTank = 0;

    int lastFill;
    FluidStack last_tankFluid = FluidStack.EMPTY;
    boolean last_getsInputFromInside;
    boolean last_getsInputFromOutside;
    boolean last_outputsToInside;
    boolean last_outputsToOutside;

    public PipeConnection(EntityPipe parent, Direction myDirection, int tankCapacity) {
        this.myDirection = myDirection;
        tank = new FluidTank(tankCapacity) {
            @Override
            protected void onContentsChanged() {
                parent.setChanged();
            }
        };
        this.parent = parent;
        renderData = new FluidRenderData();
    }

    boolean isEnabled(BlockState parent) {
        return parent.getValue(BlockPipe.connections.get(myDirection)) == BlockPipe.ConnectionState.EXTRACTION || parent.getValue(BlockPipe.connections.get(myDirection)) == BlockPipe.ConnectionState.CONNECTED;
    }

    IFluidHandler neighborFluidHandler() {
        BlockPos neighborPos = parent.getBlockPos().relative(myDirection);
        return parent.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, myDirection.getOpposite());
    }

    public boolean needsSync() {
        boolean needsUpdate = false;

        // Check if the input from inside state has changed
        if (last_getsInputFromInside != getsInputFromInside) {
            needsUpdate = true;
            last_getsInputFromInside = getsInputFromInside;
        }

        // Check if the input from outside state has changed
        if (last_getsInputFromOutside != getsInputFromOutside) {
            needsUpdate = true;
            last_getsInputFromOutside = getsInputFromOutside;
        }

        // Check if the output to inside state has changed
        if (last_outputsToInside != outputsToInside) {
            needsUpdate = true;
            last_outputsToInside = outputsToInside;
        }

        // Check if the output to outside state has changed
        if (last_outputsToOutside != outputsToOutside) {
            needsUpdate = true;
            last_outputsToOutside = outputsToOutside;
        }


        if (!FluidStack.isSameFluidSameComponents(last_tankFluid, tank.getFluid()))
            needsUpdate = true;

        if (last_tankFluid.getAmount() != tank.getFluidAmount())
            needsUpdate = true;

        last_tankFluid = tank.getFluid().copy(); // Update the last known tank fluid

        return needsUpdate;
    }

    public void saveAdditional(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag myTag = getUpdateTag(registries);
        tank.writeToNBT(registries, myTag);
        tag.put(myDirection.getName(), myTag);
    }

    public void loadAdditional(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag myTag = tag.getCompound(myDirection.getName());
        tank.readFromNBT(registries, myTag);
        handleUpdateTag(myTag, registries);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("getsInputFromInside", getsInputFromInside);
        tag.putBoolean("getsInputFromOutside", getsInputFromOutside);
        tag.putBoolean("outputsToInside", outputsToInside);
        tag.putBoolean("outputsToOutside", outputsToOutside);
        if (!tank.isEmpty()) {
            tag.put("fluid", tank.getFluid().save(registries));
        }
        return tag;
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        getsInputFromInside = tag.getBoolean("getsInputFromInside");
        getsInputFromOutside = tag.getBoolean("getsInputFromOutside");
        outputsToInside = tag.getBoolean("outputsToInside");
        outputsToOutside = tag.getBoolean("outputsToOutside");
        FluidStack newFluid = FluidStack.EMPTY;
        if (tag.contains("fluid")) {
            newFluid = (FluidStack.parse(registries, tag.getCompound("fluid")).get());
        }
        if (!FluidStack.isSameFluidSameComponents(newFluid, tank.getFluid()) || tank.getFluidAmount() != newFluid.getAmount()) {
            parent.setRequiresMeshUpdate();
            if (neighborFluidHandler() instanceof PipeConnection p)
                p.parent.setRequiresMeshUpdate();
        }
        tank.setFluid(newFluid);
    }

    void update() {
        if (lastInputFromOutside < STATE_UPDATE_TICKS + 1)
            lastInputFromOutside++;
        else if (getsInputFromOutside)
            getsInputFromOutside = false;

        if (lastInputFromInside < STATE_UPDATE_TICKS + 1)
            lastInputFromInside++;
        else if (getsInputFromInside)
            getsInputFromInside = false;

        if (lastOutputToInside < STATE_UPDATE_TICKS + 1)
            lastOutputToInside++;
        else if (outputsToInside)
            outputsToInside = false;

        if (lastOutputToOutside < STATE_UPDATE_TICKS + 1)
            lastOutputToOutside++;
        else if (outputsToOutside)
            outputsToOutside = false;

        if (!tank.isEmpty() && ticksWithFluidInTank < FORCE_OUTPUT_AFTER_TICKS + 1)
            ticksWithFluidInTank++;
        else if (tank.isEmpty()) {
            ticksWithFluidInTank = 0;
        }
    }

    @Override
    public int getTanks() {
        return this.tank.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.tank.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.tank.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return this.tank.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return fill(resource, action, false);
    }

    public int fill(FluidStack resource, FluidAction action, boolean wasFromMaster) {
        int filled = this.tank.fill(resource, action);
        if (filled > 0 && action == FluidAction.EXECUTE) {
            if (wasFromMaster) {
                lastInputFromInside = 0;
                getsInputFromInside = true;
            } else {
                lastInputFromOutside = 0;
                getsInputFromOutside = true;
            }
        }
        return filled;
    }


    void drainUpdate(boolean wasFromMaster) {
        if (wasFromMaster) {
            outputsToInside = true;
            lastOutputToInside = 0;
        } else {
            outputsToOutside = true;
            lastOutputToOutside = 0;
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return drain(resource, action, false);
    }

    public FluidStack drain(FluidStack resource, FluidAction action, boolean wasFromMaster) {
        FluidStack drained = this.tank.drain(resource, action);
        if (!drained.isEmpty() && action == FluidAction.EXECUTE) {
            drainUpdate(wasFromMaster);
        }
        return drained;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return drain(maxDrain, action, false);
    }

    public FluidStack drain(int maxDrain, FluidAction action, boolean wasFromMaster) {
        FluidStack drained = this.tank.drain(maxDrain, action);
        if (!drained.isEmpty() && action == FluidAction.EXECUTE) {
            drainUpdate(wasFromMaster);
        }
        return drained;
    }
}