package net.kitchenreborn.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;

/**
 * Visual checks of the food renderers (milestones M3b and M4): boards with 3D layer models, and sandwiches in
 * the GUI, hand (first and third person), item frames and as dropped items. Screenshots go to
 * {@code build/run/clientGameTest/screenshots}. Run with {@code ./gradlew runClientGameTest}; this is a
 * development aid and is not part of {@code build}.
 */
@SuppressWarnings("UnstableApiUsage")
public class RenderingClientGameTest implements FabricClientGameTest {
	private static String layerList(String... ids) {
		StringBuilder list = new StringBuilder("[");
		for (int i = 0; i < ids.length; i++) {
			list.append(i == 0 ? "" : ",").append("{id:\"kitchen:").append(ids[i]).append("\",count:1}");
		}
		return list.append("]").toString();
	}

	/** Item stack syntax for a finished sandwich with the given layers (bottom to top). */
	private static String sandwich(String... layers) {
		return "kitchen:sandwich[kitchen:layered_food={type:\"kitchen:sandwich\",layers:" + layerList(layers) + "}]";
	}

	private static String[] three() {
		return new String[] {"bread_slice", "bacon", "bread_slice"};
	}

	/** 16 layers: bread, then fillings and spreads, bread. */
	private static String[] sixteen() {
		return new String[] {"bread_slice", "butter", "lettuce_leaf", "tomato_slice", "bacon", "cheese_slice",
				"roast_beef", "carrot_slice", "potato_slice", "raw_bacon", "chicken_fillet", "cheese_slice",
				"lettuce_leaf", "tomato_slice", "strawberry_jam", "bread_slice"};
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set noon");
			boards(context, world);
			sandwiches(context, world);
		}
	}

	private void boards(ClientGameTestContext context, TestSingleplayerContext world) {
		// Four boards at the flat world's surface (y = -60): the 3D models bread, butter (spread / half / fresh)
		// and both kinds of bacon.
		world.getServer().runCommand("setblock -3 -60 0 kitchen:board[axis=x]");
		world.getServer().runCommand("setblock -1 -60 0 kitchen:board[axis=x]");
		world.getServer().runCommand("setblock 1 -60 0 kitchen:board[axis=x]");
		world.getServer().runCommand("setblock 3 -60 0 kitchen:board[axis=z]");
		world.getServer().runCommand("data merge block -3 -60 0 {food_type:\"kitchen:sandwich\",layers:"
				+ layerList("bread_slice", "butter", "raw_bacon", "bacon", "cheese_slice", "bread_slice") + "}");
		world.getServer().runCommand("data merge block -1 -60 0 {food_type:\"kitchen:sandwich\",layers:"
				+ layerList("bread_slice", "butter") + ",pending_spread:2}");
		world.getServer().runCommand("data merge block 1 -60 0 {food_type:\"kitchen:sandwich\",layers:"
				+ layerList("bread_slice", "butter") + ",pending_spread:1}");
		world.getServer().runCommand("data merge block 3 -60 0 {food_type:\"kitchen:sandwich\",layers:"
				+ layerList("bread_slice", "bacon", "raw_bacon", "bread_slice") + "}");

		world.getServer().runCommand("tp @p 0 -60 4.2 180 45");
		context.waitTicks(20);
		context.takeScreenshot("m4_boards_overview");
		world.getServer().runCommand("tp @p -3 -60 1.4 180 62");
		context.waitTicks(10);
		context.takeScreenshot("m4_board_bread_butter_bacon_closeup");
		world.getServer().runCommand("tp @p -1 -60 1.4 180 62");
		context.waitTicks(10);
		context.takeScreenshot("m4_board_butter_fresh");
		world.getServer().runCommand("tp @p 1 -60 1.4 180 62");
		context.waitTicks(10);
		context.takeScreenshot("m4_board_butter_half_spread");
		world.getServer().runCommand("tp @p 3 -60 1.4 180 62");
		context.waitTicks(10);
		context.takeScreenshot("m4_board_both_bacons_z_axis");
	}

	private void sandwiches(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("tp @p 20 -60 20 180 10");
		world.getServer().runCommand("item replace entity @p hotbar.0 with " + sandwich(three()));
		world.getServer().runCommand("item replace entity @p hotbar.1 with " + sandwich(sixteen()));

		// GUI: both sandwiches in the hotbar, inventory open.
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		context.takeScreenshot("m4_gui_inventory");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);

		// Hand, first person, 3 layers then 16.
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(three()));
		context.waitTicks(10);
		context.takeScreenshot("m4_hand_first_person_3_layers");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(sixteen()));
		context.waitTicks(10);
		context.takeScreenshot("m4_hand_first_person_16_layers");

		// Hand, third person (from the front so the held item faces the camera).
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("m4_hand_third_person_16_layers");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(three()));
		context.waitTicks(10);
		context.takeScreenshot("m4_hand_third_person_3_layers");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

		// Item frames on a stone wall: 3 layers, 16 layers.
		world.getServer().runCommand("fill 18 -60 12 23 -57 12 minecraft:stone");
		world.getServer().runCommand("summon minecraft:item_frame 19 -59 13 {Facing:3b,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(three()) + "}}}}");
		world.getServer().runCommand("summon minecraft:item_frame 22 -59 13 {Facing:3b,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(sixteen()) + "}}}}");
		world.getServer().runCommand("tp @p 20.5 -60 17 180 5");
		context.waitTicks(20);
		context.takeScreenshot("m4_item_frames");

		// Dropped items, held in place so they do not roll away.
		world.getServer().runCommand("summon minecraft:item 18 -60 18 {PickupDelay:32767s,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(three()) + "}}}}");
		world.getServer().runCommand("summon minecraft:item 22 -60 18 {PickupDelay:32767s,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(sixteen()) + "}}}}");
		world.getServer().runCommand("tp @p 20 -60 21.5 180 40");
		context.waitTicks(20);
		context.takeScreenshot("m4_dropped_items");
	}
}
