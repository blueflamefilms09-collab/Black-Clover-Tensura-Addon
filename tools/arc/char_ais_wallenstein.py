"""Why: Ais Wallenstein, the Sword Princess of the Loki Familia and the swordswoman who saved Bell on the 5th floor: the knight of the DanMachi saga and the friend who asks the player for the
hunt of Udaeus. Look, from the series (facts only, see docs/arc/danmachi_npcs.md): a slim young woman with long golden hair and golden eyes, a calm, nearly expressionless face, dark blue
battle clothes under a silver breastplate and light armour on the arms and shins, and a straight sword; her magic is the wind. Added for the game, not from the source: the pale green
light in the blade (the wind of her magic).
"""
SPEC = {
    "id": "ais_wallenstein",
    "name": "Ais Wallenstein",
    "role": "npc",
    "build": "slim",
    "height": 1.85,
    "skin": "fair",
    "face": {"expression": "calm", "eye_color": "#e0b040", "brows": "flat"},
    "hair": {"style": "long", "color": "#f0d070"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#2c3c6c", "trim": "#c8d4e4", "length": "thigh", "hem_trim": True},
        "inner": {"kind": "shirt", "color": "#e8ecf4"},
        "legs": {"kind": "leggings", "color": "#1e2438"},
        "boots": {"kind": "boots", "color": "#2a3048", "height": "knee", "cuff": "#c8d4e4"},
        "gloves": {"kind": "fingerless", "color": "#2a3048"},
        "belt": {"kind": "plain", "color": "#3a3a50", "buckle": "#c8d4e4"},
    },
    "armor": {"pieces": ["breastplate", "bracers", "greaves"], "style": "plate", "color": "#c4d0e0", "trim": "#5a78c8", "glow": "#9af0e0"},
    "props": {"right": {"kind": "sword", "color": "#d8e2f0", "color2": "#5a78c8", "glow": "#9af0e0"}},
    "glow": {"color": "#9af0e0", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "slash", "cast": "gesture", "idle": "proud"},
}
