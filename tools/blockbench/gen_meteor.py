#!/usr/bin/env python3
"""The shared Meteor (docs/meteor_fall.md): the falling rock the Meteor Sword, Meteor of Bones, Meteor Swarm, the Visionary's meteor, Black Comet and Merlin's Starfall drop.

    python3 -B tools/blockbench/gen_meteor.py [--preview]

WHAT THIS IS (rule 14 / 15, said plainly): Blender and Blockbench could not run in the session (no bpy, no Blockbench), so this is the FALLBACK, not a model made in a modelling app.
The rock is a cluster of 14 rotated cubes (a lumpy boulder) and the tail is 4 tapering segments of two crossed cubes each, written as Blockbench's own source format
(tools/blockbench/meteor.bbmodel, box UV, NEVER opened in Blockbench: its rotation signs follow the Bedrock codec's convention, written from memory, so open it once and look before editing it)
and exported from the same data to the game files:
    src/main/resources/assets/anime_multiverse/geo/entity/meteor_rock.geo.json   (bone "rock", 64x64, centre at y = 12 px)
    src/main/resources/assets/anime_multiverse/geo/entity/meteor_tail.geo.json   (bone "tail", 128x128, grows along +y from the rock's centre)
    textures/entity/meteor_rock_<style>.png + _glow.png   and   meteor_tail_<style>.png      for the styles fire, ice, dark, ki, holy, star (+ the fire set again as the base names)
Textures are painted here with numpy (value noise, Voronoi cracks, speckles); the glow maps are the cracks, drawn additive and full bright by the renderer.
Everything is deterministic: re-running writes the same bytes. --preview also writes docs/previews/meteor/meteor_styles.png and meteor_fall_scene.png with a tiny software rasteriser
(the preview is only an approximation of the game: no lighting model of Minecraft, cube rotations in the file's frame).
"""
import base64
import io
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools"))

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import geo_builder  # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
TEX = os.path.join(ASSETS, "textures", "entity")
PREVIEW = os.path.join(ROOT, "docs", "previews", "meteor")
STYLES = ["fire", "ice", "dark", "ki", "holy", "star"]

# ---------------------------------------------------------------- the geometry (pixels, 1/16 block; centre of the rock at (0, 12, 0))
# (centre x, y, z), (size x, y, z), rotation degrees (x, y, z): every cube turns about its own centre
ROCK = [
    ((0, 12, 0), (14, 14, 14), (18, 32, 9)),
    ((-3, 10, 2), (12, 10, 13), (-24, 61, 12)),
    ((4, 14, -3), (11, 13, 11), (35, -14, -27)),
    ((1, 7, -4), (13, 9, 12), (-12, -48, 31)),
    ((-4, 16, -1), (10, 12, 10), (50, 20, -40)),
    ((8, 11, 5), (7, 6, 7), (22, 51, 18)),
    ((-9, 13, -4), (8, 7, 6), (-33, 12, 40)),
    ((2, 20, 4), (6, 7, 7), (14, -39, 25)),
    ((-2, 4, -2), (7, 5, 8), (28, 70, -15)),
    ((6, 6, -7), (6, 6, 5), (-45, 25, 33)),
    ((-7, 7, 6), (6, 5, 7), (19, -60, 8)),
    ((9, 16, -4), (5, 6, 5), (40, 10, 55)),
    ((-5, 19, 6), (5, 5, 6), (-20, 35, -50)),
    ((0, 12, 9), (6, 6, 4), (33, -22, 12)),
]
TAIL_W = [14, 12, 10, 8, 6, 4]
TAIL_H = 14
TAIL_START = 9                                                             # px above the rock's centre where the tail begins (just inside the rock's edge)
TAIL_UV = [(0, 0), (0, 28), (0, 54), (0, 78), (0, 100), (30, 100)]


