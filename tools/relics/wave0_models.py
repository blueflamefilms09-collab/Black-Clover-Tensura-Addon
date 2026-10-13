"""Relics of Many Worlds, wave 0: the one hand model of the sixteen remade items that was clearly wrong against its card.

    python3 -B tools/relics/wave0_models.py [infernal_contract]      (idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave0_models.py        (also renders the preview beside the card)

The Devil Contract (card: Infernal Contract, legendary) was a red book with a clover; the card is an aged parchment scroll on a dark rod with a
pentagram finial and a black claw tip, covered in red-brown runes and a blood-red signature. This builds that scroll with the owner's model
method (tools/relics/mesh_lib.py) and writes it as the item's hand model (weapon_meshes/devil_contract.txt + textures/entity/weapon/devil_contract
[_glow].png). The old book is kept beside it as devil_contract_book (mesh and skins); the 16 x 16 sprite stays the inventory icon.

The method, in order: 0 target (the card art), 1 measure the reference (the P_* card pixels below -> cm), 2 geometry from sections, 3 the small raised
parts as named Parts, 4 smoothed normals and projected UVs, 5 a painted skin and a glow skin, 6 preview beside the card.
Axis: y up the rod (the claw's tip at the bottom), the parchment hangs to +x from the rod, +z toward the viewer.
"""
import math
import os
import sys
import zlib

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                           # noqa: E402
import wave1_models as w                                        # noqa: E402

ITEM = "devil_contract"

# ---------------------------------------------------------------- 1 measure: card pixels (cef699...jpg, 1080 x 1017) -> cm
# the rod runs from the claw's tip (card y 388) to the pentagram's top (card y 68): 320 px. A hand-held scroll rod of this build is about 54 cm.
K = w.px_to_cm(54.0, 388 - 68)
P_ROD_X = 205.0                                   # the rod's axis on the card
P_TIP, P_ROD_BOT, P_ROD_TOP, P_RING_BOT, P_RING_TOP = 388, 345, 148, 128, 68
P_RING_CY = (P_RING_BOT + P_RING_TOP) / 2         # the pentagram ring: 60 px across
P_SHEET_TOP, P_SHEET_BOT = 146, 332               # the parchment spans these card rows (186 px) beside the rod
P_SHEET_L, P_SHEET_R = 250, 860                   # and these columns (610 px: far longer than a hand can hold, so it is kept to a third)


def Y(py):
    return (388.0 - py) * K


R_ROD = 0.95                                      # wood rod: 2 cm thick as drawn (11 px) by 1.9 cm
Y_ROD0, Y_ROD1 = Y(P_ROD_BOT), Y(P_ROD_TOP)
R_COLLAR = 1.45
RING_R = 30 * K                                   # pentagram ring radius (30 px)
RING_T = 0.30
STAR_R = RING_R * 0.92
Y_RING = Y(P_RING_CY)
SHEET_H = (P_SHEET_BOT - P_SHEET_TOP) * K * 0.72  # a slightly shorter sheet than drawn so the rod's ends show
SHEET_Y0 = Y(P_SHEET_BOT) + 1.2
SHEET_W = 50.0                                    # long side of the held sheet (the card's is 3.3 times the rod's height; kept in hand scale)
SHEET_T = 0.14                                    # paper thickness
ROLL_R = 2.05                                     # the rolled-up part of the parchment round the rod
CLAW_LEN = (P_TIP - P_ROD_BOT) * K                # the horn tip below the rod: 43 px
SEAL_X, SEAL_Y, SEAL_R = SHEET_W - 9.0, SHEET_Y0 + SHEET_H * 0.42, 2.1


def sheet_z(x):
    """The sheet's gentle wave and droop away from the rod (cm)."""
    return 0.55 * math.sin(x * 0.30) + 0.012 * (x * x) * 0.1 - 0.0


def rag(t):
    """The torn right edge: a vertical scale of the sheet, 1 along most of it, ragged over the last tenth."""
    if t < 0.90:
        return 1.0
    k = (t - 0.90) / 0.10
    return 1.0 - 0.22 * k * (0.5 + 0.5 * math.sin(t * 160.0)) - 0.05 * k


