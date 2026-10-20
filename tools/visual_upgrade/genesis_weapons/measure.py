"""Measures the genesis_weapons slice and writes docs/visual_upgrade/genesis_weapons_baseline.json (before) or genesis_weapons_after.json (after).

    python3 -B tools/visual_upgrade/genesis_weapons/measure.py baseline
    python3 -B tools/visual_upgrade/genesis_weapons/measure.py after

Both modes use the same functions on the same fields, so the ratios are like for like. What is measured (per item, per mesh file):
  * mesh: quads per layer, welded vertices (positions rounded to 1/1000, what an index buffer would hold), submitted corners (4 per quad, per pass),
    the number of named parts (`# part` comments, else the generator header);
  * passes: the material / glow passes the renderer draws for the item (listed by name);
  * skin: size, painted texels (alpha > 8), detail texels (painted and differing from the neighbours: a flat fill is painted but not detail);
    the glow skin the same;
  * effects: ambient layers (names), idle animations (names), draw animation, swing trail, signature act VFX, looped sound id.
The "before" effects are what the code does today (read from the sources, listed in BASELINE_EFFECTS); the "after" effects are read from the item's
assets/nusmp/visual_upgrade/<id>.json, which the client reads at run time.

V (the visual score) is defined in score.py; the weights are documented there and in docs/visual_upgrade/genesis_weapons.md.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from common import *  # noqa: F401,F403,E402

# What a Genesis hand relic has TODAY beyond its mesh (WeaponRenderer, RelicGradeFx, ItemStateVariants, ElsdociaGlitterClient). Nothing is per item.
BASELINE_EFFECTS = {
    "ambient_layers": ["grade_shimmer_motes (RelicGradeFx, one mote per 8 ticks round the holder)", "genesis_extra_motes (RelicGradeFx, one in three)"],
    "idle_animations": ["glow_pulse (WeaponRenderer, sine of the mesh's glow speed, stronger for Genesis)"],
    "draw_animation": False,
    "swing_trail": False,
    "signature_act_vfx": False,
    "sound_loop": None,
}
ELSDOCIA_EXTRA_AMBIENT = ["held_glitter (ElsdociaGlitterClient)"]
ELSDOCIA_EXTRA_PASS = ["elsdocia_halo (a wider, fainter copy of the glow that breathes against the main pulse)"]

SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave")


def act_shapes(item_id):
    """The VfxShape names the item's acts use today, from the wave sources (generic shapes shared with other items)."""
    for fn in ("Wave1.java", "Wave71.java", "Wave78.java"):
        p = os.path.join(SRC, fn)
        if not os.path.exists(p):
            continue
        text = open(p, encoding="utf-8").read()
        m = re.search(r'RelicDef\.of\("%s"' % re.escape(item_id), text)
        if not m:
            continue
        nxt = text.find('RelicDef.of("', m.end())
        block = text[m.start(): nxt if nxt > 0 else len(text)]
        acts = re.findall(r'Act\.of\("(\w+)"', block)
        shapes = re.findall(r"VfxShape\.(\w+)", block)
        return {"acts": acts, "shapes": shapes}
    return {"acts": [], "shapes": []}


def mesh_block(path):
    d = parse_mesh(path)
    q = {}
    for (ly, _g, _P, _uv, _n) in d["quads"]:
        q[ly] = q.get(ly, 0) + 1
    named, header = 0, 0
    with open(path) as fh:
        for line in fh:
            if line.startswith("# part "):
                named += 1
            elif line.startswith("#") and not header:
                m = re.search(r"(\d+) parts", line)
                if m:
                    header = int(m.group(1))
    parts = max(named, header)
    body = q.get(0, 0)
    return d, {
        "file": os.path.relpath(path, ROOT).replace(os.sep, "/"),
        "quads": {"body": body, "void": q.get(1, 0), "glow": q.get(2, 0), "veins": q.get(3, 0), "halo": q.get(4, 0), "total": sum(q.values())},
        "welded_vertices_body": welded_vertices(d["quads"], {0}),
        "welded_vertices_all": welded_vertices(d["quads"]),
        "submitted_corners_per_frame": 4 * sum(q.values()),
        "named_parts": parts,
    }


