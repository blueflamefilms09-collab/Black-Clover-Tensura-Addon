"""Builds the upgraded meshes, skins, low-detail meshes and effect specs of the twelve God melee (a) items. Reproducible (every random draw is seeded).

    python3 -B tools/visual_upgrade/god_melee_a/build.py [ids...]           (no ids = all twelve)

Writes assets/nusmp/weapon_meshes/up/<id>.txt (+ <id>_lod.txt, and the crossbow's draw states), assets/nusmp/textures/entity/weapon/up/<id>*.png and
assets/nusmp/visual_upgrade/<id>.json. The shipped meshes, skins, sprites and item models are NOT touched (the old files stay; the renderer falls back to them with the client
flag visual.upgrade = false).
"""
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import common as C                                                     # noqa: E402
import ukit as U                                                       # noqa: E402
import specs as SP                                                     # noqa: E402
import items_a                                                         # noqa: E402

C_KEEP = r"^(blade|head|shaft|haft|stock|limb_[lr]?|prong_[a-z]|face|rim|grip|core|strand_\d|hub|blade_[lr])$"
ITEMS = dict(items_a.ITEMS)
for _mod in ("items_a2", "items_b", "items_b2", "items_c", "items_c2"):
    try:
        ITEMS.update(__import__(_mod).ITEMS)
    except ImportError:
        pass


def build_item(item_id):
    it = ITEMS[item_id]
    t0 = time.time()
    full = U.run(item_id, it.builder, it.extras, it.post, lod=False, glow_cfg=getattr(it, "glow_cfg", None), tube_cap=getattr(it, "tube_cap", None), keep_words=C_KEEP, soft_keep=tuple(getattr(it, "lod_keep", ())))
    st = C.mesh_stats(C.mesh_path(item_id, up=True))
    # the low-detail mesh: at most 15% of the quads of the full one (glow-layer copies included): drop small parts until it fits
    target = int(C.LOD_FRACTION * st["quads_total"])
    budget = max(40, int(target * 0.78))
    best = None
    seen = set()
    for _ in range(8):
        if budget in seen:
            break
        seen.add(budget)
        U.run(item_id, it.builder, it.extras, it.post, lod=True, lod_budget=budget, glow_cfg=getattr(it, "glow_cfg", None), tube_cap=getattr(it, "tube_cap", None), keep_words=C_KEEP, soft_keep=tuple(getattr(it, "lod_keep", ())))
        ls = C.mesh_stats(os.path.join(C.UP_MESH_DIR, item_id + "_lod.txt"))
        if ls["quads_total"] <= target and (best is None or budget > best[0]):
            best = (budget, ls)
        if target * 0.7 <= ls["quads_total"] <= target:
            break
        budget = int(budget * target / max(1, ls["quads_total"]) * (0.97 if ls["quads_total"] > target else 1.0))
    if best is not None and best[1] is not ls:
        U.run(item_id, it.builder, it.extras, it.post, lod=True, lod_budget=best[0], glow_cfg=getattr(it, "glow_cfg", None), tube_cap=getattr(it, "tube_cap", None), keep_words=C_KEEP, soft_keep=tuple(getattr(it, "lod_keep", ())))
        ls = best[1]
    spec = SP.build(item_id, **it.SPEC)
    SP.write(spec)
    print("== %s: full %d quads / %d unique vertices (cap %d), lod %d quads (%.1f%%), %.1fs" % (item_id, st["quads_total"], st["unique_vertices"], C.GOD_UNIQUE_VERTEX_CAP,
                                                                                   ls["quads_total"], 100.0 * ls["quads_total"] / st["quads_total"], time.time() - t0))
    return st, ls


if __name__ == "__main__":
    ids = sys.argv[1:] or C.IDS
    for i in ids:
        build_item(i)
