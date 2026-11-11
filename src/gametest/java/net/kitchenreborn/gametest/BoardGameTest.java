package net.kitchenreborn.gametest;

import java.lang.reflect.Method;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.kitchenreborn.blockentity.BoardBlockEntity;
import net.kitchenreborn.food.JamContents;
import net.kitchenreborn.food.LayerStack;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenItems;

/** Logic tests for the cutting board and the modular food core (milestone M3a). */
public class BoardGameTest implements CustomTestMethodInvoker {
	private static final BlockPos BOARD = new BlockPos(0, 0, 0);

	@Override
	public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
		helper.setBlock(BOARD, KitchenBlocks.BOARD);
		method.invoke(this, helper);
	}

	// -- helpers ---------------------------------------------------------------------------------------------

	private static BoardBlockEntity board(GameTestHelper helper) {
		return helper.getBlockEntity(BOARD, BoardBlockEntity.class);
	}

	private static InteractionResult put(GameTestHelper helper, Player player, Item item) {
		return board(helper).useItem(helper.getLevel(), player, new ItemStack(item), true);
	}

	private static void layers(GameTestHelper helper, Player player, Item... items) {
		for (Item item : items) {
			InteractionResult result = put(helper, player, item);
			if (result != InteractionResult.SUCCESS) {
				helper.fail("Could not add " + item + " to the board: " + result);
			}
		}
	}

	private static LayerStack.AddResult check(GameTestHelper helper, Item item) {
		return board(helper).stack().check(helper.getLevel().registryAccess(), KitchenBlocks.BOARD, item);
	}

	private static int itemsNearby(GameTestHelper helper, Item item) {
		int total = 0;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, BOARD, 4.0)) {
			if (entity.getItem().is(item)) {
				total += entity.getItem().getCount();
			}
		}
		return total;
	}

	// -- nutrition -------------------------------------------------------------------------------------------

	@GameTest
	public void nutritionIsTheSumOfTheLayers(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		// bread 0 + bacon 2 + cheese slice 3 + tomato slice 1 + bread 0
		layers(helper, player, KitchenItems.BREAD_SLICE, KitchenItems.BACON, KitchenItems.CHEESE_SLICE,
				KitchenItems.TOMATO_SLICE, KitchenItems.BREAD_SLICE);
		LayerStack.FinishResult result = board(helper).stack().finish(helper.getLevel().registryAccess());
		helper.assertValueEqual(LayerStack.FinishStatus.OK, result.status(), "finish status");
		FoodProperties food = result.stack().get(DataComponents.FOOD);
		helper.assertTrue(food != null, "finished sandwich has a FOOD component");
		helper.assertValueEqual(6, food.nutrition(), "nutrition");
		helper.assertTrue(food.canAlwaysEat(), "sandwiches are always edible");
		helper.assertTrue(result.stack().get(KitchenComponents.LAYERED_FOOD) != null, "layers are stored");
		helper.assertTrue(board(helper).stack().isEmpty(), "board is empty after finishing");
		helper.succeed();
	}

	@GameTest
	public void nutritionMayExceedTwenty(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		layers(helper, player, KitchenItems.BREAD_SLICE);
		for (int i = 0; i < 8; i++) {
			layers(helper, player, KitchenItems.ROAST_BEEF); // 4 each
		}
		layers(helper, player, KitchenItems.BREAD_SLICE);
		LayerStack.FinishResult result = board(helper).stack().finish(helper.getLevel().registryAccess());
		helper.assertValueEqual(LayerStack.FinishStatus.OK, result.status(), "finish status");
		helper.assertValueEqual(32, result.stack().get(DataComponents.FOOD).nutrition(), "nutrition of a 10-layer roast beef sandwich");
		helper.succeed();
	}

	// -- finishing -------------------------------------------------------------------------------------------

	@GameTest
	public void finishNeedsTwoLayers(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		layers(helper, player, KitchenItems.BREAD_SLICE);
		helper.assertValueEqual(LayerStack.FinishStatus.TOO_FEW_LAYERS,
				board(helper).stack().checkFinish(helper.getLevel().registryAccess()), "one layer");
		helper.assertTrue(board(helper).finish(helper.getLevel(), player, true) == InteractionResult.PASS,
				"sneak click does nothing yet");
		helper.assertValueEqual(1, board(helper).stack().layers().size(), "layer is still there");
		layers(helper, player, KitchenItems.TOAST); // bread + toast: the smallest valid sandwich
		helper.assertValueEqual(LayerStack.FinishStatus.OK,
				board(helper).stack().checkFinish(helper.getLevel().registryAccess()), "bread + toast");
		helper.succeed();
	}

	@GameTest
	public void finishNeedsBreadOnBothEnds(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		var access = helper.getLevel().registryAccess();
		layers(helper, player, KitchenItems.BREAD_SLICE, KitchenItems.BACON);
		helper.assertValueEqual(LayerStack.FinishStatus.BAD_ENDS, board(helper).stack().checkFinish(access), "no bread on top");
		board(helper).stack().clear();
		layers(helper, player, KitchenItems.BACON, KitchenItems.BREAD_SLICE);
		helper.assertValueEqual(LayerStack.FinishStatus.BAD_ENDS, board(helper).stack().checkFinish(access), "no bread on the bottom");
		layers(helper, player, KitchenItems.CHEESE_SLICE);
		helper.assertValueEqual(LayerStack.FinishStatus.BAD_ENDS, board(helper).stack().checkFinish(access), "cheese on top");
		helper.assertTrue(board(helper).finish(helper.getLevel(), player, true) == InteractionResult.PASS, "cannot finish");
		helper.assertValueEqual(3, board(helper).stack().layers().size(), "layers stay on the board");
		helper.succeed();
	}

	@GameTest
	public void tenLayersMaximum(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		for (int i = 0; i < 10; i++) {
			layers(helper, player, KitchenItems.BREAD_SLICE);
		}
		helper.assertValueEqual(LayerStack.AddResult.FULL, check(helper, KitchenItems.BACON), "11th layer");
		helper.assertTrue(put(helper, player, KitchenItems.BACON) == InteractionResult.PASS, "11th layer is rejected");
		helper.assertValueEqual(10, board(helper).stack().layers().size(), "layers");
		helper.succeed();
	}

	@GameTest
	public void nonIngredientsAreRejected(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		helper.assertValueEqual(LayerStack.AddResult.NOT_AN_INGREDIENT, check(helper, Items.DIRT), "dirt");
		helper.assertTrue(put(helper, player, Items.DIRT) == InteractionResult.PASS, "dirt is not placed");
		helper.assertTrue(board(helper).stack().isEmpty(), "board is empty");
		helper.succeed();
	}

	@GameTest
	public void removingTheTopLayerHandsItBack(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack held = new ItemStack(KitchenItems.BACON, 2);
		helper.assertTrue(board(helper).useItem(helper.getLevel(), player, held, true) == InteractionResult.SUCCESS, "add");
		helper.assertValueEqual(1, held.getCount(), "one bacon was used");
		helper.assertTrue(board(helper).useEmptyHand(helper.getLevel(), true) == InteractionResult.SUCCESS, "remove");
		helper.assertTrue(board(helper).stack().isEmpty(), "board is empty");
		helper.assertValueEqual(1, itemsNearby(helper, KitchenItems.BACON), "bacon dropped");
		helper.succeed();
	}

	// -- butter ----------------------------------------------------------------------------------------------

	@GameTest
	public void butterOnlyGoesOnABreadSlice(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		helper.assertValueEqual(LayerStack.AddResult.CANNOT_PLACE_ON_THIS, check(helper, KitchenItems.BUTTER), "on an empty board");
		layers(helper, player, KitchenItems.TOAST);
		helper.assertValueEqual(LayerStack.AddResult.CANNOT_PLACE_ON_THIS, check(helper, KitchenItems.BUTTER), "on toast");
		board(helper).stack().clear();
		layers(helper, player, KitchenItems.BREAD_SLICE, KitchenItems.BACON);
		helper.assertValueEqual(LayerStack.AddResult.CANNOT_PLACE_ON_THIS, check(helper, KitchenItems.BUTTER), "on bacon");
		board(helper).stack().clear();
		layers(helper, player, KitchenItems.BREAD_SLICE);
		helper.assertValueEqual(LayerStack.AddResult.OK, check(helper, KitchenItems.BUTTER), "on a bread slice");
		helper.succeed();
	}

	@GameTest
	public void butterNeedsTwoKnifeClicksBeforeAnythingGoesOnTop(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		var level = helper.getLevel();
		layers(helper, player, KitchenItems.BREAD_SLICE, KitchenItems.BUTTER);
		helper.assertValueEqual(2, board(helper).stack().pendingSpread(), "clicks needed");
		helper.assertValueEqual(LayerStack.AddResult.STILL_SPREADING, check(helper, KitchenItems.BACON), "bacon on unspread butter");
		helper.assertTrue(put(helper, player, KitchenItems.BACON) == InteractionResult.PASS, "bacon is rejected");

		ItemStack knife = new ItemStack(KitchenItems.KNIFE);
		helper.assertTrue(board(helper).useItem(level, player, knife, true) == InteractionResult.SUCCESS, "first click");
		helper.assertValueEqual(LayerStack.AddResult.STILL_SPREADING, check(helper, KitchenItems.BACON), "after one click");
		helper.assertTrue(board(helper).useItem(level, player, knife, true) == InteractionResult.SUCCESS, "second click");
		helper.assertValueEqual(0, board(helper).stack().pendingSpread(), "spread");
		helper.assertValueEqual(LayerStack.AddResult.OK, check(helper, KitchenItems.BACON), "after two clicks");
		helper.assertTrue(board(helper).useItem(level, player, knife, true) == InteractionResult.PASS, "a third click does nothing");
		layers(helper, player, KitchenItems.BACON);
		helper.assertValueEqual(3, board(helper).stack().layers().size(), "bread, butter, bacon");
		helper.succeed();
	}

	@GameTest
	public void onlyUntouchedButterComesBack(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		var level = helper.getLevel();
		layers(helper, player, KitchenItems.BREAD_SLICE, KitchenItems.BUTTER);
		board(helper).useEmptyHand(level, true); // untouched: returned
		helper.assertValueEqual(1, itemsNearby(helper, KitchenItems.BUTTER), "untouched butter is returned");

		layers(helper, player, KitchenItems.BUTTER);
		board(helper).useItem(level, player, new ItemStack(KitchenItems.KNIFE), true); // now partly spread
		board(helper).useEmptyHand(level, true);
		helper.assertValueEqual(1, itemsNearby(helper, KitchenItems.BUTTER), "spread butter is not returned (still just the one)");
		helper.assertValueEqual(1, board(helper).stack().layers().size(), "only the bread is left");
		helper.assertValueEqual(0, board(helper).stack().pendingSpread(), "nothing pending");
		helper.succeed();
	}

	// -- jam -------------------------------------------------------------------------------------------------

	@GameTest
	public void jamComesFromTheJarAndTheJarStaysInHand(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		var level = helper.getLevel();
		ItemStack jar = new ItemStack(KitchenItems.JAM_JAR);
		jar.set(KitchenComponents.JAM, new JamContents(Identifier.parse("kitchen:strawberry_jam"), 2));
		layers(helper, player, KitchenItems.BREAD_SLICE);

		helper.assertTrue(board(helper).useItem(level, player, jar, true) == InteractionResult.SUCCESS, "first spoonful");
		helper.assertValueEqual(1, jar.getCount(), "the jar is not placed");
		helper.assertValueEqual(1, jar.get(KitchenComponents.JAM).servings(), "one serving used");
		helper.assertTrue(board(helper).stack().layers().getLast().item().value() == KitchenItems.STRAWBERRY_JAM, "jam layer");

		helper.assertTrue(board(helper).useItem(level, player, jar, true) == InteractionResult.SUCCESS, "second spoonful");
		helper.assertTrue(jar.get(KitchenComponents.JAM) == null, "an empty jar loses its contents");
		helper.assertTrue(board(helper).useItem(level, player, jar, true) == InteractionResult.PASS, "empty jar does nothing");

		board(helper).useEmptyHand(level, true); // jam is not handed back
		board(helper).useEmptyHand(level, true);
		helper.assertValueEqual(0, itemsNearby(helper, KitchenItems.STRAWBERRY_JAM), "jam layers drop nothing");
		helper.succeed();
	}

	// -- knife on a whole ingredient -------------------------------------------------------------------------

	@GameTest
	public void knifeCutsAWholeIngredientOnTheBoard(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		var level = helper.getLevel();
		ItemStack porkchop = new ItemStack(Items.PORKCHOP, 2);
		helper.assertTrue(board(helper).useItem(level, player, porkchop, true) == InteractionResult.SUCCESS, "place porkchop");
		helper.assertValueEqual(1, porkchop.getCount(), "one porkchop placed");
		helper.assertTrue(board(helper).workpiece() != null, "board holds the porkchop");
		helper.assertTrue(put(helper, player, KitchenItems.BACON) == InteractionResult.PASS, "no layers on top of a raw porkchop");

		helper.assertTrue(board(helper).useItem(level, player, new ItemStack(KitchenItems.KNIFE), true) == InteractionResult.SUCCESS, "cut");
		helper.assertTrue(board(helper).workpiece() == null, "porkchop is gone");
		helper.assertValueEqual(3, itemsNearby(helper, KitchenItems.RAW_BACON), "porkchop makes 3 raw bacon (kitchen:cutting)");
		helper.succeed();
	}

	@GameTest
	public void knifeDoesNothingOnAnEmptyBoard(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		helper.assertTrue(board(helper).useItem(helper.getLevel(), player, new ItemStack(KitchenItems.KNIFE), true) == InteractionResult.PASS, "empty board");
		helper.succeed();
	}

	// -- naming and finishing by sneak click -----------------------------------------------------------------

	@GameTest
	public void sandwichesAreNamedAfterTheirTopTwoFillings(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		layers(helper, player, KitchenItems.BREAD_SLICE, KitchenItems.LETTUCE_LEAF, KitchenItems.BACON,
				KitchenItems.CHEESE_SLICE, KitchenItems.BREAD_SLICE);
		helper.assertTrue(board(helper).finish(helper.getLevel(), player, true) == InteractionResult.SUCCESS, "sneak click finishes");
		ItemStack sandwich = null;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, BOARD, 4.0)) {
			if (entity.getItem().is(KitchenItems.SANDWICH)) {
				sandwich = entity.getItem();
			}
		}
		helper.assertTrue(sandwich != null, "a sandwich was dropped");
		Component name = sandwich.get(DataComponents.ITEM_NAME);
		helper.assertTrue(name != null && name.getContents() instanceof TranslatableContents, "has a generated name");
		TranslatableContents contents = (TranslatableContents) name.getContents();
		helper.assertValueEqual("item.kitchen.sandwich.named_2", contents.getKey(), "name pattern");
		helper.assertValueEqual("layer.kitchen.bacon", keyOf(contents.getArgs()[0]), "first filling");
		helper.assertValueEqual("layer.kitchen.cheese_slice", keyOf(contents.getArgs()[1]), "second filling");
		helper.succeed();
	}

	private static String keyOf(Object arg) {
		return arg instanceof Component component && component.getContents() instanceof TranslatableContents contents
				? contents.getKey() : String.valueOf(arg);
	}

	@GameTest
	public void everyOriginalSandwichableIsLoaded(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		List<Item> expected = List.of(KitchenItems.BREAD_SLICE, KitchenItems.TOAST, KitchenItems.RAW_BACON,
				KitchenItems.BACON, KitchenItems.TOMATO_SLICE, KitchenItems.LETTUCE_LEAF, KitchenItems.POTATO_SLICE,
				KitchenItems.CARROT_SLICE, KitchenItems.RAW_ROAST_BEEF, KitchenItems.ROAST_BEEF,
				KitchenItems.RAW_CHICKEN_FILLET, KitchenItems.CHICKEN_FILLET, KitchenItems.CHEESE_SLICE,
				KitchenItems.BUTTER, KitchenItems.STRAWBERRY_JAM, KitchenItems.APPLE_JAM, KitchenItems.PEANUT_JAM);
		for (Item item : expected) {
			helper.assertTrue(net.kitchenreborn.food.Foods.layerFor(access, item).isPresent(), "layer definition for " + item);
		}
		helper.succeed();
	}
}
