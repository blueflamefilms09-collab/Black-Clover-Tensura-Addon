"""
FIRE remake (new round), PREVIEW ONLY: the 11 effect builders for the three directions.
  A  "anime cel"          baked flat-tone flame textures, ALPHA, flip-book every 2 ticks, hooked tongues (tutorial sheets + anime stills)
  B  "volumetric glow"    soft layered puffs with baked heat colour, additive halos, rays, embers
  C  "real 3D toon mesh"  authored geometry (tools/gen_fire_new_meshes.py), 3-tone toon + emissive core + rim light decided per quad, a few glow cards
Every builder takes (o: Out, F, T, p) and reads o.age / o.dur; the timelines copy the current layers (FireSpellLayer, FlameFxLayer, FlameMagicLayer) so the BEFORE tile
(the real Java layer) and the three new tiles are the same moment of the same spell.
"""
import math

import numpy as np

from fire_new_out import Out, WHITE, GLOW, HOTC, tint
from fire_new_raster import UP, V, norm, hsh, ease_out, ease_in, ease_back, clamp, lerp

PI = math.pi


def fr3(age, i=0):
    return "abc"[int(age // 2 + i) % 3]


def fr2(age, i=0):
    return "ab"[int(age // 2 + i) % 2]


def life(age, dur, fin, fout):
    a = 1.0 if fin <= 0 else clamp(age / fin)
    b = 1.0 if fout <= 0 else clamp((dur - age) / fout)
    return min(a, b)


def side_of(d):
    s = np.cross(d, UP)
    return norm(s) if np.linalg.norm(s) > 1e-6 else V(1, 0, 0)


def rot_about(v, axis, ang):
    axis = norm(axis)
    return v * math.cos(ang) + np.cross(axis, v) * math.sin(ang) + axis * np.dot(axis, v) * (1 - math.cos(ang))


def puff(i):
    return "B_puff_" + "abcd"[i % 4]


# ================================================================================================================ 1  FIRE_LION  Leo Rugiens
def lion_state(o, F, T, p):
    age, dur = o.age, o.dur
    path = T - F
    ln = np.linalg.norm(path)
    d = path / ln
    charge = dur * 0.65
    travel = ease_out(clamp(age / charge))
    arrived = clamp((age - charge) / (dur - charge))
    fade = life(age, dur, 3, 8)
    head = F + d * ln * travel + UP * 0.6 * p * math.sin(travel * PI)
    hs = 3.4 * p * (1 + 0.5 * arrived) * clamp(age / 4.0)
    body = min(ln * travel, 5.5 * p)
    tail = head - d * body - UP * 0.3 * p
    return d, travel, arrived, fade, head, hs, tail


def lion_A(o, F, T, p):
    d, travel, arrived, fade, head, hs, tail = lion_state(o, F, T, p)
    age = o.age
    o.bb(head - d * 0.4, hs * 1.7, hs * 1.7, "B_halo", col=tint(GLOW, 0.5 * fade), blend="ADD")
    if arrived > 0:
        o.bb(head + d * hs * 0.1, hs * 3.0, hs * 3.0, "A_rays", roll=age * 0.05, col=tint(WHITE, 0.9 * (1 - arrived) * fade))
    if np.linalg.norm(head - tail) > 0.2:
        o.strip(tail, head - d * hs * 0.2, 1.7 * p, "A_body_" + fr3(age), tint(WHITE, fade))
        o.strip(tail + UP * 0.35 * p, head - d * hs * 0.3 + UP * 0.2 * p, 1.0 * p, "A_body_" + fr3(age, 1), tint(WHITE, 0.95 * fade))
    for i in range(7):
        s = hsh(o.seed, i, 1)
        base = tail + (head - tail) * s + UP * 0.25 * p
        h = (1.5 + 1.2 * s) * p * (0.75 + 0.25 * math.sin(age * 0.8 + i * 1.7))
        o.upright(base, 1.0 * p, h, "A_tongue_" + fr3(age, i), tint(WHITE, fade), flip=(i % 2 == 0))
    o.dirbb(head, d, hs * 1.32, hs * 1.32 / 1.6, "A_lion_" + fr2(age), tint(WHITE, fade * (1 - arrived * 0.7)))
    if arrived > 0:
        o.bb(head + d * hs * 0.55 + UP * 0.1 * hs, hs * 0.9 * (1 + arrived), hs * 0.9 * (1 + arrived), "A_flash", col=tint(WHITE, (1 - arrived) * fade))
    for i in range(9):
        ph = hsh(o.seed, i, 5) * 12
        lf = ((age + ph) % 12) / 12
        q = tail + (head - tail) * hsh(o.seed, i, 6) + side_of(d) * (hsh(o.seed, i, 7) - 0.5) * 2.4 * p * lf + UP * lf * 1.8 * p
        o.bb(q, 0.28 * p * (1 - lf) + 0.05, 0.28 * p * (1 - lf) + 0.05, "A_chip_" + "ab"[i % 2], roll=age * 0.2 + i, col=tint(WHITE, fade * (1 - lf)))


def lion_B(o, F, T, p):
    d, travel, arrived, fade, head, hs, tail = lion_state(o, F, T, p)
    age = o.age
    for i in range(3):
        o.bb(tail + (head - tail) * (0.3 + 0.3 * i) + UP * 0.5, 2.2 * p, 1.6 * p, "B_smoke", col=(1, 1, 1, 0.45 * fade))
    o.bb(head - d * 0.2, hs * 2.2, hs * 2.2, "B_halo", col=tint(GLOW, 0.8 * fade), blend="ADD")
    if np.linalg.norm(head - tail) > 0.2:
        o.strip(tail, head, 2.2 * p, "B_streak", tint(WHITE, 0.9 * fade), blend="ALPHA")
        for i in range(8):
            s = (i + 0.5) / 8
            o.bb(tail + (head - tail) * s + UP * 0.2 * p, (0.9 + 1.5 * s) * p, (0.9 + 1.5 * s) * p, puff(i), roll=i * 1.3 + age * 0.05, col=tint(WHITE, 0.8 * fade * (0.3 + 0.7 * s)))
    sd, up = o.r, o.u
    for i in range(12):                                                                         # the mane: a ring of soft puffs round the head
        ang = 2 * PI * i / 12 + math.sin(age * 0.2 + i) * 0.06
        fl = 0.85 + 0.2 * math.sin(age * 0.9 + i * 1.7)
        c = head + (sd * math.cos(ang) + up * math.sin(ang)) * hs * 0.46
        o.bb(c, hs * 0.55 * fl, hs * 0.55 * fl, puff(i), roll=ang, col=tint(WHITE, 0.85 * fade))
    for i in range(3):
        o.bb(head + d * hs * (0.05 + 0.14 * i) + up * hs * (0.08 - 0.12 * i), hs * (0.72 - 0.12 * i), hs * (0.72 - 0.12 * i), puff(i + 1), roll=i, col=tint(WHITE, 0.95 * fade))
    o.bb(head + d * hs * 0.38 + up * hs * 0.0, hs * 0.55 * (1 + arrived), hs * 0.55 * (1 + arrived), "B_halo", col=tint(HOTC, (0.5 + 0.5 * arrived) * fade), blend="ADD")
    for i in range(11):
        ph = hsh(o.seed, i, 5) * 12
        lf = ((age + ph) % 12) / 12
        q = tail + (head - tail) * hsh(o.seed, i, 6) + side_of(d) * (hsh(o.seed, i, 7) - 0.5) * 2.4 * p * lf + UP * lf * 1.8 * p
        o.bb(q, 0.32 * p * (1 - lf) + 0.05, 0.32 * p * (1 - lf) + 0.05, "B_ember", col=tint(WHITE, fade * (1 - lf)), blend="ADD")


def lion_C(o, F, T, p):
    d, travel, arrived, fade, head, hs, tail = lion_state(o, F, T, p)
    age = o.age
    if fade < 0.04:
        return
    u = hs / 30.0 * clamp(fade * 3)
    sd = side_of(d)
    upv = norm(np.cross(sd, d))
    hot = 0.35 * (1 - arrived) if arrived > 0 else 0.0
    o.bb(head - d * 0.5, hs * 1.9, hs * 1.9, "B_halo", col=tint(GLOW, 0.65 * fade), blend="ADD")
    o.mesh("lion_head", head, d, xhint=sd, scale=(u, u, u), cool=0.15 * arrived, hot=hot)
    nm = 5
    for i in range(nm):                                                                         # the mane: five real flame tongues round the skull, streaming back
        ang = 2 * PI * (i + 0.5) / nm + 0.3
        rad = sd * math.cos(ang) + upv * math.sin(ang)
        fl = 0.92 + 0.16 * math.sin(age * 0.9 + i * 1.7)
        axis = norm(rad * 0.85 - d * 0.75 + norm(np.cross(d, rad)) * 0.35 * math.sin(age * 0.5 + i))
        o.mesh("tonguexs_" + fr3(age, i), head - d * hs * 0.10 + rad * hs * 0.15, axis, xhint=-d, scale=(u * 0.62 * fl, u * 0.7 * fl, u * 0.62 * fl))
    for i in range(2):                                                                          # the streaming body behind
        o.mesh("tonguexs_" + fr3(age, i + 3), head - d * hs * 0.5 + UP * (0.25 - 0.5 * i) * hs * 0.35, -d + UP * (0.25 - 0.5 * i) * 0.7, xhint=UP, scale=(u * 0.8, u * 1.4, u * 0.8))
    if arrived > 0:
        o.bb(head + d * hs * 0.5, hs * 0.9 * (1 + arrived), hs * 0.9 * (1 + arrived), "B_halo", col=tint(HOTC, (1 - arrived) * fade), blend="ADD")
    for i in range(2):
        ph = hsh(o.seed, i, 5) * 12
        lf = ((age + ph) % 12) / 12
        q = tail + (head - tail) * hsh(o.seed, i, 6) + UP * lf * 1.8 * p
        o.bb(q, 0.3 * p * (1 - lf) + 0.05, 0.3 * p * (1 - lf) + 0.05, "B_spark", roll=age * 0.3, col=tint(WHITE, fade * (1 - lf)), blend="ADD")


# ================================================================================================================ 2  FIRE_SPEAR  Sol Linea
def spear_state(o, F, T, p):
    age, dur = o.age, o.dur
    path = T - F
    ln = np.linalg.norm(path)
    d = path / ln
    travel = ease_out(clamp(age / (dur * 0.55)))
    fade = clamp((dur - age) / (dur * 0.45))
    headD = ln * travel
    trail = min(headD, 9.0)
    return d, fade, F + d * headD, F + d * (headD - trail), trail


def spear_A(o, F, T, p):
    d, fade, head, tail, trail = spear_state(o, F, T, p)
    age = o.age
    o.bb(head, 2.6 * p, 2.6 * p, "B_halo", col=tint(GLOW, 0.5 * fade), blend="ADD")
    o.strip(tail, head + d * 0.7 * p, 1.5 * p, "A_body_" + fr3(age), tint(WHITE, fade))
    o.strip(tail + d * trail * 0.25, head, 0.8 * p, "A_body_" + fr3(age, 1), tint(WHITE, 0.95 * fade), flip=False)
    for i in range(5):
        s = 0.25 + 0.14 * i
        o.upright(tail + (head - tail) * s + UP * 0.12 * p, 0.9 * p, (1.2 + 0.7 * s) * p, "A_tongue_" + fr3(age, i), tint(WHITE, fade), flip=(i % 2 == 1))
    o.bb(head + d * 0.2 * p, 1.5 * p, 1.5 * p, "A_ball_" + fr3(age), col=tint(WHITE, fade))
    o.bb(head + d * 0.9 * p, 1.4 * p, 1.4 * p, "A_flash", roll=age, col=tint(WHITE, fade))
    for i in range(7):
        s = hsh(o.seed, i, 1)
        dr = max(0.0, (age - s * o.dur * 0.55)) * 0.08
        q = F + d * np.linalg.norm(head - F) * s + side_of(d) * (hsh(o.seed, i, 2) - 0.5) * dr * 3 + UP * dr * 2
        o.bb(q, 0.22 * p, 0.22 * p, "A_chip_" + "ab"[i % 2], roll=age * 0.3, col=tint(WHITE, fade * clamp(1.5 - dr)))


def spear_B(o, F, T, p):
    d, fade, head, tail, trail = spear_state(o, F, T, p)
    age = o.age
    o.bb(head, 3.4 * p, 3.4 * p, "B_halo", col=tint(GLOW, 0.85 * fade), blend="ADD")
    o.strip(tail, head + d * 0.8 * p, 1.8 * p, "B_streak", tint(WHITE, 0.9 * fade))
    o.strip(tail, head + d * 0.8 * p, 0.7 * p, "B_streak", tint(HOTC, fade), blend="ADD")
    for i in range(6):
        s = (i + 0.5) / 6
        o.bb(tail + (head - tail) * s, (0.5 + 0.9 * s) * p, (0.5 + 0.9 * s) * p, puff(i), roll=i + age * 0.1, col=tint(WHITE, 0.8 * fade * (0.3 + 0.7 * s)))
    o.bb(head + d * 0.3 * p, 1.5 * p, 1.5 * p, puff(1), col=tint(WHITE, fade))
    o.bb(head + d * 0.3 * p, 1.1 * p, 1.1 * p, "B_halo", col=tint(HOTC, fade), blend="ADD")
    for i in range(10):
        s = hsh(o.seed, i, 1)
        dr = max(0.0, (age - s * o.dur * 0.55)) * 0.08
        q = F + d * np.linalg.norm(head - F) * s + side_of(d) * (hsh(o.seed, i, 2) - 0.5) * dr * 3 + UP * dr * 2
        o.bb(q, 0.24 * p, 0.24 * p, "B_ember", col=tint(WHITE, fade * clamp(1.5 - dr)), blend="ADD")


def spear_C(o, F, T, p):
    d, fade, head, tail, trail = spear_state(o, F, T, p)
    age = o.age
    sd = side_of(d)
    upv = norm(np.cross(sd, d))
    length = np.linalg.norm(head - tail) + 0.9 * p
    o.bb(head, 3.0 * p, 3.0 * p, "B_halo", col=tint(GLOW, 0.7 * fade), blend="ADD")
    sy = length / 64.0
    sw = 0.2 * p
    spin = age * 0.35
    xh = rot_about(sd, d, spin)
    o.mesh("lance", tail, d, xhint=xh, scale=(sw, sy, sw), hot=0.0, alpha=1.0)
    for i in range(4):                                                                          # flame tongues streaming back round the shaft
        s = 0.40 + 0.15 * i
        ang = spin * 0.5 + i * PI / 2
        rad = sd * math.cos(ang) + upv * math.sin(ang)
        base = tail + (head - tail) * s + rad * 0.12 * p
        axis = -d * 0.85 + rad * 0.45
        o.mesh("tonguexs_" + fr3(age, i), base, axis, xhint=rad, scale=(0.05 * p, 0.062 * p, 0.05 * p))
    o.mesh("gem", head + d * 0.9 * p, d, xhint=sd, scale=(0.09 * p, 0.09 * p, 0.09 * p), hot=0.4)
    o.bb(head + d * 0.9 * p, 1.2 * p, 1.2 * p, "B_spark", roll=age * 0.4, col=tint(WHITE, fade), blend="ADD")


# ================================================================================================================ 3  FIRE_PILLAR  Ignis Columna
def pillar_state(o, F, T, p):
    age, dur = o.age, o.dur
    rise = ease_back(clamp(age / 6.0))
    fade = clamp((dur - age) / 10.0)
    return 4.2 * p * rise, 0.85 * p, fade


def pillar_A(o, F, T, p):
    h, r, fade = pillar_state(o, F, T, p)
    age = o.age
    g = F + UP * 0.05
    o.ground(g, r * 2.0, "B_halo", col=tint(GLOW, 0.8 * fade), blend="ADD")
    o.ground(g + UP * 0.02, r * 1.55, "A_ring", spin=age * 0.03, col=tint(WHITE, fade))
    cfg = [(0.0, 1.0, 1.0), (-0.45, 0.85, 0.75), (0.45, 0.8, 0.8), (-0.22, 1.05, 0.6), (0.25, 0.95, 0.55)]
    for i, (ox, hk, wk) in enumerate(cfg):
        fl = 0.9 + 0.12 * math.sin(age * 0.8 + i * 2.1)
        o.upright(g + o.r * ox * r * 0.9 * V(1, 0, 1) * 0 + side_of(o.f) * ox * r, r * 2.5 * wk, h * hk * fl * 1.05, "A_tongue_" + fr3(age, i), tint(WHITE, fade * (1.0 if i == 0 else 0.92)), flip=(i % 2 == 1))
    for i in range(7):
        ang = hsh(o.seed, i, 1) * 2 * PI + age * 0.08
        ph = hsh(o.seed, i, 2) * 10
        lf = ((age + ph) % 10) / 10
        q = g + V(math.cos(ang) * r * 0.8, h * (0.2 + 0.8 * lf), math.sin(ang) * r * 0.8)
        o.upright(q - UP * 0.4 * r, r * 0.9 * (1 - lf * 0.5), r * 1.6 * (1 - lf * 0.5), "A_tongue_" + fr3(age, i + 1), tint(WHITE, fade * (1 - lf)))
    for i in range(9):
        ph = hsh(o.seed, i, 3) * 16
        lf = ((age + ph) % 16) / 16
        ang = hsh(o.seed, i, 4) * 2 * PI + lf * 3
        q = g + V(math.cos(ang) * r * (0.6 + lf), h * (0.5 + lf * 0.9), math.sin(ang) * r * (0.6 + lf))
        o.bb(q, 0.24 * p * (1 - lf) + 0.05, 0.24 * p * (1 - lf) + 0.05, "A_chip_" + "ab"[i % 2], roll=age * 0.2 + i, col=tint(WHITE, fade * (1 - lf)))


def pillar_B(o, F, T, p):
    h, r, fade = pillar_state(o, F, T, p)
    age = o.age
    g = F + UP * 0.05
    o.ground(g, r * 2.6, "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.ground(g + UP * 0.02, r * 1.6, "B_ring", spin=age * 0.05, col=tint(WHITE, fade), blend="ADD")
    for i in range(3):
        o.bb(g + UP * (h * (0.45 + 0.25 * i)), r * 2.2, r * 2.2, "B_smoke", col=(1, 1, 1, 0.4 * fade))
    for i in range(9):
        s = (i + 0.5) / 9
        sw = 0.25 * r * math.sin(age * 0.25 + i * 1.1) * s
        o.bb(g + V(sw, h * s * 0.95, 0), r * (2.1 - 1.1 * s), r * (2.1 - 1.1 * s) * 1.1, puff(i), roll=0.3 * i + age * 0.03, col=tint(WHITE, 0.75 * fade))
    for i, ox in enumerate((0.0, -0.4, 0.4)):
        o.upright(g + side_of(o.f) * ox * r, r * 1.5, h * (1.05 - 0.15 * abs(ox) * 2), "B_tongue_" + "ab"[i % 2], tint(WHITE, 0.95 * fade))
    o.bb(g + UP * h * 0.2, r * 1.6, r * 2.0, "B_halo", col=tint(HOTC, 0.6 * fade), blend="ADD")
    for i in range(14):
        ph = hsh(o.seed, i, 3) * 16
        lf = ((age + ph) % 16) / 16
        ang = hsh(o.seed, i, 4) * 2 * PI + lf * 3
        q = g + V(math.cos(ang) * r * (0.6 + lf), h * (0.5 + lf * 0.9), math.sin(ang) * r * (0.6 + lf))
        o.bb(q, 0.28 * p * (1 - lf) + 0.05, 0.28 * p * (1 - lf) + 0.05, "B_ember", col=tint(WHITE, fade * (1 - lf)), blend="ADD")


def pillar_C(o, F, T, p):
    h, r, fade = pillar_state(o, F, T, p)
    age = o.age
    if h < 0.1:
        return
    g = F + UP * 0.03
    o.ground(g, r * 2.4, "B_halo", col=tint(GLOW, 0.8 * fade), blend="ADD")
    k = clamp(fade * 2.5)
    o.mesh("ring", g, UP, xhint=V(1, 0, 0), scale=(r * 1.2 / 24 * 1.1, 0.09 * p, r * 1.2 / 24 * 1.1), cool=0.1)
    sway = rot_about(V(1, 0, 0), UP, age * 0.12)
    o.mesh("column", g, UP, xhint=sway, scale=(r / 6.0 * k, h / 64.0 * 1.04, r / 6.0 * k), hot=0.05)
    for i in range(4):
        ang = 2 * PI * i / 4 + age * 0.05 + 0.4
        rad = V(math.cos(ang), 0, math.sin(ang))
        fl = 0.9 + 0.15 * math.sin(age * 0.8 + i * 2.1)
        o.mesh("tonguelo_" + fr3(age, i), g + rad * r * 0.75, UP * 0.95 + rad * 0.32, xhint=rad, scale=(r / 7.0 * k, h * 0.42 / 32.0 * fl, r / 7.0 * k))
    for i in range(3):
        ph = hsh(o.seed, i, 3) * 16
        lf = ((age + ph) % 16) / 16
        ang = hsh(o.seed, i, 4) * 2 * PI + lf * 3
        o.bb(g + V(math.cos(ang) * r * (0.6 + lf), h * (0.5 + lf * 0.9), math.sin(ang) * r * (0.6 + lf)), 0.3 * p * (1 - lf) + 0.06, 0.3 * p * (1 - lf) + 0.06, "B_spark", roll=age * 0.3, col=tint(WHITE, fade * (1 - lf)), blend="ADD")


# ================================================================================================================ 4  FIRE_BURST  flame explosion
def burst_state(o, p):
    t = clamp(o.age / o.dur)
    e = ease_out(clamp(o.age / 8.0))
    return t, e, 1 - ease_in(t)


def burst_A(o, F, T, p):
    t, e, fade = burst_state(o, p)
    age = o.age
    c = F
    o.bb(c, 6.0 * p * e, 6.0 * p * e, "B_halo", col=tint(GLOW, 0.7 * fade), blend="ADD")
    o.bb(c, 6.4 * p * e, 6.4 * p * e, "A_rays", roll=age * 0.04, col=tint(WHITE, (1 - t) ** 1.5))
    r = 3.2 * p * e
    o.ground(c - UP * 0.4 * p, r * 1.05, "A_ring", spin=age * 0.05, col=tint(WHITE, fade))
    o.bb(c, 3.4 * p * (0.35 + 0.65 * e) * (1 - t * 0.35), 3.4 * p * (0.35 + 0.65 * e) * (1 - t * 0.35), "A_ball_" + fr3(age), col=tint(WHITE, fade))
    if age < 7:
        o.bb(c, 4.2 * p * (1 - age / 7), 4.2 * p * (1 - age / 7), "A_flash", roll=0.2, col=WHITE)
    n = 10
    for i in range(n):
        ang = 2 * PI * i / n + hsh(o.seed, i, 1) * 0.5
        dd = 2.4 * p * e * (0.7 + 0.5 * hsh(o.seed, i, 2))
        base = c + V(math.cos(ang) * dd, 1.4 * p * e - 2.5 * t * t * p, math.sin(ang) * dd) - UP * 0.6 * p
        o.upright(base, 1.2 * p * (1 - t * 0.5), 1.9 * p * (1 - t * 0.6), "A_tongue_" + fr3(age, i), tint(WHITE, fade), flip=(i % 2 == 0))
    for i in range(12):
        ang = hsh(o.seed, i, 4) * 2 * PI
        up = 0.5 + hsh(o.seed, i, 5)
        dd = 3.8 * p * e
        q = c + V(math.cos(ang) * dd, up * dd - 3 * t * t * p, math.sin(ang) * dd)
        o.bb(q, 0.3 * p, 0.3 * p, "A_chip_" + "ab"[i % 2], roll=age * 0.3 + i, col=tint(WHITE, fade))
    if t > 0.3:
        for i in range(3):
            st = (t - 0.3) / 0.7
            q = c + V((hsh(o.seed, i, 6) - 0.5) * 2 * p, st * 2.2 * p, (hsh(o.seed, i, 7) - 0.5) * 2 * p)
            o.bb(q, (1.4 + st * 1.6) * p, (1.4 + st * 1.6) * p, "A_smoke_" + "ab"[i % 2], col=tint(WHITE, 1 - st))


def burst_B(o, F, T, p):
    t, e, fade = burst_state(o, p)
    age = o.age
    c = F
    if t > 0.2:
        for i in range(4):
            st = (t - 0.2) / 0.8
            o.bb(c + V((hsh(o.seed, i, 6) - 0.5) * 2.4 * p, st * 2.4 * p + 0.6, (hsh(o.seed, i, 7) - 0.5) * 2.4 * p), (1.8 + st * 2.4) * p, (1.8 + st * 2.4) * p, "B_smoke", col=(1, 1, 1, 0.75 * (1 - st)))
    o.bb(c, 8.0 * p * e, 8.0 * p * e, "B_halo", col=tint(GLOW, 0.95 * fade), blend="ADD")
    o.ground(c - UP * 0.4 * p, 3.4 * p * e, "B_ring", spin=age * 0.05, col=tint(WHITE, fade), blend="ADD")
    for i in range(11):
        ang = 2 * PI * i / 11 + hsh(o.seed, i, 1) * 0.6
        dd = 1.9 * p * e * (0.6 + 0.6 * hsh(o.seed, i, 2))
        q = c + V(math.cos(ang) * dd, (0.4 + 1.1 * hsh(o.seed, i, 3)) * p * e - 1.5 * t * t * p, math.sin(ang) * dd)
        o.bb(q, 2.1 * p * (0.6 + 0.4 * e) * (1 - t * 0.5), 2.1 * p * (0.6 + 0.4 * e) * (1 - t * 0.5), puff(i), roll=ang, col=tint(WHITE, 0.85 * fade))
    o.bb(c, 3.6 * p * (0.4 + 0.6 * e), 3.6 * p * (0.4 + 0.6 * e), puff(1), col=tint(WHITE, fade))
    o.bb(c, 6.0 * p * e, 6.0 * p * e, "B_rays", roll=age * 0.03, col=tint(WHITE, (1 - t) ** 2), blend="ADD")
    o.bb(c, 2.2 * p * (1 - t), 2.2 * p * (1 - t), "B_halo", col=tint(HOTC, (1 - t) ** 1.5), blend="ADD")
    for i in range(16):
        ang = hsh(o.seed, i, 4) * 2 * PI
        up = 0.5 + hsh(o.seed, i, 5)
        dd = 3.8 * p * e
        q = c + V(math.cos(ang) * dd, up * dd - 3 * t * t * p, math.sin(ang) * dd)
        o.bb(q, 0.34 * p, 0.34 * p, "B_ember", col=tint(WHITE, fade), blend="ADD")


def burst_C(o, F, T, p):
    t, e, fade = burst_state(o, p)
    age = o.age
    c = F
    o.bb(c, 7.0 * p * e, 7.0 * p * e, "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.bb(c, 6.2 * p * e, 6.2 * p * e, "B_rays", roll=age * 0.03, col=tint(WHITE, (1 - t) ** 2), blend="ADD")
    k = clamp(fade * 2.2)
    r = 3.2 * p * e
    if e > 0.02:
        o.mesh("ring", c - UP * 0.4 * p, UP, xhint=V(1, 0, 0), scale=(r / 24 * 1.15, 0.08 * p, r / 24 * 1.15), cool=0.2 * t)
        sc = 0.17 * p * (0.35 + 0.65 * e) * (1 - t * 0.3) * k
        o.mesh("ball_s", c, UP, xhint=side_of(o.f), scale=(sc, sc, sc), hot=0.35 * (1 - t), cool=0.4 * t)
    n = 6
    for i in range(n):
        ang = 2 * PI * i / n + hsh(o.seed, i, 1) * 0.5
        dd = 2.4 * p * e * (0.7 + 0.5 * hsh(o.seed, i, 2))
        rad = V(math.cos(ang), 0, math.sin(ang))
        base = c + rad * dd * 0.55 + UP * (0.2 * p * e - 1.2 * t * t * p)
        axis = rad * 0.9 + UP * 0.55
        s = 0.1 * p * (1 - t * 0.5) * k
        o.mesh("tonguexs_" + fr3(age, i), base, axis, xhint=-rad, scale=(s, s * 1.2, s))
    if t > 0.3:
        st = (t - 0.3) / 0.7
        o.bb(c + UP * (st * 2.2 * p), (2.0 + st * 1.6) * p, (2.0 + st * 1.6) * p, "B_smoke", col=(1, 1, 1, 0.7 * (1 - st)))
    for i in range(3):
        ang = hsh(o.seed, i, 4) * 2 * PI
        dd = 3.8 * p * e
        o.bb(c + V(math.cos(ang) * dd, (0.5 + hsh(o.seed, i, 5)) * dd - 3 * t * t * p, math.sin(ang) * dd), 0.4 * p, 0.4 * p, "B_spark", roll=age * 0.3 + i, col=tint(WHITE, fade), blend="ADD")


# ================================================================================================================ 5  FIRE_SPIRAL  Spiral Flame
def spiral_state(o, F, T, p):
    age, dur = o.age, o.dur
    path = T - F
    ln = np.linalg.norm(path)
    d = path / ln
    travel = ease_out(clamp(age / (dur * 0.6)))
    fade = clamp((dur - age) / (dur * 0.4))
    headD = ln * travel
    sd = side_of(d)
    upv = norm(np.cross(sd, d))
    return d, sd, upv, headD, F + d * headD, fade, ln


def strand_pts(o, F, d, sd, upv, headD, p, strand, K):
    pts = []
    for k in range(K + 1):
        f = k / K
        phi = f * 9 - o.age * 0.9 + strand * 2 * PI / 3
        rad = (0.25 + 1.15 * f * f) * p
        pts.append(F + d * headD * f + sd * math.cos(phi) * rad + upv * math.sin(phi) * rad)
    return pts


def spiral_A(o, F, T, p):
    d, sd, upv, headD, head, fade, ln = spiral_state(o, F, T, p)
    age = o.age
    o.bb(head, 4.2 * p, 4.2 * p, "B_halo", col=tint(GLOW, 0.55 * fade), blend="ADD")
    o.strip(F, head, 0.9 * p, "A_body_" + fr3(age, 1), tint(WHITE, fade * 0.95))
    K = 6
    for s in range(3):                                                                          # cel flame sheets wrapped round the core: each one runs along the helix tangent
        pts = strand_pts(o, F, d, sd, upv, headD, p, s, K * 2)
        for k in range(K):
            a, b = pts[2 * k], pts[2 * k + 2]
            f = (k + 0.5) / K
            if np.linalg.norm(b - a) < 0.05:
                continue
            o.strip(a, b + (b - a) * 0.5, (0.6 + 0.9 * f) * p, "A_swoosh_" + fr2(age, s + k), tint(WHITE, fade * (0.6 + 0.4 * f)))
    o.bb(head + d * 0.5 * p, 2.7 * p, 2.7 * p, "A_ball_" + fr3(age), col=tint(WHITE, fade))
    o.bb(head + d * 1.2 * p, 2.0 * p, 2.0 * p, "A_flash", roll=age * 0.5, col=tint(WHITE, fade))
    for i in range(9):
        f = hsh(o.seed, i, 1)
        if f > ease_out(clamp(age / (o.dur * 0.6))):
            continue
        ang = hsh(o.seed, i, 2) * 2 * PI + age * 0.2
        rad = (0.4 + 1.2 * f + 0.6) * p
        q = F + d * ln * f + sd * math.cos(ang) * rad + upv * math.sin(ang) * rad
        o.bb(q, 0.26 * p, 0.26 * p, "A_chip_" + "ab"[i % 2], roll=age * 0.3 + i, col=tint(WHITE, fade))


def spiral_B(o, F, T, p):
    d, sd, upv, headD, head, fade, ln = spiral_state(o, F, T, p)
    age = o.age
    o.bb(head, 6.2 * p, 6.2 * p, "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.strip(F, head, 0.9 * p, "B_streak", tint(HOTC, 0.9 * fade), blend="ADD")
    K = 10
    for s in range(3):
        pts = strand_pts(o, F, d, sd, upv, headD, p, s, K)
        for k in range(K + 1):
            f = k / K
            o.bb(pts[k], (0.45 + 0.9 * f) * p, (0.45 + 0.9 * f) * p, puff(k + s), roll=k + s * 2 + age * 0.05, col=tint(WHITE, 0.75 * fade * (0.3 + 0.7 * f)))
    o.bb(head + d * 0.3 * p, 3.0 * p, 3.0 * p, puff(2), col=tint(WHITE, fade))
    o.bb(head + d * 0.3 * p, 1.8 * p, 1.8 * p, "B_halo", col=tint(HOTC, fade), blend="ADD")
    for i in range(12):
        f = hsh(o.seed, i, 1)
        if f > ease_out(clamp(age / (o.dur * 0.6))):
            continue
        ang = hsh(o.seed, i, 2) * 2 * PI + age * 0.2
        rad = (0.6 + 1.2 * f) * p
        o.bb(F + d * ln * f + sd * math.cos(ang) * rad + upv * math.sin(ang) * rad, 0.3 * p, 0.3 * p, "B_ember", col=tint(WHITE, fade), blend="ADD")


def spiral_C(o, F, T, p):
    d, sd, upv, headD, head, fade, ln = spiral_state(o, F, T, p)
    age = o.age
    o.bb(head, 4.6 * p, 4.6 * p, "B_halo", col=tint(GLOW, 0.7 * fade), blend="ADD")
    if headD > 0.5:
        xh = rot_about(sd, d, -age * 0.9)
        sw = p * 0.12 * (1.0 - 0.3 * (1 - fade))
        o.mesh("helix", F, d, xhint=xh, scale=(sw * 1.0, headD / 64.0, sw * 1.0), hot=0.05)
    sc = 0.11 * p
    o.mesh("gem", head + d * 0.55 * p, d, xhint=sd, scale=(sc, sc, sc), hot=0.35)
    for i in range(3):
        ang = 2 * PI * i / 3 + age * 0.5
        rad = sd * math.cos(ang) + upv * math.sin(ang)
        o.mesh("tonguexs_" + fr3(age, i), head - d * 0.3 * p + rad * 0.5 * p, -d * 0.7 + rad * 0.5, xhint=rad, scale=(0.05 * p, 0.062 * p, 0.05 * p))
    o.bb(head + d * 1.3 * p, 1.6 * p, 1.6 * p, "B_spark", roll=age * 0.5, col=tint(WHITE, fade), blend="ADD")


# ================================================================================================================ 6  FIRE_WILD  Wild Bursting Flame
def wild_waves(o, p):
    for w in range(3):
        local = o.age - w * 6
        t = clamp(local / 14.0)
        if local < 0 or t >= 1:
            continue
        yield w, t, ease_out(t), 1 - t


def wild_A(o, F, T, p):
    fade = clamp((o.dur - o.age) / 10.0)
    age = o.age
    c = F
    r = p
    rm = min(r, 3.0)
    o.bb(c + UP, 5.0, 5.0, "B_halo", col=tint(GLOW, 0.5 * fade * (0.7 + 0.3 * math.sin(age * 0.8))), blend="ADD")
    for w, t, e, wf in wild_waves(o, p):
        rr = r * (0.3 + 0.9 * e)
        o.ground(c + UP * 0.05, rr * 1.15, "A_ring", spin=age * 0.03 + w, col=tint(WHITE, fade * wf))
        n = 6
        for i in range(n):
            ang = 2 * PI * (i + 0.5 * w) / n + (hsh(o.seed, i + w * 31, 1) - 0.5) * 0.4
            lift = (hsh(o.seed, i + w * 31, 2) - 0.3) * 0.9
            out = norm(V(math.cos(ang), lift, math.sin(ang)))
            base = c + UP + out * r * 0.9 * e
            o.strip(base - out * (0.9 + 1.2 * wf) * rm, base, 0.7 * rm * (0.6 + 0.4 * wf), "A_body_" + fr3(age, i), tint(WHITE, fade * wf))
            o.bb(base + out * 0.1, 1.9 * rm * (0.7 + 0.3 * wf), 1.9 * rm * (0.7 + 0.3 * wf), "A_ball_" + fr3(age, i + w), col=tint(WHITE, fade * wf))
    for i in range(10):
        ph = hsh(o.seed, i, 4) * 14
        lt = ((age + ph) % 14) / 14
        ang = hsh(o.seed, i, 5) * 2 * PI
        dd = r * (0.3 + 1.0 * lt)
        o.bb(c + V(math.cos(ang) * dd, 1 + 2.2 * lt - 2.6 * lt * lt, math.sin(ang) * dd), 0.3, 0.3, "A_chip_" + "ab"[i % 2], roll=age * 0.3, col=tint(WHITE, fade * (1 - lt)))


def wild_B(o, F, T, p):
    fade = clamp((o.dur - o.age) / 10.0)
    age = o.age
    c = F
    r = p
    rm = min(r, 3.0)
    o.ground(c, r * 1.6, "B_halo", col=tint(GLOW, 0.6 * fade), blend="ADD")
    o.bb(c + UP, 6.0, 6.0, "B_halo", col=tint(GLOW, 0.7 * fade * (0.7 + 0.3 * math.sin(age * 0.8))), blend="ADD")
    for w, t, e, wf in wild_waves(o, p):
        rr = r * (0.3 + 0.9 * e)
        o.ground(c + UP * 0.05, rr * 1.15, "B_ring", spin=age * 0.03, col=tint(WHITE, fade * wf), blend="ADD")
        n = 8
        for i in range(n):
            ang = 2 * PI * (i + 0.5 * w) / n + (hsh(o.seed, i + w * 31, 1) - 0.5) * 0.4
            lift = (hsh(o.seed, i + w * 31, 2) - 0.3) * 0.9
            out = norm(V(math.cos(ang), lift, math.sin(ang)))
            base = c + UP + out * r * 0.9 * e
            o.strip(base - out * (1.4 + 2.4 * wf) * rm, base, 1.0 * rm * (0.6 + 0.4 * wf), "B_streak", tint(WHITE, 0.9 * fade * wf))
            o.bb(base, 1.8 * rm * (0.7 + 0.3 * wf), 1.8 * rm * (0.7 + 0.3 * wf), puff(i + w), roll=i, col=tint(WHITE, 0.9 * fade * wf))
            o.bb(base, 2.8 * rm * (0.7 + 0.3 * wf), 2.8 * rm * (0.7 + 0.3 * wf), "B_halo", col=tint(HOTC, 0.55 * fade * wf), blend="ADD")
    for i in range(14):
        ph = hsh(o.seed, i, 4) * 14
        lt = ((age + ph) % 14) / 14
        ang = hsh(o.seed, i, 5) * 2 * PI
        dd = r * (0.3 + 1.0 * lt)
        o.bb(c + V(math.cos(ang) * dd, 1 + 2.2 * lt - 2.6 * lt * lt, math.sin(ang) * dd), 0.34, 0.34, "B_ember", col=tint(WHITE, fade * (1 - lt)), blend="ADD")


def wild_C(o, F, T, p):
    fade = clamp((o.dur - o.age) / 10.0)
    age = o.age
    c = F
    r = p
    rm = min(r, 3.0)
    o.ground(c, r * 1.5, "B_halo", col=tint(GLOW, 0.55 * fade), blend="ADD")
    ws = list(wild_waves(o, p))[-2:]
    per = 3 if len(ws) > 1 else 6
    for w, t, e, wf in ws:
        rr = r * (0.3 + 0.9 * e)
        for i in range(per):
            ang = 2 * PI * (i + 0.5 * w) / per + (hsh(o.seed, i + w * 31, 1) - 0.5) * 0.3 + 0.35 * w
            lift = (hsh(o.seed, i + w * 31, 2) - 0.3) * 0.9
            out = norm(V(math.cos(ang), lift, math.sin(ang)))
            base = c + UP + out * r * 0.9 * e
            s = 0.085 * rm * (0.7 + 0.3 * wf)
            o.mesh("gem", base, out, xhint=UP, scale=(s, s, s), hot=0.3 * wf, cool=0.5 * t)
            ts = 0.05 * rm * (0.6 + 0.4 * wf)
            o.mesh("spike", base - out * 0.15 * rm, -out, xhint=UP, scale=(ts * 2.4, ts * 5.5 * (0.6 + 0.4 * wf), ts * 2.4), hot=0.2)
            o.bb(base, 2.6 * rm * (0.7 + 0.3 * wf), 2.6 * rm * (0.7 + 0.3 * wf), "B_halo", col=tint(GLOW, 0.5 * fade * wf), blend="ADD")
        if w == ws[0][0]:
            o.ground(c + UP * 0.05, rr * 1.15, "B_ring", spin=age * 0.03, col=tint(WHITE, fade * wf), blend="ADD")
    for i in range(3):
        ph = hsh(o.seed, i, 4) * 14
        lt = ((age + ph) % 14) / 14
        ang = hsh(o.seed, i, 5) * 2 * PI
        dd = r * (0.3 + 1.0 * lt)
        o.bb(c + V(math.cos(ang) * dd, 1 + 2.2 * lt - 2.6 * lt * lt, math.sin(ang) * dd), 0.4, 0.4, "B_spark", roll=age, col=tint(WHITE, fade * (1 - lt)), blend="ADD")


# ================================================================================================================ 7  FLAMEFX_CAST
def cast_state(o, F, T, p):
    age, dur = o.age, o.dur
    d = norm(T - F)
    fade = clamp((dur - age) / 6.0)
    run = clamp((age - dur * 0.12) / (dur * 0.7))
    head = ease_out(run)
    tail = max(0.0, head - 0.55)
    return d, fade, head, tail, F + (T - F) * head, F + (T - F) * tail, run


def cast_A(o, F, T, p):
    d, fade, head, tail, hp, tp, run = cast_state(o, F, T, p)
    age = o.age
    ch = clamp(age / (o.dur * 0.25))
    o.bb(F + d * 0.4, 1.6 * p * ease_back(ch), 1.6 * p * ease_back(ch), "B_halo", col=tint(GLOW, 0.7 * fade * (1 - run * 0.5)), blend="ADD")
    if head <= 0.001:
        return
    o.bb(hp, 3.0 * p, 3.0 * p, "B_halo", col=tint(GLOW, 0.5 * fade), blend="ADD")
    o.strip(tp, hp + d * 0.5 * p, 1.5 * p, "A_body_" + fr3(age), tint(WHITE, fade))
    o.strip(tp + d * 1.0, hp, 0.8 * p, "A_body_" + fr3(age, 1), tint(WHITE, 0.95 * fade))
    for i in range(3):
        s = 0.3 + 0.3 * i
        o.upright(tp + (hp - tp) * s + UP * 0.1, 0.8 * p, (1.1 + 0.4 * s) * p, "A_tongue_" + fr3(age, i), tint(WHITE, fade), flip=(i % 2 == 1))
    o.bb(hp + d * 0.3 * p, 1.6 * p, 1.6 * p, "A_ball_" + fr3(age), col=tint(WHITE, fade))
    for i in range(8):
        s = tail + (head - tail) * hsh(o.seed, i, 1)
        lf = (age * 0.07 + hsh(o.seed, i, 2)) % 1
        q = F + (T - F) * s + side_of(d) * (hsh(o.seed, i, 3) - 0.5) * 0.6 * p + UP * (hsh(o.seed, i, 4) * 0.5 * p + lf * 0.5)
        o.bb(q, 0.3 * p * (1 - lf) + 0.06, 0.3 * p * (1 - lf) + 0.06, "A_chip_" + "ab"[i % 2], roll=age * 0.3 + i, col=tint(WHITE, math.sin(lf * PI) * fade))
    if head > 0.88:
        k = (head - 0.88) / 0.12
        o.bb(T, 2.6 * p * k, 2.6 * p * k, "A_flash", roll=age * 0.1, col=tint(WHITE, (1 - 0.4 * k) * fade))


def cast_B(o, F, T, p):
    d, fade, head, tail, hp, tp, run = cast_state(o, F, T, p)
    age = o.age
    ch = clamp(age / (o.dur * 0.25))
    o.bb(F + d * 0.4, 2.6 * p * ease_back(ch), 2.6 * p * ease_back(ch), "B_halo", col=tint(GLOW, 0.85 * fade * (1 - run * 0.5)), blend="ADD")
    if head <= 0.001:
        return
    o.bb(hp, 4.6 * p, 4.6 * p, "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.strip(tp, hp + d * 0.6 * p, 1.7 * p, "B_streak", tint(WHITE, 0.9 * fade))
    o.strip(tp, hp + d * 0.6 * p, 0.7 * p, "B_streak", tint(HOTC, fade), blend="ADD")
    for i in range(6):
        s = (i + 0.5) / 6
        o.bb(tp + (hp - tp) * s, (0.5 + 0.9 * s) * p, (0.5 + 0.9 * s) * p, puff(i), roll=i + age * 0.1, col=tint(WHITE, 0.8 * fade * (0.3 + 0.7 * s)))
    o.bb(hp + d * 0.3 * p, 1.7 * p, 1.7 * p, puff(1), col=tint(WHITE, fade))
    o.bb(hp + d * 0.3 * p, 1.2 * p, 1.2 * p, "B_halo", col=tint(HOTC, fade), blend="ADD")
    for i in range(10):
        s = tail + (head - tail) * hsh(o.seed, i, 1)
        lf = (age * 0.07 + hsh(o.seed, i, 2)) % 1
        q = F + (T - F) * s + side_of(d) * (hsh(o.seed, i, 3) - 0.5) * 0.6 * p + UP * (hsh(o.seed, i, 4) * 0.5 * p + lf * 0.5)
        o.bb(q, 0.34 * p * (1 - lf) + 0.06, 0.34 * p * (1 - lf) + 0.06, "B_ember", col=tint(WHITE, math.sin(lf * PI) * fade), blend="ADD")
    if head > 0.88:
        k = (head - 0.88) / 0.12
        o.bb(T, 3.0 * p * k, 3.0 * p * k, "B_rays", roll=age * 0.1, col=tint(WHITE, (1 - 0.4 * k) * fade), blend="ADD")


def cast_C(o, F, T, p):
    d, fade, head, tail, hp, tp, run = cast_state(o, F, T, p)
    age = o.age
    sd = side_of(d)
    ch = clamp(age / (o.dur * 0.25))
    o.bb(F + d * 0.4, 2.0 * p * ease_back(ch), 2.0 * p * ease_back(ch), "B_halo", col=tint(GLOW, 0.75 * fade * (1 - run * 0.5)), blend="ADD")
    if head <= 0.001:
        return
    o.bb(hp, 3.8 * p, 3.8 * p, "B_halo", col=tint(GLOW, 0.8 * fade), blend="ADD")
    sc = 0.1 * p * clamp(fade * 3)
    o.mesh("ball_s", hp + d * 0.2 * p, d, xhint=sd, scale=(sc, sc, sc), hot=0.35)
    for i in range(3):
        off = (i - 1) * 0.5 * p
        ang = age * 0.25 + i * 2.1
        rad = sd * math.cos(ang) + UP * math.sin(ang)
        base = hp - d * 0.35 * p + rad * 0.3 * p
        L = max(0.5, np.linalg.norm(hp - tp) * 0.55)
        s = 0.05 * p
        o.mesh("tonguelo_" + fr3(age, i), base, -d + rad * 0.12, xhint=rad, scale=(s * 1.3, L / 32.0 * (0.85 + 0.15 * i), s * 1.3))
    o.bb(hp + d * 0.9 * p, 1.3 * p, 1.3 * p, "B_spark", roll=age * 0.3, col=tint(WHITE, fade), blend="ADD")
    if head > 0.88:
        k = (head - 0.88) / 0.12
        o.bb(T, 2.8 * p * k, 2.8 * p * k, "B_rays", roll=age * 0.1, col=tint(WHITE, (1 - 0.4 * k) * fade), blend="ADD")


# ================================================================================================================ 8  FLAMEFX_ZONE
def zone_state(o, p):
    age, dur = o.age, o.dur
    return ease_back(clamp(age / 10.0)), life(age, dur, 8, 12)


def zone_A(o, F, T, p):
    open_, fade = zone_state(o, p)
    age = o.age
    R = p
    g = F + UP * 0.06
    o.ground(g, R * 1.5 * open_, "B_halo", col=tint(GLOW, 0.55 * fade), blend="ADD")
    o.ground(g + UP * 0.02, R * 1.2 * open_, "A_ring", spin=-age * 0.03, col=tint(WHITE, fade))
    o.ground(g + UP * 0.03, R * 0.62 * open_, "A_ring", spin=age * 0.045, col=tint(WHITE, 0.85 * fade))
    n = 14
    for i in range(n):
        a = 2 * PI * i / n + hsh(o.seed, i, 1) * 0.2
        ph = hsh(o.seed, i, 2) * 2 * PI
        h = (0.55 + 0.35 * hsh(o.seed, i, 3)) * max(1.0, R * 0.35) * (0.8 + 0.2 * math.sin(age * 0.5 + ph)) * open_ * 1.5
        base = g + V(math.cos(a) * R * 0.98, 0, math.sin(a) * R * 0.98)
        o.upright(base, h * 0.8, h, "A_tongue_" + fr3(age, i), tint(WHITE, 0.95 * fade), flip=(i % 2 == 0))
    for i in range(10):
        lf = (age * 0.022 * (1 + 0.4 * hsh(o.seed, i, 5)) + hsh(o.seed, i, 6)) % 1
        a = hsh(o.seed, i, 7) * 2 * PI
        dd = math.sqrt(hsh(o.seed, i, 8)) * R
        o.bb(g + V(math.cos(a) * dd, 0.2 + lf * (1.2 + R * 0.3), math.sin(a) * dd), 0.24 * (1 - lf) + 0.06, 0.24 * (1 - lf) + 0.06, "A_chip_" + "ab"[i % 2], roll=age * 0.2, col=tint(WHITE, math.sin(lf * PI) * fade))


def zone_B(o, F, T, p):
    open_, fade = zone_state(o, p)
    age = o.age
    R = p
    g = F + UP * 0.06
    o.ground(g, R * 1.7 * open_, "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.ground(g + UP * 0.02, R * 1.15 * open_, "B_ring", spin=-age * 0.03, col=tint(WHITE, 0.95 * fade * (0.85 + 0.15 * math.sin(age * 0.3))), blend="ADD")
    n = 16
    for i in range(n):
        a = 2 * PI * i / n + hsh(o.seed, i, 1) * 0.2
        ph = hsh(o.seed, i, 2) * 2 * PI
        h = (0.55 + 0.35 * hsh(o.seed, i, 3)) * max(1.0, R * 0.35) * (0.8 + 0.2 * math.sin(age * 0.5 + ph)) * open_ * 1.5
        base = g + V(math.cos(a) * R * 0.98, 0, math.sin(a) * R * 0.98)
        o.upright(base, h * 0.9, h, "B_tongue_" + "ab"[i % 2], tint(WHITE, 0.9 * fade))
        o.bb(base + UP * h * 0.3, h * 1.0, h * 1.0, puff(i), roll=i, col=tint(WHITE, 0.55 * fade))
    for i in range(14):
        lf = (age * 0.022 * (1 + 0.4 * hsh(o.seed, i, 5)) + hsh(o.seed, i, 6)) % 1
        a = hsh(o.seed, i, 7) * 2 * PI
        dd = math.sqrt(hsh(o.seed, i, 8)) * R
        o.bb(g + V(math.cos(a) * dd, 0.2 + lf * (1.2 + R * 0.3), math.sin(a) * dd), 0.3 * (1 - lf) + 0.06, 0.3 * (1 - lf) + 0.06, "B_ember", col=tint(WHITE, math.sin(lf * PI) * fade), blend="ADD")


def zone_C(o, F, T, p):
    open_, fade = zone_state(o, p)
    age = o.age
    R = p
    g = F + UP * 0.04
    o.ground(g, R * 1.6 * open_, "B_halo", col=tint(GLOW, 0.8 * fade), blend="ADD")
    if open_ < 0.02:
        return
    k = clamp(fade * 3)
    o.mesh("ring", g, UP, xhint=rot_about(V(1, 0, 0), UP, -age * 0.03), scale=(R * open_ / 24.0, 0.1 * R * open_, R * open_ / 24.0), cool=0.05)
    n = 6
    for i in range(n):
        a = 2 * PI * i / n + 0.2
        ph = hsh(o.seed, i, 2) * 2 * PI
        fl = 0.85 + 0.2 * math.sin(age * 0.5 + ph)
        rad = V(math.cos(a), 0, math.sin(a))
        s = 0.046 * max(1.0, R * 0.35) * 1.5 * open_ * fl * k
        o.mesh("tonguexs_" + fr3(age, i), g + rad * R * 0.94 * open_, UP * 0.95 + rad * 0.2, xhint=-rad if i % 2 else rad, scale=(s * 1.2, s * 1.3, s * 1.2))
    for i in range(3):
        lf = (age * 0.022 * (1 + 0.4 * hsh(o.seed, i, 5)) + hsh(o.seed, i, 6)) % 1
        a = hsh(o.seed, i, 7) * 2 * PI
        dd = math.sqrt(hsh(o.seed, i, 8)) * R
        o.bb(g + V(math.cos(a) * dd, 0.2 + lf * (1.2 + R * 0.3), math.sin(a) * dd), 0.34 * (1 - lf) + 0.06, 0.34 * (1 - lf) + 0.06, "B_spark", roll=age * 0.2, col=tint(WHITE, math.sin(lf * PI) * fade), blend="ADD")


# ================================================================================================================ 9  FLAMEFX_BURST
def fxburst_state(o, p):
    t = clamp(o.age / o.dur)
    return t, 1 - ease_in(t), clamp(1 - o.age / 7.0)


def fxburst_A(o, F, T, p):
    t, fade, flash = fxburst_state(o, p)
    age = o.age
    c = F + UP * 0.6
    o.bb(c, 3.4 * p * (0.5 + ease_out(t)), 3.4 * p * (0.5 + ease_out(t)), "B_halo", col=tint(GLOW, 0.6 * fade), blend="ADD")
    sz = (1.3 + 2.4 * ease_out(clamp(t * 2))) * p
    o.bb(c, sz * 2.0, sz * 2.0, "A_rays", roll=age * 0.04, col=tint(WHITE, 0.9 if t < 0.45 else 0.0))
    if age < 5:                                                                                 # cel style: the white flash pops in at full strength and out again, no half-transparent star
        o.bb(c, sz * 1.15, sz * 1.15, "A_flash", roll=age * 0.05, col=WHITE)
    rr = (0.4 + 2.4 * ease_out(t)) * p
    o.ground(F + UP * 0.07, rr, "A_ring", spin=age * 0.08, col=tint(WHITE, 0.95 * fade))
    n = 10
    for i in range(n):
        a = 2 * PI * i / n + hsh(o.seed, i, 1) * 0.4
        reach = (0.9 + 0.9 * hsh(o.seed, i, 2)) * p
        up = (0.5 + 0.7 * hsh(o.seed, i, 3)) * p
        e = ease_out(clamp(t * 1.5))
        tip = F + V(math.cos(a) * reach * e, 0.1 + up * math.sin(clamp(t * 1.4) * PI), math.sin(a) * reach * e)
        base = F + V(math.cos(a) * 0.15 * p, 0.1, math.sin(a) * 0.15 * p)
        o.upright(base + (tip - base) * 0.3, 0.75 * p * (1 - t * 0.5), max(0.3, (tip[1] - base[1]) + 0.6) * 1.25, "A_tongue_" + fr3(age, i), tint(WHITE, fade), flip=(i % 2 == 0), lean=0.0)
    for i in range(10):
        a = hsh(o.seed, i, 4) * 2 * PI
        v = (1.2 + 2.4 * hsh(o.seed, i, 5)) * p
        vy = (1.4 + 2.2 * hsh(o.seed, i, 6)) * p
        tt = t * 1.2
        o.bb(F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.6 * tt * tt * p, math.sin(a) * v * tt * 0.7), 0.3 * p * (1 - t) + 0.08, 0.3 * p * (1 - t) + 0.08, "A_chip_" + "ab"[i % 2], roll=age * 0.3 + i, col=tint(WHITE, fade))
    if t > 0.3:
        for i in range(3):
            a = hsh(o.seed, i, 8) * 2 * PI
            st = (t - 0.3) / 0.7
            dd = (0.2 + 0.8 * hsh(o.seed, i, 9)) * p * ease_out(t)
            o.bb(F + V(math.cos(a) * dd, 0.5 + t * 1.2 * p, math.sin(a) * dd), (1.0 + 1.2 * t) * p, (1.0 + 1.2 * t) * p, "A_smoke_" + "ab"[i % 2], col=tint(WHITE, 0.9 * math.sin(st * PI)))


def fxburst_B(o, F, T, p):
    t, fade, flash = fxburst_state(o, p)
    age = o.age
    c = F + UP * 0.6
    for i in range(4):
        a = hsh(o.seed, i, 8) * 2 * PI
        dd = (0.2 + 0.8 * hsh(o.seed, i, 9)) * p * ease_out(t)
        o.bb(F + V(math.cos(a) * dd, 0.5 + t * 1.2 * p, math.sin(a) * dd), (1.2 + 1.6 * t) * p, (1.2 + 1.6 * t) * p, "B_smoke", col=(1, 1, 1, 0.8 * math.sin(clamp(t * 1.2) * PI)))
    sz = (1.3 + 2.4 * ease_out(clamp(t * 2))) * p
    o.bb(c, 4.0 * p * (0.5 + ease_out(t)), 4.0 * p * (0.5 + ease_out(t)), "B_halo", col=tint(GLOW, 0.95 * fade), blend="ADD")
    o.ground(F + UP * 0.07, (0.4 + 2.4 * ease_out(t)) * p, "B_ring", spin=age * 0.08, col=tint(WHITE, fade), blend="ADD")
    for i in range(12):
        a = 2 * PI * i / 12 + hsh(o.seed, i, 1) * 0.4
        e = ease_out(clamp(t * 1.5))
        reach = (0.7 + 0.8 * hsh(o.seed, i, 2)) * p * e
        o.bb(F + V(math.cos(a) * reach, 0.4 * p + (0.5 + 0.7 * hsh(o.seed, i, 3)) * p * math.sin(clamp(t * 1.4) * PI), math.sin(a) * reach), 1.1 * p * (1 - t * 0.5), 1.1 * p * (1 - t * 0.5), puff(i), roll=a, col=tint(WHITE, 0.85 * fade))
    o.bb(c, sz * 1.3, sz * 1.3, puff(0), col=tint(WHITE, fade))
    o.bb(c, sz * 2.0, sz * 2.0, "B_rays", roll=age * 0.04, col=tint(WHITE, min(1.0, 0.4 + flash) * fade), blend="ADD")
    o.bb(c, sz * 0.8 * (0.5 + flash), sz * 0.8 * (0.5 + flash), "B_halo", col=tint(HOTC, min(1.0, 0.3 + flash) * fade), blend="ADD")
    for i in range(14):
        a = hsh(o.seed, i, 4) * 2 * PI
        v = (1.2 + 2.4 * hsh(o.seed, i, 5)) * p
        vy = (1.4 + 2.2 * hsh(o.seed, i, 6)) * p
        tt = t * 1.2
        o.bb(F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.6 * tt * tt * p, math.sin(a) * v * tt * 0.7), 0.3 * p * (1 - t) + 0.08, 0.3 * p * (1 - t) + 0.08, "B_ember", col=tint(WHITE, fade), blend="ADD")


def fxburst_C(o, F, T, p):
    t, fade, flash = fxburst_state(o, p)
    age = o.age
    c = F + UP * 0.6
    sz = (1.3 + 2.4 * ease_out(clamp(t * 2))) * p
    o.bb(c, 4.0 * p * (0.5 + ease_out(t)), 4.0 * p * (0.5 + ease_out(t)), "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.bb(c, sz * 2.0, sz * 2.0, "B_rays", roll=age * 0.04, col=tint(WHITE, min(1.0, 0.4 + flash) * fade), blend="ADD")
    k = clamp(fade * 2.2)
    rr = (0.4 + 2.4 * ease_out(t)) * p
    o.mesh("ring", F + UP * 0.05, UP, xhint=rot_about(V(1, 0, 0), UP, age * 0.08), scale=(rr / 24.0, 0.07 * p, rr / 24.0), cool=0.25 * t)
    sc = 0.1 * p * (0.6 + 0.8 * ease_out(clamp(t * 2.5))) * k
    o.mesh("gem", c, UP, xhint=side_of(o.f), scale=(sc, sc, sc), hot=0.5 * flash + 0.1)
    n = 6
    for i in range(n):
        a = 2 * PI * i / n + hsh(o.seed, i, 1) * 0.4
        reach = (0.9 + 0.9 * hsh(o.seed, i, 2)) * p
        up = (0.5 + 0.7 * hsh(o.seed, i, 3)) * p
        e = ease_out(clamp(t * 1.5))
        rad = V(math.cos(a), 0, math.sin(a))
        base = F + rad * reach * e * 0.5 + UP * (0.15 + up * 0.35 * math.sin(clamp(t * 1.4) * PI))
        s = 0.07 * p * (1 - t * 0.5) * k
        o.mesh("tonguexs_" + fr3(age, i), base, rad * 0.9 + UP * 0.6, xhint=-rad, scale=(s, s * 1.2, s))
    for i in range(3):
        a = hsh(o.seed, i, 4) * 2 * PI
        v = (1.2 + 2.4 * hsh(o.seed, i, 5)) * p
        vy = (1.4 + 2.2 * hsh(o.seed, i, 6)) * p
        tt = t * 1.2
        o.bb(F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.6 * tt * tt * p, math.sin(a) * v * tt * 0.7), 0.42 * p, 0.42 * p, "B_spark", roll=age * 0.3 + i, col=tint(WHITE, fade), blend="ADD")


# ================================================================================================================ 10  FLAME_TRAIL
def trail_state(o, F, T, p):
    t = clamp(o.age / o.dur)
    head = ease_out(clamp(t / 0.75))
    tail = max(0.0, ease_in(clamp((t - 0.2) / 0.8)))
    alpha = clamp(t / 0.05) * clamp((1 - t) / 0.3)
    return t, F + (T - F) * head, F + (T - F) * tail, alpha, norm(T - F)


def trail_A(o, F, T, p):
    t, hp, tp, alpha, d = trail_state(o, F, T, p)
    age = o.age
    if np.linalg.norm(hp - tp) < 0.1:
        return
    o.bb(hp, 2.2 * p, 2.2 * p, "B_halo", col=tint(GLOW, 0.5 * alpha), blend="ADD")
    o.strip(tp, hp + d * 0.5 * p, 1.2 * p, "A_body_" + fr3(age), tint(WHITE, alpha))
    o.strip(tp + (hp - tp) * 0.4, hp, 0.6 * p, "A_body_" + fr3(age, 1), tint(WHITE, alpha))
    o.bb(hp + d * 0.3 * p, 1.3 * p, 1.3 * p, "A_ball_" + fr3(age), col=tint(WHITE, alpha))
    for i in range(9):
        al = hsh(o.seed, i, 1)
        lf = (age * 0.05 + hsh(o.seed, i, 2)) % 1
        q = tp + (hp - tp) * al + side_of(d) * (hsh(o.seed, i, 3) - 0.5) * p + UP * lf * 1.2 * p
        o.bb(q, 0.24 * (1 - lf) + 0.05, 0.24 * (1 - lf) + 0.05, "A_chip_" + "ab"[i % 2], roll=age * 0.3, col=tint(WHITE, alpha * (1 - lf)))


def trail_B(o, F, T, p):
    t, hp, tp, alpha, d = trail_state(o, F, T, p)
    age = o.age
    if np.linalg.norm(hp - tp) < 0.1:
        return
    o.bb(hp, 3.6 * p, 3.6 * p, "B_halo", col=tint(GLOW, 0.9 * alpha), blend="ADD")
    o.strip(tp, hp + d * 0.5 * p, 1.4 * p, "B_streak", tint(WHITE, 0.9 * alpha))
    o.strip(tp, hp + d * 0.5 * p, 0.5 * p, "B_streak", tint(HOTC, alpha), blend="ADD")
    for i in range(7):
        s = (i + 0.5) / 7
        o.bb(tp + (hp - tp) * s, (0.4 + 0.8 * s) * p, (0.4 + 0.8 * s) * p, puff(i), roll=i + age * 0.1, col=tint(WHITE, 0.8 * alpha * (0.3 + 0.7 * s)))
    o.bb(hp, 1.4 * p, 1.4 * p, puff(2), col=tint(WHITE, alpha))
    o.bb(hp, 1.0 * p, 1.0 * p, "B_halo", col=tint(HOTC, alpha), blend="ADD")
    for i in range(12):
        al = hsh(o.seed, i, 1)
        lf = (age * 0.05 + hsh(o.seed, i, 2)) % 1
        q = tp + (hp - tp) * al + side_of(d) * (hsh(o.seed, i, 3) - 0.5) * p + UP * lf * 1.2 * p
        o.bb(q, 0.26 * (1 - lf) + 0.05, 0.26 * (1 - lf) + 0.05, "B_ember", col=tint(WHITE, alpha * (1 - lf)), blend="ADD")


def trail_C(o, F, T, p):
    t, hp, tp, alpha, d = trail_state(o, F, T, p)
    age = o.age
    if np.linalg.norm(hp - tp) < 0.1:
        return
    sd = side_of(d)
    k = clamp(alpha * 2.5)
    o.bb(hp, 3.0 * p, 3.0 * p, "B_halo", col=tint(GLOW, 0.8 * alpha), blend="ADD")
    sc = 0.09 * p * k
    o.mesh("gem", hp + d * 0.1, d, xhint=sd, scale=(sc, sc, sc), hot=0.4)
    L = np.linalg.norm(hp - tp)
    for i in range(3):
        ang = age * 0.3 + i * 2.1
        rad = sd * math.cos(ang) + UP * math.sin(ang)
        s = 0.04 * p * k
        o.mesh("tonguelo_" + fr3(age, i), hp - d * (0.2 + 0.25 * i) + rad * 0.12 * p, -d + rad * 0.1, xhint=rad, scale=(s * 1.3, min(L, 6.0) * (0.55 - 0.1 * i) / 32.0, s * 1.3))
    o.mesh("lance", tp, d, xhint=sd, scale=(0.07 * p, max(0.1, L) / 64.0, 0.07 * p), hot=0.4)
    o.bb(hp + d * 0.5, 1.0 * p, 1.0 * p, "B_spark", roll=age * 0.4, col=tint(WHITE, alpha), blend="ADD")


# ================================================================================================================ 11  FLAME_EXPLOSION
def expl_state(o, p):
    t = clamp(o.age / o.dur)
    size = (ease_out(clamp(t / 0.35)) * 0.88 + 0.12 * t) * 3.2 * p
    return t, size, 1 - ease_in(t)


def expl_A(o, F, T, p):
    t, size, fade = expl_state(o, p)
    age = o.age
    c = T + UP * 0.2
    o.bb(c, size * 1.7, size * 1.7, "B_halo", col=tint(GLOW, 0.75 * fade), blend="ADD")
    o.ground(T - UP * 0.4 * p, size * (0.4 + t * 0.6) * 1.2, "A_ring", spin=age * 0.04, col=tint(WHITE, 0.8 * fade))
    if age < 9:
        o.bb(c, size * 1.7, size * 1.7, "A_rays", roll=0.3, col=tint(WHITE, 1 - age / 9))
    pos = [(0, 0, 0, 1.0), (0.45, 0.25, 0.1, 0.66), (-0.5, 0.15, 0.2, 0.6), (0.2, 0.55, -0.1, 0.55), (-0.25, -0.2, 0.35, 0.5), (0.1, -0.35, -0.2, 0.46)]
    for i, (x, y, z, k) in enumerate(pos):
        dd = 0.45 * size
        q = c + (o.r * x + o.u * y + o.f * -0.3 * z) * dd * (0.5 + 0.5 * ease_out(clamp(t * 3)))
        o.bb(q, size * 1.25 * k, size * 1.25 * k, "A_ball_" + fr3(age, i), col=tint(WHITE, fade))
    for i in range(8):
        yaw = hsh(o.seed, i, 1) * 2 * PI
        dd = size * 0.55 * (0.7 + 0.4 * hsh(o.seed, i, 2)) * ease_out(clamp(t * 2))
        base = T + V(math.cos(yaw) * dd, -0.3 + 0.3 * hsh(o.seed, i, 3), math.sin(yaw) * dd)
        o.upright(base, 1.0 * p * (1 - t * 0.5), 1.7 * p * (1 - t * 0.5), "A_tongue_" + fr3(age, i), tint(WHITE, fade), flip=(i % 2 == 0))
    if age < 5:
        o.bb(c, size * 1.2, size * 1.2, "A_flash", roll=0.2, col=tint(WHITE, 1 - age / 5))
    for i in range(10):
        yaw = hsh(o.seed, i, 6) * 2 * PI
        up = hsh(o.seed, i, 7)
        dd = size * ease_out(t) * (0.8 + hsh(o.seed, i, 8))
        o.bb(c + V(math.cos(yaw) * dd, up * dd - t * t * 2, math.sin(yaw) * dd), 0.3 * p, 0.3 * p, "A_chip_" + "ab"[i % 2], roll=yaw + age * 0.2, col=tint(WHITE, fade))
    if t > 0.35:
        for i in range(3):
            st = (t - 0.35) / 0.65
            o.bb(c + V((hsh(o.seed, i, 1) - 0.5) * 2 * p, 0.8 + st * 2.0 * p, (hsh(o.seed, i, 2) - 0.5) * 2 * p), (1.3 + 1.6 * st) * p, (1.3 + 1.6 * st) * p, "A_smoke_" + "ab"[i % 2], col=tint(WHITE, 0.9 * (1 - st)))


def expl_B(o, F, T, p):
    t, size, fade = expl_state(o, p)
    age = o.age
    c = T + UP * 0.2
    if t > 0.2:
        for i in range(4):
            st = (t - 0.2) / 0.8
            o.bb(c + V((hsh(o.seed, i, 1) - 0.5) * 2 * p, 0.6 + st * 2.4 * p, (hsh(o.seed, i, 2) - 0.5) * 2 * p), (1.6 + 2.0 * st) * p, (1.6 + 2.0 * st) * p, "B_smoke", col=(1, 1, 1, 0.8 * (1 - st)))
    o.bb(c, size * 2.4, size * 2.4, "B_halo", col=tint(GLOW, 1.0 * fade), blend="ADD")
    o.ground(T - UP * 0.4 * p, size * (0.4 + t * 0.6) * 1.2, "B_ring", spin=age * 0.04, col=tint(WHITE, 0.8 * fade), blend="ADD")
    for i in range(16):
        yaw = hsh(o.seed, i, 1) * 2 * PI
        pitch = (hsh(o.seed, i, 2) - 0.5) * PI
        dd = size * 0.42 * (0.6 + 0.4 * hsh(o.seed, i, 3))
        q = c + V(math.cos(yaw) * math.cos(pitch), math.sin(pitch), math.sin(yaw) * math.cos(pitch)) * dd
        o.bb(q, size * 0.62 * (1 - t * 0.5), size * 0.62 * (1 - t * 0.5), puff(i), roll=yaw + age * 0.05, col=tint(WHITE, 0.8 * fade))
    o.bb(c, size * 0.95, size * 0.95, puff(0), col=tint(WHITE, fade))
    o.bb(c, size * 0.8, size * 0.8, "B_halo", col=tint(HOTC, fade * 0.9), blend="ADD")
    if age < 9:
        o.bb(c, size * 1.9, size * 1.9, "B_rays", roll=0.3, col=tint(WHITE, 1 - age / 9), blend="ADD")
    for i in range(16):
        yaw = hsh(o.seed, i, 6) * 2 * PI
        up = hsh(o.seed, i, 7)
        dd = size * ease_out(t) * (0.8 + hsh(o.seed, i, 8))
        o.bb(c + V(math.cos(yaw) * dd, up * dd - t * t * 2, math.sin(yaw) * dd), 0.32 * p, 0.32 * p, "B_ember", col=tint(WHITE, fade), blend="ADD")


def expl_C(o, F, T, p):
    t, size, fade = expl_state(o, p)
    age = o.age
    c = T + UP * 0.2
    k = clamp(fade * 2.2)
    o.bb(c, size * 2.2, size * 2.2, "B_halo", col=tint(GLOW, 0.9 * fade), blend="ADD")
    o.ground(T - UP * 0.4 * p, size * (0.4 + t * 0.6) * 1.2, "B_ring", spin=age * 0.04, col=tint(WHITE, 0.8 * fade), blend="ADD")
    if age < 9:
        o.bb(c, size * 1.8, size * 1.8, "B_rays", roll=0.3, col=tint(WHITE, 1 - age / 9), blend="ADD")
    sd = side_of(o.f)
    sc = size / 17.0 * k
    o.mesh("ball_s", c, UP, xhint=sd, scale=(sc, sc, sc), hot=0.35 * (1 - t), cool=0.4 * t)
    pos = [(0.55, 0.3, 0.25), (-0.6, 0.2, 0.3), (0.15, 0.62, 0.25), (-0.15, -0.45, 0.3)]
    for i, (x, y, kk) in enumerate(pos):
        q = c + (o.r * x + o.u * y) * size * 0.55 * ease_out(clamp(t * 3))
        s = sc * kk * 1.15
        o.mesh("gem", q, norm(o.r * x + o.u * y + 0.3 * UP), xhint=sd, scale=(s, s, s), hot=0.2 * (1 - t), cool=0.5 * t)
    for i in range(4):
        yaw = 2 * PI * i / 4 + 0.5 + hsh(o.seed, i, 1)
        dd = size * 0.62 * ease_out(clamp(t * 2.0))
        rad = V(math.cos(yaw), 0, math.sin(yaw))
        s = 0.055 * p * (1 - t * 0.4) * k
        o.mesh("tonguexs_" + fr3(age, i), c + rad * dd * 0.7 + UP * (-0.5 + 0.3 * i), rad * 0.7 + UP * 0.9, xhint=-rad, scale=(s * 1.2, s * 1.4, s * 1.2))
    if t > 0.35:
        st = (t - 0.35) / 0.65
        o.bb(c + UP * (0.8 + st * 2.0 * p), (1.8 + 1.6 * st) * p, (1.8 + 1.6 * st) * p, "B_smoke", col=(1, 1, 1, 0.8 * (1 - st)))
    for i in range(3):
        yaw = hsh(o.seed, i, 6) * 2 * PI
        up = hsh(o.seed, i, 7)
        dd = size * ease_out(t) * (0.8 + hsh(o.seed, i, 8))
        o.bb(c + V(math.cos(yaw) * dd, up * dd - t * t * 2, math.sin(yaw) * dd), 0.45 * p, 0.45 * p, "B_spark", roll=yaw + age * 0.2, col=tint(WHITE, fade), blend="ADD")


# ================================================================================================================ the scene table
SCENES = [
    dict(key="lion", title="FIRE_LION  Leo Rugiens", layer="FireSpellLayer", shape="FIRE_LION", F=V(0, 1.2, 0), T=V(0, 1.2, 16), p=1.2, dur=34, cam=(9, 3, 4), target=(0, 1.5, 9),
         ages=[6, 14, 26], mid=1, figures=[[0, 0, -1, 1.8], [0, 0, 16.5, 1.8]], sky=[0.25, 0.22, 0.3], horizon=[0.55, 0.45, 0.42], cap=264, gen=(lion_A, lion_B, lion_C),
         still="Fuegoleon_Leo_Rugiens (lion head, open jaw, white-hot mouth) + Fuegoleon_easily_defeats_Carl (manga lion)"),
    dict(key="spear", title="FIRE_SPEAR  Sol Linea", layer="FireSpellLayer", shape="FIRE_SPEAR", F=V(0, 1.6, 0), T=V(0, 1.6, 20), p=1.0, dur=16, cam=(6, 2.5, 3), target=(0, 1.5, 9),
         ages=[3, 6, 11], mid=1, figures=[[0, 0, -0.5, 1.8]], cap=196, gen=(spear_A, spear_B, spear_C),
         still="Burst_Javelin (flame ridge with hooked cel tongues along a lance)"),
    dict(key="pillar", title="FIRE_PILLAR  Ignis Columna", layer="FireSpellLayer", shape="FIRE_PILLAR", F=V(0, 0, 0), T=V(0, 1, 0), p=1.0, dur=30, cam=(0, 2.0, -7), target=(0, 2, 0),
         ages=[3, 10, 22], mid=1, figures=[[2.5, 0, 1, 1.8]], sky=[0.25, 0.22, 0.3], cap=220, gen=(pillar_A, pillar_B, pillar_C),
         still="Magna_burns_his_pies (tall flame columns) + tutorial column / twister (b669a565) + 2d2ffc50 pillar sprites"),
    dict(key="burst", title="FIRE_BURST  flame explosion", layer="FireSpellLayer", shape="FIRE_BURST", F=V(0, 0.8, 0), T=V(0, 0.8, 0), p=1.0, dur=22, cam=(0, 2.0, -7), target=(0, 1, 0),
         ages=[2, 7, 15], mid=1, figures=[[2.5, 0, 1, 1.8]], cap=244, gen=(burst_A, burst_B, burst_C),
         still="Magna_Exploding_Fireball (white-yellow ball, radial speed lines, swirling orange ring)"),
    dict(key="spiral", title="FIRE_SPIRAL  Spiral Flame", layer="FireSpellLayer", shape="FIRE_SPIRAL", F=V(0, 1.6, 0), T=V(0, 1.6, 18), p=1.2, dur=22, cam=(10, 3.5, 5.4), target=(0, 1.5, 9.9),
         ages=[4, 9, 15], mid=1, figures=[[0, 0, -1, 1.8], [0, 0, 18.5, 1.8]], cap=268, gen=(spiral_A, spiral_B, spiral_C),
         still="Leopold_spiral_flame (wide yellow-white swirling flame sheets) + Grand_Slam (spiral ribbons round a white core)"),
    dict(key="wild", title="FIRE_WILD  Wild Bursting Flame", layer="FireSpellLayer", shape="FIRE_WILD", F=V(0, 0, 0), T=V(0, 1, 0), p=5.0, dur=30, cam=(0, 5, -13), target=(0, 1, 0),
         ages=[4, 10, 18], mid=1, figures=[[0, 0, 0, 1.8]], cap=288, gen=(wild_A, wild_B, wild_C),
         still="Leopold_wild_bursting_flame (round fireballs and long light beams in every direction)"),
    dict(key="cast", title="FLAMEFX_CAST  Flame book cast", layer="FlameFxLayer", shape="FLAMEFX_CAST", F=V(0, 1.2, 0), T=V(0, 1.2, 10), p=1.0, dur=24, cam=(-5.0, 2.4, 4.0), target=(0, 1.2, 5),
         ages=[5, 9, 14], mid=1, figures=[[0, 0, -0.6, 1.8]], sky=[0.3, 0.34, 0.44], cap=180, gen=(cast_A, cast_B, cast_C),
         still="Calidus_Brachium (yellow punch burst along the arm) + tutorial comet / flame tail (b669a565)"),
    dict(key="zone", title="FLAMEFX_ZONE  Flame zone", layer="FlameFxLayer", shape="FLAMEFX_ZONE", F=V(0, 0, 6), T=V(0, 1, 6), p=4.0, dur=80, cam=(-6.0, 4.0, -1.0), target=(0, 0.8, 6),
         ages=[8, 16, 40], mid=2, figures=[[0, 0, 0, 1.8], [3, 0, 6, 1.8]], sky=[0.3, 0.34, 0.44], cap=156, gen=(zone_A, zone_B, zone_C),
         still="Calidus_Brachium_Purgatory (concentric fire arcs, blue in the still: recolour) + tutorial ring of fire (b669a565)"),
    dict(key="fxburst", title="FLAMEFX_BURST  Flame burst", layer="FlameFxLayer", shape="FLAMEFX_BURST", F=V(0, 0, 6), T=V(0, 1, 6), p=1.4, dur=28, cam=(-5.0, 2.6, 0.0), target=(0, 1.0, 6),
         ages=[3, 6, 10], mid=1, figures=[[0, 0, 0, 1.8]], sky=[0.3, 0.34, 0.44], cap=216, gen=(fxburst_A, fxburst_B, fxburst_C),
         still="Calidus_Brachium_Barrage (fat radial yellow spokes from a white centre)"),
    dict(key="trail", title="FLAME_TRAIL  Explosion/Magma stream", layer="FlameMagicLayer", shape="FLAME_TRAIL", F=V(0, 1.2, 0), T=V(0, 1.2, 12), p=1.0, dur=24, cam=(-7.0, 2.8, 5.0), target=(0, 1.2, 7),
         ages=[4, 10, 16], mid=1, figures=[[0, 0, -0.6, 1.8]], sky=[0.3, 0.34, 0.44], cap=164, gen=(trail_A, trail_B, trail_C),
         still="Magna's_Vanishing_Fireball (fireball with a straight streak) + tutorial comet (b669a565)"),
    dict(key="explosion", title="FLAME_EXPLOSION  Explosion/Magma blast", layer="FlameMagicLayer", shape="FLAME_EXPLOSION", F=V(0, 0.8, 0), T=V(0, 0.8, 0), p=1.0, dur=26, cam=(0, 2.0, -7), target=(0, 1, 0),
         ages=[3, 8, 14, 22], mid=1, figures=[[2.5, 0, 1, 1.8]], cap=292, gen=(expl_A, expl_B, expl_C),
         still="Combo_Eruption (cluster of round cel fireballs with red-orange rims)"),
]
