"""Measures the demon_slayer slice and writes docs/visual_upgrade/demon_slayer_baseline.json (before) or demon_slayer_after.json (after).

    python3 -B tools/visual_upgrade/demon_slayer/ds_measure.py baseline
    python3 -B tools/visual_upgrade/demon_slayer/ds_measure.py after

The same fields as docs/visual_upgrade/genesis_weapons_{baseline,after}.json (so the ratios are like for like and score.py of that slice reads both): per mesh quads per layer,
welded vertices, submitted corners, passes, skins (size, painted texels, detail texels), ambient layers, idle animations, draw animation, swing trail, signature act VFX.
The "before" state is what the code does today: client/DemonSlayerMesh.java at rest (and its two extra states, the full-resonance bloom and the fractured blade), drawn by
client/DemonSlayerRenderer.java with the 64 px skin textures/entity/demon_slayer_sword.png.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ds_common import *  # noqa: F401,F403,E402

# What the old renderer does beyond the mesh (read from client/DemonSlayerRenderer.java, DemonSlayerMesh.java and docs/demon_slayer_genesis_spec.md).
BASE_AMBIENT = ["pommel_bloom_glow (DemonSlayerMesh#bloom: a crimson glow behind the clover, pulsing with the resonance)"]
BASE_IDLE = ["bloom_pulse (DemonSlayerMesh#bloom: the glow breathes, faster with the resonance)"]
# the acts of the item: Conceptual Severance (a full swing), Black Meteorite (sneak + right-click), Black Divider (right-click), the Nullification Field (passive)
ACTS = ["conceptual_severance", "black_meteorite", "black_divider"]
BASE_ACT_VFX = {"black_meteorite": "DEMON_METEOR + NIHILITY_ZONE (vfx/client/layer/DemonSlayerLayer, world effects, not on the item)"}


def welded(quads, layer):
    s = set()
    for (ly, P, _n, _c) in quads:
        if ly != layer:
            continue
        for p in P:
            s.add((round(float(p[0]), 3), round(float(p[1]), 3), round(float(p[2]), 3)))
    return len(s)


def counts(quads):
    q = {name: 0 for name in LAYERS.values()}
    for (ly, _P, _n, _c) in quads:
        q[LAYERS[ly]] += 1
    q["total"] = sum(q.values())
    return q


def old_state(quads):
    q = counts(quads)
    return {"quads": q, "welded_vertices_body": welded(quads, 0), "welded_vertices_all": sum(welded(quads, k) for k in LAYERS), "submitted_corners_per_frame": 4 * q["total"]}


def baseline():
    classes = build_dump()
    rest = dump_old(classes, 0, 0.0, 0.0)
    bloom4 = dump_old(classes, 4, 0.0, 0.0)
    broken = dump_old(classes, 0, 0.5, 10.0)
    st = {"rest": old_state(rest), "resonance_4": old_state(bloom4), "fractured": old_state(broken)}
    skin = texel_stats(os.path.join(OLD_TEX, "demon_slayer_sword.png"))
    bloom = texel_stats(os.path.join(OLD_TEX, "demon_slayer_bloom.png"))
    bloom_glow = texel_stats(os.path.join(OLD_TEX, "demon_slayer_bloom_glow.png"))
    passes = ["body_cutout", "void_shader", "clover_translucent", "bloom_glow_additive", "fracture_crack_additive (only while Black Meteorite runs)"]
    emissive = ["void_shader", "bloom_glow_additive", "fracture_crack_additive"]
    return {
        "id": ID, "name": NAME, "grade": "GENESIS", "kind": "greatsword",
        "source": "client/DemonSlayerMesh.java dumped with tools/item_preview/MeshDump.java (a pixel model in 1/64 block units, no weapon_meshes file)",
        "meshes": {ID: dict(st["rest"], file="src/main/java/com/newuniverse/nusmp/client/DemonSlayerMesh.java", named_parts=10)},
        "mesh": dict(st["rest"], file="src/main/java/com/newuniverse/nusmp/client/DemonSlayerMesh.java", named_parts=10),
        "states": st,
        "passes": passes,
        "emissive_pass_list": emissive,
        "material_pass_count": len(passes),
        "skin": skin, "glow_skin": bloom_glow, "clover_skin": bloom,
        "ambient_layers": BASE_AMBIENT,
        "idle_animations": BASE_IDLE,
        "draw_animation": False,
        "swing_trail": False,
        "signature_act_vfx": False,
        "act_vfx_today": {"acts": ACTS, "generic_shapes": ["DEMON_METEOR", "NIHILITY_ZONE"], "by_act": BASE_ACT_VFX},
        "sound_loop": None,
        "act_animations_today": ["fracture (Black Meteorite: four shards drift apart and re-form)", "bloom_growth (resonance 0..4)", "blade_growth (Black Divider: the blade is drawn 1.6 times as large for 8 s)"],
    }


def after():
    spec = load_json(os.path.join(SPEC_DIR, ID + ".json"))
    p = os.path.join(VU_MESH, ID + ".txt")
    d = parse_mesh(p)
    q = {}
    for (ly, _g, _P, _uv, _n) in d["quads"]:
        q[ly] = q.get(ly, 0) + 1
    named = 0
    with open(p) as fh:
        for line in fh:
            if line.startswith("# part "):
                named += 1
    blk = {
        "file": os.path.relpath(p, ROOT).replace(os.sep, "/"),
        "quads": {"body": q.get(0, 0), "void": q.get(1, 0), "glow": q.get(2, 0), "veins": q.get(3, 0), "halo": q.get(4, 0), "total": sum(q.values())},
        "welded_vertices_body": welded_vertices(d["quads"], {0}), "welded_vertices_all": welded_vertices(d["quads"]),
        "submitted_corners_per_frame": 4 * sum(q.values()), "named_parts": named, "flares": len(d["flares"]),
        "animation_groups": len({g for (_l, g, _P, _u, _n) in d["quads"] if g and g != "-"}),
    }
    lp = parse_mesh(os.path.join(VU_MESH, ID + "_lod.txt"))
    lq = {}
    for (ly, _g, _P, _uv, _n) in lp["quads"]:
        lq[ly] = lq.get(ly, 0) + 1
    blk["lod"] = {"file": "src/main/resources/assets/nusmp/weapon_meshes_vu/%s_lod.txt" % ID,
                  "quads": {"body": lq.get(0, 0), "void": lq.get(1, 0), "glow": lq.get(2, 0), "veins": lq.get(3, 0), "halo": lq.get(4, 0), "total": sum(lq.values())},
                  "welded_vertices_body": welded_vertices(lp["quads"], {0})}
    passes = ["body_cutout"]
    if blk["quads"]["void"]:
        passes.append("void_shader")
    if blk["quads"]["glow"]:
        passes.append("core_glow_additive")
    if blk["quads"]["veins"]:
        passes.append("energy_veins_translucent_wave")
    if blk["quads"]["halo"]:
        passes.append("halo_shafts_additive")
    if blk["flares"]:
        passes.append("flare_billboards_additive")
    skin_name = d["skin"] or ID
    return {
        "id": ID, "name": NAME, "grade": "GENESIS", "kind": "greatsword",
        "meshes": {ID: blk}, "mesh": blk,
        "passes": passes, "material_pass_count": len(passes), "emissive_passes": [x for x in passes if x != "body_cutout"],
        "skin": texel_stats(os.path.join(VU_TEX, skin_name + ".png")),
        "glow_skin": texel_stats(os.path.join(VU_TEX, skin_name + "_glow.png")),
        "veins_skin": texel_stats(os.path.join(VU_TEX, skin_name + "_veins.png")),
        "ambient_layers": [l["id"] + " (" + l["kind"] + ")" for l in spec["layers"]],
        "idle_animations": [a["id"] + " (" + a["kind"] + ")" for a in spec["idle"]],
        "draw_animation": bool(spec.get("draw")), "swing_trail": bool(spec.get("swing")),
        "signature_act_vfx": sorted(spec.get("acts", {}).keys()),
        "sound_loop": spec.get("sound_loop"), "lod": spec.get("lod"),
    }


def main(mode):
    item = baseline() if mode == "baseline" else after()
    out = {"slice": SLICE, "mode": mode, "note": "see tools/visual_upgrade/demon_slayer/ds_measure.py for the definitions; same fields as genesis_weapons", "items": [item]}
    if mode == "after":
        import score
        base = load_json(os.path.join(DOCS, SLICE + "_baseline.json"))
        out["score"] = score.table(base["items"], [item])
    path = os.path.join(DOCS, SLICE + "_" + mode + ".json")
    save_json(path, out)
    print("wrote", path)
    return out


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "baseline")
