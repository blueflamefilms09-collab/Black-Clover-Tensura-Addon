"""Why: Solid Silva, the second son of the Silva family and a knight of the Silver Eagle: the sneering, slowly softening noble who gives advice "for free, and not nicely". Look (summarised
from search results, see docs/arc/npcb.md section 10; no source described his hair or his uniform): a neat, refined young man of 1.69 m with pale silver-blue hair and blue eyes in a
dark-blue noble's uniform with silver trim and the Silver Eagle's silver mantle with its wings; his Water Magic gives the blue glow of the floating grimoire. Colours are a choice.
"""
SPEC = {
    "id": "solid_silva",
    "name": "Solid Silva",
    "role": "npc",
    "build": "slim",
    "height": 1.9,
    "skin": "fair",
    "face": {"expression": "smirk", "eyes": "sharp", "eye_color": "#5a8ad0", "brows": "thin", "mouth": "smirk"},
    "hair": {"style": "side_part", "color": "#c8d4ec"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#3a4668", "trim": "#c8d4ec", "collar": "high", "buttons": True, "length": "thigh"},
        "inner": {"kind": "shirt", "color": "#e8ecf4"},
        "sleeves": {"kind": "long", "cuff": "#c8d4ec"},
        "legs": {"kind": "trousers", "color": "#2a3048"},
        "boots": {"kind": "boots", "color": "#1e2028", "height": "knee"},
        "gloves": {"kind": "gloves", "color": "#e8ecf4"},
        "belt": {"kind": "plain", "color": "#3a3e4a", "buckle": "#c8d4ec"},
    },
    "cape": {"kind": "mantle", "color": "#c8ccd8", "trim": "#8890a8", "length": 0.62, "emblem": {"kind": "wings", "color": "#4a5a8a", "size": 9}, "clasp": "#c8d4ec"},
    "props": {"float": {"kind": "grimoire", "color": "#6aa0e0", "glow": "#8ac0ff", "length": 0.78}},
    "glow": {"color": "#8ac0ff", "eyes": False, "props": True, "trim": False},
    "clips": {"cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Silver Eagle (arc/knights/SilverEagleCast). A water-blue glow on the floating grimoire only.",
}
