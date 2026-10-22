#!/usr/bin/env python3
"""Builds the 3D model, the skins, the inventory icon and the animation of Shishou (the talking sword of 'Reincarnated as a Sword'), from the owner's reference image.

    python3 -B tools/gen_shishou_assets.py              build everything (deterministic, idempotent)
    python3 -B tools/gen_shishou_assets.py preview      also render docs/previews/shishou/ (needs only numpy + PIL)

The reference image (tools/shishou/shishou_reference.webp: a long straight silver-blue blade with three glowing cyan lines in the fuller, a gold crescent guard with small spikes, a grey horned dragon head
over a yellow gem and a black ornament, a white grip with cyan diamond inlays and a gold pommel) is the SOURCE of both the silhouette and the colours:
  * the silhouette of each part is read from the image (row extents for the lathe and the blade, traced and triangulated outlines for the guard, the head and the ornament: tools/shishou/shishou_geom.py);
  * the front and back skins sample the image at the place of every texel (so the ink lines, the eyes, the filigree and the cyan lines are the artist's own);
  * the cyan lines, the grip diamonds and the gem glow (the additive glow skin) come from the image's cyan and yellow masks.
Parts (model units are centimetres, y runs from the pommel (0) to the tip, x across, z through; the blade is seen from +z like the reference rotated half a turn so the tip points up):
  pommel_grip  a surface of revolution (gold pommel, grey band, the white grip with its seven cyan diamonds, the grey collar and the gold lens), traced from the image's rows
  tang         the dark checkered block under the lens
  guard        the gold crescent, its crown and its four spikes: traced outlines extruded in three stepped layers
  ornament     the black bat-wing ornament under the guard: traced outline extruded in two layers
  head         the grey horned dragon head: traced outline extruded in three layers (a domed face)
  gem          the yellow gem under the head (a glowing orb)
  blade        the long flat blade with ground edges, from the image's row extents
  inlay        the three cyan lines of the fuller, traced and raised a hair above the blade, glowing
It writes: assets/nusmp/weapon_meshes/shishou.txt, textures/entity/weapon/shishou.png + _glow.png, models/item/shishou.json + shishou_icon.json (through new197_lib.finish), textures/item/shishou.png (the inventory icon,
made from the reference), animations/entity/shishou_sword.animation.json (the four states), the class row in weapon_classes.json (both copies), the display preset and the Better Combat attribute file.
"""
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(TOOLS, "relics"))
sys.path.insert(0, os.path.join(TOOLS, "shishou"))

from new197_kit import *                                                   # noqa: F401,F403,E402
from new197_kit import P, lathe_y, orb, spike, ml, new_mesh, new_atlas, lay_and_paint, finish, plate, paint, put_glow   # noqa: E402
import shishou_geom as G                                                   # noqa: E402

REPO = os.path.abspath(os.path.join(TOOLS, ".."))
GAME_ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")
REF_PATH = os.path.join(TOOLS, "shishou", "shishou_reference.webp")
ITEM_ID = "shishou"
REAL_M = 1.45                                                              # the sword is about 1.45 blocks of sword length (the placement adds the hilt: 1.7 as drawn) long in hand (a long sword)

# ================================================================ the reference image
REF = np.asarray(Image.open(REF_PATH).convert("RGBA")).astype(np.float32)
H, W = REF.shape[:2]
RGB, AL = REF[..., :3], REF[..., 3]
CX, PY0, PY1, K = 226.0, 109.0, 1505.0, 0.1                                  # the sword's axis (px), its top (pommel) and tip rows (px), centimetres per pixel
LENGTH = (PY1 - PY0) * K
_R, _G, _B = RGB[..., 0], RGB[..., 1], RGB[..., 2]
OPAQUE = AL > 128
GOLD = OPAQUE & (_R > 150) & (_G > 120) & (_B < 170) & (_R - _B > 45)
CYAN = OPAQUE & (_B > 150) & (_R < 150) & (_B - _R > 60)
DARK = OPAQUE & (_R < 95) & (_G < 95) & (_B < 105)
WHITE = OPAQUE & (_R > 215) & (_G > 215) & (_B > 215)
GREY = OPAQUE & ~GOLD & ~CYAN & (np.abs(_R - _G) < 22) & (np.abs(_G - _B) < 30) & (_R > 100) & (_R < 215)
Y_UP = PY1 - PY0
GAIN = 1.08                                                                  # the game lights a skin a little darker than the flat art: a small gain keeps the colours of the reference


