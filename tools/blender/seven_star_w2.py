"""The seven Seven Star Weapons, style wave 2 (docs/heroweapon/style/wave2.md): hammer, axe, staff, claw, gauntlet, whip, projectile.

Run: python tools/blender/seven_star_w2.py [--only hammer,axe,...] [--trial DIR] [--fast]
  --trial DIR  writes the meshes, skins and previews into DIR instead of the mod's assets (for looking first);
  --fast       few samples and a 512 px bake (for iterating).

REPLACEMENT of tools/blender/seven_star.py's builders (which stay in the repo, untouched, together with their boxes and headers: this file imports them, so each weapon keeps its
length, its box, its place line and its grip). The previous meshes and skins are kept as the classic style copy in weapon_meshes/classic_style/ and
textures/entity/weapon/classic_style/ (client option heroStyleClassic). Guide rules applied: silhouette first, one hero feature, one named accent, no even bands, painted
wear (forms_kit.surface) instead of geometry, glow only on the accent, gems only where the lore needs one.

Idea / not-like (guide rule 10):
  hammer      the one with the big asymmetric iron head and the ember star ring on its striking face; NOT the barrel of gold bands with two gems.
  axe         the one with one huge bearded crescent of cold steel and a thin teal edge; NOT a navy block with gold bands and a back spike.
  staff       the one with a tall open eye-ring (uneven, one tine) round a floating moon crystal; NOT a crescent + ring + orbit + stars on a navy pole.
  claw        the one with three graded blades, the middle one hot; NOT three equal blades with a gold sun on the hand.
  gauntlet    the one with the worn fist, white fur and one amber stone; NOT a stack of gold lames with two gems.
  whip        the one with a leather lash and one violet stone in an iron cage; NOT gold cuffs, a star disc and two gems.
  projectile  the one with a crystal blade and a single cyan stone in the guard; NOT three gems, a bezel and a bead.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
import forms_kit as K  # noqa: E402
import seven_star as ss  # noqa: E402
import wave2_kit as W  # noqa: E402
from bl_lib import rgb  # noqa: E402
from mathutils import Vector  # noqa: E402

PI = math.pi
RUNE_FONT = ss.RUNE_FONT


def head_sec(table, x):
    """The (hy, hz) of the hammer head at x from a station table [(x, hy, hz)] (linear between stations)."""
    return K.interp([(t[0], t[1]) for t in table], x), K.interp([(t[0], t[2]) for t in table], x)


# ================================================================ the Hammer
def hammer(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("ember", ["iron", "face", "oak"], ["hide", "scratch"])
    P["iron"] = W.iron("hm_iron", rgb(60, 66, 80), seed=61, mottle=0.85, mottle_color=rgb(92, 62, 46))
    P["face"] = W.steel("hm_face", rgb(128, 134, 146), seed=62, bevel=0.06)
    P["oak"] = W.wood("hm_oak", rgb(112, 76, 44), seed=63)
    P["hide"] = W.hide("hm_hide", rgb(78, 48, 28), seed=64)
    P["scratch"] = W.scratch("hm_scratch")
    P["ember"] = W.accent_glow("hm_ember", rgb(255, 150, 44), 1.7, seed=65)
    P.check()
    cy = 146.0
    # ---- the head: a big block, wide striking face on the -x side, tapering to a short narrow peen on the +x side (asymmetric by design)
    st = [(-47.0, 27.0, 26.0), (-44.0, 30.5, 28.8), (-37.0, 27.0, 25.5), (-8.0, 26.0, 25.5), (14.0, 23.5, 25.5), (28.0, 17.5, 21.0), (40.2, 10.5, 13.0)]
    head = bl.loft("head", [ss.xring(x, bl.rrect(hy, hz, 4.0), cy) for x, hy, hz in st], P["iron"], part="head")
    bl.bevel(head, 0.9, 2)
    # the striking face: a bright worn steel plate, a raised iron ring round the ember stars
    fp = bl.loft("face_plate", [ss.xring(x, bl.rrect(hy, hz, 3.5), cy) for x, hy, hz in ((-49.4, 23.0, 22.5), (-48.4, 27.0, 26.2), (-44.0, 30.8, 29.0))], P["face"], part="face_plate")
    bl.bevel(fp, 0.5)
    ss.torus("face_ring", 15.0, 1.3, P["iron"], center=(-48.2, cy, 0), axis="x", n=28, m=6, part="face_plate")
    # two forged hoops of different width and height (not a matched pair)
    for i, (x0, x1) in enumerate(((-30.0, -22.0), (21.5, 24.8))):
        rings = []
        for x in (x0, x1):
            hy, hz = head_sec(st, x)
            rings.append(ss.xring(x, bl.rrect(hy * 1.04, hz * 1.04, 4.2), cy))
        bl.bevel(bl.loft("hoop%d" % i, rings, P["iron"], caps=(False, False), part="hoops"), 0.4)
    # ---- haft: oak through the head, one leather grip, an iron butt and socket
    bl.lathe("haft", [(3.6, 0), (3.5, 22), (3.8, 60), (4.5, 95), (5.3, 120), (5.6, 150), (5.0, 182)], P["oak"], sides=10)
    bl.lathe("grip_wrap", [(3.75, 16), (4.2, 18.5), (4.2, 52), (3.85, 55)], P["hide"], sides=10)
    bl.lathe("butt", [(0.01, -11.0), (2.4, -10.4), (4.2, -6.5), (4.9, -1.0), (4.6, 5.0)], P["iron"], sides=10, part="butt")
    bl.lathe("socket", [(5.0, 96), (6.2, 98.5), (7.4, 108), (10.0, 116), (13.2, 119.0)], P["iron"], sides=12, part="socket")
    bl.lathe("haft_cap", [(5.0, 170), (6.6, 175), (6.9, 181), (5.4, 187), (0.01, 190.0)], P["iron"], sides=10, part="butt")
    # ---- the stars: ember on the striking face only; on the +z side the same Dipper is only scratched in
    pts = [(u, cy + v) for u, v in ss.DIPPER]
    ss.face_x(ss.constellation("dipf", [(u * 1.12, cy + (p[1] - cy) * 1.12) for (u, _y), p in zip(ss.DIPPER, pts)], ss.DIPPER_LINKS, 49.45, P["ember"], P["scratch"], r=3.1, w=0.42), -1)
    ss.constellation("dips", pts, ss.DIPPER_LINKS, 25.52, P["scratch"], P["scratch"], r=1.5, w=0.26)
    # a repair patch with a few irregular rivets on the -z side: the one-off
    W.flat_plate("patch", [(-16, cy - 13), (4, cy - 14.5), (6, cy + 2), (-14, cy + 4.5)], -25.52, P["face"], 0.1)
    rr = K.rng(66)
    K.rivets("patch_rivets", [((x, y, -25.6), (0, 0, -1)) for x, y in ((-14.2, cy - 11.4), (3.8, cy - 12.6), (4.6, cy + 0.2), (-12.6, cy + 2.6))], P["face"], r=0.7, rng_=rr, jitter=0.25)
    return ss.done("vassal_hammer", out)


# ================================================================ the Axe
def axe(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("teal", ["steel", "iron", "ash"], ["hide", "scratch"])
    P["steel"] = W.steel("ax_steel", rgb(176, 186, 200), seed=71, bevel=0.1)
    P["iron"] = W.iron("ax_iron", rgb(48, 52, 62), seed=72)
    P["ash"] = W.wood("ax_ash", rgb(120, 86, 54), seed=73)
    P["hide"] = W.hide("ax_hide", rgb(72, 46, 28), seed=74)
    P["scratch"] = W.scratch("ax_scratch")
    P["edge"] = W.accent_glow("ax_edge", rgb(50, 232, 200), 1.15, seed=75)
    P.check()
    # ---- haft: ash wood, a worn leather grip, an iron shoe; two iron langets run down from the head (no gold bands)
    bl.lathe("haft", [(3.7, 0), (3.6, 30), (4.1, 76), (4.8, 120), (5.2, 150)], P["ash"], sides=10)
    bl.lathe("grip_wrap", [(3.8, 18), (4.3, 21), (4.3, 68), (3.95, 72)], P["hide"], sides=10)
    bl.lathe("butt", [(0.01, -12), (2.0, -10.8), (3.9, -6.5), (4.6, -1), (4.5, 6)], P["iron"], sides=10, part="shoe")
    for k, (x, y0, y1) in enumerate(((4.7, 96.0, 134.0), (-4.7, 108.0, 134.0))):          # two langets of different length
        pl = bl.plate("langet%d" % k, [(-0.5, y0), (0.5, y0), (0.9, y1), (-0.9, y1)], -3.0, 3.0, P["iron"], part="langets")
        bl.move(pl, loc=(x, 0, 0))
    # ---- hub: a plain iron block between the blade and the small poll
    hub = bl.loft("hub", [bl.ring(y, bl.rrect(10.0 * s, 10.0 * s, 3.2)) for y, s in ((130, 0.9), (134, 1.0), (162, 0.95), (172, 0.8))], P["iron"], part="hub")
    bl.bevel(hub, 0.6)
    poll = bl.loft("poll", [ss.xring(x, bl.rrect(hy, hz, 3.0), 152.0) for x, hy, hz in ((9, 12.0, 8.5), (14, 13.5, 10.0), (24, 12.0, 8.8), (34.5, 7.0, 5.5))], P["iron"], part="poll")
    bl.bevel(poll, 0.6)
    # ---- the blade: ONE bearded crescent, the beard hooking back toward the haft; thin teal edge, a bite out of it, the Dipper scratched in the steel
    edge = bl.smooth_path([(-46, 207.5, 0), (-66, 187, 0), (-78.5, 152, 0), (-79.8, 114, 0), (-72, 78, 0), (-56, 52, 0), (-40, 40, 0)], 3)
    outer = [(p.x, p.y) for p in edge]
    ns = len(outer)
    k_bite = ns // 2 + 2
    outer[k_bite] = (outer[k_bite][0] + 3.2, outer[k_bite][1])                    # the bite: one shallow dent in the edge, left of centre
    inner = [(-8.0, 172.0 - 70.0 * i / (ns - 1)) for i in range(ns)]
    thick = lambda t: 5.6 * max(1.0 - t, 0.0) ** 0.8                              # noqa: E731
    ss.fan_blade("blade", inner, outer, thick, P["steel"], 0.0, 0.93, 6, bulge=15.0)
    ss.fan_blade("edge", inner, outer, thick, P["edge"], 0.93, 1.0, 2, bulge=15.0)
    st = [(0.14, 0.52), (0.28, 0.30), (0.41, 0.58), (0.53, 0.28), (0.62, 0.62), (0.77, 0.36), (0.88, 0.60)]
    pts = [ss.blade_point(inner, outer, s, t, 15.0) for s, t in st]
    for i in range(6):                                                              # the Dipper as scratched lines (dark), on the +z face only
        (s0, t0), (s1, t1) = st[i], st[i + 1]
        ss.ramp_line("bln%d" % i, pts[i], thick(t0) + 0.08, pts[i + 1], thick(t1) + 0.08, 0.26, P["scratch"])
    for i, ((s, t), p) in enumerate(zip(st, pts)):
        ss.flat("bst%d" % i, ss.star_outline(p[0], p[1], 2.8 if i == 3 else 2.0), thick(t) + 0.12, P["scratch"], 0.07)
    return ss.done("vassal_axe", out)


# ================================================================ the Staff
def staff(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("moon", ["wood", "iron", "cloth"], ["steel"])
    P["wood"] = W.wood("st_wood", rgb(84, 60, 44), seed=81, bevel=0.1)
    P["iron"] = W.iron("st_iron", rgb(58, 62, 74), seed=82)
    P["cloth"] = W.cord("st_cloth", rgb(76, 84, 110), seed=83)
    P["steel"] = W.steel("st_steel", rgb(164, 172, 188), seed=84, bevel=0.07)
    P["moon"] = W.accent_gem("st_moon", rgb(120, 180, 255), rgb(105, 170, 255), 0.5, seed=85)
    P.check()
    # ---- shaft: a dark wood pole, thinner at the foot; ONE wrapped section with a hanging tail; an iron ferrule and a socket
    bl.lathe("shaft", [(2.7, 0), (3.3, 40), (3.9, 100), (4.2, 150), (4.4, 200)], P["wood"], sides=10)
    bl.lathe("grip_wrap", [(3.9, 92), (4.4, 95), (4.4, 134), (4.15, 138)], P["cloth"], sides=10)
    tail = bl.sweep("wrap_tail", bl.smooth_path([(4.3, 128, 1.0), (7.0, 114, 1.8), (6.2, 98, 2.4), (8.0, 84, 1.8)], 3), [(-1.1, -0.1), (1.1, -0.1), (1.0, 0.1), (-1.0, 0.1)], P["cloth"], up=(1, 0, 0), part="grip_wrap")
    bl.lathe("foot", [(0.01, -9), (1.4, -6.5), (2.9, 0), (3.5, 4), (3.2, 12)], P["iron"], sides=10, part="foot")
    W.band("lash_low", 142.0, 145.0, 4.0, 5.0, P["iron"], "lashing", taper=0.0)
    bl.lathe("socket", [(4.5, 182), (5.4, 186), (6.0, 196), (8.2, 206), (9.4, 214), (7.4, 219)], P["iron"], sides=12, part="socket")
    # ---- head: an open eye-ring with an uneven wall, a gap at the upper right, one tine; three prongs of unequal length hold it; the moon crystal floats tilted
    cx, cyh, R = 0.0, 252.0, 27.0
    n = 28
    a0, a1 = math.radians(52), math.radians(52 + 306)
    path = [(cx + R * math.cos(a0 + (a1 - a0) * i / n), cyh + R * math.sin(a0 + (a1 - a0) * i / n), 0) for i in range(n + 1)]
    wfun = lambda t: 2.5 + 2.1 * math.exp(-((t - 0.52) / 0.26) ** 2)                # noqa: E731  (thick low and left, thin toward the cut ends)
    tfun = lambda t: 2.4 + 1.0 * math.exp(-((t - 0.5) / 0.3) ** 2)                   # noqa: E731
    bl.sweep("eye_ring", path, bl.lozenge(1.0, 1.0, flat=0.45), P["steel"], scale=lambda t: (wfun(t), tfun(t)), part="eye_ring")
    bl.sweep("tine", bl.smooth_path([(5.0, cyh + R - 1.0, 0), (3.5, cyh + R + 3.5, 0), (-1.5, cyh + R + 6.68, 0), (-8.5, 286.6, 0)], 3), bl.lozenge(1.0, 1.0, flat=0.3), P["steel"],
             scale=lambda t: (3.2 * (1 - t) ** 0.8 + 0.12, 2.6 * (1 - t) ** 0.7 + 0.1), part="eye_ring")
    for i, (ang, reach, w0) in enumerate(((-96.0, 0.0, 2.8), (-146.0, 3.0, 2.2), (-36.0, -2.0, 1.7))):          # three prongs: the long centre, a medium left, a short right
        ta = math.radians(ang)
        tip = (cx + (R - 0.5) * math.cos(ta), cyh + (R - 0.5) * math.sin(ta), 0)
        mid = ((tip[0] * 0.55 + 0.0), 0.5 * (tip[1] + 212.0) - 1.0 * i, 0)
        bl.sweep("prong%d" % i, bl.smooth_path([(1.2 * (i - 1), 212, 0), mid, tip], 4), bl.lozenge(1.0, 1.0, flat=0.3), P["steel"],
                 scale=lambda t, w0=w0: (w0 * (1 - 0.55 * t) + 0.2, 2.5 * (1 - 0.4 * t) + 0.2), part="prongs")
    cr = bl.loft("moon_crystal", [bl.ring(y, bl.circle(r, 6)) for y, r in ((-18.0, 0.05), (-9.0, 7.4), (0.0, 10.2), (9.0, 7.4), (19.5, 0.05))], P["moon"], part="moon_crystal")
    bl.move(cr, loc=(0, cyh, 0), rot=(0, 0, 14))
    return ss.done("vassal_staff", out)


# ================================================================ the Claw (worn on the hand: the mouth of the bracer at y = 0)
def rect_pts(x0, y0, x1, y1):
    return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]


def claw(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("amber", ["iron", "steel", "hide"], ["lining", "scratch"])
    P["iron"] = W.iron("cl_iron", rgb(58, 62, 76), seed=91, mottle_color=rgb(88, 62, 48))
    P["steel"] = W.steel("cl_steel", rgb(170, 178, 192), seed=92, bevel=0.1)
    P["hide"] = W.hide("cl_hide", rgb(92, 56, 32), seed=93)
    P["lining"] = bl.mat("cl_lining", rgb(60, 36, 22), rough=0.8, bump="leather", bstr=0.4, bevel=0.0)
    P["fire"] = W.accent_glow("cl_fire", rgb(255, 150, 40), 1.6, seed=94)
    P.check()
    oval = lambda rx, rz: bl.circle(rx, 16, rz=rz)                                # noqa: E731
    # ---- bracer: the sleeve round the forearm (as before: HandWrap needs it), two lames of different height, two leather straps of different width
    bl.loft("cuff", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0, 17.5, 12.2), (5, 17.0, 12.0), (34, 16.9, 12.0), (40, 17.2, 12.2))], P["iron"], caps=(False, False))
    li = bl.loft("cuff_lining", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0.5, 16.3, 11.3), (34, 16.0, 11.1))], P["lining"], caps=(False, False))
    li.data.flip_normals()
    for k, (y0, top) in enumerate(((1.0, 21.5), (12.6, 23.4))):
        bl.loft("lame%d" % k, [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((y0 + 1.4, top, 13.3), (y0 + 6.0, 18.8, 12.9), (y0 + 8.6, 17.8, 12.5))], P["steel"], caps=(False, False), part="lames")
    for k, (y0, y1, g) in enumerate(((7.0, 9.6, 0.5), (27.0, 31.4, 0.9))):
        bl.loft("strap%d" % k, [bl.ring(y, oval(17.2 + g * 0.4, 12.4 + g * 0.3)) for y in (y0, y0 + 0.4, y1 - 0.4, y1)], P["hide"], caps=(False, False), part="straps")
    bk = bl.plate("buckle", rect_pts(-3.2, 27.0, 3.2, 31.4), 12.5, 14.4, P["iron"], part="buckles")
    bl.bevel(bk, 0.5)
    # ---- the hand shell: an iron back plate, a plain knuckle bar (no stones, no stars)
    bl.bevel(bl.loft("back_plate", [bl.ring(y, bl.rrect(w, t, 5.0)) for y, w, t in ((34, 16.6, 12.4), (38, 18.0, 13.8), (66, 18.0, 13.8), (70.5, 16.8, 13.8), (74, 13.5, 10.5))], P["iron"], caps=(False, True)), 0.5)
    bl.bevel(bl.loft("knuckle_bar", [bl.ring(y, bl.rrect(w, t, 4.0)) for y, w, t in ((70.5, 19.5, 12.8), (73, 22.0, 14.3), (81, 22.0, 14.3), (84.4, 18.5, 11.8))], P["iron"]), 0.6)

    # ---- the blades: graded 182 / 150 / 142, the long middle one is the hot one (accent), the two shorter ones plain steel; a hook and a thumb claw
    def blade(name, pts, w, th, glow=False):
        path = bl.smooth_path([(x, y, 0) for x, y in pts], 4)
        fw = lambda t: w * (1 - t) ** 0.7 + 0.12                                  # noqa: E731
        ft = lambda t: th * (1 - t) ** 0.6 + 0.1                                  # noqa: E731
        bl.sweep(name, path, bl.lozenge(1.0, 1.0, flat=0.3), P["steel"], scale=lambda t: (fw(t), ft(t)), part="blades")
        if glow:
            for s in (1, -1):
                k = (len(path) - 3.0) / (len(path) - 1.0)
                sec = lambda t, s=s, k=k: [(-0.2 * fw(t * k), s * ft(t * k) + 0.04 * s - 0.03), (0.2 * fw(t * k), s * ft(t * k) + 0.04 * s - 0.03),            # noqa: E731
                                           (0.2 * fw(t * k), s * ft(t * k) + 0.04 * s + 0.03), (-0.2 * fw(t * k), s * ft(t * k) + 0.04 * s + 0.03)]
                bl.sweep("%s_glow%d" % (name, s), path[:-2], sec, P["fire"], detail=True)
    blade("blade_a", [(-13, 80), (-21, 108), (-31, 130), (-39.5, 150)], 5.0, 3.2)
    blade("blade_b", [(-1, 82), (-6, 115), (-13, 150), (-21.4, 181.9)], 6.0, 3.6, glow=True)
    blade("blade_c", [(11, 80), (11.5, 104), (9.5, 124), (5, 142)], 4.6, 3.0)
    blade("hook", [(20, 73), (27, 69), (31, 63), (33.0, 56.5)], 3.0, 2.4)
    blade("thumb", [(-18, 62), (-25, 55), (-29, 47), (-30.2, 38.2)], 3.0, 2.4)
    return ss.done("vassal_claw", out)


# ================================================================ the Gauntlet (worn: back of the hand = +x, thumb = +z, mouth at y = 0)
def gauntlet(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("amber", ["steel", "iron", "fur"], ["hide", "lining"])
    P["steel"] = W.steel("ga_steel", rgb(150, 158, 172), seed=101, bevel=0.1)
    P["iron"] = W.iron("ga_iron", rgb(60, 64, 78), seed=102, mottle_color=rgb(90, 64, 50))
    P["fur"] = bl.mat("ga_fur", rgb(236, 232, 222), rough=0.9, bump="leather", bstr=1.0, bscale=1.6, groove=rgb(150, 142, 128), gamount=0.9, bevel=0.0)
    P["hide"] = W.hide("ga_hide", rgb(98, 60, 34), seed=103)
    P["lining"] = bl.mat("ga_lining", rgb(60, 36, 22), rough=0.8, bump="leather", bstr=0.4, bevel=0.0)
    P["amber"] = W.accent_gem("ga_amber", rgb(255, 150, 30), rgb(255, 180, 50), 0.5, seed=104)
    P.check()
    oval = lambda rx, rz, n=16: bl.circle(rx, n, rz=rz)                           # noqa: E731
    bl.loft("vambrace", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0, 11.5, 13.6), (4, 11.2, 13.3), (30, 10.8, 12.9), (38, 10.6, 12.8))], P["steel"], caps=(False, False))
    li = bl.loft("vambrace_lining", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0.5, 10.7, 12.7), (30, 10.3, 12.3))], P["lining"], caps=(False, False))
    li.data.flip_normals()
    for k, y0 in enumerate((3.0, 10.2, 18.6)):                                   # three lames at uneven steps (7.2, 8.4), the lips plain steel, ONE fin
        bl.loft("lame%d" % k, [bl.ring(y, oval(rx, rz), ox=ox) for y, rx, rz, ox in ((y0, 11.4, 13.5, 0.0), (y0 + 2.2, 11.9, 13.9, 0.2), (y0 + 6.6, 12.6, 14.6, 1.0))], P["iron"], caps=(False, False), part="lames")
        bl.loft("lame_lip%d" % k, [bl.ring(y, oval(rx, rz), ox=ox) for y, rx, rz, ox in ((y0 + 6.4, 12.7, 14.7, 1.0), (y0 + 8.0, 12.9, 14.9, 1.1), (y0 + 8.3, 12.0, 14.1, 0.9), (y0 + 7.6, 11.0, 13.2, 0.2))], P["steel"], caps=(False, False), part="lames")
    fin = bl.plate("fin", [(12.6, 11.2), (16.9, 20.6), (15.4, 21.8), (12.4, 17.0)], -0.7, 0.7, P["iron"], part="lames")
    bl.bevel(fin, 0.25)
    bl.loft("cuff_trim", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((-0.8, 11.6, 13.7), (-0.4, 12.2, 14.5), (2.2, 12.2, 14.5), (2.8, 11.3, 13.4))], P["hide"], caps=(False, False))
    bl.loft("cuff_fur", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((1.7, 12.25, 14.55), (0.4, 13.3, 15.6), (-1.6, 13.3, 15.6), (-2.852, 12.3, 14.5), (-2.852, 11.0, 13.1), (-0.6, 10.9, 13.0))], P["fur"], caps=(False, False))
    bl.loft("wrist_strap", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((27, 10.9, 13.0), (27.5, 11.3, 13.4), (32.5, 11.2, 13.3), (33, 10.8, 12.9))], P["hide"], caps=(False, False))
    bl.bevel(bl.plate("buckle", rect_pts(-3.2, 28.2, 3.2, 31.8), 13.0, 14.4, P["iron"], part="buckles"), 0.35)
    # ---- the fist: iron back, four curled fingers, thumb; four studs of graded length (the lore names them), plain iron
    bl.bevel(bl.loft("back_plate", [bl.ring(y, bl.rrect(w, t, 3.6)) for y, w, t in ((36, 10.2, 12.6), (40, 10.5, 13.0), (66, 10.5, 13.0), (71.5, 9.6, 12.5))], P["iron"], caps=(False, True)), 0.5)
    zspan = ((-12.8, -6.7), (-6.5, 0.1), (0.3, 6.5), (6.7, 12.5))                         # four fingers of different width and a different reach (index longest), not four copies
    dys = (0.0, 1.6, 0.7, -1.9)
    for i, ((z0, z1), dy) in enumerate(zip(zspan, dys)):
        for j, outline in enumerate(([(10.8, 70.5), (10.8, 77.2), (8.8, 79.9), (-2.4, 79.9), (-4.4, 78.0), (-4.4, 70.5)],
                                     [(-6.2, 60.5), (-0.4, 60.5), (-0.4, 75.6), (-4.0, 77.2), (-6.2, 75.4)],
                                     [(-5.4, 57.7), (2.6, 57.7), (3.4, 59.6), (3.4, 62.6), (-5.4, 62.6)])):
            outline = [(x, y + dy * (0.0 if y < 65.0 and j == 2 else 1.0)) for x, y in outline]
            bl.bevel(bl.plate("digit%d%d" % (i, j), outline, z0, z1, P["steel"] if j else P["iron"], part="digits"), 0.7)
    for i, (zc, ln, dy) in enumerate(((-9.7, 5.2, dys[0]), (-3.2, 5.5, dys[1]), (3.4, 5.2, dys[2]), (9.6, 4.4, dys[3]))):                       # four studs of graded length on their own finger
        ss.spike("stud%d" % i, (7.6, 78.2 + dy, zc), (7.6 + ln * 0.28, 78.2 + dy + ln, zc), 2.1, P["iron"], sides=6, part="knuckle_studs")
    th = bl.plate("thumb", [(-3.0, 42.0), (6.0, 41.4), (8.5, 52.0), (7.4, 65.0), (4.0, 73.6), (-1.0, 72.6), (-3.6, 60.0)], 13.0, 19.4, P["steel"], part="digits")
    bl.bevel(th, 0.9, 2)
    # ---- the plate on the back of the hand with its ONE stone (sunk, small): the accent
    bp = bl.plate("hand_plate", [(-9.2, 44), (-9.2, 70), (-6.5, 78), (6.5, 78), (9.2, 70), (9.2, 44), (0, 39)], 10.3, 11.5, P["iron"], part="back_plate")
    bl.bevel(bp, 0.4)
    ss.face_x(bp, 1)
    W.jewel("gem_x", (12.0, 56, 0), 4.4, P["amber"], P["iron"], mount="sunk", axis="x", part="gem")
    return ss.done("vassal_gauntlet", out)


# ================================================================ the Whip
def whip(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("violet", ["lash", "iron", "hide"], ["steel"])
    P["lash"] = W.cord("wh_lash", rgb(78, 52, 86), seed=111, bump_strength=0.8)
    P["iron"] = W.iron("wh_iron", rgb(62, 64, 78), seed=112, mottle_color=rgb(90, 64, 50))
    P["hide"] = W.hide("wh_hide", rgb(96, 58, 34), seed=113)
    P["steel"] = W.steel("wh_steel", rgb(168, 174, 190), seed=114, bevel=0.07)
    P["violet"] = W.accent_gem("wh_violet", rgb(140, 60, 250), rgb(170, 100, 255), 0.6, seed=115)
    P.check()
    # ---- handle: a braided leather grip, three unequal iron collars, a plain iron cup guard, a ring pommel with a short drop (no gems, no stars)
    bl.lathe("grip_braid", [(4.5, -2), (5.0, 3), (5.0, 41), (4.5, 46)], P["hide"], sides=10, squash=0.96)
    W.bands("hr", [(-3.5, -0.8, 1.0), (13.0, 14.6, 0.97), (38.6, 43.4, 1.0)], 4.6, 5.8, P["iron"], "handle_rings")
    bl.lathe("guard_cup", [(5.2, 43.6), (9.0, 45.2), (13.4, 49.5), (15.6, 56.4), (14.2, 58.0), (11.0, 53.0), (5.6, 47.5)], P["iron"], sides=14, squash=0.9, part="guard_cup")
    ss.torus("pommel_ring", 6.4, 1.9, P["iron"], center=(0, -6.0, 0), axis="z", n=18, m=6, part="pommel")
    ss.spike("pommel_drop", (0, -9.5, 0), (0, -24, 0), 2.2, P["iron"], sides=8, part="pommel")
    # ---- the lash: one coil over the guard, then an S up to the barbed tip; three iron cuffs at uneven distances, three barbs of graded length
    pts = [(0, 47, 0), (-2, 55, 1.5)]
    for k in range(11):
        a = PI + 2.5 * PI * k / 10
        pts.append((10 + 13.5 * math.cos(a), 60 + 50 * k / 10, 2 + 9.5 * math.sin(a)))
    pts += [(3, 122, -9), (-7, 133, -6), (-9, 143, -3.5), (-2, 151, -3.2), (6.4, 158, -3.6)]
    _ob, frames = ss.ptube("coil_and_lash", pts, lambda t: 3.0 - 1.5 * t, P["lash"], sides=8, sub=3)
    nf = len(frames)
    for j, f in enumerate((0.30, 0.46, 0.83)):
        i = int(f * (nf - 1))
        p, tan, _n = frames[i]
        r = 3.0 - 1.5 * i / (nf - 1) + 0.5
        ss.ptube("cuff%d" % j, [p - tan * (0.8 + 0.5 * (j == 1)), p + tan * (0.8 + 0.5 * (j == 1))], r, P["iron"], sides=8, sub=1, part="coil_and_lash")
    x0, x1, _y0, _y1, z0, z1 = ss.BOXES["vassal_whip"]
    for k, (f, ln) in enumerate(((0.62, 4.0), (0.74, 6.4), (0.9, 3.2))):
        i = int(f * (nf - 1))
        p, _t, nrm = frames[i]
        for d in ((nrm, -nrm) if k % 2 == 0 else (-nrm, nrm)):
            tip = p + d * ln
            if x0 + 0.3 < tip.x < x1 - 0.3 and z0 + 0.3 < tip.z < z1 - 0.3:
                ss.spike("barb%d" % k, p + d * 1.0, tip, 1.1, P["iron"], sides=5, part="tip_spikes")
                break
    tc = Vector((6.4, 169, -3.6))
    bl.loft("tip_gem", [[Vector((tc.x + px, y, tc.z + pz)) for px, pz in bl.circle(r, 6)] for y, r in ((161.6, 0.05), (166.5, 4.6), (169.5, 5.6), (173, 4.2), (177.1, 0.05))], P["violet"], part="tip_gem")
    for k in range(3):                                                              # a cage of three iron claws (not four gold ones)
        a = PI / 6 + 2 * PI / 3 * k
        dx, dz = math.cos(a), math.sin(a)
        ss.ptube("cage%d" % k, [(tc.x + dx * 2.2, 157.5, tc.z + dz * 2.2), (tc.x + dx * 6.6, 163, tc.z + dz * 6.6), (tc.x + dx * 7.0, 170, tc.z + dz * 7.0), (tc.x + dx * 4.6, 177.5, tc.z + dz * 4.6), (tc.x + dx * 1.2, 182.5, tc.z + dz * 1.2)],
                 lambda t: 1.2 - 0.4 * t, P["iron"], sides=6, sub=3, part="tip_cage")
    bl.lathe("cage_base", [(2.4, 154.5), (3.6, 156), (3.6, 158.5), (2.4, 160)], P["iron"], sides=8, origin=(tc.x, 0, tc.z), part="tip_cage")
    bl.loft("stinger", [bl.ring(y, bl.lozenge(w, t), ox=tc.x, oz=tc.z) for y, w, t in ((180, 1.6, 1.6), (185, 3.4, 2.2), (191, 2.2, 1.4), (198, 0.08, 0.05))], P["steel"], part="tip_spikes")
    return ss.done("vassal_whip", out)


# ================================================================ the Projectile (a crystal throwing blade)
def projectile(out):
    K.begin(ss.SAMPLES)
    P = W.Palette("cyan", ["crystal", "iron", "cord"], ["core", "scratch"])
    P["crystal"] = bl.mat("pr_crystal", rgb(56, 196, 186), metallic=0.6, rough=0.12, emit=rgb(60, 225, 200), strength=0.07, bevel=0.2)
    P["iron"] = W.iron("pr_iron", rgb(34, 70, 78), seed=121, mottle_color=rgb(18, 40, 46), wear_color=rgb(120, 170, 170))
    P["cord"] = W.cord("pr_cord", rgb(30, 74, 80), seed=122)
    P["core"] = bl.mat("pr_core", rgb(20, 96, 108), metallic=0.7, rough=0.22, emit=rgb(60, 215, 205), strength=0.3, bevel=0.0)
    P["cyan"] = W.accent_gem("pr_cyan", rgb(30, 205, 225), rgb(80, 240, 250), 0.6, seed=123)
    P["glow"] = W.accent_glow("pr_glow", rgb(150, 255, 245), 1.8, seed=124)
    P.check()
    st = [(44, 5.0, 3.0), (50, 9.2, 3.5), (58, 9.8, 3.5), (70, 9.2, 3.5), (84, 7.6, 3.3), (97, 5.0, 2.0), (104, 2.6, 1.1), (109, 0.1, 0.06)]
    bl.loft("blade", [bl.ring(y, bl.lozenge(w, t, flat=w * 0.34)) for y, w, t in st], P["crystal"], part="blade")
    for s in (1, -1):
        ss.flat("core%d" % s, [(-2.6, 56), (2.6, 56), (2.5, 69), (0, 78), (-2.5, 69)], 3.52 * s, P["core"], 0.05)
    bl.text("runes", "ᛉᛊᛏ", 3.4, P["glow"], (0.0, 63.5, 3.6), rot=(0, 0, 90), spacing=1.35, font=RUNE_FONT)                   # runes on ONE face only
    gpath = bl.smooth_path([(-17.1, 59.8, 0), (-13.5, 47, 0), (-7, 38.6, 0), (0, 35.6, 0), (7, 38.6, 0), (13.5, 47, 0), (17.1, 59.8, 0)], 3)
    bl.sweep("guard", gpath, bl.lozenge(1.0, 1.0, flat=0.3), P["iron"], scale=lambda t: (3.2 * math.sin(PI * t) ** 0.6 + 0.25, 3.8 * math.sin(PI * t) ** 0.5 + 0.25), part="guard")
    for s in (1, -1):                                                                  # the one stone, set into the guard from both faces
        g = bl.gem("gem_a", 4.4, P["cyan"], center=(0, 40.2, 3.6), depth=0.28, crown=0.16, sides=8, axis="z", part="gem")
        if s < 0:
            bl.move(g, scale=(1, 1, -1))
    bl.lathe("grip", [(3.2, 15), (3.7, 19), (3.7, 37), (3.2, 41)], P["cord"], sides=10)
    ss.torus("pommel_ring", 8.2, 2.4, P["iron"], center=(0, 6, 0), axis="z", n=24, m=8, part="pommel")
    ss.ptube("cord_loop", [(-4.6, 1.0, 0), (-8, -6, 1.2), (-9.8, -13, 0), (-8.4, -18, -1.0), (-6, -20.5, 0)], 0.9, P["cord"], sides=6, sub=3, caps=(True, True))
    bl.lathe("cord_knot", [(0.01, -24.633), (1.5, -23.6), (2.0, -22.2), (1.5, -20.8), (0.01, -19.9)], P["cord"], sides=8, origin=(-6, 0, 0), part="cord_loop")
    bl.loft("shard_a", [bl.ring(y, bl.circle(r, 6), ox=14.8) for y, r in ((70.0, 0.05), (78.5, 2.6), (83.5, 2.6), (93.0, 0.05))], P["cyan"], part="shards")       # one big shard, one small, not a pair
    bl.loft("shard_b", [bl.ring(y, bl.circle(r, 6), ox=-14.0) for y, r in ((60.0, 0.05), (63.5, 1.5), (66.5, 1.5), (71.5, 0.05))], P["cyan"], part="shards")
    return ss.done("vassal_projectile", out)


BUILDERS = {"hammer": hammer, "claw": claw, "axe": axe, "whip": whip, "staff": staff, "gauntlet": gauntlet, "projectile": projectile}


def main(argv):
    only = trial = None
    for i, a in enumerate(argv):
        if a == "--only":
            only = argv[i + 1].split(",")
        if a == "--trial":
            trial = argv[i + 1]
        if a == "--fast":
            ss.SAMPLES = 6
            ss.FAST.update(bake=512)
    for name, fn in BUILDERS.items():
        if only and name not in only:
            continue
        out = {}
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, "vassal_" + name + "_preview.png"))
        fn(out)
        if trial:
            K.shrink_pngs(trial, "vassal_" + name)


if __name__ == "__main__":
    main(sys.argv[1:])
