"""The Goliath, the Upper Catacombs' floor boss, in the repo's GeoLite pipeline (tools/geo_builder.py -> client.geo.GeoDraw): a colossal pale-stone giant,
6 blocks tall, built out of the ruin's own masonry, with bedrock-grey joints, moss in the creases, a heavy brow, tiny amber eyes, boulder fists and cracks
that glow faintly amber. One model, two skins: the whole stone body, and the cracked body of the Gates (more and wider cracks, all of them burning).

    python3 -B tools/gen_goliath.py                  # model, animations, both skins, preview scenes (idempotent)
    python3 -B tools/gen_goliath.py --check          # read every file back and check the model against its clips
    python3 -B tools/gen_goliath.py --preview [scene ...]    # the headless render of tools/render_preview (needs JOML_JAR / GSON_JAR or the usual lib folders)

Writes (GeoSpec.of("goliath", "body"), skin "goliath_cracked"):
    assets/nusmp/geo/entity/goliath_body.geo.json
    assets/nusmp/animations/entity/goliath_body.animation.json
    assets/nusmp/textures/entity/goliath_body.png            the stone skin, 512 wide at 2 texels per model pixel (the face at 3)
    assets/nusmp/textures/entity/goliath_body_glow.png       eyes, mouth, a few faint cracks, the rune on the fauld (additive, full bright)
    assets/nusmp/textures/entity/goliath_cracked.png         the same boxes, the skin of the Gates
    assets/nusmp/textures/entity/goliath_cracked_glow.png    every crack alight, the core in the chest, brighter eyes
and tools/render_preview/scenes.d/goliath.json.

Clips: idle walk attack cast cast_big hurt transform death rise, and one per move that has its own: slam stomp roar fist line rubble (a one-shot clip is
paced to the move's wind-up so its blow lands at 80% of its length: client/floor/goliath/GoliathPose). Rotation signs of this frame (checked in the
preview): positive x swings a hanging limb FORWARD (arms raise forward), negative x folds a knee and leans the body forward; positive z swings a hanging limb
towards +x (the model's left); positive y turns the head towards the model's right. Positions are pixels (y up), scales multiply.
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim, ROOT  # noqa: E402
from zogratis_lucius_kit import Pack, SIN, COS, TAU, keys, cyc, swing  # noqa: E402
from spirit_geo import NOISE  # noqa: E402

KEY = "goliath"
NAME = "body"
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "goliath.json")
SCRATCH = os.environ.get("GOLIATH_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/work_gol")
PREVIEW_OUT = os.path.join(SCRATCH, "previews")

# ------------------------------------------------------------------------------------------------------------------ palettes (dark -> light)
STONE = ((78, 76, 72), (104, 102, 96), (132, 130, 122), (160, 158, 148), (188, 186, 174), (212, 210, 196), (230, 228, 214))
STONE_HOT = ((74, 66, 62), (98, 88, 80), (124, 112, 102), (150, 138, 124), (176, 162, 146), (200, 186, 168), (220, 206, 186))
BED = ((26, 26, 32), (42, 42, 50), (60, 60, 70), (82, 82, 94), (106, 106, 120))
MOSS = ((34, 52, 30), (48, 72, 36), (64, 92, 42), (84, 114, 52), (106, 136, 64))
DARKEN = (16, 14, 14)
AMBER = (255, 168, 52)
AMBER_HOT = (255, 226, 140)
ORANGE = (255, 104, 28)

BAY = np.array(((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5)), np.float32)
SIDES = ("front", "back", "left", "right")


# ------------------------------------------------------------------------------------------------------------------ vectorised noise, hashes and palettes
def vz(x, y, salt=0, tab=NOISE):
    """The repo's value noise (spirit_geo.nz) over whole arrays, -0.5 .. 0.5."""
    xi = (np.asarray(x) * 3 + 128 + salt * 11).astype(np.int64) % 256
    yi = (np.asarray(y) * 3 + salt * 37).astype(np.int64) % 256
    return tab[yi, xi] - 0.5


def hv(a, b, salt=0):
    """A hash of two integer arrays to 0..1."""
    a = np.asarray(a).astype(np.int64) & 0xFFFFFFFF
    b = np.asarray(b).astype(np.int64) & 0xFFFFFFFF
    h = (a * 374761393 + b * 668265263 + ((salt * 2246822519) & 0xFFFFFFFF)) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFF) / 65535.0


def palarr(cols, t, ii, jj, dither=0.05):
    """The palette entry each tone falls on, ordered-dithered across band edges (hard-edged pixel art, not a gradient)."""
    cols = np.asarray(cols, np.float32)
    n = len(cols)
    t = np.clip(t + ((BAY[jj % 4, ii % 4] + 0.5) / 16.0 - 0.5) * dither, 0.0, 0.9999)
    return cols[(t * n).astype(int)]


def voronoi(W, H, cell, salt):
    """Per texel: the distance to the nearest and to the second nearest feature point, the nearest one's hash, and the offset to it."""
    jj, ii = np.mgrid[0:H, 0:W]
    x, y = ii + 0.5, jj + 0.5
    cx, cy = np.floor(x / cell).astype(np.int64), np.floor(y / cell).astype(np.int64)
    d1 = np.full((H, W), 1e9)
    d2 = np.full((H, W), 1e9)
    cid = np.zeros((H, W))
    ox = np.zeros((H, W))
    oy = np.zeros((H, W))
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            gx, gy = cx + dx, cy + dy
            px = (gx + 0.15 + 0.7 * hv(gx, gy, salt)) * cell
            py = (gy + 0.15 + 0.7 * hv(gx, gy, salt + 5)) * cell
            d = np.hypot(x - px, y - py)
            better = d < d1
            d2 = np.where(better, d1, np.minimum(d2, d))
            cid = np.where(better, hv(gx, gy, salt + 9), cid)
            ox = np.where(better, x - px, ox)
            oy = np.where(better, y - py, oy)
            d1 = np.where(better, d, d1)
    return d1, d2, cid, ox, oy


def bricks(W, H, rh, bw, salt):
    """A running bond of blocks: per texel the block's hash, the mortar mask, and the offsets from the block's top left corner."""
    tone = np.zeros((H, W))
    mortar = np.zeros((H, W), bool)
    fx = np.zeros((H, W))
    fy = np.zeros((H, W))
    y = -int(hv(0, 3, salt) * rh)
    row = 0
    while y < H:
        h = rh + int(hv(row, 1, salt) * 3) - 1
        x = -int(hv(row, 2, salt) * bw)
        col = 0
        while x < W:
            w = bw + int(hv(row, col + 40, salt) * 8) - 3
            t = float(hv(row, col, salt + 7))
            for yy in range(max(0, y), min(H, y + h)):
                for xx in range(max(0, x), min(W, x + w)):
                    tone[yy, xx] = t
                    fx[yy, xx] = xx - x
                    fy[yy, xx] = yy - y
                    if yy == y or yy == y + h - 1 or xx == x or xx == x + w - 1:
                        mortar[yy, xx] = True
            x += w
            col += 1
        y += h
        row += 1
    return tone, mortar, fx, fy


def crack_labels(W, H, salt, n, length, wide):
    """Cracks as random walks: an integer label (1..) on every texel a crack covers, 0 elsewhere; a few branch."""
    rng = np.random.RandomState((salt * 7919 + W * 131 + H * 17) & 0x7FFFFFFF)
    lab = np.zeros((H, W), np.int32)
    k = 0

    def walk(x, y, ang, steps, label, depth):
        for s in range(steps):
            ang += rng.normal(0, 0.5)
            x += math.cos(ang)
            y += math.sin(ang)
            xi, yi = int(x), int(y)
            for ox in range(2 if wide else 1):
                for oy in range(2 if wide and s % 2 == 0 else 1):
                    if 0 <= xi + ox < W and 0 <= yi + oy < H:
                        lab[yi + oy, xi + ox] = label
            if depth < 2 and rng.rand() < 0.07:
                walk(x, y, ang + rng.choice((-1, 1)) * rng.uniform(0.5, 1.2), int(steps * rng.uniform(0.25, 0.5)), label, depth + 1)
            if not (-4 < x < W + 4 and -4 < y < H + 4):
                return

    for _ in range(n):
        k += 1
        walk(rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0, TAU), max(6, int(length * rng.uniform(0.6, 1.2))), k, 0)
    return lab


