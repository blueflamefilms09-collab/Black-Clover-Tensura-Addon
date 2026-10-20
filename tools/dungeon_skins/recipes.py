"""Why: the WEAR of each kind of dungeon person, as data. A recipe is a list of (pass, arguments) for tools/dungeon_skins/wear.py, run on the finished skin: what the owner asked for as "shading,
wear and emissive eyes where needed", per family. The archetypes (archetypes.py) name their recipe in the eighth field of their row; the Mob Foundry's mobs take the `f_*` recipe of their family
(foundry_class.family_of), a lighter one that leaves the art kit's own design (the garments, the masks, the armour) as it is and only weathers it. Colours are the roster's palette names or RGB.
"""

RECIPES = {
    # ---- the green and the grey
    "goblin": [("blotch", dict(color="mud", k=0.5, coverage=0.30)), ("spots", dict(color="moss", density=0.07, dark=True)), ("grime", dict(k=0.5, tint="mud")), ("scars", dict(count=2)),
               ("tears", dict(coverage=0.4)), ("edge_light", {})],
    "orc": [("grime", dict(k=0.4, tint="soot")), ("scars", dict(count=3)), ("blood", dict(coverage=0.05)), ("spots", dict(color="moss", density=0.04, dark=True)), ("tears", dict(coverage=0.25)),
            ("rivets", {}), ("edge_light", {})],
    "troll": [("blotch", dict(color="moss", k=0.5, coverage=0.30, cell=3)), ("spots", dict(color="mud", density=0.06, dark=True)), ("scars", dict(count=3)), ("moss", {}), ("grime", dict(k=0.25, tint="mud"))],
    "beastman": [("fur", dict(amount=0.14)), ("grime", dict(k=0.3, tint="umber")), ("scars", dict(count=2)), ("tears", dict(coverage=0.3)), ("edge_light", {})],
    "lizard": [("spots", dict(color="jade", density=0.14)), ("spots", dict(color="bone", density=0.05)), ("grime", dict(k=0.25, tint="mud")), ("tears", dict(coverage=0.2))],
    "merfolk": [("spots", dict(color="aqua", density=0.10)), ("sheen", dict(k=0.25)), ("scars", dict(count=2)), ("grime", dict(k=0.2, tint="deep")), ("tears", dict(coverage=0.2))],
    # ---- the dead
    "skeleton": [("ribs", {}), ("skull", dict(eye="spirit")), ("cracks", dict(color=(54, 50, 44), count=6, emit=False)), ("grime", dict(k=0.28, tint="dust")), ("tears", dict(coverage=0.35)), ("rivets", {}), ("edge_light", {})],
    "zombie": [("blotch", dict(color="swamp", k=0.55, coverage=0.35)), ("blotch", dict(color=(122, 52, 50), k=0.6, coverage=0.12, salt=9)), ("stitches", {}), ("grime", dict(k=0.45, tint="mud")),
               ("tears", dict(coverage=0.45))],
    "ghoul": [("blotch", dict(color="ash", k=0.45, coverage=0.30)), ("veins", dict(color="bile")), ("scars", dict(count=3)), ("grime", dict(k=0.4, tint="mud")), ("tears", dict(coverage=0.3))],
    "spectre": [("frost", dict(color=(190, 200, 230), k=0.35, layers=(False,))), ("veins", dict(color="spirit")), ("tears", dict(coverage=0.5))],
    "lich": [("ribs", {}), ("runes", dict(color="voidlight")), ("cracks", dict(color="voidlight", count=5)), ("grime", dict(k=0.15, tint="indigo")), ("tears", dict(coverage=0.2)), ("gild", dict(color="gold"))],
    "knight": [("grime", dict(k=0.35, tint="soot")), ("scars", dict(count=2)), ("blood", dict(coverage=0.10)), ("rivets", {}), ("edge_light", {}), ("tears", dict(coverage=0.15))],
    "mummy": [("grime", dict(k=0.4, tint="dust")), ("tears", dict(coverage=0.5)), ("blotch", dict(color="rust", k=0.35, coverage=0.2)), ("scars", dict(count=2))],
    # ---- the pit
    "imp": [("scorch", dict(k=0.45)), ("spots", dict(color="crimson", density=0.06, dark=True)), ("cracks", dict(color="ember", count=3)), ("tears", dict(coverage=0.2))],
    "demon": [("scorch", {}), ("cracks", dict(color="hellfire", count=8)), ("scars", dict(count=2)), ("rivets", {}), ("edge_light", {})],
    "cultist": [("grime", dict(k=0.3, tint="soot")), ("runes", dict(color="voidlight")), ("tears", dict(coverage=0.25)), ("blood", dict(coverage=0.08))],
    # ---- the light
    "priest": [("gild", {}), ("runes", dict(color="lumen")), ("grime", dict(k=0.1, tint="dust")), ("edge_light", {})],
    "paladin": [("gild", dict(color="gold")), ("rivets", {}), ("edge_light", {}), ("scars", dict(count=1)), ("runes", dict(color="lumen"))],
    "holy": [("gild", dict(color="gold")), ("runes", dict(color="lumen")), ("edge_light", {}), ("sheen", dict(k=0.15))],
    # ---- the people
    "peasant": [("grime", dict(k=0.25, tint="dust")), ("tears", dict(coverage=0.15)), ("scars", dict(count=1))],
    "witch": [("grime", dict(k=0.25, tint="soot")), ("spots", dict(color="moss", density=0.05, dark=True, parts=("head",))), ("tears", dict(coverage=0.2)), ("runes", dict(color="spirit"))],
    "fey": [("grime", dict(k=0.15, tint="mud")), ("moss", dict(color=(88, 124, 62))), ("scars", dict(count=1))],
    "elf": [("grime", dict(k=0.15, tint="mud")), ("scars", dict(count=1)), ("tears", dict(coverage=0.1)), ("edge_light", {})],
    "dwarf": [("grime", dict(k=0.45, tint="soot")), ("scorch", dict(color=(30, 22, 18), k=0.35)), ("scars", dict(count=2)), ("rivets", {}), ("edge_light", {})],
    # ---- the stone, the ice, the fire
    "stone": [("blotch", dict(color="granite", k=0.5, cell=3, coverage=0.35)), ("moss", {}), ("cracks", dict(color="sunfire", count=8)), ("grime", dict(k=0.3, tint="soot"))],
    "ice": [("frost", {}), ("cracks", dict(color="aether", count=6)), ("rivets", {}), ("edge_light", {})],
    "fire": [("scorch", {}), ("cracks", dict(color="sunfire", count=5)), ("sheen", dict(k=0.1))],
    # ---- the dungeon's own bodies
    "mollusk": [("sheen", dict(k=0.28)), ("blotch", dict(color=(70, 48, 90), k=0.4, coverage=0.3)), ("veins", dict(color=(216, 240, 160))), ("grime", dict(k=0.3, tint="soot")),
                ("tears", dict(coverage=0.2)), ("rivets", {}), ("edge_light", {})],
    "warden": [("blotch", dict(color=(44, 32, 64), k=0.4, coverage=0.3)), ("cracks", dict(color="voidlight", count=9)), ("runes", dict(color="voidlight")), ("scars", dict(count=3)),
               ("grime", dict(k=0.3, tint="soot")), ("rivets", {}), ("edge_light", {})],
    "colossus": [("blotch", dict(color="granite", k=0.45, cell=3, coverage=0.35)), ("moss", {}), ("cracks", dict(color=(255, 176, 48), count=12)), ("scars", dict(count=3)), ("grime", dict(k=0.3, tint="soot"))],
    "colossus_open": [("blotch", dict(color="granite", k=0.4, cell=3, coverage=0.3)), ("cracks", dict(color=(255, 208, 96), count=22)), ("veins", dict(color=(255, 170, 48))), ("scars", dict(count=3)),
                      ("grime", dict(k=0.25, tint="soot"))],
    # the soul corpses' four stages of decay: each stage is the one before it and more
    "decay_0": [("grime", dict(k=0.2, tint="dust")), ("stitches", {}), ("edge_light", {})],
    "decay_1": [("blotch", dict(color="swamp", k=0.45, coverage=0.2)), ("stitches", {}), ("grime", dict(k=0.3, tint="mud")), ("tears", dict(coverage=0.15)), ("edge_light", {})],
    "decay_2": [("blotch", dict(color="swamp", k=0.55, coverage=0.32)), ("blotch", dict(color=(122, 52, 50), k=0.6, coverage=0.14, salt=9)), ("stitches", {}), ("grime", dict(k=0.4, tint="mud")),
                ("tears", dict(coverage=0.3)), ("veins", dict(color="spirit"))],
    "decay_3": [("blotch", dict(color="swamp", k=0.6, coverage=0.4)), ("blotch", dict(color=(122, 52, 50), k=0.6, coverage=0.22, salt=9)),
                ("blotch", dict(color=(214, 204, 176), k=0.8, coverage=0.12, salt=11)), ("stitches", {}), ("grime", dict(k=0.5, tint="mud")), ("tears", dict(coverage=0.5)),
                ("veins", dict(color="spirit")), ("cracks", dict(color="spirit", count=6))],
    # ---- what the Mob Foundry's mobs take (the art kit's own design stays; these only weather it)
    "f_plain": [("grime", dict(k=0.22, tint="dust")), ("tears", dict(coverage=0.12)), ("edge_light", {})],
    "f_undead": [("grime", dict(k=0.3, tint="mud")), ("blotch", dict(color="ash", k=0.4, coverage=0.2)), ("tears", dict(coverage=0.3)), ("cracks", dict(color=(50, 46, 44), count=5, emit=False)), ("edge_light", {})],
    "f_rot": [("blotch", dict(color="swamp", k=0.5, coverage=0.3)), ("stitches", {}), ("grime", dict(k=0.4, tint="mud")), ("tears", dict(coverage=0.4))],
    "f_spectre": [("veins", dict(color="spirit")), ("tears", dict(coverage=0.4)), ("frost", dict(color=(190, 200, 230), k=0.25, layers=(False,)))],
    "f_infernal": [("scorch", dict(k=0.45)), ("cracks", dict(color="hellfire", count=3)), ("edge_light", {})],
    "f_holy": [("gild", {}), ("edge_light", {}), ("sheen", dict(k=0.12))],
    "f_construct": [("blotch", dict(color="granite", k=0.35, cell=3, coverage=0.3)), ("moss", {}), ("rivets", {}), ("edge_light", {}), ("grime", dict(k=0.3, tint="soot"))],
    "f_aquatic": [("sheen", dict(k=0.18)), ("spots", dict(color="aqua", density=0.07)), ("grime", dict(k=0.2, tint="deep"))],
    "f_fey": [("grime", dict(k=0.12, tint="mud")), ("edge_light", {})],
    "f_goblin": [("blotch", dict(color="mud", k=0.4, coverage=0.25)), ("grime", dict(k=0.45, tint="mud")), ("scars", dict(count=2)), ("tears", dict(coverage=0.35)), ("edge_light", {})],
    "f_orc": [("grime", dict(k=0.35, tint="soot")), ("scars", dict(count=2)), ("blood", dict(coverage=0.08)), ("tears", dict(coverage=0.2)), ("edge_light", {})],
}

# the foundry families of foundry_class.family_of to the recipe their mobs take
FOUNDRY_RECIPE = {"goblin": "f_goblin", "orc": "f_orc", "skeleton": "f_undead", "rot": "f_rot", "spectre": "f_spectre", "infernal": "f_infernal", "holy": "f_holy",
                  "construct": "f_construct", "aquatic": "f_aquatic", "fey": "f_fey", "": "f_plain"}


def recipe(name):
    if name not in RECIPES:
        raise KeyError("no wear recipe '%s'; known: %s" % (name, ", ".join(sorted(RECIPES))))
    return RECIPES[name]


def foundry_recipe(family):
    return RECIPES[FOUNDRY_RECIPE.get(family, "f_plain")]
