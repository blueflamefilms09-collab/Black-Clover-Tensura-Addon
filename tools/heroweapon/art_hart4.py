"""Why: the art of the five Vassal weapons the logic builder hvd writes (Mirror, Musical Instrument, Harpoon, Sewing Set, Chain) of The Rising of the
Shield Hero: the 3D model in hand, its painted skin, its additive glow skin, the 16 x 16 inventory sprite, the two item models and the size class of
each weapon, all drawn here from code so that every number can be read and changed and a rebuild gives the same files.

    python3 -B tools/heroweapon/art_hart4.py [id ...]                     build the files (no ids = all five; idempotent, deterministic)
    python3 -B tools/heroweapon/art_hart4.py --check                      read the written files back and list problems (nothing is rebuilt)
    python3 -B tools/heroweapon/art_hart4.py [--preview=DIR] [--audit]    also render the pictures into DIR (or $HART4_PREVIEW) / print the size audit

Per id it writes (the files design.md section 11 asks for)
  assets/nusmp/weapon_meshes/<id>.txt                 the mesh, smoothed per-vertex normals ("qn" lines), placed on the sprite's 45 degree diagonal
  assets/nusmp/textures/entity/weapon/<id>.png        the painted skin (512 px) and <id>_glow.png the additive glow skin
  assets/nusmp/textures/item/<id>.png                 the 16 x 16 sprite: the inventory icon, drawn by hand-built pixel code
  assets/nusmp/models/item/<id>.json, <id>_icon.json  the in-hand model (builtin/entity, drawn by client/WeaponRenderer) and the flat icon model
and tools/heroweapon/classes_hart4.json (id -> size class of weapon_classes.json, merged by the lead).

The method is the owner's (tools/relics/mesh_lib.py, worked example tools/relics/wave23_models.py): decide the target, real cross-sections (lofts, sweeps,
lathes), the small raised parts as their own named parts, smoothed normals, painted materials plus a second additive glow skin, then render, look,
correct. There is no reference picture for these weapons (the series shows them little): each one is designed with intent from what the wiki summaries
say, and the comment block above each weapon's constants says what was taken from the series and what is invented.

Axes: y along the weapon (the grip at low y, the business end at high y), x across it (-x is forward, the convention of docs/weapon_scale_orientation.md),
z through it (the face of a flat piece looks toward +z). Units are model centimetres; place_on_diagonal grows the mesh to its apparent size in the hand.
"""
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import art_hart4_kit as k                                        # noqa: E402
from art_hart4_kit import V, ml, w1, unit                       # noqa: E402

PREVIEW = os.environ.get("HART4_PREVIEW", "")
IDS = ["vassal_mirror", "vassal_harpoon", "vassal_chain", "vassal_instrument", "vassal_sewing_set"]

# ================================================================ materials of this set (mesh_lib's MATERIALS, extended: base colour AND highlight colour)
ml.MATERIALS.update({
    "steel_blue": dict(type="metal", base=(143, 163, 181), hi=(228, 238, 246), streak=.10, grain=.05, blotch=.08, band=.58, band_pos=-.30, band_w=.30, edge=.20, scratches=14),
    "brown_leather": dict(type="leather", base=(90, 62, 42), hi=(142, 102, 70)),
    "iron_dark": dict(type="metal", base=(60, 64, 71), hi=(150, 158, 170), streak=.10, grain=.07, blotch=.14, band=.34, band_pos=-.30, band_w=.28, edge=.22, scratches=12),
    "dull_steel": dict(type="metal", base=(124, 135, 145), hi=(197, 205, 211), streak=.12, grain=.08, blotch=.16, band=.40, band_pos=-.30, band_w=.28, edge=.24, scratches=16),
    "iron_black": dict(type="metal", base=(46, 50, 56), hi=(120, 128, 140), streak=.08, grain=.06, blotch=.12, band=.30, band_pos=-.30, band_w=.28, edge=.20, scratches=10),
    "iron_stud": dict(type="metal", base=(105, 114, 124), hi=(190, 198, 208), streak=.08, grain=.06, blotch=.12, band=.40, band_pos=-.30, band_w=.28, edge=.20, scratches=8),
    "dark_leather": dict(type="leather", base=(74, 58, 44), hi=(120, 96, 72)),
    "spruce": dict(type="wood", base=(184, 99, 31), hi=(224, 162, 74)),
    "maple_back": dict(type="wood", base=(170, 88, 28), hi=(214, 140, 60)),
    "maple_neck": dict(type="wood", base=(150, 78, 26), hi=(205, 130, 58)),
    "maple_bridge": dict(type="wood", base=(217, 176, 122), hi=(240, 210, 160)),
    "ebony": dict(type="wood", base=(28, 20, 16), hi=(112, 88, 72)),
    "bow_wood": dict(type="wood", base=(90, 58, 34), hi=(150, 104, 66)),
    "hair": dict(type="cloth", base=(242, 237, 226), hi=(255, 255, 250)),
    "violet_wood": dict(type="wood", base=(75, 42, 107), hi=(122, 80, 164)),
    "silver_rim": dict(type="metal", base=(191, 198, 207), hi=(250, 252, 255), streak=.06, grain=.04, blotch=.06, band=.6, band_pos=-.3, band_w=.3, edge=.14, scratches=6),
    "needle_steel": dict(type="metal", base=(213, 220, 227), hi=(255, 255, 255), streak=.05, grain=.03, blotch=.05, band=.7, band_pos=-.3, band_w=.28, edge=.1, scratches=3),
    "steel_light": dict(type="metal", base=(201, 208, 216), hi=(250, 252, 255), streak=.07, grain=.04, blotch=.06, band=.6, band_pos=-.3, band_w=.3, edge=.18, scratches=6),
    "silk": dict(type="cloth", base=(192, 58, 120), hi=(232, 120, 170)),
    "cord": dict(type="cloth", base=(110, 91, 58), hi=(160, 136, 90)),
    "gold_string": dict(type="metal", base=(220, 200, 140), hi=(255, 244, 200), streak=.04, grain=.02, blotch=.04, band=.5, band_pos=-.3, band_w=.3, edge=.05, scratches=0),
    "bronze": dict(type="metal", base=(176, 122, 46), hi=(224, 176, 96), streak=.10, grain=.07, blotch=.16, band=.45, band_pos=-.32, band_w=.30, edge=.16, scratches=10),
    "rope": dict(type="cloth", base=(201, 178, 122), hi=(240, 224, 176)),
    "silver_bright": dict(type="metal", base=(190, 201, 216), hi=(240, 251, 255), streak=.08, grain=.04, blotch=.07, band=.62, band_pos=-.30, band_w=.30, edge=.16, scratches=8),
    "pearl": dict(type="metal", base=(234, 240, 246), hi=(255, 255, 255), streak=.02, grain=.02, blotch=.03, band=.50, band_pos=-.30, band_w=.30, edge=.04, scratches=0),
    "gold_warm": dict(type="metal", base=(206, 160, 62), hi=(255, 236, 158), streak=.09, grain=.05, blotch=.10, band=.60, band_pos=-.30, band_w=.30, edge=.16, scratches=6),
})


# ================================================================ small helpers used by several weapons
def section_arcs(sec):
    """(points, cumulative arc length at each point, perimeter) of a closed section: where the faces of a swept strip lie on the unrolled skin."""
    P = np.array(list(sec) + [sec[0]], float)
    cum = np.concatenate([[0.0], np.cumsum(np.linalg.norm(np.diff(P, axis=0), axis=1))])
    return P[:-1], cum, float(cum[-1])


def face_range(sec, side=+1, axis=1):
    """The arc interval (start, end) of the flat face of a closed section that looks toward +v (side +1) or -v (side -1)."""
    P, cum, per = section_arcs(sec)
    v = P[:, axis] * side
    idx = [i for i in range(len(P)) if v[i] >= v.max() - 1e-6]
    return float(cum[idx[0]]), float(cum[idx[-1]]), per


# ================================================================ 1. VASSAL MIRROR (envy by design)
# Target (the look written by the logic builder hvd in docs/heroweapon/weapon_mirror.md, read from its worktree): an ornate silver hand mirror held by its
# handle, the glass facing forward like a small shield; an oval, slightly teardrop frame, a fluted handle ending in a round pommel with a small ring and short
# chain hanging from it, a crest of three filigree lobes on the crown that holds the gem in a claw setting, two scrolls running down the sides, a row of pearls
# on the rim. Palette: polished silver, engraved scrollwork in a darker grey, white pearls, a flat bright glass with two diagonal sheen bands, an aquamarine
# gem. Glow: the glass (cold white-cyan, strongest in a diagonal band and at the rim), the gem (full), the pearls (faint); nothing else glows.
# From the series (wiki search summaries): the Vassal Mirror specialises in magical defence and movement, with skills that raise a glass shield, float a mirror and
# reflect all magic, so the glass is the face of the weapon. The shape is the logic builder's design; the engraving, the volutes and the claws are mine.
MIRROR_REAL_M = 1.20           # apparent length in the hand, blocks: the class `shield` is 0.9 to 1.2 (the logic doc asks for about 1.3 with the handle)
MIRROR_GRIP_AT = 0.25          # the hand holds a quarter of the way up the whole mesh (the sprite's hand point, display_presets' first-person anchor): that is on the fluted grip, y 5.8 to 23.6
M_HEAD_Y = 51.0                # the centre of the oval
M_FR_A, M_FR_B = 11.2, 16.4    # the semi-axes of the frame's centre line: the head is 26 cm across and 38 tall with the rim (about 0.55 x 0.8 blocks beside a 1.3 block weapon)
M_RIM_W, M_RIM_T = 3.4, 2.0    # the rim moulding: width across, thickness through
M_EGG, M_WAIST = 0.08, 0.10    # the outline: wider at the top than at the neck
M_GLASS_IN = 1.45              # the glass reaches this far in from the frame's centre line (it tucks under the rim)
M_GEM = (3.2, 4.2, 2.0)        # the crown gem's semi-axes (across, along y, through z): about 0.1 blocks tall in the hand
M_GEM_RGB = (79, 216, 232)     # aquamarine, #4FD8E8 of the logic doc
M_GEM_GLOW = (127, 242, 255)   # #7FF2FF
M_TARNISH = (141, 152, 165)    # the tarnish in the grooves of the handle, #8D98A5
M_N = 40                       # points of every frame outline (rim, bezel, glass, plate): few enough for the quad budget, smooth with the smoothed normals
M_BEADS = 12                   # pearls on the rim
M_FLUTES = 8                   # flutes of the handle


def mirror_outline(scale_a=1.0, scale_b=1.0, n=44, grow=0.0):
    return k.oval_outline(M_FR_A * scale_a + grow, M_FR_B * scale_b + grow, n=n, cy=M_HEAD_Y, egg=M_EGG, waist=M_WAIST)


