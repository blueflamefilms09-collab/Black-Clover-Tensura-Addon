"""Before / after contact sheets of the God melee (a) items (software rasteriser of tools/relics/preview_mesh.py, no game).

    python3 -B tools/visual_upgrade/god_melee_a/preview.py [ids...]          writes docs/previews/visual_upgrade/god_melee_a/<id>.png

One sheet per item, two rows. Top row = BEFORE (the shipped mesh): held view (the +z face), three-quarter, side (from +x). Bottom row = AFTER (the upgraded mesh): held
view, three-quarter, side, a close-up of the detail (the head or the hilt), the glow pass alone. Orbiting groups (layers 3 and up) are drawn at their phase 0.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import common as C                                                     # noqa: E402
import preview_mesh as pm                                              # noqa: E402

PANEL = (260, 470)


def rot_axis(axis, deg):
    a = np.array(axis, float)
    a /= np.linalg.norm(a)
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    x, y, z = a
    return np.array([[c + x * x * (1 - c), x * y * (1 - c) - z * s, x * z * (1 - c) + y * s],
                     [y * x * (1 - c) + z * s, c + y * y * (1 - c), y * z * (1 - c) - x * s],
                     [z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, c + z * z * (1 - c)]])


def load(mesh_txt, phase=0.0):
    """-> (quads [(layer, P, UV, N, tint)], glow cfg). Orbit groups (layer >= 3) are drawn as body quads turned to `phase` ticks."""
    d = pm.parse_game_txt(mesh_txt)
    orbits = {}
    for line in open(mesh_txt, encoding="utf-8"):
        v = line.split()
        if v and v[0] == "orbit":
            # orbit <layer> <px> <py> <pz> <ax> <ay> <az> <degPerTick> <phaseDeg> <glow 0|1>
            orbits[int(v[1])] = (np.array([float(x) for x in v[2:5]]), [float(x) for x in v[5:8]], float(v[8]), float(v[9]))
    out = []
    for (ly, P, UV, N) in d["quads"]:
        if ly >= 3 and ly in orbits:
            piv, ax, speed, ph = orbits[ly]
            R = rot_axis(ax, ph + speed * phase)
            P = (P - piv) @ R.T + piv
            N = N @ R.T
            out.append((0, P, UV, N, (.7, .7, .7)))
            out.append((2, P, UV, N, (.7, .7, .7)))
        else:
            out.append((ly, P, UV, N, (.7, .7, .7)))
    return out, d["glow"]


def textures(item_id, up, lod=False):
    if up:
        sfx = "_lod" if lod else ""
        sk = os.path.join(C.UP_SKIN_DIR, item_id + sfx + ".png")
        gl = os.path.join(C.UP_SKIN_DIR, item_id + sfx + "_glow.png")
    else:
        sk = os.path.join(C.SKIN_DIR, item_id + ".png")
        gl = os.path.join(C.SKIN_DIR, item_id + "_glow.png")
    return sk, gl


def _fit(pts, yaw, pitch, panel):
    Q = pts @ pm._rot(yaw, pitch).T
    w, h = Q[:, 0].max() - Q[:, 0].min(), Q[:, 1].max() - Q[:, 1].min()
    c = ((Q[:, 0].max() + Q[:, 0].min()) / 2, (Q[:, 1].max() + Q[:, 1].min()) / 2)
    return min((panel[0] - 20) / max(w, 1e-6), (panel[1] - 60) / max(h, 1e-6)), c


def views(item_id, up, scale_ref=None, focus=None, glow_only=False, which=("profile", "three_quarter", "edge"), glow_name="_glow", lod=False):
    path = C.mesh_path(item_id + ("_lod" if lod else ""), up)
    quads, glow = load(path)
    sk, gl = textures(item_id, up, lod)
    if glow_name != "_glow":
        gl = os.path.join(C.UP_SKIN_DIR, item_id + glow_name + ".png")
    pts = np.concatenate([q[1] for q in quads if q[0] == 0])
    fit_pts = pts
    if focus is not None:
        sel = pts[(pts[:, 1] >= focus[0]) & (pts[:, 1] <= focus[1])]
        fit_pts = sel if len(sel) else pts
    tiles = []
    common = min(_fit(fit_pts, *pm.VIEWS[v], PANEL)[0] for v in which) if scale_ref is None else scale_ref
    bg = pm.render_view([], 0, 0, PANEL, 1.0, (0, 0), None, None, None, ss=1)
    for v in which:
        yaw, pitch = pm.VIEWS[v]
        _, c = _fit(fit_pts, yaw, pitch, PANEL)
        if glow_only:
            qs = [q for q in quads if q[0] == 2]
            im = pm.render_view(qs, yaw, pitch, PANEL, common, c, None, gl, glow, ss=2, cull=True, vertex_normals=True)
            im = np.clip(im - bg, 0, 1)
        else:
            im = pm.render_view(quads, yaw, pitch, PANEL, common, c, sk, gl if os.path.exists(gl) else None, glow, ss=2, cull=True, vertex_normals=True)
        tiles.append((v + (" (glow)" if glow_only else ""), im))
    return tiles, common


def sheet(item_id):
    rows = []
    t_before, sc = views(item_id, False)
    rows.append([("BEFORE " + n, im) for n, im in t_before])
    fit_a = min(_fit(np.concatenate([q[1] for q in load(C.mesh_path(item_id, True))[0] if q[0] == 0]), *pm.VIEWS[v], PANEL)[0] for v in ("profile", "three_quarter", "edge"))
    sc = min(sc, fit_a)
    t_after, _ = views(item_id, True, scale_ref=sc)
    row = [("AFTER " + n, im) for n, im in t_after]
    quads, _ = load(C.mesh_path(item_id, True))
    pts = np.concatenate([q[1] for q in quads if q[0] == 0])
    ymax = pts[:, 1].max()
    ymin = pts[:, 1].min()
    # close-up: the top fifth of the weapon (a head, a blade, a crown), or the lower fifth for a long haft weapon is the same call for every item
    focus = (ymin + 0.72 * (ymax - ymin), ymax)
    t_close, _ = views(item_id, True, focus=focus, which=("three_quarter",))
    row.append(("AFTER detail", t_close[0][1]))
    t_glow, _ = views(item_id, True, scale_ref=sc, glow_only=True, which=("three_quarter",))
    row.append(("AFTER glow pass", t_glow[0][1]))
    t_lod, _ = views(item_id, True, scale_ref=sc, which=("three_quarter",), lod=True)
    row.append(("AFTER low detail", t_lod[0][1]))
    if os.path.exists(os.path.join(C.UP_SKIN_DIR, item_id + "_veins.png")):
        t_v, _ = views(item_id, True, scale_ref=sc, glow_only=True, which=("three_quarter",), glow_name="_veins")
        row.append(("AFTER veins pass", t_v[0][1]))
    rows.append(row)
    W = max(len(r) for r in rows) * PANEL[0]
    out = Image.new("RGB", (W, PANEL[1] * 2), (20, 20, 28))
    d = ImageDraw.Draw(out)
    for ri, r in enumerate(rows):
        for ci, (name, im) in enumerate(r):
            out.paste(Image.fromarray((np.clip(im, 0, 1) * 255).astype(np.uint8)), (ci * PANEL[0], ri * PANEL[1]))
            d.text((ci * PANEL[0] + 6, ri * PANEL[1] + 4), name, fill=(255, 255, 255))
    os.makedirs(C.PREVIEWS, exist_ok=True)
    p = os.path.join(C.PREVIEWS, item_id + ".jpg")
    out.save(p, quality=88, optimize=True)
    return p


def before_only(item_id, out_dir):
    t, _ = views(item_id, False)
    out = Image.new("RGB", (PANEL[0] * len(t), PANEL[1]))
    for ci, (name, im) in enumerate(t):
        out.paste(Image.fromarray((np.clip(im, 0, 1) * 255).astype(np.uint8)), (ci * PANEL[0], 0))
    os.makedirs(out_dir, exist_ok=True)
    p = os.path.join(out_dir, item_id + "_before.png")
    out.save(p)
    return p


if __name__ == "__main__":
    ids = sys.argv[1:] or C.IDS
    for i in ids:
        if os.path.exists(C.mesh_path(i, True)):
            print(sheet(i))
        else:
            print(before_only(i, os.environ.get("PV_OUT", C.PREVIEWS)))
