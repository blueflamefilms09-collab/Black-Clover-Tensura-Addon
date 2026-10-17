"""Why: Luck Voltia, the smiling lightning mage who loves a fight, a friend of the Sanctuary Hub (an NPC, record in arc/npc/BlackBullsNpcs.java; his Lightning Magic and the
Thunder Fiend mode are already in the mod). Look (from the series and the wiki summaries): short and slim, messy medium-length blond hair, light blue eyes and a wide happy grin,
an aqua tunic and teal knee-length trousers under the Black Bull cloak, gold fingerless gloves where the lightning gathers. The bull on the cloak glows lightning yellow.
Unverified: the proportions of the cloak on so small a body. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "luck_voltia",
    "name": "Luck Voltia",
    "role": "npc",
    "build": "slim",
    "height": 1.74,
    "skin": "light",
    "face": {"expression": "grin", "eyes": "wide", "eye_color": "#7ac8f0", "brows": "raised"},
    "hair": {"style": "short", "color": "#e8c84a"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#4ab8c8", "length": "thigh", "trim": "#2a8a96"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#25808a"},
        "boots": {"kind": "shoes", "color": "#2a2a30"},
        "gloves": {"kind": "fingerless", "color": "#e8c040"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#e8c040"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.6, "ragged": True, "width": 0.9,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e8c040"},
    "glow": {"color": "#ffe35a", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Black Bulls. The bull of the cloak glows lightning yellow.",
}
