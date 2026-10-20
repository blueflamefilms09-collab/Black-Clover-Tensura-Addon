"""Why: one place for the numbers Better Combat needs, shared by inventory.py (which names the profile of each weapon for the
manifest) and gen_attributes.py (which writes the files). Keeping the table out of both scripts means a balance tweak is one edit and the
manifest can never disagree with the files.

A profile is {parent, range_bonus, two_handed?, attacks?}. Since 1.46.0 CLASS_PROFILES / SPECIAL_PROFILES (bottom of the file) never set
`attacks`, so every animation is the parent preset's own. The 1.39.0 tables with our custom animations are kept as CUSTOM_CLASS_PROFILES /
CUSTOM_SPECIAL_PROFILES (gen_attributes.py --custom) and are not used by default.
"""

# Melee presets that exist in Better Combat 2.4.0 (data/bettercombat/weapon_attributes). 1.38.0 listed "pike", which does not exist; the
# authoritative list is builtin_animations.json (extract_builtin.py), this tuple is a convenience copy for the old custom tables.
BUILTIN_PARENTS = ("sword", "cutlass", "claymore", "dagger", "rapier", "katana", "axe", "heavy_axe", "double_axe", "mace", "hammer", "scythe",
                   "spear", "halberd", "glaive", "staff", "battlestaff", "trident", "fist", "claw", "wand", "pickaxe", "sickle", "lance",
                   "twin_blade", "coral_blade", "soul_knife")

HORIZ, VERT, FWD = "HORIZONTAL_PLANE", "VERTICAL_PLANE", "FORWARD_BOX"


def atk(hitbox, anim, angle, upswing, mult=1.0):
    """One attack of a combo: keys exactly as Better Combat's schema names them."""
    return {"hitbox": hitbox, "damage_multiplier": mult, "angle": angle, "upswing": upswing, "animation": "nusmp:" + anim}


# Combos shared by several classes (each is a list of attacks, played in order, then the combo starts again).
GREATSWORD_COMBO = [atk(HORIZ, "greatsword_sweep", 160, 0.55), atk(HORIZ, "greatsword_sweep_back", 160, 0.55),
                    atk(VERT, "greatsword_slam", 140, 0.70, 1.35)]
KATANA_COMBO = [atk(HORIZ, "katana_draw", 140, 0.40), atk(VERT, "katana_rise", 120, 0.45), atk(FWD, "katana_thrust", 30, 0.50, 1.15)]
RAPIER_COMBO = [atk(FWD, "rapier_lunge", 20, 0.30, 0.75), atk(FWD, "rapier_lunge", 20, 0.30, 0.75), atk(FWD, "rapier_lunge", 30, 0.45, 1.25)]
DAGGER_COMBO = [atk(FWD, "rapier_lunge", 20, 0.30, 0.8), atk(FWD, "rapier_lunge", 20, 0.30, 0.8), atk(VERT, "katana_rise", 70, 0.35, 1.1)]
AXE_COMBO = [atk(VERT, "axe_chop", 100, 0.40), atk(VERT, "axe_chop", 100, 0.40)]
BATTLEAXE_COMBO = [atk(VERT, "axe_chop", 110, 0.50), atk(VERT, "axe_chop", 110, 0.50), atk(VERT, "axe_chop", 130, 0.60, 1.25)]
GREATAXE_COMBO = [atk(VERT, "axe_chop", 120, 0.55), atk(HORIZ, "greatsword_sweep", 150, 0.55), atk(VERT, "greatsword_slam", 140, 0.70, 1.35)]
HAMMER_COMBO = [atk(HORIZ, "greatsword_sweep", 130, 0.60), atk(VERT, "greatsword_slam", 130, 0.75, 1.35)]
WHIP_COMBO = [atk(FWD, "whip_lash", 20, 0.45), atk(FWD, "whip_lash", 20, 0.45)]
DADAO_COMBO = [atk(HORIZ, "katana_draw", 150, 0.45), atk(VERT, "katana_rise", 120, 0.50), atk(VERT, "greatsword_slam", 110, 0.65, 1.3)]
TWIN_COMBO = [atk(HORIZ, "twin_cross", 120, 0.40), atk(HORIZ, "twin_cross", 140, 0.40, 1.1)]
GENESIS_COMBO = [atk(HORIZ, "greatsword_sweep", 170, 0.55), atk(HORIZ, "greatsword_sweep_back", 170, 0.55),
                 atk(VERT, "greatsword_slam", 150, 0.70, 1.35)]
