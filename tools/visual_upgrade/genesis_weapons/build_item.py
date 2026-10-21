"""Builds one item of the genesis_weapons upgrade: full mesh + LOD mesh + skins + the effect spec.

    python3 -B tools/visual_upgrade/genesis_weapons/build_item.py blackrazor [--preview]

An item module `item_<id>.py` provides:
    TEX             the skin size (1024 / 2048)
    STATES          [(mesh id, state tag)] for items with several meshes (a bow's draw stages); default [(id, '')]
    build(it, state)    fills the Item (parts, groups, flares, painters, spec lists); `state` is the tag of the mesh
    spec(it)            the extra spec entries ({'acts': ..., 'swing': ..., 'draw': ..., ...}) merged into assets/nusmp/visual_upgrade/<id>.json
"""
import importlib
import json
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import vu_item as vi                                                      # noqa: E402
from vu_item import *                                                     # noqa: E402,F401,F403

LOD_MAX_RATIO = 0.15


def _quads(it):
    return sum(len(p.F) for p in it.mesh.parts if p.layer == 0)


def build_state(mod, item_id, mesh_id, state, lod, d_override=None):
    it = vi.Item(item_id, lod=lod, tex=mod.TEX, mesh_id=mesh_id + ("_lod" if lod else ""))
    if d_override:
        it.D = d_override
    old = os.path.join(OLD_MESH, mesh_id + ".txt")
    it.place_like(old)
    mod.build(it, state)
    return it


def run(item_id, preview=False, quiet=False):
    t0 = time.time()
    mod = importlib.import_module("item_" + item_id)
    states = getattr(mod, "STATES", [(item_id, "")])
    spec_groups, spec_extra = {}, {}
    results = []
    rest = rest_lod = None
    for (mesh_id, state) in states:
        full = build_state(mod, item_id, mesh_id, state, False)
        nq = _quads(full)
        lod = None
        if rest is None:
            for d in (0.16, 0.12, 0.09, 0.07, 0.055, 0.045):
                lod = build_state(mod, item_id, mesh_id, state, True, d)
                lod_d = d
                if _quads(lod) <= LOD_MAX_RATIO * nq:
                    break
        else:
            lod = build_state(mod, item_id, mesh_id, state, True, lod_d)          # a state mesh has the topology of the rest mesh, so the same detail level
        if rest is None:                                                       # the rest mesh lays out and paints the skin; a state mesh (a draw stage) shares it
            atlas = full.finish(None, None)
            lod_atlas = lod.finish(None, None)
            rest, rest_lod = full, lod
            vi.save_textures(atlas, VU_TEX, item_id)
            vi.save_textures(lod_atlas, VU_TEX, item_id + "_lod", half_glow=False)
        else:
            atlas = full.adopt_uv(rest)
            lod_atlas = lod.adopt_uv(rest_lod)
        ox, oy, ang, sc, ymin = full.old_place
        y0 = full.core[0] if full.core and full.core[0] is not None else ymin
        counts = vi.write_vu(full, atlas, os.path.join(VU_MESH, mesh_id + ".txt"), item_id, mesh_id + "_lod", (ox, oy, ang, sc, y0))
        lcounts = vi.write_vu(lod, lod_atlas, os.path.join(VU_MESH, mesh_id + "_lod.txt"), item_id + "_lod", None, (ox, oy, ang, sc, y0))
        results.append((mesh_id, counts, lcounts, full))
        if mesh_id == item_id:
            spec_groups = full.groups
            spec_extra = {"layers": full.layers, "idle": full.idle, "draw": full.draw, "swing": full.swing, "acts": full.acts, "sound_loop": full.sound,
                          "flares": len(full.flares), "glow": {"rgb": full.glow[0], "speed": full.glow[1], "min": full.glow[2], "max": full.glow[3]}}
        if not quiet:
            body = counts[0]
            print("  %-34s body %5d  glow %4d  veins %4d  halo %3d  void %3d | LOD body %4d (%.1f%%)  skin %d" % (
                mesh_id, body, counts[2], counts[3], counts[4], counts[1], lcounts[0], 100.0 * lcounts[0] / max(1, body), mod.TEX))
    spec = {"id": item_id, "slice": SLICE, "grade": "genesis", "version": 1, "mesh": item_id, "lod": {"mesh": item_id + "_lod", "near": 12.0, "far": 24.0}}
    spec.update(spec_extra)
    spec["groups"] = spec_groups
    if hasattr(mod, "spec"):
        mod.spec(spec)
    os.makedirs(SPEC_DIR, exist_ok=True)
    __import__("runpy").run_path(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "custom_particles.py"))["convert"](spec)   # 1.110.0: every particle token of the table becomes one of the mod's own (tools/visual_upgrade/custom_particles.py)
    with open(os.path.join(SPEC_DIR, item_id + ".json"), "w", newline="\n") as fh:
        json.dump(spec, fh, indent=1)
        fh.write("\n")
    if not quiet:
        print("  %s done in %.1fs" % (item_id, time.time() - t0))
    return results


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for a in args:
        run(a, "--preview" in sys.argv)
