"""The HD arc pass: the one module a character painter, the generator, the merger and the preview import (docs/arc/hd_arc.md).

A hand-painted character `tools/arc_hd/chars/<look name>.py` is written like the Tier List ones (tools/tier_bosses/hd, docs/bosses/hd_outfits.md), only the blueprint comes from the 64x64 generator
of the arcs instead of a remake module:

    import os, sys
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    import arc_hdlib as A
    H = A.H                                                                    # the HD library of the Tier List bosses (Canvas, from64, gem, eye ...)

    def paint(spec, cid, masks):                                               # cid = the look name ('meliodas', 'meliodas_form2')
        img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair="anime_spike_swept")   # the 64x64 generator's result, scalp painted for that hair
        cv = H.Canvas(256, slim)
        A.bridge(cv, img64, glow64, spec, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)    # the baseline everywhere, except the islands you redraw
        ...                                                                    # your redraws with cv.face(...) and the H.* helpers
        skin, glow = cv.finish()
        return skin, glow, hair, hrgb, slim                                     # (skin 256, glow 256 or None, hair style id, hair rgb, slim)

Numpy and Pillow only. Deterministic.
"""
import contextlib
import copy
import importlib.util
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
HD_DIR = os.path.join(TOOLS, "tier_bosses", "hd")
for _p in (HERE, os.path.join(TOOLS, "playermodel"), TOOLS, os.path.join(TOOLS, "arc")):
    if _p not in sys.path:
        sys.path.insert(0, _p)
if HD_DIR not in sys.path:
    sys.path.append(HD_DIR)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import characters as CH  # noqa: E402
import gen_hair  # noqa: E402
import gen_skins as GS  # noqa: E402
import hdlib as H  # noqa: E402
import hair_map as HM  # noqa: E402
import roster as RO  # noqa: E402
from arc_kit import color as C  # noqa: E402
from arc_kit import spec as S  # noqa: E402

# hdlib puts its own folder first on sys.path; that folder holds a boss module called code.py, which would shadow the standard library's `code`: move it to the end (the modules are imported already)
if HD_DIR in sys.path:
    sys.path.remove(HD_DIR)
    sys.path.append(HD_DIR)

ASSETS = GS.ASSETS
SKINS = GS.SKINS
LOOKS_JSON = GS.OUT_LOOKS
FRAG_DIR = os.path.join(HERE, "looks")
CHARS_DIR = os.path.join(HERE, "chars")
SIZES = (64, 128, 256)


# ================================================================================================ the look table and the jobs
_masks = {}
_jobs = {}


def hair_masks():
    """{hair style id: its scalp mask} of every kit style (built in memory; nothing is written)."""
    if not _masks:
        gen_hair.build()
        _masks.update({sid: getattr(st, "mask", None) for sid, st in gen_hair.STYLES.items()})
    return _masks


def job_table():
    """{look name: normalized spec}: the same names and specs as tools/playermodel/gen_skins.py main() paints (a base id, `<id>_form2`, the texture-only variants of the Dark Disciple and the Avatar)."""
    if not _jobs:
        for cid in CH.humanoid_ids():
            _jobs[cid] = CH.normalized(cid, 1)
            if CH.has_form2(cid):
                _jobs[cid + "_form2"] = CH.normalized(cid, 2)
            for vname, (vraw, vform) in CH.variants(cid).items():
                _jobs[cid + "_" + vname] = S.normalize(S.form2_spec(vraw) if vform == 2 else vraw, cid)
        for cid in CH.MANUAL:
            _jobs[cid] = CH.manual(cid)
    return _jobs


def spec_of(name):
    return copy.deepcopy(job_table()[name])


def expand(ids, exact=False):
    """Base ids -> look names (an id plus its `_form2` when it exists). `exact` keeps the names as given."""
    table = job_table()
    out = []
    for i in ids:
        if i not in table:
            raise SystemExit("unknown look name %r (known: %s ...)" % (i, ", ".join(sorted(table)[:8])))
        for n in ([i] if exact else [i, i + "_form2"]):
            if n in table and n not in out:
                out.append(n)
    return out


def read_looks():
    """The look table assets/nusmp/playermodel/looks.json as an ordered dict (looks only)."""
    return json.load(open(LOOKS_JSON))["looks"]


def read_fragments(frag_dir=None):
    d = frag_dir or FRAG_DIR
    out = {}
    if os.path.isdir(d):
        for f in sorted(os.listdir(d)):
            if f.endswith(".json"):
                j = json.load(open(os.path.join(d, f)))
                out[j["id"]] = j
    return out


