"""Why: Finral Roulacase, the cowardly, flirty spatial mage who is the squad's portal, a friend of the Sanctuary Hub who explains the roads and the stairs (an NPC, record in
arc/npc/BlackBullsNpcs.java; his Spatial Magic is already in the mod). Look (from the series and the wiki summaries): a slim man with unruly light brown hair, big droopy
friendly eyes, green earrings, a light open shirt and brown trousers under the Black Bull cloak. Green is the colour of his portals: the earrings and the cloak's bull glow
green. Unverified: the shirt and the trousers, the green tips of the hair (only a streak is drawn). See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "finral_roulacase",
    "name": "Finral Roulacase",
    "role": "npc",
    "build": "slim",
    "height": 1.9,
    "skin": "light",
    "face": {"expression": "shy", "eye_color": "#5a8a5a", "brows": "sad", "mouth": "smile"},
    "hair": {"style": "short", "color": "#9a7650"},
    "outfit": {
        "top": {"kind": "shirt", "color": "#e8e2cf", "length": "hip", "collar": "wide"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#5a4630"},
        "boots": {"kind": "boots", "color": "#4a3424", "height": "calf"},
        "belt": {"kind": "plain", "color": "#3c2a1c", "buckle": "#c8a040"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.7, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e4b454"},
    "accessories": [{"kind": "earrings", "color": "#58c878", "glow": "#58e898"}],
    "glow": {"color": "#58e898", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Black Bulls. Green earrings and the green bull of the cloak glow (his portals).",
}
