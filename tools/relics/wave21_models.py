"""Relics of Many Worlds, part 4, wave 21: the twelve hand-held weapons as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave21_models.py [id ...]       (no ids = all twelve; idempotent, deterministic)
    W21_PREVIEW=<dir> python3 -B tools/relics/wave21_models.py rift_breaker      (also renders the preview beside the card)

Run tools/relics/wave21_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon).
Per id it writes assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex normals), textures/entity/weapon/<id>.png and <id>_glow.png (one 512 px skin
each). Budget: 8 to 14 named parts and at most 2800 quads per weapon. Sizes follow the 1.34 size rule: the mesh is built at the natural length of its
weapon class (the lead's size and orientation tools run afterwards).

The method, in order, for every weapon (one build_<id>() each):
  0 target decided: the card's art and the printed text.
  1 measure the reference: the card's picture gives the proportions (head against shaft, blade width against length); each is a NAMED CONSTANT in
    centimetres with a comment saying where it came from. The card art is a drawing, so the reference length is the class length of the weapon.
  2 geometry from real cross-sections (loft / sweep / lathe), never a box or a four-sided frustum for a long form (hammer heads are chamfered blocks).
  3 the small raised parts are the detail: each its own named Part with its own constants.
  4 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one skin per weapon.
  5 painted material plus a second additive glow skin.
  6 rendered beside the reference (W21_PREVIEW) and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.
Axes: y up from the butt; mesh -x is forward (edges, the strike face and the hooks of crescents face -x), +x is the back; z is the thickness.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ["W10_PREVIEW"] = os.environ.get("W21_PREVIEW", "")             # wave10_models (imported below) reads this one for the preview folder
import mesh_lib as ml                                                   # noqa: E402
import wave1_models as w                                                # noqa: E402
from wave10_models import card, glow_all                                # noqa: E402

CARDS = os.environ.get("RELICS4", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics4")
w.ZIP = CARDS                                                           # the card images live under scratchpad/relics4/img

MODELS = ["rift_breaker", "scimitar_of_the_seasons", "silver_wind_mountain_glaive", "spider_sword", "stick_of_youth", "sun_gift", "the_last_horseman",
          "the_legacy", "the_librarians_maul", "time_hammer", "vanguard_hammer", "willbreaker"]


# ================================================================ helpers shared by the twelve models
def part(m, name, material, **kw):
    kw.setdefault("smooth", 45)
    return w.add(m, name, material, **kw)


def pole(p, y0, y1, r0, r1=None, sides=10, mid=None):
    """A round shaft along y (radius r0 at the bottom, r1 at the top, an optional swelling (y, factor) in the middle)."""
    r1 = r0 if r1 is None else r1
    st = [(y0, r0, r0)]
    if mid:
        st.append((mid[0], max(r0, r1) * mid[1], max(r0, r1) * mid[1]))
    st.append((y1, r1, r1))
    return ml.loft(p, "y", (0, 0, 0), st, ml.circle(1.0, sides))


def bands(p, ys, r, h, lip=0.3, sides=10):
    """Binding rings / collars at every y."""
    for y in ys:
        ml.collar(p, y, r, h, lip=lip, sides=sides)


def tube_into(p, pts, r, taper=0.7, sides=6, point=True, caps=(True, True)):
    """A tapering round tube along a polyline, built into an existing part (radius r at the start, shrinking by `taper`; closed to a point at the end)."""
    def sc(t):
        rr = r * (1.0 - taper * t)
        return (0.0, 0.0) if (point and t > 0.985) else (rr, rr)
    return ml.sweep(p, pts, ml.circle(1.0, sides), scales=sc, caps=caps)


def helix_tube(p, y0, y1, r_axis, r_tube, turns, n, phase=0.0, sides=5):
    """A thin tube spiralling round the y axis (a wire wrap, a vine, a cord, a spiral rib): n points, its own quads only."""
    pts = w.helix(lambda t: r_axis, y0, y1, turns, n, phase)
    return ml.sweep(p, pts, ml.circle(1.0, sides), scales=lambda t: (r_tube, r_tube), caps=(False, False))


def blade_sweep(p, pts, hw, th, ridge=0.3, up=(0, 0, 1), sharp_start=False):
    """A blade plate along a polyline: hw(t) / th(t) are the half width and half thickness at t in 0..1 (a lozenge section, closed at the tip)."""
    def sc(t):
        if t > 0.995 or (sharp_start and t < 0.005):
            return (0.0, 0.0)
        return (max(0.0, hw(t)), max(0.0, th(t)))
    return ml.sweep(p, pts, ml.lozenge(2.0, 2.0, ridge), scales=sc, up=up)


def leaf_plate(p, base, tip, half_w, half_t=0.12, up=(0, 0, 1)):
    """A small flat leaf from base to tip (a lozenge that swells then closes): leaves, petals, ribbons' ends."""
    base, tip = np.array(base, float), np.array(tip, float)
    pts = [tuple(base + (tip - base) * t) for t in np.linspace(0, 1, 5)]
    prof = [0.25, 0.85, 1.0, 0.65, 0.0]
    return ml.sweep(p, pts, ml.lozenge(2.0, 2.0, 0.2), scales=lambda t: (half_w * prof[min(4, int(round(t * 4)))], half_t * (0.4 + 0.6 * prof[min(4, int(round(t * 4)))])) if t < 0.99 else (0.0, 0.0), up=up)


def hammer_block(p, x0, x1, y0, y1, hz, chamfer=1.0):
    """A chamfered block (a hammer head or a slab), x from x0 to x1, y from y0 to y1, half thickness hz."""
    return ml.box(p, (x0, y0, -hz), (x1, y1, hz), chamfer)


def finish(item_id, m, atlas, real_m, ref=None, glow=("FFFFFF", 0.14, 0.45, 1.0)):
    atlas.layout(m)
    atlas.paint(m)
    return w.finish(item_id, m, atlas, real_m=real_m, glow=glow, ref=ref)


def fresh(item_id):
    return ml.Mesh(item_id), ml.Atlas(512)


def _grain(strength=0.2, cell=4, seed=3):
    def f(c):
        g = w.noise2(c.img.shape[:2], cell, seed)
        c.img[..., :3] = np.clip(c.img[..., :3] * (1 - strength / 2 + strength * g[..., None]), 0, 255)
    return f


def _lines(sel, dark=0.55, glow_rgb=None, gstr=0.5):
    """Engraved lines on a painted patch: darker lines where sel(c) is true, optionally glowing."""
    def f(c):
        ln = sel(c)
        c.img[..., :3] = np.where(ln[..., None], c.img[..., :3] * dark, c.img[..., :3])
        if glow_rgb:
            w.set_glow(c, glow_rgb, gstr, mask=ln.astype(np.float32), core=0.25)
    return f


# ================================================================ 1 Rift Breaker (spear)
def build_rift_breaker():
    """Card art 720 x 900: a purple runed shaft with a magenta crystal leaf head, a cord binding under it and a glowing pink-yellow vortex at the butt.
    Class: spear 1.8 to 2.4 m: 210 cm. The shaft is the long form (about 80 percent), the head 38 cm (the card draws the head at 18 percent of the spear)."""
    L = 210.0
    Y_BUTT, Y_COLLAR, Y_HEAD0, Y_TIP = 0.0, 166.0, 172.0, L        # butt cap, the collar under the head, crystal leaf from here to the tip
    R_SHAFT = 1.4                                                   # 8 px thick on the card at 2.1 m: drawn heavy
    m, atlas = fresh("rift_breaker")
    sh = part(m, "shaft", "iron", base=(96, 56, 132), uv_mode="loft", smooth=50)
    pole(sh, 4.0, Y_COLLAR, R_SHAFT, R_SHAFT * 1.05, 10, mid=(90.0, 1.06))
    bc = part(m, "butt_cap", "silver", base=(176, 170, 196))
    ml.lathe(bc, "y", (0, 0, 0), [(-3.0, 0.9), (-1.0, R_SHAFT * 1.5), (2.0, R_SHAFT * 1.9), (5.0, R_SHAFT * 1.2)], sides=10)
    rn = part(m, "runes", "obsidian", base=(255, 110, 200), glow=True, smooth=30)         # the carved runes along the lower shaft (card (240..470, 240..260))
    for k in range(9):
        y = 16.0 + k * 7.5
        a = 0.6 if k % 2 else -0.6
        ml.bar(rn, (a - 0.6, y, R_SHAFT * 0.95), (a + 0.7, y + 3.0, R_SHAFT * 0.95), 0.34, 0.14, up=(0, 0, 1), taper=0.3)
        ml.bar(rn, (a + 0.5, y + 1.0, -R_SHAFT * 0.95), (a - 0.6, y + 4.0, -R_SHAFT * 0.95), 0.3, 0.14, up=(0, 0, -1), taper=0.3)
    cw = part(m, "cord_wrap", "leather", base=(40, 32, 44), wear=0.3, smooth=60)          # the black cord wound under the head (card (365..470, 240..260))
    for k in range(9):
        ml.collar(cw, Y_COLLAR - 22.0 + k * 2.1, R_SHAFT * 1.22, 1.5, lip=0.4, sides=10)
    hc = part(m, "head_collar", "silver", base=(168, 160, 200), smooth=55)
    ml.lathe(hc, "y", (0, 0, 0), [(Y_COLLAR, R_SHAFT * 1.1), (Y_COLLAR + 1.2, R_SHAFT * 2.0), (Y_HEAD0 + 2.5, R_SHAFT * 1.6), (Y_HEAD0 + 5.0, R_SHAFT * 1.2), (Y_HEAD0 + 6.0, R_SHAFT * 0.6)], sides=10)
    ct = part(m, "crystal_tip", "crystal", base=(238, 70, 168), glow=True, smooth=35, uv_mode="loft")      # the swirling magenta crystal, a long leaf (card (480..600, 225..255))
    wid = w.hermite([(0.0, 1.4), (0.1, 3.2), (0.35, 4.3), (0.6, 3.6), (0.85, 1.8), (1.0, 0.0)])
    ml.loft(ct, "y", (0, 0, 0), [(Y_HEAD0 + 4 + (Y_TIP - Y_HEAD0 - 4) * t, max(wid(t), 0.0), 0.9 * min(1.0, wid(t) / 2.0)) if t < 0.995 else (Y_TIP, 0.0, 0.0) for t in np.linspace(0, 1, 12)], ml.lozenge(2.0, 2.0, 0.35))
    ts = part(m, "tip_swirl", "crystal", base=(255, 200, 230), glow=True, smooth=50)       # the swirl cut into the crystal: two ridges spiralling up each face
    for face in (1, -1):
        pts = [(2.4 * math.sin(i * 0.8) * (1 - i / 14.0), Y_HEAD0 + 8 + i * 2.4, face * 0.95 * (1 - i / 30.0)) for i in range(14)]
        ml.sweep(ts, pts, ml.circle(1.0, 5), scales=lambda t: (0.3 * (1 - 0.5 * t), 0.2), caps=(True, True))
    rr = part(m, "rift_ring", "crystal", base=(255, 90, 190), glow=True, smooth=60)       # the vortex at the butt: an outer ring ...
    ml.sweep(rr, [(7.5 * math.cos(a), -4.0, 7.5 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 25)], ml.circle(1.0, 6), scales=lambda t: (0.7, 0.7), caps=(False, False))
    ri = part(m, "rift_core", "gold", base=(255, 232, 130), glow=True, smooth=60)         # ... a yellow inner ring ...
    ml.sweep(ri, [(3.6 * math.cos(a), -4.0, 3.6 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)], ml.circle(1.0, 6), scales=lambda t: (0.55, 0.55), caps=(False, False))
    ra = part(m, "rift_arms", "crystal", base=(240, 80, 200), glow=True, smooth=60)       # ... and three arms swirling in
    for k in range(3):
        ph = k * 2 * math.pi / 3
        pts = [((9.5 - 6.0 * t) * math.cos(ph + 2.4 * t), -4.0 - 0.6 * t, (9.5 - 6.0 * t) * math.sin(ph + 2.4 * t)) for t in np.linspace(0, 1, 9)]
        tube_into(ra, pts, 0.5, 0.8, 5)
    _tex_common(m, atlas, {"crystal_tip": ((255, 90, 200), 0.55), "tip_swirl": ((255, 190, 230), 0.8), "rift_ring": ((255, 90, 200), 0.9),
                           "rift_core": ((255, 235, 140), 0.95), "rift_arms": ((255, 100, 210), 0.8), "runes": ((255, 110, 200), 0.9)})
    finish("rift_breaker", m, atlas, L / 100.0, card("img/784541197571844772.jpg", (140, 160, 620, 320), "rift_breaker"), ("FFFFFF", 0.16, 0.45, 1.0))


