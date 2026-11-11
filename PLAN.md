# Kitchen Mod Reborn — Port & Expansion Plan (Fabric, Minecraft Java 26.3)

Port of MrSpring's *The Kitchen Mod* (1.7.10, v1.3.23, MIT) to Fabric for 26.3, plus the features the original promised but never shipped. Personal/non-commercial, played with friends.

Companion docs: `kitchen-mod-feature-summary.md` (full inventory of the original). Original source: https://github.com/MrSpring/TheKitchenMod (clone into `reference/original/`, git-ignored, read-only).

---

## 0. Ground rules

- **License:** original is MIT (© 2017 Mr. Spring). Keep their LICENSE text in `LICENSE-ORIGINAL` and credit in `fabric.mod.json` `authors`/`contributors` + README. Reused textures keep that credit.
- **Port logic, not code.** 1.7.10 Forge APIs (TileEntity, IItemRenderer, GameRegistry, raw NBT, Techne models) have no 1:1 equivalents. Read the original file listed per feature, re-implement idiomatically on 26.3.
- **Mojang names.** 26.x is unobfuscated; Fabric uses Mojang's official names. No Yarn.
- **Verify, don't assume.** 26.3 changed rendering, block codecs, fuel/compost (now item components), input (SDL), and made recipes reloadable registries. When an API from memory doesn't compile, check current Fabric docs / Fabric API source / NeoForge 26.3 porting primer before guessing.
- **Data generation for everything static** (models, blockstates, item model definitions, recipes, loot tables, tags, lang). Hand-write only what datagen can't.
- **Mod ID:** `kitchen` (matches original texture paths `assets/kitchen/...`). Display name: "The Kitchen Mod Reborn" (change freely).

## 1. Versions (pinned at M0 — commit 65d8781)

| Thing | Pinned |
|---|---|
| Minecraft | 26.3 |
| Java | 25 |
| Fabric Loader | 0.20.1 |
| Fabric Loom | 1.19-SNAPSHOT (resolved 1.19.4) |
| Gradle | 9.8.2 |
| Fabric API | 0.166.0+26.3 |

Package: `net.kitchenreborn`. Bump versions deliberately (one commit, rebuild + datagen + server + client check), never in passing.

**Gotchas found at M0:** run `build` and `runDatagen` as separate Gradle invocations (they conflict over `src/main/generated`). Vanilla hides empty creative tabs. No in-game mod list without Mod Menu (optional dev-only dep). 26.3 recipe datagen: `RecipeProvider`/`FabricRecipeProvider` take bootstrap contexts (recipes are a registry), `buildRecipes` is public.

## 2. Repo layout

```
kitchen-mod-reborn/
├─ CLAUDE.md                     # working rules for Claude Code
├─ PLAN.md                       # this file
├─ LICENSE  LICENSE-ORIGINAL  README.md
├─ reference/original/           # git-ignored clone of MrSpring/TheKitchenMod
├─ tools/                        # one-off scripts (texture import, model conversion)
└─ src/
   ├─ main/java/<pkg>/kitchen/
   │  ├─ Kitchen.java                    # ModInitializer
   │  ├─ registry/ KitchenBlocks, KitchenItems, KitchenBlockEntities,
   │  │            KitchenComponents, KitchenSounds, KitchenTabs, KitchenRecipes
   │  ├─ block/ board, oven, pan, plate, cabinet, tiles, crop, wild, + new appliances
   │  ├─ blockentity/                    # state machines + persistence
   │  ├─ item/ knife, mortar&pestle, jam jar, sandwich, + new foods
   │  ├─ food/                           # ⭐ the shared "modular food" core (see §4)
   │  ├─ recipe/                         # oven, pan, knife(board), appliance recipe types
   │  └─ world/                          # wild plant placement
   ├─ client/java/<pkg>/kitchen/client/
   │  ├─ KitchenClient.java
   │  ├─ render/ BoardRenderer, PlateRenderer, OvenRenderer, PanRenderer, ...
   │  └─ render/item/ LayeredFoodItemRenderer   # sandwich/pizza/etc. in hand
   ├─ main/generated/                    # datagen output (committed)
   └─ main/resources/
      ├─ fabric.mod.json  kitchen.mixins.json (only if truly needed)
      └─ assets/kitchen/ textures/{item,block,entity}  lang/  models/(hand-made only)
```

## 3. Original → port mapping

