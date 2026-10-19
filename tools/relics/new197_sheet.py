"""Contact sheet of the 16 x 16 sprites of the new 1.97.0 weapons (each enlarged x8 on a dark and a light ground): python3 -B tools/relics/new197_sheet.py out.png [id ...]"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from new197_roster import ROSTER                                    # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ICONS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item")


def meshes(out, only):
    """Small profile + three-quarter renders of the shipped meshes, 6 to a row."""
    import numpy as np
    import preview_mesh as pm
    ids = [e["id"] for e in ROSTER if not only or e["id"] in only or ("grade:" + e["grade"]) in only]
    ids = [i for i in ids if os.path.exists(os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "weapon_meshes", i + ".txt"))]
    tiles = []
    tmp = out + ".part.png"
    for i in ids:
        pm.render_preview(i, None, tmp, views=("profile", "three_quarter"), panel=(170, 330), label=False)
        im = Image.open(tmp).convert("RGB")
        d = ImageDraw.Draw(im)
        d.text((4, 3), i[:26], fill=(255, 255, 255))
        tiles.append(im)
    os.remove(tmp) if os.path.exists(tmp) else None
    per = 4
    rows = [tiles[k:k + per] for k in range(0, len(tiles), per)]
    W = max(sum(t.width for t in r) for r in rows)
    sheet = Image.new("RGB", (W, sum(r[0].height for r in rows)), (40, 42, 52))
    y = 0
    for r in rows:
        x = 0
        for t in r:
            sheet.paste(t, (x, y))
            x += t.width
        y += r[0].height
    sheet.save(out)
    print(out, len(ids))


def main(out, only):
    ids = [e["id"] for e in ROSTER if not only or e["id"] in only or ("grade:" + e["grade"]) in only]
    ids = [i for i in ids if os.path.exists(os.path.join(ICONS, i + ".png"))]
    cols = 9
    cell = 16 * 8 + 10
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (58, 60, 72, 255))
    d = ImageDraw.Draw(sheet)
    for n, i in enumerate(ids):
        im = Image.open(os.path.join(ICONS, i + ".png")).convert("RGBA").resize((128, 128), Image.NEAREST)
        x, y = (n % cols) * cell + 5, (n // cols) * (cell + 14) + 14
        sheet.alpha_composite(im, (x, y))
        d.text((x, y - 12), i[:20], fill=(255, 255, 255, 255))
    sheet.save(out)
    print(out, len(ids))


if __name__ == "__main__":
    if sys.argv[1] == "--meshes":
        meshes(sys.argv[2], set(sys.argv[3:]))
    else:
        main(sys.argv[1], set(sys.argv[2:]))
