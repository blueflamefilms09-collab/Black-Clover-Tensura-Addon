#!/usr/bin/env python3
"""Art of the Djinn system (relics/djinn, client/relics/djinn). Idempotent: running it again rewrites the same files.

    python3 tools/relics/gen_djinn_art.py                   everything, all twelve
    python3 tools/relics/gen_djinn_art.py --only armor      armour meshes + skins only (also: icons, skins, bust)
    python3 tools/relics/gen_djinn_art.py --djinn baal      one Djinn
    python3 tools/relics/gen_djinn_art.py --preview out_dir render the armour on a mannequin (front / three-quarter / side / back) into out_dir

What it writes (assets/nusmp):
    textures/item/djinn_seal_<id>.png + models/item/djinn_seal_<id>.json    the twelve Seals (16x16, parent item/generated)
    textures/entity/djinn/<id>_rune.png        64x64 skin overlay: the body-art, drawn as a second additive pass over the player model
    textures/entity/djinn/djinn_silhouette.png   one white 64x64 mask of the skin's UV islands (the fresnel aura pass is drawn through it)
    textures/entity/djinn/<id>_bust.png        32x32 bust of the Lesser Summon, drawn as a billboard
    textures/entity/djinn/<id>_armor.png + <id>_armor_glow.png    the armour's painted skin and its additive glow (mesh_lib Atlas)
    djinn/<id>/<piece>.txt                     the armour pieces (mesh_lib quads, per-vertex normals "qn", UV space 128)
All of it is generated here and committed as PNG / text: nothing is built at run time except the Java layer's use of these files.
The Djinn's colours and Look come from relics/djinn/Djinn.java itself (parsed below), so the table stays the single source.
"""
import argparse
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                      # noqa: E402
import djinn_parts as dp                   # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
DJINN_JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "djinn", "Djinn.java")

ROW = re.compile(r'(\w+)\("(\w+)", "([^"]+)", Affinity\.(\w+), 0x([0-9A-Fa-f]+), 0x([0-9A-Fa-f]+), "([^"]+)",\s*'
                 r'new Look\(Look\.Horns\.(\w+), Look\.Wings\.(\w+), Look\.Tail\.(\w+), Look\.Halo\.(\w+), (true|false)\)')


def roster():
    src = open(DJINN_JAVA).read()
    out = []
    for m in ROW.finditer(src):
        name, disp, title, aff, col, glow, form, hn, wg, tl, hl, ears = m.groups()
        out.append(dict(name=name, id=name.lower(), display=disp, title=title, affinity=aff, color=int(col, 16), glow=int(glow, 16), form=form,
                        look=dict(horns=hn, wings=wg, tail=tl, halo=hl, ears=(ears == "true"))))
    if len(out) != 12:
        raise SystemExit(f"expected 12 Djinn in {DJINN_JAVA}, parsed {len(out)}")
    return out


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


# ================================================================ armour
def build_mesh(dj):
    did = dj["id"]
    dp.register_materials(did, dj["color"], dj["glow"])
    m = ml.Mesh(did + "_armor")
    k = dp.Kit(m, did, dj)
    dp.build_helm(k)
    dp.build_horns(k)
    dp.build_ears(k)
    dp.build_crest(k)
    dp.build_chest(k)
    dp.build_belt(k)
    dp.build_pauldron(k)
    dp.build_vambrace(k)
    dp.build_gauntlet(k)
    dp.build_greave(k)
    dp.build_wings(k)
    dp.build_tail(k)
    dp.build_halo(k)
    dp.build_sheath(k)
    m.set_glow("FFFFFF", 0.12, 0.7, 1.0)
    return m


