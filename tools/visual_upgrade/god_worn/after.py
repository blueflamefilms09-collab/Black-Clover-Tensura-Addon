"""STEP 3 of the slice `god_worn`: measures every upgraded item with the SAME code and formula as the baseline and writes docs/visual_upgrade/god_worn_after.json with a ratio table.

    python3 -B tools/visual_upgrade/god_worn/after.py

Reads assets/nusmp/visual_upgrade/catalog.txt (the `up`, `lod` and `held` pieces), the model files, the skins and glow maps, and the effect tables. The numbers are proxies, counted from
the files the game reads (see metrics.py for every definition); they say how much was built, not how it looks. The targets of the God brief are checked and written next to them:
quads >= 5 x baseline (floor 900), painted texels >= 4 x baseline, a separate glow map, >= 3 ambient layers, an idle animation, an act visual for every act of the relic that has one,
a LOD of at most 15% and a held form of at most 2500 vertices, V_after >= 5 x V_before.
"""
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items as IT  # noqa: E402
import metrics as M  # noqa: E402
import caps as CP  # noqa: E402

OUT = os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_after.json")
SKIN_DIR = os.path.join(M.ASSETS, "textures", "relic_worn", "god")


def read_up_catalog():
    out = {"up": {}, "lod": {}, "held": {}}
    for line in open(os.path.join(M.UPG, "catalog.txt"), encoding="utf8"):
        v = line.split()
        if len(v) > 2 and v[0] in out:
            out[v[0]][v[1]] = v[2:]
    return out


