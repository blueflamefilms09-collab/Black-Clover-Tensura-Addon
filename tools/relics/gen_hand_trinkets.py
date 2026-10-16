"""gen_hand_trinkets: the signature hand models of the small relics, part 2: the rings, the bags and satchels, the figurines and toys, the books, the badges and brooches,
the coins and the odd charms. Same frame and kit as gen_hand_sig.py (see gen_hand_kit.py); the ASSIGN table at the end says which relic is held as which model.
"""
import math

import numpy as np

import gen_hand_kit as K
from gen_hand_kit import (Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT, L, ml)

MODELS, model = K.registry()
ASSIGN = {}


# ================================================================ rings
def _band(m, c=(0, 0, 0), a=3.6, t=1.0, w=1.8, slot=TRIM, tile="gilt", n=16):
    return K.band_z(m, "band", c, a, t, w, slot, tile, n)


def _setting(m, y, gem_slot=GLOW, tile="gem", r=1.5, long=1.5, claws=True):
    """A stone on top of the band (y is the top of the band): a cut gem with four claws."""
    K.jewel(m, "gem", (0, y + r * 0.9, -0.2), r, gem_slot, long, 6, "y", EMIT | HOT, tile=tile)
    if claws:
        for i in range(4):
            a = math.pi / 4 + i * math.pi / 2
            K.spike(m, "claw%d" % i, (1.1 * math.cos(a), y - 0.3, 1.1 * math.sin(a) - 0.2), (1.5 * math.cos(a), y + r * 1.3, 1.5 * math.sin(a) - 0.2), 0.35, TRIM, "gilt", 4, 0.5, 0.8)


@model
def ring_rage():
    """The Ring of Endless Rage: a heavy band with a crown of small spikes round its outside and a red flame-shaped stone burning on top."""
    m = K.new("ring_rage")
    _band(m, a=3.4, t=1.2, w=2.0, slot=MAIN, tile="metal")
    for i in range(9):
        a = (i + 0.5) * 2 * math.pi / 9
        if math.sin(a) > 0.8:
            continue                                                  # the top is the flame's
        K.spike(m, "thorn%d" % i, (4.4 * math.cos(a), 4.4 * math.sin(a), 0), (6.0 * math.cos(a), 6.0 * math.sin(a), 0), 0.45, TRIM, "metal", 4, 0.4, 0.9)
    p = Part("flame")
    lathe(p, "y", (0, 4.7, -0.2), [(0.0, 0.0), (0.8, 1.2), (2.0, 1.1), (3.4, 0.6), (4.8, 0.0)], sides=6)
    m.add(p, GLOW, "flame", EMIT | HOT, 0, "fit")
    return m


@model
def ring_memories():
    """The Ring of Memories: a dark band wound all round with fine wire and a pale cyan crystal on top, a thread of light curling off the crystal like a remembered road."""
    m = K.new("ring_memories")
    _band(m, a=3.4, t=0.9, w=1.8, slot=MAIN, tile="leather")
    pts = []
    for i in range(37):
        th = i * 2 * math.pi / 36
        k = 6
        rr = 3.9 + 0.75 * math.cos(k * th)
        pts.append((rr * math.cos(th), rr * math.sin(th), 1.0 * math.sin(k * th)))
    K.rod(m, "wire", pts, 0.2, TRIM, "chain", 4, caps=(False, False))
    _setting(m, 4.3, GLOW, "crystal", 1.4, 1.7)
    g = m.group("road", "bob", (0, 8.0, 0), 0.4, 0.1, 0.0)
    K.rod(m, "road", K.bezier((0, 7.2, -0.2), (1.8, 8.6, 0), (-1.6, 10.0, 0), (0.4, 11.6, -0.2), 7), 0.3, NONE, "white", 4, EMIT, g)
    return m


@model
def ring_power():
    """The Ring of Power: a plain, broad, heavy band of gold with a line of fine runes burning along its outside, no stone at all, only its weight."""
    m = K.new("ring_power")
    _band(m, a=3.5, t=1.5, w=2.6, slot=MAIN, tile="gilt", n=18)
    K.band_z(m, "edge1", (0, 0, -1.4), 3.2, 1.9, 0.35, TRIM, "gilt", 18)
    K.band_z(m, "edge2", (0, 0, 1.4), 3.2, 1.9, 0.35, TRIM, "gilt", 18)
    for i in range(18):
        a = i * 2 * math.pi / 18
        K.block(m, "rune%d" % i, (5.05 * math.cos(a) - 0.3, 5.05 * math.sin(a) - 0.3, -0.7 if i % 2 else -0.3), (5.05 * math.cos(a) + 0.3, 5.05 * math.sin(a) + 0.3, 0.7 if i % 3 else 0.4), GLOW, "rune", 0.0, EMIT | HOT)
    return m


@model
def ring_regeneration():
    """The Ring of Regeneration: a thin green-gold band that grows two small leaves and a curled shoot round a round green stone, the shoot slowly unfurling."""
    m = K.new("ring_regeneration")
    _band(m, a=3.5, t=0.7, w=1.5, slot=TRIM, tile="gilt")
    K.ball(m, "stone", (0, 5.2, -0.2), 1.5, GLOW, "gem", 4, 8, 0.9, EMIT | HOT, uv="fit")
    for s in (-1, 1):
        K.rod(m, "leaf%d" % s, K.bezier((s * 1.3, 4.3, 0), (s * 3.4, 5.6, 0), (s * 4.2, 7.2, 0.1), 6), 0.6, MAIN, "vein", 4,
              scales=[(0.3, 0.3), (1.0, 0.4), (1.2, 0.4), (1.0, 0.4), (0.6, 0.3), (0.2, 0.2), (0.1, 0.1)])
    g = m.group("shoot", "sway", (0, 4.3, 0), 6.0, 0.08, 0.0)
    K.rod(m, "shoot", K.bezier((0, 4.4, 0.3), (0.0, 7.4, 0.6), (1.6, 8.6, 0.4), (1.6, 7.6, 0.2), 7), 0.28, MAIN, "vein", 4, group=g)
    return m


@model
def ring_rock_eating():
    """The Ring of Rock Eating: a rough ring of grey stone, chipped and knobbly, with a chunk of raw ore stuck in it like a bite taken out of a mountain."""
    m = K.new("ring_rock_eating")
    _band(m, a=3.3, t=1.4, w=2.2, slot=MAIN, tile="scale", n=10)
    for i, (a, r, sq) in enumerate(((0.4, 1.0, 0.8), (1.9, 0.8, 1.0), (3.4, 0.9, 0.7), (4.6, 1.1, 0.9), (5.7, 0.8, 1.0))):
        K.ball(m, "lump%d" % i, (4.8 * math.cos(a), 4.8 * math.sin(a), 0), r, DARK, "scale", 3, 6, sq)
    K.block(m, "ore", (-1.6, 4.4, -1.4), (1.6, 7.2, 1.4), GLOW, "crystal", 0.7, EMIT | HOT, uv="fit")
    K.octa(m, "ore2", (1.2, 7.6, -0.4), 1.0, 1.2, 0.9, GLOW, EMIT)
    return m


@model
def ring_game():
    """The Ring of the Game: a plain gilt band with a small white die standing on top of it, its pips showing, and a tiny pawn beside it."""
    m = K.new("ring_game")
    _band(m, a=3.5, t=0.9, w=1.8)
    K.block(m, "die", (-1.7, 4.4, -1.7), (1.7, 7.8, 1.7), NONE, "white", 0.4, uv="fit")
    for i, (x, y) in enumerate(((-0.8, 5.2), (0.8, 5.2), (-0.8, 7.0), (0.8, 7.0), (0.0, 6.1))):
        K.disc(m, "pip%d" % i, (x, y, -1.75), 0.38, 0.2, DARK, "black", "z", 6, 0.0)
    p = Part("pawn")
    lathe(p, "y", (3.4, 4.4, 0), [(0.0, 1.0), (0.4, 0.8), (1.6, 0.4), (2.2, 0.7), (2.6, 0.0)], sides=8)
    m.add(p, TRIM, "gilt")
    return m


@model
def ring_mimic():
    """The Ring of the Mimic: a band that carries a tiny treasure chest on top with its lid ajar, a row of teeth along the gap and a red tongue between them."""
    m = K.new("ring_mimic")
    _band(m, a=3.5, t=0.9, w=1.8, slot=MAIN, tile="wood")
    K.block(m, "chest", (-2.2, 4.2, -1.6), (2.2, 6.0, 1.6), MAIN, "wood", 0.2)
    K.block(m, "lid", (-2.2, 6.6, -1.6), (2.2, 8.0, 1.0), TRIM, "wood", 0.2)
    K.block(m, "strap", (-0.4, 4.2, -1.75), (0.4, 8.0, -1.5), TRIM, "gilt", 0.0)
    for i in range(5):
        x = -1.8 + i * 0.9
        K.spike(m, "tooth%d" % i, (x, 6.0, -1.5), (x, 6.9, -1.5), 0.3, NONE, "bone", 4, 0.5, 0.9)
        K.spike(m, "tooth_u%d" % i, (x, 6.6, -1.5), (x, 5.7, -1.5), 0.3, NONE, "bone", 4, 0.5, 0.9)
    K.block(m, "tongue", (-0.7, 5.9, -1.6), (0.7, 6.7, -0.2), GLOW, "soft", 0.0, EMIT, uv="fit")
    return m


