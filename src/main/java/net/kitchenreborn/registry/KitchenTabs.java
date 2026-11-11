package net.kitchenreborn.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.LayeredFood;

public final class KitchenTabs {
	public static final ResourceKey<CreativeModeTab> KITCHEN_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kitchen.id("kitchen"));

	public static final CreativeModeTab KITCHEN = FabricCreativeModeTab.builder()
			.icon(KitchenTabs::basicSandwich)
			.title(Component.translatable("itemGroup.kitchen.kitchen"))
			.displayItems((params, output) -> KitchenItems.IN_TAB.forEach(output::accept))
			.build();

	/** The tab icon, like the original: bread, raw bacon, bread. */
	private static ItemStack basicSandwich() {
		ItemStack sandwich = new ItemStack(KitchenItems.SANDWICH);
		sandwich.set(KitchenComponents.LAYERED_FOOD, new LayeredFood(Kitchen.id("sandwich"), List.of(
				new ItemStackTemplate(KitchenItems.BREAD_SLICE), new ItemStackTemplate(KitchenItems.RAW_BACON),
				new ItemStackTemplate(KitchenItems.BREAD_SLICE))));
		return sandwich;
	}

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KITCHEN_KEY, KITCHEN);
	}

	private KitchenTabs() { }
}
