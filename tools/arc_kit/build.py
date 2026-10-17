"""Why: the pipeline that turns one spec into one model: skeleton, head (face, hair, mask), body and clothing, cape, armour, accessories, creatures' parts
(wings, tail, horns, ears, halo), props, the character's own `extra(kit, rig)`, the clips, the texture. Everything is deterministic: the same spec always writes
the same bytes (no randomness, no clock), which is what `gen_arc_characters.py --check` relies on.
"""
import os

from . import color as C
from . import face as F
from . import hair as Hr
from . import outfit as O
from . import spec as SP
from .core import hair_paint, skin_paint, fabric, px
from .rig import Dims, Rig

SKIRT_KINDS = ("coat", "robe", "dress", "kimono", "tabard", "tunic", "uniform")


class Look:
    """The palettes resolved once from the spec: skin (4 tones), hair (4 tones), the face palette."""

    def __init__(self, S):
        self.skin = C.skin_palette(S["skin"])
        h = S["hair"]
        self.H = C.hair_shades(h["color"], 4)
        self.streak = C.hair_shades(h["streak"], 4) if h.get("streak") else None
        self.ribbon = C.rgb(h["ribbon"]) if h.get("ribbon") else None
        f = S["face"]
        self.eye = C.rgb(f["eye_color"])
        self.eye_glow = C.rgb(f["eye_glow"]) if f.get("eye_glow") else None
        self.glow = C.rgb(S["glow"]["color"]) if S["glow"].get("color") else None


def skeleton(R):
    d = R.d
    R.bone("root", None, (0, 0, 0))
    R.bone("body", "root", (0, d.hip_y, 0))
    R.bone("head", "body", (0, d.sh_y, 0))
    for suf, sg in (("_l", 1), ("_r", -1)):
        R.bone("arm" + suf, "body", (sg * d.arm_x, d.sh_piv, 0))
        R.bone("forearm" + suf, "arm" + suf, (sg * d.arm_x, d.elbow_y, 0))
        R.bone("hand" + suf, "forearm" + suf, (sg * d.arm_x, d.wrist_y, 0))
        R.bone("leg" + suf, "root", (sg * d.leg_x, d.hip_y, 0))
        R.bone("shin" + suf, "leg" + suf, (sg * d.leg_x, d.knee_y, 0))
        R.bone("skirt" + suf, "root", (sg * d.leg_x, d.hip_y, 0))


def build_head(R, S, Lk):
    d = R.d
    f = S["face"]
    h = S["hair"]
    style = h["style"]
    fringe, hmask = Hr.build_hair(R, style, Lk.H, Lk.streak, Lk.ribbon, {"cover": h.get("cover")})
    brow = C.rgb(f["brow_color"]) if f.get("brow_color") else None
    lip = C.rgb(f["lip_color"]) if f.get("lip_color") else None
    P = F.FacePalette(Lk.skin, Lk.H, Lk.eye, Lk.eye_glow, C.rgb(f["sclera"]) if f.get("sclera") else (238, 234, 238), brow, lip)
    cv = F.face_canvas(f, P, fringe)
    if style == "hood":
        hp = lambda face, i, j, fw, fh: (None, None)
    hair_p = hair_paint(salt=2, cols=Lk.H) if style != "hood" else fabric(Lk.H, salt=3, period=7.0, amp=0.2, seam=False)
    painter = F.head_painter(cv, P, hmask, hair_p, skin_paint(Lk.skin, 1))
    R.box("head", (-4, d.head_y, -4), (8, 8, 8), painter, dens={"front": 4}, faces=("front", "left", "right", "back", "top"))
    if d.neck > 0:                                               # the neck: a skin box from the shoulder line into the head, on the head bone so it turns with it
        nw = max(2.5, min(4.0, d.tw * 0.42))
        R.box("head", (-nw / 2, d.sh_y - 0.2, -nw / 2 + 0.2), (nw, d.neck + 0.7, nw - 0.4), skin_paint(Lk.skin, 4), faces=("front", "left", "right", "back"))
    R.facts["face_canvas"] = cv
    R.facts["head_painter"] = painter


def build_character(raw, extra=None, kit=None, file_id=None, model_name=None, form=1, post=None):
    """Builds one model from a raw spec dict. Returns the Rig (finished; nothing written yet); see `save`."""
    S = SP.normalize(raw, file_id)
    d = Dims(S["build"], S["height"], S["widen"])
    name = model_name or S["id"]
    R = Rig(name, d)
    R.form = form
    R.spec = S
    R.raw = raw
    Lk = Look(S)
    R.look = Lk
    skeleton(R)
    build_head(R, S, Lk)
    body = O.build_body(R, S, Lk)
    R.body = body
    from . import accessories as Ac
    from . import props as Pr
    from . import recipes as An
    Ac.build_all(R, S, Lk, body)
    Pr.build_props(R, S, Lk)
    if extra is not None:
        extra(kit, R)
    An.build_clips(R, S)
    if post is not None:
        post(kit, R)                 # the character's own touch-ups of the finished clips (rig.A.clips)
    return R


def save(R, root=None):
    """Writes geo, animation and texture files of a built rig; returns the dict of facts for the manifest."""
    img, glow = R.finish()
    img, glow, hh = R.cropped(img, glow)
    R.m.tex_h = hh
    R.m.save(R.A)
    R.m.save_texture(img, glow)
    st = R.stats()
    st["texture"] = [R.pk.W, hh]
    return st
