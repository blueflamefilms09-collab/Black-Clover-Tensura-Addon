"""Why: turns one roster row (tools/foundry/roster_common.py) into what the game needs, using the Arc art kit (tools/arc_kit, docs/arc/art_kit.md) and nothing of its own:

  * `build_spec(row)`   the kit SPEC dict of the mob (look tokens + the three palette colours -> skin, hair, face, outfit, props, creature parts),
  * `class_of(spec)`    the ANIMATION CLASS of the mob: the clips of a kit character depend only on the build, the held props and the clip styles (plus which follow-through
                        bones exist), so 400 mobs share a few dozen animation files instead of owning 217 KB each; `canonical_spec(cls)` is the model the class's clips
                        are generated from, built with a SUPERSET of the follow-through bones (cape, hair, tail, wings, ears, hat, ribbon ...) so every member finds its bones,
  * `save_model(R, ...)` minified geometry and 8-bit palette PNGs (the kit's textures hold well under 256 colours, so this is lossless and about 2.5 times smaller),
  * `save_class_anim`   the shared animation file, without the `transform` clip (a mob never transforms).

Everything is deterministic (no randomness, no clock) so `gen_foundry.py --check` can compare a rebuild with the committed files.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)
if HERE not in sys.path:
    sys.path.insert(0, HERE)

import palette as PAL  # noqa: E402

CLIPS = "idle,walk,run,attack,cast,cast_big,hurt,death"           # the clips a mob plays (the kit's `transform` is left out)
ASSETS = os.path.join(TOOLS, "..", "src", "main", "resources", "assets", "nusmp")
KEY = "foundry"
MAGIC_PROPS = ("staff", "wand", "orb", "grimoire")
METAL_ACC = ("crown", "tiara", "circlet", "earrings", "ear_cuffs", "monocle", "chain", "pocket_watch", "glasses", "goggles")
LEFT_PROPS = ("bow", "grimoire", "shield")

_kit = None


def kit():
    """The art kit module (imported once; needs numpy and Pillow)."""
    global _kit
    if _kit is None:
        import gen_arc_kit
        _kit = gen_arc_kit
    return _kit


# ---------------------------------------------------------------------------------------------------------------- colours
def _rgb(c):
    return kit().rgb(c)


def hx(rgb):
    return "#%02x%02x%02x" % tuple(max(0, min(255, int(v))) for v in rgb)


def mixc(a, b, t):
    return hx(kit().mix(a, b, t))


def dark(c, t):
    return hx(kit().darken(c, t))


def light(c, t):
    return hx(kit().lighten(c, t))


def _parts(v):
    """'kind/a/b' -> ['kind', 'a', 'b']; a '-' or empty part is None (take the default)."""
    return [None if p in ("", "-") else p for p in v.split("/")]


def _opt(parts, i):
    return parts[i] if i < len(parts) else None


# ---------------------------------------------------------------------------------------------------------------- spec
def build_spec(r):
    """The raw kit SPEC of a parsed roster row."""
    t = r["tokens"]
    p0, p1, p2 = (PAL.hexof(n) for n in r["pal"])
    glow = PAL.hexof(t["glow"]) if t.get("glow") else light(p2, 0.3)
    tier = r["tier"]
    S = {"id": r["id"], "name": r["name"], "role": "boss", "build": r["build"], "skin": PAL.skin_or_color(t.get("skin", "light"))}

    # --- face and hair
    fp = _parts(t.get("face", "calm"))
    face = {"expression": fp[0] or "calm", "eye_color": PAL.hexof(fp[1]) if _opt(fp, 1) else glow}
    if _opt(fp, 2):
        face["marks"] = fp[2].split("+")
    for key in ("eyes", "brows", "mouth", "nose"):
        if t.get(key):
            face[key] = t[key]
    if "lashes" in t:
        face["lashes"] = True
    if t.get("mask"):
        mp = _parts(t["mask"])
        face["mask"] = {"kind": mp[0], "color": PAL.hexof(mp[1]) if _opt(mp, 1) else light(p1, 0.55), "color2": dark(p1, 0.6)}
    if "eglow" in t or tier >= 4:
        face["eye_glow"] = glow
    S["face"] = face
    hp_ = _parts(t.get("hair", "short"))
    hair = {"style": hp_[0] or "short", "color": PAL.hexof(hp_[1]) if _opt(hp_, 1) else dark(p1, 0.35)}
    if _opt(hp_, 2):
        hair["streak"] = PAL.hexof(hp_[2])
    S["hair"] = hair

    # --- outfit
    tp = _parts(t.get("top", "tunic"))
    top = {"kind": tp[0] or "tunic", "color": p0, "trim": p2}
    if _opt(tp, 1):
        top["length"] = tp[1]
    if _opt(tp, 2):
        top["material"] = tp[2]
    if _opt(tp, 3):
        top["pattern"] = tp[3]
        top["pattern_color"] = tuple(kit().mix(p0, p2, 0.45))
    if _opt(tp, 4):
        top["emblem"] = {"kind": tp[4], "color": p2, "where": "chest", "size": 7, "glow": glow if "tglow" in t else None}
    if t.get("collar"):
        top["collar"] = t["collar"]
        top["collar_color"] = mixc(p1, p0, 0.4)
    if "ragged" in t:
        top["ragged"] = True
    if "hemtrim" in t:
        top["hem_trim"] = True
    outfit = {"top": top}
    outfit["inner"] = {"kind": t.get("inner"), "color": mixc(p1, p0, 0.5)} if t.get("inner") else {"color": mixc(p1, p0, 0.5)}
    outfit["sleeves"] = {"kind": t.get("sleeves", "auto"), "cuff": p2}
    lg = _parts(t.get("legs", "trousers"))
    outfit["legs"] = {"kind": lg[0] or "trousers", "color": mixc(p1, "#101014", 0.35)}
    if _opt(lg, 1):
        outfit["legs"]["color"] = PAL.hexof(lg[1])
    bt = _parts(t.get("boots", "boots"))
    outfit["boots"] = {"kind": bt[0] or "boots", "color": dark(p1, 0.55), "height": _opt(bt, 1) or "calf", "cuff": p2 if "bootcuff" in t else None}
    gl = t.get("gloves", "none")
    outfit["gloves"] = {"kind": gl, "color": mixc(p1, "#18181c", 0.3)}
    if t.get("belt"):
        bp = t["belt"].split("+")
        outfit["belt"] = {"kind": bp[0], "color": dark(p1, 0.45), "buckle": p2, "pouches": "pouches" in bp[1:], "books": "books" in bp[1:]}
    if t.get("neck"):
        outfit["neck"] = {"kind": t["neck"], "color": mixc(p2, p1, 0.3), "glow": "nglow" in t}
    if t.get("bandage"):
        outfit["bandages"] = {"parts": t["bandage"].split("+"), "color": mixc("#d8d0b4", p1, 0.25)}
    S["outfit"] = outfit

    # --- cloak, armour
    if t.get("cape"):
        cp = t["cape"].split("/")
        S["cape"] = {"kind": cp[0], "color": p1, "trim": p2, "clasp": p2, "ragged": "ragged" in cp[1:], "hood": "hood" in cp[1:], "lining": dark(p1, 0.5)}
        if "long" in cp[1:]:
            S["cape"]["length"] = 1.0
    if t.get("armor"):
        ap = _parts(t["armor"])
        S["armor"] = {"pieces": ap[0].split("+"), "style": _opt(ap, 1) or "plate", "color": mixc(p1, "#9aa4b4", 0.55) if (_opt(ap, 1) in (None, "plate", "chain", "scale")) else mixc(p1, "#d8d0b4", 0.4),
                      "trim": p2, "glow": glow if "aglow" in t else None}

    # --- creature parts
    if t.get("wings"):
        wp = _parts(t["wings"])
        S["wings"] = {"kind": wp[0], "color": PAL.hexof(wp[1]) if _opt(wp, 1) else mixc(p1, p0, 0.3), "color2": p2,
                      "glow": glow if ("wglow" in t or wp[0] in ("angel", "fairy", "mechanical")) else None}
        if "folded" in wp[2:]:
            S["wings"]["folded"] = True
    if t.get("tail"):
        tl = _parts(t["tail"])
        S["tail"] = {"kind": tl[0], "color": PAL.hexof(tl[1]) if _opt(tl, 1) else mixc(p1, p0, 0.5), "color2": p2, "glow": glow if "tglow" in t else None}
    if t.get("horns"):
        hn = _parts(t["horns"])
        S["horns"] = {"kind": hn[0], "color": PAL.hexof(hn[1]) if _opt(hn, 1) else mixc(p2, "#d4cbb0", 0.55), "color2": p2, "glow": glow if "hglow" in t else None,
                      "size": float(_opt(hn, 2) or 1.0)}
    if t.get("ears"):
        er = _parts(t["ears"])
        S["ears"] = {"kind": er[0]}
        if _opt(er, 1):
            S["ears"]["color"] = PAL.hexof(er[1])
    if t.get("halo"):
        ho = _parts(t["halo"])
        S["halo"] = {"kind": ho[0], "color": PAL.hexof(ho[1]) if _opt(ho, 1) else light(p2, 0.2), "glow": glow}
    if t.get("acc"):
        accs = []
        for a in t["acc"].split("+"):
            accs.append({"kind": a, "color": p2 if a in METAL_ACC else mixc(p1, p0, 0.35), "color2": p2})
        S["accessories"] = accs

    # --- props: the kit's own rule decides the hand of a prop (a bow, grimoire or shield is held on the left)
    props = {}
    steel = mixc("#d8dce4", p2, 0.3)
    for key in ("prop", "off", "back"):
        if not t.get(key):
            continue
        kind = t[key]
        slot = "back" if key == "back" else ("left" if (key == "off" or kind in LEFT_PROPS) else "right")
        if slot in props:
            raise ValueError("%s: two props want the %s slot" % (r["name"], slot))
        spec = {"kind": kind, "color": steel if kind not in ("shield", "bow", "grimoire") else mixc(p1, p0, 0.3), "color2": p1 if kind not in ("shield",) else p2}
        if kind in MAGIC_PROPS or "pglow" in t:
            spec["glow"] = glow
        props[slot] = spec
    S["props"] = props
    S["glow"] = {"color": glow, "eyes": ("eglow" in t or tier >= 4), "props": False, "trim": False}
    clips = {}
    for key, name in (("atk", "attack"), ("cast", "cast"), ("idle", "idle")):
        if t.get(key):
            clips[name] = t[key]
    if clips:
        S["clips"] = clips
    return S


# ---------------------------------------------------------------------------------------------------------------- animation classes
def class_of(S):
    """The animation class of a NORMALISED spec: (build, right prop, left prop, attack, cast, idle) as an id string like `average__sword-shield__auto-auto-auto` (dashes inside a group, because a clip style may hold an underscore)."""
    pr = S["props"]
    right = pr.get("right", {}).get("kind", "none")
    left = pr.get("left", {}).get("kind", "none")
    c = S["clips"]
    return "%s__%s-%s__%s-%s-%s" % (S["build"], right, left, c["attack"], c["cast"], c["idle"])


def canonical_spec(cls):
    """The raw spec the clips of a class are generated from: the class's build, props and clip styles, dressed with every part that has a follow-through bone."""
    build, props, styles = cls.split("__")
    right, left = props.split("-")
    atk, cast, idle = styles.split("-")
    pr = {}
    if right != "none":
        pr["right"] = {"kind": right}
    if left != "none":
        pr["left"] = {"kind": left}
    clips = {}
    for k, v in (("attack", atk), ("cast", cast), ("idle", idle)):
        if v != "auto":
            clips[k] = v
    return {"id": "class_" + cls.replace("__", "_").replace("-", "_"), "role": "boss", "build": build, "props": pr, "clips": clips,
            "hair": {"style": "long"}, "cape": {"kind": "cloak"}, "wings": "bat", "tail": "devil", "ears": "elf", "halo": "ring",
            "outfit": {"top": {"kind": "coat"}, "belt": {"kind": "sash"}},
            "accessories": ["witch_hat", "ribbon", "tassel", "pocket_watch"]}


