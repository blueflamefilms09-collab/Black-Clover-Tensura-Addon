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
import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402
import sylph_art as A  # noqa: E402
import sylph_geo_lib as G  # noqa: E402
import sylph_wing_art as WA  # noqa: E402

HID = 0.001                                                   # a hidden face is scaled to this (not 0: a zero scale makes bad normals)
FORMS = ("small", "powered")
# the face plates of each form (a clip shows one): the first three of the small form are the first version's; the others were added with the poses and faces (laughing with closed eyes, happy, stern,
# smug, angry, sad / crying, surprised, sleepy, sulking); the powered form has the stern face of the third picture, the shout, a smile, anger and a laugh
KINDS = {"small": ("laugh", "smile", "stern", "smug", "angry", "sad", "surprised", "sleepy", "pout"), "powered": ("stern", "shout", "smile", "angry", "laugh")}
mg = G.merge
_CLIP = [1.0]                                                                  # the length of the clip being built (clip_len): the sine tracks run over the WHOLE clip


def clip_len(t):
    _CLIP[0] = t
    return t


def sn(length, amp, axis=0, phase=0.0, steps=8, offset=(0, 0, 0), bias=0.0):
    """A closed sine track of one rotation axis with `length` the PERIOD in seconds; it runs over the whole clip being built (T / length whole cycles). (The kit's own G.sine makes one cycle and
    then holds, which froze the sways after their first period; this repeats the cycle for the whole clip.)"""
    tr = cyc(_CLIP[0], length, amp, axis, phase, bias, per=steps)
    return {t: tuple(v[i] + offset[i] for i in range(3)) for t, v in tr.items()}


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
    kinds = KINDS[form]
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
            at.add("ribbon%d" % i, A.ribbon(12.0, 34.0, 12, 80 + i, "powered", phase=1.4 * i, sway=1.0 + 0.2 * i))
    return at.pack()


def wings_atlas_v1(form, glow=False):
    """The first version of the wings (kept as the *_v1 models): broad leaf wings with a few big cells."""
    at = G.Atlas(1024 if form == "powered" else 512)
    if form == "small":
        at.add("fore", A.wing(24, 7.5, 16, 5, "fore", "small", glow))
        at.add("hind", A.wing(18, 6.0, 16, 6, "hind", "small", glow))
    else:
        at.add("fore", A.wing(66, 18.0, 12, 5, "fore", "powered", glow))
        at.add("hind", A.wing(48, 14.0, 12, 6, "hind", "powered", glow))
        for i in range(3):
            at.add("petal%d" % i, A.petal(18 + 3 * i, 11 + 2 * i, 12, 90 + i, "powered", glow))
    return at.pack()


def wings_atlas(form, phase=None):
    """The wing sheet of the second version: the membrane with its vein network (phase None), or one phase of the sheen (0..K-1); every phase has the very same layout."""
    at = G.Atlas(1024 if form == "powered" else 640)
    for kind in ("fore", "hind"):
        w = WA.wing(form, kind)
        at.add(kind, w.base() if phase is None else w.sheen(phase))
    if form == "powered":
        for i in range(3):                                                                         # the petal wings of the skirt (unchanged): the sheen of a petal is its rim glow tinted by the phase
            if phase is None:
                at.add("petal%d" % i, A.petal(18 + 3 * i, 11 + 2 * i, 12, 90 + i, "powered", False))
            else:
                g = np.asarray(A.petal(18 + 3 * i, 11 + 2 * i, 12, 90 + i, "powered", True)).astype(np.float32)
                tint = WA.pastel(0.30 + phase / float(WA.K) + 0.05 * i, 0.35, 1.0)
                g[..., :3] = np.minimum(g[..., :3], tint[None, None])
                g[..., 3] *= 0.8
                g[..., :3] *= (g[..., 3:4] > 4)
                at.add("petal%d" % i, Image.fromarray(g.astype(np.uint8), "RGBA"))
    return at.pack()


