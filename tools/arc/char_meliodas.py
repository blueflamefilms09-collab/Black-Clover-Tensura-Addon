"""Why: Meliodas, the Dragon's Sin of Wrath and captain of the Seven Deadly Sins, the deepest of the three bosses of the first Sins group and the one whose fight
is a lesson in not feeding a boss your magic (Full Counter). The look follows what the sources agree on (docs/arc/meliodas.md lists them): a short, boyish man with
messy blond hair and emerald eyes, a dragon mark on the upper left arm, a plain dark working outfit and the short sword Lostvayne. The `form2` is the Demon King: the
Demon Mark has spread over him, the eyes blaze, the sword burns with Hellblaze. What is added for the game and NOT from a source: the exact colours of the clothes, the
black lock in the hair of the second form (the sources I could read do not say what his hair does in Assault Mode), the small horns, the crimson glow of the blade.
"""
SPEC = {
    "id": "meliodas",
    "name": "Meliodas",
    "role": "boss",
    "build": "average",
    "height": 1.84,                      # he is short (the sources say about five feet): below the build's own 2.0 blocks
    "skin": "fair",
    "face": {"expression": "grin", "eyes": "sharp", "eye_color": "#38a86c", "marks": []},
    "hair": {"style": "short", "color": "#e8c860"},
    "outfit": {
        "top": {"kind": "vest", "color": "#26201f", "trim": "#8a6a3a", "collar": "stand", "open": True},
        "inner": {"kind": "shirt", "color": "#cfc4ac"},
        "sleeves": {"kind": "short", "color": "#cfc4ac"},
        "legs": {"kind": "trousers", "color": "#26222a"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "calf"},
        "gloves": {"kind": "wraps", "color": "#cfc4ac", "side": "right"},
        "belt": {"kind": "double", "color": "#3c281c", "buckle": "#c8a040"},
    },
    "props": {"right": {"kind": "sword", "color": "#3e4350", "color2": "#c8a040", "glow": "#a01a30", "length": 0.9}},
    "glow": {"color": "#d41e3c", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "slash", "cast": "magic", "idle": "proud", "extra": []},
    "form2": {
        "name": "Meliodas, the Demon King",
        "hair": {"color": "#e8c860", "streak": "#15121a"},
        "face": {"eye_color": "#ff3a5a", "eye_glow": "#d41e3c", "eyes": "slit", "brows": "angry", "mouth": "grin", "marks": ["forehead_sigil", "cracks", "veins"]},
        "outfit": {
            "top": {"color": "#18141a", "trim": "#7a1432"},
            "legs": {"color": "#18141a"},
            "belt": {"color": "#26181c", "buckle": "#7a1432"},
        },
        "horns": {"kind": "small", "color": "#2a1c22", "glow": "#d41e3c", "size": 0.9},
        "glow": {"color": "#d41e3c", "eyes": True, "props": True, "trim": True},
    },
}
