package net.kitchenreborn.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.core.Direction;

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
			heldPlates(context, world);
			emptyPlateAndBoardItems(context, world);
			ovens(context, world);
			pans(context, world);
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

	/** A plate item with the given contents. */
	private static String plateItem(String contents) {
		return "kitchen:plate[kitchen:plate_contents=" + contents + "]";
	}

	private static String sandwichOnPlate() {
		return "[{id:\"kitchen:sandwich\",count:1,components:{\"kitchen:layered_food\":{type:\"kitchen:sandwich\",layers:"
				+ layerList(three()) + "}}}]";
	}

	private static String mixedOnPlate() {
		return layerList("bacon", "cheese_slice", "tomato_slice", "lettuce_leaf", "roast_beef", "carrot_slice", "bread_slice", "raw_bacon");
	}

	/** Plate items with a sandwich and with mixed items: GUI, hand (both), third person, dropped; placed plates beside them. */
	private void heldPlates(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("setblock 70 -60 20 kitchen:plate[rotation=8]");
		server.runCommand("setblock 68 -60 20 kitchen:plate[rotation=8]");
		server.runCommand("data merge block 70 -60 20 {items:" + sandwichOnPlate() + "}");
		server.runCommand("data merge block 68 -60 20 {items:" + mixedOnPlate() + "}");
		server.runCommand("item replace entity @p hotbar.0 with " + plateItem(sandwichOnPlate()));
		server.runCommand("item replace entity @p hotbar.1 with " + plateItem(mixedOnPlate()));
		server.runCommand("item replace entity @p hotbar.2 with kitchen:plate");
		server.runCommand("tp @p 69.5 -60 24 180 10");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		context.takeScreenshot("m51_plate_gui_inventory");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);

		// First person, both hands, sandwich plate then mixed plate.
		server.runCommand("tp @p 69 -60 21.8 180 45");
		for (String contents : new String[] {sandwichOnPlate(), mixedOnPlate()}) {
			String tag = contents.equals(sandwichOnPlate()) ? "sandwich" : "mixed";
			server.runCommand("item replace entity @p weapon.mainhand with " + plateItem(contents));
			context.waitTicks(10);
			context.takeScreenshot("m51_fp_main_" + tag);
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
			server.runCommand("item replace entity @p weapon.offhand with " + plateItem(contents));
			context.waitTicks(10);
			context.takeScreenshot("m51_fp_offhand_" + tag);
			server.runCommand("item replace entity @p weapon.offhand with minecraft:air");
		}

		// Third person, from behind, from above and from the front.
		server.runCommand("item replace entity @p weapon.mainhand with " + plateItem(sandwichOnPlate()));
		server.runCommand("tp @p 69.5 -60 23.5 180 20");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(10);
		context.takeScreenshot("m51_tp_back_sandwich");
		server.runCommand("tp @p 69.5 -60 23.5 180 90");
		context.waitTicks(10);
		context.takeScreenshot("m51_tp_birdseye_sandwich");
		server.runCommand("item replace entity @p weapon.mainhand with " + plateItem(mixedOnPlate()));
		context.waitTicks(10);
		context.takeScreenshot("m51_tp_birdseye_mixed");
		server.runCommand("tp @p 69.5 -60 23.5 180 20");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		server.runCommand("tp @p 69.5 -60 21.5 0 20");
		context.waitTicks(10);
		context.takeScreenshot("m51_tp_front_mixed");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");

		// Dropped, and in item frames.
		server.runCommand("summon minecraft:item 71.5 -60 20 {PickupDelay:32767s,Item:{id:\"kitchen:plate\",count:1,components:{\"kitchen:plate_contents\":"
				+ sandwichOnPlate() + "}}}");
		server.runCommand("summon minecraft:item 66.5 -60 20 {PickupDelay:32767s,Item:{id:\"kitchen:plate\",count:1,components:{\"kitchen:plate_contents\":"
				+ mixedOnPlate() + "}}}");
		server.runCommand("tp @p 69 -60 24 180 25");
		context.waitTicks(30);
		context.takeScreenshot("m51_dropped_beside_plates");
		server.runCommand("fill 76 -60 12 81 -57 12 minecraft:stone");
		server.runCommand("summon minecraft:item_frame 77 -59 13 {Facing:3b,Item:{id:\"kitchen:plate\",count:1,components:{\"kitchen:plate_contents\":"
				+ sandwichOnPlate() + "}}}");
		server.runCommand("summon minecraft:item_frame 80 -59 13 {Facing:3b,Item:{id:\"kitchen:plate\",count:1,components:{\"kitchen:plate_contents\":"
				+ mixedOnPlate() + "}}}");
		server.runCommand("tp @p 78.5 -60 18 180 12");
		context.waitTicks(20);
		context.takeScreenshot("m51_item_frames");
	}

	/**
	 * 0.5.2: the empty plate item, the plate with a sandwich in first person, and the cutting board item (GUI, first
	 * and third person), each next to a placed plate / board for size. Screenshots are prefixed "m52_".
	 */
	private void emptyPlateAndBoardItems(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("setblock 100 -60 20 kitchen:plate[rotation=8]");
		server.runCommand("setblock 98 -60 20 kitchen:plate[rotation=8]");
		server.runCommand("setblock 102 -60 20 kitchen:board[axis=x]");
		server.runCommand("data merge block 100 -60 20 {items:" + sandwichOnPlate() + "}");
		server.runCommand("item replace entity @p hotbar.0 with kitchen:board");
		server.runCommand("item replace entity @p hotbar.1 with kitchen:plate");
		server.runCommand("item replace entity @p hotbar.2 with " + plateItem(sandwichOnPlate()));
		server.runCommand("tp @p 100 -60 24 180 10");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		context.takeScreenshot("m52_gui_board_plate_loaded");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);

		String[] names = {"board", "empty_plate", "loaded_plate"};
		String[] items = {"kitchen:board", "kitchen:plate", plateItem(sandwichOnPlate())};
		server.runCommand("tp @p 100 -60 21.8 180 45");
		for (int i = 0; i < items.length; i++) {
			server.runCommand("item replace entity @p weapon.mainhand with " + items[i]);
			context.waitTicks(10);
			context.takeScreenshot("m52_fp_main_" + names[i]);
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
			server.runCommand("item replace entity @p weapon.offhand with " + items[i]);
			context.waitTicks(10);
			context.takeScreenshot("m52_fp_offhand_" + names[i]);
			server.runCommand("item replace entity @p weapon.offhand with minecraft:air");
		}
		server.runCommand("tp @p 100.5 -60 23.5 180 20");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		for (int i = 0; i < 2; i++) {
			server.runCommand("item replace entity @p weapon.mainhand with " + items[i]);
			context.waitTicks(10);
			context.takeScreenshot("m52_tp_back_" + names[i]);
		}
		server.runCommand("tp @p 100.5 -60 23.5 180 90");
		for (int i = 0; i < 2; i++) {
			server.runCommand("item replace entity @p weapon.mainhand with " + items[i]);
			context.waitTicks(10);
			context.takeScreenshot("m52_tp_birdseye_" + names[i]);
		}
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		server.runCommand("summon minecraft:item 103.5 -60 20 {PickupDelay:32767s,Item:{id:\"kitchen:board\",count:1}}");
		server.runCommand("summon minecraft:item 96.5 -60 20 {PickupDelay:32767s,Item:{id:\"kitchen:plate\",count:1}}");
		server.runCommand("tp @p 100 -60 24 180 25");
		context.waitTicks(30);
		context.takeScreenshot("m52_dropped_board_and_empty_plate");

		// Empty plates stacked on a plate (the plate model, nested), with food on top.
		server.runCommand("setblock 106 -60 20 kitchen:plate[rotation=8]");
		server.runCommand("setblock 108 -60 20 kitchen:plate[rotation=8]");
		server.runCommand("data merge block 106 -60 20 {items:[{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1},{id:\"kitchen:bacon\",count:1},{id:\"kitchen:cheese_slice\",count:1}]}");
		server.runCommand("data merge block 108 -60 20 {items:[{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1},{id:\"kitchen:plate\",count:1}]}");
		server.runCommand("tp @p 107 -60 22.5 180 40");
		context.waitTicks(20);
		context.takeScreenshot("m53_nested_empty_plates");
	}

	private static final String OVEN_ITEMS = "{slots:[{id:\"kitchen:raw_bacon\",count:4},{id:\"kitchen:raw_roast_beef\",count:3},"
			+ "{id:\"kitchen:raw_chicken_fillet\",count:2},{id:\"minecraft:porkchop\",count:1}]}";

	/** Ovens (M6): closed, open with contents, lit while cooking, cooked and burnt, in all four facings; the item. */
	private void ovens(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("setblock 130 -60 20 kitchen:oven[facing=south]");
		server.runCommand("setblock 132 -60 20 kitchen:oven[facing=south,open=true]");
		server.runCommand("setblock 134 -60 20 kitchen:oven[facing=south,lit=true]");
		server.runCommand("data merge block 132 -60 20 " + OVEN_ITEMS);
		server.runCommand("data merge block 134 -60 20 " + OVEN_ITEMS);
		server.runCommand("tp @p 132 -60 25 180 25");
		context.waitTicks(25);
		context.takeScreenshot("m6_ovens_closed_open_lit");
		server.runCommand("tp @p 132 -58 23 180 55");
		context.waitTicks(10);
		context.takeScreenshot("m6_oven_open_contents_from_above");
		server.runCommand("tp @p 132 -60 22.5 180 20");
		context.waitTicks(10);
		context.takeScreenshot("m6_oven_open_front");

		// All four facings, closed and open.
		server.runCommand("setblock 140 -60 20 kitchen:oven[facing=north,open=true]");
		server.runCommand("setblock 142 -60 20 kitchen:oven[facing=east,open=true]");
		server.runCommand("setblock 144 -60 20 kitchen:oven[facing=south,open=true]");
		server.runCommand("setblock 146 -60 20 kitchen:oven[facing=west,open=true]");
		server.runCommand("tp @p 143 -57 24 180 40");
		context.waitTicks(20);
		context.takeScreenshot("m6_oven_facings_open");

		// The door animation: toggle the hatch and shoot at each stage of the swing (a step is 10 degrees a tick).
		server.runCommand("setblock 170 -60 20 kitchen:oven[facing=south]");
		server.runCommand("tp @p 172.2 -60 22.2 150 25");
		context.waitTicks(20);
		context.takeScreenshot("m61_oven_hatch_closed");
		server.runCommand("setblock 170 -60 20 kitchen:oven[facing=south,open=true]");
		context.waitTicks(4);
		context.takeScreenshot("m61_oven_hatch_mid_opening");
		context.waitTicks(20);
		context.takeScreenshot("m61_oven_hatch_open");
		server.runCommand("setblock 170 -60 20 kitchen:oven[facing=south,open=false]");
		context.waitTicks(4);
		context.takeScreenshot("m61_oven_hatch_mid_closing");
		server.runCommand("setblock 170 -60 20 kitchen:oven[facing=south,open=true,lit=true]");
		context.waitTicks(30);
		context.takeScreenshot("m61_oven_hatch_open_lit");

		// A cooked and a burnt oven: smoke (and flames) from the front once done.
		server.runCommand("setblock 150 -60 20 kitchen:oven[facing=south]");
		server.runCommand("setblock 152 -60 20 kitchen:oven[facing=south]");
		server.runCommand("data merge block 150 -60 20 {slots:[{id:\"kitchen:bacon\",count:4}],stage:1,has_fuel:true,cooking:true,cook_time:450}");
		server.runCommand("data merge block 152 -60 20 {slots:[{id:\"kitchen:burnt_meat\",count:4}],stage:2,has_fuel:true,cooking:true,cook_time:650}");
		server.runCommand("tp @p 151 -60 24 180 25");
		context.waitTicks(40);
		context.takeScreenshot("m6_oven_cooked_and_burnt_smoke");

		// The item: GUI, first and third person, dropped.
		server.runCommand("item replace entity @p hotbar.0 with kitchen:oven");
		server.runCommand("tp @p 160 -60 24 180 10");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		context.takeScreenshot("m6_oven_item_gui");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		server.runCommand("setblock 160 -60 20 kitchen:oven[facing=south]");
		server.runCommand("tp @p 160 -60 22 180 15");
		server.runCommand("item replace entity @p weapon.mainhand with kitchen:oven");
		context.waitTicks(10);
		context.takeScreenshot("m6_oven_item_first_person");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		server.runCommand("item replace entity @p weapon.offhand with kitchen:oven");
		context.waitTicks(10);
		context.takeScreenshot("m6_oven_item_offhand");
		server.runCommand("item replace entity @p weapon.offhand with minecraft:air");
		server.runCommand("item replace entity @p weapon.mainhand with kitchen:oven");
		server.runCommand("tp @p 160.5 -60 23.5 180 20");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(10);
		context.takeScreenshot("m6_oven_item_third_person");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
	}

	private static String jar(String jam, int servings) {
		return "kitchen:jam_jar[kitchen:jam={jam:\"kitchen:" + jam + "\",servings:" + servings + "}]";
	}

	/** Pans (M7): empty, raw and fried food, the three jam stages in three colours, all four facings; jars and items. */
	private void pans(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("effect give @p minecraft:resistance 1000 5 true");
		// Row of ovens with pans: empty, raw bacon, cooked bacon, and the four jam stages (strawberry, apple, peanut).
		String[] contents = {
				"{}",
				"{input:{id:\"kitchen:raw_bacon\",count:1},result:{id:\"kitchen:bacon\",count:1}}",
				"{input:{id:\"kitchen:raw_bacon\",count:1},result:{id:\"kitchen:bacon\",count:1},done:true,stage:3}",
				"{input:{id:\"kitchen:raw_chicken_fillet\",count:1},result:{id:\"kitchen:chicken_fillet\",count:1},done:true,stage:3}"};
		for (int i = 0; i < contents.length; i++) {
			int x = 200 + 2 * i;
			server.runCommand("setblock " + x + " -60 20 kitchen:oven[facing=south]");
			server.runCommand("setblock " + x + " -59 20 kitchen:pan[facing=south]");
			if (i > 0) {
				server.runCommand("data merge block " + x + " -59 20 " + contents[i]);
			}
		}
		server.runCommand("tp @p 203 -59 24 180 20");
		context.waitTicks(30);
		context.takeScreenshot("m7_pans_empty_raw_fried");
		server.runCommand("tp @p 203 -55 20.8 180 90");
		context.waitTicks(10);
		context.takeScreenshot("m7_pans_closeup");

		// Jam: strawberry, apple, peanut at stages 0..3.
		String[] jams = {"strawberry_jam", "apple_jam", "peanut_jam"};
		String[] sources = {"sugared_strawberry_slices", "apple_slice", "peanut"};
		for (int j = 0; j < 3; j++) {
			for (int stage = 0; stage < 4; stage++) {
				int x = 210 + 2 * stage;
				int z = 20 + 3 * j;
				server.runCommand("setblock " + x + " -60 " + z + " kitchen:oven[facing=south]");
				server.runCommand("setblock " + x + " -59 " + z + " kitchen:pan[facing=south]");
				server.runCommand("data merge block " + x + " -59 " + z + " {input:{id:\"kitchen:" + sources[j] + "\",count:1},"
						+ "result:{id:\"kitchen:jam_jar\",count:1,components:{\"kitchen:jam\":{jam:\"kitchen:" + jams[j] + "\",servings:6}}},stage:" + stage
						+ ",done:" + (stage == 3) + "}");
			}
		}
		server.runCommand("tp @p 213 -55 20.8 180 90");
		context.waitTicks(30);
		context.takeScreenshot("m7_jam_stages_strawberry");
		server.runCommand("tp @p 213 -55 23.8 180 90");
		context.waitTicks(10);
		context.takeScreenshot("m7_jam_stages_apple");
		server.runCommand("tp @p 213 -55 26.8 180 90");
		context.waitTicks(10);
		context.takeScreenshot("m7_jam_stages_peanut");

		// The four facings, ovens facing each way with their pans.
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < 4; i++) {
			int x = 220 + 2 * i;
			server.runCommand("setblock " + x + " -60 20 kitchen:oven[facing=" + facings[i].getName() + "]");
			server.runCommand("setblock " + x + " -59 20 kitchen:pan[facing=" + facings[i].getName() + "]");
			server.runCommand("data merge block " + x + " -59 20 " + contents[2]);
		}
		server.runCommand("tp @p 223 -59 24.5 180 40");
		context.waitTicks(25);
		context.takeScreenshot("m7_pan_facings");

		// Smoke when done: one pan with finished food, close up.
		server.runCommand("tp @p 202 -58 22.5 180 40");
		context.waitTicks(5);

		// The jars (empty, 1..6 servings, three jams) in the hotbar and inventory, the pan item.
		server.runCommand("item replace entity @p hotbar.0 with kitchen:jam_jar");
		server.runCommand("item replace entity @p hotbar.1 with " + jar("strawberry_jam", 6));
		server.runCommand("item replace entity @p hotbar.2 with " + jar("apple_jam", 3));
		server.runCommand("item replace entity @p hotbar.3 with " + jar("peanut_jam", 1));
		server.runCommand("item replace entity @p hotbar.4 with kitchen:pan");
		for (int i = 1; i <= 6; i++) {
			server.runCommand("item replace entity @p inventory." + (i - 1) + " with " + jar("strawberry_jam", i));
			server.runCommand("item replace entity @p inventory." + (8 + i) + " with " + jar("apple_jam", i));
			server.runCommand("item replace entity @p inventory." + (17 + i) + " with " + jar("peanut_jam", i));
		}
		server.runCommand("tp @p 230 -60 24 180 10");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(10);
		context.takeScreenshot("m7_jars_and_pan_item_gui");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitTicks(5);
		server.runCommand("setblock 230 -60 20 kitchen:oven[facing=south]");
		server.runCommand("tp @p 230 -60 22 180 15");
		server.runCommand("item replace entity @p weapon.mainhand with " + jar("strawberry_jam", 6));
		context.waitTicks(10);
		context.takeScreenshot("m7_jar_first_person");
		server.runCommand("item replace entity @p weapon.mainhand with kitchen:pan");
		context.waitTicks(10);
		context.takeScreenshot("m7_pan_item_first_person");
		server.runCommand("tp @p 230.5 -60 23.5 180 20");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(10);
		context.takeScreenshot("m7_pan_item_third_person");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
	}
}
