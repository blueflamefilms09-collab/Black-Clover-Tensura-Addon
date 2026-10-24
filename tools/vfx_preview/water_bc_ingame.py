#!/usr/bin/env python3
"""
Water hybrid "BC", IN-GAME PATH preview.  Unlike water_bc_preview.py (phase A: quad and mesh generators written in Python), this runs the REAL layer classes of the game through the Java preview:

    BEFORE      WaterFxLayer / WaterMagicLayer (the code before the change = what vfx.element.water = V1 draws)
    BC in game  WaterBcLayer: the pure WaterBcScene (cores + glass), the toon shading of WaterBcToon (the mesh quads come out of the real WaterBcMeshBuffer), the sprites through the real VfxVertexBuffer
    LOD1        the same layer forced into the light-mesh tier,  A  the same layer forced into the flat-sprite tier (what the frame ceiling falls back to)

    python tools/gen_water_new_textures.py && python tools/gen_water_bc_textures.py && python tools/gen_water_bc_meshes.py      # once: the assets (preview folder + the game's wbc_* files)
    python tools/vfx_preview/water_bc_ingame.py                                                                              # needs JDK 21, numpy, Pillow, like preview.py

Output: docs/previews/vfx_remake/water_new/water_ingame.png (BEFORE | BC early | middle | late | LOD1 | A fall-back, same camera) and water_ingame_budget.json (peak sprite quads, mesh quads and vertices of
every tick of every effect, for the old layer and for each tier of the new one, and the V1 equivalence check).
How a frame is rasterised here (what the game does, in the same order): the mesh quads first, solid ones z-buffered and written to the depth buffer, then the translucent hulls in the order the layer made them
(fixed order, no sort, no depth write), then the glow quads additive; then the sprites of the vertex buffer in its batch order (ALPHA batches first, ADD last), depth TESTED against the solid cores, never written.
Honest limits: headless software rasteriser, no bloom, no world light, no depth against blocks, the translucent hulls and the glass sprites are not blended against each other by depth (as in the game),
never seen in a game.
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

ROOT = pv.ROOT
OUTDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "water_new")
CW, CH = 420, 315
SS = int(os.environ.get("WATER_SS", "2"))
os.chdir(ROOT)                                                               # the Java side finds the mesh files under src/main/resources from here

TINT = "FFB0121B"                                                            # a blood-page red: the splash and the ring keep a page's own colour

# key, title, old layer, scene, ages of the three BC tiles, mid index for LOD1 / A, colour (None = the layer's default)
ROWS = [
    ("roar", "WATERFX_CAST  Sea Dragon's Roar", "WaterFxLayer", "water_bc_roar", None),
    ("lance", "WATERFX_CAST (fast)  Aqua Javelin / Tide Lance", "WaterFxLayer", "water_bc_lance", None),
    ("cradle", "WATERFX_ZONE (follows)  Sea Dragon's Cradle", "WaterFxLayer", "water_bc_cradle", None),
    ("nest", "WATERFX_ZONE (placed)  Sea Dragon's Nest", "WaterFxLayer", "water_bc_nest", None),
    ("burst", "WATERFX_BURST  impact", "WaterFxLayer", "water_bc_burst", None),
    ("splash", "WATER_SPLASH  Mist Step", "WaterMagicLayer", "water_bc_splash", None),
    ("ring", "WATER_RING  Mist Veil", "WaterMagicLayer", "water_bc_ring", None),
    ("splash_tint", "WATER_SPLASH with a blood page's colour (red)  [grey twins x tint, tinted palette]", "WaterMagicLayer", "water_bc_splash", TINT),
    ("ring_tint", "WATER_RING with a blood page's colour (red)", "WaterMagicLayer", "water_bc_ring", TINT),
]


def load_scenes():
    with open(os.path.join(HERE, "scenes.d", "water_bc.json"), encoding="utf-8") as f:
        return {k: v for k, v in json.load(f).items() if not k.startswith("_")}


# ================================================================================================ mesh pass (the game's order: solid with depth, translucent in layer order, glow additive)
def raster_meshes(mesh, cam, target, ss=SS):
    """z-buffered software rasteriser for the flat coloured quads the real WaterBcToon made. Returns premultiplied colour (H, W, 3), coverage (H, W), glow (H, W, 3), inverse depth (H, W)."""
    Wd, Hd = pv.W, pv.H
    w, h = Wd * ss, Hd * ss
    f, r, u = pv.basis(cam, target)
    k = (h / 2) / math.tan(math.radians(pv.FOV / 2))
    C = np.zeros((h, w, 3), np.float32)
    A = np.zeros((h, w), np.float32)
    G = np.zeros((h, w, 3), np.float32)
    ZB = np.zeros((h, w), np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)

    def tris(pc):
        z = pc @ f
        if np.any(z < 0.05):
            return
        sx = w / 2 + (pc @ r) / z * k
        sy = h / 2 - (pc @ u) / z * k
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, math.floor(min(x0, x1, x2)))), int(min(w - 1, math.ceil(max(x0, x1, x2)))) + 1
            miny, maxy = int(max(0, math.floor(min(y0, y1, y2)))), int(min(h - 1, math.ceil(max(y0, y1, y2)))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            X = xx[miny:maxy, minx:maxx] + 0.5
            Y = yy[miny:maxy, minx:maxx] + 0.5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            msk = (a >= -1e-4) & (b >= -1e-4) & (c >= -1e-4)
            if not msk.any():
                continue
            iz = a / z[tri[0]] + b / z[tri[1]] + c / z[tri[2]]
            yield (slice(miny, maxy), slice(minx, maxx)), msk, iz.astype(np.float32)

    def rgba(c):
        return np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], np.float32) / 255.0, ((c >> 24) & 255) / 255.0

    for q in mesh:                                                           # pass 0: solid, depth written
        if q["k"] != 0:
            continue
        col, _ = rgba(q["c"])
        for sl, msk, iz in tris(np.array(q["p"], float).reshape(4, 3)):
            ok = msk & (iz > ZB[sl])
            ZB[sl] = np.where(ok, iz, ZB[sl])
            C[sl] = np.where(ok[..., None], col[None, None, :], C[sl])
            A[sl] = np.where(ok, 1.0, A[sl])
    for q in mesh:                                                           # pass 1: translucent, in the order the layer made them, depth tested, not written
        if q["k"] != 1:
            continue
        col, al = rgba(q["c"])
        for sl, msk, iz in tris(np.array(q["p"], float).reshape(4, 3)):
            ok = msk & (iz > ZB[sl])
            C[sl] = np.where(ok[..., None], C[sl] * (1 - al) + col[None, None, :] * al, C[sl])
            A[sl] = np.where(ok, A[sl] * (1 - al) + al, A[sl])
    for q in mesh:                                                           # pass 2: glow, additive (alpha-weighted), depth tested
        if q["k"] != 2:
            continue
        col, al = rgba(q["c"])
        for sl, msk, iz in tris(np.array(q["p"], float).reshape(4, 3)):
            ok = msk & (iz > ZB[sl] * 0.995)
            G[sl] = np.where(ok[..., None], G[sl] + col[None, None, :] * al * 0.55, G[sl])

    def down(a):
        return a.reshape(Hd, ss, Wd, ss, *a.shape[2:]).mean(axis=(1, 3))
    zb = ZB.reshape(Hd, ss, Wd, ss).max(axis=(1, 3))
    return down(C), down(A), down(G), zb


def raster_sprites(img, quads, cam, target, ZB):
    """pv.raster plus the depth test of the game: a sprite is hidden by a solid core in front of it, and never writes depth"""
    f, r, u = pv.basis(cam, target)
    Wd, Hd = pv.W, pv.H
    k = (Hd / 2) / math.tan(math.radians(pv.FOV / 2))
    order = sorted(range(len(quads)), key=lambda i: quads[i]["o"])          # stable: the batch order of the vertex buffer, ALPHA first, ADD last
    yy, xx = np.mgrid[0:Hd, 0:Wd].astype(np.float32)
    for qi in order:
        q = quads[qi]
        tex = pv.texture(q["t"])
        th, tw = tex.shape[:2]
        v = np.array(q["v"], dtype=np.float64)
        pts, uv = v[:, :3], v[:, 3:5]
        col = np.array([[((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255] for c in v[:, 5].astype(np.int64)])
        z = pts @ f
        if np.any(z < 0.05):
            continue
        sx = Wd / 2 + (pts @ r) / z * k
        sy = Hd / 2 - (pts @ u) / z * k
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, min(x0, x1, x2))), int(min(Wd - 1, max(x0, x1, x2))) + 1
            miny, maxy = int(max(0, min(y0, y1, y2))), int(min(Hd - 1, max(y0, y1, y2))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            X = xx[miny:maxy, minx:maxx] + 0.5
            Y = yy[miny:maxy, minx:maxx] + 0.5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            m = (a >= -1e-4) & (b >= -1e-4) & (c >= -1e-4)
            if not m.any():
                continue
            iz = np.array([1 / z[i] for i in tri])
            w_ = a * iz[0] + b * iz[1] + c * iz[2]
            m &= w_ > ZB[miny:maxy, minx:maxx] * 0.985
            if not m.any():
                continue

            def interp(vals):
                return (a * vals[0] * iz[0] + b * vals[1] * iz[1] + c * vals[2] * iz[2]) / w_
            U = interp([uv[i, 0] for i in tri])
            Vv = interp([uv[i, 1] for i in tri])
            ui = (np.mod(U, 1.0) * tw).astype(int).clip(0, tw - 1)
            vi = (np.mod(Vv, 1.0) * th).astype(int).clip(0, th - 1)
            vc = np.stack([interp([col[i, ch] for i in tri]) for ch in range(4)], axis=-1)
            s = tex[vi, ui] * vc
            al = (s[..., 3] * m)[..., None]
            region = img[miny:maxy, minx:maxx]
            if q["b"] in ("ALPHA", "WATER"):
                region[:] = region * (1 - al) + s[..., :3] * al
            else:
                region += s[..., :3] * al
    return img


def tile(fr, sc, label, sub=""):
    cam, target = sc["cam"], sc["target"]
    sd = dict(sky=sc.get("sky", [0.30, 0.34, 0.44]), figures=sc["figures"])
    img = pv.background(sd, cam, target)
    img = pv.draw_figures(img, sd, cam, target)
    zb = np.zeros((pv.H, pv.W), np.float32)
    if fr.get("mesh"):
        Cb, Ab, Gb, zb = raster_meshes(fr["mesh"], np.array(cam, float), np.array(target, float))
        img = img * (1 - Ab[..., None]) + Cb
        img = img + Gb
    img = raster_sprites(img, fr["quads"], cam, target, zb)
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, pv.W, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, pv.H - 13), sub, fill=(255, 235, 150))
    return t


def counts(fr):
    return len(fr["quads"]), len(fr.get("mesh", []))


def peak(sc, classes, layer, key, **kw):
    """peak sprite quads, mesh quads and vertices over every tick of the effect's life (the game draws once a frame; every integer age 0..duration, and the half ticks)"""
    s = dict(sc)
    s["layer"] = layer
    s.update(kw)
    s["ages"] = [a / 2 for a in range(0, 2 * int(s["duration"]) + 1)]
    data = pv.run_java(s, classes, os.path.join(pv.BUILD, f"wbc_peak_{key}_{layer}_{'_'.join(f'{k}{v}' for k, v in sorted(kw.items()))}.json"))
    spr = max(len(fr["quads"]) for fr in data["frames"])
    msh = max(len(fr.get("mesh", [])) for fr in data["frames"])
    return {"sprite_quads": spr, "mesh_quads": msh, "sprite_vertices": max(fr["vertices"] for fr in data["frames"])}


