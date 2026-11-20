#!/usr/bin/env python3
"""Previews of the second Naruto bar (naruto_leaf) beside the old one, drawn with tools/boss_bar_preview.py (the same layout and drawing order as the painter; no animation, no game font).

    python3 tools/boss_bar_art/preview_naruto_v2.py            # docs/boss_showcase/kakashi/boss_bar_v2_*.png

boss_bar_v2_health.png   100 / 60 / 30 percent
boss_bar_v2_phases.png   phase I .. IV (the Mangekyo emblem from phase III), the Kamui window ("Invulnerable"), the Susanoo shield and a wind-up
boss_bar_v2_compare.png  old bar above, new bar below, same states
boss_bar_v2_detail.png   the emblems and the tomoe marker at 6x
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import boss_bar_preview as P  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "docs", "boss_showcase", "kakashi")
TH = (0.75, 0.5, 0.25)
NAME, TITLE = "Kakashi Hatake", "the Copy Ninja"
PH = ["The Copy Ninja", "The Pack", "The Mangekyo", "The Perfect Susanoo"]
S = 3


def bar(sid, **kw):
    kw.setdefault("name", NAME)
    kw.setdefault("title", TITLE)
    kw.setdefault("phases", 4)
    kw.setdefault("thresholds", TH)
    return P.render(sid, S=S, **kw)


def sheet(rows, path, labels=None, cols=1):
    cw, ch = P.W * S + 24, P.FULL_H * S + 14
    nrows = (len(rows) + cols - 1) // cols
    img = P.backdrop(cols * cw + 8, nrows * ch + 8)
    d = ImageDraw.Draw(img)
    for i, im in enumerate(rows):
        x, y = 8 + (i % cols) * cw, 6 + (i // cols) * ch
        img.alpha_composite(im, (x, y))
        if labels:
            d.text((x + 4, y + P.FULL_H * S + 1), labels[i], font=ImageFont.load_default(), fill=(235, 240, 255, 255))
    img.convert("RGB").save(path, optimize=True)


def detail(path):
    z = 6
    sheet_im = P.A.sheet
    names = P.INDEX["emblems"]
    cells = [("naruto sharingan (old)", "sharingan"), ("sharingan_v2", "sharingan_v2"), ("naruto mangekyo (old)", "mangekyo"), ("mangekyo_v2", "mangekyo_v2"), ("tomoe", "tomoe")]
    img = P.backdrop(len(cells) * (32 * z + 16) + 8, 32 * z + 40 + 22 * 3 + 24)
    d = ImageDraw.Draw(img)
    for i, (label, n) in enumerate(cells):
        e = P.A.emblem(n)
        x = 8 + i * (32 * z + 16)
        img.alpha_composite(e.resize((32 * z, 32 * z), Image.LANCZOS), (x, 10))
        img.alpha_composite(e.resize((26, 26), Image.LANCZOS), (x, 32 * z + 20))
        img.alpha_composite(e.resize((11, 11), Image.LANCZOS), (x + 40, 32 * z + 28))
        img.alpha_composite(e.resize((6, 6), Image.LANCZOS), (x + 70, 32 * z + 30))
        d.text((x, 32 * z + 52), label, font=ImageFont.load_default(), fill=(235, 240, 255, 255))
    img.convert("RGB").save(path, optimize=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    health = [bar("naruto_leaf", hp=h, phase=ph) for h, ph in ((1.0, 1), (0.6, 2), (0.3, 3))]
    sheet(health, os.path.join(OUT, "boss_bar_v2_health.png"), ["100%", "60%", "30%"])
    phases = [
        bar("naruto_leaf", hp=0.9, phase=1, fighters=2),
        bar("naruto_leaf", hp=0.62, phase=2, shield=0.0, cast=("Lightning Cutter", 0.55), fighters=2),
        bar("naruto_leaf", hp=0.4, phase=3, invulnerable=True, fighters=2),
        bar("naruto_leaf", hp=0.2, phase=4, shield=0.7, enraged=True, lair=None, cast=("Perfect Susanoo", 0.4), fighters=3),
    ]
    sheet(phases, os.path.join(OUT, "boss_bar_v2_phases.png"), ["phase I", "phase II, winding up", "phase III, Kamui window", "phase IV, Susanoo shield"], cols=2)
    rows, labels = [], []
    for hp, ph in ((1.0, 1), (0.6, 2), (0.3, 3), (0.15, 4)):
        for sid in ("naruto", "naruto_leaf"):
            rows.append(bar(sid, hp=hp, phase=ph))
            labels.append("%s  %d%%" % ("OLD naruto" if sid == "naruto" else "NEW naruto_leaf", round(hp * 100)))
    sheet(rows, os.path.join(OUT, "boss_bar_v2_compare.png"), labels, cols=2)
    detail(os.path.join(OUT, "boss_bar_v2_detail.png"))
    print("wrote previews to", os.path.abspath(OUT))


if __name__ == "__main__":
    main()
