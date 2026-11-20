"""Measure how thin a katana mesh really is (pure Python + numpy, no Blender).

    python3 -B tools/blender/katana_measure.py [--sz] [--md] id [id ...]

For each weapon mesh (assets/anime_multiverse/weapon_meshes/<id>.txt, or the thickened copy in weapon_meshes_sz with --sz) it reads the body layer (0),
slices it along the long mesh axis (y) and reports the BLADE: the stretch from the habaki (first slice walking in from the tip that is clearly wider or
deeper than the blade, i.e. the guard) to the tip. The tip is at +y unless the mesh states `grip` above 0.5 (hilt at the far end).

  L         whole mesh length (mesh units)       blade  blade length / L (real katana 0.70 .. 0.80)
  w/B       width at the habaki / blade length (real katana 1/22 .. 1/18)
  taper     width 85% of the way to the tip / width at the habaki (real < 0.8; 1.0 = a slab)
  t/w       thickness / width (real katana <= 0.35)
  sori      sideways bow of the blade's centre line, share of blade length (real 0.01 .. 0.04)
Verdict words: THICK (w/B > 1/14), DEEP (t/w > 0.5), SLAB (taper > 0.85), STRAIGHT (sori < 0.005), ok.
"""
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
BINS = 96


def read_mesh(path, layers=(0,)):
    """-> (quads [4x3 arrays], grip fraction or None) of the given layers."""
    quads, grip = [], None
    for line in open(path):
        v = line.split()
        if not v:
            continue
        if v[0] == "grip":
            grip = float(v[1])
        elif v[0] == "q" and int(v[1]) in layers:
            quads.append(np.array([float(x) for x in v[2:22]]).reshape(4, 5)[:, :3])
        elif v[0] == "qn" and int(v[1]) in layers:
            quads.append(np.array([float(x) for x in v[2:34]]).reshape(4, 8)[:, :3])
    return quads, grip


def _clip(poly, y, ge):
    out = []
    n = len(poly)
    for k in range(n):
        a, b = poly[k], poly[(k + 1) % n]
        ia, ib = (a[1] >= y) == ge, (b[1] >= y) == ge
        if ia:
            out.append(a)
        if ia != ib and abs(b[1] - a[1]) > 1e-12:
            t = (y - a[1]) / (b[1] - a[1])
            out.append(a + t * (b - a))
    return out


def slices(quads, bins=BINS):
    ys = np.array([p[1] for q in quads for p in q])
    y0, y1 = float(ys.min()), float(ys.max())
    bh = (y1 - y0) / bins
    xmin = np.full(bins, 1e30)
    xmax = np.full(bins, -1e30)
    zmin = np.full(bins, 1e30)
    zmax = np.full(bins, -1e30)
    for q in quads:
        b0 = min(bins - 1, max(0, int((q[:, 1].min() - y0) / bh)))
        b1 = min(bins - 1, max(0, int((q[:, 1].max() - y0) / bh)))
        for b in range(b0, b1 + 1):
            poly = list(q)
            if b0 != b1:
                poly = _clip(_clip(poly, y0 + b * bh, True), y0 + (b + 1) * bh, False)
            if len(poly) < 3:
                continue
            P = np.array(poly)
            xmin[b] = min(xmin[b], P[:, 0].min())
            xmax[b] = max(xmax[b], P[:, 0].max())
            zmin[b] = min(zmin[b], P[:, 2].min())
            zmax[b] = max(zmax[b], P[:, 2].max())
    return y0, y1, xmin, xmax, zmin, zmax


def measure(path):
    quads, grip = read_mesh(path)
    if not quads:
        return None
    y0, y1, xmin, xmax, zmin, zmax = slices(quads)
    ok = xmax >= xmin
    w = np.where(ok, xmax - xmin, 0.0)
    d = np.where(ok, zmax - zmin, 0.0)
    cx = np.where(ok, (xmax + xmin) / 2, 0.0)
    n = len(w)
    if grip is not None and grip > 0.5:                 # hilt at the far end: walk the other way
        w, d, cx = w[::-1], d[::-1], cx[::-1]
    L = y1 - y0
    wm = float(np.median(w[int(n * 0.50):int(n * 0.68)]))      # the blade's own width (45-70% of the length is always blade)
    dm = float(np.median(d[int(n * 0.50):int(n * 0.68)]))
    k = n - 1
    while k > 0 and (w[k] <= 1.5 * wm + 1e-9 and d[k] <= 1.7 * dm + 1e-9):
        k -= 1
    start = k + 1                                       # first blade slice above the guard
    blade_bins = n - start
    B = blade_bins * L / n
    seg = slice(start + 1, start + 5)
    wb, db = float(np.median(w[seg])), float(np.median(d[seg]))
    t85 = start + int(0.85 * blade_bins)
    wtip = float(np.median(w[t85 - 1:t85 + 2]))
    c = cx[start + 2:n - 3]
    sori = float(np.abs(c - np.linspace(c[0], c[-1], len(c))).max()) / B if len(c) > 6 else 0.0
    return dict(L=L, blade=B, blade_frac=B / L, wb=wb, db=db, wtip=wtip, wmid=wm, wmid_over_B=wm / B if B else 0, w_over_B=wb / B if B else 0, t_over_w=db / wb if wb else 0,
                taper=wtip / wb if wb else 0, sori=sori)


