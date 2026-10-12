#!/usr/bin/env python3
"""
1.21.0: the 16x16 inventory sprites of the Dimensional Nexus relics and navigation gear (docs/living_dungeon.md), drawn to the dungeon's look:
a dark outline, three or four shades per material, small bright glints and glows.

    python3 tools/gen_nexus_relic_icons.py

Writes assets/nusmp/textures/item/
  ki_resonance_lantern.png (amber flame, the default sprite), ki_resonance_lantern_off.png, _blue.png, _red.png     the four flame states
  anti_bird_compass.png (the needle pointing up) and anti_bird_compass_00.png .. _31.png                            the 32 needle frames
  glutton_chalice.png, mantle_of_the_scavenger.png, band_of_the_spatial_anchor.png, devil_bound_sheath.png, gamblers_grimoire_page.png
and under assets/nusmp/models/item/ the item models: <id>.json (the 3D marker client/WeaponRenderer draws in hand: the lantern, the compass, the chalice, the sheath
and the page; the flat model for the mantle and the band), <id>_icon.json (the inventory icon, with the overrides of the lantern's flame, "nusmp:flame", and
the compass's needle, "nusmp:angle") and the flat sub-models those overrides point to.
tools/gen_nexus_relic_models.py (run through tools/gen_weapon_models.py) builds the in-hand 3D models and reads these sprites for placement, so run
this script first. Idempotent: running it again rewrites the same files.
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

OUTLINE = (22, 18, 30)


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def from_rows(rows, legend):
    """16 strings of 16 characters -> {(x, y): (r, g, b)}; '.' is transparent."""
    assert len(rows) == 16, len(rows)
    px = {}
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                px[(x, y)] = legend[ch]
    return px


def add_outline(px, rgb=OUTLINE, diagonal=False):
    cells = set(px)
    steps = [(-1, 0), (1, 0), (0, -1), (0, 1)] + ([(-1, -1), (1, -1), (-1, 1), (1, 1)] if diagonal else [])
    for (x, y) in cells:
        for dx, dy in steps:
            q = (x + dx, y + dy)
            if q not in cells and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                px[q] = rgb
    return px


def save(px, name):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        im.putpixel((x, y), (int(c[0]), int(c[1]), int(c[2]), 255))
    os.makedirs(ICONS, exist_ok=True)
    im.save(os.path.join(ICONS, name + ".png"))


def write_model(name, data):
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".json"), "w", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


# ================================================================ the Ki-Resonance Lantern
# An ornate iron cage round a flame, a brass collar and a ring to hold it by. Four states of the same sprite: the flame is out, amber (fuelled), blue (an
# IllusionStone wall ahead) or red (a descent gate ahead); the glass and the base runes take the flame's colour.
LANTERN_ROWS = [
    ".....kkkkkk.....",
    "....kOooooOk....",
    "....kok..kOk....",
    "....kOk..kok....",
    "....kkoOOokk....",
    "...kkLllLlIkk...",
    "..koOooooooOok..",
    "..kLSSSSSSSSIk..",
    "..kLSSSSSSSSIk..",
    "..kISSSSSSSSIk..",
    "..kISSSSSSSSIk..",
    "..kISSSSSSSSIk..",
    "..kkkkkwwkkkkk..",
    ".kIlLllrrllllIk.",
    ".kIiiiiiiiiiiIk.",
    "..kk.kk..kk.kk..",
]
# the flame, drawn over the glass on rows 7..11 (its base sits on the wick on the row below)
FLAME_ROWS = [
    "........e.......",
    ".......eFe......",
    "......efFe......",
    "......efFFe.....",
    ".....efFFFfe....",
]
LANTERN_STATES = {
    # name: (flame outer, mid, core, glass tint, rune glint, file)
    "amber": ((222, 112, 30), (255, 178, 52), (255, 244, 196), (84, 62, 30), (255, 190, 70), "ki_resonance_lantern"),
    "off": (None, None, None, (30, 38, 50), (74, 78, 98), "ki_resonance_lantern_off"),
    "blue": ((38, 98, 224), (84, 176, 255), (226, 248, 255), (24, 52, 98), (110, 190, 255), "ki_resonance_lantern_blue"),
    "red": ((168, 22, 30), (255, 78, 58), (255, 222, 200), (90, 24, 28), (255, 90, 70), "ki_resonance_lantern_red"),
}


def lantern(state):
    outer, mid, core, tint, rune, _ = LANTERN_STATES[state]
    legend = {
        "k": OUTLINE, "L": (206, 212, 232), "l": (136, 142, 170), "I": (82, 86, 112), "i": (46, 48, 66),
        "o": (190, 136, 56), "O": (248, 204, 108), "w": (58, 52, 56), "r": rune, "S": tint,
    }
    px = from_rows(LANTERN_ROWS, legend)
    lit = outer is not None
    for y in range(7, 12):                                    # the glass: a brighter strip down the left, a faint gradient to the right
        for x in range(4, 12):
            base = tuple(tint)
            if x == 4:
                base = tuple(min(255, int(c * 1.55 + 22)) for c in base)
            elif x >= 10:
                base = tuple(int(c * 0.78) for c in base)
            if lit:                                           # the flame's light fills the glass round it
                d = abs(x - 7.5) + abs(y - 9.0) * 0.6
                k = max(0.0, 1.0 - d / 4.4)
                base = tuple(min(255, int(c + (m - c) * 0.55 * k)) for c, m in zip(base, mid))
            px[(x, y)] = base
    if lit:
        for y, row in enumerate(FLAME_ROWS):
            for x, ch in enumerate(row):
                if ch != ".":
                    px[(x, 7 + y)] = {"e": outer, "f": mid, "F": core}[ch]
        px[(7, 12)] = core                                    # the wick, lit
        px[(8, 12)] = core
    else:
        px[(8, 11)] = (92, 86, 90)                            # a dead, charred wick
        px[(8, 10)] = (60, 56, 62)
    return px


# ================================================================ the Anti-Bird Compass
# A bronze case round a dark green dial, the needle a little feather (a pale tail, a biomass-green tip), a black crow's feather hanging off the ring.
COMPASS_CENTRE = (6.6, 9.4)


def compass_case():
    cx, cy = COMPASS_CENTRE
    px = {}
    bronze_d, bronze, bronze_l, bronze_h = (104, 60, 28), (178, 110, 48), (226, 162, 72), (255, 220, 132)
    r_out, r_in = 5.95, 4.15
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r_out:
                if d > r_in:                                         # the ring, lit from the upper left
                    lit = (-dx - dy) / (d + 1e-6)
                    c = bronze_h if lit > 0.84 else bronze_l if lit > 0.3 else bronze if lit > -0.45 else bronze_d
                else:                                                # the dial
                    t = d / r_in
                    c = (22, 52, 48) if t < 0.62 else (32, 72, 62)
                    if dx + dy < -1.4 and t > 0.55:
                        c = (46, 96, 82)
                px[(x, y)] = c
    for (x, y) in ((6, 4), (6, 14), (1, 9), (11, 9)):                  # the four ticks on the ring
        px[(x, y)] = (255, 236, 168)
    # the ring of the case, above it, with a dark hole
    for (x, y), c in {(5, 2): bronze_l, (6, 1): bronze_h, (7, 1): bronze_l, (8, 2): bronze, (5, 3): bronze, (8, 3): bronze_d, (6, 3): bronze_d, (7, 3): bronze_d}.items():
        px[(x, y)] = c
    # the crow's feather hanging off the ring to the upper right: a pale shaft, a black vane with a green sheen
    bx, by, tx, ty = 9.4, 3.6, 15.2, 0.6
    L = math.hypot(tx - bx, ty - by)
    ux, uy = (tx - bx) / L, (ty - by) / L
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - bx, y + 0.5 - by
            t = (dx * ux + dy * uy) / L
            a_ = -dx * uy + dy * ux
            if 0.0 <= t <= 1.0 and abs(a_) <= 1.75 * math.sin(math.pi * min(1.0, t * 0.92 + 0.08)) ** 0.8:
                px[(x, y)] = (226, 214, 180) if abs(a_) < 0.42 else (82, 160, 136) if a_ > 0 and abs(a_) < 1.1 else (36, 34, 50)
    return add_outline(px, (34, 22, 18))


def compass_frame(k):
    """The case with the needle turned k/32 of a turn clockwise from straight up."""
    px = compass_case()
    cx, cy = COMPASS_CENTRE
    th = 2 * math.pi * k / 32
    ux, uy = math.sin(th), -math.cos(th)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            along = dx * ux + dy * uy
            across = abs(-dx * uy + dy * ux)
            if -2.0 <= along <= 3.7 and across < 0.62 + (0.35 if -0.6 < along < 1.2 else 0) and math.hypot(dx, dy) <= 4.2:
                px[(x, y)] = (178, 255, 120) if along > 1.4 else (232, 232, 240) if along < -0.4 else (255, 255, 200)
    px[(int(cx), int(cy))] = (255, 232, 160)
    return px


# ================================================================ the Glutton's Chalice
# A living goblet of bone: a bowl rimmed with tiny teeth that holds a glowing green stew, red veins crawling over the bone, a knotted stem and a foot
# that sprouts little tendrils.
def chalice():
    px = {}
    cx = 7.5
    bone = {"W": (248, 242, 224), "w": (224, 208, 178), "d": (174, 152, 124), "D": (124, 102, 84)}
    half = {1: 5.2, 2: 6.8, 3: 7.2, 4: 7.0, 5: 6.4, 6: 5.6, 7: 4.6, 8: 3.4, 9: 2.2, 10: 1.6, 11: 1.6, 12: 2.4, 13: 4.2, 14: 5.8}
    for y, hw in half.items():
        for x in range(16):
            dx = x + 0.5 - cx
            if abs(dx) <= hw:
                u = dx / hw
                px[(x, y)] = bone["W"] if u < -0.55 else bone["w"] if u < 0.25 else bone["d"] if u < 0.72 else bone["D"]
    px[(5, 10)] = None
    px[(10, 10)] = None
    # the stew in the bowl: an ellipse, bright at the middle
    for y in range(1, 6):
        for x in range(16):
            dx, dy = (x + 0.5 - cx) / 5.6, (y + 0.5 - 3.2) / 2.0
            d = dx * dx + dy * dy
            if d <= 1.0:
                px[(x, y)] = (214, 255, 168) if d < 0.16 else (110, 226, 110) if d < 0.46 else (60, 176, 92) if d < 0.78 else (28, 116, 70)
    # the teeth along the front lip: white points standing up out of it, a dark gap between
    for x in (3, 5, 7, 9, 11):
        px[(x, 4)] = (255, 252, 240)
        px[(x, 5)] = (240, 232, 210)
    for x in (4, 6, 8, 10, 12):
        px[(x, 5)] = (84, 62, 56)
    for x in (5, 9):                                                       # and two on the far rim
        px[(x, 1)] = (255, 252, 240)
    # veins
    for (x, y) in ((2, 6), (3, 7), (3, 8), (4, 9)):
        px[(x, y)] = (176, 44, 70)
    for (x, y) in ((12, 6), (12, 7), (11, 8), (10, 9)):
        px[(x, y)] = (176, 44, 70)
    for (x, y) in ((8, 7), (9, 8)):
        px[(x, y)] = (228, 86, 110)
    for (x, y) in ((7, 11), (6, 12), (3, 14), (12, 14), (2, 13), (13, 13), (9, 13)):
        px[(x, y)] = (176, 44, 70)
    px = {k: v for k, v in px.items() if v is not None}
    return add_outline(px, (44, 30, 34))


# ================================================================ the Mantle of the Scavenger
# A pelt cloak: a thick fur collar with a bone clasp, a mottled hide body, and a fringe of bone shards and hide tassels along the hem.
MANTLE_ROWS = [
    "................",
    "....FFFFFFFF....",
    "...FFfFFFFfFF...",
    "..FfFFkWWkFFfF..",
    "..FFfFkWDkFFFf..",
    "...HHhHkkHHhH...",
    "...HhHhHkHHhH...",
    "..HHhHHhHHhHHH..",
    "..HhHHhHHhHHhH..",
    ".HHhHHhHHHhHHhH.",
    ".HhHHhHhHHhHHhH.",
    "BBBBBBBBBBBBBBBB",
    "HWDHHWDHHWDHHWDH",
    ".WD.hWD.hWD.hWD.",
    "..D..WD..WD..WD.",
    "................",
]
MANTLE_LEGEND = {
    "F": (226, 196, 142), "f": (186, 150, 100), "H": (122, 86, 56), "h": (88, 58, 38),
    "W": (246, 238, 214), "D": (178, 156, 128), "k": (46, 32, 28), "B": (66, 44, 34),
}


def mantle():
    px = from_rows(MANTLE_ROWS, MANTLE_LEGEND)
    for (x, y), c in list(px.items()):                                 # fur flecks: a few lighter hairs on the collar and the hide
        h = (x * 7349 + y * 1013) % 11
        if c == (122, 86, 56) and h == 0:
            px[(x, y)] = (150, 108, 70)
        if c == (226, 196, 142) and h in (1, 2):
            px[(x, y)] = (248, 224, 170)
    return add_outline(px, (30, 22, 20))


# ================================================================ the Band of the Spatial Anchor
# A silver band seen at a slant, a gold line inlaid along its outside, and a small spiral of blue-white light turning in the air above its hole (the anchor),
# a few motes round it.
def band():
    px = {}
    cx, cy = 7.5, 9.6
    rx_o, ry_o, rx_i, ry_i = 6.2, 5.2, 4.1, 3.3
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            do = (dx / rx_o) ** 2 + (dy / ry_o) ** 2
            di = (dx / rx_i) ** 2 + (dy / ry_i) ** 2
            if do <= 1.0 and di >= 1.0:                                   # the band
                lit = (-dx * 0.7 - dy) / 6.0
                c = (238, 244, 255) if lit > 0.5 else (178, 190, 214) if lit > 0.05 else (118, 130, 162) if lit > -0.4 else (72, 80, 112)
                if dy > 0.6 and 0.62 < do < 0.84:
                    c = (232, 186, 84) if dx < 1 else (170, 126, 50)       # the gold line on the front of the band
                px[(x, y)] = c
            elif di < 1.0:                                                 # the anchor: a small spiral of blue-white light turning in the ring
                u, v = dx / rx_i, dy / ry_i
                r = math.hypot(u, v)
                ang = math.atan2(v, u) - r * 3.4
                arm = 0.5 + 0.5 * math.cos(2 * ang)
                if r < 0.26:
                    c = (255, 255, 255)
                elif arm > 0.55:
                    c = (222, 246, 255) if r < 0.62 else (132, 200, 255)
                else:
                    c = (52, 92, 214) if r < 0.8 else (30, 52, 150)
                px[(x, y)] = c
    # a gold stud with a pale gem where the stone would be, on the far side of the band
    for (x, y, c) in ((7, 3, (170, 126, 50)), (8, 3, (232, 186, 84)), (7, 4, (232, 186, 84)), (8, 4, (255, 232, 150)), (6, 4, (170, 126, 50)), (9, 4, (170, 126, 50)),
                      (7, 2, (214, 246, 255)), (8, 2, (255, 255, 255))):
        px[(x, y)] = c
    out = add_outline(px, (20, 22, 44))
    for (x, y, c) in ((1, 2, (190, 226, 255)), (13, 1, (255, 255, 255)), (15, 9, (150, 200, 255)), (0, 6, (110, 170, 255)), (14, 14, (190, 226, 255)), (1, 14, (150, 200, 255))):
        out.setdefault((x, y), c)
    return out


# ================================================================ the Devil-Bound Sheath
# A black scabbard on the usual bottom-left to top-right diagonal: a dark lacquer body with a lit edge, wound with iron chain, a violet-red sigil in the
# middle (it glows when the sheath is charged), an iron mouth with a red stud and a chape at the tip.
def sheath():
    R2 = math.sqrt(2.0)
    px = {}

    def frame(x, y):
        dx, dy = x - 0.5, y - 15.5
        return (dx - dy) / R2, (-dx - dy) / R2                    # t along the sheath (up and to the right), s across it (towards the upper left)
    lacquer, lacquer_l, lacquer_d = (44, 36, 62), (94, 80, 122), (26, 20, 38)
    chain, chain_l, chain_d = (156, 162, 184), (232, 238, 252), (84, 88, 112)
    for y in range(16):
        for x in range(16):
            t, s = frame(x + 0.5, y + 0.5)
            half = 1.95 - 0.5 * max(0.0, (t - 17.0) / 3.5)
            if 0.5 <= t <= 20.8 and abs(s) <= half:
                c = lacquer_l if s > 0.7 else lacquer_d if s < -0.9 else lacquer
                if t < 2.6:                                                   # the mouth: an iron collar
                    c = (150, 156, 180) if s > 0.2 else (84, 88, 112)
                elif t > 19.2:                                                # the chape
                    c = (172, 178, 200) if s > 0.2 else (92, 96, 122)
                px[(x, y)] = c
    for (x, y) in list(px):
        t, s = frame(x + 0.5, y + 0.5)
        for tk in (4.6, 7.4, 14.6, 17.3):                                      # the chain, turn after turn
            if abs(t - tk) < 0.56:
                lit = (x + y) % 2 == 0
                px[(x, y)] = chain_l if (lit and s > 0.1) else chain if lit else chain_d
        if 9.4 <= t <= 12.6 and abs(s) < 1.75:                                 # the sigil: a violet-red diamond with a white-hot heart
            d = abs(t - 11.0) * 0.9 + abs(s) * 1.0
            px[(x, y)] = (255, 218, 244) if d < 0.55 else (240, 76, 160) if d < 1.3 else (160, 38, 130) if d < 2.0 else (92, 24, 96)
    px[(1, 14)] = (236, 62, 76)                                                 # the stud on the mouth
    return add_outline(px)


# ================================================================ the Gambler's Grimoire Page
# One torn page with a curled corner, a d20 inked on it (the hexagon, the glowing face, the three seams), runes round the edge, each a different
# colour (they shift), and glints around it.
def page():
    px = {}
    paper, paper_s, paper_d = (240, 226, 192), (214, 198, 160), (166, 146, 110)
    for y in range(1, 15):
        for x in range(2, 14):
            top_torn = y == 1 and x in (2, 3, 6, 7, 11, 12, 13)
            if top_torn:
                continue
            if x >= 9 + (13 - y) * 0 and y >= 12 and (x - 9) + (y - 12) >= 4:       # the curled corner is cut away
                continue
            px[(x, y)] = paper_s if (x + y) % 5 == 0 else paper
    for (x, y) in ((10, 12), (11, 12), (12, 13), (9, 13), (10, 13), (11, 13)):    # the curl: a darker underside, a lit edge
        if (x, y) in px:
            px[(x, y)] = paper_d if (x + y) % 2 else paper_s
    # the d20 face-on
    cx, cy, R = 7.5, 7.6, 4.2
    verts = [(cx + R * math.cos(math.radians(90 + 60 * k)), cy - R * math.sin(math.radians(90 + 60 * k))) for k in range(6)]

    def seg(p, q, x, y):
        vx, vy = q[0] - p[0], q[1] - p[1]
        t = max(0.0, min(1.0, ((x - p[0]) * vx + (y - p[1]) * vy) / (vx * vx + vy * vy)))
        return math.hypot(x - p[0] - t * vx, y - p[1] - t * vy)
    tri = [verts[0], verts[2], verts[4]]
    for y in range(16):
        for x in range(16):
            X, Y = x + 0.5, y + 0.5
            if (x, y) not in px:
                continue
            e = min(seg(verts[k], verts[(k + 1) % 6], X, Y) for k in range(6))
            inner = min(seg(tri[k], tri[(k + 1) % 3], X, Y) for k in range(3))

            def inside(a, b, c):
                s1 = (b[0] - a[0]) * (Y - a[1]) - (b[1] - a[1]) * (X - a[0])
                s2 = (c[0] - b[0]) * (Y - b[1]) - (c[1] - b[1]) * (X - b[0])
                s3 = (a[0] - c[0]) * (Y - c[1]) - (a[1] - c[1]) * (X - c[0])
                return (s1 >= 0 and s2 >= 0 and s3 >= 0) or (s1 <= 0 and s2 <= 0 and s3 <= 0)
            if e < 0.62:
                px[(x, y)] = (126, 56, 206)
            elif inner < 0.55:
                px[(x, y)] = (170, 96, 240)
            elif inside(*tri):
                d = math.hypot(X - cx, Y - (cy + 0.7))
                px[(x, y)] = (255, 250, 214) if d < 1.1 else (255, 224, 120) if d < 2.0 else (244, 184, 72)
    # runes along the edge, each a different colour
    for (x, y, c) in ((3, 3, (60, 196, 206)), (3, 7, (230, 72, 124)), (3, 11, (255, 196, 80)), (12, 3, (230, 72, 124)), (12, 8, (60, 196, 206)),
                      (5, 13, (255, 196, 80)), (8, 2, (150, 90, 230)), (6, 14, (60, 196, 206))):
        if (x, y) in px:
            px[(x, y)] = c
    out = add_outline(px, (44, 30, 36))
    for (x, y, c) in ((14, 1, (255, 240, 160)), (1, 14, (255, 240, 160)), (15, 7, (200, 150, 255)), (0, 4, (200, 150, 255))):
        out.setdefault((x, y), c)
    return out


# ================================================================ the item models
GENERATED_DISPLAY = {                                  # minecraft:item/generated's transforms
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def flat_model(sprite):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + sprite}}


def marker_model(item_id, display):
    """The 3D marker model client/WeaponRenderer draws in hand, on the ground and in frames (the inventory draws <id>_icon instead). Both hands get their
    transforms spelled out (the off-hand ones are the right-hand ones; the game mirrors them for the left hand)."""
    import copy
    display = copy.deepcopy(display)
    for side in ("thirdperson", "firstperson"):
        display.setdefault(side + "_lefthand", copy.deepcopy(display[side + "_righthand"]))
    return {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": display}


def write_models():
    import copy
    # the lantern is carried by its ring: hung a little lower in the hand than a flat item
    lantern = copy.deepcopy(GENERATED_DISPLAY)
    lantern["thirdperson_righthand"]["translation"] = [0, 1.2, 1]
    lantern["firstperson_righthand"]["translation"] = [1.13, 1.4, 1.13]
    write_model("ki_resonance_lantern", marker_model("ki_resonance_lantern", lantern))
    for state, spec in LANTERN_STATES.items():
        write_model("ki_resonance_lantern_flame_" + state, flat_model(spec[5]))
    icon = flat_model("ki_resonance_lantern_off")
    # flame: 0 off, 0.34 amber, 0.67 blue, 1.0 red; the thresholds sit between those values so a value a little off still picks its state
    icon["overrides"] = [{"predicate": {"nusmp:flame": thr}, "model": "nusmp:item/ki_resonance_lantern_flame_" + state}
                         for thr, state in ((0.17, "amber"), (0.5, "blue"), (0.84, "red"))]
    write_model("ki_resonance_lantern_icon", icon)
    # the compass: the needle's angle as a fraction of a turn clockwise from straight up, 32 frames, thresholds halfway between them (vanilla's compass way)
    write_model("anti_bird_compass", marker_model("anti_bird_compass", GENERATED_DISPLAY))
    for k in range(32):
        write_model("anti_bird_compass_%02d" % k, flat_model("anti_bird_compass_%02d" % k))
    icon = flat_model("anti_bird_compass_00")
    icon["overrides"] = [{"predicate": {"nusmp:angle": 0.0}, "model": "nusmp:item/anti_bird_compass_00"}]
    icon["overrides"] += [{"predicate": {"nusmp:angle": round((k - 0.5) / 32, 6)}, "model": "nusmp:item/anti_bird_compass_%02d" % k} for k in range(1, 32)]
    icon["overrides"].append({"predicate": {"nusmp:angle": round(31.5 / 32, 6)}, "model": "nusmp:item/anti_bird_compass_00"})
    write_model("anti_bird_compass_icon", icon)
    for item_id in ("glutton_chalice", "gamblers_grimoire_page"):
        write_model(item_id, marker_model(item_id, GENERATED_DISPLAY))
        write_model(item_id + "_icon", flat_model(item_id))
    # the sheath is long and diagonal like a sword: the Magebane Longsword's transforms
    src = json.load(open(os.path.join(MODELS, "magebane_longsword.json")))
    write_model("devil_bound_sheath", marker_model("devil_bound_sheath", src["display"]))
    write_model("devil_bound_sheath_icon", {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/devil_bound_sheath"}})
    # the mantle and the band are never held: plain flat items (the _icon twins are for uniformity and are not needed)
    for item_id in ("mantle_of_the_scavenger", "band_of_the_spatial_anchor"):
        write_model(item_id, flat_model(item_id))
        write_model(item_id + "_icon", flat_model(item_id))


# ================================================================ main
def main():
    for state, spec in LANTERN_STATES.items():
        save(lantern(state), spec[5])
    for k in range(32):
        save(compass_frame(k), "anti_bird_compass_%02d" % k)
    save(compass_frame(0), "anti_bird_compass")
    save(chalice(), "glutton_chalice")
    save(mantle(), "mantle_of_the_scavenger")
    save(band(), "band_of_the_spatial_anchor")
    save(sheath(), "devil_bound_sheath")
    save(page(), "gamblers_grimoire_page")
    write_models()
    print("wrote the relic sprites and item models under", os.path.normpath(ASSETS))


if __name__ == "__main__":
    main()
