#!/usr/bin/env python3
"""
VALKYRIE ARMOUR v2 + drill lance AB, IN-GAME ASSETS (docs/vfx_remake/valkyrie_ingame.md).

Takes the finished preview data of docs/previews/vfx_remake/valkyrie/ (meshes written by tools/gen_valkyrie_armor_v2.py and tools/gen_valkyrie_lance.py, the 11 procedural 64 x 64 textures) and writes what the
game loads:

    src/main/resources/assets/nusmp/valkyrie_meshes/valk2_<piece>[_mid|_lo].txt   the armour pieces, three levels of detail each (verbatim copies; the loader bakes them into player-part space)
    src/main/resources/assets/nusmp/valkyrie_meshes/lance_AB[_mid|_lo].txt        the merged drill lance, three levels (weapon frame: +y axis, butt at 0)
    src/main/resources/assets/nusmp/valkyrie_meshes/pieces.json                  the piece table of the preview (bones, copies, looks, textures per layer, quads) + the AB lance row
    src/main/resources/assets/nusmp/textures/entity/mode/valk2_atlas.png          256 x 256: ten 64 x 64 sheets and a white cell (the flat plates sample it), cell i at column i % 4, row i // 4
    src/main/resources/assets/nusmp/textures/entity/mode/valk2_scales.png         the tail's scale sheet (it repeats, so it cannot live in the atlas)

    python tools/gen_valkyrie_armor_v2.py ; python tools/gen_valkyrie_lance.py ; python tools/gen_valkyrie_ingame_assets.py

The cell order is ATLAS_ORDER below and `ModeArmorValkyrieRules.Tex` in the game; ModeArmorValkyrieAssetTest pins the two together. Deterministic: running it twice writes the same bytes.
"""
import json
import os
import shutil

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
PREVIEW = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MESH_OUT = os.path.join(ASSETS, "valkyrie_meshes")
TEX_OUT = os.path.join(ASSETS, "textures", "entity", "mode")

CELL = 64
COLS = 4
SIZE = 256
# cell index = position in this list (column = i % 4, row = i // 4); "white" is the flat colour cell of the plates
ATLAS_ORDER = ["book", "feather", "fin", "flame", "foam", "glow", "halo", "petal", "veil", "water", "white"]


def main():
    os.makedirs(MESH_OUT, exist_ok=True)
    os.makedirs(TEX_OUT, exist_ok=True)
    meshes = os.path.join(PREVIEW, "meshes")
    for old in os.listdir(MESH_OUT):
        if old.endswith(".txt") or old == "pieces.json":
            os.remove(os.path.join(MESH_OUT, old))
    n = 0
    for f in sorted(os.listdir(meshes)):
        if f.startswith("valk2_") or f.startswith("lance_AB"):
            if f.endswith(".txt"):
                shutil.copyfile(os.path.join(meshes, f), os.path.join(MESH_OUT, f))
                n += 1
    with open(os.path.join(PREVIEW, "pieces_v2.json"), encoding="utf-8") as fh:
        pieces = json.load(fh)
    with open(os.path.join(PREVIEW, "lances.json"), encoding="utf-8") as fh:
        lances = json.load(fh)
    out = {"units": pieces["units"], "bones": pieces["bones"], "pieces": pieces["pieces"], "lance_AB": lances["lances"]["AB"], "atlas": ATLAS_ORDER, "atlas_size": SIZE, "cell": CELL}
    with open(os.path.join(MESH_OUT, "pieces.json"), "w", encoding="utf-8", newline="\n") as fh:
        json.dump(out, fh, indent=1, sort_keys=False)
        fh.write("\n")

    atlas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for i, name in enumerate(ATLAS_ORDER):
        if name == "white":
            cell = Image.new("RGBA", (CELL, CELL), (255, 255, 255, 255))
        else:
            cell = Image.open(os.path.join(PREVIEW, "tex", "valk2_%s.png" % name)).convert("RGBA")
            assert cell.size == (CELL, CELL), name
        px = cell.load()                                                           # black under every transparent texel (docs/vfx_square_bug.md): the atlas is also drawn additively
        for y in range(CELL):
            for x in range(CELL):
                if px[x, y][3] < 4:
                    px[x, y] = (0, 0, 0, px[x, y][3])
        atlas.paste(cell, ((i % COLS) * CELL, (i // COLS) * CELL))
    atlas.save(os.path.join(TEX_OUT, "valk2_atlas.png"), optimize=True)
    shutil.copyfile(os.path.join(PREVIEW, "tex", "valk2_scales.png"), os.path.join(TEX_OUT, "valk2_scales.png"))
    print("wrote %d mesh files, pieces.json, valk2_atlas.png (%d cells), valk2_scales.png" % (n, len(ATLAS_ORDER)))


if __name__ == "__main__":
    main()
