"""Why: the Black Goliath, the irregular Monster Rex that the Dungeon sent against Hestia's power on the 17th and 18th floors of DanMachi (anime episode 13, volume 5 of
the novels): a Goliath, the stone-skinned giant boss of the 17th floor, but black, faster and much stronger, with a roar that calls monsters. Look, from the series
(facts only, see docs/arc/black_goliath.md): a towering giant with a huge frame, a hide black as basalt, a heavy jaw with tusks, and nothing but a loincloth. Added for the
game, not from the source: the cracks of ember light that run over the hide (the sign of the Dungeon's anger), the plates of black stone armour with glowing edges, the
crest of black stone spikes on the head. The kit builds only humanoid bodies, so it is a huge, widened humanoid with a bigger head made in `post` and a jaw and a crest made in `extra`. `form2` is the leveled form:
the cracks flare and a ring of flames crowns it.
"""
HEAD_SCALE = 1.65

SPEC = {
    "id": "black_goliath",
    "name": "Black Goliath",
    "role": "boss",
    "build": "huge",
    "height": 3.9,
    "widen": 1.3,
    "skin": ["#0d0b11", "#1a1620", "#2a2430", "#3e3648"],
    "face": {"expression": "angry", "eyes": "glow", "eye_color": "#ff6a3a", "eye_glow": "#ff7a40", "brows": "heavy", "mouth": "fangs", "nose": "wide",
             "marks": ["cracks"]},
    "hair": {"style": "bald", "color": "#0d0b11"},
    "outfit": {
        "top": {"kind": "bare"},
        "legs": {"kind": "shorts", "color": "#1c1620"},
        "boots": {"kind": "bare"},
        "belt": {"kind": "plain", "color": "#14101a", "buckle": "#c8502a"},
    },
    "armor": {"pieces": ["pauldrons", "bracers", "greaves"], "style": "plate", "color": "#221e2a", "trim": "#e0502a", "glow": "#ff6a3a"},
    "glow": {"color": "#ff6a3a", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "slouch", "extra": ["spawn"]},
    "form2": {
        "name": "Black Goliath, Leveled",
        "skin": ["#120a0e", "#241218", "#381c22", "#522830"],
        "face": {"eye_color": "#ffb060", "eye_glow": "#ffc070"},
        "armor": {"color": "#2c1c24", "trim": "#ffb060", "glow": "#ffb060"},
        "halo": {"kind": "flames", "color": "#ff7a3a", "glow": "#ffb060", "radius": 1.5},
        "glow": {"color": "#ffb060", "eyes": True, "props": True, "trim": True},
    },
}


def _crack(kit, rig, bone, x, y, z, steps, seed, flip=1):
    """One jagged crack of ember light: small glowing boxes in a staircase from (x, y) downwards on the face z of the bone. Deterministic from `seed`."""
    paint = kit.solid((255, 120, 56), g=(255, 130, 64, 255))
    hot = kit.solid((255, 214, 120), g=(255, 220, 130, 255))
    s = seed
    cx, cy = x, y
    for k in range(steps):
        s = (s * 1103515245 + 12345) & 0x7FFFFFFF
        dy = 1.5 + (s % 3) * 0.5
        s = (s * 1103515245 + 12345) & 0x7FFFFFFF
        dx = ((s % 5) - 2) * 0.5 * flip
        rig.box(bone, (cx, cy - dy, z), (0.5 if k % 3 else 1.0, dy, 0.5), hot if k % 4 == 0 else paint)
        cx += dx
        cy -= dy


def extra(kit, rig):
    """The jaw with its tusks on the head bone and the cracks of ember light over the hide (chest, belly, both arms, both thighs), all placed from rig.d so they
    fit both forms."""
    d = rig.d
    skin = rig.look.skin
    stone = kit.skin_paint([kit.mix(c, (20, 16, 24), 0.25) for c in skin], salt=31)

    def jaw(face, i, j, fw, fh):
        base = stone(face, i, j, fw, fh)
        if face == "front":
            if j < fh * 0.14 and fw * 0.08 < i < fw * 0.92:
                return kit.px((255, 118, 52)), kit.px((255, 138, 64))             # the mouth, lit from within
            if fh * 0.14 <= j < fh * 0.42 and (i % 5) in (1, 2) and fw * 0.1 < i < fw * 0.9:
                return kit.px((236, 228, 206)), None                               # the teeth
        return base

    rig.box("head", (-3.4, d.head_y - 1.2, -6.4), (6.8, 2.8, 2.6), jaw, dens={"front": 4}, faces=("front", "left", "right", "top", "bottom"))
    rock = kit.bone_paint(kit.shades("#2e2834", 5, 1.2), 35)
    top = d.head_y + 8.0
    rig.spike("head", (0.0, top - 0.4, -0.5), 2.6, 4.5, -14.0, 0.0, rock)                          # the crest of black stone on the head: one in the middle,
    rig.spike("head", (2.6, top - 0.4, -0.5), 2.2, 3.5, -10.0, -24.0, rock)                       # one on each side, leaning out
    rig.spike("head", (-2.6, top - 0.4, -0.5), 2.2, 3.5, -10.0, 24.0, rock)
    tusk = kit.bone_paint(kit.shades("#e6dcc0", 5, 1.1), 33)
    rig.box("head", (-3.5, d.head_y + 1.0, -6.3), (0.9, 2.4, 0.9), tusk)
    rig.box("head", (2.6, d.head_y + 1.0, -6.3), (0.9, 2.4, 0.9), tusk)
    z_front = -d.td / 2 - 0.5
    _crack(kit, rig, "body", -d.tw * 0.18, d.sh_y - 1.0, z_front, 7, 11)
    _crack(kit, rig, "body", d.tw * 0.22, d.sh_y - 2.0, z_front, 5, 23, flip=-1)
    _crack(kit, rig, "body", 0.0, d.hip_y + d.torso_h * 0.45, z_front, 4, 37)
    _crack(kit, rig, "arm_l", d.arm_x - 0.5, d.sh_y - 1.5, -d.ad / 2 - 0.5, 4, 41)
    _crack(kit, rig, "arm_r", -d.arm_x - 0.5, d.sh_y - 2.5, -d.ad / 2 - 0.5, 4, 53)
    _crack(kit, rig, "leg_l", d.leg_x - 0.4, d.hip_y - 1.0, -d.ld / 2 - 0.5, 4, 61)
    _crack(kit, rig, "leg_r", -d.leg_x - 0.2, d.hip_y - 2.0, -d.ld / 2 - 0.5, 4, 71)


def post(kit, rig):
    """A giant's head is huge and the kit's head is always 8 px: a steady scale of the head bone in every clip (the crest, the jaw and the face ride on it)."""
    s = HEAD_SCALE
    for name, clip in rig.A.clips.items():
        rig.A.scale(name, "head", {0.0: (s, s, s), clip["animation_length"]: (s, s, s)})
