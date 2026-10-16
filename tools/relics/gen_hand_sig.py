"""gen_hand_sig: the signature hand models of the most distinctive relics, part 1: the Infinity Stones and the great artefacts of wave 30, the table and kitchen
things (spoon, pot, lantern, key), the music (lyres, horn), time and direction (watch, compass), and the collars, medallions and orbs.

Each function is one object, built with the owner's method (named parts, lofts, lathes, smoothed normals) in the frame described in gen_hand_kit.py, and the ASSIGN table says
which relic ids are held as which model. Run all the generators with tools/relics/gen_hand_all.py; look at one with
python3 tools/relics/hand_preview.py --model <name> [--palette main,trim,glow].
"""
import math

import numpy as np

import gen_hand_kit as K
from gen_hand_kit import (Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT, L, ml)

MODELS, model = K.registry()
ASSIGN = {}


# ================================================================ the six Infinity Stones
@model
def stone_space():
    """The Tesseract: a glowing cube held in a dark frame of twelve bars, a second bright cube turned half a quarter and slowly spinning so its corners break out of the faces, on four short prongs."""
    m = K.new("stone_space")
    h = 4.6
    K.block(m, "core", (-h + 0.5, -h + 0.5, -h + 0.5), (h - 0.5, h - 0.5, h - 0.5), GLOW, "crystal", 0.0, EMIT | HOT, uv="fit")
    g = m.group("inner", "spin", (0, 0, 0), 3.0, 0.0, 0.0)
    K.block(m, "inner", (-3.3, -3.3, -3.3), (3.3, 3.3, 3.3), NONE, "white", 0.0, EMIT, g, uv="fit")
    e = 0.45
    for a in (-h, h):
        for b in (-h, h):
            K.block(m, "ex%+d%+d" % (a, b), (-h - e, a - e, b - e), (h + e, a + e, b + e), DARK, "metal", 0.2)
            K.block(m, "ey%+d%+d" % (a, b), (a - e, -h - e, b - e), (a + e, h + e, b + e), DARK, "metal", 0.2)
            K.block(m, "ez%+d%+d" % (a, b), (a - e, b - e, -h - e), (a + e, b + e, h + e), DARK, "metal", 0.2)
    for i, (sx, sz) in enumerate(((-1, -1), (1, -1), (-1, 1), (1, 1))):
        K.spike(m, "prong%d" % i, (sx * h * 0.8, -h - 0.2, sz * h * 0.8), (sx * h * 0.45, -h - 4.2, sz * h * 0.45), 0.7, TRIM, "gilt", 4, 0.4, 0.8)
    return m


@model
def stone_mind():
    """The Mind Stone: a long yellow crystal like the tip of a sceptre, held in four gilt claws on a short fluted socket, a ring of light round its middle."""
    m = K.new("stone_mind")
    K.jewel(m, "crystal", (0, 3.5, 0), 3.0, GLOW, 2.3, 6, "y")
    K.ring(m, "halo", (0, 3.5, 0), 4.2, 0.25, ACCENT, "spark", "xz", 14, 4, EMIT)
    p = Part("socket")
    lathe(p, "y", (0, -4.2, 0), [(0.0, 1.0), (0.8, 2.4), (3.0, 1.7), (4.4, 2.5), (5.0, 2.9)], sides=8)
    m.add(p, TRIM, "gilt")
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        c, s = math.cos(a), math.sin(a)
        K.rod(m, "claw%d" % i, K.bezier((c * 2.7, 0.6, s * 2.7), (c * 3.6, 3.0, s * 3.6), (c * 2.0, 5.4, s * 2.0)), 0.38, TRIM, "gilt", 4, scales=[(1.0, 1.0), (0.9, 0.9), (0.5, 0.5)] + [(0.5, 0.5)] * 4)
    K.spike(m, "stub", (0, -4.4, 0), (0, -9.0, 0), 1.0, DARK, "metal", 6, 0.5, 0.9)
    return m


@model
def stone_power():
    """The Power Stone: a large violet gem cut in many facets, set in five crooked prongs on a flat hex plate, with a crackle of light lines across it."""
    m = K.new("stone_power")
    K.jewel(m, "gem", (0, 2.2, 0), 4.2, GLOW, 1.2, 5, "y")
    K.jewel(m, "core", (0, 2.2, 0), 2.5, NONE, 1.1, 5, "y", EMIT)
    K.disc(m, "plate", (0, -3.2, 0), 5.6, 1.4, DARK, "plate", "y", 6, 0, uv="fit")
    for i in range(5):
        a = 0.3 + i * 2 * math.pi / 5
        c, s = math.cos(a), math.sin(a)
        K.rod(m, "prong%d" % i, K.bezier((c * 4.5, -2.6, s * 4.5), (c * 5.6, 1.5, s * 5.6), (c * 3.4, 5.0, s * 3.4)), 0.5, TRIM, "gilt", 4,
              scales=[(1.0, 1.0), (1.0, 1.0), (0.9, 0.9), (0.7, 0.7), (0.5, 0.5), (0.4, 0.4), (0.3, 0.3), (0.3, 0.3)])
    return m


@model
def stone_time():
    """The Eye of Agamotto: a round gilt amulet with a green stone in it that two hinged eyelids half cover, a ring of fine runes round it and a chain loop above."""
    m = K.new("stone_time")
    K.ring(m, "rim", (0, 0, 0), 5.6, 0.7, TRIM, "gilt", "xy", 18, 5)
    K.ring(m, "inner", (0, 0, 0), 3.6, 0.35, ACCENT, "gilt", "xy", 14, 4)
    K.disc(m, "back", (0, 0, 0.9), 5.3, 0.8, MAIN, "plate", "z", 16, 0, uv="fit")
    K.ball(m, "stone", (0, 0, -0.6), 2.9, GLOW, "gem", 5, 8, 0.7, EMIT | HOT, uv="fit")

    def lid(sign):
        def fn(u, v):
            x = -5.0 + 10.0 * u
            top = 3.6 * math.sqrt(max(0.0, 1.0 - (x / 5.2) ** 2))
            y = sign * (1.5 + (top - 1.5) * v)
            return (x, y, -1.5 - 0.6 * math.sin(math.pi * v) + 0.7 * (1 - v))
        return fn
    for sign, name in ((1, "upper"), (-1, "lower")):
        p = Part("lid_" + name)
        L.sheet(p, lid(sign), 8, 3, -0.5, True)
        m.add(p, TRIM, "gilt")
    for i in range(10):
        a = i * 2 * math.pi / 10 + 0.3
        K.octa(m, "rune%d" % i, (6.5 * math.cos(a), 6.5 * math.sin(a), -0.2), 0.3, 0.3, 0.25, GLOW, EMIT)
    K.ring(m, "bail", (0, 6.9, 0.2), 0.8, 0.2, TRIM, "gilt", "yz", 6, 4)
    K.rod(m, "chain", K.bezier((-0.4, 7.5, 0.2), (-3.6, 11.0, 0.2), (0.0, 13.0, 0.2), (3.6, 11.0, 0.2), 8), 0.22, TRIM, "chain", 4)
    return m


