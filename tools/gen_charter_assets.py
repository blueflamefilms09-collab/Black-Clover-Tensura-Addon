#!/usr/bin/env python3
"""
Art of the seven Charter Grimoires (charter/CharterTable.java): for every boss a 32x32 texture of its 3D book model, a 16x16 flat icon, the 3D
item model (plain model elements: a cover, a back, a spine, the pages, gold corners, a raised sigil plate, a ribbon and two spine bands) and the
icon model. Cover and trim colours are read from CharterTable.java so the art cannot drift from the table. Idempotent.

    python3 tools/gen_charter_assets.py            # writes the assets
    python3 tools/gen_charter_assets.py --preview  # also writes a contact sheet to the given scratch path (see PREVIEW)

Texture layout (32x32, 1 pixel = 1 model unit): front cover (0,0)-(11,15), back cover (11,0)-(22,15), spine (22,0)-(26,15), dark edge
(26,0)-(28,2), gold (28,0)-(30,2), ribbon (30,0)-(32,2), pages (0,16)-(11,32).
"""
import json
import math
import os
import re
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
TABLE = os.path.join(HERE, "..", "src", "main", "java", "com", "newuniverse", "nusmp", "charter", "CharterTable.java")

# ---- the book, in model units (a 16 unit cube space; the book stands upright, front toward +z)
X0, X1 = 2.5, 13.5          # width 11: from the spine to the fore-edge
Y0, Y1 = 0.5, 15.5          # height 15
Z0, Z1 = 6.0, 10.0          # depth 4: back cover 1, pages 2, front cover 1
COVER = 1.0                 # cover thickness
SPINE = 0.4                 # the spine strip left of the covers
PAGE_INSET_X, PAGE_INSET_Y = 1.0, 1.0   # the pages sit this far inside the covers
PLATE = 9                   # the sigil plate is 9x9 units (a 1 unit ring round the 7x7 sigil), centred on the front cover
PLATE_RAISE = 0.25
CORNER = 2.0                # gold corner pieces
CORNER_RAISE = 0.4
BAND_Y = (3.0, 12.0)        # the two gold bands round the spine
BAND_H = 0.7

# ---- sigils, 7x7 ('#' = drawn)
SIGILS = {
    "dante": ["#######", "#.....#", "#.###.#", "#.#.#.#", "#.###.#", "#.....#", "#######"],   # a gravity well: rings round a point
    "vanica": ["...#...", "..###..", "..###..", ".#####.", ".#####.", ".#####.", "..###.."],
    "zenon": ["...#...", "..#.#..", ".#...#.", "#..#..#", ".#...#.", "..#.#..", "...#..."],
    "lucius": [".#####.", "#..#..#", "#..#..#", "#..##.#", "#.....#", "#.....#", ".#####."],
    "devil_avatar": ["#.....#", "##...##", ".#####.", ".#.#.#.", ".#####.", "..#.#..", "..###.."],
    "goliath": [".......", "...#...", "..###..", ".#####.", ".##.##.", "#######", "#######"],
    "charybdis_spawn": ["#######", "#.....#", "#.###.#", "#.#.#.#", "#.#...#", "#.#####", "#......"],
}


def parse_table():
    src = open(TABLE).read()
    out = []
    for m in re.finditer(r'add\("(\w+)",\s*"\w+",\s*"[^"]*",\s*"\w+",\s*[\d.]+,\s*0x([0-9A-Fa-f]{6}),\s*0x([0-9A-Fa-f]{6})', src):
        out.append((m.group(1), int(m.group(2), 16), int(m.group(3), 16)))
    assert len(out) == 7, "CharterTable.java should list seven bosses, found %d" % len(out)
    return out


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return mix(c, (0, 0, 0), -f) if f < 0 else mix(c, (255, 255, 255), f)


GOLD = (222, 178, 58)
PAGE = (232, 220, 190)


def put(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,))


def draw_sigil(img, ox, oy, rows, colour, shadow):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch == "#":
                put(img, ox + dx, oy + dy, colour)


