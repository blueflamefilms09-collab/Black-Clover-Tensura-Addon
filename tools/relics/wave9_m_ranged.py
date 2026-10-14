"""Wave 9 ranged and flexible weapons as 3D models: the Hyperborean Crossbow (with its draw and loaded states) and the Viper Whip (the owner's model method).

Axis of every model: y runs from the butt / pommel (0) to the muzzle / lash, x across, z through (the flat toward the viewer in hand).
"""
import math
import os
import shutil

import numpy as np

import mesh_lib as ml
import wave9_models as w9
from wave9_models import w

# ================================================================ Hyperborean Crossbow
# 1 measure. Card 736 x 736 (p2/0ab34e9a...): the stock runs from the butt (545, 410) to the faceted head (275, 155): 361 px. A light crossbow's stock is about 78 cm,
# so 0.216 cm per card px. The limbs span from (200, 350) to (470, 75): 385 px = 83 cm (stylised wide; 80 used). The limbs join the stock 12% from its front.
C_STOCK = 78.0                                    # stock length, butt to front tip
C_ATTACH = 66.0                                   # where the limbs join the stock (y)
C_SPAN = 40.0                                     # half span of the limbs
C_STOCK_W, C_STOCK_T = 4.8, 3.6                   # stock section (card 28 px wide): a faceted octagon
C_LIMB_FWD, C_LIMB_BACK = 18.0, 4.0               # how far the limb tips sit ahead of the attach point at rest and when cocked
C_LOCK_Y = 24.0                                   # where the string rests when cocked: 110 px from the butt in the card
C_RAIL_Z = 1.8 + 0.5                              # the string's height above the stock when cocked
C_STATES = {"": (0.0, False), "_pull1": (0.3, False), "_pull2": (0.6, False), "_pull3": (0.9, False), "_charged": (1.0, True)}


def _limb_path(side, f):
    c = C_LIMB_FWD * (1.0 - f) + C_LIMB_BACK * f
    return [(side * (3.0 + (C_SPAN - 3.0) * i / 11.0), C_ATTACH + c * (i / 11.0) ** 2.2, 0.0) for i in range(12)]