def act_keys(rid):
    """The act keys of the relic from its RelicDef source (Wave*.java): the keys the effect table is checked against."""
    import re
    import glob
    alias = {"hand_of_vecna": "Wave30Tune.ID_HAND", "eye_of_vecna": "Wave30Tune.ID_EYE"}
    ident = '"%s"' % rid if rid not in alias else alias[rid]
    for f in glob.glob(os.path.join(M.ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave", "*.java")):
        t = open(f, encoding="utf8").read()
        m = re.search(r'RelicDef\.of\(%s,.*?(?=out\.accept|\Z)' % re.escape(ident), t, re.S)
        if m:
            return re.findall(r'Act\.of\("([a-z0-9_]+)"', m.group(0))
    return []


def measure_new(rid, pieces, lod, held, catalog_old):
    quads = emit = shell = lit = 0
    groups, ambient = [], []
    cells = set()
    skin = None
    files = []
    for p in pieces:
        name = M.piece_name(p)
        s = M.model_stats(name, atlas_cell=64, atlas_w=512, atlas_h=512)
        files.append(name)
        quads += s["quads"]
        emit += s["emit"]
        shell += s["shell"]
        lit += s["lit"]
        skin = skin or s["skin"]
        cells |= s["cells"]
        groups += [s["groups"][g] for g in sorted(s["groups"])]
        ambient += ["model:%s:%s" % (name, s["groups"][g]) for g in sorted(s["emit_groups"]) if s["groups"][g] in M.MOVING]
    theme = M.catalog_themes().get(rid)
    if theme:
        ambient.append("particles:theme_" + theme)
    fx = json.load(open(os.path.join(M.UPG, rid + ".json"), encoding="utf8"))
    layer_names = ["fx:%s(%s)" % (l["name"], l["kind"]) for l in fx["layers"]]
    ambient += layer_names
    albedo = np.array(Image.open(os.path.join(SKIN_DIR, skin + ".png")).convert("RGBA"))
    glow = np.array(Image.open(os.path.join(SKIN_DIR, skin + "_glow.png")).convert("RGB"))
    painted = glow_t = 0
    for cx, cy in cells:
        painted += int((albedo[cy * 64:(cy + 1) * 64, cx * 64:(cx + 1) * 64, 3] > 0).sum())
        glow_t += int((glow[cy * 64:(cy + 1) * 64, cx * 64:(cx + 1) * 64].max(axis=2) > 12).sum())
    armour = M.ARMOUR_PAINTED.get(rid, 0)
    passes = (["emissive_model"] if emit else []) + (["glow_map"] if (lit or emit) else []) + (["shell_rim", "shimmer"] if shell else [])
    lod_q = sum(M.model_stats(M.piece_name(p), 64, 512, 512)["quads"] for p in lod)
    held_q = sum(M.model_stats(M.piece_name(p), 64, 512, 512)["quads"] for p in held)
    keys = act_keys(rid)
    covered = [k for k in keys if k in fx["acts"] and fx["acts"][k]]
    m = {
        "pieces": len(pieces), "piece_files": files,
        "quads": quads, "vertices": quads * 4,
        "glow_passes": len(passes), "glow_pass_names": passes,
        "skin": {"texture": "textures/relic_worn/god/%s.png + %s_glow.png" % (skin, skin), "size": [512, 512], "texels_total": 512 * 512, "cells_used": len(cells), "cell": 64},
        "painted_texels": painted + armour, "armour_layer_texels": armour, "glow_map_texels": glow_t + M.ARMOUR_GLOW_PAINTED.get(rid, 0),
        "ambient_layers": len(ambient), "ambient_layer_names": ambient, "shared_grade_plan": M.plan_layers(IT.BY_ID[rid][2]),
        "animation_groups": len(groups), "animation_kinds": sorted(set(groups)),
        "act_vfx": len(covered), "acts": len(keys), "acts_without_visual": [k for k in keys if k not in covered],
        "swing_trail": False,
        "lod": {"quads": lod_q, "vertices": lod_q * 4, "fraction": round(lod_q / max(1, quads), 4)},
        "held": {"quads": held_q, "vertices": held_q * 4},
    }
    m["V"], m["V_terms"] = M.score(m)
    return m


def main():
    base = M.load_json(os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_baseline.json"))
    cat = read_up_catalog()
    cat_old = M.read_catalog()
    caps = CP.GRADE_CAPS["GOD"]
    out = {"slice": "god_worn", "state": "after the 1.102.0 visual upgrade", "grade": "GOD", "kind": base["kind"], "score": base["score"], "targets": {}, "items": {}, "ratios": {}, "caps": caps}
    ok_all = True
    for rid, name, slot, look, pal, _lore in IT.ITEMS:
        m = measure_new(rid, cat["up"][rid], cat["lod"][rid], cat["held"][rid], cat_old)
        m.update({"name": name, "slot": slot, "look": look, "grade": "GOD", "kind": "worn relic"})
        b = base["items"][rid]
        r = {
            "quads": round(m["quads"] / b["quads"], 2), "painted_texels": round(m["painted_texels"] / b["painted_texels"], 2),
            "glow_passes": round(m["glow_passes"] / max(1, b["glow_passes"]), 2), "ambient_layers": (round(m["ambient_layers"] / b["ambient_layers"], 2) if b["ambient_layers"] else None),
            "animation_groups": (round(m["animation_groups"] / b["animation_groups"], 2) if b["animation_groups"] else None), "V": round(m["V"] / b["V"], 2),
        }
        checks = {
            "quads_5x_and_floor_900": m["quads"] >= max(900, 5 * b["quads"]),
            "painted_texels_4x": m["painted_texels"] >= 4 * b["painted_texels"],
            "separate_glow_map": m["glow_map_texels"] > 0,
            "ambient_layers_ge_3": len(m["ambient_layer_names"]) >= 3,
            "idle_animation": m["animation_groups"] >= 1,
            "every_act_has_its_own_visual": not m["acts_without_visual"] and m["acts"] > 0,
            "lod_le_15pct": m["lod"]["vertices"] <= caps["lod_fraction"] * m["vertices"],
            "held_le_2500_vertices": m["held"]["vertices"] <= caps["held_vertices"],
            "ensemble_within_cap": m["vertices"] <= caps["ensemble_vertices"],
            "V_5x": m["V"] >= 5 * b["V"],
        }
        m["checks"] = checks
        ok_all = ok_all and all(checks.values())
        out["items"][rid] = m
        out["ratios"][rid] = r
    out["targets"] = {"quads": "after >= max(900, 5 x before)", "painted_texels": "after >= 4 x before", "V": "V_after >= 5 x V_before", "lod": "<= 15% of the vertices", "held": "<= 2500 vertices"}
    tot_b = {k: sum(base["items"][i][k] for i in base["items"]) for k in ("quads", "painted_texels", "ambient_layers", "animation_groups", "glow_passes", "V")}
    tot_a = {k: sum(out["items"][i][k] for i in out["items"]) for k in ("quads", "painted_texels", "ambient_layers", "animation_groups", "glow_passes", "V")}
    out["totals"] = {"before": {k: round(v, 3) for k, v in tot_b.items()}, "after": {k: round(v, 3) for k, v in tot_a.items()}, "all_checks_pass": ok_all}
    with open(OUT, "w", encoding="utf8", newline="\n") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print("%-30s %6s %6s | %6s %6s | %5s %5s | %4s %4s | %5s %5s %s" % ("item", "q_b", "q_a", "tx_b", "tx_a", "amb_b", "amb_a", "an_b", "an_a", "V_b", "V_a", "ratio"))
    for rid in IT.IDS:
        b, a, r = base["items"][rid], out["items"][rid], out["ratios"][rid]
        bad = [k for k, v in a["checks"].items() if not v]
        print("%-30s %6d %6d | %6d %6d | %5d %5d | %4d %4d | %5.2f %5.2f x%.1f %s" % (rid, b["quads"], a["quads"], b["painted_texels"], a["painted_texels"], b["ambient_layers"], a["ambient_layers"],
                                                                                b["animation_groups"], a["animation_groups"], b["V"], a["V"], r["V"], ("FAIL: " + ",".join(bad)) if bad else ""))
    print("totals", out["totals"])
    return 0 if ok_all else 1


if __name__ == "__main__":
    sys.exit(main())
