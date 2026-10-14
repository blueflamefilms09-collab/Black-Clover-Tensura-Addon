#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 14 (heads and footwear): the 16x16 inventory icons of the seventeen cards and their item models.

    python3 tools/relics/wave14_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
gold_circlet, silver_circlet, steel_circlet, tiny_duck_crown, unicorn_crown, circlet_of_the_phoenix, crown_of_the_conjurer,
diadem_of_fabulous_truthiness, dark_unicorn_crown, ruby_boots_of_planestriding, boots_of_the_indomitable, mudwalkers,
boots_of_the_high_ground, greaves_of_the_wind_walker, waterwalkers, sandals_of_sneaking, dragon_fire_boots.

Each icon is an ASCII pixel map (a 16 wide row per line, one palette per icon; rows are padded with clear pixels on the right and blank rows at the
bottom). A symmetrical icon is given as its left half (8 wide) and mirrored with sym(). A one pixel dark outline is grown round the shape (from the
colour it touches) and the grade colour is a rim sparkle in the top right corner (Special green, Rare aqua, Unique pink, Legend gold, God red) where
that corner is clear. Idempotent. With --sheet it also writes an enlarged contact sheet for looking at them.
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


# ----------------------------------------------------------------------------------------------------------------------- heads
# Gold Circlet: a gold band with a crest of dragon flame and two small horn tips.
icon("gold_circlet", "LEGEND", {
    "Y": 0xFFE680, "y": 0xE0B33A, "o": 0xA6741A, "b": 0x6B4410, "F": 0xFF7A1C, "f": 0xFFD23C, "r": 0xD8341C, "w": 0xFFF6C8, "g": 0xE0244A,
}, sym(
    ".......f",
    "......fF",
    ".....rfF",
    ".....rFF",
    ".w...ryF",
    ".wy..YyY",
    ".YyYyYyy",
    "Yyyyyyyg",
    "yooooyyy",
    ".boooooo",
))

# Silver Circlet: a silver band with a crest of ice shards.
icon("silver_circlet", "LEGEND", {
    "S": 0xF4F8FF, "s": 0xD5DCE8, "d": 0x8FA3BC, "e": 0x5A6C86, "I": 0xBFF1FF, "i": 0x6FCBF0, "n": 0x2E8FC8,
}, sym(
    ".......I",
    "......iI",
    "..I...iI",
    "..iI.nii",
    "..iisSsS",
    "..SsSsss",
    ".SssssIs",
    "sddsssds",
    "deeddddd",
    ".eeeeeee",
))

# Steel Circlet: a plain steel band notched with a lightning bolt, a storm gem at the brow.
icon("steel_circlet", "LEGEND", {
    "S": 0xC4CCDA, "s": 0x8A93A6, "d": 0x5C6579, "e": 0x353C4C, "Y": 0xFFF58A, "y": 0xE8E27A, "o": 0xB8A630,
}, sym(
    ".......Y",
    "......Yy",
    "..s..Yyo",
    "..sS.yYo",
    "..SsSsYy",
    ".SsssSyY",
    "Sdsssssy",
    "sddssdds",
    "deddeddd",
    ".eeeeeee",
))

# Tiny Duck Crown: a tiny gold crown with battlements, a duck's bill in the middle and small red-orange gems.
icon("tiny_duck_crown", "SPECIAL", {
    "Y": 0xFFE680, "y": 0xE6B33A, "o": 0x9A6A24, "b": 0x5E3A14, "r": 0xE0481C, "R": 0xFF8A3C, "w": 0xFFF6C8,
}, sym(
    "........",
    ".......R",
    ".Y.Y...R",
    ".yYyY.Ry",
    ".yyyyYyY",
    ".yryyyyy",
    ".yyyyyyy",
    ".ooooooo",
    ".bbbbbbb",
))

# Unicorn Crown: a pale circlet with feathered wings, a rainbow band, a pink gem and a tall spiralled golden horn.
icon("unicorn_crown", "UNIQUE", {
    "W": 0xFFFFFF, "w": 0xEDEEF6, "s": 0xBAC0D6, "G": 0xFFE680, "g": 0xF0C85A, "o": 0xB8902C, "p": 0xFF8AD0, "P": 0xFFC4EC, "t": 0x6FE0D0, "y": 0xFFF06A, "v": 0xA46CE0,
}, sym(
    "......WG",
    ".....Gog",
    ".....gGo",
    ".W...Gog",
    ".wW..gGo",
    ".wsW.sgs",
    ".wsswtpP",
    ".sswyvpp",
    "..ssttvp",
    "...ssss.",
))

# Circlet of the Phoenix: thin gold circlet of swept feather wings meeting in a knot set with an orange teardrop gem.
icon("circlet_of_the_phoenix", "UNIQUE", {
    "Y": 0xFFE680, "y": 0xE0A93A, "o": 0xA06A1A, "O": 0xFF9A3C, "f": 0xFFD23C, "r": 0xE5532A, "b": 0x3A8AD8, "R": 0xD8242E,
}, sym(
    ".......f",
    "......fO",
    ".Y....OO",
    ".yY..Orr",
    "YyrY.yOr",
    "ybyyYyrR",
    ".yryyyoO",
    "..yoyyyo",
    "...oooyo",
))

