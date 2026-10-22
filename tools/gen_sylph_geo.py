"""The Sylph, Yuno's wind Spirit Lord, in both of her forms (docs/magic/sylph.md): the SMALL fairy girl that floats beside the caster, and the POWERED form with the long streaming hair and the big
wings that hangs over the caster while a big wind page is cast. Real 3D models (Bedrock geo, 1/16 block units, y up, front -z), hand-painted sheets (tools/sylph_art.py), animations, three levels
of detail per form and a glow-only model:

    python3 -B tools/gen_sylph_geo.py                 writes assets/nusmp/{geo,animations,textures}/entity/sylph_{small,powered}[_lod1|_lod2|_wings|_fx]*
    python3 -B tools/gen_sylph_geo.py --count         prints the quads of every model
    python3 -B tools/gen_sylph_geo.py small           one form

    sylph_<form>          full level of detail: the body, the dress, the hair (the cap, the cards and the swaying locks), the head with its three faces (one is shown at a time)
    sylph_<form>_lod1     about half the quads         sylph_<form>_lod2    low poly (a body of boxes, a hair cap, a handful of cards)
    sylph_<form>_wings    the four dragonfly wings (and, for the powered form, the petal wings of the skirt), drawn translucent with a glow map of the veins and the rims
    sylph_<form>_fx       the glow-only model: the shown face's eyes and the hem of the skirt, drawn additive

The detail is in the texture (the face, the strands, the ruffles, the cell veins), not in the geometry. All models of a form share the skeleton of the full model (one animation file drives them all;
a bone a LOD does not have is ignored). The head carries three faces as three thin plates (a clip shows one and scales the others to nothing).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sylph_art as A  # noqa: E402
import sylph_geo_lib as G  # noqa: E402

HID = 0.001                                                   # a hidden face is scaled to this (not 0: a zero scale makes bad normals)
FORMS = ("small", "powered")
sn, mg = G.sine, G.merge


class B:
    """A model under construction: bones and cubes, with a running y offset so a part can be built at the origin and placed."""

    def __init__(self, ident, atlas, tex_scale=1.0):
        self.m = G.Model(ident, atlas, tex_scale)
        self.at = atlas
        self.dy = 0.0

    def bone(self, name, pivot, parent="root", rot=None):
        return self.m.bone(name, (pivot[0], pivot[1] + self.dy, pivot[2]), parent, rot)

    def top(self, bone, x0, y0, z0, w, h, d, faces, rot=None, pivot=None):
        if pivot is not None:
            pivot = (pivot[0], pivot[1] + self.dy, pivot[2])
        return self.m.cube(bone, (x0, y0 + self.dy, z0), (w, h, d), faces, rot, pivot)

    def box(self, bone, cx, cy, cz, w, h, d, faces, rot=None, pivot=None):
        return self.top(bone, cx - w / 2, cy - h / 2, cz - d / 2, w, h, d, faces, rot, pivot)


def win(at, name, x0=0.0, y0=0.0, x1=1.0, y1=1.0):
    return at.rect(name, x0, y0, x1, y1)


FACE_H, FACE_UP = 8.5, 5.8                                    # the face picture is 9 x 8.5 units: the cranium shows its upper 5.8, the chin its lower part


def face_faces(at, kind):
    """The uv windows of one face picture on the cranium and on the chin."""
    W = lambda *a: win(at, "face_" + kind, *a)
    return W(0, 0, 1, FACE_UP / FACE_H), W(1.0 / 9.0, FACE_UP / FACE_H, 8.0 / 9.0, 1.0)


# ====================================================================================================================== atlases
def body_atlas(form):
    P = 16
    at = G.Atlas(768 if form == "small" else 1024)
    tone = form
    kinds = ("laugh", "smile", "stern") if form == "small" else ("stern", "shout", "smile")
    for k in kinds:
        at.add("face_" + k, A.face(k, 9.0, FACE_H, P))
    at.add("skin", A.skin_flat(2, 2, 8))
    at.add("limb", A.skin_plain(2.2, 6.0, 12, edges=True))
    at.add("foot", A.skin_plain(2.4, 4.0, 12, edges=True, toes=True))
    at.add("hair_top", A.hair_block(10, 9, 10, 11, tone))
    at.add("hair_side", A.hair_block(9, 8, 10, 12, tone))
    at.add("hair_back", A.hair_block(10, 9, 10, 13, tone))
    at.add("fringe_n", A.fringe(9.8, 2.6, 16, 61, tone, teeth=8, depth=0.42))
    at.add("fringe_s", A.fringe(9.8, 2.6, 16, 62, tone, teeth=7, depth=0.25))
    at.add("fringe_w", A.fringe(8.8, 2.6, 16, 63, tone, teeth=4, depth=0.3))
    at.add("fringe_e", A.fringe(8.8, 2.6, 16, 64, tone, teeth=4, depth=0.3))
    for i in range(5):
        at.add("strand%d" % i, A.strand(2.6 + 0.2 * (i % 3), 12.0 + (i % 2) * 2, 14, 20 + i, tone, curl=0.25 * (i - 2)))
    at.add("ahoge", A.strand(1.6, 7.5, 20, 31, tone, curl=0.0, ragged=0.0))
    at.add("ear", A.ear_card(3.6, 2.0, 16))
    at.add("bodice", A.bodice(6.0, 4.4, 16, tone))
    at.add("bodice_back", A.bodice(6.0, 4.4, 16, tone, back=True))
    at.add("dress", A.dress_flat(5, 4, 12, tone))
    for k in range(4):
        at.add("ruffle%d" % k, A.ruffle(2.8 + 0.7 * k, 4.2 + 0.4 * k, 20, 50 + k, tone, waves=1 + (k > 0)))
    at.add("choker", A.choker(2.6, 0.9, 16))
    if form == "powered":
        for i in range(3):
            at.add("ribbon%d" % i, A.ribbon(9.0, 34.0, 12, 80 + i, "powered", phase=1.4 * i, sway=1.0 + 0.2 * i))
    return at.pack()


def wings_atlas(form, glow=False):
    at = G.Atlas(1024 if form == "powered" else 512)
    if form == "small":
        at.add("fore", A.wing(24, 7.5, 16, 5, "fore", "small", glow))
        at.add("hind", A.wing(18, 6.0, 16, 6, "hind", "small", glow))
    else:
        at.add("fore", A.wing(60, 17.0, 12, 5, "fore", "powered", glow))
        at.add("hind", A.wing(44, 13.0, 12, 6, "hind", "powered", glow))
        for i in range(3):
            at.add("petal%d" % i, A.petal(18 + 3 * i, 11 + 2 * i, 12, 90 + i, "powered", glow))
    return at.pack()


def fx_atlas(form):
    at = G.Atlas(512)
    kinds = ("laugh", "smile", "stern") if form == "small" else ("stern", "shout", "smile")
    for k in kinds:
        at.add("face_" + k, A.face_glow(k, 9.0, FACE_H, 16))
    for k in range(4):
        at.add("hem%d" % k, A.hem_glow(2.8 + 0.7 * k, 4.2 + 0.4 * k, 20, 70 + k, form))
    return at.pack()


# ====================================================================================================================== the parts (shared by both forms)
def head(b, at, lod, hy, kinds, powered=False):
    """The head with its chin at height hy: cranium, chin, three face plates (bones face_<kind>), pointed ears and the hair cap, the bob, the locks."""
    W = lambda n, *a: win(at, n, *a)
    skin = W("skin")
    b.dy = hy
    b.top("head", -4.5, 2.5, -4.0, 9.0, FACE_UP, 8.0, {"*": skin, "up": W("hair_top")})
    b.top("head", -3.5, 0.0, -3.5, 7.0, 2.6, 7.0, {"*": skin})
    for kind in kinds:
        nm = "face_" + kind
        b.bone(nm, (0, 4.0, -4.1), "head")
        up, low = face_faces(at, kind)
        b.top(nm, -4.5, 2.5, -4.08, 9.0, FACE_UP, 0.02, {"north": up})
        b.top(nm, -3.5, 0.0, -3.58, 7.0, 2.6, 0.02, {"north": low})
    ear = W("ear")
    b.top("head", -8.1, 4.2, -0.2, 3.9, 2.1, 0.3, {"north": ear, "south": (ear, True)}, rot=(0, 0, -14), pivot=(-4.2, 5.0, 0))
    b.top("head", 4.2, 4.2, -0.2, 3.9, 2.1, 0.3, {"north": (ear, True), "south": ear}, rot=(0, 0, 14), pivot=(4.2, 5.0, 0))
    # the hair cap with its ragged fringe, the back of the bob and the sides
    b.top("head", -4.9, 6.4, -4.4, 9.8, 2.6, 8.8, {"north": W("fringe_n"), "south": W("fringe_s"), "west": W("fringe_w"), "east": W("fringe_e"), "up": W("hair_top"), "down": None})
    b.top("head", -4.8, 0.8, 3.1, 9.6, 7.4, 1.5, {"*": W("hair_back"), "down": None, "up": None})
    if lod <= 1:
        for x in (-5.0, 4.0):
            b.top("head", x, 1.4, -3.6, 1.0, 6.6, 6.8, {"*": W("hair_side"), "up": None, "down": None, "north": None, "south": None})
    if lod == 0:
        for i, (x, rz, ry, nm) in enumerate(((-5.4, 5, -30, "side_R"), (5.4, -5, 30, "side_L"))):                                      # cheek locks down to the chin
            b.bone("lock_" + nm, (x, 7.3, -2.6), "head")
            for j in range(3 if not powered else 2):
                b.top("lock_" + nm, x - 1.3 + j * 0.7, -1.6, -2.7 + j * 0.45, 2.6, 9.6 - j, 0.14, {"north": W("strand%d" % ((i * 2 + j) % 5)), "south": W("strand%d" % ((i * 2 + j) % 5))},
                      rot=(0, ry * (1 if j == 0 else 0.4), rz * (1 + j)), pivot=(x, 7.3, -2.6))
        if not powered:
            for i, (x, ry, rz) in enumerate(((-3.8, 24, 12), (-1.9, 12, 5), (0.2, 0, 0), (2.2, -12, -5), (3.9, -24, -12))):          # nape locks flaring out at the ends
                b.bone("lock_b%d" % i, (x, 7.6, 3.6), "head")
                b.top("lock_b%d" % i, x - 1.3, -5.2, 3.6 + 0.05 * i, 2.6, 12.6, 0.14, {"north": W("strand%d" % ((i + 1) % 5)), "south": W("strand%d" % ((i + 1) % 5))}, rot=(6, ry, rz), pivot=(x, 7.6, 3.6))
    if not powered:
        prev, y = "head", 8.9                                                                                                      # the ahoge: a curl of cards, each bends more than the one below
        for k, bend in enumerate((6, 26, 38, 44, 44)):
            nm = "ahoge%d" % k
            b.bone(nm, (0.0, y, -0.4), prev, rot=(0, 0, -bend))
            b.top(nm, -0.8, y, -0.45, 1.6, 1.5, 0.12, {"north": W("ahoge", 0, 0.2 * k, 1, 0.2 * (k + 1)), "south": W("ahoge", 0, 0.2 * k, 1, 0.2 * (k + 1))})
            prev, y = nm, y + 1.5
    else:                                                                                                                          # the streaming hair: three ribbons of four cards, bending as they rise
        for i, (x, side) in enumerate(((-3.0, -1), (0.0, 0), (3.0, 1))):
            prev, y = "head", 8.6
            img = "ribbon%d" % i
            for k in range(4):
                nm = "hair%d_%d" % (i, k)
                b.bone(nm, (x, y, 0.8 - 0.6 * i), prev, rot=(0, 0, side * (14 if k == 0 else 4) * (1 if k % 2 == 0 else -1)))
                b.top(nm, x - 4.5, y, 0.8 - 0.6 * i, 9.0, 8.5, 0.12, {"north": W(img, 0, 1.0 - 0.25 * (k + 1), 1, 1.0 - 0.25 * k), "south": W(img, 0, 1.0 - 0.25 * (k + 1), 1, 1.0 - 0.25 * k)})
                prev, y = nm, y + 8.5
    b.dy = 0.0


def torso(b, at, lod, o):
    """o: the proportions of the form (hips y, waist y, chest y, shoulder y, neck y, widths)."""
    W = lambda n, *a: win(at, n, *a)
    skin = W("skin")
    b.bone("body", (0, o["hip"], 0))
    b.bone("chest", (0, o["waist"], 0), "body")
    b.bone("neck", (0, o["neck"], 0), "chest")
    b.bone("head", (0, o["head_y"], 0), "neck")
    cw, ch = o["chest_w"], o["chest_h"]
    b.box("chest", 0, o["chest_y"], 0, cw, ch, 3.6, {"*": skin, "north": W("bodice"), "south": W("bodice_back")})
    dress = {"*": W("dress"), "up": None, "down": None}
    b.box("body", 0, o["waist_y"], 0, cw - 1.1, o["waist_h"], 3.2, dress)
    b.box("body", 0, o["hip_y"], 0, cw - 0.4, o["hip_h"], 3.4, dress)
    b.box("neck", 0, o["neck"] + 0.5, 0, 1.9, 1.3, 1.9, {"*": skin})
    b.box("neck", 0, o["neck"] + 0.55, 0, 2.5, 0.8, 2.5, {"*": W("choker")})


def arms(b, at, lod, o):
    W = lambda n, *a: win(at, n, *a)
    limb = {"*": W("limb"), "up": W("skin"), "down": W("skin")}
    u, f, sx, sy = o["upper"], o["fore"], o["shoulder_x"], o["shoulder_y"]
    for s, tag in ((-1, "R"), (1, "L")):
        x = s * sx
        b.bone("arm_" + tag, (x, sy, 0), "chest")
        b.box("arm_" + tag, x, sy - u / 2, 0, o["arm_w"], u, o["arm_w"], limb)
        b.bone("fore_" + tag, (x, sy - u, 0), "arm_" + tag)
        b.box("fore_" + tag, x, sy - u - f / 2, 0, o["arm_w"] - 0.2, f, o["arm_w"] - 0.2, limb)
        b.bone("hand_" + tag, (x, sy - u - f, 0), "fore_" + tag)
        hy = sy - u - f
        b.box("hand_" + tag, x, hy - 0.9, 0, 1.7, 1.8, 1.0, limb)
        if lod == 0:
            for j in range(3):                                                           # three finger bars and a thumb
                b.box("hand_" + tag, x + (j - 1) * 0.5, hy - 2.4, 0, 0.45, 1.6, 0.6, limb)
            b.box("hand_" + tag, x - s * 1.1, hy - 1.0, -0.2, 0.5, 1.3, 0.6, limb, rot=(0, 0, s * 25), pivot=(x - s * 0.8, hy - 0.4, 0))


def legs(b, at, lod, o):
    W = lambda n, *a: win(at, n, *a)
    limb = {"*": W("limb"), "up": W("skin"), "down": W("skin")}
    foot = {"*": W("limb"), "up": W("foot"), "down": W("skin")}
    hip, th, sh = o["hip"], o["thigh"], o["shin"]
    for s, tag in ((-1, "R"), (1, "L")):
        x = s * o["leg_x"]
        b.bone("leg_" + tag, (x, hip, 0), "body")
        b.box("leg_" + tag, x, hip - th / 2, 0, o["leg_w"], th, o["leg_w"], limb)
        b.bone("shin_" + tag, (x, hip - th, 0), "leg_" + tag)
        b.box("shin_" + tag, x, hip - th - sh / 2, 0, o["leg_w"] - 0.4, sh, o["leg_w"] - 0.4, limb)
        b.bone("foot_" + tag, (x, hip - th - sh, 0), "shin_" + tag)
        b.box("foot_" + tag, x, (hip - th - sh) / 2, -1.1, o["leg_w"] - 0.3, hip - th - sh, 4.2, foot)
        if lod == 0:
            b.box("foot_" + tag, x, 0.5, -3.4, o["leg_w"] - 0.4, 1.0, 1.0, {"*": W("limb"), "up": W("foot")})        # the toes


def skirt(b, at, lod, o):
    W = lambda n, *a: win(at, n, *a)
    n = {0: 10, 1: 7, 2: 4}[lod]
    tiers = o["tiers"]
    if lod == 2:
        tiers = [tiers[0], tiers[-1]]
    top0 = o["skirt_top"]
    for i in range(n):
        yaw = 360.0 * i / n
        b.bone("sk%d" % i, (0, top0, 0), "body", rot=(0, yaw, 0))
        b.bone("sk%d_f" % i, (0, top0, -2.0), "sk%d" % i)
        for (top, ln, flare, r, t) in tiers:
            wid = 2.0 * math.pi * r / n * 1.38
            nm = "ruffle%d" % t
            b.top("sk%d_f" % i, -wid / 2, top - ln, -r, wid, ln, 0.15, {"north": W(nm), "south": W(nm), "up": None, "down": None, "west": None, "east": None}, rot=(flare, 0, 0), pivot=(0, top, -r))


# ====================================================================================================================== the two forms
SMALL = dict(hip=15.0, waist=20.5, neck=25.0, head_y=26.2, chest_y=22.6, chest_h=4.2, chest_w=6.0, waist_y=19.2, waist_h=3.0, hip_y=16.4, hip_h=2.6,
             shoulder_x=3.8, shoulder_y=24.0, upper=4.4, fore=4.0, arm_w=1.7, leg_x=1.7, thigh=6.4, shin=6.4, leg_w=2.5, skirt_top=18.6,
             tiers=[(18.6, 4.2, 14, 2.9, 0), (17.0, 4.6, 30, 3.5, 1), (15.4, 5.0, 46, 4.1, 2), (14.0, 5.0, 58, 4.7, 3)], head_dy=26.2)
POWERED = dict(hip=25.0, waist=31.0, neck=37.0, head_y=38.5, chest_y=34.0, chest_h=6.0, chest_w=6.6, waist_y=29.2, waist_h=3.2, hip_y=26.4, hip_h=3.0,
               shoulder_x=4.4, shoulder_y=36.4, upper=8.4, fore=8.0, arm_w=2.0, leg_x=2.1, thigh=11.0, shin=11.0, leg_w=2.8, skirt_top=28.6,
               tiers=[(28.6, 3.8, 16, 3.5, 0), (27.2, 4.2, 32, 4.2, 1), (25.8, 4.6, 50, 5.0, 2), (24.6, 4.6, 62, 5.8, 3)], head_dy=38.5)


def build_body(form, lod, at):
    o = SMALL if form == "small" else POWERED
    sfx = "" if lod == 0 else "_lod%d" % lod
    b = B("sylph_%s%s" % (form, sfx), at, 1.0 if lod == 0 else (0.5 if lod == 1 else 0.25))
    torso(b, at, lod, o)
    head(b, at, lod, o["head_dy"], ("laugh", "smile", "stern") if form == "small" else ("stern", "shout", "smile"), powered=form == "powered")
    arms(b, at, lod, o)
    legs(b, at, lod, o)
    skirt(b, at, lod, o)
    return b


def skeleton_only(b, o):
    b.bone("body", (0, o["hip"], 0))
    b.bone("chest", (0, o["waist"], 0), "body")
    b.bone("neck", (0, o["neck"], 0), "chest")
    b.bone("head", (0, o["head_y"], 0), "neck")


def build_wings(form, at):
    o = SMALL if form == "small" else POWERED
    b = B("sylph_%s_wings" % form, at)
    skeleton_only(b, o)
    W = lambda n, *a: win(at, n, *a)
    if form == "small":
        # (name, root x, root y, root z, length, chord, roll, yaw): a wing is a plate standing in the x-y plane (seen broad from the front and the back), rolled up and out, swept back
        specs = [("f", 1.6, 22.6, 2.6, 24.0, 7.5, 62.0, 10.0), ("h", 1.4, 21.4, 2.9, 18.0, 6.0, 24.0, 14.0)]
    else:
        specs = [("f", 2.0, 35.0, 2.6, 60.0, 17.0, 52.0, 8.0), ("h", 1.8, 32.6, 3.0, 44.0, 13.0, 18.0, 12.0)]
    for tag, s in (("R", -1), ("L", 1)):
        for nm, rx_, ry_, rz_, ln, ch, roll, yaw in specs:
            bn = "wing_%s_%s" % (nm, tag)
            b.bone(bn, (s * rx_, ry_, rz_), "chest", rot=(0, s * -yaw, s * roll))
            img = W("fore" if nm == "f" else "hind")
            x0 = s * rx_ if s > 0 else s * rx_ - ln
            # the picture has its root at the left and the leading edge up; from the front the model's right (-x) is on the viewer's right, so the +x wing (tip on the viewer's left) is mirrored
            face = {"north": (img, s > 0), "south": (img, s < 0), "up": None, "down": None, "west": None, "east": None}
            b.top(bn, x0, ry_ - ch * 0.40, rz_ - 0.05, ln, ch, 0.1, face)
    if form == "powered":                                                                          # the petal wings of the skirt: three a side, a fan from the hip
        for tag, s in (("R", -1), ("L", 1)):
            for k, (roll, yaw, ln, ch, z) in enumerate(((66.0, 6.0, 18.0, 11.0, 2.4), (38.0, 10.0, 21.0, 13.0, 2.8), (12.0, 14.0, 24.0, 15.0, 3.2))):
                bn = "petal_%d_%s" % (k, tag)
                b.bone(bn, (s * 2.6, 27.4, z), "body", rot=(0, s * -yaw, s * roll))
                img = W("petal%d" % k)
                x0 = s * 2.6 if s > 0 else s * 2.6 - ln
                face = {"north": (img, s > 0), "south": (img, s < 0), "up": None, "down": None, "west": None, "east": None}
                b.top(bn, x0, 27.4 - ch * 0.5, z - 0.05, ln, ch, 0.1, face)
    return b


def build_fx(form, at):
    o = SMALL if form == "small" else POWERED
    b = B("sylph_%s_fx" % form, at)
    skeleton_only(b, o)
    kinds = ("laugh", "smile", "stern") if form == "small" else ("stern", "shout", "smile")
    b.dy = o["head_dy"]
    for kind in kinds:
        nm = "face_" + kind
        b.bone(nm, (0, 4.0, -4.1), "head")
        up, _ = face_faces(at, kind)
        b.top(nm, -4.5, 2.5, -4.12, 9.0, FACE_UP, 0.02, {"north": up})
    b.dy = 0.0
    W = lambda n, *a: win(at, n, *a)
    n = 10
    top0 = o["skirt_top"]
    (top, ln, flare, r, t) = o["tiers"][-1]                                                        # the outer tier's hem glows
    for i in range(n):
        yaw = 360.0 * i / n
        b.bone("sk%d" % i, (0, top0, 0), "body", rot=(0, yaw, 0))
        b.bone("sk%d_f" % i, (0, top0, -2.0), "sk%d" % i)
        wid = 2.0 * math.pi * r / n * 1.38
        b.top("sk%d_f" % i, -wid / 2, top - ln, -r - 0.05, wid, ln, 0.15, {"north": W("hem%d" % t), "south": W("hem%d" % t), "up": None, "down": None, "west": None, "east": None}, rot=(flare, 0, 0), pivot=(0, top, -r - 0.05))
    return b


# ====================================================================================================================== animations
def face_tracks(kinds, shown):
    out = {}
    for k in kinds:
        v = 1.0 if k == shown else HID
        out["face_" + k] = {"scale": {0.0: (v, v, v)}}
    return out


def hold(v, T):
    return {0.0: v, T: v}


def swayers(names, T, amp, lag, axis=2, step=0.07):
    out = {}
    for i, nm in enumerate(names):
        out[nm] = {"rotation": sn(T, amp * (1 if i % 2 == 0 else -1), axis, i * lag + step * (i % 3), steps=8)}
    return out


def small_animations():
    kinds = ("laugh", "smile", "stern")
    skirt = ["sk%d_f" % i for i in range(10)]
    locks = ["lock_side_R", "lock_side_L"] + ["lock_b%d" % i for i in range(5)]
    ahoge = ["ahoge%d" % k for k in range(5)]
    clips = {}

    def wings(T, a, lag=0.1):
        return {"wing_f_L": {"rotation": sn(T, a, 2, 0.0, steps=8)}, "wing_f_R": {"rotation": sn(T, -a, 2, 0.0, steps=8)},
                "wing_h_L": {"rotation": sn(T, a * 0.9, 2, lag, steps=8)}, "wing_h_R": {"rotation": sn(T, -a * 0.9, 2, lag, steps=8)}}

    def skirt_sway(T, amp):
        out = {}
        for i, nm in enumerate(skirt):
            out[nm] = {"rotation": sn(T, amp, 0, i * 0.055, steps=8)}
        return out

    def hair(T, amp):
        out = swayers(locks, T, amp, 0.05)
        for k, nm in enumerate(ahoge):
            out[nm] = {"rotation": sn(T, amp * (0.8 + 0.3 * k), 2, 0.1 * k, steps=8)}
        return out

    # ---- idle: floating, seated and leaning back (the first picture): one knee up, one leg dangling, a hand at the cheek, the shoulders shaking with a laugh
    T = 2.4
    L = lambda v: hold(v, T)
    bones = {
        "body": {"position": sn(T, 0.5, 1, 0.0, steps=8), "rotation": mg(L((-18, 0, 4)), sn(T, 1.5, 0, 0.0, steps=8))},
        "chest": {"rotation": mg(L((14, 0, 0)), sn(T * 0.25, 2.4, 2, 0.0, steps=8))},
        "head": {"rotation": mg(L((-8, 4, -7)), sn(T, 1.5, 1, 0.2, steps=8), sn(T * 0.25, 1.8, 0, 0.0, steps=8))},
        "leg_R": {"rotation": mg(L((84, 0, 2)), sn(T, 2.5, 0, 0.0, steps=8))},
        "shin_R": {"rotation": mg(L((-100, 0, 0)), sn(T, 2.5, 0, 0.3, steps=8))},
        "foot_R": {"rotation": L((30, 0, 0))},
        "leg_L": {"rotation": mg(L((38, 0, -3)), sn(T, 4.0, 0, 0.5, steps=8))},
        "shin_L": {"rotation": mg(L((-14, 0, 0)), sn(T, 5.0, 0, 0.7, steps=8))},
        "foot_L": {"rotation": mg(L((26, 0, 0)), sn(T, 5.0, 0, 0.8, steps=8))},
        "arm_R": {"rotation": mg(L((70, 0, -16)), sn(T * 0.25, 4.0, 0, 0.0, steps=8))},
        "fore_R": {"rotation": L((96, 0, 0))},
        "hand_R": {"rotation": L((0, 0, -16))},
        "arm_L": {"rotation": L((-26, 0, 18))},
        "fore_L": {"rotation": L((18, 0, 0))},
    }
    bones.update(wings(T * 0.25, 7.0))
    bones.update(skirt_sway(T, 3.5))
    bones.update(hair(T, 4.0))
    bones.update(face_tracks(kinds, "laugh"))
    clips["idle"] = {"loop": True, "length": T, "bones": bones}

    # ---- laugh: the same pose, a bigger fit: the head thrown back, the chest heaving, the hand pressed to the mouth
    T = 1.2
    L = lambda v: hold(v, T)
    b2 = dict(bones)
    b2.update({
        "body": {"position": sn(T, 0.7, 1, 0.0, steps=8), "rotation": mg(L((-22, 0, 4)), sn(T * 0.5, 2.4, 0, 0.0, steps=8))},
        "chest": {"rotation": mg(L((14, 0, 0)), sn(T * 0.25, 4.5, 0, 0.0, steps=8))},
        "head": {"rotation": mg(L((-18, 6, -8)), sn(T * 0.25, 4.0, 0, 0.0, steps=8))},
        "leg_R": {"rotation": mg(L((84, 0, 2)), sn(T, 6.0, 0, 0.0, steps=8))},
        "shin_R": {"rotation": mg(L((-100, 0, 0)), sn(T, 6.0, 0, 0.3, steps=8))},
        "leg_L": {"rotation": mg(L((38, 0, -3)), sn(T, 10.0, 0, 0.5, steps=8))},
        "shin_L": {"rotation": mg(L((-14, 0, 0)), sn(T, 12.0, 0, 0.7, steps=8))},
        "foot_L": {"rotation": mg(L((26, 0, 0)), sn(T, 12.0, 0, 0.8, steps=8))},
        "arm_R": {"rotation": mg(L((78, 0, -8)), sn(T * 0.25, 7.0, 0, 0.0, steps=8))},
        "fore_R": {"rotation": mg(L((120, 0, 0)), sn(T * 0.25, 6.0, 0, 0.1, steps=8))},
        "arm_L": {"rotation": mg(L((-30, 0, 24)), sn(T * 0.5, 6.0, 0, 0.0, steps=8))},
    })
    b2.update(wings(T * 0.5, 10.0))
    b2.update(skirt_sway(T, 6.0))
    b2.update(hair(T, 6.0))
    b2.update(face_tracks(kinds, "laugh"))
    clips["laugh"] = {"loop": True, "length": T, "bones": b2}

    # ---- point: the second picture: standing, both index fingers at the head, a bright smile, a hop in the air
    T = 1.6
    L = lambda v: hold(v, T)
    b3 = {
        "body": {"position": sn(T, 1.2, 1, 0.25, steps=8), "rotation": mg(L((-6, 0, 0)), sn(T, 2.0, 2, 0.0, steps=8))},
        "chest": {"rotation": mg(L((4, 0, 0)), sn(T, 2.0, 1, 0.0, steps=8))},
        "head": {"rotation": mg(L((-4, 0, 6)), sn(T, 2.5, 2, 0.1, steps=8))},
        "arm_R": {"rotation": L((20, 0, -150))}, "fore_R": {"rotation": L((0, 0, 118))},
        "arm_L": {"rotation": mg(L((52, 0, 20)), sn(T, 3.0, 0, 0.0, steps=8))}, "fore_L": {"rotation": L((112, 0, 0))},
        "leg_R": {"rotation": mg(L((8, 0, -2)), sn(T, 5.0, 0, 0.0, steps=8))}, "shin_R": {"rotation": mg(L((-6, 0, 0)), sn(T, 6.0, 0, 0.1, steps=8))},
        "leg_L": {"rotation": mg(L((-30, 0, 4)), sn(T, 5.0, 0, 0.0, steps=8))}, "shin_L": {"rotation": mg(L((-100, 0, 0)), sn(T, 6.0, 0, 0.1, steps=8))},
        "foot_L": {"rotation": L((40, 0, 0))},
    }
    b3.update(wings(T * 0.25, 14.0))
    b3.update(skirt_sway(T, 7.0))
    b3.update(hair(T, 5.0))
    b3.update(face_tracks(kinds, "smile"))
    clips["point"] = {"loop": True, "length": T, "bones": b3}

    # ---- cast: she stands up in the air, one arm thrust forward with the palm open (the gust leaves her hand), the other arm back, the hair and skirt blowing back
    T = 1.0
    L = lambda v: hold(v, T)
    b4 = {
        "body": {"position": sn(T, 0.8, 1, 0.0, steps=8), "rotation": mg(L((-4, 0, 0)), sn(T, 1.0, 2, 0.0, steps=8))},
        "chest": {"rotation": L((-8, -14, 0))},
        "head": {"rotation": L((-6, 14, 0))},
        "arm_R": {"rotation": L((92, 0, -6))}, "fore_R": {"rotation": L((4, 0, 0))},
        "arm_L": {"rotation": L((-34, 0, 30))}, "fore_L": {"rotation": L((26, 0, 0))},
        "leg_R": {"rotation": L((18, 0, -4))}, "shin_R": {"rotation": L((-26, 0, 0))},
        "leg_L": {"rotation": L((-12, 0, 4))}, "shin_L": {"rotation": L((-34, 0, 0))},
        "foot_L": {"rotation": L((24, 0, 0))}, "foot_R": {"rotation": L((18, 0, 0))},
    }
    b4.update(wings(T * 0.2, 22.0))
    b4.update(skirt_sway(T, 8.0))
    b4.update(hair(T, 9.0))
    b4.update(face_tracks(kinds, "smile"))
    clips["cast"] = {"loop": True, "length": T, "bones": b4}

    # ---- fly: she follows the caster: leaning forward, the legs trailing, the wings beating fast
    T = 0.6
    L = lambda v: hold(v, T)
    b5 = {
        "body": {"position": sn(T, 0.6, 1, 0.0, steps=8), "rotation": L((-30, 0, 0))},
        "chest": {"rotation": L((-6, 0, 0))}, "head": {"rotation": mg(L((24, 0, 0)), sn(T, 2.0, 0, 0.1, steps=8))},
        "arm_R": {"rotation": L((-30, 0, -30))}, "arm_L": {"rotation": L((-30, 0, 30))}, "fore_R": {"rotation": L((20, 0, 0))}, "fore_L": {"rotation": L((20, 0, 0))},
        "leg_R": {"rotation": mg(L((-34, 0, -3)), sn(T, 5.0, 0, 0.0, steps=8))}, "leg_L": {"rotation": mg(L((-40, 0, 3)), sn(T, 5.0, 0, 0.5, steps=8))},
        "shin_R": {"rotation": L((-30, 0, 0))}, "shin_L": {"rotation": L((-34, 0, 0))},
        "foot_R": {"rotation": L((40, 0, 0))}, "foot_L": {"rotation": L((40, 0, 0))},
    }
    b5.update(wings(T, 26.0))
    b5.update(skirt_sway(T, 9.0))
    b5.update(hair(T, 10.0))
    b5.update(face_tracks(kinds, "smile"))
    clips["fly"] = {"loop": True, "length": T, "bones": b5}
    return clips


def powered_animations():
    kinds = ("stern", "shout", "smile")
    skirt = ["sk%d_f" % i for i in range(10)]
    hairs = ["hair%d_%d" % (i, k) for i in range(3) for k in range(4)]
    petals = ["petal_%d_%s" % (k, t) for k in range(3) for t in "RL"]
    locks = ["lock_side_R", "lock_side_L"]
    clips = {}

    def wings(T, a, lag=0.12):
        return {"wing_f_L": {"rotation": sn(T, a, 2, 0.0, steps=8)}, "wing_f_R": {"rotation": sn(T, -a, 2, 0.0, steps=8)},
                "wing_h_L": {"rotation": sn(T, a * 0.9, 2, lag, steps=8)}, "wing_h_R": {"rotation": sn(T, -a * 0.9, 2, lag, steps=8)}}

    def flow(T, amp, lag):
        out = {}
        for i in range(3):
            for k in range(4):
                out["hair%d_%d" % (i, k)] = {"rotation": sn(T, amp * (1.0 + 0.25 * k) * (1 if (i + k) % 2 == 0 else -1), 2, 0.08 * k + 0.13 * i + lag, steps=8)}
        for i, nm in enumerate(locks):
            out[nm] = {"rotation": sn(T, amp * 0.8 * (1 if i == 0 else -1), 2, 0.1 * i, steps=8)}
        return out

    def skirt_flow(T, amp):
        out = {}
        for i, nm in enumerate(skirt):
            out[nm] = {"rotation": sn(T, amp, 0, i * 0.05, steps=8)}
        for i, nm in enumerate(petals):
            out[nm] = {"rotation": sn(T, amp * 1.3 * (1 if i % 2 == 0 else -1), 2, 0.08 * i, steps=8)}
        return out

    # ---- hover: arms spread, legs apart, the hair streaming up, the big wings beating slowly, a stern face
    T = 2.8
    L = lambda v: hold(v, T)
    bones = {
        "body": {"position": sn(T, 1.2, 1, 0.0, steps=8), "rotation": L((0, 0, 0))},
        "chest": {"rotation": mg(L((0, 0, 0)), sn(T, 1.0, 0, 0.0, steps=8))},
        "head": {"rotation": mg(L((-4, 0, 0)), sn(T, 1.5, 1, 0.1, steps=8))},
        "arm_R": {"rotation": mg(L((4, 0, -44)), sn(T, 3.0, 2, 0.0, steps=8))}, "arm_L": {"rotation": mg(L((4, 0, 44)), sn(T, -3.0, 2, 0.0, steps=8))},
        "fore_R": {"rotation": L((0, 0, -6))}, "fore_L": {"rotation": L((0, 0, 6))},
        "hand_R": {"rotation": L((0, 0, -10))}, "hand_L": {"rotation": L((0, 0, 10))},
        "leg_R": {"rotation": mg(L((2, 0, -9)), sn(T, 1.5, 0, 0.0, steps=8))}, "leg_L": {"rotation": mg(L((-2, 0, 9)), sn(T, 1.5, 0, 0.3, steps=8))},
        "foot_R": {"rotation": L((26, 0, 0))}, "foot_L": {"rotation": L((26, 0, 0))},
    }
    bones.update(wings(T, 9.0))
    bones.update(flow(T, 5.0, 0.0))
    bones.update(skirt_flow(T, 4.0))
    bones.update(face_tracks(kinds, "stern"))
    clips["hover"] = {"loop": True, "length": T, "bones": bones}

    # ---- powered_cast: the arms come forward and up, the head lifts, the mouth opens, the hair surges, the wings spread wide
    T = 1.2
    L = lambda v: hold(v, T)
    b2 = {
        "body": {"position": sn(T, 1.6, 1, 0.0, steps=8), "rotation": L((-4, 0, 0))},
        "chest": {"rotation": L((-6, 0, 0))}, "head": {"rotation": mg(L((-14, 0, 0)), sn(T, 2.0, 0, 0.1, steps=8))},
        "arm_R": {"rotation": L((84, 0, -24))}, "arm_L": {"rotation": L((84, 0, 24))}, "fore_R": {"rotation": L((8, 0, 0))}, "fore_L": {"rotation": L((8, 0, 0))},
        "leg_R": {"rotation": L((10, 0, -8))}, "leg_L": {"rotation": L((-10, 0, 8))},
        "foot_R": {"rotation": L((28, 0, 0))}, "foot_L": {"rotation": L((28, 0, 0))},
    }
    b2.update(wings(T * 0.5, 16.0))
    b2.update(flow(T, 9.0, 0.0))
    b2.update(skirt_flow(T, 8.0))
    b2.update(face_tracks(kinds, "shout"))
    clips["powered_cast"] = {"loop": True, "length": T, "bones": b2}

    # ---- shout: the hover pose with the mouth open (the moment a page is called)
    b3 = dict(bones)
    b3.update(face_tracks(kinds, "shout"))
    clips["shout"] = {"loop": True, "length": 2.8, "bones": b3}
    return clips


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    rows = []
    for form in FORMS:
        if only and form not in only:
            continue
        body_at, wing_at, wing_glow_at, fx_at = body_atlas(form), wings_atlas(form), wings_atlas(form, True), fx_atlas(form)
        for lod in (0, 1, 2):
            b = build_body(form, lod, body_at)
            rows.append((b.m.id, b.m.cubes(), b.m.quads()))
            if not count_only:
                b.m.save()
                body_at.save(b.m.id, b.m.tex_scale)
        w = build_wings(form, wing_at)
        rows.append((w.m.id, w.m.cubes(), w.m.quads()))
        fx = build_fx(form, fx_at)
        rows.append((fx.m.id, fx.m.cubes(), fx.m.quads()))
        if not count_only:
            w.m.save()
            wing_at.save(w.m.id)
            wing_glow_at.save(w.m.id + "_glow")
            fx.m.save()
            fx_at.save(fx.m.id)
            G.write_animations("sylph_" + form, small_animations() if form == "small" else powered_animations())
    if not count_only:
        A.halo(128).save(os.path.join(G.ROOT, "textures", "entity", "sylph_halo.png"), optimize=True)
    for r in rows:
        print("%-24s cubes %4d  quads %5d" % r)


if __name__ == "__main__":
    main()
