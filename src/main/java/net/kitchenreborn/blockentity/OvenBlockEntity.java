package net.kitchenreborn.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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

import net.kitchenreborn.block.OvenBlock;
import net.kitchenreborn.oven.OvenConfig;
import net.kitchenreborn.oven.OvenCooking;
import net.kitchenreborn.registry.KitchenBlockEntities;
import net.kitchenreborn.registry.KitchenItems;

/**
 * The oven, a port of the original's state machine. Open: food (4 slots of up to 4 of one kind) and fuel can be
 * added and taken out. Closed with fuel: cooks. {@code cookTime} counts ticks since closing; at the configured cook
 * time the food is cooked, at the burn time everything is burnt meat. Opening resets the timer and the fuel is
 * lost (as in the original); food that is already cooked stays cooked.
 *
 * <p>Every interaction takes a {@code commit} flag: the client calls with {@code false} to predict the swing.
 */
public class OvenBlockEntity extends BlockEntity {
	public static final int SLOTS = 4;
	public static final int SLOT_SIZE = 4;

	/** What state the food in the oven is in; matches the original's RAW / COOKED / BURNT. */
	public enum Stage { RAW, COOKED, BURNT }

	private final List<ItemStackTemplate> slots = new ArrayList<>();
	private boolean hasFuel;
	private boolean cooking;
	private int cookTime;
	private Stage stage = Stage.RAW;

	// -- Client-side hatch animation (port of the original's OpeningAnimation: 0 to 65 degrees, 10 per tick) --
	public static final float HATCH_MAX = 65.0F;
	public static final float HATCH_STEP = 10.0F;
	private float hatchAngle = -1.0F;
	private float hatchPrevAngle = -1.0F;

	public OvenBlockEntity(BlockPos pos, BlockState state) {
		super(KitchenBlockEntities.OVEN, pos, state);
	}

	public List<ItemStackTemplate> slots() {
		return slots;
	}

	public boolean hasFuel() {
		return hasFuel;
	}

	public boolean isCooking() {
		return cooking;
	}

	public int cookTime() {
		return cookTime;
	}

	public Stage stage() {
		return stage;
	}

	public boolean isOpen() {
		return getBlockState().getValue(OvenBlock.OPEN);
	}

	/**
	 * Client tick: move the hatch toward the open or closed angle, a step per tick. The first tick after the block
	 * entity appears (a chunk loading, joining a world) snaps to the current state, so only changes animate.
	 */
	public void clientTick() {
		float target = isOpen() ? HATCH_MAX : 0.0F;
		if (hatchAngle < 0.0F) {
			hatchAngle = hatchPrevAngle = target;
			return;
		}
		hatchPrevAngle = hatchAngle;
		hatchAngle = hatchAngle < target ? Math.min(target, hatchAngle + HATCH_STEP) : Math.max(target, hatchAngle - HATCH_STEP);
	}

	/** The hatch angle in degrees, interpolated between ticks. */
	public float hatchAngle(float partialTick) {
		float angle = hatchAngle < 0.0F ? (isOpen() ? HATCH_MAX : 0.0F) : hatchAngle;
		float prev = hatchPrevAngle < 0.0F ? angle : hatchPrevAngle;
		return Math.clamp(prev + (angle - prev) * partialTick, 0.0F, HATCH_MAX);
	}

	// -- Interactions ------------------------------------------------------------------------------------------

