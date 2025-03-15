package io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.filling;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public record FillAlchemistCauldronRecipe(Ingredient input, ItemStack returned,
                                          FluidStack result) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return returned.copy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return returned.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ALCHEMIST_CAULDRON_FILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<FillAlchemistCauldronRecipe> {
        public static final MapCodec<FillAlchemistCauldronRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Ingredient.CODEC.fieldOf("input").forGetter(FillAlchemistCauldronRecipe::input),
                ItemStack.CODEC.fieldOf("returned").forGetter(FillAlchemistCauldronRecipe::returned),
                FluidStack.CODEC.fieldOf("fluid").forGetter(FillAlchemistCauldronRecipe::result)
        ).apply(builder, FillAlchemistCauldronRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, FillAlchemistCauldronRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, FillAlchemistCauldronRecipe::input,
                ItemStack.STREAM_CODEC, FillAlchemistCauldronRecipe::returned,
                FluidStack.STREAM_CODEC, FillAlchemistCauldronRecipe::result,
                FillAlchemistCauldronRecipe::new
        );

        @Override
        public MapCodec<FillAlchemistCauldronRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FillAlchemistCauldronRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public record Builder(Ingredient input, ItemStack returned, FluidStack fluid) implements RecipeBuilder {

        public Builder(Item input, Item returned, Holder<Fluid> fluid) {
            this(Ingredient.of(input), new ItemStack(returned), new FluidStack(fluid, 250)); // 250 is standard bottle
        }

        @Override
        public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
            return this;
        }

        @Override
        public RecipeBuilder group(@Nullable String groupName) {
            return this;
        }

        @Override
        public Item getResult() {
            return null;
        }

        @Override
        public void save(RecipeOutput recipeOutput, ResourceLocation id) {
            recipeOutput.accept(id, new FillAlchemistCauldronRecipe(input, returned, fluid), null);
        }
    }
}
