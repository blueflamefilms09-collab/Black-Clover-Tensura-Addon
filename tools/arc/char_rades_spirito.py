"""Why: Rades Spirito, the Soul Corpse mage cast out of the Purple Orca who set the dead on the Royal Capital: the boss that carries the Royal Capital arc of the first saga. Look (from the
Black Clover wiki and search summaries, URLs in docs/arc/rades_spirito.md): a man of average height with long, messy light-purple hair and one blue eye, dark red bandages over his forehead and
left eye with a light-green ornament on them, more bandages tight round his neck, a scar on the right cheek, a dark red sleeveless top and trousers, a glove on the left arm, a wide belt, boots
with light stripes and a tattered cape. Added for the game, not from the source: the book in his hand (his grimoire has one page) and the pale green soul glow. The second form is the Rades of
the betrayal: the soul-light burns in his eye and hair, the cape tears further and a ring of thorns of bone stands round him.
"""
SPEC = {
    "id": "rades_spirito",
    "name": "Rades Spirito",
    "role": "boss",
    "build": "average",
    "height": 1.92,
    "skin": "pale",
    "face": {"expression": "angry", "eyes": "narrow", "eye_color": "#4a7ad8", "brows": "angry", "mouth": "frown", "marks": ["scar_cheek_r", "eye_bags"]},
    "hair": {"style": "messy", "color": "#b9a2e0"},
    "outfit": {
        "top": {"kind": "tank", "color": "#6a1620"},
        "legs": {"kind": "trousers", "color": "#5a1220"},
        "boots": {"kind": "boots", "color": "#2a2024", "height": "calf", "cuff": "#c8c4b8"},
        "gloves": {"kind": "gloves", "color": "#2a2024", "side": "left"},
        "belt": {"kind": "plain", "color": "#2a1c1c", "buckle": "#a8a8b8"},
        "bandages": {"parts": ["neck", "forearm_r"], "color": "#8a1c2c"},
    },
    "cape": {"kind": "tattered", "color": "#2a1c24", "trim": "#8a1c2c", "length": 0.7, "clasp": "#a8a8b8"},
    "accessories": [{"kind": "eyepatch", "color": "#8a1c2c", "color2": "#9be8b0", "side": "left"}],
    "props": {"left": {"kind": "grimoire", "color": "#2a2e3a", "color2": "#9aa6c8", "glow": "#9be8b0"}},
    "glow": {"color": "#9be8b0", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "book", "idle": "proud"},
    "form2": {
        "name": "Rades Spirito, the Betrayed",
        "skin": "ashen",
        "face": {"expression": "mad", "eye_color": "#9be8b0", "eye_glow": "#9be8b0", "marks": ["scar_cheek_r", "eye_bags", "veins"]},
        "hair": {"color": "#d8c4ff", "style": "wild_mane"},
        "outfit": {"top": {"color": "#8a1c2c"}, "legs": {"color": "#6a1620"}},
        "cape": {"color": "#1c1018", "ragged": True},
        "halo": {"kind": "thorns", "color": "#4a5578", "glow": "#9be8b0", "radius": 1.0},
        "glow": {"color": "#9be8b0", "eyes": True, "props": True, "trim": True},
    },
    "notes": "Soul Corpse mage of the Eye of the Midnight Sun. His risen dead are vanilla zombies of the fight; the Gates (100% power) are the second form.",
}