	/** Sneak + empty hand: open or close. Opening resets the cook timer and uses up the fuel (original behaviour). */
	public InteractionResult toggle(Level level, boolean commit) {
		if (commit) {
			boolean open = !isOpen();
			if (open) {
				hasFuel = false;
				cooking = false;
				cookTime = 0;
				stage = Stage.RAW;
			} else {
				cookTime = 0;
			}
			level.setBlock(worldPosition, getBlockState().setValue(OvenBlock.OPEN, open).setValue(OvenBlock.LIT, false), Block.UPDATE_ALL);
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Right click with an item. While closed the oven ignores it (PASS: the item is used normally). While open the
	 * click is always consumed: fuel or food goes in, anything else is refused (FAIL).
	 */
	public InteractionResult useItem(Level level, Player player, ItemStack held, boolean commit) {
		if (!isOpen()) {
			return InteractionResult.PASS;
		}
		if (!commit && level.isClientSide()) {
			return InteractionResult.SUCCESS; // the server decides; the client just swings
		}
		if (held.has(DataComponents.COOKING_FUEL)) {
			if (hasFuel) {
				return InteractionResult.FAIL;
			}
			if (commit) {
				hasFuel = true;
				ItemStack remainder = remainderOf(held);
				held.consume(1, player);
				if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
					Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), remainder);
				}
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		if (OvenCooking.result(level, held).isEmpty() || !canTake(held)) {
			return InteractionResult.FAIL;
		}
		if (commit) {
			add(held);
			held.consume(1, player);
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	private static ItemStack remainderOf(ItemStack held) {
		ItemStackTemplate remainder = held.getItem().getCraftingRemainder();
		return remainder == null ? ItemStack.EMPTY : remainder.create();
	}

	/** Whether one more of {@code held} fits: on a stack of the same kind with room, or in a free slot. */
	public boolean canTake(ItemStack held) {
		for (ItemStackTemplate slot : slots) {
			if (sameKind(slot, held) && slot.count() < SLOT_SIZE) {
				return true;
			}
		}
		return slots.size() < SLOTS;
	}

	private void add(ItemStack held) {
		for (int i = 0; i < slots.size(); i++) {
			ItemStackTemplate slot = slots.get(i);
			if (sameKind(slot, held) && slot.count() < SLOT_SIZE) {
				slots.set(i, slot.withCount(slot.count() + 1));
				return;
			}
		}
		slots.add(ItemStackTemplate.fromNonEmptyStack(held, 1));
	}

	private static boolean sameKind(ItemStackTemplate slot, ItemStack held) {
		return ItemStack.isSameItemSameComponents(slot.create(), held);
	}

	/** Empty hand while open: take the first stack out. */
	public InteractionResult takeStack(Level level, Player player, boolean commit) {
		if (!isOpen() || slots.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (commit) {
			ItemStack stack = slots.removeFirst().create();
			if (!player.getInventory().add(stack)) {
				Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), stack);
			}
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	// -- The cooking cycle -------------------------------------------------------------------------------------

	private boolean canCook(Level level) {
		for (ItemStackTemplate slot : slots) {
			if (OvenCooking.result(level, slot.create()).isPresent()) {
				return true;
			}
		}
		return false;
	}

	/** One server tick (called by the block's ticker; tests call it directly). */
	public void serverTick(ServerLevel level) {
		if (isOpen() || !hasFuel) {
			if (cookTime != 0 || cooking) {
				cookTime = 0;
				cooking = false;
			}
			return;
		}
		boolean wasCooking = cooking;
		Stage before = stage;
		if (!cooking) {
			if (canCook(level)) {
				cooking = true;
				cookTime = 1;
			} else {
				cookTime = 0;
			}
		} else if (stage != Stage.BURNT) {
			cookTime++;
		}
		OvenConfig config = OvenConfig.get(level.registryAccess());
		if (cooking && stage == Stage.RAW && cookTime >= config.cookTicks()) {
			stage = Stage.COOKED;
			cookItems(level);
		}
		if (cooking && stage == Stage.COOKED && cookTime >= config.burnTicks()) {
			stage = Stage.BURNT;
			burnItems();
		}
		if (wasCooking != cooking) {
			level.setBlock(worldPosition, getBlockState().setValue(OvenBlock.LIT, cooking), Block.UPDATE_ALL);
		}
		if (wasCooking != cooking || before != stage) {
			changed();
		} else {
			setChanged();
		}
	}

	private void cookItems(Level level) {
		for (int i = 0; i < slots.size(); i++) {
			ItemStackTemplate slot = slots.get(i);
			Optional<ItemStack> result = OvenCooking.result(level, slot.create());
			if (result.isPresent()) {
				ItemStackTemplate cooked = ItemStackTemplate.fromNonEmptyStack(result.get());
				slots.set(i, cooked.withCount(cooked.count() * slot.count()));
			}
		}
	}

	private void burnItems() {
		for (int i = 0; i < slots.size(); i++) {
			slots.set(i, new ItemStackTemplate(KitchenItems.BURNT_MEAT, slots.get(i).count()));
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
		if (level != null && !level.isClientSide()) {
			for (ItemStackTemplate slot : slots) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, slot.create());
			}
			slots.clear();
		}
		super.preRemoveSideEffects(pos, state);
	}

	// -- Persistence and sync ----------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("slots", ItemStackTemplate.CODEC.listOf(), List.copyOf(slots));
		output.putBoolean("has_fuel", hasFuel);
		output.putBoolean("cooking", cooking);
		output.putInt("cook_time", cookTime);
		output.putInt("stage", stage.ordinal());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		slots.addAll(input.read("slots", ItemStackTemplate.CODEC.listOf()).orElse(List.of()));
		hasFuel = input.getBooleanOr("has_fuel", false);
		cooking = input.getBooleanOr("cooking", false);
		cookTime = input.getIntOr("cook_time", 0);
		stage = Stage.values()[Math.clamp(input.getIntOr("stage", 0), 0, Stage.values().length - 1)];
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
