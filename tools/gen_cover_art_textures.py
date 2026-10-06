"""Grimoire cover textures from the owner's art pack (0.24).

    python tools/gen_cover_art_textures.py   ->  textures/item/grimoire_book/cover_art_<set>.png   (64x80, RGBA)
                                                 textures/item/grimoire_book/cover_base_<set>.png  (64x80, greyscale)
                                                 textures/item/grimoire_book/emblem_<black_magic|god_tier>.png
                                                 build/cover_art/preview.png (not shipped)

Sources: tools/art/covers/<set>.jpg, cropped from "Black Clover Art.zip" (images 11-20):
  three_leaf (blue, gold filigree), four_leaf (green, gold floral corners), five_leaf (royal blue, black five-leaf),
  spade (black, gold spears), triple_spade (black, arcane wheels), heart (purple cloud swirls), two_heart (teal, sea serpent),
  diamond (ice-white stained glass, gold baroque frame), black_magic (black, blood-red burst), god_tier (black, gold sunburst).

Each design is split in two: its smooth background is dropped (the book's leather keeps the magic's colour, as in the anime) and
its ornament (everything that stands out from the local background: filigree, frames, spears, swirls, glass leading) is kept in
the art's own colours. The model lays it over both covers and makes it glow while the book is held.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..")
SRC = os.path.join(os.path.dirname(__file__), "art", "covers")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book")
PREVIEW = os.path.join(ROOT, "build", "cover_art")
W, H = 64, 80
SETS = ["three_leaf", "four_leaf", "five_leaf", "spade", "triple_spade", "heart", "two_heart", "diamond", "black_magic", "god_tier"]
# per set: (detail threshold low, high) on the colour distance from the local background, and a brightness lift for the ornament
TUNE = {"diamond": (0.08, 0.18, 1.0), "black_magic": (0.10, 0.22, 1.15), "god_tier": (0.10, 0.22, 1.1), "two_heart": (0.10, 0.22, 1.1),
        "four_leaf": (0.14, 0.26, 1.05), "three_leaf": (0.12, 0.24, 1.05)}


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def layers(name):
    """(ornament RGBA in the art's colours, greyscale base RGBA from the art's background) at W x H."""
    im = Image.open(os.path.join(SRC, name + ".jpg")).convert("RGB").resize((W * 4, H * 4), Image.LANCZOS)
    a = np.asarray(im).astype(np.float32) / 255
    lo, hi, lift = TUNE.get(name, (0.11, 0.24, 1.05))
    # these designs sit on one dominant colour: distance from it finds the ornament; a local-contrast term keeps fine lines
    bgc = np.median(a.reshape(-1, 3), axis=0)
    dist = np.sqrt(((a - bgc) ** 2).sum(-1) / 3)
    local = np.asarray(im.filter(ImageFilter.GaussianBlur(3))).astype(np.float32) / 255
    detail = np.sqrt(((a - local) ** 2).sum(-1) / 3)
    alpha = np.maximum(smoothstep(lo, hi, dist), smoothstep(0.07, 0.16, detail) * 0.85)
    yy, xx = np.mgrid[0:H * 4, 0:W * 4]
    edge = np.minimum.reduce([xx, yy, W * 4 - 1 - xx, H * 4 - 1 - yy])
    alpha *= np.clip(edge / 5.0, 0, 1)                    # drop the photo edge / scan border
    rgb = np.clip(a * lift + 0.03, 0, 1)
    pre = np.dstack([rgb * alpha[..., None], alpha])
    small = Image.fromarray((pre * 255).astype(np.uint8), "RGBA").resize((W, H), Image.BOX)
    p = np.asarray(small).astype(np.float32) / 255
    al = p[..., 3:4]
    col = np.where(al > 1e-3, p[..., :3] / np.maximum(al, 1e-3), 0)
    al = np.clip(al * 1.6, 0, 1)                          # thin lines survive the reduction
    al = np.where(al < 0.18, 0, al)
    orn = Image.fromarray((np.dstack([np.clip(col, 0, 1), al]) * 255).astype(np.uint8), "RGBA")
    # base: the background's own light and shade (mottling, vignette) as greyscale, ornaments filled from around them
    lum = a @ np.array([0.299, 0.587, 0.114], np.float32)
    w = 1 - alpha
    bl = np.asarray(Image.fromarray((lum * w * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
    bw = np.asarray(Image.fromarray((w * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
    fill = np.where(bw > 1e-3, bl / np.maximum(bw, 1e-3), lum)
    base = lum * w + fill * (1 - w)
    q = np.percentile(base, [3, 97])
    base = 0.62 + 0.38 * np.clip((base - q[0]) / max(1e-3, q[1] - q[0]), 0, 1)
    b = Image.fromarray((base * 255).astype(np.uint8)).resize((W, H), Image.BOX)
    bv = np.asarray(b)
    base_img = Image.fromarray(np.dstack([bv, bv, bv, np.full_like(bv, 255)]), "RGBA")
    return orn, base_img


# ---------------------------------------------------------------- emblems for the two new covers (greyscale, tinted in game)
def heart(d, cx, cy, s, ang, fill, sharp=False):
    pts = []
    for i in range(48):
        t = i / 48 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        if sharp:
            y = y - 4 * max(0.0, -math.cos(t)) ** 6            # pulls the lobes into points: a thorned leaf
        x, y = x / 17 * s, (y / 17 + 0.3) * s
        c, sn = math.cos(ang), math.sin(ang)
        pts.append((cx + x * c - y * sn, cy + x * sn + y * c))
    d.polygon(pts, fill=fill)


def emblem_black_magic(size=32):
    S = size * 8
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2
    for k in range(5):
        a = -math.pi / 2 + k * 2 * math.pi / 5
        heart(d, c + math.cos(a) * S * 0.2, c + math.sin(a) * S * 0.2, S * 0.21, a + math.pi / 2, 255, sharp=True)
    for k in range(5):                                          # dark seams
        a = -math.pi / 2 + (k + 0.5) * 2 * math.pi / 5
        d.line([c, c, c + math.cos(a) * S * 0.45, c + math.sin(a) * S * 0.45], fill=0, width=S // 22)
    d.ellipse([c - S * 0.06, c - S * 0.06, c + S * 0.06, c + S * 0.06], fill=150)
    return finish(im, size)


def emblem_god_tier(size=32):
    S = size * 8
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2
    for i in range(16):                                         # radiant rays behind
        a = i * math.pi / 8
        r1, r2 = S * 0.3, S * (0.48 if i % 2 == 0 else 0.4)
        w = 0.07
        d.polygon([(c + math.cos(a - w) * r1, c + math.sin(a - w) * r1), (c + math.cos(a) * r2, c + math.sin(a) * r2),
                   (c + math.cos(a + w) * r1, c + math.sin(a + w) * r1)], fill=170)
    for k in range(4):
        a = -math.pi / 2 + k * math.pi / 2
        heart(d, c + math.cos(a) * S * 0.15, c + math.sin(a) * S * 0.15, S * 0.17, a + math.pi / 2, 255)
    d.ellipse([c - S * 0.05, c - S * 0.05, c + S * 0.05, c + S * 0.05], fill=200)
    return finish(im, size)


def finish(mask, size):
    m = mask.resize((size, size), Image.LANCZOS)
    a = np.asarray(m).astype(np.float32)
    alpha = np.where(a > 40, 255, 0).astype(np.uint8)
    v = np.clip(a * 1.1, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([v, v, v, alpha]), "RGBA")


def main():
    os.makedirs(OUT, exist_ok=True)
    tiles = []
    for name in SETS:
        o, base = layers(name)
        o.save(os.path.join(OUT, "cover_art_" + name + ".png"), optimize=True)
        base.save(os.path.join(OUT, "cover_base_" + name + ".png"), optimize=True)
        tiles.append((name, o, base))
    emblem_black_magic().save(os.path.join(OUT, "emblem_black_magic.png"), optimize=True)
    emblem_god_tier().save(os.path.join(OUT, "emblem_god_tier.png"), optimize=True)

    # preview: each ornament over a mid-red leather (how a Flame book would wear it) and over its source
    Z = 3
    os.makedirs(PREVIEW, exist_ok=True)
    sheet = Image.new("RGB", (len(tiles) * (W * Z + 6) + 6, 2 * (H * Z + 6) + 6), (24, 24, 30))
    for i, (name, o, base) in enumerate(tiles):
        src = Image.open(os.path.join(SRC, name + ".jpg")).convert("RGBA").resize((W * Z, H * Z))
        bv = np.asarray(base).astype(np.float32)[..., :3] / 255 * np.array([170, 42, 34], np.float32)   # a Flame book's cover tint
        leather = Image.fromarray(np.dstack([bv, np.full(bv.shape[:2], 255.0)]).astype(np.uint8), "RGBA").resize((W * Z, H * Z), Image.NEAREST)
        leather.alpha_composite(o.resize((W * Z, H * Z), Image.NEAREST))
        x = 6 + i * (W * Z + 6)
        sheet.paste(src.convert("RGB"), (x, 6))
        sheet.paste(leather.convert("RGB"), (x, H * Z + 12))
    sheet.save(os.path.join(PREVIEW, "preview.png"))
    print("wrote", len(tiles), "cover art textures + 2 emblems")


if __name__ == "__main__":
    main()
