"""Skill icons v3: the 15 Dragon Ball skills, each a Unique (db_<id>_unique) and an Ultimate (db_<id>, animated). One emblem per character in the series' look: the hair, the face, the outfit colours and the signature of the move set."""
import math

import numpy as np

from icon3_lib import blur, circle, line, poly, ring, star, taper
from icon3_parts import ICONS, R, blade_sword
from icon3_symbols import M, flame, orb, pal

SKIN = (250, 208, 164)
BLACK = (34, 30, 44)


def db(key, palette, fn):
    ICONS.append(dict(id="db_%s_unique" % key, cls="unique", pal=palette, fn=fn, anim=False))
    ICONS.append(dict(id="db_%s" % key, cls="ultimate", pal=palette, fn=fn, anim=True))


def bust(c, skin=SKIN, outfit=(240, 120, 30), outfit2=None, eyes=(20, 20, 30), brow=BLACK, mouth=(150, 70, 60), face=(21, 25), cy=56, ears=True):
    """The shoulders and the face (hair is drawn by the caller: back hair before, front hair after)."""
    c.part(poly([(10, 102), (18, 80), (36, 72), (64, 72), (82, 80), (90, 102)], round_=3.0), outfit, bevel=4.0, spec=0.2)
    if outfit2:
        c.part(poly([(40, 74), (60, 74), (50, 96)], round_=1.0), outfit2, bevel=1.8, outline=False)
    c.part(poly([(42, 66), (58, 66), (60, 76), (40, 76)], round_=1.0), darkskin(skin), bevel=1.6)
    if ears:
        for sx in (-1, 1):
            c.part(circle(50 + sx * face[0], cy + 2, 4.0, 5.0), skin, bevel=1.6)
    c.part(circle(50, cy, face[0], face[1]), skin, bevel=4.6, spec=0.3)


def darkskin(s):
    return tuple(int(v * 0.8) for v in s)


def face_details(c, eyes=(20, 20, 30), brow=BLACK, mouth=(150, 70, 60), cy=56, brow_tilt=1.0, eye_w=4.0, eye_h=5.0, dx=9.0):
    for sx in (-1, 1):
        c.flat(circle(50 + sx * dx, cy + 2, eye_w * 0.55, eye_h * 0.6), (255, 255, 255), 0.95)
        c.flat(circle(50 + sx * dx, cy + 2.4, eye_w * 0.34, eye_h * 0.5), eyes, 1.0)
        c.flat(line([(50 + sx * (dx + 5), cy - 5 - brow_tilt), (50 + sx * (dx - 4.5), cy - 3 + brow_tilt)], 1.7, False), brow, 0.95)
    c.flat(line([(44, cy + 15), (56, cy + 15)], 1.3, False), mouth, 0.9)
    c.flat(line([(50, cy + 5), (49, cy + 10)], 0.9, False), darkskin(SKIN), 0.7)


def spikes(c, col, pts, front=None, hl=None):
    c.part(poly(pts, mirror=True, round_=0.6), col, bevel=3.0, spec=0.5 if hl else 0.35)
    if front:
        c.part(poly(front, mirror=True, round_=0.5), col, bevel=2.2, spec=0.4)


GOKU_HAIR = [(50, 34), (44, 6), (54, 18), (58, 0), (64, 18), (74, 6), (74, 30), (86, 24), (80, 54), (72, 44)]


def goku(c):
    c.part(ring(50, 56, 42, 3.0), (200, 225, 255), bevel=1.4, spec=0.6, outline=False)
    spikes(c, (205, 214, 236), GOKU_HAIR)
    bust(c, outfit=(240, 120, 30), outfit2=(40, 90, 200))
    face_details(c, eyes=(170, 180, 200))
    c.part(poly([(50, 28), (66, 30), (74, 42), (60, 38), (50, 40)], round_=0.5), (205, 214, 236), bevel=2.0)
    c.part(poly([(50, 28), (34, 30), (26, 42), (40, 38), (50, 40)], round_=0.5), (205, 214, 236), bevel=2.0)