def sheen_sheet(form):
    """The K phases of the sheen stacked top to bottom (each one the layout of the wing sheet), at half size: the client picks phases by shifting V (client/prop/SylphDraw). Returns a PIL image."""
    phases = [wings_atlas(form, k) for k in range(WA.K)]
    w, h = phases[0].image.size
    sheet = Image.new("RGBA", (w, h * WA.K), (0, 0, 0, 0))
    for k, at in enumerate(phases):
        assert at.image.size == (w, h) and at.rects == phases[0].rects
        sheet.paste(at.image, (0, k * h))
    sheet = sheet.resize((w // 2, h * WA.K // 2), Image.LANCZOS)
    a = np.asarray(sheet).copy()
    a[a[..., 3] < 3] = 0                                                                         # nothing under a (nearly) transparent texel: the safe additive rule
    return Image.fromarray(a, "RGBA")


def fx_atlas(form):
    at = G.Atlas(512)
    kinds = KINDS[form]
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
        for i, (x, side) in enumerate(((-4.6, -2), (-2.3, -1), (0.0, 0), (2.3, 1), (4.6, 2))):
            prev, y = "head", 8.6
            img = "ribbon%d" % (i % 3)
            s1 = 8.0 if i % 2 == 0 else -8.0
            for k in range(4):
                nm = "hair%d_%d" % (i, k)
                bend = -side * 9.0 if k == 0 else s1 * (1.0 if k % 2 else -1.2)
                b.bone(nm, (x, y, 1.0 - 0.4 * i), prev, rot=(0, 0, bend))
                b.top(nm, x - 6.0, y, 1.0 - 0.4 * i, 12.0, 8.5, 0.12, {"north": W(img, 0, 1.0 - 0.25 * (k + 1), 1, 1.0 - 0.25 * k), "south": W(img, 0, 1.0 - 0.25 * (k + 1), 1, 1.0 - 0.25 * k)})
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
    if lod == 0:                                                                                   # a second ring of ruffles half a step round and a little lower: a fuller, layered skirt
        for i in range(n):
            yaw = 360.0 * (i + 0.5) / n
            b.bone("skb%d" % i, (0, top0, 0), "body", rot=(0, yaw, 0))
            b.bone("skb%d_f" % i, (0, top0, -2.0), "skb%d" % i)
            for (top, ln, flare, r, t) in tiers[1:]:
                wid = 2.0 * math.pi * (r + 0.3) / n * 1.38
                nm = "ruffle%d" % ((t + 1) % 4)
                b.top("skb%d_f" % i, -wid / 2, top - ln - 0.7, -r - 0.3, wid, ln, 0.15, {"north": W(nm), "south": W(nm), "up": None, "down": None, "west": None, "east": None}, rot=(flare + 6, 0, 0), pivot=(0, top - 0.7, -r - 0.3))


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
    if lod == 2:
        low_poly(b, at, form, o)
        return b
    torso(b, at, lod, o)
    head(b, at, lod, o["head_dy"], KINDS[form], powered=form == "powered")
    arms(b, at, lod, o)
    legs(b, at, lod, o)
    skirt(b, at, lod, o)
    if lod == 0:
        add_wings(b, form, None)                                                                   # the wing bones (no plates: those are the wings model's) so the clips name real bones of this model
    return b


def low_poly(b, at, form, o):
    """The lowest level of detail (about a third of the quads of the half level): boxes for the body, one box a limb, the face painted straight on the head (no plates), a hair cap, the bob and the
    ribbons / ahoge as single cards, four ruffles in two tiers. Same bones as the full model where it has a part (a clip's other tracks are ignored)."""
    W = lambda n, *a: win(at, n, *a)
    skin = W("skin")
    limb = {"*": W("limb"), "up": None, "down": None}
    dress = {"*": W("dress"), "up": None, "down": None}
    powered = form == "powered"
    kind = "stern" if powered else "smile"
    b.bone("body", (0, o["hip"], 0))
    b.bone("chest", (0, o["waist"], 0), "body")
    b.bone("neck", (0, o["neck"], 0), "chest")
    b.bone("head", (0, o["head_y"], 0), "neck")
    b.box("chest", 0, o["chest_y"], 0, o["chest_w"], o["chest_h"], 3.6, {"*": skin, "north": W("bodice"), "south": W("bodice_back"), "up": None, "down": None})
    b.box("body", 0, (o["waist_y"] + o["hip_y"]) / 2, 0, o["chest_w"] - 0.6, o["waist_h"] + o["hip_h"], 3.3, dress)
    # head: the face is painted on the cranium and the chin
    b.dy = o["head_dy"]
    up, low = face_faces(at, kind)
    b.top("head", -4.5, 2.5, -4.0, 9.0, FACE_UP, 8.0, {"*": skin, "north": up, "up": None, "down": None})
    b.top("head", -3.5, 0.0, -3.5, 7.0, 2.6, 7.0, {"*": skin, "north": low, "up": None, "down": None})
    b.top("head", -4.9, 6.4, -4.4, 9.8, 2.6, 8.8, {"north": W("fringe_n"), "south": W("fringe_s"), "west": W("fringe_w"), "east": W("fringe_e"), "up": W("hair_top"), "down": None})
    b.top("head", -4.8, 0.8, 3.1, 9.6, 7.4, 1.5, {"south": W("hair_back"), "north": None, "west": None, "east": None, "up": None, "down": None})
    if powered:
        for j, i in enumerate((0, 2, 4)):                                                              # three of the five ribbons, each one card, on the pivots of the full model's first segments
            x = (-4.6, -2.3, 0.0, 2.3, 4.6)[i]
            z = 1.0 - 0.4 * i
            b.bone("hair%d_0" % i, (x, 8.6, z), "head")
            b.top("hair%d_0" % i, x - 6.0, 8.6, z, 12.0, 30.0, 0.12, {"north": W("ribbon%d" % j), "south": W("ribbon%d" % j)})
    else:
        b.bone("ahoge0", (0.0, 8.9, -0.4), "head")
        b.top("ahoge0", -0.8, 8.9, -0.45, 1.6, 4.0, 0.12, {"north": W("ahoge"), "south": W("ahoge")})
    b.dy = 0.0
    # limbs: one box each (the knee and the elbow do not bend at this distance)
    for s, tag in ((-1, "R"), (1, "L")):
        sx, sy = s * o["shoulder_x"], o["shoulder_y"]
        b.bone("arm_" + tag, (sx, sy, 0), "chest")
        b.box("arm_" + tag, sx, sy - (o["upper"] + o["fore"]) / 2, 0, o["arm_w"], o["upper"] + o["fore"], o["arm_w"], limb)
        x = s * o["leg_x"]
        b.bone("leg_" + tag, (x, o["hip"], 0), "body")
        leg_len = o["hip"] - 1.0
        b.box("leg_" + tag, x, o["hip"] - leg_len / 2, 0, o["leg_w"], leg_len, o["leg_w"], {"*": W("limb"), "up": None, "down": None})
        b.box("leg_" + tag, x, 0.5, -1.2, o["leg_w"], 1.0, 4.2, {"*": W("limb"), "up": W("foot"), "down": None})
    # four ruffles in two tiers
    n = 4
    tiers = [o["tiers"][0], o["tiers"][-1]]
    for i in range(n):
        yaw = 360.0 * i / n
        b.bone("sk%d" % i, (0, o["skirt_top"], 0), "body", rot=(0, yaw, 0))
        b.bone("sk%d_f" % i, (0, o["skirt_top"], -2.0), "sk%d" % i)
        for (top, ln, flare, r, t) in tiers:
            wid = 2.0 * math.pi * r / n * 1.38
            nm = "ruffle%d" % t
            b.top("sk%d_f" % i, -wid / 2, top - ln, -r, wid, ln, 0.15, {"north": W(nm), "south": W(nm), "up": None, "down": None, "west": None, "east": None}, rot=(flare, 0, 0), pivot=(0, top, -r))


def skeleton_only(b, o):
    b.bone("body", (0, o["hip"], 0))
    b.bone("chest", (0, o["waist"], 0), "body")
    b.bone("neck", (0, o["neck"], 0), "chest")
    b.bone("head", (0, o["head_y"], 0), "neck")


def build_wings_v1(form, at):
    o = SMALL if form == "small" else POWERED
    b = B("sylph_%s_wings_v1" % form, at)
    skeleton_only(b, o)
    add_wings_v1(b, form, at)
    return b


def build_wings(form, at):
    o = SMALL if form == "small" else POWERED
    b = B("sylph_%s_wings" % form, at)
    skeleton_only(b, o)
    add_wings(b, form, at)
    return b


def add_wings_v1(b, form, at):
    """The first version of the wing bones (kept for the *_v1 models) (and the petal bones of the powered form). With an atlas the plates are built too (the wings model); with None only the bones (the full body model carries them, so
    its one animation file names real bones)."""
    W = (lambda n, *a: win(at, n, *a)) if at is not None else None
    if form == "small":
        # (name, root x, root y, root z, length, chord, roll, yaw): a wing is a plate standing in the x-y plane (seen broad from the front and the back), rolled up and out, swept back
        specs = [("f", 1.6, 22.6, 2.6, 24.0, 7.5, 62.0, 10.0), ("h", 1.4, 21.4, 2.9, 18.0, 6.0, 24.0, 14.0)]
    else:
        specs = [("f", 2.0, 35.0, 2.6, 66.0, 18.0, 36.0, 8.0), ("h", 1.8, 32.6, 3.0, 48.0, 14.0, 12.0, 12.0)]
    for tag, s in (("R", -1), ("L", 1)):
        for nm, rx_, ry_, rz_, ln, ch, roll, yaw in specs:
            bn = "wing_%s_%s" % (nm, tag)
            b.bone(bn, (s * rx_, ry_, rz_), "chest", rot=(0, s * -yaw, s * roll))
            img = "fore" if nm == "f" else "hind"
            cut = 0.6                                                                              # the inner 60 % of the wing, then a joint the clips bend (the tip whips a little behind the root)
            ox = s * rx_
            # the picture has its root at the left and the leading edge up; from the front the model's right (-x) is on the viewer's right, so the +x wing (tip on the viewer's left) is mirrored
            x0 = ox if s > 0 else ox - ln * cut
            if at is not None: b.top(bn, x0, ry_ - ch * 0.40, rz_ - 0.05, ln * cut, ch, 0.1, {"north": (W(img, 0, 0, cut, 1), s > 0), "south": (W(img, 0, 0, cut, 1), s < 0), "up": None, "down": None, "west": None, "east": None})
            jx = ox + s * ln * cut
            bo = "wingo_%s_%s" % (nm, tag)
            b.bone(bo, (jx, ry_, rz_), bn)
            x1 = jx if s > 0 else jx - ln * (1 - cut)
            if at is not None: b.top(bo, x1, ry_ - ch * 0.40, rz_ - 0.05, ln * (1 - cut), ch, 0.1, {"north": (W(img, cut, 0, 1, 1), s > 0), "south": (W(img, cut, 0, 1, 1), s < 0), "up": None, "down": None, "west": None, "east": None})
    if form == "powered":                                                                          # the petal wings of the skirt: three a side, a fan from the hip
        for tag, s in (("R", -1), ("L", 1)):
            for k, (roll, yaw, ln, ch, z) in enumerate(((66.0, 6.0, 18.0, 11.0, 2.4), (38.0, 10.0, 21.0, 13.0, 2.8), (12.0, 14.0, 24.0, 15.0, 3.2))):
                bn = "petal_%d_%s" % (k, tag)
                b.bone(bn, (s * 2.6, 27.4, z), "body", rot=(0, s * -yaw, s * roll))
                if at is None:
                    continue
                img = W("petal%d" % k)
                x0 = s * 2.6 if s > 0 else s * 2.6 - ln
                face = {"north": (img, s > 0), "south": (img, s < 0), "up": None, "down": None, "west": None, "east": None}
                b.top(bn, x0, 27.4 - ch * 0.5, z - 0.05, ln, ch, 0.1, face)




SEG = ("wing", "wingo", "wingp", "wingq", "wingr")             # the bones of one wing from the root to the tip: "wing" keeps the name of the first version's root bone
# (root x, root y, root z, roll up from horizontal, sweep back) of the front (fore) and the rear (hind) wing of each form
WING_ROOTS = {"small": {"fore": (1.6, 22.6, 2.6, 60.0, 10.0), "hind": (1.4, 21.4, 2.9, 20.0, 16.0)},
              "powered": {"fore": (2.0, 35.0, 2.6, 38.0, 8.0), "hind": (1.8, 32.6, 3.0, 8.0, 12.0)}}
# the rest pose of the joints (degrees) from the second card on: (bend in the wing's plane, curl out of it): the S of the leading edge and the cup of the membrane
WING_JOINTS = {"fore": ((0.0, 0.0), (0.0, 3.0), (0.0, 4.0), (0.0, 5.0), (0.0, 5.0)), "hind": ((0.0, 0.0), (0.0, 3.0), (0.0, 4.0), (0.0, 5.0), (0.0, 5.0))}


def wing_bones(form, kind, tag):
    """The names of the bones of one wing, root to tip."""
    n = len(WA.SPECS[(form, kind)]["cuts"]) - 1
    return ["%s_%s_%s" % (SEG[j], "f" if kind == "fore" else "h", tag) for j in range(n)]


def add_wings(b, form, at):
    """The wings of the second version: each wing is a chain of 4 (small) or 5 (powered) cards from the root to the tip, cut out of ONE painted wing picture (tools/sylph_wing_art.py), hinged so
    the wing is an S in its plane and cups a little out of it; the clips whip the chain (the rear pair lags the front pair). With an atlas the plates are built too (the wings model); with None
    only the bones (the full body model carries them, so its one animation file names real bones). Then the petal wings of the skirt (powered form)."""
    W = (lambda n, *a: win(at, n, *a)) if at is not None else None
    for tag, s in (("R", -1), ("L", 1)):
        for kind in ("fore", "hind"):
            spec = WA.SPECS[(form, kind)]
            rx_, ry_, rz_, roll, yaw = WING_ROOTS[form][kind]
            wu, hu, cuts = spec["wu"], spec["hu"], spec["cuts"]
            ox = s * rx_
            names = wing_bones(form, kind, tag)
            prev = None
            for j, bn in enumerate(names):
                a, c = cuts[j], cuts[j + 1]
                bend, curl = WING_JOINTS[kind][j]
                if j == 0:
                    b.bone(bn, (ox, ry_, rz_), "chest", rot=(0, s * -yaw, s * roll))
                else:
                    b.bone(bn, (ox + s * wu * a, ry_, rz_), prev, rot=(0, s * -curl, s * bend))
                prev = bn
                if at is None:
                    continue
                x0 = ox + wu * a if s > 0 else ox - wu * c
                faces = {"north": (W(kind, a, 0, c, 1), s < 0), "south": (W(kind, a, 0, c, 1), s > 0), "up": None, "down": None, "west": None, "east": None}
                b.top(bn, x0, ry_ + spec["root_y"] - hu, rz_ - 0.05, wu * (c - a), hu, 0.1, faces)
    add_petals(b, form, at)


def add_petals(b, form, at):
    if form != "powered":
        return
    W = (lambda n, *a: win(at, n, *a)) if at is not None else None
    for tag, s in (("R", -1), ("L", 1)):
        for k, (roll, yaw, ln, ch, z) in enumerate(((66.0, 6.0, 18.0, 11.0, 2.4), (38.0, 10.0, 21.0, 13.0, 2.8), (12.0, 14.0, 24.0, 15.0, 3.2))):
            bn = "petal_%d_%s" % (k, tag)
            b.bone(bn, (s * 2.6, 27.4, z), "body", rot=(0, s * -yaw, s * roll))
            if at is None:
                continue
            img = W("petal%d" % k)
            x0 = s * 2.6 if s > 0 else s * 2.6 - ln
            face = {"north": (img, s > 0), "south": (img, s < 0), "up": None, "down": None, "west": None, "east": None}
            b.top(bn, x0, 27.4 - ch * 0.5, z - 0.05, ln, ch, 0.1, face)


def build_fx(form, at):
    o = SMALL if form == "small" else POWERED
    b = B("sylph_%s_fx" % form, at)
    skeleton_only(b, o)
    kinds = KINDS[form]
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
def cyc(T, period, amp, axis, phase=0.0, bias=0.0, per=8):
    """A closed sine track of one rotation axis over a clip of T seconds with T / period whole cycles (keys every period / per): the clip loops without a jump and the motion runs the WHOLE clip."""
    n = max(1, int(round(T / period)))
    steps = per * n
    return {T * i / steps: tuple(bias + amp * math.sin(math.tau * (n * i / steps + phase)) if k == axis else 0.0 for k in range(3)) for i in range(steps + 1)}


def wing_flutter(form, T, period, a, lag=0.10, size=1.0, bias=0.0):
    """The tracks of every wing bone: the root card beats about the z axis (the V of the wings opens and closes) and twists a little about the length axis; each card further out swings more and
    a little later than the one before (a wave runs to the tip); the rear pair lags the front pair by `lag` of a cycle; the left and the right wing mirror. a: the root's swing in degrees;
    size: the scale of each wing (the root card's scale carries its whole chain): the small form seated and laughing (the first picture) has small wings, standing (the second picture) long ones."""
    out = {}
    for tag, s in (("R", -1), ("L", 1)):
        for kind, off, k in (("fore", 0.0, 1.0), ("hind", lag, 0.85)):
            for j, bn in enumerate(wing_bones(form, kind, tag)):
                ph = off + 0.05 * j
                if j == 0:                                                                                                       # the root card flaps (the V opens and closes) and twists
                    rot = mg(cyc(T, period, s * a * k * 1.4, 2, ph, bias=s * bias), cyc(T, period, a * 0.30 * k, 0, ph + 0.25))
                else:                                                                                                            # the cards further out only bend out of the plane (a rotation about the shared edge never opens a gap between two cards)
                    rot = mg(cyc(T, period, -s * a * k * (0.35 + 0.12 * j), 1, ph + 0.1), cyc(T, period, a * 0.06 * k * j, 0, ph + 0.25))
                out[bn] = {"rotation": rot}
                if j == 0 and size != 1.0:
                    out[bn]["scale"] = {0.0: (size, size, size), T: (size, size, size)}
    return out


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
    kinds = KINDS["small"]
    skirt = ["sk%d_f" % i for i in range(10)] + ["skb%d_f" % i for i in range(10)]
    locks = ["lock_side_R", "lock_side_L"] + ["lock_b%d" % i for i in range(5)]
    ahoge = ["ahoge%d" % k for k in range(5)]
    clips = {}

    wings = lambda T, period, a, lag=0.10, size=1.0: wing_flutter("small", T, period, a, lag, size)

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
    def seated(T):
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
        bones.update(wings(T, 0.8, 4.5, size=0.62))
        bones.update(skirt_sway(T, 3.5))
        bones.update(hair(T, 4.0))
        bones.update(face_tracks(kinds, "laugh"))
        return bones

    T = clip_len(2.4)
    bones = seated(T)
    clips["idle"] = {"loop": True, "length": T, "bones": bones}

    # ---- laugh: the same pose, a bigger fit: the head thrown back, the chest heaving, the hand pressed to the mouth
    T = clip_len(1.2)
    L = lambda v: hold(v, T)
    b2 = seated(T)
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
    b2.update(wings(T, 0.6, 6.0, size=0.62))
    b2.update(skirt_sway(T, 6.0))
    b2.update(hair(T, 6.0))
    b2.update(face_tracks(kinds, "laugh"))
    clips["laugh"] = {"loop": True, "length": T, "bones": b2}

    # ---- point: the second picture: standing, both index fingers at the head, a bright smile, a hop in the air
    T = clip_len(1.6)
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
    b3.update(wings(T, 0.4, 7.0))
    b3.update(skirt_sway(T, 7.0))
    b3.update(hair(T, 5.0))
    b3.update(face_tracks(kinds, "smile"))
    clips["point"] = {"loop": True, "length": T, "bones": b3}

    # ---- cast: she stands up in the air, one arm thrust forward with the palm open (the gust leaves her hand), the other arm back, the hair and skirt blowing back
    T = clip_len(1.0)
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
    b4.update(wings(T, 0.25, 10.0))
    b4.update(skirt_sway(T, 8.0))
    b4.update(hair(T, 9.0))
    b4.update(face_tracks(kinds, "smile"))
    clips["cast"] = {"loop": True, "length": T, "bones": b4}

    # ---- fly: she follows the caster: leaning forward, the legs trailing, the wings beating fast
    T = clip_len(0.6)
    L = lambda v: hold(v, T)
    b5 = {
        "body": {"position": sn(T, 0.6, 1, 0.0, steps=8), "rotation": L((-30, 0, 0))},
        "chest": {"rotation": L((-6, 0, 0))}, "head": {"rotation": mg(L((24, 0, 0)), sn(T, 2.0, 0, 0.1, steps=8))},
        "arm_R": {"rotation": L((-30, 0, -30))}, "arm_L": {"rotation": L((-30, 0, 30))}, "fore_R": {"rotation": L((20, 0, 0))}, "fore_L": {"rotation": L((20, 0, 0))},
        "leg_R": {"rotation": mg(L((-34, 0, -3)), sn(T, 5.0, 0, 0.0, steps=8))}, "leg_L": {"rotation": mg(L((-40, 0, 3)), sn(T, 5.0, 0, 0.5, steps=8))},
        "shin_R": {"rotation": L((-30, 0, 0))}, "shin_L": {"rotation": L((-34, 0, 0))},
        "foot_R": {"rotation": L((40, 0, 0))}, "foot_L": {"rotation": L((40, 0, 0))},
    }
    b5.update(wings(T, 0.2, 11.0))
    b5.update(skirt_sway(T, 9.0))
    b5.update(hair(T, 10.0))
    b5.update(face_tracks(kinds, "smile"))
    clips["fly"] = {"loop": True, "length": T, "bones": b5}
    return clips


def powered_animations():
    kinds = KINDS["powered"]
    skirt = ["sk%d_f" % i for i in range(10)] + ["skb%d_f" % i for i in range(10)]
    hairs = ["hair%d_%d" % (i, k) for i in range(5) for k in range(4)]
    petals = ["petal_%d_%s" % (k, t) for k in range(3) for t in "RL"]
    locks = ["lock_side_R", "lock_side_L"]
    clips = {}

    wings = lambda T, period, a, lag=0.12: wing_flutter("powered", T, period, a, lag)

    def flow(T, amp, lag):
        out = {}
        for i in range(5):
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
    T = clip_len(2.8)
    L = lambda v: hold(v, T)
    bones = {
        "body": {"position": sn(T, 1.2, 1, 0.0, steps=8), "rotation": L((0, 0, 0))},
        "chest": {"rotation": mg(L((0, 0, 0)), sn(T, 1.0, 0, 0.0, steps=8))},
        "head": {"rotation": mg(L((-4, 0, 0)), sn(T, 1.5, 1, 0.1, steps=8))},
        "arm_R": {"rotation": mg(L((4, 0, -44)), sn(T, 3.0, 2, 0.0, steps=8))}, "arm_L": {"rotation": mg(L((4, 0, 44)), sn(T, -3.0, 2, 0.0, steps=8))},
        "fore_R": {"rotation": L((0, 0, -6))}, "fore_L": {"rotation": L((0, 0, 6))},
        "hand_R": {"rotation": L((0, 0, -10))}, "hand_L": {"rotation": L((0, 0, 10))},
        "leg_R": {"rotation": mg(L((2, 0, -14)), sn(T, 1.5, 0, 0.0, steps=8))}, "leg_L": {"rotation": mg(L((-2, 0, 14)), sn(T, 1.5, 0, 0.3, steps=8))},
        "foot_R": {"rotation": L((26, 0, 0))}, "foot_L": {"rotation": L((26, 0, 0))},
    }
    bones.update(wings(T, 1.4, 4.0))
    bones.update(flow(T, 8.0, 0.0))
    bones.update(skirt_flow(T, 4.0))
    bones.update(face_tracks(kinds, "stern"))
    clips["hover"] = {"loop": True, "length": T, "bones": bones}

    # ---- powered_cast: the arms come forward and up, the head lifts, the mouth opens, the hair surges, the wings spread wide
    T = clip_len(1.2)
    L = lambda v: hold(v, T)
    b2 = {
        "body": {"position": sn(T, 1.6, 1, 0.0, steps=8), "rotation": L((-4, 0, 0))},
        "chest": {"rotation": L((-6, 0, 0))}, "head": {"rotation": mg(L((-14, 0, 0)), sn(T, 2.0, 0, 0.1, steps=8))},
        "arm_R": {"rotation": L((84, 0, -42))}, "arm_L": {"rotation": L((84, 0, 42))}, "fore_R": {"rotation": L((8, 0, 0))}, "fore_L": {"rotation": L((8, 0, 0))},
        "leg_R": {"rotation": L((10, 0, -8))}, "leg_L": {"rotation": L((-10, 0, 8))},
        "foot_R": {"rotation": L((28, 0, 0))}, "foot_L": {"rotation": L((28, 0, 0))},
    }
    b2.update(wings(T, 0.3, 7.0))
    b2.update(flow(T, 9.0, 0.0))
    b2.update(skirt_flow(T, 8.0))
    b2.update(face_tracks(kinds, "shout"))
    clips["powered_cast"] = {"loop": True, "length": T, "bones": b2}

    # ---- shout: the hover pose with the mouth open (the moment a page is called)
    b3 = dict(bones)
    b3.update(face_tracks(kinds, "shout"))
    clips["shout"] = {"loop": True, "length": 2.8, "bones": b3}
    return clips


# ====================================================================================================================== more poses (the moods of sylph/SylphMood)
SMALL_SKIRT = ["sk%d_f" % i for i in range(10)] + ["skb%d_f" % i for i in range(10)]
SMALL_LOCKS = ["lock_side_R", "lock_side_L"] + ["lock_b%d" % i for i in range(5)]
SMALL_AHOGE = ["ahoge%d" % k for k in range(5)]


def _sway(names, T, amp, step, axis):
    return {nm: {"rotation": sn(T, amp, axis, i * step, steps=8)} for i, nm in enumerate(names)}


def _small_dress_hair(T, skirt_amp, hair_amp):
    out = _sway(SMALL_SKIRT, T, skirt_amp, 0.055, 0)
    out.update(_sway(SMALL_LOCKS, T, hair_amp, 0.05, 2))
    for k, nm in enumerate(SMALL_AHOGE):
        out[nm] = {"rotation": sn(T, hair_amp * (0.8 + 0.3 * k), 2, 0.1 * k, steps=8)}
    return out


def small_pose_clips():
    """The small form's moods (docs/magic/sylph.md, "Poses"): each clip is a pose, its face and the sway of her wings, skirt and hair, looped, bone rotation only. The server picks the clip by
    what happens (sylph/SylphMood); the first picture's seated swing, a wave, arms crossed with a smug face, a sulk, a victory, a sleepy curl, a startled jump back, a flinch, worry, crying and a
    cheer."""
    kinds = KINDS["small"]
    clips = {}

    def make(name, T, face, bones, wing=(0.8, 4.0, 1.0), skirt=3.0, hair=3.0):
        b = {k: v for k, v in bones.items()}
        b.update(wing_flutter("small", T, wing[0], wing[1], 0.10, wing[2]))
        b.update(_small_dress_hair(T, skirt, hair))
        b.update(face_tracks(kinds, face))
        clips[name] = {"loop": True, "length": T, "bones": b}

    # ---- swing: sitting on the air and swinging both legs (the first picture's seat), a hand at the cheek, a bright smile
    T = clip_len(2.0)
    L = lambda v: hold(v, T)
    make("swing", T, "smile", {
        "body": {"position": sn(T, 0.45, 1, 0.0), "rotation": mg(L((-10, 0, 3)), sn(T, 1.5, 2, 0.0))},
        "chest": {"rotation": mg(L((10, 0, 0)), sn(T, 1.5, 0, 0.25))},
        "head": {"rotation": mg(L((-6, 8, -6)), sn(T, 2.0, 1, 0.1))},
        "leg_R": {"rotation": L((84, 0, -2))}, "leg_L": {"rotation": L((84, 0, 2))},
        "shin_R": {"rotation": mg(L((-86, 0, 0)), sn(T, 26.0, 0, 0.0))}, "shin_L": {"rotation": mg(L((-86, 0, 0)), sn(T, 26.0, 0, 0.5))},
        "foot_R": {"rotation": mg(L((24, 0, 0)), sn(T, 8.0, 0, 0.1))}, "foot_L": {"rotation": mg(L((24, 0, 0)), sn(T, 8.0, 0, 0.6))},
        "arm_R": {"rotation": mg(L((68, 0, -14)), sn(T, 2.0, 0, 0.0))}, "fore_R": {"rotation": L((98, 0, 0))},
        "arm_L": {"rotation": L((-18, 0, 14))}, "fore_L": {"rotation": L((12, 0, 0))},
    }, wing=(0.8, 3.5, 0.62))

    # ---- wave: standing in the air, one arm high, the forearm waving, a little hop
    T = clip_len(1.2)
    L = lambda v: hold(v, T)
    make("wave", T, "smile", {
        "body": {"position": sn(T, 1.1, 1, 0.0), "rotation": mg(L((-4, 0, 0)), sn(T, 2.0, 2, 0.0))},
        "chest": {"rotation": L((2, 0, 0))}, "head": {"rotation": mg(L((-4, 0, 8)), sn(T, 3.0, 2, 0.1))},
        "arm_R": {"rotation": L((8, 0, -152))}, "fore_R": {"rotation": mg(L((0, 0, 10)), sn(0.4, 30.0, 2, 0.0))},
        "arm_L": {"rotation": mg(L((10, 0, 16)), sn(T, 3.0, 0, 0.0))}, "fore_L": {"rotation": L((18, 0, 0))},
        "leg_R": {"rotation": mg(L((6, 0, -3)), sn(T, 5.0, 0, 0.0))}, "shin_R": {"rotation": L((-10, 0, 0))},
        "leg_L": {"rotation": mg(L((-22, 0, 3)), sn(T, 5.0, 0, 0.5))}, "shin_L": {"rotation": L((-36, 0, 0))}, "foot_L": {"rotation": L((30, 0, 0))},
    }, wing=(0.4, 6.0, 1.0), skirt=5.0, hair=4.0)

    # ---- crossed: hovering with the arms folded and a smug little smile, the head tipped, the feet crossed
    T = clip_len(2.4)
    L = lambda v: hold(v, T)
    make("crossed", T, "smug", {
        "body": {"position": sn(T, 0.6, 1, 0.0), "rotation": mg(L((-3, 0, 0)), sn(T, 1.0, 2, 0.0))},
        "chest": {"rotation": L((-2, 0, 0))}, "head": {"rotation": mg(L((-8, 10, 8)), sn(T, 1.5, 1, 0.1))},
        "arm_R": {"rotation": L((26, 0, 6))}, "fore_R": {"rotation": L((104, 0, 58))},
        "arm_L": {"rotation": L((26, 0, -6))}, "fore_L": {"rotation": L((104, 0, -58))},
        "leg_R": {"rotation": L((6, 0, 5))}, "leg_L": {"rotation": L((-4, 0, -5))}, "shin_R": {"rotation": L((-6, 0, 0))}, "shin_L": {"rotation": L((-18, 0, 0))},
        "foot_L": {"rotation": L((28, 0, 0))}, "foot_R": {"rotation": L((20, 0, 0))},
    }, wing=(1.2, 3.0, 1.0), skirt=3.0, hair=2.5)

    # ---- pout: turned away with the arms crossed, the head down and aside, the cheeks puffed, a small stamp of the foot
    T = clip_len(2.0)
    L = lambda v: hold(v, T)
    make("pout", T, "pout", {
        "body": {"position": sn(T, 0.3, 1, 0.0), "rotation": mg(L((0, 38, 0)), sn(T, 1.0, 2, 0.0))},
        "chest": {"rotation": L((6, -8, 0))}, "head": {"rotation": mg(L((14, -50, 6)), sn(T, 1.5, 0, 0.0))},
        "arm_R": {"rotation": L((26, 0, 6))}, "fore_R": {"rotation": L((104, 0, 58))},
        "arm_L": {"rotation": L((26, 0, -6))}, "fore_L": {"rotation": L((104, 0, -58))},
        "leg_R": {"rotation": mg(L((2, 0, 2)), sn(T / 2, 5.0, 0, 0.0))}, "leg_L": {"rotation": L((-2, 0, -2))}, "shin_R": {"rotation": L((-4, 0, 0))}, "shin_L": {"rotation": L((-4, 0, 0))},
    }, wing=(1.0, 2.0, 0.8), skirt=3.0, hair=2.0)

    # ---- victory: both arms thrown up, a big hop, the head back, a laugh
    T = clip_len(1.0)
    L = lambda v: hold(v, T)
    make("victory", T, "laugh", {
        "body": {"position": sn(T, 2.0, 1, 0.0), "rotation": mg(L((-6, 0, 0)), sn(T, 3.0, 2, 0.0))},
        "chest": {"rotation": L((-8, 0, 0))}, "head": {"rotation": mg(L((-16, 0, 0)), sn(T, 3.0, 2, 0.1))},
        "arm_R": {"rotation": mg(L((6, 0, -166)), sn(T, 6.0, 2, 0.0))}, "fore_R": {"rotation": L((20, 0, 0))},
        "arm_L": {"rotation": mg(L((6, 0, 166)), sn(T, -6.0, 2, 0.0))}, "fore_L": {"rotation": L((20, 0, 0))},
        "leg_R": {"rotation": mg(L((4, 0, -12)), sn(T, 6.0, 0, 0.0))}, "leg_L": {"rotation": mg(L((-6, 0, 12)), sn(T, 6.0, 0, 0.5))},
        "shin_R": {"rotation": L((-14, 0, 0))}, "shin_L": {"rotation": L((-22, 0, 0))},
    }, wing=(0.25, 9.0, 1.0), skirt=8.0, hair=8.0)

    # ---- sleepy: curled up with the knees to the chest and the arms around them, the head nodding, small slow wings
    T = clip_len(3.2)
    L = lambda v: hold(v, T)
    make("sleepy", T, "sleepy", {
        "body": {"position": sn(T, 0.6, 1, 0.0), "rotation": mg(L((-22, 0, 0)), sn(T, 1.5, 2, 0.0))},
        "chest": {"rotation": mg(L((24, 0, 0)), sn(T, 1.5, 0, 0.1))}, "head": {"rotation": mg(L((26, 0, -16)), sn(T, 6.0, 0, 0.0))},
        "arm_R": {"rotation": L((62, 0, 6))}, "fore_R": {"rotation": L((112, 0, 20))},
        "arm_L": {"rotation": L((62, 0, -6))}, "fore_L": {"rotation": L((112, 0, -20))},
        "leg_R": {"rotation": L((98, 0, -4))}, "leg_L": {"rotation": L((102, 0, 4))}, "shin_R": {"rotation": L((-128, 0, 0))}, "shin_L": {"rotation": L((-132, 0, 0))},
        "foot_R": {"rotation": L((20, 0, 0))}, "foot_L": {"rotation": L((20, 0, 0))},
    }, wing=(1.6, 2.0, 0.55), skirt=1.5, hair=1.5)

    # ---- startled: thrown back in the air, the arms flung out, the legs kicked up, the wings flared
    T = clip_len(0.9)
    L = lambda v: hold(v, T)
    make("startled", T, "surprised", {
        "body": {"position": mg(hold((0, 2.2, 0), T), sn(T, 0.6, 1, 0.0)), "rotation": mg(L((14, 0, 0)), sn(T, 2.5, 2, 0.0))},
        "chest": {"rotation": L((8, 0, 0))}, "head": {"rotation": mg(L((-10, 0, 0)), sn(T, 3.0, 2, 0.0))},
        "arm_R": {"rotation": mg(L((-10, 0, -78)), sn(T / 3, 4.0, 2, 0.0))}, "fore_R": {"rotation": L((10, 0, -16))},
        "arm_L": {"rotation": mg(L((-10, 0, 78)), sn(T / 3, 4.0, 2, 0.5))}, "fore_L": {"rotation": L((10, 0, 16))},
        "leg_R": {"rotation": L((-26, 0, -8))}, "leg_L": {"rotation": L((-18, 0, 8))}, "shin_R": {"rotation": L((-44, 0, 0))}, "shin_L": {"rotation": L((-30, 0, 0))},
    }, wing=(0.3, 12.0, 1.1), skirt=9.0, hair=9.0)

    # ---- hurt: a flinch: hunched over, one hand pressed to the chest, trembling
    T = clip_len(0.8)
    L = lambda v: hold(v, T)
    make("hurt", T, "angry", {
        "body": {"position": sn(T / 2, 0.5, 0, 0.0), "rotation": mg(L((-6, 0, 6)), sn(T / 4, 2.0, 2, 0.0))},
        "chest": {"rotation": L((24, 0, 0))}, "head": {"rotation": mg(L((20, 0, -8)), sn(T / 4, 2.0, 1, 0.0))},
        "arm_R": {"rotation": L((52, 0, 12))}, "fore_R": {"rotation": L((124, 0, 8))},
        "arm_L": {"rotation": L((-24, 0, 42))}, "fore_L": {"rotation": L((20, 0, 0))},
        "leg_R": {"rotation": L((16, 0, -4))}, "leg_L": {"rotation": L((8, 0, 4))}, "shin_R": {"rotation": L((-30, 0, 0))}, "shin_L": {"rotation": L((-20, 0, 0))},
    }, wing=(0.4, 5.0, 0.85), skirt=6.0, hair=6.0)

    # ---- worry: hands clasped at the chest, the head tipped, trembling a little, the wings drooping
    T = clip_len(2.0)
    L = lambda v: hold(v, T)
    make("worry", T, "sad", {
        "body": {"position": sn(T, 0.4, 1, 0.0), "rotation": mg(L((4, 0, 0)), sn(T / 4, 1.2, 2, 0.0))},
        "chest": {"rotation": L((8, 0, 0))}, "head": {"rotation": mg(L((10, 0, 12)), sn(T, 2.0, 2, 0.0))},
        "arm_R": {"rotation": L((44, 0, 24))}, "fore_R": {"rotation": L((118, 0, 36))},
        "arm_L": {"rotation": L((44, 0, -24))}, "fore_L": {"rotation": L((118, 0, -36))},
        "leg_R": {"rotation": L((4, 0, -2))}, "leg_L": {"rotation": L((-4, 0, 2))}, "shin_R": {"rotation": L((-8, 0, 0))}, "shin_L": {"rotation": L((-8, 0, 0))},
    }, wing=(1.0, 2.0, 0.7), skirt=2.5, hair=2.5)

    # ---- cry: both hands at the eyes, the head down, the shoulders shaking
    T = clip_len(1.2)
    L = lambda v: hold(v, T)
    make("cry", T, "sad", {
        "body": {"position": sn(T / 4, 0.3, 1, 0.0), "rotation": L((6, 0, 0))},
        "chest": {"rotation": mg(L((14, 0, 0)), sn(T / 4, 3.0, 0, 0.0))}, "head": {"rotation": mg(L((24, 0, 0)), sn(T / 4, 2.5, 2, 0.0))},
        "arm_R": {"rotation": L((78, 0, -8))}, "fore_R": {"rotation": mg(L((150, 0, 12)), sn(T / 4, 4.0, 0, 0.0))},
        "arm_L": {"rotation": L((78, 0, 8))}, "fore_L": {"rotation": mg(L((150, 0, -12)), sn(T / 4, 4.0, 0, 0.5))},
        "leg_R": {"rotation": L((6, 0, -2))}, "leg_L": {"rotation": L((2, 0, 2))}, "shin_R": {"rotation": L((-10, 0, 0))}, "shin_L": {"rotation": L((-10, 0, 0))},
    }, wing=(1.2, 1.5, 0.6), skirt=2.0, hair=2.5)

    # ---- cheer: a fist pumped over the head again and again, the other hand on the hip, a big smile
    T = clip_len(1.0)
    L = lambda v: hold(v, T)
    make("cheer", T, "smile", {
        "body": {"position": sn(T / 2, 1.2, 1, 0.0), "rotation": mg(L((-6, 0, 0)), sn(T, 2.0, 2, 0.0))},
        "chest": {"rotation": L((-4, 0, 0))}, "head": {"rotation": mg(L((-8, 8, 0)), sn(T / 2, 2.0, 0, 0.0))},
        "arm_R": {"rotation": mg(L((10, 0, -128)), sn(T / 2, 14.0, 2, 0.0))}, "fore_R": {"rotation": mg(L((70, 0, -10)), sn(T / 2, 24.0, 0, 0.0))},
        "arm_L": {"rotation": L((14, 0, 36))}, "fore_L": {"rotation": L((110, 0, 20))},
        "leg_R": {"rotation": mg(L((4, 0, -6)), sn(T / 2, 5.0, 0, 0.0))}, "leg_L": {"rotation": mg(L((-14, 0, 6)), sn(T / 2, 5.0, 0, 0.5))}, "shin_L": {"rotation": L((-28, 0, 0))},
    }, wing=(0.25, 8.0, 1.0), skirt=6.0, hair=6.0)
    return clips


POWERED_SKIRT = ["sk%d_f" % i for i in range(10)] + ["skb%d_f" % i for i in range(10)]
POWERED_HAIR = ["hair%d_%d" % (i, k) for i in range(5) for k in range(4)]
POWERED_PETALS = ["petal_%d_%s" % (k, t) for k in range(3) for t in "RL"]


def powered_pose_clips():
    """The powered form's moods: the battle stance (the third picture, arms and legs spread, a stern face), fury (fists down, an angry face, the wings beating) and triumph (both arms raised, a laugh)."""
    kinds = KINDS["powered"]
    clips = {}

    def make(name, T, face, bones, wing, hair_amp, skirt_amp):
        b = {k: v for k, v in bones.items()}
        b.update(wing_flutter("powered", T, wing[0], wing[1], 0.12, 1.0, bias=wing[2]))
        for i in range(5):
            for k in range(4):
                b["hair%d_%d" % (i, k)] = {"rotation": sn(T, hair_amp * (1.0 + 0.25 * k) * (1 if (i + k) % 2 == 0 else -1), 2, 0.08 * k + 0.13 * i, steps=8)}
        for i, nm in enumerate(["lock_side_R", "lock_side_L"]):
            b[nm] = {"rotation": sn(T, hair_amp * 0.8 * (1 if i == 0 else -1), 2, 0.1 * i, steps=8)}
        for i, nm in enumerate(POWERED_SKIRT):
            b[nm] = {"rotation": sn(T, skirt_amp, 0, i * 0.05, steps=8)}
        for i, nm in enumerate(POWERED_PETALS):
            b[nm] = {"rotation": sn(T, skirt_amp * 1.3 * (1 if i % 2 == 0 else -1), 2, 0.08 * i, steps=8)}
        b.update(face_tracks(kinds, face))
        clips[name] = {"loop": True, "length": T, "bones": b}

    T = clip_len(2.8)
    L = lambda v: hold(v, T)
    make("battle", T, "stern", {
        "body": {"position": sn(T, 1.2, 1, 0.0), "rotation": L((0, 0, 0))},
        "chest": {"rotation": mg(L((-2, 0, 0)), sn(T, 1.0, 0, 0.0))}, "head": {"rotation": mg(L((4, 0, 0)), sn(T, 1.5, 1, 0.1))},
        "arm_R": {"rotation": mg(L((2, 0, -62)), sn(T, 3.0, 2, 0.0))}, "arm_L": {"rotation": mg(L((2, 0, 62)), sn(T, -3.0, 2, 0.0))},
        "fore_R": {"rotation": L((0, 0, -6))}, "fore_L": {"rotation": L((0, 0, 6))}, "hand_R": {"rotation": L((0, 0, -12))}, "hand_L": {"rotation": L((0, 0, 12))},
        "leg_R": {"rotation": mg(L((2, 0, -22)), sn(T, 1.5, 0, 0.0))}, "leg_L": {"rotation": mg(L((-2, 0, 22)), sn(T, 1.5, 0, 0.3))},
        "foot_R": {"rotation": L((26, 0, 0))}, "foot_L": {"rotation": L((26, 0, 0))},
    }, (1.4, 3.5, -10.0), 9.0, 4.5)

    T = clip_len(1.6)
    L = lambda v: hold(v, T)
    make("fury", T, "angry", {
        "body": {"position": sn(T, 1.0, 1, 0.0), "rotation": L((-5, 0, 0))},
        "chest": {"rotation": L((-4, 0, 0))}, "head": {"rotation": mg(L((14, 0, 0)), sn(T / 2, 1.5, 1, 0.1))},
        "arm_R": {"rotation": mg(L((-6, 0, -24)), sn(T / 2, 3.0, 2, 0.0))}, "arm_L": {"rotation": mg(L((-6, 0, 24)), sn(T / 2, -3.0, 2, 0.0))},
        "fore_R": {"rotation": L((78, 0, -10))}, "fore_L": {"rotation": L((78, 0, 10))},
        "leg_R": {"rotation": L((8, 0, -10))}, "leg_L": {"rotation": L((-8, 0, 10))}, "foot_R": {"rotation": L((26, 0, 0))}, "foot_L": {"rotation": L((26, 0, 0))},
    }, (0.4, 5.0, -4.0), 12.0, 7.0)

    T = clip_len(1.4)
    L = lambda v: hold(v, T)
    make("triumph", T, "laugh", {
        "body": {"position": sn(T, 2.0, 1, 0.0), "rotation": mg(L((-4, 0, 0)), sn(T, 2.0, 2, 0.0))},
        "chest": {"rotation": L((-8, 0, 0))}, "head": {"rotation": mg(L((-14, 0, 0)), sn(T, 2.5, 2, 0.1))},
        "arm_R": {"rotation": mg(L((4, 0, -152)), sn(T, 5.0, 2, 0.0))}, "arm_L": {"rotation": mg(L((4, 0, 152)), sn(T, -5.0, 2, 0.0))},
        "fore_R": {"rotation": L((10, 0, 0))}, "fore_L": {"rotation": L((10, 0, 0))},
        "leg_R": {"rotation": mg(L((2, 0, -16)), sn(T, 4.0, 0, 0.0))}, "leg_L": {"rotation": mg(L((-2, 0, 16)), sn(T, 4.0, 0, 0.3))},
        "foot_R": {"rotation": L((26, 0, 0))}, "foot_L": {"rotation": L((26, 0, 0))},
    }, (0.35, 6.0, -6.0), 11.0, 8.0)
    return clips


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    only = [a for a in sys.argv[1:] if not a.startswith("--")]
    rows = []
    for form in FORMS:
        if only and form not in only:
            continue
        body_at, wing_at, fx_at = body_atlas(form), wings_atlas(form), fx_atlas(form)
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
            sheen_sheet(form).save(os.path.join(G.ROOT, "textures", "entity", w.m.id + "_glow.png"), optimize=True)
            v1 = build_wings_v1(form, wings_atlas_v1(form))                                         # the first-version wings stay as *_v1 (never drawn by default)
            v1.m.save()
            wings_atlas_v1(form).save(v1.m.id)
            wings_atlas_v1(form, True).save(v1.m.id + "_glow")
            fx.m.save()
            fx_at.save(fx.m.id)
            clips = small_animations() if form == "small" else powered_animations()
            clips.update(small_pose_clips() if form == "small" else powered_pose_clips())
            G.write_animations("sylph_" + form, clips)
    if not count_only:
        A.halo(128).save(os.path.join(G.ROOT, "textures", "entity", "sylph_halo.png"), optimize=True)
    for r in rows:
        print("%-24s cubes %4d  quads %5d" % r)


if __name__ == "__main__":
    main()
