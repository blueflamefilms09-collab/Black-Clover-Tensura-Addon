"""Why: the Tower Wizard (entity nusmp:tower_wizard, wizard/TowerWizardEntity), the keeper of a Grimoire Tower who sets the questionnaire that decides a player's magic. It was a hand-built
cube model of the owner's own design (docs/attributes/art_reference/tower_wizard_design.png) and the Old Witch's cousin in spirit, so it moves to the art kit as the same design: an old
wizard in a very wide-brimmed, tall, floppy-tipped brown hat, a long grey-white beard and bushy brows over tired, kind eyes, a brown-mauve robe with wide sleeves and a belt, and a
glowing green orb held in the palm at chest height. It is an NPC (the `cast` clip is its talking clip). Colours follow the owner's design image as the old generator read it.
See docs/arc/remodel.md.
"""
ENTITIES = ["tower_wizard"]

SPEC = {
    "id": "tower_wizard",
    "name": "Tower Wizard",
    "role": "npc",
    "build": "average",
    "height": 1.9,
    "skin": "light",
    "face": {"expression": "tired", "eyes": "tired", "eye_color": "#5a7a58", "brows": "heavy", "brow_color": "#d8d4d0", "mouth": "none", "nose": "wide",
             "marks": ["eye_bags", "cheek_lines"]},
    "hair": {"style": "wizard", "color": "#d6d2d4", "streak": "#a8a4aa"},
    "outfit": {
        "top": {"kind": "robe", "color": "#79565e", "trim": "#b29270", "length": "floor", "collar": "stand", "collar_color": "#62444e", "hem_trim": True,
                "pattern": "patches", "pattern_color": (98, 70, 84)},
        "inner": {"kind": "shirt", "color": "#62464c"},
        "sleeves": {"kind": "wide", "cuff": "#b29270"},
        "legs": {"kind": "trousers", "color": "#4a3640"},
        "boots": {"kind": "shoes", "color": "#4a3828"},
        "gloves": {"kind": "none"},
        "belt": {"kind": "plain", "color": "#7a5a3a", "buckle": "#b29270", "pouches": True},
    },
    "accessories": [{"kind": "wizard_hat", "color": "#7a5a44", "color2": "#b29270"}],
    "props": {"right": {"kind": "orb", "color": "#4ab86a", "glow": "#6aff90"}},
    "glow": {"color": "#6aff90", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "cast_jab", "cast": "gesture", "idle": "relaxed"},
}


def extra(kit, rig):
    """The long grey-white beard of the design: three stepped tufts that hang from the chin over the chest, on the head bone (they nod with it) and swaying a little in every clip.
    Placed from the measured body so it clears the torso."""
    d = rig.d
    grey = kit.shades("#d8d4d6", 5, 0.9)
    fur = kit.fur(grey, salt=77, flow="y", strand=1.6, root=0.4)
    rig.box("head", (-3.4, d.head_y - 0.6, -4.9), (6.8, 3.4, 1.6), fur, faces=("front", "left", "right", "bottom"))
    rig.bone("beard", "head", (0.0, d.head_y, -4.0))
    rig.box("beard", (-3.0, d.head_y - 5.4, -d.td / 2 - 1.4), (6.0, 5.0, 1.6), fur, faces=("front", "left", "right", "bottom"))
    rig.box("beard", (-2.0, d.head_y - 9.4, -d.td / 2 - 1.2), (4.0, 4.2, 1.4), fur, faces=("front", "left", "right", "bottom"))
    rig.animate("beard", sway=0.8)
