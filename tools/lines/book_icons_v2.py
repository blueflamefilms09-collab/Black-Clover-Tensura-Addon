"""The painted 64 x 64 inventory icons of the 3.26.0 Skill Books (one per franchise x rarity, plus the layer the client tints).

How: the icon is the book's OWN 3D model (same geometry, same textures) rendered at 8x from a three-quarter view (the spine to the left, the fore-edge pages at the right, the cover tilted up
like the icons of the references), then finished like a painted sprite: a dark outline in a darker version of the local colour, a rim light on the lit edge, a slight saturation and contrast
lift, a soft glow behind the emissive books. So the icon and the hand model can never disagree, and an icon is as detailed as the 64 px allow.
The tint layer holds exactly the pixels of the ribbon, the emblem plaque and the small clasp gem (grey, lit): the client multiplies it with the line's colour (tint index 1).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import book_model_v2 as BM  # noqa: E402
import preview_book as pb  # noqa: E402

SS = 8                      # render scale
S = 64
ICON_ROT = (-14.0, 24.0, -30.0)     # roll about Z, tilt about X, turn about Y (degrees): see icon_matrix


def icon_matrix(rot=ICON_ROT):
    rz, rx, ry = rot
    return pb.Rz(rz) @ pb.Rx(rx) @ pb.Ry(ry) @ pb.S(1.0, 1.0, 1.0) @ pb.T(-0.5, -0.5, -0.5)


def render_pass(qs, tint_rgb, rot=ICON_ROT, size=S * SS, dist=6.0, scale=1.0):
    cv = pb.Canvas(size, size, (0.0, 0.0, 0.0))
    view = pb.look_at((0, 0, dist), (0, 0, 0))
    k = dist * size * scale
    m = icon_matrix(rot)
    pb.render_quads(cv, qs, m, view, k, tint_rgb)
    alpha = np.isfinite(cv.z).astype(np.float32)
    return cv.img.copy(), alpha


def shrink(img, alpha):
    """Premultiplied box downsample to the icon size."""
    pm = np.concatenate([img * alpha[..., None], alpha[..., None]], 2)
    h, w = pm.shape[:2]
    f = h // S
    pm = pm.reshape(S, f, S, f, 4).mean((1, 3))
    a = pm[..., 3]
    rgb = np.where(a[..., None] > 1e-4, pm[..., :3] / np.maximum(a[..., None], 1e-4), 0)
    return rgb, a


def paint_finish(rgb, a, glow_rgb=None):
    """Outline, rim light, saturation. rgb 0..1 (S,S,3), a 0..1 (S,S) -> RGBA float 0..255."""
    # saturation and a little contrast
    lum = (rgb * np.array([0.3, 0.59, 0.11])).sum(2, keepdims=True)
    rgb = np.clip(lum + (rgb - lum) * 1.18, 0, 1)
    rgb = np.clip((rgb - 0.5) * 1.1 + 0.5, 0, 1)
    solid = a > 0.5
    # rim light: pixels whose upper-left neighbour is outside get a lift
    up = np.zeros_like(solid)
    up[1:, 1:] = ~solid[:-1, :-1]
    rgb = np.where((solid & up)[..., None], np.clip(rgb * 1.22 + 0.04, 0, 1), rgb)
    # outline: transparent pixels that touch a solid one become a dark version of that neighbour's colour
    out = np.zeros((S, S, 4), np.float32)
    out[..., :3] = rgb * 255
    out[..., 3] = np.where(solid, 255, a * 255 * 0.0)
    pad = np.pad(solid, 1)
    pcol = np.pad(rgb, ((1, 1), (1, 1), (0, 0)))
    near = np.zeros((S, S), bool)
    acc = np.zeros((S, S, 3), np.float32)
    cnt = np.zeros((S, S), np.float32)
    for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)):
        nb = pad[1 + dy:1 + dy + S, 1 + dx:1 + dx + S]
        col = pcol[1 + dy:1 + dy + S, 1 + dx:1 + dx + S]
        w = nb.astype(np.float32) * (1.0 if abs(dy) + abs(dx) == 1 else 0.7)
        acc += col * w[..., None]
        cnt += w
        near |= nb
    edge = (~solid) & near
    oc = acc / np.maximum(cnt[..., None], 1e-4)
    oc = oc * 0.22 + np.array([0.03, 0.02, 0.05]) * 0.5
    out[edge, :3] = oc[edge] * 255
    out[edge, 3] = 255
    if glow_rgb is not None:
        gr, ga = glow_rgb
        far = (~solid) & (~edge)
        out[far, :3] = gr[far] * 255
        out[far, 3] = (ga * 255)[far]
    return out


def glow_halo(a, color, strength):
    import book_paint_v2 as P
    g = P.gauss(a.astype(np.float32), 3.2)
    ga = np.clip(g * 1.1 * strength, 0, 0.6)
    col = np.zeros((S, S, 3), np.float32) + np.array(color, np.float32) / 255.0
    return col, ga


HALO = {"epic": ((176, 90, 255), 0.7), "mythic": ((255, 120, 40), 0.95)}


def icon(franchise, rarity):
    model = pb.load_model("skill_book_v2_%s_%s.json" % (franchise, rarity))
    qs = pb.quads(model)
    img_a, alpha = render_pass(qs, (1, 1, 1))
    img_b, _ = render_pass(qs, (0, 0, 0))
    rgb_a, a = shrink(img_a, alpha)
    rgb_b, _ = shrink(img_b, alpha)
    diff = np.abs(img_a - img_b).sum(2) * alpha
    tmask_hi = (diff > 0.02).astype(np.float32)
    _, tmask = shrink(np.zeros_like(img_a), tmask_hi)
    halo = None
    if rarity in HALO:
        c, s = HALO[rarity]
        halo = glow_halo(a, c, s)
    base = paint_finish(rgb_b + (rgb_a - rgb_b) * 0.0, a, halo)
    # in the base layer the tinted pixels show the neutral (grey) version; the tint layer repeats them with the client's colour multiplied in
    base_rgb = paint_finish(rgb_a, a, halo)
    tint = np.zeros((S, S, 4), np.float32)
    lum = np.clip(rgb_a * 1.0, 0, 1)
    tm = np.clip(tmask * 1.6, 0, 1) * (a > 0.5)
    tint[..., :3] = np.clip(lum * 255 * 1.12, 0, 255)
    tint[..., 3] = (tm > 0.5) * 255
    return base_rgb, tint
