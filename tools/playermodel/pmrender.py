"""Why: nothing of the player-model kit can be run in a game here, so this is a small software renderer of exactly what the kit draws: the vanilla player model (wide or slim arms, the 64x64
skin with its jacket, sleeve, pants and hat layers), the hair styles of assets/nusmp/playermodel/hair_styles.json (scalp cap, static boxes and the chains of segments, with the same pivots,
rotations and UV layout the Java hair drawer uses) and GeoLite extras (geo.json models with their bone tree, anchored on a model part). It uses the vanilla cube geometry (the corner order and
the UV assignment of ModelPart.Cube, copied from client/geo/GeoDraw), an orthographic camera and a depth buffer, alpha-tested textures and a simple three-light shading. It is a re-implementation
for review, not the game: colours and lighting differ from Minecraft's.

Frame: model pixels, y DOWN, the entity faces -z, +x is the entity's left. Rotations are vanilla's (ZYX: x first, then y, then z).
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")


# ---------------------------------------------------------------- matrices (4x4, applied to column vectors)
def mat_identity():
    return np.eye(4)


def mat_translate(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def mat_scale(x, y=None, z=None):
    m = np.eye(4)
    m[0, 0], m[1, 1], m[2, 2] = x, x if y is None else y, x if z is None else z
    return m


def mat_rot_zyx(rx, ry, rz):
    """Vanilla ModelPart rotation: rotationZYX(z, y, x) in radians = Rz * Ry * Rx."""
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]], float)
    Ry = np.array([[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]], float)
    Rz = np.array([[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], float)
    return Rz @ Ry @ Rx


def part_matrix(px, py, pz, rx=0.0, ry=0.0, rz=0.0):
    """ModelPart.translateAndRotate: move to the pivot, then rotate (radians)."""
    return mat_translate(px, py, pz) @ mat_rot_zyx(rx, ry, rz)


# ---------------------------------------------------------------- the cube
FACE_CORNERS = {"west": (0, 4, 6, 2), "north": (1, 0, 2, 3), "east": (5, 1, 3, 7), "south": (4, 5, 7, 6), "top": (5, 4, 0, 1), "bottom": (3, 2, 6, 7)}
FACE_NORMAL = {"west": (-1, 0, 0), "north": (0, 0, -1), "east": (1, 0, 0), "south": (0, 0, 1), "top": (0, -1, 0), "bottom": (0, 1, 0)}


def box_regions(u, v, w, h, d):
    """The vanilla layout: west / north(front) / east / south(back) in a row below the top and bottom."""
    return {"west": (u, v + d, d, h), "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h),
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d)}


class Face:
    __slots__ = ("pts", "uv", "normal", "tex", "tint", "emissive", "mode")

    def __init__(self, pts, uv, normal, tex, tint, emissive, mode):
        self.pts, self.uv, self.normal, self.tex, self.tint, self.emissive, self.mode = pts, uv, normal, tex, tint, emissive, mode


def cube_faces(m, frm, size, inflate, uv_origin, uv_size, tex, tint=(1, 1, 1), mirror=False, emissive=False, mode="cutout", face_uv=None):
    """Faces of a box under the 4x4 matrix `m`. `frm` is the min corner, `size` the extent, y down. uv_origin / uv_size give the vanilla box-UV layout, or `face_uv` per face
    {name: (u1, v1, u2, v2)} in texels (the geo.json per-face form)."""
    x0, y0, z0 = frm[0] - inflate, frm[1] - inflate, frm[2] - inflate
    x1, y1, z1 = frm[0] + size[0] + inflate, frm[1] + size[1] + inflate, frm[2] + size[2] + inflate
    if mirror:
        x0, x1 = x1, x0
    corners = []
    for i in range(8):
        corners.append((x1 if i & 1 else x0, y1 if i & 2 else y0, z1 if i & 4 else z0))
    th, tw = tex.shape[0], tex.shape[1]
    out = []
    regs = None if face_uv else box_regions(uv_origin[0], uv_origin[1], uv_size[0], uv_size[1], uv_size[2])
    for name, idx in FACE_CORNERS.items():
        if face_uv:
            r = face_uv.get(name)
            if r is None:
                continue
            u1, v1, u2, v2 = r
        else:
            rx, ry, rw, rh = regs[name]
            u1, v1, u2, v2 = rx, ry, rx + rw, ry + rh
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        order = list(range(4))
        if mirror:
            order = [3 - k for k in range(4)]
        pts = []
        tuv = []
        for k in range(4):
            c = corners[idx[order[k]]]
            p = m @ np.array([c[0], c[1], c[2], 1.0])
            pts.append(p[:3])
            tuv.append((uvs[order[k]][0] / tw, uvs[order[k]][1] / th))
        n = m[:3, :3] @ np.array(FACE_NORMAL[name], float)
        n = n * (-1.0 if mirror and name in ("west", "east") else 1.0)
        out.append(Face(pts, tuv, n / (np.linalg.norm(n) or 1.0), tex, tint, emissive, mode))
    return out


# ---------------------------------------------------------------- the scene
def load_tex(path):
    return np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32) / 255.0


class Scene:
    def __init__(self):
        self.faces = []

    def add(self, faces):
        self.faces.extend(faces)


def render(scene, size=(480, 640), yaw=0.0, pitch=8.0, center=(0, 12, 0), scale=11.0, bg=(0.78, 0.82, 0.86), floor=True, light=(-0.35, -0.55, -0.75)):
    """Orthographic render. yaw 0 = viewed from the front (the camera on the -z side), positive yaw walks the camera round to the entity's LEFT."""
    W, H = size
    img = np.zeros((H, W, 3), np.float32)
    img[:] = bg
    depth = np.full((H, W), 1e9, np.float32)
    cy, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    L = np.array(light, float)
    L /= np.linalg.norm(L)

    def project(p):
        x, y, z = p[0] - center[0], p[1] - center[1], p[2] - center[2]
        x2 = x * cy + z * sy_
        z2 = -x * sy_ + z * cy
        y3 = y * cp + z2 * sp
        z3 = -y * sp + z2 * cp
        return (W / 2 + x2 * scale, H * 0.52 + y3 * scale, z3)

    def rotn(n):
        x, y, z = n
        x2 = x * cy + z * sy_
        z2 = -x * sy_ + z * cy
        return np.array([x2, y * cp + z2 * sp, -y * sp + z2 * cp])

    if floor:                                         # a soft ground shadow ellipse under the feet
        gy = int(H * 0.52 + (24 - center[1]) * cp * scale)
        for yy in range(max(0, gy - 10), min(H, gy + 10)):
            for xx in range(max(0, W // 2 - 60), min(W, W // 2 + 60)):
                d = ((xx - W / 2) / 60.0) ** 2 + ((yy - gy) / 10.0) ** 2
                if d < 1.0:
                    img[yy, xx] *= 1.0 - 0.18 * (1.0 - d)
    modes = {"cutout": 0, "translucent": 1, "additive": 2}
    tris = []
    for f in scene.faces:
        P = [project(p) for p in f.pts]
        n = rotn(f.normal)
        if n[2] >= 0.0:                                # the camera looks along +z: a face turned away is culled
            continue
        tris.append((P, f, n))
    tris.sort(key=lambda t: modes.get(t[1].mode, 0))
    for P, f, n in tris:
        shade = 1.0 if f.emissive else 0.52 + 0.48 * max(0.0, float(np.dot(n, L)))
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = P[tri[0]], P[tri[1]], P[tri[2]]
            minx = max(0, int(math.floor(min(a[0], b[0], c[0]))))
            maxx = min(W - 1, int(math.ceil(max(a[0], b[0], c[0]))))
            miny = max(0, int(math.floor(min(a[1], b[1], c[1]))))
            maxy = min(H - 1, int(math.ceil(max(a[1], b[1], c[1]))))
            if maxx < minx or maxy < miny:
                continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            xs = np.arange(minx, maxx + 1) + 0.5
            ys = np.arange(miny, maxy + 1) + 0.5
            gx, gy2 = np.meshgrid(xs, ys)
            l1 = ((b[1] - c[1]) * (gx - c[0]) + (c[0] - b[0]) * (gy2 - c[1])) / den
            l2 = ((c[1] - a[1]) * (gx - c[0]) + (a[0] - c[0]) * (gy2 - c[1])) / den
            l3 = 1.0 - l1 - l2
            inside = (l1 >= -1e-4) & (l2 >= -1e-4) & (l3 >= -1e-4)
            if not inside.any():
                continue
            z = l1 * a[2] + l2 * b[2] + l3 * c[2]
            u = l1 * f.uv[tri[0]][0] + l2 * f.uv[tri[1]][0] + l3 * f.uv[tri[2]][0]
            v = l1 * f.uv[tri[0]][1] + l2 * f.uv[tri[1]][1] + l3 * f.uv[tri[2]][1]
            th, tw = f.tex.shape[0], f.tex.shape[1]
            tx = np.clip((u * tw).astype(int), 0, tw - 1)
            ty = np.clip((v * th).astype(int), 0, th - 1)
            texel = f.tex[ty, tx]
            alpha = texel[..., 3]
            dview = depth[miny:maxy + 1, minx:maxx + 1]
            ok = inside & (alpha > 0.1) & (z < dview + 1e-4)
            if not ok.any():
                continue
            col = texel[..., :3] * np.array(f.tint, np.float32) * shade
            region = img[miny:maxy + 1, minx:maxx + 1]
            if f.mode == "additive":
                region[ok] = np.clip(region[ok] + col[ok] * alpha[ok][:, None], 0, 1)
            elif f.mode == "translucent":
                region[ok] = region[ok] * (1 - alpha[ok][:, None]) + col[ok] * alpha[ok][:, None]
                dview[ok] = z[ok]
            else:
                region[ok] = col[ok]
                dview[ok] = z[ok]
    out = (np.clip(img, 0, 1) * 255 + 0.5).astype(np.uint8)
    return Image.fromarray(out, "RGB")


# ---------------------------------------------------------------- the player model
def player_faces(skin_tex, slim=False, pose=None, tint=(1, 1, 1), overlays=True, alpha_mode="cutout"):
    """The vanilla player model as faces. `pose` = {part: (rx, ry, rz)} radians for head, body, arm_r, arm_l, leg_r, leg_l."""
    pose = pose or {}
    faces = []

    def pm(name, px, py, pz):
        rx, ry, rz = pose.get(name, (0.0, 0.0, 0.0))
        return part_matrix(px, py, pz, rx, ry, rz)

    aw = 3 if slim else 4
    ay = 2.5 if slim else 2.0
    parts = [
        ("head", 0, 0, 0, (-4, -8, -4), (8, 8, 8), (0, 0), 0.0, (32, 0), 0.5),
        ("body", 0, 0, 0, (-4, 0, -2), (8, 12, 4), (16, 16), 0.0, (16, 32), 0.25),
        ("arm_r", -5, ay, 0, (-2 if slim else -3, -2, -2), (aw, 12, 4), (40, 16), 0.0, (40, 32), 0.25),
        ("arm_l", 5, ay, 0, (-1, -2, -2), (aw, 12, 4), (32, 48), 0.0, (48, 48), 0.25),
        ("leg_r", -1.9, 12, 0, (-2, 0, -2), (4, 12, 4), (0, 16), 0.0, (0, 32), 0.25),
        ("leg_l", 1.9, 12, 0, (-2, 0, -2), (4, 12, 4), (16, 48), 0.0, (0, 48), 0.25),
    ]
    for name, px, py, pz, frm, size, uv, infl, ouv, oinfl in parts:
        m = pm(name, px, py, pz)
        faces += cube_faces(m, frm, size, infl, uv, size, skin_tex, tint, mode=alpha_mode)
        if overlays:
            faces += cube_faces(m, frm, size, oinfl, ouv, size, skin_tex, tint, mode="cutout")
    return faces


def part_pose_matrix(name, slim=False, pose=None):
    """The matrix of a model part's pose (what part.translateAndRotate applies) for anchoring hair and extras."""
    pose = pose or {}
    ay = 2.5 if slim else 2.0
    piv = {"head": (0, 0, 0), "body": (0, 0, 0), "arm_r": (-5, ay, 0), "arm_l": (5, ay, 0), "leg_r": (-1.9, 12, 0), "leg_l": (1.9, 12, 0)}[name]
    rx, ry, rz = pose.get(name, (0.0, 0.0, 0.0))
    return part_matrix(piv[0], piv[1], piv[2], rx, ry, rz), piv


# ---------------------------------------------------------------- hair
HAIR_JSON = os.path.join(ASSETS, "playermodel", "hair_styles.json")
_hair_cache = {}


def hair_doc():
    if "doc" not in _hair_cache:
        _hair_cache["doc"] = json.load(open(HAIR_JSON))
    return _hair_cache["doc"]


def tone_color(rgb, tone):
    """The Java HairPalette, in python: base, highlight, shade as 0..1 triples."""
    import colorsys
    r, g, b = [c / 255.0 for c in rgb]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    lift = 0.10 + 0.12 * (1 - l)
    drop = 0.10 + 0.08 * l
    hi = colorsys.hls_to_rgb((h - 0.012) % 1.0, min(0.97, l + lift), min(1.0, s * 1.02))
    sh = colorsys.hls_to_rgb((h + 0.018) % 1.0, max(0.03, l - drop), min(1.0, s * 0.96))
    base = (r, g, b)
    if tone == 1:
        return hi
    if tone == -1:
        return sh
    if tone == 2:
        return tuple(c * 0.82 + 0.18 for c in hi)
    if tone == -2:
        return tuple(c * 0.78 for c in sh)
    return base


def hair_faces(style_id, rgb, slim=False, pose=None, chain_angles=None, helmet=False):
    """The faces of a hair style. chain_angles = {chain name: [(pitch, roll) per segment]} in radians (default: at rest)."""
    doc = hair_doc()
    st = doc["styles"][style_id]
    tex = load_tex(os.path.join(ASSETS, "textures", "entity", "playermodel", st["texture"] + ".png"))
    faces = []
    pose = pose or {}
    for b in st["boxes"]:
        if helmet and b.get("helm", "hide") == "hide":
            continue
        m, _ = part_pose_matrix(b["part"], slim, pose)
        local = np.eye(4)
        if "rot" in b:
            pv = b["pivot"]
            r = [math.radians(a) for a in b["rot"]]
            local = mat_translate(*pv) @ mat_rot_zyx(*r) @ mat_translate(-pv[0], -pv[1], -pv[2])
        uvs = b.get("uvsize", b["size"])
        faces += cube_faces(m @ local, b["from"], b["size"], b["inflate"], b["uv"], [int(round(x)) for x in uvs], tex, tone_color(rgb, b["tone"]))
    for c in st["chains"]:
        ang = (chain_angles or {}).get(c["name"], [])
        root = c["root"]
        y_nom = 0.0
        prev_part = None
        cur = None
        for i, sg in enumerate(c["segments"]):
            part = sg["part"]
            pm_, piv = part_pose_matrix(part, slim, pose)
            if helmet and c["helm"] == "tail" and root[1] + y_nom + sg["size"][1] < 1.2:
                y_nom += sg["size"][1]
                prev_part = None
                continue
            pitch, roll = ang[i] if i < len(ang) else (0.0, 0.0)
            # the segment hangs from (root.x, root.y + y_nom, root.z) in the part's space, rotated about that point (nested within a run of segments of one part)
            piv_pt = (root[0], root[1] + y_nom, root[2])
            if prev_part != part or cur is None:
                cur = pm_
            cur = cur @ mat_translate(*piv_pt) @ mat_rot_zyx(pitch, 0.0, roll) @ mat_translate(-piv_pt[0], -piv_pt[1], -piv_pt[2])
            w, ln, d = sg["size"]
            frm = (piv_pt[0] - w / 2.0, piv_pt[1], piv_pt[2] - d / 2.0)
            uvs = sg.get("uvsize", sg["size"])
            faces += cube_faces(cur, frm, sg["size"], sg["inflate"], sg["uv"], [int(round(x)) for x in uvs], tex, tone_color(rgb, sg["tone"]))
            y_nom += ln
            prev_part = part
    return faces


def flat_skin(rgb=(232, 190, 160), shirt=(70, 90, 140), pants=(50, 50, 60)):
    """A plain 64x64 skin for hair previews: skin colour, a face, a shirt and trousers."""
    a = np.zeros((64, 64, 4), np.float32)
    a[..., 3] = 1.0
    sk = np.array(rgb, np.float32) / 255.0
    a[0:16, 0:32, :3] = sk
    a[16:32, 16:40, :3] = np.array(shirt, np.float32) / 255.0
    a[16:32, 40:56, :3] = np.array(shirt, np.float32) / 255.0
    a[48:64, 32:48, :3] = np.array(shirt, np.float32) / 255.0
    a[16:32, 0:16, :3] = np.array(pants, np.float32) / 255.0
    a[48:64, 16:32, :3] = np.array(pants, np.float32) / 255.0
    # the face: eyes and mouth on the front of the head (8..15, 8..15)
    for x in (9, 10):
        a[11, x, :3] = (1, 1, 1)
        a[12, x, :3] = (0.1, 0.1, 0.2)
    for x in (13, 14):
        a[11, x, :3] = (1, 1, 1)
        a[12, x, :3] = (0.1, 0.1, 0.2)
    a[14, 11:13, :3] = (0.6, 0.3, 0.3)
    overlay = np.zeros_like(a)
    a2 = a.copy()
    a2[32:, :, 3] = 0.0           # overlays transparent
    a2[0:16, 32:64, 3] = 0.0
    a2[16:32, 16:56, 3] = a[16:32, 16:56, 3]
    return a2
