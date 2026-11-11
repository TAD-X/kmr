package net.kitchenreborn.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.kitchenreborn.food.Foods;

public class KitchenClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Port of SandwichableTooltipEvent: mark every ingredient that can be layered on a board.
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level == null) {
				return;
			}
			Foods.layerFor(minecraft.level.registryAccess(), stack.getItem())
					.filter(def -> def.showTooltip())
					.ifPresent(def -> lines.add(Component.translatable("tooltip.kitchen.sandwichable")
							.withStyle(ChatFormatting.GRAY)));
		});
	}
}
