"""Paladin Conversion (Lucius Zogratis, Soul Magic): the halo and the wings every converted creature wears, as GeoLite models
(tools/geo_builder.py -> client/geo/GeoDraw, drawn by client/soul/PaladinLayer).

    python3 -B tools/gen_paladin_assets.py                   # writes both models, their animations, textures, glow maps and the preview scenes
    python3 -B tools/gen_paladin_assets.py --check           # reads every file back (bones, uv inside the sheet, clips)
    python3 -B tools/gen_paladin_assets.py --preview         # the headless render of tools/render_preview (JOML_JAR / GSON_JAR, see that script)

Writes (GeoSpec.of("paladin", "halo") and GeoSpec.of("paladin", "wings")):
    assets/nusmp/geo/entity/paladin_{halo,wings}.geo.json
    assets/nusmp/animations/entity/paladin_{halo,wings}.animation.json
    assets/nusmp/textures/entity/paladin_{halo,wings}.png          the cutout skin
    assets/nusmp/textures/entity/paladin_{halo,wings}_glow.png     what shines (additive, full bright; the layer pulses it)
    assets/nusmp/textures/mob_effect/paladin.png                   the 18 x 18 icon of the marker effect
and tools/render_preview/scenes.d/paladin.json.

Frame: both models are authored AROUND THE ORIGIN, not on the feet. The layer puts the origin where it wants it for the creature at hand (halo
centre above the head, wing roots between the shoulder blades) and scales the whole thing by the creature's size, so the numbers here are the
look of a human-sized wearer (halo 13 px across, wings 28 px each, about 1.8 blocks of wingspan). +x is the model's left, the back is +z.
Clips: halo idle (a slow turn and a bob), wings idle (a gentle open and close, the rows of feathers ripple one after another).
Deterministic; needs Pillow.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim, ROOT  # noqa: E402
from PIL import Image  # noqa: E402

KEY = "paladin"
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "paladin.json")
SCRATCH = os.environ.get("PALADIN_SCRATCH", os.path.join(REPO, "build", "paladin_preview"))

CREAM = (255, 246, 226)
PALE = (255, 238, 196)
GOLD = (255, 214, 120)
DEEP = (232, 176, 84)
WHITE = (255, 255, 250)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def fill(im, x0, y0, w, h, col, a=255):
    px = im.load()
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = (col[0], col[1], col[2], a)


# ================================================================================================ the halo
HALO_R = 6.5            # ring radius in pixels (a 13 px ring over a 8 px head: a clear gap, never a collar)
HALO_SEGMENTS = 16
HALO_TURN = 6.0         # seconds per turn


def build_halo():
    m = Model(KEY, "halo", 32, 16)
    m.bone("halo", pivot=(0, 0, 0))
    ring_uv = (0, 0)       # a (3,1,1) box: 8 x 2 texels
    gem_uv = (0, 4)        # a (2,2,2) box: 8 x 4 texels
    for k in range(HALO_SEGMENTS):
        th = 360.0 * k / HALO_SEGMENTS
        cx, cz = HALO_R * math.sin(math.radians(th)), HALO_R * math.cos(math.radians(th))
        m.cube("halo", origin=(cx - 1.5, -0.5, cz - 0.5), size=(3, 1, 1), uv=ring_uv, inflate=0.28, rotation=(0, th, 0), pivot=(cx, 0, cz))
    for th in (0, 90, 180, 270):
        cx, cz = HALO_R * math.sin(math.radians(th)), HALO_R * math.cos(math.radians(th))
        m.cube("halo", origin=(cx - 1, -1, cz - 1), size=(2, 2, 2), uv=gem_uv, inflate=0.2, rotation=(0, th + 45, 0), pivot=(cx, 0, cz))
    skin, glow = m.paint()
    # ring segment: bright cream with a hot upper edge; the whole box shares one 8 x 2 strip
    fill(skin, 0, 0, 8, 2, PALE)
    fill(skin, 1, 1, 3, 1, CREAM)
    fill(skin, 5, 1, 3, 1, CREAM)
    fill(glow, 0, 0, 8, 2, (255, 236, 168))
    fill(glow, 1, 1, 3, 1, WHITE)
    fill(glow, 5, 1, 3, 1, WHITE)
    # gem: white core, gold rim
    fill(skin, 0, 4, 8, 4, GOLD)
    fill(skin, 2, 6, 2, 2, WHITE)
    fill(skin, 6, 6, 2, 2, WHITE)
    fill(glow, 0, 4, 8, 4, (255, 224, 140))
    fill(glow, 2, 6, 2, 2, WHITE)
    fill(glow, 6, 6, 2, 2, WHITE)
    a = Anim()
    a.clip("idle", length=HALO_TURN, loop=True)
    a.rot("idle", "halo", {0.0: (0, 0, 0), HALO_TURN / 2: (0, 180, 0), HALO_TURN: (0, 360, 0)})
    a.pos("idle", "halo", {0.0: (0, 0, 0), HALO_TURN * 0.25: (0, 0.7, 0), HALO_TURN * 0.5: (0, 0, 0), HALO_TURN * 0.75: (0, -0.7, 0), HALO_TURN: (0, 0, 0)})
    m.save(a)
    m.save_texture(skin, glow)
    return m


# ================================================================================================ the wings
SHOULDER = (2.6, 0.0)               # where the feathers start, x and y (the wing root); z per row below
SWEEP = 24.0                        # the wing is swept back this many degrees (idle opens it a little more)
# row name -> (z of the row's plane, [(angle in degrees from +x in the wing plane, length)], width, base colour, tip colour)
ROWS = {
    # coverts nearest the viewer from behind (largest z), then the secondaries, the long primaries nearest the body
    "cov": (4.2, [(8, 6), (26, 7), (44, 8), (62, 7), (80, 6)], 3, WHITE, PALE),
    "sec": (3.1, [(-6, 9), (8, 11), (22, 13), (36, 14), (50, 13), (64, 11), (78, 9)], 3, CREAM, GOLD),
    "pri": (2.0, [(-34, 13), (-21, 16), (-8, 19), (5, 21), (18, 22), (31, 21), (45, 19), (60, 16), (76, 13)], 3, PALE, DEEP),
}
THICK = 1


def feather_paint(skin, glow, u0, v0, length, width, base, tip, row_index):
    """One feather: the box (length, width, 1) at (u0, v0). North and south carry the feather, the rest the edge colour."""
    total_w = 2 * (THICK + length)
    total_h = THICK + width
    fill(skin, u0, v0, total_w, total_h, shade(tip, 0.9))
    for face_x in (u0 + THICK, u0 + THICK + length + THICK):
        for x in range(length):
            t = x / max(1, length - 1)
            body = lerp(base, tip, t ** 1.4)
            for y in range(width):
                rib = 1.0 if y == width // 2 else 0.0
                col = lerp(body, WHITE, 0.35 * rib)
                if y != width // 2:
                    col = shade(col, 0.93)
                a = 255
                # the pointed tip: the corners of the last two columns drop out of the cutout
                if y != width // 2 and x >= length - 1:
                    a = 0
                if y != width // 2 and x == length - 2 and length >= 12:
                    a = 0
                skin.putpixel((face_x + x, v0 + THICK + y), (col[0], col[1], col[2], a))
                # glow: stronger toward the tip and along the rib
                g = min(1.0, 0.72 * (t ** 1.8) + 0.12 * rib * t)
                gc = lerp((255, 236, 170), WHITE, 0.5 * rib)
                glow.putpixel((face_x + x, v0 + THICK + y), (int(gc[0] * g), int(gc[1] * g), int(gc[2] * g), 255 if a else 0))


def build_wings():
    m = Model(KEY, "wings", 128, 112)
    m.bone("root", pivot=(0, 0, 0))
    skin, glow = m.paint()
    slots = {}
    cursor = [0]

    def slot(row, length, width, base, tip, ri):
        k = (row, length)
        if k not in slots:
            h = THICK + width
            slots[k] = (0, cursor[0])
            feather_paint(skin, glow, 0, cursor[0], length, width, base, tip, ri)
            cursor[0] += h + 1
        return slots[k]

    for side, sx in (("l", 1), ("r", -1)):
        m.bone("wing_" + side, parent="root", pivot=(sx * SHOULDER[0], SHOULDER[1], 3.0), rotation=(0, -sx * SWEEP, 0))
        for ri, (row, (z, feathers, width, base, tip)) in enumerate(ROWS.items()):
            bn = "%s_%s" % (side, row)
            m.bone(bn, parent="wing_" + side, pivot=(sx * SHOULDER[0], SHOULDER[1], z + 0.5))
            for ang, length in feathers:
                u, v = slot(row, length, width, base, tip, ri)
                px, py, pz = sx * SHOULDER[0], SHOULDER[1], z
                # both wings extend +x from the root before the turn; the right one is then turned by 180 - angle, which points it to -x
                org = (px, py - width / 2.0, pz)
                rot = (0, 0, ang if sx > 0 else 180.0 - ang)
                m.cube(bn, origin=org, size=(length, width, THICK), uv=(u, v), rotation=rot, pivot=(px, py, pz + THICK / 2.0))
        # the gold plate that holds the wing on the back
        m.cube("wing_" + side, origin=(sx * 1.4 - 1.5, -2.5, 2.2), size=(3, 5, 1), uv=(0, cursor[0]), inflate=0.15)
    fill(skin, 0, cursor[0], 8, 6, GOLD)
    fill(skin, 1, cursor[0] + 1, 3, 4, shade(GOLD, 1.12))
    fill(glow, 0, cursor[0], 8, 6, (200, 150, 60))
    a = Anim()
    L = 3.2
    a.clip("idle", length=L, loop=True)
    for side, sx in (("l", 1), ("r", -1)):
        a.rot("idle", "wing_" + side, {0.0: (0, 0, 0), L / 2: (0, -sx * 9, 1.5), L: (0, 0, 0)})
        # the rows ripple one after another: each is a quarter of a beat behind the one before
        for ri, row in enumerate(("pri", "sec", "cov")):
            ph = ri * 0.22
            a.rot("idle", "%s_%s" % (side, row), {0.0: (0, 0, 0), L * (0.25 + ph / 3): (0, 0, sx * 2.4), L * (0.75 + ph / 3 - 0.0): (0, 0, -sx * 2.4), L: (0, 0, 0)})
    m.save(a)
    m.save_texture(skin, glow)
    return m


def build_icon():
    """The 18 x 18 mob-effect icon (textures/mob_effect/paladin.png): a gold halo over a pair of white wings. The effect never shows in the
    HUD (it is a marker), but potion lists and other mods read the file, and a missing icon would log an error."""
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = im.load()
    # halo: an ellipse ring at the top
    for x in range(5, 13):
        for y in range(1, 5):
            dx, dy = (x - 8.5) / 3.6, (y - 2.5) / 1.5
            d = dx * dx + dy * dy
            if 0.45 <= d <= 1.25:
                px[x, y] = (255, 226, 140, 255) if y < 3 else (232, 176, 84, 255)
    # wings: two feathered fans below it
    for side in (-1, 1):
        for k, (ln, ang) in enumerate(((7, 0.2), (6, 0.55), (5, 0.9), (4, 1.25))):
            for i in range(ln):
                x = int(round(8.5 + side * (1.5 + i * math.cos(ang) * 1.0)))
                y = int(round(15 - i * math.sin(ang) * 1.1 - k * 0.0))
                if 0 <= x < 18 and 0 <= y < 18:
                    t = i / ln
                    col = lerp(WHITE, GOLD, t * 0.7)
                    px[x, y] = (col[0], col[1], col[2], 255)
                    if 0 <= y + 1 < 18 and k < 3:
                        px[x, y + 1] = (col[0] - 12, col[1] - 12, col[2] - 18, 255)
    for y in range(9, 16):
        px[8, y] = px[9, y] = (255, 246, 226, 255)
    os.makedirs(os.path.join(ROOT, "textures", "mob_effect"), exist_ok=True)
    im.save(os.path.join(ROOT, "textures", "mob_effect", "paladin.png"))


def build_all():
    h = build_halo()
    w = build_wings()
    build_icon()
    return h, w


# ================================================================================================ preview scenes
def scene(model, anim, ages, views, tile, argb="FFFFFFFF", glow="FFFFFFFF", floor=False):
    return {"kind": "geo", "model": "nusmp:geo/entity/paladin_%s.geo.json" % model, "texture": "nusmp:textures/entity/paladin_%s.png" % model,
            "space": "PLAYER", "layer": "CUTOUT", "argb": argb, "glow": glow, "ages": ages, "tile": list(tile), "floor": floor, "figure": False,
            "expect": [], "anim": anim, "views": views}


def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scenes():
    sc = {}
    # PLAYER space: the origin of the file lands 1.5 blocks below the layer's neck line; the target is where the origin sits in the world of the scene
    sc["paladin_halo"] = scene("halo", "idle", [0, 20, 40, 60], [view("3/4", 30, 22, (0, 0.0, 0), 2.2), view("top", 0, 70, (0, 0.0, 0), 2.2)], (360, 300))
    sc["paladin_wings_back"] = scene("wings", "idle", [0, 20, 40, 60], [view("from behind", 180, 4, (0, 0.0, 0), 4.2), view("3/4 back", 150, 12, (0, 0.0, 0), 4.2)], (420, 340))
    sc["paladin_wings_side"] = scene("wings", "idle", [0, 30], [view("side", 90, 4, (0, 0.0, 0), 4.2), view("front", 0, 4, (0, 0.0, 0), 4.2)], (360, 320))
    return sc


def write_scenes(path=SCENES_OUT):
    with open(path, "w") as f:
        json.dump(scenes(), f, indent=1)


# ================================================================================================ check
def check():
    bad = 0
    for name, clips in (("halo", ("idle",)), ("wings", ("idle",))):
        mid = "%s_%s" % (KEY, name)
        g = json.load(open(os.path.join(ROOT, "geo", "entity", mid + ".geo.json")))["minecraft:geometry"][0]
        tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                print("PROBLEM", mid, b["name"], "parent missing")
                bad += 1
            for c in b["cubes"]:
                u, v = c["uv"]
                sx, sy, sz = c["size"]
                if u + 2 * (sx + sz) > tw or v + sz + sy > th:
                    print("PROBLEM", mid, b["name"], "uv outside the sheet", c["uv"], c["size"])
                    bad += 1
        for t in (mid, mid + "_glow"):
            im = Image.open(os.path.join(ROOT, "textures", "entity", t + ".png"))
            if im.size != (tw, th):
                print("PROBLEM texture", t, im.size)
                bad += 1
        an = json.load(open(os.path.join(ROOT, "animations", "entity", mid + ".animation.json")))["animations"]
        for cn in clips:
            if cn not in an:
                print("PROBLEM", mid, "clip", cn, "missing")
                bad += 1
        for cn, cl in an.items():
            for bn in cl["bones"]:
                if bn not in names:
                    print("PROBLEM", mid, cn, "animates unknown bone", bn)
                    bad += 1
        print(mid, "cubes", sum(len(b["cubes"]) for b in g["bones"]), "OK" if not bad else "PROBLEMS")
    return 1 if bad else 0


def render_previews(names=None):
    import shutil
    import subprocess
    sc = scenes()
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp"))
    os.makedirs(SCRATCH, exist_ok=True)
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    for l in (r.stdout + r.stderr).splitlines():
        if "Picked up" not in l:
            print(l)
    for n in names:
        src = os.path.join(env["RENDER_PREVIEW_BUILD"], n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(SCRATCH, n + ".png"))
    return r.returncode


if __name__ == "__main__":
    if "--check" in sys.argv:
        sys.exit(check())
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    build_all()
    write_scenes()
