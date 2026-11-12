package net.kitchenreborn.food;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * One ingredient that can be layered into a modular food (replaces the original SandwichableConfig and
 * board events). Lives in the {@code kitchen:layer} registry: {@code data/<ns>/kitchen/layer/<name>.json}.
 *
 * @param nutrition    hunger restored by this layer
 * @param saturation   saturation modifier (like vanilla food: layer saturation = nutrition * modifier * 2)
 * @param height       height in pixels (1/16 block) the layer takes up when drawn (original, D1)
 * @param foods        food types ({@code kitchen:food_type}) that accept this layer
 * @param spreadClicks knife clicks needed to spread this layer before anything can go on top (butter: 2)
 * @param placeOn      if non-empty, this layer may only be placed directly on one of these items
 * @param dropItem     whether removing the layer from the station gives the item back (jam: no)
 * @param showTooltip  whether the item shows the "Sandwichable" tooltip
 * @param tint         colour of the jam in jars and the frying pan (RGB; only used by jam layers)
 */
public record LayerDefinition(Item item, int nutrition, float saturation, LayerRole role, float height,
		LayerRender render, List<Identifier> foods, int spreadClicks, List<Item> placeOn, boolean dropItem,
		boolean showTooltip, int tint) {
	public static final Codec<LayerDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(LayerDefinition::item),
			Codec.INT.fieldOf("nutrition").forGetter(LayerDefinition::nutrition),
			Codec.FLOAT.optionalFieldOf("saturation", 0.6F).forGetter(LayerDefinition::saturation),
			LayerRole.CODEC.fieldOf("role").forGetter(LayerDefinition::role),
			Codec.FLOAT.fieldOf("height").forGetter(LayerDefinition::height),
			LayerRender.CODEC.optionalFieldOf("render", LayerRender.FLAT).forGetter(LayerDefinition::render),
			Identifier.CODEC.listOf().fieldOf("foods").forGetter(LayerDefinition::foods),
			Codec.INT.optionalFieldOf("spread_clicks", 0).forGetter(LayerDefinition::spreadClicks),
			BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("place_on", List.of())
					.forGetter(LayerDefinition::placeOn),
			Codec.BOOL.optionalFieldOf("drop_item", true).forGetter(LayerDefinition::dropItem),
			Codec.BOOL.optionalFieldOf("show_tooltip", true).forGetter(LayerDefinition::showTooltip),
			Codec.INT.optionalFieldOf("tint", 0xFFFFFF).forGetter(LayerDefinition::tint))
			.apply(i, LayerDefinition::new));

	/** Absolute saturation this layer adds (vanilla formula). */
	public float absoluteSaturation() {
		return nutrition * saturation * 2.0F;
	}
}
