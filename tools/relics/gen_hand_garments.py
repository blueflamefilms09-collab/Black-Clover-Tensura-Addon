"""gen_hand_garments: the four garment bodies that make a chest relic an object in the hand.

The worn models of a chest relic are only the ornaments over a painted body (a collar, two pauldrons, a pair of coat tails, crossed straps): without a body under them they are
scattered pieces. Held as an object, the relic is drawn as one of these garments (a plate cuirass, a long robe, a coat, a leather vest, chosen by its look) with the relic's own
ornaments on top of it, all in the body frame of the worn models (origin at the neck line, the torso x -4..4, y -12..0, z -2..2; the garment sits a little inside that so the
ornaments, made to sit on the body, lie over it). gen_hand_all.py writes the catalog lines. Frame and kit: gen_hand_kit.py.
"""
import math

import gen_hand_kit as K
from gen_hand_kit import (Part, loft, lathe, sweep, circle, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT, L, ml)

MODELS, model = K.registry()
ASSIGN = {}                      # chest relics are assigned by gen_hand_all.py from their look and their worn models

# which garment a chest look is held as
GARMENT_OF_LOOK = {"PLATE": "garment_cuirass", "ROBE": "garment_robe", "COAT": "garment_coat", "SUIT": "garment_coat", "LEATHER": "garment_vest"}


def _torso(m, tile, slot=MAIN, widths=((0, 6.8, 3.4), (2, 7.0, 3.5), (7, 7.4, 3.7), (10, 7.6, 3.7), (12, 6.2, 3.2)), flags=0):
    p = Part("torso")
    loft(p, "y", (0, -12.0, 0), list(widths), L.octagon(1.0, 1.0, 0.24))
    return m.add(p, slot, tile, flags)


@model
def garment_cuirass():
    """A plate cuirass: a shaped breast and back plate with a ridge down the front, shoulder straps, a belt and three hanging tassets over the hips."""
    m = K.new("garment_cuirass")
    _torso(m, "plate")
    K.block(m, "keel", (-0.5, -9.0, -2.3), (0.5, -0.5, -1.7), TRIM, "gilt", 0.0)
    for s in (-1, 1):
        K.block(m, "breast%d" % s, (s * 0.5 if s > 0 else -3.6, -7.0, -2.15), (3.6 if s > 0 else -0.5, -1.4, -1.75), MAIN, "plate", 0.0, uv="fit")
        K.block(m, "strap%d" % s, (s * 3.0 - 0.6, -0.2, -1.9), (s * 3.0 + 0.6, 0.6, 1.9), TRIM, "gilt", 0.0)
    K.cyl(m, "belt", (0, -11.2, 0), 3.9, 1.0, TRIM, "gilt", sides=8, squash=0.5)
    for i, x in enumerate((-2.4, 0.0, 2.4)):
        K.block(m, "tasset%d" % i, (x - 1.1, -15.0, -2.0), (x + 1.1, -11.6, -1.6), MAIN, "plate", 0.0, uv="fit")
    return m


@model
def garment_robe():
    """A long robe: a flared body down to the hem, a sash round the waist, wide flared sleeves held out to the sides and a rolled hood collar at the neck."""
    m = K.new("garment_robe")
    p = Part("body")
    loft(p, "y", (0, -25.0, 0), [(0.0, 5.8, 5.0), (6.0, 5.0, 4.2), (14.0, 3.9, 2.9), (22.0, 3.7, 2.1), (25.0, 2.6, 1.9)], L.circle(1.0, 10))
    m.add(p, MAIN, "cloth")
    K.cyl(m, "sash", (0, -11.0, 0), 3.9, 1.3, TRIM, "silk", sides=10, squash=0.6)
    for s in (-1, 1):
        K.rod(m, "sleeve%d" % s, K.bezier((s * 3.0, -1.4, 0), (s * 6.0, -3.0, 0), (s * 9.6, -8.0, -0.4)), 1.6, MAIN, "cloth", 8,
              scales=[(0.9, 0.9), (1.0, 1.0), (1.3, 1.3), (1.7, 1.7), (2.2, 2.2), (2.6, 2.6)])
        K.cyl(m, "cuff%d" % s, (s * 9.3, -7.6, -0.4), 2.7, 0.7, TRIM, "silk", sides=8)
    K.ring(m, "collar", (0, 0.2, 0.4), 3.2, 0.9, TRIM, "silk", "xz", 12, 4)
    K.ball(m, "hood", (0, -1.6, 2.4), 2.6, MAIN, "cloth", 3, 8, 0.8)
    return m