@model
def stone_reality():
    """The Reality Stone, the Aether: a dark red orb that is half liquid, a ribbon of bright red wound round it that slowly turns, and two drips falling from its base."""
    m = K.new("stone_reality")
    K.ball(m, "orb", (0, 0, 0), 4.6, GLOW, "soft", 6, 10, 1.0, EMIT | HOT, uv="fit")
    K.ball(m, "skin", (0, 0, 0), 4.9, MAIN, "pearl", 5, 8, 1.0, 0, uv="fit")
    g = m.group("swirl", "spin", (0, 0, 0), 2.2, 0.0, 0.0)
    K.rod(m, "ribbon", K.helix((0, 0, 0), 5.3, -3.6, 3.6, 2.2, 28), 0.5, ACCENT, "spark", 4, EMIT, g, scales=[(0.4, 0.4)] + [(1.0, 1.0)] * 26 + [(0.4, 0.4)])
    for i, (x, z, ln) in enumerate(((1.4, -0.8, 3.2), (-1.6, 1.2, 2.4))):
        K.spike(m, "drip%d" % i, (x, -4.3, z), (x, -4.3 - ln, z), 0.6, GLOW, "soft", 5, 0.3, 1.0, EMIT)
    p = Part("cup")
    lathe(p, "y", (0, -5.2, 0), [(0.0, 0.0), (0.4, 2.2), (1.4, 3.4), (2.0, 3.8)], sides=10, caps=(False, False))
    m.add(p, TRIM, "gilt")
    return m


@model
def stone_soul():
    """The Soul Stone: a warm orange crystal prism with a wisp of light curling inside it, standing in an open gilt ring on three feet."""
    m = K.new("stone_soul")
    K.jewel(m, "prism", (0, 1.5, 0), 3.0, GLOW, 2.0, 6, "y")
    g = m.group("wisp", "bob", (0, 1.5, 0), 0.5, 0.09, 0.0)
    K.rod(m, "wisp", K.bezier((0, -1.4, 0), (1.4, 0.4, -0.8), (-1.2, 2.4, 0.8), (0.2, 4.6, 0), 8), 0.55, NONE, "white", 5, EMIT, g, scales=[(0.4, 0.4), (1, 1), (1, 1), (0.9, 0.9), (0.8, 0.8), (0.6, 0.6), (0.4, 0.4), (0.3, 0.3), (0.2, 0.2)])
    K.ring(m, "ring", (0, -3.4, 0), 3.7, 0.55, TRIM, "gilt", "xz", 12, 4)
    for i in range(3):
        a = i * 2 * math.pi / 3 + 0.5
        K.spike(m, "foot%d" % i, (3.4 * math.cos(a), -3.2, 3.4 * math.sin(a)), (2.4 * math.cos(a), -6.4, 2.4 * math.sin(a)), 0.55, TRIM, "gilt", 4, 0.4, 0.9)
    return m


# ================================================================ the great hand artefacts
def _finger(m, name, base, tip, r, curl, slot, tile, segs=2, sides=5):
    """A finger or claw: a tapered bar from base to tip, bent by `curl` (a bend towards -z, the front) and closed at the end."""
    b, t = np.array(base, float), np.array(tip, float)
    mid = (b + t) / 2 + np.array([0, 0, -curl])
    pts = K.bezier(tuple(b), tuple(mid), tuple(t), segs + 2)
    n = len(pts)
    sc = [(1.0 - 0.45 * i / (n - 1), 1.0 - 0.45 * i / (n - 1)) for i in range(n)]
    sc[-1] = (0.35, 0.35)
    return K.rod(m, name, pts, r, slot, tile, sides, scales=sc)


@model
def infinity_gauntlet():
    """The Infinity Gauntlet: a gilt gauntlet seen from the back of the hand, fingers up, a fluted cuff, knuckle plates, and six sockets across the back each holding a different cut stone."""
    m = K.new("infinity_gauntlet")
    p = Part("cuff")
    loft(p, "y", (0, -12.0, 0), [(0.0, 4.6, 3.8), (3.0, 4.0, 3.3), (8.0, 4.7, 3.9)], L.octagon(1.0, 1.0, 0.18))
    m.add(p, MAIN, "plate", 0, 0, "fit")
    K.cyl(m, "cuffband", (0, -12.0, 0), 5.2, 1.2, TRIM, "gilt", sides=10)
    K.cyl(m, "cuffband2", (0, -5.0, 0), 4.7, 0.9, TRIM, "gilt", sides=10)
    K.block(m, "palm", (-4.0, -4.0, -2.4), (4.0, 3.0, 2.4), MAIN, "plate", 0.5, uv="fit")
    K.block(m, "knuckles", (-4.3, 2.6, -2.8), (4.3, 4.2, 2.2), TRIM, "gilt", 0.0)
    for i, x in enumerate((-3.0, -1.0, 1.0, 3.0)):
        ln = (6.0, 7.2, 6.6, 5.2)[i]
        _finger(m, "f%d" % i, (x, 3.6, -0.4), (x, 3.6 + ln, -1.4), 0.95, 0.9, MAIN, "plate")
        K.block(m, "fp%d" % i, (x - 0.9, 5.4, -1.9), (x + 0.9, 5.9, 0.3), TRIM, "gilt", 0.0)
    _finger(m, "thumb", (4.0, -1.0, -0.6), (7.6, 3.2, -1.6), 1.1, 0.8, MAIN, "plate")
    slots = [(GLOW, "gem"), (ACCENT, "gem"), (TRIM, "gem"), (NONE, "duck"), (NONE, "orange"), (GLOW, "crystal")]
    pos = [(-3.0, 1.0), (0.0, 1.8), (3.0, 1.0), (-2.0, -2.2), (2.0, -2.2), (0.0, -0.4)]
    for i, ((sl, tl), (x, y)) in enumerate(zip(slots, pos)):
        K.disc(m, "socket%d" % i, (x, y, -2.6), 1.45, 0.8, DARK, "metal", "z", 6, 0.0)
        c = (x, y, -3.3)
        if i == 0:
            K.block(m, "g%d" % i, (x - 0.9, y - 0.9, -4.3), (x + 0.9, y + 0.9, -2.9), sl, tl, 0.1, EMIT | HOT, uv="fit")
        elif i == 1:
            K.jewel(m, "g%d" % i, c, 1.0, sl, 1.5, 6, "z")
        elif i == 2:
            K.octa(m, "g%d" % i, c, 1.1, 1.1, 1.1, sl, EMIT | HOT)
        elif i == 3:
            K.jewel(m, "g%d" % i, c, 0.95, sl, 1.3, 6, "z", EMIT, 0, "duck")
        elif i == 4:
            K.ball(m, "g%d" % i, c, 1.0, sl, tl, 4, 8, 0.8, EMIT, uv="fit")
        else:
            K.jewel(m, "g%d" % i, c, 0.9, sl, 1.7, 5, "z")
    return m


