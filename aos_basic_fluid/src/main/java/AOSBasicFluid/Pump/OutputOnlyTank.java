package AOSBasicFluid.Pump;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class OutputOnlyTank extends FluidTank {
    public OutputOnlyTank(int capacity) {
        super(capacity);
    }

    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return 0;
    }

    public int _fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return super.fill(resource, action);
    }
}
