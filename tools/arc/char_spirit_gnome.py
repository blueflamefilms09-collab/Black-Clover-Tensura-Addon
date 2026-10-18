"""Why: Gnome, the earth Spirit Lord of Black Clover (entities nusmp:elemental_spirit_lord kind GNOME and nusmp:spirit_lord kind GNOME). The old model was a hunched stone giant: cracked plates,
a dark hollow face with glowing eyes (tools/gen_spirit_lords.py). The kit's version on the player's rig keeps that: a huge, wide, slouching body of brown-grey stone, plates of rough
rock strapped over it with moss in the seams, no face but a hollow with two amber eyes, fists like boulders. It stands on the ground. The colours are the old model's.
See docs/arc/remodel.md.
"""
ENTITIES = []

SPEC = {
    "id": "spirit_gnome",
    "name": "Gnome",
    "role": "boss",
    "build": "huge",
    "height": 3.0,
    "skin": ["#3a3028", "#54463a", "#6e5e4e", "#8a7864"],
    "face": {"expression": "dead", "eyes": "glow", "eye_color": "#ffb838", "eye_glow": "#ffc858", "brows": "heavy", "mouth": "none", "nose": "none", "marks": ["cracks"]},
    "hair": {"style": "bald", "color": "#3a3028"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#54463a"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "plain", "color": "#4a4a2a", "buckle": "#8a7a4a"},
    },
    "armor": {"pieces": ["breastplate", "pauldrons", "bracers", "greaves", "tassets"], "style": "plate", "color": "#86786a", "trim": "#5a7a40"},
    "accessories": [{"kind": "shoulder_fur", "color": "#5a7a40"}],
    "glow": {"color": "#ffb838", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "slouch"},
}
