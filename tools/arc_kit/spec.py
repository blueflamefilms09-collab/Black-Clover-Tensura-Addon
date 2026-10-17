"""Why: a character spec is a plain dict that a person writes by hand, so a typo must fail loudly and helpfully (the unknown key, the list of valid ones) instead of
silently drawing the default. This module holds the DEFAULTS of every section (the one place the documentation and the code agree on them), merges a spec over
them, resolves the choices that depend on others (an expression picks eyes, brows and a mouth; a coat gets a default length), and validates every named choice
against the vocabulary of the part that draws it.
"""
import copy

from . import face as F
from . import hair as Hr
from . import outfit as O
from .rig import BUILDS

ROLES = ("npc", "boss")
ATTACK_STYLES = ("auto", "slash", "overhead", "thrust", "smash", "punch", "cast_jab", "shoot", "stab", "sweep", "none")
CAST_STYLES = ("auto", "gesture", "magic", "pray", "book")
IDLE_STYLES = ("auto", "relaxed", "guard", "proud", "slouch", "hover")
EXTRA_CLIPS = ("chest_gem_reveal", "spawn")
ACCESSORY_KINDS = ("glasses", "monocle", "eyepatch", "headband", "bandana", "cap", "beret", "wizard_hat", "witch_hat", "crown", "tiara", "circlet", "earrings",
                   "cigarette", "pipe", "pocket_watch", "shoulder_fur", "backpack", "quiver", "goggles", "mask_band", "flower", "ribbon", "veil",
                   "ear_cuffs", "tassel", "chain", "satchel")
MATERIALS = ("cloth", "heavy", "leather", "plate", "chain", "fur", "bone", "scale")
MASK_KINDS = ("half", "full", "visor", "skull", "oni", "veil", "plague", "fox")
WING_KINDS = ("bat", "feathered", "angel", "dragon", "fairy", "mechanical")
TAIL_KINDS = ("cat", "wolf", "fox", "devil", "lizard", "dragon", "rat")
HORN_KINDS = ("devil", "ram", "oni", "single", "antlers", "bull", "small")
EAR_KINDS = ("elf", "cat", "wolf", "fox", "rabbit", "pointed", "fin")
HALO_KINDS = ("ring", "gears", "broken", "thorns", "flames")
PROP_SLOTS = ("right", "left", "back", "float")
BELT_KINDS = ("plain", "double", "sash", "obi")

DEFAULTS = {
    "id": None,
    "name": None,
    "role": "npc",
    "build": "average",
    "height": None,
    "widen": 1.0,
    "skin": "light",
    "face": {"expression": None, "eyes": None, "eye_color": "#4a6aa8", "eye_glow": None, "sclera": None, "brows": None, "brow_color": None, "mouth": None,
             "lip_color": None, "nose": "small", "lashes": False, "marks": [], "eye_shift": 0, "brow_shift": 0, "mask": None},
    "hair": {"style": "short", "color": "#2a2018", "streak": None, "ribbon": None, "cover": None},
    "outfit": {
        "top": {"kind": "shirt", "color": "#4a5a7a", "trim": None, "length": None, "open": None, "collar": "none", "collar_color": None, "buttons": False,
                "pattern": "plain", "pattern_color": None, "emblem": None, "ragged": False, "fold": False, "hem_trim": False, "open_top": 7, "open_bottom": 3,
                "material": None},
        "inner": {"kind": None, "color": "#ecebea"},
        "sleeves": {"kind": "auto", "color": None, "cuff": None},
        "legs": {"kind": "trousers", "color": "#2c2c38", "chaps": "#c8b48c", "stripe": None},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "calf", "cuff": None},
        "gloves": {"kind": "none", "color": "#2a2a30", "side": "both"},
        "belt": None,
        "neck": {"kind": "none", "color": "#7a1e24", "gem": "#c83030", "glow": False},
        "bandages": None,
    },
    "cape": None,
    "armor": None,
    "accessories": [],
    "props": {},
    "wings": None,
    "tail": None,
    "halo": None,
    "horns": None,
    "ears": None,
    "glow": {"color": None, "eyes": True, "props": True, "trim": False},
    "clips": {"attack": "auto", "cast": "auto", "idle": "auto", "extra": []},
    "form2": None,
    "notes": "",
}
BELT_DEFAULT = {"kind": "plain", "color": "#3c281c", "buckle": "#c8a040", "pouches": False, "sash": None, "books": False}
CAPE_DEFAULT = {"kind": "cape", "color": "#3a2a3c", "trim": None, "length": 0.8, "width": 1.0, "emblem": None, "ragged": False, "clasp": "#c8a040", "hood": False,
                "lining": None}
