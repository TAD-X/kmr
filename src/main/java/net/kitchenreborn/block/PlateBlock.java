package net.kitchenreborn.block;

import java.util.List;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.kitchenreborn.blockentity.PlateBlockEntity;
import net.kitchenreborn.registry.KitchenItems;

/**
 * The plate. Faces the placer in 16 steps like a sign; the items on it are drawn by the block entity renderer.
 * Interaction logic lives in {@link PlateBlockEntity}.
 */
public class PlateBlock extends Block implements EntityBlock {
	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

	// Original bounds: 2 pixels in from the edges, 3 pixels high.
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 3, 14);

	public PlateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(ROTATION, Mth.floor(context.getRotation() * 16.0F / 360.0F + 0.5) & 15);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PlateBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (held.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.getBlockEntity(pos) instanceof PlateBlockEntity plate) {
			InteractionResult result = plate.useItem(player, held, !level.isClientSide());
			return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof PlateBlockEntity plate) {
			boolean commit = !level.isClientSide();
			return player.isSecondaryUseActive() ? plate.pickUp(level, player, commit) : plate.useEmptyHand(player, commit);
		}
		return InteractionResult.PASS;
	}

	/**
	 * Breaking (by hand, a piston, water...) keeps the contents in the dropped plate item, like a shulker box.
	 * Explosions drop the plate and its contents as separate items instead.
	 */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PlateBlockEntity plate && !plate.isEmpty()) {
			boolean explosion = plate.isExploding() || params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS) != null;
			ItemStack dropped = null;
			if (!explosion) {
				for (ItemStack drop : drops) {
					if (drop.is(KitchenItems.PLATE)) {
						dropped = drop;
						break;
					}
				}
			}
			if (dropped != null) {
				dropped.applyComponents(plate.collectComponents());
			} else {
				plate.items().forEach(item -> drops.add(item.create()));
			}
			plate.markContentsHandled();
		}
		return drops;
	}

	/** The loot parameters only say "explosion" for explosions with item decay, so note it on the block entity. */
	@Override
	protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
			BiConsumer<ItemStack, BlockPos> dropConsumer) {
		if (level.getBlockEntity(pos) instanceof PlateBlockEntity plate) {
			plate.markExploding();
		}
		super.onExplosionHit(state, level, pos, explosion, dropConsumer);
	}

	/** Vanilla skips block interaction while sneaking with an item in hand; picking the plate up must still work. */
	public static void registerSneakPickup() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isSecondaryUseActive()) {
				return InteractionResult.PASS;
			}
			if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
				return InteractionResult.PASS; // empty hands reach useWithoutItem normally
			}
			BlockPos pos = hit.getBlockPos();
			if (level.getBlockState(pos).getBlock() instanceof PlateBlock
					&& level.getBlockEntity(pos) instanceof PlateBlockEntity plate) {
				return plate.pickUp(level, player, !level.isClientSide());
			}
			return InteractionResult.PASS;
		});
	}
}
