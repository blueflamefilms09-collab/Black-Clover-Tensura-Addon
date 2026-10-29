#!/usr/bin/env python3
"""3.33.1: close-up of the faces of the Tier List bosses (front view of the head, big): python3 -B tools/tier_bosses/preview_heads.py out.png [id ...]"""
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


def main(argv):
    out = argv[0]
    looks = json.load(open(LOOKS))["looks"]
    ids = argv[1:] or list(looks)
    cols = 8
    w, h = 210, 210
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * rows), (255, 255, 255))
    for i, cid in enumerate(ids):
        look = dict(looks[cid])
        look["scale"] = 1.0
        img = R.render(P.scene_of(look), size=(w, h), yaw=0, pitch=0, center=(0, -4, 0), scale=19.0, floor=False, bg=(1, 1, 1))
        sheet.paste(img, ((i % cols) * w, (i // cols) * h))
        ImageDraw.Draw(sheet).text(((i % cols) * w + 4, (i // cols) * h + 4), cid, fill=(0, 0, 0))
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
