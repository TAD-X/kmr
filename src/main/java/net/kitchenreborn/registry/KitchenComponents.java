package net.kitchenreborn.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.JamContents;
import net.kitchenreborn.food.LayeredFood;

public final class KitchenComponents {
	public static final DataComponentType<LayeredFood> LAYERED_FOOD = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, Kitchen.id("layered_food"),
			DataComponentType.<LayeredFood>builder().persistent(LayeredFood.CODEC)
					.networkSynchronized(LayeredFood.STREAM_CODEC).cacheEncoding().build());

	public static final DataComponentType<JamContents> JAM = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, Kitchen.id("jam"),
			DataComponentType.<JamContents>builder().persistent(JamContents.CODEC)
					.networkSynchronized(JamContents.STREAM_CODEC).build());

	/** Forces class loading so the component types register during mod init. */
	public static void register() {
	}

	private KitchenComponents() { }
}
