package net.kitchenreborn.registry;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.blockentity.BoardBlockEntity;
import net.kitchenreborn.blockentity.CabinetBlockEntity;
import net.kitchenreborn.blockentity.OvenBlockEntity;
import net.kitchenreborn.blockentity.PanBlockEntity;
import net.kitchenreborn.blockentity.PlateBlockEntity;

public final class KitchenBlockEntities {
	public static final BlockEntityType<BoardBlockEntity> BOARD = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Kitchen.id("board"),
			new BlockEntityType<>(BoardBlockEntity::new, Set.of(KitchenBlocks.BOARD)));

	public static final BlockEntityType<PlateBlockEntity> PLATE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Kitchen.id("plate"),
			new BlockEntityType<>(PlateBlockEntity::new, Set.of(KitchenBlocks.PLATE)));

	public static final BlockEntityType<OvenBlockEntity> OVEN = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Kitchen.id("oven"),
			new BlockEntityType<>(OvenBlockEntity::new, Set.of(KitchenBlocks.OVEN)));

	public static final BlockEntityType<PanBlockEntity> PAN = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Kitchen.id("pan"),
			new BlockEntityType<>(PanBlockEntity::new, Set.of(KitchenBlocks.PAN)));

	public static final BlockEntityType<CabinetBlockEntity> CABINET = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Kitchen.id("kitchen_cabinet"),
			new BlockEntityType<>(CabinetBlockEntity::new, Set.of(KitchenBlocks.CABINET)));

	/** Forces class loading so the block entity types register during mod init. */
	public static void register() {
	}

	private KitchenBlockEntities() { }
}
