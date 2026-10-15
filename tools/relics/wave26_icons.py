#!/usr/bin/env python3
"""
Relics of Many Worlds, part 4, wave 26 (heads): the 16x16 inventory icons of the twelve cards and their item models.

    python3 tools/relics/wave26_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
frank, goggles_of_dwarvenkin, helm_of_whispers, helmet_of_headbutting, jellyfish_cap, lens_of_straight_creepin, mask_of_difference,
mask_of_indifference, mask_of_mind_melting, mask_of_the_raven_queens_disciple, mask_of_the_witchdoctor, mind_stone.

Each icon is an ASCII pixel map (a 16 wide row per line, one palette per icon; rows are padded with clear pixels on the right and blank rows at the
bottom). A symmetrical icon is given as its left half (8 wide) and mirrored with sym(). A one pixel dark outline is grown round the shape (from the
colour it touches) and the grade colour is a rim sparkle in the top right corner (Special green, Rare aqua, Unique pink, Legend gold) where that corner
is clear. Idempotent. With --sheet it also writes an enlarged contact sheet for looking at them.
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


def sym(*half):
    """Mirror left halves (8 wide) into full rows."""
    out = []
    for h in half:
        assert len(h) == 8, "half row must be 8 wide: %r" % h
        out.append(h + h[::-1])
    return out


ICONS = []


def icon(iid, grade, palette, rows):
    rows = [r.ljust(16, ".") for r in rows]
    rows += ["." * 16] * (16 - len(rows))
    ICONS.append((iid, grade, {k: hexc(v) for k, v in palette.items()}, rows))


# Frank: the warm brown carved rabbit mask, tall grey-lined ears, round eye holes, a small pink nose.
icon("frank", "UNIQUE", {"B": 0xB5733A, "b": 0x7A4A22, "L": 0xCFCFCF, "G": 0x8E8E92, "e": 0x1C1008, "W": 0xF2F2EA, "p": 0xE08A9A, "h": 0xD89A5A},
     sym("..LL....", ".bLLb...", ".bLGb...", ".bBBb...", ".bBhBbbb", "bBBBBBBB", "bBeeeBBB", "bBeWeBBB", "bBeeeBBp", ".bBBBBBp", ".bBBhBBB", "..bBBBBB", "...bBBBb"))

# Goggles of Dwarvenkin: copper frame, two amber rune-gems, tufts of white fur above, a cream strap.
icon("goggles_of_dwarvenkin", "SPECIAL", {"o": 0xA8623A, "O": 0x6E3C20, "A": 0xFFB62E, "Y": 0xFFE680, "w": 0xFFFFFF, "W": 0xDDE6EE, "c": 0xF0E2BC},
     sym("cccccccc", "..w.w..w", ".wWwWwW.", "oooooooo", "o..YY..o", "o.AYYA.o", "oAYYYYAo", "o.AYYA.o", "o..AA..o", "OooooooO"))

# Helm of Whispers: a spiked bronze helm with a pointed face plate, wreathed in green wisps.
icon("helm_of_whispers", "LEGEND", {"B": 0x8A5A32, "b": 0x4E2E18, "s": 0xC9A27A, "S": 0xE8D0A8, "G": 0x4CFF7A, "g": 0x2EA854, "W": 0xB8FFCC},
     sym("......GW", "...s..gG", "...sS.Gg", "..bBBBBb", ".bBBBBBB", ".bBBbbBB", ".sBBBBBb", "..bBBBbG", "...bBBBg", "....bBBB", ".....bBB", "......bB"))

# Helmet of Headbutting: a riveted grey viking helmet with two big banded brown ram horns, small pale horns and a green gem.
icon("helmet_of_headbutting", "RARE", {"S": 0xA8ACB4, "s": 0x6C7078, "h": 0x6A4426, "H": 0xA06A3C, "p": 0xE0D2B0, "r": 0xD8DCE4, "G": 0x4CE0A0, "d": 0x3A3C42},
     sym(".hh.....", "hHHh.SSS", "hHhhSSss", "hhhSsSSS", "pphSSSSS", "ppdrdrdr", "..sSSSSS", "..sSsssS", "...sssSG", "....sssS", ".....ssS"))

# Jellyfish Cap: a pale teal translucent dome with a gold crown rim and trailing tentacles.
icon("jellyfish_cap", "UNIQUE", {"T": 0xB4F0DC, "t": 0x6CCDB4, "w": 0xF2FFFA, "G": 0xFFD23C, "g": 0xB8861C},
     sym("........", "....tTTT", "...tTTTT", "..tTTwTT", ".tTTTTTT", "ttTTTTTT", "gGGGGGGG", "tg.t.t.t", "t..t.t.t", ".t.t..t.", "t..t.t.t", ".t..t..t", "t...t..."))

# Lens of Straight Creepin': a round glossy red-orange monocle in a thin black frame on a black bead chain with an eye-engraved cap.
icon("lens_of_straight_creepin", "SPECIAL", {"k": 0x1E1E22, "R": 0xE8401C, "O": 0xFF8A3C, "w": 0xFFE0C8, "c": 0x2C2C32, "e": 0x8A8A92},
     sym("........", ".....kkk", "...kkRRR", "..kRRwRR", ".kRRRRRO", ".kRRRROO", ".kRRROOO", "..kROOOO", "..kkOOOO", "....kkkk", ".......c", ".......c", "......ee", "......ee"))

# Mask of Difference: a bright blue leather-like mask with swirling filigree and long ribbon ties.
icon("mask_of_difference", "RARE", {"B": 0x2F7BE0, "b": 0x1A4C9C, "l": 0x9CC8FF, "k": 0x0C1C3C, "r": 0x3C8CF0},
     sym("........", "rr.bBBBB", "r.bBBBBB", "rbBBlBBB", ".bBlBBBB", "bBkkkBBl", "bBkBBBBB", "bBBBlBBB", ".bBBBBBB", ".rbBBlBB", "r.bbBBBB", "..r.bbBB"))

# Mask of Indifference: a faceted pale grey-silver paper-like mask with a blank face and dark ties.
icon("mask_of_indifference", "RARE", {"S": 0xC8CCD6, "s": 0x8E93A0, "d": 0x30323A, "k": 0x181A20},
     sym("........", "dd.sSSSS", "d.sSSsSS", "dsSSSSsS", ".sSsSSSS", "sSkkSSsS", "sSkSSSSS", "ssSSsSSs", ".sSSSsSS", ".dsSSSSS", "d..sssSS", "...d.sss"))

# Mask of Mind Melting: a blue squid-like helmet with three red eyes on top, a pink fin, a toothy mouth and long maroon tentacles.
icon("mask_of_mind_melting", "SPECIAL", {"B": 0x3E5CB8, "b": 0x22357A, "R": 0xFF3A3A, "p": 0xE06A9C, "k": 0x10142C, "w": 0xF0F0F0, "m": 0x8A2040, "M": 0x1A0C18},
     sym("......pp", ".....bBR", "....bBBB", "...bBBBR", "...bBBBB", "..bBBBBR", "..bBkkBB", ".bBBBBBB", ".bBMMMMM", ".bBwMwMw", "mbB.MMMM", "mm..m...", "m...m...", ".m..m...", "mm...m.."))

# Mask of the Raven Queen's Disciple: a sleek navy and silver raven half-mask with swept horns, a beak and a purple gem on the brow.
icon("mask_of_the_raven_queens_disciple", "RARE", {"n": 0x1E2848, "N": 0x3A4A78, "s": 0xB0B8C8, "S": 0xE4E8F2, "P": 0xB46CFF, "p": 0xE0B8FF},
     sym("n......s", "nn.sssSS", ".nnSSSpP", ".nnnSSSS", ".nn.sSSn", ".nn..nnn", "..nn.nnn", "...nsnnn", "....nSnn", ".....nSn", "......sS", ".......S"))

# Mask of the Witchdoctor: a carved brown mask of stacked planks, a green gem on the forehead, green leaves on top and pale horns.
icon("mask_of_the_witchdoctor", "SPECIAL", {"B": 0x9A5E34, "b": 0x5E3418, "l": 0x7CD83C, "L": 0xC4F070, "h": 0xE8DCC0, "G": 0x3CFF6A, "k": 0x1C1008},
     sym(".l...l..", ".lL.lL.l", "hh.bBbBb", "h.bBBbBB", ".bBbBBGG", ".bBBbBBG", ".bkkBbBB", ".bkBBbBB", ".bBbBBbB", "..bBbBBb", "..bBBbBB", "...bBbBb", "....bBBb"))

# Mind stone: a silvery open-faced helm with swirling filigree, crowned by pale yellow-green leaf-like crystals.
icon("mind_stone", "LEGEND", {"S": 0xEEF0EC, "s": 0xB8BCB8, "d": 0x6A6E70, "l": 0xC2D84A, "L": 0xE6FF70, "g": 0x8AA82C},
     sym("..l.l.L.", ".lLlLlLl", "..sSSSSS", ".sSSSSSS", ".sSdddSS", ".sSdddSS", ".sSSdSSS", ".sSSSSSs", "..sSSsSs", "...sSSSS"))


# ----------------------------------------------------------------------------------------------------------------------- rendering
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
        cols = 6
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
