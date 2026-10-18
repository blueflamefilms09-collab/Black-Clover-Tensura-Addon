"""Why: Undine, the water Spirit Lord of Black Clover (entities nusmp:elemental_spirit_lord kind UNDINE and nusmp:spirit_lord kind UNDINE). The old model was a woman of water: foam hair and a wave
dress that spill into waves, fin ears (tools/gen_spirit_lords.py, with a second translucent water model). This is the art kit's version on the player's rig: a slim woman with sea-blue
skin, long wavy foam-white hair, fin ears, a layered dress of rippled blue cloth with a frothed hem, a flowing mantle, and a ball of water held in her hand. It hovers. The renderer
draws her slightly translucent (the old water overlay's job). The colours are the old model's. See docs/arc/remodel.md.
"""
ENTITIES = []

SPEC = {
    "id": "spirit_undine",
    "name": "Undine",
    "role": "boss",
    "build": "slim",
    "height": 2.0,
    "skin": ["#3a78b8", "#58a0e0", "#88c8ff", "#c0e8ff"],
    "face": {"expression": "calm", "eyes": "big", "eye_color": "#40d8ff", "eye_glow": "#80f0ff", "brows": "arched", "mouth": "smile", "nose": "small", "lashes": True,
             "marks": ["eyeshadow"]},
    "hair": {"style": "long_wavy", "color": "#d8f4ff", "streak": "#58c8f0"},
    "outfit": {
        "top": {"kind": "dress", "color": "#3a9ae8", "trim": "#e8faff", "length": "floor", "collar": "none", "hem_trim": True, "ragged": True, "pattern": "stripes",
                "pattern_color": (150, 220, 255), "emblem": {"kind": "moon", "where": "chest", "size": 6, "glow": "#80f0ff"}},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "leggings", "color": "#58a0e0"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "sash", "color": "#e8faff", "buckle": "#80f0ff"},
        "neck": {"kind": "necklace", "color": "#e8faff", "glow": True},
    },
    "cape": {"kind": "mantle", "color": "#8ad0ff", "trim": "#e8faff", "length": 0.9, "ragged": True, "lining": "#d8f4ff"},
    "ears": {"kind": "fin", "color": "#58c8f0", "inner": "#e8faff", "size": 1.2},
    "props": {"right": {"kind": "orb", "color": "#58b8ff", "glow": "#80f0ff"}},
    "glow": {"color": "#80f0ff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "hover"},
}