def existing_entry(name, frag_dir=None, looks=None):
    """The look entry of a name as it is now: its fragment when it has one, else the entry of looks.json. Scale, holds and extras are never changed by this pass, so they are read from here."""
    fr = read_fragments(frag_dir).get(name)
    if fr:
        return copy.deepcopy(fr["look"])
    return copy.deepcopy((looks or read_looks())[name])


def entry_for(existing, slim, has_glow, hair, hair_rgb):
    """The look entry in the key order of gen_skins.look_entry: skin, slim, scale, holds, glow, hair, hair_color, extras. Skin, scale, holds and extras are kept from `existing`."""
    e = {}
    for k, v in existing.items():
        if k in ("glow", "hair", "hair_color", "extras"):
            continue
        e[k] = slim if k == "slim" else copy.deepcopy(v)
    if has_glow:
        e["glow"] = True
    if hair and hair != "bald":
        e["hair"] = hair
        e["hair_color"] = "#%02x%02x%02x" % tuple(int(x) for x in hair_rgb)
    if "extras" in existing:
        e["extras"] = copy.deepcopy(existing["extras"])
    return e


def write_fragment(name, look, source, size, hair_from, frag_dir=None):
    d = frag_dir or FRAG_DIR
    os.makedirs(d, exist_ok=True)
    frag = {"id": name, "source": source, "size": size, "hair_reason": hair_from, "look": look}
    path = os.path.join(d, name + ".json")
    with open(path, "w") as f:
        json.dump(frag, f, indent=1)
        f.write("\n")
    return path


# ================================================================================================ the 64x64 blueprint
@contextlib.contextmanager
def hair_override(spec, style):
    """While active, the 64x64 generator paints the scalp for kit hair `style` instead of its classic choice for this sheet (the 3D hair is drawn by the game; the skin only carries the scalp)."""
    if not style:
        yield
        return
    key = spec["hair"]["style"]
    had = key in CH.HAIR_MAP
    old = CH.HAIR_MAP.get(key)
    CH.HAIR_MAP[key] = style
    try:
        yield
    finally:
        if had:
            CH.HAIR_MAP[key] = old
        else:
            CH.HAIR_MAP.pop(key, None)


def blueprint(name, spec, masks=None, hair=None):
    """-> (skin64 PIL, glow64 PIL or None, hair style id, hair rgb, slim): what tools/playermodel/gen_skins.py paints for this look (identical to the shipped 64x64 PNG when `hair` is None), with the
    scalp painted for kit hair `hair`. The hair id of the result is `hair` (or the classic one when None)."""
    with hair_override(spec, hair):
        return GS.paint_character(spec, name, masks if masks is not None else hair_masks())


def classic_hair(name, spec):
    """The hair style the 64x64 generator chooses for this sheet."""
    return CH.HAIR_MAP.get(spec["hair"]["style"], "short")


def chosen_hair(name, spec):
    """The hair of the HD baseline for this look: the table of hair_map.py, or the classic style when the id is not in it."""
    return HM.style_for(name, classic_hair(name, spec))


# ================================================================================================ the baseline: the generic bridge with materials from the sheet
def _is_gold_or_steel(rgb):
    import colorsys
    h, s, v = colorsys.rgb_to_hsv(*[x / 255.0 for x in rgb[:3]])
    gold = 32 <= h * 360 <= 62 and s >= 0.38 and v >= 0.72
    steel = s <= 0.22 and v >= 0.62
    return gold or steel


def materials_for(spec, hair_rgb, part):
    """Names the materials of the baseline from the character's sheet: skin tones everywhere, the hair colour on the head, armour plates, their trim and a golden or steel buckle as metal. Matchers are
    the exact tones of the sheet's colours (`near`, never a whole hue family: a brown coat must not turn into brass); everything else is cloth."""
    mats = {}
    tones = list(C.skin_palette(spec["skin"]))
    mats["skin"] = [H.near(t, 0.075) for t in tones]
    if part == "head":
        mats["hair"] = [H.near(t, 0.08) for t in C.hair_shades(hair_rgb, 4)]
        return mats
    metal = []
    armor = spec.get("armor")
    if armor and armor.get("pieces"):
        if armor.get("color"):
            metal += [H.near(t, 0.07) for t in C.shades(C.rgb(armor["color"]), 5, 1.25)]
        if armor.get("trim"):
            metal.append(H.near(C.rgb(armor["trim"]), 0.06))
    buckle = (spec["outfit"].get("belt") or {}).get("buckle")
    if buckle and _is_gold_or_steel(C.rgb(buckle)):
        metal.append(H.near(C.rgb(buckle), 0.06))
    if metal:
        mats["metal"] = metal
    return mats


