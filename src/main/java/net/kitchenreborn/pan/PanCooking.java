package net.kitchenreborn.pan;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenRecipes;

/** What the frying pan can cook: the {@code kitchen:pan} recipes. Recipes only exist on the server. */
public final class PanCooking {
	/** The result of frying one of {@code input}, or empty if the pan cannot cook it. */
	public static Optional<ItemStack> result(Level level, ItemStack input) {
		if (!(level instanceof ServerLevel server) || input.isEmpty()) {
			return Optional.empty();
		}
		SingleRecipeInput recipeInput = new SingleRecipeInput(input.copyWithCount(1));
		return server.recipeAccess().getRecipeFor(KitchenRecipes.PAN, recipeInput, server)
				.map(holder -> holder.value().assemble(recipeInput));
	}

	/** Jam comes out as a filled jar and needs an empty jar to be taken; everything else comes out into your hand. */
	public static boolean isJam(ItemStack result) {
		return result.has(KitchenComponents.JAM);
	}

	/** Fill stage 0 to 3 for a cook time out of {@code cookTicks}: thirds, like the original's 100, 200, 300 of 300. */
	public static int stage(int cookTime, int cookTicks) {
		return Math.min(3, cookTime * 3 / Math.max(1, cookTicks));
	}

	private PanCooking() { }
}
