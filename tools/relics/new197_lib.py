"""The shared kit of the 35 weapons of 1.97.0 (wave 71): geometry builders, skin painters, the icon renderer, the item model writer and the finish step.

Everything is built with the owner's model method (tools/relics/mesh_lib.py): real cross-sections (loft / sweep / lathe), named parts, projected UVs, a painted
skin and a second additive glow skin, per-vertex smoothed normals. Units are centimetres (any unit works: `finish` scales the whole weapon to its class size).
Axis convention (docs/weapon_scale_orientation.md): y runs along the weapon from the butt (low y) to the tip or head, the cutting edge / head faces -x, the flat
faces +z. Void parts (layer 1) are drawn with the dimensional void of the Genesis Demon-Slayer.

Per weapon, `finish(...)`:
  1. places the mesh on a stand-in sprite diagonal (a temp file: the repo sprite is OUTPUT, so a rebuild is idempotent) and scales it to `real_m` apparent metres,
  2. writes weapon_meshes/<id>.txt, textures/entity/weapon/<id>.png and _glow.png,
  3. renders the 16 x 16 inventory sprite textures/item/<id>.png from the finished mesh (pixel art: outline, glow bloom),
  4. writes models/item/<id>.json (builtin/entity, the 3D hand model) and <id>_icon.json (the sprite).
"""
import json
import math
import os
import sys
import tempfile

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                                   # noqa: E402
import wave1_models as w                                                # noqa: E402
import preview_mesh as pm                                               # noqa: E402

ROOT = ml.ROOT
ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
ICON_DIR = os.path.join(ASSETS, "textures", "item")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
PREVIEW_DIR = os.environ.get("N197_PREVIEW", "")

# the apparent length (blocks) each weapon class is built to: the upper middle of the class range of assets/nusmp/weapon_classes.json
REAL_M = {"dagger": 0.60, "shortsword": 1.20, "longsword": 1.62, "rapier": 1.22, "katana": 1.62, "sabre": 1.22, "greatsword": 2.65, "handaxe": 1.25, "battleaxe": 1.85,
          "greataxe": 2.75, "scythe": 2.35, "spear": 3.2, "glaive": 3.2, "trident": 3.2, "staff": 2.3, "whip": 2.0, "crossbow": 1.1, "bow": 1.2, "hammer": 1.6,
          "wand": 1.0, "shield": 1.05, "club": 1.5}


# ================================================================ parts
def P(m, name, material="steel", **kw):
    """A new named part."""
    return w.add(m, name, material, **kw)


def plate(m, name, material, st, bevel=0.45, base=None, glow=False, smooth=45, wear=0.5):
    """A blade-like plate along y from stations (y, x_left, x_right, half_thickness): x_left is the cutting edge (-x), x_right the spine. A last station with
    equal x and zero thickness closes it to a point; a station with the same y twice makes a step (a notch)."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear)
    sts = [(y, (xr - xl) / 2.0, t, (xr + xl) / 2.0, 0.0) for (y, xl, xr, t) in st]
    ml.loft(p, "y", (0, 0, 0), sts, ml.bevelled(2.0, 2.0, bevel))
    return p


def blade_curve(m, name, material, pts, half_w, half_t, bevel=0.45, base=None, glow=False, smooth=45, wear=0.5, tip=True, up=(0, 0, 1)):
    """A curved blade swept along the centre line pts [(x, y)] (z = 0): half_w(t) and half_t(t) for t in 0..1; the end closes to a point when tip is set."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear)

    def sc(t):
        if tip and t > 0.985:
            return (0.0, 0.0)
        return (max(half_w(t), 0.02), max(half_t(t), 0.02))
    ml.sweep(p, [(x, y, 0.0) for (x, y) in pts], ml.bevelled(2.0, 2.0, bevel), scales=sc, up=up)
    return p


