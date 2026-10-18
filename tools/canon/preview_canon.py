#!/usr/bin/env python3
"""Headless preview of the Canon Grimoire 3D model from every display angle (no game needed).

usage: python3 tools/canon/preview_canon.py [--id ban] [--out build/canon_preview] [--views fp_right,tp_right_front,...] [--all]

It extends tools/lines/preview_book.py (the renderer of an item model json that uses the game's own pose maths, see docs/skilltree/lines_book_art.md section 3) with what the Canon
Grimoire needs: elements with `neoforge_data` light (the emblem and the stone) are drawn full-bright, like the game draws them; the GUI cell shows the old flat icon; a contact sheet of
one view of every book (--all). The pose maths is the game's (ItemTransform.apply, ItemInHandLayer, ItemInHandRenderer), but THIS IS NOT THE GAME: it is a software z-buffer with
nearest-texel sampling, a stand-in blocky player and no hand model in the first person.

views: fp_right fp_left tp_right_front tp_left_front tp_right_side tp_right_back tp_left_side ground_20 ground_130 fixed head_front head_side gui  and  m_front m_back m_east m_west m_top m_34 m_34b
(the bare model without a display transform)
"""
import argparse
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "lines"))
sys.path.insert(0, HERE)
import preview_book as pb  # noqa: E402

ASSETS = pb.ASSETS


def quads6(model):
    """preview_book.quads plus a seventh item: whether the element is full-bright (neoforge_data light 15)."""
    out = []
    tex = model["textures"]

    def resolve(ref):
        while ref.startswith("#"):
            ref = tex[ref[1:]]
        return ref
    for e in model["elements"]:
        frm, to = e["from"], e["to"]
        glow = e.get("neoforge_data", {}).get("block_light", 0) >= 15
        for fname, f in e["faces"].items():
            uvr = f.get("uv") or pb.default_uv(fname, frm, to)
            u0, v0, u1, v1 = uvr
            P = np.array([[(to if sel[0] else frm)[0], (to if sel[1] else frm)[1], (to if sel[2] else frm)[2]] for sel in pb.FACE_CORNERS[fname]], float)
            UV = np.array([[u0, v0], [u1, v0], [u1, v1], [u0, v1]], float)
            out.append((P, UV, pb.texture(resolve(f["texture"])), f.get("tintindex", -1), np.array(pb.NORMALS[fname], float), glow))
    return out


def render6(cv, qs, model_m, view, proj_k, tint_rgb, fov_scale=None, ortho=None):
    for (P, UV, tex, tint, n, glow) in qs:
        pts = np.c_[P / 16.0, np.ones(4)] @ model_m.T
        nw = (model_m[:3, :3] @ n)
        nw /= np.linalg.norm(nw) + 1e-9
        cam = pts @ view.T
        if np.any(-cam[:, 2] <= 0.02):
            continue
        scr = np.stack([cv.w / 2 + cam[:, 0] / -cam[:, 2] * proj_k, cv.h / 2 - cam[:, 1] / -cam[:, 2] * proj_k, 1.0 / -cam[:, 2]], axis=1)
        area = (scr[1, 0] - scr[0, 0]) * (scr[2, 1] - scr[0, 1]) - (scr[2, 0] - scr[0, 0]) * (scr[1, 1] - scr[0, 1])
        if area <= 0:
            continue
        mixed = max(0.0, nw @ pb.LIGHT0) + max(0.0, nw @ pb.LIGHT1)
        shade = 1.0 if glow else min(1.0, mixed * 0.6 + 0.4)
        for tri in ((0, 1, 2), (0, 2, 3)):
            pb.draw_tri(cv, scr[list(tri)], UV[list(tri)], tex, shade, (1, 1, 1))


pb.render_quads = render6


def load_body(ident):
    path = os.path.join(ASSETS, "models", "item", "charter_grimoire_%s_3d.json" % ident)
    data = json.load(open(path, encoding="utf-8"))
    return {"textures": data["textures"], "elements": data["elements"], "display": data["display"]}


def gui_icon(ident):
    im = Image.open(os.path.join(ASSETS, "textures", "item", "charter_grimoire_%s_icon.png" % ident)).convert("RGBA").resize((pb.W, pb.H), Image.NEAREST)
    bg = Image.new("RGBA", (pb.W, pb.H), tuple(int(c * 255) for c in pb.BG) + (255,))
    bg.alpha_composite(im)
    return bg.convert("RGB")


DEFAULT_VIEWS = ["m_front", "m_34", "m_back", "m_top", "fp_right", "fp_left", "tp_right_front", "tp_left_front", "tp_right_side", "tp_right_back", "tp_left_side", "ground_20",
                 "ground_130", "fixed", "head_front", "gui"]


FPW, FPH = 604, 340            # the first person is drawn on a 16:9 screen (the other views are square)


