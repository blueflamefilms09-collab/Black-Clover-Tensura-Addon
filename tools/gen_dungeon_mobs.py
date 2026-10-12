"""The two apex predators of the Living Dungeon's food web, built in the repo's GeoLite pipeline like the Tower Wizard:
a Direwolf (a huge, heavy, scarred dire wolf: blue-grey fur, a dark ridge of spiking mane, pale paws, lower fangs, amber eyes) and a Basilisk
(a low armoured lizard, overlapping green and black scales, a bone crest, a wedge head, venom-green eyes).

    python3 -B tools/gen_dungeon_mobs.py              # both
    python3 -B tools/gen_dungeon_mobs.py direwolf     # only the named ones

Writes (through tools/geo_builder.py; GeoSpec.of("dungeon", "direwolf") / GeoSpec.of("dungeon", "basilisk") name exactly these):
    assets/nusmp/geo/entity/dungeon_<name>.geo.json
    assets/nusmp/animations/entity/dungeon_<name>.animation.json
    assets/nusmp/textures/entity/dungeon_<name>.png          256x128, painted procedurally (fur strands / overlapping scales, shading, scars)
    assets/nusmp/textures/entity/dungeon_<name>_glow.png     the eyes (and the Basilisk's crest), additive and full bright

Clips   direwolf: idle, walk, run, attack, sleep, hurt        basilisk: idle, walk, attack, sleep, hurt
Drawn by client/dungeon/DirewolfRenderer.java and BasiliskRenderer.java with client.geo.GeoDraw (Space.PROP, front on -z).

Every cube has whole-number sizes so the painted texels stay square; the left side of a creature is painted once and the right side is a
mirrored twin of it (the same uv, mirror = true). Preview: python3 -B tools/render_preview/preview.py direwolf_walk basilisk_walk
(scenes in tools/render_preview/scenes.d/dungeon_mobs.json).
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim  # noqa: E402
from spirit_geo import Sheet, nz, fz, hsh, rgba, mix  # noqa: E402

S = math.sin
C = math.cos
TAU = 2 * math.pi
TEX_W, TEX_H = 256, 128


# ------------------------------------------------------------------------------------------------------------- small helpers
def A3(c):
    return np.array(c, np.float32)


def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def smooth(a, b, x):
    t = clamp((x - a) / (b - a)) if b != a else (1.0 if x >= a else 0.0)
    return t * t * (3 - 2 * t)


def ramp(cols, t):
    """Colour along a list of colours, t in 0..1."""
    t = clamp(t) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    a, b = A3(cols[i]), A3(cols[i + 1])
    return a * (1 - f) + b * f


def seg_dist(px, py, x0, y0, x1, y1):
    dx, dy = x1 - x0, y1 - y0
    d2 = dx * dx + dy * dy
    t = 0.0 if d2 == 0 else clamp(((px - x0) * dx + (py - y0) * dy) / d2)
    return math.hypot(px - (x0 + t * dx), py - (y0 + t * dy))


FACE_K = {"top": 0.8, "bottom": 0.7, "front": 0.96, "back": 0.9, "left": 1.0, "right": 1.0}


def axes(face, i, j, fw, fh, flow):
    """Strand / scale coordinates of a pixel: (across, along, n_across, n_along). flow 'z' = the long axis of the box is z (torso, tail, neck,
    muzzle): on the sides 'along' runs front -> back, on the top and bottom too (j = 0 is the BACK edge of those faces); flow 'y' = a limb:
    along is down the face. On the 'right' (-x) face i = 0 is the back, on the 'left' (+x) face i = 0 is the front, so both sides read alike."""
    if flow == "z":
        if face in ("left", "right"):
            return j, (i if face == "left" else fw - 1 - i), fh, fw
        if face in ("top", "bottom"):
            return i, fh - 1 - j, fw, fh
    return i, j, fw, fh


class Pack(Sheet):
    """Boxes are collected and painted at finish(), tallest first, so the sheet stays small. Twins reuse the uv of a painted box."""

    def __init__(self, w, h):
        super().__init__(w, h)
        self.q = []
        self.tw = []

    def box(self, m, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None, mirror=False):
        assert all(float(v).is_integer() for v in size), (bone, size)
        c = m.cube(bone, origin, tuple(int(v) for v in size), uv=(0, 0), inflate=inflate, rotation=rotation, pivot=pivot, mirror=mirror)
        self.q.append((c, tuple(int(v) for v in size), paint))
        return c

    def twin(self, m, bone, c):
        """The mirrored copy of a cube on the other side of the model (x -> -x), sharing its painted rectangle."""
        sx = c["size"][0]
        o = c["origin"]
        rot = c.get("rotation")
        piv = c.get("pivot")
        t = m.cube(bone, (-(o[0] + sx), o[1], o[2]), c["size"], uv=(0, 0), inflate=c.get("inflate", 0.0),
                   rotation=None if not rot else (rot[0], -rot[1], -rot[2]), pivot=None if not rot else (-piv[0], piv[1], piv[2]), mirror=True)
        self.tw.append((t, c))
        return t

    def finish(self):
        def key(k):
            c, (X, Y, Z), _ = self.q[k]
            return (-(Z + Y), -(2 * (X + Z)), k)
        for k in sorted(range(len(self.q)), key=key):
            c, (X, Y, Z), paint = self.q[k]
            u, v = self.alloc(2 * (X + Z), Z + Y)
            c["uv"] = [u, v]
            faces = {"top": (u + Z, v, X, Z), "bottom": (u + Z + X, v, X, Z), "right": (u, v + Z, Z, Y), "front": (u + Z, v + Z, X, Y),
                     "left": (u + Z + X, v + Z, Z, Y), "back": (u + 2 * Z + X, v + Z, X, Y)}
            for name, (fu, fv, fw, fh) in faces.items():
                for j in range(fh):
                    for i in range(fw):
                        cc, g = paint(name, i, j, fw, fh)
                        self._put(fu + i, fv + j, cc, g)
        for t, src in self.tw:
            t["uv"] = list(src["uv"])


class Rig:
    """Bones and boxes for a model whose left (+x) half is built once and mirrored: lbone() makes <n>_l and <n>_r, lbox() a cube and its twin."""

    def __init__(self, m, pk):
        self.m, self.pk = m, pk

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def box(self, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None):
        return self.pk.box(self.m, bone, origin, size, paint, inflate, rotation, pivot)

    def lbone(self, name, parent, pivot, rotation=None):
        """parent is the base name of a paired parent ('x' -> x_l / x_r) or a single bone when it starts with '!'."""
        for suf, sg in (("_l", 1), ("_r", -1)):
            par = parent[1:] if parent.startswith("!") else parent + suf
            rot = None if not rotation else (rotation[0], rotation[1] * sg, rotation[2] * sg)
            self.m.bone(name + suf, par, (pivot[0] * sg, pivot[1], pivot[2]), rot)

    def lbox(self, name, origin, size, paint, inflate=0.0, rotation=None, pivot=None):
        c = self.pk.box(self.m, name + "_l", origin, size, paint, inflate, rotation, pivot)
        self.pk.twin(self.m, name + "_r", c)
        return c

    def pbox(self, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None):
        """A cube on the +x side of a single bone and its mirrored twin on the -x side of the same bone."""
        c = self.pk.box(self.m, bone, origin, size, paint, inflate, rotation, pivot)
        self.pk.twin(self.m, bone, c)
        return c


# ------------------------------------------------------------------------------------------------------------- animation helpers
def cyc(a, clip, bone, kind, L, fn, n=1, steps=16):
    """A looping track: fn(theta) -> (x, y, z) sampled steps times over the clip length L, with n repeats of theta in 0..TAU."""
    frames = {}
    for k in range(steps + 1):
        frames[L * k / steps] = tuple(fn(TAU * n * k / steps))
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


def swing(base, amp, ph=0.0):
    """base + amp * sin(theta + ph) per axis (amp is a 3-tuple), for cyc()."""
    return lambda th: tuple(base[k] + amp[k] * S(th + ph) for k in range(3))


# =========================================================================================================================================
#                                                              THE DIREWOLF
# =========================================================================================================================================
FUR_D, FUR_M, FUR_L, FUR_H = (34, 42, 58), (68, 80, 102), (108, 124, 148), (150, 166, 186)
RIDGE_D, RIDGE_M, RIDGE_L = (20, 23, 33), (40, 46, 62), (84, 96, 118)
BELLY_D, BELLY_M, BELLY_L = (112, 120, 134), (156, 164, 176), (198, 204, 212)
MUZ_D, MUZ_M, MUZ_L = (104, 112, 126), (160, 166, 176), (204, 208, 214)
PAW_D, PAW_M, PAW_L = (122, 128, 140), (176, 180, 188), (214, 216, 220)
SCAR, SCAR_D = (190, 158, 154), (30, 28, 36)
NOSE, GUM, GUM_D, TOOTH, TOOTH_D = (22, 20, 26), (128, 52, 60), (82, 30, 40), (238, 230, 208), (176, 164, 140)
EYE_W, EYE_C = (255, 236, 150), (255, 176, 28)
CLAW_L, CLAW_D = (216, 206, 184), (110, 100, 88)

WOLF_RAMP = [FUR_D, FUR_M, FUR_L, FUR_H]
RIDGE_RAMP = [RIDGE_D, RIDGE_M, RIDGE_L]
BELLY_RAMP = [BELLY_D, BELLY_M, BELLY_L]
MUZ_RAMP = [MUZ_D, MUZ_M, MUZ_L]
PAW_RAMP = [PAW_D, PAW_M, PAW_L]


def strand_t(across, along, salt, wander=1.1):
    """Fur: strands that wander a little as they run, each with its own tone, shadows between them, lighter and darker bands along them."""
    x = across + wander * S(along * 0.29 + across * 0.8 + salt) + 0.5 * S(along * 0.77 + salt * 2.3)
    sid = math.floor(x)
    frac = x - sid
    h = hsh(sid, salt, 3)
    t = 0.3 + 0.44 * h + 0.1 * S(along * 0.45 + h * 9)
    if frac < 0.26:
        t -= 0.12                                                           # the shadow between two strands
    elif frac > 0.76:
        t += 0.06
    if hsh(sid, math.floor(along * 0.5), salt + 17) > 0.93:
        t += 0.2                                                            # a few pale guard hairs
    return t + nz(across * 0.3, along * 0.3, salt) * 0.3


def fur(salt, flow="z", cols=WOLF_RAMP, dark_top=0.0, belly=0.0, tip=0.0, scars=(), bottom=None, lift=0.0, paw_fade=0.0):
    """Wolf fur. dark_top: how far down the sides the dark saddle reaches (0..1); belly: how far up the sides the pale belly reaches; tip: frosted
    tips on the first rows (spikes); scars: ((face, i0, j0, i1, j1), ...) pale scars with matted fur beside them; bottom: ramp of the bottom face."""
    def p(face, i, j, fw, fh):
        a, b, na, nb = axes(face, i, j, fw, fh, flow)
        t = strand_t(a, b, salt) + lift
        v = j / max(1, fh - 1)
        if face in ("left", "right", "front", "back"):
            if dark_top > 0:
                t -= 0.27 * smooth(dark_top, 0.0, v)                          # darker on top, a saddle
            if tip > 0 and j < tip:
                t += 0.34 * (1 - j / tip)
        c = ramp(cols, t)
        if belly > 0 and face in ("left", "right", "front", "back"):
            w = smooth(1.0 - belly, 1.0 - belly * 0.45, v + 0.2 * (nz(i * 0.5, j * 0.5, salt + 5)))
            c = c * (1 - w) + ramp(BELLY_RAMP, 0.15 + 0.7 * t * 0.9 + 0.1) * w
        if paw_fade > 0 and face in ("left", "right", "front", "back"):
            w = smooth(1.0 - paw_fade, 1.0, v + 0.15 * nz(i * 0.6, j * 0.6, salt + 9))
            c = c * (1 - w) + ramp(PAW_RAMP, 0.1 + 0.8 * t) * w                      # the legs pale towards the paws
        if face == "bottom":
            c = ramp(bottom if bottom else BELLY_RAMP, 0.25 + 0.5 * t)
        c = c * FACE_K[face] * (1 - 0.1 * (v if face in ("left", "right", "front", "back") else 0))
        for (sf, i0, j0, i1, j1) in scars:
            if sf == face:
                d = seg_dist(i, j, i0, j0, i1, j1)
                if d < 0.55:
                    c = A3(SCAR) * (0.88 + 0.3 * fz(i, j, salt))
                elif d < 1.35:
                    c = c * 0.62                                                # the fur parted and matted beside it
        return rgba(c), None
    return p


def spike(salt, cols=RIDGE_RAMP, tip=3):
    """A spike of ridge fur: dark at the root, frosted at the tip."""
    return fur(salt, "y", cols, tip=tip, lift=-0.05)


def ear_paint(salt, inner=True):
    def p(face, i, j, fw, fh):
        base = fur(salt, "y", WOLF_RAMP, tip=2, lift=-0.1)(face, i, j, fw, fh)[0]
        if face == "front" and inner and 0 < i < fw - 1 and j > 0:
            c = ramp([(96, 80, 90), (150, 128, 134), (196, 176, 176)], 0.35 + 0.5 * hsh(i, j, salt) + 0.25 * (j / max(1, fh)))
            return rgba(c), None
        if face == "front" and inner:
            return rgba(A3(base[:3]) * 0.9), None
        return base, None
    return p


def eye_paint(face, i, j, fw, fh):
    if face == "front":
        k = 0.55 + 0.45 * (1 - abs(i - (fw - 1) / 2.0) / max(0.5, fw / 2.0))
        c = A3(EYE_C) * (1 - 0.25 * (1 - k)) + A3(EYE_W) * 0.35 * k
        return rgba(c), rgba(EYE_C, 255)
    return rgba(A3(EYE_C) * 0.8), rgba(EYE_C, 200)


def brow_paint(face, i, j, fw, fh):
    c = ramp([RIDGE_D, RIDGE_M, FUR_M], 0.25 + 0.5 * hsh(i, j, 77))
    return rgba(c * (1.0 if face != "bottom" else 0.6)), None


def nose_paint(face, i, j, fw, fh):
    c = A3(NOSE) + fz(i, j, 41) * 14
    if face in ("front", "top") and j == 0 and 0 < i < fw - 1:
        c = c + 46                                                           # a wet highlight
    return rgba(c), None


def tooth_paint(salt=0):
    def p(face, i, j, fw, fh):
        t = 0.35 + 0.55 * (1 - j / max(1, fh - 1)) + 0.1 * fz(i, j, salt)
        return rgba(ramp([TOOTH_D, TOOTH, (252, 248, 236)], t)), None
    return p


def claw_paint(face, i, j, fw, fh):
    t = 0.2 + 0.7 * (1 - j / max(1, fh - 1)) + 0.15 * fz(i, j, 9)
    return rgba(ramp([CLAW_D, CLAW_L, (240, 232, 214)], t)), None


def skull_paint(salt, scars=()):
    """The broad skull: fur, a dark forehead stripe, shadow under the brow, closed-lid slits on the front where the eyes are."""
    base = fur(salt, "z", WOLF_RAMP, dark_top=0.5, belly=0.28, scars=scars)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face == "top" and abs(i - (fw - 1) / 2.0) < 1.6:
            c = rgba(A3(c[:3]) * 0.66)                                         # the dark forehead stripe
        if face == "front":
            ex = i in (0, 1, fw - 2, fw - 1)
            if ex and j in (2, 3, 4):
                d = 0.5 if j == 3 else 0.8
                c = rgba(A3(c[:3]) * d)                                        # the eye socket in shadow
        return c, g
    return p


def muzzle_paint(salt, scars=()):
    base = fur(salt, "z", MUZ_RAMP, dark_top=0.0, belly=0.0, scars=scars, bottom=[GUM_D, GUM, GUM])

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face in ("left", "right"):
            a, b, na, nb = axes(face, i, j, fw, fh, "z")
            if j == 2 and a in (1, 3, 5) and a < na - 1:
                c = rgba(A3(c[:3]) * 0.45)                                     # whisker roots
            if j >= fh - 1:
                c = rgba(A3(c[:3]) * 0.75)                                     # the lip line
        if face == "front":
            if j == fh - 1 or (i == fw // 2 and j < fh - 1):
                c = rgba(A3(c[:3]) * 0.4)                                      # the mouth line and the philtrum
        if face == "top":
            t = 1 - abs(i - (fw - 1) / 2.0) / (fw / 2.0)
            c = rgba(A3(c[:3]) * (0.78 + 0.1 * t))                             # the bridge is a little darker
        if face == "bottom":
            a, b, na, nb = axes(face, i, j, fw, fh, "z")
            if a % 2 == 0 and (i == 0 or i == fw - 1):
                return rgba(TOOTH), None                                       # a row of teeth along both edges of the palate
            if i in (0, fw - 1):
                return rgba(GUM), None
        return c, g
    return p


def jaw_paint(salt):
    base = fur(salt, "z", MUZ_RAMP, dark_top=0.0, belly=0.0, bottom=[MUZ_D, MUZ_M, MUZ_L], lift=0.05)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face == "top":
            a, b, na, nb = axes(face, i, j, fw, fh, "z")
            if i in (0, fw - 1) and a % 2 == 1:
                return rgba(TOOTH), None
            return rgba(A3(GUM) * (0.9 + 0.15 * fz(i, j, salt)) * (0.7 if i in (1, fw - 2) else 1.0)), None
        return c, g
    return p


# ------------------------------------------------------------------------------------------------------------- the model
WOLF_SCARS_HEAD = (("left", 2, 1, 8, 5), ("right", 1, 4, 6, 7))
WOLF_SCARS_L = (("left", 3, 1, 9, 7), ("left", 4, 0, 10, 6), ("left", 6, 1, 11, 6))
WOLF_SCARS_R = (("right", 2, 2, 7, 8), ("right", 1, 4, 5, 8))


def build_direwolf():
    m = Model("dungeon", "direwolf", TEX_W, TEX_H)
    pk = Pack(TEX_W, TEX_H)
    r = Rig(m, pk)
    A = Anim()
    HZ = 8                                                                # the z of the hip joints (the hips sit between 3 and 12)
    LF = 1.5                                                              # everything above the legs stands this much higher (long, rangy legs)

    def Y(v):
        return v + LF

    # the hind leg is a zigzag: thigh forward 30, shin back 25 from the vertical, foot straight; its foot is as long as the hock is high
    hip_y, L1, L2 = Y(15.5), 8.0, 6.5
    knee_y, hock_y = hip_y - L1, hip_y - L1 - L2
    hock_h = hip_y - L1 * C(math.radians(30)) - L2 * C(math.radians(25))

    # bones --------------------------------------------------------------------------------------------------------
    r.bone("root", None, (0, 0, 0))
    r.bone("body", "root", (0, Y(14.5), 5))                               # hips and waist; the hind legs and the tail hang from it
    r.bone("chest", "body", (0, Y(14.5), -6))                             # the shoulders; the front legs, the neck and the head hang from it
    r.bone("ridge_f", "chest", (0, Y(17.5), -6))
    r.bone("ridge_b", "body", (0, Y(17), 5))
    r.bone("neck", "chest", (0, Y(15.5), -13))
    r.bone("head", "neck", (0, Y(16), -20))
    r.bone("jaw", "head", (0, Y(12.5), -23))
    r.lbone("ear", "!head", (4, Y(20), -22))
    r.lbone("eye", "!head", (4.5, Y(17), -27.5))
    # legs: the front ones are plain columns, the hind ones are bent (thigh forward, shin back, foot forward) by their rest rotation
    r.lbone("fa", "!chest", (5, Y(14), -9))
    r.lbone("fb", "fa", (5, 8.5, -9))
    r.lbone("fp", "fb", (5, 3.5, -9))
    r.lbone("ha", "!body", (5, hip_y, HZ), (30, 0, 0))
    r.lbone("hb", "ha", (5, knee_y, HZ), (-55, 0, 0))
    r.lbone("hf", "hb", (5, hock_y, HZ), (25, 0, 0))
    tail = ((Y(16), 12, 26), (Y(16), 17, 16), (Y(16), 22, 12), (Y(16), 26, 8))
    prev = "body"
    for n, (y, z, rx) in enumerate(tail):
        r.bone("tail_%d" % (n + 1), prev, (0, y, z), (rx, 0, 0))
        prev = "tail_%d" % (n + 1)

    # torso ----------------------------------------------------------------------------------------------------------
    r.box("body", (-5.5, Y(11.5), 3), (11, 6, 9), fur(1, "z", dark_top=0.55, belly=0.4, scars=(("left", 1, 2, 7, 5),)), 0.2)      # hips
    r.box("body", (-4.5, Y(12), -3), (9, 5, 6), fur(2, "z", dark_top=0.55, belly=0.45), 0.2)                                       # the waist, tucked up
    r.box("chest", (-6.5, Y(11), -14), (13, 7, 11), fur(3, "z", dark_top=0.5, belly=0.38, scars=WOLF_SCARS_L + WOLF_SCARS_R), 0.2)  # the deep chest
    r.box("chest", (-6, Y(10.5), -13), (12, 2, 10), fur(4, "z", cols=BELLY_RAMP), 0.3)                                             # pale chest and belly fluff
    r.box("chest", (-3.5, Y(8.5), -16), (7, 5, 3), fur(6, "y", cols=BELLY_RAMP, tip=2), 0.2)                                       # the pale bib on the breast
    r.pbox("chest", (5, Y(14.5), -13), (3, 4, 7), fur(5, "z", dark_top=0.7), 0.1)                                                  # shoulder muscle
    # the ridge: a dark mane of spikes along the spine, tallest over the shoulders, swept back
    ridge = ((-13, 4, 4), (-10.5, 4, 4), (-8, 4, 3), (-5.5, 3, 3), (-3, 3, 3))
    for n, (z, h, w) in enumerate(ridge):
        r.box("ridge_f", (-w / 2, Y(17.5), z), (w, h, 3), spike(10 + n), 0.0, (22 + 3 * n, 0, 0), (0, Y(17.5), z + 1.5))
    for n, z in enumerate((0.0, 3.0, 6.0, 9.0)):
        r.box("ridge_b", (-1.5, Y(17), z), (3, 3 if n < 3 else 2, 3), spike(20 + n), 0.0, (26, 0, 0), (0, Y(17), z + 1.5))

    # neck, ruff and head ---------------------------------------------------------------------------------------------
    r.box("neck", (-4.5, Y(12), -21), (9, 8, 9), fur(40, "z", dark_top=0.55, belly=0.35, scars=(("left", 1, 1, 6, 5),)), 0.3)
    r.box("neck", (-6, Y(11), -19), (12, 9, 3), fur(41, "y", dark_top=0.6, belly=0.3, tip=2), 0.3)                                 # the ruff round the neck
    r.box("neck", (-3, Y(20), -20), (6, 2, 7), spike(42, tip=0), 0.0)                                                              # the mane on the neck
    r.box("head", (-5.5, Y(12), -27), (11, 8, 8), skull_paint(50, WOLF_SCARS_HEAD), 0.2)                                           # the broad skull
    r.box("head", (-3.5, Y(12), -35), (7, 5, 8), muzzle_paint(51, (("left", 1, 0, 6, 4),)), 0.1)                                   # the heavy muzzle
    r.box("head", (-2, Y(15), -36), (4, 2, 1), nose_paint, 0.1)
    r.box("jaw", (-3.5, Y(10), -34), (7, 2, 11), jaw_paint(52), 0.1)
    r.pbox("head", (3.5, Y(12.5), -31), (1, 3, 4), muzzle_paint(53), 0.0)                                                          # the loose upper lips
    r.pbox("jaw", (3.5, Y(11), -33.5), (1, 3, 1), tooth_paint(1), 0.0)                                                             # the lower fangs, up past the lip
    r.pbox("head", (2, Y(10), -33.5), (1, 2, 1), tooth_paint(2), 0.0)                                                              # the upper fangs
    r.pbox("head", (5.5, Y(12), -26), (2, 4, 5), fur(54, "z", dark_top=0.4), 0.1, (0, 14, 0), (6, Y(14), -24))                     # cheek ruffs
    r.pbox("head", (5.5, Y(10), -24), (2, 3, 4), fur(55, "z", dark_top=0.2), 0.0, (0, 20, 8), (6, Y(12), -22))
    r.pbox("head", (5.5, Y(14.5), -23), (2, 3, 3), fur(56, "z", dark_top=0.3), 0.0, (0, 26, -10), (6, Y(15), -22))
    r.pbox("head", (2, Y(18.9), -28), (4, 1, 1), brow_paint, 0.0, (0, 0, 14), (4, Y(19.4), -27.5))                                 # the angry brow
    r.lbox("eye", (3.5, Y(16.5), -28), (2, 1, 1), eye_paint, 0.0, (0, 0, 12), (4.5, Y(17), -27.5))                                 # the glowing eyes
    # ears: pointed and upright; the -x one is torn (notched)
    r.box("ear_l", (2.5, Y(20), -23), (3, 2, 2), ear_paint(60), 0.0)
    r.box("ear_l", (3, Y(22), -22.5), (2, 2, 1), ear_paint(61), 0.0)
    r.box("ear_l", (3.5, Y(24), -22.5), (1, 1, 1), ear_paint(62, False), 0.0)
    r.box("ear_r", (-5.5, Y(20), -23), (3, 2, 2), ear_paint(63), 0.0)
    r.box("ear_r", (-5.5, Y(22), -22.5), (1, 3, 1), ear_paint(64, False), 0.0)                                                     # the outer tine stands ...
    r.box("ear_r", (-3.5, Y(22), -22.5), (1, 1, 1), ear_paint(65, False), 0.0)                                                     # ... the inner one is torn short

    # legs -----------------------------------------------------------------------------------------------------------
    paw = fur(70, "y", PAW_RAMP)
    r.lbox("fa", (2.5, 7.5, -12), (5, 8, 6), fur(71, "y", dark_top=0.4), 0.3)
    r.lbox("fb", (3, 3, -11.5), (4, 5, 5), fur(73, "y", dark_top=0.0, paw_fade=0.7, lift=0.0), 0.2)
    r.lbox("fp", (2.5, 0, -13.5), (5, 4, 7), paw, 0.1)
    for k in range(3):
        r.lbox("fp", (2.9 + k * 1.5, 0, -14.5), (1, 1, 1), claw_paint, 0.0)
    r.lbox("ha", (2, knee_y - 0.5, HZ - 3), (6, 9, 7), fur(80, "y", dark_top=0.45, scars=(("left", 0, 1, 3, 6),)), 0.3)
    r.lbox("hb", (3, hock_y - 0.5, HZ - 2), (4, 7, 4), fur(81, "y", paw_fade=0.6), 0.2)
    r.lbox("hf", (3, hock_y - hock_h, HZ - 1.5), (4, 4, 3), fur(82, "y", PAW_RAMP, lift=-0.05), 0.1)
    r.lbox("hf", (2.5, hock_y - hock_h, HZ - 4), (5, 2, 7), paw, 0.1)
    for k in range(3):
        r.lbox("hf", (2.9 + k * 1.5, hock_y - hock_h, HZ - 5), (1, 1, 1), claw_paint, 0.0)

    # tail: bushy, low-hung ---------------------------------------------------------------------------------------------
    for n, (w, h, d) in enumerate(((4, 4, 5), (5, 5, 5), (5, 5, 4), (4, 4, 4))):
        z0 = tail[n][1]
        r.box("tail_%d" % (n + 1), (-w / 2, Y(16) - h / 2, z0), (w, h, d), fur(90 + n, "z", dark_top=0.4, lift=0.0 if n < 3 else -0.12), 0.35 + 0.1 * (n in (1, 2)))
    r.box("tail_4", (-1.5, Y(14.5), 29), (3, 3, 3), fur(95, "z", RIDGE_RAMP, lift=0.1), 0.1)

    wolf_anims(A)
    return m, pk, A


# ------------------------------------------------------------------------------------------------------------- the Direwolf's clips
def gait(a, clip, L, ph_f, ph_h, side, up_f=24.0, up_h=22.0, flex_f=42.0, flex_h=34.0, n=1):
    """One side's legs for a walk or a run: the upper bones swing as cos, the lower bones fold while the leg swings forward (sin < 0)."""
    fn_up = lambda amp, ph: (lambda th: (amp * C(th + ph), 0, 0))
    lift = lambda ph: (lambda th: max(0.0, -S(th + ph)) ** 0.85)
    cyc(a, clip, "fa_" + side, "rot", L, fn_up(up_f, ph_f), n)
    cyc(a, clip, "fb_" + side, "rot", L, lambda th: (-flex_f * lift(ph_f)(th), 0, 0), n)
    cyc(a, clip, "fp_" + side, "rot", L, lambda th: (16 * lift(ph_f)(th) - 5 * S(th + ph_f), 0, 0), n)
    cyc(a, clip, "ha_" + side, "rot", L, fn_up(up_h, ph_h), n)
    cyc(a, clip, "hb_" + side, "rot", L, lambda th: (-flex_h * lift(ph_h + 0.4)(th), 0, 0), n)
    cyc(a, clip, "hf_" + side, "rot", L, lambda th: (22 * lift(ph_h + 0.4)(th) - 8 * S(th + ph_h), 0, 0), n)