def rod(m, name, material, y0, y1, r0, r1=None, sides=10, base=None, glow=False, smooth=55, wear=0.5, squash=1.0, x=0.0, z=0.0, caps=(True, True), swell=None):
    """A round shaft along y from y0 to y1, radius r0 growing to r1; `swell` = [(t, radius factor)] for bulges."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear, uv_mode="loft")
    r1 = r0 if r1 is None else r1
    ts = [0.0, 1.0] if not swell else [0.0] + [t for t, _ in swell] + [1.0]
    sw = {0.0: 1.0, 1.0: 1.0}
    for t, f in (swell or []):
        sw[t] = f
    st = [(y0 + (y1 - y0) * t, (r0 + (r1 - r0) * t) * sw[t], (r0 + (r1 - r0) * t) * sw[t] * squash, x, z) for t in sorted(set(ts))]
    ml.loft(p, "y", (0, 0, 0), st, ml.circle(1.0, sides), caps=caps)
    return p


def lathe_y(m, name, material, y, profile, sides=14, base=None, glow=False, smooth=50, wear=0.5, squash=1.0, x=0.0, z=0.0, caps=(True, True)):
    """A surface of revolution about the y axis through (x, z): profile = [(dy, radius)] from y."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear, uv_mode="loft")
    ml.lathe(p, "y", (x, y, z), profile, sides=sides, caps=caps, squash=squash)
    return p


def orb(m, name, material, c, r, base=None, glow=True, rx=None, ry=None, rz=None, sides=12, stacks=8, smooth=60):
    """A faceted ellipsoid (a gem, a pearl, a crystal ball) centred on c."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, uv_mode="loft")
    w.ellipsoid(p, c, rx or r, ry or r, rz or r, sides=sides, stacks=stacks)
    return p


def crystal(m, name, material, c, r, length, base=None, glow=True, sides=6, axis="y", smooth=30):
    """A cut crystal / gem: two cones round a short girdle, long axis `axis` through c."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, uv_mode="loft")
    ml.gem(p, c, r, length / (2.0 * r) if r else 1.4, sides, axis)
    return p


def bar(m, name, material, p0, p1, w_, h_, up=(0, 0, 1), taper=0.0, base=None, glow=False, smooth=50, section="lozenge", end_scale=0.35):
    """A raised bar / rib / strut from p0 to p1: width w_, height h_ out of the surface along `up`."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth)
    ml.bar(p, p0, p1, w_, h_, up=up, taper=taper, section=section, end_scale=end_scale)
    return p


def tube(m, name, material, pts, r0, r1=None, sides=8, base=None, glow=False, smooth=60, tip=False, thick=1.0, wear=0.5):
    """A round tube along a polyline [(x, y, z)], radius r0 to r1 (0 or tip = a point)."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, uv_mode="loft", wear=wear)
    r1 = r0 if r1 is None else r1

    def sc(t):
        if tip and t > 0.97:
            return (0.0, 0.0)
        r = r0 + (r1 - r0) * t
        return (r, r * thick)
    ml.sweep(p, pts, ml.circle(1.0, sides), scales=sc)
    return p


def wrap(m, name, y0, y1, r, n, material="leather", base=None, sides=10, glow=False):
    """Binding rings along a grip (flat bands)."""
    p = P(m, name, material, base=base, wear=0.3, smooth=60, glow=glow)
    for i in range(n):
        ml.collar(p, y0 + 0.4 + (y1 - y0 - 1.2) * i / max(1, n - 1), r * 1.08, 0.55, lip=0.16, sides=sides, caps=(False, False))
    return p


def studs(m, name, material, pts, r, h, normal=(0, 0, 1), base=None, glow=False, sides=8, dome=0.55):
    p = P(m, name, material, base=base, glow=glow, smooth=50)
    for pt in pts:
        ml.stud(p, pt, r, h, normal, sides, dome)
    return p


def spike(m_part, base_pt, tip_pt, r, sides=5, up=None):
    """A cone from base_pt to tip_pt (added to an existing part)."""
    b, t = np.array(base_pt, float), np.array(tip_pt, float)
    L = float(np.linalg.norm(t - b))
    ml.loft(m_part, t - b, b, [(0.0, 1.0, 1.0), (L * 0.98, 0.0, 0.0)], ml.circle(r, sides), up=up)


def mirror_part(m, p, name, axis="x"):
    """A mirrored copy of a part (the other limb, the other wing)."""
    c = p.mirrored(axis, name=name)
    m.add(c)
    return c


def void_plate(m, name, st, lift=0.06):
    """A thin plate of the dimensional void lying on a blade face: stations (y, x_left, x_right, z) (z = the face height + a lift), drawn on both faces."""
    p = ml.Part(name, layer=1)
    for sgn in (1, -1):
        for a, b in zip(st[:-1], st[1:]):
            y0, xl0, xr0, z0 = a
            y1, xl1, xr1, z1 = b
            zz0, zz1 = sgn * (z0 + lift), sgn * (z1 + lift)
            quad = [(xl0, y0, zz0), (xr0, y0, zz0), (xr1, y1, zz1), (xl1, y1, zz1)]
            i = [p.add_vertex(q) for q in quad]
            if sgn > 0:
                p.add_face(i[0], i[1], i[2], i[3])
            else:
                p.add_face(i[0], i[3], i[2], i[1])
    m.add(p)
    return p