def _tex_common(m, atlas, glows, grains=()):
    for nm in grains:
        w.paint_part(atlas, m.part(nm), _grain())
    for nm, (rgb, st) in glows.items():
        glow_all(atlas, m.part(nm), rgb, st, 0.35)


# ================================================================ 2 Scimitar of the Seasons
def build_scimitar_of_the_seasons():
    """Card art 736 x 736: a long, translucent jade-green blade curving toward the spine, a dark twisted-branch hilt with a ruby at the guard, twigs
    sprouting crimson buds and green leaves, pale leaves drifting. Class: sabre / scimitar 1.00 to 1.25 m: 105 cm (blade 78 percent as on the card)."""
    L = 105.0
    Y_GUARD = 24.0                                                   # the ruby guard (card (410, 350)): the branch ends below, the blade above
    CURVE = 19.0                                                     # the tip stands this far toward +x (the spine side) of the root: read off the card's sweep
    m, atlas = fresh("scimitar_of_the_seasons")

    def bx(y):
        t = (y - Y_GUARD) / (L - Y_GUARD)
        return CURVE * (t ** 1.8)
    ys = np.linspace(Y_GUARD, L, 18)
    path = [(bx(y), y, 0.0) for y in ys]
    wf = w.hermite([(0.0, 1.6), (0.06, 2.6), (0.35, 3.2), (0.7, 3.2), (0.9, 2.4), (0.97, 1.0), (1.0, 0.0)])
    bl = part(m, "blade", "crystal", base=(86, 214, 170), glow=True, smooth=40, uv_mode="loft")
    blade_sweep(bl, path, wf, lambda t: 0.5 * min(1.0, 0.4 + 2.0 * wf(t) / 3.2), 0.4)
    sp = part(m, "spine_ridge", "crystal", base=(150, 240, 210), glow=True, smooth=60)       # a bead along the back (+x) edge
    ml.sweep(sp, [(bx(y) + wf((y - Y_GUARD) / (L - Y_GUARD)) * 0.92, y, 0.0) for y in ys[1:-2]], ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.32, 0.4), up=(0, 0, 1))
    fu = part(m, "fuller", "crystal", base=(190, 255, 236), glow=True, smooth=60)             # two ridges along each face: the translucent centre line
    for face in (1, -1):
        ml.sweep(fu, [(bx(y) - 0.2, y, face * 0.52) for y in ys[2:-4]], ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.2 + 0.1 * math.sin(math.pi * t), 0.14), up=(0, 0, 1))
    gg = part(m, "guard_gem", "crystal", base=(220, 30, 70), glow=True, smooth=0)             # the ruby set in the square guard
    ml.gem(gg, (0.0, Y_GUARD - 0.5, 0.0), 1.7, long=1.2, sides=4, axis="z")
    br = part(m, "branch_hilt", "wood", base=(74, 44, 44), uv_mode="loft", smooth=55)         # the twisted branch hilt, bending away at the pommel (card (480, 420))
    pts = [(-1.0 * math.sin(i * 0.5) + (0.0 if i > 0 else 0.0) - 0.07 * (i ** 2) * (1 if i < 3 else 0), Y_GUARD - 1 - i * 2.4, 0.5 * math.cos(i * 0.6)) for i in range(0, 9)]
    pts += [(-2.4 - 0.9 * k, Y_GUARD - 1 - 8 * 2.4 - 1.6 * k, 0.0) for k in range(1, 4)]
    ml.sweep(br, pts, ml.circle(1.0, 8), scales=lambda t: (1.5 - 0.6 * t * t + 0.2 * math.sin(t * 18), 1.5 - 0.6 * t * t + 0.2 * math.cos(t * 18)))
    bt = part(m, "branch_twist", "wood", base=(104, 62, 56), smooth=60)                       # a second vine twisted round it
    helix_tube(bt, Y_GUARD - 20.0, Y_GUARD + 1.0, 1.5, 0.45, 3.0, 28, 0.0, 5)
    tw = part(m, "twigs", "wood", base=(90, 54, 50), smooth=60)                               # twigs climbing the guard toward the blade (card (350..500, 270..380))
    for (a, h, lng) in ((0.9, 3.0, 9.0), (-0.8, 6.0, 8.0), (0.5, 10.0, 7.0), (-1.2, 12.0, 5.0)):
        s = math.copysign(1, a)
        pts = [(s * (1.4 + k * 1.0), Y_GUARD + h * 0.2 + k * lng / 4.0, 0.3 * k * (1 if a > 0 else -1)) for k in range(5)]
        tube_into(tw, pts, 0.42, 0.75, 5)
    bu = part(m, "buds", "crystal", base=(210, 36, 84), glow=True, smooth=50)                 # crimson buds at the twig tips
    for (xx, yy, zz) in ((6.2, Y_GUARD + 9.0, 1.2), (-5.4, Y_GUARD + 8.0, -1.0), (5.0, Y_GUARD + 13.0, 0.8), (-6.6, Y_GUARD + 15.0, 1.0), (3.4, Y_GUARD + 4.0, -1.1)):
        w.ellipsoid(bu, (xx, yy, zz), 0.75, 1.3, 0.75, sides=7, stacks=5)
    lv = part(m, "leaves", "crystal", base=(84, 170, 80), smooth=40)                          # small green leaves on the twigs
    for (b, t, hw) in (((2.5, Y_GUARD + 1.0, 0.2), (5.6, Y_GUARD + 4.0, 0.6), 0.9), ((-2.2, Y_GUARD + 3.0, 0.0), (-5.4, Y_GUARD + 5.4, -0.4), 0.9),
                       ((3.0, Y_GUARD + 6.0, 0.4), (6.2, Y_GUARD + 6.6, 1.0), 0.8), ((-3.0, Y_GUARD + 9.0, 0.0), (-6.4, Y_GUARD + 11.0, -0.3), 0.8),
                       ((2.0, Y_GUARD + 11.0, 0.3), (4.4, Y_GUARD + 15.0, 0.8), 0.8)):
        leaf_plate(lv, b, t, hw, 0.12, up=(0, 1, 0))
    dl = part(m, "drift_leaves", "crystal", base=(190, 255, 240), glow=True, smooth=40)       # the pale leaves swirling round the blade: loose, small
    for (xx, yy, zz, ang) in ((-6.0, 48.0, 2.2, 0.3), (7.5, 40.0, -2.0, 0.9), (-5.0, 64.0, -2.4, 1.6), (9.0, 70.0, 2.2, 2.2), (-3.0, 34.0, 2.6, 2.9)):
        leaf_plate(dl, (xx, yy, zz), (xx + 4.2 * math.cos(ang), yy + 4.2 * math.sin(ang), zz), 1.3, 0.1, up=(0, 0, 1))
    _tex_common(m, atlas, {"blade": ((120, 255, 215), 0.35), "spine_ridge": ((170, 255, 230), 0.6), "fuller": ((200, 255, 240), 0.7), "guard_gem": ((255, 60, 100), 0.9),
                           "buds": ((255, 70, 110), 0.5), "drift_leaves": ((200, 255, 240), 0.7)}, ("branch_hilt", "branch_twist", "twigs"))
    finish("scimitar_of_the_seasons", m, atlas, L / 100.0, card("img/784541197571850487.jpg", (180, 20, 660, 440), "scimitar_of_the_seasons"), ("FFFFFF", 0.12, 0.4, 1.0))


