#!/usr/bin/env python3
"""
1.21.0: the 16x16 inventory sprites of the Dimensional Nexus relics (docs/living_dungeon.md), drawn to the dungeon's look:
a dark outline, three or four shades per material, small bright glints and glows. (The Ki-Resonance Lantern and the Anti-Bird Compass, the navigation
gear this script also drew, were removed with the Living Dungeon in 1.58.0: docs/labyrinth/mega_v2_prune.md.)

    python3 tools/gen_nexus_relic_icons.py

Writes assets/nusmp/textures/item/
  glutton_chalice.png, mantle_of_the_scavenger.png, band_of_the_spatial_anchor.png, devil_bound_sheath.png, gamblers_grimoire_page.png
  amulet_of_the_juggernauts_blindness.png (the Sentient Dungeon's fourth relic)
and under assets/nusmp/models/item/ the item models: <id>.json (the 3D marker client/WeaponRenderer draws in hand: the chalice, the sheath,
the page and the amulet; the flat model for the mantle and the band) and <id>_icon.json (the inventory icon).
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


# ================================================================ the Amulet of the Juggernaut's Blindness
# A tarnished dark-gold medallion hung on a chain (a V of links to a small ring), a closed, slit eye in its dish: a heavy gold lid over a seam of
# violet light that bleeds a little onto the lower lid, and a few violet motes round it (the Dungeon cannot see what this eye hides).
AMULET_GOLD = {"H": (244, 214, 128), "L": (206, 160, 70), "M": (152, 110, 48), "D": (98, 68, 34), "S": (58, 40, 28)}
AMULET_EYE_LID = {          # row: {x: gold shade}
    8: {6: "L", 7: "H", 8: "L", 9: "M"},
    9: {5: "M", 6: "L", 7: "L", 8: "M", 9: "M", 10: "D"},
    11: {6: "D", 7: "D", 8: "S", 9: "S"},
    12: {6: "S", 9: "S"},
}
AMULET_SEAM = {4: (104, 62, 196), 5: (104, 62, 196), 6: (184, 132, 255), 7: (246, 232, 255), 8: (246, 232, 255), 9: (184, 132, 255), 10: (104, 62, 196), 11: (104, 62, 196)}
AMULET_SEAM_BLEED = {5: (84, 52, 128), 6: (118, 78, 168), 7: (150, 104, 206), 8: (150, 104, 206), 9: (118, 78, 168), 10: (84, 52, 128)}


def amulet():
    px = {}
    g = AMULET_GOLD
    cx, cy, radius, rim = 8.0, 10.0, 5.0, 1.3
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > radius:
                continue
            lit = (-dx - dy) / (d + 1e-6)
            if d > radius - rim:                                              # the rim: a gold ring lit from the upper left
                px[(x, y)] = g["H"] if lit > 0.8 else g["L"] if lit > 0.2 else g["M"] if lit > -0.55 else g["D"]
            else:                                                             # the dish: a dark violet field, a shade lighter on the lit side
                px[(x, y)] = (30, 22, 46) if lit < 0.4 else (44, 30, 68)
    for y, row in AMULET_EYE_LID.items():                                     # the closed eye: the lids, the seam of light between them, its bleed
        for x, shade in row.items():
            px[(x, y)] = g[shade]
    for x, c in AMULET_SEAM.items():
        px[(x, 10)] = c
    for x, c in AMULET_SEAM_BLEED.items():
        px[(x, 11)] = c
    for (x, y, shade) in ((7, 3, "L"), (8, 3, "M"), (6, 4, "H"), (9, 4, "D"), (7, 5, "M"), (8, 5, "D")):      # the ring the chain hangs from
        px[(x, y)] = g[shade]
    for i, (x, y) in enumerate(((2, 0), (3, 1), (4, 2), (5, 3), (6, 4))):     # the chain: a V of links, alternately lit and in shadow
        px[(x, y)] = g["L"] if i % 2 == 0 else g["D"]
        px[(15 - x, y)] = g["M"] if i % 2 == 0 else g["S"]
    out = add_outline(px)
    for (x, y, c) in ((14, 6, (200, 160, 255)), (1, 9, (150, 100, 240)), (14, 12, (150, 100, 240))):       # motes of violet light
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
    for item_id in ("glutton_chalice", "gamblers_grimoire_page", "amulet_of_the_juggernauts_blindness"):
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
    save(chalice(), "glutton_chalice")
    save(mantle(), "mantle_of_the_scavenger")
    save(band(), "band_of_the_spatial_anchor")
    save(sheath(), "devil_bound_sheath")
    save(page(), "gamblers_grimoire_page")
    save(amulet(), "amulet_of_the_juggernauts_blindness")
    write_models()
    print("wrote the relic sprites and item models under", os.path.normpath(ASSETS))


if __name__ == "__main__":
    main()