def wolf_tail(a, clip, L, amps, n=1, lag=0.55, base=(0, 0, 0), rx=(0, 0, 0, 0)):
    for k in range(4):
        cyc(a, clip, "tail_%d" % (k + 1), "rot", L, lambda th, k=k: (rx[k] * S(th * 2 + k), amps[k] * S(th - lag * k), 0), n)


def wolf_anims(A):
    # ---------------------------------------------------------------- idle: breathing, a slow look about, an ear twitch, a swaying tail
    L = 4.0
    A.clip("idle", L, True)
    cyc(A, "idle", "chest", "scale", L, lambda th: (1, 1 + 0.02 * S(th), 1 + 0.028 * S(th)), 2)
    cyc(A, "idle", "body", "pos", L, lambda th: (0, 0.22 * S(th), 0), 2)
    cyc(A, "idle", "ridge_f", "rot", L, lambda th: (1.6 * S(th + 0.4), 0, 0), 2)
    cyc(A, "idle", "ridge_b", "rot", L, lambda th: (1.6 * S(th + 0.9), 0, 0), 2)
    cyc(A, "idle", "neck", "rot", L, lambda th: (1.5 * S(2 * th + 0.5), 3.0 * S(th + 0.6), 0), 1)
    cyc(A, "idle", "head", "rot", L, lambda th: (2.0 * S(2 * th + 1.0), 7.0 * S(th + 1.4), 1.5 * S(th)), 1)
    cyc(A, "idle", "jaw", "rot", L, lambda th: (-1.8 - 1.6 * S(th), 0, 0), 2)
    A.rot("idle", "ear_l", {0.0: (0, 0, 0), 1.4: (0, 0, 0), 1.5: (8, 0, -16), 1.62: (0, 0, 0), 1.74: (6, 0, -10), 1.86: (0, 0, 0), 4.0: (0, 0, 0)})
    A.rot("idle", "ear_r", {0.0: (0, 0, 0), 2.8: (0, 0, 0), 2.9: (8, 0, 16), 3.02: (0, 0, 0), 3.14: (6, 0, 10), 3.26: (0, 0, 0), 4.0: (0, 0, 0)})
    wolf_tail(A, "idle", L, (7, 9, 11, 13), 1, rx=(1.0, 1.5, 2.0, 2.5))
    for sd, ph in (("l", 0.0), ("r", 1.6)):
        cyc(A, "idle", "fa_" + sd, "rot", L, lambda th, ph=ph: (0.8 * S(th + ph), 0, 0), 1)

    # ---------------------------------------------------------------- walk: a diagonal trot, the head bobbing, the spine rolling
    L = 1.0
    A.clip("walk", L, True)
    gait(A, "walk", L, 0.0, 0.5, "l")
    gait(A, "walk", L, math.pi, math.pi + 0.5, "r")
    cyc(A, "walk", "body", "pos", L, lambda th: (0, 0.5 * C(2 * th + 0.5), 0), 1)
    cyc(A, "walk", "body", "rot", L, lambda th: (0.8 * S(2 * th), 2.0 * S(th), 0), 1)
    cyc(A, "walk", "chest", "rot", L, lambda th: (-1.2 * S(2 * th + 0.4), -2.4 * S(th), 0), 1)
    cyc(A, "walk", "neck", "rot", L, lambda th: (2.5 * S(2 * th + 1.6), 1.5 * S(th), 0), 1)
    cyc(A, "walk", "head", "rot", L, lambda th: (-4.0 * S(2 * th + 2.0), 2.0 * S(th + 1.0), 0), 1)
    cyc(A, "walk", "jaw", "rot", L, lambda th: (-3.0 - 1.5 * S(2 * th), 0, 0), 1)
    cyc(A, "walk", "ridge_f", "rot", L, lambda th: (3.0 * S(2 * th + 0.8), 0, 0), 1)
    cyc(A, "walk", "ridge_b", "rot", L, lambda th: (3.0 * S(2 * th + 1.3), 0, 0), 1)
    cyc(A, "walk", "ear_l", "rot", L, lambda th: (3.0 * S(2 * th + 0.6), 0, -3.0 * S(th)), 1)
    cyc(A, "walk", "ear_r", "rot", L, lambda th: (3.0 * S(2 * th + 0.9), 0, -3.0 * S(th + 1.0)), 1)
    wolf_tail(A, "walk", L, (8, 11, 14, 17), 1, lag=0.7, rx=(1.5, 2.0, 2.5, 3.0))

    # ---------------------------------------------------------------- run: a bound, the spine flexing, the mouth open, the ears pinned back
    L = 0.55
    A.clip("run", L, True)
    for sd, d in (("l", 0.0), ("r", 0.45)):
        gait(A, "run", L, d, d + 2.7, sd, 42.0, 38.0, 56.0, 46.0)
    cyc(A, "run", "body", "pos", L, lambda th: (0, 1.6 * C(th + 0.3), 0), 1)
    cyc(A, "run", "body", "rot", L, lambda th: (7.0 * S(th + 0.3), 0, 0), 1)
    cyc(A, "run", "chest", "rot", L, lambda th: (-9.0 * S(th + 0.3), 0, 0), 1)
    cyc(A, "run", "neck", "rot", L, lambda th: (-12 + 3.0 * S(th + 1.0), 0, 0), 1)
    cyc(A, "run", "head", "rot", L, lambda th: (4 + 3.0 * S(th + 1.6), 0, 0), 1)
    cyc(A, "run", "jaw", "rot", L, lambda th: (-16 - 6.0 * S(th + 0.4), 0, 0), 1)
    cyc(A, "run", "ridge_f", "rot", L, lambda th: (-10 + 4.0 * S(th + 1.0), 0, 0), 1)
    cyc(A, "run", "ridge_b", "rot", L, lambda th: (-12 + 4.0 * S(th + 1.5), 0, 0), 1)
    cyc(A, "run", "ear_l", "rot", L, lambda th: (28 + 4.0 * S(th), 0, 6.0), 1)
    cyc(A, "run", "ear_r", "rot", L, lambda th: (28 + 4.0 * S(th + 0.4), 0, -6.0), 1)
    for k, (b, am) in enumerate(((-22, 4), (-14, 6), (-10, 8), (-8, 10))):
        cyc(A, "run", "tail_%d" % (k + 1), "rot", L, lambda th, b=b, am=am, k=k: (b + 3.0 * S(th - 0.5 * k), am * S(th - 0.8 * k), 0), 1)

    # ---------------------------------------------------------------- attack: a crouch, a lunge with the jaws wide, a snap, a recover (loops while it fights)
    L = 0.9
    A.clip("attack", L, True)
    kf = lambda *pts: {t: v for t, v in pts}
    A.pos("attack", "body", kf((0.0, (0, 0, 0)), (0.18, (0, -2.2, 2.0)), (0.34, (0, 1.2, -6.5)), (0.5, (0, 0.4, -8.5)), (0.66, (0, -0.6, -3.0)), (0.9, (0, 0, 0))))
    A.rot("attack", "body", kf((0.0, (0, 0, 0)), (0.18, (-4, 0, 0)), (0.34, (-7, 0, 0)), (0.5, (-4, 0, 0)), (0.66, (-1, 0, 0)), (0.9, (0, 0, 0))))
    A.rot("attack", "chest", kf((0.0, (0, 0, 0)), (0.18, (-9, 0, 0)), (0.34, (10, 0, 0)), (0.5, (5, 0, 0)), (0.66, (-2, 0, 0)), (0.9, (0, 0, 0))))
    A.rot("attack", "neck", kf((0.0, (0, 0, 0)), (0.18, (-10, 0, 0)), (0.34, (14, 0, 0)), (0.5, (-6, 0, 0)), (0.66, (0, 0, 0)), (0.9, (0, 0, 0))))
    A.rot("attack", "head", kf((0.0, (0, 0, 0)), (0.18, (-6, 0, 0)), (0.34, (18, 0, 0)), (0.46, (10, 0, 0)), (0.56, (-8, 0, 0)), (0.9, (0, 0, 0))))
    A.rot("attack", "jaw", kf((0.0, (0, 0, 0)), (0.18, (-10, 0, 0)), (0.3, (-52, 0, 0)), (0.44, (-56, 0, 0)), (0.52, (-2, 0, 0)), (0.66, (-8, 0, 0)), (0.9, (0, 0, 0))))
    A.rot("attack", "ridge_f", kf((0.0, (0, 0, 0)), (0.18, (-18, 0, 0)), (0.5, (-22, 0, 0)), (0.9, (0, 0, 0))))
    A.rot("attack", "ridge_b", kf((0.0, (0, 0, 0)), (0.18, (-18, 0, 0)), (0.5, (-22, 0, 0)), (0.9, (0, 0, 0))))
    A.scale("attack", "ridge_f", kf((0.0, (1, 1, 1)), (0.18, (1, 1.35, 1)), (0.5, (1, 1.45, 1)), (0.9, (1, 1, 1))))
    A.scale("attack", "ridge_b", kf((0.0, (1, 1, 1)), (0.18, (1, 1.35, 1)), (0.5, (1, 1.45, 1)), (0.9, (1, 1, 1))))
    for sd, ph in (("l", 0.0), ("r", 0.05)):
        A.rot("attack", "fa_" + sd, kf((0.0, (0, 0, 0)), (0.18, (-14, 0, 0)), (0.34, (52 + 40 * ph, 0, 0)), (0.5, (62, 0, 0)), (0.66, (14, 0, 0)), (0.9, (0, 0, 0))))
        A.rot("attack", "fb_" + sd, kf((0.0, (0, 0, 0)), (0.18, (20, 0, 0)), (0.34, (-14, 0, 0)), (0.5, (-22, 0, 0)), (0.9, (0, 0, 0))))
        A.rot("attack", "fp_" + sd, kf((0.0, (0, 0, 0)), (0.34, (-18, 0, 0)), (0.5, (-26, 0, 0)), (0.9, (0, 0, 0))))
        A.rot("attack", "ha_" + sd, kf((0.0, (0, 0, 0)), (0.18, (22, 0, 0)), (0.34, (-34, 0, 0)), (0.5, (-26, 0, 0)), (0.66, (-6, 0, 0)), (0.9, (0, 0, 0))))
        A.rot("attack", "hb_" + sd, kf((0.0, (0, 0, 0)), (0.18, (-22, 0, 0)), (0.34, (18, 0, 0)), (0.5, (14, 0, 0)), (0.9, (0, 0, 0))))
        A.rot("attack", "hf_" + sd, kf((0.0, (0, 0, 0)), (0.18, (12, 0, 0)), (0.34, (-26, 0, 0)), (0.5, (-18, 0, 0)), (0.9, (0, 0, 0))))
    for sd, s in (("l", 1), ("r", -1)):
        A.rot("attack", "ear_" + sd, kf((0.0, (0, 0, 0)), (0.18, (26, 0, 10 * s)), (0.5, (30, 0, 10 * s)), (0.9, (0, 0, 0))))
    for k, (a0, a1) in enumerate(((-24, 8), (-16, 12), (-10, 14), (-8, 16))):
        A.rot("attack", "tail_%d" % (k + 1), kf((0.0, (0, 0, 0)), (0.18, (a0, 0, 0)), (0.34, (a0 - 6, a1, 0)), (0.5, (a0 - 4, -a1, 0)), (0.9, (0, 0, 0))))

    # ---------------------------------------------------------------- sleep: lying curled on its belly, the nose in the flank, the tail wrapped round it, slow breaths
    L = 4.0
    A.clip("sleep", L, True)
    A.pos("sleep", "body", {0.0: (0, -12.5, 0), 4.0: (0, -12.5, 0)})
    cyc(A, "sleep", "chest", "scale", L, lambda th: (1, 1 + 0.035 * S(th), 1 + 0.045 * S(th)), 1)
    cyc(A, "sleep", "body", "scale", L, lambda th: (1, 1 + 0.02 * S(th + 0.3), 1 + 0.02 * S(th + 0.3)), 1)
    A.rot("sleep", "chest", {0.0: (0, -16, 0), 4.0: (0, -16, 0)})
    A.rot("sleep", "neck", {0.0: (-6, -30, 0), 4.0: (-6, -30, 0)})
    A.rot("sleep", "head", {0.0: (-14, -36, 8), 4.0: (-14, -36, 8)})
    A.rot("sleep", "jaw", {0.0: (-1, 0, 0), 4.0: (-1, 0, 0)})
    for sd, s in (("l", 1), ("r", -1)):
        A.pos("sleep", "eye_" + sd, {0.0: (0, 0, 2.5), 4.0: (0, 0, 2.5)})                  # the eyes sink into the skull: shut
        A.rot("sleep", "ear_" + sd, {0.0: (-24, 0, 30 * s), 4.0: (-24, 0, 30 * s)})
        A.rot("sleep", "fa_" + sd, {0.0: (88, 0, 0), 4.0: (88, 0, 0)})                      # the forelegs lie forward, the paws flat
        A.rot("sleep", "fb_" + sd, {0.0: (0, 0, 0), 4.0: (0, 0, 0)})
        A.rot("sleep", "fp_" + sd, {0.0: (-85, 0, 0), 4.0: (-85, 0, 0)})
        A.rot("sleep", "ha_" + sd, {0.0: (60, 0, 0), 4.0: (60, 0, 0)})                      # the hind legs fold under the haunches
        A.rot("sleep", "hb_" + sd, {0.0: (-125, 0, 0), 4.0: (-125, 0, 0)})
        A.rot("sleep", "hf_" + sd, {0.0: (65, 0, 0), 4.0: (65, 0, 0)})
    for k, (rx, ry) in enumerate(((-26, 62), (-16, 52), (-12, 46), (-8, 40))):
        cyc(A, "sleep", "tail_%d" % (k + 1), "rot", L, lambda th, rx=rx, ry=ry, k=k: (rx, ry + 1.2 * S(th - 0.5 * k), 0), 1)
    A.pos("sleep", "tail_1", {0.0: (0, -2.5, 0), 4.0: (0, -2.5, 0)})

    # ---------------------------------------------------------------- hurt: a flinch (plays once)
    A.clip("hurt", 0.4, False)
    A.pos("hurt", "body", {0.0: (0, 0, 0), 0.08: (0, 0.4, 3.0), 0.4: (0, 0, 0)})
    A.rot("hurt", "chest", {0.0: (0, 0, 0), 0.08: (7, 0, 3), 0.4: (0, 0, 0)})
    A.rot("hurt", "neck", {0.0: (0, 0, 0), 0.08: (12, 0, 0), 0.4: (0, 0, 0)})
    A.rot("hurt", "head", {0.0: (0, 0, 0), 0.08: (14, 0, 0), 0.4: (0, 0, 0)})
    A.rot("hurt", "jaw", {0.0: (0, 0, 0), 0.08: (-34, 0, 0), 0.24: (-12, 0, 0), 0.4: (0, 0, 0)})
    for sd, s in (("l", 1), ("r", -1)):
        A.rot("hurt", "ear_" + sd, {0.0: (0, 0, 0), 0.08: (22, 0, 8 * s), 0.4: (0, 0, 0)})
        A.rot("hurt", "fa_" + sd, {0.0: (0, 0, 0), 0.08: (16, 0, 0), 0.4: (0, 0, 0)})
    A.rot("hurt", "tail_1", {0.0: (0, 0, 0), 0.08: (-26, 0, 0), 0.4: (0, 0, 0)})


