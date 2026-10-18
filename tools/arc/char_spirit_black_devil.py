"""Why: the Black Devil, the dark companion kind of the Spirit Lord (entity nusmp:spirit_lord kind BLACK_DEVIL: Tensura's greater daemon, a dark orb with a red rim in the orb renderer).
There is no old model of its own, so the design is new: a small, lithe devil sprite on the player's rig (the companion's hitbox is 0.6 x 1.4), black-violet skin, curled horns, short bat
wings, a pointed tail, red slit eyes, a ragged violet-black cloak and a dark orb that glows red in its hand (its anti-magic slash). Kept light enough to read in a dim hall.
See docs/arc/remodel.md. A game design, not canon.
"""
ENTITIES = []

SPEC = {
    "id": "spirit_black_devil",
    "name": "Black Devil",
    "role": "boss",
    "build": "small",
    "height": 1.4,
    "skin": ["#2c1838", "#44285a", "#5e3a78", "#7c5898"],
    "face": {"expression": "demon", "eyes": "slit", "eye_color": "#ff3040", "eye_glow": "#ff5a68", "brows": "angry", "mouth": "fangs", "nose": "none", "marks": ["cracks"]},
    "hair": {"style": "spiky", "color": "#1a1022"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#3a2250", "trim": "#c83048", "length": "hip", "collar": "stand", "ragged": True, "hem_trim": True},
        "legs": {"kind": "trousers", "color": "#2a1838"},
        "boots": {"kind": "boots", "color": "#1c1024", "height": "ankle"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "plain", "color": "#1c1024", "buckle": "#c83048"},
    },
    "cape": {"kind": "cape", "color": "#3a2250", "trim": "#c83048", "length": 0.7, "lining": "#1c1024"},
    "horns": {"kind": "devil", "color": "#1a1022", "color2": "#c83048", "glow": "#ff3040", "size": 1.0},
    "wings": {"kind": "bat", "color": "#44285a", "color2": "#7c5898", "glow": "#ff3040", "span": 1.0},
    "tail": {"kind": "devil", "color": "#2a1838", "color2": "#c83048", "glow": "#ff3040", "length": 1.3},
    "props": {"right": {"kind": "orb", "color": "#2a0a30", "glow": "#ff3040"}},
    "glow": {"color": "#ff3040", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "hover"},
}
