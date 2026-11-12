package net.kitchenreborn.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;

import net.kitchenreborn.Kitchen;
import net.kitchenreborn.food.LayeredFood;
import net.kitchenreborn.registry.KitchenComponents;
import net.kitchenreborn.registry.KitchenItems;

/**
 * Turns a plate's contents into a {@link LayerStackDrawer.Drawn}. Plain items lie flat and stack half a pixel
 * apart (the original's PlateRender); a sandwich is drawn layer by layer, like on the board. Shared by the placed
 * plate and the plate item, so both look the same.
 */
public final class PlateDrawer {
	/** Where the contents start, in pixels: the top of the plate's raised middle. */
	public static final float PLATE_TOP = 1.2F;
	/** Thickness of one plain item: the original's ITEM_PIXEL, half a pixel. */
	private static final float ITEM_HEIGHT = 0.5F;
	/** An empty plate lying on the plate adds this much height, and sits this deep in the plate below (pixels). */
	private static final float NESTED_PLATE_HEIGHT = 1.0F;
	private static final float NESTED_PLATE_SINK = 0.4F;
	/** Item definition (kitchen:plate_body) that renders just the plate model, drawn as the bottom layer of the item. */
	private static final net.minecraft.resources.Identifier BODY_MODEL = Kitchen.id("plate_body");

	/**
	 * @param withBody also draw the plate itself as the first layer (the item); the placed plate is a block model
	 *                 and passes false, its contents then start at 0 and are lifted by {@link #PLATE_TOP}
	 */
	public static LayerStackDrawer.Drawn prepare(ItemModelResolver itemModels, Level level,
			List<ItemStackTemplate> items, int seed, boolean withBody) {
		List<LayerStackDrawer.Draw> draws = new ArrayList<>();
		float bottom = 0.0F;
		if (withBody) {
			ItemStack body = new ItemStack(KitchenItems.PLATE);
			body.set(DataComponents.ITEM_MODEL, BODY_MODEL);
			ItemStackRenderState render = new ItemStackRenderState();
			itemModels.updateForTopItem(render, body, ItemDisplayContext.NONE, level, null, seed);
			draws.add(new LayerStackDrawer.Draw(render, PLATE_TOP, 0.0F, true));
			bottom = PLATE_TOP;
		}
		float start = bottom;
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i).create();
			if (stack.is(KitchenItems.PLATE)) {
				// An empty plate on the plate: the plate model itself, nested into the one below.
				ItemStack body = new ItemStack(KitchenItems.PLATE);
				body.set(DataComponents.ITEM_MODEL, BODY_MODEL);
				ItemStackRenderState render = new ItemStackRenderState();
				itemModels.updateForTopItem(render, body, ItemDisplayContext.NONE, level, null, seed + i);
				draws.add(new LayerStackDrawer.Draw(render, NESTED_PLATE_HEIGHT, bottom - NESTED_PLATE_SINK, true));
				bottom += NESTED_PLATE_HEIGHT;
				continue;
			}
			LayeredFood food = stack.get(KitchenComponents.LAYERED_FOOD);
			if (food != null) {
				LayerStackDrawer.Drawn layers = LayerStackDrawer.prepare(itemModels, level, food.layers(), 0, seed + i * 64);
				for (LayerStackDrawer.Draw draw : layers.layers()) {
					draws.add(new LayerStackDrawer.Draw(draw.item(), draw.height(), draw.bottom() + bottom, draw.isModel()));
				}
				bottom += layers.totalHeight();
			} else {
				ItemStackRenderState render = new ItemStackRenderState();
				itemModels.updateForTopItem(render, stack, ItemDisplayContext.NONE, level, null, seed + i);
				draws.add(new LayerStackDrawer.Draw(render, ITEM_HEIGHT, bottom, false));
				bottom += ITEM_HEIGHT;
			}
		}
		return new LayerStackDrawer.Drawn(draws, bottom);
	}

	private PlateDrawer() { }
}