def _canonical_extra(k, rig):
    """The two bones a hair style gives only some characters: the twin tails. Added empty, so the class's clips animate them."""
    d = rig.d
    T = d.head_y + 8.0
    for name, sx in (("hair_tail_l", 5.0), ("hair_tail_r", -5.0)):
        if not rig.has(name):
            rig.bone(name, "head", (sx, T - 2.0, 0.5))


# ---------------------------------------------------------------------------------------------------------------- building
def with_fast(value, fn):
    """Runs fn with the kit's ARC_FAST clip filter set (the kit reads it at build time)."""
    old = os.environ.get("ARC_FAST")
    os.environ["ARC_FAST"] = value
    try:
        return fn()
    finally:
        if old is None:
            os.environ.pop("ARC_FAST", None)
        else:
            os.environ["ARC_FAST"] = old


def build_model(spec):
    """(Rig, normalised spec) of a mob with NO clips (the clips come from its animation class). Geometry only: about half a second."""
    k = kit()
    S = k.normalize(dict(spec), spec["id"])
    R = with_fast("none", lambda: k.build_character(dict(spec), extra=None, kit=k, file_id=spec["id"]))
    return R, S


def build_class_anim(cls):
    """The Anim (clips dict) of an animation class, from its canonical character. Slow (a couple of seconds)."""
    k = kit()
    spec = canonical_spec(cls)
    R = with_fast(CLIPS, lambda: k.build_character(dict(spec), extra=_canonical_extra, kit=k, file_id=spec["id"]))
    return R


