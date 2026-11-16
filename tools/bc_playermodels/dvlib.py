"""Devil player models (docs/devil_looks.md): the library on top of the Black Clover builder (bclib / bcparts / bcparts2 / bcparts3, docs/bc_playermodels.md).

What is added here and nothing else (the BC modules are used as they are, rule 2):
  * DvBuild: a Build with five layers instead of three, so a devil's HAIR can be told from its GEAR:
        main       pmx_dv_<id>            always drawn: horns, wings, tail, pauldrons, collar, skirt ...
        fine       pmx_dv_<id>_fine       only within the detail distance: tassels, mantle fur, skirt fringe
        fx         pmx_dv_<id>_fx         the glowing parts (gems, halo), the only model with the glow flag
        hair       pmx_dv_<id>_hair       the cube hair mass (shell, tiers, spikes); not drawn when the look is worn on a player (the player keeps their own hair)
        hair_fine  pmx_dv_<id>_hair_fine  the strands (fringe, long hair, side locks); also detail-distance only, also not worn on a player
  * hair(): the haircuts, thinner than the bosses' (the shell is 8.9 pixels wide, 1.11 x the 8 pixel skull, so the head stays in proportion), always into the hair layers.
  * a few devil-only parts: skull (a second head), mushroom, ears_up (fox ears), flame_hair, ponytail_side, chest_eye.
Coordinates are bclib's (pixels, y up, the front on -z, 24 = the neck line).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import numpy as np  # noqa: E402

import bclib as L  # noqa: E402
import bcparts as P  # noqa: E402
import bcparts2 as Q  # noqa: E402
import bcparts3 as R3  # noqa: E402
from bclib import Anim, Geo, hsh, rgb, tone  # noqa: E402

LAYERS = (("main", ""), ("fine", "_fine"), ("fx", "_fx"), ("hair", "_hair"), ("hair_fine", "_hair_fine"))

HG, HT = 0.05, 0.4          # the hair shell's gap off the skull and its thickness: 8 + 2 * (0.05 + 0.4) = 8.9 pixels
HEAD = "head"


class DvBuild(P.Build):
    def __init__(self, name, atlas_size=256):
        super().__init__(name, atlas_size)
        for ly, suf in LAYERS:
            if ly not in self.layers:
                self.layers[ly] = Geo("pmx_dv_" + name + suf, self.atlas)
                self.anims[ly] = Anim()
            self.layers[ly].name = "pmx_dv_" + name + suf


# ================================================================================================ hair
def shell(b, c, seed=1, front_low=30.9, side_low=28.8, back_low=25.6, tiers=2, tier_w=8.5):
    """The cube hair on a thin shell (HG / HT) and `tiers` stacked slabs on top."""
    P.hair_shell(b, c, g=HG, thick=HT, seed=seed, layer="hair", front_low=front_low, side_low=side_low, back_low=back_low, side_front=-3.0)
    P.hair_tiers(b, c, n=tiers, y0=32.35, w0=tier_w, shrink=1.3, h=1.15, seed=seed + 1, layer="hair")


def hair(b, style, c, seed=1, tail=8.0, spikes=5, length=5.0, lean=14.0, fan=28.0, tip=None, part=0.0, front=3.2, streak=None, side=10.0, tiers=2):
    """A haircut: 'spiky', 'messy', 'swept', 'long', 'bob', 'short', 'wavy', 'flame' or 'bald'."""
    if style == "bald":
        return
    low = {"long": 23.4, "wavy": 23.0, "bob": 26.0}.get(style, 25.6)
    shell(b, c, seed, back_low=low, tiers=1 if style in ("bob", "short") else tiers)
    if style in ("spiky", "messy", "flame"):
        P.hair_spikes(b, c, n=spikes + (2 if style == "messy" else 0), y=33.2, spread=3.0, length=length * (1.5 if style == "flame" else 1.0), w=3.0 if style != "flame" else 2.4,
                      seed=seed + 3, layer="hair", rows=2, back_lean=lean, fan=fan, tip=tip, depth_step=3.0)
    if True:
        P.fringe(b, c, n=8, span=8.2, y_top=31.6, length=front, seed=seed + 4, layer="hair_fine", rot=12.0, part=part)
    if style in ("long", "wavy"):
        P.long_hair(b, c, y_top=31.4, y_split=19.0, tail=tail, tiers=2, count=5, width=8.6, seed=seed + 6, layer="hair_fine", sway=2.6)
        P.side_locks(b, c, y_top=29.4, length=side, count=2, seed=seed + 7, layer="hair_fine")
    elif style == "swept":
        P.side_locks(b, c, y_top=29.6, length=side * 0.6, count=1, seed=seed + 7, layer="hair_fine")
    elif style == "bob":
        P.bob_hair(b, c, y_top=31.4, y_low=24.6, seed=seed + 8, layer="hair_fine", count=5)
    if streak:                                                                   # a few strands of another colour in the fringe
        P.fringe(b, streak, n=3, span=3.4, y_top=31.7, length=front + 1.2, seed=seed + 20, layer="hair_fine", rot=8.0, part=-2.4)


def ponytail_side(b, c, seed=3, x=4.2, y=30.6, length=13.0, layer="hair_fine"):
    """A thick ponytail on the model's left (a head bone with two cards that swings)."""
    geo = b.g(layer)
    tile = b.t_strand(c, tw=12, th=48, seed=seed, prongs=3, tip=0.25)
    bone = b.sway(layer, HEAD, "pony_l", (x, y, 0.4), ax=3.0, az=4.0, walk=1.5, run=2.5)
    geo.card(bone, x + 0.6, y, 0.4, 3.0, length, tile, rot=(0.0, 0.0, -6.0), pivot=(x, y, 0.4))
    geo.cube(bone, (x + 0.6, y - length, -1.0), (0.0, length, 3.0), tile, faces=("east",), rot=(0.0, 0.0, -6.0), pivot=(x, y, 0.4))
    geo.cube(HEAD, (x - 0.9, y - 0.6, -0.8), (1.8, 1.8, 1.8), b.t_mass(c, seed=seed), faces=("north", "south", "east", "west", "up"))   # the hair tie's knot


