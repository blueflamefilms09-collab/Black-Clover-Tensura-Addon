"""The 3D models of the dungeon loot gear: eight weapons and eight trinkets, built with the owner's model method (tools/relics/mesh_lib.py, the kit of tools/relics/new197_*.py).

    python3 -B tools/gen_weapon_models.py loot [id ...]      (the hook of the weapon generator; this file can also be run directly)
    python3 -B tools/gen_loot_models.py [id ...]             no ids = all sixteen; idempotent and deterministic
    LOOT_PREVIEW=<dir> python3 -B tools/gen_loot_models.py goblin_fang_dagger     also renders a preview sheet of each

For each id it writes (through new197_lib.finish): assets/nusmp/weapon_meshes/<id>.txt (the mesh WeaponRenderer draws), textures/entity/weapon/<id>.png and _glow.png (the
painted skin and the additive glow skin), textures/item/<id>.png (the 16 x 16 inventory sprite rendered from the finished mesh) and models/item/<id>.json plus <id>_icon.json (the
3D hand model and the flat icon). Then it writes the display presets of the long weapons (the oversize rule) and the class rows (weapon_classes.json, both copies).
The weapons follow the Genesis Demon-Slayer's method: real cross-sections (blades are lofted, hafts are lathed), named parts, a painted skin, an additive glow skin and,
where the lore calls for it, a layer of dimensional void. Units are centimetres; `finish` scales each weapon to its class size.
"""
import json
import math
import os
import sys

import numpy as np

_TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(_TOOLS, "relics"))
sys.path.insert(0, os.path.join(_TOOLS, "loot"))
if os.environ.get("LOOT_PREVIEW"):
    os.environ["N197_PREVIEW"] = os.environ["LOOT_PREVIEW"]

from new197_kit import *                                                   # noqa: F401,F403,E402
from new197_kit import (P, plate, blade_curve, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, paint, put_glow, sm, step, darken, grip, pommel,   # noqa: E402
                        guard_cross, guard_wings, guard_disc, guard_horns, guard_ring, collar_ring, straight_sword, spear_head, shaft, paint_edge_glow, paint_center_glow,
                        paint_runes, paint_gradient, paint_glow, paint_checker, shade, new_mesh, new_atlas, lay_and_paint, finish)
from new197_kit2 import extrude, hammer_head, cuboid, void_poly                    # noqa: E402
from new197_god import helix_pts                                                   # noqa: E402
from new197_legend import fuller_pair, glow_parts                                  # noqa: E402
import roster as R                                                                  # noqa: E402

REPO = os.path.abspath(os.path.join(_TOOLS, ".."))                    # (the star imports above bring HERE, ROOT and ASSETS of the kit: these names are this file's own)
GAME_ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")


def finish_item(item_id, m, atlas, cls, **kw):
    return finish(item_id, m, atlas, cls, **kw)