def main():
    pv.W, pv.H = CW, CH
    scenes = load_scenes()
    os.makedirs(pv.BUILD, exist_ok=True)
    classes = pv.build(["WaterBcLayer"])
    rows, budget = [], {}
    only = [a for a in sys.argv[1:] if not a.startswith("-")]
    for key, title, old_layer, name, color in ROWS:
        if only and key not in only:
            continue
        sc = dict(scenes[name])
        if color:
            sc["color"] = color
        new = pv.run_java(sc, classes, os.path.join(pv.BUILD, f"wbc_new_{key}.json"))
        mid = sc["ages"][1]
        osc = dict(sc)
        osc["layer"] = old_layer
        osc["ages"] = [mid]
        osc.pop("follow", None)
        before = pv.run_java(osc, classes, os.path.join(pv.BUILD, f"wbc_old_{key}.json"))
        bf = before["frames"][0]
        tiles = [tile(bf, sc, f"BEFORE (old layer)  t={bf['age']:.0f}  {len(bf['quads'])} quads")]
        for fr in new["frames"]:
            n, m = counts(fr)
            tiles.append(tile(fr, sc, f"BC in game  t={fr['age']:.0f}/{new['duration']}  {m} mesh + {n} sprite quads"))
        for lab, tier in (("LOD1 light mesh", 1), ("A flat-sprite fall-back", 2)):
            t = dict(sc)
            t["ages"] = [mid]
            t["tier"] = tier
            fr = pv.run_java(t, classes, os.path.join(pv.BUILD, f"wbc_tier{tier}_{key}.json"))["frames"][0]
            n, m = counts(fr)
            tiles.append(tile(fr, sc, f"{lab}  t={fr['age']:.0f}  {m} mesh + {n} sprite quads"))
        rows.append((title, tiles))
        b = {"duration": sc["duration"], "before": peak(sc, classes, old_layer, key)}
        for tier, lab in ((0, "BC_LOD0"), (1, "BC_LOD1"), (2, "A_fallback")):
            b[lab] = peak(sc, classes, "WaterBcLayer", key, tier=tier)
        v1 = pv.run_java(dict(sc, water="V1", ages=[mid]), classes, os.path.join(pv.BUILD, f"wbc_v1_{key}.json"))["frames"][0]
        b["V1_equals_old_layer"] = v1["quads"] == bf["quads"] and not v1.get("mesh")
        budget[key] = b
        print(key, json.dumps(b), flush=True)
    if only:
        return
    cols = max(len(t) for _, t in rows)
    sheet = Image.new("RGB", (CW * cols, CH * len(rows) + 30), (16, 18, 22))
    ImageDraw.Draw(sheet).text((8, 8), "WATER in the game path:  BEFORE (real old layer) | BC hybrid (real WaterBcLayer) at three ticks | LOD1 | A fall-back   same camera; software render, no bloom, not seen in a game", fill=(255, 255, 255))
    for i, (title, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            sheet.paste(t, (j * CW, 30 + i * CH))
        ImageDraw.Draw(sheet).text((6, 30 + i * CH + CH - 28), title, fill=(180, 255, 180))
    sheet.save(os.path.join(OUTDIR, "water_ingame.png"))
    with open(os.path.join(OUTDIR, "water_ingame_budget.json"), "w", encoding="utf-8") as f:
        json.dump(budget, f, indent=1)
    print("->", os.path.join(OUTDIR, "water_ingame.png"))


if __name__ == "__main__":
    main()
