package net.kitchenreborn.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.item.KitchenToolItem;

/**
 * All plain items of the mod (M1). Texture and model names equal the item id.
 * Food stats are ported from the original item classes; see {@link #food}.
 */
public final class KitchenItems {
	/** Every registered item in creative-tab order. Datagen iterates this. */
	public static final List<Item> ALL = new ArrayList<>();

	// Tools
	public static final Item KNIFE = tool("knife");
	public static final Item MORTAR = simple("mortar");
	public static final Item PESTLE = simple("pestle");
	public static final Item MORTAR_AND_PESTLE = tool("mortar_and_pestle");
	public static final Item JAM_JAR = simple("jam_jar"); // empty jar; fill levels arrive with the pan (M7)

	// Meat
	public static final Item RAW_BACON = simple("raw_bacon");
	public static final Item BACON = simple("bacon");
	public static final Item RAW_ROAST_BEEF = simple("raw_roast_beef");
	public static final Item ROAST_BEEF = simple("roast_beef");
	public static final Item RAW_CHICKEN_FILLET = simple("raw_chicken_fillet");
	public static final Item CHICKEN_FILLET = simple("chicken_fillet");
	public static final Item CHICKEN_LEG = food("chicken_leg", 4, 0.6F);
	public static final Item BURNT_MEAT = food("burnt_meat", 1, 0.6F);

	// Dairy, baking
	public static final Item CHEESE = food("cheese", 3, 0.6F);
	public static final Item CHEESE_SLICE = simple("cheese_slice");
	public static final Item BUTTER = simple("butter");
	public static final Item FLOUR = simple("flour");
	public static final Item BREAD_SLICE = simple("bread_slice");
	public static final Item TOAST = simple("toast");

	// Produce (the originals were seed-foods: 2 hunger, 0.3 saturation; planting arrives with crops in M2)
	public static final Item TOMATO = food("tomato", 2, 0.3F);
	public static final Item LETTUCE = food("lettuce", 2, 0.3F);
	public static final Item STRAWBERRY = food("strawberry", 2, 0.3F);
	public static final Item PEANUT = food("peanut", 2, 0.3F);
	public static final Item PEANUTS_IN_SHELL = simple("peanuts_in_shell");

	// Slices
	public static final Item TOMATO_SLICE = simple("tomato_slice");
	public static final Item LETTUCE_LEAF = simple("lettuce_leaf");
	public static final Item POTATO_SLICE = simple("potato_slice");
	public static final Item CARROT_SLICE = simple("carrot_slice");
	public static final Item STRAWBERRY_SLICES = food("strawberry_slices", 1, 0.6F);
	public static final Item SUGARED_STRAWBERRY_SLICES = food("sugared_strawberry_slices", 1, 0.6F);
	public static final Item APPLE_SLICE = food("apple_slice", 1, 0.6F);

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

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Kitchen.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ALL.add(item);
		return item;
	}

	/** Forces class loading so every item registers during mod init. */
	public static void register() {
	}

	private KitchenItems() { }
}
