package net.kitchenreborn.client.datagen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.client.render.LayerModels;

/** Writes the converted 3D layer models and their item model definitions (see {@link LayerModelData}). */
public class KitchenLayerModelProvider implements DataProvider {
	private final PackOutput.PathProvider models;
	private final PackOutput.PathProvider itemDefinitions;

	public KitchenLayerModelProvider(FabricPackOutput output) {
		this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
		this.itemDefinitions = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		if (!LayerModels.KNOWN.equals(new HashSet<>(LayerModelData.names()))) {
			throw new IllegalStateException("LayerModels.KNOWN does not match the generated layer models: "
					+ LayerModelData.names());
		}
		List<CompletableFuture<?>> writes = new ArrayList<>();
		// Base model of the sandwich special renderer: only supplies the display transforms (hand, GUI, frame, ground).
		JsonObject base = new JsonObject();
		base.addProperty("parent", "minecraft:block/block");
		base.addProperty("gui_light", "side");
		base.add("textures", particle());
		// Outside the GUI a sandwich is shown at exactly its cutting board size: every display scale is 1. First person
		// and the item frame tilt the top towards the player so the layers can be seen.
		JsonObject display = new JsonObject();
		display.add("thirdperson_righthand", transform(new float[] {75, 45, 0}, new float[] {0, 10.5F, 0}, 1.0F));
		display.add("firstperson_righthand", transform(new float[] {-50, 45, 0}, new float[] {3, 5.0F, -8}, 1.0F));
		display.add("firstperson_lefthand", transform(new float[] {-50, 225, 0}, new float[] {-3, 5.0F, -8}, 1.0F));
		display.add("ground", transform(new float[] {0, 0, 0}, new float[] {0, 0, 0}, 1.0F));
		display.add("fixed", transform(new float[] {-40, 0, 0}, new float[] {0, 6, 0}, 1.0F));
		display.add("on_shelf", transform(new float[] {0, 180, 0}, new float[] {0, 0, 0}, 1.0F));
		base.add("display", display);
		writes.add(DataProvider.saveStable(cache, base, models.json(Kitchen.id("item/sandwich_base"))));
		// The GUI keeps the plain block transforms (rotated, 0.625); the renderer fits the stack into the slot.
		JsonObject gui = new JsonObject();
		gui.addProperty("parent", "minecraft:block/block");
		gui.addProperty("gui_light", "side");
		gui.add("textures", particle());
		writes.add(DataProvider.saveStable(cache, gui, models.json(Kitchen.id("item/sandwich_base_gui"))));
		for (LayerModelData.Model model : LayerModelData.MODELS) {
			writes.add(DataProvider.saveStable(cache, LayerModelData.toJson(model).get(),
					models.json(Kitchen.id("item/layer/" + model.name()))));
			writes.add(DataProvider.saveStable(cache, LayerModelData.itemDefinition(model),
					itemDefinitions.json(LayerModelData.id(model.name()))));
		}
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	/** Eating particles use the item model's particle texture; the special renderer has none of its own. */
	private static JsonObject particle() {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", "kitchen:item/bread_slice");
		return textures;
	}

	private static JsonObject transform(float[] rotation, float[] translation, float scale) {
		JsonObject json = new JsonObject();
		json.add("rotation", array(rotation));
		json.add("translation", array(translation));
		json.add("scale", array(new float[] {scale, scale, scale}));
		return json;
	}

	private static JsonArray array(float[] values) {
		JsonArray array = new JsonArray();
		for (float value : values) {
			array.add(value);
		}
		return array;
	}

	@Override
	public String getName() {
		return "Kitchen Layer Models";
	}
}
