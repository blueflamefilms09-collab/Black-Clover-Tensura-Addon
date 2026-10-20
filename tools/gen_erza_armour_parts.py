"""The standard pieces of the Erza armours (1.99.0): helm, gorget, chest, backplate, belt, skirt, pauldrons, vambraces, gauntlets, cuisses, greaves, sabatons.

Each builder takes the armour's `Ctx` (palette + spec) and returns finished pieces (already saved). The style of a piece is a name in the spec (`c.spec["helm"]`, ...), so one builder gives every armour its own
silhouette: a circlet, a crown of spikes, a winged helm, a horned one, a crested one, a visored one, a ribbon; a bust plate, a breastplate, a heavy plate, a cloth wrap; and so on.

Coordinates are the body frame of the files (feet y = 0, shoulders y = 24, the right arm at x = -5, the front on -z); the right-hand pieces are authored at x < 0 and mirrored with `mirror_surf` / `mv`.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_erza_armour_core import *  # noqa: E402,F401,F403


def _g(y, y0, w):
    """A gaussian bump of width w about y0."""
    return math.exp(-((y - y0) / w) ** 2)


# ====================================================================================================================== head
def helm(c):
    P, s = c.pal, c.spec["helm"]
    style = s if isinstance(s, str) else s["style"]
    p = c.piece("helm", skin="head")
    cap = Loft([(29.55, 4.62, 4.62, 4.62, 6), (32.1, 4.62, 4.62, 4.62, 6), (32.5, 4.4, 4.4, 4.4, 5), (33.0, 3.8, 3.8, 3.8, 4), (33.5, 2.8, 2.8, 2.8, 3.2), (33.85, 1.6, 1.6, 1.6, 2.6),
                (34.0, 0.7, 0.7, 0.7, 2.2)])
    ref = cap.ref()
    cap_mat = P["cloth"] if style == "ribbon" else P["main"]
    ys = [29.55, 30.7, 31.4, 32.1, 32.5, 32.8, 33.1, 33.45, 33.75, 34.0]
    for i in range(len(ys) - 1):                                      # one ring a row, the same panel count in every row so the seams line up (the thin top rows get thin panels)
        S = cap.surf(-180, 180, ys[i], ys[i + 1])
        p.shell("root", S, 30, 1, cap_mat, ref=ref, rim=("b" if i == 0 else ""), thick=0.55)
    flat_patch(p, "root", (0, 0), 2.6, 2.6, 34.0, cap_mat, 2, 2)
    # the brow band: a gold ring over the eyes, the face stays open below it
    band = Loft([(28.95, 4.78, 4.78, 4.78, 6), (29.75, 4.78, 4.78, 4.78, 6)])
    shell_auto(p, "root", band.surf(-180, 180, 28.95, 29.75), P["trim"], ref=band.ref(), rim="tb", thick=0.6, cell=0.9)
    # the sides and the back: ear guards, a neck guard in two lames
    side = Loft([(24.55, 4.62, 4.62, 4.62, 6), (26.5, 4.66, 4.66, 4.66, 6), (28.95, 4.62, 4.62, 4.62, 6)])
    shell_auto(p, "root", side.surf(58, 302, 26.4, 28.95), P["alt"], ref=side.ref(), rim="lrtb", thick=0.55)
    shell_auto(p, "root", side.surf(58, 302, 24.55, 26.5), P["main"], ref=side.ref(), rim="lrtb", thick=0.55)
    for sx in (0, 1):
        # cheek guard: a curved plate in front of the ear, the face stays free
        S = mirror_surf(side.surf(-98, -60, 24.6, 28.9), sx)
        shell_auto(p, "root", S, P["alt"], ref=mirror_ref(side.ref(), sx), rim="lrtb", thick=0.5, cell=1.0)
    if style == "crown":
        for k in range(7):
            th = -52 + k * (104 / 6.0)
            h = 3.1 - 0.7 * abs(k - 3) / 3.0 * 1.4
            base = cap.point(th, 29.9, 0.15)
            out = unit(V3((math.sin(math.radians(th)), 0.0, -math.cos(math.radians(th)))))
            spike(p, "root", base, V3((0, 1, 0)) + out * 0.18, h, 1.2, P["trim"], segs=4, normal=out)
    elif style == "circlet":
        gem(p, "root", V3((0, 30.7, -4.95)), 1.5, P["gem"])
        for sx in (-1, 1):
            spike(p, "root", V3((sx * 3.0, 29.9, -4.75)), V3((sx * 0.3, 1, -0.1)), 2.2, 0.9, P["trim"], segs=3)
    elif style == "winged":
        for sx in (0, 1):
            for k in range(4):
                base = V3((mx(-4.75, sx), 29.0 + 0.2 * k, -0.6 + 0.0 * k))
                dirv = V3((mx(-1.0, sx), 0.55 + 0.35 * k, 0.55 - 0.5 * k))
                blade(p, "root", base, dirv, V3((mx(-1, sx) * 0.1, 0.0, -1.0)), 3.4 - 0.25 * k, 1.4 - 0.1 * k, P["alt"] if k % 2 else P["main"], segs=3, bend=V3((0, 0.4, 0.5)))
        gem(p, "root", V3((0, 30.7, -4.95)), 1.3, P["gem"])
    elif style == "horned":
        for sx in (0, 1):
            spike(p, "root", V3((mx(-4.3, sx), 31.0, -0.8)), V3((mx(-0.8, sx), 0.9, 0.1)), 6.0, 2.0, P["trim"], segs=6, bend=V3((mx(0.9, sx), 0.9, -0.2)), normal=V3((0, 0, -1)))
            spike(p, "root", V3((mx(-4.6, sx), 30.2, 0.6)), V3((mx(-1.0, sx), 0.1, 0.5)), 3.0, 1.3, P["alt"], segs=4, bend=V3((0, 0.7, 0.3)))
        gem(p, "root", V3((0, 30.8, -4.95)), 1.3, P["gem"])
    elif style == "crest":
        for k in range(8):
            z = -3.8 + k * 1.1
            h = 2.6 - 0.9 * abs(k - 2.5) / 5.0 + 0.5
            p.between("root", V3((0, 33.0, z)), V3((0, 33.0 + h, z + 0.35)), 0.9, 0.6 + 0.0 * k, P["trim"] if k % 2 else P["alt"], normal=V3((1, 0, 0)))
        gem(p, "root", V3((0, 30.7, -4.95)), 1.3, P["gem"])
    elif style == "visor":
        S = cap.surf(-52, 52, 28.2, 30.0, extra=lambda th, y: (0.0, 0.0, -0.45 * math.cos(th)))
        shell_auto(p, "root", S, P["alt"], ref=ref, rim="lrtb", thick=0.5)
        p.between("root", V3((0, 33.0, -3.8)), V3((0, 34.2, 3.6)), 1.0, 0.7, P["trim"], normal=V3((1, 0, 0)))
    elif style == "ribbon":
        for sx in (0, 1):
            p.between("root", V3((mx(-1.0, sx), 31.0, 4.8)), V3((mx(-3.2, sx), 25.2, 5.1)), 1.5, 0.35, P["alt"], normal=V3((0, 0, 1)))
    return c.done(p)


# ====================================================================================================================== torso
BODY = Loft([(12.8, 4.52, 2.45, 2.4, 4), (16.0, 4.6, 2.55, 2.5, 4), (19.5, 4.72, 2.62, 2.55, 4), (22.9, 4.55, 2.5, 2.45, 4)])


def _chest_extra(bump, y0, wy):
    def f(th, y):
        k = max(0.0, math.cos(th)) ** 2 * math.exp(-((y - y0) / wy) ** 2)
        return (0.0, 0.0, -bump * k) if abs(math.degrees(th)) <= 90 else (0.0, 0.0, 0.0)
    return f


def chest(c):
    P, spec = c.pal, c.spec["chest"]
    style, emblem = spec["style"], spec.get("emblem")
    p = c.piece("chest", skin="body")
    bump = {"bust": 0.9, "breast": 0.45, "heavy": 0.7, "cloth": 0.15}[style]
    ex = _chest_extra(bump, 19.6, 1.9)
    emb = []
    if emblem:
        emb.append(Decal(emblem, 0.46, (0.5, 0.52), mode="glow", rot=spec.get("emblem_rot", 0.0)))
        emb.append(Decal("ring", 0.62, (0.5, 0.52), mode="inlay", stroke=0.05))
    main = P["cloth"] if style == "cloth" else P["main"]
    shell_auto(p, "root", BODY.surf(-103, 103, 17.3, 22.95, extra=ex), main, ref=BODY.ref(), rim="lrtb", decals=emb, thick=0.6)
    # the belly lames: shingled, each one flares a little at its lower edge
    ys = [17.4, 16.1, 14.8, 13.5, 12.6]
    for k in range(4):
        ya, yb = ys[k + 1] - 0.15, ys[k]
        L = Loft([(ya, 4.55 + 0.18, 2.5 + 0.12, 2.45, 4), (yb, 4.55, 2.5, 2.45, 4)], cx=0.0)
        mat = (P["alt"] if k % 2 == 0 else P["main"]).with_(lame=1, trim=0, wear=0.2)
        shell_auto(p, "root", L.surf(-101, 101, ya, yb), mat, ref=L.ref(), rim="lrtb", thick=0.5, cell=1.0)
    # the gold top rim and the central ridge
    top = Loft([(22.55, 4.6, 2.62, 2.6, 4), (23.25, 4.58, 2.6, 2.58, 4)])
    shell_auto(p, "root", top.surf(-103, 103, 22.55, 23.25), P["trim"], ref=top.ref(), rim="lrtb", thick=0.55, cell=0.9)
    if style in ("breast", "heavy", "bust"):
        for sy in (-1, 1):
            pass
    if style == "heavy":
        for sx in (-1, 1):
            for yy in (18.0, 19.8, 21.6):
                pt = BODY.point(sx * 88, yy, 0.1)
                rivet(p, "root", pt, 0.9, P["trim"], facing=V3((sx, 0, 0)))
    return c.done(p)


def backplate(c):
    P, spec = c.pal, c.spec["chest"]
    style = spec["style"]
    p = c.piece("backplate", skin="body")
    main = P["cloth"] if style == "cloth" else P["main"]
    sig = Decal(spec.get("back_emblem", "wing"), 0.5, (0.5, 0.5), mode="glow")
    ex = lambda th, y: (0.0, 0.0, 0.5 * math.exp(-((y - 20.0) / 2.2) ** 2) * max(0.0, -math.cos(th)) ** 2) if abs(math.degrees(th)) > 90 else (0, 0, 0)
    shell_auto(p, "root", BODY.surf(103, 257, 17.3, 22.95, extra=ex), main, ref=BODY.ref(), rim="lrtb", decals=[sig], thick=0.6)
    ys = [17.4, 16.1, 14.8, 13.5, 12.6]
    for k in range(4):
        ya, yb = ys[k + 1] - 0.15, ys[k]
        L = Loft([(ya, 4.55 + 0.18, 2.5, 2.45 + 0.14, 4), (yb, 4.55, 2.5, 2.45, 4)])
        mat = (P["alt"] if k % 2 == 0 else P["main"]).with_(lame=1, trim=0, wear=0.2)
        shell_auto(p, "root", L.surf(103, 257, ya, yb), mat, ref=L.ref(), rim="lrtb", thick=0.5, cell=1.0)
    top = Loft([(22.55, 4.6, 2.62, 2.6, 4), (23.25, 4.58, 2.6, 2.58, 4)])
    shell_auto(p, "root", top.surf(103, 257, 22.55, 23.25), P["trim"], ref=top.ref(), rim="lrtb", thick=0.55, cell=0.9)
    # the spine: a ridge of small plates down the middle with a glowing vent
    for k in range(8):
        y = 22.0 - k * 1.2
        p.cbox("root", V3((0, y, 2.95)), (0.9, 0.95, 0.7), P["trim"] if k % 2 == 0 else P["hot"])
    return c.done(p)


def gorget(c):
    P = c.pal
    p = c.piece("gorget", skin="body")
    for k, (ya, yb, a, mat) in enumerate(((22.35, 23.25, 4.78, "main"), (23.1, 24.0, 4.72, "alt"), (23.85, 24.8, 4.66, "trim"))):
        L = Loft([(ya, a, 2.78, 2.72, 4), (yb, a - 0.06, 2.74, 2.68, 4)])
        shell_auto(p, "root", L.surf(-180, 180, ya, yb), P[mat], ref=L.ref(), rim="tb", thick=0.5, cell=0.95)
    gem(p, "root", V3((0, 23.2, -3.35)), 1.3, P["gem"])
    return c.done(p)


def belt(c):
    P = c.pal
    p = c.piece("belt", skin="body")
    L = Loft([(11.55, 4.8, 2.72, 2.68, 4), (13.45, 4.8, 2.72, 2.68, 4)])
    shell_auto(p, "root", L.surf(-180, 180, 11.55, 13.45), P["leather"], ref=L.ref(), rim="tb", thick=0.6, cell=1.0)
    for y in (11.45, 13.4):
        R = Loft([(y, 4.88, 2.82, 2.78, 4), (y + 0.3, 4.88, 2.82, 2.78, 4)])
        shell_auto(p, "root", R.surf(-180, 180, y, y + 0.3), P["trim"], ref=R.ref(), rim="tb", thick=0.5, cell=0.8)
    p.cbox("root", V3((0, 12.5, -3.2)), (3.4, 2.4, 0.9), P["trim"])
    gem(p, "root", V3((0, 12.5, -3.8)), 1.7, P["gem"])
    for sx in (-1, 1):
        for k in range(3):
            rivet(p, "root", V3((sx * (2.6 + k * 0.0), 12.5 + (k - 1) * 0.7, -3.0)), 0.5, P["trim"])
    return c.done(p)
