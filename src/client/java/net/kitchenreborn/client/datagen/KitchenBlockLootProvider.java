package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;

import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenItems;

public class KitchenBlockLootProvider extends FabricBlockLootSubProvider {
	public KitchenBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generate() {
		dropSelf(KitchenBlocks.BOARD);
		dropSelf(KitchenBlocks.PLATE);
		dropSelf(KitchenBlocks.OVEN);

		// Tomato, lettuce, strawberry: the produce is also the seed, so drops match vanilla carrots/potatoes.
		cropWithOwnSeed(KitchenBlocks.TOMATO_CROP, KitchenItems.TOMATO);
		cropWithOwnSeed(KitchenBlocks.LETTUCE_CROP, KitchenItems.LETTUCE);
		cropWithOwnSeed(KitchenBlocks.STRAWBERRY_CROP, KitchenItems.STRAWBERRY);

		// Peanut (original BlockPeanutCrop): mature drops peanuts in shell (+ bonus shells) and, new in this port,
		// one peanut; immature drops the peanut seed.
		LootItemCondition.Builder mature = matureCondition(KitchenBlocks.PEANUT_CROP);
		add(KitchenBlocks.PEANUT_CROP, applyExplosionDecay(KitchenBlocks.PEANUT_CROP, LootTable.lootTable()
				.withPool(LootPool.lootPool().add(LootItem.lootTableItem(KitchenItems.PEANUTS_IN_SHELL).when(mature)
						.otherwise(LootItem.lootTableItem(KitchenItems.PEANUT))))
				// A mature crop also returns one plain peanut so it can be replanted (villagers need it too).
				.withPool(LootPool.lootPool().when(mature).add(LootItem.lootTableItem(KitchenItems.PEANUT)))
				.withPool(LootPool.lootPool().when(mature).add(LootItem.lootTableItem(KitchenItems.PEANUTS_IN_SHELL)
						.apply(ApplyBonusCount.addBonusBinomialDistributionCount(
								enchantments.getOrThrow(Enchantments.FORTUNE),
								0.5714286F, 3))))));

		// Wild plants always drop two (original quantityDropped = 2).
		wild(KitchenBlocks.WILD_TOMATO, KitchenItems.TOMATO);
		wild(KitchenBlocks.WILD_LETTUCE, KitchenItems.LETTUCE);
		wild(KitchenBlocks.WILD_STRAWBERRY, KitchenItems.STRAWBERRY);
		wild(KitchenBlocks.WILD_PEANUT, KitchenItems.PEANUTS_IN_SHELL);
	}

	private LootItemCondition.Builder matureCondition(Block crop) {
		return MatchBlock.blockMatches(blocks, crop,
				StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, CropBlock.MAX_AGE));
	}

	private void cropWithOwnSeed(Block crop, Item produce) {
		add(crop, createCropDrops(crop, produce, produce, matureCondition(crop)));
	}

	private void wild(Block block, Item drop) {
		add(block, applyExplosionDecay(block, LootTable.lootTable().withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(Holder.direct(new ConstantValue(2))))))));
	}
}