# =========================================================================================================================================
#                                                              THE BASILISK
# =========================================================================================================================================
SC_RAMP = [(10, 16, 12), (22, 40, 28), (38, 72, 42), (58, 100, 52), (88, 134, 66), (126, 160, 80)]
VENT_RAMP = [(104, 92, 58), (168, 152, 98), (214, 200, 140), (238, 228, 178)]
BONE_RAMP = [(88, 74, 52), (156, 138, 100), (210, 194, 154), (238, 226, 190)]
BONE_GLOW = (110, 255, 130)
VENOM, VENOM_L = (140, 255, 64), (222, 255, 140)
TONGUE, TONGUE_D = (206, 66, 84), (140, 36, 52)


def scale_pat(u, v, cw, ch, salt):
    """Overlapping scales in staggered rows: (column, row, local u, local v, hash)."""
    row = math.floor(v / ch)
    uu = u + (cw / 2.0 if row % 2 else 0.0)
    col = math.floor(uu / cw)
    return col, row, uu - col * cw, v - row * ch, hsh(col, row, salt)


def scales(salt, cw=3, ch=2, flow="z", band=1.0, vent=0.0, dorsal=0.0, bottom="vent", lift=0.0):
    """Armoured lizard hide: staggered overlapping scales, each with its own tone from near-black to forest green, a lit top row and a dark
    lower edge, dark dorsal bands across the back, and the pale yellowish belly scutes on the lower part of the sides (vent) and on the bottom face."""
    def p(face, i, j, fw, fh):
        a, b, na, nb = axes(face, i, j, fw, fh, flow)
        u, v = (b, a) if (flow == "z" and face in ("left", "right", "top", "bottom")) else (a, b)        # u along the body, v across / down
        col, row, lu, lv, h = scale_pat(u, v, cw, ch, salt)
        t = 0.2 + 0.62 * h + lift + 0.12 * nz(i * 0.4, j * 0.4, salt)
        if hsh(col, row, salt + 3) > 0.9:
            t += 0.3                                                             # a moss-yellow scale now and then
        rel = j / max(1, fh - 1)
        if face in ("left", "right", "front", "back") and dorsal > 0:
            bandw = 0.5 + 0.5 * S(u * TAU / 6.5 + salt)
            t -= dorsal * smooth(0.65, 0.0, rel) * (0.35 + 0.5 * bandw)          # dark bands across the back
        c = ramp(SC_RAMP, t)
        if lv >= ch - 1:
            c = c * 0.7                                                          # the shaded lower edge of a scale
        elif lv < 1:
            c = c * 1.16                                                         # the lit top edge
        if lu < 1 and cw >= 3:
            c = c * 0.84
        if face == "bottom" and bottom == "vent":
            sc = math.floor(b / 2) if flow == "z" else math.floor(j / 2)
            c = ramp(VENT_RAMP, 0.25 + 0.55 * hsh(sc, salt, 9) + 0.12 * fz(i, j, salt))
            if (b if flow == "z" else j) % 2 == 0:
                c = c * 0.72                                                     # the seam between two ventral scutes
        elif vent > 0 and face in ("left", "right", "front", "back"):
            w = smooth(1.0 - vent, 1.0 - vent * 0.4, rel + 0.1 * nz(i * 0.5, j * 0.5, salt + 4))
            if w > 0:
                sc = math.floor(u / 2)
                vc = ramp(VENT_RAMP, 0.2 + 0.5 * hsh(sc, salt, 9) + 0.12 * fz(i, j, salt))
                if int(u) % 2 == 0:
                    vc = vc * 0.78
                c = c * (1 - w) + vc * w
        return rgba(c * (0.82 + 0.18 * FACE_K[face])), None
    return p


