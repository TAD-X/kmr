package net.kitchenreborn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A wild plant that generates in the world and drops two of its produce when broken. */
public class WildPlantBlock extends VegetationBlock {
	// The original's bounds: x/z 0.3-0.8, height 0.6.
	private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 13.0, 10.0, 13.0);

	public WildPlantBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
