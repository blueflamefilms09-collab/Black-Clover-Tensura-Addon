"""Builds docs/previews/sylph/sylph_vs_reference.png: each reference picture of the Sylph next to the headless render of the model that is meant to match it, with a one-line caption.

    python3 -B tools/sylph_compare.py <reference dir> [<render dir>]

<reference dir> holds the owner's three pictures (119.png, 120.webp, 121.webp); <render dir> is where tools/render_preview wrote the scenes cmp_small_laugh, cmp_small_point and cmp_powered
(default: $RENDER_PREVIEW_BUILD or build/render_preview). Also writes docs/previews/sylph/sylph_scale.png: the small and the powered form next to a standing player (scenes sylph_scale and
sylph_powered_scale). Run python as `python3 -B`.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "docs", "previews", "sylph")
ROWS = [("119.png", "cmp_small_laugh", "reference 119: the laughing fairy | ours: the small form, idle (closed laughing eyes, blush, bright green ruffled dress, small wings, ahoge)"),
        ("120.webp", "cmp_small_point", "reference 120: pointing at her head | ours: the small form, pointing pose (pale green eyes, open smile, choker, scalloped strapless top, long wings)"),
        ("121.webp", "cmp_powered", "reference 121: the powered form | ours: the powered form (stern face, hair streaming up, big green veined wings, petal skirt, arms spread, glow)")]
H = 520


def fit_h(img, h):
    return img.resize((max(1, int(img.width * h / img.height)), h), Image.LANCZOS)


def main():
    refs = sys.argv[1]
    build = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(HERE, "..", "build", "render_preview")
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for ref, scene, caption in ROWS:
        a = fit_h(Image.open(os.path.join(refs, ref)).convert("RGB"), H)
        b = fit_h(Image.open(os.path.join(build, scene + ".png")).convert("RGB"), H)
        row = Image.new("RGB", (a.width + b.width + 12, H + 26), (22, 22, 28))
        row.paste(a, (0, 26))
        row.paste(b, (a.width + 12, 26))
        ImageDraw.Draw(row).text((6, 6), caption, fill=(230, 230, 230))
        rows.append(row)
    W = max(r.width for r in rows)
    sheet = Image.new("RGB", (W, sum(r.height + 6 for r in rows)), (22, 22, 28))
    y = 0
    for r in rows:
        sheet.paste(r, (0, y))
        y += r.height + 6
    sheet.save(os.path.join(OUT, "sylph_vs_reference.png"), optimize=True)
    print("wrote", os.path.join(OUT, "sylph_vs_reference.png"), sheet.size)
    pair = []
    for scene in ("sylph_scale", "sylph_powered_scale"):
        p = os.path.join(build, scene + ".png")
        if os.path.exists(p):
            pair.append(fit_h(Image.open(p).convert("RGB"), 560))
    if pair:
        sc = Image.new("RGB", (sum(i.width for i in pair) + 8 * (len(pair) - 1), 560), (22, 22, 28))
        x = 0
        for i in pair:
            sc.paste(i, (x, 0))
            x += i.width + 8
        sc.save(os.path.join(OUT, "sylph_scale.png"), optimize=True)
        print("wrote", os.path.join(OUT, "sylph_scale.png"), sc.size)


if __name__ == "__main__":
    main()
