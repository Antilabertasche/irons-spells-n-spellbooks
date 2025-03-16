package io.redspace.ironsspellbooks.datagen;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.BrewAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.concurrent.CompletableFuture;

public class IronRecipeProvider extends RecipeProvider {
    public IronRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        quadRingSalvageRecipe(recipeOutput, ItemRegistry.FIREWARD_RING.get(), Ingredient.of(ItemRegistry.CINDER_ESSENCE.get()));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.FROSTWARD_RING.get(), Ingredient.of(ItemRegistry.ICE_CRYSTAL.get()));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.POISONWARD_RING.get(), Ingredient.of(ItemRegistry.NATURE_RUNE.get()));
        quadRingSalvageRecipe(recipeOutput, ItemRegistry.COOLDOWN_RING.get(), Ingredient.of(Tags.Items.INGOTS_COPPER));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.CAST_TIME_RING.get(), Ingredient.of(Items.AMETHYST_SHARD));
        simpleNecklaceSalvageRecipe(recipeOutput, ItemRegistry.HEAVY_CHAIN.get(), Ingredient.of(Items.CHAIN), Ingredient.of(Items.CHAIN));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.EMERALD_STONEPLATE_RING.get(), Ingredient.of(Items.EXPERIENCE_BOTTLE));
        simpleNecklaceSalvageRecipe(recipeOutput, ItemRegistry.CONJURERS_TALISMAN.get(), Ingredient.of(Items.SKELETON_SKULL), Ingredient.of(Items.STRING));
        simpleNecklaceSalvageRecipe(recipeOutput, ItemRegistry.CONCENTRATION_AMULET.get(), Ingredient.of(ItemRegistry.MITHRIL_INGOT.get()), Ingredient.of(Items.CHAIN));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.AFFINITY_RING.get(), Ingredient.of(Items.BUCKET));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.EXPULSION_RING.get(), Ingredient.of(Items.WIND_CHARGE));
        simpleRingSalvageRecipe(recipeOutput, ItemRegistry.VISIBILITY_RING.get(), Ingredient.of(Items.SPYGLASS));

        cauldronBottledInteraction(recipeOutput, ItemRegistry.BLOOD_VIAL, FluidRegistry.BLOOD);
        cauldronBottledInteraction(recipeOutput, ItemRegistry.INK_COMMON, FluidRegistry.COMMON_INK);
        cauldronBottledInteraction(recipeOutput, ItemRegistry.INK_UNCOMMON, FluidRegistry.UNCOMMON_INK);
        cauldronBottledInteraction(recipeOutput, ItemRegistry.INK_RARE, FluidRegistry.RARE_INK);
        cauldronBottledInteraction(recipeOutput, ItemRegistry.INK_EPIC, FluidRegistry.EPIC_INK);
        cauldronBottledInteraction(recipeOutput, ItemRegistry.INK_LEGENDARY, FluidRegistry.LEGENDARY_INK);
        // fixme: modded buckets, even with water, wont work
        new FillAlchemistCauldronRecipe
                .Builder(Ingredient.of(Items.WATER_BUCKET), new ItemStack(Items.BUCKET), new FluidStack(Fluids.WATER, 1000), false)
                .save(recipeOutput, IronsSpellbooks.id("alchemist_cauldron/fill_water_bucket"));
        new EmptyAlchemistCauldronRecipe
                .Builder(Ingredient.of(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), new FluidStack(Fluids.WATER, 1000))
                .save(recipeOutput, IronsSpellbooks.id("alchemist_cauldron/empty_water_bucket"));

        // Upgrade common ink -> uncommon
        BrewAlchemistCauldronRecipe.builder()
                .withInput(FluidRegistry.COMMON_INK, 1000)
                .withReagent(Tags.Items.INGOTS_COPPER)
                .withResult(FluidRegistry.UNCOMMON_INK, 250)
                .save(recipeOutput);
        // Upgrade uncommon ink -> rare
        BrewAlchemistCauldronRecipe.builder()
                .withInput(FluidRegistry.UNCOMMON_INK, 1000)
                .withReagent(Tags.Items.INGOTS_IRON)
                .withResult(FluidRegistry.RARE_INK, 250)
                .save(recipeOutput);
        // Upgrade rare ink -> epic
        BrewAlchemistCauldronRecipe.builder()
                .withInput(FluidRegistry.RARE_INK, 1000)
                .withReagent(Tags.Items.INGOTS_GOLD)
                .withResult(FluidRegistry.EPIC_INK, 250)
                .save(recipeOutput);
        // Upgrade epic ink -> legendary
        BrewAlchemistCauldronRecipe.builder()
                .withInput(FluidRegistry.EPIC_INK, 1000)
                .withReagent(Tags.Items.GEMS_AMETHYST)
                .withResult(FluidRegistry.LEGENDARY_INK, 250)
                .save(recipeOutput);
    }

    /**
     * creates recipe for filling the cauldron via this item, and emptying the cauldron to this item, via a glass bottle
     */
    protected void cauldronBottledInteraction(RecipeOutput output, Holder<Item> item, Holder<Fluid> fluid) {
        cauldronTwoWayInteraction(output, item, Holder.direct(Items.GLASS_BOTTLE), fluid, 250);
    }

    /**
     * creates recipe for filling the cauldron via this item, and emptying the cauldron to this item
     */
    protected void cauldronTwoWayInteraction(RecipeOutput output, Holder<Item> item, Holder<Item> vessel, Holder<Fluid> fluid, int amount) {
        String name = item.unwrapKey().map(key -> key.location().getPath()).orElse("empty");
        new FillAlchemistCauldronRecipe
                .Builder(item.value(), vessel.value(), fluid, amount)
                .save(output, IronsSpellbooks.id("alchemist_cauldron/fill_" + name));
        new EmptyAlchemistCauldronRecipe
                .Builder(vessel.value(), item.value(), fluid, amount)
                .save(output, IronsSpellbooks.id("alchemist_cauldron/empty_" + name));
    }

    protected void simpleRingSalvageRecipe(RecipeOutput output, Item result, Ingredient modifier) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .define('M', modifier)
                .define('X', ItemRegistry.MITHRIL_SCRAP.get())
                .pattern("M ")
                .pattern(" X")
                .unlockedBy("mithril_scrap", has(ItemRegistry.MITHRIL_SCRAP.get()))
                .save(output);
    }

    protected void simpleNecklaceSalvageRecipe(RecipeOutput output, Item result, Ingredient modifier, Ingredient strap) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .define('M', modifier)
                .define('X', ItemRegistry.MITHRIL_SCRAP.get())
                .define('S', strap)
                .pattern(" S ")
                .pattern("SXS")
                .pattern(" M ")
                .unlockedBy("mithril_scrap", has(ItemRegistry.MITHRIL_SCRAP.get()))
                .save(output);
    }

    protected void quadRingSalvageRecipe(RecipeOutput output, Item result, Ingredient modifier) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .define('M', modifier)
                .define('X', ItemRegistry.MITHRIL_SCRAP.get())
                .pattern(" M ")
                .pattern("MXM")
                .pattern(" M ")
                .unlockedBy("mithril_scrap", has(ItemRegistry.MITHRIL_SCRAP.get()))
                .save(output);
    }

}