| Feature | Original source (in `reference/original/src/main/java/dk/mrspring/kitchen/`) | Port target |
|---|---|---|
| Item/block list | `KitchenItems`, `KitchenBlocks` | `registry/*` |
| Crafting/smelting | `recipe/RecipeRegistry` | datagen recipes |
| Knife recipes | `RecipeRegistry#addKnifeRecipe` | `recipe/CuttingRecipe` (data-driven) |
| Oven | `block/container/BlockOven`, `tileentity/TileEntityOven`, `recipe/OvenRecipes` | `OvenBlock` + `OvenBlockEntity` + `OvenRecipe` |
| Frying pan & jam | `BlockFryingPan`, `pan/*` (Ingredient, Jam, rendering handlers) | `PanBlock` + `PanBlockEntity` + `PanRecipe` |
| Cutting board | `BlockBoard`, `tileentity/TileEntityBoard`, `api/event/BoardEventRegistry` | `BoardBlock` + `BoardBlockEntity` + `food/LayerBehavior` |
| Sandwich item | `item/ItemSandwich`, `config/SandwichableConfig` | `food/*` + data-driven `sandwichable` JSON |
| Sandwich render | `item/render/SandwichRender` (per-layer heights!) | `client/render/item/LayeredFoodItemRenderer` |
| Plate | `BlockPlate`, `ItemBlockPlate`, `PlateRender` | `PlateBlock` + BE + `ItemContainerContents`-style component |
| Jam jar | `item/ItemJamJar`, `model/jam/*` | `JamJarItem` + `kitchen:jam` component |
| Crops / wild | `block/plant/*`, `world/gen/*` | `KitchenCropBlock`, wild bush + placed feature |
| Tooltips | `event/SandwichableTooltipEvent` | `ItemTooltipCallback` |
| Configs | `config/*` | data-driven JSON (datapack-overridable) instead of config files |
| WAILA / Crayfish compat | `comp/*` | **drop**; optional Jade plugin later (M11) |

## 4. Core architecture decisions

### 4.1 Modular food core (`food/`) — build once, reuse for every layered food
The sandwich is one instance of a general idea: *a base + ordered layers assembled on a station, then finished into an item*. Pizza, lasagna, ice cream sundae, pancake stack, and waffle toppings all reuse it.

- `LayeredFoodComponent` — a **custom DataComponentType** (not raw NBT) holding `List<ItemStack> layers` (+ optional per-food extras like `spreadClicks`). Codec + StreamCodec required.
- `LayerDefinition` — **data-driven** JSON registry (reloadable): `data/kitchen/kitchen/layer/<id>.json`
  ```json
  { "item": "kitchen:cheese_slice", "nutrition": 3, "saturation": 0.6,
    "role": "filling",              // "bread" | "filling" | "spread" | "topping" | "base"
    "height": 1.0,                  // in pixels, used by renderer (original: bread 2, model items 1, flat items 0.5)
    "render": "flat",               // "flat" | "model:<id>"  (3D layer model)
    "foods": ["sandwich", "pizza"], // which modular foods accept it
    "returns": "kitchen:jam_jar",   // item given back when applied (jam), optional
    "consume_from_container": true  // jam jar serving decrement, optional
  }
  ```
  Replaces `SandwichableConfig` and the board-event registry. Datapack-overridable so friends can add ingredients without code.
- `ModularFoodType` — registry of food kinds (`sandwich`, `pizza`, `lasagna`, `sundae`, ...): max layers, valid first/last layer rule, station block, output item, cook step (none / oven).
- **Nutrition:** on assembly, compute total nutrition/saturation from layers and set the vanilla `FOOD` (and `CONSUMABLE`) components on the output stack. No custom eat logic needed. Allow totals > 20 (original did).
- **Name/tooltip:** dynamic tooltip listing layers + total heal (port of `SandwichableTooltipEvent`). Optional auto-name ("Bacon Cheese Sandwich") — pick the top 2 fillings.

### 4.2 Stations = blocks + block entities with simple state machines
- Interaction via `useItemOn` / `useWithoutItem` (mirror original: right-click add, empty-hand take, sneak-right-click finish/toggle).
- Persist with `saveAdditional`/`loadAdditional` (check 26.3 signatures — ValueInput/ValueOutput style), sync to client with update packet + `getUpdateTag`.
- Ticking via `BlockEntityTicker` only where needed (oven, pan, new appliances).

