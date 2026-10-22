"""The Gnome, the Earth Spirit of Black Clover (docs/magic/gnome.md): a MASSIVE EARTH GOLEM in two forms, built from layered rock plates and boulders: broad boulder shoulders with moss and amber
crystal clusters, thick arms ending in huge rocky fists, a SMALL head with glowing amber eyes under a heavy brow, a magic circle (a diamond in a star) carved on the chest, crystal veins that glow,
a crown of crystal shards, floating stones, and legs of stacked boulders that stay planted on the ground.

    python3 -B tools/gen_gnome_geo.py            writes assets/nusmp/{geo,animations,textures}/entity/gnome_{full,companion}[_lod1|_lod2|_fx]*
    python3 -B tools/gen_gnome_geo.py --count    prints the quads of every model

    gnome_full        the FULL form (about 6.9 blocks with its crown, a stone giant), rising out of the ground for the big earth spells
    gnome_companion   the COMPANION form (about 1.5 blocks), the same design with a bigger head, that walks / hovers beside its master
    ..._lod1 / ..._lod2   fewer cubes and a half / quarter size copy of the sheet;  ..._fx  the glow-only model (eyes, the magic circle, rune bands, crystal veins), drawn additive

Silhouette is geometry (head, shoulders, fists, crystals); detail is texture (facets, ink lines, moss, cracks, the face, the circle): a face that is not seen is not given a uv. All models share
ONE sheet and the bones of the full model, so the one animation file of a form drives them all. Animations: idle, walk, rise, cast (both fists slam the ground), punch, stomp, sink.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gnome_geo_lib as L  # noqa: E402
import gen_gnome_art as A  # noqa: E402

D = 5.0                                   # texels per model unit of the full form
FORMS = {"full": dict(k=1.0, hs=1.0, seed=7100), "companion": dict(k=0.20, hs=1.38, seed=7200)}
HEAD_PIVOT = (0.0, 92.0, -6.0)
LOD_SCALE = {0: 1.0, 1: 0.5, 2: 0.25}


def noz(w, h):
    return np.zeros((h, w, 4), np.float32)


# ====================================================================================================================== the sheet
def make_atlas():
    at = L.Atlas(1024, 1024)
    S = 1000
    at.region("rock_a", 256, 256, lambda w, h: A.stone(w, h, S + 1, w / 5.0, moss=0.45, warm=0.22, cracks=2))
    at.region("rock_b", 256, 256, lambda w, h: A.stone(w, h, S + 2, w / 7.0, warm=0.20, cracks=3, vein=1, tone=0.0))
    at.region("rock_c", 256, 256, lambda w, h: A.stone(w, h, S + 3, w / 3.4, warm=0.25, cracks=1, moss=0.22))
    at.region("rock_top", 256, 256, lambda w, h: A.stone(w, h, S + 4, w / 5.5, tone=0.07, warm=0.12, moss=0.95, moss_top=False))
    at.region("rock_dark", 192, 192, lambda w, h: A.stone(w, h, S + 5, w / 3.6, tone=-0.15, warm=0.10, cracks=2))
    at.region("rock_lt", 256, 256, lambda w, h: A.stone(w, h, S + 16, w / 4.0, tone=0.10, warm=0.45, cracks=1, moss=0.2))
    at.region("circle", 256, 256, lambda w, h: A.magic_circle(w, h, S + 6))
    at.region("knuckle", 192, 160, lambda w, h: A.knuckle(w, h, S + 7))
    at.region("veins", 256, 256, lambda w, h: A.veins_plate(w, h, S + 8))
    at.region("crystal", 64, 128, lambda w, h: A.crystal(w, h, S + 9, "amber"))
    at.region("crystal_d", 64, 128, lambda w, h: A.crystal(w, h, S + 10, "dia"))
    at.region("bracer", 256, 48, lambda w, h: A.bracer(w, h, S + 11))
    at.region("face_front", 160, 140, lambda w, h: A.face_front(w, h, S + 12))
    at.region("face_side", 128, 112, lambda w, h: A.face_side(w, h, S + 13))
    at.region("moss", 128, 160, lambda w, h: A.moss_strand(w, h, S + 14))
    at.region("rubble", 192, 192, lambda w, h: A.rubble(w, h, S + 15))
    return at


# ====================================================================================================================== the body
class Builder:
    def __init__(self, form, lod, atlas, fx=False):
        f = FORMS[form]
        self.form, self.lod, self.fx = form, lod, fx
        self.k, self.hs = f["k"], f["hs"]
        ident = "gnome_" + form + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
        self.m = L.Model(ident, atlas, seed=f["seed"] + lod, tex_scale=0.5 if fx else LOD_SCALE[lod])
        self.dens = D / self.k                                  # a smaller body reads the same texels per face: a face of the companion shows the same window as the same face of the giant
        self.n = 0                                              # the part counter: picks the stone of a face and the window of the sheet
        self.m.cubes_n = 0

    # ---- placement helpers (full-form units in, model units out)
    def sc(self, p, head=False):
        if head and self.hs != 1.0:
            p = [HEAD_PIVOT[i] + (p[i] - HEAD_PIVOT[i]) * self.hs for i in range(3)]
        return [p[0] * self.k, p[1] * self.k, p[2] * self.k]

    def bone(self, name, pivot, parent=None, rotation=None, head=False):
        self.m.bone(name, self.sc(pivot, head), parent, rotation)

    def stone_faces(self, kind, up=True, down=False, front=True, back=True, sides=True):
        side = {"boulder": "rock_c", "plate": "rock_lt", "arm": "rock_b", "dark": "rock_dark", "leg": "rock_dark"}[kind]
        top = "rock_top" if kind not in ("dark", "leg") else "rock_dark"
        d = self.dens
        f = {}
        if up:
            f["up"] = (top, d)
        if sides:
            f["west"] = (side, d)
            f["east"] = (side, d)
        if front:
            f["north"] = (side, d)
        if back:
            f["south"] = (side, d)
        if down:
            f["down"] = ("rock_dark", d)
        return f

    def part(self, bone, c, s, faces, rot=None, piv=None, lod=2, comp=True, head=False):
        """One cube: c = centre, s = size (full-form units). lod = the highest level it appears in (2 = every level); comp False = the companion leaves it out. Faces of lower levels: no under
        side, and on level 2 no back."""
        if self.lod > lod or (self.form == "companion" and not comp):
            return
        if self.fx:
            return
        faces = dict(faces)
        if self.lod >= 1:
            faces.pop("down", None)
        if self.lod >= 2:
            faces.pop("south", None)
        if not faces:
            return
        o = [c[0] - s[0] / 2.0, c[1] - s[1] / 2.0, c[2] - s[2] / 2.0]
        hs = self.hs if head else 1.0
        self.m.cube(bone, self.sc(o, head), [v * self.k * hs for v in s], faces, rot, None if piv is None else self.sc(piv, head), self.dens)

    def glow(self, bone, c, s, faces, rot=None, piv=None, lod=1, comp=True, head=False):
        """A glow-only cube (the fx model): faces name a region of the sheet and read it whole (density 0)."""
        if not self.fx or self.lod > lod or (self.form == "companion" and not comp):
            return
        o = [c[0] - s[0] / 2.0, c[1] - s[1] / 2.0, c[2] - s[2] / 2.0]
        hs = self.hs if head else 1.0
        self.m.cube(bone, self.sc(o, head), [v * self.k * hs for v in s], faces, rot, None if piv is None else self.sc(piv, head), 0)

    def shard(self, bone, c, h, w, rot, kind="amber", lod=2, comp=True, head=False, tip=True):
        """A crystal shard: a tall thin cube leaning by rot (degrees), a small tip cube on top on the nearer levels; the glow-only model repeats it a hair bigger."""
        reg = "crystal" if kind == "amber" else "crystal_d"
        cc = (c[0], c[1] + h / 2.0, c[2])
        piv = (c[0], c[1], c[2])
        f = {"up": (reg, 0), "west": (reg, 0), "east": (reg, 0), "north": (reg, 0), "south": (reg, 0)}
        if self.fx:
            if self.lod <= 1 and lod >= self.lod:
                self.glow(bone, cc, (w * 1.08, h * 1.02, w * 1.08), {"west": (reg, 0), "east": (reg, 0), "north": (reg, 0), "south": (reg, 0)}, rot, piv, lod=1, comp=comp, head=head)
            return
        self.part(bone, cc, (w, h, w), f, rot, piv, lod, comp, head)
        if tip and self.lod == 0:
            tc = (c[0], c[1] + h + w * 0.25, c[2])
            self.part(bone, tc, (w * 0.62, w * 0.9, w * 0.62), {"west": (reg, 0), "east": (reg, 0), "north": (reg, 0), "south": (reg, 0), "up": (reg, 0)}, rot, piv, 0, comp, head)


def build(form, lod, atlas, fx=False):
    b = Builder(form, lod, atlas, fx)
    m = b.m
    P = b.part
    B = b.bone
    big = form == "full"
    X = 19.0                                           # the legs' stance
    SH = 46.0                                          # the shoulders' stance
    # ------------------------------------------------------------------ bones (the same in every model)
    B("body", (0, 40, 0))
    B("chest", (0, 48, 0), "body")
    B("head", HEAD_PIVOT, "chest", rotation=(-6, 0, 0))
    for sx, t in ((-1, "R"), (1, "L")):
        B("arm_" + t, (sx * SH, 84, 0), "chest")
        B("fore_" + t, (sx * (SH + 4), 46, -2), "arm_" + t, rotation=(14, 0, 0))
        B("fist_" + t, (sx * (SH + 4), 22, -6), "fore_" + t, rotation=(-6, 0, 0))
        B("leg_" + t, (sx * X, 36, 2), "body")
        B("shin_" + t, (sx * X, 17, 2), "leg_" + t)
        B("foot_" + t, (sx * X, 7, 2), "shin_" + t)
    orbs = [("orb1", (-64, 70, -10), 11, 1), ("orb2", (64, 94, 8), 9, 1), ("orb3", (-34, 118, 16), 8, 0), ("orb4", (40, 40, -40), 9, 0)]
    for nm, c, s, lv in orbs:
        B(nm, c, "root")

    # ------------------------------------------------------------------ legs: stacked dark boulders on a slab, planted, a clear gap between them
    for sx, t in ((-1, "R"), (1, "L")):
        x = sx * X
        P("leg_" + t, (x, 26, 2), (21, 20, 24), b.stone_faces("dark", down=False), lod=2)
        P("leg_" + t, (x, 26, -11.5), (17, 12, 5), b.stone_faces("plate", down=False, back=False), rot=(8, 0, 0), piv=(x, 26, -11), lod=1)             # knee plate
        P("leg_" + t, (x + sx * 11.5, 28, 2), (4, 14, 16), b.stone_faces("arm", down=False, front=False, back=False), lod=0)                       # a slab on the outside
        P("shin_" + t, (x, 10, 2), (19, 15, 21), b.stone_faces("leg", up=False, down=False), lod=2)
        P("shin_" + t, (x, 13, 13.5), (15, 9, 5), b.stone_faces("plate", down=False, front=False), lod=0)
        P("foot_" + t, (x, 3.5, -2), (27, 7, 35), b.stone_faces("leg", down=False), lod=2)
        for j in range(3):
            P("foot_" + t, (x + (j - 1) * 9, 5, -21.5), (8, 10, 9), b.stone_faces("arm", back=False, sides=(j != 1)), lod=0)
        P("foot_" + t, (x + sx * 15.5, 3, 0), (7, 6, 13), b.stone_faces("dark", down=False), lod=0)
    # ------------------------------------------------------------------ pelvis and belt (narrow: the body is a V)
    P("body", (0, 41, 0), (40, 13, 30), b.stone_faces("dark", down=False), lod=2)
    P("body", (0, 45, -15.6), (30, 5, 1.6), {"north": ("bracer", 0), "up": ("rock_dark", b.dens)}, lod=1)
    for sx in (-1, 1):
        P("body", (sx * 21, 37, -10), (9, 9, 12), b.stone_faces("arm", down=False, back=False), lod=0)                       # hip stones
    # ------------------------------------------------------------------ chest: a lower plate, then the great breast, the carved circle
    P("chest", (0, 56, 0), (34, 16, 28), b.stone_faces("plate", down=False), lod=2)
    P("chest", (0, 74, 0), (58, 27, 38), b.stone_faces("boulder", down=False), rot=(0, 0, 0), lod=2)
    P("chest", (0, 70, -21.5), (31, 31, 4), {"north": ("circle", 0), "up": ("rock_top", b.dens), "west": ("rock_dark", b.dens), "east": ("rock_dark", b.dens)}, lod=2)
    for sx in (-1, 1):
        P("chest", (sx * 25, 77, -20), (14, 15, 8), b.stone_faces("plate", down=False, back=False), rot=(0, 0, sx * -5), piv=(sx * 25, 77, -20), lod=1)         # pectoral stones
        P("chest", (sx * 21, 58, -17), (11, 10, 6), b.stone_faces("arm", down=False, back=False), lod=0)
    P("chest", (0, 74, 21.5), (48, 27, 7), b.stone_faces("plate", down=False, front=False), lod=1)                            # back plate
    P("chest", (0, 90.5, 6), (40, 4, 26), b.stone_faces("plate", down=False), lod=1)                                          # a ledge of slabs between the shoulders
    P("chest", (0, 86, 22), (22, 18, 8), b.stone_faces("arm", down=False, front=False), rot=(-14, 0, 0), piv=(0, 80, 22), lod=0)               # a hump
    b.shard("chest", (0, 85, 24), 20, 6, (-22, 0, 0), "amber", lod=1)
    b.shard("chest", (-11, 82, 24), 15, 5, (-18, 0, 18), "dia", lod=1, comp=False)
    b.shard("chest", (11, 82, 24), 15, 5, (-18, 0, -18), "amber", lod=0)
    # ------------------------------------------------------------------ head: SMALL, forward and low between the shoulders, a heavy brow, glowing eyes, a short crown of crystals
    P("head", (0, 100, -8), (22, 18, 22), {"north": ("face_front", 0), "west": ("face_side", 0), "east": ("face_side", 0), "up": ("rock_top", b.dens), "south": ("rock_a", b.dens)}, lod=2, head=True)
    P("head", (0, 107.5, -18), (25, 4, 7), b.stone_faces("plate", down=False, back=False), lod=1, head=True)                 # the brow overhang
    P("head", (0, 91.5, -16), (15, 5, 8), b.stone_faces("dark", up=False, down=False, back=False), lod=0, head=True)           # jaw
    P("head", (0, 90, -5), (13, 8, 13), b.stone_faces("dark", up=False, down=False), lod=1, head=True)                      # neck
    for sx in (-1, 1):
        P("head", (sx * 13, 100, -7), (4, 9, 9), b.stone_faces("arm", up=True, back=False, front=False, sides=True), lod=0, head=True)       # ear slabs
    b.shard("head", (0, 109, -7), 12, 5, (0, 0, 0), "amber", lod=2, head=True)
    b.shard("head", (-6, 108, -7), 9, 4, (0, 0, 24), "amber", lod=1, head=True)
    b.shard("head", (6, 108, -7), 9, 4, (0, 0, -24), "dia", lod=1, head=True)
    b.shard("head", (-9.5, 106, -7), 6, 3.2, (0, 0, 46), "amber", lod=0, head=True)
    b.shard("head", (9.5, 106, -7), 6, 3.2, (0, 0, -46), "amber", lod=0, head=True)
    # ------------------------------------------------------------------ shoulders, arms, fists: broad boulder shoulders, thick arms, HUGE fists
    for sx, t in ((-1, "R"), (1, "L")):
        sa, sf = "arm_" + t, "fore_" + t
        x = sx * SH
        P(sa, (x, 82, 0), (31, 27, 33), b.stone_faces("boulder", down=False), rot=(0, 0, sx * -4), piv=(x, 82, 0), lod=2)
        P(sa, (x + sx * 1, 96, 0), (36, 6, 36), b.stone_faces("plate", down=False), rot=(0, 0, -sx * 10), piv=(x, 96, 0), lod=1)             # the pauldron slab
        P(sa, (x - sx * 6, 98.5, -12), (14, 5, 14), b.stone_faces("arm", down=False), rot=(0, sx * 24, 0), piv=(x, 98, -12), lod=0)           # a loose stone on it
        P(sa, (x + sx * 2, 58, 0), (23, 30, 25), b.stone_faces("arm", up=False, down=False), lod=2)
        P(sa, (x + sx * 2, 71, -13.5), (19, 5, 3), b.stone_faces("plate", down=False, back=False), lod=0)                                   # a strap of stone across the front
        b.shard(sa, (x + sx * 6, 99, 6), 17, 5.5, (6, 0, -sx * 20), "amber" if sx < 0 else "dia", lod=1)
        b.shard(sa, (x - sx * 3, 100, -8), 12, 4.5, (-10, 0, -sx * 8), "amber", lod=0)
        b.shard(sa, (x + sx * 11, 94, -2), 10, 4, (0, 0, -sx * 42), "amber", lod=0, comp=False)
        P(sa, (x + sx * 2, 98, -18.5), (13, 14, 0.5), {"north": ("moss", 0), "south": ("moss", 0)}, lod=0, comp=False)                    # hanging moss
        P(sf, (x + sx * 4, 32, -3), (28, 30, 29), b.stone_faces("arm", up=False, down=False), lod=2)
        P(sf, (x + sx * 4, 28.5, -3), (31, 8, 32), {"north": ("bracer", 0), "west": ("bracer", 0), "east": ("bracer", 0), "south": ("bracer", 0)}, lod=1)   # rune cuff
        b.shard(sf, (x + sx * 4, 46, 12), 11, 4.5, (-62, 0, 0), "amber", lod=0, comp=False)
        fx0 = x + sx * 4
        P("fist_" + t, (fx0, 12.5, -7), (37, 25, 37), {**b.stone_faces("boulder", down=False, front=False), "north": ("knuckle", 0)}, lod=2)
        P("fist_" + t, (fx0, 25.5, -7), (39, 4, 39), b.stone_faces("plate", down=False), lod=1)
        for j in range(4):
            P("fist_" + t, (fx0 + (j - 1.5) * 9, 16, -27), (8.4, 11, 6.5), {"north": ("knuckle", 0), "up": ("rock_top", b.dens), "west": ("rock_a", b.dens), "east": ("rock_a", b.dens)}, lod=1)
        P("fist_" + t, (fx0 - sx * 20, 12, -15), (7, 10, 16), b.stone_faces("arm", down=False, back=False), lod=0)             # thumb
    # ------------------------------------------------------------------ floating stones (a few, they bob and turn)
    for nm, c, s, lv in orbs:
        P(nm, c, (s, s, s), b.stone_faces("boulder", down=True), rot=(18, 30, 12), piv=c, lod=lv, comp=(lv == 1))
    # ------------------------------------------------------------------ the rubble ring where the stone heaves up (the big form only)
    if big:
        for i in range(9):
            a = i / 9.0 * math.tau + 0.3
            r = 54 + 9 * ((i * 5) % 3)
            c = (math.cos(a) * r, 2.5, math.sin(a) * r)
            P("root", c, (9 + (i % 3) * 3, 5, 9), b.stone_faces("dark", down=False), rot=(0, math.degrees(a), 0), piv=c, lod=0)
    # ------------------------------------------------------------------ glow-only model: eyes, the circle, rune bands, veins
    G = b.glow
    G("head", (0, 100, -19.12), (22, 18, 0.2), {"north": ("face_front", 0)}, lod=1, head=True)
    G("chest", (0, 70, -23.62), (31, 31, 0.2), {"north": ("circle", 0)}, lod=1)
    G("body", (0, 45, -16.5), (30, 5, 0.2), {"north": ("bracer", 0)}, lod=0)
    for sx, t in ((-1, "R"), (1, "L")):
        sf = "fore_" + t
        x = sx * (SH + 4)
        G(sf, (x, 28.5, -19.2), (31, 8, 0.2), {"north": ("bracer", 0)}, lod=1)
        G(sf, (x + sx * 15.7, 28.5, -3), (0.2, 8, 32), {"west": ("bracer", 0)} if sx > 0 else {"east": ("bracer", 0)}, lod=0)
        G("arm_" + t, (sx * SH + sx * 13.8, 58, 0), (0.2, 26, 21), {"west": ("veins", 0)} if sx > 0 else {"east": ("veins", 0)}, lod=1)
        G("fist_" + t, (x, 12.5, -30.4), (34, 20, 0.2), {"north": ("veins", 0)}, lod=0)
        G("leg_" + t, (sx * X, 26, -10.2), (17, 15, 0.2), {"north": ("veins", 0)}, lod=0)
    G("chest", (0, 74, 25.2), (40, 22, 0.2), {"south": ("veins", 0)}, lod=1)
    return m


# ====================================================================================================================== animations
# Rotation convention of the files (checked in the preview, scenes gnome_dev_full_rise): +x swings the part BELOW the pivot forward (-z) and so tips the part above it back. A hanging arm
# or leg: + = forward / up. An upright part (chest, head): + = leans back, - = leans forward. Heights (position y) are in model units.
TICK = 0.05
SLAM_T = 0.70                                         # the rise: both fists land at 0.70 s (tick 14)
CAST_T = 0.60                                         # the cast: contact at 0.60 s (tick 12)
LENGTHS = {"rise": 1.0, "cast": 1.2, "sink": 1.0, "punch": 1.0, "stomp": 0.9}


def tracks(clip_len, fn, steps=12):
    return {i / steps * clip_len: fn(i / steps) for i in range(steps + 1)}


def rot(ts, vals):
    return {"rotation": dict(zip(ts, vals))}


def animations(form):
    clips = {}
    s = L.sine
    ORB = ("orb1", "orb2", "orb3", "orb4")

    # ---- idle: slow and heavy: the chest breathes, the head turns a little, the arms sway, the stones drift
    T = 4.0
    bones = {"chest": {"scale": tracks(T, lambda u: (1.0, 1.0 + 0.012 * math.sin(u * math.tau), 1.0 + 0.008 * math.sin(u * math.tau)), 12)},
             "head": {"rotation": L.merge(s(T, 3.0, 1, 0.15, steps=12), s(T, 1.6, 0, 0.0, steps=12))},
             "arm_L": {"rotation": s(T, 2.4, 0, 0.1, steps=12)}, "arm_R": {"rotation": s(T, -2.4, 0, 0.1, steps=12)},
             "fore_L": {"rotation": s(T, 2.0, 0, 0.3, steps=12)}, "fore_R": {"rotation": s(T, 2.0, 0, 0.3, steps=12)},
             "body": {"position": tracks(T, lambda u: (0, 0.5 * math.sin(u * math.tau), 0), 12)}}
    for i, nm in enumerate(ORB):
        bones[nm] = {"position": tracks(T, lambda u, i=i: (0, 2.2 * math.sin((u + i * 0.25) * math.tau), 0), 12), "rotation": s(T, 16, 1, i * 0.2, steps=12)}
    clips["idle"] = {"loop": True, "length": T, "bones": bones}

    # ---- walk: two heavy steps in 1.6 s, the body drops onto the planted foot, the arms swing against the legs
    W = 1.6
    A = 17.0
    bones = {"leg_L": {"rotation": s(W, A, 0, 0.0, steps=16)}, "leg_R": {"rotation": s(W, A, 0, 0.5, steps=16)},
             "shin_L": {"rotation": s(W, 7, 0, 0.12, steps=16)}, "shin_R": {"rotation": s(W, 7, 0, 0.62, steps=16)},
             "arm_L": {"rotation": s(W, -12, 0, 0.0, steps=16)}, "arm_R": {"rotation": s(W, -12, 0, 0.5, steps=16)},
             "fore_L": {"rotation": s(W, -5, 0, 0.2, steps=16)}, "fore_R": {"rotation": s(W, -5, 0, 0.7, steps=16)},
             "body": {"position": tracks(W, lambda u: (2.2 * math.sin(u * math.tau), -2.4 * abs(math.cos(u * math.tau)), 0), 16), "rotation": s(W, 4.0, 1, 0.0, steps=16)},
             "chest": {"rotation": L.merge(s(W, -3.5, 1, 0.0, steps=16), s(W, 2.0, 0, 0.25, steps=16))},
             "head": {"rotation": s(W, 2.5, 1, 0.5, steps=16)}}
    for i, nm in enumerate(ORB):
        bones[nm] = {"position": tracks(W, lambda u, i=i: (0, 3.0 * math.sin((u + i * 0.25) * math.tau), 0), 16), "rotation": s(W, 24, 1, i * 0.2, steps=16)}
    clips["walk"] = {"loop": True, "length": W, "bones": bones}

    # ---- rise: the stone bursts out of the ground (the root climbs from deep below), both arms up over the head, then both fists slam down at 0.70 s and the dust ring rolls out
    t = [0.0, 0.30, 0.55, 0.70, 0.82, 1.0]
    bones = {"root": {"position": dict(zip(t, [(0, -108, 0), (0, -46, 0), (0, 5, 0), (0, 0, 0), (0, -1.5, 0), (0, 0, 0)]))},
             "arm_L": rot(t, [(40, 0, 6), (135, 0, 10), (150, 0, 6), (-6, 0, 0), (-2, 0, 0), (0, 0, 0)]),
             "arm_R": rot(t, [(40, 0, -6), (135, 0, -10), (150, 0, -6), (-6, 0, 0), (-2, 0, 0), (0, 0, 0)]),
             "fore_L": rot(t, [(20, 0, 0), (30, 0, 0), (24, 0, 0), (-4, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "fore_R": rot(t, [(20, 0, 0), (30, 0, 0), (24, 0, 0), (-4, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "chest": rot(t, [(8, 0, 0), (10, 0, 0), (12, 0, 0), (-12, 0, 0), (-4, 0, 0), (0, 0, 0)]),
             "head": rot(t, [(-8, 0, 0), (-10, 0, 0), (-3, 0, 0), (9, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (0, 0, 0), (0, 3, 0), (0, -5, 0), (0, -1, 0), (0, 0, 0)]))}}
    clips["rise"] = {"loop": False, "length": LENGTHS["rise"], "bones": bones}

    # ---- cast: both fists up over the head, then slam the ground (contact at 0.60 s = tick 12) and rebound
    t = [0.0, 0.25, 0.45, 0.60, 0.74, 0.95, 1.2]
    bones = {"arm_L": rot(t, [(0, 0, 0), (80, 0, 6), (140, 0, 8), (-8, 0, 0), (-3, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "arm_R": rot(t, [(0, 0, 0), (80, 0, -6), (140, 0, -8), (-8, 0, 0), (-3, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "fore_L": rot(t, [(0, 0, 0), (24, 0, 0), (30, 0, 0), (-5, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "fore_R": rot(t, [(0, 0, 0), (24, 0, 0), (30, 0, 0), (-5, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "chest": rot(t, [(0, 0, 0), (5, 0, 0), (12, 0, 0), (-13, 0, 0), (-5, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "head": rot(t, [(0, 0, 0), (-6, 0, 0), (-10, 0, 0), (9, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (0, 1.5, 0), (0, 4, 0), (0, -5, 0), (0, -1, 0), (0, 0, 0), (0, 0, 0)]))}}
    clips["cast"] = {"loop": False, "length": LENGTHS["cast"], "bones": bones}

    # ---- punch: the right fist winds back and drives forward (contact at 0.50 s)
    t = [0.0, 0.28, 0.50, 0.70, 1.0]
    bones = {"arm_R": rot(t, [(0, 0, 0), (-40, -12, -6), (75, 6, 0), (30, 0, 0), (0, 0, 0)]),
             "fore_R": rot(t, [(0, 0, 0), (50, 0, 0), (8, 0, 0), (10, 0, 0), (0, 0, 0)]),
             "chest": rot(t, [(0, 0, 0), (0, 14, 0), (-6, -16, 0), (0, -6, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (0, 0, 2), (0, -2, -3), (0, 0, 0), (0, 0, 0)]))}}
    clips["punch"] = {"loop": False, "length": LENGTHS["punch"], "bones": bones}

    # ---- stomp: the left leg lifts and slams (contact at 0.5 s), the whole body drops with it
    t = [0.0, 0.25, 0.5, 0.65, 0.9]
    bones = {"leg_L": rot(t, [(0, 0, 0), (36, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "shin_L": rot(t, [(0, 0, 0), (-18, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (-1.5, 3, 0), (0, -4, 0), (0, -1, 0), (0, 0, 0)]))},
             "chest": rot(t, [(0, 0, 0), (5, 6, 0), (-5, 0, 0), (0, 0, 0), (0, 0, 0)])}
    clips["stomp"] = {"loop": False, "length": LENGTHS["stomp"], "bones": bones}

    # ---- sink: a last slow breath, the arms sag, the whole body goes down into the ground
    t = [0.0, 0.25, 0.60, 1.0]
    bones = {"root": {"position": dict(zip(t, [(0, 0, 0), (0, 1.5, 0), (0, -50, 0), (0, -108, 0)]))},
             "arm_L": rot(t, [(0, 0, 0), (10, 0, 8), (10, 0, 14), (10, 0, 14)]),
             "arm_R": rot(t, [(0, 0, 0), (10, 0, -8), (10, 0, -14), (10, 0, -14)]),
             "head": rot(t, [(0, 0, 0), (6, 0, 0), (-12, 0, 0), (-12, 0, 0)])}
    clips["sink"] = {"loop": False, "length": LENGTHS["sink"], "bones": bones}
    # the companion walks a little quicker
    if form == "companion":
        clips["walk"]["length"] = 1.2
        clips["walk"]["bones"] = {bn: {tk: {round(tt * 1.2 / 1.6, 4): v for tt, v in fr.items()} for tk, fr in trk.items()} for bn, trk in clips["walk"]["bones"].items()}
    return clips


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    at = make_atlas()
    for form in FORMS:
        if only and form not in only:
            continue
        rows = []
        for lod, fx in ((0, False), (1, False), (2, False), (0, True), (1, True)):
            m = build(form, lod, at, fx)
            if fx and lod == 1:
                m.id = "gnome_%s_fx_lod1" % form
            rows.append((m.id, m.cubes(), m.quads()))
            if not count_only:
                m.save()
                if lod == 0 and not fx:
                    at.save_color(m.id, 1.0)
                elif not fx:
                    at.save_color(m.id, m.tex_scale)
                elif lod == 0:
                    at.save_glow(m.id, 0.5)
        if not count_only:
            at.save_glow("gnome_%s_fx_lod1" % form, 0.5)
            L.write_animations("gnome_" + form, animations(form))
        for r in rows:
            print("%-26s cubes %4d  quads %5d" % r)


if __name__ == "__main__":
    main()