def _build_crossbow(f, bolt, parts_only=False):
    """All parts of the crossbow for a draw fraction f (0 = relaxed, 1 = cocked); the same faces in the same order whatever f is (the states share the skin)."""
    P = {}

    def add(name, material, **kw):
        P[name] = ml.Part(name, material=material, **kw)
        return P[name]
    # ---- 1 the stock: a long faceted octagon, fat at the front block and under the lock, slim between
    st = add("stock", "silver", base=(168, 174, 190), smooth=40, uv_mode="loft")
    ml.loft(st, "y", (0, 0, 0), [(2.0, 0.8, 0.8), (12.0, 1.0, 1.0), (30.0, 0.82, 0.9), (52.0, 0.8, 0.88), (C_ATTACH + 2.0, 1.0, 1.0), (C_STOCK - 4.0, 0.9, 0.95)], ml.octagon(C_STOCK_W, C_STOCK_T, 0.9))
    # ---- 2 the engraved lines on the top face: two ridge pairs with the groove painted dark, glowing blue
    inl = add("stock_inlay", "iron", base=(60, 70, 96), wear=0.8)
    ml.ridge_pair(inl, (0.0, 14.0, C_STOCK_T * 0.5 * 0.9), (0.0, 60.0, C_STOCK_T * 0.5 * 0.86), (0, 0, 1), 1.5, 0.16, 0.08, 3.0)
    ml.ridge_pair(inl, (0.0, 14.0, -C_STOCK_T * 0.5 * 0.9), (0.0, 60.0, -C_STOCK_T * 0.5 * 0.86), (0, 0, -1), 1.5, 0.16, 0.08, 3.0)
    # ---- 3 the bolt rail: a raised bar the bolt lies in
    rl = add("rail", "iron", base=(54, 60, 78), wear=0.6)
    ml.bar(rl, (0.0, 22.0, C_STOCK_T * 0.5 * 0.86), (0.0, C_ATTACH + 6.0, C_STOCK_T * 0.5 * 0.9), 1.3, 0.42, up=(0, 0, 1), taper=2.0, section="octagon")
    # ---- 4 the prod mount: the dark block the limbs are seated in
    pm = add("prod_mount", "iron", base=(50, 54, 68), smooth=45)
    ml.loft(pm, "y", (0, 0, 0), [(C_ATTACH - 6.0, 0.8, 0.9), (C_ATTACH - 3.0, 1.0, 1.0), (C_ATTACH + 3.0, 1.1, 1.0), (C_ATTACH + 6.0, 0.7, 0.8)], ml.octagon(7.0, 5.0, 1.4))
    # ---- 5/6 the limbs: dark curved prods, thick at the stock and thin at the tips
    for side, nm in ((-1, "limb_l"), (1, "limb_r")):
        lm = add(nm, "iron", base=(40, 44, 56), smooth=45, uv_mode="loft")
        ml.sweep(lm, _limb_path(side, f), ml.lozenge(1.0, 1.0, 0.3), scales=[(3.4 * (1 - 0.82 * (i / 11.0)) + 0.4, 2.0 * (1 - 0.7 * (i / 11.0)) + 0.35) for i in range(12)])
    # ---- 7 the limb nocks: small caps where the string rides, on both tips
    nk = add("limb_nocks", "silver", base=(180, 190, 208), smooth=55)
    for side in (-1, 1):
        tip = _limb_path(side, f)[-1]
        w9.spike(nk, (tip[0], tip[1], 0.0), (side * 0.7, 0.55, 0.0), 2.4, 0.5, sides=6, belly=0.8)
    # ---- 8 the bands: two collars bound over the limb roots
    bd = add("limb_bands", "silver", base=(150, 158, 176), wear=0.6)
    for side in (-1, 1):
        ml.collar(bd, 8.0 * side, 1.0, 1.4, lip=0.3, sides=8, axis="x", origin=(0, C_ATTACH + C_LIMB_FWD * (1 - f) * (8.0 / 40.0) ** 2.2 * 0.0, 0.0), oval=1.2)
    # ---- 9 the string: two thin glowing cords from the tips to the middle, which slides back to the lock as it is drawn
    for side, nm in ((-1, "string_l"), (1, "string_r")):
        sg = add(nm, "cloth", base=(120, 200, 255), wear=0.1)
        tip = _limb_path(side, f)[-1]
        mid_y = tip[1] * (1.0 - f) + C_LOCK_Y * f
        mid_z = C_RAIL_Z * f
        ml.sweep(sg, [(tip[0], tip[1], 0.0), (tip[0] * 0.5, (tip[1] + mid_y) / 2.0, mid_z * 0.5), (0.0, mid_y, mid_z)], ml.circle(0.16, 5), scales=lambda t: (1.0, 1.0))
    # ---- 10 the front head: the faceted arrow-head cap of the stock (card (285, 155))
    hd = add("front_head", "silver", base=(196, 204, 220), smooth=35)
    ml.lathe(hd, "y", (0, 0, 0), [(C_STOCK - 4.5, 2.3), (C_STOCK - 2.0, 2.6), (C_STOCK + 1.5, 1.7), (C_STOCK + 5.0, 0.0)], sides=6, squash=0.85)
    # ---- 11 the butt: a dark faceted cone ending in a point, with two rings (card (445, 350) to (530, 415))
    bt = add("butt_cone", "iron", base=(52, 56, 70), smooth=35)
    ml.lathe(bt, "y", (0, 0, 0), [(-7.0, 0.0), (-5.0, 1.0), (-1.0, 2.3), (6.0, 2.5), (12.0, 2.3)], sides=8, squash=0.9)
    rg = add("butt_rings", "silver", base=(170, 180, 198), wear=0.6)
    ml.collar(rg, 3.5, 2.7, 1.1, lip=0.28, sides=8)
    ml.collar(rg, 9.5, 2.5, 1.1, lip=0.28, sides=8)
    # ---- 12 the lock: a housing under the string's rest, with a short trigger
    lk = add("lock", "iron", base=(60, 66, 84), smooth=45)
    ml.loft(lk, "y", (0, 0, 0), [(C_LOCK_Y - 5.0, 0.9, 0.9), (C_LOCK_Y - 2.0, 1.1, 1.15), (C_LOCK_Y + 4.0, 1.1, 1.15), (C_LOCK_Y + 6.0, 0.85, 0.9)], ml.octagon(5.4, 4.4, 1.2))
    tg = add("trigger", "silver", base=(200, 208, 224), smooth=60)
    w9.spike(tg, (0.0, C_LOCK_Y - 1.0, -2.2), (0.0, -0.45, -1.0), 3.0, 0.5, sides=6)
    # ---- the bolt: an ice-crystal bolt on the rail (cocked state only): the shaft, the faceted crystal head, three vanes
    if bolt:
        by = 2.7                                                     # the bolt's height above the stock
        y_tail, y_head = C_LOCK_Y - 3.0, C_STOCK + 8.0
        bs = add("bolt_shaft", "crystal", base=(190, 226, 250), smooth=50, uv_mode="loft")
        ml.loft(bs, "y", (0, 0, by), [y_tail, y_head - 4.0], ml.circle(0.5, 6))
        bh = add("bolt_head", "crystal", base=(120, 200, 255), smooth=25)
        ml.gem(bh, (0.0, y_head - 1.2, by), 0.95, long=2.0, sides=6, axis="y")
        bf = add("bolt_vanes", "crystal", base=(170, 220, 255), smooth=60)
        for a in (90.0, 210.0, 330.0):
            r = math.radians(a)
            w9.leaf(bf, (0.0, y_tail + 0.3, by), (0.0, 1.0, 0.0), 4.4, 1.6, 0.12, up=(math.cos(r), 0.0, math.sin(r)))
    return P


