"""Why: Grey, the painfully shy transmutation mage who turns into other people to be brave, a friend of the Sanctuary Hub (an NPC, record in arc/npc/BlackBullsNpcs.java; her
Transmutation Magic is already in the mod). Look (from the series and the wiki summaries): a petite young woman with a light blue bob, big nervous eyes and a blush, a cream
shirt, loose pink flower-print trousers (drawn plain pink), flat shoes and the short Black Bull cloak (its hood is left off: a hood laid back cut through the bob). Unverified: the print of the trousers, the exact
blue. The bull on the cloak glows pale blue. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "grey",
    "name": "Grey",
    "role": "npc",
    "build": "slim",
    "height": 1.74,
    "skin": "fair",
    "face": {"expression": "shy", "eye_color": "#6a88d0", "marks": ["blush"], "lashes": True},
    "hair": {"style": "bob", "color": "#a8d0f4"},
    "outfit": {
        "top": {"kind": "shirt", "color": "#f0ece0", "length": "hip", "collar": "wide"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#e0a0b8"},
        "boots": {"kind": "shoes", "color": "#7a5a3a"},
        "belt": {"kind": "plain", "color": "#6a4a3a", "buckle": "#c8a040"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.6, "ragged": True, "width": 0.9,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e4b454"},
    "glow": {"color": "#9ad8ff", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Black Bulls. The bull of the cloak glows pale blue.",
}
