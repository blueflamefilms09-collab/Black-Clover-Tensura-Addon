"""Why: the Minotaur, the Level 2 monster that hunts Bell Cranel on the 5th floor of the Dungeon in DanMachi (and, when the party is strong enough, comes back at the
Gates of the fight as Asterius, the one-horned): the first monster of the DanMachi saga. Look, from the series (facts only, see docs/arc/minotaur.md): a bull-headed
brute about two and a half metres tall, a dark brown hide, big curved horns, a heavy brow, a bare muscular chest and fists like hammers. The kit builds only humanoid
bodies, so it is a huge humanoid with a bull's head made in `extra` (muzzle, nose ring, brow ridge) and `post` (the head is drawn a third bigger than a man's) and the
kit's own bull horns. Added for the game, not from the source: the gold nose ring, the wraps on the wrists, the glowing eyes. `form2` is Asterius: the same body in a
black hide with ONE whole horn and the broken stump of the other (made in `extra`), red eyes and veins that glow, the rebirth that the Level-Up of the Dungeon Level shows.
"""
HEAD_SCALE = 1.38

SPEC = {
    "id": "minotaur",
    "name": "Minotaur",
    "role": "boss",
    "build": "huge",
    "height": 3.1,
    "widen": 1.05,
    "skin": ["#3a2014", "#54301e", "#703f28", "#8e5538"],
    "face": {"expression": "angry", "eyes": "fierce", "eye_color": "#d83a2a", "eye_glow": "#ff5a3a", "brows": "heavy", "mouth": "fangs", "nose": "wide",
             "marks": ["scar_brow_r"]},
    "hair": {"style": "short", "color": "#241209"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#3a2616"},
        "boots": {"kind": "bare"},
        "gloves": {"kind": "wraps", "color": "#6a5640"},
        "belt": {"kind": "plain", "color": "#2a1a10", "buckle": "#c8a040"},
    },
    "horns": {"kind": "bull", "color": "#e6dcc0", "color2": "#8a7a58", "size": 1.35},
    "ears": {"kind": "pointed", "size": 1.3},
    "tail": {"kind": "rat", "color": "#3a2214", "length": 1.0},
    "accessories": [{"kind": "shoulder_fur", "color": "#3a2214"}],
    "glow": {"color": "#ff5a3a", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "slouch", "extra": ["spawn"]},
    "form2": {
        "name": "Asterius",
        "skin": ["#120a0c", "#22141a", "#341e24", "#4c2c34"],
        "face": {"eye_color": "#ff3a2a", "eye_glow": "#ff6a4a", "marks": ["scar_brow_r", "veins"]},
        "hair": {"color": "#0c0708"},
        "horns": None,
        "outfit": {"legs": {"color": "#1c1012"}, "gloves": {"color": "#4a4034"}},
        "accessories": [{"kind": "shoulder_fur", "color": "#1c1012"}],
        "glow": {"color": "#ff4a30", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The bull's face: a muzzle with nostrils, a gold nose ring and a heavy brow ridge on the head bone. Built from the kit's painters and placed from rig.d
    (never from fixed numbers), so the same extra fits the black form."""
    d = rig.d
    skin = rig.look.skin
    fur_cols = [kit.darken(c, 0.15) for c in skin]
    snout = [kit.mix(c, (150, 104, 92), 0.5) for c in skin]

    def muzzle(face, i, j, fw, fh):
        base = kit.skin_paint(snout, salt=21)(face, i, j, fw, fh)
        if face == "front":
            cx = fw / 2.0
            if fh * 0.45 <= j <= fh * 0.8 and (abs(i - (cx - fw * 0.22)) < 1.0 or abs(i - (cx + fw * 0.22)) < 1.0):
                return kit.px(kit.darken(skin[0], 0.55)), None            # the nostrils
        return base

    rig.box("head", (-2.8, d.head_y - 0.6, -8.6), (5.6, 3.6, 4.8), muzzle, dens={"front": 4}, faces=("front", "left", "right", "top", "bottom"))
    ring = kit.metal(kit.shades("#e4b454", 5, 1.2), 41, emboss=False)
    rig.box("head", (-1.9, d.head_y - 3.0, -8.5), (0.5, 2.6, 0.5), ring)
    rig.box("head", (1.4, d.head_y - 3.0, -8.5), (0.5, 2.6, 0.5), ring)
    rig.box("head", (-1.9, d.head_y - 3.5, -8.5), (3.8, 0.5, 0.5), ring)
    brow = kit.fur(fur_cols, salt=23, flow="y", strand=1.6)
    rig.box("head", (-4.7, d.head_y + 5.6, -4.9), (9.4, 1.5, 1.4), brow, faces=("front", "left", "right", "top", "bottom"))
    if rig.form == 2:
        # Asterius: the first form's horns are the kit's pair; the second form has none from the spec and grows ONE long bull horn (on the model's left) and the broken
        # stump of the other on its own bone, built with the kit's own horn chain so it looks like the pair did
        top = d.head_y + 8.0
        rig.bone("horns", "head", (0.0, top - 1.0, 0.0))
        whole = kit.bone_paint(kit.shades("#c8bca0", 5, 1.1), 141)
        broken = kit.bone_paint(kit.shades("#8a7a5c", 5, 1.2), 142)
        kit.accessories._horn_chain(rig, "horns", (4.2, top - 1.5, -1.0), 1, [(3.6, 60, 6), (4.0, 40, 8), (3.8, 30, 10), (3.0, 24, 14)], 3.3, whole)
        kit.accessories._horn_chain(rig, "horns", (-4.2, top - 1.5, -1.0), -1, [(2.4, 60, 6)], 3.3, broken)


def post(kit, rig):
    """A bull's head is bigger than a man's and the kit's head is always 8 px: every clip carries a steady scale of the head bone (the horns, ears, hair and the face
    ride on it), so the head, the horns and the muzzle are drawn HEAD_SCALE times as big about the neck."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
