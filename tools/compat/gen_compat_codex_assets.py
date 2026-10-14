"""Textures and models of the Compatibility Codex item (1.34.0): a grimoire in the grimoire design system, its cover showing two interlocking rings.

    python tools/compat/gen_compat_codex_assets.py
        -> src/main/resources/assets/nusmp/textures/item/grimoire_book/emblem_compat.png      (32x32, white, tinted by the emblem colour)
           src/main/resources/assets/nusmp/textures/item/grimoire_book/cover_base_compat.png  (64x80, greyscale shading, tinted by the cover colour)
           src/main/resources/assets/nusmp/textures/item/grimoire_book/cover_art_compat.png   (64x80, the ornament in its own gold / cyan)
           src/main/resources/assets/nusmp/textures/item/compat_codex.png                     (16x16 inventory icon)
           src/main/resources/assets/nusmp/models/item/compat_codex.json                      (loader nusmp:grimoire, the grimoire's display block)
           src/main/resources/assets/nusmp/models/item/compat_codex_icon.json                 (flat 16x16 variant)
           build/compat_codex/textures.png                                                    (preview of the textures, not shipped)

The three grimoire_book textures follow the conventions of tools/gen_grimoire_book_textures.py and tools/gen_cover_art_textures.py: the emblem is
white on transparent (the item colour handler tints it), the base is a smooth greyscale (tinted to the cover colour), the art keeps its own
colours and glows while the book is held. The cover, trim, emblem and glow colours are the pure compat/CompatCodexLook.java. Deterministic, PIL + numpy only.
Preview the 3D book with tools/compat/preview_compat_codex.py.
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(RES, "textures", "item", "grimoire_book")
ITEM_TEX = os.path.join(RES, "textures", "item")
MODELS = os.path.join(RES, "models", "item")
PREVIEW = os.path.join(ROOT, "build", "compat_codex")
SS = 8                                   # supersampling

GOLD = (226, 185, 90)
PALE = (248, 232, 176)
CYAN = (92, 216, 255)


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT), im.size)


def ring_mask(size, cx, cy, r, w, ss=SS):
    """Anti-aliased ring (float 0..1) of centre (cx, cy), outer radius r and thickness w, in output pixels."""
    S = size * ss
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    d = np.sqrt((xx / ss - cx) ** 2 + (yy / ss - cy) ** 2)
    return ((d <= r) & (d >= r - w)).astype(np.float32)


def down(mask, ss=SS):
    h, w = mask.shape
    return mask.reshape(h // ss, ss, w // ss, ss).mean(axis=(1, 3))


# ------------------------------------------------------------------------------------------------ the emblem: two interlocking rings
def emblem():
    size = 32
    S = size * SS
    cy = 16.0
    ca, cb = (11.6, cy), (20.4, cy)
    r, w = 8.6, 2.5
    A = ring_mask(size, *ca, r, w)
    B = ring_mask(size, *cb, r, w)

    def dil(m, px):
        im = Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(px * 2 + 1))
        return np.asarray(im).astype(np.float32) / 255

    gap = int(0.9 * SS) | 1
    yy = np.mgrid[0:S, 0:S][0].astype(np.float32) / SS
    upper, lower = (yy < cy).astype(np.float32), (yy >= cy).astype(np.float32)
    # weave: at the top crossing ring B passes over A, at the bottom crossing A passes over B (a thin transparent halo shows the over / under)
    a_cut = A * (1 - dil(B, gap // 2) * upper) + A * dil(B, gap // 2) * upper * 0
    a_cut = A * (1 - upper * np.clip(dil(B, gap // 2) - B, 0, 1))
    b_cut = B * (1 - lower * np.clip(dil(A, gap // 2) - A, 0, 1))
    m = np.clip(a_cut + b_cut, 0, 1)
    alpha = down(m)
    a8 = (np.clip(alpha, 0, 1) * 255).astype(np.uint8)
    im = np.dstack([np.full_like(a8, 255)] * 3 + [a8])
    return Image.fromarray(im, "RGBA")


# ------------------------------------------------------------------------------------------------ the cover
def noise(w, h, cell, seed):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    g = rng.random((gh + 2, gw + 2)).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def lattice(W, H, ss=4):
    """A faint lattice of interlocking circles (every circle meets its neighbours): the cover's background pattern. 0..1."""
    S = ss
    yy, xx = np.mgrid[0:H * S, 0:W * S].astype(np.float32)
    x, y = xx / S, yy / S
    pitch, r = 10.0, 6.5
    out = np.zeros_like(x)
    for row in range(-1, int(H / (pitch * 0.866)) + 2):
        cy = row * pitch * 0.866
        off = (pitch / 2) if row % 2 else 0
        for col in range(-1, int(W / pitch) + 2):
            cx = col * pitch + off
            d = np.abs(np.sqrt((x - cx) ** 2 + (y - cy) ** 2) - r)
            out = np.maximum(out, np.clip(1 - d / 0.45, 0, 1))
    return out.reshape(H, S, W, S).mean(axis=(1, 3))


