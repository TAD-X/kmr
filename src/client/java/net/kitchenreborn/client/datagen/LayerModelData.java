package net.kitchenreborn.client.datagen;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.resources.Identifier;

import net.kitchenreborn.Kitchen;

/**
 * The 3D layer models of the original (Techne models in reference/original/.../model/), converted to JSON
 * item models. The box tables below are copied from the original model classes; {@link #toJson} converts them.
 *
 * <p>The original drew these models like this (SandwichRender): scale 0.75, translate up 1.5 blocks, rotate 180
 * degrees around Z, so a model-space point (x, y, z) in pixels lands at
 * {@code (-0.75 x, 0.75 (24 - y), 0.75 z)} relative to the middle of the layer base. Model space has y pointing
 * down, and each box unwraps onto the texture the usual Techne way: u/v is the top-left of the unwrap, with the
 * faces laid out as (left d wide, front w, right d, back w) under (top, bottom) strips.
 */
public final class LayerModelData {
	/** One Techne box: texture offset (u, v), position (x, y, z) and size (w, h, d), all in model pixels. */
	record Box(float u, float v, float x, float y, float z, float w, float h, float d) { }

	/** A model: its id under models/item/layer/, texture (textures/item/layer/) and texture size. */
	record Model(String name, String texture, int textureWidth, int textureHeight, List<Box> boxes) { }

	private static final float SCALE = 0.75F;

	private static Box box(float u, float v, float x, float y, float z, float w, float h, float d) {
		return new Box(u, v, x, y, z, w, h, d);
	}

	private static final Model BREAD_SLICE_BOTTOM = new Model("bread_slice_bottom", "bread_slice_bottom", 64, 32, List.of(
			box(0, 0, -4F, 22F, -5F, 8, 1, 10),
			box(0, 0, -4F, 23F, -5F, 8, 1, 10),
			box(0, 12, -4F, 22F, 5F, 8, 1, 1),
			box(19, 12, -5F, 22F, -5F, 1, 1, 10),
			box(0, 18, 4F, 22F, -5F, 1, 1, 10),
			box(0, 15, -4F, 22F, -6F, 8, 1, 1)));

	private static final Model BREAD_SLICE_TOP = new Model("bread_slice_top", "bread_slice_top", 64, 32, List.of(
			box(0, 0, -4F, 21F, -5F, 8, 1, 10),
			box(0, 0, -4F, 22F, -5F, 8, 2, 10),
			box(0, 17, -4F, 22F, 5F, 8, 2, 1),
			box(0, 13, -4F, 22F, -6F, 8, 2, 1),
			box(37, 13, 4F, 22F, -5F, 1, 2, 10),
			box(37, 0, -5F, 22F, -5F, 1, 2, 10)));

	private static final Model RAW_BACON = new Model("raw_bacon", "raw_bacon", 64, 32, List.of(
			box(13, 21, 2.466667F, 23F, -4F, 3, 1, 2),
			box(13, 21, 2.466667F, 23F, -6F, 3, 1, 2),
			box(0, 18, 2.466667F, 23F, -2F, 3, 1, 3),
			box(0, 18, 2.466667F, 23F, 1F, 3, 1, 3),
			box(0, 18, 2.466667F, 23F, 4F, 3, 1, 3),
			box(0, 23, -1.533333F, 23F, 4F, 3, 1, 3),
			box(0, 23, -1.533333F, 23F, 1F, 3, 1, 3),
			box(13, 25, -1.533333F, 23F, -1F, 3, 1, 2),
			box(0, 23, -1.533333F, 23F, -4F, 3, 1, 3),
			box(0, 23, -1.533333F, 23F, -7F, 3, 1, 3),
			box(13, 29, -5.533333F, 23F, -7F, 3, 1, 2),
			box(0, 28, -5.533333F, 23F, -5F, 3, 1, 3),
			box(0, 28, -5.533333F, 23F, -2F, 3, 1, 3),
			box(24, 27, -5.533333F, 23F, 1F, 3, 1, 4),
			box(0, 28, -5.533333F, 23F, 5F, 3, 1, 3)));

	private static final Model COOKED_BACON = new Model("cooked_bacon", "cooked_bacon", 64, 32, List.of(
			box(13, 21, 2.466667F, 22F, -4F, 3, 1, 2),
			box(13, 21, 2.466667F, 23F, -6F, 3, 1, 2),
			box(0, 18, 2.466667F, 22F, 1F, 3, 1, 3),
			box(0, 18, 2.466667F, 23F, 4F, 3, 1, 3),
			box(0, 18, 2.466667F, 23F, -2F, 3, 1, 3),
			box(0, 23, -1.533333F, 23F, 4F, 3, 1, 3),
			box(0, 23, -1.533333F, 22F, 1F, 3, 1, 3),
			box(13, 25, -1.533333F, 23F, -1F, 3, 1, 2),
			box(0, 23, -1.533333F, 22F, -4F, 3, 1, 3),
			box(0, 23, -1.533333F, 23F, -7F, 3, 1, 3),
			box(13, 29, -5.533333F, 23F, -7F, 3, 1, 2),
			box(0, 28, -5.533333F, 22F, -5F, 3, 1, 3),
			box(0, 28, -5.533333F, 23F, -2F, 3, 1, 3),
			box(24, 27, -5.533333F, 22F, 1F, 3, 1, 4),
			box(0, 28, -5.533333F, 23F, 5F, 3, 1, 3)));

