package net.kitchenreborn.gametest;

import java.lang.reflect.Method;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.blockentity.PlateBlockEntity;
import net.kitchenreborn.food.LayeredFood;
import net.kitchenreborn.food.PlateContents;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenItems;

/** Logic tests for the plate (milestone M5): adding, removing, picking up, breaking, explosions. */
public class PlateGameTest implements CustomTestMethodInvoker {
	private static final BlockPos PLATE = new BlockPos(1, 1, 1);

	@Override
	public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
		helper.setBlock(PLATE, KitchenBlocks.PLATE);
		method.invoke(this, helper);
	}

	private static PlateBlockEntity plate(GameTestHelper helper) {
		return helper.getBlockEntity(PLATE, PlateBlockEntity.class);
	}

	private static void put(GameTestHelper helper, Player player, Item item) {
		ItemStack held = new ItemStack(item, 4);
		InteractionResult result = plate(helper).useItem(player, held, true);
		helper.assertValueEqual(InteractionResult.SUCCESS, result, "adding " + item);
		helper.assertValueEqual(3, held.getCount(), "one item is used up");
	}

	private static ItemStack sandwich() {
		ItemStack sandwich = new ItemStack(KitchenItems.SANDWICH);
		sandwich.set(KitchenComponents.LAYERED_FOOD, new LayeredFood(Kitchen.id("sandwich"), List.of(
				new ItemStackTemplate(KitchenItems.BREAD_SLICE), new ItemStackTemplate(KitchenItems.BACON),
				new ItemStackTemplate(KitchenItems.BREAD_SLICE))));
		return sandwich;
	}

	private static int itemsNearby(GameTestHelper helper, Item item) {
		int total = 0;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, PLATE, 6.0)) {
			if (entity.getItem().is(item)) {
				total += entity.getItem().getCount();
			}
		}
		return total;
	}

	private static boolean has(Player player, Item item) {
		return player.getInventory().contains(stack -> stack.is(item));
	}

	@GameTest
	public void addsOneItemAtATimeAndRemovesTheTop(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		put(helper, player, KitchenItems.BACON);
		put(helper, player, KitchenItems.CHEESE_SLICE);
		put(helper, player, KitchenItems.TOMATO_SLICE);
		helper.assertValueEqual(3, plate(helper).items().size(), "items on the plate");

		helper.assertValueEqual(InteractionResult.SUCCESS, plate(helper).useEmptyHand(player, true), "take top");
		helper.assertValueEqual(2, plate(helper).items().size(), "items left");
		helper.assertTrue(has(player, KitchenItems.TOMATO_SLICE), "the top item went to the player");
		helper.assertTrue(plate(helper).items().get(1).is(KitchenItems.CHEESE_SLICE), "cheese is now on top");

		plate(helper).useEmptyHand(player, true);
		plate(helper).useEmptyHand(player, true);
		helper.assertValueEqual(InteractionResult.PASS, plate(helper).useEmptyHand(player, true), "empty plate");
		helper.succeed();
	}

	@GameTest
	public void capsAtThirtyTwoItems(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (int i = 0; i < PlateBlockEntity.MAX_ITEMS; i++) {
			put(helper, player, KitchenItems.BACON);
		}
		ItemStack held = new ItemStack(KitchenItems.BACON, 4);
		helper.assertValueEqual(InteractionResult.PASS, plate(helper).useItem(player, held, true), "33rd item");
		helper.assertValueEqual(4, held.getCount(), "nothing consumed");
		helper.assertValueEqual(32, plate(helper).items().size(), "cap");
		helper.succeed();
	}

	@GameTest
	public void predictionDoesNotChangeThePlate(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack held = new ItemStack(KitchenItems.BACON, 2);
		helper.assertValueEqual(InteractionResult.SUCCESS, plate(helper).useItem(player, held, false), "predicted add");
		helper.assertTrue(plate(helper).isEmpty(), "plate unchanged");
		helper.assertValueEqual(2, held.getCount(), "stack unchanged");
		helper.succeed();
	}

	@GameTest
	public void aSandwichOnlyGoesOnAnEmptyPlate(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertValueEqual(InteractionResult.SUCCESS, plate(helper).useItem(player, sandwich(), true), "sandwich on empty plate");
		// Anything may go on top of it, but a second sandwich may not.
		put(helper, player, KitchenItems.CHEESE_SLICE);
		helper.assertValueEqual(InteractionResult.PASS, plate(helper).useItem(player, sandwich(), true), "second sandwich");
		helper.assertValueEqual(2, plate(helper).items().size(), "items");

		plate(helper).useEmptyHand(player, true);
		plate(helper).useEmptyHand(player, true);
		put(helper, player, KitchenItems.BACON);
		helper.assertValueEqual(InteractionResult.PASS, plate(helper).useItem(player, sandwich(), true), "sandwich on a used plate");
		helper.succeed();
	}

	@GameTest
	public void pickUpAndPlaceKeepsTheContents(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		plate(helper).useItem(player, sandwich(), true);
		put(helper, player, KitchenItems.CHEESE_SLICE);
		put(helper, player, KitchenItems.CHEESE_SLICE);
		List<ItemStackTemplate> before = List.copyOf(plate(helper).items());

		helper.assertValueEqual(InteractionResult.SUCCESS, plate(helper).pickUp(helper.getLevel(), player, true), "pick up");
		helper.assertTrue(helper.getBlockState(PLATE).isAir(), "plate block is gone");
		helper.assertTrue(itemsNearby(helper, KitchenItems.CHEESE_SLICE) == 0, "nothing was dropped loose");

		ItemStack picked = null;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(KitchenItems.PLATE)) {
				picked = stack;
			}
		}
		helper.assertTrue(picked != null, "the player holds the plate item");
		PlateContents contents = picked.get(KitchenComponents.PLATE_CONTENTS);
		helper.assertTrue(contents != null && contents.items().equals(before), "contents are in the item");

		// Placing it back restores the contents.
		helper.setBlock(PLATE, KitchenBlocks.PLATE);
		plate(helper).applyComponentsFromItemStack(picked);
		helper.assertTrue(plate(helper).items().equals(before), "contents restored on placing");
		helper.succeed();
	}

	@GameTest
	public void breakingKeepsTheContentsInTheItem(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		put(helper, player, KitchenItems.BACON);
		put(helper, player, KitchenItems.CHEESE_SLICE);

		helper.getLevel().destroyBlock(helper.absolutePos(PLATE), true);

		helper.assertTrue(helper.getBlockState(PLATE).isAir(), "plate block is gone");
		helper.assertTrue(itemsNearby(helper, KitchenItems.BACON) == 0, "bacon is not loose");
		boolean found = false;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, PLATE, 6.0)) {
			PlateContents contents = entity.getItem().get(KitchenComponents.PLATE_CONTENTS);
			if (entity.getItem().is(KitchenItems.PLATE) && contents != null && contents.items().size() == 2) {
				found = true;
			}
		}
		helper.assertTrue(found, "the dropped plate carries its two items");
		helper.succeed();
	}

	@GameTest
	public void explosionsDropTheContentsLoose(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		put(helper, player, KitchenItems.BACON);
		put(helper, player, KitchenItems.CHEESE_SLICE);
		BlockPos at = helper.absolutePos(PLATE);

		helper.getLevel().explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.TNT);

		helper.assertTrue(helper.getBlockState(PLATE).isAir(), "plate block was destroyed");
		StringBuilder seen = new StringBuilder();
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, PLATE, 6.0)) {
			seen.append(entity.getItem()).append(' ');
		}
		helper.assertTrue(itemsNearby(helper, KitchenItems.BACON) == 1, "bacon dropped loose; items: " + seen);
		helper.assertTrue(itemsNearby(helper, KitchenItems.CHEESE_SLICE) == 1, "cheese dropped loose");
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, PLATE, 6.0)) {
			helper.assertTrue(!entity.getItem().has(KitchenComponents.PLATE_CONTENTS), "no plate carries contents");
		}
		helper.succeed();
	}

	@GameTest
	public void removingThePlateAnyOtherWayDropsTheContents(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		put(helper, player, KitchenItems.BACON);
		put(helper, player, KitchenItems.BACON);

		helper.getLevel().removeBlock(helper.absolutePos(PLATE), false);

		helper.assertValueEqual(2, itemsNearby(helper, KitchenItems.BACON), "contents dropped");
		helper.succeed();
	}

	/** The tooltip merges identical neighbours and lists the top first. */
	@GameTest
	public void tooltipListsTheContents(GameTestHelper helper) {
		ItemStack plate = new ItemStack(KitchenItems.PLATE);
		plate.set(KitchenComponents.PLATE_CONTENTS, new PlateContents(List.of(new ItemStackTemplate(KitchenItems.BACON),
				new ItemStackTemplate(KitchenItems.BACON), new ItemStackTemplate(KitchenItems.CHEESE_SLICE))));
		List<String> lines = new java.util.ArrayList<>();
		plate.get(KitchenComponents.PLATE_CONTENTS).addToTooltip(Item.TooltipContext.EMPTY,
				line -> lines.add(line.getString()), net.minecraft.world.item.TooltipFlag.NORMAL, plate.getComponents());
		helper.assertValueEqual(2, lines.size(), "lines: " + lines);
		helper.assertTrue(lines.get(1).startsWith("2x"), "bacon run merged: " + lines);
		helper.succeed();
	}
}
