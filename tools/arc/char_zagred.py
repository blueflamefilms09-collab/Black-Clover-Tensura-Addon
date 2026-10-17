"""Why: Zagred, the Word Soul devil of Black Clover (the Kotodama devil, who destroyed the elves and founded the Eye of the Midnight Sun), is the finale boss of
saga 1 and the one boss whose whole fight is speech, so his model has to say two things at once: a tall, lean, tattooed man in a long black coat (the body he
borrows: written all over with glowing glyphs, the Word Soul in ink) and, at the Gates, the devil under it (grey skin, red eyes, black bat wings, horns, a long
tail, the glyphs burning red, the otherworldly trident across his back). The look follows the series' own description of the devil (a tall lean figure, grey skin,
red irises, black pointed teeth, bat wings, horns, clawed hands and an arrow-tipped tail; sources in docs/arc/zagred.md); the host form, the glyph tattoos, the
floating grimoire and the colours (violet for the Word, red for the ruin) are additions of the game, chosen to match the Kotodama VFX of the mod.
The old cubiform model (client/ZagredModel) stays registered for the legacy boss; this one is the kit's.
"""
SPEC = {
    "id": "zagred",
    "name": "Zagred",
    "role": "boss",
    "build": "lanky",
    "height": 2.3,
    "skin": "ashen",
    "face": {"expression": "sinister", "eyes": "narrow", "eye_color": "#d0203c", "eye_glow": "#ff3a5c", "brows": "angry", "mouth": "smirk",
             "marks": ["forehead_sigil", "cheek_lines", "veins"]},
    "hair": {"style": "spiky_back", "color": "#0c0a12"},
    "outfit": {
        "top": {"kind": "coat", "color": "#14101c", "trim": "#8a4cff", "length": "ankle", "collar": "high", "collar_color": "#1c1626",
                "pattern": "runes", "pattern_color": (74, 42, 138), "hem_trim": True, "open_top": 8, "open_bottom": 5,
                "emblem": {"kind": "rune", "where": "back", "size": 9, "glow": "#8a4cff"}},
        "inner": {"kind": "bare"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "trousers", "color": "#100c18"},
        "boots": {"kind": "boots", "color": "#0e0a14", "height": "knee", "cuff": "#8a4cff"},
        "gloves": {"kind": "gloves", "color": "#0e0a14"},
        "belt": {"kind": "double", "color": "#1c1626", "buckle": "#8a4cff"},
        "neck": {"kind": "none"},
    },
    "props": {"float": {"kind": "grimoire", "color": "#1e1640", "color2": "#6a5ab0", "glow": "#8a4cff"}},
    "glow": {"color": "#8a4cff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "magic", "idle": "proud", "extra": ["spawn"]},
    "form2": {
        "name": "Zagred, the Word Soul Devil",
        "skin": "grey",
        "face": {"eye_color": "#ff2a4a", "eye_glow": "#ff5a70", "eyes": "slit", "mouth": "fangs", "marks": ["forehead_sigil", "cheek_lines", "cracks", "veins"]},
        "hair": {"color": "#08060c"},
        "outfit": {
            "top": {"color": "#1a0e16", "trim": "#f2194d", "pattern_color": (122, 24, 48), "ragged": True, "emblem": {"kind": "rune", "where": "back", "size": 9, "glow": "#f2194d"}},
            "legs": {"color": "#140a12"},
            "boots": {"cuff": "#f2194d"},
            "belt": {"buckle": "#f2194d"},
        },
        "horns": {"kind": "bull", "color": "#14101a", "color2": "#3a2438", "glow": "#f2194d", "size": 1.1},
        "wings": {"kind": "bat", "color": "#0e0a14", "color2": "#2a1428", "glow": "#f2194d", "span": 1.3},
        "tail": {"kind": "devil", "color": "#1a1018", "color2": "#f2194d", "glow": "#f2194d", "length": 1.2},
        "props": {"back": {"kind": "trident", "color": "#14101c", "color2": "#2a1a4a", "glow": "#f2194d"},
                  "float": {"kind": "grimoire", "color": "#2a0e1c", "color2": "#a03050", "glow": "#f2194d"}},
        "glow": {"color": "#f2194d", "eyes": True, "props": True, "trim": True},
    },
}


# ---------------------------------------------------------------- the Word Soul written on the body
# hand-drawn runes, 3 x 5 texels each: the Kotodama glyphs he is written in (not a real script; they only have to read as one at a glance)
RUNES = (
    ("#.#", "###", ".#.", "###", "#.#"),
    ("###", "#.#", "###", "#..", "#.."),
    (".#.", "###", "#.#", ".#.", "#.#"),
    ("##.", ".#.", "###", ".#.", ".##"),
    ("#.#", "#.#", "###", ".#.", ".#."),
    ("###", "..#", ".#.", "#..", "###"),
)
FACES = {"front": 0, "back": 1, "left": 2, "right": 3, "top": 4, "bottom": 5}


def _hash(a, b, c):
    """A small integer hash (no randomness: the same texel always draws the same rune, so the build stays byte-identical)."""
    return ((a * 73856093) ^ (b * 19349663) ^ (c * 83492791)) & 0x7FFFFFFF


def _glyphs(rgb, glow, salt, only_faces=None):
    """A painter of glowing runes on a transparent skin: a grid of 4 x 6 texel cells, three of every five holding one rune, drawn in `rgb` and lit in `glow`."""
    col = (rgb[0], rgb[1], rgb[2], 255)
    lit = (glow[0], glow[1], glow[2], 230)

    def paint(face, i, j, fw, fh):
        if face in ("top", "bottom") or (only_faces and face not in only_faces):
            return None, None
        lx, ly = i % 4, j % 6
        if lx > 2 or ly > 4:
            return None, None
        cx, cy = i // 4, j // 6
        n = _hash(cx, cy, salt + FACES[face] * 17)
        if (n % 5 < 2 and (cx, cy) != (0, 0)) or RUNES[n % len(RUNES)][ly][lx] != "#":     # the first cell of every face always holds a rune: the kit rejects an empty face
            return None, None
        return col, lit
    return paint


def extra(kit, rig):
    """The tattoos: glowing runes over the bare chest (through the open coat), both upper arms and both forearms above the gloves. The colour follows the form
    (violet for the host, red for the devil) so the glyphs burn the way the rest of the model glows."""
    d = rig.d
    glow = kit.rgb(rig.spec["glow"]["color"])
    ink = kit.mix(glow, (0, 0, 0), 0.4)
    arm = _glyphs(ink, glow, 3)
    forearm = _glyphs(ink, glow, 5)
    chest = _glyphs(ink, glow, 11, only_faces=("front",))
    up_h = d.sh_y - d.elbow_y
    fw_, fd_ = max(2.0, d.aw - 0.5), max(2.0, d.ad - 0.5)
    e = 0.07
    sides = ("front", "back", "left", "right")                                   # no top or bottom: nothing is painted there, and the kit rejects a face with no texel
    rig.pair("arm_l", "arm_r", (d.arm_x - d.aw / 2 - e, d.elbow_y + 0.3, -d.ad / 2 - e), (d.aw + 2 * e, up_h - 0.6, d.ad + 2 * e), arm, faces=sides)
    rig.pair("forearm_l", "forearm_r", (d.arm_x - fw_ / 2 - e, d.wrist_y + 2.2, -fd_ / 2 - e), (fw_ + 2 * e, d.fore - 2.4, fd_ + 2 * e), forearm, faces=("front", "left", "right"))
    rig.box("body", (-1.15, d.hip_y + 3.6, -d.td / 2 - 0.08), (2.3, d.torso_h - 4.6, 0.1), chest, faces=("front",))
