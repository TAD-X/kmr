package net.kitchenreborn.client.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.world.item.Item;

import net.kitchenreborn.registry.KitchenItems;

public class KitchenModelProvider extends FabricModelProvider {
	public KitchenModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (Item item : KitchenItems.ALL) {
			// Textures are assets/kitchen/textures/item/<id>.png (see tools/import_textures.py).
			boolean tool = item == KitchenItems.KNIFE;
			generators.generateFlatItem(item, tool ? ModelTemplates.FLAT_HANDHELD_ITEM : ModelTemplates.FLAT_ITEM);
		}
	}
}
