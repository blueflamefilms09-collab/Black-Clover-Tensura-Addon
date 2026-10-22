"""
Headless preview of the Compass Magic 3D models (compass3d/CompassMesh.java, the real in-game geometry).

    python3 -B tools/compass/preview_compass3d.py

Compiles CompassMesh with tools/compass/MeshDump.java (plain javac, no Minecraft needed), dumps its quads, and rasterises scenes with a z-buffer: nearest-texel textures,
cutout alpha, item-style lighting, the glass alpha blended, glows additive (the aura, the dial glow, the glint). Output: docs/previews/compass_3d/*.png
(compass_views.png: front / three quarter / side / back; caster.png: the compass floating beside a caster; spells.png: the arrow-needle, the route, the seal disc).
This is a re-implementation of the renderer (client/compass/CompassRender), not the game: lighting and blending are close, not identical.
"""
import math
import os
import subprocess
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
MESH = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "compass3d", "CompassMesh.java")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "entity", "compass3d")
OUT = os.path.join(ROOT, "docs", "previews", "compass_3d")
BUILD = os.path.join(ROOT, "build", "compass_preview")
SS = 2                      # supersampling
W = H = 480

LAYER_TEX = {0: "rim", 1: "face", 2: "star", 3: "needle", 4: "glass"}
_cache = {}


def tex(name):
    if name not in _cache:
        _cache[name] = np.asarray(Image.open(os.path.join(TEX, name + ".png")).convert("RGBA"), np.float32) / 255
    return _cache[name]


def build_dump():
    os.makedirs(BUILD, exist_ok=True)
    subprocess.run(["javac", "-nowarn", "-d", BUILD, MESH, os.path.join(HERE, "MeshDump.java")], check=True, capture_output=True,
                   env={**os.environ, "JAVA_TOOL_OPTIONS": ""})
    out = subprocess.run(["java", "-cp", BUILD, "MeshDump"], check=True, capture_output=True, text=True, env={**os.environ, "JAVA_TOOL_OPTIONS": ""}).stdout
    parts = {}
    for line in out.splitlines():
        v = line.split()
        name, layer = v[0], int(v[1])
        a = np.array([float(x) for x in v[2:]], np.float32)
        parts.setdefault(name, []).append((layer, a[:20].reshape(4, 5), a[20:23]))
    return parts


# ---------------------------------------------------------------- matrices
def T(x, y, z):
    m = np.eye(4, dtype=np.float32); m[:3, 3] = [x, y, z]; return m


def S(x, y=None, z=None):
    y = x if y is None else y
    z = x if z is None else z
    return np.diag([x, y, z, 1]).astype(np.float32)


def RX(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], np.float32)


def RY(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], np.float32)


def RZ(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], np.float32)


def xf(m, p):
    return (m[:3, :3] @ p.T).T + m[:3, 3]


# ---------------------------------------------------------------- scene
class Prim:
    """One draw: world-space quads of a layer, a mode ('solid' cutout, 'add' additive, 'alpha' translucent), a tint colour and alpha, a texture name."""
    def __init__(self, quads, tex_name, mode, tint=(1, 1, 1), alpha=1.0, shade=True):
        self.quads, self.tex, self.mode, self.tint, self.alpha, self.shade = quads, tex_name, mode, np.array(tint, np.float32), alpha, shade


def part_prims(parts, name, m, layers=None, mode="solid", tint=(1, 1, 1), alpha=1.0, tex_override=None):
    by_layer = {}
    for layer, q, n in parts[name]:
        qq = np.concatenate([xf(m, q[:, :3]), q[:, 3:]], axis=1)
        nn = m[:3, :3] @ n
        nn = nn / max(1e-9, np.linalg.norm(nn))
        by_layer.setdefault(layer, []).append((qq, nn))
    out = []
    for layer, qs in by_layer.items():
        if layers is not None and layer not in layers:
            continue
        out.append(Prim(qs, tex_override or LAYER_TEX[layer], mode, tint, alpha))
    return out


