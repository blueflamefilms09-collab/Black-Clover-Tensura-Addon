"""Why: which characters the player-model kit draws, and the sheet of each (skin tone, face, hair, outfit, armour, glow ...). The arcs' art kit already holds a full written sheet per character
(tools/arc/char_<id>.py, a SPEC dict in the vocabulary of tools/arc_kit/spec.py), so the kit reads those instead of describing every character a second time: this module loads them, drops the
creatures that are not human-shaped, resolves the second form, and adds the few sheets of humanoids that never had an arc model (the hub's quest giver and merchant). The skin painter
(skin_lib.py) and the extras extractor (extract_extras.py) both work from what this returns.
"""
import copy
import glob
import importlib.util
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
for p in (TOOLS, os.path.join(TOOLS, "arc")):
    if p not in sys.path:
        sys.path.insert(0, p)

from arc_kit import spec as S  # noqa: E402

# arc models that are creatures, not people: they stay on their own geo models (docs/playermodel.md lists them as not converted)
NOT_HUMANOID = {"amphisbaena", "basilisk", "black_goliath", "blood_beast", "charybdis_spawn", "cotton_cloud", "cotton_sheep", "cotton_sheep_cook", "dungeon_direwolf", "dungeon_slime",
                "goliath", "grimoire_daemon", "grimoire_daemon_arch", "grimoire_daemon_greater", "grimoire_daemon_stone", "minotaur", "painted_beast", "painted_giant", "rouge",
                "spirit_black_devil", "spirit_gnome", "spirit_salamander", "spirit_sylph", "spirit_undine",
                # the dungeon's armoured bodies belong to the labyrinth builder
                "living_armor", "floor_warden"}

# arc hair style -> kit hair style
HAIR_MAP = {"bald": "bald", "crop": "short", "short": "short", "spiky": "spiky", "spiky_back": "spiky", "messy": "messy", "slick": "slick", "side_part": "swept", "bob": "bob",
            "long": "long", "long_wavy": "long", "ponytail": "ponytail", "high_ponytail": "high_ponytail", "twin_tails": "twin_tails", "twin_buns": "bun", "bun": "bun",
            "topknot": "bun", "braid": "braid", "twin_braids": "braid", "mohawk": "mohawk", "afro": "mane", "hood": "hood", "undercut": "slick", "pompadour": "pompadour",
            "wild_mane": "mane", "bangs_long": "hime", "curtains": "curtains", "shaggy": "messy", "drills": "drills", "spiky_long": "spiky_long", "samurai": "high_ponytail",
            "wizard": "long"}

_raw = {}


def raw_specs():
    """Every SPEC of tools/arc/char_*.py by character id (the dark disciple and devil avatar, which are written as functions of a devil, with their first devil)."""
    if _raw:
        return _raw
    for f in sorted(glob.glob(os.path.join(TOOLS, "arc", "char_*.py"))):
        name = os.path.basename(f)[:-3]
        spec = importlib.util.spec_from_file_location("pm_" + name, f)
        m = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(m)
        s = getattr(m, "SPEC", None)
        if s is None and hasattr(m, "spec"):
            tint = getattr(m, "LUCIFERO", None) or getattr(m, "GREY", None)
            s = m.spec(tint)
        if s is not None:
            _raw[s["id"]] = copy.deepcopy(s)
            variants = getattr(m, "VARIANTS", None)
            if variants and isinstance(variants, dict):
                out = {}
                for k, v in variants.items():
                    fn, form = (v if isinstance(v, tuple) else (v, 1))
                    out[k] = (fn() if callable(fn) else fn, form)
                _raw[s["id"]]["_variants"] = out
    return _raw


def humanoid_ids():
    return [i for i in raw_specs() if i not in NOT_HUMANOID]


def normalized(cid, form=1):
    """The normalized spec of a character's first or second form."""
    raw = raw_specs()[cid]
    if form == 2:
        if not raw.get("form2"):
            return None
        return S.normalize(S.form2_spec({k: v for k, v in raw.items() if not k.startswith("_")}), cid)
    base = {k: v for k, v in raw.items() if not k.startswith("_")}
    return S.normalize(base, cid)


def has_form2(cid):
    return bool(raw_specs()[cid].get("form2"))


def extras_models():
    """The arc model names whose extras are carried over: every humanoid and its second form."""
    out = []
    for i in humanoid_ids():
        out.append(i)
        if has_form2(i):
            out.append(i + "_form2")
    return out


def variants(cid):
    """Texture-only variants of a character ({variant: raw spec}), e.g. the Dark Disciple's four devils."""
    return raw_specs()[cid].get("_variants") or {}


# the humanoids that never had an arc model: the hub's two people, as a minimal sheet in the same vocabulary
MANUAL = {
    "quest_giver_npc": {
        "id": "quest_giver_npc", "name": "Quest Giver", "role": "npc", "build": "average", "height": 1.95, "skin": "light",
        "face": {"expression": "calm", "eyes": "round", "eye_color": "#4a6aa8", "brows": "arched", "mouth": "smile", "marks": []},
        "hair": {"style": "short", "color": "#6a4a30"},
        "outfit": {"top": {"kind": "coat", "color": "#7a2c34", "trim": "#d8b04a", "length": "knee", "collar": "stand", "hem_trim": True, "buttons": True},
                   "inner": {"kind": "shirt", "color": "#e8e0cc"}, "sleeves": {"kind": "long", "cuff": "#d8b04a"}, "legs": {"kind": "trousers", "color": "#2c2830"},
                   "boots": {"kind": "boots", "color": "#2a2220", "height": "calf"}, "belt": {"kind": "plain", "color": "#3c281c", "buckle": "#d8b04a"}},
        "glow": {"color": None},
    },
    "merchant_npc": {
        "id": "merchant_npc", "name": "Merchant", "role": "npc", "build": "average", "height": 1.95, "skin": "tan",
        "face": {"expression": "calm", "eyes": "narrow", "eye_color": "#6a4a2c", "brows": "thick", "mouth": "grin", "marks": ["moustache"]},
        "hair": {"style": "short", "color": "#2a2018"},
        "outfit": {"top": {"kind": "tunic", "color": "#3c6a4a", "trim": "#d8c070", "length": "thigh", "collar": "none", "hem_trim": True},
                   "inner": {"kind": "shirt", "color": "#e8e0cc"}, "sleeves": {"kind": "long", "cuff": "#d8c070"}, "legs": {"kind": "trousers", "color": "#5a4630"},
                   "boots": {"kind": "boots", "color": "#3a2a20", "height": "calf"}, "belt": {"kind": "double", "color": "#3c281c", "buckle": "#d8c070", "pouches": True}},
        "glow": {"color": None},
    },
}


def manual(cid):
    if cid not in MANUAL:
        return None
    return S.normalize(copy.deepcopy(MANUAL[cid]), cid)


if __name__ == "__main__":
    h = humanoid_ids()
    print(len(h), "humanoids:", ", ".join(h))
    print("extras models:", len(extras_models()))
