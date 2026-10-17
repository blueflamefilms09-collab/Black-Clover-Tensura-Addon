"""Why: Gauche Adlai, the cold and clever mirror mage who loves his little sister, a friend of the Sanctuary Hub (an NPC, record in arc/npc/BlackBullsNpcs.java; his Mirror
Magic is already in the mod). Look (from the series and the wiki summaries): short messy black hair, pale skin, tired dark eyes, a cold mouth, a dark high-collared uniform
under the Black Bull cloak, and a mirror where his left eye should be (drawn as a silver monocle that shines violet). A glass orb of mirror light orbits him slowly. Unverified:
the cut of the uniform, the colour of the orb. The monocle, the orb and the cloak's bull glow violet-white. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "gauche_adlai",
    "name": "Gauche Adlai",
    "role": "npc",
    "build": "slim",
    "height": 1.96,
    "skin": "pale",
    "face": {"expression": "cold", "eye_color": "#2b2833", "brows": "flat", "marks": ["eye_bags"]},
    "hair": {"style": "messy", "color": "#17161d"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#26252e", "collar": "high", "trim": "#8a84b8", "length": "thigh"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#1e1d26"},
        "boots": {"kind": "boots", "color": "#18171d", "height": "calf"},
        "gloves": {"kind": "gloves", "color": "#1c1b22"},
        "belt": {"kind": "plain", "color": "#2c2430", "buckle": "#c0b8e8"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.7, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#c0b8e8"},
    "accessories": [{"kind": "monocle", "color": "#c8c8dc", "glow": "#a89cff", "side": "left"}],
    "props": {"float": {"kind": "orb", "color": "#d6d0f4", "glow": "#a89cff"}},
    "glow": {"color": "#a89cff", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Black Bulls. The orb that orbits him is a mirror of glass (his Mirror Magic); the monocle stands for the mirror that replaces his left eye.",
}