def cover_base():
    W, H = 64, 80
    n = noise(W, H, 16, 31) * 0.6 + noise(W, H, 8, 32) * 0.4
    v = 226 + (n - 0.5) * 34
    lat = lattice(W, H)
    v = v - lat * 42                                              # the lattice is a darker groove in the leather
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    vig = np.clip(1 - (((xx - W / 2) / (W * 0.75)) ** 2 + ((yy - H / 2) / (H * 0.75)) ** 2), 0, 1)
    v = v * (0.9 + 0.1 * vig)
    v = np.clip(v, 0, 255).astype(np.uint8)
    a = np.full_like(v, 255)
    return Image.fromarray(np.dstack([v, v, v, a]), "RGBA")


def layer(draw_fn, W=64, H=80):
    """Draw coloured strokes on an 8x supersampled RGBA canvas and reduce it."""
    S = SS
    im = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_fn(d, S)
    return im


def ring(d, S, cx, cy, r, w, col, rx=None, ry=None):
    rx = r if rx is None else rx
    ry = r if ry is None else ry
    d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], outline=col + (255,), width=max(1, int(w * S)))


def cover_art():
    W, H = 64, 80

    def draw(d, S):
        # two frame lines, the inner one is the "inner line" the plan expects from the ornament
        d.rectangle([4.2 * S, 4.2 * S, (W - 4.2) * S, (H - 4.2) * S], outline=GOLD + (255,), width=int(0.9 * S))
        d.rectangle([7.2 * S, 7.2 * S, (W - 7.2) * S, (H - 7.2) * S], outline=PALE + (230,), width=int(0.6 * S))
        # corners: a pair of interlocking rings turned along the diagonal, with a cyan gem where they cross
        for (sx, sy) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            cx0 = 4.2 if sx > 0 else W - 4.2
            cy0 = 4.2 if sy > 0 else H - 4.2
            for k, (ox, oy) in enumerate(((7.4, 12.6), (12.6, 7.4))):
                ring(d, S, cx0 + sx * ox, cy0 + sy * oy, 5.2, 1.0, GOLD if k == 0 else PALE)
            gx, gy = cx0 + sx * 10, cy0 + sy * 10
            d.ellipse([(gx - 1.0) * S, (gy - 1.0) * S, (gx + 1.0) * S, (gy + 1.0) * S], fill=CYAN + (255,))
        # the chain of linked rings along both sides
        for side in (-1, 1):
            cx = 10.6 if side < 0 else W - 10.6
            for i in range(5):
                cy = 27 + i * 6.5
                if i % 2 == 0:
                    ring(d, S, cx, cy, 3, 0.8, GOLD, rx=1.7, ry=3.2)
                else:
                    ring(d, S, cx, cy, 3, 0.8, PALE, rx=1.7, ry=3.2)
        # top and bottom: a pair of small linked rings and a gem
        for cy0, s in ((11.0, 1), (H - 11.0, -1)):
            ring(d, S, W / 2 - 3.1, cy0, 3.4, 0.9, GOLD)
            ring(d, S, W / 2 + 3.1, cy0, 3.4, 0.9, PALE)
            d.polygon([((W / 2) * S, (cy0 + s * 6.4) * S), ((W / 2 + 1.5) * S, (cy0 + s * 8.0) * S), ((W / 2) * S, (cy0 + s * 9.6) * S),
                       ((W / 2 - 1.5) * S, (cy0 + s * 8.0) * S)], fill=CYAN + (255,))
        # a thin ring halo round the medallion (the plan puts the medallion box in the middle)
        ring(d, S, W / 2, H / 2, 17.5, 0.8, GOLD)
        ring(d, S, W / 2, H / 2, 15.2, 0.5, PALE)

    big = layer(draw)
    f = np.asarray(big).astype(np.float32) / 255
    pre = np.dstack([f[..., :3] * f[..., 3:4], f[..., 3:4]])                  # premultiplied, so transparent black does not darken the lines
    h, w = pre.shape[:2]
    pre = pre.reshape(h // SS, SS, w // SS, SS, 4).mean(axis=(1, 3))
    al = np.clip(pre[..., 3:4] * 1.6, 0, 1)                                  # thin lines survive the reduction
    rgb = np.clip(pre[..., :3] / np.maximum(pre[..., 3:4], 1e-3), 0, 1)
    al = np.where(al < 0.18, 0, al)
    out = np.dstack([rgb * (al > 0), al])
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------ the 16x16 icon
def icon():
    """A closed book seen from the front: violet leather, gold frame, two interlocking gold rings, a cream page edge. 16x16."""
    P = {"o": (22, 14, 36, 255), "c": (53, 41, 94, 255), "d": (38, 28, 70, 255), "l": (84, 68, 138, 255), "g": (226, 185, 90, 255),
         "G": (172, 128, 52, 255), "q": (214, 198, 156, 255), "y": (92, 216, 255, 255), "p": (248, 232, 176, 255)}
    rows = [
        "................",
        "..oooooooooooo..",
        ".oddllllllllllo.",
        ".odgggggggggggo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgccccccccgGo.",
        ".odgggggggggGGo.",
        ".odqqqqqqqqqqqo.",
        "..oooooooooooo..",
        "................",
    ]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = P[ch]
    # two interlocking rings (outer radius 3.2, 1 px thick) in the 8x8 field, one gold and one pale gold, the left one over the right at the top
    def ring_px(cx, cy):
        out = set()
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if 2.0 <= d <= 3.35:
                    out.add((x, y))
        return out
    A, B = ring_px(6.3, 8.0), ring_px(9.7, 8.0)
    for (x, y) in B:
        px[x, y] = P["p"]
    for (x, y) in A:
        if (x, y) in B and y >= 8:
            continue                                                   # B passes over A at the bottom crossing
        px[x, y] = P["g"]
    px[8, 4] = P["y"]
    px[8, 11] = P["y"]
    return im


DISPLAY = {   # the grimoire's own display block (assets/nusmp/models/item/grimoire.json), so hand, gui, ground and frame match the grimoires
    "gui": {"rotation": [20, 35, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1.0, 1.0, 1.0]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1.5], "scale": [0.7, 0.7, 0.7]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1.5], "scale": [0.7, 0.7, 0.7]},
    "firstperson_righthand": {"rotation": [12, -22, 0], "translation": [1.5, 2.5, -2], "scale": [0.8, 0.8, 0.8]},
    "firstperson_lefthand": {"rotation": [12, 22, 0], "translation": [-1.5, 2.5, -2], "scale": [0.8, 0.8, 0.8]},
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(path, ROOT))


