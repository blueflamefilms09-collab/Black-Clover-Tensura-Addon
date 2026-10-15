"""Relics of Many Worlds, part 4, wave 22: the twelve staves and wands as 3D hand models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave22_models.py [id ...]       (no ids = all twelve; idempotent, deterministic)
    W22_PREVIEW=<dir> python3 -B tools/relics/wave22_models.py lich_wand      (also renders the preview beside the card)

Run tools/relics/wave22_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon).
Per id it writes assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals) and textures/entity/weapon/<id>.png and <id>_glow.png (one 512 px
skin each). Budget: 8 to 14 named parts and at most 2800 quads per weapon.

The method, in order, for every weapon (each build_<id>() follows it):
  0 target decided: the card's art and the printed text.
  1 measure the reference: the card pixels along one known length give centimetres (K = real length / card px); the other proportions are NAMED
    CONSTANTS written as card px times K, with the measured px in a comment. Where the card paints a thing heavier than a hand could hold the radius is
    scaled by THIN (a stated share), as the earlier staves do.
  2 geometry from real cross-sections (loft / sweep / lathe), never a box or a four-sided frustum for a long form.
  3 the small raised parts are the detail: each its own named Part with its own constants.
  4 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one skin per weapon.
  5 painted material plus a second additive glow skin.
  6 rendered beside the reference (W22_PREVIEW) and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.
Sizes follow the 1.34 size rule: staves are built at the staff class length (2.0 - 2.6 m), wands at the wand class length (0.8 - 1.2 m).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ["W1_PREVIEW"] = os.environ.get("W22_PREVIEW", "")             # wave1_models' finish() / reference() read this one
import wave1_models as w                                                # noqa: E402
import mesh_lib as ml                                                   # noqa: E402

CARDS = os.environ.get("RELICS4", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics4/img")
w.ZIP = CARDS                                                           # the cards of this set live under scratchpad/relics4/img


# ================================================================ shared helpers
def mk(name):
    return ml.Mesh(name)


def part(m, name, material, base=None, **kw):
    return w.add(m, name, material, base=base, **kw)


def lath(m, name, material, profile, sides=10, base=None, origin=(0, 0, 0), axis="y", squash=1.0, caps=(True, True), **kw):
    """A surface of revolution as its own named part (profile = [(s, radius)] along the axis)."""
    p = part(m, name, material, base=base, **kw)
    ml.lathe(p, axis, origin, profile, sides=sides, squash=squash, caps=caps)
    return p


def loft_y(m, name, material, stations, sides=10, base=None, squash=1.0, **kw):
    """A round loft along y: stations = [(y, radius)]; its own part, UV-wrapped along the form."""
    p = part(m, name, material, base=base, uv_mode="loft", **kw)
    ml.loft(p, "y", (0, 0, 0), [(s, r, r * squash) for (s, r) in stations], ml.circle(1.0, sides))
    return p


def rings(m, name, material, ys, r, h, sides=10, base=None, lip=0.2, **kw):
    """Binding rings / collars at the heights ys (each its own raised band in one part)."""
    p = part(m, name, material, base=base, **kw)
    for y in ys:
        ml.collar(p, y, r, h, lip=min(lip, h / 2 - 1e-3), sides=sides)
    return p


def orb(m, name, material, c, r, base=None, sides=14, stacks=10, ry=None, rz=None, **kw):
    p = part(m, name, material, base=base, smooth=70, **kw)
    w.ellipsoid(p, c, r, r if ry is None else ry, r if rz is None else rz, sides=sides, stacks=stacks)
    return p


def tube_into(p, pts, radius, taper=0.7, sides=6, point=True):
    """A tapering round tube along a polyline, added to the part p."""
    def sc(t):
        r = radius * (1.0 - taper * t)
        return (r, r) if (t < 0.97 or not point) else (0.0, 0.0)
    ml.sweep(p, pts, ml.circle(1.0, sides), scales=sc)


def tube(m, name, material, pts, radius, taper=0.7, sides=6, base=None, point=True, **kw):
    p = part(m, name, material, base=base, uv_mode="loft", smooth=70, **kw)
    tube_into(p, pts, radius, taper, sides, point)
    return p


def wavy(y0, y1, n, amp, turns, phase=0.0, ax=0.0, az=0.0):
    """A gnarled centre line from y0 to y1: it wanders by `amp` cm round the axis, `turns` times."""
    out = []
    for i in range(n + 1):
        t = i / n
        a = phase + 2 * math.pi * turns * t
        out.append((ax + amp * math.sin(a), y0 + (y1 - y0) * t, az + amp * math.cos(a * 0.8 + 1.0)))
    return out


def gnarled(m, name, material, y0, y1, r0, r1, amp, turns, base=None, n=36, sides=8, phase=0.0, bulge=0.12, **kw):
    """A wandering round shaft: radius from r0 to r1 with a slow swell, wobbling about the axis (a wooden staff, a bone)."""
    p = part(m, name, material, base=base, uv_mode="loft", smooth=60, **kw)
    path = wavy(y0, y1, n, amp, turns, phase)
    ml.sweep(p, path, ml.circle(1.0, sides), scales=lambda t: ((r0 + (r1 - r0) * t) * (1.0 + bulge * math.sin(t * math.pi * 5)),) * 2)
    return p


def helix_pts(r_of_t, y0, y1, turns, n, phase=0.0, cx=0.0, cz=0.0):
    return w.helix(r_of_t, y0, y1, turns, n, phase=phase, ax=cx, az=cz)


def ring_path(cx, cy, cz, r, n=28, rot=()):
    """A closed circle of radius r in the xy plane at (cx, cy, cz), turned by the (axis, degrees) steps in rot (about its own centre)."""
    pts = []
    for i in range(n + 1):
        a = 2 * math.pi * i / n
        p = np.array([r * math.cos(a), r * math.sin(a), 0.0])
        for ax, deg in rot:
            c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
            if ax == "x":
                p = np.array([p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c])
            elif ax == "y":
                p = np.array([p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c])
            else:
                p = np.array([p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2]])
        pts.append((cx + p[0], cy + p[1], cz + p[2]))
    return pts


def band_ring(m, name, material, pts, radius, base=None, sides=8, **kw):
    p = part(m, name, material, base=base, smooth=45, **kw)
    ml.sweep(p, pts, ml.circle(radius, sides), caps=(False, False))
    return p


def arc(cx, cy, r, a0, a1, n=18, z=0.0):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n)), z) for i in range(n + 1)]


def crescent_into(p, cx, cy, r_out, width, half_deg, centre_deg, thick, n=26):
    """A crescent swept along an arc, widest in the middle, closing to points at both tips (cx, cy = centre of its outer circle)."""
    def wd(u):
        return width * max(0.0, 1.0 - (abs(u) / half_deg) ** 2)
    pts = []
    for i in range(n + 1):
        u = -half_deg + 2 * half_deg * i / n
        rr = r_out - wd(u) / 2
        a = math.radians(centre_deg + u)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a), 0.0))
    ml.sweep(p, pts, ml.bevelled(1.0, 1.0, 0.55), scales=lambda t: (wd(-half_deg + 2 * half_deg * t) if 0 < t < 1 else 0.0, thick * (math.sqrt(max(wd(-half_deg + 2 * half_deg * t), 0.0) / width) * 0.9 + 0.1) if 0 < t < 1 else 0.0))


def gem_into(p, c, r, long=1.5, sides=6, axis="y"):
    ml.gem(p, c, r, long, sides, axis)


def glow_on(atlas, m, spec):
    """spec = {part name: (rgb, strength, core)}: fills the glow skin of those parts (they must be glow=True parts)."""
    for nm, (rgb, st, core) in spec.items():
        w.paint_part(atlas, m.part(nm), lambda c, rgb=rgb, st=st, core=core: w.set_glow(c, rgb, st, core=core))


def finish(m, item_id, real_m, glow, ref=None, paint=None, glow_spec=None):
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    if paint:
        paint(m, atlas)
    if glow_spec:
        glow_on(atlas, m, glow_spec)
    w.finish(item_id, m, atlas, real_m=real_m, glow=glow, ref=ref)


def stripes(c, cols, k=3.0):
    """Paints diagonal bands of the given colours over a part's patch (a crystal's pink / blue / white)."""
    idx = np.floor((c.T * k + c.S * 1.7) * len(cols)) % len(cols)
    for i, col in enumerate(cols):
        sel = (idx == i)[..., None]
        c.img[..., :3] = np.where(sel, c.img[..., :3] * 0.35 + np.array(col, np.float32) * 0.65, c.img[..., :3])


# ================================================================ 1 Dreamcore Woven Wand
def build_dreamcore_woven_wand():
    """Card 736 x 736: the wand runs from the foot spike (583, 372) to the crystal's top (150, 92): 515 px; a wand is 1.0 m (wand class), K = 0.194 cm/px."""
    L = 100.0
    K = L / 515.0
    THIN = 0.6                                                      # the card paints the wrap 28 px wide: scaled to a hand-sized wand
    SHAFT_R0, SHAFT_R1 = 7 * K * 1.4, 9 * K * 1.4                   # shaft half width 7..9 px at the foot / below the wrap
    WRAP = (20.0, 47.0)                                             # crimson wrap, card (520, 330) to (392, 262): 146 px = 28 cm, up the wand from y 20
    WRAP_R = 14 * K * THIN * 1.6
    CRYSTAL = (84.0, 101.0)                                         # card (190,128) to (255,170): 75 px; the crystal sits in the tendrils at the top
    CRYSTAL_R = 20 * K * THIN * 1.7
    m = mk("dreamcore_woven_wand")
    lath(m, "foot_spike", "wood", [(0, 0.0), (1.4, 0.55), (8.0, 1.0)], sides=8, base=(120, 84, 58), smooth=45)
    gnarled(m, "shaft", "wood", 7.0, 86.0, SHAFT_R0, SHAFT_R1, 0.5, 3.0, base=(118, 82, 58), sides=9)
    loft_y(m, "wrap", "cloth", [(WRAP[0], WRAP_R * 0.9), (WRAP[0] + 5, WRAP_R), ((WRAP[0] + WRAP[1]) / 2, WRAP_R * 1.06), (WRAP[1] - 5, WRAP_R), (WRAP[1], WRAP_R * 0.92)],
           sides=12, base=(168, 38, 70))
    rings(m, "wrap_bands", "cloth", [WRAP[0] + 3 + 3.3 * i for i in range(7)], WRAP_R * 1.05, 0.9, sides=12, base=(206, 80, 110), lip=0.25)
    rings(m, "wrap_caps", "wood", [WRAP[0] - 1.2, WRAP[1] - 0.2], WRAP_R * 1.12, 1.4, sides=12, base=(90, 60, 42), lip=0.3)
    tube(m, "vine", "wood", helix_pts(lambda t: SHAFT_R1 * 1.5, WRAP[1] + 2, 90.0, 2.4, 60), 0.5, taper=0.5, sides=6, base=(94, 62, 44))
    kn = part(m, "knots", "wood", base=(104, 70, 48), smooth=70)
    for yy, rx in ((60.0, 1.9), (71.0, 2.2)):
        w.ellipsoid(kn, (0, yy, 0), rx, rx * 1.5, rx, sides=10, stacks=8)
    tn = part(m, "tendrils", "wood", base=(100, 68, 48), uv_mode="loft", smooth=70)
    for k in range(5):
        a = 2 * math.pi * k / 5
        pts = [(0.6 * math.cos(a), 84.0, 0.6 * math.sin(a)), (2.6 * math.cos(a), 88.0, 2.6 * math.sin(a)), (3.7 * math.cos(a + 0.4), 94.0, 3.7 * math.sin(a + 0.4)),
               (3.0 * math.cos(a + 0.8), 100.0, 3.0 * math.sin(a + 0.8)), (1.4 * math.cos(a + 1.2), 103.0, 1.4 * math.sin(a + 1.2))]
        tube_into(tn, pts, 0.5, 0.75, 6)
    hk = part(m, "tip_curls", "wood", base=(96, 64, 46), uv_mode="loft", smooth=70)
    tube_into(hk, [(0.0, 100.0, 0.0), (-2.0, 105.0, 0.5), (-5.0, 108.0, 0.0), (-8.0, 107.0, -0.6), (-9.5, 104.0, 0.0)], 0.42, 0.8, 6)
    tube_into(hk, [(0.5, 99.0, 0.5), (3.5, 103.0, 1.0), (6.0, 102.0, 0.0), (6.5, 99.0, -0.8)], 0.35, 0.8, 6)
    cr = part(m, "crystal", "crystal", base=(240, 160, 210), glow=True, smooth=0)
    ml.lathe(cr, "y", (0, 0, 0), [(CRYSTAL[0], 0.0), (CRYSTAL[0] + 2.4, CRYSTAL_R * 0.8), (CRYSTAL[0] + 8, CRYSTAL_R), (CRYSTAL[1] - 3.5, CRYSTAL_R * 0.85), (CRYSTAL[1], 0.0)], sides=6)
    rings(m, "cradle_ring", "wood", [CRYSTAL[0] + 1.6], CRYSTAL_R * 0.95, 1.3, sides=8, base=(88, 58, 42), lip=0.3)

    def paint(m_, atlas):
        w.paint_part(atlas, m_.part("crystal"), lambda c: stripes(c, [(255, 150, 200), (130, 190, 255), (255, 255, 255)], 1.0))
    finish(m, "dreamcore_woven_wand", L / 100.0, ("FFFFFF", 0.14, 0.4, 1.0), ref=w.reference("784541197571848724.jpg", (100, 60, 640, 420), "dreamcore"), paint=paint,
           glow_spec={"crystal": ((255, 170, 225), 0.3, 0.1)})


