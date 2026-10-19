"""Fairy Tail skills: the 32x32 skill icons (textures/skill/<id>.png) and the 16x16 skill book item (textures/item/fairy_tail_skill_book.png + its model).

    python3 -B tools/gen_fairy_tail_icons.py

Every icon fills its frame (a gradient in the element's colours, a bevelled border) and carries a symbol drawn at four times the size and scaled down: a flame
(Fire Dragon Slayer), a flame under a crown (Fire Dragon King), a snowflake (Ice-Make), a wind swirl (Sky Dragon Slayer), a bolt (Lightning Dragon Slayer), a gear
(Iron Dragon Slayer), a shattering burst (Crash) and a drop (Water Magic). Ids and order are fairytail/FtLine.java.
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp")
S = 128  # supersampled canvas


def gradient(top, bottom):
    img = Image.new("RGBA", (S, S))
    px = img.load()
    for y in range(S):
        t = y / (S - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,)
        for x in range(S):
            px[x, y] = c
    return img


def frame(img, edge):
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, S - 1, S - 1), outline=edge, width=8)
    d.rectangle((8, 8, S - 9, S - 9), outline=tuple(int(v * 0.6) for v in edge), width=3)


def flame(d, cx, cy, h, outer, inner, core):
    def poly(scale, col, dy=0):
        pts = [(cx, cy - h * 0.55 * scale + dy), (cx + h * 0.20 * scale, cy - h * 0.2 * scale + dy), (cx + h * 0.34 * scale, cy + h * 0.15 * scale + dy),
               (cx + h * 0.26 * scale, cy + h * 0.42 * scale + dy), (cx, cy + h * 0.5 * scale + dy), (cx - h * 0.26 * scale, cy + h * 0.42 * scale + dy),
               (cx - h * 0.34 * scale, cy + h * 0.15 * scale + dy), (cx - h * 0.12 * scale, cy - h * 0.05 * scale + dy)]
        d.polygon(pts, fill=col)
    poly(1.0, outer)
    poly(0.68, inner, dy=h * 0.08)
    poly(0.36, core, dy=h * 0.17)


def snowflake(d, cx, cy, r, col, w):
    for k in range(6):
        a = k * math.pi / 3
        ex, ey = cx + math.cos(a) * r, cy + math.sin(a) * r
        d.line((cx, cy, ex, ey), fill=col, width=w)
        for f in (0.55, 0.8):
            bx, by = cx + math.cos(a) * r * f, cy + math.sin(a) * r * f
            for s in (-1, 1):
                b = a + s * 0.9
                d.line((bx, by, bx + math.cos(b) * r * 0.25, by + math.sin(b) * r * 0.25), fill=col, width=max(2, w - 2))


def swirl(d, cx, cy, r, col, w):
    for k, rr in enumerate((r, r * 0.68, r * 0.38)):
        start = 200 + k * 40
        d.arc((cx - rr, cy - rr, cx + rr, cy + rr), start, start + 250, fill=col, width=w)
    d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=col)


def bolt(d, cx, cy, h, col, edge):
    pts = [(cx + h * 0.12, cy - h * 0.5), (cx - h * 0.28, cy + h * 0.04), (cx - h * 0.02, cy + h * 0.04), (cx - h * 0.16, cy + h * 0.5),
           (cx + h * 0.3, cy - h * 0.1), (cx + h * 0.04, cy - h * 0.1)]
    d.polygon(pts, fill=col, outline=edge)


def gear(d, cx, cy, r, col, hole):
    for k in range(8):
        a = k * math.pi / 4
        d.polygon([(cx + math.cos(a - 0.2) * r, cy + math.sin(a - 0.2) * r), (cx + math.cos(a + 0.2) * r, cy + math.sin(a + 0.2) * r),
                   (cx + math.cos(a + 0.2) * r * 1.28, cy + math.sin(a + 0.2) * r * 1.28), (cx + math.cos(a - 0.2) * r * 1.28, cy + math.sin(a - 0.2) * r * 1.28)], fill=col)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=col)
    d.ellipse((cx - r * 0.45, cy - r * 0.45, cx + r * 0.45, cy + r * 0.45), fill=hole)


def burst(d, cx, cy, r, col, dark):
    pts = []
    for k in range(16):
        a = k * math.pi / 8
        rr = r if k % 2 == 0 else r * 0.45
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=col, outline=dark)
    for k in range(5):
        a = k * 1.25 + 0.3
        d.line((cx, cy, cx + math.cos(a) * r * 0.9, cy + math.sin(a) * r * 0.9), fill=dark, width=3)


def drop(d, cx, cy, h, col, hi):
    d.polygon([(cx, cy - h * 0.55), (cx + h * 0.32, cy + h * 0.1), (cx - h * 0.32, cy + h * 0.1)], fill=col)
    d.ellipse((cx - h * 0.32, cy - h * 0.12, cx + h * 0.32, cy + h * 0.52), fill=col)
    d.ellipse((cx - h * 0.16, cy + h * 0.02, cx - h * 0.04, cy + h * 0.24), fill=hi)


def crown(d, cx, cy, w, col):
    pts = [(cx - w, cy + w * 0.35), (cx - w, cy - w * 0.1), (cx - w * 0.5, cy + w * 0.12), (cx, cy - w * 0.4), (cx + w * 0.5, cy + w * 0.12), (cx + w, cy - w * 0.1), (cx + w, cy + w * 0.35)]
    d.polygon(pts, fill=col)


def icon(name, top, bottom, edge, draw):
    img = gradient(top, bottom)
    frame(img, edge)
    d = ImageDraw.Draw(img)
    draw(d)
    out = img.resize((32, 32), Image.LANCZOS).convert("RGBA")
    px = out.load()
    for y in range(32):
        for x in range(32):
            r, g, b, _ = px[x, y]
            px[x, y] = (r, g, b, 255)
    os.makedirs(os.path.join(ROOT, "textures", "skill"), exist_ok=True)
    out.save(os.path.join(ROOT, "textures", "skill", name + ".png"))


def main():
    icon("fire_dragon_slayer", (120, 24, 10), (230, 90, 20), (255, 190, 80), lambda d: flame(d, 64, 68, 84, (255, 130, 30), (255, 200, 60), (255, 250, 200)))
    icon("fire_dragon_king", (36, 8, 12), (150, 24, 20), (255, 200, 90),
         lambda d: (flame(d, 64, 76, 74, (255, 110, 24), (255, 190, 50), (255, 245, 190)), crown(d, 64, 30, 26, (255, 215, 90))))
    icon("ice_make", (20, 60, 110), (120, 190, 240), (220, 245, 255), lambda d: snowflake(d, 64, 64, 46, (240, 252, 255), 7))
    icon("sky_dragon_slayer", (30, 120, 120), (150, 235, 220), (235, 255, 250), lambda d: swirl(d, 64, 64, 44, (255, 255, 255), 8))
    icon("lightning_dragon_slayer", (26, 28, 60), (120, 100, 20), (255, 235, 100), lambda d: bolt(d, 64, 64, 96, (255, 236, 80), (255, 255, 200)))
    icon("iron_dragon_slayer", (50, 54, 62), (150, 158, 170), (220, 226, 236), lambda d: gear(d, 64, 64, 30, (226, 232, 242), (70, 76, 88)))
    icon("crash", (40, 14, 70), (130, 70, 200), (200, 150, 255), lambda d: burst(d, 64, 64, 48, (230, 200, 255), (60, 20, 100)))
    icon("water_magic", (14, 50, 120), (70, 160, 240), (190, 230, 255), lambda d: drop(d, 64, 62, 88, (225, 245, 255), (110, 170, 240)))
    # the skill book item: a 16 x 16 book, blue cover, gold clasp and a gold star
    book = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = book.load()
    cover, dark, gold, page = (40, 70, 140, 255), (20, 36, 80, 255), (240, 200, 70, 255), (240, 232, 205, 255)
    for y in range(2, 14):
        for x in range(3, 13):
            px[x, y] = cover
    for y in range(2, 14):
        px[3, y] = dark
        px[12, y] = dark
    for x in range(3, 13):
        px[x, 2] = dark
        px[x, 13] = dark
    for y in range(3, 13):
        px[11, y] = page
    for (x, y) in ((7, 5), (6, 6), (7, 6), (8, 6), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (6, 9), (8, 9)):
        px[x, y] = gold
    for y in range(3, 13):
        px[4, y] = gold if y in (3, 12) else (30, 56, 112, 255)
    os.makedirs(os.path.join(ROOT, "textures", "item"), exist_ok=True)
    book.save(os.path.join(ROOT, "textures", "item", "fairy_tail_skill_book.png"))
    os.makedirs(os.path.join(ROOT, "models", "item"), exist_ok=True)
    with open(os.path.join(ROOT, "models", "item", "fairy_tail_skill_book.json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/fairy_tail_skill_book"}}, f)


if __name__ == "__main__":
    main()
