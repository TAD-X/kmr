package net.kitchenreborn.client.render;

import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;

import net.kitchenreborn.Kitchen;

/**
 * Chooses which 3D layer model draws a layer. Layer data says {@code "render": "model:kitchen:bread_slice"};
 * this picks the variant for the position in the stack, like the original rendering handlers did:
 * the last bread slice is the top slice and all others are bottom slices, and the butter on top of the
 * stack shows how far it has been spread. Models that do not exist fall back to the flat sprite.
 */
public final class LayerModels {
	/** The model names that exist (assets/kitchen/models/item/layer/). Checked against datagen. */
	public static final Set<String> KNOWN = Set.of("bread_slice_bottom", "bread_slice_top", "raw_bacon",
			"cooked_bacon", "butter_0", "butter_1", "butter_2");

	/** The item model definition id, e.g. {@code kitchen:layer/bread_slice_top}, if a model exists. */
	public static Optional<Identifier> choose(Identifier modelId, boolean isTop, int pendingSpread) {
		if (!modelId.getNamespace().equals(Kitchen.MOD_ID)) {
			return Optional.empty();
		}
		String name = switch (modelId.getPath()) {
			case "bread_slice" -> isTop ? "bread_slice_top" : "bread_slice_bottom";
			// 2 clicks left: a fresh block of butter, 1: half spread, 0: spread flat (always flat below the top).
			case "butter" -> "butter_" + (isTop ? Math.max(0, Math.min(2, pendingSpread)) : 0);
			default -> modelId.getPath();
		};
		return KNOWN.contains(name) ? Optional.of(Kitchen.id("layer/" + name)) : Optional.empty();
	}

	private LayerModels() { }
}