# ================================================================ 3 Silver-Wind Mountain Glaive
def build_silver_wind_mountain_glaive():
    """Card art 591 x 767: a 12-foot double-headed glaive: dark runed steel blades curving in opposite directions, a pearl at each crossguard, a foot of
    white ribbon at each blade, a pole wrapped in dark red leather. Class: glaive 1.8 to 2.4 m: 240 cm (the largest of the class: a giant's weapon)."""
    L = 240.0
    Y_LO, Y_HI = 56.0, 184.0                                         # the crossguards (card (380, 600) and (445, 175): about a quarter and three quarters of the length)
    BLADE = 52.0                                                     # each blade is about a fifth of the whole (card: 130 px of 600)
    R_POLE = 1.7
    m, atlas = fresh("silver_wind_mountain_glaive")
    pl = part(m, "pole", "iron", base=(70, 72, 84), uv_mode="loft", smooth=50)
    pole(pl, 2.0, L - 2.0, R_POLE, R_POLE, 10)
    lg = part(m, "leather_grip", "leather", base=(112, 22, 30), uv_mode="loft", smooth=50)    # the thick dark red leather wrap (card: the whole middle)
    pole(lg, Y_LO + 14.0, Y_HI - 14.0, R_POLE * 1.18, R_POLE * 1.18, 10, mid=(120.0, 1.06))
    gb = part(m, "grip_bindings", "leather", base=(70, 12, 20), wear=0.3, smooth=60)
    for k in range(8):
        ml.collar(gb, Y_LO + 16.0 + k * 15.0, R_POLE * 1.3, 1.6, lip=0.4, sides=8)

    def crescent(part_, y0, sgn, hook):
        """A curved blade from y0 going up (sgn 1) or down (sgn -1), bending to -x (top) or +x (bottom) like the card's opposite curves."""
        ys = [y0 + sgn * BLADE * (k / 12.0) for k in range(13)]
        pts = [(hook * (-20.0) * ((y - y0) / (sgn * BLADE)) ** 2.2, y, 0.0) for y in ys]
        wf = w.hermite([(0.0, 2.4), (0.12, 7.5), (0.4, 10.5), (0.7, 7.6), (0.92, 3.4), (1.0, 0.0)])
        blade_sweep(part_, pts, wf, lambda t: 0.8 * min(1.0, 0.35 + wf(t) / 7.0), 0.3, up=(0, 0, 1))
        return pts, wf
    bt = part(m, "blade_top", "iron", base=(60, 62, 72), smooth=40, uv_mode="loft")
    ptop, wtop = crescent(bt, Y_HI + 3.0, 1, 1)
    bb = part(m, "blade_bottom", "iron", base=(60, 62, 72), smooth=40, uv_mode="loft")
    pbot, wbot = crescent(bb, Y_LO - 3.0, -1, -1)
    rt = part(m, "runes_top", "obsidian", base=(150, 200, 255), glow=True, smooth=30)         # the runes inlaid in the blade faces (card (475..520, 100..220))
    rb = part(m, "runes_bottom", "obsidian", base=(150, 200, 255), glow=True, smooth=30)
    for (rp, pts, wf, y0, sgn, hook) in ((rt, ptop, wtop, Y_HI + 3.0, 1, 1), (rb, pbot, wbot, Y_LO - 3.0, -1, -1)):
        for k in range(1, 5):
            t = 0.14 + 0.15 * k
            i = int(round(t * 12))
            px, py, _ = pts[i]
            for face in (1, -1):
                ml.bar(rp, (px - 1.4, py, face * 0.55), (px + 1.0, py + sgn * 2.2, face * 0.55), 0.4, 0.15, up=(0, 0, face), taper=0.3)
                ml.bar(rp, (px + 0.4 * hook, py + sgn * 0.6, face * 0.55), (px - 2.4 * hook, py + sgn * 3.0, face * 0.55), 0.34, 0.15, up=(0, 0, face), taper=0.3)
    cg = part(m, "crossguards", "iron", base=(54, 56, 66), wear=0.4, smooth=55)               # the guard blocks at each blade root, with collars
    for y, sg in ((Y_HI, 1), (Y_LO, -1)):
        ml.lathe(cg, "y", (0, 0, 0), [(y - sg * 4.0, R_POLE * 1.2), (y - sg * 2.0, R_POLE * 2.1), (y + sg * 2.5, R_POLE * 2.1), (y + sg * 4.5, R_POLE * 1.2)], sides=10)
        ml.collar(cg, y - 7.0 if sg > 0 else y + 3.0, R_POLE * 1.4, 4.0, lip=1.0, sides=10)
    pe = part(m, "pearls", "bone", base=(250, 242, 250), glow=True, smooth=50)               # a pearl-like stone inset into both sides of each end (card (445, 175), (385, 595))
    for y in (Y_HI, Y_LO):
        for face in (1, -1):
            w.ellipsoid(pe, (0.0, y, face * R_POLE * 1.9), 1.5, 1.5, 0.9, sides=10, stacks=7)
    rb_ = part(m, "ribbons", "cloth", base=(244, 244, 250), smooth=40, uv_mode="loft")        # a foot of white ribbon tied where the pole meets the blade
    for y, sg, dx in ((Y_HI, 1, 1.0), (Y_LO, -1, -1.0)):
        pts = [(dx * (R_POLE * 2.3 + 1.8 * k + 1.6 * math.sin(k * 0.9)), y - sg * (2.0 + 1.4 * k), 1.4 * math.cos(k * 0.7)) for k in range(0, 9)]
        ml.sweep(rb_, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.5 * (1.0 - 0.4 * t), 0.07), up=(0, 0, 1))
    _tex_common(m, atlas, {"runes_top": ((160, 210, 255), 0.7), "runes_bottom": ((160, 210, 255), 0.7), "pearls": ((255, 244, 255), 0.5)}, ("pole", "blade_top", "blade_bottom"))
    finish("silver_wind_mountain_glaive", m, atlas, L / 100.0, card("img/A Wind Themed Glaive To Bring The Mountain Weather To Your Enemies.jpg", (300, 0, 591, 767), "silver_wind_mountain_glaive"), ("FFFFFF", 0.1, 0.3, 1.0))


# ================================================================ 4 Spider Sword
def build_spider_sword():
    """Card art 640 x 962 (a close-up of the dark curved blade with red resin roots over it, a flared red cup guard, a red-patterned hilt).
    Class: sabre / scimitar 1.00 to 1.25 m: 105 cm; blade 75 cm (the card's blade is three quarters of the sword)."""
    L = 105.0
    Y_CUP0, Y_BLADE0 = 12.0, 25.0                                    # cup guard from here, the blade starts here
    CURVE = 11.0
    m, atlas = fresh("spider_sword")

    def bx(y):
        t = (y - Y_BLADE0) / (L - Y_BLADE0)
        return CURVE * t ** 1.6
    ys = np.linspace(Y_BLADE0, L, 18)
    path = [(bx(y), y, 0.0) for y in ys]
    wf = w.hermite([(0.0, 3.4), (0.15, 4.4), (0.55, 4.9), (0.85, 3.8), (0.96, 1.5), (1.0, 0.0)])
    bl = part(m, "blade", "iron", base=(64, 66, 78), smooth=40, uv_mode="loft")
    blade_sweep(bl, path, wf, lambda t: 0.5, 0.25)
    sp = part(m, "spine_ridge", "iron", base=(84, 86, 98), smooth=60)
    ml.sweep(sp, [(bx(y) + wf((y - Y_BLADE0) / (L - Y_BLADE0)) * 0.9, y, 0.0) for y in ys[:-2]], ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.3, 0.45), up=(0, 0, 1))
    rw = part(m, "resin_web", "bronze", base=(186, 36, 30), glow=True, wear=0.2, smooth=60)   # the roots of red resin across both faces (card: branching from the spine to the edge)
    rng = np.random.default_rng(21)
    for face in (1, -1):
        for k in range(5):                                                # trunks: from the spine side toward the edge, along the blade
            t0 = 0.1 + 0.17 * k
            i0 = int(t0 * 17)
            x0, y0, _ = path[i0]
            hw0 = wf(t0)
            x1, y1 = x0 - hw0 * (1.5 + 0.4 * rng.random()), y0 + 3.0 + 4.0 * rng.random()
            ml.bar(rw, (x0 + hw0 * 0.8, y0, face * 0.52), (x1 + 1.0, y1, face * 0.52), 0.8, 0.26, up=(0, 0, face), taper=0.8)
            for j in range(1):                                            # branches off each root
                u = 0.5
                bx0, by0 = x0 + hw0 * 0.8 + (x1 + 1.0 - x0 - hw0 * 0.8) * u, y0 + (y1 - y0) * u
                ml.bar(rw, (bx0, by0, face * 0.52), (bx0 - 1.0 - 1.5 * rng.random(), by0 + 3.5 + 2.5 * rng.random(), face * 0.52), 0.5, 0.22, up=(0, 0, face), taper=0.6)
        ml.bar(rw, (bx(Y_BLADE0 + 2) + 0.2, Y_BLADE0 + 1.0, face * 0.52), (bx(L - 22) + 0.6, L - 24.0, face * 0.52), 0.5, 0.2, up=(0, 0, face), taper=3.0)   # the main root along the middle
    cu = part(m, "cup_guard", "bronze", base=(170, 44, 36), smooth=50, wear=0.4)              # the flared red cup (card (40..160, 290..350))
    ml.lathe(cu, "y", (0, 0, 0), [(Y_CUP0 - 1.0, 1.8), (Y_CUP0 + 2.5, 3.6), (Y_CUP0 + 8.0, 5.6), (Y_BLADE0 - 1.0, 6.4), (Y_BLADE0 + 0.5, 4.2), (Y_BLADE0 + 1.0, 1.6)], sides=12, squash=0.55)
    cr = part(m, "cup_rim", "bronze", base=(210, 70, 50), smooth=55)
    ml.collar(cr, Y_BLADE0 - 2.0, 6.5, 2.2, lip=0.6, sides=12, oval=0.56)
    ml.collar(cr, Y_CUP0 + 1.5, 3.9, 1.2, lip=0.4, sides=12, oval=0.56)
    hl = part(m, "hilt", "leather", base=(54, 40, 42), uv_mode="loft", smooth=50)
    pole(hl, -3.0, Y_CUP0, 1.45, 1.55, 8, mid=(5.0, 1.08))
    hb = part(m, "hilt_bands", "bronze", base=(150, 40, 34), wear=0.3, smooth=60)
    bands(hb, [0.5, 4.0, 7.5], 1.75, 1.2, 0.35, 8)
    hr = part(m, "hilt_roots", "bronze", base=(176, 34, 30), glow=True, smooth=60)             # the resin crawling over the hilt
    helix_tube(hr, -1.0, Y_CUP0, 1.55, 0.3, 2.0, 18, 0.5, 5)
    helix_tube(hr, -1.0, Y_CUP0, 1.55, 0.26, 2.0, 18, 3.6, 5)
    pm = part(m, "pommel", "bronze", base=(120, 34, 30), smooth=50)
    w.ellipsoid(pm, (0.0, -5.0, 0.0), 2.4, 2.8, 2.4, sides=10, stacks=7)
    dr = part(m, "drips", "bronze", base=(210, 60, 40), glow=True, smooth=30)                  # drops of resin hanging from the cup
    for (xx, yy, zz) in ((3.0, Y_CUP0 + 3.0, 1.2), (-3.4, Y_CUP0 + 6.0, -1.2), (1.5, Y_CUP0 + 1.0, 1.6)):
        w.ellipsoid(dr, (xx, yy, zz), 0.6, 1.1, 0.6, sides=6, stacks=4)
    _tex_common(m, atlas, {"resin_web": ((255, 60, 40), 0.45), "hilt_roots": ((255, 60, 40), 0.4), "drips": ((255, 80, 50), 0.5)}, ("blade", "hilt"))
    finish("spider_sword", m, atlas, L / 100.0, card("img/Spider Sword.jpg", (0, 0, 640, 560), "spider_sword"), ("FFFFFF", 0.12, 0.3, 1.0))


