package net.kitchenreborn.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.item.JamJarItem;
import net.kitchenreborn.item.PlateItem;
import net.kitchenreborn.item.KitchenToolItem;

/**
 * All plain items of the mod (M1). Texture and model names equal the item id.
 * Food stats are ported from the original item classes where they existed; the rest were chosen by the
 * project owner (cooked bacon 2, roast beef 4, chicken fillet 2, toast 2, cheese slice 1). These live in Java on
 * purpose: item food components are set at registration. Sandwich layer heals (M3) are separate JSON data.
 */
public final class KitchenItems {
	/** Every registered item. Datagen iterates this. */
	public static final List<Item> ALL = new ArrayList<>();
	/** The items shown in the Kitchen creative tab, in order. */
	public static final List<Item> IN_TAB = new ArrayList<>();

	// Tools
	public static final Item KNIFE = tool("knife");
	public static final Item MORTAR = simple("mortar");
	public static final Item PESTLE = simple("pestle");
	public static final Item MORTAR_AND_PESTLE = tool("mortar_and_pestle");
	public static final Item JAM_JAR = register("jam_jar", JamJarItem::new, new Item.Properties()); // filled by the pan in M7
	public static final Item BOARD = register("board",
			props -> new BlockItem(KitchenBlocks.BOARD, props), new Item.Properties().useBlockDescriptionPrefix());

	public static final Item PLATE = register("plate",
			props -> new PlateItem(KitchenBlocks.PLATE, props), new Item.Properties().useBlockDescriptionPrefix());

	// Meat
	public static final Item RAW_BACON = simple("raw_bacon");
	public static final Item BACON = food("bacon", 2, 0.6F);
	public static final Item RAW_ROAST_BEEF = simple("raw_roast_beef");
	public static final Item ROAST_BEEF = food("roast_beef", 4, 0.6F);
	public static final Item RAW_CHICKEN_FILLET = simple("raw_chicken_fillet");
	public static final Item CHICKEN_FILLET = food("chicken_fillet", 2, 0.6F);
	public static final Item CHICKEN_LEG = food("chicken_leg", 4, 0.6F);
	public static final Item BURNT_MEAT = food("burnt_meat", 1, 0.6F);

	// Dairy, baking
	public static final Item CHEESE = food("cheese", 3, 0.6F);
	public static final Item CHEESE_SLICE = food("cheese_slice", 1, 0.6F);
	public static final Item BUTTER = simple("butter");
	public static final Item FLOUR = simple("flour");
	public static final Item BREAD_SLICE = simple("bread_slice");
	public static final Item TOAST = food("toast", 2, 0.6F);

	// Produce: seed-foods in the original (2 hunger, 0.3 saturation). The produce itself plants the crop.
	public static final Item TOMATO = crop("tomato", KitchenBlocks.TOMATO_CROP);
	public static final Item LETTUCE = crop("lettuce", KitchenBlocks.LETTUCE_CROP);
	public static final Item STRAWBERRY = crop("strawberry", KitchenBlocks.STRAWBERRY_CROP);
	public static final Item PEANUT = crop("peanut", KitchenBlocks.PEANUT_CROP);
	public static final Item PEANUTS_IN_SHELL = register("peanuts_in_shell", Item::new, new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_LOW));

	// Slices
	public static final Item TOMATO_SLICE = simple("tomato_slice");
	public static final Item LETTUCE_LEAF = simple("lettuce_leaf");
	public static final Item POTATO_SLICE = simple("potato_slice");
	public static final Item CARROT_SLICE = simple("carrot_slice");
	public static final Item STRAWBERRY_SLICES = food("strawberry_slices", 1, 0.6F);
	public static final Item SUGARED_STRAWBERRY_SLICES = food("sugared_strawberry_slices", 1, 0.6F);
	public static final Item APPLE_SLICE = food("apple_slice", 1, 0.6F);

	// Items that exist for the food core but are not given out in the creative tab
	/** The finished sandwich; always built by the cutting board. */
	public static final Item SANDWICH = hidden("sandwich", Item::new, new Item.Properties().stacksTo(1)
			.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build()));
	/** Jam layers: only ever appear on the board, never as items in the world (as in the original). */
	public static final Item STRAWBERRY_JAM = hidden("strawberry_jam", Item::new, new Item.Properties());
	public static final Item APPLE_JAM = hidden("apple_jam", Item::new, new Item.Properties());
	public static final Item PEANUT_JAM = hidden("peanut_jam", Item::new, new Item.Properties());

	private static Item simple(String name) {
		return register(name, Item::new, new Item.Properties());
	}

	private static Item tool(String name) {
		return register(name, KitchenToolItem::new, new Item.Properties().stacksTo(1));
	}

	/**
	 * Edible item. Values come from the original: ItemFood(amount, wolfFavourite) used the 1.7.10 default
	 * saturation modifier 0.6, ItemSeedFood(2, 0.3F) for the produce.
	 */
	private static Item food(String name, int nutrition, float saturationModifier) {
		FoodProperties food = new FoodProperties.Builder()
				.nutrition(nutrition).saturationModifier(saturationModifier).build();
		return register(name, Item::new, new Item.Properties().food(food));
	}

	/** Seed-food: edible and plants {@code crop} on farmland, like the original's ItemSeedFood(2, 0.3F). */
	private static Item crop(String name, Block crop) {
		FoodProperties food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build();
		Item.Properties properties = new Item.Properties().food(food).useItemDescriptionPrefix()
				.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM);
		return register(name, props -> new BlockItem(crop, props), properties);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Kitchen.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ALL.add(item);
		IN_TAB.add(item);
		return item;
	}

	private static Item hidden(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		Item item = register(name, factory, properties);
		IN_TAB.remove(item);
		return item;
	}

	/** Forces class loading so every item registers during mod init. */
	public static void register() {
	}

	private KitchenItems() { }
}
