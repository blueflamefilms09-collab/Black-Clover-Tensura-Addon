"""Why: the painted hound of Painting Magic's Painted Menagerie (entity nusmp:painted_construct, kind BEAST: a fast hound of wet paint; its genome could give it other heads). It was a
quadruped cube model with a texture per paint; the art kit's version is a hound-folk on the player's rig, painted once in pale paint and tinted by the paint's colour by the renderer
(tools/remodel/painted.py): a lean wolf-eared, long-tailed body of brush strokes, a muzzle and claws (made in `extra`), a glowing pale eye. The genome's other heads (cat, bear, boar,
griffin, dragon) are not drawn: every beast wears the hound's. See docs/arc/remodel.md.
"""
from remodel import painted

ENTITIES = []          # drawn by the painted_construct entity through its kind (client/remodel/RemodelTable)

SPEC = painted.base(
    "painted_beast", "Painted Hound", "slim", 1.5,
    ears={"kind": "wolf", "color": "#e0e3ee", "inner": "#b4b8c8", "size": 1.6},
    tail={"kind": "wolf", "color": "#d8dbe8", "color2": "#ffffff", "length": 1.4},
    hair={"style": "shaggy", "color": "#dfe2ee"},
)
SPEC["face"]["mouth"] = "fangs"
SPEC["face"]["eyes"] = "sharp"
SPEC["outfit"]["top"] = {"kind": "vest", "color": "#dfe2ee", "trim": "#b4b8c8", "length": "hip", "collar": "fur", "collar_color": "#f4f5fa", "material": "fur"}


def extra(kit, rig):
    d = rig.d
    muz = kit.skin_paint([kit.lighten(c, 0.05) for c in rig.look.skin], salt=121)
    rig.box("head", (-2.2, d.head_y - 0.4, -8.8), (4.4, 3.4, 4.8), muz, faces=("front", "left", "right", "top", "bottom"))
    rig.box("head", (-1.1, d.head_y + 1.8, -9.3), (2.2, 1.4, 0.8), kit.solid((90, 92, 110)))
    claw = kit.bone_paint(kit.shades("#f2f3fa", 5, 1.0), 122)
    for sg, bone in ((1, "hand_l"), (-1, "hand_r")):
        for k in (-1, 0, 1):
            rig.spike(bone, (sg * d.arm_x + k * 0.9, d.wrist_y - d.hand + 0.4, -0.5), 0.8, 3.0, 180.0, 0.0, claw)