def fp_scene(model, qs, left):
    """The first person as the game draws it: camera at the origin looking down -z, 70 degrees vertical field of view, the item at translate(+-0.56, -0.52, -0.72) plus its display
    transform. No arm is drawn: the game shows none for an ordinary held item. A crosshair and the hotbar (182 x 22 gui px at gui scale 3 on a 1080p screen) are drawn for scale."""
    cv = pb.Canvas(FPW, FPH, pb.BG)
    k = (FPH / 2) / math.tan(math.radians(35))
    d = model["display"]["firstperson_lefthand" if left else "firstperson_righthand"]
    m = pb.T((-1 if left else 1) * 0.56, -0.52, -0.72) @ pb.display_matrix(d, left)
    pb.render_quads(cv, qs, m, pb.look_at((0, 0, 0), (0, 0, -1)), k, (1, 1, 1))
    img = Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    dr = ImageDraw.Draw(img)
    hw, hh = int(FPW * 546 / 1920), int(FPH * 66 / 1080)
    dr.rectangle((FPW // 2 - hw // 2, FPH - hh - 2, FPW // 2 + hw // 2, FPH - 2), outline=(20, 20, 20), fill=(48, 48, 56))
    dr.line((FPW // 2 - 6, FPH // 2, FPW // 2 + 6, FPH // 2), fill=(255, 255, 255))
    dr.line((FPW // 2, FPH // 2 - 6, FPW // 2, FPH // 2 + 6), fill=(255, 255, 255))
    return img


def tp_close(model, qs, v):
    """tpz_<right|left>_<yaw degrees>: the held book from 0.95 block away, the camera looking at the hand (yaw 0 = from the front of the player, 90 = from the player's right)."""
    _, side, yaw = v.split("_")
    left = side == "left"
    cv = pb.Canvas(pb.W, pb.H, pb.BG)
    k = (pb.H / 2) / math.tan(math.radians(22))
    D = pb.display_matrix(model["display"]["thirdperson_lefthand" if left else "thirdperson_righthand"], left)
    item_m = pb.MODEL_TO_WORLD @ pb.hand_pose(left) @ D
    hand = (pb.MODEL_TO_WORLD @ pb.hand_pose(left))[:3, 3]
    yawr = math.radians(float(yaw) * (-1 if left else 1))
    eye = hand + np.array([math.sin(yawr) * 0.95, 0.22, -math.cos(yawr) * 0.95])
    view = pb.look_at(eye, hand + np.array([0, 0.02, 0]))
    pb.draw_player(cv, view, k, -18.0 if not left else 0.0, -18.0 if left else 0.0)
    pb.render_quads(cv, qs, item_m, view, k, (1, 1, 1))
    return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))


def render_view(model, qs, ident, v):
    if v == "gui":
        return gui_icon(ident)
    if v.startswith("tpz_"):
        return tp_close(model, qs, v)
    if v.startswith("fp"):
        return fp_scene(model, qs, v.endswith("left"))
    return pb.view_scene(model, qs, v, (1, 1, 1), left=False)


def sheet_of(tiles, width=1360):
    """Tiles of different widths in rows no wider than `width`."""
    rows, cur, w = [], [], 0
    for t in tiles:
        if w + t.width > width and cur:
            rows.append(cur)
            cur, w = [], 0
        cur.append(t)
        w += t.width
    if cur:
        rows.append(cur)
    sheet = Image.new("RGB", (width, sum(max(t.height for t in r) for r in rows)))
    y = 0
    for r in rows:
        x = 0
        for t in r:
            sheet.paste(t, (x, y))
            x += t.width
        y += max(t.height for t in r)
    return sheet


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--id", default="ban")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "canon_preview"))
    ap.add_argument("--views", default="")
    ap.add_argument("--all", action="store_true", help="a contact sheet with one view of every book")
    ap.add_argument("--view", default="m_34", help="the view of the --all sheet")
    ap.add_argument("--set", action="append", default=[], help="override a display entry for the preview only: context=rx,ry,rz,tx,ty,tz,s (e.g. thirdperson_righthand=50,135,10,-1.6,2.4,0.4,0.5)")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    if a.all:
        ids = sorted(n[len("charter_grimoire_"):-len("_3d.json")] for n in os.listdir(os.path.join(ASSETS, "models", "item")) if n.startswith("charter_grimoire_") and n.endswith("_3d.json"))
        cols = 6
        sheet = Image.new("RGB", (cols * pb.W, ((len(ids) + cols - 1) // cols) * pb.H))
        for i, ident in enumerate(ids):
            m = load_body(ident)
            img = render_view(m, quads6(m), ident, a.view)
            ImageDraw.Draw(img).text((6, 4), ident, fill=(255, 255, 255))
            sheet.paste(img, ((i % cols) * pb.W, (i // cols) * pb.H))
        p = os.path.join(a.out, "all_%s.png" % a.view)
        sheet.save(p)
        print("wrote", p)
        return
    m = load_body(a.id)
    for ov in a.set:
        ctx, vals = ov.split("=")
        v = [float(x) for x in vals.split(",")]
        entry = {"rotation": v[0:3], "translation": v[3:6], "scale": [v[6]] * 3}
        m["display"][ctx] = entry
        if ctx.endswith("righthand"):
            m["display"][ctx.replace("right", "left")] = entry
    qs = quads6(m)
    views = a.views.split(",") if a.views else DEFAULT_VIEWS
    tiles = []
    for v in views:
        img = render_view(m, qs, a.id, v)
        ImageDraw.Draw(img).text((6, 4), v, fill=(255, 255, 255))
        img.save(os.path.join(a.out, "%s_%s.png" % (a.id, v)))
        tiles.append(img)
    p = os.path.join(a.out, "sheet_%s.png" % a.id)
    sheet_of(tiles).save(p)
    print("wrote", p)


if __name__ == "__main__":
    main()