ARMOR_DEFAULT = {"pieces": [], "color": "#8c98a8", "trim": "#c8a040", "style": "plate", "glow": None, "emblem": None}
ACCESSORY_DEFAULT = {"kind": None, "color": None, "color2": None, "glow": None, "side": None}
PROP_DEFAULT = {"kind": None, "color": None, "color2": None, "glow": None, "length": 1.0, "grip": None, "sheathed": False, "two_hand": None}
WING_DEFAULT = {"kind": "bat", "color": "#3a2a48", "color2": None, "glow": None, "span": 1.0, "folded": False}
TAIL_DEFAULT = {"kind": "devil", "color": "#6a2a3a", "color2": None, "glow": None, "length": 1.0}
HORN_DEFAULT = {"kind": "devil", "color": "#3a2e28", "color2": None, "glow": None, "size": 1.0}
EAR_DEFAULT = {"kind": "elf", "color": None, "inner": None, "size": 1.0}
HALO_DEFAULT = {"kind": "ring", "color": "#f2d070", "glow": "#ffe58a", "radius": 1.0, "behind": True}
FORM2_KEYS = ("name", "skin", "face", "hair", "outfit", "cape", "armor", "accessories", "props", "wings", "tail", "halo", "horns", "ears", "glow")


class SpecError(ValueError):
    pass


def _choice(path, value, valid, allow_none=False):
    if value is None and allow_none:
        return
    if value not in valid:
        raise SpecError("%s: %r is not valid; use one of: %s" % (path, value, ", ".join(str(v) for v in valid)))


def merge(default, given, path=""):
    """Recursively lays `given` over `default`; an unknown key is an error that lists the valid ones."""
    if not isinstance(given, dict):
        raise SpecError("%s: expected a dict, got %r" % (path or "spec", type(given).__name__))
    out = copy.deepcopy(default)
    for k, v in given.items():
        if k not in default:
            raise SpecError("%s: unknown key %r; valid keys: %s" % (path or "spec", k, ", ".join(default)))
        dv = default[k]
        if isinstance(dv, dict) and isinstance(v, dict) and dv:
            out[k] = merge(dv, v, path + "." + k if path else k)
        else:
            out[k] = copy.deepcopy(v)
    return out


def _section(default, given, path):
    if given is None:
        return None
    if given is True:
        return copy.deepcopy(default)
    return merge(default, given, path)


