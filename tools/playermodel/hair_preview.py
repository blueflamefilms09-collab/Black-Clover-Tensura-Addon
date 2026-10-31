#!/usr/bin/env python3
"""Why: a review tool for the anime_* haircuts (3.37 hair rework): renders every style on a plain head and on a Tier List boss, front / three-quarter / back (and a side view), with the
software renderer of pmrender.py (lighting differs from the game; the hair springs are at rest), and writes a contact sheet.

    python3 -B tools/playermodel/hair_preview.py out.png [--boss] [--cols N] [--views 0,35,180] [style ...]
        out.png        the contact sheet; every row is a style, every column a view
        --boss         the styles are drawn on a boss skin (the first boss that wears the style in looks_tier.json, see BOSS_OF) instead of on the plain head
        --views        yaw angles in degrees (default 0,35,90,180)
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import numpy as np  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

import pmrender as R  # noqa: E402

ASSETS = R.ASSETS
LOOKS_TIER = os.path.join(ASSETS, "playermodel", "looks_tier.json")

# the boss each style is tried on in the review (the one the owner named for it)
BOSS_OF = {
    "anime_short": "axe_hand_morgan", "anime_short_open": "bertholdt_hoover", "anime_swept": "light_yagami", "anime_long": "malty_melromarc", "anime_long_open": "rachel",
    "anime_afro": "foxy", "anime_afro_wide": "mr_2_bon_clay", "anime_topknot": "kurozumi_orochi", "anime_mane": "big_mom", "anime_hood": "zetsu", "anime_spiky": "code",
    "anime_spike_tall": "yammy_llargo", "anime_spike_swept": "code", "anime_curls": "big_mom", "anime_flattop": "axe_hand_morgan", "anime_bangs_long": "ao",
}
PLAIN = {"anime_afro": (60, 38, 30), "anime_afro_wide": (40, 36, 70), "anime_curls": (200, 60, 110), "anime_spike_tall": (225, 190, 70), "anime_spike_swept": (150, 150, 160),
         "anime_spiky": (225, 190, 70), "anime_flattop": (80, 70, 60), "anime_bangs_long": (50, 70, 140)}


def plain_scene(style, rgb):
    sc = R.Scene()
    sc.add(R.player_faces(R.flat_skin(), slim=False))
    sc.add(R.hair_faces(style, rgb))
    return sc


def boss_scene(style, boss, rgb=None):
    import preview as P
    looks = json.load(open(LOOKS_TIER))["looks"]
    look = dict(looks[boss])
    look["hair"] = style
    look["scale"] = 1.0
    if rgb is not None:
        look["hair_color"] = "#%02x%02x%02x" % tuple(rgb)
    return P.scene_of(look)


def tile(sc, yaw, size=(210, 250), scale=11.0, pitch=8, cy=-9):
    return R.render(sc, size=size, yaw=yaw, pitch=pitch, center=(0, cy, 0), scale=scale, floor=False, bg=(1, 1, 1))


def sheet(styles, views, on_boss, out, size=(210, 250)):
    W, H = size
    img = Image.new("RGB", (W * len(views), H * len(styles)), (255, 255, 255))
    d = ImageDraw.Draw(img)
    for r, st in enumerate(styles):
        rgb = PLAIN.get(st, (225, 190, 70))
        if on_boss:
            sc = boss_scene(st, BOSS_OF.get(st, "code"))
        else:
            sc = plain_scene(st, rgb)
        for c, yaw in enumerate(views):
            img.paste(tile(sc, yaw, size), (c * W, r * H))
        d.text((4, r * H + 4), st + (" on " + BOSS_OF.get(st, "code") if on_boss else ""), fill=(0, 0, 0))
    img.save(out)
    return img


def main(argv):
    out = argv[0]
    on_boss = "--boss" in argv
    views = [0, 35, 90, 180]
    if "--views" in argv:
        views = [int(x) for x in argv[argv.index("--views") + 1].split(",")]
    skip = {argv.index("--views") + 1} if "--views" in argv else set()
    styles = [a for i, a in enumerate(argv) if i > 0 and not a.startswith("--") and i not in skip]
    doc = R.hair_doc()
    styles = styles or [k for k in doc["styles"] if k.startswith("anime_")]
    img = sheet(styles, views, on_boss, out)
    print("wrote", out, img.size)


if __name__ == "__main__":
    main(sys.argv[1:])
