"""Textures and models of the Engravement Codex item: a grimoire in the grimoire design system, its cover showing a rune ring cut by a blade.

    python3 -B tools/engraving_codex/gen_engraving_codex_assets.py
        -> src/main/resources/assets/nusmp/textures/item/grimoire_book/emblem_engraving.png      (32x32, white, tinted by the emblem colour)
           src/main/resources/assets/nusmp/textures/item/grimoire_book/cover_base_engraving.png  (64x80, greyscale shading, tinted by the cover colour)
           src/main/resources/assets/nusmp/textures/item/grimoire_book/cover_art_engraving.png   (64x80, the ornament in its own gold / cyan)
           src/main/resources/assets/nusmp/textures/item/engraving_codex.png                     (16x16 inventory icon)
           src/main/resources/assets/nusmp/models/item/engraving_codex.json                      (loader nusmp:grimoire, the grimoire's display block)
           src/main/resources/assets/nusmp/models/item/engraving_codex_icon.json                 (flat 16x16 variant)
           build/engraving_codex/textures.png                                                    (preview of the textures, not shipped)

The three grimoire_book textures follow the conventions of tools/gen_grimoire_book_textures.py and tools/gen_cover_art_textures.py (and of the Compatibility Codex,
tools/compat/gen_compat_codex_assets.py): the emblem is white on transparent (the item colour handler tints it), the base is a smooth greyscale (tinted to the cover colour), the
art keeps its own colours and glows while the book is held. The cover, trim, emblem and glow colours are the pure engraving/codex/EngravingCodexLook.java. Deterministic, PIL + numpy only.
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
PREVIEW = os.path.join(ROOT, "build", "engraving_codex")
SS = 8                                   # supersampling

GOLD = (232, 196, 104)
PALE = (248, 232, 176)
CYAN = (92, 216, 255)
STEEL = (196, 212, 232)


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT), im.size)


def down(mask, ss=SS):
    h, w = mask.shape
    return mask.reshape(h // ss, ss, w // ss, ss).mean(axis=(1, 3))


def canvas(size):
    return Image.new("L", (size * SS, size * SS), 0)


def to_mask(im):
    return np.asarray(im).astype(np.float32) / 255


def dilate(m, px):
    im = Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(px * 2 + 1))
    return np.asarray(im).astype(np.float32) / 255


# ------------------------------------------------------------------------------------------------ the emblem: a rune ring cut by a blade
def sword_mask(size, cx, top, bottom, blade_w, guard_w, guard_y, guard_t):
    """A sword pointing up: tapered blade, crossguard, grip and pommel (float mask 0..1, supersampled)."""
    im = canvas(size)
    d = ImageDraw.Draw(im)
    S = SS
    tip = top
    shoulder = guard_y - 0.4
    d.polygon([(cx * S, tip * S), ((cx + blade_w / 2) * S, (tip + 3.4) * S), ((cx + blade_w / 2) * S, shoulder * S),
               ((cx - blade_w / 2) * S, shoulder * S), ((cx - blade_w / 2) * S, (tip + 3.4) * S)], fill=255)
    d.rectangle([(cx - guard_w / 2) * S, guard_y * S, (cx + guard_w / 2) * S, (guard_y + guard_t) * S], fill=255)
    d.rectangle([(cx - 0.75) * S, (guard_y + guard_t) * S, (cx + 0.75) * S, (bottom - 1.2) * S], fill=255)
    d.ellipse([(cx - 1.5) * S, (bottom - 2.6) * S, (cx + 1.5) * S, (bottom + 0.2) * S], fill=255)
    return to_mask(im)


def ring_mask(size, cx, cy, r, w, ticks=8, tick_len=2.4, tick_w=1.4):
    """A ring with rune ticks (short bars pointing out of it), the 'engraving' ring."""
    im = canvas(size)
    d = ImageDraw.Draw(im)
    S = SS
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=255)
    d.ellipse([(cx - r + w) * S, (cy - r + w) * S, (cx + r - w) * S, (cy + r - w) * S], fill=0)
    for k in range(ticks):
        a = math.tau * k / ticks + math.pi / ticks
        x0, y0 = cx + math.cos(a) * (r - 0.4), cy + math.sin(a) * (r - 0.4)
        x1, y1 = cx + math.cos(a) * (r + tick_len), cy + math.sin(a) * (r + tick_len)
        d.line([(x0 * S, y0 * S), (x1 * S, y1 * S)], fill=255, width=max(1, int(tick_w * S)))
    return to_mask(im)


def emblem():
    size = 32
    ring = ring_mask(size, 16, 16.5, 10.2, 2.0, ticks=8, tick_len=2.6, tick_w=1.5)
    sword = sword_mask(size, 16, 1.0, 31.0, 3.6, 13.0, 21.5, 2.2)
    halo = dilate(sword, int(0.9 * SS))
    m = np.clip(ring * (1 - halo) + sword, 0, 1)
    a8 = (np.clip(down(m), 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(np.dstack([np.full_like(a8, 255)] * 3 + [a8]), "RGBA")


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


def rune_grooves(W, H, ss=4):
    """A faint lattice of engraved diamonds with a short bar at each crossing: the cover's background pattern. 0..1."""
    yy, xx = np.mgrid[0:H * ss, 0:W * ss].astype(np.float32)
    x, y = xx / ss, yy / ss
    pitch = 11.0
    u = (x + y) / pitch
    v = (x - y) / pitch
    du = np.abs(u - np.round(u)) * pitch / math.sqrt(2)
    dv = np.abs(v - np.round(v)) * pitch / math.sqrt(2)
    lines = np.clip(1 - np.minimum(du, dv) / 0.42, 0, 1)
    cross = np.clip(1 - np.hypot(du, dv) / 1.3, 0, 1)
    out = np.maximum(lines * 0.55, cross)
    return out.reshape(H, ss, W, ss).mean(axis=(1, 3))


