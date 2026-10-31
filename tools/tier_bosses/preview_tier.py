#!/usr/bin/env python3
"""3.33.1: renders the Tier List bosses as characters (front + back, the player model with skin and hair), like a skin showcase.

    python3 -B tools/tier_bosses/preview_tier.py out.png [--back | --close] [--cols N] [id ...]

Uses the software renderer of tools/playermodel (a review tool: lighting differs from the game). Reads assets/nusmp/playermodel/looks_tier.json.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
sys.path.insert(0, os.path.join(HERE, "..", "playermodel"))

import preview as P  # noqa: E402
import pmrender as R  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

LOOKS = os.path.join(P.ASSETS, "playermodel", "looks_tier.json")


def tile(look, yaw, w=210, h=300, scale=6.3, cy=6, pitch=4):
    look = dict(look)
    look["scale"] = 1.0                      # same size for every character in the showcase
    sc = P.scene_of(look)
    img = R.render(sc, size=(w, h), yaw=yaw, pitch=pitch, center=(0, cy, 0), scale=scale, floor=False, bg=(1, 1, 1))   # 3.37: room for tall hair (afro, spikes)
    return img


def main(argv):
    out = argv[0]
    back = "--back" in argv
    skip = {argv.index("--cols") + 1} if "--cols" in argv else set()
    ids = [a for i, a in enumerate(argv) if i > 0 and not a.startswith("--") and i not in skip]
    looks = json.load(open(LOOKS))["looks"]
    ids = ids or list(looks)
    cols = int(argv[argv.index("--cols") + 1]) if "--cols" in argv else 8
    w, h = (160 * (2 if back else 1)), 300
    views = [0, 180] if back else [0]
    close = "--close" in argv                        # 3.37: a three-quarter bust of every character, like the owner's skin renders
    if close:
        w, h, views, back = 190, 270, [24], False
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * rows), (255, 255, 255))
    for i, cid in enumerate(ids):
        for vi, yaw in enumerate(views):
            t = tile(looks[cid], yaw, w // len(views), h, **({"scale": 8.5, "cy": -1, "pitch": 6} if close else {}))
            x = (i % cols) * w + vi * (w // len(views))
            sheet.paste(t, (x, (i // cols) * h))
        ImageDraw.Draw(sheet).text(((i % cols) * w + 4, (i // cols) * h + 4), cid, fill=(0, 0, 0))
    sheet = sheet.crop((0, 0, w * cols, h * rows))
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
