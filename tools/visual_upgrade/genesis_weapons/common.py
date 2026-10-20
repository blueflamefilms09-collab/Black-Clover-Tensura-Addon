"""Shared paths, the slice's item table and the file readers of the genesis_weapons visual upgrade (slice key `genesis_weapons`).

The readers understand both mesh dialects: the game's `weapon_meshes/<id>.txt` (layers 0 body, 1 void, 2 glow; `q` and `qn` lines) and the upgrade's
`weapon_meshes_vu/<id>.txt` (layers 3 veins and 4 halo, `g <group>` tags, `fl` flares); lines they do not know are skipped, exactly as WeaponRenderer does.
"""
import json
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OLD_MESH = os.path.join(ASSETS, "weapon_meshes")
VU_MESH = os.path.join(ASSETS, "weapon_meshes_vu")
OLD_TEX = os.path.join(ASSETS, "textures", "entity", "weapon")
VU_TEX = os.path.join(ASSETS, "textures", "entity", "weapon_vu")
SPEC_DIR = os.path.join(ASSETS, "visual_upgrade")
DOCS = os.path.join(ROOT, "docs", "visual_upgrade")
PREVIEWS = os.path.join(ROOT, "docs", "previews", "visual_upgrade", "genesis_weapons")
SLICE = "genesis_weapons"

# id -> (display name, source, weapon class, mesh ids the renderer can pick)
ITEMS = [
    ("astralis_the_eye_of_the_universe", "Astralis - The Eye of the Universe", "Wave1", "staff", ["astralis_the_eye_of_the_universe"]),
    ("blackrazor", "Blackrazor", "Wave71", "greatsword", ["blackrazor"]),
    ("cardinal_halo_chakram", "Cardinal Halo Chakram", "Wave71", "handaxe", ["cardinal_halo_chakram"]),
    ("fairy_glitter_gauntlets", "Fairy Glitter Gauntlets", "Wave71", "dagger", ["fairy_glitter_gauntlets"]),
    ("gae_bolg_heartseeker", "Gae Bolg, Heart-Seeker", "Wave71", "spear", ["gae_bolg_heartseeker"]),
    ("magic_emperors_black_halberd", "Magic Emperor's Black Halberd", "Wave71", "glaive", ["magic_emperors_black_halberd"]),
    ("primordial_hearth_blade", "Primordial Hearth Blade", "Wave71", "dagger", ["primordial_hearth_blade"]),
    ("starforged_aeon_bow", "Starforged Aeon Bow", "Wave71", "bow",
     ["starforged_aeon_bow", "starforged_aeon_bow_pull1", "starforged_aeon_bow_pull2", "starforged_aeon_bow_pull3"]),
    ("starsplitter_hammer", "Starsplitter Hammer", "Wave71", "hammer", ["starsplitter_hammer"]),
    ("sword_of_the_first_dawn", "Sword of the First Dawn", "Wave71", "longsword", ["sword_of_the_first_dawn"]),
    ("void_origin_katana", "Void Origin Katana", "Wave71", "katana", ["void_origin_katana"]),
    ("nekrathuls_pact_scythe", "Nekrathul's Pact Scythe", "Wave78", "scythe", ["nekrathuls_pact_scythe"]),
    ("elsdocia", "Elsdocia", "MagicWeaponItem.Kind.ELSDOCIA", "greatsword", ["elsdocia"]),
]
IDS = [i[0] for i in ITEMS]
NAMES = {i[0]: i[1] for i in ITEMS}
CLASSES = {i[0]: i[3] for i in ITEMS}
MESH_IDS = {i[0]: i[4] for i in ITEMS}


def parse_mesh(path):
    """-> dict(place, glow, grip, quads=[(layer, group, P 4x3, UV 4x2, N 4x3)], flares=[...], skin=str|None, lod=str|None)."""
    out = {"place": None, "glow": None, "grip": None, "quads": [], "flares": [], "skin": None, "lod": None, "manual": False}
    group = ""
    with open(path) as fh:
        for line in fh:
            v = line.split()
            if not v or v[0].startswith("#"):
                continue
            c = v[0]
            if c == "place":
                out["place"] = tuple(float(x) for x in v[1:6])
            elif c == "glow":
                out["glow"] = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
            elif c == "grip":
                out["grip"] = float(v[1])
            elif c == "orient" and len(v) > 1 and v[1] == "manual":
                out["manual"] = True
            elif c == "skin":
                out["skin"] = v[1]
            elif c == "lod":
                out["lod"] = v[1]
            elif c == "g":
                group = v[1] if len(v) > 1 else ""
            elif c == "fl":
                out["flares"].append(v[1:])
            elif c == "q":
                nrm = np.array([float(v[22]), float(v[23]), float(v[24])])
                P = np.array([[float(v[2 + k * 5 + j]) for j in range(3)] for k in range(4)])
                UV = np.array([[float(v[2 + k * 5 + 3]), float(v[2 + k * 5 + 4])] for k in range(4)])
                out["quads"].append((int(v[1]), group, P, UV, np.tile(nrm, (4, 1))))
            elif c == "qn":
                P = np.array([[float(v[2 + k * 8 + j]) for j in range(3)] for k in range(4)])
                UV = np.array([[float(v[2 + k * 8 + 3]), float(v[2 + k * 8 + 4])] for k in range(4)])
                N = np.array([[float(v[2 + k * 8 + 5 + j]) for j in range(3)] for k in range(4)])
                out["quads"].append((int(v[1]), group, P, UV, N))
    return out


def welded_vertices(quads, layers=None, digits=3):
    s = set()
    for (ly, _g, P, _uv, _n) in quads:
        if layers is not None and ly not in layers:
            continue
        for p in P:
            s.add((round(float(p[0]), digits), round(float(p[1]), digits), round(float(p[2]), digits)))
    return len(s)


def texel_stats(path):
    """size, painted texels (alpha > 8) and detail texels (painted texels whose colour differs from the mean of their 4 neighbours by more than 6 of 765)."""
    if not path or not os.path.exists(path):
        return None
    im = np.asarray(Image.open(path).convert("RGBA")).astype(np.int32)
    painted = im[..., 3] > 8
    rgb = im[..., :3]
    pad = np.pad(rgb, ((1, 1), (1, 1), (0, 0)), mode="edge")
    mean = (pad[:-2, 1:-1] + pad[2:, 1:-1] + pad[1:-1, :-2] + pad[1:-1, 2:]) / 4.0
    contrast = np.abs(rgb - mean).sum(axis=2)
    detail = painted & (contrast > 6)
    return {"size": int(im.shape[0]), "painted_texels": int(painted.sum()), "detail_texels": int(detail.sum())}


def load_json(path):
    with open(path) as fh:
        return json.load(fh)


def save_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        json.dump(obj, fh, indent=2, sort_keys=False)
        fh.write("\n")
