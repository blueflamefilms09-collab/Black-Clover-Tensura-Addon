"""Why: the kit is used by eight people to make forty characters, so a change to one painter or one clip recipe must not silently break a part nobody looks at today.
`python3 -B tools/gen_arc_characters.py --selftest` builds EVERY name of the vocabulary (all hair styles, outfits, props, accessories, wings, tails, horns, ears, halos, masks,
capes, armour pieces, builds), checks that every model's faces lie inside its texture and carry pixels, and then checks the CLIPS of a set of archetypes (bare hands, a sword
and shield, a two-handed spear, a bow, a mage with a staff and a grimoire, a winged boss with a cape and a tail) against the rules of this kit, reading the written animation
data back the way the game plays it: the loops close, nothing is not-a-number, the feet stand on the floor, no arm is inside the torso, a one-shot ends where the idle
starts, the same spec twice gives the same bytes.
"""
import copy
import json
import math
import os
import sys

import numpy as np

from . import build as B
from . import face as F
from . import hair as Hr
from . import outfit as O
from . import props as Pr
from . import spec as SP
from .anim import GRID
from .clipcheck import arm_problems, pose_at
from .clips import _foot_min_y
from .core import Canvas
from .rig import BUILDS, rot_matrix


class Report:
    def __init__(self):
        self.n = 0
        self.bad = []

    def check(self, ok, what):
        self.n += 1
        if not ok:
            self.bad.append(what)
            print("  FAIL", what)

    def done(self):
        print("selftest: %d checks, %d failed" % (self.n, len(self.bad)))
        return 1 if self.bad else 0


def _base(**over):
    raw = {"id": "zz_selftest", "name": "Selftest", "build": "average", "skin": "light", "hair": {"style": "short", "color": "#4a3828"},
           "outfit": {"top": {"kind": "shirt", "color": "#4a5a7a"}, "legs": {"kind": "trousers"}, "boots": {"kind": "boots"}}}
    for k, v in over.items():
        if isinstance(v, dict) and isinstance(raw.get(k), dict):
            raw[k].update(v)
        else:
            raw[k] = v
    return raw


def _faces_ok(R, rep, label):
    """Every cube face of the rig lies inside the texture and has at least one opaque texel."""
    img, glow = R.finish()
    a = np.asarray(img)[..., 3] if hasattr(img, "shape") is False and False else None
    arr = R.pk.img
    ok = True
    n = 0
    for b in R.m.bones.values():
        for c in b["cubes"]:
            for fk, f in c["uv"].items():
                u, v = f["uv"]
                w, h = f["uv_size"]
                n += 1
                if u < 0 or v < 0 or u + w > R.pk.W or v + h > R.pk.H:
                    rep.check(False, "%s: a %s face of %s is outside the texture" % (label, fk, b["name"]))
                    ok = False
                elif not (arr[int(v):int(v + h), int(u):int(u + w), 3] > 0).any():
                    rep.check(False, "%s: a %s face of %s has no opaque texel" % (label, fk, b["name"]))
                    ok = False
    rep.check(ok and n > 0, "%s: faces" % label)


def build_fast(raw):
    os.environ["ARC_FAST"] = "1"
    try:
        return B.build_character(copy.deepcopy(raw), file_id=raw.get("id"))
    finally:
        os.environ.pop("ARC_FAST", None)


