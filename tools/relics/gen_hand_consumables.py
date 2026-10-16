"""gen_hand_consumables: the hand models of the 21 consumable relics (potions, vials, scrolls, jars, food, drink): every one is a small 3D object in the hand, with its own
silhouette (a honey jar, a round box of chocolates, a tankard, a corked vial, a sealed scroll, a genie bottle ...), tinted by the relic's palette and glowing where the thing glows.
(The Devil Contract is an older item with its own model; it is not in this list.)

Same frame and kit as the other hand generators (gen_hand_kit.py): Y up, the front is -Z. Run with tools/relics/gen_hand_all.py; the ASSIGN table at the end says which relic
is held as which model. The families are shared where the objects are: the bargain-bin scrolls are one scroll with six emblems.
"""
import math

import gen_hand_kit as K
from gen_hand_kit import (Part, loft, lathe, sweep, circle, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT, L, ml)

MODELS, model = K.registry()
ASSIGN = {}


def _glass(m, name, c, profile, sides=8, slot=GLOW, tile="crystal", flags=EMIT | HOT, squash=1.0):
    p = Part(name)
    lathe(p, "y", c, profile, sides=sides, squash=squash)
    return m.add(p, slot, tile, flags, 0, "fit")


def _cork(m, y, r=1.1, h=1.6, slot=MAIN, tile="wood"):
    K.cyl(m, "cork", (0, y, 0), r, h, slot, tile, sides=7)


def _bubbles(m, c, n=3, r=0.55, slot=ACCENT, spread=1.6, rise=3.0):
    g = m.group("bubbles", "bob", (c[0], c[1], c[2]), 0.5, 0.16, 0.0)
    for i in range(n):
        a = i * 2.4
        K.ball(m, "bubble%d" % i, (c[0] + spread * math.cos(a) * (0.4 + 0.2 * i), c[1] + rise * i / max(1, n - 1), c[2] + spread * math.sin(a) * 0.5), r * (1 - 0.15 * i), slot, "pearl",
               3, 6, 1.0, EMIT, g, "fit")


# ================================================================ jars, boxes, mugs
@model
def jar_honey():
    """Fey Honey: a squat glass jar of glowing amber honey, tied with a cloth lid and twine, a drip running down the side, and two tiny sparks of fey light flitting over it."""
    m = K.new("jar_honey")
    _glass(m, "honey", (0, -5.0, 0), [(0.0, 3.4), (0.5, 4.2), (3.0, 4.6), (6.0, 4.2), (7.0, 3.2), (7.4, 2.8)], 10)
    K.cyl(m, "neck", (0, 3.0, 0), 3.1, 1.2, TRIM, "gilt", sides=10)
    p = Part("cloth")
    lathe(p, "y", (0, 3.4, 0), [(0.0, 3.0), (0.6, 4.0), (1.6, 3.6), (2.2, 0.0)], sides=10)
    m.add(p, MAIN, "cloth")
    K.ring(m, "twine", (0, 3.4, 0), 3.2, 0.25, DARK, "leather", "xz", 10, 4)
    K.rod(m, "drip", [(2.6, 2.8, -2.4), (3.2, 1.0, -3.6), (3.4, -1.0, -3.9), (3.4, -2.0, -3.9)], 0.45, GLOW, "honey", 5, EMIT | HOT, scales=[(1.0, 1.0)] * 3 + [(1.4, 1.4)])
    g = m.group("sparks", "orbit", (0, 6.0, 0), 4.0, 0.0, 0.0)
    for i, (x, y) in enumerate(((3.0, 6.4), (-2.6, 8.2))):
        K.octa(m, "spark%d" % i, (x, y, 0), 0.4, 0.4, 0.4, NONE, EMIT, g, "spark")
    return m


