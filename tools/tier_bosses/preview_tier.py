#!/usr/bin/env python3
"""3.33.1: renders the Tier List bosses as characters (front + back, the player model with skin and hair), like a skin showcase.

    python3 -B tools/tier_bosses/preview_tier.py out.png [--back] [id ...]

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


def tile(look, yaw, w=210, h=330):
    look = dict(look)
    look["scale"] = 1.0                      # same size for every character in the showcase
    sc = P.scene_of(look)
    img = R.render(sc, size=(w, h), yaw=yaw, pitch=4, center=(0, 10, 0), scale=8.6, floor=False, bg=(1, 1, 1))
    return img


def main(argv):
    out = argv[0]
    back = "--back" in argv
    ids = [a for a in argv[1:] if not a.startswith("--")]
    looks = json.load(open(LOOKS))["looks"]
    ids = ids or list(looks)
    cols = 8
    w, h = 210, 330
    views = [0, 180] if back else [0]
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols * len(views) // (1 if not back else 1), h * rows), (255, 255, 255))
    for i, cid in enumerate(ids):
        for vi, yaw in enumerate(views):
            t = tile(looks[cid], yaw, w // len(views) if back else w, h)
            x = (i % cols) * w + vi * (w // len(views))
            sheet.paste(t, (x, (i // cols) * h))
        ImageDraw.Draw(sheet).text(((i % cols) * w + 4, (i // cols) * h + 4), cid, fill=(0, 0, 0))
    sheet = sheet.crop((0, 0, w * cols, h * rows))
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
