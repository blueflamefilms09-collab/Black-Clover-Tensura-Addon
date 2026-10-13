"""djinn_icons: the 2D art of the Djinn system (called by gen_djinn_art.py): the Seal sprites, the body-art skin overlay, the Lesser Summon bust
and the silhouette mask. Pure PIL, deterministic."""
import json
import math
import os

from PIL import Image, ImageDraw


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# ================================================================ Seal sprites (16x16)
GLYPH = {
    "HEAT": ["...#...", "..##...", "..###..", ".#####.", ".#####.", ".#####.", "..###.."],
    "LIGHTNING": ["...###.", "..###..", ".###...", "..####.", "...##..", "..##...", ".#....."],
    "WATER": ["...#...", "..###..", ".#####.", "#######", "#######", ".#####.", "..###.."],
    "WIND": [".####..", "#...#..", "..###..", ".#..#..", ".#.##..", "..#....", "...###."],
    "LIFE": ["....###", "..#####", ".######", ".#####.", "#####..", "##.....", "#......"],
    "STRENGTH": ["...#...", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#.", "...#..."],
    "SOUND": [".....##", "....###", "....#.#", "....#..", "..###..", ".####..", "..##..."],
    "LIGHT": ["#..#..#", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#.", "#..#..#"],
}


def make_icon(dj, assets):
    c, g = rgb(dj["color"]), rgb(dj["glow"])
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 7.4:
                lit = 1.0 - ((x - 4.0) * 0.5 + (y - 3.0) * 0.55) / 12.0                   # lit from the top left
                base = mix(mix(c, (12, 12, 20), 0.55), mix(c, (255, 255, 255), 0.25), max(0.0, min(1.0, lit)))
                if d > 6.2:
                    base = mix(base, (14, 12, 22), 0.75)                                    # the dark rim
                elif d > 5.4:
                    base = mix(g, c, 0.35 + 0.1 * ((x + y) % 2))                            # a gilded ring
                px[x, y] = base + (255,)
    for i, (x, y) in enumerate(((7, 1), (14, 7), (7, 14), (1, 7))):                         # four rune ticks
        px[x, y] = g + (255,)
        px[x + 1 if x < 8 else x, y] = g + (255,)
    gl = GLYPH[dj["affinity"]]
    for yy, row in enumerate(gl):
        for xx, ch in enumerate(row):
            if ch == "#":
                x, y = 4 + xx, 4 + yy
                px[x, y] = mix(g, (255, 255, 255), 0.55 if yy < 3 else 0.2) + (255,)
    for (x, y) in ((4, 4), (11, 4)):                                                        # a glint
        px[x, y] = (255, 255, 255, 255)
    tex = os.path.join(assets, "textures", "item")
    mod = os.path.join(assets, "models", "item")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(mod, exist_ok=True)
    name = "djinn_seal_" + dj["id"]
    im.save(os.path.join(tex, name + ".png"))
    with open(os.path.join(mod, name + ".json"), "w", newline="\n") as fh:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + name}}, fh, indent=2)
        fh.write("\n")
    return name


# ================================================================ the skin layout of the player model (classic and slim, 64x64)
BOXES = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm_r": (40, 16, 4, 12, 4), "leg_r": (0, 16, 4, 12, 4),
         "arm_l": (32, 48, 4, 12, 4), "leg_l": (16, 48, 4, 12, 4)}


def faces(box):
    u, v, w, h, d = BOXES[box]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def make_silhouette(assets):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for b in BOXES:
        for (x, y, w, h) in faces(b).values():
            d.rectangle([x, y, x + w - 1, y + h - 1], fill=(255, 255, 255, 255))
    out = os.path.join(assets, "textures", "entity", "djinn")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, "djinn_silhouette.png"))