@model
def box_chocolates():
    """Blood Chocolates: a round box with the lid leaning open at the back, six dark chocolates in it each with a drop of red at the centre, and a ribbon tied round the box."""
    m = K.new("box_chocolates")
    p = Part("box")
    lathe(p, "y", (0, -4.0, 0), [(0.0, 5.0), (0.4, 5.4), (3.0, 5.4), (3.6, 5.0)], sides=12)
    m.add(p, MAIN, "silk")
    K.disc(m, "lining", (0, -0.4, 0), 4.6, 0.3, TRIM, "silk", "y", 12, 0, 0.0)
    for i in range(6):
        a = i * math.pi / 3
        x, z = (0.0, 0.0) if i == 0 else (2.7 * math.cos(a), 2.7 * math.sin(a))
        K.ball(m, "choc%d" % i, (x, 0.6, z), 1.2, DARK, "leather", 3, 6, 0.8)
        K.ball(m, "drop%d" % i, (x, 1.3, z), 0.45, GLOW, "gem", 3, 6, 0.6, EMIT, uv="fit")
    p = Part("lid")
    lathe(p, "y", (0, 0.0, 0), [(0.0, 5.0), (0.5, 5.6), (2.0, 5.6), (2.6, 5.0)], sides=12)
    K.turn(p, "x", -62, (0, 0, 5.4))
    m.add(p, MAIN, "silk")
    K.cyl(m, "ribbon", (0, -2.0, 0), 5.5, 0.9, ACCENT, "silk", sides=12)
    K.ball(m, "bow", (0, -2.0, -5.8), 0.9, ACCENT, "silk", 3, 6, 0.8)
    return m


@model
def tankard_ale():
    """Ale-lemental: a pewter tankard brimming with blue-green liquid that foams up into a small rippling shape with two bright eyes, a handle and bands of gilt."""
    m = K.new("tankard_ale")
    p = Part("mug")
    lathe(p, "y", (0, -5.0, 0), [(0.0, 3.4), (0.4, 3.8), (5.0, 4.3), (9.0, 4.6), (9.6, 4.6)], sides=12)
    m.add(p, MAIN, "metal")
    K.cyl(m, "band1", (0, -2.4, 0), 4.0, 0.7, TRIM, "gilt", sides=12)
    K.cyl(m, "band2", (0, 2.6, 0), 4.5, 0.7, TRIM, "gilt", sides=12)
    K.ring(m, "handle", (5.2, 0.0, 0), 2.6, 0.55, MAIN, "metal", "xy", 12, 4, 0, 0, 0.8, 1.4)
    K.disc(m, "liquid", (0, 4.7, 0), 4.2, 0.4, GLOW, "soft", "y", 12, EMIT | HOT, 0.1, uv="fit")
    g = m.group("elemental", "bob", (0, 5.0, 0), 0.5, 0.12, 0.0)
    p = Part("body")
    lathe(p, "y", (0, 4.8, 0), [(0.0, 3.0), (0.8, 3.4), (2.4, 2.8), (4.2, 1.6), (5.4, 0.0)], sides=9)
    m.add(p, GLOW, "soft", EMIT | HOT, g, "fit")
    for s in (-1, 1):
        K.ball(m, "eye%d" % s, (s * 1.0, 8.2, -1.9), 0.5, NONE, "white", 3, 6, 1.0, EMIT, g, "fit")
    for i, (x, z) in enumerate(((-2.4, 1.0), (2.2, 1.6), (0.4, -2.6))):
        K.ball(m, "foam%d" % i, (x, 5.0, z), 1.0, NONE, "snow", 3, 6, 0.8)
    return m