@model
def hand_of_vecna():
    """The Hand of Vecna: a withered, mummified left hand cut off at the wrist, bound in dark rags, its long dry fingers curled like claws and a red light sealed in the palm."""
    m = K.new("hand_of_vecna")
    p = Part("wrist")
    loft(p, "y", (0, -10.0, 0), [(0.0, 2.6, 2.2), (4.0, 2.9, 2.4), (6.0, 3.4, 2.7)], L.circle(1.0, 8))
    m.add(p, DARK, "leather")
    for i in range(3):
        K.cyl(m, "rag%d" % i, (0, -9.0 + i * 2.2, 0), 3.0 + 0.1 * i, 1.0, MAIN, "cloth", sides=9)
    K.block(m, "palm", (-3.6, -4.2, -2.0), (3.6, 3.4, 2.0), MAIN, "bone", 0.9)
    K.jewel(m, "seal", (0, -0.4, -2.3), 1.3, GLOW, 0.6, 6, "z")
    for i, x in enumerate((-2.7, -0.9, 0.9, 2.7)):
        ln = (6.4, 7.6, 7.0, 5.6)[i]
        _finger(m, "f%d" % i, (x, 3.2, 0.0), (x + (0.3 if i < 2 else -0.3), 3.2 + ln, -2.4), 0.82, 2.0, MAIN, "bone", 4)
        K.spike(m, "nail%d" % i, (x, 3.2 + ln - 0.6, -2.4), (x, 3.2 + ln + 1.7, -3.4), 0.5, DARK, "horn", 4, 0.4, 0.8)
    _finger(m, "thumb", (3.5, -1.5, 0.3), (6.6, 1.5, -2.0), 0.95, 1.4, MAIN, "bone", 3)
    K.spike(m, "thumbnail", (6.6, 1.5, -2.0), (8.0, 3.0, -2.8), 0.45, DARK, "horn", 4, 0.4, 0.8)
    return m


@model
def eye_of_vecna_hand():
    """The Eye of Vecna: a bloodshot eyeball with a slit pupil and a ring of gold round its iris, a short dried optic cord, and a bracket of three thin claws that hold it up."""
    m = K.new("eye_of_vecna_hand")
    K.ball(m, "eye", (0, 0, 0), 5.0, NONE, "white", 6, 10, 1.0, 0, uv="fit")
    K.disc(m, "iris", (0, 0, -4.7), 2.7, 0.7, GLOW, "soft", "z", 12, EMIT | HOT, 0.15, uv="fit")
    K.block(m, "pupil", (-0.35, -1.7, -5.2), (0.35, 1.7, -4.6), DARK, "black", 0.1, uv="fit")
    K.ring(m, "irisring", (0, 0, -4.9), 2.9, 0.3, TRIM, "gilt", "xy", 14, 4)
    for i in range(7):
        a = 0.5 + i * 0.85
        pts = [(3.0 * math.cos(a) * s, 3.0 * math.sin(a) * s, -4.2 + 1.2 * (1 - s)) for s in (0.2, 0.5, 0.8, 1.0)]
        K.rod(m, "vein%d" % i, [(x * 1.45, y * 1.45, z - 0.1) for x, y, z in pts], 0.16, MAIN, "vein", 3)
    K.rod(m, "cord", K.bezier((0, -4.6, 2.4), (0.8, -7.0, 3.4), (-1.0, -9.0, 2.0), (0.6, -11.0, 1.0), 7), 1.0, MAIN, "vein", 6,
          scales=[(1.2, 1.2), (1.0, 1.0), (0.9, 0.9), (0.9, 0.9), (0.8, 0.8), (0.7, 0.7), (0.6, 0.6), (0.5, 0.5)])
    for i in range(3):
        a = 2.1 + i * 2.1
        K.rod(m, "claw%d" % i, K.bezier((2.4 * math.cos(a), -5.4, 2.4 * math.sin(a)), (6.6 * math.cos(a), -1.0, 6.6 * math.sin(a)), (4.6 * math.cos(a), 4.4, 4.6 * math.sin(a))),
              0.35, TRIM, "gilt", 4)
    return m


@model
def deck_of_many_things():
    """The Deck of Many Things: a black leather card case with a gilt clasp and a glowing sigil on the lid, and one card sliding half out of it showing a strange face."""
    m = K.new("deck_of_many_things")
    K.block(m, "case", (-4.6, -6.4, -1.8), (4.6, 6.4, 1.8), MAIN, "leather", 0.6)
    K.block(m, "lidline", (-4.7, 3.0, -1.9), (4.7, 3.5, 1.9), DARK, "metal", 0.1)
    K.block(m, "clasp", (-1.4, 2.2, -2.1), (1.4, 4.4, -1.7), TRIM, "gilt", 0.3)
    K.octa(m, "sigil", (0, -1.2, -2.0), 2.0, 2.6, 0.5, GLOW, EMIT | HOT)
    K.ring(m, "sigilring", (0, -1.2, -1.9), 3.0, 0.22, TRIM, "gilt", "xy", 14, 4)
    for sx in (-1, 1):
        for sy in (-1, 1):
            K.block(m, "corner%d%d" % (sx, sy), (sx * 4.9 - 0.7, sy * 6.7 - 0.7, -2.0), (sx * 4.9 + 0.7, sy * 6.7 + 0.7, 2.0), TRIM, "gilt", 0.2)
    K.block(m, "card", (-3.4, 5.5, -0.6), (3.4, 12.5, -0.2), NONE, "paper", 0.0, uv="fit")
    K.block(m, "cardface", (-2.6, 6.5, -0.7), (2.6, 11.5, -0.55), ACCENT, "plate", 0.0, uv="fit")
    K.jewel(m, "cardgem", (0, 9.0, -0.8), 1.3, GLOW, 1.1, 4, "z", EMIT)
    return m


@model
def warrens_key():
    """Warren's Key: an old key with a trefoil bow set with a glowing stone, a long grooved shaft with two collars, and a bit cut in steps; it hangs from a loop of cord."""
    m = K.new("warrens_key")
    for i in range(3):
        a = math.pi / 2 + i * 2 * math.pi / 3
        K.ring(m, "bow%d" % i, (2.0 * math.cos(a), 8.6 + 2.0 * math.sin(a), 0), 2.0, 0.55, TRIM, "gilt", "xy", 10, 4)
    K.jewel(m, "gem", (0, 8.6, -0.4), 1.1, GLOW, 1.3, 6, "z")
    K.cyl(m, "shaft", (0, 0.0, 0), 0.95, 12.0, MAIN, "gilt", "y", 8)
    K.cyl(m, "collar1", (0, 5.2, 0), 1.55, 1.1, TRIM, "gilt", "y", 8)
    K.cyl(m, "collar2", (0, 1.4, 0), 1.45, 0.9, TRIM, "gilt", "y", 8)
    for i, (y0, ln) in enumerate(((-6.0, 3.2), (-3.8, 2.4), (-1.6, 3.4))):
        K.block(m, "tooth%d" % i, (0.7, y0, -0.8), (0.7 + ln, y0 + 1.3, 0.8), MAIN, "gilt", 0.15)
    K.rod(m, "cord", K.bezier((0, 11.0, 0), (-3.0, 14.0, 0), (3.0, 15.0, 0), (0, 13.0, 0), 6), 0.18, ACCENT, "leather", 4)
    return m