# ================================================================ 5 Stick of Youth
def build_stick_of_youth():
    """Card art 735 x 739: a plain hollow, knotted branch club, thicker toward the open top, a carved vine-like bark pattern, a broken side branch and a
    knot hole. Class: club 0.90 to 1.40 m: 100 cm. Thickness 9 cm at the top, 5 cm at the foot (card: the top end is about 1.8x the foot)."""
    L = 100.0
    R0, R1 = 2.6, 4.6
    m, atlas = fresh("stick_of_youth")

    def wob(t):
        return (1.2 * math.sin(t * 7.0), 0.9 * math.sin(t * 5.0 + 1.0))
    pts = [(wob(t)[0], L * t * 0.97, wob(t)[1]) for t in np.linspace(0, 1, 16)]
    def rad(t):
        return R0 + (R1 - R0) * t ** 1.3 + 0.35 * math.sin(t * 16.0)
    tr = part(m, "trunk", "wood", base=(150, 104, 62), uv_mode="loft", smooth=50)
    ml.sweep(tr, pts, ml.circle(1.0, 10), scales=lambda t: (rad(t), rad(t)), caps=(True, False))
    vn = part(m, "bark_vines", "wood", base=(112, 74, 44), smooth=60)                           # the carved vine pattern: two ridges spiralling up the trunk
    for ph in (0.0, math.pi):
        vp = []
        for i in range(0, 40):
            t = i / 39.0
            a = ph + t * 11.0
            r = rad(t) * 1.0 + 0.1
            vp.append((wob(t)[0] + r * math.cos(a), L * t * 0.97, wob(t)[1] + r * math.sin(a)))
        ml.sweep(vn, vp, ml.circle(1.0, 5), scales=lambda t: (0.38, 0.38), caps=(False, False))
    bp = part(m, "bark_plates", "wood", base=(176, 126, 80), smooth=55)                         # raised bark plates between the vines (the card's facets)
    rng = np.random.default_rng(5)
    for k in range(16):
        t = 0.06 + 0.055 * k
        a = rng.random() * 2 * math.pi
        r = rad(t)
        c = (wob(t)[0] + r * math.cos(a), L * t * 0.97, wob(t)[1] + r * math.sin(a))
        nrm = (math.cos(a), 0.0, math.sin(a))
        ml.stud(bp, c, 0.9 + 0.5 * rng.random(), 0.45, nrm, 6, 0.5)
    hr_ = part(m, "hollow_rim", "wood", base=(190, 140, 88), smooth=55)                         # the open end: a thick lip ...
    top = pts[-1]
    ml.collar(hr_, top[1] - 1.4, rad(1.0) * 1.04, 3.0, lip=0.9, sides=10)
    hi = part(m, "hollow_inner", "wood", base=(52, 32, 20), smooth=40)                          # ... and the dark hollow inside it
    ml.lathe(hi, "y", (0, 0, 0), [(top[1] - 6.0, rad(1.0) * 0.5), (top[1] - 0.8, rad(1.0) * 0.82)], sides=10, caps=(True, False), start=0.0)
    kt = 0.62
    kc = (wob(kt)[0] + rad(kt) * 0.98, L * kt * 0.97, wob(kt)[1])
    kn = part(m, "knot_ring", "wood", base=(176, 124, 76), smooth=55)                           # the knot hole in the side (card (375, 520)): a raised ring ...
    ml.collar(kn, kc[0] - 0.4, 2.1, 1.2, lip=0.4, sides=10, axis="x", origin=(0.0, kc[1], kc[2]), oval=1.2)
    ki = part(m, "knot_inner", "wood", base=(60, 38, 24), smooth=40)                            # ... and the dark hollow inside it
    ml.lathe(ki, "x", (0.0, kc[1], kc[2]), [(kc[0] - 0.2, 1.5), (kc[0] + 0.7, 1.7)], sides=10, caps=(True, False), squash=1.2)
    bs = part(m, "branch_stub", "wood", base=(150, 102, 62), smooth=50)                         # the broken side branch (card (230, 215))
    sc = (wob(0.38)[0] + rad(0.38) * 0.7, L * 0.38 * 0.97, wob(0.38)[1] + rad(0.38) * 0.7)
    ml.sweep(bs, [(sc[0] + 0.0, sc[1], sc[2]), (sc[0] + 1.6, sc[1] + 2.4, sc[2] + 1.2), (sc[0] + 3.2, sc[1] + 5.4, sc[2] + 2.2)], ml.circle(1.0, 7), scales=lambda t: (1.5 - 0.4 * t, 1.5 - 0.4 * t))
    bc = part(m, "branch_cut", "wood", base=(214, 170, 112), smooth=40)
    ml.lathe(bc, "y", (sc[0] + 3.2, sc[1] + 5.0, sc[2] + 2.2), [(0.0, 1.0), (0.5, 1.0)], sides=7)
    _tex_common(m, atlas, {}, ("trunk", "bark_vines", "bark_plates"))
    finish("stick_of_youth", m, atlas, L / 100.0, card("img/784541197571846355.jpg", (20, 80, 520, 690), "stick_of_youth"), ("FFFFFF", 0.1, 0.2, 1.0))


