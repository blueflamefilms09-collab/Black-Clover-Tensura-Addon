"""Why: Noelle Silva, the proud water mage of House Silva, a friend of the Sanctuary Hub (an NPC of the Black Bulls, record in arc/npc/BlackBullsNpcs.java). Look (from the
series and the wiki summaries): long silver-white hair in two low tails tied with dark blue ribbons, deep blue eyes, a fair face that blushes easily and pouts, a white
blouse with a dark blue ribbon at the neck, a dark blue skirt and boots under the short Black Bull cloak (black, the white bull on the back). Unverified: the exact length of
the skirt and the boots, the shade of the ribbons. The cloak's bull glows a cool water blue (the mod's own Valkyrie Dress colour), the only glow. See docs/arc/art_kit.md.
"""
SPEC = {
    "id": "noelle_silva",
    "name": "Noelle Silva",
    "role": "npc",
    "build": "slim",
    "height": 1.8,
    "skin": "fair",
    "face": {"expression": "calm", "eye_color": "#4a5cc8", "brows": "arched", "mouth": "pout", "lashes": True, "marks": ["blush"]},
    "hair": {"style": "twin_tails", "color": "#dfe4ef", "ribbon": "#24378a"},
    "outfit": {
        "top": {"kind": "dress", "color": "#27345e", "collar": "stand", "collar_color": "#eeeef3", "length": "thigh", "trim": "#eeeef3"},
        "inner": {"kind": "shirt", "color": "#eeeef3"},
        "sleeves": {"kind": "long", "color": "#eeeef3", "cuff": "#c8cce0"},
        "legs": {"kind": "bare"},
        "boots": {"kind": "boots", "color": "#3a2c26", "height": "knee"},
        "neck": {"kind": "cravat", "color": "#24378a"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#c8a040"},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.55, "ragged": True, "width": 0.9,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e4b454"},
    "glow": {"color": "#6ac0ff", "eyes": False, "props": False, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Black Bulls. The bull on the back of the cloak glows water blue; she carries no weapon (her Valkyrie Dress is a spell).",
}
