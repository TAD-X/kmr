package net.kitchenreborn.registry;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.blockentity.BoardBlockEntity;

public final class KitchenBlockEntities {
	public static final BlockEntityType<BoardBlockEntity> BOARD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Kitchen.id("board"),
			new BlockEntityType<>(BoardBlockEntity::new, Set.of(KitchenBlocks.BOARD)));

	/** Forces class loading so the block entity types register during mod init. */
	public static void register() {
	}

	private KitchenBlockEntities() { }
}
