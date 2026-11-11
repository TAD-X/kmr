package net.kitchenreborn.food;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * What is in a jam jar: the jam layer item (e.g. {@code kitchen:strawberry_jam}) and servings left.
 * Jars are filled by the frying pan in M7; the board already understands them.
 */
public record JamContents(Identifier jam, int servings) implements TooltipProvider {
	public static final Codec<JamContents> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("jam").forGetter(JamContents::jam),
			Codec.INT.fieldOf("servings").forGetter(JamContents::servings))
			.apply(i, JamContents::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, JamContents> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, JamContents::jam,
			ByteBufCodecs.VAR_INT, JamContents::servings,
			JamContents::new);

	public JamContents withServings(int servings) {
		return new JamContents(jam, servings);
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag,
			DataComponentGetter components) {
		consumer.accept(Component.translatable("tooltip.kitchen.servings_left", servings).withStyle(ChatFormatting.GRAY));
	}
}
