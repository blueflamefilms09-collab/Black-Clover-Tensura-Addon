"""God melee (a), items 9 to 12: Hellblaze the Demon Sin, Minotaur King's Labrys, Dragon Force Twin Fangs, Dawnpiercer Crossbow (see items_a.py for the structure)."""
import math

import numpy as np

import ukit as U
import skinkit as SK
from items_a import item, ITEMS                                         # noqa: F401

L, ml, w = U.L, U.ml, U.w
V, mk = U.V, U.mk


def flame_tongue(part, base, height, width, lean=(0.0, 0.0), sway=0.0, thick=0.35):
    """A tongue of flame: a flat leaf from `base` rising `height`, leaning and curling at the tip; the flat faces the viewer (+z)."""
    b = np.asarray(base, float)
    tip = b + V(lean[0], height, lean[1])
    mid = b + V(lean[0] * 0.4 + sway, height * 0.55, lean[1] * 0.4)
    d1 = mid - b
    d2 = tip - mid
    ml.loft(part, d1, b, [(0.0, width * 0.7, thick), (float(np.linalg.norm(d1)) * 0.5, width, thick), (float(np.linalg.norm(d1)), width * 0.75, thick * 0.8)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1), caps=(True, False))
    ml.loft(part, d2, mid, [(0.0, width * 0.75, thick * 0.8), (float(np.linalg.norm(d2)) * 0.6, width * 0.4, thick * 0.5), (float(np.linalg.norm(d2)), 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1), caps=(False, True))


# =====================================================================================================================================================
# 9. Hellblaze, the Demon Sin (The Seven Deadly Sins): a black broadsword with a jagged torn edge, lava-red veins in the fuller, a horned demon guard, a flame-red grip.
#    Upgrade: six tongues of black flame turning round the blade, a demon ring of thorns at the guard, the Sin's eye on the guard, a second pair of horns, a spiked ball on a chain,
#    a crown of flames at the pommel, extra teeth along the torn edge, the name engraved in the demon script.
# =====================================================================================================================================================
@item("hellblaze_the_demon_sin")
class Hellblaze:
    L0 = 26.0
    builder = staticmethod(lambda: U.G2.build_hellblaze_the_demon_sin())
    lod_keep = ("guard", "vein")

    @staticmethod
    def extras(m, ctx):
        L0 = Hellblaze.L0
        if not ctx.lod:
            fl = U.orbit_part(ctx, m, "up_flames", 0, V(0, 70.0, 0), (0, 1, 0), 4.0, 0.0, True, "obsidian", (255, 110, 40), 30)
            for i in range(6):
                a = 2 * math.pi * i / 6
                d = V(math.cos(a), 0, math.sin(a))
                c = V(0, 56.0 + 4.0 * (i % 2), 0) + d * 7.2
                tmp = ml.Part("t")
                flame_tongue(tmp, (0.0, 0.0, 0.0), 15.0 + 3.0 * (i % 3), 1.5, (0.0, 0.0), 1.2)
                tr = tmp.rotated("y", -math.degrees(a) + 90.0).translated(*c)
                off = len(fl.V)
                fl.V.extend(tr.V)
                fl.F.extend([tuple(i2 + off for i2 in f) for f in tr.F])
                fl.tags.extend(tr.tags)
            th = U.orbit_part(ctx, m, "up_thorn_ring", 1, V(0, L0 + 1.0, 0), (0, 1, 0), -2.2, 0.0, True, "obsidian", (60, 40, 70), 35)
            ml.sweep(th, [tuple(q) for q in U.circle_pts(V(0, L0 + 1.0, 0), 9.0, (0, 1, 0), 24)], ml.circle(1.0, 4), scales=lambda t: (0.4, 0.4), caps=(False, False))
            for i in range(8):
                a = 2 * math.pi * i / 8
                d = V(math.cos(a), 0, math.sin(a))
                U.spike3(th, tuple(V(0, L0 + 1.0, 0) + d * 9.0), tuple(V(0, L0 + 4.6, 0) + d * 11.6), 0.7, 4)
        # the Sin's eye on both faces of the guard
        ey = mk(m, "up_sin_eye", "crystal", (255, 70, 40), True, 30)
        for sg in (1, -1):
            w.ellipsoid(ey, (0.0, L0 + 3.6, sg * 1.25), 1.5, 0.7, 0.55, sides=8, stacks=4)
        # a second pair of horns, small, above the main ones
        hr = mk(m, "up_horns", "iron", (50, 38, 62), False, 35)
        for sx in (-1, 1):
            pts = [(sx * (2.0 + 5.0 * (i / 8.0) ** 0.8), L0 + 5.0 + 8.0 * (i / 8.0) ** 2.0, 0.0) for i in range(9)]
            ml.sweep(hr, pts, ml.circle(1.0, U.S(5, 3)), scales=lambda t: (0.9 * (1 - 0.95 * t), 0.9 * (1 - 0.95 * t)), caps=(True, True))
        # a chain with a spiked ball from the pommel
        if not ctx.lod:
            U.chain_links(m, "up_chain", (0.0, 1.0, 0.0), (0.0, -16.0, 0.0), 5, 1.5, "iron", (70, 66, 84), up=(0, 0, 1), sag=0.0)
            bl = mk(m, "up_spiked_ball", "iron", (56, 50, 66), False, 40)
            w.ellipsoid(bl, (0.0, -20.0, 0.0), 2.6, 2.6, 2.6, sides=8, stacks=5)
            for k in range(8):
                a = 2 * math.pi * k / 8
                for e in (0.0, 1.0):
                    d = V(math.cos(a) * math.cos(0.9 * e), 0.9 * e * 0.0 + math.sin(0.9 * e), math.sin(a) * math.cos(0.9 * e))
                    U.spike3(bl, tuple(V(0, -20.0, 0) + d * 2.4), tuple(V(0, -20.0, 0) + d * 4.9), 0.6, 4)
        # teeth along the torn edge
        tt = mk(m, "up_teeth", "obsidian", (40, 30, 52), False, 30)
        for k in range(14):
            y = L0 + 14.0 + k * 6.4
            hw = 4.4 if y < 100 else 3.4
            U.spike3(tt, (-hw, y, 0.0), (-hw - 2.0, y + 2.6, 0.0), 0.6, 4)
        # flames crowning the pommel
        cf = mk(m, "up_pommel_flames", "obsidian", (255, 120, 50), True, 30)
        for k in range(5):
            a = 2 * math.pi * k / 5
            flame_tongue(cf, (2.0 * math.cos(a), 5.6, 2.0 * math.sin(a)), 4.5 + (k % 2) * 1.5, 0.6, (math.cos(a) * 0.8, math.sin(a) * 0.8), 0.3, 0.25)

    @staticmethod
    def post(m, atlas, ctx):
        L0 = Hellblaze.L0
        LAV, RED, EMB = (255, 100, 40), (255, 70, 40), (255, 170, 80)
        SK.glow_parts(atlas, m, ["up_flames", "up_pommel_flames"], LAV, 1.0, 0.6)
        SK.glow_parts(atlas, m, ["up_sin_eye"], RED, 1.0, 0.8)
        SK.glow_parts(atlas, m, ["up_thorn_ring"], (255, 90, 50), 0.5, 0.4)
        SK.glow_parts(atlas, m, ["up_teeth"], (255, 80, 40), 0.18, 0.3)
        blade = m.part("blade")

        def name(en, pc):
            en.text_col("HELLBLAZE", 0.0, 112.0, 1.4, 2.4, 0.16, 0.45)
            en.text_col("SIN OF WRATH", 0.0, 112.0 - 9 * 3.5 - 6.0, 1.2, 2.0, 0.14, 0.45) if False else None
            en.burn(0.6, 0.18, EMB, 0.9, 0.5)
        SK.engrave_face(atlas, blade, name)
        SK.detail_all(m, atlas, skip=("up_flames", "up_thorn", "up_pommel_flames"))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, blade, LAV, 1, 1.2, (12.0, 1.6), 0.95)
            SK.paint_veins(atlas, m.part("guard"), LAV, 2, 1.0, (3.0, 1.2), 0.9)
            SK.paint_veins(atlas, m.part("up_horns"), RED, 3, 1.0, (3.0, 0.8), 0.9)
            SK.paint_veins(atlas, m.part("up_spiked_ball"), LAV, 4, 1.0, (1.5, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("up_flames"), EMB, 5, 1.6, (2.0, 1.0), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(255, 100, 40), rgb2=(255, 170, 80), length=1.62, tilt=26,
        ambient=[
            {"type": "motes", "anchor": "mid", "particle": "flame", "rgb": [255, 110, 40], "rate": 16, "radius": 0.45, "rise": 0.04},
            {"type": "edge", "anchor": "blade", "particle": "smoke", "rgb": [40, 30, 50], "rate": 12, "radius": 0.1, "from": 0.2, "to": 1.0},
            {"type": "ring", "anchor": "mid", "particle": "dust", "rgb": [255, 90, 40], "rate": 14, "radius": 0.5, "speed": 0.3, "size": 0.55, "tilt": 0.0},
            {"type": "drip", "anchor": "tip", "particle": "dripping_lava", "rgb": [255, 110, 40], "rate": 6, "radius": 0.3},
            {"type": "ground", "anchor": "feet", "particle": "flame", "rgb": [255, 100, 40], "rate": 10, "radius": 1.0, "speed": 0.07, "size": 0.5},
            {"type": "arc", "anchor": "tip", "particle": "lava", "rgb": [255, 170, 80], "rate": 4, "radius": 0.5},
        ],
        swing={"pattern": "flame", "particle": "flame", "rgb": [255, 100, 40], "rgb2": [40, 30, 50], "count": 5, "size": 0.8, "reach": 1.7},
        acts=[
            ("hellblaze_slash", [{"t": "beam", "from": "caster", "to": "to", "w": 1.6, "rgb": [255, 100, 40], "tex": "flame_trail", "dur": 14},
                                 {"t": "burst", "at": "to", "n": 8, "len": 3.6, "rgb": [255, 170, 80], "tex": "flame_trail", "dur": 14},
                                 {"t": "flash", "at": "to", "size": 3.0, "rgb": [255, 200, 120], "dur": 6}]),
            ("demon_mark", [{"t": "glyph", "at": "caster", "r": 6.0, "rgb": [255, 70, 40], "tex": "devil_circle", "dur": 44, "spin": 0.06},
                            {"t": "ring", "at": "caster", "r0": 1.0, "r1": 7.0, "rgb": [255, 110, 40], "dur": 26, "tex": "rune_ring"},
                            {"t": "column", "at": "caster", "h": 10.0, "r": 0.9, "rgb": [255, 90, 40], "dur": 24}]),
            ("black_inferno", [{"t": "glyph", "at": "caster", "r": 9.0, "rgb": [255, 70, 40], "tex": "devil_circle", "dur": 44, "spin": -0.07},
                               {"t": "rain", "at": "caster", "r": 9.0, "h": 12.0, "n": 22, "len": 4.0, "rgb": [255, 110, 40], "tex": "flame_trail", "dur": 40},
                               {"t": "burst", "at": "caster", "n": 12, "len": 8.0, "rgb": [255, 170, 80], "tex": "flame_trail", "dur": 24},
                               {"t": "flash", "at": "caster", "size": 8.0, "rgb": [255, 200, 120], "dur": 8}]),
        ],
        note="a demon sin: black flames turning round the blade, a thorn ring, the Sin's eye")


# =====================================================================================================================================================
# 10. Minotaur King's Labrys (DanMachi): a huge double-headed axe of dark iron with serrated edges, long horn tips, ember cracks, a bone-and-leather haft, a horned pommel.
#     Upgrade: a halo of twelve spectral axe heads turning in front of the hub, a bull's brow and nose ring on the hub, bone trophies hanging from the haft, a serrated rim of teeth,
#     studded bands, a spiked butt, a labyrinth etched in both blades.
# =====================================================================================================================================================
@item("minotaur_kings_labrys")
class Labrys:
    YC = 118.0
    builder = staticmethod(lambda: U.G2.build_minotaur_kings_labrys())
    lod_keep = ("horns", "hub_gem")

    @staticmethod
    def extras(m, ctx):
        YC = Labrys.YC
        HUB = V(0.0, YC, 0.0)
        if not ctx.lod:
            sp = U.orbit_part(ctx, m, "up_spectral_axes", 0, HUB, (0, 0, 1), 2.0, 0.0, True, "crystal", (255, 110, 60), 30)
            for zz in (4.8, -4.8):
                for i in range(6):
                    a = 2 * math.pi * i / 6 + (0.0 if zz > 0 else math.pi / 6)
                    d = V(math.cos(a), math.sin(a), 0.0)
                    t = V(-math.sin(a), math.cos(a), 0.0)
                    c = HUB + d * 22.0 + V(0, 0, zz)
                    ml.loft(sp, d, c - d * 1.0, [(0.0, 0.5, 0.3), (1.6, 3.0, 0.4), (3.2, 2.4, 0.34), (5.4, 1.0, 0.24), (7.4, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1))
        # the bull's brow and nose ring on the hub (front face)
        br = mk(m, "up_bull_brow", "bone", (236, 224, 196), False, 35)
        for sx in (-1, 1):
            pts = [(sx * (3.0 + 6.5 * (i / 8.0)), YC + 4.4 + 5.0 * math.sin(math.pi * (i / 8.0) * 0.9), 3.8 + 0.8 * (i / 8.0)) for i in range(9)]
            ml.sweep(br, pts, ml.circle(1.0, U.S(5, 3)), scales=lambda t: (1.1 * (1 - 0.9 * t), 1.1 * (1 - 0.9 * t)), caps=(True, True))
        nr = mk(m, "up_nose_ring", "gold", (232, 188, 80), False, 55, uv_mode="loft")
        ml.sweep(nr, [tuple(q) for q in U.circle_pts(V(0, YC - 5.6, 4.6), 2.4, (0, 0, 1), U.S(14, 8))], ml.circle(1.0, 4), scales=lambda t: (0.42, 0.42), caps=(False, False))
        # serrated rim teeth
        tt = mk(m, "up_rim_teeth", "iron", (66, 62, 76), False, 30)
        for sgn in (1, -1):
            for k in range(13):
                a = math.radians(-66 + 11 * k)
                r = 34.0
                base = V(sgn * (4.0 + r * math.cos(a)), YC + 31.0 * math.sin(a), 0.0)
                out = V(sgn * math.cos(a), math.sin(a), 0.0)
                U.spike3(tt, tuple(base), tuple(base + out * 3.6), 0.9, 4)
        # trophies: bone teeth on cords hanging from the haft
        if not ctx.lod:
            for k in range(3):
                a = 2 * math.pi * k / 3
                tube = mk(m, "up_trophy_cord_%d" % k, "leather", (90, 60, 40), False, 55, uv_mode="loft")
                c0 = V(2.5 * math.cos(a), 84.0, 2.5 * math.sin(a))
                pts = [tuple(c0 + V(0.6 * math.cos(a) * (i / 5.0), -9.0 * (i / 5.0), 0.6 * math.sin(a) * (i / 5.0))) for i in range(6)]
                ml.sweep(tube, pts, ml.circle(1.0, 4), scales=lambda t: (0.22, 0.22))
                tb = mk(m, "up_trophy_teeth_%d" % k, "bone", (240, 232, 204), False, 30)
                for j in range(3):
                    p0 = c0 + V(0.6 * math.cos(a), -2.5 - 2.4 * j, 0.6 * math.sin(a))
                    U.spike3(tb, tuple(p0), tuple(p0 + V(0.6 * math.cos(a), -2.6, 0.6 * math.sin(a))), 0.55, 4)
        # studs on the iron bands and a spiked butt
        sb = U.blocks_ring(m, "up_band_studs", V(0, 70.0, 0), 2.9, 8, 1.0, 1.2, 0.8, "iron", (84, 80, 94))
        sb2 = U.blocks_ring(m, "up_band_studs_b", V(0, 100.0, 0), 2.9, 8, 1.0, 1.2, 0.8, "iron", (84, 80, 94))
        bs = mk(m, "up_butt_spikes", "iron", (84, 80, 94), False, 30)
        for k in range(4):
            a = math.pi / 2 * k
            U.spike3(bs, (2.4 * math.cos(a), -3.0, 2.4 * math.sin(a)), (4.2 * math.cos(a), -9.0, 4.2 * math.sin(a)), 0.7, 4)

    @staticmethod
    def post(m, atlas, ctx):
        YC = Labrys.YC
        EMB, ORG, BONE = (255, 110, 50), (255, 170, 90), (255, 236, 200)
        SK.glow_parts(atlas, m, ["up_spectral_axes"], EMB, 1.0, 0.55)
        SK.glow_parts(atlas, m, ["up_nose_ring"], ORG, 0.3, 0.3)
        SK.glow_parts(atlas, m, ["up_rim_teeth"], EMB, 0.2, 0.3)

        def maze(en, pc):
            # a labyrinth: concentric squares with a gap that turns a quarter at every ring
            cx, cy = (22.0 if np.mean(pc.X) >= 0 else -22.0), YC
            for k in range(1, 8):
                r = 2.0 * k
                gap = k % 4
                pts = [(cx - r, cy - r), (cx + r, cy - r), (cx + r, cy + r), (cx - r, cy + r), (cx - r, cy - r)]
                for s in range(4):
                    if s == gap:
                        continue
                    en.line(pts[s], pts[s + 1], 0.28)
            en.burn(0.55, 0.25, EMB, 0.7, 0.5)
        for nm in ("blade_r", "blade_l"):
            SK.engrave_face(atlas, m.part(nm), maze)

        def haft(en, pc):
            en.text_col("MINOS REX LABYRINTHI", 0.0, 120.0, 1.1, 2.4, 0.13, 0.4)
            en.burn(0.5, 0.2, EMB, 0.7, 0.5)
        SK.engrave_face(atlas, m.part("haft"), haft, only_faces=False)
        SK.detail_all(m, atlas, skip=("up_spectral",))
        with SK.on_veins(atlas, ctx):
            SK.paint_veins(atlas, m.part("blade_r"), EMB, 1, 1.0, (7.0, 2.0), 0.9)
            SK.paint_veins(atlas, m.part("blade_l"), EMB, 2, 1.0, (7.0, 2.0), 0.9)
            SK.paint_veins(atlas, m.part("hub"), ORG, 3, 1.2, (2.0, 1.2), 0.9)
            SK.paint_veins(atlas, m.part("up_spectral_axes"), BONE, 4, 1.6, (2.0, 0.9), 0.9)
            SK.paint_veins(atlas, m.part("haft"), EMB, 5, 0.8, (10.0, 1.0), 0.8)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(255, 110, 50), rgb2=(255, 236, 200), length=1.85, tilt=18,
        ambient=[
            {"type": "motes", "anchor": "tip", "particle": "flame", "rgb": [255, 110, 50], "rate": 12, "radius": 0.8, "rise": 0.03},
            {"type": "ring", "anchor": "tip", "particle": "dust", "rgb": [255, 110, 50], "rate": 16, "radius": 0.8, "speed": 0.2, "size": 0.55, "tilt": 90.0},
            {"type": "edge", "anchor": "tip", "particle": "lava", "rgb": [255, 150, 70], "rate": 8, "radius": 0.9, "from": 0.0, "to": 1.0},
            {"type": "drip", "anchor": "tip", "particle": "dripping_lava", "rgb": [255, 110, 50], "rate": 6, "radius": 0.7},
            {"type": "ground", "anchor": "feet", "particle": "smoke", "rgb": [60, 50, 50], "rate": 10, "radius": 1.1, "speed": 0.05, "size": 0.5},
            {"type": "arc", "anchor": "tip", "particle": "crit", "rgb": [255, 200, 140], "rate": 4, "radius": 0.9},
        ],
        swing={"pattern": "crescent", "particle": "dust", "rgb": [255, 110, 50], "rgb2": [255, 236, 200], "count": 5, "size": 0.9, "reach": 1.9},
        acts=[
            ("labrys_cleave", [{"t": "burst", "at": "caster", "n": 10, "len": 6.0, "rgb": [255, 110, 50], "tex": "spark", "dur": 14},
                               {"t": "ring", "at": "caster", "r0": 0.6, "r1": 7.0, "rgb": [255, 170, 90], "dur": 14, "tex": "rune_ring"},
                               {"t": "flash", "at": "caster", "size": 4.0, "rgb": [255, 220, 170], "dur": 6}]),
            ("bull_charge", [{"t": "beam", "from": "caster", "to": "to", "w": 2.0, "rgb": [255, 110, 50], "tex": "flame_trail", "dur": 14},
                             {"t": "spiral", "at": "caster", "r": 1.2, "h": 3.0, "turns": 3, "n": 14, "rgb": [255, 170, 90], "dur": 14},
                             {"t": "burst", "at": "to", "n": 12, "len": 4.0, "rgb": [255, 110, 50], "tex": "spark", "dur": 14}]),
            ("kings_roar", [{"t": "ring", "at": "caster", "r0": 0.5, "r1": 12.0, "rgb": [255, 110, 50], "dur": 20, "tex": "rune_ring"},
                            {"t": "ring", "at": "caster", "r0": 0.5, "r1": 9.0, "rgb": [255, 236, 200], "dur": 16, "tex": "magic_circle"},
                            {"t": "glyph", "at": "caster", "r": 8.0, "rgb": [255, 110, 50], "tex": "devil_circle", "dur": 36, "spin": 0.1},
                            {"t": "flash", "at": "caster", "size": 7.0, "rgb": [255, 220, 170], "dur": 8}]),
        ],
        note="a bull king's axe: a halo of spectral axe heads, the labyrinth in the iron, a brow and a nose ring")


# =====================================================================================================================================================
# 11. Dragon Force Twin Fangs (Fairy Tail): two curved dragon-fang daggers, bone-white blades with ember spines, red cord grips and ring pommels.
#     Upgrade: a halo of ten dragon scales round the pair, flames curling round each fang, claw guards, dorsal spines along the blades, dragon eyes in the pommels, scales etched in the bone.
# =====================================================================================================================================================
@item("dragon_force_twin_fangs")
class Fangs:
    builder = staticmethod(lambda: U.G2.build_dragon_force_twin_fangs())
    lod_keep = ("guard", "ember")

    @staticmethod
    def blade_pt(sx, t):
        return (sx - 8.5 * t ** 2.0, 12.0 + 40.0 * t)

    @staticmethod
    def extras(m, ctx):
        if not ctx.lod:
            C = V(0.0, 28.0, 0.0)
            sc = U.orbit_part(ctx, m, "up_scales", 0, C, (0, 1, 0), 2.6, 0.0, True, "bone", (255, 120, 60), 35)
            for i in range(10):
                a = 2 * math.pi * i / 10
                d = V(math.cos(a), 0, math.sin(a))
                t = V(-math.sin(a), 0, math.cos(a))
                c = C + d * 17.0 + V(0, 5.0 * math.sin(a * 2), 0)
                ml.loft(sc, t * 1.0, c - t * 1.8, [(0.0, 0.5, 0.25), (1.6, 2.0, 0.34), (3.0, 2.2, 0.36), (4.6, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=d)
            for k, sx in enumerate((-7.5, 7.5)):
                fl = U.orbit_part(ctx, m, "up_fang_flames_%d" % k, 1 + k, V(sx - 3.0, 30.0, 0), (0, 1, 0), 4.5 * (1 if k else -1), 0.0, True, "obsidian", (255, 120, 50), 30)
                for j in range(3):
                    a = 2 * math.pi * j / 3
                    c = V(sx - 3.0, 22.0, 0) + V(math.cos(a), 0, math.sin(a)) * 4.6
                    flame_tongue(fl, tuple(c), 10.0 + 2.0 * j, 1.1, (0.0, 0.0), 0.8)
        # claw guards: three curved claws on each guard
        for sx, sf in ((-7.5, "l"), (7.5, "r")):
            cg = mk(m, "up_claws_" + sf, "bone", (244, 236, 214), False, 30)
            for k in range(3):
                off = (k - 1) * 1.5
                pts = [(sx + off * (1.0 + 0.2 * i), 11.5 + 0.6 * i * i * 0.18, 1.6 + 1.6 * (i / 5.0)) for i in range(6)]
                pts = [(p[0], 11.2 - 2.4 * (i / 5.0) ** 0.8 + 0.0, p[2] + 0.4 * i) for i, p in enumerate(pts)]
                ml.sweep(cg, pts, ml.circle(1.0, U.S(4, 3)), scales=lambda t: (0.55 * (1 - 0.9 * t), 0.55 * (1 - 0.9 * t)), caps=(True, True))
            sp = mk(m, "up_spines_" + sf, "obsidian", (255, 120, 60), True, 30)
            for i in range(3, 12, 2):
                t = i / 14.0
                bx, by = Fangs.blade_pt(sx, t)
                U.spike3(sp, (bx + 2.9 * (1 - 0.7 * t ** 1.2), by, 0.4), (bx + 2.9 * (1 - 0.7 * t ** 1.2) + 2.2, by + 3.0, 0.4), 0.55, 4)
            eg = mk(m, "up_pommel_eye_" + sf, "crystal", (255, 150, 60), True, 30)
            w.ellipsoid(eg, (sx, 0.4, 2.4), 0.7, 0.45, 0.4, sides=6, stacks=4)
            if not ctx.lod:
                U.tuft(m, "up_cord_" + sf, (sx, -0.6, 0.0), 4, 12.0, 0.9, "cloth", (190, 40, 36), 3 if sx < 0 else 4)

    @staticmethod
    def post(m, atlas, ctx):
        EMB, ORG, WHT = (255, 120, 60), (255, 180, 100), (255, 236, 200)
        SK.glow_parts(atlas, m, ["up_scales"], EMB, 1.0, 0.5)
        SK.glow_prefix(atlas, m, "up_fang_flames", ORG, 1.0, 0.6)
        SK.glow_prefix(atlas, m, "up_spines", EMB, 0.9, 0.5)
        SK.glow_prefix(atlas, m, "up_pommel_eye", ORG, 1.0, 0.8)
        SK.glow_prefix(atlas, m, "up_claws", EMB, 0.12, 0.3)

        def scales(en, pc):
            # dragon scales: rows of arcs
            y = pc.Y.max()
            row = 0
            while y > pc.Y.min():
                x = pc.X.min() + (0.0 if row % 2 == 0 else 0.7)
                while x < pc.X.max() + 1.0:
                    en.circle((x, y), 0.7, 0.06, 255, 0, 180)
                    x += 1.4
                y -= 0.7
                row += 1
            en.text_col("DRACO VIS", 0.0, pc.Y.max() - 4.0, 0.8, 1.6, 0.1, 0.4)
            en.burn(0.4, 0.2, EMB, 0.45, 0.4)
        for sf in ("l", "r"):
            SK.engrave_face(atlas, m.part("blade_" + sf), scales)
        SK.detail_all(m, atlas, skip=("up_scales", "up_fang_flames"))
        with SK.on_veins(atlas, ctx):
            for sf in ("l", "r"):
                SK.paint_veins(atlas, m.part("blade_" + sf), EMB, 1, 1.0, (5.0, 0.8), 0.9)
                SK.paint_veins(atlas, m.part("ember_spine_" + sf), ORG, 2, 1.4, (2.0, 0.5), 0.9)
            SK.paint_veins(atlas, m.part("up_scales"), ORG, 3, 1.4, (1.5, 0.8), 0.9)
            SK.paint_veins(atlas, m.part("up_fang_flames_0"), WHT, 4, 1.6, (2.0, 0.8), 0.9)
            SK.paint_veins(atlas, m.part("up_fang_flames_1"), WHT, 5, 1.6, (2.0, 0.8), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(255, 120, 60), rgb2=(255, 236, 200), length=0.6, tilt=30,
        ambient=[
            {"type": "motes", "anchor": "mid", "particle": "flame", "rgb": [255, 120, 60], "rate": 16, "radius": 0.4, "rise": 0.04},
            {"type": "edge", "anchor": "blade", "particle": "small_flame", "rgb": [255, 160, 80], "rate": 14, "radius": 0.12, "from": 0.2, "to": 1.0},
            {"type": "ring", "anchor": "mid", "particle": "dust", "rgb": [255, 120, 60], "rate": 16, "radius": 0.5, "speed": 0.34, "size": 0.5, "tilt": 0.0},
            {"type": "arc", "anchor": "tip", "particle": "lava", "rgb": [255, 180, 100], "rate": 5, "radius": 0.45},
            {"type": "ground", "anchor": "feet", "particle": "flame", "rgb": [255, 120, 60], "rate": 10, "radius": 0.9, "speed": 0.08, "size": 0.5},
            {"type": "shaft", "anchor": "tip", "particle": "smoke", "rgb": [90, 60, 50], "rate": 5, "height": 1.4},
        ],
        swing={"pattern": "claw", "particle": "flame", "rgb": [255, 120, 60], "rgb2": [255, 236, 200], "count": 6, "size": 0.75, "reach": 1.0},
        acts=[
            ("twin_fang_flurry", [{"t": "burst", "at": "to", "n": 10, "len": 3.0, "rgb": [255, 120, 60], "tex": "beast_claw", "dur": 12},
                                  {"t": "burst", "at": "caster", "n": 8, "len": 2.4, "rgb": [255, 236, 200], "tex": "spark", "dur": 10}]),
            ("dragon_force", [{"t": "glyph", "at": "caster", "r": 5.0, "rgb": [255, 120, 60], "tex": "devil_circle", "dur": 44, "spin": 0.12},
                              {"t": "spiral", "at": "caster", "r": 1.6, "h": 6.0, "turns": 5, "n": 20, "rgb": [255, 160, 80], "dur": 40},
                              {"t": "column", "at": "caster", "h": 10.0, "r": 0.9, "rgb": [255, 120, 60], "dur": 28}]),
            ("dragons_roar", [{"t": "beam", "from": "caster", "to": "to", "w": 3.0, "rgb": [255, 120, 60], "tex": "flame_trail", "dur": 20},
                              {"t": "ring", "at": "caster", "r0": 0.5, "r1": 10.0, "rgb": [255, 180, 100], "dur": 18, "tex": "rune_ring"},
                              {"t": "flash", "at": "caster", "size": 7.0, "rgb": [255, 220, 170], "dur": 8}]),
        ],
        note="two fangs with a halo of dragon scales, claw guards and a flame curling round each blade")


# =====================================================================================================================================================
# 12. Dawnpiercer Crossbow (original): a dark-wood stock, a gold sun-wing prod swept forward with feather ridges, a steel latch and rail, a glowing sunstone, a gilt bolt.
#     Upgrade: a sun halo with sixteen rays turning behind the prod, rays ribbed across the limbs (they flex with the draw), sights, sun inlays, filigree on the stock, a sunray fletching
#     on the bolt, a tassel. The draw states and the loaded bolt are rebuilt from the same parts.
# =====================================================================================================================================================
@item("dawnpiercer_crossbow")
class Crossbow:
    Y_PROD, SPAN, STOCK_T = 20.0, 25.0, 2.4
    builder = staticmethod(lambda: U.G2.build_dawnpiercer_crossbow())
    lod_keep = ("sunstone", "limb_l")

    @staticmethod
    def extras(m, ctx):
        Y_PROD, SPAN, ST = Crossbow.Y_PROD, Crossbow.SPAN, Crossbow.STOCK_T
        SUN = V(0.0, Y_PROD + 3.0, -2.6)
        if not ctx.lod:
            sh = U.orbit_part(ctx, m, "up_sun_halo", 0, SUN, (0, 0, 1), 1.4, 0.0, True, "gold", (240, 200, 96))
            ml.sweep(sh, [tuple(q) for q in U.circle_pts(SUN, 12.0, (0, 0, 1), 30)], ml.circle(1.0, 4), scales=lambda t: (0.42, 0.42), caps=(False, False))
            ml.sweep(sh, [tuple(q) for q in U.circle_pts(SUN, 9.4, (0, 0, 1), 24)], ml.circle(1.0, 4), scales=lambda t: (0.25, 0.25), caps=(False, False))
            ry = U.orbit_part(ctx, m, "up_sun_rays", 0, SUN, (0, 0, 1), 1.4, 0.0, True, "crystal", (255, 224, 130), 30)
            for i in range(16):
                a = 2 * math.pi * i / 16
                d = V(math.cos(a), math.sin(a), 0.0)
                ln = 5.6 if i % 2 == 0 else 3.4
                ml.loft(ry, d, SUN + d * 12.2, [(0.0, 0.7, 0.28), (ln * 0.4, 0.62, 0.26), (ln, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1))
        # sunray ribs across the limbs: named limb_* so they bend with the draw
        for sgn, sf in ((-1, "l"), (1, "r")):
            rb = mk(m, "limb_ribs_" + sf, "gold", (252, 224, 130), True, 30)
            for k in range(6):
                t = (k + 1) / 8.0
                x = sgn * (1.5 + (SPAN - 1.5) * t)
                y = Y_PROD + 14.0 * t ** 1.7 - 1.2 * math.sin(math.pi * t)
                ml.bar(rb, (x - sgn * 0.0, y + 1.6, 0.8), (x - sgn * 0.4, y - 1.8, 0.8), 0.42, 0.16, up=(0, 0, 1), taper=0.6)
        # sights: a front post and a rear aperture ring
        sg = mk(m, "up_sights", "steel", (190, 196, 214), False, 40)
        U.spike3(sg, (0.0, 34.0, ST * 0.85 + 0.1), (0.0, 34.0, ST * 0.85 + 2.4), 0.5, 4)
        ml.sweep(sg, [tuple(q) for q in U.circle_pts(V(0.0, -12.0, ST + 1.6), 1.5, (0, 1, 0), U.S(12, 6))], ml.circle(1.0, 4), scales=lambda t: (0.22, 0.22), caps=(False, False))
        ml.bar(sg, (0.0, -12.0, ST + 0.1), (0.0, -12.0, ST + 0.2), 0.7, 0.3, up=(0, 0, 1))
        # sun inlays on the stock (three small discs with eight ticks each)
        si = mk(m, "up_sun_inlays", "gold", (244, 206, 100), True, 35)
        for yy in (-18.0, -6.0, 26.0):
            ml.lathe(si, "z", (0.0, yy, ST + 0.05), [(0.0, 1.2), (0.4, 1.0)], sides=U.S(10, 5), caps=(True, True))
            for j in range(8):
                a = 2 * math.pi * j / 8
                ml.bar(si, (1.3 * math.cos(a), yy + 1.3 * math.sin(a), ST + 0.1), (2.1 * math.cos(a), yy + 2.1 * math.sin(a), ST + 0.1), 0.3, 0.12, up=(0, 0, 1))
        # filigree down both sides of the stock
        fg = mk(m, "up_filigree", "gold", (238, 200, 94), False, 55, uv_mode="loft")
        for sgn in (-1, 1):
            pts = [(sgn * (2.5 + 0.35 * math.sin(i * 0.8)), -22.0 + 4.6 * i, 0.4) for i in range(U.S(9, 5))]
            ml.sweep(fg, pts, ml.circle(1.0, U.S(4, 3)), scales=lambda t: (0.22, 0.22))
        # a bolt fletching of sun rays (drawn with the loaded bolt)
        bf = mk(m, "bolt_up_fletch", "crystal", (255, 224, 130), True, 30)
        for k in range(3):
            a = 2 * math.pi * k / 3 + math.pi / 2
            d = V(math.cos(a), 0.0, math.sin(a))
            ml.loft(bf, V(0, 1.0, 0) + d * 0.35, V(0.0, 2.6, ST + 0.55) + d * 0.3, [(0.0, 0.18, 0.1), (2.2, 0.9, 0.12), (4.4, 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=d)
        if not ctx.lod:
            U.tuft(m, "up_tassel", (0.0, -25.5, 0.0), 5, 14.0, 1.0, "cloth", (232, 184, 76), 8)

    @staticmethod
    def post(m, atlas, ctx):
        GOLD, SUN, WHT = (255, 214, 110), (255, 236, 160), (255, 246, 214)
        SK.glow_parts(atlas, m, ["up_sun_halo", "up_sun_inlays", "bolt_up_fletch"], GOLD, 1.0, 0.5)
        SK.glow_parts(atlas, m, ["up_sun_rays"], SUN, 1.0, 0.6)
        SK.glow_prefix(atlas, m, "limb_ribs", GOLD, 0.6, 0.4)
        SK.glow_parts(atlas, m, ["up_filigree"], GOLD, 0.3, 0.3)

        def limb(en, pc):
            en.text_row("EOS HELIOS AURORA", pc.X.min() + 0.5, pc.Y.max() - 1.0, 0.9, 1.2, 0.1)
            en.burn(0.5, 0.22, SUN, 0.8, 0.5)
        for sf in ("l", "r"):
            SK.engrave_face(atlas, m.part("limb_" + sf), limb)

        def stock(en, pc):
            en.text_col("DAWNPIERCER", 0.0, 24.0, 0.9, 1.5, 0.1, 0.45)
            en.burn(0.5, 0.2, SUN, 0.7, 0.5)
        SK.engrave_face(atlas, m.part("stock"), stock)
        SK.detail_all(m, atlas, skip=("up_sun_halo", "up_sun_rays"))
        with SK.on_veins(atlas, ctx):
            for sf in ("l", "r"):
                SK.paint_veins(atlas, m.part("limb_" + sf), GOLD, 1, 0.9, (5.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("limb_feathers"), WHT, 2, 1.4, (2.0, 0.6), 0.9)
            SK.paint_veins(atlas, m.part("up_sun_rays"), WHT, 3, 1.6, (2.0, 0.6), 0.9)
            SK.paint_veins(atlas, m.part("sunstone"), WHT, 4, 1.4, (1.0, 1.0), 0.9)
            SK.paint_veins(atlas, m.part("bolt_tip"), WHT, 5, 1.4, (1.0, 0.5), 0.9)
            SK.paint_veins(atlas, m.part("stock_gilt"), GOLD, 6, 1.0, (2.0, 0.6), 0.9)
        SK.add_faint_core(atlas, ctx)

    SPEC = dict(
        rgb=(255, 214, 110), rgb2=(255, 246, 214), length=1.1, tilt=10,
        ambient=[
            {"type": "motes", "anchor": "mid", "particle": "end_rod", "rgb": [255, 236, 160], "rate": 14, "radius": 0.45, "rise": 0.03},
            {"type": "ring", "anchor": "mid", "particle": "dust", "rgb": [255, 214, 110], "rate": 16, "radius": 0.5, "speed": 0.22, "size": 0.5, "tilt": 90.0},
            {"type": "shaft", "anchor": "mid", "particle": "end_rod", "rgb": [255, 246, 214], "rate": 8, "height": 2.4},
            {"type": "edge", "anchor": "tip", "particle": "electric_spark", "rgb": [255, 236, 160], "rate": 8, "radius": 0.35, "from": 0.0, "to": 1.0},
            {"type": "ground", "anchor": "feet", "particle": "dust", "rgb": [255, 214, 110], "rate": 10, "radius": 1.0, "speed": 0.06, "size": 0.5},
            {"type": "arc", "anchor": "mid", "particle": "glow", "rgb": [255, 236, 160], "rate": 4, "radius": 0.5},
        ],
        swing={"pattern": "ray", "particle": "end_rod", "rgb": [255, 214, 110], "rgb2": [255, 246, 214], "count": 3, "size": 0.7, "reach": 1.1},
        acts=[
            ("piercing_bolt", [{"t": "beam", "from": "caster", "to": "to", "w": 0.9, "rgb": [255, 246, 214], "tex": "streak", "dur": 10},
                               {"t": "spiral", "at": "caster", "r": 0.7, "h": 8.0, "turns": 5, "n": 18, "rgb": [255, 214, 110], "dur": 12},
                               {"t": "burst", "at": "to", "n": 10, "len": 3.4, "rgb": [255, 236, 160], "tex": "spark", "dur": 12}]),
            ("sunrise_volley", [{"t": "glyph", "at": "caster", "r": 5.0, "rgb": [255, 214, 110], "tex": "magic_circle", "dur": 28, "spin": 0.1},
                                {"t": "burst", "at": "caster", "n": 14, "len": 7.0, "rgb": [255, 236, 160], "tex": "streak", "dur": 20},
                                {"t": "beam", "from": "caster", "to": "to", "w": 1.4, "rgb": [255, 214, 110], "dur": 12}]),
            ("dawns_net", [{"t": "glyph", "at": "to", "r": 9.0, "rgb": [255, 214, 110], "tex": "barrier_hex_grid", "dur": 44, "spin": 0.03},
                           {"t": "rain", "at": "to", "r": 9.0, "h": 14.0, "n": 22, "len": 5.0, "rgb": [255, 236, 160], "tex": "streak", "dur": 36},
                           {"t": "ring", "at": "to", "r0": 1.0, "r1": 10.0, "rgb": [255, 246, 214], "dur": 24, "tex": "rune_ring"}]),
        ],
        note="a sun in a crossbow: a turning halo of sixteen rays, rays ribbed across the limbs")
