package net.kitchenreborn;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.kitchenreborn.block.BoardBlock;
import net.kitchenreborn.registry.KitchenBlockEntities;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenRegistries;
import net.kitchenreborn.registry.KitchenItems;
import net.kitchenreborn.registry.KitchenRecipes;
import net.kitchenreborn.registry.KitchenTabs;
import net.kitchenreborn.world.KitchenWorldgen;

public class Kitchen implements ModInitializer {
	public static final String MOD_ID = "kitchen";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		KitchenRegistries.register();
		KitchenComponents.register();
		KitchenBlocks.register();
		KitchenItems.register();
		KitchenBlockEntities.register();
		BoardBlock.registerSneakFinish();
		KitchenWorldgen.register();
		KitchenRecipes.register();
		KitchenTabs.register();
		LOGGER.info("The Kitchen Mod Reborn initialised");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
