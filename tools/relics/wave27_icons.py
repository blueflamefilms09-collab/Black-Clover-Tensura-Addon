#!/usr/bin/env python3
"""
Relics of Many Worlds, part 4, wave 27 (headgear and gloves): the 16x16 inventory icons of the twelve cards and their item models.

    python3 tools/relics/wave27_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
soul_searchers, the_mad_kings_crown, the_orb_of_ancient_sight, trickster_mask, arcane_tricksters_glove, bears_grip, dragons_fire_gauntlet,
gauntlet_mimic, gauntlet_of_the_stormcaller, glove_of_appreciation, gloves_of_opportunity, goristro_charger.

Each icon is an ASCII pixel map (16 rows of up to 16 characters, short rows are padded with clear pixels, one palette per icon). A one pixel dark
outline is grown round the shape from the colour it touches, and every icon gets its grade colour as a rim sparkle in the top right corner wherever
that corner is still clear. Idempotent: running it again rewrites the same files.
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


# Soul Searchers: thin bronze round sunglasses, stained-glass lenses (magenta, purple, green, yellow, pink), a tiny cross charm on one arm.
icon("soul_searchers", "RARE", {
    "b": 0xA8803A, "B": 0x6E5224, "m": 0xD040B8, "p": 0x8A3CD0, "g": 0x48C060, "y": 0xF0D850, "k": 0xF080B0, "t": 0x2A2A44, "w": 0xFFF4C0,
}, [
    "................",
    "................",
    ".tt..........tt.",
    ".tBbbbb..bbbbBt.",
    "..bmpgyb.bymkpb.",
    "..bpkmpbbbmpgyb.",
    "..bgymkb.bpkmgb.",
    "...bbbb...bbbb.w",
    "............y.w.",
    "............w...",
])

# The Mad King's Crown: a spiked purple-black helm-crown with jagged tines, red rune scribbles and a glowing red diamond on the brow.
icon("the_mad_kings_crown", "SPECIAL", {
    "p": 0x5A3A82, "P": 0x2E1E48, "d": 0x1A1226, "r": 0xE23A3A, "R": 0xFF8080, "l": 0x9A74C0,
}, [
    "...P....P...P...",
    "...PP..PPP..PP..",
    "..PpP..PpP.PpP..",
    "..PppP.PppPppP..",
    "..PplpPPpPPplP..",
    "..PpPrPpPrPpPP..",
    "..PpppdddddpPP..",
    "..PllpdRRdpllP..",
    "...PpPdrrdPpP...",
    "...PpPPdrdPPp...",
    "...PrpPPdPPpr...",
    "....PpP..PpP....",
    ".....PP..PP.....",
])

# The Orb of Ancient Sight: a floating glowing red orb with a slit-pupil eye above an orange horned-skull crown.
icon("the_orb_of_ancient_sight", "SPECIAL", {
    "r": 0xE83A3A, "R": 0xFF8A8A, "d": 0xA01828, "w": 0xFFF2E0, "o": 0xE08A30, "O": 0xA85A1C, "y": 0xF2B860, "k": 0x1A1210, "s": 0xF2EAD8,
}, [
    "......rrrr......",
    "....rrRRRRrr....",
    "...rRRwwwwRRr...",
    "...rRwwdrwwRr...",
    "...rRRwwwwRRr...",
    "....rrRRRRrr....",
    "......rrrr......",
    "................",
    "..y....y...y....",
    ".oyo..oyo.oyo...",
    ".okkkkkkkkkko...",
    ".oyyoOssOoyyo...",
    ".oyoOskksOoyo...",
    ".oOoOOkkOOoOo...",
    ".oyoyoyoyoyoo...",
    "..OOOOOOOOOO....",
])

# Trickster Mask: a teal skull mask with pink patterns, bold black-and-red rings round the eyes and a toothy dark jaw.
icon("trickster_mask", "SPECIAL", {
    "t": 0x3AA8A8, "T": 0x1E7080, "p": 0xF080A0, "k": 0x14161E, "r": 0xD02A2A, "w": 0xE8E8E0, "j": 0x2A3A44,
}, [
    "................",
    "....tttttttt....",
    "...ttpttttptt...",
    "..ttTtkttktTtt..",
    "..ttrrrkkrrrtt..",
    "..trkkrtkrkkrt..",
    "..trkkrttrkkrt..",
    "..ttrrtkktrrtt..",
    "...ttTtkktTtt...",
    "....ttTkkTtt....",
    "....jwkwkwkj....",
    "....jkwkwkwj....",
    ".....jkpkpj.....",
    "......jjjj......",
])

# Arcane Trickster's Glove: a purple glove with long fingers, gold rune rings and a gold chain bracelet.
icon("arcane_tricksters_glove", "SPECIAL", {
    "p": 0x8A2CB8, "P": 0x5A1A80, "l": 0xB860E0, "y": 0xE8C040, "Y": 0x9A7A20,
}, [
    "..l.....l.......",
    "..pl...lp...l...",
    "..pp...pp...pl..",
    "..pp.l.pp..lpp..",
    "..ypl..yp..pyp..",
    "..pp...pp..ppp..",
    "..pplpppplpppp..",
    "..ppppPppppppl..",
    "...pppPpppppp...",
    "....ppPppppp....",
    ".....pyypp......",
    "....YyYyYyY.....",
    "....yYyYyYy.....",
])

# Bear's Grip: brown leather clawed glove with a square steel back plate, steel finger plates, pale claw tips and shaggy fur at the cuff.
icon("bears_grip", "RARE", {
    "b": 0x7A4A28, "B": 0x4A2A14, "s": 0xB8B0B0, "S": 0x7A7480, "c": 0xE8DCC8, "f": 0x9A6A38, "F": 0xC08A50, "y": 0xC8C0A0,
}, [
    "..c..c..c..c....",
    "..s..s..s..s....",
    "..b..b..b..bc...",
    "..s..s..s..bs...",
    "..bbbbbbbbbb....",
    "..bsssssssbb....",
    "..bsSSSSSSsb....",
    "..bsSssssSsb....",
    "..bsSSSSSSsb....",
    "..bBBBBBBBBb....",
    "..bybBBBBbyb....",
    ".fFfFfFfFfFfF...",
    ".FfFfFfFfFfFf...",
])

# Dragon's Fire Gauntlet: a clawed dragon-hand gauntlet of dark scaled plates with long talons and molten lava cracks.
icon("dragons_fire_gauntlet", "LEGEND", {
    "k": 0x2A2024, "K": 0x4A3A3E, "g": 0x6A5A5E, "o": 0xF07A1A, "y": 0xFFD040, "r": 0xC83A10, "c": 0xC8B8B0,
}, [
    "..c..c..c.......",
    "..k..k..kc......",
    "..Kk.Kk.kK......",
    "..Kor.Kro.k.....",
    "..KkoKkorKk.....",
    "...KkKkKkK......",
    "...KkroyorK.....",
    "...kKoyyyoKk....",
    "....KkoyorKk....",
    "....gKkroKkg....",
    "...gKgKkKgKgc...",
    "...kgKgKgKgKk...",
    "..c.kkgkgkgk.c..",
    "...c..kkkk..c...",
])

# Gauntlet Mimic: a grey steel gauntlet with gold knuckle plates, a yellow slit-pupil eye, teeth along the cuff and a pink spotted tongue.
icon("gauntlet_mimic", "RARE", {
    "s": 0x8A8E98, "S": 0x5A5E6A, "g": 0xE0B848, "G": 0xA07E28, "e": 0xF0D820, "k": 0x14141A, "t": 0xE04C98, "T": 0xA02C70, "w": 0xF0E8D0, "b": 0x6A4028,
}, [
    "........tttt....",
    ".......tTtTTt...",
    "......tT...tT...",
    ".....w.t....t...",
    "..wwwwwwwwtt....",
    "..sssssssSs.....",
    "..sekksssSs.....",
    "..seksssSSs.....",
    "..sssssSSs.b....",
    "..bbbbbbbbb.....",
    "..gggggggg......",
    "..sSgsSgsSg.....",
    "..gGsgGsgGs.....",
    "...SgGSgGS......",
])

# Gauntlet of the Stormcaller: a heavy teal-green alloy gauntlet with scrollwork, nine blue gems and black spiked fingers, arcs of lightning.
icon("gauntlet_of_the_stormcaller", "RARE", {
    "t": 0x2A8A82, "T": 0x185A56, "k": 0x1C1C24, "K": 0x3A3A48, "b": 0x58C8FF, "w": 0xE8F8FF, "y": 0xFFF080,
}, [
    "..K..K..K..K..y.",
    "..k..k..k..kky..",
    "..kK.kK.kK.kK.y.",
    "..kkkkkkkkkk....",
    "..TttttttttT..w.",
    "..tbttTTtbtt....",
    "..TttbtttbtT....",
    "..ttTtbttTtt..w.",
    "...ttbtttbt.....",
    "...TtttbtttT....",
    "...ttTtttTtt....",
    "...tbtTkTtbt....",
    "...tt.TkT.tt....",
    "...Tt.....tT....",
])

# Glove of Appreciation: a brown leather glove giving a thumbs-up, a small red gem on the thumbnail, golden sparkles around it.
icon("glove_of_appreciation", "UNIQUE", {
    "b": 0x8A5430, "B": 0x5A3418, "l": 0xB07848, "r": 0xE23030, "y": 0xFFE070, "w": 0xFFF8D0,
}, [
    "y.....rr...y....",
    "......rbl.......",
    "......blb.......",
    "......bbl....w..",
    "..w...bbb.......",
    "....bbbbbbbb....",
    "...blbbbbbbbb...",
    "...bbbbbbbbbbB..",
    "...blbbbbbbbbB..",
    "...bbbbbbbbbB...",
    "...blbbbbbbbB.y.",
    "....bbbbbbbB....",
    ".y...BBBBBB.....",
    "......BBBB...w..",
])

# Gloves of Opportunity: pale grey-white plated gauntlets with ridged scale cuffs, one hand glowing translucent cyan with an eye in the palm.
icon("gloves_of_opportunity", "UNIQUE", {
    "w": 0xE8E8EC, "W": 0xB0B4BC, "G": 0x7A7E8A, "c": 0x58D8E8, "C": 0x2A9AB0, "e": 0xFFFFFF, "k": 0x1A2A3A,
}, [
    "................",
    ".cc.cc.cc.......",
    ".cCcCcCcCc..ww..",
    ".cCccccCc..wWWw.",
    ".cCcekecCc.wWWw.",
    ".cCccecCc.wWWWWw",
    "..cCcccCc.wWWWWw",
    "...cccCc..wWWWWw",
    "....cc....wwwwww",
    "...wwww..wWGWGWw",
    "..wWGWGw..wGWGWw",
    "..wGWGWw...GWGW.",
])

# Goristro Charger: jagged white bone gauntlets riveted with bronze studs, bronze wrist bands, black fingertips.
icon("goristro_charger", "RARE", {
    "w": 0xF0EEE8, "W": 0xB8B4AC, "b": 0xA87030, "B": 0x6A4418, "k": 0x1A1A20, "r": 0xC89040,
}, [
    "..w....w....w...",
    "..ww..www..ww...",
    "..wWw.wWw.wWw...",
    "..wWwwwWwwwWw...",
    "..wWrWWWWrWWw...",
    "..wWWWWrWWWWw...",
    "..wWWrWWWrWWw...",
    "..wWWWWWWWWWw...",
    "..BbBbBbBbBbB...",
    "..kbbbbbbbbbk...",
    "..kk.kk.kk.kk...",
    "..kk.kk.kk.kk...",
])


def render(iid, grade, pal, rows):
    assert len(rows) <= 16, iid + ": more than 16 rows"
    rows = [r.ljust(16, ".") for r in rows] + ["." * 16] * (16 - len(rows))
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