def dilate(mask):
    out = mask.copy()
    out[1:, :] |= mask[:-1, :]
    out[:-1, :] |= mask[1:, :]
    out[:, 1:] |= mask[:, :-1]
    out[:, :-1] |= mask[:, 1:]
    return out


# ------------------------------------------------------------------------------------------------------------------ the stone painter
def stone(salt, kind="rock", cell=10, rh=8, bw=16, moss=0.2, jt=0, jb=0, cracks=2, cracked=False, tone=0.0, art=None, pal=None, glow_frac=0.35,
          crack_len=22):
    """A painter (see tools/zogratis_lucius_kit.Pack) of weathered pale stone. kind 'rock': irregular plates (a Voronoi of cracks); 'brick': a running bond of
    blocks. jt / jb: texel rows at the top / bottom of the side faces that are bedrock-grey and mossy (a joint). moss: how much of the rest is mossy.
    cracks: how many cracks cross each face; `cracked` is the skin of the Gates (3 times as many, wider, every one of them burning). art(face, rgb, a, glow):
    a hook that draws on the finished arrays of a face (the head's face, the chest's core, a rune)."""
    cache = {}
    cols = pal or (STONE_HOT if cracked else STONE)

    def build(face, W, H):
        jj, ii = np.mgrid[0:H, 0:W]
        ft = {"top": 0.10, "bottom": -0.22, "front": 0.0, "back": -0.07, "left": -0.03, "right": -0.03}[face]
        if kind == "brick":
            bt, mortar, fx, fy = bricks(W, H, rh, bw, salt)
            t = 0.50 + (bt - 0.5) * 0.34 + vz(ii * 0.5, jj * 0.5, salt + 3) * 0.20 + ft
            t = t + np.where((fy < 1.5) & ~mortar, 0.11, 0.0) + np.where((fx < 1.5) & ~mortar, 0.06, 0.0)       # a lit top and left edge of each block
            hit = (fy > 0) & (fx > 0)
            t = np.where(mortar, np.where(hit & ((fy < 1.5) | (fx < 1.5)), 0.06, 0.04), t)
        else:
            d1, d2, cid, ox, oy = voronoi(W, H, cell, salt)
            edge = d2 - d1
            bev = -(ox * 0.35 + oy * 0.55) / cell
            t = 0.50 + (cid - 0.5) * 0.36 + bev * 0.55 + vz(ii * 0.55, jj * 0.55, salt + 3) * 0.22 + ft
            t = np.where(edge < 1.0, 0.05, np.where(edge < 2.0, t + np.where(bev > 0, 0.09, -0.09), t))
        t = t + tone + 0.05
        base = palarr(cols, t, ii, jj, 0.05)
        rgb = base.copy()
        if face in SIDES and (jt or jb):                                                  # joints: bedrock-grey, ragged
            rag = (vz(ii * 0.6, jj * 0.6, salt + 17) + 0.5) * 3.0
            joint = ((jj < jt + rag) if jt else np.zeros((H, W), bool)) | ((jj > H - 1 - jb - rag) if jb else np.zeros((H, W), bool))
            jt_tone = t * 0.8 + 0.12 + vz(ii * 0.7, jj * 0.7, salt + 19) * 0.5
            rgb = np.where(joint[..., None], palarr(BED, jt_tone, ii, jj, 0.06), rgb)
        # moss: patches on the tops and in the creases, creeping up from the bottom of the side faces
        m = vz(ii * 0.11, jj * 0.11, salt + 7) * 2.0 + vz(ii * 0.33, jj * 0.33, salt + 9) * 0.7
        bias = moss * 1.5 - 0.62 + (0.45 if face == "top" else 0.0) + (0.55 * (jj / max(1, H - 1)) ** 2 if face in SIDES else 0.0)
        mask = (m + bias) > 0.30
        mt = 0.45 + vz(ii * 0.5, jj * 0.5, salt + 11) * 1.0 + (m + bias - 0.3) * 0.5
        rgb = np.where(mask[..., None], palarr(MOSS, mt, ii, jj, 0.08), rgb)
        a = np.full((H, W), 255.0)
        glow = np.zeros((H, W, 4), np.float32)
        # cracks
        n = cracks * (3 if cracked else 1)
        if n:
            lab = crack_labels(W, H, salt * 31 + {"top": 1, "bottom": 2, "front": 3, "back": 4, "left": 5, "right": 6}[face], n, crack_len, cracked)
            on = lab > 0
            lit = on & (hv(lab, 0, salt + 21) < (1.01 if cracked else glow_frac))
            rgb = np.where(on[..., None], np.array(DARKEN, np.float32), rgb)
            amber = np.array(AMBER, np.float32)
            for r in range(3):
                glow[..., r] = np.where(lit, amber[r], 0.0)
            glow[..., 3] = np.where(lit, (255.0 if cracked else 150.0) * (0.75 + 0.25 * hv(ii, jj, salt + 23)), 0.0)
            if cracked:                                                                       # the stone beside a burning crack is lit orange too
                near = dilate(lit) & ~lit
                for r in range(3):
                    glow[..., r] = np.where(near, np.array(ORANGE, np.float32)[r], glow[..., r])
                glow[..., 3] = np.where(near, 95.0, glow[..., 3])
        if art is not None:
            art(face, rgb, a, glow, cracked)
        return rgb, a, glow

    def paint(face, i, j, fw, fh):
        k = (face, fw, fh)
        if k not in cache:
            cache[k] = build(face, fw, fh)
        rgb, a, glow = cache[k]
        if a[j, i] <= 0:
            return None, None
        c = (int(rgb[j, i, 0]), int(rgb[j, i, 1]), int(rgb[j, i, 2]), 255)
        g = glow[j, i]
        return c, ((int(g[0]), int(g[1]), int(g[2]), int(g[3])) if g[3] > 0 else None)
    return paint


# ------------------------------------------------------------------------------------------------------------------ hand-drawn details
def put(rgb, a, glow, x, y, col=None, g=None, ga=255.0):
    h, w = a.shape
    if 0 <= x < w and 0 <= y < h:
        if col is not None:
            rgb[y, x] = col
        if g is not None:
            glow[y, x] = (g[0], g[1], g[2], ga)


def face_art(face, rgb, a, glow, cracked):
    """The head's front (54 x 45 texels at 3 per pixel): the shadow under the brow, two tiny amber eyes in dark sockets, the ridge of the nose, a jagged mouth."""
    if face != "front":
        return
    H, W = a.shape
    ey = int(round(H * 0.46))
    bed = np.array(BED[0], np.float32)
    for j in range(int(H * 0.28), int(H * 0.52)):                         # the brow's shadow and the hollows under it
        k = max(0.0, 0.55 - 0.07 * (j - H * 0.28)) if j < H * 0.44 else 0.18
        rgb[j, :] = rgb[j, :] * (1 - k) + bed * k
    for cx in (int(round(W * 0.29)), int(round(W * 0.71))):               # the eyes: a bright core in a dark socket
        for j in range(ey - 3, ey + 4):
            for i in range(cx - 4, cx + 5):
                rgb[j, i] = bed
        for j in (ey - 1, ey):
            for i in range(cx - 2, cx + 3):
                put(rgb, a, glow, i, j, AMBER_HOT, AMBER_HOT, 255.0)
        for i in range(cx - 3, cx + 4):
            put(rgb, a, glow, i, ey - 1, None, ORANGE, 150.0)
            put(rgb, a, glow, i, ey, None, ORANGE, 150.0)
    mid = W // 2
    for j in range(ey, ey + 12):                                          # the bridge and the nostrils
        for i in (mid - 1, mid):
            rgb[j, i] = rgb[j, i] * 0.78
    for i in (mid - 3, mid + 2):
        put(rgb, a, glow, i, ey + 11, BED[0])
    m0 = int(round(H * 0.80))
    for i in range(int(W * 0.30), int(W * 0.70)):                         # the mouth: a long jagged slit, lit from inside
        top = m0 + (1 if (i * 7) % 5 == 0 else 0)
        for j in range(top, m0 + 4 + (1 if i % 3 == 0 else 0)):
            put(rgb, a, glow, i, j, BED[0])
            if j in (top + 1, top + 2) and W * 0.36 <= i <= W * 0.64:
                put(rgb, a, glow, i, j, None, AMBER, 230.0 if cracked else 120.0)
    for i in range(int(W * 0.34), int(W * 0.66), 3):                      # the teeth: blocks of stone across the slit
        put(rgb, a, glow, i, m0 + 1, STONE[5])
        put(rgb, a, glow, i + 1, m0 + 1, STONE[4])


