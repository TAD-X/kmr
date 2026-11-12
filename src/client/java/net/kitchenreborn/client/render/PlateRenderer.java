package net.kitchenreborn.client.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.kitchenreborn.block.PlateBlock;
import net.kitchenreborn.blockentity.PlateBlockEntity;
import net.kitchenreborn.food.LayeredFood;
import net.kitchenreborn.registry.KitchenComponents;

/**
 * Draws a plate's contents. Plain items lie flat and stack half a pixel apart (the original's PlateRender); a
 * sandwich is drawn with {@link LayerStackDrawer}, the code the board and the sandwich item use, so it is the
 * same size as on the board.
 */
public class PlateRenderer implements BlockEntityRenderer<PlateBlockEntity, PlateRenderer.State> {
	/** Where the contents start, in pixels: the top of the plate's raised middle. */
	private static final float PLATE_TOP = 1.2F;
	/** Thickness of one plain item: the original's ITEM_PIXEL, half a pixel. */
	private static final float ITEM_HEIGHT = 0.5F;

	private final ItemModelResolver itemModels;

	public PlateRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	public static class State extends BlockEntityRenderState {
		public int rotation;
		public LayerStackDrawer.Drawn drawn = LayerStackDrawer.Drawn.EMPTY;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PlateBlockEntity plate, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(plate, state, crumbling);
		state.rotation = plate.getBlockState().getValue(PlateBlock.ROTATION);
		state.drawn = LayerStackDrawer.Drawn.EMPTY;
		Level level = plate.getLevel();
		if (level != null && !plate.items().isEmpty()) {
			state.drawn = prepare(level, plate.items(), (int) plate.getBlockPos().asLong());
		}
	}

	private LayerStackDrawer.Drawn prepare(Level level, List<ItemStackTemplate> items, int seed) {
		List<LayerStackDrawer.Draw> draws = new ArrayList<>();
		float bottom = 0.0F;
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i).create();
			LayeredFood food = stack.get(KitchenComponents.LAYERED_FOOD);
			if (food != null) {
				LayerStackDrawer.Drawn layers = LayerStackDrawer.prepare(itemModels, level, food.layers(), 0, seed + i * 64);
				for (LayerStackDrawer.Draw draw : layers.layers()) {
					draws.add(new LayerStackDrawer.Draw(draw.item(), draw.height(), draw.bottom() + bottom, draw.isModel()));
				}
				bottom += layers.totalHeight();
			} else {
				ItemStackRenderState render = new ItemStackRenderState();
				itemModels.updateForTopItem(render, stack, ItemDisplayContext.NONE, level, null, seed + i);
				draws.add(new LayerStackDrawer.Draw(render, ITEM_HEIGHT, bottom, false));
				bottom += ITEM_HEIGHT;
			}
		}
		return new LayerStackDrawer.Drawn(draws, bottom);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.drawn.layers().isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5F, PLATE_TOP / 16.0F, 0.5F);
		// Like a sign: 16 steps of 22.5 degrees. A sandwich's long side faces the player who placed the plate.
		poseStack.rotateDegrees(Axis.YP, -state.rotation * 22.5F);
		LayerStackDrawer.submit(state.drawn, poseStack, collector, state.lightCoords);
		poseStack.popPose();
	}
}
