"""
The Mana Method rune while it is HELD (3.5.0), through the game's own render path: the real RuneWritingLayer, the real RuneHoldView (the client's timeline of a hold) and the real
RuneGlyphLayout.fillStaged, compiled and run by tools/vfx_preview/preview.py (which registers a RuneHoldView through Preview.java's --hold option) and rasterised with the real textures.

Three holds with the default settings (minHoldTicks 8, maxHoldTicks 40):
  a TAP        released after 4 ticks   -> a basic spell: only the hand ring is written (and finished on release)
  a HALF hold  released after 24 ticks  -> a charged spell: hand ring and middle ring, the outer ring is where the hold left it
  a FULL hold  released after 40+ ticks -> the max tier: every ring and the loose glyphs

    python tools/mana_method/preview_hold.py     # writes docs/previews/mana_method/rune_hold_tap.png, rune_hold_half.png, rune_hold_full.png, rune_hold_compare.png

Needs JDK 21, Python 3 with numpy + Pillow, JOML (tools/vfx_preview/lib/joml-1.10.5.jar or $JOML). The time magic effect is not touched.
"""
import importlib.util
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "docs", "previews", "mana_method")

spec = importlib.util.spec_from_file_location("vfxpreview", os.path.join(ROOT, "tools", "vfx_preview", "preview.py"))
pv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pv)
pv.W, pv.H = 576, 347
pv.FOV = 94.0

MIN_HOLD, MAX_HOLD = 8, 40
CASTER = np.array([0.0, 70.0, 0.0])
LOOK = np.array([0.0, -0.05, 1.0]) / np.linalg.norm([0.0, -0.05, 1.0])
CENTRE = CASTER + LOOK * 2.6
SHOULDER = [0.30, 70.55, -0.35]
TARGET = list(CENTRE + np.array([0.0, -0.1, 0.0]))
FLAME = 0 << 3 | 1          # packed RuneSpec: modifiers, True Magic bit, element (Flame, Growth)

HOLDS = {
    "tap": dict(held=4, ages=(1, 2, 3, 4, 6, 9), title="TAP (released after 4 ticks): a basic spell, power 0.5x, only the hand ring",
                names={1: "tick 1", 2: "tick 2", 3: "tick 3", 4: "tick 4: released", 6: "tick 6: hand ring finishing", 9: "tick 9: finished, bolt leaves"}),
    "half": dict(held=24, ages=(4, 8, 12, 16, 20, 24, 27, 31), title="HALF HOLD (released after 24 ticks): a charged spell, hand and middle ring, outer ring left as it was",
                 names={4: "tick 4", 8: "tick 8: minimum reached", 12: "tick 12", 16: "tick 16", 20: "tick 20", 24: "tick 24: released", 27: "tick 27", 31: "tick 31: bolt leaves"}),
    "full": dict(held=44, ages=(4, 10, 16, 22, 28, 34, 40, 44, 50), title="FULL HOLD (40 ticks and more): the max tier, power 1.6x, every ring and the loose glyphs",
                 names={4: "tick 4", 10: "tick 10", 16: "tick 16", 22: "tick 22", 28: "tick 28", 34: "tick 34", 40: "tick 40: full", 44: "tick 44: released", 50: "tick 50: bolt leaves"}),
}


def scene(held, ages, released, cam=None, target=None):
    """released False: the clock of the hold is still running (before the release); True: the view was released at tick `held` after `held` ticks."""
    return {"layer": "RuneWritingLayer", "shape": "RUNE_WRITING", "from": list(CENTRE), "to": list(CENTRE + LOOK), "power": 1.0,
            "duration": MAX_HOLD + 100, "seed": FLAME, "budget": 1200, "detail": 1.0, "cam": cam or SHOULDER, "target": target or TARGET, "ages": list(ages),
            "hold": [MIN_HOLD, MAX_HOLD, held if released else -1, held],
            "figures": [[0, 68.38, 0, 1.8]], "ground": 68.38, "sky": [0.14, 0.18, 0.2], "horizon": [0.22, 0.26, 0.28]}


def shots_for(classes, name, held, ages):
    """The frames at `ages`: the ones before the release come from a view that is still held, the others from the released view (as the client's view changes at the release)."""
    out = []
    for released in (False, True):
        part = [a for a in ages if (a >= held) == released]
        if part:
            out += frames(classes, "%s_%s" % (name, released), scene(held, part, released))
    out.sort(key=lambda t: t[1]["age"])
    return out


def frames(classes, name, sc):
    data = pv.run_java(sc, classes, os.path.join(pv.BUILD, name + ".json"))
    out = []
    for fr in data["frames"]:
        img = pv.background(sc, sc["cam"], sc["target"])
        img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
        img = pv.raster(img, fr["quads"], sc["cam"], sc["target"])
        out.append((Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)), fr))
    return out


def label(im, text, right=None):
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, im.width, 15], fill=(0, 0, 0))
    d.text((4, 2), text, fill=(235, 235, 255))
    if right:
        d.text((im.width - 90, 2), right, fill=(255, 230, 150))
    return im


def sheet(images, path, title, cols, pad=4):
    w, h = max(i.size[0] for i in images), max(i.size[1] for i in images)
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGB", (cols * (w + pad) + pad, rows * (h + pad) + pad + 20), (6, 6, 12))
    ImageDraw.Draw(out).text((8, 4), title, fill=(255, 235, 170))
    for i, im in enumerate(images):
        out.paste(im, (pad + (i % cols) * (w + pad), 20 + pad + (i // cols) * (h + pad)))
    out.save(os.path.join(OUT, path))


def main():
    os.makedirs(pv.BUILD, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    classes = pv.build(["RuneWritingLayer"])
    finals = []
    for key, h in HOLDS.items():
        shots = []
        for im, fr in shots_for(classes, "hold_" + key, h["held"], h["ages"]):
            age = int(fr["age"])
            shots.append(label(im, h["names"].get(age, "tick %d" % age), "%d verts" % fr["vertices"]).resize((pv.W // 2, pv.H // 2), Image.LANCZOS))
        sheet(shots, "rune_hold_%s.png" % key, "Mana Method, " + h["title"], 3 if len(shots) % 3 == 0 else 4)
        # the rune five ticks after the release: what the stage pays for
        age = h["held"] + 5
        im, fr = shots_for(classes, "hold_final_" + key, h["held"], (age,))[0]
        finals.append(label(im, key.upper() + ", 5 ticks after the release", "%d verts" % fr["vertices"]).resize((pv.W // 2, pv.H // 2), Image.LANCZOS))
    sheet(finals, "rune_hold_compare.png", "Tap, half hold and full hold, five ticks after the release (basic: one ring; charged: two rings; full: every ring and the loose glyphs)", 3)
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
