"""Why: Dante Zogratis, the eldest of the Dark Triad of the Spade Kingdom (entity nusmp:dante_zogratis, entity/zogratis/DanteBoss): a crowned, broad-shouldered Body Magic
brawler. It was a hand-built cube model; the owner wants every mob of the mod built like the Old Witch, on the player's proportions with the art kit, so this is the same man
rebuilt: from the old design (and the manga) a swept-back dark head of hair, a crown, scars crossed on the face, a goatee, green eyes, a brown tunic and the red scarf. `form2`
is the hybrid the Gates open (100% Lucifero: violet skin, horns through the crown, glowing violet eyes, the gravity halo), the model the old renderer called dante_hybrid.
The look is the game's own reading of the design, not a trace. See docs/arc/remodel.md.
"""
from remodel import lib

ENTITIES = ["dante_zogratis"]

SPEC = {
    "id": "dante_zogratis",
    "name": "Dante Zogratis",
    "role": "boss",
    "build": "broad",
    "height": 2.0,
    "skin": "tan",
    "face": {"expression": "grin", "eyes": "sharp", "eye_color": "#4ab84e", "brows": "thick", "mouth": "grin", "nose": "wide",
             "marks": ["scar_cheek_l", "scar_diag_r", "goatee", "stubble"]},
    "hair": {"style": "slick", "color": "#241812"},
    "outfit": {
        "top": {"kind": "tunic", "color": "#8a5a34", "trim": "#d0a860", "length": "thigh", "collar": "stand", "pattern": "patches", "pattern_color": (122, 78, 44),
                "hem_trim": True},
        "inner": {"kind": "shirt", "color": "#d8c8a8"},
        "sleeves": {"kind": "short", "cuff": "#d0a860"},
        "legs": {"kind": "trousers", "color": "#4a3828"},
        "boots": {"kind": "boots", "color": "#3a2a20", "height": "calf", "cuff": "#d0a860"},
        "gloves": {"kind": "wraps", "color": "#d8c8a8"},
        "belt": {"kind": "double", "color": "#3a2418", "buckle": "#d0a860"},
    },
    "accessories": [{"kind": "crown", "color": "#e0b448", "color2": "#c8322e"}],
    "glow": {"color": "#9a5cff", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "magic", "idle": "proud"},
    "form2": {
        "name": "Dante Zogratis, Lucifero",
        "skin": ["#5a3a78", "#7a52a0", "#9a6cc0", "#b890d8"],
        "face": {"eye_color": "#c8a0ff", "eye_glow": "#b888ff", "eyes": "slit", "mouth": "fangs", "marks": ["scar_cheek_l", "scar_diag_r", "veins", "cracks"]},
        "hair": {"color": "#161020"},
        "outfit": {"top": {"trim": "#c8a0ff", "color": "#6a4a58"}, "neck": {"color": "#a8302e"}},
        "horns": {"kind": "devil", "color": "#2a1e34", "color2": "#6a48a0", "glow": "#b888ff", "size": 1.25},
        "halo": {"kind": "broken", "color": "#6a48a0", "glow": "#c8a0ff", "radius": 1.25},
        "glow": {"color": "#b888ff", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The red scarf: a ring round the neck and two tails down the back that swing (tools/remodel/lib.py). Its colour follows the form."""
    lib.scarf(kit, rig, "#c8322e" if rig.form == 1 else "#a8302e")