def chest_art(face, rgb, a, glow, cracked):
    """The chest's front (80 x 44 texels): a jagged seam from the breastbone to the belly with the core burning in it (a faint hairline in the whole skin)."""
    if face != "front":
        return
    H, W = a.shape
    x = 40.0
    pts = []
    for j in range(2, H - 2):
        x += math.sin(j * 0.9) * 0.9 + (0.6 if j % 7 == 0 else 0.0) - 0.2
        pts.append((int(x), j))
    for (px, j) in pts:
        wide = 2 if cracked else 1
        for ox in range(-wide, wide + 1):
            put(rgb, a, glow, px + ox, j, DARKEN)
        put(rgb, a, glow, px, j, None, AMBER, 255.0 if cracked else 110.0)
        if cracked:
            put(rgb, a, glow, px - 1, j, None, ORANGE, 200.0)
            put(rgb, a, glow, px + 1, j, None, ORANGE, 200.0)
    if cracked:                                                           # the core: a pool of light where the seam opens
        for j in range(14, 30):
            for i in range(34, 47):
                d = math.hypot((i - 40.5) / 6.5, (j - 21.5) / 8.0)
                if d < 1.0:
                    put(rgb, a, glow, i, j, DARKEN if d > 0.5 else AMBER_HOT, AMBER_HOT if d < 0.5 else AMBER, 255.0 * (1.0 - 0.4 * d))


def rune_art(face, rgb, a, glow, cracked):
    """A carved rune on the stone fauld: a ring and a bar, cut dark and lit from within."""
    if face != "front":
        return
    H, W = a.shape
    cx, cy = W / 2.0, H / 2.0 - 1
    for j in range(H):
        for i in range(W):
            d = math.hypot(i + 0.5 - cx, (j + 0.5 - cy) * 1.0)
            ring = 5.0 <= d <= 6.6
            bar = abs(i + 0.5 - cx) < 0.9 and abs(j + 0.5 - cy) < 8.5
            if ring or bar:
                put(rgb, a, glow, i, j, DARKEN, AMBER, 230.0 if cracked else 150.0)


# ------------------------------------------------------------------------------------------------------------------ the model
class Build:
    """A model under construction: bones, boxes, animation (the same shape as tools/gen_zogratis_lucius.Build)."""

    def __init__(self, tw=512, th=512, dens=2):
        self.m = Model(KEY, NAME, tw, th)
        self.pk = Pack(tw, th, dens)
        self.A = Anim()

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def box(self, bone, origin, size, paint, **kw):
        return self.pk.box(self.m, bone, origin, size, paint, **kw)

    def finish(self):
        self.pk.finish()
        return self.pk.images()

    def height(self):
        used = self.pk.height_used()
        hh = 64
        while hh < used:
            hh += 64
        return hh


def build_model(cracked, tex_h=1024):
    """Every box of the giant. The skin (cracked or not) only changes how the boxes are painted, never where they are or how their faces are laid out."""
    b = Build(512, tex_h, 2)
    S = lambda salt, **kw: stone(salt, cracked=cracked, **kw)                    # noqa: E731

    # ----- bones (the model's left is +x; the pivots are the joints)
    b.bone("root", None, (0, 0, 0))
    b.bone("leg_l", "root", (11, 42, 0))
    b.bone("shin_l", "leg_l", (11, 22, 0))
    b.bone("leg_r", "root", (-11, 42, 0))
    b.bone("shin_r", "leg_r", (-11, 22, 0))
    b.bone("body", "root", (0, 44, 0))
    b.bone("chest", "body", (0, 54, 0))
    b.bone("head", "chest", (0, 78, -2))
    b.bone("arm_l", "chest", (25, 77, 0))
    b.bone("forearm_l", "arm_l", (25, 56, 0))
    b.bone("fist_l", "forearm_l", (25, 39, 0))
    b.bone("arm_r", "chest", (-25, 77, 0))
    b.bone("forearm_r", "arm_r", (-25, 56, 0))
    b.bone("fist_r", "forearm_r", (-25, 39, 0))

    # ----- legs: thick as pillars, short, with bedrock knees and mossy feet
    for suf, sg, s0 in (("_l", 1, 10), ("_r", -1, 20)):
        ox = 3 if sg > 0 else -19
        b.box("leg" + suf, (ox, 22, -7.5), (16, 20, 15), S(s0 + 1, cell=11, jb=7, moss=0.14, cracks=2))
        b.box("shin" + suf, (ox + 0.5, 6, -7), (15, 16, 14), S(s0 + 2, cell=10, jt=6, moss=0.16, cracks=2))
        b.box("shin" + suf, (ox - 1, 0, -14), (18, 6, 21), S(s0 + 3, cell=9, moss=0.38, cracks=1), faces=("top", "front", "back", "left", "right"))
        b.box("shin" + suf, (ox + 1, 18.5, -10), (14, 6, 3.5), S(s0 + 4, cell=6, moss=0.1, cracks=1), faces=("top", "front", "left", "right"))   # the knee guard
    # ----- belt, hips and the fauld with its rune
    b.box("body", (-14, 38, -8), (28, 12, 16), S(31, kind="brick", rh=7, bw=14, moss=0.15, cracks=2), faces=("top", "front", "back", "left", "right"))
    b.box("body", (-10, 29.5, -10.5), (20, 12, 3.5), S(32, kind="brick", rh=7, bw=12, moss=0.05, cracks=1, art=rune_art), faces=("front", "left", "right", "back", "bottom"))
    # ----- belly, chest, shoulders, the ridge down the back
    b.box("chest", (-15, 48, -9), (30, 10, 18), S(33, kind="brick", rh=8, bw=14, moss=0.3, cracks=2), faces=("top", "front", "back", "left", "right"))
    b.box("chest", (-20, 57, -11), (40, 21, 22), S(34, kind="brick", rh=9, bw=18, moss=0.28, cracks=3, art=chest_art), faces=("top", "front", "back", "left", "right"))
    for suf, sg, s0 in (("_l", 1, 40), ("_r", -1, 50)):
        ox = 14.5 if sg > 0 else -33.5
        b.box("chest", (ox, 73, -10), (19, 9, 20), S(s0 + 1, cell=12, moss=0.36, cracks=2), faces=("top", "front", "back", "left", "right"))          # the pauldron slab
        b.box("chest", (ox + (4 if sg > 0 else 5), 82, -3), (10, 3.5, 7), S(s0 + 2, cell=7, moss=0.4, cracks=1), faces=("top", "front", "back", "left", "right"))   # rubble on it
    for k, (y, w, d, jit, z) in enumerate(((74, 8, 5, 0.5, 11), (66, 9, 6, -0.5, 11), (58, 8, 6, 0.8, 11), (50, 7, 5, -0.6, 9))):
        b.box("chest", (-w / 2 + jit, y, z), (w, 9, d), S(60 + k, cell=6, moss=0.4, cracks=1), faces=("top", "front", "back", "left", "right"))
    # ----- the head: a heavy brow, a crest and a pair of shards of rubble; the face at 3 texels per pixel
    b.box("head", (-9, 78, -10), (18, 15, 18), S(70, cell=9, moss=0.14, cracks=1, art=face_art), dens={"front": 3, "left": 3, "right": 3, "back": 3, "top": 3},
          faces=("top", "front", "back", "left", "right"))
    b.box("head", (-10.5, 87.5, -13), (21, 5.5, 6.5), S(71, cell=7, moss=0.08, cracks=1), faces=("top", "front", "left", "right", "bottom"))
    b.box("head", (-7, 74, -12), (14, 5, 9), S(72, cell=6, moss=0.08, jb=1, cracks=1), faces=("front", "left", "right", "bottom"))
    b.box("head", (-4, 93, -7), (8, 3, 14), S(73, cell=6, moss=0.35, cracks=1), faces=("top", "front", "back", "left", "right"))
    b.box("head", (7, 92, -2.5), (3.5, 9, 5), S(74, cell=5, moss=0.15, cracks=1), rotation=(0, 0, -16), pivot=(8.75, 92, 0), faces=("top", "front", "back", "left", "right"))
    b.box("head", (-10.5, 92, -2.5), (3.5, 9, 5), S(75, cell=5, moss=0.15, cracks=1), rotation=(0, 0, 16), pivot=(-8.75, 92, 0), faces=("top", "front", "back", "left", "right"))
    # ----- arms: long enough for the fists to hang at the knees
    for suf, sg, s0 in (("_l", 1, 80), ("_r", -1, 90)):
        ox = 18 if sg > 0 else -32
        b.box("arm" + suf, (ox, 56, -7), (14, 22, 14), S(s0 + 1, cell=11, moss=0.25, cracks=2), faces=("front", "back", "left", "right", "bottom"))
        b.box("forearm" + suf, (ox - 0.5, 40, -7.5), (15, 17, 15), S(s0 + 2, cell=11, jt=5, jb=5, moss=0.2, cracks=2))
        fx = 15 if sg > 0 else -35
        b.box("fist" + suf, (fx, 21, -10), (20, 19, 20), S(s0 + 3, cell=12, moss=0.18, cracks=2))
        for k in range(3):                                                                # the knuckles
            kx = fx + 2 + k * 6.5
            b.box("fist" + suf, (kx, 30, -12.5), (5, 6, 3), S(s0 + 10 + k, cell=5, moss=0.05, cracks=0), faces=("top", "front", "left", "right"))
    return b


