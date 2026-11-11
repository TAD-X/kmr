package net.kitchenreborn.block;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.kitchenreborn.blockentity.BoardBlockEntity;

/**
 * The cutting board. Faces the player's side like the original (two orientations, the long side along
 * {@link #AXIS}). Interaction logic lives in {@link BoardBlockEntity}.
 */
public class BoardBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	// Original bounds: 1..15 along the long side, 3..13 across, 2 pixels high.
	private static final VoxelShape SHAPE_X = Block.box(1, 0, 3, 15, 2, 13);
	private static final VoxelShape SHAPE_Z = Block.box(3, 0, 1, 13, 2, 15);

	public BoardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		// Looking north or south: long side along X (the original's "metadata 0").
		Direction.Axis facing = context.getHorizontalDirection().getAxis();
		return defaultBlockState().setValue(AXIS, facing == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BoardBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (held.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.getBlockEntity(pos) instanceof BoardBlockEntity board) {
			InteractionResult result = board.useItem(level, player, held, !level.isClientSide());
			return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof BoardBlockEntity board) {
			boolean commit = !level.isClientSide();
			return player.isSecondaryUseActive() ? board.finish(level, player, commit) : board.useEmptyHand(level, commit);
		}
		return InteractionResult.PASS;
	}

	/**
	 * Vanilla skips block interaction while sneaking with an item in hand. The original finished the food on any
	 * sneak right click, so intercept that case.
	 */
	public static void registerSneakFinish() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isSecondaryUseActive()) {
				return InteractionResult.PASS;
			}
			if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
				return InteractionResult.PASS; // empty hands reach useWithoutItem normally
			}
			BlockPos pos = hit.getBlockPos();
			if (level.getBlockState(pos).getBlock() instanceof BoardBlock
					&& level.getBlockEntity(pos) instanceof BoardBlockEntity board) {
				return board.finish(level, player, !level.isClientSide());
			}
			return InteractionResult.PASS;
		});
	}
}