def vegeta(c):
    spikes(c, (36, 32, 52), [(50, 30), (56, 0), (64, 24), (76, 6), (78, 34), (88, 30), (82, 56), (72, 46)])
    bust(c, outfit=(54, 80, 190), outfit2=(240, 240, 250), ears=True)
    c.part(poly([(10, 102), (14, 84), (30, 76), (30, 92)], round_=1.5), (240, 240, 250), bevel=2.4, spec=0.4)
    c.part(poly([(90, 102), (86, 84), (70, 76), (70, 92)], round_=1.5), (240, 240, 250), bevel=2.4, spec=0.4)
    face_details(c, brow_tilt=-0.8, mouth=(120, 60, 60))
    c.part(poly([(50, 32), (66, 30), (74, 46), (66, 44), (58, 40), (50, 48)], round_=0.4), (36, 32, 52), bevel=1.6, outline=False)
    c.part(poly([(50, 32), (34, 30), (26, 46), (34, 44), (42, 40), (50, 48)], round_=0.4), (36, 32, 52), bevel=1.6, outline=False)
    c.part(star(50, 8, 5, 1.2, 4), (255, 220, 90), bevel=0.8, outline=False)


def gohan(c):
    spikes(c, (240, 242, 250), [(50, 30), (40, 2), (54, 16), (60, 0), (66, 18), (80, 8), (76, 34), (90, 30), (86, 58), (92, 86), (76, 66), (72, 44)])
    bust(c, outfit=(70, 50, 110), outfit2=(230, 230, 240))
    face_details(c, eyes=(230, 40, 50), brow_tilt=-0.9, mouth=(120, 50, 50))
    c.part(poly([(50, 28), (68, 30), (76, 48), (66, 44), (58, 38), (50, 44)], round_=0.4), (240, 242, 250), bevel=1.8, outline=False)
    c.part(poly([(50, 28), (32, 30), (24, 48), (34, 44), (42, 38), (50, 44)], round_=0.4), (240, 242, 250), bevel=1.8, outline=False)
    c.add(blur(circle(50, 58, 4), 3), (230, 60, 80), 0.0)


def piccolo(c):
    green = (112, 190, 86)
    for sx in (-1, 1):
        c.part(taper((50 + sx * 6, 36), (50 + sx * 10, 8), 4, 1.4, curve=sx * 2), green, bevel=1.4)
    c.part(poly([(8, 102), (14, 82), (34, 70), (66, 70), (86, 82), (92, 102)], round_=3.0), (250, 250, 255), bevel=4.0)
    c.part(poly([(34, 72), (66, 72), (62, 92), (38, 92)], round_=1.5), (120, 60, 160), bevel=2.0, outline=False)
    c.part(poly([(42, 66), (58, 66), (60, 76), (40, 76)], round_=1.0), (80, 150, 66), bevel=1.6)
    for sx in (-1, 1):
        c.part(poly([(50 + sx * 20, 52), (50 + sx * 38, 40), (50 + sx * 24, 62)], round_=0.5), green, bevel=1.6)
    c.part(circle(50, 56, 21, 25), green, bevel=4.6, spec=0.3)
    c.part(poly([(28, 34), (72, 34), (74, 46), (26, 46)], round_=1.5), (250, 250, 255), bevel=2.4)
    face_details(c, eyes=(30, 10, 10), brow=(40, 90, 40), brow_tilt=-1.0, mouth=(60, 90, 50))


def krillin(c):
    c.part(ring(50, 56, 42, 5.0), (255, 190, 80), bevel=2.4, spec=0.6, outline=False)
    bust(c, outfit=(240, 120, 30), outfit2=(40, 70, 170))
    face_details(c, eye_w=4.4, brow_tilt=0.4)
    for x, y in ((46, 36), (54, 36), (42, 42), (58, 42), (46, 44), (54, 44)):
        pass
    for x, y in ((44, 40), (50, 36), (56, 40), (44, 46), (50, 42), (56, 46)):
        c.flat(circle(x, y - 3, 1.5), (150, 70, 60), 1.0)
    c.flat(poly([(46, 48), (54, 48), (50, 52)], round_=0.3), darkskin(SKIN), 0.7)


