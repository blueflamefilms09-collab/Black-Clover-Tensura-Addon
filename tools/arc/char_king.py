"""Why: King, the Grizzly's Sin of Sloth of The Seven Deadly Sins, the Fairy King Harlequin who guards the Sacred Tree of the Fairy King's Forest and fights from the air with the
Spirit Spear Chastiefol: a boss of the Seven Deadly Sins saga (see docs/arc/king.md for the fight). Look (summaries of database pages, the doc lists the URLs; nothing here is
copied from a source): a fairy with the face and the build of a young boy (the listings say orange or brown hair and brownish-orange eyes), a sleepy face (the sin of sloth),
fairy wings, and a spear that is longer than he is. Unverified (the pages could not be opened, only searched): the cut and the colours of his clothes, the circlet, the flower.
Added for the game: the tree on the tunic and the cloak (the Sacred Tree), the glow in leaf green (the colour of his Disaster), and the second form (awake: brighter wings,
a halo of light, a gold glint in the eyes).
"""
SPEC = {
    "id": "king",
    "name": "King",
    "role": "boss",
    "build": "small",
    "height": 1.6,
    "skin": "fair",
    "face": {"expression": "tired", "eye_color": "#c98a3a", "eye_glow": "#ffcf70", "marks": ["eye_bags"]},
    "hair": {"style": "messy", "color": "#c9742c"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#e8e0a8", "trim": "#4fa04a", "length": "thigh", "collar": "stand",
                "emblem": {"kind": "tree", "where": "chest", "size": 6, "color": "#4fa04a", "glow": "#b8ff80"}},
        "sleeves": {"kind": "short"},
        "legs": {"kind": "shorts", "color": "#3e7a3a"},
        "boots": {"kind": "shoes", "color": "#6a4a2a"},
        "belt": {"kind": "sash", "color": "#4fa04a", "buckle": "#e8d070", "sash": "#e8d070"},
    },
    "cape": {"kind": "mantle", "color": "#9ad07a", "trim": "#e8d070", "lining": "#f4f0c0", "length": 0.55, "clasp": "#e8d070",
             "emblem": {"kind": "tree", "size": 8, "color": "#4fa04a", "glow": "#b8ff80"}},
    "accessories": [{"kind": "circlet", "color": "#e8d070"}, {"kind": "flower", "color": "#ff9ab8"}],
    "wings": {"kind": "fairy", "color": "#8fd860", "color2": "#f4ffb0", "glow": "#d8ff9a", "span": 1.2},
    "props": {"right": {"kind": "spear", "color": "#f0e2a0", "color2": "#4fa04a", "glow": "#d8ff9a", "length": 1.1}},
    "glow": {"color": "#b8ff80", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "thrust", "cast": "magic", "idle": "hover", "extra": []},
    "form2": {
        "name": "King, the Fairy King Harlequin",
        "face": {"expression": "stern", "eye_color": "#ffcf5a", "eye_glow": "#fff0a0", "marks": ["light_lines"]},
        "hair": {"color": "#f0a040", "streak": "#fff0a0"},
        "outfit": {"top": {"color": "#fff4c0", "trim": "#e8d070"}},
        "cape": {"color": "#d8f4a8", "lining": "#ffffff"},
        "wings": {"kind": "fairy", "color": "#e8ffc0", "color2": "#ffffff", "glow": "#e8ffa0", "span": 1.5},
        "halo": {"kind": "ring", "color": "#d8ff9a", "glow": "#f0ffd0", "radius": 1.0, "behind": True},
        "glow": {"color": "#d8ffa0", "eyes": True, "props": True, "trim": True},
    },
}
