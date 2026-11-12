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
import net.kitchenreborn.block.PlateBlock;
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

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (Item item : KitchenItems.ALL) {
			if (item == KitchenItems.SANDWICH) {
				generateSandwich(generators);
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
