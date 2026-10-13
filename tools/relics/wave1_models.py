"""Relics of Many Worlds, wave 1: the ten hand-held relics as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave1_models.py [id ...]        (no ids = all ten; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave1_models.py staff_of_moonlight      (also renders the preview beside the card)

Run tools/relics/wave1_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon).
Per id it writes assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png and <id>_glow.png.

The method, in order, for every weapon (each module below follows it):
  0 target decided: the card's art and the printed text.
  1 measure the reference: pixels along one known length -> centimetres; every proportion is a NAMED CONSTANT with a comment saying where it came from.
  2 geometry from real cross-sections (loft / sweep / lathe), never a box or a four-sided frustum for a long form.
  3 the small raised parts are the detail: each is its own named Part with its own constants.
  4 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one skin per weapon.
  5 painted material (steel, wood, crystal, cloth ... with highlights and wear along the form) plus a second additive glow skin.
  6 rendered beside the reference (W1_PREVIEW) and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.
"""
import importlib
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                           # noqa: E402

ROOT = ml.ROOT
ASSETS = ml.ASSETS
ICON_DIR = os.path.join(ASSETS, "textures", "item")
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
ZIP = os.environ.get("RELICS_ZIP", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics_zip/Relics of many worlds")


# ================================================================ helpers shared by the ten models
def px_to_cm(real_cm, ref_px):
    """Step 1 of the method: the centimetres one reference pixel stands for, from one known length."""
    return real_cm / float(ref_px)


def add(m, name, material, **kw):
    """A new named part in the mesh."""
    return m.add(ml.Part(name, material=material, **kw))


class Ctx:
    """What a custom painter gets for one UV group: arrays of the group's pixels and views of the skin and the glow skin."""

    def __init__(self, g, img, glow, T, S, A, B):
        self.g, self.group, self.img, self.glow, self.T, self.S, self.A, self.B = g, g, img, glow, T, S, A, B
        self.part = g.part
        self.kind = g.kind


def paint_part(atlas, part, fn):
    """Calls fn(ctx) for every UV group of the part. ctx.T = 0..1 along the form (loft groups: from the start of the loft; dominant groups: model y
    scaled to the part's y range), ctx.S = 0..1 across, ctx.A / ctx.B = arc length and length along (loft) or the model coordinates a (right) and b (up)."""
    lo = min(v[1] for v in part.V)
    hi = max(v[1] for v in part.V)
    for g in part.groups:
        x, y, w, h = g.rect
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        S = (xx + 0.5) / max(w, 1)
        if g.kind == "loft":
            T = 1.0 - (yy + 0.5) / max(h, 1)
            A = (xx + 0.5) / g.ppu
            B = T * g.ext[1]
        else:
            A = g.origin[0] + (xx + 0.5) / g.ppu
            B = g.origin[1] - (yy + 0.5) / g.ppu
            T = np.clip((B - lo) / max(hi - lo, 1e-6), 0, 1) if g.key[0] != 1 else np.zeros_like(A)
        fn(Ctx(g, atlas.img[y:y + h, x:x + w], atlas.glow[y:y + h, x:x + w], T, S, A, B))


def tint(img, rgb, k=1.0):
    """Multiplies the colour of a painted patch toward rgb (0..255) by k (0 = no change, 1 = fully rgb-tinted luminance)."""
    lum = img[..., :3].mean(axis=2, keepdims=True) / 128.0
    img[..., :3] = img[..., :3] * (1 - k) + np.clip(lum * np.array(rgb, np.float32), 0, 255) * k


def set_glow(c, rgb, strength=1.0, mask=None, core=0.35):
    """Fills the context's glow skin with a colour (a hot core a little toward white), optionally only where mask (0..1) is set."""
    m = np.ones(c.img.shape[:2], np.float32) if mask is None else np.clip(mask.astype(np.float32), 0, 1)
    col = np.array(rgb, np.float32)
    hot = col + (255 - col) * core
    out = np.zeros(c.img.shape, np.float32)
    out[..., :3] = col[None, None, :] + (hot - col)[None, None, :] * m[..., None]
    out[..., 3] = m * 255 * strength
    c.glow[...] = np.maximum(c.glow, out)


def hsv_rows(t, sat=0.75, val=1.0):
    """Array of shape t.shape + (3,): the rainbow (red -> purple) along t in 0..1, as 0..255 RGB."""
    h = np.clip(t, 0, 1) * 0.80
    i = np.floor(h * 6).astype(int) % 6
    f = h * 6 - np.floor(h * 6)
    p, q, u = val * (1 - sat), val * (1 - f * sat), val * (1 - (1 - f) * sat)
    r = np.choose(i, [val + 0 * f, q, p + 0 * f, p + 0 * f, u, val + 0 * f])
    g = np.choose(i, [u, val + 0 * f, val + 0 * f, q, p + 0 * f, p + 0 * f])
    b = np.choose(i, [p + 0 * f, p + 0 * f, u, val + 0 * f, val + 0 * f, q])
    return np.stack([r, g, b], axis=-1) * 255.0


def noise2(shape, cell, seed):
    """Smooth value noise in 0..1 (features about `cell` px)."""
    rng = np.random.default_rng(seed)
    h, w = shape
    g = rng.random((max(2, int(h / cell) + 2), max(2, int(w / cell) + 2)), dtype=np.float32)
    return np.clip(np.asarray(Image.fromarray(g).resize((w, h), Image.BICUBIC)), 0, 1)


def arc_path(cx, cy, r_of_theta, theta0, theta1, n, z=0.0):
    """Points of a planar arc (xy plane) between two angles in degrees (0 = +x); r_of_theta(deg) gives the radius at each."""
    out = []
    for i in range(n + 1):
        th = theta0 + (theta1 - theta0) * i / n
        r = r_of_theta(th)
        out.append((cx + r * math.cos(math.radians(th)), cy + r * math.sin(math.radians(th)), z))
    return out


def hermite(ctrl):
    """A smooth function f(s) through control points [(s, value)] (s increasing): cubic Hermite with finite-difference tangents (a Catmull-Rom
    for uneven spacing). Used for the wavy centre lines and width profiles read off the cards."""
    pts = sorted(ctrl)
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    n = len(pts)
    ms = []
    for i in range(n):
        a, b = max(i - 1, 0), min(i + 1, n - 1)
        ms.append((ys[b] - ys[a]) / max(xs[b] - xs[a], 1e-9))

    def f(s):
        if s <= xs[0]:
            return ys[0]
        if s >= xs[-1]:
            return ys[-1]
        i = max(j for j in range(n - 1) if xs[j] <= s)
        h = xs[i + 1] - xs[i]
        t = (s - xs[i]) / h
        h00, h10, h01, h11 = 2 * t ** 3 - 3 * t ** 2 + 1, t ** 3 - 2 * t ** 2 + t, -2 * t ** 3 + 3 * t ** 2, t ** 3 - t ** 2
        return h00 * ys[i] + h10 * h * ms[i] + h01 * ys[i + 1] + h11 * h * ms[i + 1]
    return f


def lerp_table(table):
    """A piecewise-linear function f(s) through [(s, value)]."""
    pts = sorted(table)
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    return lambda s: float(np.interp(s, xs, ys))


def scallop(r, n_lobes=12, depth=0.08, pts=48, squash=1.0):
    """A closed outline (u, v) of a fluted round section: radius r, n_lobes shallow flutes of the given relative depth (a fluted glass hilt)."""
    out = []
    for i in range(pts):
        a = 2 * math.pi * i / pts
        rr = r * (1.0 - depth * 0.5 * (1.0 - math.cos(n_lobes * a)))
        out.append((rr * math.cos(a), rr * math.sin(a) * squash))
    return out


def ellipsoid(part, center, rx, ry, rz, sides=14, stacks=10):
    """An ellipsoid (a head, a haunch, an orb, a bubble) as a lathe round y with radii rx (across), ry (along), rz (through)."""
    prof = [(-ry * math.cos(math.pi * i / stacks), rx * math.sin(math.pi * i / stacks)) for i in range(stacks + 1)]
    return ml.lathe(part, "y", center, prof, sides=sides, squash=rz / max(rx, 1e-6))


def helix(r_of_t, y0, y1, turns, n, phase=0.0, ax=0.0, az=0.0):
    """Points of a helix round the y axis from y0 to y1 (a tail, a cord, a vine): r_of_t(t) gives the radius at t in 0..1."""
    out = []
    for i in range(n + 1):
        t = i / n
        a = phase + 2 * math.pi * turns * t
        r = r_of_t(t)
        out.append((ax + r * math.cos(a), y0 + (y1 - y0) * t, az + r * math.sin(a)))
    return out


def finish(item_id, m, atlas, real_m, fit=False, glow=("FFFFFF", 0.14, 0.55, 1.0), ref=None, views=None, focus=None, icon_id=None):
    """Places the mesh on the sprite, writes the .txt and the two skins, optionally renders the preview beside the card."""
    icon = os.path.join(ICON_DIR, (icon_id or item_id) + ".png")
    if not os.path.exists(icon):
        raise SystemExit("run tools/relics/wave1_icons.py first: " + icon + " is missing")
    m.place_on_sprite(icon, real_m=real_m, fit=fit)
    m.set_glow(*glow)
    os.makedirs(MESH_DIR, exist_ok=True)
    n = m.write_txt(os.path.join(MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment="generated by tools/relics/wave1_models.py")
    atlas.save(item_id)
    st = m.stats(atlas)
    print(f"{item_id}: {len(m.parts)} parts, {n} quads {st['quads']}, length {st['length']:.1f}, atlas {atlas.size}px at {atlas.ppu:.1f} px/cm")
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        kw = {}
        if views:
            kw["views"] = views
        if focus:
            kw["focus"] = focus
        ml.preview(m, ref, os.path.join(prev, item_id + ".png"), atlas=atlas, normals="vertex", **kw)
    return n


def reference(file, box, out_name):
    """Crops the card art for the preview (a clean crop, no grid); returns its path or None when the card images are not around."""
    src = os.path.join(ZIP, file)
    prev = os.environ.get("W1_PREVIEW")
    if not prev or not os.path.exists(src):
        return None
    os.makedirs(prev, exist_ok=True)
    out = os.path.join(prev, out_name + "_ref.png")
    Image.open(src).convert("RGBA").crop(box).save(out)
    return out


MODELS = ["staff_of_moonlight", "liquid_sword", "marids_valor", "catstaff", "rainbow_rapier", "astralis_the_eye_of_the_universe", "deliverance",
          "wand_of_bubbles", "viola_of_moonlit_spirits", "book_of_mispells"]
MODULES = {"staff_of_moonlight": "wave1_m_staff", "liquid_sword": "wave1_m_liquid", "marids_valor": "wave1_m_marid", "catstaff": "wave1_m_catstaff",
           "rainbow_rapier": "wave1_m_rapier", "astralis_the_eye_of_the_universe": "wave1_m_astralis", "deliverance": "wave1_m_deliverance",
           "wand_of_bubbles": "wave1_m_wand", "viola_of_moonlit_spirits": "wave1_m_viola", "book_of_mispells": "wave1_m_book"}


def main(only=None):
    for item_id in MODELS:
        if only and item_id not in only:
            continue
        importlib.import_module(MODULES[item_id]).build()


if __name__ == "__main__":
    main(sys.argv[1:])
