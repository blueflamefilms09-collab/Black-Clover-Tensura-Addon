"""Why: Shishou's own GUIs (the Stomach screen, the HUD and the skill icon of the Tensura Ultimate skill) use one small texture atlas in an "Akashic" look: a dark blue-violet star field, fine prismatic
line-work, a glowing cyan frame (the cyan of the blade's fuller lines), a sword emblem and glowing gauges. The game draws the panels and gauges with GuiGraphics fills and gradients (nothing allocated per
frame) and blits the pieces of this atlas. This script makes the atlas, deterministically, so it can be redone:

    python3 tools/shishou/gen_shishou_gui.py          # writes the atlas and the skill icon

Atlas layout (256 x 256, regions are mirrored by client/shishou/AkashicTheme.java and by shishou_gui_preview.py):
    STAR     (  0,  0) 64x64    tileable star field
    EMBLEM   ( 64,  0) 32x32    the sword emblem
    MOTE     ( 96,  0)  8x8     a soft light mote
    CORNERS  (104,  0) 4 x 16x16  top-left, top-right, bottom-left, bottom-right ornaments
    PRISM    (  0, 64) 128x2    a prismatic line (cyan, violet, gold)
    SLOTROW  (  0, 96) 162x18   nine slot cells
    GAUGE    (  0,128) 64x8     the gauge glass (a dark tube with a fine rim), drawn stretched
Also writes textures/skill/shishou.png (32x32, the icon of the skill: black iron frame lit in cyan, as the other Ultimate skills).
"""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT_ATLAS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "shishou", "akashic.png")
OUT_ICON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "shishou.png")

CYAN = (92, 216, 255)
CYAN_HI = (127, 235, 255)
VIOLET = (155, 107, 255)
GOLD = (255, 215, 106)
INK_TOP = (6, 8, 24)
INK_BOT = (26, 15, 58)

REGIONS = {
    "star": (0, 0, 64, 64), "emblem": (64, 0, 32, 32), "mote": (96, 0, 8, 8),
    "corner_tl": (104, 0, 16, 16), "corner_tr": (120, 0, 16, 16), "corner_bl": (136, 0, 16, 16), "corner_br": (152, 0, 16, 16),
    "prism": (0, 64, 128, 2), "slotrow": (0, 96, 162, 18), "gauge": (0, 128, 64, 8),
}


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def star_field():
    """A 64x64 tileable deep-space tile: a blue-violet vertical gradient with a soft nebula and sparse stars."""
    rnd = random.Random(0x5348)
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    for y in range(64):
        for x in range(64):
            t = y / 63.0
            n = 0.5 + 0.5 * math.sin(x * 0.19 + math.sin(y * 0.11) * 2.0) * math.sin(y * 0.13 + 1.3)
            base = mix(INK_TOP, INK_BOT, 0.25 + 0.55 * t)
            glow = mix(base, (40, 28, 96), 0.35 * n)
            px[x, y] = glow + (255,)
    d = ImageDraw.Draw(img)
    for i in range(46):
        x, y = rnd.randrange(64), rnd.randrange(64)
        tone = rnd.choice([(255, 255, 255), CYAN_HI, (200, 180, 255), (255, 240, 200)])
        a = rnd.choice([110, 150, 190, 230])
        for ox in (-64, 0, 64):                               # wrap, so the tile has no seam
            for oy in (-64, 0, 64):
                d.point((x + ox, y + oy), fill=tone + (a,))
        if i % 9 == 0:                                           # a few sparkles with a cross
            for ox in (-64, 0, 64):
                for oy in (-64, 0, 64):
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        d.point((x + ox + dx, y + oy + dy), fill=tone + (70,))
    # an aurora band, and a constellation drawn with fine prismatic lines between a few stars (inside the tile, so it tiles without a seam)
    band = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    bp = band.load()
    for y in range(64):
        for x in range(64):
            k = math.exp(-((x + y * 0.6 - 52) / 12.0) ** 2)
            bp[x, y] = (110, 90, 255, int(46 * k)) if (x * 3 + y) % 64 < 40 else (60, 200, 255, int(34 * k))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    pts = [(9, 51), (19, 40), (31, 44), (42, 30), (53, 36), (57, 18)]
    for i in range(len(pts) - 1):
        col = (CYAN, VIOLET, GOLD, CYAN_HI, VIOLET)[i]
        d.line([pts[i], pts[i + 1]], fill=col + (70,), width=1)
    for (x, y) in pts:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            d.point((x + dx, y + dy), fill=(200, 235, 255, 70))
        d.point((x, y), fill=(255, 255, 255, 255))
    return img