def rock_uv(i, size):
    """A deterministic box-UV offset inside the 64x64 sheet, different for every cube so the same rock texture does not repeat on neighbours."""
    w, h, d = size
    u_max, v_max = 64 - (2 * d + 2 * w), 64 - (d + h)
    u = (i * 23) % (u_max + 1) if u_max > 0 else 0
    v = (i * 37) % (v_max + 1) if v_max > 0 else 0
    return int(u), int(v)


def build_models():
    rock = geo_builder.Model("meteor", "rock", 64, 64)
    rock.bone("rock", pivot=(0, 12, 0))
    cubes = []
    for i, (c, s, r) in enumerate(ROCK):
        o = (c[0] - s[0] / 2.0, c[1] - s[1] / 2.0, c[2] - s[2] / 2.0)
        uv = rock_uv(i, s)
        rock.cube("rock", o, s, uv=uv, rotation=r, pivot=c)
        cubes.append(dict(name="rock%02d" % i, origin=o, size=s, uv=uv, rotation=r, pivot=c))
    tail = geo_builder.Model("meteor", "tail", 128, 128)
    tail.bone("tail", pivot=(0, 0, 0))
    tcubes = []
    for i, w in enumerate(TAIL_W):
        y0 = TAIL_START + i * TAIL_H
        o = (-w / 2.0, y0, -w / 2.0)
        for j, ry in enumerate((0, 45)):
            tail.cube("tail", o, (w, TAIL_H, w), uv=TAIL_UV[i], rotation=(0, ry, 0) if ry else None, pivot=(0, y0 + TAIL_H / 2.0, 0))
            tcubes.append(dict(name="tail%d_%d" % (i, j), origin=o, size=(w, TAIL_H, w), uv=TAIL_UV[i], rotation=(0, ry, 0), pivot=(0, y0 + TAIL_H / 2.0, 0)))
    return rock, tail, cubes, tcubes


# ---------------------------------------------------------------- the paint
PALETTES = {
    #        rock base           crack glow          tail hot            tail mid            tail deep
    "fire": ((52, 38, 34), (255, 128, 30), (255, 244, 200), (255, 140, 30), (170, 30, 0)),
    "ice": ((150, 205, 235), (210, 250, 255), (240, 255, 255), (130, 215, 255), (40, 90, 200)),
    "dark": ((30, 22, 40), (176, 74, 255), (232, 192, 255), (140, 50, 230), (50, 10, 90)),
    "ki": ((236, 188, 70), (255, 255, 224), (255, 255, 240), (255, 225, 90), (255, 140, 20)),
    "holy": ((244, 238, 216), (255, 214, 100), (255, 255, 236), (255, 230, 140), (255, 190, 70)),
    "star": ((24, 34, 72), (160, 196, 255), (236, 246, 255), (130, 170, 255), (70, 70, 200)),
}


def value_noise(rng, n, cells):
    g = rng.random((cells + 1, cells + 1))
    xs = np.linspace(0, cells, n, endpoint=False)
    xi = xs.astype(int)
    f = xs - xi
    f = f * f * (3 - 2 * f)
    a = g[np.ix_(xi, xi)]
    b = g[np.ix_(xi + 1, xi)]
    c = g[np.ix_(xi, xi + 1)]
    d = g[np.ix_(xi + 1, xi + 1)]
    fx, fy = f[:, None], f[None, :]
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy


def fbm(rng, n, base=4, octaves=4):
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += amp * value_noise(rng, n, base * (2 ** o))
        tot += amp
        amp *= 0.5
    return out / tot


