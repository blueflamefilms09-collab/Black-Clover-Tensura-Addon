"""Why: the Devil Avatar, the floor boss of the Underworld Gate (entity nusmp:devil_avatar, entity/avatar/DevilAvatarBoss): a high devil of the Spade Kingdom's Underworld, tall
(3.2 blocks to the tips of its horns), gaunt, horned, with long clawed arms, a tattered cloak, a thin tail that ends in a spade, a halo of gravity sigils that turns behind the
shoulders, glowing eyes and glowing cracks. The old hand-built model is replaced by the art kit's player-proportioned character (docs/arc/remodel.md): a lanky body, devil horns, a
ragged hooded cloak, claws made in `extra`, the spade-tailed devil tail and the turning halo. `form2` is the Gates: longer horns with a second pair, spines on the back, a bigger
halo and the floating sigil tablets that circle it, more cracks. Two skins: the Avatar of Lucifero (violet) and of Megicula (crimson), the second a texture-only variant
(VARIANTS, read by tools/remodel/gen_remodel.py). Colours are the game's: kept light enough to read in a dim hall, the black of the cloak is a deep violet-grey.
"""
import math

ENTITIES = ["devil_avatar"]

LUCIFERO = {"skin": ["#3a3050", "#54486e", "#6c6090", "#8a7cb0"], "cloak": "#34284e", "trim": "#a878ff", "glow": "#b890ff", "horn": "#1e1830", "horn2": "#6a4aa8", "plate": "#4a3c70"}
MEGICULA = {"skin": ["#4a2830", "#683840", "#884a52", "#a86a6e"], "cloak": "#481e2c", "trim": "#ff5a5a", "glow": "#ff6a6a", "horn": "#26101a", "horn2": "#a02a3a", "plate": "#6a2a38"}


def spec(t):
    return {
        "id": "devil_avatar",
        "name": "Devil Avatar",
        "role": "boss",
        "build": "lanky",
        "height": 2.85,
        "skin": t["skin"],
        "face": {"expression": "demon", "eyes": "slit", "eye_color": t["glow"], "eye_glow": t["glow"], "brows": "angry", "mouth": "fangs", "nose": "none",
                 "marks": ["cracks", "veins"]},
        "hair": {"style": "bald", "color": "#14101c"},
        "outfit": {
            "top": {"kind": "tabard", "color": t["plate"], "trim": t["trim"], "length": "ankle", "collar": "wide", "ragged": True, "hem_trim": True, "pattern": "runes",
                    "pattern_color": tuple(int(t["trim"][i:i + 2], 16) for i in (1, 3, 5)), "emblem": {"kind": "spade", "where": "chest", "size": 7, "glow": t["glow"]}},
            "inner": {"kind": "bare"},
            "sleeves": {"kind": "none"},
            "legs": {"kind": "trousers", "color": t["plate"]},
            "boots": {"kind": "boots", "color": t["horn"], "height": "knee", "cuff": t["trim"]},
            "gloves": {"kind": "gauntlets", "color": t["horn"]},
            "belt": {"kind": "plain", "color": t["horn"], "buckle": t["trim"]},
        },
        "cape": {"kind": "tattered", "color": t["cloak"], "trim": t["trim"], "length": 1.0, "width": 1.2, "ragged": True, "hood": False, "lining": t["plate"]},
        "armor": {"pieces": ["pauldrons", "bracers"], "style": "bone", "color": "#cfc6dc", "trim": t["trim"], "glow": t["glow"]},
        "horns": {"kind": "devil", "color": t["horn"], "color2": t["horn2"], "glow": t["glow"], "size": 1.3},
        "ears": {"kind": "pointed", "size": 1.2},
        "tail": {"kind": "devil", "color": t["horn"], "color2": t["trim"], "glow": t["glow"], "length": 1.5},
        "halo": {"kind": "gears", "color": t["horn2"], "glow": t["glow"], "radius": 1.3},
        "glow": {"color": t["glow"], "eyes": True, "props": True, "trim": True},
        "clips": {"attack": "punch", "cast": "magic", "idle": "proud"},
        "form2": {
            "name": "Devil Avatar, Gates Open",
            "horns": {"size": 1.7, "color2": t["trim"]},
            "armor": {"pieces": ["pauldrons", "bracers", "spikes", "gorget"], "trim": t["glow"]},
            "halo": {"kind": "flames", "radius": 1.8},
            "face": {"marks": ["cracks", "veins", "forehead_sigil", "cheek_lines"]},
            "cape": {"width": 1.5},
            "glow": {"color": t["glow"], "eyes": True, "props": True, "trim": True},
        },
    }


SPEC = spec(LUCIFERO)

VARIANTS = {
    "megicula": lambda: spec(MEGICULA),
    "megicula_form2": (lambda: spec(MEGICULA), 2),
}


def extra(kit, rig):
    """Claws on the long hands, a second pair of horns (the Gates: longer horns with a second pair), and the Gates' floating sigil tablets (three plates that circle the Avatar on a
    spinning bone, rig.animate(spin=...)). Everything is placed from the measured body and from the spec's own colours, so both skins and both forms use it."""
    d = rig.d
    S = rig.spec
    claw = kit.bone_paint(kit.shades(S["armor"]["color"], 5, 1.1), 41)
    for sg, bone in ((1, "hand_l"), (-1, "hand_r")):
        for k in (-1, 0, 1):
            rig.spike(bone, (sg * d.arm_x + k * 1.0, d.wrist_y - d.hand + 0.4, -0.4), 0.9, 4.6, 180.0, 0.0, claw)
    if rig.form != 2:
        return
    horn = kit.bone_paint(kit.shades(S["horns"]["color"], 5, 1.0), 43)
    for sg in (1, -1):
        rig.spike("head", (sg * 3.0, d.head_y + 7.0, -1.0), 1.6, 8.0, 18.0, -sg * 38.0, horn)           # the second pair of horns
    glow = tuple(int(S["glow"]["color"][i:i + 2], 16) for i in (1, 3, 5))
    tab = kit.metal(kit.shades(S["horns"]["color2"], 5, 1.1), 47, glow=glow + (210,), emboss=False)
    rig.bone("tablets", "body", (0.0, d.sh_y, 0.0))
    r = d.tw * 1.5 + 8.0
    for k in range(3):
        a = k * 2.0944
        rig.box("tablets", (r * math.sin(a) - 2.0, d.sh_y - 2.0 + 3.0 * (k % 2), r * math.cos(a) - 0.5), (4.0, 6.0, 1.0), tab, rotation=(0, math.degrees(a), 0), pivot=(r * math.sin(a), d.sh_y, r * math.cos(a)))
    rig.animate("tablets", spin=0.12, spin_axis=1)
