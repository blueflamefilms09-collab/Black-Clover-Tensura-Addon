"""Why: Sylph, the wind Spirit Lord of Black Clover (entities nusmp:elemental_spirit_lord kind SYLPH, nusmp:spirit_lord kind SYLPH, and nusmp:wind_spirit_lord, the Wind Spirit Lord boss whose
renderer was a stub that drew nothing). The old model was a green fairy with dragonfly wings and drifting bubbles (tools/gen_spirit_lords.py). This is the kit's version on the player's
rig: a small, light, green-skinned sprite with big bright eyes, twin tails of pale green hair that stream in the wind, a leaf-cut dress, a pair of dragonfly (fairy) wings and a little
wand that throws the wind blade. She hovers. The wind boss wears the same model drawn larger by its hitbox (client/remodel). The colours are the old model's. See docs/arc/remodel.md.
"""
ENTITIES = ["wind_spirit_lord"]

SPEC = {
    "id": "spirit_sylph",
    "name": "Sylph",
    "role": "boss",
    "build": "small",
    "height": 1.4,
    "skin": ["#58b878", "#80d898", "#a8f0b8", "#d8ffe0"],
    "face": {"expression": "cheerful", "eyes": "big", "eye_color": "#40e890", "eye_glow": "#90ffc0", "brows": "raised", "mouth": "smile", "nose": "small", "lashes": True,
             "marks": ["blush"]},
    "hair": {"style": "twin_tails", "color": "#a8ffc8", "streak": "#58e098"},
    "outfit": {
        "top": {"kind": "dress", "color": "#58c878", "trim": "#e8ffe8", "length": "thigh", "collar": "none", "hem_trim": True, "ragged": True, "pattern": "patches",
                "pattern_color": (130, 220, 150)},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "bare"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "sash", "color": "#e8ffe8", "buckle": "#90ffc0"},
    },
    "wings": {"kind": "fairy", "color": "#a8f0c0", "color2": "#e8fff0", "glow": "#90ffc0", "span": 1.5},
    "ears": {"kind": "elf", "size": 1.1},
    "accessories": [{"kind": "flower", "color": "#ff9ac8"}],
    "props": {"right": {"kind": "wand", "color": "#58c878", "color2": "#e8ffe8", "glow": "#90ffc0"}},
    "glow": {"color": "#90ffc0", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "hover"},
}
