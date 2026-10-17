"""Why: Udaeus, the Monster Rex of the 37th floor of the Dungeon in DanMachi, the White Palace: a giant skeleton on the throne room's floor with a great black sword, a
roar that raises skeleton warriors, ribs that move to block blows, and its magic stone behind the sternum. Look, from the series (facts only, see docs/arc/udaeus.md): a huge
white skeleton, bare bone, a great black sword. Added for the game, not from the source: the tattered dark cloak, the pauldrons and greaves of bone, the violet light in
the sword, the eye holes and the stone. The kit builds only humanoid bodies, so it is a tall `huge` humanoid in bone with the kit's skull mask; the rib cage and the
magic stone are made in `extra`. `form2` is the leveled form: the sword wakes (it glows), the bone darkens and a broken halo hangs over it.
"""
HEAD_SCALE = 1.5

SPEC = {
    "id": "udaeus",
    "name": "Udaeus",
    "role": "boss",
    "build": "lanky",
    "height": 3.9,
    "widen": 1.0,
    "skin": ["#8e8874", "#bdb7a0", "#e0dac6", "#f6f2e4"],
    "face": {"expression": "dead", "eye_color": "#9ad8ff", "eye_glow": "#9ad8ff",
             "mask": {"kind": "skull", "color": "#ece6d2", "color2": "#14101a", "eye_glow": "#9ad8ff"}},
    "hair": {"style": "bald", "color": "#9a927a"},
    "outfit": {
        "top": {"kind": "tank", "color": "#13101a"},
        "inner": {"kind": "none"},
        "legs": {"kind": "bare"},
        "boots": {"kind": "bare"},
        "belt": {"kind": "plain", "color": "#241e2c", "buckle": "#8a7aa8"},
    },
    "armor": {"pieces": ["pauldrons", "gorget", "greaves"], "style": "bone", "color": "#d8d0b4", "trim": "#6a5a8a", "glow": "#9a8aff"},
    "cape": {"kind": "tattered", "color": "#1c1822", "length": 0.85, "ragged": True, "lining": "#2a2234"},
    "props": {"right": {"kind": "greatsword", "color": "#17131f", "color2": "#6a5a8a", "glow": "#8a6aff", "length": 0.55}},
    "glow": {"color": "#a89aff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "overhead", "cast": "magic", "idle": "relaxed", "extra": ["spawn"]},
    "form2": {
        "name": "Udaeus, Leveled",
        "skin": ["#5a5444", "#827c68", "#aaa492", "#d0cab6"],
        "face": {"eye_color": "#d8a0ff", "eye_glow": "#d8a0ff", "mask": {"kind": "skull", "color": "#c4bea8", "color2": "#0c0a12", "eye_glow": "#d8a0ff"}},
        "armor": {"color": "#a8a08c", "trim": "#8a5ac8", "glow": "#c8a0ff"},
        "props": {"right": {"color": "#0c0a12", "color2": "#8a5ac8", "glow": "#c8a0ff"}},
        "halo": {"kind": "broken", "color": "#2a2236", "glow": "#9a6aff", "radius": 1.5},
        "glow": {"color": "#c8a0ff", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The rib cage on the front of the torso (six pairs of ribs that curve back round the sides, a breastbone, collar bones, a spine of vertebrae down the back) and
    the magic stone, a glowing violet gem behind the sternum. Placed from rig.d so it fits both forms."""
    d = rig.d
    bone = kit.bone_paint(kit.shades("#e8e2cc", 5, 1.1), 71)
    dark = kit.bone_paint(kit.shades("#b8b09a", 5, 1.1), 72)
    stone = kit.metal(kit.shades("#b89aff", 5, 1.2), 73, glow=(184, 150, 255, 255), emboss=False)
    tw, td = d.tw, d.td
    zf = -td / 2 - 0.7
    top = d.sh_y - 2.6
    rows = 6
    gap = max(2.0, (d.torso_h - 6.0) / rows)
    for k in range(rows):
        y = top - k * gap
        wr = tw / 2 - 0.4 - 0.18 * k
        rig.pair("body", "body", (0.9, y, zf), (max(1.0, wr - 0.9), 1.0, 1.1), bone)                 # the front arc of a rib, with a gap for the breastbone
        rig.pair("body", "body", (wr - 0.1, y, zf + 1.1), (1.0, 1.0, td * 0.55), dark)               # the side of the rib, curving back
    rig.box("body", (-0.8, top - (rows - 1) * gap - 1.0, zf - 0.1), (1.6, (rows - 1) * gap + 2.6, 1.0), bone)       # the breastbone
    rig.pair("body", "body", (0.4, d.sh_y - 1.2, zf + 0.1), (tw / 2 - 0.4, 0.8, 1.0), bone)                           # the collar bones
    rig.box("body", (-1.2, top - 2.2 * gap, zf + 0.5), (2.4, 2.4, 1.2), stone)                                       # the magic stone, behind the sternum
    for k in range(8):                                                                                              # the spine, down the back
        rig.box("body", (-1.0, d.hip_y + 1.0 + k * (d.torso_h - 2.0) / 8.0, td / 2 - 0.2), (2.0, 1.2, 1.2), dark)


def post(kit, rig):
    """A skeleton giant's skull is bigger than the kit's 8 px head: a steady scale of the head bone in every clip (the mask and its eye lights ride on it)."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
