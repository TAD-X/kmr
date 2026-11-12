# The Kitchen Mod Reborn

A Fabric mod for Minecraft Java 26.3: a port of **MrSpring's _The Kitchen Mod_** (cutting boards, layered sandwiches, ovens, frying pans, jam) plus the features the original announced but never shipped (toaster, waffles, pancakes, muffins, pizza, lasagna, ice cream).

See `PLAN.md` for the roadmap and `kitchen-mod-feature-summary.md` for the original's feature inventory.

## Credits

- **Mr. Spring (MrSpring)** — creator of [The Kitchen Mod](https://github.com/MrSpring/TheKitchenMod) (Minecraft 1.7.10). Textures and original design are reused under the MIT license; see `LICENSE-ORIGINAL`.

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
