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

/**
 * {@code kitchen:cutting}: one ingredient cut into a result. The same entries also generate the shapeless
 * knife recipes for the crafting grid; the cutting board (M3) will read this type directly.
 */
public class CuttingRecipe extends SingleItemRecipe {
	public static final MapCodec<CuttingRecipe> MAP_CODEC = SingleItemRecipe.simpleMapCodec(CuttingRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, CuttingRecipe> STREAM_CODEC =
			SingleItemRecipe.simpleStreamCodec(CuttingRecipe::new);
	public static final RecipeSerializer<CuttingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public CuttingRecipe(Recipe.CommonInfo info, Ingredient input, ItemStackTemplate result) {
		super(info, input, result);
	}

	@Override
	public RecipeSerializer<CuttingRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<CuttingRecipe> getType() {
		return KitchenRecipes.CUTTING;
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
