"""Goliath, the giant stone construct boss, rebuilt in the "anime cel" style (PILOT, PREVIEW ONLY: nothing is wired into the game, no Java, no existing file is touched).

    python3 -B tools/gen_anime_goliath.py                         # model + texture + glow + animations + scenes (idempotent)
    python3 -B tools/gen_anime_goliath.py --preview [scene ...]   # render scenes with tools/render_preview (flat cel light, like the game's FlatLit) into <scratch>/previews_anime_goliath
    python3 -B tools/gen_anime_goliath.py --sheets                # compose the labelled contact sheets into docs/mobs/phaseE/goliath_*.png

Writes (all new files):
    src/main/resources/assets/nusmp/geo/entity/anime_goliath.geo.json
    src/main/resources/assets/nusmp/animations/entity/anime_goliath.animation.json       idle walk slam stomp roar rise hurt death transform (+ attack)
    src/main/resources/assets/nusmp/textures/entity/anime_goliath.png, anime_goliath_glow.png       (palettised; the glow sheet is half size, same uv)
    tools/render_preview/scenes.d/anime_goliath.json

The model: a 6.4 block stone giant (the hit box is 6.0 tall and stays as it is), 3.7 heads tall, a small head sunk between huge stepped pauldrons, fists 1.5x, a glowing eye slit under
a brow ridge, a chest sigil and rune cracks (the glow sheet), moss and vines as cards, a rubble trail at the feet and a few floating stones. Cubes and single-quad cards only; every
face painted by tools/anime_goliath_kit.py (cel bands, a painted outline, a 23 colour palette).
"""
import json
import math
import os
import random
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim  # noqa: E402
from zogratis_lucius_kit import Pack, FACES  # noqa: E402
import anime_goliath_kit as K  # noqa: E402
from anime_goliath_kit import Mat, stone, card, seed_of  # noqa: E402

