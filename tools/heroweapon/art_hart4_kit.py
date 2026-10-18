"""Why: the five weapons of the art builder hart4 (Vassal Mirror, Musical Instrument, Harpoon, Sewing Set, Chain) are built with the owner's model method
(tools/relics/mesh_lib.py), and they need the same small tools again and again: smooth curves for scrolls, chains and threads, a few solid shapes the
library lacks (a chain link, a cone, a leaf plate, a spiral), a paint kit for the skin (engraved lines, enamel inlays, gem shading, glow masks), the
pixel-sprite helpers, the writer of the item models and the placement of a mesh on its sprite. This module holds them so art_hart4.py can read as the
design of five weapons, one function each, and nothing here is specific to one weapon.

Nothing in this file writes outside the files of the five weapons. Everything is deterministic (no random numbers except seeded generators).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import mesh_lib as ml                                           # noqa: E402
import wave1_models as w1                                       # noqa: E402  (add, paint_part, set_glow, ellipsoid, helix, hermite, noise2)
from wave1_icons import Canvas                                  # noqa: E402

V = np.array


def _layout(root):
    """(root, meshes, skins, sprites, item models): where each kind of file of a weapon lives under an assets/nusmp-shaped folder."""
    return (root, os.path.join(root, "weapon_meshes"), os.path.join(root, "textures", "entity", "weapon"), os.path.join(root, "textures", "item"),
            os.path.join(root, "models", "item"))


# the mod's real assets; set_output_root() points the writers elsewhere (the self test builds twice into temporary folders)
ASSETS, MESH_DIR, SKIN_DIR, ICON_DIR, MODEL_DIR = _layout(ml.ASSETS)


def set_output_root(root):
    """Writes every file of the weapons under `root` (laid out like assets/nusmp) instead of the mod's assets; None goes back to the mod. Returns the root."""
    global ASSETS, MESH_DIR, SKIN_DIR, ICON_DIR, MODEL_DIR
    ASSETS, MESH_DIR, SKIN_DIR, ICON_DIR, MODEL_DIR = _layout(ml.ASSETS if root is None else root)
    return ASSETS


HAND_SCALE = 0.85                    # the third-person display scale every generated model json starts with (brooch_pin.json): apparent length = mesh length x this
ICON_AXIS_DEG = 45.0                 # the old sprite's diagonal: the display transforms of the item models are made for it (docs/weapon_scale_orientation.md)


# ================================================================ small maths
def unit(v):
    v = V(v, float)
    return v / max(float(np.linalg.norm(v)), 1e-9)


