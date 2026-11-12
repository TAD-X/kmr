package net.kitchenreborn.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

import net.kitchenreborn.client.render.LayeredFoodRenderer;
import net.kitchenreborn.registry.KitchenRegistries;
import net.kitchenreborn.world.KitchenWorldgen;

public class KitchenDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		LayeredFoodRenderer.register(); // the item model definitions use the kitchen:layered_food type
		net.kitchenreborn.client.render.PlateItemRenderer.register();
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(KitchenLangProvider::new);
		pack.addProvider(KitchenModelProvider::new);
		pack.addProvider(KitchenLayerModelProvider::new);
		pack.addProvider(KitchenRecipeProvider::new);
		pack.addProvider(KitchenBlockLootProvider::new);
		pack.addProvider(KitchenItemTagProvider::new);
		pack.addProvider(KitchenBlockTagProvider::new);
		pack.addProvider(KitchenWorldgenProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.FEATURE, KitchenWorldgen::features);
		registryBuilder.add(Registries.PLACED_FEATURE, KitchenWorldgen::placed);
		registryBuilder.add(KitchenRegistries.FOOD_TYPE, KitchenFoodData::foodTypes);
		registryBuilder.add(KitchenRegistries.LAYER, KitchenFoodData::layers);
	}
}