def build_mirror():
    m = ml.Mesh("vassal_mirror")
    rim_sec = ml.octagon(M_RIM_W, M_RIM_T, 0.62)
    # ---- the handle: a round pommel with the ring and chain, the fluted grip, the collar with the flared neck
    pm = k.part(m, "pommel", "silver_bright", smooth=60, uv_mode="loft", wear=0.35)
    ml.lathe(pm, "y", (0, 0, 0), [(0.0, 0.0), (0.5, 1.2), (1.6, 2.15), (3.0, 2.35), (4.2, 1.85), (5.2, 1.3), (6.0, 1.15)], sides=14)
    gr = k.part(m, "grip", "silver_bright", smooth=45, uv_mode="loft", wear=0.4)
    flute = w1.scallop(1.0, M_FLUTES, 0.16, pts=M_FLUTES * 3)
    ml.loft(gr, "y", (0, 0, 0), [(5.8, 1.15, 1.15), (7.4, 1.5, 1.5), (14.5, 1.7, 1.7), (22.0, 1.5, 1.5), (23.6, 1.2, 1.2)], flute)
    co = k.part(m, "collar", "silver_bright", smooth=55, uv_mode="loft", wear=0.35)
    st = [(23.2, 1.35, 1.35), (23.8, 2.3, 2.3), (25.2, 2.6, 2.6), (26.6, 2.3, 2.3), (27.4, 1.7, 1.7),                       # the ferrule
          (28.6, 2.1, 1.6), (30.4, 3.1, 1.3), (32.2, 4.4, 1.15), (33.6, 5.0, 1.05)]                                         # the neck flares and flattens into the frame
    ml.loft(co, "y", (0, 0, 0), st, ml.circle(1.0, 14))
    cl = k.part(m, "chain_loop", "silver_bright", smooth=60, uv_mode="loft", wear=0.4)
    k.sweep_loop(cl, [(0.0, -1.2 - 1.4 - 1.4 * math.cos(a), 1.4 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 8, endpoint=False)], ml.circle(0.34, 4), up=(1, 0, 0))
    for i in range(2):                                                                # two small links turned alternately, ending in a bead
        y = -5.8 - 2.6 * i
        k.chain_link(cl, (0.0, y, 0.0), (0, 1, 0), (1, 0, 0) if i % 2 == 0 else (0, 0, 1), 3.2, 2.0, 0.3, sides=4, n_arc=3)
    k.ball(cl, (0.0, -11.6, 0.0), 0.9, sides=6, stacks=4)
    # ---- the frame round the glass
    rim = k.part(m, "head_rim", "silver_bright", smooth=42, uv_mode="loft", wear=0.4)
    k.sweep_loop(rim, mirror_outline(n=M_N), rim_sec, up=(0, 0, 1))
    bz = k.part(m, "head_bezel", "silver_bright", smooth=70, uv_mode="loft", wear=0.3)
    bez = [(x, y, 1.0) for (x, y, _z) in mirror_outline(1.0 - 1.65 / M_FR_A, 1.0 - 1.65 / M_FR_B, n=M_N)]
    k.sweep_loop(bz, bez, ml.circle(0.42, 4), up=(0, 0, 1))
    bd = k.part(m, "head_beads", "pearl", smooth=70, wear=0.1, glow=False)
    for p in k.arc_points(k.closed_loop(mirror_outline(n=64)), M_BEADS + 1)[:-1]:
        ml.loft(bd, (0, 0, 1), (p[0], p[1], 0.8), [(0.0, 1.0, 1.0), (0.62, 0.8, 0.8)], ml.circle(0.86, 5), caps=(False, True))
    # ---- the glass (a very shallow dome set below the rim) and the back plate
    gl = k.part(m, "head_glass", "silver_bright", smooth=30, wear=0.1)
    gout = [(x, y - M_HEAD_Y) for (x, y, _z) in mirror_outline(1.0 - M_GLASS_IN / M_FR_A, 1.0 - M_GLASS_IN / M_FR_B, n=M_N)]
    ml.loft(gl, "z", (0, M_HEAD_Y, 0), [(-0.5, 1.0, 1.0), (0.0, 1.0, 1.0), (0.24, 0.82, 0.82), (0.42, 0.5, 0.5), (0.50, 0.0, 0.0)], gout)
    bp = k.part(m, "back_plate", "silver_bright", smooth=40, wear=0.3)
    pout = [(x, y - M_HEAD_Y) for (x, y, _z) in mirror_outline(0.985, 0.985, n=M_N)]
    ml.loft(bp, "z", (0, M_HEAD_Y, 0), [(-1.65, 0.955, 0.955), (-1.45, 1.0, 1.0), (-0.7, 1.0, 1.0)], pout, caps=(True, False))
    bs = k.part(m, "back_boss", "silver_bright", smooth=50, uv_mode="loft", wear=0.3)
    ml.lathe(bs, "z", (0, M_HEAD_Y, -1.62), [(0.0, 0.0), (-0.3, 2.5), (-0.8, 2.1), (-1.15, 1.0), (-1.3, 0.0)], sides=12)
    # ---- the two volutes beside the neck, the crest of three lobes and the gem in its claws
    sc = k.part(m, "scrolls", "silver_bright", smooth=55, uv_mode="loft", wear=0.4)
    for sgn in (1, -1):
        lead = [(3.4, 34.0, 0.5), (5.0, 33.7, 0.55)]
        coil = k.spiral_points((9.4, 35.8), 3.5, 0.9, 1.2, -164, n=18, z=0.6, ccw=(sgn > 0))
        pts = k.catmull([(sgn * x, y, z) for (x, y, z) in lead] + [(sgn * x, y, z) for (x, y, z) in coil[::2]], per=2)
        ml.sweep(sc, pts, ml.circle(1.0, 4), scales=lambda t: ((1.15 - 0.62 * t) * (1.0 if t < 0.97 else 0.5), (1.15 - 0.62 * t) * 0.85), up=(0, 0, 1))
        side = [mirror_outline(1.0 + 0.30 / M_FR_A, 1.0 + 0.30 / M_FR_B, n=96)[i] for i in range(52, 78)]               # a thin vine along the frame's side
        vine = [(-x if sgn > 0 else x, y, 0.5) for (x, y, _z) in side]
        ml.sweep(sc, vine[::3], ml.circle(1.0, 4), scales=lambda t: ((0.5 - 0.22 * t) if t < 0.97 else 0.0,) * 2, up=(0, 0, 1))
    ct = k.part(m, "crest", "silver_bright", smooth=40, uv_mode="loft", wear=0.3)
    top = M_HEAD_Y + M_FR_B * 0.985
    cy = top + 0.6
    petals = [([(0.0, cy + 2.0), (0.0, cy + 5.5), (0.0, cy + 9.5), (0.0, cy + 13.5)], 4.8, 0.8),
              ([(2.8, cy + 1.4), (5.6, cy + 2.2), (8.0, cy + 5.0), (8.6, cy + 10.0)], 4.0, 0.7),
              ([(-2.8, cy + 1.4), (-5.6, cy + 2.2), (-8.0, cy + 5.0), (-8.6, cy + 10.0)], 4.0, 0.7)]
    for pts2, wd, th in petals:
        path = k.catmull([(x, y, 1.45) for (x, y) in pts2], per=4)
        ml.sweep(ct, path, ml.lozenge(1.0, 1.0), scales=lambda t, wd=wd, th=th: (wd * (math.sin(math.pi * min(1.0, t * 0.92 + 0.08)) ** 0.7) if t < 0.99 else 0.0, th if t < 0.99 else 0.0), up=(0, 0, 1))
    gs = k.part(m, "gem_claws", "silver_bright", smooth=50, uv_mode="loft", wear=0.3)
    ml.loft(gs, "z", (0.0, cy, 0.6), [(0.0, 4.2, 5.2), (1.0, 4.7, 5.8), (2.0, 4.1, 5.0), (3.0, 2.9, 3.6)], ml.circle(1.0, 14), caps=(True, False))
    for a in range(0, 360, 60):                                                       # six claws that hold the cabochon
        ca, sa = math.cos(math.radians(a + 30)), math.sin(math.radians(a + 30))
        base = (4.0 * ca, cy + 5.0 * sa, 2.2)
        k.cone(gs, base, (-0.38 * ca, -0.38 * sa, 1.0), 4.0, 0.6, sides=4, r_tip=0.24, closed_base=False)
    gm = k.part(m, "head_gem", "crystal", glow=True, smooth=40, uv_mode="loft", base=M_GEM_RGB)
    k.dome(gm, (0.0, cy, 2.95), M_GEM[0] * 0.92, M_GEM[1] * 0.92, M_GEM[2], sides=12, stacks=8)
    atlas = k.lay_out(m)
    paint_mirror(m, atlas)
    return m, atlas


def paint_mirror(m, atlas):
    rim_sec = ml.octagon(M_RIM_W, M_RIM_T, 0.62)
    f0, f1, _per = face_range(rim_sec, +1)

    def p_rim(c):
        front = k.band(c.A, f0, f1, 0.05)
        mid = (f0 + f1) / 2.0
        k.engrave(c, k.band(c.A, f0 + 0.32, f0 + 0.46, 0.04) + k.band(c.A, f1 - 0.46, f1 - 0.32, 0.04), 0.5, 0.5)          # two cut lines along the moulding
        for ph in (0.0, math.pi):                                                  # an engraved scroll: two interleaved waves
            yw = mid + 0.55 * np.sin(c.B * 2.0 * math.pi / 3.6 + ph)
            k.engrave(c, k.band(np.abs(c.A - yw), 0.0, 0.13, 0.05) * front, 0.55, 0.4)
        k.noise(c, 14, 3, 0.05)
    w1.paint_part(atlas, m.part("head_rim"), p_rim)
    w1.paint_part(atlas, m.part("head_bezel"), lambda c: k.tone(c, k.band(c.S, 0.2, 0.3, 0.08), 1.2))
    w1.paint_part(atlas, m.part("head_beads"), lambda c: k.glow_on(c, np.ones(c.A.shape, np.float32), (221, 246, 255), 0.22, 0.2))

    def p_glass(c):
        x, y = c.A, c.B - M_HEAD_Y
        r = np.hypot(x / (M_FR_A - M_GLASS_IN), y / (M_FR_B - M_GLASS_IN))
        top = k.smooth01(y, -16, 14)
        lo, hi = np.array([191, 217, 234], np.float32), np.array([232, 246, 255], np.float32)                  # #BFD9EA to #E8F6FF
        base = lo[None, None, :] * (1 - top)[..., None] + hi[None, None, :] * top[..., None]
        edge = k.smooth01(r, 0.62, 1.0)
        base = base * (1.0 - 0.30 * edge)[..., None] + np.array([150, 190, 220], np.float32)[None, None, :] * (0.30 * edge)[..., None]
        c.img[..., :3] = base
        c.img[..., 3] = 255
        d = (x - y) / math.sqrt(2.0)                                            # across the diagonal that runs from the upper left to the lower right
        sheen = np.clip(k.band(d, -9.2, -6.0, 0.9) * (0.9 - 0.3 * k.smooth01(r, 0.7, 1.0)) + k.band(d, -3.6, -2.5, 0.45) * 0.8 + k.band(d, 3.5, 4.1, 0.35) * 0.5, 0, 1)
        k.mix_to(c, sheen, (255, 255, 255), 0.82)
        k.mix_to(c, k.smooth01(r, 0.90, 0.93) * (1 - k.smooth01(r, 0.97, 1.0)), (240, 250, 255), 0.9)
        for (sx, sy, sz) in ((-5.0, 8.0, 1.0), (4.5, -6.5, 0.8), (6.2, 9.0, 0.6)):
            star = np.maximum(k.seg_mask(x, y, (sx - sz, sy), (sx + sz, sy), 0.14, 0.07), k.seg_mask(x, y, (sx, sy - sz * 1.4), (sx, sy + sz * 1.4), 0.14, 0.07))
            k.mix_to(c, star, (255, 255, 255), 1.0)
            k.glow_on(c, star, (191, 239, 255), 1.0, 0.5)
        k.glow_on(c, sheen, (191, 239, 255), 0.40, 0.4)                                                          # #BFEFFF: strongest in the diagonal band
        k.glow_on(c, k.smooth01(r, 0.80, 0.93) * (1 - k.smooth01(r, 0.95, 1.0)), (191, 239, 255), 0.30, 0.3)     # and at the rim
    w1.paint_part(atlas, m.part("head_glass"), p_glass)

    def p_back(c):
        x, y = c.A, c.B - M_HEAD_Y
        r = np.hypot(x / M_FR_A, y / M_FR_B)
        for r0 in (0.9, 0.62):
            k.engrave(c, k.band(r, r0, r0 + 0.02, 0.006), 0.5, 0.4)
        pts = [(8.4 * math.sin(math.radians(a)), 8.4 * 1.28 * math.cos(math.radians(a))) for a in range(0, 360, 60)]
        star = np.zeros(c.A.shape, np.float32)
        for i in range(6):
            star = np.maximum(star, k.seg_mask(x, y, pts[i], pts[(i + 2) % 6], 0.34, 0.1))
        k.engrave(c, np.maximum(star, k.band(r, 0.505, 0.545, 0.012)), 0.5, 0.4)
        k.noise(c, 18, 5, 0.06)
    w1.paint_part(atlas, m.part("back_plate"), p_back)
    w1.paint_part(atlas, m.part("head_gem"), lambda c: k.gem_cabochon(c, M_GEM_RGB, hi=(0.25, 0.4), glow_color=M_GEM_GLOW, glow=0.55))
    w1.paint_part(atlas, m.part("back_boss"), lambda c: k.engrave(c, k.stripes(c.B, 0.8, 0.14, 0.2, 0.05), 0.4, 0.3))
    w1.paint_part(atlas, m.part("scrolls"), lambda c: (k.engrave(c, k.band(c.S, 0.225, 0.275, 0.025), 0.5, 0.4), k.noise(c, 14, 7, 0.05)))
    w1.paint_part(atlas, m.part("crest"), lambda c: k.engrave(c, k.band(c.S, 0.235, 0.265, 0.02), 0.5, 0.4))

    def p_grip(c):                                                               # the grooves of the flutes are tarnished
        groove = k.band(np.mod(c.S * M_FLUTES, 1.0), 0.28, 0.72, 0.14)
        k.mix_to(c, groove, M_TARNISH, 0.85)
        k.engrave(c, k.band(np.mod(c.S * M_FLUTES, 1.0), 0.46, 0.54, 0.05), 0.35, 0.2)
        k.noise(c, 12, 2, 0.05)
    w1.paint_part(atlas, m.part("grip"), p_grip)

    def p_collar(c):
        k.engrave(c, k.stripes(c.B, 2.0, 0.16, 0.0, 0.06) * k.band(c.B, 0.0, 10.0, 0.1), 0.45, 0.4)
        k.engrave(c, k.band(np.mod(c.A, 1.6), 0.2, 1.4, 0.15) * k.band(c.B, 1.9, 3.1, 0.1), 0.35, 0.3)
        k.mix_to(c, k.band(c.B, 1.0, 1.2, 0.05), M_TARNISH, 0.8)
    w1.paint_part(atlas, m.part("collar"), p_collar)

    def p_pommel(c):
        k.engrave(c, k.stripes(c.B, 0.9, 0.12, 0.0, 0.04) * k.band(c.B, 3.6, 5.4, 0.05), 0.4, 0.3)
        k.engrave(c, k.band(c.B, 2.55, 3.35, 0.06) * k.stripes(c.A, 1.1, 0.5, 0.0, 0.14), 0.45, 0.3)
        k.mix_to(c, k.band(c.B, 2.3, 2.5, 0.05) + k.band(c.B, 3.4, 3.6, 0.05), M_TARNISH, 0.8)
    w1.paint_part(atlas, m.part("pommel"), p_pommel)
    w1.paint_part(atlas, m.part("gem_claws"), lambda c: k.engrave(c, k.stripes(c.B, 0.9, 0.12, 0.2, 0.05), 0.4, 0.4))


