package net.kitchenreborn.pan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.registry.KitchenRegistries;

/**
 * Frying pan timing, as data ({@code data/kitchen/kitchen/pan_config/default.json}): ticks until the food in the
 * pan is done (original: 300). Jam shows its three fill stages at a third, two thirds and all of this time.
 */
public record PanConfig(int cookTicks) {
	public static final Codec<PanConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(3, 1_000_000).fieldOf("cook_ticks").forGetter(PanConfig::cookTicks))
			.apply(i, PanConfig::new));

	public static final ResourceKey<PanConfig> DEFAULT = ResourceKey.create(KitchenRegistries.PAN_CONFIG, Kitchen.id("default"));

	private static final PanConfig FALLBACK = new PanConfig(300);

	public static PanConfig get(HolderLookup.Provider access) {
		return access.lookup(KitchenRegistries.PAN_CONFIG).flatMap(lookup -> lookup.get(DEFAULT))
				.map(reference -> reference.value()).orElse(FALLBACK);
	}
}
