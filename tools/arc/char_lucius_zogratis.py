"""Why: Lucius Zogratis, the Wizard King's other soul and the mod's main Zogratis antagonist (entity nusmp:lucius_zogratis, entity/zogratis/LuciusBoss, and the clones of
entity/zogratis/ZogratisCloneEntity, which wear the same bodies). The hand-built model was the quality bar the art kit was copied from (tools/gen_zogratis_lucius.py); the owner
now wants every mob on the player's proportions with the kit, so this is the same man again as a kit character. From the old design: the human form is a pale man with a short
spiky black head of hair, red eyes, a small sigil on the left of the forehead, a faint smile, a huge white fur mantle over a tall dark collar and a choker, a long black cloak with a
dark fur lining over a brown-maroon robe with crossed dark straps, and a grimoire that floats beside him; `form2` is the divine form (barefoot, pale-blue and white layered robe,
THREE pairs of feathered wings, a halo of gears, golden stag antlers, gold eyes). The wings are the kit's `angel` wings (one pair; the old model's three pairs are made here by two
more in `extra`). The old model and its renderer stay in the tree. Colours are the game's, not a trace. See docs/arc/remodel.md.
"""
ENTITIES = ["lucius_zogratis", "zogratis_clone"]

SPEC = {
    "id": "lucius_zogratis",
    "name": "Lucius Zogratis",
    "role": "boss",
    "build": "average",
    "height": 2.1,
    "skin": "pale",
    "face": {"expression": "smirk", "eyes": "sharp", "eye_color": "#d02a34", "eye_glow": "#ff3a44", "brows": "thin", "mouth": "smile", "nose": "sharp",
             "marks": ["forehead_sigil"]},
    "hair": {"style": "spiky_back", "color": "#12101a"},
    "outfit": {
        "top": {"kind": "robe", "color": "#6a3a3a", "trim": "#2a1a1c", "length": "floor", "collar": "fur", "collar_color": "#f2f0f6", "hem_trim": True,
                "pattern": "stripes", "pattern_color": (74, 40, 40)},
        "inner": {"kind": "shirt", "color": "#2a2228"},
        "sleeves": {"kind": "wide", "cuff": "#2a1a1c"},
        "legs": {"kind": "trousers", "color": "#2a2228"},
        "boots": {"kind": "boots", "color": "#241c20", "height": "ankle"},
        "gloves": {"kind": "gloves", "color": "#2a2228"},
        "belt": {"kind": "sash", "color": "#2a1a1c", "buckle": "#c8a040"},
        "neck": {"kind": "choker", "color": "#2a2228"},
    },
    "cape": {"kind": "cloak", "color": "#1e1a24", "trim": "#e8e4ee", "length": 0.9, "lining": "#3a2c34", "clasp": "#c8a040"},
    "accessories": [{"kind": "shoulder_fur", "color": "#f2f0f6"}],
    "props": {"float": {"kind": "grimoire", "color": "#c8a040", "color2": "#f2f0f6", "glow": "#ffd870"}},
    "glow": {"color": "#ff3a44", "eyes": True, "props": True, "trim": False},
    "clips": {"attack": "punch", "cast": "book", "idle": "proud"},
    "form2": {
        "name": "Lucius Zogratis, Divine",
        "skin": "pale",
        "face": {"eye_color": "#f0c040", "eye_glow": "#ffd870", "eyes": "wide", "mouth": "neutral", "marks": ["forehead_sigil", "light_lines"]},
        "hair": {"color": "#e8e4f0"},
        "outfit": {
            "top": {"color": "#c8dcf4", "trim": "#e8b830", "pattern_color": (160, 190, 226), "emblem": {"kind": "sun", "where": "chest", "size": 7, "glow": "#ffd870"}},
            "inner": {"kind": "shirt", "color": "#f2f0f6"},
            "legs": {"color": "#e8ecf6"},
            "boots": {"kind": "bare"},
            "gloves": {"color": "#f2f0f6"},
            "belt": {"color": "#e8b830", "buckle": "#fff0b0"},
            "neck": {"kind": "necklace", "color": "#e8b830", "glow": True},
        },
        "cape": {"color": "#f2f0f6", "trim": "#e8b830", "lining": "#c8dcf4"},
        "wings": {"kind": "angel", "color": "#f2f0f6", "color2": "#c8dcf4", "glow": "#ffd870", "span": 1.5},
        "halo": {"kind": "gears", "color": "#e8b830", "glow": "#ffd870", "radius": 1.5},
        "horns": {"kind": "antlers", "color": "#e8b830", "color2": "#fff0b0", "glow": "#ffd870", "size": 1.2},
        "props": {"float": {"kind": "grimoire", "color": "#e8b830", "color2": "#f2f0f6", "glow": "#fff0b0"}},
        "glow": {"color": "#ffd870", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """The divine form's second and third pair of wings (the kit builds one pair): two more feathered pairs, shorter and lower, built with the kit's own feather routine on the SAME
    wing bones, so all three pairs flap together in every clip. Placed from the measured body. Form 1 has none."""
    if rig.form != 2:
        return
    d = rig.d
    base_len = max(14.0, d.top * 0.55) * rig.spec["wings"]["span"]
    cols, cols2 = kit.shades("#e8f0fa", 5, 1.0), kit.shades("#c8dcf4", 5, 1.0)
    glow = kit.accessories._glow4("#ffd870", 150)
    for drop, scale in ((5.0, 0.78), (10.0, 0.58)):
        kit.accessories._wing_feathers(rig, (2.4, d.sh_y - 2.0 - drop, d.td / 2 + 0.8), base_len * scale, cols, cols2, glow, rows=1, count=6)
