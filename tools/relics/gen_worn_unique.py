"""Unique worn models for the generated relics (1.70.0 and later): every relic of the generated waves gets its own ornament model `uq_<id>` on top of its slot's look.

Owner rule (1.70.0): relics must never look the same. The look of a slot (a crown, a cloak, a pair of gauntlets) is shared by many relics, so each generated relic also wears
a small ornament built from its own id: two or three motifs (spikes, a gem cluster, a ring, circling motes, a feather fan, a hanging chain, a crescent, rune bars, a horn pair,
flame tongues) picked and sized by a seed made from the id, set on the mount points of its slot. Together with the relic's own palette and its own sprite that makes the
ensemble unique by construction; `verify()` proves it (no two ornaments have the same geometry) and RelicUniqueLooksTest keeps it so.

    python3 tools/relics/gen_worn_unique.py --check      # builds every ornament, fails on a duplicate or an over-budget model
"""
import hashlib
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_worn_lib as L  # noqa: E402
from gen_worn_lib import WModel, Part, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, HOT  # noqa: E402

ORNAMENT_CAP = 900

# slot -> (anchor(s), mount points: (position, outward direction))
MOUNTS = {
    "HEAD": ("head", [((0, 8.4, 0), (0, 1, 0)), ((0, 6.0, -4.4), (0, 0, -1)), ((4.4, 5.0, 0), (1, 0, 0)), ((-4.4, 5.0, 0), (-1, 0, 0)), ((0, 6.0, 4.4), (0, 0, 1))]),
    "NECK": ("body", [((0, -2.2, -2.5), (0, 0, -1)), ((0, -3.6, -2.5), (0, 0, -1)), ((2.2, -1.6, -2.4), (1, 0, -1))]),
    "CHARM": ("body", [((5.2, -9.5, 0), (1, 0, 0)), ((-5.2, -9.5, 0), (-1, 0, 0)), ((0, -10.5, -2.6), (0, 0, -1))]),
    "CHEST": ("body", [((0, -4.0, -2.5), (0, 0, -1)), ((4.8, -0.5, 0), (1, 1, 0)), ((-4.8, -0.5, 0), (-1, 1, 0)), ((0, -8.0, -2.5), (0, 0, -1))]),
    "BACK": ("body", [((0, -2.5, 2.6), (0, 0, 1)), ((3.0, -1.0, 2.6), (1, 0, 1)), ((-3.0, -1.0, 2.6), (-1, 0, 1)), ((0, -8.0, 2.8), (0, 0, 1))]),
    "BELT": ("body", [((0, -10.8, -2.6), (0, 0, -1)), ((4.4, -10.8, 0), (1, 0, 0)), ((-4.4, -10.8, 0), (-1, 0, 0)), ((0, -10.8, 2.6), (0, 0, 1))]),
    "HANDS": ("rarm", [((-1.0, -6.0, -2.4), (0, 0, -1)), ((-3.4, -6.0, 0), (-1, 0, 0)), ((-1.0, -9.0, -2.4), (0, 0, -1))]),
    "FEET": ("rleg", [((0, -7.5, -2.4), (0, 0, -1)), ((-2.4, -7.5, 0), (-1, 0, 0)), ((0, -10.5, -2.4), (0, 0, -1))]),
}
PAIRED = {"HANDS": ("rarm", "larm~"), "FEET": ("rleg", "lleg")}

TILES_FOR = ["gilt", "gem", "crystal", "spark", "metal", "bone", "horn", "rune", "silk", "plate"]
COLOUR_SLOTS = [GLOW, ACCENT, TRIM, MAIN]


def seed_of(rid):
    return int.from_bytes(hashlib.sha256(("nusmp:uq:" + rid).encode()).digest()[:8], "big")


def _basis(d):
    d = np.array(d, float)
    d /= np.linalg.norm(d)
    ref = np.array([0, 1.0, 0]) if abs(d[1]) < 0.9 else np.array([1.0, 0, 0])
    u = np.cross(d, ref)
    u /= np.linalg.norm(u)
    v = np.cross(d, u)
    return d, u, v


# ---------------------------------------------------------------- motifs: each builds into model m at mount (c, d)
def m_spikes(m, rg, c, d, tag):
    d, u, v = _basis(d)
    n, ln, rad = int(rg.integers(3, 8)), float(rg.uniform(1.6, 4.2)), float(rg.uniform(0.25, 0.5))
    spread = float(rg.uniform(0.25, 0.9))
    p = Part(tag)
    for k in range(n):
        t = (k - (n - 1) / 2.0) / max(1.0, (n - 1) / 2.0)
        base = np.array(c) + u * t * spread * n * 0.55
        tip = base + d * ln * (1.0 - 0.35 * abs(t)) + u * t * spread * 1.5
        L.cone(p, tuple(base), tuple(tip), rad, 4, mid=0.35, mid_r=1.0)
    m.add(p, ACCENT, TILES_FOR[int(rg.integers(0, 6))], 0, uv="fit")