# ================================================================ outlines for painters
class Outline:
    """The left (edge, -x) and right (spine, +x) outline of a plate along y, for painters."""

    def __init__(self, st):
        self.y = np.array([s[0] for s in st], float)
        self.xl = np.array([s[1] for s in st], float)
        self.xr = np.array([s[2] for s in st], float)
        o = np.argsort(self.y, kind="stable")
        self.y, self.xl, self.xr = self.y[o], self.xl[o], self.xr[o]

    def l(self, Y):
        return np.interp(Y, self.y, self.xl)

    def r(self, Y):
        return np.interp(Y, self.y, self.xr)

    def c(self, Y):
        return (self.l(Y) + self.r(Y)) / 2

    def hw(self, Y):
        return np.maximum((self.r(Y) - self.l(Y)) / 2, 1e-4)

    def u(self, X, Y):
        """-1 at the edge, +1 at the spine, 0 on the centre line."""
        return (X - self.c(Y)) / self.hw(Y)


def sm(d, w_):
    """A soft mask: 1 at d = 0 falling to 0 at |d| = w_."""
    return np.clip(1.0 - np.abs(d) / max(w_, 1e-6), 0.0, 1.0)


def step(x, a, b):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


# ================================================================ painters
class PC:
    """What a painter gets for one UV group: the model coordinates X (across) and Y (along) of every pixel on a +z / -z face, the skin and the glow views."""

    def __init__(self, c):
        self.c, self.img, self.glow, self.T, self.S, self.A, self.B = c, c.img, c.glow, c.T, c.S, c.A, c.B
        g = c.group
        self.kind, self.key = g.kind, g.key
        if g.kind == "dom" and g.key[0] == 2:
            self.X = c.A if g.key[1] >= 0 else -c.A
            self.Y = c.B
            self.face = True
        else:
            self.X, self.Y, self.face = c.A, c.B, False


def paint(atlas, part, fn):
    """fn(pc) on every UV group of the part."""
    w.paint_part(atlas, part, lambda c: fn(PC(c)))


def put_glow(pc, mask, rgb, strength=1.0, core=0.35):
    w.set_glow(pc.c, rgb, strength, mask=mask, core=core)


def paint_glow(atlas, part, rgb, strength=0.8, core=0.3):
    """Glow over the whole part."""
    w.paint_part(atlas, part, lambda c: w.set_glow(c, rgb, strength, core=core))


def shade(pc, mask, rgb, k):
    """Multiplies the skin toward rgb where mask > 0 (k = 0 no change, 1 full)."""
    m3 = (np.clip(mask, 0, 1) * k)[..., None]
    pc.img[..., :3] = pc.img[..., :3] * (1 - m3) + np.clip(pc.img[..., :3].mean(axis=2, keepdims=True) / 128.0 * np.array(rgb, np.float32), 0, 255) * m3


def darken(pc, mask, k):
    pc.img[..., :3] *= (1 - np.clip(mask, 0, 1) * k)[..., None]


def paint_edge_glow(atlas, part, out, rgb, width=0.9, strength=0.9, core=0.4, side="both"):
    """A glow line along the cutting edge(s) of a plate."""
    def fn(pc):
        if not pc.face:
            return
        m = np.zeros_like(pc.X)
        if side in ("both", "edge"):
            m = np.maximum(m, sm(pc.X - out.l(pc.Y), width))
        if side in ("both", "spine"):
            m = np.maximum(m, sm(pc.X - out.r(pc.Y), width))
        put_glow(pc, m * (pc.Y > out.y.min()), rgb, strength, core)
    paint(atlas, part, fn)


def paint_center_glow(atlas, part, out, rgb, width=0.35, y0=None, y1=None, strength=0.9, core=0.5, wobble=0.0):
    """A glowing line down the middle of a plate (a fuller, a hamon, a rune seam), optionally between y0 and y1."""
    def fn(pc):
        if not pc.face:
            return
        Y = pc.Y
        cx = out.c(Y) + wobble * np.sin(Y * 0.5)
        m = sm(pc.X - cx, width)
        if y0 is not None:
            m = m * step(Y, y0, y0 + 2.0)
        if y1 is not None:
            m = m * (1 - step(Y, y1 - 2.0, y1))
        put_glow(pc, m, rgb, strength, core)
    paint(atlas, part, fn)


