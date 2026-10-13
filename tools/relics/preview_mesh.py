"""Software preview of a baked weapon mesh, beside its reference image (no game, no GPU).

    python tools/relics/preview_mesh.py <item id | path/to/mesh.txt> [--ref card.png] [--out out.png] [--views profile,three_quarter,edge,top]

Also used by mesh_lib.preview(). An orthographic z-buffered rasteriser with the game's rules: counter-clockwise quads face the viewer and back
faces are culled (a wrongly wound part shows as a hole), the skin is sampled nearest-texel with cutout alpha, the glow layer is added on top,
the void layer is a crimson star field. With a bare Mesh (no skin yet) each part is tinted by its material. Views: profile (looking at the +z
face, the view of the card art), three_quarter, edge (from +x), top (looking down the y axis at the tip, auto-zoomed), back (from -z).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

VIEWS = {"profile": (0, 0), "three_quarter": (35, 22), "edge": (-90, 0), "top": (0, 90), "back": (180, 0)}


# ================================================================ the game's file format, parsed the way WeaponRenderer.parse does
def parse_game_txt(path):
    """Returns dict(place=(ox, oy, angle, scale, ymin), glow=(rgb, speed, min, max) | None, quads=[(layer, P 4x3, UV 4x2, N 4x3)]).
    "q" lines carry one normal per quad (copied to the four vertices, exactly as the renderer does); "qn" lines carry one per vertex."""
    place, glow, quads = None, None, []
    for line in open(path):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "place":
            place = tuple(float(x) for x in v[1:6])
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "q":
            nx, ny, nz = float(v[22]), float(v[23]), float(v[24])
            P, UV = [], []
            for k in range(4):
                o = 2 + k * 5
                P.append([float(v[o]), float(v[o + 1]), float(v[o + 2])])
                UV.append([float(v[o + 3]), float(v[o + 4])])
            quads.append((int(v[1]), np.array(P), np.array(UV), np.tile([nx, ny, nz], (4, 1))))
        elif v[0] == "qn":
            P, UV, N = [], [], []
            for k in range(4):
                o = 2 + k * 8
                P.append([float(v[o]), float(v[o + 1]), float(v[o + 2])])
                UV.append([float(v[o + 3]), float(v[o + 4])])
                N.append([float(v[o + 5]), float(v[o + 6]), float(v[o + 7])])
            quads.append((int(v[1]), np.array(P), np.array(UV), np.array(N)))
    return {"place": place, "glow": glow, "quads": quads}


# ================================================================ camera
def _rot(yaw, pitch):
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return Rx @ Ry


def _stars(w, h):
    rng = np.random.default_rng(7)
    s = np.zeros((h, w, 3), np.float32) + [0.05, 0, 0.02]
    for _ in range(max(40, w * h // 450)):
        x, y = rng.integers(0, w), rng.integers(0, h)
        s[y, x] += np.array([rng.uniform(.5, 1), rng.uniform(.02, .15), rng.uniform(.05, .25)]) * rng.uniform(.3, 1)
    return np.clip(s, 0, 1)


L1 = np.array([0.35, 0.8, 0.6])
L1 = L1 / np.linalg.norm(L1)
L2 = np.array([-0.5, 0.2, 0.5])
L2 = L2 / np.linalg.norm(L2)


def _shade(n):
    return 0.42 + 0.42 * np.clip(n @ L1, 0, 1) + 0.2 * np.clip(n @ L2, 0, 1)


# ================================================================ rasteriser
def _raster(img, zb, P, UV, N, tex, tint, kind, stars, cull_info=None):
    """P: 3x3 (x, y, depth) in pixels; UV 3x2 texels; N 3x3 view-space normals; tex: float RGBA (th, tw, 4) 0..1 or None."""
    H, W = zb.shape
    x0, x1 = int(max(0, math.floor(P[:, 0].min()))), int(min(W - 1, math.ceil(P[:, 0].max())))
    y0, y1 = int(max(0, math.floor(P[:, 1].min()))), int(min(H - 1, math.ceil(P[:, 1].max())))
    if x0 > x1 or y0 > y1:
        return
    yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1].astype(np.float32) + 0.5
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = P
    den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
    if abs(den) < 1e-9:
        return
    w0 = ((by - cy) * (xx - cx) + (cx - bx) * (yy - cy)) / den
    w1 = ((cy - ay) * (xx - cx) + (ax - cx) * (yy - cy)) / den
    w2 = 1 - w0 - w1
    inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
    if not inside.any():
        return
    z = w0 * az + w1 * bz + w2 * cz
    sub, reg = zb[y0:y1 + 1, x0:x1 + 1], img[y0:y1 + 1, x0:x1 + 1]
    if kind == "void":
        m = inside & (z > sub + 1e-4)
        reg[m] = stars[y0:y1 + 1, x0:x1 + 1][m]
        sub[m] = z[m]
        return
    if tex is not None:
        th, tw = tex.shape[:2]
        u = w0 * UV[0, 0] + w1 * UV[1, 0] + w2 * UV[2, 0]
        v = w0 * UV[0, 1] + w1 * UV[1, 1] + w2 * UV[2, 1]
        s = tex[np.clip(v.astype(int), 0, th - 1), np.clip(u.astype(int), 0, tw - 1)]
    else:
        s = None
    if kind == "body":
        n = (w0[..., None] * N[0] + w1[..., None] * N[1] + w2[..., None] * N[2])
        nl = np.linalg.norm(n, axis=-1, keepdims=True)
        sh = _shade(n / np.maximum(nl, 1e-6))[..., None]
        m = inside & (z > sub + 1e-4)
        if s is not None:
            m &= s[..., 3] > 0.1
            col = s[..., :3] * sh
        else:
            col = np.broadcast_to(tint, sh.shape[:2] + (3,)) * sh
        reg[m] = col[m]
        sub[m] = z[m]
    else:                                                                     # glow: additive on what is nearly at the same depth
        if s is None:
            return
        m = inside & (z > sub - 0.6)
        reg[m] += (s[..., :3] * s[..., 3:4] * tint)[m]


MAT_TINT = {"steel": (.62, .65, .72), "iron": (.3, .32, .38), "silver": (.78, .8, .85), "gold": (.85, .68, .28), "bronze": (.62, .42, .24),
            "obsidian": (.14, .12, .2), "stone": (.45, .44, .47), "wood": (.45, .3, .17), "bone": (.88, .83, .7), "crystal": (.4, .75, .9),
            "leather": (.38, .25, .15), "cloth": (.6, .16, .2)}


def _tex(x):
    if x is None:
        return None
    if isinstance(x, np.ndarray):
        return np.clip(x / 255.0, 0, 1).astype(np.float32)
    return np.asarray(Image.open(x).convert("RGBA"), np.float32) / 255


def _quads_of(mesh, atlas, normals):
    """-> (quads [(layer, P, UV, N, tint)], glow cfg, skin, glow texture)."""
    if isinstance(mesh, str):
        path = mesh if mesh.endswith(".txt") else os.path.join(ASSETS, "weapon_meshes", mesh + ".txt")
        d = parse_game_txt(path)
        item = os.path.splitext(os.path.basename(path))[0]
        td = os.path.join(ASSETS, "textures", "entity", "weapon")
        skin = os.path.join(td, item + ".png")
        gl = os.path.join(td, item + "_glow.png")
        return ([(l, P, UV, N, (.7, .7, .7)) for (l, P, UV, N) in d["quads"]], d["glow"],
                skin if os.path.exists(skin) else None, gl if os.path.exists(gl) else None, 128.0)
    qs = [(l, P, UV, N, MAT_TINT.get(p.material, (.7, .7, .7)) if p.base is None else tuple(c / 255 for c in p.base))
          for (l, P, UV, N, p) in mesh.iter_quads(normals, atlas)]
    gc = mesh.glow_cfg
    glow = (int(gc[0], 16), gc[1], gc[2], gc[3]) if gc else (0xFFFFFF, 0.12, 0.35, 1.0)
    return qs, glow, (atlas.img if atlas is not None else None), (atlas.glow if atlas is not None and atlas.glow[..., 3].max() > 0 else None), 128.0


def render_view(quads, yaw, pitch, panel, scale, center, skin, glow_tex, glow, ss=2, cull=True, vertex_normals=True, textured=True):
    """One orthographic view -> float RGB image (panel h, w)."""
    pw, ph = panel
    W, H = pw * ss, ph * ss
    R = _rot(yaw, pitch)
    img = np.zeros((H, W, 3), np.float32) + np.array([0.30, 0.32, 0.38], np.float32)
    img *= (0.8 + 0.4 * np.linspace(1, 0, H, dtype=np.float32))[:, None, None]
    zb = np.full((H, W), -1e9, np.float32)
    stars = _stars(W, H)
    sk, gt = (_tex(skin), _tex(glow_tex)) if textured else (None, None)
    tint = np.array([(glow[0] >> 16) & 255, (glow[0] >> 8) & 255, glow[0] & 255], np.float32) / 255 * glow[3] if glow else np.ones(3, np.float32)
    k = scale * ss

    def proj(P):
        Q = P @ R.T
        return np.stack([W / 2 + (Q[:, 0] - center[0]) * k, H / 2 - (Q[:, 1] - center[1]) * k, Q[:, 2] * k], axis=1)

    for layer in (0, 1, 2):
        for (ly, P, UV, N, tn) in quads:
            if ly != layer:
                continue
            S = proj(P)
            Nv = N @ R.T
            if not vertex_normals:
                Nv = np.tile(Nv[0], (4, 1))
            for tri in ((0, 1, 2), (0, 2, 3)):
                a = (S[tri[1], 0] - S[tri[0], 0]) * (S[tri[2], 1] - S[tri[0], 1]) - (S[tri[2], 0] - S[tri[0], 0]) * (S[tri[1], 1] - S[tri[0], 1])
                if abs(a) < 1e-9:
                    continue
                if cull and layer != 1 and a > 0:                     # screen y runs down: counter-clockwise on screen has a negative area
                    continue
                uv = UV[list(tri)] * (sk.shape[1] / 128.0 if (layer == 0 and sk is not None) else (gt.shape[1] / 128.0 if (layer == 2 and gt is not None) else 1.0))
                if layer == 0:
                    _raster(img, zb, S[list(tri)], uv, Nv[list(tri)], sk, np.array(tn, np.float32), "body", stars)
                elif layer == 1:
                    _raster(img, zb, S[list(tri)], uv, Nv[list(tri)], None, None, "void", stars)
                else:
                    _raster(img, zb, S[list(tri)], uv, Nv[list(tri)], gt, tint, "glow", stars)
    img = np.clip(img, 0, 1)
    if ss > 1:
        img = img.reshape(ph, ss, pw, ss, 3).mean(axis=(1, 3))
    return img


def render_preview(mesh, reference_png=None, out_png="preview.png", atlas=None, views=("profile", "three_quarter", "edge", "top"), panel=None,
                   ss=2, cull=True, normals="quad", textured=True, label=True, focus=None):
    """mesh: a mesh_lib.Mesh, or an item id / path of a .txt in the game format. Writes out_png and returns it.
    normals 'quad' shows what the current renderer does (one normal per quad), 'vertex' the smoothed per-vertex normals.
    focus = (y0, y1): zoom every view on that stretch of the weapon's axis (a close-up of the guard, the head ...); the rest is still drawn."""
    quads, glow, skin, glow_tex, _ = _quads_of(mesh, atlas, normals)
    if not quads:
        raise ValueError("empty mesh")
    allp = np.concatenate([q[1] for q in quads if q[0] == 0] or [q[1] for q in quads])
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    ext = hi - lo
    if panel is None:
        long_side = float(max(ext))
        panel = (300, 540) if ext[1] >= 0.8 * long_side else (420, 420)
    if focus is not None:
        fp = allp[(allp[:, 1] >= focus[0]) & (allp[:, 1] <= focus[1])]
        allp_fit = fp if len(fp) else allp
        panel = panel or (420, 420)
    else:
        allp_fit = allp
    pw, ph = panel
    tiles = []
    if reference_png:
        ref = Image.open(reference_png).convert("RGBA")
        bg = Image.new("RGBA", ref.size, (84, 89, 107, 255))
        ref = Image.alpha_composite(bg, ref).convert("RGB")
        sc = min(pw * 1.6 / ref.width, ph / ref.height)
        ref = ref.resize((max(1, int(ref.width * sc)), max(1, int(ref.height * sc))), Image.LANCZOS)
        t = Image.new("RGB", (int(pw * 1.6), ph), (84, 89, 107))
        t.paste(ref, ((t.width - ref.width) // 2, (ph - ref.height) // 2))
        tiles.append(("reference", np.asarray(t, np.float32) / 255))
    # one common scale for the side views (true proportions), the top view zoomed on its own
    def fit(yaw, pitch):
        Q = allp_fit @ _rot(yaw, pitch).T
        w, h = Q[:, 0].max() - Q[:, 0].min(), Q[:, 1].max() - Q[:, 1].min()
        c = ((Q[:, 0].max() + Q[:, 0].min()) / 2, (Q[:, 1].max() + Q[:, 1].min()) / 2)
        return min((pw - 24) / max(w, 1e-6), (ph - 36) / max(h, 1e-6)), c
    common = min(fit(*VIEWS[v])[0] for v in views if v != "top") if any(v != "top" for v in views) else 1.0
    for v in views:
        yaw, pitch = VIEWS[v]
        sc, c = fit(yaw, pitch)
        use = sc if v == "top" else common
        im = render_view(quads, yaw, pitch, (pw, ph), use, c, skin, glow_tex, glow, ss, cull, normals == "vertex", textured)
        tiles.append((v, im))
    out = Image.fromarray((np.concatenate([t[1] for t in tiles], axis=1) * 255).astype(np.uint8))
    if label:
        d = ImageDraw.Draw(out)
        x = 0
        for (name, im) in tiles:
            d.text((x + 6, 4), name, fill=(255, 255, 255))
            x += im.shape[1]
    os.makedirs(os.path.dirname(os.path.abspath(out_png)), exist_ok=True)
    out.save(out_png)
    return out_png


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("mesh")
    ap.add_argument("--ref")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "relics_preview", "mesh.png"))
    ap.add_argument("--views", default="profile,three_quarter,edge,top")
    ap.add_argument("--normals", default="quad")
    ap.add_argument("--focus", help="y0,y1: zoom on that stretch of the axis")
    a = ap.parse_args(argv)
    focus = tuple(float(x) for x in a.focus.split(",")) if a.focus else None
    print("wrote", render_preview(a.mesh, a.ref, a.out, views=tuple(a.views.split(",")), normals=a.normals, focus=focus))


if __name__ == "__main__":
    main(sys.argv[1:])
