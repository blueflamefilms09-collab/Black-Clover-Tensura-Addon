"""Spirit Dive WIND, final A (4.x): the real 3D CROWN of the "Anime-true" Wind, authored in the same mesh pipeline as tools/gen_sdw3_models.py (same `q` quad lines, same atlas
`aura/sdw3_skin.png`, same loader `book/SpiritDiveWind3Mesh`), deterministic.

    python3 tools/gen_sdw4_models.py

Writes
  src/main/resources/assets/nusmp/sdw4_meshes/<name>.txt     one mesh each
  docs/previews/spirit_dive_wind_final/models.txt            vertex counts per mesh and layer (the numbers the budgets use)

Meshes (mesh space: y runs UP from the ring, x and z across the head; the painter turns +y to the head's up and spins the mesh about y):
  crown_yuno       Yuno tier: a thin ring with 14 tall arrow-tip bars of uneven height, a second lower ring (layer 2), a glow shell (ring haze + a crossed haze plane per bar)
  crown_std        standard tier: the same with 8 bars and one ring
  crown_yuno_half  the left half (x >= 0) of crown_yuno for the first dive of Wind (the half stage draws the left side only)
  crown_std_half   the left half of crown_std
Layers: 0 solid (bars, ring), 1 glow shell (drawn additive), 2 ornaments (the second ring; the standard tier does not draw it).
A bar is a thin triangular shaft with an arrow head (a three sided pyramid wider than the shaft): 6 quads, 24 vertices. The ring is an open band (outer wall and top): 2 quads a segment.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_sdw3_models as g  # noqa: E402  (the Mesh class, the atlas regions and the palette of the Wind remake)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sdw4_meshes")
DOC = os.path.join(ROOT, "docs", "previews", "spirit_dive_wind_final", "models.txt")

R_RING = 5.3            # radius of the upper ring (the head is 4 px, the hat layer 4.5)
R_LOW = 4.7             # radius of the lower ring of the Yuno tier
LOW_DY = -2.1           # the lower ring hangs this far below the upper one (mesh y, down is negative)
SEGS = 12               # segments of a ring (30 degrees each)

# heights of the bars (px above the ring), tall and short alternating like the crown of the pack, taller than the sprite crown (which was 5..7.6 px)
H_YUNO = [12.6, 8.6, 11.0, 8.0, 11.6, 8.8, 10.6, 12.8, 8.4, 11.2, 8.2, 11.8, 9.0, 10.6]
H_STD = [10.8, 7.6, 9.6, 8.0, 10.4, 7.4, 9.2, 8.2]


def tri(y, w, t, cx, cz, ang):
    """A triangular section at height y round the axis (cx, cz): half width w along the tangent of the ring at angle `ang`, depth t along the radius (the point faces outwards)."""
    rx, rz = math.cos(ang), math.sin(ang)          # radial
    tx, tz = -rz, rx                               # tangent
    return [(cx - tx * w - rx * t * 0.5, y, cz - tz * w - rz * t * 0.5),
            (cx + tx * w - rx * t * 0.5, y, cz + tz * w - rz * t * 0.5),
            (cx + rx * t, y, cz + rz * t)]


def bar(m, ang, h):
    """One arrow-tip bar standing on the ring at angle `ang` (radians, x = cos, z = sin) with height h."""
    cx, cz = R_RING * math.cos(ang), R_RING * math.sin(ang)
    m.loft(0, [tri(0.0, 0.30, 0.30, cx, cz, ang), tri(h * 0.72, 0.30, 0.30, cx, cz, ang)], g.WHT, g.WHITE)                          # the shaft
    tip = tri(h + 1.0, 0.0, 0.0, cx, cz, ang)
    m.loft(0, [tri(h * 0.72, 0.95, 0.60, cx, cz, ang), tip], g.PALE, g.BEVEL)                                                    # the arrow head (overhangs the shaft)
    return cx, cz


def haze(m, ang, h):
    """The glow shell of a bar: two crossed tapered planes (tangent plane and radial plane), additive."""
    cx, cz = R_RING * math.cos(ang), R_RING * math.sin(ang)
    rx, rz = math.cos(ang), math.sin(ang)
    tx, tz = -rz, rx
    gc = (150, 255, 210)
    u0, v0, u1, v1 = g.WHITE
    for (ax, az) in ((tx, tz), (rx, rz)):
        w0, w1 = 1.15, 0.30
        pts = [(cx - ax * w0, 0.0, cz - az * w0), (cx + ax * w0, 0.0, cz + az * w0), (cx + ax * w1, h + 0.9, cz + az * w1), (cx - ax * w1, h + 0.9, cz - az * w1)]
        m.quad(1, pts, [(u0, v1), (u1, v1), (u1, v0), (u0, v0)], gc)


def ring_arc(m, layer, R, y, half_w, half_h, a0, a1, n, rgb, region):
    """An open band (outer wall and top face) from angle a0 to a1 in n segments, centred on height y; ref point = the tube's centre so the normals face outwards / up."""
    u0, v0, u1, v1 = region
    for i in range(n):
        s0, s1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        c0, c1 = (math.cos(s0), math.sin(s0)), (math.cos(s1), math.sin(s1))
        sm = (s0 + s1) * 0.5
        ref = (R * math.cos(sm), y, R * math.sin(sm))
        ro, ri = R + half_w, R - half_w
        ob0, ot0 = (ro * c0[0], y - half_h, ro * c0[1]), (ro * c0[0], y + half_h, ro * c0[1])
        ob1, ot1 = (ro * c1[0], y - half_h, ro * c1[1]), (ro * c1[0], y + half_h, ro * c1[1])
        it0, it1 = (ri * c0[0], y + half_h, ri * c0[1]), (ri * c1[0], y + half_h, ri * c1[1])
        m.quad(layer, [ob0, ot0, ot1, ob1], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], rgb, ref=ref)          # the outer wall
        m.quad(layer, [ot0, it0, it1, ot1], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], rgb, ref=ref)          # the top face


