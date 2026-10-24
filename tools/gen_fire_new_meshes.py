#!/usr/bin/env python3
"""
FIRE remake (new round), direction C "real 3D toon meshes", PREVIEW ONLY.  Nothing here is registered in the game and nothing goes to assets/.

The flame pieces are REAL geometry (flame tongues with thickness, a low-poly fireball, a lion head, a helix, a column ...), authored in the project's mesh pipeline: the same `Mesh`
class and the loft vocabulary of tools/gen_sdw3_models.py (imported, not copied), the same text format that `SpiritDiveWind3Mesh` reads:

  q <layer> (x y z u v) x 4  nx ny nz  r g b        r g b = the HEAT tint of the quad (gold at the root, red at the tip), NOT yet shaded
  layers: 0 solid body (toon shaded when drawn), 1 soft glow shell (additive), 2 emissive ornaments (drawn flat: white-hot mouth, eye ...)

Units are model pixels (1/16 block). Mesh space: y runs along the piece (root at 0, tip at +y), x across it (a tongue curls to +x), z through it.

    python tools/gen_fire_new_meshes.py     # deterministic; writes docs/previews/vfx_remake/fire_new/meshes/<name>.txt and meshes.txt (the quad / vertex counts)
"""
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_sdw3_models import Mesh, WHITE, add, cross, mul, norm, sub  # noqa: E402  (the repo's own mesh authoring helpers)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "fire_new", "meshes")

GOLD = (255, 156, 26)
ORANGE = (238, 84, 16)
RED = (186, 32, 10)
HOT = (255, 248, 214)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def heat(t, hot=0.0):
    """root gold -> orange -> red tip"""
    c = lerp(GOLD, ORANGE, min(1.0, t * 1.6)) if t < 0.625 else lerp(ORANGE, RED, (t - 0.625) / 0.375)
    return lerp(c, HOT, hot) if hot else c


def frame_of(T, hint=(0.0, 0.0, 1.0)):
    T = norm(T)
    s = cross(T, hint)
    if s[0] ** 2 + s[1] ** 2 + s[2] ** 2 < 1e-6:
        s = cross(T, (1.0, 0.0, 0.0))
    s = norm(s)
    z = norm(cross(s, T))
    return s, z


def section(c, side, z, w, th, n, phase=0.0):
    pts = []
    for k in range(n):
        a = 2 * math.pi * k / n + phase
        pts.append(add(c, add(mul(side, w * math.cos(a)), mul(z, th * math.sin(a)))))
    return pts


def tube(m, path, widths, thick, n, hint=(0.0, 0.0, 1.0), phase=0.0, hot=0.0, tip=True):
    """a lofted tube along `path` (list of 3D points); widths / thick per path point; the last section collapses to the tip point"""
    secs = []
    for i, c in enumerate(path):
        T = sub(path[min(i + 1, len(path) - 1)], path[max(i - 1, 0)])
        side, z = frame_of(T, hint)
        if i == len(path) - 1 and tip:
            secs.append([c] * n)
        else:
            secs.append(section(c, side, z, widths[i], thick[i], n, phase))
    S = len(path) - 1
    for i in range(S):
        m.loft(0, [secs[i], secs[i + 1]], heat((i + 0.5) / S, hot), WHITE)
    return secs


def tongue(name, L=32.0, W=9.0, curl=0.55, S=4, n=4, seed=1, jag=0.0, hot=0.0, twist=0.0, wave=0.6):
    """one flame tongue: root at the origin, tip at +y.  An S-shaped lick (`wave`) that ends in a hook toward +x (`curl`), a belly low down and a pointed tip, thick like a real flame
    (the section is nearly round), so it reads as a lick of fire and not as a thorn."""
    r = random.Random(seed)
    ph = r.random() * 6.28
    m = Mesh(name)
    path, wd, th = [], [], []
    for i in range(S + 1):
        t = i / S
        x = curl * L * 0.50 * t ** 2.2 + wave * L * 0.13 * math.sin(2 * math.pi * 0.85 * t + ph) * (1 - 0.35 * t) + jag * L * 0.05 * math.sin(t * 7.1 + ph)
        z = jag * L * 0.06 * math.cos(t * 4.1 + ph) * t
        y = L * t * (1 - 0.16 * t)
        path.append((x, y, z))
        w = W * (0.30 + 0.70 * math.sin(min(1.0, t * 2.4) * math.pi / 2)) * (1 - t) ** 0.85
        wd.append(w)
        th.append(w * 0.95)
    tube(m, path, wd, th, n, phase=math.pi / n + twist, hot=hot)
    return m