def card(center, right, up, tex_name, tint, alpha, mode="add", u0=0, v0=0, u1=1, v1=1):
    c = np.array(center, np.float32); r = np.array(right, np.float32); u = np.array(up, np.float32)
    n = np.cross(r, u); n /= max(1e-9, np.linalg.norm(n))
    q = np.array([[*(c - r - u), u0, v1], [*(c + r - u), u1, v1], [*(c + r + u), u1, v0], [*(c - r + u), u0, v0]], np.float32)
    return Prim([(q, n)], tex_name, mode, tint, alpha, shade=False)


def compass_prims(parts, m, needle_deg, t=0.0, aura=True, glass=True, glint=True, solid_alpha=1.0, cam_right=None, eye=None):
    """The whole floating compass at model matrix m (model space radius 1)."""
    ps = []
    ps += part_prims(parts, "rim", m, mode="solid")
    ps += part_prims(parts, "face", m, mode="solid")
    ps += part_prims(parts, "star", m, mode="solid")
    ps += part_prims(parts, "needle", m @ RZ(needle_deg), mode="solid")
    # glow passes (additive): the dial's gold lines, the star and the needle a little
    ps += part_prims(parts, "face", m @ T(0, 0, 0.004), mode="add", alpha=0.8, tex_override="face_glow")
    ps += part_prims(parts, "star", m @ T(0, 0, 0.004), mode="add", alpha=0.22)
    ps += part_prims(parts, "needle", m @ RZ(needle_deg) @ T(0, 0, 0.004), mode="add", alpha=0.25)
    if aura:
        right = m[:3, :3] @ np.array([1, 0, 0], np.float32); up = m[:3, :3] @ np.array([0, 1, 0], np.float32)
        z = m[:3, :3] @ np.array([0, 0, 1], np.float32)
        spin = math.radians(t * 3.0)
        r2 = right * math.cos(spin) + up * math.sin(spin); u2 = -right * math.sin(spin) + up * math.cos(spin)
        side = 1.0 if eye is None or float((np.array(eye, np.float32) - m[:3, 3]) @ z) >= 0 else -1.0       # the card always sits on the far side of the compass from the camera
        centre = m[:3, 3] - z * 0.3 * side
        k = 2.15 * float(np.linalg.norm(m[:3, 0]))
        ps.append(card(centre, r2 * k, u2 * k, "aura", (1, 1, 1), 0.85 + 0.1 * math.sin(t * 0.3)))
    if glass:
        ps += part_prims(parts, "glass", m, mode="alpha", alpha=1.0)
    if glint:
        a = math.radians(40 + t * 4)
        right = m[:3, :3] @ np.array([1, 0, 0], np.float32); up = m[:3, :3] @ np.array([0, 1, 0], np.float32); z = m[:3, :3] @ np.array([0, 0, 1], np.float32)
        p = m[:3, 3] + (right * math.cos(a) + up * math.sin(a)) * 0.93 + z * 0.14
        if cam_right is not None:
            ps.append(card(p, cam_right[0] * 0.2, cam_right[1] * 0.2, "glint", (1, 1, 1), 1.0))
    return ps


# ---------------------------------------------------------------- camera and raster
def look(eye, target, fov=32):
    eye = np.array(eye, np.float32); target = np.array(target, np.float32)
    f = target - eye; f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r)
    u = np.cross(r, f)
    return eye, f, r, u, fov


def project(p, cam, w, h):
    eye, f, r, u, fov = cam
    d = p - eye
    z = d @ f
    k = (h / 2) / math.tan(math.radians(fov) / 2)
    return np.stack([w / 2 + (d @ r) / z * k, h / 2 - (d @ u) / z * k, z], axis=-1)


