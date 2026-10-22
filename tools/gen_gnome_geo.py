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
FORMS = {"full": dict(k=1.0, hs=1.0, seed=7100), "companion": dict(k=0.165, hs=1.38, seed=7200)}
HEAD_PIVOT = (0.0, 92.0, -6.0)
LOD_SCALE = {0: 1.0, 1: 0.5, 2: 0.25}


def noz(w, h):
    return np.zeros((h, w, 4), np.float32)


# ====================================================================================================================== the sheet
def make_atlas():
    at = L.Atlas(1024, 1024)
    S = 1000
    at.region("rock_a", 256, 256, lambda w, h: A.stone(w, h, S + 1, w / 4.2, moss=0.45, warm=0.22, cracks=2))
    at.region("rock_b", 256, 256, lambda w, h: A.stone(w, h, S + 2, w / 5.6, warm=0.20, cracks=3, vein=1, tone=0.0))
    at.region("rock_c", 256, 256, lambda w, h: A.stone(w, h, S + 3, w / 3.0, warm=0.16, cracks=1, moss=0.22))
    at.region("rock_top", 256, 256, lambda w, h: A.stone(w, h, S + 4, w / 5.5, tone=0.07, warm=0.12, moss=0.95, moss_top=False))
    at.region("rock_dark", 192, 192, lambda w, h: A.stone(w, h, S + 5, w / 3.6, tone=-0.15, warm=0.10, cracks=2))
    at.region("rock_lt", 256, 256, lambda w, h: A.stone(w, h, S + 16, w / 3.4, tone=0.10, warm=0.30, cracks=1, moss=0.2))
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


def make_face_atlas():
    """The six faces on one small sheet (3 x 2 regions of 160 x 140): the colour sheet and the glow sheet share the layout, so one plate model draws both."""
    at = L.Atlas(480, 280)
    for e in A.EXPRESSIONS:
        at.region("face_" + e, 160, 140, lambda w, h, e=e: A.face_front(w, h, 1012, e))
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
        self.n += 1
        if self.lod == 1 and lod == 0 and self.n % 2 == 0:                  # the near-detail parts are thinned, not dropped: every second one stays on level 1 (about half the quads)
            lod = 1
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
        if tip and self.lod == 0:                                      # a narrower tip turned 45 degrees on top: the point of the crystal
            tc = (c[0], c[1] + h + w * 0.40, c[2])
            self.part(bone, tc, (w * 0.60, w * 0.8, w * 0.60), {"west": (reg, 0), "east": (reg, 0), "north": (reg, 0), "south": (reg, 0), "up": (reg, 0)}, (rot[0], 45.0, rot[2]), piv, 0, comp, head)
            tc2 = (c[0], c[1] + h + w * 0.85, c[2])
            self.part(bone, tc2, (w * 0.30, w * 0.5, w * 0.30), {"west": (reg, 0), "east": (reg, 0), "north": (reg, 0), "south": (reg, 0), "up": (reg, 0)}, (rot[0], 0.0, rot[2]), piv, 0, comp, head)


def build_face(form, expr, atlas):
    """The face plate of one expression: the bones of the body down to the head (same pivots, same rest pose, so every clip drives it) and ONE cube, a thin plate just proud of the head's front face
    with the face of the expression on it. Drawn twice: with the colour sheet (cutout) and with the glow sheet (additive)."""
    b = Builder(form, 0, atlas, False)
    b.m.id = "gnome_%s_face_%s" % (form, expr)
    b.bone("body", (0, 40, 0))
    b.bone("chest", (0, 48, 0), "body")
    b.bone("head", HEAD_PIVOT, "chest", rotation=(-6, 0, 0))
    b.part("head", (0, 102, -22.2), (26, 22, 0.4), {"north": ("face_" + expr, 0)}, head=True)
    return b.m


