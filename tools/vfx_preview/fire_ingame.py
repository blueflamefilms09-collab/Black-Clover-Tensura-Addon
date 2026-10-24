#!/usr/bin/env python3
"""
Fire V2, IN-GAME PATH preview.  Unlike fire_new_preview.py (the preview round: quad generators written in Python), this runs the REAL layer classes of the game through the Java preview:

    BEFORE      FireSpellLayer / FlameFxLayer / FlameMagicLayer (the code before the change = what vfx.element.fire = V1 draws)
    V2 in game  FireV2Layer (the code that ships: textures fnw_*, A anime cel mixed with B volumetric glow)

    python tools/gen_fire_v2_textures.py            # once: the game textures fnw_*.png
    python tools/vfx_preview/fire_ingame.py         # needs JDK 21, numpy, Pillow, like preview.py
    python tools/vfx_preview/fire_ingame.py wild    # only the Wild Bursting Flame close-up next to the anime still

Output (docs/previews/vfx_remake/fire_new/):
  fire_ingame.png            BEFORE | V2 early | V2 middle | V2 late, all 11 shapes, same camera
  fire_wild_closeup.png      the anime still of Leopold's Wild Bursting Flame next to the V2 effect at three ticks (bigger tiles)
  fire_ingame_extras.png     a blue flame (the grey twin textures) and the reduced-flashes option, side by side with the normal look
  fire_ingame_budget.json    peak quads of every tick of every effect, old layer and V2, at full detail and at the lowest detail, plus the V1 == old-layer check
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
OUTDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "fire_new")
CW, CH = 480, 360
STILL_DIR = os.environ.get("FIRE_STILLS") or "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/elemzip/Fire2/Fire Magic VFX Effects"

# key, title, old layer, scene, mix (A = anime cel, B = volumetric glow)
ROWS = [
    ("lion", "FIRE_LION  Leo Rugiens   [A cel, the cel lion head kept]", "FireSpellLayer", "fire_v2_lion"),
    ("spear", "FIRE_SPEAR  Sol Linea   [A cel]", "FireSpellLayer", "fire_v2_spear"),
    ("pillar", "FIRE_PILLAR  Ignis Columna   [A cel: pillar + defensive flame vortex]", "FireSpellLayer", "fire_v2_pillar"),
    ("burst", "FIRE_BURST  flame explosion   [A cel]", "FireSpellLayer", "fire_v2_burst"),
    ("spiral", "FIRE_SPIRAL  Spiral Flame   [B glow]", "FireSpellLayer", "fire_v2_spiral"),
    ("wild", "FIRE_WILD  Wild Bursting Flame   [A cel, redone: ground circle + fireballs with beams]", "FireSpellLayer", "fire_v2_wild"),
    ("cast", "FLAMEFX_CAST  Flame cast   [B glow, old charge sigil kept]", "FlameFxLayer", "fire_v2_cast"),
    ("zone", "FLAMEFX_ZONE  Flame zone   [A cel flames, old sun-lion sigil kept]", "FlameFxLayer", "fire_v2_zone"),
    ("fxburst", "FLAMEFX_BURST  Flame burst   [A cel]", "FlameFxLayer", "fire_v2_fxburst"),
    ("trail", "FLAME_TRAIL  stream   [B glow]", "FlameMagicLayer", "fire_v2_trail"),
    ("explosion", "FLAME_EXPLOSION  blast   [A cel]", "FlameMagicLayer", "fire_v2_explosion"),
]


def load_scenes():
    with open(os.path.join(HERE, "scenes.d", "fire_v2.json"), encoding="utf-8") as f:
        return {k: v for k, v in json.load(f).items() if not k.startswith("_")}


def tile(quads, sc, label, sub="", w=None, h=None):
    if w:
        pv.W, pv.H = w, h
    img = pv.background(sc, sc["cam"], sc["target"])
    img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
    img = pv.raster(img, quads, sc["cam"], sc["target"])
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, t.width, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, t.height - 13), sub, fill=(255, 235, 150))
    pv.W, pv.H = CW, CH
    return t


def peak(sc, classes, layer, detail, key, style=None):
    """peak quads over every tick of the effect's life (the game draws once per frame; every integer age 0..duration)"""
    s = dict(sc)
    s["layer"] = layer
    s.pop("fire_style", None)
    if style:
        s["fire_style"] = style
    s["ages"] = list(range(0, int(s["duration"]) + 1))
    s["detail"] = detail
    data = pv.run_java(s, classes, os.path.join(pv.BUILD, f"fnw_peak_{key}_{layer}_{detail}.json"))
    return max(len(fr["quads"]) for fr in data["frames"])