# ================================================================ weapons
def build_goblin_fang_dagger():
    """Goblin-Fang Dagger: a curved yellow fang for a blade, a dark iron guard studded with two small teeth, a green cord grip and a tooth for a pommel; the tip carries a faint green glow."""
    m = new_mesh("goblin_fang_dagger")
    L0, N = 11.0, 14
    pts = [(-5.5 * (i / N) ** 2.0, L0 + 24.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "bone", pts, lambda t: 1.9 * (1 - 0.72 * t ** 1.2), lambda t: 0.55 * (1 - 0.45 * t), bevel=0.6, base=(236, 224, 176), smooth=48)
    guard_cross(m, L0, half=4.4, t=0.9, h=0.9, material="iron", base=(58, 50, 46), bulb=0.8, curve=1.4)
    teeth = P(m, "guard_teeth", "bone", base=(240, 228, 182), smooth=30)
    for sgn in (-1, 1):
        spike(teeth, (sgn * 4.2, L0 + 0.8, 0.0), (sgn * 5.8, L0 + 4.2, 0.0), 0.7, 5)
    grip(m, 2.4, L0 - 0.6, r=1.05, base=(46, 62, 38), wrap_base=(118, 150, 80), n_wraps=6, material="leather")
    tp = P(m, "pommel_tooth", "bone", base=(240, 228, 182), smooth=30)
    spike(tp, (0.0, 2.6, 0.0), (0.0, -2.4, 0.0), 1.5, 6)
    glow_tip = P(m, "fang_glow", "crystal", base=(150, 255, 130), glow=True, smooth=40)
    ml.gem(glow_tip, (-5.2, L0 + 22.0, 0.0), 0.55, 1.6, 5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_gradient(atlas, m.part("blade"), (206, 186, 126), (252, 246, 214), L0, L0 + 24.0, 0.45)
    glow_parts(atlas, m, ["fang_glow"], (150, 255, 130), 0.9, 0.5)
    finish_item("goblin_fang_dagger", m, atlas, "dagger", glow=("A0FF90", 0.12, 0.3, 0.8))


def build_delvers_longsword():
    """Delver's Longsword: the plain blade of a hundred nameless delvers: steel with a long fuller and an old chip in the edge, an iron cross guard, a leather grip and a disc pommel."""
    m = new_mesh("delvers_longsword")
    L0 = 22.0
    st = [(L0, -3.1, 3.1, 0.6), (L0 + 10, -3.2, 3.2, 0.62), (56.0, -3.0, 3.0, 0.56), (56.0, -2.5, 3.0, 0.56), (59.0, -2.5, 3.0, 0.56), (59.0, -3.0, 3.0, 0.56), (80.0, -2.8, 2.8, 0.52),
          (94.0, -2.2, 2.2, 0.46), (102.0, -1.3, 1.3, 0.36), (107.0, -0.6, 0.6, 0.26), (110.0, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=8.0, t=1.4, h=1.4, bulb=1.2, curve=1.0), grip_len=21.0, pommel_style="disc", pommel_kw=dict(r=3.0),
                         blade_material="steel", blade_base=(178, 186, 198), guard_material="iron", guard_base=(96, 86, 78), grip_base=(62, 44, 34), wrap_base=(124, 90, 58), bevel=0.5)
    fuller_pair(m, "fuller", L0 + 10, 92.0, 1.1, base=(150, 158, 172), material="steel")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_gradient(atlas, m.part("blade"), (150, 158, 170), (224, 232, 242), L0, 108.0, 0.35)
    glow_parts(atlas, m, ["fuller_f", "fuller_b"], (255, 226, 160), 0.5, 0.3)
    finish_item("delvers_longsword", m, atlas, "longsword", glow=("FFE2A0", 0.1, 0.0, 0.25))


def build_bone_lance():
    """Bone Lance: a wooden shaft bound with pale cord, a socket and a long barbed head of grave bone, and a small soul crystal that glows cold blue at the base of the point."""
    m = new_mesh("bone_lance")
    Y_S = 150.0
    shaft(m, 0.0, Y_S, r=1.7, material="wood", base=(88, 66, 46), name="shaft", sides=10)
    wrap(m, "grip_wraps", 30.0, 84.0, 1.8, 10, "leather", (206, 190, 140))
    lathe_y(m, "butt", "bone", -5.0, [(0.0, 0.0), (1.6, 1.8), (5.0, 2.2)], sides=10, base=(232, 222, 190), caps=(False, True))
    lathe_y(m, "socket", "bone", Y_S - 6.0, [(0.0, 2.2), (3.0, 3.1), (8.0, 2.7), (15.0, 1.6)], sides=12, base=(232, 222, 190))
    out = spear_head(m, Y_S + 9.0, 58.0, 5.6, t=1.05, material="bone", base=(238, 230, 204), leaf=0.4, name="head", bevel=0.55)
    spurs = P(m, "spurs", "bone", base=(228, 218, 186), smooth=30)
    for sgn in (-1, 1):
        spike(spurs, (sgn * 2.5, Y_S + 3.0, 0.0), (sgn * 8.0, Y_S + 13.0, 0.0), 0.95, 5)
    crystal(m, "soul_gem", "crystal", (0.0, Y_S + 10.0, 2.6), 1.3, 3.4, base=(150, 220, 255), glow=True)
    fuller_pair(m, "head_fuller", Y_S + 16.0, Y_S + 56.0, 1.3, base=(200, 190, 160), material="bone", w_=0.3, h=0.1, zf=1.05, zt=0.5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    head = m.part("head")
    paint_center_glow(atlas, head, out, (170, 225, 255), 0.4, Y_S + 17.0, Y_S + 60.0, 0.7, 0.45)
    glow_parts(atlas, m, ["soul_gem"], (150, 220, 255), 1.0, 0.6)
    finish_item("bone_lance", m, atlas, "spear", glow=("BFE8FF", 0.12, 0.35, 0.9))


def build_orc_cleaver():
    """Orc Cleaver: a heavy single-bit axe of dark pitted iron with a bearded edge and chipped notches, a long wrapped haft, iron bands and a curved orc tusk lashed on the back of the head."""
    m = new_mesh("orc_cleaver")
    YC = 112.0
    shaft(m, 0.0, YC + 12.0, r=1.9, material="wood", base=(72, 52, 38), name="haft", sides=10)
    wrap(m, "grip_wraps", 6.0, 46.0, 1.9, 8, "leather", (112, 82, 54))
    for y in (60.0, 96.0, YC - 6.0):
        collar_ring(m, y, 2.5, 1.8, "iron", (84, 80, 82), name="band_%d" % int(y))
    lathe_y(m, "butt", "iron", -4.0, [(0.0, 0.0), (1.6, 1.8), (4.0, 2.4)], sides=10, base=(84, 80, 82), caps=(False, True))
    pts = [(2.2, YC - 10.0), (-2.0, YC - 13.0), (-9.0, YC - 17.0), (-12.0, YC - 15.0), (-14.5, YC - 17.5), (-18.5, YC - 13.0), (-19.5, YC - 6.0), (-17.0, YC - 2.0), (-20.0, YC + 3.0),
           (-18.0, YC + 8.0), (-19.0, YC + 11.0), (-13.0, YC + 15.0), (-4.0, YC + 12.0), (2.2, YC + 9.0)]
    head = extrude(m, "head", "iron", pts, 1.5, edge=0.7, base=(100, 94, 94), smooth=38)
    tube(m, "tusk", "bone", [(2.0, YC, 0.0), (6.0, YC + 1.0, 0.0), (10.0, YC + 4.0, 0.0), (12.5, YC + 9.0, 0.0), (12.5, YC + 14.0, 0.0)], 1.6, 0.2, sides=7, tip=True, base=(234, 224, 192))
    lash = P(m, "lashing", "leather", base=(150, 40, 36), smooth=50)
    for dy in (-3.0, -1.0, 1.0, 3.0):
        ml.collar(lash, YC + dy, 2.3, 0.7, lip=0.2, sides=10)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def edge(pc):
        if not pc.face:
            return
        m_ = np.clip((-pc.X - 9.0) / 9.0, 0, 1)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.3 * m_[..., None]) + np.array([210, 220, 232], np.float32) * 0.3 * m_[..., None]
        put_glow(pc, np.clip((-pc.X - 16.0) / 3.0, 0, 1) * 0.6, (255, 90, 60), 0.5, 0.4)
    paint(atlas, head, edge)
    finish_item("orc_cleaver", m, atlas, "battleaxe", glow=("FF6A4A", 0.1, 0.25, 0.7))


def build_shadowsteel_katana():
    """Shadowsteel Katana: a long dark blade folded with a shard of shadow: a violet hamon of light along the edge and a thin ribbon of void down the flat, a dark tsuba, a violet-wrapped grip and an imp horn for a pommel."""
    m = new_mesh("shadowsteel_katana")
    L0, N = 26.0, 20
    pts = [(-8.0 * (i / N) ** 2.2, L0 + 84.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "obsidian", pts, lambda t: 2.0 * (1.0 - 0.5 * t ** 1.5), lambda t: 0.5 * (1.0 - 0.4 * t), bevel=0.6, base=(44, 40, 62), smooth=48)
    guard_disc(m, L0 - 0.5, r=4.8, thick=0.7, material="iron", base=(42, 38, 54))
    gems = P(m, "tsuba_gems", "crystal", base=(160, 100, 255), glow=True, smooth=40)
    for k in range(6):
        a = 2 * math.pi * k / 6
        ml.gem(gems, (4.0 * math.cos(a), L0 + 0.2, 4.0 * math.sin(a)), 0.5, 1.0, 5)
    collar_ring(m, L0 - 2.4, 1.9, 2.0, "silver", (150, 130, 210), name="habaki", sides=10)
    grip(m, 3.4, L0 - 2.6, r=1.6, base=(30, 26, 42), wrap_base=(122, 76, 200), n_wraps=11, material="cloth", sides=10, swell=1.1)
    horn = P(m, "pommel", "bone", base=(210, 70, 70), smooth=35)
    spike(horn, (0.0, 3.6, 0.0), (0.0, -3.8, 0.9), 1.7, 7)
    left = [(pts[i][0] + 0.1, pts[i][1]) for i in range(2, N - 2)]
    right = [(pts[i][0] + 0.9, pts[i][1]) for i in range(2, N - 2)]
    void_poly(m, "void", right + left[::-1], 0.58)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def hamon(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 84.0, 0, 1)
        cx = -8.0 * t ** 2.2
        wave = 0.45 * np.sin(pc.Y * 0.4) * (1 - 0.3 * t)
        d = pc.X - (cx - 1.0 + wave)
        mk = sm(d, 0.55)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.35 * np.clip(sm(d, 1.8), 0, 1)[..., None]) + np.array([130, 90, 220], np.float32) * 0.35 * np.clip(sm(d, 1.8), 0, 1)[..., None]
        put_glow(pc, mk * (pc.Y > L0 + 4), (150, 100, 255), 0.9, 0.5)
    paint(atlas, m.part("blade"), hamon)
    glow_parts(atlas, m, ["tsuba_gems"], (170, 120, 255), 0.9, 0.5)
    finish_item("shadowsteel_katana", m, atlas, "katana", glow=("A060FF", 0.15, 0.4, 1.0))


def build_drakefang_greatsword():
    """Drakefang Greatsword: a slab of steel with a serrated edge and rows of drake scale, a guard of two curved fangs, a red-wrapped two-handed grip and a spiked pommel; the fuller smoulders like drake fire."""
    m = new_mesh("drakefang_greatsword")
    L0 = 36.0
    st = [(L0, -5.0, 5.0, 0.9)]
    y = L0 + 8.0
    while y < 140.0:
        st += [(y, -5.4, 5.2, 0.95), (y + 0.8, -2.9, 5.2, 0.95), (y + 3.2, -5.4, 5.2, 0.95)]
        y += 12.0
    st += [(146.0, -4.8, 4.8, 0.85), (158.0, -3.2, 3.4, 0.62), (168.0, -1.3, 1.3, 0.36), (172.0, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="horns", guard_kw=dict(span=11.0, rise=8.0, t=1.3), grip_len=30.0, pommel_style="spike", pommel_kw=dict(r=2.6),
                         blade_material="steel", blade_base=(152, 158, 170), guard_material="bone", guard_base=(234, 224, 188), grip_base=(60, 36, 34), wrap_base=(176, 60, 44), bevel=0.45, ricasso=4.0)
    fuller_pair(m, "fuller", L0 + 8.0, 150.0, 1.8, base=(255, 130, 60), material="iron", w_=0.5, h=0.2, zf=0.95, zt=0.9)
    plates = P(m, "scale_plates", "crystal", base=(64, 150, 92), smooth=38)
    for k in range(5):
        yy = L0 + 14.0 + k * 9.0
        for sgn in (-1, 1):
            ml.bar(plates, (sgn * 2.2, yy, 1.0), (sgn * 2.2, yy + 6.0, 1.0), 2.6, 0.3, up=(0, 0, 1), taper=1.5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_checker(atlas, blade, (128, 136, 148), (168, 176, 188), cell=2.2, k=0.35)
    paint_center_glow(atlas, blade, out, (255, 120, 50), 0.7, L0 + 8.0, 148.0, 0.55, 0.4)
    glow_parts(atlas, m, ["fuller_f", "fuller_b"], (255, 130, 60), 0.85, 0.45)
    finish_item("drakefang_greatsword", m, atlas, "greatsword", glow=("FF8030", 0.12, 0.3, 0.9))


def build_wisp_staff():
    """Wisp Staff: a tall staff of pale wood wound with a green vine, four curved prongs cradling a glowing spirit wisp, bronze collars and a wrapped grip."""
    m = new_mesh("wisp_staff")
    YT = 190.0
    shaft(m, 0.0, YT - 8.0, r=1.5, material="wood", base=(150, 124, 86), name="shaft", sides=10)
    tube(m, "vine", "wood", helix_pts(2.0, 40.0, YT - 24.0, 7.0, 130), 0.55, 0.55, sides=5, base=(88, 132, 70), smooth=70)
    wrap(m, "grip_wraps", 10.0, 72.0, 1.5, 10, "leather", (84, 112, 72))
    for y in (36.0, 76.0, YT - 18.0):
        collar_ring(m, y, 2.4, 2.0, "bronze", (196, 148, 80), name="collar_%d" % int(y))
    lathe_y(m, "butt", "bronze", -5.0, [(0.0, 0.0), (1.6, 1.8), (5.0, 2.2)], sides=10, base=(196, 148, 80), caps=(False, True))
    for k in range(4):
        a = math.pi / 2 * k + 0.4
        c, s = math.cos(a), math.sin(a)
        pts = [(2.4 * c, YT - 16.0, 2.4 * s), (4.2 * c, YT - 6.0, 4.2 * s), (5.6 * c, YT + 6.0, 5.6 * s), (4.6 * c, YT + 16.0, 4.6 * s), (1.8 * c, YT + 24.0, 1.8 * s)]
        tube(m, "prong_%d" % k, "wood", pts, 0.95, 0.25, sides=6, tip=True, base=(168, 138, 96))
    orb(m, "wisp", "crystal", (0.0, YT + 5.0, 0.0), 4.6, base=(120, 255, 170), glow=True)
    orb(m, "wisp_core", "crystal", (0.0, YT + 5.0, 0.0), 2.1, base=(240, 255, 245), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["wisp", "wisp_core"], (130, 255, 180), 1.0, 0.6)
    glow_parts(atlas, m, ["vine"], (150, 255, 140), 0.25, 0.3)
    finish_item("wisp_staff", m, atlas, "staff", glow=("90FFC0", 0.2, 0.4, 1.0))


def build_golem_maul():
    """Golem Maul: a chamfered stone head on an iron haft, banded in iron at both faces, with a glowing golem core set in the middle and orange rune lines down each side."""
    m = new_mesh("golem_maul")
    YH = 120.0
    shaft(m, 0.0, YH + 4.0, r=1.8, material="iron", base=(72, 74, 82), name="haft", sides=10)
    wrap(m, "grip_wraps", 8.0, 48.0, 1.8, 8, "leather", (106, 82, 60))
    lathe_y(m, "butt", "iron", -4.0, [(0.0, 0.0), (1.6, 1.8), (4.0, 2.4)], sides=10, base=(72, 74, 82), caps=(False, True))
    hammer_head(m, YH, w_=30.0, h=19.0, d=17.0, material="stone", base=(124, 120, 128), name="head")
    for sgn in (-1, 1):
        bnd = P(m, "band_%s" % ("l" if sgn < 0 else "r"), "iron", base=(66, 68, 76), smooth=30)
        ml.box(bnd, (sgn * 12.4 - 1.2, YH - 9.9, -8.9), (sgn * 12.4 + 1.2, YH + 9.9, 8.9), 0.4)
    orb(m, "core", "crystal", (0.0, YH, 8.3), 3.2, base=(255, 150, 60), glow=True, rz=1.4)
    for i, (x0, y0, x1, y1) in enumerate([(-9.0, -5.0, -4.5, 3.0), (4.5, -3.0, 9.0, 5.0), (-9.0, 4.0, -5.0, 7.0), (5.0, -7.0, 9.0, -4.0)]):
        bar(m, "rune_%d" % i, "crystal", (x0, YH + y0, 8.6), (x1, YH + y1, 8.6), 0.8, 0.3, up=(0, 0, 1), base=(255, 140, 50), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["core"] + ["rune_%d" % i for i in range(4)], (255, 150, 60), 0.95, 0.55)
    finish_item("golem_maul", m, atlas, "hammer", glow=("FF9A40", 0.14, 0.4, 1.0))


# ================================================================ trinkets (small upright objects, centred on their sprite)
def ring_pts(cx, cy, r, n=24, z=0.0):
    return [(cx + r * math.cos(a), cy + r * math.sin(a), z) for a in np.linspace(0, 2 * math.pi, n + 1)]


def build_goblin_ear_ward():
    """Goblin Ear Ward: three green goblin ears hanging from a leather cord loop, with an iron nugget between them."""
    m = new_mesh("goblin_ear_ward")
    tube(m, "cord", "leather", ring_pts(0.0, 15.0, 5.0, 20), 0.5, 0.5, sides=5, base=(86, 60, 40))
    for i, x in enumerate((-4.2, 0.0, 4.2)):
        orb(m, "ear_%d" % i, "leather", (x, 6.0 - (0.0 if i == 1 else 1.6), 0.0), 1.0, base=(108, 164, 76), glow=False, rx=1.5, ry=3.2, rz=0.6)
    orb(m, "nugget", "iron", (0.0, 10.0, 0.4), 1.4, base=(110, 112, 120), glow=True)
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["nugget"], (150, 255, 130), 0.55, 0.35)
    finish_item("goblin_ear_ward", m, atlas, "handitem", real_m=0.5, fit=True, glow=("A0FF90", 0.1, 0.2, 0.5))


def build_orc_tusk_ring():
    """Orc Tusk Ring: an iron band with a curved ivory tusk set upright on its bezel."""
    m = new_mesh("orc_tusk_ring")
    tube(m, "band", "iron", ring_pts(0.0, 5.0, 5.0, 28), 1.2, 1.2, sides=7, base=(92, 92, 100))
    tube(m, "tusk", "bone", [(0.0, 9.5, 0.0), (1.5, 11.5, 0.0), (3.0, 14.5, 0.0), (2.6, 18.0, 0.0)], 1.5, 0.15, sides=7, tip=True, base=(236, 226, 194))
    collar_ring(m, 10.0, 1.8, 1.2, "iron", (92, 92, 100), name="bezel")
    ember = P(m, "ember_stud", "crystal", base=(255, 110, 70), glow=True, smooth=40)
    ml.gem(ember, (-5.0, 5.0, 0.85), 0.9, 1.3, 5)
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["ember_stud"], (255, 110, 70), 0.8, 0.5)
    finish_item("orc_tusk_ring", m, atlas, "handitem", real_m=0.5, fit=True, glow=("FF8A60", 0.1, 0.0, 0.2))


def build_lich_dust_pendant():
    """Lich Dust Pendant: a silver chain loop and a teardrop of glass full of violet dust that glows faintly."""
    m = new_mesh("lich_dust_pendant")
    tube(m, "chain", "silver", ring_pts(0.0, 13.0, 6.0, 26), 0.4, 0.4, sides=5, base=(200, 204, 214))
    collar_ring(m, 6.5, 1.2, 1.0, "silver", (200, 204, 214), name="cap")
    drop = P(m, "glass", "crystal", base=(190, 170, 240), glow=True, smooth=40)
    ml.gem(drop, (0.0, 3.2, 0.0), 2.4, 1.7, 8, "y")
    dust = P(m, "dust", "crystal", base=(150, 110, 230), glow=True, smooth=40)
    ml.gem(dust, (0.0, 3.0, 0.0), 1.3, 1.5, 6, "y")
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["glass", "dust"], (170, 130, 240), 0.9, 0.5)
    finish_item("lich_dust_pendant", m, atlas, "handitem", real_m=0.5, fit=True, glow=("B090FF", 0.12, 0.3, 0.8))


def build_wisp_lantern():
    """Wisp Lantern: a small iron cage of four bars round a drifting green wisp, a cap and a hanging ring."""
    m = new_mesh("wisp_lantern")
    tube(m, "ring", "iron", ring_pts(0.0, 17.0, 2.0, 16), 0.45, 0.45, sides=5, base=(70, 72, 80))
    lathe_y(m, "cap", "iron", 12.0, [(0.0, 3.6), (1.6, 3.4), (3.6, 1.0)], sides=8, base=(70, 72, 80))
    lathe_y(m, "foot", "iron", 0.0, [(0.0, 2.4), (1.4, 3.0)], sides=8, base=(70, 72, 80), caps=(True, True))
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        rod(m, "bar_%d" % k, "iron", 1.0, 12.0, 0.45, 0.45, sides=5, base=(70, 72, 80), x=3.0 * math.cos(a), z=3.0 * math.sin(a))
    orb(m, "wisp", "crystal", (0.0, 6.5, 0.0), 2.3, base=(130, 255, 180), glow=True)
    orb(m, "core", "crystal", (0.0, 6.5, 0.0), 1.0, base=(245, 255, 250), glow=True)
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["wisp", "core"], (130, 255, 180), 1.0, 0.6)
    finish_item("wisp_lantern", m, atlas, "handitem", real_m=0.5, fit=True, glow=("90FFC0", 0.2, 0.4, 1.0))


