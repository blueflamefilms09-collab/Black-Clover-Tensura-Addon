"""Visual-upgrade measuring tool of the slice `djinn_dark_weapons` (release 1.108.0): the 36 dark Djinn weapons (God grade) turned into NUMBERS, so "better looking" is a measured proxy.

    python3 -B tools/visual_upgrade/djinn_dark_weapons/measure.py baseline      docs/visual_upgrade/djinn_dark_weapons_baseline.json (the repo BEFORE the upgrade)
    python3 -B tools/visual_upgrade/djinn_dark_weapons/measure.py after         docs/visual_upgrade/djinn_dark_weapons_after.json (+ ratios and the V score)
    python3 -B tools/visual_upgrade/djinn_dark_weapons/measure.py table         the markdown table of the doc

It is the measuring tool of the slice god_melee_b with the item list of this slice (the same components, the same weights, so the numbers of the two slices are comparable):
mesh = quads at full detail (body + void layer + every orbiting instance), texture = painted texels (alpha > 0) of the skin and every emissive map, passes, ambient layers, idle animations.
Baseline conventions (read off the source, said plainly): a dark Djinn weapon is a `DjinnWeaponItem`, not a relic definition, so it has NO grade shimmer (client/relics/RelicGradeFx looks at
relic definitions only): its baseline ambient layer count is 0 (the ratio divides by max(1, before)); its one idle animation is the glow pulse of VariantRules.pulse; it has no swing trail;
each of its two acts has its own effect shape (DjinnFx) which stays.
"""
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
DOCS = os.path.join(ROOT, "docs", "visual_upgrade")
SLICE = "djinn_dark_weapons"

ROSTER = json.load(open(os.path.join(ROOT, "docs", "djinn_roster_36.json"), encoding="utf-8"))
NAMES = [d["id"] for d in ROSTER]                                  # roster (ordinal) order
ITEM_IDS = ["djinn_weapon_%s_dark" % n for n in NAMES]
KIND = {("djinn_weapon_%s_dark" % d["id"]): d["weapon"]["kind"] for d in ROSTER}

BASELINE_RUNTIME = {"ambient_layers": 0, "animations": 1, "trail": 0, "act_vfx": True}
WEIGHTS = {"mesh": 0.20, "texture": 0.20, "passes": 0.15, "ambient": 0.25, "animation": 0.20}
WEIGHTS_BRIEF = {"mesh": 0.35, "texture": 0.20, "passes": 0.15, "ambient": 0.20, "animation": 0.10}


def read_mesh(path):
    q = {0: 0, 1: 0, 2: 0}
    pts = []
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if not v or v[0] == "#":
            continue
        if v[0] in ("q", "qn"):
            layer = int(v[1])
            q[layer] += 1
            step = 5 if v[0] == "q" else 8
            if layer in (0, 1):
                for k in range(4):
                    o = 2 + k * step
                    pts.append((float(v[o]), float(v[o + 1]), float(v[o + 2])))
    return q, (np.array(pts, np.float64) if pts else np.zeros((0, 3)))


def welded(pts):
    return int(len({(round(x, 2), round(y, 2), round(z, 2)) for x, y, z in pts})) if len(pts) else 0


def tex_stats(path):
    if not os.path.exists(path):
        return None
    a = np.asarray(Image.open(path).convert("RGBA"))
    return {"file": os.path.relpath(path, ASSETS).replace(os.sep, "/"), "size": [a.shape[1], a.shape[0]], "texels": int(a.shape[0] * a.shape[1]), "painted": int((a[..., 3] > 0).sum())}


def load_data(item_id):
    p = os.path.join(ASSETS, "visual_upgrade", item_id + ".json")
    return json.load(open(p, encoding="utf-8")) if os.path.exists(p) else None


def measure(item_id, mesh_id, upgraded):
    q, pts = read_mesh(os.path.join(ASSETS, "weapon_meshes", mesh_id + ".txt"))
    wd = os.path.join(ASSETS, "textures", "entity", "weapon")
    skin = tex_stats(os.path.join(wd, mesh_id + ".png"))
    glows = [t for t in (tex_stats(os.path.join(wd, mesh_id + s + ".png")) for s in ("_glow", "_glow2")) if t]
    data = load_data(item_id) if upgraded else None
    out = {"item": item_id, "grade": "GOD", "kind": "3D hand weapon (MAIN_HAND, %s, loaned by the dark Djinn Equip)" % KIND[item_id], "mesh_file": "weapon_meshes/%s.txt" % mesh_id}
    body = q[0] + q[1]
    orbit_quads = orbit_instances = 0
    lod_vertices = None
    if data:
        for o in data.get("orbiters", []):
            oq, _p = read_mesh(os.path.join(ASSETS, "weapon_meshes", o["mesh"] + ".txt"))
            orbit_quads += (oq[0] + oq[1]) * o["count"]
            orbit_instances += o["count"]
        lq, _p = read_mesh(os.path.join(ASSETS, "weapon_meshes", data["lod_mesh"] + ".txt"))
        lod_vertices = 4 * (lq[0] + lq[1])
    out["mesh"] = {"quads_body": q[0], "quads_void": q[1], "quads_glow_layer": q[2], "quads_full_detail": body + orbit_quads, "orbit_instances": orbit_instances,
                   "vertices_body": 4 * body, "vertices_welded": welded(pts), "vertices_full_detail": 4 * (body + orbit_quads), "lod_vertices": lod_vertices}
    painted = (skin["painted"] if skin else 0) + sum(g["painted"] for g in glows)
    out["texture"] = {"skin": skin, "emissive_maps": glows, "skin_size_texels": skin["texels"] if skin else 0, "painted_skin_texels": skin["painted"] if skin else 0, "painted_texels_all_maps": painted}
    if data:
        passes, ambient, anims = list(data["passes"]), list(data["ambient_layers"]), list(data["animations"])
        out["swing_trail"] = bool(data.get("swing_trail"))
    else:
        passes = ["body", "core_glow"] + (["void"] if q[1] else [])
        ambient = ["grade_shimmer"] * BASELINE_RUNTIME["ambient_layers"]
        anims = ["glow_pulse"] * BASELINE_RUNTIME["animations"]
        out["swing_trail"] = False
    out["unique_act_vfx"] = True
    out["passes"] = {"count": len(passes), "list": passes}
    out["ambient_layers"] = {"count": len(ambient), "list": ambient}
    out["idle_animations"] = {"count": len(anims), "list": anims}
    return out