class Canvas:
    def __init__(self, w, h, bg):
        self.w, self.h = w * SS, h * SS
        self.img = np.zeros((self.h, self.w, 3), np.float32)
        yy = np.linspace(0, 1, self.h)[:, None, None]
        self.img[...] = np.array(bg[0], np.float32) * (1 - yy) + np.array(bg[1], np.float32) * yy
        self.z = np.full((self.h, self.w), np.inf, np.float32)

    def draw(self, prims, cam):
        eye, f, r, u, fov = cam
        light = np.array([0.2, 1.0, -0.7], np.float32); light /= np.linalg.norm(light)
        light2 = np.array([-0.2, 1.0, 0.7], np.float32); light2 /= np.linalg.norm(light2)
        for mode in ("solid", "add", "alpha"):
            for pr in prims:
                if pr.mode != mode:
                    continue
                t = tex(pr.tex)
                for q, n in pr.quads:
                    P = project(q[:, :3], cam, self.w, self.h)
                    if np.any(P[:, 2] <= 0.05):
                        continue
                    # two-sided: flip the normal toward the viewer (the game draws these no-cull)
                    nn = n if (n @ (eye - q[:, :3].mean(0))) >= 0 else -n
                    sh = 0.42 + 0.38 * max(0.0, float(nn @ light)) + 0.22 * max(0.0, float(nn @ light2)) if pr.shade else 1.0
                    for tri in ((0, 1, 2), (0, 2, 3)):
                        self.raster(P[list(tri)], q[list(tri), 3:5], t, pr, sh)

    def raster(self, P, UV, t, pr, shade):
        x0, x1 = int(max(0, math.floor(P[:, 0].min()))), int(min(self.w - 1, math.ceil(P[:, 0].max())))
        y0, y1 = int(max(0, math.floor(P[:, 1].min()))), int(min(self.h - 1, math.ceil(P[:, 1].max())))
        if x0 > x1 or y0 > y1:
            return
        (ax, ay, az), (bx, by, bz), (cx, cy, cz) = P
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-9:
            return
        yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1].astype(np.float32) + 0.5
        w0 = ((by - cy) * (xx - cx) + (cx - bx) * (yy - cy)) / den
        w1 = ((cy - ay) * (xx - cx) + (ax - cx) * (yy - cy)) / den
        w2 = 1 - w0 - w1
        inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
        if not inside.any():
            return
        z = 1 / (w0 / az + w1 / bz + w2 / cz)
        u = (w0 * UV[0, 0] / az + w1 * UV[1, 0] / bz + w2 * UV[2, 0] / cz) * z
        v = (w0 * UV[0, 1] / az + w1 * UV[1, 1] / bz + w2 * UV[2, 1] / cz) * z
        th, tw = t.shape[:2]
        s = t[np.clip((v * th).astype(int), 0, th - 1), np.clip((u * tw).astype(int), 0, tw - 1)]
        sub, region = self.z[y0:y1 + 1, x0:x1 + 1], self.img[y0:y1 + 1, x0:x1 + 1]
        if pr.mode == "solid":
            m = inside & (s[..., 3] > 0.1) & (z < sub - 1e-5)
            region[m] = (s[..., :3] * pr.tint * shade * pr.alpha)[m]
            sub[m] = z[m]
        elif pr.mode == "add":
            m = inside & (z < sub + 1e-3)
            region[m] = np.clip(region + s[..., :3] * s[..., 3:4] * pr.tint * pr.alpha, 0, 1)[m]
        else:
            m = inside & (z < sub + 1e-3)
            a = (s[..., 3:4] * pr.alpha)
            region[m] = (region * (1 - a) + s[..., :3] * pr.tint * shade * a)[m]

    def finish(self):
        img = self.img.reshape(self.h // SS, SS, self.w // SS, SS, 3).mean(axis=(1, 3))
        return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGB")


def label(img, text):
    from PIL import ImageDraw
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, len(text) * 6 + 8, 14], fill=(0, 0, 0))
    d.text((4, 2), text, fill=(255, 255, 255))
    return img


# ---------------------------------------------------------------- the caster figure (a plain stand-in: blocks, Minecraft proportions)
def box_prim(center, half, colour, yaw=0.0):
    cx, cy, cz = center
    hx, hy, hz = half
    m = T(cx, cy, cz) @ RY(yaw)
    faces = [((-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1), (0, 0, 1)), ((1, -1, -1), (-1, -1, -1), (-1, 1, -1), (1, 1, -1), (0, 0, -1)),
             ((1, -1, 1), (1, -1, -1), (1, 1, -1), (1, 1, 1), (1, 0, 0)), ((-1, -1, -1), (-1, -1, 1), (-1, 1, 1), (-1, 1, -1), (-1, 0, 0)),
             ((-1, 1, 1), (1, 1, 1), (1, 1, -1), (-1, 1, -1), (0, 1, 0)), ((-1, -1, -1), (1, -1, -1), (1, -1, 1), (-1, -1, 1), (0, -1, 0))]
    quads = []
    for a, b, c, d, n in faces:
        pts = np.array([[a[0] * hx, a[1] * hy, a[2] * hz], [b[0] * hx, b[1] * hy, b[2] * hz], [c[0] * hx, c[1] * hy, c[2] * hz], [d[0] * hx, d[1] * hy, d[2] * hz]], np.float32)
        pts = xf(m, pts)
        nn = m[:3, :3] @ np.array(n, np.float32)
        q = np.concatenate([pts, np.array([[0, 1], [1, 1], [1, 0], [0, 0]], np.float32)], axis=1)
        quads.append((q, nn))
    return Prim(quads, "_flat", "solid", colour)


