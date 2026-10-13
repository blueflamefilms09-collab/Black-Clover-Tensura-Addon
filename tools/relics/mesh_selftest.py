"""Self-test of mesh_lib: builds a small sword (a lofted blade + a dozen raised parts) the way a weapon script would, writes it in the game's
format, parses it back with a parser written from WeaponRenderer.parse, checks the numbers and renders a preview.

    python3 -B tools/relics/mesh_selftest.py [out_dir]       (default: build/relics_selftest)

Exit code 0 = every check passed. Nothing here touches the repo's assets; the preview PNG lands in out_dir.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                    # noqa: E402
import preview_mesh as pm                                # noqa: E402

# ---- proportions of the test sword, in centimetres (a 1 m arming sword; each says what it stands for)
TOTAL = 100.0            # pommel butt to point
POMMEL_R, POMMEL_H = 2.6, 4.0          # the wheel pommel
GRIP_LEN, GRIP_R = 14.0, 1.45          # grip length, radius at the swell
GUARD_SPAN, GUARD_H, GUARD_T = 21.0, 2.4, 2.2   # quillon span (x), height (y), thickness (z)
BLADE_LEN, BLADE_W, BLADE_T = 78.0, 4.8, 0.95   # blade from the guard, width at the base, thickness at the base
TIP_LEN = 9.0                                    # the point: the last stretch where the blade closes
TAPER = ((0, 1.0, 1.0), (22, 0.92, 0.9), (52, 0.76, 0.74), (BLADE_LEN - TIP_LEN, 0.6, 0.6), (BLADE_LEN - 3.0, 0.3, 0.45), (BLADE_LEN, 0.0, 0.0))
BEVEL = 0.5                                      # share of the half width that is ground
FULLER_Y = (4.0, 52.0)                           # where the fuller runs
BAND_Y = (1.2, 4.2, 7.2, 10.2)                   # binding rings on the grip (from the guard)


def blade_half_t(y):
    """Half thickness of the flats at height y, from the same TAPER stations the blade is lofted with."""
    for a, b in zip(TAPER, TAPER[1:]):
        if a[0] <= y <= b[0]:
            t = (y - a[0]) / max(b[0] - a[0], 1e-9)
            return BLADE_T * 0.5 * (a[2] + (b[2] - a[2]) * t)
    return 0.0


def build():
    m = ml.Mesh("selftest_sword")
    # 1 blade: the silhouette first (a tapering hexagonal section closing to a point)
    blade = m.add(ml.Part("blade", material="steel"))
    ml.loft(blade, "y", (0, 0, 0), TAPER, ml.bevelled(BLADE_W, BLADE_T, BEVEL))
    # 2-3 fuller: two raised ridges per face, tapering at both ends
    for sgn, nm in ((1, "front"), (-1, "back")):
        part = m.add(ml.Part("fuller_" + nm, material="steel", wear=0.8))
        z = sgn * blade_half_t(30)
        ml.fuller(part, (0, FULLER_Y[0], z), (0, FULLER_Y[1], z), (0, 0, sgn), width=0.9, ridge_w=0.16, h=0.05)
    # 4 ricasso collar
    ric = m.add(ml.Part("ricasso_collar", material="gold"))
    ml.collar(ric, -0.4, 1.0, 1.4, lip=0.25, sides=12, oval=0.5)
    # 5 guard bar: an octagonal bar along x
    guard = m.add(ml.Part("guard", material="gold"))
    ml.loft(guard, "x", (0, -GUARD_H / 2 - 0.4, 0), [(-GUARD_SPAN / 2, 0.55, 0.7), (-GUARD_SPAN / 2 + 3, 1.0, 1.0), (-2.2, 1.0, 1.0), (0, 1.12, 1.12), (2.2, 1.0, 1.0), (GUARD_SPAN / 2 - 3, 1.0, 1.0), (GUARD_SPAN / 2, 0.55, 0.7)],
            ml.octagon(GUARD_H, GUARD_T, 0.55))
    # 6-7 quillon finials (domes), 8 central boss
    for sgn, nm in ((-1, "l"), (1, "r")):
        fin = m.add(ml.Part("finial_" + nm, material="gold"))
        ml.lathe(fin, "x", (sgn * GUARD_SPAN / 2, -GUARD_H / 2 - 0.4, 0), [(0, 0.8), (sgn * 0.9, 0.95), (sgn * 1.7, 0.6), (sgn * 2.3, 0.0)], sides=12)
    boss = m.add(ml.Part("guard_boss", material="gold"))
    ml.lathe(boss, "z", (0, -GUARD_H / 2 - 0.4, 0), [(-1.7, 0.0), (-1.2, 1.5), (-0.7, 1.9), (0.7, 1.9), (1.2, 1.5), (1.7, 0.0)], sides=16)
    # 9 grip: a gently swelling round loft below the guard
    grip = m.add(ml.Part("grip", material="leather", uv_mode="loft"))
    gy1 = -GUARD_H - 0.4
    ml.loft(grip, "-y", (0, gy1, 0), [(0, 1.0, 1.0), (GRIP_LEN * 0.35, 1.06, 1.04), (GRIP_LEN * 0.7, 1.12, 1.08), (GRIP_LEN, 0.95, 0.92)], ml.ellipse(GRIP_R, GRIP_R * 0.82, 12))
    # 10-13 binding rings on the grip
    for i, y in enumerate(BAND_Y):
        band = m.add(ml.Part(f"grip_band_{i}", material="bronze"))
        ml.collar(band, gy1 - y - 0.5, GRIP_R * 1.08, 0.6, lip=0.14, sides=12, axis="y", oval=0.82)
    # 14 pommel: a lathe with a flattened wheel
    pom = m.add(ml.Part("pommel", material="gold"))
    py = gy1 - GRIP_LEN
    ml.lathe(pom, "y", (0, 0, 0), [(py + 0.2, 0.9), (py - 0.4, 1.9), (py - 1.5, POMMEL_R), (py - 2.6, POMMEL_R * 0.92), (py - POMMEL_H, 1.0), (py - POMMEL_H - 0.5, 0.0)], sides=16)
    # 15-17 rivets on the boss, 18 gem, 19 engraved line on the guard
    for i, a in enumerate((0, 120, 240)):
        riv = m.add(ml.Part(f"rivet_{i}", material="silver"))
        ml.rivet(riv, (1.35 * math.cos(math.radians(a)), -GUARD_H / 2 - 0.4 + 1.35 * math.sin(math.radians(a)), 1.2), 0.22, (0, 0, 1), 8)
    jewel = m.add(ml.Part("gem", material="crystal", glow=True, smooth=0))
    ml.gem(jewel, (0, -GUARD_H / 2 - 0.4, 2.15), 0.55, sides=6, axis="z")
    eng = m.add(ml.Part("guard_line", material="gold"))
    ml.ridge_pair(eng, (-9.0, -GUARD_H / 2 - 0.4 + 0.1, GUARD_T / 2 * 0.98), (-3.2, -GUARD_H / 2 - 0.4 + 0.1, GUARD_T / 2 * 0.98), (0, 0, 1), 0.3, 0.1, 0.05)
    ml.ridge_pair(eng, (3.2, -GUARD_H / 2 - 0.4 + 0.1, GUARD_T / 2 * 0.98), (9.0, -GUARD_H / 2 - 0.4 + 0.1, GUARD_T / 2 * 0.98), (0, 0, 1), 0.3, 0.1, 0.05)
    return m


def extra_checks():
    """sweep (a curved blade), box, mirrored / rotated copies, project_uv on one part, resample between different sections."""
    m = ml.Mesh("extras")
    sab = m.add(ml.Part("saber", material="steel"))
    R, sweepdeg, n = 60.0, 38.0, 14                                       # a scimitar: arc radius 60 cm over 38 degrees
    path = [(R - R * math.cos(math.radians(sweepdeg * i / n)), R * math.sin(math.radians(sweepdeg * i / n)), 0.0) for i in range(n + 1)]
    ml.sweep(sab, path, ml.bevelled(4.0, 0.8, 0.5), scales=lambda t: (1.0 - 0.45 * t, 1.0 - 0.3 * t) if t < 0.9 else (max(0.0, (1 - t) * 4.5), max(0.0, (1 - t) * 4.5)))
    blk = m.add(ml.Part("block", material="iron"))
    ml.box(blk, (-3, -9, -1), (3, -2, 1), chamfer=0.4)
    m.add(sab.translated(8, 0, 0, "saber_copy").mirrored("x", "saber_mirrored"))
    m.add(blk.rotated("z", 30, (0, 0, 0), "block_rot"))
    cone = m.add(ml.Part("cone", material="bone"))                       # add_ring + fan + cap by hand, winding as built (no ensure_outward)
    ring = cone.add_ring([(x, 20.0, z) for (x, z) in ml.circle(2.0, 10)])
    cone.fan(ring, (0, 26.0, 0))
    cone.cap(ring, outward=(0, -1, 0))
    check(cone.is_closed() and cone.signed_volume() > 0, f"hand-built cone (add_ring, fan, cap) is closed and outward ({cone.signed_volume():.3f})")
    mix = m.add(ml.Part("mixed", material="wood"))
    ml.loft(mix, "y", (0, 10, 0), [(0, 1, 1), (4, 0.8, 0.8), (8, 1.2, 1.2)], lambda s: ml.resample(ml.bevelled(3, 1, 0.3), 12) if s < 5 else ml.round(1.4, 12))
    void = m.add(ml.Part("void_patch", layer=1))
    ml.box(void, (-1, -6, 1.2), (1, -3, 1.3))
    for p in m.parts:
        check(p.is_closed(), f"extras {p.name}: closed")
        check(p.signed_volume() > 0, f"extras {p.name}: outward winding (volume {p.signed_volume():.3f})")
    p = m.part("saber")
    ppu = ml.project_uv(p, (10, 20, 300, 400), size=1024)
    check(all(10 / 8 - 1e-6 <= u <= 310 / 8 + 1e-6 and 20 / 8 - 1e-6 <= v <= 420 / 8 + 1e-6 for uv in p.UV for (u, v) in uv), f"project_uv keeps one part inside its rect (ppu {ppu:.2f})")
    try:
        ml.loft(ml.Part("bad"), "y", (0, 0, 0), [0, 1], lambda s: ml.round(1, 6 if s < 0.5 else 8))
        check(False, "a section with a different point count is refused")
    except ValueError:
        check(True, "a section with a different point count is refused with a clear error")
    at = ml.Atlas(512)
    at.layout(m)
    at.paint(m)
    m.place_on_sprite(os.path.join(ml.ASSETS, "textures", "item", "magebane_shield.png"), fit=True)
    check(m.place_cfg[2] == 90.0, f"place_on_sprite(fit=True): upright {m.place_cfg}")
    q = list(m.iter_quads(atlas=at))
    check(any(x[0] == 1 for x in q) and not any(x[0] == 2 for x in q), "a layer-1 part is written as void quads (no glow copy)")
    pm.render_preview(m, None, os.path.join(OUT, "selftest_extras.png"), atlas=at, views=("profile", "three_quarter"), panel=(420, 420))


# ================================================================ checks
FAILS = []


def check(cond, msg):
    print(("ok    " if cond else "FAIL  ") + msg)
    if not cond:
        FAILS.append(msg)


OUT = ""


def main(out_dir):
    global OUT
    OUT = out_dir
    os.makedirs(out_dir, exist_ok=True)
    m = build()
    m.finish()
    for p in m.parts:                                                    # winding: closed parts must have a positive volume, quads must not be degenerate
        if p.is_closed():
            check(p.signed_volume() > 0, f"{p.name}: closed, outward winding (volume {p.signed_volume():.3f})")
        for f in range(len(p.F)):
            n = p.face_normal(f)
            if abs(np.linalg.norm(n) - 1) > 1e-6:
                check(False, f"{p.name}: face {f} normal not unit")
                break
    for p in m.parts:                                                    # open parts (studs): the faces must point away from the part's middle
        if not p.is_closed():
            c = np.mean(p.V, axis=0)
            out_ = sum(float(np.dot(p.face_normal(f), np.mean([p.V[i] for i in p.F[f]], axis=0) - c)) for f in range(len(p.F)))
            check(out_ > 0, f"{p.name}: open part, faces point outward ({out_:.3f})")
    check(len(m.parts) >= 13, f"{len(m.parts)} parts (the blade + >= 12 raised parts)")
    atlas = ml.Atlas(1024)
    ppu = atlas.layout(m)
    atlas.paint(m)
    # a glow strip on the gem so the glow layer is exercised, and an engraved line + a glow line through the Atlas drawing helpers
    atlas.line(m.part("guard"), (-9, -1.6, 1.1), (-3, -1.6, 1.1), 0.12, "z", 1, dark=0.6)
    gm = m.part("gem")
    for g in gm.groups:
        x, y, w, h = g.rect
        atlas.glow[y:y + h, x:x + w] = ml.glow_from_mask(np.ones((h, w), np.float32), (110, 220, 255), 1.0, 1.0)
    m.glow_cfg = None
    # every UV must sit inside its group's rect and inside the 128 space
    bad = 0
    k = atlas.k
    for p in m.parts:
        for g in p.groups:
            x, y, w, h = g.rect
            for f in g.faces:
                for (u, v) in p.UV[f]:
                    if not (x / k - 1e-6 <= u <= (x + w) / k + 1e-6 and y / k - 1e-6 <= v <= (y + h) / k + 1e-6):
                        bad += 1
    check(bad == 0, f"all UVs inside their rects (ppu {ppu:.2f} px/cm, {len(atlas.groups)} groups)")
    check(all(0 <= u <= 128 and 0 <= v <= 128 for p in m.parts for uv in p.UV for (u, v) in uv), "all UVs inside the 128 x 128 UV space")
    icon = os.path.join(ml.ASSETS, "textures", "item", "bloodbound_katana.png")
    cfg = m.place_on_sprite(icon, real_m=TOTAL / 100.0)
    check(cfg[2] > 40 and cfg[2] < 50, f"placed on the icon diagonal: {cfg}")
    st = m.stats(atlas)
    print("stats", st)
    txt = os.path.join(out_dir, "selftest_sword.txt")
    n = m.write_txt(txt, atlas)
    skin_paths = atlas.save("selftest_sword", root=out_dir)
    # ---- parse back with the game's parser
    d = pm.parse_game_txt(txt)
    check(len(d["quads"]) == n == sum(st["quads"].values()), f"quad count {len(d['quads'])} == written {n} == stats")
    check(d["place"] is not None and len(d["place"]) == 5, "place line parsed")
    check(d["glow"] is not None, "glow line parsed (the gem has a glow layer)")
    lens = [len(l.split()) for l in open(txt) if l.startswith("q ")]
    check(set(lens) == {25}, "every q line has 25 tokens (layer + 4 x 5 + normal), as WeaponRenderer.parse expects")
    nrm = np.array([q[3][0] for q in d["quads"]])
    check(np.all(np.abs(np.linalg.norm(nrm, axis=1) - 1) < 2e-3), "every quad normal has unit length")
    check(all(np.isfinite(q[1]).all() and np.isfinite(q[2]).all() for q in d["quads"]), "all numbers finite")
    check(any(q[0] == 2 for q in d["quads"]), "layer 2 (glow) quads present")
    # geometric normal vs stored normal: they must agree (winding is what the game culls on)
    worst = 1.0
    for (layer, P, UV, N) in d["quads"]:
        gn = np.cross(P[2] - P[0], P[3] - P[1])
        if np.linalg.norm(gn) > 1e-6:
            worst = min(worst, float(np.dot(gn / np.linalg.norm(gn), N[0])))
    check(worst > 0.99, f"stored normals agree with the quad winding (worst dot {worst:.4f})")
    # layers 0/2 differ only by glow copies; layer 1 absent here
    check(not any(q[0] == 1 for q in d["quads"]), "no void layer in this mesh")
    # vertex-normal variant
    txt2 = os.path.join(out_dir, "selftest_sword_vn.txt")
    m.write_txt(txt2, atlas, normals="vertex")
    d2 = pm.parse_game_txt(txt2)
    check(len(d2["quads"]) == len(d["quads"]), "qn variant: same quad count")
    nn = np.array([q[3] for q in d2["quads"]])
    check(np.all(np.abs(np.linalg.norm(nn, axis=2) - 1) < 2e-3), "qn variant: unit vertex normals")
    # smoothing really smooths: a round grip's vertex normals differ from its face normals, a hexagon blade's bevels stay hard
    gp = m.part("grip")
    dots = [float(np.dot(gp.N[f][0], gp.face_normal(f))) for f in range(len(gp.F))]
    check(min(dots) < 0.999 and min(dots) > 0.9, f"grip normals are smoothed across its facets (min dot {min(dots):.4f})")
    bl = m.part("blade")
    check(len({tuple(np.round(bl.N[f][0], 2)) for f in range(len(bl.F))}) > 4, "blade keeps hard bevel edges (distinct normals)")
    # the existing meshes of the repo parse with the same parser (format compatibility)
    ex = pm.parse_game_txt(os.path.join(ml.ASSETS, "weapon_meshes", "bloodbound_katana.txt"))
    check(len(ex["quads"]) > 100 and ex["place"] is not None, "an existing repo mesh parses with the same parser")
    # ---- the preview
    out = os.path.join(out_dir, "selftest_preview.png")
    ref = os.path.join(out_dir, "reference_stub.png")
    from PIL import Image, ImageDraw
    im = Image.new("RGBA", (200, 520), (0, 0, 0, 0))
    ImageDraw.Draw(im).polygon([(100, 10), (112, 60), (112, 330), (88, 330), (88, 60)], fill=(190, 196, 206, 255))
    ImageDraw.Draw(im).rectangle([40, 330, 160, 346], fill=(212, 168, 66, 255))
    ImageDraw.Draw(im).rectangle([92, 346, 108, 460], fill=(96, 62, 38, 255))
    im.save(ref)
    ml.preview(m, ref, out, atlas=atlas)
    check(os.path.exists(out), "preview written: " + out)
    pm.render_preview(m, None, os.path.join(out_dir, "selftest_guard_closeup.png"), atlas=atlas, views=("profile", "three_quarter", "edge", "top"), focus=(-14.5, 4.0), panel=(380, 380))
    out_vn = os.path.join(out_dir, "selftest_preview_vertex_normals.png")
    pm.render_preview(m, None, out_vn, atlas=atlas, views=("profile", "three_quarter"), normals="vertex")
    # the same file previewed through the .txt reader (what a builder does after write_txt into the assets)
    check(all(os.path.exists(p) for p in skin_paths), "skin and glow PNGs written: " + ", ".join(os.path.basename(p) for p in skin_paths))
    extra_checks()
    print("\n%d checks failed" % len(FAILS) if FAILS else "\nALL CHECKS PASSED")
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ml.ROOT, "build", "relics_selftest")))
