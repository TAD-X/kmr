package net.kitchenreborn.world;

import java.util.List;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.registry.KitchenBlocks;

/**
 * Wild plants (port of WorldGenWildPlants): per chunk, a 1-in-10 chance to put one random wild plant on a
 * grass block at the surface, Overworld only. The numbers live in the generated placed-feature JSON
 * (data/kitchen/worldgen/placed_feature/wild_plants.json), so a datapack can retune them.
 */
public final class KitchenWorldgen {
	public static final ResourceKey<Feature> WILD_PLANTS_FEATURE =
			ResourceKey.create(Registries.FEATURE, Kitchen.id("wild_plants"));
	public static final ResourceKey<PlacedFeature> WILD_PLANTS_PLACED =
			ResourceKey.create(Registries.PLACED_FEATURE, Kitchen.id("wild_plants"));

	/** Original config default: lettuce_spawn_rate = 10 (percent of chunks), i.e. once per 10 chunks. */
	private static final int CHUNKS_PER_PATCH = 10;

	public static void features(BootstrapContext<Feature> context) {
		WeightedList.Builder<BlockState> states = WeightedList.builder();
		for (Block plant : KitchenBlocks.WILD_PLANTS) {
			states.add(plant.defaultBlockState(), 1);
		}
		context.register(WILD_PLANTS_FEATURE, new SimpleBlockFeature(new WeightedStateProvider(states)));
	}

	public static void placed(BootstrapContext<PlacedFeature> context) {
		List<PlacementModifier> modifiers = List.of(
				RarityFilter.onAverageOnceEvery(CHUNKS_PER_PATCH),
				InSquarePlacement.spread(),
				PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
				BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(new BlockPos(0, -1, 0), List.of(Blocks.GRASS_BLOCK))),
				BiomeFilter.biome());
		context.register(WILD_PLANTS_PLACED, new PlacedFeature(
				context.lookup(Registries.FEATURE).getOrThrow(WILD_PLANTS_FEATURE), modifiers));
	}

	public static void register() {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION, WILD_PLANTS_PLACED);
	}

	private KitchenWorldgen() { }
}
