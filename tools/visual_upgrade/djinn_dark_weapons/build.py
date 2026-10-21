"""Builds the upgraded 3D models of the slice `djinn_dark_weapons` (the 36 dark Djinn weapons, release 1.108.0):

    python3 -B tools/visual_upgrade/djinn_dark_weapons/build.py [djinn id ...]       (no ids = all 36; deterministic, idempotent)

For every dark Djinn weapon: reads the shipped mesh and skins (the old files stay untouched), builds the new parts on its frame (dkit / ditems), lays the enlarged old islands and the new
parts out on a new 1024 skin, paints, and writes the NEW files under up/ (mesh, level-of-detail mesh, orbiting piece, skin, core glow, vein glow) and the data table
assets/nusmp/visual_upgrade/<item>.json (read by client/GodMeleeBRender and client/vupgrade/godb).
Hook: tools/relics/djinn_weapons.py ends with one line that runs this script for the same ids (the old assets are made by that tool; these are made next to them).
"""
import json
import os
import sys
import time

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import dlib as D                                    # noqa: E402
import dkit as K                                    # noqa: E402
import ditems as I                                  # noqa: E402

ml, gb = D.ml, D.gb
GOD_WELDED_CAP = 2500                                # the brief's God cap read as WELDED vertices of the full-detail mesh including the orbiting pieces (as in god_melee_b)
TOTAL_QUAD_CAP = 2300                                # target ceiling of quads in the full-detail mesh (orbit instances included)
ORBIT_COUNT = 6


def geometry(did, detail, old):
    C = K.Ctx(I.item_id(did), I.SPECS[did], old, detail)
    I.recipe(C)
    I.orbit(C)
    return C


def counts(C):
    body = sum(len(p.F) for p in C.m.parts if not p.name.startswith("orbit_"))
    orb = sum(len(p.F) for p in C.m.parts if p.name.startswith("orbit_"))
    return body, orb


def total_quads(did, detail, old):
    C = geometry(did, detail, old)
    body, orb = counts(C)
    return old.body_count + body + ORBIT_COUNT * orb


def sized(did, old, total_target):
    """The smallest detail factor whose full-detail quad total (old + new + orbit instances) reaches the target (5x the old quads, so the mesh ratio is at least 5)."""
    lo, hi = 0.25, 3.2
    if total_quads(did, lo, old) >= total_target:
        return lo
    best = hi
    for _ in range(10):
        mid = (lo + hi) / 2
        if total_quads(did, mid, old) >= total_target:
            best, hi = mid, mid
        else:
            lo = mid
    return best


def build(did, verbose=True, cap=TOTAL_QUAD_CAP):
    t0 = time.time()
    iid = I.item_id(did)
    old = D.OldMesh(iid)
    old_q = old.body_count
    target = max(600, min(5 * old_q, cap))
    detail = sized(did, old, target)
    C = geometry(did, detail, old)
    sp = C.sp
    mesh = C.m
    atlas = D.DAtlas(D.SIZE)
    ppu = atlas.layout_old(mesh, old)
    atlas.paint(mesh)
    g2 = np.zeros_like(atlas.glow)
    for fn in C.post:
        fn(atlas, g2)
    D.tint_old(atlas, old, C.col, 0.18)
    D.detail_old(atlas, old)
    gb.detail_pass(mesh, atlas)
    D.paint_old_veins(atlas, g2, old, C.vein, density=0.55)
    body_parts = [p for p in mesh.parts if not p.name.startswith("orbit_")]
    orbit_parts = [p for p in mesh.parts if p.name.startswith("orbit_")]
    mb, mo = ml.Mesh(iid), ml.Mesh(iid + "_orbit")
    mb.parts, mo.parts = body_parts, orbit_parts
    path, nq = D.write_mesh(iid, old, mb, atlas, g2, body=True)
    opath, noq = D.write_mesh(iid + "_orbit", old, mo, atlas, g2, body=False)
    lod_path = path.replace(".txt", "_lod.txt")
    lq, bq = D.make_lod(path, lod_path)
    D.save_textures(iid, atlas, g2)
    all_y = np.concatenate([q["P"][:, 1] for q in old.quads if q["layer"] == 0] + [np.array([v[1] for p in body_parts for v in p.V])])
    ymin, ymax = float(all_y.min()), float(all_y.max())
    place = old.head["place"]
    maxr = max(np.abs(np.concatenate([q["P"] for q in old.quads])[:, [0, 2]]).max(), max(np.abs(np.array(p.V)[:, [0, 2]]).max() for p in body_parts))
    data = I.json_table(sp, place[3], ymin, ymax, ORBIT_COUNT, maxr + 5.5, nq, noq, lq)
    os.makedirs(D.DATA_DIR, exist_ok=True)
    json.dump(data, open(os.path.join(D.DATA_DIR, iid + ".json"), "w", encoding="utf-8", newline="\n"), indent=2)
    welded = gb.welded_count(path)
    full_welded = welded + ORBIT_COUNT * gb.welded_count(opath)
    if verbose:
        print("%-14s %-10s old %3d -> body %4d (+%d x %d orbit = %4d) welded %4d | lod %3d (%.1f%%) | detail %.2f | ppu %.1f k%.2f | %.1fs%s" % (
            did, sp["kind"], old_q, nq, ORBIT_COUNT, noq, nq + ORBIT_COUNT * noq, full_welded, lq, 100.0 * lq / max(1, bq), detail, ppu, atlas.old_k, time.time() - t0,
            "  OVER CAP" if full_welded > GOD_WELDED_CAP else ""))
    return {"welded": full_welded, "quads": nq + ORBIT_COUNT * noq}


def build_capped(did):
    """Builds, and when the full-detail mesh is over the welded cap builds again with a lower quad target (at most four tries)."""
    cap = TOTAL_QUAD_CAP
    r = None
    for _ in range(4):
        r = build(did, verbose=True, cap=cap)
        if r["welded"] <= GOD_WELDED_CAP - 20:
            break
        cap = int(r["quads"] - (r["welded"] - (GOD_WELDED_CAP - 40)) * 1.1)
    return r


def main(only):
    for did in I.ORDER:
        if not only or did in only:
            build_capped(did)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
