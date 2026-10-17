"""Why: Escanor, the Lion's Sin of Pride, whose power is the sun: at midnight a timid, small man, at noon the strongest of the Seven Deadly Sins. The fight follows the
clock (docs/arc/escanor.md), and so does the model: the first model is the man of the night and the dawn, the `form2` is Pride in the daylight (the boss class switches
to it when the Sunshine is high, and enlarges the hitbox, which the renderer follows). Look: what the sources I could read agree on is the contrast (a small, bashful man
who grows, with swollen muscle, as the sun climbs) and the Divine Axe Rhitta; the clothes, the colours of the hair and the lion on the belt and shoulder are design choices
for the game and are not verified. Rhitta is a prop of this character's own (the kit's one-handed axe has a thin haft and a small head; the Divine Axe is a great
crescent blade on a long haft), registered here as the kit's manual allows for a prop only one character uses; it glows with the sun.
"""
import gen_arc_kit as kit


def p_rhitta(pb):
    """The Divine Axe Rhitta: a long haft with a gilded socket and a great crescent blade of three plates (to the model's right) with a glowing edge, a back spike and a gold crest."""
    L = pb.L(30.0)
    haft = pb.leather(pb.col2 or "#4a2f20", 5)
    gold = pb.metal("#d9a030", 5)
    pb.box(-0.8, -8.0, -0.8, 1.6, L, 1.6, haft)
    pb.box(-1.2, -8.8, -1.2, 2.4, 1.2, 2.4, gold)
    top = -8.0 + L
    pb.box(-1.6, top - 10.0, -1.6, 3.2, 11.0, 3.2, gold)
    pb.box(-6.5, top - 7.5, -0.8, 5.0, 8.0, 1.6, pb.steel(1))
    pb.box(-11.5, top - 9.5, -0.7, 5.0, 12.0, 1.4, pb.steel(2))
    pb.box(-14.0, top - 12.0, -0.6, 2.5, 16.0, 1.2, pb.steel(3))
    pb.box(1.6, top - 6.0, -0.8, 4.5, 3.0, 1.6, pb.steel(4))
    pb.box(-1.0, top + 1.0, -1.0, 2.0, 5.0, 2.0, gold)


kit.register_prop("rhitta", kit.PropDef(p_rhitta, attack="smash", orient=((-1.0, 0.0, 0.0), "forward")))
kit.recipes.HOLD["rhitta"] = kit.recipes.HOLD["axe"]

SPEC = {
    "id": "escanor",
    "name": "Escanor",
    "role": "boss",
    "build": "broad",
    "height": 2.0,
    "skin": "light",
    "face": {"expression": "shy", "eyes": "big", "eye_color": "#8a6a3a", "brows": "sad", "mouth": "neutral", "marks": ["eye_bags"]},
    "hair": {"style": "short", "color": "#6a4a30"},
    "outfit": {
        "top": {"kind": "coat", "color": "#4a3a30", "trim": "#8a6a3a", "length": "thigh", "collar": "stand"},
        "inner": {"kind": "shirt", "color": "#cfc4ac"},
        "sleeves": {"kind": "long", "cuff": "#8a6a3a"},
        "legs": {"kind": "trousers", "color": "#2a2428"},
        "boots": {"kind": "boots", "color": "#2a2220", "height": "calf"},
        "belt": {"kind": "plain", "color": "#3c281c", "buckle": "#d9a030"},
    },
    "props": {"right": {"kind": "rhitta", "color": "#8a8f9a", "color2": "#4a2f20", "glow": "#ffb040"}},
    "glow": {"color": "#ffb040", "eyes": False, "props": True, "trim": False},
    "clips": {"attack": "smash", "cast": "magic", "idle": "slouch", "extra": []},
    "form2": {
        "name": "Escanor, Pride in the Sun",
        "skin": "tan",
        "hair": {"style": "wild_mane", "color": "#e0902a"},
        "face": {"expression": "grin", "eyes": "fierce", "eye_color": "#ffb040", "eye_glow": "#ffd070", "brows": "thick", "mouth": "grin", "marks": ["stubble"]},
        "outfit": {
            "top": {"kind": "tank", "color": "#1c1618", "trim": "#d9a030"},
            "inner": {"kind": "none"},
            "sleeves": {"kind": "none"},
            "legs": {"color": "#1e1a1e"},
            "belt": {"kind": "double", "color": "#3c281c", "buckle": "#ffd070"},
        },
        "armor": {"pieces": ["pauldrons", "bracers"], "style": "plate", "color": "#b8782a", "trim": "#ffd070", "glow": "#ffd070"},
        "glow": {"color": "#ffd070", "eyes": True, "props": True, "trim": True},
    },
}
