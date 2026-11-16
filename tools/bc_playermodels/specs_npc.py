"""The Black Clover NPC and Midnight Sun characters of the redesigned player models (docs/npcs_bc.md).  Read by gen_bc.py (`run`), written to looks_bc_npc.json and stats_npc.json.

Sources (rule 10): the English Black Clover Fandom wiki, Appearance section of each character's page, fetched with curl through the MediaWiki api.php in this session (Rhya, Fana, Patolli, Rades
Spirito, Asta, Yuno, Noelle Silva, Yami Sukehiro, Finral Roulacase, Magna Swing, Luck Voltia, Charmy Pappitson, Vanessa Enoteca, Gordon Agrippa, Zora Ideale, Gauche Adlai, Grey, Secre
Swallowtail, Nacht Faust, Julius Novachrono, Marx Francois, Mimosa Vermillion, Klaus Lunettes, Leopold, Fuegoleon and Mereoleona Vermillion, Charlotte Roselei, Nozel and Solid Silva, Jack the Ripper,
Dorothy Unsworth, William Vangeance, Kahono, Kiato).  The Japanese wiki was not needed.  The skin tone of each face is sampled from the character's existing HD skin, so the head matches the arms.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import bclib as L  # noqa: E402
import bcparts as P  # noqa: E402
import bcparts2 as Q  # noqa: E402
import bcskin as S  # noqa: E402
import parts_npc as N  # noqa: E402

LOOKS_NPC = os.path.join(L.ASSETS, "playermodel", "looks_bc_npc.json")
STATS_NPC = os.path.join(HERE, "stats_npc.json")


# ================================================================================================ colours and faces
def _skin_of(stem):
    """(dark, mid, light) of the character's own head skin: the chin and cheek texels of the existing 256 skin."""
    im = np.asarray(Image.open(os.path.join(S.ASSET_TEX, stem + ".png")).convert("RGBA"), np.float32) / 255.0
    k = im.shape[0] / 64.0                                      # the kit's 64 layout, 1 or 4 texels per pixel: the head front is u 8..16, v 8..16; chin and cheeks are rows 6.25-7.5 of it
    ys, xs = slice(int((8 + 6.25) * k), int((8 + 7.5) * k)), slice(int((8 + 1.75) * k), int((8 + 6.25) * k))
    patch = im[ys, xs].reshape(-1, 4)
    patch = patch[patch[:, 3] > 0.5][:, :3]
    mid = np.median(patch, axis=0) if len(patch) else np.array([0.85, 0.66, 0.54], np.float32)
    return (L.hexs(L.tone(mid, 0.82)), L.hexs(mid), L.hexs(L.tone(mid, 1.16))), L.hexs(mid)


def iris(c):
    """An iris triple (dark rim, colour, light core) from one colour."""
    return (L.hexs(L.tone(c, 0.42)), L.hexs(L.rgb(c)), L.hexs(L.tone(c, 1.55)))


def face(stem, hair, eye, glow=None, hairline=1.7, brow=None, mouth=None, marks=(), pre_eye=(), blush=0.0, skin_mid=None, **eyes):
    sk, _ = _skin_of(stem)
    if skin_mid:                                                 # the old 64x64 skins of a few characters have no readable cheek: the tone is given
        sk = (L.hexs(L.tone(skin_mid, 0.82)), L.hexs(L.rgb(skin_mid)), L.hexs(L.tone(skin_mid, 1.16)))
    e = dict(iris=iris(eye), cy=4.95, h=2.0, w=2.55, tilt=0.10, lash_w=0.40)
    if glow:
        e["glow"] = glow
    e.update(eyes)
    spec = dict(skin=sk, hair=hair, hairline=hairline, eyes=e, brow=dict(angle=0.3, thick=0.26, color=L.hexs(L.tone(hair, 0.7))), mouth=mouth or dict(kind="flat", color="#8a4a4a", lip="#c0807a"), marks=list(marks), pre_eye=list(pre_eye))
    if brow is not None:
        spec["brow"] = dict(spec["brow"], **brow)
    if blush:
        spec["blush"] = blush
    return spec


def ears_color(stem):
    return _skin_of(stem)[1]


# ================================================================================================ face marks (model pixels of the 8x8 front, x 0..8 as the viewer sees it)
def m_slashes(cx=6.3, cy=6.2, color="#c81e2a", n=3, glow=None):
    def f_(f):
        for i in range(n):
            x = cx - 0.55 + i * 0.55
            f.line((x - 0.1, cy - 0.8), (x + 0.3, cy + 0.8), color, 0.2, 0.1, a=0.95)
            if glow:
                f.glow_fill(f.m_line((x - 0.1, cy - 0.8), (x + 0.3, cy + 0.8), 0.12, 0.06), glow, 0.6)
    return f_


def m_eye_markings(color="#b81c28"):
    def f_(f):
        for cx, s in ((2.35, -1), (5.65, 1)):
            f.polyline([(cx - s * 1.5, 4.1), (cx - s * 0.6, 3.75), (cx + s * 0.9, 3.9), (cx + s * 1.55, 4.6)], color, 0.13, a=0.9)
            f.polyline([(cx - s * 1.0, 6.05), (cx, 6.3), (cx + s * 1.1, 6.0)], color, 0.11, a=0.8)
    return f_


def m_diamond(cx=4.0, cy=1.3, r=0.5, color="#b81c28", glow=None):
    def f_(f):
        f.poly([(cx, cy - r), (cx + r * 0.8, cy), (cx, cy + r), (cx - r * 0.8, cy)], color, a=0.95)
        if glow:
            f.glow_fill(f.m_ellipse(cx, cy, r * 0.5, r * 0.6), glow, 0.7)
    return f_


def m_star6(cx=5.8, cy=1.3, r=0.62, color="#3a68d0", glow=None):
    def f_(f):
        f.poly([(cx, cy - r), (cx + r * 0.87, cy + r * 0.5), (cx - r * 0.87, cy + r * 0.5)], color, a=0.95)
        f.poly([(cx, cy + r), (cx + r * 0.87, cy - r * 0.5), (cx - r * 0.87, cy - r * 0.5)], color, a=0.95)
        if glow:
            f.glow_fill(f.m_ellipse(cx, cy, r * 0.5, r * 0.5), glow, 0.7)
    return f_


