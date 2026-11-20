"""Stills of the HERO katanas (white tiger, soul eater, siderite, sakura, wind king, Hellblaze) before and after the true-katana remodel, drawn by the repo's software rasteriser
(tools/item_preview/preview_weapons.py): NOT Cycles, NOT the game. The blade stands upright and every tile uses the same camera and scale, so a blade that is half as wide looks half as wide.

    python3 -B tools/blender/hero_katana_sheet.py sheet  [--out docs/previews/hero_katanas_v2] [--trial DIR]      all_hero_katanas.png: BEFORE row above AFTER row
    python3 -B tools/blender/hero_katana_sheet.py close  ID [--out ...] [--trial DIR]                              <id>.png: side and edge-on, before and after, zoomed on the hilt and on the blade
    python3 -B tools/blender/hero_katana_sheet.py one    ID [--trial DIR] [--out PNG]                              one quick side + edge-on still of the AFTER mesh (for iterating)

BEFORE = the 4.61.0 thinned mesh kept in the classic_thin folders (weapon_meshes/classic_thin, textures/entity/weapon/classic_thin; Hellblaze in up/classic_thin).
AFTER  = the mesh now in weapon_meshes (or in --trial DIR, the output folder of hero_katanas.py --trial).
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
MESHES = os.path.join(A, "weapon_meshes")
SKINS = os.path.join(A, "textures", "entity", "weapon")
THIN = "classic_thin"
IDS = ["vassal_katana__white_tiger_katana", "vassal_katana__soul_eater_katana", "vassal_katana__siderite_katana", "vassal_katana__sakura_stone_katana",
       "legendary_sword__wind_king_katana", "hellblaze_the_demon_sin__katana"]
LABEL = {"vassal_katana__white_tiger_katana": "white tiger katana", "vassal_katana__soul_eater_katana": "soul eater katana", "vassal_katana__siderite_katana": "siderite katana",
         "vassal_katana__sakura_stone_katana": "sakura stone katana", "legendary_sword__wind_king_katana": "wind king katana", "hellblaze_the_demon_sin__katana": "hellblaze katana",
         "hellblaze_the_demon_sin__saya": "hellblaze saya"}


def paths(i, which, trial=None):
    """(mesh path, skin base path) of an id: which = 'before' (the classic_thin copy) | 'after' (weapon_meshes, or the trial folder)."""
    up = "up" if i.startswith("hellblaze_") else ""
    if which == "before":
        return os.path.join(MESHES, up, THIN, i + ".txt"), os.path.join(SKINS, up, THIN, i)
    if trial and up:                                                         # hero_hellblaze.py --trial DIR writes to DIR/up/weapon_meshes ...
        return os.path.join(trial, "up", "weapon_meshes", i + ".txt"), os.path.join(trial, "up", "textures", "entity", "weapon", i)
    if trial:
        return os.path.join(trial, "weapon_meshes", i + ".txt"), os.path.join(trial, "textures", "entity", "weapon", i)
    return os.path.join(MESHES, up, i + ".txt"), os.path.join(SKINS, up, i)


def load_raw(path):
    """The static quads of a mesh file in mesh units / 100, no place transform (blade along +y), plus the glow header."""
    glow, quads, group = None, [], "-"
    for line in open(path):
        v = line.split()
        if not v or v[0] == "#":
            continue
        if v[0] == "g":
            group = v[1] if len(v) > 1 else "-"
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "q" and group == "-":
            p = np.array([float(x) for x in v[2:22]]).reshape(4, 5)
            quads.append((int(v[1]), p, np.array([float(x) for x in v[22:25]])))
        elif v[0] == "qn" and group == "-":
            arr = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
            nn = arr[:, 5:8].sum(axis=0)
            quads.append((int(v[1]), arr[:, :5].copy(), nn / (np.linalg.norm(nn) or 1.0)))
    out = []
    for (ly, p, n) in quads:
        if ly <= 2:
            q = p.copy()
            q[:, :3] /= 100.0
            out.append((ly, q, n))
    return out, glow


def draw(mesh_path, skin_base, yaw, centre_y, half_h, size, pitch=0.0, cx=0.0):
    """One still: `half_h` cm is half the height of the picture, centred on y = centre_y."""
    pw.W, pw.H = size
    pw.STARS = np.zeros((pw.H, pw.W, 3), np.float32) + [0.05, 0, 0.02]
    quads, glow = load_raw(mesh_path)
    skin, gl = pw.tex(skin_base + ".png"), pw.tex(skin_base + "_glow.png")
    if skin is None:
        return np.zeros((pw.H, pw.W, 3), np.float32) + 0.3
    k = skin.shape[1] / 128.0
    quads = [(ly, np.concatenate([p[:, :3], p[:, 3:5] * k], axis=1), n) for (ly, p, n) in quads]
    dist = (half_h / 100.0) / math.tan(math.radians(25))
    cam = pw.view(yaw, pitch, dist=dist, target=(cx / 100.0, centre_y / 100.0, 0.0))
    return pw.render(quads, skin, gl, glow, cam)


def label(img, text, sub=""):
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, im.width, 14), fill=(20, 22, 30))
    d.text((3, 2), text[:60], fill=(255, 255, 255))
    if sub:
        d.rectangle((0, im.height - 13, im.width, im.height), fill=(20, 22, 30))
        d.text((3, im.height - 12), sub, fill=(255, 220, 150))
    return np.asarray(im, np.float32) / 255


def extent(path):
    q, _ = load_raw(path)
    ys = np.concatenate([p[:, 1] for (_, p, _) in q]) * 100.0
    return float(ys.min()), float(ys.max())


def sheet(out_dir, trial=None, tile=(260, 640)):
    os.makedirs(out_dir, exist_ok=True)
    rows = []
    for which in ("before", "after"):
        tiles = []
        for i in IDS:
            mp, sp = paths(i, which, trial)
            lo, hi = extent(paths(i, "before")[0])                                  # one frame for both rows: the old mesh's extent
            tiles.append(label(draw(mp, sp, 0, 0.5 * (lo + hi), 0.5 * (hi - lo) * 1.04, tile), LABEL[i], which.upper()))
        rows.append(np.concatenate(tiles, 1))
    full = np.concatenate(rows, 0)
    p = os.path.join(out_dir, "all_hero_katanas.png")
    Image.fromarray((full * 255).astype(np.uint8)).save(p, optimize=True)
    return p


def close(i, out_dir, trial=None):
    """Columns BEFORE | AFTER; rows: the whole weapon from the side, the hilt and guard from the side, the guard edge-on (blade seen from its edge), the blade's upper half from the side."""
    os.makedirs(out_dir, exist_ok=True)
    cols = []
    lo, hi = extent(paths(i, "before")[0])
    for which in ("before", "after"):
        mp, sp = paths(i, which, trial)
        t = lo + 0.30 * (hi - lo)
        col = [label(draw(mp, sp, 0, 0.5 * (lo + hi), 0.5 * (hi - lo) * 1.04, (330, 640)), LABEL.get(i, i) + " side", which.upper()),
               label(draw(mp, sp, 0, lo + 0.40 * (hi - lo), 0.2 * (hi - lo), (330, 330)), "hilt + guard side", which.upper()),
               label(draw(mp, sp, 90, lo + 0.40 * (hi - lo), 0.2 * (hi - lo), (330, 330)), "hilt + guard edge-on", which.upper()),
               label(draw(mp, sp, 0, lo + 0.62 * (hi - lo), 0.22 * (hi - lo), (330, 330), cx=-4), "blade side", which.upper())]
        cols.append(np.concatenate(col, 0))
    img = np.concatenate(cols, 1)
    p = os.path.join(out_dir, "close_%s.png" % i)
    Image.fromarray((img * 255).astype(np.uint8)).save(p, optimize=True)
    return p


