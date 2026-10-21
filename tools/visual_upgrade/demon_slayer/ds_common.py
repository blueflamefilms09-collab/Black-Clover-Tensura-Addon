"""Paths and readers of the demon_slayer visual upgrade (slice key `demon_slayer`, release 1.108.0): the Genesis Demon-Slayer Sword (`demon_slayer_sword`).

The slice reuses the genesis_weapons pipeline (tools/visual_upgrade/genesis_weapons: the painter, the geometry builders, the atlas packer, the mesh writer, the
halo sheet) by putting that folder on sys.path; nothing of it is edited. The old model is code (client/DemonSlayerMesh.java, a pixel model dumped by
tools/item_preview/MeshDump.java), so its baseline is measured from a dump of that code, not from a `weapon_meshes/<id>.txt`.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
GW = os.path.join(HERE, "..", "genesis_weapons")
sys.path.insert(0, GW)
from common import (ROOT, ASSETS, VU_MESH, VU_TEX, SPEC_DIR, DOCS, parse_mesh, welded_vertices, texel_stats, load_json, save_json)  # noqa: E402,F401

SLICE = "demon_slayer"
ID = "demon_slayer_sword"
NAME = "Demon-Slayer Sword (Genesis)"
PREVIEWS = os.path.join(ROOT, "docs", "previews", "visual_upgrade", SLICE)
OLD_TEX = os.path.join(ASSETS, "textures", "entity")
SCRATCH = os.environ.get("DS_SCRATCH", os.path.join(ROOT, "build", "ds_scratch"))
DS_MESH_JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "DemonSlayerMesh.java")
MESH_DUMP_JAVA = os.path.join(ROOT, "tools", "item_preview", "MeshDump.java")
LAYERS = {0: "body", 1: "void", 2: "clover", 3: "bloom", 4: "crack"}


def build_dump():
    """Compiles the real in-game mesh code with the preview's MeshDump (plain javac, no game) into the scratch folder."""
    classes = os.path.join(SCRATCH, "classes")
    os.makedirs(classes, exist_ok=True)
    subprocess.run(["javac", "-nowarn", "-d", classes, DS_MESH_JAVA, MESH_DUMP_JAVA], check=True)
    return classes


def dump_old(classes, res=0, fracture=0.0, time=0.0):
    """-> [(layer, 4x5 corners (x y z u v), normal, argb)] of the old Demon-Slayer in item space."""
    import numpy as np
    out = subprocess.run(["java", "-cp", classes, "MeshDump", str(res), str(fracture), str(time)], check=True, capture_output=True, text=True).stdout
    quads = []
    for line in out.splitlines():
        v = line.split()
        if len(v) < 25:
            continue
        quads.append((int(v[0]), np.array([float(x) for x in v[1:21]]).reshape(4, 5), np.array([float(x) for x in v[21:24]]), int(v[24])))
    return quads