def build():
    m = ml.Mesh(ITEM)

    # ---- 1 the rod: dark wood, a slight taper, a round loft
    rod = w.add(m, "rod", "wood", base=(64, 38, 26), smooth=40, uv_mode="loft")
    ml.loft(rod, "y", (0, 0, 0), [(Y_ROD0, 0.92, 0.92), (Y_ROD0 + 6, 1.0, 1.0), ((Y_ROD0 + Y_ROD1) / 2, 1.04, 1.04), (Y_ROD1 - 6, 1.0, 1.0), (Y_ROD1, 0.95, 0.95)],
            ml.circle(R_ROD, 12))
    # ---- 2/3 the iron collars at both ends of the rod
    for nm, y in (("collar_low", Y_ROD0 - 0.4), ("collar_high", Y_ROD1 - 1.0)):
        c = w.add(m, nm, "iron", base=(40, 38, 46), smooth=40)
        ml.collar(c, y, R_COLLAR, 1.5, lip=0.35, sides=14)
    # ---- 4 the black claw / horn tip under the rod: a curved, tapering sweep
    claw = w.add(m, "claw_tip", "obsidian", smooth=45, uv_mode="loft")
    path = []
    n = 14
    for i in range(n + 1):
        t = i / n
        path.append((1.6 * t * t * 1.4, Y_ROD0 - 0.6 - CLAW_LEN * t, 0.0))
    ml.sweep(claw, path, ml.circle(1.0, 10), scales=lambda t: (1.15 * (1 - t) ** 0.8 + 0.03, 1.15 * (1 - t) ** 0.8 + 0.03))
    # ---- 5 the neck of the finial between rod and ring
    stem = w.add(m, "finial_stem", "iron", base=(36, 34, 42), smooth=40)
    ml.lathe(stem, "y", (0, 0, 0), [(Y_ROD1 + 0.4, 0.55), (Y_ROD1 + 1.0, 0.75), (Y_ROD1 + 1.9, 0.5), (Y_RING - RING_R + 0.2, 0.42)], sides=10)
    # ---- 6 the pentagram's ring: a swept torus in the card plane
    ring = w.add(m, "pentagram_ring", "iron", base=(30, 28, 36), smooth=40)
    rp = [(RING_R * math.cos(math.radians(a)), Y_RING + RING_R * math.sin(math.radians(a)), 0.0) for a in range(0, 361, 15)]
    ml.sweep(ring, rp, ml.circle(RING_T, 8), caps=(False, False))
    # ---- 7 the five strokes of the star, each its own raised bar (the star glows)
    star = w.add(m, "pentagram_star", "iron", base=(52, 30, 34), glow=False, smooth=40)
    vs = [(STAR_R * math.cos(math.radians(90 + 72 * i)), Y_RING + STAR_R * math.sin(math.radians(90 + 72 * i))) for i in range(5)]
    for i in range(5):
        a, b = vs[i], vs[(i + 2) % 5]
        ml.bar(star, (a[0], a[1], 0.0), (b[0], b[1], 0.0), 0.34, 0.17, up=(0, 0, 1), taper=0.0)
    # ---- 8 five studs where the star touches the ring
    studs = w.add(m, "pentagram_studs", "iron", base=(44, 40, 52))
    for i in range(5):
        ml.stud(studs, (vs[i][0], vs[i][1], 0.0), 0.34, 0.5, normal=(0, 0, 1), sides=8)
        ml.stud(studs, (vs[i][0], vs[i][1], 0.0), 0.34, 0.5, normal=(0, 0, -1), sides=8)

    # ---- 9 the parchment: a thin sheet swept along x, wavy, with a torn right edge
    sheet = w.add(m, "parchment", "leather", base=(228, 198, 142), wear=0.45, smooth=60)
    N = 44
    x0 = 1.4
    p3 = [(x0 + SHEET_W * i / N, SHEET_Y0 + SHEET_H / 2, sheet_z(SHEET_W * i / N)) for i in range(N + 1)]
    ml.sweep(sheet, p3, ml.rect_ring(SHEET_T, 1.0), scales=lambda t: (1.0, SHEET_H * rag(t)), up=(0, 1, 0))
    # ---- 10 the part rolled round the rod
    roll = w.add(m, "parchment_roll", "leather", base=(196, 164, 112), wear=0.6, smooth=40, uv_mode="loft")
    ml.loft(roll, "y", (0, 0, 0), [(SHEET_Y0 - 0.2, 0.92, 0.92), (SHEET_Y0 + 1.0, 1.0, 1.0), (SHEET_Y0 + SHEET_H - 1.0, 1.0, 1.0), (SHEET_Y0 + SHEET_H + 0.2, 0.92, 0.92)],
            ml.circle(ROLL_R, 16))
    # ---- 11/12 the red-brown border lines along the top and bottom edges (raised, following the wave)
    for nm, fy in (("border_top", SHEET_Y0 + SHEET_H - 0.8), ("border_bottom", SHEET_Y0 + 0.8)):
        b = w.add(m, nm, "cloth", base=(112, 36, 24), wear=0.3)
        segs = 22
        for i in range(segs):
            xa, xb = x0 + 2.0 + (SHEET_W * 0.80) * i / segs, x0 + 2.0 + (SHEET_W * 0.80) * (i + 1) / segs
            for sgn in (1,):
                ml.bar(b, (xa, fy, sheet_z(xa - x0) + SHEET_T / 2), (xb, fy, sheet_z(xb - x0) + SHEET_T / 2), 0.30, 0.07, up=(0, 0, 1))
    # ---- 13 the blood-red wax seal near the right end, with its raised rim
    seal = w.add(m, "seal", "cloth", base=(150, 14, 20), wear=0.2, smooth=40, uv_mode="loft")
    sz = sheet_z(SEAL_X - x0) + SHEET_T / 2
    ml.lathe(seal, "z", (SEAL_X, SEAL_Y, sz - 0.1), [(0.0, SEAL_R * 0.95), (0.35, SEAL_R), (0.6, SEAL_R * 0.92), (0.75, SEAL_R * 0.6), (0.78, 0.0)], sides=16)
    rim = w.add(m, "seal_rim", "cloth", base=(100, 8, 14), wear=0.2)
    ml.collar(rim, sz + 0.1, SEAL_R * 0.62, 0.5, lip=0.14, sides=16, axis="z", origin=(SEAL_X, SEAL_Y, 0.0))
    # ---- 14 the signature: five short raised strokes beside the seal, drawn in dried blood
    sig = w.add(m, "signature", "cloth", base=(124, 12, 18), wear=0.3)
    strokes = [((-4.0, 3.0), (-2.4, -1.8)), ((-2.4, -1.8), (-0.6, 3.4)), ((-0.6, 3.4), (1.0, -2.2)), ((1.0, -2.2), (3.2, 2.4)), ((-4.6, -3.1), (3.8, -3.3))]
    for (a, b) in strokes:
        pa, pb = (SEAL_X - 6.0 + a[0] * 0.55, SEAL_Y + 5.0 + a[1] * 0.55), (SEAL_X - 6.0 + b[0] * 0.55, SEAL_Y + 5.0 + b[1] * 0.55)
        ml.bar(sig, (pa[0], pa[1], sheet_z(pa[0] - x0) + SHEET_T / 2), (pb[0], pb[1], sheet_z(pb[0] - x0) + SHEET_T / 2), 0.22, 0.07, up=(0, 0, 1), taper=0.2)
    # ---- 15 the sigil ring on the rolled end (the circle of runes at the card's left)
    sg = w.add(m, "sigil_ring", "cloth", base=(112, 34, 24), wear=0.3)
    sx, sy = x0 + 8.0, SHEET_Y0 + SHEET_H * 0.5
    sp = [(sx + 5.2 * math.cos(math.radians(a)), sy + 5.2 * math.sin(math.radians(a)), sheet_z(sx + 5.2 * math.cos(math.radians(a)) - x0) + SHEET_T / 2) for a in range(0, 361, 20)]
    ml.sweep(sg, sp, ml.circle(0.13, 6), caps=(False, False))

    # ---- 5 the painted skin and the glow skin
    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)

    icon = os.path.join(w.ICON_DIR, ITEM + ".png")
    m.place_on_sprite(icon, real_m=0.62, fit=True)
    m.set_glow("FFFFFF", 0.10, 0.40, 1.0)
    mesh_path = os.path.join(w.MESH_DIR, ITEM + ".txt")
    nq = m.write_txt(mesh_path, atlas, normals="vertex", comment="generated by tools/relics/wave0_models.py")
    atlas.save(ITEM)
    st = m.stats(atlas)
    print(f"{ITEM}: {len(m.parts)} parts, {n_quads(st)} quads {st['quads']}, length {st['length']:.1f}, atlas {atlas.size}px at {atlas.ppu:.1f} px/cm")
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        ref = w.reference("cef699aad5168e8eee0a0f0df2806504.jpg", (170, 50, 880, 400), ITEM)
        ml.preview(m, ref, os.path.join(prev, ITEM + ".png"), atlas=atlas, normals="vertex")
    return nq


