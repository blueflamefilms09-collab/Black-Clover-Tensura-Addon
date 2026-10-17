"""Why: Yami Sukehiro, captain of the Black Bulls: the first NPC of the roster and the reference character that proves the NPC half of the art kit. Look (from the
series and the mod's own Black Bull cloak, docs/robe_anime.md): a broad, tanned, stubbled man with wild black hair, a ragged black cloak with the bull emblem on the back
and a strap at the throat over a plain white tank top, a belt of books, light chaps over black trousers, black boots, a cigarette, and a long single-edged katana.
Unverified additions (the owner's brief asks for "bandage / scarf"): wrapped forearms. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "yami_sukehiro",
    "name": "Yami Sukehiro",
    "role": "npc",
    "build": "broad",
    "height": 2.05,
    "skin": "tan",
    "face": {"expression": "smirk", "eyes": "sharp", "eye_color": "#2c2a32", "brows": "thick", "mouth": "smirk", "marks": ["stubble", "moustache"]},
    "hair": {"style": "spiky", "color": "#15151b"},
    "outfit": {
        "top": {"kind": "tank", "color": "#ecebea"},
        "inner": {"kind": "tank", "color": "#ecebea"},
        "legs": {"kind": "chaps", "color": "#1c1c22", "chaps": "#c4b08a"},
        "boots": {"kind": "boots", "color": "#1c1c20", "height": "calf"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#e4b454", "books": True},
        "neck": {"kind": "strap", "color": "#2a2a30"},
        "bandages": {"parts": ["forearm_l", "forearm_r"], "color": "#d8d2c0"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.78, "ragged": True, "width": 0.95, "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4},
             "clasp": "#e4b454"},
    "accessories": [{"kind": "cigarette", "glow": "#ff8a30"}],
    "props": {"right": {"kind": "katana", "color": "#5a5a68", "color2": "#e4b454"}},
    "clips": {"attack": "slash", "cast": "gesture", "idle": "relaxed"},
    "notes": "NPC of the Black Bulls. Dark magic is not drawn (no glow); the cigarette ember is the only glow.",
}
