"""Saiyan battle armour (3.69.0): nine GeoLite pieces, authored like the Flame armour (tools/gen_flame_aura_geo.py), in whole-body coordinates (see tools/water_aura_lib.py):

    python3 -B tools/gen_saiyan_armor_geo.py        # writes geo/entity/saiyan_arm_*.geo.json, animations, textures + _glow maps
    python3 -B tools/geo_check.py saiyan_arm_chest ...

Look: a dark blue spandex undersuit (thin boxes just over the body), a white breastplate with a navy lower lame, big stepped shoulder pads with a gold rim and ridge, white gloves and
boots with gold bands, gold waist guards with a tail-slot notch, a gold stepped-diamond crest, and a left-eye scouter. The glow maps are NEUTRAL (near white, bright): the painter tints them
with the form's colour. Only the seams and vents glow (the plates have no speckle). No hair, no cape.

Pieces and anchors (the painter wires them): chest BODY, skirt BODY, pauldron_r ARM_R, pauldron_l ARM_L, vambrace_r ARM_R, vambrace_l ARM_L, greave_r LEG_R, greave_l LEG_L, scouter HEAD.
Idle clips: skirt (tassels sway), scouter (the lens scans). Glow: chest, skirt (buckle), pauldrons (seam), vambraces (seam + vent), greaves (seam + heel vent), scouter (lens, tip).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, sine_rot  # noqa: E402

KEY = "saiyan"


def _vent(self, u, v, w, h, edge, rng, face):
    """A fully lit panel: bright core, slightly dimmer rim (kw cells=1 adds a faint scan grid)."""
    lum = 0.78 + 0.2 * (1 - np.abs(v - 0.5) * 2)
    gl = np.where(edge < 0.6, 0.55, 1.0)
    if self.kw.get("cells"):
        gl = gl * np.where(((np.floor(u * w) + np.floor(v * h)) % 2) == 0, 1.0, 0.8)
    return lum, gl, np.ones_like(u)


Mat._vent = _vent

GLOW = "EEFBFF"
WHITE = Mat("plate", "E9ECF2", dark="8C94AA", light="FFFFFF", seed=1)
WHITE_S = Mat("plate", "E9ECF2", dark="8C94AA", light="FFFFFF", glow=GLOW, glow_amount=0.0, inlay=1, seed=2)     # inlay = a glowing cross seam, no speckle
GOLD = Mat("metal", "E2B536", dark="7A5208", light="FFF2B0", seed=3)
GOLD_S = Mat("metal", "E2B536", dark="7A5208", light="FFF2B0", glow=GLOW, glow_amount=0.8, seam=1, seed=4)       # a gold plate with a lit seam
NAVY = Mat("plate", "1F2F72", dark="0A1238", light="4A66CC", glow=GLOW, glow_amount=0.0, inlay=1, seed=5)        # the deep-blue lame, seam lit
SUIT = Mat("cloth", "20253C", dark="0A0C18", light="48507A", seed=6, grain=0.05)                              # the black-blue spandex
STEEL = Mat("metal", "7C8496", dark="353A48", light="D5DBEA", seed=7)
VENT = Mat("vent", "BFEFFF", dark="7ACBEA", light="FFFFFF", glow=GLOW, glow_amount=1.0, seed=8, rim=0.3, grain=0.02)
LENS = VENT.with_(kw={"cells": 1})


def chest():
    p = Piece(KEY, "arm_chest", density=3)
    p.box("root", (-4.25, 11.75, -2.25), (8.5, 12.5, 4.5), SUIT)                              # the undersuit over the torso
    for s in (0, 1):                                                                         # the two pectoral plates, a cleft between
        mbox(p, "root", s, (-4.65, 18.0, -3.0), (4.5, 4.4, 1.1), WHITE_S)
    p.box("root", (-4.4, 15.2, -2.9), (8.8, 2.8, 0.9), WHITE)                                # the belly plate
    p.box("root", (-4.3, 12.2, -2.8), (8.6, 2.9, 0.8), NAVY)                                  # the navy lower lame
    p.box("root", (-4.55, 14.7, -3.05), (9.1, 0.6, 1.0), GOLD)                                # gold line between plate and lame
    p.box("root", (-4.85, 22.2, -3.2), (9.7, 0.8, 1.4), GOLD)                                 # gold rim along the top of the chest
    p.box("root", (-0.2, 18.2, -2.95), (0.4, 4.0, 0.35), VENT)                                # the lit cleft
    p.box("root", (-0.6, 19.9, -3.55), (1.2, 0.8, 0.5), GOLD_S)                               # the crest: a stepped diamond, three rows
    p.box("root", (-1.2, 19.1, -3.55), (2.4, 0.8, 0.5), GOLD_S)
    p.box("root", (-0.6, 18.3, -3.55), (1.2, 0.8, 0.5), GOLD_S)
    for s in (0, 1):                                                                         # side guards
        mbox(p, "root", s, (-5.0, 13.4, -1.9), (0.8, 7.4, 3.8), WHITE)
    p.box("root", (-4.5, 13.0, 2.2), (9.0, 10.0, 0.9), WHITE)                                 # back plate
    p.box("root", (-4.2, 12.2, 2.35), (8.4, 2.0, 0.9), NAVY)                                  # back lame
    p.box("root", (-0.6, 14.6, 3.05), (1.2, 8.4, 0.35), GOLD)                                 # gold spine band
    p.box("root", (-0.2, 15.2, 3.35), (0.4, 7.0, 0.2), VENT)                                  # the lit spine
    p.box("root", (-3.6, 22.4, -2.7), (7.2, 1.2, 5.4), WHITE)                                 # the gorget
    p.box("root", (-3.75, 23.6, -2.85), (7.5, 0.5, 5.7), GOLD)                                # its gold lip
    p.save()
    return p


def skirt():
    p = Piece(KEY, "arm_skirt", density=2)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    p.box("root", (-4.5, 10.9, -2.5), (9.0, 1.6, 5.0), NAVY)                                  # the belt
    p.box("root", (-1.1, 10.8, -2.95), (2.2, 1.8, 0.7), GOLD_S)                               # the buckle
    p.box("root", (-0.45, 11.2, -3.2), (0.9, 1.0, 0.3), VENT)                                 # the lit stud on it
    for i, sx in enumerate((-1, 1)):                                                         # front guards, hung from the belt
        n = "f%d" % i
        cx = sx * 2.3
        p.bone(n, (cx, 10.9, -3.0))
        p.box(n, (cx - 1.9, 7.4, -3.2), (3.8, 3.6, 0.9), GOLD)
        p.box(n, (cx - 1.9, 6.9, -3.25), (3.8, 0.7, 1.0), WHITE)
        sine_rot(a, "idle", n, 2.0, 0, 5.0, phase=i * 0.15)
    for i, sx in enumerate((-1, 1)):                                                         # back guards: two halves, the tail-slot notch between them
        n = "b%d" % i
        cx = sx * 2.9
        p.bone(n, (cx, 10.9, 2.9))
        p.box(n, (cx - 1.9, 7.6, 2.5), (3.8, 3.4, 0.9), GOLD)
        p.box(n, (cx - 1.9, 7.1, 2.55), (3.8, 0.6, 1.0), WHITE)
        sine_rot(a, "idle", n, 2.0, 0, -5.0, phase=0.4 + i * 0.15)
    for sx in (-1, 1):                                                                       # posts that frame the notch
        p.box("root", (sx * 0.8 - 0.2, 7.8, 2.5), (0.4, 3.2, 1.0), STEEL)
    for s in (0, 1):                                                                         # hip pads
        mbox(p, "root", s, (-5.0, 8.2, -1.8), (0.9, 3.0, 3.6), WHITE)
    p.save(a)
    return p


def pauldron(side):
    n = "pauldron_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.25, 17.5, -2.25), (4.5, 6.75, 4.5), SUIT)                     # the sleeve over the upper arm
    mbox(p, "root", side, (-9.9, 20.7, -3.2), (5.2, 1.0, 6.4), NAVY)                         # the navy underside
    mbox(p, "root", side, (-10.2, 21.7, -3.9), (6.0, 1.1, 7.8), WHITE)                       # four steps of the pad, widest first
    mbox(p, "root", side, (-9.8, 22.8, -3.5), (5.2, 1.1, 7.0), WHITE)
    mbox(p, "root", side, (-9.2, 23.9, -3.0), (4.0, 1.0, 6.0), WHITE_S)
    mbox(p, "root", side, (-8.4, 24.9, -2.5), (2.4, 0.8, 5.0), WHITE)
    mbox(p, "root", side, (-10.45, 21.7, -3.9), (0.5, 1.1, 7.8), GOLD)                       # gold rim: outer edge ...
    mbox(p, "root", side, (-10.2, 21.7, -4.15), (6.0, 1.1, 0.5), GOLD)                       # ... and front edge
    mbox(p, "root", side, (-7.6, 25.7, -2.4), (0.8, 0.6, 4.8), GOLD_S)                        # the raised ridge, seam lit
    p.save()
    return p


def vambrace(side):
    n = "vambrace_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.25, 12.0, -2.25), (4.5, 6.4, 4.5), SUIT)                      # the sleeve over forearm and hand
    mbox(p, "root", side, (-8.5, 11.6, -2.5), (5.0, 3.4, 5.0), WHITE)                        # the glove
    mbox(p, "root", side, (-8.65, 15.0, -2.65), (5.3, 0.9, 5.3), GOLD)                       # first gold band at the wrist
    mbox(p, "root", side, (-8.55, 15.9, -2.55), (5.1, 2.0, 5.1), WHITE_S)                    # the forearm guard
    mbox(p, "root", side, (-8.7, 17.9, -2.7), (5.4, 0.6, 5.4), GOLD)                         # second gold band
    mbox(p, "root", side, (-8.85, 16.3, -0.8), (0.3, 1.1, 1.6), VENT)                        # the lit vent on the outer side
    p.save()
    return p


def greave(side):
    n = "greave_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.2, 1.6, -2.2), (4.4, 10.4, 4.4), SUIT)                        # the leg of the undersuit
    mbox(p, "root", side, (-4.55, 7.9, -2.55), (5.1, 2.8, 5.1), NAVY)                        # the thigh lame
    mbox(p, "root", side, (-4.65, 10.7, -2.65), (5.3, 0.5, 5.3), GOLD)                       # its gold trim
    mbox(p, "root", side, (-4.45, 5.4, -2.95), (4.9, 3.4, 0.9), WHITE_S)                     # the shin plate
    mbox(p, "root", side, (-4.7, 4.1, -2.7), (5.4, 1.0, 5.4), GOLD)                          # the gold band at the boot top
    mbox(p, "root", side, (-4.55, 0.0, -2.6), (5.1, 4.1, 5.2), WHITE)                        # the boot
    mbox(p, "root", side, (-4.4, 0.0, -3.9), (4.8, 2.0, 1.3), WHITE)                         # the toe cap
    mbox(p, "root", side, (-3.2, 1.2, 2.55), (2.0, 0.5, 0.2), VENT)                          # the lit heel vent
    p.save()
    return p


def scouter():
    p = Piece(KEY, "arm_scouter", density=3)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    p.box("root", (4.6, 25.4, -1.2), (1.3, 3.2, 2.6), STEEL)                                 # the ear piece (the model's left, +x)
    p.box("root", (4.6, 27.2, -4.6), (0.9, 0.8, 3.5), STEEL)                                 # the boom along the cheek
    p.box("root", (3.6, 25.6, -5.7), (1.0, 3.2, 1.2), STEEL)                                 # the frame round the left eye, outer bar
    p.box("root", (0.5, 25.6, -5.7), (0.8, 3.2, 1.2), STEEL)                                 # inner bar
    p.box("root", (0.5, 28.8, -5.7), (4.1, 0.6, 1.2), STEEL)                                 # top bar
    p.box("root", (0.5, 25.0, -5.7), (4.1, 0.6, 1.2), STEEL)                                 # bottom bar
    p.bone("lens", (2.45, 27.0, -5.3))
    p.box("lens", (1.3, 26.0, -5.5), (2.3, 2.0, 0.5), LENS)                                  # the glowing lens
    sine_rot(a, "idle", "lens", 2.4, 1, 3.0)
    p.box("root", (5.1, 28.2, -0.8), (0.5, 2.2, 0.5), STEEL)                                 # antenna
    p.box("root", (5.0, 30.4, -0.9), (0.7, 0.7, 0.7), VENT)                                  # its lit tip
    p.save(a)
    return p


def main():
    made = [chest(), skirt()]
    for s in (0, 1):
        made += [pauldron(s), vambrace(s), greave(s)]
    made += [scouter()]
    total = 0
    for m in made:
        print("%-28s cubes %3d" % (m.id, m.cube_count()))
        total += m.cube_count()
    print("total cubes", total)


if __name__ == "__main__":
    main()