def ball(name, R=9.0, n=6, J=4, seed=3, jit=0.12):
    """low-poly fireball: poles on y, J bands of n quads, radially jittered so every facet catches the light differently"""
    r = random.Random(seed)
    m = Mesh(name)
    secs = []
    for j in range(J + 1):
        phi = math.pi * j / J
        if j in (0, J):
            secs.append([(0.0, R * math.cos(phi) * (1 + 0.0), 0.0)] * n)
            continue
        ring = []
        for k in range(n):
            a = 2 * math.pi * k / n + 0.5 * j
            rad = R * math.sin(phi) * (1 + jit * (r.random() * 2 - 1))
            ring.append((rad * math.cos(a), R * math.cos(phi) * (1 + 0.5 * jit * (r.random() * 2 - 1)), rad * math.sin(a)))
        secs.append(ring)
    for i in range(J):
        m.loft(0, [secs[i], secs[i + 1]], heat(0.15 + 0.5 * i / J, 0.18), WHITE)
    return m


def spike(name, L=14.0, W=2.6, curl=0.3):
    return tongue(name, L=L, W=W, curl=curl, S=1, n=3, seed=7)


def column(name, L=64.0, R=7.0, n=5, S=5, seed=11):
    """the flame column of Ignis Columna: a wobbling tube that bulges low, narrows and flicks sideways at the top"""
    r = random.Random(seed)
    m = Mesh(name)
    path, wd, th = [], [], []
    for i in range(S + 1):
        t = i / S
        x = 3.2 * math.sin(t * 5.1) * t + 9.0 * t ** 3
        z = 2.2 * math.cos(t * 4.3) * t
        path.append((x, L * t, z))
        w = R * (1 - t) ** 0.7 * (0.65 + 0.35 * math.sin(min(1.0, t * 2.6) * math.pi / 2)) * (1 + 0.1 * math.sin(t * 13))
        wd.append(w)
        th.append(w)
    tube(m, path, wd, th, n, phase=0.4, hot=0.12)
    return m


def helix(name, L=64.0, R0=1.2, R1=6.0, turns=2.25, S=9, n=3, strands=2):
    """two intertwined flame strands round the y axis, widening toward +y (the head end); the preview spins the mesh about y"""
    m = Mesh(name)
    for sd in range(strands):
        path, wd, th = [], [], []
        for i in range(S + 1):
            t = i / S
            rr = R0 + (R1 - R0) * t ** 1.3
            a = turns * 2 * math.pi * t + sd * 2 * math.pi / strands
            path.append((rr * math.cos(a), L * t, rr * math.sin(a)))
            w = (1.1 + 1.9 * t ** 0.8) * (1 - 0.9 * max(0.0, t - 0.88) / 0.12)
            wd.append(w)
            th.append(w * 0.8)
        tube(m, path, wd, th, n, hint=(0.0, 1.0, 0.0), phase=0.3, hot=0.1 + 0.15 * sd)
    return m


def lance(name, L=64.0):
    """Sol Linea / javelin: a slender diamond lance with a pointed head, the shaft is bright; flame tongues are added in the scene along it"""
    m = Mesh(name)
    path = [(0.0, 0.0, 0.0), (0.0, L * 0.30, 0.0), (0.0, L * 0.74, 0.0), (0.0, L * 0.9, 0.0), (0.0, L, 0.0)]
    wd = [1.4, 2.0, 2.4, 3.4, 0.0]
    th = [1.4, 2.0, 2.4, 3.0, 0.0]
    tube(m, path, wd, th, 4, phase=math.pi / 4, hot=0.35)
    return m


def ring(name, R=24.0, N=8, w=3.4, h=1.6):
    """flat ground ring of fire: N segments, each a top band and an outer wall (two quads); y is up"""
    m = Mesh(name)
    for k in range(N):
        a0, a1 = 2 * math.pi * k / N, 2 * math.pi * (k + 1) / N
        c0, s0, c1, s1 = math.cos(a0), math.sin(a0), math.cos(a1), math.sin(a1)
        ri, ro = R - w, R
        top = [(ri * c0, h, ri * s0), (ro * c0, h, ro * s0), (ro * c1, h, ro * s1), (ri * c1, h, ri * s1)]
        wall = [(ro * c0, h, ro * s0), (ro * c0, 0.0, ro * s0), (ro * c1, 0.0, ro * s1), (ro * c1, h, ro * s1)]
        ref = ((ri + ro) / 2 * math.cos((a0 + a1) / 2), h * 0.5, (ri + ro) / 2 * math.sin((a0 + a1) / 2))
        m.quad(0, top, [(0, 0), (1, 0), (1, 1), (0, 1)], heat(0.2, 0.25), ref=(ref[0], -1.0, ref[2]))
        m.quad(0, wall, [(0, 0), (1, 0), (1, 1), (0, 1)], heat(0.5), ref=(ref[0] * 0.6, h * 0.5, ref[2] * 0.6))
    return m


