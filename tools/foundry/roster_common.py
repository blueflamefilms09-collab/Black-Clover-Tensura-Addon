"""Why: the roster of the Mob Foundry is 400 rows written by hand (tools/foundry/roster_holy.py, roster_abyssal.py), so the format has to be short enough to read at a
glance and strict enough that a typo is a loud error. A row is ONE line of eight fields separated by ` | `:

    Name | faction | build | palette | source | look tokens | archetype | abilities

    Tunneler Kobold | beast | small | rust sand iron | D&D: kobold | skin=red ears=pointed horns=small tail=lizard hair=crop/ember face=grin top=vest prop=dagger | skirmisher | dash pack_howl

This module is PURE (no art kit, no numpy): it parses the lines into dicts, and computes the *look signature* and the pairwise look distance that tools/foundry/guard.py
and the JUnit roster test use. The art kit side (turning tokens into a kit SPEC) is tools/foundry/foundry_kit.py.
"""
import re

TIERS = (1, 2, 3, 4, 5)
FACTIONS = ("undead", "goblin", "ogre", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect")
ARCHETYPES = ("brute", "skirmisher", "archer", "caster", "assassin", "tank", "support", "summoner", "flier", "charger")
BUILDS = ("slim", "average", "broad", "huge", "small", "lanky", "child")

# tokens that make up the STRUCTURE of a look (what a viewer notices first). Colours are compared separately (the palette triple, skin, hair colour, eye colour).
STRUCT_GROUPS = (
    ("build", ("@build",)),
    ("hair", ("hair.style",)),
    ("face", ("face.expr", "eyes", "brows", "mouth", "nose", "face.marks", "mask", "lashes")),
    ("top", ("top",)),
    ("legs", ("legs",)),
    ("boots", ("boots", "gloves")),
    ("trim", ("belt", "neck", "inner", "sleeves", "collar", "bandage")),
    ("cape", ("cape",)),
    ("armor", ("armor",)),
    ("wings", ("wings",)),
    ("tail", ("tail",)),
    ("horns", ("horns",)),
    ("ears", ("ears",)),
    ("halo", ("halo",)),
    ("acc", ("acc",)),
    ("props", ("prop", "off", "back")),
)


class RosterError(ValueError):
    pass


def slug(name):
    s = re.sub(r"[^a-z0-9]+", "_", name.lower().replace("'", "")).strip("_")
    if not s or not s[0].isalpha():
        raise RosterError("name %r gives no usable id" % name)
    return s


def parse_tokens(text):
    """`a=b c=d/e` -> {"a": "b", "c": "d/e"}; a bare word is a flag with the value ''. A repeated key is an error."""
    out = {}
    for w in text.split():
        k, _, v = w.partition("=")
        if k in out:
            raise RosterError("look token %r given twice" % k)
        out[k] = v
    return out


def parse_row(line, road, tier, where=""):
    """One roster line -> a dict (id, name, road, tier, faction, build, pal, source, tokens, arch, abilities, size)."""
    parts = [p.strip() for p in line.split(" | ")]
    if len(parts) != 8:
        raise RosterError("%s: a row has 8 fields separated by ' | ', got %d: %r" % (where, len(parts), line[:80]))
    name, fac, build, pal, src, look, arch, abil = parts
    tokens = parse_tokens(look)
    size = float(tokens.pop("size", "1.0"))
    pal_names = pal.split()
    if len(pal_names) != 3:
        raise RosterError("%s: %s: the palette needs 3 colour names, got %r" % (where, name, pal))
    abilities = abil.split()
    return {"id": slug(name), "name": name, "road": road, "tier": tier, "faction": fac, "build": build, "pal": tuple(pal_names), "source": src, "tokens": tokens,
            "arch": arch, "abilities": tuple(abilities), "size": size}


def parse_block(text, road, tier, where=""):
    rows = []
    for n, line in enumerate(text.strip().splitlines(), 1):
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        rows.append(parse_row(s, road, tier, "%s line %d" % (where, n)))
    return rows


def check_row(r, abilities, colors, skins):
    """The reasons a single row is unusable (empty list = fine). `abilities` is the set of ability ids, `colors` the palette names, `skins` the skin tone names."""
    bad = []
    if r["tier"] not in TIERS:
        bad.append("tier %r" % r["tier"])
    if r["faction"] not in FACTIONS:
        bad.append("faction %r is not one of %s" % (r["faction"], ", ".join(FACTIONS)))
    if r["build"] not in BUILDS:
        bad.append("build %r" % r["build"])
    if r["arch"] not in ARCHETYPES:
        bad.append("archetype %r is not one of %s" % (r["arch"], ", ".join(ARCHETYPES)))
    for c in r["pal"]:
        if c not in colors:
            bad.append("palette colour %r" % c)
    if not (2 <= len(r["abilities"]) <= 3):
        bad.append("needs 2 or 3 abilities, has %d" % len(r["abilities"]))
    if len(set(r["abilities"])) != len(r["abilities"]):
        bad.append("an ability is listed twice")
    for a in r["abilities"]:
        if a not in abilities:
            bad.append("unknown ability %r" % a)
    if not r["source"].strip():
        bad.append("empty source")
    if not (0.6 <= r["size"] <= 2.0):
        bad.append("size %r outside 0.6..2.0" % r["size"])
    skin = r["tokens"].get("skin")
    if skin and skin not in skins and skin not in colors and not skin.startswith("#"):
        bad.append("skin %r" % skin)
    return bad


# ---------------------------------------------------------------------------------------------------------------- signatures
def _tok(r, key):
    """The comparable value of one signature key of a row ('' when the look does not say)."""
    t = r["tokens"]
    if key == "@build":
        return r["build"]
    if key == "hair.style":
        return t.get("hair", "short").split("/")[0]
    if key == "face.expr":
        return t.get("face", "calm").split("/")[0]
    if key == "face.marks":
        parts = t.get("face", "").split("/")
        return "/".join(sorted(parts[2].split("+"))) if len(parts) > 2 else ""
    if key in ("top", "armor", "cape", "boots", "horns", "wings", "tail", "ears", "halo", "mask"):
        return t.get(key, "")
    if key == "acc":
        return "+".join(sorted(t.get("acc", "").split("+"))) if t.get("acc") else ""
    return t.get(key, "")


def structure(r):
    """{group: value-tuple} of a row: what the creature is made of, without any colour."""
    return {g: tuple(_tok(r, k) for k in keys) for g, keys in STRUCT_GROUPS}


def struct_distance(a, b):
    """How many structure groups differ between two rows (0 = the same silhouette and parts)."""
    sa, sb = structure(a), structure(b)
    return sum(1 for g in sa if sa[g] != sb[g])


def colour_signature(r):
    t = r["tokens"]
    hair_col = t.get("hair", "").split("/")[1] if "/" in t.get("hair", "") else ""
    eye_col = t.get("face", "").split("/")[1] if t.get("face", "").count("/") >= 1 else ""
    return (r["pal"], t.get("skin", ""), hair_col, eye_col, t.get("glow", ""))


def look_signature(r):
    """A hashable summary of the whole look (structure and colours)."""
    s = structure(r)
    return (tuple(sorted(s.items())), colour_signature(r))
