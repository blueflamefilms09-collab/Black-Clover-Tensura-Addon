#!/usr/bin/env python3
"""Bust v2: a Lesser Summon bust of its own for every Djinn whose old bust was a copy (docs/djinn_design_audit.md).

    python tools/djinn/bust2.py            # writes <id>_bust2.png (the 7 light busts that were copies) and <id>_d_bust2.png (all 36 dark ones)
    python tools/djinn/bust2.py --sheet D  # also writes before/after contact sheets into directory D

Why: the old busts (tools/relics/djinn_icons.make_bust, djinn_dark.make_dark_bust) draw one head and one pair of shoulders and only change the colour and the
Look's horns, so Baal, Vinea and Haagenti were the same picture; all 36 dark busts were one black figure with a different gem. A v2 bust is drawn from the
Djinn's ASPECT: its own headgear (the animal or object of its title: ram, lion mane, unicorn horn, crab claws, crow, peacock fan ...), its element's shoulder
mantle (flames, bolts, waves, feathers, leaves, rock, sound arcs, rays) and its element's glyph on the chest, in the Djinn's own colours. Old files are never touched
(owner rule 2); the client picks the v2 file unless `djinnLookClassic` is on (client/relics/djinn/DjinnLook.bustPath).
Pure PIL, deterministic (no random numbers), 32 x 32 like the old ones.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/anime_multiverse/textures/entity/djinn"
sys.path.insert(0, str(Path(__file__).resolve().parent))
import design_audit as da  # noqa: E402  (the Djinn table parser)

S = 4  # supersampling of the drawing, the result is reduced to 32 x 32
GOLD = (222, 178, 74)

# the light busts that were copies of another (the audit's pairs, one of each pair is kept as it is); the dark busts are all redone
LIGHT_REWORK = ["balam", "haagenti", "vinea", "alloces", "furfur", "furcas", "amdusias"]

# id -> (horns, ears, crown): the headgear of its own title. Within one element no two are equal (checked below).
HEAD = {
    # heat
    "amon": ("ram", "none", "rays"), "marchosias": ("none", "wolf", "flames"), "belial": ("none", "none", "broken"), "balam": ("ram", "none", "triple"),
    "alloces": ("none", "cat", "mane"),
    # lightning
    "baal": ("crescent", "none", "bolts"), "bifron": ("stag", "wolf", "none"), "valefor": ("none", "long", "hood"), "andromalius": ("none", "none", "seeker"),
    # water
    "vinea": ("none", "fin", "waves"), "vassago": ("giant", "none", "none"), "agares": ("none", "none", "croc"), "gusion": ("none", "none", "panes"),
    "vepar": ("none", "fin", "coral"), "crocell": ("crescent", "none", "crystals"),
    # wind
    "focalor": ("none", "none", "swirl"), "paimon": ("crescent", "none", "plume"), "seere": ("none", "none", "wingcrest"), "furcas": ("none", "none", "knight"),
    # life
    "zagan": ("stag", "leaf", "none"), "barbatos": ("stag", "none", "oak"), "buer": ("none", "leaf", "wheel"),
    # strength
    "zepar": ("giant", "none", "brow"), "bael": ("none", "none", "crown5"), "kimaris": ("none", "horse", "mane"), "glasya_labolas": ("none", "hound", "spikes"),
    "haagenti": ("bull", "none", "wingcrest"),
    # sound
    "furfur": ("none", "none", "trumpets"), "shax": ("none", "none", "dove"), "amdusias": ("unicorn", "horse", "none"), "camio": ("none", "none", "crow"),
    "murmur": ("crescent", "none", "drum"),
    # light
    "sitri": ("none", "none", "rays"), "andrealphus": ("none", "none", "peacock"), "decarabia": ("claws", "none", "none"), "dantalion": ("none", "none", "runes"),
}

ELEMENT_OF = {}  # filled from the Djinn table in main()


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


class Pen:
    def __init__(self, im):
        self.d = ImageDraw.Draw(im)

    def poly(self, pts, fill):
        self.d.polygon([(x * S, y * S) for x, y in pts], fill=fill)

    def mpoly(self, pts, fill):  # a polygon and its mirror about x = 16
        self.poly(pts, fill)
        self.poly([(32 - x, y) for x, y in pts], fill)

    def line(self, pts, fill, w=1.0):
        self.d.line([(x * S, y * S) for x, y in pts], fill=fill, width=max(1, int(w * S)))

    def mline(self, pts, fill, w=1.0):
        self.line(pts, fill, w)
        self.line([(32 - x, y) for x, y in pts], fill, w)

    def ell(self, box, fill=None, outline=None, w=1.0):
        self.d.ellipse([box[0] * S, box[1] * S, box[2] * S, box[3] * S], fill=fill, outline=outline, width=max(1, int(w * S)))

    def arc(self, box, a0, a1, fill, w=1.0):
        self.d.arc([box[0] * S, box[1] * S, box[2] * S, box[3] * S], a0, a1, fill=fill, width=max(1, int(w * S)))


# ---------------------------------------------------------------- headgear (grid units; head is x 10..22, y 7..21, brow gem at (16, 10))
def horns(p, kind, c, dark_edge):
    if kind == "ram":
        p.mpoly([(10, 10), (7, 8), (5, 10), (5, 14), (7, 16), (8, 13), (7, 11), (9, 11)], c)
    elif kind == "giant":
        p.mpoly([(10, 9), (6, 7), (3, 9), (2, 14), (4, 18), (6, 15), (5, 12), (8, 11)], c)
    elif kind == "stag":
        p.mpoly([(11, 8), (9, 2), (10, 2), (12, 7)], c)
        p.mpoly([(9.6, 5.5), (6, 3.5), (6, 4.8), (9.8, 7.2)], c)
        p.mpoly([(10.4, 3.5), (8.5, 1), (9.5, 1), (11, 3)], c)
    elif kind == "crescent":
        p.mpoly([(10, 9), (7, 7), (6, 3), (8, 1), (7.5, 4), (9, 7), (11, 8.5)], c)
    elif kind == "bull":
        p.mpoly([(10, 10), (5, 11), (3, 8), (2, 3), (4.5, 7), (7, 8.5), (11, 9)], c)
    elif kind == "unicorn":
        p.poly([(15, 9), (17, 9), (16.4, 0), (16, -1), (15.6, 0)], c)
        for y in (2, 4, 6):
            p.line([(15.2, y + 1), (16.8, y)], dark_edge, 0.6)
    elif kind == "claws":
        p.mpoly([(10, 12), (6, 11), (3, 8), (3, 4), (5, 6), (5, 8), (7, 9), (10, 10)], c)
        p.mpoly([(3, 4), (4, 1), (5.5, 3), (4.5, 5)], c)


def ears(p, kind, c):
    if kind == "wolf":
        p.mpoly([(10, 11), (8, 3), (13, 8)], c)
        p.mpoly([(9.6, 9), (8.9, 5.5), (11.4, 8)], mix(c, (20, 10, 10), 0.5))
    elif kind == "cat":
        p.mpoly([(10, 12), (8, 5), (13, 9)], c)
    elif kind == "horse":
        p.mpoly([(10, 11), (8, 2), (11.5, 3), (13, 9)], c)
    elif kind == "hound":
        p.mpoly([(10, 10), (5, 11), (4, 18), (8, 15), (10, 14)], c)
    elif kind == "long":
        p.mpoly([(10, 12), (3, 7), (2, 9), (10, 15)], c)
    elif kind == "fin":
        p.mpoly([(10, 12), (5, 8), (4, 10), (5.5, 11), (4.5, 13), (6, 14), (10, 16)], c)
    elif kind == "leaf":
        p.mpoly([(10, 13), (5, 9), (4, 12), (10, 15)], c)
        p.mline([(10, 13.5), (5.5, 10.8)], (30, 60, 20), 0.5)


def crown(p, kind, c, accent, dark_edge):
    if kind == "crown5":
        for k in (-6, -3, 0, 3, 6):
            p.poly([(16 + k - 1.5, 9), (16 + k, 2 + abs(k) * 0.4), (16 + k + 1.5, 9)], c)
    elif kind == "rays":
        for k in (-7, -4.5, -2, 0, 2, 4.5, 7):
            a = math.radians(90 + k * 11)
            x2, y2 = 16 + math.cos(a) * 12, 14 - math.sin(a) * 12
            p.poly([(16 + math.cos(a - 0.13) * 8, 14 - math.sin(a - 0.13) * 8), (x2, y2), (16 + math.cos(a + 0.13) * 8, 14 - math.sin(a + 0.13) * 8)], c)
    elif kind == "flames":
        for k, h in ((-4, 4), (0, 7), (4, 4)):
            p.poly([(16 + k - 1.8, 9), (16 + k - 0.5, 9 - h * 0.5), (16 + k + 0.4, 9 - h), (16 + k + 0.7, 9 - h * 0.45), (16 + k + 1.8, 9)], c)
    elif kind == "triple":
        for k, h in ((-5, 5), (0, 7), (5, 5)):
            p.poly([(16 + k - 1.6, 9), (16 + k, 9 - h), (16 + k + 1.6, 9)], c)
            p.ell((16 + k - 0.8, 9 - h * 0.55 - 0.8, 16 + k + 0.8, 9 - h * 0.55 + 0.8), fill=accent)
    elif kind == "broken":
        p.poly([(10.5, 9), (11, 3), (13, 6), (14.5, 4), (15, 9)], c)
        p.poly([(17.5, 9), (18.5, 5), (20, 7), (21.5, 2), (22, 9)], c)
        p.poly([(15.4, 9), (16.4, 6), (16.8, 9)], dark_edge)
    elif kind == "mane":
        for i in range(14):
            a = math.radians(-20 + i * 14.3)
            ox, oy = 16 + math.cos(math.pi - a) * 6, 14 - math.sin(math.pi - a) * 7
            ex, ey = 16 + math.cos(math.pi - a) * 11.5, 14 - math.sin(math.pi - a) * 12.5
            px, py = -math.sin(math.pi - a) * 1.6, -math.cos(math.pi - a) * 1.6
            p.poly([(ox + px, oy - py), (ex, ey), (ox - px, oy + py)], c)
    elif kind == "bolts":
        p.mline([(11, 8), (9, 5), (11, 4.5), (8, 0.5)], accent, 1.0)
        p.mline([(11, 8), (9, 5), (11, 4.5), (8, 0.5)], c, 0.5)
    elif kind == "hood":
        p.poly([(8, 20), (9, 9), (13, 4), (16, 2), (19, 4), (23, 9), (24, 20), (22, 14), (22, 9), (16, 6), (10, 9), (10, 14)], mix(c, (10, 10, 30), 0.6))
        p.line([(10, 20), (6, 25), (4, 24)], mix(c, (10, 10, 30), 0.4), 1.4)
    elif kind == "seeker":
        p.poly([(9, 14), (10, 9), (16, 4), (22, 9), (23, 14), (22, 10), (16, 7), (10, 10)], mix(c, (10, 10, 40), 0.6))
        p.ell((14.2, 5, 17.8, 8), fill=(255, 255, 255), outline=accent, w=0.6)
        p.ell((15.4, 5.6, 16.6, 7.4), fill=accent)
    elif kind == "waves":
        for k in (-6, -3, 0, 3, 6):
            p.arc((16 + k - 1.6, 4 + abs(k) * 0.4, 16 + k + 1.6, 9), 180, 360, c, 1.0)
        p.poly([(15, 9), (16, 2), (17, 9)], c)
    elif kind == "croc":
        p.poly([(10, 11), (11, 8), (16, 7), (21, 8), (22, 11), (16, 9.5)], c)
        for k in range(-3, 4):
            p.poly([(16 + k * 2.2 - 0.9, 8), (16 + k * 2.2, 5.5 - abs(k) * 0.3), (16 + k * 2.2 + 0.9, 8)], c)
        p.ell((11.6, 8.2, 13.2, 9.6), fill=accent)
        p.ell((18.8, 8.2, 20.4, 9.6), fill=accent)
    elif kind == "panes":
        for k, h in ((-6, 5), (-3, 7), (0, 9), (3, 7), (6, 5)):
            p.poly([(16 + k - 1.4, 9), (16 + k - 0.9, 9 - h), (16 + k + 0.9, 9 - h), (16 + k + 1.4, 9)], mix(c, (255, 255, 255), 0.4))
            p.line([(16 + k - 0.6, 8.5), (16 + k - 0.4, 9 - h + 1)], accent, 0.4)
    elif kind == "coral":
        for k, h in ((-5, 5), (-2, 7), (2, 6), (5, 4)):
            p.line([(16 + k, 9), (16 + k + 0.5, 9 - h)], c, 1.1)
            p.line([(16 + k + 0.3, 9 - h * 0.6), (16 + k + 2, 9 - h * 0.9)], c, 0.8)
        p.mpoly([(10, 9), (7, 6), (6, 9), (8, 11)], mix(c, (255, 255, 255), 0.5))  # a shell each side
    elif kind == "crystals":
        for k, h in ((-6, 4), (-3, 6), (0, 8), (3, 6), (6, 4)):
            p.poly([(16 + k - 1.3, 9), (16 + k - 0.6, 9 - h * 0.7), (16 + k, 9 - h), (16 + k + 0.6, 9 - h * 0.7), (16 + k + 1.3, 9)], mix(c, (255, 255, 255), 0.35))
    elif kind == "swirl":
        p.arc((9, 0, 23, 12), 190, 350, c, 1.2)
        p.arc((11, 2, 21, 10), 200, 340, accent, 0.8)
        p.mpoly([(10, 9), (5, 5), (6, 4), (11, 7)], c)
    elif kind == "plume":
        for k in (-4, 0, 4):
            p.poly([(16 + k - 1, 9), (16 + k * 1.6 - 1.2, 3), (16 + k * 1.8, 0.5), (16 + k * 1.6 + 1.2, 3), (16 + k + 1, 9)], mix(c, (255, 255, 255), 0.45))
    elif kind == "wingcrest":
        p.mpoly([(14.5, 9), (11, 6), (6, 4), (2, 5), (5, 7), (2, 8), (6, 9), (4, 11), (9, 10), (13, 10)], mix(c, (255, 255, 255), 0.4))
    elif kind == "knight":
        p.poly([(9.5, 12), (10, 6), (16, 3), (22, 6), (22.5, 12), (21, 12), (21, 8), (16, 5.5), (11, 8), (11, 12)], mix(c, (60, 70, 90), 0.5))
        p.poly([(15.4, 3), (16, -1), (16.6, 3)], mix(accent, (255, 255, 255), 0.5))
        p.line([(16, 3), (20, -1), (23, 1), (21, 6)], mix(accent, (255, 255, 255), 0.6), 1.0)
    elif kind == "oak":
        for k, h in ((-6, 4), (-3, 6), (0, 7), (3, 6), (6, 4)):
            p.ell((16 + k - 1.3, 9 - h, 16 + k + 1.3, 9 - h + 3.4), fill=mix(c, (40, 120, 40), 0.6))
    elif kind == "wheel":
        p.ell((6, -1, 26, 19), outline=mix(c, (255, 255, 255), 0.2), w=1.3)
        for a in range(0, 360, 72):
            r = math.radians(a - 90)
            p.line([(16, 9), (16 + math.cos(r) * 10, 9 + math.sin(r) * 10)], mix(c, (255, 255, 255), 0.2), 0.7)
    elif kind == "brow":
        p.poly([(9.5, 11), (10, 8.5), (16, 7.5), (22, 8.5), (22.5, 11), (16, 10)], mix(c, (30, 20, 20), 0.5))
        for k in (-6, -2, 2, 6):
            p.poly([(16 + k - 0.9, 8), (16 + k, 5.5), (16 + k + 0.9, 8)], c)
    elif kind == "spikes":
        for k in (-5, -2.5, 0, 2.5, 5):
            p.poly([(16 + k - 1.0, 9), (16 + k * 1.2, 4.5 + abs(k) * 0.4), (16 + k + 1.0, 9)], c)
    elif kind == "trumpets":
        p.mpoly([(10, 9), (8, 7), (4, 2), (2, 4), (6, 8), (9, 11)], c)
        p.mpoly([(2, 4), (4, 2), (3, 0.5), (1, 1.5)], mix(c, (255, 255, 255), 0.4))
    elif kind == "dove":
        p.mpoly([(14.5, 9), (11, 6), (6, 3), (2, 4), (5, 6), (3, 8), (7, 8), (12, 10)], (240, 240, 250))
        p.mline([(11, 8), (6, 5)], (170, 170, 190), 0.4)
        p.poly([(14, 11.5), (18, 11.5), (17, 15), (15, 15)], (240, 240, 250))  # the veil over the mouth, sewn
    elif kind == "crow":
        p.poly([(11, 9), (12, 5), (16, 3), (20, 5), (21, 9), (16, 7)], mix((20, 24, 34), accent, 0.15))
        p.poly([(21, 5), (25, 4), (21.5, 7)], mix((20, 24, 34), accent, 0.3))   # a beak
        p.mpoly([(10, 9), (7, 4), (9, 6), (7.5, 1), (11, 6)], mix((20, 24, 34), accent, 0.2))
    elif kind == "drum":
        p.ell((11, 1, 21, 6), fill=mix(c, (120, 60, 20), 0.4), outline=mix(c, (255, 255, 255), 0.4), w=0.5)
        p.line([(6, 6), (11, 1)], mix(c, (255, 255, 255), 0.5), 0.8)
        p.line([(26, 6), (21, 1)], mix(c, (255, 255, 255), 0.5), 0.8)
    elif kind == "peacock":
        for k in range(-5, 6):
            a = math.radians(90 + k * 13)
            x2, y2 = 16 + math.cos(a) * 12.5, 15 - math.sin(a) * 12.5
            p.line([(16, 9), (x2, y2)], mix(c, (20, 80, 60), 0.5), 0.7)
            p.ell((x2 - 1.1, y2 - 1.1, x2 + 1.1, y2 + 1.1), fill=(40, 190, 170), outline=(255, 220, 110), w=0.4)
    elif kind == "runes":
        p.ell((6, -1, 26, 19), outline=mix(c, (255, 255, 255), 0.4), w=0.7)
        for i in range(8):
            a = math.radians(-160 + i * 20)
            x, y = 16 + math.cos(a) * 10, 9 + math.sin(a) * 10
            p.line([(x - 0.7, y - 0.7), (x + 0.7, y + 0.7)], accent, 0.7)
            p.line([(x + 0.7, y - 0.7), (x - 0.7, y + 0.7)], accent, 0.5)


# ---------------------------------------------------------------- the element's shoulder mantle and chest glyph
def mantle(p, el, c, accent):
    if el == "HEAT":
        for side in (-1, 1):
            for k, h in ((7, 6), (10, 8), (13, 5)):
                x = 16 + side * k
                p.poly([(x - 1.6, 24), (x - 0.4, 24 - h * 0.5), (x + 0.2, 24 - h), (x + 0.6, 24 - h * 0.4), (x + 1.6, 24)], c)
    elif el == "LIGHTNING":
        for side in (-1, 1):
            pts = [(16 + side * 5, 20.5), (16 + side * 8, 21.5), (16 + side * 7, 23), (16 + side * 11, 24), (16 + side * 9.5, 25.5), (16 + side * 14, 27)]
            p.line(pts, accent, 1.5)
            p.line(pts, c, 0.7)
    elif el == "WATER":
        for side in (-1, 1):
            for i, k in enumerate((8, 10.6, 13.2)):
                x = 16 + side * k
                p.arc((x - 1.4, 21 + i * 0.8, x + 1.4, 25 + i * 0.8), 180, 360, c, 1.1)
            p.poly([(16 + side * 13, 24), (16 + side * 16, 21), (16 + side * 15, 27)], c)
    elif el == "WIND":
        for side in (-1, 1):
            for k, (dx, dy) in enumerate(((9, 4), (12, 7), (14.5, 10.5))):
                x0, y0 = 16 + side * 7, 22 + k * 1.7
                p.poly([(x0, y0 - 0.7), (16 + side * dx + side * 2, y0 + dy), (x0, y0 + 0.9)], mix(c, (255, 255, 255), 0.35))
    elif el == "LIFE":
        for side in (-1, 1):
            for k, (dx, dy) in enumerate(((8, 0), (11, 3), (13, 6))):
                x, y = 16 + side * dx, 22.5 + dy
                p.poly([(x - 2, y), (x, y - 2.4), (x + 2, y), (x, y + 2.4)], mix(c, (60, 150, 50), 0.4))
                p.line([(x - 1.6, y), (x + 1.6, y)], (220, 255, 190), 0.3)
    elif el == "STRENGTH":
        for side in (-1, 1):
            p.poly([(16 + side * 5, 21), (16 + side * 11, 19.5), (16 + side * 15, 22), (16 + side * 15, 27), (16 + side * 10, 28), (16 + side * 6, 25)], mix(c, (60, 60, 70), 0.35))
            p.poly([(16 + side * 11, 19.5), (16 + side * 12, 16), (16 + side * 13.5, 21)], c)
            p.poly([(16 + side * 14.5, 22), (16 + side * 18, 20), (16 + side * 15.5, 25)], c)
    elif el == "SOUND":
        for side in (-1, 1):
            for r in (3, 5.5, 8):
                p.arc((16 + side * 10 - r, 24 - r, 16 + side * 10 + r, 24 + r), 300 if side > 0 else 120, 60 if side > 0 else 240, c, 0.9)
    elif el == "LIGHT":
        for side in (-1, 1):
            for a in (-70, -40, -10, 20):
                r = math.radians(a)
                x0, y0 = 16 + side * 9, 24
                p.poly([(x0 + side * math.cos(r) * 2, y0 + math.sin(r) * 2 - 0.7), (x0 + side * math.cos(r) * 9, y0 + math.sin(r) * 9), (x0 + side * math.cos(r) * 2, y0 + math.sin(r) * 2 + 0.7)], c)


def glyph(p, el, accent):
    cx, cy = 16, 26
    if el == "HEAT":
        p.poly([(cx, cy - 3.2), (cx + 2.2, cy), (cx + 1.6, cy + 2.6), (cx, cy + 3.2), (cx - 1.6, cy + 2.6), (cx - 2.2, cy)], accent)
    elif el == "LIGHTNING":
        p.poly([(cx + 1.5, cy - 3.4), (cx - 1.8, cy + 0.4), (cx + 0.2, cy + 0.4), (cx - 1.2, cy + 3.4), (cx + 2.4, cy - 0.8), (cx + 0.3, cy - 0.8)], accent)
    elif el == "WATER":
        for i in (-2, 0, 2):
            p.arc((cx - 3.2, cy + i - 1.2, cx, cy + i + 1.2), 180, 360, accent, 0.9)
            p.arc((cx, cy + i - 1.2, cx + 3.2, cy + i + 1.2), 0, 180, accent, 0.9)
    elif el == "WIND":
        p.arc((cx - 3, cy - 3, cx + 3, cy + 3), 200, 20, accent, 1.0)
        p.arc((cx - 1.6, cy - 1.6, cx + 1.6, cy + 1.6), 20, 220, accent, 0.9)
        p.line([(cx + 2.8, cy + 0.8), (cx + 3.8, cy + 2.4)], accent, 0.9)
    elif el == "LIFE":
        p.poly([(cx - 3, cy + 3), (cx - 2.6, cy - 1.2), (cx, cy - 3.4), (cx + 2.8, cy - 2.4), (cx + 2.4, cy + 0.6), (cx, cy + 2.4)], accent)
        p.line([(cx - 3, cy + 3.2), (cx + 1.6, cy - 1.6)], (20, 60, 20), 0.6)
    elif el == "STRENGTH":
        p.poly([(cx - 2, cy - 3), (cx + 2, cy - 3), (cx + 3.4, cy), (cx + 2, cy + 3), (cx - 2, cy + 3), (cx - 3.4, cy)], accent)
        p.poly([(cx - 1, cy - 1.4), (cx + 1, cy - 1.4), (cx + 1.6, cy), (cx + 1, cy + 1.4), (cx - 1, cy + 1.4), (cx - 1.6, cy)], (30, 24, 30))
    elif el == "SOUND":
        p.ell((cx - 3, cy + 0.4, cx - 0.4, cy + 3), fill=accent)
        p.line([(cx - 0.6, cy + 1.6), (cx - 0.6, cy - 3.2), (cx + 2.8, cy - 2)], accent, 0.9)
    elif el == "LIGHT":
        for a in range(0, 360, 45):
            r = math.radians(a)
            p.poly([(cx + math.cos(r - 0.3) * 1.2, cy + math.sin(r - 0.3) * 1.2), (cx + math.cos(r) * (3.6 if a % 90 == 0 else 2.4), cy + math.sin(r) * (3.6 if a % 90 == 0 else 2.4)),
                    (cx + math.cos(r + 0.3) * 1.2, cy + math.sin(r + 0.3) * 1.2)], accent)


# ---------------------------------------------------------------- the whole bust
def draw(dj, dark):
    el = dj["aff"]
    c, g = rgb(dj["color"]), rgb(dj["glow"])
    horns_k, ears_k, crown_k = HEAD[dj["id"]]
    im = Image.new("RGBA", (32 * S, 32 * S), (0, 0, 0, 0))
    p = Pen(im)
    if dark:
        body = (28, 24, 38)
        trim = GOLD
        head_c = mix(GOLD, g, 0.18)
        accent = mix(g, (255, 255, 255), 0.2)
        mant = mix(GOLD, c, 0.35)
        for sg in (-1, 1):                                        # the black rukh wings of the old dark bust, kept
            p.poly([(16 + sg * 8, 21), (16 + sg * 15, 12), (16 + sg * 14, 24), (16 + sg * 10, 26)], (14, 12, 20, 180))
        p.ell((8, 0, 24, 4), outline=GOLD + (255,), w=1.0)       # the halo ring
        edge = (12, 10, 16)
    else:
        body = mix(c, (255, 255, 255), 0.15)
        trim = mix(g, (255, 255, 255), 0.25)
        head_c = trim
        accent = g
        mant = mix(c, g, 0.5)
        edge = mix(c, (0, 0, 0), 0.55)
    # headgear that sits behind the head first (wheel, runes, crow wings, mane)
    behind = crown_k in ("wheel", "runes", "mane", "peacock", "rays", "wingcrest", "dove", "hood")
    if behind:
        crown(p, crown_k, head_c + (240,), accent + (255,), edge + (255,))
    mantle(p, el, mant + (235,), accent)
    p.poly([(4, 32), (7, 22), (12, 19), (20, 19), (25, 22), (28, 32)], mix(body, (20, 20, 40), 0.35) + (210,))
    mantle(p, el, mant + (235,), accent)                       # drawn again over the shoulders: the mantle sits on them
    p.ell((10, 7, 22, 21), fill=mix(body, (255, 255, 255), 0.2) + (240,))
    p.poly([(11, 17), (21, 17), (19, 22), (13, 22)], mix(body, (20, 20, 40), 0.25) + (235,))
    if ears_k != "none":
        ears(p, ears_k, head_c + (235,))
    if horns_k != "none":
        horns(p, horns_k, head_c + (245,), edge + (255,))
    if not behind:
        crown(p, crown_k, head_c + (240,), accent + (255,), edge + (255,))
    for sgn in (-1, 1):                                        # glowing eyes
        p.d.rectangle([(16 + sgn * 3 - 1) * S, 13 * S, (16 + sgn * 3 + 1) * S, 14.5 * S], fill=(255, 255, 255, 255))
        p.d.rectangle([(16 + sgn * 3 - 1) * S - 2, 13 * S - 2, (16 + sgn * 3 + 1) * S + 2, 14.5 * S + 3], outline=accent + (200,))
    p.poly([(16, 9), (17, 11), (16, 12.2), (15, 11)], accent + (255,))      # brow gem
    glyph(p, el, accent + (255,))
    im = im.resize((32, 32), Image.LANCZOS)
    px = im.load()
    for y in range(32):                                       # the soft fade into the mist, as the old busts
        for x in range(32):
            r, gg, b, a = px[x, y]
            fade = 1.0 if y < 22 else max(0.0, 1.0 - (y - 22) / 11.0)
            px[x, y] = (r, gg, b, int(a * fade))
    return im


def sheet(path, rows, scale=5, cols=12):
    w = scale * 32
    n = len(rows)
    r = (n + cols - 1) // cols
    sh = Image.new("RGBA", (cols * w, r * (w + 14)), (74, 78, 92, 255))
    d = ImageDraw.Draw(sh)
    for i, (name, im) in enumerate(rows):
        x, y = (i % cols) * w, (i // cols) * (w + 14)
        sh.alpha_composite(im.resize((w, w), Image.NEAREST), (x, y))
        d.text((x + 3, y + w + 1), name, fill=(255, 255, 255, 255))
    sh.convert("RGB").save(path)


def main():
    roster = da.parse_enum()
    by = {d["id"]: d for d in roster}
    assert set(HEAD) == set(by), "every Djinn needs a headgear row: " + str(set(by) ^ set(HEAD))
    seen = {}
    for i, h in HEAD.items():
        k = (by[i]["aff"],) + h
        assert k not in seen, f"{i} and {seen[k]} have the same headgear within {by[i]['aff']}"
        seen[k] = i
    sheets = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else None
    before, after = [], []
    for i in sorted(by):
        dj = by[i]
        for dark in (False, True):
            if not dark and i not in LIGHT_REWORK:
                continue
            im = draw(dj, dark)
            im.save(OUT / f"{i}{'_d' if dark else ''}_bust2.png")
            old = Image.open(OUT / f"{i}{'_d' if dark else ''}_bust.png").convert("RGBA")
            before.append((i + ("_d" if dark else ""), old))
            after.append((i + ("_d" if dark else "") + " v2", im))
    print(f"wrote {len(after)} v2 busts")
    if sheets:
        Path(sheets).mkdir(parents=True, exist_ok=True)
        light = [(n, im) for n, im in before if not n.endswith("_d")]
        light2 = [(n, im) for n, im in after if "_d" not in n]
        dark = [(n, im) for n, im in before if n.endswith("_d")]
        dark2 = [(n, im) for n, im in after if "_d" in n]
        sheet(Path(sheets) / "bust_light_before.png", light, cols=7)
        sheet(Path(sheets) / "bust_light_after.png", light2, cols=7)
        sheet(Path(sheets) / "bust_dark_before.png", dark)
        sheet(Path(sheets) / "bust_dark_after.png", dark2)


if __name__ == "__main__":
    main()