@model
def ring_forgotten():
    """The Forgotten Ring: a thin worn band, nearly smooth, with a dull dark stone in it that has a hairline crack, and a speck of dust floating by."""
    m = K.new("ring_forgotten")
    _band(m, a=3.5, t=0.6, w=1.4, slot=MAIN, tile="metal", n=14)
    K.octa(m, "stone", (0, 4.6, -0.2), 1.3, 1.5, 1.0, DARK, 0, tile="crystal")
    K.rod(m, "crack", [(-0.9, 4.0, -1.1), (0.0, 4.7, -1.2), (0.7, 5.8, -1.0)], 0.1, GLOW, "spark", 3, EMIT)
    g = m.group("dust", "bob", (0, 7.0, 0), 0.6, 0.07, 0.0)
    K.octa(m, "dust", (2.0, 7.4, 0), 0.25, 0.25, 0.25, ACCENT, EMIT, g)
    return m


@model
def ring_witch():
    """The Witch's Ring: a band of two twisted wires with a black cat-eye stone, its slit pupil glowing, and a tiny crescent moon fixed at the side."""
    m = K.new("ring_witch")
    for k, ph in enumerate((0.0, math.pi)):
        pts = []
        for i in range(37):
            th = i * 2 * math.pi / 36
            rr = 3.9 + 0.55 * math.cos(5 * th + ph)
            pts.append((rr * math.cos(th), rr * math.sin(th), 0.7 * math.sin(5 * th + ph)))
        K.rod(m, "wire%d" % k, pts, 0.38, MAIN if k else TRIM, "gilt", 4, caps=(False, False))
    K.ball(m, "eye", (0, 5.0, -0.2), 1.6, DARK, "black", 4, 8, 0.9, 0, uv="fit")
    K.block(m, "pupil", (-0.15, 4.2, -1.8), (0.15, 5.9, -1.5), GLOW, "spark", 0.0, EMIT | HOT, uv="fit")
    arc = [(4.9 + 1.4 * math.cos(a), 0.4 + 1.4 * math.sin(a), 0) for a in [math.pi * (-0.5 + i / 8) for i in range(9)]]
    K.rod(m, "moon", arc, 0.28, ACCENT, "gilt", 4)
    return m


@model
def ring_dryad():
    """The Dryad's Wedding Band: a ring woven from two living twigs, with a white five-petalled blossom open on top and a green bud beside it."""
    m = K.new("ring_dryad")
    for k, ph in enumerate((0.0, math.pi)):
        pts = []
        for i in range(37):
            th = i * 2 * math.pi / 36
            rr = 3.8 + 0.5 * math.cos(4 * th + ph)
            pts.append((rr * math.cos(th), rr * math.sin(th), 0.6 * math.sin(4 * th + ph)))
        K.rod(m, "twig%d" % k, pts, 0.42, MAIN, "wood", 4, caps=(False, False))
    for i in range(5):
        a = i * 2 * math.pi / 5
        K.ball(m, "petal%d" % i, (1.0 * math.cos(a), 5.2 + 1.0 * math.sin(a), -0.6), 0.85, NONE, "pearl", 3, 6, 0.6, 0, uv="fit")
    K.ball(m, "heart", (0, 5.2, -0.9), 0.55, GLOW, "gem", 3, 6, 1.0, EMIT, uv="fit")
    K.ball(m, "bud", (3.0, 5.0, 0), 0.7, TRIM, "vein", 3, 6, 1.3)
    return m


@model
def eye_deep_one():
    """The Eye of the Deep One: a dark green ring of eight curling tentacles round one big glowing blue eye with a slit pupil."""
    m = K.new("eye_deep_one")
    p = Part("eye")
    lathe(p, "z", (0, 0, 0), [(-2.2, 0.0), (-1.9, 2.0), (-1.0, 3.2), (0.0, 3.6), (1.0, 3.2), (1.9, 2.0), (2.2, 0.0)], sides=12)
    m.add(p, NONE, "pearl", 0, 0, "fit")
    K.disc(m, "iris", (0, 0, -2.3), 2.3, 0.5, GLOW, "soft", "z", 12, EMIT | HOT, 0.1, uv="fit")
    K.block(m, "pupil", (-0.25, -1.5, -2.9), (0.25, 1.5, -2.3), DARK, "black", 0.1, uv="fit")
    for i in range(8):
        a = i * math.pi / 4
        c, s = math.cos(a), math.sin(a)
        K.rod(m, "tent%d" % i, K.bezier((c * 3.4, s * 3.4, 0), (c * 6.6, s * 6.6, 0.4), (c * 6.4 - s * 2.2, s * 6.4 + c * 2.2, -0.6)), 0.7, MAIN, "vein", 5,
              scales=[(1.0, 1.0), (0.95, 0.95), (0.85, 0.85), (0.7, 0.7), (0.55, 0.55), (0.4, 0.4), (0.25, 0.25), (0.15, 0.15)])
    return m


# ================================================================ bags and satchels
def _sack(m, slot=MAIN, tile="leather"):
    p = Part("sack")
    lathe(p, "y", (0, -6.0, 0), [(0.0, 0.0), (0.4, 2.6), (2.4, 4.7), (5.6, 5.2), (8.4, 4.0), (10.2, 2.2), (11.0, 1.5), (11.8, 2.0), (12.8, 2.9), (13.4, 2.7)], sides=10)
    m.add(p, slot, tile)
    K.ring(m, "tie", (0, 5.6, 0), 1.5, 0.35, TRIM, "leather", "xz", 10, 4)
    K.rod(m, "cord1", K.bezier((1.4, 5.6, 0), (2.4, 4.4, -0.8), (2.0, 3.0, -1.0)), 0.18, TRIM, "leather", 4)
    K.rod(m, "cord2", K.bezier((-1.2, 5.6, 0.6), (-2.6, 4.6, 0.8), (-2.4, 3.2, 1.4)), 0.18, TRIM, "leather", 4)


@model
def bag_holding():
    """The Bag of Holding: a deep patched sack with a tied neck and a four-pointed star stitched in light on its front."""
    m = K.new("bag_holding")
    _sack(m)
    K.octa(m, "star_v", (0, -0.5, -5.2), 0.7, 2.6, 0.3, GLOW, EMIT | HOT)
    K.octa(m, "star_h", (0, -0.5, -5.2), 2.6, 0.7, 0.3, GLOW, EMIT | HOT)
    K.block(m, "patch", (2.0, -3.6, -4.6), (4.4, -1.6, -3.9), ACCENT, "cloth", 0.0)
    return m


@model
def bag_devouring():
    """The Bag of Devouring: a dark sack with a wide ragged mouth on its front, a ring of jagged teeth, a red tongue and two small hungry eyes above it."""
    m = K.new("bag_devouring")
    _sack(m, slot=DARK)
    K.disc(m, "maw", (0, -1.0, -5.1), 3.2, 0.5, DARK, "black", "z", 12, 0, 0.2, uv="fit")
    K.block(m, "tongue", (-1.2, -2.6, -5.5), (1.2, -0.4, -5.2), GLOW, "soft", 0.0, EMIT, uv="fit")
    for i in range(10):
        a = i * math.pi / 5
        K.spike(m, "tooth%d" % i, (3.0 * math.cos(a), -1.0 + 3.0 * math.sin(a), -5.2), (1.7 * math.cos(a), -1.0 + 1.7 * math.sin(a), -5.6), 0.4, NONE, "bone", 4, 0.5, 0.9)
    for s in (-1, 1):
        K.jewel(m, "eye%d" % s, (s * 2.0, 3.4, -4.6), 0.5, GLOW, 1.3, 4, "x", EMIT | HOT)
    return m


@model
def bag_kittens():
    """The Bag of Kittens: a sack with two pointed ears and a cat's face on it, whiskers, a pink nose, and a small curled tail on its side."""
    m = K.new("bag_kittens")
    _sack(m)
    for s in (-1, 1):
        K.spike(m, "ear%d" % s, (s * 3.0, 5.4, -1.0), (s * 3.6, 9.6, -1.0), 1.6, MAIN, "fur", 4, 0.4, 0.9)
        K.spike(m, "earin%d" % s, (s * 3.0, 5.6, -1.9), (s * 3.5, 8.6, -1.9), 0.8, ACCENT, "soft", 4, 0.4, 0.9)
        K.jewel(m, "eye%d" % s, (s * 1.8, 0.8, -5.0), 0.6, GLOW, 1.3, 4, "x", EMIT)
        for k in range(2):
            K.rod(m, "whisker%d%d" % (s, k), [(s * 2.4, -1.4 - k * 0.7, -5.2), (s * 5.4, -1.0 - k * 1.3, -5.0)], 0.1, NONE, "white", 3)
    K.octa(m, "nose", (0, -0.8, -5.3), 0.5, 0.4, 0.3, ACCENT, 0)
    K.rod(m, "tail", K.bezier((4.8, -4.0, 0), (8.4, -3.0, 0), (8.0, 1.0, 0), (6.6, 2.4, 0), 7), 0.6, MAIN, "fur", 5, scales=[(1.1, 1.1)] * 5 + [(0.8, 0.8)] * 3)
    return m


def _emblem_bag(name, doc, build):
    def fn():
        m = K.new(name)
        _sack(m)
        build(m)
        return m
    fn.__name__ = name
    fn.__doc__ = doc
    MODELS[name] = fn
    return fn


