"""Why: the painted giant of Painting Magic (entity nusmp:painted_construct, kind GIANT: "a hulking painted giant whose blows fling foes", sneaking Living Illustration; hitbox 1.2 x 3.9). It
was the knight's body at twice the size; the art kit's version is a huge broad body on the player's rig, painted once in pale paint and tinted by the paint's colour
(tools/remodel/painted.py): a heavy brow, a bare chest of broad brush strokes with a loincloth and a rope belt, boulder fists, a glowing white eye. See docs/arc/remodel.md.
"""
from remodel import lib, painted

ENTITIES = []

SPEC = painted.base(
    "painted_giant", "Painted Giant", "huge", 3.9,
    widen=1.1,
    hair={"style": "crop", "color": "#dfe2ee"},
    armor={"pieces": ["pauldrons", "bracers"], "style": "bone", "color": "#e8eaf4", "trim": "#c0c4d4", "glow": "#ffffff"},
)
SPEC["face"]["brows"] = "heavy"
SPEC["face"]["eyes"] = "narrow"
SPEC["face"]["mouth"] = "line"
SPEC["outfit"]["belt"] = {"kind": "sash", "color": "#c0c4d4", "buckle": "#ffffff"}
SPEC["clips"] = {"attack": "punch", "cast": "magic", "idle": "slouch", "extra": ["spawn"]}


def post(kit, rig):
    """A man's 8 px head on a four-block giant looks like a pin: the head bone is drawn 1.8 times as big in every clip."""
    lib.scale_head(rig, 1.8, 1.8, 1.8)
