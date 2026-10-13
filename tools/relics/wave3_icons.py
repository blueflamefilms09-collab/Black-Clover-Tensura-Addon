#!/usr/bin/env python3
"""
Relics of Many Worlds, wave 3 (head, hands, belt and consumables): the 16x16 inventory icons of the thirteen cards and their item models.

    python3 tools/relics/wave3_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
arcanists_circlet, prides_crown, dreamwalker_cowl, captains_tricorn, godking_crown, claws_of_greed, gloves_of_instant_cleanliness,
elemental_hand_wraps, thermo_gloves, gauntlets_of_eldritch_ferocity, alchemists_belt, genie_gin, mad_marbles.

Each icon is an ASCII pixel map (16 rows of 16 characters, one palette per icon) so that every pixel is deliberate. A one pixel dark outline is
grown round the shape automatically (from the colour it touches), and every icon gets its grade colour as a rim sparkle in the top right corner
(Special green, Rare aqua, Unique pink, God red) plus the glow pixels the map itself carries. Idempotent: running it again rewrites the same files.
With --sheet it also writes an enlarged contact sheet for looking at them.
"""
import argparse
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

GRADE_RGB = {"SPECIAL": (0x55, 0xFF, 0x55), "RARE": (0x55, 0xFF, 0xFF), "UNIQUE": (0xFF, 0x77, 0xFF), "GOD": (0xFF, 0x55, 0x55)}


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


# ---------------------------------------------------------------------------------------------------------------------------- the maps
# (id, grade, palette, rows). '.' is clear. Rows are checked to be 16 wide.
ICONS = []


def icon(iid, grade, palette, rows):
    ICONS.append((iid, grade, {k: hexc(v) for k, v in palette.items()}, rows))


# Arcanist's Circlet: a silver circlet seen a little from above (the back of the band shows through), a pointed centre set with a black opal, three motes circling above it.
icon("arcanists_circlet", "SPECIAL", {
    "S": 0xEEF1F8, "s": 0xBAC3D4, "d": 0x7E879E, "e": 0x575F78,
    "g": 0x1E0C48, "G": 0x5B35C2, "h": 0xFF86E0, "j": 0x62E8FF,
    "t": 0x6FF7E6, "T": 0xD2FFF8, "p": 0xFF7FD9, "P": 0xFFD0F2, "v": 0xA98BFF, "V": 0xE6DBFF,
}, [
    ".......p........",
    "..t...pPp....v..",
    ".tTt...p....vVv.",
    "..t.........v...",
    ".......SS.......",
    "...ssssSSsss....",
    "..sd...SgGd.ss..",
    ".sd....SgjGd..s.",
    "sd.....SgGhGd..s",
    "sd.....dgGGd...s",
    "sSd...sdSGGdd.dS",
    ".sSddsSSSgGSSdSs",
    "..sSSsSSsSSsSSs.",
    "...dssdsdsdsds..",
    ".....ddddddd....",
    "................",
])

# Pride's Crown: a gold spiked crown with a tongue of flame, a tattoo-like flourish beneath it.
icon("prides_crown", "RARE", {
    "Y": 0xFFE680, "y": 0xF0B63A, "o": 0xC27A1E, "b": 0x7C4310,
    "f": 0xFF8A1E, "F": 0xFFD23C, "r": 0xE0481C, "w": 0xFFF6C8, "c": 0xE22E3C,
}, [
    ".......F........",
    "......fF.F......",
    "......fFFF......",
    ".....rfFFFf.....",
    ".....rfFFFf.....",
    "..Y...rfFfr..Y..",
    "..yY..ryyr..yY..",
    "..yyY.yYYy.Yyy..",
    "..yyyYyYYyYyyy..",
    "..yyyyyYYyyyyy..",
    "..oyyyyyyyyyyo..",
    "..ooccoyycoooo..",
    "..bbooooooobbb..",
    "...b.o.bb.o.b...",
    ".....b.oo.b.....",
    "................",
])