def paint_glow(atlas, mesh, glow_rgb):
    """Fills the additive skin under every glow part: bright along the middle of each strip, softer to its edges."""
    col = np.array(glow_rgb, np.float32)
    for g in atlas.groups:
        if not (g.part.glow or g.part.layer == 2):
            continue
        x, y, w, h = g.rect
        xs = np.linspace(-1, 1, max(w, 1), dtype=np.float32)[None, :]
        ys = np.linspace(-1, 1, max(h, 1), dtype=np.float32)[:, None]
        r = np.sqrt(xs ** 2 * 0.6 + ys ** 2 * 0.4)
        k = np.clip(1.05 - r * 0.55, 0.35, 1.0)
        core = np.clip(1 - np.abs(xs) * 1.6, 0, 1) * 0.35
        patch = np.zeros((h, w, 4), np.float32)
        patch[..., :3] = col * k[..., None] + 255 * core[..., None]
        patch[..., 3] = 255
        atlas.glow[y:y + h, x:x + w] = np.clip(patch, 0, 255)


def finish_atlas(mesh, dj, size=1024):
    atlas = ml.Atlas(size)
    atlas.layout(mesh)
    atlas.paint(mesh)
    paint_glow(atlas, mesh, rgb(dj["glow"]))
    return atlas


def export_pieces(mesh, atlas, did, out_dir):
    """One file per piece: the quads of the parts named <piece>__*, converted from author space to the game's model space."""
    mesh.finish()
    by = {}
    for layer, P, UV, N, part in mesh.iter_quads("vertex", atlas):
        piece = part.name.split("__", 1)[0]
        by.setdefault(piece, []).append((layer, P, UV, N))
    os.makedirs(out_dir, exist_ok=True)
    counts = {}
    flip = np.array([1.0, -1.0, -1.0])
    for piece in dp.PIECES:
        quads = by.get(piece, [])
        lines = [f"# {did} {piece}: generated by tools/relics/gen_djinn_art.py (mesh_lib, author space turned to model space); UV space 128"]
        for layer, P, UV, N in quads:
            if layer == 1:
                continue
            lines.append("qn %d %s" % (layer, " ".join("%.2f %.2f %.2f %.2f %.2f %.3f %.3f %.3f" % (*(P[i] * flip), *UV[i], *(N[i] * flip)) for i in range(4))))
        path = os.path.join(out_dir, piece + ".txt")
        if len(lines) == 1 and not os.path.exists(path):
            continue
        with open(path, "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")
        counts[piece] = len(lines) - 1
    return counts


def save_atlas(atlas, did):
    d = os.path.join(ASSETS, "textures", "entity", "djinn")
    os.makedirs(d, exist_ok=True)
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, did + "_armor.png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, did + "_armor_glow.png"), optimize=True)


def make_armor(dj, atlas_size=1024):
    mesh = build_mesh(dj)
    atlas = finish_atlas(mesh, dj, atlas_size)
    counts = export_pieces(mesh, atlas, dj["id"], os.path.join(ASSETS, "djinn", dj["id"]))
    save_atlas(atlas, dj["id"])
    return mesh, atlas, counts


# ================================================================ the mannequin preview
HALF_SET = ("crest", "pauldron_r", "vambrace_r", "gauntlet_r", "sheath")


