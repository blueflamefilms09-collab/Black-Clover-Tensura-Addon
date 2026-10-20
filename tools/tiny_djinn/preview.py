"""Preview of the Tiny Djinn without the game: a software renderer that repeats what client/geo/GeoDraw does (the same bone / cube / uv / rotation maths, the model's
own texture and its additive glow map), so what you see here is the model as the game poses it, minus the game's lighting.

    python3 -B tools/tiny_djinn/preview.py                      # the contact sheets + the scale scene into docs/previews/tiny_djinn/
    python3 -B tools/tiny_djinn/preview.py alloces --clip cast  # one Djinn, three views, to docs/previews/tiny_djinn/one_<id>.png
    python3 -B tools/tiny_djinn/preview.py --clips alloces      # the clip strip of one Djinn

Files written: contact_front_side_cast_<n>.png (all 36, front | side | cast pose), contact_dark.png (all 36 as the Dark Djinn), alloces_scale.png (Alloces on a
player's shoulder, to scale), clips_alloces.png (idle / cast / attack / hop / poof / exit).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(HERE, "..", "..", "docs", "previews", "tiny_djinn")
PX = 1.0 / 16.0


# ================================================================ maths of GeoDraw
def rx_m(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]], float)


def ry_m(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]], float)


def rz_m(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]], float)


def turn(rx, ry, rz):
    """GeoDraw.rotate: rotationZYX(-rz, ry, -rx) = Rz * Ry * Rx (JOML), degrees in."""
    d = math.pi / 180
    return rz_m(-rz * d) @ ry_m(ry * d) @ rx_m(-rx * d)


class Clip:
    def __init__(self, data):
        self.length = data.get("animation_length", 0.0)
        self.loop = data.get("loop", True)
        self.bones = data.get("bones", {})

    def time(self, seconds):
        if self.length <= 1e-6:
            return 0.0
        return seconds % self.length if self.loop else min(seconds, self.length)

    @staticmethod
    def sample(track, t, fill):
        if track is None:
            return None
        if not isinstance(track, dict):
            v = track if isinstance(track, list) else [track] * 3
            return [float(x) for x in v]
        keys = sorted((float(k), v) for k, v in track.items())
        if t <= keys[0][0]:
            return [float(x) for x in keys[0][1]]
        if t >= keys[-1][0]:
            return [float(x) for x in keys[-1][1]]
        for i in range(len(keys) - 1):
            if keys[i][0] <= t <= keys[i + 1][0]:
                span = keys[i + 1][0] - keys[i][0]
                k = 0 if span < 1e-6 else (t - keys[i][0]) / span
                return [keys[i][1][j] + (keys[i + 1][1][j] - keys[i][1][j]) * k for j in range(3)]
        return None


def load_json(*parts):
    with open(os.path.join(ROOT, *parts)) as f:
        return json.load(f)


class Model:
    def __init__(self, mid):
        g = load_json("geo", "entity", mid + ".geo.json")["minecraft:geometry"][0]
        self.tw, self.th = g["description"]["texture_width"], g["description"]["texture_height"]
        self.bones = {b["name"]: b for b in g["bones"]}
        self.children = {}
        self.roots = []
        for b in g["bones"]:
            p = b.get("parent")
            if p and p in self.bones:
                self.children.setdefault(p, []).append(b["name"])
            else:
                self.roots.append(b["name"])


def anim_clip(name):
    data = load_json("animations", "entity", "tinydjinn_anim.animation.json")["animations"]
    return Clip(data[name]) if name in data else None


# ================================================================ faces
# face -> (corner indices a b c d, outward normal, uv slot) as in GeoDraw.cube
FACES = [((0, 4, 6, 2), (-1, 0, 0), 0), ((1, 0, 2, 3), (0, 0, -1), 1), ((5, 1, 3, 7), (1, 0, 0), 2), ((4, 5, 7, 6), (0, 0, 1), 3),
         ((5, 4, 0, 1), (0, -1, 0), 4), ((3, 2, 6, 7), (0, 1, 0), 5)]


def cube_uvs(cube, tw, th):
    """Per face (u1, v1, u2, v2) in pixels: GeoModelData.parse / box."""
    sx, sy, sz = cube["size"]
    uv = cube["uv"]
    out = [None] * 6
    if isinstance(uv, dict):
        def face(k):
            f = uv.get(k)
            if f is None:
                return None
            return (f["uv"][0], f["uv"][1], f["uv"][0] + f["uv_size"][0], f["uv"][1] + f["uv_size"][1])
        out[0], out[1], out[2], out[3], out[4], out[5] = face("east"), face("north"), face("west"), face("south"), face("up"), face("down")
        if out[5] is not None:
            out[5] = (out[5][0], out[5][3], out[5][2], out[5][1])
    else:
        u, v = uv[0], uv[1]
        d, w, h = sz, sx, sy
        f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
        f10, f11, f12 = v, v + d, v + d + h
        out[0] = (f4, f11, f5, f12)
        out[1] = (f5, f11, f6, f12)
        out[2] = (f6, f11, f8, f12)
        out[3] = (f8, f11, f9, f12)
        out[4] = (f5, f10, f6, f11)
        out[5] = (f6, f11, f7, f10)
    return out


def pose_model(model, clip, seconds, root_xform=None):
    """Returns a list of quads: (world corners 4x3, uv corners 4x2 in pixels, normal 3) in the model's PROP space (blocks), y up, front +z."""
    t = clip.time(seconds) if clip else 0.0
    quads = []

    def bone(name, parent, M):
        b = model.bones[name]
        pv = b["pivot"]
        pp = model.bones[parent]["pivot"] if parent else (0, 0, 0)
        x, y, z = pv[0] - pp[0], -pv[1] + pp[1], pv[2] - pp[2]
        rot = list(b.get("rotation", [0, 0, 0]))
        sc = [1.0, 1.0, 1.0]
        if clip and name in clip.bones:
            a = clip.bones[name]
            r = Clip.sample(a.get("rotation"), t, 0.0)
            if r:
                rot = [rot[i] + r[i] for i in range(3)]
            p = Clip.sample(a.get("position"), t, 0.0)
            if p:
                x, y, z = x + p[0], y - p[1], z + p[2]
            s = Clip.sample(a.get("scale"), t, 1.0)
            if s:
                sc = s
        B = np.eye(4)
        B[:3, 3] = (x, y, z)
        R = np.eye(4)
        R[:3, :3] = turn(*rot)
        S = np.diag([sc[0], sc[1], sc[2], 1.0])
        Mb = M @ B @ R @ S
        for cu in b["cubes"]:
            cube(cu, b, Mb)
        for ch in model.children.get(name, []):
            bone(ch, name, Mb)

    def cube(cu, b, M):
        o, s = cu["origin"], cu["size"]
        g = cu.get("inflate", 0.0)
        bp = b["pivot"]
        minx, maxx = o[0] - bp[0] - g, o[0] - bp[0] + s[0] + g
        miny, maxy = -(o[1] + s[1]) + bp[1] - g, -o[1] + bp[1] + g
        minz, maxz = o[2] - bp[2] - g, o[2] - bp[2] + s[2] + g
        mirror = cu.get("mirror", b.get("mirror", False))
        if mirror:
            minx, maxx = maxx, minx
        corners = np.array([[minx if not (i & 1) else maxx, miny if not (i & 2) else maxy, minz if not (i & 4) else maxz] for i in range(8)], float)
        Mc = M
        if "rotation" in cu:
            pc = cu.get("pivot", bp)
            cx, cy, cz = pc[0] - bp[0], -pc[1] + bp[1], pc[2] - bp[2]
            T1 = np.eye(4)
            T1[:3, 3] = (cx, cy, cz)
            R = np.eye(4)
            R[:3, :3] = turn(*cu["rotation"])
            T2 = np.eye(4)
            T2[:3, 3] = (-cx, -cy, -cz)
            Mc = M @ T1 @ R @ T2
        uvs = cube_uvs(cu, model.tw, model.th)
        for (ci, nrm, slot) in FACES:
            if uvs[slot] is None:
                continue
            u1, v1, u2, v2 = uvs[slot]
            pts = []
            tex = []
            for k in range(4):
                i = 3 - k if mirror else k
                ccorner = ci[i]
                p = Mc @ np.array([*corners[ccorner], 1.0])
                pts.append(p[:3])
                tex.append(((u2 if i in (0, 3) else u1), (v1 if i < 2 else v2)))
            n = Mc[:3, :3] @ np.array(nrm, float)
            if mirror:
                n = Mc[:3, :3] @ np.array((-nrm[0], nrm[1], nrm[2]), float)
            quads.append((np.array(pts), np.array(tex, float), n / (np.linalg.norm(n) + 1e-9)))

    for r in model.roots:
        bone(r, None, np.eye(4) if root_xform is None else root_xform)
    # PROP space: pose.scale(PX, -PX, -PX): vanilla (x, y, z) -> world (x, -y, -z) * PX
    out = []
    for pts, tex, n in quads:
        w = np.stack([pts[:, 0], -pts[:, 1], -pts[:, 2]], 1) * PX
        nn = np.array([n[0], -n[1], -n[2]])
        out.append((w, tex, nn))
    return out