def one(i, trial, out):
    lo, hi = extent(paths(i, "before")[0])
    mp, sp = paths(i, "after", trial)
    tiles = [label(draw(mp, sp, 0, 0.5 * (lo + hi), 0.5 * (hi - lo) * 1.04, (300, 700)), "side"),
             label(draw(mp, sp, 90, 0.5 * (lo + hi), 0.5 * (hi - lo) * 1.04, (300, 700)), "edge-on"),
             label(draw(mp, sp, 0, lo + 0.40 * (hi - lo), 0.2 * (hi - lo), (460, 460)), "hilt"),
             label(draw(mp, sp, 0, lo + 0.62 * (hi - lo), 0.2 * (hi - lo), (460, 460), cx=-3), "blade")]
    right = np.concatenate(tiles[2:], 0)
    left = np.concatenate(tiles[:2], 1)
    h = max(left.shape[0], right.shape[0])
    pad = lambda a: np.concatenate([a, np.zeros((h - a.shape[0], a.shape[1], 3), np.float32) + 0.1], 0)    # noqa: E731
    img = np.concatenate([pad(left), pad(right)], 1)
    Image.fromarray((img * 255).astype(np.uint8)).save(out, optimize=True)
    return out


if __name__ == "__main__":
    args = sys.argv[1:]
    opt = {"--out": None, "--trial": None}
    for k in list(opt):
        if k in args:
            j = args.index(k)
            opt[k] = args[j + 1]
            del args[j:j + 2]
    out = opt["--out"] or os.path.join(ROOT, "docs", "previews", "hero_katanas_v2")
    if args and args[0] == "sheet":
        print(sheet(out, opt["--trial"]))
    elif args and args[0] == "close":
        print(close(args[1], out, opt["--trial"]))
    elif args and args[0] == "one":
        print(one(args[1], opt["--trial"], opt["--out"] or "/tmp/one.png"))
    else:
        print(__doc__)
