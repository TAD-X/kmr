package net.kitchenreborn.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.recipe.CuttingRecipe;
import net.kitchenreborn.recipe.OvenRecipe;
import net.kitchenreborn.recipe.PanRecipe;

public final class KitchenRecipes {
	public static final RecipeType<CuttingRecipe> CUTTING = new RecipeType<>() {
		@Override
		public String toString() {
			return "kitchen:cutting";
		}
	};

	public static final RecipeType<OvenRecipe> OVEN = new RecipeType<>() {
		@Override
		public String toString() {
			return "kitchen:oven";
		}
	};

	public static final RecipeType<PanRecipe> PAN = new RecipeType<>() {
		@Override
		public String toString() {
			return "kitchen:pan";
		}
	};

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_TYPE, Kitchen.id("pan"), PAN);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Kitchen.id("pan"), PanRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.RECIPE_TYPE, Kitchen.id("oven"), OVEN);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Kitchen.id("oven"), OvenRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.RECIPE_TYPE, Kitchen.id("cutting"), CUTTING);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Kitchen.id("cutting"), CuttingRecipe.SERIALIZER);
	}

	private KitchenRecipes() { }
}