@model
def mug_boss():
    """The Mug of the Boss: a fat white ceramic mug with a wide handle, a gold band printed round it with a crown, a little steam above and a pen standing in it like a sceptre."""
    m = K.new("mug_boss")
    p = Part("mug")
    lathe(p, "y", (0, -4.5, 0), [(0.0, 3.6), (0.3, 4.0), (4.0, 4.4), (8.0, 4.4), (8.6, 4.2)], sides=14)
    m.add(p, NONE, "white", 0, 0, "fit")
    K.disc(m, "coffee", (0, 3.6, 0), 4.0, 0.3, DARK, "leather", "y", 12, 0, 0.0)
    K.ring(m, "handle", (5.0, 0.0, 0), 2.6, 0.6, NONE, "white", "xy", 12, 4, 0, 0, 0.8, 1.3)
    K.cyl(m, "band", (0, 0.4, 0), 4.5, 3.0, MAIN, "gilt", sides=14)
    K.block(m, "crown_base", (-1.6, -0.2, -4.7), (1.6, 0.4, -4.5), TRIM, "gilt", 0.0)
    for i, x in enumerate((-1.4, 0.0, 1.4)):
        K.spike(m, "crown%d" % i, (x, 0.4, -4.6), (x, 1.7 if i == 1 else 1.3, -4.6), 0.35, TRIM, "gilt", 4, 0.5, 0.9)
    K.rod(m, "pen", [(1.6, 3.6, 0), (2.4, 9.6, 0), (2.6, 11.4, 0)], 0.4, GLOW, "gem", 5, EMIT)
    g = m.group("steam", "bob", (-1.0, 4.0, 0), 0.4, 0.1, 0.0)
    K.rod(m, "steam", K.bezier((-1.2, 4.0, 0), (-0.2, 5.8, 0), (-2.0, 7.6, 0), (-0.8, 9.4, 0), 7), 0.4, NONE, "soft", 5, EMIT, g,
          scales=[(0.5, 0.5), (1.0, 1.0), (1.2, 1.2), (1.1, 1.1), (0.9, 0.9), (0.6, 0.6), (0.3, 0.3)])
    return m


# ================================================================ vials and flasks
@model
def vial_holy():
    """Holy Water: a small clear vial with a silver cap and a cross engraved in gilt on the front, the water inside glowing white and a halo of light round the stopper."""
    m = K.new("vial_holy")
    _glass(m, "water", (0, -6.0, 0), [(0.0, 1.8), (0.5, 2.6), (5.0, 2.8), (9.0, 2.2), (10.0, 1.2), (10.6, 1.0)], 8)
    K.cyl(m, "cap", (0, 5.2, 0), 1.5, 1.8, MAIN, "metal", sides=8)
    K.ball(m, "capball", (0, 6.6, 0), 1.0, MAIN, "metal", 3, 6)
    K.block(m, "cross_v", (-0.35, -3.6, -3.0), (0.35, 1.6, -2.8), TRIM, "gilt", 0.0)
    K.block(m, "cross_h", (-1.5, -0.6, -3.0), (1.5, 0.1, -2.8), TRIM, "gilt", 0.0)
    K.ring(m, "halo", (0, 5.8, 0), 2.6, 0.18, NONE, "spark", "xz", 12, 4, EMIT)
    return m


@model
def vial_black():
    """Executioner's Bane: a slim vial cut from black crystal with a gold cap and a gold chain looped from the cap, a thin red thread of light inside the dark."""
    m = K.new("vial_black")
    p = Part("crystal")
    lathe(p, "y", (0, -6.0, 0), [(0.0, 0.0), (0.4, 1.8), (2.0, 2.4), (8.0, 2.4), (9.6, 1.8), (10.0, 1.4)], sides=6)
    m.add(p, DARK, "crystal", 0, 0, "fit")
    K.rod(m, "thread", [(0, -4.6, -1.2), (0.4, -1.0, -1.4), (-0.3, 2.6, -1.4), (0.2, 5.0, -1.2)], 0.2, GLOW, "spark", 3, EMIT | HOT)
    K.cyl(m, "cap", (0, 4.6, 0), 1.6, 1.6, TRIM, "gilt", sides=6)
    K.ring(m, "bail", (0, 6.3, 0), 0.9, 0.25, TRIM, "gilt", "yz", 8, 4)
    K.rod(m, "chain", K.bezier((0, 7.1, 0), (-3.4, 9.6, 0), (3.4, 11.0, 0), (0, 13.2, 0), 8), 0.2, TRIM, "chain", 4)
    return m


