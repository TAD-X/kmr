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
import net.kitchenreborn.recipe.PanRecipe;

/** Datagen builder for {@code kitchen:pan} recipes. */
public class PanRecipeBuilder implements RecipeBuilder {
	private final Ingredient input;
	private final ItemStackTemplate result;
	private final String name;
	private final RecipeUnlockAdvancementBuilder advancement = new RecipeUnlockAdvancementBuilder();

	private PanRecipeBuilder(Ingredient input, ItemStackTemplate result, String name) {
		this.input = input;
		this.result = result;
		this.name = name;
	}

	public static PanRecipeBuilder pan(Ingredient input, ItemStackTemplate result, String name) {
		return new PanRecipeBuilder(input, result, name);
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
		return ResourceKey.create(Registries.RECIPE, Kitchen.id("pan/" + name));
	}

	@Override
	public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
		output.accept(id, new PanRecipe(new Recipe.CommonInfo(false), input, result),
				advancement.build(output, id, RecipeCategory.FOOD));
	}
}
