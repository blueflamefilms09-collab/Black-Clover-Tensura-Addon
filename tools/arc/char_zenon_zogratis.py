"""Why: Zenon Zogratis, the youngest of the Dark Triad and the one with the least to say (entity nusmp:zenon_zogratis, entity/zogratis/ZenonBoss). Remodelled from the old hand-built
model into the art kit's player-proportioned character (docs/arc/remodel.md): lean, bareheaded (no crown, as in the references), short black hair, a vertical scar with cross bars
that runs through the forehead and one eye, pale eyes, a maroon belted vest over bare arms. `form2` is the hybrid of the Gates (100% Beelzebub): bone spines and ribs through the
vest, a bone crest for hair, the wing remnants of an insect, hollow eyes that burn indigo. The colours and the cut are the game's reading of the design, not a trace.
"""
ENTITIES = ["zenon_zogratis"]

SPEC = {
    "id": "zenon_zogratis",
    "name": "Zenon Zogratis",
    "role": "boss",
    "build": "average",
    "height": 2.0,
    "skin": "pale",
    "face": {"expression": "cold", "eyes": "narrow", "eye_color": "#c8d4e0", "brows": "flat", "mouth": "line", "nose": "sharp",
             "marks": ["scar_eye_l", "scar_brow_l", "scar_cheek_l", "eye_bags"]},
    "hair": {"style": "short", "color": "#14101a"},
    "outfit": {
        "top": {"kind": "vest", "color": "#7a2430", "trim": "#2a1418", "length": "hip", "collar": "stand", "pattern": "patches", "pattern_color": (96, 28, 40), "hem_trim": True},
        "inner": {"kind": "tank", "color": "#2a2228"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "trousers", "color": "#2a2228"},
        "boots": {"kind": "boots", "color": "#241c20", "height": "calf"},
        "gloves": {"kind": "wraps", "color": "#c8c0b0"},
        "belt": {"kind": "double", "color": "#2a1a1c", "buckle": "#b8a888", "pouches": True},
    },
    "glow": {"color": "#6a78ff", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "magic", "idle": "slouch"},
    "form2": {
        "name": "Zenon Zogratis, Beelzebub",
        "skin": ["#8a8a98", "#a8a8b8", "#c4c4d0", "#dcdce4"],
        "face": {"eye_color": "#6a78ff", "eye_glow": "#8a96ff", "eyes": "empty", "mouth": "fangs", "marks": ["scar_eye_l", "scar_brow_l", "cracks", "veins"]},
        "hair": {"style": "mohawk", "color": "#d8d0b4"},
        "outfit": {"top": {"color": "#5a1a28", "ragged": True}, "gloves": {"color": "#d8d0b4"}},
        "armor": {"pieces": ["spikes", "pauldrons", "bracers"], "style": "bone", "color": "#d8d0b4", "trim": "#6a78ff", "glow": "#6a78ff"},
        "wings": {"kind": "fairy", "color": "#8a96c8", "color2": "#c4ccee", "glow": "#6a78ff", "span": 0.7},
        "horns": {"kind": "small", "color": "#d8d0b4", "color2": "#8a8470", "glow": "#6a78ff", "size": 1.0},
        "glow": {"color": "#6a78ff", "eyes": True, "props": True, "trim": True},
    },
}