def plate_paint(salt):
    """A big dorsal armour plate: dark green-black with a bone-pale lead edge, a ridge down the middle and rivets."""
    def p(face, i, j, fw, fh):
        t = 0.12 + 0.28 * hsh(i // 2, j // 2, salt) + 0.1 * fz(i, j, salt)
        c = ramp(SC_RAMP, t)
        if face == "top":
            m = abs(i - (fw - 1) / 2.0)
            if m < 0.9:
                c = ramp(SC_RAMP, 0.5 + 0.2 * fz(i, j, salt))                  # the ridge
            if j == fh - 1:
                c = A3((98, 108, 66)) * (0.85 + 0.2 * fz(i, j, salt))             # the pale leading edge
            if j == 0:
                c = c * 0.6
            if (i % 4 == 1) and j == fh // 2:
                c = A3(BONE_RAMP[2])                                            # a rivet
        elif face == "bottom":
            c = c * 0.5
        else:
            c = c * 0.9
        return rgba(c * FACE_K[face] * 1.15), None
    return p


def bone_paint(salt, glow=40, tip=True):
    """Bone: pale ivory at the tip, stained dark towards the root, growth rings; a faint green glow in the glow layer."""
    def p(face, i, j, fw, fh):
        rel = j / max(1, fh - 1)
        t = 0.88 - 0.62 * rel + 0.1 * fz(i, j, salt)
        if (j + i) % 3 == 0:
            t -= 0.08
        c = ramp(BONE_RAMP, t)
        if face == "bottom":
            c = c * 0.7
        g = rgba(BONE_GLOW, glow * (0.4 + 0.9 * (1 - rel) ** 1.4)) if glow else None
        return rgba(c * (0.9 + 0.1 * FACE_K[face])), g
    return p


def viper_eye(face, i, j, fw, fh):
    """A venom-green eye with a black vertical slit (the glow layer lights the iris and leaves the slit dark)."""
    if face in ("left", "right"):
        slit = i == fw // 2
        if slit:
            return rgba((8, 10, 6)), None
        k = 0.7 + 0.3 * (1 - abs(j - (fh - 1) / 2.0) / max(0.5, fh / 2.0))
        return rgba(A3(VENOM) * (0.8 + 0.2 * k)), rgba(VENOM, 255)
    if face in ("front", "top"):
        return rgba(VENOM), rgba(VENOM, 230)
    return rgba(A3(SC_RAMP[1])), None


def head_scales(salt, mouth=False, nostrils=False, brow=False):
    base = scales(salt, 2, 2, "z", dorsal=0.55, vent=0.0, bottom="vent")

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face in ("left", "right"):
            a, b, na, nb = axes(face, i, j, fw, fh, "z")
            if mouth and j >= fh - 1:
                return rgba((14, 10, 10)), None                                  # the mouth line
        if face == "top" and nostrils and j >= fh - 2 and i in (1, fw - 2):
            return rgba((8, 8, 6)), None
        if face == "bottom" and mouth:
            a, b, na, nb = axes(face, i, j, fw, fh, "z")
            if i in (0, fw - 1) and a % 2 == 0:
                return rgba(TOOTH), None
            return rgba(A3(GUM) * (0.8 + 0.15 * fz(i, j, salt))), None
        if brow and face == "front" and j == 0:
            return rgba(A3(SC_RAMP[0]) * 1.2), None
        return c, g
    return p


def jaw_b(salt):
    base = scales(salt, 2, 2, "z", dorsal=0.0, vent=0.5, bottom="vent")

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face == "top":
            a, b, na, nb = axes(face, i, j, fw, fh, "z")
            if i in (0, fw - 1) and a % 2 == 1:
                return rgba(TOOTH), None
            return rgba(A3(GUM) * (0.85 + 0.15 * fz(i, j, salt))), None
        return c, g
    return p


def tongue_paint(face, i, j, fw, fh):
    c = A3(TONGUE) * (0.85 + 0.2 * fz(i, j, 31))
    if face in ("left", "right", "top") and i == fw // 2:
        c = c * 0.8
    return rgba(c), None


def claw_b(face, i, j, fw, fh):
    t = 0.15 + 0.75 * (1 - j / max(1, fh - 1)) + 0.1 * fz(i, j, 5)
    return rgba(ramp([(52, 44, 36), (150, 138, 108), (226, 216, 186)], t)), None


def build_basilisk():
    m = Model("dungeon", "basilisk", TEX_W, TEX_H)
    pk = Pack(TEX_W, TEX_H)
    r = Rig(m, pk)
    A = Anim()

    r.bone("root", None, (0, 0, 0))
    r.bone("body", "root", (0, 7, 3))                                     # the middle and the hips; the hind legs and the tail hang from it
    r.bone("chest", "body", (0, 7, -4))                                   # the shoulders; the front legs and the neck hang from it
    r.bone("neck", "chest", (0, 7.5, -12))
    r.bone("head", "neck", (0, 7.5, -17))
    r.bone("jaw", "head", (0, 5, -18))
    r.bone("tongue", "jaw", (0, 4.5, -25))
    r.lbone("eye", "!head", (5.5, 9, -21.5))
    # the legs splay out sideways: the upper arm points out (and a little forward / back), the forearm goes down, the paw lies flat
    r.lbone("fa", "!chest", (6.5, 7, -8), (0, 18, 0))
    r.lbone("fb", "fa", (11.5, 7, -8))
    r.lbone("fp", "fb", (11.5, 1, -8), (0, -26, 0))
    r.lbone("ha", "!body", (6.5, 7, 6.5), (0, -22, 0))
    r.lbone("hb", "ha", (11.5, 7, 6.5))
    r.lbone("hf", "hb", (11.5, 1, 6.5), (0, 14, 0))
    tail = ((6.5, 10), (6.5, 14), (6.5, 18), (6.5, 22), (6.5, 26))
    prev = "body"
    for n, (y, z) in enumerate(tail):
        r.bone("tail_%d" % (n + 1), prev, (0, y, z))
        prev = "tail_%d" % (n + 1)

    # torso: three heavy segments, the belly low -------------------------------------------------------------------------
    r.box("chest", (-7, 3, -12), (14, 8, 8), scales(1, 3, 2, "z", dorsal=0.6, vent=0.3), 0.2)
    r.box("body", (-7.5, 3, -4), (15, 8, 7), scales(2, 3, 2, "z", dorsal=0.6, vent=0.3), 0.2)
    r.box("body", (-7, 3, 3), (14, 8, 7), scales(3, 3, 2, "z", dorsal=0.6, vent=0.3), 0.2)
    # overlapping dorsal armour plates, each tilted a little against the next
    for n, z in enumerate((-12, -8.5, -5, -1.5, 2, 5.5, 8.5)):
        bone = "chest" if n < 2 else "body"
        r.box(bone, (-5.5, 10.5, z), (11, 2, 4), plate_paint(10 + n), 0.1, (6, 0, 0), (0, 11, z))
    # the bone crest down the spine, swept back, tallest over the shoulders
    for n, (z, h, bone) in enumerate(((-10.5, 4, "chest"), (-7, 4, "chest"), (-3.5, 4, "body"), (0, 3, "body"), (3.5, 3, "body"), (7, 3, "body"))):
        r.box(bone, (-1, 12, z), (2, h, 2), bone_paint(20 + n), 0.0, (14, 0, 0), (0, 12, z + 1))
    # spines along the flanks
    for n, (z, bone) in enumerate(((-9, "chest"), (-3, "body"), (4, "body"))):
        r.pbox(bone, (6.5, 9, z), (2, 2, 2), bone_paint(30 + n, 50), 0.0, (0, 0, 28), (6.5, 9.5, z + 1))

    # neck and head: a wedge ------------------------------------------------------------------------------------------------
    r.box("neck", (-4.5, 4.5, -17), (9, 6, 5), scales(40, 2, 2, "z", dorsal=0.5, vent=0.35), 0.2)
    r.box("neck", (-1, 10.5, -16), (2, 3, 2), bone_paint(41), 0.0, (16, 0, 0), (0, 10.5, -15))
    r.box("head", (-5, 5, -23), (10, 6, 6), head_scales(42, brow=True), 0.1)                                                       # the skull
    r.box("head", (-3.5, 5.5, -28), (7, 4, 5), head_scales(43, mouth=True, nostrils=True), 0.1)                                      # the snout
    r.box("head", (-3, 6, -29), (6, 3, 1), head_scales(44, nostrils=True), 0.1)
    r.box("head", (-5.5, 11, -24.5), (11, 1, 4), bone_paint(45, 30), 0.1)                                                                              # the bony brow crest
    r.pbox("head", (4.5, 11, -19), (1, 2, 2), bone_paint(46), 0.0, (35, 0, 0), (5, 11, -18))                                       # horns behind the brow
    r.pbox("head", (5, 5.5, -21), (2, 2, 2), bone_paint(47, 0), 0.0, (0, 35, 0), (5.5, 6.5, -20))                                  # cheek horns
    r.box("head", (-1, 11, -20), (2, 3, 2), bone_paint(48), 0.0, (18, 0, 0), (0, 11, -19))                                         # a crest on the skull
    r.pbox("head", (3, 4.3, -26.5), (1, 1, 1), tooth_paint(3), 0.0)                                              # upper teeth
    r.pbox("head", (3, 4.3, -23.5), (1, 1, 1), tooth_paint(4), 0.0)
    r.box("jaw", (-3.5, 3, -27), (7, 2, 9), jaw_b(49), 0.1)                                                                        # the lower jaw
    r.pbox("jaw", (3, 5, -26), (1, 1, 1), tooth_paint(5), 0.0)
    r.pbox("jaw", (3, 5, -22.5), (1, 1, 1), tooth_paint(6), 0.0)
    r.box("tongue", (-1, 4, -30), (2, 1, 5), tongue_paint, 0.0)                                                                    # the forked tongue, flicked in the idle clip
    r.pbox("tongue", (0.2, 4, -32), (1, 1, 2), tongue_paint, 0.0, (0, 14, 0), (0.7, 4.5, -30))
    r.lbox("eye", (5, 8, -23), (1, 2, 3), viper_eye, 0.0)                                                                        # the eyes

    # legs ---------------------------------------------------------------------------------------------------------------
    for pre, z in (("f", -8.0), ("h", 6.5)):
        r.lbox(pre + "a", (6, 5, z - 2.5), (6, 4, 5), scales(50 + (pre == "h"), 2, 2, "y", dorsal=0.4, vent=0.0, bottom="y"), 0.2)
        r.lbox(pre + "a", (11, 8.5, z - 1), (1, 2, 2), bone_paint(52, 40), 0.0, (20, 0, 0), (11.5, 9, z))                          # the elbow spur
        r.lbox(pre + "b", (10, 0.5, z - 1.5), (3, 7, 3), scales(53 + (pre == "h"), 2, 2, "y", dorsal=0.0, vent=0.0, bottom="y", lift=-0.05), 0.2)
        r.lbox(pre + ("p" if pre == "f" else "f"), (9.5, 0, z - 3), (5, 1, 6), scales(55 + (pre == "h"), 2, 2, "y", dorsal=0.0, lift=-0.1, bottom="y"), 0.1)
        for k in range(3):
            r.lbox(pre + ("p" if pre == "f" else "f"), (9.7 + k * 1.7, 0, z - 5), (1, 1, 2), claw_b, 0.0)

    # tail: thick, tapering, spiked ---------------------------------------------------------------------------------------
    for n, (w, h, d) in enumerate(((11, 7, 4), (9, 6, 4), (7, 5, 4), (5, 4, 4), (3, 3, 3))):
        z0 = tail[n][1]
        r.box("tail_%d" % (n + 1), (-w / 2, 6.5 - h / 2, z0), (w, h, d), scales(60 + n, 3 if n < 3 else 2, 2, "z", dorsal=0.6, vent=0.3), 0.15)
    for n, (h, w) in enumerate(((3, 2), (3, 2), (2, 2), (2, 1))):
        r.box("tail_%d" % (n + 1), (-w / 2, 6.5 + (7, 6, 5, 4)[n] / 2 - 0.5, tail[n][1] + 1), (w, h, 2), bone_paint(70 + n), 0.0, (14, 0, 0), (0, 10, tail[n][1] + 2))

    basilisk_anims(A)
    return m, pk, A


# ------------------------------------------------------------------------------------------------------------- the Basilisk's clips
def lizard_gait(a, clip, L, side, ph_f, ph_h, amp=26.0, lift=16.0, n=1):
    sg = 1 if side == "l" else -1
    for pre, ph in (("f", ph_f), ("h", ph_h)):
        cyc(a, clip, pre + "a_" + side, "rot", L, lambda th, ph=ph: (0, sg * amp * C(th + ph), sg * lift * max(0.0, -S(th + ph)) ** 0.8), n)
        cyc(a, clip, pre + "b_" + side, "rot", L, lambda th, ph=ph: (12 * C(th + ph), 0, 0), n)
        cyc(a, clip, pre + "p_" + side if pre == "f" else "hf_" + side, "rot", L, lambda th, ph=ph: (0, sg * 8 * S(th + ph), 0), n)


def basilisk_anims(A):
    # ---------------------------------------------------------------- idle: slow breaths, a swaying tail, a tongue flick or two
    L = 4.0
    A.clip("idle", L, True)
    cyc(A, "idle", "chest", "scale", L, lambda th: (1 + 0.012 * S(th), 1 + 0.03 * S(th), 1 + 0.02 * S(th)), 2)
    cyc(A, "idle", "body", "pos", L, lambda th: (0, 0.2 * S(th), 0), 2)
    cyc(A, "idle", "neck", "rot", L, lambda th: (1.5 * S(2 * th), 3.0 * S(th + 0.4), 0), 1)
    cyc(A, "idle", "head", "rot", L, lambda th: (2.0 * S(2 * th + 1.2), 6.0 * S(th + 1.0), 0), 1)
    cyc(A, "idle", "jaw", "rot", L, lambda th: (-1.0 - 1.0 * S(2 * th), 0, 0), 1)
    for k in range(5):
        cyc(A, "idle", "tail_%d" % (k + 1), "rot", L, lambda th, k=k: (0, (5 + 2 * k) * S(th - 0.7 * k), 0), 1)
    A.scale("idle", "tongue", {0.0: (1, 1, 0.04), 1.0: (1, 1, 0.04), 1.12: (1, 1, 1.0), 1.4: (1, 1, 1.0), 1.55: (1, 1, 0.04), 2.9: (1, 1, 0.04), 3.0: (1, 1, 1.0),
                                 3.12: (1, 1, 0.3), 3.24: (1, 1, 1.0), 3.45: (1, 1, 0.04), 4.0: (1, 1, 0.04)})
    A.rot("idle", "jaw", {0.0: (-1, 0, 0), 1.0: (-1, 0, 0), 1.12: (-8, 0, 0), 1.45: (-8, 0, 0), 1.6: (-1, 0, 0), 2.9: (-1, 0, 0), 3.0: (-8, 0, 0), 3.45: (-8, 0, 0), 3.6: (-1, 0, 0), 4.0: (-1, 0, 0)})

    # ---------------------------------------------------------------- walk: diagonal legs, the body undulating, the tail whipping behind
    L = 1.4
    A.clip("walk", L, True)
    lizard_gait(A, "walk", L, "l", 0.0, math.pi)
    lizard_gait(A, "walk", L, "r", math.pi, 0.0)
    cyc(A, "walk", "body", "rot", L, lambda th: (0, 7.0 * S(th), 0), 1)
    cyc(A, "walk", "chest", "rot", L, lambda th: (0, -11.0 * S(th), 0), 1)
    cyc(A, "walk", "neck", "rot", L, lambda th: (0, 7.0 * S(th), 0), 1)
    cyc(A, "walk", "head", "rot", L, lambda th: (2.0 * S(2 * th + 0.6), 6.0 * S(th + 0.3), 0), 1)
    cyc(A, "walk", "body", "pos", L, lambda th: (0, 0.35 * C(2 * th), 0), 1)
    cyc(A, "walk", "jaw", "rot", L, lambda th: (-3 - 1.5 * S(2 * th), 0, 0), 1)
    for k in range(5):
        cyc(A, "walk", "tail_%d" % (k + 1), "rot", L, lambda th, k=k: (0, (9 + 2 * k) * S(th - 1.0 - 0.8 * k), 0), 1)
    A.scale("walk", "tongue", {0.0: (1, 1, 0.04), 0.5: (1, 1, 0.04), 0.6: (1, 1, 1.0), 0.8: (1, 1, 1.0), 0.95: (1, 1, 0.04), 1.4: (1, 1, 0.04)})

    # ---------------------------------------------------------------- attack: it rears up on its forelegs and snaps (loops while it fights)
    L = 1.1
    A.clip("attack", L, True)
    kf = lambda *pts: {t: v for t, v in pts}
    A.rot("attack", "chest", kf((0.0, (0, 0, 0)), (0.18, (-5, 0, 0)), (0.36, (30, 0, 0)), (0.5, (24, 0, 0)), (0.7, (12, 0, 0)), (1.1, (0, 0, 0))))
    A.pos("attack", "chest", kf((0.0, (0, 0, 0)), (0.18, (0, -0.5, 1.5)), (0.36, (0, 2.0, -2.0)), (0.5, (0, 1.5, -3.5)), (0.7, (0, 0.5, -1.0)), (1.1, (0, 0, 0))))
    A.rot("attack", "neck", kf((0.0, (0, 0, 0)), (0.18, (-14, 0, 0)), (0.36, (10, 0, 0)), (0.5, (14, 0, 0)), (0.7, (4, 0, 0)), (1.1, (0, 0, 0))))
    A.rot("attack", "head", kf((0.0, (0, 0, 0)), (0.18, (-8, 0, 0)), (0.36, (8, 0, 0)), (0.46, (14, 0, 0)), (0.56, (-12, 0, 0)), (0.7, (-4, 0, 0)), (1.1, (0, 0, 0))))
    A.rot("attack", "jaw", kf((0.0, (0, 0, 0)), (0.18, (-10, 0, 0)), (0.36, (-48, 0, 0)), (0.46, (-52, 0, 0)), (0.53, (-2, 0, 0)), (0.7, (-6, 0, 0)), (1.1, (0, 0, 0))))
    A.scale("attack", "tongue", kf((0.0, (1, 1, 0.04)), (0.3, (1, 1, 0.04)), (0.4, (1, 1, 1.0)), (0.5, (1, 1, 1.0)), (0.56, (1, 1, 0.04)), (1.1, (1, 1, 0.04))))
    for sd, sg in (("l", 1), ("r", -1)):
        A.rot("attack", "fa_" + sd, kf((0.0, (0, 0, 0)), (0.18, (0, 0, 0)), (0.36, (0, sg * 24, sg * 34)), (0.5, (0, sg * 30, sg * 40)), (0.7, (0, sg * 10, sg * 12)), (1.1, (0, 0, 0))))
        A.rot("attack", "fb_" + sd, kf((0.0, (0, 0, 0)), (0.36, (24, 0, 0)), (0.5, (30, 0, 0)), (1.1, (0, 0, 0))))
        A.rot("attack", "ha_" + sd, kf((0.0, (0, 0, 0)), (0.18, (0, sg * -8, 0)), (0.36, (0, sg * 12, 0)), (0.5, (0, sg * 14, 0)), (1.1, (0, 0, 0))))
    for k in range(5):
        A.rot("attack", "tail_%d" % (k + 1), kf((0.0, (0, 0, 0)), (0.36, (-5, (6 + 3 * k) * (1 if k % 2 else -1), 0)), (0.7, (-3, (-6 - 3 * k) * (1 if k % 2 else -1), 0)), (1.1, (0, 0, 0))))

    # ---------------------------------------------------------------- sleep: flat on the ground, the legs splayed out, the tail coiled, the eyes shut
    L = 4.0
    A.clip("sleep", L, True)
    A.pos("sleep", "body", {0.0: (0, -2.8, 0), 4.0: (0, -2.8, 0)})
    cyc(A, "sleep", "chest", "scale", L, lambda th: (1 + 0.012 * S(th), 1 + 0.03 * S(th), 1 + 0.025 * S(th)), 1)
    A.rot("sleep", "chest", {0.0: (-1, 14, 0), 4.0: (-1, 14, 0)})
    A.rot("sleep", "neck", {0.0: (-6, 20, 0), 4.0: (-6, 20, 0)})
    A.rot("sleep", "head", {0.0: (-8, 22, 0), 4.0: (-8, 22, 0)})
    A.rot("sleep", "jaw", {0.0: (0, 0, 0), 4.0: (0, 0, 0)})
    for sd, sg in (("l", 1), ("r", -1)):
        A.pos("sleep", "eye_" + sd, {0.0: (-1.5 * sg, 0, 0), 4.0: (-1.5 * sg, 0, 0)})
        A.rot("sleep", "fa_" + sd, {0.0: (0, 0, sg * -12), 4.0: (0, 0, sg * -12)})
        A.rot("sleep", "fb_" + sd, {0.0: (0, 0, sg * 78), 4.0: (0, 0, sg * 78)})
        A.rot("sleep", "ha_" + sd, {0.0: (0, 0, sg * -12), 4.0: (0, 0, sg * -12)})
        A.rot("sleep", "hb_" + sd, {0.0: (0, 0, sg * 78), 4.0: (0, 0, sg * 78)})
    for k in range(5):
        cyc(A, "sleep", "tail_%d" % (k + 1), "rot", L, lambda th, k=k: (0, -30 + 1.0 * S(th - 0.5 * k), 0), 1)

    # ---------------------------------------------------------------- hurt: a flinch (plays once)
    A.clip("hurt", 0.4, False)
    A.rot("hurt", "chest", {0.0: (0, 0, 0), 0.08: (-8, 6, 0), 0.4: (0, 0, 0)})
    A.rot("hurt", "neck", {0.0: (0, 0, 0), 0.08: (16, 0, 0), 0.4: (0, 0, 0)})
    A.rot("hurt", "head", {0.0: (0, 0, 0), 0.08: (14, 0, 0), 0.4: (0, 0, 0)})
    A.rot("hurt", "jaw", {0.0: (0, 0, 0), 0.08: (-34, 0, 0), 0.24: (-10, 0, 0), 0.4: (0, 0, 0)})
    A.pos("hurt", "body", {0.0: (0, 0, 0), 0.08: (0, 0.3, 2.0), 0.4: (0, 0, 0)})
    for k in range(5):
        A.rot("hurt", "tail_%d" % (k + 1), {0.0: (0, 0, 0), 0.1: (0, 12 + 3 * k, 0), 0.25: (0, -10 - 3 * k, 0), 0.4: (0, 0, 0)})


def main():
    which = [a for a in sys.argv[1:] if not a.startswith("-")]
    for name, build in (("direwolf", build_direwolf), ("basilisk", build_basilisk)):
        if which and name not in which:
            continue
        m, pk, A = build()
        pk.finish()
        img, glow = pk.images()
        m.save(A)
        m.save_texture(img, glow)
        print(m.id, "cubes", m.cube_count(), "texture rows used", pk.used(), "of", pk.H)


if __name__ == "__main__":
    main()
