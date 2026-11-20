"""The cube parts of Kakashi Hatake on the Black Clover player-model kit (docs/bosses/kakashi.md): the silver cube hair swept to the side, the hitai-ate (covering the left eye, or pushed up) with the
Leaf plate and its tied tails, the mask cloth with the nose ridge, the flak jacket pieces (collar, pockets, shoulder pads, hem), the belt, the tool pouch and the thigh holster, the forearm guards,
the bandage wraps, and the Perfect Susanoo as a translucent glowing figure (the `fx` layer).

Same conventions as bcparts.py: every function draws into a `Build` on a layer ("main" / "fine" / "fx"); pixels, y up, front on -z, +x the model's left (the viewer's right in the front view). New
tiles are painted here with the atlas (`b.atlas.tile`), the rest come from the kit's own material tiles.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import numpy as np  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

import bclib as L  # noqa: E402
import bcparts as P  # noqa: E402
from bclib import hsh, rgb, tone  # noqa: E402

HEAD = "head"
HAIR = "#cdd3e2"
HAIR_TIP = "#e6ebf8"
BAND = "#27345e"
STEEL = "#b4becd"
SHIRT = "#222c4c"
VEST = "#5b6a4c"
VEST_L = "#79896a"
LEATHER = "#3a2c24"
BANDAGE = "#e8e2d2"
PAD = "#4a5840"


# ================================================================================================ tiles
def t_plate(b, tw=40, th=20):
    """The hitai-ate plate: brushed steel with a dark rim, four rivets and the Leaf symbol (a spiral and the notch) engraved. Painted at 8x and reduced so the lines are clean."""
    def paint():
        k = 8
        big = Image.new("RGBA", (tw * k, th * k), (0, 0, 0, 255))
        d = ImageDraw.Draw(big)
        for y in range(th * k):                                              # the metal: a bright top, a dark horizon, a warm bounce
            t = y / (th * k - 1.0)
            c = L._grad(1, [(0.0, STEEL)])[0]
            lvl = 1.28 - 0.5 * t if t < 0.6 else 0.98 - 0.35 * (t - 0.6) / 0.4
            col = tuple(int(np.clip(c[i] * lvl, 0, 1) * 255) for i in range(3))
            d.line([(0, y), (tw * k, y)], fill=col + (255,))
        rim = tuple(int(v * 255) for v in tone(STEEL, 0.38))
        d.rectangle([0, 0, tw * k - 1, th * k - 1], outline=rim, width=int(1.4 * k))
        d.rectangle([int(2.2 * k), int(2.2 * k), tw * k - int(2.2 * k), th * k - int(2.2 * k)], outline=tuple(int(v * 255) for v in tone(STEEL, 0.62)), width=int(0.6 * k))
        eng = tuple(int(v * 255) for v in tone(STEEL, 0.30))
        cx, cy, r = tw * k * 0.5, th * k * 0.5, th * k * 0.27
        pts = []                                                              # the Leaf spiral: an Archimedean spiral of two turns, open towards the notch
        for i in range(0, 260):
            a = i / 259.0 * 4.4 * math.pi
            rr = r * (0.12 + 0.88 * a / (4.4 * math.pi))
            pts.append((cx + math.cos(a + 0.9) * rr, cy + math.sin(a + 0.9) * rr))
        d.line(pts, fill=eng, width=int(1.5 * k), joint="curve")
        tip = pts[-1]
        d.polygon([(cx - r * 1.55, cy - r * 0.2), (cx - r * 0.55, cy - r * 1.0), (cx - r * 0.55, cy + r * 0.6)], fill=eng)               # the notch of the Leaf symbol
        for rx_ in (tw * k * 0.1, tw * k * 0.9):
            for ry_ in (th * k * 0.3, th * k * 0.7):
                d.ellipse([rx_ - 0.9 * k, ry_ - 0.9 * k, rx_ + 0.9 * k, ry_ + 0.9 * k], fill=tuple(int(v * 255) for v in tone(STEEL, 1.5)), outline=rim)
        im = big.resize((tw, th), Image.LANCZOS)
        a = np.asarray(im, np.float32) / 255.0
        a[..., 3] = 1.0
        return a
    return b.atlas.tile(("kk_plate", tw, th), tw, th, paint)


def t_leather(b, tw=16, th=16, seed=3):
    return b.t_cloth(LEATHER, tw, th, seed=seed, folds=2, light=1.25, dark=0.7)


def t_bandage(b, tw=32, th=8, seed=5):
    """Bandage wraps: off-white cloth with a few diagonal turns."""
    def paint():
        a = np.zeros((th, tw, 4), np.float32)
        a[..., 3] = 1.0
        base = rgb(BANDAGE)
        ramp = L._grad(th, [(0.0, tone(BANDAGE, 1.1)), (1.0, tone(BANDAGE, 0.78))])
        a[..., :3] = ramp[:, None, :]
        for x in range(tw):
            for y in range(th):
                if (x + y * 2) % 9 == 0:
                    a[y, x, :3] *= 0.84
        a[0, :, :3] *= 0.8
        a[-1, :, :3] *= 0.7
        return a
    return b.atlas.tile(("kk_bandage", tw, th, seed), tw, th, paint)


def t_ghost(b, color, edge, tw=32, th=32, seed=0, rib=8, fill=0.34, kind="panel"):
    """A Susanoo piece, light only: the main texture is empty (nothing is drawn in the cutout pass) and the glow map carries everything, so the figure is a translucent energy body added on top
    of the scene. kind 'panel': a faint fill that is brighter at the top, a bright frame and thin plate lines every `rib` texels; 'spike': a triangle with a bright core; 'solid': a hot piece
    (eye slits, blade, grip) with a white-hot centre."""
    def paint():
        a = np.zeros((th, tw, 4), np.float32)
        g = np.zeros((th, tw, 4), np.float32)
        ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
        if kind == "panel":
            edge_d = np.minimum(np.minimum(xs, tw - 1 - xs), np.minimum(ys, th - 1 - ys))
            frame = edge_d < 1.0
            ribs = (ys % rib == 0) & (xs > 0) & (xs < tw - 1)
            soft = np.clip(1.0 - edge_d / 5.0, 0, 1) ** 1.5 * 0.35
            g[..., :3] = rgb(color)[None, None, :]
            g[..., 3] = fill * (0.75 + 0.5 * (1.0 - ys / (th - 1.0))) + soft
            g[frame | ribs, :3] = rgb(edge)
            g[frame, 3] = 0.95
            g[ribs & ~frame, 3] = 0.55
        elif kind == "spike":
            half = (tw / 2.0) * (0.12 + 0.88 * (ys / (th - 1.0)) ** 0.85)
            d = np.abs(xs + 0.5 - tw / 2.0)
            m = d <= half
            core = np.clip(1.0 - d / np.maximum(half, 0.5), 0, 1)
            for k in range(3):
                g[..., k] = np.where(m, rgb(color)[k] + (rgb(edge)[k] - rgb(color)[k]) * core, 0.0)
            g[..., 3] = np.where(m, 0.5 + 0.45 * core, 0.0) * np.clip(0.35 + ys / (th - 1.0), 0, 1)
        else:
            d = np.minimum(np.minimum(xs, tw - 1 - xs), np.minimum(ys, th - 1 - ys)) / (min(tw, th) / 2.0)
            core = np.clip(d, 0, 1)
            for k in range(3):
                g[..., k] = np.interp(core, [0.0, 0.5, 1.0], [rgb(color)[k], rgb(edge)[k], 1.0])
            g[..., 3] = 0.95
        return a, g
    return b.atlas.tile(("kk_ghost", kind, L.hexs(rgb(color)), L.hexs(rgb(edge)), tw, th, seed, rib), tw, th, paint)


# ================================================================================================ hair
def hair(b):
    """Silver hair: a thin cube shell on the skull, one flat tier, spikes of crossed cards that rise and sweep to the model's left (the viewer's right, over the covered eye) and back, a few chunky
    tufts for body, a fringe over the forehead and the side tufts in front of the ears."""
    g, t = 0.05, 0.4
    P.hair_shell(b, HAIR, g=g, thick=t, seed=61, front_low=30.6, side_low=28.4, back_low=25.6, side_front=-3.0)
    P.hair_tiers(b, HAIR, n=1, y0=32.35, w0=8.7, shrink=1.2, h=1.0, seed=62)
    geo = b.g("main")
    mass = b.t_mass(HAIR, seed=63)
    # tufts of crossed cards (x, z, lean to +x in degrees, lean back in degrees, length, width) in three rows, swept over the covered eye and back
    tufts = [(-2.6, -2.3, 10, 6, 4.8, 3.4), (-1.0, -2.8, 22, 4, 5.6, 3.6), (0.8, -2.4, 34, 6, 6.0, 3.8), (2.5, -1.8, 46, 8, 5.4, 3.6), (3.4, -0.2, 58, 10, 4.6, 3.2),
             (-2.8, -0.6, 8, 20, 5.0, 3.4), (-1.2, -0.7, 18, 22, 5.8, 3.8), (0.6, -0.4, 30, 24, 6.2, 3.8), (2.3, 0.4, 42, 26, 5.6, 3.6),
             (-2.4, 1.6, 8, 38, 4.8, 3.4), (-0.6, 1.9, 20, 40, 5.4, 3.6), (1.3, 2.3, 32, 44, 5.2, 3.4), (-1.5, 3.1, 12, 54, 4.2, 3.2), (0.6, 3.3, 24, 58, 4.0, 3.0)]
    for i, (x, z, az, ax, ln, w) in enumerate(tufts):
        tile = b.t_spike(HAIR, tw=16, th=28, seed=70 + i, tip=HAIR_TIP)
        y = 33.0
        rot = (ax, 0.0, -az)
        piv = (x, y, z)
        geo.cube(HEAD, (x - w / 2, y, z), (w, ln, 0.0), tile, faces=("north",), rot=rot, pivot=piv)
        geo.cube(HEAD, (x, y, z - w / 2), (0.0, ln, w), tile, faces=("east",), rot=rot, pivot=piv)
    mass = b.t_mass(HAIR, seed=63)
    P.spike(b, geo, HEAD, (0.4, 32.6, -1.6), 30, 8, 4.2, 4.2, mass, segs=2, depth=3.8)                    # one thick swoop for body in the middle
    P.fringe(b, HAIR, n=6, span=7.2, y_top=32.0, length=2.4, z=-4.55, seed=64, layer="fine", rot=10.0, w=1.9, part=1.2)
    for s in (-1, 1):                                                                     # side tufts in front of the ears
        tile = b.t_strand(HAIR, tw=12, th=32, seed=65 + s, prongs=3, tip=0.3)
        bone = b.sway("fine", HEAD, "tuft_%s" % ("l" if s > 0 else "r"), (s * 4.55, 29.6, -1.6), ax=2.0, az=1.4)
        b.g("fine").cube(bone, (s * 4.55, 26.4, -2.8), (0.0, 3.2, 2.4), tile, faces=("east",), pivot=(s * 4.55, 29.6, -1.6))


# ================================================================================================ hitai-ate and mask
def hitai(b, covered):
    """The forehead protector: navy cloth round the head with the steel Leaf plate on the front and the knot and two tails at the back. covered: the band runs diagonally over the model's LEFT eye
    (the viewer's right) and the left side slopes down to the ear; not covered: it is pushed up, straight across the forehead, slightly raised on the left."""
    geo = b.g("main")
    cloth = b.t_cloth(BAND, 32, 8, seed=44, folds=3, light=1.25, dark=0.7)
    cloth_d = b.t_cloth(BAND, 32, 16, seed=45, folds=3, light=1.2, dark=0.7)
    plate = t_plate(b)
    zf, zb = -4.62, 4.02
    th = 0.6
    if covered:
        y0, y1 = 29.3, 31.2
        geo.cube(HEAD, (-4.62, y0, zf), (4.5, y1 - y0, th), cloth)                                         # the right half, level across the brow
        geo.cube(HEAD, (-0.9, 25.8, zf - 0.04), (5.2, 2.9, th), cloth_d, rot=(0.0, 0.0, -40.0), pivot=(1.7, 27.4, zf))   # the covering piece, falling over the left eye
        geo.cube(HEAD, (-4.62, y0, zf), (th, y1 - y0, 9.24), cloth, faces=("north", "south", "east", "west", "up", "down"))   # the right side
        geo.cube(HEAD, (4.02, 28.1 - 0.95, -4.3), (th, 1.9, 8.6), cloth, rot=(-21.0, 0.0, 0.0), pivot=(4.32, 28.1, 0.0))      # the left side, sloping down to the front
        px = -2.3
        geo.cube(HEAD, (px - 1.7, y0 - 0.1, zf - 0.5), (3.4, 1.7, 0.5), plate, faces=("north",))
        geo.cube(HEAD, (px - 1.7, y0 - 0.1, zf - 0.5), (3.4, 1.7, 0.5), b.t_metal(STEEL, 16, 4, band=False, rivets=False), faces=("up", "down", "east", "west"))
    else:
        y0, y1 = 29.9, 31.9
        geo.cube(HEAD, (-4.62, y0, zf), (9.24, y1 - y0, th), cloth, rot=(0.0, 0.0, 5.0), pivot=(0.0, 30.9, zf))
        geo.cube(HEAD, (-4.62, y0, zf), (th, y1 - y0, 9.24), cloth, faces=("north", "south", "east", "west", "up", "down"))
        geo.cube(HEAD, (4.02, y0, zf), (th, y1 - y0, 9.24), cloth, faces=("north", "south", "east", "west", "up", "down"))
        px = -0.2
        geo.cube(HEAD, (px - 1.7, y0 - 0.05, zf - 0.5), (3.4, 1.7, 0.5), plate, faces=("north",), rot=(0.0, 0.0, 5.0), pivot=(px, 30.9, zf))
        geo.cube(HEAD, (px - 1.7, y0 - 0.05, zf - 0.5), (3.4, 1.7, 0.5), b.t_metal(STEEL, 16, 4, band=False, rivets=False), faces=("up", "down", "east", "west"), rot=(0.0, 0.0, 5.0), pivot=(px, 30.9, zf))
    ytop = 31.2 if covered else 31.9
    geo.cube(HEAD, (-4.62, y0, zb), (9.24, ytop - y0, th), cloth)                                           # the back
    geo.cube(HEAD, (1.5, y0 - 0.2, zb + th), (2.0, 1.7, 1.0), cloth_d)                                        # the knot
    for i, (xo, ln, ang) in enumerate(((2.0, 8.2, 6.0), (3.0, 6.0, -10.0))):                                   # the two tails
        tile = b.t_cloth(BAND, 16, 32, seed=46 + i, folds=2, hem="ragged")
        bone = b.sway("fine", HEAD, "bandtail_%d" % i, (xo, y0 + 1.2, zb + 1.0), ax=5.0, az=3.5, walk=2.0, run=3.0, phase=0.9 * i)
        b.g("fine").card(bone, xo, y0 + 1.3, zb + 1.2 + 0.1 * i, 2.1, ln, tile, rot=(0.0, 0.0, ang), pivot=(xo, y0 + 1.3, zb + 1.2))


def mask_cloth(b):
    """The cloth mask over the lower face: a slab over the cheeks and chin, a nose ridge, the sides wrapped back to the ears and the under-jaw, folds painted on its tile."""
    geo = b.g("main")
    cloth = b.t_cloth(SHIRT, 32, 16, seed=51, folds=3, light=1.25, dark=0.72)
    geo.cube(HEAD, (-4.32, 24.0, -4.58), (8.64, 2.55, 0.58), cloth)                                         # the cheek and chin slab
    geo.cube(HEAD, (-1.0, 25.3, -4.86), (2.0, 1.7, 0.36), cloth, rot=(6.0, 0.0, 0.0), pivot=(0.0, 25.3, -4.6))      # the nose ridge
    for s in (-1, 1):
        x0 = 4.0 if s > 0 else -4.58
        geo.cube(HEAD, (x0, 24.0, -4.0), (0.58, 2.55, 3.7), cloth)                                           # the side wrap
    geo.cube(HEAD, (-4.32, 23.62, -4.58), (8.64, 0.5, 4.2), cloth)                                           # under the jaw


# ================================================================================================ the flak jacket and the kit
def flak(b):
    """The flak jacket on the shirt (the front is painted on the skin): a high collar, four pocket flaps, the shoulder pads, a hem flap over the belt, the belt, the steel buckle."""
    geo = b.g("main")
    vest = b.t_cloth(VEST, 32, 16, seed=81, folds=4, light=1.2, dark=0.72)
    vest_l = b.t_cloth(VEST_L, 16, 16, seed=82, folds=2, light=1.2, dark=0.78)
    pad = b.t_cloth(PAD, 32, 16, seed=83, folds=3, light=1.25, dark=0.7)
    steel = b.t_metal(STEEL, 16, 16, band=True, rivets=False)
    geo.cube("body", (-3.4, 22.55, -2.55), (6.8, 1.9, 5.1), vest)                                           # the high collar
    for (fx0, fx1, fy0, fy1, lit) in ((0.7, 3.4, 2.6, 4.9, True), (4.6, 7.3, 2.6, 4.9, True), (0.7, 3.4, 5.5, 8.4, False), (4.6, 7.3, 5.5, 8.4, False)):    # pockets: face x -> model x = 4 - fx on the front
        x0, x1 = 4.0 - fx1, 4.0 - fx0
        geo.cube("body", (x0, 24.0 - fy1, -2.5), (x1 - x0, fy1 - fy0, 0.5), vest_l if lit else vest, faces=("north", "east", "west", "up", "down"))
        geo.cube("body", (x0, 24.0 - fy0 - 0.9, -2.52), (x1 - x0, 0.9, 0.32), pad, faces=("north", "up"))               # the flap
    for sd in ("r", "l"):
        s = -1 if sd == "r" else 1
        geo.cube("arm_" + sd, (s * 6.0 - 2.7, 22.1, -2.75), (5.4, 1.55, 5.5), pad, faces=("north", "south", "east", "west", "up", "down"))      # the shoulder pad
    geo.cube("body", (-4.35, 13.35, -2.45), (8.7, 1.4, 4.9), vest)                                          # the hem flap over the belt
    geo.cube("body", (-4.45, 12.1, -2.55), (8.9, 1.35, 5.1), t_leather(b))                                  # the belt
    geo.cube("body", (-1.0, 12.1, -2.8), (2.0, 1.4, 0.4), steel)                                            # the buckle
    geo.cube("body", (-3.4, 11.9, 1.9), (6.8, 3.6, 1.6), t_leather(b, 16, 16, 4))                           # the tool pouch at the lower back
    geo.cube("body", (-3.5, 14.8, 1.8), (7.0, 0.8, 1.8), steel)                                              # its flap strap


def guards(b):
    """Steel forearm guards on both arms (a plate over two dark straps) and a small plate of the glove on the back of each hand."""
    geo = b.g("main")
    steel = b.t_metal("#8e9ab2", 16, 24, band=True, rivets=True)
    strap = t_leather(b, 16, 8, 6)
    for sd in ("r", "l"):
        s = -1 if sd == "r" else 1
        cx = s * 6.0
        a = "arm_" + sd
        geo.cube(a, (cx - 2.5, 12.9, -2.5), (5.0, 5.2, 5.0), steel)
        geo.cube(a, (cx - 2.6, 12.6, -2.6), (5.2, 0.65, 5.2), strap)
        geo.cube(a, (cx - 2.6, 17.35, -2.6), (5.2, 0.65, 5.2), strap)


def legwear(b):
    """The tool holster on the right thigh with its strap, the bandage wraps on both shins and the sandal straps."""
    geo = b.g("main")
    band = t_bandage(b)
    leather = t_leather(b, 16, 16, 7)
    geo.cube("leg_r", (-5.15, 5.4, -1.7), (1.4, 5.0, 3.6), leather)                                         # the holster on the outer side of the right thigh
    geo.cube("leg_r", (-5.3, 9.3, -1.9), (1.7, 1.0, 3.9), t_leather(b, 16, 8, 8))
    geo.cube("leg_r", (-4.35, 9.5, -2.45), (4.7, 0.9, 4.9), leather)                                         # the strap round the thigh
    geo.cube("leg_r", (-4.35, 5.7, -2.45), (4.7, 0.9, 4.9), leather)
    for k in range(3):                                                                                       # the bandage under the holster (right thigh) and on the left shin
        geo.cube("leg_r", (-4.3, 6.2 + k * 1.05 + 0.2, -2.3), (4.6, 0.9, 4.6), band)
    for k in range(3):
        geo.cube("leg_l", (-0.3, 3.8 + k * 1.05, -2.3), (4.6, 0.9, 4.6), band)
    for sd in ("r", "l"):
        s = -1 if sd == "r" else 1
        geo.cube("leg_" + sd, (s * 1.9 - 2.35, 1.6, -2.7), (4.7, 0.5, 0.9), t_leather(b, 16, 8, 9), faces=("north", "up", "east", "west"))   # the strap over the foot


# ================================================================================================ the Perfect Susanoo
def susanoo(b):
    """The Perfect Susanoo as a translucent armoured giant round Kakashi (the `fx` layer, so it glows): a ribbed torso cage, big horned pauldrons, a crested helm round his head with two
    glowing eye slits, upper arms and forearms, a long sword held to his right, a flared skirt of plates and a cracked halo plate behind. Panels are frames and ribs with a faint fill.

    `fx` is the glowing layer; every texture here has a glow companion. About 330 quads (the audit allows 360 for this look)."""
    geo = b.g("fx")
    col, edge = "#4f78f0", "#8fb0ff"
    deep = "#7a58e8"
    panel = t_ghost(b, col, edge, 32, 32, seed=1, rib=6)
    panel_t = t_ghost(b, col, edge, 32, 16, seed=2, rib=5)
    purple = t_ghost(b, deep, "#b8a0ff", 32, 32, seed=3, rib=7)
    eye = t_ghost(b, "#ffd890", "#ff8a30", 16, 16, kind="solid")
    blade = t_ghost(b, "#cfe4ff", "#6a9cff", 8, 32, kind="solid")
    grip = t_ghost(b, "#7a58e8", "#b8a0ff", 8, 16, kind="solid")
    # ---- the torso cage: five ribs round the body (front plates, side plates, back plates)
    for k in range(5):
        y = 13.2 + k * 2.4
        w = 12.6 + 0.5 * math.sin(k * 0.9)
        geo.cube("body", (-w / 2, y, -4.4), (w, 2.0, 0.0), panel_t, faces=("north",))
        geo.cube("body", (-w / 2, y, 4.4), (w, 2.0, 0.0), panel_t, faces=("north",))
        for s in (-1, 1):
            geo.cube("body", (s * w / 2, y, -4.4), (0.0, 2.0, 8.8), panel_t, faces=("east",))
    geo.cube("body", (-0.5, 13.0, -4.5), (1.0, 12.0, 0.0), blade, faces=("north",))                      # the spine line down the front plate
    # ---- the pauldrons: a big plate with two spikes each
    for s in (-1, 1):
        geo.cube("body", (s * 9.2 - 4.2, 24.2, -3.6), (8.4, 0.0, 7.2), purple, faces=("up",))
        geo.cube("body", (s * 9.2 - 4.2, 24.2, -3.6), (8.4, 3.4, 0.0), panel, faces=("north",))
        geo.cube("body", (s * 9.2 - 4.2, 24.2, 3.6), (8.4, 3.4, 0.0), panel, faces=("north",))
        geo.cube("body", (s * 9.2 + s * 4.2, 21.0, -3.6), (0.0, 3.4, 7.2), panel, faces=("east",))
        for j, zz in enumerate((-1.4, 1.6)):
            sp = t_ghost(b, col, edge, 16, 24, seed=90 + j, kind="spike")
            geo.cube("body", (s * 9.2 - 1.3, 27.6, zz), (2.6, 7.0, 0.0), sp, faces=("north",), rot=(0.0, 0.0, -s * 18.0), pivot=(s * 9.2, 27.6, zz))
            geo.cube("body", (s * 9.2, 27.6, zz - 1.3), (0.0, 7.0, 2.6), sp, faces=("east",), rot=(0.0, 0.0, -s * 18.0), pivot=(s * 9.2, 27.6, zz))
    # ---- the helm: a mask plate over the face with two eye slits, cheek plates swept back, a stepped dome and a fan of crest spikes
    geo.cube("head", (-5.4, 24.2, -6.2), (10.8, 9.0, 0.0), panel, faces=("north",))
    for s in (-1, 1):
        geo.cube("head", (s * 5.4, 24.2, -6.2), (0.0, 9.0, 11.6), panel, faces=("east",))
        geo.cube("head", (s * 5.4, 24.2, -6.2), (s * 1.8, 0.0, 11.6), purple, faces=("up",)) if False else None
    geo.cube("head", (-5.4, 24.2, 5.4), (10.8, 9.0, 0.0), panel, faces=("north",))
    for k, w in enumerate((10.8, 8.4, 5.6)):
        geo.cube("head", (-w / 2, 33.2 + k * 1.7, -w / 2 - 0.6 * (k == 0)), (w, 0.0, w + 0.6 * (k == 0)), purple, faces=("up",))
        geo.cube("head", (-w / 2, 33.2 + k * 1.7, -w / 2 - 0.6 * (k == 0)), (w, 1.7, 0.0), panel_t, faces=("north",))
    sp = t_ghost(b, "#7a58e8", "#b8a0ff", 16, 32, seed=95, kind="spike")
    for j, ang in enumerate((-36.0, -18.0, 0.0, 18.0, 36.0)):                                          # the crest
        bx = ang / 36.0 * 2.6
        geo.cube("head", (bx - 1.3, 36.6, 0.0), (2.6, 8.0 - abs(ang) / 14.0, 0.0), sp, faces=("north",), rot=(0.0, 0.0, -ang * 0.8), pivot=(bx, 36.6, 0.0))
    for s in (-1, 1):
        geo.cube("head", (s * 2.3 - 1.4, 27.4, -6.3), (2.8, 1.0, 0.0), eye, faces=("north",))             # the eye slits
        geo.cube("head", (s * 2.3 - 1.4, 27.4, -6.3), (2.8, 1.0, 0.0), eye, faces=("north",), rot=(0.0, 0.0, s * 6.0), pivot=(s * 2.3, 27.9, -6.3))
    # ---- the arms: an upper arm from each pauldron, forearms reaching forward, a hand on the sword
    for s in (-1, 1):
        geo.cube("body", (s * 11.2 - 1.8, 15.0, -2.0), (3.6, 9.0, 0.0), panel, faces=("north",))
        geo.cube("body", (s * 11.2 - 1.8, 15.0, 2.0), (3.6, 9.0, 0.0), panel, faces=("north",))
        geo.cube("body", (s * 11.2 - 1.8, 15.0, -2.0), (0.0, 9.0, 4.0), panel, faces=("east",))
        geo.cube("body", (s * 11.2 + 1.8, 15.0, -2.0), (0.0, 9.0, 4.0), panel, faces=("east",))
    for s in (-1, 1):
        geo.cube("body", (s * 11.2 - 1.8, 14.9, -9.6), (3.6, 0.0, 7.6), purple, faces=("up",))               # the forearms, flat plates reaching forward
        geo.cube("body", (s * 11.2 - 1.8, 12.0, -9.6), (3.6, 3.0, 0.0), panel_t, faces=("north",))
    # ---- the sword, held upright in front of the right hand
    geo.cube("body", (-11.6, 10.6, -9.6), (1.6, 4.6, 0.0), grip, faces=("north",))
    geo.cube("body", (-11.6, 10.6, -9.6), (0.0, 4.6, 1.6), grip, faces=("east",))
    geo.cube("body", (-14.0, 15.0, -9.6), (6.4, 1.3, 0.0), blade, faces=("north",))                      # the guard
    for zz in (0.0,):
        geo.cube("body", (-11.7, 16.3, -9.6), (1.8, 30.0, 0.0), blade, faces=("north",))                 # the blade
        geo.cube("body", (-10.8, 16.3, -10.5), (0.0, 30.0, 1.8), blade, faces=("east",))
    # ---- the skirt: eight flared plates round the hips
    for k in range(8):
        ang = k * 45.0
        rx, rz = math.sin(math.radians(ang)), -math.cos(math.radians(ang))
        piv = (rx * 5.2, 13.0, rz * 5.2)
        geo.cube("body", (rx * 5.2 - 2.2, 1.0, rz * 5.2), (4.4, 12.0, 0.0), panel, faces=("north",), rot=(-rz * 14.0, -ang, rx * 14.0 * 0.0), pivot=piv)
    # ---- the back: a big halo ring of light behind him
    ring = b.t_ring("#4a74f0", 64, 64, rings=2, seed=5)
    geo.cube("body", (-13.0, 12.0, 6.6), (26.0, 26.0, 0.0), ring, faces=("north",))
