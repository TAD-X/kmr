package net.kitchenreborn.oven;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import net.kitchenreborn.registry.KitchenRecipes;

/**
 * What the oven can cook (design D7): its own {@code kitchen:oven} recipes first, then every smoker recipe, then
 * every furnace recipe that is a food. Recipes only exist on the server, so on a client nothing is "cookable".
 */
public final class OvenCooking {
	/** The result of cooking one of {@code input}, or empty if the oven cannot cook it. */
	public static Optional<ItemStack> result(Level level, ItemStack input) {
		if (!(level instanceof ServerLevel server) || input.isEmpty()) {
			return Optional.empty();
		}
		SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
		var own = server.recipeAccess().getRecipeFor(KitchenRecipes.OVEN, recipeInput, server);
		if (own.isPresent()) {
			return Optional.of(own.get().value().assemble(recipeInput));
		}
		var smoking = server.recipeAccess().getRecipeFor(RecipeType.SMOKING, recipeInput, server);
		if (smoking.isPresent()) {
			return Optional.of(smoking.get().value().assemble(recipeInput));
		}
		var smelting = server.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, server);
		if (smelting.isPresent() && smelting.get().value().category() == CookingBookCategory.FOOD) {
			return Optional.of(smelting.get().value().assemble(recipeInput));
		}
		return Optional.empty();
	}

	private OvenCooking() { }
}