	private static final Model BUTTER_0 = new Model("butter_0", "butter", 32, 32, List.of(
			box(0, 0, -2F, 23F, -3F, 3, 1, 6),
			box(0, 0, -3F, 23F, 3F, 2, 1, 2),
			box(0, 0, -2F, 23F, -4F, 1, 1, 1),
			box(0, 0, 1F, 23F, 3F, 1, 1, 1),
			box(0, 0, -1F, 23F, 3F, 2, 1, 2),
			box(0, 0, 1F, 23F, 0F, 2, 1, 3),
			box(0, 0, 1F, 23F, 0F, 1, 1, 3),
			box(0, 0, 1F, 23F, -4F, 3, 1, 4),
			box(0, 0, -3F, 23F, -3F, 1, 1, 2),
			box(0, 0, -4F, 23F, -1F, 1, 1, 3),
			box(0, 0, -1F, 23F, -5F, 2, 1, 2),
			box(0, 0, -3F, 23F, 0F, 1, 1, 3),
			box(0, 0, -3F, 23F, -1F, 1, 1, 4)));

	private static final Model BUTTER_1 = new Model("butter_1", "butter", 32, 32, List.of(
			box(0, 0, -2F, 22F, -3F, 3, 2, 6),
			box(0, 0, -2F, 23F, 3F, 1, 1, 1),
			box(0, 0, -2F, 23F, -4F, 1, 1, 1),
			box(0, 0, 1F, 23F, 3F, 1, 1, 1),
			box(0, 0, -1F, 23F, 3F, 2, 1, 2),
			box(0, 0, 1F, 23F, 0F, 2, 1, 3),
			box(0, 0, 1F, 23F, 0F, 1, 1, 3),
			box(0, 0, 1F, 23F, -3F, 3, 1, 3),
			box(0, 0, -3F, 22F, -3F, 1, 2, 1),
			box(0, 0, -4F, 23F, -1F, 1, 1, 3),
			box(0, 0, -1F, 23F, -5F, 2, 1, 2),
			box(0, 0, -3F, 23F, 0F, 1, 1, 3),
			box(0, 0, -3F, 23F, -1F, 1, 1, 4)));

	private static final Model BUTTER_2 = new Model("butter_2", "butter", 32, 32, List.of(
			box(0, 0, -2F, 21F, -3F, 3, 3, 6),
			box(0, 0, -2F, 23F, 3F, 1, 1, 1),
			box(0, 0, -2F, 23F, -4F, 1, 1, 1),
			box(0, 0, -3F, 22F, 0F, 1, 1, 2),
			box(0, 0, 1F, 22F, 3F, 1, 2, 1),
			box(0, 0, 2F, 23F, 3F, 1, 1, 1),
			box(0, 0, -1F, 23F, 3F, 2, 1, 2),
			box(0, 0, 1F, 23F, 0F, 1, 1, 3),
			box(0, 0, 2F, 23F, -3F, 2, 1, 2),
			box(0, 0, -3F, 22F, -3F, 1, 2, 1),
			box(0, 0, -3F, 23F, -1F, 1, 1, 4),
			box(0, 0, 1F, 22F, -3F, 1, 2, 3),
			box(0, 0, -1F, 22F, -4F, 2, 2, 1),
			box(0, 0, -3F, 23F, 0F, 1, 1, 3)));

	/** The plate (original ModelPlate): a ring of 1 pixel boxes with a raised bottom, 64x32 texture. */
	static final Model PLATE = new Model("plate", "plate_model", 64, 32, List.of(
			box(44, 11, -3F, 22F, 4F, 6, 1, 3),
			box(16, 10, -5F, 22F, 5F, 2, 1, 1),
			box(24, 13, -7F, 22F, -3F, 3, 1, 6),
			box(44, 16, -3F, 22F, -7F, 6, 1, 3),
			box(16, 21, -6F, 22F, -5F, 1, 1, 2),
			box(16, 13, 3F, 22F, -6F, 2, 1, 1),
			box(16, 17, 5F, 22F, 3F, 1, 1, 2),
			box(24, 5, 4F, 22F, -3F, 3, 1, 6),
			box(9, 17, 5F, 22F, -5F, 1, 1, 2),
			box(9, 10, 3F, 22F, 5F, 2, 1, 1),
			box(9, 21, -6F, 22F, 3F, 1, 1, 2),
			box(9, 13, -5F, 22F, -6F, 2, 1, 1),
			box(0, 13, -5F, 22F, -5F, 2, 1, 2),
			box(0, 9, 3F, 22F, -5F, 2, 1, 2),
			box(0, 21, 3F, 22F, 3F, 2, 1, 2),
			box(0, 17, -5F, 22F, 3F, 2, 1, 2),
			box(0, 25, -3F, 22.5F, -3F, 6, 1, 6),
			box(25, 22, -3F, 22.5F, -4F, 6, 1, 1),
			box(25, 25, -4F, 22.5F, -3F, 1, 1, 6),
			box(40, 25, 3F, 22.5F, -3F, 1, 1, 6),
			box(40, 22, -3F, 22.5F, 3F, 6, 1, 1)));

