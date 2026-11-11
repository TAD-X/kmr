package net.kitchenreborn.item;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.kitchenreborn.food.JamContents;
import net.kitchenreborn.food.LayerSource;
import net.kitchenreborn.registry.KitchenComponents;

/** The jam jar: empty without a {@code kitchen:jam} component, otherwise a source of jam layers. */
public class JamJarItem extends Item implements LayerSource {
	public JamJarItem(Properties properties) {
		super(properties);
	}

	@Override
	public Optional<Item> peekLayer(ItemStack held) {
		JamContents jam = held.get(KitchenComponents.JAM);
		if (jam == null || jam.servings() <= 0) {
			return Optional.empty();
		}
		return BuiltInRegistries.ITEM.getOptional(jam.jam());
	}

	@Override
	public void consumeServing(ItemStack held) {
		JamContents jam = held.get(KitchenComponents.JAM);
		if (jam == null) {
			return;
		}
		if (jam.servings() <= 1) {
			held.remove(KitchenComponents.JAM); // back to an empty jar
		} else {
			held.set(KitchenComponents.JAM, jam.withServings(jam.servings() - 1));
		}
	}
}