def cover_face(w, h, cover, trim, boss, front, metal):
    """A cover: dark edge, a trim line one pixel in, worn leather inside (and the sigil plate on the front)."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dark = shade(cover, -0.45)
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            line = (x in (1, w - 2) and 1 <= y <= h - 2) or (y in (1, h - 2) and 1 <= x <= w - 2)
            if edge:
                c = dark
            elif line:
                c = mix(cover, trim, 0.55)
            else:
                g = 0.10 - 0.20 * (y / (h - 1)) + (0.03 if (x + y) % 2 else -0.03) + 0.04 * (1 - abs(x - (w - 1) / 2) / (w / 2))
                c = shade(cover, g)
            put(img, x, y, c)
    if front:
        # the plate: u 1..9, v 3..11 (9x9): a metal ring round a dark disc, the 7x7 sigil in the trim colour
        for y in range(3, 12):
            for x in range(1, 10):
                ring = x in (1, 9) or y in (3, 11)
                put(img, x, y, metal if ring else shade(cover, -0.55))
        draw_sigil(img, 2, 4, SIGILS[boss], trim, shade(cover, -0.75))
    else:
        for y in range(5, 10):
            for x in range(3, 8):
                if (x, y) in ((5, 5), (4, 6), (6, 6), (3, 7), (5, 7), (7, 7), (4, 8), (6, 8), (5, 9)):
                    put(img, x, y, mix(cover, trim, 0.5))
    return img


def metal_of(boss, trim):
    """The corners, bands and plate ring: gold, except on the gold Lucius book, where they are its dark trim."""
    return trim if boss == "lucius" else GOLD


def build(boss, cover_rgb, trim_rgb):
    cover, trim = rgb(cover_rgb), rgb(trim_rgb)
    metal = metal_of(boss, trim)
    if boss == "lucius":
        trim = shade(cover, 0.35)      # the dark trim cannot carry the sigil on the dark disc: it is drawn in bright gold
    tex = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    tex.paste(cover_face(11, 15, cover, trim, boss, True, metal), (0, 0))
    tex.paste(cover_face(11, 15, cover, trim, boss, False, metal), (11, 0))
    spine = Image.new("RGBA", (4, 15), (0, 0, 0, 0))
    for y in range(15):
        for x in range(4):
            c = shade(cover, -0.30 + (0.06 if x in (1, 2) else 0.0))
            if y in (3, 4, 11, 12):
                c = mix(metal, (60, 40, 10), 0.25 * (1 if y in (4, 12) else 0))
            put(spine, x, y, c)
    tex.paste(spine, (22, 0))
    for y in range(2):
        for x in range(2):
            put(tex, 26 + x, y, shade(cover, -0.5))
            put(tex, 28 + x, y, metal if (x + y) % 2 == 0 else shade(metal, -0.12))
            put(tex, 30 + x, y, mix(trim, cover, 0.15) if (x + y) % 2 == 0 else shade(trim, -0.2))
    for y in range(16):
        for x in range(11):
            c = PAGE if y % 2 == 0 else shade(PAGE, -0.08)
            if x == 10 or y == 15:
                c = shade(PAGE, -0.22)
            put(tex, x, 16 + y, c)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 15):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (1, 14)
            if edge:
                c = shade(cover, -0.5)
            elif x == 12:
                c = PAGE if y % 2 else shade(PAGE, -0.1)
            else:
                c = shade(cover, 0.08 - 0.2 * (y / 14.0) + (0.03 if (x + y) % 2 else -0.03))
            put(icon, x, y, c)
    for (cx, cy) in ((3, 2), (11, 2), (3, 12), (11, 12)):
        for dx in range(2):
            for dy in range(2):
                put(icon, cx + dx, cy + dy, metal)
    for y in range(4, 11):
        for x in range(4, 11):
            ring = x in (4, 10) or y in (4, 10)
            put(icon, x, y, metal if ring else shade(cover, -0.55))
    rows = SIGILS[boss]
    for dy in range(1, 6):
        for dx in range(1, 6):
            if rows[dy][dx] == "#":
                put(icon, 4 + dx, 4 + dy, trim)
    return tex, icon


def model(boss):
    t = "#0"
    f = lambda uv, tex=t: {"uv": uv, "texture": tex}
    dark = [26, 0, 28, 2]
    gold = [28, 0, 30, 2]
    ribbon = [30, 0, 32, 2]
    els = []
    # back cover (north face is the outside)
    els.append({"name": "back_cover", "from": [X0, Y0, Z0], "to": [X1, Y1, Z0 + COVER],
                "faces": {"north": f([11, 0, 22, 15]), "up": f(dark), "down": f(dark), "east": f(dark), "west": f(dark)}})
    # the pages
    px0, px1 = X0 + PAGE_INSET_X, X1 - PAGE_INSET_X + 0.0
    els.append({"name": "pages", "from": [px0, Y0 + PAGE_INSET_Y, Z0 + COVER], "to": [px1, Y1 - PAGE_INSET_Y, Z1 - COVER],
                "faces": {"east": f([0, 16, 2, 29]), "up": f([2, 16, 11, 18]), "down": f([2, 16, 11, 18]),
                          "north": f([0, 16, 9, 29]), "south": f([0, 16, 9, 29])}})
    # front cover
    els.append({"name": "front_cover", "from": [X0, Y0, Z1 - COVER], "to": [X1, Y1, Z1],
                "faces": {"south": f([0, 0, 11, 15]), "up": f(dark), "down": f(dark), "east": f(dark), "west": f(dark)}})
    # spine
    els.append({"name": "spine", "from": [X0 - SPINE, Y0, Z0], "to": [X0, Y1, Z1],
                "faces": {"west": f([22, 0, 26, 15]), "up": f(dark), "down": f(dark), "north": f(dark), "south": f(dark)}})
    # gold bands round the spine
    for i, by in enumerate(BAND_Y):
        els.append({"name": "band_%d" % i, "from": [X0 - SPINE - 0.1, by, Z0 - 0.1], "to": [X0 + 0.3, by + BAND_H, Z1 + 0.1],
                    "faces": {"west": f(gold), "up": f(gold), "down": f(gold), "north": f(gold), "south": f(gold)}})
    # gold corners on the front cover
    for i, (cx, cy) in enumerate(((X0, Y0), (X1 - CORNER, Y0), (X0, Y1 - CORNER), (X1 - CORNER, Y1 - CORNER))):
        els.append({"name": "corner_%d" % i, "from": [cx, cy, Z1], "to": [cx + CORNER, cy + CORNER, Z1 + CORNER_RAISE],
                    "faces": {"south": f(gold), "up": f(gold), "down": f(gold), "east": f(gold), "west": f(gold)}})
    # the raised sigil plate (u 1..10, v 3..12 of the front cover face)
    ux0, vy0 = 1, 3
    els.append({"name": "plate", "from": [X0 + ux0, Y1 - (vy0 + PLATE), Z1], "to": [X0 + ux0 + PLATE, Y1 - vy0, Z1 + PLATE_RAISE],
                "faces": {"south": f([ux0, vy0, ux0 + PLATE, vy0 + PLATE]), "up": f(gold), "down": f(gold), "east": f(gold), "west": f(gold)}})
    # the ribbon bookmark hanging out of the pages
    els.append({"name": "ribbon", "from": [8.6, Y0 - 1.6, Z0 + 2.2], "to": [9.6, Y0 + PAGE_INSET_Y + 0.2, Z0 + 2.6],
                "faces": {"south": f(ribbon), "north": f(ribbon), "east": f(ribbon), "west": f(ribbon), "down": f(ribbon)}})
    disp = {
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "gui": {"rotation": [18, -32, 0], "translation": [0, 0, 0], "scale": [0.82, 0.82, 0.82]},
    }
    return {"texture_size": [32, 32], "textures": {"0": "nusmp:item/charter_grimoire_" + boss, "particle": "nusmp:item/charter_grimoire_%s_icon" % boss},
            "elements": els, "display": disp}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


# ---- a small software preview (z-buffered point sampling of the model's faces) so the art can be looked at without the game
def rot(v, ax, ay):
    x, y, z = v
    c, s = math.cos(ax), math.sin(ax)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ay), math.sin(ay)
    x, z = x * c + z * s, -x * s + z * c
    return x, y, z


FACES = {  # face -> (corner a, axis u, axis v) as functions of from/to, plus the outward normal
    "south": lambda a, b: ((a[0], b[1], b[2]), (1, 0, 0), (0, -1, 0), (0, 0, 1)),
    "north": lambda a, b: ((b[0], b[1], a[2]), (-1, 0, 0), (0, -1, 0), (0, 0, -1)),
    "east": lambda a, b: ((b[0], b[1], b[2]), (0, 0, -1), (0, -1, 0), (1, 0, 0)),
    "west": lambda a, b: ((a[0], b[1], a[2]), (0, 0, 1), (0, -1, 0), (-1, 0, 0)),
    "up": lambda a, b: ((a[0], b[1], a[2]), (1, 0, 0), (0, 0, 1), (0, 1, 0)),
    "down": lambda a, b: ((a[0], a[1], b[2]), (1, 0, 0), (0, 0, -1), (0, -1, 0)),
}


def preview(mdl, tex, size=240, ax_deg=18, ay_deg=-32, scale=0.82):
    ax, ay = math.radians(ax_deg), math.radians(ay_deg)
    canvas = Image.new("RGBA", (size, size), (54, 56, 66, 255))
    zbuf = [[1e9] * size for _ in range(size)]
    px = tex.load()
    k = size / 16.0 * scale
    for el in mdl["elements"]:
        a, b = el["from"], el["to"]
        for fname, face in el["faces"].items():
            org, du, dv, nrm = FACES[fname](a, b)
            ext_u = abs(du[0] * (b[0] - a[0]) + du[1] * (b[1] - a[1]) + du[2] * (b[2] - a[2]))
            ext_v = abs(dv[0] * (b[0] - a[0]) + dv[1] * (b[1] - a[1]) + dv[2] * (b[2] - a[2]))
            u1, v1, u2, v2 = face["uv"]
            n = rot(nrm, ax, ay)
            if n[2] <= 0:
                continue
            light = 0.62 + 0.38 * max(0.0, n[1] * 0.5 + n[2] * 0.7 + n[0] * 0.3)
            steps_u, steps_v = max(2, int(ext_u * 14)), max(2, int(ext_v * 14))
            for i in range(steps_u + 1):
                s = i / steps_u
                for j in range(steps_v + 1):
                    t = j / steps_v
                    p = tuple(org[q] + du[q] * ext_u * s + dv[q] * ext_v * t - 8 for q in range(3))
                    x, y, z = rot(p, ax, ay)
                    sx, sy = int(size / 2 + x * k), int(size / 2 - y * k)
                    if not (0 <= sx < size and 0 <= sy < size):
                        continue
                    tu = u1 + (u2 - u1) * s
                    tv = v1 + (v2 - v1) * t
                    r, g, bl, al = px[min(31, max(0, int(tu))), min(31, max(0, int(tv)))]
                    if al == 0:
                        continue
                    for ox in (0, 1):
                        for oy in (0, 1):
                            qx, qy = sx + ox, sy + oy
                            if qx < size and qy < size and -z < zbuf[qy][qx]:
                                zbuf[qy][qx] = -z
                                canvas.putpixel((qx, qy), (int(r * light), int(g * light), int(bl * light), 255))
    return canvas


def main():
    bosses = parse_table()
    sheet = None
    for boss, cover, trim in bosses:
        tex, icon = build(boss, cover, trim)
        os.makedirs(os.path.join(RES, "textures", "item"), exist_ok=True)
        tex.save(os.path.join(RES, "textures", "item", "charter_grimoire_%s.png" % boss))
        icon.save(os.path.join(RES, "textures", "item", "charter_grimoire_%s_icon.png" % boss))
        mdl = model(boss)
        write_json(os.path.join(RES, "models", "item", "charter_grimoire_%s.json" % boss), mdl)
        write_json(os.path.join(RES, "models", "item", "charter_grimoire_%s_icon.json" % boss),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/charter_grimoire_%s_icon" % boss}})
        if "--preview" in sys.argv:
            if sheet is None:
                sheet = Image.new("RGBA", (240 * 7, 240 + 16 * 4 + 8), (30, 30, 36, 255))
            i = [b[0] for b in bosses].index(boss)
            sheet.paste(preview(mdl, tex), (240 * i, 0))
            sheet.paste(icon.resize((64, 64), Image.NEAREST), (240 * i + 20, 244))
    if sheet is not None:
        out = sys.argv[sys.argv.index("--preview") + 1] if len(sys.argv) > sys.argv.index("--preview") + 1 else "charter_preview.png"
        sheet.save(out)
        print("preview:", out)
    print("wrote", len(bosses), "grimoires")


if __name__ == "__main__":
    main()