def _crossbow_skin(m, atlas):
    w.paint_part(atlas, m.part("stock_inlay"), lambda c: w.set_glow(c, (110, 190, 255), 0.7, core=0.35))
    for nm in ("string_l", "string_r"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (130, 200, 255), 1.0, core=0.6))
    for nm in ("bolt_shaft", "bolt_head", "bolt_vanes"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (150, 220, 255), 0.9, core=0.5))
    w.paint_part(atlas, m.part("limb_nocks"), lambda c: w.set_glow(c, (140, 210, 255), 0.4, core=0.3))

    def prod(c):                                                    # the limbs: dark with a cold blue edge sheen, like the card's bow
        edge = (np.abs(c.S - 0.5) > 0.4)
        w.set_glow(c, (80, 160, 255), 0.22, mask=edge, core=0.2)
    for nm in ("limb_l", "limb_r"):
        w.paint_part(atlas, m.part(nm), prod)


def build_hyperborean_crossbow():
    cocked = ml.Mesh("hyperborean_crossbow")
    for p in _build_crossbow(1.0, True).values():
        cocked.add(p)
    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(cocked)
    atlas.paint(cocked)
    _crossbow_skin(cocked, atlas)
    icon = os.path.join(w.ICON_DIR, "hyperborean_crossbow.png")
    cocked.place_on_sprite(icon, real_m=0.9)
    cocked.set_glow("FFFFFF", 0.12, 0.5, 1.0)
    cocked.finish()
    cfg, glow = cocked.place_cfg, cocked.glow_cfg
    total = 0
    for suffix, (f, bolt) in C_STATES.items():
        parts = _build_crossbow(f, bolt)
        m = ml.Mesh("hyperborean_crossbow" + suffix)
        for name, p in parts.items():
            base = cocked.part(name)
            if len(base.F) != len(p.F):
                raise SystemExit(f"crossbow state {suffix or 'rest'}: part {name} has {len(p.F)} faces, the skin layout has {len(base.F)}")
            p.UV = [list(map(tuple, uv)) for uv in base.UV]
            m.add(p)
        m.place_cfg, m.glow_cfg = cfg, glow
        n = m.write_txt(os.path.join(w.MESH_DIR, "hyperborean_crossbow" + suffix + ".txt"), atlas, normals="vertex", comment="generated by tools/relics/wave9_models.py")
        total = max(total, n)
        print(f"hyperborean_crossbow{suffix}: {len(m.parts)} parts, {n} quads")
    atlas.save("hyperborean_crossbow")
    src = os.path.join(w.ASSETS, "textures", "entity", "weapon")
    for suffix in C_STATES:
        if suffix:                                                  # the loader reads the skin of the mesh's own name: the states carry copies
            for tail in (".png", "_glow.png"):
                shutil.copyfile(os.path.join(src, "hyperborean_crossbow" + tail), os.path.join(src, "hyperborean_crossbow" + suffix + tail))
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        r = w9.ref("p2/Relics of the world part 2/0ab34e9ab8e4b361adf8065e2616c451.jpg", (60, 40, 700, 440), "hyperborean_crossbow")
        ml.preview("hyperborean_crossbow_charged", r, os.path.join(prev, "hyperborean_crossbow.png"), atlas=atlas, normals="vertex")
        ml.preview("hyperborean_crossbow", None, os.path.join(prev, "hyperborean_crossbow_rest.png"), atlas=atlas, normals="vertex")
    if total > w9.MAX_QUADS:
        raise SystemExit("hyperborean_crossbow: over the quad budget")


