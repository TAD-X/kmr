# The Kitchen Mod Reborn: what the port does

This describes how the mod actually behaves in 1.0.0, and every place it differs from MrSpring's original
(Minecraft 1.7.10). Gameplay numbers live in datapack JSON under `data/kitchen/kitchen/`.

## Blocks and items

| Thing | How it works |
| --- | --- |
| **Cutting Board** | Slab, wooden pressure plate, slab. Right-click with a food layer to stack it; empty hand takes the top layer; sneak + right-click finishes a sandwich. A knife spreads butter (two clicks per butter layer) and cuts whole items on the board into slices. Up to 32 layers. |
| **Sandwich** | Bread on the top and bottom, at least two layers. Hunger is the sum of the layers, so it can restore more than a normal food; it is always edible. Named after its fillings ("Bacon Cheese Sandwich"). Shown in 3D with every layer in hand, dropped, in frames, on plates, and fitted to the slot in the GUI. |
| **Plate** | Clay and bone meal. Holds up to 32 items, one at a time, drawn stacked; a sandwich and unstackable items only go on an empty plate. Sneak + right-click picks the plate up with its contents. Loaded plates are unstackable. |
| **Oven** | Iron, coal, flint & steel. Sneak + empty hand opens and closes the animated hatch. Open: 4 slots of up to 4 of one food, plus one fuel item. Closed with fuel: cooks after 20 s, burns to Burnt Meat after 30 s. |
| **Frying Pan** | Five iron. Sits on top of any oven. Fries raw bacon and raw chicken fillet in 15 s; boils sugared strawberry slices, apple slices and peanuts into jam, which an empty jar collects (6 servings). |
| **Jam jar** | Shows how full it is and is tinted by the jam. Right-click a cutting board with bread to add a jam layer and use a serving. |
| **Kitchen Cabinet** | Quartz over planks. A real 27-slot container with animated doors and comparator output. Placed in eight steps like the original; the diagonal steps give the solid-front corner look. |
| **Tiles** | The original checkerboard (black dye, bone meal, four clay balls make two), plus slabs and stairs. |
| **Crops and foods** | Tomato, lettuce, strawberry and peanut crops (wild versions in the world), knife, mortar and pestle, flour, butter, cheese, bacon, roast beef, chicken fillet, carrot, potato and apple slices, toast, burnt meat. |

## Deviations from the original

These are intentional changes. Everything not listed behaves like the original.

- **Maximum sandwich layers is 32**, up from 10 (design decision D2).
- **Cabinet is a real container.** The original cabinet was decoration only. Tiles have slab and stair variants (new).
- **Plates are capped at 32 items** (the original was unlimited and laggy). New rules: unstackable items and sandwiches only on an empty plate; shulker boxes, filled bundles and loaded plates never go on a plate; plates holding items are unstackable. A refused item makes the plate act full.
- **Plate pickup keeps contents in an item component** and breaking it by hand does the same, like a shulker box. Explosions drop the plate and its contents separately.
- **Oven fuel is anything with the cooking-fuel component** (coal, charcoal, planks, lava buckets...), not only coal. One item still runs one cycle, and containers give back their empty form.
- **Oven recipes** are the original three plus every smoker recipe and every food furnace recipe (design D7). The timings are datapack JSON (400 and 600 ticks).
- **Opening the oven mid-cook** resets the timer and uses up the fuel, as the original did. Cooked food stays cooked.
- **The frying pan works on any oven**, lit or not (design D6, as in the original). It never burns food, as in the original. New: an empty hand takes the raw ingredient back before it is done (the original locked it in). It pops off when the oven under it is removed.
- **Jam jars** show fill level and colour from data (`kitchen:jam` item component). The original used item damage values.
- **Item food values** live in code (`KitchenItems.java`); the sandwich layer values are datapack JSON.
- **Bread top slice** is 2.25 pixels tall in the model but 1.5 in the layer data, butter spreads use a 0.75 px model against 0.6 px in data, and toast is a flat layer (the original had no model for it). Flat layer sprites are 0.5 block wide.
- **The layer registry loads with the world** and is not changed by `/reload`.
- **Villagers** cannot replant peanuts from shells; an extra mature-peanut drop covers it.
- **Item frames** show a sandwich smaller than board size so tall ones fit.
- **Sounds** are vanilla sound events (chopping, the oven door, sizzling, filling a jar). Hoppers do not feed the oven or the pan.
- **Not ported:** the original's config files, Waila support, MrCrayfish Furniture integration, "combo" names such as "The Good ol' BLT" (sandwiches are named from their fillings instead) and the butter-knife item (the knife does both jobs).
- **Languages:** English is complete. The other languages come from the original's translations for the keys that still exist; anything missing falls back to English.
