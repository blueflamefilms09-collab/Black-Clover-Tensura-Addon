"""Why: Liliruca Arde (Lili), the pallum supporter who carries the loot of the Hestia Familia and knows every trick of the Dungeon: the friend of the DanMachi saga who sells the player
supplies and asks for the hunt of the Amphisbaena. Look, from the series (facts only, see docs/arc/danmachi_npcs.md): a very small girl (a pallum, the people that stay child-sized), short
auburn hair, a hooded brown cloak over a plain shirt and shorts, and an enormous backpack on her back. Added for the game, not from the source: nothing beyond the colours of the cloth;
the kit has no crossbow, so she holds a short knife in the hand.
"""
SPEC = {
    "id": "liliruca_arde",
    "name": "Liliruca Arde",
    "role": "npc",
    "build": "small",
    "height": 1.45,
    "skin": "light",
    "face": {"expression": "shy", "eye_color": "#a8503a", "brows": "thin"},
    "hair": {"style": "short", "color": "#b4683c"},
    "outfit": {
        "top": {"kind": "vest", "color": "#6a5236", "trim": "#c8a468"},
        "inner": {"kind": "shirt", "color": "#e8e0cc"},
        "legs": {"kind": "shorts", "color": "#4a3a2c"},
        "boots": {"kind": "boots", "color": "#3a2c22", "height": "ankle"},
        "gloves": {"kind": "fingerless", "color": "#4a3a2c"},
        "belt": {"kind": "double", "color": "#3a2a1c", "buckle": "#c8a040", "pouches": True},
    },
    "cape": {"kind": "mantle", "color": "#5a4a38", "length": 0.6},
    "accessories": [{"kind": "backpack", "color": "#7a5a38", "color2": "#c8a468"}],
    "props": {"right": {"kind": "dagger", "color": "#9aa0ac", "color2": "#6a4a2a"}},
    "glow": {"color": "#ffd88a", "eyes": False, "props": False, "trim": False},
    "clips": {"attack": "stab", "cast": "gesture", "idle": "relaxed"},
}