# ================================================================ 2 Lich Wand
def build_lich_wand():
    """Card 736 x 736: the wand runs from the claw's fingertips (140, 400) to the bone knob (558, 235): 449 px; 1.0 m (wand class), K = 0.223 cm/px."""
    L = 100.0
    K = L / 449.0
    SHAFT = (27.0, 94.0)                                            # purple-grey bone shaft, card (290, 360) to (545, 240)
    R_SHAFT = 13 * K * 1.1                                          # card 26 px wide, scaled to a hand
    BEADS = (14.0, 19.5, 25.0)                                      # three dark skull beads, card (200..280, 380..350)
    R_BEAD = 2.3
    m = mk("lich_wand")
    pl = part(m, "claw_palm", "bone", base=(78, 70, 86), smooth=60)
    w.ellipsoid(pl, (0, 10.0, 0), 2.3, 3.4, 1.9, sides=10, stacks=8)
    fg = part(m, "claw_fingers", "bone", base=(92, 84, 104), uv_mode="loft", smooth=70)
    for k in range(5):
        a = math.radians(-70 + 35 * k)
        tube_into(fg, [(1.2 * math.sin(a), 11.0, 0.0), (2.6 * math.sin(a), 6.5, 0.4 * math.cos(a)), (3.6 * math.sin(a), 2.0, 0.8 * math.cos(a)), (3.2 * math.sin(a), 0.0, 1.0 * math.cos(a))], 0.55, 0.85, 6)
    sk = part(m, "skull_beads", "bone", base=(46, 42, 52), smooth=70)
    for y in BEADS:
        w.ellipsoid(sk, (0, y, 0), R_BEAD, R_BEAD * 1.05, R_BEAD * 0.95, sides=12, stacks=8)
    rings(m, "bead_collars", "bone", [BEADS[0] + 2.4, BEADS[1] + 2.3, SHAFT[0] - 1.5], 1.9, 1.0, sides=10, base=(214, 208, 168), lip=0.25)
    gnarled(m, "shaft", "bone", SHAFT[0], SHAFT[1], R_SHAFT, R_SHAFT * 0.95, 0.35, 2.5, base=(150, 138, 178), sides=10, bulge=0.08)
    rings(m, "shaft_joints", "bone", [SHAFT[0] + 12 + 11 * i for i in range(6)], R_SHAFT * 1.12, 1.1, sides=10, base=(172, 160, 198), lip=0.3)
    kb = part(m, "knob", "bone", base=(176, 164, 202), smooth=70)
    for sx in (-1, 1):
        w.ellipsoid(kb, (sx * 1.7, SHAFT[1] + 3.0, 0), 1.9, 2.4, 1.8, sides=10, stacks=8)
    w.ellipsoid(kb, (0, SHAFT[1] + 0.5, 0), 2.0, 1.8, 1.8, sides=10, stacks=6)
    fl = part(m, "flame", "crystal", base=(120, 235, 150), glow=True, uv_mode="loft", smooth=70)
    for k in range(7):
        y0 = SHAFT[0] + 8 + 9 * k
        a = k * 2.3
        tube_into(fl, [(2.2 * math.cos(a), y0, 2.2 * math.sin(a)), (3.2 * math.cos(a), y0 + 3.5, 3.2 * math.sin(a)), (3.8 * math.cos(a + 0.4), y0 + 8.0, 3.8 * math.sin(a + 0.4)),
                       (3.2 * math.cos(a + 0.8), y0 + 12.0, 3.2 * math.sin(a + 0.8))], 0.9, 0.95, 6)
    fw = part(m, "flame_wisps", "crystal", base=(160, 255, 185), glow=True, smooth=70)
    for k in range(6):
        a = k * 1.1
        w.ellipsoid(fw, (4.6 * math.cos(a), 30.0 + 12 * k, 4.6 * math.sin(a)), 0.8, 1.6, 0.8, sides=8, stacks=6)

    def paint(m_, atlas):
        w.paint_part(atlas, m_.part("skull_beads"), lambda c: w.tint(c.img, (60, 56, 66), 0.4))
    finish(m, "lich_wand", L / 100.0, ("8CFF9A", 0.16, 0.4, 1.0), ref=w.reference("784541197571571362.jpg", (120, 140, 600, 440), "lich_wand"), paint=paint,
           glow_spec={"flame": ((130, 255, 160), 0.7, 0.35), "flame_wisps": ((170, 255, 190), 0.9, 0.4)})


