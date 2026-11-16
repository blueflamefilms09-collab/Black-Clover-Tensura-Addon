"""Dragon Balls: the ball textures + item models, the Dragon Radar, the Shenron geo model + animation + skins, the radar recipe.

    python3 -B tools/gen_dragon_balls.py

Writes under src/main/resources/ (all files are generated, nothing is hand drawn):
    assets/anime_multiverse/textures/item/dragon_ball_<set>_<n>.png + _stone.png   set = earth | super, n = 1..8 (32x32, a shaded orb with n stars)
    assets/anime_multiverse/models/item/dragon_ball_<set>_<n>.json + _stone.json   the live model switches to the stone one on custom_model_data 1
    assets/anime_multiverse/textures/item/dragon_radar.png + models/item/dragon_radar.json
    assets/anime_multiverse/geo/entity/dragonballs_shenron.geo.json + animations/entity/dragonballs_shenron.animation.json
    assets/anime_multiverse/textures/entity/dragonballs_shenron.png (+ _glow) and dragonballs_shenron_super.png (+ _glow)
    data/anime_multiverse/recipe/dragon_radar.json

Shenron is a chain of 26 body bones (each the child of the one below) so a travelling wave in the "idle" clip makes it undulate, a head with a jaw, two horns, whiskers and glowing eyes, two arms with claws
and a dorsal ridge. Built with the geo generator (tools/geo_builder.py), NOT Blender: no Blender exists in the build container (docs/dragon_balls.md).
"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT  # noqa: E402

RES = os.path.abspath(os.path.join(ROOT, "..", ".."))          # src/main/resources
ITEM_TEX = os.path.join(ROOT, "textures", "item")
ITEM_MODELS = os.path.join(ROOT, "models", "item")
os.makedirs(ITEM_TEX, exist_ok=True)
os.makedirs(ITEM_MODELS, exist_ok=True)

# ------------------------------------------------------------------------------------------------ the balls
SET_STYLE = {
    "earth": {"light": (255, 214, 96), "mid": (245, 150, 30), "dark": (150, 70, 8), "star": (214, 24, 24), "edge": (90, 10, 10), "halo": (255, 170, 40)},
    "super": {"light": (232, 170, 255), "mid": (150, 80, 220), "dark": (60, 24, 120), "star": (255, 226, 96), "edge": (90, 60, 0), "halo": (190, 110, 255)},
}


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def star_points(cx, cy, r, rot=-math.pi / 2):
    pts = []
    for k in range(10):
        rad = r if k % 2 == 0 else r * 0.42
        a = rot + k * math.pi / 5
        pts.append((cx + math.cos(a) * rad, cy + math.sin(a) * rad))
    return pts


def layout(n):
    if n == 1:
        return [(0, 0)], 6.2
    if n == 2:
        return [(-5.5, 0), (5.5, 0)], 4.6
    if n == 3:
        return [(0, -5.5), (-5.5, 4), (5.5, 4)], 4.4
    if n == 4:
        return [(-5.5, -5.5), (5.5, -5.5), (-5.5, 5.5), (5.5, 5.5)], 4.2
    if n == 5:
        return [(-6, -6), (6, -6), (-6, 6), (6, 6), (0, 0)], 3.8
    if n == 6:
        return [(-5.5, -7), (5.5, -7), (-5.5, 0), (5.5, 0), (-5.5, 7), (5.5, 7)], 3.6
    if n == 7:
        return [(0, 0)] + [(math.cos(k * math.pi / 3) * 7.8, math.sin(k * math.pi / 3) * 7.8) for k in range(6)], 3.4
    return [(math.cos(k * math.pi / 4) * 8.6, math.sin(k * math.pi / 4) * 8.6) for k in range(8)], 3.2


def ball_image(set_id, n, stone):
    S, W = 4, 32
    img = Image.new("RGBA", (W * S, W * S), (0, 0, 0, 0))
    px = img.load()
    c = W * S / 2.0
    R = 14.0 * S
    st = SET_STYLE[set_id]
    rnd = random.Random(1000 + n * 7 + (50 if stone else 0))
    light = (-0.45, -0.55, 0.70)
    ln = math.sqrt(sum(v * v for v in light))
    light = tuple(v / ln for v in light)
    for y in range(W * S):
        for x in range(W * S):
            dx, dy = (x + 0.5 - c) / R, (y + 0.5 - c) / R
            r2 = dx * dx + dy * dy
            if r2 <= 1.0:
                nz = math.sqrt(1.0 - r2)
                lam = max(0.0, dx * light[0] + dy * light[1] + nz * light[2])
                if stone:
                    g = 70 + int(95 * lam) + rnd.randint(-9, 9)
                    px[x, y] = (g, g, int(g * 1.04), 255)
                else:
                    t = min(1.0, lam * 1.15)
                    col = lerp(st["dark"], st["mid"], min(1.0, t * 1.6)) if t < 0.62 else lerp(st["mid"], st["light"], (t - 0.62) / 0.38)
                    rim = max(0.0, 1.0 - nz) ** 3 * 0.25                      # the inner glow of the rim
                    col = lerp(col, st["halo"], rim)
                    px[x, y] = (col[0], col[1], col[2], 255)
            elif not stone and r2 <= 1.24:                                    # the faint halo round the orb
                a = int(90 * (1.0 - (math.sqrt(r2) - 1.0) / 0.1114) ** 2)
                if a > 0:
                    px[x, y] = (st["halo"][0], st["halo"][1], st["halo"][2], max(0, min(120, a)))
    d = ImageDraw.Draw(img)
    if not stone:                                                              # the specular dot
        d.ellipse([c - 7.5 * S, c - 9.5 * S, c - 3.5 * S, c - 5.5 * S], fill=(255, 255, 235, 150))
    pts, rad = layout(n)
    for ox, oy in pts:
        cx, cy = c + ox * S, c + oy * S
        edge = (50, 50, 52, 255) if stone else st["edge"] + (255,)
        fill = (95, 95, 100, 255) if stone else st["star"] + (255,)
        d.polygon(star_points(cx, cy, (rad + 0.7) * S), fill=edge)
        d.polygon(star_points(cx, cy, rad * S), fill=fill)
    return img.resize((W, W), Image.LANCZOS)


def write_json(path, obj):
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def make_balls():
    for set_id in SET_STYLE:
        for n in range(1, 9):
            ball_image(set_id, n, False).save(os.path.join(ITEM_TEX, f"dragon_ball_{set_id}_{n}.png"))
            ball_image(set_id, n, True).save(os.path.join(ITEM_TEX, f"dragon_ball_{set_id}_{n}_stone.png"))
            base = f"dragon_ball_{set_id}_{n}"
            write_json(os.path.join(ITEM_MODELS, base + ".json"), {
                "parent": "minecraft:item/generated", "textures": {"layer0": f"anime_multiverse:item/{base}"},
                "overrides": [{"predicate": {"custom_model_data": 1}, "model": f"anime_multiverse:item/{base}_stone"}]})
            write_json(os.path.join(ITEM_MODELS, base + "_stone.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"anime_multiverse:item/{base}_stone"}})


def make_radar():
    W, S = 32, 4
    img = Image.new("RGBA", (W * S, W * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([5 * S, 2 * S, 27 * S, 30 * S], radius=3 * S, fill=(26, 74, 58, 255), outline=(10, 30, 24, 255), width=S)
    d.rounded_rectangle([7 * S, 4 * S, 25 * S, 20 * S], radius=2 * S, fill=(8, 28, 20, 255), outline=(70, 150, 110, 255), width=S // 2)
    for r in (3, 6, 8):
        d.ellipse([16 * S - r * S, 12 * S - r * S, 16 * S + r * S, 12 * S + r * S], outline=(60, 200, 120, 255), width=S // 2)
    d.line([16 * S, 4 * S, 16 * S, 20 * S], fill=(60, 200, 120, 160), width=S // 3)
    d.line([7 * S, 12 * S, 25 * S, 12 * S], fill=(60, 200, 120, 160), width=S // 3)
    for bx, by in ((11, 8), (20, 15), (14, 16)):
        d.ellipse([bx * S - S, by * S - S, bx * S + S, by * S + S], fill=(255, 190, 60, 255))
    for k in range(3):
        d.ellipse([(9 + k * 7) * S, 23 * S, (12 + k * 7) * S, 26 * S], fill=((230, 60, 50, 255), (240, 190, 40, 255), (60, 200, 120, 255))[k])
    img.resize((W, W), Image.LANCZOS).save(os.path.join(ITEM_TEX, "dragon_radar.png"))
    write_json(os.path.join(ITEM_MODELS, "dragon_radar.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/dragon_radar"}})
    recipe_dir = os.path.join(RES, "data", "anime_multiverse", "recipe")
    os.makedirs(recipe_dir, exist_ok=True)
    write_json(os.path.join(recipe_dir, "dragon_radar.json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["PRP", "RCR", "PRP"],
        "key": {"P": {"item": "minecraft:glass_pane"}, "R": {"item": "minecraft:redstone"}, "C": {"item": "minecraft:compass"}},
        "result": {"id": "anime_multiverse:dragon_radar", "count": 1}})


# ------------------------------------------------------------------------------------------------ Shenron
N_SEG, SEG_H = 26, 12


class Packer:
    """Shelf packer for vanilla box-uv regions."""

    def __init__(self, width):
        self.w, self.x, self.y, self.row = width, 0, 0, 0

    def place(self, sx, sy, sz):
        w, h = 2 * (sx + sz), sy + sz
        if self.x + w > self.w:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        pos = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return pos

    def height(self):
        return self.y + self.row


def seg_width(i):
    t = min(1.0, i / 18.0)
    w = 5 + 11 * t
    if i > 20:
        w -= (i - 20) * 1.1
    return int(round(w))


def build_cubes():
    """All cubes: (bone, origin, size, kind, extra) with kind picking the paint."""
    cubes = []
    for i in range(N_SEG):
        w = seg_width(i)
        d = max(4, int(round(w * 0.85)))
        cubes.append((f"seg{i}", (-w / 2.0, i * SEG_H, -d / 2.0), (w, SEG_H, d), "body", i))
        if i >= 2:
            cubes.append((f"seg{i}", (-1.5, i * SEG_H + 1, d / 2.0), (3, SEG_H - 2, 4 if i > 12 else 3), "fin", i))
    hy = N_SEG * SEG_H
    cubes += [
        ("head", (-9, hy, -12), (18, 14, 24), "head", 0),
        ("head", (-6, hy + 2, -27), (12, 8, 15), "head", 1),
        ("head", (-4, hy + 11, 9), (8, 6, 8), "fin", 0),
        ("jaw", (-5.5, hy - 4, -27), (11, 5, 22), "jaw", 0),
        ("horn_l", (5, hy + 11, 4), (3, 3, 16), "horn", 0),
        ("horn_r", (-8, hy + 11, 4), (3, 3, 16), "horn", 0),
        ("whisker_l", (8, hy + 4, -22), (2, 2, 44), "whisker", 0),
        ("whisker_r", (-10, hy + 4, -22), (2, 2, 44), "whisker", 0),
        ("eye_l", (8, hy + 9, -21), (2, 3, 6), "eye", 0),
        ("eye_r", (-10, hy + 9, -21), (2, 3, 6), "eye", 0),
    ]
    for side, sx in (("l", 1), ("r", -1)):
        ax = 9 * sx
        cubes.append((f"arm_{side}", (ax - 2, 20 * SEG_H - 14, -2), (4, 16, 4), "limb", 0))
        cubes.append((f"hand_{side}", (ax - 3, 20 * SEG_H - 20, -3), (6, 6, 6), "limb", 1))
        for k in range(3):
            cubes.append((f"hand_{side}", (ax - 3 + k * 2.2, 20 * SEG_H - 26, -2.6), (1, 6, 1), "claw", 0))
    return cubes


def ints(cube):
    return tuple(int(round(v)) for v in cube[2])


PALETTES = {
    "earth": {"body": (46, 140, 62), "belly": (216, 206, 150), "fin": (150, 40, 30), "head": (52, 150, 70), "horn": (150, 108, 60), "whisker": (214, 190, 120), "eye": (255, 40, 30), "limb": (46, 130, 60), "claw": (240, 230, 200), "jaw": (200, 190, 140)},
    "super": {"body": (232, 190, 58), "belly": (255, 238, 170), "fin": (168, 70, 20), "head": (240, 200, 70), "horn": (150, 90, 30), "whisker": (255, 240, 170), "eye": (230, 20, 40), "limb": (226, 182, 52), "claw": (255, 250, 220), "jaw": (255, 226, 130)},
}


def paint_region(img, glow, pos, size, kind, extra, pal, rnd):
    u, v = pos
    sx, sy, sz = size
    d, g = img.load(), glow.load()
    base = pal.get(kind, pal["body"])
    W, H = 2 * (sx + sz), sy + sz
    for yy in range(H):
        for xx in range(W):
            n = rnd.randint(-10, 10)
            shade = 1.0 - 0.28 * (yy / max(1, H))
            c = tuple(max(0, min(255, int(ch * shade + n))) for ch in base)
            # scales: a faint diamond lattice
            if kind in ("body", "head", "limb") and ((xx // 3 + yy // 3) % 2 == 0):
                c = tuple(max(0, int(ch * 0.9)) for ch in c)
            # belly: the north (front) face of a body block is the pale plate
            if kind == "body" and sz <= xx < sz + sx and sz <= yy < sz + sy:
                c = tuple(max(0, min(255, int(ch + rnd.randint(-5, 5)))) for ch in pal["belly"])
                if (yy % 4) == 0:
                    c = tuple(int(ch * 0.86) for ch in c)
            if kind == "jaw" and yy >= sz:
                c = tuple(int(0.55 * a + 0.45 * b) for a, b in zip(pal["jaw"], (120, 30, 30)))
            if u + xx < img.width and v + yy < img.height:
                d[u + xx, v + yy] = (c[0], c[1], c[2], 255)
                if kind == "eye":
                    g[u + xx, v + yy] = (255, 80, 60, 255)
                elif kind == "jaw" and yy < sz and 3 < xx < W // 2 - 3:
                    g[u + xx, v + yy] = (255, 120, 40, 90)


def make_shenron():
    cubes = build_cubes()
    pk = Packer(512)
    layout_ = []
    for c in cubes:
        sx, sy, sz = ints(c)
        layout_.append(pk.place(max(1, sx), max(1, sy), max(1, sz)))
    th = (pk.height() + 7) // 8 * 8
    m = Model("dragonballs", "shenron", tex_w=512, tex_h=th)
    # the body chain
    for i in range(N_SEG):
        rot = (0, 4, 22 * math.sin(i * 0.55))
        m.bone(f"seg{i}", parent=f"seg{i - 1}" if i else None, pivot=(0, i * SEG_H, 0), rotation=rot)
    hy = N_SEG * SEG_H
    m.bone("head", parent=f"seg{N_SEG - 1}", pivot=(0, hy, 0), rotation=(8, 0, 0))
    m.bone("jaw", parent="head", pivot=(0, hy - 2, -8))
    m.bone("horn_l", parent="head", pivot=(6.5, hy + 12, 6), rotation=(-28, 0, -8))
    m.bone("horn_r", parent="head", pivot=(-6.5, hy + 12, 6), rotation=(-28, 0, 8))
    m.bone("whisker_l", parent="head", pivot=(8, hy + 5, -20))
    m.bone("whisker_r", parent="head", pivot=(-8, hy + 5, -20))
    m.bone("eye_l", parent="head", pivot=(8, hy + 10, -18))
    m.bone("eye_r", parent="head", pivot=(-8, hy + 10, -18))
    for side, sx in (("l", 1), ("r", -1)):
        m.bone(f"arm_{side}", parent="seg20", pivot=(9 * sx, 20 * SEG_H + 2, 0), rotation=(0, 0, -28 * sx))
        m.bone(f"hand_{side}", parent=f"arm_{side}", pivot=(9 * sx, 20 * SEG_H - 14, 0))
    imgs = {}
    for name in ("earth", "super"):
        imgs[name] = (Image.new("RGBA", (512, th), (0, 0, 0, 0)), Image.new("RGBA", (512, th), (0, 0, 0, 0)))
    rnd = random.Random(2024)
    for c, pos in zip(cubes, layout_):
        bone, origin, size, kind, extra = c
        m.cube(bone, origin=origin, size=size, uv=pos)
        for name in imgs:
            paint_region(imgs[name][0], imgs[name][1], pos, ints(c), kind, extra, PALETTES[name], rnd)
    # the animation: a travelling wave up the body, the jaw, the whiskers
    a = Anim()
    a.clip("idle", length=6.0, loop=True)
    for i in range(N_SEG):
        amp = 4.0 + 0.12 * i
        frames = {}
        for k in range(7):
            t = float(k)
            frames[t] = (0, 0, amp * math.sin(2 * math.pi * t / 6.0 - i * 0.45))
        a.rot("idle", f"seg{i}", frames)
    a.rot("idle", "jaw", {0.0: (0, 0, 0), 1.5: (9, 0, 0), 3.0: (0, 0, 0), 4.5: (6, 0, 0), 6.0: (0, 0, 0)})
    for side, sg in (("l", 1), ("r", -1)):
        a.rot("idle", f"whisker_{side}", {0.0: (0, 14 * sg, 0), 1.5: (6, 24 * sg, 4), 3.0: (0, 14 * sg, 0), 4.5: (-6, 6 * sg, -4), 6.0: (0, 14 * sg, 0)})
        a.rot("idle", f"arm_{side}", {0.0: (0, 0, 0), 3.0: (12, 0, 6 * sg), 6.0: (0, 0, 0)})
    m.save(a)
    out = os.path.join(ROOT, "textures", "entity")
    os.makedirs(out, exist_ok=True)
    imgs["earth"][0].save(os.path.join(out, "dragonballs_shenron.png"))
    imgs["earth"][1].save(os.path.join(out, "dragonballs_shenron_glow.png"))
    imgs["super"][0].save(os.path.join(out, "dragonballs_shenron_super.png"))
    imgs["super"][1].save(os.path.join(out, "dragonballs_shenron_super_glow.png"))
    return m.cube_count(), th


if __name__ == "__main__":
    make_balls()
    make_radar()
    n, th = make_shenron()
    print("balls: 32 textures + 32 models; radar; Shenron: %d cubes, texture 512x%d" % (n, th))