def build_own_clips(spec):
    """The mob's OWN clips (what it would have if it did not share): used by --measure to check that sharing is faithful."""
    k = kit()
    return with_fast(CLIPS, lambda: k.build_character(dict(spec), extra=None, kit=k, file_id=spec["id"]))


# ---------------------------------------------------------------------------------------------------------------- writing
def _png(img, path):
    """Saves an RGBA image as an 8-bit palette PNG with per-entry alpha (lossless when it has at most 256 colours; otherwise the plain RGBA is written)."""
    import numpy as np
    from PIL import Image
    a = np.asarray(img.convert("RGBA")).copy()
    flat = a.reshape(-1, 4)
    flat[flat[:, 3] == 0] = 0
    uniq, inv = np.unique(flat, axis=0, return_inverse=True)
    if len(uniq) > 256:
        img.save(path, optimize=True)
        return "rgba"
    pim = Image.new("P", (a.shape[1], a.shape[0]))
    pal = []
    for c in uniq:
        pal.extend(int(v) for v in c[:3])
    pim.putpalette(pal + [0] * (768 - len(pal)))
    pim.putdata([int(i) for i in inv.reshape(-1)])
    pim.save(path, transparency=bytes(int(c[3]) for c in uniq), optimize=True)
    return "palette"


def save_model(R, mid, assets=ASSETS):
    """Writes foundry_<mid>.geo.json (minified) and the two palette PNGs. Returns the sizes in bytes."""
    R.m.key = KEY
    R.m.name = mid
    img, glow = R.finish()
    img, glow, hh = R.cropped(img, glow)
    R.m.tex_h = hh
    os.makedirs(os.path.join(assets, "geo", "entity"), exist_ok=True)
    os.makedirs(os.path.join(assets, "textures", "entity"), exist_ok=True)
    gp = os.path.join(assets, "geo", "entity", "foundry_%s.geo.json" % mid)
    with open(gp, "w") as f:
        json.dump(R.m.geo(), f, separators=(",", ":"))
    tp = os.path.join(assets, "textures", "entity", "foundry_%s.png" % mid)
    gwp = os.path.join(assets, "textures", "entity", "foundry_%s_glow.png" % mid)
    _png(img, tp)
    _png(glow, gwp)
    return {"geo": os.path.getsize(gp), "tex": os.path.getsize(tp), "glow": os.path.getsize(gwp), "texture": [R.pk.W, hh], "cubes": R.m.cube_count()}


