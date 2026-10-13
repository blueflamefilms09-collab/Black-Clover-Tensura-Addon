"""Relics of Many Worlds, wave 1: the 16x16 inventory icons and the item models of the ten hand-held relics.

    python3 -B tools/relics/wave1_icons.py            (idempotent)

Writes, per id,
  assets/nusmp/textures/item/<id>.png            the sprite: the old-style inventory icon (kept as the icon, the item itself is 3D)
  assets/nusmp/models/item/<id>_icon.json        the flat inventory model of that sprite
  assets/nusmp/models/item/<id>.json             the in-hand model: builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/<id>.txt
                                                 (the display transforms of the Bloodbound Katana for the diagonal weapons, of the Spirit
                                                 Charm for the two upright tools, the Viola and the Book)
Every icon is drawn from primitives (lines, discs) on a 16 x 16 canvas, then given a one pixel dark outline; the grade shows as a
coloured pixel at the pommel / base and the glow as bright pixels at gems, runes and blade edges.
"""
import colorsys
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
ICONS = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")
S = 16

GRADE = {"NORMAL": 0xAAAAAA, "SPECIAL": 0x55FF55, "RARE": 0x55FFFF, "UNIQUE": 0xFF77FF, "LEGEND": 0xFFAA00, "GOD": 0xFF5555, "GENESIS": 0xFFFFFF}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < S and 0 <= y < S:
            self.px[(x, y)] = tuple(c)

    def line(self, x0, y0, x1, y1, c, c2=None):
        n = int(max(abs(x1 - x0), abs(y1 - y0)))
        for i in range(n + 1):
            t = i / max(1, n)
            col = c if c2 is None else mix(c, c2, t)
            self.put(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, col)

    def disc(self, cx, cy, r, c, c2=None):
        for y in range(S):
            for x in range(S):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    t = 0.0 if r <= 0 else d / r
                    self.put(x, y, c if c2 is None else mix(c, c2, t))

    def ring(self, cx, cy, r0, r1, c):
        for y in range(S):
            for x in range(S):
                if r0 <= math.hypot(x - cx, y - cy) <= r1:
                    self.put(x, y, c)

    def erase_disc(self, cx, cy, r):
        for k in [k for k in self.px if math.hypot(k[0] - cx, k[1] - cy) <= r]:
            del self.px[k]

    def outline(self, c):
        edge = {}
        for (x, y) in list(self.px):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                k = (x + dx, y + dy)
                if k not in self.px and 0 <= k[0] < S and 0 <= k[1] < S:
                    edge[k] = c
        self.px.update(edge)

    def image(self):
        im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        p = im.load()
        for (x, y), c in self.px.items():
            p[x, y] = (c[0], c[1], c[2], 255)
        return im


def diag_blade(cv, x0, y0, x1, y1, dark, mid, light, width=2):
    """A blade along the diagonal from (x0, y0) hilt end to (x1, y1) tip: light edge on the upper side, dark on the lower."""
    n = int(max(abs(x1 - x0), abs(y1 - y0)))
    for i in range(n + 1):
        t = i / max(1, n)
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        cv.put(x, y, mid)
        if width >= 2 and i < n:
            cv.put(x + 1, y, dark)
            cv.put(x, y - 1, light)
        if width >= 3 and i < n - 1:
            cv.put(x, y + 1, dark)
            cv.put(x - 1, y, mid)


def grade_pixel(cv, x, y, grade):
    cv.put(x, y, rgb(GRADE[grade]))


# ---------------------------------------------------------------- the ten icons (each returns a Canvas and the outline colour)
def staff_of_moonlight():
    cv = Canvas()
    silver, hi, lo = (200, 208, 228), (244, 248, 255), (118, 128, 168)
    blue, blue2 = (46, 58, 120), (84, 100, 190)
    cv.line(2, 14, 9, 7, silver, lo)                                  # shaft
    for k in range(3, 8):                                              # the star-speckled blue wrap with a trailing ribbon
        cv.put(2 + k, 14 - k, blue if k % 2 else blue2)
    cv.put(3, 15, blue2)
    cv.put(2, 15, blue)
    cv.put(1, 13, blue2)
    cv.put(8, 8, silver)
    for (x, y) in ((8, 6), (8, 5), (8, 4), (9, 3), (10, 2), (11, 2), (12, 2), (13, 3), (14, 4)):    # the crescent: an arc that opens to the lower right
        cv.put(x, y, silver if (x + y) % 3 else hi)
    cv.put(9, 7, silver)
    cv.put(10, 8, lo)
    cv.put(13, 3, hi)
    cv.disc(11.3, 5.2, 1.5, (236, 244, 255), (170, 196, 240))          # the moon
    cv.put(10.6, 4.6, (255, 255, 255))
    cv.put(12, 6, (150, 176, 228))
    return cv, (22, 28, 66)


