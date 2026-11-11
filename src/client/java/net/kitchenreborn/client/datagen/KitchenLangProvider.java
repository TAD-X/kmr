package net.kitchenreborn.client.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

import net.kitchenreborn.registry.KitchenTabs;

public class KitchenLangProvider extends FabricLanguageProvider {
	public KitchenLangProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
		super(out, "en_us", lookup);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider lookup, TranslationBuilder b) {
		b.add(KitchenTabs.KITCHEN_KEY, "Kitchen");
	}
}
