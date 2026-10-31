"""Code (Boruto, Kara's Inner), HD 256x256 (3.38.0, group c of docs/bosses/hd_outfits.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/code.py) through the HD bridge; then drawn again at 4x detail: the cold face (glowing cyan eyes with a
cool lid shade, flat pale brows, a thin mouth, the cyan forehead seal), the slate high-collared robe with cyan piping, the charcoal tabard with a column of seal-script runes and gems, the steel
belt with a hexagonal buckle and a cyan gem, the layered steel pauldrons with cyan rims and rivets, the riveted steel gauntlets with plate seams and a gem, the robe skirts with a glowing cyan hem,
the steel greaves over black boots, and the glowing ring crest on the back. Hair: `anime_spike_tall` (tall, thick, back-swept spikes in layered clumps), white. Original art from memory of the canon look.
Glow: the eyes, the seal, the piping, the runes, the gems, the hem and the crest (every cyan line).
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

SLATE, SLATE_D, SLATE_L = "#4b5f6b", "#2f3d47", "#6a8190"
CHAR, CHAR_D = "#2a353c", "#171f24"
STEEL, STEEL_D, STEEL_L = "#6f8997", "#3e5664", "#9ab6c4"
CYAN, CYAN_D, CYAN_L = "#58e0f0", "#1a8aa4", "#d6fbff"
BLACK = "#12181c"
SKIN = ("#d4b8ae", "#ead2c8", "#f6e4dc")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def neon(f, pts, width=0.2, a=1.0, glow=True):
    """A lit cyan line: a dark halo, the cyan body, a bright core. Also lights the glow map."""
    f.fill(f.m_polyline(pts, width + 0.22), H.tone(CYAN_D, 0.35), 0.55 * a)
    f.fill(f.m_polyline(pts, width), CYAN, a, glow=CYAN if glow else None)
    f.fill(f.m_polyline(pts, width * 0.38), CYAN_L, 0.9 * a)


def rune(f, cx, cy, s=0.9, k=0, a=1.0):
    """A tiny seal-script glyph: a few strokes in a box of about s x 1.3 s."""
    w = s * 0.5
    h = s * 0.65
    sets = (
        [[(-w, 0), (0, -h), (w, 0), (0, h), (-w, 0)], [(0, -h * 0.45), (0, h * 0.45)]],                                  # a diamond with a bar
        [[(-w, -h), (w, -h)], [(-w * 0.6, 0), (w * 0.6, 0)], [(-w, h), (w, h)], [(0, -h), (0, h)]],                      # a ladder
        [[(-w, -h * 0.2), (0, -h), (w, -h * 0.2)], [(-w, h * 0.5), (0, -h * 0.1), (w, h * 0.5)], [(0, h * 0.6), (0, h)]],  # two chevrons
        [[(-w, -h), (-w, h)], [(w, -h), (w, h)], [(-w, 0), (w, 0)], [(0, -h), (0, -h * 0.4)]],                           # an H with a mark
    )
    for seg in sets[k % len(sets)]:
        neon(f, [(cx + p[0], cy + p[1]) for p in seg], 0.11, a)


def plate(f, mask, color=STEEL, y0=None, y1=None, depth=0.3, spec=0.25):
    ys = np.where(np.asarray(mask) > 0.5)[0]
    if y0 is None:
        y0 = f.Y[ys.min(), 0] if len(ys) else 0.0
        y1 = f.Y[ys.max(), 0] if len(ys) else f.h
    f.fill(mask, H.metal_paint(y0, y1, color, 0.8))
    H.bevel(f, mask, depth=depth, gain=1.1, spec=spec, spec_pow=24, spec_color="#e8faff")


def rivet(f, x, y, r=0.13):
    H.stud(f, x, y, r, STEEL_L, rim_color=STEEL_D, spec=0.7)


def head(cv, hair):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=1.9, wave=0.4, shadow=0.22)
    # cold, flat brows, pale like the hair
    H.brow(f, (3.4, 3.35), (0.9, 3.0), "#c8d4e0", 0.22, 0.1, arch=0.02)
    H.brow(f, (4.6, 3.35), (7.1, 3.0), "#c8d4e0", 0.22, 0.1, arch=0.02)
    # glowing cyan eyes: narrow, long, a cool lid shade; the whole eye lights the glow map
    iris = ("#0a5470", "#34bcd8", "#b8f4ff")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.85, 2.7, 1.75, iris, lash="#10222c", sclera="#9ed2e0", tilt=0.12, flick=0.36, lash_w=0.32, lid_shadow=0.36, glow=True, iris_r=1.05, pupil="#0a3a4c")
    H.nose(f, 4.0, 6.2, "#c0a29a", 0.18)
    H.mouth_smile(f, 4.0, 7.0, 1.7, color="#9a6a6e", lip="#c89a96", depth=-0.04, thick=0.13)
    H.blush(f, 1.1, 6.2, 0.9, 0.4, "#b8c4d8", 0.18)
    H.blush(f, 6.9, 6.2, 0.9, 0.4, "#b8c4d8", 0.18)
    # the cyan seal on the forehead (a small diamond with a bar), under the fringe
    for g in (f,):
        g.poly([(4.0, 1.75), (4.5, 2.35), (4.0, 2.95), (3.5, 2.35)], CYAN, a=0.95, glow=CYAN)
        g.poly([(4.0, 2.05), (4.25, 2.35), (4.0, 2.65), (3.75, 2.35)], CYAN_L, a=0.9)
        neon(g, [(2.9, 2.35), (3.4, 2.35)], 0.1, 0.9)
        neon(g, [(4.6, 2.35), (5.1, 2.35)], 0.1, 0.9)


def collar(f, back=False):
    """The high slate collar with a cyan trim edge; a V opening on the front shows the throat."""
    m = f.m_band(0.0, 2.5)
    f.fill(m, H.lin((0, 0), (0, 2.5), [(0, H.tone(SLATE, 1.1)), (1, H.tone(SLATE, 0.78))]))
    H.sheen(f, m, [1.4, f.w - 1.4], width=0.3, strength=0.18, y0=0.1, y1=2.2)
    if not back and f.w > 5:
        v = f.m_poly([(3.1, -0.1), (4.9, -0.1), (4.0, 1.7)])
        f.fill(v, H.lin((0, 0), (0, 1.7), [(0, SKIN[0]), (1, H.tone(SKIN[0], 0.7))]))
        neon(f, [(3.05, 0.0), (4.0, 1.75), (4.95, 0.0)], 0.12, 0.9)
    H.trim_band(f, 2.2, 2.55, STEEL, outline=0.5)
    neon(f, [(0.0, 2.72), (f.w, 2.72)], 0.14)
    H.shadow_under(f, 2.8, 0.5, 0.3)


def robe_base(f, back=False):
    m = f.m_band(0.0, 12.0)
    f.fill(m, H.lin((0, 0), (0, 12.0), [(0, H.tone(SLATE, 1.08)), (0.6, SLATE), (1, H.tone(SLATE, 0.8))]))
    away = f.X if f.light[0] > 0 else f.w - f.X
    f.shade(m, 1.0 - 0.12 * H.smoothstep(0.35 * f.w, f.w, away))
    H.weave(f, m, 0.035)


def belt(f, front=True):
    m = f.m_band(7.1, 8.9)
    plate(f, m, STEEL_D, 7.1, 8.9, depth=0.25, spec=0.25)
    H.trim_line(f, [(0.0, 7.3), (f.w, 7.3)], STEEL_L, width=0.12, outline=0.0, spec=0.0, a=0.6)
    neon(f, [(0.0, 8.75), (f.w, 8.75)], 0.1, 0.8)
    for x in np.arange(0.5, f.w, 1.0):
        rivet(f, float(x), 8.1, 0.1)
    H.shadow_under(f, 8.95, 0.6, 0.3)


def hex_buckle(f, cx, cy, r=1.0):
    pts = [(cx + r * math.cos(math.radians(60 * i + 30)), cy + r * math.sin(math.radians(60 * i + 30))) for i in range(6)]
    f.fill(f.m_poly([(cx + (x - cx) * 1.14, cy + (y - cy) * 1.14) for x, y in pts]), H.tone(STEEL_D, 0.5))
    mk = f.m_poly(pts)
    plate(f, mk, STEEL, cy - r, cy + r, depth=0.3, spec=0.25)
    H.gem(f, cx, cy, r * 0.46, CYAN, setting=None, glow=True)


def skirt_panel(f, back=False):
    """The robe's skirt below the belt: slate panels, a glowing cyan hem with small chevrons."""
    m = f.m_band(8.9, 12.0)
    f.fill(m, H.lin((0, 8.9), (0, 12.0), [(0, H.tone(SLATE, 0.8)), (1, H.tone(SLATE, 0.6))]))
    xs = [1.4, 2.8, 4.0, 5.2, 6.6] if f.w > 5 else [1.3, 2.7]
    H.folds(f, m, xs, depth=0.2, width=0.22, y0=8.9, y1=12.0, sway=0.2)
    H.trim_band(f, 11.1, 11.5, STEEL, outline=0.5)
    neon(f, [(0.0, 11.8), (f.w, 11.8)], 0.16)
    for x in np.arange(0.5, f.w, 1.0):
        neon(f, [(float(x) - 0.28, 11.3), (float(x), 11.55), (float(x) + 0.28, 11.3)], 0.07, 0.8)