### 4.3 Rendering
- **In-world layers:** `BlockEntityRenderer` for board, plate, pan, oven contents. 26.3 reworked rendering — follow the current render-state (extract → submit) pattern from the porting primer / Fabric docs.
- **In hand/inventory:** item model definition of type `minecraft:special` backed by a registered custom special model renderer that draws `LayeredFoodComponent` layers with **true per-ingredient heights** (ported from `SandwichRender`). Fallback icon = original `sandwich.png` if anything fails.
- **3D models:** original Techne Java models (`model/*.java`) → rebuild in Blockbench as JSON (oven, pan, plate, cabinet, jar, bacon, butter, bread top/bottom). Simple shapes; read cuboid coords straight from the Java files. Flat layers = generated 16×16×1 slab rendering the item texture.

### 4.4 Tools that stay in the grid
Knife and mortar & pestle: **do both**
1. Grid recipes keep the tool (override remaining-item / crafting-remainder behaviour; optionally take 1 durability like a real tool — design choice D3).
2. Board slicing: whole ingredient on board + right-click with knife → slices (more fun, matches mod spirit).
Implement cutting as one data-driven `kitchen:cutting` recipe type used by both paths.

### 4.5 Data-driven where it's cheap
Recipes, layer definitions, oven/pan/appliance recipes, crop drops, wild plant placement — all JSON, so balance tweaks need no rebuild.

## 5. Milestones

Each milestone ends with: builds, launches client + dedicated server without errors, `runDatagen` clean, manual test checklist passed, commit. Don't start the next until the current passes.

### M0 — Scaffold (½ day)
- Template project, versions pinned (§1), mod ID `kitchen`, CLAUDE.md, README with credits, LICENSE-ORIGINAL.
- Clone original into `reference/original/`.
- `tools/import_textures.py`: copy `textures/items` → `assets/kitchen/textures/item`, `textures/blocks` → `.../block`, `textures/models` → `.../entity` or `block` as appropriate; drop `Thumbs.db`/`.psd`.
- Creative tab "Kitchen". Empty datagen providers wired.
- ✅ Launches; empty tab visible.

