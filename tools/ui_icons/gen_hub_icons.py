#!/usr/bin/env python3
"""
Paints the custom icons of the Multiverse Hub v2 (docs/ui_hub.md, "Hub v2"): one icon per hub node, one per Codex page and one per Codex category.

    python3 -B tools/ui_icons/gen_hub_icons.py            writes the PNGs into src/main/resources/assets/anime_multiverse/textures/gui/hub/
    python3 -B tools/ui_icons/gen_hub_icons.py --sheet F  also writes a contact sheet (the icons at 32, 24 and 16 px, normal and glow) to F

The icons are DRAWN BY THIS SCRIPT with PIL (vector shapes on a 256 px canvas, a dark outline grown from their silhouette, downscaled to 32 x 32 with
Lanczos): there is no Blender / Blockbench / paint program in the build session and an icon is a flat picture, so a script is the honest tool; the
script is the source of truth, edit it and run it again. Every icon has a normal and a `_glow` variant (a soft halo in its colour and a brighter fill,
drawn for the hovered / open node). A `.png.mcmeta` with `blur: true` makes the game filter them when they are drawn at 24 or 16 px.
Colours: the node icons keep the node colour of HubNodes.Tab.rgb; the Codex page icons use the colour of their category.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageChops

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "gui", "hub")
K = 8                      # supersampling
N = 32                     # final size
OUTLINE = (10, 16, 36)     # 0x0A1024, the hub's panel colour


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


WHITE, GOLD, RED, GREEN = (255, 255, 255), (232, 196, 104), (255, 106, 106), (124, 240, 176)


class Pen:
    """Draws in 32-unit coordinates on a supersampled RGBA canvas."""

    def __init__(self, color):
        self.c = rgb(color)
        self.light = mix(self.c, WHITE, 0.55)
        self.dark = mix(self.c, (0, 0, 0), 0.45)
        self.img = Image.new("RGBA", (N * K, N * K), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    @staticmethod
    def p(x):
        return x * K

    def pts(self, pts):
        return [(x * K, y * K) for x, y in pts]

    def poly(self, pts, fill):
        self.d.polygon(self.pts(pts), fill=fill + (255,))

    def circ(self, cx, cy, r, fill):
        self.d.ellipse([(cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K], fill=fill + (255,))

    def ring(self, cx, cy, r, w, fill):
        self.d.ellipse([(cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K], outline=fill + (255,), width=max(1, int(w * K)))

    def line(self, a, b, w, fill):
        self.d.line([a[0] * K, a[1] * K, b[0] * K, b[1] * K], fill=fill + (255,), width=max(1, int(w * K)))
        for q in (a, b):
            self.circ(q[0], q[1], w / 2, fill)

    def rrect(self, x0, y0, x1, y1, r, fill):
        self.d.rounded_rectangle([x0 * K, y0 * K, x1 * K, y1 * K], radius=r * K, fill=fill + (255,))

    def rect(self, x0, y0, x1, y1, fill):
        self.d.rectangle([x0 * K, y0 * K, x1 * K - 1, y1 * K - 1], fill=fill + (255,))

    def arc(self, cx, cy, r, a0, a1, w, fill):
        self.d.arc([(cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K], a0, a1, fill=fill + (255,), width=max(1, int(w * K)))

    def star(self, cx, cy, big, small, n, fill, rot=-90.0):
        pts = []
        for i in range(2 * n):
            a = math.radians(rot + i * 180.0 / n)
            r = big if i % 2 == 0 else small
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
        self.poly(pts, fill)

    def ellipse_rot(self, cx, cy, rx, ry, deg, fill, steps=40):
        a = math.radians(deg)
        pts = []
        for i in range(steps):
            t = i * 2 * math.pi / steps
            x, y = rx * math.cos(t), ry * math.sin(t)
            pts.append((cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a)))
        self.poly(pts, fill)

    def ring_rot(self, cx, cy, rx, ry, deg, w, fill, steps=60):
        a = math.radians(deg)
        pts = []
        for i in range(steps + 1):
            t = i * 2 * math.pi / steps
            x, y = rx * math.cos(t), ry * math.sin(t)
            pts.append((cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a)))
        for i in range(steps):
            self.line(pts[i], pts[i + 1], w, fill)


# ---------------------------------------------------------------------------------------------------- the icons
# Each function draws one icon with the pen (colours: p.c main, p.light, p.dark, WHITE, GOLD, RED, GREEN).

def i_status(p):
    # an aura figure: spiky halo, head, body, arms; a bar underneath
    p.star(16, 13.5, 13.5, 8.6, 8, mix(p.c, (0, 0, 0), 0.5))
    p.star(16, 13.5, 11.2, 7.6, 8, p.dark)
    p.circ(16, 8, 3.3, WHITE)
    p.poly([(11.2, 12.6), (20.8, 12.6), (22.6, 20.5), (19.2, 20.5), (19.2, 15), (12.8, 15), (12.8, 20.5), (9.4, 20.5)], p.light)
    p.rect(12.8, 12.6, 19.2, 21, p.light)
    p.rect(13.2, 21, 15.8, 24, p.c)
    p.rect(16.2, 21, 18.8, 24, p.c)
    p.rrect(5, 26, 27, 29.5, 1.5, (10, 16, 36))
    p.rrect(5.6, 26.6, 21, 28.9, 1.0, GREEN)


def i_weapons(p):
    # a bow on the right, a blade across its string on the other diagonal
    p.arc(7, 16, 17, -62, 62, 3.2, p.dark)
    p.arc(7, 16, 17, -62, 62, 1.9, p.c)
    p.line((15, 2.0), (15, 30.0), 1.0, WHITE)
    p.poly([(3.2, 5.6), (5.6, 3.2), (19.6, 18.6), (17.2, 21.0)], WHITE)
    p.poly([(3.2, 5.6), (5.6, 3.2), (4.6, 3.9)], p.light)
    p.line((6.2, 6.0), (18.0, 19.6), 0.8, p.light)
    p.line((14.4, 15.4), (22.2, 23.2), 2.6, GOLD)
    p.line((19.4, 20.4), (24.8, 26.0), 2.4, p.dark)
    p.circ(25.6, 26.8, 2.0, GOLD)


def i_skills(p):
    # a skill tree: a star on top, three runes below joined by lines
    for q in ((7, 23), (16, 25), (25, 23)):
        p.line((16, 12), q, 1.3, p.dark)
    p.star(16, 10, 8, 3.6, 5, p.c)
    p.star(16, 10, 4.6, 2.0, 5, p.light)
    for q in ((7, 23), (16, 25), (25, 23)):
        p.poly([(q[0], q[1] - 3.5), (q[0] + 3.2, q[1]), (q[0], q[1] + 3.5), (q[0] - 3.2, q[1])], p.c)
        p.circ(q[0], q[1], 1.0, WHITE)


def i_quests(p):
    # a scroll with a gold exclamation mark
    p.rrect(7, 4.5, 25, 27.5, 1.5, mix(p.c, WHITE, 0.82))
    p.rrect(5, 3, 27, 8, 2.4, p.c)
    p.rrect(5, 24, 27, 29, 2.4, p.c)
    p.line((7, 8), (25, 8), 0.8, p.dark)
    p.rrect(14.2, 10, 17.8, 19.8, 1.2, GOLD)
    p.circ(16, 22.4, 1.9, GOLD)
    p.line((9.5, 12), (12, 12), 0.9, p.light)
    p.line((20, 12), (22.5, 12), 0.9, p.light)


def i_attunement(p):
    # a meditating figure over a lotus
    for deg in (-70, -35, 0, 35, 70):
        a = math.radians(deg)
        cx, cy = 16 + 9.5 * math.sin(a), 24 - 5.5 * math.cos(a)
        p.ellipse_rot(cx, cy, 2.2, 5.2, deg, p.c if abs(deg) < 40 else p.dark)
    p.ellipse_rot(16, 21.5, 2.6, 6.5, 0, p.light)
    p.circ(16, 7.5, 3.0, WHITE)
    p.poly([(11, 17), (14, 11.5), (18, 11.5), (21, 17), (16, 16)], p.light)
    p.line((11, 17), (21, 17), 1.6, WHITE)
    p.ring(16, 12.5, 11, 0.8, p.dark)
    p.circ(16, 7.5, 1.0, p.c)


def i_multiverse(p):
    # a ringed world with two moons
    p.ring_rot(15, 17, 13.5, 4.6, -24, 1.6, p.dark)
    p.circ(15, 17, 8.2, p.c)
    p.circ(12.8, 14.6, 4.4, p.light)
    p.arc(15, 17, 5.5, 200, 330, 1.0, p.dark)
    # the front half of the ring crosses the planet
    a = math.radians(-24)
    pts = []
    for i in range(0, 31):
        t = math.pi * i / 30.0
        x, y = 13.5 * math.cos(t), 4.6 * math.sin(t)
        pts.append((15 + x * math.cos(a) - y * math.sin(a), 17 + x * math.sin(a) + y * math.cos(a)))
    for i in range(30):
        p.line(pts[i], pts[i + 1], 1.6, p.light)
    p.circ(26, 6.5, 2.6, GOLD)
    p.circ(5.5, 26.5, 1.6, WHITE)


def i_codex(p):
    # an open book with a ribbon
    p.poly([(16, 9), (5, 6), (5, 25), (16, 28)], p.light)
    p.poly([(16, 9), (27, 6), (27, 25), (16, 28)], mix(p.c, WHITE, 0.3))
    p.poly([(16, 9), (5, 6), (5, 8), (16, 11.2)], p.c)
    p.poly([(16, 9), (27, 6), (27, 8), (16, 11.2)], p.c)
    p.line((16, 10), (16, 28), 1.2, p.dark)
    for k in range(4):
        y = 12.5 + k * 3.1
        p.line((7.5, y - 1.2 + 0.0), (14, y + 0.9), 0.8, p.dark)
        p.line((18, y + 0.9), (24.5, y - 1.2), 0.8, p.dark)
    p.poly([(21, 5), (24, 4.4), (24, 12), (22.5, 10.6), (21, 12.4)], GOLD)


def i_settings(p):
    # a cog with eight teeth
    teeth = 8
    pts = []
    for i in range(teeth):
        base = -90 + i * 360.0 / teeth
        for da, r in ((-14, 10.2), (-9, 14.5), (9, 14.5), (14, 10.2)):
            a = math.radians(base + da)
            pts.append((16 + r * math.cos(a), 16 + r * math.sin(a)))
    p.poly(pts, p.c)
    p.circ(16, 16, 10.4, p.c)
    p.ring(16, 16, 9.6, 1.2, p.light)
    p.circ(16, 16, 4.4, (10, 16, 36))
    p.ring(16, 16, 4.4, 1.2, p.light)


# ---- Codex pages
def i_overview(p):
    # a framed portrait: head and shoulders
    p.rrect(5, 4, 27, 28, 3, p.dark)
    p.rrect(7, 6, 25, 26, 2, mix(p.c, (0, 0, 0), 0.7))
    p.circ(16, 13, 4.4, p.light)
    p.poly([(7.5, 26), (9, 20.5), (13, 18.5), (19, 18.5), (23, 20.5), (24.5, 26)], p.c)
    p.star(24.5, 6, 3.4, 1.4, 4, GOLD)


def i_status_page(p):
    # a heart with a pulse line
    p.circ(11.5, 11, 6.2, RED)
    p.circ(20.5, 11, 6.2, RED)
    p.poly([(5.6, 13), (26.4, 13), (16, 27)], RED)
    p.circ(10, 9.5, 1.8, mix(RED, WHITE, 0.55))
    pts = [(3, 17), (10, 17), (12.5, 11.5), (16, 24), (19, 15), (21, 17), (29, 17)]
    for i in range(len(pts) - 1):
        p.line(pts[i], pts[i + 1], 2.6, (10, 16, 36))
    for i in range(len(pts) - 1):
        p.line(pts[i], pts[i + 1], 1.4, WHITE)


def i_weapon_sword(p):
    p.poly([(25.5, 3.5), (28.5, 6.5), (13, 22.5), (9.5, 19)], WHITE)
    p.poly([(25.5, 3.5), (28.5, 6.5), (27.2, 5.2)], p.light)
    p.line((8, 16), (16, 24), 2.6, GOLD)
    p.line((9.5, 22.5), (5.5, 26.5), 2.6, p.dark)
    p.circ(5, 27, 2.0, GOLD)
    p.line((14, 18), (24, 7.5), 0.8, p.light)


def i_relics(p):
    # a ring with a big gem
    p.ring(16, 19.5, 8.2, 2.8, GOLD)
    p.poly([(16, 3.5), (22, 8.5), (16, 15.5), (10, 8.5)], p.c)
    p.poly([(16, 3.5), (22, 8.5), (16, 8.5)], p.light)
    p.poly([(10, 8.5), (16, 8.5), (16, 15.5)], p.dark)
    p.line((12.5, 6.2), (15, 5.4), 0.9, WHITE)
    p.circ(5.5, 6, 1.2, WHITE)
    p.circ(27, 12, 1.0, WHITE)


def i_bestiary(p):
    # a horned beast head with fangs
    p.poly([(7, 13), (4, 3.5), (12, 8)], p.light)
    p.poly([(25, 13), (28, 3.5), (20, 8)], p.light)
    p.poly([(5.5, 13), (16, 8), (26.5, 13), (25, 22), (16, 28.5), (7, 22)], p.c)
    p.poly([(9, 14.5), (14, 17), (9.5, 18.5)], WHITE)
    p.poly([(23, 14.5), (18, 17), (22.5, 18.5)], WHITE)
    p.poly([(11, 23), (13.5, 21), (13.5, 26)], WHITE)
    p.poly([(21, 23), (18.5, 21), (18.5, 26)], WHITE)
    p.circ(14.2, 22, 0.9, p.dark)
    p.circ(17.8, 22, 0.9, p.dark)


def i_spirit(p):
    # a spirit flame with an eye
    p.poly([(16, 2.5), (22.5, 11), (25.5, 19), (22, 26.5), (16, 29), (10, 26.5), (6.5, 19), (9.5, 12), (12.5, 14.5)], p.c)
    p.poly([(16, 11), (20.5, 17.5), (21, 23), (16, 27), (11, 23), (11.5, 17.5)], p.light)
    p.circ(16, 20, 3.2, WHITE)
    p.circ(16, 20.4, 1.7, p.dark)
    p.circ(27, 6, 1.2, p.light)
    p.circ(5, 8, 1.0, p.light)


def i_guide(p):
    # a speech bubble with a question mark
    p.rrect(4, 4, 28, 23, 4, p.c)
    p.poly([(9, 22), (9, 29), (16, 22)], p.c)
    p.arc(16, 12, 4.6, 180, 380, 2.6, WHITE)
    p.line((18.6, 15.5), (16, 17.5), 2.6, WHITE)
    p.line((16, 17.5), (16, 18.2), 2.6, WHITE)
    p.circ(16, 20.4, 1.5, WHITE)


def i_spells(p):
    # a wand with sparkles
    p.line((6, 27), (20, 13), 3.4, p.dark)
    p.line((6, 27), (19, 14), 1.8, GOLD)
    p.star(22.5, 9.5, 7.2, 2.6, 4, WHITE)
    p.star(22.5, 9.5, 4.4, 1.6, 4, GOLD)
    p.star(9, 10, 3.6, 1.2, 4, p.light)
    p.star(27, 22, 3.2, 1.1, 4, p.light)
    p.circ(13, 4.5, 1.0, WHITE)
    p.circ(5, 19, 1.0, WHITE)


def i_blocks(p):
    # an isometric cube
    p.poly([(16, 3.5), (27.5, 9.5), (16, 15.5), (4.5, 9.5)], p.light)
    p.poly([(4.5, 9.5), (16, 15.5), (16, 28.5), (4.5, 22.5)], p.c)
    p.poly([(27.5, 9.5), (16, 15.5), (16, 28.5), (27.5, 22.5)], p.dark)
    p.line((16, 15.5), (16, 28.5), 0.8, mix(p.c, WHITE, 0.4))
    p.line((16, 3.5), (27.5, 9.5), 0.8, WHITE)
    p.rect(8, 14, 11, 16.5, mix(p.c, (0, 0, 0), 0.3))
    p.rect(20.5, 17, 23.5, 19.5, mix(p.dark, (0, 0, 0), 0.3))


def i_entities(p):
    # a blocky mob head with a grim face
    p.rect(6, 5, 26, 25, p.c)
    p.rect(6, 5, 26, 8, p.light)
    p.rect(9, 11, 14, 16, (10, 16, 36))
    p.rect(18, 11, 23, 16, (10, 16, 36))
    p.rect(13, 16, 19, 20, (10, 16, 36))
    p.rect(11, 20, 14, 24, (10, 16, 36))
    p.rect(18, 20, 21, 24, (10, 16, 36))
    p.rect(10, 12, 12, 14, WHITE)
    p.rect(19, 12, 21, 14, WHITE)
    p.rect(8, 26, 24, 29, p.dark)


def i_magic(p):
    # a magic circle: rings and a hexagram
    p.ring(16, 16, 13.5, 1.6, p.c)
    p.ring(16, 16, 10.2, 1.0, p.light)
    for off in (0, 180):
        pts = []
        for i in range(3):
            a = math.radians(-90 + off + i * 120)
            pts.append((16 + 10 * math.cos(a), 16 + 10 * math.sin(a)))
        for i in range(3):
            p.line(pts[i], pts[(i + 1) % 3], 1.6, p.c if off == 0 else p.light)
    p.circ(16, 16, 2.4, GOLD)
    for i in range(6):
        a = math.radians(-90 + i * 60)
        p.circ(16 + 13.5 * math.cos(a), 16 + 13.5 * math.sin(a), 1.4, WHITE)


def i_effects(p):
    # a potion flask with bubbles
    p.poly([(13, 4), (19, 4), (19, 12), (26, 25), (24.5, 28.5), (7.5, 28.5), (6, 25), (13, 12)], mix(p.c, WHITE, 0.55))
    p.poly([(9.2, 19), (22.8, 19), (26, 25), (24.5, 28.5), (7.5, 28.5), (6, 25)], p.c)
    p.rrect(11.5, 2.5, 20.5, 6, 1.5, GOLD)
    p.circ(13, 23.5, 1.6, WHITE)
    p.circ(19.5, 24.5, 1.1, WHITE)
    p.circ(16.5, 21, 0.9, p.light)
    p.line((15, 7), (15, 12.5), 0.8, WHITE)
    p.star(25.5, 8, 3, 1.1, 4, p.light)


def i_beastwill(p):
    # a paw print
    p.ellipse_rot(16, 21, 7.6, 6.2, 0, p.c)
    p.ellipse_rot(8, 12.5, 2.6, 3.6, -22, p.c)
    p.ellipse_rot(13.2, 8.2, 2.6, 3.8, -6, p.c)
    p.ellipse_rot(18.8, 8.2, 2.6, 3.8, 6, p.c)
    p.ellipse_rot(24, 12.5, 2.6, 3.6, 22, p.c)
    p.ellipse_rot(16, 22, 4.4, 3.4, 0, p.light)
    p.line((24.5, 4), (28.5, 8.5), 1.0, GOLD)
    p.line((26.5, 3), (29.5, 6.5), 1.0, GOLD)


def i_weapon_page(p):
    # a shield with a sword
    p.poly([(16, 3), (26.5, 6.5), (26, 17), (16, 29.5), (6, 17), (5.5, 6.5)], p.dark)
    p.poly([(16, 5), (24.5, 8), (24, 16.5), (16, 27), (8, 16.5), (7.5, 8)], p.c)
    p.poly([(16, 5), (24.5, 8), (24, 16.5), (16, 16.5)], mix(p.c, WHITE, 0.25))
    p.poly([(14.6, 8), (17.4, 8), (17.4, 19.5), (16, 22), (14.6, 19.5)], WHITE)
    p.line((11.5, 13.2), (20.5, 13.2), 1.6, GOLD)


def i_rules(p):
    # scales of justice
    p.line((16, 5), (16, 26), 1.8, p.c)
    p.rrect(9.5, 25.5, 22.5, 28.5, 1.2, p.c)
    p.line((6, 9), (26, 9), 1.8, p.light)
    p.circ(16, 5.5, 2.0, GOLD)
    for cx in (6, 26):
        p.line((cx, 9), (cx - 3.4, 18), 0.8, p.light)
        p.line((cx, 9), (cx + 3.4, 18), 0.8, p.light)
        p.poly([(cx - 4.4, 18), (cx + 4.4, 18), (cx + 3, 21.4), (cx - 3, 21.4)], p.c)


def i_wiki(p):
    # a globe with meridians
    p.circ(16, 16, 12, mix(p.c, (0, 0, 0), 0.35))
    p.circ(16, 16, 10.8, p.c)
    p.poly([(9, 10), (14, 7.5), (16.5, 11), (14, 14), (10, 15)], GREEN)
    p.poly([(18, 15), (24, 13), (23, 20), (19, 23.5), (17, 20)], GREEN)
    p.ring(16, 16, 10.8, 0.8, p.light)
    p.ring_rot(16, 16, 5.2, 10.8, 0, 0.8, p.light)
    p.line((5.2, 16), (26.8, 16), 0.8, p.light)
    p.circ(11, 9, 1.8, mix(p.c, WHITE, 0.7))


def i_data(p):
    # a page with a folded corner and lines
    p.poly([(7, 3), (20, 3), (26, 9), (26, 29), (7, 29)], p.c)
    p.poly([(20, 3), (20, 9), (26, 9)], p.light)
    for k in range(4):
        p.line((10.5, 14 + k * 3.6), (22.5, 14 + k * 3.6), 1.1, WHITE if k % 2 == 0 else p.light)
    p.line((10.5, 8), (16, 8), 1.1, GOLD)


# ---- Codex categories
def i_cat_you(p):
    p.circ(16, 10, 5.2, p.light)
    p.poly([(5.5, 28), (7.5, 20), (12.5, 16.5), (19.5, 16.5), (24.5, 20), (26.5, 28)], p.c)
    p.star(16, 22, 3.2, 1.3, 4, GOLD)


def i_cat_collection(p):
    # a treasure chest
    p.rrect(4, 14, 28, 28, 2, p.dark)
    p.rrect(5.4, 15.4, 26.6, 26.6, 1.2, p.c)
    p.poly([(4, 14), (6, 6.5), (26, 6.5), (28, 14)], p.light)
    p.poly([(4, 14), (28, 14), (28, 16.5), (4, 16.5)], p.dark)
    p.rrect(13.2, 14, 18.8, 21.5, 1.2, GOLD)
    p.circ(16, 18, 1.1, p.dark)
    p.star(25, 5, 3.4, 1.2, 4, WHITE)


def i_cat_magic(p):
    p.star(16, 16, 14, 4.4, 4, p.c)
    p.star(16, 16, 8.6, 3.0, 4, p.light)
    p.circ(16, 16, 2.4, WHITE)
    p.star(26, 6, 3.2, 1.1, 4, GOLD)
    p.star(6, 26, 2.8, 1.0, 4, GOLD)


def i_cat_world(p):
    # mountains under a sun
    p.circ(22.5, 9, 4.6, GOLD)
    p.poly([(2, 27), (11.5, 11), (21, 27)], p.dark)
    p.poly([(11, 27), (21, 13), (30, 27)], p.c)
    p.poly([(11.5, 11), (14.6, 16.2), (12.5, 15), (11.5, 17), (9.7, 14.8), (8.4, 16.2)], WHITE)
    p.poly([(21, 13), (23.8, 17.5), (22.2, 16.6), (21, 18.4), (19.7, 16.5), (18.2, 17.5)], WHITE)
    p.rect(2, 27, 30, 29, p.light)


def i_cat_reference(p):
    # a stack of books
    p.rrect(4, 20, 28, 28, 1.6, p.dark)
    p.rrect(5, 21, 27, 24, 1, p.light)
    p.rrect(6, 12, 26, 20, 1.6, p.c)
    p.rrect(7, 13, 25, 16, 1, WHITE)
    p.rrect(9, 4, 23, 12, 1.6, mix(p.c, WHITE, 0.3))
    p.rect(14, 5, 18, 11, GOLD)
    p.line((8, 26), (24, 26), 0.8, p.c)


# id -> (function, rgb)
CAT_COLOR = {"you": 0x5CD8FF, "collection": 0xE8C468, "magic": 0x9A7CFF, "world": 0x7CF0B0, "reference": 0x8FD0FF}
ICONS = {
    # the eight hub nodes, in HubNodes.Tab order, with HubNodes.Tab.rgb
    "node_status": (i_status, 0x5CD8FF), "node_weapons": (i_weapons, 0xE8C468), "node_skills": (i_skills, 0x9A7CFF),
    "node_quests": (i_quests, 0x7CF0B0), "node_attunement": (i_attunement, 0xB39CF0), "node_multiverse": (i_multiverse, 0xFF9F6A),
    "node_codex": (i_codex, 0x8FD0FF), "node_settings": (i_settings, 0xC0C8D8),
    # the Codex pages, coloured by their category (HubCodexNav)
    "codex_overview": (i_overview, CAT_COLOR["you"]), "codex_status": (i_status_page, CAT_COLOR["you"]), "codex_weapon": (i_weapon_page, CAT_COLOR["you"]),
    "codex_beastwill": (i_beastwill, CAT_COLOR["you"]),
    "codex_weapons": (i_weapon_sword, CAT_COLOR["collection"]), "codex_relics": (i_relics, CAT_COLOR["collection"]),
    "codex_bestiary": (i_bestiary, CAT_COLOR["collection"]), "codex_spirit_lords": (i_spirit, CAT_COLOR["collection"]),
    "codex_spells": (i_spells, CAT_COLOR["magic"]), "codex_magic": (i_magic, CAT_COLOR["magic"]), "codex_effects": (i_effects, CAT_COLOR["magic"]),
    "codex_blocks": (i_blocks, CAT_COLOR["world"]), "codex_entities": (i_entities, CAT_COLOR["world"]),
    "codex_guide": (i_guide, CAT_COLOR["reference"]), "codex_rules": (i_rules, CAT_COLOR["reference"]), "codex_wiki": (i_wiki, CAT_COLOR["reference"]),
    "codex_data": (i_data, CAT_COLOR["reference"]),
    # the Codex categories
    "cat_you": (i_cat_you, CAT_COLOR["you"]), "cat_collection": (i_cat_collection, CAT_COLOR["collection"]), "cat_magic": (i_cat_magic, CAT_COLOR["magic"]),
    "cat_world": (i_cat_world, CAT_COLOR["world"]), "cat_reference": (i_cat_reference, CAT_COLOR["reference"]),
}


def render(fn, color, glow):
    p = Pen(color)
    fn(p)
    img = p.img
    alpha = img.getchannel("A")
    grow = alpha.filter(ImageFilter.MaxFilter(2 * K + 1))                      # one 32 px unit of dark outline
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    if glow:
        halo = alpha.filter(ImageFilter.MaxFilter(2 * K + 1)).filter(ImageFilter.GaussianBlur(2.2 * K))
        halo = halo.point(lambda v: min(255, int(v * 1.6)))
        col = Image.new("RGBA", img.size, rgb(color) + (0,))
        col.putalpha(halo.point(lambda v: int(v * 0.85)))
        out = Image.alpha_composite(out, col)
    ol = Image.new("RGBA", img.size, OUTLINE + (0,))
    ol.putalpha(grow.point(lambda v: int(v * 0.92)))
    out = Image.alpha_composite(out, ol)
    if glow:                                                                    # brighter fill for the hovered / open node
        lift = Image.new("RGBA", img.size, WHITE + (0,))
        lift.putalpha(alpha.point(lambda v: int(v * 0.22)))
        img = Image.alpha_composite(img, lift)
    out = Image.alpha_composite(out, img)
    return out.resize((N, N), Image.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet = None
    if "--sheet" in sys.argv:
        sheet = sys.argv[sys.argv.index("--sheet") + 1]
    made = []
    for name, (fn, color) in ICONS.items():
        for glow in (False, True):
            im = render(fn, color, glow)
            fname = name + ("_glow" if glow else "") + ".png"
            im.save(os.path.join(OUT, fname))
            with open(os.path.join(OUT, fname + ".mcmeta"), "w", newline="\n") as f:
                f.write('{\n  "texture": {\n    "blur": true\n  }\n}\n')
            made.append(fname)
    print("%d icons, %d files in %s" % (len(ICONS), len(made), os.path.abspath(OUT)))
    if sheet:
        make_sheet(sheet)


def make_sheet(path):
    names = list(ICONS)
    cols = 6
    cell_w, cell_h = 168, 112
    rows = (len(names) + cols - 1) // cols
    im = Image.new("RGBA", (cols * cell_w, rows * cell_h), (8, 12, 26, 255))
    d = ImageDraw.Draw(im)
    for i, name in enumerate(names):
        x0, y0 = (i % cols) * cell_w, (i // cols) * cell_h
        d.rectangle([x0 + 2, y0 + 2, x0 + cell_w - 3, y0 + cell_h - 3], outline=(30, 44, 80, 255))
        n_img = Image.open(os.path.join(OUT, name + ".png")).convert("RGBA")
        g_img = Image.open(os.path.join(OUT, name + "_glow.png")).convert("RGBA")
        im.alpha_composite(n_img.resize((64, 64), Image.NEAREST), (x0 + 8, y0 + 6))
        im.alpha_composite(g_img.resize((64, 64), Image.NEAREST), (x0 + 84, y0 + 6))
        im.alpha_composite(n_img.resize((24, 24), Image.BILINEAR), (x0 + 12, y0 + 76))
        im.alpha_composite(n_img.resize((16, 16), Image.BILINEAR), (x0 + 44, y0 + 80))
        im.alpha_composite(g_img.resize((24, 24), Image.BILINEAR), (x0 + 88, y0 + 76))
        d.text((x0 + 8, y0 + 100), name, fill=(190, 210, 235, 255))
    im.convert("RGB").save(path)
    print("sheet", path)


if __name__ == "__main__":
    main()
