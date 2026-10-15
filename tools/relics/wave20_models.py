"""Relics of Many Worlds, part 4, wave 20: the twelve weapons as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave20_models.py [id ...]          (no ids = all twelve; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave20_models.py mjolnir      (also renders the preview beside the card)

Run tools/relics/wave20_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon). Per id it writes
assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png (512 px) and <id>_glow.png.
Units are centimetres, y runs from the butt (0) to the tip, -x is forward (the cutting edge, a hook's inside, the front of a head), z is through the
weapon. Every weapon is built at the natural length of its class (the lead's size tools scale it afterwards) and stays under 14 named parts and
2800 quads. The reference of every weapon is the card image (measurements are written as named constants with the card pixels they came from).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                            # noqa: E402
import wave1_models as w                                         # noqa: E402

CARDS = "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics4"
os.environ.setdefault("RELICS_ZIP", CARDS)


# ================================================================ shared helpers
def pole(m, name, mat, base, y0, y1, r, sides=12, swell=1.0, **kw):
    """A round pole (haft, shaft, grip) from y0 to y1; `swell` is the radius factor at the middle."""
    p = w.add(m, name, mat, base=base, uv_mode="loft", **kw)
    ml.loft(p, "y", (0, 0, 0), [(y0, 1.0, 1.0), ((y0 + y1) / 2, swell, swell), (y1, 1.0, 1.0)], ml.circle(r, sides))
    return p


def bands(m, name, mat, base, ys, r, h=0.9, sides=12, lip=0.2, **kw):
    """Binding rings / ferrules round the pole at the given heights."""
    p = w.add(m, name, mat, base=base, smooth=55, **kw)
    for y in ys:
        ml.collar(p, y, r, h, lip=lip, sides=sides)
    return p


def edge_sec(sgn, ht, hw=0.5, bevel=0.55):
    """Counter-clockwise section of unit width: a cutting edge on the sgn side (-1 = -u), a thick spine on the other."""
    pts = [(hw, 0.0), (hw * (1 - 2 * bevel), ht * 0.7), (-hw, ht), (-hw, -ht), (hw * (1 - 2 * bevel), -ht * 0.7)]
    return pts if sgn > 0 else [(-p[0], p[1]) for p in pts][::-1]


def plate(part, y0, y1, wfn, thick, n=14, bevel=0.5):
    """A flat symmetric plate in the xy plane (a spade, a spear leaf): wfn(t) = width at t in 0..1, 0 at an end closes it to a point."""
    st = [(y0 + (y1 - y0) * k / n, max(wfn(k / n), 0.0), 1.0) for k in range(n + 1)]
    return ml.loft(part, "y", (0, 0, 0), st, ml.bevelled(1.0, thick, bevel))


def curved_plate(part, pts, chord, thick, sgn=-1, bevel=0.55, up=(0, 0, 1)):
    """A plate along a path in the xy plane: chord(t) wide (u = across, in the plane), edge on the sgn side."""
    return ml.sweep(part, pts, edge_sec(sgn, thick / 2, bevel=bevel), scales=lambda t: (max(chord(t), 0.0), 1.0 if t < 0.97 else 0.5), up=up)


def arc(c, r, a0, a1, n, z=0.0, sy=1.0):
    """Points of a circular arc in the xy plane (angles in degrees from +x, counter-clockwise)."""
    return [(c[0] + r * math.cos(math.radians(a0 + (a1 - a0) * k / n)), c[1] + sy * r * math.sin(math.radians(a0 + (a1 - a0) * k / n)), z) for k in range(n + 1)]


def tube(part, pts, r, sides=6, taper=0.0, caps=(True, True)):
    """A round tube along a path (a vine, a cord, a feather quill); `taper` shrinks it linearly to that share at the end."""
    return ml.sweep(part, pts, ml.circle(1.0, sides), scales=lambda t: (r * (1 - (1 - taper) * t), r * (1 - (1 - taper) * t)), caps=caps)


def helix_pts(r, y0, y1, turns, n, phase=0.0, dx=0.0, dz=0.0, rfn=None):
    out = []
    for i in range(n + 1):
        t = i / n
        a = phase + 2 * math.pi * turns * t
        rr = r if rfn is None else rfn(t)
        out.append((dx + rr * math.cos(a), y0 + (y1 - y0) * t, dz + rr * math.sin(a)))
    return out


def dirv(deg):
    """Unit vector at an angle measured from +y toward -x (forward): 0 = up, 90 = straight forward, -90 = back."""
    return (-math.sin(math.radians(deg)), math.cos(math.radians(deg)))


def feather(part, base, deg, length, width, bend, thick=0.5, n=9, z=0.0):
    """One swept feather in the xy plane from `base`, pointing at `deg` (see dirv), curling by `bend` degrees along its length."""
    pts, a = [], deg
    x, y = base
    step = length / n
    for k in range(n + 1):
        pts.append((x, y, z))
        d = dirv(a)
        x, y = x + d[0] * step, y + d[1] * step
        a += bend / n
    ml.sweep(part, pts, ml.lozenge(1.0, thick, 0.0), scales=lambda t: (width * math.sin(math.pi * (0.15 + 0.85 * (1 - t)) * 0.95) ** 0.8 if t > 0.0 else width * 0.5, 1.0 if t < 0.9 else 0.6), up=(0, 0, 1))
    return part


def glow(atlas, m, names, rgb, strength=0.7, core=0.3):
    for nm in names:
        w.paint_part(atlas, m.part(nm), lambda c, rgb=rgb, strength=strength, core=core: w.set_glow(c, rgb, strength, core=core))


def speckle(atlas, m, name, rgb, density=0.02, seed=1):
    """Glow dots scattered over a part (starlight, embers)."""
    rng = np.random.default_rng(seed)

    def fn(c):
        mask = (rng.random(c.img.shape[:2]) < density).astype(np.float32)
        w.set_glow(c, rgb, 1.0, mask=mask, core=0.6)
    w.paint_part(atlas, m.part(name), fn)


def done(item_id, m, atlas, real_m, ref=None, glow_cfg=("FFFFFF", 0.14, 0.5, 1.0)):
    return w.finish(item_id, m, atlas, real_m=real_m, glow=glow_cfg, ref=ref)


def newatlas(m):
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    return atlas


# ================================================================ 1 Demon Shovel
def demon_shovel():
    """Card 736 x 736: the shovel hangs point-down at x = 155: the D handle's top at y = 0, the blade's lowest tooth at y = 726: 726 px for a 1.10 m tool.
    s = card px from the handle end. D handle 0..130, white sleeve at the bar, shaft 130..380 (orange-brown), steel ferrule 380..408, lavender blade 408..726,
    77 px wide at the shoulders (x 78..236 = 158 px at the belly), jagged dark teeth along the rounded tip."""
    TOTAL = 1.10
    K = TOTAL * 100.0 / 726.0                                    # cm per card px (0.152)
    m = ml.Mesh("demon_shovel")
    y = lambda s: s * K
    dh = w.add(m, "d_handle", "bone", base=(236, 238, 246), smooth=55, wear=0.3)          # the white D loop: a bar at the end and two arms joining the shaft
    loop = [(0.0, y(140)), (-6.4, y(100)), (-7.2, y(40)), (-4.0, y(6)), (0.0, y(0) + 0.2), (4.0, y(6)), (7.2, y(40)), (6.4, y(100)), (0.0, y(140))]
    ml.sweep(dh, [(px, py, 0.0) for (px, py) in loop], ml.circle(1.0, 8), scales=lambda t: (1.15, 1.15))
    sl = w.add(m, "d_sleeve", "leather", base=(244, 244, 250), smooth=55, wear=0.2)
    ml.lathe(sl, "x", (0, y(2) + 0.2, 0), [(-5.2, 1.3), (-4.6, 1.7), (4.6, 1.7), (5.2, 1.3)], sides=10)
    sh = pole(m, "shaft", "wood", (170, 92, 56), y(130), y(392), 1.35, 10, swell=1.04)
    bands(m, "shaft_rings", "wood", (96, 54, 36), [y(190), y(255), y(320)], 1.5, 0.5, 10)
    fe = w.add(m, "ferrule", "steel", base=(150, 156, 170), smooth=50)
    ml.lathe(fe, "y", (0, 0, 0), [(y(372), 1.4), (y(380), 2.0), (y(404), 1.9), (y(412), 1.0)], sides=10, squash=0.8)
    bw = w.add(m, "blade", "crystal", base=(196, 112, 238), glow=True, smooth=40)           # the spade: a narrow neck, shoulders, a round belly
    ymax = y(716)

    def wid(t):
        s = y(408) + (ymax - y(408)) * t
        sh_ = float(np.interp(s, [y(408), y(450), y(490), y(560)], [5.0, 5.4, 24.0, 24.5]))
        if s > y(560):
            sh_ = 24.5 * math.sqrt(max(0.0, 1.0 - ((s - y(560)) / (ymax - y(560))) ** 2))
        return sh_
    plate(bw, y(408), ymax, wid, 1.0, n=22, bevel=0.6)
    fl = w.add(m, "foot_flanges", "steel", base=(170, 176, 190), smooth=45)                # the steps at the shoulders
    for sgn in (-1, 1):
        ml.bar(fl, (sgn * 3.2, y(474), 0.5), (sgn * 11.5, y(488), 0.5), 0.9, 0.35, up=(0, 0, 1), taper=1.0)
    rb = w.add(m, "blade_ribs", "crystal", base=(232, 170, 255), glow=True, smooth=50)       # a raised midrib and two curved ribs per face
    for face in (-1, 1):
        ml.bar(rb, (0, y(430), face * 0.4), (0, y(690), face * 0.4), 1.0, 0.28, up=(0, 0, face), taper=3.0)
        for sgn in (-1, 1):
            ml.bar(rb, (sgn * 2.5, y(500), face * 0.4), (sgn * 6.0, y(660), face * 0.4), 0.6, 0.2, up=(0, 0, face), taper=2.0)
    tt = w.add(m, "teeth", "obsidian", base=(40, 18, 56), smooth=0)                         # nine dark jagged teeth round the tip
    for k in range(11):                                          # the teeth stand on the rounded outline
        a = math.radians(-80 + 16 * k)
        cx, cy = 12.25 * math.sin(a), y(560) + (ymax - y(560)) * math.cos(a)
        ml.gem(tt, (cx, cy, 0), 0.85, long=1.9, sides=4, axis=(math.sin(a) / 12.25, math.cos(a) / (ymax - y(560)), 0))
    rm = w.add(m, "blade_rim", "crystal", base=(210, 140, 245), glow=True, smooth=45)       # the glowing rim on the shoulders
    for sgn in (-1, 1):
        ml.bar(rm, (sgn * 12.0, y(495), 0.5), (sgn * 12.0, y(600), 0.5), 0.8, 0.3, up=(0, 0, 1), taper=1.2)
    atlas = newatlas(m)
    glow(atlas, m, ["blade"], (190, 100, 255), 0.45, 0.25)
    glow(atlas, m, ["blade_ribs", "blade_rim"], (235, 170, 255), 0.8, 0.4)
    ref = w.reference("img/784541197571847716.jpg", (60, 0, 260, 736), "demon_shovel")
    done("demon_shovel", m, atlas, TOTAL, ref)


# ================================================================ 2 Dragoon's Halberd
def dragoons_halberd():
    """Card 736 x 736: the halberd lies from the butt (16, 436) to the spear tip (728, 40): 815 px for a 2.10 m halberd. s = px from the butt.
    Gold butt spike 0..30, black pole 30..640, gold band 600..640, gold crescent axe blade 470..600 (swept horn down to s 400), a blue dragon's head 650..735
    with the steel spear 700..815."""
    TOTAL = 2.10
    K = TOTAL * 100.0 / 815.0                                    # cm per px (0.258)
    m = ml.Mesh("dragoons_halberd")
    y = lambda s: s * K
    sp = w.add(m, "butt_spike", "gold", base=(214, 176, 60), smooth=50)
    ml.lathe(sp, "y", (0, 0, 0), [(0, 0.0), (y(14), 0.9), (y(30), 1.7), (y(42), 1.9)], sides=10)
    pole(m, "shaft", "obsidian", (30, 32, 42), y(40), y(676), 1.6, 12, swell=1.0, wear=0.3)
    bands(m, "shaft_bands", "steel", (70, 110, 200), [y(46), y(66), y(430)], 1.95, 0.9, 12)                     # the blue-steel rings near the butt and under the axe
    co = w.add(m, "gold_collar", "gold", base=(222, 180, 64), smooth=50)
    for s0, hh in ((600.0, 38.0), (556.0, 12.0)):
        ml.collar(co, y(s0), 2.35, y(hh), lip=0.35, sides=12)
    so = w.add(m, "axe_socket", "gold", base=(206, 164, 56), smooth=45)
    ml.lathe(so, "y", (0, 0, 0), [(y(450), 1.7), (y(462), 2.5), (y(560), 2.6), (y(572), 1.9)], sides=12)
    ys = [y(400), y(430), y(480), y(520), y(560), y(590), y(612)]                                              # the blade's outer reach in cm
    X = [3.0, 12.0, 19.0, 21.0, 17.0, 9.0, 3.0]
    ab = w.add(m, "axe_blade", "gold", base=(228, 190, 70), smooth=40)
    curved_plate(ab, [(-(1.5 + (x_ - 1.5) / 2), yy, 0.0) for x_, yy in zip(X, ys)], lambda t: (np.interp(t, np.linspace(0, 1, len(X)), X) - 1.5), 0.9, sgn=-1)
    ai = w.add(m, "axe_inlay", "bronze", base=(150, 110, 50), smooth=0)                                        # the darker engraved lines of the blade
    for face in (-1, 1):
        ml.bar(ai, (-5.0, y(440), face * 0.42), (-12.0, y(500), face * 0.42), 0.45, 0.14, up=(0, 0, face), taper=1.0)
        ml.bar(ai, (-12.0, y(500), face * 0.42), (-6.0, y(580), face * 0.42), 0.45, 0.14, up=(0, 0, face), taper=1.0)
    fk = w.add(m, "axe_fluke", "gold", base=(222, 180, 64), smooth=40)                                         # the upper swept spur
    curved_plate(fk, [(-2.0 - 2.4 * math.sin(math.pi * t * 0.55) * 1.0 - 3.2 * t, y(590) + t * y(110), 0.0) for t in np.linspace(0, 1, 9)], lambda t: 6.0 * (1 - t) ** 0.8 + 0.3, 0.8, sgn=-1)
    dh = w.add(m, "dragon_head", "stone", base=(52, 92, 168), smooth=40, uv_mode="loft")                       # the blue dragon head clasping the spear
    ml.loft(dh, "y", (0, 0, 0), [(y(644), 0.7, 0.7), (y(660), 1.5, 1.35), (y(676), 2.2, 1.9), (y(700), 1.7, 1.5), (y(722), 1.0, 0.9), (y(736), 0.55, 0.5)], ml.circle(2.0, 10))
    hr = w.add(m, "dragon_horns", "bone", base=(210, 214, 226), smooth=40)
    for sgn in (-1, 1):
        tube(hr, [(sgn * 1.4, y(676), 0.0), (sgn * 2.3, y(664), -1.0), (sgn * 3.6, y(654), -2.4)], 0.55, 6, 0.1)
    dt = w.add(m, "dragon_teeth", "bone", base=(236, 236, 244), smooth=0)
    for k in range(4):
        for sgn in (-1, 1):
            ml.gem(dt, (sgn * 1.2, y(704 + k * 8), 0.9), 0.28, long=1.5, sides=4, axis=(0, -1, 0))
    eye = w.add(m, "dragon_eyes", "crystal", base=(120, 220, 255), glow=True, smooth=0)
    for sgn in (-1, 1):
        ml.gem(eye, (sgn * 1.6, y(688), 1.5), 0.32, long=1.2, sides=5, axis=(0, 0, 1))
    st = w.add(m, "spear_tip", "steel", base=(196, 202, 214), smooth=40, uv_mode="loft")
    ml.loft(st, "y", (0, 0, 0), [(y(704), 1.0, 1.0), (y(740), 1.0, 1.0), (y(790), 0.55, 0.9), (y(815), 0.0, 0.0)], ml.lozenge(4.2, 1.1, 0.25))
    atlas = newatlas(m)
    glow(atlas, m, ["dragon_eyes"], (120, 220, 255), 1.0, 0.5)
    glow(atlas, m, ["axe_blade"], (255, 224, 140), 0.1, 0.0)
    ref = w.reference("img/784541197578608521.jpg", (0, 20, 736, 460), "dragoons_halberd")
    done("dragoons_halberd", m, atlas, TOTAL, ref, ("FFFFFF", 0.1, 0.6, 1.0))


# ================================================================ 3 Efreeti's Kusarigama
def efreetis_kusarigama():
    """Card 736 x 736: the sickle's handle runs from (372, 592) to (430, 218): 380 px for the 0.65 m handle, the flame blade curls from its top 230 px to the
    right, the iron chain loops down from the handle's foot to an ember counterweight. s = px from the end of the chain (the chain as built hangs straight
    down 0.62 m, links 2.9 cm, in a lazy S; the counterweight 4 cm across)."""
    HANDLE = 65.0
    CH = 60.0                                                    # chain length below the handle's foot
    m = ml.Mesh("efreetis_kusarigama")
    y0 = CH                                                      # y of the handle's foot
    cw = w.add(m, "counterweight", "stone", base=(140, 56, 28), glow=True, smooth=45)       # the glowing ember weight at the chain's end
    w.ellipsoid(cw, (0, 2.8, 0), 2.1, 2.8, 2.1, sides=10, stacks=7)
    cc = w.add(m, "weight_cap", "iron", base=(60, 62, 74), smooth=50)
    ml.lathe(cc, "y", (0, 0, 0), [(5.0, 0.9), (5.8, 1.9), (7.4, 1.5), (8.2, 0.7)], sides=10)
    ch = w.add(m, "chain_links", "iron", base=(86, 88, 100), smooth=50, wear=0.6)           # 22 iron links, alternate links turned a quarter
    n_links = 22
    P = [(2.6 * math.sin(k * 0.5), 8.0 + (y0 - 9.0) * k / n_links, 1.2 * math.sin(k * 0.37 + 1.0)) for k in range(n_links + 1)]
    for k in range(n_links):
        a, b = np.array(P[k]), np.array(P[k + 1])
        d = (b - a) / max(np.linalg.norm(b - a), 1e-6)
        zz = np.array([0.0, 0.0, 1.0])
        n1 = np.cross(d, zz)
        n1 /= max(np.linalg.norm(n1), 1e-6)
        nn = zz if k % 2 else n1
        c = (a + b) / 2
        loop = [tuple(c + d * 1.75 * math.cos(2 * math.pi * i / 10) + nn * 0.95 * math.sin(2 * math.pi * i / 10)) for i in range(10)]
        loop.append(loop[0])
        ml.sweep(ch, loop, ml.circle(0.3, 4), caps=(False, False))
    hd = pole(m, "handle", "leather", (100, 28, 30), y0, y0 + HANDLE, 1.5, 12, swell=1.0)           # the dark red wrapped handle
    wr = w.add(m, "handle_wrap", "leather", base=(24, 12, 14), smooth=55, wear=0.2)               # the black diamond lacing: two crossing spirals
    for ph in (0.0, math.pi):
        ml.sweep(wr, helix_pts(1.62, y0 + 6, y0 + HANDLE - 8, 6, 60, ph), ml.circle(0.18, 4), caps=(True, True))
    bands(m, "handle_rings", "iron", (54, 56, 66), [y0 + 1.0, y0 + HANDLE * 0.5, y0 + HANDLE - 7.0], 1.75, 0.8, 12)
    ft = w.add(m, "foot_ring", "iron", base=(70, 72, 84), smooth=55)                                # the ring the chain hangs from
    ml.sweep(ft, [(1.3 * math.cos(2 * math.pi * i / 12), y0 - 0.5 + 1.3 * math.sin(2 * math.pi * i / 12), 0.0) for i in range(13)], ml.circle(0.34, 5), caps=(False, False))
    so = w.add(m, "blade_socket", "iron", base=(60, 62, 74), smooth=45)
    ml.lathe(so, "y", (0, 0, 0), [(y0 + HANDLE - 6.0, 1.7), (y0 + HANDLE - 4.0, 2.3), (y0 + HANDLE + 5.0, 2.1), (y0 + HANDLE + 7.0, 1.2)], sides=10)
    top = y0 + HANDLE + 5.0
    bl = w.add(m, "sickle_blade", "crystal", base=(255, 128, 30), glow=True, smooth=40)           # the flame sickle: an arc, edge on the inside, orange to yellow
    cen = (-16.0, top + 3.0)
    path = [(cen[0] + 16.0 * math.cos(math.radians(a)), cen[1] + 16.0 * math.sin(math.radians(a)), 0.0) for a in np.linspace(0, 160, 16)]
    curved_plate(bl, path, lambda t: 5.2 * (1 - t) ** 0.7 + 0.2 if t > 0.12 else 3.6 + 12 * t, 1.0, sgn=-1)
    bk = w.add(m, "blade_back", "obsidian", base=(40, 26, 30), smooth=0)                          # the dark ridged spine and the notch at the base
    for k in range(6):
        a = math.radians(14 + 22 * k)
        ml.gem(bk, (cen[0] + 17.4 * math.cos(a), cen[1] + 17.4 * math.sin(a), 0), 0.55, long=1.5, sides=4, axis=(math.cos(a), math.sin(a), 0))
    ml.gem(bk, (1.3, top + 3.0, 0), 0.8, long=2.2, sides=4, axis=(1, 0.4, 0))
    atlas = newatlas(m)
    glow(atlas, m, ["sickle_blade"], (255, 140, 30), 0.65, 0.4)
    glow(atlas, m, ["counterweight"], (255, 150, 40), 1.0, 0.5)
    speckle(atlas, m, "chain_links", (255, 130, 30), 0.012, 4)
    ref = w.reference("img/784541197571732555.jpg", (130, 120, 600, 460), "efreetis_kusarigama")
    done("efreetis_kusarigama", m, atlas, 1.50, ref, ("FFFFFF", 0.16, 0.5, 1.0))


# ================================================================ katanas (shared)
def katana(m, total, sori, blade_w, blade_t, blade_mat, blade_base, grip_base, tsuba, gold=False, cord=None):
    """The shared katana: pommel, wrapped grip, tsuba, habaki, a blade swept along its curve toward the edge (-x). Returns the y of the tsuba and the blade path."""
    grip_len = 0.25 * total
    s_ts = grip_len + 1.0
    s_b0 = s_ts + 3.4
    metal = (216, 172, 62) if gold else (50, 50, 60)
    pm = w.add(m, "pommel", "gold" if gold else "iron", base=metal, smooth=55)
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 1.2), (0.7, 1.9), (2.4, 1.85), (3.2, 1.45)], sides=12, squash=0.75)
    gp = pole(m, "grip", "leather", grip_base, 3.0, grip_len, 1.5, 12, swell=1.06)
    ml.loft(w.add(m, "grip_core", "leather", base=(24, 18, 20), smooth=55, uv_mode="loft"), "y", (0, 0, 0), [(3.2, 1.0, 1.0), (grip_len - 0.2, 1.0, 1.0)], ml.circle(1.38, 8))
    fu = w.add(m, "grip_collar", "gold" if gold else "iron", base=metal, smooth=55)
    ml.lathe(fu, "y", (0, 0, 0), [(grip_len - 2.2, 1.45), (grip_len - 1.8, 1.8), (grip_len + 0.2, 1.8), (grip_len + 0.6, 1.4)], sides=12, squash=0.8)
    ts = w.add(m, "tsuba", "gold" if gold else "iron", base=metal, smooth=45)
    if tsuba == "round":
        ml.lathe(ts, "y", (0, 0, 0), [(s_ts - 0.4, 3.4), (s_ts - 0.4, 3.9), (s_ts + 0.3, 3.9), (s_ts + 0.3, 3.4)], sides=16)
        ml.lathe(ts, "y", (0, 0, 0), [(s_ts - 0.5, 0.2), (s_ts - 0.5, 3.9), (s_ts + 0.45, 3.9), (s_ts + 0.45, 0.2)], sides=16)
    else:
        for k in range(8):                                       # a lobed guard: eight petals round a plate
            a = math.pi * 2 * k / 8
            w.ellipsoid(ts, (3.2 * math.cos(a), s_ts, 3.2 * math.sin(a) * 0.9), 1.5, 0.55, 1.5, sides=8, stacks=5)
        ml.lathe(ts, "y", (0, 0, 0), [(s_ts - 0.5, 0.2), (s_ts - 0.5, 3.6), (s_ts + 0.5, 3.6), (s_ts + 0.5, 0.2)], sides=16)
    hb = w.add(m, "habaki", "silver" if not gold else "gold", base=(190, 194, 206) if not gold else metal, smooth=50)
    ml.lathe(hb, "y", (0, 0, 0), [(s_ts + 0.4, 1.3), (s_ts + 0.9, 1.65), (s_b0 + 1.3, 1.45), (s_b0 + 1.7, 1.0)], sides=10, squash=0.5)
    L = total - s_b0
    n = 18
    path = [(-sori * (k / n) ** 2.2, s_b0 + L * k / n, 0.0) for k in range(n + 1)]
    bl = w.add(m, "blade", blade_mat, base=blade_base, smooth=40, uv_mode="loft")
    kissaki = 0.9

    def chord(t):
        base = blade_w * (1.0 - 0.18 * t)
        if t > kissaki:
            base *= max(0.0, 1.0 - ((t - kissaki) / (1 - kissaki)) ** 1.5)
        return base
    ml.sweep(bl, path, edge_sec(-1, blade_t / 2, hw=0.5, bevel=0.5), scales=lambda t: (max(chord(t), 0.0), 1.0 if t < 0.94 else max(0.25, 1 - (t - 0.94) * 12)), up=(0, 0, 1))
    sp = w.add(m, "blade_spine", "steel", base=(120, 126, 140), smooth=50)
    ml.sweep(sp, [(p[0] + blade_w * 0.46, p[1], 0.0) for p in path[:15]], ml.circle(1.0, 6), scales=lambda t: (0.2 * (1 - 0.7 * t), 0.5 * blade_t * (1 - 0.5 * t)))
    for face, nm in ((1, "hamon_f"), (-1, "hamon_b")):
        hm = w.add(m, nm, "silver", base=(210, 214, 226), wear=0.6, smooth=0)                    # the temper line: a raised ridge along the edge side of each face
        pts = [(p[0] - blade_w * 0.2 * (1 - 0.3 * i / n), p[1], face * blade_t * 0.5) for i, p in enumerate(path[1:15], 1)]
        for a_, b_ in zip(pts[:-1:3], pts[3::3]):
            ml.bar(hm, a_, b_, 0.22, 0.07, up=(0, 0, face), taper=0.3)
    return s_ts, s_b0, grip_len, path


def diamond_wrap(m, name, base, r, y0, y1, turns=9, rad=0.2):
    """The crossed lacing of a katana grip (tsuka-ito): two spirals of opposite hand over the core."""
    p = w.add(m, name, "leather", base=base, smooth=55, wear=0.2)
    for ph, sgn in ((0.0, 1), (math.pi, 1)):
        ml.sweep(p, helix_pts(r, y0, y1, turns, 8 * turns, ph), ml.circle(rad, 5), caps=(True, True))
        ml.sweep(p, helix_pts(r, y0, y1, -turns, 8 * turns, ph), ml.circle(rad, 5), caps=(True, True))
    return p


# ================================================================ 4 Ilmare Gin, The Starlight Silver
def ilmare_gin_the_starlight_silver():
    """Card 640 x 828: the katana lies from the scabbard's end (50, 676) to the point (574, 466): 565 px for a 1.05 m sword. Black hilt and scabbard end,
    a midnight-blue blade speckled with starlight points; no visible guard but a plain round black tsuba. Grip 25% of the length, sori 2.0 cm toward the edge."""
    TOTAL = 1.05
    m = ml.Mesh("ilmare_gin_the_starlight_silver")
    katana(m, TOTAL * 100.0, 2.0, 3.0, 0.8, "obsidian", (22, 32, 84), (24, 24, 30), "round")
    diamond_wrap(m, "grip_lacing", (46, 48, 60), 1.58, 6.0, 24.0, 5, 0.16)
    cv = w.add(m, "star_inlay", "crystal", base=(190, 225, 255), glow=True, smooth=0)             # a line of little raised stars along the blade's flat
    for k in range(10):
        ml.gem(cv, (-0.2 - 0.12 * k, 36.0 + k * 6.4, 0.46), 0.26 + 0.05 * (k % 3), long=1.4, sides=4, axis=(0, 0, 1))
    atlas = newatlas(m)
    speckle(atlas, m, "blade", (200, 230, 255), 0.03, 7)
    glow(atlas, m, ["star_inlay"], (210, 235, 255), 1.0, 0.6)
    glow(atlas, m, ["blade"], (70, 120, 255), 0.07, 0.0)
    ref = w.reference("img/784541197571847338.jpg", (30, 440, 620, 700), "ilmare_gin")
    done("ilmare_gin_the_starlight_silver", m, atlas, TOTAL, ref, ("FFFFFF", 0.18, 0.5, 1.0))


# ================================================================ 5 Katana of Carnage
def katana_of_carnage():
    """Card 735 x 479: the katana lies from the hilt's end (150, 168) to the point (655, 460): 585 px for a 1.05 m sword (sori about 3.4 cm toward the edge:
    the most curved of the three). Black guard, a crimson diamond-wrapped hilt with a black cord and a small blood-stained blade charm, steel blade."""
    TOTAL = 1.05
    m = ml.Mesh("katana_of_carnage")
    s_ts, s_b0, grip_len, path = katana(m, TOTAL * 100.0, 3.4, 3.1, 0.85, "steel", (178, 184, 196), (150, 20, 44), "round")
    diamond_wrap(m, "grip_lacing", (30, 10, 16), 1.58, 5.0, grip_len - 3.0, 7, 0.17)
    kd = w.add(m, "kashira_cord", "cloth", base=(22, 22, 28), smooth=55)                          # the black cord from the pommel in a loop and a tail
    pts = [(1.0 * math.sin(t * 5.0), 0.8 - 14.0 * t, 1.2 + 6.0 * math.sin(t * 3.0) * t) for t in np.linspace(0, 1, 14)]
    tube(kd, pts, 0.28, 5, 0.8)
    ch = w.add(m, "blood_charm", "bone", base=(210, 196, 190), smooth=40)                         # the little curved blade charm at the cord's end
    cx, cy, cz = pts[-1]
    curved_plate(ch, [(cx + 0.9 * math.sin(math.pi * 0.6 * t), cy - 4.8 * t, cz) for t in np.linspace(0, 1, 7)], lambda t: 1.5 * (1 - t) + 0.2, 0.3, sgn=-1)
    bs = w.add(m, "blood_stain", "crystal", base=(170, 18, 30), glow=True, smooth=0)               # red crystal beads on the charm and along the guard
    ml.gem(bs, (cx, cy - 1.0, cz + 0.2), 0.3, long=1.2, sides=5, axis=(0, 1, 0))
    for sgn in (-1, 1):
        ml.gem(bs, (sgn * 3.3, s_ts, 0.5), 0.35, long=1.1, sides=5, axis=(sgn, 0, 0))
    atlas = newatlas(m)
    glow(atlas, m, ["blood_stain"], (255, 40, 60), 1.0, 0.4)
    glow(atlas, m, ["blade"], (255, 70, 90), 0.05, 0.0)
    ref = w.reference("img/784541197571850538.jpg", (120, 140, 700, 470), "katana_of_carnage")
    done("katana_of_carnage", m, atlas, TOTAL, ref, ("FFFFFF", 0.14, 0.5, 1.0))


# ================================================================ 6 Katana of the Crimson Shogun
def katana_of_the_crimson_shogun():
    """Card 736 x 736: the sword lies from the pommel (352, 668) to the point (650, 100): 641 px for a 1.10 m long katana (almost straight: sori 0.8 cm).
    Folded-steel grain along the blade (painted as streaks), a crimson diamond-wrapped hilt with gold fuchi, kashira and a small gold guard."""
    TOTAL = 1.10
    m = ml.Mesh("katana_of_the_crimson_shogun")
    s_ts, s_b0, grip_len, path = katana(m, TOTAL * 100.0, 0.8, 3.2, 0.85, "steel", (180, 186, 198), (110, 22, 44), "lobed", gold=True)
    diamond_wrap(m, "grip_lacing", (60, 10, 26), 1.58, 5.0, grip_len - 3.0, 8, 0.17)
    gm = w.add(m, "gold_studs", "gold", base=(226, 184, 70), smooth=0)                              # the gold menuki under the lacing
    for k in range(4):
        ml.gem(gm, (0.0, 7.0 + k * 4.2, 1.7), 0.38, long=1.1, sides=4, axis=(0, 0, 1))
    atlas = newatlas(m)

    def fold(c):                                                 # folded-steel grain: dark streaks along the blade
        rng = np.random.default_rng(11)
        rows = (np.sin(c.S * 55.0 + rng.random() * 3) * 0.5 + 0.5) * (np.sin(c.T * 3.0 * 40 + c.S * 9) * 0.5 + 0.5)
        c.img[..., :3] *= (1.0 - 0.28 * rows[..., None])
    w.paint_part(atlas, m.part("blade"), fold)
    glow(atlas, m, ["gold_studs"], (255, 220, 120), 0.5, 0.2)
    ref = w.reference("img/784541197578458196.jpg", (320, 80, 700, 700), "katana_crimson_shogun")
    done("katana_of_the_crimson_shogun", m, atlas, TOTAL, ref, ("FFFFFF", 0.1, 0.6, 1.0))


# ================================================================ 7 Lightworker's Scepter
def lightworkers_scepter():
    """Card 640 x 800: the scepter lies from the crystal pommel (120, 262) to the top of the crystals (524, 92): 438 px for a 1.55 m scepter. White-wrapped
    gold shaft (bronze collar at 40 px), a pale blue vine spiralling up it, a spread of pale-blue feathered wings and a cluster of glittering crystal shards."""
    TOTAL = 1.55
    K = TOTAL * 100.0 / 438.0
    m = ml.Mesh("lightworkers_scepter")
    y = lambda s: s * K
    pc = w.add(m, "pommel_crystal", "crystal", base=(210, 240, 255), glow=True, smooth=0)
    ml.gem(pc, (0, 3.2, 0), 1.5, long=2.2, sides=6, axis=(0, 1, 0))
    pole(m, "shaft", "gold", (226, 196, 120), y(30), y(360), 1.3, 12)
    wrp = w.add(m, "white_wrap", "cloth", base=(244, 244, 250), smooth=55, wear=0.2)               # the white cloth wound round the shaft
    for ph in (0.0, math.pi):
        ml.sweep(wrp, helix_pts(1.5, y(44), y(190), 9, 54, ph), ml.circle(0.3, 4), caps=(True, True))
    cl = w.add(m, "bronze_collars", "bronze", base=(190, 130, 62), smooth=55)
    for s0, r in ((20.0, 1.9), (36.0, 1.7), (196.0, 1.8), (352.0, 1.9)):
        ml.collar(cl, y(s0), r, 1.5, lip=0.4, sides=12)
    vn = w.add(m, "vine", "crystal", base=(130, 214, 240), glow=True, smooth=55)                    # the pale blue vine spiralling from the collar to the wings, twice round
    ml.sweep(vn, helix_pts(2.2, y(200), y(368), 3.2, 44, 0.5, rfn=lambda t: 2.2 + 0.3 * math.sin(t * 9)), ml.circle(0.34, 4), scales=lambda t: (1 - 0.5 * t, 1 - 0.5 * t))
    leaves = w.add(m, "vine_leaves", "crystal", base=(170, 230, 250), glow=True, smooth=40)
    for k in range(5):
        a = 0.5 + 2 * math.pi * 3.2 * (k + 0.5) / 5.0
        yy = y(200) + (y(368) - y(200)) * (k + 0.5) / 5.0
        feather(leaves, (2.3 * math.cos(a), yy), 30 + 40 * (k % 2), 4.8, 1.7, 40, 0.3, 4, z=2.3 * math.sin(a))
    base_top = (0.0, y(362))
    wl = w.add(m, "wings_forward", "crystal", base=(140, 214, 240), glow=True, smooth=40)           # three feathers a side, spread up and out, curling
    wr_ = w.add(m, "wings_back", "crystal", base=(120, 200, 236), glow=True, smooth=40)
    for k, (deg, ln, wd, bend) in enumerate(((22, 25, 6.0, 26), (48, 22, 5.4, 30), (74, 17, 4.6, 32))):
        feather(wl, (-0.8, base_top[1] + 1.0 + k * 0.7), deg, ln, wd, bend, 0.45, 10)
        feather(wr_, (0.8, base_top[1] + 1.0 + k * 0.7), -deg, ln * 0.92, wd * 0.95, -bend, 0.45, 10)
    cr = w.add(m, "crystal_cluster", "crystal", base=(228, 246, 255), glow=True, smooth=0)         # the glittering shards fanned above the wings
    for k, (ang, ln, r) in enumerate(((-28, 11.0, 1.5), (-10, 14.0, 1.9), (8, 17.0, 2.2), (24, 13.0, 1.7), (40, 10.0, 1.4), (14, 9.0, 1.3), (-18, 8.0, 1.1))):
        d = dirv(ang)
        ml.gem(cr, (d[0] * ln * 0.55, base_top[1] + 7.0 + d[1] * ln * 0.55, (k - 3) * 0.5), r, long=ln / (2.0 * r) * 0.9, sides=6, axis=(d[0], d[1], 0.0))
    ct = w.add(m, "crystal_core", "crystal", base=(190, 232, 255), glow=True, smooth=45)
    w.ellipsoid(ct, (0, base_top[1] + 4.0, 0), 2.2, 3.0, 2.0, sides=10, stacks=7)
    atlas = newatlas(m)
    glow(atlas, m, ["pommel_crystal", "crystal_cluster", "crystal_core"], (170, 226, 255), 0.7, 0.12)
    glow(atlas, m, ["vine", "vine_leaves", "wings_forward", "wings_back"], (120, 205, 245), 0.4, 0.15)
    ref = w.reference("img/784541197571848451.jpg", (90, 70, 560, 290), "lightworkers_scepter")
    done("lightworkers_scepter", m, atlas, TOTAL, ref, ("FFFFFF", 0.2, 0.5, 1.0))


# ================================================================ 8 Mjolnir
def mjolnir():
    """Card 736 x 920: the hammer lies from the handle's end (550, 596) to the head's far corner (110, 190): head block about 270 x 190 px (the head's
    long side across the handle), handle 250 px, whole 600 px: the head is 0.30 of the 0.70 m hammer: 21 cm wide, 12 cm high, 11 cm deep, a 40 cm handle.
    Interlaced Norse knotwork over the grey steel, framing bands, a leather-wrapped handle with dark rings and a round cap."""
    TOTAL = 0.70
    m = ml.Mesh("mjolnir")
    HEAD_Y0, HEAD_Y1, HX, HZ = 44.0, 57.0, 10.5, 5.6
    hd = pole(m, "handle", "leather", (116, 66, 50), 3.0, 43.0, 1.9, 12, swell=1.05)
    bands(m, "handle_rings", "leather", (52, 28, 26), [6.0 + 4.1 * k for k in range(8)], 2.05, 1.6, 12, lip=0.5)
    pm = w.add(m, "pommel_cap", "iron", base=(120, 82, 66), smooth=50)
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 1.5), (0.6, 2.4), (2.2, 2.55), (3.2, 2.0)], sides=12)
    sl = w.add(m, "strap_loop", "leather", base=(70, 40, 32), smooth=55)                              # a wrist loop through the cap
    ml.sweep(sl, [(0.3 * math.sin(a), -0.5 - 5.0 * (1 - math.cos(a)) * 0.9, 2.2 * math.sin(a * 0.5)) for a in np.linspace(0, 2 * math.pi, 14)], ml.circle(0.3, 5), caps=(False, False))
    co = w.add(m, "head_collar", "iron", base=(80, 86, 98), smooth=45)
    ml.lathe(co, "y", (0, 0, 0), [(40.5, 2.0), (41.0, 3.4), (43.0, 3.6), (HEAD_Y0 + 0.4, 3.2)], sides=14)
    hb = w.add(m, "head_block", "stone", base=(132, 144, 158), smooth=30, wear=0.6)
    ml.box(hb, (-HX, HEAD_Y0, -HZ), (HX, HEAD_Y1, HZ), chamfer=0.9)
    fr = w.add(m, "end_frames", "steel", base=(150, 160, 172), smooth=35)                             # the framing blocks on the striking faces
    for sgn in (-1, 1):
        ml.box(fr, (sgn * (HX - 1.3), HEAD_Y0 - 0.25, -HZ - 0.25), (sgn * (HX + 0.3), HEAD_Y1 + 0.25, HZ + 0.25), chamfer=0.7)
    kn = w.add(m, "knotwork", "steel", base=(96, 106, 120), smooth=0, wear=0.8)                       # the interlaced knotwork: nested loops and a woven cross on both faces
    kz = HZ + 0.08
    x0, x1, ya, yb = -HX + 2.4, HX - 2.4, HEAD_Y0 + 1.4, HEAD_Y1 - 1.4
    for face in (-1, 1):
        for inset in (0.0, 1.5, 3.0):
            a, b, c, d = x0 + inset, x1 - inset, ya + inset * 0.7, yb - inset * 0.7
            for p0, p1 in (((a, c), (b, c)), ((b, c), (b, d)), ((b, d), (a, d)), ((a, d), (a, c))):
                ml.bar(kn, (p0[0], p0[1], face * kz), (p1[0], p1[1], face * kz), 0.5, 0.2, up=(0, 0, face), taper=0.3)
        mx, my = (x0 + x1) / 2, (ya + yb) / 2
        for sgn in (-1, 1):
            ml.bar(kn, (mx, my, face * kz), (mx + sgn * 5.8, my + 2.8, face * kz), 0.5, 0.2, up=(0, 0, face), taper=0.3)
            ml.bar(kn, (mx, my, face * kz), (mx + sgn * 5.8, my - 2.8, face * kz), 0.5, 0.2, up=(0, 0, face), taper=0.3)
            ml.bar(kn, (mx + sgn * 2.8, ya + 0.8, face * kz), (mx + sgn * 2.8, yb - 0.8, face * kz), 0.5, 0.2, up=(0, 0, face), taper=0.3)
    for yy, upv in ((HEAD_Y1 + 0.08, 1), (HEAD_Y0 - 0.08, -1)):    # the lines along the top and bottom
        for zz in (-2.6, 0.0, 2.6):
            ml.bar(kn, (-HX + 2.4, yy, zz), (HX - 2.4, yy, zz), 0.5, 0.2, up=(0, upv, 0), taper=0.3)
    rv = w.add(m, "head_rivets", "iron", base=(170, 180, 194), smooth=0)
    for sx in (-1, 1):
        for sy in (HEAD_Y0 + 1.2, HEAD_Y1 - 1.2):
            for face in (-1, 1):
                ml.rivet(rv, (sx * (HX - 0.9), sy, face * (HZ + 0.1)), 0.34, normal=(0, 0, face))
    atlas = newatlas(m)
    glow(atlas, m, ["knotwork"], (170, 200, 255), 0.18, 0.1)
    ref = w.reference("img/784541197571844939.jpg", (60, 100, 600, 640), "mjolnir")
    done("mjolnir", m, atlas, TOTAL, ref, ("FFFFFF", 0.14, 0.4, 0.9))


# ================================================================ 9 Overprotective Halberd
def overprotective_halberd():
    """Card 640 x 1500: the halberd lies from the butt (55, 575) to the spear tip (580, 55): 740 px for a 2.00 m halberd. s = px from the butt: a grey
    chevron-patterned grip 0..222 (silver bands at 0, 214), dark wood shaft 222..480, a silver band at 452 and the collar 470..500, the ragged axe blade on the
    far side 440..600 with an eyelet, a swept spur on the near side 560..640, the spear leaf 500..740 (4.8 cm at its widest), long red ribbons from the collar."""
    TOTAL = 2.00
    K = TOTAL * 100.0 / 740.0
    m = ml.Mesh("overprotective_halberd")
    y = lambda s: s * K
    gp = pole(m, "grip", "steel", (150, 152, 160), y(0), y(222), 1.85, 8, swell=1.0)
    ch = w.add(m, "grip_chevrons", "iron", base=(60, 62, 72), smooth=0)                              # the chevron pattern on the grip: raised V bars
    for k in range(7):
        s0 = 24.0 + k * 28.0
        for sgn in (-1, 1):
            ml.bar(ch, (0, y(s0 + 20), 1.9), (sgn * 1.6, y(s0), 1.9), 0.5, 0.14, up=(0, 0.2, 1), taper=0.3)
    pole(m, "shaft", "wood", (88, 60, 54), y(222), y(500), 1.55, 10, swell=1.0)
    bands(m, "silver_bands", "silver", (200, 204, 216), [y(0), y(214), y(452), y(520)], 1.95, 1.2, 10, lip=0.35)
    cl = w.add(m, "collar", "silver", base=(176, 180, 192), smooth=50)
    ml.lathe(cl, "y", (0, 0, 0), [(y(470), 1.6), (y(480), 2.3), (y(520), 2.4), (y(532), 1.5)], sides=10)
    ub = w.add(m, "axe_upper", "steel", base=(150, 156, 168), smooth=40, wear=0.8)                      # the ragged axe blade, as an upper arm and a lower arm round an eyelet
    curved_plate(ub, [(-2.0 - 1.0 * k - 12.0 * math.sin(k / 7 * math.pi * 0.62), y(540) + k * 4.4, 0.0) for k in range(8)], lambda t: 13.0 * (1 - 0.75 * t) + 1.0, 1.0, sgn=-1)
    lb = w.add(m, "axe_lower", "steel", base=(142, 148, 160), smooth=40, wear=0.8)
    curved_plate(lb, [(-2.0 - 13.0 * math.sin(k / 9 * math.pi * 0.78), y(536) - k * 4.0, 0.0) for k in range(10)], lambda t: 11.0 * (1 - 0.8 * t) + 1.0, 1.0, sgn=-1)
    rm = w.add(m, "axe_rim", "silver", base=(206, 210, 222), smooth=45)                                  # the rim that closes the eyelet on the outer side
    ml.bar(rm, (-12.5, y(540) + 4.0, 0.0), (-13.0, y(536) - 10.0, 0.0), 1.3, 0.6, up=(0, 0, 1), taper=1.0, section="octagon")
    tt = w.add(m, "ragged_edge", "iron", base=(90, 94, 106), smooth=0)                                   # the nicks and teeth of the ragged edge
    for k, yy in enumerate((y(540) + 28.0, y(540) + 20.0, y(536) - 24.0, y(536) - 31.0, y(536) - 17.0)):
        ml.gem(tt, (-13.4 + 0.5 * (k % 2), yy, 0.0), 0.7, long=1.7, sides=4, axis=(-1, 0.2 * (k - 2), 0))
    fk = w.add(m, "spur", "steel", base=(150, 156, 168), smooth=40)                                      # the swept spur on the other side
    curved_plate(fk, [(2.0 + 9.0 * math.sin(k / 9 * math.pi * 0.62), y(556) + k * 3.6, 0.0) for k in range(10)], lambda t: 7.0 * (1 - t) + 0.4, 0.9, sgn=1)
    sp = w.add(m, "spear", "steel", base=(176, 182, 196), smooth=40, uv_mode="loft")
    ml.loft(sp, "y", (0, 0, 0), [(y(520), 1.0, 1.0), (y(560), 1.5, 1.0), (y(640), 4.8, 1.0), (y(700), 2.6, 1.0), (y(740), 0.0, 0.0)], ml.lozenge(1.0, 1.2, 0.15))
    rb = w.add(m, "spear_rib", "silver", base=(210, 214, 226), smooth=0)
    for face in (-1, 1):
        ml.bar(rb, (0, y(540), face * 0.5), (0, y(724), face * 0.4), 0.5, 0.14, up=(0, 0, face), taper=6.0)
    rg = w.add(m, "ribbons", "cloth", base=(206, 32, 40), smooth=55, wear=0.2)                          # three red ribbons tied under the collar, flying back
    for k, (dz, ln, wv) in enumerate(((0.0, 78.0, 1.0), (1.5, 62.0, -1.0), (-1.5, 50.0, 0.6))):
        tie = y(476) - k * 2.0
        pts = [(1.0 + 9.0 * t + 2.0 * wv * math.sin(t * 7.0), tie - ln * t ** 1.1, dz + 6.0 * math.sin(t * 5.0 + k)) for t in np.linspace(0, 1, 16)]
        ml.sweep(rg, pts, ml.rect_ring(1.0, 0.1), scales=lambda t: (2.6 * (1 - 0.4 * t), 1.0), up=(1, 0, 0), caps=(True, True))
    atlas = newatlas(m)
    glow(atlas, m, ["silver_bands", "axe_rim"], (200, 220, 255), 0.15, 0.1)
    ref = w.reference("img/DND Silly Items.jpg", (30, 30, 620, 600), "overprotective_halberd")
    done("overprotective_halberd", m, atlas, TOTAL, ref, ("FFFFFF", 0.12, 0.4, 0.9))


# ================================================================ 10 Phoenix Flame
def phoenix_flame():
    """Card 736 x 736: the halberd lies from the butt (690, 466) to the swept beak's tip (60, 70): the shaft is 560 px of it, the phoenix head 180 px: 740 px
    for a 2.10 m polearm, so the slim copper-red shaft is 1.60 m and the phoenix 0.50 m. Purple bindings at 3 places, a swept-wing blade in orange-gold flame."""
    TOTAL = 2.10
    m = ml.Mesh("phoenix_flame")
    HEAD = 160.0                                                 # where the phoenix begins (cm from the butt)
    bt = w.add(m, "butt_tip", "bronze", base=(150, 70, 50), smooth=50)
    ml.lathe(bt, "y", (0, 0, 0), [(0, 0.0), (2.5, 0.9), (4.5, 1.4), (6.0, 1.0)], sides=8)
    pole(m, "shaft", "wood", (170, 66, 44), 5.0, HEAD, 1.15, 10, swell=1.03, wear=0.3)
    bands(m, "bindings", "cloth", (130, 66, 150), [32.0, 78.0, 122.0, HEAD - 4.0], 1.35, 3.4, 10, lip=1.0)
    so = w.add(m, "socket", "bronze", base=(206, 110, 60), smooth=45)
    ml.lathe(so, "y", (0, 0, 0), [(HEAD - 6.0, 1.4), (HEAD - 2.0, 2.0), (HEAD + 8.0, 1.5), (HEAD + 12.0, 0.8)], sides=10)
    bd = w.add(m, "phoenix_body", "bronze", base=(224, 108, 40), glow=True, smooth=40)
    w.ellipsoid(bd, (0, HEAD + 17.0, 0), 3.4, 8.5, 2.1, sides=12, stacks=9)
    nk = w.add(m, "phoenix_head", "bronze", base=(236, 140, 50), glow=True, smooth=45)               # the crested head on a curved neck, the beak swept forward
    tube(nk, [(0, HEAD + 22.0, 0.0), (-1.0, HEAD + 29.0, 0.0), (-3.4, HEAD + 35.0, 0.0), (-7.0, HEAD + 38.5, 0.0)], 1.9, 8, 0.45)
    w.ellipsoid(nk, (-6.4, HEAD + 38.4, 0), 2.1, 2.4, 1.5, sides=10, stacks=7)
    bk = w.add(m, "beak", "gold", base=(255, 200, 80), glow=True, smooth=0)
    ml.gem(bk, (-10.4, HEAD + 38.0, 0), 0.8, long=3.4, sides=5, axis=(-1, -0.35, 0))
    ey = w.add(m, "eye", "crystal", base=(255, 240, 160), glow=True, smooth=0)
    ml.gem(ey, (-6.6, HEAD + 39.0, 1.3), 0.4, long=1.0, sides=5, axis=(0, 0, 1))
    cr = w.add(m, "crest", "bronze", base=(255, 160, 50), glow=True, smooth=40)                       # two swept crest feathers over the head
    feather(cr, (-4.6, HEAD + 40.0), 38, 14.0, 2.0, 40, 0.5, 8)
    feather(cr, (-3.4, HEAD + 39.5), 10, 17.0, 2.4, 22, 0.5, 8)
    wg = w.add(m, "wing_feathers", "bronze", base=(232, 120, 40), glow=True, smooth=40)               # five feathers sweeping forward and up, each curling back
    for k, (deg, ln, wd, bend) in enumerate(((10, 32.0, 6.0, -42), (32, 30.0, 5.6, -46), (54, 26.0, 5.0, -50), (76, 22.0, 4.4, -54), (98, 18.0, 3.8, -58))):
        feather(wg, (-1.4, HEAD + 18.0 + k * 1.1), deg, ln, wd, bend, 0.5, 11)
    wb = w.add(m, "wing_back", "bronze", base=(200, 92, 36), glow=True, smooth=40)                    # the rear wing
    for k, (deg, ln, wd, bend) in enumerate(((-20, 22.0, 4.4, 30), (-44, 19.0, 4.0, 30))):
        feather(wb, (1.0, HEAD + 20.0 + k), deg, ln, wd, bend, 0.5, 9)
    tl = w.add(m, "tail_feathers", "bronze", base=(210, 90, 36), glow=True, smooth=40)               # the tail: three sharp blades toward the shaft
    for k, (deg, ln, wd, bend) in enumerate(((-142, 22.0, 3.6, -24), (-166, 26.0, 3.2, 10), (170, 20.0, 3.0, 34))):
        feather(tl, (1.2, HEAD + 10.0 + k * 1.2), deg, ln, wd, bend, 0.5, 9)
    em = w.add(m, "flame_gems", "crystal", base=(255, 230, 120), glow=True, smooth=0)                # little ember gems along the body's centre line
    for k in range(4):
        ml.gem(em, (0, HEAD + 12.0 + k * 4.2, 1.5), 0.45, long=1.3, sides=4, axis=(0, 1, 0))
    atlas = newatlas(m)
    glow(atlas, m, ["phoenix_body", "phoenix_head", "wing_feathers", "wing_back", "tail_feathers", "crest"], (255, 140, 40), 0.55, 0.35)
    glow(atlas, m, ["beak", "eye", "flame_gems"], (255, 230, 140), 1.0, 0.6)
    ref = w.reference("img/784541197571848734.jpg", (30, 30, 720, 500), "phoenix_flame")
    done("phoenix_flame", m, atlas, TOTAL, ref, ("FFFFFF", 0.22, 0.5, 1.0))


# ================================================================ 11 Primal Chaos
def primal_chaos():
    """Card 320 x 320 (partial: only the art is clear): the rapier is shown as a vertical line of light from the knot (92, 44) down to its point (88, 300):
    256 px for a 1.10 m rapier; the line is about 8 px wide and wound with a shimmering double helix; a small bow-like knot of ribbon sits at the top, the hilt.
    The guard, grip and pommel are not drawn: invented as a plain swept hilt in the same blue-white."""
    TOTAL = 1.10
    m = ml.Mesh("primal_chaos")
    pm = w.add(m, "pommel", "crystal", base=(190, 220, 255), glow=True, smooth=0)
    ml.gem(pm, (0, 2.6, 0), 1.35, long=1.9, sides=6, axis=(0, 1, 0))
    pole(m, "grip", "leather", (36, 52, 120), 5.0, 20.0, 1.1, 10, swell=1.1)
    bands(m, "grip_rings", "silver", (190, 210, 255), [6.0, 12.0, 18.0], 1.3, 0.6, 10)
    gb = w.add(m, "guard_bar", "silver", base=(200, 220, 255), smooth=45)
    ml.loft(gb, "x", (0, 22.0, 0), [(-8.0, 0.35, 0.35), (-4.0, 0.7, 0.7), (0, 1.0, 1.0), (4.0, 0.7, 0.7), (8.0, 0.35, 0.35)], ml.circle(0.65, 8))
    kb = w.add(m, "knuckle_bow", "silver", base=(176, 202, 255), smooth=50)                             # a swept knuckle bow from the guard to the pommel
    tube(kb, [(2.0, 22.0, 0.0), (4.0, 17.0, 1.2), (3.6, 10.0, 1.4), (1.4, 5.0, 0.8)], 0.38, 6, 0.8)
    bl = w.add(m, "blade", "crystal", base=(120, 170, 255), glow=True, smooth=45, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(22.5, 1.0, 1.0), (50.0, 0.82, 0.9), (90.0, 0.55, 0.7), (108.0, 0.28, 0.4), (110.0, 0.0, 0.0)], ml.lozenge(1.5, 0.8, 0.2))
    bc = w.add(m, "blade_core", "crystal", base=(255, 255, 255), glow=True, smooth=55)
    for face in (-1, 1):
        ml.bar(bc, (0, 26.0, face * 0.3), (0, 100.0, face * 0.2), 0.34, 0.1, up=(0, 0, face), taper=6.0)
    for nm, ph, amp in (("helix_a", 0.0, 1.0), ("helix_b", math.pi, 1.0)):                              # the shimmering double helix round the blade
        hx = w.add(m, nm, "crystal", base=(90, 140, 255), glow=True, smooth=55)
        ml.sweep(hx, helix_pts(1.05, 28.0, 100.0, 7.5, 90, ph, rfn=lambda t: 1.15 * (1 - 0.75 * t) + 0.12), ml.circle(0.16, 4), scales=lambda t: (1 - 0.6 * t, 1 - 0.6 * t))
    bw = w.add(m, "bow_knot", "cloth", base=(244, 248, 255), glow=True, smooth=50)                    # the bow: a knot, two loops and two tails, at the hilt
    w.ellipsoid(bw, (0, 22.0, 1.4), 0.9, 0.9, 0.9, sides=8, stacks=6)
    for sgn in (-1, 1):
        loop = [(sgn * (0.8 + 3.4 * math.sin(a)), 22.0 + 2.6 * math.sin(2 * a) * 0.9, 1.4 + 0.8 * math.sin(a)) for a in np.linspace(0, math.pi, 9)]
        ml.sweep(bw, loop, ml.rect_ring(0.9, 0.18), scales=lambda t: (1.0, 1.0), up=(0, 0, 1), caps=(True, True))
    tl = w.add(m, "bow_tails", "cloth", base=(230, 240, 255), glow=True, smooth=50)
    for sgn, ln in ((-1, 12.0), (1, 9.0)):
        pts = [(sgn * (0.8 + 2.2 * t) + 0.8 * math.sin(t * 6) * sgn, 22.0 - ln * t, 1.4 + 1.2 * math.sin(t * 4)) for t in np.linspace(0, 1, 10)]
        ml.sweep(tl, pts, ml.rect_ring(1.0, 0.1), scales=lambda t: (1.1 * (1 - 0.45 * t), 1.0), up=(0, 0, 1), caps=(True, True))
    atlas = newatlas(m)
    glow(atlas, m, ["blade", "helix_a", "helix_b", "pommel"], (110, 170, 255), 0.85, 0.12)
    glow(atlas, m, ["blade_core", "bow_knot", "bow_tails"], (200, 228, 255), 0.9, 0.35)
    glow(atlas, m, ["guard_bar", "knuckle_bow"], (140, 190, 255), 0.35, 0.2)
    ref = w.reference("img/784541197571850726.jpg", (30, 20, 170, 310), "primal_chaos")
    done("primal_chaos", m, atlas, TOTAL, ref, ("FFFFFF", 0.3, 0.55, 1.0))


# ================================================================ 12 Raven's Wing
def ravens_wing():
    """Card 736 x 694: the glaive lies from the crescent hook's foot (140, 438) to the wing's tip (598, 44): 605 px for a 2.10 m glaive. s = px from the butt:
    the crescent hook 0..40, dark-steel shaft 40..470, black cloth straps and hanging strips at 190..330, the raven's-wing blade 470..605 of layered feathers
    on a curving back with a small hook under the blade's base."""
    TOTAL = 2.10
    K = TOTAL * 100.0 / 605.0
    m = ml.Mesh("ravens_wing")
    y = lambda s: s * K
    hk = w.add(m, "crescent_hook", "steel", base=(120, 130, 156), smooth=45)                          # the crescent moon at the butt: an open ring with the points up
    ml.sweep(hk, [(-4.0 + 4.2 * math.cos(math.radians(a)), 8.0 + 4.2 * math.sin(math.radians(a)), 0.0) for a in np.linspace(-60, 200, 14)], ml.circle(1.0, 6),
             scales=lambda t: (0.55 + 0.75 * math.sin(math.pi * t), 0.55 + 0.75 * math.sin(math.pi * t)))
    pole(m, "shaft", "steel", (54, 62, 90), y(30), y(480), 1.45, 12, swell=1.0)
    bands(m, "shaft_rings", "iron", (86, 94, 122), [y(38), y(128), y(410), y(466)], 1.75, 0.9, 12)
    st = w.add(m, "straps", "cloth", base=(18, 18, 24), smooth=55, wear=0.2)                          # black cloth wound diagonally round the shaft
    ml.sweep(st, helix_pts(1.62, y(195), y(325), 3.2, 64, 0.4), ml.rect_ring(1.0, 0.1), scales=lambda t: (1.5, 1.0), up=(0, 1, 0), caps=(True, True))
    ts = w.add(m, "strap_tails", "cloth", base=(14, 14, 20), smooth=55, wear=0.2)                      # three torn strips hanging from the wraps
    for k, (xo, ln, zz) in enumerate(((1.8, 52.0, 0.5), (-0.5, 42.0, 1.8), (-1.9, 34.0, -0.7))):
        top = y(330) - 4.0 * k
        pts = [(xo + 1.6 * math.sin(t * 4.0 + k), top - ln * t, zz + 1.0 * math.sin(t * 3.0)) for t in np.linspace(0, 1, 12)]
        ml.sweep(ts, pts, ml.rect_ring(1.0, 0.1), scales=lambda t: (2.0 * (1 - 0.3 * t), 1.0), up=(1, 0, 0), caps=(True, True))
    sk = w.add(m, "socket", "iron", base=(70, 78, 106), smooth=45)
    ml.lathe(sk, "y", (0, 0, 0), [(y(460), 1.6), (y(472), 2.4), (y(494), 2.2), (y(504), 1.4)], sides=12)
    bl = w.add(m, "blade", "steel", base=(88, 100, 140), smooth=40, uv_mode="loft")                    # the wing's main blade: a long back curving up, the edge in front
    path = [(1.2 * math.sin(t * 1.4) - 9.0 * t ** 2.4, y(496) + t * 54.0, 0.0) for t in np.linspace(0, 1, 16)]
    ml.sweep(bl, path, edge_sec(-1, 0.45, bevel=0.5), scales=lambda t: (7.0 * (1 - 0.15 * t) * (1.0 if t < 0.88 else max(0.0, 1 - (t - 0.88) / 0.12)) + 0.01, 1.0), up=(0, 0, 1))
    ff = w.add(m, "feathers_front", "steel", base=(120, 136, 180), smooth=40)                          # the layered feathers on the +z face, longer toward the tip
    fb = w.add(m, "feathers_back", "steel", base=(104, 118, 164), smooth=40)
    for k in range(6):
        t = 0.1 + 0.15 * k
        bx, by = 1.2 * math.sin(t * 1.4) - 9.0 * t ** 2.4 + 2.2, y(496) + t * 54.0
        feather(ff, (bx, by), -10 + k * 4, 17.0 + k * 1.4, 2.6, 18, 0.35, 7, z=0.5 + 0.18 * k)
        feather(fb, (bx, by), -10 + k * 4, 17.0 + k * 1.4, 2.6, 18, 0.35, 7, z=-0.5 - 0.18 * k)
    sp = w.add(m, "spine_ridge", "iron", base=(52, 60, 86), smooth=50)
    ml.sweep(sp, [(p[0] + 3.4, p[1], 0.0) for p in path[1:13]], ml.circle(1.0, 6), scales=lambda t: (0.5 * (1 - 0.6 * t), 0.7 * (1 - 0.6 * t)))
    hb = w.add(m, "beak_hook", "steel", base=(110, 122, 160), smooth=40)                               # the small hook under the blade's base
    curved_plate(hb, [(-2.0 - 3.6 * math.sin(math.pi * 0.5 * t), y(494) - 1.0 + 3.0 * t - 3.0 * t * t, 0.0) for t in np.linspace(0, 1, 7)], lambda t: 3.0 * (1 - t) + 0.3, 0.8, sgn=-1)
    atlas = newatlas(m)
    glow(atlas, m, ["blade", "feathers_front", "feathers_back"], (150, 190, 255), 0.07, 0.0)
    ref = w.reference("img/784541197571831962.jpg", (100, 30, 640, 460), "ravens_wing")
    done("ravens_wing", m, atlas, TOTAL, ref, ("FFFFFF", 0.1, 0.5, 0.9))


MODELS = {"demon_shovel": demon_shovel, "dragoons_halberd": dragoons_halberd, "efreetis_kusarigama": efreetis_kusarigama,
          "ilmare_gin_the_starlight_silver": ilmare_gin_the_starlight_silver, "katana_of_carnage": katana_of_carnage,
          "katana_of_the_crimson_shogun": katana_of_the_crimson_shogun, "lightworkers_scepter": lightworkers_scepter, "mjolnir": mjolnir,
          "overprotective_halberd": overprotective_halberd, "phoenix_flame": phoenix_flame, "primal_chaos": primal_chaos, "ravens_wing": ravens_wing}


def main(only=None):
    for item_id, fn in MODELS.items():
        if only and item_id not in only:
            continue
        fn()


if __name__ == "__main__":
    main(sys.argv[1:])
