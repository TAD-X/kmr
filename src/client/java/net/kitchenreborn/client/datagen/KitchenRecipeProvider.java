package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.ItemLike;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.registry.KitchenItems;

public class KitchenRecipeProvider extends FabricRecipeProvider {
	public KitchenRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes,
			BootstrapContext<Advancement> advancements) {
		return new RecipeProvider(recipes, advancements) {
			@Override
			public void buildRecipes() {
				tools();
				board();
				cutting();
				mortarAndPestle();
				misc();
				cooking();
			}

			private void tools() {
				// Knife: iron ingot over stick, diagonal (the original's default shape).
				shaped(RecipeCategory.TOOLS, KitchenItems.KNIFE)
						.pattern("I ").pattern(" S")
						.define('I', Items.IRON_INGOT).define('S', Items.STICK)
						.unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT)).save(output);
				shaped(RecipeCategory.TOOLS, KitchenItems.MORTAR)
						.pattern("S S").pattern(" S ")
						.define('S', Items.STONE)
						.unlockedBy(getHasName(Items.STONE), has(Items.STONE)).save(output);
				shaped(RecipeCategory.TOOLS, KitchenItems.PESTLE)
						.pattern("S ").pattern(" S")
						.define('S', Items.STONE)
						.unlockedBy(getHasName(Items.STONE), has(Items.STONE)).save(output);
				shapeless(RecipeCategory.TOOLS, KitchenItems.MORTAR_AND_PESTLE)
						.requires(KitchenItems.MORTAR).requires(KitchenItems.PESTLE)
						.unlockedBy(getHasName(KitchenItems.MORTAR), has(KitchenItems.MORTAR)).save(output);
				shaped(RecipeCategory.TOOLS, KitchenItems.JAM_JAR)
						.pattern(" I ").pattern("G G").pattern("GGG")
						.define('I', Items.IRON_INGOT).define('G', Items.GLASS)
						.unlockedBy(getHasName(Items.GLASS), has(Items.GLASS)).save(output);
			}

			private void board() {
				// Original: wood slab, wood pressure plate, wood slab.
				shaped(RecipeCategory.DECORATIONS, KitchenItems.BOARD)
						.pattern("SPS")
						.define('S', ItemTags.WOODEN_SLABS).define('P', Items.OAK_PRESSURE_PLATE)
						.unlockedBy(getHasName(Items.OAK_PRESSURE_PLATE), has(Items.OAK_PRESSURE_PLATE)).save(output);
			}

			/** One entry produces both the board recipe (kitchen:cutting) and the grid knife recipe. */
			private void cut(ItemLike input, ItemLike result, int count) {
				String name = getItemName(result);
				CuttingRecipeBuilder.cutting(Ingredient.of(input), result, count, name)
						.unlockedBy(getHasName(KitchenItems.KNIFE), has(KitchenItems.KNIFE)).save(output);
				shapeless(RecipeCategory.FOOD, result, count)
						.requires(input).requires(KitchenItems.KNIFE)
						.unlockedBy(getHasName(KitchenItems.KNIFE), has(KitchenItems.KNIFE)).save(output);
			}

			private void cutting() {
				for (CuttingTable.Cut cut : CuttingTable.CUTS) {
					cut(cut.input(), cut.result(), cut.count());
				}
				// Two-input knife recipe (strawberry + sugar): grid only, not a single-ingredient board cut.
				shapeless(RecipeCategory.FOOD, KitchenItems.SUGARED_STRAWBERRY_SLICES, 2)
						.requires(KitchenItems.STRAWBERRY).requires(Items.SUGAR).requires(KitchenItems.KNIFE)
						.unlockedBy(getHasName(KitchenItems.KNIFE), has(KitchenItems.KNIFE))
						.save(output, "sugared_strawberry_slices_from_whole");
			}

			private void mortarAndPestle() {
				mp(Items.WHEAT, KitchenItems.FLOUR, 1, "flour_from_wheat");
				mp(Items.BREAD, KitchenItems.FLOUR, 3, "flour_from_bread");
				mp(Items.MILK_BUCKET, KitchenItems.BUTTER, 1, "butter");
				mp(KitchenItems.PEANUTS_IN_SHELL, KitchenItems.PEANUT, 2, "peanuts_from_shell");
			}

			private void mp(ItemLike input, ItemLike result, int count, String id) {
				shapeless(RecipeCategory.FOOD, result, count)
						.requires(KitchenItems.MORTAR_AND_PESTLE).requires(input)
						.unlockedBy(getHasName(KitchenItems.MORTAR_AND_PESTLE), has(KitchenItems.MORTAR_AND_PESTLE))
						.save(output, id);
			}

			private void misc() {
				shapeless(RecipeCategory.FOOD, KitchenItems.CHEESE, 2)
						.requires(Items.MILK_BUCKET)
						.unlockedBy(getHasName(Items.MILK_BUCKET), has(Items.MILK_BUCKET)).save(output);
				shapeless(RecipeCategory.FOOD, KitchenItems.SUGARED_STRAWBERRY_SLICES)
						.requires(KitchenItems.STRAWBERRY_SLICES).requires(Items.SUGAR)
						.unlockedBy(getHasName(KitchenItems.STRAWBERRY_SLICES), has(KitchenItems.STRAWBERRY_SLICES))
						.save(output, "sugared_strawberry_slices_from_slices");
			}

			private void cooking() {
				cook(KitchenItems.RAW_BACON, KitchenItems.BACON, 1);
				cook(KitchenItems.RAW_CHICKEN_FILLET, KitchenItems.CHICKEN_FILLET, 1);
				cook(KitchenItems.RAW_ROAST_BEEF, KitchenItems.ROAST_BEEF, 1);
				cook(KitchenItems.FLOUR, KitchenItems.TOAST, 2);
			}

			/** Furnace (200 ticks), smoker (100) and campfire (600), like vanilla food. */
			private void cook(Item input, Item result, int count) {
				ItemStackTemplate out = new ItemStackTemplate(result, count);
				Ingredient in = Ingredient.of(input);
				cookWith("smelting", "_from_smelting", new SmeltingRecipe(new Recipe.CommonInfo(true),
						new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.FOOD, ""), in, out, 0.35F, 200), input, result);
				cookWith("smoking", "_from_smoking", new SmokingRecipe(new Recipe.CommonInfo(true),
						new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.FOOD, ""), in, out, 0.35F, 100), input, result);
				cookWith("campfire_cooking", "_from_campfire_cooking", new CampfireCookingRecipe(new Recipe.CommonInfo(true),
						new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.FOOD, ""), in, out, 0.35F, 600), input, result);
			}

			private void cookWith(String kind, String suffix, Recipe<?> recipe, Item input, Item result) {
				ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Kitchen.id(getItemName(result) + suffix));
				RecipeUnlockAdvancementBuilder adv = new RecipeUnlockAdvancementBuilder();
				adv.unlockedBy(getHasName(input), has(input));
				output.accept(key, recipe, adv.build(output, key, RecipeCategory.FOOD));
			}
		};
	}

	@Override
	public String getName() {
		return "KitchenRecipeProvider";
	}
}
