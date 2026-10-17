"""Why: Hestia, the goddess of the hearth who has come down to the lower world and founded the Hestia Familia with Bell: the second friend of the DanMachi saga, who gives the player
the Falna that Levels you up. Look, from the series (facts only, see docs/arc/danmachi_npcs.md): a short girl-shaped goddess with long black hair in two tails tied with a blue ribbon, blue
eyes, a white dress and a blue ribbon that winds round her arms. The kit has no cord prop, so the ribbon on the arms is the kit's bandage in blue; the colour of the eyes and the tie of
the tails are the series'. Added for the game, not from the source: the blue light in the ribbon (the divine blessing).
"""
SPEC = {
    "id": "hestia",
    "name": "Hestia",
    "role": "npc",
    "build": "small",
    "height": 1.5,
    "skin": "fair",
    "face": {"expression": "cheerful", "eye_color": "#3a78d8", "brows": "raised"},
    "hair": {"style": "twin_tails", "color": "#17171f", "ribbon": "#3a78d8"},
    "outfit": {
        "top": {"kind": "dress", "color": "#f4f2ec", "trim": "#3a78d8", "length": "thigh", "hem_trim": True},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "bare"},
        "boots": {"kind": "sandals", "color": "#e8e4dc"},
        "belt": {"kind": "sash", "color": "#3a78d8", "buckle": "#e8d078"},
        "bandages": {"parts": ["arm_l", "arm_r"], "color": "#3a78d8"},
    },
    "glow": {"color": "#8ac0ff", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "none", "cast": "gesture", "idle": "relaxed"},
}
