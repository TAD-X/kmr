package net.kitchenreborn.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * A kind of modular food (sandwich now; pizza, lasagna, ... later): {@code kitchen:food_type}.
 *
 * @param station the block the food is assembled on
 * @param output  the item produced when the food is finished
 */
public record FoodType(int minLayers, int maxLayers, LayerRole bottomRole, LayerRole topRole, Block station,
		Item output) {
	public static final Codec<FoodType> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("min_layers").forGetter(FoodType::minLayers),
			Codec.INT.fieldOf("max_layers").forGetter(FoodType::maxLayers),
			LayerRole.CODEC.fieldOf("bottom_role").forGetter(FoodType::bottomRole),
			LayerRole.CODEC.fieldOf("top_role").forGetter(FoodType::topRole),
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("station").forGetter(FoodType::station),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("output").forGetter(FoodType::output))
			.apply(i, FoodType::new));
}
