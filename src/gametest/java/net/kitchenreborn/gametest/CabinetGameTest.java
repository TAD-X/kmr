package net.kitchenreborn.gametest;

import java.lang.reflect.Method;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.kitchenreborn.block.CabinetBlock;
import net.kitchenreborn.blockentity.CabinetBlockEntity;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenItems;

/** Tests for the kitchen cabinet container (milestone M8) and the tiles. */
public class CabinetGameTest implements CustomTestMethodInvoker {
	private static final BlockPos CABINET = new BlockPos(1, 1, 1);

	@Override
	public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
		helper.setBlock(CABINET, KitchenBlocks.CABINET);
		method.invoke(this, helper);
	}

	private static CabinetBlockEntity cabinet(GameTestHelper helper) {
		return helper.getBlockEntity(CABINET, CabinetBlockEntity.class);
	}

	@GameTest
	public void itHolds27Slots(GameTestHelper helper) {
		helper.assertValueEqual(27, cabinet(helper).getContainerSize(), "slots");
		for (int i = 0; i < 27; i++) {
			cabinet(helper).setItem(i, new ItemStack(Items.APPLE, 64));
		}
		helper.assertValueEqual(27 * 64, cabinet(helper).countItem(Items.APPLE), "everything fits");
		helper.succeed();
	}

	@GameTest
	public void itOpensAndClosesLikeABarrel(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(!helper.getBlockState(CABINET).getValue(CabinetBlock.OPEN), "closed at first");
		cabinet(helper).startOpen(player);
		helper.assertTrue(helper.getBlockState(CABINET).getValue(CabinetBlock.OPEN), "open while a player has it open");
		cabinet(helper).stopOpen(player);
		helper.assertTrue(!helper.getBlockState(CABINET).getValue(CabinetBlock.OPEN), "closed again");
		helper.succeed();
	}

	@GameTest
	public void comparatorsReadTheFillLevel(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(CABINET);
		helper.assertValueEqual(0, helper.getBlockState(CABINET).getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH), "empty");
		for (int i = 0; i < 27; i++) {
			cabinet(helper).setItem(i, new ItemStack(Items.APPLE, 64));
		}
		helper.assertValueEqual(15, helper.getBlockState(CABINET).getAnalogOutputSignal(helper.getLevel(), at, Direction.NORTH), "full");
		helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(cabinet(helper)) == 15, "same as vanilla");
		helper.succeed();
	}

	@GameTest
	public void breakingItDropsTheContents(GameTestHelper helper) {
		cabinet(helper).setItem(0, new ItemStack(Items.DIAMOND, 5));
		cabinet(helper).setItem(13, new ItemStack(KitchenItems.BACON, 3));
		helper.getLevel().destroyBlock(helper.absolutePos(CABINET), true);
		int diamonds = 0;
		int bacon = 0;
		boolean cabinet = false;
		for (ItemEntity entity : helper.getEntities(EntityTypes.ITEM, CABINET, 6.0)) {
			diamonds += entity.getItem().is(Items.DIAMOND) ? entity.getItem().getCount() : 0;
			bacon += entity.getItem().is(KitchenItems.BACON) ? entity.getItem().getCount() : 0;
			cabinet |= entity.getItem().is(KitchenItems.CABINET);
		}
		helper.assertValueEqual(5, diamonds, "diamonds");
		helper.assertValueEqual(3, bacon, "bacon");
		helper.assertTrue(cabinet, "the cabinet dropped itself");
		helper.succeed();
	}

	@GameTest
	public void theCornerLookFollowsTheOriginalsEightSteps(GameTestHelper helper) {
		// Step n of the placer's yaw: quarter n/2 is the facing, odd steps are the corner look.
		for (int step = 0; step < 8; step++) {
			float yaw = step * 45.0F;
			int computed = net.minecraft.util.Mth.floor(yaw * 8.0F / 360.0F + 0.5) & 7;
			helper.assertValueEqual(step, computed, "step for yaw " + yaw);
		}
		helper.assertTrue(KitchenBlocks.CABINET.defaultBlockState().hasProperty(CabinetBlock.CORNER), "corner property");
		helper.succeed();
	}

	@GameTest
	public void tilesHaveSlabsAndStairs(GameTestHelper helper) {
		helper.assertTrue(KitchenItems.TILES != null && KitchenItems.TILE_SLAB != null && KitchenItems.TILE_STAIRS != null, "items exist");
		helper.assertTrue(KitchenBlocks.TILE_SLAB.defaultBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE), "slab type");
		helper.assertTrue(KitchenBlocks.TILE_STAIRS.defaultBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF), "stairs half");
		helper.assertTrue(KitchenBlocks.TILES.defaultBlockState().getDestroySpeed(helper.getLevel(), helper.absolutePos(CABINET)) == 1.5F, "original hardness 1.5");
		helper.succeed();
	}
}
