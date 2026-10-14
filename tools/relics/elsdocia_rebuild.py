"""Elsdocia, rebuilt from the owner's reference render (scratchpad/wpn/66.png, 405 x 493): the owner's modelling method in six steps.

    python tools/gen_elsdocia_icon.py                       # the 16x16 inventory sprite first (the mesh is placed on its diagonal)
    python tools/relics/elsdocia_rebuild.py                 # mesh + skin + glow skin
    ELSDOCIA_PREVIEW=out_dir python tools/relics/elsdocia_rebuild.py     # also the preview beside the reference (silhouette, facets, textured)

0  Target: a baked mesh (assets/nusmp/weapon_meshes/elsdocia.txt, per-vertex normals) drawn by client/WeaponRenderer, skin 1024 px + glow skin.
1  Measure the reference in px, convert with ONE known length (the whole sword is a 1.25 m longsword = 477 px from the pommel top to the tip):
   K = 125 cm / 477 px = 0.262 cm per px. Every number below is a measured px value times K (the table is in docs/elsdocia_rebuild.md).
2  Lofted sections (tools/relics/mesh_lib.py): ricasso (bevelled flats), crystal core (8-point section with a central ridge), crossguard bar,
   flares, boss, grip, pommel; the crystal blade's fractured look = a mosaic of closed facet plates, each with its own tilt, plus thin raised
   crack ridges; the clover is a rim + a dark inlay (two raised parts); the glow core is a separate additive (layer 2) hotspot part.
3  Normals: per part, angle smoothed (the plates are flat-shaded facets, the round parts smooth).
4  Texture: painted in model space on the dominant-axis UV groups (blue glass gradients, light facet edges, scratches, cracks, white-cyan inner
   hotspots) + a glow skin (cyan inner light, hotspots white) that the renderer pulses (the `glow` line).
5  Preview beside the reference.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                              # noqa: E402
import wave1_models as w                                           # noqa: E402

ITEM_ID = "elsdocia"
REFERENCE = os.environ.get("ELSDOCIA_REF", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/wpn/66.png")

# ================================================================ 1 measure (reference px -> cm)
TOTAL_CM = 125.0
REF_PX_TOTAL = 477.0                                                # pommel top y = 8 .. tip y = 485
K = TOTAL_CM / REF_PX_TOTAL                                         # 0.262 cm per reference px


def cm(ref_y_px):
    """Model y (cm from the pommel's end) of a reference pixel row."""
    return (ref_y_px - 8.0) * K


Y_POMMEL1 = cm(30)                                                  # the pommel bulb ends where the grip starts (5.8)
Y_GRIP1 = cm(120)                                                   # grip top, under the guard (29.3)
Y_GUARD = cm(128)                                                   # the centre of the crossguard bar (31.4)
GUARD_HALF = 150.0 * K / 2                                          # the guard spans 150 px = 39.3 cm
Y_RIC0 = cm(134)                                                    # the ricasso begins in the guard boss
Y_RIC1 = cm(225)                                                    # bronze ends, crystal begins (57.0)
Y_TIP = TOTAL_CM
TIP_LEN = 49.0 * K                                                  # the crystal tapers over 49 px (12.8 cm)
Y_TAPER = Y_TIP - TIP_LEN
BLADE_W = 65.0 * K                                                  # the crystal is 65 px wide: 17.0 cm
RIC_W = 55.0 * K                                                    # the ricasso is narrower (about 55 px: 14.4 cm)
GRIP_R = 1.6                                                        # the grip is 12 px wide on the reference (3.1 cm): radius 1.6 cm
RIC_T = 1.9                                                         # blade thickness: the reference is a thick blade (a greatsword's mass)
CORE_T = 1.7
CLOVER_Y = cm(162)                                                  # club centre (px 162), 25 px tall x 33 px wide (6.5 x 8.7 cm)
HW = BLADE_W / 2


def hw(y):
    """Half width of the crystal outline at height y (straight sides, then the pointed tip)."""
    if y <= Y_TAPER:
        return HW
    t = (y - Y_TAPER) / (Y_TIP - Y_TAPER)
    return max(0.0, HW * (1.0 - t) ** 0.95)


# ================================================================ small helpers
def vnoise(h, wd, cell, rng):
    """Smooth value noise 0..1 of shape (h, wd); `cell` = the feature size in px."""
    gh, gw = max(2, int(h / cell) + 2), max(2, int(wd / cell) + 2)
    g = rng.random((gh, gw)).astype(np.float32)
    im = Image.fromarray((g * 255).astype(np.uint8)).resize((max(1, int(gw * cell)), max(1, int(gh * cell))), Image.BICUBIC)
    return (np.asarray(im, np.float32)[:h, :wd] / 255.0) if im.width >= wd and im.height >= h else np.pad(np.asarray(im, np.float32) / 255.0, ((0, max(0, h - im.height)), (0, max(0, wd - im.width))), mode="edge")[:h, :wd]


def lerp(a, b, t):
    return a + (b - a) * t


def plane_z(pl, x, y):
    return max(0.1, pl["zc"] + pl["dzdx"] * (x - pl["cx"]) + pl["dzdy"] * (y - pl["cy"]))


# ================================================================ 2 the crystal mosaic (facet plates)
ROW_Y = [Y_RIC1, 68.0, 80.0, 92.0, 103.0, 113.5, 120.0, Y_TIP]          # tall cells: the reference's planes run long down the blade
COLS = [-1.0, -0.3, 0.38, 1.0]
ZONES = {"a": range(0, 3), "b": range(3, 5), "c": range(5, 7)}       # the plate rows of the three facet parts per face


def base_height(x, y):
    """The crystal's bevelled surface height at (x, y): a ridge down the middle falling to the edges, thinner toward the tip."""
    u = min(1.0, abs(x) / HW)
    h = 0.18 + 0.95 * (1.0 - u) ** 0.9
    return h * (0.35 + 0.65 * min(1.0, hw(y) / HW) ** 0.6)


def make_plates(seed):
    """A jittered grid of the blade area; each cell = one plate polygon + a tilted top plane. Returns (plates, grid X, grid Y)."""
    rng = np.random.default_rng(seed)
    R, C = len(ROW_Y), len(COLS)
    X = np.zeros((R, C))
    Y = np.zeros((R, C))
    for r in range(R):
        for c in range(C):
            y = ROW_Y[r]
            half = hw(y)
            if r not in (0, R - 1):
                y += rng.uniform(-1.3, 1.3) if 0 < c < C - 1 else rng.uniform(-0.9, 0.9)
            half = hw(y)
            f = COLS[c]
            x = f * half
            if 0 < c < C - 1:
                x += rng.uniform(-0.85, 0.85) * min(1.0, half / 4.0)
            elif half > 0:
                x -= math.copysign(rng.uniform(0.0, 0.4), f)           # the outline's notches
            X[r, c], Y[r, c] = x, y
    plates = []

    def mk(ded, r, c):
        if len(ded) < 3:
            return
        cx = sum(p[0] for p in ded) / len(ded)
        cy = sum(p[1] for p in ded) / len(ded)
        area = 0.5 * abs(sum(ded[i][0] * ded[(i + 1) % len(ded)][1] - ded[(i + 1) % len(ded)][0] * ded[i][1] for i in range(len(ded))))
        if area < 0.3:
            return
        ub = min(1.0, abs(cx) / HW)
        slope = -0.95 * 0.9 * (1.0 - ub) ** (-0.1) / HW * math.copysign(1.0, cx) if cx != 0 else 0.0
        slope *= 0.35 + 0.65 * min(1.0, hw(cy) / HW) ** 0.6
        zc = base_height(cx, cy) + rng.uniform(-0.1, 0.34)
        shr = []
        for p in ded:                                                # the crack between plates: 0.07 cm in from the shared edge
            dx, dy = cx - p[0], cy - p[1]
            d = math.hypot(dx, dy) or 1.0
            shr.append((p[0] + dx / d * 0.07, p[1] + dy / d * 0.07))
        plates.append(dict(r=r, c=c, poly=shr, full=ded, cx=cx, cy=cy, zc=zc, dzdx=slope + rng.uniform(-0.11, 0.11), dzdy=rng.uniform(-0.10, 0.10) - 0.02 * (r / R),
                           hot=False, tone=float(rng.uniform(0.08, 0.6)), shade=float(rng.uniform(-0.12, 0.12)), area=area))

    for r in range(R - 1):
        for c in range(C - 1):
            poly = [(X[r, c], Y[r, c]), (X[r, c + 1], Y[r, c + 1]), (X[r + 1, c + 1], Y[r + 1, c + 1]), (X[r + 1, c], Y[r + 1, c])]
            ded = []
            for p in poly:
                if not ded or math.hypot(p[0] - ded[-1][0], p[1] - ded[-1][1]) > 1e-4:
                    ded.append(p)
            if len(ded) > 1 and math.hypot(ded[0][0] - ded[-1][0], ded[0][1] - ded[-1][1]) <= 1e-4:
                ded.pop()
            if len(ded) == 4 and rng.random() < 0.6:                    # split along a diagonal: two more planes with their own tilt
                k = int(rng.integers(0, 2))
                mk([ded[k], ded[(k + 1) % 4], ded[(k + 2) % 4]], r, c)
                mk([ded[(k + 2) % 4], ded[(k + 3) % 4], ded[k]], r, c)
            else:
                mk(ded, r, c)
    big = sorted([i for i, p in enumerate(plates) if p["r"] < R - 2], key=lambda i: -plates[i]["area"])[:12]
    for i in rng.permutation(big)[:5]:
        plates[int(i)]["hot"] = True
    for p in plates:                                                   # the point of the blade burns white, as in the reference
        if p["r"] == R - 2:
            p["hot"] = True
    return plates, X, Y


def add_slab(part, pl, sgn):
    """A closed-top facet slab: base ring on the blade's midplane (buried in the core), tilted planar top ring, side skirt, fan cap."""
    poly = pl["poly"]
    b = [(x, y, 0.0) for x, y in poly]
    t = [(x, y, sgn * plane_z(pl, x, y)) for x, y in poly]
    if sgn < 0:
        b, t = b[::-1], t[::-1]
    ra, rb = part.add_ring(b), part.add_ring(t)
    f0 = len(part.F)
    part.bridge(ra, rb)
    part.cap(rb, outward=(0, 0, sgn))
    return f0


def crack_segments(plates, X, Y, rng, n):
    """Thin raised ridges along some shared plate edges: [(p0, p1, z0, z1)] on the plates' top planes."""
    by = {}
    for p in plates:
        by.setdefault((p["r"], p["c"]), []).append(p)
    R, C = X.shape
    cand = []
    for r in range(R - 1):
        for c in range(1, C - 1):                                      # vertical-ish edges (r, c)-(r+1, c)
            cand.append(((r, c), (r + 1, c), by.get((r, c - 1)), by.get((r, c))))
    for r in range(1, R - 1):
        for c in range(C - 1):                                         # horizontal edges (r, c)-(r, c+1)
            cand.append(((r, c), (r, c + 1), by.get((r - 1, c)), by.get((r, c))))
    order = rng.permutation(len(cand))
    out = []
    for i in order:
        a, b, p1, p2 = cand[i]
        if p1 is None or p2 is None:
            continue
        p0 = (X[a], Y[a])
        pe = (X[b], Y[b])
        if math.hypot(pe[0] - p0[0], pe[1] - p0[1]) < 2.0 or hw(max(p0[1], pe[1])) < 3.0:
            continue
        z0 = max(plane_z(q, *p0) for q in p1 + p2) + 0.03
        z1 = max(plane_z(q, *pe) for q in p1 + p2) + 0.03
        out.append((p0, pe, z0, z1))
        if len(out) >= n:
            break
    return out


# ================================================================ the build
def build():
    m = ml.Mesh(ITEM_ID)
    add = w.add
    # ---------------- pommel: a round bulb with a blue gem on each face, a ring under it (reference: a round pommel, a small knob)
    pb = add(m, "pommel_bulb", "bronze", base=(150, 108, 52), smooth=50)
    ml.lathe(pb, "y", (0, 0, 0), [(0.0, 0.0), (0.25, 0.5), (0.7, 0.9), (1.2, 0.55), (1.8, 1.25), (2.4, 2.5), (3.4, 3.1), (4.5, 2.75), (5.2, 1.95), (Y_POMMEL1, 1.7)], sides=16)
    pr = add(m, "pommel_ring", "gold", base=(190, 140, 54), smooth=45)
    ml.collar(pr, Y_POMMEL1 - 0.9, 2.15, 1.0, lip=0.25, sides=16)
    pg = add(m, "pommel_gem", "crystal", base=(90, 200, 255), glow=True, smooth=0)
    ml.gem(pg, (0, 3.4, 2.85), 0.8, long=0.8, sides=6, axis="z")
    ml.gem(pg, (0, 3.4, -2.85), 0.8, long=0.8, sides=6, axis="-z")
    # ---------------- grip: slim, dark leather, wraps and three bronze collars
    gp = add(m, "grip", "leather", base=(54, 40, 30), uv_mode="loft", smooth=50)
    ml.loft(gp, "y", (0, 0, 0), [(Y_POMMEL1 - 0.4, 0.95, 0.95), (14.0, 1.08, 1.08), (23.0, 1.0, 1.0), (Y_GRIP1 + 1.2, 0.95, 0.95)], ml.circle(GRIP_R, 12))
    gw = add(m, "grip_wraps", "leather", base=(86, 62, 42), wear=0.7, smooth=50)
    yy = Y_POMMEL1 + 1.0
    while yy < Y_GRIP1 - 1.5:
        ml.ring_band(gw, yy, GRIP_R * 1.16, 1.0, sides=12)
        yy += 2.3
    gr = add(m, "grip_rings", "bronze", base=(168, 120, 52), smooth=45)
    for s, r, hgt in ((Y_POMMEL1 - 0.2, 2.1, 0.8), (17.6, 1.95, 0.7), (Y_GRIP1 - 0.9, 2.2, 1.0)):
        ml.collar(gr, s, r, hgt, lip=0.2, sides=14)
    # ---------------- crossguard: a plain dark-gold bar, flared ends, a centre boss, a socket plate under the blade
    gbar = add(m, "guard_bar", "bronze", base=(160, 112, 50), smooth=45)
    ml.loft(gbar, "x", (0, Y_GUARD, 0), [(-17.0, 0.9, 0.9), (-8.0, 1.05, 1.05), (0, 1.2, 1.2), (8.0, 1.05, 1.05), (17.0, 0.9, 0.9)], ml.octagon(2.0, 2.6, 0.5))
    gfl = add(m, "guard_flares", "gold", base=(196, 146, 56), smooth=40)
    for sg in (-1, 1):
        ml.loft(gfl, "x", (0, Y_GUARD, 0), [(sg * 16.2, 0.9, 0.9), (sg * 17.4, 1.5, 1.0), (sg * 18.6, 2.1, 1.25), (sg * 19.65, 1.7, 1.0)], ml.octagon(2.0, 2.6, 0.5))
    gbs = add(m, "guard_boss", "bronze", base=(150, 106, 48), smooth=50)
    ml.lathe(gbs, "z", (0, Y_GUARD, -1.9), [(0.0, 2.2), (0.35, 3.0), (1.2, 3.25), (2.6, 3.25), (3.45, 3.0), (3.8, 2.2)], sides=18)
    gst = add(m, "guard_studs", "gold", base=(214, 168, 70), smooth=0)
    for sx in (-12.5, -9.5, -6.5, 6.5, 9.5, 12.5):
        z = 1.3 * (0.9 + 0.3 * (1 - abs(sx) / 17.0)) - 0.02
        ml.stud(gst, (sx, Y_GUARD, z), 0.3, 0.14, (0, 0, 1), sides=8)
        ml.stud(gst, (sx, Y_GUARD, -z), 0.3, 0.14, (0, 0, -1), sides=8)
    for sg in (-1, 1):
        ml.rivet(gst, (sg * 18.2, Y_GUARD, 1.2), 0.38, (0, 0, 1))
        ml.rivet(gst, (sg * 18.2, Y_GUARD, -1.2), 0.38, (0, 0, -1))
    soc = add(m, "ricasso_socket", "bronze", base=(150, 106, 48), smooth=0)
    ml.box(soc, (-RIC_W / 2 - 0.4, Y_GUARD + 1.2, -1.4), (RIC_W / 2 + 0.4, Y_RIC0 + 2.2, 1.4), chamfer=0.45)
    # ---------------- ricasso: bronze plate with flats, the club cut-out, a centre ridge and gold trim rails
    ric = add(m, "ricasso", "bronze", base=(142, 100, 46), smooth=30, wear=0.9)
    ml.loft(ric, "y", (0, 0, 0), [(Y_RIC0, RIC_W / BLADE_W, 1.0), (49.0, RIC_W / BLADE_W, 1.0), (Y_RIC1 + 0.4, 1.0, 1.0)], ml.bevelled(BLADE_W, RIC_T, 0.3))
    zs = RIC_T / 2
    rt = add(m, "ricasso_trim", "gold", base=(206, 158, 62), smooth=0)
    rr = add(m, "ricasso_ridge", "gold", base=(206, 158, 62), smooth=0)
    for sgn in (-1, 1):
        for sx in (-1, 1):
            ml.bar(rt, (sx * (RIC_W / 2 - 1.6), Y_RIC0 + 2.4, sgn * zs), (sx * (RIC_W / 2 - 1.6), 48.0, sgn * zs), 0.4, 0.1, up=(0, 0, sgn), taper=0.8)
            ml.bar(rt, (sx * (RIC_W / 2 - 1.6), 48.0, sgn * zs), (sx * (HW - 2.4), Y_RIC1 - 0.8, sgn * zs), 0.4, 0.1, up=(0, 0, sgn), taper=0.8)
        ml.bar(rr, (0, Y_RIC0 + 2.6, sgn * zs), (0, CLOVER_Y - 5.9, sgn * zs), 0.55, 0.12, up=(0, 0, sgn), taper=0.9)
        ml.bar(rr, (0, CLOVER_Y + 4.7, sgn * zs), (0, Y_RIC1 - 0.9, sgn * zs), 0.55, 0.12, up=(0, 0, sgn), taper=1.2)
    cr = add(m, "clover_rim", "bronze", base=(204, 154, 66), smooth=40)
    ci = add(m, "clover_inlay", "obsidian", base=(18, 12, 34), smooth=40, glow=True)
    lobes = [(0.0, 1.9), (-2.0, -0.6), (2.0, -0.6)]
    for sgn in (-1, 1):
        z = sgn * (zs - 0.06)
        for (lx, ly) in lobes:
            ml.stud(cr, (lx, CLOVER_Y + ly, z), 2.3 + 0.34, 0.17, (0, 0, sgn), sides=16, dome=0.1)
            ml.stud(ci, (lx, CLOVER_Y + ly, z), 2.3, 0.23, (0, 0, sgn), sides=16, dome=0.1)
        ml.bar(cr, (0, CLOVER_Y - 0.3, z), (0, CLOVER_Y - 4.0, z), 1.9, 0.17, up=(0, 0, sgn), section="rect")
        ml.bar(ci, (0, CLOVER_Y - 0.3, z), (0, CLOVER_Y - 3.7, z), 1.35, 0.23, up=(0, 0, sgn), section="rect")
    jc = add(m, "blade_collar", "bronze", base=(176, 128, 58), smooth=0)
    ml.box(jc, (-HW - 0.45, Y_RIC1 - 0.55, -1.6), (HW + 0.45, Y_RIC1 + 1.0, 1.6), chamfer=0.5)
    # ---------------- crystal: a thin core (the light you see in the cracks) under a mosaic of facet plates
    core = add(m, "crystal_core", "crystal", base=(150, 225, 255), glow=True, uv_mode="loft", smooth=38)
    cst = []
    for y in [Y_RIC1] + [Y_RIC1 + (Y_TAPER - Y_RIC1) * f for f in (0.25, 0.5, 0.75)] + [Y_TAPER, Y_TAPER + 3.2, Y_TAPER + 6.4, Y_TAPER + 9.6, Y_TIP - 0.8]:
        su = max(0.0, (hw(y) - 0.55) / HW)
        cst.append((y, su, min(1.0, su * 1.5 + 0.1) * (CORE_T / 1.7)))
    cst.append((Y_TIP, 0.0, 0.0))
    hwc = HW
    sec = [(hwc, 0), (0.7 * hwc, 0.32 * CORE_T), (0, 0.5 * CORE_T), (-0.7 * hwc, 0.32 * CORE_T), (-hwc, 0), (-0.7 * hwc, -0.32 * CORE_T), (0, -0.5 * CORE_T), (0.7 * hwc, -0.32 * CORE_T)]
    ml.loft(core, "y", (0, 0, 0), cst, sec)
    plates = {}
    facet_parts = {}
    hot_parts = {}
    crack_parts = {}
    stats = {}
    for sgn, side, seed in ((1, "front", 1311), (-1, "back", 7121)):
        pls, X, Y = make_plates(seed)
        plates[side] = pls
        for zone, rows in ZONES.items():
            part = add(m, f"facets_{side}_{zone}", "crystal", base=(60, 140, 230), smooth=20)
            facet_parts[(side, zone)] = part
            for pl in pls:
                if pl["r"] in rows:
                    pl["zone"] = zone
                    pl["f0"] = add_slab(part, pl, sgn)
        hp = m.add(ml.Part(f"glow_hotspots_{side}", layer=2, material="crystal", base=(200, 245, 255), smooth=0))
        hot_parts[side] = hp
        for pl in pls:
            if pl["hot"]:
                ring = [(pl["cx"] + (x - pl["cx"]) * 0.97, pl["cy"] + (y - pl["cy"]) * 0.97) for x, y in pl["full"]]
                rg = hp.add_ring([(x, y, sgn * (plane_z(pl, x, y) + 0.05)) for x, y in ring][::1 if sgn > 0 else -1])
                hp.cap(rg, outward=(0, 0, sgn))
        rng = np.random.default_rng(seed + 5)
        ck = add(m, f"crack_ridges_{side}", "crystal", base=(210, 240, 255), glow=True, smooth=0)
        crack_parts[side] = ck
        segs = crack_segments(pls, X, Y, rng, 9)
        stats[side + "_cracks"] = len(segs)
        for (p0, pe, z0, z1) in segs:
            ml.bar(ck, (p0[0], p0[1], sgn * z0), (pe[0], pe[1], sgn * z1), 0.17, 0.07, up=(0, 0, sgn), taper=0.5)
    return m, plates, stats


# ================================================================ 4 paint
PAL_DEEP, PAL_MID, PAL_LIGHT = np.array((14, 52, 168), np.float32), np.array((44, 124, 236), np.float32), np.array((120, 214, 255), np.float32)
HOT_RGB = np.array((232, 251, 255), np.float32)
GLOW_RGB = np.array((0x5E, 0xC8, 0xFF), np.float32)
LIGHT_DIR = np.array((-0.45, 0.55, 0.7), np.float32)
LIGHT_DIR /= np.linalg.norm(LIGHT_DIR)


def paint_facet_top(c, plates, sgn, seed):
    """The +-z group of a facet part: every pixel belongs to one plate (label raster) or to a crack (the core's light shows)."""
    g = c.group
    h, wd = c.img.shape[:2]
    rng = np.random.default_rng(seed)
    X = c.A if sgn > 0 else -c.A
    Y = c.B
    lab = Image.new("I", (wd, h), 0)
    dr = ImageDraw.Draw(lab)
    mine = [pl for pl in plates if pl.get("zone") == c.part.name[-1]]
    for i, pl in enumerate(mine):
        dr.polygon([g.local_px((x, y, 0.0)) for x, y in pl["poly"]], fill=i + 1)
    L = np.asarray(lab, np.int32)
    img = np.zeros((h, wd, 3), np.float32)
    gl = np.zeros((h, wd, 4), np.float32)
    # the gaps: the thin core light
    img[:] = (70, 170, 250)
    gl[..., :3] = GLOW_RGB
    gl[..., 3] = 150
    ppu = g.ppu
    n_big = vnoise(h, wd, 40, rng)
    n_fine = vnoise(h, wd, 5, rng)
    edge = np.zeros((h, wd), bool)
    for ax in (0, 1):
        for sh in (1, -1):
            edge |= (L != np.roll(L, sh, ax)) & (L > 0)
    lit_edge = ((L != np.roll(L, 1, 0)) | (L != np.roll(L, 1, 1))) & (L > 0)
    for i, pl in enumerate(mine):
        mk = L == i + 1
        if not mk.any():
            continue
        u = X - pl["cx"]
        v = Y - pl["cy"]
        n = np.array((-pl["dzdx"], -pl["dzdy"], 1.0), np.float32)
        n /= np.linalg.norm(n)
        nflat = float(np.dot(np.array((0, 0, 1.0), np.float32), LIGHT_DIR))
        shade = 1.0 + (float(np.dot(n * np.array((sgn, 1, 1), np.float32), LIGHT_DIR)) - nflat) * 5.0 + pl["shade"]
        t = np.clip(pl["tone"] * 0.8 + 0.1 + 0.35 * (n_big - 0.5), 0, 1)
        base = np.where((t < 0.5)[..., None], PAL_DEEP + (PAL_MID - PAL_DEEP) * (t * 2)[..., None], PAL_MID + (PAL_LIGHT - PAL_MID) * ((t - 0.5) * 2)[..., None])
        ang = rng.uniform(0, math.pi)
        grad = 0.2 * np.sin((u * math.cos(ang) + v * math.sin(ang)) * rng.uniform(0.5, 1.2))
        col = base * (shade + grad + 0.10 * (n_fine - 0.5))[..., None]
        # caustic streaks: long thin lighter bands
        streak = np.clip(np.sin((u * 0.9 - v * 0.5) * 2.4 + pl["tone"] * 9) * 0.5 + 0.2, 0, 1) ** 4
        col = col + streak[..., None] * np.array((50, 70, 40), np.float32) * 0.5
        gstr = 0.10 + 0.10 * pl["tone"]
        glw = np.full((h, wd), gstr, np.float32)
        glc = np.broadcast_to(GLOW_RGB, (h, wd, 3)).copy()
        if pl["hot"]:                                                  # white-cyan inner hotspot
            hx, hy = pl["cx"] + rng.uniform(-0.8, 0.8), pl["cy"] + rng.uniform(-1.0, 1.0)
            rad = max(1.5, math.sqrt(pl["area"]) * 0.85)
            d = np.sqrt((X - hx) ** 2 + (Y - hy) ** 2) / rad
            k = np.clip(1.0 - d, 0, 1) ** 1.3
            col = col * (1 - k[..., None] * 0.92) + HOT_RGB * (k[..., None] * 0.92)
            glw = np.maximum(glw, k * 0.95)
            glc = glc * (1 - k[..., None] * 0.8) + np.array((235, 252, 255), np.float32) * (k[..., None] * 0.8)
        img[mk] = col[mk]
        gl[..., :3][mk] = glc[mk]
        gl[..., 3][mk] = glw[mk] * 255
        # a crack line inside some plates: dark line with a bright lip
        if rng.random() < 0.55:
            ck = Image.new("L", (wd, h), 0)
            cd = ImageDraw.Draw(ck)
            pts = [g.local_px((pl["cx"] + rng.uniform(-2.2, 2.2), pl["cy"] + rng.uniform(-2.4, 2.4), 0.0)) for _ in range(rng.integers(2, 4))]
            cd.line(pts, fill=255, width=max(1, int(ppu * 0.07)))
            a = (np.asarray(ck, np.float32) / 255.0) * mk
            img[..., :3] = img * (1 - a[..., None] * 0.55) + np.array((10, 30, 110), np.float32) * (a[..., None] * 0.55)
            lip = np.roll(a, 1, 1) * (1 - a)
            img[..., :3] += lip[..., None] * np.array((120, 130, 120), np.float32) * 0.8
            gl[..., 3] = np.maximum(gl[..., 3], a * 150)
        # sparkle crosses in hot plates
        if pl["hot"] and rng.random() < 0.8:
            for _ in range(2):
                sx, sy = g.local_px((pl["cx"] + rng.uniform(-1.2, 1.2), pl["cy"] + rng.uniform(-1.2, 1.2), 0.0))
                for dx, dy, a in ((0, 0, 1), (1, 0, .6), (-1, 0, .6), (0, 1, .6), (0, -1, .6), (2, 0, .3), (-2, 0, .3), (0, 2, .3), (0, -2, .3)):
                    px, py = int(sx) + dx, int(sy) + dy
                    if 0 <= px < wd and 0 <= py < h and mk[py, px]:
                        img[py, px] = img[py, px] * (1 - a) + 255 * a
                        gl[py, px, :3] = 255
                        gl[py, px, 3] = max(gl[py, px, 3], 255 * a)
    # edges: the plate rims catch the light (upper left) or fall into shadow (lower right)
    img[lit_edge] = img[lit_edge] * 0.4 + np.array((200, 235, 255), np.float32) * 0.6
    sh_edge = edge & ~lit_edge
    img[sh_edge] *= 0.55
    gl[..., 3][edge] = np.maximum(gl[..., 3][edge], 120)
    # long fracture lines that run across several plates (the reference's cracked planes)
    lr = np.random.default_rng(seed * 3 + 1)
    lk = Image.new("L", (wd, h), 0)
    ld = ImageDraw.Draw(lk)
    for _ in range(6):
        x0, y0 = lr.uniform(-HW, HW), lr.uniform(Y_RIC1 + 3, Y_TIP - 8)
        ang = math.radians(lr.uniform(-28, 28)) + math.pi / 2
        ln = lr.uniform(10, 26)
        pts = [g.local_px((x0 + math.cos(ang) * t * ln + 0.6 * math.sin(t * 7 + x0), y0 + math.sin(ang) * t * ln, 0.0)) for t in np.linspace(0, 1, 7)]
        ld.line(pts, fill=255, width=max(1, int(ppu * 0.09)))
    la = (np.asarray(lk, np.float32) / 255.0) * (L > 0)
    img = img * (1 - la[..., None] * 0.5) + np.array((8, 26, 100), np.float32) * (la[..., None] * 0.5)
    lipa = np.roll(la, 1, 1) * (1 - la)
    img += lipa[..., None] * np.array((110, 150, 120), np.float32)
    gl[..., 3] = np.maximum(gl[..., 3], lipa * 190)
    # fine scratches across the whole face
    sc = Image.new("L", (wd, h), 0)
    sd = ImageDraw.Draw(sc)
    for _ in range(int(14 * max(1.0, wd * h / 40000))):
        x0, y0 = rng.uniform(0, wd), rng.uniform(0, h)
        a = rng.uniform(0.2, 1.1) + math.pi / 2 * rng.integers(0, 2)
        ln = rng.uniform(6, 28)
        sd.line([(x0, y0), (x0 + math.cos(a) * ln, y0 + math.sin(a) * ln)], fill=int(rng.uniform(70, 140)), width=1)
    s = np.asarray(sc, np.float32)[..., None] / 255.0 * (L > 0)[..., None]
    img += s * 70
    c.img[..., :3] = np.clip(img, 0, 255)
    c.img[..., 3] = 255
    c.glow[...] = np.clip(gl, 0, 255)


def paint_facet_side(c, rng):
    """Slab skirts and the other groups: deep glass with a light core."""
    h, wd = c.img.shape[:2]
    n = vnoise(h, wd, 6, rng)
    col = np.array((22, 84, 206), np.float32)[None, None, :] * (0.7 + 0.5 * n[..., None]) + np.array((40, 70, 40), np.float32) * (n[..., None] > 0.8)
    c.img[..., :3] = np.clip(col, 0, 255)
    c.img[..., 3] = 255
    c.glow[..., :3] = GLOW_RGB
    c.glow[..., 3] = 90 + 70 * n


def paint_hotspots(c, plates, sgn, seed):
    """Layer 2 (additive) hotspot decals: a white-cyan radial glow over the hot plates."""
    g = c.group
    rng = np.random.default_rng(seed)
    X = c.A if sgn > 0 else -c.A
    Y = c.B
    h, wd = c.img.shape[:2]
    a = np.zeros((h, wd), np.float32)
    col = np.zeros((h, wd, 3), np.float32)
    for pl in plates:
        if not pl["hot"]:
            continue
        rad = max(1.0, math.sqrt(pl["area"]) * 0.5)
        d = np.sqrt((X - pl["cx"]) ** 2 + (Y - pl["cy"]) ** 2) / rad
        k = np.clip(1.0 - d, 0, 1) ** 1.5
        a = np.maximum(a, k)
        col = np.maximum(col, k[..., None] * np.array((235, 252, 255), np.float32))
    c.glow[..., :3] = GLOW_RGB + (np.array((235, 252, 255), np.float32) - GLOW_RGB) * np.clip(a, 0, 1)[..., None]
    c.glow[..., 3] = np.clip(a * 235, 0, 255)
    c.img[..., :3] = 200
    c.img[..., 3] = 255


def paint(atlas, m, plates):
    atlas.paint(m)
    for side, sgn, seed in (("front", 1, 31), ("back", -1, 47)):
        for zone in "abc":
            part = m.part(f"facets_{side}_{zone}")
            rng = np.random.default_rng(seed + ord(zone))

            def fn(c, sgn=sgn, seed=seed, rng=rng, side=side):
                if c.group.kind == "dom" and c.group.key[0] == 2 and c.group.key[1] == sgn:
                    paint_facet_top(c, plates[side], sgn, seed + ord(c.part.name[-1]))
                else:
                    paint_facet_side(c, rng)
            w.paint_part(atlas, part, fn)
        w.paint_part(atlas, m.part(f"glow_hotspots_{side}"), lambda c, side=side, sgn=sgn, seed=seed: paint_hotspots(c, plates[side], sgn, seed))
        w.paint_part(atlas, m.part(f"crack_ridges_{side}"), lambda c: (c.img.__setitem__((..., slice(0, 3)), np.array((200, 235, 255), np.float32)), w.set_glow(c, (150, 225, 255), 0.9, core=0.5)))

    def core_fn(c):
        n = vnoise(*c.img.shape[:2], 12, np.random.default_rng(5))
        c.img[..., :3] = np.array((110, 200, 255), np.float32) * (0.8 + 0.4 * n[..., None])
        w.set_glow(c, (110, 215, 255), 0.95, core=0.45)
    w.paint_part(atlas, m.part("crystal_core"), core_fn)

    def inlay_fn(c):
        h, wd = c.img.shape[:2]
        c.img[..., :3] = np.array((16, 10, 30), np.float32)
        yy, xx = np.mgrid[0:h, 0:wd].astype(np.float32)
        d = np.sqrt((xx - wd / 2) ** 2 + (yy - h / 2) ** 2) / max(1.0, min(wd, h) / 2)
        k = np.clip(1 - d, 0, 1) ** 1.6 * 0.5                           # a faint ember: the red-violet light in the reference's cut-out
        c.glow[..., :3] = np.array((255, 70, 140), np.float32)
        c.glow[..., 3] = k * 255
    w.paint_part(atlas, m.part("clover_inlay"), inlay_fn)
    w.paint_part(atlas, m.part("pommel_gem"), lambda c: w.set_glow(c, (110, 215, 255), 1.0, core=0.5))
    w.paint_part(atlas, m.part("guard_studs"), lambda c: None)


# 1.34.0 (owner: "Elsdocia must be bigger"): the mesh is MEASURED as a 1.25 m longsword (TOTAL_CM above: every proportion comes from the reference),
# but it is PLACED as a greatsword-class crystal blade: APPARENT_M blocks in the hand (the greatsword range of tools/relics/weapon_classes.py is 2.2 - 3.0).
# Only the place scale changes, so no facet, section or texel density of the reference build is touched; weapon_fixes.json has the same target.
APPARENT_M = 2.64


def write_all(m, atlas):
    icon = os.path.join(ml.ASSETS, "textures", "item", ITEM_ID + ".png")
    m.place_on_sprite(icon, real_m=APPARENT_M, grip=0.15)
    m.set_glow("5EC8FF", 0.10, 0.50, 1.0)
    path = os.path.join(ml.ASSETS, "weapon_meshes", ITEM_ID + ".txt")
    n = m.write_txt(path, atlas, normals="vertex", comment="generated by tools/relics/elsdocia_rebuild.py")
    atlas.save(ITEM_ID)
    return n


def check_windings(m):
    """Every facet slab's top faces must face +-z and every side face must face away from the slab (a wrongly wound plate is a hole in game)."""
    bad = 0
    for p in m.parts:
        if not p.name.startswith("facets_"):
            continue
        sgn = 1 if "front" in p.name else -1
        for f in range(len(p.F)):
            nrm = p.face_normal(f)
            cen = np.mean([p.V[i] for i in p.F[f]], axis=0)
            if abs(nrm[2]) > 0.6:
                if nrm[2] * sgn < 0:
                    bad += 1
            else:
                # side skirt: its outward direction in xy must point away from the plate's centroid; verify through the base-to-top direction
                pts = [p.V[i] for i in p.F[f]]
                zmid = np.mean([q[2] for q in pts])
                if zmid * sgn < 0:
                    bad += 1
    return bad


def main():
    m, plates, stats = build()
    atlas = ml.Atlas(1024)
    atlas.layout(m, ppu_max=24.0)
    paint(atlas, m, plates)
    bad = check_windings(m)
    n = write_all(m, atlas)
    st = m.stats(atlas)
    print(f"{ITEM_ID}: {len(m.parts)} parts, {n} quads {st['quads']}, length {st['length']:.1f} cm, atlas {atlas.size}px at {atlas.ppu:.1f} px/cm, winding problems {bad}, {stats}")
    prev = os.environ.get("ELSDOCIA_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        ref = REFERENCE if os.path.exists(REFERENCE) else None
        ml.preview(m, ref, os.path.join(prev, "elsdocia_all.png"), atlas=atlas, normals="vertex")
        ml.preview(m, ref, os.path.join(prev, "elsdocia_top.png"), atlas=atlas, normals="vertex", views=("profile", "three_quarter"), focus=(0, 60))
        ml.preview(m, ref, os.path.join(prev, "elsdocia_blade.png"), atlas=atlas, normals="vertex", views=("profile", "three_quarter"), focus=(56, 125))
    return m, atlas


if __name__ == "__main__":
    main()
