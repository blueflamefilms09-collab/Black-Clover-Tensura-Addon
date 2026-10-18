"""Why: the Basilisk (entity nusmp:basilisk, dungeon/BasiliskEntity), a tier-3 predator of the Living Dungeon: a low, heavy armoured lizard in overlapping forest-green and near-black scales,
a bone crest down the back, a wedge head, venom-green eyes and a faint green glow along the crest; its bite poisons, and it sleeps when it has eaten. The hand-built model is replaced by
the art kit's version: a creature becomes an anthropomorphic character on the player's rig with its features kept, so this is a lizard-folk serpent: a scaled humanoid body whose
legs vanish into three stacked coils of serpent body (made in `extra`), a wedge snout and a bone crest for hair, venom-green eyes, a scale cuirass over the green hide, a long
scaled tail, and fin-frill ears. It is built small so the whole stays low, like the creature (the hitbox is 1.6 x 1.0). It has the kit's clips plus `sleep` (coiled down, lib.sleep_pose).
The colours are the old model's. See docs/arc/remodel.md.
"""
import math

from remodel import lib

ENTITIES = ["basilisk"]

SPEC = {
    "id": "basilisk",
    "name": "Basilisk",
    "role": "boss",
    "build": "small",
    "height": 1.55,
    "widen": 1.25,
    "skin": ["#1c3a24", "#2a5a34", "#3e7a46", "#62a860"],
    "face": {"expression": "demon", "eyes": "slit", "eye_color": "#9aff3a", "eye_glow": "#b0ff5a", "brows": "heavy", "mouth": "fangs", "nose": "none", "marks": ["cheek_lines"]},
    "hair": {"style": "mohawk", "color": "#d8d0b4"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#2a5a34"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "plain", "color": "#16261c", "buckle": "#9aff3a"},
    },
    "armor": {"pieces": ["breastplate", "pauldrons", "bracers"], "style": "scale", "color": "#1e3a28", "trim": "#9aff3a", "glow": "#7aff3a"},
    "ears": {"kind": "fin", "color": "#3e7a46", "inner": "#9aff3a", "size": 1.3},
    "tail": {"kind": "lizard", "color": "#2a5a34", "color2": "#9aff3a", "glow": "#7aff3a", "length": 1.8},
    "glow": {"color": "#9aff3a", "eyes": True, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch", "extra": ["spawn"]},
}


def extra(kit, rig):
    """The serpent half: four stacked rings of body (each of eight blocks round a circle, radius shrinking upward) that hide the legs and read as a coil, in the green and black scales of
    the cuirass, and the wedge snout of the head. Placed from the measured body (the rings rise to the hip). The rings are on the root bone: they do not swing with the legs."""
    d = rig.d
    green = kit.scale_paint(kit.shades("#2e6a3a", 5, 1.2), salt=81, size=3)
    black = kit.scale_paint(kit.shades("#1a2c20", 5, 1.1), salt=82, size=3)
    rings = max(3, int(d.hip_y // 4.0))
    h = d.hip_y / rings + 0.3
    for k in range(rings):
        r = 8.8 - 3.0 * (k / max(1.0, rings - 1.0))
        paint = green if k % 2 == 0 else black
        for j in range(8):
            a = j * math.pi / 4.0 + k * 0.4
            cx, cz = r * math.cos(a), r * math.sin(a)
            rig.box("root", (cx - 2.4, k * (d.hip_y / rings), cz - 2.4), (4.8, h, 4.8), paint, rotation=(0, -math.degrees(a), 0), pivot=(cx, k * (d.hip_y / rings), cz), faces=("front", "back", "left", "right", "top"))
    snout = kit.scale_paint(kit.shades("#2a5a34", 5, 1.2), salt=83, size=2)
    rig.box("head", (-1.8, d.head_y - 0.9, -8.6), (3.6, 2.4, 4.6), snout, faces=("front", "left", "right", "top", "bottom"))
    rig.box("head", (-1.2, d.head_y + 0.9, -9.0), (2.4, 0.9, 0.8), kit.solid((20, 36, 24)))


def post(kit, rig):
    """`sleep`: coiled down and still, for a predator that has eaten."""
    lib.sleep_pose(rig)
