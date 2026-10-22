"""The Spirit of Light, both forms (docs/magic/spirit_light.md): the golden lion cub (the companion form) and the miniature seraph (the powered form). Models (Bedrock geo, 1/16 block units, y up,
front -z), a hand-painted cel-shaded sheet each (tools/gen_spirit_light_art.py), animations, three levels of detail per form and a glow-only (additive) model:

    python3 -B tools/gen_spirit_light_geo.py            writes assets/nusmp/{geo,animations,textures}/entity/spiritlight_{cub,seraph}[_lod1|_lod2|_fx]*
    python3 -B tools/gen_spirit_light_geo.py --count    prints the quads of every model without writing anything
    python3 -B tools/gen_spirit_light_geo.py cub        only one form

    spiritlight_cub          LOD 0 (full)          spiritlight_seraph          LOD 0
    spiritlight_cub_lod1     LOD 1 (about half)    spiritlight_seraph_lod1     LOD 1
    spiritlight_cub_lod2     LOD 2 (low poly)      spiritlight_seraph_lod2     LOD 2
    spiritlight_cub_fx       the glow-only model   spiritlight_seraph_fx       halo, eyes, rings, core, prism; drawn additive on the two nearer levels

Every cube FACE has its own painted region (the detail is texture, the geometry stays low); a face that is not given costs nothing. The levels of detail are separate models and separate sheets of
lower density; the bones are the same names in every model (the animation file of the full model drives them all; a bone a LOD does not have is ignored). Clips: idle (loop), cast (once).
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spirit_light_lib as L  # noqa: E402
import gen_spirit_light_art as A  # noqa: E402
import gen_spirit_light_anim as AN  # noqa: E402

DENSITY = {"cub": (4.0, 2.8, 1.5), "seraph": (3.2, 2.2, 1.2)}        # texels per model unit, per level of detail
SHEET_W = {"cub": (512, 384, 192), "seraph": (512, 384, 192)}
FX_DENSITY = {"cub": 2.0, "seraph": 1.6}


class B:
    """A builder for one model: the cube helper knows the level of detail. tier = the highest level number the cube is still built at (2 = always, 1 = not in the low model, 0 = full model only)."""

    def __init__(self, ident, form, lod, fx=False):
        self.lod, self.fx, self.form = lod, fx, form
        d = FX_DENSITY[form] if fx else DENSITY[form][lod]
        w = 256 if fx else SHEET_W[form][lod]
        self.sheet = L.Sheet(w, d)
        self.m = L.Model(ident, self.sheet)

    def bone(self, name, pivot, parent=None, rotation=None):
        self.m.bone(name, pivot, parent, rotation)

    def cube(self, bone, origin, size, faces, rot=None, pivot=None, tier=2, drop=None):
        """A colour cube (not in the glow-only model). drop = faces to skip at the lower levels (the ones nobody sees)."""
        if self.fx or self.lod > tier:
            return None
        if drop and self.lod >= 1:
            faces = {k: v for k, v in faces.items() if k not in drop}
            if "*" in faces:
                for f in drop:
                    faces[f] = None
        return self.m.cube(bone, origin, size, faces, rot, pivot)

    def glow(self, bone, origin, size, faces, rot=None, pivot=None, tier=1):
        """A glow cube (the glow-only model only)."""
        if not self.fx or self.lod > tier:
            return None
        return self.m.cube(bone, origin, size, faces, rot, pivot)


def finish(b, ident):
    """Renders the sheet, writes the model and the texture."""
    arr = b.sheet.render(A.paint, bleed=not b.fx)
    b.sheet.save_png(ident, arr)
    b.m.save()
    return b.m


# ====================================================================================================================== the lion cub
def cub(lod, fx=False):
    ident = "spiritlight_cub" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    b = B(ident, "cub", lod, fx)
    fur = lambda pal_="cub", **o: ("fur", dict(pal=pal_, **o))
    cream = ("fur", dict(pal="cream", strands=0.5))
    mane = lambda **o: ("fur", dict(pal="mane", tuft=0.20, **o))
    manetip = lambda **o: ("fur", dict(pal="mane", tuft=0.17, points=3, tuft_edge="top", **o))
    tuft_ = lambda **o: ("fur", dict(pal="tuft", tuft=0.20, **o))
    HY, HZ = 29.5, -8.0                                                                  # the head's centre (the mane radiates from it)

    b.bone("body", (0, 10, 4))
    # ------------------------------------------------------------------ hindquarters and torso (a sitting cub)
    b.cube("body", (-8.5, 1, 1), (17, 10, 13), {"up": fur(), "north": fur(), "south": fur(), "west": fur(), "east": fur(), "down": None})
    for sx, tag in ((1, "west"), (-1, "east")):
        ox = 6.5 if sx > 0 else -11.0
        b.cube("body", (ox, 1.2, 2.5), (4.5, 10, 11), {"up": fur(), "north": fur(), "south": fur(), tag: fur(), "down": None, ("east" if sx > 0 else "west"): None}, tier=1)
        b.cube("body", (5.5 if sx > 0 else -11.0, 0, -3.5), (5.5, 2.4, 8), {"up": ("paw", {}), "north": ("paw", {}), "west": fur("paw"), "east": fur("paw"), "south": None, "down": None}, tier=1)
    b.cube("body", (-7, 5, -6), (14, 15, 12), {"up": fur(), "north": fur(), "south": fur(), "west": fur(), "east": fur(), "down": None})
    b.cube("body", (-4.6, 6, -7.3), (9.2, 12, 1.4), {"north": cream, "up": cream, "west": cream, "east": cream, "south": None, "down": None}, tier=1)           # the pale chest
    # ------------------------------------------------------------------ the forelegs and paws (bones, so they can knead and raise)
    for sx, tag in ((1, "L"), (-1, "R")):
        bn = "arm_" + tag
        b.bone(bn, (sx * 4.0, 12, -5.5), "body")
        ox = 1.8 if sx > 0 else -6.2
        b.cube(bn, (ox, 1.4, -8.6), (4.4, 11, 4.4), {"north": fur(), "south": None, "west": fur(), "east": fur(), "up": None, "down": None})
        b.cube(bn, (ox - 0.5, 0, -12.4), (5.4, 2.4, 7.2), {"up": ("paw", {}), "north": ("paw", {}), "west": fur("paw"), "east": fur("paw"), "south": None, "down": None})
        if lod == 0:
            b.cube(bn, (ox - 0.3, 7.0, -9.2), (5.0, 3.4, 1.0), {"north": fur(strands=0.6), "up": None, "south": None, "west": None, "east": None, "down": None})   # a light cuff of fur
    # ------------------------------------------------------------------ the head (big: a cub's)
    b.bone("head", (0, 21, -5), "body")
    b.cube("head", (-11, 20, -17), (22, 19, 18), {"north": ("cub_head_front", {}), "up": fur(), "south": fur(), "west": fur(), "east": fur(), "down": None})
    b.cube("head", (-5.0, 22.2, -22.4), (10, 7, 5.4), {"north": ("cub_muzzle_front", {}), "up": cream, "west": cream, "east": cream, "south": None, "down": ("mouth_in", {})}, drop=("south",))
    b.cube("head", (-2.3, 28.0, -23.6), (4.6, 2.8, 1.4), {"north": ("cub_nose", {}), "up": ("plain", dict(pal="cub", tone="deep")), "west": ("plain", dict(pal="cub", tone="deep")), "east": ("plain", dict(pal="cub", tone="deep")), "south": None, "down": None}, tier=1)
    b.bone("jaw", (0, 22.2, -15), "head")
    b.cube("jaw", (-4.3, 19.8, -21.4), (8.6, 2.8, 6.2), {"up": ("mouth_in", {}), "north": cream, "west": cream, "east": cream, "down": cream, "south": None})
    # the faces: the base face is painted on the head (happy); the other moods are thin overlay slabs just in front of it (and glow slabs in the glow-only model), one bone each. Every clip scales the
    # bones of the faces it does not show to nothing (anim.py), so a pose and its face always go together.
    b.bone("face_base", (0, 29.5, -17.3), "head")
    for mood in ("sleepy", "fierce", "surprised"):
        b.bone("face_" + mood, (0, 29.5, -17.3), "head")
        b.cube("face_" + mood, (-11, 20, -17.24), (22, 19, 0.1), {"north": ("cub_head_front", dict(mood=mood))}, tier=1)
    for sx, tag in ((1, "L"), (-1, "R")):                                              # ears
        bn = "ear_" + tag
        b.bone(bn, (sx * 7.8, 38.0, -7.5), "head", rotation=(0, 0, -sx * 10))
        ox = 5.2 if sx > 0 else -10.8
        b.cube(bn, (ox, 38.6, -9), (5.6, 5.8, 3), {"north": ("ear_front", {}), "up": fur("cub"), "south": fur(), "west": fur(), "east": fur(), "down": None})
    for sx in (1, -1):                                                                 # cheek ruffs
        ox = 10.0 if sx > 0 else -13.4
        b.cube("head", (ox, 21, -15), (3.4, 8, 7), {"north": cream, "up": cream, "west": cream if sx > 0 else None, "east": None if sx > 0 else cream, "south": None, "down": cream}, tier=1)
    # ------------------------------------------------------------------ the mane: tufts radiating from behind the head (a sunburst from the front), each bone shimmering as one
    b.bone("mane_back", (0, HY, HZ + 8), "head")
    nA = {0: 12, 1: 12, 2: 8}[lod]
    for k in range(nA):                                                                # the front ring: it sits inside the depth of the head, so it shows round the face
        ang = k * 360.0 / nA
        L_ = 16.0 + (2.5 if k % 2 else 0.0)
        b.cube("mane_back", (-4.4, HY, -10.0), (8.8, L_, 4.4), {"north": manetip(), "south": manetip(), "up": manetip(strands=0.5)}, rot=(0, 0, ang), pivot=(0, HY, -10.0))
    if lod <= 1:
        nB = 12 if lod == 0 else 8
        for k in range(nB):                                                            # the middle ring, offset half a step
            ang = (k + 0.5) * 360.0 / nB
            b.cube("mane_back", (-4.0, HY, -5.4), (8.0, 14.5 + (2.0 if k % 2 else 0.0), 3.8), {"north": manetip(), "south": manetip(), "up": manetip(strands=0.5)}, rot=(0, 0, ang), pivot=(0, HY, -5.4))
    if lod == 0:
        for k in range(8):                                                             # the back ring: the cloak of the mane seen from behind
            ang = (k + 0.25) * 45.0
            b.cube("mane_back", (-3.0, HY, -0.5), (6.0, 13.0 + (2.0 if k % 2 else 0.0), 3.4), {"north": manetip(), "south": manetip(), "up": manetip(strands=0.5)}, rot=(0, 0, ang), pivot=(0, HY, -0.5))
    b.bone("mane_L", (10.5, HY, HZ), "head")
    b.bone("mane_R", (-10.5, HY, HZ), "head")
    for sx, tag in ((1, "L"), (-1, "R")):                                              # the cheek tufts: seen from the side they give the mane depth
        if lod >= 2:
            continue
        for k, ang in enumerate((78.0, 102.0) if sx > 0 else (-78.0, -102.0)):
            b.cube("mane_" + tag, (-2.6, HY, -9.0), (5.2, 14.5, 5.0), {"north": manetip(), "south": manetip(), "up": manetip(strands=0.5), "west": manetip() if sx > 0 else None, "east": None if sx > 0 else manetip()}, rot=(0, 0, ang), pivot=(0, HY, -9.0), tier=1)
    b.bone("mane_top", (0, 39, -8), "head")
    b.bone("mane_low", (0, 22, 3), "body")
    b.cube("mane_low", (-8.5, 9, 0.5), (17, 11, 7), {"north": mane(), "south": mane(), "west": mane(), "east": mane(), "up": None, "down": None}, tier=1)
    b.bone("mane_chest", (0, 21, -7), "body")
    b.cube("mane_chest", (-8.0, 15.5, -9.6), (16, 7, 4.6), {"north": mane(), "west": mane(), "east": mane(), "up": mane(), "south": None, "down": None}, tier=1)
    # ------------------------------------------------------------------ the tail: three fat segments curling up, and a tuft
    segs = {0: 3, 1: 2, 2: 1}[lod]
    parent, y, z = "body", 4.0, 14.0
    seglen = 8.0 * 3 / segs
    for i in range(segs):
        bn = "t%d" % i
        b.bone(bn, (0, y, z), parent, rotation=(-14 if i else 8, 0, 0))
        w = 5.2 - i * 0.4
        b.cube(bn, (-w / 2, y - w / 2, z), (w, w, seglen), {"up": fur(), "west": fur(), "east": fur(), "down": fur(), "north": None, "south": None})
        parent = bn
        z += seglen * 0.80
        y += seglen * 0.22
    b.bone("tuft", (0, y, z), parent, rotation=(-16, 0, 0))
    b.cube("tuft", (-4.0, y - 3.6, z - 0.5), (8.0, 8.4, 10.0), {"north": tuft_(), "south": tuft_(), "up": tuft_(), "west": tuft_(), "east": tuft_(), "down": None})
    if lod == 0:
        b.cube("tuft", (-2.6, y - 4.4, z + 8.5), (5.2, 6.4, 4.4), {"south": tuft_(), "west": tuft_(), "east": tuft_(), "up": tuft_(), "north": None, "down": None})
    # ------------------------------------------------------------------ the halo: a thin ring above the head
    b.bone("halo_tilt", (0, 46, -8), "head", rotation=(0, 0, 0))
    b.bone("halo", (0, 46, -8), "halo_tilt")
    b.cube("halo", (-10.5, 45.6, -18.5), (21, 0.8, 21), {"up": ("ring", dict(eyes=0, ticks=12, inner=0.78, key="halo_c")), "down": ("ring", dict(eyes=0, ticks=12, inner=0.78, key="halo_c"))})
    # ------------------------------------------------------------------ the glow-only model
    b.glow("face_base", (-11, 20, -17.4), (22, 19, 0.1), {"north": ("fx_eye", dict(mood="happy"))})
    for mood in ("fierce", "surprised"):
        b.glow("face_" + mood, (-11, 20, -17.55), (22, 19, 0.1), {"north": ("fx_eye", dict(mood=mood))})
    b.glow("halo", (-15, 45.4, -23), (30, 0.1, 30), {"up": ("fx_ring", dict(r=0.72, width=0.07, beads=18, key="halo_fx")), "down": ("fx_ring", dict(r=0.72, width=0.07, beads=18, key="halo_fx"))})
    for (bn, ox, y_, z_, s) in (("mane_L", 17.5, 30.0, -8.0, 5.0), ("mane_R", -17.5, 30.0, -8.0, 5.0)):
        b.glow(bn, (ox - 0.05, y_ - s / 2, z_ - s / 2), (0.1, s, s), {"west": ("fx_spark", {"key": "spark"}), "east": ("fx_spark", {"key": "spark"})}, tier=1)
    b.glow("mane_back", (-3.0, HY + 17.5, 0.5), (6.0, 6.0, 0.1), {"north": ("fx_spark", {"key": "spark"}), "south": ("fx_spark", {"key": "spark"})}, tier=1)
    b.glow("tuft", (-3, y - 1.0, z + 9.5), (6.0, 6.0, 0.1), {"north": ("fx_spark", {"key": "spark"}), "south": ("fx_spark", {"key": "spark"})}, tier=1)
    b.glow("body", (-3.6, 8, -7.9), (7.2, 9, 0.1), {"north": ("fx_glow", dict(sigma=0.5, peak=0.55, key="chest")), "south": ("fx_glow", dict(sigma=0.5, peak=0.55, key="chest"))}, tier=0)
    return finish(b, ident)


# ====================================================================================================================== the seraph
WING_SIGN = -1.0        # a blade of the model's left wing (+x) turns from +y toward +x: that is a negative rotation about z (checked in the headless preview)


def seraph(lod, fx=False):
    ident = "spiritlight_seraph" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    b = B(ident, "seraph", lod, fx)
    cl = lambda **o: ("cloth", dict(**o))
    HEAD_Y = 46.0

    b.bone("body", (0, 24, 0))
    # ------------------------------------------------------------------ the robe: tiers of pleated cloth, widest at the hem, a gold hem band, a sash of light down the front
    b.bone("robe_low", (0, 12, 0), "body")
    b.cube("robe_low", (-10, 0, -7), (20, 7.5, 14), {"north": cl(pleats=6, trim=0.32, pal="robe"), "south": cl(pleats=6, trim=0.32), "west": cl(pleats=5, trim=0.32), "east": cl(pleats=5, trim=0.32), "up": cl(), "down": cl()})
    b.cube("robe_low", (-8.8, 7.5, -6.3), (17.6, 8, 12.6), {"north": cl(pleats=5), "south": cl(pleats=5), "west": cl(pleats=4), "east": cl(pleats=4), "up": cl(), "down": None}, tier=1)
    b.bone("robe_mid", (0, 24, 0), "body")
    b.cube("robe_mid", (-7.6, 15.5, -5.6), (15.2, 9, 11.2), {"north": cl(pleats=4, trim=0.18), "south": cl(pleats=4, trim=0.18), "west": cl(pleats=3), "east": cl(pleats=3), "up": cl(), "down": None})
    b.cube("robe_mid", (-6.2, 24.5, -4.8), (12.4, 9, 9.6), {"north": cl(pleats=3), "south": cl(pleats=3), "west": cl(pleats=3), "east": cl(pleats=3), "up": cl(), "down": None})
    b.cube("robe_mid", (-1.9, 5.5, -7.7), (3.8, 28, 0.8), {"north": ("cloth", dict(pal="gold", pleats=1, glyphs=7, edges="lr")), "west": None, "east": None, "up": None, "south": None, "down": None}, tier=1)      # the sash
    b.bone("chest", (0, 33.5, 0), "body")
    b.cube("chest", (-7.8, 33.4, -5), (15.6, 9.8, 10), {"north": cl(pleats=4, toptrim=0.14), "south": cl(pleats=4), "west": cl(pleats=3), "east": cl(pleats=3), "up": cl(), "down": None})
    for sx in (1, -1):
        ox = 7.0 if sx > 0 else -12.6
        b.cube("chest", (ox, 39.2, -4.6), (5.6, 4.4, 9.2), {"north": ("cloth", dict(pal="gold", pleats=2, edges="tblr")), "south": ("cloth", dict(pal="gold", pleats=2)), "up": ("cloth", dict(pal="gold", pleats=2)), "west": ("cloth", dict(pal="gold")), "east": ("cloth", dict(pal="gold")), "down": None}, tier=1)     # shoulder plates of light
    b.cube("chest", (-2.6, 43.2, -2.6), (5.2, 3.6, 5.2), {"*": ("core", {}), "down": None, "up": None}, tier=1)                      # the neck
    # ------------------------------------------------------------------ the head: a pale mask of light that wears a face (the base one is serene), the other moods are slabs in front of it
    b.bone("head", (0, HEAD_Y, 0), "chest")
    b.cube("head", (-5, HEAD_Y, -5), (10, 10, 10), {"north": ("seraph_face", dict(mood="serene")), "south": ("core", {}), "west": ("core", {}), "east": ("core", {}), "up": ("core", {}), "down": None})
    b.bone("face_base", (0, HEAD_Y + 5, -5.3), "head")
    for mood in ("stern", "compassionate", "radiant"):
        b.bone("face_" + mood, (0, HEAD_Y + 5, -5.3), "head")
        b.cube("face_" + mood, (-5, HEAD_Y, -5.14), (10, 10, 0.1), {"north": ("seraph_face", dict(mood=mood))}, tier=1)
    # ------------------------------------------------------------------ the arms: a sleeve and a hand of light, hanging at rest; the poses raise, open, fold and point them
    for sx, tag in ((1, "L"), (-1, "R")):
        sh = (sx * 9.4, 41.0, -0.5)
        b.bone("arm_" + tag, sh, "chest")
        ox = 7.4 if sx > 0 else -11.4
        b.cube("arm_" + tag, (ox, 30.5, -2.5), (4, 11, 4), {"north": cl(pleats=2), "south": cl(pleats=2), "west": cl(pleats=2), "east": cl(pleats=2), "up": None, "down": None}, tier=1)
        if lod <= 1:
            b.bone("fore_" + tag, (sx * 9.4, 31.0, -0.5), "arm_" + tag)
            ox2 = 7.6 if sx > 0 else -11.2
            b.cube("fore_" + tag, (ox2, 21.5, -2.2), (3.6, 9.5, 3.6), {"north": cl(pleats=2, trim=0.2), "south": cl(pleats=2), "west": cl(pleats=2), "east": cl(pleats=2), "up": None, "down": None}, tier=1)
            b.cube("fore_" + tag, (ox2 + 0.1, 17.6, -2.0), (3.4, 4.0, 3.2), {"north": ("hand", {}), "south": ("hand", {}), "west": ("hand", {}), "east": ("hand", {}), "up": None, "down": ("hand", {})}, tier=1)

    # ------------------------------------------------------------------ the wings: six, covered in eyes. A pair spread (the flying pair), a pair hanging crossed over the lower body (covering the feet), a pair raised
    # in front of the face (covering the face): each a fan of blades, a layer of coverts in front. Every blade is a thin slab with a painted feather (a cut-out) and eyes on both its faces.
    def wing(name, sx, root, blades, parent="chest"):
        bn = "%s_%s" % (name, "L" if sx > 0 else "R")
        b.bone(bn, (sx * root[0], root[1], root[2]), parent)
        for i, (ang, length, width, z, eyes, tier, kind) in enumerate(blades):
            if lod > tier:
                continue
            key = "%s_%d" % (name, i)
            opts = dict(eyes=eyes, key=key, widen=0.36 if kind != "lesser" else 0.5, tip=0.5)
            if kind == "covert":
                opts["tint"] = 1
            face = ("feather", opts)
            b.cube(bn, (sx * root[0] - width / 2, root[1], z), (width, length, 0.9),
                   {"north": ("feather", dict(opts, flip=(sx < 0))), "south": ("feather", dict(opts, flip=(sx > 0))), "up": None, "down": None, "west": None, "east": None},
                   rot=(0, 0, WING_SIGN * sx * ang), pivot=(sx * root[0], root[1], z), tier=2)
            if kind == "primary":
                b.glow(bn, (sx * root[0] - width / 2, root[1], z - 0.2), (width, length, 0.1),
                       {"north": ("fx_feather_eye", dict(eyes=eyes, key=key + "g", flip=(sx < 0))), "south": ("fx_feather_eye", dict(eyes=eyes, key=key + "g", flip=(sx > 0)))},
                       rot=(0, 0, WING_SIGN * sx * ang), pivot=(sx * root[0], root[1], z), tier=1)

    two = [(0.27, 1.0), (0.62, 0.85)]
    one = [(0.30, 1.0)]
    for sx in (1, -1):
        # A: the flying pair (attached high on the back)
        prim = [(14, 40, 9, 5.0, two, 1, "primary"), (28, 44, 9, 5.45, two, 2, "primary"), (42, 47, 9, 5.9, two, 1, "primary"), (56, 47, 9, 6.35, two, 2, "primary"),
                (70, 44, 9, 6.8, two, 1, "primary"), (84, 41, 9, 7.25, two, 2, "primary"), (98, 37, 9, 7.7, one, 1, "primary"), (112, 32, 9, 8.15, one, 2, "primary"), (126, 27, 8.5, 8.6, one, 1, "primary")]
        cov = [(22, 27, 7.5, 3.6, one, 1, "covert"), (37, 29, 7.5, 3.15, one, 2, "covert"), (52, 30, 7.5, 2.7, one, 1, "covert"), (67, 29, 7.5, 2.25, one, 2, "covert"),
               (82, 26, 7.5, 1.8, one, 1, "covert"), (97, 22, 7.2, 1.35, one, 2, "covert"), (112, 18, 7, 0.9, one, 1, "covert")]
        wing("wingA", sx, (6.0, 40.0, 4.0), prim + cov)
        # B: the lower pair, hanging down and crossing in front of the robe
        lowp = [(168, 28, 8, -9.0, two, 1, "primary"), (180, 31, 8, -9.4, two, 2, "primary"), (192, 33, 8, -9.8, two, 1, "primary"), (204, 31, 8, -10.2, two, 2, "primary"),
                (216, 27, 8, -10.6, one, 1, "primary")]
        lowc = [(174, 19, 6.8, -8.4, one, 2, "covert"), (186, 21, 6.8, -8.0, one, 1, "covert"), (198, 21, 6.8, -7.6, one, 2, "covert"), (210, 18, 6.8, -7.2, one, 1, "covert")]
        wing("wingB", sx, (6.5, 34.0, -4.0), lowp + lowc)
        # C: the pair raised before the face, parting on a slit
        facep = [(-4, 19, 6.8, -8.2, one, 2, "primary"), (-12, 21, 6.8, -8.6, one, 1, "primary"), (-20, 21, 6.8, -9.0, one, 2, "primary"), (-28, 18, 6.8, -9.4, one, 1, "primary")]
        facec = [(-8, 13, 6, -7.4, one, 1, "covert"), (-17, 14, 6, -7.0, one, 2, "covert"), (-26, 12, 6, -6.6, one, 1, "covert")]
        wing("wingC", sx, (7.2, 41.5, -4.0), facep + facec)

    # ------------------------------------------------------------------ the rings (ophanim): three gyroscope rings turning about the body, eyes along them
    for k, (nm, piv, tilt, r, eyes) in enumerate((("ring1", (0, 51.0, 9.0), (90, 0, 0), 23.0, 12), ("ring2", (0, 30.0, 0.0), (22, 0, 16), 31.0, 14), ("ring3", (0, 38.0, 0.0), (-26, 0, -64), 22.0, 10))):
        if lod == 2 and k > 0:
            continue
        b.bone(nm + "_tilt", piv, "body", rotation=tilt)
        b.bone(nm, piv, nm + "_tilt")
        b.cube(nm, (piv[0] - r, piv[1] - 0.4, piv[2] - r), (2 * r, 0.8, 2 * r), {"up": ("ring", dict(eyes=eyes, ticks=eyes, inner=0.86, dark=(k == 1), key=nm)), "down": ("ring", dict(eyes=eyes, ticks=eyes, inner=0.86, dark=(k == 1), key=nm))})
        rg = r * 1.28
        b.glow(nm, (piv[0] - rg, piv[1] - 0.2, piv[2] - rg), (2 * rg, 0.1, 2 * rg), {"up": ("fx_ring", dict(r=0.78, width=0.06, beads=eyes * 2, key=nm + "g")), "down": ("fx_ring", dict(r=0.78, width=0.06, beads=eyes * 2, key=nm + "g"))}, tier=1)
    # ------------------------------------------------------------------ the prism: a floating gem of light above the head, standing on a corner, turning
    b.bone("prism_hover", (0, 71.0, 0), "head")
    b.bone("prism", (0, 71.0, 0), "prism_hover")
    b.cube("prism", (-4.5, 66.5, -4.5), (9, 9, 9), {"*": ("prism", {})}, rot=(35.264, 45.0, 0), pivot=(0, 71.0, 0), tier=1)
    b.glow("prism", (-9, 62, 0), (18, 18, 0.1), {"north": ("fx_core", {"key": "prismg"}), "south": ("fx_core", {"key": "prismg"})}, tier=1)
    b.glow("prism", (0, 62, -9), (0.1, 18, 18), {"west": ("fx_core", {"key": "prismg"}), "east": ("fx_core", {"key": "prismg"})}, tier=1)
    # ------------------------------------------------------------------ the glow-only model: the faces, the core, the hem
    b.glow("face_base", (-5, HEAD_Y, -5.3), (10, 10, 0.1), {"north": ("fx_face", dict(mood="serene"))})
    for mood in ("stern", "compassionate", "radiant"):
        b.glow("face_" + mood, (-5, HEAD_Y, -5.45), (10, 10, 0.1), {"north": ("fx_face", dict(mood=mood))})
    b.glow("head", (-11, HEAD_Y - 3, -0.1), (22, 16, 0.1), {"north": ("fx_core", {"key": "headg"}), "south": ("fx_core", {"key": "headg"})}, tier=0)
    b.glow("chest", (-9, 34, -5.7), (18, 14, 0.1), {"north": ("fx_glow", dict(sigma=0.45, peak=0.5, key="chestg"))}, tier=0)
    b.glow("robe_low", (-11, -0.5, -7.4), (22, 3.5, 0.1), {"north": ("fx_glow", dict(sigma=0.55, peak=0.6, key="hemg"))}, tier=0)
    return finish(b, ident)


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    forms = {"cub": cub, "seraph": seraph}
    for name, fn in forms.items():
        if only and name not in only:
            continue
        for lod, fx in ((0, False), (1, False), (2, False), (0, True)):
            if count_only:
                L_ROOT = L.ROOT
            m = fn(lod, fx)
            print("%-28s cubes %4d  quads %5d  sheet %dx%d" % (m.id, m.cubes(), m.quads(), m.sheet.w, m.sheet.height))
        if not count_only:
            L.write_animations("spiritlight_" + name, {"cub": AN.cub_clips, "seraph": AN.seraph_clips}[name]())


if __name__ == "__main__":
    main()