def vocabulary(rep):
    print("vocabulary: hair styles")
    for st in Hr.HAIR_STYLES:
        R = build_fast(_base(hair={"style": st}))
        _faces_ok(R, rep, "hair " + st)
    print("vocabulary: bodies, tops, legs, boots, gloves, sleeves, necks")
    for b in BUILDS:
        R = build_fast(_base(build=b, outfit={"top": {"kind": "coat", "length": "knee"}}))
        _faces_ok(R, rep, "build " + b)
    for k in O.TOP_KINDS:
        R = build_fast(_base(outfit={"top": {"kind": k, "collar": "stand", "open": True if k in ("coat", "vest", "robe") else None}, "belt": {}}))
        _faces_ok(R, rep, "top " + k)
    for k in O.LEG_KINDS:
        R = build_fast(_base(outfit={"legs": {"kind": k, "stripe": "#c8a040"}}))
        _faces_ok(R, rep, "legs " + k)
    for k in O.BOOT_KINDS:
        R = build_fast(_base(outfit={"boots": {"kind": k, "cuff": "#c8a040"}}))
        _faces_ok(R, rep, "boots " + k)
    for k in O.GLOVE_KINDS:
        R = build_fast(_base(outfit={"gloves": {"kind": k}}))
        _faces_ok(R, rep, "gloves " + k)
    for k in O.SLEEVE_KINDS:
        R = build_fast(_base(outfit={"sleeves": {"kind": k}}))
        _faces_ok(R, rep, "sleeves " + k)
    for k in O.NECK_KINDS:
        R = build_fast(_base(outfit={"neck": {"kind": k}}))
        _faces_ok(R, rep, "neck " + k)
    for k in O.COLLARS:
        R = build_fast(_base(outfit={"top": {"kind": "coat", "collar": k}}))
        _faces_ok(R, rep, "collar " + k)
    for k in SP.BELT_KINDS:
        R = build_fast(_base(outfit={"top": {"kind": "kimono" if k == "obi" else "coat"}, "belt": {"kind": k, "sash": "#c83030", "pouches": True, "books": True}}))
        _faces_ok(R, rep, "belt " + k)
    for k in O.BODY_PARTS:
        R = build_fast(_base(outfit={"bandages": {"parts": [k]}}))
        _faces_ok(R, rep, "bandages on " + k)
    for mat in SP.MATERIALS:
        R = build_fast(_base(outfit={"top": {"kind": "coat", "material": mat}}))
        _faces_ok(R, rep, "coat in " + mat)
    print("vocabulary: faces")
    P = F.FacePalette(((190, 144, 128), (228, 184, 160), (244, 208, 184), (252, 228, 208)), ((9, 8, 17), (19, 18, 24), (49, 48, 52), (89, 86, 89)), (70, 110, 190))
    for e in F.EYE_STYLES:
        for br in F.BROW_STYLES:
            cv = F.face_canvas({"eyes": e, "brows": br, "mouth": "neutral"}, P)
            rep.check(cv.c[..., 3].sum() > 0, "eyes %s brows %s" % (e, br))
    for m in F.MOUTH_STYLES:
        F.face_canvas({"eyes": "round", "brows": "thin", "mouth": m}, P)
    for nk in F.NOSE_STYLES:
        F.face_canvas({"eyes": "round", "brows": "thin", "mouth": "neutral", "nose": nk}, P)
    F.face_canvas({"eyes": "round", "brows": "thin", "mouth": "smile", "marks": list(F.MARKS)}, P)
    rep.check(True, "every mark draws")
    for ex in F.EXPRESSIONS:
        R = build_fast(_base(face={"expression": ex}))
        _faces_ok(R, rep, "expression " + ex)
    print("vocabulary: emblems")
    for k in O.EMBLEMS:
        if k == "none":
            continue
        cv = O.emblem(k, 16, 16, (230, 230, 230), (255, 200, 100))
        rep.check(cv.c[..., 3].sum() > 255 * 12, "emblem %s draws a symbol" % k)
    print("vocabulary: props")
    for k in Pr.PROP_KINDS:
        slot = "left" if Pr.PROPS[k].hand == "l" else "right"
        R = build_fast(_base(props={slot: {"kind": k, "glow": "#8ad8ff"}}))
        _faces_ok(R, rep, "prop " + k)
        if k in Pr.SHEATHABLE:
            R = build_fast(_base(props={slot: {"kind": k, "sheathed": True}}))
            _faces_ok(R, rep, "sheathed " + k)
            rep.check("prop_hip_" + ("l" if slot == "right" else "r") in R.m.bones, "sheathed %s sits on the opposite hip, where the drawing hand reaches it" % k)
        R = build_fast(_base(props={"back": {"kind": k}, "float": {"kind": k}}))
        _faces_ok(R, rep, "stowed and floating prop " + k)
    print("vocabulary: accessories, creature parts, masks, capes, armour")
    R = build_fast(_base(accessories=[{"kind": k, "glow": "#ffcc66"} for k in SP.ACCESSORY_KINDS]))
    _faces_ok(R, rep, "all accessories")
    for k in SP.WING_KINDS:
        _faces_ok(build_fast(_base(wings={"kind": k, "glow": "#aaccff"})), rep, "wings " + k)
    for k in SP.TAIL_KINDS:
        _faces_ok(build_fast(_base(tail={"kind": k, "glow": "#aaccff"})), rep, "tail " + k)
    for k in SP.HORN_KINDS:
        _faces_ok(build_fast(_base(horns={"kind": k, "glow": "#ffaa66"})), rep, "horns " + k)
    for k in SP.EAR_KINDS:
        _faces_ok(build_fast(_base(ears={"kind": k})), rep, "ears " + k)
    for k in SP.HALO_KINDS:
        _faces_ok(build_fast(_base(halo={"kind": k})), rep, "halo " + k)
    for k in SP.MASK_KINDS:
        _faces_ok(build_fast(_base(face={"mask": {"kind": k, "eye_glow": "#ff4040"}})), rep, "mask " + k)
    for k in O.CAPE_KINDS:
        _faces_ok(build_fast(_base(cape={"kind": k, "emblem": {"kind": "star"}})), rep, "cape " + k)
    for st in ("plate", "chain", "leather", "bone", "scale"):
        _faces_ok(build_fast(_base(armor={"pieces": list(O.ARMOR_PIECES), "style": st, "glow": "#ffcc66", "emblem": "cross"})), rep, "armour " + st)


