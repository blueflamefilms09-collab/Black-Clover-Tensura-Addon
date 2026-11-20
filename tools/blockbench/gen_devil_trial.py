#!/usr/bin/env python3
"""The Trial Devil model (docs/devil_trial_model.md): the devil that steps out of the ritual circle and fights its caller (devil/phase2/DevilTrialEntity).

    python3 -B tools/blockbench/gen_devil_trial.py [--preview]

WHAT THIS IS (rule 15, said plainly): Blockbench could not run in the session, so this is the FALLBACK. The model is written as Blockbench's own source format
(tools/blockbench/devil_trial.bbmodel: bones as groups, cubes with per-face UV, the painted sheet embedded, the three clips as keyframes) and exported from the same
data to the game files (geo / animation / textures). The .bbmodel was NOT opened in Blockbench: its bone and cube rotations use the sign the Bedrock codec writes
(x and y negated), written from memory, so open it once and look before editing it by hand. Everything is deterministic: re-running writes the same bytes.

THE MODEL: a horned brute (a head with brow ridge, jaw and fangs, a broad chest, pauldrons, bracers, clawed hands, hooved legs, a loincloth, a spade tail, bat wings, two
hair locks and a mane), about 100 cubes, bones root > body > chest > head / arms / wings, tail, legs. One skeleton, ONE painted sheet PER DEVIL (26, in each devil's own
palette from tools/bc_playermodels/dvtable.py: skin, hair, accent) plus a default, each with a glow mask (eyes, brand lines, chest and belt gems, horn tips).
Clips: idle (breath, wing beat, tail and hair sway), walk, attack (the mob swing). GeckoLib plays them; the renderer is client/devil/DevilTrialGeoRenderer.
"""
import base64
import colorsys
import io
import json
import math
import os
import sys
import uuid

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "bc_playermodels"))

import numpy as np  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
GEO_OUT = os.path.join(ASSETS, "geo", "entity", "devil_trial.geo.json")
ANIM_OUT = os.path.join(ASSETS, "animations", "entity", "devil_trial.animation.json")
TEX_DIR = os.path.join(ASSETS, "textures", "entity")
BB_OUT = os.path.join(HERE, "devil_trial.bbmodel")
SHEET = 256