def flame_hair(b, c1, c2, seed=5, n=7, length=7.5):
    """Hair as flames: tall crossed spike cards, the tips in the second colour, a flame ponytail down the back."""
    P.hair_spikes(b, c1, n=n, y=33.1, spread=3.4, length=length, w=3.0, seed=seed, layer="hair", rows=2, back_lean=20.0, fan=36.0, tip=c2, depth_step=3.0)
    geo = b.g("hair_fine")
    for i in range(3):
        sp = b.t_spike(c1 if i % 2 == 0 else c2, tw=16, th=32, seed=seed + 40 + i, tip=c2)
        bone = b.sway("hair_fine", "body", "flame_%d" % i, (0.0, 24.0, 4.5 + 0.4 * i), ax=3.0 + i, az=4.0, walk=1.5, run=2.5, phase=1.1 * i)
        geo.card(bone, (i - 1) * 2.2, 26.0, 4.6 + 0.4 * i, 3.4, 12.0 + 2 * i, sp, rot=(0.0, 0.0, (i - 1) * 8.0), pivot=((i - 1) * 2.2, 26.0, 4.6))


# ================================================================================================ devil parts
def ears_up(b, color="#d07020", tip="#fff0d8", x=2.4, y=32.0, length=6.5, w=3.4, lean=10.0, layer="main", seed=7):
    """Tall pointed ears on top of the head (fox, cat): two crossed cards per side, leaning outward."""
    geo = b.g(layer)
    sp = b.t_spike(color, tw=16, th=28, seed=seed, tip=tip)
    for s in (-1, 1):
        piv = (s * x, y, 0.0)
        rot = (-6.0, 0.0, -s * lean)
        geo.cube(HEAD, (s * x - w / 2, y, 0.0), (w, length, 0.0), sp, faces=("north",), rot=rot, pivot=piv)
        geo.cube(HEAD, (s * x, y, -w / 2), (0.0, length, w), sp, faces=("east",), rot=rot, pivot=piv)


