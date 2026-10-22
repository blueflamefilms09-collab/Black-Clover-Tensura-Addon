"""Builds docs/previews/sylph/sylph_wings.png: the WINGS of each reference picture of the Sylph (a crop of the owner's picture) beside the headless render of the wings that are meant to match them,
with a one-line caption, and a strip of the wing sheet (membrane) and of the sheen phases.

    python3 -B tools/sylph_wing_compare.py <reference dir> [<render dir>]

<reference dir> holds the owner's three pictures (119.png, 120.webp, 121.webp); <render dir> is where tools/render_preview wrote the scenes cmp_wings_small, cmp_wings_powered and cmp_wings_119
(default: $RENDER_PREVIEW_BUILD or build/render_preview). Run python as `python3 -B`.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "docs", "previews", "sylph")
TEX = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity")
# (reference file, crop box in the reference (l, t, r, b), scene, caption)
ROWS = [("119.png", (110, 120, 480, 420), "cmp_wings_119", "reference 119 (small form): small translucent yellow-green wings | ours: the small wings, seen as in the first picture"),
        ("120.webp", (0, 0, 851, 520), "cmp_wings_small", "reference 120: long, narrow, tall iridescent wings with pointed tips, a large front and a smaller rear pair | ours: the small form's wings"),
        ("121.webp", (0, 30, 847, 640), "cmp_wings_powered", "reference 121 (powered form): big green dragonfly wings, a clear main vein, many fine cross veins, spread wide and angled up | ours: the powered form's wings")]
H = 440


def fit_h(img, h):
    return img.resize((max(1, int(img.width * h / img.height)), h), Image.LANCZOS)


def sheet_strip(form, width):
    """The wing sheet (membrane) over a dark ground and, below, the sheen phases, as one strip."""
    base = Image.open(os.path.join(TEX, "sylph_%s_wings.png" % form)).convert("RGBA")
    sheen = Image.open(os.path.join(TEX, "sylph_%s_wings_glow.png" % form)).convert("RGBA")
    k = 6
    ph = sheen.height // k
    a = Image.alpha_composite(Image.new("RGBA", base.size, (28, 42, 40, 255)), base)
    tiles = []
    for i in (0, 2, 4):
        t = sheen.crop((0, i * ph, sheen.width, (i + 1) * ph)).resize(base.size, Image.LANCZOS)
        tiles.append(Image.alpha_composite(Image.new("RGBA", base.size, (10, 12, 14, 255)), t))
    out = Image.new("RGB", (base.width * 2 + 6, base.height * 2 + 6), (22, 22, 28))
    out.paste(a.convert("RGB"), (0, 0))
    out.paste(tiles[0].convert("RGB"), (base.width + 6, 0))
    out.paste(tiles[1].convert("RGB"), (0, base.height + 6))
    out.paste(tiles[2].convert("RGB"), (base.width + 6, base.height + 6))
    return fit_w(out, width)


def fit_w(img, w):
    return img.resize((w, max(1, int(img.height * w / img.width))), Image.LANCZOS)


def main():
    refs = sys.argv[1]
    build = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(HERE, "..", "build", "render_preview")
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for ref, box, scene, caption in ROWS:
        a = fit_h(Image.open(os.path.join(refs, ref)).convert("RGB").crop(box), H)
        b = fit_h(Image.open(os.path.join(build, scene + ".png")).convert("RGB"), H)
        row = Image.new("RGB", (a.width + b.width + 12, H + 26), (22, 22, 28))
        row.paste(a, (0, 26))
        row.paste(b, (a.width + 12, 26))
        ImageDraw.Draw(row).text((6, 6), caption, fill=(230, 230, 230))
        rows.append(row)
    W = max(r.width for r in rows)
    strips = []
    for form in ("small", "powered"):
        s = sheet_strip(form, W // 2 - 3)
        strips.append(s)
    sh = max(s.height for s in strips)
    last = Image.new("RGB", (W, sh + 26), (22, 22, 28))
    ImageDraw.Draw(last).text((6, 6), "the wing sheets: membrane with its vein network (top left) and three phases of the iridescent sheen, small form (left) and powered form (right)", fill=(230, 230, 230))
    last.paste(strips[0], (0, 26))
    last.paste(strips[1], (W // 2 + 3, 26))
    rows.append(last)
    sheet = Image.new("RGB", (W, sum(r.height + 6 for r in rows)), (22, 22, 28))
    y = 0
    for r in rows:
        sheet.paste(r, (0, y))
        y += r.height + 6
    sheet.save(os.path.join(OUT, "sylph_wings.png"), optimize=True)
    print("wrote", os.path.join(OUT, "sylph_wings.png"), sheet.size)


if __name__ == "__main__":
    main()