# ================================================================ 6 Sun Gift
def build_sun_gift():
    """Card art 736 x 736: a double-bladed glaive, heavy brass spiral-twisted runed shaft, a maroon leather grip in the middle, scrolled brass guards and a
    silver leaf blade at each end with a ruby set at its base. Class: glaive / twin glaive 1.8 to 2.4 m: 200 cm; each blade 40 cm (a fifth, as on the card)."""
    L = 200.0
    Y_LO, Y_HI = 46.0, 154.0                                         # where each blade root and guard sit (card (150, 560) and (560, 160))
    R = 1.75
    m, atlas = fresh("sun_gift")
    sh = part(m, "shaft", "bronze", base=(184, 150, 64), uv_mode="loft", smooth=50)
    pole(sh, Y_LO - 2.0, Y_HI + 2.0, R, R, 10)
    sr = part(m, "spiral_ribs", "bronze", base=(226, 190, 96), smooth=60)                     # the spiral pattern of the shaft: two wires twisting the whole length
    helix_tube(sr, Y_LO + 2.0, Y_HI - 2.0, R * 1.02, 0.5, 26.0, 130, 0.0, 5)
    helix_tube(sr, Y_LO + 2.0, Y_HI - 2.0, R * 1.02, 0.42, 26.0, 130, math.pi, 5)
    gl = part(m, "grip_leather", "leather", base=(112, 28, 44), uv_mode="loft", smooth=50)    # the maroon grip in the middle (card (320..420, 330..410))
    pole(gl, 80.0, 124.0, R * 1.2, R * 1.2, 10, mid=(102.0, 1.05))
    gbd = part(m, "grip_bands", "bronze", base=(210, 170, 70), wear=0.3, smooth=60)
    bands(gbd, [78.0, 124.0, 56.0, 146.0], R * 1.32, 2.0, 0.5, 10)
    gd = part(m, "guards", "bronze", base=(196, 160, 70), wear=0.4, smooth=55)                # the scrolled brass guards at each blade root
    for y, sg in ((Y_HI, 1), (Y_LO, -1)):
        ml.lathe(gd, "y", (0, 0, 0), [(y - sg * 3.0, R * 1.2), (y - sg * 1.0, R * 2.3), (y + sg * 2.5, R * 2.4), (y + sg * 4.5, R * 1.4)], sides=10)
        for dx in (-1, 1):                                             # two scrolls curling away from the guard (card: the claw-like curls at both ends)
            pts = [(dx * (R * 2.0 + 5.4 * math.sin(a)), y - sg * (1.5 + 5.0 * (1 - math.cos(a))), 0.0) for a in np.linspace(0, 2.2, 8)]
            tube_into(gd, pts, 0.8, 0.55, 6)
    bt = part(m, "blade_top", "silver", base=(206, 210, 224), smooth=40, uv_mode="loft")       # the silver leaf blades (card (520..700, 30..170))
    bb = part(m, "blade_bottom", "silver", base=(206, 210, 224), smooth=40, uv_mode="loft")
    wf = w.hermite([(0.0, 2.0), (0.12, 5.2), (0.42, 6.4), (0.78, 3.6), (1.0, 0.0)])
    BL = 44.0
    for (p_, y0, sg) in ((bt, Y_HI + 3.0, 1), (bb, Y_LO - 3.0, -1)):
        ys = [y0 + sg * BL * t for t in np.linspace(0, 1, 11)]
        pts = [(0.0, y, 0.0) for y in ys]
        blade_sweep(p_, pts, wf, lambda t: 0.75 * min(1.0, wf(t) / 2.0), 0.35, up=(0, 0, 1))
    br = part(m, "blade_ribs", "silver", base=(140, 146, 170), glow=True, smooth=60)          # the engraved scroll lines: two ridges per face along each blade
    for y0, sg in ((Y_HI + 3.0, 1), (Y_LO - 3.0, -1)):
        for face in (1, -1):
            for dx in (-1.1, 1.1):
                ml.bar(br, (dx, y0 + sg * 6.0, face * 0.74), (dx * 0.6, y0 + sg * (BL - 9.0), face * 0.5), 0.3, 0.12, up=(0, 0, face), taper=2.5)
    rb = part(m, "rubies", "crystal", base=(232, 40, 36), glow=True, smooth=0)                # the ruby at the base of each blade
    for y, sg in ((Y_HI + 3.0, 1), (Y_LO - 3.0, -1)):
        ml.gem(rb, (0.0, y + sg * 1.5, 0.0), 1.5, long=1.4, sides=4, axis="z")
    cl = part(m, "collars", "bronze", base=(200, 164, 70), wear=0.4, smooth=55)
    bands(cl, [Y_LO + 14.0, Y_HI - 16.0, 60.0, 140.0], R * 1.25, 1.6, 0.4, 10)
    _tex_common(m, atlas, {"blade_ribs": ((255, 244, 180), 0.5), "rubies": ((255, 80, 60), 0.9), "spiral_ribs": ((255, 224, 130), 0.2)}, ("blade_top", "blade_bottom"))
    finish("sun_gift", m, atlas, L / 100.0, card("img/784541197571846387.jpg", (0, 0, 736, 736), "sun_gift"), ("FFF0B0", 0.12, 0.4, 1.0))


# ================================================================ 7 The Last Horseman
def build_the_last_horseman():
    """Card art 736 x 736: a long scythe, silver shaft with sculpted brass fittings, a purple glowing blade with a gold engraved plate at its base and a red
    gem in the collar, violet flame rising from the blade, a purple foot spike. Class: scythe 1.5 to 1.9 m: 180 cm; blade 42 cm across (card: a quarter)."""
    L = 180.0
    Y_TOP = 150.0                                                    # where the shaft ends and the blade collar begins (card (500, 90))
    R = 1.6
    m, atlas = fresh("the_last_horseman")
    sf = part(m, "shaft", "silver", base=(204, 208, 220), uv_mode="loft", smooth=50)
    pole(sf, 6.0, Y_TOP, R, R * 1.1, 10)
    cb = part(m, "collars", "gold", base=(210, 168, 66), wear=0.4, smooth=55)                  # brass rings down the shaft (card (310, 355), (395, 270), (60, 650))
    bands(cb, [28.0, 62.0, 102.0, 120.0], R * 1.35, 2.2, 0.6, 10)
    hf = part(m, "head_fitting", "gold", base=(196, 156, 60), wear=0.4, smooth=55)             # the sculpted brass collar under the blade (card (415, 230..270))
    ml.lathe(hf, "y", (0, 0, 0), [(Y_TOP - 8.0, R * 1.2), (Y_TOP - 6.0, R * 2.1), (Y_TOP - 1.0, R * 2.7), (Y_TOP + 4.0, R * 2.2), (Y_TOP + 6.0, R * 1.3)], sides=10)
    mo = part(m, "mid_ornament", "gold", base=(214, 174, 76), wear=0.4, smooth=55)             # the scrolled brass piece mid-shaft (card (410, 250))
    ml.lathe(mo, "y", (0, 0, 0), [(96.0, R * 1.1), (98.0, R * 1.9), (104.0, R * 2.4), (110.0, R * 1.9), (112.0, R * 1.1)], sides=10)
    ml.lathe(mo, "y", (0, 0, 0), [(34.0, R * 1.1), (35.5, R * 1.8), (39.0, R * 2.0), (42.5, R * 1.8), (44.0, R * 1.1)], sides=10)
    R_ARC, CX, CY = 32.0, -32.0, Y_TOP + 4.0                         # the blade's arc: a crescent of radius 40 cm round (CX, CY), from the collar over the top to a down-hanging tip
    TH0, TH1 = 0.0, 150.0
    ths = np.linspace(TH0, TH1, 20)
    pts = [(CX + R_ARC * math.cos(math.radians(t)), CY + R_ARC * math.sin(math.radians(t)), 0.0) for t in ths]
    wf = w.hermite([(0.0, 5.0), (0.2, 9.0), (0.5, 10.5), (0.8, 6.5), (1.0, 0.0)])      # the blade is broad: 20 cm at the belly (card: a fifth of the shaft's length)
    bd = part(m, "blade", "obsidian", base=(128, 50, 192), glow=True, smooth=40, uv_mode="loft")
    # the blade's edge is on the inside of the arc, so the plate's middle line sits half a width outside it
    pts_mid = [(CX + (R_ARC + wf(k / 19.0) * 0.0) * math.cos(math.radians(t)), CY + R_ARC * math.sin(math.radians(t)), 0.0) for k, t in enumerate(ths)]
    blade_sweep(bd, pts_mid, wf, lambda t: 0.55 * min(1.0, 0.4 + wf(t) / 6.0), 0.3, up=(0, 0, 1))
    be = part(m, "blade_edge", "obsidian", base=(214, 160, 255), glow=True, smooth=60)         # the bright cutting edge along the inner (forward) side
    ml.sweep(be, [(CX + (R_ARC - wf(k / 19.0) * 0.95) * math.cos(math.radians(t)), CY + (R_ARC - wf(k / 19.0) * 0.95) * math.sin(math.radians(t)), 0.0) for k, t in enumerate(ths[1:-1])],
             ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.3, 0.3), up=(0, 0, 1))
    gp = part(m, "gold_plate", "gold", base=(216, 176, 70), glow=True, smooth=50)             # the gold engraved plate on the root of the blade (card (440..540, 90..150))
    for face in (1, -1):
        gpp = [(CX + (R_ARC + 0.0) * math.cos(math.radians(t)), CY + R_ARC * math.sin(math.radians(t)), face * 0.55) for t in np.linspace(5, 70, 9)]
        ml.sweep(gp, gpp, ml.rect_ring(1.0, 1.0), scales=lambda t: (2.4 * math.sin(math.pi * (0.1 + 0.8 * t)) ** 0.6 + 0.4, 0.12), up=(0, 0, 1))
    gm = part(m, "gem", "crystal", base=(230, 36, 52), glow=True, smooth=0)                   # the red gem in the collar
    ml.gem(gm, (0.0, Y_TOP - 1.0, R * 2.55), 1.0, long=1.3, sides=4, axis="z")
    fl = part(m, "flames", "crystal", base=(200, 120, 255), glow=True, smooth=60)             # the violet smoke and flame rising off the blade (card (430..720, 0..180))
    rng = np.random.default_rng(31)
    for k in range(7):
        t = 10 + 20 * k
        r0 = R_ARC + wf(k / 6.0) * 0.9
        bx_, by_ = CX + r0 * math.cos(math.radians(t)), CY + r0 * math.sin(math.radians(t))
        hgt = 9.0 + 7.0 * rng.random()
        sway = 3.0 * (rng.random() - 0.5)
        tube_into(fl, [(bx_, by_, 0.0), (bx_ + sway, by_ + hgt * 0.4, 0.4), (bx_ - sway * 0.5, by_ + hgt * 0.75, -0.3), (bx_ + sway * 0.6, by_ + hgt, 0.0)], 2.4, 0.9, 6)
    ft = part(m, "foot", "obsidian", base=(100, 40, 150), smooth=50)                           # the purple foot spike with its gold cap (card (20..60, 640..700))
    ml.lathe(ft, "y", (0, 0, 0), [(-9.0, 0.0), (-3.0, R * 1.2), (2.0, R * 2.0), (7.0, R * 1.2)], sides=8)
    _tex_common(m, atlas, {"blade": ((190, 100, 255), 0.4), "blade_edge": ((230, 180, 255), 0.9), "gold_plate": ((255, 220, 120), 0.4), "gem": ((255, 70, 80), 0.9), "flames": ((200, 120, 255), 0.7)},
                ("shaft",))
    finish("the_last_horseman", m, atlas, 1.9, card("img/AdJ43YCa18YcV-zZujR3Nb8a1VAY00BhEjYof-KtGrlKP0Db4KIlTsI.jpg", (0, 0, 736, 736), "the_last_horseman"), ("D8A0FF", 0.12, 0.35, 1.0))