@model
def flask_poison():
    """The Peculiar Puking Poison: a round-bellied flask of sickly green liquid with a thin neck and a cork, bubbles popping out of it, and a small skull on the front."""
    m = K.new("flask_poison")
    _glass(m, "poison", (0, -6.0, 0), [(0.0, 0.0), (0.6, 2.2), (2.4, 4.4), (4.4, 4.8), (6.4, 3.6), (7.6, 1.6), (8.2, 1.0), (10.4, 1.1), (11.0, 1.4)], 10)
    _cork(m, 5.0)
    K.ball(m, "skull", (0, 0.2, -4.3), 1.3, NONE, "bone", 4, 8, 0.8)
    for s in (-1, 1):
        K.ball(m, "socket%d" % s, (s * 0.5, 0.4, -5.2), 0.32, DARK, "black", 3, 6)
    _bubbles(m, (0.0, 6.0, 0.0), 4, 0.6, ACCENT, 1.2, 3.4)
    return m


@model
def orb_portal():
    """The Pocket Portal: a glass orb of swirling purple with a ring of light turning round it like a doorway, set in a small three-footed gilt stand."""
    m = K.new("orb_portal")
    K.ball(m, "orb", (0, 1.0, 0), 4.4, GLOW, "soft", 6, 10, 1.0, EMIT | HOT, uv="fit")
    g = m.group("swirl", "spin", (0, 1.0, 0), 2.4, 0.0, 0.0)
    K.rod(m, "swirl", K.helix((0, 1.0, 0), 4.7, -2.0, 4.0, 2.0, 26), 0.45, NONE, "spark", 4, EMIT, g, scales=[(0.4, 0.4)] + [(1.0, 1.0)] * 24 + [(0.4, 0.4)])
    g2 = m.group("door", "spin", (0, 1.0, 0), -1.2, 0.0, 0.0)
    K.ring(m, "door", (0, 1.0, 0), 6.0, 0.3, ACCENT, "gilt", "xy", 18, 4, EMIT, g2)
    K.ring(m, "stand", (0, -4.2, 0), 2.6, 0.5, TRIM, "gilt", "xz", 10, 4)
    for i in range(3):
        a = i * 2 * math.pi / 3
        K.spike(m, "foot%d" % i, (2.4 * math.cos(a), -4.0, 2.4 * math.sin(a)), (3.2 * math.cos(a), -6.6, 3.2 * math.sin(a)), 0.5, TRIM, "gilt", 4, 0.4, 0.9)
    return m


@model
def flask_vampire():
    """The Potion of Vampirism: a tall tapered flask of dark red liquid with a bat-winged stopper, two small white fangs drawn on the front, and a single drop sliding down the glass."""
    m = K.new("flask_vampire")
    _glass(m, "blood", (0, -7.0, 0), [(0.0, 0.0), (0.5, 1.8), (2.0, 3.4), (5.0, 3.8), (8.0, 2.6), (10.0, 1.2), (11.0, 1.0), (13.0, 1.1), (13.6, 1.5)], 8)
    K.cyl(m, "stopper", (0, 6.8, 0), 1.2, 1.6, DARK, "black", sides=6)
    for s in (-1, 1):
        K.rod(m, "wing%d" % s, K.bezier((s * 0.8, 7.4, 0), (s * 3.0, 9.4, 0), (s * 4.2, 7.2, 0)), 0.7, DARK, "black", 4,
              scales=[(0.4, 0.2), (1.0, 0.3), (1.2, 0.3), (1.0, 0.3), (0.6, 0.2), (0.3, 0.2), (0.1, 0.1)])
        K.spike(m, "fang%d" % s, (s * 0.8, -0.4, -3.9), (s * 0.8, -2.6, -3.9), 0.4, NONE, "bone", 4, 0.4, 0.9)
    K.ball(m, "drop", (1.2, 0.8, -3.8), 0.4, GLOW, "gem", 3, 6, 1.5, EMIT, uv="fit")
    return m


