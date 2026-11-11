package net.kitchenreborn.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/**
 * A 0-7 age crop (like carrots). In the original the produce item itself was the seed
 * ({@code ItemSeedFood}), so planting is done by the produce's BlockItem.
 */
public class KitchenCropBlock extends CropBlock {
	private final Supplier<? extends ItemLike> seed;

	public KitchenCropBlock(Properties properties, Supplier<? extends ItemLike> seed) {
		super(properties);
		this.seed = seed;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return seed.get();
	}
}