def score(before, after, weights):
    r = {
        "mesh": after["mesh"]["quads_full_detail"] / max(1, before["mesh"]["quads_full_detail"]),
        "texture": after["texture"]["painted_texels_all_maps"] / max(1, before["texture"]["painted_texels_all_maps"]),
        "passes": after["passes"]["count"] / max(1, before["passes"]["count"]),
        "ambient": after["ambient_layers"]["count"] / max(1, before["ambient_layers"]["count"]),
        "animation": after["idle_animations"]["count"] / max(1, before["idle_animations"]["count"]),
    }
    return {k: round(x, 3) for k, x in r.items()}, round(sum(weights[k] * r[k] for k in r), 3)


def md_table():
    b = {r["item"]: r for r in json.load(open(os.path.join(DOCS, SLICE + "_baseline.json"), encoding="utf-8"))["items"]}
    a = json.load(open(os.path.join(DOCS, SLICE + "_after.json"), encoding="utf-8"))
    print("| item | kind | quads before -> after (orbit pieces included) | welded after | LOD quads | painted texels before -> after, all maps | passes | ambient | idle anim | V (ours) | V (brief) |")
    print("|---|---|---|---|---|---|---|---|---|---|---|")
    for r in a["items"]:
        x = b[r["item"]]
        print("| %s | %s | %d -> %d | %d | %d | %d -> %d (x%.1f) | %d -> %d | %d -> %d | %d -> %d | %.2f | %.2f |" % (
            r["item"].replace("djinn_weapon_", "").replace("_dark", ""), KIND[r["item"]], x["mesh"]["quads_full_detail"], r["mesh"]["quads_full_detail"], r["mesh"]["vertices_welded"] + 0,
            r["mesh"]["lod_vertices"] // 4, x["texture"]["painted_texels_all_maps"], r["texture"]["painted_texels_all_maps"], r["ratios"]["texture"], x["passes"]["count"], r["passes"]["count"],
            x["ambient_layers"]["count"], r["ambient_layers"]["count"], x["idle_animations"]["count"], r["idle_animations"]["count"], r["V_after"], r["V_after_brief_weights"]))


def main(mode):
    if mode == "table":
        md_table()
        return
    os.makedirs(DOCS, exist_ok=True)
    if mode == "baseline":
        rows = [measure(i, i, False) for i in ITEM_IDS]
        doc = {"slice": SLICE, "state": "baseline (before the upgrade; the repo at the start of release 1.108.0)", "runtime_constants": BASELINE_RUNTIME, "items": rows}
        json.dump(doc, open(os.path.join(DOCS, SLICE + "_baseline.json"), "w", encoding="utf-8", newline="\n"), indent=2)
        print("baseline: %d items" % len(rows))
        return
    base = json.load(open(os.path.join(DOCS, SLICE + "_baseline.json"), encoding="utf-8"))
    bmap = {r["item"]: r for r in base["items"]}
    rows, table = [], []
    for i in ITEM_IDS:
        a = measure(i, "up/" + i, True)
        ratios, v = score(bmap[i], a, WEIGHTS)
        _r2, v2 = score(bmap[i], a, WEIGHTS_BRIEF)
        a["ratios"], a["V_before"], a["V_after"], a["V_after_brief_weights"] = ratios, 1.0, v, v2
        rows.append(a)
        table.append({"item": i, "ratios": ratios, "V_after": v, "V_after_brief_weights": v2})
    doc = {"slice": SLICE, "state": "after the upgrade (1.108.0)", "weights": WEIGHTS, "weights_brief_example": WEIGHTS_BRIEF,
           "formula": "V = sum(weight * after/max(1,before)) over mesh, texture, passes, ambient, animation; V_before = 1", "items": rows, "ratio_table": table}
    json.dump(doc, open(os.path.join(DOCS, SLICE + "_after.json"), "w", encoding="utf-8", newline="\n"), indent=2)
    for t in table:
        print("%-34s V=%.2f (brief %.2f) %s" % (t["item"], t["V_after"], t["V_after_brief_weights"], t["ratios"]))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "baseline")
