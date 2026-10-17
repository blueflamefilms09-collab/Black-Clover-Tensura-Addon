"""Why: Nacht Faust, the blunt devil host of the Black Bulls, a friend of the Sanctuary Hub who explains devils (an NPC, record in arc/npc/BlackBullsNpcs.java; his Shadow Magic
and the four Unite modes are already in the mod). Look (from the series and the wiki summaries): a slim man with pale skin and pale blue eyes, a cold soft face, long black hair
in a high ponytail, a dark high-collared coat under the long Black Bull cloak, and on a string round his neck four relics of his four devils (drawn as four violet gems that
glow, built in extra()). Unverified: the cut of the coat, the colour of the gems (the mod's Shadow book colour is a grey violet). Only the gems and the cloak's bull glow. See docs/arc/art_kit.md.
"""
SPEC = {
    "id": "nacht_faust",
    "name": "Nacht Faust",
    "role": "npc",
    "build": "slim",
    "height": 2.04,
    "skin": "pale",
    "face": {"expression": "cold", "eye_color": "#a8cce8", "brows": "thin", "mouth": "line"},
    "hair": {"style": "high_ponytail", "color": "#12121a"},
    "outfit": {
        "top": {"kind": "uniform", "color": "#24242e", "collar": "high", "trim": "#8e8eb0", "length": "thigh"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#1b1b24"},
        "boots": {"kind": "boots", "color": "#17171d", "height": "calf"},
        "neck": {"kind": "choker", "color": "#2a2630"},
        "belt": {"kind": "plain", "color": "#2a2630", "buckle": "#8e8eb0"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.78, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#8e8eb0"},
    "glow": {"color": "#9a7cf0", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Black Bulls. The four violet gems on the cord at his chest (the relics of his four devils) are the glow.",
}


def extra(kit, rig):
    """The four relics of his four devils on a string round his neck: a thin cord across the chest and four violet gems that glow, the middle two a little lower and bigger.
    Placed from the measured body (rig.d), in front of the coat."""
    d = rig.d
    z = -d.td / 2 - 0.9
    y = d.sh_y
    cord = kit.leather(kit.cloth_palette("#2a2630", 5, 0.9), 51)
    gem = kit.metal(kit.shades("#9a7cf0", 5, 1.1), 52, glow=(154, 124, 240, 235), emboss=False, rim=False)
    rig.box("body", (-3.4, y - 1.4, z + 0.2), (6.8, 0.5, 0.4), cord, faces=("front", "top"))
    for x, dy, w, h in ((-3.0, 1.4, 1.1, 1.4), (-1.1, 2.6, 1.3, 1.8), (1.1, 2.6, 1.3, 1.8), (3.0, 1.4, 1.1, 1.4)):
        rig.box("body", (x - w / 2, y - 1.4 - dy, z), (w, h, 0.6), gem, faces=("front", "left", "right", "bottom"))
