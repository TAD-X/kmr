package net.kitchenreborn.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.kitchenreborn.Kitchen;

public final class KitchenTabs {
	public static final ResourceKey<CreativeModeTab> KITCHEN_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kitchen.id("kitchen"));

	public static final CreativeModeTab KITCHEN = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(KitchenItems.KNIFE))
			.title(Component.translatable("itemGroup.kitchen.kitchen"))
			.displayItems((params, output) -> KitchenItems.ALL.forEach(output::accept))
			.build();

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KITCHEN_KEY, KITCHEN);
	}

	private KitchenTabs() { }
}