def build_golem_core_belt():
    """Golem Core Belt: a stretch of leather belt with an iron buckle plate and a glowing golem core set in it."""
    m = new_mesh("golem_core_belt")
    cuboid(m, "strap", "leather", -11.0, 11.0, 0.0, 4.0, -0.7, 0.7, base=(98, 64, 40), chamfer=0.3)
    cuboid(m, "plate", "iron", -4.6, 4.6, -1.6, 5.6, -1.0, 1.4, base=(84, 86, 94), chamfer=0.6)
    for i, x in enumerate((-9.0, -6.5, 6.5, 9.0)):
        studs(m, "stud_%d" % i, "iron", [(x, 2.0, 0.7)], 0.5, 0.35, base=(130, 132, 142))
    orb(m, "core", "crystal", (0.0, 2.0, 1.3), 2.1, base=(255, 150, 60), glow=True, rz=0.9)
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["core"], (255, 150, 60), 1.0, 0.6)
    finish_item("golem_core_belt", m, atlas, "handitem", real_m=0.5, fit=True, glow=("FF9A40", 0.14, 0.3, 0.9))


def build_wraith_cloak():
    """Wraith Cloak: a ragged pale-violet cape that hangs half-transparent from a clasp of dark metal and a cold blue gem."""
    m = new_mesh("wraith_cloak")
    pts = [(-6.0, 16.0), (6.0, 16.0), (8.0, 8.0), (7.0, 0.0), (5.0, -3.5), (3.5, 0.5), (1.5, -5.0), (0.0, -1.0), (-1.8, -6.0), (-3.5, 0.0), (-5.5, -3.0), (-7.0, 1.0), (-8.0, 8.0)]
    extrude(m, "cape", "cloth", pts, 0.35, edge=0.8, base=(96, 84, 150), smooth=30)
    collar_ring(m, 15.0, 3.6, 1.2, "iron", (56, 52, 72), name="clasp_bar")
    orb(m, "gem", "crystal", (0.0, 15.0, 1.0), 1.4, base=(130, 190, 255), glow=True)
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)

    def fade(pc):
        t = np.clip((pc.Y + 6.0) / 22.0, 0, 1)
        pc.img[..., :3] = pc.img[..., :3] * (0.55 + 0.45 * t[..., None])
        put_glow(pc, (1.0 - t) * 0.5, (140, 170, 255), 0.5, 0.4)
    paint(atlas, m.part("cape"), fade)
    glow_parts(atlas, m, ["gem"], (130, 190, 255), 1.0, 0.6)
    finish_item("wraith_cloak", m, atlas, "handitem", real_m=0.5, fit=True, glow=("A0B8FF", 0.14, 0.3, 0.8))


