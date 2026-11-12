package net.kitchenreborn.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.block.BoardBlock;
import net.kitchenreborn.block.CabinetBlock;
import net.kitchenreborn.block.OvenBlock;
import net.kitchenreborn.block.PanBlock;
import net.kitchenreborn.block.PlateBlock;
import net.kitchenreborn.client.render.JamItemModels;
import net.kitchenreborn.client.render.LayeredFoodRenderer;
import net.kitchenreborn.client.render.PlateItemRenderer;
import net.kitchenreborn.registry.KitchenBlocks;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenItems;

public class KitchenModelProvider extends FabricModelProvider {
	public KitchenModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
		generateBoard(generators);
		generatePlate(generators);
		generateOven(generators);
		generatePan(generators);
		generateDecor(generators);
		// Original stage mapping: age 0-1 -> stage 0, 2-3 -> 1, 4-6 -> 2, 7 -> 3.
		for (Block crop : KitchenBlocks.CROPS) {
			generators.createCropBlock(crop, CropBlock.AGE, 0, 0, 1, 1, 2, 2, 2, 3);
		}
		for (Block wild : KitchenBlocks.WILD_PLANTS) {
			generators.createCrossBlock(wild, BlockModelGenerators.PlantType.NOT_TINTED);
		}
	}

	/**
	 * The cutting board: a 14x2x10 pixel slab of oak planks (the original had no custom model, just oak planks
	 * with these bounds). One model per orientation, selected by the block's axis.
	 */
	private void generateBoard(BlockModelGenerators generators) {
		Identifier modelX = Kitchen.id("block/board_x");
		Identifier modelZ = Kitchen.id("block/board_z");
		generators.modelOutput.accept(modelX, boardModel(1, 3, 15, 13));
		generators.modelOutput.accept(modelZ, boardModel(3, 1, 13, 15));
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(KitchenBlocks.BOARD)
				.with(PropertyDispatch.initial(BoardBlock.AXIS)
						.select(Direction.Axis.X, BlockModelGenerators.plainVariant(modelX))
						.select(Direction.Axis.Z, BlockModelGenerators.plainVariant(modelZ))));
		generators.itemModelOutput.accept(KitchenItems.BOARD, ItemModelUtils.plainModel(modelX));
	}

	/**
	 * The oven: closed, open (hatch swung down) and lit (closed with the "active" texture). The hatch is on the
	 * front, which is the north side of the model, so every facing is a quarter turn of it.
	 */
	private void generateOven(BlockModelGenerators generators) {
		Identifier full = Kitchen.id("block/oven");
		Identifier body = Kitchen.id("block/oven_body");
		Identifier bodyLit = Kitchen.id("block/oven_body_lit");
		// The item shows the whole closed oven. The placed block is only the body: the hatch is drawn (and swung)
		// by the block entity renderer.
		generators.modelOutput.accept(full, LayerModelData.ovenJson(false, "oven_model"));
		generators.modelOutput.accept(body, LayerModelData.ovenBaseJson("oven_model"));
		generators.modelOutput.accept(bodyLit, LayerModelData.ovenBaseJson("oven_model_active"));
		PropertyDispatch.C3<MultiVariant, Direction, Boolean, Boolean> dispatch =
				PropertyDispatch.initial(OvenBlock.FACING, OvenBlock.OPEN, OvenBlock.LIT);
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			Quadrant turn = switch (facing) {
				case EAST -> Quadrant.R90;
				case SOUTH -> Quadrant.R180;
				case WEST -> Quadrant.R270;
				default -> Quadrant.R0;
			};
			for (boolean isOpen : new boolean[] {false, true}) {
				for (boolean isLit : new boolean[] {false, true}) {
					dispatch = dispatch.select(facing, isOpen, isLit,
							BlockModelGenerators.plainVariant(isLit ? bodyLit : body).with(VariantMutator.Y_ROT.withValue(turn)));
				}
			}
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(KitchenBlocks.OVEN).with(dispatch));
		// parent block/block: the standard 3/4 GUI view and held like a block
		generators.itemModelOutput.accept(KitchenItems.OVEN, ItemModelUtils.plainModel(full));
	}

	/**
	 * Cabinet and tiles. The cabinet is the original Techne model: closed doors, or (open) both doors swung 45
	 * degrees, with a solid front instead of doors for the corner look. The tiles use the original texture.
	 */
	private void generateDecor(BlockModelGenerators generators) {
		String[] names = {"kitchen_cabinet", "kitchen_cabinet_open", "kitchen_cabinet_corner"};
		for (String name : names) {
			generators.modelOutput.accept(Kitchen.id("block/" + name), LayerModelData.cabinetJson(name.endsWith("corner"), name.endsWith("open")));
		}
		PropertyDispatch.C3<MultiVariant, Direction, Boolean, Boolean> dispatch =
				PropertyDispatch.initial(CabinetBlock.FACING, CabinetBlock.CORNER, CabinetBlock.OPEN);
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			Quadrant turn = switch (facing) {
				case EAST -> Quadrant.R90;
				case SOUTH -> Quadrant.R180;
				case WEST -> Quadrant.R270;
				default -> Quadrant.R0;
			};
			for (boolean corner : new boolean[] {false, true}) {
				for (boolean open : new boolean[] {false, true}) {
					String name = corner ? "kitchen_cabinet_corner" : open ? "kitchen_cabinet_open" : "kitchen_cabinet";
					dispatch = dispatch.select(facing, corner, open,
							BlockModelGenerators.plainVariant(Kitchen.id("block/" + name)).with(VariantMutator.Y_ROT.withValue(turn)));
				}
			}
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(KitchenBlocks.CABINET).with(dispatch));
		generators.itemModelOutput.accept(KitchenItems.CABINET, ItemModelUtils.plainModel(Kitchen.id("block/kitchen_cabinet")));

		// Tiles, with slab and stairs, through the vanilla block family helper.
		generators.new BlockFamilyProvider(TextureMapping.cube(KitchenBlocks.TILES))
				.fullBlock(KitchenBlocks.TILES, ModelTemplates.CUBE_ALL)
				.slab(KitchenBlocks.TILE_SLAB)
				.stairs(KitchenBlocks.TILE_STAIRS);
	}

	/** The pan: the original model, a quarter turn per facing; the same facing as the oven under it. */
	private void generatePan(BlockModelGenerators generators) {
		Identifier model = Kitchen.id("block/pan");
		generators.modelOutput.accept(model, LayerModelData.panJson());
		PropertyDispatch.C1<MultiVariant, Direction> dispatch = PropertyDispatch.initial(PanBlock.FACING);
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			Quadrant turn = switch (facing) {
				case EAST -> Quadrant.R90;
				case SOUTH -> Quadrant.R180;
				case WEST -> Quadrant.R270;
				default -> Quadrant.R0;
			};
			dispatch = dispatch.select(facing, BlockModelGenerators.plainVariant(model).with(VariantMutator.Y_ROT.withValue(turn)));
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(KitchenBlocks.PAN).with(dispatch));
		generators.itemModelOutput.accept(KitchenItems.PAN, ItemModelUtils.plainModel(model));
	}

	/** The plate: the original Techne model as a block model, turned in quarter steps to follow the 16 rotations. */
	private void generatePlate(BlockModelGenerators generators) {
		Identifier model = Kitchen.id("block/plate");
		generators.modelOutput.accept(model, LayerModelData.toBlockJson(LayerModelData.PLATE, Kitchen.id("block/plate_model").toString()));
		PropertyDispatch.C1<MultiVariant, Integer> dispatch = PropertyDispatch.initial(PlateBlock.ROTATION);
		for (int rotation = 0; rotation < 16; rotation++) {
			Quadrant quarter = Quadrant.values()[((rotation + 2) / 4) % 4];
			dispatch = dispatch.select(rotation, BlockModelGenerators.plainVariant(model).with(VariantMutator.Y_ROT.withValue(quarter)));
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(KitchenBlocks.PLATE).with(dispatch));
		// Every plate, empty or loaded, goes through the special renderer so it is the same size everywhere:
		// the GUI fits it to the slot, first person pivots it like the sandwich, everything else is true size.
		ItemModel.Unbaked centered = ItemModelUtils.specialModel(Kitchen.id("item/plate_base"),
				new PlateItemRenderer.Unbaked(LayeredFoodRenderer.Mode.CENTER));
		generators.itemModelOutput.accept(KitchenItems.PLATE, ItemModelUtils.select(new DisplayContext(),
				ItemModelUtils.specialModel(Kitchen.id("item/plate_base"), new PlateItemRenderer.Unbaked(LayeredFoodRenderer.Mode.BASE)),
				ItemModelUtils.when(ItemDisplayContext.GUI,
						ItemModelUtils.specialModel(Kitchen.id("item/plate_base_gui"), new PlateItemRenderer.Unbaked(LayeredFoodRenderer.Mode.GUI))),
				ItemModelUtils.when(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, centered),
				ItemModelUtils.when(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, centered)));
	}

	/**
	 * The sandwich: while it carries a layered_food component it is drawn by the special renderer (every layer at
	 * its true height; base models kitchen:item/sandwich_base (scale 1 everywhere: cutting board size) and
	 * sandwich_base_gui supply the display transforms); without one, for
	 * example in an old creative tab, it falls back to the flat original sandwich.png.
	 */
	private static void generateSandwich(ItemModelGenerators generators) {
		ModelTemplates.FLAT_ITEM.create(KitchenItems.SANDWICH, TextureMapping.layer0(KitchenItems.SANDWICH),
				generators.modelOutput);
		// GUI: fitted to the slot (tall stacks shrink). Every other context: true cutting board size.
		ItemModel.Unbaked centered = ItemModelUtils.specialModel(Kitchen.id("item/sandwich_base"),
				new LayeredFoodRenderer.Unbaked(LayeredFoodRenderer.Mode.CENTER));
		ItemModel.Unbaked layered = ItemModelUtils.select(new DisplayContext(),
				ItemModelUtils.specialModel(Kitchen.id("item/sandwich_base"), new LayeredFoodRenderer.Unbaked(LayeredFoodRenderer.Mode.BASE)),
				ItemModelUtils.when(ItemDisplayContext.GUI,
						ItemModelUtils.specialModel(Kitchen.id("item/sandwich_base_gui"), new LayeredFoodRenderer.Unbaked(LayeredFoodRenderer.Mode.GUI))),
				ItemModelUtils.when(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, centered),
				ItemModelUtils.when(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, centered));
		generators.itemModelOutput.accept(KitchenItems.SANDWICH, ItemModelUtils.conditional(
				ItemModelUtils.hasComponent(KitchenComponents.LAYERED_FOOD),
				layered,
				ItemModelUtils.plainModel(Kitchen.id("item/sandwich"))));
	}

	private static JsonObject displayTransform(float[] rotation, float[] translation, float scale) {
		JsonObject json = new JsonObject();
		json.add("rotation", floats(rotation[0], rotation[1], rotation[2]));
		json.add("translation", floats(translation[0], translation[1], translation[2]));
		json.add("scale", floats(scale, scale, scale));
		return json;
	}

	private static ModelInstance boardModel(int x1, int z1, int x2, int z2) {
		return () -> {
			JsonObject face = new JsonObject();
			face.addProperty("texture", "#all");
			JsonObject faces = new JsonObject();
			for (String name : new String[] {"down", "up", "north", "south", "west", "east"}) {
				faces.add(name, face.deepCopy());
			}
			JsonObject element = new JsonObject();
			element.add("from", array(x1, 0, z1));
			element.add("to", array(x2, 2, z2));
			element.add("faces", faces);
			JsonArray elements = new JsonArray();
			elements.add(element);
			JsonObject textures = new JsonObject();
			textures.addProperty("all", "minecraft:block/oak_planks");
			textures.addProperty("particle", "minecraft:block/oak_planks");
			JsonObject model = new JsonObject();
			model.addProperty("parent", "minecraft:block/block"); // the standard block display transforms (3/4 view in the GUI, held like a block)
			model.add("textures", textures);
			model.add("elements", elements);
			// Held in first person like a block, lifted so the thin slab (at the bottom of its cube) is on screen.
			JsonObject display = new JsonObject();
			display.add("firstperson_righthand", displayTransform(new float[] {0, 45, 0}, new float[] {0, 6, 0}, 0.4F));
			display.add("firstperson_lefthand", displayTransform(new float[] {0, 225, 0}, new float[] {0, 6, 0}, 0.4F));
			model.add("display", display);
			return model;
		};
	}

	private static JsonArray floats(float... values) {
		JsonArray array = new JsonArray();
		for (float value : values) {
			array.add(value);
		}
		return array;
	}

	private static JsonArray array(int... values) {
		JsonArray array = new JsonArray();
		for (int value : values) {
			array.add(value);
		}
		return array;
	}

	/**
	 * The jam jar: empty it is the plain jar; with jam it shows the filling for the servings left (the original's
	 * jam_jar_filling_1..6), tinted with the jam's colour.
	 */
	private static void generateJar(ItemModelGenerators generators) {
		ModelTemplates.FLAT_ITEM.create(KitchenItems.JAM_JAR, TextureMapping.layer0(KitchenItems.JAM_JAR), generators.modelOutput);
		java.util.List<net.minecraft.client.renderer.item.RangeSelectItemModel.Entry> entries = new java.util.ArrayList<>();
		for (int servings = 1; servings <= 6; servings++) {
			entries.add(ItemModelUtils.override(ItemModelUtils.tintedModel(Kitchen.id("item/jam_jar_filled_" + servings),
					ItemModelUtils.constantTint(0xFFFFFF), new JamItemModels.JamColor()), servings));
		}
		generators.itemModelOutput.accept(KitchenItems.JAM_JAR, ItemModelUtils.rangeSelect(new JamItemModels.JamServings(),
				ItemModelUtils.plainModel(Kitchen.id("item/jam_jar")), entries));
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (Item item : KitchenItems.ALL) {
			if (item == KitchenItems.SANDWICH) {
				generateSandwich(generators);
				continue;
			}
			if (item == KitchenItems.JAM_JAR) {
				generateJar(generators);
				continue;
			}
			if (item instanceof BlockItem) {
				continue; // the crop block generator already writes the seed item's model
			}
			// Textures are assets/kitchen/textures/item/<id>.png (see tools/import_textures.py).
			boolean tool = item == KitchenItems.KNIFE;
			generators.generateFlatItem(item, tool ? ModelTemplates.FLAT_HANDHELD_ITEM : ModelTemplates.FLAT_ITEM);
		}
	}
}
