"""Spirit Dive, Djinn-equip style: the palettes (the materials every plate is painted with) and the specs (the style of every piece) of the four elements at both tiers.

    element   tier S (standard: every mage)         tier C (character: Yuno / Fuegoleon / Noelle)
    wind      green plates, gold trim, white-green glow veins (Sylph; Yuno)
    fire      crimson plates, gold trim, ember glow veins (Salamander; Fuegoleon)
    water     pearl-blue plates, silver trim, wave engravings (Undine; Noelle)
    earth     stone plates, moss-gold trim, amber crystal veins (Gnome; no character: tier S only)

The materials are tools/erza_armour_paint.py Mats (the painter of the Djinn-quality armour pieces). A palette has the nine slots of tools/gen_erza_armour_sets.py (main alt trim hot gem cloth leather glass
feather). Tier S uses the same hues with a quieter glow and a plainer surface (no second inlay line, less damask); tier C is the ornate set. `PAL[(element, tier)]` and `SPEC[(element, tier)]` are what the
generator (tools/gen_spirit_dive_djinn.py) reads. Run python as `python3 -B`.
"""
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_erza_armour_sets import pal  # noqa: E402

ELEMENTS = ("wind", "fire", "water", "earth")
TIERS = ("s", "c")

# (kind, base, dark, light, glow, glow amount, accent, options)
GOLD = ("gold", "E0B040", "5A3A06", "FFEEA0", "FFD070", 0.35, None, dict(wear=0.25))
SILVER = ("gold", "C8D4E8", "4A5670", "FFFFFF", "C8ECFF", 0.35, None, dict(wear=0.25))
MOSSGOLD = ("gold", "B49A3C", "3A3010", "F0E090", "C8E070", 0.35, None, dict(wear=0.4))


def _quiet(entry, k=0.6, drop_inlay=True):
    """The standard tier's version of a material: a quieter glow, one inlay line at most."""
    kind, base, dark, light, glow, amount, accent, opts = entry
    o = dict(opts)
    if drop_inlay and o.get("inlay", 0) > 1:
        o["inlay"] = 1
    return (kind, base, dark, light, glow, round(amount * k, 3), accent, o)


def _pal(seed, c, quiet):
    names = ("main", "alt", "trim", "hot", "gem", "cloth", "leather", "glass", "feather")
    rows = [c[n] for n in names]
    if quiet:
        rows = [_quiet(r) for r in rows]
    return pal(seed, *rows)


_WIND = dict(
    main=("plate", "3A9A5E", "0A3220", "B0F0C8", "A8FFD8", 0.8, "E6C25A", dict(trim=1, grooves=1, inlay=2)),
    alt=("plate", "E4F4E8", "5A8A72", "FFFFFF", "A8FFD8", 0.55, "E6C25A", dict(trim=1, grooves=1, wear=0.25)),
    trim=GOLD,
    hot=("steel", "E8FFF0", "6AA088", "FFFFFF", "B8FFE0", 1.0, "E6C25A", dict(wear=0.2, rivets=0)),
    gem=("gem", "70F0B0", "1A7A4A", "E0FFF0", "A0FFD0", 1.0, None, {}),
    cloth=("cloth", "2E7A4E", "0A2A1A", "78D0A0", "A8FFD8", 0.5, "E6C25A", dict(trim=1, wear=0.0, shine=0.15)),
    leather=("leather", "23483A", "08180F", "5AA888", None, 0.0, None, dict(wear=0.0, shine=0.25)),
    glass=("membrane", "B8F4D8", "2A9068", "FFFFFF", "B8FFE0", 0.9, None, dict(alpha=200, ribs=4)),
    feather=("feather", "B4F4D0", "2A9068", "F4FFF8", "A8FFD8", 0.9, "E6C25A", {}),
)
_FIRE = dict(
    main=("plate", "B01E2A", "300408", "F0604A", "FF7A22", 0.8, "E0B040", dict(trim=1, grooves=1, inlay=2)),
    alt=("plate", "761018", "260408", "C83A32", "FF6A1E", 0.6, "E0B040", dict(trim=1, grooves=1)),
    trim=GOLD,
    hot=("flame", "FF7C1E", "B3200C", "FFE276", "FF9830", 0.95, None, dict(grain=0.04)),
    gem=("gem", "FF5A1E", "8A1204", "FFD090", "FF8A30", 1.0, None, {}),
    cloth=("cloth", "7A1018", "300408", "D04030", "FF7A22", 0.5, "E0B040", dict(trim=1, wear=0.0, shine=0.15)),
    leather=("leather", "2A2024", "0A0608", "6A5058", None, 0.0, None, dict(wear=0.0, shine=0.25)),
    glass=("flame", "FF9A30", "D0300C", "FFE680", "FFB040", 1.0, None, dict(grain=0.04)),
    feather=("feather", "C02A20", "50080A", "FF8A40", "FF7A22", 0.7, "E0B040", {}),
)
_WATER = dict(
    main=("plate", "7CB8E4", "1A4272", "E8F8FF", "A8E4FF", 0.8, "E6EEF8", dict(trim=1, grooves=1, inlay=2)),
    alt=("plate", "E6F2F8", "6A8A9C", "FFFFFF", "A8E4FF", 0.5, "E6EEF8", dict(trim=1, grooves=1, wear=0.25)),
    trim=SILVER,
    hot=("steel", "E8FCFF", "6A8AA0", "FFFFFF", "B8F0FF", 1.0, "E6EEF8", dict(wear=0.2, rivets=0)),
    gem=("gem", "70C8FF", "1A4A9A", "D8F0FF", "90E0FF", 1.0, None, {}),
    cloth=("cloth", "2E6EA8", "0A2A4A", "7ABCE8", "A8E4FF", 0.5, "E6EEF8", dict(trim=1, wear=0.0, shine=0.15)),
    leather=("leather", "26305A", "0A1030", "5A6AA8", None, 0.0, None, dict(wear=0.0, shine=0.25)),
    glass=("membrane", "9ADAF8", "1470A0", "F0FCFF", "A8E8FF", 0.9, None, dict(alpha=205, ribs=5)),
    feather=("membrane", "B0E4FA", "1470A0", "FFFFFF", "A8E8FF", 0.9, None, dict(alpha=205, ribs=5)),
)
_EARTH = dict(
    main=("steel", "7C7668", "22201A", "C0B8A2", "FFB040", 0.55, "8CA848", dict(wear=0.5, inlay=2, trim=1, grooves=1)),
    alt=("plate", "5E6C48", "1A2012", "9CB078", "FFB040", 0.4, "B49A3C", dict(trim=1, grooves=1, wear=0.5, inlay=1)),
    trim=MOSSGOLD,
    hot=("plate", "4A4438", "12100C", "8A806A", "FFB040", 0.9, None, dict(inlay=2, wear=0.4)),
    gem=("gem", "FFB040", "8A4A08", "FFE8A8", "FFC860", 1.0, None, {}),
    cloth=("cloth", "4E6034", "161E0C", "8CA860", "C8E070", 0.3, "B49A3C", dict(trim=1, wear=0.0, shine=0.1)),
    leather=("leather", "4A3220", "1A0E06", "9A7248", None, 0.0, None, dict(wear=0.0, shine=0.25)),
    glass=("crystal", "FFC868", "A86A10", "FFF4C8", "FFD070", 0.95, None, dict(alpha=200, cells=5)),
    feather=("steel", "7C7668", "22201A", "C0B8A2", "FFB040", 0.5, None, dict(wear=0.5)),
)
_BASE = {"wind": _WIND, "fire": _FIRE, "water": _WATER, "earth": _EARTH}
_SEED = {"wind": 400, "fire": 420, "water": 440, "earth": 460}

