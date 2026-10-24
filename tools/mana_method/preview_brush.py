"""
The Brush Rune (Rill's way, 3.6.0): a rune array painted on the surface the caster looks at, through the game's own render path: the real RuneWritingLayer, a RuneHoldView set up as the client
sets up a painted rune (paintTicks 24, fixed frame, endAge), the real RuneGlyphLayout, and the numbers of the real pure classes (BrushRules: the picture scale, the look of each surface, via DumpMm36.java).

  rune_brush_floor.png     the array on a floor: written in 24 ticks (ticks 4, 10, 16, 24), waiting (tick 90), a foe steps in and the array fires (the rune flares at tick 100 and fades by tick 110)
  rune_brush_surfaces.png  a tap's array and a full hold's array on the floor (the charge sizes the array), then the same array on a wall and on a ceiling

    python tools/mana_method/preview_brush.py

Honest stand-ins: the dark capsule is a stand-in for the foe (the picture has no mob models), and the bolt that leaves the array is the existing Mana Method bolt, not drawn here. The software raster has no
world, lighting or bloom. No gold glow of a grimoire is drawn for a painted rune (it has no book).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mm36_common as mm  # noqa: E402

pv = mm.pv
FLARE_TICKS, FADE = 6, 4          # RuneRules.FLARE_TICKS, RuneHoldView.FADE
PAINT_TICKS = 24                  # RuneClient.PAINT_TICKS


def scene(pack, centre, look, scale, ages, cam, target, paint, figures=(), ground=68.38):
    to = [centre[0] + look[0], centre[1] + look[1], centre[2] + look[2]]
    return {"layer": "RuneWritingLayer", "shape": "RUNE_WRITING", "from": list(centre), "to": to, "power": scale, "duration": 700, "seed": pack, "budget": 1200, "detail": 1.0,
            "cam": list(cam), "target": list(target), "ages": list(ages), "paint": list(paint), "figures": [list(f) for f in figures], "ground": ground,
            "sky": [0.14, 0.18, 0.2], "horizon": [0.22, 0.26, 0.28]}


def tiles(classes, name, sc, texts):
    out = []
    for im, fr in mm.frames(classes, name, sc):
        age = int(fr["age"])
        out.append(mm.half(mm.label(im, texts.get(age, "tick %d" % age), "%d verts" % fr["vertices"])))
    return out


def main():
    os.makedirs(pv.BUILD, exist_ok=True)
    nums = mm.numbers()
    classes = pv.build(["RuneWritingLayer"])
    pack = nums["packs"]["FIRE"]
    looks = nums["looks"]
    tap, full = nums["stages"][0], nums["stages"][2]
    cam = [0.0, 70.0, -1.2]
    floor_c = [0.0, 68.41, 2.4]
    floor_t = [0.0, 68.4, 2.4]
    foe = [[0.25, 68.38, 2.45, 1.8]]
    alive = [PAINT_TICKS, 700, -1]
    fired = [PAINT_TICKS, 100 + FLARE_TICKS + FADE, 100]

    # ---- the floor, written, waiting, firing
    shots = tiles(classes, "brush_write", scene(pack, floor_c, looks["up"], full["brushScale"], (4, 10, 16, 24), cam, floor_t, alive),
                  {4: "written: tick 4", 10: "written: tick 10", 16: "written: tick 16", 24: "written: tick 24 (done)"})
    shots += tiles(classes, "brush_wait", scene(pack, floor_c, looks["up"], full["brushScale"], (90,), cam, floor_t, alive), {90: "waiting (tick 90), nobody near"})
    shots += tiles(classes, "brush_fire", scene(pack, floor_c, looks["up"], full["brushScale"], (100, 103, 106, 109), cam, floor_t, fired, foe),
                   {100: "a foe stepped in: the bolt leaves (tick 100)", 103: "the array flares (tick 103)", 106: "fading (tick 106)", 109: "gone (tick 109)"})
    mm.sheet(shots, "rune_brush_floor.png", "Brush Rune on a floor (full hold: array radius %.1f blocks, lives %d s): painted in 24 ticks, waits, fires once when a foe steps in, is spent" % (full["brushRadius"], full["brushLife"] // 20), 3)

    # ---- the charge sizes the array, and the surfaces
    shots = []
    for st, nm in ((tap, "TAP"), (full, "FULL HOLD")):
        shots += tiles(classes, "brush_size_" + nm[:3], scene(pack, floor_c, looks["up"], st["brushScale"], (60,), cam, floor_t, alive, foe),
                       {60: "floor, %s: radius %.1f blocks, lives %d s" % (nm, st["brushRadius"], st["brushLife"] // 20)})
    wall_c, wall_t = [0.0, 69.7, 5.0 - 0.03], [0.0, 69.7, 5.0]
    shots += tiles(classes, "brush_wall", scene(pack, wall_c, looks["north"], full["brushScale"], (60,), [0.0, 70.0, -0.5], wall_t, alive, [[1.0, 68.38, 3.4, 1.8]]), {60: "wall (north face), full hold"})
    ceil_c, ceil_t = [0.0, 71.97, 2.6], [0.0, 71.9, 2.6]
    shots += tiles(classes, "brush_ceil", scene(pack, ceil_c, looks["down"], full["brushScale"], (60,), [0.0, 69.6, -0.4], ceil_t, alive), {60: "ceiling (down face), full hold"})
    mm.sheet(shots, "rune_brush_surfaces.png", "Brush Rune: the charge sizes the array (a tap's is %.1f blocks, a full hold's %.1f); the same array on a wall and on a ceiling" % (tap["brushRadius"], full["brushRadius"]), 2)
    print("wrote", mm.OUT)


if __name__ == "__main__":
    sys.exit(main())
