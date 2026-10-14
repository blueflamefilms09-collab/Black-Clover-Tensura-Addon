"""Wave 11 models, blades 6 to 10: Flamecutter, Heart of the Flame, Bianca, Sorrow's Edge, The Dragons Truth.

Every function returns (mesh, atlas, kwargs for wave1_models.finish). Axis: y along the weapon (the grip / butt at the bottom), x across, z through
(+z toward the viewer in hand). The measured card pixels are in the constants of each function.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave11_models import spike, torus, wrap_rings, curve, glow_part, tint_part

CARD3 = "p3/Relics of the world part 3/"
CARD2 = "p2/Relics of the world part 2/"


# ====================================================================================== 6 Flamecutter
def flamecutter():
    # ---- 1 measure: card 736 x 1019, the sword down x 560; the pommel's crescent spike (175) to the point (880): 705 px for a 125 cm sword.
    K = 125.0 / 705.0
    Y = lambda cy: (880 - cy) * K                            # card y -> model y (point at 0)
    BL_TOP, W_BL = Y(362), 46 * K                            # the blade leaves the guard at card y 362; 46 px wide
    m = ml.Mesh("flamecutter")
    sway = lambda y: 1.1 * math.sin(y * 0.085)               # the card's blade wanders a little: a flame, not a ruler
    wid = lambda t: W_BL * (0.35 + 0.65 * math.sin(min(1.0, t * 1.15) * math.pi / 2) ** 0.7) * (1 + 0.09 * math.sin(t * 19.0)) if t < 0.97 else 0.0
    sec = ml.lozenge(1.0, 1.0, 0.15)
    bl = w.add(m, "blade", "steel", base=(150, 40, 24), smooth=40)
    path = curve(sway, 0.0, BL_TOP + 0.6, 30)
    ml.sweep(bl, path, sec, scales=lambda t: (wid(t) if t < 0.985 else 0.0, 1.25 * (0.4 + 0.6 * min(1.0, t * 3)) if t < 0.985 else 0.0))
    fr = w.add(m, "flame_fringe", "steel", base=(230, 100, 30), smooth=25)                  # tongues of fire along the lower edges
    for side in (1, -1):
        for i, s in enumerate(np.linspace(10.0, BL_TOP * 0.8, 8)):
            hw = wid(s / BL_TOP) * 0.5
            ln = 3.4 + 2.4 * math.sin(i * 1.7) ** 2
            ml.loft(fr, np.array([side * 0.35, 0.94, 0.0]), (sway(s) + side * hw * 0.85, s, 0.0), [(0.0, 0.9, 0.35), (ln * 0.4, 1.0, 0.3), (ln, 0.0, 0.0)],
                    ml.lozenge(1.0, 0.4, 0.2), up=(0, 0, 1))
    cr = w.add(m, "core_ridge", "steel", base=(255, 170, 60), wear=0.9)
    for sg in (1, -1):
        pts = [(sway(y), y, sg * 0.62) for y in np.linspace(5.0, BL_TOP - 3.0, 18)]
        ml.sweep(cr, pts, ml.lozenge(1.0, 1.0), scales=lambda t: (0.8 * (1 - 0.6 * t), 0.2))
    # ---- 3 the golden horned guard: the core, two big horns curving up, spiked fins, two fins pointing down
    GY = Y(345)
    gc = w.add(m, "guard_core", "gold", base=(214, 158, 44), smooth=45)
    ml.lathe(gc, "y", (0, GY, 0), [(-5.0, 0.0), (-2.8, 2.6), (0.0, 3.4), (2.8, 2.6), (5.0, 0.0)], sides=10, squash=0.7)
    gh = w.add(m, "guard_horns", "gold", base=(222, 168, 52), smooth=40)
    gs = w.add(m, "guard_spikes", "gold", base=(240, 190, 70), smooth=25)
    for sx in (1, -1):
        pts = [(sx * (2.0 + 21.0 * t), GY + 1.0 + 11.0 * t ** 2.3 - 2.0 * t, 0.0) for t in np.linspace(0, 1, 14)]
        ml.sweep(gh, pts, ml.lozenge(1.0, 1.0, 0.2), scales=lambda t: (5.4 * (1 - 0.8 * t) + 0.4, 2.6 * (1 - 0.6 * t) + 0.3))
        for t in (0.25, 0.45, 0.62, 0.78):                                       # the serrated spikes along the horn's upper edge
            px, py = sx * (2.0 + 21.0 * t), GY + 1.0 + 11.0 * t ** 2.3 - 2.0 * t + 1.1
            spike(gs, (px, py, 0.0), (sx * 0.35, 1.0, 0.0), 3.2 - 1.6 * t, 0.65, sides=5)
        pts = [(sx * (1.5 + 6.0 * t), GY - 1.5 - 7.0 * t ** 1.6, 0.0) for t in np.linspace(0, 1, 8)]               # the fins that sweep down
        ml.sweep(gh, pts, ml.lozenge(1.0, 1.0, 0.2), scales=lambda t: (3.4 * (1 - 0.85 * t) + 0.3, 1.8 * (1 - 0.5 * t) + 0.3))
    # ---- 4 the dark red wrapped grip and the pommel: gem, crescent spike
    G0, G1 = Y(335), Y(248)
    gp = w.add(m, "grip", "leather", base=(96, 26, 34), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(G0, 0.95, 0.95), (G0 + 7, 1.05, 1.05), (G1, 0.98, 0.98)], ml.circle(1.7, 12))
    gw = w.add(m, "grip_wrap", "leather", base=(140, 48, 40), wear=0.4)
    wrap_rings(gw, G0 + 1.0, G1 - 0.6, 1.82, 9, h=0.6, sides=12)
    pm = w.add(m, "pommel_cap", "gold", base=(218, 164, 48), smooth=45)
    ml.lathe(pm, "y", (0, 0, 0), [(G1, 1.6), (G1 + 1.2, 2.5), (G1 + 3.2, 2.1), (G1 + 4.5, 1.3)], sides=12)
    pg = w.add(m, "pommel_gem", "crystal", base=(255, 130, 30), smooth=70)
    ml.gem(pg, (0, G1 + 6.2, 0), 1.6, long=1.25, sides=8, axis="y")
    cres = w.add(m, "pommel_crescent", "gold", base=(230, 176, 60), smooth=40)
    cpts = [(1.0 + 3.6 * math.sin(a), G1 + 7.0 + 4.6 * (1 - math.cos(a)) * 1.3 + 0.0, 0.0) for a in np.linspace(-0.2, 2.5, 14)]
    ml.sweep(cres, cpts, ml.lozenge(1.0, 1.0, 0.2), scales=lambda t: (1.6 * (1 - 0.9 * t) + 0.1, 1.2 * (1 - 0.8 * t) + 0.1))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)

    def hot(c):                                                                  # blade: dark red root, orange body, white-hot tip
        t = np.clip((c.B if c.kind == "dom" else c.B) / BL_TOP, 0, 1)
        col = np.stack([160 + 90 * t, 36 + 120 * t ** 1.4, 20 + 40 * t ** 3], axis=-1)
        lum = c.img[..., :3].mean(axis=2, keepdims=True) / 150.0
        c.img[..., :3] = np.clip(col * (0.7 + 0.3 * lum), 0, 255)
        w.set_glow(c, (255, 130, 30), 0.55, mask=np.clip(0.3 + 0.7 * t, 0, 1), core=0.35)
    w.paint_part(atlas, bl, hot)
    glow_part(atlas, fr, (255, 150, 40), 0.8)
    glow_part(atlas, cr, (255, 220, 120), 0.9)
    glow_part(atlas, pg, (255, 150, 40), 1.0)
    ref = w.reference(CARD3 + "eb131637b3c0024ba4f76b82e8552619.jpg", (430, 150, 700, 900), "flamecutter")
    return m, atlas, dict(real_m=1.3, glow=("FFFFFF", 0.22, 0.5, 1.0), ref=ref)


# ====================================================================================== 7 Heart of the Flame
def heart_of_the_flame():
    # ---- 1 measure: card 720 x 900. The sword runs from the pommel (75, 525) to the last shard (645, 112): 700 px for a 150 cm two-hander.
    K = 150.0 / 700.0
    S = lambda px: px * K                                     # px along the axis from the pommel -> model y
    W_PLATE = 70 * K                                          # the floating plates are about 70 px across
    m = ml.Mesh("heart_of_the_flame")
    # ---- 2 the floating plates: four slabs, each with a raised central ridge and angled-cut ends, separated by gaps (card: 180-300, 325-500, 520-600, 625-690 px)
    plates = [("plate_1", 178, 300, 1.0), ("plate_2", 325, 500, 1.0), ("plate_3", 522, 600, 0.9), ("plate_4", 628, 692, 0.75)]
    rd = w.add(m, "plate_ridges", "gold", base=(255, 226, 120), wear=0.9)
    for nm, a, b, sc in plates:
        pl = w.add(m, nm, "gold", base=(250, 176, 36), smooth=30, wear=0.3)
        ya, yb = S(a), S(b)
        ml.loft(pl, "y", (0, 0, 0), [(ya, 0.88 * sc, 0.7), (ya + 1.2, 1.0 * sc, 1.0), (yb - 1.5, 1.0 * sc, 1.0), (yb, 0.72 * sc, 0.7)],
                ml.bevelled(W_PLATE, 1.9, 0.3), caps=(True, True))
        ml.bar(rd, (0, ya + 1.5, 1.0), (0, yb - 1.5, 1.0), 0.9, 0.35, up=(0, 0, 1), taper=3.0)
        ml.bar(rd, (0, ya + 1.5, -1.0), (0, yb - 1.5, -1.0), 0.9, 0.35, up=(0, 0, -1), taper=3.0)
    sh = w.add(m, "shards", "gold", base=(255, 190, 50), smooth=25)                      # the broken pieces toward the tip
    for (y0, y1, dx, wd) in ((S(604), S(618), 2.2, 4.0), (S(696), S(706), -1.4, 2.6)):
        ml.loft(sh, "y", (dx, 0, 0), [(y0, 0.6 * wd, 0.6), (y0 + 1.0, wd, 1.0), (y1, 0.0, 0.0)], ml.bevelled(1.0, 1.0, 0.3))
    # ---- 3 the guard: a silver bar across with chevron wings and spiked ends, two orange gems
    GY = S(172)
    gb = w.add(m, "guard_bar", "silver", base=(188, 194, 208), smooth=40)
    ml.loft(gb, "x", (0, GY, 0), [(-9.0, 0.0, 0.0), (-7.0, 1.6, 1.1), (-2.5, 2.4, 1.6), (0.0, 2.8, 1.8), (2.5, 2.4, 1.6), (7.0, 1.6, 1.1), (9.0, 0.0, 0.0)], ml.octagon(1.0, 1.0, 0.25))
    gsp = w.add(m, "guard_spikes", "silver", base=(226, 230, 242), smooth=25)
    for sx in (1, -1):
        for dz, dy in ((0.0, 1.0), (0.0, -1.0)):
            spike(gsp, (sx * 8.0, GY, 0.0), (sx * 0.5, dy * 0.85, 0.0), 6.0, 1.0, sides=5)
        spike(gsp, (sx * 5.0, GY + 1.6, 0.0), (sx * 0.2, 1.0, 0.0), 4.5, 0.8, sides=5)
    gg = w.add(m, "guard_gem", "crystal", base=(255, 120, 40), smooth=70)
    ml.gem(gg, (0, GY, 1.7), 1.2, long=1.3, sides=6, axis="y")
    # ---- 4 the grip: dark grey with a chevron pattern, silver rings, the pommel with its gem
    G0, G1 = S(26), S(166)
    gp = w.add(m, "grip", "iron", base=(66, 70, 82), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(G0, 0.92, 0.92), (G0 + 20, 1.05, 1.05), (G1, 0.96, 0.96)], ml.octagon(3.0, 2.8, 0.8))
    gv = w.add(m, "grip_chevrons", "silver", base=(150, 156, 170), wear=0.6)
    for i in range(14):                                                                  # chevron ridges: pairs of bars meeting at the front
        yy = G0 + 3.0 + i * (G1 - G0 - 6.0) / 13
        for sx in (1, -1):
            ml.bar(gv, (0.0, yy, 1.45), (sx * 1.3, yy + 1.1, 1.3), 0.45, 0.16, up=(0, 0, 1))
    pm = w.add(m, "pommel", "silver", base=(176, 182, 198), smooth=35)
    ml.lathe(pm, "y", (0, 0, 0), [(S(0) - 0.4, 0.0), (S(4), 2.4), (S(10), 3.0), (S(18), 2.2), (S(27), 1.6)], sides=4)
    pg = w.add(m, "pommel_gem", "crystal", base=(255, 120, 40), smooth=70)
    ml.gem(pg, (0, S(12), 2.4), 0.8, long=1.3, sides=6, axis="y")

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    for nm, *_r in plates:
        p = m.part(nm)
        w.paint_part(atlas, p, lambda c: w.set_glow(c, (255, 150, 30), 0.38, core=0.3))
    glow_part(atlas, rd, (255, 230, 140), 0.8)
    glow_part(atlas, sh, (255, 170, 40), 0.7)
    glow_part(atlas, gg, (255, 130, 40), 1.0)
    glow_part(atlas, pg, (255, 130, 40), 1.0)
    ref = w.reference(CARD2 + "3d550745c167271594cd69c78eccb9ca.jpg", (40, 90, 700, 560), "heart_of_the_flame")
    return m, atlas, dict(real_m=1.45, glow=("FFFFFF", 0.2, 0.55, 1.0), ref=ref)


# ====================================================================================== 8 Bianca
def bianca():
    # ---- 1 measure: card 736 x 736 (a three-quarter view, so only the proportions count). Stock + body run from the butt (60, 560) to the muzzle plates
    # (350, 150): about 500 px for a 95 cm heavy repeater; the four limbs fan out from the front mount, each about 220 px (44 cm).
    K = 95.0 / 500.0
    BUTT, STOCK_END, BODY_END, MOUNT = 0.0, 40.0, 84.0, 76.0       # y positions in cm: the stock, where the body starts, its front, where the limbs mount
    m = ml.Mesh("bianca")
    STRING_Y = {"rest": 72.0, "pull1": 66.0, "pull2": 58.0, "pull3": 50.0, "charged": 50.0}      # where the string's centre sits per state (y, cm)
    st = w.add(m, "stock", "wood", base=(138, 86, 46), smooth=40)
    ml.loft(st, "y", (0, 0, 0), [(1.2, 1.0, 1.0), (10.0, 0.82, 0.8), (22.0, 0.68, 0.66), (30.0, 0.72, 0.7), (STOCK_END + 2.0, 0.95, 0.8)], ml.octagon(6.4, 12.0, 1.8))
    bp = w.add(m, "butt_plate", "steel", base=(150, 156, 170), smooth=35)
    ml.loft(bp, "y", (0, 0, 0), [(0.0, 0.96, 0.98), (1.4, 1.0, 1.0)], ml.octagon(6.6, 12.2, 1.8))
    bd = w.add(m, "body", "wood", base=(150, 96, 52), smooth=35)
    ml.loft(bd, "y", (0, 0, 0), [(STOCK_END, 1.0, 1.0), (BODY_END, 1.0, 1.0)], ml.octagon(7.6, 9.0, 1.6))
    bb = w.add(m, "brass_bands", "gold", base=(222, 168, 58), smooth=35)                 # the brass collars and the steel plate on the body
    for ya in (STOCK_END + 1.0, 56.0, 68.0, BODY_END - 4.0):
        ml.loft(bb, "y", (0, 0, 0), [(ya, 1.0, 1.0), (ya + 2.6, 1.0, 1.0)], ml.octagon(8.2, 9.6, 1.9))
    rl = w.add(m, "rail", "steel", base=(170, 176, 190), smooth=30)                      # the sight rail with its ring on top
    ml.bar(rl, (0, STOCK_END + 4, 4.6), (0, BODY_END, 4.6), 2.8, 0.7, up=(0, 0, 1), taper=3.0, section="rect")
    torus(rl, (0, 47.0, 6.6), 1.2, 0.3, plane="y", n=12, sides=5)
    ml.bar(rl, (0, 47.0, 4.8), (0, 47.0, 6.2), 0.5, 0.2, up=(0, 1, 0))
    cl = w.add(m, "coil_spring", "gold", base=(236, 190, 70), smooth=60)
    ml.sweep(cl, w.helix(lambda t: 1.5, 58.0, 76.0, 6, 42, 0.0, 6.0), ml.circle(0.32, 5), caps=(False, False))
    # ---- 3 the four steel limbs: two flat across, two swept back, each tapering and curling at the tip, with gold bands
    lm = w.add(m, "limbs", "steel", base=(168, 174, 188), smooth=40)
    lb = w.add(m, "limb_bands", "gold", base=(224, 170, 60), smooth=40)
    tips = []
    for sx in (1, -1):
        for kind, (reach, back, zoff, wid0) in (("front", (42.0, -9.0, 0.0, 5.0)), ("rear", (36.0, 22.0, -1.6, 4.4))):
            pts = [(sx * (1.5 + reach * t), MOUNT + (-back if kind == "rear" else 8.0) * (t ** 2.0 if kind == "front" else t), zoff + (0.0 if kind == "front" else 0.9 * t)) for t in np.linspace(0, 1, 10)]
            ml.sweep(lm, pts, ml.lozenge(1.0, 1.0, 0.1), scales=lambda t, w0=wid0: (w0 * (1 - 0.72 * t) + 0.4, 1.25 * (1 - 0.5 * t) + 0.25))
            for t in (0.3, 0.55, 0.78):
                px, py, pz = pts[int(t * 9)]
                ml.collar(lb, py - 0.5, 1.6, 1.0, lip=0.0, sides=6, axis="y", origin=(px, 0, pz), oval=0.5)
            tips.append(pts[-1])
    sg = w.add(m, "strings", "iron", base=(30, 28, 32), wear=0.2)
    notch = lambda tip, ny: (0.0, ny if tip[2] >= -0.5 else ny + 8.0, 2.4)            # the string's centre: front limbs to ny, rear limbs a hand behind

    def build_strings(part, ny):
        for tip in tips:
            ml.bar(part, tip, notch(tip, ny), 0.22, 0.1, up=(0, 0, 1))
    build_strings(sg, STRING_Y["rest"])
    # ---- 4 the ammo drum under the body, its six chambers and straps, the trigger guard and the bayonet
    dr = w.add(m, "drum", "iron", base=(86, 88, 100), smooth=40)
    ml.lathe(dr, "x", (-3.2, 60.0, -9.4), [(0.0, 0.0), (0.2, 6.2), (6.2, 6.2), (6.4, 0.0)], sides=14)
    ch = w.add(m, "drum_chambers", "bronze", base=(190, 130, 60), smooth=55)
    for i in range(6):
        a = 2 * math.pi * i / 6
        for sx in (1, -1):
            ml.stud(ch, (sx * 3.2, 60.0 + 4.0 * math.cos(a), -9.4 + 4.0 * math.sin(a)), 1.1, 0.5, (sx, 0, 0), sides=6)
    ds = w.add(m, "drum_straps", "leather", base=(96, 58, 34), wear=0.4)
    for ya in (54.0, 66.0):
        ml.bar(ds, (0.0, ya, -3.6), (0.0, ya, -8.0), 1.6, 0.5, up=(1, 0, 0), section="rect")
    tg = w.add(m, "trigger_guard", "steel", base=(150, 156, 172), smooth=45)
    ml.sweep(tg, [(0.0, 40.0 + 7.0 * math.sin(a), -4.4 - 4.8 * math.cos(a) * 0.9 + 0.0) for a in np.linspace(0, math.pi, 12)], ml.circle(0.4, 6), caps=(True, True))
    by = w.add(m, "bayonet", "steel", base=(200, 206, 220), smooth=35)
    ml.collar(by, BODY_END - 3.0, 2.0, 4.0, lip=0.4, sides=8, oval=0.9, origin=(0, 0, -1.6))
    ml.loft(by, "y", (0, 0, -1.6), [(BODY_END + 0.5, 1.0, 0.6), (BODY_END + 12.0, 0.8, 0.5), (BODY_END + 22.0, 0.0, 0.0)], ml.lozenge(2.6, 1.0, 0.0))

    bolt = w.add(m, "bolt", "wood", base=(150, 110, 70), smooth=35)                      # the loaded bolt on the rail (only the _charged mesh shows it)
    ml.loft(bolt, "y", (0.0, 0, 5.5), [(50.0, 0.9, 0.9), (60.0, 1.0, 1.0), (86.0, 1.0, 1.0)], ml.circle(0.33, 6), caps=(True, False))
    for ang in (0.0, 2.1, 4.2):
        ml.bar(bolt, (0.3 * math.cos(ang), 50.5, 5.5 + 0.3 * math.sin(ang)), (1.0 * math.cos(ang), 55.5, 5.5 + 1.0 * math.sin(ang)), 0.2, 0.55, up=(math.cos(ang), 0, math.sin(ang)), taper=1.0)
    ml.loft(bolt, "y", (0.0, 0, 5.5), [(86.0, 1.0, 1.0), (91.0, 0.0, 0.0)], ml.circle(0.55, 6), caps=(False, False))
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    glow_part(atlas, cl, (255, 200, 90), 0.25)
    glow_part(atlas, ch, (255, 170, 70), 0.3)
    ref = w.reference(CARD2 + "86ace2ee92d8e276ee1c8576d2aaa795.jpg", (20, 70, 700, 620), "bianca")
    m.parts.remove(bolt)                                                                  # the rest mesh has no bolt; extra() writes the draw and loaded meshes

    def extra():
        for state in ("pull1", "pull2", "pull3", "charged"):
            scratch = ml.Part("s")
            build_strings(scratch, STRING_Y[state])
            assert len(scratch.V) == len(sg.V) and len(scratch.F) == len(sg.F)
            sg.V = scratch.V
            if state == "charged":
                m.parts.append(bolt)
            w.write_state(m, atlas, "bianca_" + state)
    return m, atlas, dict(real_m=0.95, glow=("FFFFFF", 0.1, 0.4, 0.9), ref=ref, extra=extra)


# ====================================================================================== 9 Sorrow's Edge
def sorrows_edge():
    # ---- 1 measure: screenshot 900 x 2000 (display coords). The axe: pommel tuft (1500) to the head's top (580): 920 px for a 125 cm axe.
    K = 125.0 / 920.0
    Y = lambda py: (1500 - py) * K                          # screenshot y -> model y (pommel at 0)
    HAFT_R = 1.6                                            # the red haft is about 24 px across at the lower end
    HEAD_Y = Y(760)                                         # the head's centre (the crescent's middle)
    RIN = 19.0                                              # inner radius of the crescent
    CX = -12.0                                              # centre of the crescent's circle (so the edge bulges to +x)
    m = ml.Mesh("sorrows_edge")
    hf = w.add(m, "haft", "wood", base=(104, 14, 24), smooth=40, uv_mode="loft")
    ml.loft(hf, "y", (0, 0, 0), [(Y(1480), 0.9, 0.9), (Y(1400), 1.0, 1.0), (Y(1000), 0.96, 0.96), (Y(930), 1.06, 1.06)], ml.circle(HAFT_R, 12))
    hr = w.add(m, "haft_rings", "gold", base=(206, 166, 76), smooth=40)
    ml.collar(hr, Y(1385), HAFT_R * 1.14, 1.6, lip=0.3, sides=12)                        # the gold-wrapped leather band above the tuft
    ml.collar(hr, Y(925), HAFT_R * 1.25, 2.0, lip=0.4, sides=12)                          # and the neck ring under the head
    for yy in (Y(1330), Y(1300), Y(1270)):
        ml.collar(hr, yy, HAFT_R * 1.08, 0.5, lip=0.15, sides=12)
    pm = w.add(m, "pommel_cap", "iron", base=(40, 34, 36), smooth=50)
    ml.lathe(pm, "y", (0, 0, 0), [(Y(1500) + 3.2, 0.0), (Y(1500) + 3.6, 1.3), (Y(1480), 1.9), (Y(1462), 1.5)], sides=10)
    tf = w.add(m, "fur_tuft", "cloth", base=(48, 38, 36), wear=0.2, smooth=25)
    for i in range(9):
        a = 2 * math.pi * i / 9
        spike(tf, (0.0, Y(1470), 0.0), (0.55 * math.cos(a), -1.0, 0.55 * math.sin(a)), 7.0 + 1.5 * (i % 3), 0.8, sides=5, belly=0.4)
    # ---- 3 the head: a dark socket block with raised gold filigree
    hs = w.add(m, "head_socket", "iron", base=(30, 26, 34), smooth=40)
    y0, y1 = HEAD_Y - 14.0, HEAD_Y + 12.0
    ml.loft(hs, "y", (0, 0, 0), [(y0, 0.8, 0.8), (y0 + 3.0, 1.0, 1.0), (y1 - 3.0, 1.0, 1.0), (y1, 0.78, 0.78)], ml.octagon(6.6, 5.2, 1.4))
    fg = w.add(m, "filigree", "gold", base=(214, 176, 80), wear=0.5)
    for sg in (1, -1):                                                                   # the swirling gold lines: ridges that run, bend and branch
        ml.bar(fg, (0.5, y0 + 2.0, sg * 2.62), (0.5, y1 - 2.0, sg * 2.62), 0.5, 0.16, up=(0, 0, sg), taper=1.5)
        ml.bar(fg, (-1.8, y0 + 4.0, sg * 2.62), (1.4, HEAD_Y - 3.0, sg * 2.62), 0.4, 0.14, up=(0, 0, sg), taper=1.0)
        ml.bar(fg, (1.6, y0 + 5.0, sg * 2.62), (-1.4, HEAD_Y + 4.0, sg * 2.62), 0.4, 0.14, up=(0, 0, sg), taper=1.0)
        ml.bar(fg, (-2.2, HEAD_Y + 2.0, sg * 2.62), (2.2, y1 - 3.0, sg * 2.62), 0.4, 0.14, up=(0, 0, sg), taper=1.0)
    # ---- 4 the crescent blade: a flat sweep round a circle, widest at the middle, plus the edge band raised a little on both faces
    wid = lambda t: 2.0 + 6.6 * math.sin(math.pi * t) ** 0.85
    TH0, TH1 = math.radians(-64), math.radians(64)
    cb = w.add(m, "blade", "obsidian", base=(24, 24, 40), smooth=40)
    pts = []
    for i in range(0, 25):
        t = i / 24
        th = TH0 + (TH1 - TH0) * t
        r = RIN + wid(t) / 2
        pts.append((CX + r * math.cos(th), HEAD_Y + r * math.sin(th), 0.0))
    ml.sweep(cb, pts, ml.lozenge(1.0, 1.0, 0.1), scales=lambda t: (wid(t), 1.5))
    eb = w.add(m, "edge_band", "silver", base=(222, 228, 246), smooth=35, wear=0.6)
    pts = []
    for i in range(0, 25):
        t = i / 24
        th = TH0 + (TH1 - TH0) * t
        r = RIN + wid(t) - 0.2
        pts.append((CX + r * math.cos(th), HEAD_Y + r * math.sin(th), 0.0))
    ml.sweep(eb, pts, ml.lozenge(1.0, 1.0, 0.1), scales=lambda t: (1.5 * math.sin(math.pi * (0.08 + 0.84 * t)) + 0.15, 1.7))
    # ---- 5 the three claws on the back, curving up and away
    cw = w.add(m, "claws", "bone", base=(70, 62, 56), smooth=30)
    for dy, ln in ((9.0, 12.0), (1.0, 11.0), (-7.0, 9.5)):
        base = (-2.6, HEAD_Y + dy, 0.0)
        cp = [(base[0] - 8.0 * t, base[1] + 2.0 * t - ln * 0.25 * t ** 2 + (3.0 if dy > 0 else 1.0) * t ** 2, 0.0) for t in np.linspace(0, 1, 8)]
        ml.sweep(cw, cp, ml.circle(1.0, 6), scales=lambda t: (1.15 * (1 - t) + 0.05, 1.15 * (1 - t) + 0.05))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)

    def stars(c):                                                                    # a black blade speckled with stars
        rng = np.random.default_rng(int(c.img.shape[0] * 31 + c.img.shape[1]))
        mask = rng.random(c.img.shape[:2]) > 0.985
        c.img[mask, :3] = (180, 196, 255)
        w.set_glow(c, (140, 170, 255), 0.7, mask=mask.astype(np.float32), core=0.6)
    w.paint_part(atlas, cb, stars)
    w.paint_part(atlas, eb, lambda c: w.set_glow(c, (130, 160, 240), 0.35, core=0.4))
    glow_part(atlas, fg, (255, 214, 120), 0.2)
    ref = w.reference(CARD2 + "Screenshot_20261013_223125_Opera.jpg", (200, 560, 560, 1540), "sorrows_edge")
    return m, atlas, dict(real_m=1.25, glow=("FFFFFF", 0.12, 0.35, 0.9), ref=ref)


# ====================================================================================== 10 The Dragons Truth
def the_dragons_truth():
    # ---- 1 measure: card 736 x 886. The belt is drawn as a flat oval about 380 px long: a 50 cm belt (real: eight daggers of 14 cm side by side).
    # In hand it is held coiled: a ring of radius 8 cm with the eight dragon-tooth daggers standing in their loops (grips up, fangs down).
    RB, BELT_H, BELT_T = 8.4, 4.2, 0.7
    TOOTH_BLADE, GRIP_L = 12.0, 7.5
    m = ml.Mesh("the_dragons_truth")
    bt = w.add(m, "belt_band", "leather", base=(132, 62, 42), smooth=40, uv_mode="loft")
    ml.lathe(bt, "y", (0, 0, 0), [(0.0, RB), (BELT_H, RB), (BELT_H, RB - BELT_T), (0.0, RB - BELT_T), (0.0, RB)], sides=24, caps=(False, False))
    bs = w.add(m, "belt_stitching", "cloth", base=(214, 196, 150), wear=0.3)
    for ya in (0.45, BELT_H - 0.9):
        ml.collar(bs, ya, RB + 0.06, 0.4, lip=0.1, sides=24)
    # the buckle: a brass ring on the front (+z), its tongue and the belt's tail
    bk = w.add(m, "buckle_ring", "gold", base=(226, 176, 60), smooth=40)
    torus(bk, (0.0, BELT_H / 2, RB + 0.6), 2.4, 0.5, plane="z", n=14, sides=5)
    bkt = w.add(m, "buckle_tongue", "gold", base=(236, 190, 80), smooth=40)
    ml.bar(bkt, (0.0, BELT_H / 2, RB + 0.6), (2.4, BELT_H / 2, RB + 0.6), 0.5, 0.3, up=(0, 0, 1))
    tail = w.add(m, "belt_tail", "leather", base=(110, 50, 34), smooth=40)
    ml.loft(tail, "x", (0.0, 0.0, 0.0), [(0.4, 0.9, 1.0), (4.0, 0.95, 1.0), (7.0, 0.9, 1.0)], ml.rect_ring(BELT_H * 0.9, 0.5), up=(0, 0, 1)) if False else None
    ml.bar(tail, (RB * 0.35, BELT_H / 2, RB + 0.2), (RB * 0.35 + 5.5, BELT_H / 2, RB * 0.93), BELT_H * 0.8, 0.28, up=(0, 0, 1), section="rect")
    gr = w.add(m, "tooth_grips", "iron", base=(30, 28, 34), uv_mode="loft")
    pm = w.add(m, "tooth_pommels", "bone", base=(210, 196, 160), smooth=40)
    gb = w.add(m, "tooth_bands", "gold", base=(206, 160, 60), wear=0.3)
    fg = w.add(m, "fangs", "bone", base=(238, 232, 212), smooth=35, wear=0.4)
    lp = w.add(m, "loops", "leather", base=(150, 76, 48), wear=0.4)
    for i in range(8):
        a = math.radians(-67.5 + 45.0 * i) if False else 2 * math.pi * (i + 0.5) / 8
        px, pz = (RB + 0.1) * math.cos(a), (RB + 0.1) * math.sin(a)
        ml.loft(gr, "y", (px, 0, pz), [(BELT_H - 0.6, 0.9, 0.9), (BELT_H + GRIP_L * 0.5, 1.0, 1.0), (BELT_H + GRIP_L, 0.9, 0.9)], ml.circle(0.62, 6))
        for k in range(3):
            ml.collar(gb, BELT_H + 1.2 + k * 2.2, 0.72, 0.4, lip=0.0, sides=6, origin=(px, 0, pz))
        ml.lathe(pm, "y", (px, 0, pz), [(BELT_H + GRIP_L - 0.1, 0.65), (BELT_H + GRIP_L + 0.5, 0.95), (BELT_H + GRIP_L + 1.4, 0.55), (BELT_H + GRIP_L + 1.9, 0.0)], sides=6)
        ml.collar(lp, 0.4, 1.0, BELT_H - 0.8, lip=0.0, sides=6, origin=(px, 0, pz))
        fa = np.array([math.cos(a), 0.0, math.sin(a)])                           # the fang hangs below the belt and curls slightly outward
        for_pts = [(px + fa[0] * 0.9 * (s / TOOTH_BLADE) ** 2, 0.6 - s, pz + fa[2] * 0.9 * (s / TOOTH_BLADE) ** 2) for s in np.linspace(0, TOOTH_BLADE, 5)]
        ml.sweep(fg, for_pts, ml.lozenge(1.0, 1.0, 0.0), scales=lambda t: (1.5 * (1 - t ** 1.4) + 0.02, 1.0 * (1 - t ** 1.2) + 0.02), up=(fa[0], 0, fa[2]))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    glow_part(atlas, bk, (255, 200, 90), 0.2)
    w.paint_part(atlas, fg, lambda c: w.set_glow(c, (255, 224, 150), 0.28, mask=np.clip(c.T, 0, 1) ** 2, core=0.5))
    ref = w.reference(CARD2 + "be8bcd9ea3773d2c70eb40d08fdf4048.jpg", (160, 150, 600, 340), "dragons_truth")
    return m, atlas, dict(real_m=0.3, fit=True, glow=("FFFFFF", 0.12, 0.35, 0.9), ref=ref)
