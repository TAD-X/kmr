# The Kitchen Mod Reborn

A Fabric mod for Minecraft Java 26.3: a port of **MrSpring's _The Kitchen Mod_** (cutting boards, layered sandwiches, ovens, frying pans, jam) plus the features the original announced but never shipped (toaster, waffles, pancakes, muffins, pizza, lasagna, ice cream).

See `PLAN.md` for the roadmap and `kitchen-mod-feature-summary.md` for the original's feature inventory.

## Credits

- **Mr. Spring (MrSpring)** — creator of [The Kitchen Mod](https://github.com/MrSpring/TheKitchenMod) (Minecraft 1.7.10). Textures and original design are reused under the MIT license; see `LICENSE-ORIGINAL`.

## What's in it

- **Cutting board:** build layered sandwiches (up to 32 layers), spread butter and jam, cut meat and vegetables with a knife.
- **Sandwiches** shown in 3D everywhere: on the board, in your hand, dropped, on a plate.
- **Plates** that hold what you stack on them and keep it when picked up.
- **Oven** with an animated door: cooks roast beef, chicken fillets, bacon and anything a furnace or smoker cooks into food.
- **Frying pan** that sits on an oven: fries bacon and chicken, boils jam.
- **Jam jars** with fill levels, in three jams (strawberry, apple, peanut butter).
- **Kitchen cabinet** (27 slots) and **checker tiles** with slabs and stairs.
- **Crops:** tomatoes, lettuce, strawberries, peanuts, plus wild versions in the world.

See [docs/FEATURES.md](docs/FEATURES.md) for exactly how everything behaves and how it differs from the original.

### How to make a sandwich

1. Craft a **cutting board** (slab, wooden pressure plate, slab) and place it down.
2. Get **bread slices** (craft bread, cut it with a **knife** on the board or in the crafting grid) and some fillings: bacon, cheese, tomato, lettuce, roast beef...
3. Right-click the board with a bread slice, then your fillings, then a second bread slice on top. To add butter, right-click it on the bread and click twice with the knife to spread it.
4. **Sneak + right-click** the board to finish the sandwich. Hold or eat it, or put it on a **plate**.

### How to use the oven

1. Craft an **oven** (iron, coal, flint & steel) and place it facing you.
2. **Sneak + empty hand** opens the hatch. Right-click with raw bacon, raw chicken fillet, raw beef slices (up to four of a kind per slot, four slots) and with one fuel item such as coal.
3. **Sneak + empty hand** again closes it. After about 20 seconds the food is cooked (smoke appears). Take it out soon: after 30 seconds it burns to Burnt Meat.
4. Open the hatch and click with an empty hand to take stacks out. Opening the oven early resets the cooking and uses up the fuel.

### How to make jam

1. Craft a **frying pan** (five iron) and put it on top of an oven.
2. Right-click the pan with **sugared strawberry slices** (strawberry slices plus sugar), **apple slices** or **peanuts**. It fills in over about 15 seconds.
3. Right-click the finished jam with an **empty jam jar** (iron, glass). You get a full jar with six servings.
4. Right-click a cutting board that has a bread slice on it with the jar to add a jam layer. The jar stays in your hand and loses a serving.

## Playing with friends

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) 0.20.1 or newer for Minecraft 26.3.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) 0.166.0+26.3 and put it in your `mods` folder.
3. Download `kitchen-<version>.jar` from the [Releases](https://github.com/TAD-X/kmr/releases) page and put it in the same `mods` folder.

The server and every client must use the **same Kitchen jar version** (and the same Minecraft and Fabric API versions), otherwise joining fails or foods look wrong. Servers need only the jars in their own `mods` folder; clients never need anything the server does not have.

## Development

```
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runDatagen
python tools/import_textures.py   # re-import original textures (needs reference/original/)
```

Requires JDK 25. `reference/original/` is a git-ignored clone of the original repo (read-only reference).