def lion_head(name):
    """Leo Rugiens: a real low-poly lion head, +y = forward (the nose), x across, z up; skull + muzzle (3 bands of 4), a lower jaw dropped open (2 bands), a white-hot mouth quad, two fangs, two eye slits.
    The mane is not in this mesh: the scene plants flame tongues round it (they are the mane and the trail)."""
    m = Mesh(name)
    def sec(y, w, ztop, zbot):
        return [(-w, y, (ztop + zbot) * 0.5), (0.0, y, ztop), (w, y, (ztop + zbot) * 0.5), (0.0, y, zbot)]
    S = [sec(-9, 9.0, 12.0, -4.0), sec(0, 9.6, 13.0, -3.4), sec(8, 7.0, 8.0, -2.6), sec(13, 5.4, 4.6, -2.4)]
    for i in range(3):
        m.loft(0, [S[i], S[i + 1]], heat(0.22 + 0.1 * i, 0.3), WHITE)
    m.loft(0, [S[3], [(0.0, 14.6, 0.4)] * 4], heat(0.4, 0.2), WHITE)                       # the nose cap
    ang = math.radians(34)
    ca, sa = math.cos(ang), math.sin(ang)
    def jaw_pt(p):
        y, z = p[1] + 2, p[2] + 0.5
        return (p[0], -2 + y * ca + z * sa, -0.5 - y * sa + z * ca)
    J = [[(-6.0, -2.0, -1.0), (0.0, -2.0, 1.4), (6.0, -2.0, -1.0), (0.0, -2.0, -3.6)],
         [(-5.2, 6.0, -1.0), (0.0, 6.0, 1.0), (5.2, 6.0, -1.0), (0.0, 6.0, -3.2)],
         [(-4.0, 13.0, -1.0), (0.0, 13.0, 0.4), (4.0, 13.0, -1.0), (0.0, 13.0, -2.6)]]
    J = [[jaw_pt(p) for p in s] for s in J]
    for i in range(2):
        m.loft(0, [J[i], J[i + 1]], heat(0.3 + 0.1 * i, 0.25), WHITE)
    m.quad(2, [(-3.4, 0.0, -2.0), (3.4, 0.0, -2.0), (2.5, 11.0, -5.4), (-2.5, 11.0, -5.4)], [(0, 0), (1, 0), (1, 1), (0, 1)], HOT)          # the white-hot mouth
    for sx in (-1, 1):                                                                       # fangs: one flat tooth each
        m.quad(2, [(sx * 3.4 - 1.0, 11.5, -2.0), (sx * 3.4 + 1.0, 11.5, -2.0), (sx * 3.4 + 0.3, 12.2, -6.4), (sx * 3.4 + 0.3, 12.2, -6.4)], [(0, 0), (1, 0), (1, 1), (0, 1)], HOT, ref=(sx * 3.4, 5.0, -3.0))
        m.quad(2, [(sx * 6.4, 3.0, 8.2), (sx * 7.6, 6.5, 7.4), (sx * 6.2, 7.0, 8.6), (sx * 5.4, 3.5, 9.4)], [(0, 0), (1, 0), (1, 1), (0, 1)], (255, 244, 190))          # eye slit
    return m


MESHES = {}


def build():
    for i, (curl, seed, jag, wave) in enumerate([(0.55, 1, 0.5, 0.6), (0.95, 2, 0.8, -0.5), (0.30, 3, 0.4, 0.9)]):
        MESHES["tongue_" + "abc"[i]] = tongue("tongue_" + "abc"[i], L=32, W=9.0, curl=curl, S=4, n=4, seed=seed, jag=jag, wave=wave)                       # hero: 16 quads
    for i, (curl, seed, jag, wave) in enumerate([(0.70, 11, 0.5, 0.6), (1.10, 12, 0.7, -0.6), (0.40, 13, 0.4, 0.9)]):
        MESHES["tonguelo_" + "abc"[i]] = tongue("tonguelo_" + "abc"[i], L=32, W=9.0, curl=curl, S=4, n=3, seed=seed, jag=jag, wave=wave)                    # 12 quads
    for i, (curl, seed, wave) in enumerate([(0.7, 21, 0.7), (1.2, 22, -0.7), (0.3, 23, 0.9)]):
        MESHES["tonguexs_" + "abc"[i]] = tongue("tonguexs_" + "abc"[i], L=32, W=9.0, curl=curl, S=3, n=3, seed=seed, jag=0.3, wave=wave)                      # 9 quads
    MESHES["ball"] = ball("ball", R=9, n=6, J=4, seed=3)
    MESHES["ball_s"] = ball("ball_s", R=9, n=6, J=3, seed=5)
    MESHES["gem"] = ball("gem", R=9, n=4, J=2, seed=9, jit=0.18)
    MESHES["spike"] = tongue("spike", L=14, W=3.6, curl=0.3, S=1, n=3, seed=7, wave=0.0)
    MESHES["column"] = column("column")
    MESHES["helix"] = helix("helix", S=8)
    MESHES["lance"] = lance("lance")
    MESHES["ring"] = ring("ring")
    MESHES["lion_head"] = lion_head("lion_head")


def main():
    os.makedirs(OUT, exist_ok=True)
    build()
    lines = []
    for name, m in MESHES.items():
        with open(os.path.join(OUT, name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(m.text())
        c = m.counts()
        lines.append("%-12s quads %3d  layer0 %4d  layer1 %3d  layer2 %3d  total %4d vertices" % (name, len(m.q), c[0], c[1], c[2], sum(c)))
    with open(os.path.join(OUT, "meshes.txt"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