@model
def garment_coat():
    """A coat: a fitted jacket with a turned collar, two lapels with buttons, short front skirts and two long tails hanging at the back."""
    m = K.new("garment_coat")
    _torso(m, "cloth", MAIN, ((0, 6.8, 3.4), (2, 7.2, 3.5), (7, 7.4, 3.7), (10, 7.6, 3.7), (12, 6.2, 3.2)))
    for s in (-1, 1):
        K.rod(m, "lapel%d" % s, [(s * 0.6, -0.6, -2.1), (s * 2.4, -4.0, -2.1), (s * 1.6, -8.0, -2.1)], 0.9, TRIM, "silk", 4, scales=[(1.0, 0.4), (1.5, 0.4), (0.8, 0.4)])
        K.block(m, "front%d" % s, (s * 1.0 - (1.7 if s < 0 else -0.0), -17.0, -2.0), (s * 1.0 + (0.0 if s < 0 else 1.7) + s * 2.2, -11.6, -1.5), MAIN, "cloth", 0.2)
        K.block(m, "tail%d" % s, (s * 1.8 - 1.5, -22.0, 1.4), (s * 1.8 + 1.5, -11.6, 1.9), MAIN, "cloth", 0.2)
    for i in range(3):
        K.ball(m, "button%d" % i, (0, -3.0 - i * 2.4, -2.15), 0.4, TRIM, "gilt", 3, 6)
    K.ring(m, "collar", (0, 0.2, 0.2), 3.0, 0.7, MAIN, "silk", "xz", 12, 4)
    return m


@model
def garment_vest():
    """A leather vest: a short hide jerkin with two crossed straps over the chest, a broad belt with a buckle and a ruff of fur round the neck."""
    m = K.new("garment_vest")
    _torso(m, "leather", MAIN, ((0, 6.6, 3.3), (2, 6.8, 3.4), (8, 7.4, 3.7), (11, 7.4, 3.6), (12, 6.2, 3.2)))
    for s in (-1, 1):
        K.rod(m, "strap%d" % s, [(s * 3.2, -0.6, -2.0), (0.0, -6.0, -2.15), (-s * 3.2, -11.0, -2.0)], 0.5, TRIM, "leather", 4, scales=[(1.0, 0.5)] * 3)
    K.cyl(m, "belt", (0, -10.6, 0), 3.9, 1.6, DARK, "leather", sides=10, squash=0.55)
    K.block(m, "buckle", (-0.9, -11.3, -2.3), (0.9, -9.9, -2.1), TRIM, "gilt", 0.2)
    K.ring(m, "ruff", (0, 0.2, 0.0), 3.3, 1.0, ACCENT, "fur", "xz", 12, 4)
    return m


# ---------------------------------------------------------------- lite bodies, for the relics whose own ornaments are already heavy (the budget of one relic is 2600 vertices)
@model
def garment_cuirass_lite():
    """The plate cuirass reduced to its body and a belt: for a relic whose ornaments use most of the vertex budget."""
    m = K.new("garment_cuirass_lite")
    _torso(m, "plate", MAIN, ((0, 6.8, 3.4), (7, 7.4, 3.7), (12, 6.2, 3.2)))
    K.cyl(m, "belt", (0, -11.2, 0), 3.9, 1.0, TRIM, "gilt", sides=8, squash=0.5)
    return m


@model
def garment_robe_lite():
    """The long robe reduced to its flared body and a sash."""
    m = K.new("garment_robe_lite")
    p = Part("body")
    loft(p, "y", (0, -25.0, 0), [(0.0, 5.8, 5.0), (14.0, 3.9, 2.9), (25.0, 2.6, 1.9)], L.circle(1.0, 8))
    m.add(p, MAIN, "cloth")
    K.cyl(m, "sash", (0, -11.0, 0), 3.9, 1.3, TRIM, "silk", sides=8, squash=0.6)
    return m


@model
def garment_coat_lite():
    """The coat reduced to its jacket and two tails."""
    m = K.new("garment_coat_lite")
    _torso(m, "cloth", MAIN, ((0, 6.8, 3.4), (7, 7.4, 3.7), (12, 6.2, 3.2)))
    for s in (-1, 1):
        K.block(m, "tail%d" % s, (s * 1.8 - 1.5, -22.0, 1.4), (s * 1.8 + 1.5, -11.6, 1.9), MAIN, "cloth", 0.0)
    return m


@model
def garment_vest_lite():
    """The leather vest reduced to its body and belt."""
    m = K.new("garment_vest_lite")
    _torso(m, "leather", MAIN, ((0, 6.6, 3.3), (8, 7.4, 3.7), (12, 6.2, 3.2)))
    K.cyl(m, "belt", (0, -10.6, 0), 3.9, 1.6, DARK, "leather", sides=8, squash=0.55)
    return m