def emblem(size=32, ss=8):
    """The sword emblem: a diagonal blade with a cyan fuller, a gold guard, a dark grip, in a soft cyan glow."""
    S = size * ss
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    base = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(base)
    c = S / 2.0
    ang = math.radians(-45)
    ux, uy = math.cos(ang), math.sin(ang)          # along the blade, to the top right
    vx, vy = -uy, ux

    def pt(a, b):
        return (c + ux * a + vx * b, c + uy * a + vy * b)

    L = S * 0.44
    # blade
    blade = [pt(-L * 0.18, -S * 0.045), pt(L * 0.92, -S * 0.045), pt(L * 1.02, 0), pt(L * 0.92, S * 0.045), pt(-L * 0.18, S * 0.045)]
    d.polygon(blade, fill=(212, 226, 240, 255))
    d.polygon([pt(-L * 0.18, 0), pt(L * 0.92, -S * 0.045), pt(L * 1.02, 0), pt(L * 0.92, 0)], fill=(150, 170, 196, 255))
    d.line([pt(-L * 0.10, 0), pt(L * 0.93, 0)], fill=CYAN_HI + (255,), width=max(2, int(ss * 0.9)))          # the cyan fuller
    d.line([pt(-L * 0.10, 0), pt(L * 0.93, 0)], fill=(255, 255, 255, 200), width=max(1, int(ss * 0.35)))
    # guard
    d.polygon([pt(-L * 0.26, -S * 0.17), pt(-L * 0.20, -S * 0.17), pt(-L * 0.17, 0), pt(-L * 0.20, S * 0.17), pt(-L * 0.26, S * 0.17), pt(-L * 0.30, 0)], fill=GOLD + (255,))
    d.line([pt(-L * 0.23, -S * 0.17), pt(-L * 0.23, S * 0.17)], fill=(255, 245, 200, 255), width=max(1, ss // 2))
    # grip and pommel
    d.line([pt(-L * 0.28, 0), pt(-L * 0.62, 0)], fill=(40, 30, 70, 255), width=int(S * 0.055))
    for k in range(3):
        a = -L * (0.36 + 0.09 * k)
        d.line([pt(a, -S * 0.028), pt(a, S * 0.028)], fill=(120, 90, 200, 255), width=max(1, ss // 2))
    p = pt(-L * 0.68, 0)
    d.ellipse((p[0] - S * 0.05, p[1] - S * 0.05, p[0] + S * 0.05, p[1] + S * 0.05), fill=GOLD + (255,))
    p2 = pt(-L * 0.68, 0)
    d.ellipse((p2[0] - S * 0.022, p2[1] - S * 0.022, p2[0] + S * 0.022, p2[1] + S * 0.022), fill=CYAN_HI + (255,))
    # glow
    gd = ImageDraw.Draw(glow)
    gd.line([pt(-L * 0.6, 0), pt(L * 1.0, 0)], fill=CYAN + (170,), width=int(S * 0.16))
    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.06))
    out = Image.alpha_composite(glow, base)
    return out.resize((size, size), Image.LANCZOS)


def mote():
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    for y in range(8):
        for x in range(8):
            r = math.hypot(x - 3.5, y - 3.5) / 4.0
            a = max(0.0, 1.0 - r) ** 2
            px[x, y] = (210, 245, 255, int(255 * a))
    return img


def corner(flip_x, flip_y):
    S = 16 * 8
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    w = 8
    d.line([(w, S - 1), (w, w), (S - 1, w)], fill=CYAN + (255,), width=w)
    d.line([(w * 3, S - 1), (w * 3, w * 3), (S - 1, w * 3)], fill=VIOLET + (150,), width=w // 2)
    d.polygon([(w * 3, w * 3 - 2 * w), (w * 3 + 2 * w, w * 3), (w * 3, w * 3 + 2 * w), (w * 3 - 2 * w, w * 3)], fill=GOLD + (255,))
    d.ellipse((w - 4, w - 4, w + 4, w + 4), fill=(255, 255, 255, 255))
    glow = img.filter(ImageFilter.GaussianBlur(10))
    img = Image.alpha_composite(glow, img)
    img = img.resize((16, 16), Image.LANCZOS)
    if flip_x:
        img = img.transpose(Image.FLIP_LEFT_RIGHT)
    if flip_y:
        img = img.transpose(Image.FLIP_TOP_BOTTOM)
    return img


def prism():
    img = Image.new("RGBA", (128, 2), (0, 0, 0, 0))
    px = img.load()
    stops = [(0.0, CYAN), (0.35, CYAN_HI), (0.55, VIOLET), (0.8, GOLD), (1.0, CYAN)]
    for x in range(128):
        t = x / 127.0
        for i in range(len(stops) - 1):
            if stops[i][0] <= t <= stops[i + 1][0]:
                u = (t - stops[i][0]) / (stops[i + 1][0] - stops[i][0])
                col = mix(stops[i][1], stops[i + 1][1], u)
                break
        edge = min(t, 1 - t) * 8
        a = int(255 * min(1.0, 0.35 + edge))
        px[x, 0] = col + (a,)
        px[x, 1] = mix(col, (255, 255, 255), 0.25) + (int(a * 0.35),)
    return img


def slot_row():
    img = Image.new("RGBA", (162, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(7)
    for i in range(9):
        x = i * 18
        d.rectangle((x, 0, x + 17, 17), fill=(CYAN[0] * 2 // 3, CYAN[1] * 2 // 3, CYAN[2], 78))
        d.rectangle((x + 1, 1, x + 16, 16), fill=(5, 7, 20, 255))
        d.rectangle((x + 1, 1, x + 16, 1), fill=(2, 3, 10, 255))
        d.rectangle((x + 1, 16, x + 16, 16), fill=(30, 36, 86, 255))
        d.line([(x + 1, 1), (x + 1, 16)], fill=(2, 3, 10, 255))
        d.line([(x + 16, 1), (x + 16, 16)], fill=(24, 30, 72, 255))
        for k in range(2):
            d.point((x + 2 + rnd.randrange(13), 2 + rnd.randrange(13)), fill=(150, 190, 255, 90))
    return img


def gauge():
    img = Image.new("RGBA", (64, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 63, 7), fill=(CYAN[0], CYAN[1], CYAN[2], 110))
    d.rectangle((1, 1, 62, 6), fill=(3, 5, 16, 255))
    d.line([(1, 1), (62, 1)], fill=(0, 0, 0, 255))
    d.line([(1, 6), (62, 6)], fill=(22, 28, 70, 255))
    return img


def build_atlas():
    atlas = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    atlas.paste(star_field(), REGIONS["star"][:2])
    atlas.paste(emblem(), REGIONS["emblem"][:2])
    atlas.paste(mote(), REGIONS["mote"][:2])
    atlas.paste(corner(False, False), REGIONS["corner_tl"][:2])
    atlas.paste(corner(True, False), REGIONS["corner_tr"][:2])
    atlas.paste(corner(False, True), REGIONS["corner_bl"][:2])
    atlas.paste(corner(True, True), REGIONS["corner_br"][:2])
    atlas.paste(prism(), REGIONS["prism"][:2])
    atlas.paste(slot_row(), REGIONS["slotrow"][:2])
    atlas.paste(gauge(), REGIONS["gauge"][:2])
    return atlas


def skill_icon():
    """32x32: black iron frame lit in cyan (the Ultimate frame), the star field inside, the sword emblem on it."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
    field = star_field().crop((0, 0, 32, 32))
    img.paste(field, (0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 31, 31), outline=(14, 14, 22, 255))
    d.rectangle((1, 1, 30, 30), outline=(46, 50, 70, 255))
    d.rectangle((2, 2, 29, 29), outline=CYAN + (255,))
    d.rectangle((3, 3, 28, 28), outline=(CYAN[0] // 3, CYAN[1] // 3, CYAN[2] // 3, 255))
    em = emblem(26, 8)
    img.alpha_composite(em, (3, 3))
    for (x, y) in ((1, 1), (30, 1), (1, 30), (30, 30)):
        d.point((x, y), fill=GOLD + (255,))
    return img


def main():
    os.makedirs(os.path.dirname(OUT_ATLAS), exist_ok=True)
    build_atlas().save(OUT_ATLAS)
    skill_icon().save(OUT_ICON)
    print("wrote", os.path.relpath(OUT_ATLAS, ROOT), "and", os.path.relpath(OUT_ICON, ROOT))


if __name__ == "__main__":
    main()
