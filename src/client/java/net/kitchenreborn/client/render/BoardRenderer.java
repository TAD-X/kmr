package net.kitchenreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.kitchenreborn.block.BoardBlock;
import net.kitchenreborn.blockentity.BoardBlockEntity;

/**
 * Draws a board's contents with {@link LayerStackDrawer}, the same code the sandwich item uses.
 *
 * <p>26.3 render pattern: {@link #extractRenderState} copies what is needed from the block entity into a
 * render state, {@link #submit} turns that state into draw calls.
 */
public class BoardRenderer implements BlockEntityRenderer<BoardBlockEntity, BoardRenderer.State> {
	/** Top of the board in pixels: the first layer sits on it. */
	private static final float BOARD_TOP = 2.0F;

	private final ItemModelResolver itemModels;

	public BoardRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	public static class State extends BlockEntityRenderState {
		public Direction.Axis axis = Direction.Axis.X;
		public LayerStackDrawer.Drawn drawn = LayerStackDrawer.Drawn.EMPTY;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BoardBlockEntity board, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(board, state, crumbling);
		state.axis = board.getBlockState().getValue(BoardBlock.AXIS);
		state.drawn = LayerStackDrawer.Drawn.EMPTY;

		Level level = board.getLevel();
		if (level == null) {
			return;
		}
		int seed = (int) board.getBlockPos().asLong();
		ItemStackTemplate workpiece = board.workpiece();
		if (workpiece != null) {
			state.drawn = LayerStackDrawer.prepareSingle(itemModels, level, workpiece, seed);
		} else if (!board.stack().isEmpty()) {
			state.drawn = LayerStackDrawer.prepare(itemModels, level, board.stack().layers(),
					board.stack().pendingSpread(), seed);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.drawn.layers().isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5F, BOARD_TOP / 16.0F, 0.5F);
		if (state.axis == Direction.Axis.Z) {
			poseStack.rotateDegrees(Axis.YP, 90.0F);
		}
		LayerStackDrawer.submit(state.drawn, poseStack, collector, state.lightCoords);
		poseStack.popPose();
	}
}
