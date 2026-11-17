"""Software preview of the djinn/belial_bone set (no game, no GPU): the pieces are put together roughly the way DjinnArmorLayer pivots them (author frame, y up,
+z the front; the piece files are read back through the layer's (x, y, z) -> (x, -y, -z) flip) and drawn with tools/relics/preview_mesh.py's rasteriser.
The placement is an APPROXIMATION of the layer (no pose, no animation), meant to judge shapes and the atlas, not the in-game pose.

    python tools/blender/belial_bone_preview.py [--root <dir with djinn/belial_bone and textures/entity/djinn>] --out docs/belial/previews
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as PM  # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")

# piece -> list of (dx, dy, dz, mirror_x) placements in the body frame (author frame; the limb pieces use the djinn_parts ANCHOR table)
TAIL_N = 7
PLACE = {
    "chest": [(0, 0, 0)], "belt": [(0, 0, 0)], "helm": [(0, 0, 0)], "horn_l": [(0, 0, 0)], "horn_r": [(0, 0, 0)], "crest": [(0, 0, 0)],
    "pauldron_l": [(5, -2, 0)], "vambrace_l": [(5, -2, 0)], "gauntlet_l": [(5, -2, 0)],
    "pauldron_r": [(-5, -2, 0)], "vambrace_r": [(-5, -2, 0)], "gauntlet_r": [(-5, -2, 0)],
    "greave_l": [(1.9, -12, 0)], "greave_r": [(-1.9, -12, 0)],
    "wing_in_l": [(2.2, -0.6, -3.4)], "wing_in_r": [(-2.2, -0.6, -3.4)],
    "wing_out_l": [(13.2, 0.0, -3.4)], "wing_out_r": [(-13.2, 0.0, -3.4)],
    "mantle": [(0, 0, 0)], "backdrop": [(0, 6.5, -5.5)],
    "tail_seg": [(0, -10.4, -3.0 - 5.8 * i * 0.9, i) for i in range(TAIL_N)],
    "tail_tip": [(0, -10.4 - 0.0, -3.0 - 5.8 * 0.9 * TAIL_N, 0)],
    "weapon": [(-9.5, -9.0, 4.0)],
}


def load_piece(path, flip):
    d = PM.parse_game_txt(path)
    out = []
    for (layer, P, UV, N) in d["quads"]:
        if flip:
            P = P * np.array([1, -1, -1])
            N = N * np.array([1, -1, -1])
            # (x, -y, -z) is a half turn about x (det +1), so the quad winding stays as written
        out.append((layer, P, UV, N))
    return out


def assemble(root, piece_names):
    quads = []
    for name in piece_names:
        f = os.path.join(root, "djinn", "belial_bone", name + ".txt")
        if not os.path.exists(f):
            continue
        base = load_piece(f, name != "weapon")
        for pl in PLACE.get(name, [(0, 0, 0)]):
            off = np.array(pl[:3], np.float32)
            k = pl[3] if len(pl) > 3 else 0
            for (layer, P, UV, N) in base:
                Q = P.copy()
                if name == "tail_seg":
                    # each further segment curls up a little (a swing about x), like the layer's chain
                    a = 0.075 * k
                    c, s = np.cos(a), np.sin(a)
                    Q = np.stack([Q[:, 0], Q[:, 1] * c - Q[:, 2] * s, Q[:, 1] * s + Q[:, 2] * c], axis=1)
                quads.append((layer, Q + off, UV, N, (0.7, 0.7, 0.7)))
    return quads


def render(quads, skin, glow_tex, views, panel, focus=None, out=None, label=True):
    allp = np.concatenate([q[1] for q in quads if q[0] == 0] or [q[1] for q in quads])
    fit_p = allp
    if focus is not None:
        sel = (allp[:, 0] >= focus[0]) & (allp[:, 0] <= focus[1]) & (allp[:, 1] >= focus[2]) & (allp[:, 1] <= focus[3]) & (allp[:, 2] >= focus[4]) & (allp[:, 2] <= focus[5])
        fit_p = allp[sel] if sel.any() else allp
    pw, ph = panel
    tiles = []
    for v in views:
        yaw, pitch = PM.VIEWS[v]
        Q = fit_p @ PM._rot(yaw, pitch).T
        w, h = np.ptp(Q[:, 0]), np.ptp(Q[:, 1])
        c = ((Q[:, 0].max() + Q[:, 0].min()) / 2, (Q[:, 1].max() + Q[:, 1].min()) / 2)
        sc = min((pw - 24) / max(w, 1e-6), (ph - 36) / max(h, 1e-6))
        im = PM.render_view(quads, yaw, pitch, (pw, ph), sc, c, skin, glow_tex, (0xFFFFFF, 0.12, 0.45, 1.0), 2, True, False, True)
        tiles.append((v, im))
    img = Image.fromarray((np.concatenate([t[1] for t in tiles], axis=1) * 255).astype(np.uint8))
    if label:
        d = ImageDraw.Draw(img)
        x = 0
        for (name, im) in tiles:
            d.text((x + 6, 4), name, fill=(255, 255, 255))
            x += im.shape[1]
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    img.save(out)
    return out


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=ASSETS)
    ap.add_argument("--out", default=os.path.join(ROOT, "docs", "belial", "previews"))
    a = ap.parse_args(argv)
    skin = os.path.join(a.root, "textures", "entity", "djinn", "belial_bone_armor.png")
    glow = os.path.join(a.root, "textures", "entity", "djinn", "belial_bone_armor_glow.png")
    skin, glow = (skin if os.path.exists(skin) else None), (glow if os.path.exists(glow) else None)
    allp = list(PLACE.keys())
    quads = assemble(a.root, allp)
    print("quads drawn (tail repeated):", len(quads))
    render(quads, skin, glow, ("profile", "three_quarter", "back"), (520, 760), out=os.path.join(a.out, "bone_set_assembled.png"))
    dragon = assemble(a.root, ["backdrop", "tail_seg", "tail_tip", "wing_in_l", "wing_in_r", "wing_out_l", "wing_out_r", "mantle"])
    render(dragon, skin, glow, ("profile", "three_quarter", "back"), (520, 760), out=os.path.join(a.out, "bone_dragon_parts.png"))
    skull = assemble(a.root, ["backdrop"])
    render(skull, skin, glow, ("profile", "three_quarter", "profile"), (620, 620), focus=(-8, 14, 12, 28, -4, 14), out=os.path.join(a.out, "bone_skull_closeup.png"))


if __name__ == "__main__":
    main(sys.argv[1:])