# ================================================================ 3 Scepter of the Double Moon
def build_scepter_of_the_double_moon():
    """Card 736 x 951 (shared with the Superlunary Staff): the scepter is the staff on the left; its head runs from the crescents' tops (150, 40) to the orb's
    foot (160, 205): 165 px; the orb is 85 px across and a scepter is 2.3 m (staff class), the orb 13 cm: K = 0.153 cm/px; the shaft below is cropped by the card."""
    L = 230.0
    K = 0.153
    ORB_R = 42 * K * 2.0                                            # orb radius 42 px, 13 cm across
    ORB_Y = L - 21.0                                                # orb centre
    NECK = (ORB_Y - ORB_R - 8.0, ORB_Y - ORB_R + 1.0)               # the gold stem into the orb
    GRIP = (7.0, 78.0)                                              # the woven lattice grip (card x 70..110, y 250..520)
    SHAFT_R = 1.9
    m = mk("scepter_of_the_double_moon")
    lath(m, "foot", "bronze", [(0, 0.0), (0.8, 1.5), (3.0, 2.0), (7.0, 1.7)], sides=10, base=(170, 120, 62), smooth=45)
    loft_y(m, "grip_lattice", "bronze", [(GRIP[0], SHAFT_R * 0.95), (30, SHAFT_R), (55, SHAFT_R * 1.05), (GRIP[1], SHAFT_R)], sides=12, base=(138, 96, 60))
    lw = part(m, "lattice_wire", "bronze", base=(210, 168, 90), uv_mode="loft", smooth=60)
    for ph in (0.0, math.pi):
        ml.sweep(lw, helix_pts(lambda t: SHAFT_R * 1.04, GRIP[0] + 2, GRIP[1] - 2, 9.0, 60, phase=ph), ml.circle(1.0, 4), scales=lambda t: (0.16, 0.16))
    rings(m, "grip_collar", "gold", [GRIP[1], GRIP[1] + 2.2], SHAFT_R * 1.55, 1.6, sides=12, base=(236, 196, 96), lip=0.4)
    loft_y(m, "shaft", "bronze", [(GRIP[1] + 4, SHAFT_R * 0.9), (130, SHAFT_R * 0.85), (NECK[0], SHAFT_R * 0.8)], sides=12, base=(150, 106, 64))
    rb = part(m, "shaft_rubies", "crystal", base=(210, 40, 60), glow=True, smooth=0)
    for i in range(9):
        y = GRIP[1] + 12 + (NECK[0] - GRIP[1] - 18) * i / 8
        a = i * 2.1
        ml.stud(rb, (SHAFT_R * 0.84 * math.cos(a), y, SHAFT_R * 0.84 * math.sin(a)), 0.45, 0.45, (math.cos(a), 0.0, math.sin(a)), 6, 0.4)
    lath(m, "neck", "gold", [(NECK[0], SHAFT_R * 0.9), (NECK[0] + 2.0, SHAFT_R * 1.6), (NECK[0] + 4.5, SHAFT_R * 1.15), (NECK[1] + 1.0, ORB_R * 0.55)], sides=12, base=(232, 190, 92), smooth=45)
    orb(m, "orb", "stone", (0, ORB_Y, 0), ORB_R, base=(122, 138, 168), sides=18, stacks=12)
    lath(m, "orb_band", "gold", [(ORB_Y - 2.4, ORB_R * 0.94), (ORB_Y - 1.8, ORB_R * 1.03), (ORB_Y + 1.8, ORB_R * 1.03), (ORB_Y + 2.4, ORB_R * 0.94)], sides=18, base=(232, 190, 92), smooth=40, caps=(False, False))
    ob = part(m, "orb_rubies", "crystal", base=(206, 36, 58), glow=True, smooth=0)
    for i in range(12):
        a = 2 * math.pi * i / 12
        ml.stud(ob, (ORB_R * 1.0 * math.cos(a), ORB_Y, ORB_R * 1.0 * math.sin(a)), 0.55, 0.5, (math.cos(a), 0.0, math.sin(a)), 6, 0.35)
    stp = part(m, "orb_strap", "gold", base=(226, 184, 88), smooth=45)
    for sgn in (1, -1):
        pts = [(0.0, ORB_Y + ORB_R * math.sin(math.radians(a)), sgn * ORB_R * math.cos(math.radians(a)) * 1.02) for a in range(-60, 61, 12)]
        ml.sweep(stp, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.5, 0.25), caps=(True, True))
    for sx, nm, cen in ((-1, "crescent_l", 200.0), (1, "crescent_r", -20.0)):
        cp = part(m, nm, "bronze", base=(210, 170, 78), smooth=40, uv_mode="loft")
        crescent_into(cp, sx * 6.5, ORB_Y + ORB_R + 8.0, 9.5, 3.2, 120.0, cen if sx < 0 else 160.0, 1.1)

    def paint(m_, atlas):
        def marble(c):
            n = w.noise2(c.img.shape[:2], 10, 31)
            c.img[..., :3] = c.img[..., :3] * (0.85 + 0.3 * n[..., None]) * np.array([0.92, 1.0, 1.12], np.float32)
        w.paint_part(atlas, m_.part("orb"), marble)
    finish(m, "scepter_of_the_double_moon", L / 100.0, ("D8E4FF", 0.12, 0.5, 1.0), ref=w.reference("5E HOMEBREW AND RESOURCES.jpg", (0, 20, 330, 700), "scepter"), paint=paint,
           glow_spec={"orb_rubies": ((255, 80, 90), 0.8, 0.3), "shaft_rubies": ((255, 80, 90), 0.7, 0.3)})