def paint_runes(atlas, part, out, rgb, y0, y1, n, seed=3, strength=0.9, side=0.0, size=0.5):
    """Small rune ticks down a blade face: n marks between y0 and y1, a glowing line each (and a dark groove)."""
    rng = np.random.default_rng(seed)
    marks = []
    for i in range(n):
        y = y0 + (y1 - y0) * (i + 0.5) / n
        kind = int(rng.integers(0, 4))
        marks.append((y, kind, float(rng.uniform(0.7, 1.1))))

    def fn(pc):
        if not pc.face:
            return
        X, Y = pc.X - out.c(pc.Y) - side, pc.Y
        m = np.zeros_like(X)
        for (y, kind, s) in marks:
            dy = (Y - y)
            hh = size * s
            if kind == 0:
                m = np.maximum(m, sm(dy, 0.25) * (np.abs(X) < hh))
            elif kind == 1:
                m = np.maximum(m, sm(X, 0.22) * (np.abs(dy) < hh))
            elif kind == 2:
                m = np.maximum(m, sm(dy - X * 0.8, 0.28) * (np.abs(X) < hh))
            else:
                m = np.maximum(m, np.maximum(sm(dy - X * 0.8, 0.26), sm(dy + X * 0.8, 0.26)) * (np.abs(X) < hh))
        darken(pc, m, 0.5)
        put_glow(pc, m, rgb, strength, 0.5)
    paint(atlas, part, fn)


def paint_gradient(atlas, part, lo_rgb, hi_rgb, y0, y1, k=0.7):
    """Tints a part from lo_rgb at y0 to hi_rgb at y1 along its length."""
    def fn(pc):
        t = step(pc.Y, y0, y1) if pc.face or pc.kind == "dom" else np.clip(pc.T, 0, 1)
        lo, hi = np.array(lo_rgb, np.float32), np.array(hi_rgb, np.float32)
        col = lo[None, None, :] * (1 - t[..., None]) + hi[None, None, :] * t[..., None]
        lum = pc.img[..., :3].mean(axis=2, keepdims=True) / 140.0
        pc.img[..., :3] = pc.img[..., :3] * (1 - k) + np.clip(col * lum, 0, 255) * k
    paint(atlas, part, fn)


def paint_checker(atlas, part, a_rgb, b_rgb, cell=2.0, k=0.6):
    """A woven / scaled pattern (used for bindings and scale armour)."""
    def fn(pc):
        cx = np.floor(pc.A / cell) + np.floor(pc.B / cell)
        col = np.where(((cx % 2) == 0)[..., None], np.array(a_rgb, np.float32), np.array(b_rgb, np.float32))
        lum = pc.img[..., :3].mean(axis=2, keepdims=True) / 140.0
        pc.img[..., :3] = pc.img[..., :3] * (1 - k) + np.clip(col * lum, 0, 255) * k
    paint(atlas, part, fn)


# ================================================================ icon
def stub_icon(path, diag=True):
    """A stand-in sprite (a thin diagonal) the placement reads: the real sprite is rendered later from the finished mesh."""
    a = np.zeros((16, 16, 4), np.uint8)
    for i in range(14):
        a[15 - (1 + i), 1 + i] = (200, 200, 200, 255)
        if 0 < i < 13:
            a[15 - (1 + i), 2 + i] = (150, 150, 150, 255)
    Image.fromarray(a, "RGBA").save(path)


def _widen_factor(quads, maxf=3.0):
    """How much a slender weapon is widened across its axis for the 16 x 16 sprite (a sprite blade is never under about two pixels): 0.1 x length / the typical width."""
    pts = np.concatenate([q[1] for q in quads if q[0] == 0])
    ly = float(pts[:, 1].max() - pts[:, 1].min())
    bins = np.linspace(pts[:, 1].min(), pts[:, 1].max(), 33)
    widths = []
    for a, b in zip(bins[:-1], bins[1:]):
        sel = pts[(pts[:, 1] >= a) & (pts[:, 1] < b)]
        if len(sel) > 3:
            widths.append(float(sel[:, 0].max() - sel[:, 0].min()))
    if not widths:
        return 1.0
    med = float(np.percentile(widths, 55))
    return float(min(maxf, max(1.0, 0.085 * ly / max(med, 1e-6))))


