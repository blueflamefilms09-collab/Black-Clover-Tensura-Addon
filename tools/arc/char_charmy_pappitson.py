"""Why: Charmy Pappitson, the tiny, ever-hungry cotton and food mage of the Black Bulls, a friend of the Sanctuary Hub who talks about meat, stones and sheep (an NPC, record in
arc/npc/BlackBullsNpcs.java; her Cotton and Food Magic and the cotton sheep are already in the mod). Look (from the series and the wiki summaries): a petite girl with green eyes
and black hair gathered in a bun, a cream tunic and brown shorts under a Black Bull cloak that is too big for her, and a satchel for food. Unverified: the exact outfit (the series
draws her in several), the bun (the wiki says an upside-down bun). Only the cloak's bull glows, a warm biscuit colour. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "charmy_pappitson",
    "name": "Charmy Pappitson",
    "role": "npc",
    "build": "small",
    "height": 1.52,
    "skin": "fair",
    "face": {"expression": "cheerful", "eye_color": "#4aa860", "marks": ["blush"]},
    "hair": {"style": "short", "color": "#201e26"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#f0e4c8", "length": "hip", "trim": "#c89a58"},
        "sleeves": {"kind": "short"},
        "legs": {"kind": "shorts", "color": "#6a4a30"},
        "boots": {"kind": "shoes", "color": "#5a3c28"},
        "belt": {"kind": "plain", "color": "#4a3020", "buckle": "#c8a040"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.62, "ragged": False, "width": 1.0,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 4, "y": 0.5}, "clasp": "#e4b454"},
    "accessories": [{"kind": "satchel", "color": "#a8743c"}],
    "glow": {"color": "#f0b070", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Black Bulls. A child-sized body ('small'); the satchel is for food.",
}
