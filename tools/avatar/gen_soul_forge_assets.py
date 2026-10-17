#!/usr/bin/env python3
"""Why: the Soul Forge (the block that starts the Avatar ritual) and its screen need pictures, and every one of them is drawn here from code so none can be
lost and every run is byte-identical. Pillow only. It writes:

    assets/nusmp/textures/block/soul_forge_base.png   16x16  the obsidian body with faint violet veins (pillar and plinth)
    assets/nusmp/textures/block/soul_forge_trim.png   16x16  riveted gold trim (rims, studs, the basin's side)
    assets/nusmp/textures/block/soul_forge_top.png    16x16  the basin's top: a glowing rune circle with a hexagram (emissive in the model)
    assets/nusmp/textures/block/soul_forge_gem.png    16x16  the floating soul gem (faceted, glowing; emissive in the model)
    assets/nusmp/textures/item/soul_forge.png         16x16  the inventory icon (the block in the hand is the 3D model; the sprite stays the icon)
    assets/nusmp/textures/gui/soulforge/ring.png      64x64  a white ring (tinted at draw time): frames, the category buttons, the slots
    assets/nusmp/textures/gui/soulforge/disc.png      64x64  a white soft disc (tinted): the dark medallion behind an icon
    assets/nusmp/textures/gui/soulforge/slot.png      64x64  a white ring with eight notches: an empty skill or aptitude slot
    assets/nusmp/textures/gui/soulforge/cat_<identity|appearance|traits|skills|aptitudes>.png  32x32 white glyphs (tinted): the five category buttons

    python3 -B tools/avatar/gen_soul_forge_assets.py                 # write everything
    python3 -B tools/avatar/gen_soul_forge_assets.py --check         # a second render must give byte-identical files
    python3 -B tools/avatar/gen_soul_forge_assets.py --sheet out.png # a contact sheet (x8) of every picture, to look at
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import forge_art as A  # noqa: E402
from PIL import Image, ImageDraw, ImageFilter  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp", "textures")

OBS = [(20, 13, 31), (27, 18, 42), (34, 24, 52), (15, 10, 24)]
VEIN = [(86, 52, 140), (118, 78, 182)]
GOLD = [(232, 196, 104), (200, 160, 70), (150, 114, 46), (96, 72, 28)]
VIOLET = [(154, 124, 255), (190, 160, 255), (235, 225, 255)]
CYAN = (92, 216, 255)


def noise_base(seed, palette):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), palette[rnd.randrange(len(palette))] + (255,))
    return im


def frame(im, col, dark):
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel(p, col + (255,))
        if 1 < i < 14:
            for p in ((i, 1), (i, 14), (1, i), (14, i)):
                im.putpixel(p, dark + (255,))


def block_base():
    im = noise_base(11, OBS)
    rnd = random.Random(12)
    for _ in range(9):                                   # faint violet veins: short diagonal runs
        x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for k in range(rnd.randrange(2, 5)):
            if 0 < x + k < 15 and 0 < y + k < 15:
                im.putpixel((x + k, y + k), VEIN[rnd.randrange(2)] + (255,))
    frame(im, OBS[3], OBS[0])
    return im


def block_trim():
    rnd = random.Random(21)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = GOLD[1] if (y // 4) % 2 == 0 else GOLD[2]
            im.putpixel((x, y), (GOLD[0] if rnd.random() < 0.12 else base) + (255,))
    for x in range(16):
        im.putpixel((x, 0), GOLD[0] + (255,))
        im.putpixel((x, 15), GOLD[3] + (255,))
    for p in ((2, 4), (13, 4), (2, 11), (13, 11)):
        im.putpixel(p, GOLD[0] + (255,))
        im.putpixel((p[0] + 1, p[1] + 1), GOLD[3] + (255,))
    return im


def _line(px, x0, y0, x1, y1, col):
    """Bresenham on the 16x16 sheet, only inside the frame (1..14)."""
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        if 1 <= x0 <= 14 and 1 <= y0 <= 14:
            px[x0, y0] = col + (255,)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy; x0 += sx
        if e2 <= dx:
            err += dx; y0 += sy


def block_top():
    im = noise_base(31, [(15, 10, 25), (17, 11, 28), (19, 13, 31)])
    frame(im, GOLD[1], OBS[0])
    cx = cy = 7.5
    px = im.load()
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - cx, y - cy)
            if d < 7.0:                                   # a faint glow inside the circle
                g = int(10 * (1 - d / 7.0))
                r, gg, b, a = px[x, y]
                px[x, y] = (min(255, r + g // 2), min(255, gg + g // 3), min(255, b + g), 255)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if abs(d - 6.5) < 0.5 and 1 <= x <= 14 and 1 <= y <= 14:
                px[x, y] = (122, 98, 215, 255)
    r = 4.4                                               # a diamond inside the ring, and a dot on the ring at each compass point
    tips = [(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)]
    for i in range(4):
        a, b = tips[i], tips[(i + 1) % 4]
        _line(px, a[0], a[1], b[0], b[1], (176, 150, 252))
    for (x, y) in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
        px[x, y] = (205, 190, 255, 255)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = VIOLET[2] + (255,)
    return im


def block_gem():
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            # diagonal facets from deep violet to near white
            f = ((x + y) % 8) / 7.0
            f2 = (abs(x - 7.5) + abs(y - 7.5)) / 15.0
            t = max(0.0, min(1.0, 0.25 + 0.55 * (1 - f2) + 0.2 * (1 - abs(f - 0.5) * 2)))
            c = A.mix((88, 56, 190), (240, 232, 255), t)
            px[x, y] = c + (255,)
    for i in range(16):
        px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = (60, 36, 140, 255)
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        px[x, y] = (255, 255, 255, 255)
    return im


def item_icon():
    """The block seen from the front, 16x16: plinth, pillar, basin with a rune glow, a floating gem."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()

    def rect(x0, y0, x1, y1, col):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                px[xx, yy] = col + (255,)

    rect(1, 13, 14, 14, OBS[1]); rect(1, 14, 14, 14, OBS[3]); rect(1, 13, 14, 13, GOLD[1])
    rect(3, 11, 12, 12, OBS[2]); rect(3, 11, 12, 11, GOLD[2])
    rect(5, 7, 10, 10, OBS[0]); rect(5, 7, 5, 10, OBS[3]); rect(10, 7, 10, 10, OBS[2])
    rect(3, 6, 12, 7, GOLD[1]); rect(3, 6, 12, 6, GOLD[0]); rect(3, 7, 12, 7, GOLD[2])
    for x in (4, 11):
        px[x, 5] = GOLD[0] + (255,)
    for (x, y, c) in ((7, 9, VIOLET[0]), (8, 9, VIOLET[0]), (7, 8, VIOLET[1]), (8, 8, VIOLET[1])):
        px[x, y] = c + (255,)
    glow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    gp = glow.load()
    for (x, y, c) in ((7, 2, VIOLET[1]), (8, 2, VIOLET[1]), (6, 3, VIOLET[0]), (7, 3, VIOLET[2]), (8, 3, VIOLET[2]), (9, 3, VIOLET[0]), (7, 4, VIOLET[0]),
                      (8, 4, VIOLET[0]), (7, 1, VIOLET[0]), (8, 1, VIOLET[0])):
        gp[x, y] = c + (255,)
    return Image.alpha_composite(im, glow)


