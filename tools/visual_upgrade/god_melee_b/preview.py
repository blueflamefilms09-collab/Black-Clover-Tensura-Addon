"""Before / after contact sheets of the slice `god_melee_b` (the software rasteriser of tools/relics/preview_mesh.py, the same shading for both rows).

    python3 -B tools/visual_upgrade/god_melee_b/preview.py [id ...]

Writes docs/previews/visual_upgrade/god_melee_b/<id>.png: top row the OLD mesh (held view, three-quarter, close-up of the head, its glow pass), bottom row the UPGRADED
mesh with its orbiting pieces at a fixed phase (held view, three-quarter, close-up of the head, the core glow pass, the vein pass). Nothing here is the game's own renderer:
it shows the geometry and the painted maps; the animation, the particles and the additive halo can only be judged in the game.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lib                                         # noqa: E402
import preview_mesh as pm                          # noqa: E402

ASSETS = lib.ASSETS
OUT = os.path.join(lib.ROOT, "docs", "previews", "visual_upgrade", "god_melee_b")
ORDER = ["aurliels_seraph_halberd", "chronarchs_pendulum_sabre", "deepwatch_harpoon", "orials_starfall_staff", "reflected_kings_mirrorblade", "rimeheart_greatblade",
         "sanguine_wardens_cleaver", "valkarins_stormlance", "paradox"]


def load(mesh_path, tex_base):
    d = pm.parse_game_txt(mesh_path)
    skin = tex_base + ".png"
    glow = tex_base + "_glow.png"
    glow2 = tex_base + "_glow2.png"
    return d, skin, (glow if os.path.exists(glow) else None), (glow2 if os.path.exists(glow2) else None)


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def orbit_quads(orbit, spec, y0, y1, t=0.0):
    """The quads of every orbiting piece at time t (ticks), placed as client/GodMeleeBRender does."""
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


def sheet(item_id):
    spec = json.load(open(os.path.join(lib.DATA_DIR, item_id + ".json"), encoding="utf-8"))
    before, bskin, bglow, _ = load(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt"), os.path.join(ASSETS, "textures", "entity", "weapon", item_id))
    after, askin, aglow, aglow2 = load(os.path.join(ASSETS, "weapon_meshes", "up", item_id + ".txt"), os.path.join(ASSETS, "textures", "entity", "weapon", "up", item_id))
    orb = pm.parse_game_txt(os.path.join(ASSETS, "weapon_meshes", "up", item_id + "_orbit.txt"))["quads"]
    y0, y1 = spec["axis"]["y0"], spec["axis"]["y1"]
    aq = list(after["quads"]) + orbit_quads(orb, spec, y0, y1)
    allp = np.concatenate([q[1] for q in aq if q[0] == 0])
    ext = allp.max(axis=0) - allp.min(axis=0)
    pw, ph = 300, 520
    cols = []

    def fit(quads, yaw, pitch, focus=None):
        pts = np.concatenate([q[1] for q in quads if q[0] == 0])
        if focus:
            sel = pts[(pts[:, 1] >= focus[0]) & (pts[:, 1] <= focus[1])]
            pts = sel if len(sel) else pts
        Q = pts @ pm._rot(yaw, pitch).T
        w_, h_ = Q[:, 0].max() - Q[:, 0].min(), Q[:, 1].max() - Q[:, 1].min()
        c = ((Q[:, 0].max() + Q[:, 0].min()) / 2, (Q[:, 1].max() + Q[:, 1].min()) / 2)
        return min((pw - 24) / max(w_, 1e-6), (ph - 36) / max(h_, 1e-6)), c

    views = [("held", 0, 0, None), ("three-quarter", 35, 22, None), ("head close-up", 20, 12, (y1 - 0.30 * (y1 - y0), y1 + 30))]
    rows = []
    common = min(fit(aq, v[1], v[2])[0] for v in views[:2])
    for (label, quads, skin, glow, glow2) in (("BEFORE (old mesh)", tinted(before["quads"]), bskin, bglow, None), ("AFTER (upgraded mesh + orbiting pieces)", tinted(aq), askin, aglow, aglow2)):
        tiles = []
        for (vn, yaw, pitch, focus) in views:
            sc, c = fit(aq if focus is None else aq, yaw, pitch, focus)
            use = common if focus is None else sc
            gl = after["glow"] if label.startswith("AFTER") else before["glow"]
            im = pm.render_view(quads, yaw, pitch, (pw, ph), use, c, skin, glow, gl, 2, True, True, True)
            tiles.append((vn, im))
        # the glow pass alone
        gq = [q for q in quads if q[0] == 2]
        sc, c = fit(aq, 0, 0)
        gl = after["glow"] if label.startswith("AFTER") else before["glow"]
        base = np.zeros((ph, pw, 3), np.float32) + 0.05
        im = pm.render_view(gq, 0, 0, (pw, ph), common, c, skin, glow, gl, 2, True, True, True) if gq else base
        tiles.append(("core glow pass", im))
        if glow2 and label.startswith("AFTER"):
            im2 = pm.render_view(gq, 0, 0, (pw, ph), common, c, skin, glow2, (0xFFFFFF, 0.1, 0.4, 1.0), 2, True, True, True)
            tiles.append(("vein pass", im2))
        rows.append((label, tiles))
    W = max(sum(t[1].shape[1] for t in r[1]) for r in rows)
    out = Image.new("RGB", (W, 2 * (ph + 22)), (20, 22, 28))
    d = ImageDraw.Draw(out)
    for ri, (label, tiles) in enumerate(rows):
        x = 0
        y = ri * (ph + 22)
        d.text((6, y + 4), "%s: %s" % (item_id, label), fill=(255, 255, 255))
        for (vn, im) in tiles:
            out.paste(Image.fromarray((np.clip(im, 0, 1) * 255).astype(np.uint8)), (x, y + 22))
            d.text((x + 6, y + 26), vn, fill=(255, 255, 255))
            x += im.shape[1]
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, item_id + ".png")
    out.save(path)
    return path


if __name__ == "__main__":
    only = sys.argv[1:] or ORDER
    for i in only:
        print(sheet(i))
