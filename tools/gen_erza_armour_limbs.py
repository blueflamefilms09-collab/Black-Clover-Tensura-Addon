"""The skirt, the arms (pauldron, vambrace, gauntlet) and the legs (cuisse, greave, sabaton) of the Erza armours (1.99.0). See gen_erza_armour_parts.py for the vocabulary.

A pair is authored for the RIGHT side (x < 0) and mirrored for the left with `side` = 1 (`V`, `sshell`). The arm pieces are drawn by WaterAuraRig narrowed to 80 percent about the arm's pivot on a slim (Alex)
arm, so they are built around the wide arm (x from -8 to -4, centre -6) with a half width of 2.4 to 2.7: both widths are covered by at least 2 percent. The inner side of every limb piece (toward the body,
the angles 55 to 125 degrees) is left open: nothing can be seen there.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_erza_armour_core import *  # noqa: E402,F401,F403

OPEN = (-235, 55)          # the angles a limb shell covers: everything except the inner side


def _style(spec):
    return spec if isinstance(spec, str) else spec["style"]


# ====================================================================================================================== skirt
def skirt(c):
    P = c.pal
    style = _style(c.spec["skirt"])
    drop = {"tassets": 5.4, "short": 3.8, "cloth": 6.6, "long": 7.6}[style]
    p = c.piece("skirt", skin="body")
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    wide = 5.7 if style != "short" else 5.2
    L = Loft([(11.8 - drop, wide, 3.7, 3.6, 3.2), (11.8 - drop * 0.5, 5.15, 3.2, 3.1, 3.6), (11.85, 4.9, 2.9, 2.85, 4)])
    mats = [P["main"], P["alt"]] if style != "cloth" else [P["cloth"], P["alt"]]
    plates = [(-34, -9, 0.0), (-9, 9, 0.35), (9, 34, 0.7), (40, 78, 1.0), (-78, -40, 1.3), (102, 140, 1.6), (-140, -102, 1.9), (143, 215, 2.2)]
    for k, (t0, t1, ph) in enumerate(plates):
        bn = "f%d" % k
        top = L.point((t0 + t1) / 2.0, 11.85)
        p.bone(bn, (top[0], 11.8, top[2]))
        mat = mats[k % 2].with_(lame=1, trim=0, wear=0.15) if style != "cloth" else mats[k % 2].with_(pleats=1)
        shell_auto(p, bn, L.surf(t0 + 1.2, t1 - 1.2, 11.8 - drop, 11.85), mat, ref=L.ref(), rim="lrtb", thick=0.5, cell=1.05)
        hem = Loft([(11.8 - drop, wide + 0.08, 3.77, 3.67, 3.2), (11.8 - drop + 0.5, wide, 3.7, 3.6, 3.2)])
        shell_auto(p, bn, hem.surf(t0 + 1.2, t1 - 1.2, 11.8 - drop, 11.8 - drop + 0.5), P["trim"], ref=hem.ref(), rim="lrtb", thick=0.45, cell=0.8)
        sine_rot(a, "idle", bn, 3.0, 0, 3.6 + 0.4 * (k % 3), phase=ph / 2.4)
    c.done(p, a)


# ====================================================================================================================== arms
def pauldron(c):
    P = c.pal
    style = _style(c.spec["pauldron"])
    for side in (0, 1):
        p = c.piece("pauldron_" + ("l" if side else "r"), skin="arm_l" if side else "arm_r")
        D = Loft([(18.5, 2.75, 2.7, 2.7, 4), (19.5, 3.1, 2.95, 2.95, 3.4), (20.8, 3.7, 3.35, 3.35, 3), (22.3, 3.98, 3.55, 3.55, 3), (23.7, 3.78, 3.4, 3.4, 3),
                  (24.9, 3.1, 2.8, 2.8, 2.6), (25.8, 2.1, 1.9, 1.9, 2.4), (26.4, 0.9, 0.8, 0.8, 2.2)], cx=-6.3)
        ys = [20.0, 21.4, 22.6, 23.7, 24.6, 25.3, 25.85, 26.2, 26.4]
        for i in range(len(ys) - 1):                                       # the dome: rings of panels, the same count in every ring so the seams line up
            S = mirror_surf(D.surf(OPEN[0], OPEN[1], ys[i], ys[i + 1]), side)
            p.shell("root", S, 17 if i < 5 else 10, 1, P["main"], ref=mirror_ref(D.ref(), side), rim=("b" if i == 0 else "") + "lr", thick=0.55)
        for k, (ya, yb, a0, a1) in enumerate(((18.9, 20.1, 3.0, 3.25), (17.9, 19.1, 2.78, 3.0))):         # the lames of the upper arm, shingled
            Lm = Loft([(ya, a0 - 0.2, a0 - 0.25, a0 - 0.25, 3.6), (yb, a1, a1 - 0.1, a1 - 0.1, 3.4)], cx=-6.15)
            sshell(p, "root", Lm, side, OPEN[0], OPEN[1], ya, yb, (P["alt"] if k == 0 else P["main"]).with_(lame=1, trim=0, wear=0.2), rim="lrtb", thick=0.5, cell=1.0)
        R = Loft([(19.7, 3.34, 3.12, 3.12, 3.4), (20.4, 3.62, 3.3, 3.3, 3)], cx=-6.3)
        sshell(p, "root", R, side, OPEN[0], OPEN[1], 19.7, 20.4, P["trim"], rim="lrtb", thick=0.5, cell=0.8)
        if style == "spiked":
            for dz, hh in ((-1.7, 3.6), (1.5, 3.0)):
                spike(p, "root", V(-6.4, 25.8, dz, side), V(-0.25, 1.0, 0.0, side), hh, 1.7, P["trim"], segs=4, normal=V(-1, 0, 0, side))
        elif style == "wing":
            for k in range(5):
                blade(p, "root", V(-6.0 - 0.2 * k, 25.0 - k * 0.4, -1.2 + k * 0.8, side), V(-1.0, 0.7 - 0.3 * k, 0.15 * (k - 2), side), V(0, 1, 0.1, side), 4.2 - 0.3 * k, 1.5,
                      P["alt"] if k % 2 else P["main"], segs=3, bend=V(-0.3, -0.9, 0, side), thick=0.4)
        elif style == "tiered":
            for k, (yy, rr) in enumerate(((23.9, 4.45), (22.2, 4.7))):
                Tr = Loft([(yy - 0.9, rr + 0.1, rr - 0.3, rr - 0.3, 3), (yy, rr, rr - 0.4, rr - 0.4, 3)], cx=-6.3)
                sshell(p, "root", Tr, side, OPEN[0], OPEN[1], yy - 0.9, yy, (P["alt"] if k else P["main"]).with_(lame=1, trim=0, wear=0.2), rim="lrtb", thick=0.5, cell=1.0)
        elif style == "fin":
            for k in range(3):
                blade(p, "root", V(-9.6, 22.4 - k * 1.6, 0.0, side), V(-1.0, 0.1 - 0.35 * k, 0.0, side), V(0, 0, -1, side), 3.6 - 0.5 * k, 1.5, P["glass"], segs=3, bend=V(0, 0.6, 0, side), thick=0.35)
        gem(p, "root", V(-6.4, 24.6, -3.3, side), 1.3, P["gem"], facing=V(0, 0.2, -1, side))
        c.done(p)


def vambrace(c):
    P = c.pal
    style = _style(c.spec["vambrace"])
    for side in (0, 1):
        p = c.piece("vambrace_" + ("l" if side else "r"), skin="arm_l" if side else "arm_r")
        cuf = Loft([(13.4, 2.58, 2.55, 2.55, 4), (14.9, 2.45, 2.42, 2.42, 4)], cx=-6.0)
        sshell(p, "root", cuf, side, OPEN[0], OPEN[1], 13.4, 14.9, P["trim"] if style != "cloth" else P["cloth"], rim="lrtb", thick=0.5, cell=1.0)
        mid = Loft([(14.8, 2.46, 2.43, 2.43, 4), (17.2, 2.58, 2.53, 2.53, 4)], cx=-6.0)
        sshell(p, "root", mid, side, OPEN[0], OPEN[1], 14.8, 17.2, P["main"] if style != "cloth" else P["cloth"], rim="lrtb", thick=0.55,
               decals=[Decal("kreuz", 0.5, (0.5, 0.5), mode="glow")] if style == "plain" else [])
        hi = Loft([(17.1, 2.62, 2.57, 2.57, 4), (18.4, 2.72, 2.66, 2.66, 4)], cx=-6.0)
        sshell(p, "root", hi, side, OPEN[0], OPEN[1], 17.1, 18.4, P["alt"] if style != "cloth" else P["cloth"], rim="lrtb", thick=0.5, cell=1.0)
        sshell(p, "root", hi, side, 95, 215, 17.2, 19.6, P["trim"], rim="lrtb", thick=0.5, cell=1.0, dr=lambda th, y: 0.35 + 0.45 * math.exp(-((y - 18.3) / 0.9) ** 2))      # the elbow cop
        if style == "finned":
            for k in range(3):
                blade(p, "root", V(-8.5, 17.8 - k * 1.5, -0.2 + 0.3 * k, side), V(-1.0, -0.05 - 0.25 * k, 0.6 - 0.45 * k, side), V(0, 1, 0, side), 3.0 - 0.4 * k, 1.3,
                      P["alt"] if k % 2 == 0 else P["trim"], segs=3, bend=V(-0.3, -0.5, 0.4, side), thick=0.4)
        elif style == "ring":
            for yy in (15.0, 16.2):
                Rg = Loft([(yy, 2.72, 2.68, 2.68, 4), (yy + 0.45, 2.72, 2.68, 2.68, 4)], cx=-6.0)
                sshell(p, "root", Rg, side, OPEN[0], OPEN[1], yy, yy + 0.45, P["trim"], rim="lrtb", thick=0.45, cell=0.8)
        c.done(p)


def gauntlet(c):
    P = c.pal
    style = _style(c.spec["gauntlet"])
    for side in (0, 1):
        p = c.piece("gauntlet_" + ("l" if side else "r"), skin="arm_l" if side else "arm_r")
        G = Loft([(10.6, 2.35, 2.3, 2.3, 4), (12.0, 2.55, 2.5, 2.5, 4), (13.6, 2.6, 2.55, 2.55, 4)], cx=-6.0)
        sshell(p, "root", G, side, OPEN[0], OPEN[1], 10.9, 13.4, P["alt"] if style != "none" else P["cloth"], rim="lrtb", thick=0.55,
               decals=[Decal("star4", 0.45, (0.5, 0.45), mode="glow")] if style == "plain" else [])
        cuff = Loft([(12.9, 2.78, 2.7, 2.7, 4), (14.2, 2.66, 2.6, 2.6, 4)], cx=-6.0)
        sshell(p, "root", cuff, side, OPEN[0], OPEN[1], 12.9, 14.2, P["trim"], rim="lrtb", thick=0.5, cell=1.0)
        for k in range(4):                                                   # four fingers, two segments each
            fx = -7.5 + k * 1.0
            for sg, (y0, y1) in enumerate(((11.0, 9.7), (9.8, 8.6))):
                p.between("root", V(fx, y0, -2.55 - 0.1 * sg, side), V(fx, y1, -2.65 - 0.1 * sg, side), 0.9, 0.7, P["main"] if sg == 0 else P["trim"], normal=V(0, 0, -1, side))
            if style == "claw":
                spike(p, "root", V(fx, 8.7, -2.8, side), V(0.0, -1.0, -0.35, side), 1.9, 0.8, P["trim"], segs=3, normal=V(0, 0, -1, side))
        p.between("root", V(-4.4, 11.8, -1.5, side), V(-4.2, 9.8, -2.1, side), 0.9, 0.8, P["main"], normal=V(1, 0, 0, side))
        c.done(p)


# ====================================================================================================================== legs
def cuisse(c):
    P = c.pal
    style = _style(c.spec.get("cuisse", "plain"))
    for side in (0, 1):
        p = c.piece("cuisse_" + ("l" if side else "r"), skin="leg_l" if side else "leg_r")
        T = Loft([(6.6, 2.42, 2.42, 2.42, 5), (9.0, 2.48, 2.48, 2.48, 5), (11.8, 2.55, 2.52, 2.52, 5)], cx=-1.95)
        sshell(p, "root", T, side, OPEN[0], OPEN[1], 7.5, 11.8, P["main"], rim="lrtb", thick=0.55, decals=[Decal("diamond", 0.4, (0.5, 0.5), mode="glow")] if style == "plain" else [])
        low = Loft([(6.4, 2.5, 2.5, 2.5, 5), (7.7, 2.45, 2.45, 2.45, 5)], cx=-1.95)
        sshell(p, "root", low, side, OPEN[0], OPEN[1], 6.4, 7.7, P["alt"].with_(lame=1, trim=0, wear=0.2), rim="lrtb", thick=0.5, cell=1.0)
        sshell(p, "root", low, side, -70, 70, 5.4, 7.6, P["trim"], rim="lrtb", thick=0.5, cell=0.95, dr=lambda th, y: 0.25 + 0.7 * math.exp(-((y - 6.5) / 0.9) ** 2) * max(0.0, math.cos(th)))
        gem(p, "root", V(-1.95, 6.5, -3.45, side), 1.1, P["gem"])
        if style == "spiked":
            spike(p, "root", V(-1.95, 6.6, -3.3, side), V(0, 0.15, -1.0, side), 2.6, 1.7, P["trim"], segs=4, normal=V(0, 1, 0, side))
        c.done(p)


def greave(c):
    P = c.pal
    style = _style(c.spec["greave"])
    for side in (0, 1):
        p = c.piece("greave_" + ("l" if side else "r"), skin="leg_l" if side else "leg_r")
        G = Loft([(2.0, 2.3, 2.3, 2.3, 5), (4.0, 2.42, 2.42, 2.42, 5), (6.2, 2.5, 2.5, 2.5, 5)], cx=-1.95)
        sshell(p, "root", G, side, OPEN[0], OPEN[1], 2.2, 6.0, P["main"], rim="lrtb", thick=0.55,
               extra=lambda th, y: (0, 0, -0.3 * max(0.0, math.cos(th)) ** 3) if abs(math.degrees(th)) < 90 else (0, 0, 0))
        band = Loft([(2.0, 2.4, 2.4, 2.4, 5), (3.0, 2.34, 2.34, 2.34, 5)], cx=-1.95)
        sshell(p, "root", band, side, OPEN[0], OPEN[1], 2.0, 3.0, P["trim"], rim="lrtb", thick=0.5, cell=0.9)
        if style == "winged":
            for k in range(3):
                blade(p, "root", V(-4.35, 3.0 + 0.8 * k, 0.5 - 0.5 * k, side), V(-1.0, 0.25 + 0.2 * k, 0.5 - 0.3 * k, side), V(0, 1, 0, side), 2.6 - 0.3 * k, 1.1, P["alt"], segs=3,
                      bend=V(-0.2, 0.5, 0.5, side), thick=0.35)
        elif style == "spiked":
            spike(p, "root", V(-1.95, 5.0, -2.9, side), V(0, -0.1, -1.0, side), 2.2, 1.2, P["trim"], segs=3, normal=V(0, 1, 0, side))
        c.done(p)


def sabaton(c):
    P = c.pal
    style = _style(c.spec.get("sabaton", "plain"))
    for side in (0, 1):
        p = c.piece("sabaton_" + ("l" if side else "r"), skin="leg_l" if side else "leg_r")
        A = Loft([(0.0, 2.55, 2.5, 2.55, 4), (1.2, 2.5, 2.48, 2.52, 4), (2.4, 2.32, 2.34, 2.36, 4)], cx=-1.95)
        sshell(p, "root", A, side, OPEN[0], OPEN[1], 0.2, 2.4, P["alt"], rim="lrtb", thick=0.55)

        def S(u, v):                                                          # the toe: a half dome lying forward
            ph = math.radians(-90 + 180 * u)
            z = -2.0 - 3.3 * v
            w = 2.4 * math.sqrt(max(0.0, 1.0 - v ** 2.2)) + 0.05
            h = 1.9 - 1.0 * v
            return V3((mx(-1.95 + w * math.sin(ph), side), h * max(0.0, math.cos(ph)) ** 0.9, z))
        shell_auto(p, "root", S, P["main"], ref=V3((mx(-1.95, side), -2.0, -2.0)), rim="lrtb", thick=0.5, cell=1.0)
        sshell(p, "root", A, side, 95, 215, 0.0, 1.6, P["trim"], rim="lrtb", thick=0.5, cell=1.0, dr=lambda th, y: 0.3)                    # the heel
        p.cbox("root", V(-1.95, 0.12, -1.6, side), (4.9, 0.5, 8.0), P["leather"], keep=("down", "west", "east", "north", "south"))        # the sole
        if style == "pointed":
            spike(p, "root", V(-1.95, 0.9, -5.0, side), V(0, 0.05, -1.0, side), 1.8, 1.4, P["trim"], segs=3, normal=V(0, 1, 0, side))
        c.done(p)
