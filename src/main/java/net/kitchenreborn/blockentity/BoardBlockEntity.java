package net.kitchenreborn.blockentity;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.kitchenreborn.food.Foods;
import net.kitchenreborn.food.LayerSource;
import net.kitchenreborn.food.LayerStack;
import net.kitchenreborn.recipe.CuttingRecipe;
import net.kitchenreborn.registry.KitchenBlockEntities;
import net.kitchenreborn.registry.KitchenRecipes;
import net.kitchenreborn.registry.KitchenTags;

/**
 * The cutting board: either a food in progress (a {@link LayerStack}) or one whole ingredient waiting to be
 * cut with a knife. Every interaction takes a {@code commit} flag: the client calls it with {@code false} to
 * predict the result (swing animation) without changing anything.
 */
public class BoardBlockEntity extends BlockEntity {
	private final LayerStack stack = new LayerStack();
	private @Nullable ItemStackTemplate workpiece;

	public BoardBlockEntity(BlockPos pos, BlockState state) {
		super(KitchenBlockEntities.BOARD, pos, state);
	}

	public LayerStack stack() {
		return stack;
	}

	public @Nullable ItemStackTemplate workpiece() {
		return workpiece;
	}

	// -- Interactions ------------------------------------------------------------------------------------------

	/** Right click with an item in hand (not sneaking). */
	public InteractionResult useItem(Level level, Player player, ItemStack held, boolean commit) {
		RegistryAccess access = level.registryAccess();
		Block station = getBlockState().getBlock();

		if (held.is(KitchenTags.KNIVES)) {
			return useKnife(level, commit);
		}
		if (held.getItem() instanceof LayerSource source) {
			Optional<Item> layer = source.peekLayer(held);
			if (layer.isPresent() && workpiece == null
					&& stack.check(access, station, layer.get()) == LayerStack.AddResult.OK) {
				if (commit) {
					stack.add(access, station, layer.get());
					source.consumeServing(held); // the jar stays in hand, one serving lighter
					changed();
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		if (workpiece == null && stack.check(access, station, held.getItem()) == LayerStack.AddResult.OK) {
			if (commit) {
				stack.add(access, station, held.getItem());
				held.consume(1, player);
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		if (workpiece == null && stack.isEmpty() && held.is(KitchenTags.CUTTABLE)) {
			if (commit) {
				workpiece = new ItemStackTemplate(held.getItem());
				held.consume(1, player);
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private InteractionResult useKnife(Level level, boolean commit) {
		if (!stack.isEmpty()) {
			if (stack.pendingSpread() > 0) { // spreading butter
				if (commit) {
					stack.spread();
					changed();
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		if (workpiece == null) {
			return InteractionResult.PASS;
		}
		if (!commit) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.PASS;
		}
		Optional<RecipeHolder<CuttingRecipe>> recipe = server.recipeAccess()
				.getRecipeFor(KitchenRecipes.CUTTING, new SingleRecipeInput(workpiece.create()), server);
		if (recipe.isEmpty()) {
			return InteractionResult.PASS;
		}
		ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(workpiece.create()));
		workpiece = null;
		Block.popResource(level, worldPosition, result);
		// The knife chops: a short sharp wood sound.
		level.playSound(null, worldPosition, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
		changed();
		return InteractionResult.SUCCESS;
	}

	/** Right click with an empty hand (not sneaking): take the top layer, or the whole ingredient, back. */
	public InteractionResult useEmptyHand(Level level, boolean commit) {
		if (workpiece != null) {
			if (commit) {
				Block.popResource(level, worldPosition, workpiece.create());
				workpiece = null;
				changed();
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (commit) {
			stack.removeTop(level.registryAccess()).ifPresent(drop -> {
				if (!drop.isEmpty()) {
					Block.popResource(level, worldPosition, drop);
				}
			});
			changed();
		}
		return InteractionResult.SUCCESS;
	}

	/** Sneak + right click: finish the food into an item. */
	public InteractionResult finish(Level level, Player player, boolean commit) {
		if (stack.isEmpty()) {
			return InteractionResult.PASS;
		}
		LayerStack.FinishStatus status = stack.checkFinish(level.registryAccess());
		if (status != LayerStack.FinishStatus.OK) {
			if (commit) {
				player.sendOverlayMessage(Component.translatable("message.kitchen.finish." + status.name().toLowerCase()));
			}
			return InteractionResult.PASS;
		}
		if (commit) {
			LayerStack.FinishResult result = stack.finish(level.registryAccess());
			Block.popResource(level, worldPosition, result.stack());
			changed();
		}
		return InteractionResult.SUCCESS;
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
			if (workpiece != null) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, workpiece.create());
				workpiece = null;
			}
			Optional<ItemStack> drop;
			while ((drop = stack.removeTop(level.registryAccess())).isPresent()) {
				if (!drop.get().isEmpty()) {
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop.get());
				}
			}
		}
		super.preRemoveSideEffects(pos, state);
	}

	// -- Persistence and sync ----------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("layers", ItemStackTemplate.CODEC.listOf(), List.copyOf(stack.layers()));
		output.putInt("pending_spread", stack.pendingSpread());
		output.storeNullable("food_type", Identifier.CODEC, stack.type().orElse(null));
		output.storeNullable("workpiece", ItemStackTemplate.CODEC, workpiece);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		List<ItemStackTemplate> layers = input.read("layers", ItemStackTemplate.CODEC.listOf()).orElse(List.of());
		Identifier type = input.read("food_type", Identifier.CODEC).orElse(null);
		stack.restore(type, layers, input.getIntOr("pending_spread", 0));
		workpiece = input.read("workpiece", ItemStackTemplate.CODEC).orElse(null);
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
