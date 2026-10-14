#!/usr/bin/env python3
"""
Relics of Many Worlds, second set, wave 19 (scrolls, potions, sweets, the spellbook): the 16x16 inventory icons of the fourteen cards and their item models.

    python3 tools/relics/wave19_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
spellbook_of_concentration, fey_honey, scroll_of_unreadable_spells, blood_chocolates, scroll_of_absorption, ale_lemental,
scroll_of_charm_parson_bargain_bin_scrolls_1 .. scroll_of_facsimile_copy_bargain_bin_scrolls_6, mug_of_the_boss, holy_water.

Each icon is an ASCII pixel map (16 rows of 16 characters, one palette per icon) so that every pixel is deliberate. The six Bargain Bin scrolls share one
parchment-and-red-band map and differ in the 8x4 picture over the band. A one pixel dark outline is grown round the shape automatically (from the colour
it touches) and every icon gets its grade colour as a rim sparkle in the top right corner (Special green, Rare aqua, Legend gold) plus the glow pixels the
map itself carries. Idempotent: running it again rewrites the same files. With --sheet it also writes an enlarged contact sheet for looking at them.
"""
import argparse
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

GRADE_RGB = {"SPECIAL": (0x55, 0xFF, 0x55), "RARE": (0x55, 0xFF, 0xFF), "UNIQUE": (0xFF, 0x77, 0xFF), "LEGEND": (0xFF, 0xAA, 0x00), "GOD": (0xFF, 0x55, 0x55)}


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


ICONS = []


def icon(iid, grade, palette, rows):
    ICONS.append((iid, grade, {k: hexc(v) for k, v in palette.items()}, rows))


# ---------------------------------------------------------------------------------------------------------------------------- the spellbook
# Spellbook of Concentration: a red leather book seen from the front, a brass-edged cover with a strap and clasp, a purple crystal pendant hanging off the
# strap, soft light around it (the 'g' pixels).
icon("spellbook_of_concentration", "RARE", {
    "R": 0xB23434, "r": 0x8A2222, "q": 0x5E1414, "B": 0xE2C072, "b": 0xB08A3C, "P": 0xF3E6C0, "p": 0xD4C294,
    "c": 0xB77CFF, "C": 0xE6CCFF, "g": 0xFFF2B8, "s": 0x6B4A2A,
}, [
    "...g.......g....",
    "..g...........g.",
    "...bbbbbbbbbb...",
    "..bRRRRRRRRRRq..",
    "..bRrrrrrrrrRq..",
    "..bRrrrBBrrrRq..",
    "..bRrrBPPBrrRq..",
    "..bRrBPppPBrRq..",
    "..bRrrBPPBrrRq..",
    "..bRrrrBBrrrRq..",
    "..bRRRBBBBRRRq..",
    "..bRRRBssBRRRq..",
    "..bRRRRBBRRRRq..",
    "..pPbbbbbbbbPpC.",
    "...pppppppppcCc.",
    ".g..........cC.g",
])

# ---------------------------------------------------------------------------------------------------------------------------- the scrolls
# The shared Bargain Bin scroll: rod ends (k/K), parchment (P light, p mid, d shadow), a red leather band over the middle (R/r), a torn corner at the bottom
# right, and 8x4 of picture space (rows 3 to 6, columns 4 to 11) that the motifs below fill in with 'm' (the glow colour) and 'M' (its highlight).
BIN_BASE = [
    "................",
    ".kKppppppppppKk.",
    ".KkPPPPPPPPPPkK.",
    "..dPPPPPPPPPPd..",
    "..dPPPPPPPPPPd..",
    "..dPPPPPPPPPPd..",
    "..dPPPPPPPPPPd..",
    "..RRRRRRRRRRRR..",
    "..rrrrrrrrrrrr..",
    "..dPPnnnnnnPPd..",
    "..dPPPPPPPPPPd..",
    "..dPPnnnnnPPPd..",
    "..dPPPPPPPPP....",
    ".KkPPPPPPPPPkK..",
    ".kKppppppppp.Kk.",
    "................",
]

BIN_MOTIFS = {
    # a heart with a little cross of clergy
    "charm": [
        ".mm..mm.",
        "mMmmmmmm",
        ".mmmmmm.",
        "..mmmm..",
    ],
    # a ball of pine needles
    "firball": [
        ".m.mm.m.",
        "mmMmmMmm",
        ".mmMMmm.",
        "m.mmmm.m",
    ],
    # a few stones
    "rocks": [
        "..mm....",
        ".mMmm.mm",
        "mmmmmmMm",
        ".mmm.mmm",
    ],
    # a crab, claws raised
    "crab": [
        "mm....mm",
        "m.mmmm.m",
        ".mMmmMm.",
        "m.m..m.m",
    ],
    # an iron fist
    "mace": [
        ".mmmmmm.",
        "mMMmmmmm",
        "mmmmmmmm",
        ".mmmmmm.",
    ],
    # a thing and its poorer copy
    "copy": [
        "mmmmm...",
        "mMMMm.mm",
        "mMMmmmMm",
        "mmmm.mmm",
    ],
}