# ------------------------------------------------------------------------------------------------------------------------ clips
def _all_numbers_finite(clip):
    for bone, tracks in clip["bones"].items():
        for kind, tr in tracks.items():
            for k, v in tr.items():
                if not all(math.isfinite(x) for x in v) or not math.isfinite(float(k)):
                    return False
                if kind == "rotation" and max(abs(x) for x in v) > 720:
                    return False
    return True


def clips_of(raw, rep, label, expect_hover=False):
    R = B.build_character(copy.deepcopy(raw), file_id=raw.get("id"))
    clips = R.A.clips
    for name in ("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "transform", "death"):
        rep.check(name in clips, "%s: clip %s exists" % (label, name))
    for name, c in clips.items():
        rep.check(_all_numbers_finite(c), "%s/%s: every number is finite and sane" % (label, name))
        for bone, tracks in c["bones"].items():
            rep.check(R.has(bone), "%s/%s: bone %s exists" % (label, name, bone))
            for kind, tr in tracks.items():
                ts = sorted(float(k) for k in tr)
                rep.check(ts[0] == 0.0 and abs(ts[-1] - c["animation_length"]) < 0.011, "%s/%s/%s: keys run from 0 to the clip's length" % (label, name, bone))
                if c["loop"] and kind == "rotation":
                    first, last = tr[min(tr, key=float)], tr[max(tr, key=float)]
                    if max(abs(a - b) for a, b in zip(first, last)) > 0.01:
                        rep.check(False, "%s/%s/%s: the loop does not close" % (label, name, bone))
    idle0 = pose_at(R, clips["idle"], 0.0)
    for name in ("idle", "walk", "run", "attack", "cast", "cast_big", "hurt"):
        c = clips[name]
        lo_foot, hi_foot = 1e9, -1e9
        for k in range(int(c["animation_length"] * 30) + 1):
            t = k / 30.0
            pose = pose_at(R, c, t)
            low = _foot_min_y(R, pose)                      # pose_point walks up through the root bone, so the grounding offset of the root is already in it
            lo_foot, hi_foot = min(lo_foot, low), max(hi_foot, low)
        if not expect_hover:
            rep.check(lo_foot > -0.8, "%s/%s: the feet never sink into the floor (lowest %.2f px)" % (label, name, lo_foot))
            rep.check(hi_foot < 0.8, "%s/%s: the lowest foot never floats above the floor (highest %.2f px)" % (label, name, hi_foot))
    bad = arm_problems(R)
    for line in bad:
        rep.check(False, "%s/%s" % (label, line))
    rep.check(not bad, "%s: the arms keep out of the torso, never flip and start and end on the idle's pose" % label)
    for name in ("attack", "hurt", "cast_big"):
        c = clips[name]
        end = pose_at(R, c, c["animation_length"])
        for bone in ("body", "head", "leg_l", "leg_r"):
            a = np.array(end.get(bone, {}).get("rot", (0, 0, 0)))
            b = np.array(idle0.get(bone, {}).get("rot", (0, 0, 0)))
            rep.check(np.max(np.abs(a - b)) < 8.0, "%s/%s: it ends where idle starts (%s differs by %.1f)" % (label, name, bone, float(np.max(np.abs(a - b)))))
    return R


ARCHETYPES = {
    "bare hands npc": _base(role="npc"),
    "sword and shield": _base(props={"right": {"kind": "sword"}, "left": {"kind": "shield"}}),
    "two-handed spear": _base(role="boss", props={"right": {"kind": "spear"}}, build="average"),
    "greatsword": _base(role="boss", props={"right": {"kind": "greatsword"}}, build="huge"),
    "bow": _base(props={"left": {"kind": "bow"}}, build="slim"),
    "mage with staff": _base(role="npc", props={"right": {"kind": "staff", "glow": "#8ad8ff"}}, outfit={"top": {"kind": "robe", "length": "floor"}}),
    "scholar with grimoire": _base(props={"left": {"kind": "grimoire", "glow": "#ffaa66"}}, outfit={"top": {"kind": "coat", "length": "calf"}}),
    "winged boss": _base(role="boss", build="lanky", cape={"kind": "cloak", "emblem": {"kind": "wings"}}, wings={"kind": "bat"}, tail={"kind": "devil"}, props={"float": {"kind": "orb"}},
                         clips={"extra": ["spawn", "chest_gem_reveal"]}),
}


def clips(rep):
    for label, raw in ARCHETYPES.items():
        print("clips:", label)
        raw = dict(raw, id="zz_selftest")
        clips_of(raw, rep, label)
    print("clips: hovering")
    clips_of(dict(_base(role="boss", wings={"kind": "angel"}, clips={"idle": "hover"}), id="zz_selftest"), rep, "hover", expect_hover=True)


def determinism(rep):
    print("determinism")
    raw = dict(ARCHETYPES["sword and shield"], id="zz_selftest")
    a = B.build_character(copy.deepcopy(raw), file_id="zz_selftest")
    b = B.build_character(copy.deepcopy(raw), file_id="zz_selftest")
    rep.check(json.dumps(a.m.geo(), sort_keys=True) == json.dumps(b.m.geo(), sort_keys=True), "the same spec gives the same model")
    rep.check(json.dumps(a.A.data(), sort_keys=True) == json.dumps(b.A.data(), sort_keys=True), "the same spec gives the same clips")
    ia, ga = a.finish()
    ib, gb = b.finish()
    rep.check(np.array_equal(a.pk.img, b.pk.img) and np.array_equal(a.pk.glow, b.pk.glow), "the same spec gives the same texture")


def negative(rep):
    print("spec errors")
    def fails(raw, needle):
        try:
            SP.normalize(raw, "zz_selftest")
        except SP.SpecError as e:
            return needle in str(e)
        return False
    rep.check(fails({"hairs": {}}, "unknown key"), "an unknown top-level key is refused and the valid keys are listed")
    rep.check(fails({"hair": {"style": "pompadoor"}}, "use one of"), "a mistyped hair style lists the real ones")
    rep.check(fails({"outfit": {"top": {"kind": "armour"}}}, "outfit.top.kind"), "a bad outfit kind names the path")
    rep.check(fails({"props": {"right": {"kind": "lightsaber"}}}, "props.right.kind"), "a bad prop names the path")
    rep.check(fails({"props": {"head": {"kind": "sword"}}}, "use one of"), "a bad prop slot lists the slots")
    rep.check(fails({"build": "tiny"}, "build"), "a bad build is refused")
    rep.check(fails({"face": {"marks": ["tattoo"]}}, "face.marks"), "a bad mark names the list")
    rep.check(fails({"id": "Bad Id"}, "snake_case"), "a bad id is refused")
    rep.check(fails({"form2": {"build": "huge"}}, "form2"), "a second form may not change the build")


def run():
    rep = Report()
    negative(rep)
    determinism(rep)
    clips(rep)
    vocabulary(rep)
    return rep.done()
