package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;

import net.kitchenreborn.registry.KitchenItems;

public class KitchenItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public KitchenItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// The original's chicken leg was ItemFood(4, wolfFavourite = true).
		builder(ItemTags.WOLF_FOOD).add(BuiltInRegistries.ITEM.getResourceKey(KitchenItems.CHICKEN_LEG).orElseThrow());
	}
}