def bin_scroll(iid, motif, glow, light):
    rows = [list(r) for r in BIN_BASE]
    for dy, mr in enumerate(BIN_MOTIFS[motif]):
        for dx, ch in enumerate(mr):
            if ch != ".":
                rows[3 + dy][4 + dx] = ch
    pal = {"k": 0x8B5A2B, "K": 0x5A3A1A, "P": 0xEFE2BC, "p": 0xD2BF90, "d": 0xB9A574, "R": 0x9E2D2D, "r": 0x6E1C1C, "n": 0x8A7A58,
           "m": glow, "M": light}
    icon(iid, "RARE", pal, ["".join(r) for r in rows])


bin_scroll("scroll_of_charm_parson_bargain_bin_scrolls_1", "charm", 0xE65C9C, 0xFFC2DE)
bin_scroll("scroll_of_firball_bargain_bin_scrolls_2", "firball", 0x2E7D32, 0x8BE08F)
bin_scroll("scroll_of_speak_with_rocks_bargain_bin_scrolls_3", "rocks", 0x7C7C86, 0xC4C4CC)
bin_scroll("scroll_of_summon_greater_crab_bargain_bin_scrolls_4", "crab", 0xE04020, 0xFF9A6A)
bin_scroll("scroll_of_mace_hand_bargain_bin_scrolls_5", "mace", 0x8E96A8, 0xDCE2F0)
bin_scroll("scroll_of_facsimile_copy_bargain_bin_scrolls_6", "copy", 0x4FA8E0, 0xBDE6FF)

# Scroll of Unreadable Spells: a grey-white scroll on two copper rods with red gem knobs, red runic letters and splashes across the middle, a pale blue splotch.
icon("scroll_of_unreadable_spells", "RARE", {
    "k": 0xB8733A, "K": 0x6E4220, "e": 0xE02A3A, "E": 0xFF8090, "P": 0xE8E8EE, "p": 0xC4C4D0, "d": 0x9C9CB0, "b": 0xA8D0F0, "m": 0xE02A3A, "M": 0xFFA0A8,
}, [
    "................",
    ".eEkKKKKKKKKkEe.",
    ".EekPPPPPPPPkeE.",
    "..dPPbbPPPPPPd..",
    "..dPmPbbmPmPPd..",
    "..dPPmMPmmPPPd..",
    "..dPmPmPPMmPPd..",
    "..dPPPmmPPmmPd..",
    "..dPmMPPmPPPPd..",
    "..dPPmPmPmMPPd..",
    "..dPPPPmPPmPPd..",
    "..dPmPmPPPPPPd..",
    "..dPPPPPPPPPPd..",
    ".eEkPPPPPPPPkEe.",
    ".EekppppppppkeE.",
    "................",
])

# Scroll of Absorption: tan parchment on two wooden rods, red ribbons and chains hanging, a big gold-rimmed empty mirror in the middle, small sigil discs around it.
icon("scroll_of_absorption", "LEGEND", {
    "k": 0x8B5A2B, "K": 0x5A3A1A, "P": 0xE2CFA0, "p": 0xC2AC78, "d": 0xA08C5C, "G": 0xFFD24A, "g": 0xC08A1C, "w": 0xE8E4FF, "W": 0xB48CFF, "v": 0x5C4A98,
    "r": 0xC02A3A, "o": 0xFF8A2A, "x": 0xF4F4F4, "c": 0x58A8F0,
}, [
    "................",
    ".rkKkKKKKKKkKkr.",
    ".rKkPPPPPPPPkKr.",
    "..dPPPPGGPPPPd..",
    "..dPoPGggGPcPd..",
    "..dPPGgwWvgGPPd.",
    "..dPGgwWWvvgGPd.",
    "..dPGgwWvvvgGPd.",
    "..dPGgWvvvvgGPd.",
    "..dPPGgvvvgGPPd.",
    "..dPxPGggGPPPPd.",
    "..dPPPPGGPPPPd..",
    "..dPPPPPPPPPPd..",
    ".rKkPPPPPPPPkKr.",
    ".rkKkppppppkKkr.",
    "..r..........r..",
])

# ---------------------------------------------------------------------------------------------------------------------------- the potions and sweets
# Fey Honey: a squat glass jar with a metal screw lid tied with a red cord, filled with mercury-bright silver honey with a rainbow-sheened swirl on top.
icon("fey_honey", "RARE", {
    "M": 0xA6AAB8, "m": 0x6E7384, "n": 0x4A4E5C, "r": 0xD02A3A, "R": 0x8E1A26, "G": 0xDDEBF2, "g": 0xAAC4D4, "S": 0xF4F7FF, "s": 0xC4CEE4, "t": 0x8E9ABB,
    "a": 0xFFB0E0, "b": 0x9EE6FF, "y": 0xFFF0A0,
}, [
    "................",
    "....nMMMMMMn....",
    "....nMMMMMMn....",
    "....mmmmmmmm....",
    "....rRrrrrRr....",
    "...rR.gggg.Rr...",
    "..gGGGGGGGGGGg..",
    ".gGSsaabbyySSGg.",
    ".gGSbbyyaassSGg.",
    ".gGSsSSSSSSsSGg.",
    ".gGSSSSSSSSSSGg.",
    ".gGsSSSSSSSSsGg.",
    ".gGtssSSSSsstGg.",
    "..gGGttttttGGg..",
    "...gggggggggg...",
    "................",
])