def m_gems(m, rg, c, d, tag):
    d, u, v = _basis(d)
    p, q = Part(tag), Part(tag + "s")
    r = float(rg.uniform(0.7, 1.25))
    cc = np.array(c) + d * 0.5
    L.dia(p, tuple(cc), r * 0.8, r * float(rg.uniform(1.0, 1.6)), r * 0.8)
    for k in range(int(rg.integers(2, 5))):
        a = 2 * math.pi * k / 4 + float(rg.uniform(0, 1))
        o = cc + u * math.cos(a) * r * 1.9 + v * math.sin(a) * r * 1.9
        L.dia(q, tuple(o), r * 0.38, r * 0.55, r * 0.38)
    m.add(p, GLOW, "gem", EMIT | HOT, uv="fit")
    m.add(q, COLOUR_SLOTS[int(rg.integers(0, 3))], "crystal", EMIT, uv="fit")


def m_ring(m, rg, c, d, tag):
    d, u, v = _basis(d)
    p = Part(tag)
    R, r = float(rg.uniform(1.1, 2.7)), float(rg.uniform(0.18, 0.34))
    cc = np.array(c) + d * 0.7
    plane = "xy" if abs(d[2]) > 0.5 else ("yz" if abs(d[0]) > 0.5 else "xz")
    L.torus(p, tuple(cc), R, r, 14, 4, plane=plane)
    m.add(p, TRIM, TILES_FOR[int(rg.integers(0, 3))], 0, uv="fit")


def m_motes(m, rg, c, d, tag):
    n, rad, y = int(rg.integers(2, 6)), float(rg.uniform(1.8, 3.4)), float(c[1])
    g = m.group(tag, "orbit", (c[0], y, c[2]), float(rg.uniform(1.2, 3.2)) * (1 if rg.random() < 0.5 else -1), 0.0, 0.0)
    for k in range(n):
        a = 2 * math.pi * k / n
        p = Part("%s%d" % (tag, k))
        cc = (c[0] + rad * math.cos(a), y + 0.5 * math.sin(k * 2.3), c[2] + rad * math.sin(a))
        r = float(rg.uniform(0.3, 0.6))
        L.dia(p, cc, r, r * 1.5, r)
        m.add(p, GLOW, "spark", EMIT | HOT, group=g, uv="fit")


def m_fan(m, rg, c, d, tag):
    d, u, v = _basis(d)
    n, ln, w = int(rg.integers(3, 7)), float(rg.uniform(3.0, 6.0)), float(rg.uniform(0.5, 0.9))
    p = Part(tag)
    for k in range(n):
        t = (k - (n - 1) / 2.0) / max(1.0, (n - 1) / 2.0)
        tip = np.array(c) + d * ln * (1.0 - 0.25 * abs(t)) + u * t * ln * 0.55
        L.feather(p, tuple(np.array(c) + d * 0.3), tuple(tip), w, 0.3, 4, curl=float(rg.uniform(-0.1, 0.1)))
    m.add(p, ACCENT if rg.random() < 0.5 else MAIN, "feather", 0, uv="fit")


def m_chain(m, rg, c, d, tag):
    d, u, v = _basis(d)
    p = Part(tag)
    span, sag = float(rg.uniform(2.5, 5.0)), float(rg.uniform(1.0, 3.0))
    a = np.array(c) - u * span / 2
    b = np.array(c) + u * span / 2
    mid = (a + b) / 2 - np.array([0, sag, 0]) + d * 0.4
    pts = L.bez(tuple(a), tuple(mid), tuple(b), 7)
    L.beads(p, pts, 0.28, 5, every=int(rg.integers(1, 3)), bead=1.5, thin=0.7)
    m.add(p, TRIM, "gilt", 0, uv="fit")
    q = Part(tag + "p")
    L.dia(q, tuple(mid - np.array([0, 0.9, 0])), 0.55, 0.95, 0.55)
    m.add(q, GLOW, "gem", EMIT, uv="fit")


def m_crescent(m, rg, c, d, tag):
    d, u, v = _basis(d)
    p = Part(tag)
    n, R = int(rg.integers(5, 10)), float(rg.uniform(1.8, 3.4))
    sweep = float(rg.uniform(1.6, 2.8))
    for k in range(n):
        a = -sweep / 2 + sweep * k / (n - 1)
        o = np.array(c) + d * 0.6 + u * R * math.sin(a) + v * (R * (1 - math.cos(a)) * 0.8)
        r = 0.28 + 0.2 * math.sin(math.pi * k / (n - 1))
        L.dia(p, tuple(o), r, r * 1.4, r)
    m.add(p, GLOW, "spark", EMIT, uv="fit")