# ---------------------------------------------------------------- colours
def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lum(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def tones(c, lo=0.62, hi=1.18):
    """(shadow, mid, light) of one colour."""
    return (tuple(int(max(0, min(255, v * lo))) for v in c), tuple(c), tuple(int(max(0, min(255, v * hi + 6))) for v in c))


def palette(dev):
    """The devil's colours from the table the player-model looks use (skin tones, hair, accent): the same devil reads the same in both looks."""
    o = dev["outfit"]
    sk = [hexrgb(x) for x in o.get("skin", ("#8a5a3a", "#aa7248", "#c8906a"))]
    hair = hexrgb(dev["hair"]) if dev.get("hair") else mix(sk[0], (0, 0, 0), 0.5)
    acc = hexrgb(o["accent"]) if o.get("accent") else mix(hair, (255, 255, 255), 0.25)
    h, s, v = colorsys.rgb_to_hsv(*(x / 255.0 for x in acc))
    glow = hexrgb("#ff3a40") if s < 0.3 else tuple(int(x * 255) for x in colorsys.hsv_to_rgb(h, min(1.0, s * 0.85 + 0.1), 1.0))
    dark_skin = lum(sk[1]) < 70
    horn = mix(acc, (230, 226, 220), 0.55) if dark_skin else mix(sk[1], (30, 24, 28), 0.7)
    if lum(hair) < 60 and not dark_skin:
        horn = mix(hair, (200, 190, 180), 0.35)
    return dict(skin=tuple(sk), hair=tones(hair), accent=tones(acc), horn=tones(horn, 0.6, 1.25), hoof=tones(mix(sk[0], (10, 8, 10), 0.75), 0.7, 1.3),
                cloth=tones(mix(mix(hair, acc, 0.35), (14, 12, 18), 0.55)), membrane=tones(mix(mix(sk[1], acc, 0.45), (20, 16, 24), 0.4)),
                tooth=tones((236, 230, 214), 0.75, 1.05), glow=glow, glow_dark=mix(glow, (0, 0, 0), 0.6))


# ---------------------------------------------------------------- the model description
BONES = []   # dict(name, parent, pivot, rotation)
CUBES = []   # dict(bone, name, origin, size, mat, rotation, pivot, paint)


def bone(name, parent, pivot, rot=(0, 0, 0), side=-1):
    px, py, pz = pivot
    rx, ry, rz = rot
    if side > 0:
        px, ry, rz = -px, -ry, -rz
    BONES.append(dict(name=name, parent=parent, pivot=[float(px), float(py), float(pz)], rotation=[float(rx), float(ry), float(rz)]))


def cube(b, name, o, s, mat, rot=None, pv=None, side=-1, paint=None):
    """Right-side coordinates are written with x negative (the entity's right is -x); side=+1 mirrors them for the left."""
    ox = o[0] if side < 0 else -(o[0] + s[0])
    r = p = None
    if rot and any(rot):
        rx, ry, rz = rot
        if side > 0:
            ry, rz = -ry, -rz
        r = [float(rx), float(ry), float(rz)]
        pv = pv if pv is not None else (o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2)
        p = [float(-pv[0] if side > 0 else pv[0]), float(pv[1]), float(pv[2])]
    CUBES.append(dict(bone=b, name=name, origin=[float(ox), float(o[1]), float(o[2])], size=[float(x) for x in s], mat=mat, rotation=r, pivot=p, paint=paint))


def pair(fn):
    fn(-1, "r")
    fn(1, "l")


def build():
    del BONES[:]
    del CUBES[:]
    bone("root", None, (0, 0, 0))
    bone("body", "root", (0, 13, 0))
    bone("chest", "body", (0, 18, 0))
    bone("head", "chest", (0, 24.5, 0))
    bone("jaw", "head", (0, 26.2, -1.6))
    bone("mane", "head", (0, 31.5, 3.2))

    def legs(sd, n):
        bone("leg_" + n, "root", (-2.6, 13, 0), side=sd)
        cube("leg_" + n, "thigh_" + n, (-4.8, 6.6, -2.5), (4.4, 6.8, 5.0), "skin", side=sd)
        cube("leg_" + n, "thigh_plate_" + n, (-5.0, 9.0, -2.8), (4.8, 1.6, 5.6), "accent", side=sd)
        bone("leg_%s_low" % n, "leg_" + n, (-2.6, 6.8, 0.6), side=sd)
        cube("leg_%s_low" % n, "shin_" + n, (-4.5, 1.8, -1.8), (4.0, 5.4, 3.8), "skin", rot=(14, 0, 0), pv=(-2.6, 6.8, 0.6), side=sd)
        cube("leg_%s_low" % n, "greave_" + n, (-4.7, 4.2, -2.3), (4.4, 2.2, 4.4), "accent", rot=(14, 0, 0), pv=(-2.6, 6.8, 0.6), side=sd)
        bone("leg_%s_foot" % n, "leg_%s_low" % n, (-2.6, 1.8, -0.4), side=sd)
        cube("leg_%s_foot" % n, "hoof_" + n, (-4.6, 0.0, -3.4), (4.2, 2.0, 5.4), "hoof", side=sd)
        cube("leg_%s_foot" % n, "toe_a_" + n, (-4.3, 0.0, -4.4), (1.6, 1.5, 1.2), "claw", side=sd)
        cube("leg_%s_foot" % n, "toe_b_" + n, (-2.4, 0.0, -4.4), (1.6, 1.5, 1.2), "claw", side=sd)
    pair(legs)
    cube("body", "pelvis", (-4.7, 11.4, -2.7), (9.4, 3.2, 5.4), "skin")
    cube("body", "belt", (-5.0, 12.6, -3.0), (10.0, 1.8, 6.0), "accent")
    cube("body", "belt_gem", (-1.1, 12.5, -3.4), (2.2, 2.0, 0.6), "gem")
    cube("body", "loin_front", (-3.6, 5.8, -3.3), (7.2, 7.0, 0.7), "cloth")
    cube("body", "loin_front_edge", (-3.6, 5.8, -3.7), (7.2, 0.9, 0.5), "accent")
    cube("body", "loin_back", (-3.8, 6.4, 2.6), (7.6, 6.4, 0.7), "cloth")
    cube("body", "abdomen", (-4.2, 14.4, -2.4), (8.4, 4.2, 4.8), "skin", paint="abs")
    cube("chest", "chest", (-6.0, 18.4, -3.3), (12.0, 6.2, 6.6), "skin", paint="chest")
    cube("chest", "chest_gem", (-1.0, 20.6, -3.8), (2.0, 2.4, 0.7), "gem")
    cube("chest", "spine_plate", (-3.0, 18.8, 3.2), (6.0, 5.4, 0.9), "accent")
    cube("chest", "collar", (-4.2, 24.4, -2.6), (8.4, 1.4, 5.2), "accent")
    cube("chest", "neck", (-2.0, 24.4, -1.8), (4.0, 1.4, 3.6), "skin")
    # the head
    cube("head", "head", (-4.0, 25.0, -4.0), (8.0, 8.0, 8.0), "skin", paint="face")
    cube("head", "brow", (-4.4, 30.0, -4.5), (8.8, 1.2, 1.5), "skin")
    cube("head", "nose", (-0.8, 26.8, -4.8), (1.6, 2.4, 1.0), "skin")

    def face_bits(sd, n):
        cube("head", "cheek_" + n, (-5.1, 26.2, -2.4), (1.2, 1.6, 2.6), "skin", rot=(0, 0, 18), side=sd)
        cube("head", "ear_" + n, (-5.6, 28.4, -0.8), (2.4, 1.0, 2.0), "skin", rot=(0, 0, 24), side=sd)
        cube("head", "ear_tip_" + n, (-7.0, 29.0, -0.5), (1.8, 0.8, 1.4), "skin", rot=(0, 0, 38), side=sd)
    pair(face_bits)
    cube("jaw", "jaw", (-3.4, 25.0, -4.3), (6.8, 1.9, 4.7), "skin", paint="jaw")
    cube("jaw", "chin_spike", (-0.6, 23.9, -4.2), (1.2, 1.3, 1.0), "horn")
    cube("jaw", "fang_r", (-3.0, 26.8, -4.2), (0.8, 1.0, 0.8), "tooth")
    cube("jaw", "fang_l", (2.2, 26.8, -4.2), (0.8, 1.0, 0.8), "tooth")
    # hair: a scalp shell, a fringe with three tufts, a mane and two long locks
    cube("head", "scalp", (-4.4, 31.0, -4.4), (8.8, 2.4, 8.8), "hair")
    cube("head", "scalp_back", (-4.4, 25.6, 3.7), (8.8, 6.0, 1.4), "hair")
    cube("head", "fringe", (-4.4, 31.0, -4.8), (8.8, 1.2, 0.9), "hair")
    for i, (x, h) in enumerate(((-4.2, 2.6), (-0.6, 1.9), (3.0, 2.6))):
        cube("head", "fringe_tuft_%d" % i, (x, 31.4 - h, -4.9), (1.2, h, 0.8), "hair")
    cube("mane", "mane_a", (-3.0, 28.4, 4.0), (6.0, 4.4, 1.6), "hair", rot=(-12, 0, 0))
    cube("mane", "mane_b", (-2.2, 24.6, 4.6), (4.4, 4.2, 1.4), "hair", rot=(-18, 0, 0))

    def locks(sd, n):
        bone("lock_" + n, "head", (-3.6, 30.0, 4.0), side=sd)
        cube("lock_" + n, "lock_a_" + n, (-4.4, 24.0, 3.6), (1.8, 6.6, 1.6), "hair", side=sd)
        cube("lock_" + n, "lock_b_" + n, (-4.2, 20.8, 3.9), (1.4, 3.6, 1.2), "hair", rot=(-6, 0, 0), side=sd)
    pair(locks)

    def horns(sd, n):                                                      # three segments curving up and back
        bone("horn_" + n, "head", (-2.9, 32.4, -0.6), rot=(8, 0, 24), side=sd)
        cube("horn_" + n, "horn_a_" + n, (-4.3, 32.0, -2.0), (2.8, 3.2, 2.8), "horn", side=sd)
        bone("horn_%s2" % n, "horn_" + n, (-2.9, 35.2, -0.6), rot=(-18, 0, 20), side=sd)
        cube("horn_%s2" % n, "horn_b_" + n, (-4.0, 35.0, -1.8), (2.2, 3.4, 2.2), "horn", side=sd)
        bone("horn_%s3" % n, "horn_%s2" % n, (-2.9, 38.4, -0.6), rot=(-26, 0, 22), side=sd)
        cube("horn_%s3" % n, "horn_c_" + n, (-3.6, 38.2, -1.4), (1.4, 3.6, 1.4), "horn_tip", side=sd, paint="tip")
    pair(horns)

    def arms(sd, n):
        bone("arm_" + n, "chest", (-7.0, 23.6, 0), side=sd)
        cube("arm_" + n, "pauldron_" + n, (-10.4, 23.0, -3.2), (5.0, 2.2, 6.4), "accent", side=sd)
        cube("arm_" + n, "pauldron_spike_a_" + n, (-9.8, 25.0, -1.8), (1.4, 2.8, 1.4), "horn", rot=(0, 0, 14), side=sd)
        cube("arm_" + n, "pauldron_spike_b_" + n, (-8.6, 25.0, 0.6), (1.2, 2.2, 1.2), "horn", rot=(0, 0, 10), side=sd)
        cube("arm_" + n, "upper_" + n, (-10.0, 16.8, -2.3), (4.2, 7.0, 4.6), "skin", side=sd)
        bone("arm_%s_low" % n, "arm_" + n, (-8.0, 17.2, 0), side=sd)
        cube("arm_%s_low" % n, "fore_" + n, (-10.2, 10.6, -2.5), (4.6, 6.8, 5.0), "skin", side=sd)
        cube("arm_%s_low" % n, "bracer_" + n, (-10.5, 11.0, -2.8), (5.2, 3.0, 5.6), "accent", side=sd)
        cube("arm_%s_low" % n, "elbow_spike_" + n, (-11.4, 15.6, -0.6), (1.6, 1.2, 1.2), "horn", rot=(0, 0, 18), side=sd)
        bone("arm_%s_hand" % n, "arm_%s_low" % n, (-8.0, 10.8, 0), side=sd)
        cube("arm_%s_hand" % n, "palm_" + n, (-9.9, 7.8, -1.9), (3.8, 3.0, 3.8), "skin", side=sd)
        for i, z in enumerate((-1.7, -0.45, 0.8)):
            cube("arm_%s_hand" % n, "claw_%d_%s" % (i, n), (-9.5 + 0.9 * (i == 1), 5.2, z), (0.9, 2.8, 1.0), "claw", side=sd)
        cube("arm_%s_hand" % n, "thumb_" + n, (-10.6, 8.4, -0.6), (1.0, 2.6, 1.0), "claw", rot=(0, 0, -14), side=sd)
    pair(arms)

    def wings(sd, n):                                                      # a bone arm out to the side, a second segment, five stair-stepped membrane panels hanging from it, three ribs
        bone("wing_" + n, "chest", (-2.4, 23.2, 3.6), rot=(0, 14, 10), side=sd)
        cube("wing_" + n, "wing_arm_a_" + n, (-11.6, 22.4, 3.0), (9.4, 1.8, 1.8), "horn", side=sd)
        cube("wing_" + n, "wing_root_" + n, (-3.6, 21.6, 2.6), (2.4, 3.2, 2.6), "skin", side=sd)
        bone("wing_%s_tip" % n, "wing_" + n, (-11.6, 23.2, 3.9), rot=(0, 10, 8), side=sd)
        cube("wing_%s_tip" % n, "wing_arm_b_" + n, (-19.0, 22.6, 3.4), (7.6, 1.4, 1.4), "horn", side=sd)
        cube("wing_%s_tip" % n, "wing_hook_" + n, (-20.6, 22.2, 3.0), (1.8, 1.0, 2.2), "claw", side=sd)
        lens = (8.5, 10.5, 9.0, 6.5, 4.0)
        for i in range(5):
            x1 = 3.4 + i * 3.1
            cube("wing_" + n if i < 3 else "wing_%s_tip" % n, "membrane_%d_%s" % (i, n), (-(x1 + 3.1), 23.0 - lens[i], 4.1), (3.1, lens[i], 0.4), "membrane", side=sd, paint="membrane")
        for i, (x, ln) in enumerate(((7.0, 10.5), (12.0, 9.0), (17.0, 5.0))):
            cube("wing_" + n if i < 2 else "wing_%s_tip" % n, "rib_%d_%s" % (i, n), (-x - 0.4, 23.4 - ln, 3.9), (0.8, ln, 0.9), "horn", side=sd)
    pair(wings)
    # the tail: three tapering segments and a spade
    bone("tail1", "body", (0, 12.8, 2.8), rot=(-8, 0, 0))
    cube("tail1", "tail_a", (-1.7, 11.1, 2.4), (3.4, 3.4, 5.6), "skin")
    bone("tail2", "tail1", (0, 12.6, 8.0), rot=(10, 0, 0))
    cube("tail2", "tail_b", (-1.3, 11.4, 7.6), (2.6, 2.6, 5.6), "skin")
    bone("tail3", "tail2", (0, 12.4, 13.2), rot=(-16, 0, 0))
    cube("tail3", "tail_c", (-0.9, 11.6, 12.8), (1.8, 1.8, 5.0), "skin")
    cube("tail3", "tail_spade", (-2.0, 11.7, 17.4), (4.0, 0.7, 4.0), "horn", rot=(0, 45, 0))
    cube("tail3", "tail_spade_fin", (-0.35, 10.6, 16.9), (0.7, 3.0, 4.8), "horn")


# ---------------------------------------------------------------- the texture sheet
def face_sizes(c, k):
    f = lambda v: max(1, int(round(v * k)))
    w, h, d = c["size"]
    return f(w), f(h), f(d)


def pack(k):
    """A shelf packer for the box-UV crosses at density k (texels per model pixel); None when they do not fit the sheet."""
    items = []
    for i, c in enumerate(CUBES):
        fw, fh, fd = face_sizes(c, k)
        items.append((fd + fh, 2 * (fw + fd), i))
    items.sort(key=lambda t: (-t[0], -t[1], t[2]))
    x = y = shelf = 0
    pos = {}
    for hh, ww, i in items:
        if x + ww > SHEET:
            x, y, shelf = 0, y + shelf, 0
        if y + hh > SHEET:
            return None
        pos[i] = (x, y)
        x += ww
        shelf = max(shelf, hh)
    return pos


def fit_density():
    for k in (2.0, 1.75, 1.5, 1.25, 1.0):
        pos = pack(k)
        if pos is not None:
            return k, pos
    raise SystemExit("the cubes do not fit a %dx%d sheet at density 1" % (SHEET, SHEET))


def face_rects(c, k, org):
    fw, fh, fd = face_sizes(c, k)
    u, v = org
    return {"east": (u, v + fd, fd, fh), "north": (u + fd, v + fd, fw, fh), "west": (u + fd + fw, v + fd, fd, fh), "south": (u + fd + fw + fd, v + fd, fw, fh),
            "up": (u + fd, v, fw, fd), "down": (u + fd + fw, v, fw, fd)}


def rng_for(*parts):
    h = 2166136261
    for p in parts:
        for ch in str(p):
            h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return np.random.RandomState(h)


def shade_face(tn, face, fw, fh, seed):
    """Float RGB (fh, fw, 3): lit from above (top lightest, bottom darkest), noise, and a darker rim so cubes read apart."""
    lo, mid, hi = (np.array(t, np.float32) for t in tn)
    if face == "up":
        base = np.broadcast_to(hi, (fh, fw, 3)).copy()
    elif face == "down":
        base = np.broadcast_to(lo, (fh, fw, 3)).copy()
    else:
        t = np.clip(np.linspace(0, 1, fh, dtype=np.float32)[:, None, None], 0, 1)
        base = np.broadcast_to(mid + (lo - mid) * t * 0.8 + (hi - mid) * (1 - t) * 0.25, (fh, fw, 3)).copy()
        if face in ("east", "west"):
            base *= 0.9
    base *= 1.0 + rng_for(*seed).uniform(-0.05, 0.05, (fh, fw, 1)).astype(np.float32)
    if fw > 2 and fh > 2:
        base[0, :] *= 0.9
        base[-1, :] *= 0.82
        base[:, 0] *= 0.9
        base[:, -1] *= 0.9
    return base


def paint_material(mat, tn, face, fw, fh, seed):
    img = shade_face(tn, face, fw, fh, seed)
    rs = rng_for(*seed, "m")
    if mat == "skin" and face not in ("up", "down") and fh > 3:
        img[2:fh - 1:3, :] *= 0.94                                         # faint muscle lines
    elif mat in ("horn", "horn_tip"):
        img[1::3, :] *= 0.86                                               # growth rings
    elif mat == "hair":
        for x in range(0, fw, 2):                                          # strands run down
            img[:, x] *= 0.9 + 0.1 * rs.rand()
    elif mat == "accent" and fw > 3 and fh > 3:
        img[1, 1:-1] *= 1.22
        img[1:-1, 1] *= 1.12
        img[-2, 1:-1] *= 0.78
        if fw > 6 and fh > 5:
            img[fh // 2, 2:-2] *= 0.88
    elif mat == "cloth":
        img[0::2, :] *= 0.9
        img[:, 0::4] *= 0.93
    elif mat == "membrane":
        img[:, 0::5] *= 0.86                                               # veins
        if face in ("up", "down"):
            img[0::3, :] *= 0.85
    return img


def draw_front(kind, pal, fw, fh, size_w, size_h):
    """A painted detail on a face, drawn in model pixels at 6x oversampling, then boxed down: (overlay RGBA, glow RGBA) as float arrays (fh, fw, 4)."""
    S = 6
    ov = Image.new("RGBA", (fw * S, fh * S), (0, 0, 0, 0))
    gl = Image.new("RGBA", (fw * S, fh * S), (0, 0, 0, 0))
    d, g = ImageDraw.Draw(ov), ImageDraw.Draw(gl)
    sx, sy = fw * S / float(size_w), fh * S / float(size_h)
    P = lambda x, y: ((int(x * fw / size_w) + 0.5) * S, (int(y * fh / size_h) + 0.5) * S)     # snapped to texel centres: crisp pixel art, not a blur
    W = lambda v: max(S, int(v * sx))
    ink = mix(pal["skin"][0], (0, 0, 0), 0.65) + (255,)
    glow = pal["glow"] + (255,)
    if kind == "face" and (fw, fh) == (16, 16):                            # head front at 2 texels per px: the rows under the brow (6-11) are what shows
        key = {"k": ink, "g": glow}
        rows = {6: "..kk........kk..", 7: ".kgggk....kgggk.", 8: "..kgk......kgk..", 9: ".g............g.", 10: "..g...kk....g...", 11: "................"}
        rows[10] = "..g....kk....g.."     # nostrils in the middle, the second cheek mark
        assert all(len(v) == 16 for v in rows.values())
        for y, line in rows.items():
            for x, ch in enumerate(line):
                if ch != ".":
                    for tgt in ((d, g) if ch == "g" else (d,)):
                        tgt.rectangle([x * S, y * S, (x + 1) * S - 1, (y + 1) * S - 1], fill=key[ch])
    elif kind == "face":                                                   # head front, 8 x 8 model px, y down from the top
        for sgn in (-1, 1):
            cx = 4 + sgn * 1.75
            lid = [P(cx - sgn * 1.3, 3.7), P(cx + sgn * 1.5, 3.1), P(cx + sgn * 1.3, 4.2), P(cx - sgn * 1.15, 4.6)]
            d.polygon(lid, fill=ink)
            iris = [P(cx - sgn * 0.95, 3.9), P(cx + sgn * 1.05, 3.5), P(cx + sgn * 0.95, 4.1), P(cx - sgn * 0.8, 4.4)]
            d.polygon(iris, fill=glow)
            g.polygon(iris, fill=glow)
            d.line([P(cx - sgn * 0.05, 3.6), P(cx - sgn * 0.05, 4.3)], fill=(10, 4, 6, 255), width=W(0.22))        # slit pupil
            d.line([P(cx - sgn * 1.5, 3.0), P(cx + sgn * 1.7, 2.3)], fill=ink, width=W(0.5))                        # brow
            d.line([P(4 + sgn * 0.55, 5.2), P(4 + sgn * 0.55, 5.7)], fill=ink, width=W(0.26))                       # nostril
            for i in range(2):                                                                                      # cheek brand
                g.line([P(4 + sgn * 3.5, 4.9 + i * 0.7), P(4 + sgn * 2.8, 5.3 + i * 0.7)], fill=glow, width=W(0.22))
                d.line([P(4 + sgn * 3.5, 4.9 + i * 0.7), P(4 + sgn * 2.8, 5.3 + i * 0.7)], fill=glow, width=W(0.22))
        d.line([P(2.4, 6.7), P(5.6, 6.7)], fill=ink, width=W(0.26))
        for x in (3.0, 5.0):
            d.polygon([P(x - 0.22, 6.7), P(x + 0.22, 6.7), P(x, 7.6)], fill=pal["tooth"][1] + (255,))
    elif kind == "chest":                                                  # chest front 12 x 6.6: sternum line, pectoral arcs, glowing brand
        d.line([P(6, 0.3), P(6, 6.2)], fill=ink, width=W(0.3))
        for sgn in (-1, 1):
            d.line([P(6 + sgn * 0.3, 4.2), P(6 + sgn * 2.0, 5.2), P(6 + sgn * 4.4, 4.6)], fill=ink, width=W(0.28))
            d.line([P(6 + sgn * 1.0, 0.8), P(6 + sgn * 4.6, 0.3)], fill=ink, width=W(0.26))
            for ln in (((0.9, 1.0), (3.0, 3.6)), ((2.0, 0.7), (4.7, 2.8))):
                for target in (d, g):
                    target.line([P(6 + sgn * ln[0][0], ln[0][1]), P(6 + sgn * ln[1][0], ln[1][1])], fill=glow, width=W(0.2))
    elif kind == "abs":                                                    # abdomen front 8.4 x 4.2
        for y in (1.4, 2.8):
            d.line([P(0.8, y), P(7.6, y)], fill=ink, width=W(0.22))
        d.line([P(4.2, 0.2), P(4.2, 4.0)], fill=ink, width=W(0.22))
    elif kind == "jaw":
        d.line([P(0.4, 0.55), P(6.4, 0.55)], fill=ink, width=W(0.2))
    return (np.asarray(ov.resize((fw, fh), Image.BOX), np.float32), np.asarray(gl.resize((fw, fh), Image.BOX), np.float32))


def paint_sheet(pal, k, pos):
    base = np.zeros((SHEET, SHEET, 4), np.float32)
    glow = np.zeros((SHEET, SHEET, 4), np.float32)
    gem = (pal["glow_dark"], pal["glow"], tuple(min(255, int(x * 1.1 + 40)) for x in pal["glow"]))
    tn = dict(skin=pal["skin"], horn=pal["horn"], horn_tip=pal["horn"], hair=pal["hair"], accent=pal["accent"], hoof=pal["hoof"], claw=pal["horn"], cloth=pal["cloth"],
              membrane=pal["membrane"], tooth=pal["tooth"], gem=gem)
    gl_rgb = np.array(pal["glow"], np.float32)
    for i, c in enumerate(CUBES):
        w, h, d = c["size"]
        fsz = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        for face, (u, v, fw, fh) in face_rects(c, k, pos[i]).items():
            img = paint_material(c["mat"], tn[c["mat"]], face, fw, fh, (c["name"], face))
            g_rgba = np.zeros((fh, fw, 4), np.float32)
            if c["mat"] == "gem":                                         # the whole gem glows
                img = img * 0.45 + gl_rgb * 0.55
                g_rgba[..., :3], g_rgba[..., 3] = gl_rgb, 255
            if c["paint"] == "horn_tip" or c["mat"] == "horn_tip":        # the horn's last segment glows from the tip in
                t = np.linspace(1.0, 0.0, fh, dtype=np.float32)[:, None, None] if face not in ("up", "down") else 1.0
                img = img * (1 - 0.55 * t) + gl_rgb * 0.55 * t
                g_rgba[..., :3] = gl_rgb
                g_rgba[..., 3] = (255 * np.broadcast_to(t, (fh, fw, 1))[..., 0]) if face not in ("up", "down") else 255
            if c["paint"] in ("face", "chest", "abs", "jaw") and face == "north":
                ov, gl = draw_front(c["paint"], pal, fw, fh, fsz[face][0], fsz[face][1])
                a = ov[..., 3:4] / 255.0
                img = img * (1 - a) + ov[..., :3] * a
                g_rgba = np.maximum(g_rgba, gl)
            base[v:v + fh, u:u + fw] = np.concatenate([np.clip(img, 0, 255), np.full((fh, fw, 1), 255, np.float32)], axis=2)
            glow[v:v + fh, u:u + fw] = g_rgba
    return base, glow


def save_png(arr, path):
    Image.fromarray((np.clip(arr, 0, 255) + 0.5).astype(np.uint8), "RGBA").save(path, optimize=True)


# ---------------------------------------------------------------- the clips
def keys(fn, length, step):
    return {"%.2f" % (i * step): [round(float(x), 3) for x in fn(i * step)] for i in range(int(round(length / step)) + 1)}


def clips():
    TAU = 2 * math.pi
    L = 3.2
    s = lambda t, ph=0.0: math.sin(TAU * t / L + ph)
    idle = {
        "body": {"position": keys(lambda t: (0, 0.22 * s(t), 0), L, 0.2)},
        "chest": {"rotation": keys(lambda t: (1.4 * s(t, 0.4), 1.0 * s(t, 1.1), 0), L, 0.2)},
        "head": {"rotation": keys(lambda t: (2.0 * s(t, 1.0), 4.0 * s(t, 0.2), 0.8 * s(t, 2.0)), L, 0.2)},
        "arm_r": {"rotation": keys(lambda t: (2.0 * s(t, 0.6), 0, -3 + 1.5 * s(t, 0.2)), L, 0.2)},
        "arm_l": {"rotation": keys(lambda t: (2.0 * s(t, 2.2), 0, 3 - 1.5 * s(t, 0.2)), L, 0.2)},
        "arm_r_low": {"rotation": keys(lambda t: (-12 + 3 * s(t, 0.2), 0, 0), L, 0.2)},
        "arm_l_low": {"rotation": keys(lambda t: (-12 + 3 * s(t, 1.6), 0, 0), L, 0.2)},
        "wing_r": {"rotation": keys(lambda t: (0, 4.0 * s(t), -9.0 * s(t, 0.3)), L, 0.2)},
        "wing_l": {"rotation": keys(lambda t: (0, -4.0 * s(t), 9.0 * s(t, 0.3)), L, 0.2)},
        "wing_r_tip": {"rotation": keys(lambda t: (0, 3.0 * s(t, -0.6), -7.0 * s(t, -0.5)), L, 0.2)},
        "wing_l_tip": {"rotation": keys(lambda t: (0, -3.0 * s(t, -0.6), 7.0 * s(t, -0.5)), L, 0.2)},
        "tail1": {"rotation": keys(lambda t: (0, 8 * s(t), 0), L, 0.2)},
        "tail2": {"rotation": keys(lambda t: (0, 10 * s(t, -0.7), 0), L, 0.2)},
        "tail3": {"rotation": keys(lambda t: (0, 12 * s(t, -1.4), 0), L, 0.2)},
        "lock_r": {"rotation": keys(lambda t: (3 * s(t, 0.8), 0, 2 * s(t, 0.3)), L, 0.2)},
        "lock_l": {"rotation": keys(lambda t: (3 * s(t, 1.4), 0, -2 * s(t, 0.3)), L, 0.2)},
        "jaw": {"rotation": keys(lambda t: (2.0 + 2.0 * max(0.0, s(t, 0.5)), 0, 0), L, 0.2)},
    }
    W = 1.0
    w = lambda t, ph=0.0: math.sin(TAU * t / W + ph)
    shin = lambda t, ph: 34.0 * max(0.0, math.sin(TAU * t / W + ph + 1.2))
    st = 0.125
    walk = {
        "body": {"position": keys(lambda t: (0, 0.5 * abs(w(t)) - 0.2, 0), W, st), "rotation": keys(lambda t: (3.0, 2.5 * w(t, 1.57), 0), W, st)},
        "chest": {"rotation": keys(lambda t: (0, -4.0 * w(t, 1.57), 0), W, st)},
        "head": {"rotation": keys(lambda t: (-2.0, 2.0 * w(t, 1.57), 0), W, st)},
        "leg_r": {"rotation": keys(lambda t: (34 * w(t), 0, 0), W, st)},
        "leg_l": {"rotation": keys(lambda t: (34 * w(t, math.pi), 0, 0), W, st)},
        "leg_r_low": {"rotation": keys(lambda t: (shin(t, 0.0), 0, 0), W, st)},
        "leg_l_low": {"rotation": keys(lambda t: (shin(t, math.pi), 0, 0), W, st)},
        "leg_r_foot": {"rotation": keys(lambda t: (-0.4 * shin(t, 0.0), 0, 0), W, st)},
        "leg_l_foot": {"rotation": keys(lambda t: (-0.4 * shin(t, math.pi), 0, 0), W, st)},
        "arm_r": {"rotation": keys(lambda t: (-26 * w(t), 0, -3), W, st)},
        "arm_l": {"rotation": keys(lambda t: (-26 * w(t, math.pi), 0, 3), W, st)},
        "arm_r_low": {"rotation": keys(lambda t: (-18 - 10 * max(0, -w(t)), 0, 0), W, st)},
        "arm_l_low": {"rotation": keys(lambda t: (-18 - 10 * max(0, w(t)), 0, 0), W, st)},
        "wing_r": {"rotation": keys(lambda t: (0, 6.0 * w(t, 0.5), -14.0 * w(t, 0.2)), W, st)},
        "wing_l": {"rotation": keys(lambda t: (0, -6.0 * w(t, 0.5), 14.0 * w(t, 0.2)), W, st)},
        "wing_r_tip": {"rotation": keys(lambda t: (0, 4.0 * w(t, -0.2), -10.0 * w(t, -0.4)), W, st)},
        "wing_l_tip": {"rotation": keys(lambda t: (0, -4.0 * w(t, -0.2), 10.0 * w(t, -0.4)), W, st)},
        "tail1": {"rotation": keys(lambda t: (0, 12 * w(t, 1.57), 0), W, st)},
        "tail2": {"rotation": keys(lambda t: (0, 16 * w(t, 0.9), 0), W, st)},
        "tail3": {"rotation": keys(lambda t: (0, 20 * w(t, 0.2), 0), W, st)},
        "lock_r": {"rotation": keys(lambda t: (8 + 6 * w(t, 0.4), 0, 0), W, st)},
        "lock_l": {"rotation": keys(lambda t: (8 + 6 * w(t, 0.9), 0, 0), W, st)},
    }

    def tl(rows):
        return {("%.2f" % t): [float(a), float(b), float(c)] for t, (a, b, c) in rows}
    attack = {
        "body": {"rotation": tl([(0, (0, 0, 0)), (0.15, (-6, 0, 0)), (0.3, (10, 0, 0)), (0.6, (0, 0, 0))]),
                 "position": tl([(0, (0, 0, 0)), (0.15, (0, -0.4, 0.8)), (0.3, (0, -0.3, -1.4)), (0.6, (0, 0, 0))])},
        "chest": {"rotation": tl([(0, (0, 0, 0)), (0.15, (-8, -22, 0)), (0.3, (12, 26, 0)), (0.6, (0, 0, 0))])},
        "head": {"rotation": tl([(0, (0, 0, 0)), (0.15, (-8, -10, 0)), (0.3, (14, 10, 0)), (0.6, (0, 0, 0))])},
        "jaw": {"rotation": tl([(0, (0, 0, 0)), (0.15, (22, 0, 0)), (0.3, (6, 0, 0)), (0.6, (0, 0, 0))])},
        "arm_r": {"rotation": tl([(0, (0, 0, -3)), (0.15, (-150, 0, -10)), (0.3, (-52, 0, 4)), (0.45, (-30, 0, 0)), (0.6, (0, 0, -3))])},
        "arm_r_low": {"rotation": tl([(0, (-12, 0, 0)), (0.15, (-34, 0, 0)), (0.3, (-4, 0, 0)), (0.6, (-12, 0, 0))])},
        "arm_l": {"rotation": tl([(0, (0, 0, 3)), (0.15, (-34, 0, 12)), (0.3, (14, 0, 6)), (0.6, (0, 0, 3))])},
        "wing_r": {"rotation": tl([(0, (0, 0, 0)), (0.15, (0, 10, -22)), (0.3, (0, -6, 8)), (0.6, (0, 0, 0))])},
        "wing_l": {"rotation": tl([(0, (0, 0, 0)), (0.15, (0, -10, 22)), (0.3, (0, 6, -8)), (0.6, (0, 0, 0))])},
        "tail1": {"rotation": tl([(0, (0, 0, 0)), (0.15, (0, 18, 0)), (0.3, (0, -22, 0)), (0.6, (0, 0, 0))])},
        "leg_r": {"rotation": tl([(0, (0, 0, 0)), (0.15, (14, 0, 0)), (0.3, (-18, 0, 0)), (0.6, (0, 0, 0))])},
        "leg_l": {"rotation": tl([(0, (0, 0, 0)), (0.15, (-10, 0, 0)), (0.3, (8, 0, 0)), (0.6, (0, 0, 0))])},
    }
    return {"animation.devil_trial.idle": {"loop": True, "animation_length": L, "bones": idle},
            "animation.devil_trial.walk": {"loop": True, "animation_length": W, "bones": walk},
            "animation.devil_trial.attack": {"loop": False, "animation_length": 0.6, "bones": attack}}


# ---------------------------------------------------------------- the exports
def geo_doc(k, pos):
    by_bone = {}
    for i, c in enumerate(CUBES):
        r = face_rects(c, k, pos[i])
        e = {"origin": c["origin"], "size": c["size"], "uv": {f: {"uv": [r[f][0], r[f][1]], "uv_size": [r[f][2], r[f][3]]} for f in ("east", "north", "west", "south", "up", "down")}}
        if c["rotation"]:
            e["pivot"], e["rotation"] = c["pivot"], c["rotation"]
        by_bone.setdefault(c["bone"], []).append(e)
    bones = []
    for b in BONES:
        e = {"name": b["name"]}
        if b["parent"]:
            e["parent"] = b["parent"]
        e["pivot"] = b["pivot"]
        if any(b["rotation"]):
            e["rotation"] = b["rotation"]
        e["cubes"] = by_bone.get(b["name"], [])
        bones.append(e)
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": "geometry.devil_trial", "texture_width": SHEET, "texture_height": SHEET,
                                                                                 "visible_bounds_width": 5, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.2, 0]}, "bones": bones}]}