def m_scar(pts, color="#b82a34", w=0.17, glow=None):
    def f_(f):
        f.polyline(list(pts), color, w, a=0.95)
        f.polyline([(x + 0.12, y) for x, y in pts], "#f0c0b0", w * 0.5, a=0.35)
        if glow:
            f.glow_fill(f.m_line(pts[0], pts[-1], 0.1, 0.1), glow, 0.4)
    return f_


def m_straps(color="#14101a", stud="#b8b8c8"):
    """Zora's straps: black leather bands over the lower face, an opening for the mouth, studs."""
    def f_(f):
        f.fill(f.m_rect(0.0, 5.75, 8.0, 6.3, soft=1.0), color, 0.97)
        f.fill(f.m_rect(0.0, 7.25, 8.0, 8.0, soft=1.0), color, 0.97)
        f.fill(f.m_rect(0.0, 6.3, 2.7, 7.25, soft=1.0), color, 0.97)
        f.fill(f.m_rect(5.3, 6.3, 8.0, 7.25, soft=1.0), color, 0.97)
        f.fill(f.m_rect(2.7, 6.3, 5.3, 7.25, soft=1.0), "#4a1a1a", 0.9)
        for i in range(4):
            f.poly([(2.95 + i * 0.62, 6.3), (3.5 + i * 0.62, 6.3), (3.22 + i * 0.62, 6.85)], "#fffaf0", 1.0)
        for x, y in ((6.6, 5.95), (7.2, 7.6), (1.0, 7.6), (1.5, 7.6), (6.1, 7.6)):
            f.ellipse(x, y, 0.17, 0.17, stud, a=1.0)
    return f_


def m_bags(color="#5a4a6a"):
    def f_(f):
        for cx in (2.35, 5.65):
            f.polyline([(cx - 1.15, 5.95), (cx, 6.25), (cx + 1.15, 5.95)], color, 0.2, a=0.75)
    return f_


def m_eyeshadow(color="#10080e", a=0.7):
    def f_(f):
        for cx in (2.35, 5.65):
            f.ellipse(cx, 4.95, 1.95, 1.75, color, a=a)
    return f_


def m_mirror(cx=5.65, cy=4.95):
    """Gauche's left eye, a small magical mirror."""
    def f_(f):
        f.ellipse(cx, cy, 1.15, 1.15, "#59487a", a=1.0)
        f.ellipse(cx, cy, 0.95, 0.95, "#cfd6ee", a=1.0)
        f.fill(f.m_ellipse(cx - 0.2, cy - 0.25, 0.55, 0.4, soft=2.0), "#ffffff", 0.7)
        f.glow_fill(f.m_ellipse(cx, cy, 0.8, 0.8), "#9ab0ff", 0.5)
    return f_


def m_magna():
    def f_(f):
        f.line((5.4, 0.7), (5.4, 2.7), "#14101a", 0.2, 0.2, a=0.95)
        for y in (1.0, 1.7, 2.4):
            for dx in (-0.45, 0.45):
                f.ellipse(5.4 + dx, y, 0.11, 0.11, "#14101a", a=0.95)
    return f_


def m_crescents(color="#c81e2a"):
    """Patolli's red crescent tattoos at the sides of the eyes (three buds each) and the two red dots that stand for brows."""
    def f_(f):
        for cx, s in ((2.35, -1), (5.65, 1)):
            f.polyline([(cx + s * 1.55, 3.9), (cx + s * 1.85, 4.9), (cx + s * 1.5, 5.9)], color, 0.14, a=0.95)
            for y in (4.1, 4.9, 5.7):
                f.ellipse(cx + s * 1.95, y, 0.12, 0.12, color, a=0.95)
            f.ellipse(cx, 3.3, 0.26, 0.26, color, a=1.0)
    return f_


def m_stubble(color="#2a2220"):
    def f_(f):
        for i in range(34):
            x = 1.2 + 5.6 * L.hsh(i, 3)
            y = 6.5 + 1.4 * L.hsh(i, 5)
            if abs(x - 4.0) > 0.9 or y > 7.2:
                f.ellipse(x, y, 0.1, 0.1, color, a=0.42)
    return f_


def m_stone_frame(color="#c81e2a", cx=4.0, cy=1.2):
    """Fana's red tattoo round the forehead stone and the third eye below it."""
    def f_(f):
        f.polyline([(cx, cy - 0.95), (cx + 0.9, cy), (cx, cy + 0.95), (cx - 0.9, cy), (cx, cy - 0.95)], color, 0.13, a=0.95)
        f.ellipse(cx, 2.55, 0.5, 0.3, "#2a1018", a=1.0)
        f.ellipse(cx, 2.55, 0.28, 0.2, "#e8e0e8", a=0.95)
        f.glow_fill(f.m_ellipse(cx, 2.55, 0.4, 0.25), "#ff4a6a", 0.6)
    return f_


def m_tattoo_up(color="#c81e2a", glow=None):
    """Rhya's tattoo after the second reincarnation: it runs down the jaw and up over the eye."""
    def f_(f):
        f.polyline([(6.9, 2.6), (7.2, 4.0), (7.0, 5.6), (6.4, 7.2)], color, 0.2, a=0.95)
        f.polyline([(6.4, 3.4), (6.0, 3.9), (5.3, 3.8)], color, 0.16, a=0.95)
        if glow:
            f.glow_fill(f.m_line((7.2, 4.0), (6.4, 7.2), 0.12, 0.06), glow, 0.6)
    return f_


# ================================================================================================ hair recipes
def base(b, color, seed, front=30.6, side=28.4, back=25.2, tiers=1, y0=33.3, w0=8.8, thick=1.0, step_back=0.0):
    P.hair_shell(b, color, seed=seed, front_low=front, side_low=side, back_low=back, thick=thick)
    if tiers:
        P.hair_tiers(b, color, n=tiers, y0=y0, w0=w0, shrink=1.6, h=1.3, seed=seed + 1, step_back=step_back)


def spikes(b, color, seed, n=7, length=5.0, rows=2, lean=14.0, fan=30.0, w=2.6, y=34.0, tip=None, layer="main"):
    P.hair_spikes(b, color, n=n, y=y, spread=3.0, length=length, w=w, seed=seed + 2, rows=rows, back_lean=lean, fan=fan, tip=tip, layer=layer)