def bridge(cv, img64, glow64, spec, hair_rgb=None, skip=None, soft=0.7, outline=0.55, detail=1.0, shadow=0.22, face_soft=0.25, face_outline=0.0):
    """The baseline: fills the canvas from the 64x64 blueprint with the generic HD bridge (smooth upscale, cloth gradients, outlines, metal bevel, the second layer's shadow) using materials named
    from the sheet. The face (the front of the head, both layers) is bridged with a lighter blur and outline (`face_soft`, `face_outline`) so its few pixels of eyes, brows and mouth stay crisp.
    `skip(part, face, overlay)` leaves an island untouched (draw it yourself); the glow map of the blueprint is carried over for every island."""
    hair_rgb = hair_rgb or (60, 40, 30)
    heads = materials_for(spec, hair_rgb, "head")
    rest = materials_for(spec, hair_rgb, "body")

    def user_skip(part, face, ov):
        return bool(skip and skip(part, face, ov))

    def is_face(part, face, ov):
        return part == "head" and face == "front"

    H.from64(cv, img64, glow64, heads, skip=lambda p, f, o: not is_face(p, f, o) or user_skip(p, f, o), soft=face_soft, outline=face_outline, detail=detail, shadow=0)
    H.from64(cv, img64, None, heads, skip=lambda p, f, o: p != "head" or is_face(p, f, o) or user_skip(p, f, o), soft=soft, outline=outline, detail=detail, shadow=0)
    H.from64(cv, img64, None, rest, skip=lambda p, f, o: p == "head" or user_skip(p, f, o), soft=soft, outline=outline, detail=detail, shadow=0)
    if shadow > 0 and detail > 0:
        H.cross_layer_shadow(cv, shadow)


def baseline(name, spec, masks=None, hair=None, size=256):
    """-> (skin, glow or None, hair style id, hair rgb, slim) at `size`: the 64x64 generator's result through the generic HD bridge, kit hair from hair_map.py (or `hair`)."""
    style = hair if hair is not None else chosen_hair(name, spec)
    img64, glow64, hs, hrgb, slim = blueprint(name, spec, masks, hair=style)
    cv = H.Canvas(256, slim)
    bridge(cv, img64, glow64, spec, hrgb)
    skin, glow = cv.finish()
    return to_size(skin, glow, size) + (hs, hrgb, slim)


def to_size(img, glow, size):
    """A 256 skin and glow map brought to `size` (128 = an exact 2x2 average); 256 is returned as is."""
    if img.width == size:
        return img, glow
    return H.downsample(img, size), (H.downsample_glow(glow, size) if glow is not None else None)


# ================================================================================================ hand-painted modules
def hand_module_path(name):
    return os.path.join(CHARS_DIR, name + ".py")


def load_hand(name):
    """tools/arc_hd/chars/<look name>.py's `paint` function, or None when the character has no hand-painted module."""
    path = hand_module_path(name)
    if not os.path.isfile(path):
        return None
    spec_ = importlib.util.spec_from_file_location("arc_hd_" + name, path)
    mod = importlib.util.module_from_spec(spec_)
    spec_.loader.exec_module(mod)
    return getattr(mod, "paint", None)


def lay(cv, part, name):
    """The base island and the second-layer island of a face: draw the crisp things on BOTH (the game drops the second layer and the hair beyond 24 blocks, PlayerLod)."""
    return (cv.face(part, name, False), cv.face(part, name, True))


# ================================================================================================ checks shared by the generator, the merger and the tests
def png_problems(path, glow_path=None, want_size=None):
    """Why a skin PNG (and its glow map) breaks the kit's rule (PlayerModelAudit), or []: square, a power-of-two multiple of 64 up to 1024, the glow map as big as the skin."""
    out = []
    if not os.path.isfile(path):
        return [path + " is missing"]
    w, h = Image.open(path).size
    k = w // 64
    if w != h or w < 64 or w % 64 or (k & (k - 1)) or w > 1024:
        out.append("%s is %dx%d, not a square power-of-two multiple of 64" % (os.path.basename(path), w, h))
    if want_size and w != want_size:
        out.append("%s is %d wide, the fragment says %d" % (os.path.basename(path), w, want_size))
    if glow_path and os.path.isfile(glow_path):
        gw, gh = Image.open(glow_path).size
        if (gw, gh) != (w, h):
            out.append("%s is %dx%d but its skin is %dx%d" % (os.path.basename(glow_path), gw, gh, w, h))
    return out
