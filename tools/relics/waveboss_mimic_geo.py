"""The Artifact Mimic's worn model: a GeoLite belt with a small mimic chest on the front.

    python3 -B tools/relics/waveboss_mimic_geo.py          # writes the model, the animations and both textures, then runs tools/geo_check.py
    python3 -B tools/relics/waveboss_preview.py            # side / front views of the rest, jaw-open and bite poses next to the player's body boxes

Writes assets/nusmp/geo/entity/relic_mimic_belt.geo.json, animations/entity/relic_mimic_belt.animation.json and
textures/entity/relic_mimic_belt.png + _glow.png (client/relics/worn/ArtifactMimicRenderer draws it with GeoSpec.of("relic", "mimic_belt")).

FRAME. The model is authored in the player's own frame (see docs/asset_pipeline.md, section Curio attachment): y up, the neck line at y = 24, the hips at
y = 12, the front on -z, +x the player's left. The renderer draws it from model.body's pose, so everything below is relative to the torso. The torso is
8 wide (x -4..4), 4 deep (z -2..2); worn armour inflates it to x +-5, z +-3 (chest plate) so the straps sit on the outside of the plate (outer faces at
x +-5.06 and z -3.06 / 3.06) and show over leggings and bare skin too.

BONES.  root
          straps   (pivot 0,13.4,0)  the belt: front, sides and back leather straps, six studs
          chest    (pivot 0,13.0,-3)  the mimic's base on the front: wooden box, lower teeth and fangs
            lid      (pivot 0,14.6,-3)  the jaw: the lid, its domed top, the hasp, upper teeth and fangs
              eyes     (pivot 0,17.2,-5.6) two amber eyes on the brow of the lid (blink and widen)
            tongue1  (pivot 0,14.6,-4)  tongue root lying on the base
              tongue2  (pivot 0,14.5,-6)   the length hanging out of the mouth
                tongue3  (pivot 0,13.9,-8)   the tip
KEYFRAME STATES (animations, seconds; the lid turns about +x, positive raises the front edge):
  idle  3.0 s loop   closed, breathing (the lid opens 2.5 degrees), tongue wobble, one blink
  yawn  1.2 s once   closed -> jaw open 50 degrees (0.3 s) -> held -> closed (the Hoard opening)
  bite  1.4 s once   closed (0.0) -> jaw open, chest coils back 0.8 px (0.0-0.45) -> held wide at 65 degrees (0.45-0.55)
                     -> snap shut, chest lunges 2.2 px forward (0.55-0.63) -> recover, lid shivers, chest settles (0.63-1.4)
The lengths are the constants of client/relics/worn/WornMath (BITE_TICKS 28, YAWN_TICKS 24) and the server's WaveBossRules.BITE_DELAY (12 ticks = 0.6 s)
lands the damage on the snap; WornMathTest reads this file's lengths to keep the three in step.
"""
import math
import os
import random
import subprocess
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from geo_builder import Model, Anim, ROOT   # noqa: E402

TEX = 64
random.seed(7)

