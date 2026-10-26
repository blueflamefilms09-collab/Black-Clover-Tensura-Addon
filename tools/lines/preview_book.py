#!/usr/bin/env python3
"""Headless preview of the 3D Skill Book model from every display angle (no game needed).

usage: python3 tools/lines/preview_book.py [--franchise tensura] [--color E8541C] [--out build/book_preview] [--model skill_book_3d.json]

Reads the model json the game reads (parents, textures, elements, faces, tint indexes, the display block), and places it with the SAME maths as the game:
    point = Pose * T(tx,ty,tz) * Rx*Ry*Rz(rotation) * S(scale) * T(-0.5)  * (model point / 16)
where a LEFT-hand context first negates tx, ry and rz (ItemTransform.apply), and Pose is what the game puts around the item:
  firstperson  translate(+-0.56, -0.52, -0.72) in the camera's frame
  thirdperson  arm pivot, arm xRot -18 deg (ArmPose.ITEM), Rx(-90), Ry(180), translate(+-1/16, 0.125, -0.625)      (ItemInHandLayer)
  ground       translate(0, 0.25*scale.y, 0) and the entity's spin about Y                                          (ItemEntityRenderer)
  fixed        scale(0.5) in the frame, the viewer on the -z side                                                    (ItemFrameRenderer)
  head         translate(0,-0.25,0) Ry(180) scale(0.625,-0.625,-0.625) on the head                                  (CustomHeadLayer)
It rasterises with a z-buffer (nearest texel, cutout alpha, the game's two diffuse lights, back faces culled) and writes one PNG per view plus a contact sheet.
The GUI cell shows the OLD sprite, tinted, because that is what the inventory shows (neoforge:separate_transforms).
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
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
sys.path.insert(0, HERE)

# ---------------------------------------------------------------------------------------------------------- maths
def T(x, y, z):
    m = np.eye(4); m[:3, 3] = (x, y, z); return m


def S(x, y, z):
    return np.diag([x, y, z, 1.0])


def Rx(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1.0]])


def Ry(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1.0]])


def Rz(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1.0]])


def display_matrix(d, left):
    """ItemTransform.apply followed by the model's own translate(-0.5): the exact order of the game."""
    rx, ry, rz = d.get("rotation", [0, 0, 0])
    tx, ty, tz = [v / 16.0 for v in d.get("translation", [0, 0, 0])]
    sx, sy, sz = d.get("scale", [1, 1, 1])
    if left:
        ry, rz, tx = -ry, -rz, -tx
    return T(tx, ty, tz) @ Rx(rx) @ Ry(ry) @ Rz(rz) @ S(sx, sy, sz) @ T(-0.5, -0.5, -0.5)


def look_at(eye, target, up=(0, 1, 0)):
    eye, target, up = map(lambda v: np.array(v, float), (eye, target, up))
    f = target - eye; f /= np.linalg.norm(f)
    r = np.cross(f, up); r /= np.linalg.norm(r)
    u = np.cross(r, f)
    return np.array([[r[0], r[1], r[2], -r @ eye], [u[0], u[1], u[2], -u @ eye], [-f[0], -f[1], -f[2], f @ eye], [0, 0, 0, 1.0]])


# ---------------------------------------------------------------------------------------------------------- model
def load_model(name, seen=None):
    """Resolve parents like the game: textures merge (child wins), elements/display come from the nearest model that has them."""
    path = os.path.join(ASSETS, "models", "item", name if name.endswith(".json") else name + ".json")
    data = json.load(open(path, encoding="utf-8"))
    parent = data.get("parent")
    out = {"textures": {}, "elements": None, "display": {}}
    if parent and parent.startswith("nusmp:item/"):
        out = load_model(parent.split("/", 1)[1])
    out["textures"] = {**out["textures"], **data.get("textures", {})}
    if "elements" in data:
        out["elements"] = data["elements"]
    out["display"] = {**out["display"], **data.get("display", {})}
    return out


_TEX = {}


ANIM_FRAME = 0                      # which frame of an animated texture (.mcmeta next to it) the preview draws


