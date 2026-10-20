"""Visual-upgrade measuring tool of the slice `god_melee_b` (release 1.102.0): turns the files a weapon is drawn from into NUMBERS, so "better looking" is a measured proxy.

    python3 -B tools/visual_upgrade/god_melee_b/measure.py baseline      writes docs/visual_upgrade/god_melee_b_baseline.json (the state of the repo BEFORE the upgrade)
    python3 -B tools/visual_upgrade/god_melee_b/measure.py after         writes docs/visual_upgrade/god_melee_b_after.json (+ the ratio table, and the V score)

What is measured per weapon (all read from the shipped assets; nothing is guessed except the runtime constants below, which are read off the source and named):
  mesh        quads per layer of assets/nusmp/weapon_meshes/<mesh>.txt (0 body, 1 void, 2 glow copy), vertices (4 per quad, as WeaponRenderer emits them), welded vertices,
              plus the instances of the orbiting pieces (data table assets/nusmp/visual_upgrade/<id>.json) drawn at full detail
  texture     skin size and PAINTED texels (alpha > 0) of the skin and of every emissive map
  passes      draw passes of the item: body, the additive core glow, the void (if the mesh has void quads), and the upgraded item's extra passes (veins, halo, orbiters)
  ambient     ambient effect layers that run while the weapon is held (baseline: the grade shimmer of client/relics/RelicGradeFx, the only one a God weapon had)
  animation   idle animation channels (baseline: the glow pulse of VariantRules.pulse, the only one)
  trail/act   a swing trail (baseline: none), unique act VFX (the VfxShape of each act, which stays)

V (the visual score): the weighted sum of the component RATIOS after / before, so V_before = 1 by construction and V_after is the ratio the brief asks about (>= 5 for God).
The weights are documented in docs/visual_upgrade/god_melee_b.md and in WEIGHTS below.
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

SLICE = "god_melee_b"

# id -> (grade, slot, class, wave, source)
ITEMS = {
    "aurliels_seraph_halberd": ("GOD", "MAIN_HAND", "glaive", "Wave78", "rift pocket boss weapon"),
    "chronarchs_pendulum_sabre": ("GOD", "MAIN_HAND", "sabre", "Wave78", "rift pocket boss weapon"),
    "deepwatch_harpoon": ("GOD", "MAIN_HAND", "spear", "Wave78", "rift pocket boss weapon"),
    "orials_starfall_staff": ("GOD", "MAIN_HAND", "staff", "Wave78", "rift pocket boss weapon"),
    "reflected_kings_mirrorblade": ("GOD", "MAIN_HAND", "longsword", "Wave78", "rift pocket boss weapon"),
    "rimeheart_greatblade": ("GOD", "MAIN_HAND", "greatsword", "Wave78", "rift pocket boss weapon"),
    "sanguine_wardens_cleaver": ("GOD", "MAIN_HAND", "cleaver", "Wave78", "rift pocket boss weapon"),
    "valkarins_stormlance": ("GOD", "MAIN_HAND", "spear", "Wave78", "rift pocket boss weapon"),
    "paradox": ("GOD", "MAIN_HAND", "staff", "Wave30", "artifact staff of the God of Time"),
}

# The runtime constants of the BEFORE state, read off the source (client/WeaponRenderer.renderByItem, client/relics/RelicGradeFx, client/relics/item/VariantRules.pulse):
BASELINE_RUNTIME = {
    "ambient_layers": 1,        # RelicGradeFx.onClientTick: a GOD holder shimmers (one mote per 8 ticks). The ground column of a dropped relic is not a held layer.
    "animations": 1,            # the glow pass breathes (VariantRules.pulse); nothing else of the weapon moves
    "trail": 0,                 # no swing trail exists for relic weapons
    "act_vfx": True,            # each act has its own VfxShape (kept)
}

# The weights of V. Documented in docs/visual_upgrade/god_melee_b.md (mesh is held low because the performance guard caps it; ambient and animation are the cheap, LOD-able layers).
WEIGHTS = {"mesh": 0.20, "texture": 0.20, "passes": 0.15, "ambient": 0.25, "animation": 0.20}
# the weights of the brief's own example (mesh .35, texture .2, glow .15, ambient .2, animation .1), reported next to ours for transparency
WEIGHTS_BRIEF = {"mesh": 0.35, "texture": 0.20, "passes": 0.15, "ambient": 0.20, "animation": 0.10}


def read_mesh(path):
    """quads per layer, vertex positions of the body/void layers, the place line."""
    q = {0: 0, 1: 0, 2: 0}
    pts = []
    place = None
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if not v or v[0] == "#":
            continue
        if v[0] == "place":
            place = [float(x) for x in v[1:6]]
        if v[0] in ("q", "qn"):
            layer = int(v[1])
            q[layer] += 1
            step = 5 if v[0] == "q" else 8
            if layer in (0, 1):
                for k in range(4):
                    o = 2 + k * step
                    pts.append((float(v[o]), float(v[o + 1]), float(v[o + 2])))
    return q, (np.array(pts, np.float64) if pts else np.zeros((0, 3))), place


def welded(pts):
    if not len(pts):
        return 0
    return int(len({(round(x, 2), round(y, 2), round(z, 2)) for x, y, z in pts}))


def tex_stats(path):
    if not os.path.exists(path):
        return None
    im = Image.open(path).convert("RGBA")
    a = np.asarray(im)
    return {"file": os.path.relpath(path, ASSETS).replace(os.sep, "/"), "size": list(im.size), "texels": int(im.size[0] * im.size[1]), "painted": int((a[..., 3] > 0).sum())}


def load_data(item_id):
    p = os.path.join(ASSETS, "visual_upgrade", item_id + ".json")
    return json.load(open(p, encoding="utf-8")) if os.path.exists(p) else None


def measure(item_id, mesh_id, upgraded):
    grade, slot, cls, wave, src = ITEMS[item_id]
    mp = os.path.join(ASSETS, "weapon_meshes", mesh_id + ".txt")
    q, pts, _place = read_mesh(mp)
    wd = os.path.join(ASSETS, "textures", "entity", "weapon")
    skin = tex_stats(os.path.join(wd, mesh_id + ".png"))
    glows = [t for t in (tex_stats(os.path.join(wd, mesh_id + s + ".png")) for s in ("_glow", "_glow2")) if t]
    data = load_data(item_id) if upgraded else None
    out = {"item": item_id, "grade": grade, "kind": "3D hand weapon (%s, %s, %s)" % (slot, cls, src), "wave": wave, "mesh_file": "weapon_meshes/%s.txt" % mesh_id}
    body = q[0] + q[1]
    orbit_quads = 0
    orbit_instances = 0
    lod_vertices = None
    if data:
        for o in data.get("orbiters", []):
            oq, _p, _pl = read_mesh(os.path.join(ASSETS, "weapon_meshes", o["mesh"] + ".txt"))
            orbit_quads += (oq[0] + oq[1]) * o["count"]
            orbit_instances += o["count"]
        lq, _p, _pl = read_mesh(os.path.join(ASSETS, "weapon_meshes", data["lod_mesh"] + ".txt"))
        lod_vertices = 4 * (lq[0] + lq[1])
    out["mesh"] = {"quads_body": q[0], "quads_void": q[1], "quads_glow_layer": q[2], "quads_full_detail": body + orbit_quads, "orbit_instances": orbit_instances,
                   "vertices_body": 4 * body, "vertices_welded": welded(pts), "vertices_full_detail": 4 * (body + orbit_quads), "lod_vertices": lod_vertices}
    painted = (skin["painted"] if skin else 0) + sum(g["painted"] for g in glows)
    out["texture"] = {"skin": skin, "emissive_maps": glows, "skin_size_texels": skin["texels"] if skin else 0, "painted_skin_texels": skin["painted"] if skin else 0,
                      "painted_texels_all_maps": painted}
    if data:
        passes = list(data["passes"])
        ambient = list(data["ambient_layers"])
        anims = list(data["animations"])
        out["swing_trail"] = bool(data.get("swing_trail"))
        out["unique_act_vfx"] = True
    else:
        passes = ["body", "core_glow"] + (["void"] if q[1] else [])
        ambient = ["grade_shimmer"] * BASELINE_RUNTIME["ambient_layers"]
        anims = ["glow_pulse"] * BASELINE_RUNTIME["animations"]
        out["swing_trail"] = False
        out["unique_act_vfx"] = BASELINE_RUNTIME["act_vfx"]
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
    v = sum(weights[k] * r[k] for k in r)
    return {k: round(x, 3) for k, x in r.items()}, round(v, 3)


def md_table():
    """The before -> after table of docs/visual_upgrade/god_melee_b.md (markdown), from the two JSON files."""
    b = {r["item"]: r for r in json.load(open(os.path.join(DOCS, SLICE + "_baseline.json"), encoding="utf-8"))["items"]}
    a = json.load(open(os.path.join(DOCS, SLICE + "_after.json"), encoding="utf-8"))
    print("| item | quads before -> after (full detail, orbit pieces included) | welded vertices after | LOD quads | painted texels before -> after, all maps | skin px | passes | ambient layers | idle animations | V (our weights) | V (brief's example weights) |")
    print("|---|---|---|---|---|---|---|---|---|---|---|")
    for r in a["items"]:
        i = r["item"]
        x = b[i]
        print("| %s | %d -> %d | %d | %d | %d -> %d (x%.1f) | %d -> %d | %d -> %d | %d -> %d | %d -> %d | %.2f | %.2f |" % (
            i, x["mesh"]["quads_full_detail"], r["mesh"]["quads_full_detail"], r["mesh"]["vertices_welded"], r["mesh"]["lod_vertices"] // 4, x["texture"]["painted_texels_all_maps"],
            r["texture"]["painted_texels_all_maps"], r["ratios"]["texture"], round(x["texture"]["skin_size_texels"] ** 0.5), round(r["texture"]["skin_size_texels"] ** 0.5), x["passes"]["count"],
            r["passes"]["count"], x["ambient_layers"]["count"], r["ambient_layers"]["count"], x["idle_animations"]["count"], r["idle_animations"]["count"], r["V_after"], r["V_after_brief_weights"]))


def main(mode):
    if mode == "table":
        md_table()
        return
    os.makedirs(DOCS, exist_ok=True)
    if mode == "baseline":
        rows = [measure(i, i, False) for i in ITEMS]
        doc = {"slice": SLICE, "state": "baseline (before the upgrade; the repo at the start of release 1.102.0)", "runtime_constants": BASELINE_RUNTIME, "items": rows}
        json.dump(doc, open(os.path.join(DOCS, SLICE + "_baseline.json"), "w", encoding="utf-8", newline="\n"), indent=2)
        print("baseline: %d items" % len(rows))
        return
    base = json.load(open(os.path.join(DOCS, SLICE + "_baseline.json"), encoding="utf-8"))
    bmap = {r["item"]: r for r in base["items"]}
    rows, table = [], []
    for i in ITEMS:
        a = measure(i, "up/" + i, True)
        ratios, v = score(bmap[i], a, WEIGHTS)
        _r2, v2 = score(bmap[i], a, WEIGHTS_BRIEF)
        a["ratios"] = ratios
        a["V_before"] = 1.0
        a["V_after"] = v
        a["V_after_brief_weights"] = v2
        rows.append(a)
        table.append({"item": i, "ratios": ratios, "V_after": v, "V_after_brief_weights": v2})
    doc = {"slice": SLICE, "state": "after the upgrade (1.102.0)", "weights": WEIGHTS, "weights_brief_example": WEIGHTS_BRIEF,
           "formula": "V = sum(weight * after/before) over mesh, texture, passes, ambient, animation; V_before = 1", "items": rows, "ratio_table": table}
    json.dump(doc, open(os.path.join(DOCS, SLICE + "_after.json"), "w", encoding="utf-8", newline="\n"), indent=2)
    for t in table:
        print("%-30s V=%.2f (brief weights %.2f)  %s" % (t["item"], t["V_after"], t["V_after_brief_weights"], t["ratios"]))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "baseline")
