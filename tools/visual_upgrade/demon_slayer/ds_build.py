"""Builds the Genesis Demon-Slayer upgrade: full mesh + LOD mesh + skins + the effect spec, then (optionally) the measurements and the previews.

    python3 -B tools/visual_upgrade/demon_slayer/ds_build.py [--measure] [--previews] [--quick]

Writes (all NEW, next to the old files):
    src/main/resources/assets/nusmp/weapon_meshes_vu/demon_slayer_sword.txt and demon_slayer_sword_lod.txt
    src/main/resources/assets/nusmp/textures/entity/weapon_vu/demon_slayer_sword{,_glow,_veins}.png and demon_slayer_sword_lod{,_glow,_veins}.png   (the shared halo.png is the genesis_weapons one)
    src/main/resources/assets/nusmp/visual_upgrade/demon_slayer_sword.json
Deterministic. The hook line in tools/gen_weapon_models.py (end of main) calls main() of this module.
"""
import importlib
import json
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ds_common import *                                                     # noqa: E402,F401,F403
import vu_item as vi                                                        # noqa: E402

LOD_MAX_RATIO = 0.15


def _quads(it):
    return sum(len(p.F) for p in it.mesh.parts if p.layer == 0)


def build_state(mod, lod, d=None):
    it = vi.Item(ID, lod=lod, tex=mod.TEX, mesh_id=ID + ("_lod" if lod else ""))
    if d:
        it.D = d
    mod.build(it, "")
    return it


def run(quiet=False):
    t0 = time.time()
    mod = importlib.import_module("item_demon_slayer_sword")
    full = build_state(mod, False)
    nq = _quads(full)
    lod = None
    for d in (0.16, 0.12, 0.09, 0.07, 0.055, 0.045):
        lod = build_state(mod, True, d)
        if _quads(lod) <= LOD_MAX_RATIO * nq:
            break
    atlas = full.finish(None, None)
    lod_atlas = lod.finish(None, None)
    vi.save_textures(atlas, VU_TEX, ID)
    vi.save_textures(lod_atlas, VU_TEX, ID + "_lod", half_glow=False)
    place = mod.PLACE
    counts = vi.write_vu(full, atlas, os.path.join(VU_MESH, ID + ".txt"), ID, ID + "_lod", place)
    lcounts = vi.write_vu(lod, lod_atlas, os.path.join(VU_MESH, ID + "_lod.txt"), ID + "_lod", None, place)
    spec = {"id": ID, "slice": SLICE, "grade": "genesis", "version": 1, "mesh": ID, "lod": {"mesh": ID + "_lod", "near": 12.0, "far": 24.0},
            "layers": full.layers, "idle": full.idle, "draw": full.draw, "swing": full.swing, "acts": full.acts, "sound_loop": full.sound, "flares": len(full.flares),
            "glow": {"rgb": full.glow[0], "speed": full.glow[1], "min": full.glow[2], "max": full.glow[3]}, "groups": full.groups}
    mod.spec(spec)
    os.makedirs(SPEC_DIR, exist_ok=True)
    __import__("runpy").run_path(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "custom_particles.py"))["convert"](spec)   # 1.110.0: every particle token of the table becomes one of the mod's own (tools/visual_upgrade/custom_particles.py)
    with open(os.path.join(SPEC_DIR, ID + ".json"), "w", newline="\n") as fh:
        json.dump(spec, fh, indent=1)
        fh.write("\n")
    if not quiet:
        print("  %-24s body %5d  glow %4d  veins %4d  halo %3d  void %3d | LOD body %4d (%.1f%%)  skin %d  %.1fs" % (
            ID, counts[0], counts[2], counts[3], counts[4], counts[1], lcounts[0], 100.0 * lcounts[0] / max(1, counts[0]), mod.TEX, time.time() - t0))
    return full, lod


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    run()
    if "--measure" in argv or "--previews" in argv:
        import ds_measure
        ds_measure.main("after")
    if "--previews" in argv:
        import ds_preview
        print("preview", ds_preview.sheet())
        print("preview", ds_preview.faces())


if __name__ == "__main__":
    main()
