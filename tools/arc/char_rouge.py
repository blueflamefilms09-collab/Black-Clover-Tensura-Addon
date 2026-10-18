"""Why: Rouge (entity nusmp:rouge, thread/RougeEntity), the cat of red thread of Thread Magic ("Red Thread of Fate: Rouge"): in the anime a small red cat that sits on Vanessa's shoulder, bolts
to whoever is about to be hit and taps them so the blow never happens. The old model was a small cat sitting upright, tail curled round its paws, woven of crimson thread with gold eyes
(tools/gen_rouge_cat.py, drawn by client/thread/RougeRenderer). The art kit's version is a catgirl on the player's rig, built at a child's size (the renderer shrinks it to the 0.35 x 0.4
hitbox): crimson thread for skin (a woven look in the red tones), a short bob of thread-red hair, gold cat eyes, tall cat ears, a long tail with a curl, a loose thread-woven tunic, a gold
bell on a ribbon at the neck, bare paws. It has `sit` (the pose of the entity's SIT state), and the other states play the kit's clips: scurry and hop are walk and run, the leap and the tap are
`attack`, the unravelling is `death` and the re-knitting `spawn`. See docs/arc/remodel.md. The colours are the old model's.
"""
from remodel import lib

ENTITIES = ["rouge"]

SPEC = {
    "id": "rouge",
    "name": "Rouge",
    "role": "boss",
    "build": "child",
    "height": 1.4,
    "skin": ["#8a1424", "#b82a3a", "#e04858", "#ff8a92"],
    "face": {"expression": "cheerful", "eyes": "big", "eye_color": "#ffc820", "eye_glow": "#ffd848", "brows": "none", "mouth": "smile", "nose": "small", "lashes": True, "marks": ["blush"]},
    "hair": {"style": "bob", "color": "#c42438", "streak": "#ff6a78", "ribbon": "#ffd848"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#d83a4c", "trim": "#ffb0b8", "length": "thigh", "collar": "stand", "pattern": "stripes", "pattern_color": (255, 120, 132), "hem_trim": True},
        "legs": {"kind": "shorts", "color": "#a82034"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
        "neck": {"kind": "choker", "color": "#ffd848"},
    },
    "ears": {"kind": "cat", "color": "#c42438", "inner": "#ffb0b8", "size": 1.4},
    "tail": {"kind": "cat", "color": "#c42438", "color2": "#ff6a78", "length": 1.6},
    "accessories": [{"kind": "ribbon", "color": "#ffd848"}],
    "glow": {"color": "#ffd848", "eyes": True, "props": False, "trim": False},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "relaxed", "extra": ["spawn"]},
}


def post(kit, rig):
    """`sit`: the SIT state of the entity (it sits on a shoulder or on the ground)."""
    lib.sit_pose(rig)
