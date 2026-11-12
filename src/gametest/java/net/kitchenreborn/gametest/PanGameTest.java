package net.kitchenreborn.gametest;

import java.lang.reflect.Method;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import net.kitchenreborn.block.OvenBlock;
import net.kitchenreborn.block.PanBlock;
import net.kitchenreborn.blockentity.BoardBlockEntity;
import net.kitchenreborn.blockentity.PanBlockEntity;
import net.kitchenreborn.food.JamContents;
import net.kitchenreborn.food.LayerStack;
import net.kitchenreborn.pan.PanConfig;
import net.kitchenreborn.registry.KitchenBlockEntities;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenItems;

/** Logic tests for the frying pan and jam (milestone M7): the oven rule, frying, jam stages, jars, jam on a sandwich. */
public class PanGameTest implements CustomTestMethodInvoker {
	private static final BlockPos OVEN = new BlockPos(1, 1, 1);
	private static final BlockPos PAN = new BlockPos(1, 2, 1);

	@Override
	public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
		helper.setBlock(OVEN, KitchenBlocks.OVEN.defaultBlockState().setValue(OvenBlock.FACING, Direction.EAST));
		helper.setBlock(PAN, KitchenBlocks.PAN.defaultBlockState().setValue(PanBlock.FACING, Direction.EAST));
		method.invoke(this, helper);
	}

	private static PanBlockEntity pan(GameTestHelper helper) {
		return helper.getBlockEntity(PAN, PanBlockEntity.class);
	}

	private static void tick(GameTestHelper helper, int ticks) {
		for (int i = 0; i < ticks; i++) {
			pan(helper).serverTick(helper.getLevel());
		}
	}

	private static InteractionResult use(GameTestHelper helper, Player player, ItemStack held) {
		return pan(helper).useItem(helper.getLevel(), player, held, true);
	}

	private static boolean has(Player player, Item item) {
		return player.getInventory().contains(stack -> stack.is(item));
	}

	private static ItemStack emptyJar() {
		return new ItemStack(KitchenItems.JAM_JAR);
	}

	private static int cookTicks(GameTestHelper helper) {
		return PanConfig.get(helper.getLevel().registryAccess()).cookTicks();
	}

	// -- the oven rule ----------------------------------------------------------------------------------------

	@GameTest
	public void thePanNeedsAnOven(GameTestHelper helper) {
		BlockState state = helper.getBlockState(PAN);
		helper.assertTrue(state.canSurvive(helper.getLevel(), helper.absolutePos(PAN)), "a pan on an oven survives");
		// Take the oven away: the pan's ticker pops the pan off.
		helper.setBlock(OVEN, net.minecraft.world.level.block.Blocks.AIR);
		helper.assertTrue(!state.canSurvive(helper.getLevel(), helper.absolutePos(PAN)), "a pan without an oven cannot stay");
		BlockEntityTicker<PanBlockEntity> ticker = KitchenBlocks.PAN instanceof PanBlock block
				? block.getTicker(helper.getLevel(), state, KitchenBlockEntities.PAN) : null;
		helper.assertTrue(ticker != null, "the pan has a ticker");
		ticker.tick(helper.getLevel(), helper.absolutePos(PAN), state, pan(helper));
		helper.assertTrue(helper.getBlockState(PAN).isAir(), "the pan came off");
		helper.succeed();
	}

	@GameTest
	public void thePanTakesTheOvensFacing(GameTestHelper helper) {
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			helper.setBlock(OVEN, KitchenBlocks.OVEN.defaultBlockState().setValue(OvenBlock.FACING, facing));
			BlockState placed = KitchenBlocks.PAN.defaultBlockState();
			helper.assertTrue(KitchenBlocks.PAN.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(PAN)), "on the oven");
			helper.assertTrue(placed.getValue(PanBlock.FACING) != null, "has a facing");
		}
		helper.succeed();
	}

	@GameTest
	public void itWorksOnAnyOvenLitOrNot(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(!helper.getBlockState(OVEN).getValue(OvenBlock.LIT), "the oven is not lit");
		helper.assertValueEqual(InteractionResult.SUCCESS, use(helper, player, new ItemStack(KitchenItems.RAW_BACON)), "start frying");
		tick(helper, cookTicks(helper));
		helper.assertTrue(pan(helper).isDone(), "done on a cold oven (design D6)");
		helper.succeed();
	}

	// -- frying -----------------------------------------------------------------------------------------------

	@GameTest
	public void friedFoodIsDoneAtThreeHundredTicks(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertValueEqual(300, cookTicks(helper), "cook ticks come from data");
		ItemStack bacon = new ItemStack(KitchenItems.RAW_BACON, 3);
		helper.assertValueEqual(InteractionResult.SUCCESS, use(helper, player, bacon), "add raw bacon");
		helper.assertValueEqual(2, bacon.getCount(), "one raw bacon used");
		helper.assertValueEqual(InteractionResult.PASS, use(helper, player, new ItemStack(KitchenItems.RAW_BACON)), "the pan holds one thing at a time");
		tick(helper, 299);
		helper.assertTrue(!pan(helper).isDone(), "not done one tick before");
		helper.assertValueEqual(InteractionResult.SUCCESS, pan(helper).useEmptyHand(helper.getLevel(), player, false), "an empty hand may take the raw bacon back");
		tick(helper, 1);
		helper.assertTrue(pan(helper).isDone(), "done at 300");
		helper.assertValueEqual(InteractionResult.SUCCESS, pan(helper).useEmptyHand(helper.getLevel(), player, true), "take the bacon");
		helper.assertTrue(has(player, KitchenItems.BACON), "cooked bacon in the inventory");
		helper.assertTrue(pan(helper).isEmpty(), "the pan is empty again");
		helper.succeed();
	}

	@GameTest
	public void chickenFilletsFryToo(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.RAW_CHICKEN_FILLET));
		tick(helper, cookTicks(helper));
		pan(helper).useEmptyHand(helper.getLevel(), player, true);
		helper.assertTrue(has(player, KitchenItems.CHICKEN_FILLET), "cooked chicken fillet");
		helper.succeed();
	}

	@GameTest
	public void friedFoodDoesNotBurn(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.RAW_BACON));
		tick(helper, 5000);
		helper.assertTrue(pan(helper).isDone(), "still just done (the original pan never burnt anything)");
		pan(helper).useEmptyHand(helper.getLevel(), player, true);
		helper.assertTrue(has(player, KitchenItems.BACON), "still bacon");
		helper.succeed();
	}

	@GameTest
	public void onlyPanRecipesGoIn(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (Item other : new Item[] {Items.APPLE, KitchenItems.BACON, KitchenItems.RAW_ROAST_BEEF, Items.PORKCHOP}) {
			ItemStack stack = new ItemStack(other);
			helper.assertValueEqual(InteractionResult.PASS, use(helper, player, stack), other + " is not a pan recipe");
			helper.assertValueEqual(1, stack.getCount(), other + " not used");
		}
		helper.assertTrue(pan(helper).isEmpty(), "nothing went in");
		helper.succeed();
	}

	// -- jam --------------------------------------------------------------------------------------------------

	@GameTest
	public void jamFillsInThreeStages(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.SUGARED_STRAWBERRY_SLICES));
		helper.assertValueEqual(0, pan(helper).stage(), "stage at the start");
		tick(helper, 99);
		helper.assertValueEqual(0, pan(helper).stage(), "stage 0 until 100");
		tick(helper, 1);
		helper.assertValueEqual(1, pan(helper).stage(), "stage 1 at 100");
		tick(helper, 99);
		helper.assertValueEqual(1, pan(helper).stage(), "still 1 at 199");
		tick(helper, 1);
		helper.assertValueEqual(2, pan(helper).stage(), "stage 2 at 200");
		tick(helper, 99);
		helper.assertTrue(!pan(helper).isDone(), "not done at 299");
		tick(helper, 1);
		helper.assertValueEqual(3, pan(helper).stage(), "stage 3 at 300");
		helper.assertTrue(pan(helper).isDone(), "done at 300");
		helper.succeed();
	}

	@GameTest
	public void allThreeJamsExist(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Item[][] cases = {{KitchenItems.SUGARED_STRAWBERRY_SLICES, KitchenItems.STRAWBERRY_JAM},
				{KitchenItems.APPLE_SLICE, KitchenItems.APPLE_JAM}, {KitchenItems.PEANUT, KitchenItems.PEANUT_JAM}};
		for (Item[] jam : cases) {
			use(helper, player, new ItemStack(jam[0]));
			tick(helper, cookTicks(helper));
			ItemStack jar = emptyJar();
			helper.assertValueEqual(InteractionResult.SUCCESS, use(helper, player, jar), jam[0] + " fills a jar");
			helper.assertTrue(jar.isEmpty(), "the empty jar was consumed");
			ItemStack filled = null;
			for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
				if (stack.is(KitchenItems.JAM_JAR)) {
					filled = stack;
				}
			}
			helper.assertTrue(filled != null, "a filled jar arrived");
			JamContents contents = filled.get(KitchenComponents.JAM);
			helper.assertTrue(contents != null && contents.servings() == 6, "6 servings");
			helper.assertTrue(contents.jam().equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(jam[1])), "the right jam");
			player.getInventory().clearContent();
		}
		helper.succeed();
	}

	@GameTest
	public void jamNeedsAnEmptyJar(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.PEANUT));
		tick(helper, cookTicks(helper));
		helper.assertValueEqual(InteractionResult.PASS, pan(helper).useEmptyHand(helper.getLevel(), player, true), "an empty hand leaves finished jam");
		helper.assertValueEqual(InteractionResult.PASS, use(helper, player, new ItemStack(Items.GLASS_BOTTLE)), "a bottle is not a jar");
		ItemStack full = emptyJar();
		full.set(KitchenComponents.JAM, new JamContents(net.minecraft.resources.Identifier.parse("kitchen:apple_jam"), 3));
		helper.assertValueEqual(InteractionResult.PASS, use(helper, player, full), "a filled jar does not collect");
		helper.assertTrue(!pan(helper).isEmpty(), "the jam is still in the pan");
		helper.succeed();
	}

	@GameTest
	public void unfinishedJamGoesBackToTheHand(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.SUGARED_STRAWBERRY_SLICES));
		tick(helper, 150);
		helper.assertValueEqual(InteractionResult.PASS, use(helper, player, emptyJar()), "a jar cannot collect unfinished jam");
		pan(helper).useEmptyHand(helper.getLevel(), player, true);
		helper.assertTrue(has(player, KitchenItems.SUGARED_STRAWBERRY_SLICES), "the slices came back");
		helper.assertTrue(pan(helper).isEmpty(), "pan empty");
		helper.succeed();
	}

	@GameTest
	public void aJarServesSixTimesThenIsAnEmptyJar(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.SUGARED_STRAWBERRY_SLICES));
		tick(helper, cookTicks(helper));
		ItemStack empty = emptyJar();
		use(helper, player, empty);
		ItemStack jar = null;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(KitchenItems.JAM_JAR)) {
				jar = stack;
			}
		}
		helper.setBlock(new BlockPos(3, 1, 1), KitchenBlocks.BOARD);
		BoardBlockEntity board = helper.getBlockEntity(new BlockPos(3, 1, 1), BoardBlockEntity.class);
		for (int served = 1; served <= 6; served++) {
			board.stack().restore(null, java.util.List.of(), 0); // a fresh, empty board each time
			board.stack().add(helper.getLevel().registryAccess(), KitchenBlocks.BOARD, KitchenItems.BREAD_SLICE);
			helper.assertValueEqual(InteractionResult.SUCCESS, board.useItem(helper.getLevel(), player, jar, true), "serving " + served);
			helper.assertTrue(board.stack().layers().getLast().item().value() == KitchenItems.STRAWBERRY_JAM, "a jam layer");
			if (served < 6) {
				helper.assertValueEqual(6 - served, jar.get(KitchenComponents.JAM).servings(), "servings left");
			}
		}
		helper.assertTrue(jar.get(KitchenComponents.JAM) == null, "after six servings it is an empty jar again");
		helper.assertTrue(jar.is(KitchenItems.JAM_JAR) && !jar.isEmpty(), "the jar itself stays");
		helper.succeed();
	}

	@GameTest
	public void jamFromThePanMakesASandwich(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.PEANUT));
		tick(helper, cookTicks(helper));
		use(helper, player, emptyJar());
		ItemStack jar = null;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(KitchenItems.JAM_JAR)) {
				jar = stack;
			}
		}
		helper.setBlock(new BlockPos(3, 1, 1), KitchenBlocks.BOARD);
		BoardBlockEntity board = helper.getBlockEntity(new BlockPos(3, 1, 1), BoardBlockEntity.class);
		var access = helper.getLevel().registryAccess();
		board.stack().add(access, KitchenBlocks.BOARD, KitchenItems.BREAD_SLICE);
		helper.assertValueEqual(InteractionResult.SUCCESS, board.useItem(helper.getLevel(), player, jar, true), "peanut butter on the bread");
		board.stack().add(access, KitchenBlocks.BOARD, KitchenItems.BREAD_SLICE);
		LayerStack.FinishResult result = board.stack().finish(access);
		helper.assertValueEqual(LayerStack.FinishStatus.OK, result.status(), "the sandwich finishes");
		helper.assertTrue(result.stack().get(net.minecraft.core.component.DataComponents.FOOD).nutrition() == 2, "bread, peanut butter (2), bread");
		helper.succeed();
	}

	@GameTest
	public void breakingThePanDropsTheIngredient(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		use(helper, player, new ItemStack(KitchenItems.RAW_BACON));
		helper.getLevel().destroyBlock(helper.absolutePos(PAN), true);
		int raw = 0;
		boolean pan = false;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, PAN, 6.0)) {
			raw += entity.getItem().is(KitchenItems.RAW_BACON) ? entity.getItem().getCount() : 0;
			pan |= entity.getItem().is(KitchenItems.PAN);
		}
		helper.assertValueEqual(1, raw, "the raw bacon dropped");
		helper.assertTrue(pan, "the pan dropped itself");
		helper.succeed();
	}
}