def same_quads(a, b):
    if len(a["frames"]) != len(b["frames"]):
        return False
    for fa, fb in zip(a["frames"], b["frames"]):
        if len(fa["quads"]) != len(fb["quads"]):
            return False
        for qa, qb in zip(fa["quads"], fb["quads"]):
            if qa["t"] != qb["t"] or qa["b"] != qb["b"] or np.abs(np.array(qa["v"]) - np.array(qb["v"])).max() > 1e-3:
                return False
    return True


def v1_equals_old(sc, classes, old_layer, key):
    s = dict(sc)
    s["ages"] = list(range(0, int(s["duration"]) + 1, 2))
    s["fire_style"] = "V1"
    new = pv.run_java(s, classes, os.path.join(pv.BUILD, f"fnw_v1_{key}.json"))
    o = dict(s)
    o["layer"] = old_layer
    o.pop("fire_style", None)
    old = pv.run_java(o, classes, os.path.join(pv.BUILD, f"fnw_old_{key}.json"))
    return same_quads(new, old)


def sheet_all(scenes, classes):
    rows, budget = [], {}
    for key, title, old_layer, name in ROWS:
        sc = dict(scenes[name])
        new = pv.run_java(sc, classes, os.path.join(pv.BUILD, f"fnw_new_{key}.json"))
        osc = dict(sc)
        osc["layer"] = old_layer
        osc.pop("fire_style", None)
        osc["ages"] = [sc["ages"][1]]
        before = pv.run_java(osc, classes, os.path.join(pv.BUILD, f"fnw_old_{key}.json"))
        fr = before["frames"][0]
        tiles = [tile(fr["quads"], sc, f"BEFORE (old layer)  t={fr['age']:.0f}  {len(fr['quads'])} quads")]
        for fr in new["frames"]:
            tiles.append(tile(fr["quads"], sc, f"V2 in game  t={fr['age']:.0f}/{new['duration']}  {len(fr['quads'])} quads"))
        rows.append((title, tiles))
        b = {"duration": sc["duration"], "v1_equals_old_layer": v1_equals_old(sc, classes, old_layer, key)}
        for det in (1.0, 0.5):
            b[f"V2_peak_quads_detail_{det}"] = peak(sc, classes, "FireV2Layer", det, key)
            b[f"before_peak_quads_detail_{det}"] = peak(sc, classes, old_layer, det, key)
        budget[key] = b
        print(key, b)
    cols = max(len(t) for _, t in rows)
    sheet = Image.new("RGB", (CW * cols, CH * len(rows) + 30), (16, 18, 22))
    ImageDraw.Draw(sheet).text((8, 8), "FIRE in the game path:  BEFORE (real old layer) | V2 (real FireV2Layer: A anime cel mixed with B volumetric glow)   same camera; software render, no bloom, not seen in a game", fill=(255, 255, 255))
    for i, (title, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            sheet.paste(t, (j * CW, 30 + i * CH))
        ImageDraw.Draw(sheet).text((6, 30 + i * CH + CH - 28), title, fill=(180, 255, 180))
    sheet.save(os.path.join(OUTDIR, "fire_ingame.png"))
    with open(os.path.join(OUTDIR, "fire_ingame_budget.json"), "w", encoding="utf-8") as f:
        json.dump(budget, f, indent=1)
    print("->", os.path.join(OUTDIR, "fire_ingame.png"))


def still(name, w, h):
    im = Image.open(os.path.join(STILL_DIR, name)).convert("RGB")
    im = im.resize((w, h), Image.LANCZOS)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, w, 14], fill=(14, 16, 20))
    d.text((5, 2), "anime still: " + name.split(".")[0], fill=(255, 255, 255))
    return im


