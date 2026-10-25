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

    def bone(self, name, parent=None, pivot=(0, 0, 0)):
        self.m.bone(name, parent, pivot)

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
    return Mat(fam, **kw)


# ------------------------------------------------------------------------------------------------------------------ the rig and the cubes
def build():
    g = G()
    Y_HIP, Y_PELV, Y_ABD, Y_CHEST, Y_YOKE, HEAD_Y = 38.0, 49.0, 59.0, 72.0, 77.0, 74.0
    SH_X = 36.0                                              # the shoulder / arm axis
    b = g.bone
    b("root")
    b("body", "root", (0, Y_HIP, 0))
    b("chest", "body", (0, 50, 0))
    b("head", "chest", (0, HEAD_Y, -3))
    b("crest", "head", (0, 95, 0))
    for suf, s in (("_l", 1), ("_r", -1)):
        b("pauld" + suf, "chest", (s * 33, 70, 0))
        b("moss" + suf, "pauld" + suf, (s * 33, 70, -14))
        b("arm" + suf, "chest", (s * SH_X, 70, 0))
        b("forearm" + suf, "arm" + suf, (s * SH_X, 50, 0))
        b("hand" + suf, "forearm" + suf, (s * SH_X, 31, 0))
        b("vine" + suf, "forearm" + suf, (s * (SH_X + 11), 50, 6))
        b("leg" + suf, "root", (s * 14.5, Y_HIP, 0))
        b("shin" + suf, "leg" + suf, (s * 14.5, 23, 0))
        b("foot" + suf, "shin" + suf, (s * 14.5, 8, 0))
    b("tasset_c", "body", (0, 46, -16))
    b("tasset_l", "body", (16, 46, -15))
    b("tasset_r", "body", (-16, 46, -15))
    b("tasset_b", "body", (0, 46, 15))
    b("ridge", "chest", (0, 72, 14))
    b("moss_back", "chest", (0, 78, 8))
    b("orbit", "root", (0, 78, 4))
    b("rubble", "root", (0, 0, 0))
    b("trail", "root", (0, 0, 22))

    # ---------------------------------------------------------------- feet and legs
    for s, suf in ((1, "_l"), (-1, "_r")):
        sx = lambda x0, w, s=s: x0 if s > 0 else -(x0 + w)       # the x origin of a +x cube, mirrored when s < 0
        bn = lambda base, suf=suf: base + suf

        def add(base, x0, y0, z0, w, h, d, mat, nm, **kw):
            g.slab(bn(base), sx(x0, w), y0, z0, w, h, d, mat, nm + suf, **kw)

        # foot: base slab, instep, heel and three stone toes
        add("foot", 4.5, 0, -16, 20, 6, 28, M("slate", moss=0.35, chip=0.5), "foot_base", no=())
        add("foot", 6.5, 6, -9, 16, 6, 19, M("slate", moss=0.5, drip=4, crack=0.6), "foot_top", no=("bottom",))
        add("foot", 5.5, 1, 10, 18, 9, 5, M("sand", crack=0.5), "heel", no=("bottom",))
        for k, (tx, tw, tl) in enumerate(((4.5, 6.0, 7.0), (11.0, 7.0, 10.0), (18.5, 6.0, 7.0))):
            add("foot", tx, 0, -16 - tl, tw, 5, tl, M("slate" if k != 1 else "sand", chip=0.6), "toe%d" % k, no=("bottom",) if False else ())
        # shin: two stacked greaves and the knee cap
        add("shin", 5, 8, -10, 19, 8, 20, M("slate", crack=0.6, hot=0.25), "greave_lo", no=("bottom",) if False else ())
        add("shin", 4, 15, -10, 21, 9, 20, M("sand", seam="slab", moss=0.3, drip=3, crack=0.4), "greave_hi", no=("bottom",))
        add("shin", 6.5, 21, -16, 16, 10, 7, M("sand", moss=0.7, drip=4, crack=0.5, decals=[{"face": "front", "kind": "runes", "lines": [[(0.5, 0.15), (0.5, 0.5), (0.3, 0.65), (0.3, 0.9)]]}]),
            "kneecap", no=("bottom",))
        # thigh
        add("leg", 4.5, 28, -10, 20, 14, 20, M("slate", seam="brick", crack=0.5, hot=0.3), "thigh", no=("bottom",))
        add("leg", 6, 30, -14, 17, 11, 4.5, M("sand", crack=0.5, chip=0.5), "thigh_plate", no=("bottom", "back"))
        add("leg", 24, 36, -9, 3.5, 10, 18, M("sand", chip=0.5, crack=0.4), "hip_plate", no=("bottom",))

    # ---------------------------------------------------------------- pelvis, belt, tassets
    g.slab("body", -23, 38, -13, 46, 12, 26, M("slate", seam="brick", crack=0.6, hot=0.2), "pelvis", no=("bottom",))
    g.slab("body", -25, 47, -15, 50, 6, 30, M("sand", crack=0.4, chip=0.5, decals=[{"face": "front", "kind": "bar", "box": (0.46, 0.15, 0.54, 0.85)}]), "belt", no=("bottom",))
    g.slab("body", -6, 45, -18, 12, 10, 3.5, M("slate", decals=[{"face": "front", "kind": "sigil", "r": 4, "cx": 0.5, "cy": 0.5}]), "buckle", no=("bottom", "back"), dens=3)
    g.slab("tasset_c", -9, 27, -19, 18, 20, 4, M("sand", crack=0.7, seam="slab", chip=0.5, decals=[{"face": "front", "kind": "runes", "lines": [[(0.5, 0.1), (0.5, 0.45), (0.28, 0.62), (0.28, 0.9)]]}]), "tasset_c",
           no=("bottom", "back", "top"))
    g.sym("tasset_l", "tasset_r", 10, 31, -18, 11, 16, 3.5, M("sand", crack=0.6, chip=0.5), "tasset", no=("bottom", "back", "top"))
    g.slab("tasset_b", -12, 28, 15, 24, 19, 3.5, M("sand", crack=0.5, chip=0.5), "tasset_back", no=("bottom", "front", "top"))

    # ---------------------------------------------------------------- torso
    g.slab("chest", -17, 47, -11, 34, 14, 22, M("slate", seam="brick", crack=0.7, hot=0.4), "abdomen", no=("bottom",))
    g.slab("chest", -25, 59, -13, 50, 14, 26, M("slate", moss=0.35, drip=5, crack=0.6, hot=0.5, chip=0.5), "ribcage", no=("bottom",))
    g.slab("chest", -20.5, 55, -18.5, 41, 19, 5.5, M("sand", crack=0.6, chip=0.5, decals=[{"face": "front", "kind": "sigil", "r": 14, "cx": 0.5, "cy": 0.52}]), "chest_plate",
           no=("bottom", "back"), dens={"front": 3})
    g.slab("chest", -14, 50, -15, 28, 7, 5, M("sand", crack=0.5, chip=0.4, seam="slab"), "chest_low", no=("bottom", "back"))
    g.slab("chest", -29, 71, -12, 58, 7, 24, M("slate", moss=0.6, drip=6, crack=0.4, chip=0.5), "yoke", no=("bottom",))
    g.slab("chest", -22, 56, 13, 44, 17, 3.5, M("sand", crack=0.7, chip=0.5, hot=0.0, decals=[{"face": "back", "kind": "sigil", "r": 8, "cx": 0.5, "cy": 0.5}]), "back_plate", no=("bottom", "front"), dens={"back": 3})
    for k, (w, h, y0) in enumerate(((11, 6, 52), (13, 7, 59), (11, 7, 67))):
        g.slab("ridge", -w / 2.0, y0 + 1, 15, w, h, 6 - k, M("slate", moss=0.3, crack=0.6, chip=0.6, hot=0.5), "ridge%d" % k, no=("bottom",))
    for k, (x, hh, rz) in enumerate(((0, 13, 0), (-5.5, 9, 18), (5.5, 9, -18))):
        g.card("ridge", x, 74 + (1 if k == 0 else -1), 17.5, 6, hh, K.spike_card, "ridge_sp%d" % k, ry=0, rz=rz, both=True, fam="sand" if k else "slate")

    # ---------------------------------------------------------------- the head: neck, jaw, muzzle, the eye slot, brow, crown, cranium, ear plates, crest
    g.slab("head", -7, 70, -8, 14, 8, 14, M("slate", flat=True), "neck", no=("top", "bottom"))
    g.slab("head", -7, HEAD_Y + 0, -12, 14, 4, 9, M("slate", crack=0.5, chip=0.5), "jaw", no=("bottom",))
    g.slab("head", -9, HEAD_Y + 4, -13, 18, 4, 11, M("sand", crack=0.5, chip=0.4, decals=[{"face": "front", "kind": "bar", "box": (0.2, 0.42, 0.8, 0.62)}]), "muzzle", no=("bottom",))
    g.pk.box(g.m, "head", (-8, HEAD_Y + 8, -11.5), (16, 3, 9), K.eye_painter("slot"), faces=("front", "left", "right", "top", "bottom"), dens={"front": 4, "left": 2, "right": 2, "top": 2, "bottom": 2})
    g.slab("head", -13, HEAD_Y + 11, -16, 26, 6, 15, M("sand", moss=0.5, drip=3, crack=0.5, chip=0.6), "brow", no=("back",))
    g.slab("head", -10, HEAD_Y + 17, -9, 20, 4, 18, M("slate", moss=0.5, crack=0.4, chip=0.5), "crown", no=("bottom",))
    g.slab("head", -10.5, HEAD_Y + 4, -3, 21, 13, 14, M("slate", crack=0.6, chip=0.5, moss=0.2, drip=3, hot=0.4), "cranium", no=("front",))
    g.sym("head", "head", 10, HEAD_Y + 5, -9, 3.5, 10, 10, M("sand", chip=0.6, crack=0.5), "ear")
    for k, (x, hh, rz, fam) in enumerate(((0, 11, 0, "slate"), (-4.5, 8, 22, "sand"), (4.5, 8, -22, "sand"), (-8, 6, 40, "slate"), (8, 6, -40, "slate"))):
        g.card("crest", x, 95.5 - (0 if k < 3 else 1.5), 0.0, 5, hh, K.spike_card, "crest%d" % k, ry=0, rz=rz, both=True, fam=fam)

    # ---------------------------------------------------------------- pauldrons
    for s, suf in ((1, "_l"), (-1, "_r")):
        sxx = lambda x0, w, s=s: x0 if s > 0 else -(x0 + w)
        P = "pauld" + suf

        def padd(x0, y0, z0, w, h, d, mat, nm, **kw):
            g.slab(P, sxx(x0, w), y0, z0, w, h, d, mat, nm + suf, **kw)
        padd(19, 61, -14, 27, 8, 28, M("sand", seam="slab", crack=0.6, chip=0.6, moss=0.25, drip=4), "pd1", no=("bottom",))
        padd(22, 69, -12, 22, 8, 24, M("sand", crack=0.6, chip=0.6, moss=0.5, drip=3), "pd2", no=("bottom",))
        padd(26, 76, -9, 15, 7, 18, M("slate", moss=0.75, crack=0.5, chip=0.6), "pd3", no=("bottom",))
        padd(44, 58, -11, 4, 21, 22, M("sand", crack=0.7, chip=0.7, hot=0.5), "pd_guard", no=("bottom",))
        for k, (x, hh, rz) in enumerate(((29, 11, 4), (34, 14, 0), (39, 9, -10))):
            g.card(P, s * x, 83, 0, 5, hh, K.spike_card, "pth%d%s" % (k, suf), rz=-rz * s, both=True, fam="slate")
        for k, x in enumerate((24.5, 30.5, 36.5, 41.5)):
            g.card("moss" + suf, s * x, 56 + (k % 2) * 1.5, -14.3, 6, 13 - (k % 2) * 3, K.tuft_card, "mt%d%s" % (k, suf))
    for k, x in enumerate((-18, -9, 0, 9, 18)):
        g.card("moss_back", x, 66 - (k % 2) * 3, 12.6, 9, 12 + (k % 2) * 4, K.tuft_card, "mb%d" % k)

    # ---------------------------------------------------------------- arms
    for s, suf in ((1, "_l"), (-1, "_r")):
        sxx = lambda x0, w, s=s: x0 if s > 0 else -(x0 + w)
        A_, F_, H_ = "arm" + suf, "forearm" + suf, "hand" + suf

        def aadd(bone, x0, y0, z0, w, h, d, mat, nm, **kw):
            g.slab(bone, sxx(x0, w), y0, z0, w, h, d, mat, nm + suf, **kw)
        aadd(A_, 28, 50, -9.5, 16, 22, 19, M("slate", seam="brick", crack=0.7, hot=0.45, moss=0.3, drip=4), "uarm", no=("bottom",))
        aadd(A_, 26.5, 59, -12, 19, 8, 24, M("sand", seam="slab", crack=0.6, chip=0.6), "uband", no=("bottom",))
        aadd(A_, 26.5, 49, -11, 19, 7, 22, M("slate", crack=0.6, chip=0.6), "elbow", no=("bottom",))
        aadd(F_, 28, 32, -9.5, 16, 19, 19, M("slate", crack=0.5), "farm", no=("bottom", "top"))
        aadd(F_, 24.5, 33, -13, 23, 17, 26, M("sand", seam="slab", crack=0.7, chip=0.6, moss=0.2, drip=3, hot=0.5,
                                              decals=[{"face": "front", "kind": "runes", "lines": [[(0.2, 0.15), (0.45, 0.15), (0.45, 0.5), (0.7, 0.5), (0.7, 0.85)]]}]), "bracer", no=("bottom",))
        aadd(F_, 24, 49, -13.5, 24, 4, 27, M("slate", crack=0.5, chip=0.6), "cuff_hi", no=("bottom",))
        aadd(F_, 24, 30, -13.5, 24, 4, 27, M("slate", crack=0.5, chip=0.6), "cuff_lo", no=("top",) if False else ())
        # fist: palm, back-of-hand plate, four finger blocks curled at the front, a thumb on the inner side
        aadd(H_, 25, 14, -12, 22, 17, 24, M("slate", seam="brick", crack=0.6, hot=0.35), "palm", no=("bottom",))
        aadd(H_, 27, 29, -3, 18, 4, 14, M("sand", crack=0.6, chip=0.6), "hand_plate", no=("bottom",))
        for k in range(4):
            aadd(H_, 25 + 5.6 * k, 9, -19, 5.2, 14 - (k % 2), 7.5, M("sand" if k % 2 else "slate", crack=0.5, chip=0.6), "finger%d" % k, no=())
        aadd(H_, 20.5, 17, -14, 5, 10, 13, M("slate", crack=0.5, chip=0.6), "thumb", no=())
        # vine: two hanging cards from the forearm's back edge
        g.card("vine" + suf, s * (SH_X + 11), 28, 8.5, 8, 28, K.vine_card, "vine_a" + suf, both=False)
        g.card("vine" + suf, s * (SH_X - 8), 30, 14.0, 8, 22, K.vine_card, "vine_b" + suf, ry=90 * s)

    # ---------------------------------------------------------------- the rubble at the feet and the trail behind
    rng = random.Random(7)
    spots = [(-30, -26, 5), (28, -30, 6), (-42, -4, 4), (44, -6, 5), (-26, 22, 4), (22, 26, 5), (0, -34, 4), (-17, -34, 3.5), (38, 12, 3.5), (-36, 14, 4.5)]
    for k, (x, z, sz) in enumerate(spots):
        h = sz * rng.uniform(0.55, 0.9)
        g.slab("rubble", x - sz / 2, 0, z - sz / 2, sz, h, sz * rng.uniform(0.8, 1.2), M("slate" if k % 3 else "sand", moss=0.5 if k % 2 else 0.0, chip=0.7, crack=0.2, ow=1), "rub%d" % k,
               no=("bottom",), rot=(0, rng.uniform(-35, 35), 0), piv=(x, 0, z))
    for k in range(7):
        sz = 6.5 - k * 0.7
        x = rng.uniform(-8, 8) + (k % 2) * 5 - 2
        z = 26 + k * 6.5
        g.slab("trail", x - sz / 2, 0, z - sz / 2 - 22, sz, sz * 0.7, sz, M("slate" if k % 2 else "sand", chip=0.7, crack=0.2, moss=0.4 if k % 3 == 0 else 0.0, ow=1), "trl%d" % k,
               no=("bottom",), rot=(0, rng.uniform(-40, 40), 0), piv=(x, 0, z - 22))
    # a few stones floating round the shoulders (the orbit bone turns slowly)
    for k, (ang, r, y, sz) in enumerate(((20, 36, 94, 6), (110, 38, 84, 5), (200, 35, 98, 7), (290, 39, 86, 5.5))):
        x, z = r * math.sin(math.radians(ang)), r * math.cos(math.radians(ang))
        g.slab("orbit", x - sz / 2, y - sz / 2, z - sz / 2, sz, sz * 0.8, sz, M("slate" if k % 2 else "sand", chip=0.9, crack=0.3, moss=0.5 if k == 0 else 0.0, ow=1), "orb%d" % k,
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
