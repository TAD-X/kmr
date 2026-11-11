package net.kitchenreborn.client.datagen;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.FoodType;
import net.kitchenreborn.food.LayerDefinition;
import net.kitchenreborn.food.LayerRender;
import net.kitchenreborn.food.LayerRole;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenItems;
import net.kitchenreborn.registry.KitchenRegistries;

/**
 * The sandwich data: food type and the 17 sandwichable layers of the original.
 *
 * <p>Nutrition comes from the original SandwichableConfig (see kitchen-mod-feature-summary.md). Heights are the
 * original SandwichRender values in pixels (1/16 block): models were drawn at 0.75 scale, so the bread slice
 * (0.125 blocks) is 0.125 * 0.75 * 16 = 1.5 px, cooked bacon (2 px) is 1.5, raw bacon (1 px) is 0.75, butter
 * (0.05) is 0.6. Everything without a model was a flat item of 0.5 px (ITEM_PIXEL = 0.03125 blocks).
 */
public final class KitchenFoodData {
	public static final Identifier SANDWICH = Kitchen.id("sandwich");
	private static final List<Identifier> SANDWICH_ONLY = List.of(SANDWICH);
	/** Design D2 (PLAN.md section 7): the original allowed 10, this port allows 32. */
	public static final int MAX_SANDWICH_LAYERS = 32;
	private static final float FLAT = 0.5F;

	public static void foodTypes(BootstrapContext<FoodType> context) {
		context.register(ResourceKey.create(KitchenRegistries.FOOD_TYPE, SANDWICH),
				new FoodType(2, MAX_SANDWICH_LAYERS, LayerRole.BREAD, LayerRole.BREAD, KitchenBlocks.BOARD, KitchenItems.SANDWICH));
	}

	public static void layers(BootstrapContext<LayerDefinition> context) {
		// Bread: 3D top/bottom models in M4 (the original drew the last layer as the top slice).
		layer(context, KitchenItems.BREAD_SLICE, 0, LayerRole.BREAD, 1.5F, model("bread_slice"));
		layer(context, KitchenItems.TOAST, 0, LayerRole.BREAD, FLAT, LayerRender.FLAT);

		layer(context, KitchenItems.RAW_BACON, 1, LayerRole.FILLING, 0.75F, model("raw_bacon"));
		layer(context, KitchenItems.BACON, 2, LayerRole.FILLING, 1.5F, model("cooked_bacon"));
		layer(context, KitchenItems.TOMATO_SLICE, 1, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.LETTUCE_LEAF, 2, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.POTATO_SLICE, 2, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.CARROT_SLICE, 2, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.RAW_ROAST_BEEF, 1, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.ROAST_BEEF, 4, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.RAW_CHICKEN_FILLET, 1, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.CHICKEN_FILLET, 4, LayerRole.FILLING, FLAT, LayerRender.FLAT);
		layer(context, KitchenItems.CHEESE_SLICE, 3, LayerRole.FILLING, FLAT, LayerRender.FLAT);

		// Butter only goes on a bread slice and needs two knife clicks to spread (original BoardEventRegistry).
		context.register(key(KitchenItems.BUTTER), new LayerDefinition(KitchenItems.BUTTER, 1, 0.6F, LayerRole.SPREAD,
				0.6F, model("butter"), SANDWICH_ONLY, 2, List.of(KitchenItems.BREAD_SLICE), true, true));

		// Jam is applied from a jar and is gone when removed (original: dontDropItem, hideInformation).
		jam(context, KitchenItems.STRAWBERRY_JAM);
		jam(context, KitchenItems.APPLE_JAM);
		jam(context, KitchenItems.PEANUT_JAM);
	}

	private static void layer(BootstrapContext<LayerDefinition> context, Item item, int nutrition, LayerRole role,
			float height, LayerRender render) {
		context.register(key(item), new LayerDefinition(item, nutrition, 0.6F, role, height, render, SANDWICH_ONLY,
				0, List.of(), true, true));
	}

	private static void jam(BootstrapContext<LayerDefinition> context, Item item) {
		context.register(key(item), new LayerDefinition(item, 2, 0.6F, LayerRole.SPREAD, FLAT, LayerRender.FLAT,
				SANDWICH_ONLY, 0, List.of(), false, false));
	}

	private static LayerRender model(String name) {
		return LayerRender.model(Kitchen.id(name));
	}

	private static ResourceKey<LayerDefinition> key(Item item) {
		return ResourceKey.create(KitchenRegistries.LAYER, BuiltInRegistries.ITEM.getKey(item));
	}

	private KitchenFoodData() { }
}