def m_runes(m, rg, c, d, tag):
    d, u, v = _basis(d)
    p = Part(tag)
    n = int(rg.integers(2, 5))
    for k in range(n):
        o = np.array(c) + d * 0.35 + v * (k - (n - 1) / 2.0) * 0.9 + u * float(rg.uniform(-0.4, 0.4))
        w = float(rg.uniform(0.7, 1.6))
        L.dia(p, tuple(o), w, 0.2, 0.2)
    m.add(p, GLOW, "rune", EMIT, uv="fit")


def m_horns(m, rg, c, d, tag):
    d, u, v = _basis(d)
    ln, curl = float(rg.uniform(2.5, 5.0)), float(rg.uniform(0.6, 2.0))
    for s in (1, -1):
        p = Part("%s%d" % (tag, s))
        base = np.array(c) + u * s * 1.2
        pts = L.bez(tuple(base), tuple(base + u * s * curl + d * ln * 0.5), tuple(base + u * s * curl * 0.4 + d * ln), 5)
        L.horn(p, pts, float(rg.uniform(0.45, 0.8)), 0.08, 5)
        m.add(p, MAIN, "horn", 0, uv="fit")


def m_flames(m, rg, c, d, tag):
    d, u, v = _basis(d)
    n = int(rg.integers(3, 6))
    for k in range(n):
        g = m.group("%s%d" % (tag, k), "bob", tuple(c), 1.1 + 0.25 * k, 0.1 + 0.02 * k, k * 1.3)
        o = np.array(c) + u * (k - (n - 1) / 2.0) * 1.1
        p = Part("%s%d" % (tag, k))
        L.cone(p, tuple(o), tuple(o + d * float(rg.uniform(1.6, 3.0)) + v * 0.2), 0.55, 4, mid=0.3, mid_r=1.0)
        m.add(p, GLOW, "ember", EMIT | HOT, group=g, uv="fit")


MOTIFS = [m_spikes, m_gems, m_ring, m_motes, m_fan, m_chain, m_crescent, m_runes, m_horns, m_flames]


def build(rid, slot):
    anchor, mounts = MOUNTS[slot]
    rg = np.random.default_rng(seed_of(rid))
    m = WModel("uq_" + rid, anchor)
    kinds = [int(k) for k in rg.permutation(len(MOTIFS))[:int(rg.integers(2, 4))]]
    spots = [int(k) for k in rg.permutation(len(mounts))]
    for i, k in enumerate(kinds):
        c, d = mounts[spots[i % len(spots)]]
        jitter = rg.uniform(-0.5, 0.5, 3)
        c = tuple(float(x) for x in np.array(c) + jitter * (0.3 if slot in PAIRED else 1.0))
        MOTIFS[k](m, rg, c, d, "o%d" % i)
    return m


def pieces(rid, slot):
    if slot in PAIRED:
        a, b = PAIRED[slot]
        return ["uq_%s@%s" % (rid, a), "uq_%s@%s" % (rid, b)]
    return ["uq_" + rid]


def catalog_generated(cat, rows):
    """The signature entry of every generated relic: the pieces of its slot's look (as in the catalog's look lines) plus its own ornament."""
    looks = {(sl, lk): ps for sl, lk, ps in cat.looks}
    for r in rows:
        base = list(looks.get((r["slot"], r["look"]), ()))
        cat.relic(r["id"], *base, *pieces(r["id"], r["slot"]))


def models_for(rows):
    """name -> builder for every generated relic (gen_worn_all merges this into BUILDERS)."""
    out = {}
    for r in rows:
        out["uq_" + r["id"]] = (lambda rid=r["id"], slot=r["slot"]: build(rid, slot))
    return out


def signature(model):
    h = hashlib.sha256()
    for (p, slot, tile, flags, group, _uv, _d) in model.items:
        h.update(repr((slot, tile, flags, group)).encode())
        h.update(np.round(np.array(p.V, float), 3).tobytes() if len(p.V) else b"")
    h.update(repr(model.groups).encode())
    return h.hexdigest()


def verify(rows):
    seen, bad = {}, 0
    for r in rows:
        m = build(r["id"], r["slot"])
        if m.vertex_count() > ORNAMENT_CAP:
            print("ornament %s has %d vertices (cap %d)" % (r["id"], m.vertex_count(), ORNAMENT_CAP))
            bad += 1
        sg = signature(m)
        if sg in seen:
            print("ornament of %s is the same as %s" % (r["id"], seen[sg]))
            bad += 1
        seen[sg] = r["id"]
    return bad


if __name__ == "__main__":
    import gen_relic_waves as G
    allrows = G.all_rows()
    b = verify(allrows)
    print("%d ornaments, %d problems" % (len(allrows), b))
    sys.exit(1 if b else 0)
