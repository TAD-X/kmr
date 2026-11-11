# The Kitchen Mod Reborn

A Fabric mod for Minecraft Java 26.3: a port of **MrSpring's _The Kitchen Mod_** (cutting boards, layered sandwiches, ovens, frying pans, jam) plus the features the original announced but never shipped (toaster, waffles, pancakes, muffins, pizza, lasagna, ice cream).

See `PLAN.md` for the roadmap and `kitchen-mod-feature-summary.md` for the original's feature inventory.

## Credits

- **Mr. Spring (MrSpring)** — creator of [The Kitchen Mod](https://github.com/MrSpring/TheKitchenMod) (Minecraft 1.7.10). Textures and original design are reused under the MIT license; see `LICENSE-ORIGINAL`.

## Development

```
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runDatagen
python tools/import_textures.py   # re-import original textures (needs reference/original/)
```

Requires JDK 25. `reference/original/` is a git-ignored clone of the original repo (read-only reference).