def voronoi_edges(rng, n, cells):
    """F2 - F1 of a wrapped Voronoi field: close to 0 on the borders between cells (the cracks)."""
    pts = rng.random((cells * cells, 2)) * n
    ys, xs = np.mgrid[0:n, 0:n]
    best1 = np.full((n, n), 1e9)
    best2 = np.full((n, n), 1e9)
    for px, py in pts:
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                d = np.hypot(xs - (px + ox), ys - (py + oy))
                m1 = d < best1
                best2 = np.where(m1, best1, np.minimum(best2, d))
                best1 = np.where(m1, d, best1)
    return best2 - best1


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def paint_rock(style):
    rng = np.random.default_rng(1000 + STYLES.index(style))
    n = 64
    base, crack_col, hot, mid, deep = PALETTES[style]
    grain = fbm(rng, n, 4, 4)
    fine = fbm(rng, n, 16, 2)
    edges = voronoi_edges(rng, n, 5)
    crack = np.clip(1.0 - edges / 2.4, 0, 1) ** 1.6                       # 1 on a crack, 0 away
    img = np.zeros((n, n, 3))
    shade = 0.55 + 0.9 * grain + 0.25 * (fine - 0.5)
    if style == "ice":
        shade = 0.8 + 0.5 * grain + 0.35 * np.clip(edges / 10.0, 0, 1)      # facets: lighter inside each cell
    for c in range(3):
        img[..., c] = base[c] * shade
    if style in ("holy", "ki"):
        img *= 0.9 + 0.2 * fine[..., None]
    # darker rim along the cracks, then the glowing core of the crack
    img *= (1.0 - 0.45 * np.clip(crack * 1.8, 0, 1))[..., None]
    glow = np.zeros((n, n, 3))
    core = np.clip((crack - 0.35) / 0.65, 0, 1) ** 1.2
    vein = 0.65 + 0.35 * fbm(rng, n, 6, 2)
    for c in range(3):
        glow[..., c] = crack_col[c] * core * vein
    # ember patches (fire, ki) or speckles (star, holy): a few small bright dots on the rock itself
    speck = (rng.random((n, n)) > (0.985 if style in ("star", "holy") else 0.995)).astype(float)
    sp_col = hot
    for c in range(3):
        glow[..., c] = np.maximum(glow[..., c], sp_col[c] * speck * (0.6 if style in ("star", "holy") else 0.8))
    if style in ("fire", "dark"):
        patch = np.clip((fbm(rng, n, 3, 3) - 0.62) * 6.0, 0, 1)
        for c in range(3):
            glow[..., c] = np.maximum(glow[..., c], crack_col[c] * patch * 0.45)
    img = np.clip(img, 0, 255)
    rgba = np.dstack([img, np.full((n, n), 255.0)]).astype(np.uint8)
    grgba = np.dstack([np.clip(glow, 0, 255), np.full((n, n), 255.0)]).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA"), Image.fromarray(grgba, "RGBA")


def paint_tail(style):
    """Additive streak: black = nothing, bright at the rock end, soft toward the edge of every face and the far end; hot core, coloured body, deep red/blue tip."""
    rng = np.random.default_rng(2000 + STYLES.index(style))
    _, _, hot, mid, deep = PALETTES[style]
    img = np.zeros((128, 128, 3))
    total = TAIL_H * len(TAIL_W)
    for i, w in enumerate(TAIL_W):
        u0, v0 = TAIL_UV[i]
        rects = [(u0, v0 + w, w, TAIL_H), (u0 + w, v0 + w, w, TAIL_H), (u0 + 2 * w, v0 + w, w, TAIL_H), (u0 + 3 * w, v0 + w, w, TAIL_H)]
        for (rx, ry, rw, rh) in rects:
            streaks = value_noise(rng, 32, 8)[:rw, :rh] if rw <= 32 and rh <= 32 else np.full((rw, rh), 0.5)
            for py in range(rh):
                fy = py / max(1, rh - 1)
                y_world = i * TAIL_H + (1.0 - fy) * TAIL_H                       # the top of the face is the end farther from the rock
                along = 1.0 - y_world / total
                fade_in = min(1.0, y_world / 7.0)                                   # no hard edge where the tail leaves the rock
                body = max(0.0, along) ** 1.35
                col = lerp(deep, mid, min(1.0, along * 1.6)) if along < 0.62 else lerp(mid, hot, (along - 0.62) / 0.38)
                for px in range(rw):
                    fx = px / max(1, rw - 1)
                    edge = math.sin(math.pi * fx) ** 1.3
                    k = body * edge * (0.78 + 0.45 * streaks[px % streaks.shape[0], py % streaks.shape[1]])
                    k = min(1.0, k * fade_in * 0.34)                                  # up to four faces of crossed cubes add up in game
                    img[ry + py, rx + px] = [col[c] * k for c in range(3)]
    a = (img.max(axis=2) > 2).astype(float) * 255.0
    return Image.fromarray(np.dstack([np.clip(img, 0, 255), a]).astype(np.uint8), "RGBA")


