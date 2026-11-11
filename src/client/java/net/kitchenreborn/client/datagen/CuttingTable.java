package net.kitchenreborn.client.datagen;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.kitchenreborn.registry.KitchenItems;

/**
 * Single-ingredient cuts, ported from the original knife recipes. One table feeds the {@code kitchen:cutting}
 * recipes, the shapeless grid knife recipes and the {@code kitchen:cuttable} item tag.
 */
public final class CuttingTable {
	public record Cut(Item input, Item result, int count) { }

	public static final List<Cut> CUTS = List.of(
			new Cut(Items.PORKCHOP, KitchenItems.RAW_BACON, 3),
			new Cut(KitchenItems.TOMATO, KitchenItems.TOMATO_SLICE, 4),
			new Cut(KitchenItems.LETTUCE, KitchenItems.LETTUCE_LEAF, 2),
			new Cut(Items.CARROT, KitchenItems.CARROT_SLICE, 2),
			new Cut(Items.POTATO, KitchenItems.POTATO_SLICE, 3),
			new Cut(Items.BREAD, KitchenItems.BREAD_SLICE, 2),
			new Cut(Items.BEEF, KitchenItems.RAW_ROAST_BEEF, 2),
			new Cut(Items.CHICKEN, KitchenItems.RAW_CHICKEN_FILLET, 3),
			new Cut(Items.COOKED_CHICKEN, KitchenItems.CHICKEN_LEG, 2),
			new Cut(KitchenItems.CHEESE, KitchenItems.CHEESE_SLICE, 2),
			new Cut(KitchenItems.STRAWBERRY, KitchenItems.STRAWBERRY_SLICES, 2),
			new Cut(Items.APPLE, KitchenItems.APPLE_SLICE, 2));

	private CuttingTable() { }
}