def main():
    em, base, art, ic = emblem(), cover_base(), cover_art(), icon()
    save(em, os.path.join(TEX, "emblem_compat.png"))
    save(base, os.path.join(TEX, "cover_base_compat.png"))
    save(art, os.path.join(TEX, "cover_art_compat.png"))
    save(ic, os.path.join(ITEM_TEX, "compat_codex.png"))
    write_json(os.path.join(MODELS, "compat_codex.json"), {
        "loader": "nusmp:grimoire",
        "_comment": "Compatibility Codex: the grimoire geometry (GrimoireBookPlan) with the fixed look compat/CompatCodexLook (cover COMPAT, two linked rings). "
                    "Tint layers: 0 cover, 1 trim, 2 emblem (client/compat/CompatCodexClient). Textures: textures/item/grimoire_book/*_compat.png "
                    "(tools/compat/gen_compat_codex_assets.py).",
        "display": DISPLAY})
    write_json(os.path.join(MODELS, "compat_codex_icon.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/compat_codex"}})

    # preview sheet: the textures on a neutral ground, and tinted the way the colour handler does
    os.makedirs(PREVIEW, exist_ok=True)
    cover = np.array([0x35, 0x29, 0x5E], np.float32) / 255
    trim = np.array([0xE2, 0xB9, 0x5A], np.float32) / 255
    k = 5
    sheet = Image.new("RGBA", (64 * k * 3 + 32 * k + 16 * k + 80, 80 * k + 20), (58, 58, 66, 255))
    x = 10
    for im, tint in ((base, cover), (art, None), (em, trim)):
        a = np.asarray(im).astype(np.float32) / 255
        if tint is not None:
            a = np.dstack([a[..., :3] * tint, a[..., 3]])
        big = Image.fromarray((a * 255).astype(np.uint8), "RGBA").resize((im.width * k, im.height * k), Image.NEAREST)
        sheet.alpha_composite(big, (x, 10))
        x += big.width + 10
    # cover = base tinted + art overlay
    a = np.asarray(base).astype(np.float32) / 255
    comp = Image.fromarray((np.dstack([a[..., :3] * cover, a[..., 3]]) * 255).astype(np.uint8), "RGBA")
    comp.alpha_composite(art)
    sheet.alpha_composite(comp.resize((64 * k, 80 * k), Image.NEAREST), (x, 10))
    x += 64 * k + 10
    sheet.alpha_composite(ic.resize((16 * k, 16 * k), Image.NEAREST), (x, 10))
    sheet.convert("RGB").save(os.path.join(PREVIEW, "textures.png"))
    print("wrote build/compat_codex/textures.png")


if __name__ == "__main__":
    main()
