#!/usr/bin/env python3
"""
STAR MAGIC remake (phase 1), PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

For each of the seven star effects (placed star, constellation lines, Conjunction, Quartile Scutum, Quartile Hasta, Quartile Flagellum, Halation) the same moment, same camera:
    BEFORE  the REAL current layer (ElementIdentityLayer / AuraLayer) through the repo's Java preview, called the way the nearest existing page calls it (scenes.d/star_baseline.json)
    A       "anime cel"          flat baked textures, hard bands, stepped timing
    B       "volumetric glow"    soft bloom stars, lens flare, filaments, nebula dust, additive
    C       "real 3D toon meshes" authored geometry (star solids, tubes, lances, facets), 3-tone toon + emissive core + rim per quad, a few glow cards

    python tools/gen_star_new_textures.py
    python tools/vfx_preview/star_new_preview.py [scene keys ...]          # needs JDK 21, numpy, Pillow; first run needs lib/joml-1.10.5.jar

Output: docs/previews/vfx_remake/star/star_baseline.png, star_compare.png, star_A.png, star_B.png, star_C.png, star_budget.json (+ star_closeups.png).
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
import preview as pv                                                        # noqa: E402
import fire_new_raster as R                                                 # noqa: E402
import fire_new_out as FO                                                   # noqa: E402
import star_new_effects as SE                                               # noqa: E402

ROOT = pv.ROOT
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "star")
TEXDIR = os.path.join(OUT, "tex")
BASE = os.path.join(HERE, "scenes.d", "star_baseline.json")
CW, CH, SS = 520, 390, 2
NAMES = {"A": "A anime cel", "B": "B volumetric glow", "C": "C real 3D toon meshes"}


def load_tex():
    for fn in os.listdir(TEXDIR):
        if fn.endswith(".png") and fn != "tex_sheet.png":
            a = np.asarray(Image.open(os.path.join(TEXDIR, fn)).convert("RGBA"), dtype=np.float32) / 255.0
            a[..., :3] *= a[..., 3:4]
            R._TEX[fn[:-4]] = a


def scene_dict(sc):
    return {"cam": list(sc["cam"]), "target": list(sc["target"]), "figures": sc["figures"], "sky": sc["sky"], "horizon": sc["horizon"], "floor": sc["floor"]}


def to_quads(frame):
    out = []
    for q in frame["quads"]:
        v = np.array(q["v"], dtype=np.float64)
        col = np.array([[((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255] for c in v[:, 5].astype(np.int64)])
        out.append((q["t"], q["b"], q["o"], v[:, :3], v[:, 3:5], col))
    out.sort(key=lambda t: t[2])
    return out


def decorate(tile, label, sub=""):
    d = ImageDraw.Draw(tile)
    d.rectangle([0, 0, CW, 14], fill=(10, 12, 24))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.rectangle([0, CH - 14, CW, CH], fill=(10, 12, 24))
        d.text((5, CH - 12), sub, fill=(255, 235, 150))
    return tile


def render_before(sd, frame, label, sub=""):
    r = R.Raster(CW * SS, CH * SS, sd["cam"], sd["target"], sd)
    for tname, blend, o, pts, uv, col in to_quads(frame):
        r.textured(pts, uv, col, tname, blend)
    return decorate(r.result(CW, CH), label, sub)


def make_out(sc, ver, age):
    o = FO.Out(sc["cam"], sc["target"], age, sc["dur"])
    sc["build"](SE.DIRS[ver], o, sc)
    return o


def render_new(sc, ver, age, sd, peak):
    o = make_out(sc, ver, age)
    r = R.Raster(CW * SS, CH * SS, sc["cam"], sc["target"], sd)
    FO.draw(o, r)
    return decorate(r.result(CW, CH), f"{NAMES[ver]}   t={age}/{sc['dur']}   {o.verts()} verts now" + (f", peak {peak[ver]}" if peak[ver] else ""), "")


def peak_stats(sc, ver):
    pk, pk_t, mesh_q = 0, 0, 0
    for age in range(0, min(sc["dur"], 100) + 1):
        o = make_out(sc, ver, age)
        if o.verts() > pk:
            pk, pk_t, mesh_q = o.verts(), age, len(o.m)
    return pk, pk_t, mesh_q


def primitive_budget():
    """vertices of ONE primitive per direction (LOD0), on a fixed camera: the unit the game would spend per node entity / link / pane ... (C LOD1 = the B sprite version)"""
    from fire_new_raster import V
    U = SE.UP
    res = {}
    cam, tgt = (0, 2, -6), (0, 2, 0)
    for v in "ABC":
        d = SE.DIRS[v]
        out = {}

        def count(fn):
            o = FO.Out(cam, tgt, 10, 40)
            fn(d, o)
            return o.verts()

        out["node"] = count(lambda d, o: d.node(o, V(0, 2, 0), 1.0, 10))
        out["link_5_blocks"] = count(lambda d, o: d.link(o, V(0, 2, 0), V(5, 2, 0), 1.0, 0.09, 1.0, 10))
        out["sigil"] = count(lambda d, o: d.sigil(o, V(0, 0.04, 0), 1.7, 1.0, 10))
        out["pane"] = count(lambda d, o: d.pane(o, V(0, 2, 0), V(1, 0, 0), U, 2.2, 1.7, 1.0, 10))
        out["spear"] = count(lambda d, o: d.beam(o, V(0, 2, 0), V(4, 2, 0), 0.4, 1.0, 10, kind="lance"))
        out["ray"] = count(lambda d, o: d.beam(o, V(0, 2, 0), V(4, 2, 0), 0.3, 1.0, 10, kind="ray"))
        pts = SE.catmull([V(0, 1.5, 0), V(2.5, 1.8, 0), V(4.4, 1.5, 0.5), V(6, 1.4, 1.2), V(7.7, 1.5, 2.2)], per=4)
        out["whip"] = count(lambda d, o: d.whip(o, pts, 0.2, 1.0, 10))
        out["flash"] = count(lambda d, o: d.flash(o, V(0, 2, 0), 1.0, 1.0, 10))
        out["ring"] = count(lambda d, o: d.ring(o, V(0, 2, 0), o.r, o.u, 1.0, 1.0, 10))
        out["spark"] = count(lambda d, o: d.spark(o, V(0, 2, 0), 1.0, 1.0, 10))
        res[v] = out
    return res


def closeups(scenes):
    global CW, CH
    keep = (CW, CH)
    CW, CH = 860, 645
    pick = [s for s in scenes if s["key"] in ("node", "scutum", "hasta")]
    tiles = []
    for sc in pick:
        sd = scene_dict(sc)
        row = []
        for v in "ABC":
            row.append(render_new(sc, v, sc["ages"][sc["mid"]], sd, {"A": 0, "B": 0, "C": 0}))
        tiles.append(row)
    sh = Image.new("RGB", (CW * 3, CH * len(tiles)), (10, 12, 24))
    for i, row in enumerate(tiles):
        for j, t in enumerate(row):
            sh.paste(t, (j * CW, i * CH))
    sh.save(os.path.join(OUT, "star_closeups.png"))
    CW, CH = keep


def run_before(base, classes, key):
    """one Java run over every tick of the effect (for the peak) and merge the 'also' bolts of a volley"""
    sc = dict(base[key])
    dur = min(sc["duration"], 60)
    sc["ages"] = list(range(0, dur + 1))
    data = pv.run_java(sc, classes, os.path.join(pv.BUILD, "star_before_" + key + ".json"))
    for k, extra in enumerate(sc.get("also", [])):
        s2 = dict(sc)
        s2.update(extra)
        d2 = pv.run_java(s2, classes, os.path.join(pv.BUILD, "star_before_%s_also%d.json" % (key, k)))
        for fr, fr2 in zip(data["frames"], d2["frames"]):
            fr["quads"] = fr["quads"] + fr2["quads"]
            fr["vertices"] += fr2["vertices"]
    return data


def main():
    keys = [a for a in sys.argv[1:] if not a.startswith("-")]
    scenes = [s for s in SE.SCENES if not keys or s["key"] in keys]
    os.makedirs(OUT, exist_ok=True)
    load_tex()
    pv.W, pv.H = CW * SS, CH * SS
    pv.BUILD = os.environ.get("VFX_PREVIEW_BUILD") or pv.BUILD
    os.makedirs(pv.BUILD, exist_ok=True)
    with open(BASE) as f:
        base = {k: v for k, v in json.load(f).items() if not k.startswith("_")}
    classes = pv.build(sorted({base[sc["before"]]["layer"] for sc in scenes}))
    budget, rows, cmp_rows, base_rows = {}, {v: [] for v in "ABC"}, [], []
    for sc in scenes:
        sd = scene_dict(sc)
        data = run_before(base, classes, sc["before"])
        bsc = base[sc["before"]]
        bpeak = max(fr["vertices"] for fr in data["frames"])
        pk = {v: peak_stats(sc, v) for v in "ABC"}
        peaks = {v: pk[v][0] for v in "ABC"}
        budget[sc["key"]] = {"title": sc["title"], "before_scene": sc["before"], "before_layer": bsc["layer"], "before_peak_vertices": bpeak, "engine_cap_per_activation": 400,
                             "A": {"peak_vertices": pk["A"][0], "peak_tick": pk["A"][1]}, "B": {"peak_vertices": pk["B"][0], "peak_tick": pk["B"][1]},
                             "C": {"peak_vertices": pk["C"][0], "peak_tick": pk["C"][1], "mesh_quads_at_peak": pk["C"][2]}, "canon": sc["canon"], "note": bsc.get("note", "")}
        tiles = {v: [render_new(sc, v, age, sd, peaks) for age in sc["ages"]] for v in "ABC"}
        for v in "ABC":
            rows[v].append((sc, tiles[v]))
        btiles = []
        for age in bsc["ages"]:
            fr = data["frames"][min(int(age), len(data["frames"]) - 1)]
            btiles.append(render_before(sd, fr, f"BEFORE (real layer {bsc['layer']})  t={fr['age']:.0f}/{bsc['duration']}  {fr['vertices']} verts, cap 400", ""))
        base_rows.append((sc, bsc, btiles))
        bm = btiles[sc["before_mid"]]
        cmp_rows.append((sc, [bm] + [tiles[v][sc["mid"]] for v in "ABC"]))
        print(sc["key"], {v: peaks[v] for v in "ABC"}, "before", bpeak, flush=True)
    # baseline sheet
    maxc = max(len(r[2]) for r in base_rows)
    sheet = Image.new("RGB", (CW * maxc, CH * len(base_rows) + 30), (10, 12, 24))
    ImageDraw.Draw(sheet).text((8, 8), "STAR MAGIC  BEFORE: the real current layer, called like the nearest existing page (software render, no bloom, not seen in game)", fill=(255, 255, 255))
    for i, (sc, bsc, bt) in enumerate(base_rows):
        for j, t in enumerate(bt):
            sheet.paste(t, (j * CW, 30 + i * CH))
        ImageDraw.Draw(sheet).text((6, 30 + i * CH + CH - 12), sc["title"] + " :: " + bsc["note"][:90], fill=(255, 235, 150))
    sheet.save(os.path.join(OUT, "star_baseline.png"))
    for v in "ABC":
        mc = max(len(r[1]) for r in rows[v])
        sh = Image.new("RGB", (CW * mc, CH * len(rows[v]) + 30), (10, 12, 24))
        ImageDraw.Draw(sh).text((8, 8), f"STAR MAGIC  direction {NAMES[v]}   (software render, 2x supersampled, no bloom, not seen in game)", fill=(255, 255, 255))
        for i, (sc, tl) in enumerate(rows[v]):
            for j, t in enumerate(tl):
                sh.paste(t, (j * CW, 30 + i * CH))
            ImageDraw.Draw(sh).text((6, 30 + i * CH + CH - 12), sc["title"], fill=(255, 235, 150))
        sh.save(os.path.join(OUT, f"star_{v}.png"))
    cmp = Image.new("RGB", (CW * 4, CH * len(cmp_rows) + 30), (10, 12, 24))
    ImageDraw.Draw(cmp).text((8, 8), "STAR MAGIC  BEFORE | A anime cel | B volumetric glow | C real 3D toon meshes   (same camera; software render, no bloom, not seen in game)", fill=(255, 255, 255))
    for i, (sc, tl) in enumerate(cmp_rows):
        for j, t in enumerate(tl):
            cmp.paste(t, (j * CW, 30 + i * CH))
        ImageDraw.Draw(cmp).text((6, 30 + i * CH + CH - 12), sc["title"], fill=(255, 235, 150))
    cmp.save(os.path.join(OUT, "star_compare.png"))
    if not keys:
        closeups(scenes)
        with open(os.path.join(OUT, "star_budget.json"), "w") as f:
            json.dump({"scenes": budget, "primitives_vertices_lod0": primitive_budget()}, f, indent=1)


if __name__ == "__main__":
    main()
