"""Why: Morris Libardirt (spelled Moris on the wiki), the Magic Scholar who once ruled the Diamond Kingdom's king from behind the throne, was thrown out by Mars
and joined the Spade Kingdom's Dark Triad, where he became the vessel and host of the devil Lucifero. He is the first boss of the Spade Kingdom Raid after the Triad, and
the fight is a fight against a man who edits things (Modification Magic) and treats the fighters as specimens.
Look (from the Black Clover wiki as summarised by the search results of 2026-10, see docs/arc/morris_libardirt.md for the URLs): a tall, slim, pale man with short, spiky blond hair
and round black tinted glasses that cover his blind eyes; a plain shirt and trousers, and a long light coat (the wiki gives the coat to his later Paladin outfit; it is used here as
his scholar's coat, which is a design choice). When Lucifero takes him as a host a dark mark on his forehead grows, his irises change colour and his pupils turn to vertical slits,
he grows Lucifero's horns and wings, and his arms and hands turn black: that is the `form2` below (the Gates).
Added for the game, not from the source: the orange rune on the back of the coat and the orange trim (the mod draws Modification Magic in orange), the belt of notebooks, and the
orbiting sphere of rewritten structure. Unverified: the exact colour of the coat, and the glasses as seen from the front (the kit draws the frames, the eyes behind them are painted closed).
"""
SPEC = {
    "id": "morris_libardirt",
    "name": "Morris Libardirt",
    "role": "boss",
    "build": "slim",
    "height": 2.0,
    "skin": "pale",
    "face": {"expression": "calm", "eyes": "closed", "eye_color": "#8a8a98", "brows": "thin", "mouth": "smirk", "nose": "sharp", "marks": ["eye_bags"]},
    "hair": {"style": "spiky_back", "color": "#d9c27a"},
    "outfit": {
        "top": {"kind": "coat", "color": "#d8d0c0", "trim": "#ff9b58", "length": "calf", "collar": "stand",
                "emblem": {"kind": "rune", "where": "back", "size": 9, "color": "#ff9b58", "glow": "#ff9b58"}},
        "inner": {"kind": "shirt", "color": "#2c2a36"},
        "sleeves": {"kind": "long", "cuff": "#ff9b58"},
        "legs": {"kind": "trousers", "color": "#2a2830"},
        "boots": {"kind": "shoes", "color": "#1c1a1e"},
        "belt": {"kind": "plain", "color": "#4a3a2a", "buckle": "#ff9b58", "books": True},
        "neck": {"kind": "cravat", "color": "#ff9b58"},
    },
    "accessories": [{"kind": "glasses", "color": "#101014"}],
    "props": {"right": {"kind": "orb", "color": "#ffd9b0", "glow": "#ff9b58"}},
    "glow": {"color": "#ff9b58", "eyes": False, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "proud", "extra": ["spawn"]},
    "form2": {
        "name": "Morris Libardirt, Lucifero's Host",
        "face": {"eyes": "slit", "eye_color": "#b58cff", "eye_glow": "#b58cff", "marks": ["forehead_sigil", "veins"]},
        "accessories": [],
        "outfit": {"sleeves": {"color": "#15121c", "cuff": "#5b2a8c"}, "gloves": {"kind": "gloves", "color": "#15121c"}},
        "horns": {"kind": "devil", "color": "#2a2028", "size": 1.0},
        "wings": {"kind": "bat", "color": "#2a2030", "color2": "#5b2a8c", "glow": "#b58cff", "span": 0.9},
        "props": {"right": {"kind": "orb", "color": "#2a1840", "glow": "#b58cff"}},
        "glow": {"color": "#b58cff", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The tinted lenses: Morris is blind and wears round black glasses, so the lens is dark glass in front of the painted (closed) eyes; the kit's glasses draw only the frame.
    Added only while the glasses are on (the second form takes them off)."""
    if not any(a.get("kind") == "glasses" for a in rig.spec.get("accessories", [])):
        return
    d = rig.d
    ey = d.head_y + 8.0 - (18.0 / 32.0) * 8.0
    lens = kit.metal(kit.shades("#1c1a26", 5, 1.0), 33, emboss=False)
    for sg in (1, -1):
        rig.box("head", (sg * 2.6 - 1.4, ey - 1.1, -4.22), (2.8, 2.6, 0.3), lens, faces=("front",))
