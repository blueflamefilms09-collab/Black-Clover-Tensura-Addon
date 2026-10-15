#!/usr/bin/env python3
"""
Relics of Many Worlds, part 4, wave 28 (gauntlets, gloves, collars, medallions, pantaloons, potions): the 16x16 inventory icons of the twelve cards and their item models.

    python3 tools/relics/wave28_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
hellfire_gauntlet, proficiency_gloves, the_gauntlet_of_gamelon, chromatic_collar, collar_of_the_penitent_dragon, eye_of_the_aboleth, fenrirs_collar,
sunheart_medallion, witchers_medallion, buffoons_pantaloons, executioners_bane, perculiar_puking_poison.

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


# ------------------------------------------------------------------------------------------------------------------------------ hands
# Hellfire Gauntlet: a black spiked horned gauntlet with glowing red cracks and flames rising from the cuff.
icon("hellfire_gauntlet", "LEGEND", {
    "k": 0x17141A, "K": 0x2E2A32, "L": 0x4A4650, "s": 0x9A98A4, "r": 0xC02C10, "o": 0xFF7A1A, "y": 0xFFD040,
}, [
    "....y...o..y....",
    "...yo..oyo.oy...",
    "...oyo.oyooyo...",
    "....oyoyyyoo....",
    "....kKKKKKKk....",
    "...kKLrKKrLKk...",
    "..skKKKrrKKKks..",
    "..kKLKrorrKLKk..",
    "...kKKKrKKKKk...",
    "..skKLLKKLLKks..",
    "..kKKKKKKKKKKk..",
    "..kKLKsKKsKLKk..",
    "..kKKkkkkkkKKk..",
    "...kKKKKKKKKk...",
    "....kkkkkkkk....",
    "................",
])

# Proficiency Gloves: a pair of brown fingerless gauntlets, gold back plates, a chain-mail strip over the knuckles.
icon("proficiency_gloves", "SPECIAL", {
    "b": 0x6A4020, "B": 0x8E5E34, "d": 0x3E2412, "y": 0xE8B830, "Y": 0xFFE070, "c": 0xB4B8C0, "C": 0x80848C,
}, [
    "................",
    ".cCcC.....cCcC..",
    ".cCcCc...cCcCc..",
    ".bBbBb...bBbBb..",
    ".bBBBBb.bBBBBb..",
    ".bByYyB.bByYyB..",
    ".bByYyB.bByYyB..",
    ".bByYyB.bByYyB..",
    ".bBByyB.bBByyB..",
    ".dbBYyBbdbBYyBb.",
    "..dbBBBb.dbBBBb.",
    "..dbbbbb.dbbbbb.",
    "...dbbb...dbbb..",
    "................",
    "................",
    "................",
])

# The Gauntlet of Gamelon: a purple and gold armoured gauntlet with four gems (earth, fire, water, air) along the back of the hand.
icon("the_gauntlet_of_gamelon", "RARE", {
    "p": 0x4A2E6A, "P": 0x6A4A90, "d": 0x2A1A40, "y": 0xD8B450, "Y": 0xFFE090, "g": 0x40C060, "r": 0xE04030, "b": 0x3C80F0, "w": 0xF4F4FF,
}, [
    "................",
    "....dppppppd....",
    "...dpPPPPPPpd...",
    "..dpPyyyyyyPpd..",
    "..dpygrbwyyPpd..",
    "..dpPyyyyyyPpd..",
    "..dppPPPPPPppd..",
    "...dpPpPPpPpd...",
    "...dpPpPPpPpd...",
    "..dyyPPPPPPyyd..",
    "..dpYyyyyyyYpd..",
    "..dpppppppppd...",
    "...dddddddddd...",
    "................",
    "................",
    "................",
])

# ------------------------------------------------------------------------------------------------------------------------------ neck
# Chromatic Collar: a black scaled dragon-hide band seen from the front, five gems (green, red, blue, white, gold) along it.
icon("chromatic_collar", "RARE", {
    "k": 0x1E2024, "K": 0x363A44, "L": 0x545A68, "g": 0x40C060, "r": 0xE04030, "b": 0x3C80F0, "w": 0xF4F4FF, "y": 0xF0C040,
}, [
    "................",
    "................",
    "...kkkkkkkkkk...",
    "..kKLKLKLKLKLk..",
    ".kKLKLKLKLKLKLk.",
    ".kLKLKgKrKbKLKk.",
    ".kKLKLKLKLKLKLk.",
    ".kLKwKLKLKyKLKk.",
    ".kKLKLKLKLKLKLk.",
    "..kKLKLKLKLKLk..",
    "...kkKLKLKLkk...",
    "....kkkkkkkk....",
    "................",
    "................",
    "................",
    "................",
])

# Collar of the Penitent Dragon: a wide silver band with a sculpted dragon-head emblem and a ring hanging below it.
icon("collar_of_the_penitent_dragon", "SPECIAL", {
    "s": 0x8A8E98, "S": 0xB4B8C4, "L": 0xDCE0EA, "d": 0x4A4E58, "e": 0xFFF2B0, "k": 0x2A2C34,
}, [
    "................",
    "................",
    "...dssssssssd...",
    "..dsSLLLLLLSsd..",
    ".dsSLsssssssLSd.",
    ".dsSsLLLLLLsSsd.",
    ".dssSsSsSsSSssd.",
    "..dssSdSSdSssd..",
    "...dsSdeedSsd...",
    "....dsSddSsd....",
    ".....dsSSsd.....",
    "......dkkd......",
    ".....dS..Sd.....",
    ".....dS..Sd.....",
    "......dSSd......",
    "................",
])

# Eye of the Aboleth: a thin silver loop hanging from a curling setting with a glowing yellow slit-pupil eye.
icon("eye_of_the_aboleth", "SPECIAL", {
    "s": 0xB8BCC8, "S": 0x7A7E90, "d": 0x40445A, "y": 0xF0E040, "Y": 0xFFF8A0, "k": 0x1A1A22,
}, [
    "......ssss......",
    ".....s....s.....",
    ".....s....s.....",
    ".....s....s.....",
    ".....s....s.....",
    "......s..s......",
    "......s..S......",
    ".....SsssSd.....",
    "....SsyYYysS....",
    "...SsyYkkYysS...",
    "...SsyYkkYysS...",
    "....SsyYYysS....",
    ".....SsssSd.....",
    "..ss..dSSd..ss..",
    ".s..s..dd..s..s.",
    "..ss........ss..",
])

# Fenrir's Collar: a crescent of dark scaled metal with cream-gold filigree and a leaf-shaped crest holding a round red ruby.
icon("fenrirs_collar", "UNIQUE", {
    "k": 0x20222A, "K": 0x3A3E4A, "L": 0x565C6C, "y": 0xE0C878, "Y": 0xFFF0B0, "r": 0xD02030, "R": 0xFF6070, "o": 0x8A1020,
}, [
    "................",
    "..kk........kk..",
    ".kKKk......kKKk.",
    ".kKLKk....kKLKk.",
    ".kKyLKkkkkKLyKk.",
    "..kKyLKLLKLyKk..",
    "..kKLyyYYyyLKk..",
    "...kKLyYyyLKk...",
    "...kKyyrRyyKk...",
    "....kyyRrryyk...",
    "....kKyroryKk...",
    ".....kyyooyk....",
    ".....kKyyyKk....",
    "......kkyykk....",
    ".......kkkk.....",
    "................",
])

# Sunheart Medallion: a golden sun medallion on a fine chain: a glowing amber orb in the middle, curling rays all round.
icon("sunheart_medallion", "LEGEND", {
    "g": 0xB88A28, "G": 0xE0A030, "y": 0xFFD870, "Y": 0xFFF4B0, "a": 0xFF9A20, "w": 0xFFFFFF, "c": 0xA8A090,
}, [
    "......c..c......",
    ".......cc.......",
    "...y...cc...y...",
    "....yG.cc.Gy....",
    "..y..GGGGGG..y..",
    "...GGGaaaaGGG...",
    "..yGGaYYYYaGGy..",
    ".GGGaYwwwwYaGGG.",
    ".yGGaYwwwwYaGGy.",
    "..GGGaYYYYaGGG..",
    "...GGGaaaaGGG...",
    "..y.GGGGGGGG.y..",
    "...y..GGGG..y...",
    "......gGGg......",
    ".......gg.......",
    "................",
])

# Witcher's Medallion: a dark steel wolf's head (spiky fur, snarling mouth, glowing red eyes) on a black chain.
icon("witchers_medallion", "LEGEND", {
    "k": 0x14161A, "K": 0x30343C, "L": 0x5A606C, "s": 0xAEB4C0, "r": 0xE02020, "R": 0xFF8080, "c": 0x44484F,
}, [
    "......cccc......",
    ".....c....c.....",
    ".....c....c.....",
    "......c..c......",
    "..k...kccK...k..",
    "..kKk.kKKk.kKk..",
    "...kKkKLLKkKk...",
    "..kKLKLLLLKLKk..",
    "...kKrRKKRrKk...",
    "..kKLKrKKrKLKk..",
    "...kKLKssKLKk...",
    "....kKkssKkK....",
    ".....kKsskK.....",
    "......kssk......",
    ".......kk.......",
    "................",
])

# ------------------------------------------------------------------------------------------------------------------------------ belt
# Buffoon's Pantaloons: baggy bloomers fading from blue-purple at the waist to hot pink, cyan triangles down the sides, ruffled cuffs.
icon("buffoons_pantaloons", "RARE", {
    "u": 0x5A3AC0, "U": 0x7A44D0, "m": 0xB03AC8, "p": 0xE040A8, "P": 0xFF6AC0, "c": 0x40D8E8, "d": 0x3A2480,
}, [
    "................",
    "..dddddddddddd..",
    "..duUUUUUUUUud..",
    ".dcuUUUUUUUUucd.",
    ".dcuUUUmmUUUucd.",
    ".dcumUUmmUUmucd.",
    ".dcmmmmmmmmmmcd.",
    ".dcmmmmddmmmmcd.",
    ".dpmmmm..mmmmpd.",
    ".dppmmm..mmmppd.",
    ".dpppmm..mmpppd.",
    ".dpppp....ppppd.",
    "dPpPpPd..dPpPpPd",
    "dPpPpPd..dPpPpPd",
    ".PpPpP....PpPpP.",
    "................",
])

# ------------------------------------------------------------------------------------------------------------------------------ potions
# Executioner's Bane: an elongated black crystal vial with a faceted point, copper-gold fittings and a looping gold chain.
icon("executioners_bane", "UNIQUE", {
    "k": 0x14141A, "K": 0x2C2C38, "L": 0x4C4C5E, "g": 0xC8903A, "G": 0xF0C060, "d": 0x7A5018, "w": 0xE8E8F8,
}, [
    "............kk..",
    "...........kLKk.",
    "..........kLKKk.",
    "........ggGGGk..",
    ".......gGdddGg..",
    "......kKGGddGk..",
    ".....kLKKGGgKk..",
    "....kLwKKKKKk...",
    "...kLwKKKKKk....",
    "..kLKKKKKKk.G...",
    "..kLKKKKKk..G...",
    ".GkKKKKKk..G.G..",
    ".G.kKKKk..G...G.",
    ".G..kKk...G...G.",
    "..G..k...G...G..",
    "...GGGGGGG.GG...",
])

# Perculiar Puking Poison: a glass orb swirling with rainbow slime, held in a dark spiked metal claw.
icon("perculiar_puking_poison", "SPECIAL", {
    "k": 0x24202A, "K": 0x48424E, "s": 0xA8A0B0, "w": 0xF4F8FF, "r": 0xF04060, "o": 0xF0A030, "y": 0xE8E040, "g": 0x40D070, "b": 0x40A0F0, "v": 0xA060F0,
}, [
    "....k.k.k.k.....",
    "...kKkKkKkKk....",
    "....kKKKKKk.....",
    ".....kKKKk......",
    "....wwwwwww.....",
    "...wrrooyyww....",
    "..wrroyyggbbw...",
    "..wroyyggbbvw...",
    "..wooygbbvvrw...",
    "..wyyggbvvrrw...",
    "...wggbbvrrw....",
    "....wbbvvrw.....",
    "..k.kKsssKk.k...",
    "..kkkKkkkKkkk...",
    "...k.k...k.k....",
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
