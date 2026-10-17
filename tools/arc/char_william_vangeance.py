"""Why: William Vangeance, captain of the Golden Dawn: the senior knight of the squad whose juniors (Yuno, Mimosa, Klaus) already stand in the Sanctuary, and the one who
arrives after the third champion. Look (summarised from search results, see docs/arc/npcb.md section 10): a lean, tall man with short silver-white hair and purple eyes whose
face is half hidden by a mask that doubles as part of his helmet (an eye-band visor here), in a long gold-and-cream captain's coat with the Golden Dawn's blue-furred shoulders and
a gold cape that carries the World Tree, his magic. Added for the game: the colours of the coat and the lining are a choice; the World Tree emblem is the mod's name for his magic.
"""
SPEC = {
    "id": "william_vangeance",
    "name": "William Vangeance",
    "role": "npc",
    "build": "slim",
    "height": 2.04,
    "skin": "fair",
    "face": {"expression": "calm", "eyes": "narrow", "eye_color": "#7a4ea8", "brows": "thin",
             "mask": {"kind": "visor", "color": "#c8a050", "color2": "#2a2024", "eye_glow": "#b58cff"}},
    "hair": {"style": "side_part", "color": "#eceef6"},
    "outfit": {
        "top": {"kind": "coat", "color": "#e4dcc0", "trim": "#c8a050", "collar": "high", "buttons": True, "length": "knee"},
        "inner": {"kind": "shirt", "color": "#f4f0e4"},
        "sleeves": {"kind": "long", "cuff": "#c8a050"},
        "legs": {"kind": "trousers", "color": "#2a2a36"},
        "boots": {"kind": "boots", "color": "#3a2c22", "height": "knee"},
        "gloves": {"kind": "gloves", "color": "#e8e0c8"},
        "belt": {"kind": "double", "color": "#3a2a22", "buckle": "#c8a050"},
    },
    "cape": {"kind": "cape", "color": "#d8b050", "trim": "#f0e0a8", "lining": "#5f7fb0", "length": 0.8, "emblem": {"kind": "tree", "color": "#8a6a20", "size": 8}, "clasp": "#c8a050"},
    "accessories": [{"kind": "shoulder_fur", "color": "#5f7fb0"}],
    "props": {"float": {"kind": "grimoire", "color": "#e0d0a0", "glow": "#c8a050", "length": 0.8}},
    "glow": {"color": "#c8a050", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Golden Dawn (arc/knights/GoldenDawnCast). Gold glow on the floating grimoire only.",
}