# ============================================================================================ gui
def ui_ring():
    c = A.Canvas(64)
    c.ring(16, 16, 15.2, 1.6, (255, 255, 255))
    return c.image()


def ui_disc():
    c = A.Canvas(64)
    c.circle(16, 16, 15.4, (255, 255, 255, 255))
    return c.image()


def ui_slot():
    c = A.Canvas(64)
    c.ring(16, 16, 15.2, 1.2, (255, 255, 255))
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        c.line([(16 + 12.6 * math.cos(a), 16 + 12.6 * math.sin(a)), (16 + 14.6 * math.cos(a), 16 + 14.6 * math.sin(a))], 1.0, (255, 255, 255), False)
    c.ring(16, 16, 12.0, 0.5, (255, 255, 255, 120))
    return c.image()


W = (255, 255, 255)


def cat_identity():
    c = A.Canvas(32)
    c.circle(16, 11.5, 5.2, W)
    c.poly(c.spline([(6.0, 26), (6.8, 20.4), (10.4, 17.6), (16, 16.8), (21.6, 17.6), (25.2, 20.4), (26.0, 26)], n=8), W)
    return c.image()


def cat_appearance():
    c = A.Canvas(32)
    c.ellipse(16, 13.5, 8.2, 10.4, W)
    c.ellipse(16, 13.5, 6.0, 8.2, (0, 0, 0, 0))
    c.poly([(14.6, 23.0), (17.4, 23.0), (18.2, 29.0), (13.8, 29.0)], W)
    c.line([(12.0, 9.0), (14.0, 6.8)], 1.1, W)
    c.line([(11.0, 12.5), (12.4, 11.0)], 1.0, W)
    return c.image()


