#!/usr/bin/env python3
"""Copy the original Kitchen Mod textures into assets/kitchen/textures.

items/  -> item/
blocks/ -> block/
models/ -> entity/   (Techne model skins; the five 3D layer models go to item/layer/)

Skips Thumbs.db and .psd files. Names are lower-cased; item textures are renamed to this port's item ids (RENAMES).
Usage: python tools/import_textures.py
"""
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "reference/original/src/main/resources/assets/kitchen/textures"
DST = ROOT / "src/main/resources/assets/kitchen/textures"
MAP = {"items": "item", "blocks": "block", "models": "entity"}
# Textures of the 3D layer models: item models can only use the item atlas, so these live in item/layer/.
LAYER_MODEL_TEXTURES = {"bread_slice_bottom", "bread_slice_top", "butter", "cooked_bacon", "raw_bacon", "oven", "oven_active"}
# Textures of block models built from Techne boxes (block atlas): original models/<stem>.png -> block/<name>.png.
BLOCK_MODEL_TEXTURES = {"plate": "plate_model", "oven": "oven_model", "oven_active": "oven_model_active"}
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
            elif src_dir == "blocks":
                # datagen's crop models expect <block>_stage<N> (no underscore before N)
                stem = re.sub(r"_stage_([0-9])$", lambda m: "_stage" + m.group(1), stem)
            target = out
            if src_dir == "models" and stem in LAYER_MODEL_TEXTURES:
                target = DST / "item" / "layer"
                target.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, target / (stem + f.suffix.lower()))
            if src_dir == "models" and stem in BLOCK_MODEL_TEXTURES:
                shutil.copy2(f, DST / "block" / (BLOCK_MODEL_TEXTURES[stem] + f.suffix.lower()))
            copied += 1
    print(f"copied {copied}, skipped {skipped} -> {DST}")


if __name__ == "__main__":
    main()
