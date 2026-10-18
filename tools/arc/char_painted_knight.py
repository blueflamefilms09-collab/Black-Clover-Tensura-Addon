"""Why: the painted einherjar of Painting Magic (entity nusmp:painted_construct, kind KNIGHT: "Living Illustration, Master of Valhalla": a knight of wet paint with a sword; hitbox 0.6 x 1.95).
It was the vanilla zombie model with a painted skin, and the game's iron sword drawn in its hand; the art kit's version is a knight on the player's rig, painted once in pale paint and tinted
by the paint's colour (tools/remodel/painted.py): a closed helm with a visor slit that glows, a breastplate, pauldrons and greaves of painted plate, a short cape of brush strokes, and a
broad painted sword in its hand. See docs/arc/remodel.md.
"""
from remodel import painted

ENTITIES = []

SPEC = painted.base(
    "painted_knight", "Painted Knight", "average", 1.95,
    armor={"pieces": ["helm", "breastplate", "pauldrons", "bracers", "greaves"], "style": "plate", "color": "#e8eaf4", "trim": "#c0c4d4", "glow": "#ffffff"},
    cape={"kind": "cape", "color": "#d8dbe8", "trim": "#f4f5fa", "length": 0.7, "ragged": True, "lining": "#c0c4d4"},
    props={"right": {"kind": "sword", "color": "#e8eaf4", "color2": "#c0c4d4", "glow": "#ffffff"}},
)
SPEC["outfit"]["top"] = {"kind": "tunic", "color": "#dcdfec", "trim": "#f4f5fa", "length": "thigh", "hem_trim": True}
SPEC["outfit"]["boots"] = {"kind": "boots", "color": "#c8ccdc", "height": "calf"}
SPEC["outfit"]["belt"] = {"kind": "plain", "color": "#c0c4d4", "buckle": "#ffffff"}
SPEC["clips"] = {"attack": "slash", "cast": "gesture", "idle": "proud", "extra": ["spawn"]}
