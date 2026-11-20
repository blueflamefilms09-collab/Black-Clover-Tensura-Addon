"""Hellblaze, The Demon Sin, rebuilt as a TRUE katana with a true saya (scripted bpy, headless; docs/heroweapon/hero_katanas.md). Replaces hellblaze_katana.py's meshes; that script and
the 4.61.0 files (weapon_meshes/up/classic_thin, textures/entity/weapon/up/classic_thin) stay.

    python3 -B tools/blender/hero_hellblaze.py [--only katana,lod,saya] [--trial DIR] [--fast] [--samples N] [--bake PX] [--threads N]

Three meshes, same ids, same place line and glow tint as before:
  hellblaze_the_demon_sin__katana      black steel, shinogi ridge, kissaki, a dark-red hamon with a glowing edge, three jagged sin veins climbing from the habaki, a bo-hi with a line of light;
                                       a thin tsuba cut with a ring of eight thorns (sukashi windows, thorn rim), black ito over ray skin with a dark-red thread, sin-eye menuki, an eye on the kashira, a sageo
  hellblaze_the_demon_sin__katana_lod  the same weapon at about a tenth of the quads (WeaponUpgrade draws it from 12 blocks); the painted details cost nothing at either level
  hellblaze_the_demon_sin__saya        the sheath that hangs in the off hand: lacquered black, follows the blade's sori and width (1.2x), koiguchi, kojiri, kurikata, flame veins, sageo
Skins (textures/entity/weapon/up/): <id>.png, <id>_glow.png (a GREY mask: the colour is the tint of the mesh's `glow` line, dark red, shifted to purple on a hit), <id>_veins.png (softened mask).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import hero_katanas as HK  # noqa: E402  (patches katanas.CLASSIC / katanas.tsuba)
import katanas as KT  # noqa: E402
import katanas_specs as KS  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import bl_lib as bl  # noqa: E402
import hellblaze_katana as HB  # noqa: E402  (post(): place / grip lines, grey glow, veins skin)

KT.MESH_DIR = os.path.join(bl.MESH_DIR, "up")          # the visual-upgrade folders: katanas.build reads the old header from up/classic_thin, katanas.finish writes to up/
KT.SKIN_DIR = os.path.join(bl.SKIN_DIR, "up")
ID = HB.ID


# ================================================================ lore: the sin
def sin_veins(c, P, rng):
    """Three jagged veins climbing from the habaki on each face (painted: no quads), a line of light in the groove and a glowing edge."""
    for s in (1, -1):
        for v in range(3):
            y0 = c.yh + 2.0 + 1.6 * v
            ln = 15.0 + 11.0 * (v + 1) + 6.0 * rng.random()
            a = 0.30 + 0.18 * v
            path = []
            n = int(ln / 1.1)
            for i in range(n):
                t = i / max(1, n - 1)
                a += 0.05 * (rng.random() - 0.5) + 0.012 * math.sin(i * 1.3 + v)
                path.append((y0 + ln * t, min(0.78, max(0.12, a))))
            ribbon = HK.ribbon_on
            ribbon(c, "vein%d" % v, path, lambda t: 0.13 * (1.0 - 0.8 * t) + 0.02, P["line"], s, lift=0.07, thick=0.03)
    ys = [c.yh + c.Bl * (0.06 + 0.90 * i / 40) for i in range(41)]
    KT.paint(c, "edgeglow", ys, lambda y: 0.02, lambda y: 0.07, P["line"], lift=0.09, thick=0.03, part="hamon")


def sin_eye(c, P, rng, lod=False):
    """The sin-eye menuki on both flats of the grip, an eye on the kashira, and a short sageo with a tassel."""
    r0, sq, zf, ym = HK.grip_dims(c)
    if not lod:
        for s in (1, -1):
            z = s * (zf + 0.05)
            K.blob("eye%+d" % s, (0.0, ym, z), (0.62, 1.7, 0.30), P["accent"], seed=11, lump=0.03, rings=5, segs=8, part="menuki")
            K.blob("pupil%+d" % s, (0.0, ym, z + s * 0.2), (0.14, 1.1, 0.12), P["accent_dark"], seed=12, lump=0.02, rings=4, segs=6, part="menuki")
        K.blob("kashira_eye", (0.0, c.y0 - 0.3, 0.0), (1.0, 0.8, 1.0), P["accent"], seed=11, lump=0.0, rings=4, segs=8, part="kashira_gem")
        top = V((r0 * 0.9, c.yt1 - 0.4, 0.5))
        cp = [top, top + V((1.8, -3.0, 0.5)), top + V((2.6, -9.0, 0.2)), top + V((2.6, -15.0, 0.0)), top + V((2.6, -19.0, 0.0))]
        K.tapered("sageo", cp, 0.34, 0.28, P["ito2"], sides=5, sub=3, part="tassel")
        K.tapered("tassel", [cp[-1], cp[-1] + V((0, -2.5, 0)), cp[-1] + V((0, -5.2, 0))], 0.5, 0.12, P["ito2"], sides=6, sub=1, part="tassel")
    else:
        K.blob("kashira_eye", (0.0, c.y0 - 0.4, 0.0), (0.9, 0.9, 0.9), P["accent"], seed=11, lump=0.0, rings=3, segs=6, part="kashira_gem")


def katana_extras(c, P, rng):
    KS.grooves(c, P, P["line"])
    sin_veins(c, P, rng)
    sin_eye(c, P, rng)


def spec(**kw):
    S = dict(id=ID + "__katana", seed=81, div=18.0, sori=0.024, kasane=0.28, bohi=True, hamon=("midare", 0.10, None), tsuba=("thorn", 3.6), tail=2.9, nomenuki=True, hamon_base=0.30, turns=8.0,
             extras=katana_extras,
             pal=dict(steel=(34, 34, 42), steel_wear=(120, 120, 140), rough=0.30, dirt=0.3, hamon=(108, 14, 26), hamon2=(70, 8, 16), hamon_emit=(255, 30, 40), hamon_glow=0.8,
                      line=(255, 26, 36), line_s=1.7, fit=(78, 14, 22), fit_wear=(200, 70, 80), fit2=(30, 6, 10), tsuba=(26, 26, 33), tsuba_wear=(118, 112, 126), kash=(26, 26, 33),
                      ito=(16, 16, 22), ito2=(120, 12, 26), ray=(22, 22, 28), menuki=(150, 12, 28), accent=(255, 40, 50), accent_s=1.2, accent_dark=(3, 2, 4)))
    S.update(kw)
    return S


# ================================================================ the katana and its lod: the budget of the visual-upgrade katana is 1800 quad lines (HellblazeKatanaAssetsTest), the lod 320
def build(S, opts, lod=False):
    """Blade (a few rows: the shinogi is baked), thin tsuba with thorn windows, a grip core whose ito lattice is baked from bake-only cords, a kashira. Every painted detail is the same at both levels and free."""
    C.begin(opts)
    C.weights(blade=1.7, hamon=1.4, hamon_line=1.0, blade_paint=1.0, grip=1.2, guard=0.8, habaki=0.8, kashira=0.8, kashira_gem=0.6, menuki=0.6, tassel=0.5, fuchi=0.6)
    src = os.path.join(KT.MESH_DIR, KT.CLASSIC, S["id"] + ".txt")
    _, ymin, ymax = KT.old_extent(src)
    y0, L = ymin + S.get("tail", 0.0), ymax - ymin - S.get("tail", 0.0)
    c = KT.Ctx(S, y0, L)
    P = KT.mats(S)
    rng = K.rng(S["seed"] + 7)
    if lod:
        us = (0.0, 0.3, 0.62, c.uk, c.uk + 0.55 * (1 - c.uk), 1.0)
        vs, hs = (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0)
    else:
        us = [0.0, 0.06, 0.14, 0.24, 0.35, 0.47, 0.59, 0.71, 0.82, c.uk, c.uk + 0.3 * (1 - c.uk), c.uk + 0.65 * (1 - c.uk), 1.0]
        vs, hs = c.vs, c.hs
    ys = [c.yh + c.Bl * u for u in us]
    rows = [(V((c.edge(y), y, 0.0)), V((c.spine(y), y, 0.0))) for y in ys]
    hv = [c.half(y) for y in ys]
    K.strip("blade", rows, P["steel"], vs, hs, lambda t: hv[min(len(hv) - 1, int(round(t * (len(hv) - 1))))], part="blade", up=(0, 0, 1), angle=14)
    KT.hamon(c, ys, P, S["hamon"][0], S["hamon"][1], None, seed=S["seed"], line=P["line"], base=S["hamon_base"])
    sin_veins(c, P, rng)
    KS.grooves(c, P, P["line"])
    KT.SIDES = 8 if lod else 12
    if lod:
        Lb.lathe("habaki", [(0.5 * c.w0 * 1.1, c.hab_y0), (0.5 * c.w0 * 1.05, c.hab_y0 + c.hab_h)], P["fit"], "habaki", sides=6, squash=0.45)
        pts = [HK.polar(3.6 * (0.70 + 0.30 * abs(math.cos(4 * a)) ** 2.6), a) for a in (TAU * k / 12 for k in range(12))]
        K.xf(K.plate("tsuba", pts, 0.0, c.tsuba_t, P["tsuba"], part="guard"), K.frame((0, c.tsuba_y, 0), (0, 1, 0), (0, 0, 1)))
    else:
        KT.habaki(c, P)
        HK.hero_tsuba(c, P, "thorn", 3.6, 1.0, 1, lite=True)
    r0 = 0.56 * c.w0
    Lb.lathe("grip", [(r0, c.yt0), (r0 * 1.04, 0.5 * (c.yt0 + c.yt1)), (r0, c.yt1)] if lod else [(r0 * (1.0 + 0.04 * math.sin(math.pi * i / 4.0)), c.yt0 + (c.yt1 - c.yt0) * i / 4.0) for i in range(5)],
             P["ito"], "grip", sides=KT.SIDES, squash=0.8)
    if not lod:                                                  # the ito lattice: two opposite helices of cord, baked into the grip (bake-only, no quads)
        rmid = r0 * 1.05
        for k, (hand, mat, ph, lift) in enumerate(((1, P["ito2"], 0.0, 0.0), (-1, P["ito"], 0.9, 0.05))):
            ob = KT.helix("ito%d" % k, c.yt0 + 0.3, c.yt1 - 0.3, rmid, rmid * 0.8, S.get("turns", 8.0), hand, mat, 0.15 * c.w0, phase=ph, lift=lift, seed=3 + k)
            ob["detail"] = True
        Lb.lathe("fuchi", [(r0 * 1.04, c.yt1 - 0.4), (r0 * 1.18, c.yt1 + 0.1), (r0 * 1.06, c.yt1 + c.fuchi_h)], P["fit"], "fuchi", sides=KT.SIDES, squash=0.8)
    Lb.lathe("kashira", [(0.02, c.y0), (r0 * 1.0, c.y0 + 0.6 * c.kashira_h), (r0 * 1.0, c.yt0)] if lod else [(0.02, c.y0), (r0 * 0.7, c.y0 + 0.1 * c.kashira_h), (r0 * 1.06, c.y0 + 0.7 * c.kashira_h), (r0 * 1.0, c.yt0)],
             P["kash"], "kashira", sides=KT.SIDES, squash=0.8)
    sin_eye(c, P, rng, lod=lod)
    KT.SIDES = 32
    print("[%s] %s: quads ~%d" % (S["id"], "lod" if lod else "katana", K.quad_estimate()))
    return c


# ================================================================ the saya
def line_on(c, name, a_fn, b_fn, y0, y1, mat, T, s, step=2.0, lift=0.06):
    """A thin bake-only strip down face s of the sheath between the offsets a_fn(y) < b_fn(y) (cm across, from the sheath's centre line)."""
    n = max(3, int((y1 - y0) / step))
    rows = []
    for i in range(n + 1):
        y = y0 + (y1 - y0) * i / n
        rows.append((V((a_fn(y), y, s * (T(y) + lift))), V((b_fn(y), y, s * (T(y) + lift)))))
    K.strip("%s%+d" % (name, s), rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.03, part="saya_paint", up=(0, 0, s), detail=True, angle=60)


def build_saya(S, opts):
    C.begin(opts)
    C.weights(saya=1.6, koiguchi=0.7, kojiri=0.7, kurikata=0.6, saya_sageo=0.5, saya_paint=1.0)
    src = os.path.join(KT.MESH_DIR, KT.CLASSIC, ID + "__saya.txt")
    _, ymin, ymax = KT.old_extent(src)
    c = KT.Ctx(S, 0.0, 99.6)                                  # the katana's own numbers: the sheath follows its sori and width
    P = KT.mats(S)
    lac = Lb.paint("hz_lacquer", rgb(14, 14, 19), rgb(90, 90, 110), 91, rough=0.18, mottle=rgb(26, 26, 34))
    Ls = c.Bl + 8.0
    cx = lambda y: -c.Kt * (y / c.Bl) ** 2                                                    # noqa: E731  the blade's own parabola, carried on past the point
    hw = lambda y: 0.5 * c.width(min(c.ytip, c.yh + y)) * 1.20 + 0.20                          # noqa: E731
    T = lambda y: 1.30 * c.half(min(c.ytip, c.yh + y)) + 0.40                                  # noqa: E731
    ys = [0, 5, 12, 21, 30, 39, 48, 57, 65, 71, Ls - 2.5, Ls]
    K._loft("saya_body", [bl.ring(y, bl.rrect(hw(y), T(y), min(hw(y), T(y)) * 0.8), ox=cx(y)) for y in ys], lac, (True, True), "saya")
    K._loft("koiguchi", [bl.ring(y, bl.rrect(hw(y) + 0.26, T(y) + 0.26, 0.9), ox=cx(y)) for y in (-0.6, 0.0, 3.6, 4.4)], P["fit"], (True, True), "koiguchi")
    K._loft("kojiri", [bl.ring(y, bl.rrect(max(0.2, (hw(y) + 0.2) * f), max(0.2, (T(y) + 0.2) * f), 0.7), ox=cx(y)) for y, f in ((Ls - 6.4, 1.0), (Ls - 3.5, 0.98), (Ls - 1.0, 0.8), (Ls + 0.9, 0.34))],
            P["fit"], (True, True), "kojiri")
    ky = 11.0
    Lb.lathe("kurikata", [(0.01, 0.0), (0.9, 0.25), (1.2, 1.0), (1.05, 1.8), (0.01, 2.0)], P["tsuba"], "kurikata", sides=8, origin=(hw(ky) + cx(ky) - 0.3, ky, 0.0), axis="x")
    Lb.lathe("kurikata_bar", [(0.5, -1.2), (0.5, 1.2)], P["tsuba"], "kurikata", sides=6, origin=(hw(ky) + cx(ky) + 1.2, ky, 0.0), axis="z")
    for s in (1, -1):                                                                          # thin lines of light and flame veins (baked only)
        line_on(c, "line", lambda y: cx(y) - 0.7 * hw(y), lambda y: cx(y) - 0.52 * hw(y), 7.0, 70.0, P["line"], T, s)
        line_on(c, "lineb", lambda y: cx(y) + 0.50 * hw(y), lambda y: cx(y) + 0.62 * hw(y), 7.0, 70.0, P["hamon"], T, s)
        for k in range(3):
            y0 = 66.0 - 5.0 * k
            ph = k * 1.3

            line_on(c, "flame%d" % k, lambda y, ph=ph: cx(y) + 0.25 * hw(y) * math.sin(y * 0.9 + ph) - 0.07, lambda y, ph=ph: cx(y) + 0.25 * hw(y) * math.sin(y * 0.9 + ph) + 0.07, y0, Ls - 4.0, P["line"], T, s,
                    step=0.9)
    cp = [V((hw(ky) + cx(ky) + 1.4, ky, 0.0)), V((hw(ky) + 3.2, 6.0, 1.0)), V((hw(ky) + 4.2, 1.0, 0.0)), V((hw(ky) + 3.4, -4.0, -0.8))]
    K.tapered("saya_sageo", cp, 0.34, 0.28, P["ito2"], sides=5, sub=3, part="saya_sageo")
    K.tapered("saya_tassel", [cp[-1], cp[-1] + V((0, -2.0, 0)), cp[-1] + V((0, -5.0, 0))], 0.5, 0.12, P["ito2"], sides=6, sub=1, part="saya_sageo")
    print("[%s] saya: Ls=%.1f cm, quads ~%d" % (ID + "__saya", Ls, K.quad_estimate()))
    return c


def finish_saya(S, c, trial, bake, skin):
    """katanas.finish for a mesh that has no tsuka: no grip / place patch (hellblaze_katana.post writes the place line)."""
    names = []
    for o in KT.bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o["part"] not in names:
            names.append(o["part"])
    C.PART_NAMES[0] = names
    mesh_dir = os.path.join(trial, "weapon_meshes") if trial else KT.MESH_DIR
    skin_dir = os.path.join(trial, "textures", "entity", "weapon") if trial else KT.SKIN_DIR
    os.makedirs(mesh_dir, exist_ok=True)
    os.makedirs(skin_dir, exist_ok=True)
    hdr = os.path.join(KT.MESH_DIR, KT.CLASSIC, S["id"] + ".txt")
    gl = KT.old_glow(hdr)
    real = K.bl.bpy
    K.bl.bpy = C._BpyProxy()
    try:
        mesh, atlas, stats = bl.finish(S["id"], glow=gl, header_from=hdr, bake=bake, skin=skin, out_mesh=mesh_dir, out_skin=skin_dir, preview=None, write=True, budget=10 ** 6, cage=0.5, smooth=KT.SMOOTH)
    finally:
        K.bl.bpy = real
    K.shrink_pngs(skin_dir, S["id"])
    return mesh_dir, skin_dir


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default="katana,lod,saya")
    ap.add_argument("--trial")
    ap.add_argument("--fast", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--bake", type=int, default=2048)
    ap.add_argument("--threads", type=int, default=2)
    a = ap.parse_args(argv)
    C.THREADS[0] = max(1, min(4, a.threads))
    opts = {"samples": 3 if a.fast else a.samples}
    trial = os.path.join(a.trial, "up") if a.trial else None
    for key in a.only.split(","):
        if key == "katana":
            S = spec()
            c = build(S, opts)
            KT.finish(S, c, trial=trial, bake=256 if a.fast else a.bake, skin=256 if a.fast else 1024)
            md, sd = (os.path.join(trial, "weapon_meshes"), os.path.join(trial, "textures", "entity", "weapon")) if trial else (KT.MESH_DIR, KT.SKIN_DIR)
            HB.post(S["id"], md, sd)
        elif key == "lod":
            S = spec(id=ID + "__katana_lod")
            c = build(S, opts, lod=True)
            KT.finish(S, c, trial=trial, bake=256 if a.fast else min(a.bake, 1024), skin=256)
            md, sd = (os.path.join(trial, "weapon_meshes"), os.path.join(trial, "textures", "entity", "weapon")) if trial else (KT.MESH_DIR, KT.SKIN_DIR)
            HB.post(S["id"], md, sd, veins=False)
        elif key == "saya":
            S = spec(id=ID + "__saya")
            c = build_saya(S, opts)
            md, sd = finish_saya(S, c, trial, 256 if a.fast else a.bake, 256 if a.fast else 1024)
            HB.post(S["id"], md, sd, hand=False, veins=False)


if __name__ == "__main__":
    main(sys.argv[1:])