# ================================================================ 8 The Legacy
def build_the_legacy():
    """Card art 736 x 736: a warhammer whose grey steel head floats above the handle in split plates (a central block with an engraved plaque and two
    flared wings with engraved panels), blue crystals above and below the block, a teal-wrapped handle with gold rings and a flared pommel.
    Class: hammer 1.1 to 1.5 m: 130 cm. The card: head 45 percent of the height, handle 55 percent; the head is 44 cm wide, 30 cm tall."""
    L = 130.0
    Y_CRADLE, Y_HEAD0, Y_HEAD1 = 92.0, 102.0, 130.0                  # the cup at the top of the handle, the floating head from here (a 10 cm gap: levitation)
    R = 1.85
    m, atlas = fresh("the_legacy")
    hc = part(m, "handle_core", "iron", base=(66, 68, 80), uv_mode="loft", smooth=50)
    pole(hc, 6.0, Y_CRADLE, R, R, 10)
    tw = part(m, "teal_wrap", "leather", base=(30, 160, 150), uv_mode="loft", smooth=50)       # the teal grip with black diagonal stripes (card (540, 330..500))
    pole(tw, 34.0, 80.0, R * 1.2, R * 1.2, 10, mid=(57.0, 1.05))
    ws = part(m, "wrap_stripes", "leather", base=(20, 40, 44), wear=0.3, smooth=60)
    helix_tube(ws, 35.0, 79.0, R * 1.22, 0.4, 7.0, 40, 0.0, 5)
    gr = part(m, "gold_rings", "gold", base=(214, 176, 70), wear=0.4, smooth=55)               # gold rings between the wraps (card y 275, 315, 500, 625)
    bands(gr, [32.0, 38.0, 78.0, 18.0], R * 1.4, 2.0, 0.55, 10)
    pm = part(m, "pommel", "iron", base=(70, 72, 84), smooth=50)                               # the flared pommel (card (540, 640..700))
    ml.lathe(pm, "y", (0, 0, 0), [(-5.0, R * 1.2), (-2.0, R * 2.3), (4.0, R * 2.0), (10.0, R * 1.2), (12.0, R * 1.0)], sides=10, squash=0.9)
    cd = part(m, "cradle", "iron", base=(86, 88, 100), wear=0.4, smooth=50)                    # the cup that holds the head's magnet crystal (card (545, 235..275))
    ml.lathe(cd, "y", (0, 0, 0), [(Y_CRADLE - 6.0, R * 1.1), (Y_CRADLE - 2.0, R * 1.8), (Y_CRADLE + 2.0, R * 2.8), (Y_CRADLE + 6.0, R * 3.0), (Y_CRADLE + 8.0, R * 2.6)], sides=10)
    cr = part(m, "crystals", "crystal", base=(90, 200, 255), glow=True, smooth=0)             # the blue crystals: one below the block, one on top (card (560, 80), (560, 210))
    ml.gem(cr, (0.0, Y_HEAD0 + 0.5, 0.0), 2.6, long=1.4, sides=8, axis="y")
    ml.gem(cr, (0.0, Y_HEAD1 + 2.5, 0.0), 3.4, long=1.3, sides=8, axis="y")
    hb = part(m, "head_core", "iron", base=(96, 98, 110), wear=0.5, smooth=30)                 # the central block with its plaque
    hammer_block(hb, -7.0, 7.0, Y_HEAD0 + 2.0, Y_HEAD1 - 2.0, 4.5, 1.2)
    pq = part(m, "plaque", "iron", base=(60, 62, 72), glow=True, smooth=20)                    # the engraved text plaque: a raised plate with rune lines
    for face in (1, -1):
        ml.box(pq, (-4.2, Y_HEAD0 + 8.0, face * 4.3), (4.2, Y_HEAD1 - 8.0, face * 4.9), 0.3)
        for k in range(6):
            yy = Y_HEAD0 + 9.5 + k * 2.6
            ml.bar(pq, (-3.4, yy, face * 4.9), (3.4 - 0.5 * (k % 3), yy, face * 4.9), 0.5, 0.12, up=(0, 0, face), taper=0.5)
    wg = part(m, "wings", "iron", base=(100, 102, 114), wear=0.5, smooth=35, uv_mode="dominant")  # the two flared wings: plates that open toward the strike faces
    for sg in (-1, 1):
        yc = (Y_HEAD0 + Y_HEAD1) / 2.0
        ml.loft(wg, "x" if sg > 0 else "-x", (0, 0, 0), [(7.0, 10.5, 4.2, yc, 0.0), (10.0, 11.5, 4.0, yc, 0.0), (16.0, 13.5, 3.0, yc, 0.0), (22.0, 15.0, 1.9, yc, 0.0)], ml.bevelled(2.0, 2.0, 0.3))
    wp = part(m, "wing_panels", "iron", base=(54, 56, 66), wear=0.3, smooth=30)               # the engraved panels on each wing face (card: sun / bird / comet scenes)
    for sg in (-1, 1):
        for face in (1, -1):
            for (ya, yb) in ((Y_HEAD0 + 3.0, Y_HEAD0 + 11.0), (Y_HEAD1 - 11.0, Y_HEAD1 - 3.0)):
                x0, x1 = (9.0, 18.0)
                ml.box(wp, (sg * x0 if sg > 0 else -x1, ya, face * 2.9), (x1 if sg > 0 else -x0, yb, face * 3.5), 0.25)
    wt = part(m, "wing_trim", "gold", base=(206, 172, 80), wear=0.4, smooth=45)               # horizontal trim bars across each wing, and the edge bars
    for sg in (-1, 1):
        for yy in (Y_HEAD0 + 14.0, Y_HEAD0 + 15.5 + 2.0, Y_HEAD1 - 17.5):
            for face in (1, -1):
                a, b = ((9.0, 20.0) if sg > 0 else (-20.0, -9.0))
                ml.bar(wt, (a, yy, face * 3.3), (b, yy, face * 3.3), 0.5, 0.14, up=(0, 0, face), taper=0.5)
    _tex_common(m, atlas, {"crystals": ((120, 220, 255), 0.95), "plaque": ((150, 210, 255), 0.25)}, ("head_core", "wings", "handle_core"))
    finish("the_legacy", m, atlas, L / 100.0, card("img/784541197571850906.jpg", (400, 0, 736, 736), "the_legacy"), ("90E0FF", 0.14, 0.4, 1.0))


# ================================================================ 9 The Librarian's Maul
def build_the_librarians_maul():
    """Card art 533 x 533: a thick granite book with a teal cover and grey page edges, mounted end-on on a long thin wooden pole with three pale bands.
    Class: hammer / maul 1.1 to 1.5 m: 140 cm. The book is 38 cm long, 26 wide, 16 thick (card: a quarter of the length, the pole a thumb thick)."""
    L = 140.0
    Y_B0, Y_B1 = 102.0, 140.0                                        # the book's foot and head (it sits end-on on the pole: card (290, 140) ... (440, 60))
    HX, HZ = 13.0, 8.0
    m, atlas = fresh("the_librarians_maul")
    pl = part(m, "pole", "wood", base=(172, 134, 84), uv_mode="loft", smooth=50)
    pole(pl, 0.0, Y_B0 + 6.0, 1.35, 1.6, 8, mid=(60.0, 1.1))
    pb = part(m, "pole_bands", "wood", base=(226, 200, 150), wear=0.3, smooth=60)             # the three pale bands (card (95, 285), (115, 265), (150, 240))
    bands(pb, [18.0, 24.0, 34.0], 1.65, 2.0, 0.5, 8)
    pc = part(m, "pole_cap", "wood", base=(150, 112, 66), smooth=55)
    ml.lathe(pc, "y", (0, 0, 0), [(-2.0, 1.0), (-0.5, 1.9), (2.0, 1.7), (3.0, 1.4)], sides=8)
    cb = part(m, "cover_boards", "stone", base=(70, 150, 154), wear=0.5, smooth=30)           # the teal granite covers: front and back boards, thick and square
    for face in (1, -1):
        ml.box(cb, (-HX, Y_B0, face * (HZ - 1.8) if face > 0 else -HZ), (HX, Y_B1, HZ if face > 0 else -(HZ - 1.8)), 0.6)
    sg = part(m, "spine", "stone", base=(54, 128, 134), wear=0.5, smooth=45, uv_mode="loft")   # the rounded spine at the back (+x)
    ml.loft(sg, "y", (HX - 1.2, 0, 0), [(Y_B0, 1.6, HZ), (Y_B0 + 2.0, 2.2, HZ * 1.02), (Y_B1 - 2.0, 2.2, HZ * 1.02), (Y_B1, 1.6, HZ)], ml.circle(1.0, 10))
    pg = part(m, "pages", "stone", base=(176, 178, 176), wear=0.5, smooth=25)                  # the stone page block between the covers: grey, a little inset
    ml.box(pg, (-HX + 1.0, Y_B0 + 1.2, -(HZ - 1.7)), (HX - 1.0, Y_B1 - 1.2, HZ - 1.7), 0.3)
    pgl = part(m, "page_lines", "stone", base=(120, 122, 122), smooth=20)                      # the page edges: fine ridges along the fore-edge (-x) and the head
    for k in range(7):
        zz = -(HZ - 2.4) + k * (2 * (HZ - 2.4) / 6.0)
        ml.bar(pgl, (-HX + 0.9, Y_B0 + 2.0, zz), (-HX + 0.9, Y_B1 - 2.0, zz), 0.32, 0.12, up=(-1, 0, 0), taper=3.0)
        ml.bar(pgl, (-HX + 1.8, Y_B1 - 1.0, zz), (HX - 1.8, Y_B1 - 1.0, zz), 0.3, 0.12, up=(0, 1, 0), taper=3.0)
    cg = part(m, "corner_guards", "stone", base=(48, 112, 118), wear=0.5, smooth=40)           # stone corner caps
    for yy in (Y_B0 + 0.2, Y_B1 - 0.2):
        for sx in (-1, 1):
            for sz in (-1, 1):
                ml.stud(cg, (sx * (HX - 0.3), yy, sz * (HZ - 0.4)), 1.7, 0.9, (sx * 0.6, 1.0 if yy > Y_B0 + 5 else -1.0, sz * 0.6), 6, 0.5)
    tp = part(m, "title_plate", "stone", base=(130, 200, 196), glow=True, smooth=25)           # a plate with a rune title on the front cover
    for face in (1, -1):
        ml.box(tp, (-7.0, Y_B0 + 12.0, face * (HZ - 0.2)), (7.0, Y_B0 + 26.0, face * (HZ + 0.5)), 0.3)
        for k in range(3):
            ml.bar(tp, (-4.4, Y_B0 + 16.0 + k * 3.2, face * (HZ + 0.5)), (4.4 - k, Y_B0 + 16.0 + k * 3.2, face * (HZ + 0.5)), 0.7, 0.12, up=(0, 0, face), taper=0.8)
    sb = part(m, "spine_bands", "stone", base=(40, 100, 108), wear=0.4, smooth=50)             # the raised bands across the spine
    for yy in (Y_B0 + 8.0, Y_B0 + 17.0, Y_B0 + 26.0):
        ml.bar(sb, (HX + 0.5, yy, -HZ + 1.0), (HX + 0.5, yy, HZ - 1.0), 0.9, 0.5, up=(1, 0, 0), taper=1.5)
    _tex_common(m, atlas, {"title_plate": ((150, 240, 230), 0.6)}, ("cover_boards", "pages", "spine", "pole"))
    finish("the_librarians_maul", m, atlas, L / 100.0, card("img/784541197571846394.jpg", (60, 20, 500, 320), "the_librarians_maul"), ("FFFFFF", 0.1, 0.3, 1.0))


