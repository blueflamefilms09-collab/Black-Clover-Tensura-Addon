"""Why: the old witch of the Black Market, the keeper of the hidden, dusty shop the owner asked for ("run by an old witch selling high-tier / rare skill books at exorbitant prices"),
a friend of the Sanctuary Hub (an NPC; the record is in skilltree/shop/BlackMarketWitch.java, her words in data/nusmp/arc/dialogue/black_market_witch.json, her shop in
skilltree/shop/). The owner's text gives no name and no description beyond "old witch", so the look is a design, not canon: a stooped crone in a crooked pointed hat, a dusty plum
robe patched with soot-grey cloth under a ragged soot-grey shawl, stringy ash-white hair, a sallow face with a hooked nose, a wart, one amber eye and an eyepatch over the other, a
clay pipe, a belt of dusty books and coin pouches (she sells books and takes coin), and a gnarled staff that carries a lantern on a chain (her lamplight is the only glow, amber, the
colour the shop screen uses for gold). The stoop comes from a hump built under the shawl (`extra`) and the `slouch` idle. Unverified: how the hump reads in the game's light.
See docs/arc/art_kit.md for how the spec fields work.
"""
import gen_arc_kit as kit


def p_witch_staff(pb):
    """A gnarled staff of dark root-wood that zig-zags up to a burl with three crooked claws, and a lantern on a short chain from a hook at the top. Authored standing up (the kit's
    prop convention): the grip at the origin, +y up the shaft. The lantern hangs along -y, which is straight down while the staff stands upright (its rest hold is near vertical)."""
    L = pb.L(48.0)
    wood = pb.leather(pb.col2 or "#4a3424", 5)
    iron = pb.metal("#34343c", 4)
    seg = L / 5.0
    (tx, ty, tz), ang = pb.curve((0.0, -8.0, 0.0), ((seg, 0.0), (seg, 6.0), (seg, -11.0), (seg, 10.0), (seg * 0.9, -7.0)), 1.6, wood, thick=1.6, taper=0.96)
    # the burl at the top and three claws curling round where a crystal would sit
    pb.box(tx - 1.9, ty - 0.4, tz - 1.9, 3.8, 3.4, 3.8, wood, rot=(0, 0, -ang + 12.0), pivot=(tx, ty, tz))
    pb.box(tx - 2.6, ty + 1.4, tz - 0.5, 1.0, 4.4, 1.0, wood, rot=(0, 0, 18.0), pivot=(tx - 2.1, ty + 1.4, tz))
    pb.box(tx + 1.6, ty + 1.4, tz - 0.5, 1.0, 4.0, 1.0, wood, rot=(0, 0, -22.0), pivot=(tx + 2.1, ty + 1.4, tz))
    pb.box(tx - 0.5, ty + 1.6, tz + 1.6, 1.0, 4.2, 1.0, wood, rot=(14.0, 0, 0), pivot=(tx, ty + 1.6, tz + 2.1))
    # the hook and the chain of the lantern, on the side of the shaft (+x)
    hx, hy = tx + 2.7, ty - 0.8
    pb.box(hx - 0.3, hy - 0.4, tz - 0.3, 0.6, 1.6, 0.6, iron)
    pb.box(hx - 0.3, hy - 1.0, tz - 0.3, 1.6, 0.6, 0.6, iron)
    for k in range(3):
        pb.box(hx + 0.8 - (0.15 if k % 2 else 0.0), hy - 1.8 - k * 1.0, tz - 0.25, 0.5, 0.9, 0.5, iron)
    # the lantern: iron caps and posts round a glowing core
    ly = hy - 5.0
    lx = hx + 0.55
    pb.box(lx - 1.6, ly + 3.6, tz - 1.6, 3.2, 0.7, 3.2, iron)
    pb.box(lx - 1.0, ly + 4.3, tz - 1.0, 2.0, 0.7, 2.0, iron)
    pb.box(lx - 1.6, ly - 0.2, tz - 1.6, 3.2, 0.7, 3.2, iron)
    g = None if pb.glow is None else (pb.glow[0], pb.glow[1], pb.glow[2], 235)
    flame = kit.metal(kit.shades(pb.glow or (255, 176, 64), 5, 1.15), 11, glow=g if g else (255, 176, 64, 230), emboss=False, rim=False)
    pb.box(lx - 1.0, ly + 0.5, tz - 1.0, 2.0, 3.1, 2.0, flame)
    for sx in (-1.5, 1.0):
        for sz in (-1.5, 1.0):
            pb.box(lx + sx - 0.0, ly + 0.5, tz + sz - 0.0, 0.5, 3.1, 0.5, iron)


