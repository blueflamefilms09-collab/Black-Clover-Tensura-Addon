"""Why: Zora Ideale, the rude, honest prankster who hates nobles and sets traps of ash, a friend of the Sanctuary Hub who warns about the dungeon's traps (an NPC, record in
arc/npc/BlackBullsNpcs.java; his Ash Magic is already in the mod). Look (from the series and the wiki summaries): a thin man with messy red hair, blue eyes and pale skin, black
leather straps wound round the lower face and the neck (two leather rings round the jaw joined by a strap on each cheek, and belts at the neck: built in extra()), metal studs (ear
cuffs, one above the right brow), a dark sleeveless shirt and plain dark trousers under the Black Bull cloak. Unverified: the straps are two rings here and not one wrap, the studs are
few, the clothes under the cloak. The bull on the cloak glows ash white. See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "zora_ideale",
    "name": "Zora Ideale",
    "role": "npc",
    "build": "slim",
    "height": 1.94,
    "skin": "pale",
    "face": {"expression": "smirk", "eye_color": "#4a78c8", "brows": "thin"},
    "hair": {"style": "undercut", "color": "#c8402e"},
    "outfit": {
        "top": {"kind": "tank", "color": "#2a2a30", "length": "hip"},
        "legs": {"kind": "trousers", "color": "#3a3a44"},
        "boots": {"kind": "boots", "color": "#18181c", "height": "calf"},
        "gloves": {"kind": "wraps", "color": "#2a2a30"},
        "neck": {"kind": "belts", "color": "#1e1b1f"},
        "belt": {"kind": "double", "color": "#2c2420", "buckle": "#9a9aaa"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.7, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#9a9aaa"},
    "accessories": [{"kind": "ear_cuffs", "color": "#b8b8c8"}],
    "glow": {"color": "#d8d8ee", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Black Bulls. The leather straps of his face are built by hand in extra(); the bull of the cloak glows ash white.",
}


def extra(kit, rig):
    """The leather straps wound round the lower face: two rings round the jaw (one under the mouth, one under the nose) joined by a strap on each cheek, so the mouth and the
    nose stay open, and a metal stud above the right brow. Placed from the measured head (rig.d.head_y), so it fits the build."""
    d = rig.d
    hy = d.head_y
    strap = kit.leather(kit.cloth_palette("#1e1b1f", 5, 0.9), 41)
    stud = kit.metal(kit.shades("#c0c0d0", 5, 1.2), 42, emboss=False)
    ring = ("front", "back", "left", "right")
    for y0, h in ((0.1, 0.7), (1.95, 0.6)):
        rig.box("head", (-4.35, hy + y0, -4.35), (8.7, h, 8.7), strap, faces=ring)
    for x0 in (-3.6, 2.7):
        rig.box("head", (x0, hy + 0.1, -4.35), (0.9, 2.45, 0.5), strap, faces=("front", "left", "right", "top", "bottom"))
    rig.box("head", (-2.9, hy + 5.2, -4.2), (0.6, 0.6, 0.4), stud, faces=("front",))
