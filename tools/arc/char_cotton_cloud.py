"""Why: Charmy's cotton cloud (entity nusmp:cotton_cloud, entity/CottonCloudEntity), the one prop most of Cotton Magic's spells use: a flat cumulus under someone's feet (the kitchen, the place a
sheep leaps out of), a ride that carries up to four riders, a cocoon of three heaps round a foe (Sheep Bondage), a puff that swells and fades where a strike lands. It is a prop, not a body, so
the art kit's version is the smallest thing that is still a character on the player's rig: the HEAD of a child of cotton (every cube but the head's is stripped in `extra`), drawn twice as
big and set at the front rim of a heap of wool puffs that the `extra` builds on its own bone (about 2.6 blocks across and 0.6 high, the cumulus the old cube model was), so the cloud is a
heap of cotton with a sleepy smiling face at its front. The old cube model (client/CottonCloudModel) stays; the renderer (client/remodel) keeps the modes: the platform and the ride draw
one cloud, the wrap three stacked ones, the burst one that swells and fades. The colours are the old texture's. See docs/arc/remodel.md.
"""
import math

from remodel import lib

ENTITIES = ["cotton_cloud"]

SPEC = {
    "id": "cotton_cloud",
    "name": "Cotton Cloud",
    "role": "npc",
    "build": "child",
    "height": 1.4,
    "skin": ["#dde2ee", "#eceff7", "#f8f9fd", "#ffffff"],
    "face": {"expression": "calm", "eyes": "smile", "eye_color": "#6a76a0", "brows": "none", "mouth": "smile", "nose": "none", "marks": ["blush"]},
    "hair": {"style": "bob", "color": "#ffffff"},
    "outfit": {"top": {"kind": "bare"}, "legs": {"kind": "bare"}, "boots": {"kind": "bare"}},
    "glow": {"color": "#ffffff", "eyes": False, "props": False, "trim": False},
    "clips": {"attack": "none", "cast": "gesture", "idle": "relaxed"},
}

FACE_SCALE = 1.6
REACH = 17.0           # how far in front of the middle of the heap the face sits, px


def extra(kit, rig):
    """The heap: three layers of wool puffs (eight round the rim, five above, two on top), each a box in the kit's fur painter, on a bone of its own that breathes (rig.animate pulse).
    Only the head and the heap stay; the rest of the child is stripped."""
    d = rig.d
    rig.bone("heap", "root", (0.0, 0.0, 0.0))
    shade = kit.shades("#f6f8fd", 5, 0.55)
    wool = [kit.fur(shade, salt=95 + k, flow="y", strand=2.4, root=0.15) for k in range(5)]
    n = 0
    for j in range(8):
        a = j * math.pi / 4.0 + 0.2
        r = 14.5 if j % 2 == 0 else 12.0
        s = 13.0 + (j % 3)
        rig.box("heap", (r * math.sin(a) - s / 2, 0.0, r * math.cos(a) - s / 2), (s, 9.0 + (j % 2), s), wool[n % 5])
        n += 1
    for j in range(5):
        a = j * 2 * math.pi / 5.0 + 0.5
        r = 7.5
        s = 12.0 + (j % 2)
        rig.box("heap", (r * math.sin(a) - s / 2, 6.0, r * math.cos(a) - s / 2), (s, 8.0, s), wool[n % 5])
        n += 1
    for j, (x, z) in enumerate(((-2.5, 1.0), (3.0, -1.5))):
        rig.box("heap", (x - 5.0, 12.0, z - 5.0), (10.0, 5.5, 10.0), wool[n % 5])
        n += 1
    rig.animate("heap", sway=0.0, pulse=0.015)
    lib.strip_cubes(rig, keep=("head", "heap"))


def post(kit, rig):
    """The face: the head bone drawn FACE_SCALE times as big and moved to the front rim of the heap at the height of its lower layer, in every clip."""
    d = rig.d
    s = FACE_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
    lib.shift(rig, "head", 0.0, 4.5 - d.head_y, -REACH)