# ================================================================ 4 Serpant Head Staff
def build_serpant_head_staff():
    """Card 736 x 952: the staff runs from the foot loop's bottom (580, 830) to the crown's crystals (590, 55): 775 px; a quarterstaff is 2.3 m, K = 0.297 cm/px."""
    L = 230.0
    K = L / 775.0
    THIN = 0.55                                                     # the card draws the coils fat: scaled to a hand-held staff
    LOOP_Y, LOOP_R = 15.0, 6.5                                      # the foot loop: two serpents biting their tails (card (580, 790), radius 25 px)
    SHAFT = (28.0, 148.0)                                           # the scaled shaft between the foot and the twined coils
    COIL = (148.0, 196.0)                                           # the two serpents twining round each other (card y 200..400)
    CROWN_Y = 200.0                                                 # where the five necks leave the coil
    R_SHAFT = 14 * K * THIN * 2.1
    m = mk("serpant_head_staff")
    lp = part(m, "foot_loop", "silver", base=(150, 156, 168), smooth=45)
    ml.sweep(lp, ring_path(0, LOOP_Y, 0, LOOP_R, 24), ml.circle(1.0, 7), scales=lambda t: (1.6 * (1 - 0.25 * math.sin(math.pi * t)),) * 2, caps=(False, False))
    fh = part(m, "foot_heads", "silver", base=(166, 172, 184), smooth=70)
    for sx in (-1, 1):
        w.ellipsoid(fh, (sx * 2.3, LOOP_Y + LOOP_R + 1.4, 0), 1.6, 2.2, 1.4, sides=10, stacks=8)
    cy = part(m, "foot_gems", "crystal", base=(110, 230, 255), glow=True, smooth=0)
    for sx in (-1, 1):
        ml.gem(cy, (sx * 2.4, LOOP_Y + LOOP_R + 5.0, 0), 0.9, 1.3, 6)
    loft_y(m, "shaft", "silver", [(LOOP_Y + LOOP_R - 1, R_SHAFT * 1.2), (60, R_SHAFT), (110, R_SHAFT * 0.95), (SHAFT[1], R_SHAFT * 1.1)], sides=8, base=(138, 144, 156))
    rings(m, "shaft_scales", "silver", [SHAFT[0] + 4.0 + 8.0 * i for i in range(14)], R_SHAFT * 1.08, 1.2, sides=8, base=(176, 182, 194), lip=0.3)
    ca = part(m, "coil_a", "silver", base=(158, 164, 176), uv_mode="loft", smooth=60)
    cb = part(m, "coil_b", "silver", base=(132, 138, 152), uv_mode="loft", smooth=60)
    for p_, ph in ((ca, 0.0), (cb, math.pi)):
        ml.sweep(p_, helix_pts(lambda t: 3.6 * THIN + 1.4, COIL[0], COIL[1], 2.3, 44, phase=ph), ml.circle(1.0, 7), scales=lambda t: (2.5, 2.5), caps=(True, True))
    gm = part(m, "throat_gem", "crystal", base=(120, 235, 255), glow=True, smooth=70)
    w.ellipsoid(gm, (0.0, COIL[0] + 18, 3.6), 1.9, 2.2, 1.9, sides=10, stacks=8)
    nk = part(m, "crown_necks", "silver", base=(150, 156, 170), uv_mode="loft", smooth=70)
    hd = part(m, "snake_heads", "silver", base=(170, 176, 190), smooth=70)
    cs = part(m, "crown_crystals", "crystal", base=(110, 225, 255), glow=True, smooth=0)
    for k in range(5):
        a = math.radians(-90 + 45 * k) if k < 5 else 0
        sx = math.sin(a) * 1.0
        spread = [(-13, 214), (-7, 224), (0, 228), (7, 224), (13, 214)][k]
        pts = [(0, CROWN_Y - 4, 0), (sx * 1.5, CROWN_Y + 3, 0), (spread[0] * 0.55, CROWN_Y + (spread[1] - CROWN_Y) * 0.45, 0.0), (spread[0] * 0.9, spread[1] - 4, 0.0)]
        tube_into(nk, pts, 1.7, 0.35, 6, point=False)
        hx, hy = spread[0], spread[1]
        w.ellipsoid(hd, (hx, hy - 1.8, 0.0), 2.4, 3.4, 2.0, sides=10, stacks=8)
        ml.gem(cs, (hx * 1.05, hy + 4.6, 0), 1.9, 1.8, 6)
    sp = part(m, "collar_spikes", "silver", base=(176, 182, 196), smooth=30)
    for sgn in (-1, 1):
        ml.bar(sp, (0, COIL[0] + 16, 0), (sgn * 6.5, COIL[0] + 16, 0), 0.6, 0.45, up=(0, 0, 1), taper=1.5)

    def paint(m_, atlas):
        def scale(c):
            c.img[..., :3] *= (0.92 + 0.12 * np.sin(c.S * 60) * np.sin(c.T * 90))[..., None]
        w.paint_part(atlas, m_.part("shaft"), scale)
    finish(m, "serpant_head_staff", L / 100.0, ("5FE8FF", 0.14, 0.5, 1.0), ref=w.reference("Jynx's Fabulous Phantasmagoria.jpg", (480, 30, 700, 880), "serpant"), paint=paint,
           glow_spec={"foot_gems": ((110, 230, 255), 0.9, 0.4), "throat_gem": ((130, 240, 255), 0.95, 0.4), "crown_crystals": ((110, 225, 255), 0.9, 0.45)})


# ================================================================ 5 Staff of Blood Magic
def build_staff_of_blood_magic():
    """Card 736 x 1509: the staff runs from the foot (105, 670) to the head's cup (440, 285): 517 px; the head and shards add ~35 cm; a staff is 2.2 m here, K = 0.34 cm/px."""
    L = 222.0
    K = 195.0 / 517.0
    R = 12 * K * 0.65                                               # bone shaft radius (card 24 px wide at the lower half), scaled to a hand
    JOINT = 82.0                                                    # the break in the shaft with its collar (card (270, 460))
    TOP = 190.0                                                     # where the cup begins
    m = mk("staff_of_blood_magic")
    lath(m, "foot", "bone", [(0, 0.0), (2.2, 0.7), (8, R * 0.95)], sides=8, base=(150, 148, 156), smooth=45)
    gnarled(m, "shaft_low", "bone", 6.0, JOINT, R * 0.95, R * 1.25, 1.2, 1.4, base=(144, 142, 150), sides=9, phase=0.6, bulge=0.2)
    rings(m, "joint", "bone", [JOINT - 0.4, JOINT + 2.4], R * 1.9, 1.8, sides=10, base=(110, 108, 118), lip=0.5)
    gnarled(m, "shaft_high", "bone", JOINT + 4.0, TOP, R * 1.45, R * 1.05, 1.4, 1.2, base=(156, 154, 162), sides=9, phase=2.0, bulge=0.18)
    kn = part(m, "bone_knobs", "bone", base=(168, 166, 174), smooth=70)
    for yy, side in ((30.0, 1), (58.0, -1), (104.0, 1), (132.0, -1), (160.0, 1)):
        w.ellipsoid(kn, (side * R * 1.25, yy, R * 0.4), 1.3, 1.8, 1.3, sides=8, stacks=6)
    lath(m, "cup", "bone", [(TOP - 1.0, R * 0.9), (TOP + 2.5, R * 2.2), (TOP + 6.0, R * 2.9), (TOP + 7.5, R * 2.3)], sides=10, base=(120, 118, 128), smooth=45)
    ORB_Y, ORB_R = TOP + 15.0, 6.5
    orb(m, "orb", "crystal", (0, ORB_Y, 0), ORB_R, base=(240, 40, 50), sides=14, stacks=10, glow=True)
    band_ring(m, "orb_ring", "crystal", ring_path(0, ORB_Y, 0, ORB_R * 1.35, 28, rot=(("x", 80),)), 0.45, base=(255, 90, 90), glow=True)
    sh = part(m, "shards", "bone", base=(150, 148, 158), smooth=40, uv_mode="loft")
    for i, (r_, c0, half, tilt, cen) in enumerate(((15.0, 0, 38, 20, 40), (18.0, 0, 30, -55, 140), (14.0, 0, 26, 70, 250), (18.5, 0, 22, 10, 200), (16.0, 0, 20, -30, 320), (20.0, 0, 18, 45, 90), (13.0, 0, 22, -80, 10))):
        pts = []
        for j in range(17):
            u = -half + 2 * half * j / 16
            a = math.radians(cen + u)
            p0 = np.array([r_ * math.cos(a), r_ * math.sin(a), 0.0])
            ct, st = math.cos(math.radians(tilt)), math.sin(math.radians(tilt))
            p1 = np.array([p0[0], p0[1] * ct - p0[2] * st, p0[1] * st + p0[2] * ct])
            pts.append((p1[0], ORB_Y + p1[1], p1[2]))
        ml.sweep(sh, pts, ml.bevelled(1.0, 1.0, 0.5), scales=lambda t: (2.0 * math.sin(math.pi * t) + 0.05, 0.7 * math.sin(math.pi * t) + 0.05))

    def paint(m_, atlas):
        w.paint_part(atlas, m_.part("orb"), lambda c: w.tint(c.img, (255, 80, 80), 0.3))
    finish(m, "staff_of_blood_magic", L / 100.0, ("FF4040", 0.14, 0.45, 1.0), ref=w.reference("784541197571846815.jpg", (60, 20, 680, 720), "blood_magic"), paint=paint,
           glow_spec={"orb": ((255, 70, 70), 0.9, 0.4), "orb_ring": ((255, 110, 100), 0.7, 0.3)})