# ------------------------------------------------------------------------------------------------------------------ animation
def rot(a, clip, bone, pts, smooth=True):
    keys(a, clip, bone, "rot", pts, smooth_=smooth)


def pos(a, clip, bone, pts, smooth=True):
    keys(a, clip, bone, "pos", pts, smooth_=smooth)


def scl(a, clip, bone, pts):
    keys(a, clip, bone, "scale", pts)


def clips(a):
    """Every clip. A one-shot move clip has its blow at 80% of its length (the renderer paces the wind-up onto the first 80%). Arms: +x raises them forward,
    +z (left arm) / -z (right arm) swings them out. Legs: +x forward, a knee folds with a negative x on the shin. The body leans forward with a negative x."""
    # ---- idle: breathing stone
    L = 3.2
    a.clip("idle", L, True)
    cyc(a, "idle", "chest", "rot", L, swing((0, 0, 0), (1.3, 0.8, 0)))
    cyc(a, "idle", "chest", "pos", L, swing((0, 0, 0), (0, 0.7, 0)))
    cyc(a, "idle", "head", "rot", L, swing((0, 0, 0), (1.5, 3.0, 0), 0.8))
    cyc(a, "idle", "arm_l", "rot", L, swing((0, 0, 4), (2.2, 0, 0.8), 0.4))
    cyc(a, "idle", "arm_r", "rot", L, swing((0, 0, -4), (2.2, 0, -0.8), 2.6))
    cyc(a, "idle", "forearm_l", "rot", L, swing((6, 0, 0), (2.0, 0, 0), 1.0))
    cyc(a, "idle", "forearm_r", "rot", L, swing((6, 0, 0), (2.0, 0, 0), 3.2))
    cyc(a, "idle", "body", "pos", L, swing((0, 0, 0), (0, 0.4, 0)))

    # ---- walk: a heavy plod, the whole body rolling over each planted foot; 4.4 blocks of ground a cycle
    L = 2.0
    a.clip("walk", L, True)
    cyc(a, "walk", "leg_l", "rot", L, swing((0, 0, 0), (24, 0, 0)))
    cyc(a, "walk", "leg_r", "rot", L, swing((0, 0, 0), (-24, 0, 0)))
    cyc(a, "walk", "shin_l", "rot", L, lambda th: (-max(0.0, math.cos(th + 0.4)) * 34, 0, 0))
    cyc(a, "walk", "shin_r", "rot", L, lambda th: (-max(0.0, -math.cos(th + 0.4)) * 34, 0, 0))
    cyc(a, "walk", "arm_l", "rot", L, swing((0, 0, 5), (-17, 0, 0)))
    cyc(a, "walk", "arm_r", "rot", L, swing((0, 0, -5), (17, 0, 0)))
    cyc(a, "walk", "forearm_l", "rot", L, lambda th: (10 + 6 * SIN(th), 0, 0))
    cyc(a, "walk", "forearm_r", "rot", L, lambda th: (10 - 6 * SIN(th), 0, 0))
    cyc(a, "walk", "body", "rot", L, lambda th: (-3.0, 0, 3.5 * SIN(th)))
    cyc(a, "walk", "body", "pos", L, lambda th: (2.0 * SIN(th), -1.6 * abs(COS(th)) + 0.8, 0), n=1)
    cyc(a, "walk", "chest", "rot", L, lambda th: (0, -5.5 * SIN(th), -2.5 * SIN(th)))
    cyc(a, "walk", "head", "rot", L, lambda th: (2.0, 4.0 * SIN(th), 0))

    # ---- attack: the plain blow, an overhead hammer from the right fist (the clip starts 0.2 s in: the fist is already up)
    L = 1.0
    a.clip("attack", L, False)
    rot(a, "attack", "arm_r", [(0, (80, 0, -14)), (0.2, (150, 0, -18)), (0.34, (-6, 0, -4)), (0.5, (14, 0, -4)), (1.0, (0, 0, -4))])
    rot(a, "attack", "forearm_r", [(0, (30, 0, 0)), (0.2, (60, 0, 0)), (0.34, (0, 0, 0)), (1.0, (6, 0, 0))])
    rot(a, "attack", "chest", [(0, (6, -8, 0)), (0.2, (10, -14, 0)), (0.34, (-26, 12, 0)), (0.55, (-8, 4, 0)), (1.0, (0, 0, 0))])
    rot(a, "attack", "body", [(0, (0, 0, 0)), (0.34, (-8, 0, 0)), (1.0, (0, 0, 0))])
    rot(a, "attack", "arm_l", [(0, (-14, 0, 8)), (0.34, (24, 0, 10)), (1.0, (0, 0, 4))])
    rot(a, "attack", "head", [(0, (0, 0, 0)), (0.34, (-12, 0, 0)), (1.0, (0, 0, 0))])
    rot(a, "attack", "leg_l", [(0, (0, 0, 0)), (0.34, (18, 0, 0)), (1.0, (0, 0, 0))])
    rot(a, "attack", "leg_r", [(0, (0, 0, 0)), (0.34, (-8, 0, 0)), (1.0, (0, 0, 0))])

    # ---- cast: the generic wind-up, arms lifted away from the body, the chest swelling (fallback for a move without a clip of its own)
    L = 1.6
    a.clip("cast", L, True)
    cyc(a, "cast", "arm_l", "rot", L, swing((40, 0, 38), (6, 0, 4)))
    cyc(a, "cast", "arm_r", "rot", L, swing((40, 0, -38), (6, 0, -4), 1.2))
    cyc(a, "cast", "forearm_l", "rot", L, lambda th: (24, 0, 0))
    cyc(a, "cast", "forearm_r", "rot", L, lambda th: (24, 0, 0))
    cyc(a, "cast", "chest", "rot", L, swing((6, 0, 0), (2.5, 0, 0)))
    cyc(a, "cast", "head", "rot", L, swing((10, 0, 0), (2.0, 0, 0)))
    cyc(a, "cast", "body", "pos", L, swing((0, 0, 0), (0, 0.9, 0)))

    # ---- cast_big: the Bedrock Quake. Both fists overhead, trembling, the whole body drawn up (the blow lands at 80%: 1.92 s), then both fists driven into the floor
    L = 2.4
    a.clip("cast_big", L, False)
    rot(a, "cast_big", "arm_l", [(0, (0, 0, 4)), (0.8, (110, 0, 14)), (1.5, (172, 0, 10)), (1.7, (176, 0, 9)), (1.84, (178, 0, 8)), (1.92, (22, 0, 8)), (2.08, (26, 0, 6)), (2.4, (8, 0, 5))])
    rot(a, "cast_big", "arm_r", [(0, (0, 0, -4)), (0.8, (110, 0, -14)), (1.5, (172, 0, -10)), (1.7, (176, 0, -9)), (1.84, (178, 0, -8)), (1.92, (22, 0, -8)), (2.08, (26, 0, -6)), (2.4, (8, 0, -5))])
    rot(a, "cast_big", "forearm_l", [(0, (6, 0, 0)), (1.5, (6, 0, 0)), (1.92, (6, 0, 0)), (2.4, (8, 0, 0))])
    rot(a, "cast_big", "forearm_r", [(0, (6, 0, 0)), (1.5, (6, 0, 0)), (1.92, (6, 0, 0)), (2.4, (8, 0, 0))])
    rot(a, "cast_big", "chest", [(0, (0, 0, 0)), (1.2, (14, 0, 0)), (1.84, (20, 0, 0)), (1.92, (-34, 0, 0)), (2.1, (-30, 0, 0)), (2.4, (-6, 0, 0))])
    rot(a, "cast_big", "body", [(0, (0, 0, 0)), (1.84, (4, 0, 0)), (1.92, (-10, 0, 0)), (2.4, (-2, 0, 0))])
    rot(a, "cast_big", "head", [(0, (0, 0, 0)), (1.2, (-16, 0, 0)), (1.84, (-22, 0, 0)), (1.92, (14, 0, 0)), (2.4, (2, 0, 0))])
    rot(a, "cast_big", "leg_l", [(0, (0, 0, 0)), (1.84, (-4, 0, 3)), (1.92, (16, 0, 4)), (2.4, (2, 0, 0))])
    rot(a, "cast_big", "leg_r", [(0, (0, 0, 0)), (1.84, (-4, 0, -3)), (1.92, (16, 0, -4)), (2.4, (2, 0, 0))])
    rot(a, "cast_big", "shin_l", [(0, (0, 0, 0)), (1.84, (0, 0, 0)), (1.92, (-26, 0, 0)), (2.4, (-3, 0, 0))])
    rot(a, "cast_big", "shin_r", [(0, (0, 0, 0)), (1.84, (0, 0, 0)), (1.92, (-26, 0, 0)), (2.4, (-3, 0, 0))])
    pos(a, "cast_big", "root", [(0, (0, 0, 0)), (1.84, (0, 3, 0)), (1.92, (0, -4, 0)), (2.4, (0, 0, 0))])

    # ---- hurt: a flinch, the chest rocking back
    L = 0.5
    a.clip("hurt", L, False)
    rot(a, "hurt", "chest", [(0, (0, 0, 0)), (0.1, (9, 2, 2)), (0.5, (0, 0, 0))])
    rot(a, "hurt", "head", [(0, (0, 0, 0)), (0.1, (14, -6, 0)), (0.5, (0, 0, 0))])
    rot(a, "hurt", "arm_l", [(0, (0, 0, 4)), (0.12, (-10, 0, 9)), (0.5, (0, 0, 4))])
    rot(a, "hurt", "arm_r", [(0, (0, 0, -4)), (0.12, (-10, 0, -9)), (0.5, (0, 0, -4))])

    # ---- transform: the stone cracks. Crouch and clench (0 - 1.0 s), shudder, burst open at the middle (1.5 s: the skin changes), roar, settle (3.0 s)
    L = 3.0
    a.clip("transform", L, False)
    rot(a, "transform", "arm_l", [(0, (0, 0, 4)), (0.6, (58, 0, -10)), (1.2, (74, 0, -18)), (1.45, (80, 0, -20)), (1.6, (10, 0, 70)), (2.2, (22, 0, 58)), (3.0, (0, 0, 5))])
    rot(a, "transform", "arm_r", [(0, (0, 0, -4)), (0.6, (58, 0, 10)), (1.2, (74, 0, 18)), (1.45, (80, 0, 20)), (1.6, (10, 0, -70)), (2.2, (22, 0, -58)), (3.0, (0, 0, -5))])
    rot(a, "transform", "forearm_l", [(0, (6, 0, 0)), (1.2, (96, 0, 0)), (1.45, (100, 0, 0)), (1.6, (14, 0, 0)), (3.0, (6, 0, 0))])
    rot(a, "transform", "forearm_r", [(0, (6, 0, 0)), (1.2, (96, 0, 0)), (1.45, (100, 0, 0)), (1.6, (14, 0, 0)), (3.0, (6, 0, 0))])
    rot(a, "transform", "chest", [(0, (0, 0, 0)), (0.7, (-26, 0, 0)), (1.3, (-30, 0, 0)), (1.6, (22, 0, 0)), (2.3, (18, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "transform", "body", [(0, (0, 0, 0)), (0.7, (-10, 0, 0)), (1.3, (-12, 0, 0)), (1.6, (4, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "transform", "head", [(0, (0, 0, 0)), (1.3, (-20, 0, 0)), (1.6, (-30, 0, 0)), (2.3, (-24, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "transform", "leg_l", [(0, (0, 0, 0)), (0.7, (24, 0, 6)), (1.3, (26, 0, 8)), (1.6, (2, 0, 10)), (3.0, (0, 0, 0))])
    rot(a, "transform", "leg_r", [(0, (0, 0, 0)), (0.7, (24, 0, -6)), (1.3, (26, 0, -8)), (1.6, (2, 0, -10)), (3.0, (0, 0, 0))])
    rot(a, "transform", "shin_l", [(0, (0, 0, 0)), (0.7, (-44, 0, 0)), (1.3, (-48, 0, 0)), (1.6, (-4, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "transform", "shin_r", [(0, (0, 0, 0)), (0.7, (-44, 0, 0)), (1.3, (-48, 0, 0)), (1.6, (-4, 0, 0)), (3.0, (0, 0, 0))])
    pos(a, "transform", "root", [(0, (0, 0, 0)), (0.7, (0, -9, 0)), (1.3, (0, -11, 0)), (1.6, (0, 2, 0)), (2.2, (0, 0, 0)), (3.0, (0, 0, 0))])
    scl(a, "transform", "chest", [(0, (1, 1, 1)), (1.3, (1, 1, 1)), (1.6, (1.07, 1.05, 1.07)), (2.4, (1.03, 1.02, 1.03)), (3.0, (1, 1, 1))])

    # ---- death: the knees give, it topples forward onto them (the whole clip is 20 ticks of the dying body)
    L = 1.0
    a.clip("death", L, False)
    rot(a, "death", "leg_l", [(0, (0, 0, 0)), (0.5, (62, 0, 4)), (1.0, (84, 0, 6))])
    rot(a, "death", "leg_r", [(0, (0, 0, 0)), (0.5, (48, 0, -4)), (1.0, (80, 0, -6))])
    rot(a, "death", "shin_l", [(0, (0, 0, 0)), (0.5, (-72, 0, 0)), (1.0, (-100, 0, 0))])
    rot(a, "death", "shin_r", [(0, (0, 0, 0)), (0.5, (-60, 0, 0)), (1.0, (-96, 0, 0))])
    rot(a, "death", "body", [(0, (0, 0, 0)), (0.5, (-18, 0, 0)), (1.0, (-46, 0, 0))])
    rot(a, "death", "chest", [(0, (0, 0, 0)), (0.5, (-14, 4, 0)), (1.0, (-30, 0, 0))])
    rot(a, "death", "head", [(0, (0, 0, 0)), (0.5, (-10, 0, 0)), (1.0, (-28, 0, 0))])
    rot(a, "death", "arm_l", [(0, (0, 0, 4)), (0.5, (30, 0, 14)), (1.0, (62, 0, 10))])
    rot(a, "death", "arm_r", [(0, (0, 0, -4)), (0.5, (30, 0, -14)), (1.0, (62, 0, -10))])
    pos(a, "death", "root", [(0, (0, 0, 0)), (0.5, (0, -10, -2)), (1.0, (0, -27, -5))])

    # ---- rise: the clip time is the rise itself (0 to 1 over 2 s of clip): fists first, the arms thrust up, the hands plant on the floor, the chest heaves up
    L = 2.0
    a.clip("rise", L, False)
    rot(a, "rise", "arm_l", [(0, (176, 0, 8)), (0.5, (150, 0, 30)), (1.0, (40, 0, 72)), (1.5, (24, 0, 40)), (2.0, (0, 0, 4))])
    rot(a, "rise", "arm_r", [(0, (176, 0, -8)), (0.5, (150, 0, -30)), (1.0, (40, 0, -72)), (1.5, (24, 0, -40)), (2.0, (0, 0, -4))])
    rot(a, "rise", "forearm_l", [(0, (8, 0, 0)), (1.0, (26, 0, 0)), (2.0, (6, 0, 0))])
    rot(a, "rise", "forearm_r", [(0, (8, 0, 0)), (1.0, (26, 0, 0)), (2.0, (6, 0, 0))])
    rot(a, "rise", "chest", [(0, (-14, 0, 0)), (1.0, (-24, 0, 0)), (1.6, (12, 0, 0)), (2.0, (0, 0, 0))])
    rot(a, "rise", "body", [(0, (-10, 0, 0)), (1.0, (-16, 0, 0)), (2.0, (0, 0, 0))])
    rot(a, "rise", "head", [(0, (-26, 0, 0)), (1.2, (-18, 0, 0)), (1.7, (14, 0, 0)), (2.0, (0, 0, 0))])
    rot(a, "rise", "leg_l", [(0, (0, 0, 0)), (1.0, (12, 0, 4)), (2.0, (0, 0, 0))])
    rot(a, "rise", "leg_r", [(0, (0, 0, 0)), (1.0, (12, 0, -4)), (2.0, (0, 0, 0))])
    rot(a, "rise", "shin_l", [(0, (0, 0, 0)), (1.0, (-20, 0, 0)), (2.0, (0, 0, 0))])
    rot(a, "rise", "shin_r", [(0, (0, 0, 0)), (1.0, (-20, 0, 0)), (2.0, (0, 0, 0))])

    # ---- slam: the Ground Slam. Both fists lifted overhead, held and shaking, then smashed down (1.6 s of 2.0)
    L = 2.0
    a.clip("slam", L, False)
    rot(a, "slam", "arm_l", [(0, (0, 0, 4)), (0.7, (118, 0, 16)), (1.3, (168, 0, 12)), (1.5, (172, 0, 11)), (1.58, (172, 0, 11)), (1.66, (34, 0, 8)), (1.82, (38, 0, 6)), (2.0, (10, 0, 5))])
    rot(a, "slam", "arm_r", [(0, (0, 0, -4)), (0.7, (118, 0, -16)), (1.3, (168, 0, -12)), (1.5, (172, 0, -11)), (1.58, (172, 0, -11)), (1.66, (34, 0, -8)), (1.82, (38, 0, -6)), (2.0, (10, 0, -5))])
    rot(a, "slam", "forearm_l", [(0, (6, 0, 0)), (1.5, (6, 0, 0)), (1.66, (4, 0, 0)), (2.0, (8, 0, 0))])
    rot(a, "slam", "forearm_r", [(0, (6, 0, 0)), (1.5, (6, 0, 0)), (1.66, (4, 0, 0)), (2.0, (8, 0, 0))])
    rot(a, "slam", "chest", [(0, (0, 0, 0)), (1.0, (12, 0, 0)), (1.58, (18, 0, 0)), (1.66, (-32, 0, 0)), (1.85, (-28, 0, 0)), (2.0, (-8, 0, 0))])
    rot(a, "slam", "body", [(0, (0, 0, 0)), (1.58, (3, 0, 0)), (1.66, (-9, 0, 0)), (2.0, (-3, 0, 0))])
    rot(a, "slam", "head", [(0, (0, 0, 0)), (1.2, (-14, 0, 0)), (1.58, (-18, 0, 0)), (1.66, (12, 0, 0)), (2.0, (3, 0, 0))])
    rot(a, "slam", "leg_l", [(0, (0, 0, 0)), (1.58, (-3, 0, 2)), (1.66, (14, 0, 3)), (2.0, (3, 0, 0))])
    rot(a, "slam", "leg_r", [(0, (0, 0, 0)), (1.58, (-3, 0, -2)), (1.66, (14, 0, -3)), (2.0, (3, 0, 0))])
    rot(a, "slam", "shin_l", [(0, (0, 0, 0)), (1.58, (0, 0, 0)), (1.66, (-24, 0, 0)), (2.0, (-4, 0, 0))])
    rot(a, "slam", "shin_r", [(0, (0, 0, 0)), (1.58, (0, 0, 0)), (1.66, (-24, 0, 0)), (2.0, (-4, 0, 0))])
    pos(a, "slam", "root", [(0, (0, 0, 0)), (1.58, (0, 2.5, 0)), (1.66, (0, -3.5, 0)), (2.0, (0, 0, 0))])

    # ---- stomp: the Twin Stomp. The right foot rises, comes down (2.4 s of 3.0), and then the left follows (3.0 s: 12 ticks after the first)
    L = 3.0
    a.clip("stomp", L, False)
    rot(a, "stomp", "leg_r", [(0, (0, 0, 0)), (1.0, (46, 0, -4)), (2.2, (58, 0, -6)), (2.34, (58, 0, -6)), (2.44, (-2, 0, -2)), (2.7, (-3, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "stomp", "shin_r", [(0, (0, 0, 0)), (1.0, (-70, 0, 0)), (2.2, (-84, 0, 0)), (2.34, (-84, 0, 0)), (2.44, (-2, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "stomp", "leg_l", [(0, (0, 0, 0)), (2.45, (0, 0, 0)), (2.6, (30, 0, 4)), (2.9, (48, 0, 5)), (2.97, (48, 0, 5)), (3.0, (0, 0, 0))])
    rot(a, "stomp", "shin_l", [(0, (0, 0, 0)), (2.45, (0, 0, 0)), (2.6, (-52, 0, 0)), (2.9, (-70, 0, 0)), (2.97, (-70, 0, 0)), (3.0, (0, 0, 0))])
    rot(a, "stomp", "arm_l", [(0, (0, 0, 4)), (1.2, (28, 0, 36)), (2.3, (40, 0, 44)), (2.44, (10, 0, 12)), (2.9, (40, 0, 44)), (3.0, (6, 0, 6))])
    rot(a, "stomp", "arm_r", [(0, (0, 0, -4)), (1.2, (28, 0, -36)), (2.3, (40, 0, -44)), (2.44, (10, 0, -12)), (2.9, (40, 0, -44)), (3.0, (6, 0, -6))])
    rot(a, "stomp", "chest", [(0, (0, 0, 0)), (1.2, (4, 0, 4)), (2.3, (6, 0, 6)), (2.44, (-12, 0, 0)), (2.9, (4, 0, -5)), (3.0, (-6, 0, 0))])
    rot(a, "stomp", "body", [(0, (0, 0, 0)), (2.3, (0, 0, 3)), (2.44, (-5, 0, 0)), (3.0, (-3, 0, 0))])
    rot(a, "stomp", "head", [(0, (0, 0, 0)), (2.3, (-6, 0, 0)), (2.44, (6, 0, 0)), (3.0, (2, 0, 0))])
    pos(a, "stomp", "root", [(0, (0, 0, 0)), (2.3, (0, 1.4, 0)), (2.44, (0, -3.0, 0)), (2.9, (0, 1.4, 0)), (3.0, (0, -3.0, 0))])

    # ---- roar: the Roaring Shockwave. The chest swells, the head goes back, the arms open; at the blow (1.76 s of 2.2) it bellows forward
    L = 2.2
    a.clip("roar", L, False)
    rot(a, "roar", "head", [(0, (0, 0, 0)), (1.0, (-30, 0, 0)), (1.7, (-38, 0, 0)), (1.76, (12, 0, 0)), (2.2, (2, 0, 0))])
    rot(a, "roar", "chest", [(0, (0, 0, 0)), (1.0, (16, 0, 0)), (1.7, (22, 0, 0)), (1.76, (-14, 0, 0)), (2.2, (-2, 0, 0))])
    rot(a, "roar", "body", [(0, (0, 0, 0)), (1.7, (4, 0, 0)), (1.76, (-6, 0, 0)), (2.2, (0, 0, 0))])
    rot(a, "roar", "arm_l", [(0, (0, 0, 4)), (1.0, (14, 0, 56)), (1.7, (20, 0, 70)), (1.76, (30, 0, 34)), (2.2, (4, 0, 8))])
    rot(a, "roar", "arm_r", [(0, (0, 0, -4)), (1.0, (14, 0, -56)), (1.7, (20, 0, -70)), (1.76, (30, 0, -34)), (2.2, (4, 0, -8))])
    rot(a, "roar", "forearm_l", [(0, (6, 0, 0)), (1.7, (30, 0, 0)), (2.2, (8, 0, 0))])
    rot(a, "roar", "forearm_r", [(0, (6, 0, 0)), (1.7, (30, 0, 0)), (2.2, (8, 0, 0))])
    rot(a, "roar", "leg_l", [(0, (0, 0, 0)), (1.0, (-4, 0, 6)), (1.76, (4, 0, 8)), (2.2, (0, 0, 0))])
    rot(a, "roar", "leg_r", [(0, (0, 0, 0)), (1.0, (-4, 0, -6)), (1.76, (4, 0, -8)), (2.2, (0, 0, 0))])
    scl(a, "roar", "chest", [(0, (1, 1, 1)), (1.0, (1.06, 1.04, 1.06)), (1.7, (1.1, 1.06, 1.1)), (1.76, (1.0, 1.0, 1.0)), (2.2, (1, 1, 1))])
    pos(a, "roar", "root", [(0, (0, 0, 0)), (1.7, (0, 1.2, 0)), (1.76, (0, -1.5, 0)), (2.2, (0, 0, 0))])

    # ---- fist: the Bedrock Fist. The right arm drawn back and up, the left held out for balance, then one overhead hammer onto the mark (1.76 s of 2.2)
    L = 2.2
    a.clip("fist", L, False)
    rot(a, "fist", "arm_r", [(0, (0, 0, -4)), (0.9, (120, 0, -24)), (1.6, (166, 0, -14)), (1.72, (170, 0, -12)), (1.8, (36, 0, -6)), (1.95, (40, 0, -6)), (2.2, (10, 0, -5))])
    rot(a, "fist", "forearm_r", [(0, (6, 0, 0)), (1.0, (54, 0, 0)), (1.7, (74, 0, 0)), (1.8, (4, 0, 0)), (2.2, (8, 0, 0))])
    rot(a, "fist", "arm_l", [(0, (0, 0, 4)), (1.0, (30, 0, 52)), (1.7, (40, 0, 62)), (1.8, (26, 0, 30)), (2.2, (6, 0, 6))])
    rot(a, "fist", "chest", [(0, (0, 0, 0)), (1.2, (14, -16, 0)), (1.72, (20, -22, 0)), (1.8, (-34, 12, 0)), (2.0, (-26, 8, 0)), (2.2, (-4, 0, 0))])
    rot(a, "fist", "body", [(0, (0, 0, 0)), (1.72, (4, 0, 0)), (1.8, (-10, 0, 0)), (2.2, (-2, 0, 0))])
    rot(a, "fist", "head", [(0, (0, 0, 0)), (1.5, (-14, 12, 0)), (1.8, (14, -6, 0)), (2.2, (2, 0, 0))])
    rot(a, "fist", "leg_l", [(0, (0, 0, 0)), (1.72, (-6, 0, 3)), (1.8, (18, 0, 4)), (2.2, (2, 0, 0))])
    rot(a, "fist", "leg_r", [(0, (0, 0, 0)), (1.72, (6, 0, -3)), (1.8, (-8, 0, -4)), (2.2, (0, 0, 0))])
    rot(a, "fist", "shin_l", [(0, (0, 0, 0)), (1.8, (-30, 0, 0)), (2.2, (-3, 0, 0))])
    pos(a, "fist", "root", [(0, (0, 0, 0)), (1.72, (0, 2, 0)), (1.8, (0, -4, 0)), (2.2, (0, 0, 0))])

    # ---- line: the Seismic Line. It crouches, both fists to the floor, and then drives them forward along it (1.6 s of 2.0)
    L = 2.0
    a.clip("line", L, False)
    rot(a, "line", "arm_l", [(0, (0, 0, 4)), (0.8, (64, 0, 14)), (1.5, (74, 0, 12)), (1.6, (112, 0, 10)), (1.8, (100, 0, 9)), (2.0, (10, 0, 5))])
    rot(a, "line", "arm_r", [(0, (0, 0, -4)), (0.8, (64, 0, -14)), (1.5, (74, 0, -12)), (1.6, (112, 0, -10)), (1.8, (100, 0, -9)), (2.0, (10, 0, -5))])
    rot(a, "line", "forearm_l", [(0, (6, 0, 0)), (1.0, (36, 0, 0)), (1.6, (4, 0, 0)), (2.0, (8, 0, 0))])
    rot(a, "line", "forearm_r", [(0, (6, 0, 0)), (1.0, (36, 0, 0)), (1.6, (4, 0, 0)), (2.0, (8, 0, 0))])
    rot(a, "line", "chest", [(0, (0, 0, 0)), (0.9, (-36, 0, 0)), (1.5, (-42, 0, 0)), (1.6, (-30, 0, 0)), (2.0, (-6, 0, 0))])
    rot(a, "line", "body", [(0, (0, 0, 0)), (0.9, (-20, 0, 0)), (1.5, (-24, 0, 0)), (1.6, (-14, 0, 0)), (2.0, (-3, 0, 0))])
    rot(a, "line", "head", [(0, (0, 0, 0)), (0.9, (28, 0, 0)), (1.6, (22, 0, 0)), (2.0, (2, 0, 0))])
    rot(a, "line", "leg_l", [(0, (0, 0, 0)), (0.9, (36, 0, 5)), (1.6, (30, 0, 5)), (2.0, (2, 0, 0))])
    rot(a, "line", "leg_r", [(0, (0, 0, 0)), (0.9, (36, 0, -5)), (1.6, (30, 0, -5)), (2.0, (2, 0, 0))])
    rot(a, "line", "shin_l", [(0, (0, 0, 0)), (0.9, (-62, 0, 0)), (1.6, (-54, 0, 0)), (2.0, (-4, 0, 0))])
    rot(a, "line", "shin_r", [(0, (0, 0, 0)), (0.9, (-62, 0, 0)), (1.6, (-54, 0, 0)), (2.0, (-4, 0, 0))])
    pos(a, "line", "root", [(0, (0, 0, 0)), (0.9, (0, -14, 0)), (1.6, (0, -13, 0)), (2.0, (0, 0, 0))])

    # ---- rubble: the Rubble Fall. Both fists up against the ceiling, hammering at it with a growing shudder; at the blow (1.92 s of 2.4) they punch up
    L = 2.4
    a.clip("rubble", L, False)
    rot(a, "rubble", "arm_l", [(0, (0, 0, 4)), (0.7, (140, 0, 22)), (1.0, (164, 0, 14)), (1.2, (176, 0, 12)), (1.4, (160, 0, 14)), (1.6, (180, 0, 12)), (1.8, (162, 0, 14)),
                               (1.92, (186, 0, 10)), (2.1, (176, 0, 12)), (2.4, (8, 0, 5))])
    rot(a, "rubble", "arm_r", [(0, (0, 0, -4)), (0.7, (140, 0, -22)), (1.0, (164, 0, -14)), (1.1, (178, 0, -12)), (1.3, (160, 0, -14)), (1.5, (180, 0, -12)), (1.7, (162, 0, -14)),
                               (1.92, (186, 0, -10)), (2.1, (176, 0, -12)), (2.4, (8, 0, -5))])
    rot(a, "rubble", "forearm_l", [(0, (6, 0, 0)), (1.4, (20, 0, 0)), (1.92, (0, 0, 0)), (2.4, (8, 0, 0))])
    rot(a, "rubble", "forearm_r", [(0, (6, 0, 0)), (1.4, (20, 0, 0)), (1.92, (0, 0, 0)), (2.4, (8, 0, 0))])
    rot(a, "rubble", "chest", [(0, (0, 0, 0)), (1.0, (14, 0, 0)), (1.92, (24, 0, 0)), (2.1, (18, 0, 0)), (2.4, (0, 0, 0))])
    rot(a, "rubble", "head", [(0, (0, 0, 0)), (1.0, (-32, 0, 0)), (1.92, (-40, 0, 0)), (2.4, (0, 0, 0))])
    rot(a, "rubble", "body", [(0, (0, 0, 0)), (1.92, (5, 0, 0)), (2.4, (0, 0, 0))])
    pos(a, "rubble", "root", [(0, (0, 0, 0)), (1.0, (0, 1.5, 0)), (1.92, (0, 4.0, 0)), (2.1, (0, 2.0, 0)), (2.4, (0, 0, 0))])
    return a


def build_all():
    """Both skins. The geometry and the uv are the same, so the only difference is the paint; the whole thing is checked to be so."""
    b = build_model(False)
    img, glow = b.finish()
    th = b.height()
    b2 = build_model(True)
    img2, glow2 = b2.finish()
    a_geo = json.dumps(b.m.geo(), sort_keys=True)
    b_geo = json.dumps(b2.m.geo(), sort_keys=True)
    if a_geo != b_geo:
        raise RuntimeError("the two skins do not share one layout")
    clips(b.A)
    img, glow, img2, glow2 = (im.crop((0, 0, b.pk.W, th)) for im in (img, glow, img2, glow2))
    b.m.tex_h = th
    b.m.save(b.A)
    b.m.save_texture(img, glow)
    out = os.path.join(ROOT, "textures", "entity")
    img2.save(os.path.join(out, "goliath_cracked.png"))
    glow2.save(os.path.join(out, "goliath_cracked_glow.png"))
    quads = sum(len(c["uv"]) for bn in b.m.bones.values() for c in bn["cubes"])
    print(b.m.id, "cubes", b.m.cube_count(), "quads/pass", quads, "texture", b.pk.W, "x", th, "(rows used", b.pk.height_used(), ")")
    return b


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(anim, ages, views, tile=(420, 600), texture="goliath_body", sunk=0.0):
    s = {"kind": "geo", "model": "nusmp:geo/entity/goliath_body.geo.json", "texture": "nusmp:textures/entity/%s.png" % texture, "space": "PROP", "layer": "CUTOUT",
         "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile), "floor": True, "figure": False, "expect": [], "anim": anim, "views": views}
    return s


def tick(sec):
    return round(sec * 20.0, 1)


def scenes():
    sc = {}
    h, ty, d = 6.2, 3.1, 13.2
    for tex, tag in (("goliath_body", "body"), ("goliath_cracked", "cracked")):
        sc["goliath_%s_front" % tag] = scene("idle", [0], [view("front", 0, 3, (0, ty, 0), d), view("front 3/4", 35, 8, (0, ty, 0), d)], texture=tex)
        sc["goliath_%s_back" % tag] = scene("idle", [0], [view("back", 180, 5, (0, ty, 0), d), view("side", 90, 4, (0, ty, 0), d)], texture=tex)
        sc["goliath_%s_face" % tag] = scene("idle", [0], [view("face", 0, 0, (0, 5.3, 0), 4.6), view("face 3/4", 32, 6, (0, 5.3, 0), 4.6)], (420, 420), texture=tex)
    for clip, secs, n in (("walk", 2.0, 4), ("attack", 1.0, 5), ("cast", 1.6, 3), ("cast_big", 2.4, 6), ("hurt", 0.5, 3), ("transform", 3.0, 6), ("death", 1.0, 5),
                          ("rise", 2.0, 5), ("slam", 2.0, 6), ("stomp", 3.0, 6), ("roar", 2.2, 6), ("fist", 2.2, 6), ("line", 2.0, 5), ("rubble", 2.4, 6)):
        ages = [tick(secs * k / max(1, n - 1)) for k in range(n)]
        sc["goliath_%s" % clip] = scene(clip, ages, [view("3/4", 38, 6, (0, ty + (0.4 if clip in ("cast_big", "slam", "rubble", "transform", "rise") else 0.0), 0), d * 1.12)], (360, 520),
                                         texture="goliath_cracked" if clip in ("transform",) else "goliath_body")
    return sc


def write_scenes(path=SCENES_OUT):
    sc = scenes()
    with open(path, "w") as f:
        json.dump(sc, f, indent=1)
    return sc


# ------------------------------------------------------------------------------------------------------------------ checks
CLIP_NAMES = ("idle", "walk", "attack", "cast", "cast_big", "hurt", "transform", "death", "rise", "slam", "stomp", "roar", "fist", "line", "rubble")


def check():
    from PIL import Image
    paths = {"geo": os.path.join(ROOT, "geo", "entity", "goliath_body.geo.json"), "anim": os.path.join(ROOT, "animations", "entity", "goliath_body.animation.json")}
    for t in ("goliath_body", "goliath_body_glow", "goliath_cracked", "goliath_cracked_glow"):
        paths[t] = os.path.join(ROOT, "textures", "entity", t + ".png")
    bad = 0
    for k, p in paths.items():
        if not os.path.isfile(p):
            print("MISSING", k, p)
            bad += 1
    if bad:
        return 1
    geo = json.load(open(paths["geo"]))
    anim = json.load(open(paths["anim"]))
    g = geo["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    bones = {b["name"]: b for b in g["bones"]}
    for t in ("goliath_body", "goliath_body_glow", "goliath_cracked", "goliath_cracked_glow"):
        im = Image.open(paths[t])
        if im.size != (tw, th):
            print("PROBLEM texture", t, im.size, "!=", (tw, th))
            bad += 1
    for n in CLIP_NAMES:
        if n not in anim["animations"]:
            print("PROBLEM missing clip", n)
            bad += 1
    for cn, c in anim["animations"].items():
        for bn in c["bones"]:
            if bn not in bones:
                print("PROBLEM clip", cn, "animates the unknown bone", bn)
                bad += 1
    for bn, b in bones.items():
        par = b.get("parent")
        if par and par not in bones:
            print("PROBLEM bone", bn, "has the unknown parent", par)
            bad += 1
        for c in b["cubes"]:
            for face, uv in c["uv"].items():
                u, v = uv["uv"]
                w, h = uv["uv_size"]
                if u < 0 or v < 0 or u + w > tw or v + h > th:
                    print("PROBLEM uv out of the texture", bn, face, uv)
                    bad += 1
    cubes = sum(len(b["cubes"]) for b in bones.values())
    print("goliath_body: %d bones, %d cubes, %d clips, texture %dx%d, problems %d" % (len(bones), cubes, len(anim["animations"]), tw, th, bad))
    json.load(open(SCENES_OUT))
    return 1 if bad else 0


def render_previews(names=None):
    import shutil
    import subprocess
    sc = scenes()
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp"))
    os.makedirs(PREVIEW_OUT, exist_ok=True)
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    for l in (r.stdout + r.stderr).splitlines():
        if "Picked up" not in l and ("WARNING" in l or "ERROR" in l or "scenes:" in l):
            print(l)
    for n in names:
        src = os.path.join(env["RENDER_PREVIEW_BUILD"], n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(PREVIEW_OUT, n + ".png"))
    return r.returncode


def main(argv):
    build_all()
    write_scenes()
    return 0


if __name__ == "__main__":
    if "--check" in sys.argv:
        sys.exit(check())
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    sys.exit(main(sys.argv[1:]))