def Xm(px):
    return (CX - px) * K


def Ym(py):
    return (py - PY0) * K


def window(x0, y0, x1, y1):
    m = np.zeros((H, W), bool)
    m[y0:y1, x0:x1] = True
    return m


def sample(X, Y):
    """The reference's rgb (HxWx3) and alpha at the model points (X, Y) (arrays), bilinear; the sword is seen from +z with the tip up (the image turned half a turn)."""
    px = CX - np.asarray(X) / K
    py = PY0 + np.asarray(Y) / K
    px = np.clip(px, 0, W - 1.001)
    py = np.clip(py, 0, H - 1.001)
    x0, y0 = np.floor(px).astype(int), np.floor(py).astype(int)
    fx, fy = (px - x0)[..., None], (py - y0)[..., None]
    c = (REF[y0, x0] * (1 - fx) * (1 - fy) + REF[y0, x0 + 1] * fx * (1 - fy) + REF[y0 + 1, x0] * (1 - fx) * fy + REF[y0 + 1, x0 + 1] * fx * fy)
    return np.clip(c[..., :3] * GAIN, 0, 255), c[..., 3]


def mask_at(mask_f, X, Y):
    px = np.clip(np.rint(CX - np.asarray(X) / K).astype(int), 0, W - 1)
    py = np.clip(np.rint(PY0 + np.asarray(Y) / K).astype(int), 0, H - 1)
    return mask_f[py, px]


def row_extent(py, x0=0, x1=None):
    xs = np.nonzero(AL[int(py), x0:x1] > 40)[0]
    return (xs.min() + x0, xs.max() + x0) if len(xs) else None


def poly_model(poly_px):
    """A traced pixel-corner polygon (x, y down) -> model (X, Y)."""
    return [(Xm(x), Ym(y)) for (x, y) in poly_px]


def outer_polys(mask, min_area=30.0, eps=0.9):
    """All outer outlines of a mask (the loops that wind like the biggest one), simplified."""
    loops = G.trace_loops(mask)
    if not loops:
        return []
    big = max(loops, key=lambda l: abs(G.area(l)))
    sign = 1 if G.area(big) > 0 else -1
    return [G.simplify(l, eps) for l in loops if G.area(l) * sign > min_area]


# ================================================================ the painters
def make_ylook(prof):
    """For a lathe whose profile is [(Y, r)] (Y up toward the tip): a function (r, sign) -> Y of the point of the surface that faces +y (sign > 0) or -y (sign < 0) at the radius r, so the faces that look along the
    axis (the lens, the collar, the pommel's end) take the reference's colour of the place they really are."""
    seg = list(zip(prof[:-1], prof[1:]))

    def look(r, sign):
        best, best_y = 1e9, None
        for (y0, r0), (y1, r1) in seg:
            dr = r1 - r0
            if abs(dr) < 1e-6 or (dr > 0) == (sign < 0):                    # a radius that grows toward the tip faces the butt (-y)
                lo, hi = min(r0, r1), max(r0, r1)
                if lo - 1e-6 <= r <= hi + 1e-6 and abs(dr) > 1e-6:
                    return y0 + (y1 - y0) * ((r - r0) / dr)
                d = min(abs(r - lo), abs(r - hi))
                if d < best:
                    best, best_y = d, (y0 if abs(r - r0) < abs(r - r1) else y1)
        return best_y if best_y is not None else (prof[0][0] if sign < 0 else prof[-1][0])
    return np.vectorize(look)


