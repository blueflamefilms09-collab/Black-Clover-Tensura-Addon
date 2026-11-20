"""The specs of the katanas built by katanas.py: one dict each (colours, blade numbers, tsuba, hamon, one lore feature). Colours are 0..255 rgb.

Keys: id, seed, div (blade length / habaki width, real 18..22), kasane (thickness / width, real <= 0.33), sori (back-to-chord depth / blade length, real 0.01..0.04),
kissaki, taper, bohi (a groove), round (a bokken's oval section), tsuka (grip share of the length), tail (cm of a hanging charm / hook, kept inside the old length),
tsuba (shape, R, ratio, steps), hamon (kind, amplitude, period; a negative period = that many waves; None = no hamon), hamon_base, turns (ito), cord, nohabaki, nomenuki,
blade_mat (a name in extra_mats instead of steel), pal (the palette), extras(c, P, rng) (the lore feature), extra_mats {name: fn(seed) -> material}.
"""
import math

import bl_lib as bl  # noqa: F401
import forms_kit as K
import forms_vassal_lib as Lb
import katanas as KT
from forms_kit import V, rgb, TAU


# ================================================================ helpers for lore features (all painted ones are bake-only: zero quads in game)
def diamond(name, centre, size, mat, s, part="blade_paint", stretch=1.0):
    """A small bake-only diamond lying on the face s of the blade (a fleck, a star, a leaf)."""
    x, y, z = centre
    pts = [V((x - size, y, z)), V((x, y + size * stretch, z)), V((x + size, y, z)), V((x, y - size * stretch, z))]
    return K.mk(name, pts, [(0, 1, 2, 3)] if s > 0 else [(3, 2, 1, 0)], mat, part, True, smooth=False)


def blob_on(c, name, y, a, ry, ra, mat, s, lift=0.1, n=9, part="blade_paint", thick=0.05):
    """An oval patch on the blade face: centre (y, a), half sizes ry (cm along) and ra (shares across)."""
    rows = []
    ys = [y + ry * (-1 + 2 * i / (n - 1)) for i in range(n)]
    for yy in ys:
        t = (yy - y) / ry
        h = ra * math.sqrt(max(0.0, 1 - t * t))
        rows.append((c.pt(yy, a - h, s, lift), c.pt(yy, a + h + 1e-4, s, lift)))
    K.strip("%s%+d" % (name, s), rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=(0, 0, s), detail=True, angle=60)


def grooves(c, P, mat_line):
    """A line of light down the bo-hi groove of both faces."""
    ys = [c.yh + c.Bl * (0.08 + 0.80 * i / 24) for i in range(25)]
    KT.paint(c, "bohi_line", ys, lambda y: 0.795, lambda y: 0.835, mat_line, lift=0.05, thick=0.03, part="blade_paint")


def tassel(c, P, rng, mat_cord, mat_bead, length=26.0, bead=True, side=1):
    """A sageo-style cord with a charm bead hanging from the kashira (a bounded tapered tube; it hangs inside the `tail` the spec reserves)."""
    top = V((0.0, c.y0 + 0.4 * c.kashira_h, 0.0))
    pts = [top, top + V((side * 1.2, -length * 0.1, 0.5)), top + V((side * 2.6, -length * 0.35, 0.1)), top + V((side * 3.0, -length * 0.6, -0.4)), top + V((side * 2.4, -length * 0.8, 0.2)),
           top + V((side * 1.8, -length, 0.0))]
    K.tapered("tassel", pts, 0.32, 0.20, mat_cord, sides=5, sub=3, part="tassel")
    if bead:
        K.blob("tassel_bead", (pts[-1].x, pts[-1].y - 0.5, pts[-1].z), (0.8, 1.1, 0.8), mat_bead, seed=3, lump=0.05, rings=5, segs=8, part="tassel")


