package net.kitchenreborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import net.kitchenreborn.Kitchen;

public final class KitchenTags {
	/** Tools that cut on a board and spread butter. */
	public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, Kitchen.id("knives"));
	/** Whole ingredients that have a {@code kitchen:cutting} recipe (generated from the same table). */
	public static final TagKey<Item> CUTTABLE = TagKey.create(Registries.ITEM, Kitchen.id("cuttable"));

	private KitchenTags() { }
}