# ================================================================ 10 Time hammer
def build_time_hammer():
    """Card art 736 x 1713 (a thin sketch): a double-headed hammer of cracked grey stone slabs round a rune-carved stone core, a thin carved handle, glowing
    red-orange fissures and runes, floating stone fragments at the pommel. Class: hammer 1.1 to 1.5 m: 130 cm. The card: head 22 percent of the height, 40 cm wide."""
    L = 130.0
    Y_NECK, Y_H0, Y_H1 = 88.0, 98.0, 126.0
    R = 1.5
    m, atlas = fresh("time_hammer")
    hd = part(m, "handle", "stone", base=(176, 170, 162), uv_mode="loft", smooth=45)
    pole(hd, 8.0, Y_NECK, R, R * 1.1, 8, mid=(50.0, 1.15))
    hr = part(m, "handle_runes", "obsidian", base=(255, 130, 60), glow=True, smooth=30)       # the runes carved down the handle (card y 960..1330)
    for k in range(6):
        y = 20.0 + k * 10.0
        ml.bar(hr, (-0.5, y, R * 1.0), (0.7, y + 3.4, R * 1.0), 0.45, 0.16, up=(0, 0, 1), taper=0.3)
        ml.bar(hr, (0.4, y + 1.0, -R * 1.0), (-0.6, y + 4.4, -R * 1.0), 0.4, 0.16, up=(0, 0, -1), taper=0.3)
    hg = part(m, "handle_rings", "stone", base=(150, 144, 138), glow=True, wear=0.6, smooth=50)    # cracked stone rings with glowing seams (card (190, 1070..1150))
    for y in (18.0, 44.0, 70.0):
        ml.collar(hg, y, R * 1.45, 3.0, lip=0.8, sides=9)
    pmm = part(m, "pommel", "stone", base=(140, 134, 128), wear=0.6, smooth=45)               # the rough stone pommel
    ml.lathe(pmm, "y", (0, 0, 0), [(-4.0, 1.2), (-1.0, R * 2.1), (4.0, R * 2.4), (9.0, R * 1.4), (10.0, R * 1.0)], sides=8)
    fr = part(m, "fragments", "stone", base=(200, 120, 70), glow=True, wear=0.5, smooth=0)    # the stone fragments floating round the pommel (card (180..215, 1290..1350))
    for (xx, yy, zz, s) in ((-3.5, -9.0, 1.0, 1.2), (4.0, -6.0, -1.2, 1.0), (1.5, -13.0, -0.6, 0.8), (-2.0, 14.0, 1.8, 0.8), (3.2, 18.0, -0.8, 0.9)):
        ml.gem(fr, (xx, yy, zz), s, long=1.3, sides=5, axis="y")
    nk = part(m, "neck", "stone", base=(150, 144, 138), wear=0.6, smooth=40)                   # the carved joint under the head
    ml.lathe(nk, "y", (0, 0, 0), [(Y_NECK - 4.0, R * 1.2), (Y_NECK, R * 2.2), (Y_NECK + 5.0, R * 2.8), (Y_H0 + 2.0, R * 2.2)], sides=8)
    cb = part(m, "core_block", "stone", base=(170, 164, 156), wear=0.6, smooth=25)            # the rune-carved core (card (180, 70..130)): a chamfered block
    hammer_block(cb, -8.0, 8.0, Y_H0 + 1.0, Y_H1 - 1.0, 7.0, 2.0)
    cr = part(m, "core_rune", "obsidian", base=(255, 130, 70), glow=True, smooth=40)          # the spiral rune on the core
    for face in (1, -1):
        sp = []
        for i in range(0, 18):
            t = i / 17.0
            a = t * 9.0
            r = 5.2 * (1.0 - 0.8 * t)
            sp.append((r * math.cos(a) * (1 if face > 0 else -1), (Y_H0 + Y_H1) / 2 + r * math.sin(a), face * 7.0))
        ml.sweep(cr, sp, ml.circle(1.0, 5), scales=lambda t: (0.42, 0.18), caps=(True, True), up=(0, 0, face))
    sl = part(m, "slabs", "stone", base=(184, 180, 172), wear=0.6, smooth=25)                  # the two cracked slab heads (card (105..160, 45..135) and (225..265, 45..135))
    for sg in (-1, 1):
        x0, x1 = (10.0, 26.0) if sg > 0 else (-26.0, -10.0)
        hammer_block(sl, x0, x1, Y_H0 - 2.0, Y_H1 + 2.0, 6.5, 2.2)
    ck = part(m, "slab_cracks", "obsidian", base=(255, 110, 50), glow=True, smooth=30)        # the glowing fissures over each slab face
    rng = np.random.default_rng(10)
    for sg in (-1, 1):
        for face in (1, -1):
            x = sg * 18.0
            y = Y_H0 + 1.0
            for k in range(5):
                nx = x + sg * (2.0 + 2.5 * rng.random()) * (1 if k % 2 else -0.6)
                ny = y + 2.0 + 2.2 * rng.random()
                nx = min(max(nx, sg * 11.5), sg * 24.5) if sg > 0 else max(min(nx, sg * 11.5), sg * 24.5)
                ml.bar(ck, (x, y, face * 6.5), (nx, ny, face * 6.5), 0.5, 0.18, up=(0, 0, face), taper=0.4)
                x, y = nx, ny
    sf = part(m, "slab_frames", "stone", base=(150, 146, 140), wear=0.6, smooth=40)           # raised frames round each slab face
    for sg in (-1, 1):
        for face in (1, -1):
            a, b = (sg * 11.5, sg * 24.5)
            for yy in (Y_H0 + 0.5, Y_H1 - 0.5):
                ml.bar(sf, (a, yy, face * 6.6), (b, yy, face * 6.6), 0.9, 0.35, up=(0, 0, face), taper=1.0)
            for xx in (a, b):
                ml.bar(sf, (xx, Y_H0 + 0.5, face * 6.6), (xx, Y_H1 - 0.5, face * 6.6), 0.9, 0.35, up=(0, 0, face), taper=1.0)
    _tex_common(m, atlas, {"handle_runes": ((255, 140, 60), 0.9), "handle_rings": ((255, 110, 50), 0.3), "fragments": ((255, 140, 70), 0.6), "core_rune": ((255, 150, 80), 0.95),
                           "slab_cracks": ((255, 120, 50), 0.95)}, ("handle", "core_block", "slabs"))
    finish("time_hammer", m, atlas, L / 100.0, card("img/Monsters & More (@monsters-and-more) on Tumblr (3).jpg", (60, 40, 480, 760), "time_hammer"), ("FF9050", 0.12, 0.4, 1.0))


