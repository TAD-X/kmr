#!/usr/bin/env python3
"""Copy the original Kitchen Mod textures into assets/kitchen/textures.

items/  -> item/
blocks/ -> block/
models/ -> entity/   (Techne model skins; used by the Blockbench rebuilds)

Skips Thumbs.db and .psd files. Names are lower-cased; item textures are renamed to this port's item ids (RENAMES).
Usage: python tools/import_textures.py
"""
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "reference/original/src/main/resources/assets/kitchen/textures"
DST = ROOT / "src/main/resources/assets/kitchen/textures"
MAP = {"items": "item", "blocks": "block", "models": "entity"}
SKIP_NAMES = {"thumbs.db"}
SKIP_EXT = {".psd"}
# 1.7.10 texture names -> item ids used by this port (item textures are named after the item id).
RENAMES = {
    "bacon_raw": "raw_bacon",
    "bacon_cooked": "bacon",
    "chicken_fillet_raw": "raw_chicken_fillet",
    "chicken_fillet_cooked": "chicken_fillet",
    "beef_slice": "raw_roast_beef",
    "peanuts": "peanuts_in_shell",
    "strawberry_slices_sugared": "sugared_strawberry_slices",
}


def main():
    if not SRC.is_dir():
        raise SystemExit(f"Original textures not found: {SRC} (clone the original into reference/original first)")
    copied = skipped = 0
    for src_dir, dst_dir in MAP.items():
        out = DST / dst_dir
        out.mkdir(parents=True, exist_ok=True)
        for f in sorted((SRC / src_dir).iterdir()):
            if not f.is_file() or f.name.lower() in SKIP_NAMES or f.suffix.lower() in SKIP_EXT:
                skipped += 1
                continue
            stem = f.stem.lower()
            if src_dir == "items":
                stem = RENAMES.get(stem, stem)
            shutil.copy2(f, out / (stem + f.suffix.lower()))
            copied += 1
    print(f"copied {copied}, skipped {skipped} -> {DST}")


if __name__ == "__main__":
    main()
