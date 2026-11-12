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

/** {@code kitchen:oven}: one ingredient cooked into a result by the oven (the original's custom oven recipes). */
public class OvenRecipe extends SingleItemRecipe {
	public static final MapCodec<OvenRecipe> MAP_CODEC = SingleItemRecipe.simpleMapCodec(OvenRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, OvenRecipe> STREAM_CODEC =
			SingleItemRecipe.simpleStreamCodec(OvenRecipe::new);
	public static final RecipeSerializer<OvenRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public OvenRecipe(Recipe.CommonInfo info, Ingredient input, ItemStackTemplate result) {
		super(info, input, result);
	}

	@Override
	public RecipeSerializer<OvenRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public RecipeType<OvenRecipe> getType() {
		return KitchenRecipes.OVEN;
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