# Dreamwalker Cowl: a hood and long scarf tails of stitched panels (green, purple, maroon, gold), a dream-purple haze behind.
icon("dreamwalker_cowl", "RARE", {
    "G": 0x3E8A4A, "g": 0x25552E, "P": 0x8E56C8, "p": 0x5B3490, "M": 0xA02E48, "m": 0x641A2C,
    "Y": 0xE6C14E, "y": 0xB08A24, "k": 0x17111F, "h": 0xC9A0F2, "w": 0xFFF2C8,
}, [
    "................",
    ".....mMMm.......",
    "....mMGGPMm.....",
    "...mMGGGPPMm....",
    "...MGGkkkPPM....",
    "...GGkkkkkPP....",
    "...gGkkkkkPp....",
    "....gGPPPPpp....",
    "...PpGMMYyPP....",
    "..PPpgMMYGGpP...",
    "..pPPGGMMGPP....",
    "..ppGYGMMPGp....",
    "..pgGGGmmpGP....",
    "...gGPPYmPg.....",
    "...gpPgmMp......",
    "....g.pm.p......",
])

# Captain's Tricorn: a black three-cornered hat with silver braid, a long pink feather, a hanging green feather and coin medals.
icon("captains_tricorn", "RARE", {
    "K": 0x2B2B34, "k": 0x45454F, "L": 0x6C6C7A, "S": 0xE2E6EE, "s": 0xA9AFBE,
    "F": 0xFF6E9A, "f": 0xFFB6CE, "w": 0xFFFFFF, "g": 0x35C24E, "G": 0x8CF08A, "c": 0xBFC4D2,
}, [
    "..........fFF...",
    "........fFFFf...",
    "....KKKKfFFf....",
    "...KkkKKKFFK....",
    "S.KkKKKKKKKKK.S.",
    "SSKkKKKKKcsKKSS.",
    "sSKKKKKKKcSsKSSs",
    ".SSKKKKKKKcKKSS.",
    "..sSSKKKKKKSSs..",
    "...ssSSKKSSss...",
    ".....ssSSss.....",
    "..c....ss...g...",
    ".cSc........gG..",
    "..c.........g...",
    "................",
    "................",
])

# Godking Crown: a dark green-grey spiked crown set with mint gems, wreathed in floating mint flames.
icon("godking_crown", "GOD", {
    "D": 0x3C4A44, "d": 0x27322E, "L": 0x61766C, "l": 0x84A090, "k": 0x121816,
    "m": 0x5CF0B4, "M": 0xC6FFE8, "n": 0x2BB584,
}, [
    ".......M........",
    ".m....mM.....m..",
    "mMm..mMMm...mMm.",
    ".m....mm.....m..",
    ".......LL.......",
    ".......LD.......",
    ".L.....DD.....L.",
    ".LD..L.DD.L..LD.",
    ".LDD.LDDDDDL.DDd",
    ".LDDLDDDDDDDLDDd",
    ".LDDDDDDmMDDDDDd",
    ".LDDDDDDmmDDDDdd",
    ".DDDDmDDDDDDmDdd",
    ".dLlDDDDDDDDLlDd",
    "..dddddddddddd..",
    "................",
])

# Claws of Greed: a bone-coloured knuckle with long curved black talons, gold veins in the cracked bone.
icon("claws_of_greed", "SPECIAL", {
    "B": 0xE2CFA0, "b": 0xB89F6E, "d": 0x7B6743, "k": 0x1A1410,
    "Y": 0xFFC83C, "y": 0xF08A1E, "T": 0x4A4440, "t": 0x8C8278, "w": 0xFFF0C0,
}, [
    "....BBBBBBBB....",
    "...BwBBYBBBBb...",
    "..BBBBBYYBBBbd..",
    "..BbBYBBBBYBbd..",
    "..BBbBbBBbBbbd..",
    "..Bd.Bb.Bb.dbd..",
    ".Tt..Bb.Bb..tT..",
    ".Tt..Tt.Tt..tT..",
    ".Tt..Tt.Tt..tT..",
    "TTt..Tt.Tt..tTT.",
    "Tt...Tt..Tt..tT.",
    "Tt...tT..Tt..tT.",
    "T....tT...tT..T.",
    ".....T.....T....",
    "................",
    "................",
])