def paint_from_reference(fallback, glow=None, wall=0.72, ylook=None):
    """A painter: the front and back faces sample the reference (the artist's colours and ink); a side face takes the colour of the reference a little inside the silhouette (or the fallback), a cap the fallback.
    glow = (mask_float_image, rgb, strength, core) lights the glow skin where the mask is on."""
    fb = np.array(fallback, np.float32)

    def fn(pc):
        img = pc.img
        if pc.kind == "dom" and pc.key[0] == 2:
            rgb, a = sample(pc.X, pc.Y)
            use = (a > 128)[..., None]
            img[..., :3] = np.where(use, rgb, fb[None, None, :])
            X, Y = pc.X, pc.Y
        elif pc.kind == "dom" and pc.key[0] == 0:
            Y = pc.B
            X = np.zeros_like(Y)
            for j in range(Y.shape[0]):
                e = row_extent(PY0 + float(Y[j, 0]) / K)
                if e is not None:
                    X[j, :] = pc.key[1] * (e[1] - e[0]) * 0.5 * K * wall
            rgb, a = sample(X, Y)
            use = (a > 128)[..., None]
            img[..., :3] = np.where(use, rgb, fb[None, None, :]) * 0.82
        elif pc.kind == "dom" and pc.key[0] == 1 and ylook is not None:
            X = pc.A
            r = np.hypot(pc.A, pc.B)
            Y = ylook(r, pc.key[1])
            rgb, a = sample(X * 0.0 + np.clip(r, 0, 1e9) * 0.55, Y)
            use = (a > 128)[..., None]
            img[..., :3] = np.where(use, rgb, fb[None, None, :]) * 0.9
            X = None
        else:
            img[..., :3] = fb[None, None, :] * 0.8
            X = Y = None
        img[..., 3] = 255
        if glow is not None and X is not None and (pc.kind == "dom" and pc.key[0] == 2):
            m = mask_at(glow[0], X, Y)
            if m.max() > 0.01:
                put_glow(pc, m, glow[1], glow[2], glow[3])
    return fn


def blur_mask(mask, r=0.8):
    return np.asarray(Image.fromarray((mask * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r)), np.float32) / 255.0


