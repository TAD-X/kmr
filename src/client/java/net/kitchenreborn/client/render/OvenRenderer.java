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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.kitchenreborn.blockentity.OvenBlockEntity;

/**
 * Draws what is in the oven: each of the four slots lies flat on the oven floor (a stack shows one item per
 * count, piled up), like the original's TileEntityOvenRenderer. The oven body itself is a normal block model.
 */
public class OvenRenderer implements BlockEntityRenderer<OvenBlockEntity, OvenRenderer.State> {
	/** Floor height of the cavity in blocks (the original translated to 0.4). */
	private static final float FLOOR = 0.4F;
	/** Slot centres in blocks from the middle of the oven: the original's +-0.2 at its 0.75 scale. */
	private static final float[][] SLOT_OFFSETS = {{0.15F, -0.15F}, {0.15F, 0.15F}, {-0.15F, 0.15F}, {-0.15F, -0.15F}};
	private static final float ITEM_SIZE = 0.32F;
	private static final float PILE_STEP = 1.0F / 32.0F;

	private final ItemModelResolver itemModels;

	public OvenRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	public record Slot(ItemStackRenderState item, int count) { }

	public static class State extends BlockEntityRenderState {
		public List<Slot> slots = List.of();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(OvenBlockEntity oven, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(oven, state, crumbling);
		Level level = oven.getLevel();
		List<Slot> slots = new ArrayList<>();
		if (level != null) {
			int seed = (int) oven.getBlockPos().asLong();
			for (int i = 0; i < oven.slots().size(); i++) {
				ItemStackTemplate slot = oven.slots().get(i);
				ItemStackRenderState render = new ItemStackRenderState();
				itemModels.updateForTopItem(render, slot.create(), ItemDisplayContext.NONE, level, null, seed + i);
				slots.add(new Slot(render, slot.count()));
			}
		}
		state.slots = slots;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.slots.size(); i++) {
			Slot slot = state.slots.get(i);
			if (slot.item().isEmpty()) {
				continue;
			}
			float[] offset = SLOT_OFFSETS[i];
			for (int n = 0; n < slot.count(); n++) {
				poseStack.pushPose();
				poseStack.translate(0.5F + offset[0], FLOOR + n * PILE_STEP, 0.5F + offset[1]);
				poseStack.rotateDegrees(Axis.XP, -90.0F);
				poseStack.scale(ITEM_SIZE, ITEM_SIZE, 0.05F);
				slot.item().submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				poseStack.popPose();
			}
		}
	}
}
