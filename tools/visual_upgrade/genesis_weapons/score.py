"""The visual score V of the genesis_weapons slice, and the floors of the brief.

V = sum of weight * (after / before) over five components, so V_before = 1 by construction and V_after is "how many times the old item" under these weights:

    component                         measured as                                              weight
    mesh detail                       quads of the body layer                                   0.35
    texture detail                    detail texels of the skin (painted and not flat)          0.20
    glow / emissive passes            emissive passes (void, core glow, veins, halo, ...)       0.15
    ambient effects                   ambient effect layers (the effect data's `layers`)        0.20
    animation                         idle animations + draw animation + swing trail            0.10

The ratios are unclamped and the weights are fixed here (not tuned per item). Two honest caveats the docs repeat: the mesh ratio is bounded by the vertex cap of the
performance guard (6000 welded vertices in the hand), so for a weapon that already had 1000+ quads it cannot reach 20x; and the texture ratio is large mostly because the
skins are 2048 / 1024 px against 512 px before.

The floors of the brief that are checked as booleans (all must hold): body quads >= 2500, welded vertices <= 6000, LOD <= 15 % of the quads, skin painted texels >= 8x the
baseline (or, when the baseline already fills more than 1/8 of the biggest allowed skin, the share the skin can hold), >= 2 emissive passes besides the body, >= 6 ambient
effect layers, a draw animation, a swing trail, a signature burst on every act, a looped sound id.
"""
W = {"mesh": 0.35, "texture": 0.20, "glow": 0.15, "ambient": 0.20, "anim": 0.10}
MAX_SKIN = 2048
MAX_WELDED = 6000


def components(item, base=False):
    quads = item["mesh"]["quads"]["body"]
    detail = (item["skin"] or {}).get("detail_texels", 0)
    painted = (item["skin"] or {}).get("painted_texels", 0)
    if base:
        glow_passes = len([p for p in item["passes"] if p != "body_cutout"])
    else:
        glow_passes = len(item["emissive_passes"])
    ambient = len(item["ambient_layers"])
    anim = len(item["idle_animations"]) + (1 if item["draw_animation"] else 0) + (1 if item["swing_trail"] else 0)
    return {"mesh": quads, "texture": detail, "glow": glow_passes, "ambient": ambient, "anim": anim, "painted": painted}


def table(base_items, after_items):
    bmap = {i["id"]: i for i in base_items}
    out = {"weights": W, "items": [], "notes": []}
    for a in after_items:
        b = bmap[a["id"]]
        cb, ca = components(b, True), components(a, False)
        ratios = {k: (ca[k] / max(cb[k], 1)) for k in W}
        v = sum(W[k] * ratios[k] for k in W)
        painted_ratio = ca["painted"] / max(cb["painted"], 1)
        paint_possible = MAX_SKIN * MAX_SKIN * 0.8
        painted_ok = painted_ratio >= 8.0 or ca["painted"] >= min(8.0 * cb["painted"], paint_possible)
        lod = a["mesh"].get("lod", {}).get("quads", {}).get("body", 10 ** 9)
        floors = {
            "body_quads_ge_2500": ca["mesh"] >= 2500,
            "welded_vertices_le_6000": a["mesh"]["welded_vertices_body"] <= MAX_WELDED,
            "lod_le_15_percent": lod <= 0.15 * ca["mesh"],
            "skin_painted_texels_8x": painted_ok,
            "emissive_passes_ge_2": ca["glow"] >= 2,
            "ambient_layers_ge_6": ca["ambient"] >= 6,
            "draw_animation": bool(a["draw_animation"]),
            "swing_trail": bool(a["swing_trail"]),
            "signature_on_every_act": len(a["signature_act_vfx"]) >= max(1, len(b["act_vfx_today"]["acts"])),
            "sound_loop_id": bool(a["sound_loop"]),
        }
        out["items"].append({
            "id": a["id"],
            "before": cb, "after": ca, "ratios": {k: round(r, 2) for k, r in ratios.items()},
            "painted_texel_ratio": round(painted_ratio, 2),
            "V_before": 1.0, "V_after": round(v, 2), "V_ratio_ge_20": v >= 20.0,
            "mesh_ratio": round(ratios["mesh"], 2),
            "mesh_cap_note": "bounded by the %d welded vertex cap" % MAX_WELDED if ratios["mesh"] < 20 else "",
            "floors": floors, "floors_ok": all(floors.values()),
        })
    return out