# ================================================================ 6 Staff of Ember Visions
def build_staff_of_ember_visions():
    """Card 736 x 736: the staff runs from the thorn tail (570, 430) to the bud (185, 100): 507 px; a staff is 2.3 m, K = 0.45 cm/px."""
    L = 230.0
    K = L / 507.0
    R = 9 * K * 0.5                                                 # a slim black shaft (card 9 px), scaled to a hand
    TOP = 196.0                                                     # the clasp begins (card (245, 150))
    m = mk("staff_of_ember_visions")
    tl = part(m, "tail_spike", "obsidian", base=(40, 36, 48), uv_mode="loft", smooth=60)
    tube_into(tl, [(0.0, 0.0, 0.0), (-1.8, 3.0, 0.4), (0.6, 6.0, -0.4), (0.4, 11.0, 0.2), (0.0, 16.0, 0.0)][::-1], R * 1.5, 0.7, 6, point=False)
    gnarled(m, "shaft", "obsidian", 14.0, TOP, R * 1.3, R * 1.15, 0.7, 4.5, base=(46, 40, 54), sides=8, bulge=0.1, phase=1.0)
    tube(m, "twist", "obsidian", helix_pts(lambda t: R * 1.45, 40.0, TOP - 4, 9.0, 120), 0.3, taper=0.0, sides=5, base=(78, 70, 90), point=False)
    cl = part(m, "clasp", "iron", base=(62, 58, 70), uv_mode="loft", smooth=60)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.3
        tube_into(cl, [(0.0, TOP - 2, 0.0), (3.4 * math.cos(a), TOP + 3, 3.4 * math.sin(a)), (3.8 * math.cos(a), TOP + 9, 3.8 * math.sin(a)), (2.6 * math.cos(a), TOP + 14, 2.6 * math.sin(a))], 0.7, 0.5, 6, point=False)
    BUD_Y = TOP + 9.5
    orb(m, "bud", "bronze", (0.0, BUD_Y, 0.0), 4.4, base=(246, 150, 60), sides=14, stacks=10, ry=7.2, glow=True)
    pt = part(m, "bud_petals", "bronze", base=(226, 110, 40), uv_mode="loft", smooth=60, glow=True)
    for k in range(5):
        a = 2 * math.pi * k / 5
        tube_into(pt, [(2.0 * math.cos(a), BUD_Y - 5.0, 2.0 * math.sin(a)), (4.6 * math.cos(a), BUD_Y - 1.0, 4.6 * math.sin(a)), (4.2 * math.cos(a), BUD_Y + 4.0, 4.2 * math.sin(a)),
                   (1.0 * math.cos(a), BUD_Y + 8.4, 1.0 * math.sin(a))], 1.0, 0.7, 6)
    rb = part(m, "ribbon", "cloth", base=(176, 28, 52), uv_mode="loft", smooth=50)
    rpts = []
    for i in range(41):
        t = i / 40
        rpts.append((2.5 + 62.0 * t, BUD_Y - 2.0 + 14.0 * math.sin(t * 3.4) - 22.0 * t * t, 5.0 * math.sin(t * 7.0)))
    ml.sweep(rb, rpts, ml.rect_ring(1.0, 1.0), scales=lambda t: (2.6 * (1.0 - 0.7 * t), 0.18))
    bd = part(m, "bird", "bone", base=(250, 236, 212), glow=True, smooth=70)
    w.ellipsoid(bd, (14.0, 150.0, 0.0), 1.8, 3.4, 1.6, sides=10, stacks=8)
    ml.lathe(bd, "x", (16.0, 153.0, 0.0), [(0, 0.0), (2.0, 0.35), (7.0, 0.0)][::1], sides=6)
    bw = part(m, "bird_wings", "bone", base=(255, 244, 226), glow=True, smooth=50, uv_mode="loft")
    for sz in (-1, 1):
        ml.sweep(bw, [(14.0, 151.0, sz * 0.8), (11.0, 156.0, sz * 4.0), (8.5, 161.0, sz * 6.5)], ml.rect_ring(1.0, 1.0), scales=lambda t: (2.4 * (1 - 0.8 * t), 0.15))
    bt = part(m, "bird_trail", "bone", base=(246, 226, 204), glow=True, uv_mode="loft", smooth=60)
    tpts = []
    for i in range(36):
        a = i / 35 * 2 * math.pi * 1.3
        tpts.append((14.0 - 9.0 * (1 - math.cos(a)) * 0.9, 150.0 - 8.0 * i / 35 * 2.2 + 2.0 * math.sin(a * 2), 3.0 * math.sin(a)))
    tube_into(bt, tpts, 0.32, 0.8, 5)
    ep = part(m, "ember_specks", "crystal", base=(255, 190, 90), glow=True, smooth=70)
    for k in range(8):
        a = k * 0.9
        w.ellipsoid(ep, (6.0 * math.cos(a), BUD_Y + 8.0 * math.sin(a * 0.7) , 6.0 * math.sin(a)), 0.45, 0.45, 0.45, sides=6, stacks=4)
    finish(m, "staff_of_ember_visions", L / 100.0, ("FF9A3C", 0.16, 0.45, 1.0), ref=w.reference("784541197571851289.jpg", (120, 40, 640, 450), "ember_visions"),
           glow_spec={"bud": ((255, 160, 60), 0.9, 0.35), "bud_petals": ((255, 120, 40), 0.6, 0.3), "bird": ((255, 236, 200), 0.5, 0.3), "bird_wings": ((255, 244, 220), 0.55, 0.3),
                      "bird_trail": ((255, 226, 190), 0.4, 0.3), "ember_specks": ((255, 200, 100), 1.0, 0.5)})