ELSDOCIA_COMBO = [atk(HORIZ, "greatsword_sweep", 170, 0.55), atk(HORIZ, "greatsword_sweep_back", 170, 0.55),
                  atk(HORIZ, "greatsword_sweep", 200, 0.60, 1.1), atk(VERT, "greatsword_slam", 160, 0.75, 1.5)]


def P(parent, rng, two=None, attacks=None):
    return {"parent": parent, "range_bonus": rng, "two_handed": two, "attacks": attacks}


# 1.39.0 profiles with OUR animations (nusmp:*). Kept, unused by default (gen_attributes.py --custom writes them); see docs/better_combat.md 1.46.0.
# class -> profile. range_bonus follows the in-hand size of the 1.30-1.34 weapon-size classes (tools/relics/weapon_classes.json).
CUSTOM_CLASS_PROFILES = {
    "dagger": P("dagger", -0.25, None, DAGGER_COMBO),
    "shortsword": P("sword", 0.0),
    "longsword": P("sword", 0.25),
    "sabre": P("sword", 0.1),
    "rapier": P("rapier", 0.25, None, RAPIER_COMBO),
    "katana": P("katana", 0.35, None, KATANA_COMBO),
    "dadao": P("katana", 0.6, None, DADAO_COMBO),
    "greatsword": P("claymore", 1.0, True, GREATSWORD_COMBO),
    "handaxe": P("axe", 0.0, None, AXE_COMBO),
    "cleaver": P("axe", -0.1, None, AXE_COMBO),
    "battleaxe": P("axe", 0.75, True, BATTLEAXE_COMBO),
    "greataxe": P("double_axe", 1.25, True, GREATAXE_COMBO),
    "crescent": P("scythe", 0.75, True),
    "scythe": P("scythe", 1.0, True),
    "hammer": P("hammer", 0.5, True, HAMMER_COMBO),
    "club": P("mace", 0.25),
    "spear": P("spear", 1.0),
    "trident": P("trident", 1.0),
    "glaive": P("glaive", 1.25, True),
    "staff": P("staff", 0.75, True),
    "wand": P("wand", 0.0),
    "whip": P("sword", 1.5, None, WHIP_COMBO),
    "coilwhip": P("sword", 1.25, None, WHIP_COMBO),
    "fist": P("fist", 0.0),
}

# 1.39.0 named profiles with custom combos (kept, see CUSTOM_CLASS_PROFILES).
CUSTOM_SPECIAL_PROFILES = {
    "genesis": P("sword", 0.75, None, GENESIS_COMBO),          # the Genesis Demon-Slayer: one-handed like the Black Clover original, huge sweeps
    "elsdocia": P("claymore", 1.5, True, ELSDOCIA_COMBO),
    "twin_sabre": P("sword", 0.1, None, TWIN_COMBO),
    "twin_shortsword": P("sword", 0.0, None, TWIN_COMBO),
}

# Per-item in-hand size bonus over the class default (items clearly larger than the rest of their class).
SIZE_RANGE = {"buster_sword": 1.25, "giant_jawblade": 1.5, "thundercleave": 1.25, "stegosaurus_greatsword": 1.25,
              "soul_shatter_blade": 1.25, "null_blade": 1.0, "dream_glass": 1.0, "the_lumberjack": 1.0, "astralaria": 1.0}

