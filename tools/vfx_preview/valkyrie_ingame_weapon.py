#!/usr/bin/env python3
"""
The Valkyrie Drill Lance as the weapon item the game draws (docs/vfx_remake/valkyrie_ingame.md): the item preview of tools/item_preview/preview_weapons.py, framed for a 3-block weapon, with the turning drill.

    python3 tools/gen_valkyrie_lance_model.py ; python3 tools/vfx_preview/valkyrie_ingame_weapon.py

Output: docs/previews/vfx_remake/valkyrie/ingame_weapon.png
  row 1: the 16 x 16 inventory icon, then the model in hand placement at rest from the front, three quarters, the side and the back
  row 2: the drill turned by 0, 45, 90, 135 and 180 degrees (the renderer rotates the quads after `part drill` about the mesh's +y axis by idle + charge), three-quarter view
  row 3: the same at 720 degrees a second is a turn of 36 degrees a tick: frames one tick apart, 0, 36, 72, 108, 144 degrees, the tip close up
Honest limits: the software rasteriser of preview_weapons.py (nearest texel skin, cutout, item-style lighting, the glow additive, no bloom, no pulse), never seen in a game.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "item_preview"))
import preview_weapons as pw                                                   # noqa: E402

ID = "valkyrie_drill_lance"
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie", "ingame_weapon.png")


def frame(spin, yaw, pitch, dist, target):
    quads, glow = pw.load(ID, spin)
    d = os.path.join(pw.ASSETS, "textures", "entity", "weapon")
    skin, glowtex = pw.tex(os.path.join(d, ID + ".png")), pw.tex(os.path.join(d, ID + "_glow.png"))
    k = skin.shape[1] / 128.0
    quads = [(ly, np.concatenate([pts[:, :3], pts[:, 3:5] * k], axis=1), n) for (ly, pts, n) in quads]
    return pw.render(quads, skin, glowtex, glow, pw.view(yaw, pitch, dist, target)), quads


def label(img, text):
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    ImageDraw.Draw(im).text((4, 3), text, fill=(255, 255, 255))
    return np.asarray(im, np.float32) / 255


def main():
    quads, glow = pw.load(ID)
    allp = np.concatenate([q[1][:, :3] for q in quads])
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    centre = (lo + hi) / 2
    span = float(np.max(hi - lo))
    dist = 1.9 * max(1.0, span / 1.9)
    tip = np.array([hi[0] - 0.12 * (hi[0] - lo[0]), hi[1] - 0.12 * (hi[1] - lo[1]), centre[2]])
    rows = []
    r1 = [pw.icon(ID)]
    for (yw, p, nm) in ((0, 0, "front"), (35, 20, "3/4"), (88, 5, "side"), (180, 0, "back")):
        img, _ = frame(0.0, yw, p, dist, centre)
        r1.append(label(img, "at rest, " + nm))
    rows.append(np.concatenate(r1, 1))
    r2 = [pw.icon(ID) * 0 + 0.33]
    for ang in (0, 45, 90, 135, 180)[:4]:
        img, _ = frame(float(ang), 35, 20, dist, centre)
        r2.append(label(img, "drill turned %d deg" % ang))
    rows.append(np.concatenate(r2, 1))
    r3 = [pw.icon(ID) * 0 + 0.33]
    for t in range(4):
        img, _ = frame(36.0 * t, 35, 10, dist * 0.42, tip)
        r3.append(label(img, "tip, +%d ticks at full speed (%d deg)" % (t, 36 * t)))
    rows.append(np.concatenate(r3, 1))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray((np.concatenate(rows, 0) * 255).astype(np.uint8)).save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
