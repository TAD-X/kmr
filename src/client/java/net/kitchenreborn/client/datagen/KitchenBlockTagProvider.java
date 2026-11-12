package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import net.kitchenreborn.registry.KitchenBlocks;

public class KitchenBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
	public KitchenBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(BlockTags.MINEABLE_WITH_AXE).add(BuiltInRegistries.BLOCK.getResourceKey(KitchenBlocks.BOARD).orElseThrow());
		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(BuiltInRegistries.BLOCK.getResourceKey(KitchenBlocks.PLATE).orElseThrow())
				.add(BuiltInRegistries.BLOCK.getResourceKey(KitchenBlocks.OVEN).orElseThrow());
		for (Block crop : KitchenBlocks.CROPS) {
			ResourceKey<Block> key = BuiltInRegistries.BLOCK.getResourceKey(crop).orElseThrow();
			builder(BlockTags.CROPS).add(key);
			builder(BlockTags.MAINTAINS_FARMLAND).add(key); // keeps farmland from reverting under the crop
			builder(BlockTags.BEE_GROWABLES).add(key);
		}
	}
}
