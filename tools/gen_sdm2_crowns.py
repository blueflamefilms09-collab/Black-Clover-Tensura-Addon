"""Spirit Dive MERGE 2 (4.x): the SMALLER, MORE DISTINCT crowns of the Spirit Dives, one per element, authored in the mesh pipeline of tools/gen_sdw3_models.py (same `q` quad lines,
same atlas `aura/sdw3_skin.png`, same loader `book/SpiritDiveWind3Mesh.getIn("sdm2_meshes", name)`), deterministic.

    python3 -B tools/gen_sdm2_crowns.py            # writes the meshes and docs/previews/spirit_dive_merge2/crowns.txt
    python3 -B tools/gen_sdm2_crowns.py --view out.png   # also a debug contact sheet

The old crowns (tools/gen_sdw4_models.py: 14 / 8 arrow bars up to 12.8 px on a ring of radius 5.3 px, a second lower ring) stay registered; these are NEW, about 62 percent of their width and height:
  crown_wind_<tier>[_half]    a tight thin ring, 7 (character) / 5 (standard) arrow bars of alternating height, the one tall bar at the FRONT; a bright rim line over a darker band (the gap that keeps the
                              shape from blurring into the wings behind it); the first dive of Wind (half) keeps the left half
  crown_fire_<tier>           flame tongues: tapered, curving outward, hot white-yellow tips
  crown_water_<tier>          wave crests curling outward with a drop above every second crest
  crown_earth_standard        crystal spikes: five sided prisms with pointed tops, leaning out, on a rock ring
<tier> = char (the character tier: Yuno, Fuegoleon, Noelle) or std. Earth has no character tier. Layers: 0 solid (emissive), 1 glow shell (additive, tinted by the dive's glow colour). Mesh space: y runs UP from the ring,
the front of the head is +z (the painter turns +y to the head's up); the crown does not spin: it follows the head, so the front bar is always at the front.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_sdw3_models as g  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sdm2_meshes")
DOC = os.path.join(ROOT, "docs", "previews", "spirit_dive_merge2", "crowns.txt")

R = 3.3                    # radius of the ring (the old crown: 5.3)
SEGS = 10                  # segments of the ring
H_STD = [8.6, 5.6, 5.6, 7.0, 7.0]                               # bars by angle: front, +-72, +-144
H_CHAR = [9.4, 5.8, 5.8, 7.8, 7.8, 6.2, 6.2]                    # front, +-51.4, +-102.9, +-154.3

PAL = {
    "wind": dict(shaft=(190, 255, 224), head=(255, 255, 255), rim=(255, 255, 255), band=(52, 190, 150), haze=(255, 255, 255)),
    "fire": dict(base=(255, 110, 30), mid=(255, 178, 60), tip=(255, 246, 190), rim=(255, 226, 150), band=(190, 70, 20), haze=(255, 255, 255)),
    "water": dict(base=(80, 160, 255), mid=(150, 212, 255), tip=(255, 255, 255), rim=(225, 245, 255), band=(40, 110, 200), drop=(235, 250, 255), haze=(255, 255, 255)),
    "earth": dict(base=(200, 120, 40), mid=(255, 190, 80), tip=(255, 240, 170), rim=(255, 214, 120), band=(120, 70, 25), haze=(255, 255, 255)),
}


def angles(n):
    """Bar angles from the front (+z), the front bar first, then the pairs outwards: matches H_STD / H_CHAR."""
    out = [0.0]
    for k in range(1, n // 2 + 1):
        a = 2 * math.pi * k / n
        out += [a, -a]
    return out


def pos(ang, r=R):
    return r * math.sin(ang), r * math.cos(ang)       # x, z


def sec(y, cx, cz, ang, w, t, off=0.0, sw=0.0):
    """A triangular section at height y standing at the ring angle `ang`: half width w along the tangent, depth t along the radius, shifted outwards by `off` (the lean / curl) and along the tangent by `sw`."""
    rx, rz = math.sin(ang), math.cos(ang)
    tx, tz = rz, -rx
    cx, cz = cx + rx * off + tx * sw, cz + rz * off + tz * sw
    return [(cx - tx * w - rx * t * 0.5, y, cz - tz * w - rz * t * 0.5), (cx + tx * w - rx * t * 0.5, y, cz + tz * w - rz * t * 0.5), (cx + rx * t, y, cz + rz * t)]


def haze(m, ang, h, w0=0.8, w1=0.2):
    cx, cz = pos(ang)
    tx, tz = math.cos(ang), -math.sin(ang)
    u0, v0, u1, v1 = g.WHITE
    pts = [(cx - tx * w0, 0.0, cz - tz * w0), (cx + tx * w0, 0.0, cz + tz * w0), (cx + tx * w1, h + 0.6, cz + tz * w1), (cx - tx * w1, h + 0.6, cz - tz * w1)]
    m.quad(1, pts, [(u0, v1), (u1, v1), (u1, v0), (u0, v0)], (255, 255, 255))


def ring(m, pal, half, polygon=SEGS):
    """The ring: a thin bright rim (top face and outer wall) over a darker band; `half` keeps the side x >= 0."""
    a0, a1, n = (0.0, math.pi, polygon // 2) if half else (0.0, 2 * math.pi, polygon)
    u0, v0, u1, v1 = g.WHITE
    for i in range(n):
        s0, s1 = a0 + (a1 - a0) * i / n, a0 + (a1 - a0) * (i + 1) / n
        sm = (s0 + s1) / 2
        ref = (R * math.sin(sm), -0.3, R * math.cos(sm))
        def P(s, r, y): return (r * math.sin(s), y, r * math.cos(s))
        ro, ri = R + 0.24, R - 0.24
        m.quad(0, [P(s0, ro, 0.0), P(s0, ri, 0.0), P(s1, ri, 0.0), P(s1, ro, 0.0)], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], pal["rim"], ref=ref)          # the rim: the top face
        m.quad(0, [P(s0, ro, -0.62), P(s0, ro, 0.0), P(s1, ro, 0.0), P(s1, ro, -0.62)], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], pal["band"], ref=ref)       # the band: the outer wall
        m.quad(1, [P(s0, R + 1.0, 0.25), P(s0, R - 1.0, 0.25), P(s1, R - 1.0, 0.25), P(s1, R + 1.0, 0.25)], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], (255, 255, 255), ref=(0, -3.0, 0))


def sel(angs, heights, half):
    return [(a, h) for a, h in zip(angs, heights) if (not half) or math.sin(a) >= -1e-3]


def wind(n, half):
    m = g.Mesh("x")
    pal = PAL["wind"]
    hs = H_CHAR if n == 7 else H_STD
    ring(m, pal, half)
    for a, h in sel(angles(n), hs, half):
        cx, cz = pos(a)
        m.loft(0, [sec(0.0, cx, cz, a, 0.26, 0.26), sec(h * 0.62, cx, cz, a, 0.26, 0.26)], pal["shaft"], g.WHITE)                                            # the shaft
        m.loft(0, [sec(h * 0.62, cx, cz, a, 0.26, 0.26), sec(h * 0.66, cx, cz, a, 0.82, 0.56), sec(h + 1.0, cx, cz, a, 0.0, 0.0)], pal["head"], g.BEVEL)        # the arrow head
        haze(m, a, h)
    return m


def tongue(m, pal, a, h, lean, sway, kinds=("base", "mid", "tip")):
    cx, cz = pos(a)
    ys = [0.0, h * 0.34, h * 0.70, h + 0.8]
    ws = [0.46, 0.60, 0.28, 0.0]
    ts = [0.34, 0.42, 0.22, 0.0]
    offs = [0.0, lean * 0.15, lean * 0.55, lean]
    sws = [0.0, sway * 0.2, sway * 0.6, sway]
    secs = [sec(ys[i], cx, cz, a, ws[i], ts[i], offs[i], sws[i]) for i in range(4)]
    for i in range(3):
        m.loft(0, [secs[i], secs[i + 1]], pal[kinds[i]], g.BEVEL if i == 2 else g.WHITE)


def fire(n):
    m = g.Mesh("x")
    pal = PAL["fire"]
    hs = H_CHAR if n == 7 else H_STD
    ring(m, pal, False)
    for k, (a, h) in enumerate(zip(angles(n), hs)):
        tongue(m, pal, a, h * 1.04, 0.9 + 0.2 * (k % 2), 0.4 * (1 if k % 2 else -1))
        haze(m, a, h, 0.9, 0.12)
    return m


def water(n):
    m = g.Mesh("x")
    pal = PAL["water"]
    hs = H_CHAR if n == 7 else H_STD
    ring(m, pal, False)
    u0, v0, u1, v1 = g.WHITE
    for k, (a, h) in enumerate(zip(angles(n), hs)):
        cx, cz = pos(a)
        ys = [0.0, h * 0.40, h * 0.76, h * 0.96, h * 0.9]                     # a wave crest: rises, then the top curls outwards and over
        offs = [0.0, 0.08, 0.5, 1.3, 2.0]
        ws = [0.46, 0.5, 0.42, 0.26, 0.0]
        ts = [0.2, 0.2, 0.18, 0.14, 0.0]
        secs = [sec(ys[i], cx, cz, a, ws[i], ts[i], offs[i]) for i in range(5)]
        for i, kd in enumerate(("base", "mid", "mid", "tip")):
            m.loft(0, [secs[i], secs[i + 1]], pal[kd], g.BEVEL if i >= 2 else g.WHITE)
        if k % 2 == 0:                                                        # a drop above the crest (a diamond), on every second one
            dy, dx = h + 1.5, 0.0
            top, bot = (cx + math.sin(a) * 1.0, dy + 0.75, cz + math.cos(a) * 1.0), (cx + math.sin(a) * 1.0, dy - 0.65, cz + math.cos(a) * 1.0)
            mid = [(top[0] + math.cos(a) * w * 0.46, dy, top[2] - math.sin(a) * w * 0.46) for w in (1, -1)] + [(top[0] + math.sin(a) * 0.46, dy, top[2] + math.cos(a) * 0.46), (top[0] - math.sin(a) * 0.46, dy, top[2] - math.cos(a) * 0.46)]
            c = (top[0], dy, top[2])
            eq = [mid[0], mid[2], mid[1], mid[3]]
            for j in range(4):
                p, q = eq[j], eq[(j + 1) % 4]
                m.quad(0, [top, p, q, top], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], pal["drop"], ref=c)
                m.quad(0, [bot, q, p, bot], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], pal["drop"], ref=c)
        haze(m, a, h, 0.8, 0.2)
    return m


def earth(n):
    m = g.Mesh("x")
    pal = PAL["earth"]
    hs = H_STD
    ring(m, pal, False, polygon=8)
    for k, (a, h) in enumerate(zip(angles(5), hs)):
        cx, cz = pos(a)
        lean = 0.5 + 0.5 * (k % 2)
        # a crystal: a five sided prism to 62 percent, then the pointed top; sides turned a fifth of a turn apart so it reads as a gem
        rings = []
        for (y, rad, off) in ((0.0, 0.62, 0.0), (h * 0.62, 0.62, lean * 0.5), (h + 0.9, 0.0, lean)):
            rings.append([(cx + math.sin(a) * off + rad * math.cos(2 * math.pi * i / 5 + a), y, cz + math.cos(a) * off + rad * math.sin(2 * math.pi * i / 5 + a)) for i in range(5)])
        m.loft(0, [rings[0], rings[1]], pal["base"], g.WHITE)
        m.loft(0, [rings[1], rings[2]], pal["tip"], g.BEVEL)
        haze(m, a, h, 0.8, 0.25)
    return m


def build(el, tier, half=False):
    n = 7 if tier == "char" else 5
    m = {"wind": lambda: wind(n, half), "fire": lambda: fire(n), "water": lambda: water(n), "earth": lambda: earth(5)}[el]()
    m.name = "crown_%s_%s%s" % (el, tier, "_half" if half else "")
    return m


def all_meshes():
    out = []
    for el in ("wind", "fire", "water", "earth"):
        for tier in (("char", "std") if el != "earth" else ("std",)):
            out.append(build(el, tier))
            if el == "wind":
                out.append(build(el, tier, True))
    return out


def sheet(meshes, path):
    from PIL import Image, ImageDraw
    cell = 220
    out = Image.new("RGB", (cell * 4, cell * len(meshes)), (14, 16, 24))
    d = ImageDraw.Draw(out)
    for r, m in enumerate(meshes):
        for c, (yaw, pitch) in enumerate(((0, 12), (45, 12), (90, 6), (180, 12))):
            a, p = math.radians(yaw), math.radians(pitch)
            polys = []
            for layer, vs, n, rgb in m.q:
                if layer == 1:
                    continue
                pts = []
                for v in vs:
                    x, y, z = v[0] * math.cos(a) + v[2] * math.sin(a), v[1], -v[0] * math.sin(a) + v[2] * math.cos(a)
                    pts.append((x, -(y * math.cos(p) - z * math.sin(p)) , y * math.sin(p) + z * math.cos(p)))
                polys.append((sum(q[2] for q in pts) / 4, pts, rgb))
            for _, pts, rgb in sorted(polys, key=lambda t: -t[0]):
                d.polygon([(c * cell + cell / 2 + q[0] * 14, r * cell + cell * 0.7 + q[1] * 14) for q in pts], fill=tuple(rgb), outline=(30, 30, 40))
        d.text((6, r * cell + 4), m.name, fill=(255, 235, 150))
    out.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.dirname(DOC), exist_ok=True)
    rows, ms = [], all_meshes()
    for m in ms:
        with open(os.path.join(OUT, m.name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(m.text())
        c = m.counts()
        rows.append("%-22s layer0 %4d  layer1 %4d  total %4d vertices" % (m.name, c[0], c[1], c[0] + c[1] + c[2]))
    with open(DOC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("Spirit Dive MERGE 2: the smaller crowns of tools/gen_sdm2_crowns.py (the old crowns: tools/gen_sdw4_models.py)\n" + "\n".join(rows) + "\n")
    print("\n".join(rows))
    if "--view" in sys.argv:
        sheet(ms, sys.argv[sys.argv.index("--view") + 1])


if __name__ == "__main__":
    main()
