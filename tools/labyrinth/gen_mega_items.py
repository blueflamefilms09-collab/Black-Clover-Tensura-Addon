#!/usr/bin/env python3
"""
Assets of the Mega-Dungeon's guard systems (1.54.0, docs/labyrinth/mega_guard.md): the Mana Core's 16x16 inventory sprite and its item
models, the sector Gatekeeper door (its textures, models and the four blockstate variants of `kind=sector`) and the Guardian Altar (its
three textures, its model, blockstate and item model). Pillow only, deterministic (seeded), idempotent: running it again writes the same
bytes, and it only ever ADDS to the shared Gatekeeper blockstate (nothing is removed).

    python3 tools/labyrinth/gen_mega_items.py            # write everything
    python3 tools/labyrinth/gen_mega_items.py --check    # exit 1 unless every file on disk is what the script writes
    python3 tools/labyrinth/gen_mega_items.py --sheet out.png   # a contact sheet of the textures, 10x, to look at them

Why one script for an item, a door and an altar: they share ONE palette (the mana colours, teal and violet on dark iron and deepslate) and
the whole point of the set is that a player reads door, altar and core as three parts of one lock. The 3D in-hand model of the Mana Core is
NOT here: it is built by `tools/gen_weapon_models.py` (the `mana_core` function), which places the mesh on THIS sprite's box, so run this
script first (`python3 tools/labyrinth/gen_mega_items.py && python3 tools/gen_weapon_models.py mana_core`).
"""
import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))
import gen_labyrinth_seal_assets as seal  # noqa: E402

RES = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources"))
ASSETS = os.path.join(RES, "assets", "nusmp")

TEAL = (70, 214, 186)
TEAL_HI = (176, 255, 240)
TEAL_LO = (28, 110, 112)
MANA = (150, 96, 236)
MANA_HI = (214, 176, 255)
MANA_LO = (72, 44, 130)
INK = (24, 20, 34)
GOLD, GOLD_DARK = seal.GOLD, seal.GOLD_DARK


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def put(img, x, y, rgb, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (rgb[0], rgb[1], rgb[2], a))