def _cold(m):
    for i, x in enumerate((-2.4, 0.0, 2.4)):
        K.spike(m, "icicle%d" % i, (x, 4.4, -3.6), (x, 4.4 - (2.6 if i == 1 else 1.8), -3.6), 0.5, NONE, "snow", 4, 0.6, 0.9)
    for k in range(3):
        a = k * math.pi / 3
        K.block(m, "flake%d" % k, (-2.0 * math.cos(a) - 0.18, -1.2 - 2.0 * math.sin(a) - 0.18, -5.3), (2.0 * math.cos(a) + 0.18, -1.2 + 2.0 * math.sin(a) + 0.18, -5.0), ACCENT, "snow", 0.0, EMIT)


def _gold(m):
    K.disc(m, "coin", (0, -1.0, -5.1), 2.4, 0.5, NONE, "gold_fixed", "z", 12, EMIT, 0.1)
    K.disc(m, "coinin", (0, -1.0, -5.45), 1.6, 0.2, TRIM, "gilt", "z", 12, 0, 0.0)


def _mold(m):
    for i, (x, y, r) in enumerate(((-2.0, 1.0, 1.1), (1.6, -1.2, 1.4), (-0.8, -3.0, 0.9), (2.4, 2.2, 0.8), (-2.4, -1.6, 0.7))):
        K.ball(m, "mold%d" % i, (x, y, -4.9), r, GLOW, "soft", 3, 6, 0.6, EMIT, uv="fit")


def _scold(m):
    K.block(m, "bang", (-0.7, -0.4, -5.3), (0.7, 3.6, -4.9), GLOW, "spark", 0.1, EMIT | HOT, uv="fit")
    K.ball(m, "dot", (0, -2.2, -5.1), 0.8, GLOW, "spark", 3, 6, 0.6, EMIT | HOT, uv="fit")


def _unfold(m):
    for i, (p0, p1) in enumerate((((0, 3.0), (3.0, 0)), ((3.0, 0), (0, -3.0)), ((0, -3.0), (-3.0, 0)), ((-3.0, 0), (0, 3.0)))):
        K.rod(m, "fold%d" % i, [(p0[0], p0[1] - 0.8, -5.2), (p1[0], p1[1] - 0.8, -5.2)], 0.2, ACCENT, "gilt", 4)
    K.octa(m, "flap", (2.6, 3.6, -4.8), 1.8, 1.8, 0.2, TRIM, 0, tile="plate")


_emblem_bag("bag_colding", "The Bag of Colding: a sack with three small icicles hanging from its neck and a frost flake of light on the front.", _cold)
_emblem_bag("bag_golding", "The Bag of Golding: a sack with a big gold coin shining on the front.", _gold)
_emblem_bag("bag_molding", "The Bag of Molding: a sack with soft green mould blooms all over its front.", _mold)
_emblem_bag("bag_scolding", "The Bag of Scolding: a sack with a glowing exclamation mark on the front.", _scold)
_emblem_bag("bag_unfolding", "The Bag of Unfolding: a sack with a diamond of fold lines on the front and one corner flap turned back.", _unfold)


@model
def satchel_unmet():
    """The Satchel of Unmet Needs: a squarish leather satchel with a deep flap, a brass buckle and a short strap, and a crooked paper list sticking out of one side."""
    m = K.new("satchel_unmet")
    K.block(m, "body", (-5.0, -5.0, -2.2), (5.0, 3.4, 2.2), MAIN, "leather", 0.7)
    K.block(m, "flap", (-5.2, -0.6, -2.5), (5.2, 4.6, -1.8), DARK, "leather", 0.5)
    K.block(m, "buckle", (-1.1, -1.6, -2.8), (1.1, 0.8, -2.4), TRIM, "gilt", 0.2)
    K.block(m, "tongue", (-0.3, -3.2, -2.7), (0.3, -0.6, -2.5), TRIM, "gilt", 0.0)
    K.rod(m, "strap", K.bezier((-5.0, 3.0, 0), (-5.6, 9.0, 0), (5.6, 9.0, 0), (5.0, 3.0, 0), 9), 0.5, DARK, "leather", 4)
    K.block(m, "list", (2.4, 1.0, -0.4), (4.2, 5.8, -0.1), NONE, "paper", 0.0, uv="fit")
    return m


@model
def satchel_dragon():
    """The Dragon Satchel: a scaled satchel with a flap shaped like a dragon's brow, a claw for a buckle, and a row of small spines along the strap."""
    m = K.new("satchel_dragon")
    K.block(m, "body", (-5.0, -5.0, -2.4), (5.0, 3.0, 2.4), MAIN, "scale", 0.8)
    K.block(m, "flap", (-5.2, -0.4, -2.7), (5.2, 4.4, -2.0), DARK, "scale", 0.5)
    for s in (-1, 1):
        K.spike(m, "horn%d" % s, (s * 3.6, 4.4, -2.4), (s * 4.6, 7.2, -2.4), 0.7, TRIM, "horn", 4, 0.4, 0.9)
    K.rod(m, "claw", K.bezier((0, 1.2, -3.0), (1.2, 0.2, -3.6), (0.8, -1.8, -3.4)), 0.45, TRIM, "bone", 4, scales=[(1.2, 1.2)] * 3 + [(0.8, 0.8), (0.4, 0.4), (0.2, 0.2)])
    K.rod(m, "strap", K.bezier((-5.0, 2.6, 0), (-5.6, 9.0, 0), (5.6, 9.0, 0), (5.0, 2.6, 0), 9), 0.6, DARK, "scale", 4)
    for i in range(5):
        t = (i + 0.5) / 5
        K.spike(m, "spine%d" % i, (-4.4 + 8.8 * t, 7.0, 0), (-4.4 + 8.8 * t, 8.2, 0), 0.3, TRIM, "horn", 4, 0.4, 0.9)
    return m


@model
def wallet_slayer():
    """The Wallet of the Slayer: a thick black leather wallet, folded shut, a small silver skull stud on its flap and a belt chain looping off one corner."""
    m = K.new("wallet_slayer")
    K.block(m, "body", (-4.8, -3.4, -1.8), (4.8, 3.4, 1.8), MAIN, "leather", 0.6)
    K.block(m, "flap", (-4.9, 0.4, -2.2), (4.9, 3.5, -1.6), DARK, "leather", 0.4)
    K.ball(m, "skull", (0, 0.4, -2.7), 1.0, NONE, "bone", 4, 8, 1.0)
    K.block(m, "jaw", (-0.5, -0.9, -2.8), (0.5, -0.4, -2.2), NONE, "bone", 0.1)
    for s in (-1, 1):
        K.jewel(m, "eye%d" % s, (s * 0.38, 0.6, -3.6), 0.22, GLOW, 1.2, 4, "z", EMIT)
    K.ring(m, "ring", (5.2, 2.6, 0), 0.7, 0.2, TRIM, "metal", "xy", 8, 4)
    K.rod(m, "chain", K.bezier((5.9, 2.6, 0), (9.0, 1.0, 0), (9.0, -4.0, 0), (6.0, -6.0, 0), 8), 0.22, TRIM, "chain", 4)
    return m


@model
def pouch_sand():
    """The Sand of the Pocket: a small drawstring pouch with a pinch of golden sand trickling out of its mouth and a little heap of sand below."""
    m = K.new("pouch_sand")
    p = Part("pouch")
    lathe(p, "y", (0, -4.0, 0), [(0.0, 0.0), (0.3, 2.0), (2.0, 3.8), (4.4, 4.0), (6.4, 2.8), (7.6, 1.4), (8.2, 1.9), (9.2, 2.4)], sides=10)
    m.add(p, MAIN, "cloth")
    K.ring(m, "tie", (0, 3.8, 0), 1.3, 0.3, TRIM, "leather", "xz", 8, 4)
    g = m.group("trickle", "bob", (0, 4.0, 0), 0.2, 0.2, 0.0)
    K.rod(m, "trickle", [(0.2, 5.2, -1.4), (0.5, 3.6, -3.4), (0.6, 0.0, -3.9), (0.6, -3.4, -3.9)], 0.22, NONE, "honey", 4, EMIT, g)
    p = Part("heap")
    lathe(p, "y", (0, -4.6, -3.4), [(0.0, 2.8), (0.5, 2.2), (1.2, 1.0), (1.6, 0.0)], sides=8)
    m.add(p, TRIM, "honey")
    return m


# ================================================================ figurines and toys
@model
def doll_snugglable():
    """The Snugglable Doll: a soft round-headed rag doll with button eyes, a little stitched smile, floppy arms and legs and a patch on its belly."""
    m = K.new("doll_snugglable")
    K.ball(m, "head", (0, 5.0, 0), 3.4, MAIN, "cloth", 4, 8, 1.0)
    K.ball(m, "body", (0, -0.6, 0), 3.2, MAIN, "cloth", 4, 8, 1.15)
    for s in (-1, 1):
        K.ball(m, "button%d" % s, (s * 1.3, 5.6, -3.2), 0.55, DARK, "black", 3, 6, 1.0)
        K.rod(m, "arm%d" % s, K.bezier((s * 2.6, 0.8, 0), (s * 5.6, 1.4, 0), (s * 6.0, -1.8, 0.4), 6), 0.95, MAIN, "cloth", 6, scales=[(1.0, 1.0)] * 5 + [(0.9, 0.9)] * 2)
        K.rod(m, "leg%d" % s, K.bezier((s * 1.4, -3.2, 0), (s * 2.2, -6.0, 0), (s * 1.8, -8.2, -1.6), 6), 0.9, MAIN, "cloth", 6, scales=[(1.0, 1.0)] * 5 + [(0.9, 0.9)] * 2)
        K.ball(m, "ear%d" % s, (s * 3.0, 8.0, 0), 1.0, TRIM, "cloth", 3, 6)
    K.rod(m, "smile", [(-1.2, 3.8, -3.3), (-0.5, 3.3, -3.4), (0.5, 3.3, -3.4), (1.2, 3.8, -3.3)], 0.15, DARK, "black", 3)
    K.block(m, "patch", (-1.3, -2.0, -3.4), (1.3, 0.4, -3.1), ACCENT, "stripe", 0.0)
    return m


