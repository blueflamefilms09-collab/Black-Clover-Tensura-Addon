#!/usr/bin/env python3
"""A frame strip of a weapon's swing from the software renderer (tools/relics/preview_mesh.py), driven by the exact maths of the game (swing_math.py).

    python3 -B tools/weaponmotion/swing_strip.py vassal_chain vassal_whip --out docs/previews/weapon_motion/swing_strip_a.png [--cell 190x330]

One row per mesh, eight frames of a strike (the arm swings 0.3 s, the weapon lags, whips past and rings out). The weapon is turned about its grip by the arm's angle
(the integral of the drive), the bones bend on top of it. Rigid reference: the first frame. Not the game's camera: a side view of the mesh's own plane.
"""
import argparse
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "relics"))
import swing_math as SM  # noqa: E402
import preview_mesh as pm  # noqa: E402

START, DUR = 0.12, 0.30
FRAMES = (0.0, START + 0.075, START + 0.15, START + 0.225, START + 0.30, 0.55, 0.68, 0.90)


def arm_angle(t):
    """Integral of PEAK_OMEGA sin(pi u) dt over the strike: the arm's turn (rad), positive = the tip swings forward."""
    if t <= START:
        return 0.0
    u = min(1.0, (t - START) / DUR)
    a = SM.PEAK_OMEGA * DUR / math.pi * (1.0 - math.cos(math.pi * u))
    if t > START + DUR + 0.12:                                   # preview only: the arm glides back (the game's recovery is Better Combat's own animation, not simulated)
        k = min(1.0, (t - START - DUR - 0.12) / 0.4)
        a *= 1.0 - k * k * (3 - 2 * k)
    return a


def simulate(rig, fps=60):
    """State at every frame time (the same step the game does once per drawn frame)."""
    w, zeta, gain, mx = rig.params()
    th, thv = [0.0] * rig.n, [0.0] * rig.n
    dt = 1.0 / fps
    res = {}
    want = {int(round(t * fps)): t for t in FRAMES}
    for f in range(0, int(round(max(FRAMES) * fps)) + 1):
        t = f * dt
        u = (t - START) / DUR
        SM.step(th, thv, rig.n, w, zeta, gain, mx, SM.arm_accel(u, 1.0 / DUR), dt)
        if f in want:
            res[want[f]] = (list(th), list(thv))
    return res


def frame_quads(mesh_id, rig, state, t, pivot, rigid=False):
    d = pm.parse_game_txt(os.path.join(pm.ASSETS, "weapon_meshes", mesh_id + ".txt"))
    quads = d["quads"]
    th, thv = state
    if rigid:
        th, thv = [0.0] * rig.n, [0.0] * rig.n
    mats = rig.matrices(th, thv, None, t, 0.0, 0.0)
    ang = arm_angle(t)
    Rarm = SM.trans(pivot) @ SM.rot((0, 0, 1), ang) @ SM.trans(-np.array(pivot))
    out = []
    by_layer = {0: [], 1: [], 2: []}
    for q in quads:
        by_layer[q[0]].append(q)
    for layer in (0, 1, 2):
        qs = by_layer[layer]
        if not qs:
            continue
        P = np.concatenate([q[1] for q in qs])
        N = np.concatenate([q[3] for q in qs])
        Pn = rig.skin_points(layer, P, mats)
        # normals: the rotation part of the same blend (cheap and close enough for a preview)
        Nn = N.copy()
        if layer in rig.skin:
            frm, codes = rig.skin[layer]
            for k, code in enumerate(codes):
                b, wq = code >> 6, code & 63
                n0 = mats[b][:3, :3] @ N[frm + k]
                if wq:
                    n1 = mats[b + 1][:3, :3] @ N[frm + k]
                    n0 = n0 + (n1 - n0) * (wq / 63.0)
                Nn[frm + k] = n0
        Pn = (Rarm[:3, :3] @ Pn.T).T + Rarm[:3, 3]
        Nn = (Rarm[:3, :3] @ Nn.T).T
        Nn /= np.maximum(np.linalg.norm(Nn, axis=1, keepdims=True), 1e-9)
        for i, q in enumerate(qs):
            out.append((layer, Pn[i * 4:i * 4 + 4], q[2], Nn[i * 4:i * 4 + 4], (.7, .7, .7)))
    return out, d["glow"]


def strip(mesh_id, cell):
    rig = SM.Rig(os.path.join(pm.ASSETS, "weapon_meshes", mesh_id + ".txt"))
    if rig.n == 0:
        raise SystemExit(mesh_id + ": no swing rig (run tools/weaponmotion/build_swing.py)")
    sim = simulate(rig)
    pivot = (0.0, 22.0, 0.0)
    frames = [frame_quads(mesh_id, rig, sim[t], t, pivot) for t in FRAMES]
    pts = np.concatenate([q[1] for fr, _g in frames for q in fr if q[0] == 0])
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    c = ((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2)
    pw, ph = cell
    scale = min((pw - 12) / max(hi[0] - lo[0], 1e-6), (ph - 28) / max(hi[1] - lo[1], 1e-6))
    td = os.path.join(pm.ASSETS, "textures", "entity", "weapon")
    skin, gl = os.path.join(td, mesh_id + ".png"), os.path.join(td, mesh_id + "_glow.png")
    tiles = []
    for t, (qs, glow) in zip(FRAMES, frames):
        im = pm.render_view(qs, 0, 0, cell, scale, c, skin if os.path.exists(skin) else None, gl if os.path.exists(gl) else None, glow, 2, True, True, True)
        tiles.append(Image.fromarray((im * 255).astype(np.uint8)))
        ImageDraw.Draw(tiles[-1]).text((5, 3), "t=%.2fs" % t, fill=(255, 255, 255))
    row = Image.new("RGB", (pw * len(tiles), ph))
    for i, t in enumerate(tiles):
        row.paste(t, (i * pw, 0))
    ImageDraw.Draw(row).text((5, ph - 14), mesh_id, fill=(255, 235, 140))
    return row


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("ids", nargs="+")
    ap.add_argument("--out", required=True)
    ap.add_argument("--cell", default="190x330")
    a = ap.parse_args(argv)
    cw, ch = (int(x) for x in a.cell.split("x"))
    rows = [strip(i, (cw, ch)) for i in a.ids]
    sheet = Image.new("RGB", (rows[0].width, sum(r.height for r in rows)))
    y = 0
    for r in rows:
        sheet.paste(r, (0, y))
        y += r.height
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    sheet.save(a.out, optimize=True)
    print("wrote", a.out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