def build(form, lod, atlas, fx=False):
    b = Builder(form, lod, atlas, fx)
    m = b.m
    P = b.part
    B = b.bone
    big = form == "full"
    X = 20.0                                           # the legs' stance
    SH = 49.0                                          # the shoulders' stance
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
    orbs = [("orb1", (-70, 72, -10), 12, 1), ("orb2", (70, 96, 8), 10, 1), ("orb3", (-36, 124, 16), 8, 0), ("orb4", (44, 40, -42), 10, 0), ("orb5", (-52, 132, -12), 6, 0), ("orb6", (62, 60, -34), 7, 0)]
    for nm, c, s, lv in orbs:
        B(nm, c, "root")

    # ------------------------------------------------------------------ legs: stacked dark boulders on a slab, planted, a clear gap between them
    for sx, t in ((-1, "R"), (1, "L")):
        x = sx * X
        P("leg_" + t, (x, 26, 2), (22, 20, 25), b.stone_faces("dark", down=False), rot=(0, sx * 4, 0), piv=(x, 26, 2), lod=2)
        P("leg_" + t, (x, 27, -12), (18, 12, 5), b.stone_faces("plate", down=False, back=False), rot=(10, 0, 0), piv=(x, 27, -11), lod=1)             # knee plate
        P("leg_" + t, (x + sx * 12.5, 29, 2), (4, 15, 17), b.stone_faces("arm", down=False, front=False, back=False), rot=(0, 0, sx * -8), piv=(x + sx * 12.5, 22, 2), lod=0)
        P("shin_" + t, (x, 10, 2), (20, 15, 22), b.stone_faces("leg", up=False, down=False), lod=2)
        P("shin_" + t, (x, 13, 14), (16, 9, 5), b.stone_faces("plate", down=False, front=False), lod=0)
        P("foot_" + t, (x, 3.5, -2), (28, 7, 36), b.stone_faces("leg", down=False), lod=2)
        for j in range(3):
            P("foot_" + t, (x + (j - 1) * 9.5, 5, -22), (8.5, 10, 9), b.stone_faces("arm", back=False, sides=(j != 1)), lod=0)
        P("foot_" + t, (x + sx * 16.5, 3, 0), (7, 6, 14), b.stone_faces("dark", down=False), lod=0)
    # ------------------------------------------------------------------ pelvis, belt and a skirt of stone plates (the body is a V)
    P("body", (0, 41, 0), (40, 13, 30), b.stone_faces("dark", down=False), lod=2)
    P("body", (0, 45, -15.6), (30, 5, 1.6), {"north": ("bracer", 0), "up": ("rock_dark", b.dens)}, lod=1)
    for sx in (-1, 1):
        P("body", (sx * 22, 36, -10), (9, 9, 12), b.stone_faces("arm", down=False, back=False), lod=0)                       # hip stones
        P("body", (sx * 9, 33, -17), (12, 15, 3.5), b.stone_faces("plate", down=False, back=False), rot=(8, 0, sx * -7), piv=(sx * 9, 40, -17), lod=1)   # skirt plates
    # ------------------------------------------------------------------ chest: the belly, the great breast with its rib plates, the carved circle
    P("chest", (0, 55, 0), (30, 14, 26), b.stone_faces("plate", down=False), lod=2)
    P("chest", (0, 76, 0), (54, 22, 36), b.stone_faces("boulder", down=False), lod=2)
    P("chest", (0, 72, -20.5), (30, 30, 4), {"north": ("circle", 0), "up": ("rock_top", b.dens), "west": ("rock_dark", b.dens), "east": ("rock_dark", b.dens)}, lod=2)
    for sx in (-1, 1):
        P("chest", (sx * 29, 73, 0), (8, 26, 28), b.stone_faces("plate", down=False, front=False), rot=(0, 0, sx * -10), piv=(sx * 29, 60, 0), lod=1)               # rib plates
        P("chest", (sx * 26, 82, -19), (15, 14, 8), b.stone_faces("plate", down=False, back=False), rot=(0, sx * 14, sx * -6), piv=(sx * 26, 82, -19), lod=1)   # pectoral stones
        P("chest", (sx * 22, 58, -15), (11, 10, 6), b.stone_faces("arm", down=False, back=False), lod=0)
    P("chest", (0, 76, 21.5), (48, 24, 7), b.stone_faces("plate", down=False, front=False), lod=1)                            # back plate
    P("chest", (0, 88.5, 6), (38, 4, 26), b.stone_faces("plate", down=False), lod=1)                                          # a ledge of slabs between the shoulders
    P("chest", (0, 86, 23), (24, 20, 8), b.stone_faces("arm", down=False, front=False), rot=(-14, 0, 0), piv=(0, 78, 23), lod=0)                # a hump
    b.shard("chest", (0, 84, 25), 17, 6, (-22, 0, 0), "amber", lod=1)
    b.shard("chest", (-11, 82, 25), 12, 5, (-18, 0, 18), "dia", lod=1, comp=False)
    b.shard("chest", (11, 82, 25), 12, 5, (-18, 0, -18), "amber", lod=0)
    # ------------------------------------------------------------------ head: SMALL next to the shoulders, forward and low between them, a heavy brow, glowing eyes, a short crown of crystals
    P("head", (0, 102, -10), (26, 22, 24), {"north": ("face_front", 0), "west": ("face_side", 0), "east": ("face_side", 0), "up": ("rock_top", b.dens), "south": ("rock_a", b.dens)}, lod=2, head=True)
    P("head", (0, 110.5, -23.5), (29, 5, 8), b.stone_faces("plate", down=False, back=False), lod=1, head=True)                 # the brow overhang
    P("head", (0, 92.5, -19), (18, 6, 9), b.stone_faces("dark", up=False, down=False, back=False), lod=0, head=True)           # jaw
    P("head", (0, 87.5, -5), (14, 9, 14), b.stone_faces("dark", up=False, down=False), lod=1, head=True)                    # neck
    for sx in (-1, 1):
        P("head", (sx * 15, 102, -8), (4.5, 10, 10), b.stone_faces("arm", up=True, back=False, front=False, sides=True), lod=0, head=True)       # ear slabs
    b.shard("head", (0, 113, -9), 10, 5, (0, 0, 0), "amber", lod=2, head=True)
    b.shard("head", (-6, 112.5, -9), 8, 4, (0, 0, 24), "amber", lod=1, head=True)
    b.shard("head", (6, 112.5, -9), 8, 4, (0, 0, -24), "dia", lod=1, head=True)
    b.shard("head", (-10.5, 111, -9), 6, 3.2, (0, 0, 46), "amber", lod=0, head=True)
    b.shard("head", (10.5, 111, -9), 6, 3.2, (0, 0, -46), "amber", lod=0, head=True)
    # ------------------------------------------------------------------ shoulders, arms, fists: broad boulder shoulders, thick arms, HUGE fists
    for sx, t in ((-1, "R"), (1, "L")):
        sa, sf = "arm_" + t, "fore_" + t
        x = sx * SH
        P(sa, (x, 84, 0), (32, 28, 34), b.stone_faces("boulder", down=False), rot=(0, sx * 6, sx * -4), piv=(x, 84, 0), lod=2)
        P(sa, (x + sx * 1, 99, 0), (37, 6, 37), b.stone_faces("plate", down=False), rot=(0, sx * -12, -sx * 10), piv=(x, 99, 0), lod=1)             # the pauldron slab
        P(sa, (x - sx * 6, 101.5, -12), (14, 5, 14), b.stone_faces("arm", down=False), rot=(0, sx * 24, 0), piv=(x, 101, -12), lod=0)           # a loose stone on it
        P(sa, (x + sx * 2, 59, 0), (24, 31, 26), b.stone_faces("arm", up=False, down=False), rot=(0, 0, sx * 3), piv=(x, 74, 0), lod=2)
        P(sa, (x + sx * 2, 72, -14), (20, 5, 3), b.stone_faces("plate", down=False, back=False), lod=0)                                   # a strap of stone across the front
        b.shard(sa, (x + sx * 6, 102, 6), 13, 5.5, (6, 0, -sx * 20), "amber" if sx < 0 else "dia", lod=1)
        b.shard(sa, (x - sx * 3, 103, -8), 9, 4.5, (-10, 0, -sx * 8), "amber", lod=0)
        b.shard(sa, (x + sx * 12, 97, -2), 8, 4, (0, 0, -sx * 42), "amber", lod=0, comp=False)
        P(sa, (x + sx * 2, 101, -19), (13, 14, 0.5), {"north": ("moss", 0), "south": ("moss", 0)}, lod=0, comp=False)                    # hanging moss
        P(sf, (x + sx * 4, 33, -3), (30, 31, 31), b.stone_faces("arm", up=False, down=False), lod=2)
        P(sf, (x + sx * 4, 29.5, -3), (33, 8, 34), {"north": ("bracer", 0), "west": ("bracer", 0), "east": ("bracer", 0), "south": ("bracer", 0)}, lod=1)   # rune cuff
        b.shard(sf, (x + sx * 4, 47, 13), 10, 4.5, (-62, 0, 0), "amber", lod=0, comp=False)
        fx0 = x + sx * 4
        P("fist_" + t, (fx0, 13.5, -7), (40, 28, 40), {**b.stone_faces("boulder", down=False, front=False), "north": ("knuckle", 0)}, lod=2)
        P("fist_" + t, (fx0, 28, -7), (42, 4, 42), b.stone_faces("plate", down=False), lod=1)
        for j in range(4):
            P("fist_" + t, (fx0 + (j - 1.5) * 9.6, 17, -29), (9, 12, 7), {"north": ("knuckle", 0), "up": ("rock_top", b.dens), "west": ("rock_a", b.dens), "east": ("rock_a", b.dens)}, lod=1)
        P("fist_" + t, (fx0 - sx * 22, 13, -16), (8, 11, 17), b.stone_faces("arm", down=False, back=False), lod=0)             # thumb
    # ------------------------------------------------------------------ more layers: plates on the arms and the back, a gorget round the head, guards on the shins, small boulders on the shoulders
    P("chest", (0, 66, 25.5), (40, 10, 6), b.stone_faces("plate", down=False, front=False), rot=(6, 0, 0), piv=(0, 66, 25), lod=1)
    P("chest", (0, 57, 22), (30, 8, 6), b.stone_faces("plate", down=False, front=False), rot=(-6, 0, 0), piv=(0, 57, 22), lod=0)
    P("body", (0, 41, -16.5), (9, 9, 3), b.stone_faces("plate", down=False, back=False, sides=False), rot=(0, 0, 45), piv=(0, 41, -16.5), lod=0)                 # the belt's buckle: a diamond
    for sx, t in ((-1, "R"), (1, "L")):
        sa, sf = "arm_" + t, "fore_" + t
        x = sx * SH
        P(sa, (x + sx * 14, 61, 0), (6, 23, 19), b.stone_faces("plate", down=False, front=False, back=False), rot=(0, 0, sx * -9), piv=(x + sx * 14, 72, 0), lod=1)
        P(sa, (x + sx * 13, 49, 4), (5, 14, 12), b.stone_faces("arm", down=False, front=False, back=False), rot=(0, 0, sx * 7), piv=(x + sx * 13, 56, 4), lod=0)
        P("head", (sx * 17, 92, -4), (12, 10, 20), b.stone_faces("plate", down=False, back=False), rot=(0, 0, sx * -18), piv=(sx * 17, 86, -4), lod=1, head=True)         # the gorget
        P("shin_" + t, (sx * X, 15, -9.5), (15, 11, 4), b.stone_faces("plate", down=False, back=False, sides=False), rot=(-10, 0, 0), piv=(sx * X, 20, -9.5), lod=1)       # a shin guard
        for j, (dx, dz, w) in enumerate(((-9, 8, 9), (8, 10, 7), (0, -9, 8))):
            P(sa, (x + dx, 103.5 + (j % 2), dz), (w, w * 0.7, w), b.stone_faces("boulder", down=False), rot=(10 * j, 25 * j + 10, 8), piv=(x + dx, 103.5, dz), lod=0, comp=(j == 0))     # small boulders on the shoulder
        b.shard("fist_" + t, (sx * (SH + 4) + sx * 6, 29, 4), 8, 4, (0, 0, -sx * 14), "amber", lod=0, comp=False)
        b.shard(sf, (x + sx * 19, 40, 2), 9, 4, (0, 0, -sx * 50), "dia" if sx > 0 else "amber", lod=0, comp=False)
        P(sa, (x + sx * 2, 98, 20), (14, 14, 0.5), {"north": ("moss", 0), "south": ("moss", 0)}, lod=0, comp=False)
    # ------------------------------------------------------------------ floating stones (a few, they bob and turn)
    for nm, c, s, lv in orbs:
        P(nm, c, (s, s, s), b.stone_faces("boulder", down=True), rot=(18, 30, 12), piv=c, lod=lv, comp=(lv == 1))
    # ------------------------------------------------------------------ the rubble ring where the stone heaves up (the big form only)
    if big:
        for i in range(9):
            a = i / 9.0 * math.tau + 0.3
            r = 58 + 9 * ((i * 5) % 3)
            c = (math.cos(a) * r, 2.5, math.sin(a) * r)
            P("root", c, (9 + (i % 3) * 3, 5, 9), b.stone_faces("dark", down=False), rot=(0, math.degrees(a), 0), piv=c, lod=0)
    # ------------------------------------------------------------------ glow-only model: eyes, the circle, rune bands, veins
    G = b.glow
    G("chest", (0, 72, -22.62), (30, 30, 0.2), {"north": ("circle", 0)}, lod=1)
    G("body", (0, 45, -16.5), (30, 5, 0.2), {"north": ("bracer", 0)}, lod=0)
    for sx, t in ((-1, "R"), (1, "L")):
        sf = "fore_" + t
        x = sx * (SH + 4)
        G(sf, (x, 29.5, -20.2), (33, 8, 0.2), {"north": ("bracer", 0)}, lod=1)
        G(sf, (x + sx * 16.7, 29.5, -3), (0.2, 8, 34), {"west": ("bracer", 0)} if sx > 0 else {"east": ("bracer", 0)}, lod=0)
        G("arm_" + t, (sx * SH + sx * 14.8, 59, 0), (0.2, 27, 22), {"west": ("veins", 0)} if sx > 0 else {"east": ("veins", 0)}, lod=1)
        G("fist_" + t, (x, 13.5, -33.2), (36, 22, 0.2), {"north": ("veins", 0)}, lod=0)
        G("leg_" + t, (sx * X, 26, -11.2), (17, 15, 0.2), {"north": ("veins", 0)}, lod=0)
    G("chest", (0, 76, 25.2), (40, 22, 0.2), {"south": ("veins", 0)}, lod=1)
    return m