def android18(c):
    hair = (250, 214, 90)
    c.part(poly([(24, 52), (28, 26), (50, 18), (72, 26), (76, 52), (78, 74), (64, 70), (36, 70), (22, 74)], round_=3.0), hair, bevel=3.4, spec=0.5)
    bust(c, outfit=(60, 80, 130), outfit2=(240, 240, 250), ears=False)
    c.part(poly([(30, 34), (50, 28), (70, 34), (72, 50), (60, 40), (50, 38), (40, 40), (28, 50)], round_=1.6), hair, bevel=2.4, spec=0.5)
    face_details(c, eyes=(70, 150, 230), brow=(170, 130, 50), brow_tilt=-0.2, mouth=(200, 90, 100))
    c.part(ring(36, 94, 7, 2.6), (255, 255, 255), bevel=1.2, outline=False)
    c.part(ring(64, 94, 7, 2.6), (255, 255, 255), bevel=1.2, outline=False)


def android17(c):
    hx = [(50 + 40 * math.cos(math.radians(60 * i - 90)), 52 + 40 * math.sin(math.radians(60 * i - 90))) for i in range(6)]
    c.part(np.clip(poly(hx, round_=1.0) - poly([(50 + 33 * math.cos(math.radians(60 * i - 90)), 52 + 33 * math.sin(math.radians(60 * i - 90))) for i in range(6)], round_=1.0), 0, 1), (120, 240, 170), bevel=2.0, spec=0.6, outline=False)
    hair = (30, 28, 40)
    c.part(poly([(24, 50), (28, 26), (50, 18), (72, 26), (76, 50), (80, 80), (64, 72), (36, 72), (20, 80)], round_=3.0), hair, bevel=3.4, spec=0.45)
    bust(c, outfit=(40, 40, 56), outfit2=None, ears=False)
    c.part(poly([(30, 76), (70, 76), (74, 90), (26, 90)], round_=2.0), (250, 160, 40), bevel=2.4)
    c.part(poly([(30, 32), (50, 26), (70, 32), (72, 48), (58, 40), (50, 38), (42, 40), (28, 48)], round_=1.6), hair, bevel=2.4, spec=0.45)
    face_details(c, eyes=(60, 150, 200), brow_tilt=-0.5, mouth=(150, 70, 60))


def goten(c):
    spikes(c, (36, 32, 48), [(50, 32), (44, 8), (54, 18), (60, 2), (66, 20), (76, 10), (74, 32), (84, 30), (80, 54), (72, 44)])
    bust(c, outfit=(240, 120, 30), outfit2=(40, 90, 200), face=(22, 24))
    face_details(c, eye_w=5.0, eye_h=5.6, dx=9.5, brow_tilt=0.6, mouth=(170, 70, 70))
    c.flat(poly([(46, 66), (54, 66), (50, 72)], round_=0.5), (180, 60, 60), 0.9)
    c.part(poly([(50, 30), (64, 32), (70, 42), (58, 38), (50, 40)], round_=0.4), (36, 32, 48), bevel=1.6, outline=False)
    c.part(poly([(50, 30), (36, 32), (30, 42), (42, 38), (50, 40)], round_=0.4), (36, 32, 48), bevel=1.6, outline=False)
    for sx in (-1, 1):
        c.part(star(50 + sx * 34, 24, 6, 1.4, 4), (255, 230, 120), bevel=1.0, outline=False)


def trunks(c):
    hair = (170, 140, 230)
    c.part(poly([(26, 52), (28, 28), (50, 18), (72, 28), (74, 52), (66, 56), (34, 56)], round_=3.0), hair, bevel=3.4, spec=0.5)
    bust(c, outfit=(70, 90, 200), outfit2=(240, 240, 250), ears=True)
    c.part(poly([(28, 34), (50, 26), (72, 34), (74, 46), (60, 38), (50, 36), (40, 38), (26, 46)], round_=1.6), hair, bevel=2.4, spec=0.5)
    face_details(c, eyes=(70, 120, 200), brow=(110, 90, 160), brow_tilt=-0.4, mouth=(160, 70, 70))
    c.part(circle(24, 92, 7), (255, 255, 255), bevel=1.8, spec=0.5)
    c.flat(ring(24, 92, 4.2, 1.6), (210, 40, 40), 0.9)


def tien(c):
    bust(c, outfit=(60, 150, 80), outfit2=(255, 230, 120), ears=True)
    face_details(c, eyes=(20, 30, 20), brow_tilt=-1.0, mouth=(130, 60, 50))
    c.flat(circle(50, 41, 4.6, 3.2), (255, 255, 255), 0.95)
    c.flat(circle(50, 41, 2.2, 2.6), (60, 160, 220), 1.0)
    c.add(blur(circle(50, 41, 3), 3), (120, 220, 255), 0.7)
    c.part(taper((50, 24), (50, 12), 5, 1.2), (36, 32, 48), bevel=1.2)
    c.part(poly([(12, 22), (34, 8), (28, 28)], round_=0.6), (255, 220, 60), bevel=1.4, outline=False) if False else None
    c.part(poly([(76, 10), (90, 30), (62, 30)], round_=0.8), (255, 230, 120), bevel=1.6, outline=False) if False else None


