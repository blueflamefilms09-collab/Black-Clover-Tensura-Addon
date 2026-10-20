"""More ornaments for the genesis_weapons upgrade (star shards, drops, rune bands, armillary rings, scrollwork, eyes), on top of vu_geo."""
import math

import numpy as np

import vu_tex as tx
from vu_geo import *                                                       # noqa: F401,F403
from vu_geo import unit, basis, cone, catmull, ml


def bipyramid(p, c, axis, half_len, r, sides=4):
    """A two-ended crystal (a star shard) centred on c along axis."""
    ax = unit(axis)
    c = np.asarray(c, float)
    cone(p, c, c + ax * half_len, r, sides)
    cone(p, c, c - ax * half_len, r, sides)


def shard_helix(it, p, center, axis, radius, n, span, half_len, r, sides=4, phase=0.0, turns=1.0, tilt=0.0):
    """n bipyramids on a helix round `axis` through `center`: radius, total height `span`, each leaning by `tilt` radians off the axis (orbiting shards, a star halo)."""
    u, v, a = basis(axis)
    cnt = it.n(n, 3)
    for i in range(cnt):
        f = i / max(1, cnt - 1)
        ang = phase + 2 * math.pi * turns * f
        pos = np.asarray(center, float) + a * (span * (f - 0.5)) + radius * (math.cos(ang) * u + math.sin(ang) * v)
        d = unit(a * math.cos(tilt) + (math.cos(ang) * u + math.sin(ang) * v) * math.sin(tilt))
        bipyramid(p, pos, d, half_len, r, sides)


def teardrop(it, p, c, axis, length, r, sides=8):
    """A drop / wisp: round head, tapering tail along -axis."""
    ax = unit(axis)
    c = np.asarray(c, float)
    st = [(0.0, 0.0, 0.0), (length * 0.18, 0.35 * r, 0.35 * r), (length * 0.45, 0.8 * r, 0.8 * r), (length * 0.7, r, r), (length * 0.88, 0.7 * r, 0.7 * r), (length, 0.0, 0.0)]
    ml.loft(p, ax, c - ax * length * 0.75, st, ml.circle(1.0, it.n(sides, 4)), caps=(False, False))


def text_along(ctx, text, a, size, step, b0=0.0, b1=None, fname="DejaVuSans.ttf", rot=90.0):
    """A mask of `text` repeated along the length of a loft group (a rune band on a tube / ring) at across-position a."""
    b1 = ctx.length if b1 is None else b1
    m = np.zeros(ctx.shape, np.float32)
    b = b0 + step * 0.5
    i = 0
    while b < b1:
        m = np.maximum(m, tx.mask_text(ctx, text[i % len(text)], a, b, size, rot=rot, fname=fname))
        b += step
        i += 1
    return m


def spiral_scroll(it, p, c, r0, r1, turns, normal=(0, 0, 1), tube=0.3, z=0.0, n=40, start=0.0):
    """A flat spiral (a curl, a volute) of a swept tube: radius r0 shrinking or growing to r1."""
    u, v, nn = basis(normal)
    c = np.asarray(c, float)
    pts = []
    m = it.n(n, 6)
    for i in range(m + 1):
        t = i / m
        ang = start + turns * 2 * math.pi * t
        r = r0 + (r1 - r0) * t
        pts.append(tuple(c + r * (math.cos(ang) * u + math.sin(ang) * v) + nn * z))
    return ml.sweep(p, pts, ml.circle(1.0, it.n(6, 4)), scales=lambda t: (tube * (1.0 - 0.55 * t), tube * (1.0 - 0.55 * t)), caps=(False, False))