def cover_base():
    W, H = 64, 80
    n = noise(W, H, 16, 41) * 0.6 + noise(W, H, 8, 42) * 0.4
    v = 226 + (n - 0.5) * 34
    v = v - rune_grooves(W, H) * 40                              # the engraving is a darker groove in the leather
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    vig = np.clip(1 - (((xx - W / 2) / (W * 0.75)) ** 2 + ((yy - H / 2) / (H * 0.75)) ** 2), 0, 1)
    v = np.clip(v * (0.9 + 0.1 * vig), 0, 255).astype(np.uint8)
    a = np.full_like(v, 255)
    return Image.fromarray(np.dstack([v, v, v, a]), "RGBA")


def layer(draw_fn, W=64, H=80):
    im = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(im), SS)
    return im


def ring(d, S, cx, cy, r, w, col):
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=col + (255,), width=max(1, int(w * S)))


def blade(d, S, cx, cy, length, ang, col, w=1.1):
    """A small blade (a thin lozenge) centred at (cx, cy) pointing along ang (radians)."""
    dx, dy = math.cos(ang), math.sin(ang)
    px, py = -dy, dx
    tip = (cx + dx * length / 2, cy + dy * length / 2)
    base = (cx - dx * length / 2, cy - dy * length / 2)
    mid = (cx + dx * length * 0.15, cy + dy * length * 0.15)
    d.polygon([(tip[0] * S, tip[1] * S), ((mid[0] + px * w) * S, (mid[1] + py * w) * S), (base[0] * S, base[1] * S),
               ((mid[0] - px * w) * S, (mid[1] - py * w) * S)], fill=col + (255,))
    gx, gy = cx - dx * length * 0.28, cy - dy * length * 0.28
    d.line([((gx + px * w * 2.2) * S, (gy + py * w * 2.2) * S), ((gx - px * w * 2.2) * S, (gy - py * w * 2.2) * S)], fill=GOLD + (255,), width=int(0.9 * S))


def gem(d, S, cx, cy, r, col=CYAN):
    d.polygon([(cx * S, (cy - r) * S), ((cx + r * 0.75) * S, cy * S), (cx * S, (cy + r) * S), ((cx - r * 0.75) * S, cy * S)], fill=col + (255,))


