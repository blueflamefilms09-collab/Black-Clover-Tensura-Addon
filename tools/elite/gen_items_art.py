"""elite/items: the 16x16 inventory icons of every item of the subsystem (weapons keep the old-style diagonal pixel sword the 3D mesh is placed on; docs/elite/items.md).

    python3 -B tools/elite/gen_items_art.py [weapons|armor|consumables|keys|all] [--sheet]

Writes assets/nusmp/textures/item/<id>.png and (with --sheet) build/elite_preview/icons.png. Deterministic.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from art_lib import Canvas, ITEM_TEX, BUILD, diag_sword, ensure, mix, sheet, shade  # noqa: E402

GOLD = ((150, 104, 30), (226, 178, 62), (255, 224, 122))


def spark(x, y, c=(255, 255, 255)):
    return (x, y, c)


# ================================================================ weapons
def astral_edge():
    """Astral Edge: a slim star-metal longsword, pale gold-white, a crescent gold guard with a blue star gem, constellation sparks."""
    cv = diag_sword(
        blade=((255, 249, 222), (236, 218, 160), (170, 142, 92), (255, 255, 250)),
        hw=lambda d: 1 if d < 10 else 0,
        tip_d=13,
        guard=dict(d=-9, half=3, color=GOLD, thick=True),
        grip=((52, 52, 138), (36, 36, 100)),
        pommel=(130, 196, 255),
        extra=[spark(8, 7), spark(11, 4), spark(13, 2, (200, 230, 255)), spark(5, 10, (255, 240, 170)),
               (3, 12, (110, 190, 255)), (2, 9, (255, 224, 122)), (6, 13, (255, 224, 122)), (15, 0, (255, 255, 255)), (12, 1, (200, 230, 255)), (9, 4, (200, 230, 255))],
        outline=(36, 30, 70))
    return cv


def void_edge():
    """Void Edge: a black glass blade with violet cracks, a notched back edge and magenta void specks; a black guard with a violet gem."""
    cv = diag_sword(
        blade=((86, 52, 138), (36, 24, 62), (16, 10, 30), (150, 96, 230)),
        hw=lambda d: 2 if 1 <= d < 11 else (1 if d < 1 else 0) if d < 13 else 0,
        tip_d=13,
        guard=dict(d=-9, half=3, color=((46, 30, 64), (80, 52, 110), (30, 20, 44)), thick=True),
        grip=((30, 22, 44), (60, 40, 88)),
        pommel=(150, 70, 230),
        extra=[(3, 12, (210, 90, 255)), (7, 8, (190, 110, 255)), (9, 6, (255, 120, 235)), (11, 4, (170, 100, 255)), (12, 3, (255, 140, 240)), (8, 9, (110, 60, 200)),
               (10, 5, (110, 60, 200)), (14, 1, (220, 160, 255)), (15, 0, (255, 255, 255))],
        outline=(8, 4, 16))
    # the notches on the lower-right edge: transparent bites with an outline
    for x, y in ((8, 10), (10, 8), (12, 6)):
        cv.px[x, y] = (0, 0, 0, 0)
    for x, y in ((8, 10), (10, 8), (12, 6)):
        for dx, dy in ((1, 0), (0, 1)):
            if cv.filled(x + dx, y + dy):
                cv.put(x + dx, y + dy, (120, 60, 190))
    return cv


def fenrir_sword():
    """Fenrir Sword: a broad ice-silver blade, fang notches on the back, a wolf-head guard in dark steel with a pale blue eye, fur-brown grip."""
    cv = diag_sword(
        blade=((236, 246, 255), (176, 200, 226), (96, 120, 156), (255, 255, 255)),
        hw=lambda d: 2 if d < 9 else 1 if d < 13 else 0,
        tip_d=14,
        guard=dict(d=-9, half=3, color=((74, 80, 98), (112, 120, 142), (50, 54, 70)), thick=True),
        grip=((104, 72, 48), (74, 50, 34)),
        pommel=(210, 224, 240),
        extra=[(3, 12, (150, 230, 255)), (2, 11, (230, 240, 250)), (4, 13, (230, 240, 250)), (7, 9, (255, 255, 255)), (10, 6, (255, 255, 255)), (12, 4, (255, 255, 255)),
               (15, 0, (255, 255, 255)), (6, 12, (60, 40, 28)), (1, 14, (150, 110, 80))],
        outline=(22, 30, 52))
    for x, y in ((9, 8), (11, 6), (13, 4)):                     # fang bites out of the back edge
        cv.px[x, y] = (0, 0, 0, 0)
    for x, y in ((9, 8), (11, 6), (13, 4)):
        for dx, dy in ((1, 0), (0, 1)):
            if cv.filled(x + dx, y + dy):
                cv.put(x + dx, y + dy, (22, 30, 52))
    return cv


def runic(tier):
    """Runic Swords: the same bastard sword at three ages; the number of lit rune dots on the fuller is the number of slots."""
    if tier == 1:      # Renewed: mended steel with bronze fittings, one cyan rune
        blade = ((218, 226, 236), (158, 170, 188), (92, 102, 122), (255, 255, 255))
        gd = ((120, 78, 40), (186, 130, 70), (232, 184, 110))
        grip = ((84, 58, 40), (60, 40, 28))
        pom = (186, 130, 70)
        runes = [(0x7F, 0xE8, 0xFF)]
        outline = (30, 34, 48)
        hw = lambda d: 1 if d < 11 else 0       # noqa: E731
        tip_d = 13
    elif tier == 2:    # Ancient: dark gold-black blade, two amber runes
        blade = ((196, 168, 100), (122, 98, 56), (62, 48, 30), (255, 236, 160))
        gd = ((30, 26, 22), (66, 56, 40), (22, 18, 16))
        grip = ((40, 34, 28), (24, 20, 16))
        pom = (255, 200, 87)
        runes = [(0xFF, 0xC8, 0x57)] * 2
        outline = (16, 12, 8)
        hw = lambda d: 2 if d < 7 else 1 if d < 12 else 0       # noqa: E731
        tip_d = 13
    else:              # Mythical: white-violet crystal, three lilac runes, a wide crown guard
        blade = ((250, 244, 255), (210, 186, 244), (130, 104, 190), (255, 255, 255))
        gd = ((176, 140, 40), (255, 214, 90), (130, 98, 24))
        grip = ((90, 60, 150), (60, 40, 110))
        pom = (231, 184, 255)
        runes = [(0xE7, 0xB8, 0xFF)] * 3
        outline = (44, 24, 90)
        hw = lambda d: 2 if d < 11 else 1 if d < 13 else 0       # noqa: E731
        tip_d = 14
    cv = diag_sword(blade=blade, hw=hw, tip_d=tip_d, guard=dict(d=-9, half=3 if tier < 3 else 4, color=gd, thick=True), grip=grip, pommel=pom, outline=outline)
    spots = [(6, 9), (8, 7), (10, 5)][:len(runes)]
    for (x, y), c in zip(spots, runes):
        cv.put(x, y, c)
        cv.put(x, y - 1, shade(c, 0.6)) if tier == 3 else None
    cv.put(3, 12, runes[0])
    if tier == 3:
        cv.put(15, 0, (255, 255, 255))
        cv.put(12, 3, (255, 255, 255))
    return cv


WEAPONS = [("astral_edge", astral_edge), ("void_edge", void_edge), ("fenrir_sword", fenrir_sword),
           ("runic_sword_renewed", lambda: runic(1)), ("runic_sword_ancient", lambda: runic(2)), ("runic_sword_mythical", lambda: runic(3))]


def weapons():
    out = []
    for name, fn in WEAPONS:
        cv = fn()
        cv.save(os.path.join(ITEM_TEX, name + ".png"))
        out.append((name, cv.im))
    return out


# ================================================================ Nightingale armour (midnight blue, moon silver, russet)
NP = {"D": (14, 16, 42), "n": (46, 60, 130), "N": (30, 38, 92), "L": (96, 120, 200), "s": (216, 224, 242), "S": (150, 162, 196), "r": (172, 112, 70), "R": (216, 162, 110),
      "k": (10, 10, 26), "g": (190, 226, 255), "G": (120, 170, 240)}

HOOD = [
    "................",
    "......DDDD......",
    "....DDnnLLDD....",
    "...DnnLLLLnnD...",
    "..DnnLLnnLLnnD..",
    "..DnLssssssLnD..",
    "..DnsSkkkkSsnD..",
    ".DnnsSkkkkSsnnD.",
    ".DnNsSkkkkSsNnD.",
    ".DnNnsSkkSsnNnD.",
    ".DNNnnsSSsnnNND.",
    ".DNNnnnssnnnNND.",
    "..DNNnnrRrnnNDrD",
    "..DDNnnrRrnNDrRD",
    "...DDDNNNNDDDDRD",
    "............DDD.",
]
CLOAK = [
    "................",
    "..DDDD....DDDD..",
    ".DnnLDDDDDDLnnD.",
    ".DnLLsssssssLLnD",
    "DnnLnsSggGSsnLnnD"[:16],
    "DnLnnnsGGGsnnnLD",
    "DnLnnnnsSsnnnnLD",
    "DnnLnnnnrnnnnLnD",
    ".DnLnnNnrnNnnLD.",
    ".DnLnnNnnnNnnLD.",
    ".DnnLnNnnnNnLnD.",
    ".DNnLnNnnnNnLND.",
    ".DNnnLnNnNnLnND.",
    "..DNnnLnnnLnND..",
    "..DDrDDrDDrDDD..",
    "................",
]
LEGGINGS = [
    "................",
    "..DDDDDDDDDDDD..",
    "..DssLLssLLssD..",
    "..DSnnnnnnnnSD..",
    "..DnLnnnnnnLnD..",
    "..DnnnnDDnnnnD..",
    "..DnLnnDDnnLnD..",
    "..DnnnDDDDnnnD..",
    "..DnLnnDDnnLnD..",
    "..DnnnDDDDnnnD..",
    "..DNnLnDDnLnND..",
    "..DNnnnDDnnnND..",
    "..DNnLnDDnLnND..",
    "..DsSNNDDNNSsD..",
    "..DDDDDDDDDDDD..",
    "................",
]
BOOTS = [
    "................",
    "................",
    "..DDDD....DDDD..",
    ".DsSSsD..DsSSsD.",
    ".DnLnnD..DnLnnD.",
    ".DnnnND..DnnnND.",
    ".DnLnND..DnLnND.",
    ".DnnnND..DnnnND.",
    ".DNnnND..DNnnND.",
    ".DNnnnDDDDNnnnD.",
    "DNnnnnNDDNnnnnND",
    "DnLnnnnNDnLnnnnD",
    "DsSNNNNNDsSNNNND",
    "DDDDDDDDDDDDDDDD",
    "................",
    "................",
]


def armor():
    out = []
    for name, art in (("nightingale_hood", HOOD), ("nightingale_cloak", CLOAK), ("nightingale_leggings", LEGGINGS), ("nightingale_boots", BOOTS)):
        cv = Canvas().rows([r[:16].ljust(16, ".") for r in art], NP)
        cv.save(os.path.join(ITEM_TEX, name + ".png"))
        out.append((name, cv.im))
    return out


# ================================================================ potions, catalyst, book, orb, seal
POT = {"d": (74, 26, 36), "K": (150, 104, 66), "k": (110, 76, 46), "G": (204, 224, 240), "H": (255, 255, 255), "R": (214, 48, 72), "r": (255, 140, 150), "Y": (226, 178, 62), "y": (255, 224, 122),
       "o": (150, 104, 30), "s": (255, 244, 200)}
POTION_SMALL = [
    "................",
    "......dKKd......",
    "......dKkd......",
    ".......dd.......",
    "......dGGd......",
    "......dGHd......",
    ".....dGGGGd.....",
    "....dGRRRRGd....",
    "...dGRrrRRRGd...",
    "...dGRrRRRRGd...",
    "...dGRRRRRRGd...",
    "...dGRRRRRRGd...",
    "....dGRRRRGd....",
    ".....dGGGGd.....",
    "......dddd......",
    "................",
]
POTION_MEDIUM = [
    "......dddd......",
    ".....dKKKKd.....",
    ".....dKkKKd.....",
    "......dGGd......",
    "......dGGd......",
    "......dGHd......",
    ".....dGGGGd.....",
    "....dGGGGGGd....",
    "...dGRRRRRRGd...",
    "...dGRrrRRRGd...",
    "...dGRrRRRRGd...",
    "...dGRRRRRRGd...",
    "...dGRRRRRRGd...",
    "...dGRRRRRRGd...",
    "....dGGGGGGd....",
    ".....dddddd.....",
]
POTION_LARGE = [
    "..s..odddd.s....",
    "......dYYd......",
    ".....dYyyYd.....",
    ".....dYYYYd.....",
    "......dGGd......",
    ".....dGGHGd.....",
    "....dGGGGGGd....",
    "...dGGGGGGGGd...",
    "..dGRRRRRRRRGd..",
    "..dGRrrRRRRRGd..",
    "..dGRrRRRRRRGd..",
    "..dGRRRRRRRRGd..",
    "..dGRRRRRRRRGd..",
    "..dGRRRRRRRRGd..",
    "...dGGGGGGGGd...",
    "....dddddddd....",
]
CAT = {"d": (40, 22, 80), "W": (255, 255, 255), "V": (186, 130, 255), "v": (140, 90, 220), "P": (96, 56, 176), "f": (255, 236, 160), "Y": (226, 178, 62), "y": (255, 224, 122), "o": (150, 104, 30), "s": (255, 244, 200)}
CATALYST = [
    "......s.........",
    ".......WW....s..",
    "......dWWd......",
    ".....dVWWVd.....",
    "....dVVWWVVd....",
    "...dVVVVWVVVd...",
    "..dvVVVVVVVVvd..",
    "..dvvVVffVVvvd..",
    "..dPvvVffVvvPd..",
    "...dPPvVVvPPd...",
    "....dPPvvPPd....",
    ".....dPPPPd.....",
    "..yo..dPPd..oy..",
    "..oYYYYddYYYYo..",
    "...ooooooooooo..",
    "................",
]
BOOK = {"D": (10, 24, 34), "b": (30, 84, 112), "B": (46, 120, 150), "G": (226, 178, 62), "g": (255, 224, 122), "p": (240, 230, 196), "P": (196, 184, 148), "k": (20, 50, 70), "s": (190, 240, 255)}
CHRONICLE = [
    "................",
    "..DDDDDDDDDDD...",
    ".DkbbbbbbbbbbD..",
    ".DkBGGGGGGGGBDD.",
    ".DkBGbbbbbbGBDpD",
    ".DkBGbbgsbbGBDpD",
    ".DkBGbgggsbGBDpD",
    ".DkBGbsggsbGBDpD",
    ".DkBGbbgsbbGBDpD",
    ".DkBGbbbbbbGBDpD",
    ".DkBGGGGGGGGBDPD",
    ".DkbbbbGGbbbbDPD",
    ".DDDDDDDDDDDDDPD",
    "...DPPPPPPPPPPD.",
    "....DDDDDDDDDD..",
    "................",
]


def sphere(cv, cx, cy, r, base, light, dark, rim):
    for y in range(cv.h):
        for x in range(cv.w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            dist = (dx * dx + dy * dy) ** 0.5
            if dist > r:
                continue
            lx, ly = -0.55, -0.6
            lit = max(-1.0, min(1.0, (dx * lx + dy * ly) / (r * 0.8)))
            c = mix(base, light, lit) if lit > 0 else mix(base, dark, -lit)
            if dist > r - 1.0:
                c = mix(c, rim, 0.55)
            cv.put(x, y, c)


def orb():
    cv = Canvas()
    sphere(cv, 8, 8.2, 6.4, (92, 42, 170), (214, 170, 255), (24, 10, 60), (255, 220, 120))
    for (x, y, c) in [(5, 5, (255, 255, 255)), (6, 4, (240, 220, 255)), (4, 6, (240, 220, 255)), (9, 7, (255, 200, 255)), (10, 8, (200, 120, 255)), (8, 9, (255, 150, 255)), (7, 10, (160, 90, 230)), (11, 6, (140, 80, 220)),
                      (6, 8, (150, 80, 230)), (9, 11, (120, 60, 200))]:
        cv.put(x, y, c)
    cv.outline((20, 8, 44))
    for (x, y) in ((1, 3), (14, 12), (13, 2)):
        cv.put(x, y, (255, 224, 122))
    return cv


def seal():
    cv = Canvas()
    gold, dgold, lgold = (200, 150, 44), (122, 82, 22), (255, 224, 122)
    sphere_like = [(x, y) for y in range(16) for x in range(16) if ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5 <= 6.6]
    for (x, y) in sphere_like:
        d = ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5
        cv.put(x, y, gold if d > 5.2 else (60, 22, 72))
        if d > 5.2:
            cv.put(x, y, lgold if (x + y) % 5 == 0 else gold if x < 8 else dgold)
    sig = [(8, 3), (8, 4), (8, 5), (8, 6), (8, 9), (8, 10), (8, 11), (8, 12), (3, 8), (4, 8), (5, 8), (6, 8), (9, 8), (10, 8), (11, 8), (12, 8), (5, 5), (6, 6), (10, 5), (9, 6), (5, 11), (6, 10),
           (10, 11), (9, 10)]
    for (x, y) in sig:
        cv.put(x, y, (220, 120, 255))
    for (x, y) in ((8, 7), (7, 8), (8, 8), (9, 8), (8, 9)):
        cv.put(x, y, (255, 240, 255))
    cv.outline((30, 16, 8))
    return cv


def key(body, dark, light, gem, glow=None):
    """A key on the diagonal: the bow (a ring with the gem in it) at the top right, a shaft with two teeth towards the bottom left."""
    cv = Canvas()
    cx, cy = 11.0, 4.0
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if 1.5 <= d <= 3.4:
                cv.put(x, y, light if (x + 0.5 - cx) + (y + 0.5 - cy) < 0 else body if d < 2.9 else dark)
    for k in range(0, 8):
        x, y = 9 - k, 6 + k
        cv.put(x, y, light)
        cv.put(x + 1, y, body)
        cv.put(x, y + 1, dark)
    for (x, y) in ((3, 10), (2, 10), (5, 12), (4, 12), (2, 12), (1, 12)):
        cv.put(x, y, body)
    for (x, y) in ((3, 11), (5, 13), (1, 13), (2, 11)):
        cv.put(x, y, dark)
    if gem is not None:
        for (x, y) in ((11, 4), (10, 4), (11, 5), (10, 3), (11, 3)):
            cv.put(x, y, gem)
        cv.put(10, 3, glow or light)
    cv.outline(shade(dark, 0.35))
    return cv


def consumables():
    out = []

    def add(name, cv):
        cv.save(os.path.join(ITEM_TEX, name + ".png"))
        out.append((name, cv.im))
    add("health_potion_small", Canvas().rows(POTION_SMALL, POT))
    add("health_potion_medium", Canvas().rows(POTION_MEDIUM, POT))
    add("health_potion_large", Canvas().rows(POTION_LARGE, POT))
    add("potential_catalyst", Canvas().rows(CATALYST, CAT))
    add("chronicle_book", Canvas().rows(CHRONICLE, BOOK))
    add("walpurgis_orb", orb())
    add("walpurgis_seal", seal())
    return out


def keys():
    out = []
    for name, cv in (("common_crate_key", key((176, 180, 190), (84, 88, 100), (232, 236, 244), None)),
                     ("rare_crate_key", key((200, 214, 232), (70, 90, 150), (250, 252, 255), (70, 150, 240), (170, 220, 255))),
                     ("elite_crate_key", key((236, 190, 70), (130, 84, 20), (255, 240, 160), (176, 80, 240), (240, 190, 255)))):
        cv.save(os.path.join(ITEM_TEX, name + ".png"))
        out.append((name, cv.im))
    return out


# ================================================================ armour layers (the painted vanilla layers: mobs, armour stands, relicfx.models off) and the model jsons
def _noise(x, y, salt):
    from plush_lib import hash2
    return (hash2(x, y, salt) - 0.5) * 12


def _fill(cv, x0, y0, w, h, base, salt=1, k=1.0):
    for j in range(h):
        for i in range(w):
            n = _noise(x0 + i, y0 + j, salt)
            c = tuple(max(0, min(255, int(base[t] * k + n))) for t in range(3))
            cv.put(x0 + i, y0 + j, c)


def armor_layers():
    """textures/models/armor/nightingale_layer_1.png (hood, cloak with sleeves, boots), _layer_2.png (leggings and the belt), blank_layer.png (fully transparent, 64x32)."""
    out = os.path.join(ITEM_TEX, "..", "models", "armor")
    ensure(out)
    navy, dark, light, silver, russet = NP["n"], NP["N"], NP["L"], NP["s"], NP["r"]
    cv = Canvas(64, 32)
    # head: top (8,0), bottom (16,0), right (0,8), front (8,8), left (16,8), back (24,8); every face 8x8
    for (x, y) in ((8, 0), (16, 0), (0, 8), (8, 8), (16, 8), (24, 8)):
        _fill(cv, x, y, 8, 8, navy, 2, 1.1 if (y == 0 and x == 8) else 1.0)
    for i in range(8):                                                  # a silver hem along the front of the hood and a face opening
        cv.put(8 + i, 8, silver)
    for j in range(2, 7):
        for i in range(1, 7):
            cv.px[8 + i, 8 + j] = (0, 0, 0, 0)
    for j in range(3):                                                  # a russet feather on the right side of the hood
        cv.put(2 + j, 10 + j, NP["R"])
        cv.put(3 + j, 10 + j, russet)
    # body: top (20,16,8,4) bottom (28,16,8,4) right (16,20,4,12) front (20,20,8,12) left (28,20,4,12) back (32,20,8,12)
    _fill(cv, 20, 16, 8, 4, navy, 3, 1.1)
    _fill(cv, 28, 16, 8, 4, navy, 4, 0.7)
    _fill(cv, 16, 20, 4, 12, navy, 5)
    _fill(cv, 20, 20, 8, 12, navy, 6)
    _fill(cv, 28, 20, 4, 12, navy, 7)
    _fill(cv, 32, 20, 8, 12, navy, 8)
    for i in range(8):
        cv.put(20 + i, 20, silver)
        cv.put(20 + i, 31, dark)
    for j in range(4):
        cv.put(23, 20 + j, silver)
        cv.put(24, 20 + j, silver)
    cv.put(23, 23, NP["g"])
    cv.put(24, 23, NP["g"])
    for j in range(2, 12):                                              # the cloak's folds
        for x in (21, 26, 34, 37):
            cv.put(x, 20 + j, dark)
    # arms: right arm (40,16): top (44,16,4,4) bottom (48,16,4,4) right (40,20,4,12) front (44,20,4,12) left (48,20,4,12) back (52,20,4,12)
    _fill(cv, 44, 16, 4, 4, navy, 9, 1.1)
    _fill(cv, 48, 16, 4, 4, navy, 10, 0.7)
    for x in (40, 44, 48, 52):
        _fill(cv, x, 20, 4, 12, navy, 11 + x)
        for i in range(4):
            cv.put(x + i, 29, silver)
            cv.put(x + i, 30, light)
    # boots: the bottom of each leg (right leg at (0,16): right (0,20) front (4,20) left (8,20) back (12,20), each 4x12): rows 8..11 are the boot
    for x in (0, 4, 8, 12):
        _fill(cv, x, 28, 4, 4, navy, 21 + x, 0.9)
        for i in range(4):
            cv.put(x + i, 28, silver)
    _fill(cv, 4, 16, 4, 4, dark, 30)
    _fill(cv, 8, 16, 4, 4, dark, 31)
    cv.save(os.path.join(out, "nightingale_layer_1.png"))
    cv = Canvas(64, 32)                                                 # layer 2: the leggings (legs rows 0..8) and the waist
    for x in (0, 4, 8, 12):
        _fill(cv, x, 20, 4, 8, navy, 40 + x)
        for i in range(4):
            cv.put(x + i, 20, silver)
            cv.put(x + i, 27, light)
    _fill(cv, 4, 16, 4, 4, navy, 50, 1.1)
    _fill(cv, 8, 16, 4, 4, navy, 51, 0.7)
    for x, w in ((16, 4), (20, 8), (28, 4), (32, 8)):                 # the belt: the lowest three rows of the body
        _fill(cv, x, 29, w, 3, navy, 60 + x)
        for i in range(w):
            cv.put(x + i, 29, silver)
    cv.put(23, 30, NP["g"])
    cv.put(24, 30, NP["g"])
    cv.save(os.path.join(out, "nightingale_layer_2.png"))
    Canvas(64, 32).save(os.path.join(out, "blank_layer.png"))


DISPLAY_ARMOR = {
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def jdump(o):
    import json
    return json.dumps(o, indent=2) + "\n"


def models():
    """The item model jsons of everything that is not a weapon or a block: armour (builtin/entity + the _icon sprite model, the way the worn-model library wants it), flat items
    (item/generated), the Chronicle Book (a separate-transforms model: the 3D book in the hand, the flat icon in the inventory)."""
    from art_lib import ASSETS, write_text
    m = os.path.join(ASSETS, "models", "item")
    for i in ("nightingale_hood", "nightingale_cloak", "nightingale_leggings", "nightingale_boots"):
        write_text(os.path.join(m, i + ".json"), jdump({"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + i}, "display": DISPLAY_ARMOR}))
        write_text(os.path.join(m, i + "_icon.json"), jdump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + i}}))
    for i in ("potential_catalyst", "health_potion_small", "health_potion_medium", "health_potion_large", "walpurgis_orb", "walpurgis_seal", "common_crate_key", "rare_crate_key", "elite_crate_key"):
        write_text(os.path.join(m, i + ".json"), jdump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + i}}))
    write_text(os.path.join(m, "chronicle_book_2d.json"), jdump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/chronicle_book"}}))
    write_text(os.path.join(m, "chronicle_book.json"), jdump({"loader": "neoforge:separate_transforms", "gui_light": "front", "textures": {"particle": "nusmp:item/chronicle_book_3d"},
                                                              "base": {"parent": "nusmp:item/chronicle_book_3d"}, "perspectives": {"gui": {"parent": "nusmp:item/chronicle_book_2d"}}}))


def chronicle_book():
    """The 3D Chronicle Book (models/item/chronicle_book_3d.json + textures/item/chronicle_book_3d.png): a teal cover with a gold frame and an hourglass of light, a gold clasp and
    corners, cream pages, a ribbon. 9 wide, 13 high, 4 thick; the front cover looks south (+z), the spine is on the west (-x), like the Skill Book's model."""
    import plush_lib
    from plush_lib import Plush, hexc
    from art_lib import ASSETS, ensure, write_text
    teal, tealdark, gold, golddark, page = hexc("1E5470"), hexc("143A4E"), hexc("E2B23E"), hexc("96691C"), hexc("EFE4C4")
    P = Plush("chronicle_book_3d", teal)
    back = P.cube("back_board", (9, 13, 1), (3.5, 1.5, 6.0), tealdark, noise=5)
    front = P.cube("front_board", (9, 13, 1), (3.5, 1.5, 9.0), teal, noise=5)
    P.cube("spine", (1, 13, 4), (2.5, 1.5, 6.0), tealdark, noise=5)
    pages = P.cube("pages", (8, 12, 2), (4.0, 2.0, 7.0), page, noise=4)
    for f in ("east", "north", "south", "up", "down"):
        rx, ry, rw, rh = pages.rects[f]
        for j in range(rh):
            for i in range(rw):
                if (j if f in ("up", "down") else i) % 2 == 0:
                    P.put(pages, f, i, j, plush_lib.shade(page, 0.86))
    P.cube("ribbon", (1, 2, 1), (8.5, -0.5, 7.5), hexc("B02A3A"), noise=2)
    P.cube("clasp", (2, 3, 1), (11.5, 6.0, 10.0), gold, noise=4)
    P.cube("gem", (1, 1, 1), (12.0, 7.0, 11.0), hexc("8AD8FF"), noise=0)
    for k, (x, y) in enumerate(((3.5, 1.5), (11.5, 1.5), (3.5, 12.5), (11.5, 12.5))):
        P.cube("cap%d" % k, (1, 1, 2), (x, y, 9.0), gold, noise=3)
    w, h = 9, 13                                                        # the front cover (south face): a gold frame and an hourglass of pale light
    for i in range(w):
        P.put(front, "south", i, 0, gold)
        P.put(front, "south", i, h - 1, gold)
    for j in range(h):
        P.put(front, "south", 0, j, gold)
        P.put(front, "south", w - 1, j, gold)
    for i in range(2, 7):
        P.put(front, "south", i, 3, gold)
        P.put(front, "south", i, 9, gold)
    glass = hexc("BEEBFF")
    for y, (x0, x1) in {4: (3, 5), 5: (4, 4), 6: (4, 4), 7: (4, 4), 8: (3, 5)}.items():
        for x in range(x0, x1 + 1):
            P.put(front, "south", x, y, glass)
    P.put(front, "south", 4, 6, hexc("FFF4B0"))
    for i in range(9):
        P.put(back, "north", i, 0, golddark)
        P.put(back, "north", i, 12, golddark)
    m = P.model_json(display=False)
    m["textures"] = {"0": "nusmp:item/chronicle_book_3d", "particle": "nusmp:item/chronicle_book_3d"}
    m.pop("render_type", None)
    m["display"] = {"thirdperson_righthand": {"rotation": [60, 150, 0], "translation": [0.0, 1.0, 3.0], "scale": [0.5, 0.5, 0.5]},
                    "thirdperson_lefthand": {"rotation": [60, 150, 0], "translation": [0.0, 1.0, 3.0], "scale": [0.5, 0.5, 0.5]},
                    "firstperson_righthand": {"rotation": [-15, -15, 15], "translation": [-2.9, 4.3, -1.3], "scale": [0.42, 0.42, 0.42]},
                    "firstperson_lefthand": {"rotation": [-15, -15, 15], "translation": [-2.9, 4.3, -1.3], "scale": [0.42, 0.42, 0.42]},
                    "ground": {"rotation": [-90, 0, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
                    "gui": {"rotation": [20, -155, 0], "translation": [0, 0, 0], "scale": [0.62, 0.62, 0.62]},
                    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 0], "scale": [1, 1, 1]},
                    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]}}
    write_text(os.path.join(ASSETS, "models", "item", "chronicle_book_3d.json"), jdump(m))
    tp = os.path.join(ITEM_TEX, "chronicle_book_3d.png")
    ensure(os.path.dirname(tp))
    P.tex.im.save(tp, optimize=True)
    return P


def main(argv):
    what = [a for a in argv if not a.startswith("--")] or ["all"]
    made = []
    if "weapons" in what or "all" in what:
        made += weapons()
    if "armor" in what or "all" in what:
        made += armor()
    if "consumables" in what or "all" in what:
        made += consumables()
    if "keys" in what or "all" in what:
        made += keys()
    if "models" in what or "all" in what:
        armor_layers()
        models()
        chronicle_book()
    if "--sheet" in argv and made:
        ensure(BUILD)
        sheet([im for _, im in made], scale=10, cols=8).save(os.path.join(BUILD, "icons.png"))
        print("sheet:", os.path.join(BUILD, "icons.png"))
    print("icons written:", ", ".join(n for n, _ in made))


if __name__ == "__main__":
    main(sys.argv[1:])
