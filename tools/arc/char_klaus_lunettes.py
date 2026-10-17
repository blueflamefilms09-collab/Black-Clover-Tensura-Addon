"""Why: Klaus Lunettes, the Golden Dawn's proud, careful strategist: the junior who tells a delver to read the warnings and the ground before committing. Look (from the Black
Clover wiki as summarised by search results, see docs/arc/npcb.md section 10): a tall, slender nobleman with short light-blue hair swept to the sides and back with an angular fringe,
dark rectangular glasses, gold hoop earrings and fuchsia eyes, in a double-breasted, high-collared shirt with gold trim; his Steel Magic makes walls and lances, so he wears steel
bracers and his grimoire floats at his side (a held lance was twice his height and shrank him in every picture). The squad's gold mantle with the blue-furred shoulders is added so he reads as the Golden Dawn from a distance. The fringe side is not modelled.
"""
SPEC = {
    "id": "klaus_lunettes",
    "name": "Klaus Lunettes",
    "role": "npc",
    "build": "slim",
    "height": 1.99,
    "skin": "light",
    "face": {"expression": "stern", "eyes": "sharp", "eye_color": "#d870b0", "brows": "thin", "mouth": "line"},
    "hair": {"style": "side_part", "color": "#8fb8e0"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#2a3248", "trim": "#d8b050", "collar": "high", "buttons": True, "length": "thigh"},
        "inner": {"kind": "shirt", "color": "#e8e4dc"},
        "sleeves": {"kind": "long", "cuff": "#d8b050"},
        "legs": {"kind": "trousers", "color": "#1e2230"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "knee"},
        "gloves": {"kind": "gloves", "color": "#e8e4dc"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#d8b050"},
    },
    "cape": {"kind": "mantle", "color": "#c8a860", "trim": "#f0e0a8", "length": 0.62, "emblem": {"kind": "sun", "color": "#f0e0a8", "size": 7}, "clasp": "#d8b050"},
    "accessories": [{"kind": "glasses", "color": "#1c1c24"}, {"kind": "earrings", "color": "#d8b050"}, {"kind": "shoulder_fur", "color": "#5f7fb0"}],
    "armor": {"pieces": ["bracers"], "style": "plate", "color": "#9ab0c0", "trim": "#d8e0e8"},
    "props": {"float": {"kind": "grimoire", "color": "#9ab0c0", "glow": "#bcd4ec", "length": 0.8}},
    "glow": {"color": "#bcd4ec", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Golden Dawn (arc/knights/GoldenDawnCast). A faint steel-blue glow on the floating grimoire only.",
}
