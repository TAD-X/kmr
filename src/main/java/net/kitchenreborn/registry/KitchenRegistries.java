package net.kitchenreborn.registry;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.FoodType;
import net.kitchenreborn.food.LayerDefinition;
import net.kitchenreborn.oven.OvenConfig;
import net.kitchenreborn.pan.PanConfig;

/**
 * Data-driven registries of the modular food core. Both are synced to clients (the board renderer and
 * tooltips need them). They load with the world, like worldgen data: edit a datapack and re-enter the world.
 */
public final class KitchenRegistries {
	/** {@code data/<ns>/kitchen/layer/<name>.json} */
	public static final ResourceKey<Registry<LayerDefinition>> LAYER =
			ResourceKey.createRegistryKey(Kitchen.id("layer"));
	/** {@code data/<ns>/kitchen/food_type/<name>.json} */
	public static final ResourceKey<Registry<FoodType>> FOOD_TYPE =
			ResourceKey.createRegistryKey(Kitchen.id("food_type"));

	/** {@code data/<ns>/kitchen/oven_config/<name>.json}: oven timings (only the server uses it). */
	public static final ResourceKey<Registry<OvenConfig>> OVEN_CONFIG =
			ResourceKey.createRegistryKey(Kitchen.id("oven_config"));

	/** {@code data/<ns>/kitchen/pan_config/<name>.json}: frying pan timing (only the server uses it). */
	public static final ResourceKey<Registry<PanConfig>> PAN_CONFIG =
			ResourceKey.createRegistryKey(Kitchen.id("pan_config"));

	public static void register() {
		DynamicRegistries.register(PAN_CONFIG, PanConfig.CODEC);
		DynamicRegistries.register(OVEN_CONFIG, OvenConfig.CODEC);
		DynamicRegistries.registerSynced(LAYER, LayerDefinition.CODEC);
		DynamicRegistries.registerSynced(FOOD_TYPE, FoodType.CODEC);
	}

	private KitchenRegistries() { }
}
