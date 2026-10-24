#!/usr/bin/env python3
"""
VALKYRIE ARMOUR = PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

A software render (numpy + Pillow) of the player-fitted Valkyrie armour of tools/gen_valkyrie_armor_meshes.py on the same block-figure stand-in the transformation-armour preview
(tools/mode_preview/preview.py) uses, posed like the player model (head yaw / pitch, arms, legs, sneaking lean, hover) with the flutter of the wings.

    python tools/gen_valkyrie_armor_meshes.py
    python tools/vfx_preview/valkyrie_preview.py [ref|turn|vs|flutter|fit|lod|pieces ...]       (no argument: everything)

Output (docs/previews/vfx_remake/valkyrie/v1/): valkyrie_ref_crop.png, valkyrie_wings_front_back_side.png, valkyrie_vs_ref.png, valkyrie_flutter.png, valkyrie_player_fit.png, valkyrie_lod.png,
valkyrie_pieces.png.

How a frame is put together (what the layer would do): opaque plate pieces and the stand-in z-buffered and toon shaded per quad (3 tones from the quad normal against a light from the upper left of the
camera, one small specular, a pale rim at the grazing edge), the translucent wings painter-sorted far to near with a fresnel alpha and no depth write, then the emissive layer (glow outlines, veins) additive
and depth tested against the opaque parts only (so the veins show through the glass).  Honest limits: software render, no bloom, no world light or lightmap, additive drawn as plain addition, bubbles and
the background are flat 2D drawing, nothing was seen in a game, GPU cost not measured.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
sys.path.insert(0, TOOLS)
import gen_valkyrie_armor_meshes as gv                                       # noqa: E402

OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie", "v1")        # v1, kept; the redo is valkyrie_v2_preview.py
REF = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie", "ref", "valkyrie_ref_130.png")      # the owner's reference (identical to Valkyrie_Dress.webp of the elemzip Water pack)
FOV = 34.0

TONES = {"plate": [(70, 138, 204), (136, 210, 243), (232, 250, 255)],
         "glass": [(70, 168, 236), (128, 214, 250), (226, 249, 255)],
         "skin": [(176, 124, 96), (226, 170, 138), (250, 214, 186)],
         "suit": [(18, 22, 44), (30, 38, 70), (52, 62, 104)],
         "hair": [(150, 158, 184), (196, 204, 226), (240, 244, 252)],
         "dark": [(10, 10, 18), (16, 16, 26), (30, 30, 44)]}
RIM = np.array([205, 246, 255], np.float32) / 255
GLOW = np.array([190, 255, 255], np.float32) / 255


def v3(*a): return np.array(a, np.float64)


# ================================================================================================ pose / skeleton
def rx(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], float)


def ry(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], float)


def rz(a):
    c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], float)


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def about(p, m):
    return tr(*p) @ m @ tr(-p[0], -p[1], -p[2])


class Pose:
    def __init__(self, **kw):
        self.hover = 0.0          # whole figure up (px)
        self.lean = 0.0           # body pitch about the hips (sneaking: about 28)
        self.yaw = 0.0            # head
        self.pitch = 0.0
        self.arm_l = (0.0, 0.0)   # (forward degrees, outward degrees)
        self.arm_r = (0.0, 0.0)
        self.leg_l = 0.0
        self.leg_r = 0.0
        self.flap = 0.0           # wings: degrees open (+) / closed (-)
        self.bend = 0.0           # extra bend of the tips
        self.sweep = 0.0          # tips forward (+) / back (-)
        self.yaw_body = 0.0
        self.grim_bob = 0.0
        self.__dict__.update(kw)


def bones(p):
    root = rz(0)
    root = tr(0, p.hover, 0) @ about((0, 12, 0), rx(p.lean))
    B = {"body": root, "root": root}
    B["head"] = root @ about((0, 24, 0), ry(p.yaw) @ rx(p.pitch))
    for s, key, arm in ((1, "l", p.arm_l), (-1, "r", p.arm_r)):
        piv = (6 * s, 22, 0)
        B["arm_" + key] = root @ about(piv, rz(s * arm[1]) @ rx(-arm[0]))
    B["leg_l"] = tr(0, p.hover, 0) @ about((2, 12, 0), rx(p.leg_l))
    B["leg_r"] = tr(0, p.hover, 0) @ about((-2, 12, 0), rx(p.leg_r))
    return B


def apply(M, pts):
    return (np.c_[pts, np.ones(len(pts))] @ M.T)[:, :3]


def mirror(pts):
    q = pts.copy()
    q[:, 0] *= -1
    return q


def wing_bend(pts, hinge, length, s, p, B):
    """per-vertex flutter about the hinge: weight w = distance from the hinge / wing length; the whole wing opens by `flap`, the tips by an extra `bend` and forward / back by `sweep`."""
    d = np.linalg.norm(pts - hinge, axis=1) / length
    w = np.clip(d, 0, 1)
    out = np.empty_like(pts)
    for i in range(len(pts)):
        ang = p.flap + p.bend * w[i] ** 1.4
        sw = p.sweep * w[i]
        M = about(tuple(hinge), rz(-s * ang) @ rx(sw))
        out[i] = apply(M, pts[i:i + 1])[0]
    return apply(B["root"], out)


# ================================================================================================ geometry containers
class Quads:
    def __init__(self):
        self.P = []     # (n, 4, 3)
        self.L = []     # layer
        self.T = []     # tint
        self.K = []     # kind
        self.A = []     # base alpha

    def add(self, pts, layer, tint, kind, alpha):
        self.P.append(pts)
        self.L.append(layer)
        self.T.append(tint)
        self.K.append(kind)
        self.A.append(alpha)

    def __len__(self): return len(self.P)


def mesh_to_quads(out, mesh, kind, alpha, fn):
    """fn(points (4n,3)) -> world points"""
    allp = np.array([[v[:3] for v in qd[1]] for qd in mesh.q], float).reshape(-1, 3)
    if len(allp) == 0:
        return
    wp = fn(allp).reshape(-1, 4, 3)
    for k, qd in enumerate(mesh.q):
        out.add(wp[k], qd[0], np.array(qd[3], np.float32) / 255, kind, alpha)


def box_quads(out, mn, mx, kind, M, tint=(255, 255, 255), pal=None):
    x0, y0, z0 = mn
    x1, y1, z1 = mx
    pts = {}
    for i, (x, y, z) in enumerate([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]):
        pts[i] = (x, y, z)
    faces = [(0, 1, 2, 3), (4, 5, 6, 7), (0, 3, 7, 4), (1, 2, 6, 5), (3, 2, 6, 7), (0, 1, 5, 4)]
    for f in faces:
        p = apply(M, np.array([pts[i] for i in f], float))
        out.add(p, 0, np.array(tint, np.float32) / 255, kind, 1.0)


def figure(out, B, skin=True):
    """the block-figure stand-in of tools/mode_preview/preview.py (same boxes), with hair and eyes added so the head reads from every side."""
    box_quads(out, (-4, 12, -2), (4, 24, 2), "suit", B["body"])
    box_quads(out, (-4, 24, -4), (4, 32, 4), "skin", B["head"])
    box_quads(out, (-4.4, 29, -4.4), (4.4, 32.4, 4.4), "hair", B["head"])                      # hair cap
    box_quads(out, (-4.4, 25, -4.4), (4.4, 29, -3.0), "hair", B["head"])                       # back of the hair
    box_quads(out, (-4.4, 28.2, 4.0), (4.4, 29.6, 4.5), "hair", B["head"])                     # fringe
    for sx in (-1, 1):
        box_quads(out, (sx * 4.5 - 0.5, 20, -1.8), (sx * 4.5 + 0.5, 28, 1.8), "hair", B["head"])    # side locks (twin tails, short)
        box_quads(out, (sx * 2.4 - 0.6, 26.4, 4.02), (sx * 2.4 + 0.6, 27.6, 4.3), "dark", B["head"])      # eyes
    for s, key in ((1, "l"), (-1, "r")):
        x0, x1 = (4, 8) if s > 0 else (-8, -4)
        box_quads(out, (x0, 12, -2), (x1, 24, 2), "skin", B["arm_" + key])
        lx0, lx1 = (0, 4) if s > 0 else (-4, 0)
        box_quads(out, (lx0, 0, -2), (lx1, 12, 2), "suit", B["leg_" + key])


def armour(out, B, p, lod, grimoire=True, wings=True):
    meshes = gv.build_all()
    for name, fn, bone, copies, pas, alpha, opt in gv.PIECES:
        if opt and not grimoire:
            continue
        if name.startswith("wing") and not wings:
            continue
        m = meshes[(name, lod)]
        for c in range(copies):
            s = 1 if c == 0 else -1
            if bone in ("arm", "leg"):
                M = B["%s_%s" % (bone, "l" if s > 0 else "r")]
            elif bone == "head":
                M = B["head"]
            elif bone == "body":
                M = B["body"]
            else:
                M = None
            if M is not None:
                def f(pts, M=M, s=s):
                    return apply(M, pts if s > 0 else mirror(pts))
            else:
                H = np.array({"wing_hinge": (3.4, 21.6, -2.8), "wing_hinge_low": (2.2, 15.5, -3.0)}[bone], float)
                Ls = gv.WING_L["main" if bone == "wing_hinge" else "low"]

                def f(pts, s=s, H=H, Ls=Ls):
                    q = pts if s > 0 else mirror(pts)
                    h = H if s > 0 else np.array([-H[0], H[1], H[2]])
                    return wing_bend(q, h, Ls, s, p, B) if bone == "wing_hinge" else wing_bend(q, h, Ls, s, Pose(flap=-0.7 * p.flap, bend=-0.5 * p.bend, sweep=0.4 * p.sweep), B)
            mesh_to_quads(out, m, "glass" if pas == "glass" else "plate", alpha, f)
    return out


# ================================================================================================ rasteriser
def basis(eye, target):
    f = target - eye
    f = f / np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0])
    r = r / np.linalg.norm(r)
    u = np.cross(r, f)
    return f, r, u


def render(quads, eye, target, size, bg, ss=2, fov=FOV, exposure=1.0):
    Wd, Hd = size
    w, h = Wd * ss, Hd * ss
    f, r, u = basis(eye, target)
    k = (h / 2) / math.tan(math.radians(fov / 2))
    SUN = -0.55 * r + 0.8 * u * 0.9 - 0.3 * f + 0.25 * np.array([0, 1, 0])
    SUN = SUN / np.linalg.norm(SUN)
    img = np.asarray(bg.resize((w, h), Image.BILINEAR).convert("RGB"), np.float32) / 255
    img = img.copy()
    ZB = np.zeros((h, w), np.float32)              # inverse depth of the nearest opaque sample
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    recs = []
    for i in range(len(quads)):
        P = quads.P[i]
        n = np.cross(P[2] - P[0], P[3] - P[1])
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            n = np.cross(P[1] - P[0], P[3] - P[0])
            ln = np.linalg.norm(n)
            if ln < 1e-9:
                continue
        n = n / ln
        cen = P.mean(axis=0)
        Vv = eye - cen
        dist = np.linalg.norm(Vv)
        Vv = Vv / dist
        ndv = float(n @ Vv)
        if ndv < 0:
            n = -n
        ndv = abs(ndv)
        pc = P - eye
        z = pc @ f
        if np.any(z < 0.3):
            continue
        kind, layer = quads.K[i], quads.L[i]
        if layer == 1:
            col = GLOW * (0.55 + 0.45 * ndv)
            al = 0.95
        else:
            pal = TONES[kind]
            hl = 0.5 + 0.5 * float(n @ SUN)
            tone = 2 if hl > 0.86 else 1 if hl > 0.55 else 0
            tint = quads.T[i]
            base = np.array(pal[tone], np.float32) / 255
            if kind in ("plate", "glass"):
                col = base * (0.62 + 0.38 * tint)                   # the tint only warms / cools the tone (emissive floor: nothing goes dark)
                col = np.maximum(col, base * 0.78)
            else:
                col = base * tint
            Hh = SUN + Vv
            Hh = Hh / np.linalg.norm(Hh)
            if kind in ("plate", "glass") and float(n @ Hh) > 0.9985:
                col = col * 0.25 + 0.75
            elif ndv < 0.22 and kind in ("plate", "glass"):
                col = col * 0.55 + RIM * 0.45
            al = quads.A[i]
            if kind == "glass":
                al = float(np.clip(al * (0.68 + 0.75 * (1 - ndv) ** 1.1), 0.3, 0.92))
        recs.append((layer, kind, al, col * exposure, pc, z))

    def tris(pc, z):
        sx = w / 2 + (pc @ r) / z * k
        sy = h / 2 - (pc @ u) / z * k
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, math.floor(min(x0, x1, x2)))), int(min(w - 1, math.ceil(max(x0, x1, x2)))) + 1
            miny, maxy = int(max(0, math.floor(min(y0, y1, y2)))), int(min(h - 1, math.ceil(max(y0, y1, y2)))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            X = xx[miny:maxy, minx:maxx] + 0.5
            Y = yy[miny:maxy, minx:maxx] + 0.5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            msk = (a >= -2e-3) & (b >= -2e-3) & (c >= -2e-3)
            if not msk.any():
                continue
            iz = a / z[tri[0]] + b / z[tri[1]] + c / z[tri[2]]
            yield (slice(miny, maxy), slice(minx, maxx)), msk, iz.astype(np.float32)

    for layer, kind, al, col, pc, z in recs:                                           # opaque
        if layer == 1 or kind == "glass":
            continue
        for sl, msk, iz in tris(pc, z):
            ok = msk & (iz > ZB[sl])
            ZB[sl] = np.where(ok, iz, ZB[sl])
            img[sl] = np.where(ok[..., None], col[None, None, :], img[sl])
    glass = sorted([q for q in recs if q[1] == "glass" and q[0] != 1], key=lambda q: -float(np.mean(q[5])))
    for layer, kind, al, col, pc, z in glass:                                          # translucent, far to near
        for sl, msk, iz in tris(pc, z):
            ok = msk & (iz > ZB[sl])
            img[sl] = np.where(ok[..., None], img[sl] * (1 - al) + col[None, None, :] * al, img[sl])
    for layer, kind, al, col, pc, z in recs:                                           # emissive, additive
        if layer != 1:
            continue
        for sl, msk, iz in tris(pc, z):
            ok = msk & (iz > ZB[sl] * 0.995)
            img[sl] = np.where(ok[..., None], np.clip(img[sl] + col[None, None, :] * 0.85, 0, 1), img[sl])
    out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    out = out.resize((Wd, Hd), Image.LANCZOS)
    return out, (eye, f, r, u, k / ss, Wd, Hd)


def project(cam, p):
    eye, f, r, u, k, Wd, Hd = cam
    pc = np.asarray(p, float) - eye
    z = pc @ f
    return Wd / 2 + (pc @ r) / z * k, Hd / 2 - (pc @ u) / z * k, z


# ================================================================================================ backgrounds, bubbles
def bg_plain(size, top=(20, 40, 52), bot=(46, 86, 96)):
    Wd, Hd = size
    a = np.linspace(0, 1, Hd)[:, None, None]
    img = (np.array(top)[None, None, :] * (1 - a) + np.array(bot)[None, None, :] * a) * np.ones((Hd, Wd, 1))
    return Image.fromarray(img.astype(np.uint8))


def bg_ref(size, seed=4):
    """the underwater green of the anime still: horizontal light streaks over a green gradient, bright mint glow at the bottom."""
    Wd, Hd = size
    rng = np.random.default_rng(seed)
    a = np.linspace(0, 1, Hd)[:, None, None]
    img = (np.array([10, 118, 64])[None, None, :] * (1 - a) + np.array([34, 168, 98])[None, None, :] * a) * np.ones((Hd, Wd, 1))
    im = Image.fromarray(img.astype(np.uint8))
    lay = Image.new("RGB", (Wd, Hd), (0, 0, 0))
    d = ImageDraw.Draw(lay)
    for i in range(46):
        y = rng.uniform(0, Hd * 0.82)
        x = rng.uniform(-0.1 * Wd, 1.0 * Wd)
        ln = rng.uniform(0.18, 0.55) * Wd
        th = rng.uniform(0.004, 0.016) * Hd
        c = rng.uniform(0.4, 1.0)
        d.ellipse((x, y, x + ln, y + th), fill=(int(120 * c), int(210 * c), int(80 * c)))
    lay = lay.filter(ImageFilter.GaussianBlur(Hd * 0.004))
    im = Image.fromarray(np.clip(np.asarray(im, np.float32) + np.asarray(lay, np.float32) * 0.75, 0, 255).astype(np.uint8))
    glow = np.zeros((Hd, Wd, 3), np.float32)
    yy = np.linspace(0, 1, Hd)[:, None]
    g = np.clip((yy - 0.62) / 0.38, 0, 1) ** 1.6
    glow[..., 0] = g * 150
    glow[..., 1] = g * 235
    glow[..., 2] = g * 190
    im = Image.fromarray(np.clip(np.asarray(im, np.float32) + glow, 0, 255).astype(np.uint8))
    return im


def bubbles(img, cam, pts_sizes, seed=2):
    """flat 2D bubbles: a thin bright ring, a faint fill and a glint; pts_sizes = [(world point, radius in blocks)]."""
    d = ImageDraw.Draw(img, "RGBA")
    for p, rad in pts_sizes:
        x, y, z = project(cam, p)
        r = rad / z * cam[4] * 16 / 16
        r = max(2.0, r)
        d.ellipse((x - r, y - r, x + r, y + r), fill=(200, 255, 245, 30), outline=(235, 255, 250, 200), width=max(1, int(r * 0.12)))
        d.ellipse((x - r * 0.55, y - r * 0.6, x - r * 0.15, y - r * 0.2), fill=(255, 255, 255, 190))
    return img


def bubble_points(seed, n, centre, spread):
    rng = np.random.default_rng(seed)
    out = []
    for i in range(n):
        out.append((centre + rng.normal(0, 1, 3) * spread * np.array([1, 1.4, 0.6]), float(rng.uniform(0.012, 0.045))))
    return out


def foam(img, cam, pts, seed=1):
    """soft white water puffs under the legs, like the splash of the still (flat 2D, stand-in for the existing water FX)."""
    rng = np.random.default_rng(seed)
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    for p, rad in pts:
        x, y, z = project(cam, p)
        r = rad / z * cam[4]
        for j in range(16):
            ox, oy = rng.normal(0, r * 0.75), rng.normal(-r * 0.2, r * 0.5)
            rr = r * rng.uniform(0.18, 0.42)
            d.ellipse((x + ox - rr, y + oy - rr, x + ox + rr, y + oy + rr), fill=(240, 255, 252, 190))
    lay = lay.filter(ImageFilter.GaussianBlur(max(1, img.size[1] * 0.0025)))
    img.paste(lay, (0, 0), lay)
    return img


# ================================================================================================ scenes
FONT = None


def font(sz=13):
    try:
        return ImageFont.load_default(size=sz)
    except TypeError:
        return ImageFont.load_default()


def label(img, text, xy=(6, 4), sz=13, fill=(255, 255, 255), shadow=True):
    d = ImageDraw.Draw(img)
    f = font(sz)
    if shadow:
        d.text((xy[0] + 1, xy[1] + 1), text, fill=(0, 0, 0), font=f)
    d.text(xy, text, fill=fill, font=f)


def scene(pose, lod, eye, target, size, bg, ss=2, wings=True, grimoire=True, fig=True, arm=True):
    q = Quads()
    B = bones(pose)
    if fig:
        figure(q, B)
    if arm:
        armour(q, B, pose, lod, grimoire=grimoire, wings=wings)
    return render(q, eye, target, size, bg, ss=ss), q


POSES = {
    "stand": Pose(),
    "cast": Pose(arm_l=(0, 150), arm_r=(0, 150), pitch=-8, flap=6, bend=8),
    "ref": Pose(hover=3, lean=-5, arm_l=(0, 156), arm_r=(0, 150), pitch=-10, leg_l=14, leg_r=-16, flap=4, bend=6, yaw=6),
    "point": Pose(arm_r=(88, 4), arm_l=(0, 6)),
    "sneak": Pose(lean=26, pitch=-26, leg_l=-10, leg_r=14, arm_l=(-16, 4), arm_r=(10, 4)),
    "walk": Pose(arm_l=(-32, 3), arm_r=(32, 3), leg_l=30, leg_r=-30, flap=3, bend=4),
}


def orbit(angle, dist=3.6, height=1.5, look_y=1.05):
    a = math.radians(angle)
    return v3(math.sin(a) * dist, height, math.cos(a) * dist), v3(0, look_y, 0)


def sc(eye, target):
    return eye, target


def px(v):
    return v / 16.0


def world(q):
    """the quads are in px; rescale to blocks for the camera."""
    out = Quads()
    out.P = [p / 16.0 for p in q.P]
    out.L, out.T, out.K, out.A = q.L, q.T, q.K, q.A
    return out


def scene_b(pose, lod, eye, target, size, bg, ss=2, **kw):
    q = Quads()
    B = bones(pose)
    if kw.get("fig", True):
        figure(q, B)
    if kw.get("arm", True):
        armour(q, B, pose, lod, grimoire=kw.get("grimoire", True), wings=kw.get("wings", True))
    img, cam = render(world(q), eye, target, size, bg, ss=ss)
    return img, cam, q


def count_by(q):
    return len(q)


def turnaround():
    tiles = []
    names = ["front", "front-left 45", "left side", "back-left 135", "back", "back-right 225", "right side", "front-right 315"]
    pose = POSES["stand"]
    pose = Pose(flap=2, bend=3)
    for i, name in enumerate(names):
        eye, tgt = orbit(i * 45, dist=7.2, height=1.6, look_y=1.55)
        img, cam, q = scene_b(pose, 0, eye, tgt, (300, 440), bg_plain((300, 440)), ss=2)
        label(img, name)
        tiles.append(img)
    sheet = Image.new("RGB", (300 * 4, 440 * 2), (10, 14, 18))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 4) * 300, (i // 4) * 440))
    path = os.path.join(OUT, "valkyrie_wings_front_back_side.png")
    sheet.save(path)
    return path


def fit_sheet():
    """player-fitted previews: standing and casting (arms raised), each front / three-quarter / side / back; plus a pointing arm, sneaking and walking."""
    rows = [("standing", POSES["stand"]), ("casting, arms raised", POSES["cast"]), ("one arm raised (lance pose)", POSES["point"]), ("sneaking", POSES["sneak"]), ("walking", POSES["walk"])]
    views = [("front", 0), ("three-quarter", 40), ("side", 90), ("back", 180)]
    TW, TH = 240, 340
    sheet = Image.new("RGB", (TW * len(views), TH * len(rows)), (10, 14, 18))
    for r, (rn, pose) in enumerate(rows):
        for c, (vn, ang) in enumerate(views):
            eye, tgt = orbit(ang, dist=7.2, height=1.6, look_y=1.5)
            img, cam, q = scene_b(pose, 0, eye, tgt, (TW, TH), bg_plain((TW, TH)), ss=2)
            label(img, "%s / %s" % (rn, vn), sz=12)
            sheet.paste(img, (c * TW, r * TH))
    path = os.path.join(OUT, "valkyrie_player_fit.png")
    sheet.save(path)
    return path


def flutter_sheet():
    keys = [("A  closed -14 / tips -18", Pose(flap=-14, bend=-18, sweep=5)), ("B  -6 / -8", Pose(flap=-6, bend=-8, sweep=2)), ("C  rest", Pose()),
            ("D  +8 / +10", Pose(flap=8, bend=10, sweep=-2)), ("E  open +16 / +24", Pose(flap=16, bend=24, sweep=-5))]
    TW, TH = 280, 400
    sheet = Image.new("RGB", (TW * len(keys), TH * 2), (10, 14, 18))
    for i, (n, p) in enumerate(keys):
        for j, (ang, nm) in enumerate(((180, "back"), (35, "3/4 front"))):
            eye, tgt = orbit(ang, dist=7.2, height=1.7, look_y=1.55)
            img, cam, q = scene_b(p, 0, eye, tgt, (TW, TH), bg_plain((TW, TH)), ss=2)
            label(img, "%s (%s)" % (n, nm), sz=12)
            sheet.paste(img, (i * TW, j * TH))
    path = os.path.join(OUT, "valkyrie_flutter.png")
    sheet.save(path)
    return path


def lod_sheet():
    TW, TH = 300, 440
    sheet = Image.new("RGB", (TW * 3, TH * 2), (10, 14, 18))
    info = []
    for lod, nm in enumerate(("NEAR", "MID", "FAR")):
        for j, ang in enumerate((35, 180)):
            eye, tgt = orbit(ang, dist=7.2, height=1.6, look_y=1.55)
            img, cam, q = scene_b(Pose(flap=2, bend=3), lod, eye, tgt, (TW, TH), bg_plain((TW, TH)), ss=2, fig=True)
            arm_q = sum(1 for k in q.K if k in ("plate", "glass"))
            label(img, "%s  %d armour quads" % (nm, arm_q), sz=12)
            sheet.paste(img, (lod * TW, j * TH))
    path = os.path.join(OUT, "valkyrie_lod.png")
    sheet.save(path)
    return path


def pieces_sheet():
    names = ["crown", "bodice", "skirt", "pauldron", "gauntlet", "greave", "wing_main", "wing_low", "grimoire"]
    TW, TH = 300, 300
    sheet = Image.new("RGB", (TW * 3, TH * 3), (10, 14, 18))
    ms = gv.build_all()
    for i, nm in enumerate(names):
        m = ms[(nm, 0)]
        q = Quads()
        kind = "glass" if nm.startswith("wing") else "plate"
        mesh_to_quads(q, m, kind, 0.8 if kind == "glass" else 1.0, lambda p: p)
        allp = np.concatenate(q.P)
        c = (allp.max(axis=0) + allp.min(axis=0)) / 2
        ext = float(np.max(allp.max(axis=0) - allp.min(axis=0)))
        eye = c + v3(0.5, 0.35, 1.0) / np.linalg.norm(v3(0.5, 0.35, 1.0)) * ext * 2.3
        w = Quads()
        w.P = [p / 16 for p in q.P]
        w.L, w.T, w.K, w.A = q.L, q.T, q.K, q.A
        img, cam = render(w, eye / 16, c / 16, (TW, TH), bg_plain((TW, TH)), ss=2)
        label(img, "%s  %d quads (near, left copy)" % (nm, len(m.q)), sz=12)
        sheet.paste(img, ((i % 3) * TW, (i // 3) * TH))
    path = os.path.join(OUT, "valkyrie_pieces.png")
    sheet.save(path)
    return path


# ---- the reference crop with notes
def ref_crop():
    ref = Image.open(REF).convert("RGB")
    S = 5
    big = ref.resize((ref.width * S, ref.height * S), Image.LANCZOS)
    pad = 330
    canvas = Image.new("RGB", (big.width + pad, big.height), (14, 18, 22))
    canvas.paste(big, (0, 0))
    d = ImageDraw.Draw(canvas)
    f = font(15)
    fs = font(13)
    red = (255, 90, 90)
    yel = (255, 226, 90)

    def note(xy, tx, text, col=yel):
        d.line([xy, tx], fill=col, width=2)
        d.ellipse((xy[0] - 4, xy[1] - 4, xy[0] + 4, xy[1] + 4), outline=col, width=2)
        return tx
    notes = [((300, 90), "1  viewer-left wing: a long narrow blade, ~30 deg from vertical, tip high and outward; pale cyan glass, brighter spine on the outer edge"),
             ((505, 100), "2  tiara: 5-7 tall thorns on a band, the middle ones tallest"),
             ((760, 130), "3  viewer-right wing: same blade, a little longer, widest near the shoulder (a sleeve-like fold), tapering to a needle tip"),
             ((520, 250), "4  bodice: short ruffle of overlapping frosty petals / feathers, white-aqua"),
             ((870, 330), "5  bubbles drift up everywhere (thin ring + glint)"),
             ((600, 340), "6  flat glowing grimoire / book at the hip"),
             ((520, 395), "7  short flared petal skirt over dark legs"),
             ((470, 480), "8  legs wrapped in spiky ice-water, foam and splashes below")]
    yb = 30
    for (a_, t) in notes:
        b_ = (1010, yb)
        d.line([a_, (b_[0] - 4, b_[1] + 8)], fill=yel, width=1)
        d.ellipse((a_[0] - 5, a_[1] - 5, a_[0] + 5, a_[1] + 5), outline=red, width=2)
        words, line, y = t.split(), "", b_[1]
        for wd in words:
            if d.textlength(line + " " + wd, font=fs) > pad - 22:
                d.text((b_[0], y), line, fill=(235, 245, 240), font=fs)
                y += 17
                line = wd
            else:
                line = (line + " " + wd).strip()
        d.text((b_[0], y), line, fill=(235, 245, 240), font=fs)
        yb = max(yb + 78, y + 40)
    d.text((8, 6), "reference: anime still of the Valkyrie Dress (=Valkyrie_Dress.webp), 200x144 upscaled x5", fill=(255, 255, 255), font=f)
    path = os.path.join(OUT, "valkyrie_ref_crop.png")
    canvas.save(path)
    return path


def vs_ref(tag="valkyrie_vs_ref.png", pose_name="ref"):
    ref = Image.open(REF).convert("RGB")
    size = (1000, 720)
    refbig = ref.resize(size, Image.LANCZOS)
    pose = POSES[pose_name]
    eye = v3(-1.9, 0.9, 5.0)
    tgt = v3(0.0, 1.6, 0.0)
    img, cam, q = scene_b(pose, 0, eye, tgt, size, bg_ref(size), ss=2)
    pts = bubble_points(7, 34, v3(0.0, 1.5, 0.0), 0.9)
    foam(img, cam, [(v3(-0.55, 0.15, 0.1), 0.42), (v3(0.45, 0.1, 0.15), 0.36), (v3(0.0, 0.0, 0.2), 0.5), (v3(-0.95, 0.35, -0.1), 0.3)])
    bubbles(img, cam, pts)
    sheet = Image.new("RGB", (size[0] * 2 + 6, size[1] + 24), (12, 12, 14))
    sheet.paste(refbig, (0, 24))
    sheet.paste(img, (size[0] + 6, 24))
    label(sheet, "REFERENCE (anime still, Valkyrie Dress)", (8, 3), 14)
    label(sheet, "NEW 3D VALKYRIE ARMOUR, same pose / camera idea (software render: toon + rim + emissive, flat 2D bubbles and foam)", (size[0] + 14, 3), 14)
    path = os.path.join(OUT, tag)
    sheet.save(path)
    return path


def main():
    os.makedirs(OUT, exist_ok=True)
    todo = sys.argv[1:] or ["ref", "turn", "vs", "flutter", "fit", "lod", "pieces"]
    fn = {"ref": ref_crop, "turn": turnaround, "vs": vs_ref, "flutter": flutter_sheet, "fit": fit_sheet, "lod": lod_sheet, "pieces": pieces_sheet}
    for t in todo:
        print("wrote", fn[t]())


if __name__ == "__main__":
    main()
