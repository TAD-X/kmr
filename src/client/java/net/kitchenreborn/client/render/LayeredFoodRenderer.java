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
 * is as big as it was on the board. Only the GUI ({@code mode: gui}) makes it bigger and shrinks tall stacks
 * so they stay inside the slot: see {@link #fitScale}.
 */
public class LayeredFoodRenderer implements SpecialModelRenderer<LayerStackDrawer.Drawn> {
	/** Registered name: {@code "type": "kitchen:layered_food"} in an item model definition. */
	public static final net.minecraft.resources.Identifier ID = Kitchen.id("layered_food");

	/** GUI size of a stack, relative to the unit item model space. */
	private static final float BASE_SCALE = 1.5F;
	/** The tallest a stack may be in the GUI, in blocks of item model space, before it is shrunk. */
	private static final float MAX_HEIGHT = 1.2F;
	/** Where in the stack (0 = bottom, 1 = top) a held sandwich pivots in first person. */
	public static final float FIRST_PERSON_PIVOT = 0.3F;
	private static final int CACHE_SIZE = 64;

	/** How the stack sits in the item cube. */
	public enum Mode {
		/** Stands on the bottom of the cube: dropped, third person, frames (the hold point is the bottom bun). */
		BASE,
		/** Centred on the cube and scaled to fit the slot. */
		GUI,
		/** Centred on the cube, true size: first person, where the eating animation swings the item around its centre. */
		CENTER
	}

	private final Mode mode;

	public LayeredFoodRenderer(Mode mode) {
		this.mode = mode;
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
		if (mode != Mode.BASE) {
			// GUI: special renderers start at the corner of the item cube, so move to its middle, shrink tall
			// stacks to fit and centre the stack vertically.
			float scale = mode == Mode.GUI ? fitScale(drawn.totalHeight()) : 1.0F;
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.scale(scale, scale, scale);
			// The GUI pivots on the middle of the stack; in first person on a point low in the stack (near the bottom
			// bun), so the fillings stay in view when it swings up to be eaten.
			float pivot = mode == Mode.GUI ? 0.5F : FIRST_PERSON_PIVOT;
			poseStack.translate(0.0F, -drawn.totalHeight() * pivot / 16.0F, 0.0F);
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

	/** The item model definition entry: {@code {"type": "kitchen:layered_food", "mode": "base"}}. */
	public record Unbaked(Mode mode) implements SpecialModelRenderer.Unbaked<LayerStackDrawer.Drawn> {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.xmap(name -> Mode.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
							m -> m.name().toLowerCase(java.util.Locale.ROOT))
					.optionalFieldOf("mode", Mode.BASE).forGetter(Unbaked::mode))
				.apply(i, Unbaked::new));

		@Override
		public SpecialModelRenderer<LayerStackDrawer.Drawn> bake(SpecialModelRenderer.BakingContext context) {
			return new LayeredFoodRenderer(mode);
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
