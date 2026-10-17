"""Why: Asta, the boy with no magic and an anti-magic sword, the second friend a player meets in the Sanctuary Hub (an NPC of the Black Bulls, record in
arc/npc/BlackBullsNpcs.java). Look (from the series and the mod's own Black Bull cloak, docs/robe_anime.md): short upswept ash-grey hair, green eyes and a wide grin, a
strong young body in a black sleeveless top and dark trousers, the dark Black Bull headband with two trailing tails, the black cloak with the white bull on the back,
and the huge black Demon-Slayer sword carried across the back. Unverified: the exact grey of the hair (it reads silver in some art, white in other), the trousers and the
boots (plain dark ones are drawn); the sword has no glow because anti-magic is black. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "asta",
    "name": "Asta",
    "role": "npc",
    "build": "average",
    "height": 1.88,
    "skin": "light",
    "face": {"expression": "grin", "eye_color": "#4fae5c", "brows": "thick"},
    "hair": {"style": "spiky_back", "color": "#cfd2d8"},
    "outfit": {
        "top": {"kind": "tank", "color": "#1d1d22"},
        "legs": {"kind": "trousers", "color": "#26262d"},
        "boots": {"kind": "boots", "color": "#2c2219", "height": "calf"},
        "belt": {"kind": "plain", "color": "#4a3626", "buckle": "#c8a040"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.62, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e4b454"},
    "accessories": [{"kind": "headband", "color": "#1b1b20"}],
    "props": {"back": {"kind": "greatsword", "color": "#26262c", "color2": "#6a5a4a", "length": 0.62}},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Black Bulls. The anti-magic sword rides on the back (it has no glow: anti-magic is black); nothing else glows.",
}
