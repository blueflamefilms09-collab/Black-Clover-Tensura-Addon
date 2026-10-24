"""Great Spirit GUI mock-up scenes: the HUD with the concentric gauge, the holographic grimoire menu (ink to matrix), the clover icon set, the alert
widget and the slime ripple, composed from gs_sprites textures with the gs_kit canvas. A software re-implementation, NOT the game."""
import math
import os
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

from gs_kit import (Canvas, SERIF, SERIF_BOLD, SERIF_ITALIC, bilinear, clamp01, hearts_row, hotbar, lerp, mixc, smooth, with_a, world_backdrop, ROOT)
from gs_sprites import (clover_frame, core_swirl, crest, filigree_corner, flow_strip, ring_bezel, ripple_ring)

SKILL = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill")
THEME_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "theme")

ELEMENTS = {"fire": (255, 126, 52), "water": (70, 164, 255), "wind": (112, 240, 176), "earth": (214, 168, 92), "time": (255, 214, 110), "dark": (190, 110, 255)}


class Kit:
    """All the sprites of one direction, built once."""

    def __init__(self, th):
        self.th = th
        self.corner = filigree_corner(th)
        self.bezel = ring_bezel(th)
        self.strip = np.asarray(flow_strip(th)).astype(np.float32) / 255.0
        self.swirl = np.asarray(core_swirl(th)).astype(np.float32) / 255.0
        self.clover = {n: clover_frame(th, n) for n in (3, 4, 5)}
        self.crest = crest(th)
        self.ripple = ripple_ring(th)
        self.circle = Image.open(os.path.join(THEME_TEX, "circle_outer.png")).convert("RGBA")
        self.art = {}

    def icon_art(self, rel):
        if rel not in self.art:
            im = Image.open(os.path.join(SKILL, rel + ".png")).convert("RGBA")
            self.art[rel] = im.crop((4, 4, 28, 28))               # the baked square frame is 4 px thick: only the picture is used
        return self.art[rel]


# ------------------------------------------------------------------------------------------------------ frame / panel
def neon_rect(c, x, y, w, h, rgb, a=1.0, halo=1.0):
    th_ = 1.0
    c.vector((x, y, x + w, y + h), lambda d, k, ox, oy: d.rectangle([(x - ox) * k, (y - oy) * k, (x + w - ox) * k, (y + h - oy) * k], outline=rgb + (255,), width=max(1, int(2.0 * k))),
             blur=2.2, add=True, alpha=0.55 * a * halo, gain=1.4)
    c.outline(x, y, w, h, rgb + (int(235 * a),), th_)
    c.outline(x + 1, y + 1, w - 2, h - 2, rgb + (int(70 * a),), 1.0)