def liquid_sword():
    cv = Canvas()
    aqua, aqua_hi, aqua_lo = (120, 214, 214), (214, 250, 246), (60, 150, 170)
    for k in range(0, 9):                                              # a blade that twists: the diagonal wobbles by a pixel
        x, y = 6 + k, 9 - k
        wob = 1 if k in (3, 4, 5) else 0
        cv.put(x, y + wob, aqua)
        cv.put(x + 1, y + wob, aqua_lo)
        cv.put(x, y - 1 + wob, aqua_hi)
    cv.put(15, 0, (236, 252, 255))
    cv.put(8, 12, aqua_lo)                                             # a drip
    cv.put(11, 9, aqua_hi)
    pale, pale2 = (210, 240, 226), (150, 196, 184)
    cv.line(2, 13, 5, 10, pale, pale2)                                 # the fluted glass hilt
    cv.put(3, 13, pale2)
    cv.line(4, 12, 6, 14, (236, 244, 240))                             # the white wing crossguard
    cv.put(5, 9, (236, 244, 240))
    cv.put(6, 10, (255, 255, 255))
    cv.disc(1.5, 14.5, 1.3, (244, 252, 250), (200, 232, 224))          # the pearl pommel
    return cv, (20, 70, 84)


def marids_valor():
    cv = Canvas()
    ice, ice_hi, ice_lo = (120, 200, 246), (214, 244, 255), (58, 130, 206)
    pts = [(5, 10), (6, 9), (7, 7), (8, 6), (9, 4), (10, 3), (11, 2), (12, 1), (13, 1), (14, 0), (14, 1)]
    for (x, y) in pts:                                                 # the curved scimitar blade
        cv.put(x, y, ice)
        cv.put(x + 1, y + 1, ice_lo)
        cv.put(x - 1, y, ice_hi)
    cv.put(15, 0, (240, 252, 255))
    cv.put(9, 8, ice_lo)                                               # spray
    cv.put(11, 7, ice_hi)
    steel, steel_hi = (176, 186, 204), (238, 244, 255)
    cv.line(2, 13, 4, 11, (110, 124, 150))                             # grip
    cv.line(3, 10, 6, 13, steel, steel_hi)                             # the simple crossguard
    cv.put(1, 14, steel)                                               # the curled hook pommel
    cv.put(0, 13, steel_hi)
    cv.put(0, 12, steel)
    cv.put(1, 12, steel)
    return cv, (20, 52, 96)


def catstaff():
    cv = Canvas()
    wood, wood_hi, wood_lo = (212, 192, 150), (240, 226, 190), (150, 124, 90)
    red, red2 = (164, 54, 50), (214, 92, 80)
    cv.line(1, 14, 10, 5, wood, wood_lo)
    for k in range(4, 8):                                              # the red cloth grip
        cv.put(1 + k, 14 - k, red if k % 2 else red2)
        cv.put(2 + k, 14 - k, red)
    cv.put(3, 10, wood_hi)                                             # the tail spiralling round the shaft
    cv.put(4, 11, wood_hi)
    cv.put(6, 8, wood_hi)
    cv.put(7, 7, wood_lo)
    fur, fur2 = (176, 150, 118), (120, 96, 72)                         # the sitting cat on top
    cv.disc(12.2, 3.4, 2.1, fur, fur2)
    cv.put(10.5, 0.8, fur2)
    cv.put(10.5, 1.5, fur)                                             # ears
    cv.put(14.6, 0.8, fur2)
    cv.put(14.4, 1.5, fur)
    cv.put(11.2, 3.2, (252, 218, 70))                                  # yellow eyes
    cv.put(13.4, 3.2, (252, 218, 70))
    cv.put(12.2, 2.0, (160, 70, 52))                                   # red-brown face mark
    cv.put(12.2, 4.4, (230, 190, 170))
    cv.put(0, 15, (160, 70, 52))                                       # the second cat at the foot
    cv.put(1, 15, (160, 70, 52))
    return cv, (60, 40, 36)