KEY, NAME = "anime", "goliath"
REPO = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "anime_goliath.json")
SCRATCH = os.environ.get("AG_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
PREVIEW_OUT = os.path.join(SCRATCH, "previews_anime_goliath")
DOCS_OUT = os.path.join(REPO, "docs", "mobs", "phaseE")
TEX = 1024


# ------------------------------------------------------------------------------------------------------------------ the builder
class G:
    """Cubes through tools/zogratis_lucius_kit.Pack (every face gets its own texel rectangle, painted by a K.Painter)."""

    def __init__(self):
        self.m = Model(KEY, NAME, TEX, TEX)
        self.pk = Pack(TEX, TEX, dens=2)
        self.A = Anim()
        self.n = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def slab(self, bone, x0, y0, z0, w, h, d, mat, name=None, no=(), faces=None, dens=None, rot=None, piv=None):
        self.n += 1
        nm = name or "s%d" % self.n
        fs = tuple(faces) if faces else tuple(f for f in FACES if f not in no)
        return self.pk.box(self.m, bone, (x0, y0, z0), (w, h, d), stone(mat, nm), rotation=rot, pivot=piv, faces=fs, dens=dens)

    def sym(self, bl, br, x0, y0, z0, w, h, d, mat, name, rot=None, piv=None, **kw):
        """The +x slab on bone bl and its mirror (x -> -x) on bone br; each is painted on its own (another seed), so moss and cracks differ."""
        self.slab(bl, x0, y0, z0, w, h, d, mat, name + "_l", rot=rot, piv=piv, **kw)
        r2 = None if not rot else (rot[0], -rot[1], -rot[2])
        p2 = None if not piv else (-piv[0], piv[1], piv[2])
        self.slab(br, -(x0 + w), y0, z0, w, h, d, mat, name + "_r", rot=r2, piv=p2, **kw)

    def card(self, bone, x, y, z, w, h, fn, name, ry=0.0, rz=0.0, both=False, dens=None, **kw):
        """A single quad (cube 0.5 thick, only the front face drawn) standing at (x, y = its bottom or top edge handled by the caller, z), turned `ry` about y. Cross cards: both=True."""
        self.n += 1
        out = []
        for k, yaw in enumerate((ry, ry + 90.0) if both else (ry,)):
            pivot = (x, y, z)
            out.append(self.pk.box(self.m, bone, (x - w / 2.0, y, z - 0.25), (w, h, 0.5), card(fn, seed_of(name, k), **kw), rotation=(0, yaw, rz) if (yaw or rz) else None,
                                   pivot=pivot if (yaw or rz) else None, faces=("front",), dens=dens or 2, key=None))
        return out

    def finish(self):
        self.pk.finish()
        return self.pk.images()


def M(fam="slate", **kw):
    if "chip" in kw:
        kw["chip"] *= 0.5
    if "crack" in kw:
        kw["crack"] *= 0.65
    return Mat(fam, **kw)


# ------------------------------------------------------------------------------------------------------------------ the rig and the cubes
RUNES = [
    [[(0.18, 0.10), (0.18, 0.46), (0.5, 0.46), (0.5, 0.90)]],
    [[(0.5, 0.08), (0.5, 0.40), (0.26, 0.58), (0.26, 0.92)], [(0.5, 0.40), (0.74, 0.58), (0.74, 0.80)]],
    [[(0.12, 0.20), (0.40, 0.20), (0.40, 0.62), (0.76, 0.62), (0.76, 0.92)]],
    [[(0.30, 0.10), (0.30, 0.34), (0.62, 0.34), (0.62, 0.66), (0.40, 0.66), (0.40, 0.92)]],
    [[(0.84, 0.10), (0.84, 0.50), (0.56, 0.50), (0.56, 0.92)], [(0.20, 0.30), (0.20, 0.72)]],
]


def rune(k, face="front"):
    return [{"face": face, "kind": "runes", "lines": RUNES[k % len(RUNES)]}]


def build():
    g = G()
    HEAD_Y = 77.0
    AX = 38.0                                                # the arm axis
    b = g.bone
    b("root")
    b("body", "root", (0, 38, 0))
    b("chest", "body", (0, 50, 0), (-9, 0, 0))               # a slight hunch is part of the rest pose
    b("head", "chest", (0, HEAD_Y, -6), (8, 0, 0))
    b("crest", "head", (0, HEAD_Y + 21, -2))
    for suf, s in (("_l", 1), ("_r", -1)):
        b("pauld" + suf, "chest", (s * 33, 72, 0))
        b("moss" + suf, "pauld" + suf, (s * 33, 72, -17))
        b("arm" + suf, "chest", (s * AX, 72, 0), (12, 0, s * 5))
        b("forearm" + suf, "arm" + suf, (s * AX, 50, 0), (9, 0, s * -2))
        b("hand" + suf, "forearm" + suf, (s * AX, 31, 0))
        b("vine" + suf, "forearm" + suf, (s * (AX + 10), 50, 6))
        b("leg" + suf, "root", (s * 14.5, 38, 0))
        b("shin" + suf, "leg" + suf, (s * 14.5, 23, 0))
        b("foot" + suf, "shin" + suf, (s * 14.5, 8, 0))
    b("tasset_c", "body", (0, 47, -16))
    b("tasset_l", "body", (14, 47, -14))
    b("tasset_r", "body", (-14, 47, -14))
    b("tasset_b", "body", (0, 47, 14))
    b("ridge", "chest", (0, 74, 14))
    b("moss_back", "chest", (0, 80, 8))
    b("orbit", "root", (0, 84, 2))
    b("rubble", "root", (0, 0, 0))
    b("trail", "root", (0, 0, 22))

    # ---------------------------------------------------------------- feet and legs (the +x leg is drawn, the -x leg is its mirror with its own painting)
    for s, suf in ((1, "_l"), (-1, "_r")):
        sx = lambda x0, w, s=s: x0 if s > 0 else -(x0 + w)

        def add(base, x0, y0, z0, w, h, d, mat, nm, s=s, suf=suf, sx=sx, **kw):
            g.slab(base + suf, sx(x0, w), y0, z0, w, h, d, mat, nm + suf, **kw)

        add("foot", 3.5, 0, -15, 22, 6, 29, M("slate", moss=0.35, chip=0.5), "foot_base")
        add("foot", 6, 6, -9, 17, 6, 20, M("basalt", moss=0.5, drip=4, crack=0.6), "foot_top", no=("bottom",))
        add("foot", 5, 1, 11, 19, 10, 5, M("basalt", crack=0.5), "heel", no=("bottom",))
        for k, (tx, tw, tl) in enumerate(((3.5, 6.5, 7.0), (10.5, 8.0, 10.0), (19.0, 6.5, 7.0))):
            add("foot", tx, 0, -15 - tl, tw, 5.5, tl, M("slate" if k != 1 else "sand", chip=0.6, crack=0.4), "toe%d" % k)
        add("shin", 5, 8, -10, 19, 8, 20, M("slate", crack=0.6, hot=0.25), "greave_lo")
        add("shin", 4, 15, -11, 21, 9, 22, M("basalt", seam="slab", moss=0.3, drip=3, crack=0.4), "greave_hi", no=("bottom",))
        add("shin", 6, 21, -17, 17, 10, 7, M("basalt", moss=0.7, drip=4, crack=0.5, panel=True, decals=rune(0)), "kneecap", no=("bottom",), dens={"front": 3})
        add("leg", 4.5, 29, -10, 20, 13, 20, M("slate", seam="brick", crack=0.5, hot=0.3), "thigh", no=("bottom",))
        add("leg", 6, 30, -14, 17, 11, 4.5, M("basalt", crack=0.5, chip=0.5, panel=True), "thigh_plate", no=("bottom", "back"))
        add("leg", 24, 37, -9, 3.5, 10, 18, M("basalt", chip=0.5, crack=0.4), "hip_plate", no=("bottom",))
    # ---------------------------------------------------------------- pelvis, belt, the stone loincloth
    g.slab("body", -20, 38, -12, 40, 12, 24, M("slate", seam="brick", crack=0.6, hot=0.2), "pelvis", no=("bottom",))
    g.slab("body", -23, 47, -14.5, 46, 6, 29, M("basalt", crack=0.4, chip=0.5), "belt", no=("bottom",))
    g.slab("body", -6, 44, -18, 12, 11, 3.5, M("slate", panel=True, decals=[{"face": "front", "kind": "sigil", "r": 4, "cx": 0.5, "cy": 0.5}]), "buckle", no=("bottom", "back"), dens=3)
    g.slab("tasset_c", -7, 24, -19, 14, 23, 4, M("basalt", crack=0.7, seam="slab", chip=0.5, decals=rune(1)), "tasset_c", no=("bottom", "back", "top"))
    g.sym("tasset_l", "tasset_r", 8, 29, -17.5, 9, 18, 3.5, M("basalt", crack=0.6, chip=0.5), "tasset", no=("bottom", "back", "top"))
    g.sym("tasset_l", "tasset_r", 17, 33, -15, 6, 14, 3.5, M("slate", crack=0.6, chip=0.5), "tasset2", no=("bottom", "back", "top"))
    g.slab("tasset_b", -12, 28, 15, 24, 19, 3.5, M("basalt", crack=0.5, chip=0.5), "tasset_back", no=("bottom", "front", "top"))
    # ---------------------------------------------------------------- torso: a V (broad ribcage over a narrow waist)
    g.slab("chest", -14, 47, -10, 28, 13, 20, M("slate", seam="brick", crack=0.7, hot=0.4), "abdomen", no=("bottom",))
    g.slab("chest", -19, 59, -12, 38, 9, 24, M("slate", moss=0.3, drip=4, crack=0.6, hot=0.5, chip=0.5), "rib_lo", no=("bottom",))
    g.slab("chest", -24, 67, -13, 48, 8, 26, M("slate", moss=0.35, drip=5, crack=0.6, hot=0.5, chip=0.5), "rib_hi", no=("bottom",))
    g.slab("chest", -17, 55, -17.5, 34, 20, 5.5, M("basalt", crack=0.6, chip=0.5, panel=True, decals=[{"face": "front", "kind": "sigil", "r": 13, "cx": 0.5, "cy": 0.5}]), "chest_plate",
           no=("bottom", "back"), dens={"front": 3})
    g.sym("chest", "chest", 9, 74, -12, 20, 6, 24, M("slate", moss=0.6, drip=6, crack=0.4, chip=0.5), "yoke", no=("bottom",))
    g.slab("chest", -9, 74, -10, 18, 4, 20, M("slate", crack=0.5), "collar", no=("bottom",))
    g.slab("chest", -20, 56, 13, 40, 19, 3.5, M("basalt", crack=0.7, chip=0.5, panel=True, decals=[{"face": "back", "kind": "sigil", "r": 7, "cx": 0.5, "cy": 0.5}]), "back_plate",
           no=("bottom", "front"), dens={"back": 3})
    for k, (w, h, y0) in enumerate(((11, 6, 52), (13, 7, 59), (11, 7, 67))):
        g.slab("ridge", -w / 2.0, y0 + 1, 15, w, h, 6 - k, M("slate", moss=0.3, crack=0.6, chip=0.6, hot=0.5), "ridge%d" % k, no=("bottom",))
    for k, (x, hh, lean) in enumerate(((0, 14, 0), (-5.5, 11, 20), (5.5, 11, -20))):
        g.card("ridge", x, 74 + (1 if k == 0 else -1), 17.5, 7, hh, K.spike_card, "ridge_sp%d" % k, rz=lean, both=True, fam="basalt" if k else "slate")
    # ---------------------------------------------------------------- the head: neck, jaw, muzzle, the eye slot, brow, crown, cranium, ear plates, crest
    g.slab("head", -7, HEAD_Y - 6, -8, 14, 8, 14, M("slate", flat=True), "neck", no=("top", "bottom"))
    g.slab("head", -7, HEAD_Y, -12, 14, 4, 9, M("slate", crack=0.5, chip=0.5), "jaw", no=("bottom",), dens=3)
    g.slab("head", -10, HEAD_Y + 4, -13, 20, 4, 11, M("slate", crack=0.5, chip=0.4, decals=[{"face": "front", "kind": "mouth"}]), "muzzle", no=("bottom",), dens=3)
    g.pk.box(g.m, "head", (-10, HEAD_Y + 8, -11.5), (20, 3, 9), K.eye_painter("slot"), faces=("front", "left", "right", "top", "bottom"), dens={"front": 4, "left": 2, "right": 2, "top": 2, "bottom": 2})
    g.slab("head", -15, HEAD_Y + 11, -16, 30, 6, 16, M("basalt", moss=0.5, drip=3, crack=0.5, chip=0.6), "brow", no=("back",), dens={"front": 3, "top": 3, "left": 2, "right": 2, "bottom": 2})
    g.slab("head", -12, HEAD_Y + 17, -9, 24, 4, 19, M("slate", moss=0.5, crack=0.4, chip=0.5), "crown", no=("bottom",))
    g.slab("head", -12, HEAD_Y + 4, -3, 24, 13, 15, M("slate", crack=0.6, chip=0.5, moss=0.2, drip=3, hot=0.4), "cranium", no=("front",))
    g.sym("head", "head", 12, HEAD_Y + 5, -9, 3.5, 11, 11, M("basalt", chip=0.6, crack=0.5), "ear")
    for k, (x, hh, lean, fam) in enumerate(((0, 9, 0, "slate"), (-5, 8, 24, "sand"), (5, 8, -24, "sand"), (-9, 7, 46, "slate"), (9, 7, -46, "slate"))):
        g.card("crest", x, HEAD_Y + 21 - (0 if k < 3 else 1.5), 0.0, 6.5, hh, K.spike_card, "crest%d" % k, rz=lean, both=True, fam=fam)
    # ---------------------------------------------------------------- pauldrons: a twisted pagoda of three tiers, a flared outer guard, a front hood, two long sweeping horns, moss
    for s, suf in ((1, "_l"), (-1, "_r")):
        sxx = lambda x0, w, s=s: x0 if s > 0 else -(x0 + w)
        P = "pauld" + suf

        def padd(x0, y0, z0, w, h, d, mat, nm, rot=None, piv=None, s=s, suf=suf, sxx=sxx, **kw):
            r2 = None if not rot else ((rot[0], rot[1] * s, -rot[2] * s))
            p2 = None if not piv else (s * piv[0], piv[1], piv[2])
            g.slab(P, sxx(x0, w), y0, z0, w, h, d, mat, nm + suf, rot=r2, piv=p2, **kw)
        padd(17, 60, -15, 30, 9, 30, M("basalt", seam="slab", crack=0.6, chip=0.6, moss=0.25, drip=4, panel=True), "pd1", rot=(0, 0, 7), piv=(17, 65, 0), no=("bottom",))
        padd(20, 66, -13, 25, 8, 26, M("slate", crack=0.6, chip=0.6, moss=0.4, drip=3, hot=0.4), "pd2", rot=(0, -12, 0), piv=(33, 70, 0), no=("bottom",))
        padd(24, 72, -10, 17, 7, 20, M("basalt", moss=0.8, crack=0.5, chip=0.6), "pd3", rot=(0, 14, 0), piv=(33, 76, 0), no=("bottom",))
        padd(44, 55, -12, 5, 24, 24, M("basalt", crack=0.7, chip=0.7, hot=0.5, panel=True), "pd_guard", rot=(0, 0, 11), piv=(44, 64, 0), no=("bottom",))
        padd(22, 58, -19.5, 24, 14, 5, M("basalt", crack=0.6, chip=0.7, hot=0.3, panel=True, decals=rune(2)), "pd_hood", no=("bottom", "back"))
        g.card(P, s * 29, 78, 0, 9, 23, K.spike_card, "horn1" + suf, rz=-36 * s, both=True, fam="slate", curve=0.12)
        g.card(P, s * 36, 78, 0, 7.5, 18, K.spike_card, "horn2" + suf, rz=-60 * s, both=True, fam="sand", curve=0.12)
        for k, (x, hh, lean) in enumerate(((25, 9, -8), (41.5, 8, 28))):
            g.card(P, s * x, 78, 0, 5, hh, K.spike_card, "pth%d%s" % (k, suf), rz=-lean * s, both=True, fam="basalt")
        for k, x in enumerate((24.5, 30.5, 36.5, 42.0)):
            g.card("moss" + suf, s * x, 58 + (k % 2) * 1.5, -20.0, 6.5, 13 - (k % 2) * 3, K.tuft_card, "mt%d%s" % (k, suf))
    for k, x in enumerate((-17, -8.5, 0, 8.5, 17)):
        g.card("moss_back", x, 68 - (k % 2) * 3, 12.6, 9, 12 + (k % 2) * 4, K.tuft_card, "mb%d" % k)
    # ---------------------------------------------------------------- arms
    for s, suf in ((1, "_l"), (-1, "_r")):
        sxx = lambda x0, w, s=s: x0 if s > 0 else -(x0 + w)
        A_, F_, H_ = "arm" + suf, "forearm" + suf, "hand" + suf

        def aadd(bone, x0, y0, z0, w, h, d, mat, nm, s=s, suf=suf, sxx=sxx, **kw):
            g.slab(bone, sxx(x0, w), y0, z0, w, h, d, mat, nm + suf, **kw)
        aadd(A_, 30, 50, -9.5, 16, 22, 19, M("slate", seam="brick", crack=0.7, hot=0.45, moss=0.3, drip=4), "uarm", no=("bottom",))
        aadd(A_, 28.5, 58, -12, 19, 8, 24, M("basalt", seam="slab", crack=0.6, chip=0.6, panel=True), "uband", no=("bottom",))
        aadd(A_, 28.5, 49, -11, 19, 7, 22, M("slate", crack=0.6, chip=0.6), "elbow", no=("bottom",))
        g.card(A_, s * (AX + 1), 52, 11.2, 8, 12, K.spike_card, "elb_sp" + suf, rz=0, both=True, fam="sand")
        aadd(F_, 31, 32, -9, 14, 19, 18, M("slate", crack=0.5), "farm", no=("bottom", "top"))
        aadd(F_, 27, 33, -13, 22, 17, 26, M("basalt", seam="slab", crack=0.7, chip=0.6, moss=0.2, drip=3, hot=0.4, panel=True, decals=rune(3)), "bracer", no=("bottom",), dens={"front": 3, "left": 3, "right": 3})
        aadd(F_, 26, 49, -13.5, 24, 4, 27, M("slate", crack=0.5, chip=0.6), "cuff_hi", no=("bottom",))
        aadd(F_, 26, 30, -13.5, 24, 4, 27, M("slate", crack=0.5, chip=0.6), "cuff_lo")
        aadd(H_, 28, 14, -11, 20, 17, 22, M("slate", seam="brick", crack=0.6, hot=0.35), "palm", no=("bottom",))
        aadd(H_, 30, 29, -3, 16, 4, 14, M("basalt", crack=0.6, chip=0.6), "hand_plate", no=("bottom",))
        for k in range(4):
            aadd(H_, 28 + 5.0 * k, 9, -18.5, 4.8, 14 - (k % 2), 7.5, M("sand" if k % 2 else "slate", crack=0.5, chip=0.6), "finger%d" % k)
        aadd(H_, 23.5, 17, -14, 5, 10, 13, M("slate", crack=0.5, chip=0.6), "thumb")
        g.card("vine" + suf, s * (AX + 10), 28, 9.5, 8, 28, K.vine_card, "vine_a" + suf)
        g.card("vine" + suf, s * (AX - 8), 30, 14.0, 8, 22, K.vine_card, "vine_b" + suf, ry=90 * s)
    # ---------------------------------------------------------------- the rubble at the feet and the trail behind
    rng = random.Random(7)
    spots = [(-30, -26, 5), (28, -30, 6), (-42, -4, 4), (44, -6, 5), (-26, 22, 4), (22, 26, 5), (0, -34, 4), (-17, -34, 3.5), (38, 12, 3.5), (-36, 14, 4.5)]
    for k, (x, z, sz) in enumerate(spots):
        h = sz * rng.uniform(0.55, 0.9)
        g.slab("rubble", x - sz / 2, 0, z - sz / 2, sz, h, sz * rng.uniform(0.8, 1.2), M("slate" if k % 3 else "basalt", moss=0.5 if k % 2 else 0.0, chip=0.7, crack=0.2, ow=1), "rub%d" % k,
               no=("bottom",), rot=(0, rng.uniform(-35, 35), 0), piv=(x, 0, z))
    for k in range(7):
        sz = 6.5 - k * 0.7
        x = rng.uniform(-8, 8) + (k % 2) * 5 - 2
        z = 26 + k * 6.5
        g.slab("trail", x - sz / 2, 0, z - sz / 2 - 22, sz, sz * 0.7, sz, M("slate" if k % 2 else "basalt", chip=0.7, crack=0.2, moss=0.4 if k % 3 == 0 else 0.0, ow=1), "trl%d" % k,
               no=("bottom",), rot=(0, rng.uniform(-40, 40), 0), piv=(x, 0, z - 22))
    for k, (ang, r, y, sz) in enumerate(((20, 40, 96, 6), (110, 42, 86, 5), (200, 38, 100, 7), (290, 43, 90, 5.5))):
        x, z = r * math.sin(math.radians(ang)), r * math.cos(math.radians(ang))
        g.slab("orbit", x - sz / 2, y - sz / 2, z - sz / 2, sz, sz * 0.8, sz, M("slate" if k % 2 else "basalt", chip=0.9, crack=0.3, moss=0.5 if k == 0 else 0.0, ow=1), "orb%d" % k,
               rot=(random.Random(k).uniform(-25, 25), random.Random(k + 9).uniform(-45, 45), random.Random(k + 5).uniform(-25, 25)), piv=(x, y, z))
    return g


# ------------------------------------------------------------------------------------------------------------------ files
def palettise(img, path, colors=256):
    """RGBA image -> PNG8 with a transparency table, exact colours (they all come from the palette); falls back to a quantiser if there are more than `colors`."""
    from PIL import Image
    a = np.array(img.convert("RGBA"))
    flat = a.reshape(-1, 4)
    flat = np.where((flat[:, 3:4] == 0), 0, flat)
    uniq, inv = np.unique(flat, axis=0, return_inverse=True)
    if len(uniq) > colors:
        q = img.quantize(colors=colors, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
        q.save(path, optimize=True)
        return len(q.getpalette()) // 3
    order = np.argsort(uniq[:, 3] != 0, kind="stable")                      # clear colours first (cosmetic)
    rank = np.empty(len(order), int)
    rank[order] = np.arange(len(order))
    idx = rank[inv.reshape(a.shape[:2])].astype(np.uint8)
    u = uniq[order]
    im = Image.fromarray(idx, "P")
    im.putpalette([int(v) for c in u for v in c[:3]])
    im.save(path, optimize=True, transparency=bytes(int(c[3]) for c in u))
    return len(u)


def half(img):
    """The glow sheet at half size: every 2 x 2 block becomes the brightest of its texels (so a one texel rune line survives), the alpha stays 255 where anything glows."""
    from PIL import Image
    a = np.array(img, dtype=np.int32)
    h, w = a.shape[0] // 2, a.shape[1] // 2
    b = a[:h * 2, :w * 2].reshape(h, 2, w, 2, 4).transpose(0, 2, 1, 3, 4).reshape(h, w, 4, 4)
    lum = b[..., :3].sum(axis=3) + (b[..., 3] > 0) * 1
    best = lum.argmax(axis=2)
    out = np.take_along_axis(b, best[..., None, None].repeat(4, axis=3), axis=2)[:, :, 0, :]
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def write_all(g, anim=True):
    from PIL import Image
    img, glow = g.finish()
    used = g.pk.height_used()
    hh = 64
    while hh < used:
        hh += 64
    img, glow = img.crop((0, 0, TEX, hh)), glow.crop((0, 0, TEX, hh))
    g.m.tex_h = hh
    if anim:
        clips(g.A)
        prune(g)
    g.m.save(g.A)
    tdir = os.path.join(ASSETS, "textures", "entity")
    os.makedirs(tdir, exist_ok=True)
    n1 = palettise(img, os.path.join(tdir, "anime_goliath.png"))
    n2 = palettise(half(glow), os.path.join(tdir, "anime_goliath_glow.png"))
    cubes = g.m.cube_count()
    quads = sum(len(c["uv"]) for bn in g.m.bones.values() for c in bn["cubes"])
    info = {"cubes": cubes, "quads": quads, "tex": [TEX, hh], "glow_tex": [TEX // 2, hh // 2], "colours": [n1, n2], "bones": len(g.m.bones)}
    sizes = {f: os.path.getsize(os.path.join(tdir, f)) for f in ("anime_goliath.png", "anime_goliath_glow.png")}
    info["bytes"] = sizes
    print("anime_goliath", info)
    return info


def prune(g):
    have = set(g.m.bones)
    for c in g.A.clips.values():
        for bn in [x for x in c["bones"] if x not in have]:
            del c["bones"][bn]


def clips(a):
    from gen_anime_goliath_clips import all_clips
    all_clips(a)


if __name__ == "__main__":
    if "--preview" in sys.argv or "--sheets" in sys.argv:
        from gen_anime_goliath_scenes import main as scenes_main
        sys.exit(scenes_main(sys.argv[1:]))
    gg = build()
    write_all(gg, anim=os.path.exists(os.path.join(HERE, "gen_anime_goliath_clips.py")))
    try:
        from gen_anime_goliath_scenes import write_scenes
        write_scenes()
    except ImportError:
        pass
