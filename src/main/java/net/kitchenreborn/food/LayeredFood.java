package net.kitchenreborn.food;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * The finished modular food: which food type it is and its layers, bottom to top. Nutrition and name are
 * computed once when the food is finished and stored in the vanilla FOOD / ITEM_NAME components.
 */
public record LayeredFood(Identifier type, List<ItemStackTemplate> layers) implements TooltipProvider {
	public static final Codec<LayeredFood> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("type").forGetter(LayeredFood::type),
			ItemStackTemplate.CODEC.listOf().fieldOf("layers").forGetter(LayeredFood::layers))
			.apply(i, LayeredFood::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, LayeredFood> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, LayeredFood::type,
			ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), LayeredFood::layers,
			LayeredFood::new);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
			DataComponentGetter components) {
		// Top layer first, like the original.
		for (int i = layers.size() - 1; i >= 0; i--) {
			consumer.accept(Component.translatable(layers.get(i).item().value().getDescriptionId())
					.withStyle(ChatFormatting.GRAY));
		}
		FoodProperties food = components.get(DataComponents.FOOD);
		if (food != null) {
			consumer.accept(Component.translatable("tooltip.kitchen.restores", food.nutrition())
					.withStyle(ChatFormatting.DARK_GREEN));
		}
	}
}
