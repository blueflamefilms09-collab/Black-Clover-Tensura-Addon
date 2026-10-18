"""Why: the legacy Zagred (entity nusmp:zagred_legacy, entity/ZagredBossEntity): the first Zagred boss, kept registered when the remade one (char_zagred.py, nusmp:zagred) took the id.
Its 0.48 "true form" was a hand-built cube devil: a tall black devil with spread bat wings, horns, claws and a whip tail, red eyes, a ribcage lit by cracks and runes, and from the
fourth phase the otherworldly trident in its right claws. The owner asked for every mob of the mod to be a player-model character built with the art kit, so this is that devil
again, on the kit's body: a black-violet lanky devil with a bare cracked ribcage, glowing red rune lines, bat wings with lit veins, curved horns and a long whip tail. It stays
a DIFFERENT design from the remade Zagred (grey skin, a black coat, a floating grimoire). `form2` is "phase 4": the same body with the trident in its right hand; the renderer wears it
from the fourth phase (the old renderer drew the trident as an item in the hand). The shader aura and the target reticle of the old renderer are kept (client/remodel). See docs/arc/remodel.md.
"""
ENTITIES = ["zagred_legacy"]

SPEC = {
    "id": "zagred_legacy",
    "name": "Zagred (legacy)",
    "role": "boss",
    "build": "lanky",
    "height": 2.2,
    "skin": ["#2a2236", "#403454", "#584a70", "#74648c"],
    "face": {"expression": "demon", "eyes": "slit", "eye_color": "#ff3040", "eye_glow": "#ff4a58", "brows": "angry", "mouth": "fangs", "nose": "none",
             "marks": ["cracks", "veins", "forehead_sigil"]},
    "hair": {"style": "spiky_long", "color": "#14101c"},
    "outfit": {
        "top": {"kind": "bare"},
        "inner": {"kind": "none"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "trousers", "color": "#241c2c", "stripe": "#ff3040"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "sash", "color": "#2a1a2c", "buckle": "#ff3040"},
        "neck": {"kind": "necklace", "color": "#ff3040", "glow": True},
    },
    "armor": {"pieces": ["spikes", "bracers"], "style": "bone", "color": "#8a7a9c", "trim": "#ff3040", "glow": "#ff3040"},
    "horns": {"kind": "ram", "color": "#1a1422", "color2": "#6a4a78", "glow": "#ff3040", "size": 1.3},
    "ears": {"kind": "pointed", "size": 1.3},
    "wings": {"kind": "bat", "color": "#3a2c52", "color2": "#6a5a88", "glow": "#ff3040", "span": 1.5},
    "tail": {"kind": "devil", "color": "#241c30", "color2": "#ff3040", "glow": "#ff3040", "length": 1.7},
    "glow": {"color": "#ff3040", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "proud"},
    "form2": {
        "name": "Zagred (legacy), phase 4",
        "props": {"right": {"kind": "trident", "color": "#2a2038", "color2": "#8a6aa8", "glow": "#ff3040", "length": 1.1}},
        "glow": {"color": "#ff4a58", "eyes": True, "props": True, "trim": True},
    },
}
