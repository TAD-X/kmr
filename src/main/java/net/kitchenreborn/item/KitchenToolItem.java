package net.kitchenreborn.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/** A crafting tool (knife, mortar and pestle) that stays in the crafting grid, undamaged (design D3). */
public class KitchenToolItem extends Item {
	public KitchenToolItem(Properties properties) {
		super(properties);
	}

	// Fabric's per-stack remainder hook (the vanilla property cannot reference its own item).
	@Override
	public ItemStackTemplate getCraftingRemainder(ItemStack stack) {
		return new ItemStackTemplate(this);
	}
}
