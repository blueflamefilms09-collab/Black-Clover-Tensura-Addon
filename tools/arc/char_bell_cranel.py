"""Why: Bell Cranel, the white-haired boy of the Hestia Familia and the hero of DanMachi, the first friend the player meets in the saga (on the stairs to the Dungeon).
Look, from the series (facts only, see docs/arc/danmachi_npcs.md): a slight boy of about fourteen with white hair and red eyes (the reason people call him a rabbit), a plain
light chest guard over a white shirt, dark trousers, brown boots and a short black knife at the hip, the knife his goddess gave him. Added for the game, not from the source: the
blue light in the knife, the sign of the Falna that grows with every Level.
"""
SPEC = {
    "id": "bell_cranel",
    "name": "Bell Cranel",
    "role": "npc",
    "build": "slim",
    "height": 1.8,
    "skin": "fair",
    "face": {"expression": "cheerful", "eye_color": "#d8404c", "brows": "thin"},
    "hair": {"style": "shaggy", "color": "#f2f2f6"},
    "outfit": {
        "top": {"kind": "shirt", "color": "#e6e8f0", "collar": "stand"},
        "legs": {"kind": "trousers", "color": "#3a3432"},
        "boots": {"kind": "boots", "color": "#5a4030", "height": "ankle"},
        "gloves": {"kind": "fingerless", "color": "#4a3a30"},
        "belt": {"kind": "plain", "color": "#4a3424", "buckle": "#c8c8d0", "pouches": True},
    },
    "armor": {"pieces": ["breastplate", "bracers"], "style": "plate", "color": "#aeb6c4", "trim": "#e8e8f0"},
    "props": {"right": {"kind": "dagger", "color": "#2a2a38", "color2": "#d8c070", "glow": "#8ac0ff", "sheathed": False}},
    "glow": {"color": "#8ac0ff", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "stab", "cast": "gesture", "idle": "relaxed"},
}