# ================================================================ 7 Staff of the Apprentice
def build_staff_of_the_apprentice():
    """Card 640 x 604: the staff runs from the foot (548, 222) to the crystals (125, 100): 440 px; a staff is 2.3 m, K = 0.52 cm/px."""
    L = 230.0
    K = L / 440.0
    R = 9 * K * 0.55                                                # a gnarled grey-brown shaft (card 9 px), scaled to a hand
    TOP = 192.0
    m = mk("staff_of_the_apprentice")
    lath(m, "foot", "wood", [(0, 0.0), (2.0, 0.8), (9, R * 1.1)], sides=8, base=(100, 86, 74), smooth=45)
    gnarled(m, "shaft", "wood", 8.0, TOP, R * 1.15, R * 1.5, 1.4, 4.0, base=(122, 106, 90), sides=9, bulge=0.22, phase=0.4)
    vp = part(m, "vines", "wood", base=(92, 78, 66), uv_mode="loft", smooth=60)
    for ph in (0.0, 2.1, 4.2):
        tube_into(vp, helix_pts(lambda t: R * 1.7, 20.0, 170.0, 3.0, 56, phase=ph), 0.5, 0.2, 5, point=False)
    cw = part(m, "cord_wrap", "cloth", base=(214, 204, 176), smooth=50)
    for i in range(10):
        ml.collar(cw, 92.0 + 2.5 * i, R * 1.95, 0.9, lip=0.25, sides=10)
    ch = part(m, "charms", "cloth", base=(206, 196, 168), uv_mode="loft", smooth=60)
    for k, (ang, ln) in enumerate(((0.3, 9.0), (2.4, 6.0), (4.4, 7.5))):
        a = ang
        tube_into(ch, [(R * 1.9 * math.cos(a), 94.0 - k, R * 1.9 * math.sin(a)), (R * 2.6 * math.cos(a), 94.0 - ln * 0.5, R * 2.6 * math.sin(a)), (R * 2.7 * math.cos(a), 94.0 - ln, R * 2.7 * math.sin(a))], 0.16, 0.2, 5, point=False)
    cb = part(m, "charm_beads", "bone", base=(236, 226, 196), smooth=70)
    for k, (ang, ln) in enumerate(((0.3, 9.0), (2.4, 6.0), (4.4, 7.5))):
        w.ellipsoid(cb, (R * 2.7 * math.cos(ang), 94.0 - ln - 0.8, R * 2.7 * math.sin(ang)), 0.6, 0.8, 0.6, sides=8, stacks=6)
    hc = part(m, "head_curls", "wood", base=(96, 82, 70), uv_mode="loft", smooth=60)
    for k in range(3):
        a = 2 * math.pi * k / 3
        pts = [(0.0, TOP - 3, 0.0), (3.0 * math.cos(a), TOP + 3, 3.0 * math.sin(a)), (4.6 * math.cos(a + 0.5), TOP + 9, 4.6 * math.sin(a + 0.5)), (3.4 * math.cos(a + 1.0), TOP + 14, 3.4 * math.sin(a + 1.0)),
               (1.4 * math.cos(a + 1.5), TOP + 15.5, 1.4 * math.sin(a + 1.5))]
        tube_into(hc, pts, 0.8, 0.7, 6)
    cr = part(m, "crystals", "crystal", base=(150, 150, 255), glow=True, smooth=0)
    for k, (ang, tilt, ln) in enumerate(((0.0, 0.0, 14.0), (1.4, 0.7, 11.0), (3.4, -0.7, 10.0), (5.0, 0.9, 9.0))):
        c0 = np.array([0.0, TOP + 7.0, 0.0])
        d = np.array([math.sin(tilt) * math.cos(ang), math.cos(tilt), math.sin(tilt) * math.sin(ang)])
        ml.gem(cr, tuple(c0 + d * ln * 0.6), 2.0 + 0.1 * k, ln / 2.6, 6, axis=tuple(d))
    sp = part(m, "sparkles", "crystal", base=(210, 240, 255), glow=True, smooth=0)
    for k in range(6):
        a = k * 1.05
        ml.gem(sp, (6.5 * math.cos(a), TOP + 4.0 + 5.0 * math.sin(a * 1.3), 6.5 * math.sin(a)), 0.5, 1.4, 4)

    def paint(m_, atlas):
        w.paint_part(atlas, m_.part("crystals"), lambda c: stripes(c, [(130, 170, 255), (190, 140, 255), (225, 235, 255)], 1.0))
    finish(m, "staff_of_the_apprentice", L / 100.0, ("A08CFF", 0.16, 0.45, 1.0), ref=w.reference("Here#U2019s to us, DND Nerds around the globe_ Items, potions and armor galore_ (4).jpg", (90, 60, 600, 250), "apprentice"),
           paint=paint, glow_spec={"crystals": ((170, 170, 255), 0.85, 0.4), "sparkles": ((220, 245, 255), 1.0, 0.5)})


# ================================================================ 8 Superlunary Staff
def build_superlunary_staff():
    """Card 736 x 951 (shared with the Scepter): the staff is the thin bronze one, from the foot (20, 940) to the armillary's top (245, 168): 797 px; a staff is 2.3 m, K = 0.289 cm/px."""
    L = 230.0
    K = L / 797.0
    R = 6 * K * 0.9                                                 # the thin bronze shaft (card 6 px wide)
    RING_Y = 214.0                                                  # the armillary's centre (card (258, 235))
    RING_R = 52 * K * 0.85                                          # the outer ring, card radius 52 px (scaled slightly)
    m = mk("superlunary_staff")
    lath(m, "foot_cap", "bronze", [(0, 0.0), (1.2, 1.0), (4.0, 1.6), (8.0, R * 1.1)], sides=10, base=(130, 84, 48), smooth=45)
    loft_y(m, "shaft", "wood", [(8.0, R * 1.05), (90, R), (170, R * 0.95), (188, R * 0.95)], sides=10, base=(116, 74, 46))
    lath(m, "bulbs", "bronze", [(172, R * 0.9), (174.5, 2.0), (177, 2.3), (179.5, 1.2), (181, 2.0), (183.5, 2.3), (186, R * 0.95)], sides=12, base=(164, 112, 60), smooth=50)
    rings(m, "shaft_bands", "gold", [60.0, 61.6, 120.0, 121.6], R * 1.6, 1.0, sides=10, base=(226, 184, 90), lip=0.3)
    lath(m, "neck", "bronze", [(186, R * 0.95), (190, 1.9), (196, 1.2), (200, 0.9)], sides=10, base=(172, 120, 62), smooth=45)
    ro = part(m, "ring_outer", "gold", base=(236, 196, 100), smooth=45)
    ml.sweep(ro, ring_path(0, RING_Y, 0, RING_R, 36, rot=(("x", 78),)), ml.circle(1.0, 8), scales=lambda t: (1.15, 0.6), caps=(False, False))
    for nm, rot, rr in (("ring_a", (("y", 0),), 0.82), ("ring_b", (("y", 60),), 0.82), ("ring_c", (("y", -60),), 0.82)):
        band_ring(m, nm, "gold", ring_path(0, RING_Y, 0, RING_R * rr, 32, rot=rot), 0.65, base=(228, 186, 90))
    sn = orb(m, "sun", "crystal", (0, RING_Y, 0), 4.3, base=(255, 150, 60), sides=14, stacks=10, glow=True)
    sf = part(m, "sun_flare", "crystal", base=(255, 210, 110), glow=True, smooth=30)
    for k in range(10):
        a = 2 * math.pi * k / 10
        ml.bar(sf, (4.0 * math.cos(a), RING_Y + 4.0 * math.sin(a), 0), (6.8 * math.cos(a), RING_Y + 6.8 * math.sin(a), 0), 0.5, 0.35, up=(0, 0, 1), taper=0.8)
    sk = part(m, "spokes", "bronze", base=(170, 118, 62), smooth=30)
    for sgn in (-1, 1):
        ml.bar(sk, (0, 200.0, 0), (sgn * RING_R * 0.9, RING_Y - RING_R * 0.2, 0), 0.7, 0.5, up=(0, 0, 1), taper=1.2)
    finish(m, "superlunary_staff", L / 100.0, ("FFB050", 0.16, 0.45, 1.0), ref=w.reference("5E HOMEBREW AND RESOURCES.jpg", (150, 140, 440, 951), "superlunary"),
           glow_spec={"sun": ((255, 160, 70), 0.95, 0.4), "sun_flare": ((255, 220, 120), 0.8, 0.4)})


# ================================================================ 9 The Bifrost
def build_the_bifrost():
    """Card 539 x 397: the staff runs from the foot (350, 372) to the finial (478, 42): 354 px; a quarterstaff is 2.0 m, K = 0.565 cm/px."""
    L = 200.0
    K = L / 354.0
    R = 5 * K * 0.8
    TOP = 178.0
    m = mk("the_bifrost")
    lath(m, "foot", "silver", [(0, 0.0), (1.6, 1.0), (4.0, 1.7), (10.0, R * 1.2)], sides=10, base=(216, 220, 232), smooth=45)
    loft_y(m, "core", "silver", [(8.0, R), (60, R * 0.95), (120, R), (TOP - 6, R * 0.9)], sides=8, base=(232, 232, 236))
    sw = part(m, "strand_white", "bone", base=(246, 242, 230), uv_mode="loft", smooth=60)
    sg = part(m, "strand_gold", "gold", base=(240, 200, 100), uv_mode="loft", smooth=60)
    for p_, ph in ((sw, 0.0), (sg, math.pi)):
        ml.sweep(p_, helix_pts(lambda t: R * 1.45, 12.0, TOP - 8, 10.0, 84, phase=ph), ml.circle(1.0, 5), scales=lambda t: (0.75, 0.75), caps=(True, True))
    rings(m, "grip_bands", "gold", [36.0, 44.0, 52.0, 60.0, 68.0], R * 1.85, 1.3, sides=10, base=(238, 198, 96), lip=0.35)
    lath(m, "finial", "silver", [(TOP - 4, R * 0.9), (TOP, R * 2.2), (TOP + 3, R * 2.6), (TOP + 7, R * 2.2), (TOP + 11, R * 1.0), (TOP + 13.5, 0.0)], sides=10, base=(222, 226, 238), smooth=40)
    fs = part(m, "finial_spikes", "silver", base=(238, 240, 248), smooth=30)
    for k in range(8):
        a = 2 * math.pi * k / 8
        ml.stud(fs, (R * 2.5 * math.cos(a), TOP + 4.5, R * 2.5 * math.sin(a)), 0.5, 2.0, (math.cos(a), 0.55, math.sin(a)), 5, 0.9)
    rings(m, "finial_cap", "gold", [TOP - 3.0], R * 2.0, 1.4, sides=10, base=(238, 198, 96), lip=0.4)
    cols = [("rb_red", (255, 90, 90)), ("rb_orange", (255, 170, 70)), ("rb_yellow", (255, 240, 100)), ("rb_green", (110, 230, 120)), ("rb_blue", (100, 190, 255)), ("rb_violet", (180, 120, 255))]
    for i, (nm, col) in enumerate(cols):
        p_ = part(m, nm, "crystal", base=col, glow=True, uv_mode="loft", smooth=60)
        pts = []
        for j in range(25):
            t = j / 24
            y = 14.0 + (TOP - 8.0 - 14.0) * t
            r = 3.2 + 3.5 * math.sin(t * math.pi * 2.0 + i * 0.6) ** 2 + 1.5 * math.sin(t * 9 + i)
            a = 2 * math.pi * 1.3 * t + i * math.pi / 3
            pts.append((r * math.cos(a), y, r * math.sin(a)))
        ml.sweep(p_, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: ((0.35 + 1.1 * math.sin(math.pi * t) ** 0.7), 0.18))

    def paint(m_, atlas):
        for nm, col in cols:
            w.paint_part(atlas, m_.part(nm), lambda c, col=col: (c.img.__setitem__((Ellipsis, slice(0, 3)), np.array(col, np.float32)), w.set_glow(c, col, 0.8, core=0.4)))
    finish(m, "the_bifrost", L / 100.0, ("FFC8F0", 0.18, 0.45, 1.0), ref=w.reference("784541197571316108.jpg", (330, 20, 520, 390), "bifrost"), paint=paint)