def body_front(f):
    robe_base(f)
    # the charcoal tabard framed by two cyan lines
    tb = f.m_poly([(2.25, 2.4), (5.75, 2.4), (5.75, 12.0), (2.25, 12.0)], soft=1.0)
    f.fill(tb, H.lin((0, 2.4), (0, 12.0), [(0, H.tone(CHAR, 1.15)), (1, CHAR_D)]))
    H.folds(f, tb, [3.1, 4.9], depth=0.14, width=0.2, y0=3.0, y1=12.0)
    H.weave(f, tb, 0.04)
    neon(f, [(2.2, 2.6), (2.2, 11.9)], 0.17)
    neon(f, [(5.8, 2.6), (5.8, 11.9)], 0.17)
    # the rune column and the gems down the tabard
    for i, y in enumerate((3.5, 4.9, 6.3)):
        rune(f, 4.0, y, 0.7, k=i)
    # side panels: seam lines and small plates
    for x0, x1 in ((0.0, 2.0), (6.0, 8.0)):
        H.trim_line(f, [((x0 + x1) / 2.0, 3.0), ((x0 + x1) / 2.0, 7.0)], SLATE_L, width=0.1, outline=0.0, spec=0.0, a=0.5)
        H.trim_line(f, [((x0 + x1) / 2.0, 9.2), ((x0 + x1) / 2.0, 11.0)], SLATE_L, width=0.1, outline=0.0, spec=0.0, a=0.5)
    belt(f)
    hex_buckle(f, 4.0, 8.0, 1.05)
    skirt_panel(f)
    for x in (4.0,):
        rune(f, x, 10.2, 0.75, k=3)
    collar(f)