@model
def flask_sleep():
    """Romlo's Potion of Sleep: a round blue flask wearing a floppy nightcap for a stopper with a pom-pom, a crescent moon and a Z drawn on the front."""
    m = K.new("flask_sleep")
    _glass(m, "dream", (0, -6.0, 0), [(0.0, 0.0), (0.6, 2.2), (2.4, 4.4), (4.4, 4.8), (6.4, 3.6), (7.6, 1.8), (8.4, 1.2), (9.6, 1.3)], 10)
    p = Part("cap")
    lathe(p, "y", (0, 3.6, 0), [(0.0, 1.8), (0.6, 1.9), (2.0, 1.4), (3.6, 0.7), (4.4, 0.4)], sides=8)
    m.add(p, MAIN, "cloth")
    K.rod(m, "tail", K.bezier((0, 7.6, 0), (2.4, 9.0, 0), (3.6, 7.6, 0), 5), 0.7, MAIN, "cloth", 6, scales=[(1.0, 1.0), (0.9, 0.9), (0.8, 0.8), (0.7, 0.7), (0.6, 0.6), (0.5, 0.5)])
    K.ball(m, "pom", (3.6, 7.2, 0), 0.9, NONE, "white", 3, 6)
    arc = [(-1.2 + 1.8 * math.cos(a), 1.6 + 1.8 * math.sin(a), -4.4) for a in [math.pi * (0.5 + i / 8 * 1.2) for i in range(9)]]
    K.rod(m, "moon", arc, 0.35, NONE, "spark", 4, EMIT | HOT)
    K.rod(m, "z", [(1.0, 1.6, -4.5), (2.6, 1.6, -4.5), (1.0, 0.0, -4.5), (2.6, 0.0, -4.5)], 0.2, NONE, "spark", 3, EMIT)
    return m


@model
def jar_spider():
    """Spider Brew: a squat dark jar with a cloth lid, a web of thin lines spun across the front and a big hairy spider sitting on the lid with all eight legs."""
    m = K.new("jar_spider")
    _glass(m, "brew", (0, -5.0, 0), [(0.0, 3.6), (0.5, 4.6), (3.0, 4.8), (6.0, 4.4), (7.0, 3.4), (7.4, 3.0)], 10, GLOW, "crystal", EMIT)
    K.cyl(m, "lid", (0, 2.8, 0), 3.4, 1.2, MAIN, "cloth", sides=10)
    for i in range(5):
        a = math.pi * (0.1 + i * 0.2)
        K.rod(m, "web%d" % i, [(0, -0.4, -4.9), (4.6 * math.cos(a), -0.4 + 4.6 * math.sin(a) - 2.0, -4.7)], 0.1, NONE, "white", 3)
    for k, r in enumerate((1.4, 2.6, 3.8)):
        K.rod(m, "webring%d" % k, [(r * math.cos(a), -0.4 + r * math.sin(a), -4.85) for a in [math.pi * (0.05 + i * 0.9 / 8) for i in range(9)]], 0.08, NONE, "white", 3)
    K.ball(m, "spider", (0, 4.4, 0), 1.3, DARK, "black", 4, 8, 0.9)
    K.ball(m, "spiderhead", (0, 4.6, -1.5), 0.8, DARK, "black", 3, 6)
    for s in (-1, 1):
        for k in range(4):
            z = -0.9 + k * 0.6
            K.rod(m, "leg%d%d" % (s, k), [(s * 1.0, 4.4, z), (s * 2.8, 5.8, z * 1.6), (s * 3.8, 3.4, z * 2.0)], 0.14, DARK, "black", 3)
        K.jewel(m, "eye%d" % s, (s * 0.35, 4.8, -2.3), 0.18, GLOW, 1.2, 4, "z", EMIT)
    return m