# ================================================================ the rasteriser
class Raster:
    def __init__(self, w, h, bg=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.col = np.zeros((h, w, 4), np.float32)
        self.col[:] = bg
        self.depth = np.full((h, w), -1e9, np.float32)
        self.add = np.zeros((h, w, 3), np.float32)

    def tri(self, P, T, tex, shade, mode):
        """P: 3x3 screen (x, y, depth), T: 3x2 uv (pixels), tex: HxWx4 float array, shade: brightness, mode 'main' | 'glow'."""
        x0, y0 = int(max(0, math.floor(min(P[:, 0])))), int(max(0, math.floor(min(P[:, 1]))))
        x1, y1 = int(min(self.w - 1, math.ceil(max(P[:, 0])))), int(min(self.h - 1, math.ceil(max(P[:, 1]))))
        if x1 < x0 or y1 < y0:
            return
        xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        d = (P[1, 1] - P[2, 1]) * (P[0, 0] - P[2, 0]) + (P[2, 0] - P[1, 0]) * (P[0, 1] - P[2, 1])
        if abs(d) < 1e-9:
            return
        l0 = ((P[1, 1] - P[2, 1]) * (xs - P[2, 0]) + (P[2, 0] - P[1, 0]) * (ys - P[2, 1])) / d
        l1 = ((P[2, 1] - P[0, 1]) * (xs - P[2, 0]) + (P[0, 0] - P[2, 0]) * (ys - P[2, 1])) / d
        l2 = 1 - l0 - l1
        inside = (l0 >= -1e-4) & (l1 >= -1e-4) & (l2 >= -1e-4)
        if not inside.any():
            return
        z = l0 * P[0, 2] + l1 * P[1, 2] + l2 * P[2, 2]
        u = l0 * T[0, 0] + l1 * T[1, 0] + l2 * T[2, 0]
        v = l0 * T[0, 1] + l1 * T[1, 1] + l2 * T[2, 1]
        th, tw = tex.shape[:2]
        ui = np.clip(np.floor(u).astype(int), 0, tw - 1)
        vi = np.clip(np.floor(v).astype(int), 0, th - 1)
        px = tex[vi, ui]
        sl = (slice(y0, y1 + 1), slice(x0, x1 + 1))
        if mode == "main":
            ok = inside & (px[..., 3] > 8)
            near = ok & (z >= self.depth[sl] - 1e-6)
            opaque = near & (px[..., 3] >= 200)
            trans = near & (px[..., 3] < 200)
            c = px[..., :3] * shade
            col = self.col[sl]
            dep = self.depth[sl]
            col[opaque, :3] = c[opaque]
            col[opaque, 3] = 255
            dep[opaque] = z[opaque]
            a = (px[..., 3] / 255.0)[trans][:, None]
            col[trans, :3] = col[trans, :3] * (1 - a) + c[trans] * a
            col[trans, 3] = np.maximum(col[trans, 3], px[trans, 3])
        else:
            ok = inside & (px[..., 3] > 8) & (z >= self.depth[sl] - 0.02)
            self.add[sl][ok] += px[..., :3][ok]

    def image(self, bg=(24, 24, 32)):
        c = np.clip(self.col[..., :3] + self.add * 0.85, 0, 255)
        a = self.col[..., 3:4] / 255.0
        a = np.maximum(a, np.clip(self.add.max(axis=2, keepdims=True) / 255.0, 0, 1))
        out = c * a + np.array(bg, np.float32) * (1 - a)
        return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB")


def tex_array(path):
    return np.asarray(Image.open(path).convert("RGBA"), np.float32)


LIGHT0, LIGHT1 = np.array([0.2, 1.0, -0.7]), np.array([-0.2, 1.0, 0.7])
LIGHT0, LIGHT1 = LIGHT0 / np.linalg.norm(LIGHT0), LIGHT1 / np.linalg.norm(LIGHT1)


def shade_of(n, view_yaw):
    b = 0.4 + 0.6 * max(0.0, float(n @ LIGHT0)) * 0.9 + 0.6 * max(0.0, float(n @ LIGHT1)) * 0.5
    return min(1.0, b)


def render(quads_list, size, scale, cx, cy, yaw_deg, pitch_deg=12.0, bg=(26, 26, 36), ss=2):
    """quads_list: [(quads, tex_main, tex_glow or None, mat or None)]. Ortho camera looking at the figure from yaw (0 = from the front, +z)."""
    W = (size[0] * ss, size[1] * ss)
    R = Raster(W[0], W[1])
    yaw = math.radians(yaw_deg)
    pit = math.radians(pitch_deg)
    cam = ry_m(-yaw)                                       # rotate the world so the camera looks along -z of its own frame
    # x' = x cos + z sin ; z' = -x sin + z cos with the camera on +z at yaw 0 (x to the right)
    Cm = rx_m(pit) @ np.array([[math.cos(yaw), 0, math.sin(yaw)], [0, 1, 0], [-math.sin(yaw), 0, math.cos(yaw)]])
    for quads, tex, glow, mat in quads_list:
        for (w, uv, n) in quads:
            pw = w if mat is None else (w @ mat[:3, :3].T + mat[:3, 3])
            nn = n if mat is None else (mat[:3, :3] @ n)
            cp = pw @ Cm.T
            nc = Cm @ nn
            scr = np.stack([W[0] / 2 + (cp[:, 0] - cx) * scale * ss, W[1] / 2 - (cp[:, 1] - cy) * scale * ss, cp[:, 2]], 1)
            sh = shade_of(np.array([nn[0], nn[1], nn[2]]), yaw)
            for tri in ((0, 1, 2), (0, 2, 3)):
                R.tri(scr[list(tri)], uv[list(tri)], tex, sh, "main")
    for quads, tex, glow, mat in quads_list:
        if glow is None:
            continue
        for (w, uv, n) in quads:
            pw = w if mat is None else (w @ mat[:3, :3].T + mat[:3, 3])
            cp = pw @ Cm.T
            scr = np.stack([W[0] / 2 + (cp[:, 0] - cx) * scale * ss, W[1] / 2 - (cp[:, 1] - cy) * scale * ss, cp[:, 2]], 1)
            for tri in ((0, 1, 2), (0, 2, 3)):
                R.tri(scr[list(tri)], uv[list(tri)], glow, 1.0, "glow")
    img = R.image(bg)
    return img.resize(tuple(size), Image.LANCZOS) if ss > 1 else img


# ================================================================ a figure
FIG_H = 33.0                      # model px from the tail tip (y -18) to the head top (+15); TinyDjinnLayer.MODEL_HEIGHT_PX
TARGET_BLOCKS = 0.45


def figure_quads(did, clip_name="idle", seconds=0.0, dark=False):
    mid = "tinydjinn_" + did
    m = Model(mid)
    clip = anim_clip(clip_name)
    q = pose_model(m, clip, seconds)
    tag = "_dark" if dark else ""
    tex = tex_array(os.path.join(ROOT, "textures", "entity", mid + tag + ".png"))
    glow = tex_array(os.path.join(ROOT, "textures", "entity", mid + tag + "_glow.png"))
    return q, tex, glow


def sheet_cell(did, clip, secs, yaw, dark=False, size=(150, 190)):
    q, tex, glow = figure_quads(did, clip, secs, dark)
    # blocks -> a view where the figure fills the cell: 1 block = 16 model px at scale 1 (model px = 1/16 block)
    scale = min(size[1] / (44.0 * PX), size[0] / (30.0 * PX))
    return render([(q, tex, glow, None)], size, scale, 0.0, 2.0 * PX, yaw)


def sheet(ids, views, cols, name, dark=False, labels=True, cell=(150, 190)):
    rows = (len(ids) + cols - 1) // cols
    vw = len(views)
    W, H = cols * cell[0] * vw, rows * (cell[1] + 14)
    img = Image.new("RGB", (W, H), (20, 20, 28))
    d = ImageDraw.Draw(img)
    for i, did in enumerate(ids):
        cx, cy = (i % cols) * cell[0] * vw, (i // cols) * (cell[1] + 14)
        for k, (clip, secs, yaw) in enumerate(views):
            img.paste(sheet_cell(did, clip, secs, yaw, dark, cell), (cx + k * cell[0], cy + 14))
        d.text((cx + 4, cy + 1), did + (" (dark)" if dark else ""), fill=(230, 230, 240))
    path = os.path.join(OUT, name)
    img.save(path)
    print("wrote", path, img.size)


# ================================================================ the scale scene: Alloces on a player's right shoulder
def box_quads(o, s, rot_y=0.0):
    x0, y0, z0 = o
    x1, y1, z1 = x0 + s[0], y0 + s[1], z0 + s[2]
    c = [(x0 if not (i & 1) else x1, y0 if not (i & 2) else y1, z0 if not (i & 4) else z1) for i in range(8)]
    out = []
    for (ci, nrm, slot) in FACES:
        pts = np.array([c[k] for k in ci], float) * PX
        out.append((pts, np.array([[0, 0], [1, 0], [1, 1], [0, 1]], float), np.array(nrm, float)))
    return out


def flat_tex(rgb):
    t = np.zeros((2, 2, 4), np.float32)
    t[:] = (rgb[0], rgb[1], rgb[2], 255)
    return t


def scale_scene(did="alloces", path=None):
    """A player (32 px tall: head 8, body 12 x 8 x 4, arms and legs 4 x 12 x 4) seen from the front-right, Alloces hovering at the right shoulder at the size the layer draws it."""
    parts = [((-4, 24, -4), (8, 8, 8), (214, 168, 132)), ((-4, 12, -2), (8, 12, 4), (60, 120, 170)), ((-8, 12, -2), (4, 12, 4), (60, 120, 170)),
             ((4, 12, -2), (4, 12, 4), (60, 120, 170)), ((-4, 0, -2), (4, 12, 4), (50, 56, 110)), ((0, 0, -2), (4, 12, 4), (50, 56, 110))]
    lst = []
    for o, s, col in parts:
        lst.append((box_quads(o, s), flat_tex(col), None, None))
    q, tex, glow = figure_quads(did, "idle", 1.2)
    k = TARGET_BLOCKS / (FIG_H * PX)                         # the layer's scale: the figure is 0.45 block tall
    # the anchor: behind and to the player's right (-x for a figure facing +z), a little above the shoulder
    ax, ay, az = -0.55, 1.55, 0.35
    M = np.eye(4)
    M[:3, :3] = np.eye(3) * k
    M[:3, 3] = (ax, ay, az)
    lst.append((q, tex, glow, M))
    img = render(lst, (520, 520), 220.0, 0.0, 0.95, 28.0, 10.0, ss=2)
    d = ImageDraw.Draw(img)
    d.text((8, 6), "%s on the right shoulder: 0.45 block tall, a player is 2 blocks" % did.capitalize(), fill=(230, 230, 240))
    path = path or os.path.join(OUT, "%s_scale.png" % did)
    img.save(path)
    print("wrote", path)


def clip_strip(did):
    from tdanim import CLIPS
    cells = [("idle", 1.5), ("cast", 0.4), ("attack", 0.36), ("hop", 0.4), ("poof", 0.18), ("exit", 0.3)]
    img = Image.new("RGB", (len(cells) * 170, 230), (20, 20, 28))
    d = ImageDraw.Draw(img)
    for i, (c, t) in enumerate(cells):
        cell = sheet_cell(did, c, t, 28.0, False, (170, 210))
        img.paste(cell, (i * 170, 20))
        d.text((i * 170 + 6, 4), "%s %.2fs" % (c, t), fill=(230, 230, 240))
    path = os.path.join(OUT, "clips_%s.png" % did)
    img.save(path)
    print("wrote", path)


def all_ids():
    with open(os.path.join(HERE, "specs.json")) as f:
        return [s["id"] for s in json.load(f)["djinn"]]


def main(argv):
    os.makedirs(OUT, exist_ok=True)
    ids = all_ids()
    if "--clips" in argv:
        clip_strip(argv[argv.index("--clips") + 1])
        return
    sel = [a for a in argv if not a.startswith("-") and a in ids]
    if sel and "--clip" not in argv:
        sheet(sel, [("idle", 1.5, 0.0), ("idle", 1.5, 90.0), ("cast", 0.4, 28.0)], 1, "one_%s.png" % sel[0], cell=(240, 300))
        return
    if sel:
        sheet(sel, [("idle", 1.5, 0.0), ("idle", 1.5, 90.0), ("cast", 0.4, 28.0)], 1, "one_%s.png" % sel[0], cell=(240, 300))
        return
    n = len(ids)
    half = (n + 1) // 2
    sheet(ids[:half], [("idle", 1.5, 0.0), ("idle", 1.5, 90.0), ("cast", 0.4, 28.0)], 3, "contact_front_side_cast_1.png")
    sheet(ids[half:], [("idle", 1.5, 0.0), ("idle", 1.5, 90.0), ("cast", 0.4, 28.0)], 3, "contact_front_side_cast_2.png")
    sheet(ids, [("idle", 1.5, 20.0)], 9, "contact_dark.png", dark=True)
    scale_scene("alloces")
    clip_strip("alloces")


if __name__ == "__main__":
    main(sys.argv[1:])