def uid(*parts):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "devil_trial/" + "/".join(str(p) for p in parts)))


def bb_doc(k, pos, sheet_png, anims):
    """The Blockbench source: free-model cubes with per-face UV, bones as groups. Rotations use the Bedrock codec's sign (x and y negated); see the module docstring."""
    elements, ids = [], {}
    for i, c in enumerate(CUBES):
        o, s = c["origin"], c["size"]
        r = face_rects(c, k, pos[i])
        e = {"name": c["name"], "box_uv": False, "rescale": False, "locked": False, "light_emission": 0, "render_order": "default", "allow_mirror_modeling": False,
             "from": o, "to": [o[0] + s[0], o[1] + s[1], o[2] + s[2]], "autouv": 0, "color": 0,
             "origin": c["pivot"] or [o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2],
             "faces": {f: {"uv": [r[f][0], r[f][1], r[f][0] + r[f][2], r[f][1] + r[f][3]], "texture": 0} for f in ("north", "east", "south", "west", "up", "down")},
             "type": "cube", "uuid": uid("cube", c["name"])}
        if c["rotation"]:
            e["rotation"] = [-c["rotation"][0], -c["rotation"][1], c["rotation"][2]]
        elements.append(e)
        ids[c["name"]] = e["uuid"]
    groups = {b["name"]: {"name": b["name"], "origin": b["pivot"], "rotation": [-b["rotation"][0], -b["rotation"][1], b["rotation"][2]], "color": 0, "uuid": uid("bone", b["name"]),
                          "export": True, "isOpen": True, "visibility": True, "autouv": 0, "children": []} for b in BONES}
    outliner = []
    for b in BONES:
        (groups[b["parent"]]["children"] if b["parent"] else outliner).append(groups[b["name"]])
    for c in CUBES:
        groups[c["bone"]]["children"].append(ids[c["name"]])
    buf = io.BytesIO()
    sheet_png.save(buf, "PNG")
    tex = {"path": "", "name": "devil_trial.png", "folder": "", "namespace": "", "id": "0", "particle": False, "render_mode": "default", "render_sides": "auto", "frame_time": 1,
           "frame_order_type": "loop", "frame_order": "", "frame_interpolation": False, "visible": True, "internal": True, "saved": False, "uuid": uid("tex"),
           "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")}
    banims = []
    for name, a in anims.items():
        animators = {}
        for bn, tr in a["bones"].items():
            kf = []
            for ch in ("rotation", "position"):
                for t, v in tr.get(ch, {}).items():
                    x = -v[0] if ch == "position" else v[0]
                    kf.append({"channel": ch, "data_points": [{"x": x, "y": v[1], "z": v[2]}], "uuid": uid("kf", name, bn, ch, t), "time": float(t), "color": -1, "interpolation": "linear"})
            animators[groups[bn]["uuid"]] = {"name": bn, "type": "bone", "keyframes": kf}
        banims.append({"uuid": uid("anim", name), "name": name.split(".")[-1], "loop": "loop" if a["loop"] else "once", "override": False, "length": a["animation_length"], "snapping": 20,
                       "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators})
    return {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False}, "name": "devil_trial", "model_identifier": "devil_trial", "visible_box": [5, 4, 1],
            "variable_placeholders": "", "bedrock_animation_mode": "entity", "resolution": {"width": SHEET, "height": SHEET}, "elements": elements, "outliner": outliner,
            "textures": [tex], "animations": banims}


def neutral():
    return dict(outfit=dict(skin=("#6a2a26", "#8a3a30", "#a85a46"), accent="#e8a838"), hair="#2a1418")


def write_all():
    import dvtable
    build()
    k, pos = fit_density()
    anims = clips()
    sheets = {}
    for suffix, dev in [("", neutral())] + [("_" + d["id"], d) for d in dvtable.DEVILS]:
        base, glow = paint_sheet(palette(dev), k, pos)
        gl = np.where(glow[..., 3:4] > 0, glow, 0).astype(np.float32)
        gl[..., 3] = np.where(glow[..., 3] > 0, glow[..., 3], 0)
        save_png(base, os.path.join(TEX_DIR, "devil_trial%s.png" % suffix))
        save_png(gl, os.path.join(TEX_DIR, "devil_trial%s_glowmask.png" % suffix))
        sheets[suffix] = (base, gl)
    geo = geo_doc(k, pos)
    json.dump(geo, open(GEO_OUT, "w"), indent=1)
    json.dump({"format_version": "1.8.0", "animations": anims}, open(ANIM_OUT, "w"), indent=1)
    first = Image.fromarray((np.clip(sheets[""][0], 0, 255) + 0.5).astype(np.uint8), "RGBA")
    json.dump(bb_doc(k, pos, first, anims), open(BB_OUT, "w"), indent=1)
    stats = dict(bones=len(BONES), cubes=len(CUBES), quads=len(CUBES) * 6, density=k, sheet=SHEET, devils=len(dvtable.DEVILS))
    json.dump(stats, open(os.path.join(HERE, "devil_trial_stats.json"), "w"), indent=1)
    print("devil_trial:", stats)
    return sheets, geo["minecraft:geometry"][0], anims, dvtable


if __name__ == "__main__":
    out = write_all()
    if "--preview" in sys.argv:
        sys.path.insert(0, HERE)
        import preview_trial
        preview_trial.main(*out)