def caster_prims(yaw=0.0):
    ps = []
    skin, cloth, dark = (0.86, 0.68, 0.55), (0.18, 0.42, 0.30), (0.16, 0.16, 0.2)
    def part(c, h, col):
        ps.append(box_prim(c, h, col, yaw))
    # in the model frame of a player facing +z (feet at y = 0), rotated by yaw about the feet
    def at(x, y, z):
        m = RY(yaw)
        return tuple(xf(m, np.array([[x, y, z]], np.float32))[0])
    ps.append(Prim(box_prim(at(0, 1.62, 0), (0.25, 0.25, 0.25), skin, yaw).quads, "_flat", "solid", skin))
    ps.append(Prim(box_prim(at(0, 1.12, 0), (0.25, 0.375, 0.125), cloth, yaw).quads, "_flat", "solid", cloth))
    ps.append(Prim(box_prim(at(-0.375, 1.12, 0), (0.125, 0.375, 0.125), skin, yaw).quads, "_flat", "solid", skin))
    ps.append(Prim(box_prim(at(0.375, 1.12, 0), (0.125, 0.375, 0.125), skin, yaw).quads, "_flat", "solid", skin))
    ps.append(Prim(box_prim(at(-0.125, 0.375, 0), (0.125, 0.375, 0.125), dark, yaw).quads, "_flat", "solid", dark))
    ps.append(Prim(box_prim(at(0.125, 0.375, 0), (0.125, 0.375, 0.125), dark, yaw).quads, "_flat", "solid", dark))
    return ps


def _flat_tex():
    _cache["_flat"] = np.ones((2, 2, 4), np.float32)