# Crown of the Conjurer: a pale bronze band with many branching antler tines rising from it, a green fey spark between them.
icon("crown_of_the_conjurer", "UNIQUE", {
    "Y": 0xE8D49A, "y": 0xC9AE6E, "o": 0x8A7440, "b": 0x3E3326, "G": 0x6FE08A, "g": 0x2E9A58, "k": 0x17120C,
}, sym(
    "Y..Y....",
    "yY.yY...",
    ".yYyY...",
    "..yyY..G",
    "..yyyyGg",
    ".YyyyyyY",
    ".yoooooo",
    ".bbbbbbb",
))

# Diadem of Fabulous Truthiness: an ornate silver Art-Nouveau diadem of swirls and fans, a green gem at the peak.
icon("diadem_of_fabulous_truthiness", "RARE", {
    "S": 0xF4F8FF, "s": 0xC9D2DE, "d": 0x8E9CB0, "e": 0x5A6880, "G": 0x4FD08A, "g": 0x7CFFB0, "h": 0x1F8F5C,
}, sym(
    ".......g",
    "......Gh",
    ".....sGG",
    "..s..SsG",
    ".sSs.sSs",
    ".sSsSsGs",
    "sdsGdsdS",
    "sddsdsss",
    ".eddddds",
    "..eeeeee",
))

# Dark Unicorn Crown: a black circlet with swept-back wings and a tall spiralling dark-red horn, red gems in the brow.
icon("dark_unicorn_crown", "UNIQUE", {
    "K": 0x1E1822, "k": 0x39303F, "L": 0x554A63, "R": 0xE0244A, "r": 0x8E1B2E, "d": 0x4A0F1C, "H": 0xFF5470, "w": 0xB0A4C0,
}, sym(
    ".......H",
    ".......R",
    "......Rr",
    "......rR",
    ".K....Rr",
    ".kK...rR",
    ".kLK.KdR",
    ".kLKkKRK",
    "..kKkKKK",
    "...kkkk.",
))

# ----------------------------------------------------------------------------------------------------------------------- boots
# Ruby Boots of Planestriding: a pair of glossy crimson lace-up ankle boots with a small heel and black laces.
icon("ruby_boots_of_planestriding", "UNIQUE", {
    "R": 0xFF5470, "r": 0xC21E3E, "d": 0x7C0F26, "k": 0x1A1216, "w": 0xFFC4D0, "g": 0xFF9AA8,
}, [
    "..kk......kk....",
    "..kr......kr....",
    ".krRr....krRr...",
    ".krwr....krwr...",
    ".krkr....krkr...",
    ".krRr....krRr...",
    ".krRrrr..krRrrr.",
    ".krRRRRr.krRRRRr",
    ".kRrRrrr.kRrRrrr",
    ".kdddddd.kdddddd",
    ".kkkkkkk.kkkkkkk",
])

# Boots of the Indomitable: tall stitched brown leather boots with wide open cuffs and heavy grey steel toe caps and heel guards.
icon("boots_of_the_indomitable", "RARE", {
    "B": 0x9A6A3E, "b": 0x7A5230, "d": 0x4E3320, "S": 0xC0C4CC, "s": 0x8A8E96, "e": 0x575B64, "k": 0x1A1A1E, "t": 0xD8B070,
}, [
    ".bBBb....bBBb...",
    ".bBBBb..bBBBBb..",
    "..bBBb....bBBb..",
    "..bBtb....bBtb..",
    "..bBBb....bBBb..",
    "..bBtb....bBtb..",
    "..bBBb....bBBb..",
    "..bBBbbb..bBBbbb",
    "..bBBBBSs.bBBBBS",
    "..sbBBSSSs.bBBSS",
    "..ssSSSSes.ssSSS",
    "..kkkkkkkk.kkkkk",
])

# Mudwalkers: knee-high lace-up brown boots with ragged folded cuffs, caked in reddish mud with clods around them.
icon("mudwalkers", "SPECIAL", {
    "B": 0x8A5E3A, "b": 0x6F4E32, "d": 0x4A3220, "M": 0xB0583A, "m": 0x8A3E2A, "c": 0x5E2A1C, "t": 0xD8C8A0, "k": 0x1E1612,
}, [
    "..bbBb...bbBb...",
    ".bbBBBb.bbBBBb..",
    "..bBMb...bBMb...",
    "..btbb...btbb...",
    "..bBbb...bBbb...",
    "..btbb...btbb...",
    "..bBMb...bBMb...",
    "..bBBbbb.bBBbbb.",
    ".mbBMMMBbmbBMMMB",
    "mMmbBBBBbmMmbBBB",
    ".mmcMmmcmmmcMmmc",
    "..cmcc.cm.mcc.cm",
])

