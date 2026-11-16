"""Why: every Labyrinth Guardian without an abyssal look wore the same violet-and-gold Floor Warden suit. Dungeon-boss rework 2 gives each of the nine sector traits its own COLOURWAY of that suit:
the violet of the skin, the extras model's texture and both glow maps is turned to the trait's hue and the gold trim to the trait's trim colour. Same meshes, same animations, new paint (NOT new models:
no Blender was available). Writes textures/entity/playermodel/guardian_<trait>(.png, _glow.png), textures/entity/pmx_guardian_<trait>(.png, _glow.png) and the rows guardian_<trait> of
playermodel/looks_dungeon.json (copies of the floor_warden row with the new skin and extras texture):

    python3 -B tools/dungeon_bosses/gen_guardian_colourways.py

Needs numpy and Pillow. Reproducible (no randomness).
"""
import colorsys
import copy
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
TOOLS = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
sys.path.insert(0, TOOLS)
sys.path.insert(0, os.path.join(TOOLS, "dungeon_skins"))
import gen_dungeon_skins as G  # noqa: E402

ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "entity")
SKINS = os.path.join(TEX, "playermodel")

# trait -> (body hue degrees, saturation factor, value factor, trim hue degrees, glow hue degrees)   order = GuardianScaling.Trait
WAYS = {
    "gravity": (262, 0.9, 0.95, 40, 275),
    "blood": (350, 1.15, 0.95, 38, 355),
    "spatial": (285, 1.0, 1.05, 190, 300),
    "flame": (18, 1.25, 1.0, 45, 28),
    "frost": (196, 0.8, 1.2, 200, 190),
    "verdant": (118, 0.95, 0.95, 50, 130),
    "storm": (212, 0.95, 1.1, 55, 200),
    "shadow": (270, 0.55, 0.6, 285, 285),
    "astral": (45, 0.85, 1.25, 52, 48),
}
SRC_BODY_LO, SRC_BODY_HI = 215.0, 310.0     # the violet of the warden
SRC_TRIM_LO, SRC_TRIM_HI = 20.0, 70.0       # its gold


def recolour(path_in, path_out, way, glow=False):
    body_h, sat_f, val_f, trim_h, glow_h = way
    img = Image.open(path_in).convert("RGBA")
    a = np.asarray(img).astype(np.float64) / 255.0
    rgb = a[..., :3]
    out = rgb.copy()
    h, w = rgb.shape[:2]
    for y in range(h):
        for x in range(w):
            if a[y, x, 3] <= 0.0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(*rgb[y, x])
            deg = hh * 360.0
            if glow:                                              # a glow map is all light: one hue, saturation kept
                hh = glow_h / 360.0
                ss = min(1.0, max(ss, 0.35) * sat_f)
            elif ss > 0.12 and SRC_TRIM_LO <= deg <= SRC_TRIM_HI:
                hh = trim_h / 360.0
            elif ss > 0.12 and SRC_BODY_LO <= deg <= SRC_BODY_HI:
                hh = (body_h + (deg - 265.0) * 0.35) % 360.0 / 360.0     # keep a little of the original shading drift
                ss = min(1.0, ss * sat_f)
                vv = min(1.0, vv * val_f)
            elif ss <= 0.12:                                       # greys and whites take a faint tint of the body hue
                hh, ss = body_h / 360.0, min(0.18, ss + 0.06) * sat_f
                vv = min(1.0, vv * val_f)
            out[y, x] = colorsys.hsv_to_rgb(hh % 1.0, ss, vv)
    res = np.concatenate([out, a[..., 3:4]], axis=2)
    Image.fromarray((np.clip(res, 0, 1) * 255.0 + 0.5).astype(np.uint8), "RGBA").save(path_out, optimize=True)


def main():
    table = G.load_table()
    base = table["floor_warden"]
    for trait, way in WAYS.items():
        lid = "guardian_" + trait
        recolour(os.path.join(SKINS, "floor_warden.png"), os.path.join(SKINS, lid + ".png"), way)
        recolour(os.path.join(SKINS, "floor_warden_glow.png"), os.path.join(SKINS, lid + "_glow.png"), way, glow=True)
        recolour(os.path.join(TEX, "pmx_floor_warden.png"), os.path.join(TEX, "pmx_" + lid + ".png"), way)
        recolour(os.path.join(TEX, "pmx_floor_warden_glow.png"), os.path.join(TEX, "pmx_" + lid + "_glow.png"), way, glow=True)
        row = copy.deepcopy(base)
        row["skin"] = lid
        for ex in row.get("extras", []):
            ex["texture"] = "pmx_" + lid                 # the geo and its animation stay pmx_floor_warden's
        table[lid] = row
        print("colourway", lid)
    G.save_table(table)


if __name__ == "__main__":
    main()
