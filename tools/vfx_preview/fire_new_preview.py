#!/usr/bin/env python3
"""
FIRE remake (new round), PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

For every fire shape (11) four pictures of the same spell at the same tick, same camera:
    BEFORE  the REAL current layer (FireSpellLayer / FlameFxLayer / FlameMagicLayer) through the repo's Java preview
    A       "anime cel"          flat-tone baked flame textures, hooked tongues, flip-book every 2 ticks
    B       "volumetric glow"    soft layered puffs, additive halos, rays, embers
    C       "real 3D toon meshes" authored geometry in the sdw3 mesh format, 3-tone toon + emissive core + rim light per quad

    python tools/gen_fire_new_textures.py
    python tools/gen_fire_new_meshes.py
    python tools/vfx_preview/fire_new_preview.py [scene keys ...]          # needs JDK 21, numpy, Pillow (like preview.py); first run needs lib/joml-1.10.5.jar

Output: docs/previews/vfx_remake/fire_new/fire_compare.png, fire_A.png, fire_B.png, fire_C.png, fire_budget.json (+ fire_compare_ref.png with the canon still).
Honest limits: headless software rasteriser (2x supersampled), painter's order for alpha / additive, depth buffer only for the C meshes, no bloom, no world light, never seen in a game.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview as pv                                                        # noqa: E402  (the repo's own preview tool)
import fire_new_raster as R                                                 # noqa: E402
import fire_new_out as FO                                                   # noqa: E402
import fire_new_effects as FE                                               # noqa: E402

ROOT = pv.ROOT
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "fire_new")
TEXDIR = os.path.join(OUT, "tex")
PACK = os.environ.get("FIRE_PACK", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/elemzip/Fire2/Fire Magic VFX Effects")
CW, CH, SS = 520, 390, 2
STILL = {"lion": "Fuegoleon_Leo_Rugiens.webp", "spear": "Burst_Javelin.webp", "pillar": "Magna_burns_his_pies.webp", "burst": "Magna_Exploding_Fireball.webp",
         "spiral": "Leopold_spiral_flame.webp", "wild": "Leopold_wild_bursting_flame.webp", "cast": "Calidus_Brachium.webp", "zone": "Calidus_Brachium_Purgatory.webp",
         "fxburst": "Calidus_Brachium_Barrage.webp", "trail": "Magna%27s_Vanishing_Fireball.webp", "explosion": "Combo_Eruption.webp"}
UPV = np.array([0.0, 1.0, 0.0])
NAMES = {"A": "A anime cel", "B": "B volumetric glow", "C": "C real 3D toon meshes"}


def scene_dict(sc):
    d = {"layer": sc["layer"], "shape": sc["shape"], "from": [float(x) for x in sc["F"]], "to": [float(x) for x in sc["T"]], "power": sc["p"], "duration": sc["dur"],
         "cam": list(sc["cam"]), "target": list(sc["target"]), "ages": sc["ages"], "figures": sc["figures"]}
    for k in ("sky", "horizon"):
        if k in sc:
            d[k] = sc[k]
    return d


def to_quads(frame):
    out = []
    for q in frame["quads"]:
        v = np.array(q["v"], dtype=np.float64)
        col = np.array([[((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255] for c in v[:, 5].astype(np.int64)])
        out.append((q["t"], q["b"], q["o"], v[:, :3], v[:, 3:5], col))
    out.sort(key=lambda t: t[2])
    return out


def render_before(sd, frame, label):
    r = R.Raster(CW * SS, CH * SS, sd["cam"], sd["target"], sd)
    for tname, blend, o, pts, uv, col in to_quads(frame):
        r.textured(pts, uv, col, tname, blend)
    return decorate(r.result(CW, CH), label, "")


def decorate(tile, label, sub):
    d = ImageDraw.Draw(tile)
    d.rectangle([0, 0, CW, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.rectangle([0, CH - 14, CW, CH], fill=(14, 16, 20))
        d.text((5, CH - 12), sub, fill=(255, 235, 150))
    return tile


def make_out(sc, ver, age):
    o = FO.Out(sc["cam"], sc["target"], age, sc["dur"])
    sc["gen"]["ABC".index(ver)](o, sc["F"], sc["T"], sc["p"])
    return o


def render_new(sc, ver, age, sd, peak):
    o = make_out(sc, ver, age)
    r = R.Raster(CW * SS, CH * SS, sc["cam"], sc["target"], sd)
    FO.draw(o, r)
    v = o.verts()
    return decorate(r.result(CW, CH), f"{NAMES[ver]}   t={age}/{sc['dur']}   {v} verts now, peak {peak[ver]} / current cap {sc['cap']}", "")


def peak_stats(sc, ver):
    pk = 0
    pk_t = 0
    mesh_q = 0
    for age in range(0, min(sc["dur"], 120) + 1):
        o = make_out(sc, ver, age)
        if o.verts() > pk:
            pk, pk_t, mesh_q = o.verts(), age, len(o.m)
    return pk, pk_t, mesh_q


def mesh_sheet():
    """every authored mesh on its own: three turntable views, drawn with the same toon rule, on a neutral backdrop (no effect, only the geometry)"""
    names = ["lion_head", "tongue_a", "tongue_b", "tonguelo_a", "tonguexs_a", "ball_s", "gem", "column", "helix", "lance", "ring", "spike"]
    W, H = 220, 220
    sheet = Image.new("RGB", (W * 3, (H + 14) * len(names)), (16, 18, 22))
    d = ImageDraw.Draw(sheet)
    sd = {"sky": [0.20, 0.21, 0.26], "horizon": [0.30, 0.30, 0.34], "floor": [0.18, 0.18, 0.2], "ground": -50.0}
    for i, n in enumerate(names):
        m = FO.mesh(n)
        pts = np.concatenate([q[1] for q in m.q])
        ext = float(np.abs(pts).max()) / 16.0
        yl = np.array([1.0, 0.0, 0.0]) if n == "lion_head" else UPV
        xh = np.array([0.0, 0.0, 1.0]) if n == "lion_head" else np.array([1.0, 0.0, 0.0])
        mid = (pts.max(axis=0) + pts.min(axis=0)) / 32.0
        cy = mid[1] if n != "lion_head" else 0.0
        d.text((4, i * (H + 14) + 1), f"{n}   {len(m.q)} quads = {len(m.q) * 4} vertices", fill=(255, 235, 150))
        for j, yaw in enumerate((0.5, 1.9, 3.3)):
            tgt = np.array([0.0, cy if n != "lion_head" else 0.0, 0.0]) if n != "lion_head" else np.array([0.0, 0.0, 0.0])
            if n == "lion_head":
                tgt = np.array([0.0, 0.0, 0.0])
            cam = tgt + np.array([math.sin(yaw) * ext * 2.6, ext * 0.55, -math.cos(yaw) * ext * 2.6])
            o = FO.Out(cam, tgt, 0, 10)
            o.mesh(m, np.array([0.0, 0.0, 0.0]), yl, xhint=xh, layers=(0, 2))
            r = R.Raster(W * SS, H * SS, cam, tgt, sd, fov=40.0)
            FO.draw(o, r)
            sheet.paste(r.result(W, H), (j * W, i * (H + 14) + 14))
    sheet.save(os.path.join(OUT, "fire_C_meshes.png"))


def main():
    if "--meshes" in sys.argv:
        mesh_sheet()
        return
    keys = [a for a in sys.argv[1:] if not a.startswith("-")]
    scenes = [s for s in FE.SCENES if not keys or s["key"] in keys]
    os.makedirs(OUT, exist_ok=True)
    R.load_dir("A_", TEXDIR) if False else None
    for fn in os.listdir(TEXDIR):
        if fn.endswith(".png") and fn != "tex_sheet.png":
            a = np.asarray(Image.open(os.path.join(TEXDIR, fn)).convert("RGBA"), dtype=np.float32) / 255.0
            a[..., :3] *= a[..., 3:4]
            R._TEX[fn[:-4]] = a
    pv.W, pv.H = CW * SS, CH * SS
    pv.BUILD = os.environ.get("VFX_PREVIEW_BUILD") or pv.BUILD
    os.makedirs(pv.BUILD, exist_ok=True)
    classes = pv.build([sc["layer"] for sc in scenes])
    budget = {}
    rows = {v: [] for v in "ABC"}
    cmp_rows = []
    for sc in scenes:
        sd = scene_dict(sc)
        data = pv.run_java(sd, classes, os.path.join(pv.BUILD, "fire_new_before_" + sc["key"] + ".json"))
        bq = max(fr["vertices"] for fr in data["frames"])
        pk = {v: peak_stats(sc, v) for v in "ABC"}
        peaks = {v: pk[v][0] for v in "ABC"}
        budget[sc["key"]] = {"title": sc["title"], "shape": sc["shape"], "cap_current_vertices": sc["cap"], "before_peak_vertices_in_preview": bq,
                             "A": {"peak_vertices": pk["A"][0], "peak_tick": pk["A"][1]}, "B": {"peak_vertices": pk["B"][0], "peak_tick": pk["B"][1]},
                             "C": {"peak_vertices": pk["C"][0], "peak_tick": pk["C"][1], "mesh_quads_at_peak": pk["C"][2]}, "still": sc["still"]}
        tiles = {v: [render_new(sc, v, age, sd, peaks) for age in sc["ages"]] for v in "ABC"}
        for v in "ABC":
            rows[v].append((sc, tiles[v], pk[v][0]))
        mid = sc["mid"]
        fr = data["frames"][mid]
        before = render_before(sd, fr, f"BEFORE (real current layer)   t={fr['age']:.0f}/{sc['dur']}   {fr['vertices']} verts, cap {sc['cap']}")
        cmp_rows.append((sc, [before] + [tiles[v][mid] for v in "ABC"]))
        print(sc["key"], {v: peaks[v] for v in "ABC"}, "cap", sc["cap"], "before", bq, flush=True)
    for v in "ABC":
        maxc = max(len(r[1]) for r in rows[v])
        sheet = Image.new("RGB", (CW * maxc, CH * len(rows[v]) + 30), (16, 18, 22))
        ImageDraw.Draw(sheet).text((8, 8), f"FIRE  direction {NAMES[v]}   (software render, 2x supersampled, no bloom, not seen in game)", fill=(255, 255, 255))
        for i, (sc, tl, pkv) in enumerate(rows[v]):
            for j, t in enumerate(tl):
                sheet.paste(t, (j * CW, 30 + i * CH))
        sheet.save(os.path.join(OUT, f"fire_{v}.png"))
    cmp = Image.new("RGB", (CW * 4, CH * len(cmp_rows) + 30), (16, 18, 22))
    ImageDraw.Draw(cmp).text((8, 8), "FIRE  BEFORE | A anime cel | B volumetric glow | C real 3D toon meshes   (same camera, same tick; software render, no bloom, not seen in game)", fill=(255, 255, 255))
    ref = Image.new("RGB", (300 + CW * 4, CH * len(cmp_rows) + 30), (16, 18, 22))
    ref.paste(cmp.crop((0, 0, CW * 4, 30)), (300, 0))
    dr = ImageDraw.Draw(ref)
    for i, (sc, tl) in enumerate(cmp_rows):
        for j, t in enumerate(tl):
            cmp.paste(t, (j * CW, 30 + i * CH))
            ref.paste(t, (300 + j * CW, 30 + i * CH))
        pth = os.path.join(PACK, STILL[sc["key"]].replace("%27", "%27"))
        try:
            st = Image.open(pth).convert("RGB")
            k = min(288 / st.width, (CH - 60) / st.height)
            st = st.resize((int(st.width * k), int(st.height * k)), Image.LANCZOS)
            ref.paste(st, (6, 30 + i * CH + 18))
        except OSError:
            pass
        dr.text((6, 30 + i * CH + 3), STILL[sc["key"]][:44] + "  (canon)", fill=(255, 200, 120))
    cmp.save(os.path.join(OUT, "fire_compare.png"))
    ref.save(os.path.join(OUT, "fire_compare_ref.png"))
    if not keys:
        with open(os.path.join(OUT, "fire_budget.json"), "w") as f:
            json.dump(budget, f, indent=1)


if __name__ == "__main__":
    main()
