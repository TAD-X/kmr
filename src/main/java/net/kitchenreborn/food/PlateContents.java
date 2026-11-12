package net.kitchenreborn.food;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/** What a plate carries, bottom to top: the item component of a picked-up plate (like a shulker box's contents). */
public record PlateContents(List<ItemStackTemplate> items) implements TooltipProvider {
	public static final PlateContents EMPTY = new PlateContents(List.of());

	public static final Codec<PlateContents> CODEC = ItemStackTemplate.CODEC.listOf().xmap(PlateContents::new,
			PlateContents::items);

	public static final StreamCodec<RegistryFriendlyByteBuf, PlateContents> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC
			.apply(ByteBufCodecs.list()).map(PlateContents::new, PlateContents::items);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
			DataComponentGetter components) {
		// Top first, like the sandwich tooltip; identical neighbours are merged ("3x Bacon").
		List<Component> lines = new ArrayList<>();
		int run = 0;
		ItemStackTemplate previous = null;
		for (int i = items.size() - 1; i >= 0; i--) {
			ItemStackTemplate item = items.get(i);
			if (previous != null && previous.equals(item)) {
				run++;
				continue;
			}
			if (previous != null) {
				lines.add(line(previous, run));
			}
			previous = item;
			run = 1;
		}
		if (previous != null) {
			lines.add(line(previous, run));
		}
		lines.forEach(consumer);
	}

	private static Component line(ItemStackTemplate item, int count) {
		ItemStack stack = item.create();
		Component name = stack.getHoverName();
		return (count > 1 ? Component.literal(count + "x ").append(name) : name.copy()).withStyle(ChatFormatting.GRAY);
	}
}
