"""Why: Vanica Zogratis, the middle of the Dark Triad (entity nusmp:vanica_zogratis, entity/zogratis/VanicaBoss), the crowned blood-mage with the eyepatch. Remodelled from the old
hand-built model into the art kit's player-proportioned character (docs/arc/remodel.md): slim, a black bob under a crown, one red eye and a black eyepatch with a gold emblem,
an open smile, a belted short dress with a ring buckle, black sleeves and long black boots. `form2` is the hybrid of the Gates (100% Megicula): curse sigils burning
on the skin, horns, long red claws, and the ring of floating blood drops that the old model had as a halo. Colours are the game's reading of the design, not a trace.
"""
ENTITIES = ["vanica_zogratis"]

SPEC = {
    "id": "vanica_zogratis",
    "name": "Vanica Zogratis",
    "role": "boss",
    "build": "slim",
    "height": 1.9,
    "skin": "fair",
    "face": {"expression": "grin", "eyes": "big", "eye_color": "#d02a34", "brows": "arched", "mouth": "open", "nose": "small", "lashes": True,
             "marks": ["lipstick", "blush"]},
    "hair": {"style": "bob", "color": "#1a1418"},
    "outfit": {
        "top": {"kind": "dress", "color": "#7a1e2c", "trim": "#d8b448", "length": "thigh", "collar": "stand", "hem_trim": True,
                "emblem": {"kind": "spade", "where": "chest", "size": 6}},
        "inner": {"kind": "none"},
        "sleeves": {"kind": "long", "color": "#1a1418", "cuff": "#d8b448"},
        "legs": {"kind": "leggings", "color": "#1a1418"},
        "boots": {"kind": "boots", "color": "#1a1418", "height": "knee", "cuff": "#d8b448"},
        "gloves": {"kind": "gloves", "color": "#1a1418"},
        "belt": {"kind": "plain", "color": "#2a1a1c", "buckle": "#d8b448"},
    },
    "accessories": [{"kind": "crown", "color": "#e0b448", "color2": "#d02a34"}, {"kind": "eyepatch", "color": "#14101a", "color2": "#e0b448", "side": "left"}],
    "glow": {"color": "#ff3a4a", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "magic", "idle": "proud"},
    "form2": {
        "name": "Vanica Zogratis, Megicula",
        "skin": ["#8a3a40", "#b04a50", "#d06a68", "#e89890"],
        "face": {"eye_color": "#ff5a50", "eye_glow": "#ff3a3a", "eyes": "slit", "mouth": "fangs", "marks": ["lipstick", "veins", "cracks", "forehead_sigil"]},
        "outfit": {"top": {"color": "#5a0e1e", "trim": "#ff5a50"}, "boots": {"cuff": "#ff5a50"}, "belt": {"buckle": "#ff5a50"}},
        "horns": {"kind": "oni", "color": "#2a1014", "color2": "#8a2030", "glow": "#ff3a3a", "size": 1.1},
        "halo": {"kind": "thorns", "color": "#a01828", "glow": "#ff4a4a", "radius": 1.3},
        "props": {"float": {"kind": "orb", "color": "#c01830", "glow": "#ff3a3a"}},
        "glow": {"color": "#ff3a3a", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """Long red claws (form 2): a bone-red spike on each hand, placed from the measured body so it fits the slim build. Form 1 has none."""
    if rig.form != 2:
        return
    d = rig.d
    claw = kit.bone_paint(kit.shades("#b01828", 5, 1.2), 61)
    for sg, bone in ((1, "hand_l"), (-1, "hand_r")):
        for k in (-1, 0, 1):
            rig.spike(bone, (sg * d.arm_x + k * 0.9, d.wrist_y - d.hand + 0.6, -0.4), 0.8, 4.2, 180.0, 0.0, claw)
