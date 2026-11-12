package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

import net.kitchenreborn.registry.KitchenRegistries;

public class KitchenWorldgenProvider extends FabricDynamicRegistryProvider {
	public KitchenWorldgenProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.addAll(registries.lookupOrThrow(Registries.FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
		entries.addAll(registries.lookupOrThrow(KitchenRegistries.FOOD_TYPE));
		entries.addAll(registries.lookupOrThrow(KitchenRegistries.LAYER));
		entries.addAll(registries.lookupOrThrow(KitchenRegistries.OVEN_CONFIG));
		entries.addAll(registries.lookupOrThrow(KitchenRegistries.PAN_CONFIG));
	}

	@Override
	public String getName() {
		return "Kitchen Data Registries";
	}
}
