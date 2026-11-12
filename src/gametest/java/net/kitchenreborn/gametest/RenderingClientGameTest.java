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
			plates(context, world);
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

	/**
	 * Sandwiches next to boards holding the same layers, so sizes can be compared: outside the GUI a sandwich must be
	 * as big as it was on the board.
	 */
	private void sandwiches(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("tp @p 20 -60 22 180 10");
		world.getServer().runCommand("item replace entity @p hotbar.0 with " + sandwich(three()));
		world.getServer().runCommand("item replace entity @p hotbar.1 with " + sandwich(sixteen()));

		// GUI: both sandwiches in the hotbar, inventory open (tall stacks may shrink here).
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		context.takeScreenshot("m4_gui_inventory");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);

		// The reference: the same 3 and 16 layers on boards.
		world.getServer().runCommand("setblock 20 -60 20 kitchen:board[axis=x]");
		world.getServer().runCommand("setblock 18 -60 20 kitchen:board[axis=x]");
		world.getServer().runCommand("data merge block 20 -60 20 {food_type:\"kitchen:sandwich\",layers:" + layerList(three()) + "}");
		world.getServer().runCommand("data merge block 18 -60 20 {food_type:\"kitchen:sandwich\",layers:" + layerList(sixteen()) + "}");

		// Hand, first person, main hand and off hand: 3 layers then 16, looking at the matching board.
		world.getServer().runCommand("tp @p 20.5 -60 21.8 180 45");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(three()));
		context.waitTicks(10);
		context.takeScreenshot("m4b_fp_main_3_layers_beside_board");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		world.getServer().runCommand("item replace entity @p weapon.offhand with " + sandwich(three()));
		context.waitTicks(10);
		context.takeScreenshot("m4b_fp_offhand_3_layers_beside_board");
		world.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:air");

		world.getServer().runCommand("tp @p 19 -60 21.8 180 45");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(sixteen()));
		context.waitTicks(10);
		context.takeScreenshot("m4b_fp_main_16_layers_beside_board");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		world.getServer().runCommand("item replace entity @p weapon.offhand with " + sandwich(sixteen()));
		context.waitTicks(10);
		context.takeScreenshot("m4b_fp_offhand_16_layers_beside_board");
		world.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:air");

		// Third person: the player holds a sandwich next to the board (F5 back, then F5 front).
		world.getServer().runCommand("tp @p 20.5 -60 23.5 180 20");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(three()));
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(10);
		context.takeScreenshot("m4b_tp_back_3_layers_beside_board");
		// Bird's eye view of the held sandwich (camera looks straight down from behind and above).
		world.getServer().runCommand("tp @p 20.5 -60 23.5 180 90");
		context.waitTicks(10);
		context.takeScreenshot("m4b_tp_birdseye_3_layers");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(sixteen()));
		context.waitTicks(10);
		context.takeScreenshot("m4b_tp_birdseye_16_layers");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(three()));
		world.getServer().runCommand("tp @p 20.5 -60 23.5 180 20");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		world.getServer().runCommand("tp @p 20.5 -60 21.5 0 20");
		context.waitTicks(10);
		context.takeScreenshot("m4b_tp_front_3_layers");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(sixteen()));
		context.waitTicks(10);
		context.takeScreenshot("m4b_tp_front_16_layers");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:air");

		// Eating: hold use for a while so the item moves to the mouth and eating particles spawn.
		world.getServer().runCommand("tp @p 20.5 -60 21.8 180 10");
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(three()));
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(12);
		context.takeScreenshot("m4c_eating_3_layers");
		context.waitTicks(10);
		context.takeScreenshot("m4c_eating_3_layers_particles");
		context.getInput().releaseKey(options -> options.keyUse);
		world.getServer().runCommand("item replace entity @p weapon.mainhand with " + sandwich(sixteen()));
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(12);
		context.takeScreenshot("m4c_eating_16_layers");
		context.getInput().releaseKey(options -> options.keyUse);
		world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:air");

		// Dropped items on the ground right beside the boards.
		world.getServer().runCommand("summon minecraft:item 21.5 -60 20 {PickupDelay:32767s,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(three()) + "}}}}");
		world.getServer().runCommand("summon minecraft:item 16.5 -60 20 {PickupDelay:32767s,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(sixteen()) + "}}}}");
		world.getServer().runCommand("tp @p 19 -60 24 180 25");
		context.waitTicks(30);
		context.takeScreenshot("m4b_dropped_beside_boards");

		// Item frames on a wall with a board in front for comparison.
		world.getServer().runCommand("fill 28 -60 12 33 -57 12 minecraft:stone");
		world.getServer().runCommand("setblock 30 -60 14 kitchen:board[axis=x]");
		world.getServer().runCommand("data merge block 30 -60 14 {food_type:\"kitchen:sandwich\",layers:" + layerList(three()) + "}");
		world.getServer().runCommand("summon minecraft:item_frame 29 -59 13 {Facing:3b,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(three()) + "}}}}");
		world.getServer().runCommand("summon minecraft:item_frame 32 -59 13 {Facing:3b,Item:{id:\"kitchen:sandwich\",count:1,"
				+ "components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:" + layerList(sixteen()) + "}}}}");
		world.getServer().runCommand("tp @p 30.5 -60 18 180 12");
		context.waitTicks(20);
		context.takeScreenshot("m4b_item_frames_beside_board");
	}

	private static String itemList(String... ids) {
		return layerList(ids);
	}

	/** Plates: a sandwich, mixed plain items, a full plate, and the four facings (M5). */
	private void plates(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("setblock 40 -60 0 kitchen:plate[rotation=0]");
		server.runCommand("setblock 42 -60 0 kitchen:plate[rotation=0]");
		server.runCommand("setblock 44 -60 0 kitchen:plate[rotation=0]");
		server.runCommand("data merge block 40 -60 0 {items:" + itemList("bread_slice", "butter", "bacon", "cheese_slice", "bread_slice") + "}");
		// A finished sandwich on a plate with an item on top of it.
		server.runCommand("data merge block 42 -60 0 {items:[{id:\"kitchen:sandwich\",count:1,components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:"
				+ layerList(three()) + "}}},{id:\"kitchen:tomato_slice\",count:1}]}");
		server.runCommand("data merge block 44 -60 0 {items:" + itemList("bacon", "cheese_slice", "tomato_slice", "lettuce_leaf", "roast_beef",
				"carrot_slice", "bread_slice", "raw_bacon", "chicken_fillet", "potato_slice") + "}");
		server.runCommand("tp @p 42 -60 4 180 35");
		context.waitTicks(20);
		context.takeScreenshot("m5_plates_overview");
		server.runCommand("tp @p 40 -60 2.2 180 55");
		context.waitTicks(10);
		context.takeScreenshot("m5_plate_items_closeup");
		server.runCommand("tp @p 42 -60 2.2 180 55");
		context.waitTicks(10);
		context.takeScreenshot("m5_plate_sandwich_closeup");
		server.runCommand("tp @p 44 -60 2.2 180 55");
		context.waitTicks(10);
		context.takeScreenshot("m5_plate_mixed_closeup");

		// Facings: rotation 0, 4, 8, 12 with the same sandwich.
		for (int i = 0; i < 4; i++) {
			server.runCommand("setblock " + (50 + 2 * i) + " -60 0 kitchen:plate[rotation=" + (4 * i) + "]");
			server.runCommand("data merge block " + (50 + 2 * i) + " -60 0 {items:[{id:\"kitchen:sandwich\",count:1,components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:"
					+ layerList(three()) + "}}}]}");
		}
		server.runCommand("tp @p 53 -60 4 180 45");
		context.waitTicks(20);
		context.takeScreenshot("m5_plate_facings");

		// A full plate (32 items) and the plate as an item in hand and on the ground.
		server.runCommand("setblock 46 -60 0 kitchen:plate[rotation=0]");
		StringBuilder many = new StringBuilder("[");
		for (int i = 0; i < 32; i++) {
			many.append(i == 0 ? "" : ",").append("{id:\"kitchen:").append(i % 2 == 0 ? "bacon" : "cheese_slice").append("\",count:1}");
		}
		server.runCommand("data merge block 46 -60 0 {items:" + many + "]}");
		server.runCommand("tp @p 46 -60 3 180 40");
		context.waitTicks(15);
		context.takeScreenshot("m5_plate_full_32");
	}
}