kit.register_prop("witch_staff", kit.PropDef(p_witch_staff, attack="cast_jab", cast="gesture", orient=((0.0, 0.0, 1.0), "forward")))
kit.recipes.HOLD["witch_staff"] = kit.recipes.HOLD["staff"]

SPEC = {
    "id": "black_market_witch",
    "name": "Old Witch",
    "role": "npc",
    "build": "slim",
    "height": 1.72,
    "skin": "olive",
    "face": {"expression": "sinister", "eyes": "narrow", "eye_color": "#f0b030", "brows": "heavy", "nose": "sharp", "mouth": "grin",
             "marks": ["cheek_lines", "eye_bags", "mole"]},
    "hair": {"style": "bob", "color": "#9a94a6", "streak": "#5e5868"},
    "outfit": {
        "top": {"kind": "robe", "color": "#47285a", "trim": "#8a6a3a", "length": "floor", "collar": "high", "pattern": "patches", "pattern_color": (92, 86, 98), "hem_trim": True},
        "sleeves": {"kind": "wide", "cuff": "#8a6a3a"},
        "legs": {"kind": "trousers", "color": "#2c2028"},
        "boots": {"kind": "boots", "color": "#2a2024", "height": "ankle"},
        "gloves": {"kind": "wraps", "color": "#7a6a56"},
        "belt": {"kind": "double", "color": "#3a2a22", "buckle": "#c8a040", "pouches": True, "books": True},
    },
    "cape": {"kind": "cloak", "color": "#4d4853", "trim": "#8a6a9a", "length": 0.8, "ragged": True, "width": 0.9, "clasp": "#c8a040"},
    "accessories": [
        {"kind": "witch_hat", "color": "#2b1a38", "color2": "#c8a040"},
        {"kind": "eyepatch", "color": "#1a1620", "side": "left"},
        {"kind": "pipe", "color": "#5a3c28", "glow": "#ff9a30"},
        {"kind": "earrings", "color": "#c8a040"},
    ],
    "props": {"right": {"kind": "witch_staff", "color2": "#4a3424", "glow": "#ffb040"}},
    "glow": {"color": "#ffb040", "eyes": True, "props": True, "trim": False},
    "clips": {"attack": "cast_jab", "cast": "gesture", "idle": "slouch"},
    "notes": "NPC of the Black Market. The lantern on the staff and her one visible eye glow amber; the pipe's ember is the only other light. The hump under the shawl is built in extra().",
}


def extra(kit, rig):
    """The stoop, part one: a hump between the shoulder blades made of the SHAWL's own cloth (same material as the cloak, so it reads as the cloak bulging over a bent back, not as
    a plum lump showing through it). Placed from the measured body (`rig.d`), never from fixed numbers, so it follows the build."""
    d = rig.d
    cloth = kit.mat("heavy", kit.rgb("#4d4853"), 97, seam=False)
    rig.box("body", (-d.tw / 2 + 0.9, d.sh_y - 6.0, d.td / 2 + 0.2), (d.tw - 1.8, 5.2, 2.2), cloth)


STOOP = 6.0


def post(kit, rig):
    """The stoop, part two: every clip leans the whole body a few degrees further forward and lifts the head a little to keep her eyes on the customer, by the same constant in
    every key, so a looping clip still ends where it began and the arms (solved against the body) keep their hold on the staff."""
    for clip in rig.A.clips.values():
        bones = clip["bones"]
        for bone, dx in (("body", -STOOP), ("head", STOOP * 0.5)):
            track = bones.get(bone, {}).get("rotation")
            if not track:
                continue
            for key in track:
                track[key][0] += dx