def sprite_rows(rows, pal, outline):
    """A 16 x 16 sprite from sixteen rows of sixteen characters ('.' is empty) and a palette: the hand-built pixel art; the one pixel outline is added."""
    cv = k.new_sprite()
    assert len(rows) == 16, len(rows)
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                cv.put(x, y, pal[ch])
    return cv, outline


def icon_mirror():
    pal = dict(s=(150, 162, 186), S=(214, 222, 236), d=(88, 100, 128), g=(232, 248, 255), G=(176, 218, 238), b=(124, 180, 214), W=(255, 255, 255),
               h=(176, 188, 206), H=(236, 242, 250), j=(110, 122, 146), p=(238, 244, 248), e=(79, 216, 232), E=(206, 250, 255), n=(36, 150, 170))
    return sprite_rows([
        "................",
        "...........pp...",
        ".........SpEep..",
        "........Sggeng..",
        ".......SWggGGd..",
        "......SggWGGbd..",
        ".....SWggGWbbd..",
        ".....SgWGGbWd...",
        ".....SgGWbbbd...",
        ".....SGGbbbd....",
        ".....hHsdddd....",
        "....Hh..........",
        "...hj...........",
        ".dHj............",
        ".HSp............",
        "................"], pal, (18, 30, 42))


# ================================================================ 2. VASSAL HARPOON
# Target (the look written by hvd in docs/heroweapon/weapon_harpoon.md): a whaling harpoon about 2.3 blocks long with a tether: a straight weathered shaft with
# three bronze bands and a leather grip wrap, a weighted iron butt cap, an iron socket, a ring-shaped eyelet just below the socket, a broad leaf-shaped head with
# one large swept-back barb on each side, the gem at the neck of the head held by two iron claws, a rope wound in two loose coils on the shaft with its free
# end running from the eyelet and hanging in a loop. Glow: the gem, a thin crooked lightning vein down the head and along both barbs, a faint ring round the
# eyelet. From the series (wiki search summaries): the Vassal Harpoon's skills are Lightning Strike Dragon, Whale Hunting Harpoon (a second harpoon is thrown), Brave
# Fish Blaster, Lightning Shock Harpoon and Crucifixion, which is why the head carries a lightning vein; its power-up method is Magic and Skill Mastery. The shape is
# the logic builder's design; the wave engraving of the socket, the scalloped mouth and the sizes are mine.
HARPOON_REAL_M = 2.40           # apparent length in the hand: class `staff` is 2.0 to 2.6 (a harpoon is shorter than a lance; class spear starts at 2.8)
HARPOON_GRIP_AT = 0.25
H_LEN = 279.0                   # the whole mesh, cm: the point is at this y
H_SHAFT_R = 2.45                # the haft's radius, cm: a 4.9 cm haft is a thick whaling pole
H_Y_GRIP = (32.0, 108.0)        # the leather wrap: centred on a quarter of the length, where the hand is
H_Y_EYELET = 187.0              # the iron eyelet the line runs through, just below the socket
H_Y_SOCKET = (196.0, 238.0)     # the iron socket that takes the haft
H_BANDS = (70.0, 138.0, 190.0)  # the three bronze rings on the shaft (the first two also hide the seams between the three pieces of the haft)
H_GEM = (2.7, 3.7, 1.8)         # the gem's semi-axes (across, along y, through z), cm: 0.045 x 0.064 blocks in the hand (it must sit on a socket 0.08 blocks wide)
H_GEM_RGB = (127, 230, 255)     # #7FE6FF, an electric blue-white cabochon
H_GEM_GLOW = (191, 246, 255)    # #BFF6FF
H_VEIN = (159, 239, 255)        # #9FEFFF, the glowing lightning vein
H_BOLT = [(0.2, 250.6), (1.9, 254.6), (-1.6, 258.4), (1.8, 262.8), (-1.0, 266.8), (0.5, 271.0), (0.0, 275.0)]      # the vein down the middle of the head: (x, y) on its face


def build_harpoon():
    m = ml.Mesh("vassal_harpoon")
    R = H_SHAFT_R
    # ---- the weighted butt cap: an iron ball and a band
    bt = k.part(m, "butt", "iron_dark", smooth=60, uv_mode="loft", wear=0.6)
    ml.lathe(bt, "y", (0, 0, 0), [(0.0, 0.0), (0.6, 1.5), (2.0, 3.0), (4.2, 3.35), (6.4, 3.0), (8.0, 2.4), (9.6, 2.9), (11.4, 2.9), (12.4, R)], sides=12)
    # ---- the haft in three pieces (one skin strip each: a 2 m strip would force a tiny scale on the whole atlas)
    sh = k.part(m, "shaft", "wood", smooth=70, uv_mode="loft", wear=0.55, base=(78, 56, 40))
    cuts = [12.4, H_BANDS[0], H_BANDS[1], 200.0]
    rad = lambda y: R * (0.94 + 0.12 * math.sin(math.pi * min(1.0, y / 210.0)))
    for y0, y1 in zip(cuts[:-1], cuts[1:]):
        ml.lathe(sh, "y", (0, 0, 0), [(y, rad(y)) for y in np.linspace(y0, y1, 5)], sides=10)
    gp = k.part(m, "grip_wrap", "brown_leather", smooth=60, uv_mode="loft", wear=0.5)
    ml.lathe(gp, "y", (0, 0, 0), [(H_Y_GRIP[0], rad(H_Y_GRIP[0]) + 0.1), (H_Y_GRIP[0] + 1.6, 2.95), (H_Y_GRIP[1] - 1.6, 2.95), (H_Y_GRIP[1], rad(H_Y_GRIP[1]) + 0.1)], sides=10)
    bn = k.part(m, "bands", "bronze", smooth=60, uv_mode="loft", wear=0.45)
    for y in H_BANDS:
        ml.collar(bn, y - 1.4, 3.1, 2.8, lip=0.6, sides=10)
    # ---- the line: two loose coils on the haft, the run between them, the run to the eyelet and the loop that hangs from the eyelet
    rp = k.part(m, "rope", "rope", smooth=75, uv_mode="loft", wear=0.4)
    ca = w1.helix(lambda t: R + 0.62, 112.0, 130.0, 2.6, 26, phase=0.0)
    ml.sweep(rp, ca, ml.circle(1.0, 4), scales=lambda t: (0.68, 0.68), caps=(True, True))
    cb = w1.helix(lambda t: R + 0.62, 146.0, 166.0, 2.8, 28, phase=2.2)
    ml.sweep(rp, cb, ml.circle(1.0, 4), scales=lambda t: (0.68, 0.68), caps=(True, True))
    link = k.catmull([ca[-1], (-3.4, 134.0, 3.3), (0.6, 139.0, 3.7), cb[0]], per=4)
    ml.sweep(rp, link, ml.circle(1.0, 4), scales=lambda t: (0.6, 0.6), caps=(False, False))
    run = k.catmull([cb[-1], (-3.6, 171.0, 3.6), (-6.0, 178.0, 2.4), (-8.2, H_Y_EYELET - 3.0, 1.2)], per=4)
    ml.sweep(rp, run, ml.circle(1.0, 4), scales=lambda t: (0.6, 0.6), caps=(False, False))
    loop = k.catmull([(-11.4, H_Y_EYELET - 3.6, 0.4), (-13.8, 172.0, 1.0), (-14.8, 160.0, 1.6), (-12.2, 150.5, 1.4), (-8.6, 153.0, 1.0), (-7.0, 164.0, 0.8), (-7.0, 176.0, 0.5), (-6.4, H_Y_EYELET - 3.4, 0.3)], per=4)
    ml.sweep(rp, loop, ml.circle(1.0, 4), scales=lambda t: (0.62, 0.62), caps=(False, False))
    ey = k.part(m, "eyelet", "iron_dark", smooth=65, uv_mode="loft", wear=0.6)
    ml.collar(ey, H_Y_EYELET - 1.6, 3.3, 3.2, lip=0.6, sides=10)
    ring = [(-8.2 + 5.0 * math.cos(a), H_Y_EYELET + 5.0 * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, 14, endpoint=False)]
    k.sweep_loop(ey, ring, ml.circle(0.95, 5), up=(0, 0, 1))
    # ---- the socket: a long iron cone with three bronze trims, and the neck of the head with the gem in two claws
    so = k.part(m, "socket", "iron_dark", smooth=55, uv_mode="loft", wear=0.45)
    prof = [(196.0, 4.3), (197.8, 4.55), (200.0, 3.7), (205.0, 3.35), (212.0, 3.0), (222.0, 2.4), (230.0, 1.9), (238.0, 1.6)]
    ml.lathe(so, "y", (0, 0, 0), prof, sides=14)
    sb = k.part(m, "socket_bands", "bronze", smooth=60, uv_mode="loft", wear=0.35)
    for y, r in ((201.0, 3.8), (217.0, 2.9), (229.5, 2.2)):
        ml.collar(sb, y, r, 1.5, lip=0.4, sides=12)
    hn = k.part(m, "head_neck", "iron_dark", smooth=55, uv_mode="loft", wear=0.5)
    ml.loft(hn, "y", (0, 0, 0), [(236.5, 1.7, 1.7), (238.6, 3.4, 3.4), (243.0, 4.4, 4.4), (247.0, 3.2, 3.2), (249.0, 1.6, 1.6)], ml.circle(1.0, 12))
    for sgn in (1, -1):                                                           # two iron claws, above and below the gem
        k.cone(hn, (0.0, 243.0 + sgn * 4.4, 4.0), (0.0, -sgn * 0.5, 1.0), 3.2, 0.7, sides=4, r_tip=0.25)
    gm = k.part(m, "gem", "crystal", glow=True, smooth=40, uv_mode="loft", base=H_GEM_RGB)
    k.dome(gm, (0.0, 243.0, 4.2), *H_GEM, sides=10, stacks=7)
    # ---- the head: the leaf of the blade and two large swept-back barbs
    bl = k.part(m, "head_blade", "steel_blue", smooth=28, wear=0.45)
    st = [(248.4, 2.4, 1.0), (251.4, 8.6, 1.6), (255.6, 12.0, 1.85), (261.0, 10.6, 1.75), (267.4, 6.8, 1.35), (273.4, 3.0, 0.9), (H_LEN, 0.0, 0.0)]
    ml.loft(bl, "y", (0, 0, 0), st, ml.lozenge(1.0, 1.0, 0.3))
    bb = k.part(m, "head_barbs", "steel_blue", smooth=40, wear=0.45)
    for sgn in (1, -1):
        path = k.catmull([(sgn * 3.2, 257.6, 0.0), (sgn * 8.4, 256.6, 0.0), (sgn * 12.8, 252.0, 0.0), (sgn * 14.4, 244.0, 0.0)], per=5)
        ml.sweep(bb, path, ml.lozenge(1.0, 1.0, 0.25), scales=lambda t: ((4.2 - 3.9 * t, 1.25 - 0.7 * t) if t < 0.985 else (0.0, 0.0)), up=(0, 0, 1))
    atlas = k.lay_out(m)
    paint_harpoon(m, atlas)
    return m, atlas