def spine_teeth(c, mat, n=7, y0=0.22, y1=0.62, size=0.55):
    """Small bone fangs along the spine (+x): flat triangles in the plane of the blade, leaning toward the tip. Bounded: n up to 9."""
    for i in range(n):
        u = y0 + (y1 - y0) * i / max(1, n - 1)
        y = c.yh + u * c.Bl
        sp = c.spine(y)
        h = c.w0 * size * (0.8 + 0.3 * math.sin(i * 1.7))
        th = c.half(y) * 0.8
        base0, base1 = y, y + c.w0 * 0.55
        tip = V((sp + h, base1 + c.w0 * 0.25, 0.0))
        K.strip("tooth%d" % i, [(V((sp - 0.1, base0, 0)), V((sp - 0.1, base0 + 0.01, 0))), (V((sp + 0.2, base0 + 0.35 * (base1 - base0), 0)), V((sp + 0.2, base1 - 0.15, 0))), (tip, tip + V((0, 0.02, 0)))],
                mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), th, part="spine_teeth", up=(0, 0, 1), angle=60)


def stars(c, P, rng, n=46, mat=None, size=0.22):
    """Starlight flecks over the blade (bake-only diamonds)."""
    mat = mat or P["accent"]
    for i in range(n):
        s = 1 if i % 2 else -1
        y = c.yh + c.Bl * rng.uniform(0.07, 0.93)
        a = rng.uniform(0.12, 0.92)
        p = c.pt(y, a, s, 0.07)
        diamond("star%d" % i, (p.x, p.y, p.z), size * rng.uniform(0.5, 1.6), mat, s, stretch=rng.uniform(1.0, 1.7))


def scatter(c, rng, n, mat, ry=(1.2, 3.0), ra=(0.07, 0.16), y=(0.12, 0.9), a=(0.25, 0.8), name="patch", lift=0.1):
    """n oval patches scattered over both faces (bake-only paint). Bounded: n up to 12."""
    for i in range(n):
        blob_on(c, "%s%d" % (name, i), c.yh + c.Bl * rng.uniform(*y), rng.uniform(*a), rng.uniform(*ry), rng.uniform(*ra), mat, 1 if i % 2 else -1, lift=lift)


def slashes(c, mat, n=4, y0=0.12, y1=0.5):
    """Thin claw cuts of light along the ridge of the blade (long thin diamonds, bake-only)."""
    for i in range(n):
        s = 1 if i % 2 else -1
        y = c.yh + c.Bl * (y0 + (y1 - y0) * i / max(1, n - 1))
        p = c.pt(y, 0.55 + 0.1 * (i % 2), s, 0.07)
        diamond("slash%d" % i, (p.x, p.y, p.z), c.w0 * 0.07, mat, s, stretch=9.0)


def grain(c, rng, mat, n=5):
    """Wood-grain lines along a bokken (bake-only strips of a darker wood)."""
    for i in range(n):
        a0 = 0.12 + 0.17 * i + rng.uniform(-0.03, 0.03)
        ph = rng.uniform(0, 6.28)
        ys = [c.yh + c.Bl * (0.04 + 0.9 * k / 20) for k in range(21)]
        KT.paint(c, "grain%d" % i, ys, lambda y, a0=a0, ph=ph: a0 + 0.015 * math.sin(y / 5.0 + ph), lambda y, a0=a0, ph=ph: a0 + 0.03 + 0.015 * math.sin(y / 5.0 + ph), mat,
                 lift=0.04, thick=0.02, part="blade_paint")


def eye_mark(c, P, y_share=0.5):
    """The staring eye of the Corpse Eater: sclera, red iris and a slit pupil painted into both faces (bake-only, so no quads)."""
    y = c.yh + c.Bl * y_share
    for s in (1, -1):
        blob_on(c, "eye_ball", y, 0.5, 3.4, 0.30, P["ray"], s, lift=0.08, n=11)
        blob_on(c, "eye_iris", y, 0.5, 1.8, 0.17, P["accent"], s, lift=0.11, n=9)
        blob_on(c, "eye_pupil", y, 0.5, 1.4, 0.04, P["accent_dark"], s, lift=0.14, n=9)


def hook(c, P):
    """A small iron hook under the kashira (the Dragon Tooth Blade's tail): a tapered tube, 9 cm, bounded."""
    t = V((0.0, c.y0 + 0.5, 0.0))
    pts = [t, t + V((0.0, -2.6, 0.0)), t + V((1.8, -5.2, 0.0)), t + V((4.2, -6.6, 0.0)), t + V((6.0, -5.2, 0.0)), t + V((6.4, -3.0, 0.0))]
    K.tapered("hook", pts, 0.55, 0.12, P["fit"], sides=6, sub=3, part="hook")


