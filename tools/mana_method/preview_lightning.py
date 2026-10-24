"""
True Lightning grows with the charge (3.6.0), through the game's own render path and the numbers of the real pure classes (LightningCharge via DumpMm36.java):

  rune_lightning_rune.png    the Lightning rune (the real RuneWritingLayer + RuneHoldView + RuneGlyphLayout) five ticks after the release of a TAP, a HALF hold and a FULL hold:
                             the arcs of the written rune come from one, two and three rings, longer, thicker and brighter with each ring
  rune_lightning_strike.png  the strike at the impact (the real ElementShapesLayer, shape LIGHTNING_SPEAR, sent exactly as RuneManager.lightningFx sends it): 1 / 2 / 3 jagged bolts,
                             8 / 16.6 / 22 blocks tall, 0.9 / 1.45 / 1.8 wide. Three frames each (ticks 1, 4, 8 of the bolt's life).

    python tools/mana_method/preview_lightning.py

The strike is the visual-only bolt: nothing is placed and nothing is hurt by the picture (the real vanilla LightningBolt that TrueLightning spawns stays OFF by default, 'lightningRealBolt').
Honest limit: the software raster shows shape, colour and size; it has no world, lighting or bloom, and the bolt re-rolls its zigzag every 2 ticks as in the game.
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mm36_common as mm  # noqa: E402

pv = mm.pv
MIN_HOLD, MAX_HOLD = 8, 40
CASTER = np.array([0.0, 70.0, 0.0])
LOOK = np.array([0.0, -0.05, 1.0]) / np.linalg.norm([0.0, -0.05, 1.0])
CENTRE = CASTER + LOOK * 2.6
SHOULDER = [0.30, 70.55, -0.35]
TARGET = list(CENTRE + np.array([0.0, -0.1, 0.0]))
NAMES = {"BASIC": "TAP", "CHARGED": "HALF HOLD", "FULL": "FULL HOLD"}


def rune_scene(pack, held, age):
    return {"layer": "RuneWritingLayer", "shape": "RUNE_WRITING", "from": list(CENTRE), "to": list(CENTRE + LOOK), "power": 1.0, "duration": MAX_HOLD + 100, "seed": pack, "budget": 1200,
            "detail": 1.0, "cam": SHOULDER, "target": TARGET, "ages": [age], "hold": [MIN_HOLD, MAX_HOLD, held, held],
            "figures": [[0, 68.38, 0, 1.8]], "ground": 68.38, "sky": [0.14, 0.18, 0.2], "horizon": [0.22, 0.26, 0.28]}


def strike_scenes(st, impact, cam, target, ages):
    """One scene per bolt, sent the way RuneManager.lightningFx sends them: from = ground + 0.6 x offset + (0, height, 0), to = ground, power = width, duration = ticks."""
    scenes = []
    for k in range(st["bolts"]):
        ox, oz = st["offsets"][k]
        ground = [impact[0] + ox, impact[1], impact[2] + oz]
        top = [ground[0] + ox * 0.6, ground[1] + st["height"], ground[2] + oz * 0.6]
        scenes.append({"layer": "ElementShapesLayer", "shape": "LIGHTNING_SPEAR", "from": top, "to": ground, "power": st["width"], "duration": st["ticks"], "color": "FFFFE45C", "seed": 1000 + 31 * k,
                       "cam": cam, "target": target, "ages": ages, "detail": 1.0, "figures": [[0.0, 68.38, impact[2] - 3.0, 1.8]], "ground": 68.38,
                       "sky": [0.10, 0.12, 0.2], "horizon": [0.2, 0.22, 0.3]})
    return scenes


def main():
    os.makedirs(pv.BUILD, exist_ok=True)
    nums = mm.numbers()
    classes = pv.build(["RuneWritingLayer", "ElementShapesLayer"])
    pack = nums["packs"]["LIGHTNING"]

    # ---- the rune: the arcs by ring (the arcs are present two slices out of three and re-roll every 3 ticks: each stage shows the three frames after the release that carry the most arc strips)
    rows = []
    for st in nums["stages"]:
        held = {"BASIC": 4, "CHARGED": 24, "FULL": 44}[st["stage"]]
        ages = [held + 3 + i for i in range(14)]
        shots = mm.frames(classes, "lrune_%s" % st["stage"], dict(rune_scene(pack, held, ages[0]), ages=ages))
        scored = sorted(shots, key=lambda t: -sum(1 for q in t[1]["quads"] if "rune_arc_strip" in q["t"]))
        picked = sorted(scored[:3], key=lambda t: t[1]["age"])
        for im, fr in picked:
            arcs = sum(1 for q in fr["quads"] if "rune_arc_strip" in q["t"]) // 7
            rows.append(mm.half(mm.label(im, "%s (held %d), %d ticks after the release" % (NAMES[st["stage"]], held, fr["age"] - held), "%d arc(s), %d v" % (arcs, fr["vertices"]))))
    mm.sheet(rows, "rune_lightning_rune.png", "True Lightning, the written rune: one arc from the hand ring (tap), two from the middle ring (half hold), three from the outer ring (full hold): longer, thicker, brighter", 3)

    # ---- the strike (cropped to the middle of the picture, where the bolts are)
    old = pv.FOV
    pv.FOV = 60.0
    impact = [0.0, 68.4, 12.0]
    cam, target = [8.0, 70.0, -1.0], [0.0, 76.0, 12.0]
    rows = []
    for st in nums["stages"]:
        scenes = strike_scenes(st, impact, cam, target, [1, 4, 8])
        for im, verts, age in mm.merged(classes, "lstrike_%s" % st["stage"], scenes):
            tile = im.crop((144, 0, 432, pv.H))
            rows.append(mm.label(tile, "%s: %d bolt(s), %.0f blocks" % (NAMES[st["stage"]], st["bolts"], st["height"]), "tick %d" % age))
    pv.FOV = old
    mm.sheet(rows, "rune_lightning_strike.png", "True Lightning, the strike at the impact: 1, 2 and 3 bolts, 8, 16.6 and 22 blocks tall, 0.9 / 1.45 / 1.8 wide (ticks 1, 4 and 8 of the bolt; 10, 14 and 16 ticks long)", 3)
    print("wrote", mm.OUT)
    for st in nums["stages"]:
        print(st["stage"], "bolts", st["bolts"], "height", st["height"], "width", st["width"], "cost", st["costFraction"], "radius x", st["radiusMult"])


if __name__ == "__main__":
    sys.exit(main())