# ================================================================ Viper Whip
# 1 measure. Card 640 x 604 (p3/e48e2d18...): the whip lies in a flat coil of about two and a half turns round a centre at (290, 215); the handle runs from (335, 330) to the
# green pommel at (430, 395): 118 px; the coil's outer radius is 110 px; the viper's head sits on the inner end at (305, 238). A bullwhip handle is about 30 cm,
# so 0.25 cm per card px: coil outer radius 14 cm, handle 30 cm, lash about 2.4 turns.
V_HANDLE = (0.0, 30.0)                            # the wrapped handle, pommel at y 0
V_COIL_C = (0.0, 44.0)                            # the coil's centre in the x-y plane: the lash leaves the handle at the coil's bottom point
V_R0, V_R1 = 14.0, 5.0                            # outer and inner radius of the coil
V_TURNS = 2.4
V_LASH_R = (1.0, 0.55)                            # the lash's radius at the handle and at the head
V_N = 70                                          # stations along the lash


def _lash_point(t):
    a = math.radians(-90.0 + 360.0 * V_TURNS * t)
    r = V_R0 + (V_R1 - V_R0) * t
    return (V_COIL_C[0] + r * math.cos(a), V_COIL_C[1] + r * math.sin(a), 0.5 * math.sin(t * math.pi * 3.0))


