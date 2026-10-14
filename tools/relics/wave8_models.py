"""Relics of Many Worlds, second card set, wave 8: the ten blades and axes as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave8_models.py [id ...]        (no ids = all ten; idempotent, deterministic)
    W1_PREVIEW=<dir> python3 -B tools/relics/wave8_models.py shadow_burn      (also renders the preview beside the card)

Run tools/relics/wave8_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon). Per id it writes
assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png and <id>_glow.png (512 px skins).

The method, in order (see COMMON_R.md): 0 target, 1 measure the reference (a known length -> centimetres; every proportion a NAMED CONSTANT with
a comment where it came from), 2 geometry from real cross-sections (loft / sweep / lathe), 3 the small raised parts each their own named Part,
4 normals smoothed by angle per part, UVs projected on the dominant axis into one skin, 5 a painted material plus an additive glow skin,
6 rendered beside the card and corrected. Each weapon is one function below; units are centimetres, y runs from the pommel (0) to the point,
x across, z through (+z toward the viewer), the edge or the head of an axe on +x.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                      # noqa: E402
import wave1_models as w                   # noqa: E402

ATLAS = 512                                # one skin of 512 px per weapon (the first wave used 1024)
CARDS = os.environ.get("RELICS2_DIR", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics2")
P2, P3 = "p2/Relics of the world part 2/", "p3/Relics of the world part 3/"


# ================================================================ shared helpers
def card(file, box, name):
    """Crops the card art for the preview (None when the images are not around or no preview is asked for)."""
    src = os.path.join(CARDS, file)
    prev = os.environ.get("W1_PREVIEW")
    if not prev or not os.path.exists(src):
        return None
    os.makedirs(prev, exist_ok=True)
    out = os.path.join(prev, name + "_ref.png")
    Image.open(src).convert("RGBA").crop(box).save(out)
    return out


def add(m, name, material, **kw):
    return w.add(m, name, material, **kw)


def cone(part, base, direction, length, r, sides=8, squash=1.0):
    """A tapering spike from `base` along `direction` (closes to a point)."""
    d = np.asarray(direction, float)
    d = d / np.linalg.norm(d)
    return ml.lathe(part, tuple(d), base, [(0.0, r), (length * 0.55, r * 0.55), (length, 0.0)], sides=sides, squash=squash)


def rod(part, p0, p1, r, sides=6):
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    return ml.sweep(part, [tuple(p0), tuple(p1)], ml.circle(r, sides), scales=lambda t: (1.0, 1.0))


def normal_xy(path, i):
    """The in-plane (xy) unit normal of a polyline at point i, pointing to +x for a path that runs up."""
    a, b = np.asarray(path[max(i - 1, 0)], float), np.asarray(path[min(i + 1, len(path) - 1)], float)
    t = b - a
    n = np.array([t[1], -t[0], 0.0])
    ln = np.linalg.norm(n)
    return n / ln if ln > 1e-9 else np.array([1.0, 0.0, 0.0])


def offset_xy(path, d, dz=0.0):
    return [tuple(np.asarray(p, float) + normal_xy(path, i) * d + np.array([0, 0, dz])) for i, p in enumerate(path)]


def curve(fx, y0, y1, n, z=0.0):
    return [(fx(y0 + (y1 - y0) * i / n), y0 + (y1 - y0) * i / n, z) for i in range(n + 1)]


def slab(part, outline, centre, thick, bevel=0.035, edge=0.18):
    """A flat plate from an (x, y) outline (about `centre`): a short bevel on both faces, extruded through z."""
    cx, cy = centre
    pts = [(x - cx, y - cy) for (x, y) in outline]
    h = thick / 2
    ml.loft(part, "z", (cx, cy, 0), [(-h, 1 - bevel, 1 - bevel), (-h + edge, 1, 1), (h - edge, 1, 1), (h, 1 - bevel, 1 - bevel)], pts)


def helix_tube(m, name, material, base, r_of_t, y0, y1, turns, n, tube, phase=0.0, glow=False, ax=0.0, az=0.0, scale=None):
    p = add(m, name, material, base=base, glow=glow, smooth=60)
    pts = w.helix(r_of_t, y0, y1, turns, n, phase=phase, ax=ax, az=az)
    ml.sweep(p, pts, ml.circle(tube, 5), scales=scale or (lambda t: (1.0, 1.0)))
    return p


def finish(item_id, m, atlas, real_m, ref=None, glow=("FFFFFF", 0.14, 0.55, 1.0), views=None, focus=None):
    return w.finish(item_id, m, atlas, real_m=real_m, glow=glow, ref=ref, views=views, focus=focus)


def new_atlas(m):
    a = ml.Atlas(ATLAS)
    a.layout(m)
    a.paint(m)
    return a


def glow_parts(atlas, m, names, rgb, strength=0.9, core=0.35):
    for n in names:
        w.paint_part(atlas, m.part(n), lambda c, rgb=rgb, s=strength, k=core: w.set_glow(c, rgb, s, core=k))


def recolour(atlas, m, names, rgb, k=0.6):
    for n in names:
        w.paint_part(atlas, m.part(n), lambda c, rgb=rgb, k=k: w.tint(c.img, rgb, k))


def wrap_cord(m, name, material, base, r, y0, y1, turns, tube=0.16, both=True):
    """A cord wound over a grip (one or two counter-wound strands)."""
    for nm, ph, d in ((name + "_a", 0.0, 1), (name + "_b", 1.5, -1)) if both else ((name, 0.0, 1),):
        helix_tube(m, nm, material, base, lambda t, r=r: r * 1.06, y0 + 0.3, y1 - 0.3, turns * d, 48, tube, phase=ph)


# ================================================================ 1 Spotlight
def spotlight():
    """Card: 736 x 736 "Spotlight" (+1 shortsword): a cartoon blade of warm light shining out of a black stage-lamp housing, a wooden grip, a clawed pommel."""
    TOTAL = 0.80                                # a shortsword with its light blade: about 80 cm
    L_BLADE0, L_BLADE1 = 35.0, 80.0             # the beam: card px 55..430 of 600 along the sword -> about 45 cm
    R_HOUSING, HOUSING = 3.6, (21.0, 33.0)      # the lamp drum, card 60 px tall of 600 -> 12 cm long, 7 cm across
    GRIP, R_GRIP = (4.0, 20.0), 1.45            # the wooden handle, card 110 px -> 16 cm
    m = ml.Mesh("spotlight")
    pc = add(m, "pommel_claws", "iron", base=(40, 40, 46), smooth=50)
    for k in range(3):                          # three prongs fanned at the end of the grip (card: a small claw)
        a = math.radians(-70 + 70 * k)
        cone(pc, (0, 4.4, 0), (math.sin(a) * 0.9, -1.0, 0.0), 4.4, 0.55, sides=6)
    pb = add(m, "pommel_ring", "iron", base=(52, 52, 60))
    ml.collar(pb, 3.4, 1.7, 1.2, lip=0.3, sides=10)
    gp = add(m, "grip", "wood", base=(132, 84, 46), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 0.9, 0.9), (GRIP[0] + 6, 1.0, 1.0), (GRIP[1], 0.95, 0.95)], ml.circle(R_GRIP, 10))
    gb = add(m, "grip_bands", "iron", base=(40, 40, 48))
    for s in (GRIP[0] + 2.0, 11.5, GRIP[1] - 3.0):
        ml.collar(gb, s, R_GRIP * 1.18, 1.0, lip=0.25, sides=10)
    dr = add(m, "housing", "iron", base=(34, 34, 40), smooth=50)
    ml.lathe(dr, "y", (0, 0, 0), [(HOUSING[0], 1.2), (HOUSING[0] + 0.6, R_HOUSING * 0.95), (27.0, R_HOUSING), (HOUSING[1] - 0.6, R_HOUSING * 0.92), (HOUSING[1], R_HOUSING * 0.7)], sides=14)
    hr = add(m, "housing_ribs", "iron", base=(70, 70, 80), wear=0.8)
    for s in (23.0, 25.5, 28.5, 31.0):
        ml.collar(hr, s, R_HOUSING * 1.04, 0.7, lip=0.2, sides=14)
    fl = add(m, "flange", "iron", base=(52, 52, 60))
    ml.collar(fl, HOUSING[1] - 0.2, R_HOUSING * 1.22, 1.3, lip=0.35, sides=14)
    for sgn, nm in ((-1, "l"), (1, "r")):       # the little bracket knobs the card shows on both sides of the drum
        kb = add(m, "knob_" + nm, "iron", base=(60, 60, 70), smooth=50)
        ml.lathe(kb, "x", (sgn * R_HOUSING * 0.9, 29.0, 0), [(0.0, 0.35), (sgn * 1.2, 0.35), (sgn * 1.9, 0.75), (sgn * 2.4, 0.5)], sides=8)
    ln = add(m, "lens", "crystal", base=(255, 240, 170), glow=True, smooth=0)
    ml.lathe(ln, "y", (0, 0, 0), [(HOUSING[1] + 0.4, 0.0), (HOUSING[1] + 0.6, R_HOUSING * 0.8), (HOUSING[1] + 1.4, R_HOUSING * 0.85), (HOUSING[1] + 1.5, 0.0)], sides=14, caps=(False, False))
    bl = add(m, "beam", "crystal", base=(255, 232, 150), glow=True, smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(L_BLADE0, 0.85, 0.7), (L_BLADE0 + 6, 1.0, 1.0), (62.0, 0.98, 0.95), (74.0, 0.75, 0.7), (78.5, 0.4, 0.4), (L_BLADE1, 0.0, 0.0)], ml.lozenge(5.6, 1.7, 0.3))
    co = add(m, "beam_core", "crystal", base=(255, 252, 226), glow=True, smooth=40, uv_mode="loft")
    ml.loft(co, "y", (0, 0, 0), [(L_BLADE0 + 1, 0.7, 0.7), (60.0, 0.7, 0.7), (72.0, 0.5, 0.5), (77.0, 0.0, 0.0)], ml.lozenge(2.6, 2.1, 0.0))
    sp = add(m, "sparkles", "crystal", base=(255, 250, 200), glow=True, smooth=0)
    for (x, y_, z) in ((-5.5, 50.0, 1.4), (6.0, 57.0, 1.0), (-4.5, 66.0, -1.2), (5.2, 44.0, -1.4)):   # the card's little gold stars
        ml.gem(sp, (x, y_, z), 0.45, long=1.9, sides=4, axis="y")
    atlas = new_atlas(m)
    glow_parts(atlas, m, ("lens", "sparkles"), (255, 236, 150), 1.0, 0.5)
    glow_parts(atlas, m, ("beam",), (255, 228, 130), 0.85, 0.35)
    glow_parts(atlas, m, ("beam_core",), (255, 255, 235), 1.0, 0.7)
    ref = card(P2 + "2b86d9388efcb168933cce6e838e12fe.jpg", (40, 250, 700, 430), "spotlight")
    finish("spotlight", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.20, 0.6, 1.0))


# ================================================================ 2 Duality Blade
def duality_blade():
    """Card: 640 x 640 "Duality Blade": a matte black shortsword with a twisted grip, a curved wing-like guard and a flat dark blade, spiralled by lilac streamers."""
    TOTAL = 0.78                                # the card sword is 520 px long; a shortsword about 75 cm
    GRIP, R_GRIP = (3.0, 17.0), 1.4             # the twisted black grip, card 120 px of 520
    GUARD_Y, GUARD_SPAN, GUARD_RISE = 18.5, 8.5, 5.0     # the wing: card guard 100 px tall, curling up toward the blade
    BLADE0, TIP = 19.5, 76.0                    # the blade: card 150..520 px
    BLADE_W, BLADE_T = 5.0, 0.7                 # flat and broad: 90 px wide of 520 -> wide for a dagger-like shortsword
    m = ml.Mesh("duality_blade")
    pb = add(m, "pommel", "obsidian", base=(26, 24, 32), smooth=50)
    ml.lathe(pb, "y", (0, 0, 0), [(0.0, 0.6), (0.6, 1.5), (1.8, 1.7), (3.2, 1.2)], sides=12)
    gp = add(m, "grip", "obsidian", base=(30, 26, 38), uv_mode="loft", smooth=50)
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 1.0, 1.0), (GRIP[0] + 5, 1.12, 1.12), (GRIP[1], 1.0, 1.0)], ml.circle(R_GRIP, 12))
    for nm, ph in (("grip_twist_a", 0.0), ("grip_twist_b", math.pi)):
        helix_tube(m, nm, "obsidian", (60, 52, 80), lambda t: R_GRIP * 1.08, GRIP[0] + 0.5, GRIP[1] - 0.5, 3.0, 54, 0.3, phase=ph)
    cl = add(m, "grip_collar", "obsidian", base=(44, 38, 56))
    ml.collar(cl, GRIP[1] - 0.2, 1.9, 1.2, lip=0.3, sides=12)
    bs = add(m, "guard_boss", "obsidian", base=(40, 34, 52), smooth=50)
    ml.lathe(bs, "y", (0, 0, 0), [(GUARD_Y - 1.2, 1.5), (GUARD_Y, 2.2), (GUARD_Y + 1.4, 2.0), (GUARD_Y + 2.2, 1.2)], sides=12, squash=0.7)
    for sgn, nm in ((-1, "l"), (1, "r")):       # each wing is a swept blade-shaped horn: out, then up and curling toward the tip
        wg = add(m, "guard_wing_" + nm, "obsidian", base=(34, 30, 44), smooth=55)
        pts = []
        for i in range(0, 11):
            t = i / 10
            pts.append((sgn * (1.5 + GUARD_SPAN * math.sin(t * math.pi * 0.5) * 0.95), GUARD_Y + 0.4 + GUARD_RISE * t ** 1.8 - 1.2 * (1 - t), 0.0))
        ml.sweep(wg, pts, ml.lozenge(1.0, 1.0, 0.0), scales=lambda t: (2.4 * (1 - 0.8 * t ** 1.5) if t < 0.97 else 0.0, 1.1 * (1 - 0.6 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
    bl = add(m, "blade", "obsidian", base=(28, 26, 36), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(BLADE0, 0.85, 1.0), (BLADE0 + 3, 1.0, 1.0), (50.0, 1.0, 1.0), (66.0, 0.9, 0.95), (TIP - 7, 0.6, 0.7), (TIP, 0.0, 0.0)], ml.bevelled(BLADE_W, BLADE_T, 0.25))
    for face, nm in ((1, "midrib_f"), (-1, "midrib_b")):
        f = add(m, nm, "obsidian", base=(80, 70, 104), wear=0.9)
        ml.fuller(f, (0, BLADE0 + 3, face * BLADE_T * 0.5), (0, TIP - 12, face * BLADE_T * 0.42), (0, 0, face), 0.9, 0.16, 0.08, taper=2.5)
    ec = add(m, "ricasso_plate", "obsidian", base=(46, 40, 60), smooth=50)
    ml.collar(ec, BLADE0 - 0.6, 2.7, 1.4, lip=0.3, sides=10, oval=0.35)
    # the streamers: two lilac ribbons spiralling round the blade, a thin third close to it
    for nm, ph, rr, tb in (("streamer_a", 0.0, 3.3, 0.5), ("streamer_b", math.pi * 0.8, 3.7, 0.44), ("streamer_c", math.pi * 1.5, 2.8, 0.3)):
        helix_tube(m, nm, "crystal", (226, 214, 255), lambda t, rr=rr: rr * (1 - 0.55 * t) * 1.0, BLADE0 + 2, TIP - 8, 2.1, 40, tb, phase=ph, glow=True,
                   scale=lambda t, tb=tb: (1.0 if t > 0.04 else 0.4, 1.0 if t < 0.96 else 0.3))
    atlas = new_atlas(m)
    glow_parts(atlas, m, ("streamer_a", "streamer_b", "streamer_c"), (206, 180, 255), 0.95, 0.5)
    for nm in ("midrib_f", "midrib_b"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (170, 130, 240), 0.35, core=0.1))
    recolour(atlas, m, ("streamer_a", "streamer_b", "streamer_c"), (235, 220, 255), 0.6)
    ref = card(P2 + "45074064fa4f86b133687d85d4048031.jpg", (40, 130, 600, 300), "duality_blade")
    finish("duality_blade", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.16, 0.55, 1.0))


# ================================================================ 3 Frostfang
def frostfang():
    """Card: screenshot "Frostfang" (battleaxe): a jagged, spiked icy blue-teal head shaped like a beast skull, a long curved twisting haft ending in a sharp point."""
    TOTAL = 1.15                                # a battleaxe for a hero: haft 0.8 m plus a big head (the card's head is a third of the length)
    HAFT_TOP, HAFT_R = 80.0, 1.55               # the haft runs 0..80
    BEND = lambda y: 4.5 * math.sin(y / 80.0 * math.pi * 1.25) * (1 - y / 120.0)     # the card's S-curved haft: a slow double bend
    HC = HAFT_TOP + 11.0                        # centre height of the skull
    m = ml.Mesh("frostfang")
    hp = add(m, "haft", "bone", base=(122, 160, 178), smooth=55, uv_mode="loft")
    path = curve(BEND, 8.0, HAFT_TOP + 4.0, 24)
    ml.sweep(hp, path, ml.circle(1.0, 10), scales=lambda t: (HAFT_R * (1.05 - 0.2 * t) * (1 + 0.12 * math.sin(t * 40)), HAFT_R * (1.05 - 0.2 * t) * (1 + 0.12 * math.sin(t * 40))))
    sp = add(m, "butt_spike", "steel", base=(176, 214, 232), smooth=50)
    ml.sweep(sp, [(BEND(8.0), 8.0, 0), (BEND(8.0) - 0.4, 4.0, 0), (BEND(8.0) - 0.6, 0.0, 0)], ml.circle(1.0, 8), scales=lambda t: (1.4 * (1 - t) ** 0.6, 1.4 * (1 - t) ** 0.6) if t < 0.97 else (0.0, 0.0))
    kn = add(m, "haft_knots", "stone", base=(86, 120, 140), smooth=50)
    for s_ in (18.0, 34.0, 50.0, 66.0):         # the knotted bumps of the card's twisting haft
        ml.lathe(kn, "y", (BEND(s_), 0, 0), [(s_ - 1.6, 0.0), (s_ - 0.8, HAFT_R * 1.4), (s_, HAFT_R * 1.7), (s_ + 0.8, HAFT_R * 1.4), (s_ + 1.6, 0.0)], sides=9)
    helix_tube(m, "haft_vine", "bone", (150, 196, 214), lambda t: HAFT_R * 1.0, 14.0, 74.0, 3.0, 50, 0.34, scale=lambda t: (1.0, 1.0))
    hx = BEND(HAFT_TOP)
    # the head: a beast-skull mass on the haft top, the axe edge forward (+x), a crest of spikes swept back (-x), fangs below
    sk = add(m, "skull", "stone", base=(96, 156, 184), smooth=45)
    w.ellipsoid(sk, (hx - 0.5, HC, 0), 6.8, 11.0, 4.2, sides=12, stacks=9)
    sn = add(m, "snout", "stone", base=(110, 170, 196), smooth=45)
    w.ellipsoid(sn, (hx + 5.0, HC - 5.5, 0), 4.6, 4.4, 3.2, sides=10, stacks=7)
    br = add(m, "brow", "stone", base=(120, 180, 204), smooth=45)
    ml.sweep(br, [(hx - 6.0, HC + 3.0, 0), (hx - 1.0, HC + 7.0, 0), (hx + 4.0, HC + 6.5, 0), (hx + 7.0, HC + 2.5, 0)], ml.lozenge(1.0, 1.0), scales=lambda t: (1.6 * (1 - 0.4 * t), 5.4 * (1 - 0.3 * t)), up=(0, 0, 1))
    axb = add(m, "axe_blade", "steel", base=(150, 204, 228), smooth=40, uv_mode="loft")
    bp = []
    for i in range(0, 17):                      # the cutting edge: a crescent sweeping forward from the skull's brow to its jaw
        t = i / 16
        a = math.radians(-64 + 128 * t)
        bp.append((hx + 1.0 + 16.0 * math.cos(a) * 0.95, HC + 0.5 + 14.5 * math.sin(a), 0.0))
    ml.sweep(axb, bp, ml.lozenge(1.0, 1.0, 0.1), scales=lambda t: (3.6 * math.sin(math.pi * min(1, t * 0.96 + 0.02)) ** 0.7 + 0.3, 2.0 * math.sin(math.pi * min(1, t * 0.96 + 0.02)) ** 0.5 + 0.15), up=(0, 0, 1))
    for i in range(0, 7):                       # the swept-back crest of spikes, longest at the top
        sg = add(m, f"crest_{i}", "bone", base=(170, 214, 232), smooth=50)
        a = math.radians(95 + 12 * i)
        cone(sg, (hx - 3.0 + 3.0 * math.cos(a), HC + 3.0 + 7.0 * math.sin(a) * 0.9 - 0.7 * i, 0.0), (math.cos(a) * 1.1 - 0.35, math.sin(a) * 0.6 + 0.12, 0.0), 14.0 - 1.1 * i, 1.8 - 0.14 * i, sides=6)
    hn = add(m, "horn", "bone", base=(196, 232, 245), smooth=50)
    cone(hn, (hx + 1.0, HC + 8.5, 0.0), (0.18, 1.0, 0.0), 12.0, 1.7, sides=6)
    for i in range(0, 5):                       # the fangs hanging under the skull, the card's jaw
        th = add(m, f"fang_{i}", "bone", base=(215, 238, 248), smooth=50)
        cone(th, (hx - 3.5 + 2.8 * i, HC - 10.0, 0.0), (0.15, -1.0, 0.0), 5.2 + (1.0 if i in (1, 3) else 0), 0.9, sides=6)
    for z, nm in ((3.7, "eye"), (-3.7, "eye_back")):
        ey = add(m, nm, "crystal", base=(190, 250, 255), glow=True, smooth=0)
        w.ellipsoid(ey, (hx + 2.5, HC + 1.5, z), 1.2, 1.2, 0.7, sides=8, stacks=5)
    fr = add(m, "frost_crystals", "crystal", base=(180, 236, 255), glow=True, smooth=0)
    for (s_, ang) in ((28, 0.5), (42, 2.6), (58, 4.4), (72, 1.2), (78, 3.6)):   # ice growing on the haft
        ml.gem(fr, (BEND(s_) + HAFT_R * 1.2 * math.cos(ang), s_, HAFT_R * 1.2 * math.sin(ang)), 0.5, long=2.4, sides=5, axis="y")
    atlas = new_atlas(m)
    glow_parts(atlas, m, ("eye", "eye_back"), (160, 245, 255), 1.0, 0.6)
    glow_parts(atlas, m, ("frost_crystals",), (140, 230, 255), 0.75, 0.4)
    glow_parts(atlas, m, ("axe_blade",), (150, 225, 255), 0.18, 0.0)
    recolour(atlas, m, ("skull", "brow", "snout", "haft"), (120, 175, 205), 0.35)
    ref = card(P2 + "Screenshot_20261013_223243_Opera.jpg", (528, 936, 1020, 1560), "frostfang")
    finish("frostfang", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.12, 0.5, 1.0))


# ================================================================ 4 Soul Shatter Blade
def soul_shatter_blade():
    """Card: 736 x 952 "Soul Shatter Blade": a tall jagged greatsword, spiked crimson-black blade, ornate thorny guard, glowing red-orange with embers."""
    TOTAL = 1.55                                # tall, a greatsword: the card sword runs 860 of 952 px
    GRIP, R_GRIP = (6.0, 30.0), 1.5             # the long wrapped grip, card 160 px of 860 -> 24 cm
    GUARD_Y, GUARD_SPAN = 33.0, 15.0            # the thorned guard, card 150 px wide at the crossguard -> half span 15 cm
    BLADE0, TIP = 36.0, 150.0                   # the blade, card 700 px of 860 -> 114 cm
    BLADE_W, BLADE_T = 7.0, 1.1
    m = ml.Mesh("soul_shatter_blade")
    pk = add(m, "pommel", "iron", base=(60, 24, 28), smooth=45)
    ml.lathe(pk, "y", (0, 0, 0), [(0.0, 0.0), (1.0, 1.9), (3.5, 2.3), (5.0, 1.7), (GRIP[0], 1.3)], sides=10)
    ps = add(m, "pommel_spike", "iron", base=(90, 30, 34), smooth=50)
    cone(ps, (0, 0.6, 0), (0, -1, 0), 6.0, 0.9, sides=8)
    gp = add(m, "grip", "leather", base=(48, 18, 22), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 0.95, 0.95), (GRIP[0] + 8, 1.05, 1.05), (GRIP[1], 1.0, 1.0)], ml.circle(R_GRIP, 10))
    wrap_cord(m, "grip_cord", "bronze", (130, 40, 36), R_GRIP, GRIP[0], GRIP[1], 5.0, 0.16)
    gb = add(m, "grip_rings", "iron", base=(96, 34, 36))
    for s in (GRIP[0] + 0.5, 18.0, GRIP[1] - 1.4):
        ml.collar(gb, s, R_GRIP * 1.3, 1.0, lip=0.3, sides=10)
    gd = add(m, "guard_bar", "iron", base=(70, 24, 30), smooth=45)
    ml.loft(gd, "x", (0, GUARD_Y, 0), [(-GUARD_SPAN, 0.5, 0.5), (-GUARD_SPAN * 0.6, 1.4, 1.2), (-4.0, 1.6, 1.8), (4.0, 1.6, 1.8), (GUARD_SPAN * 0.6, 1.4, 1.2), (GUARD_SPAN, 0.5, 0.5)], ml.octagon(2.6, 2.0, 0.6))
    cb = add(m, "guard_crest", "iron", base=(96, 30, 34), smooth=45)
    w.ellipsoid(cb, (0, GUARD_Y + 0.5, 0), 3.6, 3.8, 2.6, sides=10, stacks=8)
    for sgn, nm in ((-1, "l"), (1, "r")):       # the thorns: each side a fan of three, longest outward and angled up
        for k, (ang, ln, ox) in enumerate(((20, 13.0, 6.5), (55, 10.0, 9.5), (88, 7.5, 12.0))):
            th = add(m, f"thorn_{nm}{k}", "iron", base=(110, 32, 34), smooth=50)
            a = math.radians(ang)
            cone(th, (sgn * ox, GUARD_Y + 0.5, 0), (sgn * math.cos(a), math.sin(a), 0.0), ln, 1.4 - 0.15 * k, sides=6)
        lt = add(m, f"thorn_low_{nm}", "iron", base=(90, 28, 32), smooth=50)
        cone(lt, (sgn * 7.0, GUARD_Y - 0.6, 0), (sgn * 0.7, -0.7, 0), 5.0, 0.9, sides=6)
    gm = add(m, "guard_gem", "crystal", base=(255, 110, 60), glow=True, smooth=0)
    ml.gem(gm, (0, GUARD_Y + 0.5, 2.3), 0.9, long=1.5, sides=6, axis="z")
    bl = add(m, "blade", "obsidian", base=(34, 10, 14), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(BLADE0, 0.7, 0.9), (BLADE0 + 4, 1.0, 1.0), (70.0, 0.95, 1.0), (110.0, 0.82, 0.9), (TIP - 14, 0.5, 0.7), (TIP - 4, 0.22, 0.4), (TIP, 0.0, 0.0)], ml.lozenge(BLADE_W, BLADE_T, 0.18))
    for face, nm in ((1, "fuller_f"), (-1, "fuller_b")):   # the glowing red channel down the middle
        fu = add(m, nm, "crystal", base=(255, 90, 50), glow=True, smooth=40)
        ml.fuller(fu, (0, BLADE0 + 5, face * BLADE_T * 0.5), (0, TIP - 22, face * BLADE_T * 0.42), (0, 0, face), 1.3, 0.34, 0.1, taper=6.0)
    for sgn, nm in ((-1, "l"), (1, "r")):       # barbs along both edges, swept back toward the guard, the jagged silhouette of the card
        for k, (s, ln, wd) in enumerate(((44.0, 8.0, 1.8), (58.0, 7.0, 1.6), (73.0, 6.0, 1.5), (90.0, 5.0, 1.3), (108.0, 4.2, 1.1))):
            ba = add(m, f"barb_{nm}{k}", "obsidian", base=(80, 18, 24), smooth=50)
            half = BLADE_W * 0.5 * (0.95 - 0.1 * k)
            cone(ba, (sgn * (half - 0.3), s, 0), (sgn * 0.9, -0.55, 0), ln, wd, sides=5, squash=0.35)
    atlas = new_atlas(m)
    glow_parts(atlas, m, ("fuller_f", "fuller_b"), (255, 100, 40), 1.0, 0.45)
    glow_parts(atlas, m, ("guard_gem",), (255, 120, 70), 1.0, 0.6)
    glow_parts(atlas, m, ("blade",), (255, 70, 30), 0.10, 0.0)
    recolour(atlas, m, ("guard_bar", "guard_crest"), (150, 34, 30), 0.35)
    ref = card(P3 + "617b5d36d924fcc218479263fdbcfe3e.jpg", (540, 40, 736, 950), "soul_shatter_blade")
    finish("soul_shatter_blade", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.14, 0.5, 1.0))


# ================================================================ 5 Buster Sword
def buster_sword():
    """Card: 736 x 920 "Buster Sword": a huge flat single-edged slab of grey steel with a stepped angled tip, a riveted block at the top, two round holes, three grooves, a red wrapped handle."""
    TOTAL = 1.50
    K = 1.0                                     # cm per model unit; the card sword is 590 px from pommel to tip, 1.5 m -> 0.25 cm per px
    GRIP, R_GRIP = (4.0, 30.0), 1.35            # the red wrapped handle (card 120 px -> 30 cm)
    BLOCK = (30.0, 36.0)                        # the riveted guard block at the top of the blade, card 48 px wide x 24 px -> 17 x 6 cm
    BW = 16.0                                   # the slab is 16 cm across (card 95 px at the guard)
    TIP_SPINE, TIP_EDGE = 150.0, 130.0          # the stepped angled tip: the spine reaches 150, the edge ends at 130
    m = ml.Mesh("buster_sword")
    outline = [(-BW / 2, BLOCK[1]), (BW / 2, BLOCK[1]), (BW / 2, TIP_SPINE - 3.0), (BW / 2 - 0.8, TIP_SPINE), (BW / 2 - 5.0, TIP_SPINE - 5.0), (-BW / 2 + 0.6, TIP_EDGE + 4.0), (-BW / 2, TIP_EDGE)]
    bl = add(m, "blade", "steel", base=(168, 174, 184), smooth=30)
    slab(bl, outline, (0.0, 90.0), 1.3, bevel=0.012, edge=0.22)
    eg = add(m, "edge_bevel", "silver", base=(206, 212, 222), wear=0.9, smooth=30)
    for face in (1, -1):                        # the ground edge on both faces along the cutting side
        ml.bar(eg, (-BW / 2 + 0.45, BLOCK[1] + 2, face * 0.65), (-BW / 2 + 0.4, TIP_EDGE - 1, face * 0.65), 0.7, 0.12, up=(0, 0, face), taper=3.0)
    sr = add(m, "spine_ridge", "iron", base=(120, 126, 136), smooth=30)
    for face in (1, -1):
        ml.bar(sr, (BW / 2 - 0.5, BLOCK[1] + 2, face * 0.65), (BW / 2 - 0.5, TIP_SPINE - 6, face * 0.65), 0.9, 0.12, up=(0, 0, face), taper=3.0)
    for i, (y0_, y1_) in enumerate(((78.0, 104.0), (84.0, 112.0), (90.0, 120.0))):    # the three slanted grooves near the tip, each as a pair of ridges
        for face, nm in ((1, "f"), (-1, "b")):
            gr = add(m, f"groove_{nm}{i}", "silver", base=(212, 218, 228), wear=0.9)
            ml.ridge_pair(gr, (-BW / 2 + 1.5 + 0.0 * i, y0_ - 12 * 0, face * 0.65), (BW / 2 - 4.0 - 0.0 * i, y1_ - 6, face * 0.65), (0, 0, face), 0.5, 0.12, 0.07, taper=1.2)
    for sx, sy, nm in ((0.5, 112.0 - 38.0 + 6.0, "a"), (2.2, 112.0 - 38.0 + 11.0, "b")):         # the two round materia holes near the top
        for face, f in ((1, "f"), (-1, "b")):
            ring = add(m, f"materia_ring_{nm}{f}", "iron", base=(96, 100, 110), smooth=45)
            ml.collar(ring, face * 0.62, 1.45, 0.16, lip=0.04, sides=14, axis="z", origin=(sx, sy, 0))
            hole = add(m, f"materia_hole_{nm}{f}", "obsidian", base=(18, 18, 24), smooth=0)
            ml.lathe(hole, "z", (sx, sy, 0), [(face * 0.6, 1.05), (face * 0.78, 1.05)], sides=14)
    gb = add(m, "guard_block", "steel", base=(150, 156, 166), smooth=25)
    ml.box(gb, (-BW / 2 - 1.5, BLOCK[0], -2.3), (BW / 2 + 1.5, BLOCK[1] + 0.4, 2.3), chamfer=0.5)
    rv = add(m, "guard_rivets", "iron", base=(86, 90, 100), smooth=0)
    for face in (1, -1):
        for i in range(6):
            ml.rivet(rv, (-BW / 2 + 0.6 + i * (BW - 1.2) / 5, BLOCK[1] - 2.4, face * 2.3), 0.5, (0, 0, face), 8)
    gp = add(m, "grip", "leather", base=(150, 30, 42), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 0.95, 0.95), (GRIP[1] / 2, 1.05, 1.05), (BLOCK[0] + 0.6, 1.0, 1.0)], ml.circle(R_GRIP, 10))
    wrap_cord(m, "grip_wrap", "cloth", (96, 14, 26), R_GRIP, GRIP[0], BLOCK[0], 6.0, 0.2)
    pc = add(m, "pommel_cap", "iron", base=(100, 104, 114), smooth=45)
    ml.lathe(pc, "y", (0, 0, 0), [(0.0, 1.2), (0.4, 1.7), (2.2, 1.8), (GRIP[0], 1.35)], sides=10)
    pr = add(m, "pommel_ring", "iron", base=(120, 124, 134))
    ml.collar(pr, GRIP[0] - 0.4, R_GRIP * 1.35, 0.8, lip=0.2, sides=10)
    atlas = new_atlas(m)
    ref = card(P3 + "e022ee61292158d695d3003a62ef0a6c.jpg", (40, 40, 700, 640), "buster_sword")
    finish("buster_sword", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.0, 1.0, 1.0))


# ================================================================ 6 Dragon Tooth Blade [Red]
def dragon_tooth_blade_red():
    """Card: 548 x 767 black-and-white ink "Dragon Tooth Blade [Red]": a long thin strongly curved fang-shaped blade, a wrapped handle with a knotted cord, a tail-like curved pommel."""
    TOTAL = 1.15                                # longsword: the drawn fang is 520 px; tail included
    GRIP = (10.0, 28.0)                         # the wrapped handle, card 120 px of 520
    BLADE0, TIP = 30.0, 112.0                   # the blade from the guard knot to the point
    BEND = lambda y: 0.0 - 0.0016 * (y - 30.0) ** 2 if y > 30.0 else 0.0           # the fang curves toward -x: 11 cm off the line at the tip
    m = ml.Mesh("dragon_tooth_blade_red")
    bp = curve(BEND, BLADE0 - 1.0, TIP, 30)
    bl = add(m, "blade", "bone", base=(228, 224, 214), smooth=40, uv_mode="loft")
    ml.sweep(bl, bp, ml.lozenge(1.0, 1.0, 0.25), scales=lambda t: (3.6 * (1 - 0.7 * t ** 1.3) if t < 0.985 else 0.0, 0.95 * (1 - 0.55 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    for face, nm in ((1, "rib_f"), (-1, "rib_b")):       # the card's fine ridge line down the blade, a raised spine each face
        rb = add(m, nm, "silver", base=(244, 242, 236), wear=0.9, smooth=45)
        pts = offset_xy(bp[2:-3], 0.35, face * 0.42)
        ml.sweep(rb, pts, ml.circle(0.2, 5), scales=lambda t: (1.0 - 0.6 * t, 1.0 - 0.6 * t))
    bk = add(m, "back_edge", "iron", base=(150, 146, 140), smooth=45)
    ml.sweep(bk, offset_xy(bp[1:-2], -1.0, 0.0), ml.circle(0.28, 6), scales=lambda t: (1.0 - 0.5 * t, 1.0 - 0.5 * t))
    hb = add(m, "habaki", "gold", base=(180, 150, 88), smooth=45)
    ml.collar(hb, BLADE0 - 2.2, 1.9, 2.4, lip=0.4, sides=10, oval=0.5)
    gp = add(m, "grip", "leather", base=(60, 40, 38), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 0.95, 0.95), (19.0, 1.08, 1.08), (GRIP[1], 1.0, 1.0)], ml.circle(1.3, 10))
    wrap_cord(m, "grip_cord", "cloth", (170, 34, 34), 1.3, GRIP[0], GRIP[1], 6.5, 0.2)
    kn = add(m, "knot", "cloth", base=(150, 30, 34), smooth=60)
    for (x, y_, z, r) in ((0.9, 29.5, 1.0, 1.1), (-0.8, 30.5, 0.6, 1.0), (0.2, 28.4, -1.1, 1.0), (1.2, 27.0, 0.8, 0.9)):   # the knotted cord cluster at the guard
        w.ellipsoid(kn, (x, y_, z), r, r, r, sides=8, stacks=6)
    gd = add(m, "guard_ring", "iron", base=(86, 82, 78), smooth=45)
    ml.lathe(gd, "y", (0, 0, 0), [(GRIP[1] - 0.2, 1.2), (GRIP[1] + 0.4, 2.1), (GRIP[1] + 1.2, 2.0), (GRIP[1] + 1.8, 1.0)], sides=12, squash=0.6)
    tail = add(m, "tail", "bone", base=(120, 110, 100), smooth=50, uv_mode="loft")
    TX = lambda t: 5.5 * math.sin(t * math.pi * 0.85)                       # the tail swings out to +x and curls back, as the card's tail does
    tpath = [(TX(i / 14), GRIP[0] - 1.0 - 11.0 * (i / 14), 0.0) for i in range(15)]
    ml.sweep(tail, tpath, ml.circle(1.0, 8), scales=lambda t: (1.15 * (1 - 0.85 * t) if t < 0.97 else 0.0, 1.15 * (1 - 0.85 * t) if t < 0.97 else 0.0))
    tr = add(m, "tail_scales", "iron", base=(96, 88, 80), smooth=50)
    for i in range(1, 11):                       # the card's scaled tail: ring bands along it
        t = i / 14
        ml.collar(tr, GRIP[0] - 1.0 - 11.0 * t - 0.3, 1.15 * (1 - 0.85 * t) * 1.2 + 0.05, 0.6, lip=0.15, sides=8, origin=(TX(t), 0, 0))
    gl = add(m, "ember_edge", "crystal", base=(255, 90, 50), glow=True, smooth=40)
    ml.sweep(gl, offset_xy(bp[6:-4], 1.3, 0.0), ml.circle(0.16, 5), scales=lambda t: (1.0 - 0.5 * t, 1.0 - 0.5 * t))
    atlas = new_atlas(m)
    glow_parts(atlas, m, ("ember_edge",), (255, 80, 40), 0.9, 0.4)
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (255, 70, 40), 0.12, core=0.0))
    ref = card(P3 + "f23f1ffb29e630a916fccacd041436ff.jpg", (30, 160, 510, 520), "dragon_tooth_blade_red")
    finish("dragon_tooth_blade_red", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.14, 0.5, 1.0))


# ================================================================ 7 Corpse Eater
def corpse_eater():
    """Card: 736 x 756 "Corpse Eater" (frenzy form): a katana with a black-red-white diamond-wrapped handle, a gold tsuba, a dark spine and a jagged white edge split open to reveal red tendons and one staring eye."""
    TOTAL = 1.15                                # a katana: card sword 800 px, a long blade
    GRIP = (6.0, 30.0)                          # the diamond-wrapped handle, card 150 px of 800 -> 22 cm
    TSUBA_Y = 31.0                              # the gold guard
    BLADE0, TIP = 33.0, 112.0                   # blade from the tsuba to the point
    BEND = lambda y: -0.0013 * (y - 33.0) ** 2 if y > 33.0 else 0.0     # a gentle katana curve toward -x
    GAP = 1.1                                   # the split between the spine and the edge strip: gap of the card
    m = ml.Mesh("corpse_eater")
    bp = curve(BEND, BLADE0 - 1.0, TIP, 24)
    sp = add(m, "spine", "obsidian", base=(36, 28, 32), smooth=40, uv_mode="loft")
    ml.sweep(sp, offset_xy(bp, 2.2, 0.0), ml.lozenge(1.0, 1.0, 0.1), scales=lambda t: (2.7 * (1 - 0.3 * t) if t < 0.985 else 0.0, 1.5 * (1 - 0.3 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    ed = add(m, "edge_strip", "silver", base=(238, 238, 242), smooth=40, uv_mode="loft")
    jag = lambda t: 1.0 + 0.4 * (((t * 12.0) % 1.0) - 0.5)          # the saw-tooth outer edge of the card
    ml.sweep(ed, offset_xy(bp[3:], -2.5, 0.0), ml.lozenge(1.0, 1.0, 0.15), scales=lambda t: (2.4 * jag(t) * (1 - 0.35 * t) if t < 0.985 else 0.0, 0.7 * (1 - 0.3 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    teeth = add(m, "edge_teeth", "silver", base=(250, 250, 252), smooth=60)
    for i in range(5, 24, 2):                    # the jagged teeth along the cutting edge
        p = np.array(bp[i]) + normal_xy(bp, i) * -3.6
        cone(teeth, tuple(p), tuple(-normal_xy(bp, i) * 0.9 + np.array([0, -0.45, 0])), 2.2 - 0.03 * i, 0.6, sides=5, squash=0.5)
    for i, s_ in enumerate((5, 8, 11, 14, 17, 20)):   # the red tendons across the split: thin bars from the spine to the edge strip, slanted
        a, b = np.array(bp[s_]), np.array(bp[min(s_ + 2, len(bp) - 1)])
        td = add(m, f"tendon_{i}", "cloth", base=(176, 28, 40), smooth=45)
        rod(td, a + normal_xy(bp, s_) * 1.2, b + normal_xy(bp, s_ + 2) * -1.6, 0.36 - 0.012 * i, sides=5)
    fl = add(m, "flesh_seam", "cloth", base=(130, 20, 30), smooth=45)
    ml.sweep(fl, offset_xy(bp[2:-4], -0.2, 0.0), ml.lozenge(1.0, 1.0, 0.0), scales=lambda t: (2.0 * (1 - 0.4 * t), 0.6), up=(0, 0, 1))
    ex, ey_, ez = BEND(60.0) - 0.1, 60.0, 0.0
    sc = add(m, "eye_white", "bone", base=(250, 244, 240), smooth=0)
    w.ellipsoid(sc, (ex, ey_, 0.3), 1.35, 1.1, 0.7, sides=12, stacks=8)
    ir = add(m, "eye_iris", "crystal", base=(220, 24, 40), glow=True, smooth=0)
    w.ellipsoid(ir, (ex, ey_, 0.8), 0.62, 0.62, 0.42, sides=10, stacks=6)
    pu = add(m, "eye_pupil", "obsidian", base=(10, 6, 8), smooth=0)
    w.ellipsoid(pu, (ex, ey_, 1.1), 0.24, 0.3, 0.2, sides=8, stacks=5)
    ts = add(m, "tsuba", "gold", base=(206, 164, 70), smooth=40)
    ml.lathe(ts, "y", (0, 0, 0), [(TSUBA_Y, 4.3), (TSUBA_Y + 0.3, 4.6), (TSUBA_Y + 0.7, 4.5), (TSUBA_Y + 0.9, 3.0)], sides=16, squash=0.8)
    hb = add(m, "habaki", "gold", base=(180, 146, 66), smooth=45)
    ml.collar(hb, TSUBA_Y + 0.9, 1.8, 2.2, lip=0.4, sides=10, oval=0.55)
    sd = add(m, "seppa", "gold", base=(220, 184, 90))
    ml.collar(sd, TSUBA_Y - 0.5, 2.2, 0.55, lip=0.12, sides=12)
    gp = add(m, "grip_core", "leather", base=(34, 20, 24), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 0.95, 0.95), (18.0, 1.1, 1.1), (TSUBA_Y - 0.5, 1.0, 1.0)], ml.circle(1.4, 10))
    for nm, mat, col, ph, d in (("wrap_red_a", "cloth", (172, 24, 36), 0.0, 1), ("wrap_red_b", "cloth", (172, 24, 36), 1.4, -1), ("wrap_white_a", "cloth", (236, 232, 228), 0.7, 1), ("wrap_white_b", "cloth", (236, 232, 228), 2.1, -1)):
        helix_tube(m, nm, mat, col, lambda t: 1.52, GRIP[0] + 0.3, TSUBA_Y - 1.0, 4.0 * d, 40, 0.2, phase=ph)
    fu = add(m, "fuchi", "gold", base=(206, 164, 70))
    ml.collar(fu, TSUBA_Y - 1.4, 1.75, 0.9, lip=0.2, sides=12)
    kb = add(m, "kashira", "gold", base=(196, 154, 66), smooth=45)
    ml.lathe(kb, "y", (0, 0, 0), [(0.0, 0.9), (0.6, 1.7), (GRIP[0] + 1.0, 1.7), (GRIP[0] + 1.4, 1.4)], sides=12, squash=0.85)
    atlas = new_atlas(m)
    glow_parts(atlas, m, ("eye_iris",), (255, 50, 70), 1.0, 0.5)
    glow_parts(atlas, m, ("flesh_seam",), (230, 40, 50), 0.35, 0.1)
    ref = card(P2 + "2caad00290ef46d44d19a057de239c56.jpg", (0, 20, 700, 740), "corpse_eater")
    finish("corpse_eater", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.20, 0.5, 1.0))


# ================================================================ axes: a shared head builder
def axe_head(m, prefix, material, base, centre_x, y_poll, outline_w, y0, y1, thick, edge_curve, n=18, smooth=35):
    """An axe head as a loft along y: a wedge section (poll thick, edge thin) scaled in width per station, so the silhouette is the profile
    `outline_w(y)` (how far the edge reaches +x) and `edge_curve(y)` (a shift of the edge). The poll stays on the plane x = centre_x - 1.6."""
    hd = add(m, prefix, material, base=base, smooth=smooth)
    sec = [(1.0, 0.0), (0.5, 0.5), (0.0, 0.45), (0.0, -0.45), (0.5, -0.5)]
    sts = []
    for i in range(n + 1):
        y_ = y0 + (y1 - y0) * i / n
        wd = max(outline_w(y_), 0.0)
        sts.append((y_, max(wd, 1e-4), thick * (0.35 + 0.65 * min(1.0, wd / max(outline_w(y_poll), 1e-3))), 0.0, 0.0))
    sts[0] = (sts[0][0], sts[0][1], sts[0][2], 0.0, 0.0)
    return hd, sec, sts


# ================================================================ 8 Random Axe of Charity
def random_axe_of_charity():
    """Card: screenshot "Random Axe of Charity" (+1 handaxe): a simple handaxe, brown wooden haft, grey steel head with a blood-red stain, copper coins scattering round it."""
    TOTAL = 0.52                                # a handaxe: card haft 500 px of ~560 -> about 45 cm of haft, head on top
    HAFT = (0.0, 56.0)                          # the wooden haft
    R_HAFT = 1.45
    HEAD_Y = (26.0, 56.0)                       # the head stretches 30 cm along the haft (beard down to y 26), card head 155 px
    m = ml.Mesh("random_axe_of_charity")
    hf = add(m, "haft", "wood", base=(138, 90, 50), uv_mode="loft", smooth=50)
    ml.loft(hf, "y", (0, 0, 0), [(HAFT[0], 1.25, 1.25), (6.0, 0.95, 0.95), (24.0, 1.0, 1.0), (44.0, 1.12, 1.12), (58.0, 1.2, 1.2)], ml.circle(R_HAFT, 12))
    sw = add(m, "haft_swell", "wood", base=(120, 78, 44), smooth=55)
    ml.lathe(sw, "y", (0, 0, 0), [(0.0, 1.9), (1.2, 2.2), (3.4, 1.7), (5.0, R_HAFT * 1.0)], sides=12, squash=0.85)       # the flared butt
    gw = add(m, "grip_wrap", "leather", base=(86, 52, 32), uv_mode="loft")
    ml.loft(gw, "y", (0, 0, 0), [(7.0, 1.0, 1.0), (21.0, 1.0, 1.0)], ml.circle(R_HAFT * 1.08, 12))
    for s in (7.0, 14.0, 20.0):
        gb = add(m, f"wrap_band_{int(s)}", "leather", base=(60, 38, 24))
        ml.collar(gb, s, R_HAFT * 1.2, 0.9, lip=0.25, sides=12)
    edge_w = lambda y: (11.0 * (1 - ((y - 38.0) / 17.0) ** 2) ** 0.75 + 2.0) if 26.0 <= y <= 55.0 and y < 38.0 else (13.0 * (1 - ((y - 38.0) / 17.5) ** 2) ** 0.8 + 1.0) if 38.0 <= y <= 55.0 else 0.0
    hd = add(m, "head", "steel", base=(150, 156, 166), smooth=35)
    sec = [(1.0, 0.0), (0.55, 0.5), (0.0, 0.48), (0.0, -0.48), (0.55, -0.5)]
    sts = [(y_, max(edge_w(y_), 1e-4), 2.8 * (0.3 + 0.7 * min(1.0, edge_w(y_) / 13.0)), 0.4, 0.0) for y_ in np.linspace(HEAD_Y[0] + 0.2, HEAD_Y[1] - 0.2, 20)]
    sts[0] = (sts[0][0], 1e-4, 0.3, sts[0][3], 0.0)
    sts[-1] = (sts[-1][0], 1e-4, 0.3, sts[-1][3], 0.0)
    ml.loft(hd, "y", (0, 0, 0), sts, sec)
    eye = add(m, "eye_collar", "iron", base=(96, 98, 106), smooth=45)
    ml.lathe(eye, "y", (0, 0, 0), [(36.0, R_HAFT * 1.3), (37.0, 2.0), (51.0, 2.0), (52.0, R_HAFT * 1.3)], sides=12, squash=0.95)
    cheek = add(m, "edge_bevel", "silver", base=(214, 220, 230), wear=0.9, smooth=30)
    for face in (1, -1):
        pts = [(0.4 + edge_w(y_) - 0.5, y_, face * 0.42) for y_ in np.linspace(30.0, 54.0, 14)]
        ml.sweep(cheek, pts, ml.circle(0.2, 5), scales=lambda t: (1.0 - 0.5 * abs(2 * t - 1) ** 2, 1.0 - 0.5 * abs(2 * t - 1) ** 2))
    wd = add(m, "wedge_pin", "iron", base=(60, 60, 66), smooth=45)
    ml.lathe(wd, "z", (0.0, 53.5, 0), [(-2.1, 0.0), (-1.9, 0.5), (1.9, 0.5), (2.1, 0.0)], sides=6)
    rg = add(m, "charm_ring", "gold", base=(190, 120, 64), smooth=50)
    ml.sweep(rg, [(1.0 + 1.8 * math.cos(a), -1.0 + 1.8 * math.sin(a) - 1.5, 0.0) for a in np.linspace(0, 2 * math.pi, 14)], ml.circle(0.2, 6), scales=lambda t: (1.0, 1.0), caps=(False, False))
    for i, (cx, cy, cz) in enumerate(((2.4, -4.2, 0.0), (-0.2, -4.7, 1.0), (1.2, -5.2, -1.0))):      # three copper coins on the butt ring
        co = add(m, f"coin_{i}", "bronze", base=(204, 122, 64), smooth=45)
        ml.lathe(co, "z", (cx, cy, cz), [(-0.12, 0.9), (0.12, 0.9)], sides=12)
    atlas = new_atlas(m)

    def stain(c):                                # the blood-red stain across the lower blade, painted on the head's skin
        y_ = c.B
        mask = ((y_ > 28.0) & (y_ < 46.0) & (c.S > 0.35)).astype(np.float32)
        wob = w.noise2(c.img.shape[:2], 5.0, 4)
        k = np.clip(mask * (0.55 + 0.6 * wob), 0, 1)[..., None]
        c.img[..., :3] = c.img[..., :3] * (1 - k) + np.array([166, 20, 30], np.float32) * k
    w.paint_part(atlas, m.part("head"), stain)
    recolour(atlas, m, ("coin_0", "coin_1", "coin_2"), (230, 130, 70), 0.4)
    glow_parts(atlas, m, ("charm_ring",), (255, 170, 80), 0.25, 0.1)
    ref = card(P2 + "Screenshot_20261013_223256_Opera.jpg", (24, 732, 672, 1464), "random_axe_of_charity")
    finish("random_axe_of_charity", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.10, 0.5, 1.0))


# ================================================================ 9 Leviathan Axe
def leviathan_axe():
    """No art: the card (screenshot header "Leviathan Axe - Weapon (handaxe), Legendary (requires attunement)") is cut off. A frost axe after the name: a
    rune-carved bearded head of pale blue steel on a leather-wrapped haft with a runed pommel, a frost crown spike on top. Invented; says so in the report."""
    TOTAL = 0.78
    HAFT = (0.0, 66.0)
    R_HAFT = 1.55
    m = ml.Mesh("leviathan_axe")
    hf = add(m, "haft", "wood", base=(70, 86, 104), uv_mode="loft", smooth=50)
    ml.loft(hf, "y", (0, 0, 0), [(0.0, 0.9, 0.9), (8.0, 1.0, 1.0), (40.0, 1.0, 1.0), (HAFT[1], 1.12, 1.12)], ml.circle(R_HAFT, 12))
    gw = add(m, "grip_wrap", "leather", base=(46, 70, 96), uv_mode="loft")
    ml.loft(gw, "y", (0, 0, 0), [(8.0, 1.0, 1.0), (30.0, 1.0, 1.0)], ml.circle(R_HAFT * 1.08, 12))
    for s in (8.0, 15.0, 22.0, 29.0):
        gb = add(m, f"wrap_band_{int(s)}", "iron", base=(120, 150, 176))
        ml.collar(gb, s, R_HAFT * 1.2, 0.8, lip=0.22, sides=12)
    pm = add(m, "pommel", "steel", base=(150, 196, 222), smooth=45)
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 1.0), (0.8, 2.3), (3.2, 2.5), (5.0, R_HAFT * 1.1)], sides=12)
    pr = add(m, "pommel_rune_ring", "crystal", base=(120, 230, 255), glow=True, smooth=0)
    ml.collar(pr, 2.0, 2.52, 0.5, lip=0.12, sides=12)
    edge_w = lambda y: (11.0 - 7.8 * abs((y - 56.0) / 14.0) ** 1.7) if 42.0 <= y <= 70.0 else 0.0
    hd = add(m, "head", "steel", base=(168, 212, 236), smooth=35)
    sec = [(1.0, 0.0), (0.55, 0.5), (0.0, 0.48), (0.0, -0.48), (0.55, -0.5)]
    ys = np.linspace(42.3, 69.7, 22)
    sts = [(y_, max(edge_w(y_), 1e-4), 3.3 * (0.3 + 0.7 * min(1.0, edge_w(y_) / 11.0)), 0.5, 0.0) for y_ in ys]
    sts[0] = (sts[0][0], 1e-4, 0.3, 0.5, 0.0)
    sts[-1] = (sts[-1][0], 1e-4, 0.3, 0.5, 0.0)
    ml.loft(hd, "y", (0, 0, 0), sts, sec)
    eye = add(m, "eye_collar", "iron", base=(96, 126, 150), smooth=45)
    ml.lathe(eye, "y", (0, 0, 0), [(49.0, R_HAFT * 1.35), (50.0, 2.5), (63.0, 2.5), (64.0, R_HAFT * 1.35)], sides=12, squash=0.95)
    cheek = add(m, "edge_bevel", "silver", base=(224, 244, 255), wear=0.9, smooth=30)
    for face in (1, -1):
        pts = [(0.6 + edge_w(y_) - 0.5, y_, face * 0.34) for y_ in np.linspace(45.0, 67.0, 14)]
        ml.sweep(cheek, pts, ml.circle(0.22, 5), scales=lambda t: (1.0 - 0.5 * abs(2 * t - 1) ** 2, 1.0 - 0.5 * abs(2 * t - 1) ** 2))
    bk = add(m, "back_spike", "steel", base=(150, 196, 222), smooth=45)
    cone(bk, (-1.0, 56.5, 0), (-1.0, 0.1, 0), 7.5, 1.35, sides=8, squash=0.8)
    cr = add(m, "frost_crown", "crystal", base=(190, 240, 255), glow=True, smooth=0)
    for k, (dx, ln) in enumerate(((-1.4, 5.0), (0.0, 7.5), (1.4, 5.0))):       # a crown of ice rising from the haft top
        ml.gem(cr, (dx * 0.7, HAFT[1] + ln * 0.45, 0), 0.5, long=ln / 1.4 * 0.5 + 1.2, sides=5, axis="y")
    for i, (yy, sz) in enumerate(((46.0, 0.8), (62.0, 0.7), (56.5, 0.9))):      # ice crystals on the flat of the head, edge side
        ic = add(m, f"ice_{i}", "crystal", base=(190, 240, 255), glow=True, smooth=0)
        ml.gem(ic, (2.0 + 2.0 * i, yy, 1.7), sz * 0.5, long=2.0, sides=5, axis="y")
    atlas = new_atlas(m)
    # glowing rune lines engraved on both faces of the head
    for sign in (1, -1):
        for (a, b) in (((3.5, 52.0), (6.0, 58.0)), ((6.0, 58.0), (3.8, 61.5)), ((3.2, 49.0), (3.2, 56.0)), ((7.0, 53.0), (8.0, 56.5))):
            atlas.line(m.part("head"), (a[0], a[1], sign * 1.3), (b[0], b[1], sign * 1.3), 0.32, axis="z", sign=sign, dark=0.5, glow_rgb=(110, 232, 255), glow_strength=1.0)
    glow_parts(atlas, m, ("frost_crown", "pommel_rune_ring", "ice_0", "ice_1", "ice_2"), (130, 235, 255), 0.9, 0.5)
    w.paint_part(atlas, m.part("head"), lambda c: w.set_glow(c, (120, 220, 255), 0.06, core=0.0))
    finish("leviathan_axe", m, atlas, TOTAL, None, glow=("FFFFFF", 0.12, 0.5, 1.0))


# ================================================================ 10 Shadow Burn
def shadow_burn():
    """Card: 736 x 421 "Shadow Burn": a broad slab-like greatsword, a rectangular dark-steel blade with an orange-red glowing rune/engraving on the lower blade, shadow wisps in blue-purple, a gold-trimmed crossguard, a red-wrapped grip and a glowing purple pommel orb."""
    TOTAL = 1.40                                # heavy greatsword: card sword 440 px of 421-high panel; 6 lb, a real greatsword is ~1.4 m
    GRIP, R_GRIP = (6.0, 26.0), 1.45            # red-wrapped grip: card 85 px of 440
    GUARD_Y, GUARD_SPAN = 28.0, 11.0            # the gold-trimmed crossguard, card 110 px wide -> half span 11 cm
    BLADE0, TIP = 30.0, 138.0                   # the slab: 108 cm
    BW, BT = 11.0, 1.0                          # broad: 11 cm wide
    m = ml.Mesh("shadow_burn")
    outline = [(-BW / 2, BLADE0), (BW / 2, BLADE0), (BW / 2, TIP - 6.0), (BW / 2 - 1.2, TIP), (-BW / 2 + 1.2, TIP), (-BW / 2, TIP - 6.0)]
    bl = add(m, "blade", "iron", base=(104, 108, 126), smooth=30)
    slab(bl, outline, (0.0, 84.0), BT, bevel=0.012, edge=0.2)
    for face, nm in ((1, "f"), (-1, "b")):
        sr = add(m, "edge_trim_" + nm, "gold", base=(214, 176, 90), wear=0.9, smooth=30)
        for sx in (-BW / 2 + 0.4, BW / 2 - 0.4):
            ml.bar(sr, (sx, BLADE0 + 3, face * BT / 2), (sx, TIP - 7, face * BT / 2), 0.55, 0.1, up=(0, 0, face), taper=3.0)
        tp = add(m, "tip_cap_" + nm, "gold", base=(222, 184, 100), wear=0.9, smooth=30)
        ml.bar(tp, (-BW / 2 + 1.0, TIP - 0.7, face * BT / 2), (BW / 2 - 1.0, TIP - 0.7, face * BT / 2), 0.5, 0.1, up=(0, 0, face), taper=1.5)
    for face, nm in ((1, "f"), (-1, "b")):       # the card's engraved reddish panel on the lower blade, as a thin raised plate
        pl = add(m, "rune_plate_" + nm, "bronze", base=(120, 52, 36), smooth=30)
        ml.box(pl, (-3.9, 38.0, min(face * BT / 2 - 0.05, face * (BT / 2 + 0.18))), (3.9, 77.0, max(face * BT / 2 - 0.05, face * (BT / 2 + 0.18))), chamfer=0.07)
    rg = add(m, "ricasso", "gold", base=(190, 150, 70), smooth=40)
    ml.box(rg, (-BW / 2 - 0.3, BLADE0 - 1.0, -0.9), (BW / 2 + 0.3, BLADE0 + 2.2, 0.9), chamfer=0.25)
    gd = add(m, "guard_bar", "gold", base=(206, 160, 70), smooth=45)
    ml.loft(gd, "x", (0, GUARD_Y, 0), [(-GUARD_SPAN, 0.6, 0.7), (-GUARD_SPAN * 0.75, 1.3, 1.2), (-3.0, 1.6, 1.6), (3.0, 1.6, 1.6), (GUARD_SPAN * 0.75, 1.3, 1.2), (GUARD_SPAN, 0.6, 0.7)], ml.octagon(2.4, 2.2, 0.55))
    for sgn, nm in ((-1, "l"), (1, "r")):        # the guard ends turn down into points (card: gold quillons curling toward the grip)
        qp = add(m, "quillon_" + nm, "gold", base=(222, 180, 84), smooth=50)
        cone(qp, (sgn * GUARD_SPAN * 0.97, GUARD_Y, 0), (sgn * 0.45, -1.0, 0.0), 5.5, 1.0, sides=6)
        up = add(m, "quillon_up_" + nm, "gold", base=(212, 168, 76), smooth=50)
        cone(up, (sgn * GUARD_SPAN * 0.55, GUARD_Y + 0.8, 0), (sgn * 0.3, 1.0, 0.0), 3.6, 0.8, sides=6)
    ob = add(m, "guard_boss", "gold", base=(190, 150, 70), smooth=45)
    w.ellipsoid(ob, (0, GUARD_Y + 0.4, 0), 2.6, 2.4, 2.0, sides=10, stacks=8)
    gp = add(m, "grip", "leather", base=(130, 22, 34), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(GRIP[0], 0.95, 0.95), (16.0, 1.06, 1.06), (GRIP[1], 1.0, 1.0)], ml.circle(R_GRIP, 10))
    wrap_cord(m, "grip_cord", "cloth", (200, 160, 80), R_GRIP, GRIP[0], GRIP[1], 6.0, 0.17)
    for s in (GRIP[0] + 0.3, GRIP[1] - 1.3):
        gb = add(m, f"grip_ring_{int(s)}", "gold", base=(214, 172, 80))
        ml.collar(gb, s, R_GRIP * 1.3, 1.0, lip=0.28, sides=10)
    pc = add(m, "pommel_cup", "gold", base=(200, 156, 68), smooth=45)
    ml.lathe(pc, "y", (0, 0, 0), [(0.0, 0.8), (0.4, 2.4), (2.2, 2.6), (GRIP[0], 1.5)], sides=12)
    po = add(m, "pommel_orb", "crystal", base=(170, 110, 255), glow=True, smooth=0)
    w.ellipsoid(po, (0, -1.2, 0), 1.9, 1.9, 1.9, sides=12, stacks=8)
    for k, (nm, ph, rr) in enumerate((("wisp_a", 0.0, 5.0), ("wisp_b", math.pi, 5.4))):     # the swirling blue-purple shadow round the lower blade
        wp = add(m, nm, "crystal", base=(140, 110, 255), glow=True, smooth=60)
        pts = [(rr * (1 - 0.6 * t) * math.cos(ph + 2 * math.pi * 1.5 * t), BLADE0 + 6 + (96.0 - BLADE0 - 6) * t, 1.3 * math.sin(ph + 2 * math.pi * 1.5 * t)) for t in np.linspace(0, 1, 36)]
        ml.sweep(wp, pts, ml.circle(1.0, 5), scales=lambda t: (0.55 * math.sin(math.pi * min(1.0, t * 1.05)) + 0.08, 0.35 * math.sin(math.pi * min(1.0, t * 1.05)) + 0.06))
    atlas = new_atlas(m)
    for sign in (1, -1):                          # the orange-red glowing runes on the lower blade (the card's engraved band)
        pl = m.part("rune_plate_" + ("f" if sign == 1 else "b"))
        pts = [((-3.4, 40.0), (3.4, 40.0)), ((3.4, 40.0), (3.4, 74.0)), ((3.4, 74.0), (-3.4, 74.0)), ((-3.4, 74.0), (-3.4, 40.0)), ((0.0, 44.0), (0.0, 70.0)),
               ((-2.0, 50.0), (2.0, 54.0)), ((2.0, 58.0), (-2.0, 62.0)), ((-2.0, 66.0), (2.0, 69.0))]
        for (a, b) in pts:
            atlas.line(pl, (a[0], a[1], sign * (BT / 2 + 0.16)), (b[0], b[1], sign * (BT / 2 + 0.16)), 0.45, axis="z", sign=sign, dark=0.55, glow_rgb=(255, 110, 40), glow_strength=1.0)
    glow_parts(atlas, m, ("pommel_orb",), (170, 110, 255), 1.0, 0.15)
    glow_parts(atlas, m, ("wisp_a", "wisp_b"), (140, 100, 255), 0.85, 0.2)
    recolour(atlas, m, ("wisp_a", "wisp_b"), (150, 120, 255), 0.8)
    ref = card(P3 + "6756d5759f7140671d0a9b038530e7c5.jpg", (0, 0, 370, 421), "shadow_burn")
    finish("shadow_burn", m, atlas, TOTAL, ref, glow=("FFFFFF", 0.16, 0.55, 1.0))


BUILDERS = {"spotlight": spotlight, "duality_blade": duality_blade, "frostfang": frostfang, "soul_shatter_blade": soul_shatter_blade,
            "buster_sword": buster_sword, "dragon_tooth_blade_red": dragon_tooth_blade_red, "corpse_eater": corpse_eater,
            "random_axe_of_charity": random_axe_of_charity, "leviathan_axe": leviathan_axe, "shadow_burn": shadow_burn}


def main(only=None):
    for item_id, fn in BUILDERS.items():
        if only and item_id not in only:
            continue
        fn()


if __name__ == "__main__":
    main(sys.argv[1:])
