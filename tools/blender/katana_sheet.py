"""Before / after stills of the katanas, drawn by the repo's software rasteriser (tools/item_preview/preview_weapons.py): NOT Cycles, NOT the game.

    python3 -B tools/blender/katana_sheet.py sheet [--out docs/previews/katanas_v2]      the contact sheet (before row, after row) + every pair
    python3 -B tools/blender/katana_sheet.py close ID_A [ID_B] [--out ...]                a side-view close-up of two blades (before and after)

BEFORE = what the game draws today for the id: the thickened copy (weapon_meshes_sz) when there is one, else the mesh; the classic copy kept for it is the same
geometry. AFTER = the mesh now in weapon_meshes. Both use the skin that goes with the file, the same camera and the same place scale, so a blade that is half
as thick looks half as thick.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "item_preview"))
import preview_weapons as pw  # noqa: E402

A = pw.ASSETS
SKINS = os.path.join(A, "textures", "entity", "weapon")

# id -> (label, before mesh path under weapon_meshes*, before skin base path under textures/entity/weapon, after mesh path, after skin base)
# the 'before' of an id with a classic copy is that copy (the kept old model); else the thickened copy (the old size rule) when it exists, else the mesh itself
CLASSIC_KATANA = "classic_vassal"      # where this change keeps the old katana meshes and skins (weapon_meshes/classic_vassal, textures/entity/weapon/classic_vassal)
IDS = [
    "bloodbound_katana", "demon_slasher_katana", "miasma_infused_katana", "hakurou_school_practice_katana", "selection_hopefuls_bokken",
    "katana_of_carnage", "katana_of_the_crimson_shogun", "shadowsteel_katana", "void_origin_katana", "corpse_eater", "ilmare_gin_the_starlight_silver",
    "kusanagi_the_grass_cutter", "dragon_tooth_blade_red", "wavebreaker",
    "vassal_katana", "vassal_katana__white_tiger_katana", "vassal_katana__soul_eater_katana", "vassal_katana__siderite_katana", "vassal_katana__sakura_stone_katana",
    "legendary_sword__wind_king_katana", "hellblaze_the_demon_sin__katana",
]


def load_mesh(path):
    """pw.load for any path: body (0), void (1) and glow (2) layers of the static group only (a `g` group other than '-' is an effect)."""
    place, glow, quads, group = None, None, [], "-"
    for line in open(path):
        v = line.split()
        if not v or v[0] == "#":
            continue
        if v[0] == "g":
            group = v[1] if len(v) > 1 else "-"
        elif v[0] == "place":
            place = [float(x) for x in v[1:6]]
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "q" and group == "-":
            pts = np.array([float(x) for x in v[2:22]]).reshape(4, 5)
            quads.append((int(v[1]), pts, np.array([float(x) for x in v[22:25]])))
        elif v[0] == "qn" and group == "-":
            arr = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
            nn = arr[:, 5:8].sum(axis=0)
            quads.append((int(v[1]), arr[:, :5].copy(), nn / (np.linalg.norm(nn) or 1.0)))
    quads = [q for q in quads if q[0] <= 2]
    ox, oy, ang, sc, ymin = place
    a = math.radians(ang - 90)
    R = np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])
    out = []
    for (layer, pts, n) in quads:
        p = pts.copy()
        p[:, :3] = (p[:, :3] - [0, ymin, 0]) * sc @ R.T + [ox, oy, 0.5]
        out.append((layer, p, R @ n))
    return out, glow


def draw(mesh_path, skin_base, view, size=(230, 230)):
    pw.W, pw.H = size
    pw.STARS = np.zeros((pw.H, pw.W, 3), np.float32) + [0.05, 0, 0.02]
    quads, glow = load_mesh(mesh_path)
    skin, gl = pw.tex(skin_base + ".png"), pw.tex(skin_base + "_glow.png")
    if skin is None:
        return np.zeros((pw.H, pw.W, 3), np.float32) + 0.3
    k = skin.shape[1] / 128.0
    quads = [(ly, np.concatenate([p[:, :3], p[:, 3:5] * k], axis=1), n) for (ly, p, n) in quads]
    return pw.render(quads, skin, gl, glow, view)


def paths(i, which):
    """(mesh path, skin base) of an id: which = 'before' | 'after'. The before copy is the classic_vassal copy when it was made, else what is drawn now."""
    m = os.path.join(A, "weapon_meshes")
    if i.startswith("hellblaze_"):                           # the visual-upgrade meshes live in up/ (the old copy in up/classic_vassal, kept on disk)
        up = os.path.join(m, "up")
        if which == "before":
            return os.path.join(up, CLASSIC_KATANA, i + ".txt"), os.path.join(SKINS, "up", CLASSIC_KATANA, i)
        return os.path.join(up, i + ".txt"), os.path.join(SKINS, "up", i)
    if which == "before":
        c = os.path.join(m, CLASSIC_KATANA, i + ".txt")
        if os.path.exists(c):
            return c, os.path.join(SKINS, CLASSIC_KATANA, i)
        up = os.path.join(m, "up", i + ".txt")
        vu = os.path.join(A, "weapon_meshes_vu", i + ".txt")
        sz = os.path.join(A, "weapon_meshes_sz", i + ".txt")
        for cand, skin in ((os.path.join(A, "weapon_meshes_vu_sz", i + ".txt"), None), (os.path.join(A, "weapon_meshes_sz", "up", i + ".txt"), os.path.join(SKINS, "up", i)),
                           (sz, os.path.join(SKINS, i)), (up, os.path.join(SKINS, "up", i)), (os.path.join(m, i + ".txt"), os.path.join(SKINS, i))):
            if os.path.exists(cand) and (skin is None or os.path.exists(skin + ".png")):
                return cand, skin or os.path.join(SKINS, i)
    return os.path.join(m, i + ".txt"), os.path.join(SKINS, i)


def label(img, text, sub=""):
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, im.width, 14), fill=(20, 22, 30))
    d.text((3, 2), text[:34], fill=(255, 255, 255))
    if sub:
        d.rectangle((0, im.height - 13, im.width, im.height), fill=(20, 22, 30))
        d.text((3, im.height - 12), sub, fill=(255, 220, 150))
    return np.asarray(im, np.float32) / 255


def sheet(out_dir, ids=IDS, cols=7, size=(230, 230)):
    os.makedirs(out_dir, exist_ok=True)
    view = pw.view(0, 0)
    bands = []
    for b in range(0, len(ids), cols):
        chunk = ids[b:b + cols]
        rows = []
        for which in ("before", "after"):
            tiles = []
            for i in chunk:
                mp, sp = paths(i, which)
                if not os.path.exists(mp):
                    tiles.append(np.zeros((size[1], size[0], 3), np.float32) + 0.2)
                    continue
                tiles.append(label(draw(mp, sp, view, size), i.replace("legendary_sword__", "").replace("vassal_katana__", "vassal: ").replace("hellblaze_the_demon_sin__", "hellblaze: "), which.upper()))
            while len(tiles) < cols:
                tiles.append(np.zeros((size[1], size[0], 3), np.float32) + 0.12)
            rows.append(np.concatenate(tiles, 1))
        bands.append(np.concatenate(rows, 0))
    full = np.concatenate(bands, 0)
    p = os.path.join(out_dir, "all_katanas.png")
    Image.fromarray((full * 255).astype(np.uint8)).save(p, optimize=True)
    return p


def close(a, b, out_dir, size=(520, 900)):
    """Two blades side by side, before and after each: the face seen from the side the player sees it, zoomed on the blade."""
    os.makedirs(out_dir, exist_ok=True)
    cols = []
    for i in (a, b):
        if not i:
            continue
        for which in ("before", "after"):
            mp, sp = paths(i, which)
            # the blade's middle on the diagonal of the sprite cube, seen from the front: target a little above the middle of the cube
            view = pw.view(0, 0, dist=0.95, target=(0.62, 0.62, 0.5))
            cols.append(label(draw(mp, sp, view, (size[0] // 2, size[1] // 2)), i, which.upper()))
            view2 = pw.view(88, 0, dist=0.95, target=(0.5, 0.62, 0.62))
            cols[-1] = np.concatenate([cols[-1], label(draw(mp, sp, view2, (size[0] // 2, size[1] // 2)), i + " edge-on", which.upper())], 0)
    img = np.concatenate(cols, 1)
    p = os.path.join(out_dir, "close_%s.png" % "_".join(x for x in (a, b) if x))
    Image.fromarray((img * 255).astype(np.uint8)).save(p, optimize=True)
    return p


if __name__ == "__main__":
    args = sys.argv[1:]
    out = os.path.join(ROOT, "docs", "previews", "katanas_v2")
    if "--out" in args:
        k = args.index("--out")
        out = args[k + 1]
        del args[k:k + 2]
    if args and args[0] == "sheet":
        print(sheet(out))
    elif args and args[0] == "close":
        print(close(args[1], args[2] if len(args) > 2 else None, out))
    else:
        print(__doc__)