def mohawk(b, color, tip=None, seed=150, n=7, length=7.0):
    geo = b.g("main")
    sp = b.t_spike(color, tw=12, th=24, seed=seed, tip=tip)
    for i in range(n):
        z = -3.2 + i * (7.0 / (n - 1.0))
        ln = length * (1.0 - 0.18 * abs(i - (n - 1) / 2.0) / 3.0) * (0.85 + 0.3 * L.hsh(i, seed))
        rot = (3.0 + 1.0 * i, 0.0, 0.0)
        geo.cube("head", (0.0, 33.0, z - 1.2), (0.0, ln, 2.4), sp, faces=("east",), rot=rot, pivot=(0.0, 33.0, z))
        geo.cube("head", (-1.1, 33.0, z), (2.2, ln * 0.9, 0.0), sp, faces=("north",), rot=rot, pivot=(0.0, 33.0, z))


# ================================================================================================ Midnight Sun
def rhya(b):
    black, white = "#1a1a26", "#eef0f8"
    base(b, black, 201, front=30.8, tiers=1)
    spikes(b, black, 201, n=7, length=5.2, lean=10.0, fan=36.0, y=33.8)
    P.hair_spikes(b, white, n=2, y=33.6, spread=2.4, length=4.4, w=2.4, seed=207, rows=1, back_lean=6.0, fan=40.0, z0=-0.6)
    P.fringe(b, black, n=9, span=8.4, y_top=31.6, length=3.4, seed=204, rot=14.0)
    P.fringe(b, white, n=2, span=3.0, y_top=31.6, length=3.8, seed=205, rot=8.0, part=-2.6)
    N.hood_down(b, "#d8d0bc", "#8a8070")


def rhya_f2(b, ear):
    white, black = "#f2f4fc", "#1a1a26"
    base(b, white, 211, front=30.8, tiers=1)
    spikes(b, white, 211, n=8, length=5.8, lean=8.0, fan=38.0, y=33.8)
    P.hair_spikes(b, black, n=3, y=33.6, spread=2.6, length=4.6, w=2.4, seed=217, rows=1, back_lean=6.0, fan=40.0, z0=-0.6)
    P.fringe(b, white, n=9, span=8.4, y_top=31.6, length=3.6, seed=214, rot=14.0)
    P.fringe(b, black, n=2, span=3.0, y_top=31.6, length=3.8, seed=215, rot=8.0, part=2.4)
    N.pointed_ears(b, ear)
    N.hood_down(b, "#d8d0bc", "#8a8070")


def fana(b):
    pink = "#e8709e"
    base(b, pink, 221, front=31.8, side=27.0, back=24.6, tiers=1, w0=8.8)
    P.fringe(b, pink, n=8, span=8.4, y_top=31.8, length=2.4, seed=224, rot=10.0, part=3.4)
    P.bob_hair(b, pink, y_top=31.4, y_low=23.8, seed=225, count=7)
    P.side_locks(b, pink, y_top=29.4, length=7.5, count=2, seed=226)
    N.forehead_stone(b, "#a84ae8")
    P.mantle(b, "#7a1a2a", "#4a0e18", y=21.9, thick=1.6, width=11.8, depth=6.2, drop=1.8, seed=227, collar_h=2.2)
    N.hood_down(b, "#f0ece0", "#c8b890")


def fana_f2(b, ear):
    pink = "#ff7a9e"
    base(b, pink, 231, front=31.8, side=27.0, back=24.6, tiers=2, w0=8.6)
    spikes(b, pink, 231, n=7, length=6.2, rows=2, lean=18.0, fan=44.0, y=34.0)
    P.fringe(b, pink, n=8, span=8.4, y_top=31.8, length=2.6, seed=234, rot=12.0, part=3.4)
    P.bob_hair(b, pink, y_top=31.4, y_low=23.4, seed=235, count=7)
    P.side_locks(b, pink, y_top=29.4, length=8.0, count=2, seed=236)
    N.forehead_stone(b, "#ff6ad0")
    N.pointed_ears(b, ear)
    P.mantle(b, "#9a1a38", "#5a0e22", y=21.9, thick=1.6, width=11.8, depth=6.2, drop=1.8, seed=237, collar_h=2.2)
    N.hood_down(b, "#f0ece0", "#c8b890")


def patolli(b, ear):
    white = "#f2efe6"
    base(b, white, 241, front=31.4, side=27.4, back=25.0, tiers=0)
    N.kippah(b)
    for i, x in enumerate((-2.2, 0.0, 2.2)):
        N.braid(b, white, x=x, y_top=30.5, z=4.9, segs=9, seg=2.3, w=1.8, tie_color="#c8a040", seed=243 + i, sway=2.2)
    N.earrings(b, "#e8b830", hoop=True, y=27.0)
    N.pointed_ears(b, ear)
    N.shawl(b, "#1c1c24", "#e8b830", ornament="#e8b830")


def patolli_f2(b, ear):
    white = "#f6f2ff"
    base(b, white, 251, front=31.4, side=27.4, back=25.0, tiers=0)
    N.kippah(b, "#5a4a7a", "#1c1428")
    for i, x in enumerate((-2.2, 0.0, 2.2)):
        N.braid(b, white, x=x, y_top=30.5, z=4.9, segs=9, seg=2.3, w=1.8, tie_color="#b890ff", seed=253 + i, sway=2.2)
    N.earrings(b, "#c8a0ff", hoop=True, y=27.0)
    N.pointed_ears(b, ear)
    N.shawl(b, "#14101c", "#b890ff", ornament="#b890ff")


def rades(b):
    pur = "#b890f0"
    base(b, pur, 261, front=30.4, side=26.0, back=23.0, tiers=1)
    spikes(b, pur, 261, n=6, length=4.0, lean=6.0, fan=40.0, y=33.6)
    P.fringe(b, pur, n=8, span=8.4, y_top=31.6, length=3.6, seed=264, rot=16.0, part=-1.0)
    P.long_hair(b, pur, y_top=31.4, y_split=19.0, tail=9.0, tiers=2, count=6, width=9.0, seed=266, sway=2.8)
    P.side_locks(b, pur, y_top=29.4, length=10.0, count=2, seed=267)
    N.eye_bandage(b)


def rades_f2(b, ear):
    pur = "#d8b8ff"
    base(b, pur, 271, front=30.4, side=25.0, back=22.0, tiers=2)
    spikes(b, pur, 271, n=8, length=5.4, lean=10.0, fan=46.0, y=34.0)
    P.fringe(b, pur, n=8, span=8.4, y_top=31.6, length=3.8, seed=274, rot=16.0, part=-1.0)
    P.long_hair(b, pur, y_top=31.4, y_split=19.0, tail=12.0, tiers=3, count=7, width=9.4, seed=276, sway=3.0)
    P.side_locks(b, pur, y_top=29.4, length=12.0, count=2, seed=277)
    N.pointed_ears(b, ear)
    N.eye_bandage(b, "#5a1a3a", "#8affc8")