def yamcha(c):
    for i in range(3):
        c.flat(line([(14 + i * 7, 8), (42 + i * 5, 40)], 3.2, False), (255, 180, 70), 0.55)
    hair = (36, 32, 48)
    c.part(poly([(24, 54), (26, 28), (50, 20), (74, 28), (76, 54), (82, 82), (64, 72), (36, 72), (18, 82)], round_=3.0), hair, bevel=3.2, spec=0.35)
    bust(c, outfit=(240, 120, 30), outfit2=(255, 255, 255), ears=False)
    c.part(poly([(28, 36), (50, 28), (72, 36), (74, 48), (60, 40), (50, 38), (40, 40), (26, 48)], round_=1.6), hair, bevel=2.4, spec=0.35)
    face_details(c, brow_tilt=-0.4, mouth=(140, 70, 60))
    c.flat(line([(58, 58), (68, 70)], 1.4, False), (170, 60, 60), 0.9)
    c.part(taper((72, 100), (60, 78), 8, 2), (246, 244, 236), bevel=1.6, spec=0.6) if False else None


def chiaotzu(c):
    face = (250, 240, 238)
    c.part(ring(50, 56, 42, 4.0), (190, 150, 255), bevel=2.0, spec=0.6, outline=False)
    bust(c, skin=face, outfit=(70, 80, 170), outfit2=(240, 240, 250), ears=True, face=(21, 23))
    face_details(c, eyes=(20, 20, 30), brow_tilt=0.2, eye_w=3.0, eye_h=3.6, dx=8.5, mouth=(150, 80, 80))
    for sx in (-1, 1):
        c.flat(circle(50 + sx * 14, 64, 3.8), (240, 70, 70), 0.85)
    c.part(poly([(38, 34), (50, 20), (62, 34), (56, 30), (50, 36), (44, 30)], round_=0.8), (36, 32, 48), bevel=1.8)
    c.part(circle(50, 14, 4.2), (36, 32, 48), bevel=1.6)


def roshi(c):
    shell = poly([(50 + 44 * math.cos(math.radians(60 * i - 30)), 60 + 42 * math.sin(math.radians(60 * i - 30))) for i in range(6)], round_=2.0)
    c.part(shell, (60, 150, 100), bevel=4.0, spec=0.3)
    c.flat(line([(50, 20), (50, 100)], 1.4, False), (30, 90, 60), 0.6)
    c.flat(line([(12, 42), (88, 80)], 1.4, False), (30, 90, 60), 0.6)
    c.flat(line([(88, 42), (12, 80)], 1.4, False), (30, 90, 60), 0.6)
    bust(c, outfit=(250, 140, 60), ears=True)
    face_details(c, eyes=(20, 20, 20), mouth=(150, 70, 60))
    c.part(poly([(30, 60), (70, 60), (66, 82), (50, 96), (34, 82)], round_=4.0), (250, 250, 252), bevel=3.4, spec=0.3)
    c.part(poly([(38, 52), (50, 58), (62, 52), (60, 62), (50, 64), (40, 62)], round_=2.0), (250, 250, 252), bevel=2.0)
    c.part(poly([(28, 46), (48, 46), (48, 56), (30, 56)], round_=1.6), (20, 18, 28), bevel=2.0, spec=0.6)
    c.part(poly([(52, 46), (72, 46), (70, 56), (52, 56)], round_=1.6), (20, 18, 28), bevel=2.0, spec=0.6)
    c.flat(line([(48, 48), (52, 48)], 1.6, False), (20, 18, 28), 1.0)


