#!/usr/bin/env python3
"""Owner decision (3.31.0): the 14 weapon skins of entity/weapon_vu that are 2048 px are shrunk to 1024 px (the jar is almost all textures; these 14 were ~70 MB).
Re-runnable: a file that is already <= --size is left alone. Alpha-aware (premultiplied) Lanczos so cut-out edges get no dark halo; UVs are normalised so the models are unchanged.
    python3 -B tools/downscale_weapon_skins.py [--size 1024] [--check]
"""
import argparse
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity", "weapon_vu"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--size", type=int, default=1024)
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    before = after = n = 0
    for f in sorted(os.listdir(ROOT)):
        p = os.path.join(ROOT, f)
        if not f.endswith(".png"):
            continue
        im = Image.open(p)
        if im.width <= a.size and im.height <= a.size:
            continue
        old = os.path.getsize(p)
        im.load()
        out = im.convert("RGBA").convert("RGBa").resize((a.size, a.size), Image.LANCZOS).convert("RGBA")
        if not a.check:
            out.save(p + ".tmp", "PNG", optimize=True)
            os.replace(p + ".tmp", p)
        new = old if a.check else os.path.getsize(p)
        before += old
        after += new
        n += 1
        print("%-48s %5d KB -> %5d KB" % (f, old // 1024, new // 1024))
    print("%d file(s): %d KB -> %d KB" % (n, before // 1024, after // 1024))


if __name__ == "__main__":
    main()