def normalize(raw, file_id=None):
    """The full spec: defaults merged, expression and lengths resolved, every named choice checked."""
    S = merge(DEFAULTS, raw)
    S["id"] = S["id"] or file_id
    if not S["id"] or not str(S["id"]).replace("_", "").isalnum() or S["id"] != S["id"].lower():
        raise SpecError("id: %r must be snake_case (letters, digits, underscore); it is the file name char_<id>.py and the model name arc_<id>" % S["id"])
    S["name"] = S["name"] or S["id"].replace("_", " ").title()
    _choice("role", S["role"], ROLES)
    _choice("build", S["build"], tuple(BUILDS))
    f = S["face"]
    ex = f["expression"]
    if ex is not None:
        _choice("face.expression", ex, tuple(F.EXPRESSIONS))
    base = F.EXPRESSIONS[ex or "calm"]
    for k in ("eyes", "brows", "mouth"):
        if f[k] is None:
            f[k] = base[k]
    _choice("face.eyes", f["eyes"], F.EYE_STYLES)
    _choice("face.brows", f["brows"], F.BROW_STYLES)
    _choice("face.mouth", f["mouth"], F.MOUTH_STYLES)
    _choice("face.nose", f["nose"], F.NOSE_STYLES)
    for i, m in enumerate(f["marks"]):
        kind = m if isinstance(m, str) else m.get("kind")
        _choice("face.marks[%d]" % i, kind, F.MARKS)
    if f["mask"] is not None:
        f["mask"] = merge({"kind": "half", "color": "#d8d0b4", "color2": "#2a2024", "glow": None, "eye_glow": None}, f["mask"] if isinstance(f["mask"], dict) else {"kind": f["mask"]}, "face.mask")
        _choice("face.mask.kind", f["mask"]["kind"], MASK_KINDS)
    _choice("hair.style", S["hair"]["style"], Hr.HAIR_STYLES)
    o = S["outfit"]
    _choice("outfit.top.kind", o["top"]["kind"], O.TOP_KINDS)
    if o["top"]["length"] is None:
        o["top"]["length"] = O.DEFAULT_LENGTH[o["top"]["kind"]]
    _choice("outfit.top.length", o["top"]["length"], tuple(O.LENGTHS))
    if o["top"]["open"] is None:
        o["top"]["open"] = O.DEFAULT_OPEN.get(o["top"]["kind"], False)
    _choice("outfit.top.collar", o["top"]["collar"], O.COLLARS)
    _choice("outfit.top.material", o["top"]["material"], MATERIALS, allow_none=True)
    _choice("outfit.top.pattern", o["top"]["pattern"], O.PATTERNS)
    if o["top"]["emblem"] is not None:
        o["top"]["emblem"] = merge({"kind": "none", "color": None, "where": "back", "size": 6, "aspect": 1.0, "y": 3, "dx": 0, "glow": None, "dark": None},
                                   o["top"]["emblem"], "outfit.top.emblem")
        _choice("outfit.top.emblem.kind", o["top"]["emblem"]["kind"], O.EMBLEMS)
        if o["top"]["emblem"]["color"] is None:
            o["top"]["emblem"]["color"] = o["top"]["trim"] or "#e8e4dc"
    if o["inner"]["kind"] is None:
        o["inner"]["kind"] = "bare" if o["top"]["kind"] in ("bare", "vest") else ("none" if o["top"]["kind"] in ("jumpsuit",) else "tank")
    _choice("outfit.inner.kind", o["inner"]["kind"], O.INNER_KINDS)
    _choice("outfit.sleeves.kind", o["sleeves"]["kind"], O.SLEEVE_KINDS)
    _choice("outfit.legs.kind", o["legs"]["kind"], O.LEG_KINDS)
    _choice("outfit.boots.kind", o["boots"]["kind"], O.BOOT_KINDS)
    _choice("outfit.boots.height", o["boots"]["height"], ("ankle", "calf", "knee"))
    _choice("outfit.gloves.kind", o["gloves"]["kind"], O.GLOVE_KINDS)
    _choice("outfit.gloves.side", o["gloves"]["side"], ("both", "left", "right"))
    _choice("outfit.neck.kind", o["neck"]["kind"], O.NECK_KINDS)
    if o["belt"] is not None:
        o["belt"] = _section(BELT_DEFAULT, o["belt"], "outfit.belt")
        _choice("outfit.belt.kind", o["belt"]["kind"], BELT_KINDS)
    if o["bandages"] is not None:
        o["bandages"] = merge({"parts": [], "color": "#d8d0b4"}, o["bandages"], "outfit.bandages")
        for p in o["bandages"]["parts"]:
            _choice("outfit.bandages.parts", p, O.BODY_PARTS)
    if S["cape"] is not None:
        S["cape"] = _section(CAPE_DEFAULT, S["cape"], "cape")
        _choice("cape.kind", S["cape"]["kind"], O.CAPE_KINDS)
    if S["armor"] is not None:
        S["armor"] = _section(ARMOR_DEFAULT, S["armor"], "armor")
        for p in S["armor"]["pieces"]:
            _choice("armor.pieces", p, O.ARMOR_PIECES)
        _choice("armor.style", S["armor"]["style"], ("plate", "chain", "leather", "bone", "scale"))
    accs = []
    for i, a in enumerate(S["accessories"]):
        a = {"kind": a} if isinstance(a, str) else a
        a = merge(ACCESSORY_DEFAULT, a, "accessories[%d]" % i)
        _choice("accessories[%d].kind" % i, a["kind"], ACCESSORY_KINDS)
        accs.append(a)
    S["accessories"] = accs
    props = {}
    from . import props as Pr
    for slot, p in S["props"].items():
        _choice("props", slot, PROP_SLOTS)
        p = {"kind": p} if isinstance(p, str) else p
        p = merge(PROP_DEFAULT, p, "props." + slot)
        _choice("props.%s.kind" % slot, p["kind"], Pr.PROP_KINDS)
        if p["sheathed"] and p["kind"] not in Pr.SHEATHABLE:
            raise SpecError("props.%s.sheathed: only %s can be worn in a scabbard" % (slot, ", ".join(Pr.SHEATHABLE)))
        props[slot] = p
    S["props"] = props
    for key, dflt, kinds in (("wings", WING_DEFAULT, WING_KINDS), ("tail", TAIL_DEFAULT, TAIL_KINDS), ("horns", HORN_DEFAULT, HORN_KINDS), ("ears", EAR_DEFAULT, EAR_KINDS),
                             ("halo", HALO_DEFAULT, HALO_KINDS)):
        if S[key] is not None:
            S[key] = _section(dflt, S[key] if not isinstance(S[key], str) else {"kind": S[key]}, key)
            _choice(key + ".kind", S[key]["kind"], kinds)
    c = S["clips"]
    _choice("clips.attack", c["attack"], ATTACK_STYLES)
    _choice("clips.cast", c["cast"], CAST_STYLES)
    _choice("clips.idle", c["idle"], IDLE_STYLES)
    for e in c["extra"]:
        _choice("clips.extra", e, EXTRA_CLIPS)
    if S["form2"] is not None:
        for k in S["form2"]:
            if k not in FORM2_KEYS:
                raise SpecError("form2: unknown key %r; a second form may change: %s" % (k, ", ".join(FORM2_KEYS)))
    _inherit_glow(S)
    return S