@model
def bottle_genie():
    """Genie Gin: an ornate bulb-bellied bottle in gilt filigree, its stopper a little turban dome, and a thin curl of blue smoke rising out of it in the shape of a genie's tail."""
    m = K.new("bottle_genie")
    _glass(m, "gin", (0, -6.0, 0), [(0.0, 0.0), (0.6, 2.8), (2.4, 5.0), (4.6, 5.4), (7.0, 3.8), (8.4, 1.6), (9.4, 1.0), (11.4, 1.1), (12.0, 1.5)], 10, ACCENT, "crystal", EMIT)
    K.ring(m, "belly", (0, -1.8, 0), 5.4, 0.35, TRIM, "gilt", "xz", 12, 4)
    K.ring(m, "belly2", (0, 1.4, 0), 4.9, 0.3, TRIM, "gilt", "xz", 12, 4)
    for i in range(6):
        a = i * math.pi / 3
        K.ball(m, "stud%d" % i, (5.0 * math.cos(a), -0.2, 5.0 * math.sin(a)), 0.5, TRIM, "gilt", 3, 6)
    K.cyl(m, "collar", (0, 6.4, 0), 1.7, 0.9, TRIM, "gilt", sides=8)
    p = Part("turban")
    lathe(p, "y", (0, 6.8, 0), [(0.0, 0.9), (0.4, 1.6), (1.6, 2.0), (2.6, 1.3), (3.2, 0.0)], sides=9)
    m.add(p, MAIN, "silk")
    K.octa(m, "jewel", (0, 8.2, -1.8), 0.5, 0.5, 0.3, GLOW, EMIT | HOT)
    g = m.group("smoke", "sway", (0, 9.8, 0), 8.0, 0.1, 0.0)
    K.rod(m, "smoke", K.bezier((0, 9.8, 0), (2.6, 12.0, 0), (-2.6, 13.8, 0), (1.0, 16.4, 0), 8), 0.8, GLOW, "soft", 6, EMIT, g,
          scales=[(0.6, 0.6), (1.0, 1.0), (1.4, 1.4), (1.5, 1.5), (1.3, 1.3), (1.0, 1.0), (0.6, 0.6), (0.3, 0.3)])
    return m


@model
def sack_marbles():
    """Mad Marbles: a small cloth sack tied at the neck, tipped slightly, with eight glass marbles spilling out of its mouth and over the floor, each with a little bolt of lightning inside."""
    m = K.new("sack_marbles")
    p = Part("sack")
    lathe(p, "y", (0, -4.5, 0), [(0.0, 0.0), (0.4, 2.0), (2.0, 3.6), (4.4, 3.8), (6.4, 2.6), (7.6, 1.4), (8.4, 1.8)], sides=9)
    K.turn(p, "z", 38, (0, -4.5, 0))
    p.V = [v + (3.0, 2.5, 0) for v in p.V]
    m.add(p, MAIN, "cloth")
    spots = ((-1.6, -6.0, -1.2), (-3.6, -6.4, 1.0), (0.6, -6.5, 1.8), (-2.2, -6.5, -3.4), (1.0, -6.4, -2.6), (-5.4, -6.4, -1.4), (-3.8, -4.6, -0.2), (-1.4, -4.4, 0.4))
    for i, (x, y, z) in enumerate(spots):
        K.ball(m, "marble%d" % i, (x - 1.0, y, z), 1.0, GLOW if i % 2 == 0 else ACCENT, "crystal", 3, 7, 1.0, EMIT | (HOT if i % 3 == 0 else 0), uv="fit")
        K.rod(m, "bolt%d" % i, [(x - 1.3, y + 0.5, z - 0.9), (x - 0.8, y, z - 1.0), (x - 1.3, y - 0.3, z - 1.0), (x - 0.8, y - 0.7, z - 1.0)], 0.1, NONE, "white", 3, EMIT)
    K.ring(m, "tie", (4.0, 2.0, 0), 1.2, 0.3, TRIM, "leather", "xz", 8, 4)
    return m


# ================================================================ scrolls
def _scroll_body(m, slot=NONE, tile="paper", ribbon=ACCENT):
    K.cyl(m, "roll", (0, 0, 0), 2.4, 12.0, slot, tile, sides=10)
    for s in (-1, 1):
        K.cyl(m, "rod%d" % s, (0, s * 6.4, 0), 1.0, 1.4, DARK, "wood", sides=8)
        K.ball(m, "knob%d" % s, (0, s * 7.6, 0), 0.9, TRIM, "gilt", 3, 6)
    K.cyl(m, "band", (0, 0.0, 0), 2.7, 1.2, ribbon, "silk", sides=10)


