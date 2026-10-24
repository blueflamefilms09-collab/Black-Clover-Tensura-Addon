#!/usr/bin/env python3
"""Great Spirit GUI textures (theme 'great_spirit'): writes the PNGs of the two shipped skins, B (Parchment & Ink) and C (Slime Crest), into
src/main/resources/assets/nusmp/textures/gui/gs/<b|c>/. Direction A (Holo Grimoire) is not built and there is no gauge texture at all.

    python3 tools/gen_gs_gui_textures.py [--out src/main/resources/assets/nusmp/textures/gui/gs]

The generators are the ones of the mock-up toolchain (tools/gui_redesign/gs_sprites.py, 2 texels per GUI pixel). Every PNG gets a .mcmeta with
blur + clamp (the art is drawn at half size, like the magic circles). Needs Pillow and numpy. Deterministic (fixed seeds).
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(HERE, "gui_redesign"))

import gs_sprites as SP  # noqa: E402
from gs_book import parchment  # noqa: E402

MCMETA = {"texture": {"blur": True, "clamp": True}}


def build(key):
    th = SP.THEMES[key]
    out = {
        "corner": SP.filigree_corner(th),
        "crest": SP.crest(th),
        "ripple": SP.ripple_ring(th),
    }
    for n in (3, 4, 5):
        r = SP.clover_frame(th, n)
        frame, glow, drips = r if isinstance(r, tuple) else (r, None, None)
        out["clover%d_frame" % n] = frame
        if n >= 4:
            out["clover%d_glow" % n] = glow
        if n == 5:
            out["clover5_drips"] = drips
    if key == "B":
        out["page"] = parchment(163, 172)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "src/main/resources/assets/nusmp/textures/gui/gs"))
    a = ap.parse_args()
    total = 0
    for key in ("B", "C"):
        d = os.path.join(a.out, key.lower())
        os.makedirs(d, exist_ok=True)
        for name, im in sorted(build(key).items()):
            p = os.path.join(d, name + ".png")
            im.convert("RGBA").save(p, optimize=True)
            with open(p + ".mcmeta", "w", encoding="utf-8", newline="\n") as f:
                json.dump(MCMETA, f)
            total += os.path.getsize(p)
            print("%s/%s.png %dx%d %d bytes" % (key.lower(), name, im.width, im.height, os.path.getsize(p)))
    print("total PNG bytes: %d" % total)


if __name__ == "__main__":
    main()
