package net.kitchenreborn.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.kitchenreborn.blockentity.PanBlockEntity;
import net.kitchenreborn.registry.KitchenBlockEntities;

/**
 * The frying pan. It only exists on top of an oven and takes the oven's facing: it sits on the front right of the
 * oven top (original BlockFryingPan bounds). Logic lives in {@link PanBlockEntity}.
 */
public class PanBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	// Original bounds for an oven facing north: the pan body in the east half of the front, two pixels high.
	private static final VoxelShape SHAPE_NORTH = Block.box(8, 0, 2, 16, 2, 10);
	private static final VoxelShape SHAPE_EAST = Block.box(6, 0, 8, 14, 2, 16);
	private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 6, 8, 2, 14);
	private static final VoxelShape SHAPE_WEST = Block.box(2, 0, 0, 10, 2, 8);

	public PanBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** The pan only goes on an oven and faces the same way. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
		if (!(below.getBlock() instanceof OvenBlock)) {
			return null;
		}
		return defaultBlockState().setValue(FACING, below.getValue(OvenBlock.FACING));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).getBlock() instanceof OvenBlock;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case EAST -> SHAPE_EAST;
			case SOUTH -> SHAPE_SOUTH;
			case WEST -> SHAPE_WEST;
			default -> SHAPE_NORTH;
		};
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PanBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != KitchenBlockEntities.PAN) {
			return null;
		}
		return (world, pos, blockState, entity) -> {
			if (!(world instanceof ServerLevel server) || !(entity instanceof PanBlockEntity pan)) {
				return;
			}
			if (!canSurvive(blockState, server, pos)) {
				server.destroyBlock(pos, true); // the oven was removed: the pan comes off
				return;
			}
			pan.serverTick(server);
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (held.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.getBlockEntity(pos) instanceof PanBlockEntity pan) {
			InteractionResult result = pan.useItem(level, player, held, !level.isClientSide());
			return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof PanBlockEntity pan) {
			return pan.useEmptyHand(level, player, !level.isClientSide());
		}
		return InteractionResult.PASS;
	}

	/** Sizzling while frying, bubbling while jam cooks, and smoke once the food is done (original). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof PanBlockEntity pan) || pan.isEmpty()) {
			return;
		}
		VoxelShape shape = getShape(state, level, pos, CollisionContext.empty());
		double x = pos.getX() + shape.min(Direction.Axis.X) + random.nextDouble() * (shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X));
		double z = pos.getZ() + shape.min(Direction.Axis.Z) + random.nextDouble() * (shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z));
		if (pan.isDone()) {
			level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.16, z, 0.0, 0.0, 0.0);
		} else if (random.nextInt(8) == 0) {
			level.playLocalSound(x, pos.getY(), z, pan.isJam() ? SoundEvents.LAVA_POP : SoundEvents.FIRE_AMBIENT,
					SoundSource.BLOCKS, 0.3F, 0.8F + random.nextFloat() * 0.4F, false);
		}
	}
}
