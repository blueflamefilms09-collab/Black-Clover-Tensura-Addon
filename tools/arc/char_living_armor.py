"""Why: the Living Armor (entity nusmp:living_armor, dungeon/LivingArmorEntity), the tier-2 armour colony of the Living Dungeon: "a humanoid mass of dark mollusk flesh wearing whatever
armour it found". It was the vanilla zombie model with a dark skin and the game's armour layer; this is the art kit's player-proportioned version of the same idea: a body of wet
dark-violet flesh (a slick, ridged skin with a pale pit of an eye) packed into mismatched scavenged plate, a dented breastplate and one big pauldron, a cracked helm that does not
close over the face, rusty greaves, and a notched sword. The armour the entity picks up is no longer drawn on it (the kit model is one piece); the painted plate stands for it. The
Floor Warden (char_floor_warden.py) is this body grown huge on the Spire's magicules. See docs/arc/remodel.md. A game design, not canon.
"""
ENTITIES = ["living_armor"]

SPEC = {
    "id": "living_armor",
    "name": "Living Armor",
    "role": "boss",
    "build": "broad",
    "height": 1.95,
    "skin": ["#32283a", "#483c52", "#62546e", "#7e7090"],
    "face": {"expression": "mad", "eyes": "glow", "eye_color": "#e8f0c8", "eye_glow": "#d8f0a0", "brows": "heavy", "mouth": "fangs", "nose": "none", "marks": ["veins", "cracks"]},
    "hair": {"style": "bald", "color": "#32283a"},
    "outfit": {
        "top": {"kind": "tank", "color": "#4a3c52", "trim": "#6a5a74", "length": "hip", "ragged": True, "pattern": "patches", "pattern_color": (88, 72, 98), "material": "leather"},
        "inner": {"kind": "bare"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "trousers", "color": "#3a2e44"},
        "boots": {"kind": "boots", "color": "#5a4a40", "height": "knee"},
        "gloves": {"kind": "gauntlets", "color": "#7a7068"},
        "belt": {"kind": "plain", "color": "#4a3a30", "buckle": "#a89a7a"},
    },
    "armor": {"pieces": ["breastplate", "pauldrons", "greaves"], "style": "plate", "color": "#8e8a84", "trim": "#a8683a"},
    "props": {"right": {"kind": "sword", "color": "#8a8a90", "color2": "#5a4030", "glow": "#d8f0a0"}},
    "glow": {"color": "#d8f0a0", "eyes": True, "props": False, "trim": False},
    "clips": {"attack": "slash", "cast": "gesture", "idle": "slouch"},
}