# ================================================================ the table, the kitchen, the lantern
@model
def soup_spoon():
    """The Soup Spoon of Improbability: a long silver spoon, a deep round bowl with a swirl of light in it, and a handle that curls back on itself into a spiral at the top."""
    m = K.new("soup_spoon")
    p = Part("bowl")
    lathe(p, "z", (0, -8.2, 0.9), [(-1.0, 3.3), (0.0, 3.0), (1.2, 2.1), (2.2, 0.9), (2.6, 0.0)], sides=12, squash=1.35)
    m.add(p, MAIN, "metal")
    K.disc(m, "soup", (0, -8.2, -0.12), 2.9, 0.3, GLOW, "soft", "z", 12, EMIT | HOT, 0.1, uv="fit")
    K.rod(m, "swirl", [(1.9 * math.cos(2.4 * math.pi * t + 0.3) * (1 - 0.8 * t), -8.2 + 2.6 * math.sin(2.4 * math.pi * t + 0.3) * (1 - 0.8 * t), -0.35) for t in [i / 15 for i in range(16)]],
          0.2, NONE, "white", 4, EMIT)
    K.rod(m, "neck", [(0, -5.2, 0.9), (0, -2.8, 0.7), (0, 0.6, 0.2), (0.1, 5.0, 0.0), (0.4, 8.4, -0.4)], 0.8, MAIN, "metal", 6,
          scales=[(1.9, 0.9), (1.2, 1.0), (0.9, 1.0), (1.0, 1.0), (1.2, 1.2)])
    pts = []
    for i in range(22):
        t = i / 21
        a = 3.2 * t * math.pi
        r = 3.2 - 2.6 * t
        pts.append((0.6 + r * math.sin(a), 8.8 + r * math.cos(a) * 0.9 - 1.0 + 0.0, -0.4 - 0.0))
    K.rod(m, "curl", pts, 0.78, MAIN, "metal", 6, scales=[(1.0, 1.0)] * 18 + [(0.8, 0.8), (0.7, 0.7), (0.6, 0.6), (0.5, 0.5)])
    K.ball(m, "tip", (pts[-1][0], pts[-1][1], pts[-1][2]), 0.9, TRIM, "gilt", 3, 6)
    return m


@model
def cooking_pot():
    """The Instant Cooking Pot: a fat iron pot with a lid on top, a knob on the lid, two ring handles on the sides and a glowing rune on the belly, steam curling up from the lid."""
    m = K.new("cooking_pot")
    p = Part("body")
    lathe(p, "y", (0, -5.0, 0), [(0.0, 3.4), (1.0, 5.4), (4.0, 6.6), (7.0, 6.2), (8.0, 5.6)], sides=12)
    m.add(p, MAIN, "metal")
    K.cyl(m, "rim", (0, 3.0, 0), 5.9, 0.9, DARK, "metal", sides=12)
    p = Part("lid")
    lathe(p, "y", (0, 3.4, 0), [(0.0, 5.7), (0.6, 5.9), (1.6, 4.8), (2.6, 2.4), (3.0, 0.9)], sides=12)
    m.add(p, TRIM, "gilt")
    K.ball(m, "knob", (0, 6.9, 0), 1.0, DARK, "metal", 3, 6)
    for sx in (-1, 1):
        K.ring(m, "handle%d" % sx, (sx * 7.4, 0.8, 0), 1.7, 0.45, DARK, "chain", "xy", 10, 4)
    K.octa(m, "rune", (0, -1.0, -6.7), 1.6, 1.8, 0.4, GLOW, EMIT | HOT)
    K.ring(m, "runering", (0, -1.0, -6.5), 2.4, 0.2, TRIM, "gilt", "xy", 12, 4)
    g = m.group("steam", "bob", (0, 8.0, 0), 0.5, 0.12, 0.0)
    for i, (x, z) in enumerate(((-0.6, 0.0), (0.7, 0.4))):
        K.rod(m, "steam%d" % i, K.bezier((x, 7.6, z), (x + 1.4, 9.6, z), (x - 1.2, 11.6, z), (x + 0.6, 13.6, z), 7), 0.5, NONE, "soft", 5, EMIT, g,
              scales=[(0.5, 0.5), (1.0, 1.0), (1.2, 1.2), (1.2, 1.2), (1.0, 1.0), (0.8, 0.8), (0.6, 0.6), (0.3, 0.3)])
    return m


@model
def spirit_lantern():
    """The Spirit Lantern: a tall iron lantern with a pointed cap and a ring on top, four slender posts, glass sides and a small ghostly flame hanging in the middle, a lamp-hook at the base."""
    m = K.new("spirit_lantern")
    K.cyl(m, "base", (0, -6.4, 0), 3.8, 1.4, MAIN, "metal", sides=8)
    K.cyl(m, "baseflare", (0, -5.4, 0), 4.2, 0.7, DARK, "metal", sides=8)
    p = Part("cap")
    lathe(p, "y", (0, 5.0, 0), [(0.0, 4.6), (0.6, 4.7), (1.4, 3.6), (3.0, 1.3), (3.6, 0.6)], sides=8)
    m.add(p, MAIN, "metal")
    K.ring(m, "ring", (0, 9.8, 0), 1.2, 0.3, TRIM, "gilt", "yz", 8, 4)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        K.cyl(m, "post%d" % i, (3.8 * math.cos(a), -0.2, 3.8 * math.sin(a)), 0.5, 10.0, DARK, "metal", sides=6)
    p = Part("glass")
    lathe(p, "y", (0, -4.8, 0), [(0.0, 3.5), (1.0, 3.7), (8.0, 3.7), (9.6, 3.5)], sides=8, caps=(False, False))
    m.add(p, ACCENT, "crystal", EMIT, uv="fit")
    g = m.group("flame", "pulse", (0, -1.0, 0), 0.12, 0.2, 0.0)
    p = Part("flame")
    lathe(p, "y", (0, -3.2, 0), [(0.0, 0.0), (0.8, 1.6), (2.0, 1.5), (3.6, 0.8), (5.0, 0.0)], sides=7, caps=(True, True))
    m.add(p, GLOW, "flame", EMIT | HOT, g, "fit")
    K.ball(m, "core", (0, -1.0, 0), 0.8, NONE, "white", 3, 6, 1.0, EMIT, g)
    K.cyl(m, "wick", (0, -4.0, 0), 0.3, 1.4, DARK, "metal", sides=4)
    return m


@model
def lyre_bewitching():
    """The Bewitching Lyre: a classical lyre with a rounded sound box, two curved arms that end in small curls, a crossbar and seven fine glowing strings."""
    m = K.new("lyre_bewitching")
    p = Part("box")
    lathe(p, "y", (0, -9.0, 0), [(0.0, 1.8), (1.0, 3.0), (3.0, 3.6), (4.6, 2.6), (5.4, 1.4)], sides=10, squash=0.55)
    m.add(p, MAIN, "wood")
    K.disc(m, "plate", (0, -6.6, -1.7), 2.2, 0.4, TRIM, "gilt", "z", 10)
    for sx in (-1, 1):
        K.rod(m, "arm%d" % sx, K.bezier((sx * 2.8, -4.4, 0), (sx * 5.8, 0.0, 0), (sx * 5.2, 5.0, 0), (sx * 3.6, 9.2, 0), 8), 0.7, MAIN, "wood", 6,
              scales=[(1.2, 1.2)] + [(1.0, 1.0)] * 6 + [(0.9, 0.9)])
        K.ring(m, "curl%d" % sx, (sx * 3.0, 9.8, 0), 0.8, 0.3, TRIM, "gilt", "xy", 8, 4)
    K.cyl(m, "bar", (0, 9.4, 0), 0.5, 7.0, TRIM, "gilt", "x", 6)
    K.cyl(m, "bridge", (0, -4.4, 0), 0.4, 5.6, TRIM, "gilt", "x", 6)
    for i in range(7):
        x = -2.7 + i * 0.9
        K.cyl(m, "string%d" % i, (x, 2.5, 0), 0.1, 13.4, GLOW, "spark", "y", 4, EMIT)
    return m


