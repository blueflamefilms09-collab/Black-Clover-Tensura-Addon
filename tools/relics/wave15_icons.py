#!/usr/bin/env python3
"""
Relics of Many Worlds, second set, wave 15 (footwear, cloaks, bracers, the Crest): the 16x16 inventory icons of the seventeen cards and their item models.

    python3 tools/relics/wave15_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
boots_of_burglery, handy_towel_cape, cape_of_bees, cloak_of_the_first_snow, autumn_cloak, darkened_cloak_of_elvenkind, cloak_of_levitation,
cape_of_projection, mantle_of_selflessness, bracers_of_phasing, bracer_of_initiative, phantoms_bracelet, bracers_of_manhandling,
bracers_of_arcane_blades, bear_bracer, bracelets_of_faithful_friendship, crest_of_foresight.

Each icon is an ASCII pixel map (16 rows of 16 characters, one palette per icon) so that every pixel is deliberate. A one pixel dark outline is
grown round the shape automatically (from the colour it touches), and every icon gets its grade colour as a rim sparkle in the top right corner
(Special green, Rare aqua, Unique pink, Legend gold) wherever that corner is still clear, plus the glow pixels the map itself carries.
Idempotent: running it again rewrites the same files. With --sheet it also writes an enlarged contact sheet for looking at them.
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


# ------------------------------------------------------------------------------------------------------------------------------ feet
# Boots of Burglery: a tall black lace-up combat boot seen from the side, steel toe cap, two buckles, wisps of white sparkle dissolving from the top.
icon("boots_of_burglery", "UNIQUE", {
    "k": 0x17171C, "K": 0x2C2C36, "L": 0x4A4A58, "s": 0xC4CAD6, "S": 0x8E96A8, "b": 0xE2C260, "w": 0xFFFFFF, "v": 0xBFE4FF, "l": 0xD8D8E4,
}, [
    "....w...v.w.....",
    "...v..w..v......",
    "....kKKKKk.w....",
    "....kLlLKk......",
    "....kKlKKk......",
    "....kLlLKk......",
    "....bbbbbk......",
    "....kKlKKk......",
    "....kLlLKk......",
    "....bbbbbk......",
    "....kKKKKkkk....",
    "....kKKKKKKkk...",
    "...kKKKKKKKKKk..",
    "...kKKKKKSSSSsk.",
    "...kkKKKKSsssSk.",
    "....kkkkkkkkkk..",
])

# ------------------------------------------------------------------------------------------------------------------------------ back
# Handy Towel-Cape: a plush crimson hood and cape with gold stripes, a row of little green snails along the hem, tied with a gold cord.
icon("handy_towel_cape", "RARE", {
    "R": 0x8A2331, "r": 0xB03A48, "d": 0x5C1220, "Y": 0xE6C34A, "y": 0xB08A24, "g": 0x7CC96A, "G": 0x3E8A3A, "w": 0xF2D8D0,
}, [
    "................",
    "......dRRd......",
    ".....dRrrRd.....",
    "....dRrddrRd....",
    "....RRdddRRR....",
    "....rRRYYRRr....",
    "...dRRRyyRRRd...",
    "...RrRRRRRRrR...",
    "..dRRYYYYYYRRd..",
    "..RrRRRRRRRRrR..",
    "..RRYYYYYYYYRR..",
    ".dRrRRRRRRRRrRd.",
    ".RRRRRRRRRRRRRR.",
    ".wGgwGgwGgwGgwGg",
    ".wwwwwwwwwwwwww.",
    "................",
])

# Cape of Bees: a short cape of black and yellow diagonal stripes with a white fur collar, two bees flying out.
icon("cape_of_bees", "RARE", {
    "k": 0x1E1C1A, "Y": 0xF2C230, "y": 0xC89A18, "w": 0xFFFFFF, "g": 0xD8D8E4, "b": 0x5C3A10, "x": 0xDCEEFF,
}, [
    "................",
    "....gwwwwwwg....",
    "...gwwgggwwwg...",
    "...kYkYkYkYkY...",
    "..kYkYkYkYkYk...",
    "..YkYkYkYkYkY...",
    ".kYkYkYkYkYkYk..",
    ".YkYkYkYkYkYkY..",
    "kYkYkYkYkYkYkYk.",
    "YkYkYkYkYkYkYkY.",
    "kkkkkkkkkkkkkkk.",
    "..............xb",
    "............xYkb",
    ".........xb..xx.",
    "........xYkb....",
    "................",
])

# Cloak of the First Snow: a long navy-to-teal cloak with a black fur collar and a silver chain clasp, white snowflakes on it.
icon("cloak_of_the_first_snow", "RARE", {
    "n": 0x1F3E66, "N": 0x2F5E8A, "t": 0x3C8296, "k": 0x15151C, "K": 0x30303C, "s": 0xD8DCE8, "S": 0x9AA0B0, "w": 0xFFFFFF, "c": 0xA8E4F8, "C": 0x7CB8E0,
}, [
    "................",
    ".....kKKKKk.....",
    "....kKkkkkKk....",
    "....kKnssnKk....",
    "...nNnnsSnnNn...",
    "...nNcnnnnwNn...",
    "..nNnnnwnnnNNn..",
    "..nNnwcwnnnNnn..",
    "..nNnnwnnncnNn..",
    ".nNNnnnnnwcwNNn.",
    ".nNNnnncnnwnNNn.",
    ".nNtnwcwnnnnNtn.",
    "nNNtnnwnnnnnNNtn",
    "nNtttnnnnnnntttn",
    "nttCtttnnntttCtn",
    "................",
])

# Autumn Cloak: a hooded cloak of overlapping leaves fading from red through orange and gold to brown at a ragged hem.
icon("autumn_cloak", "SPECIAL", {
    "r": 0x8C2A1E, "R": 0xB8381C, "o": 0xDE7A22, "O": 0xF0A030, "y": 0xF2C84A, "b": 0x6A3A1C, "m": 0x3E2412, "s": 0xBFC4CC,
}, [
    "................",
    ".....rrrrr......",
    "....rRRRRRr.....",
    "...srRmmmRrs....",
    "...rRmmmmmRr....",
    "....RRrRrRR.....",
    "...RRoRRoRRR....",
    "..RoOoRoOoRoR...",
    "..oOoOoOoOoOo...",
    ".oOyOoOyOoOyOo..",
    ".OyOyOyOyOyOyO..",
    ".yOyyOyyOyyOyy..",
    "yOyybyOybyOyyby.",
    "ybyymbybymbyybyb",
    ".b.mb.bm.mb.b.m.",
    "................",
])

# Darkened Cloak of Elvenkind: a full-length charcoal hooded cloak fading to navy, silver spiderweb trim, a small silver spider clasp, an empty black hood.
icon("darkened_cloak_of_elvenkind", "UNIQUE", {
    "c": 0x2A2D35, "C": 0x3E424E, "n": 0x1C2440, "k": 0x08080C, "s": 0xC8D0DC, "S": 0x8E98AA, "p": 0x6A7BA8,
}, [
    "................",
    ".....cCCCc......",
    "....cCkkkCc.....",
    "...cCkkkkkCc....",
    "...cCkkkkkCc....",
    "....cCkkkCc.....",
    "...sScCsCcSs....",
    "..cCcCscsCcCc...",
    "..cCscScSscCc...",
    ".ccCcsCsCscCcc..",
    ".cCcsCcscCcsCc..",
    ".cCsCcnpncCsCc..",
    "cCcCnnnnnnncCcC.",
    "cnnSnSnnSnSnnSn.",
    "nnSnnnSnnnSnnnS.",
    "................",
])

# Cloak of Levitation: a high-collared crimson cloak with gold trim and a round gold clasp, floating (a faint glow beneath).
icon("cloak_of_levitation", "LEGEND", {
    "R": 0xB02A2A, "r": 0xD04242, "d": 0x701818, "Y": 0xE6B94A, "y": 0xA88020, "g": 0xFFD070, "b": 0x2A4A8A,
}, [
    "................",
    "..dR.......Rd...",
    ".dRrd.....dRrd..",
    ".dRrRd...dRrRd..",
    ".dRRrRdYdRrRRd..",
    "..dRRRRYRRRRd...",
    "..dRRRYgYRRRd...",
    "..dRRrRYRrRRd...",
    "..dRRRrRrRRRd...",
    "..dRrRRRRRrRd...",
    "..dRRRrRrRRRd...",
    "..dRRRRRRRRRd...",
    "..YdRRrRrRRdY...",
    "...YYdddddYY....",
    "....g..g..g.....",
    "...g...g...g....",
])

# Cape of Projection: a long black cape with a white ermine collar spotted black, gold edging and a red sash with a gold chain.
icon("cape_of_projection", "RARE", {
    "k": 0x1A1A20, "K": 0x32323C, "w": 0xF5F0E0, "e": 0xC8C0B0, "Y": 0xE8C050, "y": 0xA88428, "r": 0xB02A36, "o": 0x0A0A0E,
}, [
    "................",
    "....wwewwewwe...",
    "...wowwowwowwe..",
    "...wwewwewwewe..",
    "...kkYkkkkYkk...",
    "..kKkkrrrrkkKk..",
    "..kKkYrYYrYkKk..",
    "..kKkkrrrrkkKk..",
    ".kKkkkkYYkkkkKk.",
    ".kKkkkkkkkkkkKk.",
    ".kKkkkkkkkkkkKk.",
    "kKkkkkkkkkkkkKk.",
    "kKkkkkkkkkkkkKk.",
    "kYkkkkkkkkkkkYk.",
    "YyYkkkkkkkkkYyY.",
    "................",
])

# Mantle of Selflessness: a short white-silver shoulder mantle, high collar, gold trim and leaf-petal pattern, a silver tassel clasp on the chest.
icon("mantle_of_selflessness", "UNIQUE", {
    "w": 0xF2F4FA, "W": 0xC8CEDC, "d": 0x8E96AC, "Y": 0xE6C25A, "y": 0xB08A28, "s": 0xE8ECF4, "t": 0xA0A8BC,
}, [
    "................",
    "....dWwwwwWd....",
    "...dWwwddwwWd...",
    "...dWwdwwdwWd...",
    "..YdwwwYYwwwdY..",
    ".YyWwYwwYYwYwWyY",
    ".YWwwYYwwwYYwwWY",
    ".YWwYywYYwYywwWY",
    ".yWwwYwwwwYwwWWy",
    "..YWwwwtstwwwWY.",
    "..YYWwwwtwwwWYY.",
    "...yYYWwtwWYYy..",
    ".....yYYtYYy....",
    ".......ttt......",
    ".......tst......",
    "................",
])

# ------------------------------------------------------------------------------------------------------------------------------ hands
# Bracers of Phasing: a long dark bronze forearm bracer with banded rings, three round glowing gold rune discs and a triangular plate over the back of the hand.
icon("bracers_of_phasing", "UNIQUE", {
    "b": 0x6A4A22, "B": 0x8A6A32, "L": 0xB48C48, "d": 0x3A2812, "g": 0xFFD860, "G": 0xFFF2A8, "o": 0xD8B454,
}, [
    "................",
    "...........oo...",
    "..........oLLo..",
    ".........bBBLBb.",
    "........dbBGGBb.",
    ".......dbBBgGBb.",
    "......dbBLBBBBb.",
    ".....dbBBgGBBb..",
    "....dbBBGgBLb...",
    "...dbBLBBBBb....",
    "..dbBBgGBBb.....",
    "..dBBGgBLb......",
    ".dbBLBBBb.......",
    ".dbBBBBb........",
    "..dbbbb.........",
    "................",
])

# Bracer of Initiative: a black and brown leather forearm bracer with brass studs, buckled straps and loops that hold polyhedral dice (blue, red, teal).
icon("bracer_of_initiative", "RARE", {
    "k": 0x1E1612, "n": 0x4A3424, "N": 0x6A4A32, "y": 0xC8A040, "Y": 0xF0D070, "B": 0x3C6CE8, "R": 0xE04040, "T": 0x30C8B0, "w": 0xF0F0F0,
}, [
    "................",
    "..kkkkkkkkkk....",
    ".knNNNNNNNNnk...",
    ".knyNNNNNNyNk...",
    ".knNNkkkkNNnk...",
    ".kyYyNNNNyYyk...",
    ".knNNkkkkNNnk...",
    ".knNNNNNNNNnk...",
    ".kyYyNNNNyYyk...",
    ".knNNkkkkNNnk...",
    "..knNNNNNNnk....",
    "..kkkkkkkkkk....",
    "....BwRwTw......",
    "....BBRRTT......",
    "................",
    "................",
])

# Phantom's Bracelet: a thin polished silver-black band with three draped loops of fine grey chain, on a pale purple haze.
icon("phantoms_bracelet", "RARE", {
    "s": 0xC8CCD8, "S": 0x8E92A4, "d": 0x4A4C5C, "c": 0x9A9EAE, "p": 0xC8A8F0, "P": 0x9A78D0,
}, [
    "................",
    "...p.....p......",
    "..pPp...pPp.....",
    "...sssssssss....",
    "..sSddddddSds...",
    ".sSdsssssssdSs..",
    ".sd.........ds..",
    ".sd..c...c..ds..",
    ".s..c.c.c.c..s..",
    "....c.c.c.c.....",
    "...c...c...c....",
    "...c...c...c....",
    "..c.....c.c.....",
    "..c.....c.c.....",
    ".c.......c......",
    "................",
])

# Bracers of Manhandling: a long angular blue-grey steel-and-leather bracer with pale silver rims, a pink gem near the wrist end, a second one peeking behind.
icon("bracers_of_manhandling", "RARE", {
    "b": 0x4A5870, "B": 0x6A7890, "L": 0x8E9CB4, "s": 0xDCE4F0, "S": 0xA8B4C8, "d": 0x2C3648, "p": 0xFF6AD0, "P": 0xFFB6EA, "l": 0x5A4A3A,
}, [
    "................",
    ".ss.............",
    ".sSBb...........",
    "..sBLBb.........",
    "..dBBLBb...ss...",
    "...dBLBBb.sSBb..",
    "...dbBLBBbbBLBb.",
    "....dbBBLBBBBLb.",
    ".....dbBBLBBLBd.",
    ".....dlbBBBBBbd.",
    "......dlbBpPBbd.",
    "......dlbBPpBbd.",
    ".......dlbBBbd..",
    "........ssSSss..",
    "................",
    "................",
])

# Bracers of Arcane Blades: reddish-brown leather bracers with gold filigree swirls, gold buckles and laces; a dagger of purple arcane energy beside them.
icon("bracers_of_arcane_blades", "RARE", {
    "r": 0x7C4A30, "R": 0x9A6040, "d": 0x4A2A18, "Y": 0xD8B050, "y": 0xA88428, "v": 0x9A7CFF, "V": 0xDCCCFF, "w": 0xFFFFFF,
}, [
    ".............V..",
    "............Vv..",
    "...........Vv...",
    "..........Vv....",
    ".dddddd..vv.....",
    ".dRRRRRRd.v.....",
    ".dRYRRYRd.......",
    ".dRyYYyRd.......",
    ".dYRRRRYd.......",
    ".dRyYYyRd.......",
    ".dRRYYRRd.......",
    ".dYRRRRYd.......",
    ".dRRRRRRd.......",
    ".drrrrrrd.......",
    "..dddddd........",
    "................",
])

# Bear Bracer: a grey steel forearm bracer with a round bear-paw emblem, orange-brown rivets at the wrist, brown straps; blue claw slashes behind.
icon("bear_bracer", "RARE", {
    "g": 0x808890, "G": 0xA8B0B8, "d": 0x4A5058, "o": 0xC27838, "O": 0xE8A050, "n": 0x6A4426, "b": 0x58A8FF, "B": 0xB4DCFF, "k": 0x20242A,
}, [
    "................",
    "..b..b..b.......",
    "..Bb.Bb.Bb......",
    "...Bb.Bb.Bb.....",
    "....ddddddd.....",
    "...dGGGGGGGd....",
    "...dGgkgkgGd....",
    "...dGkkkkkGd....",
    "...dGgkkkgGd....",
    "...dGGGGGGGd....",
    "...nnnnnnnnn....",
    "...doOdoOdoO....",
    "...dGGGGGGGd....",
    "....ddddddd.....",
    "................",
    "................",
])

# Bracelets of Faithful Friendship: a pair of braided cuff bracelets of silver-grey and teal cords, each with a silver plate of clasped hands.
icon("bracelets_of_faithful_friendship", "RARE", {
    "s": 0xC8D0DC, "S": 0x8E98A8, "t": 0x2FB5B0, "T": 0x7CE8E0, "d": 0x1C5A5A, "w": 0xF0F4F8, "k": 0x3A424E,
}, [
    "................",
    "..ssss....ssss..",
    ".sTtStt..sttSTs.",
    "sTtSttSt.tSttStt",
    "tStTStTt.tTtStSt",
    "TtSkwwkS.SkwwkSt",
    "tStkwskT.TkswkSt",
    "StTkkkkt.tkkkkTS",
    "tSttTtSt.tSttTtS",
    "sTtStTtS.StTtStT",
    ".sTtSttt.ttStTs.",
    "..sTtStT.TtStTs.",
    "...ssTtS.StTss..",
    "......dd..dd....",
    "................",
    "................",
])

# Crest of Foresight: a golden shield-shaped crest with scrollwork flourishes, a clock face with a great many hands, two crossed ornate spears behind.
icon("crest_of_foresight", "LEGEND", {
    "Y": 0xE0B83A, "y": 0xA8841C, "d": 0x6A4A10, "g": 0xFFF0A0, "w": 0xFFFFFF, "k": 0x3A2A08, "c": 0xF8F0D8, "s": 0xC8CCD8,
}, [
    "s..............s",
    ".s....yYYy....s.",
    "..s..yYggYy..s..",
    "...sYYccccYYs...",
    "..yYYcckkccYYy..",
    ".yYYcckwkkccYYy.",
    ".YYgckkkkwkcgYY.",
    ".YYgckwkkkkcgYY.",
    ".yYYcckkwkccYYy.",
    "..yYYcckkccYYy..",
    "..syYYcccYYYys..",
    ".s.yyYYYYYyy.s..",
    "s...dyYYYyd...s.",
    "......dyyd......",
    ".......dd.......",
    "................",
])


# ------------------------------------------------------------------------------------------------------------------------------ drawing
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
