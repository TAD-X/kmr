package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenItems;
import net.kitchenreborn.registry.KitchenTabs;

public class KitchenLangProvider extends FabricLanguageProvider {
	public KitchenLangProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
		super(out, "en_us", lookup);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider lookup, TranslationBuilder b) {
		b.add(KitchenTabs.KITCHEN_KEY, "Kitchen");

		// Names follow the original en_US.lang where it had one.
		b.add(KitchenItems.KNIFE, "Knife");
		b.add(KitchenItems.MORTAR, "Mortar");
		b.add(KitchenItems.PESTLE, "Pestle");
		b.add(KitchenItems.MORTAR_AND_PESTLE, "Mortar and Pestle");
		b.add(KitchenItems.JAM_JAR, "Empty Jar");

		b.add(KitchenItems.RAW_BACON, "Raw Bacon");
		b.add(KitchenItems.BACON, "Cooked Bacon");
		b.add(KitchenItems.RAW_ROAST_BEEF, "Raw Beef Slice");
		b.add(KitchenItems.ROAST_BEEF, "Roastbeef");
		b.add(KitchenItems.RAW_CHICKEN_FILLET, "Raw Chicken Fillet");
		b.add(KitchenItems.CHICKEN_FILLET, "Cooked Chicken Fillet");
		b.add(KitchenItems.CHICKEN_LEG, "Chicken Leg");
		b.add(KitchenItems.BURNT_MEAT, "Burnt Meat");

		b.add(KitchenItems.CHEESE, "Cheese");
		b.add(KitchenItems.CHEESE_SLICE, "Cheese Slice");
		b.add(KitchenItems.BUTTER, "Butter");
		b.add(KitchenItems.FLOUR, "Flour");
		b.add(KitchenItems.BREAD_SLICE, "Slice of Bread");
		b.add(KitchenItems.TOAST, "Piece of Toast");

		b.add(KitchenItems.TOMATO, "Tomato");
		b.add(KitchenItems.LETTUCE, "Lettuce");
		b.add(KitchenItems.STRAWBERRY, "Strawberry");
		b.add(KitchenItems.PEANUT, "Peanut");
		b.add(KitchenItems.PEANUTS_IN_SHELL, "Peanuts in Shell");

		b.add(KitchenItems.TOMATO_SLICE, "Tomato Slice");
		b.add(KitchenItems.LETTUCE_LEAF, "Lettuce Leaves");
		b.add(KitchenItems.POTATO_SLICE, "Slice of Potato");
		b.add(KitchenItems.CARROT_SLICE, "Carrot Slices");
		b.add(KitchenItems.STRAWBERRY_SLICES, "Strawberry Slices");
		b.add(KitchenItems.SUGARED_STRAWBERRY_SLICES, "Sugared Strawberry Slices");
		b.add(KitchenItems.APPLE_SLICE, "Apple Slices");

		b.add(KitchenBlocks.BOARD, "Cutting Board");
		b.add(KitchenBlocks.PLATE, "Plate");
		b.add(KitchenBlocks.OVEN, "Oven");
		b.add(KitchenItems.SANDWICH, "Sandwich");
		b.add(KitchenItems.STRAWBERRY_JAM, "Strawberry Jam");
		b.add(KitchenItems.APPLE_JAM, "Apple Jam");
		b.add(KitchenItems.PEANUT_JAM, "Peanut Butter");

		// Auto-names (design D13): "<filling> <filling> Sandwich"
		b.add("item.kitchen.sandwich.named_1", "%s Sandwich");
		b.add("item.kitchen.sandwich.named_2", "%s %s Sandwich");
		// Short layer names used in the auto-names
		b.add("layer.kitchen.raw_bacon", "Raw Bacon");
		b.add("layer.kitchen.bacon", "Bacon");
		b.add("layer.kitchen.tomato_slice", "Tomato");
		b.add("layer.kitchen.lettuce_leaf", "Lettuce");
		b.add("layer.kitchen.potato_slice", "Potato");
		b.add("layer.kitchen.carrot_slice", "Carrot");
		b.add("layer.kitchen.raw_roast_beef", "Raw Beef");
		b.add("layer.kitchen.roast_beef", "Roast Beef");
		b.add("layer.kitchen.raw_chicken_fillet", "Raw Chicken");
		b.add("layer.kitchen.chicken_fillet", "Chicken");
		b.add("layer.kitchen.cheese_slice", "Cheese");
		b.add("layer.kitchen.butter", "Butter");
		b.add("layer.kitchen.strawberry_jam", "Strawberry Jam");
		b.add("layer.kitchen.apple_jam", "Apple Jam");
		b.add("layer.kitchen.peanut_jam", "Peanut Butter");

		b.add("tooltip.kitchen.sandwichable", "Sandwichable");
		b.add("tooltip.kitchen.restores", "Restores %s hunger");
		b.add("tooltip.kitchen.servings_left", "Servings left: %s");
		b.add("message.kitchen.finish.too_few_layers", "Not enough layers yet");
		b.add("message.kitchen.finish.bad_ends", "It needs bread on the top and the bottom");

		b.add(KitchenBlocks.TOMATO_CROP, "Tomato Plant");
		b.add(KitchenBlocks.LETTUCE_CROP, "Lettuce Crop");
		b.add(KitchenBlocks.STRAWBERRY_CROP, "Strawberry Crop");
		b.add(KitchenBlocks.PEANUT_CROP, "Peanut Crop");
		b.add(KitchenBlocks.WILD_TOMATO, "Wild Tomato");
		b.add(KitchenBlocks.WILD_LETTUCE, "Wild Lettuce");
		b.add(KitchenBlocks.WILD_STRAWBERRY, "Wild Strawberry");
		b.add(KitchenBlocks.WILD_PEANUT, "Wild Peanut");
	}
}