@model
def perfume_bottle():
    """The Paralyzing Perfume: a faceted glass flask with a gilt collar and a pumped atomizer bulb on top, a tassel of silk, and violet liquid swirling inside."""
    m = K.new("perfume_bottle")
    p = Part("glass")
    lathe(p, "y", (0, -6.0, 0), [(0.0, 3.2), (0.5, 4.0), (3.0, 4.2), (6.0, 3.6), (7.4, 2.0), (8.0, 1.2)], sides=6)
    m.add(p, GLOW, "crystal", EMIT, 0, "fit")
    K.cyl(m, "collar", (0, 2.4, 0), 1.6, 1.1, TRIM, "gilt", sides=8)
    K.ball(m, "bulb", (0, 5.0, 0), 2.0, MAIN, "silk", 4, 8, 1.15)
    K.cyl(m, "neck", (0, 3.2, 0), 0.7, 1.2, TRIM, "gilt", sides=6)
    K.rod(m, "tube", [(2.0, 2.4, 0), (4.0, 0.0, 0), (3.0, -2.4, 0)], 0.2, TRIM, "silk", 4)
    K.ball(m, "tassel", (3.0, -3.2, 0), 0.7, TRIM, "silk", 3, 6)
    return m


@model
def sunscreen_bottle():
    """Drow Sunscreen: a fat white squeeze bottle with a flip cap, a big black sun crossed out on the front, and a glob of cream on the nozzle."""
    m = K.new("sunscreen_bottle")
    p = Part("bottle")
    lathe(p, "y", (0, -6.5, 0), [(0.0, 2.4), (0.6, 3.6), (4.0, 3.9), (8.0, 3.4), (9.6, 2.0), (10.4, 1.2)], sides=10, squash=0.6)
    m.add(p, NONE, "white", 0, 0, "fit")
    K.cyl(m, "cap", (0, 5.0, 0), 1.7, 2.0, MAIN, "plate", sides=8)
    K.ball(m, "glob", (1.6, 6.6, 0), 0.8, NONE, "white", 3, 6, 0.8, 0, uv="fit")
    K.disc(m, "sun", (0, 0.4, -2.45), 1.9, 0.3, DARK, "black", "z", 10, 0, 0.0, uv="fit")
    for i in range(8):
        a = i * math.pi / 4
        K.block(m, "ray%d" % i, (2.5 * math.cos(a) - 0.18, 0.4 + 2.5 * math.sin(a) - 0.18, -2.5), (3.3 * math.cos(a) + 0.18, 0.4 + 3.3 * math.sin(a) + 0.18, -2.3), DARK, "black", 0.0)
    K.rod(m, "cross", [(-3.0, -2.8, -2.6), (3.0, 3.4, -2.6)], 0.22, GLOW, "spark", 4, EMIT)
    return m


@model
def jar_bats():
    """The Jar of Bats: a round glass jar with a cork lid that three small black bats are circling, flapping, a faint violet glow from the glass and a bat-shaped smudge on its side."""
    m = K.new("jar_bats")
    p = Part("glass")
    lathe(p, "y", (0, -6.0, 0), [(0.0, 3.0), (0.6, 4.4), (4.0, 4.9), (8.0, 4.0), (9.6, 2.8)], sides=10)
    m.add(p, ACCENT, "crystal", EMIT, 0, "fit")
    K.cyl(m, "cork", (0, 4.0, 0), 2.8, 1.6, MAIN, "wood", sides=8)
    K.ball(m, "smudge", (0, -1.0, -4.9), 1.0, DARK, "black", 3, 6, 0.4)
    for s in (-1, 1):
        K.spike(m, "smudgewing%d" % s, (s * 0.8, -1.0, -4.9), (s * 2.8, -0.2, -4.7), 0.7, DARK, "black", 4, 0.4, 0.9)
    for i in range(3):
        g = m.group("orbit%d" % i, "orbit", (0, 8.0, 0), 2.2 + 0.4 * i, 0.0, 0.0)
        a = i * 2 * math.pi / 3
        x, y, z = 7.2 * math.cos(a), 6.5 + 1.6 * math.sin(a * 2), 7.2 * math.sin(a)
        K.ball(m, "bat%d" % i, (x, y, z), 0.7, DARK, "black", 3, 6, 1.0, 0, g)
        for s in (-1, 1):
            p2 = Part("wing%d_%d" % (i, s))
            L.quad_strip(p2, [(x + s * 0.4, y + 0.6, z), (x + s * 1.8, y + 1.3, z), (x + s * 3.0, y + 0.4, z)], [(x + s * 0.4, y - 0.5, z), (x + s * 1.5, y - 0.4, z), (x + s * 2.8, y - 1.1, z)], 0.15, True)
            m.add(p2, DARK, "black", 0, g, "tile")
    return m


@model
def idol_god():
    """The Portable God: a small golden idol seated cross-legged, many-armed, with a halo of light behind its head, a serene smile and a lotus plinth."""
    m = K.new("idol_god")
    p = Part("lotus")
    lathe(p, "y", (0, -7.0, 0), [(0.0, 3.6), (0.6, 4.4), (1.4, 3.8), (2.0, 2.8)], sides=10)
    m.add(p, ACCENT, "gilt")
    K.ball(m, "legs", (0, -4.2, 0), 3.4, MAIN, "gilt", 3, 8, 0.55)
    K.ball(m, "torso", (0, -0.4, 0), 2.4, MAIN, "gilt", 4, 8, 1.4)
    K.ball(m, "head", (0, 4.2, 0), 1.7, MAIN, "gilt", 4, 8, 1.1)
    K.spike(m, "crown", (0, 5.6, 0), (0, 8.0, 0), 1.2, TRIM, "gilt", 6, 0.5, 0.8)
    K.ring(m, "halo", (0, 4.4, 1.6), 3.8, 0.28, GLOW, "spark", "xy", 18, 4, EMIT | HOT)
    for i in range(6):
        a = (i - 2.5) * 0.55
        K.rod(m, "arm%d" % i, K.bezier((math.sin(a) * 2.2, 1.0, 0), (math.sin(a) * 5.2, 1.6, -0.6), (math.sin(a) * 6.4, -1.6, -1.4)), 0.4, MAIN, "gilt", 4,
              scales=[(1.0, 1.0)] * 4 + [(0.8, 0.8), (0.6, 0.6), (0.4, 0.4), (0.3, 0.3)])
    return m


@model
def idol_strange():
    """The Strange Idol: a squat stone figure with a wide mouth and three eyes in a column on its face, a pointed hat and its hands pressed to its knees, one eye faintly lit."""
    m = K.new("idol_strange")
    K.ball(m, "body", (0, -2.0, 0), 4.2, MAIN, "scale", 4, 8, 0.95)
    K.ball(m, "head", (0, 3.6, 0), 3.3, MAIN, "scale", 4, 8, 0.95)
    K.spike(m, "hat", (0, 6.0, 0), (0.6, 11.0, 0), 2.4, DARK, "scale", 6, 0.5, 0.6)
    for i, y in enumerate((5.0, 3.6, 2.2)):
        K.ball(m, "eye%d" % i, (0, y, -3.2), 0.65, GLOW if i == 1 else DARK, "gem" if i == 1 else "black", 3, 6, 0.6, EMIT | HOT if i == 1 else 0, uv="fit")
    K.block(m, "mouth", (-1.9, 0.2, -3.2), (1.9, 0.9, -2.9), DARK, "black", 0.0)
    for s in (-1, 1):
        K.ball(m, "hand%d" % s, (s * 3.4, -3.8, -2.2), 1.1, MAIN, "scale", 3, 6, 0.8)
        K.ball(m, "foot%d" % s, (s * 2.6, -6.2, -1.8), 1.3, MAIN, "scale", 3, 6, 0.6)
    return m


@model
def runechalk():
    """A Runechalk: a fat stick of chalk with a paper wrapper around its middle and a rune drawn on the wrapper, a dusty crumbled end and a few specks of glowing dust."""
    m = K.new("runechalk")
    K.cyl(m, "stick", (0, 0, 0), 1.9, 14.0, MAIN, "bone", sides=8)
    K.cyl(m, "wrap", (0, -1.0, 0), 2.1, 7.0, NONE, "paper", sides=8)
    K.octa(m, "rune", (0, -1.0, -2.2), 1.1, 1.5, 0.3, GLOW, EMIT | HOT)
    K.spike(m, "tip", (0, 7.0, 0), (0.0, 8.6, 0), 1.4, MAIN, "bone", 8, 0.3, 0.9)
    g = m.group("dust", "bob", (0, 9.0, 0), 0.5, 0.09, 0.0)
    for i, (x, y) in enumerate(((-1.4, 9.4), (1.2, 10.6), (0.2, 12.0))):
        K.octa(m, "dust%d" % i, (x, y, 0), 0.25, 0.25, 0.25, GLOW, EMIT, g)
    return m


