#!/usr/bin/env python3
"""
Wind pixel look, IN-GAME PATH preview.  Unlike mc_wind_preview.py (phase A: quad generators written in Python), this runs the REAL layer classes of the game through the Java preview:

    BEFORE      WindMagicLayer / WindSpellLayer (the code before the change = what vfx.element.wind = V1 draws)
    C in game   WindMcLayer (the code that ships, nearest-neighbour textures wmc_*, motion on twos, flip-book, ghosts)

    python tools/gen_wind_mc_textures.py            # once: the textures (preview folder + the game's wmc_*.png)
    python tools/vfx_preview/wind_mc_ingame.py      # needs JDK 21, numpy, Pillow, like preview.py

Output: docs/previews/vfx_remake/mc/wind_mc_ingame.png (BEFORE | C early | C middle | C late, same camera) and wind_mc_ingame_budget.json (peak quads of every tick of every effect, at full detail
and at the lowest detail, for the old layer and the new one).
Honest limits: headless software rasteriser, painter's-order sort instead of a depth buffer, no bloom, no world light, no depth against blocks, never seen in a game.
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview as pv                                                        # noqa: E402

ROOT = pv.ROOT
OUTDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "mc")
CW, CH = 480, 360

# key, title, old layer (None = no layer before), scene, ages of the three C tiles, BEFORE tile index into those ages
ROWS = [
    ("slash", "WIND_SLASH  Crescent Kamaitachi", "WindMagicLayer", "wind_mc_slash", [3, 6, 9]),
    ("slash_tint", "WIND_SLASH with another magic's colour (orange)  [grey twin textures x tint]", "WindMagicLayer", "wind_mc_slash_tint", [3, 6, 9]),
    ("emperor", "WIND_EMPEROR  Slicing Wind Emperor", "WindSpellLayer", "wind_mc_emperor", [3, 6, 10]),
    ("gale", "WIND_GALE  Gale (swallow sprites kept)", "WindSpellLayer", "wind_mc_gale", [5, 10, 16]),
    ("gust", "FX_WIND_GUST  Gust Lane (drew only dust before)", None, "wind_mc_gust", [4, 8, 13]),
    ("ring", "WIND_RING  Wind Ring", "WindMagicLayer", "wind_mc_ring", [4, 10, 18]),
]


def load_scenes():
    with open(os.path.join(HERE, "scenes.d", "wind_mc.json"), encoding="utf-8") as f:
        return {k: v for k, v in json.load(f).items() if not k.startswith("_")}


def tile(quads, sc, label, sub=""):
    img = pv.background(sc, sc["cam"], sc["target"])
    img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
    img = pv.raster(img, quads, sc["cam"], sc["target"])
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, CW, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, CH - 13), sub, fill=(255, 235, 150))
    return t


def peak(sc, classes, layer, detail, key):
    """peak quads over every tick of the effect's life (the game draws once per frame; every integer age 0..duration)"""
    s = dict(sc)
    s["layer"] = layer
    s.pop("style", None)
    s["ages"] = list(range(0, int(s["duration"]) + 1))
    s["detail"] = detail
    data = pv.run_java(s, classes, os.path.join(pv.BUILD, f"wmc_peak_{key}_{layer}_{detail}.json"))
    return max(len(fr["quads"]) for fr in data["frames"])


def main():
    pv.W, pv.H = CW, CH
    scenes = load_scenes()
    os.makedirs(pv.BUILD, exist_ok=True)
    classes = pv.build(["WindMcLayer"])
    rows, budget = [], {}
    for key, title, old_layer, name, ages in ROWS:
        sc = dict(scenes[name])
        sc["ages"] = ages
        new = pv.run_java(sc, classes, os.path.join(pv.BUILD, f"wmc_new_{key}.json"))
        tiles = []
        if old_layer:
            osc = dict(sc)
            osc["layer"] = old_layer
            osc.pop("style", None)
            osc["ages"] = [ages[1]]
            before = pv.run_java(osc, classes, os.path.join(pv.BUILD, f"wmc_old_{key}.json"))
            fr = before["frames"][0]
            tiles.append(tile(fr["quads"], sc, f"BEFORE (old layer)  t={fr['age']:.0f}  {len(fr['quads'])} quads"))
        else:
            tiles.append(tile([], sc, "BEFORE: no layer, only vanilla dust", "nothing of the effect was drawn"))
        for fr in new["frames"]:
            tiles.append(tile(fr["quads"], sc, f"C in game  t={fr['age']:.0f}/{new['duration']}  {len(fr['quads'])} quads"))
        rows.append((title, tiles))
        b = {"duration": sc["duration"]}
        for det in (1.0, 0.5):
            b[f"C_peak_quads_detail_{det}"] = peak(sc, classes, "WindMcLayer", det, key)
            if old_layer:
                b[f"before_peak_quads_detail_{det}"] = peak(sc, classes, old_layer, det, key)
        budget[key] = b
        print(key, b)
    cols = max(len(t) for _, t in rows)
    sheet = Image.new("RGB", (CW * cols, CH * len(rows) + 30), (16, 18, 22))
    ImageDraw.Draw(sheet).text((8, 8), "WIND in the game path:  BEFORE (real old layer) | C pixel-anime (real WindMcLayer)   same camera; software render, no bloom, not seen in a game", fill=(255, 255, 255))
    for i, (title, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            sheet.paste(t, (j * CW, 30 + i * CH))
        ImageDraw.Draw(sheet).text((6, 30 + i * CH + CH - 28), title, fill=(180, 255, 180))
    sheet.save(os.path.join(OUTDIR, "wind_mc_ingame.png"))
    with open(os.path.join(OUTDIR, "wind_mc_ingame_budget.json"), "w", encoding="utf-8") as f:
        json.dump(budget, f, indent=1)
    print("->", os.path.join(OUTDIR, "wind_mc_ingame.png"))


if __name__ == "__main__":
    main()