def cover_art():
    W, H = 64, 80

    def draw(d, S):
        # two frame lines, the inner one is the "inner line" the plan expects from the ornament
        d.rectangle([4.2 * S, 4.2 * S, (W - 4.2) * S, (H - 4.2) * S], outline=GOLD + (255,), width=int(0.9 * S))
        d.rectangle([7.2 * S, 7.2 * S, (W - 7.2) * S, (H - 7.2) * S], outline=PALE + (230,), width=int(0.6 * S))
        # corners: a crossed pair of blades over a small rune ring, with a cyan gem
        for (sx, sy) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            cx0 = 4.2 + sx * 0 if sx > 0 else W - 4.2
            cy0 = 4.2 if sy > 0 else H - 4.2
            cx, cy = cx0 + sx * 9.6, cy0 + sy * 9.6
            ring(d, S, cx, cy, 4.4, 0.8, PALE)
            blade(d, S, cx, cy, 10.4, math.radians(45 if sx * sy > 0 else -45), STEEL)
            blade(d, S, cx, cy, 10.4, math.radians(135 if sx * sy > 0 else -135), STEEL)
            gem(d, S, cx, cy, 1.5)
        # the rows of rune marks along both sides: short bars of different lengths, like a line of engraved letters
        for side in (-1, 1):
            cx = 10.6 if side < 0 else W - 10.6
            for i in range(6):
                cy = 25 + i * 5.6
                ln = (3.2, 2.0, 3.6, 1.6, 2.8, 2.2)[i]
                d.line([(cx * S, (cy - ln / 2) * S), (cx * S, (cy + ln / 2) * S)], fill=(GOLD if i % 2 == 0 else PALE) + (255,), width=int(1.0 * S))
                d.line([((cx - 1.5) * S, (cy - ln / 4) * S), ((cx + 1.5) * S, (cy - ln / 4) * S)], fill=(GOLD if i % 2 == 0 else PALE) + (255,), width=int(0.7 * S))
        # top and bottom: a pair of rune rings and a gem
        for cy0, s in ((11.0, 1), (H - 11.0, -1)):
            ring(d, S, W / 2 - 3.6, cy0, 3.0, 0.9, GOLD)
            ring(d, S, W / 2 + 3.6, cy0, 3.0, 0.9, PALE)
            gem(d, S, W / 2, cy0 + s * 7.4, 1.8)
        # a thin halo round the medallion with eight rune ticks (the plan puts the medallion box in the middle)
        ring(d, S, W / 2, H / 2, 17.5, 0.8, GOLD)
        ring(d, S, W / 2, H / 2, 15.2, 0.5, PALE)
        for k in range(8):
            a = math.tau * k / 8 + math.pi / 8
            x0, y0 = W / 2 + math.cos(a) * 17.5, H / 2 + math.sin(a) * 17.5
            x1, y1 = W / 2 + math.cos(a) * 20.3, H / 2 + math.sin(a) * 20.3
            d.line([(x0 * S, y0 * S), (x1 * S, y1 * S)], fill=GOLD + (255,), width=int(0.8 * S))

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
    """A closed book seen from the front: steel-blue leather, gold frame, a gold rune ring cut by a pale blade, a cream page edge. 16x16."""
    P = {"o": (14, 22, 38, 255), "c": (29, 51, 80, 255), "d": (20, 36, 60, 255), "l": (52, 84, 124, 255), "g": (232, 196, 104, 255),
         "G": (172, 134, 56, 255), "q": (214, 202, 164, 255), "y": (92, 216, 255, 255), "p": (248, 232, 176, 255), "s": (214, 226, 242, 255)}
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
    # the emblem on the cover (the 8x8 field at x 4..11, y 4..11): a gold rune ring (an octagon), a pale blade through it with a crossguard and a cyan gem in the hilt
    emblem_rows = [          # a silver rune ring (so it does not merge with the gold frame), a gold blade, a gold crossguard, a cyan gem
        ".ssss.",
        "s.gp.s",
        "s.gp.s",
        "sggggs",
        "s.gy.s",
        ".ssss.",
    ]
    for dy, row in enumerate(emblem_rows):
        for dx, ch in enumerate(row):
            if ch != ".":
                px[5 + dx, 5 + dy] = P[ch]
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
    save(em, os.path.join(TEX, "emblem_engraving.png"))
    save(base, os.path.join(TEX, "cover_base_engraving.png"))
    save(art, os.path.join(TEX, "cover_art_engraving.png"))
    save(ic, os.path.join(ITEM_TEX, "engraving_codex.png"))
    write_json(os.path.join(MODELS, "engraving_codex.json"), {
        "loader": "nusmp:grimoire",
        "_comment": "Engravement Codex: the grimoire geometry (GrimoireBookPlan) with the fixed look engraving/codex/EngravingCodexLook (cover ENGRAVING, a rune ring cut by a blade). "
                    "Tint layers: 0 cover, 1 trim, 2 emblem (client/engraving/EngravingCodexClient). Textures: textures/item/grimoire_book/*_engraving.png "
                    "(tools/engraving_codex/gen_engraving_codex_assets.py).",
        "display": DISPLAY})
    write_json(os.path.join(MODELS, "engraving_codex_icon.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/engraving_codex"}})

    # preview sheet: the textures on a neutral ground, and tinted the way the colour handler does
    os.makedirs(PREVIEW, exist_ok=True)
    cover = np.array([0x1D, 0x33, 0x50], np.float32) / 255
    trim = np.array([0xE8, 0xC4, 0x68], np.float32) / 255
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
    a = np.asarray(base).astype(np.float32) / 255
    comp = Image.fromarray((np.dstack([a[..., :3] * cover, a[..., 3]]) * 255).astype(np.uint8), "RGBA")
    comp.alpha_composite(art)
    sheet.alpha_composite(comp.resize((64 * k, 80 * k), Image.NEAREST), (x, 10))
    x += 64 * k + 10
    sheet.alpha_composite(ic.resize((16 * k, 16 * k), Image.NEAREST), (x, 10))
    sheet.convert("RGB").save(os.path.join(PREVIEW, "textures.png"))
    print("wrote build/engraving_codex/textures.png")


if __name__ == "__main__":
    main()