@model
def spindle_weaversong():
    """Weaversong: a slender wooden spindle with a fat whorl and a cop of glowing thread wound on it, a loose strand singing off into a spiral of light."""
    m = K.new("spindle_weaversong")
    K.cyl(m, "shaft", (0, 0, 0), 0.5, 18.0, MAIN, "wood", sides=6)
    p = Part("whorl")
    lathe(p, "y", (0, -5.0, 0), [(0.0, 0.5), (0.4, 3.0), (1.2, 3.2), (1.8, 0.7)], sides=10)
    m.add(p, TRIM, "wood")
    p = Part("cop")
    lathe(p, "y", (0, -1.0, 0), [(0.0, 0.6), (0.8, 1.9), (3.6, 2.4), (6.0, 1.9), (6.8, 0.6)], sides=10)
    m.add(p, GLOW, "silk", EMIT | HOT, 0, "tile")
    K.spike(m, "tip", (0, -9.0, 0), (0, -11.0, 0), 0.5, DARK, "wood", 4, 0.4, 0.9)
    g = m.group("thread", "sway", (0, 6.0, 0), 8.0, 0.1, 0.0)
    K.rod(m, "thread", [(0.4 * math.cos(2.6 * t) * t * 5.0, 6.0 + t * 5.0, 0.4 * math.sin(2.6 * t) * t * 5.0) for t in [i / 14 for i in range(15)]], 0.18, NONE, "white", 4, EMIT, g)
    return m


def _coin(m, tile="gold_fixed", slot=NONE, r=5.0):
    K.disc(m, "coin", (0, 0, 0), r, 1.0, slot, tile, "z", 18, 0, 0.3)
    K.band_z(m, "rim", (0, 0, 0), r - 0.5, 0.7, 1.4, TRIM, "gilt", 18)


@model
def coin_draconic():
    """The Coin of Draconic Luck: a heavy gold coin with a ring of tiny dragon scales round its edge, a curled dragon pressed in the middle and a ruby at its eye."""
    m = K.new("coin_draconic")
    _coin(m)
    for i in range(14):
        a = i * 2 * math.pi / 14
        K.ball(m, "scale%d" % i, (4.0 * math.cos(a), 4.0 * math.sin(a), -0.7), 0.5, TRIM, "gilt", 3, 6, 0.5)
    K.rod(m, "dragon", [(2.2 * math.cos(a) * (1 - 0.5 * t), 2.2 * math.sin(a) * (1 - 0.5 * t), -0.9) for a, t in [(1.2 * math.pi * k / 12 - 0.5, k / 12) for k in range(13)]], 0.55, MAIN, "scale", 5,
          scales=[(1.0, 1.0)] * 9 + [(0.7, 0.7), (0.5, 0.5), (0.4, 0.4), (0.3, 0.3)])
    K.ball(m, "ruby", (0.2, 0.9, -1.3), 0.45, GLOW, "gem", 3, 6, 1.0, EMIT | HOT, uv="fit")
    return m


@model
def coin_fortune():
    """The Coin of Fortune: a bright coin with a four-leaf clover pressed into it, a shine of light that crosses it now and then, and a ribbed edge."""
    m = K.new("coin_fortune")
    _coin(m)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        K.ball(m, "leaf%d" % i, (1.4 * math.cos(a), 1.4 * math.sin(a), -0.9), 1.3, GLOW, "soft", 3, 6, 0.4, EMIT, uv="fit")
    K.rod(m, "stem", [(0.0, -0.6, -0.9), (0.6, -2.4, -0.9), (1.8, -3.6, -0.9)], 0.25, TRIM, "gilt", 4)
    for i in range(24):
        a = i * 2 * math.pi / 24
        K.block(m, "rib%d" % i, (5.0 * math.cos(a) - 0.2, 5.0 * math.sin(a) - 0.2, -0.5), (5.3 * math.cos(a) + 0.2, 5.3 * math.sin(a) + 0.2, 0.5), TRIM, "gilt", 0.0)
    return m


@model
def coin_devils_due():
    """The Devil's Due: a rusted, warm penny, dull brown and worn, with two small horns grown out of its top and a thin red glow in its cracks."""
    m = K.new("coin_devils_due")
    K.disc(m, "coin", (0, 0, 0), 4.6, 1.0, MAIN, "metal", "z", 14, 0, 0.3)
    K.band_z(m, "rim", (0, 0, 0), 4.1, 0.7, 1.2, DARK, "metal", 14)
    for s in (-1, 1):
        K.spike(m, "horn%d" % s, (s * 2.2, 4.0, 0), (s * 3.4, 7.6, 0), 0.9, DARK, "horn", 5, 0.4, 0.9)
    K.rod(m, "crack1", [(-2.8, 1.0, -0.6), (-0.8, 0.2, -0.6), (1.4, 1.6, -0.6), (3.0, 0.6, -0.6)], 0.14, GLOW, "ember", 3, EMIT | HOT)
    K.rod(m, "crack2", [(0.6, -3.6, -0.6), (0.2, -1.6, -0.6), (1.2, 0.0, -0.6)], 0.14, GLOW, "ember", 3, EMIT | HOT)
    K.ball(m, "face", (0, -0.4, -0.9), 1.5, DARK, "metal", 3, 6, 0.4)
    return m


@model
def hearts_divide():
    """The Lover's Divide: a heart split in two halves held a little apart, one rose-red and one pale, a thin crack of light between them and a ribbon joining the tops."""
    m = K.new("hearts_divide")
    for s, (slot, tile) in ((-1, (MAIN, "silk")), (1, (TRIM, "pearl"))):
        p = Part("half%d" % s)
        pts_l = [(s * 1.3, -6.0), (s * 1.3 + s * 3.0, -2.6), (s * 1.3 + s * 4.6, 1.8), (s * 1.3 + s * 2.4, 4.8), (s * 1.3 + s * 0.2, 4.0)]
        pts_r = [(s * 1.3, -6.0), (s * 1.3 + s * 0.2, -2.0), (s * 1.3 + s * 0.4, 1.4), (s * 1.3 + s * 0.3, 3.6), (s * 1.3 + s * 0.2, 4.0)]
        L.quad_strip(p, [(x, y, -1.2) for x, y in pts_l], [(x, y, -1.2) for x, y in pts_r], 2.4, True)
        m.add(p, slot, tile, 0, 0, "tile" if tile == "silk" else "fit")
    g = m.group("crack", "pulse", (0, 0, -1.4), 0.15, 0.2, 0.0)
    K.block(m, "light", (-0.15, -5.6, -1.5), (0.15, 4.0, -0.9), GLOW, "spark", 0.0, EMIT | HOT, g, "fit")
    K.rod(m, "ribbon", K.bezier((-2.6, 4.9, -0.2), (-1.0, 6.8, 0.0), (1.0, 6.8, 0.0), (2.6, 4.9, -0.2), 7), 0.25, ACCENT, "silk", 4)
    return m


@model
def elephant_room():
    """The Elephant of the Room: a grey elephant figurine, big and quite impossible to ignore, standing four-square with its trunk raised and its ears out wide."""
    m = K.new("elephant_room")
    K.ball(m, "body", (0, 0, 0), 4.8, MAIN, "leather", 4, 8, 0.8)
    K.ball(m, "head", (0, 2.6, -4.6), 3.0, MAIN, "leather", 4, 8, 1.0)
    for s in (-1, 1):
        K.ball(m, "ear%d" % s, (s * 3.6, 3.0, -4.2), 2.4, MAIN, "leather", 3, 8, 0.9)
        K.cyl(m, "leg1%d" % s, (s * 2.6, -4.6, -2.4), 1.5, 4.8, MAIN, "leather", sides=6)
        K.cyl(m, "leg2%d" % s, (s * 2.6, -4.6, 2.6), 1.5, 4.8, MAIN, "leather", sides=6)
        K.spike(m, "tusk%d" % s, (s * 1.0, 1.2, -6.8), (s * 1.8, -0.8, -9.6), 0.5, NONE, "bone", 5, 0.4, 0.9)
        K.ball(m, "eye%d" % s, (s * 1.7, 3.8, -6.9), 0.35, DARK, "black", 3, 6)
    K.rod(m, "trunk", K.bezier((0, 1.4, -7.2), (0, -3.0, -9.4), (0, -2.0, -11.6), (0, 3.2, -11.2), 8), 1.1, MAIN, "leather", 6,
          scales=[(1.2, 1.2), (1.1, 1.1), (1.0, 1.0), (0.9, 0.9), (0.8, 0.8), (0.7, 0.7), (0.6, 0.6), (0.5, 0.5), (0.45, 0.45)])
    K.rod(m, "tail", [(0, 2.0, 4.6), (0, 0.0, 6.0), (0, -2.4, 5.8)], 0.3, DARK, "leather", 4)
    return m


@model
def mirror_spell():
    """The Mirror of Spell Reflection: a hand mirror with an oval silver glass that shines, a carved gilt frame with four jewels and a slim twisted handle."""
    m = K.new("mirror_spell")
    K.disc(m, "glass", (0, 4.0, 0), 3.7, 0.5, NONE, "white", "z", 16, EMIT, 0.0, uv="fit")
    K.disc(m, "silver", (0, 4.0, -0.4), 3.5, 0.2, GLOW, "soft", "z", 16, EMIT | HOT, 0.0, uv="fit")
    K.ring(m, "frame", (0, 4.0, 0), 4.0, 0.6, TRIM, "gilt", "xy", 18, 5)
    for i, a in enumerate((0.0, math.pi / 2, math.pi, 3 * math.pi / 2)):
        K.octa(m, "jewel%d" % i, (4.7 * math.cos(a), 4.0 + 4.7 * math.sin(a), -0.5), 0.55, 0.55, 0.45, ACCENT, EMIT)
    K.rod(m, "handle", [(0, -0.6, 0), (0.0, -4.5, 0), (0.0, -9.0, 0)], 0.65, MAIN, "gilt", 6, scales=[(0.9, 0.9), (0.8, 0.8), (1.0, 1.0)])
    K.ball(m, "pommel", (0, -9.6, 0), 0.9, TRIM, "gilt", 3, 6)
    return m