def void_ring(c, P):
    """The ring of the Void Origin Katana, hovering round the kashira: a glowing torus (120 quads)."""
    Lb.ring_torus("void_ring", (0.0, c.y0 + 0.2 * c.kashira_h, 0.0), 0.95 * c.w0 + 0.6, 0.16, P["accent"], "void_ring", plane="xz", sides=6, segs=20)


# ================================================================ the specs
SPECS = {}


def spec(key):
    def deco(fn):
        SPECS[key] = fn()
        return fn
    return deco


@spec("bloodbound")
def _bloodbound():
    def ex(c, P, rng):
        grooves(c, P, P["line"])
        scatter(c, rng, 5, P["accent_dark"], ry=(0.8, 2.0), ra=(0.05, 0.10), y=(0.1, 0.55), a=(0.2, 0.6), name="blood", lift=0.08)
        tassel(c, P, rng, P["ito"], P["accent"], length=10.0, bead=True)
    return dict(id="bloodbound_katana", seed=21, div=16.0, sori=0.028, kasane=0.30, bohi=True, hamon=("gunome", 0.10, None), tsuba=("mokko", None, 0.95), tail=11.0, extras=ex,
                pal=dict(steel=(40, 20, 28), steel_wear=(150, 60, 72), hamon=(150, 16, 34), hamon2=(90, 10, 22), hamon_emit=(255, 40, 70), hamon_glow=0.55, line=(255, 40, 70), line_s=1.6,
                         fit=(62, 32, 38), fit_wear=(170, 70, 82), fit2=(30, 14, 18), tsuba=(30, 18, 22), ito=(112, 14, 28), ito2=(22, 12, 16), ray=(66, 44, 48), menuki=(210, 52, 72),
                         accent=(255, 40, 70), accent_s=1.0, accent_dark=(60, 6, 14)))


@spec("slasher")
def _slasher():
    def ex(c, P, rng):
        slashes(c, P["accent"], n=5, y0=0.12, y1=0.62)
    return dict(id="demon_slasher_katana", seed=31, div=16.0, sori=0.030, kasane=0.30, hamon=("midare", 0.12, None), tsuba=("thorn", None, 0.95), extras=ex,
                pal=dict(steel=(30, 26, 32), steel_wear=(130, 70, 80), hamon=(170, 24, 44), hamon2=(110, 14, 30), hamon_emit=(255, 60, 80), hamon_glow=0.5, line=(255, 60, 80), line_s=1.5,
                         fit=(52, 40, 44), fit_wear=(160, 90, 96), fit2=(24, 16, 20), tsuba=(26, 20, 24), ito=(20, 16, 20), ito2=(130, 20, 34), ray=(70, 62, 64), menuki=(220, 60, 80),
                         accent=(255, 60, 80), accent_s=1.2))


@spec("miasma")
def _miasma():
    def mat(seed):
        return Lb.glow("k_miasma", rgb(150, 90, 235), 0.9, seed, flicker=0.5)

    def ex(c, P, rng):
        scatter(c, rng, 9, P["miasma"], ry=(1.5, 4.0), ra=(0.08, 0.2), y=(0.08, 0.92), a=(0.2, 0.85), name="miasma", lift=0.08)
    return dict(id="miasma_infused_katana", seed=41, div=16.0, sori=0.026, kasane=0.30, hamon=("choji", 0.12, None), tsuba=("cloud", None, 0.95), extras=ex, extra_mats={"miasma": mat},
                pal=dict(steel=(204, 204, 214), steel_wear=(240, 240, 248), mottle=(150, 150, 168), hamon=(250, 248, 255), hamon2=(190, 170, 235), hamon_emit=(150, 90, 235), hamon_glow=0.3,
                         line=(150, 90, 235), line_s=0.9, fit=(58, 52, 70), fit_wear=(160, 150, 190), fit2=(28, 24, 36), tsuba=(40, 36, 52), ito=(230, 228, 234), ito2=(24, 22, 30),
                         ray=(220, 218, 224), menuki=(170, 130, 240)))


