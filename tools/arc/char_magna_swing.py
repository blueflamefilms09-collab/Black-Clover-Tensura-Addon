"""Why: Magna Swing, the hot-headed fire mage and ex-delinquent of the Black Bulls, a friend of the Sanctuary Hub (an NPC, record in arc/npc/BlackBullsNpcs.java; his Fire Magic is
already in the mod). Look (from the series and the wiki summaries): a lean man with a black mohawk whose top is light grey, tinted shades, a white shirt under a short navy leather
jacket, black fingerless gloves and boots, the Black Bull cloak, and his broom Crazy Cyclone (a character file may add a prop only it uses: the broom is built here and stowed across
his back, its straw glowing like embers). Unverified: the jacket's cut, the shade of the lenses (goggles with blue lenses stand for his shades). See docs/arc/art_kit.md.
"""
import gen_arc_kit as kit


def p_broom(pb):
    """Crazy Cyclone: a wooden shaft, a band, a bundle of straw whose tips glow like embers. Authored standing up (the kit's prop convention), with the origin 9 units up the shaft
    from the band so that, stowed diagonally on the back, the straw ends above the floor instead of sinking into it."""
    o = 9.0
    shaft = pb.L(34.0)
    pb.box(-0.7, -8.0 + o, -0.7, 1.4, shaft, 1.4, pb.leather(pb.col2 or "#6a4a2e", 5))
    straw = pb.wrap(pb.col or "#d8b868", 6)
    pb.box(-1.0, -9.2 + o, -1.0, 2.0, 1.4, 2.0, pb.metal("#8a6a3a", 9))                     # the band
    pb.box(-2.2, -16.2 + o, -2.2, 4.4, 7.0, 4.4, straw)                                      # the bundle
    pb.box(-3.0, -20.7 + o, -3.0, 6.0, 4.5, 6.0, straw)                                      # its spread tips
    g = None if pb.glow is None else (pb.glow[0], pb.glow[1], pb.glow[2], 230)
    ember = kit.metal(kit.shades(pb.glow or (255, 150, 60), 5, 1.1), 7, glow=g if g else (255, 150, 60, 220), emboss=False, rim=False)
    pb.box(-3.2, -22.1 + o, -3.2, 6.4, 1.4, 6.4, ember)


kit.register_prop("broom", kit.PropDef(p_broom, attack="smash", cast="gesture", grip=(-90.0, 0.0, 0.0)))
kit.recipes.HOLD["broom"] = kit.recipes.HOLD["staff"]

SPEC = {
    "id": "magna_swing",
    "name": "Magna Swing",
    "role": "npc",
    "build": "average",
    "height": 1.96,
    "skin": "light",
    "face": {"expression": "grin", "eye_color": "#3a3a46", "brows": "angry"},
    "hair": {"style": "mohawk", "color": "#17171b", "streak": "#c4c8cf"},
    "outfit": {
        "top": {"kind": "coat", "color": "#2a3555", "length": "hip", "collar": "stand"},
        "inner": {"kind": "shirt", "color": "#ecebea"},
        "sleeves": {"kind": "long"},
        "legs": {"kind": "trousers", "color": "#23232b"},
        "boots": {"kind": "boots", "color": "#18181c", "height": "calf"},
        "gloves": {"kind": "fingerless", "color": "#1c1c20"},
        "belt": {"kind": "plain", "color": "#3a2a22", "buckle": "#c8a040", "pouches": True},
    },
    "cape": {"kind": "cloak", "color": "#1c1c20", "trim": "#e8e4dc", "length": 0.62, "ragged": True, "width": 0.95,
             "emblem": {"kind": "bull", "color": "#e8e4dc", "size": 8, "y": 4}, "clasp": "#e4b454"},
    "accessories": [{"kind": "goggles", "color": "#1c1c24", "color2": "#3a78d8"}],
    "props": {"back": {"kind": "broom", "color": "#d8b868", "color2": "#6a4a2e", "glow": "#ff8a30", "length": 0.55}},
    "glow": {"color": "#ff8a30", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "punch", "cast": "gesture", "idle": "proud"},
    "notes": "NPC of the Black Bulls. The broom is a custom prop of this file (registered at import); the straw tips and the cloak's bull glow ember orange.",
}