@model
def tent_campsite():
    """The Pocket Campsite: a tiny tent with its flap tied open, a small campfire of crossed logs with a flame in front of it, and a pennant on a pole."""
    m = K.new("tent_campsite")
    p = Part("tent")
    lathe(p, "y", (0, -3.0, 0), [(0.0, 5.8), (1.0, 5.6), (4.0, 3.0), (6.4, 0.3), (6.6, 0.0)], sides=4, caps=(True, True))
    m.add(p, MAIN, "cloth")
    K.block(m, "door", (-1.4, -3.0, -4.0), (1.4, 1.6, -3.0), DARK, "black", 0.0)
    K.block(m, "ground", (-9.0, -3.6, -8.0), (9.0, -3.0, 8.0), TRIM, "leather", 0.0)
    for i in range(3):
        a = i * math.pi / 3
        K.rod(m, "log%d" % i, [(5.4 + 2.4 * math.cos(a), -2.2, -5.0 + 2.4 * math.sin(a)), (5.4 - 2.4 * math.cos(a), -2.2, -5.0 - 2.4 * math.sin(a))], 0.6, DARK, "wood", 5)
    g = m.group("fire", "pulse", (5.4, -2.0, -5.0), 0.1, 0.25, 0.0)
    p = Part("flame")
    lathe(p, "y", (5.4, -2.0, -5.0), [(0.0, 0.0), (0.6, 1.4), (1.8, 1.3), (3.2, 0.6), (4.4, 0.0)], sides=6)
    m.add(p, GLOW, "flame", EMIT | HOT, g, "fit")
    K.cyl(m, "pole", (-5.4, 2.0, -3.6), 0.28, 11.0, DARK, "wood", sides=4)
    K.block(m, "pennant", (-5.1, 5.8, -3.9), (-1.8, 7.6, -3.3), ACCENT, "cloth", 0.0)
    return m


@model
def crystal_dragon():
    """The Crystal Dragon: a small dragon figurine cut from clear crystal, wings half open, a long neck and tail curled round its feet, and a glow inside its chest."""
    m = K.new("crystal_dragon")
    K.ball(m, "body", (0, 0, 0), 3.4, MAIN, "crystal", 4, 8, 0.9, 0, uv="fit")
    K.rod(m, "neck", K.bezier((0, 2.0, -2.0), (0, 5.6, -3.0), (0, 7.0, -4.6), (0, 8.8, -4.0), 6), 1.4, MAIN, "crystal", 6, scales=[(1.0, 1.0), (0.9, 0.9), (0.8, 0.8), (0.7, 0.7), (0.6, 0.6)])
    K.ball(m, "head", (0, 9.2, -4.8), 1.4, MAIN, "crystal", 3, 6, 0.9, 0, uv="fit")
    K.spike(m, "snout", (0, 9.0, -5.6), (0, 8.6, -7.6), 0.8, MAIN, "crystal", 5, 0.4, 0.9)
    K.rod(m, "tail", K.bezier((0, -1.0, 3.0), (3.0, -3.4, 7.0), (6.0, -5.0, 0.0), (2.4, -5.4, -4.0), 8), 1.4, MAIN, "crystal", 6, scales=[(1.0, 1.0)] * 5 + [(0.7, 0.7), (0.5, 0.5), (0.3, 0.3), (0.2, 0.2)])
    for s in (-1, 1):
        K.rod(m, "wing%d" % s, K.bezier((s * 2.0, 2.4, 1.6), (s * 7.0, 6.0, 3.0), (s * 9.0, 4.0, 4.2), 6), 0.7, ACCENT, "crystal", 4,
              scales=[(1.0, 2.0), (1.0, 3.0), (0.8, 3.0), (0.6, 2.0), (0.4, 1.0), (0.2, 0.5), (0.1, 0.2)])
        K.cyl(m, "leg%d" % s, (s * 1.8, -3.6, -1.6), 0.95, 3.0, MAIN, "crystal", sides=5, uv="fit")
        K.jewel(m, "eye%d" % s, (s * 0.9, 9.5, -6.0), 0.3, GLOW, 1.2, 4, "x", EMIT)
    K.ball(m, "heart", (0, 0.4, -2.4), 1.3, GLOW, "gem", 3, 6, 1.0, EMIT | HOT, uv="fit")
    return m


@model
def cupcake_madama():
    """Madama Cupcake: a frosted cupcake in a pleated paper case, a swirl of pink icing, sprinkles, a red cherry on top, and a faint sweet shimmer."""
    m = K.new("cupcake_madama")
    p = Part("case")
    lathe(p, "y", (0, -6.0, 0), [(0.0, 2.6), (0.6, 2.9), (3.6, 4.2), (5.0, 4.4)], sides=12)
    m.add(p, TRIM, "stripe")
    p = Part("cake")
    lathe(p, "y", (0, -1.2, 0), [(0.0, 4.4), (1.2, 4.7), (2.0, 4.4)], sides=12)
    m.add(p, DARK, "honey")
    p = Part("icing")
    lathe(p, "y", (0, 0.4, 0), [(0.0, 4.6), (0.8, 4.9), (2.0, 4.0), (3.4, 3.0), (4.6, 2.0), (5.8, 1.0), (6.8, 0.0)], sides=10)
    m.add(p, MAIN, "silk")
    K.ball(m, "cherry", (0, 7.4, 0), 1.0, GLOW, "gem", 3, 8, 1.0, EMIT | HOT, uv="fit")
    for i in range(8):
        a = i * 2.4
        K.cyl(m, "sprinkle%d" % i, (2.6 * math.cos(a) * (0.5 + 0.1 * (i % 3)), 2.2 + (i % 4) * 0.8, 2.6 * math.sin(a) * (0.5 + 0.1 * (i % 3))), 0.14, 0.7, (GLOW, ACCENT, NONE, TRIM)[i % 4], "white", "x" if i % 2 else "z", 4)
    return m


@model
def notebook():
    """An Ancient Notebook: a thick bound notebook with a cloth cover, a leather spine, a band of elastic round it and a coloured ribbon bookmark hanging from the pages, a symbol of its subject on the cover."""
    m = K.new("notebook")
    K.block(m, "cover", (-4.2, -6.0, -1.9), (4.2, 6.0, 1.9), MAIN, "cloth", 0.4)
    K.block(m, "pages", (-3.9, -5.7, -1.5), (4.0, 5.7, 1.5), NONE, "paper", 0.0, uv="fit")
    K.block(m, "spine", (-4.6, -6.1, -2.0), (-3.7, 6.1, 2.0), DARK, "leather", 0.3)
    K.block(m, "elastic", (2.4, -6.2, -2.05), (3.0, 6.2, 2.05), ACCENT, "stripe", 0.0)
    K.octa(m, "mark", (-0.4, 1.4, -2.1), 1.5, 1.8, 0.3, GLOW, EMIT | HOT)
    K.block(m, "label", (-3.0, 3.4, -2.05), (2.0, 4.8, -1.95), NONE, "paper", 0.0, uv="fit")
    K.rod(m, "ribbon", [(0.5, -5.8, 0.4), (0.7, -8.4, 0.5), (0.3, -10.2, 0.4)], 0.3, TRIM, "silk", 3, scales=[(1.0, 1.4)] * 3)
    return m


@model
def tome_ornate():
    """An ornate spellbook: a thick book in a leather binding with gilt corners and a big clasp, a glowing sigil on the cover and a ribbon marker; used for the master's spellbook and the book of concentration."""
    m = K.new("tome_ornate")
    K.block(m, "cover", (-4.6, -6.4, -2.2), (4.6, 6.4, 2.2), MAIN, "leather", 0.6)
    K.block(m, "pages", (-4.3, -6.1, -1.7), (4.4, 6.1, 1.7), NONE, "paper", 0.0, uv="fit")
    K.block(m, "spine", (-5.0, -6.5, -2.3), (-4.0, 6.5, 2.3), DARK, "leather", 0.4)
    for sx in (-1, 1):
        for sy in (-1, 1):
            K.block(m, "corner%d%d" % (sx, sy), (sx * 4.4 - 1.0, sy * 6.2 - 1.0, -2.45), (sx * 4.4 + 1.0, sy * 6.2 + 1.0, -2.0), TRIM, "gilt", 0.0)
    K.block(m, "clasp", (3.4, -1.0, -2.6), (5.0, 1.0, -2.0), TRIM, "gilt", 0.3)
    K.octa(m, "sigil", (-0.2, 0.2, -2.4), 2.2, 2.6, 0.4, GLOW, EMIT | HOT)
    K.ring(m, "sigilring", (-0.2, 0.2, -2.3), 3.3, 0.25, TRIM, "gilt", "xy", 16, 4)
    K.rod(m, "ribbon", [(1.0, -6.0, 0.4), (1.2, -8.8, 0.6), (0.8, -10.8, 0.4)], 0.3, ACCENT, "silk", 3, scales=[(1.0, 1.4)] * 3)
    return m


