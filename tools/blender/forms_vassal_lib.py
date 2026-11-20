"""Small helpers shared by the four vassal form builders (materials with a house look, lancets, claws, eyes, petals, curved katana blades). Own file: forms_kit.py is not edited."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,F401
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402


# ================================================================ materials (every one is baked into the skin: free in game)
def metal(name, col, wear, seed, rough=0.3, dirt=0.55, dirt_col=(0.02, 0.02, 0.025, 1), mottle=None, bump="brushed", bs=0.1, **kw):
    return K.surface(name, col, 1.0, rough, wear=1.0, wear_color=wear, dirt=dirt, dirt_color=dirt_col, scratches=0.8, bump=bump, bump_strength=bs, mottle=0.5 if mottle else 0.0,
                     mottle_color=mottle, fine=0.3, seed=seed, bevel=0.18, ao_dist=1.6, wear_radius=0.5, **kw)


def hammered(name, col, wear, seed, rough=0.34, dirt_col=(0.04, 0.025, 0.006, 1), mottle=None, **kw):
    return K.surface(name, col, 1.0, rough, wear=0.9, wear_color=wear, dirt=0.75, dirt_color=dirt_col, scratches=0.6, bump="hammer", bump_scale=0.7, bump_strength=0.16,
                     mottle=0.6 if mottle else 0.0, mottle_color=mottle, fine=0.4, seed=seed, bevel=0.16, ao_dist=1.8, **kw)


def paint(name, col, wear, seed, rough=0.5, dirt_col=(0.02, 0.02, 0.02, 1), mottle=None, bump=None, **kw):
    """Paint / lacquer / bone over a base: worn edges show `wear`, crevices dirt (not metallic)."""
    return K.surface(name, col, 0.0, rough, wear=0.7, wear_color=wear, wear_metal=0.0, dirt=0.7, dirt_color=dirt_col, mottle=0.7 if mottle else 0.0, mottle_color=mottle,
                     bump=bump, bump_strength=0.35, fine=0.4, seed=seed, bevel=0.1, ao_dist=1.6, **kw)


def cloth(name, col, groove, seed, mottle=None, bump="cross", scale=1.2, **kw):
    return K.surface(name, col, 0.0, 0.65, bump=bump, bump_scale=scale, bump_strength=0.8, groove=groove, groove_amount=0.8, dirt=0.65, dirt_color=(0.03, 0.025, 0.018, 1),
                     mottle=0.8 if mottle else 0.0, mottle_color=mottle, wear=0.55, wear_color=tuple(min(1.0, c * 1.5 + 0.08) for c in col[:3]) + (1.0,), wear_metal=0.0, fine=0.5, seed=seed, bevel=0.0, **kw)


def glow(name, col, strength, seed, flicker=0.6, ramp=None, rough=0.3, base=None, **kw):
    return K.surface(name, base or col, 0.0, rough, emit=col, emit_strength=strength, emit_flicker=flicker, emit_ramp=ramp, seed=seed, bevel=0.0, mottle=0.8,
                     mottle_color=tuple(min(1.0, c * 1.25 + 0.15) for c in col[:3]) + (1.0,), **kw)


def gem(name, col, strength, seed, emit=None, **kw):
    return K.surface(name, col, 0.0, 0.06, emit=emit or col, emit_strength=strength, emit_flicker=0.5, mottle=0.9, mottle_color=tuple(min(1.0, c * 1.3 + 0.25) for c in col[:3]) + (1.0,),
                     seed=seed, bevel=0.0, rim=(tuple(min(1.0, c * 1.4 + 0.3) for c in col[:3]) + (1.0,), 0.5), **kw)


def glass(name, col, seed, strength=0.25, **kw):
    return K.surface(name, col, 0.0, 0.04, transmission=0.0, emit=col, emit_strength=strength, emit_flicker=0.7, mottle=1.0, mottle_color=(1, 1, 1, 1), seed=seed, bevel=0.0,
                     rim=((1, 1, 1, 1), 0.7), scratches=0.5, **kw)


def mirrorm(name, col, seed, **kw):
    """Polished mirror glass: a bright metallic face with a diagonal sheen (baked), faint glow."""
    return K.surface(name, col, 1.0, 0.07, wear=0.2, wear_color=(1, 1, 1, 1), scratches=0.55, mottle=0.9, mottle_color=(1, 1, 1, 1), seed=seed, bevel=0.0, emit=col, emit_strength=0.12,
                     cramp=("y", 30.0, 72.0, [(0.0, col), (0.5, tuple(min(1.0, c * 1.6 + 0.1) for c in col[:3]) + (1.0,)), (1.0, col)]), **kw)


# ================================================================ geometry
def lancet_rows(c, d, length, width, n=6, curve=0.0, tipw=0.0, belly=0.35):
    """Rows (left, right) of a flat lancet pointing from c along d (xy plane): widest near `belly`, a needle tip; optional sideways curve."""
    d = V(d).normalized()
    s = V((-d.y, d.x, 0))
    rows = []
    for i in range(n + 1):
        t = i / n
        w = width * 0.5 * (math.sin(math.pi * min(1.0, t ** (0.5 / max(belly, 0.1) * 0.35 + 0.35))) ** 0.8 if t < 1 else 0.0)
        p = V(c) + d * (length * t) + s * (curve * t * t)
        rows.append((p - s * max(w, tipw if t >= 1 else 0), p + s * max(w, tipw if t >= 1 else 0)))
    return rows


def lancet(name, c, d, length, width, mat, thick=0.35, part="rays", curve=0.0, z=0.0, n=6, belly=0.35, up=(0, 0, 1)):
    rows = [(l + V((0, 0, z)), r + V((0, 0, z))) for l, r in lancet_rows(c, d, length, width, n, curve, belly=belly)]
    return K.strip(name, rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=up, angle=50)


def claw(name, base, direction, length, r, mat, curl=0.0, side=(0, 0, 1), part="claws", sides=5):
    """A curved talon / fang / thorn: a tapered tube from `base` along `direction`, hooking by `curl` cm toward `side`."""
    d = V(direction).normalized()
    sd = V(side).normalized()
    pts = [V(base) + d * (length * t) + sd * (curl * t * t) for t in (0.0, 0.33, 0.66, 1.0)]
    return K.tapered(name, pts, r, 0.04, mat, sides=sides, sub=2, part=part)


def fur(prefix, bases, dirs, length, r, mat, rng_, part="fur", tip_mat=None, jit=0.3):
    """A row of fur tufts / quills: short tapered spikes with seeded length and bend (`bases`, `dirs` lists)."""
    out = []
    for i, (b, d) in enumerate(zip(bases, dirs)):
        ln = length * rng_.uniform(1 - jit, 1 + jit)
        dv = V(d).normalized()
        sd = dv.cross(V((0, 0, 1)))
        if sd.length < 0.1:
            sd = V((1, 0, 0))
        sd.normalize()
        bend = sd * rng_.uniform(-0.25, 0.25) * ln + V((0, 0, rng_.uniform(-0.3, 0.3) * ln * 0.5))
        pts = [V(b), V(b) + dv * ln * 0.5 + bend * 0.5, V(b) + dv * ln + bend]
        out.append(K.tapered("%s%d" % (prefix, i), pts, r * rng_.uniform(0.8, 1.2), 0.04, tip_mat if (tip_mat and i % 2) else mat, sides=4, sub=1, part=part))
    return out


def petal(name, base, direction, length, width, mat, facing=(0, 0, 1), curl=1.0, cup=0.25, thick=0.12, part="petals", seed=0, notch=True, rows=6, simple=False):
    """A cherry petal / leaf: a cupped teardrop with a notched tip (K.leaf_rows profile), lens section."""
    prof = (lambda t: math.sin(math.pi * min(1.0, t ** 0.8)) ** 0.7 * (1.0 if not (notch and t > 0.9) else 0.75))
    L, R, C = K.leaf_rows(base, direction, length, width, curl=curl, rows=rows, facing=facing, seed=seed, profile=prof)
    fz = V(facing).normalized()
    wmax = max((r - l).length for l, r in zip(L, R)) or 1.0
    rws = []
    for l, r in zip(L, R):
        w = (r - l).length / wmax
        rws.append((l + fz * (cup * width * w * w * 0.5), r + fz * (cup * width * w * w * 0.5)))
    if simple:                                                           # 4 quads a row: a thin cupped lens
        return K.strip(name, rws, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=facing, angle=70)
    return K.strip(name, rws, mat, (-1.0, -0.5, 0.0, 0.5, 1.0), (0.0, 0.75, 1.0, 0.75, 0.0), thick, part=part, up=facing, angle=70)


def eye(name, center, r, normal, sclera, iris, pupil, rimmat, part="eyes", up=(0, 1, 0), rng_=None, slit=True):
    """An eyeball bulging out of a surface: a lumpy globe, an iris disc, a slit pupil and a metal rim ring. Built facing `normal` at `center`."""
    M = K.frame(center, normal, up)
    g = K.blob(name + "_ball", (0, 0, 0), (r, r, r * 0.8), sclera, seed=7, lump=0.03, rings=6, segs=9, part=part)
    K.xf(g, M)
    ir = K.bl.lathe(name + "_iris", [(0.01, r * 0.80), (r * 0.52, r * 0.78), (r * 0.58, r * 0.66), (r * 0.5, r * 0.58)], iris, sides=12, axis="z", part=part)
    K.xf(ir, M)
    K._finish_obj(ir, part, True, 50.0)
    if slit:
        pu = K.bl.gem(name + "_pupil", r * 0.5, pupil, center=(0, 0, r * 0.74), depth=0.15, crown=0.1, sides=6, axis="z", rx=r * 0.1, ry=r * 0.5, part=part)
        K.xf(pu, M)
        K._finish_obj(pu, part, True, 40.0)
    rg = K.bl.lathe(name + "_rim", [(r * 1.18, -r * 0.3), (r * 1.3, 0.0), (r * 1.15, r * 0.35), (r * 0.98, r * 0.3), (r * 1.0, -r * 0.2)], rimmat, sides=14, axis="z", caps=(False, False), part=part + "_rim")
    K.xf(rg, M)
    K._finish_obj(rg, part + "_rim", True, 50.0)


def tapered_path(name, pts, r0, r1, mat, part, sides=5, bulge=None, sub=2):
    return K.tapered(name, [V(p) for p in pts], r0, r1, mat, sides=sides, sub=sub, part=part, bulge=bulge)


def ring_torus(name, center, R, r, mat, part, plane="xy", sides=6, segs=20, squash=1.0, z=0.0):
    """A torus (a ring / hoop) lying in the xy plane (or xz / yz) round `center`: tube radius r, ring radius R (optionally squashed in the second axis)."""
    pts = []
    for i in range(segs):
        a = TAU * i / segs
        u, v = R * math.cos(a), R * math.sin(a) * squash
        pts.append({"xy": V((u, v, 0)), "xz": V((u, 0, v)), "yz": V((0, u, v))}[plane] + V(center))
    # closed sweep: loft rings round the path
    rings = []
    n = len(pts)
    for i in range(n):
        p = pts[i]
        tan = (pts[(i + 1) % n] - pts[(i - 1) % n]).normalized()
        ax = V((0, 0, 1)) if plane == "xy" else V((0, 1, 0)) if plane == "xz" else V((1, 0, 0))
        nrm = ax.cross(tan).normalized()
        bi = tan.cross(nrm).normalized()
        rings.append([p + nrm * (r * math.cos(TAU * k / sides)) + bi * (r * math.sin(TAU * k / sides)) for k in range(sides)])
    verts = [p for rg in rings for p in rg]
    faces = []
    for i in range(n):
        j = (i + 1) % n
        for k in range(sides):
            k2 = (k + 1) % sides
            faces.append((i * sides + k, i * sides + k2, j * sides + k2, j * sides + k))
    ob = K.mk(name, verts, faces, mat, part)
    return ob


# ================================================================ katana pieces (the base: tsuka y 1..28, tsuba y 29..31, blade from y 30, edge toward +x)
def katana_blade(name, mat, y0, y1, stations, sori, *, notL=(), notR=(), seed=0, tipL=None, tipR=None, thick=1.1, sec=None, fuller=0.0, wob=0.03, res=5.0, jit=0.04):
    """A curved single-edged blade (edge +x, spine -x): the whole blade bows by `sori` cm. Returns xc(y) so inlays can follow the curve."""
    def xc(y):
        u = (y - y0) / (y1 - y0)
        return sori * 4.0 * u * (1.0 - u) * (1.0 - 0.25 * u)
    L = K.blade_edge(-1, stations, y0, y1, notches=notL, jitter=jit, res=res, seed=seed + 1, tip=tipL)
    R = K.blade_edge(1, stations, y0, y1, notches=notR, jitter=jit, res=res, seed=seed + 2, tip=tipR)
    L = [V((p.x + xc(p.y), p.y, 0)) for p in L]
    R = [V((p.x + xc(p.y), p.y, 0)) for p in R]
    th = (lambda t: thick * (1.0 - 0.45 * t ** 2.0)) if not callable(thick) else thick
    if sec is None and fuller:
        sec = ((-1.0, -0.9, -0.62, -0.5, -0.2, -0.1, 0.66, 1.0), (0.5, 0.9, 1.0, 0.55, 0.55, 1.0, 0.55, 0.0))
    K.blade(name, mat, L, R, thick=th, section=sec, seed=seed + 3, wob=wob)
    return xc


def tsuka(P, y0=1.5, y1=27.5, r=1.8, seed=3, cord_mat=None, wrap_mat=None, core_mat=None, band_mat=None, turns=8.0, bands=(2.4, 14.5, 26.6)):
    """The hilt: swelling core, wound cord, metal bands at both ends (name `grip` so the exporter finds the grip)."""
    K.grip_wrapped("grip", y0, y1, r, core_mat, wrap_mat, turns=turns, cord=0.4, bands=bands, band_mat=band_mat, swell=0.12, part="grip", seed=seed, sides=10)


def lathe(name, prof, mat, part, sides=12, squash=1.0, origin=(0, 0, 0), axis="y", caps=(True, True), angle=45.0):
    ob = K.bl.lathe(name, prof, mat, sides=sides, part=part, squash=squash, origin=origin, axis=axis, caps=caps)
    return K._finish_obj(ob, part, True, angle)


def disc_guard(name, center_y, R, mat, part, steps=2, step_h=0.7, inset=1.0, lobes=0, lobe_amp=0.0, sq=0.85, rr=None, sides=28, phase=0.0, y_squash=1.0):
    """A tsuba-like plate: a disc (optionally lobed) in the xz plane at y = center_y (like the base's tsuba), stepped, both faces; returns nothing."""
    pts = []
    for i in range(sides):
        a = TAU * i / sides + phase
        rad = R * (1.0 + (lobe_amp * math.cos(a * lobes) if lobes else 0.0))
        pts.append((rad * math.cos(a), rad * math.sin(a) * sq))
    # plate() extrudes in z; turn it to the xz plane afterwards
    obs = K.stepped_plate(name, pts, 0.0, mat, steps=steps, step_h=step_h, inset=inset, part=part, bevel_w=0.1, up=1)
    for o in obs:
        K.xf(o, K.frame((0, center_y, 0), (0, 1, 0), (0, 0, 1)))
    return obs


def star_outline(n, r_out, r_in, cx=0.0, cy=0.0, phase=0.0, jit=0.0, rng_=None):
    """The outline [(x, y)] of an n-point star (alternating radii), optionally with seeded irregular arms."""
    pts = []
    for i in range(2 * n):
        a = phase + TAU * i / (2 * n)
        r = r_out if i % 2 == 0 else r_in
        if jit and rng_:
            r *= 1.0 + rng_.uniform(-jit, jit)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def flame(name, base, direction, length, width, mat, *, bend=(0, 0, 0), curl=0.0, facing=(0, 0, 1), thick=0.28, part="flames", seed=0, rows=7):
    """A flame / wisp tongue: swaying, wide low, a needle tip (leaf_rows with a flame profile). ~rows*4 quads."""
    prof = lambda t: (math.sin(math.pi * min(1.0, t ** 0.5)) ** 0.9) * (1.0 - 0.1 * t)       # noqa: E731
    L, R, C = K.leaf_rows(base, direction, length, width, bend=bend, curl=curl, rows=rows, facing=facing, seed=seed, profile=prof)
    return K.strip(name, list(zip(L, R)), mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: thick * (1.0 - 0.7 * t), part=part, up=facing, angle=60)


def blossom(name, center, normal, size, petal_mat, heart_mat, stamen_mat, rng_, *, petals=5, part="blossoms", up=(0, 1, 0), rows=4, thick=0.1):
    """A five-petal cherry flower facing `normal` at `center` (petals notched at the tip and cupped, a glowing heart with stamens). ~petals*16 + 30 quads."""
    M = K.frame(center, normal, up)
    ph = rng_.uniform(0, TAU)
    for k in range(petals):
        a = ph + TAU * k / petals + rng_.uniform(-0.08, 0.08)
        d = M.to_3x3() @ V((math.cos(a), math.sin(a), 0.18))
        petal("%s_p%d" % (name, k), V(center) + M.to_3x3() @ V((math.cos(a), math.sin(a), 0)) * size * 0.1, d, size * rng_.uniform(0.9, 1.1), size * 0.62, petal_mat,
              facing=M.to_3x3() @ V((0, 0, 1)), curl=size * 0.18, cup=0.35, thick=thick, part=part, seed=rng_.randint(0, 999), rows=rows, simple=True)
    K.blob(name + "_heart", V(center) + M.to_3x3() @ V((0, 0, size * 0.1)), (size * 0.16, size * 0.16, size * 0.12), heart_mat, seed=3, lump=0.05, rings=4, segs=6, part=part)
    for k in range(5):
        a = TAU * k / 5 + ph * 0.5
        p0 = V(center) + M.to_3x3() @ V((0, 0, size * 0.1))
        p1 = p0 + M.to_3x3() @ V((math.cos(a) * size * 0.3, math.sin(a) * size * 0.3, size * 0.32))
        K.tapered("%s_s%d" % (name, k), [p0, (p0 + p1) * 0.5, p1], size * 0.03 + 0.04, 0.03, stamen_mat, sides=3, sub=1, part=part)


def flat_ring(name, center, R, section, mat, part, segs=40, squash=1.0, angle=45.0, a0=0.0, a1=TAU):
    """A ring in the xy plane round `center` with a custom closed section [(dr, dz)] (dr radial from R, dz along z): a chakram, a halo, a flat hoop with a sharp edge.
    a0..a1 < TAU leaves an open arc (ends capped)."""
    full = abs((a1 - a0) - TAU) < 1e-6
    n = segs if full else segs + 1
    rings = []
    for i in range(n):
        a = a0 + (a1 - a0) * i / segs
        c, s_ = math.cos(a), math.sin(a)
        rings.append([V(center) + V(((R + dr) * c, (R + dr) * s_ * squash, dz)) for dr, dz in section])
    m = len(section)
    verts = [p for rg in rings for p in rg]
    faces = []
    for i in range(n if full else n - 1):
        j = (i + 1) % n
        for k in range(m):
            k2 = (k + 1) % m
            faces.append((i * m + k, i * m + k2, j * m + k2, j * m + k))
    if not full:
        faces.append(tuple(range(m - 1, -1, -1)))
        faces.append(tuple(range((n - 1) * m, n * m)))
    return K.mk(name, verts, faces, mat, part, angle=angle)
