#!/usr/bin/env python3
"""
Relics of Many Worlds, part 4, wave 29 (four potions): the 16x16 inventory icons and their item models.

    python3 tools/relics/wave29_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
pocket_portal, potion_of_vampirism, romlos_potion_of_sleep, spider_brew.

Each icon is an ASCII pixel map (16 rows of 16 characters, one palette per icon). A one pixel dark outline is grown round the shape from the colour it
touches and every icon gets its grade colour as a rim sparkle in the top right corner (Special green, Unique pink) when that corner is clear.
Idempotent. With --sheet it also writes an enlarged contact sheet for looking at them.
"""
import argparse
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

GRADE_RGB = {"SPECIAL": (0x55, 0xFF, 0x55), "RARE": (0x55, 0xFF, 0xFF), "UNIQUE": (0xFF, 0x77, 0xFF), "LEGEND": (0xFF, 0xAA, 0x00)}


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


ICONS = []


def icon(iid, grade, palette, rows):
    ICONS.append((iid, grade, {k: hexc(v) for k, v in palette.items()}, rows))


# Pocket Portal: a clear glass sphere swirling purple and blue, a dark purple band with a zig-zag silver edge running from the top right to the lower left,
# and a round wax seal with a spiral on the band; a white glint top left, a pale blue underside.
icon("pocket_portal", "UNIQUE", {
    "q": 0x5A3A9A, "p": 0x8A5CD0, "P": 0xB48CF0, "W": 0xF0F4FF, "B": 0x6A8CE8, "b": 0xA0C4FF, "D": 0x3A2466, "d": 0x28184A,
    "S": 0x5A3A8A, "s": 0x8A66C0, "g": 0xB4B4CC,
}, [
    "................",
    "......qppqq.....",
    "....qpPWWPDDq...",
    "...qpPWPPPDSDq..",
    "..qpPPPPPDsSSDq.",
    "..qpPPPPPDSsSDq.",
    "..qpPPPPDDgDDpq.",
    "..qpPPPDDgDDPPq.",
    "..qpPPDgDDPPPBq.",
    "..qpPPDDgDPPBBq.",
    "..qpPBDDDPPBBbq.",
    "...qpBBPPPBBbq..",
    "...qpBBBBBBbbq..",
    "....qpBBBbbbq...",
    ".....qqppqqq....",
    "................",
])

# Potion of Vampirism: a teardrop flask in the shape of a pale skull with red glowing eyes, a bloody fanged tongue hanging as the point, a dark cork bound
# with silver at the top.
icon("potion_of_vampirism", "UNIQUE", {
    "k": 0x5A3A4A, "K": 0x3A2230, "s": 0xC4CCDC, "S": 0x8A92A8, "C": 0xF0E6D0, "c": 0xC8B898, "R": 0xD0182C, "r": 0x8A0E1E, "L": 0xFF6070, "w": 0xFFFFFF,
}, [
    "......kKkk......",
    "......KkkK......",
    "......ssSs......",
    ".....cCCCCc.....",
    "...ccCCCCCCcc...",
    "..cCCCCCCCCCCc..",
    "..cCRRRCCRRRCc..",
    "..CCRLRCCRLRCC..",
    "..cCRRRCCRRRCc..",
    "...cCCCrrCCCc...",
    "...cCwCCCCwCc...",
    "....cwRRRRwc....",
    ".....cwRLRw.....",
    "......cRRc......",
    ".......Rr.......",
    ".......r........",
])

