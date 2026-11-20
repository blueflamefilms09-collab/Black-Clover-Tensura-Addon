"""Icon forge v3: the six root icons that were painted by one-off scripts outside the main repaint (tools/gen_sin_skill_icons.py: the five Berserk weapon skills;
the Saiyan Prodigy). Each gets a symbol of the same kind as the other root icons (a Ctx scene in 0..100 units) so it goes through the same pixel painter.
They are NOT in the V3 manifest / index (the manifest is pinned at 121 by SkillIconsV3Test); forge.py writes them as plain stills."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "skills"))
from icon3_lib import blur, circle, line, poly, ring, star, taper  # noqa: E402
from icon3_symbols import flame, pal, M, orb               # noqa: E402

MISC = []


def misc(id_, cls, palette):
    def deco(fn):
        MISC.append(dict(id=id_, cls=cls, pal=palette, fn=fn, anim=False))
        return fn
    return deco


@misc("lord_of_slothful_weight", "ultimate", pal((150, 120, 210), (70, 60, 110), (200, 180, 255), (140, 110, 230), (50, 36, 90), (8, 6, 20)))
def _(c):
    # a heavy iron kettle-weight on a chain, drowsy "z" above it
    c.part(ring(50, 20, 9, 3.0), (170, 170, 190), bevel=1.6, spec=0.6)
    c.part(line([(50, 24), (50, 40)], 3.0, False), (150, 150, 170), bevel=1.4)
    c.part(poly([(34, 46), (66, 46), (74, 62), (80, 86), (20, 86), (26, 62)], round_=2.6), (96, 92, 120), bevel=4.4, spec=0.45)
    c.part(poly([(30, 44), (70, 44), (70, 52), (30, 52)], round_=1.6), (130, 126, 156), bevel=2.0, spec=0.5)
    c.flat(poly([(40, 66), (60, 66), (60, 70), (46, 78), (60, 78), (60, 82), (40, 82), (40, 78), (54, 70), (40, 70)], round_=0.3), (200, 180, 255), 0.9)
    for (x, y, s) in ((70, 20, 7), (80, 10, 5)):
        c.flat(poly([(x - s, y - s), (x + s, y - s), (x - s, y + s), (x + s, y + s)], round_=0.2), (210, 190, 255), 0.95, outline=False)


@misc("lord_of_green_eyed_envy", "ultimate", pal((60, 220, 110), (20, 120, 60), (200, 255, 210), (50, 230, 110), (14, 70, 40), (4, 12, 8)))
def _(c):
    almond = poly([(8, 52), (26, 32), (50, 24), (74, 32), (92, 52), (74, 72), (50, 80), (26, 72)], round_=3.0)
    c.part(almond, (40, 150, 80), bevel=4.0, spec=0.3)
    c.part(circle(50, 52, 22), (210, 255, 200), bevel=5.0, spec=0.5)
    c.flat(circle(50, 52, 15), (30, 200, 90), 1.0)
    c.flat(poly([(46, 38), (54, 38), (56, 52), (54, 66), (46, 66), (44, 52)], round_=1.6), (6, 20, 12), 1.0)
    c.add(blur(circle(50, 52, 17), 3), (80, 255, 130), 0.5)
    c.flat(circle(43, 45, 3.2), (255, 255, 255), 0.9)
    c.part(taper((62, 76), (66, 92), 6, 2, curve=2), (120, 255, 160), bevel=2.0, spec=0.6)


@misc("lord_of_empty_vanity", "ultimate", pal((255, 120, 210), (255, 220, 240), (255, 255, 255), (255, 110, 210), (100, 36, 90), (14, 6, 20)))
def _(c):
    c.part(taper((34, 92), (48, 62), 7, 5), (230, 180, 90), bevel=2.0, spec=0.6)
    c.part(ring(56, 36, 28, 7.0), (240, 190, 90), bevel=3.0, spec=0.6)
    c.part(circle(56, 36, 22), (255, 170, 230), bevel=6.0, spec=0.7)
    c.flat(poly([(46, 18), (54, 14), (62, 20), (56, 26)], round_=0.4), (255, 255, 255), 0.8)
    c.flat(line([(48, 52), (60, 28), (68, 44), (74, 22)], 1.4, False), (120, 40, 100), 0.85)
    c.dot(76, 16, 2.4, (255, 255, 255), 0.95)


@misc("lord_of_the_twin_rings", "ultimate", pal((255, 90, 90), (200, 210, 235), (255, 230, 230), (255, 90, 100), (100, 24, 40), (14, 6, 10)))
def _(c):
    c.part(ring(36, 54, 27, 8.0), (230, 70, 70), bevel=3.0, spec=0.6)
    c.part(ring(64, 54, 27, 8.0), (206, 214, 236), bevel=3.0, spec=0.6)
    c.part(ring(36, 54, 27, 8.0, 300, 60), (230, 70, 70), bevel=3.0, spec=0.6, outline=False)
    c.add(blur(ring(50, 54, 6, 2.0), 3), (255, 255, 255), 0.6)
    for a in (30, 150, 210, 330):
        x, y = 50 + 36 * math.cos(math.radians(a)), 54 + 36 * math.sin(math.radians(a))
        c.part(poly([(x, y - 6), (x + 2, y - 2), (x + 6, y), (x + 2, y + 2), (x, y + 6), (x - 2, y + 2), (x - 6, y), (x - 2, y - 2)], round_=0.2), (255, 240, 220), bevel=1.0, outline=False)


@misc("seifort_holy_judgement", "ultimate", pal((255, 244, 200), (255, 210, 110), (255, 255, 255), (255, 240, 180), (120, 100, 50), (14, 12, 20)))
def _(c):
    for i in range(12):
        a = math.radians(-90 + i * 30)
        c.flat(taper((50 + 14 * math.cos(a), 46 + 14 * math.sin(a)), (50 + 46 * math.cos(a), 46 + 46 * math.sin(a)), 3.4, 0.4), (255, 240, 170), 0.5)
    c.part(poly([(44, 14), (50, 4), (56, 14), (56, 62), (44, 62)], round_=1.0), (236, 240, 250), bevel=2.6, spec=0.7)
    c.part(poly([(24, 60), (76, 60), (72, 69), (28, 69)], round_=1.4), (246, 200, 80), bevel=2.2, spec=0.6)
    c.part(poly([(46, 69), (54, 69), (54, 88), (46, 88)], round_=0.8), (170, 100, 60), bevel=1.6)
    c.part(circle(50, 92, 5), (246, 200, 80), bevel=2.0, spec=0.6)
    c.add(blur(poly([(48, 16), (52, 16), (52, 58), (48, 58)], round_=0.4), 3), (255, 255, 255), 0.8)


@misc("saiyan_prodigy", "ultimate", pal((255, 214, 70), (255, 150, 40), (255, 255, 200), (255, 220, 80), (110, 70, 14), (16, 8, 20)))
def _(c):
    # a Saiyan bust: golden flame-spiked hair over a calm face, blue-white aura sparks
    c.part(poly([(10, 102), (18, 82), (36, 74), (64, 74), (82, 82), (90, 102)], round_=3.0), (240, 120, 30), bevel=4.0, spec=0.2)
    c.part(circle(50, 56, 21, 23), (250, 208, 164), bevel=4.6, spec=0.3)
    hair = [(50, 38), (44, 6), (54, 20), (58, 0), (64, 20), (74, 6), (74, 32), (86, 26), (80, 56), (72, 46)]
    c.part(poly(hair, mirror=True, round_=0.6), (255, 220, 70), bevel=3.0, spec=0.5, grad=((255, 236, 120), (255, 160, 30)))
    for sx in (-1, 1):
        c.flat(circle(50 + sx * 9, 60, 2.6, 3.2), (255, 255, 255), 0.95)
        c.flat(circle(50 + sx * 9, 60.4, 1.7, 2.6), (30, 150, 130), 1.0)
        c.flat(line([(50 + sx * 14, 52), (50 + sx * 5, 55)], 1.8, False), (150, 110, 20), 0.95)
    c.flat(line([(44, 70), (56, 70)], 1.3, False), (150, 70, 60), 0.9)
    for (x, y) in ((14, 34), (88, 44)):
        c.part(star(x, y, 6, 1.6, 4), (170, 235, 255), bevel=1.0, outline=False)
