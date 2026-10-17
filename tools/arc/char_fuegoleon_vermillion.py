"""Why: Fuegoleon Vermillion, captain of the Crimson Lion: the calm, honourable elder brother of the Vermillion family, the first captain the Sanctuary welcomes after the first
champion has fallen. Look (summarised from search results, see docs/arc/npcb.md section 10; no source described his face, so hair and eyes are a choice): a tall, upright
man with swept-back deep-red hair, a high-collared dark-red uniform with gold trim, black gloves and a crimson mantle with a gold flame; his grimoire floats at his side with a
flame glow (his magic is Flame Magic with the fire spirit Salamander). Added for the game: everything of the palette except the squad's red.
"""
SPEC = {
    "id": "fuegoleon_vermillion",
    "name": "Fuegoleon Vermillion",
    "role": "npc",
    "build": "average",
    "height": 2.08,
    "skin": "light",
    "face": {"expression": "calm", "eyes": "sharp", "eye_color": "#c8782a", "brows": "thick", "mouth": "line"},
    "hair": {"style": "slick", "color": "#b8301c"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#4a1a18", "trim": "#e8b850", "collar": "high", "buttons": True, "length": "thigh"},
        "inner": {"kind": "shirt", "color": "#e8e4dc"},
        "sleeves": {"kind": "long", "cuff": "#e8b850"},
        "legs": {"kind": "trousers", "color": "#1c1c22"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "knee"},
        "gloves": {"kind": "gloves", "color": "#1c1c22"},
        "belt": {"kind": "double", "color": "#3a2a22", "buckle": "#e8b850"},
    },
    "cape": {"kind": "mantle", "color": "#b02a1c", "trim": "#e8b850", "length": 0.72, "emblem": {"kind": "flame", "color": "#e8b850", "size": 8}, "clasp": "#e8b850"},
    "props": {"float": {"kind": "grimoire", "color": "#8e1a14", "glow": "#ff7a30", "length": 0.8}},
    "glow": {"color": "#ff7a30", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Crimson Lion (arc/knights/CrimsonLionCast). Flame glow on the floating grimoire only.",
}
