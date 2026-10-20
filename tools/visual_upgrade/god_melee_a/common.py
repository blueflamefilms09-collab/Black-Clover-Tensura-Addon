"""Shared constants and readers of the God melee (a) visual upgrade (slice key god_melee_a, release 1.102.0).

The slice: the twelve God grade hand weapons of wave 71 that tools/relics/new197_god.py and new197_god2.py build. Everything here is plain Python 3 + numpy + PIL.
"""
import json
import os
import re

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
UP_MESH_DIR = os.path.join(MESH_DIR, "up")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "weapon")
UP_SKIN_DIR = os.path.join(SKIN_DIR, "up")
SPEC_DIR = os.path.join(ASSETS, "visual_upgrade")
DOCS = os.path.join(ROOT, "docs", "visual_upgrade")
PREVIEWS = os.path.join(ROOT, "docs", "previews", "visual_upgrade", "god_melee_a")
WAVE71 = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave", "Wave71.java")

SLICE = "god_melee_a"
IDS = ["staff_of_the_magi", "wizard_kings_vow", "wand_of_the_wisdom_king", "hammer_of_thunderbolts", "gungnir_the_unerring", "kusanagi_the_grass_cutter",
       "leviathans_crown_trident", "calamity_wave_aegis", "hellblaze_the_demon_sin", "minotaur_kings_labrys", "dragon_force_twin_fangs", "dawnpiercer_crossbow"]
# the extra state meshes of the crossbow (draw stages and the loaded bolt); they share the skin of the rest mesh
STATE_MESHES = {"dawnpiercer_crossbow": ["dawnpiercer_crossbow_pull1", "dawnpiercer_crossbow_pull2", "dawnpiercer_crossbow_pull3", "dawnpiercer_crossbow_charged"]}

# the visual score V = sum of weight * component, every component a plain count divided by one fixed reference (never capped), so a ratio of two V is a ratio of sums
WEIGHTS = {"mesh": 0.35, "texture": 0.20, "glow": 0.15, "ambient": 0.20, "animation": 0.10}
REFS = {"mesh_quads": 600.0, "texels": 512.0 * 512.0, "passes": 2.0, "ambient_layers": 3.0}

# the performance guard of the brief for one God item held near the camera, counted in WELDED vertices (a vertex shared by the quads that meet there counts once)
GOD_UNIQUE_VERTEX_CAP = 2500
LOD_FRACTION = 0.15


def mesh_path(item_id, up=False):
    return os.path.join(UP_MESH_DIR if up else MESH_DIR, item_id + ".txt")


def read_mesh(path):
    """Parses a weapon mesh file: header lines, quads as (layer, P 4x3, UV 4x2, N 4x3), part comments."""
    head, quads, parts, orbits = {}, [], [], []
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            v = line.split()
            if not v:
                continue
            if v[0] == "#":
                if len(v) > 1 and v[1] == "part":
                    parts.append(v[2])
                continue
            if v[0] in ("place", "glow", "grip", "orient"):
                head[v[0]] = v[1:]
            elif v[0] == "orbit":
                orbits.append(v[1:])
            elif v[0] == "qn":
                a = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
                quads.append((int(v[1]), a[:, :3], a[:, 3:5], a[:, 5:8]))
            elif v[0] == "q":
                a = np.array([float(x) for x in v[2:22]]).reshape(4, 5)
                quads.append((int(v[1]), a[:, :3], a[:, 3:5], np.tile([float(x) for x in v[22:25]], (4, 1))))
    return {"head": head, "quads": quads, "parts": parts, "orbits": orbits}


def mesh_stats(path):
    m = read_mesh(path)
    layers = {}
    verts = set()
    for (ly, P, UV, N) in m["quads"]:
        layers[ly] = layers.get(ly, 0) + 1
        for p in P:
            verts.add((round(float(p[0]), 2), round(float(p[1]), 2), round(float(p[2]), 2)))
    body = sum(c for k, c in layers.items() if k not in (1, 2))             # body (0) and orbiting groups (3 and up) are skin quads
    return {"quads_total": len(m["quads"]), "quads_body": body, "quads_glow": layers.get(2, 0), "quads_void": layers.get(1, 0),
            "unique_vertices": len(verts), "emitted_vertices": 4 * len(m["quads"]), "parts": len(set(m["parts"])), "layers": {str(k): v for k, v in sorted(layers.items())},
            "orbit_groups": len(m["orbits"])}


def png_stats(path):
    if not os.path.exists(path):
        return None
    im = Image.open(path).convert("RGBA")
    a = np.asarray(im)[..., 3]
    return {"size": [im.width, im.height], "texels": int(im.width * im.height), "painted_texels": int((a > 0).sum()), "lit_texels": int((a > 24).sum())}


def wave71_acts(item_id):
    """[(act key, VfxShape name or None)] of one wave 71 weapon, read from the generated Wave71.java."""
    s = open(WAVE71, encoding="utf-8").read()
    a = s.index('RelicDef.of("%s"' % item_id)
    b = s.find("out.accept", a + 10)
    block = s[a:b if b > 0 else len(s)]
    acts = []
    for line in block.split("\n"):
        mm = re.search(r'\.act\(Act\.of\("([a-z_]+)"', line)
        if mm:
            sh = re.search(r"VfxShape\.([A-Z_]+)", line)
            acts.append((mm.group(1), sh.group(1) if sh else None))
    return acts


def weapon_class(item_id):
    d = json.load(open(os.path.join(ASSETS, "weapon_classes.json"), encoding="utf-8"))
    return d.get("weapons", {}).get(item_id)


def visual_score(rec):
    """V of one record: see WEIGHTS / REFS. Returns (V, components)."""
    comp = {"mesh": rec["mesh"]["quads_total"] / REFS["mesh_quads"],
            "texture": rec["skin"]["painted_texels"] / REFS["texels"],
            "glow": rec["passes"]["count"] / REFS["passes"],
            "ambient": rec["ambient"]["count"] / REFS["ambient_layers"],
            "animation": rec["animation"]["score"]}
    v = sum(WEIGHTS[k] * comp[k] for k in WEIGHTS)
    return v, comp
