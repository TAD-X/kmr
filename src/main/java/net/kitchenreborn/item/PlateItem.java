package net.kitchenreborn.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;

/**
 * The plate item. Vanilla decides what may go inside shulker boxes and bundles per item type, not per stack, so a
 * plate can never go inside one (the loaded ones are the reason: a plate holding a shulker box holding a plate...).
 */
public class PlateItem extends BlockItem {
	public PlateItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}
}
