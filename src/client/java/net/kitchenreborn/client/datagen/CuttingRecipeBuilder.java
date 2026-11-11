package net.kitchenreborn.client.datagen;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.recipe.CuttingRecipe;

/** Datagen builder for {@code kitchen:cutting} recipes. */
public class CuttingRecipeBuilder implements RecipeBuilder {
	private final Ingredient input;
	private final ItemStackTemplate result;
	private final String name;
	private final RecipeUnlockAdvancementBuilder advancement = new RecipeUnlockAdvancementBuilder();

	private CuttingRecipeBuilder(Ingredient input, ItemLike result, int count, String name) {
		this.input = input;
		this.result = new ItemStackTemplate(result.asItem(), count);
		this.name = name;
	}

	public static CuttingRecipeBuilder cutting(Ingredient input, ItemLike result, int count, String name) {
		return new CuttingRecipeBuilder(input, result, count, name);
	}

	@Override
	public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
		advancement.unlockedBy(name, criterion);
		return this;
	}

	@Override
	public RecipeBuilder group(String group) {
		return this;
	}

	@Override
	public ResourceKey<Recipe<?>> defaultId() {
		return ResourceKey.create(Registries.RECIPE, Kitchen.id("cutting/" + name));
	}

	@Override
	public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
		output.accept(id, new CuttingRecipe(new Recipe.CommonInfo(false), input, result),
				advancement.build(output, id, RecipeCategory.FOOD));
	}
}