def _quads_rotated(m, atlas, deg, widen=True):
    """The mesh's quads rotated in the sprite plane by deg about z (what `place` does to the weapon axis); a slender weapon is widened across for the sprite."""
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    R = np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
    raw = [(layer, Pp, UV, N, tint_part) for (layer, Pp, UV, N, tint_part) in m.iter_quads("vertex", atlas)]
    f = _widen_factor(raw) if widen else 1.0
    S = np.diag([f, 1.0, 1.0])
    out = []
    for (layer, Pp, UV, N, tint_part) in raw:
        tn = (0.7, 0.7, 0.7) if tint_part.base is None else tuple(v / 255 for v in tint_part.base)
        out.append((layer, (Pp @ S) @ R.T, UV, N @ R.T, tn))
    return out


def render_icon(m, atlas, out_png, angle, boost=1.0, widen=True):
    """Renders the finished mesh as a 16 x 16 pixel-art sprite: the weapon turned by (angle - 90) degrees as `place` turns it, seen from +z, the skin and the glow
    layer, one dark outline pixel round the shape, a little saturation. The void layer is drawn as a dark crimson."""
    quads = _quads_rotated(m, atlas, angle - 90.0, widen)
    glow = (int(m.glow_cfg[0], 16), m.glow_cfg[1], m.glow_cfg[2], m.glow_cfg[3]) if m.glow_cfg else (0xFFFFFF, 0.12, 0.4, 1.0)
    pts = np.concatenate([q[1] for q in quads if q[0] == 0])
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    ext = float(max(hi[0] - lo[0], hi[1] - lo[1], 1e-6))
    PANEL = 14 * 16
    scale = PANEL / ext
    center = ((hi[0] + lo[0]) / 2, (hi[1] + lo[1]) / 2)
    img = pm.render_view(quads, 0, 0, (16 * 16, 16 * 16), scale, center, atlas.img, atlas.glow if atlas.glow[..., 3].max() > 0 else None, glow, ss=1, cull=True, vertex_normals=True)
    H = 16 * 16
    bgc = np.array([0.30, 0.32, 0.38], np.float32)
    bgrow = (0.8 + 0.4 * np.linspace(1, 0, H, dtype=np.float32))[:, None, None]
    bg = np.zeros((H, H, 3), np.float32) + bgc
    bg = bg * bgrow
    fg = (np.abs(img - bg).sum(axis=2) > 0.02).astype(np.float32)
    cell = img.reshape(16, 16, 16, 16, 3)
    fgc = fg.reshape(16, 16, 16, 16)
    a = fgc.mean(axis=(1, 3))
    wsum = np.maximum(fgc.sum(axis=(1, 3)), 1)[..., None]
    col = (cell * fgc[..., None]).sum(axis=(1, 3)) / wsum
    mask = a > 0.38
    if mask.sum() < 34:                                                            # a slender weapon (a pole, a lash, a bow): keep every pixel it touches
        mask = a > 0.14
    if mask.sum() < 40:                                                            # still thin: grow it by one pixel (the new pixels take the mean colour of their neighbours)
        pm_ = np.pad(mask, 1)
        grown = mask.copy()
        sums = np.zeros((16, 16, 3), np.float32)
        cnts = np.zeros((16, 16, 1), np.float32)
        cpad = np.pad(col, ((1, 1), (1, 1), (0, 0)))
        for dy, dx in ((0, 1), (1, 0), (0, -1), (-1, 0)):
            nb = pm_[1 + dy:17 + dy, 1 + dx:17 + dx]
            grown |= nb
            sums += cpad[1 + dy:17 + dy, 1 + dx:17 + dx] * nb[..., None]
            cnts += nb[..., None]
        new = grown & ~mask
        col = np.where(new[..., None], sums / np.maximum(cnts, 1), col)
        mask = grown
    out = np.zeros((16, 16, 4), np.float32)
    col = np.clip(col, 0, 1)
    lum = col.mean(axis=2, keepdims=True)
    col = np.clip(lum + (col - lum) * 1.25, 0, 1)                                  # a little more saturation
    col = np.clip((col - 0.5) * 1.12 + 0.5 + 0.02 * boost, 0, 1)
    out[..., :3] = col * 255
    out[..., 3] = np.where(mask, 255, 0)
    # one pixel outline in a dark shade of the neighbouring colour
    pad = np.pad(mask, 1)
    ring = np.zeros_like(mask)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dy or dx:
                ring |= pad[1 + dy:17 + dy, 1 + dx:17 + dx]
    ring &= ~mask
    avg = np.zeros((16, 16, 3), np.float32)
    cnt = np.zeros((16, 16, 1), np.float32)
    padc = np.pad(out[..., :3], ((1, 1), (1, 1), (0, 0)))
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dy or dx:
                avg += padc[1 + dy:17 + dy, 1 + dx:17 + dx] * np.pad(mask, 1)[1 + dy:17 + dy, 1 + dx:17 + dx][..., None]
                cnt += np.pad(mask, 1)[1 + dy:17 + dy, 1 + dx:17 + dx][..., None]
    ocol = (avg / np.maximum(cnt, 1)) * 0.28 + np.array([6, 4, 10], np.float32)
    out[ring, :3] = ocol[ring]
    out[ring, 3] = 255
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA").save(out_png)
    return out_png