def rainbow_rapier():
    cv = Canvas()
    for k in range(0, 10):                                             # a thin blade through the spectrum
        x, y = 5 + k, 10 - k
        r, g, b = colorsys.hsv_to_rgb(min(0.82, k / 9 * 0.82), 0.65, 1.0)
        c = (int(r * 255), int(g * 255), int(b * 255))
        cv.put(x, y, c)
        cv.put(x + 1, y, shade(c, 0.72))
    cv.put(15, 0, (255, 255, 255))
    sil, sil_hi = (186, 194, 214), (244, 248, 255)
    cv.line(2, 12, 4, 14, sil, sil_hi)                                 # swept guard
    cv.line(3, 10, 6, 13, sil, sil_hi)
    cv.put(4, 10, sil_hi)
    cv.put(6, 12, sil)
    cv.disc(3.0, 13.0, 1.3, (150, 120, 230), (96, 76, 170))           # the cup hilt with its ribbon
    cv.put(1, 15, (110, 176, 255))
    cv.put(0, 14, (110, 176, 255))
    cv.put(2, 15, (150, 120, 230))
    return cv, (44, 36, 90)


def astralis():
    cv = Canvas()
    silver, hi, lo = (206, 212, 226), (248, 250, 255), (120, 128, 156)
    purple, purple2 = (122, 62, 196), (176, 112, 255)
    cv.line(1, 14, 10, 5, silver, lo)
    for k in range(5, 9):                                              # the purple wrapped section
        cv.put(1 + k, 14 - k, purple if k % 2 else purple2)
    cv.put(1, 15, silver)
    cv.put(0, 15, lo)
    cv.line(9, 8, 14, 3, silver)                                       # the swept filigree crescent
    cv.line(8, 5, 12, 1, silver)
    cv.put(8, 4, hi)
    cv.put(14, 4, hi)
    cv.put(15, 2, silver)
    cv.disc(11.4, 3.8, 1.9, purple, purple2)                           # the faceted gem
    cv.put(10.8, 3.2, (255, 238, 255))
    cv.put(12.2, 4.4, (92, 36, 150))
    cv.put(11, 7, (210, 170, 255))                                     # the purple watercolour glow
    cv.put(14, 6, (210, 170, 255))
    return cv, (44, 22, 84)


def deliverance():
    cv = Canvas()
    teal, teal_hi, teal_lo = (64, 176, 156), (170, 240, 214), (24, 104, 96)
    for k in range(0, 10):
        x, y = 5 + k, 10 - k
        cv.put(x, y, teal)
        cv.put(x + 1, y, teal_lo)
        cv.put(x, y - 1, teal_hi)
    for k in (1, 3, 5, 7):                                             # the serpent of green fire winding up the blade
        cv.put(5 + k, 9 - k, (140, 255, 170))
        cv.put(6 + k, 10 - k, (96, 224, 130))
    cv.put(15, 0, (230, 255, 240))
    gold, gold_hi = (150, 128, 60), (236, 212, 120)
    cv.line(3, 10, 6, 13, gold, gold_hi)                               # the cross-guard
    cv.put(4, 11, (20, 70, 64))
    cv.put(2, 12, (20, 66, 60))                                        # the grip and the pommel
    cv.put(1, 13, (20, 56, 52))
    cv.put(0, 14, gold_hi)
    return cv, (6, 40, 38)


def wand_of_bubbles():
    cv = Canvas()
    rod, rod_hi, rod_lo = (190, 176, 236), (232, 224, 255), (130, 116, 188)
    cv.line(2, 13, 11, 4, rod, rod_hi)
    brown, brown2 = (122, 84, 54), (170, 120, 80)
    for k in range(1, 4):
        cv.put(2 + k, 13 - k, brown if k % 2 else brown2)
    cv.put(1, 14, (206, 240, 255))
    cv.put(0, 15, (206, 240, 255))
    cv.disc(13.0, 2.4, 1.5, (190, 236, 250), (150, 200, 240))          # the bubble blown from the loop at the tip
    cv.put(12.4, 1.8, (255, 255, 255))
    for (x, y) in ((5, 3), (8, 0), (13, 7), (10, 9)):                  # a few free bubbles
        cv.put(x, y, (255, 255, 255))
        cv.put(x + 1, y, (190, 226, 250))
        cv.put(x, y + 1, (190, 226, 250))
        cv.put(x + 1, y + 1, (160, 200, 244))
    return cv, (70, 74, 140)