# ================================================================ the model
def build():
    m = new_mesh(ITEM_ID)
    # ---- the lathe: pommel, grip, collar, lens (rows of the image, py 111 .. 390)
    prof = []
    for py in range(112, 391, 4):
        e = row_extent(py, 150, 300)
        if e is None:
            continue
        prof.append((Ym(py), (e[1] - e[0] + 1) * 0.5 * K))
    p = P(m, "pommel_grip", "gold", base=(226, 214, 168), smooth=50)
    full = [(0.0, prof[0][1] * 0.6)] + [(y - 0.0, r) for (y, r) in prof] + [(Ym(392), 0.0)]
    ml.lathe(p, "y", (0, 0, 0), full, sides=16, caps=(True, True))
    ylook = make_ylook(full)
    # ---- the checkered tang between the lens and the guard
    t = P(m, "tang", "iron", base=(60, 60, 70), smooth=35)
    ml.box(t, (-1.25, Ym(386), -1.0), (1.25, Ym(424), 1.0), 0.12)
    # ---- the guard: the gold of py 395..545, traced, three stepped layers
    gold = G.close(GOLD & window(38, 392, 415, 548), 2)
    gold = G.fill_holes(gold)
    guard = P(m, "guard", "gold", base=(232, 204, 100), smooth=35)
    for er, hz in ((0, 0.85), (7, 1.2)):
        for poly in outer_polys(G.erode(gold, er) if er else gold, 25.0, 1.0):
            G.extrude(guard, poly_model(poly), -hz, hz)
    # ---- the black ornament under the guard
    dark = G.close(DARK & window(120, 430, 335, 580) & ~window(188, 400, 266, 505), 3)
    dark = G.fill_holes(dark | window(190, 470, 262, 535) & OPAQUE)
    orn = P(m, "ornament", "iron", base=(52, 54, 62), smooth=35)
    for er, hz in ((0, 0.42), (6, 0.58)):
        for poly in outer_polys(G.erode(dark, er) if er else dark, 40.0, 1.0):
            G.extrude(orn, poly_model(poly), -hz, hz)
    # ---- the head: grey face, horns, side fins
    head = (GREY | WHITE | (DARK & window(206, 440, 246, 480))) & window(183, 400, 272, 512)
    head = G.fill_holes(G.close(head, 3))
    head = G.largest_component(head) | G.largest_component(head & ~G.dilate(G.largest_component(head), 2))
    hd = P(m, "head", "silver", base=(166, 174, 188), smooth=35)
    for er, hz in ((0, 0.9), (4, 1.25), (9, 1.6)):
        for poly in outer_polys(G.erode(head, er) if er else head, 20.0, 0.9):
            G.extrude(hd, poly_model(poly), -hz, hz)
    # ---- the gem
    gem = orb(m, "gem", "crystal", (Xm(226), Ym(519), 0.0), 1.0, base=(210, 222, 70), glow=True, rx=1.25, ry=1.8, rz=1.0, sides=14, stacks=9, smooth=60)
    # ---- the blade (alpha extents of the image rows), hexagonal section: a flat of half thickness 0.5 and ground edges
    st = [(Ym(520), None, None, 0.0)]
    rows = list(range(536, 1480, 24)) + [1480, 1490, 1498]
    st = []
    for py in rows:
        e = row_extent(py, 120, 330)
        if e is None:
            continue
        s = (py - 536) / (PY1 - 536)
        st.append((Ym(py), Xm(e[1] + 1), Xm(e[0]), 0.62 * (1 - 0.55 * s ** 2)))
    e0 = row_extent(536, 120, 330)
    st.insert(0, (Ym(512), Xm(e0[1] + 1), Xm(e0[0]), 0.62))
    st.append((Ym(PY1), Xm(226), Xm(226), 0.0))
    blade = plate(m, "blade", "steel", st, bevel=0.42, base=(188, 204, 214), smooth=40)
    # ---- the three cyan lines of the fuller, raised a hair above the flat
    cy = G.close(CYAN & window(160, 540, 295, 1300), 1)
    inlay = P(m, "inlay", "crystal", base=(80, 214, 255), glow=True, smooth=20)
    for poly in outer_polys(cy, 12.0, 0.8):
        mp = poly_model(poly)
        G.extrude(inlay, mp, -0.64, 0.64)
    atlas = new_atlas(1024)
    lay_and_paint(m, atlas)
    cyan_f = blur_mask(CYAN, 0.7)
    gem_f = blur_mask(GOLD & window(212, 498, 242, 540), 0.8)
    glow_cyan = (cyan_f, (70, 215, 255), 1.0, 0.45)
    paint(atlas, m.part("pommel_grip"), paint_from_reference((214, 214, 222), glow_cyan, ylook=ylook))
    paint(atlas, m.part("tang"), paint_from_reference((60, 60, 70)))
    paint(atlas, m.part("guard"), paint_from_reference((232, 204, 100)))
    paint(atlas, m.part("ornament"), paint_from_reference((52, 54, 62)))
    paint(atlas, m.part("head"), paint_from_reference((166, 174, 188), (blur_mask(GOLD & window(222, 436, 232, 456), 0.6), (255, 220, 90), 0.8, 0.4)))
    paint(atlas, m.part("blade"), paint_from_reference((188, 204, 214), glow_cyan))
    paint(atlas, m.part("inlay"), paint_from_reference((80, 214, 255), glow_cyan))

    def gem_paint(pc):
        t = np.clip(pc.T, 0, 1)
        base = np.array([196, 214, 60], np.float32)
        hi = np.array([255, 252, 190], np.float32)
        k = (np.clip(1.0 - 2.4 * np.abs(t - 0.62), 0, 1) * 0.7)[..., None]
        pc.img[..., :3] = base * (1 - k) + hi * k
        pc.img[..., 3] = 255
        put_glow(pc, np.full(pc.img.shape[:2], 0.55, np.float32), (235, 240, 90), 0.55, 0.5)
    paint(atlas, gem, gem_paint)
    return m, atlas