# ================================================================================================================ the Mana Core sprite
def core_sprite():
    """16x16: a hexagonal teal-violet crystal in the middle, a bright core in its heart, and a floating ellipse of runes round its belt
    (the back half behind the crystal, the front half over it). The tip and the belt reach the edge of the sprite on purpose: the 3D model
    is fitted to this sprite's box, and a small sprite would make the held object small."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.0
    ring_cy, ring_rx, ring_ry = 8.6, 7.6, 2.9      # the ring is an ellipse seen from slightly above: wide, shallow

    def ellipse_d(x, y):    # 1.0 on the ellipse's centre line
        return math.hypot((x - cx) / ring_rx, (y - ring_cy) / ring_ry)

    def crystal(x, y):      # 0 outside; else the facet (1 left light teal, 2 right violet, 3 the bright upper-left bevel)
        dy = y - cy
        ay = abs(dy)
        if ay >= 7.4:
            return 0
        half = 3.3 * (1.0 - (ay / 7.4) ** 1.6) + 0.35
        if abs(x - cx) > half:
            return 0
        if x < cx - 0.4:
            return 3 if (dy < -1.5 and x < cx - 0.9) else 1
        return 2

    back, front = {}, {}
    for y in range(16):
        for x in range(16):
            d = ellipse_d(x + 0.5, y + 0.5)
            if 0.74 <= d <= 1.26:
                (back if y + 0.5 < ring_cy else front)[(x, y)] = d
    # back arc of the ring (behind the crystal)
    for (x, y), d in back.items():
        put(img, x, y, mix(MANA_LO, MANA, 0.55) if d < 1.0 else mix(INK, MANA_LO, 0.9))
    # the crystal
    for y in range(16):
        for x in range(16):
            f = crystal(x + 0.5, y + 0.5)
            if not f:
                continue
            dy = abs(y + 0.5 - cy) / 7.4
            if f == 1:
                c = mix(TEAL, TEAL_HI, max(0.0, 0.55 - dy)) if dy < 0.55 else mix(TEAL, TEAL_LO, (dy - 0.55) * 1.6)
            elif f == 3:
                c = mix(TEAL_HI, (255, 255, 255), 0.35)
            else:
                c = mix(MANA, MANA_HI, max(0.0, 0.4 - dy) * 1.3) if dy < 0.4 else mix(MANA, MANA_LO, (dy - 0.4) * 1.5)
            if abs(x + 0.5 - cx) < 0.55:
                c = mix(c, (255, 255, 255), 0.18)
            put(img, x, y, c)
    # the bright core in the heart
    for (x, y, k) in ((7, 6, 1.0), (8, 6, 1.0), (7, 7, 0.9), (8, 7, 0.9), (7, 5, 0.5), (8, 5, 0.4), (7, 8, 0.4), (8, 8, 0.45), (6, 6, 0.35), (9, 7, 0.35)):
        put(img, x, y, mix(mix(TEAL_HI, MANA_HI, 0.35), (255, 255, 255), k))
    # the front arc of the ring over the crystal: a dark violet band with teal-white rune dashes in it
    for (x, y), d in sorted(front.items()):
        c = mix(MANA_LO, MANA, 0.55) if d < 0.98 else mix(MANA, MANA_HI, 0.3)
        if (x + (0 if y % 2 else 1)) % 3 == 0 and 0.84 < d < 1.18:
            c = TEAL_HI if (x // 3) % 2 == 0 else TEAL
        put(img, x, y, c)
    # runes on the back arc, where it shows past the crystal
    for (x, y), d in sorted(back.items()):
        if abs(x + 0.5 - cx) > 3.6 and (x * 2 + y) % 5 == 0 and 0.84 < d < 1.16:
            put(img, x, y, TEAL)
    # a one pixel dark outline round everything, like the other inventory icons of the mod
    solid = {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3] > 0}
    for (x, y) in solid:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                q = (x + dx, y + dy)
                if q not in solid and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    put(img, q[0], q[1], INK)
    return img


def core_item_models():
    """models/item/mana_core.json: the 3D held model (builtin/entity, drawn by client/WeaponRenderer from weapon_meshes/mana_core.txt) with the
    display transforms of an upright held object (the Hourglass's), and mana_core_icon.json: the flat sprite the inventory shows."""
    def t(rot, tr, sc):
        return {"rotation": rot, "translation": tr, "scale": [sc, sc, sc]}
    display = {
        "thirdperson_righthand": t([0, 0, 0], [0, 3, 1], 0.62),
        "firstperson_righthand": t([0, -90, 25], [1.13, 3.2, 1.13], 0.7),
        "ground": t([0, 0, 0], [0, 3, 0], 0.55),
        "head": t([0, 180, 0], [0, 13, 7], 1),
        "fixed": t([0, 180, 0], [0, 0, 0], 1),
        "gui": t([0, 0, 0], [0, 0, 0], 1),
    }
    return {
        "models/item/mana_core.json": {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/mana_core"}, "display": display},
        "models/item/mana_core_icon.json": {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/mana_core"}},
    }


# ================================================================================================================ the sector door
def _hex_points(cx, cy, r):
    return [(cx + r * math.cos(math.radians(60 * k)), cy + r * math.sin(math.radians(60 * k))) for k in range(6)]


def _dist_to_segment(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = 0.0 if dx == dy == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def sector_door_frame(phase):
    """One 16x16 frame of the sector door's lower face: the riveted iron plate of every Gatekeeper door, and in its middle a hexagonal socket
    (the shape of the Mana Core's crystal) outlined in teal and violet, a dark well inside it and a heart that pulses between the two mana
    colours; six gold rune ticks sit outside the corners. The warden door is violet, the boss door crimson, the puzzle door teal; this one
    is both of the mana colours, so the four read apart across a hall."""
    rng = random.Random(54)
    img = seal.iron_plate(rng)
    glow = 0.5 + 0.5 * math.sin(phase * math.pi * 2)
    cx = cy = 7.5
    hexpts = _hex_points(cx, cy, 5.4)
    for y in range(16):
        for x in range(16):
            d = min(_dist_to_segment(x, y, hexpts[k][0], hexpts[k][1], hexpts[(k + 1) % 6][0], hexpts[(k + 1) % 6][1]) for k in range(6))
            inside = math.hypot(x - cx, y - cy) < 4.3
            if d < 0.75:
                put(img, x, y, mix(TEAL, MANA, 0.5 + 0.5 * math.sin(math.atan2(y - cy, x - cx) + phase * math.pi * 2)))
            elif inside:
                put(img, x, y, mix(seal.IRON_DARK, MANA_LO, 0.35 + 0.25 * glow))
    heart = mix(TEAL_HI, MANA_HI, glow)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        put(img, x, y, heart)
    for (x, y) in ((7, 6), (8, 9), (6, 8), (9, 7)):
        put(img, x, y, mix(MANA, heart, 0.5 + 0.3 * glow))
    for p in hexpts:
        rx, ry = int(round(cx + (p[0] - cx) * 1.28)), int(round(cy + (p[1] - cy) * 1.28))
        if 0 <= rx < 16 and 0 <= ry < 16:
            put(img, rx, ry, GOLD)
    return img


def sector_door_bottom():
    sheet = Image.new("RGBA", (16, 64))
    for f in range(4):
        sheet.paste(sector_door_frame(f / 4.0), (0, f * 16))
    return sheet


def sector_door_top():
    """The upper face: the plate under a rhombus of gold studs, with a violet chain hanging from its point and a teal gem at the top: a
    crystal gate, not the pointed arch of the warden and boss doors."""
    rng = random.Random(61)
    img = seal.iron_plate(rng)
    pts = [(7, 2), (8, 2), (6, 3), (9, 3), (5, 4), (10, 4), (4, 5), (11, 5), (5, 6), (10, 6), (6, 7), (9, 7), (7, 8), (8, 8)]
    for (x, y) in pts:
        put(img, x, y, GOLD)
    put(img, 7, 4, TEAL_HI)
    put(img, 8, 4, TEAL)
    put(img, 7, 5, TEAL)
    put(img, 8, 5, TEAL_LO)
    for y in range(9, 16):
        put(img, 7, y, mix(MANA, seal.IRON_LIGHT, 0.4))
        put(img, 8, y, mix(MANA_LO, seal.IRON_DARK, 0.4))
    return img


# ================================================================================================================ the Guardian Altar
def _stone(seed, base, dark, light):
    return seal.stone(seed, base=base, dark=dark, light=light, bricks=False)


ALT_BASE, ALT_DARK, ALT_LIGHT = (30, 30, 44), (16, 16, 26), (50, 50, 68)


def altar_side():
    """The plinth's faces: deepslate-dark bricks with a gold trim line and a band of teal rune pixels with violet gaps."""
    img = seal.stone(71, base=ALT_BASE, dark=ALT_DARK, light=ALT_LIGHT)
    for x in range(16):
        put(img, x, 3, GOLD_DARK)
        put(img, x, 4, GOLD)
    for x in range(1, 15, 2):
        put(img, x, 11, TEAL if (x // 2) % 2 == 0 else MANA)
        if (x // 2) % 3 == 0:
            put(img, x, 12, mix(TEAL, ALT_DARK, 0.45))
    return img


def altar_top():
    """The crown: dark stone with a hexagon of teal, a violet inner ring and a bright heart where the Guardian rises, four gold studs."""
    img = _stone(73, ALT_BASE, ALT_DARK, ALT_LIGHT)
    cx = cy = 7.5
    hexpts = _hex_points(cx, cy, 6.2)
    for y in range(16):
        for x in range(16):
            d = min(_dist_to_segment(x, y, hexpts[k][0], hexpts[k][1], hexpts[(k + 1) % 6][0], hexpts[(k + 1) % 6][1]) for k in range(6))
            r = math.hypot(x - cx, y - cy)
            if d < 0.7:
                put(img, x, y, TEAL)
            elif 3.0 <= r <= 3.9:
                put(img, x, y, MANA)
            elif r < 1.7:
                put(img, x, y, mix(TEAL_HI, (255, 255, 255), 0.5))
            elif r < 3.0:
                put(img, x, y, mix(ALT_DARK, MANA_LO, 0.5))
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, GOLD)
    return img


def altar_pillar():
    """The four corner pillars: a dark shaft with a vertical thread of teal runes, gold collars at the top and the foot."""
    img = _stone(75, ALT_BASE, ALT_DARK, ALT_LIGHT)
    for x in range(16):
        for y in (0, 1, 14, 15):
            put(img, x, y, GOLD if y in (1, 14) else GOLD_DARK)
    for y in range(3, 14):
        put(img, 7, y, TEAL if y % 2 == 0 else mix(TEAL, MANA, 0.6))
        put(img, 8, y, mix(TEAL_LO, ALT_DARK, 0.3))
    for (x, y) in ((3, 5), (12, 9), (4, 11), (11, 4)):
        put(img, x, y, MANA_LO)
    return img


def altar_models():
    tex = {"particle": "nusmp:block/guardian_altar_side", "side": "nusmp:block/guardian_altar_side", "top": "nusmp:block/guardian_altar_top",
           "pillar": "nusmp:block/guardian_altar_pillar", "bottom": "minecraft:block/polished_deepslate"}

    def el(a, b, side, up, down="#bottom"):
        return {"from": a, "to": b, "faces": {"north": {"texture": side}, "south": {"texture": side}, "east": {"texture": side}, "west": {"texture": side},
                                              "up": {"texture": up}, "down": {"texture": down}}}
    elements = [el([0, 0, 0], [16, 4, 16], "#side", "#top"),                                  # the base slab
                el([3, 4, 3], [13, 9, 13], "#side", "#top"),                                  # the plinth
                el([5, 9, 5], [11, 13, 11], "#side", "#top")]                                 # the crown
    for (x0, z0) in ((0, 0), (13, 0), (0, 13), (13, 13)):                                     # the four rune pillars
        elements.append(el([x0, 4, z0], [x0 + 3, 16, z0 + 3], "#pillar", "#pillar"))
    return {
        "models/block/guardian_altar.json": {"parent": "minecraft:block/block", "textures": tex, "elements": elements},
        "blockstates/guardian_altar.json": {"variants": {"": {"model": "nusmp:block/guardian_altar"}}},
        "models/item/guardian_altar.json": {"parent": "nusmp:block/guardian_altar"},
    }


def sector_door_models():
    out = {}
    for half, tex in (("lower", "gatekeeper_door_sector_bottom"), ("upper", "gatekeeper_door_sector_top")):
        out["models/block/gatekeeper_door_sector_%s.json" % half] = {"parent": "minecraft:block/cube", "textures": {
            "particle": "nusmp:block/" + tex, "down": "nusmp:block/gatekeeper_door_end", "up": "nusmp:block/gatekeeper_door_end",
            "north": "nusmp:block/" + tex, "south": "nusmp:block/" + tex, "east": "nusmp:block/" + tex, "west": "nusmp:block/" + tex}}
    return out


DOOR_STATES = {"kind=sector,open=false,half=lower": {"model": "nusmp:block/gatekeeper_door_sector_lower"},
               "kind=sector,open=true,half=lower": {"model": "nusmp:block/gatekeeper_door_open"},
               "kind=sector,open=false,half=upper": {"model": "nusmp:block/gatekeeper_door_sector_upper"},
               "kind=sector,open=true,half=upper": {"model": "nusmp:block/gatekeeper_door_open"}}


# ================================================================================================================ the file set
def png_bytes(img):
    b = io.BytesIO()
    img.save(b, format="PNG", optimize=True)
    return b.getvalue()


def js(obj):
    return (json.dumps(obj, indent=2) + "\n").encode()


def build():
    """published relative path (under assets/nusmp) -> bytes, for every file this script owns outright."""
    out = {}
    out["textures/item/mana_core.png"] = png_bytes(core_sprite())
    for path, obj in {**core_item_models(), **altar_models(), **sector_door_models()}.items():
        out[path] = js(obj)
    out["textures/block/gatekeeper_door_sector_bottom.png"] = png_bytes(sector_door_bottom())
    out["textures/block/gatekeeper_door_sector_bottom.png.mcmeta"] = js({"animation": {"frametime": 6}})
    out["textures/block/gatekeeper_door_sector_top.png"] = png_bytes(sector_door_top())
    out["textures/block/guardian_altar_side.png"] = png_bytes(altar_side())
    out["textures/block/guardian_altar_top.png"] = png_bytes(altar_top())
    out["textures/block/guardian_altar_pillar.png"] = png_bytes(altar_pillar())
    return out


def shared_files():
    """The one file another part of the mod owns, with this script's additions merged in (only appended, nothing removed): path -> bytes."""
    p = os.path.join(ASSETS, "blockstates", "gatekeeper_door.json")
    st = json.load(open(p))
    for k, v in DOOR_STATES.items():
        st["variants"].setdefault(k, v)
    return {"blockstates/gatekeeper_door.json": js(st)}


def write():
    n = 0
    for rel, data in {**build(), **shared_files()}.items():
        path = os.path.join(ASSETS, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        if os.path.exists(path) and open(path, "rb").read() == data:
            continue
        with open(path, "wb") as f:
            f.write(data)
        n += 1
    print("%d file(s) written" % n)
    return 0


def check():
    bad = 0
    for rel, data in {**build(), **shared_files()}.items():
        path = os.path.join(ASSETS, *rel.split("/"))
        if not os.path.exists(path):
            print("missing " + rel)
            bad += 1
        elif open(path, "rb").read() != data:
            print("stale   " + rel)
            bad += 1
    sizes = {"textures/item/mana_core.png": (16, 16), "textures/block/gatekeeper_door_sector_bottom.png": (16, 64)}
    for rel, data in build().items():
        if rel.endswith(".png"):
            im = Image.open(io.BytesIO(data))
            want = sizes.get(rel, (16, 16))
            if im.size != want or im.mode != "RGBA":
                print("bad size/mode %s %s %s" % (rel, im.size, im.mode))
                bad += 1
    print("check:", "ok" if not bad else "%d problem(s)" % bad)
    return 1 if bad else 0


def sheet(path):
    names = [r for r in build() if r.endswith(".png")]
    cell = 16 * 10
    cols = 4
    rows = (len(names) + cols - 1) // cols
    img = Image.new("RGBA", (cols * (cell + 10), rows * (cell + 10)), (46, 46, 56, 255))
    for k, rel in enumerate(names):
        im = Image.open(io.BytesIO(build()[rel])).convert("RGBA").crop((0, 0, 16, 16)).resize((cell, cell), Image.NEAREST)
        img.paste(im, ((k % cols) * (cell + 10) + 5, (k // cols) * (cell + 10) + 5), im)
    img.save(path)
    print(path, len(names), "textures")
    return 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--sheet", metavar="PNG")
    a = ap.parse_args()
    sys.exit(check() if a.check else sheet(a.sheet) if a.sheet else write())
