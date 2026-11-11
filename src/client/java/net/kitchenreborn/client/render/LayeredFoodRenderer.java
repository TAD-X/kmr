package net.kitchenreborn.client.render;

import java.util.LinkedHashMap;
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
import net.minecraft.world.item.ItemStack;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.LayeredFood;
import net.kitchenreborn.registry.KitchenComponents;

/**
 * The special item model of finished layered foods (sandwich now, pizza and others later): draws the stored
 * layers at their true heights with the same {@link LayerStackDrawer} the cutting board uses.
 *
 * <p>Outside the GUI the stack is drawn at exactly its cutting board size (1 block of model = 1 block in the
 * world) and the item model's display transforms are scale 1, so a sandwich in hand, in a frame or on the ground
 * is as big as it was on the board. Only the GUI ({@code fit_to_slot}) makes it bigger and shrinks tall stacks
 * so they stay inside the slot: see {@link #fitScale}.
 */
public class LayeredFoodRenderer implements SpecialModelRenderer<LayerStackDrawer.Drawn> {
	/** Registered name: {@code "type": "kitchen:layered_food"} in an item model definition. */
	public static final net.minecraft.resources.Identifier ID = Kitchen.id("layered_food");

	/** GUI size of a stack, relative to the unit item model space. */
	private static final float BASE_SCALE = 1.5F;
	/** The tallest a stack may be in the GUI, in blocks of item model space, before it is shrunk. */
	private static final float MAX_HEIGHT = 1.2F;
	private static final int CACHE_SIZE = 64;

	private final boolean fitToSlot;

	public LayeredFoodRenderer(boolean fitToSlot) {
		this.fitToSlot = fitToSlot;
	}

	/** Recently drawn foods, so a sandwich is only resolved once, not every frame. */
	private final Map<LayeredFood, LayerStackDrawer.Drawn> cache = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<LayeredFood, LayerStackDrawer.Drawn> eldest) {
			return size() > CACHE_SIZE;
		}
	};

	/** Scale for a stack of the given height in pixels: full size for small stacks, shrunk to fit tall ones. */
	public static float fitScale(float totalHeightPixels) {
		float blocks = totalHeightPixels / 16.0F;
		return blocks * BASE_SCALE > MAX_HEIGHT ? MAX_HEIGHT / blocks : BASE_SCALE;
	}

	@Override
	public LayerStackDrawer.Drawn extractArgument(ItemStack stack) {
		LayeredFood food = stack.get(KitchenComponents.LAYERED_FOOD);
		Minecraft minecraft = Minecraft.getInstance();
		if (food == null || minecraft.level == null) {
			return null;
		}
		return cache.computeIfAbsent(food, key -> LayerStackDrawer.prepare(minecraft.getItemModelResolver(),
				minecraft.level, key.layers(), 0, key.hashCode()));
	}

	@Override
	public void submit(LayerStackDrawer.Drawn drawn, PoseStack poseStack, SubmitNodeCollector collector,
			int light, int overlay, boolean foil, int outlineColor) {
		if (drawn == null || drawn.layers().isEmpty()) {
			return;
		}
		poseStack.pushPose();
		if (fitToSlot) {
			// GUI: special renderers start at the corner of the item cube, so move to its middle, shrink tall
			// stacks to fit and centre the stack vertically.
			float scale = fitScale(drawn.totalHeight());
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.scale(scale, scale, scale);
			poseStack.translate(0.0F, -drawn.totalHeight() / 32.0F, 0.0F);
		} else {
			// Everywhere else the stack stands on the bottom of the cube (the hold point is the bottom bun), so tall
			// stacks grow upwards instead of sinking into the ground or the hand.
			poseStack.translate(0.5F, 0.0F, 0.5F);
		}
		LayerStackDrawer.submit(drawn, poseStack, collector, light);
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		// Item model space has the item cube at 0..1. The GUI centres the stack on the cube; elsewhere it stands on
		// the bottom (this also decides how high a dropped item hovers above the ground).
		output.accept(new Vector3f(0.0F, 0.0F, 0.0F));
		output.accept(new Vector3f(1.0F, 1.0F, 1.0F));
	}

	/** The item model definition entry: {@code {"type": "kitchen:layered_food", "fit_to_slot": false}}. */
	public record Unbaked(boolean fitToSlot) implements SpecialModelRenderer.Unbaked<LayerStackDrawer.Drawn> {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.BOOL.optionalFieldOf("fit_to_slot", false).forGetter(Unbaked::fitToSlot))
				.apply(i, Unbaked::new));

		@Override
		public SpecialModelRenderer<LayerStackDrawer.Drawn> bake(SpecialModelRenderer.BakingContext context) {
			return new LayeredFoodRenderer(fitToSlot);
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
