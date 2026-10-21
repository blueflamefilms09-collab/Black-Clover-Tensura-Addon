"""A contact sheet of the 29 inventory sprites of Wave102 (8 x enlarged, labelled): docs/previews/god_special_new/sprites.png.

    python3 -B tools/relics/new_wave102/sheet102.py [out.png]
"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from roster102 import WEAPONS, GEAR  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ICONS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item")


def main(out):
    items = [(e["id"], e["grade"]) for e in WEAPONS] + [(r["id"], r["grade"]) for r in GEAR]
    items.sort(key=lambda x: (x[1] != "GOD", x[0]))
    cols, cell, scale = 8, 150, 8
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (44, 46, 58, 255))
    d = ImageDraw.Draw(sheet)
    for i, (rid, grade) in enumerate(items):
        im = Image.open(os.path.join(ICONS, rid + ".png")).convert("RGBA").resize((16 * scale, 16 * scale), Image.NEAREST)
        x, y = (i % cols) * cell + (cell - 16 * scale) // 2, (i // cols) * (cell + 14) + 4
        sheet.alpha_composite(im, (x, y))
        d.text(((i % cols) * cell + 4, (i // cols) * (cell + 14) + 16 * scale + 6), "%s %s" % ("G" if grade == "GOD" else "S", rid[:22]), fill=(230, 230, 240, 255))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.convert("RGB").save(out)
    print("wrote", os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "previews", "god_special_new", "sprites.png"))
