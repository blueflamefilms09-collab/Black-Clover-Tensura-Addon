"""Why: Vanessa Enoteca, the lazy and wise witch of the Black Bulls, a friend of the Sanctuary Hub (an NPC, record in arc/npc/BlackBullsNpcs.java; her Thread Magic and
Rouge are already in the mod's grimoires). Look (from the series and the wiki summaries): tall, long wavy pink hair, purple eyes with heavy lids, painted lips, a dark
purple top and short skirt with knee boots under a very short Black Bull cloak, gold earrings, and a red thread tied round the right forearm (her Red Thread of Fate, drawn
as a red wrap). Unverified: the cut of her clothes (the series draws a revealing outfit; this one stays modest on purpose), the exact pink. Only the cloak's bull glows red.
See docs/arc/art_kit.md for how the spec fields work.
"""
SPEC = {
    "id": "vanessa_enoteca",
    "name": "Vanessa Enoteca",
    "role": "npc",
    "build": "average",
    "height": 1.97,
    "skin": "fair",
    "face": {"expression": "calm", "eyes": "tired", "eye_color": "#8a5ad0", "brows": "thin", "mouth": "smile", "lashes": True, "marks": ["lipstick", "eyeshadow"]},
    "hair": {"style": "long_wavy", "color": "#e87aae"},
    "outfit": {
        "top": {"kind": "dress", "color": "#2d1d40", "length": "thigh", "trim": "#8a2a4a"},
        "sleeves": {"kind": "none"},
        "legs": {"kind": "bare"},
        "boots": {"kind": "boots", "color": "#1d1722", "height": "knee"},
        "belt": {"kind": "sash", "color": "#8a2a4a"},
        "bandages": {"parts": ["forearm_r"], "color": "#d03048"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.5, "ragged": True, "width": 0.9,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e4b454"},
    "accessories": [{"kind": "earrings", "color": "#e8c060"}],
    "glow": {"color": "#ff4a6a", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Black Bulls. The red wrap on the right forearm is her thread; the bull of the cloak glows red.",
}


def extra(kit, rig):
    """Her Red Thread of Fate: a glowing red cord tied round the right wrist whose loose end hangs down along the outside of the hand and sways in every clip (a bone of its
    own, `thread`). Thin on purpose: a wide cuff and a long straight cord read as a sword."""
    d = rig.d
    wx = -d.arm_x
    red = kit.metal(kit.shades("#e0344e", 5, 1.2), 51, glow=(255, 70, 100, 235), emboss=False, rim=False)
    rig.box("forearm_r", (wx - d.aw / 2 - 0.2, d.wrist_y + 0.6, -d.ad / 2 - 0.2), (d.aw + 0.4, 0.6, d.ad + 0.4), red)
    rig.bone("thread", "hand_r", (wx - d.aw / 2, d.wrist_y + 0.6, 0.0))
    rig.box("thread", (wx - d.aw / 2 - 0.5, d.wrist_y - 6.0, -0.2), (0.4, 6.6, 0.4), red, rotation=(0, 0, -7), pivot=(wx - d.aw / 2, d.wrist_y + 0.6, 0.0))
    rig.animate("thread", sway=1.0)
