package net.kitchenreborn.blockentity;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.kitchenreborn.item.JamJarItem;
import net.kitchenreborn.pan.PanConfig;
import net.kitchenreborn.pan.PanCooking;
import net.kitchenreborn.registry.KitchenBlockEntities;
import net.kitchenreborn.registry.KitchenComponents;

/**
 * The frying pan, a port of the original's TileEntityPan: one ingredient at a time, cooked while the pan sits on
 * an oven (design D6: any oven, lit or not). Fried food is done after the configured time (original: 300 ticks)
 * and does not burn. Jam shows three fill stages on the way and is taken with an empty jar.
 *
 * <p>The server counts {@code cookTime}; clients only see the synced {@code stage} (0 to 3) and {@code done}.
 * Interactions take a {@code commit} flag: the client calls with {@code false} to predict the swing.
 */
public class PanBlockEntity extends BlockEntity {
	private @Nullable ItemStackTemplate input;
	private @Nullable ItemStackTemplate result;
	private int cookTime;
	private int stage;
	private boolean done;

	public PanBlockEntity(BlockPos pos, BlockState state) {
		super(KitchenBlockEntities.PAN, pos, state);
	}

	public @Nullable ItemStackTemplate input() {
		return input;
	}

	public @Nullable ItemStackTemplate result() {
		return result;
	}

	public int cookTime() {
		return cookTime;
	}

	/** Jam fill stage 0 to 3 (fried food only uses 0 and 3). */
	public int stage() {
		return stage;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isEmpty() {
		return input == null;
	}

	/** Whether the thing in the pan comes out as jam (needs an empty jar). */
	public boolean isJam() {
		return result != null && PanCooking.isJam(result.create());
	}

	// -- Interactions ------------------------------------------------------------------------------------------

	/**
	 * Right click with an item in hand. An empty pan takes one cookable item; a finished jam is collected with an
	 * empty jar (which turns into a full one). Anything else is ignored (PASS).
	 */
	public InteractionResult useItem(Level level, Player player, ItemStack held, boolean commit) {
		if (input == null) {
			if (!commit && level.isClientSide()) {
				return InteractionResult.PASS; // recipes live on the server
			}
			Optional<ItemStack> cooked = PanCooking.result(level, held);
			if (cooked.isEmpty()) {
				return InteractionResult.PASS;
			}
			if (commit) {
				input = ItemStackTemplate.fromNonEmptyStack(held, 1);
				result = ItemStackTemplate.fromNonEmptyStack(cooked.get());
				cookTime = 0;
				stage = 0;
				done = false;
				held.consume(1, player);
				level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F); // the sizzle
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		if (done && isJam() && held.getItem() instanceof JamJarItem && !held.has(KitchenComponents.JAM)) {
			if (commit) {
				ItemStack filled = result.create();
				held.consume(1, player);
				give(level, player, filled);
				level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
				clear();
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/**
	 * Empty hand: take the finished fried food. Before it is done, take the raw ingredient back (the original
	 * locked it in until done). Finished jam needs a jar, so an empty hand leaves it alone.
	 */
	public InteractionResult useEmptyHand(Level level, Player player, boolean commit) {
		if (input == null || (done && isJam())) {
			return InteractionResult.PASS;
		}
		if (commit) {
			give(level, player, (done ? result : input).create());
			clear();
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	private static void give(Level level, Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), stack);
		}
	}

	private void clear() {
		input = null;
		result = null;
		cookTime = 0;
		stage = 0;
		done = false;
	}

	// -- Cooking -----------------------------------------------------------------------------------------------

	/** One server tick (called by the block's ticker; tests call it directly). */
	public void serverTick(ServerLevel level) {
		if (input == null || done) {
			return;
		}
		cookTime++;
		int cookTicks = PanConfig.get(level.registryAccess()).cookTicks();
		int newStage = PanCooking.stage(cookTime, cookTicks);
		boolean finished = cookTime >= cookTicks;
		if (newStage != stage || finished) {
			stage = newStage;
			done = finished;
			changed();
		} else {
			setChanged();
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	// -- Removal -----------------------------------------------------------------------------------------------

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide() && input != null) {
			// Finished fried food is food; everything else (raw, or jam without a jar) gives the ingredient back.
			ItemStack drop = (done && !isJam() ? result : input).create();
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
			clear();
		}
		super.preRemoveSideEffects(pos, state);
	}

	// -- Persistence and sync ----------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.storeNullable("input", ItemStackTemplate.CODEC, input);
		output.storeNullable("result", ItemStackTemplate.CODEC, result);
		output.putInt("cook_time", cookTime);
		output.putInt("stage", stage);
		output.putBoolean("done", done);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.input = input.read("input", ItemStackTemplate.CODEC).orElse(null);
		this.result = input.read("result", ItemStackTemplate.CODEC).orElse(null);
		cookTime = input.getIntOr("cook_time", 0);
		stage = input.getIntOr("stage", 0);
		done = input.getBooleanOr("done", false);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
