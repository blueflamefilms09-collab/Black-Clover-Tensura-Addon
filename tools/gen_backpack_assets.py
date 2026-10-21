"""The Backpack's art, all of it generated (deterministic, so it can be redone): the five tier textures of the 3D block / item model, the four upgrade-item sprites, the block models
(one per tier, Blockbench-style element models), the item models (one per tier plus the main one that picks by tier), and the blockstate.

    python3 -B tools/gen_backpack_assets.py            writes assets/nusmp/{textures/block,textures/item,models/block,models/item,blockstates}/backpack*
    python3 -B tools/gen_backpack_assets.py sheet X    also writes a contact sheet of the five textures and the four sprites (8x) to X (for a look; not part of the build)

The model is one list of cuboids (ELEMENTS) in the 16-unit grid, front toward north (the blockstate turns it). Every visible face gets its own rectangle of the texture; the
rectangles are packed by a shelf packer into a 128 x 128 atlas at four pixels per model unit (three if it does not fit), and the SAME rectangles are written into the model's uv, so
the picture and the model cannot disagree. Each tier changes the palette: the leather body stays a leather brown (a little darker for each tier), while the trim, the buckle and the
rivets are leather, iron, gold, diamond and netherite. The netherite pack has a violet rune on the flap so it reads as the dungeon tier at a glance.
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

TIERS = ["leather", "iron", "gold", "diamond", "netherite"]
SLOTS = [27, 36, 45, 54, 63]

# body, body shadow, trim, trim shadow, buckle, rune
PALETTE = {
    "leather":   dict(body=(122, 74, 43), trim=(74, 44, 24), buckle=(201, 162, 74), strap=(92, 55, 31), rune=None),
    "iron":      dict(body=(106, 70, 44), trim=(168, 173, 181), buckle=(217, 221, 226), strap=(84, 56, 36), rune=None),
    "gold":      dict(body=(98, 62, 42), trim=(224, 184, 58), buckle=(255, 240, 160), strap=(76, 50, 34), rune=None),
    "diamond":   dict(body=(58, 78, 98), trim=(69, 217, 208), buckle=(184, 255, 246), strap=(44, 60, 78), rune=None),
    "netherite": dict(body=(44, 38, 52), trim=(78, 64, 92), buckle=(176, 92, 255), strap=(34, 30, 42), rune=(210, 150, 255)),
}

F = ("north", "south", "east", "west", "up", "down")
# name, from, to, role, faces
ELEMENTS = [
    ("body",      (3, 0, 5),      (13, 12, 11),    "body",   ("north", "south", "east", "west", "up")),
    ("flap",      (3, 11.5, 4),   (13, 14, 11.5),  "flap",   ("north", "south", "east", "west", "up")),
    ("pocket",    (4.5, 1, 3.5),  (11.5, 7, 5),    "pocket", ("north", "east", "west", "up")),
    ("pocket_l",  (2, 1, 6.5),    (3, 6, 9.5),     "pocket", ("north", "south", "west", "up")),
    ("pocket_r",  (13, 1, 6.5),   (14, 6, 9.5),    "pocket", ("north", "south", "east", "up")),
    ("strap_l",   (4, 3, 11),     (5.5, 12, 11.75), "strap",  ("south", "east", "west", "up")),
    ("strap_r",   (10.5, 3, 11),  (12, 12, 11.75), "strap",  ("south", "east", "west", "up")),
    ("buckle",    (7, 9, 3.6),    (9, 12, 4),      "buckle", ("north", "east", "west", "up")),
    ("handle",    (6, 14, 6.5),   (10, 15.5, 9.5), "strap",  ("north", "south", "east", "west", "up")),
]

ATLAS = 128


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade(c, k):
    return clamp([v * k for v in c])


def mix(a, b, t):
    return clamp([x + (y - x) * t for x, y in zip(a, b)])


def noise(x, y, seed):
    """A small deterministic value in [0, 1) for a pixel (no random module: the same picture every run)."""
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65536.0


def face_size(f, a, b):
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    if f in ("north", "south"):
        return dx, dy
    if f in ("east", "west"):
        return dz, dy
    return dx, dz


def pack(scale):
    """Shelf-packs every visible face; returns {(element, face): (x, y, w, h)} in pixels or None if it does not fit."""
    items = []
    for name, a, b, role, faces in ELEMENTS:
        for f in faces:
            w, h = face_size(f, a, b)
            items.append((name, f, max(2, int(round(w * scale))), max(2, int(round(h * scale)))))
    items.sort(key=lambda t: (-t[3], -t[2]))
    out, x, y, row = {}, 1, 1, 0
    for name, f, w, h in items:
        if x + w + 1 > ATLAS:
            x, y, row = 1, y + row + 1, 0
        if y + h + 1 > ATLAS:
            return None
        out[(name, f)] = (x, y, w, h)
        x += w + 1
        row = max(row, h)
    return out


def paint(role, face, w, h, pal, tier, seed):
    """One face as a list of rows of RGB."""
    body, trim, buckle, strap = pal["body"], pal["trim"], pal["buckle"], pal["strap"]
    base = {"body": body, "flap": mix(body, trim, 0.10), "pocket": shade(body, 0.92), "strap": strap, "buckle": buckle}[role]
    rows = []
    for j in range(h):
        row = []
        for i in range(w):
            n = noise(i, j, seed)
            c = shade(base, 0.90 + 0.18 * n)
            if role in ("body", "flap", "pocket"):
                if (i + j) % 7 == 0 and n > 0.55:
                    c = shade(c, 0.93)                                # leather grain
            if role == "buckle":
                c = mix(buckle, (255, 255, 255), 0.35 * (1 - j / max(1, h - 1))) if face == "north" else shade(buckle, 0.8)
            edge_top, edge_left = j == 0, i == 0
            edge_bottom, edge_right = j == h - 1, i == w - 1
            if role != "buckle":
                if edge_top:
                    c = shade(c, 1.15)
                if edge_bottom:
                    c = shade(c, 0.70)
                if edge_left:
                    c = shade(c, 1.05)
                if edge_right:
                    c = shade(c, 0.80)
            row.append(c)
        rows.append(row)
    # stitching one pixel inside the border, every other pixel, on the big visible faces
    if role in ("body", "flap", "pocket") and face in ("north", "south") and w >= 8 and h >= 6:
        st = shade(base, 0.62)
        for i in range(2, w - 2, 2):
            rows[2][i] = st
            rows[h - 3][i] = st
        for j in range(2, h - 2, 2):
            rows[j][2] = st
            rows[j][w - 3] = st
    # trim: a band along the bottom of the flap's front and the pocket's top edge, in the tier's trim colour with a bright top line
    if role == "flap" and face == "north":
        band = max(2, h // 3)
        for j in range(h - band, h):
            for i in range(w):
                t = shade(trim, 0.85 + 0.2 * noise(i, j, seed + 5))
                rows[j][i] = mix(t, (255, 255, 255), 0.25) if j == h - band else t
    if role == "pocket" and face == "north":
        for i in range(w):
            rows[0][i] = mix(trim, (255, 255, 255), 0.2)
            if h > 3:
                rows[1][i] = trim
        for (ci, cj) in ((1, h - 2), (w - 2, h - 2)):
            if 0 <= ci < w and 0 <= cj < h:
                rows[cj][ci] = buckle
    if role == "strap" and face in ("south", "north") and h >= 8:
        for j in range(1, h - 1, 3):
            rows[j][w // 2] = shade(trim, 1.1)
    # the body's back is padded: a lighter panel with a vertical seam
    if role == "body" and face == "south" and w >= 8:
        for j in range(3, h - 3):
            for i in range(3, w - 3):
                rows[j][i] = mix(rows[j][i], trim, 0.12)
        for j in range(2, h - 2):
            rows[j][w // 2] = shade(body, 0.65)
    # side and top faces carry the trim colour on their rim so the tier reads from every angle
    if role in ("body", "flap") and face in ("east", "west"):
        for j in range(h):
            rows[j][0 if face == "west" else w - 1] = shade(trim, 0.9)
    if role == "flap" and face == "up":
        for i in range(w):
            rows[0][i] = trim
            rows[h - 1][i] = trim
    # the netherite rune on the flap's front: a small diamond with a point in the middle
    if tier == "netherite" and role == "flap" and face == "north" and pal["rune"]:
        r = pal["rune"]
        cx, cy = w // 2, max(1, (h - max(2, h // 3)) // 2)
        for d in range(0, 2):
            for (dx, dy) in ((d, 0), (-d, 0), (0, d), (0, -d)):
                x, y = cx + dx, cy + dy
                if 0 <= x < w and 0 <= y < h - max(2, h // 3):
                    rows[y][x] = r
    return rows


def write_texture(tier, layout, path):
    pal = PALETTE[tier]
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    px = img.load()
    for (name, f), (x, y, w, h) in layout.items():
        role = [e[3] for e in ELEMENTS if e[0] == name][0]
        rows = paint(role, f, w, h, pal, tier, sum(map(ord, name)) + ord(f[0]))
        for j in range(h):
            for i in range(w):
                px[x + i, y + j] = tuple(rows[j][i]) + (255,)
    # a small plain patch at the corner for the particle texture
    img.save(path)


def model_json(tier, layout):
    elements = []
    for name, a, b, role, faces in ELEMENTS:
        fs = {}
        for f in faces:
            x, y, w, h = layout[(name, f)]
            fs[f] = {"uv": [x * 16 / ATLAS, y * 16 / ATLAS, (x + w) * 16 / ATLAS, (y + h) * 16 / ATLAS], "texture": "#all"}
        elements.append({"name": name, "from": list(a), "to": list(b), "faces": fs})
    display = {
        "gui": {"rotation": [28, 205, 0], "translation": [0, -1.5, 0], "scale": [0.82, 0.82, 0.82]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -1, 0], "scale": [0.8, 0.8, 0.8]},
        "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, 200, 0], "translation": [0, 2, 1], "scale": [0.62, 0.62, 0.62]},
        "firstperson_lefthand": {"rotation": [0, 160, 0], "translation": [0, 2, 1], "scale": [0.62, 0.62, 0.62]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 0], "scale": [1.0, 1.0, 1.0]},
    }
    return {"credit": "generated by tools/gen_backpack_assets.py", "texture_size": [ATLAS, ATLAS], "gui_light": "side",
            "textures": {"all": "nusmp:block/backpack_" + tier, "particle": "nusmp:block/backpack_" + tier}, "elements": elements, "display": display}


def upgrade_sprite(tier, path):
    pal = PALETTE[tier]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    trim, body, buckle = pal["trim"], pal["body"], pal["buckle"]
    outline = shade(trim, 0.35)
    # a plate with a strap loop: a rounded rectangle 12 x 10 with a rivet in each corner and the tier's emblem in the middle
    for y in range(3, 13):
        for x in range(2, 14):
            corner = (x in (2, 13)) and (y in (3, 12))
            if corner:
                continue
            edge = x in (2, 13) or y in (3, 12)
            if edge:
                c = outline
            else:
                k = 1.18 if y < 6 else (0.8 if y > 9 else 1.0)
                n = noise(x, y, 11)
                c = shade(trim, k * (0.92 + 0.16 * n))
            px[x, y] = c + (255,)
    for (x, y) in ((3, 4), (12, 4), (3, 11), (12, 11)):
        px[x, y] = buckle + (255,)
    for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        px[x, y] = shade(body, 0.8) + (255,)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = buckle + (255,)
    # the strap loop on the top edge
    for x in range(6, 10):
        px[x, 2] = outline + (255,)
    px[6, 1] = outline + (255,)
    px[9, 1] = outline + (255,)
    for x in (7, 8):
        px[x, 1] = shade(pal["strap"], 1.1) + (255,)
    img.save(path)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    layout = None
    for scale in (4, 3):
        layout = pack(scale)
        if layout:
            break
    if not layout:
        raise SystemExit("the faces do not fit the atlas")
    tex_dir = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex_dir, exist_ok=True)
    item_tex = os.path.join(ASSETS, "textures", "item")
    os.makedirs(item_tex, exist_ok=True)
    for tier in TIERS:
        write_texture(tier, layout, os.path.join(tex_dir, "backpack_%s.png" % tier))
        write_json(os.path.join(ASSETS, "models", "block", "backpack_%s.json" % tier), model_json(tier, layout))
        write_json(os.path.join(ASSETS, "models", "item", "backpack_%s.json" % tier), {"parent": "nusmp:block/backpack_" + tier})
        if tier != "leather":
            upgrade_sprite(tier, os.path.join(item_tex, "backpack_upgrade_%s.png" % tier))
            write_json(os.path.join(ASSETS, "models", "item", "backpack_upgrade_%s.json" % tier),
                       {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/backpack_upgrade_%s" % tier}})
    # the main item model picks the tier model by the item's tier property (registered on the client as nusmp:backpack_tier; each override matches from its value up)
    overrides = [{"predicate": {"nusmp:backpack_tier": i}, "model": "nusmp:item/backpack_" + t} for i, t in enumerate(TIERS) if i > 0]
    write_json(os.path.join(ASSETS, "models", "item", "backpack.json"), {"parent": "nusmp:item/backpack_leather", "overrides": overrides})
    # the blockstate: four facings x five tiers
    variants = {}
    for i, t in enumerate(TIERS):
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            v = {"model": "nusmp:block/backpack_" + t}
            if y:
                v["y"] = y
            variants["facing=%s,tier=%d" % (f, i)] = v
    write_json(os.path.join(ASSETS, "blockstates", "backpack.json"), {"variants": variants})
    if len(sys.argv) > 2 and sys.argv[1] == "sheet":
        sheet = Image.new("RGBA", (ATLAS * 5 + 6, ATLAS + 20 + 16 * 4), (24, 24, 30, 255))
        for i, t in enumerate(TIERS):
            sheet.paste(Image.open(os.path.join(tex_dir, "backpack_%s.png" % t)), (i * (ATLAS + 1) + 1, 1))
            if t != "leather":
                sheet.paste(Image.open(os.path.join(item_tex, "backpack_upgrade_%s.png" % t)), (i * 20, ATLAS + 4))
        sheet = sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST)
        sheet.save(sys.argv[2])
    print("backpack assets written; atlas %dx%d, %d faces" % (ATLAS, ATLAS, len(layout)))


if __name__ == "__main__":
    main()