# ================================================================================================ Black Bulls
def asta(b):
    g = "#d4d8e2"
    base(b, g, 301, front=31.6, side=28.6, back=25.4, tiers=1)
    spikes(b, g, 301, n=8, length=4.6, lean=4.0, fan=42.0, y=33.8, w=2.8)
    P.hair_spikes(b, g, n=1, y=34.4, spread=0.0, length=7.0, w=1.8, seed=309, rows=1, back_lean=4.0, fan=2.0, z0=-1.6)
    P.fringe(b, g, n=9, span=8.4, y_top=31.8, length=1.9, seed=304, rot=14.0)
    N.headband(b, "#14121a", "#e8b830", y=29.9)
    N.hood_down(b, "#14101a", "#e8b830")


def yami(b):
    k = "#15151d"
    base(b, k, 311, front=30.8, side=28.2, back=25.0, tiers=1)
    spikes(b, k, 311, n=8, length=6.4, lean=34.0, fan=40.0, y=33.8, w=2.8)
    P.fringe(b, k, n=8, span=8.4, y_top=31.6, length=2.6, seed=314, rot=18.0)
    N.banner(b)


def noelle(b):
    s = "#e0e5f2"
    base(b, s, 321, front=30.9, side=28.0, back=25.0, tiers=1)
    P.fringe(b, s, n=9, span=8.4, y_top=31.7, length=3.5, seed=324, rot=9.0, part=0.5)
    P.side_locks(b, s, y_top=30.0, length=5.0, count=1, seed=326)
    for sx in (-1, 1):
        N.hair_tail(b, s, x=sx * 5.7, y_top=30.6, z=-0.4, length=10.5, w=3.0, count=2, seed=328 + sx, sway=3.4, flare=14.0, head_only=True, layer="fine")
        N.tie(b, sx * 5.7, 30.6, -0.4, "#8a4ad0", 1.7)
    N.earrings(b, "#a860e8", y=27.2)
    N.hood_down(b, "#14101a", "#e8b830")


def finral(b):
    h, tipc = "#b98c52", "#6ac08a"
    base(b, h, 331, front=30.9, side=28.4, back=25.4, tiers=1)
    spikes(b, h, 331, n=6, length=3.6, lean=8.0, fan=34.0, y=33.6, tip=tipc)
    N.swept_fringe(b, h, side=1, y_top=31.7, n=6, length=4.8, seed=334)
    N.earrings(b, "#4ac080", y=27.0)
    N.hood_down(b, "#14101a", "#e8b830")


def magna(b):
    k = "#1a1a24"
    base(b, k, 341, front=31.2, side=26.8, back=24.8, tiers=0)
    P.hair_tiers(b, "#c8c8d4", n=1, y0=33.3, w0=6.4, shrink=1.0, h=1.0, seed=342)
    mohawk(b, k, tip="#c8c8d4")
    N.glasses(b, "#1c1c28", "#2a60c0")
    N.hood_down(b, "#14101a", "#e8b830")


def luck(b):
    y = "#f8d63c"
    base(b, y, 351, front=29.8, side=26.0, back=24.6, tiers=1)
    spikes(b, y, 351, n=7, length=3.8, lean=10.0, fan=36.0, y=33.6)
    P.fringe(b, y, n=9, span=8.4, y_top=31.8, length=3.8, seed=354, rot=16.0)
    P.side_locks(b, y, y_top=29.5, length=5.0, count=1, seed=356)
    N.hood_down(b, "#14101a", "#e8b830")


def charmy(b):
    k = "#2a2634"
    base(b, k, 361, front=31.8, side=28.0, back=25.0, tiers=0)
    N.bun(b, k, y=31.2, z=3.4, size=3.8, ribbon="#e8b830")
    P.side_locks(b, k, y_top=30.4, length=5.5, count=1, seed=366, z=-3.0)
    N.hood_down(b, "#14101a", "#e8b830")


def vanessa(b):
    p = "#f5559f"
    base(b, p, 371, front=30.8, side=26.0, back=23.4, tiers=1)
    N.swept_fringe(b, p, side=-1, y_top=31.7, n=7, length=5.0, seed=374)
    P.bob_hair(b, p, y_top=31.4, y_low=21.0, seed=375, count=7)
    P.side_locks(b, p, y_top=29.4, length=9.0, count=2, seed=376)
    N.witch_hat(b, "#a02a6a", "#6a2a9a", "#e8b830", brim=14.0, cone_h=9.0, droop=26.0)


def gordon(b):
    k = "#1b1b26"
    base(b, k, 381, front=32.0, side=27.0, back=24.0, tiers=0)
    N.cap(b)


def zora(b):
    r = "#d02e1a"
    base(b, r, 391, front=31.4, side=28.0, back=25.2, tiers=1)
    spikes(b, r, 391, n=9, length=7.4, lean=6.0, fan=40.0, y=33.8, w=2.6, tip="#ff9a50")
    N.hood_down(b, "#14101a", "#e8b830")


def gauche(b):
    k = "#8a6a46"
    base(b, k, 401, front=30.6, side=27.2, back=24.8, tiers=1)
    spikes(b, k, 401, n=7, length=3.4, lean=12.0, fan=36.0, y=33.6)
    N.swept_fringe(b, k, side=1, y_top=31.8, n=7, length=6.0, seed=404)
    P.fringe(b, k, n=4, span=4.4, y_top=31.8, length=3.2, seed=405, rot=10.0, part=-2.0)
    N.hood_down(b, "#14101a", "#e8b830")


def grey(b):
    c = "#a8d0f4"
    base(b, c, 411, front=30.8, side=26.4, back=24.0, tiers=1)
    P.fringe(b, c, n=9, span=8.4, y_top=31.7, length=3.6, seed=414, rot=11.0, part=0.4)
    P.bob_hair(b, c, y_top=31.4, y_low=24.4, seed=415, count=7)
    P.side_locks(b, c, y_top=29.8, length=6.0, count=1, seed=416)
    N.hood_down(b, "#14101a", "#e8b830")


def secre(b):
    k = "#26263a"
    base(b, k, 421, front=30.9, side=26.6, back=24.4, tiers=0)
    P.fringe(b, k, n=9, span=8.4, y_top=31.7, length=3.2, seed=424, rot=8.0)
    P.bob_hair(b, k, y_top=31.4, y_low=24.6, seed=425, count=7)
    N.antenna_locks(b, k, y=33.0, n=2, length=5.0)
    N.hood_down(b, "#14101a", "#e8b830")