# ================================================================ item models
def _template_display():
    with open(os.path.join(MODEL_DIR, "dead_blade.json"), encoding="utf-8") as f:
        d = json.load(f)
    d.pop("nusmp_display_preset", None)
    return d


def write_models(item_id, display=None):
    base = _template_display()
    base["textures"] = {"particle": "nusmp:item/" + item_id}
    if display:
        base["display"] = display
        base["nusmp_display_preset"] = "manual"
    with open(os.path.join(MODEL_DIR, item_id + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(base, f, indent=2)
    with open(os.path.join(MODEL_DIR, item_id + "_icon.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}, f, indent=2)


# ================================================================ finish
def shield_display():
    """The display block of the Aegis of the Abyss, the mod's upright shield (a shield is placed and held like it)."""
    with open(os.path.join(MODEL_DIR, "aegis_of_the_abyss.json"), encoding="utf-8") as f:
        return json.load(f)["display"]


def finish(item_id, m, atlas, cls, glow=("FFFFFF", 0.16, 0.45, 1.0), real_m=None, fit=False, angle=None, display=None, write_mesh=True, icon_boost=1.0, shield=False):
    """Places, writes and renders everything of one weapon (see the module doc). `cls` picks the apparent length; `fit` = an upright centred object; `shield` places the
    mesh upright like the Aegis of the Abyss (1.84 item units tall, centred) and gives it that shield's display block."""
    real = real_m or REAL_M[cls]
    tmp = os.path.join(tempfile.gettempdir(), "n197_stub_%d.png" % os.getpid())
    stub_icon(tmp)
    m.place_on_sprite(tmp, real_m=None if (fit or shield) else real, fit=fit or shield)
    if shield:
        ymin, ymax = m.y_range()
        sc = 1.84 / (ymax - ymin)
        m.place(0.536, 0.49 - 0.5 * (ymax - ymin) * sc, 90.0, sc, ymin)
        display = shield_display()
    if angle is not None and m.place_cfg:
        ox, oy, a0, sc, ymin = m.place_cfg
        m.place(ox, oy, angle, sc, ymin)
    m.set_glow(*glow)
    os.makedirs(MESH_DIR, exist_ok=True)
    n = 0
    if write_mesh:
        n = m.write_txt(os.path.join(MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment="generated by tools/relics/new197_*.py")
    atlas.save(item_id)
    ang = m.place_cfg[2]
    render_icon(m, atlas, os.path.join(ICON_DIR, item_id + ".png"), ang, icon_boost)
    write_models(item_id, display)
    st = m.stats(atlas)
    print(f"{item_id}: {len(m.parts)} parts, {n} quads {st['quads']}, length {st['length']:.1f}, atlas {atlas.size}px at {atlas.ppu:.1f} px/cm")
    if PREVIEW_DIR:
        os.makedirs(PREVIEW_DIR, exist_ok=True)
        ml.preview(m, None, os.path.join(PREVIEW_DIR, item_id + ".png"), atlas=atlas, normals="vertex")
    return st


def new_mesh(name):
    return ml.Mesh(name)


def new_atlas(size=512):
    return ml.Atlas(size)


def lay_and_paint(m, atlas, along=None):
    atlas.layout(m)
    atlas.paint(m, along)