def texture(ref):
    key = ref.split(":", 1)[1] if ":" in ref else ref
    ck = (key, ANIM_FRAME)
    if ck not in _TEX:
        path = os.path.join(ASSETS, "textures", key + ".png")
        arr = np.asarray(Image.open(path).convert("RGBA"), np.float32) / 255.0
        if os.path.exists(path + ".mcmeta") and arr.shape[0] > arr.shape[1]:          # an animation strip: square frames below each other
            fh = arr.shape[1]
            n = arr.shape[0] // fh
            f = ANIM_FRAME % n
            arr = arr[f * fh:(f + 1) * fh]
        _TEX[ck] = arr
    return _TEX[ck]


FACE_CORNERS = {        # TL, TR, BR, BL as (x, y, z) selectors: 0 = from, 1 = to  (see book_model.py)
    "south": [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)],
    "north": [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)],
    "east": [(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)],
    "west": [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)],
    "up": [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)],
    "down": [(0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)],
}
NORMALS = {"south": (0, 0, 1), "north": (0, 0, -1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def default_uv(face, frm, to):
    f, t = frm, to
    return {"north": [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]], "south": [f[0], 16 - t[1], t[0], 16 - f[1]], "east": [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]],
            "west": [f[2], 16 - t[1], t[2], 16 - f[1]], "up": [f[0], f[2], t[0], t[2]], "down": [f[0], 16 - t[2], t[0], 16 - f[2]]}[face]


def rot_matrix(axis, angle_deg):
    a = math.radians(angle_deg)
    c, s_ = math.cos(a), math.sin(a)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s_], [0, s_, c]])
    if axis == "y":
        return np.array([[c, 0, s_], [0, 1, 0], [-s_, 0, c]])
    return np.array([[c, -s_, 0], [s_, c, 0], [0, 0, 1]])


def quads(model):
    """[(world corners 4x3 in model units, uv corners 4x2, texture array, tintindex, normal, glow)]; honours element rotation and neoforge_data full-bright light."""
    out = []
    tex = model["textures"]

    def resolve(ref):
        while ref.startswith("#"):
            ref = tex[ref[1:]]
        return ref
    for e in model["elements"]:
        frm, to = e["from"], e["to"]
        glow = (e.get("neoforge_data", {}).get("block_light", 0) or 0) >= 12
        rot = e.get("rotation")
        R = rot_matrix(rot["axis"], rot["angle"]) if rot else None
        origin = np.array(rot["origin"], float) if rot else None
        for fname, f in e["faces"].items():
            uvr = f.get("uv") or default_uv(fname, frm, to)
            u0, v0, u1, v1 = uvr
            P = np.array([[(to if sel[0] else frm)[0], (to if sel[1] else frm)[1], (to if sel[2] else frm)[2]] for sel in FACE_CORNERS[fname]], float)
            n = np.array(NORMALS[fname], float)
            if R is not None:
                P = (P - origin) @ R.T + origin
                n = R @ n
            UV = np.array([[u0, v0], [u1, v0], [u1, v1], [u0, v1]], float)
            out.append((P, UV, texture(resolve(f["texture"])), f.get("tintindex", -1), n, glow))
    return out


# ---------------------------------------------------------------------------------------------------------- rasteriser
class Canvas:
    def __init__(self, w, h, bg):
        self.w, self.h = w, h
        self.img = np.zeros((h, w, 3), np.float32) + np.array(bg, np.float32)
        self.z = np.full((h, w), np.inf, np.float32)


