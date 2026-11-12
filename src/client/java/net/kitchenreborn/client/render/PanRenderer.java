package net.kitchenreborn.client.render;

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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.block.PanBlock;
import net.kitchenreborn.blockentity.PanBlockEntity;
import net.kitchenreborn.pan.PanCooking;
import net.kitchenreborn.registry.KitchenComponents;

/**
 * Draws what is in the pan: fried food as a flat item that swaps from raw to cooked when done (original
 * ItemBaseRenderingHandler), jam as a tinted layer that fills in over three stages (original ModelJam0..3).
 */
public class PanRenderer implements BlockEntityRenderer<PanBlockEntity, PanRenderer.State> {
	/** The middle of the pan body for an oven facing north, in blocks: pixel (12, 6) of the block. */
	private static final float CENTER_X = 12.0F / 16.0F;
	private static final float CENTER_Z = 6.0F / 16.0F;
	private static final float FOOD_HEIGHT = 0.075F;
	/** The original drew the item at 0.74 of its half-size sprite. */
	private static final float FOOD_SIZE = 0.37F;

	private final ItemModelResolver itemModels;

	public PanRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	public static class State extends BlockEntityRenderState {
		public Direction facing = Direction.NORTH;
		public ItemStackRenderState content = new ItemStackRenderState();
		public boolean isJam;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PanBlockEntity pan, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pan, state, crumbling);
		state.facing = pan.getBlockState().getValue(PanBlock.FACING);
		state.content = new ItemStackRenderState();
		state.isJam = false;
		Level level = pan.getLevel();
		ItemStackTemplate input = pan.input();
		ItemStackTemplate result = pan.result();
		if (level == null || input == null || result == null) {
			return;
		}
		ItemStack shown = (pan.isDone() ? result : input).create();
		ItemStack cooked = result.create();
		if (PanCooking.isJam(cooked)) {
			// Jam: one model per fill stage, tinted by the jam in the stack's jam component.
			state.isJam = true;
			shown = new ItemStack(cooked.getItem());
			shown.set(DataComponents.ITEM_MODEL, Kitchen.id("pan_jam_" + pan.stage()));
			shown.set(KitchenComponents.JAM, cooked.get(KitchenComponents.JAM));
		}
		itemModels.updateForTopItem(state.content, shown, ItemDisplayContext.NONE, level, null, 0);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.content.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.0F, 0.5F);
		poseStack.rotateDegrees(Axis.YP, -turn(state.facing));
		poseStack.translate(-0.5F, 0.0F, -0.5F);
		if (state.isJam) {
			// The jam models are in block coordinates already (centred item model, like the oven hatch).
			poseStack.translate(0.5F, 0.5F, 0.5F);
		} else {
			poseStack.translate(CENTER_X, FOOD_HEIGHT, CENTER_Z);
			poseStack.rotateDegrees(Axis.XP, -90.0F);
			poseStack.scale(FOOD_SIZE, FOOD_SIZE, 0.05F);
		}
		state.content.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

	private static float turn(Direction facing) {
		return switch (facing) {
			case EAST -> 90.0F;
			case SOUTH -> 180.0F;
			case WEST -> 270.0F;
			default -> 0.0F;
		};
	}
}
