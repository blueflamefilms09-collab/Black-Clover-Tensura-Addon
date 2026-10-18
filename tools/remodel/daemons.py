"""Why: the four tiers of Zagred's Grimoire Daemons (lesser, greater, arch, rule stone: entity nusmp:grimoire_daemon, one entity whose synced tier picks the look) share one design
(the old model's description: "a floating tattered robe and hood, thin arms holding a grimoire out in front that snaps open and flips its pages while it casts; horns from the
greater tier, two pairs of membrane wings for the arch tier, or just a glyph slab for the Overwrite rule stones"), and differ in the palette of tools/gen_daemon_textures.py,
the number of leaves of the grimoire and the size. Each tier is its own character file (tools/arc/char_grimoire_daemon*.py, the art kit wants one body per file); this module is the
one place the shared parts live, so the four files stay short and cannot drift apart. It does not touch the kit: it only returns spec dicts and calls the kit's public hooks.
"""
import math

# palette per tier: robe, trim, hood, glow, cover (from tools/gen_daemon_textures.py TIERS, the colours the old skins had)
TIERS = {
    "lesser": dict(robe="#4a4850", trim="#8a828a", hood="#34303c", glow="#ffbe5a", cover="#3a3844", height=1.5, build="slim"),
    "greater": dict(robe="#701a2a", trim="#d6402e", hood="#3a1018", glow="#ff6e32", cover="#8a1a26", height=2.3, build="average"),
    "arch": dict(robe="#46347c", trim="#d6aa46", hood="#241a44", glow="#be96ff", cover="#3a2468", height=3.2, build="average"),
    "stone": dict(robe="#6a6e78", trim="#9aa0ac", hood="#52565e", glow="#ffd870", cover="#4a4e58", height=1.7, build="average"),
}


def spec(tier, daemon_id):
    t = TIERS[tier]
    stone = tier == "stone"
    out = {
        "id": daemon_id,
        "name": "Grimoire Daemon" if not stone else "Rule Stone",
        "role": "boss",
        "build": t["build"],
        "height": t["height"],
        "skin": "grey" if stone else "ashen",
        "face": {"expression": "dead" if stone else "cold", "eyes": "glow" if not stone else "empty", "eye_color": t["glow"], "eye_glow": t["glow"], "brows": "none", "mouth": "none",
                 "nose": "none", "marks": ["cracks"] if stone else []},
        "hair": {"style": "hood", "color": t["hood"], "cover": t["hood"]},
        "outfit": {
            "top": {"kind": "robe", "color": t["robe"], "trim": t["trim"], "length": "floor", "collar": "wide" if tier == "arch" else "stand", "hem_trim": True, "ragged": True,
                    "pattern": "runes", "pattern_color": tuple(int(t["trim"][i:i + 2], 16) for i in (1, 3, 5)),
                    "material": "plate" if stone else None,
                    "emblem": {"kind": "rune", "where": "chest", "size": 8 if stone else 6, "glow": t["glow"]}},
            "sleeves": {"kind": "wide" if not stone else "none", "cuff": t["trim"]},
            "legs": {"kind": "trousers", "color": t["hood"]},
            "boots": {"kind": "bare" if stone else "boots", "color": t["hood"], "height": "ankle"},
            "gloves": {"kind": "none"},
        },
        "cape": None if stone else {"kind": "hooded_cloak", "color": t["robe"], "trim": t["trim"], "length": 0.75, "ragged": True, "hood": True, "lining": t["hood"]},
        "props": {} if stone else {"left": {"kind": "grimoire", "color": t["cover"], "color2": t["trim"], "glow": t["glow"], "length": 0.8 + 0.12 * ("lesser greater arch".split().index(tier))}},
        "glow": {"color": t["glow"], "eyes": True, "props": True, "trim": True},
        "clips": {"attack": "cast_jab" if not stone else "none", "cast": "book" if not stone else "pray", "idle": "hover" if not stone else "relaxed"},
    }
    if tier in ("greater", "arch"):
        out["horns"] = {"kind": "devil" if tier == "greater" else "ram", "color": "#1c1418", "color2": t["trim"], "glow": t["glow"], "size": 1.0 if tier == "greater" else 1.3}
    if tier == "arch":
        out["wings"] = {"kind": "bat", "color": "#4a3a78", "color2": "#8a78c8", "glow": t["glow"], "span": 1.2}
        out["halo"] = {"kind": "ring", "color": t["trim"], "glow": t["glow"], "radius": 1.1}
    return out


def second_wings(kit, rig):
    """The arch tier's second pair of membrane wings: the kit builds one pair, so a lower, shorter pair is built with its own membrane routine on the SAME wing bones (they flap
    together in every clip). Placed from the measured body."""
    d = rig.d
    base_len = max(14.0, d.top * 0.55) * rig.spec["wings"]["span"] * 0.72
    cols, cols2 = kit.shades("#4a3a78", 5, 1.0), kit.shades("#8a78c8", 5, 1.0)
    kit.accessories._wing_membrane(rig, (2.4, d.sh_y - 8.0, d.td / 2 + 0.8), base_len, cols, cols2, kit.accessories._glow4("#be96ff", 120))


def stone_slab(kit, rig):
    """The rule stone's glyph slab: a tall tablet standing behind the idol's shoulders (a few pixels bigger than the body), carved with a glowing glyph. On the body bone, so it
    does not move."""
    d = rig.d
    rock = kit.plate(kit.shades("#8a8e98", 5, 1.2), 33, rivets=False)
    rig.box("body", (-d.tw / 2 - 2.0, d.hip_y - 2.0, d.td / 2 + 0.2), (d.tw + 4.0, d.sh_y - d.hip_y + 10.0, 3.0), rock)
    glyph = kit.metal(kit.shades("#ffd870", 5, 1.1), 35, glow=(255, 216, 112, 230), emboss=False)
    rig.box("body", (-1.0, d.hip_y + 4.0, d.td / 2 + 3.2), (2.0, d.sh_y - d.hip_y, 0.5), glyph)
    rig.box("body", (-3.5, d.sh_y - 3.0, d.td / 2 + 3.2), (7.0, 1.6, 0.5), glyph)