# Gloves of Instant Cleanliness: a pair of blue-grey plated gauntlets, silver edged, gold shield plates on the backs, a clean sparkle.
icon("gloves_of_instant_cleanliness", "SPECIAL", {
    "B": 0x7C8DB0, "b": 0x55658A, "n": 0x38456A, "S": 0xE6ECFA, "s": 0xB4BFD6,
    "Y": 0xFFD866, "y": 0xD6A030, "o": 0x9A6A1E, "w": 0xFFFFFF, "c": 0x8FE8FF,
}, [
    "................",
    "..B.B.B.B.B.B.B.",
    "..B.B.B.B.B.B.B.",
    "..BbBbBbBbBbBbBb",
    ".BBBBBBBB.BBBBBB",
    ".BbYYYYbb.bYYYYb",
    ".BbYoYybb.bYoYyb",
    ".BbsYyYsb.bsYyYb",
    "..bbsYssb.bbsYsb",
    "..nbbsssn.nbbssn",
    "..SSSSSSS.SSSSSS",
    "..sssssss.ssssss",
    "..nnnnnnn.nnnnnn",
    ".c..............",
    "cwc...........c.",
    ".c...........cwc",
])

# Elemental Hand Wraps: a roll of ribbon (gold edged: red flame, blue lightning, pale cyan frost, green acid) with a loose end trailing from it.
icon("elemental_hand_wraps", "UNIQUE", {
    "R": 0xC93A34, "r": 0x8E2222, "F": 0xFFAE3C, "B": 0x2F4FA8, "b": 0x1E3070, "Z": 0xFFF05A,
    "C": 0x9DE8F0, "c": 0x62B6C8, "G": 0x58B848, "g": 0x347A2C, "Y": 0xE8C34A, "y": 0xA8821E, "w": 0xFFFFFF,
}, [
    "................",
    "....YYYYYY......",
    "..YYRRRRRRYY....",
    ".YRRRYYYYRRRYY..",
    ".YRYYBBBBYYRRYy.",
    "YRYBBYYYYBBYRRYy",
    "YRYBYYCCYYBYRrYy",
    "YRYBYCwGGCYBYrYy",
    "YRYBYYCGgYYBYrYy",
    "YRYBBYYYYBBYRrYy",
    ".YRYYBBBBYYRrYyy",
    ".YRRRYYYYRrrYyy.",
    "..YYRRRRrrYYy.CC",
    "....YYYYYyyCCCcc",
    "..........CCcc..",
    "..........Cc....",
])

# Thermo-Gloves: a red glove (flame, triangle rune) and a blue-white glove (frost, inverted triangle rune), side by side.
icon("thermo_gloves", "SPECIAL", {
    "R": 0xB02A2E, "r": 0x741820, "D": 0x4A0E16, "F": 0xFF9A3C, "Y": 0xFFE06A,
    "C": 0xDFF6FF, "c": 0x7FC4E4, "b": 0x3A7CB0, "n": 0x1C4A78, "w": 0xFFFFFF, "p": 0xE8B0A0,
}, [
    ".F.F.F.......w.w",
    ".R.R.R....C.C.C.",
    ".R.R.R.R..C.C.Cc",
    ".RRRRRRR..CCCCCc",
    "RRRRRRRR..CCCCCc",
    "RRRRrrRR..CcCCCc",
    "RRRrYrRR..CcwCcb",
    ".RRrYYrR..cbcwcb",
    ".rRRrrRr..cbbwcb",
    "..rrRRrr..cbbbbn",
    "..rrrrrr..nbbbbn",
    "..DrrrrD..nnbbnn",
    "..DDDDDD..nnnnnn",
    "...DDDD....nnnn.",
    "................",
    "................",
])

# Gauntlets of Eldritch Ferocity: dark leather gauntlets plated with grey stone carved with teal runes, teal wisps from the fingers.
icon("gauntlets_of_eldritch_ferocity", "RARE", {
    "L": 0x5E4636, "l": 0x3C2A20, "d": 0x23170F, "S": 0x8A8E96, "s": 0x5E626E, "e": 0x3A3D48,
    "T": 0x52F2E0, "t": 0x1FA698, "W": 0xB8FFF4,
}, [
    "..T..W...T......",
    ".tWT..TtW.T.....",
    "..t..tT..tT.....",
    "..LL.LL.LL.LL...",
    "..LLLLLLLLLLLl..",
    ".LLLLLLLLLLLLl..",
    "LLLlSSsTSSsLLl..",
    "LLLlSTSsSTSLLl..",
    ".LLlSsSTsSsLl...",
    "..LlSSTsSSSl....",
    "..ldSsSTsdl.....",
    "..lSSSSSSSSl....",
    "..lSeSTSeSSl....",
    "..ddeeeeeedd....",
    "...dddddddd.....",
    "................",
])