def _round(o, nd=1):
    """Rotations and positions to one decimal (a tenth of a degree or of a model pixel is invisible; the kit writes three): the jar shrinks by half."""
    if isinstance(o, float):
        r = round(o, nd)
        return 0.0 if r == 0 else r
    if isinstance(o, dict):
        return {k: _round(v, nd) for k, v in o.items()}
    if isinstance(o, (list, tuple)):
        return [_round(v, nd) for v in o]
    return o


def _thin_track(track, tol):
    """Ramer-Douglas-Peucker over the keys of one track {"0.00": [x, y, z], ...}: a key that a straight line between its neighbours misses by less than `tol` goes (the kit
    thins at 0.12 degrees; a mob shown at game size does not need that, and the jar shrinks by about 45 percent). The first and last key always stay, so a loop still closes."""
    ks = sorted((float(t), v) for t, v in track.items())
    n = len(ks)
    if n < 3:
        return track
    keep = {0, n - 1}
    stack = [(0, n - 1)]
    while stack:
        a, b = stack.pop()
        if b - a < 2:
            continue
        t0, v0 = ks[a]
        t1, v1 = ks[b]
        best, bi = -1.0, -1
        for i in range(a + 1, b):
            u = (ks[i][0] - t0) / (t1 - t0) if t1 > t0 else 0.0
            err = max(abs(ks[i][1][k] - (v0[k] * (1 - u) + v1[k] * u)) for k in range(3))
            if err > best:
                best, bi = err, i
        if best > tol:
            keep.add(bi)
            stack += [(a, bi), (bi, b)]
    return {"%.2f" % ks[i][0]: ks[i][1] for i in sorted(keep)}