@model
def tome_illegal():
    """The Tome of Wait, That's Illegal: a slim black book with a red-and-white hazard stripe across the cover, a glowing exclamation mark and a lock hanging off its clasp."""
    m = K.new("tome_illegal")
    K.block(m, "cover", (-4.4, -6.0, -1.8), (4.4, 6.0, 1.8), MAIN, "leather", 0.5)
    K.block(m, "pages", (-4.1, -5.7, -1.4), (4.2, 5.7, 1.4), NONE, "paper", 0.0, uv="fit")
    K.block(m, "spine", (-4.8, -6.1, -1.9), (-3.9, 6.1, 1.9), DARK, "leather", 0.3)
    K.block(m, "stripe", (-4.5, 2.4, -1.95), (4.5, 4.4, -1.85), ACCENT, "stripe", 0.0)
    K.block(m, "bang", (-0.4, -3.4, -2.0), (0.4, 0.8, -1.9), GLOW, "spark", 0.0, EMIT | HOT, uv="fit")
    K.ball(m, "dot", (0, -4.8, -2.0), 0.55, GLOW, "spark", 3, 6, 0.4, EMIT | HOT, uv="fit")
    K.block(m, "lock", (2.8, -3.4, -2.6), (4.4, -1.4, -2.0), TRIM, "gilt", 0.2)
    K.ring(m, "shackle", (3.6, -1.1, -2.3), 0.7, 0.2, TRIM, "gilt", "xy", 8, 4)
    return m


# ================================================================ dials, badges, brooches, the odd charms
@model
def dial_defender():
    """The Defender's Dial: a round shield-like dial with a raised rim, a boss in the centre and a pointer that turns to one of four notches, hung on a belt loop."""
    m = K.new("dial_defender")
    K.disc(m, "face", (0, 0, 0), 4.6, 1.0, MAIN, "plate", "z", 16, 0, 0.3, uv="fit")
    K.ring(m, "rim", (0, 0, -0.5), 4.7, 0.5, TRIM, "gilt", "xy", 18, 4)
    K.ball(m, "boss", (0, 0, -1.0), 1.3, TRIM, "gilt", 3, 8, 0.6)
    for i in range(8):
        a = i * math.pi / 4
        K.block(m, "notch%d" % i, (3.5 * math.cos(a) - 0.22, 3.5 * math.sin(a) - 0.22, -0.9), (3.5 * math.cos(a) + 0.22, 3.5 * math.sin(a) + 0.22 + (0.5 if i % 2 == 0 else 0), -0.5), DARK, "metal", 0.0)
    g = m.group("pointer", "sway", (0, 0, 0), 30.0, 0.05, 0.0)
    K.spike(m, "pointer", (0, 0, -1.2), (0, 3.8, -1.2), 0.7, GLOW, "gem", 4, 0.3, 0.9, EMIT | HOT, g)
    K.ring(m, "loop", (0, 6.0, 0), 1.2, 0.3, TRIM, "leather", "yz", 8, 4)
    return m


@model
def orbit_tiny():
    """The Tiny Orbit: a small bright sphere with a ring tilted round it and a smaller bead running round the ring, each turning at its own pace."""
    m = K.new("orbit_tiny")
    K.ball(m, "sun", (0, 0, 0), 2.4, GLOW, "soft", 5, 8, 1.0, EMIT | HOT, uv="fit")
    g = m.group("ring", "spin", (0, 0, 0), 2.0, 0.0, 0.0)
    K.ring(m, "ring", (0, 0, 0), 5.0, 0.3, TRIM, "gilt", "xz", 18, 4, 0, g)
    g2 = m.group("bead", "orbit", (0, 0, 0), 3.0, 0.0, 0.0)
    K.ball(m, "bead", (5.0, 0, 0), 0.8, ACCENT, "pearl", 3, 6, 1.0, EMIT, g2, "fit")
    g3 = m.group("ring2", "spin", (0, 0, 0), -1.3, 0.0, 0.0)
    K.ring(m, "ring2", (0, 0, 0), 6.4, 0.2, ACCENT, "gilt", "xy", 18, 4, 0, g3)
    return m


@model
def ghost_steps():
    """The Phantom Steps: a pair of faint boots made of a pale mist that stand a little off the ground, a tall shaft and a long toe each, no laces and no soles, wisps trailing from the heels."""
    m = K.new("ghost_steps")
    for s in (-1, 1):
        p = Part("shaft%d" % s)
        lathe(p, "y", (s * 3.0, -2.0, 1.0), [(0.0, 1.9), (1.0, 2.1), (4.0, 1.8), (7.4, 1.9), (8.0, 2.2)], sides=8, squash=0.95)
        m.add(p, GLOW, "soft", EMIT, 0, "fit")
        p = Part("foot%d" % s)
        loft(p, "z", (s * 3.0, -2.6, 2.6), [(0.0, 1.8, 1.5), (2.0, 2.0, 1.6), (4.0, 1.9, 1.3), (6.0, 1.5, 0.9), (7.4, 0.9, 0.6), (8.0, 0.0, 0.0)], circle(1.0, 8))
        K.turn(p, "y", 180, (s * 3.0, 0, 2.6))
        m.add(p, GLOW, "soft", EMIT, 0, "fit")
        g = m.group("wisp%d" % (s + 1), "bob", (s * 3.0, -2.0, 4.0), 0.8, 0.12, s)
        K.rod(m, "wisp%d" % s, K.bezier((s * 3.0, -1.0, 3.2), (s * 3.4, -1.8, 5.4), (s * 2.4, -0.6, 7.6), 6), 0.5, ACCENT, "soft", 5, EMIT, g,
              scales=[(1.0, 1.0), (0.9, 0.9), (0.7, 0.7), (0.5, 0.5), (0.3, 0.3), (0.2, 0.2), (0.1, 0.1)])
    return m


@model
def badge_official():
    """The Official-Looking Badge: a gold shield-shaped badge with two crossed hammers on it, a small star above them, and a pin with a chain."""
    m = K.new("badge_official")
    p = Part("shield")
    L.quad_strip(p, [(-4.6, 4.8, 0), (-4.6, 0.0, 0), (-2.4, -4.0, 0), (0, -6.2, 0)], [(4.6, 4.8, 0), (4.6, 0.0, 0), (2.4, -4.0, 0), (0, -6.2, 0)], 1.0, True)
    m.add(p, MAIN, "gilt")
    for s in (-1, 1):
        K.rod(m, "haft%d" % s, [(s * -3.0, -3.0, -0.8), (s * 3.0, 2.8, -0.8)], 0.38, DARK, "wood", 4)
        K.block(m, "head%d" % s, (s * 3.0 - 1.4, 2.4, -1.4), (s * 3.0 + 1.4, 4.0, -0.2), TRIM, "metal", 0.2)
    K.octa(m, "star", (0, 4.6, -0.9), 1.0, 1.0, 0.4, GLOW, EMIT | HOT)
    K.rod(m, "pin", [(-3.2, 6.2, 0.6), (3.2, 6.2, 0.6)], 0.2, TRIM, "metal", 4)
    return m


@model
def brooch_poeur():
    """The Brooch of Po'eur: a silver leaf-and-feather brooch with a pale blue stone in the middle, a fan of three feathers behind it and a hooked pin."""
    m = K.new("brooch_poeur")
    for i, a in enumerate((-0.5, 0.0, 0.5)):
        K.rod(m, "feather%d" % i, K.bezier((0, 0, 0.2), (math.sin(a) * 3.0, 5.0, 0.2), (math.sin(a) * 4.2, 9.0, 0.2)), 0.8, MAIN, "feather", 5,
              scales=[(0.3, 0.3), (1.0, 0.4), (1.6, 0.4), (1.5, 0.4), (1.0, 0.4), (0.5, 0.3), (0.2, 0.2)])
    K.disc(m, "setting", (0, -0.2, -0.4), 2.6, 0.8, TRIM, "gilt", "z", 12)
    K.jewel(m, "stone", (0, -0.2, -1.0), 1.7, GLOW, 1.0, 6, "z")
    K.rod(m, "pin", [(-3.0, -1.0, 0.8), (3.0, -1.0, 0.8)], 0.2, TRIM, "metal", 4)
    return m


@model
def sigil_flame():
    """The Sigil of the Faint Flame: a dark disc with a thin flame drawn in light across it, barely brighter than an ember, and a ring of tiny runes round the edge."""
    m = K.new("sigil_flame")
    K.disc(m, "disc", (0, 0, 0), 4.4, 1.0, MAIN, "plate", "z", 16, 0, 0.3, uv="fit")
    K.ring(m, "rim", (0, 0, -0.5), 4.5, 0.4, TRIM, "gilt", "xy", 18, 4)
    p = Part("flame")
    lathe(p, "y", (0, -2.6, -0.8), [(0.0, 0.0), (0.7, 1.1), (2.0, 1.2), (3.4, 0.8), (5.2, 0.0)], sides=6, squash=0.3)
    m.add(p, GLOW, "flame", EMIT | HOT, 0, "fit")
    for i in range(12):
        a = i * math.pi / 6
        K.octa(m, "rune%d" % i, (3.7 * math.cos(a), 3.7 * math.sin(a), -0.7), 0.25, 0.25, 0.2, ACCENT, EMIT)
    K.ring(m, "bail", (0, 5.7, 0), 0.9, 0.25, TRIM, "gilt", "yz", 8, 4)
    return m


