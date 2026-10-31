#!/usr/bin/env python3
"""3.33.1: renders the Tier List bosses as characters (front + back, the player model with skin and hair), like a skin showcase.

    python3 -B tools/tier_bosses/preview_tier.py out.png [--back | --close | --trio] [--cols N] [--aa N] [--skin-dir DIR] [id ...]

Uses the software renderer of tools/playermodel (a review tool: lighting differs from the game). Reads assets/nusmp/playermodel/looks_tier.json.
3.38.0 (HD outfits, docs/bosses/hd_outfits.md): `--trio` draws front, back and a three-quarter close-up of every id in one row; `--aa N` renders at N times the size and averages down (default 1
for a 64x64 skin, 2 for an HD skin: the renderer samples nearest, so an HD skin needs it to show its detail); `--skin-dir DIR` reads the skins (and glow maps) from another folder, for example the
64x64 version made with `gen_tier_looks.py --size 64`, to put the two side by side.
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


def skin_size(look, skin_dir=None):
    """The width of the look's skin PNG (64 for the older characters, 256 for an HD one)."""
    d = skin_dir or os.path.join(P.ASSETS, "textures", "entity", "playermodel")
    try:
        return Image.open(os.path.join(d, look["skin"] + ".png")).width
    except OSError:
        return 64


def tile(look, yaw, w=210, h=300, scale=6.3, cy=6, pitch=4, aa=None, skin_dir=None):
    look = dict(look)
    look["scale"] = 1.0                      # same size for every character in the showcase
    if aa is None:
        aa = 2 if skin_size(look, skin_dir) > 64 else 1                      # 3.38: an HD skin is drawn at 2x and averaged down so its detail shows
    if skin_dir:                                                             # the skin and its _glow map are read relative to the kit's skin folder: a relative path reaches another folder
        look["skin"] = os.path.relpath(os.path.join(skin_dir, look["skin"]), os.path.join(P.ASSETS, "textures", "entity", "playermodel"))
    sc = P.scene_of(look)
    img = R.render(sc, size=(w * aa, h * aa), yaw=yaw, pitch=pitch, center=(0, cy, 0), scale=scale * aa, floor=False, bg=(1, 1, 1))   # 3.37: room for tall hair (afro, spikes)
    return img.resize((w, h), Image.LANCZOS) if aa > 1 else img


def main(argv):
    out = argv[0]
    back = "--back" in argv
    opts = {"--cols": 8, "--aa": None, "--skin-dir": None}
    skip = set()
    for k in opts:
        if k in argv:
            skip.add(argv.index(k) + 1)
            opts[k] = argv[argv.index(k) + 1]
    ids = [a for i, a in enumerate(argv) if i > 0 and not a.startswith("--") and i not in skip]
    looks = json.load(open(LOOKS))["looks"]
    ids = ids or list(looks)
    cols = int(opts["--cols"])
    aa = int(opts["--aa"]) if opts["--aa"] else None
    sd = opts["--skin-dir"]
    w, h = (160 * (2 if back else 1)), 300
    views = [0, 180] if back else [0]
    kw = [{}, {}]
    close = "--close" in argv                        # 3.37: a three-quarter bust of every character, like the owner's skin renders
    if close:
        w, h, views, back = 190, 270, [24], False
        kw = [{"scale": 8.5, "cy": -1, "pitch": 6}]
    if "--trio" in argv:                             # 3.38: front, back and a three-quarter bust in one row per character
        w, h, views, back = 210 * 3, 300, [0, 180, 24], False
        kw = [{}, {}, {"scale": 9.5, "cy": 0, "pitch": 6}]
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * rows), (255, 255, 255))
    for i, cid in enumerate(ids):
        for vi, yaw in enumerate(views):
            tw = w // len(views)
            t = tile(looks[cid], yaw, tw, h, aa=aa, skin_dir=sd, **kw[vi % len(kw)])
            x = (i % cols) * w + vi * tw
            sheet.paste(t, (x, (i // cols) * h))
        ImageDraw.Draw(sheet).text(((i % cols) * w + 4, (i // cols) * h + 4), cid, fill=(0, 0, 0))
    sheet = sheet.crop((0, 0, w * cols, h * rows))
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
