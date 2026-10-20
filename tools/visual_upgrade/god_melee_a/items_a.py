"""God melee (a), items 1 to 4: Staff of the Magi, Wizard King's Vow, Wand of the Wisdom King, Hammer of Thunderbolts.

Each item = `extras` (new parts, added to the old builder's mesh before the UVs), `post` (painters run on the finished layout) and `SPEC` (assets/nusmp/visual_upgrade/<id>.json).
Units: centimetres, y along the weapon, the edge toward -x, the flat toward +z (docs/weapon_scale_orientation.md).
"""
import math

import numpy as np

import ukit as U
import skinkit as SK
import specs as SP

L, ml, w = U.L, U.ml, U.w
V, mk = U.V, U.mk
ITEMS = {}


def item(item_id):
    def deco(cls):
        ITEMS[item_id] = cls
        cls.id = item_id
        return cls
    return deco


# =====================================================================================================================================================
# 1. Staff of the Magi (Dungeons and Dragons): the master staff of a great mage, a twisted violet shaft bound in gold, a floating crystal.
#    Upgrade: an armillary of three counter-turning rune rings and six shards orbiting the crystal, filigree tendrils round the cradle, engraved sleeves, tassels, a clawed butt.
# =====================================================================================================================================================
@item("staff_of_the_magi")
class Staff:
    Y1 = 205.0
    CY = 225.0                                                            # centre of the great crystal
    builder = staticmethod(lambda: U.G1.build_staff_of_the_magi())
    tube_cap = 32
    lod_keep = ("magi_crystal", "cradle", "halo", "strand", "core")

    @staticmethod
    def extras(m, ctx):
        Y1, CY = Staff.Y1, Staff.CY
        C = V(0, CY, 0)
        # ---- the armillary: three rune rings turning about different axes (orbiting groups 0, 1, 2) and six shards (group 3)
        if not ctx.lod:
            a = U.orbit_part(ctx, m, "up_ring_a", 0, C, (0, 1, 0), 2.4, 0.0, True, "gold", (236, 196, 90))
            for pts_part in (a,):
                pts = U.circle_pts(C, 16.0, (0, 1, 0), 30)
                ml.sweep(pts_part, [tuple(q) for q in pts], ml.circle(1.0, 4), scales=lambda t: (0.5, 0.5), caps=(False, False))
            blocks = U.orbit_part(ctx, m, "up_ring_a_runes", 0, C, (0, 1, 0), 2.4, 0.0, True, "crystal", (255, 224, 140))
            for i in range(10):
                ang = 2 * math.pi * i / 10
                d = V(math.cos(ang), 0, math.sin(ang))
                ml.loft(blocks, d, C + d * 15.2, [(0.0, 0.62, 0.9), (1.2, 0.62, 0.9), (1.5, 0.4, 0.6)], ml.rect_ring(2.0, 2.0), up=(0, 1, 0))
            b = U.orbit_part(ctx, m, "up_ring_b", 1, C, (0.7, 0.7, 0.0), -3.1, 40.0, True, "silver", (220, 214, 250))
            ml.sweep(b, [tuple(q) for q in U.circle_pts(C, 12.5, (0.6, 0.2, 0.77), 24)], ml.circle(1.0, 4), scales=lambda t: (0.4, 0.4), caps=(False, False))
            c = U.orbit_part(ctx, m, "up_ring_c", 2, C, (0.0, 0.7, 0.7), 3.8, 100.0, True, "gold", (232, 188, 84))
            ml.sweep(c, [tuple(q) for q in U.circle_pts(C, 9.5, (0.9, -0.3, 0.3), 22)], ml.circle(1.0, 4), scales=lambda t: (0.35, 0.35), caps=(False, False))
            sh = U.orbit_part(ctx, m, "up_shards", 3, C, (0, 1, 0), -1.5, 0.0, True, "crystal", (190, 140, 255), 25)
            for i in range(6):
                ang = 2 * math.pi * i / 6
                pos = C + V(math.cos(ang) * 20.0, 3.0 * math.sin(ang * 2.0), math.sin(ang) * 20.0)
                ml.gem(sh, tuple(pos), 1.1, 2.2, 5, axis="y")
        # ---- filigree tendrils climbing the cradle (static): three golden scrolls with leaf tips
        td = mk(m, "up_filigree", "gold", (240, 200, 96), False, 55, uv_mode="loft")
        for k in range(3 if not ctx.lod else 1):
            ph = 2 * math.pi * k / 3 + 0.4
            pts = []
            for i in range(U.S(11, 5)):
                t = i / (U.S(11, 5) - 1)
                r = 4.6 + 3.0 * math.sin(t * math.pi * 1.3)
                pts.append((r * math.cos(ph + t * 4.2), Y1 + 3.0 + 20.0 * t, r * math.sin(ph + t * 4.2)))
            ml.sweep(td, pts, ml.circle(1.0, U.S(4, 3)), scales=lambda t: (0.42 * (1 - 0.6 * t), 0.42 * (1 - 0.6 * t)), caps=(True, True))
        gems = mk(m, "up_cradle_gems", "crystal", (170, 110, 255), True, 25)
        for k in range(4):
            ang = math.pi / 2 * k + math.pi / 4
            ml.gem(gems, (4.4 * math.cos(ang), Y1 + 1.4, 4.4 * math.sin(ang)), 0.75, 1.3, U.S(5, 4), axis="y")
        # ---- sleeves (engraved with the staff's lore text)
        for i, y in enumerate((50.0, 102.0, 160.0)):
            sl = mk(m, "up_sleeve_%d" % i, "gold", (226, 182, 74), False, 40, uv_mode="loft")
            ml.lathe(sl, "y", (0, y, 0), [(0.0, 3.55), (0.5, 3.95), (3.5, 3.95), (4.0, 3.55), (7.0, 3.55), (7.5, 3.95), (11.0, 3.95), (11.5, 3.55)], sides=U.S(10, 5))
        # ---- two tassels of violet silk from the middle sleeve
        if not ctx.lod:
            U.tuft(m, "up_tassel_a", (3.9, 100.0, 0.0), 5, 26.0, 1.0, "cloth", (122, 70, 190), 1)
            U.tuft(m, "up_tassel_b", (-3.9, 100.0, 0.0), 5, 20.0, 1.0, "cloth", (232, 190, 84), 2)
        # ---- a clawed butt
        bt = mk(m, "up_butt_claws", "gold", (224, 178, 70), False, 35)
        for k in range(4):
            ang = math.pi / 2 * k
            U.spike3(bt, (2.2 * math.cos(ang), 1.0, 2.2 * math.sin(ang)), (4.2 * math.cos(ang), -9.0, 4.2 * math.sin(ang)), 0.75, 4)

    @staticmethod
    def post(m, atlas, ctx):
        VIO, GOLD, LAV = (190, 130, 255), (255, 222, 130), (150, 100, 240)
        SK.glow_parts(atlas, m, ["up_ring_a", "up_ring_b", "up_ring_c"], GOLD, 0.85, 0.5)
        SK.glow_parts(atlas, m, ["up_ring_a_runes"], (255, 236, 170), 1.0, 0.7)
        SK.glow_parts(atlas, m, ["up_shards", "up_cradle_gems"], VIO, 1.0, 0.6)
        SK.glow_parts(atlas, m, ["up_filigree"], GOLD, 0.45, 0.4)
        SK.glow_parts(atlas, m, ["up_butt_claws"], GOLD, 0.2, 0.3)
        text = "ARCANA OBEDIT MAGO SINE FINE"
        for i in range(3):
            def eng(en, pc, i=i):
                # a band of letters round the sleeve and two thin rings above and below
                en.text_row(text[i * 9:i * 9 + 9] + text[:4], 0.2, 8.4, 0.55, 0.9, 0.09)
                en.line((0.0, 11.0), (30.0, 11.0), 0.07)
                en.line((0.0, 1.0), (30.0, 1.0), 0.07)
                en.burn(0.6, 0.25, (255, 220, 130), 0.8, 0.5)
            SK.engrave_face(atlas, m.part("up_sleeve_%d" % i), eng, only_faces=False)
        SK.detail_all(m, atlas, skip=("up_ring", "up_shards"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, m.part("strand_0"), LAV, 1, 1.0, (7.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("strand_1"), LAV, 2, 1.0, (7.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("core"), GOLD, 3, 1.0, (9.0, 1.0), 0.8)
            SK.paint_veins(atlas, m.part("magi_crystal"), VIO, 4, 1.4, (2.6, 2.6), 0.9)
            for i in range(3):
                SK.paint_veins(atlas, m.part("up_sleeve_%d" % i), GOLD, 5 + i, 1.0, (3.0, 1.2), 0.9)
            SK.paint_veins(atlas, m.part("up_filigree"), GOLD, 9, 2.0, (2.0, 0.8), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(190, 130, 255), rgb2=(255, 222, 130), length=2.3, tilt=12,
        ambient=[
            {"type": "motes", "anchor": "tip", "particle": "enchant", "rgb": [190, 130, 255], "rate": 14, "radius": 0.5, "rise": 0.03},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [255, 222, 130], "rate": 18, "radius": 0.62, "speed": 0.22, "size": 0.55, "tilt": 0.0},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [200, 150, 255], "rate": 12, "radius": 0.42, "speed": -0.31, "size": 0.45, "tilt": 55.0},
            {"type": "arc", "anchor": "tip", "particle": "electric_spark", "rgb": [220, 190, 255], "rate": 4, "radius": 0.7},
            {"type": "shaft", "anchor": "tip", "particle": "end_rod", "rgb": [210, 170, 255], "rate": 5, "height": 1.6},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [170, 110, 255], "rate": 10, "radius": 0.95, "speed": 0.06, "size": 0.5},
        ],
        swing={"pattern": "sigil", "particle": "dust", "rgb": [200, 150, 255], "rgb2": [255, 222, 130], "count": 3, "size": 0.7, "reach": 1.9},
        acts=[
            ("retributive_strike", [{"t": "glyph", "at": "caster", "r": 5.0, "rgb": [190, 130, 255], "tex": "magic_circle", "dur": 26, "spin": 0.08},
                                    {"t": "ring", "at": "caster", "r0": 0.5, "r1": 9.0, "rgb": [255, 222, 130], "dur": 18, "tex": "rune_ring"},
                                    {"t": "burst", "at": "caster", "n": 12, "len": 4.0, "rgb": [200, 160, 255], "tex": "spark", "dur": 14},
                                    {"t": "column", "at": "caster", "h": 9.0, "r": 0.7, "rgb": [190, 130, 255], "dur": 16}]),
            ("spell_turning", [{"t": "orbit", "at": "caster", "r": 1.8, "n": 6, "rgb": [255, 222, 130], "tex": "rune_ring", "dur": 34, "spin": 0.18, "y": 1.1, "size": 0.7},
                               {"t": "glyph", "at": "caster", "r": 2.6, "rgb": [200, 150, 255], "tex": "elf_circle", "dur": 34, "spin": -0.12},
                               {"t": "ring", "at": "caster", "r0": 2.6, "r1": 3.0, "rgb": [255, 240, 190], "dur": 34, "tex": "magic_circle"}]),
            ("arcane_tempest", [{"t": "glyph", "at": "caster", "r": 8.0, "rgb": [170, 110, 255], "tex": "magic_circle", "dur": 40, "spin": 0.05},
                                {"t": "rain", "at": "caster", "r": 9.0, "h": 14.0, "n": 22, "len": 3.5, "rgb": [200, 160, 255], "tex": "spark", "dur": 36},
                                {"t": "arcs", "at": "caster", "n": 6, "r": 9.0, "h": 12.0, "rgb": [235, 215, 255], "dur": 30},
                                {"t": "flash", "at": "caster", "size": 6.0, "rgb": [255, 230, 190], "dur": 8}]),
        ],
        sound="nusmp:item.upgrade.staff_of_the_magi.hum",
        note="the great crystal, the armillary and the shards come from the lore text: a staff that takes magic in and turns it back")
