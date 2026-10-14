"""Rune Splitter (card: Pinterest card, "Rune Splitter", very rare, requires attunement, battleaxe +1): a tall single-bladed battleaxe: a large curved
golden crescent head engraved with a sun-and-rune diamond, a long dark red knobbed haft with glowing yellow star studs, golden collars and a gold
ring, a strap that seems made of light trailing along the haft, and a flared red foot. The 3D model, built with the owner's method.

Reference: the card screenshot, 1080 x 2400 (it shows 900 x 2000; the figures below are in the 900-wide view, x 1.2 for the file): the axe runs from the
foot (690, 1790) to the finial (668, 700): 1090 px. An axe of this height is 1.35 m, so K = 0.1239 cm per px. s = px from the foot.
Axis: y up from the foot to the finial; the head's crescent opens toward -x (the card's left) and its face looks at +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all

# ---------------------------------------------------------------- 1 measure
TOTAL = 135.0
S_TOT = 1090.0
K = TOTAL / S_TOT


def Y(s):                                             # s = card px above the foot
    return s * K


R_HAFT = 2.2                                          # the haft is 36 px thick on the card, drawn heavy: 2.2 cm radius
S_FOOT = (0, 70)                                      # the flared foot (card y 1720..1790)
S_HAFT = (70, 790)                                    # the red knobbed haft up to the sleeve (card y 1000..1720)
S_SLEEVE = (790, 905)                                 # the gold sleeve with its diamond (card y 885..1000)
S_UPPER = (905, 1040)                                 # the smooth upper haft, red with gold rivets (card y 750..885)
S_SOCKET = (1040, 1090)                               # the gold socket and finial cap (card y 700..750)
KNOBS = [95, 200, 305, 410, 515, 620, 710]            # a swelling and a star stud every ~105 px
R_KNOB = 2.9
# the crescent head, in model cm (x toward -x, y up from the foot): outer arc control points read off the card (head spans card x 505..705, y 650..990)
HEAD_PATH = [(-1.0, Y(1040) - 1.0), (-4.0, Y(1010)), (-9.5, Y(990) - 2.0), (-16.5, Y(965)), (-22.0, Y(925)), (-24.0, Y(880)), (-22.0, Y(835)), (-17.0, Y(805)), (-9.0, Y(790)), (-1.2, Y(800))]
HEAD_PATH = [(x * 1.25, Y(1040) - (Y(1040) - y) * 1.3) for (x, y) in HEAD_PATH]               # the card's head is a third of the whole axe: scale it up to that
HEAD_HALF_W = [0.2, 2.0, 4.2, 6.4, 7.6, 7.8, 7.0, 5.6, 3.4, 1.0]             # the web of the blade: its outer edge is the path, its width reaches in toward the haft
HEAD_T = 0.55                                         # half thickness of the head plate
SPIKE_TOP = [(-1.0, Y(1040)), (-1.2, Y(1075)), (-5.2, Y(1100))]       # the hooked upper horn of the crescent (card (640, 650))


def build():
    m = ml.Mesh("rune_splitter")
    # ---- 1 the knobbed haft: a lathe of the red wood with a swelling at every star stud
    hf = w.add(m, "haft", "wood", base=(112, 28, 30), smooth=45, uv_mode="loft")
    prof = [(Y(S_HAFT[0]), R_HAFT * 0.85)]
    for kp in KNOBS:
        prof += [(Y(kp) - 3.0, R_HAFT), (Y(kp), R_KNOB), (Y(kp) + 3.0, R_HAFT)]
    prof += [(Y(S_SLEEVE[0]) + 0.5, R_HAFT)]
    ml.loft(hf, "y", (0, 0, 0), [(s, r, r) for (s, r) in prof], ml.circle(1.0, 10))
    up = w.add(m, "haft_upper", "wood", base=(118, 30, 32), smooth=45, uv_mode="loft")
    ml.loft(up, "y", (0, 0, 0), [(Y(S_UPPER[0]) - 0.5, R_HAFT, R_HAFT), (Y(S_UPPER[1]) + 0.5, R_HAFT * 0.95, R_HAFT * 0.95)], ml.circle(1.0, 10))
    # ---- 2 the five glowing star studs: each a small gold diamond (gem on its side) standing out of a knob
    ss = w.add(m, "star_studs", "gold", base=(255, 236, 140), glow=True, smooth=0)
    for kp in KNOBS[1:6]:
        ml.gem(ss, (0.0, Y(kp), R_KNOB * 0.98), 0.9, long=1.5, sides=4, axis="y")
    # ---- 3 the gold sleeve with its diamond plate, 4 the collars and the gold ring below the head
    sl = w.add(m, "sleeve", "gold", base=(222, 170, 70), smooth=45)
    ml.lathe(sl, "y", (0, 0, 0), [(Y(S_SLEEVE[0]), R_HAFT * 1.05), (Y(S_SLEEVE[0]) + 0.8, R_HAFT * 1.3), (Y(S_SLEEVE[1]) - 0.8, R_HAFT * 1.3), (Y(S_SLEEVE[1]), R_HAFT * 1.05)], sides=12)
    dm = w.add(m, "sleeve_diamond", "gold", base=(255, 220, 110), glow=True, smooth=0)
    ml.gem(dm, (0.0, (Y(S_SLEEVE[0]) + Y(S_SLEEVE[1])) / 2, R_HAFT * 1.25), 1.5, long=2.0, sides=4, axis="y")
    co = w.add(m, "collars", "gold", base=(214, 164, 64), wear=0.4, smooth=55)
    for s in (S_HAFT[1] - 22, S_UPPER[0] + 10, S_UPPER[1] - 14):
        ml.collar(co, Y(s), R_HAFT * 1.16, 1.3, lip=0.35, sides=12)
    for s in (S_UPPER[0] + 40, S_UPPER[0] + 85):                                                  # the rivets down the upper haft
        ml.rivet(co, (0.0, Y(s), R_HAFT * 0.95), 0.5, (0, 0, 1), 6)
    # ---- 5 the socket and its finial (the gold cap with a ring, card (668, 700..750))
    sk = w.add(m, "socket", "gold", base=(230, 180, 76), smooth=45)
    ml.lathe(sk, "y", (0, 0, 0), [(Y(S_SOCKET[0]) - 0.3, R_HAFT * 1.1), (Y(S_SOCKET[0]) + 1.0, R_HAFT * 1.35), (Y(S_SOCKET[1]) - 2.6, R_HAFT * 1.1), (Y(S_SOCKET[1]) - 0.8, R_HAFT * 1.3), (Y(S_SOCKET[1]), 0.8)], sides=10)
    ml.sweep(sk, [(1.0 * math.cos(a) * 1.1, Y(S_SOCKET[1]) + 1.2 + 1.1 * math.sin(a) * 0.9 + 1.0, 0.0) for a in np.linspace(0, 2 * math.pi, 13)], ml.circle(1.0, 5), scales=lambda t: (0.28, 0.28), caps=(False, False))
    # ---- 6 the crescent head: a swept plate (a bevelled band, wide in the belly, thin at the horns), 7 its hooked upper horn
    hd = w.add(m, "head_blade", "gold", base=(236, 184, 70), smooth=40, uv_mode="loft")
    cx, cy = -11.0, Y(880)                                         # the middle of the crescent: the web lies between the outer edge and here
    path = []
    for (x, y), hw in zip(HEAD_PATH, HEAD_HALF_W):
        d = math.hypot(cx - x, cy - y) or 1.0
        path.append((x + (cx - x) / d * hw, y + (cy - y) / d * hw, 0.0))
    ml.sweep(hd, path, ml.lozenge(2.0, 1.0, 0.45), scales=lambda t: (HEAD_HALF_W[min(len(HEAD_HALF_W) - 1, int(t * (len(HEAD_HALF_W) - 1) + 0.5))], HEAD_T) if t < 0.98 else (0.0, 0.0), up=(0, 0, 1))
    hp = w.add(m, "head_horn", "gold", base=(246, 200, 84), smooth=45, uv_mode="loft")
    ml.sweep(hp, [(x, y, 0.0) for (x, y) in SPIKE_TOP], ml.lozenge(2.0, 1.0, 0.4), scales=lambda t: (1.6 * (1 - t) ** 0.7 + 0.05 if t < 0.97 else 0.0, 0.5 * (1 - 0.5 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
    # ---- 8 the engraved sun-and-rune diamond on the face of the blade, 9 the studs round it, 10 the engraved rim ridge
    sun = w.add(m, "sun_rune", "gold", base=(255, 232, 130), glow=True, smooth=0)
    ml.gem(sun, (-14.0, Y(880), HEAD_T * 0.8), 3.4, long=1.2, sides=4, axis="y")
    ml.gem(sun, (-14.0, Y(880), -HEAD_T * 0.8), 3.4, long=1.2, sides=4, axis="y")
    sd = w.add(m, "head_studs", "gold", base=(214, 160, 56), smooth=50)
    for idx in (2, 3, 4, 5, 6, 7):                                                                # studs along the engraved rim, a hand's width inside the edge
        x, y = HEAD_PATH[idx]
        d = math.hypot(cx - x, cy - y) or 1.0
        px, py = x + (cx - x) / d * 1.1, y + (cy - y) / d * 1.1
        for face in (1, -1):
            ml.rivet(sd, (px, py, face * HEAD_T * 0.62), 0.45, (0, 0, face), 6)
    # ---- 11 the strap that appears to be made of light: a wavy ribbon down the haft, glowing
    lt = w.add(m, "light_strap", "crystal", base=(255, 240, 130), glow=True, smooth=60, uv_mode="loft")
    pts = [(R_HAFT * 1.35 + 0.8 * math.sin(i * 0.55), Y(60 + i * 24), 0.6 * math.cos(i * 0.4)) for i in range(0, 38)]
    ml.sweep(lt, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (0.55 * (0.4 + 0.6 * math.sin(math.pi * t) ** 0.5), 0.1), up=(0, 0, 1))
    # ---- 12 the flared foot with its gem
    ft = w.add(m, "foot", "wood", base=(104, 24, 28), smooth=45)
    ml.lathe(ft, "y", (0, 0, 0), [(Y(S_FOOT[0]), R_HAFT * 0.6), (Y(S_FOOT[0]) + 1.2, R_HAFT * 1.5), (Y(40), R_HAFT * 1.85), (Y(S_FOOT[1]) - 1.0, R_HAFT * 1.2), (Y(S_FOOT[1]), R_HAFT * 0.9)], sides=10, squash=0.85)
    ml.gem(ss, (R_HAFT * 0.9, Y(22), R_HAFT * 1.3), 0.7, long=1.3, sides=6, axis="z")                       # the foot's gem, in the star studs' part

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p2/Relics of the world part 2/Screenshot_20261013_223011_Opera.jpg", (560, 760, 1080, 2200), "rune_splitter")
    w.finish("rune_splitter", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.14, 0.4, 1.0), ref=ref)


def _paint(m, atlas):
    def haft(c):
        sh = c.img.shape[:2]
        g = w.noise2(sh, 3, 5)
        c.img[..., :3] = np.clip(c.img[..., :3] * (0.82 + 0.4 * g[..., None]), 0, 255)
    for nm in ("haft", "haft_upper", "foot"):
        w.paint_part(atlas, m.part(nm), haft)
    for nm, rgb, st in (("star_studs", (255, 236, 120), 0.95), ("sleeve_diamond", (255, 236, 120), 0.8), ("sun_rune", (255, 230, 110), 0.7), ("light_strap", (255, 244, 130), 0.95)):
        glow_all(atlas, m.part(nm), rgb, st, 0.4)

    def head(c):                                                  # engraved runes: darker lines in the gold, glowing faintly
        sh = c.img.shape[:2]
        ln = (np.abs(np.sin(c.A * 1.1 + c.S * 8)) < 0.07) | (np.abs(np.sin(c.B * 0.9)) < 0.05)
        c.img[..., :3] = np.where(ln[..., None], c.img[..., :3] * 0.55, c.img[..., :3])
        w.set_glow(c, (255, 220, 100), 0.35, mask=ln.astype(np.float32), core=0.2)
    w.paint_part(atlas, m.part("head_blade"), head)