def rays(it, p, c, normal, n, r_in, r_out, width, thick=0.25, phase=0.0, long_every=2, long_k=1.6):
    """n flat pointed rays round a centre in the plane with this normal (eyelashes, a sunburst, a star): every long_every-th is longer."""
    u, v, nn = basis(normal)
    c = np.asarray(c, float)
    cnt = it.n(n, 4)
    for i in range(cnt):
        a = phase + 2 * math.pi * i / cnt
        d = math.cos(a) * u + math.sin(a) * v
        L = r_out * (long_k if (i % long_every == 0) else 1.0)
        base = c + d * r_in
        tip = c + d * L
        ml.loft(p, tip - base, base, [(0.0, 1.0, 1.0), (float(np.linalg.norm(tip - base)) * 0.985, 0.0, 0.0)], ml.bevelled(width, thick, 0.5), up=tuple(nn))


def section_marks(section, su=1.0, sv=1.0):
    """Where the landmarks of a closed section lie across a loft group's strip, as fractions 0..1 of its perimeter (the strip starts at the section's first point):
    tip0 = the first point, tip1 = the point farthest along u the other way, top / bot = where the section crosses u = 0 at v > 0 / v < 0."""
    pts = [(p[0] * su, p[1] * sv) for p in section]
    n = len(pts)
    seg = [math.hypot(pts[(i + 1) % n][0] - pts[i][0], pts[(i + 1) % n][1] - pts[i][1]) for i in range(n)]
    tot = sum(seg)
    cum = [0.0]
    for L in seg:
        cum.append(cum[-1] + L)
    marks = {"tip0": 0.0}
    mi = min(range(n), key=lambda i: pts[i][0])
    marks["tip1"] = cum[mi] / tot
    for i in range(n):
        a, b = pts[i], pts[(i + 1) % n]
        if (a[0] - 0.0) * (b[0] - 0.0) <= 0 and a[0] != b[0]:
            t = (0.0 - a[0]) / (b[0] - a[0])
            v = a[1] + (b[1] - a[1]) * t
            s = (cum[i] + seg[i] * t) / tot
            marks["top" if v > 0 else "bot"] = s
    return marks


def star_plate(it, p, c, normal, r_out, r_in=None, n=5, thick=0.35, phase=0.0, taper=0.6):
    """A flat n-pointed star (a plate with a domed back) centred on c facing along `normal`: 3 x n quads and the cap, instead of n separate rays."""
    r_in = r_out * 0.42 if r_in is None else r_in
    u, v, nn = basis(normal)
    ring = []
    for i in range(2 * n):
        a = phase + math.pi * i / n
        r = r_out if i % 2 == 0 else r_in
        ring.append((r * math.cos(a), r * math.sin(a)))
    sec = ml.resample(ring, 2 * n) if False else ring
    ml.loft(p, nn, np.asarray(c, float), [(0.0, 1.0, 1.0), (thick * 0.5, 0.97, 0.97), (thick, taper, taper)], sec, frame=(tuple(u), tuple(v)), caps=(True, True))


def perturb(part, amp, seed=0, freq=0.35, axes=(1, 1, 1)):
    """Hand-hewn roughness: moves every vertex of a part by a smooth noise vector (the same for coincident vertices, so the surface stays closed)."""
    rng = np.random.default_rng(seed)
    ph = rng.uniform(0, 2 * math.pi, (3, 3))
    for i, v in enumerate(part.V):
        d = np.array([math.sin(v[0] * freq + ph[0][0]) * math.cos(v[1] * freq * 1.3 + ph[0][1]) + math.sin(v[2] * freq * 0.8 + ph[0][2]),
                      math.sin(v[1] * freq + ph[1][0]) * math.cos(v[2] * freq * 1.1 + ph[1][1]) + math.sin(v[0] * freq * 0.9 + ph[1][2]),
                      math.sin(v[2] * freq + ph[2][0]) * math.cos(v[0] * freq * 1.2 + ph[2][1]) + math.sin(v[1] * freq * 0.7 + ph[2][2])])
        part.V[i] = v + d * 0.5 * amp * np.asarray(axes, float)
    part.UV = part.N = None
