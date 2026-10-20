"""God melee (a), items 5 to 8: Gungnir the Unerring, Kusanagi the Grass-Cutter, Leviathan's Crown Trident, Calamity Wave Aegis (see items_a.py for the structure)."""
import math

import numpy as np

import ukit as U
import skinkit as SK
from items_a import item, ITEMS                                         # noqa: F401

L, ml, w = U.L, U.ml, U.w
V, mk = U.V, U.mk


def raven(part, c, size=1.0, flip=1.0):
    """A stylised raven (body, two swept wings, tail, beak) at c, flying along +x (flip = -1: along -x)."""
    s = size
    w.ellipsoid(part, (c[0], c[1], c[2]), 1.5 * s, 0.9 * s, 0.9 * s, sides=6, stacks=4)
    for sz in (-1, 1):
        ml.loft(part, V(0.35 * flip, 0.25, sz * 1.0), V(c[0] - 0.2 * flip * s, c[1] + 0.2, c[2] + sz * 0.5 * s),
                [(0.0, 0.25, 0.2), (1.8 * s, 0.55 * s, 0.14), (3.6 * s, 0.4 * s, 0.1), (4.8 * s, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 1, 0))
    ml.loft(part, V(-flip, -0.1, 0), V(c[0] - 1.2 * flip * s, c[1], c[2]), [(0.0, 0.25, 0.1), (1.2 * s, 0.55 * s, 0.1), (2.6 * s, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 1, 0))
    U.spike3(part, (c[0] + 1.3 * flip * s, c[1] + 0.1, c[2]), (c[0] + 2.5 * flip * s, c[1] - 0.2, c[2]), 0.28 * s, 4)


# =====================================================================================================================================================
# 5. Gungnir, the Unerring (Norse myth): a long golden spear with a rune-carved leaf head, a blue-wrapped grip, a gold wire spiral, a blue gem at the socket.
#    Upgrade: Odin's two ravens flying round the head, a ring of nine rune stones turning at the socket, socket wings, rune sleeves, cord tassels, head barbs, runes engraved down the head.
# =====================================================================================================================================================
@item("gungnir_the_unerring")
class Gungnir:
    Y_S = 176.0
    builder = staticmethod(lambda: U.G1.build_gungnir_the_unerring())
    tube_cap = 72
    lod_keep = ("socket", "grip_wraps")

    @staticmethod
    def extras(m, ctx):
        Y_S = Gungnir.Y_S
        HC = V(0, Y_S + 30.0, 0)
        if not ctx.lod:
            rs = U.orbit_part(ctx, m, "up_runestones", 0, V(0, Y_S + 12.0, 0), (0, 1, 0), 2.0, 0.0, True, "stone", (150, 160, 190), 35)
            for i in range(9):
                a = 2 * math.pi * i / 9
                d = V(math.cos(a), 0, math.sin(a))
                ml.loft(rs, d, V(0, Y_S + 12.0, 0) + d * 12.0, [(0.0, 1.0, 2.2), (1.0, 1.0, 2.2), (1.7, 0.75, 1.7)], ml.rect_ring(2.0, 2.0), up=(0, 1, 0))
            rv = U.orbit_part(ctx, m, "up_ravens", 1, HC, (0, 1, 0), 2.6, 0.0, False, "obsidian", (40, 40, 58), 40)
            for k in range(2):
                a = math.pi * k
                pos = V(math.cos(a) * 24.0, 6.0 * math.sin(a + 1.0), math.sin(a) * 24.0)
                # a raven flies along the tangent: build it flying +x at (0, y, 0) then turn it onto the circle
                tmp = ml.Part("tmp")
                raven(tmp, (0.0, 0.0, 0.0), 2.6)
                ang = math.degrees(-a) + 90.0
                tr = tmp.rotated("y", ang).translated(*(HC + pos))
                off = len(rv.V)
                rv.V.extend(tr.V)
                rv.F.extend([tuple(i2 + off for i2 in f) for f in tr.F])
                rv.tags.extend(tr.tags)
            ey = U.orbit_part(ctx, m, "up_raven_eyes", 1, HC, (0, 1, 0), 2.6, 0.0, True, "crystal", (255, 210, 120), 25)
            for k in range(2):
                a = math.pi * k
                pos = V(math.cos(a) * 24.0, 6.0 * math.sin(a + 1.0), math.sin(a) * 24.0)
                ey_c = HC + pos + V(math.cos(a + math.pi / 2) * 0.0, 0.3, 0.0)
                ml.gem(ey, tuple(ey_c + V(math.cos(a) * 0.0 + 3.1 * math.cos(a + math.pi / 2) * 0.0, 0.5, 0.0)), 0.6, 1.2, 4, axis="y")
        # socket wings (two pairs of gold plates swept back along the shaft)
        sw = mk(m, "up_socket_wings", "gold", (240, 204, 100), False, 40)
        for k in range(4):
            a = math.pi / 2 * k + math.pi / 4
            d = V(math.cos(a), 0.0, math.sin(a))
            ml.loft(sw, d * 1.0 + V(0, -0.35, 0), V(0, Y_S - 1.0, 0) + d * 2.6, [(0.0, 0.5, 0.3), (4.0, 1.5, 0.35), (8.0, 0.9, 0.3), (12.0, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(-d[2], 0, d[0]))
        # two rune sleeves on the shaft
        for i, y in enumerate((100.0, 150.0)):
            sl = mk(m, "up_sleeve_%d" % i, "gold", (238, 198, 92), False, 40, uv_mode="loft")
            ml.lathe(sl, "y", (0, y, 0), [(0.0, 2.3), (0.4, 2.7), (5.0, 2.7), (5.4, 2.3)], sides=U.S(12, 5))
        # barbs along the head
        bb = mk(m, "up_head_barbs", "gold", (252, 232, 150), True, 30)
        for sx in (-1, 1):
            for k in range(4):
                y = Y_S + 20.0 + k * 7.0
                hw = 5.4 * (1.0 - 0.14 * k)
                U.spike3(bb, (sx * hw * 0.92, y, 0.0), (sx * (hw + 1.6), y + 2.8, 0.0), 0.5, 4)
        # cords and tassels
        if not ctx.lod:
            U.tuft(m, "up_tassel_blue", (2.0, Y_S - 8.0, 0.0), 5, 16.0, 1.2, "cloth", (60, 100, 200), 5)
            U.tuft(m, "up_tassel_gold", (-2.0, Y_S - 8.0, 0.0), 5, 12.0, 1.2, "cloth", (238, 198, 92), 6)

    @staticmethod
    def post(m, atlas, ctx):
        Y_S = Gungnir.Y_S
        GOLD, BLU, WHT = (255, 226, 130), (130, 180, 255), (255, 244, 200)
        SK.glow_parts(atlas, m, ["up_runestones"], GOLD, 1.0, 0.6)
        SK.glow_parts(atlas, m, ["up_raven_eyes", "up_head_barbs"], (255, 214, 120), 1.0, 0.7)
        SK.glow_parts(atlas, m, ["up_socket_wings"], GOLD, 0.3, 0.3)
        head = m.part("head")

        def runes(en, pc):
            en.text_col("GUNGNIR ODIN", 1.8, Y_S + 46.0, 0.9, 2.2, 0.1, 0.4)
            en.text_col("NUNQUAM ERRAT", -1.8, Y_S + 46.0, 0.9, 2.2, 0.1, 0.4)
            en.burn(0.55, 0.25, GOLD, 0.85, 0.5)
        SK.engrave_face(atlas, head, runes)
        for i in range(2):
            def eng(en, pc, i=i):
                en.text_row("ASGARD VALHALLA ODIN" if i == 0 else "HUGINN MUNINN VOLVA", 0.1, 5.0, 0.55, 1.2, 0.09)
                en.burn(0.55, 0.25, GOLD, 0.8, 0.5)
            SK.engrave_face(atlas, m.part("up_sleeve_%d" % i), eng, only_faces=False)
        SK.detail_all(m, atlas, skip=("up_runestones", "up_ravens"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, head, GOLD, 1, 1.0, (5.0, 1.2), 0.9)
            SK.paint_veins(atlas, m.part("spiral"), GOLD, 2, 1.0, (4.0, 0.5), 0.9)
            SK.paint_veins(atlas, m.part("shaft"), BLU, 3, 0.9, (9.0, 0.9), 0.7)
            SK.paint_veins(atlas, m.part("up_runestones"), WHT, 4, 1.4, (1.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("up_socket_wings"), GOLD, 5, 1.2, (3.0, 1.0), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(255, 226, 130), rgb2=(130, 180, 255), length=3.2, tilt=14,
        ambient=[
            {"type": "motes", "anchor": "tip", "particle": "end_rod", "rgb": [255, 226, 130], "rate": 14, "radius": 0.5, "rise": 0.03},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [255, 226, 130], "rate": 16, "radius": 0.7, "speed": 0.2, "size": 0.55, "tilt": 0.0},
            {"type": "edge", "anchor": "blade", "particle": "electric_spark", "rgb": [255, 240, 190], "rate": 12, "radius": 0.08, "from": 0.8, "to": 1.0},
            {"type": "orbit", "anchor": "tip", "particle": "dust", "rgb": [130, 180, 255], "rate": 8, "radius": 0.9, "speed": -0.2, "size": 0.45, "tilt": 25.0},
            {"type": "shaft", "anchor": "tip", "particle": "end_rod", "rgb": [255, 244, 200], "rate": 5, "height": 2.2},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [255, 214, 110], "rate": 10, "radius": 1.0, "speed": 0.05, "size": 0.5},
        ],
        swing={"pattern": "thrust", "particle": "end_rod", "rgb": [255, 226, 130], "rgb2": [130, 180, 255], "count": 3, "size": 0.7, "reach": 2.4},
        acts=[
            ("unerring_cast", [{"t": "beam", "from": "caster", "to": "to", "w": 0.7, "rgb": [255, 236, 170], "dur": 12},
                               {"t": "spiral", "at": "caster", "r": 0.8, "h": 6.0, "turns": 4, "n": 16, "rgb": [255, 214, 110], "dur": 14},
                               {"t": "burst", "at": "to", "n": 8, "len": 4.0, "rgb": [255, 226, 130], "tex": "spark", "dur": 14},
                               {"t": "glyph", "at": "to", "r": 2.6, "rgb": [255, 214, 110], "tex": "rune_ring", "dur": 22, "spin": 0.1}]),
            ("allfathers_pledge", [{"t": "glyph", "at": "caster", "r": 6.0, "rgb": [255, 226, 130], "tex": "magic_circle", "dur": 44, "spin": 0.05},
                                   {"t": "column", "at": "caster", "h": 14.0, "r": 0.8, "rgb": [255, 240, 190], "dur": 30},
                                   {"t": "orbit", "at": "caster", "r": 2.6, "n": 2, "rgb": [40, 40, 60], "tex": "glow", "dur": 44, "spin": 0.16, "y": 3.0, "size": 1.4}]),
            ("valhalla_rain", [{"t": "glyph", "at": "caster", "r": 9.0, "rgb": [255, 226, 130], "tex": "magic_circle", "dur": 44, "spin": -0.06},
                               {"t": "rain", "at": "caster", "r": 10.0, "h": 18.0, "n": 26, "len": 5.0, "rgb": [255, 236, 170], "tex": "streak", "dur": 40},
                               {"t": "ring", "at": "caster", "r0": 1.0, "r1": 11.0, "rgb": [130, 180, 255], "dur": 24, "tex": "rune_ring"}]),
        ],
        note="Odin's spear: two ravens, nine rune stones, a head that carries its own name")


# =====================================================================================================================================================
# 6. Kusanagi, the Grass-Cutter (Japanese myth): a long curved blade whose hamon is a line of wind, a cloud guard, a white and green grip, a jade pommel.
#    Upgrade: eight magatama beads (the eight heads of the serpent) turning at the guard, three wind rings round the blade, a sacred rope with paper streamers, cloud scrolls, a horimono.
# =====================================================================================================================================================
@item("kusanagi_the_grass_cutter")
class Kusanagi:
    L0 = 26.0
    builder = staticmethod(lambda: U.G1.build_kusanagi_the_grass_cutter())
    lod_keep = ("guard", "pommel")

    @staticmethod
    def bx(y):
        t = max(0.0, (y - Kusanagi.L0) / 96.0)
        return -9.0 * t ** 2.2

    @staticmethod
    def extras(m, ctx):
        L0 = Kusanagi.L0
        if not ctx.lod:
            mg = U.orbit_part(ctx, m, "up_magatama", 0, V(0, L0, 0), (0, 1, 0), 3.0, 0.0, True, "crystal", (90, 220, 160), 30)
            for i in range(8):
                a = 2 * math.pi * i / 8
                d = V(math.cos(a), 0, math.sin(a))
                c = V(0, L0 + 0.5, 0) + d * 10.5
                t = V(-math.sin(a), 0, math.cos(a))
                w.ellipsoid(mg, tuple(c), 1.5, 1.9, 1.5, sides=6, stacks=4)
                U.spike3(mg, tuple(c), tuple(c + t * 4.8 + V(0, 1.5, 0)), 0.9, 4)
            for k, (y, R, sp) in enumerate(((52.0, 6.8, 3.4), (77.0, 5.8, -4.2), (100.0, 4.8, 5.0))):
                cx = Kusanagi.bx(y)
                ring = U.orbit_part(ctx, m, "up_windring_%d" % k, 1 + k, V(cx, y, 0), (0, 1, 0), sp, 0.0, True, "silver", (190, 240, 214))
                ml.sweep(ring, [tuple(q) for q in U.circle_pts(V(cx, y, 0), R, (0, 1, 0), 24)], ml.circle(1.0, 4), scales=lambda t: (0.22, 0.22), caps=(False, False))
                for j in range(4):
                    a = math.pi / 2 * j
                    ml.gem(ring, (cx + R * math.cos(a), y, R * math.sin(a)), 0.6, 1.4, 4, axis="y")
        # cloud scrolls round the guard
        cl = mk(m, "up_cloud_scrolls", "gold", (236, 200, 96), False, 55, uv_mode="loft")
        for k in range(4):
            a = math.pi / 2 * k + math.pi / 4
            c = V(6.4 * math.cos(a), L0 - 0.2, 6.4 * math.sin(a))
            e1 = V(-math.sin(a), 0, math.cos(a))
            pts = U.scroll2d(c, (e1, V(0, 1, 0)), 1.6, 0.2, 1.4, U.S(12, 5))
            ml.sweep(cl, [tuple(q) for q in pts], ml.circle(1.0, U.S(4, 3)), scales=lambda t: (0.26, 0.26))
        # the sacred rope round the grip, with four paper streamers
        rp = mk(m, "up_shimenawa", "cloth", (232, 224, 190), False, 55, uv_mode="loft")
        pts = [(2.15 * math.cos(2 * math.pi * 2.5 * t), 8.0 + 8.0 * t, 2.15 * math.sin(2 * math.pi * 2.5 * t)) for t in np.linspace(0, 1, U.S(30, 10))]
        ml.sweep(rp, pts, ml.circle(1.0, 5), scales=lambda t: (0.42, 0.42))
        if not ctx.lod:
            for k in range(4):
                a = math.pi / 2 * k + 0.3
                U.ribbon(m, "up_shide_%d" % k, (2.4 * math.cos(a), 16.0, 2.4 * math.sin(a)), 8.0 + 1.5 * (k % 2), 0.9, 0.9, k * 1.3, "cloth", (250, 250, 250), 6, (0, -1, 0), (math.cos(a), 0, math.sin(a)))
        # the jade tassel
        if not ctx.lod:
            U.tuft(m, "up_tassel", (0.0, -3.5, 0.0), 6, 16.0, 1.0, "cloth", (60, 150, 110), 7)

    @staticmethod
    def post(m, atlas, ctx):
        L0 = Kusanagi.L0
        JADE, GOLD, WIND = (110, 255, 190), (255, 226, 120), (170, 255, 220)
        SK.glow_parts(atlas, m, ["up_magatama"], JADE, 1.0, 0.6)
        SK.glow_prefix(atlas, m, "up_windring_", WIND, 0.95, 0.6)
        SK.glow_parts(atlas, m, ["up_cloud_scrolls"], GOLD, 0.45, 0.4)
        blade = m.part("blade")

        def horimono(en, pc):
            # the engraved inscription near the tang and a line of wind marks down the blade
            en.text_col("KUSANAGI NO TSURUGI", 0.0, L0 + 44.0 + Kusanagi.bx(L0 + 40) * 0.0, 0.9, 1.9, 0.09, 0.4)
            for k in range(6):
                y = L0 + 52.0 + 8.0 * k
                x = Kusanagi.bx(y) + 0.9
                en.line((x - 0.5, y), (x + 0.5, y + 1.4), 0.08)
            en.burn(0.5, 0.25, WIND, 0.8, 0.5)
        SK.engrave_face(atlas, blade, horimono)
        SK.detail_all(m, atlas, skip=("up_magatama", "up_windring"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, blade, WIND, 1, 1.0, (9.0, 0.8), 0.9)
            SK.paint_veins(atlas, m.part("guard"), GOLD, 2, 1.0, (2.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("up_magatama"), JADE, 3, 1.3, (1.2, 0.8), 0.9)
            SK.paint_veins(atlas, m.part("pommel"), JADE, 4, 1.2, (1.0, 1.0), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(110, 255, 190), rgb2=(255, 226, 120), length=1.62, tilt=24,
        ambient=[
            {"type": "edge", "anchor": "blade", "particle": "cloud", "rgb": [200, 255, 230], "rate": 14, "radius": 0.12, "from": 0.2, "to": 1.0},
            {"type": "motes", "anchor": "mid", "particle": "happy_villager", "rgb": [110, 255, 190], "rate": 8, "radius": 0.55, "rise": 0.02},
            {"type": "ring", "anchor": "mid", "particle": "dust", "rgb": [170, 255, 220], "rate": 16, "radius": 0.6, "speed": 0.3, "size": 0.45, "tilt": 0.0},
            {"type": "orbit", "anchor": "tip", "particle": "dust", "rgb": [255, 226, 120], "rate": 8, "radius": 0.4, "speed": 0.45, "size": 0.4, "tilt": 50.0},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [120, 240, 180], "rate": 12, "radius": 1.1, "speed": 0.08, "size": 0.5},
            {"type": "arc", "anchor": "tip", "particle": "end_rod", "rgb": [220, 255, 240], "rate": 4, "radius": 0.6},
        ],
        swing={"pattern": "wind", "particle": "cloud", "rgb": [200, 255, 230], "rgb2": [110, 255, 190], "count": 4, "size": 0.9, "reach": 1.8},
        acts=[
            ("wind_cut", [{"t": "beam", "from": "caster", "to": "to", "w": 1.4, "rgb": [170, 255, 220], "tex": "wind_slash", "dur": 12},
                          {"t": "burst", "at": "to", "n": 8, "len": 3.2, "rgb": [200, 255, 230], "tex": "wind_slash", "dur": 12}]),
            ("cloud_step", [{"t": "ring", "at": "caster", "r0": 0.4, "r1": 4.5, "rgb": [200, 255, 230], "dur": 16, "tex": "glow"},
                            {"t": "spiral", "at": "caster", "r": 1.2, "h": 3.0, "turns": 2, "n": 14, "rgb": [170, 255, 220], "dur": 16},
                            {"t": "orbit", "at": "caster", "r": 1.6, "n": 8, "rgb": [110, 255, 190], "tex": "wind_slash", "dur": 16, "spin": 0.4, "y": 0.8, "size": 0.9}]),
            ("eightfold_cut", [{"t": "burst", "at": "caster", "n": 8, "len": 9.0, "rgb": [170, 255, 220], "tex": "wind_slash", "dur": 20},
                               {"t": "glyph", "at": "caster", "r": 7.0, "rgb": [110, 255, 190], "tex": "magic_circle", "dur": 30, "spin": 0.1},
                               {"t": "ring", "at": "caster", "r0": 1.0, "r1": 10.0, "rgb": [255, 226, 120], "dur": 20, "tex": "rune_ring"}]),
        ],
        note="the sword of the serpent's tail: eight beads, three wind rings, a sacred rope")


# =====================================================================================================================================================
# 7. Leviathan's Crown Trident (Greek myth): a three-pronged crown of blue steel and coral, barbs, a ring of coral spikes, a pearl, a wave-etched shaft.
#    Upgrade: two wave rings and six bubbles turning round the prongs, a real crown of pearl-tipped points, hanging kelp, shell fans, scales etched down the shaft.
# =====================================================================================================================================================
@item("leviathans_crown_trident")
class Trident:
    Y_S = 170.0
    builder = staticmethod(lambda: U.G1.build_leviathans_crown_trident())
    lod_keep = ("socket", "coral")

    @staticmethod
    def extras(m, ctx):
        Y_S = Trident.Y_S
        C = V(0, Y_S + 30.0, 0)
        if not ctx.lod:
            for k, (R, sp, ph) in enumerate(((13.0, 2.4, 0.0), (10.0, -3.0, 25.0))):
                wr = U.orbit_part(ctx, m, "up_wave_%d" % k, k, C, (0, 1, 0), sp, ph, True, "crystal", (70, 210, 220), 30)
                pts = [(C[0] + (R + 1.5 * math.sin(6 * a)) * math.cos(a), C[1] + 2.2 * math.sin(6 * a + k), (R + 1.5 * math.sin(6 * a)) * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 41)]
                ml.sweep(wr, pts, ml.circle(1.0, 4), scales=lambda t: (0.4, 0.4), caps=(False, False))
            bb = U.orbit_part(ctx, m, "up_bubbles", 2, C, (0, 1, 0), -1.7, 0.0, True, "crystal", (200, 245, 255), 40)
            for i in range(6):
                a = 2 * math.pi * i / 6
                w.ellipsoid(bb, (C[0] + 17.5 * math.cos(a), C[1] + 5.0 * math.sin(a * 2.0), 17.5 * math.sin(a)), 1.2, 1.2, 1.2, sides=6, stacks=4)
        # the crown: seven pearl-tipped points round the socket
        cr = mk(m, "up_crown", "gold", (232, 196, 90), False, 35)
        pe = mk(m, "up_crown_pearls", "crystal", (240, 250, 255), True, 40)
        for k in range(7):
            a = 2 * math.pi * k / 7 + 0.2
            b = V(5.0 * math.cos(a), Y_S + 5.0, 5.0 * math.sin(a))
            tip = V(6.4 * math.cos(a), Y_S + 15.0 + (k % 2) * 2.0, 6.4 * math.sin(a))
            U.spike3(cr, tuple(b), tuple(tip), 0.8, 4)
            w.ellipsoid(pe, tuple(tip + V(0, 0.4, 0)), 0.55, 0.55, 0.55, sides=6, stacks=3)
        # shell fans on the socket
        sh = mk(m, "up_shells", "bone", (250, 226, 214), False, 35)
        for k in range(2):
            sx = -1 if k == 0 else 1
            for j in range(5):
                ang = math.radians(-50 + 25 * j)
                d = V(sx * math.cos(ang) * 0.9, math.sin(ang) + 0.9, 0.0)
                d = d / np.linalg.norm(d)
                ml.loft(sh, d, V(sx * 3.2, Y_S - 2.0, 0.0), [(0.0, 0.2, 0.2), (3.0, 0.9, 0.22), (6.0, 0.8, 0.2), (7.0, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1))
        # hanging kelp
        if not ctx.lod:
            for k in range(3):
                a = 2 * math.pi * k / 3 + 0.5
                U.ribbon(m, "up_kelp_%d" % k, (3.9 * math.cos(a), Y_S - 6.0, 3.9 * math.sin(a)), 20.0 + 4.0 * k, 1.4, 2.0, k * 1.7, "cloth", (40, 140, 110), 9, (0, -1, 0), (math.cos(a), 0, math.sin(a)))
        # two sleeves
        for i, y in enumerate((40.0, 105.0)):
            sl = mk(m, "up_sleeve_%d" % i, "gold", (60, 196, 190), False, 40, uv_mode="loft")
            ml.lathe(sl, "y", (0, y, 0), [(0.0, 2.2), (0.4, 2.6), (6.0, 2.6), (6.4, 2.2)], sides=U.S(12, 5))

    @staticmethod
    def post(m, atlas, ctx):
        Y_S = Trident.Y_S
        TEAL, WHT, CORAL = (80, 230, 235), (225, 250, 255), (255, 140, 110)
        SK.glow_prefix(atlas, m, "up_wave_", TEAL, 0.95, 0.5)
        SK.glow_parts(atlas, m, ["up_bubbles", "up_crown_pearls"], WHT, 1.0, 0.7)
        SK.glow_parts(atlas, m, ["up_crown"], (255, 220, 120), 0.3, 0.3)
        SK.glow_parts(atlas, m, ["up_shells"], CORAL, 0.25, 0.3)

        def scales(pc):
            # fish scales: overlapping arcs down the shaft
            en = SK.Engraver(pc)
            ppu = en.ppu
            y = pc.Y.max()
            row = 0
            while y > pc.Y.min() - 1.0:
                x = pc.X.min() + (0.0 if row % 2 == 0 else 0.55)
                while x < pc.X.max() + 1.0:
                    en.circle((x, y), 0.55, 0.06, 255, 0, 180)
                    x += 1.1
                y -= 0.55
                row += 1
            en.burn(0.35, 0.18, TEAL, 0.5, 0.4)
        L.paint(atlas, m.part("shaft"), scales)
        for i in range(2):
            def eng(en, pc, i=i):
                en.text_row("MARE NOSTRUM REX UNDARUM" if i == 0 else "LEVIATHAN CORONA MARIS", 0.1, 5.0, 0.5, 1.1, 0.08)
                en.burn(0.5, 0.25, WHT, 0.8, 0.5)
            SK.engrave_face(atlas, m.part("up_sleeve_%d" % i), eng, only_faces=False)

        def prong(en, pc):
            en.text_col("TRIDENS", 0.0, Y_S + 46.0, 0.8, 1.8, 0.08, 0.4)
            en.burn(0.5, 0.25, WHT, 0.8, 0.5)
        SK.engrave_face(atlas, m.part("prong_c"), prong)
        SK.detail_all(m, atlas, skip=("up_wave_", "up_bubbles"))
        with SK.on_veins(atlas, ctx):
            for nm in ("prong_c", "prong_l", "prong_r"):
                SK.paint_veins(atlas, m.part(nm), TEAL, 1, 1.0, (5.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("shaft"), TEAL, 2, 0.9, (7.0, 0.8), 0.8)
            SK.paint_veins(atlas, m.part("up_wave_0"), WHT, 3, 1.4, (1.5, 0.6), 0.9)
            SK.paint_veins(atlas, m.part("coral"), CORAL, 4, 1.4, (1.5, 0.8), 0.8)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(80, 230, 235), rgb2=(255, 140, 110), length=3.2, tilt=14,
        ambient=[
            {"type": "motes", "anchor": "tip", "particle": "bubble", "rgb": [200, 245, 255], "rate": 14, "radius": 0.6, "rise": 0.03},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [80, 230, 235], "rate": 18, "radius": 0.7, "speed": 0.25, "size": 0.55, "tilt": 0.0},
            {"type": "drip", "anchor": "tip", "particle": "dripping_water", "rgb": [120, 220, 255], "rate": 8, "radius": 0.45},
            {"type": "orbit", "anchor": "tip", "particle": "dust", "rgb": [255, 140, 110], "rate": 6, "radius": 0.5, "speed": -0.3, "size": 0.4, "tilt": 35.0},
            {"type": "ground", "anchor": "feet", "particle": "bubble_pop", "rgb": [120, 220, 255], "rate": 10, "radius": 1.0, "speed": 0.07, "size": 0.5},
            {"type": "shaft", "anchor": "tip", "particle": "end_rod", "rgb": [200, 250, 255], "rate": 4, "height": 1.8},
        ],
        swing={"pattern": "wave", "particle": "dust", "rgb": [80, 230, 235], "rgb2": [225, 250, 255], "count": 4, "size": 0.7, "reach": 2.4},
        acts=[
            ("tidal_lance", [{"t": "beam", "from": "caster", "to": "to", "w": 1.0, "rgb": [80, 230, 235], "tex": "water_splash", "dur": 12},
                             {"t": "spiral", "at": "caster", "r": 1.0, "h": 6.0, "turns": 4, "n": 16, "rgb": [200, 245, 255], "dur": 14},
                             {"t": "burst", "at": "to", "n": 8, "len": 3.0, "rgb": [120, 220, 255], "tex": "water_splash", "dur": 14}]),
            ("maelstrom", [{"t": "glyph", "at": "caster", "r": 7.0, "rgb": [80, 230, 235], "tex": "spirit_spiral", "dur": 44, "spin": 0.3},
                           {"t": "ring", "at": "caster", "r0": 1.0, "r1": 8.0, "rgb": [200, 245, 255], "dur": 26, "tex": "water_splash"},
                           {"t": "orbit", "at": "caster", "r": 4.0, "n": 8, "rgb": [120, 220, 255], "tex": "glow", "dur": 44, "spin": 0.25, "y": 0.8, "size": 0.9}]),
            ("sea_kings_wrath", [{"t": "column", "at": "caster", "h": 14.0, "r": 1.4, "rgb": [80, 230, 235], "dur": 30},
                                 {"t": "rain", "at": "caster", "r": 10.0, "h": 14.0, "n": 24, "len": 3.0, "rgb": [200, 245, 255], "tex": "water_splash", "dur": 36},
                                 {"t": "ring", "at": "caster", "r0": 1.0, "r1": 12.0, "rgb": [255, 140, 110], "dur": 24, "tex": "rune_ring"}]),
        ],
        note="a sea king's crown: wave rings, bubbles, pearls, shells and kelp")


# =====================================================================================================================================================
# 8. Calamity Wave Aegis (The Rising of the Shield Hero): a heater shield of sea-blue steel with a gold border, a sapphire boss, three gold wave crests, rim studs, a strap behind.
#    Upgrade: a ring of sapphire beads and three crescent wave blades turning round the boss, five more wave crests, sea-fan fins along the top, scroll curls, a star of spikes round the boss.
# =====================================================================================================================================================
@item("calamity_wave_aegis")
class Aegis:
    builder = staticmethod(lambda: U.G2.build_calamity_wave_aegis())
    lod_keep = ("boss", "sapphire", "wave_crests")

    @staticmethod
    def extras(m, ctx):
        B = V(0.0, 8.0, 5.6)
        if not ctx.lod:
            rg = U.orbit_part(ctx, m, "up_bead_ring", 0, B, (0, 0, 1), 2.6, 0.0, True, "gold", (236, 194, 86))
            ml.sweep(rg, [tuple(q) for q in U.circle_pts(B, 11.0, (0, 0, 1), 30)], ml.circle(1.0, 4), scales=lambda t: (0.5, 0.5), caps=(False, False))
            for i in range(8):
                a = 2 * math.pi * i / 8
                ml.gem(rg, (B[0] + 11.0 * math.cos(a), B[1] + 11.0 * math.sin(a), B[2]), 1.0, 1.4, 5, axis="y")
            cs = U.orbit_part(ctx, m, "up_crescents", 1, B, (0, 0, 1), -1.6, 0.0, True, "crystal", (110, 190, 255), 35)
            for k in range(3):
                a0 = 360.0 / 3 * k
                pts = U.circle_pts(B + V(0, 0, -0.5), 17.0, (0, 0, 1), 8, a0, a0 + 70.0)
                ml.sweep(cs, [tuple(q) for q in pts], ml.bevelled(2.0, 2.0, 0.5), scales=lambda t: (0.0 if t < 0.02 or t > 0.98 else 1.6 * math.sin(math.pi * t) ** 0.7 + 0.1, 0.4), caps=(True, True), up=(0, 0, 1))
        # five more wave crests across the face (the old three stay)
        cr = mk(m, "up_wave_crests", "gold", (240, 204, 100), True, 40)
        for k, (y0, wid) in enumerate(((20.0, 19.0), (29.0, 14.0), (-37.0, 6.0))):
            pts = [(-wid + 2.0 * wid * i / 12.0, y0 + 2.4 * math.sin(i * 0.9 + k * 1.3), 2.5) for i in range(13)]
            ml.sweep(cr, pts, ml.lozenge(2.0, 2.0), scales=lambda t: (0.5 * (1 - 0.6 * abs(2 * t - 1) ** 2), 0.5), up=(0, 0, 1))
        # curls beside the boss
        sc = mk(m, "up_curls", "gold", (236, 194, 86), False, 55, uv_mode="loft")
        for sx in (-1, 1):
            pts = U.scroll2d(V(sx * 16.0, 8.0, 2.6), (V(1, 0, 0), V(0, 1, 0)), 1.6, 0.5, 4.2, U.S(14, 6), phase=0.0 if sx > 0 else math.pi)
            ml.sweep(sc, [tuple(q) for q in pts], ml.circle(1.0, U.S(4, 3)), scales=lambda t: (0.34, 0.34))
        # sea-fan fins along the top of the rim
        fn = mk(m, "up_fins", "crystal", (90, 170, 240), True, 35)
        for k in range(9):
            x = -21.0 + 5.25 * k
            top = 38.0 + 1.6 * math.cos(math.pi * (k - 4) / 8.0) * 0.0
            sh = 8.0 - abs(k - 4) * 0.9
            ml.loft(fn, V(0.0, 1.0, 0.0), V(x, top - 1.0, 0.4), [(0.0, 1.2, 0.35), (sh * 0.5, 1.6, 0.4), (sh, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1))
        # a star of eight spikes round the boss
        sp = mk(m, "up_boss_spikes", "gold", (240, 204, 100), False, 30)
        for k in range(8):
            a = 2 * math.pi * k / 8 + math.pi / 8
            U.spike3(sp, (B[0] + 6.6 * math.cos(a), B[1] + 6.6 * math.sin(a), 2.6), (B[0] + 10.0 * math.cos(a), B[1] + 10.0 * math.sin(a), 3.4), 0.9, 4)

    @staticmethod
    def post(m, atlas, ctx):
        SEA, GOLD, WHT = (110, 190, 255), (255, 226, 130), (225, 245, 255)
        SK.glow_parts(atlas, m, ["up_bead_ring", "up_crescents", "up_fins"], SEA, 1.0, 0.6)
        SK.glow_parts(atlas, m, ["up_wave_crests"], GOLD, 0.9, 0.5)
        SK.glow_parts(atlas, m, ["up_curls", "up_boss_spikes"], GOLD, 0.3, 0.3)
        face = m.part("face")

        def waves(en, pc):
            for k in range(7):
                y = -34.0 + 11.0 * k
                pts = [(-22.0 + 44.0 * i / 40.0, y + 1.4 * math.sin(i * 0.55 + k)) for i in range(41)]
                for a, b in zip(pts[:-1], pts[1:]):
                    en.line(a, b, 0.12)
            en.text_row("UNDA REDIT FLUCTUS", -9.0, -38.0, 1.1, 1.9, 0.14)
            en.burn(0.35, 0.2, SEA, 0.5, 0.4)
        SK.engrave_face(atlas, face, waves)
        SK.detail_all(m, atlas, skip=("up_bead_ring", "up_crescents"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, face, SEA, 1, 0.9, (6.0, 1.4), 0.8)
            SK.paint_veins(atlas, m.part("rim"), GOLD, 2, 1.0, (4.0, 0.8), 0.8)
            SK.paint_veins(atlas, m.part("up_fins"), WHT, 3, 1.4, (2.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("up_crescents"), WHT, 4, 1.4, (2.0, 1.0), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(110, 190, 255), rgb2=(255, 226, 130), length=1.05, tilt=0,
        ambient=[
            {"type": "ring", "anchor": "front", "particle": "dust", "rgb": [110, 190, 255], "rate": 18, "radius": 0.75, "speed": 0.2, "size": 0.55, "tilt": 90.0},
            {"type": "motes", "anchor": "front", "particle": "bubble", "rgb": [200, 235, 255], "rate": 12, "radius": 0.5, "rise": 0.03},
            {"type": "edge", "anchor": "front", "particle": "end_rod", "rgb": [225, 245, 255], "rate": 10, "radius": 0.45, "from": 0.0, "to": 1.0},
            {"type": "orbit", "anchor": "front", "particle": "dust", "rgb": [255, 226, 130], "rate": 8, "radius": 0.55, "speed": -0.3, "size": 0.45, "tilt": 90.0},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [110, 190, 255], "rate": 10, "radius": 1.1, "speed": 0.06, "size": 0.5},
            {"type": "drip", "anchor": "front", "particle": "dripping_water", "rgb": [120, 200, 255], "rate": 6, "radius": 0.4},
        ],
        swing={"pattern": "wave", "particle": "dust", "rgb": [110, 190, 255], "rgb2": [225, 245, 255], "count": 4, "size": 0.7, "reach": 1.2},
        acts=[
            ("wave_guard", [{"t": "glyph", "at": "caster", "r": 3.2, "rgb": [110, 190, 255], "tex": "barrier_hex_grid", "dur": 36, "spin": 0.0},
                            {"t": "ring", "at": "caster", "r0": 1.0, "r1": 3.4, "rgb": [225, 245, 255], "dur": 36, "tex": "barrier_ring"},
                            {"t": "orbit", "at": "caster", "r": 3.0, "n": 6, "rgb": [255, 226, 130], "tex": "barrier_shard", "dur": 36, "spin": 0.14, "y": 1.2, "size": 0.7}]),
            ("retaliating_surge", [{"t": "ring", "at": "caster", "r0": 0.5, "r1": 10.0, "rgb": [110, 190, 255], "dur": 18, "tex": "water_splash"},
                                   {"t": "ring", "at": "caster", "r0": 0.2, "r1": 8.0, "rgb": [225, 245, 255], "dur": 14, "tex": "rune_ring"},
                                   {"t": "burst", "at": "caster", "n": 12, "len": 5.0, "rgb": [110, 190, 255], "tex": "water_splash", "dur": 16}]),
            ("aegis_anthem", [{"t": "glyph", "at": "caster", "r": 8.0, "rgb": [255, 226, 130], "tex": "elf_circle", "dur": 50, "spin": 0.04},
                              {"t": "column", "at": "caster", "h": 12.0, "r": 1.2, "rgb": [110, 190, 255], "dur": 34},
                              {"t": "rain", "at": "caster", "r": 8.0, "h": 10.0, "n": 18, "len": 2.0, "rgb": [225, 245, 255], "tex": "mana_mote", "dur": 44}]),
        ],
        note="a shield that keeps the wave: bead ring, crescent blades, fins and curls")
