"""Why: Vetto, the elf in a human body who is the Third Eye's brute and the owner's reference for anti-magic against overwhelming physical stats. The model has to read as the biggest,
wildest thing in the early roster: a huge-shouldered man with a mane of hair, a sadistic grin, a painted face and claws, with the beast auras of the fight (VFX, in the boss class) added
round him. Look (facts only, from one search summary of the Black Clover wiki, not confirmed by a second source: see docs/arc/vetto.md): a tall, muscular man with enormous arms, long
blond hair, a beard, a red tattoo across the face, a sadistic grin. The mod's own additions: bare arms, a leather vest, hand wraps, a beast pelt on one shoulder, a necklace of fangs,
glowing claws on both hands. The second form is the Gates: the Mythical Beast Magic, beasts that never lived: white hair (the canon's Evil Eye turns his hair white), red eyes, the horns of a
bull and the tail of a dragon, all glowing red.
"""
SPEC = {
    "id": "vetto",
    "name": "Vetto",
    "role": "boss",
    "build": "broad",
    "height": 2.45,
    "skin": "tan",
    "face": {"expression": "mad", "eyes": "wide", "eye_color": "#c83030", "eye_glow": "#ff6040", "brows": "angry", "mouth": "grin", "marks": ["beard", "war_paint", "scar_cheek_r"]},
    "hair": {"style": "wild_mane", "color": "#d8b868"},
    "outfit": {
        "top": {"kind": "vest", "color": "#3a2418", "trim": "#8a6a48"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "baggy", "color": "#2a2420"},
        "boots": {"kind": "boots", "color": "#2a1c14", "height": "calf"},
        "gloves": {"kind": "wraps", "color": "#d8d0b4"},
        "belt": {"kind": "sash", "color": "#7a1e24", "buckle": "#c8a040"},
        "neck": {"kind": "necklace", "color": "#e8e0c8"},
    },
    "accessories": [{"kind": "shoulder_fur", "color": "#8a6a48", "color2": "#c8a878", "side": "left"}],
    "glow": {"color": "#ff8a30", "eyes": True, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "magic", "idle": "proud"},
    "form2": {
        "name": "Vetto, Mythical Beast",
        "hair": {"color": "#f0eee8"},
        "face": {"eye_color": "#ff3030", "eye_glow": "#ff5040"},
        "horns": {"kind": "bull", "color": "#3a2e28", "glow": "#e0283c", "size": 1.2},
        "tail": {"kind": "dragon", "color": "#6a2a1a", "glow": "#e0283c"},
        "glow": {"color": "#e0283c"},
    },
    "notes": "Beast Magic boss of the Seabed Temple Arc. The beast auras of the fight are effects drawn by the boss class, not parts of the model.",
}


def extra(kit, rig):
    """Three claws on each hand, pointing forward and down, glowing like the beast aura."""
    d = rig.d
    claw = kit.metal(kit.shades("#ffc27a", 5, 1.3), 71, glow=(255, 138, 48, 230), emboss=False)
    for k in range(3):
        x = d.arm_x - d.aw / 2 + (k + 0.5) * d.aw / 3
        for bone_l, bone_r in (("hand_l", "hand_r"),):
            c = rig.spike(bone_l, (x, d.wrist_y - d.hand + 0.4, -d.ad / 2 + 0.2), 0.9, 4.0, -70, 0, claw, fracs=(0.6, 0.4), taper=0.5)
            for cube in c:
                rig.pk.twin(rig.m, bone_r, cube)
