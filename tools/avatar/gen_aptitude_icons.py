#!/usr/bin/env python3
"""Why: the Soul Forge shows every Magic Aptitude as a circular icon (the Sims-style grid of page 5). This draws all of them, deterministically, from the
table in forge_tables.py and the glyphs of forge_art.py, so no hand-painted file can be lost and a retint is one edit.

    python3 -B tools/avatar/gen_aptitude_icons.py                 # write assets/nusmp/textures/gui/soulforge/apt/<id>.png (32 x 32, RGBA)
    python3 -B tools/avatar/gen_aptitude_icons.py --check         # rendering again must give byte-identical files
    python3 -B tools/avatar/gen_aptitude_icons.py --sheet out.png # also a contact sheet (x6) on the screen's panel colour, for looking at

The rarity ring around an icon is drawn by the screen at runtime (so a retuned rarity needs no new picture); the picture itself is the disc and the glyph.
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import forge_tables as T  # noqa: E402
import forge_art as A  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "soulforge", "apt")


def render(a):
    (aid, name, rarity, tax, magic, skills, tint, glyph, desc) = a
    if glyph not in A.GLYPHS:
        raise SystemExit("no glyph '%s' for %s" % (glyph, aid))
    return A.medallion(glyph, tint)


def sheet(path):
    cols, cell, pad = 10, 32 * 6, 14
    rows = (len(T.APTITUDES) + cols - 1) // cols
    img = Image.new("RGBA", (cols * (cell + pad) + pad, rows * (cell + pad + 14) + pad), (8, 12, 26, 255))
    d = ImageDraw.Draw(img)
    for i, a in enumerate(T.APTITUDES):
        x, y = pad + (i % cols) * (cell + pad), pad + (i // cols) * (cell + pad + 14)
        icon = render(a).resize((cell, cell), Image.NEAREST)
        img.alpha_composite(icon, (x, y))
        d.text((x, y + cell + 2), a[0] + " " + a[2][:3], fill=(190, 205, 230, 255))
    img.save(path)
    print("sheet", path)


def main():
    check = "--check" in sys.argv
    os.makedirs(OUT, exist_ok=True)
    diffs = 0
    for a in T.APTITUDES:
        im = render(a)
        path = os.path.join(OUT, a[0] + ".png")
        if check:
            if not os.path.isfile(path) or Image.open(path).convert("RGBA").tobytes() != im.tobytes():
                print("DIFFERS", path); diffs += 1
        else:
            im.save(path)
    if "--sheet" in sys.argv:
        sheet(sys.argv[sys.argv.index("--sheet") + 1])
    print("%d icons, %s" % (len(T.APTITUDES), "%d differences" % diffs if check else "written"))
    return 1 if diffs else 0


if __name__ == "__main__":
    sys.exit(main())
