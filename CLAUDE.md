# CLAUDE.md — Kitchen Mod Reborn

Fabric mod for Minecraft Java 26.3. Port of MrSpring's Kitchen Mod (1.7.10, MIT) + never-shipped promised features. Full plan: `PLAN.md`. Original feature inventory: `kitchen-mod-feature-summary.md`.

## Rules
- Work milestone by milestone (PLAN.md §5). Don't start the next until the current one builds, launches client AND dedicated server, passes `runDatagen`, and its checklist is done. Commit at each milestone.
- Original source lives in `reference/original/` (read-only, git-ignored). Port the *behaviour*, not the code — 1.7.10 Forge APIs don't exist anymore.
- Minecraft 26.x is unobfuscated: use Mojang names. No Yarn.
- 26.3 changed rendering, block codecs, block-entity save/load, fuel/compost (item components), input (SDL), and made recipes reloadable registries. If an API you remember doesn't compile, look it up (Fabric docs, Fabric API source, NeoForge 26.3 porting primer) instead of guessing repeatedly.
- Mod ID `kitchen`. Assets under `assets/kitchen/`.
- Everything static goes through datagen (models, blockstates, item model definitions, recipes, loot, tags, lang). Commit `src/main/generated/`.
- Gameplay numbers (heal values, timers, layer rules) live in data JSON, not Java constants. **Exception (accepted):** item food stats (hunger/saturation) live in `KitchenItems.java`, because item components are set at registration. Sandwich *layer* heals are still JSON.
- Client-only code stays in `src/client/`. Never reference client classes from `src/main/`.
- Modular foods (sandwich, pizza, lasagna, waffle, pancake stack, sundae) all go through the shared `food/` core — don't special-case a new food in BE code.
- Design decisions D1–D14 in PLAN.md §7: use the bolded default unless the user changed it; ask before deviating.

## Commands
- `./gradlew build` · `./gradlew runClient` · `./gradlew runServer` · `./gradlew runDatagen` · `./gradlew runGameTest` (server game tests; `build` runs them too) · `./gradlew runClientGameTest` (screenshots to build/run/clientGameTest/screenshots)

## Releases and remote
- Remote `origin` = https://github.com/TAD-X/kmr.git (branch `main`). Push after every milestone commit.
- Version scheme `0.<milestone>.<patch>` until M8 = `1.0.0`; set `version=` in `gradle.properties`. After every milestone (and after patch fixes users should get): bump the version, `./gradlew build`, commit, tag `vX.Y.Z`, `git push origin main --tags`.
- GitHub Release for the tag with the remapped jar `build/libs/kitchen-X.Y.Z.jar` attached (never the `-sources` jar). Notes live in `docs/release-notes/vX.Y.Z.md`: what's new for players, the exact Minecraft / Fabric Loader / Fabric API versions required (from `gradle.properties`), and "both server and all clients must use the same jar". With the GitHub CLI (on Windows `C:Program FilesGitHub CLIgh.exe`, may not be on PATH): `gh release create vX.Y.Z build/libs/kitchen-X.Y.Z.jar --title "vX.Y.Z" --notes-file docs/release-notes/vX.Y.Z.md`.
- Never track `reference/original/` or `run/` (both git-ignored; check `git ls-files`).

## Credits
Keep `LICENSE-ORIGINAL` (MIT, © 2017 Mr. Spring) and credit MrSpring in README and fabric.mod.json for reused textures/design.