def old_item(item_id):
    meshes = {}
    for mid in MESH_IDS[item_id]:
        p = os.path.join(OLD_MESH, mid + ".txt")
        if os.path.exists(p):
            meshes[mid] = mesh_block(p)[1]
    rest = meshes[item_id]
    passes = ["body_cutout"]
    if rest["quads"]["void"]:
        passes.append("void_shader")
    if rest["quads"]["glow"]:
        passes += ["glow_additive", "genesis_emissive_translucent"]      # a Genesis stack adds the translucent emissive pass (ItemStateVariants.intensify)
    effects = dict(BASELINE_EFFECTS)
    if item_id == "elsdocia":
        passes += ELSDOCIA_EXTRA_PASS
        effects["ambient_layers"] = BASELINE_EFFECTS["ambient_layers"] + ELSDOCIA_EXTRA_AMBIENT
    skin = texel_stats(os.path.join(OLD_TEX, item_id + ".png"))
    glow = texel_stats(os.path.join(OLD_TEX, item_id + "_glow.png"))
    sh = act_shapes(item_id)
    return {
        "id": item_id, "name": NAMES[item_id], "grade": "GENESIS", "kind": CLASSES[item_id],
        "meshes": meshes,
        "mesh": rest,
        "passes": passes,
        "material_pass_count": len(passes),
        "skin": skin, "glow_skin": glow,
        "ambient_layers": effects["ambient_layers"],
        "idle_animations": effects["idle_animations"],
        "draw_animation": effects["draw_animation"],
        "swing_trail": effects["swing_trail"],
        "signature_act_vfx": effects["signature_act_vfx"],
        "act_vfx_today": {"acts": sh["acts"], "generic_shapes": sh["shapes"]},
        "sound_loop": effects["sound_loop"],
    }


def new_item(item_id):
    spec_path = os.path.join(SPEC_DIR, item_id + ".json")
    spec = load_json(spec_path)
    meshes = {}
    for mid in MESH_IDS[item_id]:
        p = os.path.join(VU_MESH, mid + ".txt")
        if os.path.exists(p):
            d, blk = mesh_block(p)
            lod = d["lod"]
            if lod and os.path.exists(os.path.join(VU_MESH, lod + ".txt")):
                blk["lod"] = mesh_block(os.path.join(VU_MESH, lod + ".txt"))[1]
            meshes[mid] = blk
    rest = meshes[item_id]
    passes = ["body_cutout"]
    if rest["quads"]["void"]:
        passes.append("void_shader")
    if rest["quads"]["glow"]:
        passes.append("core_glow_additive")
    if rest["quads"]["veins"]:
        passes.append("energy_veins_translucent_wave")
    if rest["quads"]["halo"]:
        passes.append("halo_shafts_additive")
    skin_name = load_skin_name(item_id)
    skin = texel_stats(os.path.join(VU_TEX, skin_name + ".png"))
    glow = texel_stats(os.path.join(VU_TEX, skin_name + "_glow.png"))
    veins = texel_stats(os.path.join(VU_TEX, skin_name + "_veins.png"))
    return {
        "id": item_id, "name": NAMES[item_id], "grade": "GENESIS", "kind": CLASSES[item_id],
        "meshes": meshes,
        "mesh": rest,
        "passes": passes,
        "material_pass_count": len(passes),
        "emissive_passes": [p for p in passes if p != "body_cutout"],
        "skin": skin, "glow_skin": glow, "veins_skin": veins,
        "ambient_layers": [l["id"] + " (" + l["kind"] + ")" for l in spec["layers"]],
        "idle_animations": [a["id"] + " (" + a["kind"] + ")" for a in spec["idle"]],
        "draw_animation": bool(spec.get("draw")),
        "swing_trail": bool(spec.get("swing")),
        "signature_act_vfx": sorted(spec.get("acts", {}).keys()),
        "sound_loop": spec.get("sound_loop"),
        "lod": spec.get("lod"),
    }


def load_skin_name(item_id):
    d = parse_mesh(os.path.join(VU_MESH, item_id + ".txt"))
    return d["skin"] or item_id


def main(mode):
    items = []
    for item_id in IDS:
        if mode == "baseline":
            items.append(old_item(item_id))
        else:
            if not os.path.exists(os.path.join(VU_MESH, item_id + ".txt")):
                continue
            items.append(new_item(item_id))
    out = {"slice": SLICE, "mode": mode, "note": "see tools/visual_upgrade/genesis_weapons/measure.py for the definitions", "items": items}
    if mode == "after":
        import score
        base = load_json(os.path.join(DOCS, SLICE + "_baseline.json"))
        out["score"] = score.table(base["items"], items)
    path = os.path.join(DOCS, SLICE + "_" + mode + ".json")
    save_json(path, out)
    print("wrote", path, len(items), "items")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "baseline")
