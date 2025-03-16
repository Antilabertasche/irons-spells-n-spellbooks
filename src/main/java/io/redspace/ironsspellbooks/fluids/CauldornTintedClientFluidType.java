package io.redspace.ironsspellbooks.fluids;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

public class CauldornTintedClientFluidType extends SimpleClientFluidType implements ICauldronColoredFluid{
    final int color;

    public CauldornTintedClientFluidType(ResourceLocation texture, int color) {
        super(texture);
        this.color = color;
    }

    @Override
    public int getColor(FluidStack stack) {
        return color;
    }
}
