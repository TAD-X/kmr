package net.kitchenreborn.client.render;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.PlateContents;
import net.kitchenreborn.registry.KitchenComponents;

/**
 * The special item model of a plate that carries something: the plate and its contents, drawn with the same code
 * as the placed plate, at the same size. Like the sandwich, only the GUI ({@code mode: gui}) fits it into the slot;
 * every other context stands it on the bottom of the item cube at true size.
 */
public class PlateItemRenderer implements SpecialModelRenderer<LayerStackDrawer.Drawn> {
	public static final Identifier ID = Kitchen.id("plate");
	private static final int CACHE_SIZE = 32;

	private final LayeredFoodRenderer.Mode mode;
	private final Map<PlateContents, LayerStackDrawer.Drawn> cache = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<PlateContents, LayerStackDrawer.Drawn> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	public PlateItemRenderer(LayeredFoodRenderer.Mode mode) {
		this.mode = mode;
	}

	@Override
	public LayerStackDrawer.Drawn extractArgument(ItemStack stack) {
		// An empty plate has no component: it goes through the same path with nothing on it.
		PlateContents contents = stack.getOrDefault(KitchenComponents.PLATE_CONTENTS, PlateContents.EMPTY);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return null;
		}
		return cache.computeIfAbsent(contents, key -> PlateDrawer.prepare(minecraft.getItemModelResolver(),
				minecraft.level, key.items(), key.hashCode(), true));
	}

	@Override
	public void submit(LayerStackDrawer.Drawn drawn, PoseStack poseStack, SubmitNodeCollector collector,
			int light, int overlay, boolean foil, int outlineColor) {
		if (drawn == null || drawn.layers().isEmpty()) {
			return;
		}
		poseStack.pushPose();
		if (mode != LayeredFoodRenderer.Mode.BASE) {
			// GUI: centred and fitted to the slot. First person: true size, pivoting low in the stack like the sandwich.
			float scale = mode == LayeredFoodRenderer.Mode.GUI ? LayeredFoodRenderer.fitScale(drawn.totalHeight()) : 1.0F;
			float pivot = mode == LayeredFoodRenderer.Mode.GUI ? 0.5F : LayeredFoodRenderer.FIRST_PERSON_PIVOT;
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.scale(scale, scale, scale);
			poseStack.translate(0.0F, -drawn.totalHeight() * pivot / 16.0F, 0.0F);
		} else {
			poseStack.translate(0.5F, 0.0F, 0.5F);
		}
		LayerStackDrawer.submit(drawn, poseStack, collector, light);
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
		output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
	}

	/** The item model definition entry: {@code {"type": "kitchen:plate", "mode": "base"}}. */
	public record Unbaked(LayeredFoodRenderer.Mode mode) implements SpecialModelRenderer.Unbaked<LayerStackDrawer.Drawn> {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.xmap(name -> LayeredFoodRenderer.Mode.valueOf(name.toUpperCase(Locale.ROOT)),
						m -> m.name().toLowerCase(Locale.ROOT))
						.optionalFieldOf("mode", LayeredFoodRenderer.Mode.BASE).forGetter(Unbaked::mode))
				.apply(i, Unbaked::new));

		@Override
		public SpecialModelRenderer<LayerStackDrawer.Drawn> bake(SpecialModelRenderer.BakingContext context) {
			return new PlateItemRenderer(mode);
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}

	private static boolean registered;

	/** Registers the renderer type; called by the client entrypoint and by datagen. */
	public static synchronized void register() {
		if (!registered) {
			registered = true;
			SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC);
		}
	}
}