def draw_tri(cv, P, UV, tex, shade, tint, solid=None):
    """P: 3x(sx, sy, w) screen coords with 1/w kept; perspective-correct uv; nearest texel; alpha test 0.1."""
    x0 = int(max(0, math.floor(P[:, 0].min()))); x1 = int(min(cv.w - 1, math.ceil(P[:, 0].max())))
    y0 = int(max(0, math.floor(P[:, 1].min()))); y1 = int(min(cv.h - 1, math.ceil(P[:, 1].max())))
    if x0 > x1 or y0 > y1:
        return
    yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1].astype(np.float32) + 0.5
    (ax, ay, aw), (bx, by, bw), (cx, cy, cw) = P
    den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
    if abs(den) < 1e-9:
        return
    l0 = ((by - cy) * (xx - cx) + (cx - bx) * (yy - cy)) / den
    l1 = ((cy - ay) * (xx - cx) + (ax - cx) * (yy - cy)) / den
    l2 = 1 - l0 - l1
    inside = (l0 >= -1e-4) & (l1 >= -1e-4) & (l2 >= -1e-4)
    if not inside.any():
        return
    iw = l0 * aw + l1 * bw + l2 * cw                       # 1/w, linear in screen space
    depth = 1.0 / np.maximum(iw, 1e-9)
    region = cv.z[y0:y1 + 1, x0:x1 + 1]
    ok = inside & (depth < region)
    if not ok.any():
        return
    if solid is not None:
        col = np.zeros(depth.shape + (3,), np.float32) + np.array(solid, np.float32)
        alpha = np.ones(depth.shape, bool)
    else:
        u = (l0 * UV[0, 0] * aw + l1 * UV[1, 0] * bw + l2 * UV[2, 0] * cw) * depth
        v = (l0 * UV[0, 1] * aw + l1 * UV[1, 1] * bw + l2 * UV[2, 1] * cw) * depth
        th, tw = tex.shape[:2]
        px = np.clip((u / 16.0 * tw).astype(int), 0, tw - 1)
        py = np.clip((v / 16.0 * th).astype(int), 0, th - 1)
        texel = tex[py, px]
        col = texel[..., :3] * np.array(tint, np.float32)
        alpha = texel[..., 3]
    if solid is not None:
        ok &= alpha
        sub = cv.img[y0:y1 + 1, x0:x1 + 1]
        sub[ok] = (col * shade)[ok]
        region[ok] = depth[ok]
        return
    ok &= alpha > 0.03                                      # translucent texels blend over what is behind (the game's item sheet is translucent); only solid ones write depth
    sub = cv.img[y0:y1 + 1, x0:x1 + 1]
    a3 = alpha[..., None]
    blended = col * shade * a3 + sub * (1 - a3)
    sub[ok] = blended[ok]
    solid_ok = ok & (alpha > 0.5)
    region[solid_ok] = depth[solid_ok]


LIGHT0 = np.array([0.2, 1.0, -0.7]); LIGHT0 /= np.linalg.norm(LIGHT0)
LIGHT1 = np.array([-0.2, 1.0, 0.7]); LIGHT1 /= np.linalg.norm(LIGHT1)


def render_quads(cv, qs, model_m, view, proj_k, tint_rgb, fov_scale=None, ortho=None):
    """model_m: 4x4 from the model's 0..1 block space to world. view: world -> camera. Perspective with focal length proj_k (pixels)."""
    for (P, UV, tex, tint, n, glow) in qs:
        pts = np.c_[P / 16.0, np.ones(4)] @ model_m.T            # world
        nw = (model_m[:3, :3] @ n)
        nw /= np.linalg.norm(nw) + 1e-9
        cam = pts @ view.T
        if np.any(-cam[:, 2] <= 0.02):
            continue
        scr = np.stack([cv.w / 2 + cam[:, 0] / -cam[:, 2] * proj_k, cv.h / 2 - cam[:, 1] / -cam[:, 2] * proj_k, 1.0 / -cam[:, 2]], axis=1)
        area = (scr[1, 0] - scr[0, 0]) * (scr[2, 1] - scr[0, 1]) - (scr[2, 0] - scr[0, 0]) * (scr[1, 1] - scr[0, 1])
        if area <= 0:
            continue                                               # back face (screen y is down, so a face seen from outside, TL-TR-BR, has a positive area)
        mixed = max(0.0, nw @ LIGHT0) + max(0.0, nw @ LIGHT1)
        shade = 1.0 if glow else min(1.0, mixed * 0.6 + 0.4)
        t = tint_rgb if tint == 1 else (1, 1, 1)
        for tri in ((0, 1, 2), (0, 2, 3)):
            draw_tri(cv, scr[list(tri)], UV[list(tri)], tex, shade, t)