### M1 — Ingredients & simple items (1 day)
- All plain items: slices, raw/cooked bacon, fillets, roast beef, chicken leg, cheese, butter, flour, toast, burnt meat, strawberry/sugared slices, apple slice, peanut(s), lettuce, tomato, strawberry.
- Food stats for directly edible ones (original's are in each item class / ItemBase).
- Knife, mortar, pestle, mortar & pestle, empty jam jar.
- Grid recipes (§ summary) + vanilla furnace/smoker/campfire cooking for bacon, fillets, roast beef, flour→toast.
- Cutting recipe type + grid knife recipes with tool retained.
- ✅ Every original recipe craftable; knife/M&P stay in grid.

### M2 — Crops & world (1 day)
- Lettuce, tomato, strawberry, peanut crops (4 stages; use original textures — map onto age property sensibly, e.g. age 0-3).
- Seeds (original used produce as seeds? check `ItemSeedBase`), loot tables, bonemeal, compostable via `COMPOSTABLE` component.
- Wild plants + placed feature in grassy overworld biomes; rate tunable in JSON.
- Villager farmers can farm them (tag check).
- ✅ Find wild plants in a new world, farm all four.

### M3 — Cutting Board & Sandwich (2–3 days) ⭐ core
- Food core (§4.1) + sandwich food type + layer JSONs for all 17 original sandwichables (heal values in summary doc).
- Board block (model from original), BE storing layers, interactions:
  RC add (≤10) · empty RC remove top · sneak-RC finish if bread top & bottom & ≥2 layers.
- Butter rule: only on bread slice, needs 2 more RCs to spread before stacking (port BoardEventRegistry logic).
- Jam rule: RC with filled jar adds jam layer, jar loses a serving, jar not placed.
- Board slicing with knife (§4.4).
- Sandwich item: component, dynamic FOOD, tooltip, eat.
- Board BER showing layers at true heights.
- ✅ Build a 10-layer sandwich, see it on board, eat it, correct hunger.

### M4 — Sandwich item rendering (1–2 days)
- Special item model renderer for the sandwich (in hand, GUI, item frame, dropped).
- Bread top/bottom 3D models; bacon/butter/jam 3D layer models; flat layers for the rest.
- ✅ Sandwich in hand matches board.

### M5 — Plate (½–1 day)
- Places facing player; RC adds any item (cap at e.g. 32 for FPS — design choice D5); empty RC removes top; sneak-RC picks up plate keeping contents (container component) with tooltip.
- Renders items stacked; sandwiches render fully.
- ✅ Pick up/replace plate with contents intact.

### M6 — Oven (1–2 days)
- Model + open/closed + active texture. Sneak-RC empty toggles open.
- 4 slots × 4 same-type stacks; only items with an oven recipe (fallback: any smoker/furnace food recipe).
- Fuel: coal/charcoal RC (use `COOKING_FUEL` component check rather than hardcoding coal — design choice).
- Timer: cooks at 400 ticks, burns at 600 → burnt meat. Smoke particles when done. Empty RC removes a stack.
- `kitchen:oven` recipe type (defaults: raw roast beef, raw fillet, raw bacon) + auto-include food smelting recipes.
- ✅ Cook 16 items; over-cook into burnt meat.

### M7 — Frying Pan & Jam (1–2 days)
- Pan only works on top of an oven that is *lit/fueled* (original: just "on oven" — design choice D6).
- Fry: raw bacon / raw fillet → cooked at 300 ticks, visual swap.
- Jam: sugared strawberry slices / apple slices / peanuts → jam; 3 fill stages (100/200/300); RC with empty jar fills (6 servings); `kitchen:jam` component on jar (type + servings); jar texture by fill level (original filling_1..6 textures) via item model `range_dispatch`/select.
- Smoke when done; `kitchen:pan` recipe type.
- ✅ Make all three jams, use on a sandwich.

### M8 — Decor & polish (½ day)
- Kitchen cabinet (original model/texture), checker tiles.
- Sounds (sizzle, oven door, knife chop) — vanilla sound events are fine.
- Lang: port `en_US.lang` → `en_us.json`; other 24 languages optional via script.
- ✅ Original feature set complete. **Tag release v1.0.0.**

### M9 — Promised features, wave 1: breakfast (3–5 days)
See §6.1–6.4. Toaster, waffle iron, waffles, pancakes.

### M10 — Promised features, wave 2: dinner & dessert (4–6 days)
See §6.5–6.8. Muffins, pizza, lasagna, ice cream.

### M11 — Optional extras
- Jade tooltip plugin (replaces WAILA compat): oven timer, pan progress, board layer count.
- REI/EMI recipe display for cutting/oven/pan/appliance recipes.
- Advancements ("Dagwood": 10-layer sandwich, "Charcoal Chef": burn food, etc.).
- Modrinth-ready packaging for friends (or a shared Prism/Modrinth App instance export).

## 6. Promised-but-never-shipped features — proposed designs

The original announced these ("waffles, waffle iron, toaster, toasted toast, pancakes, ice cream, muffins, lasagna, pizza") with no implementation, so these designs are new. Each reuses the station/food core. **All numbers are first-pass; tune in JSON.**

New base ingredients needed across wave 1–2: **egg** (vanilla), **milk** (vanilla bucket / new *milk bottle*? — D8), **batter** (bowl: flour + egg + milk + sugar), **dough** (flour + water bottle), **tomato sauce** (pan: tomato slices), **cream** (M&P + milk bucket? or churn — D9), **chocolate** (cocoa beans in pan), **ice**/snowball for ice cream.

### 6.1 Toaster + Toasted Toast
- Block, 2 slots. RC with bread slice (up to 2) → push lever (sneak-RC) → 160 ticks → pops toast up (render slices rising) with ding sound. Leave in too long (another 160) → burnt toast.
- "Toasted toast": the original had *toast* (smelted flour) as a bread type. Proposal: rename smelted product "Toast Loaf"? **Simpler: keep original `toast`, toaster turns `bread_slice` → `toast` too**, plus `toast` → `toasted_toast` (extra-crispy, +1 heal) as the joke the name implies.
- Counts as `role: bread` for sandwiches.

### 6.2 Waffle Iron + Waffles (modular toppings)
- Block placed on any surface; open/close like oven. RC with batter bowl → close → 200 ticks → waffle (returns bowl).
- **Waffle is modular-lite:** finish on a plate/board with up to 4 toppings (butter, jams, strawberry slices, syrup, whipped cream). Uses `ModularFoodType "waffle"` with base = waffle, max 5 layers.
- Overcook → burnt waffle.

### 6.3 Pancakes (stackable)
- Cook batter in frying pan (on oven) → pancake; flip timing mini-game optional (sneak-RC at the right moment = golden pancake bonus) — D10.
- **Pancake stack** = modular food: pancakes + toppings on a plate, up to 8 layers; rendered as a stack.

### 6.4 Batter & breakfast ingredients
- Batter: shapeless bowl + flour + egg + milk bucket(→bucket returned) + sugar.
- Syrup: pan + sugar + water? or sap from oak via knife? — keep simple: shapeless sugar ×2 + water bottle → syrup bottle. D11.

### 6.5 Muffins
- **Muffin tin** item (iron). Fill tin with batter + optional mix-in (strawberry slices, chocolate, apple slices, peanuts) via crafting → raw muffin tray → oven → 6 muffins of that flavor. Muffins use `variant` component for flavor; one item, tinted/selected model per flavor.

### 6.6 Pizza (modular)
- Dough → RC on board with **rolling pin** (new tool, shapeless planks+sticks) → pizza base on board.
- Add tomato sauce (spread rule like butter: base only, 2 clicks), cheese, toppings (bacon, chicken, tomato/carrot/potato slices, lettuce? why not). Max 8 toppings.
- Sneak-RC → raw pizza item (component holds layers) → oven (counts as one oven item, 1 per slot) → cooked pizza.
- **Slicing:** cooked pizza on board + knife RC → 8 pizza slices, each carrying the topping list (nutrition ÷ 8, min 1). Rendered top-down with toppings scattered (deterministic positions from layer index seed).
- Pizza on a plate renders whole.

### 6.7 Lasagna (modular, layered)
- **Baking dish** block (clay/terracotta). Layers alternate: pasta sheet (dough + knife on board) / sauce / meat (roast beef, cooked bacon) / cheese. Must start and end with pasta, max 9.
- Oven-bake the whole dish block? → simplest: sneak-RC dish → raw lasagna dish item → oven slot → cooked lasagna dish block placed back; RC with empty hand → serve a portion (6 portions, each with full layer list, nutrition ÷ 6). Dish returned when empty.

### 6.8 Ice Cream
- **Ice cream maker** block: crank (RC repeatedly, 20 clicks) with cream + sugar + flavor (strawberry slices, chocolate, apple, peanut) + ice/snowball → tub of flavored ice cream (8 scoops).
- **Cone** (wafer: batter in waffle iron with different shape?) or bowl. Scoop onto cone/bowl: modular up to 3 scoops + toppings (jam, chocolate, strawberries). Sundae = bowl version, more layers.
- Melts? (cone in hand > 2 min in hot biome → melted) — fun but optional, D12.

## 7. Design decisions for Heron (defaults in **bold**, change before the relevant milestone)

| # | Decision | Default |
|---|---|---|
| D1 | Sandwich layer heights | **True per-ingredient heights (as original)** |
| D2 | Max sandwich layers | **10 (original)** |
| D3 | Knife/M&P take durability in grid? | **No (original)**, but board slicing exists |
| D4 | Configs as files vs data-driven JSON | **JSON (datapack-overridable)** |
| D5 | Plate item cap | **32** (original unlimited, laggy) |
| D6 | Pan needs a *lit* oven vs any oven | **Any oven (original)** |
| D7 | Oven accepts any furnace food vs own recipes only | **Own recipes + all furnace food recipes** |
| D8 | Milk source for recipes | **Vanilla milk bucket (bucket returned)** |
| D9 | Cream source | **M&P + milk bucket → cream bottle** |
| D10 | Pancake flip mini-game | **Off for v1** |
| D11 | Syrup recipe | **Sugar ×2 + water bottle** |
| D12 | Ice cream melting | **Off** |
| D13 | Auto-generated food names ("Bacon Cheese Sandwich") | **On** |
| D14 | Keep original crop textures (low-res 2014 style) or redraw | **Keep for v1** |

## 8. Testing approach
- Fabric GameTest for logic: sandwich nutrition sum, board finish validation, butter/jam rules, oven timer cook/burn, pan jam fill, pizza slicing division.
- Manual checklist per milestone run in a dev client; dedicated-server launch each milestone (catches client-class leaks into common code).
- Multiplayer sanity before sharing with friends: two clients on a local server, check BE sync (board layers, oven state) appears for both.

## 9. Known risk list
1. **26.3 rendering changes** (BER + special item model renderer) — biggest unknown; budget extra time in M3/M4; consult porting primer.
2. Item-in-hand dynamic rendering API may have moved since 1.21.x — if special model renderer registration is unclear, ship M3 with the static sandwich icon and do M4 separately.
3. Techne → Blockbench model conversion is tedious but low-risk.
4. Data-driven reloadable registries for layer definitions — follow Fabric's `DynamicRegistries.registerReloadable` (new in 26.3) or a plain `SimpleJsonResourceReloadListener` if simpler.
