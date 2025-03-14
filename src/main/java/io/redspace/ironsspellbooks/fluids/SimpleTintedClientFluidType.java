package io.redspace.ironsspellbooks.fluids;

import net.minecraft.resources.ResourceLocation;

public class SimpleTintedClientFluidType extends SimpleClientFluidType {
    final int color;

    public SimpleTintedClientFluidType(ResourceLocation texture, int color) {
        super(texture);
        this.color = color;
    }

    @Override
    public int getTintColor() {
        return color;
    }
}
