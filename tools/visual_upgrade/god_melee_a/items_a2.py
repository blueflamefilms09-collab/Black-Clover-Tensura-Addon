"""God melee (a), items 2 to 4: Wizard King's Vow, Wand of the Wisdom King, Hammer of Thunderbolts (see items_a.py for the structure)."""
import math

import numpy as np

import ukit as U
import skinkit as SK
from items_a import item, ITEMS                                         # noqa: F401

L, ml, w = U.L, U.ml, U.w
V, mk = U.V, U.mk


# =====================================================================================================================================================
# 2. Wizard King's Vow (Black Clover): a slim white rapier, a four-leaf clover guard of green and gold, a basket ring, a mana gem pommel.
#    Upgrade: petals of mana turning round the blade (Magic Zone), a gyroscope ring, a five-leaf crest on the blade root, clover wings, the cape in two silk ribbons,
#    a wire-wound grip, an engraved vow down both faces of the blade.
# =====================================================================================================================================================
@item("wizard_kings_vow")
class Vow:
    L0 = 24.0
    builder = staticmethod(lambda: U.G1.build_wizard_kings_vow())
    tube_cap = 24
    lod_keep = ("guard", "pommel")

    @staticmethod
    def extras(m, ctx):
        L0 = Vow.L0
        if not ctx.lod:
            pet = U.orbit_part(ctx, m, "up_petals", 0, (0, 66.0, 0), (0, 1, 0), 3.6, 0.0, True, "crystal", (120, 235, 150), 25)
            for i in range(6):
                a = 2 * math.pi * i / 6
                c = V(math.cos(a) * 6.4, 66.0 + 4.0 * math.sin(a * 2), math.sin(a) * 6.4)
                tangent = V(-math.sin(a), 0.0, math.cos(a))
                ml.loft(pet, tangent * 1.0 + V(0, 0.55, 0), c - tangent * 3.4, [(0.0, 0.1, 0.1), (2.4, 1.5, 0.36), (5.0, 1.8, 0.4), (7.5, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 1, 0))
            ring = U.orbit_part(ctx, m, "up_gyro", 1, (0, 88.0, 0), (0.2, 1, 0.1), -3.0, 30.0, True, "gold", (238, 198, 88))
            ml.sweep(ring, [tuple(q) for q in U.circle_pts(V(0, 88.0, 0), 5.0, (0.1, 1, 0.3), 24)], ml.circle(1.0, 4), scales=lambda t: (0.28, 0.28), caps=(False, False))
        # the five-leaf crest on the blade root, both faces
        cr = mk(m, "up_crest", "gold", (236, 196, 84), True, 40)
        for sg in (1, -1):
            for k in range(5):
                a = math.radians(90 + 72 * k)
                c0 = V(0.0, L0 + 8.5, sg * 0.5)
                tip = c0 + V(math.cos(a) * 3.4, math.sin(a) * 3.4, sg * 0.2)
                ml.loft(cr, tip - c0, c0, [(0.0, 0.2, 0.12), (1.5, 0.95, 0.2), (3.2, 0.5, 0.14), (3.5, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, sg))
        # wings of the guard: two leaves rising on each side of the blade root
        wg = mk(m, "up_clover_wings", "gold", (96, 196, 118), False, 40)
        for sx in (-1, 1):
            for k, (lx, ly) in enumerate(((4.6, 11.0), (7.0, 7.0))):
                ml.loft(wg, V(sx * lx, ly, 0.0), V(sx * 2.2, L0 + 1.5, 0.0), [(0.0, 0.3, 0.35), (ly * 0.35, 1.2, 0.4), (ly * 0.8, 0.7, 0.3), (math.hypot(lx, ly), 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1))
        # the basket: two more filigree bars
        fb = mk(m, "up_basket_filigree", "gold", (238, 198, 88), False, 55, uv_mode="loft")
        for sg in (1, -1):
            pts = [(sg * 6.4 * math.cos(t), L0 - 2.4 - 9.0 * (t / 1.2), sg * 6.4 * math.sin(t) * 0.6) for t in np.linspace(0, 1.2, U.S(8, 4))]
            ml.sweep(fb, pts, ml.circle(1.0, U.S(4, 3)), scales=lambda t: (0.34, 0.34))
        # the cape: two silk ribbons from the pommel
        if not ctx.lod:
            U.ribbon(m, "up_cape_a", (0.0, 1.0, 1.6), 24.0, 1.7, 2.2, 0.0, "cloth", (244, 244, 250), 10, (0, -1, 0), (0, 0, 1))
            U.ribbon(m, "up_cape_b", (0.0, 1.0, -1.6), 19.0, 1.4, 1.8, 2.0, "cloth", (70, 170, 100), 10, (0, -1, 0), (0, 0, 1))
        # gold wire wound on the grip
        wr = mk(m, "up_grip_wire", "gold", (240, 202, 96), False, 60, uv_mode="loft")
        pts = [(1.95 * math.cos(2 * math.pi * 5.0 * t), 6.0 + 16.0 * t, 1.95 * math.sin(2 * math.pi * 5.0 * t)) for t in np.linspace(0, 1, U.S(36, 12))]
        ml.sweep(wr, pts, ml.circle(1.0, 4), scales=lambda t: (0.2, 0.2))
        # five petals round the pommel gem
        pp = mk(m, "up_pommel_petals", "crystal", (120, 235, 150), True, 30)
        for k in range(5):
            a = 2 * math.pi * k / 5
            ml.loft(pp, V(math.cos(a), -0.2, math.sin(a)), V(0.0, 2.6, 0.0), [(0.0, 0.2, 0.2), (1.6, 0.9, 0.3), (3.6, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 1, 0))

    @staticmethod
    def post(m, atlas, ctx):
        GRN, GOLD, MINT = (130, 255, 170), (255, 224, 120), (200, 255, 220)
        SK.glow_parts(atlas, m, ["up_petals"], GRN, 1.0, 0.6)
        SK.glow_parts(atlas, m, ["up_gyro", "up_crest", "up_pommel_petals"], GOLD, 0.9, 0.55)
        SK.glow_parts(atlas, m, ["up_clover_wings"], GRN, 0.4, 0.4)
        SK.glow_parts(atlas, m, ["up_grip_wire", "up_basket_filigree"], GOLD, 0.25, 0.3)
        blade = m.part("blade")

        def vow(en, pc):
            en.text_col("TREFOIL VOW", 0.0, 112.0, 0.8, 1.7, 0.10, 0.45)
            en.burn(0.55, 0.25, MINT, 0.85, 0.5)
        SK.engrave_face(atlas, blade, vow)
        SK.detail_all(m, atlas, skip=("up_petals", "up_gyro"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, blade, GRN, 1, 0.8, (10.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("guard"), GOLD, 2, 1.0, (3.0, 1.5), 0.9)
            for nm in ("up_petals", "up_pommel_petals", "up_clover_wings"):
                SK.paint_veins(atlas, m.part(nm), GRN, 3, 1.4, (1.6, 1.6), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(130, 255, 170), rgb2=(255, 224, 120), length=1.22, tilt=28,
        ambient=[
            {"type": "motes", "anchor": "mid", "particle": "happy_villager", "rgb": [130, 255, 170], "rate": 9, "radius": 0.5, "rise": 0.02},
            {"type": "edge", "anchor": "blade", "particle": "end_rod", "rgb": [200, 255, 220], "rate": 12, "radius": 0.06, "from": 0.25, "to": 1.0},
            {"type": "ring", "anchor": "mid", "particle": "dust", "rgb": [255, 224, 120], "rate": 14, "radius": 0.55, "speed": 0.25, "size": 0.5, "tilt": 0.0},
            {"type": "orbit", "anchor": "tip", "particle": "dust", "rgb": [130, 255, 170], "rate": 8, "radius": 0.3, "speed": -0.4, "size": 0.45, "tilt": 70.0},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [120, 235, 150], "rate": 10, "radius": 1.1, "speed": 0.05, "size": 0.5},
            {"type": "shaft", "anchor": "tip", "particle": "end_rod", "rgb": [220, 255, 235], "rate": 3, "height": 1.2},
        ],
        swing={"pattern": "clover", "particle": "dust", "rgb": [130, 255, 170], "rgb2": [255, 224, 120], "count": 4, "size": 0.65, "reach": 1.4},
        acts=[
            ("clover_thrust", [{"t": "beam", "from": "caster", "to": "to", "w": 0.5, "rgb": [200, 255, 220], "dur": 10},
                               {"t": "spiral", "at": "caster", "r": 0.9, "h": 5.0, "turns": 3, "n": 14, "rgb": [130, 255, 170], "dur": 14},
                               {"t": "burst", "at": "to", "n": 5, "len": 3.0, "rgb": [255, 224, 120], "tex": "shard", "dur": 12}]),
            ("magic_zone", [{"t": "glyph", "at": "caster", "r": 7.0, "rgb": [130, 255, 170], "tex": "elf_circle", "dur": 44, "spin": 0.04},
                            {"t": "ring", "at": "caster", "r0": 1.0, "r1": 7.5, "rgb": [255, 224, 120], "dur": 24, "tex": "rune_ring"},
                            {"t": "rain", "at": "caster", "r": 6.5, "h": 7.0, "n": 14, "len": 1.6, "rgb": [170, 255, 200], "tex": "mana_mote", "dur": 40}]),
            ("kings_judgement", [{"t": "column", "at": "caster", "h": 16.0, "r": 1.0, "rgb": [200, 255, 220], "dur": 22},
                                 {"t": "glyph", "at": "caster", "r": 9.0, "rgb": [255, 224, 120], "tex": "magic_circle", "dur": 30, "spin": -0.08},
                                 {"t": "orbit", "at": "caster", "r": 3.0, "n": 5, "rgb": [130, 255, 170], "tex": "elf_circle", "dur": 30, "spin": 0.22, "y": 2.2, "size": 1.1},
                                 {"t": "flash", "at": "caster", "size": 7.0, "rgb": [235, 255, 245], "dur": 8}]),
        ],
        note="a Wizard King's rapier: mana petals, a five-leaf crest and the cape")


# =====================================================================================================================================================
# 3. Wand of the Wisdom King (Tensura): a slim crystal wand with a floating core, an orbit ring and three satellites.
#    Upgrade: the Great Sage's thought, drawn: three hexagonal gyroscope rings, a cloud of data shards, a turning interface disc behind the core, circuit traces on the shaft.
# =====================================================================================================================================================
@item("wand_of_the_wisdom_king")
class Wand:
    Y1 = 34.0
    builder = staticmethod(lambda: U.G2.build_wand_of_the_wisdom_king())
    lod_keep = ("core_crystal",)

    @staticmethod
    def extras(m, ctx):
        Y1 = Wand.Y1
        C = V(0, Y1 + 8.0, 0)
        if not ctx.lod:
            for k, (R, ax, sp, ph) in enumerate(((8.2, (0, 1, 0), 3.0, 0.0), (10.2, (1, 0, 0), -2.4, 20.0), (12.2, (0.5, 0.6, 0.6), 1.7, 55.0))):
                g = U.orbit_part(ctx, m, "up_hex_%d" % k, k, C, ax, sp, ph, True, "silver", (200, 220, 255) if k != 1 else (190, 170, 255))
                nrm = {0: (0, 1, 0), 1: (1, 0, 0), 2: (0.5, 0.6, 0.6)}[k]
                pts = U.circle_pts(C, R, nrm, 6)
                ml.sweep(g, [tuple(q) for q in pts], ml.circle(1.0, 4), scales=lambda t: (0.34, 0.34), caps=(False, False))
                for q in pts[:6]:
                    ml.gem(g, tuple(q), 0.62, 1.3, 4, axis="y")
            sh = U.orbit_part(ctx, m, "up_data", 3, C, (0.1, 1, 0.0), -2.0, 0.0, True, "crystal", (150, 200, 255), 25)
            for i in range(8):
                a = 2 * math.pi * i / 8
                pos = C + V(math.cos(a) * 9.0, 2.6 * math.sin(a * 3), math.sin(a) * 9.0)
                ml.box(sh, tuple(pos - 0.55), tuple(pos + 0.55), 0.22)
            dk = U.orbit_part(ctx, m, "up_disc", 4, V(0, Y1 + 8.0, -3.4), (0, 0, 1), 1.1, 0.0, True, "gold", (200, 214, 250))
            ml.sweep(dk, [tuple(q) for q in U.circle_pts(V(0, Y1 + 8.0, -3.4), 7.4, (0, 0, 1), 28)], ml.circle(1.0, 4), scales=lambda t: (0.3, 0.3), caps=(False, False))
            ml.sweep(dk, [tuple(q) for q in U.circle_pts(V(0, Y1 + 8.0, -3.4), 5.4, (0, 0, 1), 20)], ml.circle(1.0, 4), scales=lambda t: (0.2, 0.2), caps=(False, False))
            for i in range(6):
                a = math.pi * i / 6
                ml.bar(dk, (math.cos(a) * 7.2, Y1 + 8.0 + math.sin(a) * 7.2, -3.4), (-math.cos(a) * 7.2, Y1 + 8.0 - math.sin(a) * 7.2, -3.4), 0.34, 0.14, up=(0, 0, 1))
            tk = U.orbit_part(ctx, m, "up_disc_ticks", 4, V(0, Y1 + 8.0, -3.4), (0, 0, 1), 1.1, 0.0, True, "crystal", (190, 220, 255))
            for i in range(24):
                a = 2 * math.pi * i / 24
                d = V(math.cos(a), math.sin(a), 0)
                ml.loft(tk, d, V(0, Y1 + 8.0, -3.4) + d * 7.6, [(0.0, 0.26, 0.2), (1.0 if i % 2 == 0 else 0.55, 0.26, 0.2)], ml.rect_ring(2.0, 2.0), up=(0, 0, 1))
        # hexagonal collars with notches, and four fins on the cradle
        for i, y in enumerate((11.0, 25.0)):
            hc = mk(m, "up_hexcollar_%d" % i, "silver", (214, 222, 245), False, 30, uv_mode="loft")
            ml.lathe(hc, "y", (0, y, 0), [(0.0, 1.5), (0.3, 2.35), (1.6, 2.35), (1.9, 1.5)], sides=6)
        fn = mk(m, "up_cradle_fins", "crystal", (170, 200, 255), True, 30)
        for k in range(4):
            a = math.pi / 2 * k + math.pi / 4
            ml.loft(fn, V(math.cos(a) * 0.5, 1.0, math.sin(a) * 0.5), V(math.cos(a) * 1.4, Y1 + 1.0, math.sin(a) * 1.4), [(0.0, 0.2, 0.3), (3.0, 0.9, 0.35), (7.0, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(-math.sin(a), 0, math.cos(a)))
        bg = mk(m, "up_butt_gem", "crystal", (170, 200, 255), True, 30)
        ml.gem(bg, (0, -3.4, 0), 1.0, 1.4, 6, axis="y")

    @staticmethod
    def post(m, atlas, ctx):
        BLU, VIO, ICE = (150, 200, 255), (190, 160, 255), (220, 240, 255)
        SK.glow_prefix(atlas, m, "up_hex_", ICE, 0.9, 0.6)
        SK.glow_parts(atlas, m, ["up_data", "up_disc_ticks", "up_cradle_fins", "up_butt_gem"], BLU, 1.0, 0.6)
        SK.glow_parts(atlas, m, ["up_disc"], VIO, 0.7, 0.5)
        SK.glow_prefix(atlas, m, "up_hexcollar", BLU, 0.3, 0.3)
        shaft = m.part("shaft")

        def circuit(pc):
            # printed circuit traces: right-angle lines on the shaft's facets
            h, wd = pc.img.shape[:2]
            rng = np.random.default_rng(h * 31 + wd)
            en = SK.Engraver(pc)
            for _ in range(7):
                x = float(rng.uniform(pc.X.min(), pc.X.max()))
                y0 = float(rng.uniform(pc.Y.min(), pc.Y.max()))
                en.line((x, y0), (x, y0 + float(rng.uniform(2, 6))), 0.09)
                x2 = x + float(rng.choice([-0.5, 0.5]))
                en.line((x, y0 + 3.0), (x2, y0 + 3.0), 0.09)
                en.dot((x2, y0 + 3.0), 0.12)
            en.burn(0.35, 0.2, BLU, 0.9, 0.5)
        L.paint(atlas, shaft, circuit)
        for i in range(2):
            def eng(en, pc):
                en.text_row("SAGE MIND SAGE MIND", 0.1, 1.5, 0.28, 0.45, 0.05)
                en.burn(0.5, 0.2, BLU, 0.8, 0.5)
            SK.engrave_face(atlas, m.part("up_hexcollar_%d" % i), eng, only_faces=False)
        SK.detail_all(m, atlas, skip=("up_hex_", "up_data"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, shaft, ICE, 1, 1.4, (5.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("core_crystal"), BLU, 2, 1.6, (1.8, 1.8), 0.9)
            SK.paint_veins(atlas, m.part("up_cradle_fins"), VIO, 3, 1.4, (2.0, 1.0), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(150, 200, 255), rgb2=(190, 160, 255), length=1.0, tilt=18,
        ambient=[
            {"type": "motes", "anchor": "tip", "particle": "end_rod", "rgb": [190, 220, 255], "rate": 12, "radius": 0.35, "rise": 0.02},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [150, 200, 255], "rate": 16, "radius": 0.45, "speed": 0.3, "size": 0.45, "tilt": 0.0},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [190, 160, 255], "rate": 12, "radius": 0.6, "speed": -0.22, "size": 0.4, "tilt": 90.0},
            {"type": "arc", "anchor": "tip", "particle": "electric_spark", "rgb": [220, 240, 255], "rate": 5, "radius": 0.55},
            {"type": "orbit", "anchor": "tip", "particle": "dust", "rgb": [210, 230, 255], "rate": 8, "radius": 0.38, "speed": 0.5, "size": 0.35, "tilt": 35.0},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [150, 190, 255], "rate": 9, "radius": 0.8, "speed": -0.07, "size": 0.45},
        ],
        swing={"pattern": "circuit", "particle": "dust", "rgb": [150, 200, 255], "rgb2": [190, 160, 255], "count": 3, "size": 0.55, "reach": 1.1},
        acts=[
            ("sagely_barrage", [{"t": "orbit", "at": "caster", "r": 1.6, "n": 8, "rgb": [150, 200, 255], "tex": "spark", "dur": 14, "spin": 0.35, "y": 1.6, "size": 0.5},
                                {"t": "beam", "from": "caster", "to": "to", "w": 0.35, "rgb": [200, 225, 255], "dur": 12},
                                {"t": "burst", "at": "to", "n": 10, "len": 2.6, "rgb": [190, 160, 255], "tex": "spark", "dur": 12}]),
            ("absolute_defence", [{"t": "glyph", "at": "caster", "r": 3.0, "rgb": [150, 200, 255], "tex": "barrier_hex_grid", "dur": 40, "spin": 0.0},
                                  {"t": "ring", "at": "caster", "r0": 3.0, "r1": 3.4, "rgb": [220, 240, 255], "dur": 40, "tex": "barrier_ring"},
                                  {"t": "orbit", "at": "caster", "r": 3.0, "n": 6, "rgb": [190, 160, 255], "tex": "barrier_shard", "dur": 40, "spin": -0.12, "y": 1.2, "size": 0.7}]),
            ("accelerated_thought", [{"t": "glyph", "at": "caster", "r": 4.5, "rgb": [190, 160, 255], "tex": "magic_circle", "dur": 50, "spin": 0.3},
                                     {"t": "spiral", "at": "caster", "r": 1.0, "h": 6.0, "turns": 5, "n": 20, "rgb": [150, 200, 255], "dur": 34},
                                     {"t": "rain", "at": "caster", "r": 3.5, "h": 6.0, "n": 14, "len": 1.2, "rgb": [220, 240, 255], "tex": "mana_mote", "dur": 44}]),
        ],
        note="the Great Sage's thought drawn as gyroscope rings, data shards and an interface disc")


# =====================================================================================================================================================
# 4. Hammer of Thunderbolts (Dungeons and Dragons): a huge blue-grey steel head with gold bands and lightning engraved, on an iron haft with a lanyard ring.
#    Upgrade: forked bolts circling the head, storm orbs, lightning rods crowning the head, rivet rows, end plates, a chain with a bolt charm, anvil flanges, a thunder-script haft.
# =====================================================================================================================================================
@item("hammer_of_thunderbolts")
class Hammer:
    YH = 112.0
    builder = staticmethod(lambda: U.G1.build_hammer_of_thunderbolts())
    lod_keep = ("bands", "up_rods")

    @staticmethod
    def extras(m, ctx):
        YH = Hammer.YH
        HC = V(0, YH + 11.0, 0)
        if not ctx.lod:
            for k, x in enumerate((-13.0, 0.0, 13.0)):
                bt = U.orbit_part(ctx, m, "up_bolt_%d" % k, 0, HC, (1, 0, 0), 5.0 * (1 if k != 1 else -1), 40.0 * k, True, "crystal", (190, 225, 255), 25)
                pts = []
                n = 22
                for i in range(n + 1):
                    a = 2 * math.pi * i / n
                    r = 17.0 + (2.0 if i % 2 == 0 else -2.0) * (0.0 if i in (0, n) else 1.0)
                    pts.append((x, HC[1] + r * math.cos(a), r * math.sin(a)))
                ml.sweep(bt, pts, ml.circle(1.0, 4), scales=lambda t: (0.36, 0.36), caps=(False, False))
            orb = U.orbit_part(ctx, m, "up_storm_orbs", 1, HC, (0, 0, 1), 2.2, 0.0, True, "crystal", (140, 200, 255), 30)
            for i in range(4):
                a = 2 * math.pi * i / 4 + 0.4
                w.ellipsoid(orb, (HC[0] + 31.0 * math.cos(a), HC[1] + 31.0 * math.sin(a), 0.0), 1.9, 1.9, 1.9, sides=8, stacks=5)
        rods = mk(m, "up_rods", "silver", (210, 226, 250), True, 30)
        for i, x in enumerate((-17.0, -8.5, 0.0, 8.5, 17.0)):
            U.spike3(rods, (x, YH + 22.5, 0.0), (x, YH + 22.5 + (11.0 if i % 2 == 0 else 7.5), 0.0), 0.8, 5)
        st = mk(m, "up_rivets", "gold", (236, 194, 86), False, 40)
        for sz in (12.0, -12.0):
            for x in (-20.0, -11.5, -2.0, 2.0, 11.5, 20.0):
                for y in (YH + 3.5, YH + 18.5):
                    ml.stud(st, (x, y, sz), 0.95, 0.9, (0, 0, 1 if sz > 0 else -1), U.S(6, 4), 0.55)
        for sx in (-1, 1):
            ep = mk(m, "up_endplate_%s" % ("r" if sx > 0 else "l"), "gold", (230, 186, 78), False, 40)
            ml.loft(ep, "x", (0, YH + 11.0, 0), [sx * 22.4, sx * 24.6], ml.octagon(17.0, 17.0, 4.0))
        # the lanyard chain with a bolt charm
        U.chain_links(m, "up_chain", (0.0, 4.2, 4.8), (0.0, -14.0, 4.8), 6, 1.5, "iron", (110, 118, 140), up=(0, 0, 1), sag=2.0)
        ch = mk(m, "up_charm", "crystal", (170, 215, 255), True, 35)
        zig = [(-1.8, 0.0), (0.6, -2.4), (-0.6, -3.2), (1.8, -6.4), (0.0, -6.8), (-1.2, -9.0)]
        for a, b in zip(zig[:-1], zig[1:]):
            ml.bar(ch, (a[0], -15.0 + a[1], 4.8), (b[0], -15.0 + b[1], 4.8), 1.1, 0.4, up=(0, 0, 1))
        # the anvil flanges of the butt
        fl = mk(m, "up_butt_flanges", "iron", (96, 104, 124), False, 35)
        for k in range(4):
            a = math.pi / 2 * k + math.pi / 4
            ml.loft(fl, V(math.cos(a) * 0.55, -0.5, math.sin(a) * 0.55), V(math.cos(a) * 2.2, 1.0, math.sin(a) * 2.2), [(0.0, 0.3, 0.5), (3.0, 1.0, 0.7), (6.0, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 1, 0))

    @staticmethod
    def post(m, atlas, ctx):
        ICE, WHT, GOLD = (140, 205, 255), (225, 240, 255), (255, 226, 130)
        SK.glow_prefix(atlas, m, "up_bolt_", WHT, 1.0, 0.7)
        SK.glow_parts(atlas, m, ["up_storm_orbs", "up_rods", "up_charm"], ICE, 1.0, 0.65)
        SK.glow_parts(atlas, m, ["up_rivets"], GOLD, 0.25, 0.3)
        head = m.part("head")
        YH_ENG = Hammer.YH + 20.0

        def thunder(en, pc):
            en.text_row("TONITRUS", -10.5, YH_ENG - 6.0, 2.2, 3.0, 0.2, 0.2)
            en.burn(0.5, 0.25, ICE, 0.8, 0.5)
        SK.engrave_face(atlas, head, thunder)

        def haft(en, pc):
            en.text_col("FULGUR TONITRUS DEI", 0.0, 95.0, 0.9, 2.0, 0.1, 0.4)
            en.burn(0.45, 0.2, ICE, 0.7, 0.5)
        SK.engrave_face(atlas, m.part("haft"), haft, only_faces=False)
        SK.detail_all(m, atlas, skip=("up_bolt_", "up_storm"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, head, ICE, 1, 1.4, (6.0, 3.0), 0.95)
            SK.paint_veins(atlas, m.part("haft"), ICE, 2, 1.0, (10.0, 1.2), 0.8)
            SK.paint_veins(atlas, m.part("bands"), GOLD, 3, 1.0, (4.0, 2.0), 0.9)
            SK.paint_veins(atlas, m.part("up_rods"), WHT, 4, 1.4, (2.0, 1.0), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(140, 205, 255), rgb2=(225, 240, 255), length=1.6, tilt=20,
        ambient=[
            {"type": "arc", "anchor": "tip", "particle": "electric_spark", "rgb": [190, 225, 255], "rate": 16, "radius": 0.9},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [140, 205, 255], "rate": 16, "radius": 0.7, "speed": 0.28, "size": 0.5, "tilt": 90.0},
            {"type": "motes", "anchor": "tip", "particle": "electric_spark", "rgb": [225, 240, 255], "rate": 8, "radius": 0.6, "rise": 0.04},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [120, 190, 255], "rate": 10, "radius": 1.2, "speed": 0.09, "size": 0.5},
            {"type": "shaft", "anchor": "tip", "particle": "end_rod", "rgb": [200, 230, 255], "rate": 4, "height": 2.0},
            {"type": "edge", "anchor": "blade", "particle": "electric_spark", "rgb": [190, 225, 255], "rate": 6, "radius": 0.15, "from": 0.0, "to": 0.4},
        ],
        swing={"pattern": "bolt", "particle": "electric_spark", "rgb": [190, 225, 255], "rgb2": [255, 255, 255], "count": 5, "size": 0.7, "reach": 1.6},
        acts=[
            ("thunderclap", [{"t": "ring", "at": "caster", "r0": 0.5, "r1": 10.0, "rgb": [190, 225, 255], "dur": 14, "tex": "rune_ring"},
                             {"t": "arcs", "at": "caster", "n": 6, "r": 5.0, "h": 6.0, "rgb": [225, 240, 255], "dur": 14},
                             {"t": "flash", "at": "caster", "size": 5.0, "rgb": [235, 245, 255], "dur": 6}]),
            ("hurled_bolt", [{"t": "beam", "from": "caster", "to": "to", "w": 0.8, "rgb": [190, 225, 255], "dur": 10},
                             {"t": "arcs", "at": "to", "n": 5, "r": 2.5, "h": 4.0, "rgb": [235, 245, 255], "dur": 12},
                             {"t": "burst", "at": "to", "n": 8, "len": 3.5, "rgb": [140, 205, 255], "tex": "spark", "dur": 12}]),
            ("tempest", [{"t": "glyph", "at": "caster", "r": 9.0, "rgb": [140, 205, 255], "tex": "magic_circle", "dur": 40, "spin": 0.12},
                         {"t": "rain", "at": "caster", "r": 10.0, "h": 16.0, "n": 24, "len": 4.0, "rgb": [200, 230, 255], "tex": "spark", "dur": 36},
                         {"t": "arcs", "at": "caster", "n": 8, "r": 10.0, "h": 15.0, "rgb": [235, 245, 255], "dur": 32},
                         {"t": "flash", "at": "caster", "size": 8.0, "rgb": [235, 245, 255], "dur": 8}]),
        ],
        note="a storm in a hammer: bolts circling the head, orbs, rods and a thunder script")
