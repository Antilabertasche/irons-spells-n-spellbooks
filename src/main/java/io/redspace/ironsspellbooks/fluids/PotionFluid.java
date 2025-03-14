package io.redspace.ironsspellbooks.fluids;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class PotionFluid extends NoopFluid {
    public PotionFluid(BaseFlowingFluid.Properties properties) {
        super(properties);
    }

    public static FluidStack of(int amount, PotionContents potionContents, PotionFluid.BottleType bottleType) {
        FluidStack fluidStack = new FluidStack(FluidRegistry.POTION_FLUID, amount);
        addPotionToFluidStack(fluidStack, potionContents);
        fluidStack.set(ComponentRegistry.POTION_BOTTLE_TYPE, bottleType);
        return fluidStack;
    }

    public static FluidStack addPotionToFluidStack(FluidStack fs, PotionContents potionContents) {
        if (potionContents == PotionContents.EMPTY) {
            fs.remove(DataComponents.POTION_CONTENTS);
            return fs;
        } else {
            fs.set(DataComponents.POTION_CONTENTS, potionContents);
            return fs;
        }
    }

    public enum BottleType implements StringRepresentable {
        REGULAR,
        SPLASH,
        LINGERING;

        public static final Codec<PotionFluid.BottleType> CODEC = StringRepresentable.fromEnum(PotionFluid.BottleType::values);
        public static final StreamCodec<ByteBuf, PotionFluid.BottleType> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        BottleType() {
        }

        public @NotNull String getSerializedName() {
            return this.toString().toLowerCase(Locale.US);
        }
    }
}