def build_viper_whip():
    m = ml.Mesh("viper_whip")
    # ---- 1 the lash: a long scaled rope that coils; thick near the handle, thin at the head
    ls = w.add(m, "lash", "leather", base=(84, 96, 52), smooth=50, uv_mode="loft")
    pts = [(0.0, V_HANDLE[1] - 1.0, 0.0)] + [_lash_point(i / V_N) for i in range(V_N + 1)]
    ml.sweep(ls, pts, ml.circle(1.0, 8), scales=lambda t: (V_LASH_R[0] + (V_LASH_R[1] - V_LASH_R[0]) * t, V_LASH_R[0] + (V_LASH_R[1] - V_LASH_R[0]) * t))
    # ---- 2 the lattice: diamond scale bands, raised thin rings along the lash (every few stations) so the skin reads as the card's scale pattern
    lt = w.add(m, "lash_scales", "leather", base=(140, 150, 88), wear=0.5)
    for i in range(4, V_N, 5):
        px, py, pz = _lash_point(i / V_N)
        qx, qy, qz = _lash_point(min(1.0, (i + 1) / V_N))
        d = (qx - px, qy - py, qz - pz)
        n = math.sqrt(sum(v * v for v in d)) or 1.0
        r = V_LASH_R[0] + (V_LASH_R[1] - V_LASH_R[0]) * (i / V_N)
        ml.loft(lt, d, (px, py, pz), [(-0.12, r * 1.02), (0.12, r * 1.18), (0.36, r * 1.02)], ml.circle(1.0, 8), caps=(False, False))
    # ---- 3 the handle: dark wrapped leather, slightly swollen in the grip
    hd = w.add(m, "handle", "leather", base=(52, 40, 36), uv_mode="loft")
    ml.loft(hd, "y", (0, 0, 0), [(V_HANDLE[0] + 1.0, 1.25, 1.25), (6.0, 1.6, 1.6), (V_HANDLE[1] - 8.0, 1.5, 1.5), (V_HANDLE[1], 1.2, 1.2)], ml.circle(1.0, 10))
    # ---- 4 the wrap: leather strips spiralling round it as raised bands
    wr = w.add(m, "handle_wrap", "leather", base=(84, 66, 56), wear=0.6)
    for i in range(9):
        ml.collar(wr, 3.0 + i * 2.6, 1.78, 0.9, lip=0.22, sides=10)
    # ---- 5 the ferrule where the lash joins the handle
    fe = w.add(m, "ferrule", "bronze", base=(120, 120, 90), smooth=50)
    ml.collar(fe, V_HANDLE[1] - 3.2, 1.8, 3.6, lip=0.7, sides=10)
    # ---- 6 the pommel: a green gem in a dark cap (the card's green pommel)
    pc = w.add(m, "pommel_cap", "iron", base=(44, 44, 36), smooth=50)
    ml.lathe(pc, "y", (0, 0, 0), [(-1.8, 0.0), (-1.2, 1.2), (0.4, 1.7), (1.8, 1.5)], sides=10)
    pg = w.add(m, "pommel_gem", "crystal", base=(64, 214, 110), smooth=30)
    ml.gem(pg, (0, -1.6, 0), 1.15, long=1.0, sides=6, axis="y")
    # ---- 7 the viper's head: a flattened wedge with a short snout, on the lash's inner end, 8 the eyes, 9 the fangs and the forked tongue
    end = _lash_point(1.0)
    pre = _lash_point(1.0 - 1.0 / V_N)
    dx, dy, dz = end[0] - pre[0], end[1] - pre[1], end[2] - pre[2]
    dn = math.sqrt(dx * dx + dy * dy + dz * dz) or 1.0
    dirv = (dx / dn, dy / dn, dz / dn)
    vh = w.add(m, "viper_head", "leather", base=(56, 168, 84), smooth=50)
    ml.loft(vh, dirv, end, [(-0.3, 0.55, 0.45), (0.8, 1.15, 0.8), (2.4, 1.6, 0.95), (4.0, 1.25, 0.7), (4.9, 0.55, 0.4), (5.2, 0.0, 0.0)], ml.ellipse(1.0, 1.0, 10), up=(0, 0, 1))
    ey = w.add(m, "viper_eyes", "crystal", base=(255, 210, 60), smooth=30)
    for sgn in (1, -1):
        ex, ey_, ez = end[0] + dirv[0] * 2.4, end[1] + dirv[1] * 2.4, end[2] + dirv[2] * 2.4
        ml.stud(ey, (ex + 0.0, ey_ + 0.0, ez + sgn * 0.62), 0.28, 0.2, (0, 0, sgn), sides=6)
    fg = w.add(m, "viper_fangs", "bone", base=(240, 236, 220), smooth=60)
    for sgn in (1, -1):
        w9.spike(fg, (end[0] + dirv[0] * 4.5, end[1] + dirv[1] * 4.5, end[2] + sgn * 0.3), (dirv[0] * 0.4, dirv[1] * 0.4 - 0.5, sgn * 0.5), 1.2, 0.18, sides=5)
    tg = w.add(m, "viper_tongue", "cloth", base=(230, 50, 70), smooth=60)
    tip = (end[0] + dirv[0] * 5.0, end[1] + dirv[1] * 5.0, end[2])
    for sgn in (1, -1):
        ml.sweep(tg, [tip, (tip[0] + dirv[0] * 1.2, tip[1] + dirv[1] * 1.2, tip[2] + sgn * 0.25), (tip[0] + dirv[0] * 2.4, tip[1] + dirv[1] * 2.4, tip[2] + sgn * 0.7)], ml.circle(0.09, 4), scales=lambda t: (1.0, 1.0))

    atlas = ml.Atlas(w9.ATLAS_PX)
    atlas.layout(m)
    atlas.paint(m)

    def scales(c):                                                  # a diamond lattice of lighter lines over olive-brown scales, the card's pattern
        yy, xx = np.mgrid[0:c.img.shape[0], 0:c.img.shape[1]].astype(np.float32)
        a = (xx + yy) % 9.0
        b = (xx - yy) % 9.0
        line = ((a < 1.2) | (b < 1.2)).astype(np.float32)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - 0.45 * line) + np.array([172, 176, 112], np.float32) * 0.45 * line
        w.set_glow(c, (80, 255, 120), 0.10, core=0.1)
    w.paint_part(atlas, m.part("lash"), scales)
    w.paint_part(atlas, m.part("pommel_gem"), lambda c: w.set_glow(c, (80, 255, 130), 1.0, core=0.5))
    w.paint_part(atlas, m.part("viper_eyes"), lambda c: w.set_glow(c, (255, 220, 70), 1.0, core=0.5))
    w.paint_part(atlas, m.part("viper_head"), lambda c: w.set_glow(c, (80, 255, 130), 0.14, core=0.1))
    r = w9.ref("p3/Relics of the world part 3/e48e2d183a584cc835fc238e3257b351.jpg", (150, 90, 480, 430), "viper_whip")
    w9.finish("viper_whip", m, atlas, real_m=0.62, glow=("FFFFFF", 0.12, 0.5, 1.0), ref_png=r)
