"""Why: the Amphis Baena (Amphisbaena), the Monster Rex of the 27th floor of the Dungeon in DanMachi, the Water City: a huge two-headed white water dragon whose one head
breathes blue flame that burns through steel and whose other head breathes a red mist that dissipates magic. Look, from the series (facts only, see docs/arc/amphisbaena.md):
a gigantic two-headed white dragon with a long body and fins. The kit builds only humanoid bodies, so it is an upright, widened, scaled `huge` humanoid with a dragon's tail,
fins for ears and horns; the second (red) head grows out of the right shoulder on its own bone, made in `extra`, and both heads are drawn bigger in `post`. Added for the game,
not from the source: the light plate armour with blue edges and the colour of the two heads' eyes (blue for the flame, red for the mist). `form2` is the leveled form: the blue flame
spreads into a ring of flames over the body.
"""
HEAD_SCALE = 1.4

SPEC = {
    "id": "amphisbaena",
    "name": "Amphisbaena",
    "role": "boss",
    "build": "huge",
    "height": 4.1,
    "widen": 1.2,
    "skin": ["#8eaac2", "#b0cade", "#d0e2ef", "#ecf5fb"],
    "face": {"expression": "demon", "eyes": "slit", "eye_color": "#4ab0ff", "eye_glow": "#7ad4ff", "brows": "angry", "mouth": "fangs", "nose": "none"},
    "hair": {"style": "bald", "color": "#d4e6f2"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#a8c4d8"},
        "boots": {"kind": "bare"},
    },
    "armor": {"pieces": ["pauldrons", "bracers", "greaves", "gorget"], "style": "plate", "color": "#dfeaf4", "trim": "#3a8ae0", "glow": "#4ab0ff"},
    "horns": {"kind": "devil", "color": "#c8d8e8", "size": 1.2},
    "ears": {"kind": "fin", "color": "#3a8ae0", "size": 1.5},
    "tail": {"kind": "dragon", "color": "#b0cade", "length": 1.8},
    "glow": {"color": "#4ab0ff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "slouch", "extra": ["spawn"]},
    "form2": {
        "name": "Amphisbaena, Leveled",
        "face": {"eye_color": "#9ad8ff", "eye_glow": "#bfe8ff"},
        "armor": {"trim": "#9ad8ff", "glow": "#bfe8ff"},
        "halo": {"kind": "flames", "color": "#3a8aff", "glow": "#7ac8ff", "radius": 1.6},
        "glow": {"color": "#7ac8ff", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The two muzzles, and the second (red) head on its own neck bone beside the first. The main head gets a dragon's muzzle; the red head is as big as the main one
    (the post hook draws the main head HEAD_SCALE times as big, so the red one is built at that size at once), stands on a neck bone `head2` that rises from the shoulder
    and sways like cloth (rig.animate), and has a muzzle, horns, fangs and red eyes that glow. Placed from rig.d, so it fits both forms."""
    d = rig.d
    skin = rig.look.skin
    blue = [kit.mix(c, (110, 170, 230), 0.25) for c in skin]
    red = [kit.mix(c, (214, 58, 56), 0.72) for c in skin]
    rig.box("head", (-2.4, d.head_y + 0.2, -8.2), (4.8, 3.2, 4.4), kit.skin_paint(blue, salt=81), faces=("front", "left", "right", "top", "bottom"))
    size = 8.0 * HEAD_SCALE
    cx = -(d.tw / 2 + size / 2 - 2.2)                                      # the red head's centre: -x is the model's right, so it stands on the viewer's left
    rig.bone("head2", "body", (cx, d.sh_y, 0.0))
    rig.box("head2", (cx - 3.4, d.sh_y - 1.5, -3.4), (6.8, 5.5, 6.8), kit.skin_paint(red, salt=82))
    y0 = d.sh_y + 3.0

    def head2(face, i, j, fw, fh):
        base = kit.skin_paint(red, salt=83)(face, i, j, fw, fh)
        if face == "front":
            if fh * 0.30 <= j <= fh * 0.46 and (fw * 0.14 <= i <= fw * 0.36 or fw * 0.64 <= i <= fw * 0.86):
                return kit.px((255, 70, 60)), kit.px((255, 90, 70))       # the eyes, glowing
            if fh * 0.30 <= j <= fh * 0.34 and (fw * 0.10 <= i <= fw * 0.40 or fw * 0.60 <= i <= fw * 0.90):
                return kit.px((60, 16, 20)), None                          # the angry brows
        return base

    rig.box("head2", (cx - size / 2, y0, -size / 2), (size, size, size), head2, dens={"front": 4}, faces=("front", "left", "right", "back", "top", "bottom"))

    def muzzle(face, i, j, fw, fh):
        base = kit.skin_paint(red, salt=84)(face, i, j, fw, fh)
        if face == "front":
            if fh * 0.55 <= j <= fh * 0.85 and (i % 4 == 1) and fw * 0.15 < i < fw * 0.85:
                return kit.px((250, 246, 238)), None                       # the fangs
            if fh * 0.2 <= j <= fh * 0.4 and (abs(i - fw * 0.28) < 1.0 or abs(i - fw * 0.72) < 1.0):
                return kit.px((70, 20, 22)), None                          # the nostrils
        return base

    rig.box("head2", (cx - 3.4, y0 + 0.6, -size / 2 - 3.8), (6.8, 4.6, 3.9), muzzle, dens={"front": 4}, faces=("front", "left", "right", "top", "bottom"))
    horn = kit.bone_paint(kit.shades("#e8dcd0", 5, 1.1), 85)
    for sg in (1, -1):
        rig.spike("head2", (cx + sg * 3.6, y0 + size - 0.4, -0.5), 2.6, 5.5, -8.0, -sg * 22.0, horn)
    rig.animate("head2", sway=0.5, phase=1.3)


def post(kit, rig):
    """Both heads are bigger than the kit's 8 px head: a steady scale of the head bones in every clip."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