@spec("hakurou")
def _hakurou():
    return dict(id="hakurou_school_practice_katana", seed=11, div=18.0, sori=0.022, hamon=("notare", 0.06, None), tsuba=("round", None, 0.95),
                pal=dict(steel=(196, 208, 224), hamon=(246, 250, 255), hamon2=(205, 220, 242), hamon_emit=(150, 205, 255), hamon_glow=0.22, line=(150, 205, 255), line_s=0.9,
                         fit=(52, 52, 60), fit_wear=(140, 140, 150), fit2=(30, 30, 36), tsuba=(36, 36, 44), ito=(26, 38, 96), ray=(232, 228, 216), menuki=(190, 196, 210)))


@spec("bokken")
def _bokken():
    def wood(seed):
        return Lb.paint("k_wood", rgb(168, 120, 66), rgb(210, 168, 108), seed, rough=0.78, mottle=rgb(112, 72, 36), bump="grain")

    def dark(seed):
        return Lb.paint("k_wood_dark", rgb(92, 58, 28), rgb(140, 100, 60), seed, rough=0.8)

    def ex(c, P, rng):
        grain(c, rng, P["dark"], n=5)
    return dict(id="selection_hopefuls_bokken", seed=51, div=17.0, sori=0.012, kasane=0.62, round=True, hamon=None, nohabaki=True, nomenuki=True, blade_mat="wood", tsuba=("round", None, 0.95, 2),
                tsuka=0.24, cord=0.2, turns=5.0, extras=ex, extra_mats={"wood": wood, "dark": dark},
                pal=dict(steel=(150, 150, 150), fit=(96, 70, 40), fit_wear=(150, 120, 80), fit2=(60, 40, 20), tsuba=(70, 46, 26), tsuba_wear=(130, 100, 70), ito=(86, 56, 30), ito2=(120, 84, 48),
                         ray=(170, 124, 70), kash=(96, 70, 40), menuki=(120, 90, 50)))


@spec("carnage")
def _carnage():
    def mat(seed):
        return Lb.paint("k_blood", rgb(96, 8, 16), rgb(150, 30, 40), seed, rough=0.4)

    def ex(c, P, rng):
        scatter(c, rng, 6, P["blood"], ry=(0.9, 2.4), ra=(0.05, 0.12), y=(0.1, 0.7), a=(0.15, 0.6), name="stain", lift=0.07)
        tassel(c, P, rng, P["ito"], P["accent"], length=13.5, bead=True)
    return dict(id="katana_of_carnage", seed=61, div=19.0, sori=0.030, kasane=0.27, hamon=("midare", 0.14, None), tsuba=("mokko", None, 0.95), tail=15.0, extras=ex, extra_mats={"blood": mat},
                pal=dict(steel=(176, 172, 180), steel_wear=(245, 240, 245), mottle=(120, 112, 122), hamon=(250, 240, 242), hamon2=(210, 170, 176), hamon_emit=(255, 100, 120), hamon_glow=0.25,
                         line=(255, 100, 120), line_s=1.0, fit=(70, 62, 66), fit_wear=(170, 150, 156), fit2=(34, 28, 30), tsuba=(40, 30, 34), ito=(128, 14, 28), ito2=(24, 14, 18),
                         ray=(220, 212, 206), menuki=(200, 60, 76), accent=(255, 90, 110), accent_s=0.9))


@spec("shogun")
def _shogun():
    def ex(c, P, rng):
        for k, s in enumerate((1, -1)):
            p = c.pt(c.yh + c.Bl * 0.10, 0.45, s, 0.07)
            for j in range(3):                                   # a mitsu-domoe stand-in: three gold diamonds in a ring
                ang = TAU * j / 3 + 0.5
                diamond("mon%d_%d" % (k, j), (p.x + 0.14 * c.w0 * math.cos(ang), p.y + 0.21 * c.w0 * math.sin(ang), p.z), c.w0 * 0.09, P["accent"], s, stretch=1.6)
    return dict(id="katana_of_the_crimson_shogun", seed=71, div=19.0, sori=0.027, kasane=0.27, hamon=("gunome", 0.10, None), tsuba=("sun", None, 0.95, 4), extras=ex,
                pal=dict(steel=(196, 200, 212), steel_wear=(240, 242, 250), mottle=(140, 146, 164), bump="grain", bump_s=0.14, hamon=(248, 248, 252), hamon2=(196, 204, 224), hamon_emit=(255, 120, 120),
                         hamon_glow=0.12, fit=(214, 168, 56), fit_wear=(255, 238, 170), fit2=(150, 96, 24), tsuba=(40, 30, 32), tsuba_wear=(214, 168, 56), ito=(140, 16, 30), ito2=(58, 10, 16),
                         ray=(230, 224, 212), menuki=(224, 178, 66), accent=(240, 190, 70), accent_s=0.35))