def body_back(f):
    robe_base(f, True)
    collar(f, back=True)
    # the ring crest: a double cyan ring with a glyph inside
    cx, cy = 4.0, 5.2
    f.fill(f.m_ring(cx, cy, 2.05, 0.5), H.tone(CYAN_D, 0.35), 0.6)
    f.fill(f.m_ring(cx, cy, 2.05, 0.3), CYAN, glow=CYAN)
    f.fill(f.m_ring(cx, cy, 2.05, 0.1), CYAN_L, 0.9)
    f.fill(f.m_ring(cx, cy, 1.35, 0.14), CYAN, 0.9, glow=CYAN)
    for i in range(8):
        a = math.tau * i / 8
        neon(f, [(cx + math.cos(a) * 1.4, cy + math.sin(a) * 1.4), (cx + math.cos(a) * 2.0, cy + math.sin(a) * 2.0)], 0.1, 0.9)
    rune(f, cx, cy, 1.1, k=0)
    for x in (0.9, 7.1):
        H.trim_line(f, [(x, 3.0), (x, 7.0)], SLATE_L, width=0.1, outline=0.0, spec=0.0, a=0.5)
    neon(f, [(2.0, 8.9), (2.0, 11.9)], 0.12, 0.8)
    neon(f, [(6.0, 8.9), (6.0, 11.9)], 0.12, 0.8)
    belt(f, False)
    skirt_panel(f, True)


def body_side(f):
    robe_base(f)
    collar(f)
    neon(f, [(2.0, 3.0), (2.0, 7.0)], 0.12, 0.7)
    belt(f)
    skirt_panel(f)


def body(cv):
    for nm in SIDES:
        for f in lay(cv, "body", nm):
            if nm == "front":
                body_front(f)
            elif nm == "back":
                body_back(f)
            else:
                body_side(f)
    for f in lay(cv, "body", "top"):
        f.fill(f.m_all(), H.rad((f.w * 0.5, f.h * 0.5), f.w * 0.8, [(0, SLATE_L), (1, SLATE_D)]))
    # the overlay keeps the collar, the tabard frame edges, the belt and the skirt; the middle of the robe is the base layer
    for nm in SIDES:
        f = cv.face("body", nm, True)
        if nm in ("front", "back"):
            f.clear(f.m_band(2.9, 7.0))
        else:
            f.clear(f.m_band(2.9, 7.0))


def pauldron(f, nm):
    """Two layered steel plates with a cyan rim and rivets over the shoulder."""
    w = f.w
    p2 = f.m_rect(-0.3, 1.5, w + 0.3, 3.5, r=0.5)
    f.fill(f.m_rect(-0.3, 1.52, w + 0.3, 3.62, r=0.5), H.tone(STEEL_D, 0.5), 0.7)
    plate(f, p2, STEEL, 1.5, 3.5, depth=0.35, spec=0.25)
    neon(f, [(0.0, 3.35), (w, 3.35)], 0.13, 0.9)
    p1 = f.m_rect(-0.3, 0.0, w + 0.3, 2.3, r=0.45)
    f.fill(f.m_rect(-0.3, 0.0, w + 0.3, 2.42, r=0.45), H.tone(STEEL_D, 0.5), 0.7)
    plate(f, p1, STEEL_L, 0.0, 2.3, depth=0.35, spec=0.25)
    neon(f, [(0.0, 2.15), (w, 2.15)], 0.12, 0.8)
    for x in (0.6, w - 0.6):
        rivet(f, x, 1.1, 0.13)
        rivet(f, x, 2.75, 0.12)


