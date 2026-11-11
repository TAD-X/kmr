package net.kitchenreborn.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;

import net.kitchenreborn.registry.KitchenComponents;

/**
 * The state machine of a station holding a modular food in progress. Pure game logic with no world access,
 * so it is shared by every station and covered by game tests. All rules come from layer and food-type data:
 * max/min layers, end roles, "place on", spread clicks.
 */
public final class LayerStack {
	private final List<ItemStackTemplate> layers = new ArrayList<>();
	private Identifier type;
	private int pendingSpread;

	public List<ItemStackTemplate> layers() {
		return layers;
	}

	public boolean isEmpty() {
		return layers.isEmpty();
	}

	public Optional<Identifier> type() {
		return Optional.ofNullable(type);
	}

	/** Knife clicks still needed on the top layer before anything may be placed on it. */
	public int pendingSpread() {
		return pendingSpread;
	}

	public void restore(Identifier type, List<ItemStackTemplate> layers, int pendingSpread) {
		this.layers.clear();
		this.layers.addAll(layers);
		this.type = layers.isEmpty() ? null : type;
		this.pendingSpread = pendingSpread;
	}

	public void clear() {
		layers.clear();
		type = null;
		pendingSpread = 0;
	}

	/** Why an item can or cannot be added right now. */
	public enum AddResult { OK, NOT_AN_INGREDIENT, WRONG_FOOD, FULL, STILL_SPREADING, CANNOT_PLACE_ON_THIS }

	public AddResult check(HolderLookup.Provider access, Block station, Item item) {
		Optional<LayerDefinition> def = Foods.layerFor(access, item);
		if (def.isEmpty()) {
			return AddResult.NOT_AN_INGREDIENT;
		}
		if (pendingSpread > 0) {
			return AddResult.STILL_SPREADING;
		}
		Identifier foodType = layers.isEmpty() ? Foods.foodTypeFor(access, station, def.get()).orElse(null) : type;
		if (foodType == null || !def.get().foods().contains(foodType)) {
			return AddResult.WRONG_FOOD;
		}
		FoodType food = Foods.foodType(access, foodType).orElse(null);
		if (food == null) {
			return AddResult.WRONG_FOOD;
		}
		if (layers.size() >= food.maxLayers()) {
			return AddResult.FULL;
		}
		if (!def.get().placeOn().isEmpty()) {
			Item top = layers.isEmpty() ? null : layers.getLast().item().value();
			if (top == null || !def.get().placeOn().contains(top)) {
				return AddResult.CANNOT_PLACE_ON_THIS;
			}
		}
		return AddResult.OK;
	}

	public boolean add(HolderLookup.Provider access, Block station, Item item) {
		if (check(access, station, item) != AddResult.OK) {
			return false;
		}
		LayerDefinition def = Foods.layerFor(access, item).orElseThrow();
		if (layers.isEmpty()) {
			type = Foods.foodTypeFor(access, station, def).orElseThrow();
		}
		layers.add(new ItemStackTemplate(item));
		pendingSpread = def.spreadClicks();
		return true;
	}

	/** One knife click on the top layer; returns true if it was still being spread. */
	public boolean spread() {
		if (pendingSpread > 0) {
			pendingSpread--;
			return true;
		}
		return false;
	}

	/**
	 * Removes the top layer. The returned stack is what to hand back (empty when the layer is consumed, like
	 * jam, or like butter that was already partly spread). Empty optional if there was nothing to remove.
	 */
	public Optional<ItemStack> removeTop(HolderLookup.Provider access) {
		if (layers.isEmpty()) {
			return Optional.empty();
		}
		ItemStackTemplate removed = layers.removeLast();
		LayerDefinition def = Foods.layerFor(access, removed.item().value()).orElse(null);
		boolean untouchedSpread = def == null || def.spreadClicks() == 0 || pendingSpread == def.spreadClicks();
		boolean drops = def == null || (def.dropItem() && untouchedSpread);
		pendingSpread = 0;
		if (layers.isEmpty()) {
			type = null;
		}
		return Optional.of(drops ? removed.create() : ItemStack.EMPTY);
	}

	public enum FinishStatus { OK, EMPTY, TOO_FEW_LAYERS, BAD_ENDS }

	public record FinishResult(FinishStatus status, ItemStack stack) { }

	/** Whether the food could be finished right now, without changing anything. */
	public FinishStatus checkFinish(HolderLookup.Provider access) {
		if (layers.isEmpty() || type == null) {
			return FinishStatus.EMPTY;
		}
		FoodType food = Foods.foodType(access, type).orElse(null);
		if (food == null) {
			return FinishStatus.EMPTY;
		}
		if (layers.size() < food.minLayers()) {
			return FinishStatus.TOO_FEW_LAYERS;
		}
		if (!hasRole(access, layers.getFirst(), food.bottomRole()) || !hasRole(access, layers.getLast(), food.topRole())) {
			return FinishStatus.BAD_ENDS;
		}
		return FinishStatus.OK;
	}

	/** Finishes the food: validates the end rules, builds the item and empties the station. */
	public FinishResult finish(HolderLookup.Provider access) {
		FinishStatus status = checkFinish(access);
		if (status != FinishStatus.OK) {
			return new FinishResult(status, ItemStack.EMPTY);
		}
		FoodType food = Foods.foodType(access, type).orElseThrow();
		ItemStack result = new ItemStack(food.output());
		List<ItemStackTemplate> finished = List.copyOf(layers);
		result.set(KitchenComponents.LAYERED_FOOD, new LayeredFood(type, finished));
		result.set(DataComponents.FOOD, Foods.nutrition(access, finished));
		Foods.name(access, food, finished).ifPresent(name -> result.set(DataComponents.ITEM_NAME, name));
		clear();
		return new FinishResult(FinishStatus.OK, result);
	}

	private static boolean hasRole(HolderLookup.Provider access, ItemStackTemplate layer, LayerRole role) {
		return Foods.layerFor(access, layer.item().value()).map(def -> def.role() == role).orElse(false);
	}
}
