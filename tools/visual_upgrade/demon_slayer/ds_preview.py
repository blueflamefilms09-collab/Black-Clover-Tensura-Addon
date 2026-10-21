"""Software before / after contact sheet of the Genesis Demon-Slayer (no game, no GPU): docs/previews/visual_upgrade/demon_slayer/demon_slayer_sword.png

    python3 -B tools/visual_upgrade/demon_slayer/ds_preview.py [--tmp]

Row 1: the OLD model (client/DemonSlayerMesh.java dumped by tools/item_preview/MeshDump.java, moved into the mesh frame of the new one) at rest, held / side / front / detail.
Row 2: the NEW model at rest (every animation group in its first pose). Row 3: the new model's emissive passes alone, the new model in the state of Black Meteorite (the
shards drifted apart, the light between them), at Resonance 4 (the bloom grown), and the LOD mesh. The rasteriser is tools/relics/preview_mesh.py's (z-buffered, nearest-texel skins,
additive glow); it shows geometry, skins and emissive layers, NOT the game's shaders, lighting, animation or particles.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ds_common import *                                                     # noqa: E402,F401,F403
import vu_preview as vp                                                     # noqa: E402

PLACE_T = np.array([0.516, 0.523, 0.5])
SCALE = 0.98 / 64.0
YSHIFT = 11.75


def rz(deg):
    a = math.radians(deg)
    return np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])


def old_in_model_frame(res=0, fracture=0.0, time=0.0):
    """The old Demon-Slayer's quads as (layer, group, P, UV in the 128 space, N) in the new mesh's frame (the old pixel frame): layer 0 body, 1 void, 2 bloom glow."""
    classes = build_dump()
    out = []
    R = rz(45.0)
    for (ly, pts, n, _argb) in dump_old(classes, res, fracture, time):
        P = (R @ (pts[:, :3] - PLACE_T).T).T / SCALE + np.array([0.0, YSHIFT, 0.0])
        N = np.tile(R @ n, (4, 1))
        if ly == 0:
            out.append((0, "", P, pts[:, 3:5] * 2.0, N))
        elif ly == 1:
            out.append((1, "", P, np.zeros((4, 2)), N))
        elif ly == 3:
            out.append((2, "", P, pts[:, 3:5] * 128.0, N))
    return out


def tex_old():
    T = {"skin": vp.tex(os.path.join(OLD_TEX, "demon_slayer_sword.png")), "glow": vp.tex(os.path.join(OLD_TEX, "demon_slayer_bloom_glow.png")), "veins": None, "halo": None}
    return T


def pose_groups(quads, groups, t, fracture=0.0, resonance=0):
    """Applies the group animations of the spec to the quads: spin / bob at time t (ticks), fracture and resonance as the renderer would."""
    out = []
    for (ly, g, P, UV, N) in quads:
        spec = groups.get(g)
        if not spec or not g:
            out.append((ly, g, P, UV, N))
            continue
        R = np.eye(3)
        off = np.zeros(3)
        sc = 1.0
        for a in spec.get("anim", []):
            k = a.get("kind")
            if k in ("spin", "bob") and t is not None:
                R2, o2 = vp.group_matrix({"anim": [a]}, t)
                R = R2 @ R
                off += o2
            elif k == "fracture" and fracture > 0:
                ax = np.array(a["axis"], float)
                off += ax * a.get("amp", 0.0) * fracture
                za, xa = a.get("deg", 0.0) * fracture, a.get("rate", 0.0) * fracture
                cz, sz, cx, sx = math.cos(math.radians(za)), math.sin(math.radians(za)), math.cos(math.radians(xa)), math.sin(math.radians(xa))
                R = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]) @ np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]) @ R
            elif k == "resonance":
                sc *= 1.0 + a.get("amp", 0.0) * resonance
        piv = np.array(spec.get("pivot", [0, 0, 0]), float)
        P2 = ((P - piv) @ R.T) * sc + piv + off
        out.append((ly, g, P2, UV, N @ R.T))
    return out


def hide(quads, groups, names):
    return [q for q in quads if q[1] not in names]


def sheet(panel=(260, 470), ss=2, tmp=False):
    spec = json.load(open(os.path.join(SPEC_DIR, ID + ".json")))
    groups = spec["groups"]
    d1, q1, T1, g1 = vp.load(ID, VU_MESH, VU_TEX, spec, 0.0)
    q_rest = pose_groups(q1, groups, None)
    # groups that show only when their state is on: the light between the shards (alpha follows the fracture) and, at rest, nothing of it
    rest_hidden = {"crack_1", "crack_2", "crack_3"}
    q_view = hide(q_rest, groups, rest_hidden)
    body = np.concatenate([q[2] for q in q_view if q[0] == 0])
    q0 = old_in_model_frame()
    T0 = tex_old()
    specs = [("held", 28.0, 14.0, None), ("side", -90.0, 0.0, None), ("front", 0.0, 0.0, None), ("detail: hilt and pommel", 24.0, 12.0, (-30.0, -2.0))]
    cams = []
    sc_common = None
    for name, yaw, pitch, fc in specs:
        sc, c = vp.fit(body, yaw, pitch, panel, fc)
        if fc is None:
            sc_common = sc if sc_common is None else min(sc_common, sc)
        cams.append((name, yaw, pitch, fc, sc, c))
    glow_cfg = (0xFFFFFF, 0.14, 0.4, 1.0)

    def tiles(quads, T, layers):
        out = []
        for (name, yaw, pitch, fc, sc, c) in cams:
            use = sc if fc is not None else sc_common
            out.append((name, vp.render(quads, T, glow_cfg, groups, yaw, pitch, panel, use, c, ss, layers)))
        return out

    old_tiles = tiles(q0, T0, (0, 1, 2))
    new_tiles = tiles(q_view, T1, (0, 1, 2, 3, 4))
    # row 3
    sc, c = vp.fit(body, 20.0, 10.0, panel)
    glow = vp.render(q_view, T1, glow_cfg, groups, 20.0, 10.0, panel, sc, c, ss, layers=(0, 2, 3, 4), body_visible=False, bg=[0.02, 0.02, 0.04])
    q_frac = pose_groups(q1, groups, None, fracture=1.0)
    frac = vp.render(q_frac, T1, glow_cfg, groups, 24.0, 8.0, panel, sc_common, c, ss, (0, 1, 2, 3, 4))
    q_res = pose_groups(q1, groups, None, resonance=4)
    res = vp.render(q_res, T1, glow_cfg, groups, 20.0, 10.0, panel, vp.fit(body, 20.0, 10.0, panel, (-34.0, -10.0))[0], vp.fit(body, 20.0, 10.0, panel, (-34.0, -10.0))[1], ss, (0, 1, 2, 3, 4))
    lod_im = None
    d2, q2, T2, g2 = vp.load(d1["lod"], VU_MESH, VU_TEX, spec, 0, lod=True)
    lb = np.concatenate([q[2] for q in q2 if q[0] == 0])
    lsc, lc = vp.fit(lb, 28.0, 14.0, panel)
    lod_im = vp.render(q2, T2, d2["glow"], g2, 28.0, 14.0, panel, lsc, lc, ss, layers=(0, 2))

    W = panel[0] * 4
    H = (panel[1] + 18) * 3
    out = Image.new("RGB", (W, H), (10, 10, 14))
    dr = ImageDraw.Draw(out)
    y = 0
    for title, row in (("before: the old model (client/DemonSlayerMesh.java, %d body quads)" % sum(1 for q in q0 if q[0] == 0), old_tiles),
                       ("after: the upgraded model at rest (%d body quads, 2048 px skin; floating parts and halo cards included)" % sum(1 for q in q_view if q[0] == 0), new_tiles)):
        dr.text((6, y + 3), title, fill=(255, 255, 255))
        for i, (name, im) in enumerate(row):
            out.paste(Image.fromarray((im * 255).astype(np.uint8)), (i * panel[0], y + 18))
            dr.text((i * panel[0] + 6, y + 20), name, fill=(255, 255, 255))
        y += panel[1] + 18
    dr.text((6, y + 3), "after: emissive passes alone | Black Meteorite (fracture 1) | Resonance 4 (bloom grown, clover swollen) | LOD mesh (> 12 blocks, %d body quads)" % sum(1 for q in q2 if q[0] == 0), fill=(255, 255, 255))
    for i, im in enumerate((glow, frac, res, lod_im)):
        out.paste(Image.fromarray((im * 255).astype(np.uint8)), (i * panel[0], y + 18))
    path = os.path.join(SCRATCH, "demon_slayer_sword.png") if tmp else os.path.join(PREVIEWS, ID + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path, optimize=True)
    return path


def faces(tmp=False, panel=(520, 860), ss=2):
    """Both faces of the new blade close up (front from the right, back from the left), floating parts and rings hidden: the geometry and the painted skin."""
    spec = json.load(open(os.path.join(SPEC_DIR, ID + ".json")))
    groups = spec["groups"]
    d1, q1, T1, g1 = vp.load(ID, VU_MESH, VU_TEX, spec, 0.0)
    q = hide(pose_groups(q1, groups, None), groups, {"crack_1", "crack_2", "crack_3", "aura", "field", "field_low", "ring_guard", "ring_inner", "arcs", "wake", "orbit", "fragments", "leaves"})
    body = np.concatenate([x[2] for x in q if x[0] == 0])
    out = Image.new("RGB", (panel[0] * 2, panel[1]), (10, 10, 14))
    for i, yaw in enumerate((12.0, -150.0)):
        sc, c = vp.fit(body, yaw, 6.0, panel, (-3.0, 50.0))
        im = vp.render(q, T1, (0xFFFFFF, 0.14, 0.4, 1.0), groups, yaw, 6.0, panel, sc, c, ss, (0, 1, 2, 3, 4))
        out.paste(Image.fromarray((im * 255).astype(np.uint8)), (i * panel[0], 0))
    path = os.path.join(SCRATCH, "demon_slayer_sword_faces.png") if tmp else os.path.join(PREVIEWS, ID + "_faces.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path, optimize=True)
    return path


if __name__ == "__main__":
    print("wrote", sheet(tmp="--tmp" in sys.argv))
    print("wrote", faces(tmp="--tmp" in sys.argv))
