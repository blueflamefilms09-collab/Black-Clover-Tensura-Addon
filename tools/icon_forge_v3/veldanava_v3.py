"""Icon forge v3: the five Veldanava Ultimates (Hercules, Israfil, Jibril, Sandalphon, Solomon) as native 32 px pixel-art medallions, 32 frames.

Their ornate 128 px strips (<id>_anim_hd.png, drawn on the Will of the Creator card screen) are NOT touched. What changes is the 32 px picture the skill lists draw
(<id>.png and <id>_anim.png): it used to be the 128 px art box-filtered down (about 600 colours of mush; the ring and the emblem blurred together), now it is a
symbol redrawn for 32 px: a bold emblem, a 1 px outline, 4 flat tones, a lit gold ring, and the same loop technique as the other animated icons (glint across the
symbol, twinkles, pulse). 32 frames at 2 ticks like before (AnimatedIconsTest pins 32 frames and a frame time of 2). The pre-redesign pictures stay under
textures/skill_old/veldanava/."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "skills"))
from icon3_lib import blur, circle, line, poly, ring, star, taper  # noqa: E402
from icon3_symbols import pal, wing, M                               # noqa: E402
from icon3_parts import hexagram                                     # noqa: E402

VELD = []
GOLD = dict(outer=(30, 14, 4), light=(255, 232, 130), mid=(226, 160, 44), dark=(150, 84, 14), inner=(40, 20, 6), corner=(255, 250, 210))


def veld(id_, palette):
    def deco(fn):
        VELD.append(dict(id=id_, pal=palette, fn=fn))
        return fn
    return deco


@veld("hercules", pal((246, 190, 70), (255, 236, 150), (255, 90, 50), (255, 110, 50), (150, 40, 24), (30, 6, 10)))
def _(c):
    # the golden gauntlet fist inside a laurel wreath
    c.part(ring(50, 58, 38, 4.4, 95, 265), (110, 160, 64), bevel=1.6, spec=0.2)
    c.part(ring(50, 58, 38, 4.4, 275, 85), (110, 160, 64), bevel=1.6, spec=0.2)
    c.part(poly([(28, 46), (72, 46), (77, 62), (68, 78), (32, 78), (23, 62)], round_=3.0), (232, 168, 54), bevel=4.6, spec=0.5)
    for x in (33, 44, 56, 67):
        c.part(circle(x, 44, 7.2, 8.0), (255, 208, 90), bevel=3.0, spec=0.7)
    c.part(poly([(16, 56), (30, 52), (32, 70), (20, 74)], round_=2.0), (240, 180, 60), bevel=2.4, spec=0.5)
    c.part(poly([(32, 78), (68, 78), (66, 92), (34, 92)], round_=1.6), (200, 130, 40), bevel=2.0, spec=0.3)
    c.flat(line([(30, 62), (70, 62)], 1.8, False), (150, 84, 20), 0.85)
    c.dot(50, 85, 2.2, (255, 70, 60), 1.0, glow=False)


@veld("israfil", pal((246, 200, 80), (255, 240, 170), (120, 220, 255), (70, 180, 255), (20, 80, 160), (4, 12, 40)))
def _(c):
    # a golden trumpet seen from the side: mouthpiece left, three valves on top, the bell opening to the right
    c.part(ring(34, 68, 15, 3.6, 10, 175), (226, 164, 50), bevel=1.8, spec=0.6)
    c.part(taper((10, 56), (60, 56), 6.0, 7.2), (246, 200, 80), bevel=2.6, spec=0.7)
    c.part(circle(8, 56, 4.4), (255, 224, 120), bevel=1.6, spec=0.7)
    c.part(poly([(58, 51), (90, 30), (90, 82), (58, 62)], round_=1.6), (255, 214, 96), bevel=3.8, spec=0.7)
    c.part(circle(90, 56, 5.0, 26.0), (255, 240, 170), bevel=2.0, spec=0.7)
    for x in (26, 36, 46):
        c.part(poly([(x - 2.4, 36), (x + 2.4, 36), (x + 2.4, 52), (x - 2.4, 52)], round_=0.8), (255, 224, 120), bevel=1.4, spec=0.6)
        c.dot(x, 33, 2.2, (150, 235, 255), 1.0, glow=False)
    c.flat(line([(64, 54), (82, 44)], 1.6, False), (255, 255, 255), 0.7)
    for (x, y) in ((70, 18), (84, 12)):
        c.part(star(x, y, 6, 1.6, 4), (170, 235, 255), bevel=1.0, outline=False)


@veld("jibril", pal((250, 246, 255), (246, 200, 90), (255, 220, 240), (255, 200, 240), (150, 120, 200), (40, 30, 90)))
def _(c):
    # the balance of the dawn: white wings, a halo, golden scales
    wg = [(56, 52), (70, 30), (90, 18), (88, 34), (98, 42), (88, 54), (94, 68), (76, 66), (60, 62)]
    M(c, wg, (250, 248, 255), bevel=3.6, spec=0.3)
    c.part(np_ring_ellipse(50, 18, 18, 6, 14, 3.4), (255, 232, 130), bevel=1.6, spec=0.7)
    c.part(taper((50, 30), (50, 80), 4.0, 4.0), (246, 190, 70), bevel=1.6, spec=0.7)
    c.part(poly([(34, 82), (66, 82), (62, 92), (38, 92)], round_=1.2), (226, 160, 50), bevel=1.6, spec=0.5)
    c.part(line([(20, 42), (80, 42)], 3.4, False), (255, 214, 96), bevel=1.4, spec=0.7)
    for x in (20, 80):
        c.part(poly([(x - 1, 43), (x + 1, 43), (x + 9, 58), (x - 9, 58)], round_=0.4), (226, 170, 60), bevel=0.8, outline=False)
        c.part(poly([(x - 13, 57), (x + 13, 57), (x + 9, 67), (x - 9, 67)], round_=1.6), (255, 220, 110), bevel=2.0, spec=0.7)
    c.dot(50, 42, 3.2, (255, 245, 200), 1.0, glow=False)


def np_ring_ellipse(cx, cy, rx, ry, ix, iy):
    import numpy as np
    return np.clip(circle(cx, cy, rx, ry) - circle(cx, cy, ix, iy), 0, 1)


@veld("sandalphon", pal((214, 224, 240), (246, 200, 80), (140, 190, 255), (110, 160, 255), (30, 60, 130), (4, 8, 30)))
def _(c):
    # the great sword hanging point-down between golden wing-guards
    for i in range(10):
        a = math.radians(i * 36)
        c.flat(taper((50 + 14 * math.cos(a), 58 + 14 * math.sin(a)), (50 + 46 * math.cos(a), 58 + 46 * math.sin(a)), 3.0, 0.4), (150, 190, 255), 0.35)
    c.part(poly([(43, 34), (57, 34), (57, 66), (50, 92), (43, 66)], round_=1.2), (222, 232, 248), bevel=3.0, spec=0.8)
    c.flat(line([(50, 36), (50, 80)], 1.4, False), (255, 255, 255), 0.75)
    M(c, [(50, 34), (78, 24), (88, 34), (74, 36), (84, 46), (64, 44), (50, 44)], (246, 196, 74), bevel=2.6, spec=0.6)
    c.part(poly([(47, 16), (53, 16), (53, 34), (47, 34)], round_=0.8), (120, 70, 44), bevel=1.4)
    c.part(circle(50, 12, 5.2), (246, 196, 74), bevel=2.0, spec=0.7)
    c.add(blur(poly([(48, 44), (52, 44), (52, 70), (50, 80), (48, 70)], round_=0.4), 3), (255, 255, 255), 0.8)


@veld("solomon", pal((246, 200, 70), (255, 240, 170), (130, 255, 235), (150, 110, 255), (90, 40, 150), (14, 6, 40)))
def _(c):
    # the seal of Solomon: a hexagram in two gold rings round a cyan light
    c.part(ring(50, 50, 42, 4.0), (230, 170, 56), bevel=1.8, spec=0.6)
    hexagram(c, 50, 50, 34, 4.6, (246, 200, 70))
    c.part(ring(50, 50, 13, 3.0), (255, 236, 150), bevel=1.4, spec=0.6)
    c.part(circle(50, 50, 8.5), (150, 255, 240), bevel=3.0, spec=0.7)
    c.add(blur(circle(50, 50, 10), 3.0), (255, 255, 255), 0.9)
    for a in range(0, 360, 60):
        x, y = 50 + 42 * math.cos(math.radians(a - 90)), 50 + 42 * math.sin(math.radians(a - 90))
        c.dot(x, y, 2.0, (255, 250, 200), 1.0, glow=False)
