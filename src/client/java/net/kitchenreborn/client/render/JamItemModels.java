package net.kitchenreborn.client.render;

import java.lang.reflect.Field;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.Foods;
import net.kitchenreborn.food.JamContents;
import net.kitchenreborn.registry.KitchenComponents;

/**
 * Item model pieces for jam, registered by id so item model definitions can use them:
 * {@code kitchen:jam_color} (tint source: the colour of the jam in a stack's {@code kitchen:jam} component, from
 * the jam's layer definition) and {@code kitchen:jam_servings} (range property: servings left, 0 when empty).
 */
public final class JamItemModels {
	/** The tint source {@code {"type": "kitchen:jam_color"}}. */
	public record JamColor() implements ItemTintSource {
		public static final MapCodec<JamColor> MAP_CODEC = MapCodec.unit(new JamColor());
		private static final int NO_TINT = 0xFFFFFFFF;

		@Override
		public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
			JamContents jam = stack.get(KitchenComponents.JAM);
			if (jam == null || level == null) {
				return NO_TINT;
			}
			return BuiltInRegistries.ITEM.getOptional(jam.jam())
					.flatMap(item -> Foods.layerFor(level.registryAccess(), item))
					.map(layer -> 0xFF000000 | layer.tint())
					.orElse(NO_TINT);
		}

		@Override
		public MapCodec<JamColor> type() {
			return MAP_CODEC;
		}
	}

	/** The range property {@code {"property": "kitchen:jam_servings"}}. */
	public record JamServings() implements RangeSelectItemModelProperty {
		public static final MapCodec<JamServings> MAP_CODEC = MapCodec.unit(new JamServings());

		@Override
		public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
			JamContents jam = stack.get(KitchenComponents.JAM);
			return jam == null ? 0.0F : jam.servings();
		}

		@Override
		public MapCodec<JamServings> type() {
			return MAP_CODEC;
		}
	}

	private static boolean registered;

	/** Registers both by id; called by the client entrypoint and by datagen (the vanilla id mappers are private). */
	public static synchronized void register() {
		if (registered) {
			return;
		}
		registered = true;
		try {
			put(ItemTintSources.class, Kitchen.id("jam_color"), JamColor.MAP_CODEC);
			put(RangeSelectItemModelProperties.class, Kitchen.id("jam_servings"), JamServings.MAP_CODEC);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not register the jam item model pieces", e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void put(Class<?> owner, Identifier id, MapCodec<?> codec) throws ReflectiveOperationException {
		Field field = owner.getDeclaredField("ID_MAPPER");
		field.setAccessible(true);
		((ExtraCodecs.LateBoundIdMapper) field.get(null)).put(id, codec);
	}

	private JamItemModels() { }
}
