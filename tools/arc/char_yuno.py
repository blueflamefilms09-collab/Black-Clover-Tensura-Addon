"""Why: Yuno, the Golden Dawn's young wind mage and the quiet rival of the Black Bulls' Asta: the first of the juniors a new delver meets in the Sanctuary, so he must be
recognisable at a glance. Look (from the series, summarised from search results; see docs/arc/npcb.md section 10 for what is unverified): a slim young man with short,
messy black hair and a calm, composed face, in the Golden Dawn's gold-tan mantle with a feathered blue collar over a plain black shirt, his four-leaf grimoire floating at
his side with a wind-green glow (his spirit is Sylph, the Spirit of Zephyr). Added for the game: the green glow of the grimoire pages; the eye colour is a guess.
"""
SPEC = {
    "id": "yuno",
    "name": "Yuno",
    "role": "npc",
    "build": "slim",
    "height": 2.0,
    "skin": "fair",
    "face": {"expression": "calm", "eyes": "sharp", "eye_color": "#2f6f5a", "brows": "thin"},
    "hair": {"style": "bangs_long", "color": "#16161c"},
    "outfit": {
        "top": {"kind": "shirt", "color": "#1c1c24", "trim": "#c8a050", "collar": "stand"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#2a2a36"},
        "boots": {"kind": "boots", "color": "#3a2c22", "height": "calf"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#c8a050"},
    },
    "cape": {"kind": "mantle", "color": "#b79b6a", "trim": "#f0e0a8", "length": 0.62, "emblem": {"kind": "sun", "color": "#f0e0a8", "size": 7}, "clasp": "#c8a050"},
    "accessories": [{"kind": "shoulder_fur", "color": "#5f7fb0"}],
    "props": {"float": {"kind": "grimoire", "color": "#2e3a1c", "glow": "#7fd6a0", "length": 0.8}},
    "glow": {"color": "#7fd6a0", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Golden Dawn (arc/knights/GoldenDawnCast). The wind-green glow is on the floating grimoire only.",
}
