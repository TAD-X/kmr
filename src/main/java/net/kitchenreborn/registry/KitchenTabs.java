package net.kitchenreborn.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.kitchenreborn.Kitchen;

public final class KitchenTabs {
	public static final ResourceKey<CreativeModeTab> KITCHEN_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kitchen.id("kitchen"));

	// Placeholder icon (bread) until M1 adds the knife.
	public static final CreativeModeTab KITCHEN = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(Items.BREAD))
			.title(Component.translatable("itemGroup.kitchen.kitchen"))
			// Vanilla hides creative tabs with no items, so show a placeholder until M1 (remove then).
			.displayItems((params, output) -> output.accept(Items.BREAD))
			.build();

	public static void register() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KITCHEN_KEY, KITCHEN);
	}

	private KitchenTabs() { }
}
