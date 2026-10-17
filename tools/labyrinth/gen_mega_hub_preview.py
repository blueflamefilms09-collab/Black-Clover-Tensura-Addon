#!/usr/bin/env python3
"""
Pictures of the plaza template for `gen_mega_hub.py --preview` (needs Pillow): a top-down plan and an isometric view, coloured by block id.

Why our own and not `mega_preview.py`: that tool shades by the Mega-Dungeon's floor row and has no colours for flowers, wool, water or signs, so
a paved square with a fountain and four stalls comes out as one grey blur. The point of a picture is to judge shape, pattern, colour balance and
clutter before anything is in the game, so every block the plaza uses has a colour here (the table is checked: an unknown block is a loud grey).
"""
import math
import os
import re
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

EXACT = {
    "stone_bricks": (124, 124, 124), "cracked_stone_bricks": (112, 112, 112), "mossy_stone_bricks": (108, 128, 98),
    "chiseled_stone_bricks": (118, 118, 124), "polished_andesite": (156, 156, 154), "andesite": (136, 137, 135), "smooth_stone": (168, 168, 168),
    "stone_brick_stairs": (128, 128, 128), "stone_brick_slab": (130, 130, 130), "stone_brick_wall": (126, 126, 126), "dirt": (134, 96, 67),
    "grass_block": (92, 160, 58), "short_grass": (80, 150, 50), "water": (46, 92, 210), "sea_lantern": (196, 232, 224),
    "light_blue_stained_glass": (132, 176, 236), "lantern": (250, 196, 84), "barrel": (122, 92, 52), "flower_pot": (160, 90, 70),
    "labyrinth_gate": (70, 120, 255), "poppy": (206, 40, 40), "dandelion": (244, 222, 58), "cornflower": (80, 104, 224),
    "azure_bluet": (232, 232, 238), "oxeye_daisy": (240, 240, 235), "allium": (176, 100, 204), "red_tulip": (210, 50, 40), "pink_tulip": (240, 150, 190),
    "white_tulip": (238, 238, 240), "orange_tulip": (238, 140, 40), "lily_of_the_valley": (240, 245, 240), "rose_bush": (190, 40, 50),
    "peony": (236, 160, 200), "lilac": (190, 150, 220), "fern": (70, 130, 50), "tall_grass": (80, 150, 50), "large_fern": (70, 130, 50),
    "dark_oak_planks": (66, 43, 20), "deepslate_tiles": (54, 54, 58), "cracked_deepslate_tiles": (50, 50, 54), "lightning_rod": (196, 122, 76),
    "iron_bars": (150, 150, 156), "chain": (60, 66, 78),
}
SUFFIX = [
    (r".*_wool|.*_carpet|.*_wall_banner|.*_banner", None),
    (r"spruce_.*|stripped_spruce_.*", (104, 76, 46)),
    (r".*_sign", (160, 128, 84)),
    (r"potted_.*", (190, 80, 70)),
    (r".*_leaves", (60, 130, 50)),
]
WOOL = {"red": (180, 44, 40), "blue": (50, 70, 170), "yellow": (232, 200, 50), "green": (70, 150, 50), "white": (238, 238, 238), "black": (28, 28, 32)}


def color_of(block):
    path = block.split(":", 1)[1] if ":" in block else block
    if path in EXACT:
        return EXACT[path] + (255,) if len(EXACT[path]) == 3 else EXACT[path]
    for pat, col in SUFFIX:
        if re.fullmatch(pat, path):
            if col is None:
                key = path.split("_", 1)[0]
                return WOOL.get(key, (200, 200, 200)) + (255,)
            return col + (255,)
    return (255, 0, 255, 255)           # unknown: loud


def shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)), c[3] if len(c) > 3 else 255)


def top_view(t, out, scale=16):
    sx, sy, sz = t.size
    img = Image.new("RGBA", (sx * scale, sz * scale), (40, 80, 40, 255))
    d = ImageDraw.Draw(img)
    best = {}
    for (x, y, z), c in t.cells.items():
        if c.block == "minecraft:air":
            continue
        if (x, z) not in best or y > best[(x, z)][0]:
            best[(x, z)] = (y, c)
    for (x, z), (y, c) in best.items():
        col = shade(color_of(c.block), 0.62 + 0.38 * min(1.0, y / 9.0))
        d.rectangle([x * scale, z * scale, (x + 1) * scale - 1, (z + 1) * scale - 1], fill=col)
    img.save(out)


def _rot(x, z, size, k):
    """Turn a cell a quarter turn k times about the middle of the footprint (so the plaza can be looked at from all four sides)."""
    for _ in range(k % 4):
        x, z = size - 1 - z, x
    return x, z