def paint_harpoon(m, atlas):
    def p_shaft(c):
        k.noise(c, 24, 11, 0.08)
        grain = k.streaks(c.A.shape, 21, 70.0, 2.2)
        k.mix_to(c, k.smooth01(grain, 0.66, 0.86) * 0.5, (106, 77, 55), 0.7)                        # the lighter grain, #6A4D37
        k.mix_to(c, k.smooth01(k.streaks(c.A.shape, 5, 90.0, 1.6), 0.74, 0.9) * 0.4, (200, 192, 176), 0.4)       # a little salt in the grain
        k.tone(c, k.smooth01(k.streaks(c.A.shape, 33, 50.0, 2.0), 0.7, 0.9), 0.72)                  # dark water stains
    w1.paint_part(atlas, m.part("shaft"), p_shaft)

    def p_grip(c):
        cord = k.stripes(c.A * 1.0 + c.B * 1.35, 1.25, 0.62, 0.0, 0.16)
        k.tone(c, cord, 0.62)
        k.tone(c, np.roll(cord, 1, axis=0) * (1 - cord), 1.22)
        k.noise(c, 10, 5, 0.07)
    w1.paint_part(atlas, m.part("grip_wrap"), p_grip)
    w1.paint_part(atlas, m.part("bands"), lambda c: (k.engrave(c, k.stripes(c.B, 1.2, 0.14, 0.4, 0.05), 0.5, 0.4), k.mix_to(c, k.band(c.B, 0.2, 0.5, 0.1), (224, 176, 96), 0.7)))

    def p_rope(c):
        twist = k.stripes(c.A * 1.2 + c.B * 2.2, 0.8, 0.4, 0.0, 0.14)
        k.tone(c, twist, 0.62)
        k.noise(c, 6, 4, 0.1)
    w1.paint_part(atlas, m.part("rope"), p_rope)

    def p_socket(c):
        y = c.B + H_Y_SOCKET[0]                                              # the model y of each pixel row (the socket is one lathe)
        for lo, hi in ((203.6, 214.4), (219.6, 227.4)):                       # two panels of wave scrollwork in lighter iron
            for off in (0.3, 0.7):
                yw = lo + (hi - lo) * off + 0.55 * np.sin(c.A * 2.0 * math.pi / 3.2 + off * 6.0)
                k.mix_to(c, k.band(np.abs(y - yw), 0.0, 0.2, 0.08) * k.band(y, lo, hi, 0.1), (122, 130, 142), 0.9)
        k.engrave(c, k.stripes(c.A, 1.1, 0.35, 0.0, 0.12) * k.band(y, 196.2, 198.8, 0.1), 0.45, 0.3)                   # scallops on the mouth
        k.noise(c, 12, 8, 0.07)
    w1.paint_part(atlas, m.part("socket"), p_socket)
    w1.paint_part(atlas, m.part("socket_bands"), lambda c: k.engrave(c, k.stripes(c.A, 0.7, 0.1, 0.0, 0.04), 0.45, 0.4))
    w1.paint_part(atlas, m.part("head_neck"), lambda c: (k.engrave(c, k.stripes(c.A, 1.0, 0.16, 0.0, 0.05), 0.4, 0.3), k.noise(c, 10, 2, 0.06)))
    w1.paint_part(atlas, m.part("gem"), lambda c: k.gem_cabochon(c, H_GEM_RGB, hi=(0.25, 0.4), glow_color=H_GEM_GLOW, glow=0.6))
    w1.paint_part(atlas, m.part("eyelet"), lambda c: k.fill_glow(c, H_VEIN, 0.16, 0.1) if c.B.max() > 10.0 else None)               # a faint ring round the eyelet (the long strip is the ring, the short one its collar)

    def p_blade(c):
        x, y = c.A, c.B
        zig = k.poly_mask(x, y, H_BOLT, 0.3, 0.08)
        centre = k.seg_mask(x, y, (0.0, 247.5), (0.0, 277.0), 0.28, 0.08)
        k.mix_to(c, centre * (1 - zig), (94, 114, 133), 0.8)                       # the darker centre line, #5E7285
        k.mix_to(c, zig, (232, 244, 255), 0.95)
        k.glow_on(c, zig, H_VEIN, 1.0, 0.6)
        k.glow_on(c, k.poly_mask(x, y, H_BOLT, 1.2, 0.4) * 0.45, H_VEIN, 0.35, 0.2)
        half = 12.0 * np.clip(1.0 - np.abs(y - 255.4) / 24.0, 0.05, 1.0)
        k.mix_to(c, k.smooth01(np.abs(x) / half, 0.82, 1.0), (221, 233, 242), 0.7)  # the bright honed edge, #DDE9F2
    w1.paint_part(atlas, m.part("head_blade"), p_blade)

    def p_barb(c):
        crook = 0.25 + 0.03 * (4.0 * np.abs(np.mod(c.T * 5.0, 1.0) - 0.5) - 1.0)           # a crooked line along the front ridge of the barb
        v = k.band(np.abs(c.S - crook), 0.0, 0.014, 0.008)
        k.mix_to(c, v, (232, 244, 255), 0.9)
        k.glow_on(c, v, H_VEIN, 0.9, 0.5)
        k.mix_to(c, k.band(c.S, 0.0, 0.08, 0.04) + k.band(c.S, 0.5, 0.58, 0.04), (221, 233, 242), 0.6)
    w1.paint_part(atlas, m.part("head_barbs"), p_barb)


def icon_harpoon():
    pal = dict(w=(96, 70, 50), W=(138, 102, 72), r=(214, 192, 138), R=(168, 142, 96), i=(70, 76, 92), I=(128, 138, 160), B=(176, 112, 50),
               s=(166, 186, 206), S=(236, 246, 255), c=(98, 126, 160), d=(60, 84, 120), g=(70, 200, 255), G=(210, 245, 255), y=(250, 238, 150))
    return sprite_rows([
        "................",
        ".............cS.",
        ".........I..sGc.",
        "........c.Isys..",
        "..........GSsd..",
        ".........gIicc..",
        ".........Ii.I...",
        "........Ww..I...",
        ".......Ww.......",
        "......WrR.......",
        ".....BB.r.......",
        "....Wwr.R.......",
        "...Ww.Rr........",
        ".iWw............",
        ".Ii.............",
        "................"], pal, (14, 20, 34))


# ================================================================ 3. VASSAL CHAIN (sloth by design)
# Target (the look written by hvd in docs/heroweapon/weapon_chain.md): a heavy war chain without the sickle: a leather-wrapped hand grip with a heavy pommel and a
# short leather strap, a forged clasp (ring and hook) that houses the slate-blue gem, fifteen big links turned alternately that fall in a slow S-curve forward and
# DOWN (mesh -x is down in the third-person hand), a swivel, and a weight: a thick bar ending in a ball with four studs. Palette: dull steel with bright wear on
# the outer curves and rust-brown patches, dark iron weight, blackened clasp, brown leather. Glow: the gem (full) and a very faint pulse along the first three
# links, slow, as if the weapon were heavy with sleep. From the series (wiki search summaries): the Vassal Chain comes from S'yne's world and its skills bind and
# incapacitate targets, defend with chains, teleport, spin the chain into a ring and wear it as armour. The shape is the logic builder's design; the proportions of the
# links and the sag are mine.
CHAIN_REAL_M = 1.75            # apparent length along the weapon's axis: class `whip` is 1.6 to 2.4 (the chain falls away from that axis, so its path is about 2.4 blocks)
CHAIN_GRIP_AT = 0.25
C_LINK_L, C_LINK_W, C_WIRE = 12.4, 7.4, 1.6      # the centre line of a link: length, width, and the wire's radius, cm (a heavy 15 cm link, a ship's chain)
C_PITCH = C_LINK_L - 2 * C_WIRE                  # interlocking: the next link's wire passes through this link's hole
C_LINKS = 15
C_PATH = [(0.0, 56.0), (0.6, 72.0), (0.2, 92.0), (-3.0, 112.0), (-11.0, 130.0), (-25.0, 143.0), (-45.0, 151.0), (-66.0, 153.0)]       # the chain's centre line: out along +y, then down toward -x
C_GEM = (3.1, 3.8, 1.8)                          # the gem's semi-axes (across, along y, through z), cm: 0.062 x 0.078 blocks in the hand, in an iron bezel on the clasp
C_GEM_RGB = (127, 163, 199)                      # #7FA3C7, slate blue
C_GEM_GLOW = (191, 216, 238)                     # #BFD8EE
C_LINK_GLOW = (143, 176, 204)                    # #8FB0CC, the faint glow of the first three links


def build_chain():
    m = ml.Mesh("vassal_chain")
    # ---- the hand grip: a heavy pommel, the leather-wrapped grip (the wrap is painted), a short strap
    gc = k.part(m, "grip_cap", "iron_black", smooth=60, uv_mode="loft", wear=0.6)
    ml.lathe(gc, "y", (0, 0, 0), [(-5.0, 0.0), (-4.4, 2.0), (-2.4, 3.9), (0.6, 4.2), (2.6, 3.4), (3.6, 2.8)], sides=12)
    gr = k.part(m, "grip", "dark_leather", smooth=60, uv_mode="loft", wear=0.6)
    ml.lathe(gr, "y", (0, 0, 0), [(3.2, 2.9), (6.0, 3.15), (14.0, 3.35), (30.0, 3.15), (40.0, 3.0), (46.0, 3.2)], sides=12)
    sp = k.part(m, "strap", "dark_leather", smooth=50, uv_mode="loft", wear=0.6)
    loop = k.catmull([(-3.0, 8.0, 0.0), (-8.6, 5.0, 0.0), (-13.6, 10.0, 0.0), (-12.0, 19.0, 0.0), (-6.4, 24.0, 0.0), (-3.0, 22.0, 0.0)], per=3)
    ml.sweep(sp, loop, ml.rounded_rect(1.8, 0.6, 0.2, 1), caps=(True, True), up=(0, 0, 1))
    # ---- the clasp: a blackened collar, the ring the first link runs through, a hook, the gem in an iron bezel
    cl = k.part(m, "clasp", "iron_black", smooth=55, uv_mode="loft", wear=0.55)
    ml.lathe(cl, "y", (0, 0, 0), [(45.0, 3.2), (46.5, 5.0), (49.0, 5.8), (52.0, 5.0), (54.0, 3.4), (56.0, 2.2)], sides=12)
    arm = ml.rounded_rect(2.0, 2.8, 0.6, 2)
    for sx in (-1, 1):                                                                # the clevis: two forged ears the first link hangs between
        ml.loft(cl, "y", (sx * 3.7, 0, 0), [(52.5, 1.0, 1.0), (59.2, 1.0, 1.0), (60.8, 0.72, 0.85), (61.7, 0.36, 0.5)], arm)
    ml.lathe(cl, "x", (0, 57.0, 0), [(-5.0, 0.9), (-4.4, 1.5), (-3.8, 0.95), (3.8, 0.95), (4.4, 1.5), (5.0, 0.9)], sides=8)       # the pin through the link, with two heads
    k.sweep_loop(cl, [(-6.4, 57.0 + 2.6 * math.cos(a), 2.6 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 10, endpoint=False)], ml.circle(0.85, 4), up=(1, 0, 0))   # the ring on the pin's head
    hook = k.catmull([(3.7, 60.4, 0.0), (5.6, 63.4, 0.0), (3.0, 66.0, 0.0), (0.4, 64.2, 0.0), (0.3, 62.0, 0.0)], per=4)               # the hook that closes over the pin
    ml.sweep(cl, hook, ml.circle(1.0, 5), scales=lambda t: ((1.3 - 0.8 * t, 1.3 - 0.8 * t) if t < 0.97 else (0.0, 0.0)), up=(0, 0, 1))
    bz = k.part(m, "clasp_bezel", "iron_black", smooth=50, uv_mode="loft", wear=0.5)
    ml.loft(bz, "z", (0.0, 49.5, 4.0), [(0.0, 3.9, 4.6), (0.9, 4.3, 5.1), (1.9, 3.6, 4.3), (2.6, 2.8, 3.4)], ml.circle(1.0, 12), caps=(True, False))
    gm = k.part(m, "gem", "crystal", glow=True, smooth=40, uv_mode="loft", base=C_GEM_RGB)
    k.dome(gm, (0.0, 49.5, 5.8), *C_GEM, sides=10, stacks=7)
    # ---- the links: fifteen, turned alternately, along the S-curve
    path = k.catmull([(x, y, 0.0) for (x, y) in C_PATH], per=10)
    lk = k.part(m, "links", "dull_steel", smooth=70, uv_mode="loft", wear=0.7)
    s0 = 3.5
    zax = V((0.0, 0.0, 1.0))
    for i in range(C_LINKS):
        c, t = k.at_arc(path, s0 + i * C_PITCH)
        t = V((t[0], t[1], 0.0))
        n_flat = unit(np.cross(zax, t))                                                    # the in-plane direction across the chain
        n = zax if i % 2 == 0 else n_flat
        k.chain_link(lk, c, t, n, C_LINK_L, C_LINK_W, C_WIRE, sides=5, n_arc=3)
    # ---- the swivel and the weight at the end: a thick bar, a ball with four studs
    e, te = k.at_arc(path, s0 + (C_LINKS - 1) * C_PITCH + C_LINK_L * 0.5 + 3.5)
    te = unit(V((te[0], te[1], 0.0)))
    sw = k.part(m, "swivel", "iron_black", smooth=55, uv_mode="loft", wear=0.55)
    ml.loft(sw, tuple(te), tuple(e), [(0.0, 2.4, 2.4), (1.6, 3.2, 3.2), (5.2, 3.2, 3.2), (6.8, 2.4, 2.4), (8.0, 1.8, 1.8)], ml.circle(1.0, 10))
    wt = k.part(m, "weight", "iron_dark", smooth=55, uv_mode="loft", wear=0.65)
    bar0 = e + te * 8.0
    ml.loft(wt, tuple(te), tuple(bar0), [(0.0, 3.0, 3.0), (3.0, 4.0, 4.0), (14.0, 4.2, 4.2), (16.0, 3.6, 3.6)], ml.circle(1.0, 10))
    ball_c = bar0 + te * (16.0 + 8.8)
    k.ball(wt, tuple(ball_c), 9.4, sides=12, stacks=8)
    sk = k.part(m, "spikes", "iron_stud", smooth=40, uv_mode="loft", wear=0.4)
    side = unit(np.cross(zax, te))
    for ang in (45, 135, 225, 315):
        d = side * math.cos(math.radians(ang)) + zax * math.sin(math.radians(ang))
        k.cone(sk, tuple(ball_c + d * 7.6), tuple(d), 7.4, 2.7, sides=5, r_tip=0.35)
    k.cone(sk, tuple(ball_c + te * 8.2), tuple(te), 7.8, 2.8, sides=5, r_tip=0.35)
    atlas = k.lay_out(m)
    paint_chain(m, atlas)
    return m, atlas