def mannequin_scene(dj, wings_open=True, half=False):
    """The armour of one Djinn on a mannequin in the poses the game puts it in: a Mesh to hand to mesh_lib.preview()."""
    base = build_mesh(dj)
    scene = ml.Mesh(dj["id"] + "_scene")
    ANCH = dict(dp.ANCHOR)
    for p in base.parts:
        piece = p.name.split("__", 1)[0]
        if half and piece not in HALF_SET:
            continue
        if piece in ANCH:
            off = ANCH[piece]
        elif piece in ("wing_in_l", "wing_in_r"):
            sgn = 1 if piece.endswith("_l") else -1
            off = (sgn * 1.8, -3.0, -2.9)
        elif piece in ("wing_out_l", "wing_out_r"):
            sgn = 1 if piece.endswith("_l") else -1
            off = (sgn * (1.8 + 11 * 0.78), -3.0 + 11 * 0.55, -3.2)
        elif piece == "tail_seg":
            off = (0, -11.0, -2.8)
        elif piece == "tail_tip":
            off = (0, -11.0, -2.8 - 6.0)
        elif piece == "halo":
            off = (0, 12.5, 0)
        elif piece == "sheath":
            off = (-5.0, -12.5, 6.0)
        else:
            off = (0, 0, 0)
        q = p.translated(*off, name=p.name)
        if piece in ("sheath",):
            q = p.rotated("x", -90, name=p.name).translated(*off, name=p.name)
        if piece.startswith("wing_out") and wings_open:
            sgn = 1 if piece.endswith("_l") else -1
            q = p.rotated("z", sgn * -25, name=p.name).translated(*off, name=p.name)
        if piece.startswith("wing_in") and wings_open:
            sgn = 1 if piece.endswith("_l") else -1
            q = p.rotated("z", sgn * 38, name=p.name).translated(*off, name=p.name)
        scene.add(q)
    # the mannequin: grey boxes in the player's proportions
    def box(name, p0, p1, base=(150, 118, 96)):
        part = ml.Part("mq__" + name, material="stone", base=base, smooth=0)
        ml.box(part, p0, p1)
        scene.add(part)
    box("head", (-4, 0, -4), (4, 8, 4))
    box("body", (-4, -12, -2), (4, 0, 2))
    box("arm_r", (-8, -12, -2), (-4, 0, 2))
    box("arm_l", (4, -12, -2), (8, 0, 2))
    box("leg_r", (-4, -24, -2), (0, -12, 2))
    box("leg_l", (0, -24, -2), (4, -12, 2))
    scene.set_glow("FFFFFF", 0.12, 0.9, 1.0)
    return scene


VIEWS = ["profile", "three_quarter", "edge", "back"]
PANEL = (420, 640)


def make_preview(dj, out_dir, half=False):
    scene = mannequin_scene(dj, half=half)
    atlas = finish_atlas(scene, dj, 2048)
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, dj["id"] + ("_half" if half else "_armor") + ".png")
    ml.preview(scene, out_png=path, atlas=atlas, views=tuple(VIEWS), panel=PANEL, normals="vertex", label=True)
    return path


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", choices=["armor", "icons", "skins", "bust"])
    ap.add_argument("--djinn")
    ap.add_argument("--preview")
    ap.add_argument("--atlas", type=int, default=1024)
    ap.add_argument("--half", action="store_true", help="preview the Half Equip set only")
    ap.add_argument("--views", default="profile,three_quarter,edge,back")
    ap.add_argument("--panel", default="420x640")
    a = ap.parse_args(argv)
    global VIEWS, PANEL
    VIEWS = a.views.split(",")
    PANEL = tuple(int(x) for x in a.panel.split("x"))
    djs = [d for d in roster() if not a.djinn or d["id"] == a.djinn.lower()]
    if not djs:
        raise SystemExit("no such Djinn")
    for dj in djs:
        if a.preview:
            print("preview", make_preview(dj, a.preview, a.half))
            continue
        if a.only in (None, "armor"):
            mesh, atlas, counts = make_armor(dj, a.atlas)
            print(dj["id"], "armour:", sum(counts.values()), "quads in", len(counts), "pieces; atlas ppu %.2f" % atlas.ppu)
        if a.only in (None, "icons"):
            import djinn_icons
            djinn_icons.make_icon(dj, ASSETS)
        if a.only in (None, "skins"):
            import djinn_icons
            djinn_icons.make_skin(dj, ASSETS)
        if a.only in (None, "bust"):
            import djinn_icons
            djinn_icons.make_bust(dj, ASSETS)
    if a.only in (None, "skins") and not a.preview:
        import djinn_icons
        djinn_icons.make_silhouette(ASSETS)


if __name__ == "__main__":
    main(sys.argv[1:])