# (id, bone, origin, size, material, inflate)
CUBES = [
    # the belt: front strap behind the chest, two sides, the back strap
    ("s_front", "straps", (-5, 12.4, -3), (10, 2, 1), "leather", 0.06),
    ("s_left", "straps", (-5, 12.4, -2), (1, 2, 4), "leather", 0.06),
    ("s_right", "straps", (4, 12.4, -2), (1, 2, 4), "leather", 0.06),
    ("s_back", "straps", (-5, 12.4, 2), (10, 2, 1), "leather", 0.06),
    # six studs: two on the front, one on each side, two on the back
    ("stud_fl", "straps", (-4.5, 12.9, -3.5), (1, 1, 1), "gold", 0.0),
    ("stud_fr", "straps", (3.5, 12.9, -3.5), (1, 1, 1), "gold", 0.0),
    ("stud_l", "straps", (-5.5, 12.9, -0.5), (1, 1, 1), "gold", 0.0),
    ("stud_r", "straps", (4.5, 12.9, -0.5), (1, 1, 1), "gold", 0.0),
    ("stud_bl", "straps", (-3.5, 12.9, 2.5), (1, 1, 1), "gold", 0.0),
    ("stud_br", "straps", (2.5, 12.9, 2.5), (1, 1, 1), "gold", 0.0),
    # the mimic's base and its lower teeth
    ("base", "chest", (-3.5, 11.6, -6), (7, 3, 3), "wood", 0.0),
    ("lt0", "chest", (-2.5, 14.6, -5.9), (1, 1, 1), "tooth", 0.0),
    ("lt1", "chest", (-0.5, 14.6, -5.9), (1, 1, 1), "tooth", 0.0),
    ("lt2", "chest", (1.5, 14.6, -5.9), (1, 1, 1), "tooth", 0.0),
    ("lf0", "chest", (-3.5, 14.6, -5.9), (1, 2, 1), "tooth", 0.0),
    ("lf1", "chest", (2.5, 14.6, -5.9), (1, 2, 1), "tooth", 0.0),
    # the lid (the jaw)
    ("lid", "lid", (-3.5, 14.6, -6), (7, 2, 3), "wood", 0.0),
    ("lid_top", "lid", (-3, 16.6, -5.5), (6, 1, 2), "wood", 0.0),
    ("hasp", "lid", (-1, 13.7, -6.2), (2, 2, 1), "gold", 0.0),
    ("ut0", "lid", (-1.5, 13.6, -5.9), (1, 1, 1), "tooth", 0.0),
    ("ut1", "lid", (0.5, 13.6, -5.9), (1, 1, 1), "tooth", 0.0),
    ("uf0", "lid", (-3.5, 12.6, -5.9), (1, 2, 1), "tooth", 0.0),
    ("uf1", "lid", (2.5, 12.6, -5.9), (1, 2, 1), "tooth", 0.0),
    ("eye_l", "eyes", (-2, 16.7, -5.7), (1, 1, 1), "eye", 0.0),
    ("eye_r", "eyes", (1, 16.7, -5.7), (1, 1, 1), "eye", 0.0),
    # the tongue
    ("tongue1", "tongue1", (-1.5, 14.5, -6), (3, 1, 2), "tongue", 0.0),
    ("tongue2", "tongue2", (-1, 13.4, -8), (2, 1, 2), "tongue", 0.0),
    ("tongue3", "tongue3", (-0.5, 12.8, -9), (1, 1, 1), "tongue", 0.0),
]
# cubes of one kind and size share one box-uv rectangle
SHARED = {("gold", (1, 1, 1)), ("tooth", (1, 1, 1)), ("tooth", (1, 2, 1)), ("eye", (1, 1, 1))}

PALETTE = {
    "leather": [(58, 36, 20), (84, 54, 30), (112, 74, 42)],
    "wood": [(70, 44, 24), (102, 66, 36), (136, 92, 52)],
    "gold": [(122, 86, 24), (200, 156, 52), (250, 214, 110)],
    "tooth": [(170, 160, 138), (224, 216, 192), (250, 246, 228)],
    "eye": [(150, 70, 10), (236, 160, 40), (255, 226, 130)],
    "tongue": [(110, 26, 40), (160, 48, 62), (208, 96, 104)],
    "flesh": [(70, 14, 26), (112, 28, 44), (150, 48, 62)],
}
SHADE = {"top": 1.12, "bottom": 0.6, "north": 1.0, "south": 0.78, "west": 0.88, "east": 0.94}


def clamp(v, lo=0, hi=255):
    return lo if v < lo else hi if v > hi else int(v)


