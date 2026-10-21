"""Before / after contact sheets of the slice `djinn_dark_weapons` (the software rasteriser of tools/relics/preview_mesh.py, the same shading for both rows).

    python3 -B tools/visual_upgrade/djinn_dark_weapons/preview.py [djinn id ...]

Writes docs/previews/visual_upgrade/djinn_dark_weapons/<id>.png: top row the OLD mesh (held view, three-quarter, close-up of the head, its glow pass), bottom row the UPGRADED mesh with its
orbiting pieces at a fixed phase (held view, three-quarter, close-up of the head, the core glow pass, the vein pass). Nothing here is the game's own renderer: it shows the geometry and
the painted maps; the animation, the particles and the additive halo can only be judged in the game. The sheets are palette-quantised PNGs (about 150 KB each).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import dlib as D                                   # noqa: E402
import preview_mesh as pm                          # noqa: E402
import ditems as I                                 # noqa: E402

ASSETS = D.ASSETS
OUT = os.path.join(D.ROOT, "docs", "previews", "visual_upgrade", "djinn_dark_weapons")
PW, PH = 240, 400


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def load(mesh_path, tex_base):
    d = pm.parse_game_txt(mesh_path)
    g = tex_base + "_glow.png"
    g2 = tex_base + "_glow2.png"
    return d, tex_base + ".png", (g if os.path.exists(g) else None), (g2 if os.path.exists(g2) else None)


def orbit_quads(orbit, spec, y0, y1, t=0.0):
    out = []
    for o in spec["orbiters"]:
        for i in range(o["count"]):
            ang = o["phase"] + o["speed"] * t + 2 * math.pi * i / o["count"]
            y = y0 + (y1 - y0) * o["y_frac"] + o["bob"] * math.sin(o["bob_speed"] * t + i * 1.3)
            c = np.array([o["radius"] * math.cos(ang), y, o["radius"] * math.sin(ang)])
            R = rot_y(-ang + o["spin"] * t + i) @ rot_x(o["tilt"])
            for (layer, P, UV, N) in orbit:
                out.append((layer, (P * o["scale"]) @ R.T + c, UV, N @ R.T))
    return out


def tinted(quads):
    return [(l, P, UV, N, (0.7, 0.7, 0.7)) for (l, P, UV, N) in quads]


def sheet(did):
    iid = I.item_id(did)
    spec = json.load(open(os.path.join(D.DATA_DIR, iid + ".json"), encoding="utf-8"))
    td = os.path.join(ASSETS, "textures", "entity", "weapon")
    before, bskin, bglow, _ = load(os.path.join(ASSETS, "weapon_meshes", iid + ".txt"), os.path.join(td, iid))
    after, askin, aglow, aglow2 = load(os.path.join(ASSETS, "weapon_meshes", "up", iid + ".txt"), os.path.join(td, "up", iid))
    orb = pm.parse_game_txt(os.path.join(ASSETS, "weapon_meshes", "up", iid + "_orbit.txt"))["quads"]
    y0, y1 = spec["axis"]["y0"], spec["axis"]["y1"]
    aq = list(after["quads"]) + orbit_quads(orb, spec, y0, y1)

    def fit(quads, yaw, pitch, focus=None):
        pts = np.concatenate([q[1] for q in quads if q[0] == 0])
        if focus:
            sel = pts[(pts[:, 1] >= focus[0]) & (pts[:, 1] <= focus[1])]
            pts = sel if len(sel) else pts
        Q = pts @ pm._rot(yaw, pitch).T
        w_, h_ = Q[:, 0].max() - Q[:, 0].min(), Q[:, 1].max() - Q[:, 1].min()
        c = ((Q[:, 0].max() + Q[:, 0].min()) / 2, (Q[:, 1].max() + Q[:, 1].min()) / 2)
        return min((PW - 20) / max(w_, 1e-6), (PH - 34) / max(h_, 1e-6)), c

    head_focus = (y1 - 0.34 * (y1 - y0), y1 + 20)
    views = [("held", 0, 0, None), ("three-quarter", 35, 22, None), ("head close-up", 20, 12, head_focus)]
    common = min(fit(aq, v[1], v[2])[0] for v in views[:2])
    rows = []
    for (label, quads, skin, glow, glow2) in (("BEFORE (old mesh)", tinted(before["quads"]), bskin, bglow, None), ("AFTER (upgraded mesh + orbiting pieces)", tinted(aq), askin, aglow, aglow2)):
        tiles = []
        gl = after["glow"] if label.startswith("AFTER") else before["glow"]
        for (vn, yaw, pitch, focus) in views:
            sc, c = fit(aq, yaw, pitch, focus)
            im = pm.render_view(quads, yaw, pitch, (PW, PH), common if focus is None else sc, c, skin, glow, gl, 2, True, True, True)
            tiles.append((vn, im))
        gq = [q for q in quads if q[0] == 2]
        sc, c = fit(aq, 0, 0)
        base = np.zeros((PH, PW, 3), np.float32) + 0.05
        tiles.append(("core glow pass", pm.render_view(gq, 0, 0, (PW, PH), common, c, skin, glow, gl, 2, True, True, True) if gq else base))
        if glow2 and label.startswith("AFTER"):
            tiles.append(("vein pass", pm.render_view(gq, 0, 0, (PW, PH), common, c, glow2, glow2, (0xFFFFFF, 0.1, 0.4, 1.0), 2, True, True, True) if gq else base))
        rows.append((label, tiles))
    W = max(sum(t[1].shape[1] for t in r[1]) for r in rows)
    out = Image.new("RGB", (W, 2 * (PH + 20)), (20, 22, 28))
    d = ImageDraw.Draw(out)
    for ri, (label, tiles) in enumerate(rows):
        x, y = 0, ri * (PH + 20)
        d.text((6, y + 4), "%s: %s" % (iid, label), fill=(255, 255, 255))
        for (vn, im) in tiles:
            out.paste(Image.fromarray((np.clip(im, 0, 1) * 255).astype(np.uint8)), (x, y + 20))
            d.text((x + 6, y + 24), vn, fill=(255, 255, 255))
            x += im.shape[1]
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, did + ".png")
    out.quantize(colors=128, method=Image.MEDIANCUT, dither=Image.NONE).save(path, optimize=True)
    return path


def gallery(ids, path, pw=170, ph=330):
    """A contact sheet of several Djinn: the old weapon above, the upgraded one (with its orbiting pieces) below, the held view of each."""
    td = os.path.join(ASSETS, "textures", "entity", "weapon")
    cols = []
    for did in ids:
        iid = I.item_id(did)
        spec = json.load(open(os.path.join(D.DATA_DIR, iid + ".json"), encoding="utf-8"))
        before, bskin, bglow, _ = load(os.path.join(ASSETS, "weapon_meshes", iid + ".txt"), os.path.join(td, iid))
        after, askin, aglow, _ = load(os.path.join(ASSETS, "weapon_meshes", "up", iid + ".txt"), os.path.join(td, "up", iid))
        orb = pm.parse_game_txt(os.path.join(ASSETS, "weapon_meshes", "up", iid + "_orbit.txt"))["quads"]
        aq = list(after["quads"]) + orbit_quads(orb, spec, spec["axis"]["y0"], spec["axis"]["y1"])
        pts = np.concatenate([q[1] for q in aq if q[0] == 0])
        w_, h_ = pts[:, 0].max() - pts[:, 0].min(), pts[:, 1].max() - pts[:, 1].min()
        c = ((pts[:, 0].max() + pts[:, 0].min()) / 2, (pts[:, 1].max() + pts[:, 1].min()) / 2)
        sc = min((pw - 8) / max(w_, 1e-6), (ph - 8) / max(h_, 1e-6))
        col = []
        for quads, skin, glow, gl in ((tinted(before["quads"]), bskin, bglow, before["glow"]), (tinted(aq), askin, aglow, after["glow"])):
            col.append(pm.render_view(quads, 0, 0, (pw, ph), sc, c, skin, glow, gl, 2, True, True, True))
        cols.append(col)
    out = Image.new("RGB", (pw * len(cols), 2 * ph), (20, 22, 28))
    d = ImageDraw.Draw(out)
    for i, col in enumerate(cols):
        for r in (0, 1):
            out.paste(Image.fromarray((np.clip(col[r], 0, 1) * 255).astype(np.uint8)), (i * pw, r * ph))
        d.text((i * pw + 4, 3), ids[i], fill=(255, 255, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.quantize(colors=128, method=Image.MEDIANCUT, dither=Image.NONE).save(path, optimize=True)
    return path


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--gallery":
        print(gallery(args[2:], args[1]))
    else:
        for did in (args or I.ORDER):
            print(sheet(did))