@model
def lyre_silver_tongued():
    """The Silver-Tongued Lyre: a slim lyre whose arms are two swan necks of silver turning outward, a tall narrow sound box, a pearl at the foot and five bright strings."""
    m = K.new("lyre_silver_tongued")
    p = Part("box")
    lathe(p, "y", (0, -10.0, 0), [(0.0, 1.2), (1.4, 2.6), (3.6, 2.8), (5.6, 1.6), (6.4, 0.9)], sides=8, squash=0.5)
    m.add(p, MAIN, "metal")
    K.ball(m, "pearl", (0, -10.4, 0), 1.0, NONE, "pearl", 3, 6, 1.0, 0, uv="fit")
    for sx in (-1, 1):
        K.rod(m, "neck%d" % sx, K.bezier((sx * 1.6, -3.4, 0), (sx * 5.2, 0.0, 0), (sx * 1.8, 5.0, 0), (sx * 4.0, 9.0, 0), 9), 0.6, TRIM, "metal", 6,
              scales=[(1.2, 1.2)] + [(1.0, 1.0)] * 7 + [(0.7, 0.7)])
        K.ball(m, "head%d" % sx, (sx * 4.4, 9.6, 0), 0.9, TRIM, "metal", 3, 6, 0.8)
        K.spike(m, "beak%d" % sx, (sx * 4.9, 9.7, 0), (sx * 6.4, 9.3, 0), 0.35, DARK, "gilt", 4, 0.4, 0.9)
    K.cyl(m, "bar", (0, 9.0, 0), 0.4, 6.6, DARK, "metal", "x", 6)
    for i in range(5):
        x = -2.0 + i
        K.cyl(m, "string%d" % i, (x, 2.8, 0), 0.1, 12.4, GLOW, "spark", "y", 4, EMIT)
    return m


@model
def horn_of_leroy():
    """The Horn of Leroy: a big curved war horn with a flared bell, three gilt bands, a leather strap loop and a mouthpiece, a faint glow breathing out of the bell."""
    m = K.new("horn_of_leroy")
    pts = K.bezier((7.0, -7.0, 0), (5.0, -1.0, 0), (-1.0, 3.0, 0), (-6.0, 6.0, 0), 12)
    sc = []
    for i in range(len(pts)):
        t = i / (len(pts) - 1)
        sc.append((0.5 + 2.6 * t ** 1.6, 0.5 + 2.6 * t ** 1.6))
    K.rod(m, "horn", pts, 1.0, MAIN, "horn", 8, scales=sc)
    K.disc(m, "bell", (-6.2, 6.3, 0), 3.9, 0.6, TRIM, "gilt", "x", 10)
    for i, t in enumerate((0.35, 0.58, 0.8)):
        k = int(t * (len(pts) - 1))
        K.ring(m, "band%d" % i, pts[k], 1.0 + 2.6 * t ** 1.6, 0.28, TRIM, "gilt", "yz", 10, 4)
    K.cyl(m, "mouth", (7.1, -7.6, 0), 0.9, 1.8, DARK, "metal", "y", 8)
    K.rod(m, "strap", K.bezier((3.6, -4.0, 0), (0.0, -9.0, 0.6), (-4.0, -4.0, 0.6), (-2.2, 1.8, 0.2), 7), 0.4, ACCENT, "leather", 4)
    g = m.group("breath", "pulse", (-6.5, 6.8, 0), 0.2, 0.15, 0.0)
    K.disc(m, "glow", (-6.9, 6.7, 0), 3.0, 0.2, GLOW, "soft", "x", 10, EMIT | HOT, 0.0, uv="fit")
    return m


# ================================================================ time and direction, luck and flowers
@model
def pocket_watch():
    """The Pocket Time: a round golden hunter watch with a domed glass front, a ring of twelve hour marks and two hands, a crown and bow on top and a short chain."""
    m = K.new("pocket_watch")
    p = Part("case")
    lathe(p, "z", (0, 0, 0), [(-1.8, 4.6), (-1.0, 5.6), (1.0, 5.6), (1.8, 4.6)], sides=18)
    m.add(p, TRIM, "gilt")
    K.disc(m, "dial", (0, 0, -1.4), 4.5, 0.4, NONE, "paper", "z", 18, 0, 0.1, uv="fit")
    K.ball(m, "glass", (0, 0, -1.2), 4.6, ACCENT, "crystal", 3, 12, 0.18, 0, uv="fit")
    for i in range(12):
        a = i * math.pi / 6
        big = i % 3 == 0
        K.block(m, "mark%d" % i, (-0.2 * (1.6 if big else 1) + 3.3 * math.sin(a) * 1.0, 3.3 * math.cos(a) - 0.3, -1.9), (0.2 * (1.6 if big else 1) + 3.3 * math.sin(a), 3.3 * math.cos(a) + 0.3 + (0.3 if big else 0), -1.6), DARK, "metal", 0.0)
    g = m.group("hands", "spin", (0, 0, 0), 0.0, 0.0, 0.0)
    K.block(m, "hand1", (-0.25, 0, -2.2), (0.25, 3.0, -1.9), DARK, "metal", 0.0)
    K.block(m, "hand2", (0.0, -0.3, -2.3), (2.2, 0.2, -2.0), DARK, "metal", 0.0)
    K.ball(m, "pin", (0, 0, -2.3), 0.5, TRIM, "gilt", 3, 6)
    K.cyl(m, "crown", (0, 6.6, 0), 0.9, 1.8, TRIM, "gilt", "y", 8)
    K.ring(m, "bow", (0, 8.6, 0), 1.7, 0.4, TRIM, "gilt", "yz", 10, 4)
    K.rod(m, "chain", K.bezier((0, 10.3, 0), (-3.5, 13.0, 0.4), (3.5, 14.0, -0.4), (0.5, 16.0, 0), 8), 0.2, TRIM, "chain", 4)
    return m


@model
def monster_compass():
    """The Monster Compass: a brass compass in a round case with its lid hinged open at the back, a glass over a card with a red and a pale needle that swings, and a loop on top."""
    m = K.new("monster_compass")
    p = Part("case")
    lathe(p, "z", (0, 0, 0), [(-1.8, 4.2), (-1.0, 5.2), (1.0, 5.2), (1.8, 4.2)], sides=16)
    m.add(p, MAIN, "gilt")
    K.disc(m, "card", (0, 0, -1.3), 4.3, 0.4, NONE, "paper", "z", 16, 0, 0.1, uv="fit")
    K.ring(m, "glassring", (0, 0, -1.5), 4.4, 0.25, TRIM, "gilt", "xy", 16, 4)
    for i, n in enumerate(("n", "e", "s", "w")):
        a = i * math.pi / 2
        K.block(m, "t" + n, (3.3 * math.sin(a) - 0.25, 3.3 * math.cos(a) - 0.25, -1.9), (3.3 * math.sin(a) + 0.25, 3.3 * math.cos(a) + 0.25 + 0.5, -1.6), TRIM, "gilt", 0.0)
    g = m.group("needle", "sway", (0, 0, 0), 25.0, 0.06, 0.0)
    p = Part("needle_n")
    loft(p, "y", (0, 0, -2.2), [(0.0, 0.7, 0.2), (3.4, 0.0, 0.0)], L.circle(1.0, 4))
    m.add(p, GLOW, "gem", EMIT | HOT, g, "fit")
    p = Part("needle_s")
    loft(p, "y", (0, 0, -2.2), [(0.0, 0.7, 0.2), (-3.4, 0.0, 0.0)], L.circle(1.0, 4))
    m.add(p, NONE, "white", 0, g, "fit")
    K.ball(m, "pivot", (0, 0, -2.3), 0.45, TRIM, "gilt", 3, 6)
    K.ring(m, "loop", (0, 6.7, 0), 1.3, 0.35, TRIM, "gilt", "yz", 10, 4)
    K.cyl(m, "stem", (0, 5.5, 0), 0.7, 1.4, TRIM, "gilt", "y", 6)
    K.disc(m, "lid", (0, 0, 2.6), 5.0, 0.6, MAIN, "gilt", "z", 16)
    return m


