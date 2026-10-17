"""Why: Leopold Vermillion, the youngest of the Crimson Lion's Vermillions: the loud, sincere beginner's cheerleader who calls every delver his rival, so he is the lively first
face of the squad in the Sanctuary. Look (summarised from search results, see docs/arc/npcb.md section 10): a young, wiry man with short spiky red hair, a sharp canine tooth and an
eager face, in a dark-red tunic under the Crimson Lion's red mantle with a gold flame, his grimoire floating at his side with a flame glow. Added for the game: the colours of the tunic
and the eyes (no source describes them).
"""
SPEC = {
    "id": "leopold_vermillion",
    "name": "Leopold Vermillion",
    "role": "npc",
    "build": "average",
    "height": 1.88,
    "skin": "light",
    "face": {"expression": "grin", "eyes": "big", "eye_color": "#c8782a", "brows": "thick", "mouth": "grin"},
    "hair": {"style": "spiky", "color": "#d8431d"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#8a2018", "trim": "#d8a848", "length": "thigh", "collar": "stand"},
        "sleeves": {"kind": "short", "cuff": "#d8a848"},
        "legs": {"kind": "trousers", "color": "#1c1c22"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "calf"},
        "gloves": {"kind": "fingerless", "color": "#2a2220"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#d8a848"},
    },
    "cape": {"kind": "mantle", "color": "#b02a1c", "trim": "#d8a848", "length": 0.6, "emblem": {"kind": "flame", "color": "#d8a848", "size": 7}, "clasp": "#d8a848"},
    "props": {"float": {"kind": "grimoire", "color": "#b02a1c", "glow": "#ff9a40", "length": 0.75}},
    "glow": {"color": "#ff9a40", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Crimson Lion (arc/knights/CrimsonLionCast). Flame glow on the floating grimoire only.",
}
