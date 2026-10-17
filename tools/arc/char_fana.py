"""Why: Fana, the Hatred of the Third Eye and the fire elf who calls the spirit Salamander: the boss of the Witches' Forest raid in the first saga. Look (from the Black Clover
wiki and search summaries, URLs in docs/arc/fana.md): a short, slim girl's body (a Diamond Kingdom orphan) with thick wavy pink hair to just below the shoulders and large sea-green
eyes, a purple mage stone on the forehead framed by a red tattoo with the third eye beneath it, a high-collared furry maroon dress under the light hooded robe of the Eye of the
Midnight Sun, knee-high boots and a shoulder pouch. Her grimoire of fire spells is in her hand (the wiki says it holds her fire spells). Added for the game, not from the source: the spark of Salamander that orbits her (a floating orb) and the glow of the stone.
The second form is the Evil Heart of her last stand: the hatred takes the body, the eyes and the stone burn, a crown of flames stands round her head and the hair whips.
"""
SPEC = {
    "id": "fana",
    "name": "Fana",
    "role": "boss",
    "build": "slim",
    "height": 1.74,
    "skin": "fair",
    "face": {"expression": "angry", "eyes": "big", "eye_color": "#3fb894", "brows": "angry", "mouth": "frown", "marks": [{"kind": "forehead_sigil", "color": "#c02030"}, {"kind": "forehead_gem", "color": "#9a4ae0", "glow": "#c58cff"}, {"kind": "third_eye", "color": "#d84a3a", "glow": "#ff6a4a"}]},
    "hair": {"style": "long", "color": "#e88fb4"},
    "outfit": {
        "top": {"kind": "dress", "color": "#7a1a2c", "trim": "#c8a0a8", "length": "knee", "collar": "fur", "collar_color": "#efe4da", "emblem": {"kind": "eye", "where": "chest", "color": "#e8c8c0", "size": 6, "y": 6}},
        "sleeves": {"kind": "long", "cuff": "#efe4da"},
        "legs": {"kind": "leggings", "color": "#3a1c24"},
        "boots": {"kind": "boots", "color": "#5a3a2c", "height": "knee", "cuff": "#c8a0a8"},
        "belt": {"kind": "plain", "color": "#3c281c", "buckle": "#c8a040"},
    },
    "cape": {"kind": "hooded_cloak", "color": "#e7e2d6", "trim": "#7a1a2c", "length": 0.85, "width": 0.9, "lining": "#b8b0a4"},
    "accessories": [{"kind": "satchel", "color": "#5a3a2c"}],
    "props": {"left": {"kind": "grimoire", "color": "#7a1a2c", "color2": "#c8a040", "glow": "#ffb347"}, "float": {"kind": "orb", "color": "#ff9a40", "glow": "#ffb347"}},
    "glow": {"color": "#ff7a2a", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "book", "idle": "proud"},
    "form2": {
        "name": "Fana, the Evil Heart",
        "face": {"expression": "mad", "eye_color": "#ff5a2a", "eye_glow": "#ff5a2a", "marks": [{"kind": "forehead_sigil", "color": "#ff3a2a", "glow": "#ff3a2a"}, {"kind": "forehead_gem", "color": "#ff4a6a", "glow": "#ff7a8a"}, {"kind": "third_eye", "color": "#ffd35a", "glow": "#ffb347"}, "veins"]},
        "hair": {"color": "#ff9ab0", "style": "wild_mane"},
        "outfit": {"top": {"color": "#b02030", "trim": "#ffb347"}, "legs": {"color": "#5a1c24"}},
        "cape": {"color": "#d8c8b8", "trim": "#b02030", "ragged": True},
        "halo": {"kind": "flames", "color": "#ff5a1a", "glow": "#ffb347", "radius": 1.1},
        "glow": {"color": "#ff4a1a", "eyes": True, "props": True, "trim": True},
    },
    "notes": "Fana of the Eye of the Midnight Sun. Salamander is an effect of the fight, not a model: the floating orb is its spark.",
}
