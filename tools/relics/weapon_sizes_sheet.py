"""weapon_sizes_sheet: before / after contact sheets of the daggers and katanas (1.110.0, docs/previews/weapon_sizes/).

    python tools/relics/weapon_sizes_sheet.py [--out docs/previews/weapon_sizes] [--ids a,b]

Uses the rasteriser of tools/item_preview/preview_weapons.py (the same mesh placement, skins and lighting as that tool). Each row is one mesh: the OLD file (left three tiles:
front, three-quarter, edge-on) and the NEW weapon_meshes_sz copy (right three tiles), all in the same camera, so a thicker blade and a longer dagger are directly comparable.
The numbers under each row are the apparent third-person length and the blade width / thickness in blocks (tools/relics/weapon_sizes.py --table).
"""
import argparse
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "item_preview"))
import preview_weapons as pw          # noqa: E402
import weapon_sizes as ws             # noqa: E402

TILE = 176


def load_path(path):
    place, glow, quads, vu, skin_name, group = None, None, [], False, None, "-"
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "vu":
            vu = True
        elif v[0] == "skin":
            skin_name = v[1]
        elif v[0] == "g":
            group = v[1] if len(v) > 1 else "-"
        elif v[0] == "place":
            place = [float(x) for x in v[1:]]
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "q":
            layer = int(v[1])
            if layer < 3:
                quads.append((layer, np.array([float(x) for x in v[2:22]]).reshape(4, 5), np.array([float(x) for x in v[22:25]])))
        elif v[0] == "qn":
            layer = int(v[1])
            if layer < 3 and (not vu or group == "-"):
                arr = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
                nn = arr[:, 5:8].sum(axis=0)
                quads.append((layer, arr[:, :5].copy(), nn / (np.linalg.norm(nn) or 1.0)))
    ox, oy, ang, sc, ymin = place
    a = math.radians(ang - 90)
    R = np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])
    out = []
    for (layer, pts, n) in quads:
        p = pts.copy()
        p[:, :3] = (p[:, :3] - [0, ymin, 0]) * sc @ R.T + [ox, oy, 0.5]
        out.append((layer, p, R @ n))
    return out, glow, skin_name


def skins(item_id, tag, skin_name):
    d = os.path.join(pw.ASSETS, "textures", "entity")
    if tag == "vu":
        base = os.path.join(d, "weapon_vu", (skin_name or item_id))
    elif tag == "up":
        base = os.path.join(d, "weapon", "up", item_id)
    else:
        base = os.path.join(d, "weapon", item_id)
    return pw.tex(base + ".png"), pw.tex(base + "_glow.png")


def tiles(path, item_id, tag):
    quads, glow, skin_name = load_path(path)
    skin, glowtex = skins(item_id, tag, skin_name)
    if skin is None:
        return [np.zeros((pw.H, pw.W, 3), np.float32)] * 3
    k = skin.shape[1] / 128.0
    quads = [(ly, np.concatenate([pts[:, :3], pts[:, 3:5] * k], axis=1), n) for (ly, pts, n) in quads]
    return [pw.render(quads, skin, glowtex, glow, pw.view(yw, p)) for (yw, p) in ((0, 0), (35, 20), (88, 5))]


def label(img, text):
    im = Image.fromarray((img * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, im.width, 13], fill=(20, 20, 28))
    d.text((3, 1), text, fill=(235, 235, 235))
    return np.asarray(im, np.float32) / 255


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ws.ROOT, "docs", "previews", "weapon_sizes"))
    ap.add_argument("--ids")
    args = ap.parse_args(argv)
    pw.W = pw.H = TILE
    flt = set(args.ids.split(",")) if args.ids else None
    rows = ws.table_rows(flt)
    os.makedirs(args.out, exist_ok=True)
    groups = {"dagger": [], "katana": [], "sword_classes": []}
    for r in rows:
        if r["class"] in ("dagger", "katana"):
            groups[r["class"]].append(r)
    for cname, rs in groups.items():
        if cname == "sword_classes" or not rs:
            continue
        out_rows = []
        for r in rs:
            if not r["changed"]:
                continue
            files, _ = ws.all_variants(("dagger", "katana"))
            old = next(p for (b, t, n), p in files.items() if b == r["id"] and t == r["variant"])
            name = os.path.basename(old)[:-4]
            new = os.path.join(ws.ASSETS, ws.rel_dst(r["variant"], name))
            a = tiles(old, r["id"], r["variant"])
            b = tiles(new, r["id"], r["variant"])
            head = "%s [%s]  L3 %.2f>%.2f  w3 %.3f>%.3f  d3 %.3f>%.3f  1P %.2f>%.2f" % (r["id"], r["variant"], r["b"]["L3"], r["a"]["L3"], r["b"]["w3"], r["a"]["w3"], r["b"]["d3"], r["a"]["d3"], r["b"]["L1"], r["a"]["L1"])
            row = np.concatenate(a + [np.full((pw.H, 4, 3), 0.9, np.float32)] + b, 1)
            out_rows.append(label(row, head))
        if out_rows:
            path = os.path.join(args.out, cname + "s_before_after.png")
            Image.fromarray((np.concatenate(out_rows, 0) * 255).astype(np.uint8)).save(path)
            print("wrote", os.path.relpath(path, ws.ROOT), len(out_rows), "rows")


if __name__ == "__main__":
    main()