# ================================================================ the body-art: glowing rune lines on the skin (additive second pass)
def make_skin(dj, assets):
    g, c = rgb(dj["glow"]), rgb(dj["color"])
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    G, C, W = g + (255,), mix(c, g, 0.35) + (255,), (255, 255, 255, 255)

    def put(x, y, col):
        if 0 <= x < 64 and 0 <= y < 64:
            px[x, y] = col

    def vline(x, y0, y1, col=G, dash=0):
        for y in range(y0, y1 + 1):
            if dash and (y - y0) % dash == dash - 1:
                continue
            put(x, y, col)

    def hline(x0, x1, y, col=G):
        for x in range(x0, x1 + 1):
            put(x, y, col)

    f = {b: faces(b) for b in BOXES}
    # head: glowing eyes, a brow mark and cheek lines
    hx, hy = f["head"]["front"][0], f["head"]["front"][1]
    for x in (1, 2, 5, 6):
        put(hx + x, hy + 4, W if x in (2, 5) else G)
    put(hx + 3, hy + 1, G), put(hx + 4, hy + 1, G), put(hx + 3, hy, C), put(hx + 4, hy, C)
    put(hx + 1, hy + 6, C), put(hx + 6, hy + 6, C), put(hx + 2, hy + 5, C), put(hx + 5, hy + 5, C)
    for side in ("right", "left", "back"):
        x, y, w, h = f["head"][side]
        hline(x, x + w - 1, y + 3, C)
        vline(x + w // 2, y, y + 2, C)
    x, y, w, h = f["head"]["top"]
    vline(x + w // 2, y, y + h - 1, C)
    # body: a sigil on the chest, a spine on the back
    x, y, w, h = f["body"]["front"]
    cx, cy = x + w // 2, y + 5
    for a in range(0, 360, 30):
        put(cx + int(round(2.4 * math.cos(math.radians(a)))), cy + int(round(2.4 * math.sin(math.radians(a)))), G)
    put(cx, cy, W), put(cx - 1, cy, C), put(cx, cy - 1, C)
    vline(cx, y, y + 1, G), vline(cx, cy + 3, y + h - 1, G, 3)
    hline(x, x + 1, y + 2, C), hline(x + w - 2, x + w - 1, y + 2, C)
    hline(x + 1, x + w - 2, y + 9, C)
    x, y, w, h = f["body"]["back"]
    vline(x + w // 2 - 1, y, y + h - 1, G), vline(x + w // 2, y, y + h - 1, C)
    for t in range(1, h, 3):
        hline(x + w // 2 - 3, x + w // 2 - 2, y + t, C), hline(x + w // 2 + 1, x + w // 2 + 2, y + t, C)
    for side in ("right", "left"):
        x, y, w, h = f["body"][side]
        vline(x + w // 2, y + 1, y + h - 2, C, 3)
    # arms and legs: stripes with rings at the joints, drawn on columns that exist on the classic and the slim skin alike
    for box, cols in (("arm_r", {"front": (1,), "back": (1, 2), "right": (1, 2), "left": (1, 2)}), ("arm_l", {"front": (1,), "back": (1, 2), "right": (1, 2), "left": (1, 2)}),
                      ("leg_r", {"front": (1, 2), "back": (1, 2), "right": (1, 2), "left": (1, 2)}), ("leg_l", {"front": (1, 2), "back": (1, 2), "right": (1, 2), "left": (1, 2)})):
        for face, cs in cols.items():
            x, y, w, h = f[box][face]
            for cidx in cs:
                vline(x + cidx, y + 1, y + h - 2, G if face == "front" else C, 4 if face != "front" else 0)
            ring_rows = (2, 8) if box.startswith("arm") else (3, 8)
            for r in ring_rows:
                hline(x, x + min(w, 3) - 1, y + r, C)
    out = os.path.join(assets, "textures", "entity", "djinn")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, dj["id"] + "_rune.png"))


# ================================================================ the Lesser Summon's bust (32x32, drawn as a translucent billboard)
def make_bust(dj, assets):
    c, g = rgb(dj["color"]), rgb(dj["glow"])
    S = 4
    im = Image.new("RGBA", (32 * S, 32 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    body = mix(c, (255, 255, 255), 0.15)
    # shoulders and chest fading into mist
    d.polygon([(4 * S, 32 * S), (7 * S, 22 * S), (12 * S, 19 * S), (20 * S, 19 * S), (25 * S, 22 * S), (28 * S, 32 * S)], fill=mix(body, (20, 20, 40), 0.35) + (205,))
    d.ellipse([10 * S, 7 * S, 22 * S, 21 * S], fill=mix(body, (255, 255, 255), 0.2) + (230,))                      # the head
    d.polygon([(11 * S, 17 * S), (21 * S, 17 * S), (19 * S, 22 * S), (13 * S, 22 * S)], fill=mix(body, (20, 20, 40), 0.25) + (225,))
    # horns by style
    hs = dj["look"]["horns"]
    horn = mix(g, (255, 255, 255), 0.25) + (240,)
    for sgn in (-1, 1):
        cx = 16 * S
        def X(v):
            return cx + sgn * v * S
        if hs == "CRESCENT":
            d.polygon([(X(5), 9 * S), (X(9), 4 * S), (X(10), 1 * S), (X(8), 3 * S), (X(6), 7 * S)], fill=horn)
        elif hs == "RAM":
            d.polygon([(X(6), 9 * S), (X(10), 8 * S), (X(12), 12 * S), (X(10), 15 * S), (X(9), 12 * S), (X(8), 10 * S)], fill=horn)
        elif hs == "STAG":
            d.polygon([(X(5), 9 * S), (X(7), 2 * S), (X(8), 2 * S), (X(6), 9 * S)], fill=horn)
            d.polygon([(X(7), 6 * S), (X(11), 4 * S), (X(11), 5 * S), (X(7), 8 * S)], fill=horn)
        elif hs == "SPIKE":
            d.polygon([(X(6), 9 * S), (X(11), 5 * S), (X(8), 10 * S)], fill=horn)
        elif hs == "CROWN" and sgn == 1:
            for k in (-6, -3, 0, 3, 6):
                d.polygon([(cx + (k - 1.4) * S, 9 * S), (cx + k * S, (3 + abs(k) * 0.35) * S), (cx + (k + 1.4) * S, 9 * S)], fill=horn)
    if dj["look"]["ears"]:
        for sgn in (-1, 1):
            d.polygon([(16 * S + sgn * 6 * S, 13 * S), (16 * S + sgn * 11 * S, 11 * S), (16 * S + sgn * 6 * S, 16 * S)], fill=body + (220,))
    # glowing eyes, a brow gem, a rune on the chest
    for sgn in (-1, 1):
        d.rectangle([16 * S + sgn * 3 * S - S, 13 * S, 16 * S + sgn * 3 * S + S, 14 * S + 2], fill=(255, 255, 255, 255))
        d.rectangle([16 * S + sgn * 3 * S - S - 2, 13 * S - 2, 16 * S + sgn * 3 * S + S + 2, 14 * S + 4], outline=g + (200,))
    d.polygon([(16 * S, 9 * S), (17 * S, 11 * S), (16 * S, 12 * S), (15 * S, 11 * S)], fill=g + (255,))
    d.ellipse([14 * S, 24 * S, 18 * S, 28 * S], outline=g + (255,), width=2)
    im = im.resize((32, 32), Image.LANCZOS)
    # a soft fade into the mist at the bottom
    px = im.load()
    for y in range(32):
        for x in range(32):
            r, gg, b, a = px[x, y]
            fade = 1.0 if y < 22 else max(0.0, 1.0 - (y - 22) / 10.0)
            px[x, y] = (r, gg, b, int(a * fade))
    out = os.path.join(assets, "textures", "entity", "djinn")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, dj["id"] + "_bust.png"))