def write_textures():
    os.makedirs(TEX, exist_ok=True)
    for s in STYLES:
        rock, glow = paint_rock(s)
        tail = paint_tail(s)
        for prefix in ([s] + ([""] if s == "fire" else [])):
            suf = "_" + prefix if prefix else ""
            rock.save(os.path.join(TEX, "meteor_rock%s.png" % suf))
            glow.save(os.path.join(TEX, "meteor_rock%s_glow.png" % suf))
            tail.save(os.path.join(TEX, "meteor_tail%s.png" % suf))


# ---------------------------------------------------------------- Blockbench source (fallback, see the docstring)
def bb_cube(c, tex_index=0):
    o, s, r, p = c["origin"], c["size"], c["rotation"], c["pivot"]
    e = {"name": c["name"], "box_uv": True, "rescale": False, "locked": False, "light_emission": 0, "render_order": "default", "allow_mirror_modeling": False,
         "from": list(o), "to": [o[0] + s[0], o[1] + s[1], o[2] + s[2]], "autouv": 1, "color": 0, "origin": list(p),
         "uv_offset": list(c["uv"])}
    if any(r):
        e["rotation"] = [-r[0], -r[1], r[2]]                                      # the Bedrock codec's sign (x and y negated); see the module docstring
    return e


def uid(*parts):
    import uuid
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "meteor/" + "/".join(str(x) for x in parts)))


