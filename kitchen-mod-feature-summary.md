# The Kitchen Mod (MrSpring) — Feature Summary

**Source of truth:** github.com/MrSpring/TheKitchenMod (master, last release v1.3.23, Dec 2016, MC 1.7.10, Forge). MIT license (© 2017 Mr. Spring) — code and textures can be reused for a port if the license notice is kept.
**Newer version?** None. CurseForge lists it as *Abandoned*. Planned features (waffles, waffle iron, toaster, pancakes, ice cream, muffins, lasagna, pizza) were never shipped. Spiritual successors exist for older Fabric (Sandwichable, ≤1.20.1; Culinary Construct) but none replicate this mod's in-world cutting-board stacking.

## Blocks
| Block | Recipe | Behavior |
|---|---|---|
| Cutting Board | `S P S` (wood slab, wood pressure plate, wood slab) | Right-click to add a layer (max 10). Right-click empty hand removes top layer. Shift-right-click with valid stack (bread/toast top and bottom, ≥2 layers) pops out a **Sandwich** item. Layers render stacked in-world. |
| Oven | `III / ICI / IFI` (iron, coal, flint&steel) | Shift-RC empty hand toggles open/closed. While open, RC with food adds it: 4 slots × 4 of one type = 16 items. RC with coal = fuel. Close → cooks. Timer: 400 ticks (20 s) → cooked; 600 ticks (30 s) → everything becomes **Burnt Meat**. Smoke particles when done. RC empty hand takes a stack out. Uses vanilla smelting results + custom oven recipes (raw roast beef/chicken fillet/bacon). `oven_active` texture. |
| Frying Pan | `III / II_` (5 iron) | Must sit on top of an Oven. RC with raw bacon / raw chicken fillet → fries; visual swaps at cookTime ≥300. RC empty hand to take off. Jam mode: sugared strawberry slices → strawberry jam, apple slices → apple jam, peanuts → peanut butter ("peanut jam"); jam model fills in 3 stages (100/200/300). RC with Empty Jam Jar to collect (jar holds up to 6 servings, textures jam_jar_filling_1..6). |
| Plate | `C B C / _ C _` (clay, bone meal) | Faces placer. RC adds any item (unlimited stack, rendered). RC empty takes top item. Shift-RC picks up plate with contents preserved in NBT, tooltip lists contents. Sandwiches render fully on it. |
| Kitchen Cabinet | `QQQ / PPP / PPP` (quartz, planks) | Decorative. |
| Tiles | black dye, bone meal, 4 clay → 2 | Decorative black/white checker tile. |
| Crops ×4 | Lettuce, Tomato, Strawberry, Peanut (4 growth stages each) | Seeds/items replant. |
| Wild plants ×4 | wild_lettuce/tomato/peanut/strawberry | Worldgen: Overworld, on grass, non-flat worlds, % chance per chunk (config `lettuce_spawn_rate`). Source of starter crops. |

## Tools
- **Knife** — iron + stick (diagonal; shape configurable). Used *in the crafting grid* as a non-consumed tool ("knife recipes").
- **Mortar & Pestle** — Mortar (`S_S / _S_` stone) + Pestle (diagonal 2 stone) shapeless → M&P. Non-consumed crafting tool.

## Knife recipes (knife + input → output)
porkchop→3 raw bacon · tomato→4 tomato slice · lettuce→2 lettuce leaf · carrot→2 carrot slice · potato→3 potato slice · bread→2 bread slice · raw beef→2 raw roast beef · raw chicken→3 raw chicken fillet · cooked chicken→2 chicken leg · cheese→2 cheese slice · strawberry→2 strawberry slices · strawberry+sugar→2 sugared strawberry slices · apple→2 apple slices

## Other recipes
- M&P + wheat → flour; M&P + bread → 3 flour; M&P + milk bucket → butter; M&P + peanuts-in-shell → 2 peanuts
- Milk bucket → 2 cheese (shapeless)
- Strawberry slices + sugar → sugared strawberry slices
- Jam Jar: `_I_ / G_G / GGG` (iron, glass)
- Smelting: raw bacon→bacon, flour→2 toast, raw chicken fillet→cooked, raw roast beef→roast beef

## Sandwich system
Sandwichable items and hunger value per layer (sandwich heals the **sum**):
| Item | Heal | | Item | Heal |
|---|---|---|---|---|
| bread slice (bread) | 0 | | raw roast beef | 1 |
| toast (bread) | 0 | | roast beef | 4 |
| raw bacon | 1 | | raw chicken fillet | 1 |
| cooked bacon | 2 | | cooked chicken fillet | 4 |
| tomato slice | 1 | | cheese slice | 3 |
| lettuce leaf | 2 | | butter | 1 |
| potato slice | 2 | | strawberry/apple/peanut jam | 2 each |
| carrot slice | 2 | | | |

Rules: ≥2 layers, bread/toast top and bottom, max 10 layers. Sandwich item stores `SandwichLayers` NBT and renders every layer in hand/on plate; tooltip lists layers + heal. Special board events: **Butter** only goes on a bread slice and must be RC'd twice (spreading) before anything stacks on top. **Jam** is applied from the jar (jar spends one serving, isn't placed itself). Config allows adding any item as sandwichable with custom heal. No hidden "combo" bonuses exist in the source.

## Items (45 textures)
apple slice, apple/strawberry/peanut jam, raw & cooked bacon, beef slice, bread slice, burnt meat, butter, carrot slice, cheese, cheese slice, raw & cooked chicken fillet, chicken leg, flour, jam jar (+6 fill states), knife, lettuce, lettuce leaf, mortar, pestle, mortar & pestle, peanut, peanuts (in shell), potato slice, roast beef, sandwich, strawberry, strawberry slices, sugared strawberry slices, toast, tomato, tomato slice.
Custom 3D models (Techne Java models): bread slice top/bottom, butter, bacon raw/cooked, jam, jar, oven, pan, plate, cabinet.

## Config
Knife recipe shape, sandwichable list + heal amounts, custom oven recipes, wild-plant spawn rate, per-block render toggles. Integrations: WAILA tooltips, MrCrayfish Furniture recipes. 25 lang files.

## Port-relevant notes
- Hardest parts to replicate: in-world layered rendering (board/plate/sandwich item), the Oven/Pan block-entity state machines, and NBT-driven dynamic sandwich nutrition.
- Non-consumed crafting tools (knife, M&P) map cleanly to vanilla `remainder`-style mechanics.
- 1.7.10 Techne models must be rebuilt as JSON block/item models; 16×16 item textures are reusable as-is.
