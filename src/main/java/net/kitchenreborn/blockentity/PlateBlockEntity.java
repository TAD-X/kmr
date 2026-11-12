package net.kitchenreborn.blockentity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import net.kitchenreborn.food.PlateContents;
import net.kitchenreborn.registry.KitchenBlockEntities;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenItems;

/**
 * A plate: any items stacked one at a time, bottom to top. A sandwich (a stack with the layered_food component)
 * only goes on an empty plate, as in the original. Interactions take a {@code commit} flag: the client calls
 * them with {@code false} to predict the result (swing animation) without changing anything.
 */
public class PlateBlockEntity extends BlockEntity {
	/** Design decision D5: the original was unlimited (and laggy); the port caps a plate at 32 items. */
	public static final int MAX_ITEMS = 32;

	private final List<ItemStackTemplate> items = new ArrayList<>();
	/** Set when the contents were handed out with the block's drops, so removing the block does not drop them twice. */
	private boolean contentsHandled;
	private boolean exploding;

	public PlateBlockEntity(BlockPos pos, BlockState state) {
		super(KitchenBlockEntities.PLATE, pos, state);
	}

	public List<ItemStackTemplate> items() {
		return items;
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	// -- Interactions ------------------------------------------------------------------------------------------

	/** Why an item cannot go on the plate. A refused item makes the plate act full (see {@link #useItem}). */
	public enum Refusal {
		FULL, SHULKER_BOX, HOLDS_CONTENTS, UNSTACKABLE
	}

	/** Anything that could carry more items inside it: a loaded plate, a filled bundle (or shulker box). */
	public static boolean holdsContents(ItemStack stack) {
		PlateContents plate = stack.get(KitchenComponents.PLATE_CONTENTS);
		if (plate != null && !plate.items().isEmpty()) {
			return true;
		}
		BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null && !bundle.isEmpty()) {
			return true;
		}
		ItemContainerContents container = stack.get(DataComponents.CONTAINER);
		return container != null && container.nonEmptyItemCopyStream().findAny().isPresent();
	}

	/**
	 * The rule that stops {@code held} from going on this plate, or null if it may. Unstackable items (swords, tools,
	 * a sandwich, a loaded plate...) act like the sandwich always did: they only go on an empty plate, so a plate
	 * can never be used to squeeze many unstackable items into one inventory slot.
	 */
	public @Nullable Refusal refusal(ItemStack held) {
		if (items.size() >= MAX_ITEMS) {
			return Refusal.FULL;
		}
		if (held.is(ItemTags.SHULKER_BOXES)) {
			return Refusal.SHULKER_BOX; // never on a plate
		}
		if (holdsContents(held)) {
			return Refusal.HOLDS_CONTENTS; // loaded plates, filled bundles
		}
		if (held.getMaxStackSize() == 1 && !items.isEmpty()) {
			return Refusal.UNSTACKABLE;
		}
		return null;
	}

	/**
	 * Right click with an item in hand (not sneaking): put one on the plate. A refused item PASSes, so the plate
	 * acts as if it were full: the block then lets an empty-handed click take the top item off.
	 */
	public InteractionResult useItem(Player player, ItemStack held, boolean commit) {
		if (held.isEmpty() || refusal(held) != null) {
			return InteractionResult.PASS;
		}
		if (commit) {
			items.add(ItemStackTemplate.fromNonEmptyStack(held, 1));
			held.consume(1, player);
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	/** Right click with an empty hand (not sneaking): take the top item. */
	public InteractionResult useEmptyHand(Player player, boolean commit) {
		if (items.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (commit) {
			give(player, items.removeLast().create());
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	/** Sneak + right click: pick the plate up with everything on it kept in the item. */
	public InteractionResult pickUp(Level level, Player player, boolean commit) {
		if (commit) {
			ItemStack plate = new ItemStack(KitchenItems.PLATE);
			plate.applyComponents(collectComponents());
			items.clear(); // now part of the item, not to be dropped again
			level.removeBlock(worldPosition, false);
			give(player, plate);
		}
		return InteractionResult.SUCCESS;
	}

	private void give(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
		}
	}

	/** Called by the block's drop logic after it took the contents along (in the item or as loose stacks). */
	public void markContentsHandled() {
		contentsHandled = true;
	}

	/** The plate is being blown up: its contents drop as loose items, not inside the plate. */
	public void markExploding() {
		exploding = true;
	}

	public boolean isExploding() {
		return exploding;
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
		// Normal breaking hands the contents out with the drops (see PlateBlock#getDrops); anything else that
		// removes the plate (setblock, a piston...) must not lose them.
		if (level != null && !level.isClientSide() && !contentsHandled) {
			for (ItemStackTemplate item : items) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.create());
			}
			items.clear();
		}
		super.preRemoveSideEffects(pos, state);
	}

	// -- Persistence, item component and sync ------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("items", ItemStackTemplate.CODEC.listOf(), List.copyOf(items));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		items.addAll(input.read("items", ItemStackTemplate.CODEC.listOf()).orElse(List.of()));
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!items.isEmpty()) {
			components.set(KitchenComponents.PLATE_CONTENTS, new PlateContents(List.copyOf(items)));
			// A plate with something on it is an unstackable item (no stacks of identical loaded plates).
			components.set(DataComponents.MAX_STACK_SIZE, 1);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		items.clear();
		PlateContents contents = components.get(KitchenComponents.PLATE_CONTENTS);
		if (contents != null) {
			items.addAll(contents.items());
		}
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