def faces(u, v, sx, sy, sz):
    """The six face rectangles (x, y, w, h) of the vanilla box layout at (u, v)."""
    return {"west": (u, v + sz, sz, sy), "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy),
            "south": (u + 2 * sz + sx, v + sz, sx, sy), "top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz)}


def pack(cubes):
    """Shelf-pack the unfolded boxes into the 64x64 sheet; returns {key: (u, v)}."""
    slots, order = {}, []
    for c in cubes:
        key = (c[4], tuple(c[3])) if (c[4], tuple(c[3])) in SHARED else c[0]
        if key not in slots:
            slots[key] = None
            order.append((key, tuple(c[3])))
    order.sort(key=lambda kv: -(kv[1][2] + kv[1][1]))
    x = y = row = 0
    for key, (sx, sy, sz) in order:
        w, h = 2 * (sx + sz), sz + sy
        if x + w > TEX:
            x, y, row = 0, y + row, 0
        slots[key] = (x, y)
        x += w
        row = max(row, h)
        if y + row > TEX:
            raise SystemExit("the atlas is full")
    return slots


def paint_face(img, glow, rect, material, face, cid, interior=False):
    x0, y0, w, h = rect
    pal = PALETTE["flesh" if interior else material]
    for j in range(h):
        for i in range(w):
            n = random.random()
            t = 0.35 + 0.35 * n
            if material == "wood" and not interior:
                t += 0.12 * (1 if (i + (cid % 3)) % 3 == 0 else -1 if (i + (cid % 3)) % 3 == 1 else 0)      # plank lines
                if j == 0 or j == h - 1:
                    t -= 0.25                                                                             # dark seams between boards
            if material == "leather":
                t += 0.10 * math.sin(i * 1.7 + j * 0.4)
                if j in (1, h - 2) and h > 3:
                    t += 0.18                                                                             # stitching
            if material == "gold":
                t = 0.30 + 0.5 * (1 - (j / max(1, h - 1))) + 0.15 * n
            if material == "tooth":
                t = 0.35 + 0.5 * (1 - (j / max(1, h - 1)))
            if material == "tongue":
                t += 0.15 * math.sin(i * 1.3 + j)
            if interior:
                t = 0.3 + 0.4 * n + 0.15 * math.sin(i * 0.9 + j * 1.1)
            t = max(0.0, min(1.0, t))
            a, b = (pal[0], pal[1]) if t < 0.5 else (pal[1], pal[2])
            k = (t if t < 0.5 else t - 0.5) * 2
            col = [a[c] + (b[c] - a[c]) * k for c in range(3)]
            s = SHADE[face]
            img.putpixel((x0 + i, y0 + j), (clamp(col[0] * s), clamp(col[1] * s), clamp(col[2] * s), 255))
            if material == "eye":
                glow.putpixel((x0 + i, y0 + j), (255, 200, 80, 255))
            elif material == "tongue":
                glow.putpixel((x0 + i, y0 + j), (170, 40, 60, 90))
            elif interior:
                glow.putpixel((x0 + i, y0 + j), (150, 30, 40, 70))


def build():
    m = Model("relic", "mimic_belt", tex_w=TEX, tex_h=TEX)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("straps", parent="root", pivot=(0, 13.4, 0))
    m.bone("chest", parent="root", pivot=(0, 13.0, -3))
    m.bone("lid", parent="chest", pivot=(0, 14.6, -3))
    m.bone("eyes", parent="lid", pivot=(0, 17.2, -5.6))
    m.bone("tongue1", parent="chest", pivot=(0, 14.6, -4))
    m.bone("tongue2", parent="tongue1", pivot=(0, 14.5, -6))
    m.bone("tongue3", parent="tongue2", pivot=(0, 13.9, -8))
    slots = pack(CUBES)
    img, glow = m.paint()
    painted = set()
    for n, (cid, bone, origin, size, material, inflate) in enumerate(CUBES):
        key = (material, tuple(size)) if (material, tuple(size)) in SHARED else cid
        u, v = slots[key]
        m.cube(bone, origin=origin, size=size, uv=(u, v), inflate=inflate)
        if key in painted:
            continue                                                                                        # a shared rectangle is painted once
        painted.add(key)
        for face, rect in faces(u, v, *size).items():
            interior = (cid == "base" and face == "top") or (cid == "lid" and face == "bottom")           # the inside of the mouth
            paint_face(img, glow, rect, material, face, n, interior)
    # the hasp's front: a bright plate with a keyhole that glows
    u, v = slots["hasp"]
    fx, fy, fw, fh = faces(u, v, 2, 2, 1)["north"]
    for i in range(fw):
        for j in range(fh):
            img.putpixel((fx + i, fy + j), (232, 190, 80, 255))
    img.putpixel((fx, fy + 1), (40, 24, 8, 255))
    glow.putpixel((fx, fy + 1), (255, 180, 60, 255))
    return m, img, glow


def animations():
    a = Anim()
    # ---- idle: a slow loop, closed
    a.clip("idle", length=3.0, loop=True)
    a.rot("idle", "lid", {0.0: (0, 0, 0), 1.0: (2.0, 0, 0), 1.6: (2.5, 0, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "tongue1", {0.0: (0, 0, 0), 0.75: (5, 0, 0), 1.5: (0, 0, 0), 2.25: (-4, 0, 0), 3.0: (0, 0, 0)})
    a.rot("idle", "tongue2", {0.0: (0, 0, 0), 0.75: (-8, 0, 8), 1.5: (4, 0, 0), 2.25: (-6, 0, -8), 3.0: (0, 0, 0)})
    a.rot("idle", "tongue3", {0.0: (0, 0, 0), 1.2: (-18, 0, 6), 2.4: (10, 0, -6), 3.0: (0, 0, 0)})
    a.scale("idle", "eyes", {0.0: (1, 1, 1), 2.4: (1, 1, 1), 2.5: (1, 0.15, 1), 2.6: (1, 1, 1), 3.0: (1, 1, 1)})
    # ---- yawn: the hoard opens its mouth and shuts it again
    a.clip("yawn", length=1.2, loop=False)
    a.rot("yawn", "lid", {0.0: (0, 0, 0), 0.3: (48, 0, 0), 0.9: (50, 0, 0), 1.2: (0, 0, 0)})
    a.rot("yawn", "tongue1", {0.0: (0, 0, 0), 0.3: (-10, 0, 0), 0.9: (-8, 0, 0), 1.2: (0, 0, 0)})
    a.rot("yawn", "tongue2", {0.0: (0, 0, 0), 0.3: (-25, 0, 0), 0.9: (-20, 0, 6), 1.2: (0, 0, 0)})
    a.scale("yawn", "eyes", {0.0: (1, 1, 1), 0.3: (1.3, 1.3, 1.3), 0.9: (1.3, 1.3, 1.3), 1.2: (1, 1, 1)})
    # ---- bite: closed, jaw open + coil, held wide, snap + lunge, recover
    a.clip("bite", length=1.4, loop=False)
    a.rot("bite", "lid", {0.0: (0, 0, 0), 0.45: (62, 0, 0), 0.55: (65, 0, 0), 0.63: (0, 0, 0), 0.7: (6, 0, 0), 0.78: (0, 0, 0), 0.9: (3, 0, 0), 1.0: (0, 0, 0), 1.4: (0, 0, 0)})
    a.pos("bite", "chest", {0.0: (0, 0, 0), 0.45: (0, 0.3, 0.8), 0.55: (0, 0.3, 0.8), 0.63: (0, 0, -2.2), 0.9: (0, 0, -0.6), 1.4: (0, 0, 0)})
    a.rot("bite", "tongue1", {0.0: (0, 0, 0), 0.45: (-14, 0, 0), 0.63: (10, 0, 0), 1.4: (0, 0, 0)})
    a.rot("bite", "tongue2", {0.0: (0, 0, 0), 0.45: (-30, 0, 0), 0.63: (20, 0, 0), 0.8: (-10, 0, 0), 1.4: (0, 0, 0)})
    a.rot("bite", "tongue3", {0.0: (0, 0, 0), 0.45: (-20, 0, 0), 0.63: (30, 0, 8), 0.9: (-8, 0, 0), 1.4: (0, 0, 0)})
    a.scale("bite", "eyes", {0.0: (1, 1, 1), 0.45: (1.4, 1.4, 1.4), 0.55: (1.4, 1.4, 1.4), 0.63: (1, 0.3, 1), 0.9: (1, 1, 1), 1.4: (1, 1, 1)})
    return a


def main():
    m, img, glow = build()
    m.save(animations())
    m.save_texture(img, glow)
    print("relic_mimic_belt: %d cubes" % m.cube_count())
    tools = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "geo_check.py")
    sys.exit(subprocess.call([sys.executable, "-B", tools, "relic_mimic_belt"]))


if __name__ == "__main__":
    main()
