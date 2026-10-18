"""Why: the Dark Disciple, the hooded acolyte a Zogratis devil-bearer calls up (entity nusmp:dark_disciple, entity/zogratis/DarkDiscipleEntity). It was a hand-built cube
model; the owner asked for the mod's mobs to be player-model characters of the Old Witch's quality, so this is the art kit's version of the same mob: the same hood, the
same bone mask and the same devil tint (Lucifero violet, Megicula crimson, Beelzebub indigo, Astaroth gold, any other devil neutral grey), now a full character with a
painted face under the mask, a ragged robe written with the devil's runes, a ritual dagger and a floating orb of the devil's colour that gathers when it casts.
The skins are the same body repainted: VARIANTS below is read by tools/remodel/gen_remodel.py, which writes only their textures (arc_dark_disciple_<devil>.png).
Look is the game's own (the old model's description), not canon. See docs/arc/remodel.md.
"""

ENTITIES = ["dark_disciple"]          # the entity ids this model draws (tools/remodel/gen_remodel.py and the roster test read it)

GREY = {"cloth": "#5a566a", "trim": "#d8cfb4", "rune": (190, 184, 214), "glow": "#c8c4e8", "orb": "#9c98c0"}


def spec(t):
    return {
        "id": "dark_disciple",
        "name": "Dark Disciple",
        "role": "boss",
        "build": "average",
        "height": 1.9,
        "skin": "ashen",
        "face": {"expression": "cold", "eyes": "narrow", "eye_color": t["glow"], "eye_glow": t["glow"], "brows": "flat",
                 "mask": {"kind": "skull", "color": "#ece4cc", "color2": "#2a2430", "eye_glow": t["glow"]}},
        "hair": {"style": "hood", "color": t["cloth"], "cover": t["cloth"]},
        "outfit": {
            "top": {"kind": "robe", "color": t["cloth"], "trim": t["trim"], "length": "ankle", "collar": "high", "pattern": "runes", "pattern_color": t["rune"],
                    "ragged": True, "hem_trim": True, "emblem": {"kind": "skull", "where": "back", "size": 8, "glow": t["glow"]}},
            "sleeves": {"kind": "wide", "cuff": t["trim"]},
            "legs": {"kind": "trousers", "color": "#2c2a36"},
            "boots": {"kind": "boots", "color": "#2c2a36", "height": "ankle"},
            "gloves": {"kind": "wraps", "color": "#c9c0a8"},
            "belt": {"kind": "sash", "color": "#241f2c", "buckle": t["trim"]},
        },
        "cape": {"kind": "hooded_cloak", "color": t["cloth"], "trim": t["trim"], "length": 0.7, "ragged": True, "hood": True, "lining": t["rune"]},
        "props": {"right": {"kind": "dagger", "color": "#e0d8c0", "color2": "#3a3040", "glow": t["glow"], "length": 0.75},
                  "float": {"kind": "orb", "color": t["orb"], "glow": t["glow"]}},
        "glow": {"color": t["glow"], "eyes": True, "props": True, "trim": True},
        "clips": {"attack": "stab", "cast": "magic", "idle": "slouch"},
        "notes": "The devil's tint is the cloth, the trim runes, the glow and the orb; the body is shared by all five skins.",
    }


SPEC = spec(GREY)

VARIANTS = {
    "lucifero": lambda: spec({"cloth": "#4a3a80", "trim": "#d8c4ff", "rune": (190, 150, 255), "glow": "#9a5cff", "orb": "#7a46d8"}),
    "megicula": lambda: spec({"cloth": "#7a2c3c", "trim": "#ffc8be", "rune": (255, 120, 120), "glow": "#ff3a4a", "orb": "#d02a3a"}),
    "beelzebub": lambda: spec({"cloth": "#34408a", "trim": "#c0caff", "rune": (130, 150, 255), "glow": "#5a70ff", "orb": "#4458d0"}),
    "astaroth": lambda: spec({"cloth": "#806a30", "trim": "#fff0b0", "rune": (255, 220, 110), "glow": "#ffc83a", "orb": "#d8a428"}),
}
