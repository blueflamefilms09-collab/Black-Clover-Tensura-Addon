"""Why: the God of Order (entity nusmp:god_of_order, relics/order/GodOfOrder), the arbiter that comes for a second copy of a relic. The old hand-built model is replaced by the art
kit's player-proportioned character (docs/arc/remodel.md), keeping its design: tall (3.4 blocks) and slender in a white and gold robe with a stole of ledger glyphs, a shoulder mantle
and a long cape, a hood over a porcelain mask (no mouth, two slits of light for eyes), a crown of gold prongs, a halo of ledger glyphs that turns behind the shoulders, and a long
measuring staff graduated like a rule. `form2` is the ledger opened (100% authority): a second, larger counter-turning ring and six pages that orbit it (made in `extra`).
It also has the three clips only this boss plays, made in `post` with tools/remodel/lib.py: `read` (arms open while it reads its verdict), `beam` (the staff levelled at the
holder) and `bow` (it leaves). Colours are the game's, not canon.
"""
import math

from remodel import lib

ENTITIES = ["god_of_order"]

SPEC = {
    "id": "god_of_order",
    "name": "God of Order",
    "role": "boss",
    "build": "lanky",
    "height": 3.2,
    "skin": "pale",
    "face": {"expression": "calm", "eyes": "glow", "eye_color": "#ffe9a0", "eye_glow": "#ffe08a", "brows": "none", "mouth": "none", "nose": "none",
             "mask": {"kind": "full", "color": "#f4efe4", "color2": "#c8a850", "eye_glow": "#ffe08a"}},
    "hair": {"style": "hood", "color": "#f2eee4", "cover": "#f2eee4"},
    "outfit": {
        "top": {"kind": "robe", "color": "#f2eee4", "trim": "#d8b448", "length": "floor", "collar": "high", "collar_color": "#e8e2d0", "hem_trim": True, "pattern": "runes",
                "pattern_color": (220, 184, 80), "emblem": {"kind": "diamond", "where": "chest", "size": 7, "glow": "#ffe08a"}},
        "inner": {"kind": "shirt", "color": "#e8e2d0"},
        "sleeves": {"kind": "bell", "color": "#f2eee4", "cuff": "#d8b448"},
        "legs": {"kind": "trousers", "color": "#e8e2d0"},
        "boots": {"kind": "boots", "color": "#d8b448", "height": "ankle"},
        "gloves": {"kind": "gloves", "color": "#f2eee4"},
        "belt": {"kind": "sash", "color": "#d8b448", "buckle": "#fff0b0"},
        "neck": {"kind": "necklace", "color": "#d8b448", "glow": True},
    },
    "cape": {"kind": "cloak", "color": "#f6f2e8", "trim": "#d8b448", "length": 1.0, "width": 1.1, "lining": "#e8dcb8", "clasp": "#d8b448", "hood": False},
    "armor": {"pieces": ["pauldrons", "gorget"], "style": "plate", "color": "#e8d28a", "trim": "#fff0b0", "glow": "#ffe08a"},
    "accessories": [{"kind": "crown", "color": "#e8c04a", "color2": "#fff0b0"}],
    "halo": {"kind": "gears", "color": "#d8b448", "glow": "#ffe08a", "radius": 1.4},
    "props": {"right": {"kind": "staff", "color": "#f2eee4", "color2": "#d8b448", "glow": "#ffe08a", "length": 1.15}},
    "glow": {"color": "#ffe08a", "eyes": True, "props": True, "trim": True},
    "clips": {"attack": "cast_jab", "cast": "magic", "idle": "proud"},
    "form2": {
        "name": "God of Order, Ledger Open",
        "armor": {"pieces": ["pauldrons", "gorget", "bracers", "tassets"], "color": "#f0dc9a"},
        "halo": {"kind": "flames", "radius": 1.9, "color": "#e8c04a"},
        "outfit": {"top": {"trim": "#fff0b0"}},
        "glow": {"color": "#fff0b0", "eyes": True, "props": True, "trim": True},
    },
}


def extra(kit, rig):
    """Form 2: the six pages of the open ledger that orbit the God (thin glowing sheets on a spinning bone, as the old model had), placed from the measured body."""
    if rig.form != 2:
        return
    d = rig.d
    paper = kit.metal(kit.shades("#f6f0dc", 5, 0.8), 51, glow=(255, 224, 138, 190), emboss=False)
    rig.bone("pages", "body", (0.0, d.sh_y - 6.0, 0.0))
    r = d.tw * 1.4 + 9.0
    for k in range(6):
        a = k * math.pi / 3.0
        y = d.sh_y - 12.0 + 7.0 * (k % 3)
        rig.box("pages", (r * math.sin(a) - 3.0, y, r * math.cos(a) - 0.3), (6.0, 8.0, 0.6), paper, rotation=(0, math.degrees(a) + 90.0, 0), pivot=(r * math.sin(a), y, r * math.cos(a)))
    rig.animate("pages", spin=0.1, spin_axis=1)


def post(kit, rig):
    """read, beam and bow: the poses the visit's phases play (the renderer picks them from the synced phase of the visit). Each is another clip of the model held by
    lib.pose_clip, so the arms stay solved against the staff by the kit: `beam` is the attack's thrust frozen at its hit (the staff levelled at the holder), `read` the middle of the
    cast_big wind-up (both arms open, the free hand writing the air), `bow` the idle with the body leaned forward and the arms turned back by the same angle so the staff stays upright.
    Rotations are added to the rest pose (positive x swings a limb forward, negative x leans the body forward)."""
    lib.pose_clip(rig, "beam", "attack", 0.38, 2.4, loop=True, ramp=0.3, sway={"head": (1.0, 0, 0)})
    lib.pose_clip(rig, "read", "cast_big", 1.1, 2.4, loop=True, ramp=0.4, sway={"head": (2.0, 0, 0)})
    lib.pose_clip(rig, "bow", "idle", 0.0, 2.0, adds={"body": (-26, 0, 0), "head": (-10, 0, 0), "arm_l": (26, 0, 0), "arm_r": (26, 0, 0)}, ramp=0.7, loop=False)