def paint_chain(m, atlas):
    rust = (107, 78, 59)                  # #6B4E3B
    wear = (197, 205, 211)                # #C5CDD3
    seen = {"n": 0}                       # paint_part calls the painter once per link, in the order the links were made

    def p_links(c):
        n = seen["n"]
        seen["n"] += 1
        k.mix_to(c, k.smooth01(w1.noise2(c.A.shape, 7, 17 + n), 0.62, 0.8) * 0.8, rust, 0.75)                       # rust-brown patches, different on every link
        k.mix_to(c, k.band(c.S, 0.2, 0.3, 0.1) * k.smooth01(w1.noise2(c.A.shape, 5, 29 + n), 0.35, 0.7), wear, 0.8)  # bright wear on the outer curve
        k.tone(c, k.smooth01(w1.noise2(c.A.shape, 9, 3 + n), 0.7, 0.9), 0.7)                                          # soot
        if n < 3:                                                                                                       # the first three links nearest the clasp breathe faintly
            k.fill_glow(c, C_LINK_GLOW, 0.2, 0.15)
    w1.paint_part(atlas, m.part("links"), p_links)

    def p_grip(c):
        wrap = k.stripes(c.A * 0.9 + c.B * 1.5, 1.7, 0.5, 0.0, 0.14)
        k.mix_to(c, wrap, (106, 83, 64), 0.9)                                                                          # the wrap lines, #6A5340
        k.tone(c, np.roll(wrap, 1, axis=0) * (1 - wrap), 0.7)
        k.noise(c, 8, 6, 0.07)
    w1.paint_part(atlas, m.part("grip"), p_grip)
    w1.paint_part(atlas, m.part("strap"), lambda c: k.noise(c, 6, 4, 0.08))
    w1.paint_part(atlas, m.part("gem"), lambda c: k.gem_cabochon(c, C_GEM_RGB, hi=(0.25, 0.4), glow_color=C_GEM_GLOW, glow=0.55, body=0.7))
    for nm in ("clasp", "clasp_bezel", "swivel", "weight", "grip_cap", "spikes"):
        w1.paint_part(atlas, m.part(nm), lambda c: (k.noise(c, 10, 5, 0.08), k.engrave(c, k.stripes(c.B, 1.6, 0.12, 0.3, 0.05) * 0.5, 0.3, 0.2)))


def icon_chain():
    pal = dict(b=(104, 80, 56), B=(150, 118, 84), D=(60, 48, 38), i=(46, 50, 58), I=(110, 118, 132), s=(124, 135, 145), S=(205, 212, 218), u=(127, 163, 199),
               k=(56, 62, 72), K=(112, 120, 134))
    return sprite_rows([
        "................",
        "............I...",
        "...........Kk...",
        "..........KKikI.",
        ".........Ikkkk..",
        ".........s.kk...",
        ".........S......",
        "........s.......",
        ".......iS.......",
        "......ii........",
        ".....Iu.........",
        "....Bb..........",
        "...Bb...........",
        "..Bb............",
        ".iD.............",
        "................"], pal, (16, 18, 26))


# ================================================================ 4. VASSAL MUSICAL INSTRUMENT (a violin with its bow)
# Target (the look written by hvd in docs/heroweapon/weapon_instrument.md): a full-size violin held by the neck with the scroll up and forward, the bow laid across
# the strings diagonally with its tip forward; real proportions (two bouts and a waist, arched plates over ribs, f-holes, bridge, tailpiece, chin rest, scroll, a
# pegbox with four pegs, four strings), a thin gold line of purfling round the top plate, a rose-coral gem in the eye of the scroll set in gold claws. Palette:
# maple-amber varnish shading to dark brown at the edges, ebony fittings, pale gold strings, a maple bridge, a brown bow stick with white hair. Glow: the gem
# (full), the f-holes (a thin warm line), three tiny note inlays on the top plate, the strings (very faint); the varnish does not glow. From the series (wiki
# search summaries): a violin is one of the Vassal Musical Instrument's forms; its wielders were Hidemasa Miyaji and then Itsuki Kawasumi. The choice of the violin
# and the proportions are the logic builder's; the scroll modelling, the figured maple and the note inlays are mine. The violin is built in real centimetres with the
# scroll and pegbox 1.3 times larger than life so the gem can be seen.
INSTRUMENT_REAL_M = 1.15       # apparent length in the hand, blocks (violin and bow): class `wand` is 0.8 to 1.2 (the doc: a violin of about 0.9 with its bow, about 1.0 in all)
INSTRUMENT_HAND_AT = 0.58      # the hand holds the neck where it leaves the body, about half way along: the body hangs behind the hand, the scroll and the bow tip lead
V_GEM_RGB = (255, 122, 200)    # #FF7AC8, a rose-coral cabochon: a lens 5.4 cm long (x) and 3.6 cm across, 0.084 x 0.055 blocks in the hand (the profile is in build_instrument)
V_GEM_GLOW = (255, 154, 216)   # #FF9AD8
V_BODY = [(0.0, 0.0), (5.0, 0.35), (8.9, 2.4), (10.25, 7.0), (9.4, 12.0), (6.8, 15.2), (5.6, 17.8), (6.4, 20.4), (8.1, 24.0), (8.35, 28.6), (7.0, 32.8), (4.0, 35.0), (0.0, 35.5)]
V_BODY_LEN = 35.5              # the body of a full-size violin, cm; the right half is listed above, the left is its mirror
V_BRIDGE_Y, V_NUT_Y = 16.0, 48.5   # a 32.5 cm string length
V_FB_END_Y = 21.5              # the fingerboard is 27 cm long
V_RIB = 3.3                    # height of the ribs
V_STRING_X = {"bridge": (-1.65, -0.55, 0.55, 1.65), "nut": (-1.05, -0.35, 0.35, 1.05)}
V_BOW_FROG = (6.4, 3.0, 5.9)   # the bow lies over the strings: the frog at the lower right, the tip at the upper left (forward is -x)
V_BOW_TIP = (-14.5, 70.0, 5.9)
V_SCROLL = (60.2, 0.4)         # the centre of the scroll's curl (y, z): its eye
V_NOTES = [(-3.2, 22.0), (3.4, 27.5), (-1.6, 30.2)]      # three tiny note inlays on the upper bout (x, y)


def violin_outline(n):
    right = V_BODY
    left = [(-x, y) for (x, y) in reversed(V_BODY[1:-1])]
    dense = k.catmull([(x, y, 0.0) for (x, y) in right + left], per=10, closed=True)
    return [(p[0], p[1]) for p in k.arc_points(dense + [dense[0]], n + 1)[:-1]]


def build_instrument():
    m = ml.Mesh("vassal_instrument")
    NB = 36
    out = violin_outline(NB)
    yc = V_BODY_LEN / 2.0
    sec = [(x, y - yc) for (x, y) in out]
    # ---- the body: the arched top plate, the arched back plate and the ribs between them
    tp = k.part(m, "body_top", "spruce", smooth=40, wear=0.2)
    ml.loft(tp, "z", (0, yc, 0), [(0.0, 1.0, 1.0), (0.7, 0.92, 0.92), (1.2, 0.76, 0.76), (1.52, 0.46, 0.46), (1.62, 0.0, 0.0)], sec, caps=(False, False))
    bk = k.part(m, "body_back", "maple_back", smooth=40, wear=0.2)
    ml.loft(bk, "z", (0, yc, 0), [(-3.3, 1.0, 1.0), (-4.0, 0.92, 0.92), (-4.45, 0.74, 0.74), (-4.74, 0.42, 0.42), (-4.82, 0.0, 0.0)], sec, caps=(False, False))
    rb = k.part(m, "ribs", "maple_back", smooth=55, uv_mode="loft", wear=0.2)
    k.sweep_loop(rb, [(x, y, -V_RIB / 2.0) for (x, y) in out], ml.rect_ring(0.34, V_RIB), up=(0, 0, 1))
    # ---- the neck, the pegbox and the scroll in one piece of maple (the pegbox and the scroll are 1.3 times life size so the eye of the scroll can carry the gem)
    nk = k.part(m, "neck_scroll", "maple_neck", smooth=60, uv_mode="loft", wear=0.25)
    ml.loft(nk, "y", (0, 0, 0), [(31.0, 2.1, 1.15, 0, 1.2), (36.0, 1.55, 1.08, 0, 1.6), (44.0, 1.3, 1.02, 0, 1.9), (48.5, 1.3, 1.0, 0, 1.95)], ml.circle(1.0, 10))
    ml.loft(nk, "y", (0, 0, 0), [(48.5, 1.5, 1.1, 0, 2.2), (52.5, 1.8, 1.3, 0, 2.4), (56.8, 1.95, 1.45, 0, 2.5)], ml.rounded_rect(1.0, 1.0, 0.3, 2))
    cy, cz = V_SCROLL
    path = []
    for t in np.linspace(0.0, 1.0, 20):
        a = math.radians(160.0 - 470.0 * t)                       # clockwise in (y, z): over the top and round to the back
        r = 4.8 - 3.0 * t
        path.append((0.0, cy + r * math.cos(a), cz + r * math.sin(a)))
    sp = k.catmull([(0.0, 55.2, 2.5), (0.0, 56.6, 2.5)] + path, per=2)
    ml.sweep(nk, sp, ml.rounded_rect(1.0, 1.0, 0.32, 2), scales=lambda t: (2.5 - 1.3 * t, 3.0 - 0.4 * t), caps=(False, True), up=(1, 0, 0))
    fb = k.part(m, "fingerboard", "ebony", smooth=30, wear=0.25)
    ml.loft(fb, "y", (0, 0, 0), [(V_FB_END_Y, 2.15, 0.36, 0, 3.05), (30.0, 1.9, 0.36, 0, 3.12), (40.0, 1.5, 0.36, 0, 3.2), (V_NUT_Y, 1.25, 0.36, 0, 3.3)], ml.rect_ring(1.0, 1.0))
    ml.box(fb, (-1.3, V_NUT_Y - 0.2, 3.2), (1.3, V_NUT_Y + 0.35, 3.85))
    # ---- the pegs, two on each side, alternately
    pg = k.part(m, "pegs", "ebony", smooth=50, uv_mode="loft", wear=0.25)
    for i, y in enumerate((50.0, 51.9, 53.8, 55.4)):
        sgn = 1 if i % 2 == 0 else -1
        ml.lathe(pg, "x", (0, y, 2.45), [(-sgn * 2.0, 0.26), (sgn * 1.9, 0.26), (sgn * 2.3, 0.42), (sgn * 2.4, 0.72), (sgn * 2.9, 0.68), (sgn * 3.0, 0.0)], sides=6)
    # ---- the four strings from the tailpiece over the bridge to the nut
    st = k.part(m, "strings", "gold_string", smooth=70, uv_mode="loft", wear=0.1)
    for xb, xn in zip(V_STRING_X["bridge"], V_STRING_X["nut"]):
        pts = [(xb * 0.55, 3.6, 2.0), (xb, V_BRIDGE_Y, 4.95), (lerp_x(xb, xn, 0.5), 32.0, 4.3), (xn, V_NUT_Y, 3.7)]
        ml.sweep(st, k.catmull(pts, per=3), ml.circle(0.2, 3), caps=(False, False), up=(0, 0, 1))
    # ---- the bridge, the tailpiece, the chin rest, the end button: maple and ebony fittings
    br = k.part(m, "bridge", "maple_bridge", smooth=35, wear=0.15)
    prof = [(-2.1, 1.35), (-1.55, 1.35), (-1.2, 1.9), (-1.05, 2.5), (-1.5, 3.1), (-1.75, 4.0), (-1.2, 4.75), (-0.6, 4.95), (0.6, 4.95), (1.2, 4.75), (1.75, 4.0), (1.5, 3.1), (1.05, 2.5), (1.2, 1.9), (1.55, 1.35), (2.1, 1.35), (1.4, 2.0), (-1.4, 2.0)]
    ml.loft(br, "y", (0, V_BRIDGE_Y, 0), [(-0.11, 1.0, 1.0), (0.11, 1.0, 1.0)], prof)
    ft = k.part(m, "fittings", "ebony", smooth=45, wear=0.25)
    ml.loft(ft, "y", (0, 0, 0), [(1.8, 0.85, 0.4, 0, 1.95), (4.0, 1.25, 0.42, 0, 2.0), (8.0, 1.85, 0.42, 0, 2.05), (11.2, 2.1, 0.4, 0, 2.1)], ml.circle(1.0, 10))
    k.dome(ft, (-6.3, 6.4, 2.2), 3.0, 2.3, 1.2, sides=10, stacks=6)
    ml.lathe(ft, "y", (0, 0, 0), [(-1.2, 0.0), (-1.0, 0.8), (0.2, 0.95), (0.9, 0.5)], sides=8)
    # ---- the gem in the eye of the scroll, in two gold rings with claws (one on each side of the scroll)
    gm = k.part(m, "gem", "crystal", glow=True, smooth=40, uv_mode="loft", base=V_GEM_RGB)
    ml.lathe(gm, "x", (0, cy, cz), [(-2.7, 0.0), (-2.3, 0.95), (-1.6, 1.5), (-0.7, 1.78), (0.7, 1.78), (1.6, 1.5), (2.3, 0.95), (2.7, 0.0)], sides=10)
    gc = k.part(m, "gold_fittings", "gold_warm", smooth=50, uv_mode="loft", wear=0.3)
    for sgn in (1, -1):
        k.sweep_loop(gc, [(sgn * 1.55, cy + 1.95 * math.cos(a), cz + 1.95 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 10, endpoint=False)], ml.circle(0.34, 4), up=(1, 0, 0))
        for a in (45, 135, 225, 315):
            ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
            k.cone(gc, (sgn * 1.5, cy + 1.95 * ca, cz + 1.95 * sa), (sgn * 0.8, -0.5 * ca, -0.5 * sa), 1.5, 0.3, sides=4, r_tip=0.12)
    # ---- the bow laid across the strings, tip forward
    F, T = V_BOW_FROG, V_BOW_TIP
    d = unit(V(T) - V(F))
    length = float(np.linalg.norm(V(T) - V(F)))
    side = unit(np.cross(d, (0.0, 0.0, 1.0)))
    stick_pts = [tuple(V(F) + d * (length * t) + V((0, 0, 1)) * (1.0 * math.sin(math.pi * t))) for t in np.linspace(0.0, 1.0, 14)]
    bs = k.part(m, "bow_stick", "bow_wood", smooth=70, uv_mode="loft", wear=0.3)
    ml.sweep(bs, stick_pts, ml.circle(1.0, 6), scales=lambda t: (0.46 - 0.15 * t, 0.46 - 0.15 * t), caps=(True, True))
    k.dome(bs, tuple(V(T) + d * 0.6 - V((0, 0, 0.6))), 1.1, 2.4, 1.0, sides=8, stacks=5)
    bh = k.part(m, "bow_hair", "hair", smooth=30, uv_mode="loft", wear=0.1)
    off = side * 1.3                                                          # the bow is tilted a little so the hair shows beside the stick
    h0 = V(F) + d * 4.8 - V((0, 0, 2.7)) + off
    h1 = V(T) - V((0, 0, 1.8)) + d * 0.2 + off
    ml.loft(bh, tuple(unit(h1 - h0)), tuple(h0), [(0.0, 0.95, 1.0), (float(np.linalg.norm(h1 - h0)), 0.95, 1.0)], ml.lozenge(1.0, 0.16), up=(0, 0, 1))
    bf = k.part(m, "bow_frog", "ebony", smooth=45, uv_mode="loft", wear=0.25)
    ml.loft(bf, tuple(d), tuple(V(F) - d * 0.6 - V((0, 0, 1.5))), [(0.0, 0.55, 1.6), (1.5, 0.62, 1.7), (5.2, 0.62, 1.7), (6.0, 0.5, 1.4)], ml.rounded_rect(1.0, 1.0, 0.3, 2), up=(0, 0, 1))
    ml.lathe(bf, tuple(-d), tuple(V(F) - d * 0.9), [(0.0, 0.5), (1.0, 0.6), (1.2, 0.0)], sides=8)
    ml.collar(gc, 0.0, 0.78, 0.55, lip=0.12, sides=8, axis=tuple(d), origin=tuple(V(F) + d * 5.7 - V((0, 0, 0.3))))        # the gold ferrule at the front of the frog
    ml.collar(gc, 0.0, 0.66, 0.5, lip=0.1, sides=8, axis=tuple(d), origin=tuple(V(F) - d * 0.5 - V((0, 0, 0.2))))        # and the gold ring of the screw button
    atlas = k.lay_out(m)
    paint_instrument(m, atlas)
    return m, atlas