def wild_closeup(scenes, classes, tag=""):
    sc = dict(scenes["fire_v2_wild"])
    sc["cam"] = [8.5, 4.6, -9.5]
    sc["target"] = [0, 1.4, 0]
    sc["ages"] = [5, 11, 18, 25]
    new = pv.run_java(sc, classes, os.path.join(pv.BUILD, "fnw_wild_close.json"))
    w, h = 640, 360
    tiles = [tile(fr["quads"], sc, f"V2 Wild Bursting Flame  t={fr['age']:.0f}/{new['duration']}  {len(fr['quads'])} quads", w=w, h=h) for fr in new["frames"]]
    sheet = Image.new("RGB", (w * 3, h * 2), (16, 18, 22))
    sheet.paste(still("Leopold_wild_bursting_flame.webp", w, h), (0, 0))
    for i, t in enumerate(tiles[:2]):
        sheet.paste(t, (w * (i + 1), 0))
    for i, t in enumerate(tiles[2:]):
        sheet.paste(t, (w * i, h))
    ImageDraw.Draw(sheet).text((w * 2 + 6, h + 20), "left to right: anime still | V2 early | V2 middle | V2 late | V2 end", fill=(255, 235, 150))
    sheet.save(os.path.join(OUTDIR, "fire_wild_closeup%s.png" % tag))
    print("->", os.path.join(OUTDIR, "fire_wild_closeup%s.png" % tag))


def extras(scenes, classes):
    """blue flame (grey twins x colour) and the reduced-flashes option, same camera and tick as the normal look"""
    rows = []
    for key, title, name, ages_i, extra in [
        ("lion_blue", "FIRE_LION in a blue flame colour (5599FF): the grey twins x colour", "fire_v2_lion", 1, {"color": "FF5599FF"}),
        ("burst_blue", "FIRE_BURST in a blue flame colour", "fire_v2_burst", 0, {"color": "FF5599FF"}),
        ("burst_reduced", "FIRE_BURST, reduced flashes ON (tick 2: no star, no rays)  <->  normal tick 2 on the left", "fire_v2_burst", 0, {"reduced": 1}),
        ("explosion_reduced", "FLAME_EXPLOSION, reduced flashes ON (tick 3)  <->  normal tick 3 on the left", "fire_v2_explosion", 0, {"reduced": 1}),
    ]:
        sc = dict(scenes[name])
        sc.update(extra)
        sc["ages"] = [sc["ages"][ages_i], sc["ages"][min(ages_i + 1, 2)]]
        norm = dict(sc)
        norm.pop("color", None)
        norm.pop("reduced", None)
        a = pv.run_java(norm, classes, os.path.join(pv.BUILD, f"fnw_ex_{key}_n.json"))
        b = pv.run_java(sc, classes, os.path.join(pv.BUILD, f"fnw_ex_{key}_x.json"))
        tiles = [tile(a["frames"][0]["quads"], sc, f"normal  t={a['frames'][0]['age']:.0f}  {len(a['frames'][0]['quads'])} quads")]
        for fr in b["frames"]:
            tiles.append(tile(fr["quads"], sc, f"{key}  t={fr['age']:.0f}  {len(fr['quads'])} quads"))
        rows.append((title, tiles))
    sheet = Image.new("RGB", (CW * 3, CH * len(rows)), (16, 18, 22))
    for i, (title, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            sheet.paste(t, (j * CW, i * CH))
        ImageDraw.Draw(sheet).text((6, i * CH + CH - 28), title, fill=(180, 255, 180))
    sheet.save(os.path.join(OUTDIR, "fire_ingame_extras.png"))
    print("->", os.path.join(OUTDIR, "fire_ingame_extras.png"))


def main():
    pv.W, pv.H = CW, CH
    scenes = load_scenes()
    os.makedirs(pv.BUILD, exist_ok=True)
    os.makedirs(OUTDIR, exist_ok=True)
    classes = pv.build(["FireV2Layer"])
    what = sys.argv[1:] or ["all", "wild", "extras"]
    if "all" in what:
        sheet_all(scenes, classes)
    if "wild" in what:
        wild_closeup(scenes, classes)
    if "extras" in what:
        extras(scenes, classes)


if __name__ == "__main__":
    main()
