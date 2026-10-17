"""Why: Lucifero, the King of Devils, the devil of Gravity Magic, the one the Magic Knight captains could not stand against; he is the last boss of the Spade Kingdom Raid and
the likely guardian of the deepest floor of the Abyssal road.
Look (from the Black Clover wiki as summarised by the search results of 2026-10, see docs/arc/lucifero.md for the URLs): in his full manifestation a tall, shadowy figure with
long white hair, two pairs of horns on the sides of his head, two pairs of hooked bat-like wings on his back, thin bony hands, grey skin covered in black patterns across the whole
body; at full power spikes form on his shoulders and elbows, his horns grow thicker, and a third eye without iris or pupil opens on his forehead.
Added for the game, not from the source: the violet glow (the gravity violet of the Zogratis family), the dark sphere he holds on his palm (a compressed gravity well), and the hover
(he levitates freely in the air in the story). Unverified: the second pair of horns and wings is drawn by `extra` below as a modest lower pair, not from a reference picture.
"""
SPEC = {
    "id": "lucifero",
    "name": "Lucifero",
    "role": "boss",
    "build": "lanky",
    "height": 2.7,
    "skin": "grey",
    "face": {"expression": "cold", "eyes": "slit", "eye_color": "#ece4ff", "eye_glow": "#b58cff", "brows": "flat", "mouth": "line", "nose": "sharp", "marks": ["third_eye", "veins"]},
    "hair": {"style": "long", "color": "#ece8f4"},
    "outfit": {
        "top": {"kind": "tank", "color": "#807e8e", "pattern": "runes", "pattern_color": (20, 16, 28)},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "leggings", "color": "#807e8e", "stripe": "#14101c"},
        "boots": {"kind": "bare"},
        "belt": {"kind": "sash", "color": "#26222e", "buckle": "#b58cff"},
    },
    "armor": {"pieces": ["pauldrons", "bracers"], "style": "bone", "color": "#2a2630", "trim": "#b58cff", "glow": "#b58cff"},
    "horns": {"kind": "devil", "color": "#6a5a86", "size": 1.1},
    "wings": {"kind": "bat", "color": "#2a2434", "color2": "#4a3a6a", "glow": "#b58cff", "span": 1.2},
    "props": {"right": {"kind": "orb", "color": "#1a1028", "glow": "#b58cff"}},
    "glow": {"color": "#b58cff", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "hover", "extra": ["spawn"]},
    "form2": {
        "name": "Lucifero, King of Devils (full power)",
        "face": {"eye_glow": "#ffffff", "marks": ["third_eye", "veins", "cracks"]},
        "armor": {"pieces": ["pauldrons", "bracers", "spikes"], "color": "#1e1a24", "glow": "#d8c0ff"},
        "horns": {"kind": "devil", "color": "#7a68a0", "size": 1.45},
        "wings": {"kind": "bat", "color": "#1e1a28", "color2": "#5b2a8c", "glow": "#d8c0ff", "span": 1.5},
        "props": {"right": {"kind": "orb", "color": "#0e0818", "glow": "#d8c0ff"}},
        "glow": {"color": "#d8c0ff", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The King has four horns and four wings in the story; the kit builds one pair of each, so this adds the second pair: two lower horns on the temples, sweeping out and back, and a
    smaller pair of bat wings hanging below the main pair (the same rays-and-membrane build as the kit's `bat` wings, scaled to the spec's `span`, and swaying like cloth)."""
    d = rig.d
    w = rig.spec["wings"]
    span = w["span"]
    horn = kit.bone_paint(kit.shades(rig.spec["horns"]["color"] if rig.spec.get("horns") else "#6a5a86", 5, 1.1), 141)
    size = rig.spec["horns"]["size"] if rig.spec.get("horns") else 1.0
    for sg in (1, -1):
        rig.spike("head", (sg * 4.0, d.head_y + 4.6, -0.2), 2.4 * size, 7.0 * size, 10.0, -sg * 40.0, horn, fracs=(0.55, 0.45), taper=0.6)
    cols, cols2 = kit.shades(w["color"], 5, 1.0), kit.shades(w["color2"] or "#4a3a6a", 5, 1.0)
    bone_p, mem = kit.bone_paint(cols2, 17), kit.fabric(cols, 18, period=5.0, amp=0.18, seam=False, cut="b", cut_depth=3)
    ln_main = max(14.0, d.top * 0.55) * span * 0.62
    P = (2.2, d.sh_y - 7.5, d.td / 2 + 1.7)
    for suf, sg in (("_l", 1), ("_r", -1)):
        rig.bone("wing2" + suf, "body", (sg * P[0], P[1], P[2]), rotation=(0, sg * -16.0, 0))
        rig.animate("wing2" + suf, sway=1.2, phase=0.8)
    import math
    rays = [(104.0, 0.80), (126.0, 1.0), (150.0, 0.84)]
    for k, (a, f) in enumerate(rays):
        c = rig.box("wing2_l", (P[0] - 0.5, P[1], P[2] - 0.5), (1.0, round(ln_main * f * 2) / 2, 1.0), bone_p, rotation=(0, 0, -a), pivot=P)
        rig.pk.twin(rig.m, "wing2_r", c)
        if k > 0:
            a0, f0 = rays[k - 1]
            lm = round(ln_main * (f + f0) / 2.0 * 0.96 * 2) / 2
            wid = max(4.0, 2 * lm * math.sin(math.radians((a - a0) / 2.0)) + 0.8)
            c2 = rig.box("wing2_l", (P[0] - wid / 2, P[1], P[2] - 0.2), (wid, lm, 0.4), mem, rotation=(0, 0, -(a + a0) / 2.0), pivot=P, faces=("front", "back"), dens=4)
            rig.pk.twin(rig.m, "wing2_r", c2)