def nacht(b):
    k = "#1c1c28"
    base(b, k, 431, front=30.9, side=28.2, back=25.4, tiers=1)
    P.fringe(b, k, n=9, span=8.4, y_top=31.7, length=3.2, seed=434, rot=11.0, part=0.6)
    N.hair_tail(b, k, x=0.0, y_top=31.4, z=5.6, length=15.0, w=3.2, count=3, seed=436, sway=3.4)
    N.tie(b, 0.0, 31.2, 4.7, "#3a68d0", 1.8)
    N.necklace(b, "#6a5a4a", "#9a9ae8")
    N.hood_down(b, "#6a6a78", "#3a3a44")


# ================================================================================================ Magic Knights
def julius(b):
    y = "#f4e6a6"
    base(b, y, 441, front=30.9, side=28.0, back=25.0, tiers=1)
    spikes(b, y, 441, n=8, length=4.6, lean=12.0, fan=40.0, y=33.8)
    P.fringe(b, y, n=9, span=8.4, y_top=31.7, length=3.6, seed=444, rot=16.0)
    P.mantle(b, "#f4f0ec", "#c8c4bc", y=21.9, thick=2.2, width=14.2, depth=6.4, drop=2.4, seed=447)
    N.necklace(b, "#e8b830", "#3a9a5a")


def marx(b):
    h = "#d9c79a"
    base(b, h, 451, front=30.6, side=27.0, back=25.8, tiers=1)
    P.fringe(b, h, n=9, span=8.6, y_top=31.7, length=2.8, seed=454, rot=3.0, side_bangs=False)
    P.mantle(b, "#ece8e0", "#bcb8b0", y=21.9, thick=2.2, width=14.0, depth=6.4, drop=2.6, seed=457)
    N.necklace(b, "#c8ccd8", "#e8b830")


def yuno(b):
    k = "#1a1a24"
    base(b, k, 461, front=31.0, side=27.8, back=25.0, tiers=1)
    spikes(b, k, 461, n=7, length=3.4, lean=12.0, fan=34.0, y=33.6)
    P.fringe(b, k, n=9, span=8.4, y_top=31.8, length=4.4, seed=464, rot=12.0, part=1.0)
    P.side_locks(b, k, y_top=30.0, length=5.5, count=1, seed=466)
    N.necklace(b, "#e8b830", "#3a68d0")
    N.hood_down(b, "#e8c868", "#c89a30", fur="#7ab0e8")


def mimosa(b):
    o = "#f06a3c"
    base(b, o, 471, front=30.8, side=27.0, back=24.4, tiers=1)
    P.fringe(b, o, n=9, span=8.4, y_top=31.8, length=3.6, seed=474, rot=9.0, part=0.3)
    P.long_hair(b, o, y_top=31.4, y_split=20.0, tail=8.0, tiers=2, count=6, width=9.0, seed=476, sway=2.6)
    P.side_locks(b, o, y_top=29.6, length=9.0, count=2, seed=477)
    N.earrings(b, "#d81c40", y=27.0)
    N.hood_down(b, "#e8c868", "#c89a30", fur="#7ab0e8")


def klaus(b):
    c = "#6fa4ee"
    base(b, c, 481, front=31.0, side=28.4, back=25.6, tiers=1)
    N.swept_fringe(b, c, side=1, y_top=31.8, n=6, length=4.2, seed=484)
    N.glasses(b, "#1c1c28", "#14141e")
    N.earrings(b, "#e8b830", hoop=True, y=27.0)
    N.hood_down(b, "#e8c868", "#c89a30", fur="#7ab0e8")


def _lion(b, braid_on=False):
    N.scarf(b)
    N.sash(b)


def leopold(b):
    v = "#de4a1e"
    base(b, v, 491, front=30.9, side=27.6, back=25.0, tiers=1)
    spikes(b, v, 491, n=9, length=5.6, lean=14.0, fan=44.0, y=33.8, w=2.8)
    P.fringe(b, v, n=9, span=8.4, y_top=31.7, length=3.2, seed=494, rot=16.0)
    N.braid(b, v, x=3.2, y_top=29.6, z=4.9, segs=6, seg=2.1, w=1.8, tie_color="#e8b830", seed=496, flare=0.0)
    N.scarf(b)


def fuegoleon(b):
    v = "#c2321c"
    base(b, v, 501, front=30.8, side=27.0, back=24.8, tiers=1)
    N.swept_fringe(b, v, side=1, y_top=31.8, n=7, length=5.6, seed=504)
    P.long_hair(b, v, y_top=31.4, y_split=22.0, tail=4.0, tiers=2, count=6, width=9.0, seed=506, sway=2.0)
    N.earrings(b, "#d81c40", y=27.0)
    N.scarf(b)
    N.sash(b)


def mereoleona(b):
    v = "#d4421c"
    base(b, v, 511, front=30.8, side=25.4, back=22.0, tiers=2)
    spikes(b, v, 511, n=8, length=5.0, lean=16.0, fan=46.0, y=33.8)
    P.fringe(b, v, n=9, span=8.4, y_top=31.7, length=3.6, seed=514, rot=16.0)
    P.long_hair(b, v, y_top=31.4, y_split=19.0, tail=14.0, tiers=3, count=7, width=9.4, seed=516, sway=3.0)
    P.side_locks(b, v, y_top=29.4, length=11.0, count=2, seed=517)
    N.scarf(b)
    N.sash(b)


def charlotte(b):
    g = "#e8d890"
    base(b, g, 521, front=30.8, side=28.0, back=25.6, tiers=1)
    N.swept_fringe(b, g, side=1, y_top=31.8, n=7, length=4.4, seed=524)
    N.bun(b, g, y=31.0, z=3.6, size=3.6, ribbon="#3a68d0")
    N.braid(b, g, x=-4.6, y_top=29.6, z=-1.4, segs=8, seg=1.9, w=1.7, tie_color="#3a68d0", seed=526)
    Q.pauldrons(b, "#c8ccd8", "#7a8098", spikes=0, size=5.4)
    N.gauntlets(b, True)
    N.collar_plate(b)