@model
def lucky_egg():
    """The Lucky Egg: a black egg banded in silver, a four-leaf clover shown on its front in light, and a small gold ring at the top with a chain."""
    m = K.new("lucky_egg")
    p = Part("egg")
    lathe(p, "y", (0, -5.5, 0), [(0.0, 0.0), (0.7, 2.2), (2.2, 3.9), (4.6, 4.7), (7.0, 4.1), (9.0, 2.6), (10.4, 1.0), (11.0, 0.0)], sides=12)
    m.add(p, MAIN, "pearl", 0, 0, "fit")
    K.cyl(m, "band1", (0, -1.0, 0), 4.7, 0.7, TRIM, "metal", sides=12)
    K.cyl(m, "band2", (0, 3.0, 0), 4.5, 0.7, TRIM, "metal", sides=12)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        K.ball(m, "leaf%d" % i, (1.4 * math.cos(a), 1.0 + 1.4 * math.sin(a), -4.4), 1.0, GLOW, "soft", 3, 6, 0.4, EMIT | HOT, uv="fit")
    K.ring(m, "ring", (0, 6.4, 0), 0.9, 0.3, ACCENT, "gilt", "yz", 8, 4)
    K.rod(m, "chain", K.bezier((0, 7.2, 0), (-3.0, 10.0, 0), (3.0, 11.5, 0), (0, 13.2, 0), 7), 0.2, ACCENT, "chain", 4)
    return m


@model
def rose_petal():
    """The Rose Petal: one huge curved petal, ridged with fine veins, glowing along its edge, its base pinched into a short stalk that is bound with a silver thread."""
    m = K.new("rose_petal")

    def petal(u, v):
        x = (u - 0.5) * 2 * (2.0 + 5.4 * math.sin(math.pi * min(1.0, v * 0.95 + 0.05)) ** 0.8)
        y = -9.0 + 18.0 * v
        z = -1.8 * math.sin(math.pi * u) * (0.3 + v) + 2.2 * (v - 0.5) ** 2
        return (x, y, z)
    p = Part("petal")
    L.sheet(p, petal, 10, 8, 0.35, True)
    m.add(p, MAIN, "silk", 0, 0, "tile")
    K.rod(m, "midrib", [(0, -8.5, 0.2), (0, -3.0, -1.6), (0, 3.0, -2.2), (0, 8.6, -0.6)], 0.25, ACCENT, "vein", 4)
    for s in (-1, 1):
        K.rod(m, "vein%d" % s, [(0, -5.0, -0.8), (s * 2.0, 0.0, -1.7), (s * 3.4, 5.0, -1.6)], 0.14, ACCENT, "vein", 3)
    g = m.group("shine", "pulse", (0, 0, -2.0), 0.08, 0.07, 0.0)
    K.rod(m, "edge", [(-2.6, 9.0, -0.4), (-5.2, 4.0, -1.0), (-6.4, -1.0, -1.1), (-5.0, -6.0, -0.6), (-2.0, -9.0, 0.2)], 0.3, GLOW, "spark", 4, EMIT | HOT, g)
    K.cyl(m, "bind", (0, -8.4, 0.2), 1.1, 1.2, TRIM, "silk", sides=6)
    return m


# ================================================================ collars, medallions, orbs
@model
def collar_chromatic():
    """The Chromatic Collar: a black scaled collar lying in a ring with five round stones set evenly in it and a plate of overlapping scales over the clasp."""
    m = K.new("collar_chromatic")
    p = Part("band")
    L.band(p, -1.4, 1.4, 6.0, 6.0, 0.9, 20, 0.0, 0.3, 2.0)
    m.add(p, MAIN, "scale")
    K.ring(m, "rimtop", (0, 1.4, 0), 6.5, 0.28, TRIM, "gilt", "xz", 20, 4)
    K.ring(m, "rimbot", (0, -1.4, 0), 6.3, 0.28, TRIM, "gilt", "xz", 20, 4)
    for i in range(5):
        a = i * 2 * math.pi / 5 - math.pi / 2
        c = (6.6 * math.cos(a), 0.0, 6.6 * math.sin(a))
        sl = (GLOW, ACCENT, TRIM, NONE, GLOW)[i]
        tl = ("gem", "gem", "gem", "orange", "crystal")[i]
        K.ball(m, "stone%d" % i, c, 1.2, sl, tl, 3, 6, 0.8, EMIT | HOT if i != 2 else 0, uv="fit")
        K.ring(m, "set%d" % i, c, 1.4, 0.22, TRIM, "gilt", "xz", 8, 4)
    return m


@model
def collar_penitent():
    """The Collar of the Penitent Dragon: a slim silver collar ring whose front is a small dragon's head with a pearl in its jaws, the tail wrapped round the back."""
    m = K.new("collar_penitent")
    p = Part("band")
    L.band(p, -0.9, 0.9, 5.6, 5.6, 0.7, 20, 0.0, 0.0, 2.0)
    m.add(p, MAIN, "metal")
    K.rod(m, "tail", [(5.8 * math.cos(a), 0.0, 5.8 * math.sin(a)) for a in [math.pi / 2 + i * 0.35 for i in range(9)]], 0.5, MAIN, "scale", 5,
          scales=[(1.0, 1.0)] * 5 + [(0.8, 0.8), (0.6, 0.6), (0.4, 0.4), (0.25, 0.25)])
    K.ball(m, "head", (0, 0.3, -6.4), 1.7, MAIN, "scale", 4, 8, 0.8)
    K.block(m, "snout", (-1.0, -0.4, -9.2), (1.0, 0.6, -7.2), MAIN, "scale", 0.3)
    K.block(m, "jaw", (-0.8, -1.3, -8.8), (0.8, -0.5, -6.8), DARK, "scale", 0.2)
    K.ball(m, "pearl", (0, -0.7, -8.0), 0.6, NONE, "pearl", 3, 6, 1.0, 0, uv="fit")
    for s in (-1, 1):
        K.spike(m, "horn%d" % s, (s * 1.0, 1.2, -6.0), (s * 1.8, 3.6, -4.4), 0.45, TRIM, "gilt", 4)
        K.ball(m, "eye%d" % s, (s * 0.9, 0.7, -7.9), 0.28, GLOW, "gem", 3, 6, 1.0, EMIT, uv="fit")
    return m