# ================================================================ 11 Vanguard Hammer
def build_vanguard_hammer():
    """Card art 480 x 480: a grey steel warhammer, blocky head banded with a red stripe and a spike on the back, a long grey haft with a pommel knob and a
    ribbon of crimson cloth spiralling down it. Class: hammer 1.1 to 1.5 m: 125 cm. The head is 30 cm long, 18 cm tall (card: a quarter of the length)."""
    L = 125.0
    Y_SOCKET, Y_H0, Y_H1 = 90.0, 96.0, 118.0
    HX, HZ = 15.0, 8.5
    R = 1.7
    m, atlas = fresh("vanguard_hammer")
    hf = part(m, "haft", "steel", base=(150, 154, 164), uv_mode="loft", smooth=50)
    pole(hf, 3.0, Y_SOCKET + 4.0, R, R, 10)
    pm = part(m, "pommel", "steel", base=(120, 124, 136), wear=0.5, smooth=50)                 # the pommel knob (card (95, 410))
    ml.lathe(pm, "y", (0, 0, 0), [(-4.0, 1.0), (-2.5, 2.4), (0.5, 2.8), (3.5, R * 1.3), (4.5, R * 1.0)], sides=10)
    hr = part(m, "haft_rings", "steel", base=(110, 114, 126), wear=0.4, smooth=55)
    bands(hr, [8.0, 13.0, 76.0], R * 1.3, 2.0, 0.5, 10)
    sk = part(m, "socket", "steel", base=(130, 134, 146), wear=0.4, smooth=50)
    ml.lathe(sk, "y", (0, 0, 0), [(Y_SOCKET - 4.0, R * 1.2), (Y_SOCKET - 1.0, R * 2.0), (Y_H0 + 1.0, R * 2.4), (Y_H0 + 2.0, R * 1.8)], sides=10)
    hd = part(m, "head", "steel", base=(156, 160, 170), wear=0.6, smooth=25)                   # the blocky head, struck face at -x
    hammer_block(hd, -HX, HX - 4.0, Y_H0, Y_H1, HZ, 1.6)
    rs = part(m, "red_stripe", "cloth", base=(196, 28, 54), wear=0.3, glow=True, smooth=30)    # the red stripe slanting over the head (card (320, 60..120))
    for face in (1, -1):
        ml.bar(rs, (-HX + 2.0, Y_H0 + 5.0, face * HZ), (HX - 6.0, Y_H1 - 4.0, face * HZ), 3.6, 0.35, up=(0, 0, face), taper=1.5)
    ml.bar(rs, (-HX - 0.1, Y_H0 + 5.0, 0.0), (-HX - 0.1, Y_H1 - 4.0, 0.0), 3.6, 0.3, up=(-1, 0, 0), taper=1.5)     # and across the striking face
    ht = part(m, "head_trim", "steel", base=(120, 124, 136), wear=0.6, smooth=40)              # raised plate edges and rivets
    for face in (1, -1):
        for yy in (Y_H0 + 1.5, Y_H1 - 1.5):
            ml.bar(ht, (-HX + 1.0, yy, face * HZ), (HX - 5.0, yy, face * HZ), 1.2, 0.35, up=(0, 0, face), taper=1.2)
        for (xx, yy) in ((-HX + 2.5, Y_H0 + 3.0), (-HX + 2.5, Y_H1 - 3.0), (HX - 6.5, Y_H0 + 3.0), (HX - 6.5, Y_H1 - 3.0)):
            ml.rivet(ht, (xx, yy, face * HZ), 0.7, (0, 0, face), 6)
    spk = part(m, "spike", "steel", base=(170, 174, 184), smooth=45, uv_mode="loft")           # the spike on the back (+x)
    ml.loft(spk, "x", (HX - 4.5, (Y_H0 + Y_H1) / 2, 0), [(0.0, 3.6, 3.6), (2.0, 3.2, 3.2), (6.0, 1.6, 1.6), (9.5, 0.0, 0.0)], ml.circle(1.0, 8))
    rb = part(m, "ribbon", "cloth", base=(190, 24, 52), smooth=40, uv_mode="loft")            # the crimson ribbon wound down the haft (card (170..290, 120..300))
    pts = []
    for i in range(0, 34):
        t = i / 33.0
        a = t * 3.2 * 2 * math.pi
        r = R * 1.5 + 6.5 * math.sin(math.pi * min(1.0, t * 1.1)) ** 1.3
        pts.append((r * math.cos(a), 84.0 - 66.0 * t, r * math.sin(a)))
    ml.sweep(rb, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.9 * (1.0 - 0.3 * t), 0.07), up=(0, 1, 0), caps=(False, False))
    rt = part(m, "ribbon_tail", "cloth", base=(160, 18, 44), smooth=40, uv_mode="loft")       # the loose end flying off the head
    ml.sweep(rt, [(-2.0 + 3.0 * k, 88.0 + 1.6 * k, 4.0 + 2.2 * math.sin(k * 0.9)) for k in range(0, 9)], ml.rect_ring(1.0, 1.0), scales=lambda t: (1.6 * (1.0 - 0.6 * t), 0.07), up=(0, 0, 1))
    _tex_common(m, atlas, {"red_stripe": ((255, 50, 90), 0.25)}, ("haft", "head"))
    finish("vanguard_hammer", m, atlas, L / 100.0, card("img/784541197571851338.jpg", (80, 30, 440, 320), "vanguard_hammer"), ("FFFFFF", 0.1, 0.25, 1.0))


# ================================================================ 12 Willbreaker
def build_willbreaker():
    """Card art 736 x 919 (a flat illustration): a long dark iron-grey war hammer, a flat grey head engraved with an eye, red tassels tied at three points down
    the shaft and a crosshatched grip. Class: hammer 1.1 to 1.5 m, drawn long: 150 cm. The head is 28 cm wide and 20 cm tall; the shaft 128 cm."""
    L = 150.0
    Y_SOCKET, Y_H0, Y_H1 = 124.0, 128.0, 148.0
    HX, HZ = 14.0, 4.6
    R = 1.5
    m, atlas = fresh("willbreaker")
    hf = part(m, "haft", "iron", base=(66, 68, 78), uv_mode="loft", smooth=50)
    pole(hf, 3.0, Y_SOCKET + 4.0, R, R, 8, mid=(70.0, 1.05))
    gp = part(m, "grip", "leather", base=(46, 38, 44), uv_mode="loft", smooth=50)              # the crosshatched grip on the lower end (card (230, 780..900))
    pole(gp, 0.0, 30.0, R * 1.2, R * 1.2, 8, mid=(15.0, 1.05))
    gx = part(m, "grip_cross", "leather", base=(110, 100, 110), smooth=60)                     # its diamond cross-hatching: two opposite spirals
    helix_tube(gx, 1.0, 29.0, R * 1.24, 0.28, 3.5, 30, 0.0, 4)
    helix_tube(gx, 1.0, 29.0, R * 1.24, 0.28, -3.5, 30, 0.0, 4)
    bt = part(m, "butt", "iron", base=(56, 58, 68), smooth=50)
    ml.lathe(bt, "y", (0, 0, 0), [(-2.5, 0.8), (-0.5, R * 1.5), (2.0, R * 1.9), (4.0, R * 1.3)], sides=8)
    sk = part(m, "socket", "iron", base=(80, 82, 94), wear=0.5, smooth=50)
    ml.lathe(sk, "y", (0, 0, 0), [(Y_SOCKET - 5.0, R * 1.2), (Y_SOCKET - 2.0, R * 2.1), (Y_H0 + 1.0, R * 2.4), (Y_H0 + 2.0, R * 1.9)], sides=8)
    hd = part(m, "head", "iron", base=(116, 118, 128), wear=0.6, smooth=25)                    # the flat head, struck faces at both ends
    hammer_block(hd, -HX, HX, Y_H0, Y_H1, HZ, 1.0)
    hfl = part(m, "head_flange", "iron", base=(86, 88, 100), wear=0.6, smooth=40)              # the raised lip round the head face
    for face in (1, -1):
        for yy in (Y_H0 + 1.0, Y_H1 - 1.0):
            ml.bar(hfl, (-HX + 1.0, yy, face * HZ), (HX - 1.0, yy, face * HZ), 1.0, 0.3, up=(0, 0, face), taper=1.5)
        for xx in (-HX + 1.0, HX - 1.0):
            ml.bar(hfl, (xx, Y_H0 + 1.0, face * HZ), (xx, Y_H1 - 1.0, face * HZ), 1.0, 0.3, up=(0, 0, face), taper=1.5)
    ey = part(m, "eye", "obsidian", base=(40, 14, 20), glow=True, smooth=30)                   # the engraved eye (card (205, 105)): an almond outline round a pupil
    for face in (1, -1):
        ec = ((Y_H0 + Y_H1) / 2)
        top = [(-7.0 + 14.0 * t, ec + 3.6 * math.sin(math.pi * t), face * HZ) for t in np.linspace(0, 1, 9)]
        bot = [(-7.0 + 14.0 * t, ec - 3.6 * math.sin(math.pi * t), face * HZ) for t in np.linspace(0, 1, 9)]
        for line in (top, bot):
            ml.sweep(ey, line, ml.circle(1.0, 5), scales=lambda t: (0.35, 0.18), caps=(True, True), up=(0, 0, face))
        ml.stud(ey, (0.0, ec, face * HZ), 1.6, 0.5, (0, 0, face), 8, 0.4)
    tk = part(m, "tassel_knots", "cloth", base=(196, 30, 40), smooth=55)                       # the red knots tied round the shaft (card y 245, 400, 775)
    KN = [112.0, 80.0, 40.0]
    bands(tk, KN, R * 1.35, 2.4, 0.7, 8)
    ts = part(m, "tassels", "cloth", base=(200, 34, 44), smooth=55)                            # the tassels hanging from each knot with a frayed end
    for k, y in enumerate(KN):
        for j in range(3):
            sw = 0.7 * (j - 1)
            tube_into(ts, [(R * 1.4, y, 0.0), (R * 1.4 + 2.6 + sw, y - 4.0, 0.5 * (j - 1)), (R * 1.4 + 6.0 + sw * 2, y - 9.0, 0.8 * (j - 1)), (R * 1.4 + 8.0 + sw * 3, y - 14.0 - 2.0 * (k % 2), 0.0)], 1.2 - 0.3 * (j % 2), 0.8, 5)
    _tex_common(m, atlas, {"eye": ((255, 70, 60), 0.5)}, ("haft", "head"))
    finish("willbreaker", m, atlas, L / 100.0, card("img/784541197571848400.jpg", (60, 20, 360, 900), "willbreaker"), ("FFFFFF", 0.1, 0.25, 1.0))


BUILDERS = {i: globals()["build_" + i] for i in MODELS}


def main(only=None):
    for item_id in MODELS:
        if only and item_id not in only:
            continue
        BUILDERS[item_id]()


if __name__ == "__main__":
    main(sys.argv[1:])
