"""Why: the Blood Beast (entity nusmp:blood_beast, entity/zogratis/BloodBeastEntity), Vanica's creature of living blood: a lion-wolf with a mane of blood thorns and glowing veins that rises
out of a puddle, crouches over the telegraph of its pounce, hangs open-jawed in the leap, bites, and melts into a puddle when it dies. The hand-built model is replaced by the art kit's
version, a creature as an anthropomorphic character on the player's rig: a small, lean, crimson body of translucent-looking blood-red skin, no clothes, a mane of blood thorns for hair
(the kit's `wild_mane` in dark red with a bright streak), a lion-wolf muzzle with fangs (made in `extra`), pointed ears, a tufted tail, claws, and veins that glow along the face and
chest. It uses `spawn` (the rise from the puddle), `attack` (the bite and, held at its lunge pose by the renderer, the leap) and `death` (the collapse the renderer fades into a puddle).
The colours are the old model's. See docs/arc/remodel.md.
"""
ENTITIES = ["blood_beast"]

SPEC = {
    "id": "blood_beast",
    "name": "Blood Beast",
    "role": "boss",
    "build": "small",
    "height": 1.5,
    "widen": 1.1,
    "skin": ["#5a0e1a", "#8a1a28", "#c02a38", "#f06068"],
    "face": {"expression": "mad", "eyes": "slit", "eye_color": "#ffd0c0", "eye_glow": "#ff8070", "brows": "angry", "mouth": "fangs", "nose": "none", "marks": ["veins", "cheek_lines"]},
    "hair": {"style": "wild_mane", "color": "#6a0e1c", "streak": "#ff4050"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#7a1420"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "none"},
    },
    "armor": {"pieces": ["spikes"], "style": "bone", "color": "#a01828", "trim": "#ff4050", "glow": "#ff3040"},
    "ears": {"kind": "wolf", "color": "#a01828", "inner": "#ff4050", "size": 1.4},
    "tail": {"kind": "wolf", "color": "#8a1a28", "color2": "#ff4050", "glow": "#ff3040", "length": 1.3},
    "glow": {"color": "#ff3040", "eyes": True, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch", "extra": ["spawn"]},
}


def extra(kit, rig):
    """The lion-wolf muzzle with its pale fangs, glowing veins down the chest (a thin bright strip on the torso bone), and claws. Placed from the measured body, in the skin's own
    colours (blood, with the veins in the glow layer)."""
    d = rig.d
    skin = rig.look.skin
    muz = kit.skin_paint([kit.mix(c, (255, 160, 150), 0.18) for c in skin], salt=91)
    rig.box("head", (-2.2, d.head_y - 0.4, -8.8), (4.4, 3.4, 4.8), muz, faces=("front", "left", "right", "top", "bottom"))
    rig.box("head", (-1.1, d.head_y + 1.8, -9.3), (2.2, 1.4, 0.8), kit.solid((60, 8, 16)))
    fang = kit.bone_paint(kit.shades("#f6ecdc", 5, 1.0), 92)
    for sg in (1, -1):
        rig.box("head", (sg * 1.4 - 0.4, d.head_y - 1.6, -8.6), (0.8, 1.8, 0.8), fang)
    vein = kit.metal(kit.shades("#ff4050", 5, 1.1), 93, glow=(255, 64, 80, 220), emboss=False)
    rig.box("body", (-0.6, d.hip_y + 1.0, -d.td / 2 - 0.15), (1.2, d.sh_y - d.hip_y - 2.0, 0.3), vein, faces=("front",))
    for sg in (1, -1):
        rig.box("body", (sg * 2.2 - 0.4, d.hip_y + 3.0, -d.td / 2 - 0.15), (0.8, d.sh_y - d.hip_y - 6.0, 0.3), vein, faces=("front",))
    claw = kit.bone_paint(kit.shades("#e8d8c8", 5, 1.0), 94)
    for sg, bone in ((1, "hand_l"), (-1, "hand_r")):
        for k in (-1, 0, 1):
            rig.spike(bone, (sg * d.arm_x + k * 0.9, d.wrist_y - d.hand + 0.4, -0.5), 0.8, 3.2, 180.0, 0.0, claw)
