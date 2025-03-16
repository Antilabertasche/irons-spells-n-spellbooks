package io.redspace.ironsspellbooks.fluids;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public interface ICauldronColoredFluid {
    int getColor(FluidStack fluidStack);

    static ICauldronColoredFluid of(IClientFluidTypeExtensions clientFluid) {
        return clientFluid instanceof ICauldronColoredFluid cauldronColoredFluid ? cauldronColoredFluid : clientFluid::getTintColor;
    }
}
