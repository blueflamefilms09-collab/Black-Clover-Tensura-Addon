"""Software previews of the upgrade: before / after contact sheets (held view, side view, front, close-up of detail, glow pass), no game, no GPU.

    python3 -B tools/visual_upgrade/genesis_weapons/vu_preview.py blackrazor [more ids]

The rasteriser is tools/relics/preview_mesh.py's (z-buffered, back faces culled like the game, nearest-texel skins, additive glow). The upgrade adds the layers it
draws: the veins (3) and halo cards (4) additive with their group tint, and the group transforms at a chosen time. This is NOT the game: it shows geometry,
skins and the emissive layers, not the shaders, the animation or the particles.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import preview_mesh as pm                                                 # noqa: E402
from common import *                                                      # noqa: E402,F401,F403


def tex(path):
    return np.asarray(Image.open(path).convert("RGBA"), np.float32) / 255.0 if os.path.exists(path) else None


def hex_rgb(h):
    h = h.lstrip("#")
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], np.float32) / 255.0


def group_matrix(g, t):
    """The rotation (3x3) and offset of a group at time t (ticks) for the preview: spin and sway only (the preview is a still)."""
    R = np.eye(3)
    off = np.zeros(3)
    for a in g.get("anim", []):
        k = a.get("kind")
        if k == "spin":
            ax = np.array(a["axis"], float)
            ax = ax / max(np.linalg.norm(ax), 1e-9)
            ang = math.radians(a.get("dps", 30.0) * t / 20.0 + a.get("phase", 0.0))
            K = np.array([[0, -ax[2], ax[1]], [ax[2], 0, -ax[0]], [-ax[1], ax[0], 0]])
            R = (np.eye(3) + math.sin(ang) * K + (1 - math.cos(ang)) * K @ K) @ R
        elif k == "bob":
            off += np.array(a.get("axis", [0, 1, 0]), float) * a.get("amp", 1.0) * math.sin(2 * math.pi * (t / a.get("period", 80.0) + a.get("phase", 0.0)))
    return R, off


def place_matrix(place, extra_yaw=0.0):
    ang = math.radians(place[2] - 90.0)
    return np.array([[math.cos(ang), -math.sin(ang), 0], [math.sin(ang), math.cos(ang), 0], [0, 0, 1]])


def load(mesh_id, base_dir, tex_dir, spec=None, t=0.0, lod=False):
    d = parse_mesh(os.path.join(base_dir, mesh_id + ".txt"))
    skin_name = d["skin"] or mesh_id
    T = {"skin": tex(os.path.join(tex_dir, skin_name + ".png")), "glow": tex(os.path.join(tex_dir, skin_name + "_glow.png")),
         "veins": tex(os.path.join(tex_dir, skin_name + "_veins.png")), "halo": tex(os.path.join(tex_dir, "halo.png"))}
    groups = (spec or {}).get("groups", {})
    quads = []
    for (ly, g, P, UV, N) in d["quads"]:
        P = P.copy()
        N = N.copy()
        if g and g in groups and not lod:
            R, off = group_matrix(groups[g], t)
            piv = np.array(groups[g].get("pivot", [0, 0, 0]), float)
            P = (P - piv) @ R.T + piv + off
            N = N @ R.T
        quads.append((ly, g, P, UV, N))
    return d, quads, T, groups


def render(quads, T, glow_cfg, groups, yaw, pitch, panel, scale, center, ss=2, layers=(0, 1, 2, 3, 4), body_visible=True, bg=None, old_style=False):
    pw, ph = panel
    W, H = pw * ss, ph * ss
    R = pm._rot(yaw, pitch)
    img = np.zeros((H, W, 3), np.float32) + np.array(bg if bg is not None else [0.30, 0.32, 0.38], np.float32)
    if bg is None:
        img *= (0.8 + 0.4 * np.linspace(1, 0, H, dtype=np.float32))[:, None, None]
    zb = np.full((H, W), -1e9, np.float32)
    stars = pm._stars(W, H)
    k = scale * ss
    glow_tint = np.array([(glow_cfg[0] >> 16) & 255, (glow_cfg[0] >> 8) & 255, glow_cfg[0] & 255], np.float32) / 255 * glow_cfg[3] if glow_cfg else np.ones(3, np.float32)

    def proj(P):
        Q = P @ R.T
        return np.stack([W / 2 + (Q[:, 0] - center[0]) * k, H / 2 - (Q[:, 1] - center[1]) * k, Q[:, 2] * k], axis=1)

    order = [0, 1, 2, 3, 4]
    for layer in order:
        if layer not in layers:
            continue
        for (ly, g, P, UV, N) in quads:
            if ly != layer:
                continue
            S = proj(P)
            Nv = N @ R.T
            t_ = T["skin"] if layer == 0 else T["glow"] if layer == 2 else T["veins"] if layer == 3 else T["halo"] if layer == 4 else None
            if layer in (0, 2, 3, 4) and t_ is None:
                continue
            uvk = (t_.shape[1] / 128.0) if t_ is not None else 1.0
            if layer == 0:
                tint = None
            elif layer == 2:
                tint = glow_tint
            elif layer == 3:
                tint = glow_tint * 0.9
            elif layer == 4:
                gt = groups.get(g, {}).get("tint", "FFFFFF") if not isinstance(groups.get(g, {}).get("tint"), dict) else groups[g]["tint"].get("rgb", "FFFFFF")
                tint = hex_rgb(gt) * groups.get(g, {}).get("alpha", 0.8)
            else:
                tint = None
            for tri in ((0, 1, 2), (0, 2, 3)):
                a = (S[tri[1], 0] - S[tri[0], 0]) * (S[tri[2], 1] - S[tri[0], 1]) - (S[tri[2], 0] - S[tri[0], 0]) * (S[tri[1], 1] - S[tri[0], 1])
                if abs(a) < 1e-9:
                    continue
                if layer != 1 and a > 0:
                    continue
                uv = UV[list(tri)] * uvk
                if layer == 0:
                    if body_visible:
                        pm._raster(img, zb, S[list(tri)], uv, Nv[list(tri)], t_, np.array([0.7, 0.7, 0.7], np.float32), "body", stars)
                    else:
                        pm._raster(img, zb, S[list(tri)], uv, Nv[list(tri)], np.zeros((2, 2, 4), np.float32) + 1e-3, np.array([0.05, 0.05, 0.05], np.float32), "body", stars)
                elif layer == 1:
                    pm._raster(img, zb, S[list(tri)], uv, Nv[list(tri)], None, None, "void", stars)
                else:
                    pm._raster(img, zb, S[list(tri)], uv, Nv[list(tri)], t_, tint, "glow", stars)
    img = np.clip(img, 0, 1)
    if ss > 1:
        img = img.reshape(ph, ss, pw, ss, 3).mean(axis=(1, 3))
    return img


def fit(allp, yaw, pitch, panel, focus=None):
    pw, ph = panel
    pts = allp
    if focus is not None:
        sel = allp[(allp[:, 1] >= focus[0]) & (allp[:, 1] <= focus[1])]
        pts = sel if len(sel) else allp
    Q = pts @ pm._rot(yaw, pitch).T
    w, h = Q[:, 0].max() - Q[:, 0].min(), Q[:, 1].max() - Q[:, 1].min()
    c = ((Q[:, 0].max() + Q[:, 0].min()) / 2, (Q[:, 1].max() + Q[:, 1].min()) / 2)
    return min((pw - 24) / max(w, 1e-6), (ph - 36) / max(h, 1e-6)), c


def views_for(quads, T, glow_cfg, groups, panel, focus, ss=2, held_angle=None, layers=(0, 1, 2, 3, 4)):
    body = np.concatenate([q[2] for q in quads if q[0] == 0])
    tiles = []
    specs = [("held", 28.0, 14.0, None), ("side", -90.0, 0.0, None), ("front", 0.0, 0.0, None), ("detail", 24.0, 12.0, focus)]
    sc_common = None
    for name, yaw, pitch, fc in specs:
        sc, c = fit(body, yaw, pitch, panel, fc)
        if fc is None:
            sc_common = sc if sc_common is None else min(sc_common, sc)
    for name, yaw, pitch, fc in specs:
        sc, c = fit(body, yaw, pitch, panel, fc)
        use = sc if fc is not None else sc_common if name in ("side", "front") else sc
        tiles.append((name, render(quads, T, glow_cfg, groups, yaw, pitch, panel, use, c, ss, layers)))
    return tiles


def glow_only(quads, T, glow_cfg, groups, panel, ss=2):
    body = np.concatenate([q[2] for q in quads if q[0] == 0])
    sc, c = fit(body, 20.0, 10.0, panel)
    return render(quads, T, glow_cfg, groups, 20.0, 10.0, panel, sc, c, ss, layers=(0, 2, 3, 4), body_visible=False, bg=[0.02, 0.02, 0.04])


def sheet(item_id, out_path, panel=(300, 540), focus=None, ss=2, t=0.0, quiet=False):
    spec_path = os.path.join(SPEC_DIR, item_id + ".json")
    spec = json.load(open(spec_path)) if os.path.exists(spec_path) else {}
    rows = []
    # before
    d0, q0, T0, g0 = load(item_id, OLD_MESH, OLD_TEX)
    for ly in (0,):
        pass
    old_tiles = views_for(q0, T0, d0["glow"], g0, panel, focus, ss, layers=(0, 1, 2))
    rows.append(("before: " + NAMES.get(item_id, item_id), old_tiles))
    d1, q1, T1, g1 = load(item_id, VU_MESH, VU_TEX, spec, t)
    new_tiles = views_for(q1, T1, d1["glow"], g1, panel, focus, ss)
    rows.append(("after (static, t=%.0f ticks)" % t, new_tiles))
    gl = glow_only(q1, T1, d1["glow"], g1, panel, ss)
    lines = []
    W = panel[0] * 4
    out = Image.new("RGB", (W, (panel[1] + 18) * 3), (10, 10, 14))
    dr = ImageDraw.Draw(out)
    y = 0
    for title, tiles in rows:
        dr.text((6, y + 3), title, fill=(255, 255, 255))
        for i, (name, im) in enumerate(tiles):
            out.paste(Image.fromarray((im * 255).astype(np.uint8)), (i * panel[0], y + 18))
            dr.text((i * panel[0] + 6, y + 20), name, fill=(255, 255, 255))
        y += panel[1] + 18
    dr.text((6, y + 3), "after: emissive passes only (core glow, veins, halo cards)", fill=(255, 255, 255))
    out.paste(Image.fromarray((gl * 255).astype(np.uint8)), (0, y + 18))
    # lod beside the glow pass
    lod_name = d1["lod"]
    if lod_name and os.path.exists(os.path.join(VU_MESH, lod_name + ".txt")):
        d2, q2, T2, g2 = load(lod_name, VU_MESH, VU_TEX, spec, 0, lod=True)
        body = np.concatenate([q[2] for q in q2 if q[0] == 0])
        sc, c = fit(body, 28.0, 14.0, panel)
        lod_im = render(q2, T2, d2["glow"], g2, 28.0, 14.0, panel, sc, c, ss, layers=(0, 2))
        out.paste(Image.fromarray((lod_im * 255).astype(np.uint8)), (panel[0], y + 18))
        dr.text((panel[0] + 6, y + 20), "LOD mesh (> 12 blocks), %d body quads" % sum(1 for q in q2 if q[0] == 0), fill=(255, 255, 255))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    out.save(out_path)
    return out_path


if __name__ == "__main__":
    import importlib
    ids = [a for a in sys.argv[1:] if not a.startswith("--")]
    for i in ids:
        focus = None
        try:
            mod = importlib.import_module("item_" + i)
            focus = getattr(mod, "FOCUS", None)
        except ImportError:
            pass
        out = os.path.join(PREVIEWS, i + ".png")
        if "--tmp" in sys.argv:
            out = os.path.join("/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad", "gw_prev_" + i + ".png")
        print("wrote", sheet(i, out, focus=focus))
