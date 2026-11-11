package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

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
	}
}