@model
def collar_fenrir():
    """Fenrir's Collar: a heavy grey collar ring studded with spikes all round, a thick chain link hanging from the front clasp, and a wolf's head badge at the throat."""
    m = K.new("collar_fenrir")
    p = Part("band")
    L.band(p, -1.4, 1.4, 6.0, 6.0, 1.1, 20, 0.0, 0.0, 2.0)
    m.add(p, MAIN, "leather")
    for i in range(10):
        a = i * 2 * math.pi / 10
        K.spike(m, "spike%d" % i, (6.6 * math.cos(a), 0.0, 6.6 * math.sin(a)), (8.4 * math.cos(a), 0.0, 8.4 * math.sin(a)), 0.55, TRIM, "metal", 4, 0.4, 0.9)
    K.ball(m, "wolf", (0, 0, -7.0), 1.8, TRIM, "metal", 4, 8, 0.9)
    K.block(m, "muzzle", (-0.8, -1.4, -9.3), (0.8, -0.2, -7.2), TRIM, "metal", 0.2)
    for s in (-1, 1):
        K.spike(m, "ear%d" % s, (s * 1.0, 1.4, -6.8), (s * 1.6, 3.4, -6.2), 0.5, TRIM, "metal", 4)
        K.ball(m, "eye%d" % s, (s * 0.8, 0.5, -8.4), 0.28, GLOW, "gem", 3, 6, 1.0, EMIT, uv="fit")
    for i in range(3):
        K.ring(m, "link%d" % i, (0, -2.4 - i * 1.7, -7.0), 1.0, 0.3, DARK, "chain", "xy" if i % 2 == 0 else "yz", 8, 4)
    return m


@model
def medallion_sunheart():
    """The Sunheart Medallion: a golden sun disc with twelve rays of two lengths round a dome, a red heart-shaped stone in the middle and a bail with a short chain."""
    m = K.new("medallion_sunheart")
    K.disc(m, "disc", (0, 0, 0), 4.2, 1.2, MAIN, "gilt", "z", 16)
    p = Part("dome")
    lathe(p, "z", (0, 0, -0.5), [(0.0, 3.4), (0.7, 3.0), (1.4, 1.8), (1.8, 0.0)], sides=14)
    m.add(p, TRIM, "gilt")
    for i in range(12):
        a = i * math.pi / 6
        ln = 3.6 if i % 2 == 0 else 2.4
        K.spike(m, "ray%d" % i, (4.0 * math.cos(a), 4.0 * math.sin(a), 0), ((4.0 + ln) * math.cos(a), (4.0 + ln) * math.sin(a), 0), 0.9, TRIM, "gilt", 4, 0.3, 0.9)
    K.octa(m, "heartl", (-0.55, 0.2, -2.0), 0.9, 1.1, 0.5, GLOW, EMIT | HOT)
    K.octa(m, "heartr", (0.55, 0.2, -2.0), 0.9, 1.1, 0.5, GLOW, EMIT | HOT)
    K.octa(m, "hearttip", (0, -0.7, -2.0), 0.9, 1.0, 0.5, GLOW, EMIT | HOT)
    K.ring(m, "bail", (0, 8.6, 0), 0.9, 0.25, TRIM, "gilt", "yz", 8, 4)
    K.rod(m, "chain", K.bezier((0, 9.4, 0), (-3.2, 12.4, 0), (3.2, 13.4, 0), (0, 15.4, 0), 7), 0.2, TRIM, "chain", 4)
    return m


@model
def medallion_witcher():
    """The Witcher's Medallion: a round silver medallion with a snarling wolf's head cast in it, bared fangs and two glowing slit eyes, which trembles when magic is near; chain above."""
    m = K.new("medallion_witcher")
    K.ring(m, "rim", (0, 0, 0), 4.4, 0.6, MAIN, "metal", "xy", 18, 4)
    K.disc(m, "plate", (0, 0, 0.3), 4.2, 0.8, DARK, "plate", "z", 16, 0, uv="fit")
    K.ball(m, "skull", (0, 0.4, -0.9), 2.4, MAIN, "metal", 4, 8, 0.9)
    K.block(m, "muzzle", (-1.2, -2.6, -2.8), (1.2, -0.2, -0.2), MAIN, "metal", 0.4)
    K.block(m, "nose", (-0.5, -0.6, -3.1), (0.5, 0.1, -2.5), DARK, "metal", 0.2)
    for s in (-1, 1):
        K.spike(m, "ear%d" % s, (s * 1.6, 2.4, -0.8), (s * 2.6, 4.4, -0.8), 0.7, MAIN, "metal", 4)
        K.spike(m, "fang%d" % s, (s * 0.8, -2.6, -2.5), (s * 0.8, -4.0, -2.5), 0.28, NONE, "bone", 4, 0.5, 0.9)
        K.jewel(m, "eye%d" % s, (s * 1.0, 0.8, -2.7), 0.45, GLOW, 1.4, 4, "x", EMIT | HOT)
    K.ring(m, "bail", (0, 5.3, 0), 0.8, 0.22, MAIN, "metal", "yz", 8, 4)
    K.rod(m, "chain", K.bezier((0, 6.1, 0), (-3.0, 9.0, 0), (3.0, 10.0, 0), (0, 12.0, 0), 7), 0.2, MAIN, "chain", 4)
    return m


@model
def eye_of_aboleth():
    """The Eye of the Aboleth: a drop-shaped pendant that is one slimy eye with three-part pupil, set in a frame of wet green tentacles that curl round it, a loop and a cord above."""
    m = K.new("eye_of_aboleth")
    K.ball(m, "eye", (0, 0, 0), 3.8, NONE, "pearl", 5, 8, 1.2, 0, uv="fit")
    K.disc(m, "iris", (0, 0.2, -3.5), 2.2, 0.5, GLOW, "soft", "z", 12, EMIT | HOT, 0.1, uv="fit")
    K.block(m, "pupil", (-0.3, -1.2, -3.9), (0.3, 1.5, -3.4), DARK, "black", 0.1, uv="fit")
    for i in range(7):
        a = math.pi + i * math.pi / 3.2 + 0.2
        pts = [(4.2 * math.cos(a), 5.0 * math.sin(a) * 0.0 + 4.9 * math.sin(a) - 0.4 * i, 0.0)]
        c, s = math.cos(a), math.sin(a)
        K.rod(m, "tent%d" % i, K.bezier((c * 3.6, s * 4.4, 0.4), (c * 6.4, s * 7.0, -0.6), (c * 5.8 + s * 2.2, s * 6.0 - c * 2.2, 0.6)), 0.55, MAIN, "vein", 5,
              scales=[(1.0, 1.0), (0.9, 0.9), (0.8, 0.8), (0.65, 0.65), (0.5, 0.5), (0.35, 0.35), (0.25, 0.25), (0.2, 0.2)])
    K.ring(m, "bail", (0, 5.6, 0), 0.8, 0.25, TRIM, "gilt", "yz", 8, 4)
    K.rod(m, "cord", K.bezier((0, 6.4, 0), (-2.4, 9.0, 0), (2.4, 10.0, 0), (0, 12.0, 0), 7), 0.22, DARK, "leather", 4)
    return m


