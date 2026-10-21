"""Builds the upgraded 3D models of the slice `god_melee_b`:

    python3 -B tools/visual_upgrade/god_melee_b/build.py [id ...]         (no ids = all nine; deterministic, idempotent)

For every weapon: runs its ORIGINAL builder with the slice's hooks (lib.run_old_builder), paints on top (items_a / items_b), writes the NEW files under `up/`
(mesh, level-of-detail mesh, orbiting piece, skin, core glow, vein glow) and the data table assets/nusmp/visual_upgrade/<id>.json. The old files are not touched.
"""
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lib                                          # noqa: E402
import items_a                                      # noqa: E402
import items_b                                      # noqa: E402
from items_a import PASSES, AMBIENT, ANIMATIONS     # noqa: E402

ITEMS = {i["id"]: i for i in items_a.ITEMS_A + items_b.ITEMS_B}
ORDER = ["aurliels_seraph_halberd", "chronarchs_pendulum_sabre", "deepwatch_harpoon", "orials_starfall_staff", "reflected_kings_mirrorblade", "rimeheart_greatblade",
         "sanguine_wardens_cleaver", "valkarins_stormlance", "paradox"]
GOD_WELDED_CAP = 2500              # the brief's God cap, read as WELDED vertices of the full-detail mesh including the orbiting pieces (see docs/visual_upgrade/god_melee_b.md)


def build(item_id, verbose=True):
    it = ITEMS[item_id]
    place, grip = lib.old_header(item_id)
    cap = lib.run_old_builder(item_id, it["geometry"], size=it.get("atlas", lib.ATLAS_SIZE))
    m, atlas = cap["m"], cap["atlas"]
    g2 = np.zeros_like(atlas.glow)
    it["paint"](m, atlas, g2)
    lib.detail_pass(m, atlas)
    from mesh_lib import Mesh
    orbit_parts = [p for p in m.parts if p.name.startswith("orbit_")]
    body_parts = [p for p in m.parts if not p.name.startswith("orbit_")]
    mb = Mesh(item_id)
    mb.parts = body_parts
    mo = Mesh(item_id + "_orbit")
    mo.parts = orbit_parts
    glow_cfg = cap["glow"]
    path, nq = lib.write_mesh("%s" % item_id, mb, atlas, g2, place, grip, glow_cfg)
    opath, noq = lib.write_mesh("%s_orbit" % item_id, mo, atlas, g2, [0.0, 0.0, 90.0, 0.02, 0.0], None, glow_cfg)
    cq = lib.count_quads(path)
    nq = cq[0] + cq[1]
    lod_path = path.replace(".txt", "_lod.txt")
    lq, bq = lib.make_lod(path, lod_path)
    lib.save_textures(item_id, atlas, g2)
    ymin, ymax = mb.y_range()
    spec = dict(it["spec"])
    n_orbit_inst = sum(o["count"] for o in spec["orbiters"])
    oq = lib.count_quads(opath)
    data = {
        "item": item_id, "version": 1,
        "mesh": "up/" + item_id, "lod_mesh": "up/" + item_id + "_lod", "orbit_mesh": "up/" + item_id + "_orbit",
        "axis": {"y0": round(ymin, 3), "y1": round(ymax, 3), "length_blocks": round((ymax - ymin) * place[3] * 0.85, 3)},
        "passes": PASSES, "ambient_layers": AMBIENT, "animations": ANIMATIONS,
        "palette": spec["palette"], "veins": spec["veins"], "halo": spec["halo"], "core": {"flicker": 0.10, "flicker_speed": 0.37},
        "orbiters": [dict(o, mesh="up/" + item_id + "_orbit") for o in spec["orbiters"]],
        "swing_trail": spec["swing_trail"], "particles": {"layers": spec["particles"], "ground_radius": spec["ground_radius"], "shaft_height": spec["shaft_height"]},
        "lod": {"near_blocks": 12.0, "additive_blocks": 24.0, "particle_blocks": 32.0},
        "budget": {"body_quads": nq, "orbit_instances": n_orbit_inst, "orbit_piece_quads": oq[0] + oq[1], "lod_quads": lq},
    }
    os.makedirs(lib.DATA_DIR, exist_ok=True)
    __import__("runpy").run_path(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "custom_particles.py"))["convert"](data)   # 1.110.0: every particle token of the table becomes one of the mod's own (tools/visual_upgrade/custom_particles.py)
    json.dump(data, open(os.path.join(lib.DATA_DIR, item_id + ".json"), "w", encoding="utf-8", newline="\n"), indent=2)
    welded = lib.welded_count(path)
    full_welded = welded + n_orbit_inst * lib.welded_count(opath)
    if verbose:
        print("%-30s body %4d quads (welded %4d) +%d x %d orbit | lod %3d (%.1f%%) | atlas %d px @ %.1f px/cm | full-detail welded %d%s" % (
            item_id, nq, welded, n_orbit_inst, oq[0] + oq[1], lq, 100.0 * lq / max(1, bq), atlas.size, atlas.ppu, full_welded, "  OVER CAP" if full_welded > GOD_WELDED_CAP else ""))
    return data


def main(only):
    for i in ORDER:
        if not only or i in only:
            build(i)


if __name__ == "__main__":
    main(set(sys.argv[1:]))
