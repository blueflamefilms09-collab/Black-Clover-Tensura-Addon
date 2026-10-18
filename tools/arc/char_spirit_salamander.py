"""Why: Salamander, the fire Spirit Lord of Black Clover (entities nusmp:elemental_spirit_lord kind SALAMANDER, nusmp:spirit_lord kind SALAMANDER, the companion). Its old model was a winged
fire dragon (red-orange scales, flames down the spine and along the tail, tools/gen_spirit_lords.py); the owner wants every mob of the mod on the player's rig with the art kit, and a
creature becomes an anthropomorphic design that keeps its features, so this is a dragonfolk spirit: a broad, red-orange scaled body with a mane of living flame for hair, ram-curled
dragon horns, bat-membrane dragon wings that glow along their veins, a long scaled tail whose tip is a flame, a scale cuirass and glowing slit eyes. It hovers (the spirit flies).
The colours are the old model's. See docs/arc/remodel.md.
"""
ENTITIES = []          # drawn by the elemental_spirit_lord and spirit_lord entities through their kind (client/remodel/RemodelTable)

SPEC = {
    "id": "spirit_salamander",
    "name": "Salamander",
    "role": "boss",
    "build": "broad",
    "height": 1.9,
    "skin": ["#8a2a14", "#b8421c", "#e0642a", "#ff9248"],
    "face": {"expression": "demon", "eyes": "slit", "eye_color": "#ffe040", "eye_glow": "#ffe860", "brows": "angry", "mouth": "fangs", "nose": "none", "marks": ["cheek_lines"]},
    "hair": {"style": "spiky_long", "color": "#ff7a20", "streak": "#ffe070"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "trousers", "color": "#8a2a14"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "sash", "color": "#7a2414", "buckle": "#ffd060"},
        "neck": {"kind": "necklace", "color": "#ffb030", "glow": True},
    },
    "armor": {"pieces": ["breastplate", "pauldrons", "bracers"], "style": "scale", "color": "#c8461c", "trim": "#ffb030", "glow": "#ff9a30"},
    "horns": {"kind": "ram", "color": "#3a1810", "color2": "#ff9a30", "glow": "#ff8a20", "size": 1.2},
    "wings": {"kind": "dragon", "color": "#a83018", "color2": "#ff9a30", "glow": "#ff8a20", "span": 1.3},
    "tail": {"kind": "dragon", "color": "#c8461c", "color2": "#ffd060", "glow": "#ff8a20", "length": 1.6},
    "glow": {"color": "#ff8a20", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "hover"},
}