# ================================================================ 10 Wand-o-Wisp
def build_wand_o_wisp():
    """Card 540 x 1476: the wand runs from the hexagonal pommel (150, 480) to the needle's tip (408, 38): 512 px; a wand is 1.0 m, K = 0.195 cm/px."""
    L = 100.0
    K = L / 512.0
    GRIP = (5.0, 43.0)                                              # the teal hexagonal crystal grip, card (160, 460) to (245, 300): 40 cm
    R_GRIP = 28 * K * 0.85                                          # card hex half width 28 px, scaled
    COLLAR = (GRIP[1], GRIP[1] + 5.0)
    m = mk("wand_o_wisp")
    lath(m, "pommel", "silver", [(0, R_GRIP * 0.8), (0.7, R_GRIP * 1.45), (4.2, R_GRIP * 1.45), (5.2, R_GRIP * 1.1)], sides=6, base=(204, 210, 222), smooth=0, caps=(False, True))
    rings(m, "pommel_ring", "silver", [1.2], R_GRIP * 1.5, 1.6, sides=6, base=(224, 228, 238), lip=0.35)
    gp = part(m, "grip", "crystal", base=(110, 218, 212), glow=True, smooth=0, uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], R_GRIP, R_GRIP), (24, R_GRIP * 1.06, R_GRIP * 1.06), (GRIP[1], R_GRIP * 0.96, R_GRIP * 0.96)], ml.circle(1.0, 6))
    ge = part(m, "grip_edges", "silver", base=(206, 214, 226), smooth=30)
    for k in range(6):
        a = math.radians(60 * k)
        ml.bar(ge, (R_GRIP * 1.03 * math.cos(a), GRIP[0] + 0.5, R_GRIP * 1.03 * math.sin(a)), (R_GRIP * 0.97 * math.cos(a), GRIP[1] - 0.5, R_GRIP * 0.97 * math.sin(a)), 0.35, 0.28, up=(math.cos(a), 0, math.sin(a)), taper=1.0)
    lath(m, "collar", "silver", [(COLLAR[0] - 0.4, R_GRIP * 0.98), (COLLAR[0] + 0.8, R_GRIP * 1.22), (COLLAR[0] + 3.2, R_GRIP * 0.82), (COLLAR[1], R_GRIP * 0.55)], sides=6, base=(214, 220, 230), smooth=0)
    nd = part(m, "needle", "steel", base=(204, 210, 220), smooth=0, uv_mode="loft")
    ml.loft(nd, "y", (0, 0, 0), [(COLLAR[1] - 0.2, R_GRIP * 0.55, R_GRIP * 0.55), (70.0, R_GRIP * 0.42, R_GRIP * 0.42), (88.0, R_GRIP * 0.2, R_GRIP * 0.2), (L, 0.0, 0.0)], ml.circle(1.0, 6))
    nr = part(m, "needle_ridges", "silver", base=(240, 244, 250), smooth=30)
    for k in range(3):
        a = math.radians(120 * k + 30)
        ml.bar(nr, (R_GRIP * 0.52 * math.cos(a), COLLAR[1] + 1.0, R_GRIP * 0.52 * math.sin(a)), (R_GRIP * 0.2 * math.cos(a), 84.0, R_GRIP * 0.2 * math.sin(a)), 0.3, 0.2, up=(math.cos(a), 0, math.sin(a)), taper=2.0)
    for nm, (cx, cy, cz, sgn) in (("wisp_a", (-6.5, 56.0, 1.0, 1)), ("wisp_b", (5.5, 22.0, -1.0, -1))):
        wb = part(m, nm, "crystal", base=(190, 238, 255), glow=True, smooth=70)
        w.ellipsoid(wb, (cx, cy, cz), 0.9, 1.5, 0.9, sides=8, stacks=6)
        ww = part(m, nm + "_wings", "crystal", base=(210, 245, 255), glow=True, smooth=40, uv_mode="loft")
        for s2 in (-1, 1):
            ml.sweep(ww, [(cx, cy + 0.5, cz), (cx + s2 * 2.2, cy + 2.6, cz + 0.3), (cx + s2 * 4.0, cy + 4.6, cz + 0.5)], ml.rect_ring(1.0, 1.0), scales=lambda t: (1.9 * math.sin(math.pi * (0.25 + 0.7 * t)), 0.1))

    def paint(m_, atlas):
        w.paint_part(atlas, m_.part("grip"), lambda c: stripes(c, [(120, 225, 218), (90, 200, 200), (160, 240, 232)], 1.0))
    finish(m, "wand_o_wisp", L / 100.0, ("A8F0F0", 0.16, 0.45, 1.0), ref=w.reference("Post by @monsters-and-more.jpg", (100, 20, 460, 520), "wand_o_wisp"), paint=paint,
           glow_spec={"grip": ((130, 240, 232), 0.5, 0.3), "wisp_a": ((200, 245, 255), 0.95, 0.5), "wisp_a_wings": ((210, 248, 255), 0.7, 0.4),
                      "wisp_b": ((200, 245, 255), 0.95, 0.5), "wisp_b_wings": ((210, 248, 255), 0.7, 0.4)})


