package net.kitchenreborn.client.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;

import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenItems;

public class KitchenModelProvider extends FabricModelProvider {
	public KitchenModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
		// Original stage mapping: age 0-1 -> stage 0, 2-3 -> 1, 4-6 -> 2, 7 -> 3.
		for (Block crop : KitchenBlocks.CROPS) {
			generators.createCropBlock(crop, CropBlock.AGE, 0, 0, 1, 1, 2, 2, 2, 3);
		}
		for (Block wild : KitchenBlocks.WILD_PLANTS) {
			generators.createCrossBlock(wild, BlockModelGenerators.PlantType.NOT_TINTED);
		}
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (Item item : KitchenItems.ALL) {
			if (item instanceof BlockItem) {
				continue; // the crop block generator already writes the seed item's model
			}
			// Textures are assets/kitchen/textures/item/<id>.png (see tools/import_textures.py).
			boolean tool = item == KitchenItems.KNIFE;
			generators.generateFlatItem(item, tool ? ModelTemplates.FLAT_HANDHELD_ITEM : ModelTemplates.FLAT_ITEM);
		}
	}
}