@model
def scroll_scrawl():
    """The Scroll of Unreadable Spells: a half-unrolled scroll that hangs from its top roller, its page crowded with scribbled lines nobody can read, the ink faintly glowing."""
    m = K.new("scroll_scrawl")
    K.cyl(m, "top", (0, 7.0, 0), 1.0, 10.6, DARK, "wood", "x", 8)
    for s in (-1, 1):
        K.ball(m, "knob%d" % s, (s * 5.5, 7.0, 0), 1.0, TRIM, "gilt", 3, 6)

    def sheet(u, v):
        return ((u - 0.5) * 9.4, 6.6 - 13.6 * v, -0.12 * math.sin(math.pi * v * 3) + 0.5 * v)
    p = Part("page")
    L.sheet(p, sheet, 4, 6, 0.3, True)
    m.add(p, NONE, "paper")
    K.cyl(m, "bottom", (0, -7.2, 0), 0.9, 10.0, DARK, "wood", "x", 8)
    for i in range(7):
        y = 4.6 - i * 1.7
        w = 7.0 - (i % 3) * 1.6
        K.rod(m, "line%d" % i, [(-w / 2, y, -0.9), (-w / 6, y + 0.3, -0.9), (w / 6, y - 0.3, -0.9), (w / 2, y + 0.2, -0.9)], 0.16, GLOW, "rune", 3, EMIT)
    return m


@model
def scroll_sealed():
    """The Scroll of Absorption: a tight scroll with gilt rollers and a ribbon, sealed with a big wax seal that holds a glowing sigil, a ring of runes drifting round it."""
    m = K.new("scroll_sealed")
    _scroll_body(m, NONE, "paper", ACCENT)
    K.disc(m, "wax", (0, 0.0, -2.7), 1.9, 0.6, MAIN, "wood", "z", 10, 0, 0.1)
    K.octa(m, "sigil", (0, 0.0, -3.2), 1.0, 1.0, 0.3, GLOW, EMIT | HOT)
    g = m.group("runes", "spin", (0, 0.0, 0), 2.0, 0.0, 0.0)
    for i in range(6):
        a = i * math.pi / 3
        K.octa(m, "rune%d" % i, (4.4 * math.cos(a), 0.0, 4.4 * math.sin(a)), 0.35, 0.5, 0.2, GLOW, EMIT | HOT, g)
    return m


def _bargain(name, doc, emblem):
    def fn():
        m = K.new(name)
        _scroll_body(m, NONE, "paper", MAIN)
        K.block(m, "tag", (1.6, -2.4, -2.6), (4.4, -0.2, -2.4), TRIM, "paper", 0.0, uv="fit")
        K.rod(m, "string", [(1.8, -0.2, -2.5), (1.0, 0.8, -2.6), (0.0, 1.0, -2.6)], 0.1, DARK, "leather", 3)
        emblem(m)
        return m
    fn.__name__ = name
    fn.__doc__ = doc
    MODELS[name] = fn
    return fn


def _e_charm(m):
    K.octa(m, "hl", (-0.6, 1.8, -2.9), 0.7, 0.8, 0.3, GLOW, EMIT | HOT)
    K.octa(m, "hr", (0.6, 1.8, -2.9), 0.7, 0.8, 0.3, GLOW, EMIT | HOT)
    K.octa(m, "ht", (0, 1.0, -2.9), 0.8, 0.8, 0.3, GLOW, EMIT | HOT)


def _e_fire(m):
    p = Part("flame")
    lathe(p, "y", (0, 0.0, -2.9), [(0.0, 0.0), (0.4, 0.8), (1.2, 0.9), (2.2, 0.5), (3.0, 0.0)], sides=6, squash=0.3)
    m.add(p, GLOW, "flame", EMIT | HOT, 0, "fit")