def smooth01(x, a, b):
    t = np.clip((np.asarray(x, np.float32) - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


# ================================================================ curves
def catmull(points, per=8, closed=False, alpha=0.5):
    """A centripetal Catmull-Rom spline through the points (no overshoot, no loops): the dense polyline of a scroll, a thread, a sagging chain."""
    P = [V(p, float) for p in points]
    n = len(P)
    ext = ([P[-1]] + P + [P[0], P[1]]) if closed else ([2 * P[0] - P[1]] + P + [2 * P[-1] - P[-2]])
    out = []
    for i in range(n if closed else n - 1):
        p0, p1, p2, p3 = ext[i], ext[i + 1], ext[i + 2], ext[i + 3]
        t0 = 0.0
        t1 = t0 + max(float(np.linalg.norm(p1 - p0)), 1e-6) ** alpha
        t2 = t1 + max(float(np.linalg.norm(p2 - p1)), 1e-6) ** alpha
        t3 = t2 + max(float(np.linalg.norm(p3 - p2)), 1e-6) ** alpha
        for k in range(per):
            t = t1 + (t2 - t1) * k / per
            a1 = (t1 - t) / (t1 - t0) * p0 + (t - t0) / (t1 - t0) * p1
            a2 = (t2 - t) / (t2 - t1) * p1 + (t - t1) / (t2 - t1) * p2
            a3 = (t3 - t) / (t3 - t2) * p2 + (t - t2) / (t3 - t2) * p3
            b1 = (t2 - t) / (t2 - t0) * a1 + (t - t0) / (t2 - t0) * a2
            b2 = (t3 - t) / (t3 - t1) * a2 + (t - t1) / (t3 - t1) * a3
            out.append((t2 - t) / (t2 - t1) * b1 + (t - t1) / (t2 - t1) * b2)
    if not closed:
        out.append(P[-1])
    return out


def arc_points(pts, n):
    """The polyline resampled to n points at equal arc length (the ends are kept)."""
    P = [V(p, float) for p in pts]
    seg = [float(np.linalg.norm(P[i + 1] - P[i])) for i in range(len(P) - 1)]
    cum = np.concatenate([[0.0], np.cumsum(seg)])
    out = []
    for s in np.linspace(0.0, cum[-1], n):
        i = int(min(np.searchsorted(cum, s, side="right") - 1, len(seg) - 1))
        t = (s - cum[i]) / max(seg[i], 1e-9)
        out.append(P[i] + (P[i + 1] - P[i]) * t)
    return out


def at_arc(pts, s):
    """(point, unit tangent) at arc length s along the polyline (clamped to its ends)."""
    P = [V(p, float) for p in pts]
    run = 0.0
    for i in range(len(P) - 1):
        d = float(np.linalg.norm(P[i + 1] - P[i]))
        if run + d >= s or i == len(P) - 2:
            t = min(max((s - run) / max(d, 1e-9), 0.0), 1.0)
            return P[i] + (P[i + 1] - P[i]) * t, unit(P[i + 1] - P[i])
        run += d
    return P[-1], unit(P[-1] - P[-2])


def closed_loop(pts):
    """A polyline closed by repeating its first point (for resampling a closed outline by arc length)."""
    pts = [tuple(p) for p in pts]
    return pts + [pts[0]]


def sweep_loop(p, pts, section, up):
    """Sweeps a section round a CLOSED loop of points (listed once, not repeated) and welds the seam.
    Why: ml.sweep gives the first and the last ring of a path a one-sided tangent, so a loop swept as a path that ends where it began has two end rings turned against
    each other by one polyline step: a wedge-shaped crack (and a hard lighting edge) at the seam of every ring, link and eye. Here every ring, the seam included, takes the
    central tangent from its two neighbours round the loop, and the section keeps the loop's plane normal `up`, so the closing ring lies exactly on the first one and the
    normals smooth across the seam. `up` is the normal of the loop's plane. ml._build is the engine under ml.sweep and takes explicit frames."""
    P = [V(q, float) for q in pts]
    n = len(P)
    if n < 3:
        raise ValueError(f"{p.name}: a loop needs at least three points")
    up = unit(up)
    frames, infos = [], []
    for i in range(n + 1):
        t = unit(P[(i + 1) % n] - P[(i - 1) % n])
        ev = unit(up - t * float(np.dot(up, t)))
        frames.append((P[i % n], np.cross(t, ev), ev, t))
        infos.append((i / n, 1.0, 1.0, 0.0, 0.0))
    return ml._build(p, frames, infos, section, (False, False), p.name)


def oval_outline(a, b, n=56, cy=0.0, egg=0.0, z=0.0, waist=0.0):
    """A counter-clockwise (seen from +z) closed outline, starting at the top: semi-axes a (x) and b (y); egg > 0 makes the top wider than the bottom;
    waist > 0 pinches the lower end toward the neck. n points, not repeated."""
    pts = []
    for i in range(n):
        th = 2 * math.pi * i / n
        w = 1.0 + egg * math.cos(th) - waist * max(0.0, -math.cos(th)) ** 2
        pts.append((-a * math.sin(th) * w, cy + b * math.cos(th), z))
    return pts


# ================================================================ solid shapes the library lacks
def part(m, name, material, **kw):
    """A new named part in the mesh (wave1_models.add)."""
    return w1.add(m, name, material, **kw)


def cone(p, base, direction, length, r, sides=5, r_tip=0.0, closed_base=False, bulge=0.0):
    """A spike: a cone (or a thorn with a slightly bulged flank) from `base` along `direction`."""
    st = [(0.0, 1.0, 1.0)]
    if bulge:
        st.append((length * 0.35, 1.0 - 0.55 + bulge, 1.0 - 0.55 + bulge))
    else:
        st.append((length * 0.35, 0.62, 0.62))
    st.append((length, r_tip / max(r, 1e-9), r_tip / max(r, 1e-9)))
    return ml.loft(p, tuple(unit(direction)), tuple(base), st, ml.circle(r, sides), caps=(closed_base, r_tip > 0))


def ball(p, center, R, sides=12, stacks=8, squash=1.0):
    """A sphere (an ellipsoid whose through-thickness is `squash` of its radius)."""
    return w1.ellipsoid(p, center, R, R, R * squash, sides=sides, stacks=stacks)


def dome(p, center, rx, ry, rz, sides=12, stacks=8):
    """A cabochon: an ellipsoid with radii across (rx), along y (ry) and through z (rz): a gem, a boss, a pearl."""
    return w1.ellipsoid(p, center, rx, ry, rz, sides=sides, stacks=stacks)


def leaf_plate(p, root, direction, length, width, thick, normal=(0, 0, 1), base_w=0.35, widest=0.38, ridge=0.0, tip_w=0.0):
    """A flat leaf (a petal, a feather, a blade of a crown): from `root` along `direction`, widest at `widest` of its length, lens section."""
    st = [(0.0, base_w, 1.0), (length * widest, 1.0, 1.0), (length * 0.78, 0.62, 0.8), (length, tip_w, tip_w)]
    return ml.loft(p, tuple(unit(direction)), tuple(root), st, ml.lozenge(width, thick, ridge), up=tuple(normal), caps=(True, tip_w > 0))


def spiral_points(center, r0, r1, turns, a0_deg, n=24, plane=("x", "y"), z=0.0, ccw=True):
    """A planar spiral from radius r0 (angle a0) to r1 after `turns`: the curl at the end of a scroll."""
    out = []
    for i in range(n + 1):
        t = i / n
        a = math.radians(a0_deg) + (1 if ccw else -1) * 2 * math.pi * turns * t
        r = r0 + (r1 - r0) * t
        out.append((center[0] + r * math.cos(a), center[1] + r * math.sin(a), z))
    return out


def stadium(Lc, Wc, n_arc=4):
    """A stadium (a chain link's centre line) as a counter-clockwise (s, q) loop: long axis s, total centre-line length Lc and width Wc."""
    R, h = Wc / 2.0, (Lc - Wc) / 2.0
    pts = []
    for k in range(n_arc + 1):
        phi = math.radians(-90 + 180.0 * k / n_arc)
        pts.append((h + R * math.cos(phi), R * math.sin(phi)))
    for k in range(n_arc + 1):
        phi = math.radians(90 + 180.0 * k / n_arc)
        pts.append((-h + R * math.cos(phi), R * math.sin(phi)))
    return pts


def chain_link(p, center, T, N, Lc, Wc, r, sides=5, n_arc=4):
    """One chain link: a round wire (radius r) bent into a stadium with its long axis along T and its plane spanned by T and N."""
    T, N = unit(T), unit(N)
    c = V(center, float)
    pts = [tuple(c + T * s + N * q) for (s, q) in stadium(Lc, Wc, n_arc)]
    return sweep_loop(p, pts, ml.circle(r, sides), up=tuple(np.cross(T, N)))


# ================================================================ painting: the kit that works on a UV group's pixels (w1.paint_part gives ctx)
def blur(mask, r):
    im = Image.fromarray((np.clip(mask, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, np.float32) / 255.0


def band(x, lo, hi, soft=0.03):
    """1 inside [lo, hi], soft edges of width `soft` (in the units of x)."""
    return smooth01(x, lo - soft, lo + soft) * (1.0 - smooth01(x, hi - soft, hi + soft))


def stripes(x, period, width, phase=0.0, soft=0.06):
    """Periodic bars along coordinate x (model units): a bar of `width` centred on `phase`, repeated every `period`."""
    f = np.mod(np.asarray(x, np.float32) - phase + period / 2.0, period) - period / 2.0
    return band(f, -width / 2.0, width / 2.0, soft)


def disc_mask(A, B, ca, cb, r, soft=0.15):
    d = np.hypot(np.asarray(A, np.float32) - ca, np.asarray(B, np.float32) - cb)
    return 1.0 - smooth01(d, r - soft, r + soft)


def seg_mask(A, B, p0, p1, width, soft=0.12):
    """A line segment p0 -> p1 (in the group's A / B coordinates) of the given width."""
    A = np.asarray(A, np.float32)
    B = np.asarray(B, np.float32)
    ax, ay = p1[0] - p0[0], p1[1] - p0[1]
    L2 = max(ax * ax + ay * ay, 1e-9)
    t = np.clip(((A - p0[0]) * ax + (B - p0[1]) * ay) / L2, 0.0, 1.0)
    d = np.hypot(A - (p0[0] + t * ax), B - (p0[1] + t * ay))
    return 1.0 - smooth01(d, width / 2 - soft, width / 2 + soft)


def seg_dist(A, B, p0, p1):
    """Unsigned distance of every pixel (A, B) to the segment p0 -> p1."""
    A = np.asarray(A, np.float32)
    B = np.asarray(B, np.float32)
    ax, ay = p1[0] - p0[0], p1[1] - p0[1]
    L2 = max(ax * ax + ay * ay, 1e-9)
    t = np.clip(((A - p0[0]) * ax + (B - p0[1]) * ay) / L2, 0.0, 1.0)
    return np.hypot(A - (p0[0] + t * ax), B - (p0[1] + t * ay))


def poly_dist(A, B, pts):
    """Unsigned distance of every pixel to a closed polygon's outline."""
    d = np.full(np.shape(A), 1e9, np.float32)
    n = len(pts)
    for i in range(n):
        d = np.minimum(d, seg_dist(A, B, pts[i], pts[(i + 1) % n]))
    return d


def poly_inside(A, B, pts):
    """Even-odd test of every pixel against a closed polygon."""
    A = np.asarray(A, np.float32)
    B = np.asarray(B, np.float32)
    inside = np.zeros(A.shape, bool)
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        cond = (y1 > B) != (y2 > B)
        xint = (x2 - x1) * (B - y1) / (y2 - y1 + 1e-12) + x1
        inside ^= cond & (A < xint)
    return inside


def poly_mask(A, B, pts, width, soft=0.12, closed=False):
    m = np.zeros(np.shape(A), np.float32)
    seq = list(pts) + ([pts[0]] if closed else [])
    for a, b in zip(seq[:-1], seq[1:]):
        m = np.maximum(m, seg_mask(A, B, a, b, width, soft))
    return m


def mix_to(ctx, mask, color, k=1.0):
    m = (np.clip(mask, 0, 1) * k)[..., None]
    ctx.img[..., :3] = ctx.img[..., :3] * (1.0 - m) + np.array(color, np.float32) * m


def tone(ctx, mask, k):
    """Multiplies the brightness inside the mask by k (below 1 darkens, above 1 lightens)."""
    m = np.clip(mask, 0, 1)[..., None]
    ctx.img[..., :3] = np.clip(ctx.img[..., :3] * (1.0 + (k - 1.0) * m), 0, 255)


def engrave(ctx, mask, depth=0.5, lip=0.5):
    """A cut line: dark inside the mask and a light lip on its lower edge (the light comes from above)."""
    d = np.clip(mask, 0, 1)
    lipm = np.clip(np.roll(d, 1, axis=0) - d, 0, 1)
    tone(ctx, d, 1.0 - depth)
    ctx.img[..., :3] = np.clip(ctx.img[..., :3] + 255.0 * 0.5 * lip * lipm[..., None], 0, 255)


def glow_on(ctx, mask, color, strength=1.0, core=0.3):
    """The additive glow skin, where the mask is set."""
    w1.set_glow(ctx, color, strength, mask=np.clip(mask, 0, 1), core=core)


def fill_glow(ctx, color, strength=1.0, core=0.3):
    w1.set_glow(ctx, color, strength, core=core)


def noise(ctx, cell, seed, amp=0.1):
    """A soft brightness noise over the group (patina, brushing): cell in pixels."""
    n = w1.noise2(ctx.img.shape[:2], cell, seed) - 0.5
    ctx.img[..., :3] = np.clip(ctx.img[..., :3] * (1.0 + 2.0 * amp * n[..., None]), 0, 255)


def streaks(shape, seed, along=60.0, across=2.5):
    """Smooth noise in 0..1 that is long in the direction of the rows (the length of a lofted strip) and narrow across it: wood grain, salt streaks, rust runs."""
    return ml._noise(np.random.default_rng(seed), shape[0], shape[1], along, across)


def gem_lens(ctx, color, glow_color=None, glow=0.6, body=0.85, hot=(255, 255, 255)):
    """A gem lathed along the x axis (two domes back to back, the poles are the two faces that show): bright toward each pole with a white spot, dark at the rim."""
    S, T = ctx.S, ctx.T
    pole = smooth01(np.abs(T - 0.5) * 2.0, 0.15, 1.0)
    col = np.array(color, np.float32) * body
    base = 0.5 + 0.6 * pole
    img = col[None, None, :] * base[..., None]
    spot = np.maximum(np.exp(-((np.hypot((S - 0.5) * 1.6, T - 0.2)) / 0.09) ** 2), np.exp(-((np.hypot((S - 0.5) * 1.6, T - 0.8)) / 0.09) ** 2))
    img = img + (np.array(hot, np.float32) - img) * (spot * 0.95)[..., None]
    ctx.img[..., :3] = np.clip(img, 0, 255)
    ctx.img[..., 3] = 255
    w1.set_glow(ctx, glow_color or color, glow, mask=np.clip(0.4 + 0.6 * pole, 0, 1), core=0.45)


def gem_cabochon(ctx, color, hi=(0.25, 0.38), hot=(255, 255, 255), glow=0.6, facets=0, seed=1, glow_color=None, body=0.85):
    """Paints a gem on a lathe's unrolled skin (S round the axis, T from pole to pole): a deep body (`color` x `body`), a bright centre, a white specular
    spot at `hi` (S, T), a darker rim toward the poles; the glow skin gets `glow_color` (default `color`) with a hot core at strength `glow`. With `facets` > 0
    the skin is cut into that many flat facets of different brightness (a faceted jewel)."""
    S, T = ctx.S, ctx.T
    col = np.array(color, np.float32) * body
    dS = np.minimum(np.abs(S - hi[0]), 1 - np.abs(S - hi[0]))
    dist = np.hypot(dS * 2.2, T - hi[1])
    centre = np.exp(-(dist / 0.34) ** 2)
    pole = 1.0 - smooth01(np.abs(T - 0.5), 0.30, 0.5) * 0.65
    base = (0.5 + 0.55 * centre) * pole
    if facets:
        rng = np.random.default_rng(seed)
        vals = 0.72 + 0.5 * rng.random(facets)
        idx = np.floor(S * facets).astype(int) % facets
        base = base * vals[idx]
        edge = np.minimum(np.mod(S * facets, 1.0), 1.0 - np.mod(S * facets, 1.0))
        base = base + 0.35 * (1.0 - smooth01(edge, 0.0, 0.06))
    img = col[None, None, :] * base[..., None]
    img = img + (np.array(hot, np.float32) - img) * (np.exp(-(dist / 0.075) ** 2) * 0.95)[..., None]
    ctx.img[..., :3] = np.clip(img, 0, 255)
    ctx.img[..., 3] = 255
    g = np.clip(0.55 + 0.45 * centre, 0, 1) * pole
    w1.set_glow(ctx, glow_color or color, glow, mask=g, core=0.45)


# ================================================================ sprites (the 16 x 16 inventory icons)
def new_sprite():
    """A blank 16 x 16 icon canvas (the wave 1 icon helper)."""
    return Canvas()


def save_sprite(cv, outline_rgb, item_id):
    """Outlines the canvas, writes textures/item/<id>.png and returns its path."""
    cv.outline(outline_rgb)
    os.makedirs(ICON_DIR, exist_ok=True)
    path = os.path.join(ICON_DIR, item_id + ".png")
    cv.image().save(path)
    return path


def sheet_of_icons(ids, out, zoom=14):
    """The enlarged contact sheet of icons, for looking at them."""
    n = len(ids)
    sh = Image.new("RGBA", (n * (16 * zoom + 10) + 10, 16 * zoom + 20), (74, 76, 92, 255))
    for k, i in enumerate(ids):
        im = Image.open(os.path.join(ICON_DIR, i + ".png")).convert("RGBA")
        big = im.resize((16 * zoom, 16 * zoom), Image.NEAREST)
        sh.paste(big, (10 + k * (16 * zoom + 10), 10), big)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sh.save(out)
    return out


# ================================================================ the item models, the placement, the writing of a weapon
def write_item_models(item_id):
    """models/item/<id>.json (the in-hand model, builtin/entity, the display transforms of brooch_pin) and <id>_icon.json (the inventory icon, handheld)."""
    with open(os.path.join(ml.ASSETS, "models", "item", "brooch_pin.json"), encoding="utf-8") as f:
        src = json.load(f)
    os.makedirs(MODEL_DIR, exist_ok=True)
    model = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": src["display"]}
    with open(os.path.join(MODEL_DIR, item_id + ".json"), "w", newline="\n", encoding="utf-8") as f:
        json.dump(model, f, indent=2)
    icon = {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}
    with open(os.path.join(MODEL_DIR, item_id + "_icon.json"), "w", newline="\n", encoding="utf-8") as f:
        json.dump(icon, f, indent=2)


def apply_display_preset(item_id):
    """The first-person / ground / frame scales of a long weapon (tools/relics/display_presets.py, for this id only, so the shared manifest stays the lead's)."""
    import display_presets as dp
    mesh = dp.read_mesh(os.path.join(MESH_DIR, item_id + ".txt"))
    path = os.path.join(MODEL_DIR, item_id + ".json")
    with open(path, encoding="utf-8") as f:
        raw = f.read()
    model = json.loads(raw)
    plan = dp.plan_for(item_id, model, mesh)
    if plan["action"] in ("preset", "restore"):
        text = json.dumps(dp.apply_plan(model, plan), indent=2)
        if text != raw:
            with open(path, "w", newline="\n", encoding="utf-8") as f:
                f.write(text)
    return plan


def diag_axis(icon_png, angle=ICON_AXIS_DEG):
    """(origin, angle, length) of the sprite along the fixed diagonal: the lowest opaque pixel on the axis, and the extent of the pixels along it."""
    a = np.asarray(Image.open(icon_png).convert("RGBA"))
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    P = np.stack([(xs + 0.5) / S, 1 - (ys + 0.5) / S], 1)
    d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
    c = P.mean(0)
    t = (P - c) @ d
    return c + d * t.min(), angle, float(t.max() - t.min())


HAND_POINT = 0.25                    # where the hand holds the sprite, as a share of its diagonal from the butt (display_presets.GRIP_FRACTION; measured on the held views)


def place_on_diagonal(mesh, icon_png, real_m, grip):
    """mesh_lib's place_on_sprite with the axis fixed at 45 degrees: the butt on the sprite's lowest point, the mesh grown to an apparent `real_m` blocks in the
    hand (mesh length x 0.85), and placed so that the point of the mesh at share `grip` of its length (from the butt) lies where the hand holds a sprite (a quarter
    of the way along its diagonal). For grip = 0.25 this is exactly place_on_sprite's growth about the hand."""
    ymin, ymax = mesh.y_range()
    origin, angle, length = diag_axis(icon_png)
    scale = length / max(ymax - ymin, 1e-6)
    kk = real_m / (HAND_SCALE * length)
    d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
    origin = origin + d * length * (HAND_POINT - grip * kk)
    mesh.place(float(origin[0]), float(origin[1]), angle, scale * kk, ymin)
    return kk


def write_weapon(mesh, atlas, item_id, icon_png, real_m, grip, glow=("FFFFFF", 0.12, 0.35, 1.0)):
    """Places the mesh on its sprite, writes the mesh (smoothed per-vertex normals), both skins and the item models; returns the stats."""
    kk = place_on_diagonal(mesh, icon_png, real_m, grip)
    mesh.set_glow(*glow)
    os.makedirs(MESH_DIR, exist_ok=True)
    mesh.write_txt(os.path.join(MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment="generated by tools/heroweapon/art_hart4.py")
    atlas.save(item_id, root=ASSETS)
    st = mesh.stats(atlas)
    write_item_models(item_id)
    apply_display_preset(item_id)
    quads = sum(st["quads"].values())
    if os.environ.get("HART4_PARTS") == "1":
        glow_n = {}
        for (layer, _P, _UV, _N, pt) in mesh.iter_quads("vertex", atlas):
            if layer == 2:
                glow_n[pt.name] = glow_n.get(pt.name, 0) + 1
        for pt in mesh.parts:
            print(f"    {pt.name:18s} {len(pt.F):5d} quads  + {glow_n.get(pt.name, 0):4d} glow  ({pt.material})")
    print(f"{item_id}: {len(mesh.parts)} parts, {quads} quad lines {st['quads']}, length {st['length']:.1f}, atlas {atlas.size}px at {atlas.ppu:.2f} px/unit, grown x{kk:.2f}")
    return st


def lay_out(mesh, size=512, ppu_max=24.0):
    """The atlas of a mesh: every part's UV groups packed, every part painted with its material (the custom painters run after this)."""
    atlas = ml.Atlas(size)
    atlas.layout(mesh, ppu_max=ppu_max)
    atlas.paint(mesh)
    return atlas


def render(item_id, out_png, views=("profile", "three_quarter", "edge", "top"), panel=None):
    """Renders the written mesh and skins (the game's own files) with the software preview; returns the path."""
    import preview_mesh
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    return preview_mesh.render_preview(item_id, None, out_png, views=views, normals="vertex", panel=panel)


# ================================================================ looking at a weapon the way the game shows it (the preview tools of the repo, put together)
def _boxes_steve():
    """Steve's side view as tinted boxes in player model space (blocks, y down, forward -z, the right side is -x): head, body, legs and the right arm
    turned by -18 degrees about its pivot like the arm of a player who holds an item (weapon_sheet.arm_poly)."""
    import weapon_sheet as ws
    kk = 1 / 16.0
    boxes = [((-4 * kk, 4 * kk), (-8 * kk, 0), (-4 * kk, 4 * kk), (0.30, 0.37, 0.50)),
             ((-4 * kk, 4 * kk), (0, 12 * kk), (-2 * kk, 2 * kk), (0.20, 0.25, 0.36)),
             ((-4 * kk, 4 * kk), (12 * kk, 24 * kk), (-2 * kk, 2 * kk), (0.17, 0.20, 0.30))]
    out = []
    for (x0, x1), (y0, y1), (z0, z1), col in boxes:
        out.append((_box_quads(x0, x1, y0, y1, z0, z1), np.eye(4), col))
    arm = _box_quads(-8 * kk, -4 * kk, -2 * kk, 10 * kk, -2 * kk, 2 * kk)
    M = ws.T(-5 * kk, 2 * kk, 0) @ ws.Rx(-18.0) @ ws.T(5 * kk, -2 * kk, 0)
    out.append((arm, M, (0.36, 0.42, 0.55)))
    return out


def _box_quads(x0, x1, y0, y1, z0, z1):
    """The six faces of an axis-aligned box as (P 4x3, N) with counter-clockwise winding seen from outside."""
    c = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    f = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 4, 7, 3), (1, 2, 6, 5), (0, 1, 5, 4), (3, 7, 6, 2)]
    n = [(0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0), (0, -1, 0), (0, 1, 0)]
    return [(np.array([c[i] for i in q], float), np.array(n[j], float)) for j, q in enumerate(f)]


def held_view(item_id, out_png, mode="third", size=None, fov_deg=70.0, zoom=None):
    """The weapon placed by the game's own transform chain (model json display, place line) and drawn with the repo's software rasteriser with its
    real skin and glow. mode 'third': the right hand seen from the player's right side next to Steve (orthographic, the player faces right);
    mode 'first': the first person right hand through a 70 degree perspective camera (vanilla's resting hand position). Returns the path."""
    import weapon_sheet as ws
    import preview_mesh as pm
    d = pm.parse_game_txt(os.path.join(MESH_DIR, item_id + ".txt"))
    with open(os.path.join(MODEL_DIR, item_id + ".json"), encoding="utf-8") as f:
        model = json.load(f)["display"]
    place = d["place"]
    Mp = ws.place_matrix(place)
    skin = pm._tex(os.path.join(SKIN_DIR, item_id + ".png"))
    gpath = os.path.join(SKIN_DIR, item_id + "_glow.png")
    gtex = pm._tex(gpath) if os.path.exists(gpath) else None
    glow = d["glow"]
    tint = np.array([(glow[0] >> 16) & 255, (glow[0] >> 8) & 255, glow[0] & 255], np.float32) / 255 * glow[3] if glow else np.ones(3, np.float32)
    if mode == "third":
        W, H = size or (760, 640)
        rot, tr, sc = (model["thirdperson_righthand"][key] for key in ("rotation", "translation", "scale"))
        Mw = ws.hand_matrix(rot, tr, sc) @ Mp
        px = zoom or 190.0
        A = np.array([[0, 0, -1], [0, -1, 0], [-1, 0, 0]], float) * ws.ENTITY_SCALE            # player model space -> (screen right, screen up, toward the viewer)
        cx, cy = W * 0.40, H * 0.46

        def project(P):
            Q = P @ A.T
            return np.stack([cx + Q[:, 0] * px, cy - Q[:, 1] * px, Q[:, 2] * px], axis=1), Q
        rotv = A / ws.ENTITY_SCALE
    else:
        W, H = size or (760, 428)
        rot, tr, sc = (model["firstperson_righthand"][key] for key in ("rotation", "translation", "scale"))
        Mw = ws.T(0.56 + tr[0] / 16, -0.52 + tr[1] / 16, -0.72 + tr[2] / 16) @ ws.Rx(rot[0]) @ ws.Ry(rot[1]) @ ws.Rz(rot[2]) @ ws.S(*sc) @ ws.T(-0.5, -0.5, -0.5) @ Mp
        f = (H / 2) / math.tan(math.radians(fov_deg / 2))

        def project(P):
            z = np.maximum(-P[:, 2], 0.02)
            return np.stack([W / 2 + P[:, 0] / z * f, H / 2 - P[:, 1] / z * f, 1.0 / z * 300.0], axis=1), P
        rotv = np.eye(3)
    ss = 2
    Wn, Hn = W * ss, H * ss
    img = np.zeros((Hn, Wn, 3), np.float32) + np.array([0.30, 0.32, 0.38], np.float32)
    img *= (0.8 + 0.4 * np.linspace(1, 0, Hn, dtype=np.float32))[:, None, None]
    zb = np.full((Hn, Wn), -1e9, np.float32)
    stars = pm._stars(Wn, Hn)

    def draw(quads, M, skin_tex, glow_tex, tn, layer_filter):
        for (layer, P, UV, N) in quads:
            if layer not in layer_filter:
                continue
            Pw = np.array([(M @ np.append(p, 1.0))[:3] for p in P])
            Nw = np.array([unit(M[:3, :3] @ n) for n in N])
            S3, Q = project(Pw)
            S3 = S3 * np.array([ss, ss, 1.0])
            Nv = np.array([unit(rotv @ n) for n in Nw])
            for tri in ((0, 1, 2), (0, 2, 3)):
                a = (S3[tri[1], 0] - S3[tri[0], 0]) * (S3[tri[2], 1] - S3[tri[0], 1]) - (S3[tri[2], 0] - S3[tri[0], 0]) * (S3[tri[1], 1] - S3[tri[0], 1])
                if abs(a) < 1e-9 or a > 0:
                    continue
                if layer == 0:
                    uv = UV[list(tri)] * (skin_tex.shape[1] / 128.0 if skin_tex is not None else 1.0)
                    pm._raster(img, zb, S3[list(tri)], uv, Nv[list(tri)], skin_tex, np.array(tn, np.float32), "body", stars)
                elif layer == 2 and glow_tex is not None:
                    uv = UV[list(tri)] * (glow_tex.shape[1] / 128.0)
                    pm._raster(img, zb, S3[list(tri)], uv, Nv[list(tri)], glow_tex, tint, "glow", stars)
    if mode == "third":
        for (bq, Mq, col) in _boxes_steve():
            qs = [(0, P, np.zeros((4, 2)), np.tile(n, (4, 1))) for (P, n) in bq]
            draw(qs, ws.T(0, 0, 0) @ Mq, None, None, col, (0,))
    draw(d["quads"], Mw, skin, gtex, (0.7, 0.7, 0.7), (0,))
    draw(d["quads"], Mw, skin, gtex, (0.7, 0.7, 0.7), (2,))
    out = np.clip(img, 0, 1).reshape(H, ss, W, ss, 3).mean(axis=(1, 3))
    os.makedirs(os.path.dirname(os.path.abspath(out_png)), exist_ok=True)
    Image.fromarray((out * 255).astype(np.uint8)).save(out_png)
    return out_png
