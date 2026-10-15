#!/usr/bin/env python3
"""
Relics of Many Worlds, part 4, wave 24 (twelve charms): the 16x16 inventory icons and their item models.

    python3 tools/relics/wave24_icons.py [--sheet PATH]

Writes src/main/resources/assets/nusmp/textures/item/<id>.png and models/item/<id>.json (parent item/generated) for:
instant_cooking_pot, lucky_egg, lumpy, monster_compass, official_looking_badge, orb_of_plagues, orb_of_the_dryad, pocket_time,
ring_of_endless_rage, ring_of_memories, ring_of_power, ring_of_regeneration.

Each icon is an ASCII pixel map (16 rows of 16 characters, one palette per icon) so every pixel is deliberate; the four rings share one band
(rows 8 to 13) and differ in the stone or leaves above it. A one pixel dark outline is grown round the shape automatically (from the colour it
touches) and every icon gets its grade colour as a rim sparkle in the top right corner where that corner is still clear.
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


# ---------------------------------------------------------------------------------------------------------------------------- Instant Cooking Pot
# A small grey saucepan seen from the side, black handle on the left, scorched brown bottom and painted flames (o orange, y yellow).
icon("instant_cooking_pot", "SPECIAL", {
    "R": 0xC8CCD4, "G": 0x8E929C, "g": 0x5E626C, "b": 0x6B4A2A, "o": 0xFF8A2A, "y": 0xFFD04A, "K": 0x2A2A30, "H": 0xA8ACB4,
}, [
    "................",
    "................",
    "................",
    "................",
    ".....RRRRRRRRR..",
    ".....GRGGGGGGGg.",
    "KKKKHHGGGGGGGGg.",
    "KKKKHHGGoGGGoGg.",
    "....HGGGoyGGoyg.",
    ".....GGoyyoGoyg.",
    ".....GoyyyoooyG.",
    ".....bbbbbbbbbg.",
    "......bbbbbbbg..",
    "................",
    "................",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Lucky Egg
# A glossy egg, black above fading to silver below, with a white shine, hanging by a gold ring from a short gold chain.
icon("lucky_egg", "SPECIAL", {
    "g": 0xE8C860, "d": 0xA88A30, "K": 0x16161C, "k": 0x30303A, "w": 0xF4F8FF, "S": 0xDDE4EE, "s": 0x9AA4B4,
}, [
    "......gggg......",
    ".....g....g.....",
    "....g..dd..g....",
    ".....g.dd.g.....",
    "......kKKk......",
    ".....KKwwKKK....",
    "....KKKwKKKKK...",
    "....KKKKKKKKK...",
    "....KKKKKKKKK...",
    "....kKKKKKKKk...",
    "....SSkkkkkSS...",
    "....SSSSSSSSS...",
    ".....SSSSSSS....",
    ".....sSSSSSs....",
    "......ssss......",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Lumpy
# The felt hand puppet: pointed blue hat with an orange pom-pom, a pale face, an orange neckerchief, a blue robe, two little hands.
icon("lumpy", "SPECIAL", {
    "o": 0xF08A2A, "b": 0x4A78D8, "B": 0x7AA4F0, "d": 0x2E4C98, "S": 0xF0D0A8, "s": 0xC8A47C, "e": 0x1C1C24, "m": 0x8A2A3A, "n": 0xF08A2A, "N": 0xC06418,
}, [
    "...........oo...",
    "..........bBb...",
    ".........bBbb...",
    "........bBbbb...",
    ".......bbbbbbb..",
    "......SSSSSSSs..",
    "......SeSSSeSs..",
    "......SSSSSSSs..",
    "......SmmmmmSs..",
    ".....nnnnnnnnn..",
    "....bbnnNnnbbb..",
    "...bBbbbNbbbbbb.",
    "..SbbbbbbbbbbbS.",
    "..SsbbbbbbbbbsS.",
    "....dddddddddd..",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Monster Compass
# An octagonal red lacquered case, the lid open on the left with a yellow slit-pupil eye, the base on the right with a silver needle on a red dial;
# pale claw-horns (h) on the rim.
icon("monster_compass", "SPECIAL", {
    "r": 0x6E1E18, "R": 0xB02A22, "L": 0xD84A3A, "y": 0xF0E030, "Y": 0xFFFC80, "K": 0x1C1C24, "h": 0xE8E0D0, "S": 0xC8C8D4, "n": 0x8A8A98,
}, [
    "..rrrrrr........",
    ".rRRRRRRr.......",
    ".rRRyYyRr.......",
    "hrRyKYKyr.......",
    ".rRRyYyRr.......",
    "..rRRRRRr.rrrrr.",
    "...rrrrrr.rRRRRr",
    ".......rrrrLRRRr",
    "......rRRSSSSRRr",
    "h.....rRSnKnSRRr",
    "......rRRSSSSRRr",
    "h.....rRRRRRRRRr",
    ".......rrrrrrrr.",
    ".........hh.....",
    "................",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Official-Looking Badge
# A shield-shaped gold badge on a short chain, two crossed hammers stamped in it (h) and a silver plaque with runes at the foot.
icon("official_looking_badge", "SPECIAL", {
    "c": 0x9AA0AC, "g": 0xB8921E, "G": 0xE8C23A, "L": 0xFFE27A, "h": 0x9A7A14, "s": 0xC8C8D4, "S": 0x7A7A88,
}, [
    "......c.c.......",
    ".......c........",
    "...ggggggggg....",
    "..gGLGGGGGGGg...",
    "..gGhGGGGGhGg...",
    "..gGGhGGGhGGg...",
    "..gGGGhGhGGGg...",
    "..gGGGGhGGGGg...",
    "..gGGGhGhGGGg...",
    "..gGGhGGGhGGg...",
    "...gGhGGGhGg....",
    "...gGsSsSsGg....",
    "....gGsSsSg.....",
    ".....ggGGgg.....",
    ".......gg.......",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Orb of Plagues
# A murky olive-gold sphere (o, O, highlight G) trailing green vapour (v) to the right, held in a cage of black thorny roots (k) with green sap (V).
icon("orb_of_plagues", "UNIQUE", {
    "o": 0x6E6A1E, "O": 0xB0A436, "G": 0xF0E070, "k": 0x1C1814, "V": 0x4FA83A, "v": 0x7CD84A,
}, [
    "................",
    "....oooooo......",
    "...oOOOOOOo..vv.",
    "..oOOGGOOOo.vVv.",
    "..oOGGOOOOoovVv.",
    "..oOOOOOoOo.vv..",
    "..oOOOOOOOovv...",
    "..oOOOOOOOo.vv..",
    "...oOOOOOOo.v...",
    "....oookoo......",
    "..kkkkkkkkkk....",
    "...kVkkkVkk.....",
    "..kkkkkkkkkk....",
    "...kkVkkkVk.....",
    "....kkkkkk......",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Orb of the Dryad
# A sphere of carved golden-brown wood with swirling spiral grooves (s), pale green mist (m) round it and glowing green motes (G).
icon("orb_of_the_dryad", "SPECIAL", {
    "d": 0x5C3A18, "B": 0xC8982E, "s": 0x7A5420, "G": 0x6CFF7A, "m": 0xB8E8B8, "M": 0xDCF4D8,
}, [
    "..m.......G.m...",
    "...mmdddddd.....",
    "..mmdBBBBBBd.G..",
    ".mmdBBsssBBBd...",
    ".mdBBsBBBsBBd..G",
    ".mdBsBBGBsBBd...",
    ".mdBsBBBBsBBd...",
    ".mdBBsssBBBsd...",
    "G.dBBBBBBBsBd.m.",
    "..mdBGBBsssBd...",
    "...mmdddddd.....",
    "..m.mmmmm...G...",
    "....M..m.m......",
    "................",
    "................",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- Pocket Time
# A round golden pocket watch with a ring at the top: cream face, black ticks at 12, 3, 6, 9, a hand pointing up and right.
icon("pocket_time", "SPECIAL", {
    "g": 0xB8921E, "G": 0xE8C23A, "L": 0xFFF0A0, "W": 0xFFF4C8, "K": 0x2A2418, "h": 0x6A4A10,
}, [
    "......gggg......",
    ".....g....g.....",
    "......gGGg......",
    "....gggggggg....",
    "..ggGGGGGGGGgg..",
    ".gGWWWWKWWWWWGg.",
    ".gGWWWWWWWWWWGg.",
    ".gGWWWWWWWhWWGg.",
    ".gGWWWWWWhWWWGg.",
    ".gGKWWWWKWWWKGg.",
    ".gGWWWWWWWWWWGg.",
    ".gGWWWWKWWWWWGg.",
    "..ggGGGGGGGGgg..",
    "....gggggggg....",
    "................",
    "................",
])

# ---------------------------------------------------------------------------------------------------------------------------- the four rings
# Shared band (rows 8 to 13): s edge, S body, d engraving, h highlight. Each ring then has its stone or leaves in rows 1 to 7.
BAND = [
    "...ssssssssss...",
    "..sShSSSSSSSSs..",
    "..sSSSSSSSSSSs..",
    "..sSSSSSSSSSSs..",
    "...sSSSSSSSSs...",
    "....ssssssss....",
]


def ring(iid, grade, pal, top, band=None):
    assert len(top) == 8, iid
    rows = list(top) + list(band or BAND) + ["", ""]
    rows = rows[:16]
    rows += ["................"] * (16 - len(rows))
    rows = [r if r else "................" for r in rows]
    icon(iid, grade, pal, rows)


# Ring of Endless Rage: a silver band with runes, a thorn spike, a big blood-red ruby held between two crescent-moon prongs.
RAGE_BAND = [
    "...ssssssssss...",
    "..sShSSSSSSSSs..",
    "..sSdSSSSSSSSs..",
    "..sSSSSSSSSSSs..",
    "...sSSSttSSSs...",
    "....ssssssss....",
]
ring("ring_of_endless_rage", "SPECIAL", {
    "s": 0x6E7480, "S": 0xB8BCC8, "h": 0xE8ECF4, "d": 0x4A4E5A, "t": 0x8A1A22, "c": 0xD0D4DE, "r": 0x8A0E1A, "R": 0xD81E32, "L": 0xFF8A98,
}, [
    "................",
    ".....rrrrrr.....",
    "....rRRLRRRr....",
    "..c.rRRRRRRr.c..",
    "..cc.rRRRRr.cc..",
    "...ccrrRRrrcc...",
    "....cc.rr.cc....",
    ".....cc..cc.....",
], RAGE_BAND)

# Ring of Memories: a dark engraved band with thin looping wire (w) round a long pale glowing cyan crystal (c light, C core).
ring("ring_of_memories", "RARE", {
    "s": 0x2A2C34, "S": 0x4A4C56, "h": 0x7A7E8C, "w": 0x9AA0AC, "c": 0x5CC8D8, "C": 0xCFFAFA,
}, [
    "......cc........",
    ".....cCCc.ww....",
    "...w.cCCc.w.w...",
    "..w..cCcc...w...",
    "..w..cCCc..w....",
    "...w.cCCc.w.....",
    "....wwcCcw......",
    "......cc........",
])

# Ring of Power: a rose-bronze band holding a cube-shaped glowing purple gem (p cage, P, u glow) in a cage with insect-like articulated legs (l).
ring("ring_of_power", "LEGEND", {
    "s": 0x6E4A3C, "S": 0xA87A66, "h": 0xD8A890, "p": 0x4A2A78, "P": 0x9A4AE8, "u": 0xE0B0FF, "l": 0xB8B8C8,
}, [
    "....l.....l.....",
    "....l.pppp.l....",
    ".....pPPPPp.....",
    "...llpPuuPpll...",
    "....lpPuuPpl....",
    ".....pPPPPp.l...",
    "....l.pppp.l....",
    "...l.l....l.l...",
])

# Ring of Regeneration: a thick wood-grain band carved with vines (d), studded with bright green leaves (L, with a light edge l) that also sprout on top.
REGEN_BAND = [
    "...ssssssssss...",
    "..sShSLSSSLSSs..",
    "..sSLdSSdSSLSs..",
    "..sSSSdLSSSSSs..",
    "...sSLSSSdLSs...",
    "....ssssssss....",
]
ring("ring_of_regeneration", "UNIQUE", {
    "s": 0x4A2E18, "S": 0x7A5230, "h": 0xA07848, "d": 0x5A3820, "L": 0x3FD83A, "l": 0xA0FF8A,
}, [
    "................",
    "................",
    "....L.....L.....",
    "...lLL...lLL....",
    "....L..L..L.....",
    ".......lL.......",
    "......LL........",
    "......L.........",
], REGEN_BAND)


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