def glow_ring(m, R, a0, a1, n):
    """A wider flat haze band on the ring (layer 1)."""
    u0, v0, u1, v1 = g.WHITE
    for i in range(n):
        s0, s1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        ro, ri = R + 1.5, R - 1.5
        pts = [(ro * math.cos(s0), 0.3, ro * math.sin(s0)), (ri * math.cos(s0), 0.3, ri * math.sin(s0)), (ri * math.cos(s1), 0.3, ri * math.sin(s1)), (ro * math.cos(s1), 0.3, ro * math.sin(s1))]
        m.quad(1, pts, [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], (150, 255, 210), ref=(0.0, -3.0, 0.0))


def crown(name, heights, low_ring, half):
    m = g.Mesh(name)
    n = len(heights)
    keep = [i for i in range(n) if (not half) or math.cos(2 * math.pi * i / n) >= -1e-3]
    for i in keep:
        ang = 2 * math.pi * i / n
        bar(m, ang, heights[i])
        haze(m, ang, heights[i])
    a0, a1, segs = (-math.pi / 2, math.pi / 2, SEGS // 2) if half else (0.0, 2 * math.pi, SEGS)
    ring_arc(m, 0, R_RING, 0.0, 0.34, 0.30, a0, a1, segs, g.WHT, g.WHITE)
    glow_ring(m, R_RING, a0, a1, segs)
    if low_ring:
        ring_arc(m, 2, R_LOW, LOW_DY, 0.30, 0.26, a0, a1, segs, g.MINT, g.BEVEL)
    return m


MESHES = [lambda: crown("crown_yuno", H_YUNO, True, False), lambda: crown("crown_std", H_STD, False, False),
          lambda: crown("crown_yuno_half", H_YUNO, True, True), lambda: crown("crown_std_half", H_STD, False, True)]


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.dirname(DOC), exist_ok=True)
    lines = []
    for f in MESHES:
        m = f()
        with open(os.path.join(OUT, m.name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(m.text())
        c = m.counts()
        lines.append("%-16s layer0 %4d  layer1 %4d  layer2 %4d  total %4d vertices" % (m.name, c[0], c[1], c[2], sum(c)))
    with open(DOC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
