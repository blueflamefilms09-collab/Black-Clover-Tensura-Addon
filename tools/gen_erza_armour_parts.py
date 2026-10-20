"""The head and torso pieces of the Erza armours (1.99.0): helm, gorget, chest, backplate, belt. The skirt, the arms and the legs are in gen_erza_armour_limbs.py, the signature pieces in
gen_erza_armour_extras.py.

Each builder takes the armour's `Ctx` (palette + spec) and saves its piece. The style of a piece is a name in the spec (`c.spec["helm"]`, ...), so one builder gives every armour its own silhouette: a
circlet, a crown of spikes, a winged helm, a horned one, a crested one, a visored one, a ribbon; a bust plate, a breastplate, a heavy plate, a cloth wrap.

Coordinates are the body frame of the files (feet y = 0, shoulders y = 24, the right arm at x = -5, the front on -z).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_erza_armour_core import *  # noqa: E402,F401,F403


def _style(spec):
    return spec if isinstance(spec, str) else spec["style"]


def top_ridge(p, cap, mat, height, width, y0=31.0, y1=34.0, cell=1.2, thick=0.6, bone="root"):
    """A fin over the top of a dome, from the brow to the nape: its height above the dome follows a half sine."""
    def S(u, v):
        t = v
        th = 0.0 if t < 0.5 else 180.0
        s = (t * 2.0) if t < 0.5 else (2.0 - t * 2.0)               # 0 at the brow / nape, 1 on the top
        y = y0 + (y1 - y0) * s
        P = cap.point(th, y, height * math.sin(math.pi * t) ** 0.9)
        return V3((P[0] + (u - 0.5) * width * (0.35 + 0.65 * math.sin(math.pi * t)), P[1], P[2]))
    nv = max(6, int(2 * (y1 - y0 + 4.0) / cell))
    return p.shell(bone, S, 1, nv, mat, thick=thick, rim="", ref=V3((0, 28.0, 0)), double=True)


# ====================================================================================================================== head
def helm(c):
    P = c.pal
    style = _style(c.spec["helm"])
    p = c.piece("helm", skin="head")
    cap = Loft([(29.55, 4.62, 4.62, 4.62, 6), (32.1, 4.62, 4.62, 4.62, 6), (32.7, 4.45, 4.45, 4.45, 4.8), (33.4, 4.0, 4.0, 4.0, 3.8), (34.1, 3.2, 3.2, 3.2, 3.0), (34.7, 2.2, 2.2, 2.2, 2.6),
                (35.1, 1.2, 1.2, 1.2, 2.3), (35.3, 0.5, 0.5, 0.5, 2.1)])
    ref = cap.ref()
    cap_mat = P["cloth"] if style == "ribbon" else P["main"]
    ys = [29.55, 30.7, 31.4, 32.1, 32.7, 33.4, 34.1, 34.7, 35.1, 35.3]
    dec = [Decal(c.spec.get("helm_emblem", "star4"), 0.34, (0.5, 0.14), mode="glow")] if style in ("circlet", "crown", "winged", "visor", "crest") else []
    p.shell("root", cap.surf_rows(-180, 180, ys), 26, len(ys) - 1, cap_mat, ref=ref, rim="b", thick=0.55, decals=dec)        # the dome: one painted sheet over rings of panels
    flat_patch(p, "root", (0, 0), 1.6, 1.6, 35.3, cap_mat, 2, 2)
    # the brow band: a gold ring over the eyes, the face stays open below it
    band = Loft([(28.95, 4.78, 4.78, 4.78, 6), (29.75, 4.78, 4.78, 4.78, 6)])
    shell_auto(p, "root", band.surf(-180, 180, 28.95, 29.75), P["trim"], ref=band.ref(), rim="tb", thick=0.6, cell=0.9)
    # the sides and the back: ear guards, and a neck guard in two lames that flares outward toward its lower edge
    side = Loft([(24.45, 4.62, 4.62, 4.62, 6), (26.5, 4.66, 4.66, 4.66, 6), (28.95, 4.62, 4.62, 4.62, 6)])
    flare = lambda th, y: 0.7 * max(0.0, (26.6 - y) / 2.2) ** 1.4
    shell_auto(p, "root", side.surf(58, 302, 26.4, 28.95), P["alt"], ref=side.ref(), rim="lrtb", thick=0.55)
    shell_auto(p, "root", side.surf(58, 302, 24.45, 26.5, dr=flare), P["main"].with_(lame=1, trim=0, wear=0.2), ref=side.ref(), rim="lrtb", thick=0.55)
    for sx in (0, 1):
        S = mirror_surf(side.surf(-98, -60, 24.6, 28.9), sx)                                     # the cheek guard: a curved plate in front of the ear, the face stays free
        shell_auto(p, "root", S, P["alt"], ref=mirror_ref(side.ref(), sx), rim="lrtb", thick=0.5, cell=1.2)
    if style not in ("crest", "crown", "horned"):
        top_ridge(p, cap, P["trim"], 0.5, 1.2)
    if style == "crown":
        for k in range(7):
            th = -52 + k * (104 / 6.0)
            h = 3.2 - 0.9 * abs(k - 3) / 3.0
            base = cap.point(th, 29.9, 0.15)
            out = unit(V3((math.sin(math.radians(th)), 0.0, -math.cos(math.radians(th)))))
            spike(p, "root", base, V3((0, 1, 0)) + out * 0.18, h, 1.2, P["trim"], segs=4, normal=out)
            if k % 2 == 0:
                gem(p, "root", base + V3((0, h + 0.3, 0)) + out * 0.2, 0.7, P["gem"])
        top_ridge(p, cap, P["trim"], 1.4, 1.0)
    elif style == "circlet":
        gem(p, "root", V3((0, 30.7, -4.95)), 1.6, P["gem"])
        for sx in (-1, 1):
            spike(p, "root", V3((sx * 3.0, 29.9, -4.75)), V3((sx * 0.3, 1, -0.1)), 2.2, 0.9, P["trim"], segs=3)
    elif style == "winged":
        for sx in (0, 1):
            for k in range(6):                                               # a fan of feather plates sweeping up and back from the temple
                base = V3((mx(-4.8, sx), 29.5 + 0.1 * k, -1.2 + 0.55 * k))
                dirv = V3((mx(-0.75, sx), 0.35 + 0.34 * k, 0.55 + 0.1 * k))
                blade(p, "root", base, dirv, V3((mx(-1, sx) * 0.2, 0.0, -1.0)), 6.0 - 0.55 * k, 2.2 - 0.12 * k, P["alt"] if k % 2 else P["main"], segs=4, bend=V3((mx(-0.3, sx), 0.7, 0.5)), thick=0.4)
        gem(p, "root", V3((0, 30.7, -4.95)), 1.5, P["gem"])
    elif style == "horned":
        for sx in (0, 1):
            spike(p, "root", V3((mx(-4.3, sx), 31.0, -0.8)), V3((mx(-0.8, sx), 0.9, 0.1)), 6.4, 2.2, P["trim"], segs=7, bend=V3((mx(0.9, sx), 0.9, -0.2)), normal=V3((0, 0, -1)))
            spike(p, "root", V3((mx(-4.6, sx), 30.2, 0.6)), V3((mx(-1.0, sx), 0.1, 0.5)), 3.2, 1.4, P["alt"], segs=4, bend=V3((0, 0.7, 0.3)))
        gem(p, "root", V3((0, 30.8, -4.95)), 1.5, P["gem"])
        top_ridge(p, cap, P["alt"], 1.0, 1.0)
    elif style == "crest":
        top_ridge(p, cap, P["hot"], 3.4, 1.6)
        top_ridge(p, cap, P["trim"], 1.2, 0.9)
        gem(p, "root", V3((0, 30.7, -4.95)), 1.5, P["gem"])
        for sx in (0, 1):
            blade(p, "root", V3((mx(-4.7, sx), 30.5, 0.0)), V3((mx(-0.5, sx), 0.9, 0.3)), V3((0, 0, -1)), 4.2, 1.6, P["trim"], segs=3, bend=V3((mx(-0.4, sx), 0.6, 0.6)), thick=0.45)
    elif style == "visor":                                                  # a raised brow plate with a peak that juts forward over the eyes, and a nose ridge
        peak = lambda th, y: 1.5 * max(0.0, (31.2 - y) / 1.4) ** 1.2 * max(0.0, math.cos(th * 1.3))
        S = cap.surf(-62, 62, 29.8, 32.3, dr=peak)
        shell_auto(p, "root", S, P["alt"], ref=ref, rim="lrtb", thick=0.5, cell=1.0, decals=[Decal("line", 0.9, (0.5, 0.4), mode="glow", stroke=0.05), Decal("line", 0.9, (0.5, 0.75), mode="glow", stroke=0.05)])
        gem(p, "root", V3((0, 31.0, -5.7)), 1.3, P["gem"])
        top_ridge(p, cap, P["trim"], 1.0, 1.0)
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


def _lames(p, c, th0, th1, ys, flare_front, cell=1.25):
    P = c.pal
    for k in range(len(ys) - 1):
        ya, yb = ys[k + 1] - 0.15, ys[k]
        L = Loft([(ya, 4.55 + 0.18, 2.5 + flare_front, 2.45 + 0.1, 4), (yb, 4.55, 2.5, 2.45, 4)], cx=0.0)
        mat = P["main"].with_(lame=1, trim=0, wear=0.2, seed=P["main"].seed + 7 * k)
        shell_auto(p, "root", L.surf(th0, th1, ya, yb), mat, ref=L.ref(), rim="lrtb", thick=0.5, cell=cell)


def chest(c):
    P, spec = c.pal, c.spec["chest"]
    style, emblem = spec["style"], spec.get("emblem")
    p = c.piece("chest", skin="body")
    bump = {"bust": 0.9, "breast": 0.5, "heavy": 0.75, "cloth": 0.15}[style]
    ex = _chest_extra(bump, 19.6, 1.9)
    emb = []
    if emblem:
        emb.append(Decal(emblem, 0.46, (0.5, 0.52), mode="glow", rot=spec.get("emblem_rot", 0.0)))
        emb.append(Decal("ring", 0.62, (0.5, 0.52), mode="inlay", stroke=0.05))
    main = P["cloth"] if style == "cloth" else P["main"]
    shell_auto(p, "root", BODY.surf(-103, 103, 17.3, 22.95, extra=ex), main, ref=BODY.ref(), rim="lrtb", decals=emb, thick=0.6, cell=1.25)
    _lames(p, c, -101, 101, [17.4, 16.1, 14.8, 13.5, 12.6], 0.12)
    top = Loft([(22.55, 4.6, 2.62, 2.6, 4), (23.25, 4.58, 2.6, 2.58, 4)])
    shell_auto(p, "root", top.surf(-103, 103, 22.55, 23.25), P["trim"], ref=top.ref(), rim="lrtb", thick=0.55, cell=0.9)
    if style in ("breast", "heavy"):                                   # the keel: a raised ridge down the middle of the breastplate
        shell_auto(p, "root", BODY.surf(-5, 5, 17.4, 22.5, extra=ex, dr=0.32), P["trim"], ref=BODY.ref(), rim="tb", thick=0.5, cell=0.9)
    if style == "heavy":                                               # a second, raised plate over the pectoral with rivets, and rivets down the sides
        shell_auto(p, "root", BODY.surf(-58, 58, 18.3, 22.3, extra=ex, dr=0.5), P["alt"], ref=BODY.ref(), rim="lrtb", thick=0.6, cell=1.2,
                   decals=[Decal("shield", 0.6, (0.5, 0.5), mode="glow", stroke=0.06)])
        for sx in (-1, 1):
            for yy in (18.0, 19.8, 21.6):
                rivet(p, "root", BODY.point(sx * 88, yy, 0.1), 0.9, P["trim"], facing=V3((sx, 0, 0)))
    if style == "bust":                                                # a gold under-bust band
        shell_auto(p, "root", BODY.surf(-92, 92, 18.0, 18.6, extra=ex, dr=0.12), P["trim"], ref=BODY.ref(), rim="lrtb", thick=0.5, cell=0.85)
    return c.done(p)


def backplate(c):
    P, spec = c.pal, c.spec["chest"]
    style = spec["style"]
    p = c.piece("backplate", skin="body")
    main = P["cloth"] if style == "cloth" else P["main"]
    sig = [Decal(spec.get("back_emblem", "wing"), 0.5, (0.5, 0.5), mode="glow"), Decal("ring", 0.66, (0.5, 0.5), mode="inlay", stroke=0.05)]
    ex = lambda th, y: (0.0, 0.0, 0.5 * math.exp(-((y - 20.0) / 2.2) ** 2) * max(0.0, -math.cos(th)) ** 2) if abs(math.degrees(th)) > 90 else (0, 0, 0)
    shell_auto(p, "root", BODY.surf(103, 257, 17.3, 22.95, extra=ex), main, ref=BODY.ref(), rim="lrtb", decals=sig, thick=0.6, cell=1.25)
    _lames(p, c, 103, 257, [17.4, 16.1, 14.8, 13.5, 12.6], 0.0)
    top = Loft([(22.55, 4.6, 2.62, 2.6, 4), (23.25, 4.58, 2.6, 2.58, 4)])
    shell_auto(p, "root", top.surf(103, 257, 22.55, 23.25), P["trim"], ref=top.ref(), rim="lrtb", thick=0.55, cell=0.9)
    for sx in (0, 1):                                                  # shoulder-blade plates
        shell_auto(p, "root", mirror_surf(BODY.surf(135, 165, 18.4, 22.2, extra=ex, dr=0.4), sx), P["alt"], ref=mirror_ref(BODY.ref(), sx), rim="lrtb", thick=0.5, cell=1.2)
    for k in range(8):                                                 # the spine: a ridge of small plates down the middle with a glowing vent
        y = 22.0 - k * 1.2
        p.cbox("root", V3((0, y, 3.0)), (0.9, 0.95, 0.7), P["trim"] if k % 2 == 0 else P["hot"])
    return c.done(p)


def gorget(c):
    P = c.pal
    p = c.piece("gorget", skin="body")
    for k, (ya, yb, a, mat) in enumerate(((22.35, 23.25, 4.78, "main"), (23.1, 24.0, 4.72, "alt"), (23.85, 24.8, 4.66, "trim"))):
        L = Loft([(ya, a, 2.78, 2.72, 4), (yb, a - 0.06, 2.74, 2.68, 4)])
        shell_auto(p, "root", L.surf(-180, 180, ya, yb), P[mat], ref=L.ref(), rim="tb", thick=0.5, cell=0.95)
    gem(p, "root", V3((0, 23.2, -3.35)), 1.3, P["gem"])
    for sx in (0, 1):                                                  # shoulder flares: two small wing plates at the sides of the neck
        for k in range(3):
            blade(p, "root", V3((mx(-4.7, sx), 23.6 + 0.3 * k, -0.6 + 0.9 * k)), V3((mx(-1.0, sx), 0.35, 0.0)), V3((0, 1, 0)), 2.4 - 0.2 * k, 1.1, P["alt"] if k % 2 else P["trim"], segs=2,
                  bend=V3((0, -0.5, 0)), thick=0.4)
    return c.done(p)


def belt(c):
    P = c.pal
    p = c.piece("belt", skin="body")
    L = Loft([(11.55, 4.8, 2.72, 2.68, 4), (13.45, 4.8, 2.72, 2.68, 4)])
    shell_auto(p, "root", L.surf(-180, 180, 11.55, 13.45), P["leather"], ref=L.ref(), rim="tb", thick=0.6, cell=1.2)
    for y in (11.45, 13.4):
        R = Loft([(y, 4.88, 2.82, 2.78, 4), (y + 0.3, 4.88, 2.82, 2.78, 4)])
        shell_auto(p, "root", R.surf(-180, 180, y, y + 0.3), P["trim"], ref=R.ref(), rim="tb", thick=0.5, cell=0.8)
    p.cbox("root", V3((0, 12.5, -3.2)), (3.4, 2.4, 0.9), P["trim"])
    gem(p, "root", V3((0, 12.5, -3.8)), 1.7, P["gem"])
    for sx in (-1, 1):
        for k in range(3):
            rivet(p, "root", V3((sx * 2.6, 12.5 + (k - 1) * 0.7, -3.0)), 0.5, P["trim"])
        for k in range(4):                                             # studs round the belt
            ang = math.radians(sx * (45 + k * 38))
            rivet(p, "root", V3((4.9 * math.sin(ang), 12.5, -2.8 * math.cos(ang))), 0.5, P["trim"], facing=V3((math.sin(ang), 0, -math.cos(ang))))
    return c.done(p)
