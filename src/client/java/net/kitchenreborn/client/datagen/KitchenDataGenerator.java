package net.kitchenreborn.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class KitchenDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(KitchenLangProvider::new);
		pack.addProvider(KitchenModelProvider::new);
		pack.addProvider(KitchenRecipeProvider::new);
		pack.addProvider(KitchenBlockLootProvider::new);
		pack.addProvider(KitchenItemTagProvider::new);
		pack.addProvider(KitchenBlockTagProvider::new);
	}
}
