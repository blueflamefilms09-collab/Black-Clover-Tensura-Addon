"""
Authors the Mana Method rune glyph alphabet: 32 angular rune glyphs, 32x32 each, in an 8x4 atlas (row major, the order of RuneGlyphLayout.GLYPH_COUNT), with a bright core and a
red-orange outline like the owner's reference frame (readable symbols, vertical stems with hooks and ticks), in one tint per element.

    python tools/mana_method/gen_rune_glyphs.py          # writes docs/previews/mana_method/glyph_atlas/*.png and rune_glyph_alphabet.png

Deterministic (seeded): the glyph shapes are strokes on a 5x5 lattice (horizontal, vertical and diagonal steps only, so every glyph is angular), 2 to 4 strokes, sometimes a dot or a
tick; a candidate is kept only if it is different enough from every glyph before it. Each glyph is stored as two masks (core, outline) and recoloured per element, so one script gives
the whole family. Nothing here is a game asset yet: the atlases are preview output.
"""
import json
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "docs", "previews", "mana_method")
ATLAS_DIR = os.path.join(OUT, "glyph_atlas")

N, SS, SIZE = 32, 4, 32                      # glyphs, supersampling, pixels per glyph
COLS, ROWS = 8, 4
LATTICE = 5

# element -> (core colour, outline colour). "gold" is the reference frame's look and the True Magic default.
TINTS = {
    "gold": ((255, 238, 110), (232, 84, 22)),
    "flame": ((255, 214, 96), (222, 52, 12)),
    "water": ((190, 240, 255), (28, 108, 226)),
    "wind": ((228, 255, 238), (52, 176, 134)),
    "earth": ((255, 232, 170), (150, 98, 44)),
    "lightning": ((255, 255, 190), (240, 168, 20)),
    "plant": ((220, 255, 170), (66, 170, 52)),
}

DIRS = [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)]


def make_strokes(rng):
    """One glyph as a list of strokes, each a list of lattice points (0..4, 0..4)."""
    strokes = []
    n = rng.choice([2, 2, 3, 3, 4])
    used = set()
    # a vertical-ish stem most of the time, like the reference's tall glyphs
    if rng.random() < 0.7:
        x = rng.choice([1, 2, 2, 3])
        y0, y1 = rng.choice([0, 0, 1]), rng.choice([3, 4, 4])
        strokes.append([(x, y0), (x, y1)])
        used.add(((x, y0), (x, y1)))
    while len(strokes) < n:
        x, y = rng.randrange(LATTICE), rng.randrange(LATTICE)
        pts = [(x, y)]
        for _ in range(rng.choice([1, 2, 2, 3])):
            for _try in range(8):
                dx, dy = rng.choice(DIRS)
                step = rng.choice([1, 1, 2])
                nx, ny = x + dx * step, y + dy * step
                if 0 <= nx < LATTICE and 0 <= ny < LATTICE and ((x, y), (nx, ny)) not in used and ((nx, ny), (x, y)) not in used:
                    used.add(((x, y), (nx, ny)))
                    pts.append((nx, ny))
                    x, y = nx, ny
                    break
        if len(pts) >= 2:
            strokes.append(pts)
    dot = None
    if rng.random() < 0.4:
        dot = (rng.randrange(LATTICE), rng.choice([0, 4]))
    return strokes, dot