def viola_of_moonlit_spirits():
    cv = Canvas()
    body, body_hi = (44, 46, 58), (92, 96, 120)
    for y in range(8, 15):                                             # a wide flat paddle body at the lower left
        for x in range(1, 8):
            if (x - 1) + (14 - y) < 11:
                cv.put(x, y, body if (x + y) % 4 else body_hi)
    cv.line(7, 8, 12, 3, (70, 56, 50), (110, 86, 70))                  # the neck
    cv.put(13, 2, (238, 232, 210))                                     # pegs
    cv.put(11, 2, (238, 232, 210))
    cv.put(14, 1, (190, 196, 210))                                     # the crescent-hooked scroll
    cv.put(15, 2, (190, 196, 210))
    cv.put(15, 3, (150, 156, 170))
    cv.ring(4.2, 11.0, 0.8, 1.9, (140, 220, 255))                      # the glowing crescent sound hole
    cv.erase_disc(5.4, 10.2, 1.4)
    cv.put(3, 11, (220, 248, 255))
    cv.put(2, 9, (140, 200, 255))                                      # ghostly wisps
    cv.put(3, 7, (170, 214, 255))
    cv.put(5, 6, (140, 200, 255))
    return cv, (14, 16, 24)


def book_of_mispells():
    cv = Canvas()
    teal, teal_hi, teal_lo = (42, 118, 126), (76, 168, 170), (22, 70, 84)
    for y in range(3, 14):                                             # a thick book, slightly turned
        for x in range(3, 13):
            cv.put(x, y, teal if (x + y) % 5 else teal_hi)
    for y in range(4, 14):
        cv.put(12, y, (232, 220, 190))                                 # the page block
    cv.put(13, 5, (210, 196, 166))
    cv.put(13, 12, (210, 196, 166))
    gold, gold_hi = (200, 160, 70), (250, 224, 130)
    for (x, y) in ((3, 3), (4, 3), (3, 4), (11, 3), (10, 3), (11, 4), (3, 13), (4, 13), (3, 12), (11, 13), (10, 13), (11, 12)):
        cv.put(x, y, gold if (x + y) % 2 else gold_hi)                 # corner caps
    cv.put(12, 8, gold_hi)                                             # the clasp
    cv.put(13, 8, gold)
    for (x, y) in ((6, 5), (7, 6), (7, 7), (8, 8), (6, 8), (7, 9), (9, 6)):
        cv.put(x, y, (20, 24, 40))                                     # the scorch and ink damage
    cv.put(5, 6, (236, 230, 170))                                      # the wobbly title
    cv.put(6, 6, (236, 230, 170))
    cv.put(9, 11, (150, 100, 200))                                     # ribbon bookmarks
    cv.put(9, 12, (150, 100, 200))
    cv.put(10, 14, (100, 160, 90))
    cv.put(8, 14, (210, 190, 140))
    return cv, (12, 34, 44)


ICONS_DEF = [
    # id, drawer, grade, upright (the two tools stand upright and use the upright display), the pixel for the grade (x, y)
    ("staff_of_moonlight", staff_of_moonlight, "UNIQUE", False, (1, 14)),
    ("liquid_sword", liquid_sword, "RARE", False, (0, 15)),
    ("marids_valor", marids_valor, "RARE", False, (0, 14)),
    ("catstaff", catstaff, "UNIQUE", False, (0, 14)),
    ("rainbow_rapier", rainbow_rapier, "RARE", False, (1, 13)),
    ("astralis_the_eye_of_the_universe", astralis, "GENESIS", False, (0, 14)),
    ("deliverance", deliverance, "UNIQUE", False, (1, 15)),
    ("wand_of_bubbles", wand_of_bubbles, "NORMAL", False, (0, 14)),
    ("viola_of_moonlit_spirits", viola_of_moonlit_spirits, "RARE", True, (2, 14)),
    ("book_of_mispells", book_of_mispells, "RARE", True, (4, 14)),
]

DIAGONAL_DISPLAY = "bloodbound_katana"
UPRIGHT_DISPLAY = "spirit_charm"


def write_models(item_id, upright):
    src = json.load(open(os.path.join(MODELS, UPRIGHT_DISPLAY if upright else DIAGONAL_DISPLAY) + ".json"))
    model = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": src["display"]}
    with open(os.path.join(MODELS, item_id + ".json"), "w", newline="\n") as f:
        json.dump(model, f, indent=2)
    icon = {"parent": "minecraft:item/generated" if upright else "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}
    with open(os.path.join(MODELS, item_id + "_icon.json"), "w", newline="\n") as f:
        json.dump(icon, f, indent=2)


def main(only=None):
    os.makedirs(ICONS, exist_ok=True)
    for item_id, fn, grade, upright, gp in ICONS_DEF:
        if only and item_id not in only:
            continue
        cv, outline = fn()
        cv.outline(outline)
        gx, gy = gp
        cv.put(gx, gy, rgb(GRADE[grade]))                              # the grade-coloured rim pixel
        cv.image().save(os.path.join(ICONS, item_id + ".png"))
        write_models(item_id, upright)
        print("icon", item_id)


if __name__ == "__main__":
    import sys
    main(sys.argv[1:])
