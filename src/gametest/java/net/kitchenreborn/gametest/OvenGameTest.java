package net.kitchenreborn.gametest;

import java.lang.reflect.Method;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.kitchenreborn.block.OvenBlock;
import net.kitchenreborn.blockentity.OvenBlockEntity;
import net.kitchenreborn.oven.OvenConfig;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenItems;

/** Logic tests for the oven (milestone M6): add and remove rules, fuel, the cook cycle, open and close. */
public class OvenGameTest implements CustomTestMethodInvoker {
	private static final BlockPos OVEN = new BlockPos(1, 1, 1);

	@Override
	public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
		helper.setBlock(OVEN, KitchenBlocks.OVEN);
		method.invoke(this, helper);
	}

	private static OvenBlockEntity oven(GameTestHelper helper) {
		return helper.getBlockEntity(OVEN, OvenBlockEntity.class);
	}

	private static void open(GameTestHelper helper) {
		oven(helper).toggle(helper.getLevel(), true);
		helper.assertTrue(oven(helper).isOpen(), "the oven is open");
	}

	private static void close(GameTestHelper helper) {
		oven(helper).toggle(helper.getLevel(), true);
		helper.assertTrue(!oven(helper).isOpen(), "the oven is closed");
	}

	private static InteractionResult use(GameTestHelper helper, Player player, ItemStack held) {
		return oven(helper).useItem(helper.getLevel(), player, held, true);
	}

	private static void put(GameTestHelper helper, Player player, Item item, int times) {
		for (int i = 0; i < times; i++) {
			helper.assertValueEqual(InteractionResult.SUCCESS, use(helper, player, new ItemStack(item, 64)), "adding " + item);
		}
	}

	private static void fuel(GameTestHelper helper, Player player) {
		helper.assertValueEqual(InteractionResult.SUCCESS, use(helper, player, new ItemStack(Items.COAL, 4)), "adding fuel");
		helper.assertTrue(oven(helper).hasFuel(), "the oven has fuel");
	}

	private static void tick(GameTestHelper helper, int ticks) {
		for (int i = 0; i < ticks; i++) {
			oven(helper).serverTick(helper.getLevel());
		}
	}

	private static int count(OvenBlockEntity oven, Item item) {
		int total = 0;
		for (ItemStackTemplate slot : oven.slots()) {
			if (slot.item().value() == item) {
				total += slot.count();
			}
		}
		return total;
	}

	// -- adding and removing ----------------------------------------------------------------------------------

	@GameTest
	public void nothingCanBeAddedWhileClosed(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertValueEqual(InteractionResult.PASS, use(helper, player, new ItemStack(KitchenItems.RAW_BACON)), "food while closed");
		helper.assertValueEqual(InteractionResult.PASS, use(helper, player, new ItemStack(Items.COAL)), "fuel while closed");
		helper.assertTrue(oven(helper).slots().isEmpty() && !oven(helper).hasFuel(), "nothing went in");
		helper.succeed();
	}

	@GameTest
	public void fourSlotsOfFourOfOneKind(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 4);
		helper.assertValueEqual(1, oven(helper).slots().size(), "four of one kind share a slot");
		put(helper, player, KitchenItems.RAW_BACON, 1);
		helper.assertValueEqual(2, oven(helper).slots().size(), "the fifth starts a new slot");
		put(helper, player, KitchenItems.RAW_BACON, 11);
		helper.assertValueEqual(16, count(oven(helper), KitchenItems.RAW_BACON), "16 of one kind fill the oven");
		helper.assertValueEqual(InteractionResult.FAIL, use(helper, player, new ItemStack(KitchenItems.RAW_BACON)), "the 17th");
		helper.succeed();
	}

	@GameTest
	public void differentKindsTakeSeparateSlots(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 1);
		put(helper, player, KitchenItems.RAW_ROAST_BEEF, 1);
		put(helper, player, KitchenItems.RAW_CHICKEN_FILLET, 1);
		put(helper, player, Items.PORKCHOP, 1);
		helper.assertValueEqual(4, oven(helper).slots().size(), "four kinds, four slots");
		helper.assertValueEqual(InteractionResult.FAIL, use(helper, player, new ItemStack(Items.BEEF)), "a fifth kind");
		put(helper, player, KitchenItems.RAW_BACON, 3);
		helper.assertValueEqual(4, count(oven(helper), KitchenItems.RAW_BACON), "more of an existing kind still fits");
		helper.succeed();
	}

	@GameTest
	public void onlyCookableItemsGoIn(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		// Own recipes, smoker recipes and furnace food recipes are accepted...
		for (Item food : new Item[] {KitchenItems.RAW_BACON, KitchenItems.RAW_ROAST_BEEF, KitchenItems.RAW_CHICKEN_FILLET, Items.PORKCHOP}) {
			helper.assertTrue(use(helper, player, new ItemStack(food)) == InteractionResult.SUCCESS, food + " is cookable");
			oven(helper).slots().clear();
		}
		helper.assertTrue(use(helper, player, new ItemStack(Items.POTATO)) == InteractionResult.SUCCESS, "a potato (furnace food recipe)");
		oven(helper).slots().clear();
		// ...other items and non-food furnace recipes are not.
		for (Item other : new Item[] {Items.APPLE, Items.COBBLESTONE, Items.RAW_IRON, Items.GOLD_NUGGET, KitchenItems.BACON}) {
			helper.assertValueEqual(InteractionResult.FAIL, use(helper, player, new ItemStack(other)), other + " is refused");
		}
		helper.assertTrue(oven(helper).slots().isEmpty(), "nothing went in");
		helper.succeed();
	}

	@GameTest
	public void anEmptyHandTakesOutAStackWhileOpen(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 6);
		put(helper, player, KitchenItems.RAW_ROAST_BEEF, 1);
		helper.assertValueEqual(InteractionResult.SUCCESS, oven(helper).takeStack(helper.getLevel(), player, true), "take");
		helper.assertValueEqual(2, oven(helper).slots().size(), "one stack came out");
		helper.assertValueEqual(2, count(oven(helper), KitchenItems.RAW_BACON), "the first stack (4 bacon) was taken");
		helper.assertTrue(player.getInventory().contains(stack -> stack.is(KitchenItems.RAW_BACON) && stack.getCount() == 4), "the player has the 4 bacon");
		close(helper);
		helper.assertValueEqual(InteractionResult.PASS, oven(helper).takeStack(helper.getLevel(), player, true), "closed ovens give nothing");
		helper.succeed();
	}

	@GameTest
	public void predictionDoesNotChangeTheOven(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		ItemStack held = new ItemStack(KitchenItems.RAW_BACON, 2);
		helper.assertValueEqual(InteractionResult.SUCCESS, oven(helper).useItem(helper.getLevel(), player, held, false), "predicted add");
		helper.assertTrue(oven(helper).slots().isEmpty(), "unchanged");
		helper.assertValueEqual(2, held.getCount(), "stack unchanged");
		helper.succeed();
	}

	// -- fuel -------------------------------------------------------------------------------------------------

	@GameTest
	public void fuelIsAnythingThatBurns(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		for (Item fuel : new Item[] {Items.COAL, Items.CHARCOAL, Items.STICK}) {
			helper.assertValueEqual(InteractionResult.SUCCESS, use(helper, player, new ItemStack(fuel)), fuel + " is fuel");
			oven(helper).toggle(helper.getLevel(), true); // close
			oven(helper).toggle(helper.getLevel(), true); // open again: the fuel is used up
		}
		helper.assertValueEqual(InteractionResult.FAIL, use(helper, player, new ItemStack(Items.APPLE)), "an apple is not fuel");
		// Only one fuel item at a time.
		fuel(helper, player);
		ItemStack second = new ItemStack(Items.COAL, 3);
		helper.assertValueEqual(InteractionResult.FAIL, use(helper, player, second), "a second fuel item");
		helper.assertValueEqual(3, second.getCount(), "not consumed");
		helper.succeed();
	}

	@GameTest
	public void fuelIsConsumedAndRemaindersComeBack(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		ItemStack coal = new ItemStack(Items.COAL, 2);
		use(helper, player, coal);
		helper.assertValueEqual(1, coal.getCount(), "one coal used");
		oven(helper).toggle(helper.getLevel(), true);
		oven(helper).toggle(helper.getLevel(), true);
		ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
		use(helper, player, lava);
		helper.assertTrue(lava.isEmpty(), "lava bucket used");
		helper.assertTrue(player.getInventory().contains(stack -> stack.is(Items.BUCKET)), "the empty bucket came back");
		helper.succeed();
	}

	@GameTest
	public void withoutFuelNothingCooks(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 2);
		close(helper);
		tick(helper, 1000);
		helper.assertValueEqual(2, count(oven(helper), KitchenItems.RAW_BACON), "still raw");
		helper.assertTrue(!oven(helper).isCooking(), "not cooking");
		helper.succeed();
	}

	@GameTest
	public void withoutCookableFoodNothingBurns(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		fuel(helper, player);
		close(helper);
		tick(helper, 1000);
		helper.assertTrue(!oven(helper).isCooking(), "an empty oven with fuel does not start");
		helper.assertValueEqual(OvenBlockEntity.Stage.RAW, oven(helper).stage(), "stage");
		helper.succeed();
	}

	// -- the cook cycle ---------------------------------------------------------------------------------------

	@GameTest
	public void timingsComeFromData(GameTestHelper helper) {
		OvenConfig config = OvenConfig.get(helper.getLevel().registryAccess());
		helper.assertValueEqual(400, config.cookTicks(), "cook ticks");
		helper.assertValueEqual(600, config.burnTicks(), "burn ticks");
		helper.succeed();
	}

	@GameTest
	public void cooksAtFourHundredTicks(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 3);
		put(helper, player, Items.PORKCHOP, 1);
		fuel(helper, player);
		close(helper);
		int cook = OvenConfig.get(helper.getLevel().registryAccess()).cookTicks();
		tick(helper, cook - 1);
		helper.assertTrue(oven(helper).isCooking(), "cooking");
		helper.assertTrue(helper.getBlockState(OVEN).getValue(OvenBlock.LIT), "the oven is lit while cooking");
		helper.assertValueEqual(3, count(oven(helper), KitchenItems.RAW_BACON), "still raw one tick before");
		tick(helper, 1);
		helper.assertValueEqual(OvenBlockEntity.Stage.COOKED, oven(helper).stage(), "stage");
		helper.assertValueEqual(3, count(oven(helper), KitchenItems.BACON), "3 raw bacon became 3 bacon (own recipe)");
		helper.assertValueEqual(1, count(oven(helper), Items.COOKED_PORKCHOP), "the porkchop became a cooked porkchop (furnace recipe)");
		helper.succeed();
	}

	@GameTest
	public void burnsAtSixHundredTicks(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 6);
		put(helper, player, KitchenItems.RAW_ROAST_BEEF, 2);
		fuel(helper, player);
		close(helper);
		int burn = OvenConfig.get(helper.getLevel().registryAccess()).burnTicks();
		tick(helper, burn - 1);
		helper.assertValueEqual(OvenBlockEntity.Stage.COOKED, oven(helper).stage(), "cooked, not yet burnt");
		tick(helper, 1);
		helper.assertValueEqual(OvenBlockEntity.Stage.BURNT, oven(helper).stage(), "stage");
		helper.assertValueEqual(8, count(oven(helper), KitchenItems.BURNT_MEAT), "everything became burnt meat, count kept");
		helper.assertValueEqual(0, count(oven(helper), KitchenItems.BACON), "no bacon left");
		tick(helper, 2000);
		helper.assertValueEqual(OvenBlockEntity.Stage.BURNT, oven(helper).stage(), "stays burnt");
		helper.succeed();
	}

	// -- opening and closing ----------------------------------------------------------------------------------

	@GameTest
	public void openingMidCookResetsTheTimerAndLosesTheFuel(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 2);
		fuel(helper, player);
		close(helper);
		tick(helper, 100);
		helper.assertTrue(oven(helper).isCooking() && oven(helper).cookTime() > 0, "cooking");
		open(helper);
		helper.assertTrue(!oven(helper).hasFuel(), "the fuel is gone");
		helper.assertTrue(!oven(helper).isCooking() && oven(helper).cookTime() == 0, "the timer is reset");
		helper.assertTrue(!helper.getBlockState(OVEN).getValue(OvenBlock.LIT), "no longer lit");
		helper.assertValueEqual(2, count(oven(helper), KitchenItems.RAW_BACON), "the food is untouched");
		// Closing again without new fuel does nothing.
		close(helper);
		tick(helper, 1000);
		helper.assertValueEqual(2, count(oven(helper), KitchenItems.RAW_BACON), "no fuel, still raw");
		helper.succeed();
	}

	@GameTest
	public void cookedFoodStaysCookedWhenOpened(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 2);
		fuel(helper, player);
		close(helper);
		tick(helper, OvenConfig.get(helper.getLevel().registryAccess()).cookTicks());
		open(helper);
		helper.assertValueEqual(2, count(oven(helper), KitchenItems.BACON), "cooked bacon");
		helper.assertValueEqual(OvenBlockEntity.Stage.RAW, oven(helper).stage(), "the timer stage restarts");
		// Cooked bacon has no oven recipe of its own, so closing it again with new fuel does not burn it.
		fuel(helper, player);
		close(helper);
		tick(helper, 2000);
		helper.assertValueEqual(2, count(oven(helper), KitchenItems.BACON), "nothing left to cook, nothing burns");
		helper.succeed();
	}

	@GameTest
	public void sneakingWithAnEmptyHandTogglesTheHatch(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.useBlock(OVEN, player);
		helper.assertTrue(helper.getBlockState(OVEN).getValue(OvenBlock.OPEN), "opened");
		helper.useBlock(OVEN, player);
		helper.assertTrue(!helper.getBlockState(OVEN).getValue(OvenBlock.OPEN), "closed");
		// Without sneaking an empty hand does not toggle.
		player.setShiftKeyDown(false);
		helper.useBlock(OVEN, player);
		helper.assertTrue(!helper.getBlockState(OVEN).getValue(OvenBlock.OPEN), "still closed");
		helper.succeed();
	}

	@GameTest
	public void breakingTheOvenDropsTheFood(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		open(helper);
		put(helper, player, KitchenItems.RAW_BACON, 5);
		put(helper, player, KitchenItems.RAW_ROAST_BEEF, 2);
		helper.getLevel().destroyBlock(helper.absolutePos(OVEN), true);
		int bacon = 0;
		int beef = 0;
		boolean oven = false;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, OVEN, 6.0)) {
			bacon += entity.getItem().is(KitchenItems.RAW_BACON) ? entity.getItem().getCount() : 0;
			beef += entity.getItem().is(KitchenItems.RAW_ROAST_BEEF) ? entity.getItem().getCount() : 0;
			oven |= entity.getItem().is(KitchenItems.OVEN);
		}
		helper.assertValueEqual(5, bacon, "bacon dropped");
		helper.assertValueEqual(2, beef, "beef dropped");
		helper.assertTrue(oven, "the oven dropped itself");
		helper.succeed();
	}
}
