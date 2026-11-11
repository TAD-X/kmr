package net.kitchenreborn.client.render;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

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
 * layers at their true heights with the same {@link LayerStackDrawer} the cutting board uses. Used for the
 * item in hand (first and third person), the GUI, item frames and dropped items.
 *
 * <p>Tall stacks are shrunk so they stay inside the slot: see {@link #fitScale}.
 */
public class LayeredFoodRenderer implements SpecialModelRenderer<LayerStackDrawer.Drawn> {
	/** Registered name: {@code "type": "kitchen:layered_food"} in an item model definition. */
	public static final net.minecraft.resources.Identifier ID = Kitchen.id("layered_food");

	/** Normal size of a stack, relative to the unit item model space. */
	private static final float BASE_SCALE = 1.5F;
	/** The tallest a stack may be, in blocks of item model space, before it is shrunk. */
	private static final float MAX_HEIGHT = 1.2F;
	private static final int CACHE_SIZE = 64;

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
		float scale = fitScale(drawn.totalHeight());
		poseStack.pushPose();
		// Special renderers start at the corner of the item cube: move to its middle first.
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.scale(scale, scale, scale);
		// Centre the stack vertically on the middle of the cube.
		poseStack.translate(0.0F, -drawn.totalHeight() / 32.0F, 0.0F);
		LayerStackDrawer.submit(drawn, poseStack, collector, light);
		poseStack.popPose();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(-0.5F, -0.5F, -0.5F));
		output.accept(new Vector3f(0.5F, 0.5F, 0.5F));
	}

	/** The item model definition entry: {@code {"type": "kitchen:layered_food"}}. */
	public record Unbaked() implements SpecialModelRenderer.Unbaked<LayerStackDrawer.Drawn> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public SpecialModelRenderer<LayerStackDrawer.Drawn> bake(SpecialModelRenderer.BakingContext context) {
			return new LayeredFoodRenderer();
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