# Blood Chocolates: two square dark chocolates stamped with bats, one broken with dark red syrup running out, a red foil wrapper with a purple bat beside them.
icon("blood_chocolates", "SPECIAL", {
    "C": 0x5A3426, "c": 0x3A2018, "d": 0x25120C, "h": 0x7A4A36, "B": 0xB01030, "b": 0x7A0A20, "L": 0xE04060, "W": 0xC02030, "w": 0x8E1624, "V": 0xA040E0,
    "f": 0xFF8090,
}, [
    "................",
    "..hCCCCCCh......",
    "..CcdcCdcC..WWW.",
    "..CddCCddC.WWwWW",
    "..CcddddcC.WwVwW",
    "..CCcddcCC.WVVVW",
    "..chCCCChc.WwVwW",
    "...cccccc..WWWWw",
    "...........wwww.",
    ".hCCCCCCh.......",
    ".CcdcCdBBC......",
    ".CddCCBLbC......",
    ".CcddBbBbC......",
    ".CCcBLbhCC.f....",
    "..cBbBcccc......",
    "...bB...........",
])

# Ale-lemental: an orange wooden barrel-style tankard with darker bands, a grey lid and a curled handle; a droplet of blue spirit rising from it.
icon("ale_lemental", "RARE", {
    "O": 0xD88A3A, "o": 0xB06A28, "q": 0x7A4418, "H": 0xF0B060, "G": 0x9CA0AC, "g": 0x6E727E, "b": 0x58B8FF, "B": 0xBDE4FF, "n": 0x4A2A10,
}, [
    "......b.........",
    ".....bBb........",
    ".....bbb........",
    "...GGGGGGGG.....",
    "..GggggggggG....",
    "...nHOOOOoq.....",
    "...qqqqqqqq.....",
    "...nHOOOOoqnnn..",
    "...nHOOOOoq..n..",
    "...qqqqqqqq..n..",
    "...nHOOOOoq..n..",
    "...nHOOOOoqnnn..",
    "...nHOOOOoq.....",
    "...qqqqqqqq.....",
    "....nooooon.....",
    "................",
])

# Mug of The Boss: a plain white ceramic coffee mug with a handle, black printed lines of its title, carved runes in gold at the rim.
icon("mug_of_the_boss", "RARE", {
    "W": 0xFAFAFA, "w": 0xDADCE4, "s": 0xB4B8C8, "k": 0x2A2A32, "G": 0xFFD25A, "g": 0xC8961E, "c": 0x6B4A2A,
}, [
    "................",
    "...G.G.GG.G.....",
    "...wWWWWWWw.....",
    "..wWWWWWWWWw....",
    "..wWkkWkkWWwwww.",
    "..wWWWWWWWWw..w.",
    "..wWkWkkWkWw..ws",
    "..wWWWWWWWWw..ws",
    "..wWkkkWkkWwwws.",
    "..wWWWWWWWWw....",
    "..wWWWWWWWWw....",
    "..swWWWWWWws....",
    "...swwwwwws.....",
    "....ssssss......",
    "................",
    "................",
])

# Holy Water: a tapering glass flask (square pyramid) of clear water with a speck in it, a cork stopper tied with a red wax-sealed ribbon, runes cut in the glass.
icon("holy_water", "RARE", {
    "C": 0xB07A44, "c": 0x7A4E26, "r": 0xC02030, "R": 0x7A1018, "y": 0xFFE9A0, "G": 0xA9CCEA, "g": 0x6E9CC8, "w": 0xCBE3F8, "W": 0xFFFFFF, "b": 0x8EC0FF,
    "x": 0xF8F8D0, "u": 0x8FB8E2,
}, [
    "................",
    ".......cC.......",
    ".......Cc.......",
    "......rRrR......",
    ".....gGGGGg.....",
    ".....gwWwwg.....",
    "....gGwWwwGg....",
    "....gwbwwxwg....",
    "...gGwwwwwwGg...",
    "...gwwuWwwwwg...",
    "..gGwwwwwWwwGg..",
    "..gwwWwwwwuwwg..",
    ".gGwwwwWwwwwwGg.",
    ".guuwwwwwwwwuug.",
    ".gggggggggggggg.",
    "................",
])


# ---------------------------------------------------------------------------------------------------------------------------- drawing
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
    # a one pixel dark outline, taken from the colour it touches
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
    # the grade colour as a rim sparkle in the top right corner (only where that corner is still clear)
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
