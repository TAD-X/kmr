package net.kitchenreborn.food;

import java.util.Optional;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item that supplies a layer without being placed itself (the jam jar: it adds a jam layer and loses a
 * serving, but stays in the player's hand).
 */
public interface LayerSource {
	/** The layer item this stack would add right now, if any. */
	Optional<Item> peekLayer(ItemStack held);

	/** Called after the layer was added: use up one serving. */
	void consumeServing(ItemStack held);
}
