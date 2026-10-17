"""Why: Merlin, the Boar's Sin of Gluttony of The Seven Deadly Sins, the greatest mage of Britannia: a boss of the Seven Deadly Sins saga (see
docs/arc/merlin.md for the fight). Look (summaries of fan and database pages, see the doc for the URLs; nothing here is copied from a source): a woman with short black hair,
yellow eyes, make-up and a faint smile, in a dark purple dress with a fur collar, detached bell sleeves, long thigh boots and a cloak; her Sacred Treasure Aldan, a crystal
orb, turns over her palm. Unverified (the pages could not be opened, only searched): the exact colours of the dress, the cloak's emblem, the mole. Added for the game: the
gold trim and the violet streak in the hair (the colours of her spells), and the gear halo of the second form (Infinity, the power that stops the time of her spells).
"""
SPEC = {
    "id": "merlin",
    "name": "Merlin",
    "role": "boss",
    "build": "slim",
    "height": 1.9,
    "skin": "pale",
    "face": {"expression": "calm", "eyes": "sharp", "eye_color": "#e8c040", "eye_glow": "#ffd86b", "brows": "thin", "mouth": "smile", "lip_color": "#a8203c",
             "lashes": True, "marks": ["eyeshadow", "lipstick"]},
    "hair": {"style": "bob", "color": "#17131f", "streak": "#7a5cff"},
    "outfit": {
        "top": {"kind": "dress", "color": "#4a3ca8", "trim": "#d8b24a", "length": "thigh", "collar": "fur", "collar_color": "#f0eaf8",
                "emblem": {"kind": "star", "where": "chest", "size": 5, "color": "#d8b24a", "glow": "#ffd86b"}},
        "inner": {"kind": "none"},
        "sleeves": {"kind": "bell", "color": "#33288a", "cuff": "#d8b24a"},
        "legs": {"kind": "leggings", "color": "#17131f"},
        "boots": {"kind": "boots", "color": "#241a38", "height": "knee", "cuff": "#d8b24a"},
        "gloves": {"kind": "gloves", "color": "#17131f"},
        "belt": {"kind": "sash", "color": "#d8b24a", "buckle": "#fff0b0", "sash": "#7a5cff"},
        "neck": {"kind": "choker", "color": "#d8b24a", "gem": "#7a5cff", "glow": True},
    },
    "cape": {"kind": "cloak", "color": "#2a1a5a", "trim": "#d8b24a", "lining": "#7a60e0", "length": 0.85, "clasp": "#d8b24a",
             "emblem": {"kind": "crest", "size": 9, "color": "#d8b24a", "glow": "#ffd86b"}},
    "accessories": [{"kind": "earrings", "color": "#d8b24a"}],
    "props": {"right": {"kind": "orb", "color": "#fff3c0", "color2": "#d8b24a", "glow": "#ffd86b"}},
    "glow": {"color": "#ffd86b", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "proud", "extra": []},
    "form2": {
        "name": "Merlin, Infinity Unbound",
        "hair": {"streak": "#cdbcff"},
        "face": {"eye_color": "#fff0b0", "eye_glow": "#ffffff"},
        "outfit": {"top": {"color": "#6a54d8", "trim": "#fff0b0"}, "sleeves": {"color": "#4a3cb8", "cuff": "#fff0b0"}, "belt": {"sash": "#cdbcff"}},
        "cape": {"color": "#3a2a80", "lining": "#a090ff", "trim": "#fff0b0"},
        "halo": {"kind": "gears", "color": "#d8b24a", "glow": "#ffd86b", "radius": 1.1, "behind": True},
        "glow": {"color": "#ffe9a0", "eyes": True, "props": True, "trim": True},
    },
}
