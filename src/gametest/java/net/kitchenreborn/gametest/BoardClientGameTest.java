package net.kitchenreborn.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Visual check of the board renderer (milestone M3b): builds a few boards and takes screenshots into
 * {@code build/run/clientGameTest/screenshots}. Run with {@code ./gradlew runClientGameTest}; it is a
 * development aid, not part of {@code build}.
 */
@SuppressWarnings("UnstableApiUsage")
public class BoardClientGameTest implements FabricClientGameTest {
	private static String layers(String... ids) {
		StringBuilder list = new StringBuilder("[");
		for (int i = 0; i < ids.length; i++) {
			list.append(i == 0 ? "" : ",").append("{id:\"kitchen:").append(ids[i]).append("\",count:1}");
		}
		return list.append("]").toString();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();

			// Three boards in a row at the flat world's surface (y = -60): a sandwich, a tall stack, a whole ingredient.
			world.getServer().runCommand("setblock -2 -60 0 kitchen:board[axis=x]");
			world.getServer().runCommand("setblock 0 -60 0 kitchen:board[axis=x]");
			world.getServer().runCommand("setblock 2 -60 0 kitchen:board[axis=z]");
			world.getServer().runCommand("data merge block -2 -60 0 {food_type:\"kitchen:sandwich\",layers:"
					+ layers("bread_slice", "lettuce_leaf", "tomato_slice", "bacon", "cheese_slice", "bread_slice") + "}");
			world.getServer().runCommand("data merge block 0 -60 0 {food_type:\"kitchen:sandwich\",layers:"
					+ layers("bread_slice", "butter", "roast_beef", "cheese_slice", "raw_bacon", "carrot_slice",
							"potato_slice", "toast") + ",pending_spread:0}");
			world.getServer().runCommand("data merge block 2 -60 0 {workpiece:{id:\"minecraft:porkchop\",count:1}}");

			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("tp @p 0.5 -60 3.2 180 50");
			context.waitTicks(20);
			context.takeScreenshot("board_all_three");

			world.getServer().runCommand("tp @p -1.5 -60 1.6 180 55");
			context.waitTicks(10);
			context.takeScreenshot("board_sandwich_close");

			world.getServer().runCommand("tp @p 0.5 -60 1.6 180 55");
			context.waitTicks(10);
			context.takeScreenshot("board_tall_stack_close");

			world.getServer().runCommand("tp @p 2.5 -60 1.6 180 55");
			context.waitTicks(10);
			context.takeScreenshot("board_workpiece_close");
		}
	}
}
