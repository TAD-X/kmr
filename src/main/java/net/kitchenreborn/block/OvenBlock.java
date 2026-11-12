package net.kitchenreborn.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.kitchenreborn.blockentity.OvenBlockEntity;
import net.kitchenreborn.registry.KitchenBlockEntities;

/**
 * The oven. Faces the player who placed it (its hatch is on the front). {@link #OPEN} is the hatch, {@link #LIT}
 * is the "active" look while it is cooking. Logic lives in {@link OvenBlockEntity}.
 */
public class OvenBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public OvenBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, LIT);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new OvenBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type != KitchenBlockEntities.OVEN) {
			return null;
		}
		if (level.isClientSide()) {
			return (world, pos, blockState, entity) -> {
				if (entity instanceof OvenBlockEntity oven) {
					oven.clientTick();
				}
			};
		}
		return (world, pos, blockState, entity) -> {
			if (entity instanceof OvenBlockEntity oven && world instanceof net.minecraft.server.level.ServerLevel server) {
				oven.serverTick(server);
			}
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (held.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.getBlockEntity(pos) instanceof OvenBlockEntity oven) {
			return oven.useItem(level, player, held, !level.isClientSide());
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof OvenBlockEntity oven) {
			boolean commit = !level.isClientSide();
			return player.isSecondaryUseActive() ? oven.toggle(level, commit) : oven.takeStack(level, player, commit);
		}
		return InteractionResult.PASS;
	}

	/** Particles: flames while there is fuel inside, smoke (and flames if burnt) from the front once it is done. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof OvenBlockEntity oven)) {
			return;
		}
		if (oven.hasFuel()) {
			level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.5,
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
		}
		if (state.getValue(OPEN) || oven.stage() == OvenBlockEntity.Stage.RAW) {
			return;
		}
		Direction front = state.getValue(FACING);
		double along = 0.1 + random.nextDouble() * 0.8;
		double x = pos.getX() + 0.5 + front.getStepX() * 0.375 + (front.getAxis() == Direction.Axis.Z ? along - 0.5 : 0.0);
		double z = pos.getZ() + 0.5 + front.getStepZ() * 0.375 + (front.getAxis() == Direction.Axis.X ? along - 0.5 : 0.0);
		double y = pos.getY() + 0.8125;
		level.addParticle(ParticleTypes.SMOKE, x, y, z, front.getStepX() * 0.025, 0.0, front.getStepZ() * 0.025);
		if (oven.stage() == OvenBlockEntity.Stage.BURNT) {
			level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
		}
	}
}