PAL = {}
for _e in ELEMENTS:
    PAL[(_e, "c")] = _pal(_SEED[_e], _BASE[_e], False)
    PAL[(_e, "s")] = _pal(_SEED[_e] + 10, _BASE[_e], True)

# the style names of tools/gen_erza_armour_parts.py / _limbs.py, per piece; "extras" are this kit's own pieces (tools/sdj_parts.py)
SPEC = {
    ("wind", "c"): dict(helm="circlet", chest=dict(style="breast", emblem="leaf", back_emblem="wing"), skirt="short", pauldron="wing", vambrace="finned", gauntlet="plain", cuisse="plain",
                        greave="winged", sabaton="plain", helm_emblem="leaf", pauldron_emblem="leaf"),
    ("wind", "s"): dict(helm="circlet", chest=dict(style="breast", emblem="leaf", back_emblem="leaf"), skirt="short", pauldron="wing", vambrace="plain", gauntlet="plain", cuisse="plain",
                        greave="winged", sabaton="plain", helm_emblem="leaf", pauldron_emblem="leaf"),
    ("fire", "c"): dict(helm="crest", chest=dict(style="breast", emblem="sun", back_emblem="flame"), skirt="short", pauldron="spiked", vambrace="finned", gauntlet="claw", cuisse="spiked",
                        greave="spiked", sabaton="pointed", helm_emblem="flame", pauldron_emblem="flame"),
    ("fire", "s"): dict(helm="visor", chest=dict(style="breast", emblem="flame", back_emblem="flame"), skirt="short", pauldron="spiked", vambrace="plain", gauntlet="claw", cuisse="plain",
                        greave="spiked", sabaton="pointed", helm_emblem="flame", pauldron_emblem="flame"),
    ("water", "c"): dict(helm="crown", chest=dict(style="breast", emblem="wave", back_emblem="drop"), skirt="long", pauldron="fin", vambrace="finned", gauntlet="plain", cuisse="plain",
                         greave="winged", sabaton="plain", helm_emblem="drop", pauldron_emblem="wave"),
    ("water", "s"): dict(helm="circlet", chest=dict(style="breast", emblem="wave", back_emblem="drop"), skirt="tassets", pauldron="fin", vambrace="plain", gauntlet="plain", cuisse="plain",
                         greave="winged", sabaton="plain", helm_emblem="drop", pauldron_emblem="wave"),
    ("earth", "s"): dict(helm="visor", chest=dict(style="heavy", emblem="diamond", back_emblem="shield"), skirt="tassets", pauldron="tiered", vambrace="ring", gauntlet="plain", cuisse="plain",
                         greave="plain", sabaton="plain", helm_emblem="diamond", pauldron_emblem="diamond"),
}
# the character tier of Earth does not exist (no canon character); the same spec keeps a tier-C build possible for a later character
SPEC[("earth", "c")] = SPEC[("earth", "s")]

# which pieces each set has (the generator builds exactly these; book/SpiritDiveDjinnPieces lists the same names and the unit test compares them)
CORE = ("helm", "chest", "backplate", "belt", "pauldron", "vambrace", "gauntlet", "greave", "sabaton")
ORNATE = ("gorget", "cuisse", "skirt")
EXTRA = {
    ("wind", "s"): ("crest_l", "crest_r"),
    ("wind", "c"): ("crest_l", "crest_r", "crest_c", "wing"),
    ("fire", "s"): ("crest",),
    ("fire", "c"): ("crest", "plume", "mantle", "wing"),
    ("water", "s"): ("crest", "skirt"),
    ("water", "c"): ("crest", "wing"),
    ("earth", "s"): ("crest", "backcrystals", "cuisse"),
    ("earth", "c"): ("crest", "backcrystals", "cuisse"),
}