@model
def orb_plagues():
    """The Orb of Plagues: a sickly orb on a bent iron claw stand, clouded green glass with small boils and a ring of bubbles round it, a cloud of fumes curling off the top."""
    m = K.new("orb_plagues")
    K.ball(m, "orb", (0, 2.0, 0), 4.6, GLOW, "soft", 6, 10, 1.0, EMIT | HOT, uv="fit")
    K.ball(m, "glass", (0, 2.0, 0), 4.9, MAIN, "crystal", 5, 8, 1.0, 0, uv="fit")
    for i, (a, e) in enumerate(((0.3, 0.5), (1.7, -0.2), (2.9, 0.4), (4.1, -0.6), (5.2, 0.2), (0.9, -0.7))):
        K.ball(m, "boil%d" % i, (4.7 * math.cos(a) * math.cos(e), 2.0 + 4.7 * math.sin(e), 4.7 * math.sin(a) * math.cos(e)), 0.8, ACCENT, "pearl", 3, 6, 1.0, 0, uv="fit")
    for i in range(3):
        a = i * 2 * math.pi / 3 + 0.4
        K.rod(m, "claw%d" % i, K.bezier((2.0 * math.cos(a), -5.4, 2.0 * math.sin(a)), (5.0 * math.cos(a), -3.0, 5.0 * math.sin(a)), (3.6 * math.cos(a), 0.4, 3.6 * math.sin(a))), 0.5, DARK, "metal", 4)
    K.cyl(m, "stem", (0, -6.2, 0), 0.8, 2.4, DARK, "metal", sides=6)
    K.cyl(m, "foot", (0, -7.6, 0), 3.0, 0.8, DARK, "metal", sides=8)
    g = m.group("fumes", "bob", (0, 8.0, 0), 0.4, 0.1, 0.0)
    for i, x in enumerate((-1.0, 1.2)):
        K.rod(m, "fume%d" % i, K.bezier((x, 7.4, 0), (x + 1.6, 9.2, 0.4), (x - 1.4, 11.0, -0.4), (x + 0.8, 12.8, 0)), 0.55, ACCENT, "soft", 5, EMIT, g,
              scales=[(0.5, 0.5), (1.0, 1.0), (1.2, 1.2), (1.1, 1.1), (0.9, 0.9), (0.7, 0.7), (0.5, 0.5), (0.3, 0.3)])
    return m


@model
def orb_dryad():
    """The Orb of the Dryad: a glowing green seed orb held in a cage of twigs, a sprout with two leaves growing out of the top and tiny blossoms on the cage."""
    m = K.new("orb_dryad")
    K.ball(m, "orb", (0, 0, 0), 3.4, GLOW, "soft", 5, 8, 1.0, EMIT | HOT, uv="fit")
    for i in range(6):
        a = i * math.pi / 3
        c, s = math.cos(a), math.sin(a)
        K.rod(m, "twig%d" % i, K.bezier((0.5 * c, -5.2, 0.5 * s), (6.0 * c, -0.5, 6.0 * s), (0.5 * c, 5.0, 0.5 * s), (0.2 * c, 5.6, 0.2 * s), 8), 0.42, MAIN, "wood", 5)
    K.ring(m, "knot", (0, -5.1, 0), 0.8, 0.5, MAIN, "wood", "xz", 8, 4)
    K.rod(m, "stem", K.bezier((0, 5.4, 0), (0.6, 7.6, 0), (-0.6, 9.4, 0), 6), 0.35, TRIM, "vein", 5)
    for s in (-1, 1):
        K.rod(m, "leaf%d" % s, K.bezier((0, 8.2, 0), (s * 2.4, 9.6, 0), (s * 3.6, 8.4, 0), 6), 0.7, TRIM, "vein", 4,
              scales=[(0.3, 0.3), (1.0, 0.4), (1.2, 0.4), (1.0, 0.4), (0.6, 0.3), (0.2, 0.2), (0.1, 0.1)])
    for i, (a, y) in enumerate(((0.5, 1.5), (2.4, -2.0), (4.2, 2.8), (5.5, -0.6))):
        K.ball(m, "bloom%d" % i, (4.6 * math.cos(a), y, 4.6 * math.sin(a)), 0.55, NONE, "pearl", 3, 6, 1.0, 0, uv="fit")
    return m


@model
def holy_handgrenade():
    """The Holy Handgrenade: a golden orb with a band of gilt round its middle and a second one crossing it, a small jewelled cross on top, and a pin ring on a short chain."""
    m = K.new("holy_handgrenade")
    K.ball(m, "orb", (0, 0, 0), 4.8, MAIN, "gilt", 6, 12, 1.0)
    K.ring(m, "band1", (0, 0, 0), 4.9, 0.55, TRIM, "gilt", "xz", 18, 4)
    K.ring(m, "band2", (0, 0, 0), 4.9, 0.55, TRIM, "gilt", "xy", 18, 4)
    K.ring(m, "band3", (0, 0, 0), 4.9, 0.45, TRIM, "gilt", "yz", 18, 4)
    K.cyl(m, "collar", (0, 5.0, 0), 1.5, 1.0, TRIM, "gilt", sides=8)
    K.block(m, "cross_v", (-0.6, 5.4, -0.6), (0.6, 10.4, 0.6), TRIM, "gilt", 0.15)
    K.block(m, "cross_h", (-2.2, 8.0, -0.6), (2.2, 9.2, 0.6), TRIM, "gilt", 0.15)
    K.octa(m, "cross_gem", (0, 8.6, -0.8), 0.7, 0.7, 0.4, GLOW, EMIT | HOT)
    K.ring(m, "pin", (3.2, 5.0, 0), 1.0, 0.25, ACCENT, "gilt", "xy", 8, 4)
    K.octa(m, "jewel", (0, 0, -4.9), 1.0, 1.0, 0.6, GLOW, EMIT | HOT)
    return m


# ================================================================ who holds what
ASSIGN.update({
    "the_space_stone_or_the_tesseract": ["stone_space"],
    "the_mind_stone": ["stone_mind"],
    "the_power_stone": ["stone_power"],
    "the_time_stone_or_the_eye_of_agamotto": ["stone_time"],
    "the_reality_stone": ["stone_reality"],
    "the_soul_stone": ["stone_soul"],
    "the_infinity_gauntlet": ["infinity_gauntlet"],
    "hand_of_vecna": ["hand_of_vecna"],
    "eye_of_vecna": ["eye_of_vecna_hand"],
    "deck_of_many_things": ["deck_of_many_things"],
    "warrens_key": ["warrens_key"],
    "soup_spoon_of_improbability": ["soup_spoon"],
    "instant_cooking_pot": ["cooking_pot"],
    "spirit_lantern": ["spirit_lantern"],
    "bewitching_lyre": ["lyre_bewitching"],
    "silver_tongued_lyre": ["lyre_silver_tongued"],
    "horn_of_leroy": ["horn_of_leroy"],
    "pocket_time": ["pocket_watch"],
    "monster_compass": ["monster_compass"],
    "lucky_egg": ["lucky_egg"],
    "rose_petal": ["rose_petal"],
    "chromatic_collar": ["collar_chromatic"],
    "collar_of_the_penitent_dragon": ["collar_penitent"],
    "fenrirs_collar": ["collar_fenrir"],
    "sunheart_medallion": ["medallion_sunheart"],
    "witchers_medallion": ["medallion_witcher"],
    "eye_of_the_aboleth": ["eye_of_aboleth"],
    "orb_of_plagues": ["orb_plagues"],
    "orb_of_the_dryad": ["orb_dryad"],
    "holy_handgrenade": ["holy_handgrenade"],
})