# Boots of The High Ground: a pair of tall plain black leather riding boots, knee high, grey shading, a low heel.
icon("boots_of_the_high_ground", "UNIQUE", {
    "K": 0x2A2A30, "k": 0x3E3E46, "g": 0x6A6A74, "G": 0x8E8E98, "d": 0x15151A, "Y": 0xFFD27A,
}, [
    "..kKKk...kKKk...",
    "..kGgk...kGgk...",
    "..kKgk...kKgk...",
    "..kKKk...kKKk...",
    "..kGKk...kGKk...",
    "..kKKk...kKKk...",
    "..kKgk...kKgk...",
    "..kKKkkk.kKKkkk.",
    "..kGKKKKk.kGKKKK",
    "..kKKggKKk.kKKgg",
    "..dddddddd.ddddd",
    "..kkkkkkkk.kkkkk",
])

# Greaves of the Wind Walker: a dark navy leg greave with gold-tan plated knee and ankle guards, a ribbed gold calf, glowing blue-violet gems.
icon("greaves_of_the_wind_walker", "GOD", {
    "N": 0x2A2D4E, "n": 0x1A1C34, "T": 0xC9A45A, "t": 0x9A7A38, "Y": 0xE8CE8A, "V": 0x7A6CFF, "v": 0xC6BFFF, "B": 0x8A5E3A, "b": 0x5E3E24, "k": 0x101020,
}, [
    "....tYYt........",
    "...tYVvYt.......",
    "...TYVvYT.......",
    "...tTYYTt.......",
    "....nNNn........",
    "....TNNT........",
    "....tTTt........",
    "....TNNT........",
    "....tTTt........",
    "....nNNn........",
    "...tYVvYt.......",
    "...TTTTTT.......",
    "..bBBBBBBBb.....",
    ".bBBBBBBBBBb....",
    ".kkkkkkkkkkk....",
])

# Waterwalkers: brown leather laced boots with bright blue water-pattern side panels and a water-droplet gem on each cuff.
icon("waterwalkers", "SPECIAL", {
    "B": 0x9A6A3E, "b": 0x7A5230, "d": 0x4E3320, "W": 0x3AA4F0, "w": 0x7AD8FF, "D": 0x1F6AB0, "t": 0xD8C8A0, "k": 0x1A1612,
}, [
    "..bBBb....bBBb..",
    "..bwWb....bwWb..",
    "..bWDb....bWDb..",
    "..bBBb....bBBb..",
    "..btbb....btbb..",
    "..bBWb....bBWb..",
    "..btbb....btbb..",
    "..bBwWbb..bBwWbb",
    "..bBWDBBb.bBWDBB",
    "..bBBBBBBb.bBBBB",
    "..bbbbbbbb.bbbbb",
    "..kkkkkkkk.kkkkk",
])

# Sandals of Sneaking: knee-high grey-silver gladiator sandals, blue glowing laces spiralling up the shin, rune soles.
icon("sandals_of_sneaking", "SPECIAL", {
    "S": 0xD0D5E0, "s": 0xA9AFBE, "d": 0x6C7488, "e": 0x4E5668, "B": 0x4FA8FF, "b": 0x1F6AD8, "w": 0xC6E4FF, "k": 0x20242E,
}, [
    "..sSs.....sSs...",
    "..sBs.....sBs...",
    "..SsBs...SsBs...",
    "..sBbs...sBbs...",
    "..sSBs...sSBs...",
    "..sBbs...sBbs...",
    "..SsBs...SsBs...",
    "..sBsss..sBsss..",
    "..sSsSSs.sSsSSs.",
    "..dsSSSSsddsSSSs",
    "..eBdeBdeeBdeBde",
    "..kkkkkkkkkkkkkk",
])

# Dragon Fire Boots: grey scaled boots with brown-red dragon-head toes, white horn trim, bat-wing membranes and orange flame jets from the soles.
icon("dragon_fire_boots", "RARE", {
    "G": 0x8A8A94, "g": 0x6E6E76, "d": 0x4A4A52, "W": 0xF0ECDC, "w": 0xC8C2AC, "R": 0xA0422A, "r": 0x6E2A1A, "F": 0xFF8A1C, "f": 0xFFD23C, "k": 0x1E1E24,
}, [
    "..gGg.....gGg...",
    ".wgGGg...wgGGg..",
    "wRgGdg...RgGdg..",
    ".wgGGg...wgGGg..",
    "..gdGg....gdGg..",
    "..gGgg....gGgg..",
    "..gGdgW...gGdgW.",
    "..gGGgRRW.gGGgRR",
    "..gdgRrrWWgdgRrr",
    "..kkkkkkkk.kkkkk",
    "...FfFfFf..FfFfF",
    "....FfFf....FfFf",
    ".....Ff......Ff.",
])


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