BOX_FACES = [((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)), ((1, 0, 1), (0, 0, 1), (0, 1, 1), (1, 1, 1)), ((0, 0, 1), (0, 0, 0), (0, 1, 0), (0, 1, 1)),
             ((1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)), ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)), ((0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0))]


def render_box(cv, lo, hi, m, view, k, color):
    """A solid box (model-space units/16 through m) for the stand-in player."""
    lo, hi = np.array(lo, float), np.array(hi, float)
    for face in BOX_FACES:
        P = np.array([[(hi if s[0] else lo)[0], (hi if s[1] else lo)[1], (hi if s[2] else lo)[2]] for s in face], float)
        pts = np.c_[P / 16.0, np.ones(4)] @ m.T
        e1, e2 = pts[1, :3] - pts[0, :3], pts[2, :3] - pts[0, :3]
        nw = np.cross(e1, e2); nw /= np.linalg.norm(nw) + 1e-9
        cam = pts @ view.T
        if np.any(-cam[:, 2] <= 0.02):
            continue
        scr = np.stack([cv.w / 2 + cam[:, 0] / -cam[:, 2] * k, cv.h / 2 - cam[:, 1] / -cam[:, 2] * k, 1.0 / -cam[:, 2]], axis=1)
        area = (scr[1, 0] - scr[0, 0]) * (scr[2, 1] - scr[0, 1]) - (scr[2, 0] - scr[0, 0]) * (scr[1, 1] - scr[0, 1])
        if area <= 0:
            continue
        shade = min(1.0, (max(0, nw @ LIGHT0) + max(0, nw @ LIGHT1)) * 0.6 + 0.4)
        for tri in ((0, 1, 2), (0, 2, 3)):
            draw_tri(cv, scr[list(tri)], None, None, shade, None, solid=np.array(color, np.float32) / 255.0)


# ---------------------------------------------------------------------------------------------------------- the stand-in player (model space: x to the player's LEFT, y DOWN, z backwards; blocks)
MODEL_TO_WORLD = np.array([[-1, 0, 0, 0], [0, -1, 0, 1.5], [0, 0, 1, 0], [0, 0, 0, 1.0]])
SKIN, SHIRT, PANTS = (205, 160, 128), (62, 112, 178), (58, 58, 130)


def player_parts(right_xrot, left_xrot):
    """[(lo, hi, 4x4 in blocks of model space, colour)] for head, body, arms and legs; the held arm gets its xRot."""
    parts = [((-4, -8, -4), (4, 0, 4), np.eye(4), SKIN), ((-4, 0, -2), (4, 12, 2), np.eye(4), SHIRT)]
    for side, px, xr in (("r", -5, right_xrot), ("l", 5, left_xrot)):
        parts.append(((-3, -2, -2) if side == "r" else (-1, -2, -2), (1, 10, 2) if side == "r" else (3, 10, 2), T(px / 16, 2 / 16, 0) @ Rx(xr), SKIN))
    parts.append(((-2, 0, -2), (2, 12, 2), T(-1.9 / 16, 12 / 16, 0), PANTS))
    parts.append(((-2, 0, -2), (2, 12, 2), T(1.9 / 16, 12 / 16, 0), PANTS))
    return parts


def draw_player(cv, view, k, right_xrot, left_xrot):
    for lo, hi, m, col in player_parts(right_xrot, left_xrot):
        render_box(cv, lo, hi, MODEL_TO_WORLD @ m, view, k, col)


def hand_pose(left, xrot=-18.0):
    """Model-space matrix (blocks) from the item's display frame to the player's model space (ItemInHandLayer)."""
    pivot = T((5 if left else -5) / 16.0, 2 / 16.0, 0)
    return pivot @ Rx(xrot) @ Rx(-90) @ Ry(180) @ T((-1 if left else 1) / 16.0, 0.125, -0.625)


# ---------------------------------------------------------------------------------------------------------- scenes
W = H = 340
BG = (0.36, 0.40, 0.46)