def future_trunks(c):
    blade_sword(c, ang=38, top=-6, bot=100, w=9, blade=(210, 226, 246), guard=(236, 190, 70), grip=(90, 60, 50), wings=False, runes=None)
    hair = (170, 140, 230)
    c.part(poly([(24, 56), (26, 28), (50, 18), (74, 28), (76, 56), (80, 70), (62, 60), (38, 60), (20, 70)], round_=3.0), hair, bevel=3.4, spec=0.5)
    bust(c, outfit=(60, 60, 90), outfit2=(240, 240, 250), ears=True)
    c.part(poly([(28, 34), (50, 26), (72, 34), (74, 48), (60, 40), (50, 36), (40, 40), (26, 48)], round_=1.6), hair, bevel=2.4, spec=0.5)
    face_details(c, eyes=(70, 120, 200), brow=(110, 90, 160), brow_tilt=-0.8, mouth=(150, 70, 70))
    c.part(circle(24, 92, 7), (255, 255, 255), bevel=1.8, spec=0.5)
    c.flat(ring(24, 92, 4.2, 1.6), (210, 40, 40), 0.9)


def bulma(c):
    c.part(circle(50, 54, 38), (70, 120, 90), bevel=5.0, spec=0.5)
    c.part(circle(50, 54, 30), (30, 70, 50), bevel=2.0, spec=0.1, outline=False)
    for r in (10, 20, 28):
        c.flat(ring(50, 54, r, 0.9), (120, 240, 160), 0.55)
    c.flat(line([(50, 24), (50, 84)], 0.9, False), (120, 240, 160), 0.5)
    c.flat(line([(20, 54), (80, 54)], 0.9, False), (120, 240, 160), 0.5)
    for x, y in ((40, 40), (62, 46), (34, 62), (56, 70), (66, 60), (46, 52), (58, 34)):
        c.dot(x, y, 2.0, (255, 150, 60), 1.0)
    c.part(taper((50, 18), (50, 4), 5, 2), (60, 100, 80), bevel=1.2)
    c.part(circle(50, 3, 3), (255, 100, 100), bevel=1.0)


db("goku", pal((205, 214, 236), (240, 120, 30), (170, 220, 255), (170, 210, 255), (60, 80, 120), (6, 8, 24)), goku)
db("vegeta", pal((54, 80, 190), (240, 240, 250), (255, 220, 90), (110, 150, 255), (40, 52, 120), (4, 6, 24)), vegeta)
db("gohan", pal((240, 242, 250), (70, 50, 110), (230, 40, 50), (200, 150, 255), (70, 40, 120), (8, 4, 22)), gohan)
db("piccolo", pal((112, 190, 86), (250, 250, 255), (120, 60, 160), (150, 240, 120), (36, 90, 44), (4, 14, 8)), piccolo)
db("krillin", pal((250, 208, 164), (240, 120, 30), (255, 190, 80), (255, 190, 80), (120, 70, 24), (16, 8, 8)), krillin)
db("android18", pal((250, 214, 90), (60, 80, 130), (70, 150, 230), (120, 190, 255), (46, 66, 110), (6, 8, 22)), android18)
db("android17", pal((30, 28, 40), (250, 160, 40), (120, 240, 170), (120, 240, 170), (30, 80, 70), (4, 12, 12)), android17)
db("goten", pal((36, 32, 48), (240, 120, 30), (255, 230, 120), (255, 200, 90), (110, 76, 30), (14, 8, 8)), goten)
db("trunks", pal((170, 140, 230), (70, 90, 200), (210, 40, 40), (160, 150, 255), (60, 56, 130), (6, 6, 24)), trunks)
db("tien", pal((250, 208, 164), (60, 150, 80), (120, 220, 255), (120, 230, 140), (40, 100, 56), (4, 14, 8)), tien)
db("yamcha", pal((36, 32, 48), (240, 120, 30), (255, 180, 70), (255, 170, 70), (120, 70, 24), (16, 8, 6)), yamcha)
db("chiaotzu", pal((250, 240, 238), (70, 80, 170), (190, 150, 255), (190, 150, 255), (70, 56, 130), (8, 6, 24)), chiaotzu)
db("roshi", pal((250, 250, 252), (60, 150, 100), (250, 140, 60), (255, 190, 100), (40, 100, 64), (4, 14, 10)), roshi)
db("future_trunks", pal((170, 140, 230), (60, 60, 90), (236, 190, 70), (190, 160, 255), (60, 50, 110), (6, 6, 22)), future_trunks)
db("bulma", pal((70, 120, 90), (255, 150, 60), (120, 240, 160), (120, 240, 160), (30, 80, 60), (4, 12, 10)), bulma)
