"""Why: Gordon Agrippa, the shy, mumbling poison and curse mage with the kindest heart of the Black Bulls, a friend of the Sanctuary Hub (an NPC, record in
arc/npc/BlackBullsNpcs.java; his Poison Magic and Curse-Worker's Neighbor are already in the mod). Look (from the series and the wiki summaries): tall and thin, pale, a long face
with a large nose, no eyebrows, slicked-back black hair, dark circles under tired eyes, a dark grey coat under the Black Bull cloak with its hood laid back. Unverified: the coat
and the hood (the series draws him in the plain squad robe). The bull on the cloak glows poison green. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "gordon_agrippa",
    "name": "Gordon Agrippa",
    "role": "npc",
    "build": "lanky",
    "height": 2.08,
    "skin": "pale",
    "face": {"expression": "tired", "eye_color": "#3a3a46", "brows": "none", "mouth": "frown", "nose": "wide", "marks": ["eye_bags"]},
    "hair": {"style": "side_part", "color": "#0f0f14"},
    "outfit": {
        "top": {"kind": "coat", "color": "#2c2b38", "length": "knee", "collar": "high"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#1e1d27"},
        "boots": {"kind": "boots", "color": "#16161b", "height": "calf"},
        "belt": {"kind": "plain", "color": "#2c2430", "buckle": "#8a9a70"},
    },
    "cape": {"kind": "hooded_cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.8, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#8a9a70"},
    "glow": {"color": "#a0e060", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Black Bulls. The bull of the cloak glows poison green.",
}
