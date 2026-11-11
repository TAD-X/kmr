package net.kitchenreborn.food;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;

import net.kitchenreborn.registry.KitchenRegistries;

/** Lookups and calculations shared by every modular food (sandwich, later pizza, lasagna, ...). */
public final class Foods {
	/** The layer definition for an item, if the item is an ingredient of any modular food. */
	public static Optional<LayerDefinition> layerFor(HolderLookup.Provider access, Item item) {
		return access.lookup(KitchenRegistries.LAYER).flatMap(lookup -> lookup.listElements()
				.map(Holder.Reference::value)
				.filter(def -> def.item() == item)
				.findFirst());
	}

	public static Optional<FoodType> foodType(HolderLookup.Provider access, Identifier id) {
		return access.lookup(KitchenRegistries.FOOD_TYPE)
				.flatMap(lookup -> lookup.get(ResourceKey.create(KitchenRegistries.FOOD_TYPE, id)))
				.map(Holder.Reference::value);
	}

	/** The food type assembled on {@code station} that accepts {@code first} as its first layer. */
	public static Optional<Identifier> foodTypeFor(HolderLookup.Provider access, Block station, LayerDefinition first) {
		return access.lookup(KitchenRegistries.FOOD_TYPE).flatMap(lookup -> lookup.listElements()
				.filter(type -> type.value().station() == station)
				.filter(type -> first.foods().contains(type.key().identifier()))
				.map(type -> type.key().identifier())
				.findFirst());
	}

	/** Total nutrition and saturation of the layers; may exceed 20 (the original allowed that). */
	public static FoodProperties nutrition(HolderLookup.Provider access, List<ItemStackTemplate> layers) {
		int nutrition = 0;
		float saturation = 0;
		for (ItemStackTemplate layer : layers) {
			Optional<LayerDefinition> def = layerFor(access, layer.item().value());
			if (def.isPresent()) {
				nutrition += def.get().nutrition();
				saturation += def.get().absoluteSaturation();
			}
		}
		return new FoodProperties(nutrition, saturation, true);
	}

	/**
	 * Auto-name (design D13): the two top-most distinct fillings, shown bottom to top, e.g.
	 * "Bacon Cheese Sandwich". Falls back to spreads, then to the plain item name.
	 */
	public static Optional<Component> name(HolderLookup.Provider access, FoodType type, List<ItemStackTemplate> layers) {
		List<Item> chosen = pickNaming(access, layers, LayerRole.FILLING);
		if (chosen.isEmpty()) {
			chosen = pickNaming(access, layers, LayerRole.SPREAD);
		}
		if (chosen.isEmpty()) {
			return Optional.empty();
		}
		Object[] args = chosen.stream().map(item -> (Object) Component.translatable(layerNameKey(item))).toArray();
		return Optional.of(Component.translatable(type.output().getDescriptionId() + ".named_" + args.length, args));
	}

	private static List<Item> pickNaming(HolderLookup.Provider access, List<ItemStackTemplate> layers, LayerRole role) {
		Set<Item> picked = new LinkedHashSet<>();
		for (int i = layers.size() - 1; i >= 0 && picked.size() < 2; i--) {
			Item item = layers.get(i).item().value();
			if (layerFor(access, item).map(def -> def.role() == role).orElse(false)) {
				picked.add(item);
			}
		}
		List<Item> result = new ArrayList<>(picked);
		java.util.Collections.reverse(result); // bottom to top
		return result;
	}

	/** Lang key of a layer's short name used in auto-names, e.g. {@code layer.kitchen.bacon}. */
	public static String layerNameKey(Item item) {
		return Util.makeDescriptionId("layer", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
	}

	private Foods() { }
}
