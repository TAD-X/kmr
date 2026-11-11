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
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.kitchenreborn.block.BoardBlock;
import net.kitchenreborn.blockentity.BoardBlockEntity;
import net.kitchenreborn.food.Foods;
import net.kitchenreborn.food.LayerDefinition;

/**
 * Draws a board's contents at their true heights (design D1): layers are stacked from the top of the board,
 * each as thick as its layer definition says (original SandwichRender values). Layers without a 3D model
 * (everything until M4) are drawn as the item sprite lying flat.
 *
 * <p>26.3 render pattern: {@link #extractRenderState} copies what is needed from the block entity into a
 * render state on the main thread, {@link #submit} turns that state into draw calls.
 */
public class BoardRenderer implements BlockEntityRenderer<BoardBlockEntity, BoardRenderer.State> {
	/** Top of the board in pixels: the first layer sits on it. */
	private static final float BOARD_TOP = 2.0F;
	/** Width of a flat layer sprite in blocks (a 16 pixel sprite is shrunk to fit the 10 pixel wide board). */
	private static final float FLAT_SIZE = 0.6F;
	/** Thickness in pixels of a whole ingredient lying on the board. */
	private static final float WORKPIECE_HEIGHT = 1.0F;

	private final ItemModelResolver itemModels;

	public BoardRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	/** One drawn layer: its item, how thick it is and where its bottom sits (pixels above the block's base). */
	public record DrawnLayer(ItemStackRenderState item, float height, float bottom) { }

	public static class State extends BlockEntityRenderState {
		public Direction.Axis axis = Direction.Axis.X;
		public final List<DrawnLayer> layers = new ArrayList<>();
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
		state.layers.clear();

		Level level = board.getLevel();
		if (level == null) {
			return;
		}
		RegistryAccess access = level.registryAccess();
		int seed = (int) board.getBlockPos().asLong();
		float bottom = BOARD_TOP;
		for (ItemStackTemplate layer : board.stack().layers()) {
			float height = Foods.layerFor(access, layer.item().value()).map(LayerDefinition::height).orElse(0.5F);
			state.layers.add(new DrawnLayer(resolve(layer, level, seed), height, bottom));
			bottom += height;
		}
		ItemStackTemplate workpiece = board.workpiece();
		if (workpiece != null) {
			state.layers.add(new DrawnLayer(resolve(workpiece, level, seed), WORKPIECE_HEIGHT, BOARD_TOP));
		}
	}

	private ItemStackRenderState resolve(ItemStackTemplate template, Level level, int seed) {
		ItemStackRenderState renderState = new ItemStackRenderState();
		itemModels.updateForTopItem(renderState, template.create(), ItemDisplayContext.NONE, level, null, seed);
		return renderState;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		for (DrawnLayer layer : state.layers) {
			if (layer.item().isEmpty()) {
				continue;
			}
			poseStack.pushPose();
			// Center of the layer: middle of the block, half way up its own thickness.
			poseStack.translate(0.5F, (layer.bottom() + layer.height() / 2.0F) / 16.0F, 0.5F);
			if (state.axis == Direction.Axis.Z) {
				poseStack.rotateDegrees(Axis.YP, 90.0F);
			}
			// Item models are centred on the origin. Lay the sprite flat (its front faces up) and squash its
			// 1 pixel depth to the layer height.
			poseStack.rotateDegrees(Axis.XP, -90.0F);
			poseStack.scale(FLAT_SIZE, FLAT_SIZE, layer.height());
			layer.item().submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}
}
