"""Why: Patolli, the elf who leads the Eye of the Midnight Sun behind the face of Licht: the boss that carries the Julius Novachrono fight of the first saga (light speed against
foresight). Look (from the Black Clover wiki and search summaries, URLs in docs/arc/patolli.md): tall and slim, long white hair worn in a braid, gold eyes ringed with red tattoos, a long
white hooded robe edged in black over dark layers and light trousers, a black shawl, a small cap, and the four-leaf grimoire he took for Light Magic (the book in his hand). The second
form is the Dark Elf he becomes when the light turns (Demon Light Magic): dark skin, black eyes ringed with violet fire, pointed ears, a robe that has turned to night, a broken halo.
Added for the game, not from the source: the glow of the grimoire and the broken halo.
"""
SPEC = {
    "id": "patolli",
    "name": "Patolli",
    "role": "boss",
    "build": "slim",
    "height": 2.02,
    "skin": "fair",
    "face": {"expression": "cold", "eyes": "narrow", "eye_color": "#e8c050", "eye_glow": "#ffe58a", "brows": "thin", "mouth": "smirk",
             "marks": [{"kind": "eyeshadow", "color": "#b02828"}, {"kind": "war_paint", "color": "#b02828"}]},
    "hair": {"style": "braid", "color": "#f2f0ea"},
    "outfit": {
        "top": {"kind": "robe", "color": "#ece8e0", "trim": "#1c1c24", "length": "floor", "collar": "stand", "hem_trim": True},
        "inner": {"kind": "shirt", "color": "#2a2a32"},
        "sleeves": {"kind": "wide", "cuff": "#1c1c24"},
        "legs": {"kind": "trousers", "color": "#d8d4cc"},
        "boots": {"kind": "boots", "color": "#1c1c22", "height": "calf"},
        "belt": {"kind": "sash", "color": "#1c1c24", "buckle": "#c8a040"},
    },
    "cape": {"kind": "mantle", "color": "#1c1c24", "trim": "#c8a040", "length": 0.42, "width": 1.0},
    "accessories": [{"kind": "circlet", "color": "#c8a040", "color2": "#e8c050"}],
    "props": {"left": {"kind": "grimoire", "color": "#e8e0c0", "color2": "#c8a040", "glow": "#ffe58a"}},
    "glow": {"color": "#ffe9a8", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "book", "idle": "proud"},
    "form2": {
        "name": "Patolli, the Dark Elf",
        "skin": "#5a4868",
        "face": {"expression": "sinister", "eye_color": "#14101c", "eye_glow": "#c060ff", "marks": [{"kind": "eyeshadow", "color": "#9a40e0"}, {"kind": "war_paint", "color": "#9a40e0", "glow": "#c060ff"}]},
        "hair": {"color": "#f4f0ff"},
        "outfit": {"top": {"color": "#2a1a3a", "trim": "#9a40e0"}, "inner": {"color": "#120a1c"}, "sleeves": {"cuff": "#9a40e0"}, "legs": {"color": "#1c1226"}, "belt": {"color": "#120a1c", "buckle": "#c060ff"}},
        "cape": {"color": "#0e0816", "trim": "#9a40e0"},
        "accessories": [{"kind": "circlet", "color": "#9a40e0", "color2": "#c060ff", "glow": "#c060ff"}],
        "props": {"left": {"kind": "grimoire", "color": "#2a1a3a", "color2": "#9a40e0", "glow": "#c060ff"}},
        "ears": {"kind": "elf"},
        "halo": {"kind": "broken", "color": "#6a3aa8", "glow": "#c060ff", "radius": 1.0},
        "glow": {"color": "#c060ff", "eyes": True, "props": True, "trim": True},
    },
    "notes": "The leader of the Eye of the Midnight Sun. Light Magic is an effect of the fight; the Dark Elf form is the Gates (100% power).",
}
