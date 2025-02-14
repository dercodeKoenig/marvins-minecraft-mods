package AOSBasicFluid.Pump;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class PumpFluidTank extends FluidTank {
    public PumpFluidTank(int capacity) {
        super(capacity);
    }

    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return 0;
    }

    public int _fill(FluidStack resource, IFluidHandler.FluidAction action) {
        if (!resource.isEmpty() && this.isFluidValid(resource)) {
            if (action.simulate()) {
                if (this.fluid.isEmpty()) {
                    return Math.min(this.capacity, resource.getAmount());
                } else {
                    return !FluidStack.isSameFluidSameComponents(this.fluid, resource) ? 0 : Math.min(this.capacity - this.fluid.getAmount(), resource.getAmount());
                }
            } else if (this.fluid.isEmpty()) {
                this.fluid = resource.copyWithAmount(Math.min(this.capacity, resource.getAmount()));
                this.onContentsChanged();
                return this.fluid.getAmount();
            } else if (!FluidStack.isSameFluidSameComponents(this.fluid, resource)) {
                return 0;
            } else {
                int filled = this.capacity - this.fluid.getAmount();
                if (resource.getAmount() < filled) {
                    this.fluid.grow(resource.getAmount());
                    filled = resource.getAmount();
                } else {
                    this.fluid.setAmount(this.capacity);
                }

                if (filled > 0) {
                    this.onContentsChanged();
                }

                return filled;
            }
        } else {
            return 0;
        }
    }
}