def thin(clips, rot=0.5, pos=0.15, scale=0.01):
    """The clips with every track thinned (see _thin_track)."""
    out = {}
    for cn, c in clips.items():
        bones = {}
        for b, tr in c["bones"].items():
            bones[b] = {kind: _thin_track(v, {"rotation": rot, "position": pos, "scale": scale}[kind]) for kind, v in tr.items()}
        out[cn] = dict(c, bones=bones)
    return out


def save_class_anim(R, cls, assets=ASSETS):
    clips = R.A.clips
    data = {"format_version": "1.8.0", "animations": thin({k: _round(v) for k, v in clips.items() if k != "transform"})}
    os.makedirs(os.path.join(assets, "animations", "entity"), exist_ok=True)
    p = os.path.join(assets, "animations", "entity", "foundry_anim_%s.animation.json" % cls)
    with open(p, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    return os.path.getsize(p)


def clip_deviation(own, shared, bones):
    """The largest difference (degrees) between the rotation of a bone in the mob's own clips and in the shared class clips, over the bones the mob has. Both are
    {clip: {"bones": {bone: {"rotation": {"0.00": [x, y, z], ...}}}}}; keys are sampled on a common 0.05 s grid with linear interpolation."""
    worst = 0.0
    where = ""
    for cn, c in own.items():
        sc = shared.get(cn)
        if not sc:
            continue
        for b in bones:
            a = c["bones"].get(b, {}).get("rotation")
            s = sc["bones"].get(b, {}).get("rotation")
            if not a and not s:
                continue
            n = int(c["animation_length"] / 0.05) + 1
            for i in range(n):
                t = min(c["animation_length"], i * 0.05)
                d = max(abs(x - y) for x, y in zip(_sample(a, t), _sample(s, t)))
                if d > worst:
                    worst, where = d, "%s/%s@%.2f" % (cn, b, t)
    return worst, where


def _sample(track, t):
    if not track:
        return (0.0, 0.0, 0.0)
    ks = sorted((float(k), v) for k, v in track.items())
    if t <= ks[0][0]:
        return tuple(ks[0][1])
    for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
        if t0 <= t <= t1:
            u = (t - t0) / max(1e-9, t1 - t0)
            return tuple(x + (y - x) * u for x, y in zip(v0, v1))
    return tuple(ks[-1][1])
