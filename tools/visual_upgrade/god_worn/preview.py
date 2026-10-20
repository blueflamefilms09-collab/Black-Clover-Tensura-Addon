"""Software preview of the God worn upgrade, before and after, next to a Steve-like figure (no game, no GPU).

    python3 -B tools/visual_upgrade/god_worn/preview.py --ids flammes_founder_cloak [--out dir] [--level full|lod|held] [--sheet]

Draws like the game: the model's file space (y down, front is -z), every on-body piece at 0.85 (WornPlacer.BODY_SCALE) about its anchor, the skin tinted by the colour slots and lit
on the smoothed normals, EMIT quads dark in the solid pass and bright in the additive pass, LIT quads additive from the glow map in the glow colour, animation groups at rest.
`--sheet` writes, per relic, a contact sheet: BEFORE (old pieces, old atlas) and AFTER (new pieces, own skin) in front / three-quarter / back / close-up views, plus the glow pass alone.
The rasteriser of tools/relics/preview_worn.py is reused (orthographic, z-buffered).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import preview_worn as PW  # noqa: E402
import metrics as M  # noqa: E402
import items as IT  # noqa: E402

ASSETS = M.ASSETS
SKIN_DIR = os.path.join(ASSETS, "textures", "relic_worn", "god")
OLD_ATLAS = os.path.join(ASSETS, "textures", "relic_worn", "atlas.png")
BODY_SCALE = 0.85
HEAD = "head"

_cache = {}


def tex(path):
    if path not in _cache:
        _cache[path] = np.asarray(Image.open(path).convert("RGBA"), np.float32) / 255.0
    return _cache[path]


def parse_model(name):
    path = M.model_path(name)
    anchor, skin, quads, groups = "head", None, [], {}
    for line in open(path, encoding="utf8"):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "anchor":
            anchor = v[1]
        elif v[0] == "skin":
            skin = v[1]
        elif v[0] == "grp":
            groups[int(v[1])] = v[3]
        elif v[0] == "q":
            g, slot, flags = int(v[1]), int(v[2]), int(v[3])
            P, UV, N = [], [], []
            for k in range(4):
                o = 4 + k * 8
                P.append([float(x) for x in v[o:o + 3]])
                UV.append([float(x) for x in v[o + 3:o + 5]])
                N.append([float(x) for x in v[o + 5:o + 8]])
            quads.append((g, slot, flags, np.array(P), np.array(UV), np.array(N)))
    return anchor, skin, quads, groups


def parse_piece(p):
    mirror = p.endswith("~")
    p = p.rstrip("~")
    name, _, anchor = p.partition("@")
    return name, anchor or None, mirror


def render(pieces, palette, yaw, pitch, crop, scale, size, figure=True, glow_only=False, bg=(0.33, 0.36, 0.43)):
    x0, x1, y0, y1 = crop
    W, H = size
    R = PW.rot(yaw, pitch)
    cxm, cym = (x0 + x1) / 2, (y0 + y1) / 2
    img = np.zeros((H, W, 3), np.float32) + np.array(bg, np.float32)
    img *= (0.85 + 0.3 * np.linspace(1, 0, H, dtype=np.float32))[:, None, None]
    zb = np.full((H, W), 1e9, np.float32)

    def proj(Pm):
        Q = Pm @ R.T
        return np.stack([W / 2 + (Q[:, 0] - cxm) * scale, H / 2 + (Q[:, 1] - cym) * scale, Q[:, 2] * scale], axis=1), Q

    def tri_draw(Pm, UV, N, tx, tint, mode, bias=0.0):
        S, Q = proj(Pm)
        Nv = N @ R.T
        for tri in ((0, 1, 2), (0, 2, 3)):
            ii = list(tri)
            PW._raster(img, zb, S[ii], UV[ii], Nv[ii], tx, tint, mode, bias)

    if figure:
        for b in PW.figure_boxes():
            for (Pq, n) in PW.box_quads(b[:6]):
                tri_draw(Pq, np.zeros((4, 2)), np.tile(n, (4, 1)), None, np.array(b[6]), "solid")
        for (ex, col) in ((-2.0, (0.1, 0.35, 0.4)), (1.0, (0.1, 0.35, 0.4))):
            Pq = np.array([[ex, -5, -4.06], [ex + 1.0, -5, -4.06], [ex + 1.0, -4, -4.06], [ex, -4, -4.06]])
            tri_draw(Pq, np.zeros((4, 2)), np.tile([0, 0, -1.0], (4, 1)), None, np.array(col), "solid")
    cols = PW.slot_colors(*palette)
    emit = []
    for (name, anchor, mirror) in pieces:
        a, skin, quads, groups = parse_model(name)
        a = anchor or a
        off = np.array(PW.ANCHORS[a], float)
        sc = 1.0 if a == HEAD else BODY_SCALE
        alb = tex(os.path.join(SKIN_DIR, skin + ".png")) if skin else tex(OLD_ATLAS)
        glo = tex(os.path.join(SKIN_DIR, skin + "_glow.png")) if skin else alb
        for (g, slot, flags, P, UV, N) in quads:
            Pm, Nm = P.copy() * sc, N.copy()
            if mirror:
                Pm[:, 0] *= -1
                Nm[:, 0] *= -1
            Pm = Pm + off
            tint = cols[slot]
            if not glow_only and not flags & 16:
                tri_draw(Pm, UV, Nm, alb, tint * (0.55 if flags & 1 else 1.0), "solid")
            if flags & 25:
                emit.append((Pm, UV, Nm, tint if flags & 17 else cols[2], glo))
    for (Pm, UV, Nm, tint, glo) in emit:
        tri_draw(Pm, UV, Nm, glo, tint * 0.9, "add", bias=0.5)
    return np.clip(img, 0, 1)


ZOOM = {"BACK": (-17, 17, -12, 27), "BELT": (-12, 12, -1, 27), "CHEST": (-13, 13, -10, 22), "FEET": (-10, 10, 8, 34), "NECK": (-9, 9, -9, 13), "HANDS": (-16, 14, -2, 26), "HEAD": (-13, 13, -24, 8)}
VIEWS = {"front": (0, 0), "three": (35, 12), "side": (90, 0), "back": (180, 0), "backthree": (215, 14)}


def tile_row(pieces, palette, slot, views, panel=(280, 320), glow_only=False, zoom=None):
    crop = zoom or ZOOM[slot]
    w, h = crop[1] - crop[0], crop[3] - crop[2]
    scale = min((panel[0] - 6) / w, (panel[1] - 6) / h)
    tiles = []
    for v in views:
        yaw, pitch = VIEWS[v]
        ss = 2
        im = render(pieces, palette, yaw, pitch, crop, scale * ss, (panel[0] * ss, panel[1] * ss), glow_only=glow_only)
        tiles.append(im.reshape(panel[1], ss, panel[0], ss, 3).mean(axis=(1, 3)))
    return np.concatenate(tiles, axis=1)


def old_pieces(rid):
    relics, looks = M.read_catalog()
    slot, look = IT.BY_ID[rid][2], IT.BY_ID[rid][3]
    return relics.get(rid) or looks.get((slot, look))


def new_pieces(rid, kind="up"):
    path = os.path.join(M.UPG, "catalog.txt")
    for line in open(path):
        v = line.split()
        if len(v) > 2 and v[0] == kind and v[1] == rid:
            return v[2:]
    return None


VIEWS_OF = {"BACK": ("back", "backthree", "side"), "BELT": ("front", "three", "back"), "CHEST": ("front", "three", "back"), "FEET": ("front", "three", "side"),
            "NECK": ("front", "three", "side"), "HANDS": ("front", "three", "side"), "HEAD": ("front", "three", "side")}


def sheet(rid, out_dir, level="up", views=None):
    rid_, name, slot, look, pal, _lore = IT.BY_ID[rid]
    views = views or VIEWS_OF[slot]
    before = [parse_piece(p) for p in old_pieces(rid)]
    after_names = new_pieces(rid, level)
    rows = [("BEFORE (old models, shared atlas)", tile_row(before, pal, slot, views))]
    if after_names:
        after = [parse_piece(p) for p in after_names]
        rows.append(("AFTER (%s)" % level, tile_row(after, pal, slot, views)))
        rows.append(("AFTER, glow pass only", tile_row(after, pal, slot, views[:2], glow_only=True, panel=(280, 320))))
    out = Image.new("RGB", (max(r[1].shape[1] for r in rows), sum(r[1].shape[0] + 16 for r in rows)), (20, 20, 24))
    d = ImageDraw.Draw(out)
    y = 0
    for label, arr in rows:
        d.text((6, y + 2), "%s: %s" % (name, label), fill=(255, 255, 255))
        out.paste(Image.fromarray((arr * 255).astype(np.uint8)), (0, y + 16))
        y += arr.shape[0] + 16
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "%s_%s.png" % (rid, level))
    out.save(path)
    return path


CLOSE = {"BACK": (-11, 11, -6, 16), "BELT": (-9, 9, 0, 20), "CHEST": (-10, 10, -6, 14), "FEET": (-7, 7, 10, 26), "NECK": (-6, 6, -4, 12), "HANDS": (-13, 1, 2, 18), "HEAD": (-9, 9, -20, 4)}


def bbox(pieces):
    """The box {x0, x1, y0, y1} of the pieces in layer space (with the body scale and anchor offset), padded."""
    lo = np.array([1e9, 1e9])
    hi = np.array([-1e9, -1e9])
    for (name, anchor, mirror) in pieces:
        a, skin, quads, groups = parse_model(name)
        a = anchor or a
        off = np.array(PW.ANCHORS[a], float)
        sc = 1.0 if a == HEAD else BODY_SCALE
        for (g, slot, flags, P, UV, N) in quads:
            xy = P[:, :2] * sc
            if mirror:
                xy = xy * np.array([-1, 1])
            xy = xy + off[:2]
            lo = np.minimum(lo, xy.min(axis=0))
            hi = np.maximum(hi, xy.max(axis=0))
    pad = 1.5
    w, h = hi[0] - lo[0], hi[1] - lo[1]
    side = max(w, h) / 2 + pad
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    return (cx - side, cx + side, cy - side, cy + side)


def detail_sheet(rid, out_dir):
    """Second sheet: a close-up of the detail, the LOD form beyond 12 blocks, the held form (the object alone, as drawn in the hand, on the ground and in a frame) and the side view."""
    rid_, name, slot, look, pal, _lore = IT.BY_ID[rid]
    up = [parse_piece(p) for p in new_pieces(rid, "up")]
    lod = [parse_piece(p) for p in new_pieces(rid, "lod")]
    held = [parse_piece(p) for p in new_pieces(rid, "held")]
    panel = (280, 320)
    tiles, labels = [], []
    close = CLOSE[slot]
    w, h = close[1] - close[0], close[3] - close[2]
    sc = min((panel[0] - 6) / w, (panel[1] - 6) / h)
    ss = 2
    def shot(pieces, yaw, pitch, crop, figure=True, glow_only=False):
        cw, ch = crop[1] - crop[0], crop[3] - crop[2]
        s2 = min((panel[0] - 6) / cw, (panel[1] - 6) / ch)
        im = render(pieces, pal, yaw, pitch, crop, s2 * ss, (panel[0] * ss, panel[1] * ss), figure=figure, glow_only=glow_only)
        return im.reshape(panel[1], ss, panel[0], ss, 3).mean(axis=(1, 3))
    tiles.append(shot(up, *VIEWS["three" if slot not in ("BACK",) else "backthree"], close)); labels.append("close-up of the detail")
    tiles.append(shot(lod, *VIEWS["three" if slot not in ("BACK",) else "backthree"], ZOOM[slot])); labels.append("LOD form (beyond 12 blocks)")
    hb = bbox(held)
    tiles.append(shot(held, 35 if slot not in ("BACK",) else 215, 12, hb, figure=False)); labels.append("held form (hand, ground, frame)")
    tiles.append(shot(up, 90, 0, ZOOM[slot])); labels.append("side view")
    row = np.concatenate(tiles, axis=1)
    out = Image.new("RGB", (row.shape[1], row.shape[0] + 16), (20, 20, 24))
    d = ImageDraw.Draw(out)
    for i, lb in enumerate(labels):
        d.text((6 + i * panel[0], 2), "%s: %s" % (name, lb), fill=(255, 255, 255))
    out.paste(Image.fromarray((row * 255).astype(np.uint8)), (0, 16))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "%s_detail.png" % rid)
    out.save(path)
    return path


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--ids", default="")
    ap.add_argument("--out", default=os.path.join(M.ROOT, "docs", "previews", "visual_upgrade", "god_worn"))
    ap.add_argument("--level", default="up")
    a = ap.parse_args(argv)
    ids = a.ids.split(",") if a.ids else IT.IDS
    for rid in ids:
        print(sheet(rid, a.out, a.level))
        print(detail_sheet(rid, a.out))


if __name__ == "__main__":
    main(sys.argv[1:])