def panel(c, K, x, y, w, h, corner=24, glow=1.0, alpha=1.0, corners=True, mid=True, body=True, accent=None):
    th = K.th
    if body:
        c.gradient(x, y, x + w, y + h, with_a(th.panel_top[:3], th.panel_top[3] / 255 * alpha), with_a(th.panel_bot[:3], th.panel_bot[3] / 255 * alpha))
        c.gradient(x + 1, y + 1, x + w - 1, y + 1 + min(h // 3, 22), (255, 255, 255, int(22 * alpha)), (255, 255, 255, 0))
    neon_rect(c, x, y, w, h, accent or th.neon, alpha, glow)
    if corners:
        cs = corner
        c.blit(K.corner, x - 1.5, y - 1.5, cs, cs)
        c.blit(K.corner, x + w - cs + 1.5, y - 1.5, cs, cs, flip_x=True)
        c.blit(K.corner, x - 1.5, y + h - cs + 1.5, cs, cs, flip_y=True)
        c.blit(K.corner, x + w - cs + 1.5, y + h - cs + 1.5, cs, cs, flip_x=True, flip_y=True)
    if mid and w > 60:
        for yy in (y, y + h):
            c.vector((x + w / 2 - 6, yy - 4, x + w / 2 + 6, yy + 4), lambda d, k, ox, oy, yy=yy: (
                d.polygon([((x + w / 2 - ox) * k, (yy - 3.2 - oy) * k), ((x + w / 2 + 5 - ox) * k, (yy - oy) * k), ((x + w / 2 - ox) * k, (yy + 3.2 - oy) * k), ((x + w / 2 - 5 - ox) * k, (yy - oy) * k)],
                          fill=th.metal_hi + (255,), outline=th.outline + (255,)),
                d.ellipse([(x + w / 2 - 1.2 - ox) * k, (yy - 1.2 - oy) * k, (x + w / 2 + 1.2 - ox) * k, (yy + 1.2 - oy) * k], fill=th.jewel + (255,))))


# ------------------------------------------------------------------------------------------------------ the concentric gauge
def gauge(c, K, cx, cy, d, mana, magi, t, elem=(255, 126, 52), corrupt=0.0):
    """Outer lane = Mana (flowing elemental particles), inner core = Magicules (swirling slime liquid). d = diameter in GUI px."""
    th = K.th
    N = c.px(d)
    if N < 8:
        return
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    ctr = (N - 1) / 2.0
    dx, dy = xx - ctr, yy - ctr
    R = N / 2.0
    rr = np.sqrt(dx * dx + dy * dy) / R
    ang = (np.arctan2(dx, -dy) % (2 * math.pi)) / (2 * math.pi)          # 0 at the top, clockwise
    px_ = 1.0 / R                                                         # one real pixel in rr units
    out = np.zeros((N, N, 4), np.float32)

    def over(src_rgb, src_a):
        """straight-alpha 'over' into out"""
        a0 = out[..., 3:4] / 255.0
        a1 = src_a[..., None]
        ao = a1 + a0 * (1 - a1)
        out[..., :3] = np.where(ao > 0, (src_rgb * a1 + out[..., :3] * a0 * (1 - a1)) / np.maximum(ao, 1e-6), 0)
        out[..., 3:4] = ao * 255.0

    disc = np.clip((0.90 - rr) / (1.5 * px_), 0, 1)                       # dark glass behind everything
    over(np.broadcast_to(np.array(th.outline, np.float32), (N, N, 3)) * 0.6, disc * 0.62)

    # ---- mana lane: u runs along the arc, v across the lane
    lane_in, lane_out = 0.605, 0.835
    lane = np.clip((rr - lane_in) / (1.2 * px_), 0, 1) * np.clip((lane_out - rr) / (1.2 * px_), 0, 1)
    arclen = 2 * math.pi * 0.72 * (d / 2.0)
    u = ang * arclen * 2.0 - t * 2.6
    v = (rr - lane_in) / (lane_out - lane_in) * (K.strip.shape[0] - 1)
    sm = bilinear(K.strip, u, v, wrap_u=True, wrap_v=False)
    sa = sm[..., 3]
    filled = ang < mana
    e = np.array(elem, np.float32)
    white = np.array([255, 255, 255], np.float32)
    hot = e[None, None, :] * (1 - 0.55 * (sa[..., None] ** 2)) + white * 0.55 * (sa[..., None] ** 2)
    lane_f = np.clip(0.30 + sa * 1.0, 0, 1)
    fillmask = filled & (rr > 0)
    over(np.where(fillmask[..., None], hot, 0), np.where(fillmask, lane_f * lane, 0))
    over(np.broadcast_to(np.array((24, 30, 46), np.float32), (N, N, 3)), np.where(fillmask, 0, 0.62 * lane))
    over(np.broadcast_to(np.array(th.metal_hi, np.float32) * 0.8, (N, N, 3)), np.where(fillmask, 0, sa * 0.10 * lane))
    # head of the fill: a spark
    head = np.exp(-(((ang - mana) * arclen * 2.0) ** 2) / 7.0) * lane * (mana > 0.01) * (ang <= mana + 0.02)
    over(np.broadcast_to(np.array(mixc(elem, (255, 255, 255), 0.6), np.float32), (N, N, 3)), np.clip(head * 0.75, 0, 1))
    # inner lane edge glow = element tint, makes the lane read as lit
    edge = np.exp(-((rr - lane_in - 0.012) / 0.02) ** 2) * fillmask
    over(np.broadcast_to(e, (N, N, 3)), np.clip(edge * 0.6, 0, 1))

    # ---- slime core
    core_r = 0.548
    core = np.clip((core_r - rr) / (1.3 * px_), 0, 1)
    top_y, bot_y = ctr - core_r * R, ctr + core_r * R
    level = bot_y - (bot_y - top_y) * magi
    wave = (0.9 * c.s / 3.0 + 0.4) * np.sin(xx / R * 7.5 + t * 0.32) + 0.45 * np.sin(xx / R * 13 - t * 0.5)
    surf = level + wave
    liquid = (yy > surf)
    rot = t * 0.018
    cs_, sn_ = math.cos(rot), math.sin(rot)
    sx, sy = (dx * cs_ + dy * sn_) / (core_r * R), (-dx * sn_ + dy * cs_) / (core_r * R)
    tw = K.swirl.shape[1]
    sw = bilinear(K.swirl, sx * (tw / 2.0) + tw / 2.0, sy * (tw / 2.0) + tw / 2.0, wrap_u=False, wrap_v=False)
    lum = sw[..., 0]
    depth = np.clip((yy - surf) / max(1.0, bot_y - level), 0, 1)
    a_ = np.array(th.core_a, np.float32)
    b_ = np.array(th.core_b, np.float32)
    mixk = (np.clip(1 - depth ** 0.6, 0, 1) * 0.55 + lum * 0.55)[..., None]
    liq_rgb = a_[None, None, :] * (1 - mixk) + b_[None, None, :] * mixk
    liq_rgb = liq_rgb * (0.78 + 0.5 * lum[..., None] * (1 - depth[..., None] * 0.6))
    over(np.where(liquid[..., None], np.clip(liq_rgb, 0, 255), 0), np.where(liquid, 0.94 * core, 0))
    # empty glass above the surface, a faint swirl so the core never looks dead
    over(np.broadcast_to(np.array((10, 16, 30), np.float32), (N, N, 3)), np.where(liquid, 0, 0.55 * core))
    over(np.broadcast_to(a_ * 1.2, (N, N, 3)), np.where(liquid, 0, 0.16 * lum * core))
    # meniscus line
    men = np.exp(-((yy - surf) / (0.9 * c.s / 2.0 + 0.6)) ** 2) * core * (magi > 0.02)
    over(np.broadcast_to(b_ * 0.4 + 150, (N, N, 3)), np.clip(men * 0.95, 0, 1))
    # glass vignette and specular
    vig = np.clip((rr - 0.30) / 0.25, 0, 1) ** 1.6 * core
    over(np.zeros((N, N, 3), np.float32), vig * 0.45)
    spec = np.exp(-(((dx + 0.22 * R) / (0.16 * R)) ** 2 + ((dy + 0.30 * R) / (0.07 * R)) ** 2)) * core
    over(np.broadcast_to(white, (N, N, 3)), np.clip(spec * 0.55, 0, 1))
    # bubbles
    for i in range(4):
        ph = (t * (0.011 + 0.003 * i) + i * 0.31) % 1.0
        bx = ctr + (math.sin(i * 2.3 + t * 0.03) * 0.28) * R
        by = bot_y - ph * (bot_y - level)
        br = (0.035 + 0.01 * (i % 2)) * R
        rb = np.sqrt((xx - bx) ** 2 + (yy - by) ** 2)
        ring = np.exp(-((rb - br) / (0.012 * R + 0.5)) ** 2) * core * (by > level + 2)
        over(np.broadcast_to(white, (N, N, 3)), np.clip(ring * 0.6, 0, 1))
    if th.key == "C" and magi > 0.25:                                      # the corrupt slime watches
        for sx_ in (-1, 1):
            ex, ey = ctr + sx_ * 0.17 * R, ctr + 0.02 * R
            em = np.exp(-(((xx - ex) / (0.045 * R)) ** 2 + ((yy - ey) / (0.085 * R)) ** 2) ** 1.5) * core * liquid
            over(np.broadcast_to(np.array(th.neon2, np.float32), (N, N, 3)), np.clip(em * 0.9, 0, 1))
    if corrupt > 0:
        over(np.broadcast_to(np.array((255, 40, 100), np.float32), (N, N, 3)), np.clip(core * corrupt * 0.35 * (0.5 + 0.5 * np.sin(t * 0.3)), 0, 1))
    lay = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")
    x0, y0 = cx - d / 2.0, cy - d / 2.0
    # lit lane glows into the world
    c.layer(lay, x0, y0)
    gl = lay.filter(ImageFilter.GaussianBlur(c.s * 1.6))
    c.layer(gl, x0, y0, add=True, alpha=0.45)
    c.blit(K.bezel, x0, y0, d, d)


# ------------------------------------------------------------------------------------------------------ the HUD
def hud_scene(K, scale, mana=0.82, magi=0.64, t=40.0, elem="fire", win=(1280, 720)):
    th = K.th
    gw, gh = win[0] / scale, win[1] / scale
    bg = world_backdrop(win[0], win[1], mood="day")
    c = Canvas(gw, gh, scale, bg)
    c.fill(gw / 2 - 5, gh / 2 - 0.5, gw / 2 + 5, gh / 2 + 0.5, (255, 255, 255, 220))
    c.fill(gw / 2 - 0.5, gh / 2 - 5, gw / 2 + 0.5, gh / 2 + 5, (255, 255, 255, 220))
    hx, hy = hotbar(c, gw / 2, gh - 1, 2)
    hearts_row(c, hx + 1, hy - 11, 10, 8)
    # the plate: frame + gauge + labels
    pw, ph = 118, 64
    px_, py_ = 7, gh - ph - 7
    panel(c, K, px_, py_, pw, ph, corner=22)
    gauge(c, K, px_ + 33, py_ + ph / 2, 50, mana, magi, t, ELEMENTS[elem])
    lx = px_ + 62
    c.text("JULIUS", lx, py_ + 8, with_a(th.metal_hi, 1), True)
    c.text("Time Magic", lx, py_ + 17, with_a(th.dim, 1), True, k=0.75)
    # two gauge legends with tiny markers
    for i, (name, val, col) in enumerate((("MANA", mana, ELEMENTS[elem]), ("MAGICULE", magi, th.core_b))):
        yy_ = py_ + 29 + i * 15
        c.fill(lx, yy_ + 1, lx + 3, yy_ + 6, col + (255,))
        c.text(name, lx + 6, yy_, (*th.text, 255), True, k=0.75)
        c.text("%d%%" % round(val * 100), lx + 6, yy_ + 6.5, (*mixc(col, (255, 255, 255), 0.35), 255), True, k=0.75)
    return c


def gauge_variants(K, scale):
    th = K.th
    gw, gh = 360, 120
    bg = world_backdrop(int(gw * scale), int(gh * scale), seed=3, mood="dusk")
    c = Canvas(gw, gh, scale, bg)
    panel(c, K, 4, 4, gw - 8, gh - 8, corner=20)
    names = ["fire", "water", "wind", "earth"]
    fills = [(0.92, 0.35), (0.55, 0.85), (0.30, 0.55), (0.08, 0.12)]
    for i, (n, (m, g)) in enumerate(zip(names, fills)):
        cx = 52 + i * 86
        gauge(c, K, cx, 52, 56, m, g, 30 + i * 21, ELEMENTS[n])
        c.text_c(n.upper(), cx, 86, (*th.text, 255), True, k=0.75)
        c.text_c("%d%% / %d%%" % (round(m * 100), round(g * 100)), cx, 96, (*th.dim, 255), True, k=0.75)
    return c
