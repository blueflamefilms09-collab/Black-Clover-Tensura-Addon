"""Why: Mars, the Diamond Kingdom's weapon of the Dungeon Exploration Arc: an orphan with mage stones set into his body who fights with Crystal Magic. The model has to read as a
young soldier whose body is part crystal, and the three mage stones the fight is built on (docs/arc/mars.md, the StoneLedger) are visible on him: one in the forehead (a face mark), one
on the chest and one on the left forearm, each glowing. Look: the search could not confirm his hair, eyes or clothes (only "a Shining General of the Diamond Kingdom with mage stones in
his body"), so everything is the mod's own: pale skin, short spiky hair of the colour of crystal, cold eyes, a dark indigo uniform with the diamond of his kingdom on the back (the chest is left to his stone), lavender
crystal armour (pauldrons, bracers, greaves: the Nemean Armor), a crystal sword (the Harpe) and a crystal that orbits him. The second form is the Gates: the stones burn with Fana's
fire (the canon says the fire of Fana was implanted in him), so the crystal turns warm and a ring of flames stands behind his head.
"""
SPEC = {
    "id": "mars",
    "name": "Mars",
    "role": "boss",
    "build": "average",
    "height": 1.95,
    "skin": "pale",
    "face": {"expression": "cold", "eyes": "narrow", "eye_color": "#a48ce8", "eye_glow": "#c8b8ff", "brows": "flat", "mouth": "line", "marks": [{"kind": "forehead_gem", "color": "#d8ccff", "glow": "#a48ce8"}, {"kind": "cracks", "color": "#6a5a9a", "glow": "#b8a4ff"}]},
    "hair": {"style": "spiky", "color": "#d8d4ea", "streak": "#a48ce8"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#2a2a46", "trim": "#a48ce8", "collar": "stand", "length": "hip",
                "emblem": {"kind": "diamond", "where": "back", "size": 8, "color": "#e6f4ff", "glow": "#c8b8ff"}},
        "sleeves": {"kind": "long", "cuff": "#a48ce8"},
        "legs": {"kind": "trousers", "color": "#1e1e32"},
        "boots": {"kind": "boots", "color": "#26263a", "height": "calf"},
        "gloves": {"kind": "gloves", "color": "#2a2a40"},
        "belt": {"kind": "plain", "color": "#34344c", "buckle": "#a48ce8"},
    },
    "armor": {"pieces": ["pauldrons", "bracers", "greaves"], "style": "plate", "color": "#b8a8e8", "trim": "#e6f4ff", "glow": "#a48ce8"},
    "props": {
        "right": {"kind": "sword", "color": "#cfc4f0", "color2": "#a48ce8", "glow": "#a48ce8"},
        "float": {"kind": "orb", "color": "#d8ccff", "glow": "#a48ce8"},
    },
    "glow": {"color": "#a48ce8", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "slash", "cast": "magic", "idle": "proud"},
    "form2": {
        "name": "Mars, Stones Aflame",
        "hair": {"color": "#fff0d8", "streak": "#ff9a40"},
        "face": {"eye_color": "#ffb060", "eye_glow": "#ffd080",
                 "marks": [{"kind": "forehead_gem", "color": "#ffe0b0", "glow": "#ff9a40"}, {"kind": "cracks", "color": "#8a4a20", "glow": "#ffb060"}]},
        "outfit": {"top": {"trim": "#ff9a40", "emblem": {"kind": "diamond", "where": "back", "size": 8, "color": "#fff0d8", "glow": "#ffb060"}}, "sleeves": {"cuff": "#ff9a40"},
                   "belt": {"buckle": "#ff9a40"}},
        "armor": {"color": "#f0d8b8", "trim": "#ffd080", "glow": "#ff9a40"},
        "props": {"right": {"color": "#ffe8c8", "color2": "#ff9a40", "glow": "#ff9a40"}, "float": {"color": "#ffe0b0", "glow": "#ff9a40"}},
        "halo": {"kind": "flames", "color": "#ff9a40", "glow": "#ffd080", "radius": 0.9},
        "glow": {"color": "#ff9a40"},
    },
    "notes": "Crystal Magic boss of the Dungeon Exploration Arc. Three mage stones glow on him (forehead, chest, left forearm): the StoneLedger of the fight.",
}


def extra(kit, rig):
    """The two stones that are not on the face: one on the chest, over the breastbone, and one on the back of the left forearm, both glowing (orange in the second form)."""
    d = rig.d
    hot = getattr(rig, "form", 1) == 2                       # the Gates: the stones burn with Fana's fire instead of glowing cold
    gem = kit.metal(kit.shades("#ffd080" if hot else "#c8b8ff", 5, 1.3), 61, glow=(255, 176, 96, 235) if hot else (200, 184, 255, 230), emboss=False)
    rig.box("body", (-1.6, d.sh_y - 6.5, -d.td / 2 - 1.5), (3.2, 3.2, 1.2), gem)
    rig.box("forearm_l", (d.arm_x - 1.0, d.wrist_y + 2.4, -d.ad / 2 - 0.9), (2.0, 2.2, 0.9), gem)