def lerp_x(a, b, t):
    return a + (b - a) * t


def violin_fholes():
    """The two f-holes as (polyline, eye discs): the right one, the left is its mirror. Positions are on the top plate, in cm."""
    stem = [(5.0, 19.7), (4.6, 18.1), (4.1, 16.4), (3.85, 14.6), (4.1, 12.9), (4.8, 11.6)]
    eyes = [(5.15, 20.0, 0.5), (4.95, 11.4, 0.6)]
    both = []
    for sgn in (1, -1):
        both.append(([(sgn * x, y) for (x, y) in stem], [(sgn * x, y, r) for (x, y, r) in eyes]))
    return both


def paint_instrument(m, atlas):
    out = violin_outline(36)
    varnish_dark, purfling = (58, 28, 10), (227, 179, 65)

    def plate(c, flame):
        if c.g.key[0] != 2:                                                     # the sloping rim of a plate: just darker
            k.tone(c, np.ones(c.A.shape, np.float32), 0.78)
            return None, None
        x, y = c.A, c.B
        d = k.poly_dist(x, y, out)
        inside = k.poly_inside(x, y, out)
        edge = 1.0 - k.smooth01(d, 0.0, 2.4)
        k.mix_to(c, edge * inside, varnish_dark, 0.62)                           # the varnish is darker toward the edge
        k.mix_to(c, np.exp(-(((x - 0.6) / 6.5) ** 2 + ((y - 26.8) / 5.0) ** 2)) * inside, (236, 178, 90), 0.28)       # a pale gold highlight on the upper bout
        k.mix_to(c, np.exp(-(((x + 0.4) / 7.0) ** 2 + ((y - 7.0) / 4.5) ** 2)) * inside, (236, 178, 90), 0.2)         # and a softer one on the lower bout
        if flame:
            f = 0.5 + 0.5 * np.sin(2.0 * math.pi * (y * 0.66 + 0.4 * np.sin(x * 0.5) + 0.1 * x))
            k.tone(c, k.smooth01(f, 0.55, 0.95) * inside, 0.72)
        k.mix_to(c, k.band(d, 0.38, 0.46, 0.03) + k.band(d, 0.66, 0.74, 0.03), (30, 14, 6), 0.9)              # the dark lines of the purfling ...
        k.mix_to(c, k.band(d, 0.48, 0.64, 0.03), purfling, 0.9 if not flame else 0.0)                          # ... and its thin gold inlay on the top plate
        return x, y

    def p_top(c):
        x, y = plate(c, False)
        if x is None:
            return
        fm = np.zeros(c.A.shape, np.float32)
        for stem, eyes in violin_fholes():
            fm = np.maximum(fm, k.poly_mask(x, y, stem, 0.34, 0.07))
            for (ex, ey, er) in eyes:
                fm = np.maximum(fm, k.disc_mask(x, y, ex, ey, er, 0.08))
        k.mix_to(c, fm, (24, 12, 6), 0.96)
        k.glow_on(c, np.clip(k.blur(fm, 1.5) * 1.9 - fm * 1.2, 0, 1), (255, 179, 71), 0.85, 0.3)               # a thin warm line round each f-hole, #FFB347
        for (nx, ny) in V_NOTES:                                                                                 # three tiny notes: a head, a stem and a flag
            nm = np.maximum(k.disc_mask(x, y, nx, ny, 0.3, 0.07), np.maximum(k.seg_mask(x, y, (nx + 0.27, ny), (nx + 0.27, ny + 1.25), 0.1, 0.05), k.seg_mask(x, y, (nx + 0.27, ny + 1.25), (nx + 0.75, ny + 0.8), 0.1, 0.05)))
            k.mix_to(c, nm, (255, 221, 170), 0.95)
            k.glow_on(c, nm, (255, 221, 170), 0.9, 0.4)
        k.noise(c, 16, 3, 0.04)
    w1.paint_part(atlas, m.part("body_top"), p_top)

    def p_back(c):
        x, y = plate(c, True)
        if x is not None:
            k.noise(c, 12, 4, 0.05)
    w1.paint_part(atlas, m.part("body_back"), p_back)

    def p_ribs(c):
        face = k.band(c.A, 0.0, 3.3, 0.05)
        f = 0.5 + 0.5 * np.sin(2.0 * math.pi * (c.B * 0.34 + c.A * 0.2))
        k.tone(c, k.smooth01(f, 0.6, 0.95) * face, 0.74)
        k.noise(c, 10, 5, 0.05)
    w1.paint_part(atlas, m.part("ribs"), p_ribs)

    def p_neck(c):
        f = 0.5 + 0.5 * np.sin(2.0 * math.pi * (c.B * 0.55 + 0.35 * np.sin(c.A * 1.3)))
        k.tone(c, k.smooth01(f, 0.6, 0.95), 0.78)
        k.noise(c, 8, 6, 0.06)
    w1.paint_part(atlas, m.part("neck_scroll"), p_neck)
    w1.paint_part(atlas, m.part("strings"), lambda c: k.fill_glow(c, (255, 233, 176), 0.10, 0.2))
    w1.paint_part(atlas, m.part("gem"), lambda c: k.gem_lens(c, V_GEM_RGB, glow_color=V_GEM_GLOW, glow=0.6))

    def p_bridge(c):
        z = -c.B if c.g.key[1] >= 0 else c.B
        for (cx, cz, r) in ((0.0, 3.0, 0.55), (-1.3, 2.2, 0.28), (1.3, 2.2, 0.28)):
            k.mix_to(c, k.disc_mask(c.A, z, cx, cz, r, 0.06), (66, 36, 14), 0.9)
    w1.paint_part(atlas, m.part("bridge"), p_bridge)

    def p_stick(c):
        if c.B.max() < 6.0:                                                      # the head of the bow: an ivory face
            k.mix_to(c, np.ones(c.A.shape, np.float32), (236, 228, 208), 0.92)
        else:
            k.mix_to(c, k.band(c.S, 0.0, 0.05, 0.03) + k.band(c.S, 0.5, 0.55, 0.03), (130, 92, 60), 0.6)
        k.noise(c, 10, 8, 0.05)
    w1.paint_part(atlas, m.part("bow_stick"), p_stick)
    w1.paint_part(atlas, m.part("bow_hair"), lambda c: k.noise(c, 4, 9, 0.06))


def icon_instrument():
    pal = dict(a=(184, 99, 31), A=(228, 168, 80), d=(112, 52, 18), e=(34, 24, 18), E=(78, 58, 44), p=(255, 122, 200), P=(255, 208, 236), w=(246, 242, 232), W=(176, 170, 156),
               b=(104, 68, 40), g=(227, 179, 65), f=(36, 18, 8))
    return sprite_rows([
        "................",
        ".............gP.",
        "....W.......dAp.",
        ".....w......Aad.",
        ".....w......e...",
        "......w....e....",
        ".......wa.e.....",
        "......AwaE......",
        "......aawad.....",
        "...AAaaaawd.....",
        "..Afaaaadw......",
        ".AAaaaad..w.....",
        ".Aaaafdd...w....",
        "..aaadd....b....",
        "..aaddd.....b...",
        "................"], pal, (24, 14, 10))