# ====================================================================================================================== animations
# Rotation convention of the files (checked in the preview, scenes gnome_dev_full_rise): +x swings the part BELOW the pivot forward (-z) and so tips the part above it back. A hanging arm
# or leg: + = forward / up. An upright part (chest, head): + = leans back, - = leans forward. Heights (position y) are in model units.
TICK = 0.05
SLAM_T = 0.70                                         # the rise: both fists land at 0.70 s (tick 14)
CAST_T = 0.60                                         # the cast: contact at 0.60 s (tick 12)
LENGTHS = {"rise": 1.0, "cast": 1.2, "sink": 1.0, "punch": 1.0, "stomp": 0.9, "slam": 1.1, "shrug": 1.1, "pound": 1.8, "kneel": 2.0}
GUARD_Z, GUARD_Y = -26.0, 60.0                         # the crossed arms: the upper arms swing inward, the forearms turn across the chest (checked in the preview)


def tracks(clip_len, fn, steps=12):
    return {i / steps * clip_len: fn(i / steps) for i in range(steps + 1)}


def rot(ts, vals):
    return {"rotation": dict(zip(ts, vals))}


def animations(form):
    clips = {}
    s = L.sine
    ORB = ("orb1", "orb2", "orb3", "orb4", "orb5", "orb6")

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
    bones = {"leg_L": rot(t, [(0, 0, 0), (58, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "shin_L": rot(t, [(0, 0, 0), (-34, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (-2.5, 6, 0), (0, -6, 0), (0, -1.5, 0), (0, 0, 0)]))},
             "chest": rot(t, [(0, 0, 0), (5, 6, 0), (-5, 0, 0), (0, 0, 0), (0, 0, 0)])}
    clips["stomp"] = {"loop": False, "length": LENGTHS["stomp"], "bones": bones}

    # ---- sink: a last slow breath, the arms sag, the whole body goes down into the ground
    t = [0.0, 0.25, 0.60, 1.0]
    bones = {"root": {"position": dict(zip(t, [(0, 0, 0), (0, 1.5, 0), (0, -50, 0), (0, -108, 0)]))},
             "arm_L": rot(t, [(0, 0, 0), (10, 0, 8), (10, 0, 14), (10, 0, 14)]),
             "arm_R": rot(t, [(0, 0, 0), (10, 0, -8), (10, 0, -14), (10, 0, -14)]),
             "head": rot(t, [(0, 0, 0), (6, 0, 0), (-12, 0, 0), (-12, 0, 0)])}
    clips["sink"] = {"loop": False, "length": LENGTHS["sink"], "bones": bones}
    # ---- slam: ONE fist drops on the ground like a hammer (a mid page): the right arm up, then down at 0.50 s (tick 10)
    t = [0.0, 0.30, 0.50, 0.65, 0.85, 1.1]
    bones = {"arm_R": rot(t, [(0, 0, 0), (105, 0, -6), (-8, 0, 0), (-3, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "fore_R": rot(t, [(0, 0, 0), (34, 0, 0), (-4, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "chest": rot(t, [(0, 0, 0), (9, 0, 0), (-11, 0, 0), (-3, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "head": rot(t, [(0, 0, 0), (-6, 0, 0), (8, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (0, 2, 0), (0, -4, 0), (0, -1, 0), (0, 0, 0), (0, 0, 0)]))}}
    clips["slam"] = {"loop": False, "length": LENGTHS["slam"], "bones": bones}

    # ---- shrug: the hit rolls off the stone: the shoulders heave up, the head rocks, the chest jerks back and settles (22 ticks)
    t = [0.0, 0.10, 0.30, 0.55, 0.80, 1.1]
    bones = {"arm_L": {"position": dict(zip(t, [(0, 0, 0), (0, 6, 0), (0, 5, 0), (0, 2, 0), (0, 0, 0), (0, 0, 0)])), "rotation": dict(zip(t, [(0, 0, 0), (0, 0, 10), (0, 0, 8), (0, 0, 3), (0, 0, 0), (0, 0, 0)]))},
             "arm_R": {"position": dict(zip(t, [(0, 0, 0), (0, 6, 0), (0, 5, 0), (0, 2, 0), (0, 0, 0), (0, 0, 0)])), "rotation": dict(zip(t, [(0, 0, 0), (0, 0, -10), (0, 0, -8), (0, 0, -3), (0, 0, 0), (0, 0, 0)]))},
             "chest": rot(t, [(0, 0, 0), (9, 4, 0), (5, -4, 0), (2, 2, 0), (0, 0, 0), (0, 0, 0)]),
             "head": rot(t, [(0, 0, 0), (-4, 10, 7), (-2, -8, 5), (0, 4, 0), (0, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (0, -2, 1.5), (0, -1, 0.5), (0, 0, 0), (0, 0, 0), (0, 0, 0)]))}}
    clips["shrug"] = {"loop": False, "length": LENGTHS["shrug"], "bones": bones}

    # ---- guard: the arms crossed over the chest, the head level and stern, a slow breath (a loop, held while the master is in danger)
    GT = 3.0
    ga = {"arm_L": (62, 0, GUARD_Z), "arm_R": (62, 0, -GUARD_Z), "fore_L": (88, GUARD_Y, 0), "fore_R": (88, -GUARD_Y, 0)}
    bones = {k: {"rotation": tracks(GT, lambda u, v=v: (v[0] + 1.5 * math.sin(u * math.tau), v[1], v[2]), 12)} for k, v in ga.items()}
    bones["chest"] = {"rotation": tracks(GT, lambda u: (-3 + 1.5 * math.sin(u * math.tau), 0, 0), 12), "scale": tracks(GT, lambda u: (1.0, 1.0 + 0.012 * math.sin(u * math.tau), 1.0), 12)}
    bones["head"] = {"rotation": tracks(GT, lambda u: (2, 4 * math.sin(u * math.tau), 0), 12)}
    for i, nm in enumerate(ORB):
        bones[nm] = {"position": tracks(GT, lambda u, i=i: (0, 2.0 * math.sin((u + i * 0.25) * math.tau), 0), 12)}
    clips["guard"] = {"loop": True, "length": GT, "bones": bones}

    # ---- pound: the victory chest-pound: the head tips back, the fists hammer the chest one after the other, twice each (1.8 s)
    t = [0.0, 0.25, 0.40, 0.55, 0.70, 0.85, 1.00, 1.15, 1.30, 1.8]
    up = (50, 0, 0)
    bones = {"arm_L": rot(t, [(0, 0, 0), (50, 0, GUARD_Z * 0.5), (60, 0, GUARD_Z * 0.5), (50, 0, GUARD_Z * 0.5), (0, 0, 0), (0, 0, 0), (60, 0, GUARD_Z * 0.5), (50, 0, GUARD_Z * 0.5), (40, 0, 4), (0, 0, 0)]),
             "arm_R": rot(t, [(0, 0, 0), (0, 0, 0), (0, 0, 0), (50, 0, -GUARD_Z * 0.5), (60, 0, -GUARD_Z * 0.5), (50, 0, -GUARD_Z * 0.5), (0, 0, 0), (0, 0, 0), (40, 0, -4), (0, 0, 0)]),
             "fore_L": rot(t, [(0, 0, 0), (95, 0, 0), (110, 0, 0), (95, 0, 0), (20, 0, 0), (20, 0, 0), (110, 0, 0), (95, 0, 0), (60, 0, 0), (0, 0, 0)]),
             "fore_R": rot(t, [(0, 0, 0), (20, 0, 0), (20, 0, 0), (95, 0, 0), (110, 0, 0), (95, 0, 0), (20, 0, 0), (20, 0, 0), (60, 0, 0), (0, 0, 0)]),
             "chest": rot(t, [(0, 0, 0), (7, 0, 0), (5, 0, 0), (7, 0, 0), (5, 0, 0), (7, 0, 0), (5, 0, 0), (7, 0, 0), (4, 0, 0), (0, 0, 0)]),
             "head": rot(t, [(0, 0, 0), (12, 0, 0), (14, 0, 0), (12, 0, 0), (14, 0, 0), (12, 0, 0), (14, 0, 0), (12, 0, 0), (8, 0, 0), (0, 0, 0)]),
             "body": {"position": dict(zip(t, [(0, 0, 0), (0, 1, 0), (0, -1.5, 0), (0, 1, 0), (0, -1.5, 0), (0, 1, 0), (0, -1.5, 0), (0, 1, 0), (0, 0, 0), (0, 0, 0)]))}}
    clips["pound"] = {"loop": False, "length": LENGTHS["pound"], "bones": bones}

    # ---- doze: the sleepy settle: the whole body sinks into a crouch, the head droops and nods, the arms hang loose, the stones barely move (a loop)
    DT = 4.0
    bones = {"body": {"position": tracks(DT, lambda u: (0, -9 + 0.8 * math.sin(u * math.tau), 0), 12)},
             "leg_L": {"rotation": tracks(DT, lambda u: (38, 0, 0), 12)}, "leg_R": {"rotation": tracks(DT, lambda u: (38, 0, 0), 12)},
             "shin_L": {"rotation": tracks(DT, lambda u: (-30, 0, 0), 12)}, "shin_R": {"rotation": tracks(DT, lambda u: (-30, 0, 0), 12)},
             "chest": {"rotation": tracks(DT, lambda u: (-5 + 1.5 * math.sin(u * math.tau), 0, 0), 12), "scale": tracks(DT, lambda u: (1.0, 1.0 + 0.02 * math.sin(u * math.tau), 1.0 + 0.012 * math.sin(u * math.tau)), 12)},
             "head": {"rotation": tracks(DT, lambda u: (-14 + 5 * max(0.0, math.sin(u * math.tau * 2)) ** 2, 0, 3 * math.sin(u * math.tau)), 12)},
             "arm_L": {"rotation": tracks(DT, lambda u: (10, 0, 6), 12)}, "arm_R": {"rotation": tracks(DT, lambda u: (10, 0, -6), 12)}}
    for i, nm in enumerate(ORB):
        bones[nm] = {"position": tracks(DT, lambda u, i=i: (0, 0.8 * math.sin((u + i * 0.25) * math.tau) - 6, 0), 12)}
    clips["doze"] = {"loop": True, "length": DT, "bones": bones}

    # ---- kneel: it comes up out of a kneel, as if rising from the earth (2 s): a deep kneel (the thighs folded, the head bowed, a fist on the ground), then it rises, the stones lifting after it
    t = [0.0, 0.50, 0.90, 1.30, 1.65, 2.0]
    bones = {"root": {"position": dict(zip(t, [(0, -14, 0), (0, -14, 0), (0, -8, 0), (0, 2, 0), (0, 0, 0), (0, 0, 0)]))},
             "body": {"position": dict(zip(t, [(0, -14, 0), (0, -14, 0), (0, -8, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)]))},
             "leg_L": rot(t, [(70, 0, 0), (70, 0, 0), (45, 0, 0), (10, 0, 0), (0, 0, 0), (0, 0, 0)]), "leg_R": rot(t, [(40, 0, 0), (40, 0, 0), (28, 0, 0), (6, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "shin_L": rot(t, [(-110, 0, 0), (-110, 0, 0), (-60, 0, 0), (-10, 0, 0), (0, 0, 0), (0, 0, 0)]), "shin_R": rot(t, [(-70, 0, 0), (-70, 0, 0), (-40, 0, 0), (-6, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "chest": rot(t, [(-22, 0, 0), (-22, 0, 0), (-12, 0, 0), (3, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "head": rot(t, [(-28, 0, 0), (-28, 0, 0), (-14, 0, 0), (6, 0, 0), (0, 0, 0), (0, 0, 0)]),
             "arm_L": rot(t, [(30, 0, 6), (30, 0, 6), (18, 0, 4), (0, 0, 0), (0, 0, 0), (0, 0, 0)]), "arm_R": rot(t, [(30, 0, -6), (30, 0, -6), (18, 0, -4), (0, 0, 0), (0, 0, 0), (0, 0, 0)])}
    clips["kneel"] = {"loop": False, "length": LENGTHS["kneel"], "bones": bones}
    # the positions above are in units of the FULL form; the companion's models are authored at k times that size, so its bobs and crouches shrink with it (the root of the rise and the sink stays:
    # it only has to go far enough under the ground)
    if form == "companion":
        k = FORMS[form]["k"]
        for name, c in clips.items():
            for bone, trk in c["bones"].items():
                if "position" in trk and not (bone == "root" and name in ("rise", "sink")):
                    trk["position"] = {tt: tuple(v * k for v in vec) for tt, vec in trk["position"].items()}
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
    fa = make_face_atlas()
    if not count_only:
        fa.save_color("gnome_faces", 1.0)
        fa.save_glow("gnome_faces_fx", 1.0)
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
        for expr in A.EXPRESSIONS:
            fm = build_face(form, expr, fa)
            rows.append((fm.id, fm.cubes(), fm.quads()))
            if not count_only:
                fm.save("gnome")
        if not count_only:
            at.save_glow("gnome_%s_fx_lod1" % form, 0.5)
            L.write_animations("gnome_" + form, animations(form))
        for r in rows:
            print("%-26s cubes %4d  quads %5d" % r)


if __name__ == "__main__":
    main()
