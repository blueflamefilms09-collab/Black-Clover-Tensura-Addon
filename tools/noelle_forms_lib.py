"""Noelle Silva's armour forms (1.99.0): the shared kit of tools/gen_noelle_forms_geo.py.

It sits on tools/great_spirits_lib.py (FnMat, Piece, Anim, grid / paste / glow helpers) and adds the materials and builders the four forms share:

  * enamel   pearl-white enamel plate with a gold border, a seigaiha (wave) engraving, an optional gem        (Valkyrie / Saint Valkyrie armour)
  * wfeather an angel's feather, white at the quill to a glowing blue tip, barbs, a quill, a rounded point       (wings of the Valkyrie and Mermaid forms)
  * dscale   dragon scales: rows of overlapping keeled scales, belly plates, glowing seams                       (Sea Dragon God and Black Dragon armour)
  * membrane a dragon-wing membrane: veins fanning from the arm, a scalloped edge (ragged for the black form)
  * horn     a ringed horn, root to tip
  * gem      a cut gem
  * gold / ink  brushed gold; ink-black with a blue flame edge

Frame of the files: model pixels = 1/16 block, y UP, front on -z, +x the model's left; right arm at x = -5, shoulders at y = 24 (see water_aura_lib.py).
Everything is deterministic (seeded by the model id, the cube number and the face), so the generator can be re-run. Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import (FnMat, Piece, Anim, Mat, grid, blank, paste, add_glow, cover, disc, ellipse, capsule, film, hexcells, hash2, col, fbm,  # noqa: E402,F401
                               smooth, sine_rot, loop_rot, spin_y, mbox, stepped_spike, shell_texture, rgb, TAU, shaded, solid)
from water_aura_lib import mpivot, feather_wing, tail_chain  # noqa: E402,F401
from geo_builder import ROOT  # noqa: E402,F401


def _rng(tag, face, seed):
    return np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 7919) % (2 ** 32))


def _ramp(t, stops):
    """Colour along stops [(pos, rgb array), ...] for t in 0..1 (array): returns (h, w, 3)."""
    t = np.clip(np.asarray(t, dtype=np.float32), 0.0, 1.0)
    out = np.zeros(t.shape + (3,), dtype=np.float32)
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        m = (t >= p0) & (t <= p1)
        k = ((t - p0) / max(1e-6, p1 - p0))[..., None]
        out = np.where(m[..., None], c0 + (c1 - c0) * k, out)
    out = np.where((t < stops[0][0])[..., None], stops[0][1], out)
    out = np.where((t > stops[-1][0])[..., None], stops[-1][1], out)
    return out


def _finish(img, g, face, shade=True):
    """The common face shading: a lighter top, a darker bottom, slightly darker sides."""
    if shade:
        k = {"up": 1.10, "down": 0.80, "west": 0.94, "east": 0.94}.get(face, 1.0)
        img[..., :3] = np.clip(img[..., :3] * k, 0, 255)
    return img, g


# ====================================================================================================================== enamel armour plate
def enamel(seed, base="EEF5FF", dark="8DA8CE", light="FFFFFF", trim="E6BE5E", trim_d="8A6520", inlay="3F7FDC", glow="9FE8FF", gem=None, wave=0.55, trim_glow=0.40, border=0.085, pearl=0.07):
    """A pearl-white enamel plate. `gem` = hex colour of a cut gem in the middle of the north face (None = no gem); `wave` the strength of the engraved seigaiha waves; the border is gold."""
    B, D, L, T, TD, IN, GL = col(base), col(dark), col(light), col(trim), col(trim_d), col(inlay), col(glow)

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        img, g = blank(w, h)
        n = fbm(rng, w, h, 3, 3) - 0.5
        lum = np.clip(0.74 + 0.22 * (1 - v) - 0.07 * np.abs(u - 0.5) + n * 0.08, 0, 1)
        c = np.where((lum < 0.55)[..., None], D + (B - D) * (lum / 0.55)[..., None], B + (L - B) * ((lum - 0.55) / 0.45)[..., None])
        sheen = film(0.35 * u + 0.5 * v + seed * 0.07, sat=0.22, lo=0.48, hi=0.92)
        c = c * (1 - pearl) + sheen * pearl
        # engraved seigaiha waves: rows of overlapping arcs
        if wave > 0:
            cs = max(5.0, min(w, h) * 0.55)
            rowh = cs * 0.5
            row = np.floor(y / rowh)
            cx = ((x + (row % 2) * cs * 0.5) % cs) - cs * 0.5
            cy = (y % rowh) - rowh
            d = np.hypot(cx, cy) / (cs * 0.5)
            ring = (np.abs(((d * 3.0) % 1.0) - 0.5) > 0.40) & (d < 1.02)
            c = np.where(ring[..., None], c * (1 - 0.40 * wave) + IN * 0.40 * wave, c)
        img[..., :3] = c
        img[..., 3] = 255
        ex = np.minimum(x, w - x)
        ey = np.minimum(y, h - y)
        edge = np.minimum(ex, ey)
        bw = max(1.1, min(w, h) * border)
        goldv = np.clip(0.55 + 0.45 * (1 - v) + 0.2 * (fbm(rng, w, h, 3, 4) - 0.5), 0, 1)
        gc = TD + (T - TD) * goldv[..., None]
        gc = gc + (np.array([255, 245, 200], dtype=np.float32) - gc) * (np.clip(1 - np.abs(edge - bw * 0.45) / (bw * 0.45), 0, 1) * 0.45)[..., None]
        on = edge < bw
        img[..., :3] = np.where(on[..., None], gc, img[..., :3])
        line = (edge >= bw) & (edge < bw + max(0.9, bw * 0.45))
        img[..., :3] = np.where(line[..., None], IN, img[..., :3])
        add_glow(g, col("FFE49A"), on * trim_glow)
        add_glow(g, GL, line * 0.55)
        if gem and face == "north":
            gcx, gcy = w * 0.5, h * 0.5
            r = max(2.0, min(w, h) * 0.22)
            dm = (np.abs(x - gcx) + np.abs(y - gcy) * 1.2) / r
            ins = dm < 1.0
            rim = (dm >= 0.82) & (dm < 1.0)
            gc2 = col(gem)
            facet = 0.55 + 0.45 * np.cos((np.arctan2(y - gcy, x - gcx) * 4.0))
            gcol = gc2 * (0.55 + 0.6 * facet[..., None]) * (1.0 - 0.45 * dm[..., None]) + np.array([255, 255, 255], dtype=np.float32) * (np.clip(0.4 - dm, 0, 1) * 1.3)[..., None]
            img[..., :3] = np.where(ins[..., None], np.clip(gcol, 0, 255), img[..., :3])
            img[..., :3] = np.where(rim[..., None], T, img[..., :3])
            add_glow(g, gc2, ins * (0.9 - 0.4 * dm))
        return _finish(img, g, face)
    return FnMat(fn, "enamel")


def gold(seed, base="E6BE5E", dark="7C5818", light="FFF4C4", glow="FFE49A", glow_a=0.35, stripe=True):
    B, D, L, GL = col(base), col(dark), col(light), col(glow)

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        streak = fbm(rng, w, 1, 3, 5)[0][None, :] if w > 1 else np.zeros((1, 1), np.float32)
        lum = np.clip(0.55 + 0.3 * (1 - v) + (np.broadcast_to(streak, u.shape) - 0.5) * 0.3, 0, 1)
        c = np.where((lum < 0.5)[..., None], D + (B - D) * (lum * 2)[..., None], B + (L - B) * ((lum - 0.5) * 2)[..., None])
        img, g = blank(w, h)
        img[..., :3] = c
        img[..., 3] = 255
        if stripe:
            s = np.abs(v - 0.5) * h < max(0.5, h * 0.06)
            img[..., :3] = np.where(s[..., None], img[..., :3] * 0.7, img[..., :3])
        add_glow(g, GL, np.full((h, w), glow_a, dtype=np.float32) * (np.clip(lum - 0.55, 0, 1) * 2.2))
        return _finish(img, g, face)
    return FnMat(fn, "gold")


def gem(seed, c="43B8FF", glow="9FEFFF", alpha=255):
    C = col(c)

    def fn(face, w, h, tag):
        u, v, x, y = grid(w, h)
        ang = np.arctan2(v - 0.5, u - 0.5)
        r = np.hypot(u - 0.5, v - 0.5) * 2
        facet = 0.5 + 0.5 * np.cos(ang * 3.0 + seed)
        img, g = blank(w, h)
        img[..., :3] = np.clip(C * (0.5 + 0.7 * facet[..., None]) * (1.0 - 0.3 * r[..., None]) + 255 * np.clip(0.35 - r, 0, 1)[..., None], 0, 255)
        img[..., 3] = alpha
        add_glow(g, col(glow), np.clip(0.95 - 0.5 * r, 0, 1))
        return img, g
    return FnMat(fn, "gem")


# ====================================================================================================================== wing feather
def wfeather(seed, root="F6FBFF", mid="BFE8FF", tip="5AB4FF", edge="FFFFFF", glow="BFF4FF", flip=False, glow_k=1.0, alpha=225, barb=1.0, gold_edge=None):
    """An angel's feather on a flat cube whose long axis is u (the quill is at u = 0 unless `flip`, then at u = 1): pearl-white at the quill, pale blue through the vane, a saturated
    glowing blue at the point, barbs slanting out from a bright quill, a rounded point. `gold_edge` (hex) outlines the vane in gold (the Saint Valkyrie's primaries)."""
    R, M, T, E, GL = col(root), col(mid), col(tip), col(edge), col(glow)
    GE = col(gold_edge) if gold_edge else None

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        t = (1 - u) if flip else u
        if face == "south":
            pass
        img, g = blank(w, h)
        lat = np.abs(v - 0.5) * 2.0                                   # 0 on the quill, 1 on the vane edge
        rise = smooth(t, 0.0, 0.34)
        fall = np.clip(1.0 - (np.clip(t - 0.34, 0, 1) / 0.66) ** 1.55, 0, 1) ** 0.75
        taper = (0.30 + 0.70 * rise) * fall
        inside = lat <= taper
        slant = (t * w * 0.55 + (v - 0.5) * h * 0.9 * np.where(v > 0.5, 1, -1) * 1.0) * barb
        barbs = 0.5 + 0.5 * np.sin(slant * 2.4 + fbm(rng, w, h, 2, 4) * 1.5)
        colr = _ramp(t, [(0.0, R), (0.45, M), (1.0, T)])
        lum = 0.80 + 0.22 * barbs - 0.18 * lat
        c = colr * lum[..., None]
        quill = lat < max(0.06, 0.9 / max(1, h))
        c = np.where(quill[..., None], E, c)
        rim = inside & (lat > taper - max(0.07, 1.2 / max(1, h))) & (lat > 0.2)
        c = np.where(rim[..., None], E * 0.92 + colr * 0.08, c)
        if GE is not None:
            c = np.where((rim & (t > 0.1))[..., None], GE, c)
        img[..., :3] = np.clip(c, 0, 255)
        img[..., 3] = np.where(inside, alpha, 0)
        a = (0.18 + 0.82 * t ** 1.3) * glow_k
        ga = np.clip(quill * 0.9 + rim * 1.0 + np.clip(barbs - 0.86, 0, 1) * 3.0 * 0.5, 0, 1) * a
        fill = (0.10 + 0.34 * t) * (0.7 + 0.3 * barbs) * glow_k                      # a faint glow over the whole vane: the feather never reads grey from behind
        add_glow(g, GL, np.maximum(ga, fill) * inside)
        return _finish(img, g, face, shade=False)
    return FnMat(fn, "wfeather")


# ====================================================================================================================== dragon scales and membrane
def dscale(seed, base="1F5FA8", dark="0A2450", light="7CC8F0", seam="46E4FF", belly="CFE8F2", cell=8.0, glow_k=0.8, belly_rows=False, sheen=None, alpha=255):
    """Rows of overlapping dragon scales (texels per scale = `cell`): each scale is a shield with a lit free edge, a dark gap between the scales, a soft gradient and a faint per-scale tone;
    a few scales glow along their lower edge. `belly_rows`: pale horizontal belly plates instead. `sheen` (hex) tints the highlights (the black form's blue)."""
    B, D, L, S, BL = col(base), col(dark), col(light), col(seam), col(belly)
    SH = col(sheen) if sheen else None

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        img, g = blank(w, h)
        cs = max(4.0, min(cell, w / 2.0))
        rowh = cs * 0.72
        row = np.floor(y / rowh)
        xo = (row % 2) * cs * 0.5
        cx = np.floor((x + xo) / cs)
        fx = ((x + xo) % cs) / cs
        fy = (y % rowh) / rowh
        d = np.hypot((fx - 0.5) / 0.60, (1.0 - fy) / 1.18)
        inside = d < 1.0
        tone = (hash2(cx, row, seed) - 0.5) * 0.12
        lum = np.clip(0.30 + 0.34 * fy + 0.22 * (1 - np.abs(fx - 0.5) * 2) * (1 - fy * 0.5) + tone, 0, 1)
        rim = inside & (d > 0.80)
        lum = np.where(rim, np.clip(lum + 0.30 * (d - 0.80) / 0.20 + 0.18, 0, 1), lum)
        c = np.where((lum < 0.5)[..., None], D + (B - D) * (lum * 2)[..., None], B + (L - B) * ((lum - 0.5) * 2)[..., None])
        if SH is not None:
            c = c + (SH - c) * (np.clip((lum - 0.55) * 1.6, 0, 1) * 0.55)[..., None]
        c = np.where(inside[..., None], c, D * 0.85)
        if belly_rows:
            ph = (y % (cs * 1.1)) / (cs * 1.1)
            plate = 0.62 + 0.38 * np.clip(1 - ph * 1.1, 0, 1)
            c = BL * plate[..., None]
            c = np.where((ph > 0.86)[..., None], BL * 0.45, c)
            inside = np.ones_like(inside)
        img[..., :3] = c
        img[..., 3] = alpha
        lit = (hash2(cx, row, seed + 3) > 0.55) & rim & (fy > 0.45)
        add_glow(g, S, lit * 0.75 * glow_k)
        add_glow(g, S, (inside & ~rim) * 0.06 * glow_k)
        return _finish(img, g, face)
    return FnMat(fn, "dscale")


def membrane(seed, base="2E8FD8", dark="0E2F66", light="BFEFFF", vein="CFF8FF", glow="6CE8FF", alpha=190, ragged=0.0, flip=False, glow_k=1.0, panels=3):
    """A dragon-wing membrane panel: u runs from the arm (0) to the trailing edge (1) unless flipped; v across. Veins fan out from the arm, the trailing edge is scalloped between the
    finger tips, ragged / torn when `ragged` > 0. Translucent."""
    B, D, L, V, GL = col(base), col(dark), col(light), col(vein), col(glow)

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        t = (1 - u) if flip else u
        img, g = blank(w, h)
        n = fbm(rng, w, h, 3, 4)
        veins = np.zeros_like(u)
        for k in range(panels + 1):
            vk = k / panels
            veins = np.maximum(veins, np.clip(1.0 - np.abs(v - (0.5 + (vk - 0.5) * (0.35 + 0.65 * t))) * h / 0.9, 0, 1))
        cross = np.clip(1.0 - np.abs(((t * 6.0) % 1.0) - 0.5) * w / 6.0 / 0.7, 0, 1) * 0.0
        scallop = 0.88 + 0.12 * np.cos((v - 0.5) * TAU * panels)
        edge = scallop - ragged * (0.35 * n + 0.25 * np.abs(np.sin(v * 9.0 + seed)))
        inside = t < edge
        lum = np.clip(0.35 + 0.45 * t + (n - 0.5) * 0.25, 0, 1)
        c = np.where((lum < 0.5)[..., None], D + (B - D) * (lum * 2)[..., None], B + (L - B) * ((lum - 0.5) * 2)[..., None])
        c = c + (V - c) * (veins * 0.85)[..., None]
        rim = inside & (t > edge - max(0.05, 1.3 / max(1, w)))
        c = np.where(rim[..., None], V, c)
        img[..., :3] = c
        img[..., 3] = np.where(inside, alpha * (0.55 + 0.45 * np.clip(1.4 - t, 0, 1)), 0)
        img[..., 3] = np.where(veins > 0.5, np.where(inside, 245, 0), img[..., 3])
        add_glow(g, GL, (veins * 0.85 + rim * 1.0) * inside * glow_k)
        return img, g
    return FnMat(fn, "membrane")


def horn(seed, base="EDE6D2", dark="6E6248", light="FFFFF0", glow="FFE8B0", glow_a=0.25, rings=7.0):
    B, D, L, GL = col(base), col(dark), col(light), col(glow)

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        img, g = blank(w, h)
        ax = v if h >= w else u                                            # the long axis of the cube's face
        ring = 0.5 + 0.5 * np.cos(ax * rings * TAU)
        lum = np.clip(0.45 + 0.35 * ring + 0.12 * (fbm(rng, w, h, 3, 3) - 0.5) - 0.2 * (ax if h >= w else 1 - ax) * 0.0, 0, 1)
        c = np.where((lum < 0.5)[..., None], D + (B - D) * (lum * 2)[..., None], B + (L - B) * ((lum - 0.5) * 2)[..., None])
        tipf = np.clip((ax - 0.75) * 4.0, 0, 1)[..., None]
        c = c * (1 - tipf * 0.0)
        img[..., :3] = c
        img[..., 3] = 255
        add_glow(g, GL, np.clip(ax - 0.6, 0, 1) * 2.0 * glow_a * (1.0 if True else 0))
        return _finish(img, g, face)
    return FnMat(fn, "horn")


def ink(seed, base="07090F", dark="020306", light="22304A", flame="2C7CFF", flame2="9CD6FF", glow_k=1.0, alpha=255, ragged=0.0):
    """Obsidian / ink black with a flame-blue sheen and a glowing edge; for the Black Dragon form's plates and tendrils."""
    B, D, L, F, F2 = col(base), col(dark), col(light), col(flame), col(flame2)

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        n = fbm(rng, w, h, 4, 3)
        img, g = blank(w, h)
        lum = np.clip(0.30 + 0.55 * n * n + 0.15 * (1 - v), 0, 1)
        c = np.where((lum < 0.5)[..., None], D + (B - D) * (lum * 2)[..., None], B + (L - B) * ((lum - 0.5) * 2)[..., None])
        vein = np.abs(n - 0.5) < 0.03
        c = np.where(vein[..., None], F, c)
        img[..., :3] = c
        a = np.ones_like(u) * alpha
        if ragged > 0:
            a = a * smooth(1.2 - v - (fbm(rng, w, h, 3, 5) - 0.5) * ragged * 1.4, 0.05, 0.3)
        img[..., 3] = a
        add_glow(g, F, vein * 0.9 * glow_k)
        add_glow(g, F2, (np.clip(n - 0.78, 0, 1) * 4.0) * 0.4 * glow_k)
        return _finish(img, g, face)
    return FnMat(fn, "ink")


def tailscale(t0, t1, seed, c0="8CD0F0", c1="2E86E0", c2="5CF0FF", cell=4.5, flame=False):
    """A tail segment's scales (from gen_undine_mermaid, recoloured): the colour runs from c0 (hip, t=0) through c1 to c2 (the fin, t=1)."""
    C0, C1, C2 = col(c0), col(c1), col(c2)
    CY = col("8CE8FF")

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        cs = max(3.0, cell)
        rowh = cs * 0.62
        row = np.floor(y / rowh)
        xo = (row % 2) * cs * 0.5
        fx = ((x + xo) % cs) / cs
        fy = (y % rowh) / rowh
        d = np.hypot((fx - 0.5) * 1.9, (1.0 - fy) * 1.15)
        body = np.clip(1.0 - d, 0, 1)
        edge = np.clip((d - 0.80) * 5.0, 0, 1) * (d < 1.12)
        tt = np.clip(t0 + (t1 - t0) * v, 0, 1)
        basec = _ramp(tt, [(0.0, C0), (0.5, C1), (1.0, C2)])
        lum = 0.98 + 0.30 * body - 0.20 * edge + (fbm(rng, w, h, 3, 4) - 0.5) * 0.10
        c = basec * lum[..., None]
        sheen = film(0.3 * u + 0.4 * v + seed * 0.11, sat=0.5, lo=0.45, hi=0.9)
        c = c * 0.86 + sheen * 0.14 * (0.5 + body[..., None])
        c = c + np.clip(body - 0.7, 0, 1)[..., None] * 80 * np.array([0.6, 0.9, 1.0])
        img, g = blank(w, h)
        img[..., :3] = c
        img[..., 3] = 255
        add_glow(g, CY, np.clip(body - 0.72, 0, 1) * 4.0 * (0.4 + 0.6 * tt) * (hash2(np.floor((x + xo) / cs), row, seed) > 0.32))
        return _finish(img, g, face)
    return FnMat(fn, "tailscale")


def waterband(seed, alpha=170, c0="5EB8F2", c1="E8FAFF", glow="8CE8FF", streaks=1.0):
    """Flowing, translucent water (cloth / veils): long soft streaks running down, a few glints, fading toward v = 1."""
    C0, C1, GL = col(c0), col(c1), col(glow)

    def fn(face, w, h, tag):
        rng = _rng(tag, face, seed)
        u, v, x, y = grid(w, h)
        n = fbm(rng, w, h, 3, 3)
        flow = 0.5 + 0.5 * np.sin(u * w * 0.55 * streaks + n * 4.0 + v * 2.0)
        ripple = 0.5 + 0.5 * np.sin(v * h * 0.22 * streaks + n * 5.0 + u * 3.0)
        lum = np.clip(0.40 + 0.28 * flow + 0.18 * ripple + 0.18 * (1 - v), 0, 1)
        c = C0 + (C1 - C0) * np.clip(lum - 0.30, 0, 1)[..., None] * 1.5
        glint = (fbm(rng, w, h, 4, 6) > 0.74) & (flow > 0.55)
        c = np.where(glint[..., None], col("F4FDFF"), c)
        img, g = blank(w, h)
        img[..., :3] = c
        img[..., 3] = np.where(glint, 235, alpha * (0.65 + 0.4 * flow)) * np.clip(1.5 * (1 - v) + 0.5, 0, 1)
        add_glow(g, GL, np.clip(flow - 0.55, 0, 1) * 0.9 + glint * 0.9)
        return img, g
    return FnMat(fn, "waterband")