def _skull_tile(b, bone, dark, seed=0):
    def paint():
        tw = th = 16
        a = np.zeros((th, tw, 4), np.float32)
        a[..., 3] = 1.0
        a[..., :3] = L._grad(th, [(0.0, tone(bone, 1.15)), (1.0, tone(bone, 0.75))])[:, None, :]
        for cx in (4.5, 11.5):                                   # eye sockets
            for y in range(4, 9):
                for x in range(16):
                    if ((x + 0.5 - cx) / 2.3) ** 2 + ((y + 0.5 - 6.2) / 2.6) ** 2 < 1.0:
                        a[y, x, :3] = rgb(dark)
        for y in range(9, 12):                                   # the nose hole
            for x in range(7, 9):
                a[y, x, :3] = rgb(dark)
        for x in range(2, 14):                                   # teeth
            a[13, x, :3] = rgb(dark)
            if x % 2 == 0:
                a[14, x, :3] = rgb(dark)
                a[15, x, :3] = tone(bone, 0.6)
        g = np.zeros((th, tw, 4), np.float32)
        for cx in (4.5, 11.5):
            g[5:8, int(cx - 1):int(cx + 1), :3] = rgb("#c070ff")
            g[5:8, int(cx - 1):int(cx + 1), 3] = 0.9
        return a, g
    return b.atlas.tile(("skull", L.hexs(rgb(bone)), L.hexs(rgb(dark)), seed), 16, 16, paint)


def skull(b, bone="#e6e0d0", dark="#1a1018", anchor="body", pos=(-6.5, 24.0, 0.0), size=4.6, layer="main", lean=14.0):
    """A second, small skull (a cube with a skull face on the front) sitting on a shoulder, with a horn pair; used by the Curse Devil."""
    geo = b.g(layer)
    face = _skull_tile(b, bone, dark)
    plain = b.t_horn(bone, tip="#fff8e8", tw=8, th=16, seed=31, ridges=3)
    x, y, z = pos
    piv = (x, y, z)
    geo.cube(anchor, (x - size / 2, y, z - size / 2), (size, size, size), {"north": (face.u, face.v, face.w, face.h), "south": (plain.u, plain.v, plain.w, plain.h),
                                                                            "east": (plain.u, plain.v, plain.w, plain.h), "west": (plain.u, plain.v, plain.w, plain.h),
                                                                            "up": (plain.u, plain.v, plain.w, 4), "down": (plain.u, plain.v, plain.w, 4)}, rot=(0.0, 0.0, -lean * (1 if x > 0 else -1)), pivot=piv)
    sp = b.t_spike(bone, tip="#fff8e8", seed=33)
    for s in (-1, 1):
        geo.cube(anchor, (x + s * size * 0.35 - 0.7, y + size - 0.2, z - 0.7), (1.4, 3.0, 0.0), sp, faces=("north",), rot=(0.0, 0.0, -s * 22.0), pivot=(x + s * size * 0.35, y + size - 0.2, z))


def mushroom(b, anchor="body", x=0.0, y=24.0, z=0.0, r=2.4, cap="#7a2a6a", stem="#d8d0b0", spots="#e8d8a0", layer="main", eyes=False):
    """A demonic mushroom: a stem box and a flatter, wider cap (the cap is the cloth tile, so it gets a lit top and a dark rim)."""
    geo = b.g(layer)
    geo.cube(anchor, (x - r * 0.28, y, z - r * 0.28), (r * 0.56, r * 0.9, r * 0.56), b.t_flat(stem, 8, 8, 0.2))
    geo.cube(anchor, (x - r, y + r * 0.9, z - r), (2 * r, r * 0.65, 2 * r), b.t_cloth(cap, 16, 16, seed=int(abs(x) * 7 + abs(z) * 3) % 9, folds=2, trim=spots, trim_h=2))
    geo.cube(anchor, (x - r * 0.6, y + r * 1.55, z - r * 0.6), (r * 1.2, r * 0.4, r * 1.2), b.t_cloth(cap, 16, 16, seed=3, folds=1))
    if eyes:
        b.g("fx").cube(anchor, (x - r * 0.35, y + r * 1.05, z - r - 0.05), (r * 0.7, r * 0.3, 0.3), b.t_gem("#ff3030"), faces=("north",))