@model
def clasp_true_heart():
    """The True Heart Clasp: a heart-shaped silver clasp split down the middle by a small hinge, a ruby heart fitted in it, and two short chains that meet at the hinge."""
    m = K.new("clasp_true_heart")
    for s in (-1, 1):
        p = Part("half%d" % s)
        pts_l = [(s * 0.2, -5.6), (s * 3.2, -2.0), (s * 4.8, 1.6), (s * 2.6, 4.4), (s * 0.4, 3.4)]
        pts_r = [(s * 0.2, -5.6), (s * 0.5, -2.0), (s * 0.6, 1.2), (s * 0.5, 3.0), (s * 0.4, 3.4)]
        L.quad_strip(p, [(x, y, 0) for x, y in pts_l], [(x, y, 0) for x, y in pts_r], 1.2, True)
        m.add(p, MAIN, "metal")
    K.octa(m, "ruby_l", (-1.5, 1.4, -0.9), 1.0, 1.2, 0.5, GLOW, EMIT | HOT)
    K.octa(m, "ruby_r", (1.5, 1.4, -0.9), 1.0, 1.2, 0.5, GLOW, EMIT | HOT)
    K.cyl(m, "hinge", (0, 0.4, 0.4), 0.4, 3.0, TRIM, "gilt", sides=6)
    for s in (-1, 1):
        K.rod(m, "chain%d" % s, K.bezier((s * 1.2, 4.2, 0.6), (s * 4.0, 7.4, 0.6), (s * 1.2, 10.0, 0.6)), 0.2, TRIM, "chain", 4)
    return m


@model
def badge_fury():
    """Fury of the Fallen: a dark iron badge in the shape of a cracked shield with two slit eyes cut in it, glowing red, and a spiked rim."""
    m = K.new("badge_fury")
    K.disc(m, "badge", (0, 0, 0), 4.6, 1.2, MAIN, "plate", "z", 7, 0, 0.3, uv="fit")
    for i in range(7):
        a = i * 2 * math.pi / 7 + 0.2
        K.spike(m, "spike%d" % i, (4.5 * math.cos(a), 4.5 * math.sin(a), 0), (6.0 * math.cos(a), 6.0 * math.sin(a), 0), 0.5, DARK, "metal", 4, 0.4, 0.9)
    K.block(m, "eye_l", (-3.6, 0.6, -0.9), (-1.2, 1.4, -0.4), GLOW, "ember", 0.0, EMIT | HOT, uv="fit")
    K.block(m, "eye_r", (1.2, 0.6, -0.9), (3.6, 1.4, -0.4), GLOW, "ember", 0.0, EMIT | HOT, uv="fit")
    K.rod(m, "crack", [(0, 4.0, -0.7), (0.8, 2.0, -0.7), (-0.6, 0.2, -0.7), (0.4, -2.4, -0.7), (0, -4.2, -0.7)], 0.15, GLOW, "ember", 3, EMIT)
    return m


@model
def puppet_lumpy():
    """Lumpy: a lumpy blue felt hand puppet in a pointed wizard hat, a wide mouth with a pink tongue, two button eyes set at different heights, a floppy sleeve underneath and a star on the hat."""
    m = K.new("puppet_lumpy")
    p = Part("sleeve")
    lathe(p, "y", (0, -9.0, 0), [(0.0, 2.6), (1.0, 3.0), (5.0, 2.8), (6.4, 3.2)], sides=9)
    m.add(p, MAIN, "fur")
    K.ball(m, "head", (0, 0.8, 0), 4.0, MAIN, "fur", 4, 8, 0.9)
    K.ball(m, "lump1", (-2.6, 3.4, -1.2), 1.4, MAIN, "fur", 3, 6)
    K.ball(m, "lump2", (2.4, -2.0, -2.4), 1.2, MAIN, "fur", 3, 6)
    K.block(m, "mouth", (-2.4, -2.4, -4.2), (2.4, -0.4, -2.6), DARK, "black", 0.4)
    K.block(m, "tongue", (-1.2, -2.3, -4.3), (1.2, -1.2, -3.4), ACCENT, "soft", 0.3, uv="fit")
    K.ball(m, "eye1", (-1.6, 1.8, -3.8), 0.9, NONE, "white", 3, 6, 1.0, 0, uv="fit")
    K.ball(m, "eye2", (1.6, 2.6, -3.6), 0.7, NONE, "white", 3, 6, 1.0, 0, uv="fit")
    K.ball(m, "pupil1", (-1.5, 1.8, -4.5), 0.35, DARK, "black", 3, 6)
    K.ball(m, "pupil2", (1.5, 2.6, -4.2), 0.3, DARK, "black", 3, 6)
    K.disc(m, "brim", (0, 4.6, 0), 6.0, 0.6, TRIM, "cloth", "y", 12, 0, 0.1)
    p = Part("hat")
    lathe(p, "y", (0, 4.8, 0), [(0.0, 3.2), (1.2, 2.8), (4.0, 1.7), (7.0, 0.8), (9.4, 0.0)], sides=9)
    m.add(p, TRIM, "cloth")
    K.rod(m, "hattip", K.bezier((0, 13.6, 0), (1.4, 14.4, 0), (2.4, 13.4, 0)), 0.4, TRIM, "cloth", 4)
    K.octa(m, "star", (0, 7.8, -2.4), 0.9, 0.9, 0.3, GLOW, EMIT | HOT)
    return m


@model
def pact_cards():
    """The Pact of the Deck: three playing cards fanned out and bound at the bottom by a red wax seal, each with a different glowing pip, a thin contract line running across them."""
    m = K.new("pact_cards")
    for i, a in enumerate((-22.0, 0.0, 22.0)):
        p = Part("card%d" % i)
        ml.box(p, (-3.0, -1.0, -0.3), (3.0, 9.0, 0.3), 0.3)
        K.turn(p, "z", a, (0, -2.0, 0))
        p.V = [v + (0, 0, -i * 0.7) for v in p.V]
        m.add(p, NONE, "paper", 0, 0, "fit")
        p2 = Part("pip%d" % i)
        ml.gem(p2, (0, 4.0, -0.5), 1.2, 1.3, 4, "z")
        K.turn(p2, "z", a, (0, -2.0, 0))
        p2.V = [v + (0, 0, -i * 0.7 - 0.2) for v in p2.V]
        m.add(p2, (GLOW, ACCENT, TRIM)[i], "gem", EMIT | HOT, 0, "fit")
    K.disc(m, "seal", (0, -1.4, -2.4), 1.6, 0.8, MAIN, "wood", "z", 10, 0, 0.1)
    K.octa(m, "sealgem", (0, -1.4, -3.0), 0.8, 0.8, 0.3, GLOW, EMIT | HOT)
    K.rod(m, "ribbon", [(-3.6, -1.4, -1.8), (0, -1.6, -2.0), (3.6, -1.4, -1.8)], 0.3, MAIN, "silk", 3)
    return m


# ================================================================ who holds what
ASSIGN.update({
    "lumpy": ["puppet_lumpy"],
    "pact_of_the_deck": ["pact_cards"],
    "ring_of_endless_rage": ["ring_rage"],
    "ring_of_memories": ["ring_memories"],
    "ring_of_power": ["ring_power"],
    "ring_of_regeneration": ["ring_regeneration"],
    "ring_of_rock_eating": ["ring_rock_eating"],
    "ring_of_the_game": ["ring_game"],
    "ring_of_the_mimic": ["ring_mimic"],
    "the_forgotten_ring": ["ring_forgotten"],
    "witchs_ring": ["ring_witch"],
    "dryads_wedding_band": ["ring_dryad"],
    "eye_of_the_deep_one": ["eye_deep_one"],
    "bag_of_holding": ["bag_holding"],
    "bag_of_devouring": ["bag_devouring"],
    "bag_of_kittens": ["bag_kittens"],
    "bag_of_colding": ["bag_colding"],
    "bag_of_golding": ["bag_golding"],
    "bag_of_molding": ["bag_molding"],
    "bag_of_scolding": ["bag_scolding"],
    "bag_of_unfolding": ["bag_unfolding"],
    "satchel_of_unmet_needs": ["satchel_unmet"],
    "dragon_satchel": ["satchel_dragon"],
    "wallet_of_the_slayer": ["wallet_slayer"],
    "sand_of_the_pocket": ["pouch_sand"],
    "snugglable_doll": ["doll_snugglable"],
    "paralyzing_perfume": ["perfume_bottle"],
    "drow_sunscreen": ["sunscreen_bottle"],
    "jar_of_bats": ["jar_bats"],
    "portable_god": ["idol_god"],
    "strange_idol": ["idol_strange"],
    "simple_blue_runechalk": ["runechalk"],
    "alabaster_white_runechalk": ["runechalk"],
    "weaversong": ["spindle_weaversong"],
    "coin_of_draconic_luck": ["coin_draconic"],
    "coin_of_fortune": ["coin_fortune"],
    "the_devils_due": ["coin_devils_due"],
    "the_lovers_divide": ["hearts_divide"],
    "elephant_of_the_room": ["elephant_room"],
    "mirror_of_spell_reflection": ["mirror_spell"],
    "the_pocket_campsite": ["tent_campsite"],
    "crystal_dragon": ["crystal_dragon"],
    "madama_cupcake": ["cupcake_madama"],
    "arcanists_notebook": ["notebook"],
    "botanists_notebook": ["notebook"],
    "historians_notebook": ["notebook"],
    "physicians_notebook": ["notebook"],
    "theologians_notebook": ["notebook"],
    "old_masters_spellbook": ["tome_ornate"],
    "spellbook_of_concentration": ["tome_ornate"],
    "the_tome_of_wait_thats_illegal": ["tome_illegal"],
    "defenders_dial": ["dial_defender"],
    "tiny_orbit": ["orbit_tiny"],
    "phantom_steps": ["ghost_steps"],
    "official_looking_badge": ["badge_official"],
    "brooch_of_poeur": ["brooch_poeur"],
    "sigil_of_the_faint_flame": ["sigil_flame"],
    "true_heart_clasp": ["clasp_true_heart"],
    "fury_of_the_fallen": ["badge_fury"],
})
