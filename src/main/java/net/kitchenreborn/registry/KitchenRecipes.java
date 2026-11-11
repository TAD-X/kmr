package net.kitchenreborn.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.recipe.CuttingRecipe;

public final class KitchenRecipes {
	public static final RecipeType<CuttingRecipe> CUTTING = new RecipeType<>() {
		@Override
		public String toString() {
			return "kitchen:cutting";
		}
	};

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_TYPE, Kitchen.id("cutting"), CUTTING);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Kitchen.id("cutting"), CuttingRecipe.SERIALIZER);
	}

	private KitchenRecipes() { }
}
