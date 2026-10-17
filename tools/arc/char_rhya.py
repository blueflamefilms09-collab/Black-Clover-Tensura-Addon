"""Why: Rhya, the Disloyal of the Third Eye: the elf in a human body whose Copy Magic takes the spells of every grimoire he touches. The boss that carries the Imitation fight of the first
saga and, with Fana (and Vetto, another builder's), the Third Eye as a trio. Look (from the Black Clover wiki and search summaries, URLs in docs/arc/rhya.md): a tall young man (181 cm) with messy
black hair marked by patches of white, black eyes, a red tattoo on the left cheek, and a third eye hidden under his hair in the middle of the forehead. Once the elf in him wakes: white hair
with black patches, gold eyes, pointed ears, the tattoo reaching further, the third eye open (the second form below). The wiki gives no outfit: he wears the light hooded robe of the Eye of
the Midnight Sun over a dark shirt (an addition), and the book in his hand is the borrowed grimoire (an addition).
"""
SPEC = {
    "id": "rhya",
    "name": "Rhya",
    "role": "boss",
    "build": "slim",
    "height": 2.0,
    "skin": "light",
    "face": {"expression": "smirk", "eyes": "sharp", "eye_color": "#14141c", "brows": "thin", "mouth": "smirk", "marks": [{"kind": "scar_cheek_l", "color": "#c02030"}]},
    "hair": {"style": "messy", "color": "#14141c", "streak": "#e8e8f0"},
    "outfit": {
        "top": {"kind": "coat", "color": "#d8d4c8", "trim": "#2a2a34", "length": "knee", "collar": "stand"},
        "inner": {"kind": "shirt", "color": "#2a2a34"},
        "sleeves": {"kind": "long", "cuff": "#2a2a34"},
        "legs": {"kind": "trousers", "color": "#2a2a34"},
        "boots": {"kind": "boots", "color": "#1c1c22", "height": "calf"},
        "belt": {"kind": "double", "color": "#3c3040", "buckle": "#b8b8d0"},
    },
    "cape": {"kind": "hooded_cloak", "color": "#e0dccf", "trim": "#2a2a34", "length": 0.8, "width": 0.9, "lining": "#9a96a8"},
    "props": {"left": {"kind": "grimoire", "color": "#4a4a62", "color2": "#c8c8e0", "glow": "#b8a0ff"}},
    "glow": {"color": "#b8a0ff", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "book", "idle": "relaxed"},
    "form2": {
        "name": "Rhya, the Open Eye",
        "face": {"expression": "sinister", "eye_color": "#f2c048", "eye_glow": "#ffd35a", "marks": [{"kind": "scar_cheek_l", "color": "#c02030"}, "cheek_lines", {"kind": "third_eye", "color": "#ffd35a", "glow": "#ffb347"}]},
        "hair": {"style": "spiky_back", "color": "#f0f0f8", "streak": "#14141c"},
        "ears": {"kind": "elf"},
        "glow": {"color": "#ffd35a", "eyes": True, "props": True, "trim": True},
        "props": {"left": {"kind": "grimoire", "color": "#4a4a62", "color2": "#c8c8e0", "glow": "#ffd35a"}},
    },
    "notes": "The Disloyal of the Third Eye. The hidden third eye of the first form is simply covered by the hair; the open one is the Gates (100% power).",
}
