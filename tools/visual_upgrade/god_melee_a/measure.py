"""Measures the visual proxies of the twelve God melee (a) items.

    python3 -B tools/visual_upgrade/god_melee_a/measure.py baseline     writes docs/visual_upgrade/god_melee_a_baseline.json (the shipped 1.101.0 state)
    python3 -B tools/visual_upgrade/god_melee_a/measure.py after        writes docs/visual_upgrade/god_melee_a_after.json (the upgraded state + the ratio table)

The baseline reads the old mesh weapon_meshes/<id>.txt and its skins. The after state reads weapon_meshes/up/<id>.txt, textures/entity/weapon/up/<id>*.png and the effect
spec assets/nusmp/visual_upgrade/<id>.json. What a number means is in docs/visual_upgrade/god_melee_a.md (section 2); nothing here has been seen in a game.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common as C                                                     # noqa: E402

# what a held God weapon had before this release, found in the code (docs/visual_upgrade/god_melee_a.md section 2 lists where):
BASE_AMBIENT = [{"layer": "grade_shimmer", "where": "client/relics/RelicGradeFx.onClientTick: one mote round a carrier of a God relic every 8 ticks"}]
BASE_IDLE = ["glow_pulse"]          # WeaponRenderer: the glow tint breathes between glowMin and glowMax (VariantRules.pulse)


def baseline_record(item_id):
    path = C.mesh_path(item_id)
    acts = C.wave71_acts(item_id)
    rec = {
        "id": item_id, "grade": "god", "kind": C.weapon_class(item_id), "slice": C.SLICE, "state": "baseline (1.101.0)",
        "mesh": C.mesh_stats(path),
        "skin": C.png_stats(os.path.join(C.SKIN_DIR, item_id + ".png")),
        "glow_map": C.png_stats(os.path.join(C.SKIN_DIR, item_id + "_glow.png")),
        "passes": {"count": 2, "list": ["skin (entityCutout)", "glow (eyes, additive, full bright, pulsing)"]},
        "ambient": {"count": len(BASE_AMBIENT), "layers": BASE_AMBIENT},
        "animation": {"idle": BASE_IDLE, "swing_trail": False, "unique_act_vfx": 0, "score": 1.0},
        "act_vfx": [{"act": k, "shape": s} for k, s in acts],
        "lod_mesh": False,
    }
    if item_id in C.STATE_MESHES:
        rec["state_meshes"] = {s: C.mesh_stats(C.mesh_path(s))["quads_total"] for s in C.STATE_MESHES[item_id]}
    v, comp = C.visual_score(rec)
    rec["V"] = round(v, 4)
    rec["V_components"] = {k: round(x, 4) for k, x in comp.items()}
    return rec


def after_record(item_id):
    spec_path = os.path.join(C.SPEC_DIR, item_id + ".json")
    spec = json.load(open(spec_path, encoding="utf-8"))
    path = C.mesh_path(item_id, up=True)
    lod = os.path.join(C.UP_MESH_DIR, item_id + "_lod.txt")
    rec = {
        "id": item_id, "grade": "god", "kind": C.weapon_class(item_id), "slice": C.SLICE, "state": "upgraded (1.102.0)",
        "mesh": C.mesh_stats(path),
        "skin": C.png_stats(os.path.join(C.SKIN_DIR, spec["maps"]["skin"])),
        "glow_map": C.png_stats(os.path.join(C.SKIN_DIR, spec["maps"]["glow"])),
        "extra_maps": {k: C.png_stats(os.path.join(C.SKIN_DIR, v)) for k, v in spec["maps"].items() if k not in ("skin", "glow")},
        "passes": {"count": len(spec["passes"]), "list": spec["passes"]},
        "ambient": {"count": len(spec["ambient"]), "layers": [{"layer": a["type"], "where": a.get("anchor", "")} for a in spec["ambient"]]},
        "animation": {"idle": spec["idle"], "swing_trail": bool(spec.get("swing")), "unique_act_vfx": len(spec.get("acts", [])), "score": 0.0},
        "act_vfx": [{"act": a["act"], "signature": a["id"]} for a in spec.get("acts", [])],
        "lod_mesh": os.path.exists(lod),
        "lod": C.mesh_stats(lod) if os.path.exists(lod) else None,
    }
    n_acts = max(1, len(C.wave71_acts(item_id)))
    rec["animation"]["score"] = len(spec["idle"]) + (1.0 if spec.get("swing") else 0.0) + min(1.0, len(spec.get("acts", [])) / n_acts)
    if item_id in C.STATE_MESHES:
        rec["state_meshes"] = {s: C.mesh_stats(C.mesh_path(s, up=True))["quads_total"] for s in C.STATE_MESHES[item_id] if os.path.exists(C.mesh_path(s, up=True))}
    v, comp = C.visual_score(rec)
    rec["V"] = round(v, 4)
    rec["V_components"] = {k: round(x, 4) for k, x in comp.items()}
    return rec


def main(mode):
    os.makedirs(C.DOCS, exist_ok=True)
    out = {"slice": C.SLICE, "grade": "god", "weights": C.WEIGHTS, "references": C.REFS, "unique_vertex_cap": C.GOD_UNIQUE_VERTEX_CAP, "lod_fraction": C.LOD_FRACTION,
           "note": "V = sum(weight * component); component = a plain count over a fixed reference (never capped). Proxies, not a measure of how it looks in the game.", "items": []}
    if mode == "baseline":
        out["items"] = [baseline_record(i) for i in C.IDS]
        path = os.path.join(C.DOCS, "god_melee_a_baseline.json")
    else:
        base = {r["id"]: r for r in json.load(open(os.path.join(C.DOCS, "god_melee_a_baseline.json"), encoding="utf-8"))["items"]}
        for i in C.IDS:
            r = after_record(i)
            b = base[i]
            ratios = {
                "V": round(r["V"] / b["V"], 3),
                "mesh_quads": round(r["mesh"]["quads_total"] / b["mesh"]["quads_total"], 3),
                "unique_vertices": round(r["mesh"]["unique_vertices"] / b["mesh"]["unique_vertices"], 3),
                "skin_painted_texels": round(r["skin"]["painted_texels"] / b["skin"]["painted_texels"], 3),
                "glow_map_painted_texels": round(r["glow_map"]["painted_texels"] / max(1, b["glow_map"]["painted_texels"]), 3),
                "passes": round(r["passes"]["count"] / b["passes"]["count"], 3),
                "ambient_layers": round(r["ambient"]["count"] / b["ambient"]["count"], 3),
                "animation": round(r["animation"]["score"] / b["animation"]["score"], 3),
            }
            r["ratios_vs_baseline"] = ratios
            r["baseline_V"] = b["V"]
            out["items"].append(r)
        path = os.path.join(C.DOCS, "god_melee_a_after.json")
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print("wrote", path)
    for r in out["items"]:
        print("%-28s V=%.3f quads=%d uniq=%d skin=%d" % (r["id"], r["V"], r["mesh"]["quads_total"], r["mesh"]["unique_vertices"], r["skin"]["painted_texels"]),
              r.get("ratios_vs_baseline", {}).get("V", ""))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "baseline")