def build_cultist_sigil_ring():
    """Cultist Sigil Ring: a gold band with a flat round seal on top carrying a ring of glowing runes."""
    m = new_mesh("cultist_sigil_ring")
    tube(m, "band", "gold", ring_pts(0.0, 5.0, 5.0, 28), 1.1, 1.1, sides=7, base=(214, 170, 70))
    seal = P(m, "seal", "gold", base=(150, 40, 56), smooth=40, uv_mode="loft")
    ml.lathe(seal, "z", (0.0, 12.0, 0.0), [(-0.7, 0.0), (-0.6, 3.4), (0.6, 3.4), (0.7, 0.0)], sides=16, caps=(False, False))
    runes = P(m, "runes", "crystal", base=(255, 200, 96), glow=True, smooth=40)
    for k in range(6):
        a = 2 * math.pi * k / 6
        ml.gem(runes, (2.0 * math.cos(a), 12.0 + 2.0 * math.sin(a), 0.8), 0.38, 1.0, 5)
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["runes"], (255, 200, 96), 1.0, 0.6)
    finish_item("cultist_sigil_ring", m, atlas, "handitem", real_m=0.5, fit=True, glow=("FFC860", 0.14, 0.3, 0.8))


def build_tide_pearl_amulet():
    """Tide Pearl Amulet: a silver wire loop round a cool white pearl held in three small coral prongs."""
    m = new_mesh("tide_pearl_amulet")
    tube(m, "wire", "silver", ring_pts(0.0, 13.0, 5.5, 26), 0.35, 0.35, sides=5, base=(196, 204, 216))
    orb(m, "pearl", "crystal", (0.0, 5.0, 0.0), 2.6, base=(210, 235, 245), glow=True)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.5
        c, s = math.cos(a), math.sin(a)
        tube(m, "prong_%d" % k, "bone", [(2.4 * c, 2.8, 2.4 * s), (3.0 * c, 5.0, 3.0 * s), (1.6 * c, 7.6, 1.6 * s)], 0.45, 0.15, sides=5, tip=True, base=(238, 130, 120))
    collar_ring(m, 9.4, 1.0, 1.0, "silver", (196, 204, 216), name="bail")
    atlas = new_atlas(256)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["pearl"], (180, 225, 245), 0.7, 0.45)
    finish_item("tide_pearl_amulet", m, atlas, "handitem", real_m=0.5, fit=True, glow=("B0E0F8", 0.1, 0.2, 0.6))