def sleeve(f, nm):
    w = f.w
    m = f.m_band(3.5, 7.1)
    f.fill(m, H.lin((0, 3.5), (0, 7.1), [(0, H.tone(SLATE, 0.8)), (0.5, SLATE), (1, H.tone(SLATE, 0.78))]))
    H.folds(f, m, [1.0, 2.9], depth=0.15, width=0.22, y0=3.7, y1=7.1)
    H.weave(f, m, 0.03)
    neon(f, [(w * 0.5, 3.9), (w * 0.5, 6.7)], 0.1, 0.7)
    H.trim_band(f, 6.9, 7.5, STEEL, outline=0.5)
    neon(f, [(0.0, 7.6), (w, 7.6)], 0.13)


def gauntlet(f, nm):
    w = f.w
    m = f.m_band(7.7, 11.2)
    plate(f, m, STEEL, 7.7, 11.2, depth=0.3, spec=0.25)
    H.trim_line(f, [(0.0, 9.1), (w, 9.1)], STEEL_D, width=0.1, outline=0.0, spec=0.0, a=0.8)
    H.trim_line(f, [(0.0, 10.2), (w, 10.2)], STEEL_D, width=0.1, outline=0.0, spec=0.0, a=0.8)
    H.trim_band(f, 7.6, 8.0, STEEL_L, outline=0.5)
    if nm in ("front", "back"):
        H.gem(f, w / 2.0, 8.9, 0.34, CYAN, setting=STEEL_D, glow=True)
        for x in (0.7, w - 0.7):
            rivet(f, x, 10.7, 0.11)
    knuckle = f.m_rect(0.3, 10.4, w - 0.3, 11.3, r=0.25)
    plate(f, knuckle, STEEL_L, 10.4, 11.3, depth=0.2, spec=0.25)
    # the black glove below
    gl = f.m_band(11.2, 12.0)
    f.fill(gl, H.lin((0, 11.2), (0, 12.0), [(0, "#1e272d"), (1, BLACK)]))


def arms(cv):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            base, ov = lay(cv, part, nm)
            for f in (base, ov):
                if f.overlay:
                    f.clear()
                sleeve(f, nm)
                gauntlet(f, nm)
                pauldron(f, nm)
        for f in lay(cv, part, "top"):
            f.fill(f.m_all(), H.rad((f.w * 0.4, f.h * 0.4), f.w, [(0, STEEL_L), (1, STEEL_D)]))
            rivet(f, f.w / 2.0, f.h / 2.0, 0.3)
        for f in lay(cv, part, "bottom"):
            f.fill(f.m_all(), BLACK)


def leg(cv, part):
    for nm in SIDES:
        for f in lay(cv, part, nm):
            if f.overlay:
                f.clear()
            w = f.w
            # slate trousers with a seam, the glowing cyan hem, steel greaves, black boots
            m = f.m_band(0.0, 7.2)
            f.fill(m, H.lin((0, 0), (0, 7.2), [(0, H.tone(SLATE, 0.78)), (1, H.tone(SLATE, 0.62))]))
            H.folds(f, m, [1.0, 2.9], depth=0.2, width=0.22, y0=0.0, y1=7.2)
            H.weave(f, m, 0.03)
            H.trim_band(f, 6.7, 7.1, STEEL, outline=0.5)
            neon(f, [(0.0, 7.35), (w, 7.35)], 0.15)
            g = f.m_band(7.7, 10.9)
            plate(f, g, STEEL, 7.7, 10.9, depth=0.3, spec=0.25)
            H.trim_line(f, [(0.0, 9.2), (w, 9.2)], STEEL_D, width=0.1, outline=0.0, spec=0.0, a=0.8)
            if nm == "front":
                neon(f, [(w / 2.0, 7.9), (w / 2.0, 10.7)], 0.1, 0.8)
                for x in (0.6, w - 0.6):
                    rivet(f, x, 8.5, 0.11)
            boot = f.m_band(10.9, 12.0)
            f.fill(boot, H.lin((0, 10.9), (0, 12.0), [(0, "#222c32"), (1, BLACK)]))
            H.trim_band(f, 10.8, 11.1, STEEL_D, outline=0.4)
            neon(f, [(0.0, 11.82), (w, 11.82)], 0.08, 0.7)
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), BLACK)
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), SLATE_D)


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    hair = "anime_spike_tall"
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.family(STEEL, 16, 0.5)], "gem": [H.near("#6fe3f0", 0.2)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arms(cv)
    for part in ("leg_r", "leg_l"):
        leg(cv, part)
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim
