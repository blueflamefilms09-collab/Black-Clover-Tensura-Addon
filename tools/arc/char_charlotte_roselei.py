"""Why: Charlotte Roselei, captain of the Blue Rose: the abrupt, cool-faced captain who tells a delver that a trap seen is half defused, and who does not want her name mentioned
to a certain Black Bull. Look (summarised from search results, see docs/arc/npcb.md section 10): a slender woman with long blonde hair and blue eyes, in the Blue Rose's blue cape and
silver half-armour with a flower on the chest (the kit's sun emblem stands for the rose); a cold, severe face with a touch of lipstick. Added for the game: the cut of the armour.
"""
SPEC = {
    "id": "charlotte_roselei",
    "name": "Charlotte Roselei",
    "role": "npc",
    "build": "slim",
    "height": 1.92,
    "skin": "fair",
    "face": {"expression": "cold", "eyes": "narrow", "eye_color": "#4a7ac8", "brows": "arched", "mouth": "line", "lashes": True, "marks": ["lipstick"]},
    "hair": {"style": "long", "color": "#e8d890"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#2a6a9a", "trim": "#c8d8e8", "collar": "stand", "length": "thigh"},
        "inner": {"kind": "shirt", "color": "#e8ecf4"},
        "sleeves": {"kind": "long", "cuff": "#c8d8e8"},
        "legs": {"kind": "trousers", "color": "#1c2a3c"},
        "boots": {"kind": "boots", "color": "#1c2430", "height": "knee"},
        "gloves": {"kind": "gloves", "color": "#c8d8e8"},
        "belt": {"kind": "plain", "color": "#1c2a3c", "buckle": "#c8d8e8"},
    },
    "armor": {"pieces": ["breastplate", "pauldrons", "bracers"], "style": "plate", "color": "#c8d4e0", "trim": "#2a6a9a", "emblem": {"kind": "sun", "color": "#2a6a9a"}},
    "cape": {"kind": "cape", "color": "#2a6a9a", "trim": "#c8d8e8", "lining": "#c8d8e8", "length": 0.86, "emblem": {"kind": "sun", "color": "#c8d8e8", "size": 9}, "clasp": "#c8d8e8"},
    "props": {"float": {"kind": "grimoire", "color": "#2a6a9a", "glow": "#60b0ff", "length": 0.8}},
    "glow": {"color": "#60b0ff", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Blue Rose (arc/knights/CaptainsCast). The sun emblem is the nearest of the kit's emblems to a rose.",
}