def nozel(b):
    s = "#f2f4ff"
    base(b, s, 531, front=31.0, side=28.0, back=25.2, tiers=1)
    spikes(b, s, 531, n=8, length=6.0, lean=36.0, fan=34.0, y=33.8, w=2.6)
    P.long_hair(b, s, y_top=31.4, y_split=21.0, tail=12.0, tiers=2, count=6, width=8.6, seed=536, sway=2.2)
    N.braid(b, s, x=0.0, y_top=31.7, z=-5.1, segs=4, seg=1.3, w=0.9, tie_color="#8fb4f4", seed=538, sway=1.2)
    P.mantle(b, "#e6e8f2", "#a8acc4", y=21.9, thick=2.2, width=14.2, depth=6.4, drop=2.4, seed=537)
    N.feather_panels(b, "#f2f4ff", "#3a3a54", y=19.4, n=6, length=6.0)


def solid(b):
    s = "#8fb4f4"
    base(b, s, 541, front=30.8, side=28.4, back=25.8, tiers=1)
    spikes(b, s, 541, n=7, length=3.6, lean=14.0, fan=40.0, y=33.6)
    P.fringe(b, s, n=9, span=8.4, y_top=31.8, length=3.8, seed=544, rot=14.0)
    N.braid(b, s, x=0.0, y_top=30.4, z=4.9, segs=9, seg=2.2, w=2.0, tie_color="#3a68d0", seed=546, sway=2.0)
    for i in range(2):
        b.g("fx").cube("head", (-4.55, 29.0 - i * 1.5, -1.4 + i * 1.0), (0.4, 1.0, 1.0), b.t_gem("#3a68d0"), faces=("east", "west"))
    P.mantle(b, "#e6e8f2", "#a8acc4", y=21.9, thick=2.2, width=14.2, depth=6.4, drop=2.4, seed=547)
    N.feather_panels(b, "#f2f4ff", "#3a3a54", y=19.4, n=6, length=6.0)


def jack(b):
    k = "#1c1c22"
    base(b, k, 551, front=30.8, side=26.8, back=24.6, tiers=1)
    spikes(b, k, 551, n=7, length=3.8, lean=14.0, fan=40.0, y=33.6)
    P.fringe(b, k, n=9, span=8.4, y_top=31.7, length=3.8, seed=554, rot=14.0, part=-1.0)
    P.side_locks(b, k, y_top=29.6, length=5.5, count=1, seed=556)
    Q.belts_x(b, "#14201a", "#8ac060", y0=19.2)
    N.hood_down(b, "#3a8a4a", "#1c4a28")


def dorothy(b):
    l = "#b090d0"
    base(b, l, 561, front=30.8, side=26.6, back=24.6, tiers=0)
    P.fringe(b, l, n=9, span=8.4, y_top=31.7, length=3.4, seed=564, rot=9.0)
    P.bob_hair(b, l, y_top=31.4, y_low=24.6, seed=565, count=7)
    N.witch_hat(b, "#6a4a9a", "#3a6a4a", "#f2d868", brim=12.6, cone_h=8.0, droop=18.0, flowers=["#f26aa0", "#f2d868", "#fff6f0", "#a8d868"])


def william(b):
    w = "#f4f5fb"
    base(b, w, 571, front=30.0, side=27.4, back=25.0, tiers=1)
    spikes(b, w, 571, n=6, length=3.2, lean=10.0, fan=34.0, y=33.6)
    N.half_mask(b)
    P.hair_tiers(b, "#c82a34", n=1, y0=33.4, w0=4.0, shrink=1.0, h=0.8, seed=574, step_back=3.0)
    for i, x in enumerate((1.6, 2.8)):
        sp = b.t_spike("#ffffff", tw=12, th=24, seed=575 + i)
        b.g("main").cube("head", (x - 0.8, 33.6, 0.0), (1.6, 5.0, 0.0), sp, faces=("north",), rot=(-6.0, 0.0, -10.0 - 8.0 * i), pivot=(x, 33.6, 0.0))
    N.necklace(b, "#e8b830", "#3a68d0")


def kahono(b):
    r = "#a02a30"
    base(b, r, 581, front=30.9, side=28.0, back=25.4, tiers=1)
    P.fringe(b, r, n=9, span=8.4, y_top=31.7, length=3.2, seed=584, rot=10.0)
    N.hair_tail(b, r, x=0.0, y_top=31.2, z=5.5, length=16.0, w=3.0, count=3, seed=586, sway=3.2)
    N.tie(b, 0.0, 31.2, 4.7, "#f6e6d8", 1.8)
    N.shells(b, "#f6e6d8", n=3, x=0.0, y=30.0, z=5.9)
    N.necklace(b, "#e8b830", "#e8f0ff")
    N.fish_mask(b, "#9ac0e8", "#3a5a8a", bill=False)


def kiato(b):
    k = "#1c2a4a"
    base(b, k, 591, front=30.9, side=28.4, back=25.6, tiers=1)
    spikes(b, k, 591, n=7, length=3.4, lean=12.0, fan=34.0, y=33.6)
    P.fringe(b, k, n=9, span=8.4, y_top=31.7, length=3.2, seed=594, rot=12.0)
    N.fish_mask(b, "#c8d8f0", "#5a7aa8", bill=True)
    N.hood_down(b, "#e8ecf4", "#8a9ab8")


# ================================================================================================ faces and the table
MOUTH_SMIRK = dict(kind="smirk", color="#8a4a4a", lip="#c0807a")
MOUTH_GRIN = dict(kind="grin", color="#7a2a2a", lip="#c0807a", w=3.0)


