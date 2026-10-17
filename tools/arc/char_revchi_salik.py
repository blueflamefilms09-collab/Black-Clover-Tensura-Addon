"""Why: Revchi Salik, the former Purple Orca Magic Knight turned thief who steals Yuno's grimoire in the first chapter and fights with Chain Magic: the first boss of the
story, so the model has to read at a glance as "a man made of chains". Look (facts only, from the anibase profile and the Black Clover wiki as summarised by search; see
docs/arc/revchi_salik.md): a slim man of about 32, 176 cm, with long droopy hair, a large burn scar over the left side of his face, a greedy, calculating face. Everything
else is the mod's own: a dark thief's coat with a steel trim, a ragged purple cape (what is left of his Purple Orca robe), steel chains coiled round both forearms and a
chain that ends in a kunai blade hanging from his belt (the chains of Chain Magic end in kunai blades in the mod's own ChainLayer), a faint cold glow in the steel.
Unverified: every colour of the clothes.
"""
SPEC = {
    "id": "revchi_salik",
    "name": "Revchi Salik",
    "role": "boss",
    "build": "slim",
    "height": 1.95,
    "skin": "fair",
    "face": {"expression": "smirk", "eyes": "sharp", "eye_color": "#3a3a46", "brows": "thin", "mouth": "smirk", "marks": ["scar_cheek_l", "scar_eye_l", "stubble"]},
    "hair": {"style": "shaggy", "color": "#2a2622"},
    "outfit": {
        "top": {"kind": "coat", "color": "#2c2a36", "trim": "#7a8498", "length": "knee", "collar": "high", "ragged": True},
        "inner": {"kind": "shirt", "color": "#cfc8bc"},
        "sleeves": {"kind": "long", "cuff": "#7a8498"},
        "legs": {"kind": "trousers", "color": "#1e1e26"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "knee"},
        "gloves": {"kind": "fingerless", "color": "#2a2a30"},
        "belt": {"kind": "double", "color": "#3c2c24", "buckle": "#aab4c4", "pouches": True},
        "neck": {"kind": "scarf", "color": "#5a4470"},
    },
    "cape": {"kind": "tattered", "color": "#5a4470", "length": 0.5, "ragged": True, "clasp": "#aab4c4"},
    "accessories": [{"kind": "chain", "color": "#aab4c4", "glow": "#d4dce8"}],
    "props": {"right": {"kind": "dagger", "color": "#c8d4e8", "color2": "#6e7a90", "glow": "#bcd0ff"}},
    "glow": {"color": "#bcd0ff", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "stab", "cast": "magic", "idle": "relaxed"},
    "notes": "Chain Magic boss of the Magic Knights Entrance Arc. The glow is only in the steel of the chains.",
}


def extra(kit, rig):
    """Chains coiled round both forearms and a chain with a kunai blade hanging from the belt at the back, swinging in every clip (rig.animate)."""
    d = rig.d
    steel = kit.metal(kit.shades("#aab4c4", 5, 1.25), 41, glow=(188, 208, 255, 70), emboss=False)
    link = kit.metal(kit.shades("#6e7a90", 5, 1.25), 42, glow=(188, 208, 255, 110), emboss=False)
    blade = kit.metal(kit.shades("#c8d4e8", 5, 1.3), 43, glow=(220, 232, 255, 190), emboss=False)
    for k in range(3):
        y = d.wrist_y + 0.8 + k * 2.4
        rig.pair("forearm_l", "forearm_r", (d.arm_x - d.aw / 2 - 0.4, y, -d.ad / 2 - 0.4), (d.aw + 0.8, 1.0, d.ad + 0.8), steel if k % 2 == 0 else link)
    rig.bone("chain_tail", "body", (d.tw / 2 - 2.0, d.hip_y + 1.0, d.td / 2 + 0.4))
    for k in range(6):
        rig.box("chain_tail", (d.tw / 2 - 2.5, d.hip_y + 0.4 - k * 1.6, d.td / 2 + 0.1), (1.0, 1.4, 1.0), steel if k % 2 == 0 else link)
    rig.box("chain_tail", (d.tw / 2 - 3.0, d.hip_y + 0.4 - 6 * 1.6 - 3.2, d.td / 2 + 0.2), (2.0, 3.4, 0.6), blade)
    rig.animate("chain_tail", sway=1.2, phase=0.7)