BUILDERS = {
    "goblin_fang_dagger": build_goblin_fang_dagger, "delvers_longsword": build_delvers_longsword, "bone_lance": build_bone_lance, "orc_cleaver": build_orc_cleaver,
    "shadowsteel_katana": build_shadowsteel_katana, "drakefang_greatsword": build_drakefang_greatsword, "wisp_staff": build_wisp_staff, "golem_maul": build_golem_maul,
    "goblin_ear_ward": build_goblin_ear_ward, "orc_tusk_ring": build_orc_tusk_ring, "lich_dust_pendant": build_lich_dust_pendant, "wisp_lantern": build_wisp_lantern,
    "golem_core_belt": build_golem_core_belt, "wraith_cloak": build_wraith_cloak, "cultist_sigil_ring": build_cultist_sigil_ring, "tide_pearl_amulet": build_tide_pearl_amulet,
}


# ================================================================ class rows and display presets
def class_rows():
    """Adds a row to both copies of weapon_classes.json for every weapon (its type) and trinket (handitem), keeping every other row and the file's formatting."""
    rows = {x[0]: x[2] for x in R.WEAPONS}
    rows.update({x[0]: "handitem" for x in R.TRINKETS})
    game = os.path.join(GAME_ASSETS, "weapon_classes.json")
    text = open(game, encoding="utf-8").read()
    d = json.loads(text)
    for k, v in rows.items():
        d["weapons"][k] = v
    open(game, "w", encoding="utf-8", newline="\n").write(json.dumps(d, indent=1, ensure_ascii=False) + "\n")
    tools = os.path.join(REPO, "tools", "relics", "weapon_classes.json")
    t = json.loads(open(tools, encoding="utf-8").read())
    for k, v in rows.items():
        t["weapons"][k] = {"class": v}
    open(tools, "w", encoding="utf-8", newline="\n").write(json.dumps(t, indent=1, ensure_ascii=False))