def _chars():
    C = []

    def add(cid, build, fc, keep=True):
        C.append(dict(id=cid, face=fc, build=build, keep_extras=keep))

    def ear(cid):
        return ears_color(cid)

    # ---- Midnight Sun
    add("rhya", rhya, face("rhya", "#1a1a26", "#34345a", hairline=1.7, brow=dict(angle=0.5), mouth=MOUTH_SMIRK, marks=[m_slashes(6.3, 6.1)], tilt=0.14))
    add("rhya_form2", lambda b: rhya_f2(b, ear("rhya_form2")), face("rhya_form2", "#f2f4fc", "#f0c030", glow="#ffd040", hairline=1.7, brow=dict(angle=0.7), mouth=MOUTH_SMIRK, marks=[m_tattoo_up(glow="#ff4a4a")], tilt=0.16))
    add("fana", fana, face("fana", "#e8709e", "#3ab08a", hairline=0.5, brow=dict(angle=0.5, color="#b04a70"), mouth=dict(kind="flat", color="#8a3a4a", lip="#c0708a"), marks=[m_stone_frame()], h=2.3, w=2.7, hi=1.0))
    add("fana_form2", lambda b: fana_f2(b, ear("fana_form2")), face("fana_form2", "#ff7a9e", "#ffa020", glow="#ffb840", hairline=0.5, brow=dict(angle=0.9, color="#c0486a"), mouth=dict(kind="fang", color="#8a3a4a", lip="#c0708a"), marks=[m_stone_frame("#ff3a5a")], h=2.3, w=2.7, slit=True, tilt=0.14))
    add("patolli", lambda b: patolli(b, ear("patolli")), face("patolli", "#f2efe6", "#f0c030", glow="#ffd040", hairline=1.4, brow=dict(skip=(0, 1)), mouth=dict(kind="flat", color="#8a4a4a", lip="#c0807a"), marks=[m_crescents()], h=2.1, tilt=0.12))
    add("patolli_form2", lambda b: patolli_f2(b, ear("patolli_form2")), face("patolli_form2", "#f6f2ff", "#e060ff", glow="#ff70ff", hairline=1.4, brow=dict(skip=(0, 1)), mouth=dict(kind="fang", color="#2a1a3a", lip="#5a3a7a"), marks=[m_crescents("#e040c8")], h=2.1, tilt=0.14, dark_sclera="#0c0612"))
    add("rades_spirito", rades, face("rades_spirito", "#b890f0", "#4a8ae8", hairline=1.0, brow=dict(angle=0.6, skip=(1,)), skip=(1,), mouth=dict(kind="smirk", color="#7a3a4a", lip="#b0707a"), marks=[m_scar([(1.4, 5.4), (1.9, 6.3), (2.6, 7.2)])]))
    add("rades_spirito_form2", lambda b: rades_f2(b, ear("rades_spirito_form2")), face("rades_spirito_form2", "#d8b8ff", "#40e0a0", glow="#60ffb8", hairline=1.0, brow=dict(angle=0.8, skip=(1,)), skip=(1,), mouth=dict(kind="fang", color="#7a3a4a", lip="#b0707a"), marks=[m_scar([(1.4, 5.4), (1.9, 6.3), (2.6, 7.2)], glow="#40ffa0")], slit=True))
    # ---- Black Bulls
    add("asta", asta, face("asta", "#d4d8e2", "#2eb05a", hairline=1.9, brow=dict(angle=0.8, thick=0.3), mouth=MOUTH_GRIN, tilt=0.06, h=2.1))
    add("yami_sukehiro", yami, face("yami_sukehiro", "#15151d", "#9aa0aa", hairline=1.6, brow=dict(angle=0.9, thick=0.32), mouth=MOUTH_SMIRK, marks=[m_stubble(), lambda f: S.mark_moustache(f, "#1a1618", goatee=True)], tilt=0.18, h=1.8))
    add("noelle_silva", noelle, face("noelle_silva", "#e0e5f2", "#e070b0", hairline=1.6, brow=dict(angle=0.4, color="#9aa0c0"), mouth=dict(kind="open", color="#8a3a4a", lip="#d08088", w=1.8), blush=0.35, h=2.3, w=2.7, lash="#5a4a6a"))
    add("finral_roulacase", finral, face("finral_roulacase", "#b98c52", "#4a9a6a", hairline=1.6, brow=dict(angle=-0.4), mouth=dict(kind="open", color="#8a3a3a", lip="#c0807a", w=2.0), tilt=-0.10, h=1.9))
    add("magna_swing", magna, face("magna_swing", "#1a1a24", "#2a60c0", hairline=2.0, brow=dict(angle=0.9, thick=0.3), mouth=MOUTH_GRIN, marks=[m_magna()], pre_eye=[]))
    add("luck_voltia", luck, face("luck_voltia", "#f8d63c", "#8ad0f0", hairline=1.4, brow=dict(angle=0.9, thick=0.3, color="#b89a20"), mouth=MOUTH_GRIN, tilt=0.12, h=2.1))
    add("charmy_pappitson", charmy, face("charmy_pappitson", "#2a2634", "#4ac060", hairline=0.6, brow=dict(angle=0.0, thick=0.18), mouth=dict(kind="open", color="#8a3a3a", lip="#d08088", w=2.0, open=0.7), blush=0.55, h=2.4, w=2.8))
    add("vanessa_enoteca", vanessa, face("vanessa_enoteca", "#f5559f", "#8a4ad0", hairline=1.4, brow=dict(angle=0.2), mouth=dict(kind="smirk", color="#b02a58", lip="#e0508a"), tilt=0.10, h=2.0, lash_w=0.55))
    add("gordon_agrippa", gordon, face("gordon_agrippa", "#1b1b26", "#d02a2a", hairline=1.9, brow=dict(skip=(0, 1)), mouth=dict(kind="flat", color="#14101a", lip="#14101a", w=1.6), pre_eye=[m_eyeshadow()], tilt=-0.04, h=1.9))
    add("zora_ideale", zora, face("zora_ideale", "#d02e1a", "#c87a2a", hairline=1.7, brow=dict(angle=0.9, thick=0.3), mouth=dict(kind="flat", color="#4a1a1a", lip="#4a1a1a"), marks=[m_straps()], tilt=0.16, h=1.9))
    add("gauche_adlai", gauche, face("gauche_adlai", "#8a6a46", "#7a5030", hairline=1.4, brow=dict(angle=0.2), skip=(1,), mouth=dict(kind="flat", color="#8a4a4a", lip="#c0807a"), marks=[m_mirror()], h=1.9))
    add("grey", grey, face("grey", "#a8d0f4", "#5a8ac8", skin_mid="#f2cdb6", hairline=1.6, brow=dict(angle=-0.2), mouth=dict(kind="flat", color="#8a4a5a", lip="#c0808a", w=1.5), blush=0.25, h=2.2))
    add("secre_swallowtail", secre, face("secre_swallowtail", "#26263a", "#4a4a6a", hairline=1.6, brow=dict(angle=0.5), mouth=dict(kind="flat", color="#6a3a4a", lip="#a07080", w=1.5), marks=[m_bags()], tilt=0.20, h=1.8))
    add("nacht_faust", nacht, face("nacht_faust", "#1c1c28", "#8ac8f0", hairline=1.6, brow=dict(angle=0.7), mouth=MOUTH_SMIRK, tilt=0.14, h=1.9))
    # ---- Magic Knights
    add("julius_novachrono", julius, face("julius_novachrono", "#f4e6a6", "#a060e0", hairline=1.6, brow=dict(angle=0.3, color="#c8b070"), mouth=dict(kind="smirk", color="#8a4a4a", lip="#c0807a"), marks=[m_star6()], h=2.0))
    add("marx_francois", marx, face("marx_francois", "#d9c79a", "#6a7aa0", hairline=1.5, brow=dict(angle=0.5, color="#a89868"), mouth=dict(kind="flat", color="#8a4a4a", lip="#b0807a", w=1.5), tilt=0.12, h=1.5, lash_w=0.55))
    add("yuno", yuno, face("yuno", "#1a1a24", "#e8b830", glow="#ffd040", hairline=1.7, brow=dict(angle=0.5), mouth=dict(kind="flat", color="#8a4a4a", lip="#c0807a"), tilt=0.12, h=2.0))
    add("mimosa_vermillion", mimosa, face("mimosa_vermillion", "#f06a3c", "#e8c030", hairline=1.4, brow=dict(angle=-0.3, color="#c0502a"), mouth=dict(kind="open", color="#8a3a3a", lip="#d08088", w=1.8), blush=0.4, h=2.3, w=2.7))
    add("klaus_lunettes", klaus, face("klaus_lunettes", "#6fa4ee", "#e060c0", hairline=1.6, brow=dict(angle=0.8, color="#4a78b8"), mouth=dict(kind="flat", color="#8a4a5a", lip="#c0808a", w=1.5), tilt=0.16, h=1.8))
    add("leopold_vermillion", leopold, face("leopold_vermillion", "#de4a1e", "#30c0b8", hairline=1.7, brow=dict(angle=0.9, thick=0.3, color="#a83a18"), mouth=dict(kind="grin", color="#7a2a2a", lip="#c0807a", w=3.0), marks=[m_eye_markings(), m_diamond()], tilt=0.12, h=2.1))
    add("fuegoleon_vermillion", fuegoleon, face("fuegoleon_vermillion", "#c2321c", "#a060d0", hairline=1.5, brow=dict(angle=0.6, color="#a02a18"), mouth=MOUTH_SMIRK, marks=[m_eye_markings(), m_diamond()], tilt=0.10, h=1.9))
    add("mereoleona_vermillion", mereoleona, face("mereoleona_vermillion", "#d4421c", "#4a8ae8", hairline=1.5, brow=dict(angle=1.0, thick=0.32, color="#a83a18"), mouth=dict(kind="fang", color="#7a2a2a", lip="#c0807a"), marks=[m_eye_markings()], tilt=0.16, h=1.9))
    add("charlotte_roselei", charlotte, face("charlotte_roselei", "#e8d890", "#8ac8f0", hairline=1.5, brow=dict(angle=0.2, color="#b8a860"), mouth=dict(kind="flat", color="#b04a62", lip="#e0708a", w=1.8), h=2.3, lash_w=0.55))
    add("nozel_silva", nozel, face("nozel_silva", "#f2f4ff", "#9a7ae0", hairline=1.7, brow=dict(angle=0.6, thick=0.2, color="#b8bcd0"), mouth=dict(kind="flat", color="#8a4a5a", lip="#b8808a", w=1.5), tilt=0.14, h=1.6))
    add("solid_silva", solid, face("solid_silva", "#8fb4f4", "#4a78e0", hairline=1.7, brow=dict(angle=0.3), mouth=dict(kind="flat", color="#8a4a5a", lip="#c0808a", w=1.5), h=2.0))
    add("jack_the_ripper", jack, face("jack_the_ripper", "#1c1c22", "#f0c030", glow="#ffd040", hairline=1.7, brow=dict(angle=0.9, thick=0.3), mouth=dict(kind="flat", color="#3a1a1a", lip="#5a2a2a", w=2.4), marks=[m_scar([(5.4, 0.4), (5.6, 2.2), (5.2, 4.2), (5.7, 5.9), (6.0, 7.3)])], tilt=0.20, h=1.7))
    add("dorothy_unsworth", dorothy, face("dorothy_unsworth", "#b090d0", "#8a60d0", skin_mid="#f2d2be", hairline=1.6, brow=dict(angle=-0.1, color="#8a6aa8"), mouth=dict(kind="flat", color="#a04a5a", lip="#d08090", w=1.4), blush=0.3, h=1.7, lid=0.55))
    add("william_vangeance", william, face("william_vangeance", "#f4f5fb", "#9a60e0", hairline=1.6, brow=dict(angle=0.2, color="#c8cad8"), mouth=dict(kind="flat", color="#8a4a5a", lip="#b8808a", w=1.5), h=1.9))
    add("kahono", kahono, face("kahono", "#a02a30", "#d02a3a", skin_mid="#ecc4a6", hairline=1.6, brow=dict(angle=0.3), mouth=dict(kind="open", color="#8a3a3a", lip="#d08088", w=1.8), blush=0.2, h=2.2))
    add("kiato", kiato, face("kiato", "#1c2a4a", "#4a5a90", hairline=1.6, brow=dict(angle=0.4), mouth=dict(kind="flat", color="#8a4a4a", lip="#c0807a"), h=2.0))
    return C