def write_bbmodel(cubes, tcubes):
    rock_png = os.path.join(TEX, "meteor_rock_fire.png")
    tail_png = os.path.join(TEX, "meteor_tail_fire.png")

    def tex(name, path, idx):
        b = open(path, "rb").read()
        return {"path": "", "name": name, "folder": "", "namespace": "", "id": str(idx), "particle": False, "render_mode": "default", "render_sides": "auto", "frame_time": 1,
                "frame_order_type": "loop", "frame_order": "", "frame_interpolation": False, "visible": True, "internal": True, "saved": False, "uuid": uid("tex", name),
                "source": "data:image/png;base64," + base64.b64encode(b).decode("ascii")}

    elements, children = [], []
    for c in cubes:
        e = bb_cube(c)
        e["uuid"] = uid(c["name"])
        elements.append(e)
    for c in tcubes:
        e = bb_cube(c)
        e["uuid"] = uid(c["name"])
        e["name"] = c["name"]
        elements.append(e)
    rock_ids = [e["uuid"] for e in elements if e["name"].startswith("rock")]
    tail_ids = [e["uuid"] for e in elements if e["name"].startswith("tail")]
    doc = {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": True}, "name": "meteor", "model_identifier": "meteor",
           "visible_box": [4, 4, 0], "variable_placeholders": "", "bedrock_animation_mode": "entity", "resolution": {"width": 64, "height": 64},
           "elements": elements,
           "outliner": [{"name": "rock", "origin": [0, 12, 0], "rotation": [0, 0, 0], "color": 0, "uuid": uid("bone", "rock"), "export": True, "isOpen": True, "visibility": True, "autouv": 0, "children": rock_ids},
                        {"name": "tail", "origin": [0, 0, 0], "rotation": [0, 0, 0], "color": 0, "uuid": uid("bone", "tail"), "export": True, "isOpen": False, "visibility": True, "autouv": 0, "children": tail_ids}],
           "textures": [tex("meteor_rock_fire.png", rock_png, 0), tex("meteor_tail_fire.png", tail_png, 1)], "animations": []}
    with open(os.path.join(HERE, "meteor.bbmodel"), "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=1)


# ---------------------------------------------------------------- the preview (software rasteriser)
def rot_mat(rx, ry, rz):
    """Rz . Ry . Rx in the file's y-up frame (x first), the rotation GeoDraw builds (see client/geo/GeoDraw#rotate)."""
    ax, ay, az = math.radians(rx), math.radians(ry), math.radians(rz)
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def cube_faces(c, post=None):
    """Six quads: (4 corners, uv rect (u0, v0, u1, v1) in pixels with v0 the top edge, normal) of a box-UV cube, rotated about its pivot."""
    o, s, r, p, (u, v) = np.array(c["origin"], float), c["size"], c["rotation"], np.array(c["pivot"], float), c["uv"]
    w, h, d = s
    R = rot_mat(*r)
    x0, y0, z0 = o
    x1, y1, z1 = o + np.array(s, float)

    def T(pt):
        q = R @ (np.array(pt, float) - p) + p
        return post(q) if post else q

    faces = []
    # north (-z): seen from -z, +x on the left
    faces.append(([T((x1, y1, z0)), T((x0, y1, z0)), T((x0, y0, z0)), T((x1, y0, z0))], (u + d, v + d, u + d + w, v + d + h)))
    faces.append(([T((x0, y1, z0)), T((x0, y1, z1)), T((x0, y0, z1)), T((x0, y0, z0))], (u + d + w, v + d, u + 2 * d + w, v + d + h)))      # west (-x)
    faces.append(([T((x0, y1, z1)), T((x1, y1, z1)), T((x1, y0, z1)), T((x0, y0, z1))], (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)))  # south (+z)
    faces.append(([T((x1, y1, z1)), T((x1, y1, z0)), T((x1, y0, z0)), T((x1, y0, z1))], (u, v + d, u + d, v + d + h)))                       # east (+x)
    faces.append(([T((x1, y1, z0)), T((x1, y1, z1)), T((x0, y1, z1)), T((x0, y1, z0))], (u + d, v, u + d + w, v + d)))                       # up
    faces.append(([T((x1, y0, z1)), T((x1, y0, z0)), T((x0, y0, z0)), T((x0, y0, z1))], (u + d + w, v, u + 2 * d + w, v + d)))              # down
    return faces


def raster(cubes_by_tex, size, view, light, bg=None, ss=2):
    """cubes_by_tex: list of (cubes, post, image, glow image or None, additive flag). view: 3x3 world->camera matrix, camera looks down -z, y up on screen."""
    W = size * ss
    zbuf = np.full((W, W), -1e9)
    col = np.zeros((W, W, 3))
    add = np.zeros((W, W, 3))
    scale = W / 150.0
    for cubes, post, img, glow, additive in cubes_by_tex:
        tex = np.asarray(img.convert("RGB"), float)
        gl = np.asarray(glow.convert("RGB"), float) if glow is not None else None
        th, tw = tex.shape[:2]
        for c in cubes:
            for quad, (u0, v0, u1, v1) in cube_faces(c, post):
                pts = [view @ q for q in quad]
                n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
                nl = np.linalg.norm(n)
                if nl < 1e-9:
                    continue
                n = n / nl
                facing = n[2]
                if facing < 0 and not additive:
                    continue                                                    # back face
                nw = np.cross(quad[1] - quad[0], quad[2] - quad[0])
                nw = nw / max(1e-9, np.linalg.norm(nw))
                lam = 0.45 + 0.7 * max(0.0, float(nw @ light))
                sc = [(W / 2 + p[0] * scale, W / 2 - p[1] * scale, p[2]) for p in pts]
                uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                for tri in ((0, 1, 2), (0, 2, 3)):
                    a, b, cc = [sc[i] for i in tri]
                    ua, ub, uc = [uvs[i] for i in tri]
                    minx, maxx = int(max(0, math.floor(min(a[0], b[0], cc[0])))), int(min(W - 1, math.ceil(max(a[0], b[0], cc[0]))))
                    miny, maxy = int(max(0, math.floor(min(a[1], b[1], cc[1])))), int(min(W - 1, math.ceil(max(a[1], b[1], cc[1]))))
                    if maxx < minx or maxy < miny:
                        continue
                    den = (b[1] - cc[1]) * (a[0] - cc[0]) + (cc[0] - b[0]) * (a[1] - cc[1])
                    if abs(den) < 1e-9:
                        continue
                    ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
                    px, py = xs + 0.5, ys + 0.5
                    l1 = ((b[1] - cc[1]) * (px - cc[0]) + (cc[0] - b[0]) * (py - cc[1])) / den
                    l2 = ((cc[1] - a[1]) * (px - cc[0]) + (a[0] - cc[0]) * (py - cc[1])) / den
                    l3 = 1 - l1 - l2
                    m = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
                    if not m.any():
                        continue
                    z = l1 * a[2] + l2 * b[2] + l3 * cc[2]
                    uu = l1 * ua[0] + l2 * ub[0] + l3 * uc[0]
                    vv = l1 * ua[1] + l2 * ub[1] + l3 * uc[1]
                    tx = np.clip(uu.astype(int), 0, tw - 1)
                    ty = np.clip(vv.astype(int), 0, th - 1)
                    if additive:
                        sel = m & (z > zbuf[miny:maxy + 1, minx:maxx + 1] - 3.0)
                        sub = add[miny:maxy + 1, minx:maxx + 1]
                        sub[sel] += tex[ty, tx][sel]
                    else:
                        zb = zbuf[miny:maxy + 1, minx:maxx + 1]
                        sel = m & (z > zb)
                        zb[sel] = z[sel]
                        cl = col[miny:maxy + 1, minx:maxx + 1]
                        cl[sel] = tex[ty, tx][sel] * lam
                        if gl is not None:
                            ad = add[miny:maxy + 1, minx:maxx + 1]
                            ad[sel] = gl[ty, tx][sel] * 1.0
    return col, add, zbuf > -1e8


def compose(col, add, mask, bg):
    from PIL import ImageFilter
    W = col.shape[0]
    base = np.array(bg, float)[None, None, :].repeat(W, 0).repeat(W, 1) if isinstance(bg, tuple) else bg.copy()
    body = np.where(mask[..., None], col, base)
    g = Image.fromarray(np.clip(add, 0, 255).astype(np.uint8), "RGB")
    bloom = np.asarray(g.filter(ImageFilter.GaussianBlur(W / 40.0)), float) * 1.4 + np.asarray(g.filter(ImageFilter.GaussianBlur(W / 14.0)), float) * 1.1
    out = np.clip(body + add + bloom, 0, 255)
    return Image.fromarray(out.astype(np.uint8), "RGB")


def view_matrix(yaw, pitch):
    """World -> camera: yaw about y, then pitch about x; the camera looks along -z."""
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    P = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return P @ Y


def render_style(style, size=360, yaw=-35, pitch=-14, spin=20.0, tail_dir=(0.55, 0.8, 0.1), bg=(10, 13, 26), ss=2):
    """One meteor of a style on a night-sky tile: the rock spun by `spin` degrees about its slanted axis, the tail pointed along tail_dir (away from the way it flies)."""
    rock, glow = paint_rock(style)
    tail = paint_tail(style)
    _, _, cubes, tcubes = build_models()
    view = view_matrix(yaw, pitch)
    ax = np.array([0.55, 0.75, 0.35])
    ax /= np.linalg.norm(ax)
    a = math.radians(spin)
    K = np.array([[0, -ax[2], ax[1]], [ax[2], 0, -ax[0]], [-ax[1], ax[0], 0]])
    spin_m = np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * (K @ K)
    centre = np.array([0.0, 12.0, 0.0])
    tdir = np.array(tail_dir, float)
    tdir /= np.linalg.norm(tdir)
    up = np.array([0.0, 1.0, 0.0])
    axis = np.cross(up, tdir)
    ang = math.acos(max(-1.0, min(1.0, float(up @ tdir))))
    if np.linalg.norm(axis) > 1e-6:
        axis = axis / np.linalg.norm(axis)
        K2 = np.array([[0, -axis[2], axis[1]], [axis[2], 0, -axis[0]], [-axis[1], axis[0], 0]])
        tail_m = np.eye(3) + math.sin(ang) * K2 + (1 - math.cos(ang)) * (K2 @ K2)
    else:
        tail_m = np.eye(3)
    light = np.array([-0.4, 0.8, 0.45])
    light /= np.linalg.norm(light)
    # the rock spins about its own centre and sits at the origin; the tail starts at that centre (its y = 0)
    mid = tdir * 30.0                                                                # the picture is centred between the rock and the middle of the tail
    col, add, mask = raster([(cubes, lambda p: view @ (spin_m @ (p - centre) - mid), rock, glow, False),
                             (tcubes, lambda p: view @ (tail_m @ p - mid), tail, None, True)], size, np.eye(3), light, ss=ss)
    return compose(col, add, mask, bg).resize((size, size), Image.LANCZOS)


def run():
    from PIL import ImageDraw
    os.makedirs(PREVIEW, exist_ok=True)
    tiles = [render_style(s) for s in STYLES]
    sheet = Image.new("RGB", (360 * 3, 360 * 2), (10, 13, 26))
    d = ImageDraw.Draw(sheet)
    for i, (s, t) in enumerate(zip(STYLES, tiles)):
        sheet.paste(t, ((i % 3) * 360, (i // 3) * 360))
        d.text(((i % 3) * 360 + 10, (i // 3) * 360 + 8), s, fill=(230, 230, 240))
    sheet.save(os.path.join(PREVIEW, "meteor_styles.png"))
    # the fall: three moments of one fire meteor coming in at an angle onto a ground line
    W, H = 1080, 540
    scene = Image.new("RGB", (W, H))
    px = scene.load()
    for y in range(H):
        t = y / H
        for x in range(W):
            px[x, y] = (int(8 + 30 * t), int(10 + 28 * t), int(24 + 40 * t))
    d = ImageDraw.Draw(scene)
    d.rectangle([0, 470, W, H], fill=(34, 30, 28))
    d.line([0, 470, W, 470], fill=(70, 62, 56), width=2)
    d.ellipse([760, 456, 960, 484], outline=(255, 130, 40), width=2)
    vel = np.array([-0.55, -0.85, 0.0])                                           # down and to the left
    for k, (cx, cy, sc) in enumerate([(210, 110, 0.62), (470, 260, 0.82), (740, 420, 1.0)]):
        tile = render_style("fire", size=int(300 * sc), yaw=-20, pitch=0, spin=40 * k, tail_dir=tuple(-vel))
        scene.paste(tile, (cx - tile.width // 2, cy - tile.height // 2), Image.fromarray((np.asarray(tile.convert("L")) > 22).astype(np.uint8) * 255, "L"))
    d.text((12, 10), "fall: slow start, fast end, in at an angle; the ring is the impact point", fill=(220, 220, 230))
    scene.save(os.path.join(PREVIEW, "meteor_fall_scene.png"))
    print("meteor: preview written to", PREVIEW)


def main():
    rock, tail, cubes, tcubes = build_models()
    rock.save()
    tail.save()
    write_textures()
    write_bbmodel(cubes, tcubes)
    print("meteor: wrote %d rock cubes, %d tail cubes, textures for %s, meteor.bbmodel" % (len(cubes), len(tcubes), ", ".join(STYLES)))
    if "--preview" in sys.argv:
        run()


if __name__ == "__main__":
    sys.path.insert(0, HERE)
    main()