def _e_rock(m):
    K.ball(m, "rock", (0, 1.0, -2.9), 1.1, GLOW, "scale", 3, 6, 0.7)
    K.ball(m, "rock2", (1.0, 2.2, -2.8), 0.6, GLOW, "scale", 3, 6, 0.8)


def _e_crab(m):
    for s in (-1, 1):
        K.rod(m, "claw%d" % s, K.bezier((s * 0.6, 0.6, -2.9), (s * 2.0, 1.4, -3.2), (s * 1.2, 2.6, -3.0)), 0.35, GLOW, "horn", 4, EMIT)
    K.ball(m, "shell", (0, 0.6, -2.9), 0.9, GLOW, "scale", 3, 6, 0.6, EMIT)


def _e_mace(m):
    K.rod(m, "haft", [(0, -0.8, -2.9), (0, 1.6, -2.9)], 0.22, GLOW, "wood", 3, EMIT)
    K.ball(m, "head", (0, 2.2, -2.9), 0.8, GLOW, "metal", 3, 6, 1.0, EMIT)


def _e_copy(m):
    K.block(m, "page1", (-1.4, 0.2, -2.9), (0.2, 2.4, -2.8), GLOW, "paper", 0.0, EMIT, uv="fit")
    K.block(m, "page2", (-0.2, -0.2, -3.0), (1.4, 2.0, -2.9), GLOW, "paper", 0.0, EMIT, uv="fit")


_bargain("scroll_bargain_charm", "Scroll of Charm Parson: a cheap rolled scroll with a price tag on a string and a small glowing heart on it.", _e_charm)
_bargain("scroll_bargain_fire", "Scroll of Firball: a cheap rolled scroll with a price tag and a small glowing flame on it.", _e_fire)
_bargain("scroll_bargain_rock", "Scroll of Speak with Rocks: a cheap rolled scroll with a price tag and a pebble stuck to it.", _e_rock)
_bargain("scroll_bargain_crab", "Scroll of Summon Greater Crab: a cheap rolled scroll with a price tag and a pair of small claws drawn on it.", _e_crab)
_bargain("scroll_bargain_mace", "Scroll of Mace Hand: a cheap rolled scroll with a price tag and a tiny glowing mace on it.", _e_mace)
_bargain("scroll_bargain_copy", "Scroll of Facsimile Copy: a cheap rolled scroll with a price tag and two overlapping glowing pages on it.", _e_copy)


# ================================================================ who holds what
ASSIGN.update({
    "fey_honey": ["jar_honey"],
    "blood_chocolates": ["box_chocolates"],
    "ale_lemental": ["tankard_ale"],
    "mug_of_the_boss": ["mug_boss"],
    "holy_water": ["vial_holy"],
    "scroll_of_unreadable_spells": ["scroll_scrawl"],
    "scroll_of_absorption": ["scroll_sealed"],
    "scroll_of_charm_parson_bargain_bin_scrolls_1": ["scroll_bargain_charm"],
    "scroll_of_firball_bargain_bin_scrolls_2": ["scroll_bargain_fire"],
    "scroll_of_speak_with_rocks_bargain_bin_scrolls_3": ["scroll_bargain_rock"],
    "scroll_of_summon_greater_crab_bargain_bin_scrolls_4": ["scroll_bargain_crab"],
    "scroll_of_mace_hand_bargain_bin_scrolls_5": ["scroll_bargain_mace"],
    "scroll_of_facsimile_copy_bargain_bin_scrolls_6": ["scroll_bargain_copy"],
    "executioners_bane": ["vial_black"],
    "perculiar_puking_poison": ["flask_poison"],
    "pocket_portal": ["orb_portal"],
    "potion_of_vampirism": ["flask_vampire"],
    "romlos_potion_of_sleep": ["flask_sleep"],
    "spider_brew": ["jar_spider"],
    "genie_gin": ["bottle_genie"],
    "mad_marbles": ["sack_marbles"],
})
