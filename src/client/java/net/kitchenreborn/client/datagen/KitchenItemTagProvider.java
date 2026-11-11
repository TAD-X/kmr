package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;

import net.kitchenreborn.registry.KitchenItems;
import net.kitchenreborn.registry.KitchenTags;

public class KitchenItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public KitchenItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(KitchenTags.KNIVES).add(BuiltInRegistries.ITEM.getResourceKey(KitchenItems.KNIFE).orElseThrow());
		for (CuttingTable.Cut cut : CuttingTable.CUTS) {
			builder(KitchenTags.CUTTABLE).add(BuiltInRegistries.ITEM.getResourceKey(cut.input()).orElseThrow());
		}
		// The original's chicken leg was ItemFood(4, wolfFavourite = true).
		builder(ItemTags.WOLF_FOOD).add(BuiltInRegistries.ITEM.getResourceKey(KitchenItems.CHICKEN_LEG).orElseThrow());
		// Farmer villagers plant the produce (it is its own seed) and pick it up after harvesting.
		for (Item produce : new Item[] {KitchenItems.TOMATO, KitchenItems.LETTUCE, KitchenItems.STRAWBERRY, KitchenItems.PEANUT}) {
			builder(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(BuiltInRegistries.ITEM.getResourceKey(produce).orElseThrow());
			builder(ItemTags.VILLAGER_PICKS_UP).add(BuiltInRegistries.ITEM.getResourceKey(produce).orElseThrow());
		}
		builder(ItemTags.VILLAGER_PICKS_UP).add(BuiltInRegistries.ITEM.getResourceKey(KitchenItems.PEANUTS_IN_SHELL).orElseThrow());
	}
}
