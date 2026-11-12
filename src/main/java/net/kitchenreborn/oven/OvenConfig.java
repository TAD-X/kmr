package net.kitchenreborn.oven;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.registry.KitchenRegistries;

/**
 * Oven timing, as data ({@code data/kitchen/kitchen/oven_config/default.json}): how many ticks after the oven is
 * closed with fuel the food is cooked, and how many until everything is burnt (original: 400 and 600).
 */
public record OvenConfig(int cookTicks, int burnTicks) {
	public static final Codec<OvenConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(1, 1_000_000).fieldOf("cook_ticks").forGetter(OvenConfig::cookTicks),
			Codec.intRange(1, 1_000_000).fieldOf("burn_ticks").forGetter(OvenConfig::burnTicks))
			.apply(i, OvenConfig::new));

	public static final ResourceKey<OvenConfig> DEFAULT = ResourceKey.create(KitchenRegistries.OVEN_CONFIG, Kitchen.id("default"));

	/** Only used when the datapack entry is missing. */
	private static final OvenConfig FALLBACK = new OvenConfig(400, 600);

	public static OvenConfig get(HolderLookup.Provider access) {
		return access.lookup(KitchenRegistries.OVEN_CONFIG).flatMap(lookup -> lookup.get(DEFAULT))
				.map(reference -> reference.value()).orElse(FALLBACK);
	}
}
