"""The five Weapons of Mortal Sin and the Holy Sword that the Black Sword of Greed's series (Berserk of Gluttony) adds to the Black Sword, as 3D models.

    python3 -B tools/berserk_weapons/build.py [id ...]          (deterministic, idempotent; no argument = all five)

Built with the owner's model method through the Blender-less kit (tools/relics/new197_lib.py -> mesh_lib.py: real cross-sections, named parts, projected UVs, a painted skin and an additive
glow skin) because Blender is not installed in the build container (CLAUDE.md rule 14: this is reported in docs/berserk_weapons.md). Per weapon `finish()` writes
weapon_meshes/<id>.txt, textures/entity/weapon/<id>.png + _glow.png, the 16 x 16 inventory sprite textures/item/<id>.png and models/item/<id>.json + <id>_icon.json.
Axes (docs/weapon_scale_orientation.md): y along the weapon from the butt to the tip, the cutting edge toward -x, the flat toward +z. Units are centimetres.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "relics"))
import numpy as np                                                                                  # noqa: E402

from new197_kit import (P, plate, rod, lathe_y, crystal, bar, tube, spike, ml, Outline, guard_ring, collar_ring, straight_sword, spear_head, shaft,      # noqa: E402
                        paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, paint_glow, new_mesh, new_atlas, lay_and_paint, finish, grip, pommel, guard_cross)


def build_black_axe_of_sloth():
    """Black Axe of Sloth: a heavy bearded war axe, black iron on a black haft with iron bands; a dull violet glow sits in the edge and in drooping runes (sloth: weight, slowness)."""
    m = new_mesh("black_axe_of_sloth")
    shaft(m, 0.0, 114.0, r=1.7, material="wood", base=(30, 28, 38), name="haft", sides=10)
    for y in (6.0, 38.0, 74.0):
        collar_ring(m, y, 2.15, 1.5, "iron", (58, 54, 72), name="band_%d" % int(y))
    lathe_y(m, "butt", "iron", -4.0, [(0.0, 0.0), (1.5, 1.7), (4.0, 2.3)], sides=10, base=(58, 54, 72), caps=(False, True))
    st = [(84.0, -3.0, 3.2, 0.9), (88.0, -9.0, 3.2, 1.0), (98.0, -21.0, 3.2, 1.1), (107.0, -18.0, 3.2, 1.0), (117.0, -9.0, 3.2, 0.9), (121.0, -3.0, 3.2, 0.8), (123.0, 0.0, 0.0, 0.0)]
    plate(m, "head", "iron", st, bevel=0.5, base=(34, 32, 44), glow=True, smooth=45)
    poll = P(m, "poll", "iron", base=(52, 48, 66), smooth=45)
    spike(poll, (3.0, 100.0, 0.0), (15.0, 104.0, 0.0), 1.6, 6)
    lathe_y(m, "cap", "iron", 112.0, [(0.0, 1.9), (2.0, 2.5), (4.0, 1.4)], sides=10, base=(58, 54, 72))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    out, head = Outline(st), m.part("head")
    paint_edge_glow(atlas, head, out, (150, 90, 235), 0.7, 0.85, 0.5, side="edge")
    paint_runes(atlas, head, out, (176, 120, 255), 90.0, 116.0, 5, seed=5, size=0.8, side=1.2)
    paint_gradient(atlas, head, (26, 24, 36), (70, 62, 92), 84.0, 122.0, 0.4)
    finish("black_axe_of_sloth", m, atlas, "battleaxe", glow=("9A5CFF", 0.10, 0.45, 1.0))


def build_black_gunblade_of_envy():
    """Black Gunblade of Envy: a broad black sword whose spine carries a barrel and a chamber, a trigger loop at the guard; the chamber and the edge burn envy green."""
    m = new_mesh("black_gunblade_of_envy")
    L0 = 24.0
    st = [(L0, -4.2, 4.0, 0.75), (L0 + 8, -4.3, 4.1, 0.78), (64.0, -4.1, 3.9, 0.72), (86.0, -3.5, 3.5, 0.66), (96.0, -2.4, 2.8, 0.55), (102.0, -1.2, 1.6, 0.4), (105.0, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=7.0, t=1.3, h=1.4, bulb=1.3, curve=-1.2), grip_len=21.0, pommel_style="disc", pommel_kw=dict(r=3.0),
                         blade_material="iron", blade_base=(36, 40, 44), guard_material="iron", guard_base=(52, 58, 62), grip_base=(24, 30, 28), wrap_base=(70, 120, 84), bevel=0.5)
    rod(m, "barrel", "iron", L0 + 1.0, L0 + 40.0, 1.5, 1.2, sides=8, base=(46, 52, 56), x=5.6, z=0.0)
    lathe_y(m, "chamber", "iron", L0 - 7.0, [(0.0, 1.9), (3.0, 2.8), (9.0, 2.8), (12.0, 1.9)], sides=10, base=(50, 56, 60), x=5.2, z=0.0)
    lathe_y(m, "muzzle", "iron", L0 + 38.0, [(0.0, 1.6), (1.4, 1.9), (3.0, 1.2)], sides=8, base=(70, 76, 80), x=5.6, z=0.0)
    pts = [(2.6 + 4.0 * math.cos(a), L0 - 9.0 + 4.0 * math.sin(a) - 4.0, 0.0) for a in np.linspace(math.pi * 0.2, math.pi * 1.8, 11)]
    tube(m, "trigger_loop", "iron", pts, 0.6, 0.6, sides=6, base=(60, 66, 70))
    crystal(m, "envy_gem", "crystal", (5.2, L0 + 1.0, 2.1), 1.1, 2.6, base=(60, 255, 130), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_edge_glow(atlas, blade, out, (70, 255, 140), 0.55, 0.8, 0.5, side="edge")
    paint_center_glow(atlas, blade, out, (60, 240, 120), 0.4, L0 + 10, 96.0, 0.7, 0.45)
    paint_runes(atlas, blade, out, (110, 255, 170), L0 + 14, 90.0, 6, seed=9, size=0.7, side=1.4)
    for nm in ("envy_gem", "muzzle"):
        paint_glow(atlas, m.part(nm), (70, 255, 140), 0.9, 0.5)
    finish("black_gunblade_of_envy", m, atlas, "longsword", glow=("46FF8C", 0.12, 0.45, 1.0))


def build_black_spear_of_vanity():
    """Black Spear of Vanity: a long black spear whose leaf head is backed by a polished mirror disc, gilt barbs and a magenta glow (vanity: the admired self)."""
    m = new_mesh("black_spear_of_vanity")
    Y0 = 146.0
    shaft(m, 0.0, Y0, r=1.6, material="wood", base=(26, 22, 32), name="shaft", sides=10)
    for y in (8.0, 60.0, 100.0):
        collar_ring(m, y, 2.0, 1.4, "gold", (200, 160, 70), name="band_%d" % int(y))
    lathe_y(m, "butt", "gold", -4.0, [(0.0, 0.0), (1.4, 1.6), (4.0, 2.1)], sides=10, base=(200, 160, 70), caps=(False, True))
    lathe_y(m, "mirror", "silver", Y0 - 3.0, [(0.0, 0.5), (0.8, 6.0), (1.8, 6.0), (2.6, 0.8)], sides=20, base=(214, 218, 236), glow=True)
    barbs = P(m, "barbs", "gold", base=(214, 172, 74), smooth=45)
    for k in range(8):
        a = 2 * math.pi * k / 8
        spike(barbs, (6.0 * math.cos(a), Y0 - 1.2, 6.0 * math.sin(a)), (8.6 * math.cos(a), Y0 - 7.0, 8.6 * math.sin(a)), 0.6, 4)
    out = spear_head(m, Y0 + 1.0, 46.0, 4.8, t=0.9, base=(30, 26, 40), material="iron", glow=True, leaf=0.38)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    head = m.part("head")
    paint_edge_glow(atlas, head, out, (255, 90, 200), 0.6, 0.85, 0.5, side="both")
    paint_center_glow(atlas, head, out, (255, 130, 220), 0.35, Y0 + 6.0, Y0 + 40.0, 0.7, 0.45)
    paint_glow(atlas, m.part("mirror"), (255, 150, 225), 0.55, 0.35)
    finish("black_spear_of_vanity", m, atlas, "spear", glow=("FF5AC8", 0.10, 0.45, 1.0))


def build_black_chakrams():
    """Black Chakrams: ONE ring (the item draws one in each hand): a black steel hoop with ten curved outer blades, a grip bar across its middle and a crimson glow in the cutting rim."""
    m = new_mesh("black_chakrams")
    R, CY = 13.0, 18.0
    ring = [(R * math.cos(a), CY + R * math.sin(a), 0.0) for a in np.linspace(0.0, 2 * math.pi, 33)]
    tube(m, "hoop", "iron", ring, 1.15, 1.15, sides=8, base=(30, 32, 38), glow=True)
    inner = [((R - 1.8) * math.cos(a), CY + (R - 1.8) * math.sin(a), 0.0) for a in np.linspace(0.0, 2 * math.pi, 33)]
    tube(m, "inner_rim", "iron", inner, 0.45, 0.45, sides=6, base=(150, 40, 60), glow=True)
    blades = P(m, "blades", "iron", base=(40, 42, 50), smooth=45, glow=True)
    for k in range(10):
        a = 2 * math.pi * k / 10
        b = (( R + 0.6) * math.cos(a), CY + (R + 0.6) * math.sin(a), 0.0)
        t = ((R + 6.0) * math.cos(a + 0.35), CY + (R + 6.0) * math.sin(a + 0.35), 0.0)
        spike(blades, b, t, 1.3, 4, up=(0, 0, 1))
    bar(m, "grip_bar", "leather", (-(R - 2.0), CY, 0.0), (R - 2.0, CY, 0.0), 2.4, 2.0, base=(40, 24, 28))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    for nm in ("hoop", "inner_rim", "blades"):
        paint_glow(atlas, m.part(nm), (255, 60, 80), 0.8, 0.45)
    finish("black_chakrams", m, atlas, "handaxe", glow=("FF3C50", 0.12, 0.45, 1.0), real_m=0.62)


def build_seifort_holy_sword():
    """Seifort Holy Sword: a Holy Knight's longsword forged in Seifort's Military District from an orichalcum alloy: a pale rose-gold blade, a gold wing guard, a cross inlay that shines (Grand Cross)."""
    m = new_mesh("seifort_holy_sword")
    L0 = 24.0
    st = [(L0, -3.4, 3.4, 0.6), (L0 + 8, -3.5, 3.5, 0.62), (68.0, -3.3, 3.3, 0.55), (98.0, -2.7, 2.7, 0.48), (110.0, -1.7, 1.7, 0.4), (117.0, -0.8, 0.8, 0.3), (120.0, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="wings", guard_kw=dict(span=10.5, feathers=4, rise=8.0, t=0.9), grip_len=23.0, pommel_style="disc", pommel_kw=dict(r=3.4),
                         blade_material="silver", blade_base=(238, 214, 196), guard_material="gold", guard_base=(228, 184, 90), grip_base=(236, 232, 224), wrap_base=(196, 160, 92), bevel=0.5)
    for sgn, suf in ((1, "_f"), (-1, "_b")):
        f = P(m, "fuller" + suf, "gold", base=(255, 228, 160), glow=True, smooth=50)
        ml.ridge_pair(f, (0.0, L0 + 10.0, sgn * 0.5), (0.0, 100.0, sgn * 0.4), (0, 0, sgn), 1.2, 0.28, 0.12, 5.0)
        bar(m, "cross" + suf, "gold", (-2.6, 76.0, sgn * 0.62), (2.6, 76.0, sgn * 0.62), 0.9, 0.3, up=(0, 0, sgn), base=(255, 236, 170), glow=True)
    crystal(m, "cross_gem", "crystal", (0.0, L0 + 2.6, 0.9), 1.1, 2.6, base=(255, 240, 190), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_center_glow(atlas, blade, out, (255, 232, 160), 0.5, L0 + 10, 100.0, 0.8, 0.5)
    paint_edge_glow(atlas, blade, out, (255, 240, 190), 0.45, 0.55, 0.5, side="both")
    paint_runes(atlas, blade, out, (255, 220, 130), L0 + 14, 66.0, 6, seed=21, size=0.8, side=1.6)
    paint_gradient(atlas, blade, (240, 210, 190), (255, 248, 236), L0, 120.0, 0.4)
    for nm in ("fuller_f", "fuller_b", "cross_f", "cross_b", "cross_gem"):
        paint_glow(atlas, m.part(nm), (255, 228, 140), 0.85, 0.5)
    finish("seifort_holy_sword", m, atlas, "longsword", glow=("FFE8A0", 0.12, 0.5, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

if __name__ == "__main__":
    ids = sys.argv[1:] or list(BUILDERS)
    for i in ids:
        BUILDERS[i]()
