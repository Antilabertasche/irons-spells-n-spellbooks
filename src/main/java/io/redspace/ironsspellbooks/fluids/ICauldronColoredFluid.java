package io.redspace.ironsspellbooks.fluids;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public interface ICauldronColoredFluid {
    int getColor();

    static ICauldronColoredFluid of(IClientFluidTypeExtensions clientFluid) {
        return clientFluid instanceof ICauldronColoredFluid cauldronColoredFluid ? cauldronColoredFluid : clientFluid::getTintColor;
    }
}
