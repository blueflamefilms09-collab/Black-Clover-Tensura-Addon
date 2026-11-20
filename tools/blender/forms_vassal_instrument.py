"""Weapon-form models of the VASSAL INSTRUMENT (base `vassal_instrument`, a violin, size class wand), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_instrument.py --only guitar [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids (docs/heroweapon/form_models.md): vassal_instrument__<form>:
  guitar               Stun Beat Guitar: a sunburst cutaway guitar, a long fretted neck, a slanted headstock with six pegs, pickups, a lightning inlay and sound-wave arcs, a hanging strap
  humming_fairy_lyre   Humming Fairy Lyre: a gold-and-wood lyre with two swept arms, a yoke with amethyst pegs, nine strings, big veined fairy wings, flower vines and fairy orbs
  grilled_fish_horn    Grilled Fish Horn: a charred grilled fish on a bamboo skewer (the handle) whose open mouth is a brass bell, valves and piping on its back, fins and garnish
  instrument_zero      Musical Instrument 0: a violet-black hoop (the 0) strung across, pufferfish spikes, venom fangs and drips, a spider-eyed head, broken shackles
The base violin has no `grip` line (its hold is the thin lower end), so no part of these forms is named like a grip; the lowest part is the thin end at y 0 (the skewer of the fish).
Every form keeps the base's place line and its length (top at y <= 74), the face toward +z.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
from forms_vassal_run import main  # noqa: E402

MANIFEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forms_manifest_vassal_instrument.json")
BASE = "vassal_instrument"


def wood(name, col, groove, seed, mottle=None, scale=1.4):
    return K.surface(name, col, 0.0, 0.42, bump="grain", bump_scale=scale, bump_strength=0.7, groove=groove, groove_amount=0.7, dirt=0.6, dirt_color=(0.03, 0.015, 0.006, 1), mottle=0.9 if mottle else 0.0,
                     mottle_color=mottle, wear=0.5, wear_color=tuple(min(1.0, c * 1.6 + 0.08) for c in col[:3]) + (1.0,), wear_metal=0.0, fine=0.4, seed=seed, bevel=0.1, ao_dist=1.8)


def outline(wl, wr, ys):
    """A closed outline [(x, y)] from a left half-width table and a right half-width table sampled at the heights ys."""
    return [(-K.interp(wl, y), y) for y in ys] + [(K.interp(wr, y), y) for y in reversed(ys)]


def pegs_x(prefix, x0, x1, y, z, r, mat, part, n_sides=6):
    """A tuning peg lying along x from x0 to x1 with a flat knob at the end."""
    Lb.lathe(prefix, [(r * 0.5, x0), (r * 0.55, (x0 + x1) / 2), (r * 0.5, x1 - (x1 - x0) * 0.2), (r * 1.6, x1 - (x1 - x0) * 0.1), (r * 1.4, x1)], mat, part, sides=n_sides, axis="x", origin=(0.0, y, z))


# ================================================================ 1. STUN BEAT GUITAR
def guitar_pal():
    P = {}
    P["burst"] = K.surface("gt_burst", rgb(232, 142, 36), 0.0, 0.32, bump="grain", bump_scale=1.6, bump_strength=0.5, groove=rgb(120, 40, 10), groove_amount=0.6, dirt=0.6, dirt_color=(0.02, 0.005, 0.0, 1),
                           mottle=0.9, mottle_color=rgb(150, 30, 12), wear=0.7, wear_color=rgb(255, 224, 160), wear_metal=0.0, fine=0.4, seed=901, bevel=0.1, ao_dist=2.0,
                           cramp=("y", 0.0, 34.0, [(0.0, rgb(130, 24, 10)), (0.35, rgb(222, 120, 30)), (0.7, rgb(246, 196, 70)), (1.0, rgb(150, 36, 12))]))
    P["bind"] = Lb.paint("gt_bind", rgb(240, 228, 196), rgb(255, 252, 235), 902, rough=0.4, dirt_col=(0.06, 0.04, 0.02, 1))
    P["neck"] = wood("gt_neck", rgb(150, 92, 52), rgb(50, 24, 10), 903, mottle=rgb(90, 50, 24))
    P["board"] = wood("gt_board", rgb(40, 26, 22), rgb(6, 3, 2), 904, mottle=rgb(70, 46, 36), scale=2.2)
    P["chrome"] = Lb.metal("gt_chrome", rgb(200, 206, 216), rgb(255, 255, 255), 905, rough=0.18, dirt=0.4, dirt_col=(0.03, 0.03, 0.04, 1), mottle=rgb(130, 140, 160))
    P["black"] = Lb.paint("gt_black", rgb(16, 16, 20), rgb(120, 120, 130), 906, rough=0.3)
    P["gold"] = Lb.hammered("gt_gold", rgb(226, 176, 62), rgb(255, 244, 200), 907, mottle=rgb(160, 100, 30))
    P["string"] = Lb.metal("gt_string", rgb(214, 214, 205), rgb(255, 255, 255), 908, rough=0.15, dirt=0.1, bump=None)
    P["bolt"] = Lb.glow("gt_bolt", rgb(255, 226, 80), 1.0, 909, flicker=0.6, ramp=("y", 0.0, 34.0, [(0.0, rgb(255, 170, 40)), (0.5, rgb(255, 235, 110)), (1.0, rgb(255, 250, 200))]))
    P["wave"] = Lb.glow("gt_wave", rgb(255, 220, 90), 0.75, 910, flicker=0.8)
    P["strap"] = K.surface("gt_strap", rgb(70, 36, 24), 0.0, 0.7, bump="leather", bump_strength=0.7, dirt=0.7, mottle=0.8, mottle_color=rgb(120, 70, 40), wear=0.6, wear_color=rgb(200, 150, 110), wear_metal=0.0, seed=911, bevel=0.0)
    return P


def stun_guitar(opts):
    C.begin(opts)
    C.weights(body=1.2, top=1.2, neck=1.0, bolts=1.0, waves=0.8, strap=0.8)
    P = guitar_pal()
    rr = K.rng(9101)
    ys = [0, 2.5, 6, 10, 14, 18, 21, 24, 27, 30, 32.5, 34.5]
    wl = [(0, 4.2), (2.5, 8.6), (6, 11.2), (10, 11.9), (14, 10.8), (18, 8.6), (21, 7.4), (24, 8.0), (27, 9.4), (30, 9.8), (32.5, 8.6), (34.5, 5.2)]
    wr = [(0, 4.2), (2.5, 8.6), (6, 11.4), (10, 12.2), (14, 11.0), (18, 8.8), (21, 7.6), (24, 8.0), (27, 7.4), (30, 5.6), (32.5, 3.6), (34.5, 2.4)]            # the right shoulder is a deep cutaway horn
    out = outline(wl, wr, ys)
    K.plate("body", out, -3.6, 1.2, P["bind"], part="body", bevel_w=0.35)
    top = K.inset_poly(out, 0.8)
    K.plate("top", top, 1.2, 1.75, P["burst"], part="top", bevel_w=0.12)
    K.plate("back", K.inset_poly(out, 0.7), -4.15, -3.6, P["burst"], part="body_back", bevel_w=0.1)
    for rim in (0.0, -0.6):                                                                  # ribs seen from the side: two dark purfling lines
        K.seam("purfling%d" % (rim < 0), [(p[0], p[1]) for p in K.inset_poly(out, 1.3 - rim)] + [K.inset_poly(out, 1.3 - rim)[0]], P["black"], w=0.1, depth=0.1, z=1.78, part="top", detail=True)
    # sound hole with a rosette of rings and a speaker-like cone, a pickguard, a bridge and its saddle
    Lb.ring_torus("hole_ring", (0.0, 17.5, 1.8), 4.6, 0.5, P["gold"], "rosette", plane="xy", sides=5, segs=24)
    Lb.ring_torus("hole_ring2", (0.0, 17.5, 1.8), 5.7, 0.3, P["black"], "rosette", plane="xy", sides=4, segs=24)
    K.plate("hole", [(4.2 * math.cos(TAU * i / 24), 17.5 + 4.2 * math.sin(TAU * i / 24)) for i in range(24)], 1.4, 1.9, P["black"], part="top", bevel_w=0.0)
    K.plate("pickguard", [(5.8, 11.5), (8.4, 15.0), (7.8, 24.0), (5.0, 26.6), (2.5, 22.0), (3.2, 13.0)], 1.75, 2.05, P["black"], part="top", bevel_w=0.1)
    K.plate("bridge", [(-4.6, 7.8), (4.6, 7.8), (4.2, 10.0), (-4.2, 10.0)], 1.75, 2.8, P["board"], part="top", bevel_w=0.18)
    K.bar("saddle", (-3.8, 9.5, 2.9), (3.8, 9.5, 2.9), 0.35, 0.3, P["bind"], part="top")
    for k in range(6):
        K.rivets("bpin%d" % k, [(V((-3.2 + k * 1.28, 8.5, 2.85)), (0, 0, 1))], P["chrome"], r=0.34, rng_=rr, sides=6, detail=False)
    # two humbucker pickups and three knobs
    for k, y in enumerate((20.0, 26.0)):
        K.plate("pickup%d" % k, [(-2.6, y - 0.9), (2.6, y - 0.9), (2.6, y + 0.9), (-2.6, y + 0.9)], 1.75, 2.5, P["chrome"], part="pickups", bevel_w=0.14)
        for j in range(6):
            K.rivets("pole%d_%d" % (k, j), [(V((-2.0 + j * 0.8, y, 2.5)), (0, 0, 1))], P["gold"], r=0.28, rng_=rr, sides=6, detail=False)
    for k, (x, y) in enumerate(((3.6, 4.4), (6.4, 5.4), (8.4, 7.4))):
        Lb.lathe("knob%d" % k, [(0.01, 0.0), (0.8, 0.0), (0.9, 0.7), (0.7, 1.1), (0.01, 1.1)], P["chrome"], "knobs", sides=8, axis="z", origin=(x, y, 1.75))
    # neck and fretboard, frets and position dots, a bone nut
    K.bar("neck", (0.0, 32.0, -0.5), (0.0, 62.0, -0.6), 2.0, 1.15, P["neck"], part="neck", taper=0.85)
    K.plate("board", [(-1.95, 33.0), (1.95, 33.0), (1.6, 61.0), (-1.6, 61.0)], 0.55, 1.2, P["board"], part="neck", bevel_w=0.1)
    for n in range(1, 17):
        y = 33.0 + (61.0 - 33.0) * (1.0 - 2.0 ** (-n / 12.0)) * 2.2 / 1.1 * 0.5
        y = 33.0 + (28.0) * (1.0 - 2.0 ** (-n / 12.0)) * 1.0 / (1.0 - 2.0 ** (-16 / 12.0))
        wd = K.lerp(1.95, 1.6, (y - 33.0) / 28.0)
        K.bar("fret%d" % n, (-wd, y, 1.3), (wd, y, 1.3), 0.12, 0.09, P["chrome"], part="neck", detail=True)
    for y in (39.0, 44.0, 48.0, 52.0, 58.0):
        K.blob("dot%d" % int(y), (0.0, y, 1.25), (0.4, 0.4, 0.14), P["bind"], seed=1, lump=0.0, rings=3, segs=6, part="neck", detail=True)
    K.bar("nut", (-1.6, 61.4, 1.3), (1.6, 61.4, 1.3), 0.25, 0.3, P["bind"], part="neck")
    # the headstock: a slanted, notched plate with six pegs (three each side) and gold ornament, strings run on to the pegs
    head = [(-3.0, 61.8), (3.0, 61.8), (3.4, 66.0), (3.0, 70.2), (1.4, 73.4), (-1.4, 73.4), (-3.0, 70.2), (-3.4, 66.0)]
    K.stepped_plate("head", head, -1.9, P["neck"], steps=2, step_h=0.9, inset=0.35, part="headstock", up=1, bevel_w=0.1)
    for k in range(3):
        y = 64.0 + k * 3.2
        pegs_x("pegL%d" % k, -3.2, -6.4, y, -0.7, 0.55, P["chrome"], "headstock")
        pegs_x("pegR%d" % k, 3.2, 6.4, y, -0.7, 0.55, P["chrome"], "headstock")
    K.gem_setting("headgem", V((0.0, 69.8, 0.35)), 1.2, P["bolt"], P["gold"], normal=(0, 0, 1), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.4)
    xs_b = [-3.2 + k * 1.28 for k in range(6)]
    xs_n = [-1.25 + k * 0.5 for k in range(6)]
    for k in range(6):
        K.bar("string%d" % k, (xs_b[k], 9.3, 3.0), (xs_n[k], 61.5, 1.55), 0.11, 0.09, P["string"], part="strings", bevel_w=0.0)
    # the stun beat: a golden lightning bolt across the body, a pulse of sound-wave arcs round the sound hole
    pts = [(-8.0, 31.0), (-1.2, 25.0), (-3.6, 22.4), (4.6, 14.2), (1.8, 13.0), (8.2, 4.4)]
    for s_ in (1,):
        rows = []
        for i, p in enumerate(pts):
            n_ = V((1.0, 0.0, 0.0))
            rows.append((V((p[0] - 0.9, p[1], 2.05)), V((p[0] + 0.9, p[1], 2.05))))
        K.strip("bolt", rows, P["bolt"], (-1.0, 0.0, 1.0), (0.3, 1.0, 0.3), 0.3, part="bolts", up=(0, 0, 1))
    for k, (rad, a0, a1) in enumerate(((7.4, 0.25, 1.25), (9.2, 0.15, 1.35), (11.0, 0.05, 1.45))):
        for side, flip in ((1, 1), (-1, -1)):
            ptsw = []
            for i in range(9):
                a = K.lerp(a0, a1, i / 8.0)
                ptsw.append(V((side * rad * math.cos(a) * 0.9, 17.5 + rad * math.sin(a) * 1.0 + 0.0, 2.2)))
            K.tapered("wave%d_%d" % (k, side), ptsw, 0.3 + 0.05 * (2 - k), 0.12, P["wave"], sides=4, sub=1, part="waves")
    for k in range(4):                                                                       # sparks flying off the cutaway horn (bounded)
        K.shard("spark%d" % k, V((6.0 + 3.0 * k + rr.uniform(-1, 1), 34.0 + 3.0 * k, rr.uniform(-1, 1) + 1.0)), 0.7, P["bolt"], rr, part="sparks", sides=4, stretch=2.4, dir_=V((0.7, 1.0, 0.0)))
    # binding tube along the body edge, filigree scrolls on the soundboard, a stun speaker cone in the cutaway horn, a tail block
    ring_pts = [V((x, y, 1.45)) for (x, y) in K.inset_poly(out, 0.25)]
    K.tube("binding", ring_pts + [ring_pts[0], ring_pts[1]], 0.28, P["bind"], sides=5, sub=1, part="binding")
    for sx, y0 in ((-1, 27.0), (1, 5.0)):
        K.filigree("scroll%d" % sx, K.spiral((sx * 7.0, y0), 3.0, 0.8, 1.6, start=1.0 * sx, n=12, flip=sx), P["gold"], w=0.4, h=0.28, z=1.85, part="top", sub=3)
    Lb.lathe("cone", [(0.01, 0.0), (1.0, 0.1), (2.6, 0.9), (3.0, 1.6), (2.6, 1.4), (1.2, 0.6), (0.01, 0.4)], P["black"], "speaker", sides=14, axis="z", origin=(3.6, 31.0, 1.8))
    Lb.ring_torus("cone_rim", (3.6, 31.0, 3.4), 2.9, 0.3, P["chrome"], "speaker", plane="xy", sides=4, segs=14)
    K.blob("cone_glow", (3.6, 31.0, 2.6), (1.0, 1.0, 0.5), P["bolt"], seed=2, lump=0.0, rings=4, segs=8, part="speaker")
    Lb.lathe("tailblock", [(0.01, -1.0), (1.3, -0.9), (1.6, 0.2), (1.0, 1.4)], P["chrome"], "body_tail", sides=8)
    # horns on the headstock, a whammy bar, a coiled output cable with a plug, studs on the pickguard
    for k, (dx, ln) in enumerate(((-2.2, 4.2), (0.0, 5.4), (2.2, 4.2))):
        K.tapered("hhorn%d" % k, [V((dx, 72.6, -1.9)), V((dx * 1.5, 72.6 + ln * 0.55, -1.9)), V((dx * 2.0, 72.6 + ln, -1.9))], 0.55, 0.06, P["gold"], sides=5, sub=1, part="headstock")
    K.tapered("whammy", [V((0.0, 9.0, 2.9)), V((-4.0, 8.0, 5.0)), V((-8.0, 6.4, 8.6))], 0.28, 0.2, P["chrome"], sides=6, sub=2, part="top")
    K.blob("whammy_tip", (-8.0, 6.4, 8.8), (0.5, 0.5, 0.5), P["gold"], seed=3, lump=0.0, rings=4, segs=6, part="top")
    cab = [V(p) for p in K.bl.smooth_path([(5.5, 1.0, 0.4), (10.0, -3.0, 1.6), (6.0, -7.4, 2.8), (-1.0, -6.0, 1.8), (-2.0, -10.8, 0.6), (4.0, -14.0, 1.2), (8.0, -17.0, 0.4)], 4)]
    K.tube("cable", cab, 0.4, P["strap"], sides=6, sub=1, part="cable")
    Lb.lathe("plug", [(0.01, -17.0), (0.7, -17.0), (0.7, -19.4), (0.45, -19.6), (0.45, -22.0)], P["chrome"], "cable", sides=8, origin=(8.0, 0, 0.4))
    K.rivets("pg_studs", [(V((x, y, 2.05)), (0, 0, 1)) for x, y in ((6.2, 13.0), (7.6, 17.0), (7.2, 22.0), (5.2, 24.6), (3.0, 20.6), (3.6, 14.4))], P["gold"], r=0.3, rng_=rr, sides=6, detail=False)
    # strap: a leather strap from the headstock-side shoulder swinging out to the left and back to the tail button
    sp = [V((-8.8, 29.0, -2.2)), V((-15.0, 22.0, -2.6)), V((-14.0, 10.0, -2.8)), V((-6.0, 1.0, -3.2))]
    path = [V(p) for p in K.bl.smooth_path(sp, 5)]
    K.ribbon("strap", path, 2.4, P["strap"], facing=(1, 0, 0.2), thick=0.14, part="strap", fray=0.0, rng_=rr, edge_wave=0.02)
    for k, (x, y, z) in enumerate(((-8.8, 29.0, -2.0), (-6.0, 1.0, -3.4))):
        Lb.lathe("button%d" % k, [(0.01, -0.5), (0.9, -0.3), (1.0, 0.6), (0.01, 0.8)], P["chrome"], "strap", sides=8, axis="z", origin=(x, y, z - 0.2))
    return K.quad_estimate()


# ================================================================ 2. HUMMING FAIRY LYRE
def lyre_pal():
    P = {}
    P["gold"] = Lb.hammered("ly_gold", rgb(232, 184, 70), rgb(255, 246, 208), 1001, rough=0.28, mottle=rgb(170, 110, 34))
    P["gold2"] = Lb.metal("ly_gold2", rgb(190, 140, 52), rgb(250, 222, 150), 1002, rough=0.4, dirt_col=(0.05, 0.025, 0.004, 1), bump="hammer", bs=0.15)
    P["wood"] = wood("ly_wood", rgb(210, 150, 100), rgb(90, 44, 24), 1003, mottle=rgb(150, 90, 60))
    P["rose"] = wood("ly_rose", rgb(160, 80, 70), rgb(60, 20, 18), 1004, mottle=rgb(110, 50, 44))
    P["ame"] = Lb.gem("ly_ame", rgb(170, 90, 255), 0.9, 1005)
    P["wing"] = K.surface("ly_wing", rgb(210, 190, 255), 0.0, 0.2, mottle=1.0, mottle_color=rgb(255, 255, 255), seed=1006, bevel=0.0,
                          rim=(rgb(255, 245, 255), 0.6), scratches=0.5, tramp=[(0.0, rgb(150, 120, 240)), (0.5, rgb(210, 190, 255)), (1.0, rgb(255, 214, 240))])
    P["vein"] = Lb.glow("ly_vein", rgb(230, 190, 255), 0.7, 1007, flicker=0.6)
    P["leaf"] = Lb.paint("ly_leaf", rgb(96, 170, 100), rgb(200, 240, 180), 1008, rough=0.6, mottle=rgb(50, 110, 60))
    P["vinem"] = K.surface("ly_vine", rgb(70, 120, 70), 0.0, 0.7, bump="grain", bump_scale=1.4, bump_strength=0.7, groove=rgb(14, 36, 16), groove_amount=0.8, dirt=0.6, seed=1009, bevel=0.0)
    P["petal"] = K.surface("ly_petal", rgb(255, 200, 224), 0.0, 0.6, tramp=[(0.0, rgb(255, 240, 246)), (0.6, rgb(255, 190, 214)), (1.0, rgb(240, 130, 180))], wear=0.4, wear_color=rgb(255, 250, 252), wear_metal=0.0, mottle=0.7,
                           mottle_color=rgb(255, 140, 190), seed=1010, bevel=0.0, fine=0.3)
    P["string"] = Lb.glow("ly_string", rgb(255, 238, 190), 0.8, 1011, flicker=0.4)
    P["orb"] = Lb.gem("ly_orb", rgb(255, 240, 160), 1.2, 1012)
    P["heart"] = Lb.glow("ly_heart", rgb(255, 214, 120), 0.9, 1013, flicker=0.4)
    P["stamen"] = Lb.paint("ly_stamen", rgb(255, 232, 190), rgb(255, 255, 240), 1014, rough=0.5)
    return P


def fairy_wing(name, root, ang, length, width, mat, vein_mat, rr, z, seed, curve=0.0):
    """A dragonfly wing: a lens-section blade shaped like a long teardrop rooted at `root`, with a bright leading vein and four cross veins (real geometry)."""
    d = V((math.cos(ang), math.sin(ang), 0))
    rows = Lb.lancet_rows(V((root[0], root[1], z)), d, length, width, n=9, curve=curve, belly=0.55)
    K.strip(name, rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.12, part="wings", up=(0, 0, 1), angle=60)
    s = V((-d.y, d.x, 0))
    lead = [(l * 0.0 + r) for l, r in rows]
    K.tapered(name + "_lead", [V(p) + V((0, 0, 0.15)) for p in lead[:-1:2]] + [V(lead[-1])], 0.22, 0.07, vein_mat, sides=4, sub=1, part="wing_veins")
    for k in range(1, 5):
        l, r = rows[k * 2]
        K.tapered("%s_x%d" % (name, k), [V(l) + V((0, 0, 0.12)), (V(l) + V(r)) * 0.5 + V((0, 0, 0.16)), V(r) + V((0, 0, 0.12))], 0.1, 0.1, vein_mat, sides=3, sub=1, part="wing_veins")


def fairy_lyre(opts):
    C.begin(opts)
    C.weights(body=1.2, arms=1.0, wings=0.9, strings=0.7, orbs=0.8)
    P = lyre_pal()
    rr = K.rng(10102)
    # the sound box: a deep rounded bowl (a half-lathe cut), a gold rim, a rose-wood soundboard with a heart-shaped sound hole of petals
    box = K.bl.lathe("box", [(0.4, 0.2), (4.6, 0.8), (8.6, 3.6), (10.6, 8.0), (11.0, 13.0), (10.4, 18.0), (9.2, 22.0)], P["wood"], sides=18, axis="y", part="body", squash=0.5, caps=(True, False))
    K._finish_obj(box, "body", True, 50.0)
    Lb.ring_torus("box_rim", (0.0, 22.0, 0.0), 9.4, 0.8, P["gold"], "body_rim", plane="xz", sides=6, segs=20, squash=0.5)
    K.plate("board", [(9.0 * math.cos(TAU * i / 24), 12.0 + 10.2 * math.sin(TAU * i / 24) * 1.0) for i in range(24)], 3.8, 4.6, P["rose"], part="soundboard", bevel_w=0.2)
    for k in range(6):
        a = TAU * k / 6
        Lb.petal("hole%d" % k, V((0.0, 14.0, 4.7)), V((math.cos(a), math.sin(a), 0.1)), 3.4, 2.2, P["gold"], facing=(0, 0, 1), curl=0.5, cup=0.2, thick=0.18, part="soundboard", seed=k, simple=True, rows=4, notch=False)
    K.blob("holeheart", (0.0, 14.0, 4.6), (1.1, 1.1, 0.7), P["heart"], seed=2, lump=0.05, rings=4, segs=8, part="soundboard")
    K.plate("bridge", [(-5.0, 22.4), (5.0, 22.4), (4.6, 24.2), (-4.6, 24.2)], 3.9, 5.2, P["gold2"], part="soundboard", bevel_w=0.15)
    Lb.lathe("tail", [(0.01, -1.2), (0.6, -1.0), (1.2, 0.2), (2.2, 1.6), (1.6, 2.6)], P["gold"], "tail_tip", sides=8)
    # the arms: two long, outward-then-inward curves rising from the box rim, gold-bound, tipped with flower buds; a yoke across the top with six amethyst pegs
    for sx in (-1, 1):
        pts = [V((sx * 8.6, 21.0, 0.0)), V((sx * 13.0 + 0.4 * sx, 30.0, 0.0)), V((sx * 14.6, 42.0, 0.0)), V((sx * 12.2, 54.0, 0.0)), V((sx * 8.2, 63.0, 0.0)), V((sx * 6.2, 65.4, 0.0))]
        K.tube("arm%d" % sx, pts, 1.2, P["wood"], sides=8, sub=4, part="arms", taper=(1.5, 1.0))
        for j, y in enumerate((26.0, 38.0, 50.0, 60.0)):
            xx = sx * K.interp([(21, 8.6), (30, 13.4), (42, 14.6), (54, 12.2), (63, 8.2)], y)
            if j % 2 == 0:
                K.gem_setting("armgem%d_%d" % (sx, j), V((xx, y, 1.4)), 0.7, P["ame"], P["gold"], normal=(0, 0, 1), claws=0, sides=6, part="gem", rng_=rr, claw_r=0.3)
        # leaf-and-flower vine winding round the arm
        vp = []
        for i in range(11):
            t = i / 10.0
            y = 22.0 + 40.0 * t
            xx = sx * K.interp([(21, 8.6), (30, 13.4), (42, 14.6), (54, 12.2), (63, 8.2)], y)
            vp.append(V((xx + 1.7 * math.sin(t * 17.0 + sx), y, 1.7 * math.cos(t * 17.0 + sx))))
        K.vine("vine%d" % sx, vp, 0.34, P["vinem"], sides=5, leaves=3, leaf_mat=P["leaf"], rng_=rr, leaf_len=3.2, part="vines")
        for j, t in enumerate((0.4, 0.85)):
            i = int(t * 10)
            Lb.blossom("fl%d_%d" % (sx, j), vp[i] + V((0, 0, 0.5)), V((sx * 0.2, 0.2, 1.0)), 3.0 - 0.3 * j, P["petal"], P["stamen"], P["stamen"], rr, part="flowers", rows=3)
    yoke = [V((-6.6, 64.8, 0.0)), V((0.0, 66.6, 0.0)), V((6.6, 64.8, 0.0))]
    K.tube("yoke", yoke, 1.1, P["gold"], sides=8, sub=3, part="yoke")
    for k in range(7):
        x = -5.4 + k * 1.8
        Lb.lathe("peg%d" % k, [(0.2, -0.6), (0.7, -0.4), (0.7, 1.4), (1.1, 1.8), (0.01, 2.0)], P["gold2"], "yoke", sides=6, axis="z", origin=(x, 65.7 + 0.5 * (1 - abs(x) / 6.0), 0.8))
    for k, x in enumerate((-3.6, 0.0, 3.6)):
        K.gem_setting("yg%d" % k, V((x, 64.2 + 1.6 * (1 - abs(x) / 5.0), 1.8)), 0.9, P["ame"], P["gold"], normal=(0, 0, 1), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.3)
    for k in range(9):                                                                         # nine glowing strings, box to yoke
        xb = -4.4 + k * 1.1
        xt = -5.2 + k * 1.3
        K.bar("string%d" % k, (xb, 24.4, 5.1), (xt, 65.0, 1.6), 0.1, 0.09, P["string"], part="strings", bevel_w=0.0)
    # fairy wings: a big upper and a small lower wing behind each shoulder of the box (translucent, veined); left and right are not twins
    wl = ((1, 0.95, 36.0, 12.0, 0.22), (1, -0.55, 21.0, 8.4, -0.15), (-1, math.pi - 0.82, 33.0, 11.0, -0.2), (-1, math.pi + 0.5, 19.0, 7.8, 0.15))
    for k, (sx, ang, ln, wd, cv) in enumerate(wl):
        fairy_wing("wing%d" % k, (sx * 7.0, 17.0), ang, ln, wd, P["wing"], P["vein"], rr, -3.4 - 0.5 * (k % 2), k, curve=cv * ln * sx)
    # fairy orbs: small glowing beads with two tiny wings each (bounded: 4)
    for k, (x, y, z) in enumerate(((-17.0, 58.0, 2.0), (17.5, 46.0, 1.0), (-19.0, 33.0, -1.0), (16.0, 66.0, -2.0))):
        K.blob("orb%d" % k, (x, y, z), (1.2, 1.2, 1.2), P["orb"], seed=k, lump=0.05, rings=5, segs=8, part="orbs")
        for s_ in (-1, 1):
            Lb.petal("owing%d_%d" % (k, s_), V((x, y + 0.4, z)), V((s_ * 0.8, 0.7, 0.0)), 3.0, 1.5, P["wing"], facing=(0, 0, 1), curl=0.3, cup=0.1, thick=0.06, part="orbs", seed=k + s_, simple=True, rows=3, notch=False)
    return K.quad_estimate()


# ================================================================ 3. GRILLED FISH HORN
def fish_pal():
    P = {}
    P["skin"] = K.surface("fh_skin", rgb(206, 132, 54), 0.0, 0.42, bump="scales", bump_scale=2.2, bump_strength=0.9, groove=rgb(96, 44, 14), groove_amount=0.7, dirt=0.7, dirt_color=(0.03, 0.012, 0.0, 1),
                          mottle=1.0, mottle_color=rgb(60, 24, 8), wear=0.7, wear_color=rgb(255, 214, 140), wear_metal=0.0, fine=0.5, seed=1101, bevel=0.1, ao_dist=2.2,
                          cramp=("y", 10.0, 66.0, [(0.0, rgb(120, 56, 20)), (0.4, rgb(214, 140, 60)), (0.75, rgb(238, 176, 84)), (1.0, rgb(150, 80, 30))]))
    P["belly"] = Lb.paint("fh_belly", rgb(238, 214, 168), rgb(255, 244, 214), 1102, rough=0.5, dirt_col=(0.08, 0.04, 0.01, 1), mottle=rgb(190, 140, 90), bump="scales")
    P["char"] = Lb.paint("fh_char", rgb(14, 10, 8), rgb(80, 60, 44), 1103, rough=0.8, bump="leather")
    P["fin"] = K.surface("fh_fin", rgb(190, 112, 44), 0.0, 0.55, tramp=[(0.0, rgb(150, 70, 26)), (0.6, rgb(226, 150, 70)), (1.0, rgb(250, 214, 140))], wear=0.6, wear_color=rgb(255, 230, 170), wear_metal=0.0, dirt=0.6,
                         dirt_color=(0.03, 0.012, 0.0, 1), mottle=0.8, mottle_color=rgb(60, 24, 8), seed=1104, bevel=0.0, fine=0.4)
    P["brass"] = Lb.hammered("fh_brass", rgb(224, 170, 64), rgb(255, 244, 196), 1105, rough=0.22, mottle=rgb(150, 90, 24))
    P["brass2"] = Lb.metal("fh_brass2", rgb(176, 124, 46), rgb(240, 206, 130), 1106, rough=0.38, dirt_col=(0.05, 0.025, 0.004, 1), bump="hammer", bs=0.15)
    P["bamboo"] = K.surface("fh_bamboo", rgb(206, 178, 96), 0.0, 0.5, bump="grain", bump_scale=2.4, bump_strength=0.7, groove=rgb(90, 66, 24), groove_amount=0.7, dirt=0.6, mottle=0.9, mottle_color=rgb(130, 150, 70),
                            wear=0.5, wear_color=rgb(240, 220, 150), wear_metal=0.0, seed=1107, bevel=0.1)
    P["paper"] = Lb.cloth("fh_paper", rgb(240, 232, 214), rgb(150, 130, 100), 1108, mottle=rgb(200, 180, 140), bump="cross", scale=0.8)
    P["sclera"] = Lb.paint("fh_sclera", rgb(236, 228, 200), rgb(255, 252, 230), 1109, rough=0.3, mottle=rgb(200, 190, 150))
    P["iris"] = Lb.gem("fh_iris", rgb(255, 190, 40), 0.8, 1110)
    P["pupil"] = Lb.paint("fh_pupil", rgb(4, 3, 2), rgb(30, 24, 16), 1111, rough=0.2)
    P["steam"] = Lb.glow("fh_steam", rgb(255, 236, 200), 0.55, 1112, flicker=0.9, ramp=("y", 40.0, 76.0, [(0.0, rgb(255, 170, 80)), (0.5, rgb(255, 224, 170)), (1.0, rgb(255, 250, 240))]))
    P["lemon"] = Lb.paint("fh_lemon", rgb(252, 224, 70), rgb(255, 250, 190), 1113, rough=0.5, mottle=rgb(250, 190, 40), bump="leather")
    P["leaf"] = Lb.paint("fh_leaf", rgb(80, 160, 70), rgb(190, 235, 160), 1114, rough=0.6, mottle=rgb(40, 100, 44))
    P["ember"] = Lb.glow("fh_ember", rgb(255, 130, 40), 0.8, 1115, flicker=0.9)
    return P


def fish_horn(opts):
    C.begin(opts)
    C.weights(fish=1.2, bell=1.0, fins=0.9, steam=0.7, skewer=0.8)
    P = fish_pal()
    rr = K.rng(11103)
    prof = [(11.0, 1.0), (14.0, 1.4), (18.0, 2.6), (23.0, 4.4), (30.0, 6.4), (38.0, 7.6), (46.0, 7.2), (53.0, 6.0), (59.0, 4.6), (62.0, 3.6)]
    dx = lambda y: 0.9 * math.sin((y - 10.0) / 52.0 * math.pi) * 0.0                                     # noqa: E731
    rings = []
    for i in range(27):
        y = 11.0 + 51.0 * i / 26.0
        rx_ = K.interp(prof, y)
        rz = rx_ * 0.58 + 0.35
        ring = [V((dx(y) + rx_ * math.cos(TAU * k / 18) * (1.0 + 0.04 * K.noise3((k, i * 0.7, 0), 0.8, 3)), y, rz * math.sin(TAU * k / 18))) for k in range(18)]
        rings.append(ring)
    fb = K._loft("fishbody", rings, P["skin"], (True, False), "fish")
    # the belly stripe (lighter underside) along the -x side, lying on the body
    for s_ in (1, -1):
        rows = []
        for (y, rx_) in prof[1:-1]:
            rz = rx_ * 0.58 + 0.35
            rows.append((V((-rx_ * 0.96, y, s_ * rz * 0.28)), V((-rx_ * 0.62, y, s_ * rz * 0.78))))
        K.strip("belly%d" % s_, rows, P["belly"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.12, part="belly", up=(0, 0, s_), detail=True)
    # grill marks: charred bars across both sides (bake only), burnt tail and fin edges
    for s_ in (1, -1):
        for k in range(8):
            y = 20.0 + k * 5.2 + rr.uniform(-0.5, 0.5)
            rx_ = K.interp(prof, y) * 0.8
            rz = K.interp(prof, y) * 0.58 + 0.35
            rows = []
            for j in range(7):
                u = -0.86 + 1.72 * j / 6.0
                zz = s_ * rz * math.sqrt(max(0.04, 1.0 - u * u)) * 1.0
                yy = y + 2.4 * u
                rows.append((V((u * rx_ / 0.8 * 0.8, yy - 0.5, zz)), V((u * rx_ / 0.8 * 0.8, yy + 0.5, zz))))
            K.strip("grill%d_%d" % (s_, k), rows, P["char"], (-1.0, 0.0, 1.0), (0.2, 1.0, 0.2), 0.3, part="grillmarks", up=(0, 0, s_), detail=True)
    # head: gill plate, a big round eye, lips, and the open mouth that carries the brass bell
    for s_ in (1, -1):
        gp = Lb.lancet_rows(V((-2.0, 47.0, 0.0)), V((0.05, 1.0, 0.0)), 14.0, 8.4, n=7, belly=0.4)
        rows = [(l + V((0.0, 0.0, s_ * (4.5 - 0.0))), r + V((0.0, 0.0, s_ * 4.5))) for l, r in gp]
        K.strip("gill%d" % s_, rows, P["skin"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.5, part="fish_gill", up=(0, 0, s_), angle=60)
    Lb.eye("fisheye", V((1.0, 55.0, 4.3)), 1.9, V((0.2, 0.0, 1.0)), P["sclera"], P["iris"], P["pupil"], P["brass"], part="fish_eye", rng_=rr, slit=False)
    Lb.lathe("lip", [(3.5, 59.4), (4.8, 60.4), (4.6, 62.0), (3.4, 62.6)], P["belly"], "fish_lips", sides=14, squash=0.62, angle=60.0)
    Lb.lathe("bell", [(3.2, 61.0), (3.0, 63.0), (3.6, 65.6), (5.2, 68.0), (7.4, 70.4), (8.6, 71.6), (8.0, 72.2), (6.8, 70.6), (4.6, 67.4), (2.6, 64.2), (2.2, 61.4)], P["brass"], "bell", sides=18, squash=0.78, caps=(False, False))
    Lb.ring_torus("bellrim", (0.0, 71.8, 0.0), 8.0, 0.55, P["brass2"], "bell", plane="xz", sides=5, segs=22, squash=0.78)
    # fins: tail (a fan of five lancets), dorsal fan along the +x back (seven), pelvic fan along -x (four), pectoral on the flank
    for k in range(5):
        ang = math.radians(-90 + (k - 2) * 19.0)
        Lb.lancet("tailfin%d" % k, V((0.0, 12.0, 0.0)), V((math.cos(ang), math.sin(ang), 0)), 13.0 - 1.5 * abs(k - 2), 4.6, P["fin"], thick=0.22, part="fins", curve=(k - 2) * 0.5, n=7, belly=0.5)
    for k in range(7):
        y = 22.0 + k * 4.4
        bx = K.interp(prof, y)
        ang = math.radians(35 + k * 4.0)
        Lb.lancet("dorsal%d" % k, V((bx * 0.92, y, 0.0)), V((math.cos(ang), math.sin(ang), 0)), 7.0 - 0.45 * abs(k - 3), 2.2, P["fin"], thick=0.2, part="fins", curve=-1.3, n=5, belly=0.4)
    for k in range(4):
        y = 30.0 + k * 5.0
        bx = K.interp(prof, y)
        ang = math.radians(210 - k * 6.0)
        Lb.lancet("pelvic%d" % k, V((-bx * 0.9, y, 0.0)), V((math.cos(ang), math.sin(ang), 0)), 6.0 - 0.6 * k, 2.0, P["fin"], thick=0.2, part="fins", curve=1.0, n=5)
    L_, R_, C_ = K.leaf_rows(V((1.5, 46.0, 4.7)), V((0.2, -1.0, 0.1)), 12.0, 5.6, curl=2.0, rows=7, facing=(0, 0, 1), seed=3)
    K.strip("pectoral", list(zip(L_, R_)), P["fin"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.25, part="fins", up=(0, 0, 1), angle=60)
    # brass piping: three valves on the back with their little tubes, and a long coiled pipe from the bell down the back
    for k, y in enumerate((38.0, 44.0, 50.0)):
        bx = K.interp(prof, y) + 0.8
        Lb.lathe("valve%d" % k, [(1.0, 0.0), (1.0, 2.4), (1.4, 2.8), (1.4, 3.6), (0.8, 4.0), (0.01, 4.1)], P["brass"], "valves", sides=8, axis="x", origin=(bx - 0.4, y, 0.6))
    pp = [V(p) for p in ((7.0, 66.0, 0.0), (11.4, 60.0, 1.0), (10.6, 48.0, 2.2), (9.4, 36.0, 1.4), (11.8, 26.0, 0.8), (8.0, 20.0, 0.2))]
    K.tapered("pipe", pp, 0.95, 0.7, P["brass2"], sides=7, sub=3, part="piping")
    for k, y in enumerate((60.0, 48.0, 36.0, 26.0)):
        Lb.ring_torus("pipering%d" % k, (K.interp([(26, 11.8), (36, 9.4), (48, 10.6), (60, 11.4)], y), y, K.interp([(26, 0.8), (36, 1.4), (48, 2.2), (60, 1.0)], y)), 1.15, 0.28, P["brass"], "piping", plane="xz", sides=4, segs=8)
    # scale plates laid over both flanks in overlapping rows (real, shingled), burnt crust lumps along the back, the exposed fish-bones of a bitten belly (-x side)
    for s_ in (1, -1):
        for row in range(4):
            for col in range(6):
                y = 21.0 + col * 6.0 + (3.0 if row % 2 else 0.0)
                rx_ = K.interp(prof, y)
                u = -0.62 + row * 0.3
                if abs(u * rx_) > rx_ * 0.9:
                    continue
                rz = rx_ * 0.58 + 0.35
                zz = s_ * rz * math.sqrt(max(0.06, 1 - u * u)) * 1.02
                K.scale_plate("scale%d_%d_%d" % (s_, row, col), V((u * rx_, y, zz)), V((0.0, 0.1, s_)), 1.5, P["fin"] if (row + col) % 3 else P["belly"], up=(0, 1, 0), thick=0.12, rows=2, part="scales", tilt=22.0)
    for k in range(7):
        y = 24.0 + k * 6.0
        K.blob("crust%d" % k, (K.interp(prof, y) * 0.8 + rr.uniform(-0.5, 0.5), y, rr.uniform(-0.8, 0.8)), (rr.uniform(0.8, 1.4), rr.uniform(1.0, 1.8), 1.0), P["char"], seed=k + 20, lump=0.25, rings=4, segs=7, part="crust")
    for k in range(6):
        y = 26.0 + k * 3.8
        bx = -K.interp(prof, y) * 0.7
        K.bone("fbone%d" % k, V((-1.0, y, 1.5)), V((bx - 2.8, y - 2.2, 3.4)), 0.3, P["belly"], knob=1.4, sub=2, sides=5, part="fishbones", bow=V((0.0, 1.2, 1.4)), seed=k)
    for k, y in enumerate((42.0, 54.0)):                                         # brass pipe clamps with rivets
        K.rivets("clamp%d" % k, [(V((10.8 + 0.9 * math.cos(a), y, 1.4 + 0.9 * math.sin(a))), (math.cos(a), 0.0, math.sin(a))) for a in [TAU * j / 6 for j in range(6)]], P["brass"], r=0.4, rng_=rr, detail=False, sides=5)
    pp2 = [V(p) for p in ((11.4, 58.0, 1.0), (14.6, 54.0, 2.0), (14.8, 46.0, 2.4), (11.6, 40.0, 2.0))]
    K.tapered("pipe2", pp2, 0.8, 0.7, P["brass"], sides=7, sub=3, part="piping")
    # the skewer: a bamboo stick through the tail and out the bottom (the hold), tied with paper at the grip, charred where it enters the fish
    K.tube("skewer", [V((0.0, -1.2, 0.0)), V((0.0, 6.0, 0.0)), V((0.0, 14.0, 0.0))], 0.8, P["bamboo"], sides=8, sub=1, part="skewer", taper=(0.8, 0.95))
    for k, y in enumerate((2.0, 7.0)):
        Lb.lathe("knot%d" % k, [(0.9, y - 0.6), (1.4, y - 0.3), (1.5, y), (1.4, y + 0.3), (0.9, y + 0.6)], P["bamboo"], "skewer", sides=8)
    K.ribbon("papertie", [V((0.0, 8.8 - 1.2 * j, 1.0 + 0.4 * math.sin(j))) for j in range(5)], 2.0, P["paper"], facing=(1, 0, 0), thick=0.05, part="paper", fray=0.5, rng_=rr, edge_wave=0.1)
    # garnish: a lemon wedge and parsley sprigs at the gill, and steam rising from the bell (bounded)
    for k in range(2):
        K.blob("lemon%d" % k, (-4.0 + 2.2 * k, 40.0 - 2.0 * k, 5.4), (1.5, 1.2, 0.7), P["lemon"], seed=k, lump=0.06, rings=4, segs=8, part="garnish")
    for k in range(4):
        a = k * 1.4
        Lb.petal("parsley%d" % k, V((-1.6, 38.0, 5.4)), V((-0.5 + 0.35 * math.cos(a), -0.2 + 0.3 * math.sin(a), 0.7)), 3.4, 2.0, P["leaf"], facing=(0, 0, 1), curl=0.6, cup=0.2, thick=0.08, part="garnish", seed=k, simple=True, rows=4, notch=True)
    for k, (x, ln, w, bend) in enumerate(((-2.4, 4.6, 1.8, 1.2), (2.2, 5.2, 2.0, -1.6), (0.0, 4.0, 1.6, 0.8))):
        Lb.flame("steam%d" % k, V((x, 72.0 - 0.0 * k, 0.0)), V((0.0, 1.0, 0.0)), ln, w, P["steam"], bend=V((bend, 0, 0)), facing=(0, 0, 1), part="steam", seed=k, rows=6, thick=0.12)
    return K.quad_estimate()


# ================================================================ 4. MUSICAL INSTRUMENT 0
def zero_pal():
    P = {}
    P["violet"] = Lb.metal("zr_violet", rgb(56, 34, 86), rgb(190, 150, 235), 1201, rough=0.3, mottle=rgb(20, 8, 40), dirt=0.7, dirt_col=(0.0, 0.0, 0.01, 1), bump="hammer", bs=0.2,
                           cramp=("y", 0.0, 66.0, [(0.0, rgb(36, 20, 60)), (0.5, rgb(66, 40, 100)), (1.0, rgb(40, 56, 70))]))
    P["black"] = Lb.metal("zr_black", rgb(16, 12, 24), rgb(130, 110, 170), 1202, rough=0.4, dirt=0.5, dirt_col=(0, 0, 0, 1), bump="hammer", bs=0.15)
    P["bone"] = Lb.paint("zr_bone", rgb(214, 204, 176), rgb(255, 252, 230), 1203, rough=0.55, dirt_col=(0.08, 0.05, 0.02, 1), mottle=rgb(150, 130, 90), bump="leather")
    P["puff"] = K.surface("zr_puff", rgb(120, 140, 70), 0.0, 0.5, bump="scales", bump_scale=2.0, bump_strength=0.8, groove=rgb(40, 50, 20), groove_amount=0.7, dirt=0.6, dirt_color=(0.01, 0.02, 0.0, 1), mottle=1.0,
                          mottle_color=rgb(200, 190, 110), wear=0.6, wear_color=rgb(240, 240, 170), wear_metal=0.0, seed=1204, bevel=0.1)
    P["poison"] = Lb.glow("zr_poison", rgb(150, 255, 80), 0.9, 1205, flicker=0.8, ramp=("y", 0.0, 50.0, [(0.0, rgb(110, 60, 220)), (0.5, rgb(120, 230, 90)), (1.0, rgb(200, 255, 120))]))
    P["venom"] = Lb.glow("zr_venom", rgb(190, 90, 255), 0.9, 1206, flicker=0.8)
    P["eye"] = Lb.gem("zr_eye", rgb(255, 50, 80), 1.1, 1207)
    P["string"] = Lb.glow("zr_string", rgb(210, 255, 180), 0.8, 1208, flicker=0.4)
    P["gold"] = Lb.hammered("zr_gold", rgb(214, 170, 70), rgb(255, 240, 190), 1209, mottle=rgb(130, 80, 30))
    P["iron"] = Lb.metal("zr_iron", rgb(110, 110, 124), rgb(220, 224, 235), 1210, rough=0.45, dirt=0.8, dirt_col=(0.02, 0.01, 0.0, 1), bump="hammer", bs=0.2)
    return P


def instrument_zero(opts):
    C.begin(opts)
    C.weights(hoop=1.2, spikes=0.9, head=1.1, drips=0.8, strings=0.7)
    P = zero_pal()
    rr = K.rng(12104)
    cx, cy, rx, ry = 0.0, 24.0, 12.4, 21.0
    # the 0: a heavy oval hoop, ribbed, with bone-ridged bands every few cm
    Lb.ring_torus("hoop", (cx, cy, 0.0), rx, 3.3, P["violet"], "hoop", plane="xy", sides=9, segs=44, squash=ry / rx)
    for k in range(10):
        a = TAU * (k + 0.5) / 10
        if math.sin(a) > 0.9:
            continue
        pos = V((rx * math.cos(a), cy + ry * math.sin(a), 0.0))
        tan = V((-rx * math.sin(a), ry * math.cos(a), 0.0)).normalized()
        outn = V((ry * math.cos(a), rx * math.sin(a), 0.0)).normalized()
        K.bar("rib%d" % k, pos - outn * 0.0 + V((0, 0, 3.2)) - tan * 1.4, pos + V((0, 0, 3.2)) + tan * 1.4, 0.45, 0.35, P["bone"], part="hoop_ribs", bevel_w=0.1)
    # pufferfish spikes all round the hoop, long on the outside, short on the inside; the skin patches between them
    bases, dirs, lens = [], [], []
    for k in range(26):
        a = TAU * (k + rr.uniform(-0.2, 0.2)) / 26
        if math.sin(a) > 0.93:
            continue
        outn = V((ry * math.cos(a), rx * math.sin(a), 0.0)).normalized()
        bases.append(V((rx * math.cos(a), cy + ry * math.sin(a), 0.0)) + outn * 3.0 + V((0, 0, rr.uniform(-1.4, 1.4))))
        dirs.append(outn + V((0, 0, rr.uniform(-0.3, 0.3))))
        lens.append(rr.uniform(3.4, 6.4))
    K.spike_array("spk", bases, dirs, lens, 0.75, P["bone"], rr, bend=0.35, sides=5, part="spikes", len_jit=0.2)
    # strings: eight taut strings straight across the hole (the hoop is the instrument), with a muting bridge in the middle (the silent note)
    for k in range(8):
        x = -rx * 0.6 + k * (rx * 1.2 / 7.0)
        h = ry * math.sqrt(max(0.05, 1 - (x / rx) ** 2))
        K.bar("string%d" % k, (x, cy - h + 2.2, 0.0), (x, cy + h - 2.2, 0.0), 0.12, 0.12, P["string"], part="strings", bevel_w=0.0)
    K.bar("mute", (-rx * 0.62, cy - 0.5, 0.0), (rx * 0.62, cy + 0.5, 0.0), 0.5, 0.9, P["black"], part="mute", bevel_w=0.12)
    for k in range(5):
        K.rivets("mute_rv%d" % k, [(V((-rx * 0.5 + k * rx * 0.25, cy, 0.95)), (0, 0, 1))], P["gold"], r=0.4, rng_=rr, detail=False, sides=6)
    # the neck: a bone spine up from the top of the hoop to the spider-eyed headstock (top stays under y 74)
    K.bone("neck", V((0.0, cy + ry - 1.0, 0.0)), V((0.0, 62.0, 0.0)), 1.5, P["bone"], knob=1.2, sub=4, sides=8, part="neck", bow=V((0.0, 0.0, 0.0)), seed=6)
    for k in range(7):
        y = 48.0 + k * 2.6
        if k % 2 == 0:
            Lb.lathe("vert%d" % k, [(1.7, y - 0.5), (2.2, y - 0.2), (2.3, y + 0.5), (1.7, y + 0.9)], P["bone"], "neck_vert", sides=8, squash=0.9)
    K.blob("headskull", (0.0, 66.0, 0.0), (4.4, 5.2, 3.6), P["puff"], seed=15, lump=0.12, rings=8, segs=12, part="head")
    for k in range(8):                                                                       # eight spider eyes in a cluster, two big in front
        a = TAU * k / 8
        r_ = 1.0 if k >= 2 else 1.5
        x, y = (-1.8 + 3.6 * k, 69.2) if k < 2 else (3.2 * math.cos(a) * 0.9, 66.8 + 3.0 * math.sin(a) * 0.9)
        Lb.eye("seye%d" % k, V((x, y, 3.0)), r_ * 0.8, V((0.2 * math.cos(a), 0.2 * math.sin(a), 1.0)), P["eye"], P["eye"], P["black"], P["gold"], part="head_eyes", rng_=rr, slit=False)
    for sx in (-1, 1):                                                                       # curved fangs either side of the head
        Lb.claw("hfang%d" % sx, V((sx * 2.2, 62.8, 2.4)), V((sx * 0.35, -1.0, 0.4)), 5.2, 0.8, P["bone"], curl=sx * -1.2, side=(-sx, 0, 0), part="head_fangs", sides=6)
    for k, (dx, y) in enumerate(((-5.4, 69.0), (-6.8, 63.0), (5.4, 69.0), (6.8, 63.0))):          # pegs
        pegs_x("peg%d" % k, dx * 0.7, dx * 1.25, y, -1.0, 0.6, P["gold"], "head_pegs")
    # tailpiece: two long venom fangs hanging from the bottom of the hoop, venom beads and drips
    for sx in (-1, 1):
        Lb.claw("fang%d" % sx, V((sx * 3.0, cy - ry - 1.0, 0.0)), V((sx * 0.2, -1.0, 0.0)), 6.6, 1.0, P["bone"], curl=sx * -1.0, side=(-sx, 0, 0), part="fangs", sides=6)
    for k, x in enumerate((-8.0, -2.4, 5.0, 9.0)):
        y0 = cy - ry * math.sqrt(max(0.05, 1 - (x / rx) ** 2)) - 3.0
        K.drip("drip%d" % k, V((x, y0, 0.4)), 4.0 + 2.0 * (k % 3), 0.85, P["venom"] if k % 2 else P["poison"], seed=k + 3, part="drips")
    # broken shackles hanging from the hoop's flank: open cuffs and a few links (the silent note frees allies from bonds)
    for sx in (-1, 1):
        K.chain("chain%d" % sx, [V((sx * (rx + 2.6), cy - 4.0, 0.0)), V((sx * (rx + 4.4), cy - 9.0, 0.4)), V((sx * (rx + 3.4), cy - 14.0, 0.0))], 2.3, 1.5, 0.3, P["iron"], max_links=4, part="chains", sides=4, segs=6)
        Lb.ring_torus("cuff%d" % sx, (sx * (rx + 3.6), cy - 16.4, 0.0), 2.0, 0.55, P["iron"], "chains", plane="xy", sides=5, segs=10)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["guitar"] = (stun_guitar, "vassal_instrument__guitar", "guitar")
BUILDERS["lyre"] = (fairy_lyre, "vassal_instrument__humming_fairy_lyre", "humming_fairy_lyre")
BUILDERS["fish"] = (fish_horn, "vassal_instrument__grilled_fish_horn", "grilled_fish_horn")
BUILDERS["zero"] = (instrument_zero, "vassal_instrument__instrument_zero", "instrument_zero")

if __name__ == "__main__":
    main(sys.argv[1:], BUILDERS, BASE, "wand", MANIFEST)