# Alchemist's Belt: a wide brown leather belt with a steel plate, hung with corked vials of six colours.
icon("alchemists_belt", "UNIQUE", {
    "L": 0x8A5A34, "l": 0x5E3A1E, "d": 0x3A220E, "S": 0xB8C0CC, "s": 0x7C8494, "Y": 0xE8C34A,
    "R": 0xE0343A, "B": 0x3A78E8, "G": 0x3CC85A, "A": 0xFFC83C, "P": 0xA050E0, "K": 0x2A2230,
    "c": 0xE8D8B0, "w": 0xFFFFFF,
}, [
    "................",
    "..cc..cc..cc....",
    "..RR..BB..GG..cc",
    ".RwRR.BwB.GwG.AA",
    ".RRRR.BBB.GGG.AA",
    ".RRRR.BBB.GGG.AA",
    "LLLLLLLLLLLLLLLL",
    "LlLLSSSSSSLLLLlL",
    "lLLYSssssSYLLLLl",
    "LLLlSsYYsSlLLLLL",
    "dlLLSSSSSSLLLLld",
    "ddlLLLLLLLLLllld",
    "..PP..KK..PP..c.",
    ".PwPP.KKK.PPP.c.",
    ".PPPP.KKK.PPP...",
    "..PP...K..PP....",
])

# Genie Gin: a pink-mauve glass bottle with an engraved swirl and label, the cork popped, a pastel genie smoke curling out of it.
icon("genie_gin", "UNIQUE", {
    "G": 0xC48AA8, "g": 0x8A5A7C, "D": 0x5E3A58, "H": 0xF4D2E6, "w": 0xFFFFFF, "C": 0x8B5A3A, "c": 0x5A3820,
    "T": 0x8AF0DC, "t": 0xB8A0F2, "u": 0xDCC8FF, "n": 0xA8F8E8, "l": 0xF4E8B0, "q": 0xFFF6DC,
}, [
    "..nT.......CC...",
    ".nTuT..tT..cCc..",
    "..TuTtTuT...c...",
    "...tTuTt........",
    "......HGg.......",
    ".......Gg.......",
    ".......Gg.......",
    "......HGGg......",
    ".....HGGGGg.....",
    "....HGGGGGGg....",
    "....HGqqqqGg....",
    "....GGqDlqGg....",
    "....GGqlDqGg....",
    "....GGqqqqgg....",
    ".....GGgggD.....",
    "......DDDD......",
])

# Mad Marbles: a brown stitched drawstring sack tied with rope, tag on a string, glass marbles with coloured lightning spilling out.
icon("mad_marbles", "SPECIAL", {
    "B": 0xA8704A, "b": 0x7C4A2E, "d": 0x4E2C1A, "R": 0x9AA85A, "r": 0x5E6A34, "T": 0xD2C49A, "t": 0x8C7C50,
    "M": 0xD8EEF0, "m": 0x9CC4D0, "P": 0xE250D8, "O": 0xFF9A2E, "C": 0x2EE8D8, "G": 0x58E848, "s": 0xE6C890,
}, [
    "................",
    ".......dd.......",
    "......dBBd......",
    ".....dBBBBd.....",
    "......RrRr......",
    ".....bBRRBb.....",
    "....dBBsBBBd....",
    "...dBBBsBBBBd...",
    "..dBBbBBsBBBbd..",
    "..dBBbBBBsBBbd..",
    "..dBBBbBBBsbBd..",
    "...dBBbbBBBbd.MM",
    "....ddBBBbdd.MPm",
    "..TT..dddd..MmCm",
    ".TtTT...MM...MM.",
    ".TTTT..MOOm.....",
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
            op[x, y] = tuple(int(c * k + 255 * (1 - k) * 0.0) for c in gr) + (255 if k == 1.0 else 190,)
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
        cols = 5
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
