package net.kitchenreborn.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.block.BoardBlock;
import net.kitchenreborn.block.KitchenCropBlock;
import net.kitchenreborn.block.WildPlantBlock;

public final class KitchenBlocks {
	public static final List<Block> CROPS = new ArrayList<>();
	public static final List<Block> WILD_PLANTS = new ArrayList<>();

	public static final Block TOMATO_CROP = crop("tomato_crop", () -> KitchenItems.TOMATO);
	public static final Block LETTUCE_CROP = crop("lettuce_crop", () -> KitchenItems.LETTUCE);
	public static final Block STRAWBERRY_CROP = crop("strawberry_crop", () -> KitchenItems.STRAWBERRY);
	public static final Block PEANUT_CROP = crop("peanut_crop", () -> KitchenItems.PEANUT);

	public static final Block WILD_TOMATO = wild("wild_tomato");
	public static final Block WILD_LETTUCE = wild("wild_lettuce");
	public static final Block WILD_STRAWBERRY = wild("wild_strawberry");
	public static final Block WILD_PEANUT = wild("wild_peanut");

	public static final Block BOARD = board();

	private static Block board() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kitchen.id("board"));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key)
				.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion();
		return Registry.register(BuiltInRegistries.BLOCK, key, new BoardBlock(properties));
	}

	private static Block crop(String name, Supplier<? extends ItemLike> seed) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kitchen.id(name));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key)
				.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak()
				.sound(SoundType.CROP).pushReaction(PushReaction.POPPED);
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, new KitchenCropBlock(properties, seed));
		CROPS.add(block);
		return block;
	}

	private static Block wild(String name) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kitchen.id(name));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key)
				.mapColor(MapColor.PLANT).noCollision().instabreak()
				.sound(SoundType.GRASS).pushReaction(PushReaction.POPPED);
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, new WildPlantBlock(properties));
		WILD_PLANTS.add(block);
		return block;
	}

	/** Forces class loading so every block registers during mod init (before items). */
	public static void register() {
	}

	private KitchenBlocks() { }
}