def n_quads(st):
    return sum(st["quads"].values())


def _hash(a, b, seed):
    h = (a.astype(np.int64) * 73856093) ^ (b.astype(np.int64) * 19349663) ^ (seed * 83492791)
    return (h ^ (h >> 13)) & 0x7FFFFFFF


def _paint(m, atlas):
    def runes(c):
        """Columns of runic text on both faces of the sheet, struck-out lines, the sigil circle, edge burn; the ink glows faintly red."""
        A, B = c.A, c.B
        sheet_lo, sheet_hi = SHEET_Y0, SHEET_Y0 + SHEET_H
        inside = (A > x0_ + 10.5) & (A < x0_ + SHEET_W - 1.0) & (B > sheet_lo + 2.2) & (B < sheet_hi - 2.2)
        # vertical lines of runic text, as on the card: each column a thin spine with short ticks and hooks every half centimetre
        cw, ch = 1.25, 0.55
        col = np.floor((A - x0_ - 10.5) / cw)
        row = np.floor((B - sheet_lo - 2.2) / ch)
        fx = ((A - x0_ - 10.5) / cw) % 1.0
        fy = ((B - sheet_lo - 2.2) / ch) % 1.0
        h = _hash(col, row, 7)
        spine = (np.abs(fx - 0.5) < 0.07) & (fy > 0.05) & (fy < 0.95) & (((h >> 3) & 7) != 0)
        tick_l = (np.abs(fy - 0.5) < 0.16) & (fx > 0.12) & (fx < 0.5) & ((h & 1) == 1)
        tick_r = (np.abs(fy - 0.5) < 0.16) & (fx > 0.5) & (fx < 0.88) & ((h & 2) == 2)
        hook = (np.abs((fx - 0.5) + (fy - 0.5)) < 0.16) & (fx > 0.2) & (fx < 0.8) & ((h & 4) == 4)
        gap = (col % 5 == 4)                                           # a gap between texts
        ink = (spine | tick_l | tick_r | hook) & inside & ~gap
        # the struck-out lines (the card's heavy black bars): two columns
        strike = inside & (np.abs(fx - 0.5) < 0.30) & np.isin(col, [6, 17, 28]) & (np.floor((B - sheet_lo) / 2.6) % 2 == 0)
        mask = (ink | strike).astype(np.float32)
        base = c.img[..., :3]
        ink_col = np.array([86, 30, 20], np.float32)
        c.img[..., :3] = base * (1 - 0.85 * mask[..., None]) + ink_col * 0.85 * mask[..., None]
        # the sigil: a ring and an inner star mark at the left of the sheet
        d = np.hypot(A - (x0_ + 8.0), B - (SHEET_Y0 + SHEET_H * 0.5))
        sig = ((np.abs(d - 5.2) < 0.22) | (np.abs(d - 3.6) < 0.14)).astype(np.float32)
        c.img[..., :3] = c.img[..., :3] * (1 - 0.8 * sig[..., None]) + np.array([110, 22, 20], np.float32) * 0.8 * sig[..., None]
        # stains and burnt edges
        n1 = w.noise2(c.img.shape[:2], 11, 5)
        stain = np.clip((n1 - 0.55) * 2.6, 0, 1)
        c.img[..., :3] *= (1 - 0.18 * stain)[..., None]
        edge = np.clip(np.minimum.reduce([B - sheet_lo, sheet_hi - B, x0_ + SHEET_W - A]) / 1.4, 0, 1)
        c.img[..., :3] *= (0.55 + 0.45 * edge)[..., None]
        # the ink glows like embers
        gl = np.maximum(mask * (((h >> 6) & 7) == 0) * 0.6, sig * 0.8)
        out = np.zeros(c.img.shape, np.float32)
        out[..., 0], out[..., 1], out[..., 2] = 255, 70, 40
        out[..., 3] = gl * 200
        c.glow[...] = np.maximum(c.glow, out)
    w.paint_part(atlas, m.part("parchment"), runes)

    def glow_red(strength):
        def f(c):
            w.set_glow(c, (255, 40, 30), strength, core=0.25)
        return f
    w.paint_part(atlas, m.part("seal"), glow_red(0.55))
    w.paint_part(atlas, m.part("signature"), glow_red(0.8))
    w.paint_part(atlas, m.part("pentagram_star"), glow_red(0.9))
    w.paint_part(atlas, m.part("sigil_ring"), glow_red(0.6))
    w.paint_part(atlas, m.part("claw_tip"), lambda c: w.tint(c.img, (90, 70, 84), 0.2))


x0_ = 1.4


if __name__ == "__main__":
    only = sys.argv[1:]
    if not only or "infernal_contract" in only or ITEM in only:
        build()