# ================================================================ 5. VASSAL SEWING SET (a needle on a spool)
# Target (the look written by hvd in docs/heroweapon/weapon_sewingset.md): a long steel needle with a large oval eye near its blunt end, set into a dark violet
# thread spool that is the grip (two silver-rimmed flanges, crimson silk wound on the core), a purple cabochon in the centre of the top flange where the needle enters, a
# silk thread that runs out through the eye and trails in one loose S-curve in the air, and a small pair of scissors hanging from the spool's side on a ring and a cord.
# Palette: polished steel, violet lacquered wood with silver rims, crimson silk fading to violet along the trail, brass scissor rings, an amethyst gem. Glow: the gem
# (full), the trailing thread (faint violet, brighter toward its free end), a hair-thin line down the needle. From the series (wiki search summaries): the Vassal Sewing Set
# comes from S'yne's world, has a purple gem that absorbs materials, and has thread skills (Bind Wire, Spider Wire), needle skills (Needle Shot, Jumping Needle) and
# scissor skills (Scissor Cut, Scissor Blaze); the same summaries describe its main form as a giant pair of scissors (and a ball of yarn as another form), which the
# logic builder chose not to build (a needle holds the thread better): the hanging scissors and the spool here are the nod to them. The shape is the logic builder's
# design; the sizes are mine.
SEW_REAL_M = 1.30              # apparent length in the hand, blocks: class `rapier` is 1.0 to 1.3 (the doc: about 1.5 in all with the spool)
SEW_GRIP_AT = 0.25
S_FLANGE_R = 6.5               # flange radius (a 13 cm spool, about 0.15 blocks across)
S_CORE_R, S_WOUND_R = 3.4, 4.6
S_NEEDLE_TOP = 111.0           # the point of the needle, cm: the mesh runs from the thimble pommel at -11.6 up to here; the scissors hang a little below the spool
S_EYE = (36.0, 1.7, 8.3)       # the eye: its centre y and semi-axes (x, y)
S_GEM = (3.9, 3.3, 3.9)        # the gem's semi-axes (x, along y, z), cm: 0.083 x 0.070 x 0.083 blocks in the hand, a dome in the top flange's silver bezel
S_GEM_RGB = (166, 77, 255)     # #A64DFF, an amethyst
S_GEM_GLOW = (215, 168, 255)   # #D7A8FF
S_THREAD_A, S_THREAD_B = (192, 58, 120), (122, 63, 181)       # crimson silk fading to violet: #C03A78 to #7A3FB5
S_THREAD_GLOW = (183, 124, 240)                               # #B77CF0
S_PIVOT = (-29.0, 11.0)        # the pivot of the hanging scissors


def build_sewing_set():
    m = ml.Mesh("vassal_sewing_set")
    # ---- the spool: the core and two flanges in violet lacquered wood, silver rims, the wound silk
    gc = k.part(m, "grip_core", "violet_wood", smooth=45, uv_mode="loft", wear=0.2)
    ml.lathe(gc, "y", (0, 0, 0), [(0.0, 0.0), (0.15, 5.6), (0.6, S_FLANGE_R), (1.5, S_FLANGE_R), (1.95, 5.7), (2.3, S_CORE_R), (21.7, S_CORE_R), (22.05, 5.7), (22.5, S_FLANGE_R),
                                  (23.4, S_FLANGE_R), (23.85, 5.6), (24.0, 3.0)], sides=16)
    gr = k.part(m, "grip_rims", "silver_rim", smooth=55, uv_mode="loft", wear=0.35)
    for y in (0.35, 22.35):
        ml.collar(gr, y, S_FLANGE_R + 0.18, 1.45, lip=0.35, sides=16)
    th0 = k.part(m, "grip_thimble", "silver_rim", smooth=55, uv_mode="loft", wear=0.35)                      # the needle's blunt end comes out of the spool into a thimble: the pommel
    ml.lathe(th0, "y", (0, 0, 0), [(-11.6, 0.0), (-11.2, 2.0), (-9.6, 3.5), (-6.0, 4.4), (-2.4, 4.9), (-0.6, 5.0), (0.2, 3.6)], sides=14)
    gt = k.part(m, "grip_thread", "silk", smooth=75, uv_mode="loft", wear=0.2)
    ml.lathe(gt, "y", (0, 0, 0), [(2.3, S_CORE_R), (2.7, S_WOUND_R - 0.3), (3.4, S_WOUND_R), (20.6, S_WOUND_R), (21.3, S_WOUND_R - 0.3), (21.7, S_CORE_R)], sides=16)
    # ---- the gem in the top flange, in a silver bezel, with the needle through it
    bz = k.part(m, "gem_bezel", "silver_rim", smooth=50, uv_mode="loft", wear=0.3)
    ml.lathe(bz, "y", (0, 0, 0), [(23.9, 4.6), (24.4, 4.9), (25.2, 4.5), (25.8, 4.0)], sides=14)
    gm = k.part(m, "gem", "crystal", glow=True, smooth=40, uv_mode="loft", base=S_GEM_RGB)
    k.dome(gm, (0.0, 26.6, 0.0), *S_GEM, sides=12, stacks=8)
    # ---- the needle: a shank, the large oval eye, the long shaft to the point
    ne = k.part(m, "needle_eye", "needle_steel", smooth=60, uv_mode="loft", wear=0.25)
    ml.loft(ne, "y", (0, 0, 0), [(25.0, 1.6, 1.6), (28.2, 1.2, 1.2), (S_EYE[0] - S_EYE[2] + 0.6, 1.0, 1.0)], ml.circle(1.0, 8))
    eye = [(S_EYE[1] * math.cos(a), S_EYE[0] + S_EYE[2] * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, 16, endpoint=False)]
    k.sweep_loop(ne, eye, ml.circle(0.62, 5), up=(0, 0, 1))
    np_ = k.part(m, "needle_point", "needle_steel", smooth=60, uv_mode="loft", wear=0.25)
    ml.loft(np_, "y", (0, 0, 0), [(S_EYE[0] + S_EYE[2] - 0.6, 1.0, 1.0), (50.0, 1.25, 1.25), (70.0, 1.1, 1.1), (92.0, 0.7, 0.7), (104.0, 0.36, 0.36), (S_NEEDLE_TOP, 0.0, 0.0)], ml.circle(1.0, 8))
    # ---- the thread: from the wound silk up over the gem, through the eye from the front to the back, then out in a loose S in the air. It is ONE spline (two splines
    #      joined end to end kink at the join) and it never turns back down: the run rises to the eye, and behind the eye it keeps clear of the ring and the shaft.
    th = k.part(m, "thread_trail", "silk", smooth=75, uv_mode="loft", wear=0.2)
    eye_in = S_EYE[0] - 3.0                                                                    # where the thread crosses the plane of the eye: in the hole, above the gem
    run = [(S_WOUND_R * 0.7, 21.0, S_WOUND_R * 0.7), (4.0, 25.4, 2.9), (2.9, 28.6, 2.4), (1.2, 30.4, 1.0), (0.0, eye_in, 0.0)]
    trail = [(-0.2, 36.4, -0.9), (-0.5, 40.0, -1.0), (-2.6, 47.0, -1.2), (-8.0, 56.0, 4.6), (-14.0, 66.0, 6.0), (-10.0, 80.0, 2.0), (-4.0, 91.0, -3.4), (-9.0, 101.0, -6.0), (-16.0, 108.0, -5.0)]
    ml.sweep(th, k.catmull(run + trail, per=4), ml.circle(1.0, 4), scales=lambda t: (0.5 - 0.12 * t, 0.5 - 0.12 * t), caps=(True, True))
    # ---- the scissors hang from the spool's side on a ring and a cord (-x is down in the hand)
    cd = k.part(m, "cord", "cord", smooth=70, uv_mode="loft", wear=0.3)
    k.sweep_loop(cd, [(S_WOUND_R + 0.3) * np.array([math.cos(a), 0.0, math.sin(a)]) + np.array([0.0, 21.0, 0.0]) for a in np.linspace(0, 2 * math.pi, 12, endpoint=False)], ml.circle(0.36, 4), up=(0, 1, 0))
    cord = k.catmull([(-S_WOUND_R - 0.3, 21.0, 0.0), (-8.6, 19.4, 0.2), (-13.0, 18.4, 0.2), (-17.4, 17.4, 0.2), (-19.0, 16.3, 0.2)], per=4)
    ml.sweep(cd, cord, ml.circle(1.0, 4), scales=lambda t: (0.42, 0.42), caps=(True, True))
    sc = k.part(m, "scissors", "steel_light", smooth=40, wear=0.35)
    px, py = S_PIVOT
    for sgn in (1, -1):
        z = 0.3 * sgn
        tip = (px - 15.0, py + sgn * 1.8, z)
        k.leaf_plate(sc, (px + 1.0, py, z), unit((tip[0] - px, tip[1] - py, 0.0)), 16.0, 2.5, 0.65, normal=(0, 0, 1), base_w=0.8, widest=0.15, tip_w=0.0)
        shank = k.catmull([(px + 1.0, py, z), (px + 4.6, py + sgn * 1.7, z), (px + 7.6, py + sgn * 3.8, z)], per=3)
        ml.sweep(sc, shank, ml.circle(1.0, 4), scales=lambda t: (0.6, 0.5), caps=(True, True))
        ring = [(px + 10.2 + 2.5 * math.cos(a), py + sgn * 5.2 + 2.5 * math.sin(a), z) for a in np.linspace(0, 2 * math.pi, 10, endpoint=False)]
        k.sweep_loop(sc, ring, ml.circle(0.55, 4), up=(0, 0, 1))
    k.dome(sc, (px, py, 0.0), 0.8, 0.8, 0.8, sides=6, stacks=4)
    atlas = k.lay_out(m)
    paint_sewing_set(m, atlas)
    return m, atlas


def paint_sewing_set(m, atlas):
    def p_core(c):
        k.noise(c, 14, 3, 0.05)
        k.engrave(c, k.stripes(c.A, 1.2, 0.12, 0.0, 0.05) * (k.band(c.B, 0.6, 1.9, 0.05) + k.band(c.B, 22.4, 23.7, 0.05)), 0.4, 0.3)
    w1.paint_part(atlas, m.part("grip_core"), p_core)
    w1.paint_part(atlas, m.part("grip_rims"), lambda c: k.engrave(c, k.stripes(c.A, 0.9, 0.1, 0.0, 0.04), 0.4, 0.3))

    def p_thimble(c):                                                                 # the dimples of a thimble: a staggered grid of little pits
        row = np.floor(c.B / 1.5)
        pits = k.disc_mask(np.mod(c.A + 0.75 * (row % 2), 1.5), np.mod(c.B, 1.5), 0.75, 0.75, 0.3, 0.12) * k.band(c.B, 1.2, 10.0, 0.3)
        k.engrave(c, pits, 0.5, 0.3)
        k.engrave(c, k.band(c.B, 10.6, 11.0, 0.08), 0.45, 0.3)
    w1.paint_part(atlas, m.part("grip_thimble"), p_thimble)

    def p_wound(c):
        k.tone(c, k.stripes(c.B, 0.42, 0.14, 0.0, 0.05), 0.62)                    # the turns of the silk lie side by side
        k.mix_to(c, np.ones(c.A.shape, np.float32), S_THREAD_A, 0.55)
        k.noise(c, 8, 4, 0.05)
    w1.paint_part(atlas, m.part("grip_thread"), p_wound)
    w1.paint_part(atlas, m.part("gem"), lambda c: k.gem_cabochon(c, S_GEM_RGB, hi=(0.25, 0.4), glow_color=S_GEM_GLOW, glow=0.6))
    w1.paint_part(atlas, m.part("gem_bezel"), lambda c: k.engrave(c, k.stripes(c.A, 0.8, 0.1, 0.0, 0.04), 0.4, 0.3))

    def p_needle(c):
        k.noise(c, 20, 6, 0.04)
        k.mix_to(c, k.band(c.S, 0.2, 0.3, 0.05), (232, 216, 255), 0.5)                  # a hair-thin bright line down the needle
        k.glow_on(c, k.band(c.S, 0.24, 0.26, 0.02), (232, 216, 255), 0.5, 0.3)
    w1.paint_part(atlas, m.part("needle_point"), p_needle)
    w1.paint_part(atlas, m.part("needle_eye"), lambda c: (k.noise(c, 14, 6, 0.04), k.glow_on(c, k.band(c.S, 0.24, 0.26, 0.02), (232, 216, 255), 0.5, 0.3)))

    def p_thread(c):
        t = np.clip(c.T, 0, 1)[..., None]
        col = np.array(S_THREAD_A, np.float32) * (1 - t) + np.array(S_THREAD_B, np.float32) * t
        c.img[..., :3] = col * (0.8 + 0.3 * k.smooth01(np.sin(c.A * 9.0 + c.B * 5.0), -1.0, 1.0)[..., None])
        c.img[..., 3] = 255
        k.fill_glow(c, S_THREAD_GLOW, 0.10, 0.3)
        k.glow_on(c, np.clip(c.T, 0, 1), S_THREAD_GLOW, 0.5, 0.35)                            # brighter toward the free end
    w1.paint_part(atlas, m.part("thread_trail"), p_thread)
    w1.paint_part(atlas, m.part("cord"), lambda c: k.tone(c, k.stripes(c.A * 1.0 + c.B * 2.0, 0.5, 0.25, 0.0, 0.08), 0.7))

    w1.paint_part(atlas, m.part("scissors"), lambda c: k.noise(c, 8, 5, 0.06))


def icon_sewing_set():
    pal = dict(n=(205, 212, 222), N=(255, 255, 255), m=(120, 132, 150), r=(192, 58, 120), R=(240, 130, 180), u=(166, 77, 255), U=(226, 190, 255),
               s=(176, 184, 196), S=(240, 244, 250), t=(122, 63, 181), T=(190, 120, 240), d=(120, 28, 76))
    return sprite_rows([
        "................",
        ".........Tt...N.",
        "........t....nm.",
        "........Tt..Nm..",
        "..........rnm...",
        "..........dn....",
        ".........nn.....",
        "........Nm......",
        ".....SuU........",
        "....RSsu........",
        "...Rrrss........",
        "..Rrrdd.........",
        ".Ssrdd..........",
        ".ss.d...........",
        "...s............",
        "................"], pal, (20, 12, 30))