	public static final List<Model> MODELS = List.of(BREAD_SLICE_BOTTOM, BREAD_SLICE_TOP, RAW_BACON, COOKED_BACON,
			BUTTER_0, BUTTER_1, BUTTER_2);

	/** Where a layer model lives as an item model definition and model: {@code kitchen:layer/<name>}. */
	public static Identifier id(String name) {
		return Kitchen.id("layer/" + name);
	}

	/** The item model definition (assets/kitchen/items/layer/&lt;name&gt;.json) pointing at the model. */
	public static JsonObject itemDefinition(Model model) {
		JsonObject inner = new JsonObject();
		inner.addProperty("type", "minecraft:model");
		inner.addProperty("model", Kitchen.id("item/layer/" + model.name()).toString());
		JsonObject json = new JsonObject();
		json.add("model", inner);
		return json;
	}

	/** A block model (parent block/block for the item transforms) built from the boxes of a Techne model. */
	public static ModelInstance toBlockJson(Model model, String texture) {
		return () -> {
			JsonArray elements = new JsonArray();
			for (Box box : model.boxes()) {
				elements.add(element(box, model));
			}
			JsonObject textures = new JsonObject();
			textures.addProperty("all", texture);
			textures.addProperty("particle", texture);
			JsonObject json = new JsonObject();
			json.addProperty("parent", "minecraft:block/block");
			json.add("textures", textures);
			json.add("elements", elements);
			return json;
		};
	}

	public static ModelInstance toJson(Model model) {
		return () -> {
			JsonArray elements = new JsonArray();
			for (Box box : model.boxes()) {
				elements.add(element(box, model));
			}
			JsonObject textures = new JsonObject();
			textures.addProperty("all", Kitchen.id("item/layer/" + model.texture()).toString());
			textures.addProperty("particle", Kitchen.id("item/layer/" + model.texture()).toString());
			JsonObject json = new JsonObject();
			json.add("textures", textures);
			json.add("elements", elements);
			return json;
		};
	}

	private static JsonObject element(Box b, Model model) {
		// World extents (pixels, middle of the layer at x = z = 8, base at y = 0).
		float x1 = 8 - SCALE * (b.x() + b.w());
		float x2 = 8 - SCALE * b.x();
		float y1 = SCALE * (24 - (b.y() + b.h()));
		float y2 = SCALE * (24 - b.y());
		float z1 = 8 + SCALE * b.z();
		float z2 = 8 + SCALE * (b.z() + b.d());

		float u = b.u();
		float v = b.v();
		float w = b.w();
		float h = b.h();
		float d = b.d();
		JsonObject faces = new JsonObject();
		// Each face maps the Techne unwrap onto the rotated box (derivation in the class comment).
		faces.add("west", face(model, u + d + w, v + d, u + 2 * d + w, v + d + h));
		faces.add("east", face(model, u, v + d, u + d, v + d + h));
		faces.add("up", face(model, u + d + w, v + d, u + d, v));
		faces.add("down", face(model, u + d + 2 * w, v + d, u + d + w, v));
		faces.add("north", face(model, u + d, v + d, u + d + w, v + d + h));
		faces.add("south", face(model, u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h));

		JsonObject element = new JsonObject();
		element.add("from", array(x1, y1, z1));
		element.add("to", array(x2, y2, z2));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject face(Model model, float u1, float v1, float u2, float v2) {
		JsonObject face = new JsonObject();
		float su = 16.0F / model.textureWidth();
		float sv = 16.0F / model.textureHeight();
		face.add("uv", array(u1 * su, v1 * sv, u2 * su, v2 * sv));
		face.addProperty("texture", "#all");
		return face;
	}

	private static JsonArray array(float... values) {
		JsonArray array = new JsonArray();
		for (float value : values) {
			array.add(value);
		}
		return array;
	}

	/** Names of the model files, for the item definition provider. */
	public static List<String> names() {
		return MODELS.stream().map(Model::name).toList();
	}

	private LayerModelData() { }
}