def render(strokes, dot):
    """-> (core mask, outline mask) as float arrays N x N (0..1)."""
    S = SIZE * SS
    pad = 5 * SS
    span = S - 2 * pad
    def P(pt):
        return (pad + pt[0] / (LATTICE - 1) * span, pad + pt[1] / (LATTICE - 1) * span)
    outline = Image.new("L", (S, S), 0)
    core = Image.new("L", (S, S), 0)
    for img, width in ((outline, 5.2 * SS), (core, 2.4 * SS)):
        d = ImageDraw.Draw(img)
        for pts in strokes:
            xy = [P(p) for p in pts]
            d.line(xy, fill=255, width=int(width), joint="curve")
            for q in xy:
                r = width / 2
                d.rectangle([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=255)          # square joints keep it angular
        if dot is not None:
            q = P(dot)
            r = width * 0.55
            d.rectangle([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=255)
    f = lambda im: np.asarray(im.resize((SIZE, SIZE), Image.LANCZOS)).astype(np.float32) / 255.0
    return f(core), f(outline)


def different(a, b):
    return float(np.abs(a - b).mean())


def build():
    rng = random.Random(20260424)
    glyphs, masks = [], []
    tries = 0
    while len(glyphs) < N and tries < 20000:
        tries += 1
        strokes, dot = make_strokes(rng)
        core, outline = render(strokes, dot)
        cover = float((outline > 0.5).mean())
        if not (0.16 <= cover <= 0.40):
            continue
        small = np.asarray(Image.fromarray((outline * 255).astype(np.uint8)).resize((8, 8), Image.BOX)).astype(np.float32) / 255.0
        # also reject the mirror image: two glyphs that are mirrors of each other are hard to tell apart on a ring
        if any(different(small, m) < 0.085 or different(small[:, ::-1], m) < 0.06 for m in masks):
            continue
        masks.append(small)
        glyphs.append({"strokes": [[list(p) for p in s] for s in strokes], "dot": list(dot) if dot else None})
    assert len(glyphs) == N, "only %d glyphs after %d tries" % (len(glyphs), tries)
    return glyphs


def colourise(core, outline, tint):
    cc, oc = np.array(TINTS[tint][0], np.float32) / 255.0, np.array(TINTS[tint][1], np.float32) / 255.0
    glow = np.asarray(Image.fromarray((outline * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.6))).astype(np.float32) / 255.0 * 0.45
    a = np.clip(outline + glow, 0, 1)
    rgb = oc[None, None, :] * (np.clip(outline, 0, 1)[..., None]) + oc[None, None, :] * glow[..., None] * 0.6
    rgb = rgb * (1 - core[..., None]) + cc[None, None, :] * core[..., None]
    a = np.maximum(a, core)
    return np.dstack([np.clip(rgb, 0, 1), a])


def atlas(glyph_masks, tint):
    sheet = np.zeros((ROWS * SIZE, COLS * SIZE, 4), np.float32)
    for i, (core, outline) in enumerate(glyph_masks):
        r, c = divmod(i, COLS)
        sheet[r * SIZE:(r + 1) * SIZE, c * SIZE:(c + 1) * SIZE] = colourise(core, outline, tint)
    return sheet


def to_image(sheet):
    return Image.fromarray((np.clip(sheet, 0, 1) * 255).astype(np.uint8), "RGBA")


def load_masks():
    glyphs = json.load(open(os.path.join(HERE, "rune_glyphs.json")))
    return [render([[tuple(p) for p in s] for s in g["strokes"]], tuple(g["dot"]) if g["dot"] else None) for g in glyphs]


def main():
    path = os.path.join(HERE, "rune_glyphs.json")
    if not os.path.exists(path) or "--regen" in sys.argv:
        json.dump(build(), open(path, "w"), indent=0)
    masks = load_masks()
    os.makedirs(ATLAS_DIR, exist_ok=True)
    for tint in TINTS:
        to_image(atlas(masks, tint)).save(os.path.join(ATLAS_DIR, "rune_glyphs_%s.png" % tint))
    # the alphabet sheet: every glyph large on a dark ground, in gold, then the tints as one row each (first 8 glyphs)
    cell, scale = 70, 2
    sheet = Image.new("RGB", (COLS * cell + 100, (ROWS + len(TINTS) - 1) * cell + 70), (12, 12, 24))
    d = ImageDraw.Draw(sheet)
    d.text((10, 6), "Mana Method rune alphabet: 32 glyphs, 32x32 (shown 2x), atlas cell = index", fill=(255, 235, 170))
    gold = atlas(masks, "gold")
    for i in range(N):
        r, c = divmod(i, COLS)
        tile = to_image(gold[r * SIZE:(r + 1) * SIZE, c * SIZE:(c + 1) * SIZE]).resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        bg = Image.new("RGB", tile.size, (12, 12, 24))
        bg.paste(tile, (0, 0), tile)
        sheet.paste(bg, (10 + c * cell + 3, 28 + r * cell + 3))
        d.text((10 + c * cell + 4, 28 + r * cell + 4), str(i), fill=(120, 130, 170))
    y0 = 28 + ROWS * cell + 6
    d.text((10, y0 - 4), "per element tints (glyphs 0..7):", fill=(255, 235, 170))
    for k, tint in enumerate([t for t in TINTS if t != "gold"]):
        sh = atlas(masks, tint)
        for c in range(COLS):
            tile = to_image(sh[0:SIZE, c * SIZE:(c + 1) * SIZE]).resize((SIZE * scale, SIZE * scale), Image.NEAREST)
            bg = Image.new("RGB", tile.size, (12, 12, 24))
            bg.paste(tile, (0, 0), tile)
            sheet.paste(bg, (10 + c * cell + 3, y0 + 12 + k * cell))
        d.text((12 + COLS * cell - 4, y0 + 12 + k * cell + 24), tint, fill=(200, 205, 230))
    os.makedirs(OUT, exist_ok=True)
    sheet.save(os.path.join(OUT, "rune_glyph_sprites_alt.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
