"""The characters of the Black Clover boss player models: per look a face spec (bcskin) and a build function (bcparts).  CHARS is read by gen_bc.py.

Sources (rule 10): every canon detail below is from the English Black Clover Fandom wiki's Appearance sections (Lucius Zogratis, Dante Zogratis, Vanica Zogratis, Zenon Zogratis, Zagred,
Lucifero, Megicula), fetched with curl in this session; the Japanese wiki was not needed. The mod's own colours (the family's dark blue fur coats, the divine forms' white and gold) are the
earlier skins' choice and are kept.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import bcparts as P  # noqa: E402
import bcparts2 as Q  # noqa: E402
import bcparts3 as R3  # noqa: E402
import bcskin as S  # noqa: E402

PALE = ("#d4bcc4", "#ead4d4", "#fbeeea")
FAIR = ("#d8a88c", "#f0caae", "#ffe4d0")
ASH = ("#b4a8b6", "#d0c4d0", "#ece2ea")
GREY = ("#8a808c", "#a89ea8", "#c8c0c8")
GOLD = "#e8b830"
RED = "#d81c40"
COAT = "#1b2a4e"


def _lines(pts_fn):
    return lambda f: pts_fn(f)


# ================================================================================================ Lucius Zogratis
def lucius(b):
    hair = "#241e2c"
    P.hair_shell(b, hair, seed=1, front_low=30.6, side_low=28.6, back_low=25.2)
    P.hair_tiers(b, hair, n=2, y0=33.3, w0=8.8, shrink=1.6, h=1.3, seed=2)
    P.hair_spikes(b, hair, n=5, y=34.3, spread=2.9, length=5.2, w=3.3, seed=3, rows=2, back_lean=18.0, fan=34.0)
    P.fringe(b, hair, n=9, span=8.4, y_top=31.6, length=3.2, seed=4, rot=12.0)
    P.mantle(b, "#f2f0f6", "#bcb8cc", y=21.9, thick=2.5, width=16.4, depth=6.8, drop=2.8, seed=10)


def lucius_divine(b):
    hair = "#f2eef8"
    P.hair_shell(b, hair, seed=1, front_low=30.6, side_low=28.4, back_low=24.4)
    P.hair_tiers(b, hair, n=4, y0=33.6, w0=9.4, shrink=1.9, h=1.4, seed=2)
    P.hair_spikes(b, hair, n=7, y=36.2, spread=2.6, length=4.4, w=2.0, seed=5, rows=1, back_lean=10.0, fan=22.0)
    P.fringe(b, hair, n=9, span=8.4, y_top=31.6, length=3.8, seed=4, rot=10.0, part=0.4)
    P.long_hair(b, hair, y_top=31.4, y_split=19.0, tail=9.0, tiers=2, count=5, width=8.8, seed=6, sway=2.6)
    P.side_locks(b, hair, y_top=29.4, length=10.0, count=2, seed=7)
    R3.crown(b, GOLD, "#4cb8ff", spikes=5, y=32.45, tall=3.2, width=9.8)
    P.halo(b, y=37.8, r=7.4, color="#fff2a8", tilt=-8.0, spin=18.0, rings=2)
    P.mantle(b, "#f6f4fa", "#c8c4d8", y=21.9, thick=2.5, width=16.4, depth=6.8, drop=2.8, seed=10)
    P.bracers(b, False, y_top=16.4, rings=3, gold=GOLD, gem="#4cb8ff", tassel="#cdd8ff")
    P.skirt(b, "#e6eeff", GOLD, y_top=13.4, length=10.0, layers_=2, seed=12, radius=4.9, panels=8)


def face_lucius():
    return dict(skin=PALE, hair="#241e2c", hairline=1.9,
                eyes=dict(iris=("#5a0a14", "#d02a34", "#ff9a9a"), glow="#ff4a54", cy=4.95, h=1.95, tilt=0.20, lash_w=0.42),
                brow=dict(angle=0.9, thick=0.27), mouth=dict(kind="smirk", color="#8a4a58", lip="#c48088"),
                marks=[lambda f: S.mark_sun(f, cx=2.2, cy=1.25, r=0.5, glow="#802030")])


def face_lucius_divine():
    return dict(skin=("#e4d6dc", "#f6eaec", "#fffafa"), hair="#f2eef8", hairline=1.9,
                eyes=dict(iris=("#8a5a10", "#f0c040", "#fff6c0"), glow="#ffd870", cy=4.9, h=2.25, w=2.7, tilt=0.0, lash="#8a6a20", lash_w=0.36, hi=1.0, pupil="#fff2b0"),
                brow=dict(color="#cfc8d8", angle=0.0, thick=0.22, arch=0.14), mouth=dict(kind="flat", color="#b87a7a", lip="#e0a8a0"), blush=0.25, blush_color="#f0b0b0",
                marks=[lambda f: S.mark_sun(f, cx=2.2, cy=1.25, r=0.5, color="#c89a30", glow="#ffd870"),
                       lambda f: [f.polyline([(7.4 - 0.3 * i, 5.9 + 0.55 * i), (6.0, 6.0 + 0.55 * i + 0.1)], "#f0c040", 0.14, a=0.9, glow="#ffd870") for i in range(3)]])


# ================================================================================================ Dante Zogratis
def dante(b):
    hair = "#14101a"
    P.hair_shell(b, hair, seed=11, front_low=31.0, side_low=28.0, back_low=24.6, thick=1.0)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.6, shrink=1.4, h=1.2, seed=12, step_back=0.4)
    P.long_hair(b, hair, y_top=31.4, y_split=22.0, tail=3.5, tiers=1, count=6, width=8.4, seed=13, sway=1.2)
    R3.crown(b, GOLD, RED, spikes=5, y=32.55, tall=4.8, width=9.9, point_w=1.8)
    P.cape(b, COAT, "#0c1430", length=22.0, y_top=23.4, width=9.2, tiers=3, trim=None, fur="#ece8f2", seed=14, hem="ragged")
    P.mantle(b, "#f0eef4", "#c4c0d2", y=22.2, thick=2.2, width=16.2, depth=6.6, drop=2.4, seed=15)
    Q.belts_x(b, "#14101a", GOLD)


def dante_devil(b):
    hair = "#1a1224"
    P.hair_shell(b, hair, seed=11, front_low=31.0, side_low=28.0, back_low=24.6)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.6, shrink=1.4, h=1.2, seed=12, step_back=0.4)
    R3.crown(b, GOLD, RED, spikes=5, y=32.55, tall=4.8, width=9.9, point_w=1.8)
    P.horns(b, "#17101f", "#7a5ab0", base=(3.3, 31.8, -1.2), segs=5, length=3.2, w0=2.7, w1=0.8, curl=11.0, out=40.0)
    P.horns(b, "#17101f", "#7a5ab0", base=(1.2, 32.6, -4.3), segs=3, length=2.6, w0=1.5, w1=0.5, curl=7.0, out=18.0)
    R3.wing(b, "#1a1228", "#6a4a9a", "#9a7ad0", span=24.0, y=21.5)
    P.cape(b, "#140e24", "#0a0614", length=21.0, y_top=23.4, width=9.2, tiers=3, trim=None, fur="#2a2038", seed=14, hem="ragged")
    P.mantle(b, "#241c34", "#120c1e", y=22.2, thick=2.4, width=16.4, depth=6.8, drop=2.4, seed=15)
    Q.belts_x(b, "#0e0a14", "#9a7ad0")
    Q.pauldrons(b, "#241c34", "#120c1e", spikes=2, spike_len=3.2)


def face_dante(devil=False):
    marks = [lambda f: S.mark_band(f, 0.5, 1.3, "#0e0c12"), lambda f: S.mark_moustache(f)]
    eyes = dict(iris=("#0e3a1c", "#2e9a48", "#b8f0b0"), cy=4.95, h=1.95, tilt=0.12, glow="#4aff6a", lash_w=0.42)
    if devil:
        marks = [lambda f: S.mark_lines(f, glow="#9a5aff"), lambda f: S.mark_moustache(f)]
        eyes = dict(iris=("#6a3a00", "#f0a020", "#ffe890"), cy=4.95, h=1.95, tilt=0.12, glow="#ffc040", slit=True, pupil="#10060a", lash_w=0.42, dark_sclera="#1a1018")
    return dict(skin=FAIR if not devil else ASH, hair="#14101a", hairline=1.7, eyes=eyes, brow=dict(angle=0.7, thick=0.28), mouth=dict(kind="flat", color="#7a3a3a", lip="#a86060"), marks=marks)


# ================================================================================================ Vanica Zogratis
def vanica(b):
    hair = "#221a20"
    P.hair_shell(b, hair, seed=21, front_low=30.6, side_low=27.0, back_low=26.4)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.8, shrink=1.6, h=1.2, seed=22)
    P.bob_hair(b, hair, y_top=31.4, y_low=24.2, seed=23, count=6)
    P.fringe(b, hair, n=8, span=8.4, y_top=31.6, length=3.4, seed=24, rot=12.0, part=0.0)
    R3.crown(b, GOLD, RED, spikes=5, y=32.45, tall=3.4, width=9.7, point_w=1.5, band_h=1.0)
    P.cape(b, COAT, "#0c1430", length=14.0, y_top=23.4, width=8.6, tiers=2, trim=None, fur="#ece8f2", seed=25, hem="ragged")
    P.mantle(b, "#f0eef4", "#c4c0d2", y=22.2, thick=2.0, width=15.6, depth=6.4, drop=2.2, seed=26)
    Q.belts_x(b, "#2a1a14", GOLD)
    Q.pouch(b)


def vanica_devil(b):
    hair = "#221a20"
    P.hair_shell(b, hair, seed=21, front_low=30.6, side_low=27.0, back_low=26.4)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.8, shrink=1.6, h=1.2, seed=22)
    P.bob_hair(b, hair, y_top=31.4, y_low=24.2, seed=23, count=6)
    P.fringe(b, hair, n=8, span=8.4, y_top=31.6, length=3.4, seed=24, rot=12.0)
    P.fringe(b, "#d8d0e8", n=3, span=3.6, y_top=31.6, length=4.0, seed=27, rot=8.0, part=-3.0, layer="fine")     # the right side of her hair lightens
    P.horns(b, "#17101f", "#d8d0e8", base=(2.4, 32.2, -1.0), segs=6, length=3.4, w0=2.6, w1=0.8, curl=14.0, out=24.0, back=1.5)
    R3.crown(b, GOLD, RED, spikes=5, y=32.45, tall=3.0, width=9.7, point_w=1.4, band_h=1.0, layer="main")
    R3.wing(b, "#1c1226", "#7a3a6a", "#c85a9a", span=22.0, y=21.5)
    P.tail(b, "#1c1226", "#c8283a", segs=8, length=24.0, spade="heart", y=13.4, z=2.2, w0=1.9, w1=0.6, tip_size=6.0)
    P.cape(b, "#140e24", "#0a0614", length=14.0, y_top=23.4, width=8.6, tiers=2, trim=None, fur="#2a2038", seed=25, hem="ragged")
    P.mantle(b, "#241c34", "#120c1e", y=22.2, thick=2.2, width=15.8, depth=6.4, drop=2.2, seed=26)
    Q.belts_x(b, "#14101a", "#c85a9a")


def face_vanica(devil=False):
    eyes = dict(iris=("#3a1a14", "#9a5a30", "#f0c890"), cy=4.95, h=2.15, w=2.6, tilt=0.06, skip=(0,), lash_w=0.38)
    marks = [lambda f: S.mark_eyepatch(f)]
    mouth = dict(kind="smirk", color="#a84a50", lip="#d8847a")
    if devil:
        eyes = dict(iris=("#6a0a2a", "#d82a6a", "#ffa0c8"), cy=4.95, h=2.1, w=2.6, tilt=0.06, skip=(0,), glow="#ff4a9a", slit=True, lash_w=0.38)
        marks = [lambda f: S.mark_eyepatch(f), lambda f: S.mark_lines(f, xs=(0.9, 1.5, 2.1), y0=3.6, y1=7.4, color="#2a0a1a", glow="#ff4a9a"), lambda f: S.mark_tear(f, 5.4, 5.8, 7.5, "#7a1a3a", "#ff4a9a")]
        mouth = dict(kind="fang", color="#a84a50", lip="#d8847a")
    return dict(skin=("#dcb8a0", "#f2d4bc", "#ffeee0"), hair="#221a20", hairline=1.8, eyes=eyes, brow=dict(angle=0.5, thick=0.24, skip=(0,)), mouth=mouth, marks=marks, blush=0.3)


# ================================================================================================ Zenon Zogratis
def zenon(b):
    hair = "#1a1618"
    P.hair_shell(b, hair, seed=31, front_low=30.4, side_low=28.4, back_low=25.4)
    P.hair_tiers(b, hair, n=2, y0=33.3, w0=8.8, shrink=1.6, h=1.3, seed=32)
    P.hair_spikes(b, hair, n=6, y=34.3, spread=3.2, length=4.8, w=3.0, seed=33, rows=2, back_lean=-4.0, fan=40.0)
    P.fringe(b, hair, n=9, span=8.4, y_top=31.6, length=3.6, seed=34, rot=14.0)
    P.cape(b, COAT, "#0c1430", length=21.0, y_top=23.4, width=9.2, tiers=3, trim=None, fur="#b8b8c4", seed=35, hem="ragged", emblem="spade", emblem_color="#14101a")
    P.mantle(b, "#b8b8c4", "#8a8a98", y=22.2, thick=2.2, width=16.2, depth=6.6, drop=2.4, seed=36)


def zenon_devil(b):
    hair = "#14101a"
    bone = "#e6e0d0"
    P.hair_shell(b, hair, seed=31, front_low=30.4, side_low=28.4, back_low=25.4)
    P.hair_tiers(b, hair, n=2, y0=33.3, w0=8.8, shrink=1.6, h=1.3, seed=32)
    P.hair_spikes(b, hair, n=5, y=34.3, spread=3.0, length=4.4, w=3.0, seed=33, rows=2, back_lean=-4.0, fan=40.0)
    P.horns(b, "#17101f", "#c8c0b8", base=(3.1, 31.8, -0.8), segs=5, length=3.8, w0=2.6, w1=0.8, curl=8.0, out=24.0)
    P.horns(b, "#17101f", "#c8c0b8", base=(1.5, 32.6, -4.3), segs=3, length=2.6, w0=1.5, w1=0.5, curl=6.0, out=14.0)
    Q.bone_helmet(b, bone, "#8a8070", side=1)
    R3.wing(b, "#14101a", "#6a6070", "#b0a8b8", span=24.0, y=21.5)
    Q.back_spikes(b, bone, n=4, y=22.0, length=9.0)
    Q.ribcage(b, bone, "#8a8070")
    Q.pauldrons(b, bone, "#8a8070", spikes=3)
    P.mantle(b, "#2a2630", "#14101a", y=22.2, thick=2.4, width=16.4, depth=6.8, drop=2.4, seed=36)
    P.tail(b, "#14101a", "#c8c0b8", segs=8, length=26.0, spade="trident", y=13.4, z=2.2, w0=1.8, w1=0.5, tip_size=6.5)
    P.leg_bands(b, False, gold=bone, y=2.4, rings=2)


def face_zenon(devil=False):
    eyes = dict(iris=("#2a1a14", "#7a5a40", "#e0c8a0"), cy=4.95, h=1.95, tilt=0.14, lash_w=0.42)
    marks = [lambda f: S.mark_cross(f)]
    if devil:
        eyes = dict(iris=("#6a0a0a", "#e03030", "#ffb0a0"), cy=4.95, h=1.95, tilt=0.14, glow="#ff4040", slit=True, lash_w=0.42, dark_sclera="#1a1018")
        marks = [lambda f: S.mark_cross(f, cx=5.65, arm=2.4, glow="#ff4040"), lambda f: S.mark_cross(f, cx=2.35, cy=3.0, arm=0.9, down=2.2)]
    return dict(skin=FAIR if not devil else ASH, hair="#1a1618", hairline=1.7, eyes=eyes, brow=dict(angle=0.8, thick=0.28), mouth=dict(kind="flat", color="#7a3a3a", lip="#a86060"), marks=marks)


# ================================================================================================ Zagred
def zagred(b):
    hair = "#16121e"
    P.hair_shell(b, hair, seed=41, front_low=30.6, side_low=28.0, back_low=25.0)
    P.hair_tiers(b, hair, n=2, y0=33.3, w0=8.8, shrink=1.6, h=1.3, seed=42)
    P.hair_spikes(b, hair, n=6, y=34.3, spread=3.2, length=6.4, w=3.0, seed=43, rows=2, back_lean=22.0, fan=30.0)
    P.fringe(b, hair, n=7, span=8.0, y_top=31.6, length=3.0, seed=44, rot=14.0)
    P.horns(b, "#120c18", "#9a7ac0", base=(3.2, 31.4, -0.6), segs=5, length=3.2, w0=2.2, w1=0.7, curl=8.0, out=48.0)
    Q.pauldrons(b, "#1c1426", "#0e0a14", spikes=2, spike_len=3.4)
    P.tail(b, "#14101c", "#c84a6a", segs=7, length=20.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.5)
    P.collar(b, "#14101c", "#8a5ab0")


def zagred_devil(b):
    hair = "#0e0a14"
    P.hair_shell(b, hair, seed=41, front_low=30.6, side_low=28.0, back_low=25.0)
    P.hair_tiers(b, hair, n=2, y0=33.3, w0=8.8, shrink=1.6, h=1.3, seed=42)
    P.hair_spikes(b, hair, n=6, y=34.3, spread=3.2, length=6.8, w=3.0, seed=43, rows=2, back_lean=24.0, fan=32.0)
    P.fringe(b, hair, n=7, span=8.0, y_top=31.6, length=3.0, seed=44, rot=14.0)
    P.horns(b, "#0c0812", "#9a7ac0", base=(3.2, 31.4, -0.6), segs=6, length=3.6, w0=2.6, w1=0.8, curl=9.0, out=46.0)
    P.horns(b, "#0c0812", "#9a7ac0", base=(1.6, 32.4, -3.8), segs=3, length=2.6, w0=1.5, w1=0.5, curl=6.0, out=16.0)
    R3.wing(b, "#0e0a14", "#5a3a8a", "#9a6ad0", span=28.0, y=21.5)
    P.tail(b, "#0e0a14", "#e0508a", segs=9, length=28.0, spade="arrow", y=13.4, z=2.2, w0=2.0, w1=0.6, tip_size=6.5)
    Q.pauldrons(b, "#1c1426", "#0e0a14", spikes=3, spike_len=4.0)
    P.collar(b, "#0e0a14", "#9a6ad0")


def face_zagred(devil=False):
    eyes = dict(iris=("#6a0a20", "#e02a50", "#ffc0d0"), cy=4.95, h=1.95, tilt=0.16, glow="#ff4a8a", slit=True, pupil="#ff9ac0", lash_w=0.42, dark_sclera="#14101c")
    marks = [lambda f: S.mark_lines(f, xs=(1.4, 2.6, 5.4, 6.6), y0=1.8, y1=7.4, color="#14101c", a=0.8, w=0.1)]
    if devil:
        marks = [lambda f: S.mark_lines(f, xs=(0.9, 1.7, 2.5, 5.5, 6.3, 7.1), y0=0.4, y1=7.6, color="#0c0812", a=0.9, glow="#c84aff", w=0.13)]
    return dict(skin=("#c8bcc8", "#e0d4e0", "#f4ecf4"), hair="#16121e", hairline=1.8, eyes=eyes, brow=dict(angle=1.0, thick=0.26), mouth=dict(kind="teeth_dark", w=2.6), marks=marks)


# ================================================================================================ Lucifero
def lucifero(b):
    hair = "#ece8f6"
    P.hair_shell(b, hair, seed=51, front_low=30.6, side_low=26.0, back_low=22.0)
    P.hair_tiers(b, hair, n=3, y0=33.3, w0=9.0, shrink=1.8, h=1.3, seed=52)
    P.fringe(b, hair, n=9, span=8.4, y_top=31.6, length=4.2, seed=54, rot=14.0, part=0.3)
    P.long_hair(b, hair, y_top=31.4, y_split=19.0, tail=18.0, tiers=3, count=7, width=9.0, seed=56, sway=3.0)
    P.side_locks(b, hair, y_top=29.4, length=14.0, count=2, seed=57)
    P.horns(b, "#17101f", "#8a6ab0", base=(3.1, 31.8, -0.6), segs=6, length=3.0, w0=2.6, w1=0.8, curl=13.0, out=36.0)
    P.horns(b, "#17101f", "#8a6ab0", base=(4.1, 28.6, -0.6), segs=5, length=2.6, w0=2.2, w1=0.7, curl=12.0, out=62.0)
    P.elf_ears(b, "#c8bcc8", length=3.2)
    R3.wing(b, "#1a1228", "#6a4a9a", "#9a7ad0", span=30.0, y=22.0)
    R3.wing(b, "#16102a", "#5a3a8a", "#8a6ac0", span=22.0, y=18.0, pair=2, z=2.6)
    P.skirt(b, "#1c1426", "#8a5ab0", y_top=13.6, length=13.0, layers_=2, seed=58, radius=4.9, panels=8)
    P.collar(b, "#1c1426", "#8a5ab0")


def lucifero_form2(b):
    hair = "#f2eefa"
    P.hair_shell(b, hair, seed=51, front_low=30.6, side_low=26.0, back_low=22.0)
    P.hair_tiers(b, hair, n=3, y0=33.3, w0=9.0, shrink=1.8, h=1.3, seed=52)
    P.fringe(b, hair, n=9, span=8.4, y_top=31.6, length=4.2, seed=54, rot=14.0, part=0.3)
    P.long_hair(b, hair, y_top=31.4, y_split=19.0, tail=18.0, tiers=3, count=7, width=9.0, seed=56, sway=3.0)
    P.side_locks(b, hair, y_top=29.4, length=14.0, count=2, seed=57)
    P.horns(b, "#17101f", "#c8a0ff", base=(3.1, 31.8, -0.6), segs=6, length=3.2, w0=3.2, w1=1.0, curl=13.0, out=36.0)
    P.horns(b, "#17101f", "#c8a0ff", base=(4.1, 28.6, -0.6), segs=5, length=2.8, w0=2.8, w1=0.9, curl=12.0, out=62.0)
    P.elf_ears(b, "#c8bcc8", length=3.2)
    R3.wing(b, "#1a1228", "#6a4a9a", "#b890ff", span=30.0, y=22.0)
    R3.wing(b, "#16102a", "#5a3a8a", "#a07ae0", span=22.0, y=18.0, pair=2, z=2.6)
    P.skirt(b, "#1c1426", "#b890ff", y_top=13.6, length=7.0, layers_=2, seed=58, radius=4.9, panels=8)
    Q.pauldrons(b, "#1c1426", "#0e0a14", spikes=3, spike_len=4.6)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#3a2a50", gem="#c8a0ff", tassels=False)
    P.collar(b, "#1c1426", "#b890ff")
    P.halo(b, y=37.0, r=6.4, color="#c8a0ff", tilt=-6.0, spin=14.0, rings=2, layer="fx")


def face_lucifero(form2=False):
    eyes = dict(iris=("#6a0a20", "#e02a50", "#ffc0d0"), cy=4.95, h=1.95, tilt=0.16, glow="#ff4a8a", slit=True, pupil="#10060a", lash_w=0.42, dark_sclera="#1a1018")
    marks = [S.mark_wrinkles] if not form2 else [lambda f: S.mark_third_eye(f)]
    return dict(skin=("#bcb0bc", "#d8ccd8", "#efe6ef"), hair="#ece8f6", hairline=1.6, eyes=eyes, brow=dict(angle=1.0, thick=0.26), mouth=dict(kind="teeth_dark", w=2.6), marks=marks)


CHARS = [
    dict(id="lucius_zogratis", face=face_lucius(), build=lucius),
    dict(id="lucius_zogratis_form2", face=face_lucius_divine(), build=lucius_divine),
    dict(id="dante_zogratis", face=face_dante(), build=dante),
    dict(id="dante_zogratis_form2", face=face_dante(True), build=dante_devil, keep_extras=False),
    dict(id="vanica_zogratis", face=face_vanica(), build=vanica),
    dict(id="vanica_zogratis_form2", face=face_vanica(True), build=vanica_devil, keep_extras=False),
    dict(id="zenon_zogratis", face=face_zenon(), build=zenon),
    dict(id="zenon_zogratis_form2", face=face_zenon(True), build=zenon_devil, keep_extras=False),
    dict(id="zagred", face=face_zagred(), build=zagred),
    dict(id="zagred_form2", face=face_zagred(True), build=zagred_devil, keep_extras=False),
    dict(id="zagred_legacy", face=face_zagred(), build=zagred),
    dict(id="zagred_legacy_form2", face=face_zagred(True), build=zagred_devil, keep_extras=False),
    dict(id="lucifero", face=face_lucifero(), build=lucifero, keep_extras=False),
    dict(id="lucifero_form2", face=face_lucifero(True), build=lucifero_form2, keep_extras=False),
]