def cat_traits():
    c = A.Canvas(32)
    pts = [(16, 27.0), (6.2, 17.0), (5.6, 11.4), (8.6, 7.4), (12.6, 7.0), (16, 10.4), (19.4, 7.0), (23.4, 7.4), (26.4, 11.4), (25.8, 17.0)]
    c.poly(c.spline(pts, n=8), W)
    c.erase_poly([(11.0, 11.4), (13.0, 9.6), (13.6, 11.2), (12.0, 13.2)])
    return c.image()


def cat_skills():
    c = A.Canvas(32)
    pts = []
    for k in range(10):
        r = 12.4 if k % 2 == 0 else 5.2
        a = math.radians(k * 36 - 90)
        pts.append((16 + r * math.cos(a), 16.8 + r * math.sin(a)))
    c.poly(pts, W)
    return c.image()


def cat_aptitudes():
    c = A.Canvas(32)
    c.ring(16, 16, 11.6, 1.7, W)
    for tri in (0, 180):
        pts = [(16 + 9.0 * math.cos(math.radians(k * 120 + tri - 90)), 16 + 9.0 * math.sin(math.radians(k * 120 + tri - 90))) for k in range(3)]
        pts.append(pts[0])
        c.line(pts, 1.5, W, False)
    c.circle(16, 16, 2.0, W)
    return c.image()


OUT = {
    "block/soul_forge_base.png": block_base, "block/soul_forge_trim.png": block_trim, "block/soul_forge_top.png": block_top, "block/soul_forge_gem.png": block_gem,
    "item/soul_forge.png": item_icon, "gui/soulforge/ring.png": ui_ring, "gui/soulforge/disc.png": ui_disc, "gui/soulforge/slot.png": ui_slot,
    "gui/soulforge/cat_identity.png": cat_identity, "gui/soulforge/cat_appearance.png": cat_appearance, "gui/soulforge/cat_traits.png": cat_traits,
    "gui/soulforge/cat_skills.png": cat_skills, "gui/soulforge/cat_aptitudes.png": cat_aptitudes,
}


def sheet(path):
    items = [(n, f()) for n, f in OUT.items()]
    cell, pad = 8 * 16, 16
    cols = 5
    rows = (len(items) + cols - 1) // cols
    img = Image.new("RGBA", (cols * (cell + pad) + pad, rows * (cell + pad + 12) + pad), (24, 30, 52, 255))
    d = ImageDraw.Draw(img)
    for i, (n, im) in enumerate(items):
        x, y = pad + (i % cols) * (cell + pad), pad + (i // cols) * (cell + pad + 12)
        k = max(1, cell // im.size[0])
        big = im.resize((im.size[0] * k, im.size[1] * k), Image.NEAREST)
        # white glyph pictures are drawn tinted so they can be seen on the sheet
        if n.startswith("gui/"):
            big = _tinted(big, (92, 216, 255))
        img.alpha_composite(big, (x, y))
        d.text((x, y + cell + 2), n.split("/")[-1], fill=(200, 215, 240, 255))
    img.save(path)
    print("sheet", path)


def _tinted(im, rgb):
    out = Image.new("RGBA", im.size, rgb + (0,))
    out.putalpha(im.getchannel("A"))
    return out


def main():
    check = "--check" in sys.argv
    diffs = 0
    for rel, fn in OUT.items():
        im = fn()
        path = os.path.join(ASSETS, *rel.split("/"))
        if check:
            if not os.path.isfile(path) or Image.open(path).convert("RGBA").tobytes() != im.tobytes():
                print("DIFFERS", rel); diffs += 1
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            im.save(path)
    if "--sheet" in sys.argv:
        sheet(sys.argv[sys.argv.index("--sheet") + 1])
    print("%d pictures, %s" % (len(OUT), "%d differences" % diffs if check else "written"))
    return 1 if diffs else 0


if __name__ == "__main__":
    sys.exit(main())