# ---------------------------------------------------------------- scenes
def views(parts):
    shots = [("front", 0, 4), ("three-quarter", 38, 14), ("side", 85, 6), ("back", 180, 8)]
    tiles = []
    for name, yaw, pitch in shots:
        c = Canvas(W, H, ((0.10, 0.13, 0.12), (0.22, 0.25, 0.22)))
        a = math.radians(yaw); p = math.radians(pitch)
        eye = (4.4 * math.sin(a) * math.cos(p), 4.4 * math.sin(p), 4.4 * math.cos(a) * math.cos(p))
        cam = look(eye, (0, 0, 0))
        cr = (cam[2], cam[3])
        prims = compass_prims(parts, np.eye(4, dtype=np.float32), 28, t=10, cam_right=cr, eye=eye)
        c.draw(prims, cam)
        tiles.append(label(c.finish(), name))
    sheet = Image.new("RGB", (W * 2, H * 2))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 2) * W, (i // 2) * H))
    return sheet


def caster_scene(parts):
    shots = [("front, target to the left", 18, 0, 20), ("beside the caster", -55, 8, 140), ("behind", 170, 10, -40)]
    tiles = []
    for name, cam_yaw, pitch, needle in shots:
        c = Canvas(W, H, ((0.16, 0.2, 0.26), (0.34, 0.42, 0.34)))
        yaw = 0.0
        right = np.array([-1, 0, 0], np.float32)                  # the caster's right (facing +z)
        pos = np.array([0, 1.55, 0], np.float32) + right * 1.25 + np.array([0, 0, -0.35], np.float32)
        m = T(*pos) @ RY(-25) @ S(0.62)                           # faces forward and outward (to the caster's right)
        a = math.radians(cam_yaw); p = math.radians(pitch)
        eye = (5.6 * math.sin(a) * math.cos(p), 1.7 + 5.6 * math.sin(p), 5.6 * math.cos(a) * math.cos(p))
        cam = look(eye, (-0.5, 1.4, 0), 34)
        # ground plane
        g = box_prim((0, -0.05, 0), (4, 0.05, 4), (0.26, 0.32, 0.24))
        prims = [g] + caster_prims(yaw) + compass_prims(parts, m, needle, t=12, cam_right=(cam[2], cam[3]), eye=eye)
        c.draw(prims, cam)
        tiles.append(label(c.finish(), name))
    sheet = Image.new("RGB", (W * len(tiles), H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    return sheet


def spells_scene(parts):
    tiles = []
    # 1: the arrow-needle, three quarter, with a glow trail
    c = Canvas(W, H, ((0.10, 0.12, 0.16), (0.24, 0.26, 0.28)))
    cam = look((2.6, 1.2, 2.4), (0, 0, 0.1), 30)
    ps = part_prims(parts, "arrow", T(0, 0, -0.1) @ RY(-30) @ S(0.9))
    ps.append(card((0, 0, 0), (0.0, 0.0, 0.0), (0.0, 0.0, 0.0), "glint", (1, 1, 1), 0))
    c.draw(ps, cam)
    tiles.append(label(c.finish(), "arrow-needle (Useless North)"))
    # 2: the route: bars and chevrons from the compass to a target
    c = Canvas(W, H, ((0.10, 0.12, 0.16), (0.24, 0.26, 0.28)))
    cam = look((1.6, 2.4, 5.2), (0, 0.8, 0), 34)
    ps = [box_prim((0, -0.05, 0), (3, 0.05, 3), (0.26, 0.32, 0.24))]
    start = np.array([-2.0, 1.4, 0.0], np.float32); end = np.array([1.6, 1.0, -1.2], np.float32)
    dvec = end - start; L = float(np.linalg.norm(dvec)); d = dvec / L
    yaw = math.degrees(math.atan2(d[0], d[2])); pitch = -math.degrees(math.asin(d[1]))
    n = 7
    for i in range(n):
        p0 = start + d * (L * i / n)
        ps += part_prims(parts, "bar", T(*p0) @ RY(yaw) @ RX(pitch) @ S(0.05, 0.05, L / n * 0.55))
        if i % 2 == 0:
            ps += part_prims(parts, "chevron", T(*(p0 + d * (L / n * 0.6))) @ RY(yaw) @ RX(pitch) @ S(0.16, 0.16, 0.22))
    ps += part_prims(parts, "arrow", T(*(start + d * 1.0)) @ RY(yaw) @ RX(pitch) @ S(0.45))
    c.draw(ps, cam)
    tiles.append(label(c.finish(), "route + needle (Another Atlas)"))
    # 3: the seal disc lying flat (Compass Rose): a mini compass, flat, with the ground ring
    c = Canvas(W, H, ((0.10, 0.12, 0.10), (0.24, 0.30, 0.22)))
    cam = look((2.4, 2.2, 3.0), (0, 0.1, 0), 32)
    m = T(0, 0.12, 0) @ RX(-90) @ S(0.6)
    ps = [box_prim((0, -0.05, 0), (3, 0.05, 3), (0.26, 0.32, 0.24))]
    ps += part_prims(parts, "rim", m) + part_prims(parts, "face", m) + part_prims(parts, "star", m) + part_prims(parts, "needle", m @ RZ(35))
    ps.append(card((0, 0.02, 0), (1.1, 0, 0), (0, 0, 1.1), "seal_ring", (1, 1, 1), 0.9))
    ps += part_prims(parts, "glass", m, mode="alpha")
    c.draw(ps, cam)
    tiles.append(label(c.finish(), "seal disc (Compass Rose)"))
    sheet = Image.new("RGB", (W * len(tiles), H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    return sheet


def main():
    _flat_tex()
    parts = build_dump()
    print({k: len(v) for k, v in parts.items()}, "quads")
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("compass_views", views), ("caster", caster_scene), ("spells", spells_scene)):
        img = fn(parts)
        img.save(os.path.join(OUT, name + ".png"), optimize=True)
        print("wrote", os.path.join(OUT, name + ".png"))


if __name__ == "__main__":
    sys.exit(main())
