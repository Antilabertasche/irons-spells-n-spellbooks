package io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron;

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
/**
 * Recipe Type for taking liquids out of the cauldron (emptying cauldron)
 */
public record EmptyAlchemistCauldronRecipe(Ingredient input, ItemStack result,
                                           FluidStack fluid) implements Recipe<SingleRecipeInput> {
    public ItemStack result() {
        return result.copy();
    }

    @Override
    public FluidStack fluid() {
        return fluid.copy();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
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
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ALCHEMIST_CAULDRON_EMPTY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.ALCHEMIST_CAULDRON_EMPTY_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<EmptyAlchemistCauldronRecipe> {
        public static final MapCodec<EmptyAlchemistCauldronRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Ingredient.CODEC.fieldOf("input").forGetter(EmptyAlchemistCauldronRecipe::input),
                ItemStack.CODEC.fieldOf("result").forGetter(EmptyAlchemistCauldronRecipe::result),
                FluidStack.CODEC.fieldOf("fluid").forGetter(EmptyAlchemistCauldronRecipe::fluid)
        ).apply(builder, EmptyAlchemistCauldronRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmptyAlchemistCauldronRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, EmptyAlchemistCauldronRecipe::input,
                ItemStack.STREAM_CODEC, EmptyAlchemistCauldronRecipe::result,
                FluidStack.STREAM_CODEC, EmptyAlchemistCauldronRecipe::fluid,
                EmptyAlchemistCauldronRecipe::new
        );

        @Override
        public MapCodec<EmptyAlchemistCauldronRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EmptyAlchemistCauldronRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public record Builder(Ingredient input, ItemStack returned, FluidStack fluid) implements RecipeBuilder {

        public Builder(Item input, Item returned, Holder<Fluid> fluid, int amount) {
            this(Ingredient.of(input), new ItemStack(returned), new FluidStack(fluid, amount));
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
            recipeOutput.accept(id, new EmptyAlchemistCauldronRecipe(input, returned, fluid), null);
        }
    }
}