@spec("shadowsteel")
def _shadowsteel():
    def ex(c, P, rng):
        grooves(c, P, P["line"])
        scatter(c, rng, 7, P["accent_dark"], ry=(1.8, 5.0), ra=(0.08, 0.2), y=(0.1, 0.9), a=(0.3, 0.9), name="shade", lift=0.06)
    return dict(id="shadowsteel_katana", seed=81, div=20.0, sori=0.032, kasane=0.27, bohi=True, hamon=("notare", 0.09, None), tsuba=("cloud", None, 0.95), extras=ex,
                pal=dict(steel=(32, 24, 46), steel_wear=(150, 120, 210), hamon=(150, 100, 230), hamon2=(90, 56, 160), hamon_emit=(160, 100, 255), hamon_glow=0.7, line=(170, 110, 255), line_s=1.5,
                         fit=(48, 36, 70), fit_wear=(150, 120, 210), fit2=(24, 18, 36), tsuba=(26, 20, 38), tsuba_wear=(120, 90, 190), ito=(52, 30, 84), ito2=(20, 14, 30), kash=(150, 30, 46),
                         ray=(60, 50, 76), menuki=(170, 110, 255), accent_dark=(8, 6, 14)))


@spec("void")
def _void():
    return dict(id="void_origin_katana", seed=91, div=21.0, sori=0.034, kasane=0.25, hamon=("suguha", 0.0, None), hamon_base=0.12, tsuba=("round", None, 0.95), extras=lambda c, P, r: void_ring(c, P),
                pal=dict(steel=(14, 20, 34), steel_wear=(90, 160, 200), hamon=(80, 200, 240), hamon2=(30, 120, 180), hamon_emit=(90, 220, 255), hamon_glow=0.9, line=(130, 235, 255), line_s=1.8,
                         fit=(24, 32, 48), fit_wear=(100, 180, 220), fit2=(10, 14, 24), tsuba=(12, 16, 26), tsuba_wear=(70, 150, 200), ito=(16, 24, 40), ito2=(30, 90, 120), ray=(40, 56, 74),
                         menuki=(120, 230, 255), accent=(110, 230, 255), accent_s=1.6))


@spec("corpse")
def _corpse():
    def bone(seed):
        return Lb.paint("k_bone", rgb(226, 214, 188), rgb(250, 244, 226), seed, rough=0.6, mottle=rgb(176, 160, 130), bump="grain")

    def ex(c, P, rng):
        spine_teeth(c, P["bone"], n=7, y0=0.2, y1=0.64, size=0.5)
        eye_mark(c, P, 0.5)
    return dict(id="corpse_eater", seed=101, div=19.0, sori=0.028, kasane=0.28, hamon=("midare", 0.14, None), tsuba=("thorn", None, 0.95), extras=ex, extra_mats={"bone": bone},
                pal=dict(steel=(78, 80, 72), steel_wear=(190, 196, 180), mottle=(50, 56, 46), hamon=(210, 200, 180), hamon2=(150, 40, 44), hamon_emit=(200, 30, 40), hamon_glow=0.5,
                         line=(230, 40, 50), line_s=1.4, fit=(180, 168, 140), fit_wear=(240, 232, 204), fit2=(100, 84, 60), tsuba=(52, 50, 44), tsuba_wear=(180, 170, 140), ito=(96, 18, 26),
                         ito2=(30, 22, 22), ray=(220, 210, 188), menuki=(226, 214, 188), accent=(255, 40, 50), accent_s=1.4, accent_dark=(8, 6, 8)))