# The 12 hand-made files that existed before 1.38.0 (the Rimeheart gets 0.5 added, it had none). Their parent (and range_bonus where they had one) is kept exactly; `combo` False means
# the owner chose a one-handed `sword` parent for a greatsword-class item (the Rimeheart is documented as one-handed for dual wielding),
# so no two_handed and no custom combo are added to it.
LEGACY = {
    "demon_destroyer_sword": {"parent": "sword", "range_bonus": 0.5, "combo": False},
    "demon_dweller_sword": {"parent": "sword", "range_bonus": 0.25, "combo": False},
    "demon_slasher_katana": {"parent": "sword", "range_bonus": 0.25, "combo": False},
    "demon_slayer_sword": {"parent": "sword", "range_bonus": 0.75, "combo": True},
    "licht_destroyer_sword": {"parent": "sword", "range_bonus": 0.5, "combo": False},
    "licht_dweller_sword": {"parent": "sword", "range_bonus": 0.25, "combo": False},
    "magic_tool_spear": {"parent": "spear", "combo": True},
    "magic_tool_sword": {"parent": "sword", "combo": True},
    "miasma_infused_katana": {"parent": "katana", "combo": True},
    "rimeheart_runeblade": {"parent": "sword", "range_bonus": 0.5, "combo": False},
    "severing_greatsword": {"parent": "claymore", "combo": True},
    "spell_forged_rapier": {"parent": "rapier", "combo": True},
}

# Djinn weapon kinds (relics/djinn/weapon/DjinnWeaponRules) -> class. "bow" (Sitri) is ranged and excluded by inventory.py.
DJINN_KIND_CLASS = {"lance": "spear", "greatsword": "greatsword", "trident": "trident", "glaive": "glaive", "staff": "staff",
                    "axe": "greataxe", "horn": "club", "claws": "fist", "scythe": "scythe"}
# 1.101.0: the kinds the 1.93.0 roster added (their weapons shipped no Better Combat file, so they swung with the vanilla attack); appended, nothing above changes.
DJINN_KIND_CLASS.update({"claymore": "greatsword", "harpoon": "trident", "daggers": "dagger", "rapier": "rapier", "anchor": "hammer", "fans": "dagger",
                         "pincers": "fist", "sabre": "sabre", "spear": "spear", "cleaver": "cleaver", "halberd": "glaive", "longsword": "longsword", "mace": "club"})
# Classes that are not melee at all: no profile.
NON_MELEE_CLASSES = ("handitem", "crossbow", "bow", "shield", "item")

# ======================================================================= 1.46.0: Better Combat's own presets only
# A profile here never has `attacks`: the combo (and so every animation id) is the parent preset's, which Better Combat itself ships.
# Only `range_bonus` (the weapon's real in-hand size) and `two_handed` are set. Parents are all in builtin_animations.json "presets".
# range_bonus note: Better Combat treats 0 as "not set" and keeps the parent's value, so a wanted 0 on a parent that is not 0 is written as
# NEARLY_ZERO (see gen_attributes.effective_range).
NEARLY_ZERO = 0.01

CLASS_PROFILES = {
    "dagger": P("dagger", -0.25),
    "shortsword": P("sword", 0.0),
    "longsword": P("sword", 0.25),
    "sabre": P("cutlass", 0.1),
    "rapier": P("rapier", 0.25),
    "katana": P("katana", 0.35),
    "dadao": P("katana", 0.6),
    "greatsword": P("claymore", 1.0, True),
    "handaxe": P("axe", 0.0),
    "cleaver": P("axe", -0.1),
    "battleaxe": P("heavy_axe", 0.75, True),
    "greataxe": P("double_axe", 1.25, True),
    "crescent": P("scythe", 0.75, True),
    "scythe": P("scythe", 1.0, True),
    "hammer": P("hammer", 0.5, True),
    "club": P("mace", 0.25),
    "spear": P("spear", 1.0),
    "trident": P("trident", 1.0),
    "glaive": P("glaive", 1.25, True),
    "staff": P("staff", 0.75, True),
    "wand": P("wand", 0.0),
    "whip": P("sword", 1.5),
    "coilwhip": P("sword", 1.25),
    "fist": P("claw", 0.0),
}

SPECIAL_PROFILES = {
    "genesis": P("sword", 0.75),                 # the Genesis Demon-Slayer: one-handed like the Black Clover original
    "elsdocia": P("claymore", 1.5, True),
    "twin_sabre": P("sword", 0.1),               # a pair: Better Combat dual wields two one-handed swords by itself
    "twin_shortsword": P("sword", 0.0),
}
