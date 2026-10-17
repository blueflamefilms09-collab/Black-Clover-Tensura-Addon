"""Why: Gowther, the Goat's Sin of Lust of The Seven Deadly Sins, a doll with the power of Invasion (minds, memories and illusions): a boss of the Seven Deadly Sins saga (see
docs/arc/gowther.md for the fight). Look (summaries of database and fan pages, the doc lists the URLs; nothing here is copied from a source): a pale, androgynous doll with
dark pink hair and amber eyes, calm to the point of being empty, who wears a coat and carries Herritt, a pair of energy bows. Unverified (the pages could not be opened, only
searched): the colour and cut of the coat (the sources only say he once wore a heavy suit of armour made by Merlin), the cravat, the blush. Added for the game: the glow in
pink (the colour of his Invasion), and the second form (the doll's body opened: mechanical wings and a broken halo, the cracks of a body that was never made to feel).
"""
SPEC = {
    "id": "gowther",
    "name": "Gowther",
    "role": "boss",
    "build": "slim",
    "height": 1.85,
    "skin": "pale",
    "face": {"expression": "calm", "eyes": "round", "eye_color": "#e0a030", "eye_glow": "#ffcf70", "brows": "thin", "mouth": "neutral", "lashes": True, "marks": ["blush"]},
    "hair": {"style": "curtains", "color": "#b4457f", "streak": "#e68ac0"},
    "outfit": {
        "top": {"kind": "coat", "color": "#2f2a66", "trim": "#e68ac0", "length": "knee", "collar": "high", "buttons": True,
                "emblem": {"kind": "crest", "where": "chest", "size": 5, "color": "#e68ac0", "glow": "#ff8ad0"}},
        "inner": {"kind": "shirt", "color": "#f4eef8"},
        "sleeves": {"kind": "long", "cuff": "#e68ac0"},
        "legs": {"kind": "trousers", "color": "#1e1b44"},
        "boots": {"kind": "boots", "color": "#15122a", "height": "calf", "cuff": "#e68ac0"},
        "gloves": {"kind": "gloves", "color": "#f4eef8"},
        "belt": {"kind": "double", "color": "#1e1b44", "buckle": "#e68ac0"},
        "neck": {"kind": "cravat", "color": "#e68ac0"},
    },
    "props": {"left": {"kind": "bow", "color": "#f4d0ea", "color2": "#e68ac0", "glow": "#ff8ad0"},
              "back": {"kind": "bow", "color": "#f4d0ea", "color2": "#e68ac0", "glow": "#ff8ad0"}},
    "glow": {"color": "#ff8ad0", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "shoot", "cast": "magic", "idle": "relaxed", "extra": []},
    "form2": {
        "name": "Gowther, the Doll Unbound",
        "hair": {"style": "long_wavy", "color": "#f0b8d8", "streak": "#fff0fa"},
        "face": {"eye_color": "#ffd070", "eye_glow": "#ffffff", "marks": ["blush", "cracks", "light_lines"]},
        "outfit": {"top": {"color": "#4a448c", "trim": "#ffd0ec"}, "sleeves": {"cuff": "#ffd0ec"}, "neck": {"color": "#ffd0ec"}},
        "wings": {"kind": "mechanical", "color": "#8a7aa8", "color2": "#e68ac0", "glow": "#ff8ad0", "span": 1.1},
        "halo": {"kind": "broken", "color": "#e68ac0", "glow": "#ff8ad0", "radius": 1.0, "behind": True},
        "glow": {"color": "#ffb0e4", "eyes": True, "props": True, "trim": True},
    },
}