@spec("ilmare")
def _ilmare():
    def ex(c, P, rng):
        stars(c, P, rng, n=40, mat=P["accent"], size=0.2)
    return dict(id="ilmare_gin_the_starlight_silver", seed=111, div=21.0, sori=0.030, kasane=0.26, hamon=("notare", 0.05, None), tsuba=("round", None, 0.95), extras=ex,
                pal=dict(steel=(24, 32, 72), steel_wear=(150, 172, 235), mottle=(14, 20, 52), hamon=(206, 218, 255), hamon2=(150, 170, 235), hamon_emit=(180, 205, 255), hamon_glow=0.45,
                         line=(190, 215, 255), line_s=1.2, fit=(150, 160, 190), fit_wear=(235, 240, 255), fit2=(70, 80, 120), tsuba=(20, 26, 56), tsuba_wear=(150, 170, 230), ito=(16, 18, 32),
                         ito2=(60, 84, 160), ray=(210, 214, 230), menuki=(220, 232, 255), accent=(232, 242, 255), accent_s=1.0))


@spec("kusanagi")
def _kusanagi():
    def ex(c, P, rng):
        for i in range(9):                                       # grass blades: thin leaves climbing the ridge
            s = 1 if i % 2 else -1
            p = c.pt(c.yh + c.Bl * (0.10 + 0.08 * i), 0.5 + 0.12 * ((i % 3) - 1), s, 0.07)
            diamond("leaf%d" % i, (p.x, p.y, p.z), c.w0 * 0.08, P["accent"], s, stretch=5.0)
        tassel(c, P, rng, P["ito"], P["accent"], length=9.5, bead=True)
    return dict(id="kusanagi_the_grass_cutter", seed=121, div=19.0, sori=0.030, kasane=0.27, hamon=("notare", 0.08, None), tsuba=("aoi", None, 0.95), tail=11.0, extras=ex,
                pal=dict(steel=(196, 224, 208), steel_wear=(240, 255, 246), mottle=(150, 196, 170), hamon=(240, 255, 246), hamon2=(170, 230, 196), hamon_emit=(110, 255, 170), hamon_glow=0.5,
                         line=(120, 255, 180), line_s=1.2, fit=(196, 204, 156), fit_wear=(250, 250, 214), fit2=(110, 120, 70), tsuba=(60, 100, 76), tsuba_wear=(190, 230, 200), ito=(36, 104, 72),
                         ito2=(214, 232, 218), ray=(226, 232, 216), menuki=(120, 255, 180), accent=(120, 255, 180), accent_s=1.1))


@spec("dragon")
def _dragon():
    return dict(id="dragon_tooth_blade_red", seed=131, div=18.5, sori=0.029, kasane=0.28, hamon=("choji", 0.14, None), tsuba=("thorn", None, 0.95), tail=9.0, extras=lambda c, P, r: hook(c, P),
                pal=dict(steel=(232, 222, 206), steel_wear=(255, 250, 240), mottle=(196, 182, 160), hamon=(210, 48, 44), hamon2=(150, 26, 30), hamon_emit=(255, 70, 60), hamon_glow=0.5,
                         line=(255, 90, 70), line_s=1.2, fit=(168, 150, 120), fit_wear=(244, 232, 206), fit2=(90, 70, 40), tsuba=(60, 26, 26), tsuba_wear=(180, 90, 80), ito=(140, 16, 24),
                         ito2=(30, 12, 14), ray=(226, 214, 192), menuki=(230, 70, 60)))


@spec("wavebreaker")
def _wavebreaker():
    def ex(c, P, rng):
        scatter(c, rng, 8, P["accent"], ry=(2.0, 5.0), ra=(0.06, 0.12), y=(0.1, 0.9), a=(0.15, 0.4), name="foam", lift=0.07)
        tassel(c, P, rng, P["ito"], P["menuki"], length=9.5, bead=True)
    return dict(id="wavebreaker", seed=141, div=22.0, sori=0.020, kasane=0.26, hamon=("notare", 0.13, -14), tsuba=("aoi", None, 0.95), tsuka=0.22, tail=11.0, extras=ex,
                pal=dict(steel=(52, 132, 150), steel_wear=(180, 240, 250), mottle=(30, 96, 120), hamon=(240, 252, 255), hamon2=(150, 220, 236), hamon_emit=(140, 230, 255), hamon_glow=0.35,
                         line=(130, 230, 255), line_s=1.2, fit=(158, 178, 196), fit_wear=(236, 244, 255), fit2=(70, 90, 110), tsuba=(30, 70, 90), tsuba_wear=(150, 210, 230), ito=(22, 52, 110),
                         ito2=(150, 220, 235), ray=(214, 226, 230), menuki=(200, 240, 250), accent=(210, 246, 255), accent_s=0.5))