def view_scene(model, qs, name, tint, left=False):
    d = model["display"]
    cv = Canvas(W, H, BG)
    k = (H / 2) / math.tan(math.radians(35))
    if name.startswith("fp"):
        disp = d["firstperson_lefthand" if left else "firstperson_righthand"]
        m = T((-1 if left else 1) * 0.56, -0.52, -0.72) @ display_matrix(disp, left)
        view = look_at((0, 0, 0), (0, 0, -1))
        k = (H / 2) / math.tan(math.radians(35))
        # a reticle and the floor line, so the pose reads against something
        render_quads(cv, qs, m, view, k, tint)
        im = (np.clip(cv.img, 0, 1) * 255).astype(np.uint8)
        pil = Image.fromarray(im)
        dr = ImageDraw.Draw(pil)
        dr.line((W // 2 - 6, H // 2, W // 2 + 6, H // 2), fill=(255, 255, 255)); dr.line((W // 2, H // 2 - 6, W // 2, H // 2 + 6), fill=(255, 255, 255))
        return pil
    if name.startswith("tp"):
        parts = name.split("_")                       # tp_<hand>_<angle>
        left = parts[1] == "left"
        yaw = {"front": 25, "side": 100, "back": 200}[parts[2]] * (-1 if left and parts[2] == "front" else 1)
        disp = d["thirdperson_lefthand" if left else "thirdperson_righthand"]
        D = display_matrix(disp, left)
        item_m = MODEL_TO_WORLD @ T(0, 0, 0) @ hand_pose(left) @ D           # model blocks -> world
        eye = np.array([math.sin(math.radians(yaw)) * 1.9, 1.15, -math.cos(math.radians(yaw)) * 1.9])
        view = look_at(eye, (0, 1.05, 0))
        draw_player(cv, view, k, -18.0 if not left else 0.0, -18.0 if left else 0.0)
        render_quads(cv, qs, item_m, view, k, tint)
        return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    if name.startswith("head"):
        side = name.split("_")[1]
        disp = d["head"]
        pose = T(0, -0.25, 0) @ Ry(180) @ S(0.625, -0.625, -0.625)
        item_m = MODEL_TO_WORLD @ pose @ display_matrix(disp, False)
        yaw = 15 if side == "front" else 100
        eye = np.array([math.sin(math.radians(yaw)) * 2.2, 1.8, -math.cos(math.radians(yaw)) * 2.2])
        view = look_at(eye, (0, 1.7, 0))
        draw_player(cv, view, k, 0.0, 0.0)
        render_quads(cv, qs, item_m, view, k, tint)
        return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    if name.startswith("ground"):
        parts = name.split("_")
        spin = float(parts[1])
        mode = parts[2] if len(parts) > 2 else "mid"
        disp = d["ground"]
        sy = disp.get("scale", [1, 1, 1])[1]
        m = T(0, 0.25 * sy + 0.06, 0) @ Ry(spin) @ display_matrix(disp, False)
        eye = {"mid": (0.0, 0.62, 0.7), "top": (0.0, 1.0, 0.02), "low": (0.0, 0.14, 0.8), "far": (0.0, 1.0, 1.25)}[mode]
        view = look_at(eye, (0, 0.08, 0))
        k = (H / 2) / math.tan(math.radians(24))
        # the ground: a checker of two greens
        for gx in range(-4, 4):
            for gz in range(-4, 4):
                lo = (gx * 16, -1, gz * 16); hi = (gx * 16 + 16, 0, gz * 16 + 16)
                render_box(cv, lo, hi, np.eye(4), view, k, (84, 120, 70) if (gx + gz) % 2 == 0 else (92, 132, 76))
        render_quads(cv, qs, m, view, k, tint)
        return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    if name.startswith("z_"):                      # the bare model, zoomed in (the book fills the tile): z_front, z_back, z_34, z_34b, z_east, z_west, z_top, z_low
        eyes = {"front": (0, 0, 1.0), "back": (0, 0, -1.0), "east": (1.0, 0, 0), "west": (-1.0, 0, 0), "top": (0, 1.0, 0.001), "34": (0.5, 0.3, 0.72), "34b": (-0.5, 0.3, -0.72), "low": (0.5, -0.3, 0.72), "34w": (-0.55, 0.3, 0.7)}
        m = T(-0.5, -0.5, -0.5)
        view = look_at(eyes[name[2:]], (0, 0, 0))
        k = (H / 2) / math.tan(math.radians(28))
        render_quads(cv, qs, m, view, k, tint)
        return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    if name.startswith("m_"):                      # the bare model, no display transform: m_front, m_back, m_east, m_west, m_top, m_34
        eyes = {"front": (0, 0, 1.6), "back": (0, 0, -1.6), "east": (1.6, 0, 0), "west": (-1.6, 0, 0), "top": (0, 1.6, 0.001), "34": (0.8, 0.5, 1.2), "34b": (-0.8, 0.5, -1.2)}
        m = T(-0.5, -0.5, -0.5)
        view = look_at(eyes[name[2:]], (0, 0, 0))
        k = (H / 2) / math.tan(math.radians(28))
        render_quads(cv, qs, m, view, k, tint)
        return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    if name == "fixed":
        disp = d["fixed"]
        m = S(0.5, 0.5, 0.5) @ display_matrix(disp, False)
        view = look_at((0, 0, -2.2), (0, 0, 0))
        k = (H / 2) / math.tan(math.radians(22))
        render_box(cv, (-12, -12, 7.2), (12, 12, 9.6), np.eye(4), view, k, (118, 82, 48))         # the frame's board behind (z > 0 is away from the viewer)
        render_box(cv, (-10, -10, 6.0), (10, 10, 7.2), np.eye(4), view, k, (180, 150, 110))
        render_quads(cv, qs, m, view, k, tint)
        return Image.fromarray((np.clip(cv.img, 0, 1) * 255).astype(np.uint8))
    raise ValueError(name)


def gui_sprite(tint):
    """The old inventory icon: layer 0 as is, layer 1 (the cover) multiplied by the line colour."""
    a = np.asarray(Image.open(os.path.join(ASSETS, "textures", "item", "skill_book.png")).convert("RGBA"), np.float32) / 255
    b = np.asarray(Image.open(os.path.join(ASSETS, "textures", "item", "skill_book_cover.png")).convert("RGBA"), np.float32) / 255
    out = np.zeros((16, 16, 4), np.float32)
    b[..., :3] *= np.array(tint)
    for layer in (a, b):
        al = layer[..., 3:4]
        out[..., :3] = layer[..., :3] * al + out[..., :3] * (1 - al)
        out[..., 3:4] = np.maximum(out[..., 3:4], al)
    im = Image.fromarray((out * 255).astype(np.uint8), "RGBA").resize((W, H), Image.NEAREST)
    bg = Image.new("RGBA", (W, H), tuple(int(c * 255) for c in BG) + (255,))
    bg.alpha_composite(im)
    return bg.convert("RGB")


# ---------------------------------------------------------------------------------------------------------- 3.26.0: the painted books (skill_book_v2_<franchise>_<rarity>.json)
RARITY_NAMES = ["common", "uncommon", "rare", "epic", "mythic"]
_MODELS = {}


def v2_model(f, r):
    key = (f, r)
    if key not in _MODELS:
        m = load_model("skill_book_v2_%s_%s.json" % (f, r))
        _MODELS[key] = (m, quads(m))
    return _MODELS[key]


def parse_model_name(name):
    """'skill_book_v2_<franchise>_<rarity>.json' -> (franchise, rarity) or None."""
    base = name[:-5] if name.endswith(".json") else name
    if not base.startswith("skill_book_v2_"):
        return None
    rest = base[len("skill_book_v2_"):]
    for r in RARITY_NAMES:
        if rest.endswith("_" + r):
            return rest[:-(len(r) + 1)], r
    return None


def line_color(franchise):
    """The colour of the first line of a franchise in the shipped table (a real tint for the previews), else the default orange."""
    try:
        t = json.load(open(os.path.join(ASSETS, "skill_books.json"), encoding="utf-8"))
        for lid, l in t["lines"].items():
            if l.get("franchise") == franchise:
                return l["color"].lstrip("#")
    except Exception:
        pass
    return "E8541C"


def icon_image(f, r, tint_hex, size=192, bg=(0.36, 0.40, 0.46)):
    """The painted icon as the inventory draws it: layer 0 plus the tint layer multiplied by the line colour, on a flat background."""
    base = Image.open(os.path.join(ASSETS, "textures", "item", "skill_book_icons", "%s_%s.png" % (f, r))).convert("RGBA")
    tintp = os.path.join(ASSETS, "textures", "item", "skill_book_icons", "%s_%s_tint.png" % (f, r))
    out = Image.new("RGBA", base.size, tuple(int(c * 255) for c in bg) + (255,))
    out.alpha_composite(base)
    if os.path.exists(tintp):
        t = np.asarray(Image.open(tintp).convert("RGBA"), np.float32)
        col = np.array([int(tint_hex[i:i + 2], 16) for i in (0, 2, 4)], np.float32) / 255.0
        t[..., :3] *= col
        out.alpha_composite(Image.fromarray(np.clip(t, 0, 255).astype(np.uint8), "RGBA"))
    return out.convert("RGB").resize((size, size), Image.NEAREST)


def render_tile(f, r, view, tint_hex, ss=2, frame=0):
    """One view of one book, supersampled and reduced to W x H of the module (340)."""
    global W, H, ANIM_FRAME
    w0, h0, a0 = W, H, ANIM_FRAME
    ANIM_FRAME = frame
    W = H = w0 * ss
    try:
        if frame:
            _TEX.clear()
        model, qs = v2_model(f, r)
        if frame:
            qs = quads(model)
        tint = tuple(int(tint_hex[i:i + 2], 16) / 255.0 for i in (0, 2, 4))
        img = view_scene(model, qs, view, tint, left=view.endswith("left") if view.startswith("fp") else False)
        img = img.resize((w0, h0), Image.LANCZOS)
    finally:
        W, H, ANIM_FRAME = w0, h0, a0
    return img


def labelled(img, text):
    dr = ImageDraw.Draw(img)
    dr.text((6, 4), text, fill=(255, 255, 255))
    return img


def franchise_sheet(f, out, ss=2):
    """One franchise, the five rarities side by side: the inventory icon, the book zoomed in at a three-quarter angle, in the first-person hand, and on the ground."""
    col = line_color(f)
    views = [("z_34", "34"), ("fp_right", "hand"), ("tp_right_front", "third person"), ("ground_20_far", "ground")]
    icon_h = 200
    sheet = Image.new("RGB", (5 * W, icon_h + len(views) * H), (30, 32, 38))
    for j, r in enumerate(RARITY_NAMES):
        ic = icon_image(f, r, col, icon_h, (0.2, 0.22, 0.27))
        sheet.paste(ic, (j * W + (W - icon_h) // 2, 0))
        ImageDraw.Draw(sheet).text((j * W + 8, 6), "%s %s" % (f, r), fill=(255, 255, 255))
        for i, (v, label) in enumerate(views):
            sheet.paste(labelled(render_tile(f, r, v, col, ss), label), (j * W, icon_h + i * H))
    path = os.path.join(out, "franchise_%s.png" % f)
    sheet.save(path)
    return path


def overview_sheet(franchises, out, size=128):
    """The whole 11 x 5 matrix of icons in one picture."""
    pad = 6
    label_w = 190
    sheet = Image.new("RGB", (label_w + 5 * (size + pad), len(franchises) * (size + pad) + 22), (30, 32, 38))
    dr = ImageDraw.Draw(sheet)
    for j, r in enumerate(RARITY_NAMES):
        dr.text((label_w + j * (size + pad) + 8, 5), r, fill=(255, 255, 255))
    for i, f in enumerate(franchises):
        dr.text((6, 22 + i * (size + pad) + size // 2 - 6), f, fill=(255, 255, 255))
        col = line_color(f)
        for j, r in enumerate(RARITY_NAMES):
            sheet.paste(icon_image(f, r, col, size, (0.2, 0.22, 0.27)), (label_w + j * (size + pad), 22 + i * (size + pad)))
    path = os.path.join(out, "overview_matrix.png")
    sheet.save(path)
    return path


def hand_ground_sheet(out, picks, ss=2):
    """Hand and ground views of a few books: rows = book, columns = first person right, left, third person front, side, ground (two spins), item frame."""
    views = ["fp_right", "fp_left", "tp_right_front", "tp_right_side", "ground_20_far", "ground_130_far", "fixed"]
    sheet = Image.new("RGB", (len(views) * W, len(picks) * H), (30, 32, 38))
    for i, (f, r) in enumerate(picks):
        col = line_color(f)
        for j, v in enumerate(views):
            sheet.paste(labelled(render_tile(f, r, v, col, ss), "%s %s %s" % (f, r, v)), (j * W, i * H))
    path = os.path.join(out, "hand_ground_views.png")
    sheet.save(path)
    return path


def main_v2(a):
    import time
    t0 = time.time()
    fr = json.load(open(os.path.join(ASSETS, "skill_books.json"), encoding="utf-8"))["franchises"]
    os.makedirs(a.out, exist_ok=True)
    wanted = a.franchises.split(",") if a.franchises else fr
    written = []
    if "overview" in a.sheets:
        written.append(overview_sheet(fr, a.out))
    if "franchise" in a.sheets:
        for f in wanted:
            written.append(franchise_sheet(f, a.out, a.ss))
    if "hand" in a.sheets:
        picks = [("exiled_heavy_knight", "common"), ("tensura", "uncommon"), ("black_clover", "rare"), ("magi", "epic"), ("exiled_heavy_knight", "mythic"), ("fairy_tail", "mythic")]
        written.append(hand_ground_sheet(a.out, picks, a.ss))
    for w in written:
        print("wrote", w)
    print("%.1fs" % (time.time() - t0))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--franchise", default="generic")
    ap.add_argument("--color", default="E8541C")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "book_preview"))
    ap.add_argument("--model", default="skill_book_3d.json")
    ap.add_argument("--views", default="")
    ap.add_argument("--matrix", action="store_true", help="3.26.0: render the contact sheets of all 55 painted books (overview, one per franchise, hand and ground) into --out")
    ap.add_argument("--sheets", default="overview,franchise,hand", help="with --matrix: which sheets")
    ap.add_argument("--franchises", default="", help="with --matrix: only these franchises")
    ap.add_argument("--ss", type=int, default=2, help="supersampling of the tiles")
    a = ap.parse_args()
    if a.matrix:
        return main_v2(a)
    v2 = parse_model_name(a.model)
    model_name = a.model if a.franchise == "generic" else "skill_book_3d_%s.json" % a.franchise
    model = load_model(model_name)
    qs = quads(model)
    tint = tuple(int(a.color[i:i + 2], 16) / 255.0 for i in (0, 2, 4))
    views = ["fp_right", "fp_left", "tp_right_front", "tp_left_front", "tp_right_side", "tp_right_back", "ground_20", "ground_130", "fixed", "head_front", "head_side", "gui"]
    if a.views:
        views = a.views.split(",")
    os.makedirs(a.out, exist_ok=True)
    tiles = []
    for v in views:
        if v == "gui" and v2:
            img = icon_image(v2[0], v2[1], a.color, W)
        elif v == "gui":
            img = gui_sprite(tint)
        elif v2:
            img = render_tile(v2[0], v2[1], v, a.color, a.ss)
        else:
            img = view_scene(model, qs, v, tint, left=v.endswith("left") if v.startswith("fp") else False)
        dr = ImageDraw.Draw(img)
        dr.text((6, 4), v, fill=(255, 255, 255))
        img.save(os.path.join(a.out, "%s_%s.png" % (a.franchise, v)))
        tiles.append(img)
    cols = 4
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * W, rows * H))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % cols) * W, (i // cols) * H))
    sheet.save(os.path.join(a.out, "sheet_%s.png" % a.franchise))
    print("wrote", os.path.join(a.out, "sheet_%s.png" % a.franchise))


if __name__ == "__main__":
    main()