# Romlo's Potion of Sleep: a squat glass bottle (pale green rim and glass) of bright lime liquid with a brown cork, a black label bearing a white curly S.
icon("romlos_potion_of_sleep", "SPECIAL", {
    "o": 0x9A6A3A, "O": 0x6A4422, "G": 0xBFE6CC, "g": 0x8FC4A4, "L": 0x9CF03C, "l": 0x6CC41C, "m": 0x4C9A10, "k": 0x1E1E24, "W": 0xF0F0F0, "w": 0xB4B4BC,
    "h": 0xE8FFE0,
}, [
    "................",
    ".....oOOOo......",
    ".....OooOO......",
    ".....gGGGg......",
    "....gGhGGGg.....",
    "...gGGGGGGGg....",
    "...gLLLLLLLg....",
    "..gGLkkkkkLGg...",
    "..gGLkWWwkLGg...",
    "..gGLkwWkkLGg...",
    "..gGLkWWwkLGg...",
    "..gGLkkkkkLGg...",
    "..gGlLLLLLlGg...",
    "..ggGllllllGg...",
    "...gggmmmmggg...",
    "................",
])

# Spider Brew: a stoppered bottle of blue liquid wrapped in cobwebs, a cork topped with a web, a gold chain with a teal-gem spider pendant hanging down the front.
icon("spider_brew", "SPECIAL", {
    "o": 0xA08C78, "O": 0x6E5A48, "G": 0xB4D0E8, "g": 0x7A9CBC, "B": 0x3A78E0, "b": 0x2458B0, "n": 0x7CB0FF, "w": 0xF4F4F8, "y": 0xE8C048, "Y": 0xA88020,
    "T": 0x40E0C8, "t": 0x1C9C90,
}, [
    "................",
    "....wooOOow.....",
    ".....oOwOo......",
    ".....gGGGg......",
    "....gGnGGg......",
    "...gwBBBBBg.....",
    "..gwBnBBBBBg....",
    "..gBwBByBBBg....",
    "..gBBwBYBBBg....",
    "..gBBBwTtBBg....",
    "..gBnBYTtYBg....",
    "..gBBbBYYBBg....",
    "..gbBBBBBbBg....",
    "..ggbBBBBbgg....",
    "...ggggggggw....",
    "................",
])


def render(iid, grade, pal, rows):
    assert len(rows) == 16, iid + ": needs 16 rows, has %d" % len(rows)
    for y, r in enumerate(rows):
        assert len(r) == 16, "%s row %d is %d wide: %r" % (iid, y, len(r), r)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            assert ch in pal, "%s: no colour for %r at %d,%d" % (iid, ch, x, y)
            px[x, y] = pal[ch] + (255,)
    out = img.copy()
    op = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                continue
            near = [px[nx, ny] for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)) if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3]]
            if not near:
                continue
            r = sum(c[0] for c in near) // len(near)
            g = sum(c[1] for c in near) // len(near)
            b = sum(c[2] for c in near) // len(near)
            op[x, y] = (int(r * 0.28), int(g * 0.26), int(b * 0.3), 255)
    gr = GRADE_RGB[grade]
    for (x, y, k) in ((15, 0, 1.0), (14, 0, 0.55), (15, 1, 0.55)):
        if op[x, y][3] == 0:
            op[x, y] = tuple(int(c * k) for c in gr) + (255 if k == 1.0 else 190,)
    return out


def write_model(iid):
    path = os.path.join(MODELS, iid + ".json")
    os.makedirs(MODELS, exist_ok=True)
    with open(path, "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + iid}}, f, indent=2)
        f.write("\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet", help="also write an enlarged contact sheet to this path")
    args = ap.parse_args()
    os.makedirs(TEX, exist_ok=True)
    imgs = []
    for iid, grade, pal, rows in ICONS:
        im = render(iid, grade, pal, rows)
        im.save(os.path.join(TEX, iid + ".png"))
        write_model(iid)
        imgs.append((iid, im))
    print("wrote %d icons and models" % len(imgs))
    if args.sheet:
        S = 12
        cols = 4
        rowsn = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * (16 * S + 8) + 8, rowsn * (16 * S + 8) + 8), (74, 76, 92, 255))
        for i, (iid, im) in enumerate(imgs):
            big = im.resize((16 * S, 16 * S), Image.NEAREST)
            x0 = 8 + (i % cols) * (16 * S + 8)
            y0 = 8 + (i // cols) * (16 * S + 8)
            sheet.paste(big, (x0, y0), big)
        sheet.save(args.sheet)


if __name__ == "__main__":
    main()
