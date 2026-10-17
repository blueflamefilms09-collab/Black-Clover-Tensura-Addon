"""Why: Diane, the Serpent's Sin of Envy of The Seven Deadly Sins, a giant of the Giant Clan who fights with the war hammer Gideon and the earth-shaping power Creation: a boss of the
Seven Deadly Sins saga (see docs/arc/diane.md for the fight). Look (summaries of database pages, the doc lists the URLs; nothing here is copied from a source): a young woman with
brown hair in two long twin tails, purple eyes, a childlike cheerful face, an orange one-piece suit, boots up to the knee, studded gauntlets and a metal band on her arm, over
thirty feet tall in her giant form (165 cm when she is human-sized). The MODEL is made at about two blocks, with the proportions of a person; the game draws it twice as big
(the hitbox of the giant is about 3.9 blocks, and the renderer scales a boss by hitbox over model height), and at its own size when she shrinks. Unverified (the pages could
not be opened, only searched): the cut of the suit and the studs, the colour of the boots. Added for the game: the glow in the orange of earth and sun, and the second form
(Drole's Dance: gold hair, crystal plates that glow, light lines on the face).
"""
SPEC = {
    "id": "diane",
    "name": "Diane",
    "role": "boss",
    "build": "average",
    "height": 2.0,
    "widen": 1.12,
    "skin": "light",
    "face": {"expression": "cheerful", "eye_color": "#9a58d8", "lashes": True, "marks": ["blush"]},
    "hair": {"style": "twin_tails", "color": "#8a5434", "ribbon": "#e8782a"},
    "outfit": {
        "top": {"kind": "dress", "color": "#e8782a", "trim": "#f8c070", "length": "hip"},
        "inner": {"kind": "none"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "shorts", "color": "#e8782a"},
        "boots": {"kind": "boots", "color": "#6a4426", "height": "knee", "cuff": "#f8c070"},
        "gloves": {"kind": "gauntlets", "color": "#8a8a98", "side": "both"},
        "belt": {"kind": "plain", "color": "#6a4426", "buckle": "#f8c070"},
    },
    "armor": {"pieces": ["bracers"], "style": "plate", "color": "#8a8a98", "trim": "#f8c070"},
    "props": {"right": {"kind": "hammer", "color": "#7a7a8a", "color2": "#c8a040", "glow": "#ffb040", "length": 1.25}},
    "glow": {"color": "#ffb040", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "smash", "cast": "magic", "idle": "relaxed", "extra": []},
    "form2": {
        "name": "Diane, in Drole's Dance",
        "face": {"eye_color": "#ffd070", "eye_glow": "#fff0b0", "marks": ["blush", "light_lines"]},
        "hair": {"color": "#c47a3a", "streak": "#ffd070"},
        "outfit": {"top": {"color": "#f09a3a", "trim": "#ffe0a0", "emblem": {"kind": "rune", "where": "chest", "size": 6, "color": "#ffe0a0", "glow": "#ffd070"}}},
        "armor": {"pieces": ["pauldrons", "bracers", "greaves"], "style": "scale", "color": "#e8a850", "trim": "#ffd070", "glow": "#ffd070"},
        "glow": {"color": "#ffd070", "eyes": True, "props": True, "trim": True},
    },
}
