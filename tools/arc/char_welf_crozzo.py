"""Why: Welf Crozzo, the smith of the Hestia Familia who forges the weapons of the DanMachi saga: the friend who asks the player for the hunt of the Black Goliath (the Goliath's black
hide is a smith's puzzle). Look, from the series (facts only, see docs/arc/danmachi_npcs.md): a sturdy young man with red-brown hair, a bandana, a leather work outfit with arm guards, a smith's
hands, and a great sword on his back (a Crozzo magic sword). Added for the game, not from the source: the hammer in the hand (a smith is shown at work) and the ember glow in the great sword.
"""
SPEC = {
    "id": "welf_crozzo",
    "name": "Welf Crozzo",
    "role": "npc",
    "build": "broad",
    "height": 1.95,
    "skin": "tan",
    "face": {"expression": "grin", "eye_color": "#a8482a", "brows": "thick", "marks": ["scar_cheek_l"]},
    "hair": {"style": "spiky_back", "color": "#8a2e1c"},
    "outfit": {
        "top": {"kind": "vest", "color": "#2a2020", "trim": "#a05a30"},
        "inner": {"kind": "tank", "color": "#c8b898"},
        "legs": {"kind": "trousers", "color": "#2c2622"},
        "boots": {"kind": "boots", "color": "#3a2a20", "height": "calf"},
        "gloves": {"kind": "fingerless", "color": "#6a4a30"},
        "belt": {"kind": "double", "color": "#4a3020", "buckle": "#c8782a", "pouches": True},
    },
    "armor": {"pieces": ["bracers", "pauldrons"], "style": "leather", "color": "#6a4a30", "trim": "#c8782a"},
    "accessories": [{"kind": "headband", "color": "#b83a2a"}],
    "props": {"right": {"kind": "hammer", "color": "#6a6a74", "color2": "#8a5a30"},
              "back": {"kind": "greatsword", "color": "#8a909c", "color2": "#c8782a", "glow": "#ff8a3a"}},
    "glow": {"color": "#ff8a3a", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "smash", "cast": "gesture", "idle": "relaxed"},
}