def presets(check=False):
    """The oversized-weapon display rule for the eight weapons only (the rows of every other weapon stay as they are): see tools/relics/new197_presets.py."""
    import display_presets as dp
    ids = [x[0] for x in R.WEAPONS]
    entries = []
    for item_id in ids:
        mesh = dp.read_mesh(os.path.join(dp.MESHES, item_id + ".txt"))
        mjson = os.path.join(dp.MODELS, item_id + ".json")
        raw = open(mjson, encoding="utf-8").read()
        model = json.loads(raw)
        plan = dp.plan_for(item_id, model, mesh)
        entry = {"id": item_id, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
        if plan["action"] == "preset":
            entry.update(first_person_scale=plan["scale"], first_person_translation_px=plan["translation"], tip_out_of_view=not plan["tip_in_view"],
                         ground_scale=plan["views"][0], fixed_scale=plan["views"][1])
        if plan["action"] in ("preset", "restore"):
            new = dp.apply_plan(model, plan)
            text = json.dumps(new, indent=2)
            if text != raw and not check:
                open(mjson, "w", encoding="utf-8", newline="\n").write(text)
        entries.append(entry)
    if not check:
        man = json.load(open(dp.MANIFEST, encoding="utf-8"))
        mine = set(ids)
        rows = [r for r in man["weapons"] if r["id"] not in mine] + entries
        man["weapons"] = sorted(rows, key=lambda r: r["id"])
        open(dp.MANIFEST, "w", encoding="utf-8", newline="\n").write(json.dumps(man, indent=2) + "\n")
    return entries


def main(only=None):
    only = set(only or [])
    n = 0
    for item_id, fn in BUILDERS.items():
        if only and item_id not in only:
            continue
        fn()
        n += 1
    print("built", n)
    if only and "finalize" not in only:
        return                                                  # a partial build writes the meshes only; `finalize` (or no ids at all) also writes the class rows and display presets
    class_rows()
    entries = presets()
    for e in entries:
        if e["action"] == "preset":
            print("  %-26s L=%.2f s=%.3f%s" % (e["id"], e["length_blocks"], e["first_person_scale"], "  OUT-OF-VIEW" if e.get("tip_out_of_view") else ""))


if __name__ == "__main__":
    main(sys.argv[1:])
