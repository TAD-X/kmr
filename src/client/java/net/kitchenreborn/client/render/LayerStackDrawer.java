package net.kitchenreborn.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;

import net.kitchenreborn.food.Foods;
import net.kitchenreborn.food.LayerDefinition;

/**
 * Draws a stack of food layers at their true heights (design D1). This one code path is shared by the cutting
 * board and the sandwich item renderer, so a sandwich in hand always matches the one on the board.
 *
 * <p>"Stack space": the origin is the middle of the stack's base, y grows upward, one unit is one block.
 * Layer heights come from the layer data in pixels (1/16 block).
 */
public final class LayerStackDrawer {
	/** Width of a flat layer sprite in blocks: the original drew items at half size (a 16 pixel sprite = 8 px). */
	public static final float FLAT_SIZE = 0.5F;

	/** One layer ready to draw: how it renders, how thick it is, and its bottom (pixels above the base). */
	public record Draw(ItemStackRenderState item, float height, float bottom, boolean isModel) { }

	/** A prepared stack and its total height in pixels. */
	public record Drawn(List<Draw> layers, float totalHeight) {
		public static final Drawn EMPTY = new Drawn(List.of(), 0.0F);
	}

	public static Drawn prepare(ItemModelResolver resolver, Level level, List<ItemStackTemplate> layers,
			int pendingSpread, int seed) {
		HolderLookup.Provider access = level.registryAccess();
		List<Draw> draws = new ArrayList<>(layers.size());
		float bottom = 0.0F;
		for (int i = 0; i < layers.size(); i++) {
			ItemStackTemplate layer = layers.get(i);
			Optional<LayerDefinition> def = Foods.layerFor(access, layer.item().value());
			float height = def.map(LayerDefinition::height).orElse(0.5F);
			boolean isTop = i == layers.size() - 1;

			Optional<Identifier> model = def.flatMap(d -> d.render().model())
					.flatMap(id -> LayerModels.choose(id, isTop, pendingSpread));
			ItemStack stack = layer.create();
			model.ifPresent(id -> stack.set(DataComponents.ITEM_MODEL, id));
			ItemStackRenderState state = new ItemStackRenderState();
			resolver.updateForTopItem(state, stack, ItemDisplayContext.NONE, level, null, seed + i);
			draws.add(new Draw(state, height, bottom, model.isPresent()));
			bottom += height;
		}
		return new Drawn(draws, bottom);
	}

	/** A single ingredient lying on the board (a whole porkchop to be cut), one pixel thick. */
	public static Drawn prepareSingle(ItemModelResolver resolver, Level level, ItemStackTemplate item, int seed) {
		ItemStackRenderState state = new ItemStackRenderState();
		resolver.updateForTopItem(state, item.create(), ItemDisplayContext.NONE, level, null, seed);
		return new Drawn(List.of(new Draw(state, 1.0F, 0.0F, false)), 1.0F);
	}

	public static void submit(Drawn drawn, PoseStack poseStack, SubmitNodeCollector collector, int light) {
		for (Draw layer : drawn.layers()) {
			if (layer.item().isEmpty()) {
				continue;
			}
			poseStack.pushPose();
			if (layer.isModel()) {
				// Item models are centred on the origin, so their base (y = 0 px) is half a block down.
				poseStack.translate(0.0F, layer.bottom() / 16.0F + 0.5F, 0.0F);
			} else {
				poseStack.translate(0.0F, (layer.bottom() + layer.height() / 2.0F) / 16.0F, 0.0F);
				// Lay the sprite flat (its front faces up) and squash its 1 pixel depth to the layer height.
				poseStack.rotateDegrees(Axis.XP, -90.0F);
				poseStack.scale(FLAT_SIZE, FLAT_SIZE, layer.height());
			}
			layer.item().submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}

	private LayerStackDrawer() { }
}
