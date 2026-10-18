"""Why: the Goliath (entity nusmp:goliath, dungeon/floor/goliath/GoliathBoss), the Upper Catacombs' floor boss: a colossal pale-stone giant, six blocks tall, built out of the ruin's own
masonry, with bedrock-grey joints, moss in the creases, a heavy brow, tiny amber eyes, boulder fists and cracks that glow faintly amber. The hand-built model (tools/gen_goliath.py,
kept) is replaced by the art kit's player-proportioned giant: a `huge` body stretched to 6 blocks and widened, skin of pale cut stone, plates of masonry for armour with moss on the
trims, a hollow-eyed face under a heavy brow ridge, bare boulder fists, and cracks that glow. `form2` is the Gates: every crack opened and burning (the old model's "cracked" skin, the
renderer wears it from the middle of the transformation). The old boss had a clip for each of its moves (slam, stomp, roar, fist, line, rubble); the kit has the standard set, so every
regular move plays `cast` and the ultimate `cast_big`, the rise from the floor plays `spawn`. See docs/arc/remodel.md. Colours are the old model's.
"""
ENTITIES = ["goliath"]

SPEC = {
    "id": "goliath",
    "name": "Goliath",
    "role": "boss",
    "build": "huge",
    "height": 6.0,
    "widen": 1.9,
    "skin": ["#6e6c64", "#8a887e", "#a8a69a", "#c4c2b4"],
    "face": {"expression": "angry", "eyes": "narrow", "eye_color": "#ffb030", "eye_glow": "#ffc050", "brows": "heavy", "mouth": "line", "nose": "wide", "marks": ["cracks"]},
    "hair": {"style": "bald", "color": "#6e6c64"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#7a786e"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "plain", "color": "#5a5850", "buckle": "#ffb030"},
    },
    "armor": {"pieces": ["breastplate", "pauldrons", "bracers", "gauntlets", "greaves", "tassets"], "style": "plate", "color": "#9c9a8e", "trim": "#6a8a48"},
    "accessories": [{"kind": "shoulder_fur", "color": "#5a7a3a"}],
    "glow": {"color": "#ffa030", "eyes": True, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "magic", "idle": "slouch", "extra": ["spawn"]},
    "form2": {  # the second model is drawn by the renderer from the middle of the Gates' transformation
        "name": "Goliath, Gates Open",
        "face": {"marks": ["cracks", "veins", "cheek_lines", "forehead_sigil"], "eye_color": "#ffd060", "eye_glow": "#ffd060"},
        "armor": {"trim": "#ffa030", "glow": "#ffa030"},
        "outfit": {"belt": {"color": "#8a5a28"}},
        "glow": {"color": "#ffa030", "eyes": True, "props": True, "trim": True},
    },
}

HEAD_SCALE = 2.1


def post(kit, rig):
    """A man's 8 px head on a six-block giant looks like a pin: the head bone is drawn HEAD_SCALE times as big in every clip (a steady scale, as the minotaur's and the Amphisbaena's)."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
