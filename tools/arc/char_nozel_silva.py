"""Why: Nozel Silva, captain of the Silver Eagle: the cold, proud noble who says the least and means the most, who tells a delver that a defence with no gap is worth ten attacks.
Look (summarised from search results, see docs/arc/npcb.md section 10): a tall, slender man with shoulder-length white-silver hair and blue eyes, a silver-white coat and cape with the
eagle's wings, a faint mercury shimmer (his Mercury Magic); a cold, unsmiling face. Added for the game: the cut of the coat and the dark lining of the cape.
"""
SPEC = {
    "id": "nozel_silva",
    "name": "Nozel Silva",
    "role": "npc",
    "build": "slim",
    "height": 2.0,
    "skin": "fair",
    "face": {"expression": "cold", "eyes": "narrow", "eye_color": "#5a8ad0", "brows": "flat", "mouth": "line"},
    "hair": {"style": "long", "color": "#e0e4ee"},
    "outfit": {
        "top": {"kind": "coat", "color": "#d4d8e4", "trim": "#8890a8", "collar": "high", "buttons": True, "length": "knee"},
        "inner": {"kind": "shirt", "color": "#2a2e3a"},
        "sleeves": {"kind": "long", "cuff": "#8890a8"},
        "legs": {"kind": "trousers", "color": "#2a2e3a"},
        "boots": {"kind": "boots", "color": "#1e2028", "height": "knee"},
        "gloves": {"kind": "gloves", "color": "#d4d8e4"},
        "belt": {"kind": "plain", "color": "#3a3e4a", "buckle": "#c8ccd8"},
    },
    "cape": {"kind": "cape", "color": "#c8ccd8", "trim": "#8890a8", "lining": "#2a3a6a", "length": 0.86, "emblem": {"kind": "wings", "color": "#4a5a8a", "size": 10}, "clasp": "#e0e4ee"},
    "props": {"float": {"kind": "grimoire", "color": "#c8ccd8", "glow": "#a0b8e0", "length": 0.8}},
    "glow": {"color": "#a0b8e0", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Silver Eagle (arc/knights/SilverEagleCast). A mercury-blue glow on the floating grimoire only.",
}
