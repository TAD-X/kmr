package net.kitchenreborn.food;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.resources.Identifier;

/**
 * How a layer is drawn: {@code "flat"} (the item texture as a thin slab) or {@code "model:<id>"}
 * (a 3D layer model, added in M4). Clients fall back to flat when a model is missing.
 */
public record LayerRender(Optional<Identifier> model) {
	public static final LayerRender FLAT = new LayerRender(Optional.empty());
	private static final String MODEL_PREFIX = "model:";

	public static final Codec<LayerRender> CODEC = Codec.STRING.comapFlatMap(LayerRender::parse, LayerRender::serialize);

	public static LayerRender model(Identifier id) {
		return new LayerRender(Optional.of(id));
	}

	private static DataResult<LayerRender> parse(String value) {
		if (value.equals("flat")) {
			return DataResult.success(FLAT);
		}
		if (value.startsWith(MODEL_PREFIX)) {
			Identifier id = Identifier.tryParse(value.substring(MODEL_PREFIX.length()));
			return id == null ? DataResult.error(() -> "Invalid model id: " + value) : DataResult.success(model(id));
		}
		return DataResult.error(() -> "Render must be \"flat\" or \"model:<id>\", got: " + value);
	}

	private String serialize() {
		return model.map(id -> MODEL_PREFIX + id).orElse("flat");
	}
}