# ================================================================ 11 Wand of Spider Summoning
def build_wand_of_spider_summoning():
    """Card 640 x 604: the wand runs from the pommel claw (145, 300) to the blade's tip (515, 135): 405 px; 1.05 m (wand class), K = 0.26 cm/px."""
    L = 105.0
    K = L / 405.0
    GRIP = (7.0, 40.0)                                              # the black segmented spider-leg grip, card (150, 300) to (250, 265)
    R = 9 * K * 0.9
    HUB_Y = 43.0                                                    # the guard's hub with the two pink gem eyes (card (262, 262))
    m = mk("wand_of_spider_summoning")
    cw = part(m, "claw", "crystal", base=(240, 80, 200), glow=True, uv_mode="loft", smooth=60)
    ml.sweep(cw, [(math.sin(math.radians(a)) * 5.0 + 1.5, 7.0 - 5.0 * (1 - math.cos(math.radians(a))), 0.0) for a in range(-20, 171, 15)], ml.circle(1.0, 6), scales=lambda t: (1.5 * (1 - 0.8 * t) + 0.2,) * 2)
    for nm, (y0, y1, rr) in (("grip_a", (GRIP[0], 22.0, R)), ("grip_b", (22.0, GRIP[1], R * 0.92))):
        loft_y(m, nm, "obsidian", [(y0, rr * 0.9), ((y0 + y1) / 2, rr * 1.08), (y1, rr * 0.9)], sides=9, base=(44, 38, 52))
    jn = part(m, "joints", "obsidian", base=(64, 56, 74), smooth=70)
    for yy in (GRIP[0] + 1.0, 22.0, GRIP[1] - 0.5):
        w.ellipsoid(jn, (0, yy, 0), R * 1.25, R * 1.1, R * 1.25, sides=10, stacks=8)
    sp = part(m, "spines", "obsidian", base=(70, 60, 84), smooth=30)
    for i in range(18):
        y = GRIP[0] + 3 + (GRIP[1] - GRIP[0] - 6) * i / 17
        a = i * 2.4
        ml.stud(sp, (R * 0.95 * math.cos(a), y, R * 0.95 * math.sin(a)), 0.28, 1.5, (math.cos(a), 0.4, math.sin(a)), 4, 0.9)
    orb(m, "guard_hub", "obsidian", (0, HUB_Y, 0), 2.4, base=(52, 44, 62), sides=12, stacks=8, ry=3.0)
    gl = part(m, "guard_legs", "obsidian", base=(40, 34, 48), uv_mode="loft", smooth=60)
    for sx in (-1, 1):
        for sz, ln in ((0.0, 1.0), (1.0, 0.7)):
            pts = [(sx * 1.8, HUB_Y - 0.5, sz * 1.2), (sx * 5.6, HUB_Y + 1.8, sz * 2.0), (sx * 8.0 * ln + sx * 0.5, HUB_Y + 7.0, sz * 2.4), (sx * 6.0 * ln, HUB_Y + 13.0, sz * 1.8)]
            tube_into(gl, pts, 0.75, 0.8, 6)
    ey = part(m, "eyes", "crystal", base=(255, 110, 220), glow=True, smooth=0)
    for sz in (-1, 1):
        ml.gem(ey, (0.3, HUB_Y + 1.2, sz * 1.9), 0.8, 1.3, 6, axis="y")
    bl = part(m, "blade", "crystal", base=(230, 70, 190), glow=True, smooth=30, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(HUB_Y + 1.5, 1.6, 0.6), (60.0, 1.35, 0.5), (85.0, 0.8, 0.3), (L, 0.0, 0.0)], ml.lozenge(2.0, 1.0))
    th = part(m, "blade_thorns", "crystal", base=(255, 150, 230), glow=True, smooth=30)
    for i in range(16):
        y = HUB_Y + 5.0 + (L - HUB_Y - 12.0) * i / 15
        side = 1 if i % 2 == 0 else -1
        wdt = 1.35 * (1 - (y - HUB_Y) / (L - HUB_Y)) + 0.2
        ml.stud(th, (side * wdt * 0.8, y, 0.0), 0.28, 1.3, (side, 0.5, 0), 4, 0.9)
    cr = part(m, "blade_core", "crystal", base=(255, 190, 240), glow=True, smooth=30)
    ml.bar(cr, (0, HUB_Y + 4.0, 0.34), (0, L - 12.0, 0.1), 0.35, 0.12, up=(0, 0, 1), taper=3.0)
    finish(m, "wand_of_spider_summoning", L / 100.0, ("FF4AD0", 0.16, 0.45, 1.0), ref=w.reference("784541197571317328.jpg", (110, 110, 560, 330), "spider"),
           glow_spec={"claw": ((255, 110, 220), 0.6, 0.3), "eyes": ((255, 130, 230), 1.0, 0.5), "blade": ((255, 80, 210), 0.7, 0.3), "blade_thorns": ((255, 150, 230), 0.8, 0.4),
                      "blade_core": ((255, 210, 245), 0.9, 0.5)})


# ================================================================ 12 Windwheel Staff
def build_windwheel_staff():
    """Card 736 x 736: the staff runs from the foot (183, 700) to the ring's top (430, 25): 719 px; a staff is 2.3 m, K = 0.32 cm/px."""
    L = 230.0
    K = L / 719.0
    R = 8 * K * 0.9                                                 # red-brown shaft, card 8 px wide
    RING_Y, RING_R = 216.0, 72 * K * 0.85                            # the blue-steel ring, card radius 36..42 px (scaled)
    m = mk("windwheel_staff")
    ft = part(m, "foot", "iron", base=(80, 90, 120), smooth=30, uv_mode="loft")
    ml.loft(ft, "y", (0, 0, 0), [(0.0, 0.6, 0.5), (3.0, 3.6, 1.2), (11.0, 2.4, 1.3), (14.0, R * 1.1, R * 1.1)], ml.octagon(1.0, 1.0, 0.25))
    loft_y(m, "shaft", "wood", [(14.0, R * 1.1), (60, R), (130, R * 0.95), (182, R)], sides=10, base=(130, 52, 40))
    rings(m, "bands", "gold", [14.0, 15.6, 180.0, 181.6], R * 1.55, 1.1, sides=10, base=(226, 184, 88), lip=0.3)
    lath(m, "neck", "gold", [(182, R * 1.05), (184.5, R * 1.7), (190, R * 1.35), (196, R * 1.15), (199, R * 1.5)], sides=10, base=(214, 172, 80), smooth=45)
    band_ring(m, "ring", "iron", ring_path(0, RING_Y, 0, RING_R, 36), 1.55, base=(88, 100, 136), sides=8)
    rb = part(m, "ring_bosses", "iron", base=(66, 76, 108), smooth=40)
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        ml.stud(rb, (RING_R * math.cos(a), RING_Y + RING_R * math.sin(a), 0.0), 1.3, 1.6, (math.cos(a), math.sin(a), 0.0), 8, 0.4)
    rg = part(m, "ring_gems", "crystal", base=(255, 214, 80), glow=True, smooth=0)
    for k in range(4):
        a = math.pi / 2 * k
        ml.gem(rg, (RING_R * 0.82 * math.cos(a), RING_Y + RING_R * 0.82 * math.sin(a), 0.0), 1.0, 1.3, 5, axis="y")
    pw = part(m, "pinwheel", "crystal", base=(255, 190, 70), glow=True, smooth=30, uv_mode="loft")
    for k in range(8):
        a = 2 * math.pi * k / 8
        ex, ey = math.cos(a), math.sin(a)
        px, py = -ey, ex
        pts = [(0.0, RING_Y, 0.0), (RING_R * 0.45 * ex + px * 2.0, RING_Y + RING_R * 0.45 * ey + py * 2.0, 0.0), (RING_R * 0.78 * ex + px * 5.5, RING_Y + RING_R * 0.78 * ey + py * 5.5, 0.0)]
        ml.sweep(pw, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (2.5 * (0.4 + 0.6 * t), 0.22))
    orb(m, "hub", "gold", (0, RING_Y, 0), 2.6, base=(240, 200, 96), sides=12, stacks=8, rz=1.8)
    hr = part(m, "hang_rings", "gold", base=(228, 186, 90), smooth=45)
    for sx in (-1, 1):
        for dz, rr in ((0.0, 4.6), (2.4, 4.2)):
            ml.sweep(hr, ring_path(sx * RING_R * 0.78, RING_Y - RING_R * 0.7, dz, rr, 20, rot=(("y", 70), ("z", 15 * sx))), ml.circle(0.5, 6), caps=(False, False))
    sk = part(m, "ring_spokes", "iron", base=(70, 80, 112), smooth=30)
    for sgn in (-1, 1):
        ml.bar(sk, (0, 199.0, 0), (sgn * RING_R * 0.7, RING_Y - RING_R * 0.72, 0), 1.0, 0.8, up=(0, 0, 1), taper=1.2)
    finish(m, "windwheel_staff", L / 100.0, ("FFC84A", 0.16, 0.45, 1.0), ref=w.reference("784541197571571364.jpg", (160, 0, 520, 736), "windwheel"),
           glow_spec={"ring_gems": ((255, 220, 90), 0.9, 0.4), "pinwheel": ((255, 190, 70), 0.55, 0.3)})


MODELS = ["dreamcore_woven_wand", "lich_wand", "scepter_of_the_double_moon", "serpant_head_staff", "staff_of_blood_magic", "staff_of_ember_visions",
          "staff_of_the_apprentice", "superlunary_staff", "the_bifrost", "wand_o_wisp", "wand_of_spider_summoning", "windwheel_staff"]


def main(only=None):
    for item_id in MODELS:
        if only and item_id not in only:
            continue
        globals()["build_" + item_id]()


if __name__ == "__main__":
    main(sys.argv[1:])