def verdict(m):
    v = []
    if m["w_over_B"] > 1 / 14:
        v.append("THICK")
    if m["t_over_w"] > 0.5:
        v.append("DEEP")
    if m["taper"] > 0.85:
        v.append("SLAB")
    if m["sori"] < 0.005:
        v.append("STRAIGHT")
    return ",".join(v) or "ok"


def row(name, m):
    return "%-46s L=%6.1f blade=%.2f w/B=1/%-5.1f taper=%.2f t/w=%.2f sori=%.3f  %s" % (
        name, m["L"], m["blade_frac"], 1 / m["w_over_B"] if m["w_over_B"] else 0, m["taper"], m["t_over_w"], m["sori"], verdict(m))


def drawn_path(i, sz=True):
    """The file the game really draws: the thickened copy when there is one, else the mesh."""
    p = os.path.join(ASSETS, "weapon_meshes_sz", i + ".txt")
    return p if sz and os.path.exists(p) else os.path.join(ASSETS, "weapon_meshes", i + ".txt")


def mid_blade(path):
    """The mid-blade section the way the mod's size rule (WeaponSizeRule.measure) takes it: the median width and depth of the slices between 45 and 70 percent of the whole mesh
    length. -> (width / length, thickness / width, length) in mesh units."""
    quads, grip = read_mesh(path)
    if not quads:
        return None
    y0, y1, xmin, xmax, zmin, zmax = slices(quads)
    n = len(xmin)
    ok = xmax >= xmin
    w = np.where(ok, xmax - xmin, 0.0)
    d = np.where(ok, zmax - zmin, 0.0)
    if grip is not None and grip > 0.5:
        w, d = w[::-1], d[::-1]
    sel = slice(int(n * 0.45), int(np.ceil(n * 0.70)))
    wm, dm = float(np.median(w[sel])), float(np.median(d[sel]))
    L = y1 - y0
    return wm / L, (dm / wm if wm else 0.0), L


def table(ids):
    """A markdown table: for each id the old mesh (classic copy), what the game drew until now (the thickened copy) and the mesh now, as width/length and thickness/width of the mid blade."""
    rows = ["| katana | old mesh w/L | drawn until now w/L | now w/L | now t/w | blade w0 / blade length (now) |", "|---|---|---|---|---|---|"]
    for i in ids:
        up = "up" if i.startswith("hellblaze_") else ""
        cl = os.path.join(ASSETS, "weapon_meshes", up, "classic_vassal", i + ".txt")
        sz = os.path.join(ASSETS, "weapon_meshes_sz", "classic_vassal", i + ".txt")
        nw = os.path.join(ASSETS, "weapon_meshes", up, i + ".txt")

        def f(p):
            m = mid_blade(p) if os.path.exists(p) else None
            return m
        o, z, n = f(cl), f(sz), f(nw)
        mm = measure(nw) if os.path.exists(nw) else None
        fmt = lambda m: ("1/%.0f" % (1 / m[0])) if m and m[0] else "-"          # noqa: E731
        rows.append("| %s | %s | %s | %s | %.2f | %s |" % (i, fmt(o), fmt(z) if z else "(same as old)", fmt(n), n[1] if n else 0, ("1/%.1f" % (1 / mm["w_over_B"])) if mm and mm["w_over_B"] else "-"))
    return "\n".join(rows)


if __name__ == "__main__" and "--table" in sys.argv:
    sys.path.insert(0, HERE)
    import katana_sheet
    print(table(katana_sheet.IDS))
    sys.exit(0)

if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    for i in args:
        for tag, p in (("mesh", os.path.join(ASSETS, "weapon_meshes", i + ".txt")), ("drawn", drawn_path(i))):
            if tag == "drawn" and p == os.path.join(ASSETS, "weapon_meshes", i + ".txt"):
                continue
            m = measure(p)
            print(row(i + " [" + tag + "]", m) if m else i + ": no quads")