# ================================================================ the inventory icon
def make_icon(path, size=32, widen=1.75):
    """The sprite: the reference widened across (a slender sword would vanish in 32 pixels), turned onto the sprite diagonal (tip to the upper right), downsampled, its colours pushed (saturation, the cyan
    lines kept bright), one outline pixel, as a 32 x 32 pixel icon."""
    im = Image.fromarray(np.clip(REF, 0, 255).astype(np.uint8), "RGBA").crop((30, 100, 420, 1512))
    im = im.resize((int(im.size[0] * widen), im.size[1]), Image.LANCZOS)
    im = im.rotate(-45 + 180, expand=True, resample=Image.BICUBIC)                      # the pommel to the lower left, the tip to the upper right
    im = im.crop(im.getbbox())
    s = (size - 2) / max(im.size)
    im = im.resize((max(1, round(im.size[0] * s)), max(1, round(im.size[1] * s))), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(im, ((size - im.size[0]) // 2, (size - im.size[1]) // 2))
    a = np.asarray(canvas).astype(np.float32).copy()
    mask = a[..., 3] > 100
    rgb = a[..., :3]
    lum = rgb.mean(axis=2, keepdims=True)
    rgb[:] = np.clip(lum + (rgb - lum) * 1.2, 0, 255)                                  # a little more colour
    rgb[:] = np.clip((rgb - 128) * 1.12 + 128, 0, 255)
    cyan = (rgb[..., 2] > rgb[..., 0] + 85) & (rgb[..., 2] > 200)
    rgb[cyan] = rgb[cyan] * 0.4 + np.array([60, 215, 255], np.float32) * 0.6
    a[..., 3] = np.where(mask, 255, 0)
    out = a.astype(np.uint8)
    pad = np.pad(mask, 1)
    ring = np.zeros_like(mask)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dy or dx:
                ring |= pad[1 + dy:size + 1 + dy, 1 + dx:size + 1 + dx]
    ring &= ~mask
    out[ring] = (14, 18, 30, 255)
    Image.fromarray(out, "RGBA").save(path)


# ================================================================ the animation (the mod's own animation.json format, parsed by client/geo/GeoAnim)
def animation_json():
    """Four clips on one bone, 'sword': rotation in degrees (x: pitch, tip DOWN positive; y: yaw; z: roll about the blade) and position in pixels (x right, y up, z forward), linearly interpolated.
    idle: a slow vertical sine, the blade angled 15 degrees down; aggro: horizontal with a small rotational jitter; attack: pulls back, thrusts and slashes (not looping, held on its last frame);
    dormant: static, the tip into the ground."""
    idle_t = [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0]
    idle_y = [0.0, 1.13, 1.6, 1.13, 0.0, -1.13, -1.6, -1.13, 0.0]                      # a sine, 1.6 px (0.1 block) high, four seconds a period
    idle_rz = [0.0, 1.0, 1.5, 1.0, 0.0, -1.0, -1.5, -1.0, 0.0]
    jit_t = [0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4]
    jit_rz = [0.0, 1.4, -1.0, 1.2, -1.5, 0.9, -1.1, 1.3, 0.0]
    jit_ry = [0.0, -1.0, 1.2, -0.8, 1.0, -1.3, 0.7, -0.9, 0.0]

    def keys(ts, vals):
        return {("%g" % t): v for t, v in zip(ts, vals)}
    doc = {"format_version": "1.8.0", "nusmp_note": "bone 'sword': rotation degrees (x pitch, tip down positive; y yaw; z roll about the blade), position pixels (x right, y up, z forward). Drawn by client/shishou/ShishouRenderer.",
           "animations": {
               "animation.shishou.idle": {"loop": True, "animation_length": 4.0, "bones": {"sword": {
                   "rotation": {"0": [15, 0, 0], "1": [15, 0, 1.5], "2": [15, 0, 0], "3": [15, 0, -1.5], "4": [15, 0, 0]},
                   "position": keys(idle_t, [[0, y, 0] for y in idle_y])}}},
               "animation.shishou.aggro": {"loop": True, "animation_length": 0.4, "bones": {"sword": {
                   "rotation": keys(jit_t, [[0, jit_ry[i], jit_rz[i]] for i in range(len(jit_t))]),
                   "position": {"0": [0, 0, 0]}}}},
               "animation.shishou.attack": {"loop": "hold_on_last_frame", "animation_length": 0.7, "bones": {"sword": {
                   "rotation": {"0": [0, 0, 0], "0.2": [-12, 0, 0], "0.3": [4, 0, -10], "0.4": [6, 0, -50], "0.55": [2, 0, 40], "0.7": [0, 0, 0]},
                   "position": {"0": [0, 0, 0], "0.2": [0, 0, -14], "0.3": [0, 0, 22], "0.4": [0, 0, 14], "0.55": [0, 0, 8], "0.7": [0, 0, 0]}}}},
               "animation.shishou.dormant": {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": {"sword": {
                   "rotation": {"0": [90, 0, 4], "1": [90, 0, 4]},
                   "position": {"0": [0, 13.6, 0], "1": [0, 13.6, 0]}}}}}}
    return doc


def write_animation():
    d = os.path.join(GAME_ASSETS, "animations", "entity")
    os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, "shishou_sword.animation.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(animation_json(), f, indent=2)
        f.write("\n")


# ================================================================ the rows of the shared tables (class, display preset, Better Combat)
def class_rows():
    """The class row (longsword) in both copies of weapon_classes.json, by text insertion (the files keep their formatting and every other row)."""
    game = os.path.join(GAME_ASSETS, "weapon_classes.json")
    text = open(game, encoding="utf-8", newline="").read()
    row = '    "%s": "longsword",' % ITEM_ID
    if re.search(r'^    "%s": "[^"]*",' % ITEM_ID, text, re.M):
        text = re.sub(r'^    "%s": "[^"]*",' % ITEM_ID, row, text, flags=re.M)
    else:
        text = re.sub(r'(^    "club_of_rot": "club",)', lambda mt: row + "\n" + mt.group(1), text, count=1, flags=re.M)
    open(game, "w", encoding="utf-8", newline="").write(text)
    tools = os.path.join(REPO, "tools", "relics", "weapon_classes.json")
    text = open(tools, encoding="utf-8", newline="").read()
    block = '    "%s": {\n      "class": "longsword"\n    },' % ITEM_ID
    if '"%s": {' % ITEM_ID in text:
        text = re.sub(r'    "%s": \{\s*"class": "[^"]*"\s*\},' % ITEM_ID, lambda mt: block, text)
    else:
        text = re.sub(r'(    "club_of_rot": \{)', lambda mt: block + "\n" + mt.group(1), text, count=1)
    open(tools, "w", encoding="utf-8", newline="").write(text)


def presets():
    """The oversized-weapon display rule for this one weapon (the rows of every other weapon stay as they are): tools/relics/display_presets.py."""
    import display_presets as dp
    mesh = dp.read_mesh(os.path.join(dp.MESHES, ITEM_ID + ".txt"))
    mjson = os.path.join(dp.MODELS, ITEM_ID + ".json")
    raw = open(mjson, encoding="utf-8").read()
    model = json.loads(raw)
    plan = dp.plan_for(ITEM_ID, model, mesh)
    entry = {"id": ITEM_ID, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
    if plan["action"] == "preset":
        entry.update(first_person_scale=plan["scale"], first_person_translation_px=plan["translation"], tip_out_of_view=not plan["tip_in_view"],
                     ground_scale=plan["views"][0], fixed_scale=plan["views"][1])
    if plan["action"] in ("preset", "restore"):
        new = dp.apply_plan(model, plan)
        text = json.dumps(new, indent=2)
        if text != raw:
            open(mjson, "w", encoding="utf-8", newline="\n").write(text)
    text = open(dp.MANIFEST, encoding="utf-8", newline="").read()
    frag = "\n".join("    " + ln for ln in json.dumps(entry, indent=2).split("\n"))
    existing = re.search(r'    \{\n      "id": "%s",.*?\n    \}' % ITEM_ID, text, re.S)
    if existing:
        text = text[:existing.start()] + frag + text[existing.end():]
    else:
        at = None
        for mt in re.finditer(r'    \{\n      "id": "([^"]+)",', text):
            if mt.group(1) > ITEM_ID:
                at = mt.start()
                break
        if at is None:
            raise SystemExit("no place for the manifest row")
        text = text[:at] + frag + ",\n" + text[at:]
    open(dp.MANIFEST, "w", encoding="utf-8", newline="").write(text)
    return entry


def add_gripxyz():
    """A `gripxyz x y z` line in the mesh (docs/weapon_grip.md): the hand holds the middle of the white grip."""
    path = os.path.join(GAME_ASSETS, "weapon_meshes", ITEM_ID + ".txt")
    lines = open(path, encoding="utf-8").read().split("\n")
    lines = [ln for ln in lines if not ln.startswith("gripxyz")]
    at = 0
    for i, ln in enumerate(lines):
        if ln.startswith("place") or ln.startswith("glow") or ln.startswith("grip "):
            at = i + 1
    lines.insert(at, "gripxyz 0.000 %.3f 0.000" % Ym(265))
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines))


def main(args=None):
    args = args or []
    m, atlas = build()
    finish(ITEM_ID, m, atlas, "longsword", glow=("7FEBFF", 0.10, 0.55, 1.0), real_m=REAL_M)
    add_gripxyz()
    make_icon(os.path.join(GAME_ASSETS, "textures", "item", ITEM_ID + ".png"))
    write_animation()
    class_rows()
    e = presets()
    print("  %s L=%.2f %s" % (ITEM_ID, e["length_blocks"], e["action"]))
    if "preview" in args:
        sys.path.insert(0, os.path.join(TOOLS, "shishou"))
        import shishou_preview
        shishou_preview.main()


if __name__ == "__main__":
    main(sys.argv[1:])
