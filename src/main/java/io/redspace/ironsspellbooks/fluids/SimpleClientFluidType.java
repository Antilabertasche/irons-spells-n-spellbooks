package io.redspace.ironsspellbooks.fluids;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class SimpleClientFluidType implements IClientFluidTypeExtensions {

    private final ResourceLocation texture;

    public SimpleClientFluidType(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    public ResourceLocation getStillTexture() {
        return texture;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return texture;
    }

    @Override
    public ResourceLocation getOverlayTexture() {
        return texture;
    }
}
