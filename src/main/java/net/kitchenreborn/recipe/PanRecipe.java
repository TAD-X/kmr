package net.kitchenreborn.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

import net.kitchenreborn.registry.KitchenRecipes;

/** {@code kitchen:pan}: one ingredient fried (or boiled into jam) in the frying pan. */
public class PanRecipe extends SingleItemRecipe {
	public static final MapCodec<PanRecipe> MAP_CODEC = SingleItemRecipe.simpleMapCodec(PanRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, PanRecipe> STREAM_CODEC =
			SingleItemRecipe.simpleStreamCodec(PanRecipe::new);
	public static final RecipeSerializer<PanRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public PanRecipe(Recipe.CommonInfo info, Ingredient input, ItemStackTemplate result) {
		super(info, input, result);
	}

	@Override
	public RecipeSerializer<PanRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<PanRecipe> getType() {
		return KitchenRecipes.PAN;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}
}