def chest_eye(b, color="#3ad8d8", y=20.5, size=3.0):
    """An eye set in the chest: a dark socket plate and a glowing gem."""
    geo = b.g("main")
    geo.cube("body", (-size * 0.8, y - size * 0.5, -2.62), (size * 1.6, size * 1.3, 0.4), b.t_metal("#2a2a30", 16, 8, band=False, rivets=False))
    b.g("fx").cube("body", (-size * 0.5, y - size * 0.25, -2.9), (size, size * 0.8, 0.5), b.t_gem(color), faces=("north", "up"))


def vines(b, color="#2a8a5a", tip="#a8f0c0", n=3, length=11.0, layer="fine", seed=3):
    """Streamers hanging from each forearm (the Future Devil's vines): thin cards that sway."""
    geo = b.g(layer)
    for sd, s in (("r", -1), ("l", 1)):
        for k in range(n):
            x = s * (5.0 + 0.9 * k)
            bone = b.sway(layer, "arm_" + sd, "vine_%s%d" % (sd, k), (x, 20.0, 0.0), ax=6.0, az=5.0, walk=2.5, run=3.5, phase=1.2 * k)
            geo.card(bone, x, 20.0, -1.0 + 1.0 * k, 1.2, length * (1.0 - 0.15 * k), b.t_strand(color, tw=8, th=40, seed=seed + k, prongs=2, tip=0.25), pivot=(x, 20.0, 0.0))


def horn_set(b, color, tip, kind="curved", layer="main", s=1.0):
    """A few horn presets (all mirrored pairs, about 9 pixels long at most: a head is 8): 'curved' (backswept), 'ram' (curls forward and down), 'bull' (out then up), 'ox' (wide, up), 'small' (two nubs),
    'four' (two pairs), 'brow' (short, forward)."""
    if kind == "curved":
        P.horns(b, color, tip, base=(3.1, 31.7, -0.8), segs=4, length=2.2 * s, w0=2.2 * s, w1=0.7, curl=10.0, out=40.0, layer=layer)
    elif kind == "ram":
        P.horns(b, color, tip, base=(3.5, 31.2, -0.8), segs=5, length=2.0 * s, w0=2.4 * s, w1=0.8, curl=34.0, out=72.0, layer=layer, seed=11)
    elif kind == "bull":
        P.horns(b, color, tip, base=(3.2, 31.0, -1.0), segs=4, length=2.0 * s, w0=2.0 * s, w1=0.7, curl=-16.0, out=78.0, layer=layer, seed=12)
    elif kind == "ox":
        P.horns(b, color, tip, base=(3.4, 31.2, -0.6), segs=5, length=2.3 * s, w0=2.4 * s, w1=0.8, curl=-10.0, out=80.0, layer=layer, seed=13)
    elif kind == "small":
        P.horns(b, color, tip, base=(2.2, 32.2, -2.0), segs=3, length=1.7 * s, w0=1.7 * s, w1=0.6, curl=5.0, out=20.0, layer=layer, seed=14)
    elif kind == "four":
        P.horns(b, color, tip, base=(3.1, 31.8, -0.6), segs=4, length=2.3 * s, w0=2.2 * s, w1=0.7, curl=14.0, out=36.0, layer=layer)
        P.horns(b, color, tip, base=(4.1, 29.4, -0.6), segs=4, length=1.8 * s, w0=1.8 * s, w1=0.6, curl=13.0, out=62.0, layer=layer, seed=15)
    elif kind == "brow":
        P.horns(b, color, tip, base=(2.8, 32.0, -3.0), segs=3, length=2.0 * s, w0=1.8 * s, w1=0.6, curl=-20.0, out=30.0, fwd=3.0, layer=layer, seed=16)