def iso_view(t, out, scale=9, rot=0, crop=None):
    sx_, _, sz_ = t.size
    cells = {}
    for (x, y, z), c in t.cells.items():
        if c.block == "minecraft:air":
            continue
        nx, nz = _rot(x, z, sx_, rot)
        cells[(nx, y, nz)] = c
    if crop:
        x1, x2, z1, z2 = crop
        cells = {p: c for p, c in cells.items() if x1 <= p[0] <= x2 and z1 <= p[2] <= z2}
    tw, th, vh = 2 * scale, scale, scale
    xs = [(x - z) * tw // 2 for (x, y, z) in cells] or [0]
    ys = [(x + z) * th // 2 - y * vh for (x, y, z) in cells] or [0]
    ox, oy = -min(xs) + tw, -min(ys) + vh * 2
    img = Image.new("RGBA", (max(xs) - min(xs) + tw * 2 + 2, max(ys) - min(ys) + vh * 3 + th + 4), (150, 190, 240, 255))
    d = ImageDraw.Draw(img, "RGBA")
    for (x, y, z) in sorted(cells, key=lambda p: (p[0] + p[1] + p[2], p[1])):
        if all((x + dx, y + dy, z + dz) in cells for dx, dy, dz in ((1, 0, 0), (0, 0, 1), (0, 1, 0))):
            continue
        c = color_of(cells[(x, y, z)].block)
        sx = (x - z) * tw // 2 + ox
        sy = (x + z) * th // 2 - y * vh + oy
        top_pts = [(sx, sy - th // 2), (sx + tw // 2, sy), (sx, sy + th // 2), (sx - tw // 2, sy)]
        left_pts = [(sx - tw // 2, sy), (sx, sy + th // 2), (sx, sy + th // 2 + vh), (sx - tw // 2, sy + vh)]
        right_pts = [(sx + tw // 2, sy), (sx, sy + th // 2), (sx, sy + th // 2 + vh), (sx + tw // 2, sy + vh)]
        d.polygon(left_pts, fill=shade(c, 0.74))
        d.polygon(right_pts, fill=shade(c, 0.56))
        d.polygon(top_pts, fill=shade(c, 1.0))
    img.save(out)


def render(t, prefix):
    top_view(t, prefix + "_top.png")
    for k in range(4):
        iso_view(t, "%s_iso%d.png" % (prefix, k), scale=8, rot=k)
    print("wrote %s_top.png and %s_iso0..3.png" % (prefix, prefix))


def elevation(t, out, x1, x2, z1, z2, scale=28, from_south=True, y1=0):
    """A flat front view of a part of the template: looking north (from the south) or south, each column shows the nearest block."""
    sx, sy, sz = t.size
    w, h = (x2 - x1 + 1) * scale, (sy - y1) * scale
    img = Image.new("RGBA", (w, h), (150, 190, 240, 255))
    d = ImageDraw.Draw(img)
    for x in range(x1, x2 + 1):
        for y in range(y1, sy):
            zs = range(z2, z1 - 1, -1) if from_south else range(z1, z2 + 1)
            for z in zs:
                c = t.cells.get((x, y, z))
                if c is not None and c.block != "minecraft:air":
                    col = shade(color_of(c.block), 1.0 - 0.015 * abs((z2 if from_south else z1) - z))
                    px = (x - x1) * scale
                    py = (sy - 1 - y) * scale
                    d.rectangle([px, py, px + scale - 1, py + scale - 1], fill=col, outline=(0, 0, 0, 60))
                    break
    img.save(out)


# ------------------------------------------------------------------ the map of the whole Hub
def _font(size, bold=False):
    from PIL import ImageFont
    for name in (("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"), "LiberationSans-Regular.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def layout_map(k, out):
    """A plan and a side section of the whole Hub drawn from the integer constants of HubLayout / HubPlazaLayout / HubProtect (`k`, a dict): where the
    plaza, the Holy Spire, the Abyssal Chasm and the three gates stand, what is protected, how high and how deep the landmarks go. The point is to see at a
    glance that the numbers the three builders share make one coherent place (nothing overlaps, every gate is inside its landmark, the dimension fits)."""
    sx = 3                                                     # pixels per block, horizontally
    x_min, x_max = k["CHASM_X"] - k["CHASM_RIM_RADIUS"] - 30, k["SPIRE_X"] + k["SPIRE_BASE_RADIUS"] + 30
    z_half = max(k["CHASM_RIM_RADIUS"], k["SPIRE_BASE_RADIUS"], k["PLAZA_RADIUS"]) + 16
    w = (x_max - x_min) * sx
    plan_h = 2 * z_half * sx
    y_lo, y_hi = -64, 319                                      # the dimension type: min_y -64, height 384
    sy = 1.0                                                   # pixels per block, vertically
    sec_h = int((y_hi - y_lo) * sy)
    head, gap = 70, 56
    img = Image.new("RGB", (w + 40, head + plan_h + gap + sec_h + 70), (24, 26, 32))
    d = ImageDraw.Draw(img, "RGBA")
    f_small, f_mid, f_big = _font(13), _font(16), _font(22, True)
    ox, oy = 20, head
    d.text((20, 14), "Floor 0, the Sanctuary Hub (nusmp:sanctuary): plan and section along z = 0", font=f_big, fill=(235, 235, 240))
    d.text((20, 44), "every number is read from HubLayout.java / HubPlazaLayout.java / HubProtect.java", font=f_small, fill=(150, 154, 166))

    def X(x):
        return ox + (x - x_min) * sx

    def Z(z):
        return oy + (z + z_half) * sx

    def circle(cx, cz, r, **kw):
        d.ellipse([X(cx - r), Z(cz - r), X(cx + r), Z(cz + r)], **kw)

    d.rectangle([ox, oy, ox + w, oy + plan_h], fill=(70, 128, 62))                           # the plains
    circle(k["PLAZA_X"], k["PLAZA_Z"], k["PLAZA_RADIUS"] + 24, fill=(82, 140, 70))              # the blend
    circle(k["PLAZA_X"], k["PLAZA_Z"], k["PLAZA_RADIUS"], fill=(98, 152, 82))                   # the flattened disc
    d.line([X(k["CHASM_DOOR_X"]), Z(0), X(k["SPIRE_DOOR_X"]), Z(0)], fill=(168, 168, 164), width=int(3 * sx))      # the road along z = 0
    circle(k["SPIRE_X"], k["SPIRE_Z"], k["SPIRE_BASE_RADIUS"], fill=(236, 236, 240), outline=(200, 170, 70), width=2)
    circle(k["SPIRE_X"], k["SPIRE_Z"], k["SPIRE_TOP_RADIUS"], outline=(120, 120, 130), width=1)
    circle(k["CHASM_X"], k["CHASM_Z"], k["CHASM_RIM_RADIUS"], fill=(22, 18, 30), outline=(150, 110, 200), width=2)
    circle(k["CHASM_X"], k["CHASM_Z"], k["CHASM_FLOOR_RADIUS"], outline=(110, 90, 150), width=1)
    ph = k["SIZE_X"] // 2
    d.rectangle([X(k["PLAZA_X"] - ph), Z(k["PLAZA_Z"] - ph), X(k["PLAZA_X"] + ph + 1), Z(k["PLAZA_Z"] + ph + 1)], fill=(150, 150, 150), outline=(220, 70, 70), width=2)
    r = k["GATE_SPHERE_RADIUS"]
    for gx, gz, col in ((k["HOLY_GATE_X"], k["HOLY_GATE_Z"], (240, 200, 60)), (k["ABYSS_GATE_X"], k["ABYSS_GATE_Z"], (170, 110, 240))):
        circle(gx, gz, r, outline=(220, 70, 70), width=2)
        d.rectangle([X(gx - 1), Z(gz - 1), X(gx + 2), Z(gz + 2)], fill=col)
    d.rectangle([X(k["HOME_GATE_X"] - 1), Z(k["HOME_GATE_Z"]), X(k["HOME_GATE_X"] + 2), Z(k["HOME_GATE_Z"] + 1)], fill=(120, 90, 235))
    d.ellipse([X(k["SPAWN_X"]) - 4, Z(k["SPAWN_Z"]) - 4, X(k["SPAWN_X"]) + 4, Z(k["SPAWN_Z"]) + 4], fill=(70, 220, 230))
    for x, z, lines in (
            (k["SPIRE_X"], k["SPIRE_Z"] + k["SPIRE_BASE_RADIUS"] + 6, ("Holy Spire (Ascension)", "doorway on the plaza side,", "stairs up to the Holy gate")),
            (k["CHASM_X"], k["CHASM_Z"] + k["CHASM_RIM_RADIUS"] + 6, ("Abyssal Chasm (Descent)", "rim path, ramp down to the Abyssal gate")),
            (k["PLAZA_X"], k["PLAZA_Z"] + ph + 6, ("plaza (33 x 33 template)", "arrival cell, notice pavilion, fountain,", "Gate of Return in the north"))):
        for i, line in enumerate(lines):
            d.text((X(x), Z(z) + 17 * i), line, font=f_small, fill=(235, 235, 240), anchor="ma")
    d.text((X(k["HOME_GATE_X"]) + 12, Z(k["HOME_GATE_Z"]) - 8), "home gate", font=f_small, fill=(205, 190, 255))
    d.text((X(k["SPAWN_X"]) + 8, Z(k["SPAWN_Z"]) - 8), "arrival", font=f_small, fill=(120, 235, 240))
    d.text((X(k["HOLY_GATE_X"]), Z(k["HOLY_GATE_Z"]) - 30), "Holy gate (y %d)" % k["HOLY_GATE_Y"], font=f_small, fill=(120, 80, 0), anchor="ma")
    d.text((X(k["ABYSS_GATE_X"]), Z(k["ABYSS_GATE_Z"]) - 30), "Abyssal gate (y %d)" % k["ABYSS_GATE_Y"], font=f_small, fill=(200, 160, 250), anchor="ma")
    d.text((ox + 8, oy + 8), "red outlines = protected volumes (the plaza box, a sphere of %d blocks round each polarity gate)" % r, font=f_small, fill=(255, 190, 190))

    # the side section: x to the right, y up
    sy0 = head + plan_h + gap
    d.text((20, sy0 - 30), "section along z = 0 (height: min_y %d .. highest block %d)" % (y_lo, y_hi), font=f_mid, fill=(235, 235, 240))

    def Y(y):
        return sy0 + (y_hi - y) * sy

    d.rectangle([ox, sy0, ox + w, sy0 + sec_h], fill=(120, 170, 235))                          # the sky
    surf = k["SURFACE_Y"]
    d.rectangle([ox, Y(surf - 1), ox + w, sy0 + sec_h], fill=(104, 86, 66))                      # the ground (stone, deepslate below)
    d.rectangle([ox, Y(surf - 1), ox + w, Y(surf - 1) + 2], fill=(92, 160, 58))
    d.rectangle([ox, Y(0), ox + w, sy0 + sec_h], fill=(86, 86, 92))
    d.rectangle([ox, Y(-64 + 1), ox + w, sy0 + sec_h], fill=(30, 30, 34))
    top = surf + k["SPIRE_HEIGHT"]
    d.polygon([(X(k["SPIRE_X"] - k["SPIRE_BASE_RADIUS"]), Y(surf)), (X(k["SPIRE_X"] - k["SPIRE_TOP_RADIUS"]), Y(top)),
               (X(k["SPIRE_X"] + k["SPIRE_TOP_RADIUS"]), Y(top)), (X(k["SPIRE_X"] + k["SPIRE_BASE_RADIUS"]), Y(surf))], fill=(236, 236, 240), outline=(200, 170, 70))
    d.polygon([(X(k["SPIRE_X"] - k["SPIRE_BASE_RADIUS"] + 3), Y(surf)), (X(k["SPIRE_X"] - k["SPIRE_TOP_RADIUS"] + 3), Y(top)),
               (X(k["SPIRE_X"] + k["SPIRE_TOP_RADIUS"] - 3), Y(top)), (X(k["SPIRE_X"] + k["SPIRE_BASE_RADIUS"] - 3), Y(surf))], fill=(190, 205, 235))     # hollow
    bottom = surf - k["CHASM_DEPTH"]
    d.polygon([(X(k["CHASM_X"] - k["CHASM_RIM_RADIUS"]), Y(surf - 1)), (X(k["CHASM_X"] - k["CHASM_FLOOR_RADIUS"]), Y(bottom)),
               (X(k["CHASM_X"] + k["CHASM_FLOOR_RADIUS"]), Y(bottom)), (X(k["CHASM_X"] + k["CHASM_RIM_RADIUS"]), Y(surf - 1))], fill=(22, 18, 30))
    d.rectangle([X(k["PLAZA_X"] - ph), Y(surf + 8), X(k["PLAZA_X"] + ph + 1), Y(surf - 3)], fill=(150, 150, 150), outline=(220, 70, 70))
    for gx, gy, col in ((k["HOLY_GATE_X"], k["HOLY_GATE_Y"], (240, 200, 60)), (k["ABYSS_GATE_X"], k["ABYSS_GATE_Y"], (170, 110, 240)), (k["HOME_GATE_X"], k["HOME_GATE_Y"], (120, 90, 235))):
        d.rectangle([X(gx - 1), Y(gy + 2), X(gx + 2), Y(gy - 2)], fill=col)
    for y, label in ((surf, "SURFACE_Y %d" % surf), (y_hi, "build limit %d" % y_hi), (y_lo, "bottom %d" % y_lo), (top, "Spire crown %d" % top), (bottom, "Chasm floor %d" % bottom)):
        d.line([ox, Y(y), ox + w, Y(y)], fill=(255, 255, 255, 70), width=1)
        d.text((ox + 6, Y(y) - 15), label, font=f_small, fill=(255, 255, 255))
    d.text((X(k["SPIRE_X"]) - 40, Y(top) - 20), "Holy Spire", font=f_small, fill=(40, 40, 50))
    d.text((X(k["CHASM_X"]) - 40, Y(surf) + 6), "Abyssal Chasm", font=f_small, fill=(230, 230, 240))
    img.save(out)
