"""Why: Mereoleona Vermillion, the strongest Vermillion: loud, merciless about training, the one who tells a delver to learn the Mana Zone and to hit something big. Look (summarised
from search results, see docs/arc/npcb.md section 10): a tall, muscular woman with long, wavy vermilion hair, blue eyes, a sharp canine tooth and a long red cape; here a sleeveless
dark-red top that shows her arms, wrapped fists, black trousers and boots, a gold-trimmed red cape with a flame, and a hot flame glow on her floating grimoire. Added for the game:
the sleeveless top and the wraps (no source describes her outfit); the mane style stands for her hair.
"""
SPEC = {
    "id": "mereoleona_vermillion",
    "name": "Mereoleona Vermillion",
    "role": "npc",
    "build": "broad",
    "height": 2.0,
    "widen": 0.94,
    "skin": "tan",
    "face": {"expression": "grin", "eyes": "fierce", "eye_color": "#4a7ac8", "brows": "thick", "mouth": "grin"},
    "hair": {"style": "wild_mane", "color": "#d0421e"},
    "outfit": {
        "top": {"kind": "tank", "color": "#5a1a14", "trim": "#e8b850"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "trousers", "color": "#241c1c"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "knee"},
        "gloves": {"kind": "wraps", "color": "#d8c8a8"},
        "belt": {"kind": "double", "color": "#3a2a22", "buckle": "#e8b850"},
    },
    "cape": {"kind": "cape", "color": "#a02020", "trim": "#e8b850", "length": 0.88, "emblem": {"kind": "flame", "color": "#e8b850", "size": 9}, "clasp": "#e8b850"},
    "props": {"float": {"kind": "grimoire", "color": "#c2401a", "glow": "#ff7a30", "length": 0.8}},
    "glow": {"color": "#ff7a30", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Crimson Lion (arc/knights/CrimsonLionCast). The proud idle (standing tall) is her stance; the kit's guard idle breaks its own arm rules for a character with no held prop.",
}