CHARS = _chars()
IDS = [c["id"] for c in CHARS]


def run(flt, olds, build_one, stats):
    """Builds the looks of this module (all, or those whose id contains one of the `flt` words) with gen_bc's build_one and writes looks_bc_npc.json and stats_npc.json."""
    looks = {}
    if os.path.isfile(LOOKS_NPC) and flt:
        looks = json.load(open(LOOKS_NPC))["looks"]
    mine = {}
    for spec in CHARS:
        if flt and not any(f in spec["id"] for f in flt):
            continue
        nid, look = build_one(spec, olds)
        looks[nid] = look
        s = stats.pop(nid)
        mine[nid] = s
        print("%-34s cubes %s quads %s glow_quads %s tiles %d fill %.2f" % (nid, s["cubes"], s["quads"], s["glow_quads"], s["atlas_tiles"], s["atlas_fill"]))
    if not mine:
        return
    doc = {"version": 1, "note": "Black Clover NPC and Midnight Sun player models (docs/npcs_bc.md), written by tools/bc_playermodels/gen_bc.py (specs_npc.py). Every id is 'bc_' + the id of the look it replaces; the old looks stay in looks.json.", "looks": dict(sorted(looks.items()))}
    with open(LOOKS_NPC, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")
    allstats = json.load(open(STATS_NPC)) if os.path.isfile(STATS_NPC) else {}
    allstats.update(mine)
    with open(STATS_NPC, "w") as f:
        json.dump(allstats, f, indent=1, sort_keys=True)