def _inherit_glow(S):
    """`glow.color` is the character's one glow colour: the eyes glow in it (glow.eyes), so do the props that have no glow of their own (glow.props) and the emblems of the
    outfit and the cape (glow.trim). A colour given on the part itself always wins; without glow.color nothing is inherited."""
    g = S["glow"]
    if not g["color"]:
        return
    if g["eyes"] and S["face"]["eye_glow"] is None:
        S["face"]["eye_glow"] = g["color"]
    if g["props"]:
        for p in S["props"].values():
            if p["glow"] is None:
                p["glow"] = g["color"]
    if g["trim"]:
        em = S["outfit"]["top"]["emblem"]
        if em is not None and em["glow"] is None:
            em["glow"] = g["color"]
        if S["cape"] is not None and S["cape"]["emblem"] and S["cape"]["emblem"].get("glow") is None:
            S["cape"]["emblem"] = dict(S["cape"]["emblem"], glow=g["color"])


def form2_spec(raw):
    """The raw spec of the second form: the first form's raw spec with the `form2` overrides laid over it section by section (dicts merge, anything else replaces)."""
    base = copy.deepcopy(raw)
    over = copy.deepcopy(raw.get("form2") or {})
    base.pop("form2", None)

    def lay(a, b):
        for k, v in b.items():
            if isinstance(v, dict) and isinstance(a.get(k), dict):
                lay(a[k], v)
            else:
                a[k] = v
    lay(base, over)
    base["id"] = raw["id"] if raw.get("id") else None
    return base