# ================================================================ driver
# The size class of each weapon (an existing class of assets/nusmp/weapon_classes.json; the lead merges classes_hart4.json into the table). Why these:
#   shield    the Mirror is held up like a small shield and works like one (the Vassal Mirror reflects magic); 0.9 to 1.2 blocks
#   staff     the Harpoon is shorter than a lance: 2.0 to 2.6 blocks (the spear / trident classes start at 2.8)
#   whip      the Chain is a long flexible weapon: 1.6 to 2.4 blocks along its axis (its path falls away from the axis, so it is longer still)
#   wand      the Violin and its bow are held like a casting implement: 0.8 to 1.2 blocks
#   rapier    the Needle is a thin pointed weapon: 1.0 to 1.3 blocks
CLASSES = {"vassal_mirror": "shield", "vassal_harpoon": "staff", "vassal_chain": "whip", "vassal_instrument": "wand", "vassal_sewing_set": "rapier"}
BUILDS = {"vassal_mirror": (build_mirror, icon_mirror, MIRROR_REAL_M, MIRROR_GRIP_AT, ("FFFFFF", 0.10, 0.40, 1.0)),
          "vassal_harpoon": (build_harpoon, icon_harpoon, HARPOON_REAL_M, HARPOON_GRIP_AT, ("FFFFFF", 0.22, 0.45, 1.0)),
          "vassal_chain": (build_chain, icon_chain, CHAIN_REAL_M, CHAIN_GRIP_AT, ("FFFFFF", 0.045, 0.45, 1.0)),
          "vassal_instrument": (build_instrument, icon_instrument, INSTRUMENT_REAL_M, INSTRUMENT_HAND_AT, ("FFFFFF", 0.14, 0.40, 1.0)),
          "vassal_sewing_set": (build_sewing_set, icon_sewing_set, SEW_REAL_M, SEW_GRIP_AT, ("FFFFFF", 0.12, 0.40, 1.0))}
# The gem of each weapon: the part that carries it and the colour its glow skin must have (design.md section 11: the gem is on the glow skin, in the weapon's colour).
GEMS = {"vassal_mirror": ("head_gem", M_GEM_GLOW), "vassal_harpoon": ("gem", H_GEM_GLOW), "vassal_chain": ("gem", C_GEM_GLOW), "vassal_instrument": ("gem", V_GEM_GLOW),
        "vassal_sewing_set": ("gem", S_GEM_GLOW)}
MAX_QUADS, MIN_PARTS, MAX_PARTS = 2800, 8, 14          # the budget of design.md section 11 (quad lines of one mesh file, named parts of one weapon)
SKIN_PX = 512
GEM_HUE_TOLERANCE = 25.0                               # degrees: the glow under the gem may be paler than the colour (a hot core) but not another colour
GEM_MIN_ALPHA = 40.0                                   # mean alpha (0..255) of the glow skin under the gem: the gem bodies are deep colours and the light is the glow, strength 0.6


def make(item_id):
    """The sprite, the mesh, both skins and the item models of one weapon; returns the mesh statistics."""
    build, icon, real_m, hand_at, glow = BUILDS[item_id]
    cv, outline = icon()
    icon_png = k.save_sprite(cv, outline, item_id)
    mesh, atlas = build()
    return k.write_weapon(mesh, atlas, item_id, icon_png, real_m, hand_at, glow)


def files_of(item_id):
    """The six files design.md section 11 asks for, relative to assets/nusmp (what HeroAssetsTest looks for)."""
    return ["models/item/%s.json" % item_id, "models/item/%s_icon.json" % item_id, "textures/item/%s.png" % item_id, "weapon_meshes/%s.txt" % item_id,
            "textures/entity/weapon/%s.png" % item_id, "textures/entity/weapon/%s_glow.png" % item_id]


def write_classes():
    """tools/heroweapon/classes_hart4.json: id -> size class of weapon_classes.json, the fragment the lead merges."""
    path = os.path.join(HERE, "classes_hart4.json")
    with open(path, "w", newline="\n", encoding="utf-8") as f:
        json.dump({i: CLASSES[i] for i in IDS}, f, indent=2)
        f.write("\n")
    return path


def audit(ids, sheet=None):
    """The repo's size and orientation audit (tools/relics/audit_weapon_scale.py) for these ids, with this fragment's classes merged in memory (the shared
    class table is the lead's): the table, and with `sheet` the contact sheet of the weapon in the sprite plane and held next to Steve."""
    import weapon_classes as wc
    import audit_weapon_scale as aud
    data = wc.load()
    for i, c in CLASSES.items():
        data["weapons"][i] = {"class": c}
    rows = [r for r in (aud.audit_one(i, data, k.MESH_DIR, k.MODEL_DIR) for i in ids) if r]
    aud.print_table(rows)
    if sheet:
        print("sheet:", aud.make_sheet(rows, sheet, data, k.MESH_DIR))
    return rows


def read_game_mesh(path):
    """A written mesh as the game reads it: (place, glow, quads, parts, plain). quads = [(layer, 4 x (x, y, z, u, v, nx, ny, nz))] from the `qn` lines,
    parts = {name: (index of its first quad, quad count)} from the `# part` comments, plain = the number of old `q` lines without normals (there must be none)."""
    place = glow = None
    quads, parts, plain = [], {}, 0
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v:
                continue
            if v[0] == "place":
                place = [float(x) for x in v[1:6]]
            elif v[0] == "glow":
                glow = v[1:]
            elif v[0] == "#" and len(v) > 4 and v[1] == "part":
                parts[v[2]] = (int(v[3]), int(v[4]))
            elif v[0] == "qn":
                vals = [float(x) for x in v[2:34]]
                quads.append((int(v[1]), [tuple(vals[q * 8:q * 8 + 8]) for q in range(4)]))
            elif v[0] == "q":
                plain += 1
    return place, glow, quads, parts, plain


def hue_of(rgb):
    """The hue in degrees of an 0..255 colour."""
    import colorsys
    return colorsys.rgb_to_hsv(*(np.asarray(rgb, np.float32)[:3] / 255.0))[0] * 360.0


def gem_glow_problem(item_id):
    """None when the gem of the written weapon glows in its colour, else what is wrong: the glow skin is sampled where each glow quad of the gem part lies
    (the quad's UV centre), and the opaque samples must have the gem's hue (to GEM_HUE_TOLERANCE) and be mostly opaque."""
    from PIL import Image
    _place, _glow, quads, parts, _plain = read_game_mesh(os.path.join(k.MESH_DIR, item_id + ".txt"))
    name, rgb = GEMS[item_id]
    if name not in parts:
        return "no part %s" % name
    first, count = parts[name]
    with Image.open(os.path.join(k.SKIN_DIR, item_id + "_glow.png")) as im:
        tex = np.asarray(im.convert("RGBA"), np.float32)
    size = tex.shape[0]
    samples = []
    for layer, verts in quads[first:first + count]:
        if layer == 2:
            u = sum(p[3] for p in verts) / 4.0
            w = sum(p[4] for p in verts) / 4.0
            samples.append(tex[min(size - 1, int(w / 128.0 * size)), min(size - 1, int(u / 128.0 * size))])
    if not samples:
        return "the gem part %s has no glow quads" % name
    px = np.array(samples)
    lit = px[px[:, 3] > 0]
    if len(lit) < 0.8 * len(px) or float(px[:, 3].mean()) < GEM_MIN_ALPHA:
        return "the glow skin is empty under %d of %d gem quads (mean alpha %.0f)" % (len(px) - len(lit), len(px), float(px[:, 3].mean()))
    hue = hue_of((lit[:, :3] * lit[:, 3:4]).sum(axis=0) / lit[:, 3].sum())
    want = hue_of(rgb)
    off = abs(hue - want)
    if min(off, 360.0 - off) > GEM_HUE_TOLERANCE:
        return "the gem glows at hue %.0f, the weapon's colour is hue %.0f" % (hue, want)
    return None


def check(ids):
    """Reads the written files back and returns a list of problems (empty: all good): the six files exist, the mesh parses and stays in its budget, its
    UVs and normals are sane, the skins are 512 px with a glow, the gem glows in its colour, the sprite is 16 x 16, the audit finds nothing, the hand holds the grip end."""
    from PIL import Image
    problems = []
    for i in ids:
        for rel in files_of(i):
            if not os.path.exists(os.path.join(k.ASSETS, rel)):
                problems.append("%s: missing %s" % (i, rel))
        path = os.path.join(k.MESH_DIR, i + ".txt")
        if not os.path.exists(path):
            continue
        place, glow, quads, parts, plain = read_game_mesh(path)
        if plain:
            problems.append("%s: %d quads without per-vertex normals" % (i, plain))
        for layer, verts in quads:
            if any(not (0.0 <= v[3] <= 128.0 and 0.0 <= v[4] <= 128.0) for v in verts):
                problems.append("%s: a uv is outside the skin" % i)
                break
            if any(abs(math.sqrt(v[5] ** 2 + v[6] ** 2 + v[7] ** 2) - 1.0) > 0.02 for v in verts):
                problems.append("%s: a normal is not a unit vector" % i)
                break
        if len(quads) > MAX_QUADS:
            problems.append("%s: %d quads (budget %d)" % (i, len(quads), MAX_QUADS))
        if not MIN_PARTS <= len(parts) <= MAX_PARTS:
            problems.append("%s: %d named parts (budget %d to %d)" % (i, len(parts), MIN_PARTS, MAX_PARTS))
        if place is None or glow is None:
            problems.append("%s: no place or glow line" % i)
        for suffix in ("", "_glow"):
            skin = os.path.join(k.SKIN_DIR, i + suffix + ".png")
            if os.path.exists(skin):
                with Image.open(skin) as im:
                    if im.size != (SKIN_PX, SKIN_PX):
                        problems.append("%s: %s is %s px" % (i, suffix or "skin", im.size))
                    if suffix and im.convert("RGBA").getchannel("A").getextrema()[1] == 0:
                        problems.append("%s: the glow skin is empty" % i)
        if os.path.exists(os.path.join(k.SKIN_DIR, i + "_glow.png")):
            why = gem_glow_problem(i)
            if why:
                problems.append("%s: %s" % (i, why))
        icon = os.path.join(k.ICON_DIR, i + ".png")
        if os.path.exists(icon):
            with Image.open(icon) as im:
                if im.size != (16, 16) or int((np.asarray(im.convert("RGBA"))[..., 3] > 0).sum()) < 20:
                    problems.append("%s: the sprite is not a 16 x 16 picture" % i)
    import weapon_classes as wc
    classes = wc.load()["classes"]
    for i in ids:
        if CLASSES[i] not in classes:
            problems.append("%s: class %s is not in weapon_classes.json" % (i, CLASSES[i]))
    for r in audit(ids):
        if r["status"] or r["flags"] or r["grip_at"] != "low":
            problems.append("%s: audit status %s flags %s grip %s" % (r["id"], r["status"], r["flags"], r["grip_at"]))
    return problems


def previews(ids, outdir):
    """The pictures: four views of each weapon, its back, held in the third-person and first-person hand, the sprites enlarged, the audit sheet."""
    os.makedirs(outdir, exist_ok=True)
    out = []
    zoom = {"vassal_harpoon": 120.0, "vassal_chain": 170.0}
    for i in ids:
        out.append(k.render(i, os.path.join(outdir, i + ".png")))
        out.append(k.render(i, os.path.join(outdir, i + "_back.png"), views=("back", "three_quarter", "edge"), panel=(420, 560)))
        out.append(k.held_view(i, os.path.join(outdir, "held_%s_third.png" % i), "third", zoom=zoom.get(i, 230.0)))
        out.append(k.held_view(i, os.path.join(outdir, "held_%s_first.png" % i), "first"))
    out.append(k.sheet_of_icons(ids, os.path.join(outdir, "sprites.png"), zoom=14))
    audit(ids, os.path.join(outdir, "audit_sheet.png"))
    return out


def main(argv):
    ids = [a for a in argv if not a.startswith("--")] or IDS
    for i in ids:
        if i not in BUILDS:
            raise SystemExit("unknown weapon " + i + "; known: " + ", ".join(IDS))
    if "--check" not in argv:
        for i in ids:
            make(i)
        write_classes()
    out = next((a.split("=", 1)[1] for a in argv if a.startswith("--preview=")), PREVIEW)
    if out:
        previews(ids, out)
    if "--audit" in argv:
        audit(ids)
    if "--check" in argv:
        problems = check(ids)
        print("\n".join(problems) if problems else "check: %d weapons, no problem" % len(ids))
        return 1 if problems else 0
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
